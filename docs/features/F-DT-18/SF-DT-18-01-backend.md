# Mini-spec — F-DT-18 / SF-DT-18-01 Backend Indemnité de fin de mission intérim

## Identifiant
`F-DT-18 / SF-DT-18-01`

## Feature parente
`F-DT-18` — Indemnité de fin de mission intérim (art. L.1251-32 Code du travail)

## Statut `draft` · Date `2026-04-25` · Branche `feat/SF-DT-18-01-fin-mission-interim-backend`

---

## Objectif

Fournir un outil décisionnel dédié au calcul de l'indemnité de fin de mission ("IFM") due au salarié intérimaire à l'issue de chaque mission de travail temporaire — art. L.1251-32 Code du travail (10 % de la rémunération totale brute perçue). Pattern miroir strict de F-DT-17 (indemnité précarité CDD), avec une situation métier distincte (intérim ≠ CDD) et un jeu d'exclusions propre à l'intérim.

---

## Comportement attendu

### Cas nominal

**Calcul standard (taux 10 %) :**
- Entrées : `totalRemunerationsBrutesEur` (€, somme de toute la rémunération brute perçue pendant la mission), `dureeMissionJours` (jours calendaires de la mission, > 0), `dateFinMission` (ISO date), `motifExclusion` (optionnel).
- Sortie : `montantIndemniteEur = totalRemunerationsBrutesEur × 10 / 100`, arrondie au centime (HALF_UP).
- Formule affichée : `12 000,00 € × 10 % = 1 200,00 €`.
- Base juridique : `Art. L.1251-32 Code du travail`.

**Différence avec F-DT-17 :** pas de taux 6 %. À l'intérim, le taux 10 % est figé par la loi — il n'existe pas d'équivalent CCN avec contrepartie permettant un taux réduit (contrairement au CDD via L.1243-9). On expose donc un seul taux côté API : `tauxApplique = 0.10`.

**Cas d'exclusion (motifExclusion non null) :**
| Code | Description | Résultat |
|---|---|---|
| `CONTRAT_INDETERMINEE_PROPOSE` | Mission débouchant sur un CDI proposé par l'entreprise utilisatrice (L.1251-32 al. 2) | indemnité = 0 |
| `RUPTURE_ANTICIPEE_SALARIE` | Rupture anticipée à l'initiative du salarié intérimaire | indemnité = 0 |
| `FAUTE_GRAVE` | Rupture anticipée pour faute grave de l'intérimaire | indemnité = 0 |
| `FORCE_MAJEURE` | Rupture anticipée pour force majeure | indemnité = 0 |
| `MISSION_PEPINIERE_QUALIFIANTE` | Contrat dans le cadre d'une mission "pépinière" / formation qualifiante (L.1251-33) | indemnité = 0 |
| `INTERIMAIRE_REFUS_PROPOSITION_CDI` | L'intérimaire refuse un CDI à conditions équivalentes | indemnité = 0 |

Quand un `motifExclusion` est fourni : `montantIndemniteEur = 0`, `exclusionRetenue = true`, formule neutre, message explicatif citant la base légale (L.1251-32 / L.1251-33).

### Cas d'erreur
| Situation | Comportement | Code HTTP |
|---|---|---|
| `totalRemunerationsBrutesEur` nul, négatif ou absent | 400 "Total des rémunérations brutes requis et strictement positif" | 400 |
| `dureeMissionJours` ≤ 0 ou absent | 400 "Durée de la mission requise et strictement positive" | 400 |
| `motifExclusion` non reconnu | 400 message enum | 400 |
| Dossier d'un autre domaine (FAMILLE/IMMIGRATION) | 400 "Ce dossier n'est pas un dossier de droit du travail" | 400 |
| Workspace différent | 404 "Case file not found" | 404 |
| GET sans POST préalable | 404 "Aucune analyse d'indemnité de fin de mission intérim trouvée pour ce dossier" | 404 |

---

## Contrat API (figé pour SF-DT-18-02 frontend en parallèle)

### Endpoint
| Méthode | URL |
|---|---|
| POST | `/api/v1/case-files/{caseFileId}/fin-mission-interim` |
| GET  | `/api/v1/case-files/{caseFileId}/fin-mission-interim` |

