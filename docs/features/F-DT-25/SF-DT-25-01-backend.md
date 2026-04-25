# Mini-spec — F-DT-25 / SF-DT-25-01 Backend Indemnité compensatrice de préavis FR

## Identifiant
`F-DT-25 / SF-DT-25-01`

## Feature parente
`F-DT-25` — Indemnité compensatrice de préavis FR (art. L.1234-1 Code du travail + CCN)

## Statut `draft` · Date `2026-04-25` · Branche `feat/SF-DT-25-01-indemnite-preavis-backend`

---

## Objectif

Fournir un outil décisionnel dédié au calcul de l'indemnité compensatrice de préavis due au salarié dispensé d'effectuer son préavis par l'employeur, selon l'art. L.1234-1 Code du travail, en consultant le référentiel CCN (`CONVENTION_PREAVIS`) pour utiliser une durée plus favorable que la durée légale lorsque la convention collective le prévoit.

---

## Comportement attendu

### Cas nominal

**Détermination de la durée légale (L.1234-1) :**
- Ancienneté < 6 mois → fallback CCN/usage. À défaut de CCN, 0 mois (laissé à l'usage local — message dédié).
- Ancienneté ∈ [6 mois ; 24 mois[ → 1 mois.
- Ancienneté ≥ 24 mois → 2 mois.

**Application de la CCN (`CONVENTION_PREAVIS`) :**
- Si `conventionCollectiveCode` fourni et présent dans la table `legal_referentials` (type `CONVENTION_PREAVIS`, FR), récupérer la durée prévue par la CCN pour la combinaison (`fonction`, `ancienneteMois`).
- La durée retenue = `max(durée légale, durée CCN)` — la CCN ne peut être que plus favorable au salarié (art. L.2253-3, principe de faveur).
- `sourceDuree` = `CCN` quand la durée CCN est strictement supérieure à la légale, sinon `LEGALE`.
- `USAGE` : retenu uniquement quand ancienneté < 6 mois et CCN absente — la durée d'usage n'est pas codifiée, on retourne 0 mois avec un message demandant à l'avocat de vérifier l'usage local.

**Calcul du montant :**
- Si `exemptionEmployeur=false` → `montantIndemnite=0`, `exemptionRetenue=false`, message expliquant que l'indemnité n'est due que si l'employeur a dispensé du préavis.
- Sinon `montantIndemnite = dureePreavisMois × salaireMensuelBrutEur`, arrondi au centime (HALF_UP).

**Sortie** :
- `dureePreavisMois`, `sourceDuree` enum, `montantIndemniteEur`, `exemptionRetenue`, `baseJuridique`, `formule`, `messages`, `country=FRANCE`.

### Cas d'erreur
| Situation | Comportement | Code HTTP |
|---|---|---|
| `ancienneteMois` nul ou négatif | 400 "Ancienneté en mois requise et positive" | 400 |
| `salaireMensuelBrutEur` nul ou ≤0 | 400 "Salaire mensuel requis et positif" | 400 |
| `fonction` enum invalide | 400 message enum | 400 |
| `dateRupture` absente | 400 "Date de rupture requise" | 400 |
| Dossier d'un autre domaine (FAMILLE/IMMIGRATION) | 400 "Ce dossier n'est pas un dossier de droit du travail" | 400 |
| Dossier workspace BELGIQUE | 400 "Outil disponible uniquement pour les dossiers FRANCE" | 400 |
| Workspace différent | 404 "Case file not found" | 404 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier travail** :
  - F-DT-15 inaptitude (utilise déjà `PREAVIS_MOIS_DEFAUT_FR=2` codé en dur — pourrait à terme consommer la nouvelle méthode `LegalReferentialService.getConventionPreavisData(...)` mais hors périmètre de cette SF).
  - F-DT-17 indemnité précarité CDD : différent (concerne CDD, pas le préavis CDI).
  - F-DT-09 comparateur indemnités : licenciement Macron uniquement, pas d'indemnité préavis.
  - F-132 rupture conventionnelle : indemnité spécifique RC, pas de préavis dans ce cadre.
  - **Aucun outil existant ne calcule l'indemnité compensatrice de préavis.**
- [x] **Autres pays** : Belgique — F-DT-25 est explicitement FR-only ; le préavis BE est calculé via l'art. 37/§2 Loi 03/07/1978 (formule Claeys → CCT 109) et serait une **feature jumelle séparée** (F-DT-25-BE à proposer au backlog si demandé). Justifié par les sources juridiques distinctes.
- [x] **Autres domaines** : non applicable — concept exclusif au droit du travail.
- [x] **Référentiel CCN** : la table `legal_referentials` héberge déjà des entrées CCN (type `CONVENTION_BAREMES` SF-129). On ajoute un nouveau type `CONVENTION_PREAVIS` (entry par IDCC, valueJson contient une matrice `{ fonction × ancienneteMois → preavisMois }`).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Calculator FR | Oui | Intégré (`IndemnitePreavisCalculator`) |
| Référentiel `CONVENTION_PREAVIS` | Oui | Migration 134 seed quelques IDCC types (IDCC_2120/IDCC_3248/IDCC_1486 = banque/métallurgie/syntec) — autres CCN seedées au fil de l'eau |
| Méthode `LegalReferentialService.getConventionPreavis(...)` | Oui | Ajoutée (DB-first, fallback null → durée légale) |
| Visibility rule F-IA-04 | Oui | ALWAYS_ON DROIT_DU_TRAVAIL FRANCE priority 53 |
| Frontend Angular | Reporté | SF-DT-25-02 (parallélisable via contrat figé ci-dessous) |
| F-DT-15 inaptitude consommant la nouvelle méthode | Hors périmètre | À refactoriser dans une SF ultérieure pour cohérence (backlog implicite) |
| Préavis BE | Hors périmètre | À traiter dans une feature séparée (F-DT-25-BE non créée à ce stade) |

### Décision

- [x] Backend intégré dans cette SF (calculator + entity + endpoint + migration + référentiel CONVENTION_PREAVIS + visibility rule)
- [x] Frontend reporté à SF-DT-25-02 (parallélisable — contrat API figé ci-dessous)
- [x] Refactor F-DT-15 → backlog implicite (pas bloquant)

---

## Nouveau pattern UI ou service partagé

Cette SF introduit un **nouveau type de référentiel** `CONVENTION_PREAVIS` consommé par une nouvelle méthode `LegalReferentialService.getConventionPreavis(code, fonction, ancienneteMois)`. Pattern aligné sur les autres méthodes du service (DB-first, fallback documenté). Pas de DTO réutilisable hors de cette feature ; pas de directive frontend introduite (la SF est backend pur).

Aucune harmonisation transverse nécessaire à ce stade — la consommation par F-DT-15 (inaptitude) est documentée comme amélioration future.

---

## Impact par domaine métier

**Sensible au domaine** : spécifique DROIT_DU_TRAVAIL FRANCE.
- **Droit du travail FR** : cœur de la SF.
- **Droit du travail BE** : non applicable (sources juridiques distinctes — Claeys/CCT 109). Feature jumelle BE à ouvrir si besoin futur.
- **Droit immigration** / **famille** : non applicable.

---

## Parité des domaines métier

Outil de **niveau 3** (calculateur). Règle de parité ≥5 ne s'applique pas. Pas d'équivalent en immigration / famille. La parité FR/BE est volontairement différée : le BE utilise une méthode de calcul radicalement différente (formule Claeys + CCT 109) qui justifie un calculateur séparé, à ouvrir comme feature jumelle au besoin.

---

## Critères d'acceptation

- [ ] **C1** : `IndemnitePreavisCalculator.compute(60, "EMPLOYE", "IDCC_2120", new BigDecimal("2500.00"), true, LocalDate.now())` avec CCN qui prévoit 3 mois retourne `dureePreavisMois=3`, `sourceDuree=CCN`, `montantIndemniteEur=7500.00`.
- [ ] **C2** : Même appel sans CCN connue (code null ou IDCC_INCONNU) → `dureePreavisMois=2` (légal ≥ 24 mois), `sourceDuree=LEGALE`, `montantIndemniteEur=5000.00`.
- [ ] **C3** : `compute(12, "EMPLOYE", null, salaire, true, …)` → `dureePreavisMois=1`, `sourceDuree=LEGALE`.
- [ ] **C4** : `compute(3, "EMPLOYE", null, salaire, true, …)` → `dureePreavisMois=0`, `sourceDuree=USAGE`, message demandant vérification.
- [ ] **C5** : Si CCN renvoie une durée < légale → durée légale conservée (principe de faveur), `sourceDuree=LEGALE`.
- [ ] **C6** : `exemptionEmployeur=false` → `montantIndemniteEur=0`, message "Indemnité non due — l'employeur n'a pas dispensé du préavis".
- [ ] **C7** : `ancienneteMois` négatif → `IllegalArgumentException`.
- [ ] **C8** : `salaireMensuelBrutEur` ≤ 0 → `IllegalArgumentException`.
- [ ] **C9** : `fonction=null` → `IllegalArgumentException`.
- [ ] **C10** : Migration 134 crée la table `indemnite_preavis_analyses` avec contraintes correctes + INSERT 3 entrées `CONVENTION_PREAVIS` + 1 visibility rule (UUID `f1a04001-0000-0000-0000-ee0000000251`, priority 53).
- [ ] **C11** : POST `/api/v1/case-files/{id}/indemnite-preavis` valide retourne 200 + persiste.
- [ ] **C12** : POST avec dossier d'un autre workspace → 404.
- [ ] **C13** : POST avec dossier DROIT_IMMIGRATION → 400.
- [ ] **C14** : GET idempotent retourne le résultat persisté.
- [ ] **C15** : POST avec dossier workspace BELGIQUE → 400 "FRANCE only".

---

## Périmètre

### Hors scope (explicite)
- **Frontend Angular** — reporté à SF-DT-25-02 (parallélisable, contrat figé).
- **Préavis BE (formule Claeys)** — feature jumelle distincte à ouvrir au backlog.
- **Refactor F-DT-15 inaptitude pour consommer le nouveau référentiel** — laissé au backlog implicite.
- **Seed exhaustif des CCN françaises** — la migration 134 amorce 3 CCN représentatives (banque, métallurgie, Syntec). Les autres seront seedées à la demande.
- **Calcul des indemnités compensatrices de congés payés sur le préavis** — couvert par F-DT-26 (séparée).

---

## Technique

### Endpoints
| Méthode | URL | Auth | Rôle minimum |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/indemnite-preavis` | OAuth2 | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/indemnite-preavis` | OAuth2 | MEMBER |

### Contrat API (figé pour SF-DT-25-02 frontend)

**Request body (POST)** :
```json
{
  "ancienneteAnnees": 5,
  "ancienneteMois": 60,
  "salaireMensuelBrutEur": 2500.00,
  "conventionCollectiveCode": "IDCC_2120",
  "fonction": "EMPLOYE",
  "exemptionEmployeur": true,
  "dateRupture": "2026-04-25"
}
```

- `fonction` enum : `OUVRIER` | `EMPLOYE` | `AGENT_MAITRISE` | `CADRE`.
- `conventionCollectiveCode` nullable.
- `ancienneteAnnees` cosmétique (la source de vérité est `ancienneteMois`).

**Response** :
```json
{
  "caseFileId": "uuid",
  "ancienneteAnnees": 5,
  "ancienneteMois": 60,
  "salaireMensuelBrutEur": 2500.00,
  "conventionCollectiveCode": "IDCC_2120",
  "fonction": "EMPLOYE",
  "exemptionEmployeur": true,
  "dateRupture": "2026-04-25",
  "dureePreavisMois": 3,
  "sourceDuree": "CCN",
  "montantIndemniteEur": 7500.00,
  "exemptionRetenue": true,
  "baseJuridique": "Art. L.1234-1 Code du travail + CCN IDCC_2120",
  "formule": "3 mois × 2 500,00 € = 7 500,00 €",
  "messages": ["..."],
  "country": "FRANCE"
}
```

- `sourceDuree` enum : `LEGALE` | `CCN` | `USAGE`.
- Codes erreur : `400` (validation), `404` (case file inconnu / autre workspace).

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `indemnite_preavis_analyses` | CREATE | nouvelle table 1:1 avec `case_files` |
| `legal_referentials` | INSERT 3 entrées `CONVENTION_PREAVIS` (FR) | seed migration 134 — IDCC_2120, IDCC_3248, IDCC_1486 |
| `decision_tool_visibility_rules` | INSERT 1 ligne | tool_id `F-DT-25-indemnite-preavis`, ALWAYS_ON, DROIT_DU_TRAVAIL, FRANCE, priority 53 |

### Migration Liquibase
- [x] Oui — `134-create-indemnite-preavis-analyses.xml` (table + seed CONVENTION_PREAVIS + visibility rule).
- Rollback : DROP TABLE + DELETE seed entries + DELETE rule.

### Composants modifiés / créés
- Backend :
  - `IndemnitePreavisCalculator.java`
  - `IndemnitePreavisFonction.java` (enum)
  - `IndemnitePreavisAnalysis.java` (entity)
  - `IndemnitePreavisRepository.java`
  - `IndemnitePreavisRequest.java` / `IndemnitePreavisResponse.java` / `IndemnitePreavisResult.java` / `IndemnitePreavisSourceDuree.java`
  - `IndemnitePreavisService.java`
  - `IndemnitePreavisController.java`
  - `LegalReferentialService` : ajout méthode `getConventionPreavis(String code, IndemnitePreavisFonction fonction, int ancienneteMois)`.
  - Migration `134-create-indemnite-preavis-analyses.xml`
- Tests :
  - `IndemnitePreavisCalculatorTest.java` (≥ 12 UT)
  - `IndemnitePreavisControllerIT.java` (≥ 8 IT)

### Contraintes de validation

| Champ | Obligatoire | Format | Valeurs autorisées | Unicité |
|-------|-------------|--------|-------------------|---------|
| `ancienneteMois` | Oui | int ≥ 0 | — | — |
| `salaireMensuelBrutEur` | Oui | BigDecimal > 0 (precision 12 scale 2) | positif | — |
| `fonction` | Oui | enum | OUVRIER, EMPLOYE, AGENT_MAITRISE, CADRE | — |
| `exemptionEmployeur` | Oui | boolean | — | — |
| `dateRupture` | Oui | LocalDate | passé/futur libre | — |
| `conventionCollectiveCode` | Non | string | normalisé via `ConventionCodeNormalizer` | — |
| `case_file_id` | — | — | — | Unique (1:1) |

---

## Plan de test

### Tests unitaires (`IndemnitePreavisCalculatorTest`)

- [ ] `compute_anciennete5ans_emp_idcc2120_returnsCcn3Mois` (CCN > légale)
- [ ] `compute_anciennete5ans_emp_sansCcn_returnsLegale2Mois`
- [ ] `compute_anciennete12mois_returnsLegale1Mois`
- [ ] `compute_anciennete3mois_returnsUsage0Mois_avecMessage`
- [ ] `compute_ccnInferieureALegale_priorisationLegale` (principe de faveur)
- [ ] `compute_exemptionFalse_returnsZero_avecMessage`
- [ ] `compute_anciennete0_returnsZero_avecMessage`
- [ ] `compute_salaireZero_throwsIllegalArgument`
- [ ] `compute_ancienneteNegative_throwsIllegalArgument`
- [ ] `compute_fonctionNull_throwsIllegalArgument`
- [ ] `compute_cadre_idccSyntec_returns3Mois` (différencie cadre vs employé)
- [ ] `compute_formule_arrondiCentime_HALF_UP`

### Tests d'intégration (`IndemnitePreavisControllerIT`)

- [ ] `POST_nominal_persists_and_returns200`
- [ ] `POST_sansCcn_returnsLegale`
- [ ] `POST_exemptionFalse_returnsZero`
- [ ] `POST_upsertReplacesExistingAnalysis`
- [ ] `POST_otherWorkspace_returns404`
- [ ] `POST_immigrationCaseFile_returns400`
- [ ] `POST_belgiumWorkspace_returns400`
- [ ] `GET_afterPost_returnsPersistedAnalysis`
- [ ] `GET_withoutPriorPost_returns404`

### Isolation workspace

- [x] Applicable — couvert par `POST_otherWorkspace_returns404` (filtrage `findByUserAndPrimaryTrue`).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context (lecture seule, pattern standard)
- [ ] Plans / limites
- [ ] Navigation / routing
- [x] **F-IA-04 visibility rule** — ajout 1 règle (déclaratif).
- [x] **Outil décisionnel métier** — création nouveau calculateur, scan effectué (cf. Analyse de cohérence ci-dessus).
- [x] **Référentiel statique vs DB** — nouvelle entrée seedée en DB (CONVENTION_PREAVIS) sans fallback Java statique → service retourne null si absente, calculator bascule sur durée légale (comportement explicite).

### Composants / endpoints impactés

- `LegalReferentialService` : ajout méthode `getConventionPreavis(...)` — pas d'impact sur les méthodes existantes.
- `DecisionToolVisibilityService` : ajout d'une règle déclarative — aucune modification du moteur.
- Tests existants F-DT-15 inaptitude : non impactés (le préavis FR par défaut reste codé en dur — refacto ultérieure).

### Smoke tests E2E concernés

- [x] Aucun — endpoint isolé, pas de modification de l'auth/routing/workspace.

---

## Dépendances

### Subfeatures bloquantes
- SF-140-03 (description obligatoire pour entries `is_system=true`) — respectée.
- SF-IA-04-01 (moteur visibility) — done, on s'y branche.
- F-129 / SF-129-01 (normalisation des codes CCN) — utilisée via `ConventionCodeNormalizer.normalize(...)`.

### Questions ouvertes impactées
Aucune.

---

## Notes et décisions

- **Décision 1** : la matrice CCN `(fonction × ancienneteMois → preavisMois)` est stockée en JSON dans `legal_referentials.value_json` sous la forme :
  ```json
  {
    "fonctions": {
      "OUVRIER":      [{"min": 0, "max": 6, "mois": 0}, {"min": 6, "max": 24, "mois": 1}, {"min": 24, "max": null, "mois": 2}],
      "EMPLOYE":      [...],
      "AGENT_MAITRISE": [...],
      "CADRE":        [{"min": 0, "max": 6, "mois": 1}, {"min": 6, "max": null, "mois": 3}]
    },
    "article": "CCN art. 27"
  }
  ```
  Le calcul retient la première tranche dont `[min, max[` couvre l'ancienneté en mois (`max=null` = +∞).
- **Décision 2** : la durée retenue = `max(durée légale, durée CCN)` — principe de faveur. Si la CCN renvoie une durée inférieure (très rare), la durée légale prime et `sourceDuree=LEGALE`.
- **Décision 3** : `USAGE` n'est pas un fallback documenté en DB — c'est juste une étiquette retournée quand `< 6 mois` et CCN absente, accompagnée d'un message demandant à l'avocat de vérifier l'usage local. La durée retournée est 0.
- **Décision 4** : si `exemptionEmployeur=false`, on continue à retourner la durée du préavis (c'est une information utile pour l'avocat) mais `montantIndemniteEur=0` car le salarié reçoit son salaire normal pendant le préavis.
- **Décision 5** : le seed CCN initial couvre 3 IDCC : IDCC_2120 (banque), IDCC_3248 (métallurgie), IDCC_1486 (Syntec). Les autres CCN seront seedées dans des migrations ultérieures à la demande.
- **Décision 6** : pas de fallback Java statique (`*Referentiel.java`) pour `CONVENTION_PREAVIS` — la DB est la seule source. Si l'IDCC n'est pas en DB, on retourne null et le calcul retombe sur la durée légale. Cela évite la dette de convergence Java/DB.
- **Pas de prefill IA dans cette SF** — le prefill sera couvert par SF-DT-25-02 (frontend) avec passage de `aiData` dans le `TOOL_REGISTRY` (pattern F-IA-03 + F-IA-04).
