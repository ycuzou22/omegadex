# Outils d'import des données du jeu

La base jeu `app/src/main/assets/database/omegadex_jeu.db` est générée par ces scripts, jamais modifiée à la main.

## Importer toutes les données

```bash
python3 tools/import/importer.py --version-donnees N
```

La commande crée la base vide, importe espèces, attaques, lieux, rencontres, étapes et quêtes, applique les corrections manuelles (`corrections.json`), puis écrit le rapport de contrôle `rapport_controle.md`. Une erreur bloquante interrompt l'import.

## Corrections manuelles

En dernier recours seulement, quand aucune source ne couvre une donnée : ajouter dans `corrections.json` une entrée `{"sql": …, "justification": …, "source": …}`. Une correction sans justification ni source est refusée.

## Créer la base vide

```bash
python3 tools/import/creer_base_jeu.py --version-donnees 1 --sources "…"
```

Le script lit le dernier schéma exporté par Room (`app/schemas/com.omegadex.app.data.jeu.JeuDatabase/<n>.json`) et crée exactement les mêmes tables et index, avec `PRAGMA user_version` égal à la version du schéma. Il faut donc compiler l'application après toute modification des entités de la base jeu.

## Publier de nouvelles données

1. Si une version de l'app contenant la base a déjà été publiée, augmenter `JeuDatabase.VERSION` (même si seul le contenu change) : à l'ouverture, Room recopiera la base embarquée. Compiler pour exporter le schéma de la nouvelle version.
2. Lancer `python3 tools/import/importer.py --version-donnees N` avec N augmenté de 1 : la base est écrite directement dans `app/src/main/assets/database/omegadex_jeu.db`.
3. Vérifier `rapport_controle.md` (aucune erreur bloquante) et commiter la base, le rapport et le schéma.

Version actuelle des données : **8** (schéma Room v6, 60 modèles de défis, banque de 45 480 défis pour 229 familles, quêtes d'échange pour les exclusivités Saphir Alpha, 248 quêtes d'objets).

## Banque de défis par famille

`import_banque_defis.py` génère environ 200 défis pour chaque famille d'évolution capturable dans Rubis Oméga (Pokédex national compris), à partir des données déjà importées. Il tourne à la fin de `importer.py`, après les corrections, et peut se relancer seul sur la base existante : `python3 tools/import/import_banque_defis.py`. Le format est décrit dans `docs/modeles-defis.md` (section « Banque de défis par famille »).

## Quêtes d'objets

Les objets utiles (`donnees/objets_rosa.json`) deviennent des quêtes annexes « Récupérer / Acheter / Recevoir / Gagner X (lieu) », placées à l'étape où l'objet est accessible : lieu débloqué, capacité de terrain utilisable (`CS_ETAPE`) et condition d'histoire (`CONDITIONS_OBJET`). Les récompenses déjà listées dans une quête principale sont ignorées. Exceptions fixées dans les données : `etape` impose l'étape (Méga-Bracelet à l'étape 6), `lieuLibre` nomme un lieu absent de la base (objets des lieux mirages, en post-game). Pour rejouer seulement ces quêtes sur la base embarquée, sans réseau :

```bash
python3 tools/import/import_etapes.py --objets
```

## Attention aux schémas exportés

Room ne réécrit pas un fichier de schéma déjà présent pour la même version. Avant la première release d'une version, si une entité change sans changement de version, supprimer le fichier `<n>.json` concerné et recompiler pour le régénérer. Après une release, toute modification passe par une nouvelle version et une migration (base progression) ou une recopie (base jeu).
