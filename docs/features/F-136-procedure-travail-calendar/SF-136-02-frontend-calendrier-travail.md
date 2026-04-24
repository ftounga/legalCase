# Mini-spec — F-136 / SF-136-02 — Frontend calendrier procédural travail (FR + BE) + endpoint REST

## Identifiant

`F-136 / SF-136-02`

## Feature parente

`F-136` — Calendrier procédural prud'hommes / tribunal travail BE (`PRODUCT_SPEC.md` ligne 388, V7).

> Le précédent F-136 (Enrichissement massif des référentiels) est Terminé et vit dans `docs/features/F-136-massive-referentials-enrichment/`. La nouvelle F-136 (calendrier procédural travail) ré-utilise le numéro et vit dans `docs/features/F-136-procedure-travail-calendar/`.

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-136-02-frontend-calendrier-travail`

---

## Objectif

Livrer la couche endpoint REST + UI Angular qui permet à l'avocat (a) de choisir un type de procédure travail parmi les 6 codes seedés en SF-136-01, (b) de saisir une date déclencheur (saisine / signification / pourvoi), (c) d'afficher la timeline des jalons procéduraux avec délais en jours et dates cibles calculées. La SF inclut **la plomberie HTTP manquante côté backend** (le service Java existe mais aucun endpoint ne l'expose) et le composant timeline frontend.

---

## Contrat API (figé pour le frontend)

### `GET /api/v1/case-files/{caseFileId}/travail-procedure-jalons`

**Query params**

| Param | Type | Requis | Valeurs |
|-------|------|--------|---------|
| `typeProcedure` | string | oui | `PRUDHOMMES_FR`, `APPEL_CA_SOCIALE_FR`, `CASSATION_SOCIALE_FR`, `TRIBUNAL_TRAVAIL_BE`, `COUR_TRAVAIL_BE`, `CASSATION_BE` |
| `dateDeclencheur` | string ISO `YYYY-MM-DD` | non | si fourni → chaque jalon embarque `dateCible = dateDeclencheur + offsetDays` |

**Réponse `200`** : `TravailProcedureJalonResponse[]`
```json
[
  { "code": "PRUDHOMMES_FR", "label": "Convocation au bureau de conciliation et d'orientation (BCO)",
    "offsetDays": 45, "articleRef": "Code travail Art. R.1452-3", "dateCible": "2026-06-09" }
]
```

**Codes erreur**

| Code | Cas |
|------|-----|
| `400` | `typeProcedure` absent ou non parmi les 6 valeurs |
| `400` | `dateDeclencheur` mal formée (non ISO) |
| `404` | Case file introuvable / hors workspace utilisateur (isolation) |

**Notes** :
- Endpoint **transversal aux pays** : pas de filtrage par `workspace.country` côté backend (l'avocat peut consulter une procédure FR depuis un workspace BE — l'UI gate l'affichage avec une bannière info, pas un masquage silencieux).
- Pas de persistance — service stateless qui lit `LegalReferentialService.getTravailProcedureJalons(typeProcedure, country)` où `country` est dérivé du suffixe (`_FR` / `_BE`) du `typeProcedure`.

---

## Comportement attendu

### Backend (controller + DTO + tests IT)

1. `TravailProcedureController` (package `fr.ailegalcase.casefile`) expose `GET /api/v1/case-files/{caseFileId}/travail-procedure-jalons`.
2. Le controller vérifie l'accès workspace via le pattern existant (charge `CaseFile`, vérifie membre du workspace via `WorkspaceMemberRepository.findByUserAndPrimaryTrue`, retourne 404 sinon — symétrique à `AesEtudiantService.resolveCaseFile`).
3. Valide `typeProcedure` parmi les 6 codes — sinon `400`.
4. Dérive `country` (`FR` ou `BE`) du suffixe du `typeProcedure`.
5. Appelle `LegalReferentialService.getTravailProcedureJalons(typeProcedure, country)`.
6. Si `dateDeclencheur` fournie, calcule `dateCible = dateDeclencheur + offsetDays` pour chaque jalon.
7. Retourne `200` + liste de DTO `TravailProcedureJalonResponse(code, label, offsetDays, articleRef, dateCible)`.

### Frontend

1. Composant `<app-travail-procedure-section>` — header collapsible (palette navy/or, **pas rouge**).
2. Mode formulaire :
   - `<mat-select>` du type de procédure (groupé `FRANCE` / `BELGIQUE`) — gate selon `workspaceCountry` (`FRANCE` → 3 codes FR, `BELGIQUE` → 3 codes BE).
   - `<input type="date">` `dateDeclencheur` (optionnel).
   - Bouton "Calculer le calendrier" (désactivé si pas de type sélectionné).
3. Après clic : appel `TravailProcedureService.getJalons(...)` puis affichage timeline verticale (chaque jalon = bloc avec `offsetDays` en chip, label, `dateCible` si présente, `articleRef` en JetBrains Mono).
4. Pré-fill IA :
   - Lit `aiData?.procedureTravailDetectee` (champ qui n'existe pas encore dans `TravailExtractedData` — pré-fill = no-op gracieux : type-safe via `(aiData as any)?.procedureTravailDetectee` ou cast). Ce sera branché en SF future quand l'IA détectera le type de procédure.
   - Si présent ET valide ET pas déjà saisi → applique `typeProcedure` + `dateDeclencheur`, met `provenanceTypeProcedure.set('IA')`.
   - Badge `<mat-icon>auto_awesome</mat-icon> Pré-rempli depuis l'analyse` à côté du select.
