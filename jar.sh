#!/bin/bash

# Nettoyage
rm -rf out
mkdir -p out

# Compilation
javac -parameters -Xlint -cp "lib/*" -d out $(find . -name "*.java")

# Vérification
if [ $? -ne 0 ]; then
    echo "Erreur de compilation"
    exit 1
fi

# Création du JAR
jar cf JSF.jar -C out .
mkdir -p output 
mv JSF.jar output

rm ../JSF-test/lib/JSF.jar
cp output/JSF.jar ../JSF-test/lib/

echo "JAR créé : JSF.jar"
rm -rf out