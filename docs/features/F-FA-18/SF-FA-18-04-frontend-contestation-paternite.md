# SF-FA-18-04 — Frontend contestation de paternité (art. 332-335 Cciv)

> **SF-04 du chantier F-FA-18 (Filiation)** — pendant frontend de SF-FA-18-03
> (backend mergé PR #660). Contrat API importé de SF-FA-18-03.

## Objectif

Exposer côté Angular l'outil décisionnel "Contestation de paternité" (FR — art. 332-335 + 311-1 + 321 + 372 Cciv), branché sur les endpoints `/api/v1/case-files/{id}/contestation-paternite-analysis` exposés par le backend SF-FA-18-03. Une seule mini-spec frontend isomorphe à SF-FA-18-02 (reconnaissance paternelle).

## Contrat API (importé de SF-FA-18-03)

- **POST** `/api/v1/case-files/{caseFileId}/contestation-paternite-analysis` body :
  ```json
  {
    "qualiteAagir": "PERE_DECLARE" | "PERE_BIOLOGIQUE_PRESUME" | "MERE" | "ENFANT_MAJEUR",
    "dateEtablissementFiliation": "2018-04-15",
    "dateConnaissanceVerite": "2025-01-20",
    "dateMajoriteEnfant": "2026-06-01" | null,
    "possessionEtatConforme5Ans": false,
    "expertiseAdnDemandee": true,
    "motifsSerieux": true
  }
  ```
- **Response 200** :
  ```json
  {
    "caseFileId": "uuid",
    "qualiteAagir": "PERE_DECLARE",
    "verdictRecevabilite": "ELEVEE" | "MOYENNE" | "FAIBLE",
    "scoreRecevabilite": 88,
    "delaiPrescriptionAns": 5,
    "delaiPrescriptionRestantMois": 42,
    "expertiseAdnRecommandee": true,
    "risquesRefus": ["..."],
    "documentsRequis": ["..."],
    "baseJuridique": "Art. 332-335 + 311-1 + 321 + 372 Cciv",
    "formule": "...",
    "messages": ["..."],
    "country": "FRANCE"
  }
  ```
- **Erreurs** : 400 (validation, country BE, domain mismatch), 404 (case file inconnu, GET sans POST).

## Comportement nominal

1. Section repliée par défaut (icône `gavel`).
2. À l'expansion :
   - Si `workspaceCountry === 'BELGIQUE'` → bannière info "Outil français uniquement" (équivalent CC art. 318/330 — feature jumelle au backlog).
   - Sinon, GET silencieux : si 200 → mode résultat ; si 404 → mode formulaire + pré-fill IA.
3. Form :
   - **Radio qualité à agir** (4 valeurs) — obligatoire.
   - **Date d'établissement de la filiation** (`<input type="date">`) — obligatoire.
   - **Date de connaissance de la vérité** (`<input type="date">`) — obligatoire.
   - **Date de majorité de l'enfant** (`<input type="date">`) — affiché et obligatoire seulement si `qualiteAagir === 'ENFANT_MAJEUR'`.
   - **Toggle** `expertiseAdnDemandee`.
   - **Radio** `motifsSerieux` (oui/non) — obligatoire.
   - **Radio** `possessionEtatConforme5Ans` (oui/non) — obligatoire.
4. POST → résultat hydraté + chip alerte si `delaiPrescriptionRestantMois < 6` (rouge) ou `< 12` (or). Délai affiché en chip avec `delaiPrescriptionAns` (5 ou 10).
5. Bouton "Modifier" pour retour au form.

## Cas d'erreur

- 400 backend → snackbar rouge avec message backend.
- 404 GET initial → mode formulaire (silencieux).
- Form invalide → bouton "Analyser" disabled.

## Critères d'acceptation vérifiables

1. Section repliée par défaut, dépliable au clic header.
2. workspace BE → bannière info, aucun appel HTTP.
3. workspace FR + GET 404 → mode formulaire visible.
4. workspace FR + GET 200 → résultat hydraté affiché, badge verdict colorisé (navy/or/rouge).
5. Form invalide → submit disabled.
6. POST nominal → résultat affiché + snackbar OK + `triggerRefresh()`.
7. POST 400 → snackbar rouge.
8. Pré-fill IA depuis `aiData` (qualité, dates) si présent.
9. Provenance IA effacée au changement manuel d'un champ pré-rempli.
10. F-IA-03 : alertes de cohérence sur `qualiteAagir`, `motifsSerieux`, `expertiseAdnDemandee`.
11. Chip alerte affichée si `delaiPrescriptionRestantMois < 6` (rouge) ou `< 12` (or).
12. `dateMajoriteEnfant` requis seulement si qualité = `ENFANT_MAJEUR`.

## Plan de test

- **Jest** (`contestation-paternite-section.component.spec.ts`, ≥ 12) :
  - Gate FRANCE/BELGIQUE.
  - GET 200 → result hydraté, GET 404 → form.
  - Pré-fill IA sur 3 fields.
  - Provenance reset au onChange.
  - formValid avec qualité ENFANT_MAJEUR (date majorité requise).
  - calculate() POST + snackbar OK + result.
  - calculate() ignoré si form invalide.
  - calculate() erreur 400 → snackbar rouge.
  - F-IA-03 alerte qualité divergence IA.
  - chip alerte délai < 6 mois.

## Tables / endpoints / composants impactés

- **Composants** : `frontend/src/app/case-files/contestation-paternite-section/` (4 fichiers).
- **Modèle** : `frontend/src/app/core/models/contestation-paternite.model.ts`.
- **Service** : `frontend/src/app/core/services/contestation-paternite.service.ts`.
- **Pré-fill IA** : extension `FamilleExtractedData` (3-4 champs `*Detected`).
- **Panel** : entrée `'F-FA-18-contestation-paternite'` dans `decisional-tools-panel.component.ts`.

## Hors périmètre

- Backend (couvert SF-FA-18-03 mergé PR #660).
- Belgique — feature jumelle au backlog.
- Action en recherche de paternité (art. 327 et s.) → SF-FA-18-05.

## Impact par domaine métier

Feature **sensible au domaine** :
- **Droit du travail / Immigration** : non applicable (404 / 400 par gate côté backend).
- **Droit famille FR** : couvert.
- **Droit famille BE** : feature jumelle au backlog (régime CC art. 318/330 distinct).

## Parité des domaines métier (outil de niveau 5)

Ce composant frontend reflète l'outil scoring backend SF-FA-18-03. Parité = celle de SF-FA-18-03 (FR seulement, BE au backlog).

## Analyse de cohérence transversale

- **Outils décisionnels existants** : SF-FA-18-04 réutilise strictement le pattern `reconnaissance-paternelle-section` (PR #655 — chantier F-FA-18 jumeau). Mêmes conventions : signal-based, `<input type="date">`, gate `isFrance`, `MatSnackBar`, `CaseDashboardRefreshService`, `CoherenceAlertBuilder`, palette navy/or/rouge.
- **Patterns transversaux** : aucun nouveau composant partagé / DTO / directive. Réutilisation `CoherencePopoverTriggerDirective`, `LegalCitationsPipe`.

## Préoccupations transversales

- **Auth / Principal** : aucun changement (header `OAuth2/JWT` géré par interceptor).
- **Workspace context** : aucun changement (`workspaceCountry` injecté par parent).
- **Plans / limites** : non concerné.
- **Navigation / routing** : aucun ajout.
- **Outil décisionnel métier** : nouveau composant, scan effectué — un outil = une situation métier (contestation 332-335 ≠ reconnaissance volontaire 316 = SF-FA-18-02). Pas de mélange.

## Nouveau pattern UI ou service partagé

Aucun. Ré-emploi strict du pattern jumeau `reconnaissance-paternelle-section`.
