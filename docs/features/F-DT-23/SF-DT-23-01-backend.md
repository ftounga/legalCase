# Mini-spec — F-DT-23 / SF-DT-23-01 Requalification intérim → CDI (backend)

## Identifiant
`F-DT-23 / SF-DT-23-01`

## Feature parente
`F-DT-23` — Requalification intérim → CDI (art. L.1251-40, L.1251-41, L.1251-32 Code du travail)

## Statut `draft` · Date `2026-04-25` · Branche `feat/SF-DT-23-01-requalification-interim-cdi-backend`

---

## Objectif

Fournir un outil décisionnel **backend** dédié à l'analyse de requalification d'une (ou plusieurs) mission(s) d'intérim en CDI : détection des motifs interdits (art. L.1251-6 / L.1251-9), évaluation de la succession de missions chez la même entreprise utilisatrice (art. L.1251-12, L.1251-36 délai de carence), scoring de probabilité de requalification (0-100) et calcul de l'indemnité de requalification (1 mois minimum, art. L.1251-41 al. 2) cumulée avec l'indemnité de fin de mission (art. L.1251-32, rappel F-DT-18). L'outil produit un verdict et une formule traçables que l'avocat peut intégrer à son argumentaire — y compris pour décider si la requalification se demande contre l'**entreprise utilisatrice** (art. L.1251-40, jurisprudence Cass. soc. 2018) ou contre l'**agence d'intérim** (L.1251-41).

Pattern jumeau direct de F-DT-22 (CDD → CDI), adapté à la **relation triangulaire** propre à l'intérim (entreprise utilisatrice / agence / salarié).

---

## Comportement attendu

### Cas nominal

**Entrée** :
- `motifInterimInvoque` (enum, requis) : motif officiellement invoqué (`ACCROISSEMENT_TEMPORAIRE`, `REMPLACEMENT_SALARIE`, `EMPLOI_SAISONNIER`, `EMPLOI_USAGE`, `MISSION_PEPINIERE`, `AUTRE`).
- `motifInterdit` (booléen, requis) : indique si l'avocat a identifié un motif structurellement interdit (emploi permanent, remplacement gréviste, travaux dangereux). Cette appréciation reste juridique.
- `motifInterditType` (enum nullable) : précision du motif interdit (`EMPLOI_PERMANENT`, `REMPLACEMENT_GREVISTE`, `TRAVAUX_DANGEREUX`, `AUTRE`), requise quand `motifInterdit=true`.
- `successionMissions` (liste, ≥ 1) : suite chronologique des missions, chaque entrée porte `dateDebut`, `dateFin`, `motif` (texte libre) et `entrepriseUtilisatrice` (identifiant ou nom).
- `delaiCarenceRespecte` (booléen, requis) : `true` si l'entreprise utilisatrice a respecté la carence entre missions (art. L.1251-36).
- `dureeMissionsTotaleMois` (entier, > 0) : durée totale (en mois) cumulée des missions considérées.
- `salaireMensuelBrutEur` (BigDecimal, > 0) : salaire brut mensuel de référence.
- `dateFinDerniereMission` (LocalDate, requis) : date de fin de la dernière mission (sert au rappel de prescription L.1471-1).
- `memeEntrepriseUtilisatrice` (booléen, requis) : `true` si toutes les missions ont eu lieu chez la même entreprise utilisatrice (déclencheur du critère de succession).

**Sortie** :
- `scoreRequalification` (0-100) calculé selon l'algorithme ci-dessous.
- `verdictProbabiliteRequalification` ∈ `ELEVEE` (≥ 60), `MOYENNE` (30-59), `FAIBLE` (< 30).
- `indemniteRequalificationEur = max(salaireMensuelBrutEur, 1 × salaireMensuelBrutEur)` — plancher 1 mois forfaitaire (art. L.1251-41 al. 2).
- `indemniteFinMissionInterimEur = totalRemunerations × 10 %` où `totalRemunerations = salaireMensuelBrutEur × dureeMissionsTotaleMois`. Rappel chiffré de l'indemnité de fin de mission (art. L.1251-32) — l'outil dédié reste F-DT-18.
- `totalDommagesIndemniteEur = indemniteRequalificationEur + indemniteFinMissionInterimEur`.
- `baseJuridique` : citations articles applicables.
- `formule` : description littérale du calcul.
- `messages` : rappels de prescription, mises en garde, citations jurisprudentielles (notamment Cass. soc. 2018 sur l'action contre l'entreprise utilisatrice).
- `country = "FRANCE"`.

