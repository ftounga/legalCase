# Mini-spec — F-282 / SF-282-01 — Cycle contradictoire du dossier (rounds d'échange) — V1

> Feature parente : **F-282**. Étape 0 : `SF-282-00-coherence.md` (GO). Étape 0 bis : `SF-282-00b-ux-coherence.md` (GO avec ajustements). Statut : `ready` · Date : 2026-06-12 · Branche : `feat/F-282-cycle-contradictoire`.

## Objectif
Modéliser le **cycle contradictoire** d'un dossier comme une suite de **rounds** (échanges de conclusions nous ↔ adverse), et l'afficher en une **frise visuelle de premier ordre** dans l'onglet Suivi, avec un **fil rouge** en en-tête (round courant, à qui le tour, prochaine échéance).

## Comportement attendu

### Cas nominal
1. À l'ouverture d'un dossier sans round, la frise montre l'état initial **« Round 1 — votre saisine »** (créé à la volée, party=OURS, non figé).
2. L'avocat **ajoute un round** (ex. « l'adversaire a conclu le 14/06, je dois répondre avant le 14/07 ») via un dialogue : party (Nous/Adverse), libellé, date (`dated_at`), échéance de réponse (`response_due_at`, optionnelle), pièce/conclusion source (optionnelle).
3. La **frise** affiche les rounds ordonnés : distinction visuelle **nous vs adverse**, état (déposé / reçu / **à vous · échéance J**), dates en JetBrains Mono.
4. Le **stepper d'en-tête** affiche un badge « Round N · à vous / en attente adverse · échéance JJ/MM » (calculé : round max + à qui le tour).
5. Au round « à vous », un bouton **« Générer ma réplique »** sur la frise **route vers l'onglet Décision** (génération des conclusions).
6. L'avocat peut **éditer / supprimer** un round.

### Cas d'erreur
| Situation | Comportement | Code |
|---|---|---|
| Champ obligatoire absent (party, dated_at) | message explicite | 400 |
| Dossier d'un autre workspace | accès refusé | 403/404 |
| Round inexistant (update/delete) | 404 | 404 |
| `response_due_at` < `dated_at` | 400 (validation) | 400 |

