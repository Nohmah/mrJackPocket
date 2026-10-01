# Mr. Jack Pocket

Jeu de plateau numérique inspiré de **Mr. Jack Pocket**, réalisé en Java dans le
cadre du projet du groupe 19.

Le joueur peut incarner **Mr. Jack** ou l'**Enquêteur**. Le projet propose :

- une partie solo contre une intelligence artificielle ;
- une partie multijoueur en réseau local ;
- la sauvegarde et le chargement d'une partie ;
- un menu de règles et des options de jeu ;
- plusieurs implémentations d'IA, dont une recherche de type Minimax ;
- une interface graphique développée avec Java Swing.

## Prérequis

- **JDK 8 ou supérieur** ;
- Git, uniquement pour récupérer le dépôt ;
- un environnement graphique compatible avec Java Swing.

Aucune dépendance externe ni gestionnaire de paquets n'est nécessaire.

## Récupérer le projet

```bash
git clone <URL_DU_DEPOT>
cd groupe-19
```

## Lancer le projet

### Avec IntelliJ IDEA

1. Ouvrir le dossier du projet dans IntelliJ IDEA.
2. Vérifier que le SDK du projet correspond à un JDK installé.
3. Marquer le dossier `src` comme **Sources Root** si IntelliJ ne le détecte
   pas automatiquement.
4. Exécuter la classe `src.MrJackPocket` depuis `src/MrJackPocket.java`.

Les ressources graphiques sont stockées dans `res/Images`. Le dossier `res`
doit rester accessible à la racine du projet lors de l'exécution.

### Depuis un terminal

Depuis la racine du dépôt, compiler les sources dans le dossier `bin` :

**Windows PowerShell**

```powershell
New-Item -ItemType Directory -Force bin | Out-Null
javac -d bin (Get-ChildItem -Recurse src -Filter *.java | ForEach-Object { $_.FullName })
```

Puis lancer le menu principal :

```powershell
java -cp "bin;." src.MrJackPocket
```

**Linux/macOS**

```bash
mkdir -p bin
javac -d bin $(find src -name "*.java")
java -cp "bin:." src.MrJackPocket
```

Si les images ne sont pas trouvées, vérifier que le lancement est effectué
depuis la racine du projet et que le dossier `res` n'a pas été déplacé.

## Jouer en multijoueur

Le multijoueur fonctionne en réseau local :

1. Depuis le menu principal, choisir **Partie multijoueur**.
2. Le premier joueur crée la partie et devient l'hôte.
3. Le second joueur rejoint la partie avec l'adresse IP de l'hôte.
4. Les deux joueurs doivent indiquer qu'ils sont prêts pour lancer la partie.

Le serveur est démarré automatiquement par l'hôte et écoute sur le port
`1201`. Ce port doit être autorisé par le pare-feu et accessible entre les deux
machines. Pour jouer sur le même ordinateur, utiliser `localhost`.

## Sauvegardes et configuration

- `save.dat` contient la sauvegarde de la partie ;
- `pseudo.txt` contient le pseudonyme configuré dans les options.

Ces fichiers sont créés ou mis à jour dans le répertoire depuis lequel le
programme est lancé. Une sauvegarde existante peut être chargée depuis le menu
correspondant.

## Organisation du projet

```text
src/
├── MrJackPocket.java          # Point d'entrée de l'application
├── modele/                    # Règles du jeu, états et IA
├── reseau/                    # Client, serveur et messages réseau
├── utils/                     # Sauvegarde et chargement des ressources
└── vue/                       # Interface Swing, menus et affichage
res/Images/                    # Ressources graphiques
```

Les rapports de projet et d'IA sont disponibles à la racine du dépôt au format
PDF.

## Licence

Aucune licence open source n'est actuellement définie dans ce dépôt.
