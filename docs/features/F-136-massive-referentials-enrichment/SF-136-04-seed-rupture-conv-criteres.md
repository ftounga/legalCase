# Mini-spec — F-136 / SF-136-04 Seedage RUPTURE_CONV_CRITERES

## Identifiant · `F-136 / SF-136-04`
## Date · `2026-04-20` · Branche · `feat/SF-136-04-seed-rupture-conv-criteres`

## Objectif
Combler le gap architectural identifié par F-138 SF-138-01 : le type `RUPTURE_CONV_CRITERES` n'était jamais seedé en DB (uniquement `RuptureConvCritereReferentiel` Java). Seeder les 6 critères FR.

## Critères seedés (6 critères FR, somme poids = 100)

| Code | Bloquant | Poids | Article |
|---|---|---|---|
| RC_CONSENTEMENT | ✅ | 25 | L1237-11 |
| RC_DELAI_RETRACTATION | ✅ | 20 | L1237-13 al.2 |
| RC_HOMOLOGATION | ✅ | 25 | L1237-14 |
| RC_ASSISTANCE | ❌ | 10 | L1237-12 dernier al. |
| RC_INDEMNITE | ✅ | 15 | L1237-13 al.1 |
| RC_ENTRETIENS | ❌ | 5 | L1237-12 al.1 |

Pattern JSON homogène avec `LICENCIEMENT_CRITERES` : `{"poids":N,"bloquant":bool,"description":"..."}`.

## Critères d'acceptation
- [x] Migration 092 avec 6 INSERT + rollback DELETE
- [x] Données 1:1 avec `RuptureConvCritereReferentiel.java` (label, poids, bloquant, description, source_ref)
- [x] UUIDs c6-cb sans collision (b8-be SF-129-04, bf-c5 SF-136-03)
- [x] Context load PASS

## Hors scope explicite

Dettes architecturales notées par F-138 mais **hors périmètre data-only de F-136** :

1. `BAREME_MACRON` seedé comme marker `{supported:true}` mais `IndemniteJurisprudentielReferentiel` lit toujours hardcoded Java. Alignement DB-first patten (F-129-03) à appliquer via SF follow-up.
2. `INDEMNITE_BAREMES` (MACRON + CCT109) seedés mais `IndemniteComparatifCalculator` n'utilise pas `LegalReferentialService`. Même traitement.
3. `RuptureConvAnalyzer` utilise toujours le fallback Java — le seedage pose la fondation, l'alignement code viendra quand le besoin se matérialise (override admin, versioning).

→ Ces 3 points sont notés au **backlog F-139** proposé : *Alignement DB-first des référentiels métier restants* (pattern F-129-03 appliqué aux référentiels statiques restants).

## Technique
Migration pure SQL, pas de changement de code.
