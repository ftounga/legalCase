# SF-266-03 — Alerte des emplacements à compléter avant export

> Extension F-266 (« acte déposable »). Signal PO 2026-06-11. Frontend-only.

## Objectif (1 phrase)
Avant l'export PDF/Word des conclusions, **alerter l'avocat** des emplacements `[ … ]` encore à compléter (date, nom/qualité de l'avocat…) et **confirmer** avant d'exporter un acte incomplet.

## Comportement nominal
- À l'affichage d'un acte `DONE`, on calcule `placeholdersToFill` = liste dédupliquée des tokens `[ … ]` présents dans le `content` (hors liens markdown et renvois numériques).
- **Si ≥ 1 placeholder** : un **bandeau** s'affiche au-dessus des boutons d'export — titre « N élément(s) à compléter avant dépôt », liste des placeholders, bouton **« Compléter »** (entre en mode édition F-264).
- **Au clic « Télécharger PDF / Word »** : s'il reste des placeholders → **confirmation** (`ConfirmDialogComponent`) « L'acte contient encore N élément(s) à compléter : … Exporter quand même ? ». Sur **Exporter quand même** → export ; sur **Annuler** → rien.
- **Si 0 placeholder** : aucun bandeau, export direct (comportement F-266 inchangé).

## Cas d'erreur / bords
- `content` vide/null → `placeholdersToFill = []` (pas de bandeau).
- Liens markdown `[texte](url)` et renvois `[1]` → **non** comptés.
- Export hors état `DONE` → ignoré (inchangé).

## Critères d'acceptation
1. `extractPlaceholders` détecte `[à compléter]`, `[Nom et qualité de l'avocat]`, `[Date]`, `[Lieu]` ; déduplique ; ignore `[lib](url)` et `[1]`.
2. Bandeau visible ssi acte `DONE` + ≥ 1 placeholder ; liste exacte ; compteur exact.
3. Export avec placeholders → confirmation ouverte ; export seulement si confirmé.
4. Export sans placeholder → aucun dialog, export direct.
5. Bouton « Compléter » → passe en mode édition.

## Plan de test
- Jest (6, ci-dessus) — `conclusions-section.component.spec.ts` : fonction pure + bandeau + confirmation acceptée/refusée + export direct.
- Isolation workspace : N/A (lecture du content déjà chargé).

## Composants impactés
- `conclusions-section.component.{ts,html,scss,spec.ts}` uniquement. Réutilise `ConfirmDialogComponent`.

## Hors périmètre
- Détection sémantique fine (ex. « date manquante » sans crochet) — on s'appuie sur les placeholders posés par le générateur.
- Toute modification backend / du prompt.
