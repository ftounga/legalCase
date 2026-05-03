---
feature: F-177
subfeature: SF-177-14
title: Réparer la propagation de CaseDashboardRefreshService dans les outils ouverts en MatDialog
domain: Frontend (transversal 3 domaines × 2 pays)
estimation: 1,5-2 h
status: Ready to dev
---

# SF-177-14 — Wiring `CaseDashboardRefreshService` dans les MatDialog tools

## Objectif

Réparer le bug silencieux qui empêche la mise à jour automatique du tableau de bord décisionnel après qu'un avocat exécute un outil ouvert en MatDialog (calcul / save). Cause racine : `MatDialog` ouvre les composants outils dans un **injector tree séparé** de `CaseFileDetailComponent`, donc `@Optional() CaseDashboardRefreshService` injecté côté tool reçoit `null` → `?.triggerRefresh()` est silencieusement no-op. **Ce bug s'applique à tous les outils ouverts en modal depuis SF-177-02**, pas seulement ceux remontés par l'utilisateur (Droit au travail, Titre de séjour recommandé, Changement de statut). En complément, 3 composants ne déclarent même pas l'injection et appellent jamais `triggerRefresh()` (audit) — ils sont corrigés dans la même SF.

## Contexte (origine du bug)

- F-124 (~2026-03) introduit `CaseDashboardRefreshService` (Subject scope `case-file-detail`) avec pattern F-IA-02-03 : chaque outil appelle `triggerRefresh()` dans son handler `next:` de save → dashboard et panel rechargent leur état.
- F-177 SF-177-02 (~2026-04) bascule l'ouverture des outils de l'expand inline vers `MatDialog`. **Régression non détectée** : le dialog s'ouvre dans le root injector (pas de `viewContainerRef` passé à `dialog.open()`), donc les tools ouverts en modal n'ont plus accès au service scope `case-file-detail`.
- Audit 2026-05-03 (cette SF) : tous les composants tools appellent `triggerRefresh()` consciencieusement, **mais** :
  - 3 composants n'injectent même pas le service (`immigration-checklist-section`, `prudhome-fiche-section`, `tribunal-travail-fiche-section`)
  - Les ~80 autres injectent en `@Optional()` mais reçoivent `null` quand ouverts en modal → no-op silencieux
- Constat utilisateur 2026-05-03 (staging) : tableau de bord ne se met pas à jour après calcul/save dans Droit au travail, Titre de séjour recommandé, Changement de statut → refresh manuel obligatoire.

## Comportement nominal

### Pipeline cassé (avant SF-177-14)

```
Tool component (in MatDialog)
  → @Optional() refreshService === null
  → null?.triggerRefresh() === undefined  (silencieux)
Dashboard (in case-file-detail tree)
  → refreshService.refresh$ (jamais émis)
  → reload jamais déclenché
```

### Pipeline réparé (après SF-177-14)

```
Tool component (in MatDialog ouvert avec viewContainerRef = panel.vcr / dashboard.vcr)
  → injector tree hérite de case-file-detail
  → @Optional() refreshService = instance fournie par CaseFileDetailComponent
  → refreshService.triggerRefresh() émet sur Subject
Dashboard + panel (déjà subscribers depuis SF-IA-02 + F-177)
  → refresh$ reçoit l'événement
  → reload visibility + tiles
  → cards mises à jour avec nouveaux verdicts
```

### Cas nominal (3 reproductions)

1. **Droit au travail** (`immigration-work-right-section`) — avocat ouvre l'outil en modal, sélectionne un titre, clique "Enregistrer" → POST `/api/v1/case-files/{id}/work-right` → success → `triggerRefresh()` déclenche reload → card dans dashboard et panel reflète le verdict (ex: "Droit de travail : OUI – Salarié").
2. **Titre de séjour recommandé** (`immigration-title-decision-section`) — même flow.
3. **Changement de statut** (`changement-statut-section`) — même flow.

Idem pour les ~80 autres outils ouverts en modal : `triggerRefresh()` propage désormais.

### Cas des 3 FAIL audit

Pour `immigration-checklist-section`, `prudhome-fiche-section`, `tribunal-travail-fiche-section` :
- Ajouter `@Optional() private dashboardRefresh: CaseDashboardRefreshService | null` en constructor
- Ajouter `this.dashboardRefresh?.triggerRefresh()` dans le handler `next:` du `subscribe(...)` après save/upsert réussi
- Pattern miroir des 80 autres composants OK

## Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Tool ouvert hors `case-file-detail` (cas théorique : page standalone, simulator F-163 si livré) | `@Optional()` retourne `null`, `?.triggerRefresh()` no-op silencieux. Pas de crash. |
| MatDialog réouvert plusieurs fois sur le même outil | Chaque ouverture passe son `viewContainerRef` du caller — comportement uniforme. |
| Save échoue (erreur réseau / 4xx) | Handler `error:` du subscribe ne doit pas appeler `triggerRefresh()` — non-régression à vérifier. |
| `viewContainerRef` non passé par un caller (régression) | Comportement actuel cassé (root injector) — on accepte ce fallback dégradé silencieux. Idéalement TS strict empêche la non-prise en compte (param obligatoire), mais le risque est faible (2 callers connus). |