### Algorithme de scoring (0-100)

Score initial = `0`.
- Si `motifInterdit == true` → `+50`.
- Si `successionMissions.size() ≥ 3 && memeEntrepriseUtilisatrice == true` → `+25` (spécificité intérim — Cass. soc. veille).
- Si `delaiCarenceRespecte == false` → `+20`.
- Si `motifInterimInvoque == AUTRE` → `+10`.

Score plafonné à `100`. Verdict :
- `ELEVEE` ≥ 60.
- `MOYENNE` 30-59.
- `FAIBLE` < 30.

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|---|---|---|
| `motifInterimInvoque` null/inconnu | 400 "Motif intérim invoqué requis" | 400 |
| `motifInterdit=true` mais `motifInterditType=null` | 400 "Type de motif interdit requis quand motifInterdit=true" | 400 |
| `motifInterditType` inconnu | 400 "Type de motif interdit inconnu" | 400 |
| `successionMissions` vide / null | 400 "Au moins une mission requise dans la succession" | 400 |
| `successionMissions[i].dateDebut > dateFin` | 400 "Dates incohérentes pour la mission #i" | 400 |
| `dureeMissionsTotaleMois ≤ 0` | 400 "Durée missions (mois) requise et > 0" | 400 |
| `salaireMensuelBrutEur ≤ 0` | 400 "Salaire mensuel brut requis et > 0" | 400 |
| `dateFinDerniereMission` null | 400 "Date de fin de la dernière mission requise" | 400 |
| `memeEntrepriseUtilisatrice` null | 400 "memeEntrepriseUtilisatrice requis (true/false)" | 400 |
| Dossier d'un autre domaine | 400 "Ce dossier n'est pas un dossier de droit du travail" | 400 |
| Workspace différent | 404 | 404 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils travail FR** : F-DT-22 (CDD→CDI, pattern direct), F-DT-17 (précarité CDD), F-DT-18 (indemnité fin mission intérim), F-DT-21 (travail dissimulé). Aucun ne couvre la requalification intérim→CDI. Situation métier propre.
- [x] **Pays Belgique** : la requalification d'une mission d'intérim en CDI est encadrée par la loi du 24/07/1987 (art. 1, art. 8) avec une logique distincte. Pas dans cette SF — feature jumelle BE candidate au backlog.
- [x] **Pattern** : Réutilisation directe du squelette F-DT-22 (calculator statique + entity 1:1 + service + controller + migration + visibility rule). Pas de nouveau pattern UI/service partagé.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Création outil requalification intérim→CDI FR | Oui | Intégré (cette SF) |
| F-IA-04 visibility rule | Oui | Règle ALWAYS_ON DROIT_DU_TRAVAIL/FRANCE priority 58 |
| Frontend Angular | Reporté | SF-DT-23-02 |
| BE jumeau | Backlog | F-DT-23-BE candidat |
| Cumul avec F-DT-18 | Référencé | indemnité de fin de mission rappelée pour cohérence — F-DT-18 reste source de vérité |

### Décision

