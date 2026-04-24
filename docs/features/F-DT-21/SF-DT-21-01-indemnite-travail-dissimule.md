# Mini-spec — F-DT-21 / SF-DT-21-01 Indemnité forfaitaire pour travail dissimulé

## Identifiant
`F-DT-21 / SF-DT-21-01`

## Feature parente
`F-DT-21` — Travail dissimulé (art. L.8223-1 Code du travail)

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-DT-21-01-indemnite-travail-dissimule`

---

## Objectif

Fournir un outil décisionnel dédié au calcul de l'indemnité forfaitaire pour travail dissimulé (art. L.8223-1 Code du travail) : **6 mois de salaire** due au salarié en cas de rupture de la relation de travail lorsque l'employeur a méconnu L.8221-3 (non-déclaration URSSAF) ou L.8221-5 (dissimulation d'heures). L'outil rappelle également les règles de cumul jurisprudentiel avec les autres indemnités.

---

## Comportement attendu

### Cas nominal

- Entrée : `salaireMensuelReference` (BigDecimal, > 0) — le salaire mensuel de référence retenu par l'avocat (salaire moyen des 12 derniers mois, ou des 3 derniers mois si plus favorable, selon jurisprudence).
- Sortie : `indemniteForfaitaire = 6 × salaireMensuelReference`
- Formule : `6 mois × 2 500,00 € = 15 000,00 €`
- Base juridique : `Art. L.8223-1 Code du travail`
- Messages :
  - "Indemnité forfaitaire cumulable avec les indemnités de rupture (licenciement, préavis, congés payés) selon la jurisprudence constante de la Cour de cassation (Cass. soc., ch. mixte, 26 mars 2010)."
  - "Condition d'application : rupture de la relation de travail ET infraction caractérisée (intention de l'employeur, méconnaissance L.8221-3 ou L.8221-5)."
  - "Non cumulable avec l'indemnité forfaitaire pour défaut de visite médicale (art. L.4624-1)."

### Cas d'erreur
| Situation | Comportement | Code HTTP |
|---|---|---|
| `salaireMensuelReference` nul ou négatif | 400 "Salaire mensuel de référence requis et strictement positif" | 400 |
| Dossier d'un autre domaine | 400 "Ce dossier n'est pas un dossier de droit du travail" | 400 |
| Workspace différent | 404 | 404 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils travail** : F-DT-01 (licenciement), F-DT-09 (comparateur indemnités), F-DT-17 (précarité CDD), F-132 (rupture conv). Aucun ne couvre le travail dissimulé. Situation métier propre.
- [x] **Pays** : Belgique — l'art. 53 de la loi-programme du 27/12/2006 et la loi Sociale BE prévoient des sanctions pénales/administratives pour le travail non déclaré, mais sans indemnité forfaitaire de 6 mois équivalente. Pas de jumeau BE dans cette SF.
- [x] **Pattern** : Réutilisation exacte du squelette F-DT-17 (calculator statique + entity 1:1 + service + controller + migration). Pas de nouveau pattern UI/service partagé.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Création outil travail dissimulé | Oui | Intégré |
| F-IA-04 visibility rule | Oui | Règle ALWAYS_ON DROIT_DU_TRAVAIL/FRANCE ajoutée |
| Frontend | Reporté | SF-DT-21-02 (backlog) |
| BE jumeau | Non applicable | Pas d'équivalent en droit belge |

### Décision

- [x] Backend complet dans cette SF
- [x] Frontend reporté
- [x] Belgique non applicable (justifié)

---

## Impact par domaine métier

**Sensible au domaine** : DROIT_DU_TRAVAIL FRANCE uniquement.
- Travail FR : cœur de la SF.
- Travail BE : pas d'équivalent forfaitaire — la sanction belge est pénale/administrative, pas civile.
- Immigration / Famille : non applicable.

---

## Parité des domaines métier

Niveau 3 (calculateur). Règle de parité niveau ≥5 non applicable.

---

## Critères d'acceptation

- [ ] **C1** : `IndemniteTravailDissimuleCalculator.compute(BigDecimal.valueOf(2500))` → `indemnite=15000.00`, formule `"6 mois × 2 500,00 € = 15 000,00 €"`, base `Art. L.8223-1 Code du travail`.
- [ ] **C2** : 3 messages affichés (cumul, condition application, non cumul visite médicale).
- [ ] **C3** : salaire nul → `IllegalArgumentException`.
- [ ] **C4** : salaire négatif → `IllegalArgumentException`.
- [ ] **C5** : salaire null → `IllegalArgumentException`.
- [ ] **C6** : Migration Liquibase 110 crée la table `travail_dissimule_analyses` (UNIQUE case_file_id).
- [ ] **C7** : Seed `decision_tool_visibility_rules` (ALWAYS_ON, DROIT_DU_TRAVAIL, FRANCE, tool_id `F-DT-21-travail-dissimule`).
- [ ] **C8** : `POST /api/v1/case-files/{id}/travail-dissimule` valide → 200.
- [ ] **C9** : `POST` dossier DROIT_IMMIGRATION → 400.
- [ ] **C10** : `POST` workspace étranger → 404.
- [ ] **C11** : `POST` upsert (2e appel remplace le premier).
- [ ] **C12** : `GET` renvoie le résultat persisté.

---

## Périmètre

### Hors scope (explicite)
- Frontend Angular → SF ultérieure.
- Détection automatique du travail dissimulé par l'IA → hors périmètre (nécessite analyse fine des pièces : URSSAF, bulletins, contrats).
- Cumul effectif avec les autres indemnités du dossier → pas de calcul agrégé, juste l'indemnité forfaitaire isolée.
- Prescription (5 ans art. L.3245-1) → pas calculée ici.

---

## Technique

### Endpoints
| Méthode | URL | Auth |
|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/travail-dissimule` | OAuth2 |
| GET | `/api/v1/case-files/{caseFileId}/travail-dissimule` | OAuth2 |

