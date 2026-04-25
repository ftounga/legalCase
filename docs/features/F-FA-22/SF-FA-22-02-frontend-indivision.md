# Mini-spec — F-FA-22 / SF-FA-22-02 Indivision post-communautaire (art. 815 Cciv) — FRONTEND

## Identifiant

`F-FA-22 / SF-FA-22-02`

## Feature parente

`F-FA-22` — Indivision post-communautaire (gestion, partage, mesures conservatoires).

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-22-02-frontend-indivision`

---

## Objectif

Implémenter le composant Angular `indivision-section` (model + service + composant + tests) consommant l'endpoint `POST/GET /api/v1/case-files/{caseFileId}/indivision` exposé par SF-FA-22-01, intégré au panel décisionnel F-IA-04 via `TOOL_REGISTRY` (tool id `F-FA-22-indivision`), avec gate FRANCE bannière info, pré-remplissage IA + validation F-IA-03, et rendu d'un verdict, score, montant indemnité d'occupation et 4 cartes de recommandations procédurales.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre le panneau outils sur un dossier FR DROIT_FAMILLE → l'outil "Indivision (art. 815 Cciv)" est visible.
2. Le composant fait `GET /api/v1/case-files/{caseFileId}/indivision` :
   - **200** → restaure la réponse et masque le form (`showForm = false`).
   - **404** → reste en mode formulaire ; pré-fill IA depuis `aiData?` si dispo.
3. L'avocat saisit :
   - `dateOrigineIndivision` : `<input type="date">` natif (ISO `YYYY-MM-DD`).
   - `natureBiens` : `mat-select multiple` (6 options : `IMMOBILIER`, `MOBILIER`, `COMPTES_BANCAIRES`, `TITRES_FINANCIERS`, `FONDS_COMMERCE`, `AUTRE`).
   - `valeurEstimeeTotaleEur` : input number ≥ 0.
   - `nbIndivisaires` : input number ≥ 2 (min 2 indivisaires).
   - `quotesPart` : input texte CSV (parsé en `number[]` — pourcentages, somme attendue ~100). Permet `60,40` ou `33.33; 33.33; 33.34`.
   - `tentativesPartageAmiable` : `mat-select multiple` (5 options : `PROPOSITION_NOTAIRE`, `MEDIATION`, `EXPERTISE_VALORISATION`, `LICITATION_AMIABLE`, `AUCUNE`).
   - 3 toggles boolean : `consentementPartageGlobal`, `occupationBienParUnIndivisaire`, `conflitOuvertEntreIndivisaires`, `demandeMesuresConservatoires`.
   - `indivisionDureeAnnees` : input number ≥ 0 (durée entière).
4. L'avocat clique "Analyser" → POST avec le payload complet → réponse stockée dans `result()`, `showForm = false`, `triggerRefresh()` appelé.
5. Affichage du résultat :
   - **Bannière verdict** : palette navy/or/rouge selon `verdictRecommandation`.
   - **Score** : `scoreEligibilitePartageJudiciaire` (0-100).
   - **4 cartes recommandations** :
     - Expertise notariale (recommandée / non, art. 815-2)
     - Licitation (recommandée / non, art. 1377 CPC)
     - Mesures conservatoires (oui / non, art. 815-6)
     - Médiation (préalable / non)
   - **Montant indemnité d'occupation** : `indemniteOccupationDueEur` en JetBrains Mono (art. 815-9 al. 2).
   - **Délai procédure partage judiciaire** : `delaiProcedurePartageJudiciaireMois` en JetBrains Mono.
   - **Base juridique + formule** en JetBrains Mono.
   - **Liste de messages** (puces) avec citations juridiques en `<code>`.
6. Bouton "Modifier" → revient au form.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Workspace BELGIQUE | Bannière info "Outil propre au droit français — voir l'outil BE équivalent (backlog F-FA-22-BE)" + form masqué |
| Date origine invalide / future | Bouton "Analyser" disabled |
| `nbIndivisaires < 2` | Bouton "Analyser" disabled |
| `natureBiens` vide | Bouton "Analyser" disabled |
| `quotesPart` ne somme pas ~100 (±0.5) | Bouton autorisé mais hint visuel ; backend rejette si vraiment divergent |
| HTTP 400 backend | `MatSnackBar` rouge avec message backend (`err.error.message`) |
| HTTP 5xx | `MatSnackBar` "Erreur lors de l'analyse" |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils famille FR** : F-FA-05 (partage immobilier), F-FA-15 (récompenses), F-FA-19 (autorité parentale, désaccords parentaux), F-FA-13 (révisions post-divorce). **Pattern aligné** : composant standalone + service + modèle, mat-select + form, `triggerRefresh()`, `MatSnackBar` erreurs, palette navy/or, JetBrains Mono pour `formule` / `baseJuridique`.
- [x] **Autres pays** : FR uniquement (régime art. 815 et s. Cciv). BE : bannière info renvoyée vers backlog (régime BE diffère — art. 577-2 anciens C. civ. BE, hors scope F-FA-22).
- [x] **Pattern UI partagé** : utilisera `CoherencePopoverTriggerDirective` + `CoherenceAlertBuilder` + `CoherenceAlert<F>` partagés (F-155-05). Multi-select + CSV liste éditable réutilise pattern `desaccords-parentaux-section` (F-FA-19-06). Pas de nouveau pattern UI introduit.
- [x] **Refresh dashboard (F-IA-02)** : oui, déclenché après POST réussi.
- [x] **Pré-remplissage IA** : graceful no-op — aucune extraction IA spécifique indivision côté `FamilleExtractedData` à ce jour. Pré-fill optionnel pour `dateOrigineIndivision` (depuis `dateSeparation` réutilisé), `natureBiens` (heuristique si patrimoine connu), `occupationBienParUnIndivisaire` (depuis `logementCommunDetected` ou flag spécifique). Sans nouveau champ IA dédié, on reste minimal sur SF-FA-22-02.
- [x] **Validation F-IA-03** : alerte cohérence sur `DATE_ORIGINE_INDIVISION` quand l'IA détecte une date divergente, et `OCCUPATION_BIEN` quand l'IA détecte un logement commun et l'avocat n'a pas coché.

### Cas spécifique : nouvel outil décisionnel

- [x] **Cohérence F-IA-03** : oui, sur 2 fields :
  - `DATE_ORIGINE_INDIVISION` (sources : IA `aiData.dateSeparation`, F96 `FA22_DATE_ORIGINE`, QUESTION_IA, PIECE_MANQUANTE).
  - `OCCUPATION_BIEN` (sources : IA `aiData.logementCommunDetected`, F96 `FA22_OCCUPATION`, PIECE_MANQUANTE).
- [x] **Refresh dashboard (F-IA-02)** : `triggerRefresh()` injecté optionnel + appelé dans `next:` du POST.
- [x] **Pré-remplissage IA** : `prefillFromAi()` invoqué `ngOnInit` + `ngOnChanges` (si `aiData` change avant première résolution) avec signals `provenanceDateOrigine`, `provenanceOccupation`.
- [x] **Persistance des inputs** : la response contient les valeurs du request → `applyPersistedResult()` rejoue tous les fields après GET 200.
- [x] **Masquage conditionnel** : gate FRANCE via bannière info (pas de masquage).
- [x] **Alertes après calcul (anti-bug SF-IA-03-12)** : `coherenceAlerts` gated par `showForm()` strict.

---

## Critères d'acceptation

1. Mount FR avec dossier sans analyse → `GET 404` → form visible.
2. Mount FR avec analyse persistée → `GET 200` → cartes verdict visibles, form masqué.
3. Mount BE → bannière info "outil FR uniquement" visible, form masqué.
4. `formValid` exige `dateOrigineIndivision` ISO valide + `natureBiens` non vide + `nbIndivisaires >= 2` + `quotesPart` non vide + `valeurEstimeeTotaleEur >= 0`.
5. Click "Analyser" → POST émis avec payload complet → result enregistré + `showForm = false` + `triggerRefresh()` appelé.
6. POST 400 → snackBar erreur visible.
7. Click "Modifier" sur un résultat → `showForm = true`.
8. Pré-fill IA : `aiData = { dateSeparation: '2025-01-15', logementCommunDetected: true }` → `dateOrigineIndivision = '2025-01-15'` + `occupationBienParUnIndivisaire = true` + badges "Pré-rempli depuis l'analyse".
9. Modifier manuellement la date → badge IA disparaît (`provenanceDateOrigine = null`).
10. Divergence IA `dateSeparation = '2025-01-15'` vs avocat `'2024-06-01'` → alerte F-IA-03 affichée avec popover.
11. `coherenceAlerts` vide quand `showForm() === false` (anti-bug SF-IA-03-12).

## Plan de test minimal

≥ 11 tests Jest :
- `mount FR → GET 404 → form visible`
- `mount FR → GET 200 → result + masque form`
- `mount BE → bannière + form masqué`
- `formValid : exige date + natureBiens + nbIndivisaires + quotesPart`
- `parseQuotesPart : '60,40' → [60, 40]`
- `analyser : POST émis avec payload complet`
- `analyser succès → triggerRefresh appelé`
- `analyser erreur 400 → snackBar`
- `prefillFromAi : dateSeparation → provenanceDateOrigine = 'IA'`
- `onDateOrigineChange manuel : provenanceDateOrigine → null`
- `coherenceAlerts : divergence IA vs avocat sur DATE_ORIGINE → alerte`
- `coherenceAlerts : showForm=false → vide`

---

## Tables / endpoints / composants impactés

### Endpoints (consommés)
- POST `/api/v1/case-files/{caseFileId}/indivision` (SF-FA-22-01)
- GET `/api/v1/case-files/{caseFileId}/indivision` (SF-FA-22-01)

### Tables
- Aucune création (consomme `indivision_analyses` via API).

### Visibility rule
- Existante (créée par SF-FA-22-01) : tool id `F-FA-22-indivision`, ALWAYS_ON FR + DROIT_FAMILLE.

### Composants Angular
- `frontend/src/app/core/models/indivision.model.ts` (NOUVEAU)
- `frontend/src/app/core/services/indivision.service.ts` (NOUVEAU)
- `frontend/src/app/case-files/indivision-section/indivision-section.component.{ts,html,scss,spec.ts}` (NOUVEAU)
- `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` (MODIF — entrée TOOL_REGISTRY)

---

## Hors périmètre

- Régime BE de l'indivision (backlog futur F-FA-22-BE).
- Édition individuelle d'une recommandation (analyse atomique).
- Export PDF (couvert par F-148).
- Pré-fill IA avancé sur `nbIndivisaires`, `valeurEstimeeTotaleEur` (pas d'extraction IA dédiée pour l'instant).

---

## Contrat API (importé de SF-FA-22-01)

```typescript
export type NatureBien = 'IMMOBILIER' | 'MOBILIER' | 'COMPTES_BANCAIRES' | 'TITRES_FINANCIERS' | 'FONDS_COMMERCE' | 'AUTRE';
export type TentativePartage = 'PROPOSITION_NOTAIRE' | 'MEDIATION' | 'EXPERTISE_VALORISATION' | 'LICITATION_AMIABLE' | 'AUCUNE';
export type VerdictIndivision = 'PARTAGE_AMIABLE_POSSIBLE' | 'PARTAGE_JUDICIAIRE_RECOMMANDE' | 'LICITATION_REQUISE' | 'MEDIATION_PREALABLE';

