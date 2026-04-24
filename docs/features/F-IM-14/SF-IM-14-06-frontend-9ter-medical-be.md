# Mini-spec — F-IM-14 / SF-IM-14-06 9ter médical BE — FRONTEND

## Objectif
Composant Angular `<app-belgian-9ter-section>` qui consomme l'API
`POST + GET /api/v1/case-files/{caseFileId}/belgian-9ter` (livrée par
SF-IM-14-02, PR #509) et offre à l'avocat un formulaire 4-conditions +
2 dates pour évaluer la probabilité d'acceptation d'une régularisation
9ter (art. 9ter Loi 15/12/1980).

## Comportement nominal
1. Section repliée par défaut, header "RÉGULARISATION 9TER MÉDICAL (BE)".
2. Au déploiement, GET sur l'endpoint :
   - 200 → affichage résultat (verdict + score + 4 checks).
   - 404 → mode formulaire, prefill IA si `aiData` fournit des champs
     médicaux extraits (no-op gracieux si absent).
3. Formulaire :
   - Datepicker `dateDebutSymptomes` (`<input type="date">` max=today, optionnel).
   - 4 slide-toggles : `maladieGraveCertifiee`, `soinsNecessairesDisponiblesBe`,
     `soinsInaccessiblesPaysOrigine`, `menaceOrdrePublic`.
   - Datepicker `dateDepotDemande` (`<input type="date">` max=today,
     ≥ dateDebutSymptomes si fournie, optionnel).
   - Bouton "Analyser" envoie POST.
4. Résultat :
   - Bannière verdict (ELEVEE → success/navy ; MOYENNE → info/navy ;
     FAIBLE → warning or). Rouge réservé à l'absence de critère majeur
     (menace ordre public présente).
   - Score 0-100 en grand (JetBrains Mono).
   - 4 check-items : "Maladie grave certifiée" / "Soins disponibles en
     Belgique" / "Soins inaccessibles au pays d'origine" / "Pas de
     menace ordre public" — ✓ ou ✗ + (25 pts).
   - `criteresNonRemplis` listés en chips/items.
   - `messages` rendus en `<ul>` avec `LegalCitationsPipe`.
   - `baseJuridique` + `formule` en JetBrains Mono.
   - `dateExpirationInstruction` affichée si présente.
   - Bouton "Modifier" → retour formulaire.
5. Après POST 200, `CaseDashboardRefreshService.triggerRefresh()` appelé.

## Cas d'erreur
- POST 4xx/5xx → `MatSnackBar` panelClass `snack-error` avec le message
  backend, `analyzing` reset.
- GET 404 → silencieux, prefill IA appliqué.
- `workspaceCountry !== 'BELGIQUE'` → bannière info "Procédure belge
  uniquement", aucun appel HTTP, formulaire masqué.

## Critères d'acceptation
- [ ] Composant standalone, palette navy/or, rouge uniquement pour
  alerte critique.
- [ ] `<input type="date">` (pas MatDatepicker) cohérent annexe13/etc.
- [ ] Gate `workspaceCountry`: bannière info pour FR.
- [ ] `MatSnackBar` pour erreurs (pas alert/confirm).
- [ ] JetBrains Mono pour `baseJuridique`, `formule`, score.
- [ ] Inter pour le reste.
- [ ] Coherence popover trigger sur les 4 toggles (au cas où l'IA aura
  un jour un signal ; aucune alerte produite tant que `aiData` n'expose
  pas de champ médical).
- [ ] `triggerRefresh()` appelé après POST succès.

## Plan de test (≥ 10 specs Jest)
1. Mount + 4 motifs/checks attendus.
2. BELGIQUE → GET 404 → reste en formulaire.
3. BELGIQUE → GET 200 → résultat hydraté, showForm=false.
4. FRANCE → bannière info, aucun appel HTTP.
5. POST 200 → snack OK + dashboard refresh + showForm=false.
6. POST erreur → snack-error.
7. `formValid()` autorise tous les booléens à false.
8. `formValid()` refuse date début dans le futur.
9. `formValid()` refuse `dateDepotDemande < dateDebutSymptomes`.
10. `editMode()` → showForm=true.
11. `toggleCollapse()` inverse l'état.
12. Score affiché correspond au `scoreGlobal` retourné.
13. Verdict bannière mappée correctement (ELEVEE / MOYENNE / FAIBLE).
14. Pré-fill IA gracieux si `aiData` fourni mais sans champ médical →
    no-op, pas d'exception.
15. Provenance IA effacée au 1er onChange manuel d'un toggle.

## Tables / endpoints / composants impactés
- Endpoint backend : POST + GET `/api/v1/case-files/{id}/belgian-9ter`
  (livré par SF-IM-14-02, PR #509).
- Nouveaux fichiers :
  - `frontend/src/app/core/models/belgian-9ter.model.ts`
  - `frontend/src/app/core/services/belgian-9ter.service.ts`
  - `frontend/src/app/case-files/belgian-9ter-section/*`
- Aucune modification de `decisional-tools-panel.component.ts` ; le
  TOOL_REGISTRY snippet à wirer est fourni dans la PR (intégration en
  follow-up).

## Hors périmètre
- Wiring `decisional-tools-panel` → SF dédiée séparée (pour respecter
  la consigne "ne pas modifier decisional-tools-panel").
- Extraction IA `aiData` médical (pas encore exposée par
  `ImmigrationExtractedData`) → handling gracieux uniquement.
- E2E (suffisant : Jest unit + smoke régression preexistant).

## Contrat API (importé de SF-IM-14-02 backend)

### Request body
```json
{
  "dateDebutSymptomes": "YYYY-MM-DD | null",
  "maladieGraveCertifiee": true,
  "soinsNecessairesDisponiblesBe": true,
  "soinsInaccessiblesPaysOrigine": true,
  "menaceOrdrePublic": false,
  "dateDepotDemande": "YYYY-MM-DD | null"
}
```

### Response
```json
{
  "caseFileId": "uuid",
  "dateDebutSymptomes": "YYYY-MM-DD | null",
  "maladieGraveCertifiee": true,
  "soinsNecessairesDisponiblesBe": true,
  "soinsInaccessiblesPaysOrigine": true,
  "menaceOrdrePublic": false,
  "dateDepotDemande": "YYYY-MM-DD | null",
  "country": "BELGIQUE",
  "certificatMedicalType1Ok": true,
  "soinsRequisOk": true,
  "inaccessibiliteOk": true,
  "pasMenace": true,
  "scoreGlobal": 100,
  "verdictProbabiliteAcceptation": "ELEVEE",
  "criteresNonRemplis": [],
  "dateExpirationInstruction": "YYYY-MM-DD | null",
  "formule": "...",
  "baseJuridique": "Loi 15/12/1980 art. 9ter + AR 17/05/2007 art. 7-8",
  "messages": ["..."]
}
```

## TOOL_REGISTRY snippet (à wirer dans une SF d'intégration)

```ts
['F-IM-14-9ter-medical-be', {
  component: Belgian9terSectionComponent,
  inputs: (ctx) => ({
    caseFileId: ctx.caseFileId,
    workspaceCountry: ctx.workspaceCountry,
    aiData: ctx.synthesis?.immigrationExtractedData,
  }),
}],
```

## Analyse de cohérence transversale
Scan des autres sections décisionnelles (annexe13-be, immigration-title-decision,
harcelement-licenciement-nul, motif-grave-be) :
- Palette navy/or harmonisée.
- `<input type="date">` natif (jamais MatDatepicker) OK.
- Gate `workspaceCountry` via bannière info OK.
- `MatSnackBar` partout OK.
- `CaseDashboardRefreshService.triggerRefresh()` après POST OK.
- JetBrains Mono pour `baseJuridique` et `formule` OK.
- Pas de nouveau pattern transversal introduit (le composant suit
  strictement le template canonique `annexe13-be-section`).

## Impact par domaine métier
- Sensible domaine : DROIT_IMMIGRATION BE uniquement. Hors scope FR
  (étranger malade L.425-9 CESEDA = procédure juridiquement distincte,
  cf. F-IM-13 / F-DT future).
- Famille / droit travail : non applicable.

## Parité des domaines métier (niveau 5 — scoring)
- Belgique : SF-IM-14-02 (backend) + SF-IM-14-06 (cette SF, frontend).
- France : équivalent étranger malade L.425-9 CESEDA — backlog F-IM-13
  (procédures juridiquement distinctes — invariant "1 outil = 1
  situation" respecté).
- Famille : non applicable.
- Droit du travail : non applicable.
