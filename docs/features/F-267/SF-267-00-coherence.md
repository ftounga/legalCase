# F-267 — Cadrage cohérence (étape 0) + cohérence écran (étape 0 bis)

> Feature : **Page dédiée « Conclusions »**. Signal terrain PO 2026-06-11 (« visuellement extrêmement moche, tout est serré »). 2026-06-11.

## Verdict : **GO avec ajustements**

## Intention
Sortir le module conclusions de l'onglet **Décision** (où il est empilé/serré, colonne droite sticky avec le tableau de bord) vers une **page dédiée, pleine largeur, très soignée** — l'acte présenté comme une vraie feuille, les actions (génération/édition/co-rédaction/export/versions) dans un panneau propre.

## Étape 0 — cohérence fonctionnelle
- **Amont** : `conclusions-section` est **autonome** (prend `caseFileId`, 14 signals internes, toute la logique F-98/261/264/265/266). Le pattern de **route dédiée** existe déjà (`/case-files/:id/synthesis`). Aucun trou amont.
- **Aval** : génération/édition/export/versions inchangés (le composant est déplacé, pas réécrit). Le CTA « Voir les outils à calculer » (F-258) navigue vers l'onglet Décision (`?section=decision`) — conservé.
- **Verdict** : GO.

## Étape 0 bis — cohérence écran
- **Parcours** : onglet Décision = outils décisionnels (saisie) + tableau de bord. Les **conclusions** déménagent sur **`/case-files/:id/conclusions`** (page pleine largeur).
- **Placement** : route dédiée symétrique de `/synthesis`. Accès depuis : (a) l'onglet Décision via un **CTA/carte « Voir le projet de conclusions »** (remplace le composant empilé), (b) le stepper du tableau de bord (étape « Conclusions »).
- **Layout cible (décision PO par défaut, autonome — design system marine/or, Merriweather/Inter)** : page pleine largeur ; **l'acte centré comme une feuille** (max-width lecture confortable, marges généreuses) ; les **actions** (générer / éditer / co-rédiger / exporter / versions / cycle de vie) dans un **panneau latéral ou en barre supérieure propre**, pas empilées au-dessus de l'acte. En édition (F-264) : éditeur + aperçu côte à côte sur large, bascule sur étroit.
- **Charge écran** : l'onglet Décision **se désencombre** (les conclusions partent) ; la nouvelle page respire.
- **Continuité** : état terminal « projet de conclusions » désormais sur sa page dédiée ; retour facile vers le dossier/outils.
- **Verdict** : GO avec ajustements (soigner le layout « feuille » + le panneau d'actions ; responsive).

## Invariants
1. **Réutiliser `conclusions-section` tel quel** (autonome) — ne pas réécrire la logique ; le travail est la **page d'accueil + le layout**.
2. **Retirer** `<app-conclusions-section>` de l'onglet Décision + **CTA** vers la page (pas de double affichage).
3. **Beauté** : design system (marine #1A3A5C / or #C9973A, Merriweather titres, Inter corps), espacements généreux (multiples de 4), l'acte comme une feuille lisible.
4. **Responsive** : pleine largeur ; édition côte-à-côte/bascule (F-264 déjà responsive).
5. Liens entrants vers l'onglet Décision (F-258 `?section=decision`) conservés.

## Fichiers
- **Créer** : `case-files/case-file-conclusions-page/` (wrapper léger : route param → `caseFileId`, fetch `hasCompletedAnalysis`/stade, rend `conclusions-section` dans un layout page soigné).
- **Modifier** : `app.routes.ts` (route `/case-files/:id/conclusions`) ; `case-file-detail.component.html` (retirer `conclusions-section` de l'onglet Décision, ajouter CTA) + `.ts` (retirer import si plus utilisé) ; éventuellement le stepper (étape Conclusions → navigue).

## Décision finale
**GO avec ajustements.** Page dédiée `/case-files/:id/conclusions`, pleine largeur soignée, réutilisant le composant autonome ; désencombre l'onglet Décision. Layout « feuille + panneau d'actions » décidé par défaut (autonome), à juger visuellement par le PO après livraison.
