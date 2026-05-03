# SF-185-07 — Fix `PartialJsonSectionExtractor` : gérer le wrapper Markdown de Sonnet

## Objectif (1 phrase)
Corriger le bug `chunks=446 sections=0 persists=0` observé en staging le 2026-05-03 sur le dossier Chen 7 : le streaming Anthropic fonctionne, mais l'extracteur abandonne sur le 1er backtick du wrapper Markdown (` ```json\n{...}\n``` `) que Sonnet 4.6 ajoute autour de sa sortie JSON, donc aucune section n'est jamais émise et `partial_state` reste NULL → l'écran "Synthèse en cours" reste vide.

## Diagnostic

### Observation prod (post-SF-185-05)

Logs staging 2026-05-03 sur dossier Chen 7 (`b3dbf669-...`) après le déploiement de l'instrumentation SF-185-05 :

```
Case analysis START for caseFile b3dbf669-... (24832 chars, streaming)
Case analysis STREAMING SUMMARY caseFile=b3dbf669-... chunks=446 sections=0 persists=0
```

→ Le stream Anthropic fonctionne (446 deltas reçus), mais `PartialJsonSectionExtractor` n'a détecté aucune section. Bug localisé au parser.

### Cause racine

`PartialJsonSectionExtractor.scanNewSections()` Phase 1 :
```java
int rootStart = indexOfNonWhitespace(buffer, 0);
if (rootStart < 0 || buffer.charAt(rootStart) != '{') return newlyClosed;
```

Sonnet 4.6 enveloppe régulièrement sa sortie JSON dans un bloc Markdown :
```
```json
{"timeline": [...], "faits": [...], ...}
```
```

