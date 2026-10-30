# Format des modèles de défis

Les modèles de défis sont la seule donnée saisie à la main. Ils sont écrits dans `tools/import/donnees/defis.json`, validés par `tools/import/import_defis.py` à chaque import, puis copiés dans la table `modele_defi` de la base jeu. Le moteur de défis (T11) les instancie pour chaque run.

## Structure d'un modèle

```json
{
  "id": 101,
  "type": "ETAPE",
  "etape": 1,
  "titre": "Sans le starter",
  "gabarit": "Battre {adversaire} sans utiliser {starter}",
  "parametres": {},
  "difficulteMin": "NORMAL",
  "deblocage": { "etapeMin": 1, "conditions": [] }
}
```

| Champ | Type | Rôle |
| --- | --- | --- |
| `id` | entier unique | Identifiant stable (repris par les défis enregistrés des joueurs, ne jamais le réutiliser) |
| `type` | texte | `ETAPE`, `ESPECE`, `EVOLUTION`, `ROTATION`, `MONOTYPE`, `NAVI_DEX`, `MEGA` ou `CONCOURS` |
| `etape` | entier ou `null` | Étape (0 à 12) d'un défi `ETAPE` ; `null` pour les autres types |
| `titre` | texte | Libellé court affiché dans les listes |
| `gabarit` | texte | Énoncé du défi, avec des marqueurs entre accolades |
| `parametres` | objet | Valeurs des marqueurs propres au modèle (`type`, `nombre`…) |
| `difficulteMin` | texte | `NORMAL` (défi de base), `DIFFICILE` ou `HARDCORE` : difficulté de run à partir de laquelle le défi est proposé |
| `deblocage.etapeMin` | entier | Première étape où le défi peut être proposé |

Un défi `MONOTYPE` n'est proposé qu'à partir de l'étape où **deux familles** de son type s'obtiennent, starters exclus (`tools/import/monotypes.py`) : ni Monotype Feu, Glace, Spectre, Dragon ou Électrik contre Bastien. `test_monotypes.py` vérifie que `defis.json` suit ce calcul.
| `deblocage.conditions` | liste | Conditions supplémentaires vérifiées par le moteur (voir ci-dessous) |

## Marqueurs

Remplis par le moteur : `{adversaire}`, `{plafond}`, `{lieu}`, `{etape}`, `{starter}`, `{espece}`, `{evolution}`, `{niveau}`.
Tout autre marqueur doit avoir sa valeur dans `parametres` (par exemple `{type}` avec `"parametres": {"type": "eau"}` ; plusieurs types séparés par des virgules). Les types utilisent les clés de l'app : `eau`, `feu`, `electrik`, `fee`…

## Conditions de déblocage

| Condition | Le défi n'est proposé que si… |
| --- | --- |
| `ESPECE_CAPTUREE` | l'espèce ciblée a été capturée |
| `ESPECE_DANS_EQUIPE` | l'espèce ciblée est dans l'équipe actuelle |
| `EVOLUTION_SOUS_PLAFOND` | l'évolution de l'espèce est atteignable sans dépasser le plafond de l'étape |
| `MEGA_DISPONIBLE` | l'espèce a une Méga-Évolution disponible |
| `TALENT_CACHE` | l'espèce a un talent caché trouvable au Navi-Dex |
| `CONCOURS_OUVERTS` | les Concours Live sont accessibles |
| `POKEDEX_NATIONAL` | le Pokédex national est obtenu |

## Validation

Un modèle invalide arrête l'import avec un message qui cite le modèle et le champ en cause, par exemple :

```
defis.json, modèle n° 3 (id 7) : marqueur(s) sans valeur dans « parametres » : {type}
```

Contrôles : champs obligatoires et types, champs inconnus, type de défi, étape, difficulté, marqueurs, types Pokémon, conditions, identifiants uniques, JSON lisible. Tests : `python3 -m unittest discover tools/import/tests`.

## Banque de défis par famille

En plus des 60 modèles saisis à la main, la table `modele_defi` contient une **banque d'environ 200 défis par famille d'évolution** (Arcko, Massko et Jungko forment une famille). Elle n'est pas saisie : `tools/import/import_banque_defis.py` la génère depuis les données du jeu, et la même base donne toujours la même banque.

| Point | Règle |
| --- | --- |
| Familles | Les 229 familles dont au moins un membre s'obtient dans Rubis Oméga sans échange (Pokédex national compris). Les autres membres comptent s'ils s'obtiennent par évolution (hors évolution par échange) ou par reproduction. |
| Identifiants | `100000 + racine × 1000 + n` : la **racine** est la plus petite espèce de la famille qui n'est l'évolution d'aucune autre (Arcko, 252 → défis 352001 à 352200). |
| Cible | Chaque défi vise un membre (`parametres.cible`) ; son nom est déjà écrit dans l'énoncé. Seuls `{adversaire}` et `{niveau}` restent à remplir par le moteur. |
| Déblocage | `etapeMin` vaut au moins l'étape où la cible s'obtient (même règle que la disponibilité des rencontres dans l'app), puis l'étape où le niveau cité passe sous le plafond, où la CT, la pierre ou la Méga-Gemme est accessible, où la Pension (étape 3), les Concours Live (étape 3) ou l'Atoll de Combat (étape 12) ouvrent. Un défi qui vise des Pokémon d'un type (« Chasse au type Glace », « Capture Spectre », « Face au type Dragon », « Eau contre Feu ») attend en plus qu'on puisse croiser ce type à l'état sauvage. |
| Catégories | `parametres.categorie` : combat, attaque (niveau, CT, Donneur de capacités), type, talent, évolution, statistique, lieu, capture, élevage, concours, méga. Le `type` du modèle reste un type connu de l'app (`ESPECE`, `EVOLUTION`, `NAVI_DEX`, `MEGA`, `CONCOURS`). |
| Difficulté | Environ 54 % Normal, 30 % Difficile, 15 % Hardcore. |

Quelques familles ont moins de 200 défis parce que leurs espèces ont peu d'attaques ou de talents : Métamorph et Zarbi (113), et une douzaine de légendaires ou d'espèces sans évolution entre 177 et 198 (Raikou, Entei, Suicune, Cresselia, Kyogre, Heatran, Coquiperl…).

### Tirage au sort dans l'app

L'app lit les modèles saisis à la main par `ModeleDefiDao.modeles()` (identifiants sous 100000) et la banque famille par famille (`ModeleDefiDao.banque(racine)`). Dans l'onglet Défis (`ProposerDefisUseCase`, `BanqueDefis`) :

- chaque Pokémon **de l'équipe** reçoit **un seul défi tiré au sort** dans la banque de sa famille, parmi les défis qui le visent, débloqués à l'étape en cours, de la difficulté de la run et jamais relevés dans cette run ;
- le tirage est stable : il dépend de la run, du Pokémon, de l'étape et du nombre de défis que ce Pokémon a déjà terminés. Il change donc à chaque nouvelle étape et après chaque défi réussi ou raté ;
- tant qu'un Pokémon relève un défi espèce, il n'en reçoit pas d'autre ; un défi raté se reprend avec « Retenter » ;
- les Pokémon capturés hors de l'équipe, et ceux dont la famille n'a pas de banque, gardent les modèles espèce saisis à la main ; les défis d'étape ne changent pas.
