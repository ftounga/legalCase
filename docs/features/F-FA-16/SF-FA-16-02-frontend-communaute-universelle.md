# SF-FA-16-02 — Frontend communauté universelle (art. 1526 + 1527 Cciv)

## Objectif

Exposer dans le panel d'outils décisionnels (F-IA-04) un composant Angular
permettant à l'avocat d'analyser un régime de **communauté universelle**
(4ᵉ régime matrimonial FR — art. 1526 Cciv) sur 2 dispositifs : validité de
la convention de mariage et liquidation au décès (avec/sans clause
d'attribution intégrale + action en retranchement art. 1527 al. 2 Cciv).

## Contrat API (importé de SF-FA-16-01-backend, mergé PR #648)

- POST `/api/v1/case-files/{caseFileId}/communaute-universelle-analysis`
- GET `/api/v1/case-files/{caseFileId}/communaute-universelle-analysis`

Body (dispositif `VALIDITE_CONVENTION`) :

```json
{
  "dispositifAnalyse": "VALIDITE_CONVENTION",
  "contratNotarie": true,
  "inscriptionEtatCivil": true,
  "consentementLibreDesEpoux": true,
  "respectReserveHereditaire": true,
  "clauseAttributionIntegrale": null,
  "enfantsNonCommuns": null,
  "valeurCommunauteEur": null
}
```

Body (dispositif `LIQUIDATION_DECES`) :

```json
{
  "dispositifAnalyse": "LIQUIDATION_DECES",
  "contratNotarie": true,
  "inscriptionEtatCivil": null,
  "consentementLibreDesEpoux": null,
  "respectReserveHereditaire": null,
  "clauseAttributionIntegrale": true,
  "enfantsNonCommuns": true,
  "valeurCommunauteEur": 800000.0
}
```

Response 200 :

```json
{
  "caseFileId": "uuid",
  "dispositifAnalyse": "VALIDITE_CONVENTION" | "LIQUIDATION_DECES",
  "verdictValidite": "VALIDE" | "CONTESTABLE" | "NUL",
  "actionRetranchementPossible": true,
  "partAttributionConjointPct": 100,
  "valeurAttributionEur": 800000.0,
  "scoreValidite": 90,
  "risquesIdentifies": ["..."],
  "baseJuridique": "Art. 1526 Cciv + 1527 al. 2 Cciv (action en retranchement)",
  "formule": "...",
  "messages": ["..."],
  "country": "FRANCE"
}
```

Codes d'erreur : 400 (dispositif manquant, BE, mauvais domaine, valeurs
manquantes), 404 (case file inconnu / autre workspace, GET sans POST).

## Comportement nominal

1. Composant `CommunauteUniverselleSectionComponent` affiché par le panel
   F-IA-04 quand le tool_id `'F-FA-16-communaute-universelle'` est visible
   (migration 177 — ALWAYS_ON, DROIT_FAMILLE, FRANCE).
2. Au mount FR : GET → mode résultat hydraté si 200, sinon mode formulaire.
3. Mode formulaire : radio dispositif (`VALIDITE_CONVENTION` |
   `LIQUIDATION_DECES`) + champs conditionnels selon dispositif :
   - Toujours : `contratNotarie` (radio Oui/Non).
   - Dispositif `VALIDITE_CONVENTION` : `inscriptionEtatCivil`,
     `consentementLibreDesEpoux`, `respectReserveHereditaire`.
   - Dispositif `LIQUIDATION_DECES` : `clauseAttributionIntegrale`,
     `enfantsNonCommuns`, `valeurCommunauteEur`.
4. Submit POST → bandeau verdict :
   - `NUL` → bandeau rouge (préalable obligatoire — art. 1394 Cciv).
   - `CONTESTABLE` → bandeau orange (vice consentement / réserve héréditaire).
   - `VALIDE` + `actionRetranchementPossible=true` → bandeau orange (CAI +
     enfants 1er lit).
   - `VALIDE` sinon → bandeau navy/info.
5. Pré-fill IA via `aiData?: FamilleExtractedData` (no-op si absent).
6. Validation F-IA-03 : alertes de cohérence multi-sources sur
   `CONTRAT_NOTARIE` et `ENFANTS_NON_COMMUNS`.

## Cas d'erreur

- Workspace BE → bannière info (outil français uniquement).
- Form invalide → bouton désactivé + message inline (champs requis).
- 400 → MatSnackBar rouge avec message backend.
- 404 GET initial → mode formulaire (silencieux).

## Critères d'acceptation vérifiables

1. FR + dispositif `VALIDITE_CONVENTION` + tous critères → POST envoyé,
   bandeau navy/info "VALIDE".
2. `contratNotarie=false` → bandeau rouge "NUL", message inclut "1394 Cciv".
3. Dispositif `LIQUIDATION_DECES` + CAI + enfants non communs →
   `actionRetranchementPossible=true`, bandeau orange (action retranchement
   possible art. 1527 al. 2 Cciv), `partAttributionConjointPct=100` affichée.
4. Workspace BE → bannière info, aucun POST envoyé.
5. Pré-fill IA : `regimeMatrimonialDetecte='COMMUNAUTE_UNIVERSELLE'` →
   `dispositifAnalyse=VALIDITE_CONVENTION` par défaut + booléens IA si
   présents (`contratNotarieDetected`, `enfantsNonCommunsDetected`).
