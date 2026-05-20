# SF-216-13 — Audition du mineur par le JAF (art. 388-1 Cciv) — backend

## Objectif

Outil décisionnel `F-FA-AUDITION-ENFANT` : évalue les conditions de recevabilité d'une demande d'audition du mineur par le JAF (art. 388-1 Cciv), détermine si le juge peut refuser (motivation obligatoire) et prépare la demande formelle.

## Comportement nominal

- Endpoint `POST/GET /api/v1/case-files/{caseFileId}/audition-mineur`.
- Body :
  - `ageEnfant` (int, requis)
  - `capaciteDiscernement` (CERTAINE | PROBABLE | DOUTEUSE | INCONNUE)
  - `demandeFormalisee` (boolean) — demande déjà formulée par les parties ou l'enfant
  - `demandeParEnfantLuiMeme` (boolean) — art. 388-1 al. 1 : l'enfant peut demander lui-même
  - `refusMotive` (boolean, optionnel) — le juge a-t-il déjà refusé ?
  - `motivationRefus` (texte, optionnel) — si oui, motif invoqué
  - `procedureEnCours` (DIVORCE | AUTORITÉ_PARENTALE | GARDE | SUCCESSION | AUTRE)
- Calculator :
  - **Droit à l'audition** : enfant capable de discernement + demande → droit à audition (art. 388-1 al. 1). Pas de seuil d'âge légal fixe — appréciation du discernement.
  - **Refus du juge** : possible mais doit être motivé (art. 388-1 al. 2). Si refus non motivé → voie de recours.
  - **Modalités** : seul, avec avocat, avec tiers (art. 388-1 al. 3).
  - **Alerte âge** : si enfant < 5 ans → discernement hautement improbable ; alerte.
  - **Alerte manipulation** : si `demandeFormalisee` dans contexte de conflit sévère → alerte prudence.
- Retourne : `conditionsRemplies`, `droitAuditionReconnu`, `modaliteRecommandee`, `baseLegale`, `messages`, `alertes`.
- Persiste 1:1 par dossier.

## Cas d'erreur

- `country ≠ FRANCE` → 400.
- `ageEnfant < 0` ou > 18 → 400.

## Source juridique

- **art. 388-1 Cciv** — droit à l'audition du mineur capable de discernement.
- **art. 1074-1 à 1074-3 CPC** — modalités procédurales de l'audition.
- **Convention internationale des droits de l'enfant (CIDE) art. 12** — droit de l'enfant à être entendu.
- **Cass. 1ère civ., 18/3/2015, n°14-11.392** — refus d'audition doit être spécialement motivé.

## Champs IA à extraire (FamilleExtractedData)

**Réutilisés (F-246)** :
- `filiation_detection_v2.agesEnfantsDetectes`
- `filiation_detection_v2.nombreEnfantsDetecte`

**Nouveaux champs à ajouter** :
- `auditionMineurEnvisagee` (boolean | null) — détecté si mention « audition mineur », « art. 388-1 », « entendre l'enfant ».
- `demandeAuditionFormaliseeDetectee` (boolean | null) — demande déjà formulée détectée dans les pièces.

## Plan de test

- UT calculator : (a) âge 10 ans + discernement probable → droit reconnu ; (b) âge 3 ans → alerte discernement improbable ; (c) refus non motivé → voie de recours suggérée ; (d) demande par l'enfant lui-même → art. 388-1 al. 1 direct.
- UT service : gates.
- IT : POST + GET.

## Composants impactés

- Migration Liquibase 283 : table `audition_mineur_analyses`.
- Migration Liquibase 284 : INSERT `decision_tool_visibility_rules` CONTEXTUAL `auditionMineurEnvisagee`, `DROIT_FAMILLE`, `FRANCE`, priority 104.
- Java : `AuditionMineurCalculator`, result, analysis, repository, service, controller.
- `CaseAnalysisResponse.java` — ajout `auditionMineurEnvisagee`, `demandeAuditionFormaliseeDetectee`.
- `LegalDomainPromptBuilder`.

## Critères d'acceptation

- AC1 : enfant 10 ans, discernement probable → droit reconnu, modalités proposées.
- AC2 : enfant 3 ans → alerte.
- AC3 : refus non motivé détecté → voie de recours.
- AC4 : `country=BELGIQUE` → 400.

## Hors périmètre

- Frontend (SF-216-14).
- Audition au pénal / tribunal pour enfants (hors scope JAF civil).
