#!/bin/bash

# Définition des variables
APP_NAME="Banky"
SRC_DIR="src/main/java"
WEB_DIR="src/main/webapp"
BUILD_DIR="build"
LIB_DIR="lib"
TOMCAT_WEBAPPS="/home/tflow/Documents/s3/progsys/apache-tomcat-10.0.16/webapps"
SERVLET_API_JAR="$LIB_DIR/servlet-api.jar"
MYSQL_CONNECTOR_JAR="$LIB_DIR/mysql-connector-java-9.5.0.jar"

# Nettoyage et création du répertoire temporaire
# rm -rf $BUILD_DIR
mkdir -p $BUILD_DIR/WEB-INF/classes
# Compilation des fichiers Java avec les JARs nécessaires
find $SRC_DIR -name "*.java" > sources.txt
javac -cp "$SERVLET_API_JAR:$MYSQL_CONNECTOR_JAR" -d $BUILD_DIR/WEB-INF/classes @sources.txt
rm sources.txt

# Copier les fichiers web (web.xml, JSP, etc.)
cp -r $WEB_DIR/* $BUILD_DIR/

# Copier les JARs de lib dans WEB-INF/lib
mkdir -p $BUILD_DIR/WEB-INF/lib
cp $LIB_DIR/*.jar $BUILD_DIR/WEB-INF/lib/

# Générer le fichier .war dans le dossier build
cd $BUILD_DIR || exit
jar -cvf $APP_NAME.war *
cd ..

# Déploiement dans Tomcat
cp -f $BUILD_DIR/$APP_NAME.war $TOMCAT_WEBAPPS/

echo ""

echo "Déploiement terminé. Redémarrez Tomcat si nécessaire."

echo ""
