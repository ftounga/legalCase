# SF-215-13 — Recours CCE annulation 30 jours calendaires — backend

## Identifiant
`F-215 / SF-215-13`

## Feature parente
`F-215` — P2 Immigration BE — ~10 outils fréquence haute

## Statut
`ready`

## Date de création
2026-05-20

## Branche Git
`feat/SF-215-13-cce-annulation-30j-be-backend`

---

## Objectif
Livrer le Calculator + Service + Entity + Endpoint backend pour `F-IM-31-cce-annulation-30j-be` : calculateur de délais du recours en annulation devant le Conseil du Contentieux des Étrangers (CCE) — 30 jours **calendaires** depuis la notification de la décision de l'OE ou du CGRA (art. 39/82 §4 al. 1 Loi 15/12/1980), BELGIQUE UNIQUEMENT. Outil dédié (au-delà du générateur générique F-IM-06).

---

## Comportement attendu

### Cas nominal
- POST `/api/v1/case-files/{caseFileId}/cce-annulation-be-analysis`
- Body :
  - `dateNotificationDecision` (LocalDate, requis)
  - `typeDecision` (enum : REFUS_TITRE / REFUS_REGROUPEMENT / REFUS_9BIS / REFUS_9TER / OQT_ANNEXE13 / DECISION_CGRA / AUTRE, requis)
  - `recoursForme` (Boolean, requis)
  - `dateRecours` (LocalDate, optionnel — si recours déjà formé)
- `CceAnnulationBeCalculator` calcule (en jours **calendaires**) :
  - `dateLimiteRecours` = dateNotificationDecision + 30 jours calendaires
  - `joursRestants` = dateLimiteRecours − today (négatif si expiré)
  - `statut` ∈ { DISPONIBLE (joursRestants > 10), URGENT (joursRestants ∈ [1, 10]), EXPIRE (joursRestants ≤ 0), RECOURS_FORME }
  - `recommandation` : si URGENT ou EXPIRE → « Basculer vers recours en extrême urgence (SF-215-15 / F-IM-32) si OQT exécutoire »
  - `delaisMemoire` : « Requête + mémoire administratif + mémoire en réplique 8 jours (art. 39/63 al. 4) »
- Distinction forte avec `F-IM-08-annexe13-be` (Annexe 13 = calculateur OQT simple — délais CCE calculés en sus). Ici : outil dédié recours CCE pour **toute décision** (pas seulement OQT).

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| workspace.country ≠ BELGIQUE | 400 | 400 |
| legalDomain ≠ DROIT_IMMIGRATION | 400 | 400 |
| dateNotificationDecision future | Date de notification future impossible | 400 |
| recoursForme=true sans dateRecours | 400 | 400 |
| dateRecours avant dateNotification | 400 | 400 |

---

## Analyse de cohérence transversale

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `F-IM-08-annexe13-be` | Oui | F-IM-08 couvre OQT + délais CCE en sous-partie. F-IM-31 = outil dédié CCE annulation pour **toute** décision OE/CGRA. Complémentaires, non redondants. Documenter la distinction dans les deux outils. |
| SF-215-15 (extrême urgence 5j) | Oui | F-IM-32 = extrême urgence 5j ouvrables. F-IM-31 = annulation 30j calendaires. Deux procédures distinctes du CCE — deux outils. Recommandation de basculement cross-outil documentée. |
| `F-IM-06-recours` (générateur générique) | Oui | F-IM-06 génère le document de recours. F-IM-31 calcule les délais + statut + recommandations. Complémentaires — pas de duplication. |
| `BelgianBusinessDaysCalculator` | Non applicable | F-IM-31 = jours **calendaires**. `BelgianBusinessDaysCalculator` = jours ouvrables (utilisé par SF-215-15). |

---

## Conformité F-IA-04
- [ ] **Non applicable** — SF backend pure. Composant : SF-215-14.

---

## Champs IA à extraire (pré-remplissage)

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|-------|------|-----------------------------------------|-----------|
| `dateNotificationDecision` | date | `recoursCceDateNotification` | Nouveau |
| `typeDecision` | enum | `recoursCceTypeDecision` | Nouveau — whitelist 7 valeurs |

2 champs réels. `recoursForme` et `dateRecours` = aspirationnels (actions procédurales, non extractibles des pièces).

---

## Critères d'acceptation

- [ ] `dateLimiteRecours` = dateNotification + 30j calendaires
- [ ] `statut = URGENT` si joursRestants ∈ [1, 10]
- [ ] `statut = EXPIRE` si joursRestants ≤ 0
- [ ] `statut = RECOURS_FORME` si recoursForme=true
- [ ] `recommandation` contient référence F-IM-32 si statut URGENT ou EXPIRE
- [ ] POST workspace FR → 400
- [ ] POST dateNotification future → 400
- [ ] UT Calculator : ≥ 8 cas (statuts, dates limites, expiré, formé)
- [ ] IT Controller : ≥ 6 tests
- [ ] `F-IM-31-cce-annulation-30j-be` dans `KNOWN_FRONTEND_TOOL_IDS`
- [ ] Migration : table `cce_annulation_be_analyses` + visibility CONTEXTUAL (`recours_cce_envisage=true`)

---

## Hors périmètre
- Composant Angular (SF-215-14)
- Recours CCE extrême urgence (SF-215-15/16)
- Génération du document requête CCE (F-IM-06)

---

## Plan de test
- `CceAnnulationBeCalculatorTest` : ≥ 8 cas (DISPONIBLE, URGENT, EXPIRE, RECOURS_FORME, +30j exact, date future rejetée)
- `CceAnnulationBeControllerIT`

## Notes et décisions
- Source : Loi 15/12/1980 art. 39/2, 39/56 à 39/85 ; loi 15/09/2006 portant création du Conseil du Contentieux des Étrangers.
- 30 jours **calendaires** (pas ouvrables) — conforme art. 39/82 §4 al. 1 — distinction critique avec 5j ouvrables de l'extrême urgence.
- Délai mémoire en réplique 8 jours = art. 39/63 al. 4 — à annoter `// (à vérifier)`.
