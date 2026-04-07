# Mini-spec — F-DT-07 / SF-DT-07-01 Référentiel barèmes et calculateur ancienneté/congés

## Identifiant
`F-DT-07 / SF-DT-07-01`

## Feature parente
`F-DT-07` — Barème d'ancienneté et congés conventionnels

## Branche Git
`feat/SF-DT-07-01-referentiel-anciennete`

## Objectif
Référentiel statique des barèmes d'ancienneté par convention collective (FR) et commission paritaire (BE). Calculateur pur : date d'entrée + convention → ancienneté, congés acquis, prime d'ancienneté, écart avec le minimum légal. Entity 1:1, migration.

## Critères d'acceptation
- Min 5 conventions FR + 3 commissions paritaires BE
- Chaque barème contient : congés légaux, congés supplémentaires par tranche, prime d'ancienneté
- Calculateur retourne ancienneté en années/mois, congés acquis, prime, écarts
- France + Belgique
- Persistance 1:1, pattern existant

## Analyse d'impact
- Aucune préoccupation transversale

## Dépendances
- Aucune
