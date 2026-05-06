# Mini-spec — F-194 / SF-194-02 Frontend — UI tags pièces dans synthèse + sortie outils + tile dashboard

## Identifiant

`F-194 / SF-194-02`

## Feature parente

`F-194` — Pièces manquantes markables + matérialisation au run enrichi

## Statut

`draft`

## Date de création

2026-05-06

## Branche Git

`feat/SF-194-02-frontend-pieces-markables`

## Pattern de référence

**SF-192-02 + SF-193-02** (mergées 2026-05-06 / en cours). Nouveauté F-194 : **introduction d'une UI markable** sur le bloc pieces_manquantes du `SynthesisComponent` (aujourd'hui en lecture seule).

## Contrat API importé de SF-194-01-backend

- `PUT /api/v1/case-files/{id}/pieces-manquantes/status` body `{ pieceLibelleOriginal, statut: 'A_DEMANDER' | 'OBTENUE' | 'NON_APPLICABLE', raisonNonApp?, destinataire? }` → `PieceManquanteStatus`
- `GET /api/v1/case-files/{id}/pieces-manquantes-alignment` → `PieceManquanteAlignment[]`
- `PieceManquanteAlignment { libelle, statut, toolIdsCibles[], destinataire?, raisonNonApp? }`
- Tile dashboard `{ toolId: 'F-194-pieces-summary', theme: 'DOCUMENTS', label, primaryValue, secondaryValue?, alertLevel? }`

---

## Objectif

Côté frontend : (1) ajouter une UI **markable** sur le bloc Pièces manquantes du `SynthesisComponent` (3 boutons statut par pièce, MatSelect pour destinataire, champ raison non applicable optionnel) ; (2) afficher l'alignement pièces ↔ outils décisionnels qui en dépendent (signal `piecesObtenues` pour pré-cocher dans le formulaire outil) ; (3) tile dashboard `F-194-pieces-summary` thème DOCUMENTS clic → scroll vers le bloc Pièces de la synthèse.

---

## Comportement attendu

### Cas nominal

