# Étude : prise en charge de Saphir Alpha (T21, #87)

Omégadex suit aujourd'hui une run de **Rubis Oméga**. Cette étude liste ce qui change pour suivre une run de
**Saphir Alpha**, où ces différences se trouvent dans l'app, et propose un découpage chiffré.

## 1. Différences entre les deux versions

### Histoire et étapes
| Point | Rubis Oméga | Saphir Alpha | Effet dans l'app |
|---|---|---|---|
| Équipe adverse | Team Magma (Max) | Team Aqua (Arthur) | Titre et adversaire de l'étape 8, textes des quêtes principales qui nomment la Team |
| Légendaire de l'histoire | Groudon (Primo-Groudon) | Kyogre (Primo-Kyogre) | Étape 8, quête « Capturer/Vaincre Groudon », défis d'étape qui citent Groudon |
| Repaire de la Team | Planque Magma (Chemin Ardent) | Planque Aqua (Nénucrique) | Lieux débloqués et objets de l'étape 7–8 |
| Conditions « après avoir vaincu Groudon ou Kyogre » | déjà neutres dans la base | identiques | Aucun changement |

Les arènes, la Ligue, l'Épisode Delta et le post-game sont les mêmes : les étapes 0 à 7 et 9 à 12 ne changent pas
(plafonds de niveau compris).

### Rencontres (chiffres de la base actuelle)
- **33 rencontres** de **18 espèces** sont marquées « Saphir Alpha uniquement » : Tadmorv, Lugia, Nénupiot, Lombre,
  Ténéfix, Posipi, Négapi, Tylton, Séviper, Séléroc, Latias, Latios, Sancoki, Dialga, Karaclée, Fulguris, Zekrom,
  Flingouste. Elles existent déjà dans la base : il suffit de ne plus les écarter.
- **48 rencontres** de **25 espèces** sont « Rubis Oméga uniquement » (Ho-Oh, Palkia, Reshiram, Boréas, Mangriff,
  Solaroc, Pifeuil, Mysdibule…). Elles devront être écartées en Saphir Alpha : la règle est le miroir de
  `possibleDansRubisOmega()`.
- Les quêtes annexes « Obtenir X par échange (exclusif à Saphir Alpha) » s'inversent : en Saphir Alpha, ce sont les
  exclusivités de Rubis Oméga qui demandent un échange.
- Latias et Latios échangent leur place (rencontre de l'histoire contre rencontre après la Ligue).

### Défis
- **418 modèles de défis** (sur la banque d'environ 45 000) citent Groudon ou la Team Magma, surtout dans les défis
  d'étape 8 et les défis liés aux légendaires. Ils doivent avoir une variante Kyogre / Team Aqua.
- Les défis « Monotype » et « de type » dépendent des espèces capturables à chaque étape : ils se recalculent
  automatiquement si les rencontres sont filtrées par version.

### Interface et DA
- La DA « Rubis Oméga » (rubis, magma, Ω, Groudon) a besoin d'un pendant « Saphir Alpha » : saphir, eau profonde,
  α, Kyogre. Couleurs du thème, gemmes, animations (étape validée, capture, fin d'aventure sur un décor de lave →
  décor sous-marin), icône et sons d'ambiance.
- Écran de fin d'aventure : sprite de Primo-Kyogre et décor marin.

## 2. Architecture proposée

1. **Version de la run** : nouvelle colonne `version` (`RUBIS_OMEGA` par défaut) dans `run`, migration de la base
   progression, choix à la création de la run, export / import JSON mis à jour.
2. **Filtre de rencontres par version** : `possibleDansRubisOmega()` devient `possibleDans(version)` ; toutes les
   règles qui l'utilisent (Pokédex accessible, rencontres de l'étape, origines des filtres) reçoivent la version.
3. **Données d'étape par version** : soit des étapes et quêtes dupliquées pour l'étape 8 avec une colonne `version`
   dans `etape` et `quete`, soit des textes à variables (« {team} », « {legendaire} ») résolus à l'affichage.
   La seconde option garde une seule base et une seule numérotation de quêtes : c'est celle que je recommande.
4. **Banque de défis** : même principe de variables dans les gabarits ; régénération par `import_banque_defis.py`.
5. **Thème** : `OmegadexTheme(version)` qui choisit la palette et les ressources ; les composants lisent déjà leurs
   couleurs dans le thème.

## 3. Estimation

| Tâche | Couche | Taille |
|---|---|---|
| Version de la run (base, migration, création, sauvegarde) | backend + frontend | M |
| Filtre de rencontres et d'échanges par version | backend | S |
| Variables Team / légendaire dans les étapes, quêtes et défis, import | backend (données) | M |
| Thème Saphir Alpha (palette, gemmes, icône, sons) | frontend | M |
| Animations et écran de fin version Saphir (décor marin, Primo-Kyogre) | frontend | M |
| Recette d'une run Saphir Alpha jusqu'à la Ligue | test | M |

Ordre de grandeur : **5 à 7 tâches de taille S/M**, soit un milestone comparable à M3. Rien ne bloque côté données :
les rencontres exclusives de Saphir Alpha sont déjà importées et marquées.

## 4. Risques
- Garder deux DA cohérentes double le travail de design à chaque nouvel écran.
- Les identifiants de quêtes doivent rester stables entre versions (la progression des runs les référence) :
  c'est pourquoi les variables de texte sont préférables à des quêtes dupliquées.