export interface IndivisionRequest {
  dateOrigineIndivision: string;
  natureBiens: NatureBien[];
  valeurEstimeeTotaleEur: number;
  nbIndivisaires: number;
  quotesPart: number[];
  tentativesPartageAmiable: TentativePartage[];
  consentementPartageGlobal: boolean;
  occupationBienParUnIndivisaire: boolean;
  indivisionDureeAnnees: number;
  demandeMesuresConservatoires: boolean;
  conflitOuvertEntreIndivisaires: boolean;
}
export interface IndivisionResponse {
  caseFileId: string;
  // input echoed
  scoreEligibilitePartageJudiciaire: number;
  verdictRecommandation: VerdictIndivision;
  indemniteOccupationDueEur: number;
  expertiseNotarialeRecommandee: boolean;
  licitationRecommandee: boolean;
  delaiProcedurePartageJudiciaireMois: number;
  baseJuridique: string;
  formule: string;
  messages: string[];
  country: 'FRANCE';
}
```

---

## Impact par domaine métier

Cette feature est **spécifique au droit de la famille FR** :
- **Droit du travail** : non applicable.
- **Immigration** : non applicable.
- **Droit famille FR** : cœur fonctionnel.
- **Droit famille BE** : non couvert (régime juridique différent — bannière info).

Aucun risque d'asymétrie 3 domaines.

## Préoccupations transversales

- Auth / Principal : aucune modification.
- Workspace context : standard (utilise `caseFileId` qui est filtré côté backend).
- Plans / limites : aucun.
- Routing / navigation : N/A (composant intégré au panel F-IA-04 sur la page case-file-detail existante).
- Outil décisionnel métier : nouveau frontend, isolé.

---

## Pattern de référence

- **Template canonique** : `harcelement-licenciement-nul-section` (F-DT-11-02) — référence pré-fill IA + F-IA-03.
- **Multi-select + CSV liste + 5 fields IA** : `desaccords-parentaux-section` (F-FA-19-06) — pattern multi-select Tentatives + ages CSV.
- **Form famille avec montants + palette navy/or** : `recompenses-section` (F-FA-15-02) — palette navy/or, layout, typographie.

## Self-check pré-commit

- ≥ 1 import `CoherenceAlertBuilder`
- ≥ 1 import `coherence-alert-builder`
- ≥ 2 occurrences `appCoherencePopover`
- ≥ 1 méthode `prefillFromAi`
- ≥ 1 occurrence `auto_awesome`
- ≥ 4 occurrences `provenance`
- ≥ 1 occurrence `coherenceAlerts`
- ≥ 2 handlers `on*Change`
- 0 interface locale `CoherenceAlert` (utilise le partagé)
