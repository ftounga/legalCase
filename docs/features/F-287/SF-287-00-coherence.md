# F-287 — Cadrage cohérence (étape 0) + cohérence écran (étape 0 bis)

> Feature : **Streaming de la génération des conclusions + barre de progression précise**. Signal terrain PO 2026-06-13 (« la génération prend trop de temps », demande d'une « jolie barre de progression visible et précise »). Optimisation d'un flux existant (F-98).

## Verdict : **GO** (étape 0) / **GO avec ajustements** (étape 0 bis)

## Intention
La génération d'un jeu de conclusions = **un seul appel IA** (~6 000 tokens, `MAX_TOKENS=8000`, Sonnet + prompt caching). Aujourd'hui le worker utilise l'appel **non-streamé** → l'avocat fixe un **spinner ~1,5–2 min** puis l'acte apparaît d'un coup. **Saut** : **streamer** l'acte (il se construit en direct) + une **barre de progression précise** (pourcentage + section en cours).

## Étape 0 — cohérence fonctionnelle
- **Amont** : l'infra streaming **existe déjà** (F-185, synthèse) : `AnthropicService.analyzeWithSystemCacheStreaming`, `SseEmitterRegistry`, `AnalysisStatusStreamController` (SSE `text/event-stream` + isolation workspace), `PartialJsonSectionExtractor`. Le worker conclusions (`@RabbitListener` → `generate()`) appelle `analyzeWithSystemCache` (non-streamé) — point unique à basculer.
- **Aval** : **aucun changement de contenu** — le résultat final est identique (mêmes gardes F-271/261/bordereau/numérotation/placeholders). Le streaming est une amélioration de **latence perçue** + UX, pas de fond. La finalisation (`finalize` : content + bordereau déterministe) reste inchangée à la fin du flux.
- **Anti-gadget** : ce n'est pas un gadget — la latence perçue est un frein d'adoption réel (signal PO). Le temps total reste borné par la longueur de l'acte (incompressible), mais le **perçu** chute (premier texte en ~2-3 s).
- **Verdict** : **GO.**

## Étape 0 bis — cohérence écran (la barre de progression est un élément visible)
- **Parcours** : page conclusions (F-267) → clic **Régénérer** → état **PROCESSING**. Aujourd'hui = spinner + texte « Génération en cours… » (polling 3 s). **Cible** : l'**acte se construit en direct** dans la feuille + une **barre de progression** en tête de la zone de génération.
- **Barre « précise »** : 
  - **pourcentage** = tokens reçus / `MAX_TOKENS` (réel, plafonné à 100 % à la fin) ;
  - **libellé de section en cours** = dernier titre `##`/`###` détecté dans le flux partiel (« Rédaction : DISCUSSION — III. Insubordination ») + compteur indicatif (« section 8 ») ;
  - **état terminal** : disparaît à `DONE`, l'acte final s'affiche (avec bordereau).
- **Charge écran** : la barre **remplace** le spinner muet (pas d'ajout net) ; l'acte qui coule occupe la place de la feuille (déjà prévue).
- **Design** : `mat-progress-bar` stylé **design system** (piste marine `#1A3A5C`, remplissage or `#C9973A`), libellé Inter, hauteur confortable, **visible** (pas un filet de 2px). Animation fluide, pas de saut brutal.
- **Continuité** : à `DONE`, transition douce barre → acte final ; en cas d'échec (`FAILED`), la barre laisse place au message d'erreur existant.
- **Verdict** : **GO avec ajustements** (soigner la barre + le libellé de section ; transition fin ; fallback si le navigateur perd le SSE → repli sur le polling actuel).

## Invariants
1. **Contenu final inchangé** (le streaming ne change pas l'acte produit ; mêmes gardes ; bordereau toujours assemblé déterministiquement à la fin).
2. **Réutiliser l'infra F-185** (SSE + registry + variante streaming), pas de pipeline parallèle à maintenir.
3. **Isolation workspace** sur le nouvel endpoint SSE (même contrat que `analysis-status/stream`).
4. **Fallback** : si le SSE échoue/expire, repli transparent sur le **polling existant** (jamais d'écran bloqué).
5. **Barre précise mais honnête** : % basé sur tokens reçus, jamais de fausse progression ; section = dernier titre réellement détecté.
6. **Design system** : barre marine/or, visible, animée, accessible (aria-valuenow).

## Fichiers (prévision)
- **Back** : worker `CaseConclusionService.generate` → variante streaming ; `ConclusionStreamEmitterRegistry` (ou réutilise `SseEmitterRegistry`) ; nouveau `ConclusionStreamController` (`GET /case-files/{id}/conclusions/stream`) ; calcul progression (tokens + section).
- **Front** : `conclusions-section` (souscription SSE pendant PROCESSING, rendu live, barre + libellé) ; nouveau composant `conclusion-generation-progress` ; service de stream.

## Décision finale
**GO.** Streaming des conclusions (réutilise F-185) + barre de progression précise (tokens % + section), design system, fallback polling. Étape 0 bis GO avec ajustements (soin de la barre).