## Analyse de cohérence transversale
- **Autres domaines/pays** : le cycle contradictoire est **transversal** (FR+BE, 3 domaines) — la V1 ne porte aucune règle métier domaine-spécifique (un round = un échange daté). ✅ uniforme.
- **Conclusions (V4 en cours)** : **NE PAS toucher `app-conclusions-section`** en V1 (chantier parallèle) → le CTA *route* vers l'onglet Décision sans modifier la section. Intégration fine (bandeau round-aware, génération ciblée sur le jeu adverse du round) **différée V1.1**.
- **Échéances (F-69)** : un `response_due_at` de round **pourra** alimenter `app-case-deadlines-section` — **différé V1.1** (V1 affiche l'échéance dans la frise, invariant anti-orphelin respecté localement).
- **Résultat scan** : V1 = modèle + frise + en-tête, **0 modification de composant partagé décisionnel**, 0 outil décisionnel touché.

### Décision
- [x] V1 limitée au modèle de round + frise + indicateur en-tête (cibles applicables couvertes).
- [x] Backlog V1.1 : intégration `conclusions-section` (bandeau + génération round-aware) + alimentation `case-deadlines` + auto-dérivation des rounds.

## Conformité F-IA-04 (SF frontend décisionnelle)
- [x] **Non applicable** — F-282 n'est pas un outil décisionnel (`TOOL_REGISTRY`), c'est une vue de suivi procédural. Pas de pré-fill décisionnel, pas d'endpoint POST décisionnel.

## Champs IA à extraire (pré-remplissage)
- [x] **Aucun pré-remplissage IA en V1** — les rounds sont saisis par l'avocat (la dérivation auto depuis conclusions/documents = V1.1). Justification : V1 = modèle + frise ; l'extraction adverse (F-261) reste découplée.

## Critères d'acceptation
- [ ] Table `contradictoire_rounds` + migration Liquibase (isolation via `case_file_id` → workspace).
- [ ] CRUD `GET/POST/PUT/DELETE /api/v1/case-files/{id}/contradictoire-rounds`, **isolation workspace** vérifiée (un user du workspace A ne voit/écrit pas les rounds d'un dossier du workspace B).
- [ ] Round 1 « votre saisine » présenté par défaut si aucun round.
- [ ] Calcul du **round courant + à qui le tour + prochaine échéance** exposé pour le stepper.
- [ ] Validation `response_due_at ≥ dated_at` ; party ∈ {OURS, ADVERSE}.
- [ ] **Frise `app-contradictoire-timeline`** dans l'onglet Suivi : distinction nous/adverse, états de round, dates, **conforme `DESIGN_SYSTEM.md`** (navy/or, Merriweather/Inter/JetBrains Mono, 4px), **zéro tableau brut, zéro AI-generic**.
- [ ] Badge round dans `app-case-dashboard-stepper` (fil rouge inter-onglets).
- [ ] Bouton « Générer ma réplique » (au round « à vous ») → navigation vers l'onglet Décision.
- [ ] **Revue visuelle PO** de la frise (la beauté est un critère, pas un bonus).
- [ ] Aucune modification de `app-conclusions-section` (anti-collision V4).

## Périmètre — Hors scope V1 (explicite)
- **Badge round dans `app-case-dashboard-stepper`** (fil rouge en en-tête) → **V1.1** (décision dev : éviter de toucher un composant d'en-tête partagé ; le résumé « Round N · à vous · échéance » est rendu de façon proéminente **dans la frise** — bandeau-pilule en haut de la section Suivi, l'invariant fil-rouge est satisfait localement).
- Intégration *dans* `conclusions-section` (bandeau « réplique au round N », génération ciblée sur le jeu adverse du round) → **V1.1**.
- Alimentation de `app-case-deadlines-section` par les échéances de round → **V1.1**.
- Auto-dérivation des rounds (depuis versions de conclusions DEPOSITED / documents tagués adverses) → **V1.1**.
- Spécificités BE / multi-juridiction.

## Technique
### Tables
| Table | Opération | Notes |
|---|---|---|
| `contradictoire_rounds` | CREATE (migration) + CRUD | `id` UUID, `case_file_id` UUID FK, `round_number` int, `party` varchar(10) {OURS,ADVERSE}, `label` varchar(200), `dated_at` date, `response_due_at` date null, `source_document_id` UUID null, `source_conclusion_id` UUID null, `created_at`/`updated_at` |
### Migration Liquibase
- [x] Oui — `{NNN}-create-contradictoire-rounds.xml`
### Endpoints
| Méthode | URL | Rôle |
|---|---|---|
| GET | `/api/v1/case-files/{caseFileId}/contradictoire-rounds` | LAWYER (workspace) |
| POST | `/api/v1/case-files/{caseFileId}/contradictoire-rounds` | LAWYER |
| PUT | `/api/v1/case-files/{caseFileId}/contradictoire-rounds/{roundId}` | LAWYER |
| DELETE | `/api/v1/case-files/{caseFileId}/contradictoire-rounds/{roundId}` | LAWYER |
### Composants Angular
- `ContradictoireTimelineComponent` (frise, onglet Suivi) — la pièce vedette design.
- `ContradictoireRoundDialogComponent` (ajout/édition).
- `ContradictoireService` + `ContradictoireRound` model.
- Enrichissement `app-case-dashboard-stepper` (badge round).

## Plan de test
### Unitaires (backend)
- [ ] `ContradictoireService` — création, calcul round courant / à qui le tour, validation `response_due_at ≥ dated_at`.
### Intégration
- [ ] `POST` → 201 ; `GET` ordonné ; `PUT`/`DELETE` ; 400 champ manquant / date incohérente ; **403/404 isolation workspace**.
### Isolation workspace
- [x] Applicable — testée (workspace A ≠ B).
### Jest (frontend)
- [ ] Frise rend les rounds (nous/adverse), états ; dialogue valide/invalide ; bouton « Générer ma réplique » navigue vers Décision ; badge stepper.

## Analyse d'impact
### Préoccupations transversales
- [x] **Aucune** — pas d'auth/Principal modifié, pas de nouveau contexte workspace (réutilise l'isolation via `case_file`), pas de plan/limite, **nouvelle route Angular interne au détail dossier** (onglet Suivi, pas de guard nouveau).
### Smoke E2E
- [x] Aucun smoke bloquant (pas d'impact auth/workspace global). Couvert par IT backend + Jest.
