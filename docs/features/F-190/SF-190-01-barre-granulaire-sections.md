# SF-190-01 — Barre de progression granulaire + liste sections cliquables (frontend pure)

## Objectif

Remplacer le spinner indéterminé du bandeau « Synthèse en cours… » de F-185 SF-185-01 par une **barre de progression %** basée sur le nombre de sections reçues vs attendues, et afficher la **liste cliquable des sections déjà arrivées** sous le bandeau pour permettre à l'avocat de scroller vers le contenu disponible immédiatement.

## Contexte

F-185 SF-185-01 streame déjà les sections au fil de la génération Sonnet (`CASE_ANALYSIS_PARTIAL` SSE → `applyPartial()` projette le partial sur `synthesis()`). Aujourd'hui :
- Le bandeau affiche un spinner Material indéterminé + texte fixe.
- L'avocat ne sait pas combien de sections sont arrivées vs en attente.
- Pour voir une section déjà arrivée, l'avocat doit deviner si le contenu en bas de page est complet ou tronqué.

Cette SF est **pure exploitation côté frontend** des données déjà disponibles dans `CaseAnalysisPartialResponse` — aucune modification backend.

## Comportement nominal

1. Pendant le streaming (`isStreaming() === true`), le bandeau affiche :
   - Une **barre de progression linéaire** Material (`<mat-progress-bar mode="determinate">`) avec `value = round(received / expected * 100)`.
   - Un libellé `« N/M sections reçues »` (ex. `« 4/7 sections reçues »`).
   - Une **liste de chips cliquables** des sections déjà arrivées (Faits ✓, Timeline ✓, Risques ✓, …).
2. Cliquer sur une chip = scroll fluide vers le panel correspondant dans la pile inférieure (réutilise `scrollToBlock(anchor)` SF-162-01).
3. À la fin du streaming (`isStreaming() === false`), le bandeau disparaît (comportement actuel inchangé).
4. Sur la première réception du partial, si aucune section n'est encore arrivée, la barre est à 0 % et la liste est vide (texte « En cours… »).

## Sections attendues (sources de vérité)

Liste fixe de **7 sections visibles** côté avocat — alignée sur le contrat Sonnet existant :

| ID partial | Libellé UI | Anchor (pile inférieure) |
|------------|-----------|--------------------------|
| `timeline` | Chronologie | `section-timeline` |
| `faits` | Faits | `section-faits` |
| `points_juridiques` | Points juridiques | `section-points-juridiques` |
| `risques` | Risques | `section-risques` |
| `questions_ouvertes` | Questions ouvertes | `section-questions-ouvertes` |
| `pieces_manquantes` | Pièces manquantes | `section-pieces` |
| `risk_level` | Niveau de risque | (pas d'anchor, badge meta) |

Les champs `*_extracted_data` (travail / immigration / famille) ne sont **pas comptés** : ce sont des structures internes consommées par les outils décisionnels F-IA-04, pas des sections affichées dans la synthèse.

## Cas d'erreur / edge cases

- `isStreaming() === false` → bandeau invisible (inchangé).
- `lastPartial() === null` → barre à 0 %, liste « En cours… ».
- Le partial arrive avec une section présente mais en `null` (cas théorique du parseur incrémental SF-185-01) → la section n'est **pas** comptée comme reçue.
- Mobile (≤ 720px) : chips wrappent en plusieurs lignes, barre prend toute la largeur.

## Critères d'acceptation

- [ ] Pendant le streaming, le bandeau affiche `<mat-progress-bar mode="determinate">` avec value % calculée.
- [ ] Le texte affiche `« N/7 sections reçues »` à jour à chaque event SSE PARTIAL.
- [ ] Une chip par section reçue, dans l'ordre canonique de la liste fixe (pas dans l'ordre d'arrivée).
- [ ] Cliquer une chip scrolle vers le panel correspondant.
- [ ] Le bandeau disparaît à la fin du streaming (`status=DONE`).
- [ ] Sections de type `*_extracted_data` ignorées dans le compte.
- [ ] Tests Jest U1-U6 verts.
- [ ] DESIGN_SYSTEM.md respecté.

## Plan de test minimal

- **Jest** :
  - U1 : `streamingProgress()` retourne `{ received: [], expected: 7, percent: 0 }` quand `lastPartial` est null.
  - U2 : avec `partial.sections = { faits: [...], risques: [...] }` → `received.length === 2`, `percent === Math.round(2/7*100)`.
  - U3 : `risk_level` présent dans sections compté comme une section reçue.
  - U4 : `travail_extracted_data` présent dans sections **NON** compté.
  - U5 : `streamingSectionsReceived()` retourne les sections dans l'ordre canonique (pas l'ordre d'arrivée).
  - U6 : `applyPartial()` met à jour `lastPartial` ET `synthesis` (les deux signals).
- **Smoke E2E** : non requis.

## Tables / endpoints / composants impactés

- **Composant Angular** : `frontend/src/app/case-files/synthesis/synthesis.component.{ts,html,scss}`.
- **Tests** : `synthesis.component.spec.ts`.
- **Aucun nouvel endpoint, aucune migration**.

## Hors périmètre

- Streaming `EnrichedAnalysisService` → SF-190-02.
- Progression dans le bandeau case-file-detail (F-159) — la grille de badges F-162 sur la synthèse couvre déjà ce besoin.
- Refonte du parseur incrémental (`PartialJsonSectionExtractor`) — pas concerné.
- Affichage du contenu inline sous la barre — la pile inférieure F-185 et la grille de badges F-162 sont déjà la preview live, pas de duplication.

## Analyse de cohérence transversale

- **Préoccupations transversales** : aucune. Pure UI sur un composant existant.
- **Nouveau pattern UI** : barre de progression déterminée + chips cliquables. `<mat-progress-bar>` est déjà utilisé ailleurs (`AnalysisProgressBanner` F-159 SF-159-01 — barre indéterminée). Variante déterminée alignée DESIGN_SYSTEM.md (navy track, or fill). Pas de risque de divergence.
- **Impact par domaine métier** : transversal — affecte tous les domaines (Travail / Immigration / Famille) et 2 pays (FR / BE) de manière identique. Infra UI partagée du pipeline IA.

## Contrat API

Aucun. Consomme `CaseAnalysisPartialResponse` déjà disponible (SF-185-01).
