# Contribuer à Omégadex

Ce document fixe les règles du dépôt : branches, commits, pull requests et nommage des fichiers. Les contrôles automatiques de `.github/workflows/pr-conventions.yml` refusent toute pull request qui ne les respecte pas.

## 1. Branches

```
main ◄── dev ◄── dev-frontend ◄── feature/…, fix/…  (écrans, thème, composants, animations)
              ◄── dev-backend  ◄── feature/…, fix/…  (données, domaine, import, sécurité, CI)
```

| Branche | Rôle | Règles |
| --- | --- | --- |
| `main` | Version stable | Aucun push direct, aucun push forcé, suppression interdite. Reçoit **une pull request de release depuis `dev` à chaque milestone entièrement validé**. |
| `dev` | Branche par défaut, intégration générale | Aucun push direct, aucun push forcé, suppression interdite. Reçoit uniquement les pull requests de `dev-frontend` et `dev-backend`. |
| `dev-frontend` | Intégration de l'interface | Aucun push direct, aucun push forcé, suppression interdite. Reçoit les branches éphémères des sous-tâches `couche: frontend`. |
| `dev-backend` | Intégration des données et de la logique | Aucun push direct, aucun push forcé, suppression interdite. Reçoit les branches éphémères des sous-tâches `couche: backend`. |
| Branches éphémères | Une par sous-tâche | Créées depuis `dev-frontend` ou `dev-backend` selon le label `couche` de la sous-tâche, fusionnées par pull request, **supprimées automatiquement** après la fusion. |

On ne code jamais directement sur `main`, `dev`, `dev-frontend` ni `dev-backend`.

| Couche | Contenu | Paquets concernés |
| --- | --- | --- |
| `couche: frontend` | Écrans Compose, navigation, thème, composants, animations | `ui/`, `feature/*/` (écrans et ViewModels) |
| `couche: backend` | Room, DAO, repositories, cas d'usage, moteur de défis, import des données, sécurité, CI | `data/`, `domain/`, `di/`, `tools/import/`, `.github/` |

### Nommage des branches éphémères

```
<type>/<numéro-issue>-<description-courte>
```

- `type` : `feature`, `fix`, `docs`, `test`, `refactor`, `chore` ou `ci`
- `numéro-issue` : numéro de la sous-tâche GitHub traitée
- `description-courte` : 2 à 6 mots en minuscules, sans accents, séparés par des tirets

| Type | Quand l'utiliser | Exemple |
| --- | --- | --- |
| `feature` | Nouvelle fonctionnalité ou écran | `feature/42-ecran-etape-en-cours` |
| `fix` | Correction d'un bug | `fix/87-plafond-mal-calcule` |
| `docs` | Documentation uniquement | `docs/12-guide-emulateur` |
| `test` | Ajout ou correction de tests | `test/55-tests-moteur-defis` |
| `refactor` | Réorganisation sans changement de comportement | `refactor/60-extraire-gem-checkbox` |
| `chore` | Outillage, dépendances, configuration | `chore/9-ajouter-detekt` |
| `ci` | Workflows GitHub Actions | `ci/10-workflow-android` |

Expression vérifiée par la CI : `^(feature|fix|docs|test|refactor|chore|ci)/[0-9]+-[a-z0-9]+(-[a-z0-9]+)*$`

## 2. Commits

```
[TAG] description courte (#numéro-issue)
```

| Tag | Usage |
| --- | --- |
| `[ADD]` | Ajout d'une fonctionnalité, d'un écran, d'un fichier |
| `[UPDATE]` | Modification d'une fonctionnalité existante |
| `[FIX]` | Correction de bug |
| `[DEL]` | Suppression de code ou de fichiers |
| `[REFACTO]` | Réorganisation du code sans changement de comportement |
| `[STYLE]` | Formatage, mise en forme, sans changement de logique |
| `[TEST]` | Ajout ou modification de tests |
| `[DOCS]` | Documentation |
| `[CONFIG]` | Configuration, dépendances, Gradle, outillage |
| `[CI]` | Workflows GitHub Actions |
| `[MERGE]` | Fusion d'une branche d'intégration dans `dev`, ou résolution de conflits |
| `[RELEASE]` | Publication d'une version (`dev` vers `main`) à la fin d'un milestone |

Règles :

- En français, description en minuscules après le tag, à l'infinitif ou au présent : `[ADD] écran de connexion (#31)`.
- 72 caractères maximum sur la première ligne.
- Un commit = un changement logique. Pas de « divers », « wip » ou « fix ».
- Le numéro de l'issue est obligatoire, sauf pour `[MERGE]` et `[RELEASE]`.
- Si besoin, un corps après une ligne vide explique le **pourquoi**.

## 3. Pull requests

Trois niveaux de pull request, chacun contrôlé par `pr-conventions.yml` :

| PR | Source → cible | Titre | Fusion |
| --- | --- | --- | --- |
| Sous-tâche | branche éphémère → `dev-frontend` ou `dev-backend` | `[TAG] description (#numéro)` | *Squash and merge*, branche supprimée automatiquement |
| Intégration | `dev-frontend` ou `dev-backend` → `dev` | `[MERGE] dev-frontend vers dev` ou `[MERGE] dev-backend vers dev` | *Merge commit* |
| Release | `dev` → `main` | `[RELEASE] vX.Y.Z` | *Merge commit*, puis tag `vX.Y.Z` |

