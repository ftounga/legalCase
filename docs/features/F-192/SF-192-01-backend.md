# Mini-spec — F-192 / SF-192-01 Backend — Service d'alignement pistes retenues ↔ outils + propagation pieces/délais + tile dashboard

## Identifiant

`F-192 / SF-192-01`

## Feature parente

`F-192` — Propagation des pistes stratégiques retenues vers outils décisionnels + dashboard + autres blocs synthèse

## Statut

`draft`

## Date de création

2026-05-06

## Branche Git

`feat/SF-192-01-backend-retained-pistes-alignment`

---

## Objectif

Exposer côté backend l'alignement entre les pistes stratégiques 🟢 Retenue et les sorties des outils décisionnels du dossier, **calculé et matérialisé au moment du run de Synthèse enrichie** (cohérent avec le pattern F-176 actuel : tag piste = persistance pure, effets matérialisés à la prochaine synthèse enrichie).

---

## Modèle d'activation — gating Synthèse enrichie

Le modèle F-176 actuel est strict : `StrategicOptionService.updateStatus` reste un PUT pur sans side-effect. Tag d'une piste 🟢 Retenue = simple persistance, **rien d'autre ne se passe**. Les effets prévus par F-192 (alignement, propagation pieces/délais, tile dashboard, badge card, sortie outil enrichie, section PDF) ne se déclenchent **qu'au prochain run de Synthèse enrichie** (`EnrichedAnalysisService.run`).

Avant le clic « Synthèse enrichie », l'avocat voit ses tags posés sur les pistes existantes — strictement le comportement F-176 actuel. Aucun signal nouveau côté tile / badge / sortie outil. Le clic « Synthèse enrichie » devient **le moment unique de commit** où :

1. L'IA développe les RETAINED dans la nouvelle analyse (existant F-176)
2. L'alignement est calculé et persisté contre la nouvelle `CaseAnalysis` (nouveau)
3. Les conditions des RETAINED s'ajoutent à `pieces_manquantes` de la nouvelle analyse (nouveau)
4. Les horizons des RETAINED matérialisent des entrées dans `case_deadlines` (nouveau)
5. Le dashboard tile, le badge card panel, la sortie outils, la section PDF — tout ça **lit l'état de la dernière `CaseAnalysis` DONE**, donc se rafraîchit en cascade après le run

---

## Comportement attendu

### Cas nominal

1. L'avocat tag des pistes en statut `RETAINED` via `PUT /api/v1/case-files/{id}/strategic-options/{optionId}` (endpoint F-176 existant — **strictement inchangé, aucun side-effect ajouté**).
2. L'avocat clique « Synthèse enrichie ». `EnrichedAnalysisService.run(caseFileId)` exécute son flow existant (collect RETAINED + DISCARDED, injecter dans le prompt, appel IA, persister `CaseAnalysis`, propager RETAINED+DISCARDED via `StrategicOptionService.propagateRetainedAndDiscarded`). À la **fin** du run, après commit de la nouvelle analyse, `RetainedPisteAlignmentService.materializeForAnalysis(newAnalysis)` :
   - Calcule l'alignement de chaque piste `RETAINED` clonée sur la nouvelle analyse contre les outputs des outils (cf. stratégie matching ci-dessous), persiste sur la `CaseAnalysis` (champ `retained_pistes_alignment_json` ajouté en migration)
   - Insère dans `pieces_manquantes` de la nouvelle analyse les conditions des pistes RETAINED non déjà présentes (source = `PISTE_RETENUE`)
   - Insère dans `case_deadlines` du dossier les horizons parsables des pistes RETAINED (source = `PISTE_RETENUE`)
3. Le frontend lit l'état via deux endpoints :
   - `GET /api/v1/case-files/{id}/retained-pistes-alignment` retourne l'alignement persisté sur la dernière `CaseAnalysis` DONE — pure lecture, aucun calcul à la volée
   - `GET /api/v1/case-files/{id}/dashboard` (existant) renvoie désormais la tile `RETAINED_PISTES_SUMMARY` agrégée à partir du même alignement persisté
