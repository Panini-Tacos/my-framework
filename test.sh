javac -Xlint -cp "lib/servlet-api.jar" -d test $(find . -name "*.java")
java -cp "lib/*.jar:test" lilifw.Main

rm -Rf test