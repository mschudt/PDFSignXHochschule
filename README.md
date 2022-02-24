# Übersicht der Funktionsweise

1. Word-Dateien (DOC oder DOCX) werden durch Aufrufen des Programms `unoconv`, welches das `libreoffice` cli aufruft, zu
   PDF/A konformen PDF-Dateien konvertiert.
2. Die PDF/A Dateien werden mit Hilfer der Apache PDFBox Bibliothek mit einem Zertifikat der Hochschule Mannheim im
   PKCS#12 Format und dem dazugehörigen Passwort signiert.
3. Die Signatur der PDF/A Dateien wird durch die DSS Bibliothek validiert.
4. Die fertigen PDF/A Dateien werden mit der Apache Preflight Bibliothek auf Format-Korrektheit validiert.
5. Die XHochschule XML-Dateien (V0.7) werden mit den Daten der übergebenen Konfiguration erzeugt.
6. Die fertigen XHochschule XML-Dateien werden anhand des XHochschule XML-Schemas validiert.

# Abhängikeiten

- Apache Maven 3.8.4
- `unoconv` Version 0.9.0 (per Kommandozeile aufrufbar)
- Libreoffice 7.2.1.2
- veraPDF 1.18.6

Gegebenenfalls:

- `openssl` Version 1.1.1l (per Kommandozeile aufrufbar)

Die Entwicklung dieses Projektes wurde auf einem M1 Macbook Air unter macOS Monterey 12.1 durchgeführt.

Zum Kompilieren und Ausführen des Programms wurde `IntelliJ IDEA` in Kombination mit dem `OpenJDK 11` verwendet.

# Export des Programms
Das Programm kann mit dem inkludierten Skript `export_jar.sh` als ausführbare JAR-Datei exportiert werden.


# Aufrufen des Progamms
Das Programm kann mit folgendem Befehl ausgeführt werden:

`java -jar PDFSignXHochschule.jar exportDir=student_123456 inputFile=files/zeugnis.docx`

Die Verzeichnisse, die als Argument übergeben werden, sind relative Pfade.

# Erstellen eines selbst signiertes Zertifikat

Zum Testen des Signaturverfahrens wurde ein selbstsigniertes Zertifikat wie folgt via `openssl` generiert:

Erstellen eines X.509 Zertifikates `cert.pem` und eines Private Key `key.pem`:

```
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -sha256 -days 365
```

Kombinieren von `cert.pem` und `key.pem` zu `cert.p12` im `PKCS#12` Format:

```
openssl pkcs12 -export -out cert.p12 -in cert.pem -inkey key.pem
```

Aufteilen von `cert.p12` in `cert.pem` und `key.pem`:

```
openssl pkcs12 -in cert.p12 -nocerts -out key.pem -nodes
openssl pkcs12 -in cert.p12 -clcerts -nokeys -out cert.pem
```