- **Une pull request par sous-tâche.** Corps : modèle proposé avec `Closes #<numéro>` ; le workflow `close-issues.yml` ferme la sous-tâche dès la fusion dans `dev-frontend` ou `dev-backend`.
- **Assignée** à son auteur, avec les labels et le milestone de la sous-tâche.
- **Vérifications obligatoires** : contrôle des conventions et CI Android au vert.
- **Intégration** : dès qu'une tâche parente est terminée, sa branche d'intégration est fusionnée dans `dev`.
- **Release à chaque milestone** : quand **toutes** les tâches d'un milestone sont fermées et intégrées dans `dev`, l'issue de release du milestone est traitée : pull request `dev` → `main`, fusion, tag et fermeture du milestone. C'est le seul chemin vers `main`.

| Milestone | Version publiée sur `main` |
| --- | --- |
| M0 · Initialisation | `v0.1.0` |
| M1 · Données du jeu | `v0.2.0` |
| M2 · MVP | `v1.0.0` |
| M3 · V2 | `v1.1.0` |
| M4 · V3 (option) | `v2.0.0` |

## 4. Déroulé type d'une sous-tâche

Exemple pour une sous-tâche `couche: frontend` (remplacer par `dev-backend` pour le backend) :

```bash
git switch dev-frontend && git pull
git switch -c feature/42-ecran-etape-en-cours
# … développer, commiter …
git commit -m "[ADD] écran de l'étape en cours (#42)"
git push -u origin feature/42-ecran-etape-en-cours
# ouvrir la pull request vers dev-frontend, attendre la CI, squash and merge
git switch dev-frontend && git pull && git branch -d feature/42-ecran-etape-en-cours
```

Fin de milestone :

```bash
# 1. intégrer les deux branches dans dev (pull requests [MERGE] … vers dev)
# 2. ouvrir la pull request dev → main titrée [RELEASE] vX.Y.Z et la fusionner
git switch main && git pull
git tag -a vX.Y.Z -m "[RELEASE] vX.Y.Z" && git push origin vX.Y.Z
# 3. remettre dev-frontend et dev-backend à jour depuis dev (pull request [MERGE] dev vers …)
```

## 5. Nommage des fichiers et dossiers

Les noms métier sont en français sans accents (`Etape`, `Espece`, `Quete`), les suffixes techniques en anglais (`Screen`, `ViewModel`, `Dao`).

| Élément | Convention | Exemples |
| --- | --- | --- |
| Paquets Kotlin | minuscules, un mot par niveau | `com.omegadex.app.feature.pokedex` |
| Dossiers de code | minuscules | `ui/`, `domain/`, `data/`, `di/`, `feature/aventure/` |
| Fichiers Kotlin | PascalCase, nom de la classe principale | `PokedexScreen.kt`, `EtapeRepository.kt` |
| Écrans Compose | suffixe `Screen` | `FichePokemonScreen.kt` |
| États d'écran | suffixe `UiState` | `AccueilUiState.kt` |
| ViewModels | suffixe `ViewModel` | `DefisViewModel.kt` |
| Cas d'usage | verbe + `UseCase` | `DeverrouillerEtapeUseCase.kt` |
| Repositories | `Repository` (interface), `RepositoryImpl` | `EquipeRepository.kt`, `EquipeRepositoryImpl.kt` |
| Room | suffixes `Entity` et `Dao` | `EspeceEntity.kt`, `EspeceDao.kt` |
| Modules Hilt | suffixe `Module` | `DatabaseModule.kt` |
| Composables | PascalCase | `GemCheckbox`, `OmegaButton` |
| Fonctions et variables | camelCase | `plafondNiveau`, `deverrouillerEtape()` |
| Constantes | UPPER_SNAKE_CASE | `NIVEAU_MAX` |
| Ressources Android | snake_case avec préfixe | `ic_omega.xml`, `img_badge_dynamo.webp`, `anim_evolution.xml` |
| Chaînes de caractères | snake_case par écran | `pokedex_titre`, `aventure_quete_principale` |
| Tests | miroir du paquet, suffixe `Test` | `DeverrouillerEtapeUseCaseTest.kt` |
| Scripts d'import | snake_case dans `tools/import/` | `import_especes.py` |
| Workflows | kebab-case | `android-ci.yml` |
| Documentation | kebab-case dans `docs/` | `docs/roadmap.md` |

Arborescence cible :

```
app/src/main/java/com/omegadex/app/
├── core/          # utilitaires partagés
├── data/          # Room, DAO, repositories, import
├── di/            # modules Hilt
├── domain/        # modèles métier et cas d'usage
├── feature/
│   ├── accueil/
│   ├── aventure/
│   ├── comptes/
│   ├── defis/
│   ├── equipe/
│   └── pokedex/
└── ui/
    ├── components/  # OmegaButton, GemCheckbox…
    └── theme/       # couleurs, typographies, formes
```

## 6. Tâches GitHub

- Les tâches parentes portent le label `type: tâche`, les sous-tâches `type: sous-tâche`, et sont liées entre elles.
- L'ordre d'exécution est donné par le numéro `T01`, `T02`… et détaillé dans `docs/roadmap.md` et dans chaque issue.
- On traite une sous-tâche à la fois, dans l'ordre indiqué, en commençant par la priorité la plus haute (`P0`).
