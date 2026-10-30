# Images des badges d'arène

Ce dossier reçoit les images des 8 badges, affichées dans la boîte à badges de l'accueil et dans
l'animation qui range un nouveau badge. Il est ignoré par Git : seul ce fichier est versionné, aucune
image ne part sur le dépôt.

| Fichier | Badge | Arène |
|---|---|---|
| `1.png` | Badge Roche | Roxanne, Mérouville |
| `2.png` | Badge Poing | Bastien, Myokara |
| `3.png` | Badge Dynamo | Voltère, Lavandia |
| `4.png` | Badge Chaleur | Adriane, Vermilava |
| `5.png` | Badge Balance | Norman, Clémenti-Ville |
| `6.png` | Badge Plume | Alizée, Cimetronelle |
| `7.png` | Badge Esprit | Lévy et Tatia, Algatia |
| `8.png` | Badge Pluie | Marc, Atalanopolis |

- PNG à fond transparent, carré de préférence (256 × 256 suffit) : l'image est centrée et réduite pour tenir
  dans la case, sans être déformée.
- Sans fichier pour un badge, l'app dessine le badge original (`dessinerBadge` dans `ui/components/DessinBadge.kt`).
  On peut n'en fournir que certains.
- Les images sont lues à la compilation : après en avoir ajouté, relancer l'app depuis Android Studio.
