# Mini-spec — F-DT-22 / SF-DT-22-01 Requalification CDD → CDI (backend)

## Identifiant
`F-DT-22 / SF-DT-22-01`

## Feature parente
`F-DT-22` — Requalification CDD → CDI (art. L.1245-1, L.1245-2, L.1242-12 Code du travail)

## Statut `draft` · Date `2026-04-25` · Branche `feat/SF-DT-22-01-requalification-cdd-cdi-backend`

---

## Objectif

Fournir un outil décisionnel **backend** dédié à l'analyse de requalification d'un (ou plusieurs) CDD en CDI : détection des motifs interdits (art. L.1242-1 / L.1242-3), évaluation de la succession de CDD non conforme (art. L.1244-3 délai de carence), scoring de probabilité de requalification (0-100) et calcul de l'indemnité de requalification (1 mois minimum, art. L.1245-2) cumulée avec la prime de précarité (art. L.1243-8, rappel F-DT-17). L'outil produit un verdict et une formule traçables que l'avocat peut intégrer à son argumentaire.

---

## Comportement attendu

### Cas nominal

**Entrée** :
- `motifCddInvoque` (enum, requis) : motif officiellement invoqué par l'employeur dans le contrat.
- `motifInterdit` (booléen, requis) : indique si l'avocat a identifié un motif structurellement interdit (emploi permanent, remplacement gréviste, travaux dangereux). Cette appréciation reste juridique et ne peut être déduite mécaniquement.
- `motifInterditType` (enum nullable) : précision du motif interdit, requise quand `motifInterdit=true`.
- `successionCdd` (liste, ≥ 1) : suite chronologique de CDD entre l'employeur et le salarié, chaque entrée porte `dateDebut`, `dateFin` et `motif` (texte libre).
- `delaiCarenceRespecte` (booléen, requis) : `true` si l'employeur a respecté la durée de carence entre deux CDD (art. L.1244-3).
- `dureeContratMois` (entier, > 0) : durée totale (en mois) cumulée des CDD considérés.
- `salaireMensuelBrutEur` (BigDecimal, > 0) : salaire brut mensuel de référence.
- `dateFinDernierContrat` (LocalDate, requis) : date de fin du dernier CDD considéré (sert au rappel de prescription L.1471-1).

**Sortie** :
- `scoreRequalification` (0-100) calculé selon l'algorithme ci-dessous.
- `verdictProbabiliteRequalification` ∈ `ELEVEE` (≥ 60), `MOYENNE` (30-59), `FAIBLE` (< 30).
- `indemniteRequalificationEur = max(salaireMensuelBrutEur, 1 × salaireMensuelBrutEur)` — le plancher d'1 mois est forfaitaire (art. L.1245-2). On laisse la borne `max` pour préparer l'évolution future (jurisprudence : possibilité de retenir un salaire reconstitué supérieur).
- `indemnitePrecariteEur = totalRemunerations × 10 %` où `totalRemunerations = salaireMensuelBrutEur × dureeContratMois`. C'est un rappel chiffré de l'indemnité de l'art. L.1243-8 — l'outil dédié reste F-DT-17.
- `totalDommagesIndemniteEur = indemniteRequalificationEur + indemnitePrecariteEur`.
- `baseJuridique` : citations articles applicables.
- `formule` : description littérale du calcul.
- `messages` : rappels de prescription, mises en garde, citations jurisprudentielles.
- `country = "FRANCE"`.

### Algorithme de scoring (0-100)

Score initial = `0`.
- Si `motifInterdit == true` → `+50`.
- Si `successionCdd.size() ≥ 3` → `+20`.
- Si `delaiCarenceRespecte == false` → `+20`.
- Si `motifCddInvoque == AUTRE` ou `motifCddInvoque == EMPLOI_USAGE` (motif souvent contesté) → `+10`.

