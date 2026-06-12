# SF-286-01 — Stratégie de dossier unifiée

**Feature parente** : F-286 — Stratégie de dossier unifiée
**Type** : nouvelle capacité (back + front), couche LLM de synthèse en lecture
**Étapes 0 / 0 bis** : GO (`SF-286-00-coherence.md`, `SF-286-00b-ux-coherence.md`)

---

## Objectif (une phrase)

Offrir à l'avocat, dans l'onglet Décision, une **recommandation stratégique consolidée et lisible**
(voie référé/fond, posture concilier-transiger/plaider, priorisation des chefs de demande, séquencement),
**générée par un appel LLM en lecture** des verdicts des outils décisionnels **calculés** + de la synthèse,
**sans modifier aucun outil**.

---

## Comportement nominal

1. L'avocat ouvre l'onglet **Décision** d'un dossier. La carte **« Stratégie de dossier »** coiffe la colonne
   verdict (au-dessus du tableau de bord décisionnel).
2. Au chargement, le front appelle `GET /api/v1/case-files/{caseFileId}/strategy` :
   - si une stratégie a déjà été générée → affichée (texte structuré + date de génération + nb de verdicts pris
     en compte) ;
   - sinon → état initial avec CTA « Générer la stratégie ».
3. L'avocat clique « Générer la stratégie » → `POST /api/v1/case-files/{caseFileId}/strategy` :
   - le backend agrège les **verdicts d'outils CALCULÉS** (`assembleDecisionToolTiles`) + la **synthèse `DONE`**
     la plus récente + les **pistes stratégiques RETAINED** ;
   - construit le prompt `SYSTEM_CASE_STRATEGY` (system + user), appelle `AnthropicService.analyze`
     (Sonnet, temperature 0, gate F-257 via `AiCallContext.systemLevel(SYSTEM_CASE_STRATEGY, caseFileId)`) ;
   - persiste le résultat (texte markdown) dans `case_strategy` (1 ligne courante par dossier, upsert) avec
     `generated_at`, `tools_considered`, `model_used`, tokens ;
   - renvoie la stratégie générée.
4. La reco s'affiche : sections markdown rendues (voie procédurale, posture, priorisation des chefs,
   séquencement), date de génération (JetBrains Mono), badge « N verdict(s) pris en compte ».
5. L'avocat peut **régénérer** (le dossier vit, les verdicts changent) → re-POST, upsert.

## Honnêteté du vide (reprise F-258 — POINT DUR)

- Si **0 outil calculé** au moment du POST : le backend **ne génère pas** de stratégie inventée. Il renvoie un
  statut `EMPTY_INPUT` (200, corps `{ status: "EMPTY_INPUT", toolsConsidered: 0, ... }`) et le front affiche
  l'encart « Calculez vos outils décisionnels pour obtenir une recommandation stratégique fondée » + le nombre
  d'outils restant à calculer (déjà connu côté front via le panel décisionnel / dashboard).
- Le CTA « Générer la stratégie » reste cliquable (il déclenche le POST qui répond EMPTY_INPUT proprement) mais
  l'encart explicatif est montré tant que `toolsConsidered === 0`.

## Cas d'erreur

