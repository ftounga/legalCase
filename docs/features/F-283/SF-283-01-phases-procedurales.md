# Mini-spec — F-283 / SF-283-01 — Phases procédurales (progression datée du dossier)

> Feature parente : **F-283**. Étape 0 : `SF-283-00-coherence.md` (GO). Étape 0 bis : `SF-283-00b-ux-coherence.md` (GO avec ajustements). Statut : `ready` · Date : 2026-06-12 · Branches : `feat/SF-283-01-phases-back` (backend) // `feat/SF-283-01-phases-front` (frontend).

## Objectif
Modéliser la **progression du dossier par phases datées** (Saisine → Conciliation → Fond → Appel → Cassation) et l'afficher en une **frise compacte de premier ordre** en tête de l'onglet Suivi, avec la **phase courante** en exergue.

## Comportement attendu

### Cas nominal
1. À l'ouverture d'un dossier sans phase, la frise affiche l'état initial **« Phase 1 — Saisine »** (présenté à la volée, non figé en base).
2. L'avocat **ajoute une transition de phase** via un formulaire inline (mirror F-282) : `phase` (sélecteur parmi le référentiel), `label` (optionnel), `enteredAt` (date d'entrée dans la phase), `note` (optionnelle).
3. La **frise** affiche les phases ordonnées par `enteredAt` puis `phaseOrder` : puce + libellé (`Merriweather`) + date (`JetBrains Mono`), la **phase courante** (la dernière entrée) en navy plein avec pulsation discrète.
4. L'avocat peut **éditer / supprimer** une transition.
5. La frise est **compacte** (bande verticale type frise F-282, max-width « document »).

### Cas d'erreur
| Situation | Comportement | Code |
|---|---|---|
| Champ obligatoire absent (`phase`, `enteredAt`) | message explicite | 400 |
| `phase` hors référentiel | rejet | 400 |
| Dossier d'un autre workspace | accès refusé | 404 |
| Phase inexistante (update/delete) | 404 | 404 |

## Référentiel de phases (réutilise le vocabulaire F-243, pas de nouveau référentiel métier risqué)
Enum `CasePhaseType` (ordre = `phaseOrder`) :
`SAISINE(1)`, `CONCILIATION(2)`, `MISE_EN_ETAT(3)`, `FOND(4)`, `JUGEMENT(5)`, `APPEL(6)`, `CASSATION(7)`, `EXECUTION(8)`.
Transversal (FR+BE, 3 domaines) — un libellé de phase = un stade procédural daté, sans règle métier domaine-spécifique. Libellés FR affichés côté front via un mapping de présentation.

## Analyse de cohérence transversale
- **vs F-243** : `procedure_stage` (libellé courant) **non touché**. `case_phases` = trace datée. La frise lit/affiche, ne réécrit pas F-243. (Synchronisation auto F-243↔phase courante = **différée V1.1**.)
- **vs F-282** : table distincte, axe orthogonal (phase ⊃ rounds). Aucun composant F-282 touché.
- **Conclusions / outils décisionnels** : **0 modification** (`TOOL_REGISTRY`, gate F-IA-03, pré-fill : non applicable).
- **Échéances (F-69)** : dérivation d'échéances-type par phase = **différée V1.1** (V1 affiche la date d'entrée dans la frise, invariant anti-orphelin respecté localement).

## Conformité F-IA-04 (SF frontend décisionnelle)
- [x] **Non applicable** — vue de suivi procédural, pas d'outil décisionnel (`TOOL_REGISTRY`), pas de pré-fill, pas d'endpoint décisionnel.

## Champs IA à extraire (pré-remplissage)
- [x] **Aucun pré-remplissage IA en V1** — les phases sont saisies par l'avocat (dérivation auto depuis documents = V1.1).