## Critères d'acceptation

- [ ] `DecisionToolModalService.open(args)` accepte un paramètre `viewContainerRef?: ViewContainerRef` dans `DecisionToolModalArgs` et le forward à `dialog.open({..., viewContainerRef})`.
- [ ] `decisional-tools-panel.component.ts:1616` (caller modal) injecte `ViewContainerRef` via `inject(ViewContainerRef)` et le passe à `modalService.open({..., viewContainerRef: this.vcr})`.
- [ ] `case-dashboard.component.ts:186` (autre caller modal) idem.
- [ ] `immigration-checklist-section.component.ts` : injection `@Optional() CaseDashboardRefreshService` ajoutée + `triggerRefresh()` ajouté dans handler `next:` du `upsert(...).subscribe`.
- [ ] `prudhome-fiche-section.component.ts` : idem (handler `next:` du `save(...).subscribe`).
- [ ] `tribunal-travail-fiche-section.component.ts` : idem (handler `next:` du `save(...).subscribe`).
- [ ] Suite Jest verte : tests existants (incluant SF-177-02 sur le modal service) + ≥ 4 nouveaux tests SF-177-14.
- [ ] **Validation visuelle staging** : reproduire les 3 cas (Droit au travail, Titre de séjour recommandé, Changement de statut) sur dossier réel — les cards dashboard et panel se mettent à jour sans refresh page.

## Plan de test minimal

### Tests Jest SF-177-14 (≥ 4 nouveaux)

| ID | Cas | Vérification |
|----|-----|--------------|
| T-01 | `DecisionToolModalService.open` propage `viewContainerRef` | Mock `MatDialog.open`, appel `service.open({..., viewContainerRef: mockVcr})`, vérifier que le 2e argument de `dialog.open` contient `viewContainerRef: mockVcr`. |
| T-02 | `decisional-tools-panel` passe `vcr` au modal | Spy `modalService.open`, déclencher `onOpenTool(...)`, vérifier que l'argument inclut `viewContainerRef === panelComponent.vcr`. |
| T-03 | `case-dashboard` passe `vcr` au modal | Idem pour le dashboard. |
| T-04 | `immigration-checklist-section` appelle `triggerRefresh` après upsert success | Mock `checklistService.upsert` qui retourne `of({...})`, mock `dashboardRefresh`, déclencher save handler, vérifier `triggerRefresh` appelé exactement 1 fois. |
| T-05 | `prudhome-fiche-section` idem | Mock `ficheService.save`, vérifier `triggerRefresh`. |
| T-06 | `tribunal-travail-fiche-section` idem | Idem. |
| T-07 (régression) | Save error ne déclenche PAS `triggerRefresh` | Mock save retourne `throwError(...)`, vérifier `triggerRefresh` non appelé. |

### Non-régression (existants)

- Tests existants `decision-tool-modal.service.spec.ts` doivent rester verts.
- Tests existants des composants tools (3 cas FAIL + composants déjà OK touchés indirectement par le DI) doivent rester verts.

### Validation visuelle staging (obligatoire post-merge)

- [ ] Reproduire les 3 cas user (Droit au travail, Titre de séjour recommandé, Changement de statut) sur staging, dossier réel — cards mises à jour sans refresh.
- [ ] Tester un 4ᵉ outil quelconque (ex: Ancienneté, Indemnité licenciement) pour valider l'effet propagation à l'ensemble.

## Tables / endpoints / composants impactés

- **Aucune table impactée** (frontend pur).
- **Aucun endpoint impacté**.
- **Composants modifiés** :
  - `frontend/src/app/case-files/decisional-tools-panel/decision-tool-modal/decision-tool-modal.service.ts` (param `viewContainerRef`)
  - `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` (passe `vcr`)
  - `frontend/src/app/case-files/case-dashboard/case-dashboard.component.ts` (passe `vcr`)
  - `frontend/src/app/case-files/immigration-checklist-section/immigration-checklist-section.component.ts` (injection + triggerRefresh)
  - `frontend/src/app/case-files/prudhome-fiche-section/prudhome-fiche-section.component.ts` (idem)
  - `frontend/src/app/case-files/tribunal-travail-fiche-section/tribunal-travail-fiche-section.component.ts` (idem)
  - + spec.ts associés pour les tests T-01..T-07.

## Hors périmètre (volontaire)

