# SF-268-02 — Catalogue d'outils repliable & groupé (correction de périmètre)

> Correction de SF-268-01 après précision PO (2026-06-11). Frontend-only.

## Objectif (1 phrase)
Désencombrer le panneau d'outils décisionnels en **repliant le « Catalogue »** (outils à activation manuelle) **par défaut** et en **groupant ses chips par thème**, tout en **laissant les outils visibles/pré-remplis en sections empilées visibles** (annulation des onglets de SF-268-01).

## Contexte
SF-268-01 a mis en onglets les **outils visibles** (le mauvais bloc). Le PO veut au contraire que ces outils (dont les pré-remplis) restent **visibles d'emblée**, et que la surcharge soit traitée sur la section **« Catalogue »** (la longue liste d'outils complémentaires à activer manuellement).

## Comportement nominal
- **Outils visibles** (alwaysOn ∪ contextual) : rendus en **sections thématiques empilées** (F-169), toutes visibles — état pré-F-268 restauré (suppression de `mat-tab-group`, `selectedThemeIndex`, `visibleThemes`, `selectThemeForTool`).
- **Catalogue** (`visibility().catalog`) : en-tête **repliable** « Catalogue — N outils disponibles », **replié par défaut** (`catalogExpanded = false`). Au clic, dépliage → chips **groupées par thème** (`themedCatalog()`, ordre canonique `THEMES_ORDERED`, fallback `DIAGNOSTIC`).
- Rechargement de la visibilité (fin d'analyse) → Catalogue **re-replié**.

## Cas d'erreur / bords
- Catalogue vide → la section n'est pas rendue (inchangé).
- toolId du catalogue sans mapping thème → groupe `DIAGNOSTIC` (même fallback que `themedTools()`).
- Aucun appel réseau ; aucun impact visibilité/calcul.

## Critères d'acceptation
1. Aucun `mat-tab-group` dans le panneau ; les thèmes visibles non vides apparaissent en sections (titres `.theme-title`).
2. Catalogue replié au 1er rendu : `.catalog-toggle` présent, **0** `.catalog-chip` rendue, compteur exact.
3. Clic sur le toggle → `catalogExpanded()===true`, chips rendues, ≥1 `.catalog-group`.
4. `themedCatalog()` répartit tous les toolIds du catalogue sans perte/doublon, groupes dans l'ordre `THEMES_ORDERED`.
5. Rechargement visibilité → `catalogExpanded()===false`.

## Plan de test minimal
- Unitaires Jest (5, ci-dessus) — `decisional-tools-panel.component.spec.ts`.
- Isolation workspace : N/A (lecture visibilité existante, pas de nouvelle donnée).

## Composants impactés
- `decisional-tools-panel.component.{html,ts,scss,spec.ts}` uniquement.

## Hors périmètre
- Aucune modification métier (visibilité, calcul, thématisation des outils).
- Aucune nouvelle route / endpoint (la page conclusions = F-267, séparée).
