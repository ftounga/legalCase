# Mini-spec — F-249 / SF-249-02 — Allègement du hero d'accueil (retour PO)

## Identifiant

`F-249 / SF-249-02`

## Feature parente

`F-249` — Refonte du tableau de bord d'accueil. SF-249-01 a livré le hero « futuriste » (gros dégradé navy + halo doré + glassmorphism). **Retour PO 2026-06-03 : bloc trop massif / grossier.**

## Statut

`ready`

## Date de création

2026-06-03

## Branche Git

`feat/SF-249-02-hero-sobre`

## Type

Ajustement visuel piloté PO (style → `DESIGN_SYSTEM.md`). Exempté étapes 0/0 bis (aucun nouvel élément d'écran ni workflow ; on allège un bloc existant). Amende la dérogation `DESIGN_SYSTEM.md §10`.

---

## Objectif

Remplacer le hero d'accueil massif par un **bandeau navy fin et sobre** (salutation + date), sortir les KPI du bleu en **cartes blanches**, déplacer la sparkline hors du bandeau, retirer le halo doré.

---

## Comportement attendu

### Cas nominal

`/dashboard` affiche :
1. **Bandeau navy fin** = salutation (`greeting()` → « Bonjour Maître X ») + date uniquement. Padding réduit ~`16px 24px` (vs 28×32), titre `~1.2rem` (vs 1.6rem), **halo doré retiré**, ombre allégée (`$shadow-card` au lieu de `$shadow-hero`).
2. **Headline d'orientation** (alerte délais/statut) : sorti du bandeau, posé sous lui sur fond clair, restyle (garde la sémantique couleur critique/warn/ok mais lisible sur fond clair).
3. **4 KPI en cartes blanches** sous le bandeau : fond `$surface`, `$shadow-card`, `kpi-value` en `$navy`, `kpi-label` en `$muted`. Les états alerte gardent un accent (rouge / orange) discret. Comportement clic (scrollTo) et count-up inchangés.
4. **Sparkline « Votre semaine »** conservée mais déplacée hors du bandeau, en carte blanche discrète (cliquable → `scrollTo('activity')`).

### Cas d'erreur / dégradé

| Situation | Comportement |
|-----------|-------------|
| `prefers-reduced-motion` | animations d'entrée/count-up désactivées (comportement existant conservé) |
| `userFirstName` absent | greeting = « Bonjour » (fallback existant inchangé) |
| Pas d'activité semaine | sparkline affiche « Aucune analyse » (inchangé) |

---

## Critères d'acceptation vérifiables

1. ✅ Le bandeau navy ne contient plus que salutation + date (pas de KPI, pas de sparkline, pas de headline dedans).
2. ✅ `.hero-glow` (halo doré) supprimé du HTML et du SCSS.
3. ✅ Padding du bandeau ≤ `16px 24px` ; `hero-greeting` ≤ `1.25rem`.
4. ✅ Les 4 KPI sont des cartes blanches (fond clair, texte navy), plus de glassmorphism (`backdrop-filter` retiré).
5. ✅ La sparkline est rendue hors du bandeau et reste cliquable vers l'activité.
6. ✅ Espacements multiples de 4px ; couleurs dans la palette (`$navy`, `$gold`, `$muted`…) — conforme DESIGN_SYSTEM.
7. ✅ Aucun changement backend (DTO `DashboardSummary` inchangé) ni des sections sous le hero.

---

## Plan de test minimal

- **Jest `dashboard.component.spec.ts`** : adapter les specs SF-249-01 qui cassent — retirer toute assertion sur `.hero-glow` / glassmorphism ; vérifier (a) greeting rendu, (b) 4 `.kpi` rendus, (c) sparkline rendue hors `.hero`, (d) clics KPI/sparkline appellent `scrollTo`. Conserver les tests de count-up et de logique inchangés.
- **Isolation workspace / backend** : N/A (aucun appel modifié).

---

## Tables / endpoints / composants impactés

- `frontend/src/app/dashboard/dashboard.component.html` — restructuration hero (l.16-84).
- `frontend/src/app/dashboard/dashboard.component.scss` — `.hero`, `.hero-glow` (suppr.), `.hero-greeting`, `.hero-kpis`/`.kpi` (→ cartes blanches), `.hero-spark` (repositionnée), `.hero-headline` (restyle fond clair).
- `frontend/src/app/dashboard/dashboard.component.ts` — uniquement si la structure de `greeting()`/`sparkline()` doit bouger (a priori non, logique conservée).
- `docs/DESIGN_SYSTEM.md` §10 — **amendement** : la couche d'accueil dérogatoire est ramenée à un bandeau sobre (halo/glassmorphism retirés).
- **Aucun** backend, table, endpoint, migration.

---

## Hors périmètre

- Sections sous le hero (urgences, dossiers, checklist, activité) — non touchées.
- Backend `DashboardSummary` / `weeklyActivity` — inchangé.
- Toute autre refonte visuelle du dashboard.
