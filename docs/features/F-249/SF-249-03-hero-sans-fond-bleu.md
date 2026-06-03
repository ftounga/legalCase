# Mini-spec — F-249 / SF-249-03 — Suppression du fond bleu du hero d'accueil (retour PO)

## Identifiant

`F-249 / SF-249-03`

## Feature parente

`F-249` — Tableau de bord d'accueil. Suite de SF-249-02 (bande navy fine). **Retour PO 2026-06-04 : le bandeau bleu, même affiné, reste indésirable** → le retirer entièrement.

## Statut

`ready`

## Type

Ajustement visuel piloté PO (style → `DESIGN_SYSTEM.md`), validé sur aperçu rendu. Exempté étapes 0/0 bis.

## Objectif

Supprimer tout aplat bleu de l'en-tête d'accueil : « Bonjour Maître X » devient un titre navy sur fond clair, la date en gris. Le reste (KPI cartes blanches, sparkline, headline) est déjà sobre depuis SF-249-02.

## Comportement attendu

`/dashboard` : l'en-tête `.hero` n'a **plus de fond/dégradé bleu, plus d'ombre, plus de padding de carte**. `hero-greeting` en `$navy` (Merriweather 1.5rem), `hero-date` en `$muted`. Aucun changement de logique (greeting/date/KPI/sparkline inchangés), aucun backend.

## Critères d'acceptation

1. ✅ `.hero` n'a plus `background` bleu ni `box-shadow` ni `padding` de carte.
2. ✅ `hero-greeting` en couleur `$navy` (plus de `#fff`).
3. ✅ `hero-date` en `$muted`.
4. ✅ Tests dashboard verts ; logique inchangée.
5. ✅ Conforme palette/espacements DESIGN_SYSTEM ; §10 amendé.

## Plan de test

- Jest `dashboard.component.spec.ts` : doit rester vert (aucune assertion sur le fond bleu) — exécuté : **22/22**.

## Composants impactés

- `frontend/src/app/dashboard/dashboard.component.scss` — `.hero`, `.hero-greeting`, `.hero-date`.
- `docs/DESIGN_SYSTEM.md` §10 — amendement : l'en-tête d'accueil n'a plus de fond bleu (dérogation « couche d'accueil » close).
- Aucun HTML/TS/backend modifié.

## Hors périmètre

Sections sous le hero, backend, KPI/sparkline (déjà sobres).
