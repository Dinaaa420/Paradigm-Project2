<?php

declare(strict_types=1);

namespace Darija;

/** Thrown when the translator service answers with an error or cannot be reached. */
final class TranslatorException extends \RuntimeException
{
}

/**
 * Minimal PHP client for the Darija Translator REST service (cURL + HTTP Basic auth).
 */
final class TranslatorClient
{
    public function __construct(
        private readonly string $baseUrl,
        private readonly string $username,
        private readonly string $password,
        private readonly int $timeoutSeconds = 60,
    ) {
    }

    public static function fromEnvironment(): self
    {
        return new self(
            getenv('TRANSLATOR_URL') ?: 'http://localhost:8080',
            getenv('TRANSLATOR_USER') ?: 'student',
            getenv('TRANSLATOR_PASSWORD') ?: 'darija2026',
        );
    }

    /**
     * POST /api/translate
     *
     * @return array{translation:string, script:string, provider:string, model:string, cached:bool, durationMs:int}
     */
    public function translate(string $text, string $sourceLanguage = 'auto', string $script = 'arabic'): array
    {
        return $this->request('POST', '/api/translate', [
            'text' => $text,
            'sourceLanguage' => $sourceLanguage,
            'script' => $script,
        ]);
    }

    /** GET /api/health (public). */
    public function health(): array
    {
        return $this->request('GET', '/api/health', null, false);
    }

    private function request(string $method, string $path, ?array $body, bool $authenticate = true): array
    {
        $curl = curl_init(rtrim($this->baseUrl, '/') . $path);
        $headers = ['Accept: application/json'];
        $options = [
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_CUSTOMREQUEST => $method,
            CURLOPT_CONNECTTIMEOUT => 10,
            CURLOPT_TIMEOUT => $this->timeoutSeconds,
        ];
        if ($authenticate) {
            $options[CURLOPT_HTTPAUTH] = CURLAUTH_BASIC;
            $options[CURLOPT_USERPWD] = $this->username . ':' . $this->password;
        }
        if ($body !== null) {
            $headers[] = 'Content-Type: application/json';
            $options[CURLOPT_POSTFIELDS] = json_encode($body, JSON_UNESCAPED_UNICODE | JSON_THROW_ON_ERROR);
        }
        $options[CURLOPT_HTTPHEADER] = $headers;
        curl_setopt_array($curl, $options);

        $raw = curl_exec($curl);
        $status = (int) curl_getinfo($curl, CURLINFO_RESPONSE_CODE);
        $error = curl_error($curl);
        curl_close($curl);

        if ($raw === false) {
            throw new TranslatorException("Cannot reach the translator at {$this->baseUrl}: {$error}");
        }
        $json = json_decode((string) $raw, true);
        if ($status === 401) {
            throw new TranslatorException('Wrong username or password.');
        }
        if ($status >= 400 || !is_array($json)) {
            $message = is_array($json) && isset($json['message']) ? $json['message'] : "HTTP {$status}";
            throw new TranslatorException("Translation failed: {$message}");
        }
        return $json;
    }
}
