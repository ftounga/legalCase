# SF-216-01 — Prestation compensatoire FR — backend

## Objectif

Restaurer et compléter le calculateur backend de la prestation compensatoire (art. 270-281 Cciv) : à partir de la durée du mariage, des revenus, du patrimoine et de l'âge des époux, calculer le montant prévisionnel, recommander la forme (capital / rente) et évaluer l'incidence de l'avantage matrimonial.

> Outil `F-FA-01-prestation-compensatoire` — DELETE migration 191, à restaurer.

## Comportement nominal

- Endpoint `POST/GET /api/v1/case-files/{caseFileId}/prestation-compensatoire`.
- Body :
  - `dureeMariageAnnees` (int, requis)
  - `revenusAnnuelsEpoux1Eur` (int, requis) — époux demandeur / le moins aisé
  - `revenusAnnuelsEpoux2Eur` (int, requis) — époux défendeur
  - `patrimoinePropre1Eur` (int, optionnel)
  - `patrimoinePropre2Eur` (int, optionnel)
  - `ageEpoux1Annees` (int, requis)
  - `ageEpoux2Annees` (int, requis)
  - `formePrestationDemandee` (CAPITAL | RENTE | RENTE_CONVERTIBLE | INCERTAIN)
  - `avantageMatrimonialDetecte` (boolean, optionnel)
- Calculator `PrestationCompensatoireCalculator` détermine :
  - **Disparité de niveaux de vie** : delta revenus + estimation impact patrimoinial.
  - **Montant indicatif capital** : formule (durée × delta revenus mensuel moyen × facteur correctif âge/durée).
  - **Rente mensuelle indicative** : delta revenus mensuel limité au tiers de la différence.
  - **Recommandation de forme** : capital si demandeur < 60 ans et durée < 20 ans ; rente si durée > 20 ans OU demandeur > 60 ans (jurisprudence Cass. 1ère civ. 2014).
  - **Flag avantage matrimonial** : si communauté universelle + clause attribution intégrale détectée, réduire la prestation estimée (avantage matrimonial imputable art. 270 al. 2 Cciv).
- Retourne : `montantCapitalIndicatifEur`, `renteIndicativeMensuelleEur`, `formeRecommandee`, `dispAriteNiveauxVie`, `baseJuridique`, `messages`, `alertes`.
- Persiste 1:1 par dossier (table `prestation_compensatoire_analyses`).

## Cas d'erreur

- `country ≠ FRANCE` → 400 « outil FR uniquement ».
- `legalDomain ≠ DROIT_FAMILLE` → 400.
- `dureeMariageAnnees < 0` ou `revenusAnnuels*Eur < 0` → 400.
- Workspace mismatch / dossier introuvable → 404.

## Source juridique

- **art. 270-281 Cciv** — prestation compensatoire, formes, critères.
- **art. 272 Cciv** — liste non exhaustive des critères (durée, état de santé, qualification professionnelle, revenus, droits retraite).
- **Cass. 1ère civ., 8/10/2014, n°13-21.804** — capital = règle, rente = exception.
- **art. 276-4 Cciv** — conversion rente en capital.

## Champs IA à extraire (FamilleExtractedData)

**Réutilisés (F-246)** :
- `vie_commune_detection.dureeMariageAnnees`
- `vie_commune_detection.revenusAnnuelsEpoux1`
- `vie_commune_detection.revenusAnnuelsEpoux2`
- `vie_commune_detection.patrimoineCommunEur`
- `regimes_vie_commune_detection_v2.clauseAttributionIntegraleDetected`
- `regimes_vie_commune_detection_v2.regimeMatrimonialDetecte`

**Nouveaux champs à ajouter à `FamilleExtractedData` + prompt `FAMILLE_INSTRUCTION`** :
- `ageEpoux1Annees` (int | null) — âge du demandeur extrait de la pièce d'identité ou de la copie intégrale d'acte de naissance.
- `ageEpoux2Annees` (int | null) — âge du défendeur, idem.
- `prestationCompensatoireEnvisagee` (boolean | null) — flag CONTEXTUAL détecté si mention « prestation compensatoire », « art. 270 », « disparité de niveaux de vie » dans le dossier.

## Plan de test

- UT calculator : (a) revenus très inégaux → capital fort ; (b) revenus égaux → `dispAriteNiveauxVie = NULLE`, PC ≈ 0 ; (c) durée 25 ans + demandeur 62 ans → forme `RENTE` ; (d) avantage matrimonial détecté → montant réduit ; (e) revenus négatifs → erreur 400.
- UT service : gates `country`, `legalDomain`, workspace mismatch.
- IT controller : POST + GET round-trip.

## Composants impactés

- Migration Liquibase 271 : table `prestation_compensatoire_analyses`.
- Migration Liquibase 272 : INSERT `decision_tool_visibility_rules` CONTEXTUAL `prestationCompensatoireEnvisagee`, `DROIT_FAMILLE`, `FRANCE`, priority 98.
- Java : `PrestationCompensatoireCalculator`, `PrestationCompensatoireResult`, `PrestationCompensatoireAnalysis`, `PrestationCompensatoireRepository`, `PrestationCompensatoireRequest`, `PrestationCompensatoireResponse`, `PrestationCompensatoireService`, `PrestationCompensatoireController`, `FormePrestationEnum`.
- `CaseAnalysisResponse.java` — ajout de `ageEpoux1Annees`, `ageEpoux2Annees`, `prestationCompensatoireEnvisagee` dans `FamilleExtractedData`.
- `LegalDomainPromptBuilder` — section `FAMILLE_INSTRUCTION` : ajouter extraction de `ageEpoux1Annees`, `ageEpoux2Annees`, `prestationCompensatoireEnvisagee`.

## Critères d'acceptation

- AC1 : revenus (20 000 € / 80 000 € annuels), durée 15 ans → montant > 0, forme `CAPITAL`.
- AC2 : revenus égaux → `dispAriteNiveauxVie = NULLE`, `formeRecommandee = INCERTAIN`, PC ≈ 0.
- AC3 : `country=BELGIQUE` → 400.
- AC4 : POST puis GET → réponse identique.
- AC5 : champs IA `ageEpoux1Annees` correctement pré-rempli depuis le record.

## Hors périmètre

- Frontend (SF-216-02).
- Calcul fiscal (ISF, droits de mutation) de la prestation compensatoire.
- Conversion de rente post-jugement (art. 276-4 → à intégrer dans F-FA-13-revisions si absent).
