# Sources des données du jeu

Les données de Rubis Oméga embarquées dans l'application sont importées par les scripts de `tools/import/`. Aucun sprite ni logo officiel n'est utilisé.

| Source | Données | Accès | Licence |
| --- | --- | --- | --- |
| [Poképédia](https://www.pokepedia.fr) | Équipes des champions et des adversaires de chaque étape (modèle « Tableau Dresseur »), d'où le plafond de niveau ; lieux de Rubis Oméga (zones annexes et lieux mirages compris), rencontres : méthode, niveaux, taux, conditions, exclusivités de version | Module « Localisations/Données/Pokémon Rubis Oméga et Saphir Alpha » lu par l'API MediaWiki (`source_pokepedia.py`), mis en cache | [CC BY-NC-SA 3.0](https://creativecommons.org/licenses/by-nc-sa/3.0/deed.fr) : seuls des faits en sont extraits |
| Serebii, PokémonDB, SegmentNext, Pokébip, Bulbapedia | Objets utiles de Rubis Oméga (CT, CS, objets clés, objets d'évolution, objets tenus, Méga-Gemmes) : lieu, mode d'obtention, CS ou condition d'histoire nécessaire (`donnees/objets_rosa.json`) | Pages consultées et croisées à la main, faits seulement (nom, lieu, condition), notes rédigées pour le projet ; entrées incertaines marquées `"fiable": false` | Faits non protégés ; aucun texte repris |
| [PokéAPI](https://github.com/PokeAPI/pokeapi) | Espèces, noms français, types, statistiques, talents, évolutions, attaques et apprentissages | Fichiers CSV du dépôt (`data/v2/csv`), commit figé dans `source_pokeapi.py` | Code sous licence BSD-3-Clause ; données issues du projet [veekun/pokedex](https://github.com/veekun/pokedex) |

Les 13 étapes et leurs quêtes principales sont décrites dans `tools/import/donnees/etapes_rosa.json` d'après la progression du jeu ; les captures ne sont plus des quêtes annexes : elles se font avec le bouton « Attraper » de la liste des rencontres, qui montre chaque espèce à l'étape où elle devient capturable (méthode, canne ou CS disponible, conditions comme « après avoir vaincu Groudon »).

L'étape de déblocage de chaque lieu est fixée dans `tools/import/donnees/lieux_rosa.json` d'après la progression du jeu. Une espèce introuvable dans Hoenn et hors de la famille d'évolution d'une espèce trouvable est rattachée au lieu « Hors de Hoenn » avec la mention « échange requis ».

Les apprentissages d'attaques sont ceux du groupe de versions Rubis Oméga / Saphir Alpha (`version_group_id = 16`), CT / CS numérotées comme dans ce jeu.

Les valeurs sont celles de la 6e génération : quand une donnée a changé après Rubis Oméga (statistiques, types, talents, puissance d'une attaque…), l'import reprend la valeur d'origine grâce aux tables `*_past` et `move_changelog` de PokéAPI.

Pokémon et les noms associés sont des marques de Nintendo, Creatures Inc., GAME FREAK inc. et The Pokémon Company. Projet personnel et non commercial.
