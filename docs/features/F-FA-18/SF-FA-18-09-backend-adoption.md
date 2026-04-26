# SF-FA-18-09 — Backend adoption (art. 343-370-2 Cciv)

## Objectif

Exposer un outil décisionnel back-end qui évalue, pour un dossier de droit
de la famille (FRANCE), la **recevabilité d'une adoption** (plénière ou
simple) au regard des conditions cardinales du Code civil et propose la
**forme recommandée** lorsque les conditions de la forme demandée ne sont
pas remplies (typiquement : bascule plénière → simple).

## Contexte métier

Le droit français connaît deux formes d'adoption :

### Adoption plénière (art. 343 à 359 Cciv)

Remplace définitivement la filiation d'origine — l'adopté entre dans la
famille de l'adoptant comme s'il y était né.

Conditions cardinales :
- Adoptant ≥ 28 ans (art. 343-1) — abaissé à 26 ans pour un couple marié
  ou des partenaires (loi n°2022-219 du 21 février 2022)
- Différence d'âge ≥ 15 ans avec l'adopté (art. 344) — sauf circonstances
  exceptionnelles autorisées par le tribunal
- Adopté < 15 ans (art. 345) sauf exceptions (≤ 21 ans si recueilli avant
  ou possession d'état d'enfant adoptif)
- Placement de 6 mois minimum (art. 345-1)
- Consentement des parents biologiques (art. 348) ou statut de pupille
  de l'État (art. 347)
- Consentement de l'adopté ≥ 13 ans (art. 345 al. 3 — par renvoi)
- Consentement du conjoint si adoptant marié (art. 343)
- Enquêtes sociales conduites (services départementaux art. L. 225-2 CASF)

Effet : filiation d'origine effacée, irrévocable.

### Adoption simple (art. 360 à 370-2 Cciv)

Ajoute un lien sans effacer la filiation d'origine — double filiation.

Conditions cardinales :
- Adoptant ≥ 26 ans (art. 343 al. 1 par renvoi via art. 361)
- Différence d'âge ≥ 15 ans (art. 344) — sauf circonstances
- Aucune condition d'âge maximum pour l'adopté
- Consentement des parents biologiques si adopté mineur non orphelin
- Consentement de l'adopté ≥ 13 ans (art. 360 al. 3)
- Consentement du conjoint si adoptant marié

Effet : double filiation maintenue. Révocable pour motif grave (art. 370).

## Comportement nominal

- Endpoint `POST /api/v1/case-files/{caseFileId}/adoption-analysis`
  reçoit la forme demandée + critères, calcule un verdict
  (`ELEVEE` / `MOYENNE` / `FAIBLE`), une **forme recommandée**
  (peut basculer `PLENIERE` → `SIMPLE` si conditions plénière non remplies),
  liste les critères non remplis, expose le délai d'instruction, les
  documents requis, les risques de refus et les messages avocat.
- Réponse persistée 1:1 par dossier, ré-écrite à chaque appel.
- `GET` même URL renvoie l'analyse persistée.

## Cas d'erreur

| Cas | HTTP |
|---|---|
| Body absent / `formeAdoption` null / `ageAdoptant` ou `ageAdopte` null ou < 0 | 400 |
| Workspace `country = BELGIQUE` (régime distinct, art. 343 et s. CC belge) | 400 |
| Dossier d'un autre `legalDomain` que `DROIT_FAMILLE` | 400 |
| Dossier appartenant à un autre workspace | 404 |
| `GET` avant `POST` | 404 |

## Critères d'acceptation

- 2 formes (`PLENIERE`, `SIMPLE`) modélisées en enum.
- 9 entrées : `formeAdoption`, `ageAdoptant`, `ageAdopte`,
  `consentementParents`, `consentementAdopte`,
  `consentementConjointAdoptant`, `enquetes`, `placement6mois`,
  `pupilleEtat`.
- `differenceAgeAns` calculé = `ageAdoptant - ageAdopte`.
- Verdict :
  - `ELEVEE` si tous les critères de la forme demandée remplis
  - `MOYENNE` si critères mineurs manquants (ex : enquêtes pas encore
    finalisées) mais cardinaux OK
  - `FAIBLE` si un critère cardinal manque
- Forme recommandée :
  - Demande `PLENIERE` + plénière OK → `PLENIERE`
  - Demande `PLENIERE` + plénière KO + simple OK → `SIMPLE` (bascule)
  - Demande `SIMPLE` + simple OK → `SIMPLE`
  - Demande `SIMPLE` + simple KO → `AUCUNE`
- `delaiInstructionMois` typique : 6 (cas simple) à 18 (cas complexe).
- `documentsRequis` : extrait acte naissance adopté, consentements
  authentiques, enquête services sociaux, agrément si pupille, etc.
