# SF-210-02 — Médiation familiale obligatoire (frontend FR)

## Objectif
Composant Angular minimal `<app-mediation-familiale-section>` qui consomme l'API SF-210-01 pour afficher le verdict de recevabilité et permettre à l'avocat de qualifier la situation.

## Comportement nominal
- Formulaire avec : motif saisine (radio), médiation tentée (toggle), date médiation (input date), exception applicable (select).
- Bouton "Analyser la recevabilité" → POST → affiche le verdict (banner navy/or/rouge selon RECEVABLE/NON_CONCERNE/IRRECEVABLE) + base juridique + pièces à joindre.
- Au mount, GET pour récupérer la dernière analyse (404 attendu si jamais lancée).
- Pré-fill IA via `aiData?.motifSaisineMediationDetecte` et `aiData?.exceptionMediationDetectee` (champs étendus de `FamilleExtractedData` côté frontend — pré-remplissage best-effort).
- Validation F-IA-03 sur le champ `motifSaisine` via `CoherenceAlertBuilder` quand l'avocat saisit un motif différent de celui détecté par l'IA.

## Cas d'erreur
- Erreur HTTP → MatSnackBar.
- Pays ≠ FRANCE → bannière info "outil disponible uniquement pour la France".

## Critères d'acceptation
- AC1 : composant standalone, importé dans `decisional-tools-panel`.
- AC2 : entrée `TOOL_REGISTRY['mediation-familiale-pre-saisine']` créée.
- AC3 : `KNOWN_FRONTEND_TOOL_IDS` mis à jour avec `mediation-familiale-pre-saisine`.
- AC4 : 1 spec Jest qui mocke le service et asserte le rendu du verdict RECEVABLE.
- AC5 : badge `auto_awesome` "Pré-rempli depuis l'analyse" si `motifSaisineMediationDetecte` présent.
- AC6 : `static getPrefillCount()` exposé pour le panel.

## Plan de test
- Spec Jest : 1 test rendering form + verdict après mock POST.

## Tables / endpoints / composants impactés
- Endpoint consommé : `POST/GET /api/v1/case-files/{id}/mediation-familiale-pre-saisine` (livré SF-210-01).
- Composant : `frontend/src/app/case-files/mediation-familiale-section/`.
- Service : `frontend/src/app/core/services/mediation-familiale.service.ts`.
- Modèles : `frontend/src/app/core/models/mediation-familiale.model.ts`.
- TOOL_REGISTRY entry dans `decisional-tools-panel.component.ts`.

## Hors périmètre
- Backend (livré SF-210-01).
- Pré-fill IA exhaustif (best-effort, fields optionnels).

## Analyse de cohérence transversale
- Pas de pattern UI partagé nouveau. Réutilise `CoherenceAlertBuilder`, `CoherencePopoverTriggerDirective`, `MatSnackBar`, `CaseDashboardRefreshService`.

## Nouveau pattern UI ou service partagé
- Aucun.

## Impact par domaine métier
- **Famille FR** : seul domaine concerné. `workspaceCountry='BELGIQUE'` → bannière info, pas de POST.
- **Travail/Immigration/Famille BE** : non applicable.

## Contrat API
Importé de SF-210-01 (cf. mini-spec backend).
