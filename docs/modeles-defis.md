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
