# Mini-spec — F-185 / SF-185-01 Streaming SSE de la synthèse + Sonnet streaming + persistance partielle

## Identifiant

`F-185 / SF-185-01`

## Feature parente

`F-185` — Pipeline IA réduction de la latence perçue (umbrella)

## Statut

`ready`

## Date de création

2026-05-03

## Branche Git

`feat/SF-185-01-streaming-synthesis`

---

## Objectif

Quand l'avocat lance une analyse de dossier, lui présenter la synthèse **section par section au fur et à mesure** que Sonnet la produit (au lieu d'attendre la réponse complète pendant 30-60 sec à 5-10 min), avec **persistance partielle DB** pour qu'un refresh de la page synthèse pendant le streaming ne fasse pas perdre les sections déjà arrivées.

---

## Comportement attendu

### Cas nominal — page synthèse pendant streaming

1. Avocat clique "Analyser" sur la page dossier.
2. Backend reçoit le message RabbitMQ `CaseAnalysisMessage` et crée une `CaseAnalysis` row en `PROCESSING` (comportement actuel inchangé).
3. Backend appelle Anthropic en **mode streaming** (`stream=true`). Sonnet émet des `content_block_delta` au fil de la génération.
4. Backend accumule les deltas dans un buffer ; à chaque fois qu'une section JSON top-level (`faits`, `points_juridiques`, `risques`, `timeline`, `questions_ouvertes`, `pieces_manquantes`, `points_procedure`, `pistes_strategiques`, `score_risque`, `delais_detectes`, `source_explanations`) est détectée comme **complètement closée** dans le buffer, le backend :
   - Persiste l'état partiel dans `case_analyses.partial_state JSONB` (UPDATE)
   - Émet un nouvel événement SSE `CASE_ANALYSIS_PARTIAL` avec payload `{caseFileId, status: "PARTIAL", jobType: "CASE_ANALYSIS", sections: ["faits", "points_juridiques"]}` (juste les noms des sections complétées à ce stade — l'avocat fait un GET sur l'endpoint partial pour récupérer le contenu)
5. Frontend `case-file-detail` reçoit le 1er `CASE_ANALYSIS_PARTIAL` → set signal `hasPartialSynthesis = true` → bouton **"Voir la synthèse (en cours…)"** apparaît avec icône pulse.
6. Avocat clique le bouton → arrive sur `/case-files/:id/synthesis`.
7. Page synthèse :
   - Au load, fait `GET /api/v1/case-files/{id}/case-analysis/partial` → reçoit le `partial_state` actuel
   - Affiche les sections déjà complètes + skeleton placeholders sur les sections en attente
   - Souscrit aux événements SSE `events$` du `GlobalAnalysisNotificationService` ; à chaque `CASE_ANALYSIS_PARTIAL`, refait un GET partial → met à jour les sections
   - Au `CASE_ANALYSIS_DONE`, fait un GET full standard → bascule en mode complet
8. Backend, à la fin du streaming Sonnet :
   - Comportement final inchangé : `finalizeCaseAnalysis` persiste le résultat complet, lance procedure checks / strategic options / deadlines / source explanations, fire `CASE_ANALYSIS_DONE` event
   - **Purge `partial_state` (set NULL)** — l'état complet remplace le partiel

### Cas nominal — refresh de la page synthèse pendant streaming

L'avocat F5 sa page synthèse alors que l'analyse est encore PROCESSING. Le frontend :
1. Au load, query `analysis-jobs` pour connaître l'état du job CASE_ANALYSIS
2. Si `PROCESSING` → fait `GET /api/v1/case-files/{id}/case-analysis/partial`
3. Si endpoint retourne 200 avec partial → affiche les sections accumulées + skeletons sur les sections manquantes + se souscrit aux SSE PARTIAL pour la suite
4. Si endpoint retourne 204 (analyse a démarré mais aucune section encore) → spinner global + souscription SSE
5. Si l'endpoint retourne 404 (pas d'analyse en cours) → comportement actuel