- [x] Backend complet dans cette SF
- [x] Frontend reporté (SF-DT-23-02)
- [x] Jumeau Belgique reporté (à valider)
- [x] Cumul F-DT-18 limité à un rappel chiffré inline (pas d'override)

---

## Impact par domaine métier

**Sensible au domaine** : DROIT_DU_TRAVAIL FRANCE uniquement.
- Travail FR : cœur de la SF.
- Travail BE : régime distinct (loi du 24/07/1987) — non couvert dans cette SF, candidat backlog.
- Immigration / Famille : non applicable.

---

## Parité des domaines métier

Niveau 5 (scoring + analyse de validité). La règle de parité s'applique :
- Travail FR : couvert par cette SF.
- Travail BE : non couvert ; candidat F-DT-23-BE backlog (à valider).
- Immigration / Famille : non applicable (absence de notion équivalente).

---

## Critères d'acceptation

- [ ] **C1** : `RequalificationInterimCdiCalculator.compute(...)` avec `motifInterdit=true, motifInterditType=EMPLOI_PERMANENT, succession=2 missions, delaiCarenceRespecte=false, dureeMissionsTotaleMois=12, salaireMensuelBrutEur=2500, memeEntrepriseUtilisatrice=true` → score 70, verdict `ELEVEE`, indemnité requalif 2500.00, fin mission 3000.00, total 5500.00.
- [ ] **C2** : Score plafonné à 100 (3 missions même UE + carence non respectée + motif interdit + AUTRE = 105 → 100).
- [ ] **C3** : Score 0 (motif licite, 1 seule mission, carence respectée, motif standard) → verdict `FAIBLE`.
- [ ] **C4** : Score = 60 → verdict `ELEVEE` (frontière inclusive).
- [ ] **C5** : Score = 30 → verdict `MOYENNE` (frontière inclusive).
- [ ] **C6** : `motifInterdit=true` sans `motifInterditType` → IllegalArgumentException.
- [ ] **C7** : `successionMissions` vide → IllegalArgumentException.
- [ ] **C8** : `successionMissions[0].dateDebut > dateFin` → IllegalArgumentException avec mention "#1".
- [ ] **C9** : `dureeMissionsTotaleMois=0` → IllegalArgumentException.
- [ ] **C10** : `salaireMensuelBrutEur=0` → IllegalArgumentException.
- [ ] **C11** : Migration 147 crée la table `requalification_interim_cdi_analyses` + UNIQUE `case_file_id`.
- [ ] **C12** : Seed `decision_tool_visibility_rules` (ALWAYS_ON, DROIT_DU_TRAVAIL, FRANCE, priority 58, UUID `f1a04001-0000-0000-0000-ee0000000231`).
- [ ] **C13** : `POST /api/v1/case-files/{id}/requalification-interim-cdi` valide → 200.
- [ ] **C14** : `POST` dossier DROIT_IMMIGRATION → 400.
- [ ] **C15** : `POST` workspace étranger → 404.
- [ ] **C16** : `POST` upsert (2e appel remplace le premier).
- [ ] **C17** : `GET` après POST renvoie le résultat persisté.
- [ ] **C18** : `GET` sans POST → 404.
- [ ] **C19** : 3 missions chez la même UE + carence non respectée → score 45.
- [ ] **C20** : 3 missions chez UE différentes → critère succession non déclenché.

---

## Périmètre

### Hors scope (explicite)
- Frontend Angular → SF-DT-23-02.
- Détection automatique par l'IA → hors périmètre.
- Cumul détaillé avec licenciement, congés payés → l'outil F-DT-09 (comparateur) reste la référence.
- Belgique → hors scope, candidat backlog.

---

## Technique

### Contrat API

#### `POST /api/v1/case-files/{caseFileId}/requalification-interim-cdi`

Request body (JSON) :
```json
{
  "motifInterimInvoque": "ACCROISSEMENT_TEMPORAIRE",
  "motifInterdit": false,
  "motifInterditType": null,
  "successionMissions": [
    {"dateDebut": "2024-01-01", "dateFin": "2024-06-30", "motif": "ACCROISSEMENT", "entrepriseUtilisatrice": "ENT_X"},
    {"dateDebut": "2024-08-01", "dateFin": "2024-12-31", "motif": "REMPLACEMENT", "entrepriseUtilisatrice": "ENT_X"}
  ],
  "delaiCarenceRespecte": false,
  "dureeMissionsTotaleMois": 12,
  "salaireMensuelBrutEur": 2500.00,
  "dateFinDerniereMission": "2024-12-31",
  "memeEntrepriseUtilisatrice": true
}
```

`motifInterimInvoque` enum :
- `ACCROISSEMENT_TEMPORAIRE`
- `REMPLACEMENT_SALARIE`
- `EMPLOI_SAISONNIER`
- `EMPLOI_USAGE`
- `MISSION_PEPINIERE`
- `AUTRE`

`motifInterditType` enum nullable :
- `EMPLOI_PERMANENT`
- `REMPLACEMENT_GREVISTE`
- `TRAVAUX_DANGEREUX`
- `AUTRE`

Response body (JSON) :
```json
{
  "caseFileId": "uuid",
  "motifInterimInvoque": "ACCROISSEMENT_TEMPORAIRE",
  "motifInterdit": false,
  "motifInterditType": null,
  "successionMissions": [...],
  "delaiCarenceRespecte": false,
  "dureeMissionsTotaleMois": 12,
  "salaireMensuelBrutEur": 2500.00,
  "dateFinDerniereMission": "2024-12-31",
  "memeEntrepriseUtilisatrice": true,
  "scoreRequalification": 45,
  "verdictProbabiliteRequalification": "MOYENNE",
  "indemniteRequalificationEur": 2500.00,
  "indemniteFinMissionInterimEur": 3000.00,
  "totalDommagesIndemniteEur": 5500.00,
  "baseJuridique": "Art. L.1251-40+L.1251-41 (requalif) + L.1251-32 (fin mission)",
  "formule": "Indemnité requalif = 1 × 2500,00 € (plancher art. L.1251-41 al. 2). Fin mission 10 % × 30000,00 € = 3000,00 €.",
  "messages": [
    "Action prescrite par 12 mois (art. L.1471-1)",
    "Requalification possible contre l'entreprise utilisatrice (Cass. soc. 2018)"
  ],
  "country": "FRANCE"
}
```

Codes erreur :
- `400` : validation input (cf. tableau "Cas d'erreur").
- `404` : dossier inexistant ou hors workspace.

#### `GET /api/v1/case-files/{caseFileId}/requalification-interim-cdi`

Sans body. Retourne la dernière analyse persistée ou `404` si aucune.

### Endpoints
| Méthode | URL | Auth |
|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/requalification-interim-cdi` | OAuth2 |
| GET | `/api/v1/case-files/{caseFileId}/requalification-interim-cdi` | OAuth2 |

### Tables impactées
| Table | Opération | Notes |
|---|---|---|
| `requalification_interim_cdi_analyses` | CREATE | nouvelle table 1:1 avec `case_files` |
| `decision_tool_visibility_rules` | INSERT 1 ligne | F-IA-04 ALWAYS_ON DROIT_DU_TRAVAIL FRANCE priority 58 |

### Migration Liquibase
- [x] Oui — `147-create-requalification-interim-cdi-analyses.xml`

UUID visibility rule : `f1a04001-0000-0000-0000-ee0000000231`.

### Composants créés
- Backend :
  - `RequalificationInterimCdiCalculator` (statique, 4 critères de scoring, calcul indemnités)
  - `RequalificationInterimCdiAnalysis` (entity)
  - `RequalificationInterimCdiRepository`
  - `RequalificationInterimCdiRequest` (record + record interne `MissionInterim`)
  - `RequalificationInterimCdiResponse` (record)
  - `RequalificationInterimCdiResult` (record)
  - `RequalificationInterimCdiService`
  - `RequalificationInterimCdiController`
- Tests : `RequalificationInterimCdiCalculatorTest` (UT, ≥ 14), `RequalificationInterimCdiControllerIT` (IT, ≥ 8).

### Contraintes de validation

| Champ | Obligatoire | Format |
|---|---|---|
| `motifInterimInvoque` | Oui | enum |
| `motifInterdit` | Oui | bool |
| `motifInterditType` | Conditionnel (oui ssi `motifInterdit=true`) | enum |
| `successionMissions` | Oui (≥ 1) | liste |
| `delaiCarenceRespecte` | Oui | bool |
| `dureeMissionsTotaleMois` | Oui | int > 0 |
| `salaireMensuelBrutEur` | Oui | BigDecimal > 0 |
| `dateFinDerniereMission` | Oui | LocalDate |
| `memeEntrepriseUtilisatrice` | Oui | bool |
| `case_file_id` | — | UNIQUE |

---

## Plan de test

### Tests unitaires (≥ 14)
- [ ] `compute_nominal_motifInterdit_returnsScore50_verdictMoyenne`
- [ ] `compute_motifInterditTrueWithoutType_throws`
- [ ] `compute_unknownMotifInterditType_throws`
- [ ] `compute_unknownMotifInterimInvoque_throws`
- [ ] `compute_emptySuccession_throws`
- [ ] `compute_invalidDates_throws`
- [ ] `compute_zeroDureeMissionsTotaleMois_throws`
- [ ] `compute_zeroSalaire_throws`
- [ ] `compute_3MissionsSameUEWithoutCarence_returnsScore45`
- [ ] `compute_3MissionsDifferentUE_doesNotTriggerSuccession`
- [ ] `compute_motifAutre_addsTen`
- [ ] `compute_allCriteriaMet_scoreCappedAt100_verdictElevee`
- [ ] `compute_baseJuridique_includesL125140_andL125132`
- [ ] `compute_formule_containsCalculDetails`
- [ ] `compute_score60_isVerdictElevee_inclusiveBoundary`
- [ ] `compute_score30_isVerdictMoyenne_inclusiveBoundary`
- [ ] `compute_indemniteFinMission_calculatedFromTotalRemunerations`
- [ ] `compute_messages_includePrescriptionAndCassSoc`
- [ ] `compute_nullRequest_throws`

### Tests d'intégration (≥ 8)
- [ ] `POST_nominal_returns200`
- [ ] `POST_upsert_replacesAnalysis`
- [ ] `POST_invalidInput_returns400` (succession vide)
- [ ] `POST_motifInterditWithoutType_returns400`
- [ ] `POST_immigrationCaseFile_returns400`
- [ ] `POST_otherWorkspace_returns404`
- [ ] `GET_afterPost_returnsPersisted`
- [ ] `GET_withoutPost_returns404`
- [ ] `POST_motifInterditTrue_returnsScore70`

### Isolation workspace
- [x] `POST_otherWorkspace_returns404` couvre le pattern.

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] Aucune (isolé)

### Composants / endpoints impactés
Aucun — ajout pur. Pas de modification de F-DT-18.

### Smoke tests E2E concernés
- [x] Aucun.

---

## Dépendances

### Subfeatures bloquantes
- SF-IA-04-01 (visibility engine) — done, on s'y branche via règle déclarative.

### Questions ouvertes impactées
Aucune.

---

## Notes et décisions

- **Spécificité intérim — relation triangulaire** : la requalification peut être demandée contre l'entreprise utilisatrice (art. L.1251-40, jurisprudence Cass. soc. 7 mars 2018 n° 16-26.616) ou contre l'agence (L.1251-41). L'outil est neutre — le choix tactique reste à l'avocat. Les `messages` rappellent les deux options.
- **Critère de succession +25 (vs +20 CDD)** : la jurisprudence considère la succession ≥ 3 missions chez la même entreprise utilisatrice comme un signal très fort. On lui attribue un poids légèrement supérieur à la succession CDD (+25 au lieu de +20).
- **Plafonnement à 100** : 50+25+20+10 = 105, plafonné à 100.
- **Indemnité fin de mission réaffichée** : on rappelle le calcul (10 %) pour donner une vue chiffrée totale. F-DT-18 reste l'outil de référence pour le calcul détaillé. Pas d'override des résultats F-DT-18.
- **Indemnité requalification = 1 × salaire** : plancher art. L.1251-41 al. 2.
- **Pas de calcul de prescription** : l'outil rappelle dans `messages` la prescription 12 mois (art. L.1471-1 + Cass. soc. 29 janv. 2020) mais ne refuse pas le calcul si dépassée.
