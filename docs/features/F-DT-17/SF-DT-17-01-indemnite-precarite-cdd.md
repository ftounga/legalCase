# Mini-spec — F-DT-17 / SF-DT-17-01 Calculateur indemnité de précarité CDD

## Identifiant
`F-DT-17 / SF-DT-17-01`

## Feature parente
`F-DT-17` — Indemnité précarité CDD (art. L.1243-8 Code du travail)

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-DT-17-01-indemnite-precarite-cdd`

---

## Objectif

Fournir un outil décisionnel dédié au calcul de l'indemnité de fin de contrat ("prime de précarité") due au salarié en CDD à l'expiration du contrat, selon l'art. L.1243-8 Code du travail — 10 % des salaires bruts (ou 6 % par convention, art. L.1243-9) — avec gestion des 6 cas d'exclusion de l'art. L.1243-10.

---

## Comportement attendu

### Cas nominal

**Calcul standard :**
- Entrées : `totalSalairesBruts` (€, somme de toute la rémunération brute perçue pendant le CDD), `tauxPrecarite` (10 ou 6 — 10 par défaut), `casExclusion` (optionnel).
- Sortie : `indemnitePrecarite = totalSalairesBruts × tauxPrecarite / 100`, arrondie au centime.
- Formule affichée : `10 % × 18 500 € = 1 850,00 €`.
- Base juridique : `Art. L.1243-8 Code du travail`.

**Taux réduit 6 % (L.1243-9) :**
- Applicable si la CCN étendue prévoit des contreparties (typiquement une formation). L'avocat sélectionne `tauxPrecarite=6` manuellement.
- Message : "Taux réduit 6 % applicable uniquement si la CCN prévoit des contreparties formelles (art. L.1243-9)."

**Exclusions L.1243-10 :**
| Code | Description | Résultat |
|---|---|---|
| `CDD_ETUDIANT_VACANCES` | CDD conclu avec jeune pour vacances scolaires/univ. | indemnité = 0 |
| `CDD_SAISONNIER` | CDD saisonnier | indemnité = 0 |
| `CDD_USAGE` | CDD d'usage (L.1242-2 3°) | indemnité = 0 |
| `CDI_REFUSE_PAR_SALARIE` | CDI pour même emploi refusé par le salarié | indemnité = 0 |
| `RUPTURE_ANTICIPEE_SALARIE` | Rupture anticipée à l'initiative du salarié | indemnité = 0 |
| `RUPTURE_ANTICIPEE_FAUTE_GRAVE` | Rupture anticipée pour faute grave ou force majeure | indemnité = 0 |

Quand un `casExclusion` est fourni : `indemnitePrecarite = 0`, formule neutre, message explicatif citant la base légale (L.1243-10).

### Cas d'erreur
| Situation | Comportement | Code HTTP |
|---|---|---|
| `totalSalairesBruts` nul ou négatif | 400 "Total des salaires bruts requis et positif" | 400 |
| `tauxPrecarite` autre que 6 ou 10 | 400 "Taux applicable : 10 % (standard) ou 6 % (convention)" | 400 |
| `casExclusion` non reconnu | 400 message enum | 400 |
| Dossier d'un autre domaine (FAMILLE/IMMIGRATION) | 400 "Ce dossier n'est pas un dossier de droit du travail" | 400 |
| Workspace différent | 404 "Case file not found" | 404 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier travail** : F-DT-01 indemnités licenciement (différent — concerne licenciement, pas CDD), F-DT-07 ancienneté (utilise un concept similaire de salaires mais pour ancienneté), F-DT-09 comparateur indemnités (licenciement uniquement), F-DT-10 / F-132 rupture conventionnelle (différent). **Aucun outil existant ne couvre CDD.**
- [x] **Autres pays** : Belgique — **pas d'équivalent standard**. Le CDD belge (loi 03/07/1978) ne prévoit pas d'indemnité de précarité forfaitaire. Les seules indemnités applicables sont l'indemnité de rupture anticipée (art. 40 de la loi). → outil France uniquement, justifié.
- [x] **Autres domaines** : non applicable.
- [x] **Feature jumelle** : F-DT-18 (Indemnité fin mission intérim) — pattern identique, traitement dédié ~1 SF à suivre. Ne pas fusionner : situations métier distinctes (CDD ≠ intérim), invariant "un outil = une situation".

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Création outil CDD précarité | Oui | Intégré dans cette SF |
| Outil jumeau intérim (F-DT-18) | Feature séparée | À implémenter dans une SF propre (pattern jumeau, pas de factorisation anticipée) |
| Affichage panel F-IA-04 | Oui mais non bloquant | Ajout règle `decision_tool_visibility_rules` → F-IA-04 pattern ALWAYS_ON pour DROIT_DU_TRAVAIL+FRANCE. Voir "Analyse d'impact" |
| Frontend Angular | Reporté à SF-DT-17-02 | Pour limiter cette SF à 1 journée (quick) |

### Décision

- [x] Backend intégré dans cette SF (calculator + entity + endpoint + migration + visibility rule)
- [x] Frontend reporté à SF-DT-17-02 (backlog V7 si souhaité — mais le spec dit "~1 SF" backend-only suffit pour un MVP)
- [x] F-DT-18 (jumeau intérim) — feature séparée au backlog

---

## Impact par domaine métier

**Sensible au domaine** : spécifique DROIT_DU_TRAVAIL FRANCE.
- **Droit du travail FR** : cœur de la SF.
- **Droit du travail BE** : non applicable — pas d'équivalent standard, l'art. 40 loi 03/07/1978 couvre seulement la rupture anticipée (différent).
- **Droit immigration** / **famille** : non applicable.

Asymétrie FR ↔ BE justifiée par le droit matériel (la prime de précarité est une spécificité française). Feature BE jumelle non nécessaire.

---

## Parité des domaines métier

Outil de **niveau 3** (calculateur) — règle de parité niveau ≥5 ne s'applique pas. Pas d'équivalent pertinent en Immigration ou Famille (la prime de précarité est un concept propre à la relation de travail en CDD).

---

## Critères d'acceptation

- [ ] **C1** : `IndemnitePrecariteCddCalculator.compute(18500.00, 10, null)` retourne `indemnite=1850.00`, formule `"10 % × 18 500,00 € = 1 850,00 €"`.
- [ ] **C2** : `compute(18500.00, 6, null)` retourne `indemnite=1110.00` avec message taux réduit.
- [ ] **C3** : `compute(18500.00, 10, "CDD_ETUDIANT_VACANCES")` retourne `indemnite=0` et message citant L.1243-10 1°.
- [ ] **C4** : chaque code d'exclusion (6 codes) produit `indemnite=0` avec un message spécifique.
- [ ] **C5** : `compute(0.00, 10, null)` ou salaires négatifs → `IllegalArgumentException`.
- [ ] **C6** : `compute(18500, 15, null)` (taux inconnu) → `IllegalArgumentException`.
- [ ] **C7** : Migration Liquibase 109 crée la table `cdd_indemnite_precarite_analyses` avec colonnes `case_file_id` UNIQUE + `total_salaires_bruts` + `taux_precarite` + `cas_exclusion` (nullable) + `result_data` + timestamps.
- [ ] **C8** : `POST /api/v1/case-files/{id}/cdd-indemnite-precarite` valide, retourne 200 + résultat, persiste en base.
- [ ] **C9** : `GET` idempotent retourne le résultat persisté.
- [ ] **C10** : `POST` avec dossier d'un autre workspace → 404.
- [ ] **C11** : `POST` avec dossier DROIT_IMMIGRATION → 400.
- [ ] **C12** : Règle `decision_tool_visibility_rules` ajoutée (ALWAYS_ON, DROIT_DU_TRAVAIL, FRANCE) pour que l'outil s'affiche dans le panel F-IA-04.

---

## Périmètre

### Hors scope (explicite)
- **Frontend Angular** — reporté à SF-DT-17-02 (backlog). Backend prêt pour consommation.
- **F-DT-18 Indemnité fin mission intérim** — feature jumelle distincte (pattern à reproduire sans factorisation prématurée).
- **Intégration dans la synthèse IA CaseAnalysis** — le calcul reste manuel avocat pour l'instant (à améliorer dans une SF ultérieure avec prefill IA des salaires).
- **Gestion des CDD successifs / chaîne de CDD** — hors périmètre (chaque contrat est calculé indépendamment).
- **Requalification du CDD en CDI** — hors périmètre (impact sur le montant mais calculé ailleurs ; ici on calcule la précarité due à la fin normale du CDD).

---

## Technique

### Endpoints
| Méthode | URL | Auth | Rôle minimum |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/cdd-indemnite-precarite` | OAuth2 | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/cdd-indemnite-precarite` | OAuth2 | MEMBER |

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `cdd_indemnite_precarite_analyses` | CREATE | nouvelle table 1:1 avec `case_files` |
| `decision_tool_visibility_rules` | INSERT 1 ligne | pour F-IA-04 panel — tool_id `F-DT-17-cdd-indemnite-precarite`, layer ALWAYS_ON, legal_domain DROIT_DU_TRAVAIL, country FRANCE |

### Migration Liquibase
- [x] Oui — `109-create-cdd-indemnite-precarite-analyses.xml` (+ seed visibility rule dans le même changelog)
- Rollback : DROP TABLE + DELETE rule.

### Composants modifiés / créés
- Backend :
  - `IndemnitePrecariteCddCalculator.java` (statique)
  - `IndemnitePrecariteCddAnalysis.java` (entity)
  - `IndemnitePrecariteCddRepository.java`
  - `IndemnitePrecariteCddRequest.java` / `IndemnitePrecariteCddResponse.java` / `IndemnitePrecariteCddResult.java`
  - `IndemnitePrecariteCddService.java`
  - `IndemnitePrecariteCddController.java`
  - Migration `109-*.xml`
- Tests :
  - `IndemnitePrecariteCddCalculatorTest.java` (UT, ~8 tests)
  - `IndemnitePrecariteCddControllerIT.java` (IT, ~4 tests : POST nominal, exclusion, GET, workspace isolation)

### Contraintes de validation

| Champ | Obligatoire | Format | Valeurs autorisées | Unicité |
|-------|-------------|--------|-------------------|---------|
| `totalSalairesBruts` | Oui | BigDecimal > 0, precision 12 scale 2 | positif | — |
| `tauxPrecarite` | Non (default 10) | int | 6 ou 10 | — |
| `casExclusion` | Non | string | `CDD_ETUDIANT_VACANCES`, `CDD_SAISONNIER`, `CDD_USAGE`, `CDI_REFUSE_PAR_SALARIE`, `RUPTURE_ANTICIPEE_SALARIE`, `RUPTURE_ANTICIPEE_FAUTE_GRAVE` | — |
| `case_file_id` | — | — | — | Unique (1:1) |

---

## Plan de test

### Tests unitaires (IndemnitePrecariteCddCalculatorTest)

- [ ] `compute_nominal_10pct_returnsCorrectAmount`
- [ ] `compute_nominal_6pct_returnsCorrectAmount_withReducedRateMessage`
- [ ] `compute_allSixExclusions_returnZeroWithArticleCitation` (parameterized)
- [ ] `compute_negativeSalaires_throwsIllegalArgument`
- [ ] `compute_zeroSalaires_throwsIllegalArgument`
- [ ] `compute_invalidRate_throwsIllegalArgument`
- [ ] `compute_unknownExclusionCode_throwsIllegalArgument`
- [ ] `compute_formula_rounding_at2Decimals`

### Tests d'intégration (IndemnitePrecariteCddControllerIT)

- [ ] `POST_nominal_persists_and_returns200`
- [ ] `POST_withExclusion_returnsZero`
- [ ] `POST_upsertReplacesExistingAnalysis`
- [ ] `POST_otherWorkspace_returns404`
- [ ] `GET_afterPost_returns200`
- [ ] `GET_withoutPriorPost_returns404`
- [ ] `POST_immigrationCaseFile_returns400`

### Isolation workspace

- [x] Applicable — test `POST_otherWorkspace_returns404` couvre le filtrage `findByUserAndPrimaryTrue`.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context (lecture seule, pattern standard)
- [ ] Plans / limites
- [ ] Navigation / routing
- [x] **F-IA-04 visibility rule** — ajout d'une règle (pas une modification du moteur)

### Composants / endpoints impactés

- `DecisionToolVisibilityService` : ajout d'une règle déclarative (migration seed). Aucun changement de code.
- Aucun impact régression sur les autres outils décisionnels.

### Smoke tests E2E concernés

- [x] Aucun — pas de changement d'auth / workspace / navigation / plans. Le nouvel endpoint est un ajout isolé.

---

## Dépendances

### Subfeatures bloquantes
- SF-140-03 (description obligatoire) — respectée.
- SF-IA-04-01 (moteur visibility) — done, on s'y branche via visibility rule.

### Questions ouvertes impactées
Aucune.

---

## Notes et décisions

- **Separation d'avec F-DT-18** : pas de factorisation préfatigante. Les deux calculateurs auront un code très similaire (10 % des salaires, cas d'exclusion) mais des bases légales et situations métier distinctes (CDD vs intérim). Invariant "un outil = une situation". Si une bibliothèque commune émerge après F-DT-18, on factorisera à ce moment-là.
- **Pas de prefill IA** : dans cette SF, le totalSalairesBruts reste saisi par l'avocat. Un prefill automatique depuis la synthèse IA est possible mais ajouterait une boucle d'aller-retour sur les tests → reporté.
- **Taux par défaut 10 %** : raisonnable car c'est la règle générale. Le 6 % est l'exception.
- **Exclusion `CDI_REFUSE_PAR_SALARIE`** : subtile — nécessite que l'employeur ait formellement proposé un CDI pour le même emploi, avec les mêmes conditions. La formalisation du refus doit être prouvable. L'outil présume que l'avocat a vérifié.
- **Alignement avec F-DT-01** : F-DT-01 couvre l'indemnité de licenciement, F-DT-17 couvre l'indemnité de précarité CDD. Ce sont deux contextes distincts (licenciement vs fin de CDD). Pas de chevauchement.