Score plafonné à `100`. Verdict :
- `ELEVEE` ≥ 60.
- `MOYENNE` 30-59.
- `FAIBLE` < 30.

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|---|---|---|
| `motifCddInvoque` null/inconnu | 400 "Motif CDD invoqué requis" | 400 |
| `motifInterdit=true` mais `motifInterditType=null` | 400 "Type de motif interdit requis quand motifInterdit=true" | 400 |
| `motifInterditType` inconnu | 400 "Type de motif interdit inconnu" | 400 |
| `successionCdd` vide / null | 400 "Au moins un CDD requis dans la succession" | 400 |
| `successionCdd[i].dateDebut > dateFin` | 400 "Dates incohérentes pour le CDD #i" | 400 |
| `dureeContratMois ≤ 0` | 400 "Durée contrat (mois) requise et > 0" | 400 |
| `salaireMensuelBrutEur ≤ 0` | 400 "Salaire mensuel brut requis et > 0" | 400 |
| `dateFinDernierContrat` null | 400 "Date de fin du dernier contrat requise" | 400 |
| Dossier d'un autre domaine | 400 "Ce dossier n'est pas un dossier de droit du travail" | 400 |
| Workspace différent | 404 | 404 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils travail FR** : F-DT-01 (licenciement), F-DT-09 (comparateur indemnités), F-DT-17 (précarité CDD), F-DT-21 (travail dissimulé), F-132 (rupture conv), F-136 (procédure prud'h). Aucun ne couvre la requalification CDD→CDI. Situation métier propre.
- [x] **Pays Belgique** : la requalification de CDD successifs en CDI est encadrée par la loi du 03/07/1978 (art. 10 succession illicite) mais selon une logique différente (succession ininterrompue, motif justificatif, durée). Pas dans cette SF — feature jumelle BE candidate au backlog si demande utilisateur.
- [x] **Pattern** : Réutilisation du squelette F-DT-17 / F-DT-21 (calculator statique + entity 1:1 + service + controller + migration + visibility rule). Pas de nouveau pattern UI/service partagé.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Création outil requalification CDD→CDI FR | Oui | Intégré (cette SF) |
| F-IA-04 visibility rule | Oui | Règle ALWAYS_ON DROIT_DU_TRAVAIL/FRANCE priority 54 |
| Frontend Angular | Reporté | SF-DT-22-02 (à créer) |
| BE jumeau | Backlog | F-DT-22-BE candidat (à valider) |
| Cumul avec F-DT-17 | Référencé | indemnité de précarité réaffichée pour cohérence — l'outil F-DT-17 reste la source de vérité |

### Décision

- [x] Backend complet dans cette SF
- [x] Frontend reporté
- [x] Jumeau Belgique reporté (à valider)
- [x] Cumul F-DT-17 limité à un rappel chiffré inline (pas d'override)

---

## Impact par domaine métier

**Sensible au domaine** : DROIT_DU_TRAVAIL FRANCE uniquement.
- Travail FR : cœur de la SF.
- Travail BE : régime distinct (loi du 03/07/1978) — non couvert dans cette SF, candidat backlog.
- Immigration / Famille : non applicable.

---

## Parité des domaines métier

Niveau 5 (scoring + analyse de validité). La règle de parité s'applique :
- Travail FR : couvert par cette SF.
- Travail BE : non couvert ; candidat F-DT-22-BE backlog (à valider).
- Immigration / Famille : non applicable (absence de notion équivalente).

---

## Critères d'acceptation

- [ ] **C1** : `RequalificationCddCdiCalculator.compute(...)` avec `motifInterdit=true, motifInterditType=EMPLOI_PERMANENT, succession=2 contrats, delaiCarenceRespecte=true, dureeContratMois=12, salaireMensuelBrutEur=2500` → score 50, verdict `MOYENNE`, indemnité requalif 2500.00, précarité 3000.00, total 5500.00.
- [ ] **C2** : Score plafonné à 100 (3 CDD + carence non respectée + motif interdit + AUTRE = 100).
- [ ] **C3** : Score 0 (motif licite, 1 seul CDD, carence respectée, motif standard) → verdict `FAIBLE`.
- [ ] **C4** : Score = 60 → verdict `ELEVEE` (frontière inclusive).
- [ ] **C5** : Score = 30 → verdict `MOYENNE` (frontière inclusive).
- [ ] **C6** : `motifInterdit=true` sans `motifInterditType` → IllegalArgumentException.
- [ ] **C7** : `successionCdd` vide → IllegalArgumentException.
- [ ] **C8** : `successionCdd[0].dateDebut > dateFin` → IllegalArgumentException avec mention "#1".
- [ ] **C9** : `dureeContratMois=0` → IllegalArgumentException.
- [ ] **C10** : `salaireMensuelBrutEur=0` → IllegalArgumentException.
- [ ] **C11** : Migration 140 crée la table `requalification_cdd_cdi_analyses` + UNIQUE `case_file_id`.
- [ ] **C12** : Seed `decision_tool_visibility_rules` (ALWAYS_ON, DROIT_DU_TRAVAIL, FRANCE, priority 54, UUID `f1a04001-0000-0000-0000-ee0000000221`).
- [ ] **C13** : `POST /api/v1/case-files/{id}/requalification-cdd-cdi` valide → 200.
- [ ] **C14** : `POST` dossier DROIT_IMMIGRATION → 400.
- [ ] **C15** : `POST` workspace étranger → 404.
- [ ] **C16** : `POST` upsert (2e appel remplace le premier).
- [ ] **C17** : `GET` après POST renvoie le résultat persisté.
- [ ] **C18** : `GET` sans POST → 404.

---

## Périmètre

### Hors scope (explicite)
- Frontend Angular → SF ultérieure.
- Détection automatique par l'IA → hors périmètre (analyse fine de chaque CDD individuel, contrats, motifs réels).
- Cumul détaillé avec licenciement, droit individuel à la formation, congés payés → l'outil F-DT-09 (comparateur) reste la référence pour les agrégats. Cette SF rappelle la précarité CDD et l'indemnité de requalif.
- Belgique → hors scope, candidat backlog.

---

## Technique

### Contrat API

#### `POST /api/v1/case-files/{caseFileId}/requalification-cdd-cdi`

Request body (JSON) :
```json
{
  "motifCddInvoque": "ACCROISSEMENT_TEMPORAIRE",
  "motifInterdit": false,
  "motifInterditType": null,
  "successionCdd": [
    {"dateDebut": "2024-01-01", "dateFin": "2024-06-30", "motif": "ACCROISSEMENT"},
    {"dateDebut": "2024-08-01", "dateFin": "2024-12-31", "motif": "REMPLACEMENT"}
  ],
  "delaiCarenceRespecte": false,
  "dureeContratMois": 12,
  "salaireMensuelBrutEur": 2500.00,
  "dateFinDernierContrat": "2024-12-31"
}
```

`motifCddInvoque` enum :
- `ACCROISSEMENT_TEMPORAIRE`
- `REMPLACEMENT_SALARIE`
- `EMPLOI_SAISONNIER`
- `EMPLOI_USAGE`
- `CONTRAT_VENDANGE`
- `INSERTION_PROFESSIONNELLE`
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
  "motifCddInvoque": "ACCROISSEMENT_TEMPORAIRE",
  "motifInterdit": false,
  "motifInterditType": null,
  "successionCdd": [...],
  "delaiCarenceRespecte": false,
  "dureeContratMois": 12,
  "salaireMensuelBrutEur": 2500.00,
  "dateFinDernierContrat": "2024-12-31",
  "scoreRequalification": 40,
  "verdictProbabiliteRequalification": "MOYENNE",
  "indemniteRequalificationEur": 2500.00,
  "indemnitePrecariteEur": 3000.00,
  "totalDommagesIndemniteEur": 5500.00,
  "baseJuridique": "Art. L.1245-2 Code travail (1 mois minimum) + L.1243-8 (précarité)",
  "formule": "Indemnité requalification = 1 × 2500,00 € (plancher art. L.1245-2). Précarité 10 % × 30000,00 € = 3000,00 €.",
  "messages": [...],
  "country": "FRANCE"
}
```

Codes erreur :
- `400` : validation input (cf. tableau "Cas d'erreur").
- `404` : dossier inexistant ou hors workspace.

#### `GET /api/v1/case-files/{caseFileId}/requalification-cdd-cdi`

Sans body. Retourne la dernière analyse persistée ou `404` si aucune.

### Endpoints
| Méthode | URL | Auth |
|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/requalification-cdd-cdi` | OAuth2 |
| GET | `/api/v1/case-files/{caseFileId}/requalification-cdd-cdi` | OAuth2 |

### Tables impactées
| Table | Opération | Notes |
|---|---|---|
| `requalification_cdd_cdi_analyses` | CREATE | nouvelle table 1:1 avec `case_files` |
| `decision_tool_visibility_rules` | INSERT 1 ligne | F-IA-04 ALWAYS_ON DROIT_DU_TRAVAIL FRANCE priority 54 |

### Migration Liquibase
- [x] Oui — `140-create-requalification-cdd-cdi-analyses.xml`

UUID visibility rule : `f1a04001-0000-0000-0000-ee0000000221`.

### Composants créés
- Backend :
  - `RequalificationCddCdiCalculator` (statique, motif des 4 critères de scoring, calcul indemnités)
  - `RequalificationCddCdiAnalysis` (entity)
  - `RequalificationCddCdiRepository`
  - `RequalificationCddCdiRequest` (record + record interne `CddSuccessionEntry`)
  - `RequalificationCddCdiResponse` (record)
  - `RequalificationCddCdiResult` (record)
  - `RequalificationCddCdiService`
  - `RequalificationCddCdiController`
- Tests : `RequalificationCddCdiCalculatorTest` (UT, ≥ 14), `RequalificationCddCdiControllerIT` (IT, ≥ 8).

### Contraintes de validation

| Champ | Obligatoire | Format |
|---|---|---|
| `motifCddInvoque` | Oui | enum |
| `motifInterdit` | Oui | bool |
| `motifInterditType` | Conditionnel (oui ssi `motifInterdit=true`) | enum |
| `successionCdd` | Oui (≥ 1) | liste |
| `delaiCarenceRespecte` | Oui | bool |
| `dureeContratMois` | Oui | int > 0 |
| `salaireMensuelBrutEur` | Oui | BigDecimal > 0 |
| `dateFinDernierContrat` | Oui | LocalDate |
| `case_file_id` | — | UNIQUE |

---

## Plan de test

### Tests unitaires (≥ 14)
- [ ] `compute_nominal_motifInterdit_returnsScore50_verdictMoyenne`
- [ ] `compute_motifInterditTrueWithoutType_throws`
- [ ] `compute_unknownMotifInterditType_throws`
- [ ] `compute_unknownMotifCddInvoque_throws`
- [ ] `compute_emptySuccession_throws`
- [ ] `compute_invalidDates_throws`
- [ ] `compute_zeroDureeContratMois_throws`
- [ ] `compute_zeroSalaire_throws`
- [ ] `compute_3CddSucessionsWithoutCarence_returnsScore40`
- [ ] `compute_motifAutre_addsTen`
- [ ] `compute_motifEmploiUsage_addsTen`
- [ ] `compute_allCriteriaMet_scoreCappedAt100_verdictElevee`
- [ ] `compute_baseJuridique_includesL12452_andL12438`
- [ ] `compute_formule_containsCalculDetails`
- [ ] `compute_score60_isVerdictElevee_inclusiveBoundary`
- [ ] `compute_score30_isVerdictMoyenne_inclusiveBoundary`
- [ ] `compute_score29_isVerdictFaible`
- [ ] `compute_indemnitePrecarite_calculatedFromTotalRemunerations`

### Tests d'intégration (≥ 8)
- [ ] `POST_nominal_returns200`
- [ ] `POST_upsert_replacesAnalysis`
- [ ] `POST_invalidInput_returns400` (succession vide)
- [ ] `POST_motifInterditWithoutType_returns400`
- [ ] `POST_immigrationCaseFile_returns400`
- [ ] `POST_otherWorkspace_returns404`
- [ ] `GET_afterPost_returnsPersisted`
- [ ] `GET_withoutPost_returns404`
- [ ] `POST_motifInterditTrue_returnsScore50`

### Isolation workspace
- [x] `POST_otherWorkspace_returns404` couvre le pattern.

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] Aucune (isolé)

### Composants / endpoints impactés
Aucun — ajout pur. Pas de modification de F-DT-17.

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

- **Scoring binaire-additif et non bayésien** : le besoin produit est de fournir un signal lisible à l'avocat, pas une probabilité statistiquement calibrée. L'avocat reste le décideur final.
- **Plafonnement à 100** : le score est plafonné pour préserver la lisibilité du verdict. Aucun cas réel ne devrait additionner les 4 critères max sans signal jurisprudentiel net.
- **Indemnité de précarité réaffichée** : on rappelle le calcul (10 %) pour donner une vue chiffrée totale au prud'hommes. F-DT-17 reste l'outil de référence pour le calcul détaillé (taux 6 %, exclusions L.1243-10). Pas d'override des résultats F-DT-17.
- **Indemnité requalification = 1 × salaire** : plancher art. L.1245-2. La jurisprudence permet de retenir un salaire reconstitué supérieur — l'outil prend le salaire fourni par l'avocat tel quel.
- **Pas de calcul de prescription** : l'outil rappelle dans `messages` la prescription quinquennale (art. L.1471-1, 12 mois pour requalification depuis 2017 selon Cass. soc. 29 janv. 2020) mais ne refuse pas le calcul si dépassée. C'est l'avocat qui apprécie.
