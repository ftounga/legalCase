# Mini-spec — F-238 / SF-238-02 Activation manuelle frontend

## Identifiant

`F-238 / SF-238-02`

## Feature parente

`F-238` — Catalogue d'outils décisionnels cliquable + labels humains

## Statut

`ready`

## Date de création

2026-05-11

## Branche Git

`feat/F-238-catalogue-cliquable-labels`

---

## Objectif

Rendre les chips du catalogue cliquables → POST sur l'endpoint backend d'activation manuelle (contrat figé SF-238-03) → l'outil migre du catalog vers `contextual` au prochain refresh visibility.

---

## Comportement attendu

### Cas nominal

1. Le SCSS `.catalog-chip` passe de `cursor: not-allowed` à `cursor: pointer`.
2. Le tooltip de chaque chip devient `"Activer cet outil manuellement"`.
3. Le texte UI ligne 53 du HTML devient : `"Outils disponibles selon la situation détectée. Cliquer pour activer manuellement."`
4. `(click)="activateManually(toolId)"` est branché sur chaque `.catalog-chip`.
5. `activateManually(toolId)` :
   - Bascule un signal local d'état `activatingToolIds: Set<string>` → chip affiche un spinner inline + désactive le click.
   - Appelle `DecisionToolManualActivationService.activate(caseFileId, toolId)`.
   - Sur **succès** : `refreshService.triggerRefresh()` (debounce 300 ms re-fetch la visibilité → l'outil disparaît du catalog et apparaît dans la grille thématique).
   - Sur **erreur** : `MatSnackBar` avec message « Activation impossible. Réessayez plus tard. » + retire le toolId de `activatingToolIds`.
6. Service `DecisionToolManualActivationService` dans `frontend/src/app/core/services/` :
   - `activate(caseFileId, toolId): Observable<ManualActivationResponse>` → POST.
   - `deactivate(caseFileId, toolId): Observable<void>` → DELETE (exposé pour usages futurs ; pas câblé UI dans cette SF).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Backend 404 (case_file inconnu) | Snackbar erreur générique | 404 |
| Backend 400 (toolId hors registry) | Snackbar erreur générique | 400 |
| Backend 409 (déjà activé) | Snackbar info « Outil déjà activé » + refresh quand même | 409 |
| Backend 500 / réseau | Snackbar « Réessayez plus tard » + retire de activatingToolIds | 5xx |

---

## Contrat API (importé de SF-238-03)

- **POST** `/api/v1/case-files/{id}/decision-tools-visibility/manual-activations`
  - Body : `{"toolId": "<tool_id>"}` (string requis, non vide, longueur ≤ 128).
  - Réponse 200 : `{"id": "<uuid>", "toolId": "<tool_id>", "activatedAt": "<ISO-8601>"}`
  - 400 si toolId hors TOOL_REGISTRY (vérif backend), 404 case_file inconnu, 409 si déjà activé (deactivated_at IS NULL).
- **DELETE** `/api/v1/case-files/{id}/decision-tools-visibility/manual-activations/{toolId}` → 204.

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] Autres composants chip cliquables : aucun équivalent — c'est le 1er chip décisionnel cliquable.
- [x] Autres patterns POST + refresh : `CaseDashboardRefreshService.triggerRefresh()` réutilisé (pattern SF-IA-02-03).
- [x] Service nouveau : isolé `DecisionToolManualActivationService` — pas de conflit avec un service existant.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Chip catalogue panel | Oui | Modifié dans cette SF |
| Cards thémées (contextual/alwaysOn) | Non | Déjà cliquables via `(open)` → `openTool()` |
| Pattern POST + refresh | Oui | `triggerRefresh()` (SF-IA-02-03) |

### Décision

- [x] Étendu à toutes les cibles applicables.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — SF d'interaction UI sur le panel parent F-IA-04, pas un nouveau composant décisionnel (`<app-XXX-section>`). Aucun pré-fill IA, aucune validation F-IA-03 ne s'applique (rien n'est saisi par l'avocat dans le chip).

---

## Impact par domaine métier

Cette SF est **transversale (infrastructure UI)** : elle ajoute l'interaction « clic pour activer » sur tous les outils du catalogue, quels que soient le domaine (Travail / Immigration / Famille) et le pays (FR / BE). Aucune adaptation par domaine — comportement identique partout.

---

## Parité des domaines métier (niveau ≥ 5)

- [x] Niveau du tool décisionnel livré : **non applicable** — SF d'interaction UI infrastructure.

---

## Critères d'acceptation

- [ ] SCSS `.catalog-chip` : `cursor: pointer` (au lieu de `not-allowed`).
- [ ] HTML : tooltip = `"Activer cet outil manuellement"`, texte ligne 53 = `"Outils disponibles selon la situation détectée. Cliquer pour activer manuellement."`.
- [ ] `(click)="activateManually(toolId)"` branché sur `.catalog-chip`.
- [ ] Service `DecisionToolManualActivationService` créé dans `frontend/src/app/core/services/`.
- [ ] Sur succès POST → `refreshService?.triggerRefresh()`.
- [ ] Sur erreur → `MatSnackBar` (pas d'`alert()` / `confirm()`).
- [ ] Tests Jest : 1 test « click déclenche activate() », 1 test « displayLabel rendu », 1 test « erreur → snackbar ».
- [ ] `npm run build` frontend OK.

---

## Périmètre

### Hors scope

- Bouton « désactiver l'outil » (DELETE) UI → service exposé mais pas câblé.
- Persistence côté backend → SF-238-03.

---

## Plan de test

### Jest

1. Click sur chip → appel `service.activate(caseFileId, toolId)` avec bon payload.
2. Succès POST → `refreshService.triggerRefresh()` invoqué.
3. Erreur HTTP → `snackBar.open()` invoqué, pas de refresh.

### Isolation workspace

- N/A côté frontend (le backend vérifie déjà workspace via `CurrentUserResolver`).

---

## Tables / endpoints / composants impactés

- **Composants** : `decisional-tools-panel.component.{html,scss,ts,spec.ts}`.
- **Service** : `frontend/src/app/core/services/decision-tool-manual-activation.service.ts` (nouveau).
- **Endpoints consommés** : `POST /api/v1/case-files/{id}/decision-tools-visibility/manual-activations` (livré par SF-238-03 dans la même PR groupée).
- **Tables** : aucune (frontend).