4. `CaseFileDashboardService.assembleTiles(caseFileId)` ajoute la tile `RETAINED_PISTES_SUMMARY` dans le thème `DIAGNOSTIC` quand la dernière `CaseAnalysis` DONE a au moins une piste `RETAINED` clonée :
   - `primaryValue` = nombre total de pistes retenues sur la dernière analyse
   - `secondaryValue` = nombre de pistes en `DIVERGENT`
   - `alertLevel` dérivé : `ALERT` si ≥ 1 `DIVERGENT`, `WARNING` si 0 `DIVERGENT` mais ≥ 1 `NOT_ANALYZED`, `OK` sinon
5. La matérialisation est **fail-open** : si le matching ou la propagation échoue, la nouvelle analyse n'est pas annulée, log warn, l'alignement persisté reste vide, les pieces/délais non créés, mais la synthèse enrichie est exploitable.

### Définition matchStatus (calculé à la matérialisation)

- `ALIGNED` : la piste apparaît dans le top-3 des recommandations de l'outil cible (lecture des `*Analysis` du dossier au moment du run)
- `DIVERGENT` : l'outil a tourné sur le dossier mais la piste n'est pas dans son top-3
- `NOT_ANALYZED` : l'outil cible n'a pas encore tourné sur le dossier (pas de `*Analysis` correspondante)
- `NO_TARGET_TOOL` : aucun mapping tool trouvé pour la piste (ni baseJuridique ni keyword reconnaissable)

### Stratégie de matching piste → outil

Le matching `piste.texte + piste.baseJuridique → toolId` se fait via une table de mapping statique en code (pas en DB pour V1) :

```
RetainedPisteToolMatcher (Java)
  - Méthode resolveToolId(piste) renvoie String|null
  - Stratégie ordre :
    1. Match exact sur baseJuridique (ex. "Art. L.421-14 CESEDA" → F-IM-05)
    2. Match keyword sur baseJuridique (ex. "CESEDA" → F-IM-05 par défaut, "L.512-1 CESEDA" → F-IM-06)
    3. Match keyword sur texte (ex. "passeport talent", "VPF", "carte de résident" → F-IM-05 ; "RAPO", "recours hiérarchique", "REP" → F-IM-06)
    4. Sinon null (la piste n'a pas d'outil cible identifiable — cas NO_TARGET_TOOL)
```

Le mapping V1 couvre les outils Immigration FR/BE TITRE DE SÉJOUR RECOMMANDÉ (`F-IM-05`) + RECOURS IMMIGRATION (`F-IM-06`). Famille et Travail traités V2 si signal terrain (mini-spec V2 distincte). Les outils déductifs purs (DROIT AU TRAVAIL `F-IM-07`) ne reçoivent pas de mapping — pas de liste à comparer.

### Stratégie de match dans la sortie outil

Pour `matchStatus = ALIGNED / DIVERGENT`, le service interroge la dernière analyse de l'outil cible (table `*_analysis` correspondante via `*AnalysisRepository`) :
- `F-IM-05` → `ImmigrationTitleDecisionAnalysis.recommendedTitles` (top-3)
- `F-IM-06` → `ImmigrationRecoursAnalysis.recommendedRecours` (top-3)

Match piste → recommandation :
- Comparaison sur `baseJuridique` extraite de la piste (regex `(L\.\d+(-\d+)*\s+CESEDA|art\.\s*\d+|RAPO|REP)` etc.) confrontée à l'attribut équivalent de la recommandation
- Fallback : match keyword sur le label de la recommandation