- `risqueRefus` : raisons probables de refus.
- Base juridique : `Art. 343-370-2 Cciv`.
- Single-country FRANCE — appel BE → 400.

## Plan de test

### Calculator (≥ 18 unitaires)

1. `plenierePleinementValide_returnsELEVEE_formePLENIERE`
2. `simplePleinementValide_returnsELEVEE_formeSIMPLE`
3. `pleniereDemande_maisAdoptantTropJeune_basculeSIMPLE_siSimpleOk`
4. `pleniereDemande_maisAdopteTropAge_basculeSIMPLE`
5. `pleniereDemande_sansPlacement6mois_FAIBLE_etRecommandeSIMPLE`
6. `simpleDemande_avecAdopteAdulte_resteELEVEE` (pas de limite d'âge)
7. `differenceAgeInsuffisante_FAIBLE`
8. `consentementParentsManquant_FAIBLE`
9. `consentementAdolescent13ans_obligatoire`
10. `consentementConjointMarié_obligatoire` — heuristique : si non, FAIBLE
11. `enquetesNonRealisees_MOYENNE_carCritereSecondaire`
12. `pupilleEtat_facilitePleniere`
13. `adoptantTresJeune_FAIBLE`
14. `country_BELGIQUE_throws`
15. `country_FRANCE_normalized_lowercase`
16. `validation_formeAdoption_null_throws`
17. `validation_ageAdoptant_negative_throws`
18. `differenceAge_calculee_correctement`
19. `formule_contient_verdict_et_forme`
20. `messages_mentionnent_articles_343_360`
21. `documentsRequis_listeNonVide_quandPlenierePropose`
22. `risqueRefus_alimente_quandFaible`
23. `delaiInstructionMois_dans_fourchette_6_18`
24. `aucuneFormeApplicable_returnsAUCUNE_etFAIBLE`

### IT (≥ 7)

1. `POST_fr_pleniereValide_returnsELEVEE_formePLENIERE`
2. `POST_fr_simpleValide_returnsELEVEE_formeSIMPLE`
3. `POST_fr_basculePleniereSimple_quandPleniereImpossible`
4. `POST_fr_critereCardinalManquant_returnsFAIBLE`
5. `POST_workspaceBe_returns400`
6. `POST_droitDuTravail_returns400`
7. `POST_otherWorkspace_returns404`
8. `POST_missingFormeAdoption_returns400`
9. `POST_upsert_replacesAnalysis`
10. `GET_afterPost_returnsPersisted`
11. `GET_withoutPost_returns404`

## Tables / endpoints / composants

- Nouvelle table `adoption_analyses` (1:1 par `case_file_id`).
- Migration `187-create-adoption-analyses.xml` + visibility
  `ALWAYS_ON / DROIT_FAMILLE / FRANCE / priority 96 /
  UUID f1a04001-0000-0000-0000-ee0000000187 /
  tool_id 'F-FA-18-adoption'`.
- Endpoints `POST/GET /api/v1/case-files/{caseFileId}/adoption-analysis`.
- Aucun composant Angular dans cette SF (frontend en SF-FA-18-10).

## Hors périmètre

- Belgique (régime CC belge — feature jumelle au backlog).
- Frontend (SF-FA-18-10).
- Adoption internationale (Conv. La Haye 1993) — feature dédiée future.
- Génération automatique de la requête en adoption (relève d'une SF
  ultérieure dédiée à la production documentaire).

## Analyse de cohérence transversale

- **Autres outils F-FA-18** : 8 SF déjà livrées (reconnaissance paternelle,
  contestation paternité, recherche paternité, possession d'état). Cet
  outil traite l'**établissement adoptif** de la filiation — pas
  d'overlap fonctionnel.
- **Autres pays** : ouverture future BE (feature jumelle à inscrire au
  backlog F-FA-18-11/12).
- **Autres domaines** : non applicable (filiation/adoption = droit de la
  famille).
- **UI patterns** : harmonisation lors de la SF frontend SF-FA-18-10
  (même template canonique).

## Impact par domaine métier

Sensible au domaine **droit de la famille** uniquement. Non applicable
aux domaines **droit du travail** et **immigration** (sauf via une
articulation future avec immigration : adoption d'un étranger ouvrant
droits — relève d'une feature dédiée). Pour la Belgique, le régime
CC belge art. 343 et s. diffère (notamment conditions d'âge et procédure
parquet) — feature jumelle au backlog plutôt qu'un switch dans le
calculateur.

## Parité des domaines métier

Outil de niveau 5 (scoring / analyse de validité) :
- **Droit de la famille FR** : livré dans cette SF (forme plénière + simple).
- **Droit de la famille BE** : feature jumelle au backlog (régime CC belge).
- **Droit du travail / immigration** : non pertinent (concept propre à la
  filiation civile).

## Référence

Pattern : `SF-FA-18-07-backend-possession-etat.md` (PR #670).
