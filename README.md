# Omégadex

[![Android CI](https://github.com/ycuzou22/omegadex/actions/workflows/android-ci.yml/badge.svg?branch=dev)](https://github.com/ycuzou22/omegadex/actions/workflows/android-ci.yml)

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

L'application se teste gratuitement sur PC avec l'émulateur d'Android Studio, sans téléphone.

### 1. Prérequis

| Élément | Détail |
| --- | --- |
| Android Studio | Dernière version stable (elle fournit le JDK utilisé par Gradle, version 17 ou plus) |
| SDK Android | Depuis *Settings › Languages & Frameworks › Android SDK* : la dernière plateforme (API 37), **Android Emulator** et **Android SDK Platform-Tools** |
| Processeur | Virtualisation activée dans le BIOS / UEFI (Intel VT-x ou AMD-V / SVM) |
| Windows | Fonctionnalité Windows **Plateforme de l'hyperviseur Windows** (*Windows Hypervisor Platform*) activée, puis redémarrage |
| macOS | Rien à faire, l'accélération est intégrée |
| Linux | KVM disponible (`/dev/kvm`) et utilisateur dans le groupe `kvm` |
| Mémoire | 16 Go de RAM conseillés (8 Go minimum) |

Sous Windows, la fonctionnalité s'active dans *Panneau de configuration › Programmes › Activer ou désactiver des fonctionnalités Windows*, ou dans un PowerShell administrateur :

```powershell
Enable-WindowsOptionalFeature -Online -FeatureName HypervisorPlatform
```

Pour vérifier l'accélération : *Tools › SDK Manager › SDK Tools* doit montrer l'émulateur installé, et `emulator -accel-check` (dans `<SDK>/emulator`) doit répondre `accel: 0` suivi du nom de l'hyperviseur.

### 2. Récupérer et ouvrir le projet

```bash
git clone https://github.com/ycuzou22/omegadex.git
```

Dans Android Studio : *File › Open*, choisir le dossier `omegadex`, puis attendre la fin de la synchronisation Gradle (barre d'état en bas). Le fichier `local.properties` est créé automatiquement et ne doit pas être commité.

### 3. Créer les émulateurs

Dans *Tools › Device Manager › Create Virtual Device*, créer ces trois profils :

| Profil | Appareil | Image système | Usage |
| --- | --- | --- | --- |
| Principal | Pixel récent (Pixel 9) | Dernière API (37), *Google APIs*, x86_64 | Développement au quotidien |
| Minimum | Pixel 4 | Android 8.0 (API 26), *Google APIs*, x86_64 | Vérifier le `minSdk` |
| Tablette | Pixel Tablet | Dernière API (37), *Google APIs*, x86_64 | Vérifier les grands écrans |

Sur un PC à processeur ARM (Snapdragon), prendre les images `arm64-v8a` à la place de `x86_64`.

Les mêmes profils peuvent être créés en ligne de commande (outils dans `<SDK>/cmdline-tools/latest/bin`). Exemple pour le profil minimum :

```bash
sdkmanager "system-images;android-26;google_apis;x86_64"
avdmanager create avd -n Omegadex_API26 -k "system-images;android-26;google_apis;x86_64" -d pixel_4
```

Pour les deux autres, reprendre le nom exact de la dernière image donné par `sdkmanager --list | grep system-images`, avec `-d pixel_9` et `-d pixel_tablet`.

### 4. Lancer l'application

- Dans Android Studio : choisir l'émulateur dans la barre d'outils, puis **Run ▶** (`Maj+F10`). L'émulateur démarre et l'application s'ouvre sur l'onglet Accueil.
- En ligne de commande, avec un émulateur déjà démarré :

```bash
./gradlew installDebug      # Windows : gradlew.bat installDebug
adb shell am start -n com.omegadex.app/.MainActivity
```

### 5. Aperçus Compose

Chaque écran a une fonction `@Preview` (par exemple `AccueilPreview` dans `feature/accueil/AccueilScreen.kt`). Ouvrir le fichier, passer en vue **Split** ou **Design** en haut à droite de l'éditeur : l'aperçu s'affiche sans lancer l'émulateur. Le mode interactif (icône ▶ sur l'aperçu) permet de cliquer dedans, et *Run Preview* l'installe seul sur l'émulateur.

### 6. Vérifier avant une pull request

```bash
./gradlew ktlintFormat
./gradlew build lint ktlintCheck detekt test
```

Ce sont les mêmes commandes que la CI (voir [CONTRIBUTING.md](CONTRIBUTING.md#6-qualité-du-code)).

## Organisation

- Branches : `main` (stable, une release par milestone) ← `dev` (branche par défaut) ← `dev-frontend` / `dev-backend` ← branches éphémères par sous-tâche.
- Conventions de branches, commits, pull requests et nommage : [CONTRIBUTING.md](CONTRIBUTING.md).
- Ordre d'exécution des tâches : [docs/roadmap.md](docs/roadmap.md).

## Avertissement

Projet personnel et non commercial, sans lien avec Nintendo, Creatures Inc., GAME FREAK inc. ni The Pokémon Company. Pokémon et les noms associés sont des marques de leurs détenteurs respectifs. Aucun sprite ni logo officiel n'est distribué dans ce dépôt.
