# Images des espèces

Ce dossier reçoit les images des espèces affichées dans le Pokédex. Il est ignoré par Git :
seul ce fichier est versionné, aucune image ne part sur le dépôt.

- Un fichier PNG par espèce, nommé par son **numéro national** : `252.png`, `265.png`…
- Image carrée de préférence, fond transparent (l'ombre des espèces non vues est tirée de la transparence).
- Sans fichier, l'app affiche l'image par défaut (`spriteParDefaut` dans `ui/components/SpriteEspece.kt`).
- Les images sont lues à la compilation : après en avoir ajouté, relancer l'app depuis Android Studio.
