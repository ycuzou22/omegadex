# Images des dresseurs

Les images sont fournies par toi : elles ne sont pas versionnées (dossier ignoré par Git).

## Comment les nommer

- Minuscules, sans accents, espaces, tirets et apostrophes remplacés par `_`, `&` écrit `et`, `œ` écrit `oe`.
  L'app accepte aussi les tirets et les majuscules (`pierre-rochard.jpg`, `Pierre_Rochard.JPG`), mais les
  soulignés sont conseillés : Android refuse les tirets dans les ressources.
- Extension `.jpg`, `.jpeg`, `.png` ou `.webp`.
- L'app cherche d'abord `<cle>.*` (la clé du dresseur dans `assets/donnees/dresseurs.json`), puis son image
  de repli (`"image"` dans le même fichier) : le personnage pour un dresseur nommé, sa classe pour les autres.
  Une seule image `gamin.jpg` sert donc à tous les Gamins.
- Pour donner une image à un combat précis, utilise sa clé : `max_mont_chimnee.jpg` passe avant `max.jpg`.

## Personnages

`flora`, `max`, `kelvin`, `lilie`, `timmy`, `amaryllis`, `damien`, `spectra`, `glacia`, `aragon`,
`pierre_rochard`, et les champions `roxanne`, `bastien`, `voltere`, `adriane`, `norman`, `alizee`,
`levy_et_tatia`, `marc`.

## Classes de dresseurs

`aromatherapeute`, `canon`, `cerveau_et_muscles`, `collectionneur`, `combattante`, `dracologue`, `eleve`,
`eleveur`, `eleveuse`, `entomomaniac`, `expert`, `experte`, `fan_de_camping`, `fan_de_pique_nique`,
`fille_a_la_bouee`, `fillette`, `fillette_de_conte`, `flotteur`, `frere_et_soeur`, `gamin`, `garcon_a_la_bouee`,
`gentleman`, `guitariste`, `jeune_couple`, `journalistes`, `jumelles`, `karateka`, `kinesiste`, `mademoiselle`,
`marin`, `montagnard`, `mystimaniac`, `nageur`, `nageuse`, `ninja_amateur`, `ornithologue`, `pecheur`,
`pecheuse_de_perles`, `plongeur`, `pokefan`, `pokemaniac`, `pokemon_ranger`, `randonneur`, `richard`,
`ruinemaniac`, `saltimbanque`, `sbire_de_la_team_magma`, `scout`, `senior_et_junior`, `soeur_parasol`,
`soeurs_etranges`, `topdresseur`, `topdresseuse`, `topduo`, `terreur`, `triathlete`, `vieux_couple`.

## Format

Portrait d'environ 900 × 1 800 px, personnage au centre. Sans image, la fiche affiche un fond aux couleurs
du type du dresseur.
