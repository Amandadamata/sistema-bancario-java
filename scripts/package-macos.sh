#!/bin/sh
# Gera dist/Linha.app (com Java incluído) e os dois ZIPs publicados nas releases.
set -eu
cd "$(dirname "$0")/.."
if [ "$(uname -s)" != Darwin ]; then
  echo 'Gere o aplicativo macOS em um Mac.' >&2
  exit 1
fi
if [ -e dist/Linha.app ]; then
  echo 'dist/Linha.app já existe. Mova a versão anterior antes de gerar outra.' >&2
  exit 1
fi
VERSAO=1.2.0
JAR="sistema-bancario-java-$VERSAO.jar"

./mvnw --batch-mode clean verify
mkdir -p target/jpackage-input dist
cp "target/$JAR" target/jpackage-input/
jpackage --type app-image --name Linha --app-version "$VERSAO" \
  --input target/jpackage-input --main-jar "$JAR" \
  --main-class com.github.amandadamata.sistemabancario.DesktopMain \
  --mac-package-identifier com.github.amandadamata.linha \
  --icon src/packaging/macos/Linha.icns \
  --java-options '--enable-native-access=ALL-UNNAMED' \
  --dest dist

# --norsrc e --noextattr evitam a pasta __MACOSX e os arquivos ._ dentro do ZIP.
rm -f dist/Linha-Mac-AppleSilicon.zip dist/Linha-JAR-portatil.zip
ditto -c -k --norsrc --noextattr --keepParent dist/Linha.app dist/Linha-Mac-AppleSilicon.zip

rm -rf dist/linha-jar-portatil
mkdir dist/linha-jar-portatil
cp "target/$JAR" dist/linha-jar-portatil/Linha.jar
cp src/packaging/LEIA-ME.txt dist/linha-jar-portatil/
ditto -c -k --norsrc --noextattr --keepParent dist/linha-jar-portatil dist/Linha-JAR-portatil.zip

echo 'Gerados: dist/Linha.app, dist/Linha-Mac-AppleSilicon.zip e dist/Linha-JAR-portatil.zip'