### Auth
- OAuth2/OIDC (Google ou Microsoft).
- Workspace primaire de l'utilisateur doit posséder le `caseFile`.
- `legal_domain` du dossier = `DROIT_DU_TRAVAIL`.

### Request body (POST)
```json
{
  "totalRemunerationsBrutesEur": 12000.00,
  "dureeMissionJours": 90,
  "motifExclusion": null,
  "dateFinMission": "2026-04-25"
}
```

| Champ | Obligatoire | Type | Validation |
|---|---|---|---|
| `totalRemunerationsBrutesEur` | Oui | BigDecimal | > 0, precision 12 scale 2 |
| `dureeMissionJours` | Oui | int | > 0 |
| `dateFinMission` | Non | LocalDate (ISO) | format ISO `YYYY-MM-DD` |
| `motifExclusion` | Non (nullable) | string enum | Valeurs autorisées ci-dessous |

### Enum `motifExclusion`
- `CONTRAT_INDETERMINEE_PROPOSE`
- `RUPTURE_ANTICIPEE_SALARIE`
- `FAUTE_GRAVE`
- `FORCE_MAJEURE`
- `MISSION_PEPINIERE_QUALIFIANTE`
- `INTERIMAIRE_REFUS_PROPOSITION_CDI`

### Response body (POST + GET)
```json
{
  "caseFileId": "uuid",
  "totalRemunerationsBrutesEur": 12000.00,
  "dureeMissionJours": 90,
  "motifExclusion": null,
  "dateFinMission": "2026-04-25",
  "tauxApplique": 0.10,
  "montantIndemniteEur": 1200.00,
  "exclusionRetenue": false,
  "baseJuridique": "Art. L.1251-32 Code du travail",
  "formule": "12 000,00 € × 10 % = 1 200,00 €",
  "messages": ["Indemnité de fin de mission due à l'issue de la mission d'intérim, sauf cas d'exclusion."],
  "country": "FRANCE"
}
```

### Codes erreur
- `400 Bad Request` — champs invalides ou domaine non DROIT_DU_TRAVAIL.
- `404 Not Found` — dossier inexistant ou hors workspace ; GET sans analyse persistée.
- `401 Unauthorized` — non authentifié (Spring Security par défaut).

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier travail FR** :
  - F-DT-17 indemnité précarité CDD — **pattern miroir strict**, situation métier distincte (CDD ≠ intérim). Code dupliqué volontairement (invariant "un outil = une situation", pas de factorisation prématurée).
  - F-DT-19 heures sup — concerne autre situation, pas d'overlap.
  - F-DT-21 travail dissimulé — concerne situation distincte (sanction).
  - F-132 rupture conventionnelle — situation distincte.
- [x] **Belgique** : pas d'équivalent. Le travail intérimaire belge (loi 24/07/1987) ne prévoit pas d'indemnité forfaitaire de fin de mission. → outil France uniquement, justifié.
- [x] **Autres domaines (Immigration / Famille)** : non applicable.
- [x] **Frontend** : SF-DT-18-02 parallèle (pattern visuel `indemnite-precarite-cdd-section`). Contrat API figé ci-dessus.
- [x] **Pattern partagé** : aucun nouveau service / composant transversal créé. Calculator statique réutilise le pattern existant (BigDecimal + RoundingMode.HALF_UP + formatMontant fr-FR).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Création outil intérim | Oui | Intégré dans cette SF |
| Frontend Angular | SF-DT-18-02 parallèle | Contrat API figé ci-dessus |
| F-DT-17 jumeau CDD | Déjà livré, distinct | Pas de factorisation |
| Affichage panel F-IA-04 | Oui | Migration 132 ajoute règle `decision_tool_visibility_rules` ALWAYS_ON FRANCE DROIT_DU_TRAVAIL priority 51 |
| Belgique | Non applicable | Justification matérielle |

### Décision
- [x] Backend intégré dans cette SF (calculator + entity + endpoint + migration + visibility rule)
- [x] Frontend SF-DT-18-02 parallèle (mini-spec dédiée référence ce contrat API)

---

## Impact par domaine métier

**Sensible au domaine** : spécifique DROIT_DU_TRAVAIL FRANCE.
- **Droit du travail FR** : cœur de la SF.
- **Droit du travail BE** : non applicable — pas d'équivalent forfaitaire pour le travail temporaire en Belgique.
- **Droit immigration** / **Droit famille** : non applicable.

