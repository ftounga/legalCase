# Mini-spec — F-REF-01 / SF-REF-01-01 Migration seed data + méthodes LegalReferentialService

## Branche Git
`feat/SF-REF-01-01-seed-data-service`

## Objectif
Insérer les données des 9 référentiels statiques dans `legal_referentials` via migration Liquibase. Ajouter les méthodes de résolution dans `LegalReferentialService` (DB first → fallback statique).

## 9 nouveaux referential_type
- IMMIGRATION_TITLES, IMMIGRATION_RECOURS, IMMIGRATION_WORK_RIGHTS
- CONVENTION_BAREMES, LICENCIEMENT_CRITERES, INDEMNITE_BAREMES
- GARDE_MODES, DIVORCE_ETAPES, DIVORCE_PIECES

## Critères d'acceptation
- Toutes les données des 9 référentiels sont en DB
- LegalReferentialService a une méthode de résolution par type
- Chaque méthode fait DB first → fallback statique
- Les tests vérifient le fallback et la résolution DB

## Dépendances
- Aucune
