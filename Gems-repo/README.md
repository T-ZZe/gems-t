# Gems (Paper 1.21.11)

Build : `mvn clean package` -> `target/Gems.jar` (Java 21), à mettre dans `plugins/`.

Toutes les commandes sont réservées aux modos (permission `gems.admin`) :
- `/gem give <joueur> <gem> [nombre]`
- `/gem craft <gem>` : éditeur de craft en interface (grille, activer/désactiver, limite, compteur)
- `/gem list`
- `/gem reload`

Les crafts sont sauvegardés dans `plugins/Gems/crafts.yml`.
