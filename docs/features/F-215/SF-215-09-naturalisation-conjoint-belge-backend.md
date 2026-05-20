# SF-215-09 — Naturalisation conjoint Belge art. 16 CNB — backend

## Identifiant
`F-215 / SF-215-09`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-09-naturalisation-conjoint-belge-be-backend`

---

## Objectif
Livrer le Calculator + Service + Entity + Endpoint backend pour `F-IM-29-naturalisation-conjoint-belge-be` : analyse d'éligibilité à la déclaration de nationalité belge par mariage avec un Belge (art. 16 Code nationalité belge 1984), BELGIQUE UNIQUEMENT. Voie distincte de l'art. 12bis — un outil = une situation.

---

## Comportement attendu

### Cas nominal
- POST `/api/v1/case-files/{caseFileId}/naturalisation-conjoint-belge-be-analysis`
- Body :
  - `dateMarriage` (LocalDate, requis)
  - `cohabitationLegale` (Boolean — cohabitation légale ou mariage enregistré, requis)
  - `dureeCohabitationMois` (Integer — mois de cohabitation ininterrompue, requis)
  - `niveauLangue` (enum : INFERIEUR_A2 / A2 / SUPERIEUR_A2, requis)
  - `preuveIntegration` (Boolean — cours intégration ou diplôme BE, requis)
  - `menaceOrdrePublic` (Boolean, requis)
  - `condamnationPenale` (Boolean — ≥ 3 mois ferme dans 5 ans, requis)
- `NaturalisationConjointBelgeBeCalculator` calcule :
  - `conditionMariage` = mariage légal avec Belge (implicite car il s'agit du dossier)
  - `conditionCohabitation` = dureeCohabitationMois ≥ 60 mois (5 ans — art. 16 §1 2°)
  - `conditionLangue` = niveauLangue ∈ {A2, SUPERIEUR_A2}
  - `conditionIntegration` = preuveIntegration
  - `conditionOrdrePublic` = !menaceOrdrePublic + !condamnationPenale
  - `eligible` = toutes conditions satisfaites
  - `dureeManquante` = max(0, 60 − dureeCohabitationMois)
  - `criteresNonRemplis` : liste humaine
  - `delaiDeclaration` : « Déclaration devant officier d'état civil de la commune de résidence — délai d'instruction : 3-6 mois »

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| workspace.country ≠ BELGIQUE | 400 | 400 |
| legalDomain ≠ DROIT_IMMIGRATION | 400 | 400 |
| dureeCohabitationMois < 0 ou > 600 | 400 | 400 |
| dateMarriage future | Mariage non encore célébré | 400 |

---

## Analyse de cohérence transversale

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| SF-215-07 (art. 12bis) | Oui | Voie distincte — art. 16 = conjoint Belge vs art. 12bis = résidence longue durée. Un outil = une situation. Flag CONTEXTUAL partagé `naturalisation_be_envisagee`. |
| `F-IM-14-40ter-familial-belge-be` | Non | 40ter = regroupement familial (séjour) ; art. 16 = nationalité. Objectifs distincts. |

---

## Conformité F-IA-04
- [ ] **Non applicable** — SF backend pure. Composant : SF-215-10.

---

## Champs IA à extraire (pré-remplissage)

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|-------|------|-----------------------------------------|-----------|
| `dateMarriage` | date | `naturalisationBeArt16DateMarriage` | Nouveau |
| `dureeCohabitationMois` | entier | `naturalisationBeArt16DureeCohabitation` | Nouveau |
| `niveauLangue` | enum | `naturalisationBeArt16NiveauLangue` | Nouveau |
| `preuveIntegration` | booléen | aspirationnel | PREFILL_COUNT_ALWAYS_ZERO |
| `cohabitationLegale` | booléen | aspirationnel | PREFILL_COUNT_ALWAYS_ZERO |

3 champs réels, 2 aspirationnels.

---

## Critères d'acceptation

- [ ] POST → eligible=true si cohabitation ≥ 60 mois + langue A2 + intégration + pas OP
- [ ] POST → dureeManquante = 60 − moisCohabitation si mois < 60
- [ ] POST workspace FR → 400
- [ ] UT : ≥ 7 cas
- [ ] IT : ≥ 6 tests
- [ ] `F-IM-29-naturalisation-conjoint-belge-be` dans `KNOWN_FRONTEND_TOOL_IDS`
- [ ] Migration : table `naturalisation_conjoint_belge_be_analyses` + visibility CONTEXTUAL (`naturalisation_be_envisagee=true`)

---

## Hors périmètre
- Composant Angular (SF-215-10)
- Art. 12bis (SF-215-07/08)
- Cas divorce pendant procédure naturalisation (P3)

---

## Plan de test
- `NaturalisationConjointBelgeBeCalculatorTest`
- `NaturalisationConjointBelgeBeControllerIT`

## Notes et décisions
- Source : Code de la nationalité belge art. 16 ; AR 14/01/2013 ; loi modif. 04/12/2012.
- Durée 5 ans de cohabitation = art. 16 §1 2° CNB — à vérifier par avocat BE (annotation `// (à vérifier)`).
- Déclaration devant officier d'état civil communal (≠ FR où c'est auprès du TI).
