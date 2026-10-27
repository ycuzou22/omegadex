# Omégadex

Application Android hors-ligne qui accompagne une partie de **Pokémon Rubis Oméga** étape par étape : suivi de l'aventure badge par badge, Pokédex interactif lié à la progression et défis par Pokémon selon la difficulté choisie, sans jamais dépasser le niveau du champion d'arène.

## Fonctionnalités prévues (MVP)

- **Comptes locaux** protégés par mot de passe, plusieurs joueurs sur un même téléphone.
- **Aventure** : seule l'étape en cours est visible, avec ses quêtes principales et annexes ; l'étape suivante se débloque quand les quêtes principales sont terminées.
- **Pokédex interactif** relié à l'avancement, Pokédex national après la Ligue.
- **Fiche Pokémon** complète, inspirée de Datadex : infos, stats, évolution, attaques, lieux, défis.
- **Défis** en trois difficultés (Normal, Difficile, Hardcore) avec un plafond de niveau par étape.
- **Équipe**, **tableau de bord**, **sauvegarde** JSON et animations (étape validée, capture, évolution).

## Direction artistique

« Épuré Oméga » : base moderne et épurée, rouge rubis, veines de magma inspirées de Groudon et signe Ω.

## Stack technique

Kotlin · Jetpack Compose · MVVM · Room (base jeu en lecture seule + base progression chiffrée) · Hilt · Coroutines / Flow · GitHub Actions.

Android 8.0 (API 26) minimum. 100 % hors-ligne.

## Lancer le projet

Le projet Android sera initialisé par la tâche T01. Le guide de lancement sur émulateur (Android Studio, profils AVD, aperçus Compose) sera ajouté ici à la sous-tâche T01.5.

## Organisation

- Branches : `main` (stable) ← `dev` (intégration) ← branches éphémères par sous-tâche.
- Conventions de branches, commits, pull requests et nommage : [CONTRIBUTING.md](CONTRIBUTING.md).
- Ordre d'exécution des tâches : [docs/roadmap.md](docs/roadmap.md).

## Avertissement

Projet personnel et non commercial, sans lien avec Nintendo, Creatures Inc., GAME FREAK inc. ni The Pokémon Company. Pokémon et les noms associés sont des marques de leurs détenteurs respectifs. Aucun sprite ni logo officiel n'est distribué dans ce dépôt.