Asymétrie FR ↔ BE justifiée par le droit matériel (la prime de fin de mission est une spécificité française).

---

## Parité des domaines métier

Outil de **niveau 3** (calculateur) — règle de parité niveau ≥ 5 ne s'applique pas. Pas d'équivalent pertinent en Immigration ou Famille (concept propre à la relation de travail temporaire).

---

## Critères d'acceptation

- [ ] **C1** : `IndemniteFinMissionInterimCalculator.compute(12000.00, 90, null)` retourne `montant=1200.00`, `tauxApplique=0.10`, formule `"12 000,00 € × 10 % = 1 200,00 €"`.
- [ ] **C2** : Chacun des 6 codes d'exclusion produit `montant=0.00`, `exclusionRetenue=true`, message citant L.1251-32 ou L.1251-33.
- [ ] **C3** : `compute(0, 90, null)` ou rémunérations négatives → `IllegalArgumentException`.
- [ ] **C4** : `dureeMissionJours <= 0` → `IllegalArgumentException`.
- [ ] **C5** : `motifExclusion` non reconnu → `IllegalArgumentException`.
- [ ] **C6** : Migration Liquibase 132 crée la table `indemnite_fin_mission_interim_analyses` (id, case_file_id UNIQUE, total_remunerations_brutes_eur, duree_mission_jours, motif_exclusion nullable, date_fin_mission nullable, result_data, timestamps).
- [ ] **C7** : Migration 132 INSERT `decision_tool_visibility_rules` ALWAYS_ON FRANCE DROIT_DU_TRAVAIL priority 51 UUID `f1a04001-0000-0000-0000-ee0000000181` (hex pur, conforme convention F-DT-11 `ee0000000111` / F-DT-27 `ee0000000271` ; le `dt` de l'intention initiale n'est pas valide en hex UUID).
- [ ] **C8** : `POST /api/v1/case-files/{id}/fin-mission-interim` valide retourne 200 + résultat, persiste en base, idempotent (upsert 1:1 par `case_file_id`).
- [ ] **C9** : `GET` idempotent retourne le résultat persisté.
- [ ] **C10** : `POST` avec dossier d'un autre workspace → 404.
- [ ] **C11** : `POST` avec dossier DROIT_IMMIGRATION → 400.
- [ ] **C12** : Arrondi HALF_UP au centime sur le résultat.

---

## Périmètre

### Hors scope (explicite)
- **Frontend Angular** — SF-DT-18-02 parallèle.
- **F-DT-17 (CDD)** — déjà livré.
- **Successions de missions / requalification intérim → CDI** — F-DT-23 dédiée.
- **Pré-fill IA** — réservé à la SF frontend (`indemnite-precarite-cdd-section` pattern).
- **Belgique intérim** — pas d'équivalent légal.

---

## Technique

### Endpoints
| Méthode | URL | Auth | Rôle minimum |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/fin-mission-interim` | OAuth2 | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/fin-mission-interim` | OAuth2 | MEMBER |

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `indemnite_fin_mission_interim_analyses` | CREATE | nouvelle table 1:1 avec `case_files` |
| `decision_tool_visibility_rules` | INSERT 1 ligne | UUID `f1a04001-0000-0000-0000-ee0000000181` (hex pur, convention F-DT-11 / F-DT-27), tool_id `F-DT-18-fin-mission-interim`, ALWAYS_ON, FRANCE, DROIT_DU_TRAVAIL, priority 51 |

### Migration Liquibase
- [x] Oui — `132-create-indemnite-fin-mission-interim-analyses.xml` (+ seed visibility rule dans le même changelog)
- Rollback : DROP TABLE + DELETE rule.

### Composants modifiés / créés
- Backend :
  - `IndemniteFinMissionInterimCalculator.java` (statique)
  - `IndemniteFinMissionInterimAnalysis.java` (entity)
  - `IndemniteFinMissionInterimRepository.java`
  - `IndemniteFinMissionInterimRequest.java` / `IndemniteFinMissionInterimResponse.java` / `IndemniteFinMissionInterimResult.java`
  - `IndemniteFinMissionInterimService.java`
  - `IndemniteFinMissionInterimController.java`
  - Migration `132-create-indemnite-fin-mission-interim-analyses.xml`
