# Contribuer à Omégadex

Ce document fixe les règles du dépôt : branches, commits, pull requests et nommage des fichiers. Les contrôles automatiques de `.github/workflows/pr-conventions.yml` refusent toute pull request qui ne les respecte pas.

## 1. Branches

| Branche | Rôle | Règles |
| --- | --- | --- |
| `main` | Version stable, publiée | Aucun push direct, aucun push forcé, suppression interdite. Reçoit uniquement des pull requests de release venant de `dev`. |
| `dev` | Branche d'intégration | Aucun push direct, aucun push forcé, suppression interdite. Reçoit les pull requests des branches éphémères. |
| Branches éphémères | Une par sous-tâche | Créées depuis `dev`, fusionnées dans `dev` par pull request, **supprimées automatiquement** après la fusion. |

On ne code jamais directement sur `main` ni sur `dev`.

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
| `[MERGE]` | Résolution de conflits lors d'une fusion |
| `[RELEASE]` | Publication d'une version (`dev` vers `main`) |

Règles :

- En français, description en minuscules après le tag, à l'infinitif ou au présent : `[ADD] écran de connexion (#31)`.
- 72 caractères maximum sur la première ligne.
- Un commit = un changement logique. Pas de « divers », « wip » ou « fix ».
- Le numéro de l'issue est obligatoire, sauf pour `[MERGE]` et `[RELEASE]`.
- Si besoin, un corps après une ligne vide explique le **pourquoi**.

## 3. Pull requests

- **Une pull request par sous-tâche**, de la branche éphémère vers `dev`.
- **Titre** : même format qu'un commit, par exemple `[ADD] écran de l'étape en cours (#42)`. Il devient le message du commit de fusion.
- **Corps** : suivre le modèle proposé et écrire `Closes #<numéro>` pour fermer la sous-tâche à la fusion.
- **Assignée** à son auteur, avec les labels et le milestone de la sous-tâche.
- **Vérifications obligatoires** : contrôle des conventions et CI Android au vert.
- **Fusion** dans `dev` : *Squash and merge* uniquement. La branche est ensuite supprimée automatiquement.
- **Release** : une pull request de `dev` vers `main`, titrée `[RELEASE] vX.Y.Z`, fusionnée par *Merge commit*. C'est le seul chemin vers `main`.

## 4. Déroulé type d'une sous-tâche

```bash
git switch dev && git pull
git switch -c feature/42-ecran-etape-en-cours
# … développer, commiter …
git commit -m "[ADD] écran de l'étape en cours (#42)"
git push -u origin feature/42-ecran-etape-en-cours
# ouvrir la pull request vers dev, attendre la CI, squash and merge
git switch dev && git pull && git branch -d feature/42-ecran-etape-en-cours
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