5. `coherenceAlerts` computed : si `procedureChecks` contient `{ critereCode: 'TRAVAIL_PROCEDURE_TYPE', expectedValue: '...' }` ou `aiQuestions` similaires, afficher `<app-coherence-popover-trigger>` sur le select.
6. `CaseDashboardRefreshService.triggerRefresh()` après GET succès.
7. `MatSnackBar` pour erreurs HTTP (pas d'alert).
8. Bannière info (pas masquage) si `workspaceCountry` mismatch — message : *« Cet outil présente uniquement les procédures du pays du workspace. »*

### Cas d'erreur

| Situation | Comportement |
|-----------|--------------|
| `typeProcedure` non sélectionné | bouton Calculer désactivé |
| GET 400 (typeProcedure invalide) | snackbar erreur |
| GET 404 (case file hors workspace) | snackbar erreur |
| Liste vide retournée par l'API | message *« Aucun jalon disponible pour cette procédure »* |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils décisionnels timeline / calendrier** : `case-deadlines-section` (timeline générique workspace-saisis), `oqtf-avec-delai-section` (date pivot + délais simples). Pas d'autre composant qui consomme `LegalReferentialService.get*Jalons` côté frontend (immigration n'a pas encore d'équivalent UI — c'est le 1er composant timeline référentiel).
- [x] **Autres pays** : FR + BE en parité dans la même SF (les 6 codes sont seedés et exposés ensemble). Bannière info pour mismatch — ni masquage silencieux ni dual UI séparé.
- [x] **Autres domaines** : `DROIT_DU_TRAVAIL` cible. `DROIT_FAMILLE` (F-137) ouvrira le pendant famille via la même API pattern. `DROIT_IMMIGRATION` n'a pas encore de composant timeline référentiel — backlog VN.
- [x] **Autres UI patterns** : datepicker `<input type="date">` (convention F-155 SF-155-07 DIV-9 — pas MatDatepicker), palette navy/or (DESIGN_SYSTEM), badge IA `auto_awesome` (pattern SF-155-04), `<app-coherence-popover-trigger>` (F-IA-03-15c — pattern SF-155-06).

### Niveaux de vérification

- [x] **Modèle TypeScript** — `TravailProcedureJalon`, enum `TravailProcedureCode`, options groupées par pays.
- [x] **Service Angular** — `TravailProcedureService.getJalons(caseFileId, typeProcedure, dateDeclencheur?)`.
- [x] **Composant Angular** — `app-travail-procedure-section`.
- [x] **DTO backend** — `TravailProcedureJalonResponse` (record).
- [x] **Controller backend** — `TravailProcedureController` (REST GET).
- [x] **Tests d'intégration** — 6 cas d'IT (nominal FR, nominal BE, dateDeclencheur calculée, isolation workspace, validation typeProcedure invalide, validation dateDeclencheur invalide).
- [x] **Tests Jasmine frontend** — ≥10 cas (mount, GET success, gate FR/BE, pré-fill IA no-op, snackbar erreur, etc).

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Où le nouveau pattern pourrait-il être réutilisé ?** Le composant timeline référentiel est nouveau — F-137 (famille) le réutilisera (pattern symétrique). Pour éviter la dette de convergence, on suit strictement la convention `<input type="date">`, palette navy/or et badge `auto_awesome`. Pas de service partagé créé : `TravailProcedureService` est spécifique au domaine.
- [x] **Y a-t-il des patterns concurrents ?** Pas de timeline référentiel existant. La timeline générique `case-deadlines-section` cible les délais saisis manuellement — concept différent.
- [x] **Le nouveau service / endpoint peut-il servir à d'autres features ?** Le pattern peut servir à F-137. L'endpoint est case-file-scoped (vérifie isolation workspace), donc réutilisable par toute UI qui consomme un calendrier travail.
- [x] **Le nouveau composant a-t-il un équivalent design qu'il remplace ?** Non — création.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Composant timeline famille (`FAMILLE_PROCEDURE_JALONS`) | Oui | Backlog — F-137 (PRODUCT_SPEC ligne 389). Hors scope. |
| Composant timeline immigration (`IMMIGRATION_JALONS` UI) | Oui | Backlog VN — concept symétrique mais pas encore demandé fonctionnellement. À ouvrir si besoin métier. |
| Endpoint REST exposant le service Java | Oui | **Intégré dans cette SF** (plomberie manquante — le service Java existe mais aucun controller). |
| TOOL_REGISTRY entry pour panel F-IA-04 | Oui | Documentée ici, ajout effectif dans une PR orchestrateur séparée (consigne ticket). |
| Frontend FR + BE en parité | Oui | Intégré — la même UI gère les 2 pays via gate `workspaceCountry`. |

### Décision

- [x] Étendu à toutes les cibles applicables : backend endpoint + frontend composant + tests + DTO + service Angular + modèle TS.
- [x] Subfeature(s) parallèle(s) créée(s) pour les cibles restantes : F-137 (jumeau famille) déjà au backlog.
- [x] Backlog VN pour les cibles non prioritaires : composant timeline immigration UI (à ouvrir si besoin).
- [x] TOOL_REGISTRY entry à brancher en PR orchestrateur (snippet exact dans le rapport final).

---

## Critères d'acceptation

### Backend

- [ ] `TravailProcedureController` créé dans `fr.ailegalcase.casefile`, expose `GET /api/v1/case-files/{caseFileId}/travail-procedure-jalons`.
- [ ] DTO `TravailProcedureJalonResponse(String code, String label, int offsetDays, String articleRef, LocalDate dateCible)` dans le même package.
- [ ] Validation `typeProcedure` parmi les 6 codes — sinon `400`.
- [ ] Validation `dateDeclencheur` ISO si fournie — sinon `400`.
- [ ] Isolation workspace : `404` si caseFile inaccessible (pattern symétrique à `AesEtudiantService.resolveCaseFile`).
- [ ] Si `dateDeclencheur` fournie, `dateCible = dateDeclencheur + offsetDays` calculé pour chaque jalon.
- [ ] Tests `TravailProcedureControllerIT` avec ≥4 cas verts.

### Frontend

- [ ] `frontend/src/app/core/models/travail-procedure.model.ts` : `TravailProcedureJalon`, `TravailProcedureCode`, `TRAVAIL_PROCEDURE_OPTIONS_FR`, `TRAVAIL_PROCEDURE_OPTIONS_BE`.
- [ ] `frontend/src/app/core/services/travail-procedure.service.ts` : `getJalons(caseFileId, typeProcedure, dateDeclencheur?)`.
- [ ] `frontend/src/app/case-files/travail-procedure-section/travail-procedure-section.component.{ts,html,scss,spec.ts}` créés.
- [ ] Palette navy + or (pas de rouge dominant).
- [ ] `<input type="date">` natif (pas MatDatepicker).
- [ ] Gate `workspaceCountry` : bannière info pour mismatch — pas masquage.
- [ ] Pré-fill IA depuis `aiData?.procedureTravailDetectee` (no-op si champ absent).
- [ ] Badge `auto_awesome` quand pré-rempli.
- [ ] `coherenceAlerts` branché sur `procedureChecks` + `aiQuestions`.
- [ ] `CaseDashboardRefreshService.triggerRefresh()` après GET succès.
- [ ] `MatSnackBar` pour erreurs.
- [ ] ≥10 tests Jasmine verts.

### Global

- [ ] `cd backend && ./mvnw test -Dtest=TravailProcedureControllerIT` vert.
- [ ] `cd frontend && npx tsc --noEmit` sans erreur.
- [ ] `cd frontend && npm run test -- --watch=false` vert (suite spec du composant).

---

## Périmètre

### Hors scope (explicite)

- L'entrée `TOOL_REGISTRY` (`F-136-travail-procedure`) n'est **pas** ajoutée dans `decisional-tools-panel.component.ts` ici — sera branchée par une PR orchestrateur séparée. Snippet exact documenté dans le rapport final.
- Pas de mise à jour `docs/PRODUCT_SPEC.md` ici — sera faite par l'orchestrateur post-merge.
- Pas d'extension de `TravailExtractedData` pour ajouter `procedureTravailDetectee` — le pré-fill est en no-op gracieux. Sera branché en SF ultérieure (extension IA).
- Pas de jumeau famille (F-137 séparée).

---

## Technique

### Endpoint(s)

`GET /api/v1/case-files/{caseFileId}/travail-procedure-jalons?typeProcedure=X&dateDeclencheur=Y`

### Tables impactées

Aucune (lecture seule via `LegalReferentialService` → `legal_referentials` déjà seedée).

### Migration Liquibase

Aucune.

### Composants Angular

| Fichier | Rôle |
|---------|------|
| `core/models/travail-procedure.model.ts` | Types TS + options FR/BE |
| `core/services/travail-procedure.service.ts` | Wrapper HttpClient |
| `case-files/travail-procedure-section/*.{ts,html,scss,spec.ts}` | Section UI timeline |

---

## Plan de test

### Tests d'intégration backend (`TravailProcedureControllerIT`)

- [ ] `GET_nominalFr_returns200WithJalons` — `typeProcedure=PRUDHOMMES_FR`, sans `dateDeclencheur`. Liste de 5 jalons.
- [ ] `GET_nominalBe_returns200WithJalons` — `typeProcedure=TRIBUNAL_TRAVAIL_BE`. Liste de 4 jalons.
- [ ] `GET_withDateDeclencheur_computesDateCible` — `dateDeclencheur=2026-01-01`, vérifie `dateCible` du 1er jalon = `2026-01-01 + offsetDays`.
- [ ] `GET_invalidTypeProcedure_returns400`.
- [ ] `GET_otherWorkspace_returns404` — auth d'un workspace différent du caseFile.
- [ ] `GET_invalidDate_returns400` — `dateDeclencheur=not-a-date`.

### Tests Jasmine frontend (`travail-procedure-section.component.spec.ts`)

- [ ] mount sans erreur.
- [ ] FRANCE → 3 options FR exposées.
- [ ] BELGIQUE → 3 options BE exposées.
- [ ] click "Calculer" appelle service et flush 200 → timeline affichée.
- [ ] liste vide → message "Aucun jalon".
- [ ] erreur HTTP → snackbar appelée.
- [ ] pré-fill IA depuis `aiData.procedureTravailDetectee` → typeProcedure sélectionné + provenanceTypeProcedure='IA'.
- [ ] changement manuel typeProcedure → provenance effacée.
- [ ] toggle collapse expand/collapse correct.
- [ ] coherenceAlerts retourne entrée si `procedureChecks` divergent.
- [ ] dashboardRefresh.triggerRefresh appelé après succès.

### Isolation workspace

- [x] Vérifiée par `GET_otherWorkspace_returns404` (pattern symétrique aux autres controllers casefile).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale critique** — endpoint REST nouveau ajouté, mais pas de changement Auth / Workspace context / Plans / Navigation. Le pattern d'isolation est strictement copié des controllers existants (`AesEtudiantService.resolveCaseFile`).

### Impact par domaine métier

- **DROIT_DU_TRAVAIL** : oui — outil principal. FR + BE en parité.
- **DROIT_IMMIGRATION** : non — outil orthogonal.
- **DROIT_FAMILLE** : non concerné — F-137 livrera l'équivalent famille.

### Composants existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|-----------|--------|------------------------|
| `LegalReferentialService.getTravailProcedureJalons` | Aucun (consommateur en lecture) | Tests SF-136-01 restent verts |
| `case-file-detail.component` | Aucun (l'intégration au panel F-IA-04 / TOOL_REGISTRY est hors scope ici) | n/a |
| `decisional-tools-panel.component` | Aucun (TOOL_REGISTRY entry ajoutée plus tard) | n/a |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — composant non encore branché au panel, donc invisible dans le flow case-file-detail tant que TOOL_REGISTRY n'est pas mis à jour.

---

## TOOL_REGISTRY (à ajouter en PR orchestrateur séparée)

```ts
'F-136-travail-procedure': {
  toolId: 'F-136-travail-procedure',
  domain: 'DROIT_DU_TRAVAIL',
  countries: ['FRANCE', 'BELGIQUE'],
  selector: 'app-travail-procedure-section',
  inputs: (ctx) => ({
    caseFileId: ctx.caseFileId,
    workspaceCountry: ctx.workspaceCountry,
    aiData: ctx.travailExtractedData,
    procedureChecks: ctx.procedureChecks,
    aiQuestions: ctx.aiQuestions,
  }),
}
```

---

## Notes et décisions

- **Endpoint stateless** : pas de table `travail_procedure_analysis` créée — le calcul `dateDeclencheur + offsetDays` est trivial et peut être ré-exécuté à chaque ouverture (pas besoin de persistance).
- **Pas de gate `country` côté backend** : l'endpoint accepte les 6 codes pour les 2 pays — c'est la responsabilité de l'UI de filtrer. Cela permet de réutiliser l'endpoint pour des cas avancés (ex. avocat FR qui cite une procédure BE en référence).
- **Bannière info pour mismatch** : convention SF-155-07 (DIV-9) — pas de masquage silencieux, l'avocat voit l'outil mais sait qu'il n'est pas en phase avec son workspace.
- **Pré-fill IA en no-op** : volontaire — évite de forcer un changement de `TravailExtractedData` (sera branché plus tard quand le pipeline IA détectera réellement le type de procédure).
- **TOOL_REGISTRY différé** : par cohérence avec la pratique F-IA-04 — l'entrée est ajoutée par l'orchestrateur après merge des SF UI individuelles, pour éviter les conflits Git sur `decisional-tools-panel.component.ts`.
