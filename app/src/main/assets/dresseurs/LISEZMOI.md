# Images des dresseurs

Les images sont fournies par toi : elles ne sont pas versionnées (dossier ignoré par Git).

## Comment les nommer

- Minuscules, sans accents, espaces et apostrophes remplacés par `-`, `&` écrit `et`, `œ` écrit `oe`.
- Extension `.jpg`, `.png` ou `.webp`.
- L'app cherche d'abord `<cle>.*` (la clé du dresseur dans `assets/donnees/dresseurs.json`), puis son image
  de repli (`"image"` dans le même fichier) : le personnage pour un dresseur nommé, sa classe pour les autres.
  Une seule image `gamin.jpg` sert donc à tous les Gamins.
- Pour donner une image à un combat précis, utilise sa clé : `max-mont-chimnee.jpg` passe avant `max.jpg`.

## Personnages

`flora`, `max`, `kelvin`, `lilie`, `timmy`, `amaryllis`, `damien`, `spectra`, `glacia`, `aragon`,
`pierre-rochard`, et les champions `roxanne`, `bastien`, `voltere`, `adriane`, `norman`, `alizee`,
`levy-et-tatia`, `marc`.

## Classes de dresseurs

`aromatherapeute`, `canon`, `cerveau-et-muscles`, `collectionneur`, `combattante`, `dracologue`, `eleve`,
`eleveur`, `eleveuse`, `entomomaniac`, `expert`, `experte`, `fan-de-camping`, `fan-de-pique-nique`,
`fille-a-la-bouee`, `fillette`, `fillette-de-conte`, `flotteur`, `frere-et-soeur`, `gamin`, `garcon-a-la-bouee`,
`gentleman`, `guitariste`, `jeune-couple`, `journalistes`, `jumelles`, `karateka`, `kinesiste`, `mademoiselle`,
`marin`, `montagnard`, `mystimaniac`, `nageur`, `nageuse`, `ninja-amateur`, `ornithologue`, `pecheur`,
`pecheuse-de-perles`, `plongeur`, `pokefan`, `pokemaniac`, `pokemon-ranger`, `randonneur`, `richard`,
`ruinemaniac`, `saltimbanque`, `sbire-de-la-team-magma`, `scout`, `senior-et-junior`, `soeur-parasol`,
`soeurs-etranges`, `topdresseur`, `topdresseuse`, `topduo`, `terreur`, `triathlete`, `vieux-couple`.

## Format

Portrait d'environ 900 × 1 800 px, personnage au centre. Sans image, la fiche affiche un fond aux couleurs
du type du dresseur.
