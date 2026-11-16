package ma.darija.translator.security;

import jakarta.security.auth.message.AuthException;
import jakarta.security.auth.message.AuthStatus;
import jakarta.security.auth.message.MessageInfo;
import jakarta.security.auth.message.MessagePolicy;
import jakarta.security.auth.message.callback.CallerPrincipalCallback;
import jakarta.security.auth.message.callback.GroupPrincipalCallback;
import jakarta.security.auth.message.module.ServerAuthModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * HTTP Basic authentication implemented as a <b>Jakarta Authentication</b> {@link ServerAuthModule} (SAM).
 * <p>
 * The servlet container calls {@link #validateRequest} before every request reaches the
 * Jakarta REST resources. The module either
 * <ul>
 *   <li>lets the request through anonymously (CORS pre-flight, public paths such as /api/health),</li>
 *   <li>authenticates the caller and hands the identity to the container through the standard
 *       {@link CallerPrincipalCallback} / {@link GroupPrincipalCallback}, or</li>
 *   <li>answers 401 with a {@code WWW-Authenticate: Basic} challenge.</li>
 * </ul>
 * Because it only uses the Jakarta Authentication API it runs unchanged on Tomcat, GlassFish,
 * Payara, WildFly, Open Liberty...
 */
public class BasicAuthServerAuthModule implements ServerAuthModule {

    private static final Logger LOG = Logger.getLogger(BasicAuthServerAuthModule.class.getName());
    private static final Class<?>[] SUPPORTED_MESSAGE_TYPES = {HttpServletRequest.class, HttpServletResponse.class};

    private final UserStore users;
    private final String realm;
    private final Set<String> publicPaths;
    private final String corsAllowedOrigin;
    private CallbackHandler handler;

    public BasicAuthServerAuthModule(UserStore users, String realm, Set<String> publicPaths, String corsAllowedOrigin) {
        this.users = users;
        this.realm = realm.replace("\"", "");
        this.publicPaths = Set.copyOf(publicPaths);
        this.corsAllowedOrigin = corsAllowedOrigin;
    }

    @Override
    public void initialize(MessagePolicy requestPolicy, MessagePolicy responsePolicy,
                           CallbackHandler handler, @SuppressWarnings("rawtypes") Map options) {
        this.handler = handler;
    }

    @Override
    public Class<?>[] getSupportedMessageTypes() {
        return SUPPORTED_MESSAGE_TYPES.clone();
    }

    @Override
    public AuthStatus validateRequest(MessageInfo messageInfo, Subject clientSubject, Subject serviceSubject)
            throws AuthException {
        HttpServletRequest request = (HttpServletRequest) messageInfo.getRequestMessage();
        HttpServletResponse response = (HttpServletResponse) messageInfo.getResponseMessage();

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if ("OPTIONS".equals(request.getMethod()) || publicPaths.contains(path)) {
            return anonymous(clientSubject);
        }

        Optional<BasicCredentials> credentials = BasicCredentials.parse(request.getHeader("Authorization"));
        if (credentials.isPresent()) {
            String username = credentials.get().username();
            Optional<Set<String>> groups = users.validate(username, credentials.get().password());
            if (groups.isPresent()) {
                handle(new CallerPrincipalCallback(clientSubject, username),
                        new GroupPrincipalCallback(clientSubject, groups.get().toArray(String[]::new)));
                return AuthStatus.SUCCESS;
            }
            LOG.warning(() -> "Failed login for user '" + username + "' from " + request.getRemoteAddr());
            challenge(response, "Invalid username or password");
            return AuthStatus.SEND_FAILURE;
        }

        challenge(response, "Authentication required (HTTP Basic)");
        return AuthStatus.SEND_CONTINUE;
    }

    @Override
    public AuthStatus secureResponse(MessageInfo messageInfo, Subject serviceSubject) {
        return AuthStatus.SEND_SUCCESS;
    }

    @Override
    public void cleanSubject(MessageInfo messageInfo, Subject subject) {
        if (subject != null) {
            subject.getPrincipals().clear();
        }
    }

    /** Jakarta Authentication way of saying "proceed without a caller identity". */
    private AuthStatus anonymous(Subject clientSubject) throws AuthException {
        handle(new CallerPrincipalCallback(clientSubject, (java.security.Principal) null));
        return AuthStatus.SUCCESS;
    }

    private void handle(Callback... callbacks) throws AuthException {
        try {
            handler.handle(callbacks);
        } catch (Exception e) {
            AuthException authException = new AuthException("Container rejected authentication callbacks");
            authException.initCause(e);
            throw authException;
        }
    }

    private void challenge(HttpServletResponse response, String message) throws AuthException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\", charset=\"UTF-8\"");
        CorsHeaders.apply(corsAllowedOrigin, response::setHeader);
        response.setContentType("application/json");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        try {
            response.getWriter().write("{\"error\":\"unauthorized\",\"message\":\"" + message + "\",\"status\":401}");
        } catch (IOException e) {
            AuthException authException = new AuthException("Cannot write 401 response");
            authException.initCause(e);
            throw authException;
        }
    }
}