**Aucune perte d'information au refresh** — c'est le bénéfice central de l'Option B (persistance DB).

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| Streaming Anthropic échoue en cours (HTTP 5xx, network) | Retry avec backoff (pattern `doAnalyze` existant). Si tous les retries échouent → fallback vers `analyzeWithSystemCache` non-streaming. Si ça échoue aussi → `FAILED` standard. **Aucune régression.** |
| Sonnet retourne du JSON malformé en cours de stream | L'accumulateur ne détecte aucune section complète → aucun PARTIAL émis. À la fin, `finalizeCaseAnalysis` fait son traitement standard (extraction JSON, peut échouer). Pas de perte. |
| Backend redémarre pendant streaming | `partial_state` est persisté en DB, mais le job `CASE_ANALYSIS` reste `PROCESSING` orphelin. Comportement existant : pas de re-prise automatique du job (limitation hors scope SF-185-01). L'avocat doit relancer une analyse. |
| Frontend refresh juste après `CASE_ANALYSIS_DONE` mais avant que la synthèse complète soit chargée | GET partial retourne 204 (déjà purgé) → fallback sur GET synthèse complète standard |
| L'avocat n'a pas encore visité la page synthèse pendant streaming | Bouton "Voir la synthèse (en cours…)" sur la page dossier dès le 1er PARTIAL — il peut cliquer quand il veut |

---

## Analyse de cohérence transversale

- [x] **Autres endpoints SSE** : `SseNotificationService` est l'unique service de push SSE. Ajout du nouveau type d'événement `CASE_ANALYSIS_PARTIAL` côté backend + status `PARTIAL` dans l'enum `AnalysisStatus`. Cohabite avec `*_DONE`/`*_FAILED` existants (pattern miroir).
- [x] **Pattern persistance partielle** : nouveau pour ce projet. Pas d'autre flux qui persiste un état intermédiaire JSONB. Encapsulé dans `case_analyses` (entité existante) — pas de nouvelle table.
- [x] **Pattern Anthropic streaming** : nouveau dans `AnthropicService`. Ajout d'une méthode `analyzeWithSystemCacheStreaming()` ; les méthodes existantes (`analyze`, `analyzeFast`, `analyzeWithSystemCache`, `analyzeWithImages`) restent inchangées. F-120 blog utilise déjà `cache_control` mais pas streaming — pas d'overlap.
- [x] **Frontend SSE consumer** : `AnalysisSseService` (durci par SF-159-01) consomme déjà les événements typés `*_DONE`/`*_FAILED`. Ajout du type `CASE_ANALYSIS_PARTIAL` (status `PARTIAL`) — propagation via `events$` du `GlobalAnalysisNotificationService` (durci par SF-159-01) sans changer le contrat existant.
- [x] **Pattern skeleton UI** : nouveau dans le projet. Encapsulé dans `SynthesisComponent`. Pas de directive `shared/` extraite (un seul consommateur).
- [x] **Sections JSON synthèse** : les 11 sections (`faits`, `points_juridiques`, `risques`, `timeline`, `questions_ouvertes`, `pieces_manquantes`, `points_procedure`, `pistes_strategiques`, `score_risque`, `delais_detectes`, `source_explanations`) sont définies dans `CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE`. Le streaming respecte leur ordre d'apparition dans le JSON output (qui dépend de l'ordre dans le prompt).

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature.

---

## Critères d'acceptation