6. Changement manuel d'un champ pré-rempli → badge IA effacé.
7. F-IA-03 : avocat saisit `contratNotarie=false` alors qu'IA dit `true` →
   alerte `CONTRAT_NOTARIE` source `IA` visible.
8. F-IA-03 multi-sources : F96 + IA convergents → source `MULTI`.
9. Affichage valeurAttributionEur formatée en € avec separateurs FR.
10. Affichage formule + baseJuridique en JetBrains Mono via legalCitations.
11. CaseDashboardRefreshService.triggerRefresh() appelé après POST 200.
12. Tests Jest ≥ 12.

## Plan de test

- **Jest unit** (`communaute-universelle-section.component.spec.ts`,
  ≥ 12) : gate FR/BE, init GET 200/404, switch dispositif, form validation,
  POST + snackbar succès + refresh, POST erreur 400 → snackbar rouge,
  pré-fill IA (regime + booléens), F-IA-03 simple + MULTI, edit mode.

## Tables / endpoints / composants impactés

- **Composant** : `frontend/src/app/case-files/communaute-universelle-section/`
  (.ts/.html/.scss/.spec.ts).
- **Modèle** : `frontend/src/app/core/models/communaute-universelle.model.ts`.
- **Service** : `frontend/src/app/core/services/communaute-universelle.service.ts`.
- **Modification** : `decisional-tools-panel.component.ts` — import + entrée
  TOOL_REGISTRY `'F-FA-16-communaute-universelle'`.
- **Modification** : `divorce-accepte.model.ts` — ajout 2 champs optionnels
  IA dans `FamilleExtractedData` (`contratNotarieDetected`,
  `enfantsNonCommunsDetected`) — alignés avec l'extraction backend future.

## Nouveau pattern UI ou service partagé

Aucun. Réutilisation stricte de `CoherenceAlertBuilder`,
`CoherencePopoverTriggerDirective`, `CaseDashboardRefreshService`,
`LegalCitationsPipe`. Pattern de référence : `partage-judiciaire-section`
(SF-FA-17-02 PR #638). Aucune divergence visuelle.

## Hors périmètre

- Pré-fill backend IA réel (pipeline d'extraction `regimeMatrimonialDetecte`
  / `contratNotarieDetected` côté backend) — l'extraction IA des champs
  spécifiques communauté universelle est un sujet à part. Le composant
  est no-op gracieux si `aiData` absent.
- Belgique — hors scope (régime distinct CC belge).
- Calcul exact du retranchement (montant à reverser aux enfants 1er lit) —
  hors scope (backlog jumeau).

## Impact par domaine métier

Feature **sensible au domaine** :

- **Droit du travail / Immigration** : non applicable (gate domain backend).
- **Droit famille FR** : couvert par cette SF.
- **Droit famille BE** : non couvert (régime matrimonial distinct
  CC livre III) — bannière info BE + backlog jumeau si demande.

## Parité des domaines métier (outil de niveau 5)

Symétrique à SF-FA-16-01 backend :

- Droit du travail FR/BE : non applicable.
- Droit immigration FR/BE : non applicable.
- Droit famille FR : couvert.
- Droit famille BE : régime distinct — backlog jumeau si pertinent.

## Analyse de cohérence transversale

- **Pattern de référence frontend** : `partage-judiciaire-section`
  (SF-FA-17-02, PR #638) — outil famille FR avec verdict + multi-radios +
  F-IA-03 multi-sources. Strict alignement palette/typo/handlers.
- **Helper CoherenceAlertBuilder** : utilisé sans surcharge (skill
  `frontend-coherence-audit.md` §5).
- **Aucun composant partagé nouveau** — le composant suit les 5 exigences
  du blocage automatique (template canonique, palette, pré-fill IA,
  validation F-IA-03, TOOL_REGISTRY symétrique).

## Préoccupations transversales

- **Auth / Principal** : aucun changement.
- **Workspace context** : gate via `workspaceCountry='FRANCE'`.
- **Plans / limites** : non concerné.
- **Navigation / routing** : aucun ajout (composant intégré au panel
  existant).
- **Outil décisionnel métier** : nouvel outil. Scan effectué — pas de
  switch country dans le composant (gate dur via bannière info BE). Pas de
  conflit avec F-FA-15 (Récompenses) qui couvre la mécanique de récompenses
  entre époux des 4 régimes. Cet outil traite spécifiquement la validité du
  contrat + liquidation décès du régime communauté universelle.

## Self-check (skill `frontend-coherence-audit.md`)

1. Template canonique référencé : `partage-judiciaire-section` ✓
2. Palette navy/or/rouge (rouge réservé verdict NUL + risque retranchement) ✓
3. `<input type="number">` standard, pas de MatDatepicker ✓
4. Gate `workspaceCountry` via bannière info (pas masquage silencieux) ✓
5. `CaseDashboardRefreshService.triggerRefresh()` après POST 200 ✓
6. Pré-fill IA via `aiData` + signals provenance + handlers onChange ✓
7. F-IA-03 via `coherenceAlerts = computed(...)` + `CoherenceAlertBuilder` ✓
8. Entrée TOOL_REGISTRY symétrique (caseFileId, workspaceCountry, aiData,
   procedureChecks, aiQuestions, piecesManquantes) ✓
9. JetBrains Mono pour `baseJuridique` + `formule` ✓
10. MatSnackBar pour erreurs (pas alert/confirm) ✓
