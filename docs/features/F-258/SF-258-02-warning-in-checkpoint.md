# Mini-spec — F-258 / SF-258-02 — Avertissement « outils non calculés » fort, dans le checkpoint de génération

> Cadrage : `SF-258-02-00-coherence.md` (étape 0 + 0 bis GO). **Frontend-only.**

## Identifiant
`F-258 / SF-258-02`

## Statut
`draft`

## Branche
`feat/SF-258-02-warning-in-checkpoint`

## Objectif
> Au clic « Générer », porter l'alerte des outils pré-remplis non calculés **dans la modale de composition** (avertissement fort + accès pour les compléter), et **ouvrir la modale même quand rien n'est calculé** s'il reste des non-calculés — pour que le checkpoint ne soit plus contournable.

## Comportement attendu
### Cas nominal
1. Clic « Générer le projet de conclusions » (ou « Régénérer »).
2. `composeThenGenerate` ouvre la modale si **`missingToolsCount > 0`** OU s'il y a des éléments curables (calculés / moyens). *(Avant : seulement si curables.)*
3. La modale affiche, **en tête et seulement si `missingToolsCount > 0`**, un bloc d'avertissement : « ⚠ N outils pertinents ne sont pas calculés. Ils ne nourriront pas l'acte tant qu'ils ne sont pas complétés et calculés. » + bouton **[ Aller compléter ces outils ]**.
4. Sous le bloc : la composition habituelle (outils calculés + moyens, cases à cocher) — inchangée.
5. Actions :
   - **[ Aller compléter ces outils ]** → ferme la modale **sans générer**, émet `viewToolsRequested` (le parent défile vers `#section-outils-decisionnels`).
   - **[ Confirmer & générer ]** → persiste la composition (F-288) puis génère. **Disponible même avec des non-calculés** (non bloquant).
   - **[ Annuler ]** → ferme, ne génère pas.

### Cas d'erreur / limites
| Situation | Comportement |
|---|---|
| `missingToolsCount = 0` | Pas de bloc d'avertissement → modale = celle de F-288 aujourd'hui |
| 0 non-calculé ET 0 curable | **Pas de modale** (génération directe, inchangé) |
| Échec de calcul de `missingToolsCount` | Dégradation silencieuse `= 0` (déjà géré F-258) → pas de bloc |
| Tout décoché + non-calculés présents | Génération autorisée (non bloquant) |

## Critères d'acceptation
- [ ] **C1** — `missingToolsCount > 0` + 0 outil calculé → la modale **s'ouvre** au clic Générer (avant : génération directe).
- [ ] **C2** — Le bloc d'avertissement n'apparaît **que si** `missingToolsCount > 0` ; il affiche le **nombre** N.
- [ ] **C3** — [ Aller compléter ces outils ] ferme la modale **sans générer** et déclenche la navigation vers les outils (`viewToolsRequested`).
- [ ] **C4** — [ Confirmer & générer ] reste actif malgré des non-calculés (non bloquant) → persiste + génère.
- [ ] **C5** — `missingToolsCount = 0` → aucun bloc ; non-régression du flux F-288 (curation outils/moyens).
- [ ] **C6** — Aucun appel backend nouveau ; réutilise `missingToolsCount` et `viewToolsRequested`.

## Périmètre
### Hors scope
- ❌ Auto-calcul des outils pré-remplis (« tout calculer ») — non viable (pré-fill incomplet).
- ❌ Blocage dur de la génération.
- ❌ Toute logique backend / nouvelle donnée (les noms des outils ne sont pas requis : le bloc affiche le **nombre**, comme F-258).
- Le bandeau F-258 sur la page reste (décision 0 bis).

## Technique
- **`conclusions-section.component.ts`** :
  - `composeThenGenerate` : condition d'ouverture de la modale = `hasCurable || this.missingToolsCount() > 0`. Passer `missingToolsCount()` dans `data` du dialog.
  - Traiter le nouveau résultat du dialog : `navigateToTools` → `this.viewToolsRequested.emit()` (pas de génération) ; `exclusions` → `persistThenGenerate` (inchangé) ; `undefined` → annulation.
  - Si la modale est ouverte **uniquement** à cause des non-calculés (0 curable) : la composition est vide → seules les sections vides ne s'affichent pas, le bloc d'avertissement + Confirmer/Annuler suffisent.
- **`conclusion-composition-dialog`** :
  - `data` gagne `missingToolsCount: number`.
  - Template : bloc d'avertissement en tête `@if (missingToolsCount > 0)` (design system : encart info navy/or, **pas rouge** — avertissement, pas erreur critique ; `mat-icon` warning/info) + bouton `[ Aller compléter ces outils ]`.
  - Résultat : type étendu `ConclusionCompositionDialogResult` → `{ navigateToTools: true }` OU `{ exclusions: CompositionExclusion[] }`.
- **Aucune migration, aucun endpoint.**

## Plan de test (Jest)
- [ ] missing>0 & 0 curable → modale ouverte (C1).
- [ ] bloc affiché ssi missing>0, avec N (C2).
- [ ] clic « Aller compléter » → `viewToolsRequested` émis, pas de PUT ni POST generate (C3).
- [ ] « Confirmer & générer » actif avec missing>0 → PUT puis generate (C4).
- [ ] missing=0 → pas de bloc, flux F-288 inchangé (C5, non-régression).

## Analyse d'impact
- [x] **Aucune préoccupation transversale** (frontend-only ; pas d'auth/route/workspace/plan ; pas de backend). Réutilise des outputs existants.
