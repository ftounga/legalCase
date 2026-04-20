# Mini-spec — F-136 / SF-136-02 Enrichissement CCN FR vague 2

## Identifiant · `F-136 / SF-136-02`
## Date · `2026-04-20` · Branche · `feat/SF-136-02-ccn-fr-wave-2`

## Objectif
Enrichir 10 CCN FR à fort effectif / visibilité métier, non encore enrichies par SF-129-02 (top 10 vague 1) ni par SF-129-01 (5 legacy).

## CCN enrichies (IDCC)
| IDCC | Secteur | Effectif indicatif |
|---|---|---|
| 2120 | Banques (AFB) | ~230k |
| 1351 | Prévention & sécurité | ~180k |
| 2098 | Prestataires services tertiaire | ~160k |
| 1480 | Journalistes | spécifique (30j congés) |
| 1483 | Habillement | détail |
| 2596 | Coiffure | détail |
| 2148 | Télécommunications | |
| 2511 | Sport | |
| 3127 | Promotion-construction | |
| 1516 | Organismes de formation | |

## Critères d'acceptation
- [x] Migration 090 `UPDATE` avec `value_json` enrichi + `source_ref` documenté
- [x] `source_ref` mentionne "valeurs indicatives" pour les CCN dont le barème n'est pas certifié primaire
- [x] Rollback à la minimaliste (`congesSupp:[], primes:[]`)
- [x] Context load PASS (981 → 984 puis 984 après SF-137-01 : vert)
- [x] Aucune régression

## Hors scope
- Enrichir HCR / COMMERCE / autres CCN : les valeurs actuelles sont déjà plausibles → pas de touche
- CP belges (SF-136-03)
- Autres référentiels (SF-136-04)

## Technique
- Migration Liquibase `090-enrich-ccn-fr-wave-2.xml` — 10 UPDATE idempotents
- Pas de changement de code (DB-only)
