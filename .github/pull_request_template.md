<!--
Sous-tâche : [TAG] description (#numéro-issue), vers dev-frontend ou dev-backend selon le label « couche ».
  Exemple : [ADD] écran de l'étape en cours (#42)
Intégration : [MERGE] dev-frontend vers dev  |  [MERGE] dev-backend vers dev
Release (fin de milestone) : [RELEASE] vX.Y.Z, de dev vers main
-->

## Sous-tâche traitée

Closes #

## Ce qui change

-

## Comment tester

1.

## Captures (si l'interface change)

| Avant | Après |
| --- | --- |
|  |  |

## Vérifications

- [ ] La branche suit le format `<type>/<numéro-issue>-<description>` et cible la bonne branche d'intégration (`dev-frontend` ou `dev-backend`)
- [ ] Le titre suit le format `[TAG] description (#numéro-issue)`
- [ ] Les commits suivent la norme de CONTRIBUTING.md
- [ ] Le build et les tests passent en local (`./gradlew build test`)
- [ ] Les critères d'acceptation de la sous-tâche sont remplis
- [ ] La PR est assignée, avec les labels et le milestone de la sous-tâche
