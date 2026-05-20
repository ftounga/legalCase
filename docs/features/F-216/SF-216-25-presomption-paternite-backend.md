# SF-216-25 — Présomption de paternité du mari et désaveu — backend

## Objectif

Outil décisionnel `F-FA-PRESOMPTION-PATERNITE` : évalue l'application de la présomption de paternité du mari (art. 312 Cciv), vérifie si elle peut être renversée (art. 313-314 Cciv) et analyse la recevabilité d'une action en désaveu (art. 316 al. 2 Cciv + jurisprudence).

## Comportement nominal

- Endpoint `POST/GET /api/v1/case-files/{caseFileId}/presomption-paternite`.
- Body :
  - `dateNaissanceEnfant` (LocalDate, requis)
  - `dateConclusionMariage` (LocalDate, requis)
  - `dateDissolutionMariage` (LocalDate, optionnel) — si mariage déjà dissous
  - `dateAccouchement` (LocalDate, requis) — = `dateNaissanceEnfant` sauf accouchement posthume
  - `conceptionEn180PremiersMoisMariage` (boolean) — conception < 180 jours après mariage
  - `enfantNeApresDisso` (boolean) — né > 300 jours après dissolution
  - `desaveuEnvisage` (boolean)
  - `possessionEtatConformeDetecte` (boolean) — si possession d'état conforme → renversement désaveu difficile
- Calculator :
  - **Présomption applicable** (art. 312) : enfant conçu pendant le mariage + né moins de 300 jours après dissolution.
  - **Présomption renversée** (art. 313) : enfant né plus de 300 jours après la dissolution OU né moins de 180 jours après la conclusion du mariage ET mari nie être le père + pas de possession d'état (art. 313 al. 2).
  - **Désaveu** (art. 316 al. 2) : le mari peut contester la présomption. Délai 6 mois (art. 316 al. 2 + jurisprudence : à compter de la naissance ou de la connaissance de la naissance).
  - **Possession d'état conforme** : si établie → renforcement de la présomption, désaveu difficile (art. 333 al. 1).
- Retourne : `presomptionApplicable`, `presomptionRenversee`, `voieDesaveu`, `delaiDesaveu`, `possessionEtatImpact`, `baseLegale`, `messages`, `alertes`.
- Persiste 1:1 par dossier.

## Cas d'erreur

- `country ≠ FRANCE` → 400.
- Dates incohérentes (dissolution avant mariage) → 400.

## Source juridique

- **art. 312 Cciv** — présomption de paternité du mari.
- **art. 313-314 Cciv** — renversement de la présomption.
- **art. 316 Cciv** — désaveu de paternité (délai 6 mois).
- **art. 333 al. 1 Cciv** — possession d'état conforme neutralise contestation.
- **Cass. 1ère civ., 19/2/2014** — point de départ du délai de désaveu.

## Champs IA à extraire (FamilleExtractedData)

**Réutilisés (F-246)** :
- `filiation_detection_v2.dateNaissanceEnfantDetectee`
- `filiation_detection_v2.paterniteVraisemblableDetected`
- `filiation_detection_v2.possessionEtatConforme5AnsDetected`
- `vie_commune_detection.dateSeparation` (approximation dissolution)

**Nouveaux champs à ajouter** :
- `desaveuEnvisage` (boolean | null) — détecté si mention « désaveu », « art. 316 », « contester présomption paternité ».
- `dateConclusionMariageDetectee` (LocalDate | null) — date de mariage extraite des pièces.
- `dateDissolutionMariageDetectee` (LocalDate | null) — date de dissolution extraite des pièces.

## Plan de test

- UT calculator : (a) conception < 180j après mariage + pas possession d'état → présomption renversable ; (b) né > 300j après dissolution → présomption inapplicable ; (c) possession d'état conforme → désaveu difficile, alerte ; (d) délai 6 mois dépassé → désaveu irrecevable.
- UT service : gates.
- IT : POST + GET.

## Composants impactés

- Migration Liquibase 295 : table `presomption_paternite_analyses`.
- Migration Liquibase 296 : INSERT `decision_tool_visibility_rules` CONTEXTUAL `desaveuEnvisage`, `DROIT_FAMILLE`, `FRANCE`, priority 110.
- Java : `PresomptionPaternitéCalculator`, result, analysis, repository, service, controller.
- `CaseAnalysisResponse.java` — ajout `desaveuEnvisage`, `dateConclusionMariageDetectee`, `dateDissolutionMariageDetectee`.
- `LegalDomainPromptBuilder`.

## Critères d'acceptation

- AC1 : né 290j après dissolution → présomption applicable.
- AC2 : né 310j après dissolution → présomption inapplicable.
- AC3 : possession d'état conforme → alerte désaveu difficile.
- AC4 : `country=BELGIQUE` → 400.

## Hors périmètre

- Frontend (SF-216-26).
- Contestation paternité (outil existant F-FA-18-contestation-paternite — différent : vise la filiation déjà établie, pas la présomption).
