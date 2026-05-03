# SF-185-05 — Fix streaming SF-185-01 réellement fonctionnel

## Objectif (1 phrase)
Restaurer la promesse produit de SF-185-01 : pendant qu'une `CaseAnalysis` tourne, l'avocat clique "Voir la synthèse en cours" et voit les sections (faits, points juridiques, etc.) **réellement apparaître au fil de l'eau** au lieu de l'écran "Synthèse non disponible" observé sur staging le 2026-05-03.

## Diagnostic

3 bugs identifiés sur staging par l'utilisateur (test sur dossier `immigration-chen-7`, caseFile `b3dbf669-...`) :

| # | Bug | Lieu | Sévérité |
|---|---|---|---|
| 1 | `partial_state` n'est jamais écrit en DB pendant le stream Anthropic — soit le streaming RestClient ne stream pas vraiment (buffer), soit l'extracteur ne détecte aucune section, soit `persistPartialAndNotify` échoue silencieusement | Backend `CaseAnalysisService.consumeCaseAnalysis` + `AnthropicService.analyzeWithSystemCacheStreaming` + `PartialJsonSectionExtractor` | **Élevée** — promesse produit cassée |
| 2 | `synthesis.component.html:15` affiche "Synthèse non disponible" si `versions().length === 0` SANS regarder `synthesis()` — donc même si `tryLoadPartial()` a chargé l'état partiel, l'écran masque tout | Frontend `synthesis.component.html` | **Élevée** — masque tout effort backend |
| 3 | Collision de numéro migration `203-` (autre PR mergée le même jour avec `203-create-blog-usage-events.xml`) | Backend `db/changelog/migrations/` | **Faible** — fonctionnel mais dette de maintenance |

## Approche du fix

**Bug 2 + bug 3** : fix immédiat (certain).

**Bug 1** : fix observabilité d'abord pour confirmer le diagnostic réel — ajout de logs INFO temporaires dans la chaîne streaming pour mesurer en prod ce qui se passe (count chunks reçus, count sections détectées, count persist appelés). Si après deploy on observe `chunks=N sections=0` → bug parser. Si `chunks=N sections=M persist=M` mais partial_state NULL → bug transaction. Si `chunks=0` → RestClient ne stream pas, fix structurel à faire (probablement migrer vers `WebClient`).

Cette SF livre les 3 fixes + les logs. Une SF-185-06 ultérieure (si nécessaire après lecture des logs) traitera le fix structurel du streaming si bug 1 est confirmé.

## Comportement nominal après fix

### Frontend (bug 2)
1. Avocat clique "Analyser le dossier" sur la page dossier
2. Avocat clique "Voir la synthèse (en cours…)" → navigation vers `/case-files/{id}/synthesis`
3. `loadVersions` retourne `[]` (aucune analyse DONE)
4. `tryLoadPartial` appelle `getPartial` → 200 avec `partial_state` non vide → `applyPartial` set `synthesis()`
5. Frontend affiche **la synthèse partielle** (avec bandeau "Synthèse en cours de génération…") au lieu de "Synthèse non disponible"
6. SSE event `CASE_ANALYSIS_PARTIAL` arrive → `getPartial` rappelé → synthesis enrichie progressivement
7. SSE event `CASE_ANALYSIS_DONE` arrive → `loadVersions` rappelé → bascule sur la version DONE finale

### Backend (logs bug 1)
- Chaque appel `consumeCaseAnalysis` logge à INFO :
  - `Streaming START caseFile=X` au début
  - `Streaming chunks=N sections=M persists=K caseFile=X` à la fin (après que le stream Anthropic ait terminé)
- Si `chunks=0` ou `sections=0`, on saura immédiatement où regarder (fix structurel ultérieur)

### Migration (bug 3)
- Renommer fichier `203-drop-case-analyses-is-provisional.xml` → `204-drop-case-analyses-is-provisional.xml`
- `changeSet id` interne reste "203-drop-case-analyses-is-provisional" — Liquibase track par `id+author+filepath` ; le nouveau filepath crée un "nouveau" changeSet, mais la `preCondition columnExists` `onFail="MARK_RAN"` skip silencieusement (la colonne a déjà été droppée sur staging)
- Aucun risque sur staging ni prod

## Cas d'erreur

- **Streaming Anthropic 5xx** : exception throw → log warn `Streaming Anthropic failed` → fallback synchrone `analyzeWithSystemCache` (comportement actuel conservé)
- **persistPartialAndNotify exception** (transaction race, FK manquante) : log warn `Partial state update failed` → analyse continue (comportement actuel conservé)
- **Aucune section détectée par l'extracteur** : pas de persist → partial_state reste NULL → frontend voit `getPartial` retourner 404 → affiche "Synthèse non disponible" (cohérent : si rien à afficher, ne rien promettre)

