# Mini-spec — F-197 / SF-197-02 Frontend — UI override type litige + extension TOOL_REGISTRY pre-fill

## Identifiant

`F-197 / SF-197-02`

## Statut

`draft` — 2026-05-06

## Branche Git

`feat/SF-197-02-frontend-type-override`

## Pattern de référence

**SF-192-02 + SF-194-02**.

## Contrat API importé de SF-197-01-backend

- `PUT /api/v1/case-files/{id}/type-litige-override` body `{ type, raison? }`
- `GET /api/v1/case-files/{id}/type-litige-override` → `{ typeLitigeAvocat?, typeProcedureAvocat?, raison? }`

---

## Objectif

(1) UI override dans le `SynthesisComponent` (badge type litige détecté + bouton « Modifier » → MatSelect + raison libre) ; (2) propagation override via TOOL_REGISTRY pour pre-fill F-DT-08/09/10 / F-IM-08/20 ; (3) badge « Modifié par l'avocat » sur grille de badges F-162.

---

## Comportement attendu

### Cas nominal

1. **UI override dans `SynthesisComponent`** :
   - Le badge `type_litige_detecte` actuel (dans la grille F-162 ou ailleurs) reçoit un bouton « Modifier » Material Icon `edit` à côté
   - Clic → MatDialog ou MatSelect inline avec :
     - Liste enum selon le domaine du dossier (Travail FR : 7 valeurs ; Immigration FR : ~6 valeurs OQTF/etc.)
     - Champ texte raison libre (optionnel, max 500 chars)
     - Bouton "Valider" → PUT
   - Si override déjà persisté : MatSelect pré-sélectionné sur la valeur override + indicateur visuel « Modifié par avocat »
2. **Lecture override** : `TypeLitigeOverrideService.getForCaseFile(id)` au montage du dossier, signal cache
3. **Propagation TOOL_REGISTRY** : extension `aiData.typeLitige` dans `inputs(ctx)` pour outils Travail (F-DT-08/09/10/12/13) et `aiData.typeProcedure` pour outils Immigration (F-IM-08/20).
   - Si override présent : le pré-fill outil utilise la valeur override (priorité absolue sur `aiData.typeLitigeIA`).
   - Si pas d'override : comportement actuel inchangé (utilise valeur IA).
4. **Indicateur visuel grille de badges F-162** : si override présent, le badge type litige affiche pictogramme `edit` Material à côté + tooltip "Modifié par votre avocat : <raison>"
5. **Refresh** : signal `override` ré-fetché uniquement au SSE `ENRICHED_ANALYSIS DONE` (cohérence stricte)

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| PUT 400 | Snackbar erreur, MatDialog reste ouvert |
| PUT 5xx ou timeout | Snackbar erreur, optimistic update annulé |
| GET timeout | Fail-open silencieux, override = null |

---

## Critères d'acceptation

- [ ] **CA-01** : bouton « Modifier » à côté du badge type litige détecté
- [ ] **CA-02** : clic ouvre MatDialog/MatSelect avec valeurs enum du domaine
- [ ] **CA-03 validation enum** : MatSelect ne propose que les valeurs valides du domaine du workspace
- [ ] **CA-04 raison libre** : champ texte optionnel ≤ 500 chars
- [ ] **CA-05 override persisté visible** : pré-sélection MatSelect + indicateur "Modifié par avocat"
- [ ] **CA-06 PUT sans refresh** : aucun `triggerRefresh()` après PUT (cohérence F-176)
- [ ] **CA-07 propagation TOOL_REGISTRY** : F-DT-09 reçoit `aiData.typeLitige = "LICENCIEMENT_ECONOMIQUE"` (override) au lieu de la valeur IA
- [ ] **CA-08 indicateur grille F-162** : pictogramme `edit` + tooltip raison
- [ ] **CA-09 fail-open** : timeout GET → override = null, comportement actuel
- [ ] **CA-10 OnPush + markForCheck**
- [ ] **CA-11 visuel charte** : palette navy/or DESIGN_SYSTEM.md

---

## Hors scope V1

- (a) Historique des overrides
- (b) Override Famille (`regime_matrimonial`) — V2
- (c) UI inline dans la grille de badges (V1 = MatDialog)
- (d) Validation backend transversale (V1 = simple enum check)

---

## Composants impactés

- `TypeLitigeOverrideService` (nouveau) + modèle
- `<app-synthesis>` extension — bouton Modifier + MatDialog
- `<app-decisional-tools-panel>` `TOOL_REGISTRY` — propagation override
- Composants outils Travail (F-DT-08/09/10/12/13) — `aiData.typeLitige` reçoit override
- Composants outils Immigration (F-IM-08/20) — `aiData.typeProcedure` reçoit override
- Grille de badges F-162 (`<app-synthesis>` template) — indicateur override

---

## Tests Jest (~10)

- `TypeLitigeOverrideServiceTest` (3)
- `SynthesisComponentTest` extension (4 : bouton Modifier rendu, clic ouvre dialog, PUT sans refresh, indicateur override)
- 1-2 outils représentatifs : pre-fill avec override (2)
- Grille F-162 indicateur (1)

---

## Dépendances

- F-IA-04 ✅
- F-DT-08/09/10 ✅, F-IM-08/20 ✅
- F-162 ✅ (grille de badges)
- **SF-197-01 backend**

---

## Notes 2026-05-06

- **Saillance produit forte** : l'override est l'action la plus impactante de F-19X — change la visibilité des outils + le pre-fill. Indicateur visuel discret mais clair (pictogramme `edit` + tooltip raison) — éviter l'effet "alerte" mais signaler clairement
- MatDialog plutôt qu'inline pour clarifier l'action (pas un toggle léger comme un statut)
- V1 single value, pas multi-override (un avocat ne devrait pas avoir besoin de surcharger plusieurs types simultanément sur un dossier)
