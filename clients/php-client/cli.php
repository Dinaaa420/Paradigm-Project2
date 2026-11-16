<?php

declare(strict_types=1);

// Command-line usage:  php cli.php "Good morning, how are you?" [--latin]

require __DIR__ . '/src/TranslatorClient.php';

use Darija\TranslatorClient;
use Darija\TranslatorException;

$args = array_slice($argv, 1);
$script = in_array('--latin', $args, true) ? 'latin' : 'arabic';
$text = implode(' ', array_filter($args, static fn ($a) => !str_starts_with($a, '--')));

if ($text === '') {
    fwrite(STDERR, "Usage: php cli.php \"text to translate\" [--latin]\n");
    exit(2);
}

try {
    $result = TranslatorClient::fromEnvironment()->translate($text, 'auto', $script);
    echo $result['translation'], PHP_EOL;
} catch (TranslatorException $e) {
    fwrite(STDERR, $e->getMessage() . PHP_EOL);
    exit(1);
}
