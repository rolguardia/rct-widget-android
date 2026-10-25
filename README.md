# Widget RCT pour Android TV / Monet

Projet Android Kotlin minimal qui fournit un widget système affichant le prochain match du Rugby Club Toulonnais et sa chaîne de diffusion.

## Important avant de commencer

- La compilation est effectuée dans GitHub Actions : Android Studio n'est pas nécessaire sur votre PC.
- **La présence d'un widget Android dans l'APK ne garantit pas que Monet Launcher accepte de l'afficher.** Il faut vérifier le support des widgets tiers sur votre version/appareil.
- Le projet n'est pas encore relié à une source officielle automatique de calendrier ou de programmation TV. Les données distantes viennent d'un JSON HTTPS que vous gérez. Ne renseignez `channelConfirmed: true` que lorsque la chaîne exacte a été vérifiée.
- Les informations d'exemple ne sont pas des informations de match réelles.

## Étape 1 — Créer le dépôt GitHub

1. Connectez-vous à https://github.com/new
2. Nom du dépôt : `rct-widget-android`
3. Choisissez **Public** ou **Private** (au choix).
4. Créez le dépôt, idéalement vide sans README généré par GitHub.

## Étape 2 — Envoyer les fichiers

1. Téléchargez et décompressez l'archive ZIP de ce projet.
2. Ouvrez votre nouveau dépôt GitHub dans le navigateur.
3. Cliquez sur **Add file → Upload files**.
4. Envoyez le contenu du dossier décompressé (les dossiers `app`, `.github`, `data` et les fichiers Gradle), pas le ZIP lui-même.
5. Cliquez sur **Commit changes**. La branche doit s'appeler `main`.

## Étape 3 — Compiler sans Android Studio

1. Dans le dépôt, ouvrez l'onglet **Actions**.
2. Sélectionnez **Build RCT Widget APK** dans la liste à gauche.
3. Cliquez sur **Run workflow**, puis sur le bouton vert **Run workflow**.
4. Attendez que l'exécution soit terminée avec une coche verte.
5. Ouvrez l'exécution terminée et descendez à **Artifacts**.
6. Téléchargez `rct-widget-debug-apk` et décompressez-le : il contient `app-debug.apk`.

Le premier build peut prendre quelques minutes. GitHub Actions télécharge automatiquement le JDK, Gradle et les composants Android nécessaires.

## Étape 4 — Fournir les données du match

Le widget peut lire un fichier JSON public accessible en HTTPS. Le format attendu est montré dans `data/match.example.json` :

- `competition` : compétition (ex. TOP 14)
- `matchTitle` : affichez les deux équipes
- `kickoff` : date/heure à afficher, déjà formatée en français
- `channel` : chaîne exacte (ex. Canal+ Sport ou Canal+ Live 1)
- `channelConfirmed` : `true` uniquement après vérification
- `updatedAt` : date/heure de la dernière vérification

Pour publier le JSON avec GitHub, vous pouvez copier son contenu dans un fichier `match.json` à la racine du dépôt, puis utiliser l'URL brute HTTPS du fichier. Attention : un dépôt privé ne fournira pas un JSON public accessible sans authentification.

**Configurer l'URL :** dans `app/src/main/res/values/strings.xml`, remplacez le contenu vide de `match_json_url` par l'URL HTTPS du JSON, par exemple :

```xml
<string name="match_json_url" translatable="false">https://raw.githubusercontent.com/VOTRE_COMPTE/rct-widget-android/main/match.json</string>
```

Committez les changements et relancez la compilation via Actions. Le widget tentera de récupérer le JSON lorsqu'il est actualisé. Android peut limiter les mises à jour périodiques ; ce mécanisme n'est pas un système temps réel.

## Étape 5 — Installer et afficher

1. Transférez `app-debug.apk` sur votre appareil Android TV (clé USB, application de transfert ou téléchargement depuis un emplacement de confiance).
2. Autorisez temporairement l'installation d'applications provenant de cette source si Android vous le demande.
3. Installez l'APK.
4. Cherchez « Prochain match du RCT » dans le sélecteur de widgets Android, s'il existe sur votre appareil/launcher.

Si Monet ne propose pas les widgets Android tiers, l'installation de l'APK ne fera pas apparaître automatiquement le widget dans Monet. Il faudra alors confirmer si Monet offre une API ou un mécanisme d'extension, ou envisager une autre présentation compatible.

## Dépannage

- **Action absente** : vérifier que les fichiers sont à la racine du dépôt et que la branche s'appelle `main`.
- **Compilation rouge** : ouvrir le run Actions, cliquer sur le job `build`, puis copier ici le message d'erreur.
- **APK installée mais widget introuvable** : probablement une limitation du launcher, pas une erreur de compilation.
- **Chaîne incorrecte ou inconnue** : laisser `channelConfirmed` à `false` ou `channel` vide. Ne pas deviner le canal Live.