## Critères d'acceptation
- [ ] Table `case_phases` + migration Liquibase (isolation via `case_file_id` → workspace).
- [ ] CRUD `GET/POST/PUT/DELETE /api/v1/case-files/{id}/phases`, **isolation workspace** vérifiée (workspace A ≠ B).
- [ ] « Phase 1 — Saisine » présentée par défaut si aucune phase.
- [ ] Calcul de la **phase courante** (dernière entrée) exposé dans la réponse.
- [ ] Validation `phase ∈ référentiel`, `enteredAt` requis.
- [ ] **Frise `app-case-phases-timeline`** en tête de l'onglet Suivi : phase courante en exergue, dates `JetBrains Mono`, **conforme `DESIGN_SYSTEM.md`** (navy/or, Merriweather/Inter/JetBrains Mono, 4px), **zéro tableau brut, zéro AI-generic**, **compacte** (ne concurrence pas le contradictoire).
- [ ] Champs date **natifs `type=date` lang=fr-FR** (pas MatDatepicker).
- [ ] **Revue visuelle PO** de la frise (beauté = critère).
- [ ] Aucune modification de `app-procedure-stage-section` (F-243) ni de la frise contradictoire (F-282).

## Périmètre — Hors scope V1 (explicite)
- Synchronisation auto `procedure_stage` (F-243) ↔ phase courante → **V1.1**.
- Dérivation d'échéances-type par phase (F-69) → **V1.1**.
- Auto-dérivation des phases depuis documents/analyse → **V1.1**.
- Spécificités BE / multi-juridiction (référentiel uniforme V1).

## Technique
### Contrat API (figé — parallélisation back//front)
- `GET /api/v1/case-files/{caseFileId}/phases` → `{ phases: CasePhaseResponse[], currentPhase: CasePhaseType | null }`
- `POST` body `CasePhaseRequest { phase, label?, enteredAt (yyyy-MM-dd), note? }` → 201 `CasePhaseResponse`
- `PUT /{phaseId}` body `CasePhaseRequest` → 200 `CasePhaseResponse`
- `DELETE /{phaseId}` → 204
- `CasePhaseResponse { id, phase, label, enteredAt, note, createdAt, updatedAt }`

### Tables
| Table | Opération | Notes |
|---|---|---|
| `case_phases` | CREATE (migration **601**) + CRUD | `id` UUID PK, `case_file_id` UUID FK CASCADE, `phase` varchar(30) NOT NULL, `label` varchar(200) null, `entered_at` date NOT NULL, `note` varchar(2000) null, `created_at`/`updated_at` timestamptz NOT NULL. Index sur `case_file_id`. UUID changeSet pré-assigné : `601-create-case-phases`. |

### Endpoints
| Méthode | URL | Rôle |
|---|---|---|
| GET | `/api/v1/case-files/{caseFileId}/phases` | LAWYER (workspace) |
| POST | `/api/v1/case-files/{caseFileId}/phases` | LAWYER |
| PUT | `/api/v1/case-files/{caseFileId}/phases/{phaseId}` | LAWYER |
| DELETE | `/api/v1/case-files/{caseFileId}/phases/{phaseId}` | LAWYER |

### Composants Angular
- `CasePhasesTimelineComponent` (`app-case-phases-timeline`, frise, onglet Suivi en tête) — pièce design.
- `CasePhaseService` + `CasePhase` model (`core/services` + `core/models`).

## Plan de test
### Unitaires (backend)
- [ ] `CasePhaseService` — création, calcul phase courante, validation `phase` requis / enum, isolation.
### Intégration
- [ ] `POST` → 201 ; `GET` ordonné + `currentPhase` ; `PUT`/`DELETE` ; 400 champ manquant ; **404 isolation workspace** (A ≠ B).
### Isolation workspace
- [x] Applicable — testée.
### Jest (frontend)
- [ ] Frise rend les phases ordonnées + phase courante en exergue ; état initial « Phase 1 — Saisine » si vide ; formulaire add/edit valide/invalide ; delete recharge.

## Analyse d'impact
### Préoccupations transversales
- [x] **Navigation / routing** : aucune nouvelle route Angular (composant interne à l'onglet Suivi du détail dossier, pas de guard). **Auth/Principal** : inchangé (réutilise l'isolation `case_file` via `WorkspaceMemberRepository`, pattern F-282). **Plans/limites** : aucun. **Outil décisionnel** : aucun.
- Composants impactés : `case-file-detail.component.html` (insertion `<app-case-phases-timeline>` en tête onglet Suivi). **Aucun** composant partagé décisionnel.
### Smoke E2E
- [x] Aucun smoke bloquant (pas d'impact auth/workspace/navigation globale). Couvert par IT backend + Jest.
