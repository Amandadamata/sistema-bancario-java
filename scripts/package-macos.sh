#!/bin/sh
# Builds dist/Linha.app (Java included) and the two ZIPs published with each release.
set -eu
cd "$(dirname "$0")/.."
if [ "$(uname -s)" != Darwin ]; then
  echo 'Build the macOS app on a Mac.' >&2
  exit 1
fi
if [ -e dist/Linha.app ]; then
  echo 'dist/Linha.app already exists. Move the previous version before building another.' >&2
  exit 1
fi
VERSION=1.3.0
JAR="sistema-bancario-java-$VERSION.jar"

./mvnw --batch-mode clean verify
mkdir -p target/jpackage-input dist
cp "target/$JAR" target/jpackage-input/
jpackage --type app-image --name Linha --app-version "$VERSION" \
  --input target/jpackage-input --main-jar "$JAR" \
  --main-class com.github.amandadamata.sistemabancario.DesktopMain \
  --mac-package-identifier com.github.amandadamata.linha \
  --icon src/packaging/macos/Linha.icns \
  --java-options '--enable-native-access=ALL-UNNAMED' \
  --dest dist

# --norsrc and --noextattr keep the __MACOSX folder and ._ files out of the ZIP.
rm -f dist/Linha-Mac-AppleSilicon.zip dist/Linha-portable-JAR.zip
ditto -c -k --norsrc --noextattr --keepParent dist/Linha.app dist/Linha-Mac-AppleSilicon.zip

rm -rf dist/linha-portable-jar
mkdir dist/linha-portable-jar
cp "target/$JAR" dist/linha-portable-jar/Linha.jar
cp src/packaging/README.txt dist/linha-portable-jar/
ditto -c -k --norsrc --noextattr --keepParent dist/linha-portable-jar dist/Linha-portable-JAR.zip

echo 'Built: dist/Linha.app, dist/Linha-Mac-AppleSilicon.zip and dist/Linha-portable-JAR.zip'
