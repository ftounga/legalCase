# Mini-spec — F-DT-08 / SF-DT-08-01 Référentiel critères et analyseur de licenciement

## Identifiant
`F-DT-08 / SF-DT-08-01`

## Branche Git
`feat/SF-DT-08-01-analyseur-licenciement`

## Objectif
Référentiel statique des critères de validité du licenciement (FR + BE). Analyseur : réponses par critère → score de risque (0-100), verdict. Entity 1:1, migration.

## Critères d'acceptation
- Min 6 critères FR + 6 critères BE
- Score 0-100, verdict VALIDE/RISQUE_MODERE/RISQUE_ELEVE/INVALIDE
- Chaque critère a un poids dans le score
- France + Belgique
- Persistance 1:1, pattern existant

## Dépendances
- Aucune
