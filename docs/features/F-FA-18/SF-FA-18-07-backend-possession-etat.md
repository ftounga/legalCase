# SF-FA-18-07 — Backend possession d'état (art. 311-1+ + 317 Cciv)

## Objectif

Exposer un outil décisionnel back-end qui évalue, pour un dossier de droit
de la famille (FRANCE), la **recevabilité d'une possession d'état** comme
mode de preuve / d'établissement de la filiation et son **dispositif
applicable** (constat par notaire — art. 317 — vs preuve judiciaire — 311-1 / 311-2).

## Contexte métier

La **possession d'état** (art. 311-1 Cciv) est un mode de preuve de la
filiation par les faits, constituée par un faisceau d'indices :

- **Tractatus** : l'enfant a été traité comme tel par le supposé parent
  (logé, nourri, éduqué, présenté à la famille).
- **Fama** : la famille, la société, l'autorité publique le considèrent
  comme tel.
- **Nomen** : il porte le nom de famille du parent (facultatif depuis
  l'ordonnance n°2005-759 du 4 juillet 2005).

Conditions cardinales (art. 311-2) : la possession d'état doit être
**continue, paisible, publique et non équivoque**.

Effets selon le dispositif applicable :

| Dispositif | Article | Force |
|---|---|---|
| Constat par notaire (acte de notoriété) | 317 | Force probante, contestable 5 ans à compter de l'établissement de l'acte |
| Preuve en justice à l'occasion d'une action en recherche / contestation | 311-1 + 311-2 | Soumis au juge, possible 10 ans à compter de la cessation |
| Possession publique longue (≥ 5 ans) | 311-1 + 317 | Présomption renforcée |

## Comportement nominal

- Endpoint `POST /api/v1/case-files/{caseFileId}/possession-etat-analysis`
  reçoit la durée alléguée et les booléens de critères, calcule un score,
  un verdict (`ELEVEE` / `MOYENNE` / `FAIBLE`), un dispositif applicable
  (`CONSTAT_NOTAIRE` / `PREUVE_JUSTICE`), liste les critères remplis et
  manquants, expose les délais de contestation et les messages avocat.
- Réponse persistée 1:1 par dossier, ré-écrite à chaque appel.
- `GET` même URL renvoie l'analyse persistée.

## Cas d'erreur

| Cas | HTTP |
|---|---|
| Body absent / `dateDebutPossession` ou `dateFinPossession` null / `dateFinPossession < dateDebutPossession` | 400 |
| Workspace `country = BELGIQUE` (régime distinct, art. 331/331-1 CC belge) | 400 |
| Dossier d'un autre `legalDomain` que `DROIT_FAMILLE` | 400 |
| Dossier appartenant à un autre workspace | 404 |
| `GET` avant `POST` | 404 |

## Critères d'acceptation

- 3 critères constitutifs (`tractatus`, `fama`, `nomen`) + 3 conditions
  (`continue`, `paisible`, `nonEquivoque`) modélisés en booléens.
- Durée calculée en années depuis `dateDebutPossession` jusqu'à
  `dateFinPossession`.
- Verdict :
  - `ELEVEE` si tractatus + fama + continue + paisible + nonEquivoque + durée ≥ 5 ans
  - `MOYENNE` si ≥ 4 critères présents et durée ≥ 1 an
  - `FAIBLE` sinon
- Dispositif :
  - `CONSTAT_NOTAIRE` (art. 317) : possession ≥ 5 ans + tous les critères
    cardinaux + non-contestée → acte de notoriété possible
  - `PREUVE_JUSTICE` : à défaut, voie judiciaire dans le cadre d'une
    action en recherche ou contestation
- Délais : 5 ans depuis l'acte de notoriété (`delaiContestationActeAns`) ;
  10 ans depuis la cessation (`delaiContestationCessationAns`).
- Base juridique : `Art. 311-1 + 311-2 + 317 Cciv`.
- Single-country FRANCE — appel BE → 400.

## Plan de test

### Calculator (≥ 15 unitaires)

1. `tousCriteresRemplis_dureeLongue_returnsELEVEE_dispositifConstatNotaire`
2. `dureeCourte_avecTousCriteres_returnsMOYENNE_dispositifPreuveJustice`
3. `aucunCritere_returnsFAIBLE`
4. `nomenAbsent_maisAutresCriteres_resteRecevable` (nomen facultatif)
5. `nonContinue_classeCommeFAIBLE` (cassée par la condition cardinale)
6. `nonPaisible_classeCommeFAIBLE`
7. `equivoque_classeCommeFAIBLE`
8. `dureeMoinsDe5Ans_dispositifPreuveJustice`
9. `dureeSuperieureA5Ans_avecConditionsRemplies_dispositifConstatNotaire`
10. `delaiContestation_acteNotaire_5ans`
11. `delaiContestation_cessation_10ans`
12. `criteresRemplisListNonVide_quandPossessionPositive`
13. `criteresManquantsListNonVide_quandManquements`
14. `country_BELGIQUE_throws`
15. `validation_dateDebut_null_throws`
16. `validation_dateFin_avant_debut_throws`
17. `country_FRANCE_normalized_lowercase`
18. `formule_contient_score_et_verdict`
19. `messages_mentionnent_artNot317`
20. `messages_mentionnent_caractereFacultatifNomen`
21. `booleanNull_traitesCommeFalse`
22. `dispositif_aucunCritere_resteFAIBLE`
23. `messages_mentionnent_tribunalCompetent`

### IT (≥ 7)

1. `POST_fr_tousCriteresRemplis5ans_returnsELEVEE_constatNotaire`
2. `POST_fr_dureeCourte_returnsMOYENNE_preuveJustice`
3. `POST_fr_aucunCritere_returnsFAIBLE`
4. `POST_workspaceBe_returns400`
5. `POST_droitDuTravail_returns400`
6. `POST_otherWorkspace_returns404`
7. `POST_missingDateDebut_returns400`
8. `POST_dateFinAvantDebut_returns400`
9. `POST_upsert_replacesAnalysis`
10. `GET_afterPost_returnsPersisted`
11. `GET_withoutPost_returns404`

## Tables / endpoints / composants

- Nouvelle table `possession_etat_analyses` (1:1 par `case_file_id`).
- Migration `185-create-possession-etat-analyses.xml` + visibility
  `ALWAYS_ON / DROIT_FAMILLE / FRANCE / priority 94 /
  UUID f1a04001-0000-0000-0000-ee0000000185 /
  tool_id 'F-FA-18-possession-etat'`.
- Endpoints `POST/GET /api/v1/case-files/{caseFileId}/possession-etat-analysis`.
- Aucun composant Angular dans cette SF (frontend en SF-FA-18-08).

## Hors périmètre

- Belgique (régime CC belge art. 331-1 — feature jumelle au backlog).
- Frontend (SF-FA-18-08).
- Génération de l'acte de notoriété (relève de F-FA-18 SF dédiée plus tard).

## Analyse de cohérence transversale

- **Autres outils F-FA-18** : 5 SF déjà livrées (reconnaissance paternelle,
  contestation paternité, recherche paternité). Cet outil constitue le
  pendant probatoire — pas d'overlap fonctionnel.
- **Autres pays** : ouverture future BE (feature jumelle à inscrire au
  backlog F-FA-18-9).
- **Autres domaines** : non applicable (filiation = droit de la famille).
- **UI patterns** : harmonisation lors de la SF frontend.

## Impact par domaine métier

Sensible au domaine **droit de la famille** uniquement. Non applicable
aux domaines **droit du travail** et **immigration** (filiation = droit
de la personne / famille). Pour la Belgique, le régime CC art. 331-1
diffère (mode de preuve résiduel vs établissement subsidiaire) — feature
jumelle au backlog plutôt qu'un switch dans le calculateur (invariant
"un outil = une situation métier").

## Parité des domaines métier

Outil de niveau 5 (scoring / analyse de validité) :
- **Droit de la famille FR** : livré dans cette SF.
- **Droit de la famille BE** : feature jumelle au backlog (régime CC belge).
- **Droit du travail / immigration** : non pertinent (concept propre à la filiation).

## Référence

Pattern : `SF-FA-18-05-backend-action-recherche-paternite.md` (PR #664).
