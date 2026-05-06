# Mini-spec — F-194 / SF-194-01 Backend — Pièces manquantes markables + matérialisation au run enrichi

## Identifiant

`F-194 / SF-194-01`

## Feature parente

`F-194` — Pièces manquantes markables + matérialisation au run enrichi (todo-list pièces + délais auto au client)

## Statut

`draft`

## Date de création

2026-05-06

## Branche Git

`feat/SF-194-01-backend-pieces-markables`

## Pattern de référence

**SF-192-01-backend.md** (mergée 2026-05-06, PR #861) + **SF-193-01-backend.md** (en cours de dev) — F-194 réplique ce pattern sur le bloc `pieces_manquantes` mais introduit une **nouveauté** : passer pieces_manquantes de lecture-seule à markable. Étend F-92 (Terminée).

---

## Objectif

(1) Passer le bloc `pieces_manquantes` (aujourd'hui en lecture seule, persisté dans le JSON `analysis_result`) en **markable** : chaque pièce reçoit un statut trichotomique (`À_DEMANDER` / `OBTENUE` / `NON_APPLICABLE`) curable par l'avocat. (2) Appliquer le pattern F-192/F-193 : tag = persistance pure, effets matérialisés au run de Synthèse enrichie (propagation délais auto, signal vers outils décisionnels qui dépendent d'une pièce, tile dashboard).

---

## Modèle d'activation — gating Synthèse enrichie strict

Strictement aligné F-192/F-193. Le PUT statut pièce reste un acte pur, **strictement aucun side-effect**. Tous les effets F-194 se déclenchent uniquement au prochain run de Synthèse enrichie via un nouveau hook `materializeForAnalysis(newAnalysis)` appelé depuis `EnrichedAnalysisService.run` à la fin (APRÈS F-192 et F-193 hooks).

---

## Architecture — décision « table dédiée vs JSON »

**Option retenue V1 — Option A : nouvelle table `piece_manquante_status`** (overlay sur le JSON pieces_manquantes existant). Justification :

- F-92 actuel : `pieces_manquantes` est un tableau JSON dans `case_analyses.analysis_result`. Pas de PRIMARY KEY pour identifier individuellement chaque pièce d'une analyse.
- Refactoring vers une table dédiée (Option B) = changement de format de données massif, casserait F-92 et F-IA-03 (qui lit `pieces_manquantes` en cohérence).
- Option A : conserver le JSON existant ET ajouter une table `piece_manquante_status` qui stocke le statut avocat **par libellé normalisé**. Lors du run de Synthèse enrichie, l'analyse régénère sa liste pieces_manquantes, et la matérialisation re-joint le statut avocat via libellé normalisé.

**Schéma `piece_manquante_status`** :
```
id UUID PRIMARY KEY
case_file_id UUID NOT NULL REFERENCES case_files(id)
workspace_id UUID NOT NULL REFERENCES workspaces(id)  -- isolation stricte
piece_libelle_normalise VARCHAR(500) NOT NULL  -- trim().toLowerCase()
piece_libelle_original VARCHAR(500) NOT NULL  -- pour ré-affichage
statut VARCHAR(20) NOT NULL  -- A_DEMANDER / OBTENUE / NON_APPLICABLE
raison_non_app TEXT NULL
destinataire VARCHAR(200) NULL  -- "client" / "ex-employeur" / etc.
created_at TIMESTAMP NOT NULL
updated_at TIMESTAMP NOT NULL
UNIQUE (case_file_id, piece_libelle_normalise)
```

Le statut par défaut implicite (sans entrée table) = `À_DEMANDER`.

---

## Comportement attendu

### Cas nominal

1. L'avocat tag une pièce via `PUT /api/v1/case-files/{id}/pieces-manquantes/status` avec body `{ pieceLibelleOriginal, statut, raisonNonApp?, destinataire? }` :
   - Le service normalise le libellé (`trim().toLowerCase()`), upsert dans `piece_manquante_status` (clé `(case_file_id, piece_libelle_normalise)`)
   - **Aucun side-effect** : pas de mise à jour pieces_manquantes JSON, pas de délai créé
   - Réponse 200 OK avec l'entrée upsertée
2. L'avocat clique « Synthèse enrichie ». `EnrichedAnalysisService.run` exécute son flow existant. À la **fin** du run, après commit de la nouvelle analyse, `PieceManquanteAlignmentService.materializeForAnalysis(newAnalysis)` :
   - Lit le `pieces_manquantes` JSON de la nouvelle analyse (généré par l'IA — peut différer de l'analyse précédente si les documents ont changé)
   - Pour chaque pièce, joint le statut avocat depuis `piece_manquante_status` via libellé normalisé
   - Sérialise l'alignement en JSON dans nouvelle colonne `case_analyses.pieces_alignment_json` (mapping `pieceLibelle → { statut, toolIdsCibles[], destinataire?, raison_non_app? }`)
   - Pour chaque pièce statut `À_DEMANDER` : si pas déjà présent, crée entrée `case_deadlines` source `PIECE_A_DEMANDER` avec `dateCible = today + 14j` (configurable plus tard) et label `"Demander au client : <pièce>"` (idempotent sur clé `(caseFileId, source, label)`)
   - Pour chaque pièce statut `OBTENUE` : pas de side-effect immédiat sur pieces_manquantes (l'IA a déjà la liste — la matérialisation retire juste la pièce du JSON pieces_manquantes (ou la flag) en V1 pas de modif, juste l'alignement)
3. Le frontend lit l'état via `GET /api/v1/case-files/{id}/pieces-manquantes-alignment` (lecture pure sur la dernière `CaseAnalysis` DONE).
4. **Prompt enrichi** étendu : `EnrichedAnalysisService` reçoit aussi les statuts pièces avocat et instruit l'IA :
   - `[Pièces déjà obtenues — ne pas réclamer]` : liste des pièces statut OBTENUE
   - `[Pièces non applicables au dossier — ne pas mentionner]` : liste des pièces statut NON_APPLICABLE
   - `[Pièces à demander au client — pousser explicitement dans la nouvelle synthèse]` : liste des pièces statut À_DEMANDER

   **Implémentation** : nouveau service `PieceManquanteStatusService.collectForEnrichment(caseFileId)` retournant `{obtenues: List<String>, nonApplicables: List<String>, aDemander: List<String>}`. Hook ajouté dans `EnrichedAnalysisService.buildEnrichedPrompt` (existant — étendre avec sections similaires aux RETAINED/DISCARDED de F-176).
5. `CaseFileDashboardService.assembleTiles` ajoute la tile `F-194-pieces-summary` thème **DOCUMENTS** :
   - `primaryValue` = N total pièces matérialisées
   - `secondaryValue` = "X à demander · Y obtenues · Z non applicables"
   - `alertLevel` : `WARNING` si ≥ 1 À_DEMANDER ET dernier run > 7j, `OK` sinon

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Case file inexistant ou autre workspace | 404 camouflage | 404 |
| Statut invalide (hors enum) | 400 "Statut invalide" | 400 |
| `pieceLibelleOriginal` vide ou > 500 chars | 400 | 400 |
| `raisonNonApp` fournie mais statut ≠ NON_APPLICABLE | 400 | 400 |
| Aucune analyse DONE sur le dossier | endpoint GET retourne 200 + `[]` | 200 |
| `pieces_alignment_json` null (legacy ou matérialisation échouée) | endpoint GET retourne 200 + `[]` | 200 |
| Matérialisation échoue (DB lock, etc.) | log warn, run synthèse enrichie réussit quand même (fail-open) | — |

---

## Critères d'acceptation

- [ ] **CA-01 PUT statut pur** : `PUT /pieces-manquantes/status` upsert dans `piece_manquante_status` SANS aucune mutation `pieces_manquantes` JSON ni `case_deadlines`
- [ ] **CA-02 PUT idempotent** : 2 PUT successifs avec mêmes clés → 1 seule entrée mise à jour (UNIQUE constraint), pas de doublon
- [ ] **CA-03 matérialisation au run** : après `EnrichedAnalysisService.run` avec ≥ 1 pièce statut À_DEMANDER, la nouvelle `CaseAnalysis` porte `pieces_alignment_json` non vide + entrée idempotente `case_deadlines` source `PIECE_A_DEMANDER`
- [ ] **CA-04 endpoint lecture pure** : `GET /pieces-manquantes-alignment` retourne 200 + JSON sur la dernière `CaseAnalysis` DONE
- [ ] **CA-05 prompt enrichi** : sections `[Pièces déjà obtenues]`, `[Pièces non applicables]`, `[Pièces à demander]` injectées dans le prompt enrichi quand pieces avec statuts existent
- [ ] **CA-06 tile dashboard** : tile `F-194-pieces-summary` thème DOCUMENTS apparaît, primary/secondary/alertLevel corrects
- [ ] **CA-07 délais idempotents** : 2 runs successifs avec mêmes pieces À_DEMANDER → 1 seule entrée `case_deadlines`
- [ ] **CA-08 isolation workspace** : autre workspace → 404 (PUT et GET)
- [ ] **CA-09 fail-open matérialisation** : exception → run réussit + log warn
- [ ] **CA-10 cohérence F-92 strict** : le JSON `pieces_manquantes` n'est PAS modifié par F-194 (test de régression)
- [ ] **CA-11 jointure libellé normalisé** : pièce JSON "Contrat de travail original" et `piece_manquante_status.piece_libelle_normalise = "contrat de travail original"` → statut joint correctement
- [ ] **CA-12 régénération JSON pieces_manquantes** : si l'IA régénère la liste pieces_manquantes (mots différents pour pièce similaire), le statut avocat ne se joint pas — V1 accepte cette limitation, traité hors scope V1 (cf. ci-dessous)

---

## Périmètre

### Hors scope V1

- (a) Matching fuzzy entre libellés différents (ex. « Contrat de travail » vs « Contrat de travail signé » → statut OBTENUE perdu si libellé évolue) — V1 = match exact normalisé. Si signal terrain confirme le besoin, ajouter normalisation supplémentaire en V2 (Levenshtein, embedding match)
- (b) Suppression auto de la pièce du JSON pieces_manquantes quand statut = OBTENUE — V1 = pas de modif du JSON, le frontend filtre à l'affichage
- (c) Notification email proactive aux destinataires (« relance client »)
- (d) Workflow de signature (la pièce demande une réponse formelle du client)
- (e) Date butoir personnalisable par l'avocat (V1 = today + 14j fixe)

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| PUT | `/api/v1/case-files/{id}/pieces-manquantes/status` | Oui | MEMBER |
| GET | `/api/v1/case-files/{id}/pieces-manquantes-alignment` | Oui | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `piece_manquante_status` | CREATE TABLE | Nouvelle table — schéma cf. ci-dessus |
| `case_analyses` | ALTER + UPDATE | Nouvelle colonne `pieces_alignment_json TEXT NULL` |
| `case_deadlines` | INSERT (idempotent) | Nouvelle source enum `PIECE_A_DEMANDER` |

### Migration Liquibase

- `XXX-f194-pieces-markables.xml` :
  - CREATE TABLE `piece_manquante_status` avec UNIQUE constraint
  - ADD COLUMN `case_analyses.pieces_alignment_json TEXT NULL`
  - Élargir contrainte `case_deadlines.source` pour `PIECE_A_DEMANDER`

### Composants Spring Boot

- `PieceManquanteStatus` (entité JPA), `PieceManquanteStatusRepository`
- `PieceManquanteStatusService` — CRUD upsert + `collectForEnrichment(caseFileId)` (lecture statuts pour prompt enrichi)
- `PieceManquanteAlignmentService` — `materializeForAnalysis(newAnalysis)` + `getForLatestAnalysis(caseFileId)`
- `PieceManquanteController` (nouveau) — endpoint PUT + GET
- `PieceManquanteAlignmentResponse` + `PieceManquanteAlignment` (records DTO avec libelle, statut, toolIdsCibles, destinataire, raisonNonApp)
- `CaseFileDashboardService.assembleTiles` (étendu) — tile `F-194-pieces-summary` thème DOCUMENTS
- `EnrichedAnalysisService` (étendu) :
  - `buildEnrichedPrompt` : ajouter sections `[Pièces déjà obtenues]`, `[Pièces non applicables]`, `[Pièces à demander]`
  - `run` : appel `PieceManquanteAlignmentService.materializeForAnalysis(newAnalysis)` à la fin (APRÈS F-192 et F-193 hooks)

---

## Plan de test

### Tests unitaires (~12-14 UT)

- `PieceManquanteStatusServiceTest` — upsert idempotent, normalisation libellé, validation statut/raison
- `PieceManquanteAlignmentServiceTest` — `materializeForAnalysis` join correct, propagation délais idempotente, fail-open
- `CaseFileDashboardServiceTest` extension — tile correct selon mix statuts
- `EnrichedAnalysisServiceTest` — sections prompt enrichi correctes (3 sections selon statuts)

### Tests d'intégration (~10 IT)

- `PieceManquanteControllerIT` — PUT/GET 200 / autres workspaces 404 / unauthenticated 401 / statut invalide 400
- `EnrichedAnalysisServiceIT` — run avec pièces statuts → matérialisation correcte + propagation délais ; 2 runs → pas de doublons délais ; exception matérialisation → run réussit
- `PieceManquanteStatusServiceIT` — PUT statut ne déclenche AUCUNE mutation `pieces_manquantes` JSON (régression cohérence F-92 strict)

### Isolation workspace

- [x] IT explicite : workspace A ne peut pas lire/modifier les statuts pièces de workspace B

---

## Dépendances

- F-92 ✅ Terminée (pieces_manquantes JSON existant)
- F-192 SF-192-01 ✅ Terminée (pattern + précédent enum value)
- F-193 SF-193-01 (en cours dev) — pattern + valeurs enum partagées sur `case_deadlines.source`
- F-167 ✅ Terminée

---

## Impact par domaine métier

V1 = transversal 3 domaines × 2 pays. Les pièces typiques diffèrent (Travail : contrat / fiches de paie / lettre licenciement ; Immigration : titre de séjour / acte mariage / justificatif domicile / convention d'accueil ; Famille : acte mariage / acte naissance / livret famille / rapport d'enquête sociale ; FR + BE équivalents) mais le mécanisme markable + matérialisation est strictement uniforme.

---

## Notes et décisions

- **Décision 2026-05-06** : Option A retenue (nouvelle table `piece_manquante_status` overlay) plutôt qu'Option B (refactor pieces_manquantes en table) — éviter de casser F-92 + F-IA-03 + l'export PDF synthèse F-95.
- **Décision 2026-05-06** : V1 délai butoir fixe à today + 14j. Configurable plus tard si retour terrain.
- **Décision 2026-05-06** : V1 match exact libellé normalisé. Pas de fuzzy matching (Levenshtein, embedding) — V2 si retour terrain montre des pertes de statut significatives.
- **Décision 2026-05-06** : Pas de rollback automatique des délais quand statut repasse de À_DEMANDER → OBTENUE (cohérent F-192/F-193).
- **Décision 2026-05-06** : Tile dashboard thème DOCUMENTS (différent de F-192 DIAGNOSTIC, F-193 DELAIS) — les pièces relèvent naturellement du thème DOCUMENTS.
- **Décision 2026-05-06** : prompt enrichi étendu avec 3 sections (vs 2 pour F-176 RETAINED/DISCARDED) — cohérent avec la trichotomie statut.