Le 1er char non-whitespace est `` ` `` (backtick), pas `{`. L'extracteur retourne immédiatement et **n'avance jamais le cursor** → tous les appels `append()` suivants ré-évaluent la même condition et retournent vide → 0 section émise pour toujours.

C'est pour ça que `CaseAnalysisResponse.stripMarkdownCodeBlock()` (utilisée par le path **non-streaming**) existe : Sonnet wrap parfois en Markdown, parfois pas. Le path streaming n'avait pas l'équivalent.

### Reproducer

Test isolé (sans dépendance Anthropic), JSON Chen 7 réel (40 ko) wrappé dans ` ```json\n...\n``` ` et streamé par chunks de 80 chars (taille typique d'un `text_delta` Sonnet) :

```java
String wrapped = "```json\n" + json + "\n```";
PartialJsonSectionExtractor ext = new PartialJsonSectionExtractor();
for (int i = 0; i < wrapped.length(); i += 80) {
    ext.append(wrapped.substring(i, Math.min(i + 80, wrapped.length())));
}
assertThat(ext.snapshot().keySet()).contains("timeline");  // FAIL avant fix
```

→ AVANT fix : `snapshot().keySet()` est vide → assertion échoue → bug prod reproduit en isolation.

## Approche du fix

Changer Phase 1 :

```java
// AVANT
int rootStart = indexOfNonWhitespace(buffer, 0);
if (rootStart < 0 || buffer.charAt(rootStart) != '{') return newlyClosed;
cursor = rootStart + 1;

// APRÈS
int rootStart = buffer.indexOf("{");
if (rootStart < 0) return newlyClosed; // pas encore de `{`, on attend
cursor = rootStart + 1;
```

Effets :
- **Pure JSON** (`{...}`) : `indexOf("{")` retourne 0, comportement identique.
- **Whitespace prefix** (`   {...}`) : skip whitespace, comportement identique.
- **Markdown wrapper** (`` ```json\n{...}\n``` ``) : skip les 8 chars de préambule, trouve le `{`, parse normalement. **Bug fixé**.
- **Préambule textuel** (ex. `Here is the JSON: {...}`) : skip le préambule, parse normalement. Robuste.
- **Chunk arrivant sans aucun `{`** (ex. premier chunk = juste ` ``` `) : `indexOf("{")` retourne -1 → on attend le prochain chunk → progressif. Comportement attendu.

Le suffixe ` \n``` ` final n'est pas un problème : la Phase 2 voit `}` (fin de l'objet racine) et termine proprement avant d'atteindre les backticks.

## Comportement nominal après fix

1. Avocat clique "Analyser le dossier"
2. Backend lance le stream Anthropic
3. Sonnet émet ` ```json\n{"timeline": [...` chunk par chunk
4. `PartialJsonSectionExtractor.append()` skip ` ```json\n `, trouve `{`, parse les sections au fil de l'eau
5. Chaque section close → `persistPartialAndNotify()` → `partial_state` mis à jour en DB → SSE `CASE_ANALYSIS_PARTIAL` envoyé
6. Frontend reçoit l'event, recharge `partial_state`, affiche les sections progressivement
7. Logs : `chunks=446 sections=11 persists=11` (au lieu de `chunks=446 sections=0 persists=0`)

## Cas d'erreur

- **JSON malformé en cours de stream** : comportement actuel inchangé (la Phase 2 attend silencieusement, retourne liste vide jusqu'à pouvoir parser).
- **Sonnet n'émet pas de wrapper** : `indexOf("{")` retourne 0, comportement identique à AVANT.
- **Préambule très long sans `{`** : on accumule dans le buffer jusqu'à voir le 1er `{`. Mémoire bornée par la taille de la sortie Sonnet (~40 ko typique).

## Critères d'acceptation

1. ✅ Test `streamRealChen7Output_withMarkdownCodeBlock_emitsSections` (JSON Chen 7 réel wrappé en Markdown, chunks de 80 chars) **passe** avec ≥ 5 sections détectées dont "timeline".
2. ✅ Test `streamRealChen7Output_emitsMultipleSections` (JSON Chen 7 réel sans wrapper, chunks de 80 chars) reste vert (régression).
3. ✅ Test `streamRealChen7Output_singleAppend_emitsAllSections` (single-append) reste vert.
4. ✅ 12 tests existants `PartialJsonSectionExtractorTest` restent verts.
5. ✅ Build `mvn package -DskipTests` vert.
6. ✅ Après deploy staging, déclencher une analyse Chen 7 → logs `STREAMING SUMMARY chunks=N sections>0 persists>0` → frontend affiche les sections progressivement.

## Plan de test

### Backend (UT)
- T-SF-185-07-01 : `streamRealChen7Output_withMarkdownCodeBlock_emitsSections` — JSON réel 40 ko wrappé ` ```json\n...\n``` ` streamé par 80 chars → ≥ 5 sections, "timeline" présent.
- T-SF-185-07-02 : `streamRealChen7Output_emitsMultipleSections` — non-régression chunked sans wrapper.
- T-SF-185-07-03 : `streamRealChen7Output_singleAppend_emitsAllSections` — non-régression single-append.
- T-existants : 12 tests `PartialJsonSectionExtractorTest` (couvrent edge cases, escapes, sections imbriquées).

### Smoke staging post-merge
1. Aller sur dossier Chen 7
2. Cliquer "Analyser le dossier"
3. Cliquer "Voir la synthèse (en cours…)"
4. **Attendu** : sections (timeline, faits, points juridiques, …) apparaissent progressivement avec bandeau "Synthèse en cours de génération…"
5. Vérifier logs : `kubectl logs -n staging deployment/legalcase-backend --tail=200 | grep STREAMING` → `chunks=N sections>0 persists>0`

## Tables / endpoints / composants impactés

- **Backend** :
  - `PartialJsonSectionExtractor.java` — Phase 1 modifiée (3 lignes)
- **Tests** :
  - `PartialJsonSectionExtractorRealOutputTest.java` — nouveau (3 tests)
  - `streaming-fixtures/chen7-real-output.json` — fixture (40 ko, JSON réel Chen 7 v3)

## Hors périmètre

- Refonte du parser (state machine plus stricte / parser externe Jackson streaming).
- Réduction du wrapper Markdown côté Sonnet (impossible — c'est un comportement du modèle).
- Augmentation de la fréquence SSE (debounce, throttle).

## Analyse de cohérence transversale

- **Auth/Principal** : non concerné.
- **Workspace context** : non concerné.
- **Plans/limites** : non concerné.
- **Navigation/routing** : non concerné.
- **Outil décisionnel métier** : non concerné — pipeline IA infra.
- **Nouveau pattern partagé** : non — modification interne d'un composant existant.

## Impact par domaine métier

Transversal — aucune adaptation par domaine (Travail / Immigration / Famille) ni par pays (FR / BE). C'est un fix infra du pipeline IA qui s'applique à toutes les analyses dossier.

## Risques

- **Préambule contenant `{`** (cas pathologique) : si Sonnet émettait un préambule du type `Voici le résultat {de mon analyse} : {...}`, le parser commencerait sur le mauvais `{`. Risque très théorique : Sonnet n'a jamais été observé produire ce pattern, et `stripMarkdownCodeBlock` côté path non-streaming n'a pas non plus de protection. Si ça se produit un jour, le test fallback (parsing complet du JSON après réception totale) corrige automatiquement.
