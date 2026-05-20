# SF-215-17 — Annexe 13quinquies OQT + interdiction d'entrée art. 74/11 — backend

## Identifiant
`F-215 / SF-215-17`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-17-annexe13quinquies-ie-be-backend`

---

## Objectif
Livrer le Calculator + Service + Entity + Endpoint backend pour `F-IM-33-annexe13quinquies-ie-be` : outil dédié à l'Annexe 13quinquies (OQT + interdiction d'entrée art. 74/11 Loi 15/12/1980) — calcul de la durée de l'interdiction (3 / 5 / 8 ans selon motif) + délais de recours CCE + conditions de levée (art. 74/12), BELGIQUE UNIQUEMENT. Distinct de `F-IM-08-annexe13-be` (Annexe 13 simple — OQT sans IE).

---

## Comportement attendu

### Cas nominal
- POST `/api/v1/case-files/{caseFileId}/annexe13quinquies-be-analysis`
- Body :
  - `dateNotificationAnnexe` (LocalDate, requis)
  - `motifInterdictionEntree` (enum : SEJOUR_IRREGULIER / MENACE_ORDRE_PUBLIC / RAISONS_SECURITE_NATIONALE / ATTEINTE_INTERET_UE / DECISION_JUDICIAIRE, requis)
  - `precedentSejour` (Boolean — étranger avait un titre de séjour avant, requis)
  - `recoursForme` (Boolean, requis)
  - `dateRecours` (LocalDate, optionnel)
- `Annexe13quinquiesBeCalculator` calcule :
  - `dureeInterdiction` = selon `motifInterdictionEntree` :
    - SEJOUR_IRREGULIER + !precedentSejour → 3 ans
    - SEJOUR_IRREGULIER + precedentSejour → 5 ans
    - MENACE_ORDRE_PUBLIC / SECURITE_NATIONALE → 5 ans
    - ATTEINTE_INTERET_UE / DECISION_JUDICIAIRE → 8 ans (maximum)
  - `dateFinInterdiction` = dateNotificationAnnexe + dureeInterdiction années
  - `datePossibleLevePrecoce` = dateNotificationAnnexe + (2/3 × dureeInterdiction × 12) mois (levée anticipée possible si ≥ 2/3 du délai écoulé — art. 74/12)
  - `dateLimiteRecoursAnnulation` = dateNotificationAnnexe + 30 jours calendaires (recours CCE annulation)
  - `joursRestantsRecours` = dateLimiteRecoursAnnulation − today
  - `statutRecours` ∈ { DISPONIBLE, URGENT, EXPIRE, FORME }
  - `conditionsLevee` : liste des conditions de levée anticipée art. 74/12 (aucune infraction, intégration, raisons humanitaires graves)

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| workspace.country ≠ BELGIQUE | 400 | 400 |
| legalDomain ≠ DROIT_IMMIGRATION | 400 | 400 |
| motifInterdictionEntree inconnu | 400 | 400 |
| dateNotificationAnnexe future > 1 j | 400 | 400 |

---

## Analyse de cohérence transversale

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `F-IM-08-annexe13-be` | Oui | F-IM-08 = Annexe 13 simple (OQT sans IE). Annexe 13quinquies = OQT + IE. Outil distinct. Distinction documentée dans les deux components. |
| SF-215-13 (CCE annulation 30j) | Oui | Délais CCE identiques — mais F-IM-33 les recalcule localement (pas d'appel cross-outil, pour auto-suffisance). |

---

## Conformité F-IA-04
- [ ] **Non applicable** — SF backend pure. Composant : SF-215-18.

---

## Champs IA à extraire (pré-remplissage)

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|-------|------|-----------------------------------------|-----------|
| `dateNotificationAnnexe` | date | `interdictionEntreeDateNotification` | Nouveau |
| `motifInterdictionEntree` | enum | `interdictionEntreeMotif` | Nouveau — whitelist 5 valeurs |

2 champs réels. `precedentSejour`, `recoursForme`, `dateRecours` aspirationnels.

---

## Critères d'acceptation

- [ ] `dureeInterdiction = 3` si SEJOUR_IRREGULIER + !precedentSejour
- [ ] `dureeInterdiction = 8` si DECISION_JUDICIAIRE
- [ ] `datePossibleLevePrecoce` = 2/3 du délai calculée correctement
- [ ] `dateLimiteRecoursAnnulation` = notification + 30j calendaires
- [ ] POST workspace FR → 400
- [ ] UT Calculator : ≥ 8 cas (durées 3/5/8 ans, levée précoce, statut recours)
- [ ] IT Controller : ≥ 6 tests
- [ ] `F-IM-33-annexe13quinquies-ie-be` dans `KNOWN_FRONTEND_TOOL_IDS`
- [ ] Migration : table `annexe13quinquies_be_analyses` + visibility CONTEXTUAL (`interdiction_entree_be_detectee=true`)

---

## Hors périmètre
- Composant Angular (SF-215-18)
- Levée effective de l'interdiction (recours art. 74/12 → F-IM-06 générique)
- Interdiction d'entrée suite expulsion art. 20-22 (F-221)

---

## Plan de test
- `Annexe13quinquiesBeCalculatorTest` : ≥ 8 cas
- `Annexe13quinquiesBeControllerIT`

## Notes et décisions
- Source : Loi 15/12/1980 art. 74/11 (interdiction d'entrée), 74/12 (levée interdiction) ; AR 08/10/1981 (Annexe 13quinquies).
- Durées 3/5/8 ans = art. 74/11 §2 — annotation `// (à vérifier par avocat BE)`.
- 2/3 du délai = seuil levée anticipée — à vérifier. Certaines jurisprudences CCE admettent des demandes de levée plus tôt pour motifs humanitaires.
