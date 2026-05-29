# Mini-spec — F-JU-04 / SF-JU-04-03 — Fiabiliser JurisprudenceBeWebSearchClient

## Identifiant
`F-JU-04 / SF-JU-04-03`

## Date
2026-05-29

## Branche Git
`feat/SF-JU-04-03-harden-be-web-search`

## Type
Bugfix — le client web_search BE (SF-JU-04-02) ne produit aucun arrêt exploitable.

---

## Objectif (1 phrase)
Faire en sorte que `JurisprudenceBeWebSearchClient` retourne effectivement des arrêts belges structurés, en corrigeant le budget `max_tokens` insuffisant qui tronque la réponse web_search avant l'émission du JSON.

## Cause racine (revue de code)
`fetchArretsByKeyword` appelle `anthropic.analyzeWithWebSearch(ctx, SYSTEM_PROMPT, userMessage, MAX_TOKENS=2000, MAX_WEB_SEARCHES=5)`. Avec l'outil `web_search`, les **blocs `web_search_result` comptent dans le budget de sortie** (`max_tokens`). 2000 tokens sont consommés par les résultats de recherche + le raisonnement **avant** que Claude n'émette le JSON `{arrets:[…]}` (jusqu'à 5 arrêts × chapeau ≤ 2000 car. ≈ 2500+ tokens rien que pour les chapeaux). Conséquence : `stop_reason=max_tokens` (cf. garde-fou `AnthropicService` l.358), JSON **tronqué** → `parseArrets` échoue → `[]` → 0 candidat → évaluateur `NONE` → outil skippé.

> Le pilote de 10 outils BE du 2026-05-29 (0 créé) a en outre été **coupé par un rolling update** (commit docs option-A en vol) → verdict non concluant. Cette SF corrige le bug de fond puis un **re-test propre du pilote** (sans déploiement en vol) tranchera.

## Comportement attendu

### Nominal
- Pour un mot-clé BE, le client effectue 1-3 recherches web puis retourne **jusqu'à 3** arrêts belges structurés (ref + juridiction + date + n° de rôle + lien JUPORTAL + chapeau), sans troncature JSON.
- Le bootstrap route déjà les entrées BE vers ce client (`isBelgianEntry`, inchangé).

### Cas d'erreur
| Situation | Comportement |
|---|---|
| Aucun arrêt BE pertinent | `{"arrets": []}` → skip (silence > erreur, inchangé) |
| Réponse non-JSON / tronquée | parse fail → `[]` (inchangé, mais ne devrait plus se produire grâce au budget) |
| Appel Anthropic échoue / timeout | `[]` (try/catch existant) |

## Critères d'acceptation
- [ ] `MAX_TOKENS` relevé de 2000 à **12000** (headroom web_search + JSON 3 arrêts).
- [ ] `MAX_WEB_SEARCHES` ramené de 5 à **3** (borne coût/latence, suffisant pour un sujet ciblé).
- [ ] Prompt : demande **jusqu'à 3** arrêts (au lieu de 5) + chapeau **≤ 600 caractères** (JSON compact, anti-troncature).
- [ ] `parseArrets` tronque défensivement le chapeau à 2000 (inchangé) — borne haute conservée.
- [ ] UT `JurisprudenceBeWebSearchClientTest` : parsing d'un JSON 3 arrêts valide → 3 `JudilibreArret` ; JSON vide → `[]` ; JSON tronqué/invalide → `[]`.
- [ ] Anti-régression : routage `isBelgianEntry` inchangé.

## Plan de test
- **UT** `JurisprudenceBeWebSearchClientTest` (parse 3 arrêts / vide / tronqué) — sans appel réseau (on teste `parseArrets`).
- **Validation opérationnelle** (hors CI) : re-run du `bootstrap-be-pilot.csv` (10 outils) **après déploiement effectif** (pod Ready, aucun déploiement en vol) → inspection d'authenticité des arrêts créés (n° de rôle BE, juridiction, lien JUPORTAL vérifiable) avant tout passage aux ~80 outils.

## Composants impactés
- `backend/.../JurisprudenceBeWebSearchClient.java` (constantes + prompt).
- `backend/.../JurisprudenceBeWebSearchClientTest.java` (UT, nouveau ou étendu).
- **Aucune migration, aucun frontend, aucun changement de `AnthropicService`** (on passe juste un `maxTokens` plus élevé en paramètre).

## Hors périmètre
- Durcissement éventuel de l'évaluateur `ClaudeJurisprudenceEvaluator` pour les arrêts BE (→ SF-JU-04-04 si le re-test montre que les arrêts sont bien retournés mais rejetés à l'évaluation).
- Passage à l'échelle des ~80 outils BE (opérationnel, après validation du pilote).
- Cron veille BE.

## Préoccupations transversales
- **Outil décisionnel métier** : aucun outil ajouté/modifié ; on fiabilise une source de citations. Additif.
- **Plans/limites (gate F-257)** : l'appel reste `SYSTEM_JP_BOOTSTRAP` (déjà géré post-SF-257-02) ; `max_tokens` plus élevé = coût LLM légèrement supérieur par recherche BE, borné par `MAX_WEB_SEARCHES=3` et top-3.
- Auth/workspace/navigation : non concernés.
