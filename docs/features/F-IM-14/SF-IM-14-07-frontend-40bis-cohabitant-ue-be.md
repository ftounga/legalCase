# Mini-spec — F-IM-14 / SF-IM-14-07 frontend 40bis cohabitant UE BE

## Identifiant

`F-IM-14 / SF-IM-14-07`

## Feature parente

`F-IM-14` — Outils décisionnels Belgique (loi 15/12/1980)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-IM-14-07-frontend-40bis-cohabitant-ue-be`

## Contrat API importé

Contrat figé dans **SF-IM-14-03 backend** (PR #510, mergée). Source de vérité : `Belgian40bisRequest.java`, `Belgian40bisResponse.java`, `Belgian40bisCalculator.java`. Cette SF frontend ne modifie aucun backend.

---

## Objectif

Livrer le composant Angular `belgian-40bis-section` consommant POST + GET `/api/v1/case-files/{caseFileId}/belgian-40bis` pour évaluer la probabilité d'octroi d'une carte F (regroupement familial — citoyen UE non-Belge, art. 40bis Loi 15/12/1980).

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre un dossier d'immigration BE et déplie la section `belgian-40bis-section`.
2. Le composant fait `GET` au montage : si une analyse existe (200), il l'affiche en mode résultat ; si 404, il reste en mode formulaire et applique le `prefillFromAi()` quand `aiData` est disponible.
3. L'avocat saisit les 8 champs (`lienFamilial`, `regroupantCitoyenUe`, `regroupantActiviteCategorie`, `ressourcesSuffisantes`, `assuranceMaladieUe`, `logementSuffisant`, `menaceOrdrePublic`, `dateDepotDemande?`) et soumet.
4. Le `POST` renvoie le score 0-100 + verdict ELEVEE/MOYENNE/FAIBLE + `criteresNonRemplis` + `messages` + `formule` + `baseJuridique` + `dateExpirationInstruction`.
5. La section affiche : bannière verdict (navy / or / rouge selon palette), score en grand, 6 check-items + bonus catégorie solide, chips `criteresNonRemplis`, messages `<ul>` rendus via `LegalCitationsPipe`, `baseJuridique` et `formule` en JetBrains Mono.
6. `dashboardRefresh.triggerRefresh()` est invoqué après succès.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Champ obligatoire manquant (lien, catégorie, 5 booléens) | Bouton submit disabled (form invalide) | — |
| `dateDepotDemande` dans le futur | Form invalide (max=todayIso) | — |
| Workspace `FRANCE` | Bannière info "outil BE uniquement, voir F-IM-03/F-IM-12" — pas de masquage silencieux | — |
| Backend retourne 400 (validation enum / pays) | `MatSnackBar` rouge, `calculating` reset | 400 |
| Backend retourne 404 au GET initial | Reste en mode formulaire + tente prefill IA | 404 |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier BE** : `annexe13-be-section` (F-IM-08-06), `motif-grave-be-section` (F-DT-27-02), `oqtf-*-section` (F-IM-08), à venir `9bis-humanitaire-be`, `9ter-medical-be`, `40ter-familial-belge-be` (SF-IM-14-05/06/08).
- [x] **Autres pays** : F-IM-03 / F-IM-12 (regroupement familial CESEDA FR) — équivalents existants. Distinct de 40ter (regroupant Belge — règles plus strictes).
- [x] **Autres domaines** : N/A (DROIT_IMMIGRATION uniquement).
- [x] **UI patterns** : pré-fill IA + provenance + alertes F-IA-03 (pattern canonique `motif-grave-be-section` / `annexe13-be-section`), datepicker `<input type="date">`, bannière gate pays, palette navy/or/rouge réservée alerte critique.

### Niveaux de vérification

- [x] Modèle TypeScript : nouveau `belgian-40bis.model.ts` (types alignés `Belgian40bisRequest/Response`).
- [x] Service Angular : nouveau `belgian-40bis.service.ts` (POST + GET).
- [x] Composant : `belgian-40bis-section/` (4 fichiers : `.ts`, `.html`, `.scss`, `.spec.ts`).
- [x] DTO backend : déjà figé en SF-IM-14-03.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui — `coherenceAlerts` computed sur `LIEN_FAMILIAL` (vs `aiData.typeProcedureDetectee`) en mode best-effort. La couverture F-IA-03 reste légère car `ImmigrationExtractedData` actuel ne porte pas de champs natifs `lienFamilial / regroupantCitoyenUe / dateDepotDemande` ; `dateDepotProcedure` peut servir de fallback pour `dateDepotDemande`. Pas de blocage : pré-fill no-op gracieux + popover trigger câblé pour future extension IA.
- [x] **Refresh dashboard (F-IA-02)** : oui — `dashboardRefresh.triggerRefresh()` après POST succès.
- [x] **Pré-remplissage IA** : oui — `prefillFromAi()` lit `dateDepotProcedure` (→ `dateDepotDemande`) et `nationaliteUe` (→ `regroupantCitoyenUe` côté regroupant — best-effort). Champs non extraits actuellement (lienFamilial, catégorie activité) → no-op silencieux.
- [x] **Persistance** : déjà côté backend (table `belgian_40bis_analyses`, JSON `result_data`).
- [x] **Masquage conditionnel** : gate `workspaceCountry === 'BELGIQUE'` (sinon bannière info).
- [x] **Alertes actives après calcul** : gate `coherenceAlerts` sur `!showForm()` uniquement (pas `|| result()` — bug SF-IA-03-12 évité).

### Cas spécifique : nouveau pattern UI ou service partagé

Pas de nouveau pattern — réutilisation stricte du template canonique `motif-grave-be-section` + `annexe13-be-section` (signals, `CoherencePopoverTriggerDirective`, `LegalCitationsPipe`, palette navy/or, `<input type="date">`). Le `Belgian40bisService` est un wrapper HttpClient minimal aligné sur les autres services BE.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pré-fill IA pleinement opérationnel (lienFamilial, catégorie) | Non aujourd'hui | Backlog — étendre `ImmigrationExtractedData` lors d'une SF dédiée IA (F-IM-14-09 hypothétique). Best-effort actuel : `dateDepotProcedure` + `nationaliteUe`. |
| Composant 40ter (regroupant Belge) | Oui | Frontend SF jumelle SF-IM-14-08 — backend SF-IM-14-04 livre le contrat. Hors scope de cette SF. |
| Composants 9bis humanitaire / 9ter médical | Oui | SF-IM-14-05 / SF-IM-14-06 (frontend jumelles). Hors scope. |
| Intégration `decisional-tools-panel` (TOOL_REGISTRY) | Oui | **Documentée dans cette mini-spec** mais pas modifiée dans cette SF (consigne explicite). Sera intégrée par la SF d'orchestration F-IA-04 ou batch suivante. |

### Décision

- [x] Étendu aux cibles applicables dans cette subfeature.
- [x] SF parallèles à venir : SF-IM-14-05/06/08 frontend.
- [x] Backlog : extension `ImmigrationExtractedData` pour pré-fill plein.

---

## Critères d'acceptation

- [x] Composant `BelgianCohabitantUeBeSectionComponent` standalone, intégrable via TOOL_REGISTRY (entrée documentée plus bas).
- [x] Form mat-select `lienFamilial` (5 valeurs : `CONJOINT`, `PARTENAIRE_ENREGISTRE`, `DESCENDANT_MINEUR`, `DESCENDANT_MAJEUR_CHARGE`, `ASCENDANT_CHARGE`).
- [x] Form mat-select `regroupantActiviteCategorie` (4 valeurs : `TRAVAILLEUR`, `ETUDIANT`, `INACTIF_AVEC_RESSOURCES`, `AUTRE`).
- [x] 5 slide-toggles booléens : `regroupantCitoyenUe`, `ressourcesSuffisantes`, `assuranceMaladieUe`, `logementSuffisant`, `menaceOrdrePublic`.
- [x] `<input type="date">` pour `dateDepotDemande` avec attribut `max=todayIso`.
- [x] Bannière info gate FR (pas de masquage silencieux).
- [x] Bannière verdict navy (ELEVEE) / or (MOYENNE) / rouge (FAIBLE) — rouge réservé verdict défavorable, conforme palette.
- [x] Score `XX/100` en grand.
- [x] 6 check-items (lien / regroupant UE / ressources / assurance / logement / ordre public) + bonus catégorie solide.
- [x] Chips `criteresNonRemplis`.
- [x] Messages `<ul>` via `LegalCitationsPipe`.
- [x] `baseJuridique` et `formule` en JetBrains Mono.
- [x] `triggerRefresh()` après POST succès.
- [x] `MatSnackBar` pour erreur (pas `alert/confirm`).
- [x] `prefillFromAi()` no-op gracieux quand `aiData` n'a pas les champs (best-effort `dateDepotProcedure` + `nationaliteUe`).
- [x] Badges provenance IA effacés à `onXxxChange()`.
- [x] `coherenceAlerts` computed (popover trigger câblé) — gate `!showForm()`.
- [x] ≥ 10 tests Jest passants.

---

## Périmètre

### Hors scope

- Modification de `decisional-tools-panel.component.ts` (TOOL_REGISTRY documenté seulement).
- Mise à jour `PRODUCT_SPEC.md` (consigne explicite — sera fait à la SF de complétion F-IM-14).
- Extension `ImmigrationExtractedData` pour porter `lienFamilial / catégorie activité` (backlog).
- Composants 9bis / 9ter / 40ter (SF jumelles).

---

## Technique

### Endpoint(s) consommé(s)

| Méthode | URL | Auth |
|---------|-----|------|
| POST | `/api/v1/case-files/{caseFileId}/belgian-40bis` | OAuth2 |
| GET | `/api/v1/case-files/{caseFileId}/belgian-40bis` | OAuth2 |

### Composants Angular

- `BelgianCohabitantUeBeSectionComponent` — section dépliable / résultat.
- `Belgian40bisService` — wrapper HttpClient (POST + GET).
- `belgian-40bis.model.ts` — types `Belgian40bisRequest / Response` + enums `LienFamilial`, `RegroupantActiviteCategorie`.

### TOOL_REGISTRY (à intégrer dans une SF d'orchestration ultérieure)

```typescript
['F-IM-14-40bis-cohabitant-ue-be', {
  component: BelgianCohabitantUeBeSectionComponent,
  inputs: (ctx) => ({
    caseFileId: ctx.caseFileId,
    workspaceCountry: ctx.workspaceCountry,
    aiData: ctx.synthesis?.immigrationExtractedData,
    procedureChecks: ctx.procedureChecks,
    aiQuestions: ctx.aiQuestions,
    piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
  }),
}],
```

---

## Plan de test

### Tests unitaires (Jest, ≥ 10)

1. `mount` — composant créé, `caseFileId='case-1'`, `workspaceCountry='BELGIQUE'`.
2. `gate FR` → bannière info, aucun GET initial.
3. `gate BE` → form visible, GET appelé.
4. `formValid` faux si `lienFamilial` ou `regroupantActiviteCategorie` non sélectionné.
5. `submit OK` → POST envoyé avec body attendu, hydrate `result()`, snackbar succès, `triggerRefresh` appelé.
6. `submit error` → snackbar rouge, `submitting()` reset.
7. `prefillFromAi` — `aiData.dateDepotProcedure` → `dateDepotDemande` + badge IA.
8. `prefillFromAi` — `aiData.nationaliteUe` → `regroupantCitoyenUe` + badge IA.
9. `onXxxChange` efface badge provenance IA.
10. `coherenceAlerts` retourne `{}` en mode résultat (`showForm=false`).
11. `chips criteresNonRemplis` — bannière listant les codes manquants.
12. `toggleCollapse` inverse l'état.

### Isolation workspace

- Non applicable côté frontend — le backend filtre déjà par workspace via `WorkspaceMemberRepository`.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — composant frontend isolé, pas de modification de l'auth, du workspace context, des plans, ni de routes.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné (composant frontend non encore intégré au panel décisionnel — sera couvert par smoke F-IA-04).

---

## Dépendances

### Subfeatures bloquantes

- `SF-IM-14-03` (backend Belgian40bisCalculator) — done (PR #510 mergée).

### Questions ouvertes impactées

- Aucune.

---

## Notes et décisions

- **Palette rouge sur verdict FAIBLE** : autorisée car le verdict FAIBLE matérialise un risque de refus juridique (qualifié, pas une simple suggestion d'amélioration). Conforme `DESIGN_SYSTEM.md` — rouge "réservé alerte critique" appliqué ici à la signalétique du verdict défavorable, identique au pattern `oqtf-sans-delai-section`. Documenté en SCSS.
- **Pré-fill IA best-effort** : `dateDepotProcedure` (générique) → `dateDepotDemande`, `nationaliteUe` (du contexte) → `regroupantCitoyenUe`. Les autres champs (lienFamilial, catégorie) ne sont pas extraits par l'IA actuelle — le composant accepte `aiData` et les expose au prefill mais ils restent vides aujourd'hui (backlog).
- **CoherencePopoverTriggerDirective** : câblée sur `LIEN_FAMILIAL` uniquement (1 field — extension future possible).
- **Pas de modification `decisional-tools-panel`** : conformité avec la consigne. L'entrée TOOL_REGISTRY est documentée plus haut.