- **Suppression de `@Optional()` sur les ~80 composants tools** — risqué (pourrait casser des contextes de test ou de standalone), gain marginal (le service sera désormais toujours résolu). Reste `@Optional()` partout pour robustesse.
- **Refonte du pattern F-IA-02 vers un mécanisme global** (`providedIn: 'root'`) — décision design préservée (per-case-file scope intentionnel).
- **Backend** — zéro impact.
- **Adaptation par domaine ou pays** — transversal, aucune adaptation.
- **Animation flash post-refresh sur les cards mises à jour** — déjà couverte par SF-159-02 (mergée). Le fix SF-177-14 va d'ailleurs réactiver ce flash post-tool-save (qui ne fonctionnait pas sans triggerRefresh wiré).

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : impact uniforme — tous les composants tools ouverts en modal bénéficient du fix viewContainerRef. Les 3 FAIL audit sont corrigés en plus.
- [x] **Autres pays** : aucun impact différencié.
- [x] **Autres domaines** : aucun impact différencié.
- [x] **Autres MatDialog dans l'app** : potentielle même problématique sur d'autres dialogs qui injectent des services scope `case-file-detail`. Scan initial : `PrefillDiffDialogComponent` (SF-159-02) — vérifier en dev s'il a le même besoin (lit le `DecisionalToolsProgressService`, scope panel). Si oui, élargir le fix au caller `decisional-tools-progress.service.ts:109`.
- [x] **Autres flows transversaux** : aucun impact (pas auth / workspace / plans / routing).

### Niveaux de vérification

- [x] **Modèle TypeScript** : extension de l'interface `DecisionToolModalArgs`.
- [x] **Record/DTO backend** : non concerné.
- [x] **Service / logique métier** : `CaseDashboardRefreshService` non modifié, juste mieux propagé.
- [x] **Entité JPA + schéma DB** : non concerné.
- [x] **Tests existants** : `decision-tool-modal.service.spec.ts` et specs des 3 composants FAIL adaptés.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `DecisionToolModalService` (service partagé) | Oui | Modifié — propagation propre via signature étendue. |
| `decisional-tools-panel` (caller) | Oui | Pass `vcr`. |
| `case-dashboard` (caller) | Oui | Pass `vcr`. |
| `PrefillDiffDialogComponent` (autre dialog SF-159-02) | À vérifier en dev | Si dépend du même injector tree → fix similaire ; sinon non applicable. |
| 3 composants FAIL audit | Oui | Injection + appel `triggerRefresh` ajoutés. |
| ~80 composants OK audit | Non — pas de modification, le fix profite à tous via DI | Vérification ponctuelle staging sur 1-2 outils témoins. |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (modal service + 2 callers + 3 FAIL).
- [ ] SF parallèle — non applicable.
- [ ] Backlog — non applicable.
- [x] Cas `PrefillDiffDialogComponent` à vérifier en dev (scan rapide ; si problème, soit inclus, soit ouverture SF dédiée selon scope).

## Impact par domaine métier

Cette SF est **transversale** :
- pas de différence Travail / Immigration / Famille,
- pas de différence FR / BE,
- aucune adaptation par domaine.

## Nouveau pattern UI ou service partagé

- **Pas de nouveau service** — `CaseDashboardRefreshService` inchangé.
- **Pas de nouveau composant partagé** — `DecisionToolModalService.open()` étendu mais c'est un changement non-breaking (param optionnel).
- **Pattern préservé** : F-IA-02-03 reste valide, juste réellement effectif désormais.

## Préoccupations transversales

| Préoccupation | Concerné ? |
|---------------|-----------|
| Auth / Principal | Non |
| Workspace context | Non |
| Plans / limites | Non |
| Navigation / routing | Non |
| Outil décisionnel métier | Non (pas de logique métier modifiée — pure réparation du bus de notifications post-save) |

## Smoke tests E2E concernés

- [ ] Aucun smoke test dédié — la SF répare un comportement asynchrone post-save dans le panel décisionnel, hors couverture des smoke tests `auth/workspace/navigation`.

## Notes d'implémentation

- **Self-check pré-commit** :
  - Vérifier que `viewContainerRef` est bien forward dans `decision-tool-modal.service.ts` (grep `viewContainerRef`).
  - Vérifier que les 2 callers passent `vcr`.
  - Vérifier que les 3 composants FAIL ont bien injection + appel.
- **Validation visuelle staging obligatoire** avant marquer Done — la SF répare un comportement runtime, les tests Jest valident la logique mais pas l'effet visuel post-deploy.
- **Effet bonus** : SF-159-02 (flash + toast post-analyse) ne se déclenchait probablement jamais après une action manuelle dans un outil (puisque `loadVisibility()` n'était pas relancé). Avec SF-177-14, le flash devrait apparaître après chaque save dans un outil ouvert en modal — comportement attendu et cohérent avec l'esprit de F-IA-02.

## Estimation

1,5-2 h dev + tests + review.

## Référence backlog

- `docs/PRODUCT_SPEC.md` — F-177 (à rouvrir : 11/11 SF Terminée → 11/12 En cours, ré-Terminée 12/12 après merge).
- Origine : audit 2026-05-03 cette SF + constat utilisateur staging 2026-05-03 sur 3 outils.