Si l'outil cible n'a pas tourné (`*AnalysisRepository.findByCaseFileId` empty) → `matchStatus = NOT_ANALYZED`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Case file inexistant | 404 "Case file not found" | 404 |
| Case file dans un autre workspace | 404 "Case file not found" (camouflage) | 404 |
| Aucune analyse DONE sur le dossier | 200 avec `[]` (rien à lire) | 200 |
| Dernière analyse DONE existe mais sans pistes RETAINED | 200 avec `[]` | 200 |
| Dernière analyse DONE existe sans alignement persisté (pré-F-192 ou run échoué) | 200 avec `[]` (cas legacy — rien à afficher tant qu'une nouvelle synthèse enrichie n'a pas tourné) | 200 |
| Matching tool échoue sur une piste donnée (à la matérialisation) | piste persistée avec `toolIdCible = null` et `matchStatus = NO_TARGET_TOOL` (fail-open) | — |
| Lecture `*Analysis` cible jette exception (à la matérialisation) | piste persistée avec `matchStatus = NOT_ANALYZED` (fail-open) | — |
| Propagation pieces/délais échoue (à la matérialisation) | log warn, ne bloque pas le run synthèse enrichie (fail-open), nouvelle analyse persistée quand même | — |
| `EnrichedAnalysisService.run` échoue avant la matérialisation | aucun side-effect F-192 ne se déclenche (cohérence transactionnelle avec F-176 actuel) | — |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : F-IM-05 (TITRE DE SÉJOUR RECOMMANDÉ) + F-IM-06 (RECOURS IMMIGRATION) couverts V1. Famille (F-FA-05/06/07) et Travail (F-DT-09 indemnités, F-DT-08 validité licenciement, F-DT-31 transaction) **non couverts V1** — les pistes Famille/Travail seront traitées V2 si signal terrain. F-IM-07 DROIT AU TRAVAIL **non concerné** (déductif pur, pas de liste).
- [x] **Autres pays** : Immigration FR + BE déjà couverts par le matching baseJuridique (CESEDA / loi 15/12/1980). Le mapping `RetainedPisteToolMatcher` doit reconnaître les 2 référentiels juridiques.
- [x] **Autres domaines** : V1 = Immigration uniquement. V2 = Famille (DIVORCE faute/amiable, PARTAGE judiciaire/amiable, GARDE alternée/exclusive) + Travail (INDEMNITES Macron vs négociées, TRANSACTION vs prud'hommes) selon signal terrain.
- [x] **Autres flows transversaux** : `CaseFileDashboardService` étendu avec une nouvelle tile (pattern miroir des 85 mappers F-167). Pieces_manquantes utilise un nouveau champ `source` enum value `PISTE_RETENUE`. Délais utilise un nouveau champ `source` enum value `PISTE_RETENUE`. Aucun impact auth / workspace context / plans / navigation.

### Niveaux de vérification couverts

- [x] **Modèle TypeScript / API exposée** : nouveau DTO `RetainedPisteAlignmentResponse` côté backend, miroir TypeScript en SF-192-02
- [x] **Record / DTO backend** : nouveau record `RetainedPisteAlignment` (pisteId, texte, baseJuridique, horizonTemporel, conditions, toolIdCible, matchStatus)
- [x] **Service / logique métier** : nouveau `RetainedPisteAlignmentService` + nouveau `RetainedPisteToolMatcher` + extension `CaseFileDashboardService.assembleTiles`
- [x] **Entité JPA + schéma DB** : aucune nouvelle table, aucune nouvelle colonne. Réutilise `strategic_options` (F-176), `pieces_manquantes` (F-92), `case_deadlines` (F-DT-03) avec nouvelle valeur d'enum `source = PISTE_RETENUE`
- [x] **Tests existants** : couverture `StrategicOptionService` F-176 (pattern miroir), `CaseFileDashboardService` F-167 (intégration tile)

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Nouveau service `RetainedPisteAlignmentService`** : peut servir à d'autres features ? Probablement V2 (Famille/Travail). Service générique par construction (matching ne dépend pas du domaine via la stratégie keyword).
- [x] **Nouveau pattern `source = PISTE_RETENUE` dans pieces_manquantes / case_deadlines** : pourra être réutilisé par F-176 V2 ou F-IM-21 si conditions stratégiques propagées en mode auto. Documenter dans javadoc.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-IM-05 TITRE DE SÉJOUR RECOMMANDÉ | Oui | Intégré V1 (matching baseJuridique CESEDA + keyword "passeport talent"/"VPF"/"carte résident") |
| F-IM-06 RECOURS IMMIGRATION | Oui | Intégré V1 (matching keyword "RAPO"/"REP"/"hiérarchique") |
| F-IM-07 DROIT AU TRAVAIL | Non — déductif pur | Non applicable — pas de liste à comparer |
| F-FA-05/06/07 Famille | Oui mais V2 | Backlog F-192 V2 — V1 ne couvre que Immigration pour cadrer le scope |
| F-DT-08/09/31 Travail | Oui mais V2 | Backlog F-192 V2 |
| Pieces_manquantes existante | Oui | Intégré V1 (nouvelle valeur enum `source = PISTE_RETENUE`) |
| Case_deadlines existante | Oui | Intégré V1 (nouvelle valeur enum `source = PISTE_RETENUE`) |
| Dashboard tile F-167 | Oui | Intégré V1 (nouveau mapper `RETAINED_PISTES_SUMMARY` thème DIAGNOSTIC) |
| Belgique | Oui | Intégré V1 (matching loi 15/12/1980 BE en plus du CESEDA FR) |

### Décision

- [x] Étendu à toutes les cibles applicables V1 (Immigration FR+BE)
- [x] Subfeature(s) parallèle(s) créée(s) : SF-192-02 frontend + SF-192-03 export PDF
- [x] Backlog V2 pour Famille/Travail (intégré dans la note F-192 PRODUCT_SPEC.md)

---

## Critères d'acceptation

- [ ] **CA-01 PUT statut sans side-effect** : `PUT /strategic-options/{id} { statut: RETAINED }` reste un PUT pur — aucune entrée créée dans `pieces_manquantes` ni `case_deadlines`, aucune tile dashboard ne change, aucun alignement n'est calculé. Strictement le comportement F-176 actuel.
- [ ] **CA-02 matérialisation au run synthèse enrichie** : après `EnrichedAnalysisService.run(caseFileId)` avec ≥ 1 piste RETAINED, la nouvelle `CaseAnalysis` persistée porte un `retained_pistes_alignment_json` non vide reflétant chaque piste avec `pisteId`, `toolIdCible`, `matchStatus`
- [ ] **CA-03 endpoint lecture pure** : `GET /api/v1/case-files/{id}/retained-pistes-alignment` retourne 200 + JSON tableau lisant directement la dernière `CaseAnalysis` DONE (pas de calcul à la volée). Si pas de matérialisation persistée → 200 + `[]`
- [ ] **CA-04 matching ALIGNED** : pour une piste RETAINED avec `baseJuridique = "Art. L.421-14 CESEDA"`, `toolIdCible = "F-IM-05-arbre-decisionnel-titre"` et `matchStatus = ALIGNED` si le top-3 de `ImmigrationTitleDecisionAnalysis.recommendedTitles` (au moment du run) contient `L.421-14`
- [ ] **CA-05 matching NOT_ANALYZED** : pour une piste RETAINED dont l'outil cible n'a pas tourné (analyse F-IM-05 absente au moment du run), `matchStatus = NOT_ANALYZED`
- [ ] **CA-06 matching NO_TARGET_TOOL** : pour une piste sans baseJuridique parsable, `toolIdCible = null` et `matchStatus = NO_TARGET_TOOL` (fail-open)
- [ ] **CA-07 tile dashboard** : `GET /api/v1/case-files/{id}/dashboard` inclut une tile `RETAINED_PISTES_SUMMARY` thème DIAGNOSTIC quand la dernière `CaseAnalysis` DONE a ≥ 1 RETAINED matérialisée, avec `alertLevel = ALERT` si ≥ 1 DIVERGENT, `WARNING` si 0 DIVERGENT mais ≥ 1 NOT_ANALYZED, sinon `OK`
- [ ] **CA-08 propagation pieces** : après run synthèse enrichie sur dossier avec piste RETAINED `conditions = ["Convention d'accueil INRIA"]`, le bloc `pieces_manquantes` de la nouvelle analyse contient `{ texte: "Convention d'accueil INRIA", source: "PISTE_RETENUE" }` non déjà présent (matching texte normalisé `trim().toLowerCase()`)
- [ ] **CA-09 propagation délais** : après run synthèse enrichie sur piste RETAINED `horizon_temporel = "Court terme (3-6 mois)"`, `case_deadlines` du dossier contient une entrée nouvelle `dateCible = today + 6 mois`, `source = "PISTE_RETENUE"`, `label = "Stratégie : <texte piste>"` (idempotent — clé `(caseFileId, source, label)`)
- [ ] **CA-10 idempotence run multiples** : 2 runs successifs de Synthèse enrichie avec les mêmes pistes RETAINED ne créent pas 2 entrées dans `pieces_manquantes` ou `case_deadlines` — la 2ᵉ matérialisation détecte le doublon
- [ ] **CA-11 erreur 404 isolation workspace** : `GET /case-files/{wrongId}/retained-pistes-alignment` renvoie 404 si dossier dans un autre workspace
- [ ] **CA-12 fail-open matching** : si la lecture d'`ImmigrationTitleDecisionAnalysis` lève exception lors de la matérialisation, la piste est persistée avec `matchStatus = NOT_ANALYZED` (fail-open), le run synthèse enrichie réussit quand même
- [ ] **CA-13 fail-open propagation** : si `propagateToPiecesManquantes` ou `propagateToDeadlines` jette exception (DB lock, etc.), le run synthèse enrichie réussit quand même + log warn, la nouvelle analyse est persistée
- [ ] **CA-14 cohérence F-176 strict** : le hook existant `propagateRetainedAndDiscarded` (clonage RETAINED+DISCARDED sur nouvelle analyse) reste exécuté avant `materializeForAnalysis` — la matérialisation lit les pistes clonées, pas l'analyse précédente

---

## Périmètre

### Hors scope (explicite)

- (a) Matching Famille / Travail (V2 selon signal terrain — V1 = Immigration FR+BE seulement)
- (b) Persistance de l'alignement (calculé à la volée, pas de cache DB)
- (c) Notification email sur divergence avocat ↔ outil
- (d) Mapping baseJuridique → outil paramétrable en DB (V1 = code Java statique, suffit pour le périmètre figé)
- (e) Création automatique de pistes RETAINED depuis une recommandation outil validée (asymétrie volontaire)

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `case_analyses.retained_pistes_alignment_json` | `null` | Nouveau champ (migration) ; renseigné UNIQUEMENT à la matérialisation post-run synthèse enrichie |
| pieces_manquantes.source | `PISTE_RETENUE` | Nouvelle valeur d'enum, valeurs existantes inchangées |
| case_deadlines.source | `PISTE_RETENUE` | Nouvelle valeur d'enum |

Comportements à la matérialisation (post-run synthèse enrichie uniquement) :
- `retained_pistes_alignment_json` rempli avec un tableau `[{pisteId, toolIdCible, matchStatus}]` pour chaque piste RETAINED clonée sur la nouvelle analyse
- Une entrée `pieces_manquantes` source `PISTE_RETENUE` est créée si la condition n'est pas déjà présente sur la nouvelle analyse (texte exact, après `trim().toLowerCase()`)
- Une entrée `case_deadlines` source `PISTE_RETENUE` est créée si `(caseFileId, source, label)` n'existe pas déjà au niveau dossier
- Les analyses pré-F-192 (ou runs F-192 antérieurs où la matérialisation a échoué) ont `retained_pistes_alignment_json = null` → endpoint `/retained-pistes-alignment` retourne `[]`

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| toolIdCible | Non | 64 | match clé `TOOL_REGISTRY` frontend OU null | Non | — |
| matchStatus | Oui | — | `ALIGNED` / `DIVERGENT` / `NOT_ANALYZED` / `NO_TARGET_TOOL` | Non | — |
| pieces_manquantes.source enum | Oui | — | `IA` / `AVOCAT` / `PISTE_RETENUE` (existant + nouveau) | Non | — |
| case_deadlines.source enum | Oui | — | `IA` / `AVOCAT` / `PISTE_RETENUE` (existant + nouveau) | Non | — |

Notes :
- Le matching baseJuridique → toolId est figé en code dans `RetainedPisteToolMatcher`. Toute extension nécessite update Java + tests UT.
- `horizon_temporel` regex de parsing : `/(\d+)\s*(mois|ans?|an)/i`. Si match → `nbMois` calculé, sinon délai non créé (fail-open).

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{id}/retained-pistes-alignment` | Oui | MEMBER (workspace) |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_analyses` | ALTER + UPDATE | Nouvelle colonne `retained_pistes_alignment_json TEXT NULL`, renseignée à la matérialisation |
| `strategic_options` | SELECT | Lecture des pistes RETAINED clonées sur la nouvelle analyse (F-176, inchangée) |
| `pieces_manquantes` | INSERT (idempotent) | Nouvelle source enum `PISTE_RETENUE` |
| `case_deadlines` | INSERT (idempotent) | Nouvelle source enum `PISTE_RETENUE` |
| `immigration_title_decision_analysis` | SELECT | Lecture `recommendedTitles` pour matching ALIGNED/DIVERGENT (au moment du run) |
| `immigration_recours_analysis` | SELECT | Lecture `recommendedRecours` pour matching ALIGNED/DIVERGENT (au moment du run) |

### Migration Liquibase

- [x] Oui — `XXX-f192-retained-pistes-materialization.xml` : (a) ajoute colonne `retained_pistes_alignment_json TEXT NULL` à `case_analyses` ; (b) ajoute la valeur `PISTE_RETENUE` aux enums `pieces_manquantes.source` et `case_deadlines.source` (CHECK constraint élargie OU enum Postgres ALTER TYPE selon implé existante — à vérifier dans la mini-spec finale)

### Composants Spring Boot

- `RetainedPisteAlignmentService` (nouveau) — orchestration matching + propagation, **n'expose qu'une méthode `materializeForAnalysis(CaseAnalysis newAnalysis)` appelée depuis `EnrichedAnalysisService.run`** + une méthode lecture `getForLatestAnalysis(caseFileId)` pour l'endpoint
- `RetainedPisteToolMatcher` (nouveau) — table de mapping statique baseJuridique/keyword → toolId
- `RetainedPisteAlignmentController` (nouveau) — endpoint GET pure lecture
- `RetainedPisteAlignmentResponse` + `RetainedPisteAlignment` (DTO/record nouveaux)
- `CaseFileDashboardService.assembleTiles` (étendu) — ajoute la tile `RETAINED_PISTES_SUMMARY` lue depuis `retained_pistes_alignment_json` de la dernière `CaseAnalysis` DONE
- `StrategicOptionService.updateStatus` — **strictement inchangé**, aucun side-effect ajouté (cohérence F-176)
- `EnrichedAnalysisService.run` (étendu) — appel `RetainedPisteAlignmentService.materializeForAnalysis(newAnalysis)` à la **fin** du run, **après** `propagateRetainedAndDiscarded` (qui clone les RETAINED+DISCARDED sur la nouvelle analyse, F-176 SF-176-01) — la matérialisation lit les RETAINED clonées

---

## Plan de test

### Tests unitaires

- [ ] `RetainedPisteToolMatcherTest` — match exact baseJuridique CESEDA → F-IM-05
- [ ] `RetainedPisteToolMatcherTest` — match keyword "RAPO" → F-IM-06
- [ ] `RetainedPisteToolMatcherTest` — texte sans baseJuridique parsable → null
- [ ] `RetainedPisteToolMatcherTest` — loi 15/12/1980 BE → F-IM-05 (BE)
- [ ] `RetainedPisteAlignmentServiceTest` — `materializeForAnalysis` piste retenue alignée avec top-3 outil → matchStatus ALIGNED persisté dans `retained_pistes_alignment_json`
- [ ] `RetainedPisteAlignmentServiceTest` — `materializeForAnalysis` piste retenue absente du top-3 → matchStatus DIVERGENT
- [ ] `RetainedPisteAlignmentServiceTest` — `materializeForAnalysis` outil cible jamais tourné → matchStatus NOT_ANALYZED
- [ ] `RetainedPisteAlignmentServiceTest` — `materializeForAnalysis` exception lecture *Analysis → fail-open NOT_ANALYZED
- [ ] `RetainedPisteAlignmentServiceTest` — `materializeForAnalysis` propagateToPiecesManquantes idempotent (2 runs = 1 entrée)
- [ ] `RetainedPisteAlignmentServiceTest` — `materializeForAnalysis` propagateToDeadlines parse "Court terme (3-6 mois)" → today+6 mois
- [ ] `RetainedPisteAlignmentServiceTest` — `materializeForAnalysis` horizon non parsable → pas de délai créé (fail-open)
- [ ] `RetainedPisteAlignmentServiceTest` — `getForLatestAnalysis` lit `retained_pistes_alignment_json` de la dernière analyse DONE
- [ ] `RetainedPisteAlignmentServiceTest` — `getForLatestAnalysis` analyse legacy sans json → retourne `[]`
- [ ] `CaseFileDashboardServiceTest` — pas d'alignement matérialisé → pas de tile `RETAINED_PISTES_SUMMARY`
- [ ] `CaseFileDashboardServiceTest` — alignement avec 3 RETAINED dont 1 DIVERGENT → tile alertLevel ALERT, primary=3, secondary=1
- [ ] `StrategicOptionServiceTest` — `updateStatus` reste pur, aucune mutation pieces/délais (régression test cohérence F-176)

### Tests d'intégration

- [ ] `RetainedPisteAlignmentControllerIT` — `GET /retained-pistes-alignment` après run synthèse enrichie → 200 avec entrées matérialisées
- [ ] `RetainedPisteAlignmentControllerIT` — `GET /retained-pistes-alignment` sur dossier sans analyse DONE → 200 + `[]`
- [ ] `RetainedPisteAlignmentControllerIT` — `GET /retained-pistes-alignment` sur dossier avec analyse DONE pré-F-192 (alignment_json null) → 200 + `[]`
- [ ] `RetainedPisteAlignmentControllerIT` — case file dans autre workspace → 404
- [ ] `RetainedPisteAlignmentControllerIT` — utilisateur non authentifié → 401
- [ ] `EnrichedAnalysisServiceIT` — run synthèse enrichie sur dossier avec piste RETAINED L.421-14 + outil F-IM-05 retourne L.421-14 en top-1 → après run, `retained_pistes_alignment_json` contient matchStatus ALIGNED
- [ ] `EnrichedAnalysisServiceIT` — run avec piste RETAINED `conditions = ["Convention INRIA"]` → `pieces_manquantes` de la nouvelle analyse contient l'entrée source `PISTE_RETENUE`
- [ ] `EnrichedAnalysisServiceIT` — run avec piste RETAINED `horizon_temporel = "Court terme (3-6 mois)"` → `case_deadlines` contient une entrée nouvelle date today+6mois source `PISTE_RETENUE`
- [ ] `EnrichedAnalysisServiceIT` — 2 runs successifs identiques → pas de doublons `pieces_manquantes` ni `case_deadlines`
- [ ] `EnrichedAnalysisServiceIT` — run où `materializeForAnalysis` lève exception → run réussit quand même, log warn, alignment_json reste null (fail-open)
- [ ] `StrategicOptionServiceIT` — `PUT /strategic-options/{id} { statut: RETAINED }` ne déclenche AUCUNE mutation pieces_manquantes/case_deadlines (test de régression cohérence F-176 strict)
- [ ] `CaseFileDashboardControllerIT` — la réponse contient la tile `RETAINED_PISTES_SUMMARY` après run synthèse enrichie avec ≥ 1 piste matérialisée

### Isolation workspace

- [x] Applicable — IT explicite : workspace A ne peut pas lire l'alignement de dossier workspace B (404 camouflage)
- [x] Applicable — IT explicite : update statut piste workspace A propage uniquement aux pieces/délais du dossier workspace A

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non touché
- [ ] Workspace context — non touché (réutilise `WorkspaceMemberRepository.findByUserAndPrimaryTrue` existant via `StrategicOptionService`)
- [ ] Plans / limites — non touché
- [ ] Navigation / routing frontend — non concerné (mini-spec backend)
- [ ] **Aucune préoccupation transversale critique** — extension fonctionnelle isolée

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `CaseFileDashboardService.assembleTiles` | Ajout d'une tile — peut casser tests de comptage existants | IT existant `CaseFileDashboardControllerIT` à étendre, comptage tile par theme conservé |
| `StrategicOptionService.updateStatus` | **AUCUN side-effect ajouté** (cohérence F-176 stricte) — comportement strictement inchangé | IT régression `StrategicOptionServiceIT` — vérifie que PUT statut n'affecte aucune autre table |
| `EnrichedAnalysisService.run` | Nouveau hook `materializeForAnalysis` à la fin du run, après `propagateRetainedAndDiscarded`. Run plus lent (+200-500 ms en lecture *Analysis tables) | IT existant `EnrichedAnalysisServiceIT` à étendre, perf p99 < 500 ms supplémentaires |
| `case_analyses` table | Nouvelle colonne `retained_pistes_alignment_json` — peut affecter sérialisation `CaseAnalysis` JPA | UT `CaseAnalysisRepositoryTest` à valider sur read/write |
| `pieces_manquantes` consumers (synthesis frontend, export PDF F-95) | Nouvelle valeur d'enum source `PISTE_RETENUE` à reconnaître côté lecture | F-95 export PDF `PISTE_RETENUE` traité comme `IA` par défaut V1 (raffinement V2) |
| `case_deadlines` consumers (delais-section, dashboard tile delais) | Idem | Vérifier rendu — devrait juste afficher l'entrée comme une autre |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/auth.spec.ts` — pas concerné (pas de changement auth)
- [ ] `e2e/smoke/workspace.spec.ts` — pas concerné
- [ ] Aucun smoke E2E nouveau requis V1 — couverture IT suffisante

---

## Dépendances

### Subfeatures bloquantes

- F-176 SF-176-01 / SF-176-02 — Terminée (`strategic_options` table + endpoints CRUD)
- F-167 SF-167-01..05 — Terminée (`DashboardTile` + `CaseFileDashboardService`)
- F-IA-04 — Terminée (TOOL_REGISTRY, mapping outils décisionnels)

### Questions ouvertes impactées

- [ ] Aucune question ouverte de `OPEN_QUESTIONS.md` n'est concernée

---

## Impact par domaine métier

Cette feature est sensible au domaine et au pays :

- **Droit du travail** : V1 hors scope (V2 si signal terrain). Pistes futures concerneront `F-DT-08/09/31` (validité licenciement, indemnités, transaction). Mapping baseJuridique Code du travail → toolId à définir V2.
- **Droit immigration** : V1 couvert intégralement. FR (CESEDA) + BE (loi 15/12/1980). Mapping baseJuridique → F-IM-05 (titres) ou F-IM-06 (recours) figé en code Java.
- **Droit famille** : V1 hors scope (V2 si signal terrain). Pistes futures concerneront `F-FA-05/06/07` (partage, garde, divorce). Mapping baseJuridique Code civil → toolId à définir V2.

L'asymétrie V1 (Immigration only) est acceptable : les pistes stratégiques sont **transversales** par nature (F-176 livre le bloc pour les 3 domaines) mais leur **propagation aux outils** dépend de la stratégie de matching, qui est plus simple à figer V1 sur un seul domaine pour valider le concept avant extension.

---

## Parité des domaines métier

Cette SF n'est pas un outil décisionnel niveau ≥ 5 — elle propage des choix avocat depuis F-176 vers les outils existants. La règle de parité (sortir un scoring/comparateur sur les 3 domaines simultanément) ne s'applique pas. Mais la décision V1 = Immigration only doit être **réévaluée** lors d'une éventuelle V2 :
- Famille : pistes typiques "Divorce pour faute vs amiable", "Liquidation différée" → mapping vers `F-FA-07` checklist divorce + `F-FA-05` partage
- Travail : pistes typiques "Prise d'acte vs licenciement nul", "Transaction vs prud'hommes" → mapping vers `F-DT-08/09` validité + `F-DT-31` transaction

Si signal terrain confirme que les avocats Famille/Travail utilisent intensivement le bloc Pistes, ouvrir F-192 V2 SF-192-04 (mapping Famille) + SF-192-05 (mapping Travail).

---

## Notes et décisions

- **Décision 2026-05-06** : V1 limite le matching à Immigration FR+BE pour cadrer le scope et valider le concept. Famille/Travail → V2 selon signal terrain.
- **Décision 2026-05-06 (rectif)** : **gating strict Synthèse enrichie**. Le PUT statut piste reste un PUT pur sans side-effect (cohérence F-176 stricte). Tous les effets F-192 (matérialisation alignement, propagation pieces/délais, tile dashboard, badges) se déclenchent **uniquement au run de Synthèse enrichie**. Modèle mental : « tag piste = signal pour la prochaine synthèse enrichie ». Évite les ghost entries quand l'avocat tag puis détag.
- **Décision 2026-05-06** : alignement persisté en DB (`case_analyses.retained_pistes_alignment_json`) plutôt que recalculé à la volée — calcul potentiellement coûteux (lecture multiple `*Analysis`), persister à la matérialisation est plus efficace et garantit la cohérence visuelle (pas de drift entre tile dashboard et endpoint alignement).
- **Décision 2026-05-06** : asymétrie volontaire — la validation d'une recommandation outil ne crée PAS automatiquement de piste RETAINED. Les pistes restent une saisie IA + curation avocat manuelle.
- **Décision 2026-05-06** : propagation pieces/délais idempotente, sans rollback. Si avocat repasse une piste de RETAINED → TO_STUDY puis relance synthèse enrichie, les pieces/délais déjà créés ne sont PAS supprimés (l'avocat les supprime à la main si besoin). Évite les suppressions silencieuses.
- **Décision 2026-05-06** : propagation `PISTE_RETENUE` traitée comme `IA` par les consumers V1 qui ne reconnaissent pas la nouvelle valeur (rendu lecture). Raffinement UI V2.