- [ ] Migration Liquibase 201 ajoute colonne `case_analyses.partial_state JSONB NULL` (rétrocompat zéro impact).
- [ ] `AnalysisStatus` enum accepte une valeur `PARTIAL` (en plus de `PENDING`/`PROCESSING`/`DONE`/`FAILED`).
- [ ] `AnthropicService.analyzeWithSystemCacheStreaming(systemPrompt, prompt, maxTokens, sectionListener)` ajouté ; consume Anthropic streaming SSE, accumule deltas, appelle `sectionListener.onSection(name, partialJson)` à chaque section JSON complète détectée. Retour final identique à `analyzeWithSystemCache`.
- [ ] `CaseAnalysisService.consumeCaseAnalysis` utilise la version streaming. Le `sectionListener` met à jour `case_analyses.partial_state` (UPDATE atomique) et publie `AnalysisStatusEvent(caseFileId, PARTIAL, CASE_ANALYSIS)`.
- [ ] `SseNotificationService` émet l'événement nommé `CASE_ANALYSIS_PARTIAL` avec payload `{caseFileId, status: "PARTIAL", jobType: "CASE_ANALYSIS"}` (sans contenu — frontend doit GET).
- [ ] À la fin du streaming, `finalizeCaseAnalysis` set `partial_state = NULL` dans la même transaction que la persistance de `analysis_result`.
- [ ] Nouveau endpoint `GET /api/v1/case-files/{id}/case-analysis/partial` :
  - Auth : MEMBER, isolation workspace
  - 200 + `{partial_state: {...}}` si analyse PROCESSING avec partial state non null
  - 204 No Content si analyse PROCESSING sans partial state OU analyse DONE (vient d'être complétée)
  - 404 si pas d'analyse en cours sur ce dossier
- [ ] Frontend `SynthesisComponent` au load :
  - Si analysis status PROCESSING → fait GET partial. Si 200 → affiche les sections déjà reçues + skeletons. Souscrit `events$` pour la suite.
  - Si analysis status DONE → comportement actuel
- [ ] Frontend `case-file-detail` : nouveau signal `hasPartialSynthesis()` true dès qu'un événement `CASE_ANALYSIS_PARTIAL` est reçu via `events$` (ou au load si l'API partial retourne 200). Le bouton "Voir la synthèse (en cours…)" remplace le spinner texte quand `caseAnalysisRunning() && hasPartialSynthesis()`.
- [ ] CSS `.icon-pulse` + `.synthesis-link--live` : icône qui pulse subtilement (1.5s loop ease-in-out), badge or "en cours…" à côté du label. `prefers-reduced-motion: reduce` désactive l'animation.
- [ ] Fallback gracieux : si `analyzeWithSystemCacheStreaming` lève une exception non-retryable, tomber sur `analyzeWithSystemCache` (mode actuel). Logué WARN.
- [ ] Tests UT backend : 6+ cas (streaming happy path 5 sections détectées + persistance partial + émission events ; section JSON malformée ignorée ; fallback non-streaming ; endpoint partial 200/204/404 ; isolation workspace endpoint).
- [ ] Tests UT frontend : 5+ cas (SynthesisComponent load avec partial / sans partial / réception PARTIAL via events$ / réception DONE / case-file-detail bouton states 3 modes).
- [ ] Suite Jest frontend complète verte. Suite backend > 3000 tests verte.
- [ ] Build prod frontend OK. Build backend `mvn package -DskipTests` OK.

---

## Périmètre

### Hors scope (→ SF-185-02 / SF-185-03 / futur)

