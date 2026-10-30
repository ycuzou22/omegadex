# Recette du MVP (T16)

Résultat de la recette du milestone M2 · MVP, du 29 octobre 2026, sur `dev` au commit `8b57061` (données du jeu en version 5).

**Verdict : prêt pour la release v1.0.0 une fois fusionnées les PR #202, #203 et #204.** Elles corrigent les trois défauts trouvés (B1, B2 et B3). Avec ces correctifs, la recette passe sur les trois profils d'émulateur. Restent la vraie run de T16.3 et la validation à l'œil des maquettes.

## Synthèse

| Point de la checklist T16.1 | État | Comment c'est vérifié |
| --- | --- | --- |
| Comptes isolés | ✅ | `SecuriteComptesTest`, `ListerEtChoisirRunTest` ; recette : deux comptes, chacun ne voit que sa progression |
| Déblocage d'étape | ✅ | `CocherQueteUseCaseTest` ; recette : étapes 0 à 10, l'étape suivante est vérifiée cachée après chaque quête |
| Fiche complète | ✅ | `FichePokemonViewModelTest`, `ChaineEvolutionTest` ; recette : les 6 onglets |
| Plafond et difficulté | ✅ | `ReglesDifficulteTest`, `MoteurDefisReglesTest`, `MoteurDefisEmbarqueTest` (couverture de `domain/defi` : 99 %) |
| Statut en 1 touche | ✅ | `PokedexViewModelTest` ; recette : Non vu → Vu → Capturé sur les 3 profils (Android 8 après #204) |
| Export / import | ✅ | `ExportSauvegardeTest`, `ImportSauvegardeTest`, `SauvegardeViewModelTest` ; recette : export avec le sélecteur de fichiers, réimport sans doublon (après #202) |
| Mode avion | ✅ | L'application ne déclare pas la permission `INTERNET` ; recette : redémarrage et les 5 onglets en mode avion |

## Défauts trouvés

| N° | Gravité | Défaut | Où |
| --- | --- | --- | --- |
| B1 (#199) | Bloquant · **corrigé par #202** | L'export et l'import de la sauvegarde (T15) n'avaient pas d'interface. Correctif : carte « Sauvegarde » dans Mes runs, avec export et import par le sélecteur de fichiers Android (`CreateDocument` / `OpenDocument`) et un message pour chaque résultat. | `feature/sauvegarde/` |
| B2 (#200) | Bloquant sur tablette · **corrigé par #203** | Les écrans de connexion, d'inscription et de création de run passaient sous les barres système (`enableEdgeToEdge()` sans marges). Sur tablette, « Commencer l'aventure » était sous la barre des tâches. Correctif : `safeDrawingPadding()` sur ces écrans, et le `Scaffold` consomme ses marges. | `ConnexionScreen`, `InscriptionScreen`, `CreationRunScreen`, `OmegadexApp` |
| B3 (#201) | **Corrigé par #204** | Sous Android 8, toucher la bande des rubis ouvrait la fiche : c'était une zone cliquable placée dans celle de la case. Correctif : le haut de la case et la bande sont deux zones voisines. Vérifié sur l'émulateur Android 8. | `PokedexScreen.kt` : `CaseEspece` |

## Recette sur émulateur (T16.2 et T16.3)

Workflow `Recette` (`.github/workflows/recette.yml`) et script `tools/recette/recette.py`. Chaque vérification produit une ligne OK ou ÉCHEC et une capture d'écran, disponibles dans les artefacts du workflow.

| Profil | Appareil | Résultat |
| --- | --- | --- |
| Pixel | Pixel 6, Android 15 (API 35), 1080×2400 | **50 / 50** (avec la run jusqu'à la Ligue) |
| Android 8.0 | API 26, 1080×1920 | **38 / 38** avec #204 (avant : bloqué par B3) |
| Tablette | Nexus 10, Android 14 (API 34), 2560×1600 | **38 / 38** avec #203 (avant : bloqué par B2) |
| Tests instrumentés | Pixel 6, Android 15 | ✅ (`ChiffrementProgressionTest` : la base progression n'est pas lisible sans la clé) |

Aucun plantage ni ANR d'Omégadex n'a été relevé sur les trois profils. Les résultats « avec » ont été obtenus sur `dev-frontend` augmenté des trois correctifs, le 29 octobre 2026.

### Pour relancer la recette

- **Depuis GitHub** : onglet *Actions* → *Recette* → *Run workflow*.
- **En local**, avec un émulateur démarré et l'APK installé : `python3 tools/recette/recette.py --profil pixel --jusqu-a-la-ligue`.

## Critères d'acceptation des sous-tâches (T06 à T15)

✅ vérifié · ⚠️ partiellement · ❌ non conforme · 👁 à valider à l'œil sur appareil (conformité aux maquettes, fluidité)

| Tâche | Critère d'acceptation | Vérification | Preuve | État |
| --- | --- | --- | --- | --- |
| T06.1 | Connexion impossible avec un mauvais mot de passe | test JVM + émulateur | `CompteUseCasesTest` « connexion impossible avec un mauvais mot de passe » ; recette « mauvais mot de passe refusé » | ✅ |
| T06.2 | Après redémarrage, le dernier joueur reste connecté | test JVM + émulateur | `SessionDataStoreTest` ; recette « le joueur reste connecté après redémarrage » | ✅ |
| T06.3 | Le fichier de base n'est pas lisible sans la clé | test instrumenté | `ChiffrementProgressionTest` (job `tests-instrumentes`) | ✅ |
| T06.4 | Les tests de sécurité passent dans la CI | CI | `SecuriteComptesTest` (5 tests), CI Android | ✅ |
| T07.1 | La nouvelle run s'ouvre sur l'étape 0 | test JVM + émulateur | `CreerRunUseCaseTest` ; recette « la run s'ouvre sur l'étape 0 » | ✅ |
| T07.2 | Changer de run met à jour tout l'affichage | test JVM | `ListerEtChoisirRunTest` « changer de run met à jour la run en cours » | ✅ |
| T08.1 | Conforme à la maquette E · Épuré Oméga | revue visuelle | captures de la recette (Aventure), à valider par toi | 👁 |
| T08.2 | L'état des quêtes survit au redémarrage | émulateur | recette « une quête cochée survit au redémarrage » | ✅ |
| T08.3 | L'étape n+1 n'est jamais visible avant la fin des quêtes principales de n | test JVM + émulateur | `CocherQueteUseCaseTest` ; recette (vérifié à chaque quête, étapes 0 à 10) | ✅ |
| T08.4 | Fiche de lieu accessible depuis l'étape en cours | test JVM | `FicheLieuViewModelTest`, `AventureScreen` (lien « Lieux de l'étape ») | ✅ |
| T09.1 | Défilement fluide sur toute la liste | revue sur appareil | grille `LazyVerticalGrid` à clés stables ; mesure impossible sur émulateur sans GPU | 👁 |
| T09.2 | Les jauges de l'accueil se mettent à jour aussitôt | test JVM + émulateur | `PokedexViewModelTest` « les jauges de l'accueil suivent aussitôt… » ; recette « la jauge Pokédex compte la capture » | ✅ |
| T09.3 | Résultats instantanés | test JVM + émulateur | `FiltresPokedexTest`, `PokedexViewModelTest` ; recette « recherche Gobou » | ✅ |
| T09.4 | Le national est invisible avant la Ligue | test JVM + émulateur | `PokedexNationalTest` ; recette « le Pokédex national s'ouvre après la Ligue » | ✅ |
| T10.1 | Navigation fluide entre onglets | émulateur | recette « fiche de Massko : 6 onglets » + un passage par onglet | ✅ |
| T10.2 | Valeurs identiques à la base jeu | test JVM | `FichePokemonViewModelTest` « les infos viennent de la base jeu… » | ✅ |
| T10.3 | Conditions affichées pour chaque palier | test JVM | `ChaineEvolutionTest` | ✅ |
| T10.4 | Liste conforme à la version Rubis Oméga | import + test JVM | apprentissages du groupe de versions ROSA (PokéAPI 16) ; `FichePokemonViewModelTest` | ✅ |
| T10.5 | Mention « échange requis » si besoin | test JVM | `FichePokemonViewModelTest` « une espèce à échanger est annoncée à part » | ✅ |
| T11.1 | Tests unitaires sur les 3 difficultés | CI | `GenerateurDefisTest`, `MoteurDefisReglesTest`, `MoteurDefisEmbarqueTest` | ✅ |
| T11.2 | Les jauges se mettent à jour à la validation | test JVM | `CycleDefisTest`, `DefisViewModelTest` « relever puis valider… met à jour… la jauge » | ✅ |
| T11.3 | Conforme à la DA | revue visuelle | captures de la recette (Défis), à valider par toi | 👁 |
| T11.4 | Couverture ≥ 70 % du domaine des défis | CI | `verifier_couverture.py` : 99,2 % sur `domain/defi` | ✅ |
| T12.1 | Alerte visuelle si un membre dépasse le plafond | test JVM | `EquipeViewModelTest` « chaque niveau comparé au plafond » | ✅ |
| T12.2 | Impossible d'ajouter une espèce non capturée | test JVM | `AjoutMembreViewModelTest` « seules les captures hors équipe sont proposées » | ✅ |
| T12.3 | Tests unitaires de la détection | CI | `DetectionEvolutionTest` (5 tests) | ✅ |
| T13.1 | Mise à jour immédiate après une action | test JVM + émulateur | `ObserverJaugesUseCaseTest` ; recette jauge Pokédex | ✅ |
| T13.2 | Conforme à la maquette | revue visuelle | captures de la recette (Accueil), à valider par toi | 👁 |
| T14.1–T14.3, T14.5, T14.6 | Conforme à la maquette | revue visuelle | animations à juger sur appareil | 👁 |
| T14.4, T14.5 | Testé avec « Supprimer les animations » | test JVM + émulateur | `AnimationsReduitesTest` ; la recette tourne animations coupées | ✅ |
| T15.1 | Le fichier ne contient aucun mot de passe | test JVM | `ExportSauvegardeTest` « … sans aucun mot de passe » | ✅ |
| T15.1 | Export via le sélecteur de fichiers Android (travail à réaliser) | émulateur | recette « export dans un fichier JSON » (après #202) | ✅ |
| T15.2 | Un fichier invalide est refusé proprement | test JVM | `ImportSauvegardeTest`, `SauvegardeViewModelTest` « un fichier invalide est refusé avec sa raison » | ✅ |

## Reste à faire avant la release v1.0.0

1. Fusionner #202, #203 et #204 dans `dev-frontend`, puis intégrer dans `dev`.
2. Valider à l'œil les points 👁 : maquettes, animations, fluidité de la grille.
3. T16.3 : jouer une vraie run avec l'application jusqu'à la Ligue et ouvrir une issue `type: bug` par problème trouvé.
4. Relancer le workflow Recette sur `dev` : les trois profils doivent rester au vert.

Petit défaut relevé en passant, sans gravité : « 1 étapes validées sur 13 » dans Mes runs (pluriel de `mes_runs_progression`).