### Tables impactées
| Table | Opération | Notes |
|---|---|---|
| `travail_dissimule_analyses` | CREATE | nouvelle table 1:1 avec `case_files` |
| `decision_tool_visibility_rules` | INSERT 1 ligne | pour F-IA-04 |

### Migration Liquibase
- [x] Oui — `110-create-travail-dissimule-analyses.xml`

### Composants créés
- Backend : `IndemniteTravailDissimuleCalculator`, `TravailDissimuleAnalysis`, `TravailDissimuleRepository`, `TravailDissimuleRequest/Response/Result`, `TravailDissimuleService`, `TravailDissimuleController`.
- Tests : `IndemniteTravailDissimuleCalculatorTest` (UT), `TravailDissimuleControllerIT` (IT).

### Contraintes de validation

| Champ | Obligatoire | Format |
|---|---|---|
| `salaireMensuelReference` | Oui | BigDecimal > 0, precision 12 scale 2 |
| `case_file_id` | — | UNIQUE |

---

## Plan de test

### Tests unitaires
- [ ] `compute_nominal_returns6moisSalaire`
- [ ] `compute_negativeSalaire_throwsIllegalArgument`
- [ ] `compute_zeroSalaire_throwsIllegalArgument`
- [ ] `compute_nullSalaire_throwsIllegalArgument`
- [ ] `compute_formula_properlyFormatted`
- [ ] `compute_messages_contains3Items_includingCumulAndNonCumul`

### Tests d'intégration
- [ ] `POST_nominal_returns200`
- [ ] `POST_upsert_replacesAnalysis`
- [ ] `POST_salaireZero_returns400`
- [ ] `POST_immigrationCaseFile_returns400`
- [ ] `POST_otherWorkspace_returns404`
- [ ] `GET_afterPost_returnsPersisted`
- [ ] `GET_withoutPost_returns404`

### Isolation workspace
- [x] `POST_otherWorkspace_returns404` couvre le pattern.

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] Aucune (isolé)

### Composants / endpoints impactés
Aucun — ajout pur.

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

- **6 mois forfaitaire inconditionnel** : la Cour de cassation a retenu le caractère forfaitaire et non-plafonné. On ne calcule donc pas un prorata.
- **Salaire de référence** : la jurisprudence retient le salaire le plus favorable entre 12 derniers mois et 3 derniers mois. L'outil ne fait pas ce choix — l'avocat saisit le salaire qu'il a retenu après analyse des bulletins. Un futur `prefillFromAi` pourrait proposer le plus favorable.
- **Pas d'IndemniteTravailDissimule*` préfix** : on simplifie en `TravailDissimule*` pour les DTOs/entity, plus lisible. Le Calculator garde le préfixe `Indemnite` pour cohérence avec les autres calculators du package.