- Streaming pour `EnrichedAnalysisService` (re-synthèse enrichie) — **report SF-185-01b** si signal terrain. Le flow re-synthèse est moins critique en latence perçue (avocat moins anxieux qu'au 1er paint).
- Évolution du bandeau SF-159-01 vers compteur "N/M sections" (gardé indéterminé)
- Q&A async (→ SF-185-02)
- Synthèse incrémentale par doc (→ SF-185-03)
- Reprise auto d'un job CASE_ANALYSIS orphelin après redémarrage backend
- Page synthèse refonte structurelle (badges + pages dédiées) — **F-162** sépare

### Hors scope total

- Streaming pour `ChunkAnalysisService` (chunks individuels) — pas de valeur perçue (chunks invisibles à l'avocat)

---

## Contraintes de validation

| Champ | Obligatoire | Format | Notes |
|---|---|---|---|
| `case_analyses.partial_state` | Non | JSONB | Nullable. Set non-null pendant PROCESSING avec section complète, NULL au DONE/FAILED |
| Section name (sectionListener) | Oui | string énuméré | Une parmi : `faits`, `points_juridiques`, `risques`, `timeline`, `questions_ouvertes`, `pieces_manquantes`, `points_procedure`, `pistes_strategiques`, `score_risque`, `delais_detectes`, `source_explanations` |
| Endpoint partial response | — | JSON | `{partial_state: {...}}` ou 204 No Content |

---

## Technique

### Backend — fichiers touchés ou créés

| Fichier | Modification |
|---|---|
| `db/changelog/migrations/201-add-case-analyses-partial-state.xml` | NOUVEAU — ALTER TABLE add column `partial_state JSONB NULL` |
| `analysis/AnalysisStatus.java` | Ajouter valeur enum `PARTIAL` |
| `analysis/CaseAnalysis.java` | Ajouter champ `String partialState` mappé sur la nouvelle colonne |
| `analysis/AnthropicService.java` | Ajouter méthode `analyzeWithSystemCacheStreaming(...)` qui consomme le SSE Anthropic et invoque un callback à chaque section JSON complète |
| `analysis/CaseAnalysisService.java` | Modifier `consumeCaseAnalysis` pour utiliser la version streaming + listener qui persiste partial_state + publie event PARTIAL ; modifier `finalizeCaseAnalysis` pour set partial_state = NULL |
| `analysis/PartialJsonSectionExtractor.java` | NOUVEAU — utilitaire qui parse incrémentalement le JSON et détecte les sections top-level closées |
| `analysis/SseNotificationService.java` | Aucun changement — l'eventName est dérivé du jobType + status, donc CASE_ANALYSIS + PARTIAL → `CASE_ANALYSIS_PARTIAL` automatiquement |
| `analysis/CaseAnalysisController.java` (ou équivalent) | Ajouter endpoint `GET /api/v1/case-files/{id}/case-analysis/partial` |

### Frontend — fichiers touchés

| Fichier | Modification |
|---|---|
| `core/services/analysis-sse.service.ts` | Ajouter type `'CASE_ANALYSIS_PARTIAL'` au pattern d'événement reconnu (parser les événements SSE nommés `CASE_ANALYSIS_PARTIAL`) |
| `core/services/global-analysis-notification.service.ts` | Le `events$` Subject propage déjà — étendre la doc + ne pas afficher de toast pour `PARTIAL` (seulement DONE/FAILED ont des toasts) |
| `core/services/case-file.service.ts` | Ajouter méthode `getCaseAnalysisPartial(id): Observable<{partial_state: any} \| null>` (retourne null sur 204) |
| `case-files/synthesis/synthesis.component.ts` | Au load, si analysis PROCESSING → fetch partial, render skeletons + sections reçues, subscribe events$ |
| `case-files/case-file-detail/case-file-detail.component.ts` | Nouveau signal `hasPartialSynthesis = signal(false)` ; subscribe events$ pour set true sur PARTIAL ; expose computed `synthesisLinkMode` (none / live / done) |
| `case-files/case-file-detail/case-file-detail.component.html` | Modifier le block synthesis pour afficher 3 états (spinner / lien live / lien done) |
| `case-files/case-file-detail/case-file-detail.component.scss` | Ajouter `.synthesis-link--live` + `@keyframes iconPulse` + `prefers-reduced-motion` |

### Migration Liquibase

- [x] Oui — `201-add-case-analyses-partial-state.xml`
- Réversible : `<rollback>DROP COLUMN partial_state</rollback>`

---

## Plan de test

### Tests unitaires backend (8+ cas)

- `AnthropicServiceStreamingTest` — happy path : 5 sections streamées dans l'ordre → callback invoqué 5 fois, valeurs correctes
- `AnthropicServiceStreamingTest` — section avec virgules dans le contenu (édge case parsing) — détection correcte de la fermeture
- `AnthropicServiceStreamingTest` — JSON malformé en cours → aucun callback (fail-open)
- `AnthropicServiceStreamingTest` — fallback non-streaming si streaming throw
- `CaseAnalysisServiceStreamingTest` — listener met à jour partial_state + publie event PARTIAL
- `CaseAnalysisServiceStreamingTest` — finalizeCaseAnalysis nettoie partial_state au DONE
- `CaseAnalysisControllerTest` — endpoint partial : 200 / 204 / 404 / isolation workspace
- `PartialJsonSectionExtractorTest` — 4-6 cas (sections détectées dans l'ordre, sections vides, sections imbriquées, payload minimal)

### Tests intégration backend

- `CaseAnalysisStreamingIT` — flow complet : message RabbitMQ → streaming mocké → partial_state mis à jour 3 fois → DONE → partial_state = NULL → endpoint partial 204

### Tests Jest frontend (5+ cas)

- `synthesis.component.spec.ts` — load avec partial 200 : sections rendues + skeletons sur les manquantes
- `synthesis.component.spec.ts` — load avec partial 204 : skeletons partout
- `synthesis.component.spec.ts` — réception SSE PARTIAL via events$ : refetch partial + update sections
- `case-file-detail.component.spec.ts` — `hasPartialSynthesis` true sur PARTIAL → bouton "Voir la synthèse (en cours…)"
- `case-file-detail.component.spec.ts` — synthesis() loaded → bouton normal "Voir la synthèse"

### Smoke manuel staging — checklist

1. Ouvrir un dossier riche (5-10 docs)
2. Cliquer "Analyser"
3. Bandeau SF-159-01 apparaît
4. **Dans 5-10 sec** : bouton "Voir la synthèse (en cours…)" apparaît sur la page dossier
5. Cliquer le bouton → page synthèse avec sections déjà reçues + skeletons sur les autres
6. Attendre que les sections suivantes arrivent → skeletons remplacés par contenu
7. F5 sur la page synthèse pendant le streaming → sections reprises sans perte
8. Attendre fin → flash dashboard SF-159-02, toast SF-159-02
9. Aucune erreur console

### Isolation workspace

- [x] Applicable : endpoint `GET /case-files/{id}/case-analysis/partial` doit vérifier que le caseFile appartient au workspace de l'utilisateur (pattern existant des endpoints case-analysis).

---

## Analyse d'impact — préoccupations transversales

- [ ] **Auth / Principal** : non touché.
- [ ] **Workspace context** : non touché.
- [ ] **Plans / limites** : non touché.
- [ ] **Navigation / routing** : non touché — pas de nouvelle route, juste un bouton qui change d'état.
- [ ] **Outil décisionnel métier** : non.
- [x] **Aucune préoccupation transversale critique**.

### Décision

- [x] Aucune préoccupation transversale critique. Smoke manuel staging suffit.

---

## Impact par domaine métier

Cette SF est **purement transversale infrastructure UX**. Le streaming s'applique identiquement aux 3 domaines (Travail / Immigration / Famille) × 2 pays (FR / BE) — c'est le pipeline IA générique qui change, pas un outil métier spécifique.

---

## Dépendances

- F-39 SSE infra : Terminée ✅ (utilisé)
- SF-159-01 fix EventSource frontend : Terminée ✅ (utilisé)
- SF-159-02 flash + toast : Terminée ✅ (compatible — déclenchés au DONE final, pas au PARTIAL)
- F-37 versioning synthèse : Terminée ✅ (versions inchangées par streaming)

---

## Notes et décisions

- **Choix Anthropic streaming via RestClient existant** : pas d'ajout de WebClient ou OkHttp pour minimiser la surface technique. On utilise `restClient.post().retrieve().body(...)` avec un `ParameterizedTypeReference` pour récupérer le body en `InputStream` ou un `ResponseExtractor`. Implémentation : appel HTTP avec header `Accept: text/event-stream`, lecture ligne par ligne du body, parsing des events `data: {json}`.
- **Choix payload SSE PARTIAL minimal** : on n'envoie PAS le contenu de la section dans l'événement SSE — on notifie juste qu'une section est dispo, le frontend fait un GET. Avantages : (a) évite de pousser des MB via SSE qui n'est pas conçu pour ça ; (b) permet au frontend de récupérer l'état à jour en une seule requête peu importe le nombre d'événements ratés ; (c) découple le throughput SSE de la taille du contenu.
- **Choix purger partial_state au DONE** : l'état complet (`analysis_result`) remplace le partiel — le partial_state n'a plus de valeur. Garder le partial alourdirait la table sans bénéfice. En cas de besoin de debug, les logs structurés Sentry capturent les transitions.
- **Choix nouvel enum value PARTIAL** : alternative aurait été un nouveau type d'événement séparé (pas un AnalysisStatus). Mais le `SseNotificationService` dérive le nom d'événement de `jobType + status`, donc ajouter PARTIAL à l'enum est plus simple et cohérent. Le statut `PARTIAL` n'est PAS persisté sur l'AnalysisJob (qui reste `PROCESSING` pendant le streaming) — c'est seulement un statut SSE éphémère.
- **Pas de feature flag** : fallback gracieux dans le code suffit. Si streaming Anthropic plante, on tombe sur l'appel synchrone existant. Pas besoin de toggle env.
- **Ordre des sections** : déterminé par Sonnet selon le JSON schema dans le prompt. Aujourd'hui : `timeline, faits, points_juridiques, risques, questions_ouvertes, pieces_manquantes, points_procedure, pistes_strategiques, score_risque, delais_detectes, source_explanations`. Si ROI démontré sur le first paint, on pourra réécrire le prompt pour forcer un ordre stratégique (faits en 1er) — hors scope SF-185-01.