| Cas | Comportement |
|---|---|
| Dossier inexistant / autre workspace | 404 `Case file not found` (isolation `resolveCaseFileForUser`) |
| Échec appel Anthropic | 502/500 → front : snackbar erreur, la dernière stratégie persistée reste affichée |
| Gate token dépassé (système : skip user gate) | system-level → pas de gate user ; record obligatoire userId=null |
| 0 outil calculé | 200 `EMPTY_INPUT` (pas d'appel LLM), encart honnête |
| Pas de synthèse `DONE` mais des outils calculés | génération sur la base des seuls verdicts (synthèse = section vide dans le prompt) |

---

## Contrat API (FIGÉ)

### `GET /api/v1/case-files/{caseFileId}/strategy`
Réponse `200` — `CaseStrategyResponse` :
```json
{
  "status": "READY | EMPTY | EMPTY_INPUT",
  "content": "string|null",            // markdown de la reco (null si jamais généré)
  "generatedAt": "ISO-8601|null",
  "toolsConsidered": 0,                  // nb de verdicts d'outils calculés pris en compte
  "modelUsed": "string|null"
}
```
- `EMPTY` = aucune stratégie encore générée. `READY` = une stratégie persistée existe.

### `POST /api/v1/case-files/{caseFileId}/strategy`
Corps : aucun. Réponse `200` — `CaseStrategyResponse` :
- `READY` si génération réussie (content rempli) ;
- `EMPTY_INPUT` si 0 outil calculé (aucun appel LLM, content null).

Isolation : `OidcUser` + `Principal` → `resolveUser` → `resolveCaseFileForUser` (mirror Contradictoire).

---

## Tables / endpoints / composants impactés

### Backend (nouveau)
- **Migration 604** `604-create-case-strategy.xml` — table `case_strategy` (additive).
- `CaseStrategy` (entité), `CaseStrategyRepository`.
- `CaseStrategyService` (agrégation lecture + prompt + appel Anthropic + upsert), `CaseStrategyPromptBuilder`.
- `CaseStrategyController` (GET/POST), `CaseStrategyResponse` (DTO).
- `JobType.SYSTEM_CASE_STRATEGY` (system-level, ajout enum).
- **Réutilise en lecture** : `CaseFileDashboardService.assembleDecisionToolTiles`, `CaseAnalysisRepository`
  (synthèse `DONE`), `AnthropicService.analyze`.

### Frontend (nouveau)
- `app-case-strategy` (standalone, OnPush) : `frontend/src/app/case-files/case-strategy/`.
- `CaseStrategyService` (core) + `case-strategy.model.ts`.
- Branchement dans `case-file-detail.component.html`, colonne `decision-space__verdict`, **au-dessus** de la
  section `decisional-summary-panel`.

### Table `case_strategy` (additive)
| Colonne | Type | Notes |
|---|---|---|
| `id` | uuid PK | |
| `case_file_id` | uuid NOT NULL FK case_files | **unique** (1 stratégie courante / dossier) |
| `content` | text | markdown de la reco |
| `tools_considered` | int NOT NULL default 0 | nb de verdicts calculés pris en compte |
| `model_used` | varchar(100) | |
| `prompt_tokens` / `completion_tokens` | int | |
| `generated_at` | timestamp NOT NULL | |
| `created_at` / `updated_at` | timestamp NOT NULL | |

---

## Critères d'acceptation (vérifiables)

1. `POST` avec ≥1 outil calculé → `200 READY`, `content` non vide, `case_strategy` upserté (1 ligne/dossier).
2. `POST` avec 0 outil calculé → `200 EMPTY_INPUT`, **aucun appel Anthropic** (vérifié par mock non invoqué),
   `content` null.
3. `GET` avant toute génération → `200 EMPTY`. Après génération → `200 READY` avec content + generatedAt.
4. Régénération → upsert (pas de 2ᵉ ligne), `generated_at` mis à jour.
5. Isolation : `GET`/`POST` sur un dossier d'un autre workspace → `404`.
6. Le service **n'injecte aucun repository `*_analysis` en écriture** ; **aucun** INSERT/UPDATE sur
   `decision_tool_visibility_rules` (preuve invariant 1 outil = 1 situation).
7. `AiCallContext` = `SYSTEM_CASE_STRATEGY` system-level → record usage userId=null valide (NOT NULL/varchar OK).
8. Front : carte au-dessus du tableau de bord, états vide / chargement / erreur / READY soignés (gabarit F-282).
9. `npm run build` + `npm test` verts ; `mvn -pl backend test` vert.

## Plan de test minimal

- **Unitaires back** : `CaseStrategyServiceTest` — agrégation (tiles + synthèse), branche EMPTY_INPUT (0 tile →
  pas d'appel mock), upsert (2 POST → 1 ligne), mapping DTO/status. `CaseStrategyPromptBuilderTest` — prompt
  contient les verdicts, exclut toute donnée non calculée.
- **Intégration back** : `CaseStrategyControllerIT` — GET EMPTY → POST READY → GET READY ; isolation workspace 404.
- **Front** : `case-strategy.component.spec.ts` — états (empty/loading/ready/empty_input/error), rendu reco,
  régénération, CTA.
- **Isolation workspace** : couverte par l'IT (dossier d'un autre workspace → 404).

## Hors périmètre

- Pas de modification d'un outil décisionnel, de leur visibilité ou positionnement (invariant dur).
- Pas d'historisation multi-versions de la stratégie (1 ligne courante upsert ; versions = backlog futur si besoin).
- Pas d'injection automatique de la stratégie dans les conclusions (lien léger seulement ; les conclusions sont
  déjà nourries par les mêmes verdicts).
- Pas d'export dédié (la reco est copiable).
- Pas de génération asynchrone RabbitMQ : appel **synchrone** borné (1 appel Sonnet, input plafonné), comme
  HelpChat / Chat.

## Préoccupations transversales — composants impactés (anti-régression)

- **Outil décisionnel métier** : F-286 **observe** les verdicts en lecture (`assembleDecisionToolTiles`).
  Invariant 1 outil = 1 situation **préservé** — aucune écriture sur les `*_analysis` ni la visibilité.
  Composants en lecture seule : `CaseFileDashboardService.assembleDecisionToolTiles`.
- **Navigation / routing** : aucune nouvelle route Angular (la carte vit dans l'onglet Décision existant).
- **Auth / workspace** : aucun nouveau moyen de résolution — réutilise `resolveUser` / `resolveCaseFileForUser`.
- **Plans / limites** : aucun nouveau gate ; `SYSTEM_CASE_STRATEGY` system-level (skip gate user, record obligatoire).
- Smoke E2E : non requis (pas de changement auth/workspace/navigation transversal).
