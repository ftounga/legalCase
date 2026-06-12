# SF-287-01 — Streaming des conclusions + barre de progression précise

> Étape 0 (`SF-287-00-coherence.md`, GO) + 0 bis (GO avec ajustements). Réutilise l'infra streaming F-185. Back + front (contrat SSE figé).

## Objectif (1 phrase)
Streamer la génération des conclusions (l'acte se construit en direct) et afficher une **barre de progression précise** (pourcentage réel + section en cours), pour effondrer la latence **perçue** sans changer le contenu produit.

## Contrat SSE (FIGÉ)
- **Endpoint** : `GET /api/v1/case-files/{id}/conclusions/stream` (`text/event-stream`), isolation workspace (même contrat que `/analysis-status/stream`).
- **Événements** (un `event:` nommé + `data:` JSON) :
  - `progress` → `{ "tokens": <int reçus>, "maxTokens": 8000, "percent": <0..100>, "currentSection": "<dernier titre ## détecté ou null>", "sectionIndex": <int|null>, "partialContent": "<markdown partiel>" }`
  - `done` → `{ "status": "DONE", "versionNumber": <int> }` (le content final + bordereau est récupéré via le `GET /conclusions` existant)
  - `failed` → `{ "status": "FAILED", "message": "<court>" }`
- **Heartbeat** : commentaire SSE périodique pour garder la connexion (comme F-185).
- **Fallback** : si pas de flux (SSE indispo/expiré), le front retombe sur le **polling `GET /conclusions` existant** (3 s).

## Backend
1. **Worker** `CaseConclusionService.generate` : remplacer `analyzeWithSystemCache(...)` par `analyzeWithSystemCacheStreaming(...)` (variante existante), avec un **callback de fragment** qui :
   - accumule le content partiel ;
   - calcule `tokens` reçus (approx. via longueur / ratio, ou compteur du SDK si exposé) et `percent = min(100, round(tokens/maxTokens*100))` ;
   - détecte `currentSection` = dernier titre markdown `##`/`###` dans le partiel (réutiliser un util de parsing de sections) + `sectionIndex` ;
   - publie un événement `progress` à chaque fragment (throttlé ~1/250 ms) vers les emitters du dossier.
2. **Registry emitters conclusions** : réutiliser `SseEmitterRegistry` (clé `caseFileId`) ou un `ConclusionStreamEmitterRegistry` jumeau.
3. **`ConclusionStreamController`** : `GET .../conclusions/stream` → enregistre un `SseEmitter` (timeout aligné F-185), isolation workspace synchrone AVANT création de l'emitter (cf. `AnalysisStatusStreamController`). Court-circuit si aucune génération active (envoie l'état courant puis complète).
4. **finalize** inchangé (à la fin du flux : content + bordereau déterministe, événement `done`). En cas d'exception : événement `failed` + statut FAILED existant.
5. **Gates inchangées** : `AiCallContext` obligatoire conservé sur l'appel streaming ; aucun changement de prompt/modèle/MAX_TOKENS.

## Frontend
6. **Composant `conclusion-generation-progress`** (nouveau) : `mat-progress-bar` **déterminée** (mode `determinate`, `[value]="percent"`), stylée design system (piste marine `#1A3A5C`, remplissage or `#C9973A`, hauteur ~10px, coins arrondis), + libellé Inter « **Rédaction : {{ currentSection }}** » + « {{ percent }} % ». `aria-valuenow`. Animation fluide (transition CSS sur la largeur).
7. **`conclusions-section`** : à l'état PROCESSING, **souscrire au SSE** (`EventSource`/service) → mettre à jour la barre + **rendre le `partialContent` en direct** (réutilise `conclusion-document` / l'aperçu) au lieu du spinner muet. À `done` → recharger la version finale (`GET /conclusions`) ; à `failed` → message d'erreur existant. **Fermer l'EventSource** au DONE/FAILED/destroy. **Fallback** : si l'EventSource erreur → polling existant.

## Critères d'acceptation
1. Pendant la génération, l'acte **s'affiche progressivement** (premier texte en quelques secondes) au lieu d'un spinner figé.
2. La **barre de progression** avance avec un **pourcentage réel** (tokens/maxTokens), visible (design system marine/or), et affiche la **section en cours** (titre détecté).
3. À la fin, l'acte **final** (avec bordereau, identique au mode non-streamé) s'affiche ; barre disparue.
4. Échec → message d'erreur ; perte de SSE → repli polling, jamais d'écran bloqué.
5. **Contenu produit inchangé** (mêmes gardes, même qualité) ; isolation workspace respectée sur le nouvel endpoint.
6. Tests back (controller SSE + worker streaming émet progress/done/failed) + front (composant barre + souscription/fermeture + fallback) verts.

## Hors périmètre
- Génération par sections en parallèle (gain de temps RÉEL, autre chantier).
- Changement de modèle / réduction de l'acte.

## Fichiers
- **Back** : `CaseConclusionService` (worker streaming + progress), `ConclusionStreamController` (+ registry réutilisé), DTO événement.
- **Front** : `conclusion-generation-progress.component.*` (nouveau), `conclusions-section.component.*` (souscription + rendu live), service de stream.
