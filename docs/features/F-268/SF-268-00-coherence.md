# F-268 — Cadrage cohérence (étape 0) + cohérence écran (étape 0 bis)

> Feature : **Réorganisation du catalogue d'outils décisionnels** (désencombrement). Signal terrain PO 2026-06-11 (« pas possible d'afficher tous les outils comme ça, trop chargé »). 2026-06-11.

## Verdict : **GO avec ajustements**

## Intention
Désencombrer le panneau d'outils décisionnels (`decisional-tools-panel`, onglet Décision) : aujourd'hui les 5 sections thématiques sont **toutes empilées** (60+ outils visibles d'un coup) → surcharge. Les présenter **un thème à la fois** (onglets).

## Étape 0 — cohérence fonctionnelle
- **Amont** : les outils visibles (alwaysOn ∪ contextual) sont **déjà groupés en 5 thèmes** (F-169 : `INDEMNITES` Indemnités & calculs, `VALIDITE` Validité & contestation, `DELAIS` Délais & procédure, `DOCUMENTS`, `DIAGNOSTIC`) via `THEME_BY_TOOL_ID` + `themedTools()`. Le Catalogue (activation manuelle) est une section à part. **Aucun trou amont** — c'est une réorganisation de présentation.
- **Aval** : aucun impact sur le calcul/visibilité des outils (F-IA-04), ni sur la consommation par les conclusions (F-258/verdicts). Pur affichage.
- **Verdict** : GO.

## Étape 0 bis — cohérence écran
- **Placement** : reste dans `decisional-tools-panel` (onglet Décision, colonne saisie). Pas de nouvelle route (option page dédiée écartée : perte d'état sticky, plus lourde, UX moins fluide).
- **Layout cible (décision PO par défaut, autonome)** : envelopper les 5 thèmes dans un **`mat-tab-group`** (un onglet par thème, libellés = `THEMES_ORDERED.label`) → **un seul thème affiché à la fois**. N'afficher que les onglets de thèmes **non vides**. Le **Catalogue** (chips d'activation manuelle) reste visible (en bas ou onglet dédié). Conserver le compteur/contexte.
- **Charge écran** : passe de « 5 sections empilées » à « 1 thème visible » → **désencombrement net**.
- **Continuité** : les ancres entrantes (stepper, `?section=decision`, F-258 « Voir les outils à calculer ») doivent **activer le bon onglet** si elles ciblent un outil d'un thème précis (ajustement : sélectionner l'onglet du thème ciblé au scroll).
- **Verdict** : GO avec ajustements (gérer l'activation d'onglet sur navigation entrante ; design des onglets selon le design system).

## Invariants
1. **Pas de changement métier** : visibilité/calcul/thématisation des outils inchangés (réutilise `themedTools()`).
2. **Onglets = thèmes non vides** ; Catalogue conservé ; compteurs conservés.
3. **Navigation entrante** : si une ancre cible un outil, activer l'onglet de son thème.
4. **Design system** : onglets Material stylés marine/or, espacements 4px.
5. Faible risque : mutation d'un signal `selectedThemeIndex` + template `mat-tab-group`, pas de logique métier touchée.

## Fichiers
- **Modifier** : `decisional-tools-panel.component.html` (envelopper `@for (theme...)` dans `<mat-tab-group>`), `.ts` (signal `selectedThemeIndex` + helper d'activation d'onglet sur ancre), `.scss` (style onglets).

## Décision finale
**GO avec ajustements.** Onglets par thème (`mat-tab-group`) dans le panneau d'outils, un thème à la fois, Catalogue conservé, activation d'onglet sur navigation entrante. Décidé par défaut (autonome), à juger visuellement après livraison.
