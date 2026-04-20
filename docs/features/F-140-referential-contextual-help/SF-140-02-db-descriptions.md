# Mini-spec — F-140 / SF-140-02 Descriptions métier persistées en DB

## Identifiant · `F-140 / SF-140-02`
## Date · `2026-04-20` · Branche · `feat/SF-140-02-db-descriptions`

## Contexte — correction d'arbitrage
L'approche initiale "génération frontend des descriptions depuis les données" viole F-139 (DB = seule source de vérité). Si on redéploie l'infra from-scratch, les descriptions disparaissent. Correction : toutes les descriptions vivent en DB, le frontend les lit.

## Changements
- **Migration 093** : ajoute la colonne `description TEXT NULL` à `legal_referentials`
- **Migration 094** : peuple la description pour 9 types (~100 entries) via UPDATE SQL compatibles H2 + PostgreSQL (pas de fonctions JSON) — templates par type qui s'appuient sur `label` et `country`
- **Backend** :
  - `LegalReferential` entity : nouveau champ `description`
  - `ReferentialResponse.Entry` record : nouveau champ `description` exposé par l'API
  - `LegalReferentialService.getReferentials` + `updateReferential` mappent le nouveau champ
- **Frontend** :
  - `ReferentialEntry.description` (`string | null`)
  - `extractMetierDescription` : priorité à `entry.description` DB, fallback vers extraction JSON pour les 6 types à description riche native (rétrocompat)
- **Tests** : 2 nouveaux tests (DB prime sur JSON, DB vide → fallback)

## Types couverts par la migration 094

| Type | Couverture | Volume |
|---|---|---|
| LITIGATION_TYPE | ✅ template label | 7 |
| BAREME_MACRON | ✅ template label | 3 |
| INDEMNITE_BAREMES | ✅ spécifique (MACRON / CCT109) | 2 |
| CONVENTION_BAREMES | ✅ template label × country | ~66 |
| PENSION_TAUX | ✅ template label | 2 |
| PRESTATION_COEFF | ✅ template label | 2 |
| IMMIGRATION_JALONS | ✅ template label | 4 |
| IMMIGRATION_PIECES | ✅ template label | 10 |
| GARDE_MODES | ✅ template label | 6 |
| **Total** | | **~102** |

Types **non peuplés** par 094 (volontairement — ils ont déjà une description riche native dans leur JSON, exposée par le fallback frontend) :
- LICENCIEMENT_CRITERES (14 entries, champ `description` dans JSON)
- RUPTURE_CONV_CRITERES (6 entries, champ `description` dans JSON)
- IMMIGRATION_TITLES (13 entries, champs `motif` + `conditions` dans JSON)
- IMMIGRATION_RECOURS (6 entries, champs `juridiction` + `textesApplicables`)
- IMMIGRATION_WORK_RIGHTS (16 entries, champs `droitTravail` + `conditions`)
- DIVORCE_ETAPES (13 entries, champ `description` dans JSON)
- DIVORCE_PIECES (17 entries, champ `description` dans JSON)

Ces 7 types pourront être migrés en DB à l'avenir si besoin d'édition via admin F-110 — le frontend les prend en charge automatiquement dès qu'une description DB est présente.

## Critères d'acceptation
- [x] Migration 093 ajoute `description TEXT NULL` à `legal_referentials`
- [x] Migration 094 peuple ~102 entries
- [x] Entity + DTO + service backend exposent `description`
- [x] Model frontend inclut `description?: string | null`
- [x] `extractMetierDescription` : DB first, JSON fallback
- [x] Context load PASS (961/961 backend + 1063/1063 frontend avec +2 tests)

## Hors scope
- Édition via admin F-110 : fonctionne déjà automatiquement (label est éditable → description aussi sera éditable en ajoutant le champ au dialog F-110, mais c'est un autre SF si priorité)
- Descriptions plus riches / cabinet-spécifiques : l'admin peut override entrée par entrée