## Critères d'acceptation

### Backend
1. ✅ `CaseAnalysisService.consumeCaseAnalysis` logge INFO `Streaming START caseFile=X` au lancement du stream
2. ✅ `CaseAnalysisService.consumeCaseAnalysis` logge INFO `Streaming SUMMARY caseFile=X chunks=N sections=M persists=K` après que le stream ait terminé (avant le fallback synchrone éventuel)
3. ✅ Migration renommée `204-drop-case-analyses-is-provisional.xml`, `changeSet id` interne inchangé
4. ✅ Build mvn vert, tests existants verts

### Frontend
5. ✅ `synthesis.component.html` : la condition "Synthèse non disponible" inclut `&& !synthesis()`
6. ✅ Le bloc `@if (synthesis())` qui suit gère bien le cas `versions() === [] && synthesis() set` (synthesis partielle affichée, version-row masquée car `versions()[0]` n'existe pas)
7. ✅ Test Jest : 3 cas (versions vide + pas de synthesis → "non dispo", versions vide + synthesis partielle → render synthese sans crash, versions DONE → render version)
8. ✅ Build npm vert, suite Jest verte

### Pipeline
9. ✅ Après deploy staging, déclencher une analyse → observer en logs INFO `Streaming SUMMARY caseFile=... chunks=... sections=... persists=...` → conclure sur le fix bug 1 nécessaire ou non

## Plan de test

### Backend (UT)
- T-01 : tests `CaseAnalysisServiceTest` existants restent verts (les logs INFO n'impactent pas la logique)
- T-02 : tests `PartialJsonSectionExtractorTest` existants restent verts

### Frontend (Jest)
- T-03 : `synthesis.component.spec.ts` — versions vide + synthesis null → render "Synthèse non disponible"
- T-04 : `synthesis.component.spec.ts` — versions vide + synthesis partielle (status PARTIAL) → render le contenu sans crash
- T-05 : `synthesis.component.spec.ts` — versions DONE → render normalement (régression)

### Smoke staging post-merge
1. Aller sur dossier de test (n'importe lequel avec ≥1 doc analysé)
2. Cliquer "Analyser le dossier"
3. Cliquer immédiatement "Voir la synthèse (en cours…)"
4. **Attendu** : page synthèse affiche bandeau "Synthèse en cours de génération…" + sections progressivement (PAS "Synthèse non disponible")
5. À la fin : verdict définitif affiché normalement
6. **Vérification logs** : `kubectl logs -n staging deployment/legalcase-backend --tail=50 | grep "Streaming SUMMARY"` → lire `chunks=N sections=M persists=K`

## Tables / endpoints / composants impactés

- **Backend** :
  - `CaseAnalysisService.java` (3 logs INFO ajoutés, AtomicInteger pour compteurs callback)
  - Migration `203-drop-case-analyses-is-provisional.xml` → renommée `204-drop-case-analyses-is-provisional.xml`
- **Frontend** :
  - `synthesis.component.html` (1 condition modifiée)
  - `synthesis.component.spec.ts` (2-3 nouveaux tests Jest)

## Hors périmètre

- Fix structurel du streaming RestClient si bug 1 est confirmé (= migration vers WebClient ou autre HTTP client streaming-friendly) — sera traité en SF-185-06 si nécessaire après lecture des logs INFO post-deploy.
- Refonte du `PartialJsonSectionExtractor` (l'isoler des problèmes de stream).
- Augmentation de la fréquence d'émission SSE (debounce, throttle).

## Analyse de cohérence transversale

- **Auth/Principal** : non concerné.
- **Workspace context** : non concerné.
- **Plans/limites** : non concerné.
- **Navigation/routing** : non concerné.
- **Outil décisionnel métier** : non concerné — pipeline IA infra.
- **Préoccupation transversale "nouveau pattern partagé"** : non concerné.

## Impact par domaine métier

Transversal — aucune adaptation par domaine (Travail / Immigration / Famille) ni par pays (FR / BE), c'est de l'infra UX du pipeline IA.

## Risques

- **Logs INFO en prod** : low signal en cas de gros volume — mais le périmètre est 1 ligne par CaseAnalysis, pas un appel par chunk. Volume négligeable.
- **Renommage migration** : si Liquibase voit le nouveau filepath comme un changeSet jamais exécuté, la `preCondition columnExists` doit fonctionner correctement. Testée localement. Si elle échoue, la migration est marquée RAN sans rien faire (comportement attendu).
