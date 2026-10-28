# Règles de difficulté et de plafond

Le **plafond** d'une étape est le niveau du Pokémon le plus fort de son champion (importé avec la base jeu, voir `tools/import/rapport_controle.md`). Ces règles sont implémentées dans `domain/defi/ReglesDifficulte.kt` et testées par `ReglesDifficulteTest` ; le moteur de défis (T11) les applique.

## Difficultés

| Difficulté | Défis proposés | Règle de niveau |
| --- | --- | --- |
| Normal | modèles `NORMAL` | aucun Pokémon au-dessus du plafond |
| Difficile | modèles `NORMAL` et `DIFFICILE` | aucun Pokémon au-dessus du plafond |
| Hardcore | modèles `NORMAL`, `DIFFICILE` et `HARDCORE` + règle Hardcore | tous les Pokémon disponibles avant l'arène exactement au niveau du plafond |

## Règle générale du plafond

- Aucun défi ne demande de dépasser le plafond : un modèle peut viser un niveau **sous** le plafond (« plafond − 5 », paramètre `ecart` ≥ 0), jamais au-dessus.
- Pendant une étape, l'app signale les Pokémon de l'équipe qui dépassent le plafond.
- Un défi d'**évolution** n'est proposé que si l'évolution est atteignable sous le plafond : une évolution par niveau exige un niveau ≤ plafond ; les évolutions sans niveau (pierre, échange, bonheur, objet, lieu) sont toujours atteignables.

## Règle Hardcore

Avant le combat contre le champion de l'étape, **tous les Pokémon disponibles** (équipe et réserve utilisée pendant l'étape) doivent être montés **exactement** au niveau du plafond, pas au-delà. L'app indique combien de niveaux il reste à gagner.

## Contrôle des données

`tools/import/import_defis.py` refuse un modèle de défi dont le paramètre `ecart` est négatif.
