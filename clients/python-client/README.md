# Python client

Command-line app (plus a reusable `TranslatorClient` class) for the Darija Translator REST service.

```bash
python -m venv .venv && source .venv/bin/activate     # Windows: .venv\Scripts\activate
pip install -r requirements.txt

export TRANSLATOR_URL=http://localhost:8080 TRANSLATOR_USER=student TRANSLATOR_PASSWORD=darija2026

python -m darija_client "Good morning, how are you?"
python -m darija_client --latin -v "I want a coffee please"
echo "Thank you very much" | python -m darija_client
python -m darija_client          # interactive mode (:latin, :arabic, :source French, :quit)
```

Use it from your own code:

```python
from darija_client import TranslatorClient

client = TranslatorClient("http://localhost:8080", "student", "darija2026")
print(client.translate("See you tomorrow", script="latin").translation)
```

Run the tests (they start nothing; they use a fake HTTP server): `python -m unittest -v`