1. **UI markable dans `SynthesisComponent`** — bloc `pieces_manquantes` actuel passe de simple liste à liste interactive :
   - Chaque pièce affiche 3 boutons statut alignés DESIGN_SYSTEM.md : 📩 `À_DEMANDER` (par défaut, navy/or) / ✅ `OBTENUE` (vert/or) / 🚫 `NON_APPLICABLE` (gris)
   - Clic sur statut → `PUT /pieces-manquantes/status` avec libellé original + statut sélectionné
   - Si `NON_APPLICABLE` : champ texte `raisonNonApp` (optionnel) se déplie sous la pièce
   - Si `À_DEMANDER` : MatSelect `destinataire` (optionnel) avec choix : "Client", "Ex-employeur", "Préfecture", "Autre" (pour libre)
   - Provenance : si pièce a déjà un statut persisté en base, le bouton correspondant est mis en évidence ; sinon par défaut `À_DEMANDER` (visuel implicite, pas d'appel PUT)
   - **Cohérence F-176 stricte** : aucun rafraîchissement UI au PUT (pas de `triggerRefresh()`, pas de re-fetch alignement)
2. **Lecture alignement** : au montage du dossier, `PieceManquanteAlignmentService.getForCaseFile(caseFileId)` charge l'alignement matérialisé. Signal cache exposé.
3. **Sortie outils décisionnels** : extension `TOOL_REGISTRY` pour les outils qui dépendent d'une ou plusieurs pièces, recevant `piecesObtenues: string[]` (libellés des pièces statut OBTENUE depuis `alignment`). Outils impactés (à valider par audit dans le dev — exemples) :
   - `F-DT-04-fiche-prudhomale` (FR) : libellés "contrat de travail", "fiches de paie", "lettre de licenciement"
   - `F-DT-06-requete-tribunal-travail` (BE) : équivalents
   - `F-IM-05-arbre-decisionnel-titre` : "titre de séjour actuel", "acte de mariage" (si motif FAMILLE)
   - `F-IM-06-recours` : "décision préfectorale contestée", "preuve dépôt RAPO"
   - `F-FA-07-checklist-divorce` : "acte de mariage", "livret de famille"
   
   Pour chaque outil concerné : nouveau `@Input() piecesObtenues?: string[]` + utilisation pour pré-cocher / valider les champs internes correspondants (ex. checkbox "J'ai le contrat de travail" pré-cochée).
4. **Tile dashboard** `F-194-pieces-summary` thème DOCUMENTS rendue via `<app-dashboard-tile>` existant. Particularité : clic → `Router.navigate(['/case-files', id, 'synthesis'], { fragment: 'section-pieces' })` (créer anchor si nécessaire).
5. **Cycle de rafraîchissement** : signal `alignment` ré-fetché uniquement à la réception de l'event SSE `ENRICHED_ANALYSIS DONE`. Le PUT statut pièce ne déclenche AUCUN refresh côté front.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| PUT statut → 400 (statut invalide) | Snackbar erreur "Statut invalide", bouton revenu à son état précédent |
| PUT statut → 401/403/404 | Snackbar erreur générique, bouton revenu à son état précédent |
| PUT statut → 5xx ou timeout | Snackbar erreur "Erreur serveur, réessayez", optimistic update annulé |
| GET alignment → 404/500/timeout | Fail-open silencieux, alignement = [], log console, sortie outils inchangée |
| Aucune pièce matérialisée | Tile absente du dashboard |

---

## Critères d'acceptation

- [ ] **CA-01** : `PieceManquanteStatusService.update(caseFileId, libelle, statut, ...)` PUT effectif sur backend, signal local mis à jour optimistiquement
- [ ] **CA-02 UI markable** : bloc Pieces manquantes du `SynthesisComponent` affiche 3 boutons statut par pièce, statut persisté visible (mis en évidence du bon bouton)
- [ ] **CA-03 NON_APPLICABLE raison** : clic sur 🚫 NON_APPLICABLE → champ texte `raisonNonApp` se déplie ; saisie + blur → PUT inclut la raison
- [ ] **CA-04 destinataire À_DEMANDER** : clic sur 📩 À_DEMANDER → MatSelect destinataire affiché ; sélection → PUT inclut le destinataire
- [ ] **CA-05 PUT sans refresh** : après PUT, **aucun appel** à `triggerRefresh()` ni à `loadAlignment()` côté SynthesisComponent. Test régression critique.
- [ ] **CA-06 alignement lecture** : `PieceManquanteAlignmentService.getForCaseFile(id)` au montage du dossier, signal exposé pour outils + dashboard
- [ ] **CA-07 sortie outils piecesObtenues** : F-DT-04 reçoit `piecesObtenues = ["contrat de travail"]` quand cette pièce est statut OBTENUE → la checkbox correspondante est pré-cochée
- [ ] **CA-08 tile dashboard** : tile `F-194-pieces-summary` thème DOCUMENTS rendue avec primary/secondary/alertLevel corrects
- [ ] **CA-09 tile clic** : clic → navigation vers `/case-files/{id}/synthesis#section-pieces`
- [ ] **CA-10 refresh au run synthèse enrichie** : event SSE `ENRICHED_ANALYSIS DONE` → re-fetch alignement → tile + outils mis à jour
- [ ] **CA-11 fail-open** : endpoint GET timeout → blocs vides, pas de spinner persistant
- [ ] **CA-12 OnPush + markForCheck** : tous subscribe() qui mutent l'état affiché injectent ChangeDetectorRef + markForCheck()
- [ ] **CA-13 visuel charte** : palette navy/or/gris DESIGN_SYSTEM.md (cohérent F-176 SF-176-02 — pas de rouge dominant pour NON_APPLICABLE qui est gris discret)
- [ ] **CA-14 erreur PUT** : snackbar visible, état UI revient à son statut précédent (rollback optimistic update)

---

## Périmètre

### Hors scope V1

- (a) Édition libre du libellé pièce (V1 = libellé issu de l'IA, l'avocat ne peut que tagger)
- (b) Workflow de relance email automatique aux destinataires
- (c) Drag-and-drop entre statuts
- (d) Date butoir personnalisable côté frontend (V1 = today + 14j fixe côté backend)
- (e) Filtres / recherche dans la liste pièces (V1 = liste flat ordonnée par défaut)
- (f) Notifications push quand délai butoir approche

---

## Technique

### Composants Angular impactés

- `PieceManquanteAlignmentService` (nouveau) — `core/services/piece-manquante-alignment.service.ts`
- `PieceManquanteStatusService` (nouveau) — `core/services/piece-manquante-status.service.ts` — méthode `update(caseFileId, payload)`
- `piece-manquante-alignment.model.ts` (nouveau) — interface miroir
- `<app-synthesis>` extension — bloc Pieces manquantes refondu (3 boutons + champ raison + select destinataire)
- Outils décisionnels concernés (~5-6) à étendre avec `@Input() piecesObtenues?: string[]` et pré-cochage cohérent
- `<app-decisional-tools-panel>` `TOOL_REGISTRY` étendu pour propager `piecesObtenues`
- `<app-case-dashboard>` mapping toolId `F-194-pieces-summary` → label "Pièces" + clic handler

### Migration

- [x] Aucune (couverte SF-194-01)

---

## Plan de test

### Tests Jest (~12-15 tests)

- `PieceManquanteStatusServiceTest` (3 : update success / 400 / 500)
- `PieceManquanteAlignmentServiceTest` (3 : success / 404 / 500 fail-open)
- `SynthesisComponentTest` extension (4-5 tests : 3 boutons rendu, clic OBTENUE → PUT, NON_APPLICABLE → raison field, À_DEMANDER → destinataire select, refresh au SSE seulement)
- 2-3 outils représentatifs : pré-cochage `piecesObtenues` (3 tests)
- `CaseDashboardComponentTest` (2 tests : tile + clic)

### Isolation workspace

- [x] Non applicable côté frontend pur

---

## Dépendances

- F-92 ✅ Terminée
- F-192 SF-192-02 ✅ Terminée (pattern partagé)
- F-193 SF-193-02 (en cours dev) — pattern partagé
- F-167 ✅ Terminée
- **SF-194-01 backend** — contrat API figé

---

## Notes et décisions

- **Décision 2026-05-06** : statuts `À_DEMANDER` / `OBTENUE` / `NON_APPLICABLE` cohérents F-176 trichotomie. NON_APPLICABLE en gris discret (cohérent F-176 statut Écartée gris).
- **Décision 2026-05-06** : optimistic update au PUT (UI changement immédiat, rollback si erreur) — UX plus fluide. Pas de re-fetch alignement post-PUT (cohérence F-176 stricte).
- **Décision 2026-05-06** : pré-cochage `piecesObtenues` dans les outils via match libellé exact — cohérent backend mini-spec V1. Évolution V2 fuzzy matching si besoin.
- **Décision 2026-05-06** : tile dashboard thème DOCUMENTS (différent F-192 DIAGNOSTIC / F-193 DELAIS / F-194 DOCUMENTS — les 3 cohérents avec leur nature respective).
