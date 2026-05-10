# SF-210-01 — Médiation familiale obligatoire pré-saisine JAF (backend FR)

## Objectif
Backend de l'outil décisionnel `mediation-familiale-obligatoire-pre-saisine` : analyseur qui, à partir de la situation procédurale, détermine si la tentative de médiation familiale est OBLIGATOIRE avant saisine JAF (art. 373-2-10 al. 3 Cciv + Loi 18/11/2016 + art. 1108 CPC) et calcule la recevabilité de la requête.

## Comportement nominal
- Endpoint `POST /api/v1/case-files/{caseFileId}/mediation-familiale-pre-saisine` reçoit l'objet de demande contenant : motif principal de saisine (autorité parentale / contribution entretien / résidence enfant / autre), tentative de médiation effectuée (oui/non), date de la tentative (optionnel), exception applicable (urgence / violence / accord préalable / motif légitime / autre / aucune).
- Le calculator détermine :
  - **Recevable** : médiation tentée OU exception applicable applicable.
  - **Irrecevable** : médiation obligatoire (motif soumis) ET pas d'exception ET pas de tentative.
  - **Non concerné** : motif hors champ de la médiation obligatoire (ex : divorce, succession, urgence pure).
- Renvoie un verdict `RECEVABLE` / `IRRECEVABLE` / `NON_CONCERNE`, la base juridique citée, des messages explicatifs, la liste des pièces à joindre (attestation de tentative ou justificatif de l'exception), et la dispense applicable (le cas échéant).
- Persiste l'analyse 1:1 par dossier (table `mediation_familiale_pre_saisine_analyses`).

## Cas d'erreur
- Workspace mismatch / dossier sans accès → 404.
- Pays ≠ FRANCE → 400 (outil FR-only — Loi française).
- Domaine légal ≠ DROIT_FAMILLE → 400.
- Body manquant (motif null) → 400.

## Critères d'acceptation
- AC1 : un dossier FR DROIT_FAMILLE peut POSTer la requête → 200, entité créée.
- AC2 : un dossier BE → 400 message explicite "outil FR uniquement".
- AC3 : motif "AUTORITE_PARENTALE" + médiation non tentée + sans exception → verdict `IRRECEVABLE`.
- AC4 : motif "AUTORITE_PARENTALE" + exception "VIOLENCE" → verdict `RECEVABLE`.
- AC5 : motif "DIVORCE_CONTENTIEUX" → verdict `NON_CONCERNE` (médiation préalable obligatoire ne s'applique pas, art. 373-2-10 vise les saisines AP/contribution).
- AC6 : POST puis GET → réponse identique (idempotence).

## Plan de test
- UT calculator : 3 motifs in-scope, 3 motifs out-of-scope, 5 exceptions, sans tentative / avec tentative.
- UT service : gates pays + domaine + workspace.
- IT controller : 1 happy path POST + GET round-trip.

## Tables / endpoints / composants impactés
- Migration Liquibase : 218 (nouveau fichier).
- Table : `mediation_familiale_pre_saisine_analyses`.
- Java : package `fr.ailegalcase.casefile` (Calculator, Result, Analysis, Repository, Request, Response, Service, Controller, MotifEnum, ExceptionEnum, VerdictEnum).
- Endpoint : `POST/GET /api/v1/case-files/{id}/mediation-familiale-pre-saisine`.
- Migration 218 INSERT règle CONTEXTUAL `mediation_familiale_pre_saisine_pertinente` (priority 96).

## Hors périmètre
- Frontend (SF-210-02).
- Détection IA du flag `mediation_familiale_pre_saisine_pertinente` ajoutée par F-200 (sera ajoutée dans cette migration : flag dans `FamilleExtractedData` + prompt FR + extracteur).

## Analyse de cohérence transversale
- **Outils décisionnels Famille FR existants** : 35 outils (cf. `KNOWN_FRONTEND_TOOL_IDS`). Aucun ne couvre la médiation obligatoire pré-saisine (gap identifié rang 4 dans `audit-famille-fr-exhaustif.md`).
- **Pas de doublon** avec `F-FA-19-desaccords-parentaux` (outil de qualification du conflit, pas de la recevabilité procédurale).
- **Pas d'override** sur outils existants. Pure addition.

## Nouveau pattern UI ou service partagé
- Aucun pattern partagé introduit. Suit le template `OrdonnanceProtectionService`/`Controller`/`Calculator`.

## Impact par domaine métier
- **Travail FR/BE** : non applicable (procédure prud'homale distincte).
- **Immigration FR/BE** : non applicable.
- **Famille FR** : **central** — le motif de saisine est analysé selon les catégories de l'art. 373-2-10 al. 3 Cciv.
- **Famille BE** : non applicable (procédure BE distincte — Tribunal de la famille, pas de médiation obligatoire pré-saisine généralisée).

## Audit "Impact F-166 cross-C×D"
- FRxTravail : aucun impact.
- FRxImmigration : aucun impact.
- FRxFamille : nouvelle entrée CONTEXTUAL `mediation_familiale_pre_saisine_pertinente` priority 96. Apparaît si l'IA détecte une saisine JAF in-scope (autorité parentale / contribution entretien) — sinon caché.
- BExTravail/Immigration/Famille : aucun impact (FR-only).

## Audit "exhaustivité droit national FR"
- Source : Cciv art. 373-2-10 al. 3 (modifié par Loi n°2016-1547 du 18/11/2016) + CPC art. 1108. Contrainte : tentative obligatoire avant saisine JAF si motif AP / contribution entretien d'enfant. Exception : urgence, violences, motif légitime, accord conjoint préalable.
- Équivalent BE : pas d'équivalent strict (la médiation BE est encouragée par CJ art. 1734 mais non sanctionnée d'irrecevabilité pré-saisine pour la même catégorie de motifs). Outil FR-only justifié.

## Contrat API
- `POST /api/v1/case-files/{caseFileId}/mediation-familiale-pre-saisine`
- Body :
  ```json
  {
    "motifSaisine": "AUTORITE_PARENTALE|CONTRIBUTION_ENTRETIEN|RESIDENCE_ENFANT|DROIT_VISITE|DIVORCE_CONTENTIEUX|SUCCESSION|AUTRE",
    "mediationTentee": true,
    "dateMediation": "2026-04-15",
    "exceptionApplicable": "AUCUNE|URGENCE|VIOLENCE|ACCORD_PREALABLE|MOTIF_LEGITIME|AUTRE",
    "exceptionDetail": "Violences conjugales documentées par main courante du 12/04/2026"
  }
  ```
- Réponse : objet contenant `caseFileId`, `motifSaisine`, `mediationTentee`, `dateMediation`, `exceptionApplicable`, `verdict` (RECEVABLE/IRRECEVABLE/NON_CONCERNE), `motifInScope` (boolean), `dispenseApplicable` (boolean), `piecesAJoindre` (List), `baseJuridique`, `formule`, `messages`, `country`.
- Codes erreur : 400 si pays ≠ FR, body invalide ; 404 si dossier inaccessible.
