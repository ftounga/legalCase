# Mini-spec — F-271 / SF-271-01 — Conclusions récapitulatives & reprise des éditions (+ F-278 garde anti-écrasement)

## Identifiant
`F-271 / SF-271-01` (backend) + `F-278 / SF-278-01` (frontend, couplé — même PR logique, livré après le backend)

## Feature parente
`F-271` — Conclusions V4 ① récapitulatives & reprise des éditions (art. 768 CPC), **couplée** `F-278` — garde anti-écrasement à la régénération.

## Statut
`ready`

## Date
2026-06-12

## Branches
- Backend : `feat/SF-271-01-recap-reprise-editions`
- Frontend : `feat/SF-278-01-garde-regeneration`

---

## Objectif
À la régénération de conclusions, repartir du **dernier jeu (content édité inclus)** et le **consolider** (jeu récapitulatif, art. 768 CPC), au lieu de regénérer from-scratch — et **confirmer** la régénération par une boîte de dialogue informative unique (F-278).

---

## Comportement attendu

### Backend (F-271)
- **Cas nominal (régénération)** : il existe au moins une version **DONE** avec `content` non vide pour le dossier → `prepare()` charge ce content comme **base récapitulative** et l'injecte dans le user message (section `=== BASE À CONSOLIDER (jeu de conclusions précédent) ===`), avec une **garde 768 CPC** dans `REDACTION_QUALITY_GUARD` : reprendre tous les chefs de demande/moyens de la base (rien d'abandonné sans raison), les enrichir des nouveaux éléments (analyse, moyens adverses), produire un jeu **récapitulatif**.
- **Première génération** : aucune version DONE antérieure (ou content vide) → comportement **inchangé** (from-scratch), pas de section « base à consolider ».
- **Base = dernière version DONE** par `version_number` desc, content non blank. Les versions PENDING/PROCESSING/FAILED/content vide sont ignorées. La version **courante en cours de génération** (PENDING qu'on vient de créer) n'est jamais sa propre base.

### Frontend (F-278)
- Au clic « Régénérer », **si** `hasVersions()` (une version existe déjà) → ouvrir `ConfirmDialogComponent` : titre « Régénérer les conclusions », message « La régénération crée une nouvelle version qui **repart de vos conclusions actuelles (vos modifications incluses)** et les consolide. Continuer ? », confirm « Régénérer » (`primary`). Sur confirmation → `generate()` actuel. Sur annulation → no-op.
- **Première génération** (aucune version) → pas de dialogue, `generate()` direct (inchangé).

### Cas d'erreur
- Lecture de la base échoue / content corrompu → **fail-open** : on génère sans base (log warn), jamais d'exception propagée (cohérent avec `loadActiveStyleSignatures`).
- Toutes les gardes existantes de `triggerGeneration` (STAGE_NOT_SET, COMBINATION_NOT_SUPPORTED, ANALYSIS_NOT_READY, ALREADY_GENERATING) restent prioritaires et inchangées.

---

## Critères d'acceptation vérifiables
1. Régénération avec une version DONE éditée → le user message contient la section « BASE À CONSOLIDER » avec le content édité. (test unitaire prompt builder + service)
2. Première génération (aucune version DONE) → pas de section « BASE À CONSOLIDER ». (test)
3. Version précédente PENDING/PROCESSING/content vide → ignorée, pas de base. (test)
4. `REDACTION_QUALITY_GUARD` contient la consigne récapitulatif/768 quand une base est présente. (test)
5. Échec de lecture base → génération poursuivie sans base (fail-open). (test)
6. Frontend : clic « Régénérer » avec version existante → dialogue ouvert ; confirmation → `generate()` appelé ; annulation → `generate()` non appelé. (test composant)
7. Frontend : première génération → pas de dialogue, `generate()` appelé directement. (test)
8. Isolation workspace inchangée (la base est lue via le repository déjà scoping caseFileId du dossier résolu en workspace).

---

## Contrat (figé — parallélisation back/front)
**Aucun changement d'API REST.** F-271 est 100 % interne au worker de génération (prompt). F-278 est 100 % frontend (dialogue avant l'appel `POST .../generate` existant). Les deux branches sont **indépendantes** : le backend modifie le prompt builder + service de génération ; le frontend ajoute un dialogue avant l'appel existant. Pas de champ DTO partagé → contrat figé = « endpoint `generate` inchangé ».

---

## Tables / endpoints / composants impactés
- **Backend** : `CaseConclusionService.prepare()` (charge la base), `CaseConclusionPromptBuilder` (nouveau champ `previousRecapContent` dans `ConclusionPromptInput` + section + garde 768 dans `REDACTION_QUALITY_GUARD`). **Aucune migration, aucune table, aucun endpoint.**
- **Frontend** : `conclusions-section.component.ts` (méthode `regenerate()` enveloppant `generate()` via `ConfirmDialogComponent`), template bouton « Régénérer » → `regenerate()`.

## Hors périmètre
- WYSIWYG (F-277), diff de versions (F-280), sommaire (F-276), « sauf à parfaire » (F-273), pièces adverses (F-274), identités (F-275 — déjà partiellement couvert SF-266 / garde point 6).
- Choix « régénérer from-scratch » vs « actualiser » côté UI (un seul mode : récapitulatif). Différé si signal terrain.
- BE / multi-pays : la garde 768 est CPC (FR) ; en pratique la consigne « reprends et consolide la base » reste pertinente toutes cellules, mais la référence 768 n'est mentionnée que comme principe de reprise.

## Transversal
- Auth/Principal : inchangé. Workspace : inchangé (base lue dans le dossier déjà résolu). Plans/limites : inchangé. Navigation : inchangé. Outil décisionnel : non concerné.
- Pas de smoke E2E obligatoire (aucune route/auth/workspace touchée) ; tests unitaires backend + composant frontend suffisent.
