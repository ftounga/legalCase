# Mini-spec — F-FA-05 / SF-FA-05-01 Calculateur de soulte et droits de partage

## Branche Git
`feat/SF-FA-05-01-calculateur-soulte`

## Objectif
Calculateur pur : valeur vénale + capital restant dû + quote-parts → soulte due, droits de partage (1.1% FR / 1% ou 2.5% BE), frais notaire. Entity 1:1, migration.

## Critères d'acceptation
- Calcul soulte = (valeur nette × quote-part attributaire) - (valeur nette × quote-part cédant)
- Droit de partage FR : 1.1% de l'actif net partagé
- Droits d'enregistrement BE : 1% (partage suite divorce) ou 2.5% (autres cas)
- France + Belgique
- Persistance 1:1

## Dépendances
- Aucune
