<?php

declare(strict_types=1);

require __DIR__ . '/../src/TranslatorClient.php';

use Darija\TranslatorClient;
use Darija\TranslatorException;

$client = TranslatorClient::fromEnvironment();

$languages = ['auto' => 'Detect language', 'English' => 'English', 'French' => 'French',
    'Spanish' => 'Spanish', 'German' => 'German', 'Modern Standard Arabic' => 'Modern Standard Arabic'];

$text = '';
$source = 'auto';
$script = 'arabic';
$result = null;
$error = null;

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $text = trim((string) ($_POST['text'] ?? ''));
    $source = array_key_exists($_POST['source'] ?? '', $languages) ? $_POST['source'] : 'auto';
    $script = ($_POST['script'] ?? '') === 'latin' ? 'latin' : 'arabic';
    if ($text === '') {
        $error = 'Please type some text to translate.';
    } else {
        try {
            $result = $client->translate($text, $source, $script);
        } catch (TranslatorException $e) {
            $error = $e->getMessage();
        }
    }
}

function e(string $value): string
{
    return htmlspecialchars($value, ENT_QUOTES | ENT_SUBSTITUTE, 'UTF-8');
}
?>
<!doctype html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Darija Translator · PHP client</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
<main>
    <header>
        <h1>Darija Translator</h1>
        <p class="sub">PHP client for the LLM-powered REST service</p>
    </header>

    <form method="post" class="card">
        <label for="text">Text to translate</label>
        <textarea id="text" name="text" rows="5" maxlength="5000" required
                  placeholder="e.g. Where can I find a good restaurant?"><?= e($text) ?></textarea>

        <div class="row">
            <label>From
                <select name="source">
                    <?php foreach ($languages as $value => $label): ?>
                        <option value="<?= e($value) ?>" <?= $value === $source ? 'selected' : '' ?>><?= e($label) ?></option>
                    <?php endforeach; ?>
                </select>
            </label>
            <fieldset>
                <legend>Script</legend>
                <label><input type="radio" name="script" value="arabic" <?= $script === 'arabic' ? 'checked' : '' ?>> العربية</label>
                <label><input type="radio" name="script" value="latin" <?= $script === 'latin' ? 'checked' : '' ?>> Latin</label>
            </fieldset>
            <button type="submit">Translate</button>
        </div>
    </form>

    <?php if ($error !== null): ?>
        <p class="error" role="alert"><?= e($error) ?></p>
    <?php endif; ?>

    <?php if ($result !== null): ?>
        <section class="card result">
            <div class="output" id="output" dir="<?= $result['script'] === 'arabic' ? 'rtl' : 'ltr' ?>"
                 lang="ary"><?= e($result['translation']) ?></div>
            <div class="row meta">
                <button type="button" id="speak" data-script="<?= e($result['script']) ?>">🔊 Read aloud</button>
                <span><?= e($result['provider']) ?> / <?= e($result['model']) ?> ·
                    <?= $result['cached'] ? 'cached' : (int) $result['durationMs'] . ' ms' ?></span>
            </div>
        </section>
    <?php endif; ?>

    <footer>Server: <?= e(getenv('TRANSLATOR_URL') ?: 'http://localhost:8080') ?></footer>
</main>
<script>
    // Read aloud with the browser's built-in speech synthesis.
    document.getElementById('speak')?.addEventListener('click', (e) => {
        const latin = e.target.dataset.script === 'latin';
        const u = new SpeechSynthesisUtterance(document.getElementById('output').textContent);
        u.lang = latin ? 'fr-FR' : 'ar-MA';
        const voice = speechSynthesis.getVoices().find(v => v.lang.startsWith(latin ? 'fr' : 'ar'));
        if (voice) u.voice = voice;
        speechSynthesis.cancel();
        speechSynthesis.speak(u);
    });
</script>
</body>
</html>