- Tests :
  - `IndemniteFinMissionInterimCalculatorTest.java` (UT, ≥ 12 tests)
  - `IndemniteFinMissionInterimControllerIT.java` (IT, ≥ 8 tests)

---

## Plan de test

### Tests unitaires (`IndemniteFinMissionInterimCalculatorTest`)

- [ ] `compute_nominal_returnsTenPct`
- [ ] `compute_smallAmount_rounding`
- [ ] `compute_amountWithRounding_halfUp` (arrondi HALF_UP)
- [ ] `compute_largeAmount_returnsCorrect`
- [ ] `compute_allSixExclusions_returnZero` (parameterized x 6)
- [ ] `compute_negativeAmount_throws`
- [ ] `compute_zeroAmount_throws`
- [ ] `compute_nullAmount_throws`
- [ ] `compute_zeroDuree_throws`
- [ ] `compute_negativeDuree_throws`
- [ ] `compute_unknownExclusion_throws`
- [ ] `compute_messageContainsBaseLegale`

### Tests d'intégration (`IndemniteFinMissionInterimControllerIT`)

- [ ] `POST_nominal_persists_and_returns200`
- [ ] `POST_withExclusion_returnsZero`
- [ ] `POST_upsertReplacesExistingAnalysis`
- [ ] `POST_amountZero_returns400`
- [ ] `POST_dureeZero_returns400`
- [ ] `POST_otherWorkspace_returns404`
- [ ] `POST_immigrationCaseFile_returns400`
- [ ] `GET_afterPost_returnsPersistedAnalysis`
- [ ] `GET_withoutPost_returns404`

### Isolation workspace
- [x] Applicable — test `POST_otherWorkspace_returns404` couvre le filtrage `findByUserAndPrimaryTrue`.

---

## Analyse d'impact

### Préoccupations transversales touchées
- [ ] Auth / Principal — pas de changement, pattern OidcUser standard.
- [ ] Workspace context — lecture seule, pattern standard.
- [ ] Plans / limites — aucun gate appliqué (pattern F-DT-17).
- [ ] Navigation / routing — endpoint isolé, aucun impact.
- [x] **Outil décisionnel métier** — création d'un nouvel outil. Scan effectué (cf. cohérence transversale). Aucun outil existant ne couvre l'intérim. Invariant "un outil = une situation" respecté (pattern jumeau distinct de F-DT-17).
- [x] **F-IA-04 visibility rule** — ajout déclaratif d'une règle (pas de modification du moteur).

### Composants / endpoints impactés
- `DecisionToolVisibilityService` : ajout d'une règle déclarative (migration seed). Aucun changement de code.
- Aucun impact régression sur les autres outils décisionnels.

### Smoke tests E2E concernés
- Aucun — pas de changement d'auth / workspace / navigation / plans. Le nouvel endpoint est un ajout isolé.

---

## Dépendances

### Subfeatures bloquantes
- SF-140-03 (description obligatoire `legal_referentials`) — non applicable, on n'INSERT pas dans `legal_referentials`.
- SF-IA-04-01 (moteur visibility) — done, on s'y branche via visibility rule.

### Questions ouvertes impactées
Aucune.

---

## Notes et décisions

- **Pas de factorisation avec F-DT-17** : invariant "un outil = une situation" — code volontairement dupliqué pour préserver l'évolution indépendante des deux régimes (CDD vs intérim).
- **Pas de taux 6 %** : différence métier explicite avec F-DT-17. À l'intérim, l'art. L.1251-32 fige le taux à 10 % sans alternative CCN.
- **`tauxApplique = 0.10` (BigDecimal)** : exposé dans la réponse pour cohérence avec un éventuel futur affichage frontend. Format décimal (0.10) plutôt qu'entier (10) pour cohérence visuelle avec un taux IFM légal.
- **`exclusionRetenue` (boolean)** : champ dérivé de `motifExclusion != null` — explicité dans la réponse pour faciliter le rendu frontend.
- **`country`** : toujours `"FRANCE"` (FR-only). Champ ajouté pour traçabilité et alignement avec le contrat figé attendu côté SF-DT-18-02 frontend.
- **Migration 132** : 130 et 131 réservés (130 pris par seed travail-procedure-jalons, 131 réservé F-DT-19 hot-fix éventuel). Dossier vérifié — pas de collision UUID.
