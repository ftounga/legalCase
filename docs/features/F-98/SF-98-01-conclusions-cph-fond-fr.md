# Mini-spec — F-98 / SF-98-01 — Génération du projet de conclusions CPH bureau de jugement (fond), FR, demandeur, droit du travail

> Template : `project-governance/templates/subfeature-template.md`
> Validée AVANT dev. Cadrages amont : `SF-98-00-coherence.md` (étape 0, GO avec ajustements) + `SF-98-00b-ux-coherence.md` (étape 0 bis, GO avec ajustements).

---

## Identifiant

`F-98 / SF-98-01`

## Feature parente

`F-98` — Génération de courrier / conclusions

## Statut

`ready`

## Date de création

2026-05-18

## Branches Git (dev parallélisé back / front)

- `feat/SF-98-01-backend-conclusions`
- `feat/SF-98-01-frontend-conclusions`

> Parallélisation autorisée : contrat API figé ci-dessous (section Technique), branches isolées (cf. `ai-skills/parallel-frontback-delivery.md`).

---

## Objectif

Permettre à l'avocat de générer, depuis un dossier de droit du travail (France) déjà analysé, un **projet de conclusions** pour le Conseil de prud'hommes en bureau de jugement (fond), côté demandeur (salarié), en consolidant la synthèse, les pièces, les outils décisionnels et les pistes stratégiques.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre l'onglet **Décision** du détail dossier → section **Conclusions** (`app-conclusions-section`), en bas, après le tableau de bord décisionnel.
2. La section appelle `GET /api/v1/case-files/{id}/conclusions` au montage et affiche l'état :
   - `NOT_GENERATED` + pré-requis OK → bandeau d'explication des intrants + bouton **« Générer le projet de conclusions »**.
   - `NOT_GENERATED` + pré-requis manquant → message guidant (cf. cas d'erreur), bouton désactivé.
   - `PENDING` / `PROCESSING` → indicateur « Génération en cours… ».
   - `DONE` → bandeau de transparence + texte des conclusions + bouton **Copier** + bouton **Régénérer**.
   - `FAILED` → message d'échec + bouton **Réessayer**.
3. Clic « Générer » → `POST /api/v1/case-files/{id}/conclusions/generate` → `202`. Le frontend démarre un **polling** `GET …/conclusions` toutes les 3 s.
4. Backend (worker asynchrone RabbitMQ) : `CaseConclusionService` assemble le prompt via `CaseConclusionPromptBuilder` (stade procédural + synthèse + pièces + verdicts des outils décisionnels + pistes stratégiques retenues), appelle `AnthropicService.analyzeWithSystemCache()` (modèle Sonnet, `maxTokens = 8000`), stocke le texte généré, passe le statut à `DONE`.
5. Le polling détecte `DONE` → affiche le texte + bandeau de transparence. Le polling s'arrête sur `DONE` ou `FAILED`.
6. « Régénérer » rejoue l'étape 3 ; la nouvelle génération **écrase** la précédente (1 conclusion par dossier).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Stade procédural non renseigné (juridiction, stade ou position `null`) | Erreur `STAGE_NOT_SET` — message « Renseignez le stade procédural du dossier (onglet Dossier) avant de générer les conclusions. » | 409 |
| Aucune analyse terminée (pas de `CaseAnalysis` au statut `DONE`) | Erreur `ANALYSIS_NOT_READY` — message « Lancez et terminez l'analyse du dossier avant de générer les conclusions. » | 409 |
| Combinaison non couverte par la V1 (domaine ≠ `DROIT_DU_TRAVAIL`, pays ≠ `FRANCE`, juridiction ≠ `CPH`, stade ≠ `FOND`, ou position ≠ `DEMANDEUR`) | Erreur `COMBINATION_NOT_SUPPORTED` — message « La génération de conclusions est disponible pour l'instant uniquement pour : Conseil de prud'hommes, bureau de jugement, côté demandeur, droit du travail (France). » | 409 |
| Génération déjà en cours (`PENDING` ou `PROCESSING`) | Erreur `ALREADY_GENERATING` — message « Une génération est déjà en cours pour ce dossier. » | 409 |
| Dossier inexistant ou appartenant à un autre workspace | Accès refusé (pas de fuite d'existence) | 404 |
| Utilisateur non authentifié | Rejet | 401 |
| Échec de l'appel IA (exception, timeout, `stop_reason` anormal) | Statut `FAILED` + `errorMessage` renseigné ; l'avocat peut relancer | 202 au déclenchement, `FAILED` lu au polling |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : la section conclusions **n'est pas un outil décisionnel** (pas de calculator/analyzer, pas d'entrée `TOOL_REGISTRY`, pas de panel F-IA-04). C'est un **générateur de document**. Le scan des outils décisionnels ne s'applique pas — voir section « Conformité F-IA-04 ».
- [x] **Autres pays** : Belgique — couverte par la matrice F-98 (SF-98-11 à SF-98-17, droit du travail BE). Hors V1.
- [x] **Autres domaines** : immigration (SF-98-18→29) et famille (SF-98-30→45) — couverts par la matrice F-98. Hors V1.
- [x] **Autres UI patterns** : la section consomme un endpoint asynchrone — réutilise le pattern de polling existant de `case-file-detail` (jobs d'analyse), pas de nouveau pattern UI.
- [x] **Autres flows transversaux** : aucun. La SF lit des entités existantes (synthèse, pièces, outils, pistes) sans modifier auth / workspace / plans / navigation.

### Cas spécifique : nouvelle feature d'outil décisionnel

Non applicable — la section conclusions est un générateur de document, pas un outil décisionnel. Aucun champ saisi, aucun verdict, aucune intégration `TOOL_REGISTRY` / panel F-IA-04 / `CaseDashboardRefreshService`.

### Cas spécifique : nouveau pattern UI ou service partagé

- `ConclusionsSectionComponent` et `ConclusionsService` (frontend), `CaseConclusionPromptBuilder` (backend) sont **spécifiques à F-98** — pas de composant `shared/` ni de service `core/` réutilisable hors F-98.
- `CaseConclusionPromptBuilder` est conçu pour être **étendu** par les SF-98-02→45 (chaque cellule de la matrice = un prompt système distinct, mêmes intrants). C'est une dette de convergence assumée et tracée : la matrice F-98 est la feuille de route.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Conclusions autres juridictions/stades/positions FR travail | Oui | Backlog — SF-98-02 à SF-98-10 (matrice F-98) |
| Conclusions droit du travail BE | Oui | Backlog — SF-98-11 à SF-98-17 |
| Conclusions immigration / famille (FR + BE) | Oui | Backlog — SF-98-18 à SF-98-45 |
| Style learning | Oui | Backlog — SF-98-46 à SF-98-48 |
| Éditeur / export / versions / bandeau régénération | Oui | Backlog — SF-98-49 à SF-98-53 |
| Intégration outils décisionnels (TOOL_REGISTRY, F-IA-04) | Non | La section conclusions n'est pas un outil décisionnel |

### Décision

- [x] Subfeature(s) restante(s) déjà tracée(s) au backlog : la matrice exhaustive des 53 SF figure dans `SF-98-00-coherence.md` et dans la ligne F-98 de `PRODUCT_SPEC.md`. SF-98-01 livre **une seule cellule** ; les 52 autres sont explicitement gated « par vagues selon signal terrain ». Réduction de périmètre **explicite et tracée**, pas silencieuse.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : `ConclusionsSectionComponent` est un **générateur de document**, pas une section décisionnelle. Elle ne consomme pas d'endpoint décisionnel POST/GET produisant un verdict, n'est pas enregistrée dans `TOOL_REGISTRY`, n'apparaît pas dans le panel F-IA-04, et ne participe ni au pré-fill IA des outils, ni à la validation de cohérence F-IA-03, ni au refresh du tableau de bord. Les 5 blocs F-IA-04 (cohérence visuelle décisionnelle, pré-fill, F-IA-03, TOOL_REGISTRY, parité domaines niveau ≥ 5) ne s'appliquent pas. Le composant respecte néanmoins le `docs/DESIGN_SYSTEM.md` (palette navy/or, `MatSnackBar` pour les erreurs, espacements multiples de 4px).

---

## Critères d'acceptation

- [ ] **CA1** — `POST …/conclusions/generate` sur un dossier travail FR, stade CPH/FOND/DEMANDEUR, avec une analyse `DONE` → `202`, une ligne `case_conclusions` est créée au statut `PENDING`.
- [ ] **CA2** — Le worker asynchrone assemble un prompt contenant le stade procédural, les faits/points juridiques/risques de la synthèse, les pièces numérotées, les verdicts des outils décisionnels remplis et les pistes stratégiques retenues, appelle l'IA, et passe le statut à `DONE` avec `content` non vide.
- [ ] **CA3** — Le projet généré est structuré en sections de conclusions prud'homales : en-tête (POUR / CONTRE), FAITS ET PROCÉDURE, DISCUSSION (moyens en droit), PAR CES MOTIFS (dispositif chiffré).
- [ ] **CA4** — `GET …/conclusions` reflète l'état courant : `NOT_GENERATED` si aucune ligne, sinon `PENDING`/`PROCESSING`/`DONE`/`FAILED` avec le `content` quand `DONE`.
- [ ] **CA5** — `POST …/generate` → `409 STAGE_NOT_SET` si le stade procédural est incomplet.
- [ ] **CA6** — `POST …/generate` → `409 ANALYSIS_NOT_READY` si aucune `CaseAnalysis` n'est au statut `DONE`.
- [ ] **CA7** — `POST …/generate` → `409 COMBINATION_NOT_SUPPORTED` si la combinaison stade/domaine/pays sort de CPH/FOND/DEMANDEUR/DROIT_DU_TRAVAIL/FRANCE.
- [ ] **CA8** — `POST …/generate` → `409 ALREADY_GENERATING` si une génération est déjà `PENDING`/`PROCESSING`.
- [ ] **CA9** — Sécurité : un utilisateur du workspace A reçoit `404` sur `POST` et `GET` pour un dossier du workspace B.
- [ ] **CA10** — En cas d'échec de l'appel IA, le statut passe à `FAILED`, `errorMessage` est renseigné, et une relance est possible.
- [ ] **CA11** — Frontend : la section affiche le bandeau de transparence « Projet généré automatiquement — relecture par l'avocat obligatoire avant tout dépôt » au-dessus du texte dès que le statut est `DONE`.
- [ ] **CA12** — Frontend : quand un pré-requis manque, la section affiche le message guidant correspondant et le bouton « Générer » est désactivé ; le bouton « Copier » place le texte des conclusions dans le presse-papier.

---

## Périmètre

### Hors scope (explicite)

- Les 52 autres cellules de la matrice F-98 (autres juridictions, stades, positions, domaines, pays) — SF-98-02 à SF-98-45.
- **Style learning** (apprentissage du style rédactionnel de l'avocat) — SF-98-46 à SF-98-48.
- **Éditeur riche de relecture** — SF-98-49. V1 = affichage en lecture seule + copier ; pas d'édition du texte persistée.
- **Export Word / PDF** — SF-98-50 / SF-98-51. V1 = action « Copier » dans le presse-papier.
- **Versions multiples** (brouillon / validé / déposé + historique) — SF-98-52. V1 = une conclusion par dossier ; régénérer écrase.
- **Bandeau « conclusions à régénérer »** sur changement d'un input amont — SF-98-53. V1 = régénération manuelle uniquement.
- **Volet courrier** (mises en demeure, lettres) — cadrage distinct ultérieur.
- **Citation jurisprudentielle enrichie** (F-241 / F-242).
- Aucun nouveau gate de plan : la génération suit le même mode de consommation IA que les analyses ; les tokens consommés sont enregistrés sur la ligne `case_conclusions` pour observabilité. Un éventuel quota dédié relève d'une SF future.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `status` | `PENDING` | Toujours à la création de la ligne, lors du déclenchement |
| `content` | `null` | Renseigné par le worker au passage `DONE` |
| `error_message` | `null` | Renseigné uniquement au passage `FAILED` |
| `generated_at` | `null` | Renseigné par le worker au passage `DONE` |
| `model_used`, `prompt_tokens`, `completion_tokens` | `null` | Renseignés par le worker au passage `DONE` |
| `jurisdiction_code`, `stage_code`, `position_code` | snapshot du stade du dossier au déclenchement | Figés à la création |

Comportements à la création :
- `created_at` / `updated_at` renseignés automatiquement.
- `workspace_id` = workspace du contexte de sécurité (isolation).
- 1 ligne `case_conclusions` par dossier (`case_file_id` UNIQUE) : régénérer réutilise la ligne (UPDATE), ne crée pas de doublon.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `case_file_id` | Oui | — | UUID, FK `case_files` | Oui (1:1) | — |
| `workspace_id` | Oui | — | UUID, FK `workspaces` | Non | — |
| `status` | Oui | 20 | `PENDING`, `PROCESSING`, `DONE`, `FAILED` | Non | — |
| `content` | Non | TEXT (illimité) | texte généré par l'IA | Non | — |
| `jurisdiction_code` / `stage_code` / `position_code` | Oui | 50 | codes `ProcedureStageCatalog` | Non | — |
| `error_message` | Non | TEXT | texte libre | Non | — |

Notes :
- `status = NOT_GENERATED` est une **valeur de réponse synthétique** (DTO) quand aucune ligne n'existe — elle n'est jamais persistée.
- Le `content` n'est pas plafonné en base ; le `maxTokens = 8000` de l'appel IA borne sa taille en pratique.

---

## Technique

### Endpoints (contrat API FIGÉ pour parallélisation back/front)

| Méthode | URL | Auth | Rôle min. | Réponses |
|---------|-----|------|-----------|----------|
| POST | `/api/v1/case-files/{caseFileId}/conclusions/generate` | Oui | LAWYER | `202 {"status":"PENDING"}` ; `409 {"error":"STAGE_NOT_SET\|ANALYSIS_NOT_READY\|COMBINATION_NOT_SUPPORTED\|ALREADY_GENERATING","message":"<texte avocat>"}` ; `404` ; `401` |
| GET | `/api/v1/case-files/{caseFileId}/conclusions` | Oui | MEMBER | `200 ConclusionResponse` ; `404` (dossier inexistant / autre workspace) ; `401` |

**`ConclusionResponse`** (corps du `GET 200`) :

```json
{
  "id": "uuid | null",
  "caseFileId": "uuid",
  "status": "NOT_GENERATED | PENDING | PROCESSING | DONE | FAILED",
  "content": "string | null",
  "jurisdictionLabel": "string | null",
  "stageLabel": "string | null",
  "positionLabel": "string | null",
  "modelUsed": "string | null",
  "generatedAt": "ISO-8601 | null",
  "errorMessage": "string | null",
  "createdAt": "ISO-8601 | null",
  "updatedAt": "ISO-8601 | null"
}
```

- `status = NOT_GENERATED` ⇒ tous les champs sauf `caseFileId` et `status` valent `null`. Le frontend interprète `NOT_GENERATED` comme « jamais généré » et affiche le bouton « Générer ».
- Les `*Label` sont les libellés humains résolus depuis `ProcedureStageCatalog` (ex. « Conseil de prud'hommes », « Bureau de jugement (fond) », « Demandeur (salarié) »).

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_conclusions` | CREATE (nouvelle table) + INSERT / SELECT / UPDATE | 1:1 avec `case_files` |
| `case_files` | SELECT | lecture du domaine + stade procédural |
| `case_analyses` | SELECT | lecture de la synthèse `DONE` la plus récente |
| `document_pieces` | SELECT | lecture des pièces numérotées |
| `strategic_options` | SELECT | lecture des pistes stratégiques |
| outils décisionnels (`*_analyses`) | SELECT | lecture des verdicts des outils remplis |

### Migration Liquibase

- [x] Oui — `233-create-case-conclusions.xml` (numéro 233 : 232 est réservé par F-240 SF-240-01 en cours sur une autre branche — collision évitée).

### Composants Backend (branche `feat/SF-98-01-backend-conclusions`)

- `CaseConclusion` (entité, table `case_conclusions`)
- `CaseConclusionStatus` (enum `PENDING/PROCESSING/DONE/FAILED`)
- `CaseConclusionRepository` (`findByCaseFileId`)
- `CaseConclusionController` (POST generate, GET)
- `CaseConclusionCommandService` (`@Transactional` — validation des gardes, création de la ligne `PENDING`, envoi du message RabbitMQ)
- `CaseConclusionService` (`@RabbitListener` — worker : assemblage prompt + appel IA + persistance résultat + statut)
- `CaseConclusionPromptBuilder` (assemblage du prompt système + message utilisateur depuis les données du dossier)
- `CaseConclusionMessage` (record RabbitMQ : `caseConclusionId`)
- `ConclusionResponse`, `ConclusionGenerationResponse` (DTO)
- `CaseConclusionException` + gestion `409` dans le `@RestControllerAdvice` existant ou le controller
- RabbitMQ : queue `case.conclusion`, exchange + routing key dédiés

### Composants Angular (branche `feat/SF-98-01-frontend-conclusions`)

- `ConclusionsSectionComponent` (`app-conclusions-section`) — standalone, `ChangeDetectionStrategy.OnPush`, `inject(ChangeDetectorRef)` + `markForCheck()` dans chaque `next:`/`error:`
- `ConclusionsService` — `GET`/`POST` HTTP
- Intégration dans `case-file-detail.component.html`, onglet Décision (`TAB_DECISION = 2`), après `app-case-dashboard` — `@Input() caseFileId`, `procedureStageComplete`, `hasCompletedAnalysis`
- `conclusion.model.ts` — types TS du `ConclusionResponse`

---

## Plan de test

### Tests unitaires (backend)

- [ ] `CaseConclusionCommandServiceTest` — déclenchement nominal → ligne `PENDING` + message publié.
- [ ] `CaseConclusionCommandServiceTest` — `STAGE_NOT_SET` si stade incomplet.
- [ ] `CaseConclusionCommandServiceTest` — `ANALYSIS_NOT_READY` si pas d'analyse `DONE`.
- [ ] `CaseConclusionCommandServiceTest` — `COMBINATION_NOT_SUPPORTED` hors CPH/FOND/DEMANDEUR/travail/FR.
- [ ] `CaseConclusionCommandServiceTest` — `ALREADY_GENERATING` si génération en cours.
- [ ] `CaseConclusionServiceTest` — worker : succès → statut `DONE`, `content`/`generatedAt`/tokens renseignés.
- [ ] `CaseConclusionServiceTest` — worker : exception IA → statut `FAILED`, `errorMessage` renseigné.
- [ ] `CaseConclusionPromptBuilderTest` — le prompt contient stade, faits, pièces, verdicts d'outils, pistes retenues.
- [ ] `CaseConclusionPromptBuilderTest` — dossier sans piste / sans outil rempli → prompt valide (sections vides tolérées).

### Tests unitaires (frontend, Jest)

- [ ] `conclusions-section.component.spec.ts` — montage → `GET` déclenché ; `NOT_GENERATED` → bouton « Générer » visible.
- [ ] `conclusions-section.component.spec.ts` — pré-requis manquant → message guidant + bouton désactivé.
- [ ] `conclusions-section.component.spec.ts` — `DONE` → bandeau de transparence + texte affichés ; « Copier » écrit dans le presse-papier.
- [ ] `conclusions-section.component.spec.ts` — clic « Générer » → `POST` puis polling ; `FAILED` → message d'échec.
- [ ] `conclusions.service.spec.ts` — URLs et méthodes HTTP conformes au contrat.

### Tests d'intégration (backend)

- [ ] `CaseConclusionControllerIT` — `POST …/generate` → `202` + ligne créée.
- [ ] `CaseConclusionControllerIT` — `POST …/generate` → `409` pour chacun des 4 codes d'erreur.
- [ ] `CaseConclusionControllerIT` — `GET …/conclusions` → `200 NOT_GENERATED` puis état après déclenchement.
- [ ] `CaseConclusionControllerIT` — `POST` et `GET` → `404` pour un dossier d'un autre workspace.
- [ ] `CaseConclusionControllerIT` — `POST` et `GET` → `401` sans authentification.

### Isolation workspace

- [x] Applicable — `CaseConclusionControllerIT` vérifie qu'un utilisateur du workspace A ne peut ni déclencher ni lire les conclusions d'un dossier du workspace B (`404`). Le `workspace_id` est porté par la table et contrôlé à chaque accès.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature additive et isolée. Elle crée une table, deux endpoints et un composant ; elle **lit** des entités existantes sans modifier les mécanismes d'auth, de résolution de workspace, de plans/quotas ni de navigation. Les endpoints consomment `@AuthenticationPrincipal` selon le pattern standard (aucun nouveau type d'auth). Aucune route Angular ni guard ajouté/modifié (section intégrée à un onglet existant).

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — la SF n'ajoute pas de route, ne modifie ni l'auth, ni le contexte workspace, ni la navigation. `cd e2e && npm test` non requis comme garde-fou de cette SF.

---

## Dépendances

### Subfeatures bloquantes

- `F-243` (stade procédural du dossier) — statut : **done** (PR #975 + #976 mergées 2026-05-15).
- `F-DT-36` (nullité de procédure — pré-requis qualité) — statut : **done** (2/2 SF mergées 2026-05-17).

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

- **Modèle IA** : `AnthropicService.analyzeWithSystemCache()` avec le modèle Sonnet par défaut (`claude-sonnet-4-6`) — la qualité rédactionnelle prime ; le prompt système (instructions de rédaction, stable) bénéficie du cache éphémère lors des régénérations. `maxTokens = 8000`.
- **Asynchrone** : génération via RabbitMQ (queue `case.conclusion`), conformément à la règle « les analyses de dossiers sont asynchrones » (CLAUDE.md). Le frontend suit l'avancement par **polling** `GET …/conclusions` (3 s) — pas de SSE en V1, pour ne pas toucher l'enum `JobType` ni l'infrastructure SSE existante (périmètre réduit, risque transversal évité).
- **Pré-requis IA dans le prompt** : le `CaseConclusionPromptBuilder` reprend les invariants anti-gadget 2-5 du cadrage étape 0 — citation des pièces par numéro, reprise des verdicts des outils décisionnels, reprise des pistes stratégiques retenues, demandes chiffrées issues des calculs (pas de montants réinventés). L'invariant 10 (transparence) est porté côté frontend par le bandeau permanent.
- **1:1 dossier ↔ conclusion** : choix V1 assumé. Le versioning (SF-98-52) introduira la multiplicité ; ici régénérer = `UPDATE` de la ligne.
- **`COMBINATION_NOT_SUPPORTED`** : garde explicite qui matérialise le périmètre V1 (une seule cellule de la matrice). Les SF-98-02+ lèveront la garde cellule par cellule.
