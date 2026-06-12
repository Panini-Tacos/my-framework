#!/bin/bash

# Définition des variables
APP_NAME="framework"
SRC_DIR="src/main/java"
WEB_DIR="src/main/webapp"
BUILD_DIR="build"
LIB_DIR="lib"
TOMCAT_WEBAPPS="/home/unknowm/Documents/tomcat/apache-tomcat-10.0.16/webapps"
SERVLET_API_JAR="$LIB_DIR/servlet-api.jar"
MYSQL_CONNECTOR_JAR="$LIB_DIR/mysql-connector-j-9.5.0.jar"  # ✅ Nom corrigé

# ================================
# Nettoyage
# ================================
echo ">>> Nettoyage..."
rm -rf $BUILD_DIR
mkdir -p $BUILD_DIR/WEB-INF/classes
mkdir -p $BUILD_DIR/WEB-INF/lib

# ================================
# Vérification des JARs
# ================================
echo ">>> Vérification des JARs..."

if [ ! -f "$SERVLET_API_JAR" ]; then
    echo "❌ ERREUR : $SERVLET_API_JAR introuvable !"
    exit 1
fi

if [ ! -f "$MYSQL_CONNECTOR_JAR" ]; then
    echo "❌ ERREUR : $MYSQL_CONNECTOR_JAR introuvable !"
    exit 1
fi

echo "✅ JARs trouvés !"

# ================================
# Compilation
# ================================
echo ">>> Compilation..."

find $SRC_DIR -name "*.java" > sources.txt

javac -cp "$SERVLET_API_JAR:$MYSQL_CONNECTOR_JAR" \
      -d $BUILD_DIR/WEB-INF/classes \
      @sources.txt

# Vérification compilation
if [ $? -ne 0 ]; then
    echo "❌ ERREUR : Compilation échouée !"
    rm sources.txt
    exit 1
fi

rm sources.txt
echo "✅ Compilation réussie !"

# ================================
# Copie des fichiers web
# ================================
echo ">>> Copie des fichiers web..."

if [ -d "$WEB_DIR" ] && [ "$(ls -A $WEB_DIR)" ]; then
    cp -r $WEB_DIR/* $BUILD_DIR/
    echo "✅ Fichiers web copiés !"
else
    echo "⚠️  Aucun fichier web trouvé dans $WEB_DIR"
fi

# ================================
# Copie des JARs
# ================================
echo ">>> Copie des JARs..."
cp $LIB_DIR/*.jar $BUILD_DIR/WEB-INF/lib/
echo "✅ JARs copiés !"

# ================================
# Création du JAR (framework)
# ================================
echo ">>> Création du JAR..."
jar -cvf $BUILD_DIR/$APP_NAME.jar \
    -C $BUILD_DIR/WEB-INF/classes .
echo "✅ JAR créé : $BUILD_DIR/$APP_NAME.jar"

# ================================
# Création du WAR
# ================================
echo ">>> Création du WAR..."
cd $BUILD_DIR || exit
jar -cvf $APP_NAME.war *
cd ..
echo "✅ WAR créé : $BUILD_DIR/$APP_NAME.war"

# ================================
# Déploiement Tomcat
# ================================
echo ">>> Déploiement dans Tomcat..."
cp -f $BUILD_DIR/$APP_NAME.war $TOMCAT_WEBAPPS/
echo "✅ WAR déployé dans : $TOMCAT_WEBAPPS"

echo ""
echo "==============================="
echo "✅ Déploiement terminé !"
echo "==============================="
echo "   JAR : $BUILD_DIR/$APP_NAME.jar"
echo "   WAR : $TOMCAT_WEBAPPS/$APP_NAME.war"
echo ""