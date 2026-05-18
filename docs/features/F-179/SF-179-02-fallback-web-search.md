# Mini-spec — [F-179 / SF-179-02] Backend — fallback web search Légifrance / Juridat

> Mini-spec produite via `ai-skills/story-writer.md`. À valider avant dev.

---

## Identifiant

`F-179 / SF-179-02`

## Feature parente

`F-179` — Vérification de jurisprudence citée dans les documents uploadés (FR + BE)

## Statut

`ready`

## Date de création

2026-05-18

## Branche Git

`feat/SF-179-02-fallback-web-search`

---

## Objectif

Quand Sonnet renvoie un statut incertain pour une référence jurisprudentielle (`UNCERTAIN`, ou `NOT_FOUND` avec confiance non `HIGH`), tenter une recherche web publique (Légifrance pour la France, Juridat pour la Belgique) pour confirmer l'existence de l'arrêt, en tolérant tout échec (timeout / erreur HTTP) par un repli sur `UNCERTAIN`.

---

## Comportement attendu

### Cas nominal

1. Après la persistance initiale des `jurisprudence_checks` par `JurisprudenceVerificationService` (SF-179-01), pour chaque check dont le statut justifie une vérification complémentaire :
   - `UNCERTAIN` → web search systématique ;
   - `NOT_FOUND` avec `claudeConfidence` ≠ `HIGH` → web search (un faux `NOT_FOUND` discréditerait l'outil) ;
   - `VERIFIED` / `SUSPECT` / `NOT_FOUND` avec `HIGH` → pas de web search (Claude est fiable, économie de coût).
2. `WebSearchService.searchJurisprudence(reference, country)` est appelé : country `FRANCE` → Légifrance, `BELGIQUE` → Juridat. Le pays cible est déduit du **format de la référence** (ex. `Trib. trav.`, `Cour const. BE` → BE ; `Cass. soc.`, `CE` → FR), avec repli sur le pays du workspace si le format est ambigu.
3. Le service exécute une requête HTTP (API ou recherche web) avec **timeout court** (ex. 8 s), **un retry** avec backoff exponentiel sur erreur réseau / 5xx, et **rate limiting** (au plus N requêtes / dossier — borne dure, ex. 10).
4. Résultat du web search :
   - arrêt trouvé en ligne → check mis à jour : si statut était `UNCERTAIN`/`NOT_FOUND`, il passe `VERIFIED` (existence confirmée), `sourceUrl` renseigné, `webSearchUsed = true`. La fidélité de la position n'est PAS re-jugée par le web search (Sonnet reste seul juge de `SUSPECT`) ;
   - arrêt confirmé introuvable (résultat vide non ambigu) → check reste/passe `NOT_FOUND`, `webSearchUsed = true` ;
   - timeout / erreur HTTP / résultat ambigu → check passe/reste `UNCERTAIN`, `webSearchUsed = true`, `sourceUrl` null.
5. Les checks mis à jour sont re-persistés. L'ensemble du bloc web search est **fail-open** : une exception globale laisse les checks dans l'état SF-179-01.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Timeout HTTP Légifrance / Juridat | Check → `UNCERTAIN`, `webSearchUsed = true`, jamais d'échec d'analyse | — |
| Erreur HTTP 5xx | 1 retry backoff exponentiel ; si échec persistant → `UNCERTAIN` | — |
| Erreur HTTP 4xx (auth, quota API) | Pas de retry, log warn, check → `UNCERTAIN` | — |
| Rate limit dossier atteint | Les checks restants ne sont pas web-searchés, restent en l'état SF-179-01 | — |
| Réponse non parsable | Check → `UNCERTAIN`, log warn | — |
| Exception globale du bloc web search | Fail-open : checks inchangés (état SF-179-01), `CaseAnalysis` reste `DONE` | — |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : aucun outil décisionnel impacté — SF backend pure, prolongement de SF-179-01.
- [x] **Autres pays** : FR (Légifrance) + BE (Juridat) — c'est précisément le point d'adaptation par pays de F-179. Le pays cible est déduit du format de la référence.
- [x] **Autres domaines** : transversal — aucune adaptation métier.
- [x] **Autres UI patterns** : aucun (SF backend pure).
- [x] **Autres flows transversaux** : aucun nouveau flow auth/workspace/plan/navigation. Le `WebSearchService` est un service applicatif partagé potentiel — voir section dédiée.

### Cas spécifique : nouveau pattern UI ou service partagé

`WebSearchService` est un **service applicatif potentiellement réutilisable**.

- [x] **Où le service pourrait-il être réutilisé ?** `F-120 SF-120-04` (vérification des articles de blog générés par l'IA — concept de vérification de sources externes similaire). Le `PRODUCT_SPEC.md` de F-179 laisse explicitement le choix : service générique réutilisable OU appel direct WebFetch tant qu'un seul use case.
- [x] **Patterns concurrents existants ?** Aucun service de web search dans le codebase actuel (vérifié — pas de `WebSearchService` ni d'appel WebFetch sortant existant).
- [x] **Classement** : on crée un **`WebSearchService` dédié à F-179**, dans le package `analysis`, avec une API simple (`searchJurisprudence(reference, country)`). Il n'est PAS sur-généralisé en V1 (un seul use case réel). Si F-120 SF-120-04 démarre, sa généralisation sera tracée à ce moment — pas de spéculation anticipée (cohérent avec la consigne F-179 « plus simple tant qu'un seul use case »).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Légifrance (FR) | Oui | Intégré dans cette SF. |
| Juridat (BE) | Oui | Intégré dans cette SF. |
| F-120 SF-120-04 (vérif blog) | Oui — futur | Backlog : `WebSearchService` sera généralisé si/quand SF-120-04 démarre. Pas de sur-conception V1. |
| Outils décisionnels | Non | F-179 n'est pas un outil décisionnel. |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (FR Légifrance + BE Juridat).
- [x] Backlog : généralisation `WebSearchService` pour F-120 SF-120-04 si ce use case démarre — non prioritaire, tracé.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF backend pure, aucun composant frontend décisionnel.

---

## Critères d'acceptation

- [ ] Quand un check est `UNCERTAIN` après SF-179-01, un web search est déclenché et le check est mis à jour (`VERIFIED` / `NOT_FOUND` / `UNCERTAIN`) avec `webSearchUsed = true`.
- [ ] Quand un check est `NOT_FOUND` avec `claudeConfidence = HIGH`, aucun web search n'est déclenché (économie de coût).
- [ ] Quand le web search trouve l'arrêt en ligne, le check passe `VERIFIED` et `sourceUrl` est renseigné.
- [ ] Quand le web search timeout ou renvoie une erreur HTTP, le check est `UNCERTAIN`, `webSearchUsed = true`, et l'analyse reste `DONE` (fail-open vérifié par test avec mock HTTP).
- [ ] Une référence au format belge (`Trib. trav.`, `Cour const. BE`) déclenche une recherche Juridat ; une référence FR (`Cass. soc.`, `CE`) déclenche Légifrance.
- [ ] Le rate limit dossier est respecté : au-delà de N web searches, les checks restants ne sont pas interrogés (test unitaire).
- [ ] Le web search ne re-juge jamais la fidélité de la position : un check `SUSPECT` (SF-179-01) reste `SUSPECT`.

---

## Périmètre

### Hors scope (explicite)

- Re-vérification de la fidélité de la position alléguée par le web search — Sonnet (SF-179-01) reste seul juge de `SUSPECT`. Le web search ne statue que sur l'**existence**.
- Affichage frontend du `sourceUrl` → SF-179-03.
- Généralisation du `WebSearchService` à d'autres use cases (F-120 SF-120-04) → backlog.
- Aucune nouvelle migration : le schéma `jurisprudence_checks` (colonnes `source_url`, `web_search_used`) est créé en SF-179-01.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `web_search_used` | passe `true` | dès qu'un web search est tenté pour ce check (succès ou échec) |
| `source_url` | renseigné | uniquement si le web search trouve l'arrêt en ligne |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `source_url` | Non | 1000 | URL absolue (`https://...`) | Non | trim ; rejet si non `http(s)` |
| timeout HTTP | — | — | borné (constante, ex. 8 s) | — | — |
| rate limit / dossier | — | — | borné (constante, ex. 10 web searches max) | — | — |

Notes :
- Les endpoints / clés Légifrance API PISTE et le mode d'accès Juridat (API ou scraping HTML) sont des **constantes de configuration** (`application.yml`) — si une clé API est requise et indisponible en environnement de test, le service tombe en `UNCERTAIN` proprement (couvert par les tests avec mock HTTP).

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint exposé — SF-179-02 enrichit les données lues par le `GET .../jurisprudence-checks` de SF-179-01. Le contrat API reste inchangé (`sourceUrl` et `webSearchUsed` sont déjà dans le DTO depuis SF-179-01).

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `jurisprudence_checks` | UPDATE | Mise à jour de `statut`, `source_url`, `web_search_used` après web search. |

### Migration Liquibase

- [x] Non applicable — schéma créé en SF-179-01 (migration 245).

### Composants Java

- `WebSearchService` — `searchJurisprudence(String reference, String country)` → résultat (`FOUND` + url / `NOT_FOUND` / `UNCERTAIN`). Encapsule `RestClient`, timeout, retry backoff, rate limit.
- `WebSearchResult` — record (`outcome`, `sourceUrl`).
- Extension de `JurisprudenceVerificationService` : après la persistance initiale, boucle de web search sur les checks éligibles, mise à jour fail-open.
- Configuration `application.yml` : base URLs Légifrance / Juridat, timeout, rate limit, éventuelle clé API.

### Composants Angular

- Aucun.

---

## Plan de test

### Tests unitaires

- [ ] `WebSearchService` — référence FR → URL Légifrance interrogée (mock `RestClient`).
- [ ] `WebSearchService` — référence BE → URL Juridat interrogée.
- [ ] `WebSearchService` — timeout HTTP → résultat `UNCERTAIN` (pas d'exception propagée).
- [ ] `WebSearchService` — 5xx puis succès → retry effectué, résultat `FOUND`.
- [ ] `WebSearchService` — rate limit atteint → requêtes suivantes non émises.
- [ ] `JurisprudenceVerificationService` — check `UNCERTAIN` + web search `FOUND` → statut `VERIFIED`, `sourceUrl` rempli, `webSearchUsed = true`.
- [ ] `JurisprudenceVerificationService` — check `NOT_FOUND` confiance `HIGH` → aucun web search.
- [ ] `JurisprudenceVerificationService` — check `SUSPECT` → web search ne change pas le statut.
- [ ] `JurisprudenceVerificationService` — exception globale du bloc web search → checks inchangés (fail-open).

### Tests d'intégration

- [ ] Analyse complète avec mock HTTP web search → checks enrichis correctement persistés, lisibles via `GET .../jurisprudence-checks`.

### Isolation workspace

- [x] Non applicable directement (pas de nouvel endpoint) — l'isolation est portée par le `GET` de SF-179-01. Les UPDATE se font sur des checks déjà rattachés au bon `workspace_id`.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — SF backend, prolongement de SF-179-01. Nouveau service HTTP sortant (`WebSearchService`), sans impact auth/workspace/plan/navigation.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `JurisprudenceVerificationService` | Bloc web search ajouté après la persistance SF-179-01. | Test fail-open : exception web search → checks inchangés. |
| Réseau sortant | Nouvel appel HTTP externe (Légifrance / Juridat). | Timeout borné + rate limit + fail-open. |

### Coût IA estimé

Le web search **n'appelle pas de modèle IA** (pas de coût token). Coût marginal = appels HTTP publics, déclenchés uniquement sur incertitude Claude (rare). Impact sur le coût total F-179 : négligeable. Total F-179 reste ≤ 0,10 €/dossier.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — pas de route, pas de guard, pas d'auth/workspace touché.

---

## Dépendances

### Subfeatures bloquantes

- `SF-179-01` — doit être `done` (table `jurisprudence_checks`, `JurisprudenceVerificationService`).

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

- **Choix d'implémentation web search** : `WebSearchService` dédié (pas WebFetch inline) — anticipe la réutilisation F-120 SF-120-04 sans la sur-concevoir. API minimale ; généralisation tracée au backlog.
- Le web search statue **uniquement sur l'existence** — jamais sur la fidélité. Cela préserve l'invariant : Sonnet seul détecte `SUSPECT` (la mauvaise foi adverse).
- Tolérance d'échec stricte : tout incident HTTP → `UNCERTAIN`. Conforme à l'invariant anti-gadget 4 du cadrage étape 0.
- Pas de migration : `source_url` et `web_search_used` sont déjà dans la table (245).
