javac -d bin src/**/*.java src/*.java

java -cp bin src.MrJackPocket

Si erreur lors de chargement des images : 
cp -r res bin/
java -cp "bin;res" src.MrJackPocket