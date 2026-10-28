# Sources des données du jeu

Les données de Rubis Oméga embarquées dans l'application sont importées par les scripts de `tools/import/`. Aucun sprite ni logo officiel n'est utilisé.

| Source | Données | Accès | Licence |
| --- | --- | --- | --- |
| [PokéAPI](https://github.com/PokeAPI/pokeapi) | Espèces, noms français, types, statistiques, talents, évolutions, attaques et apprentissages | Fichiers CSV du dépôt (`data/v2/csv`), commit figé dans `source_pokeapi.py` | Code sous licence BSD-3-Clause ; données issues du projet [veekun/pokedex](https://github.com/veekun/pokedex) |

Les valeurs sont celles de la 6e génération : quand une donnée a changé après Rubis Oméga (statistiques, types, talents, puissance d'une attaque…), l'import reprend la valeur d'origine grâce aux tables `*_past` et `move_changelog` de PokéAPI.

Pokémon et les noms associés sont des marques de Nintendo, Creatures Inc., GAME FREAK inc. et The Pokémon Company. Projet personnel et non commercial.
