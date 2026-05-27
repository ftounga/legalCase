# Mini-spec — F-JU-04 / SF-JU-04-02 Client BE via Anthropic web_search

## Identifiant

`F-JU-04 / SF-JU-04-02`

## Date de création

2026-05-27

## Branche Git

`feat/SF-JU-04-02-be-web-search`

---

## Objectif

Intégrer une source de jurisprudence belge dans le bootstrap F-JU-01, **sans dépendre d'une API officielle BE qui n'existe pas** (pas d'équivalent PISTE en Belgique en 2026 — cf. mémoire `reference_be_jurisprudence_sources`). Utiliser l'outil natif **`web_search` server-tool d'Anthropic** pour que Claude interroge directement JUPORTAL / Cassation BE / Cour constitutionnelle BE et retourne un JSON structuré.

Permet de compléter automatiquement les 4 outils BE actuellement sans mapping (`at-fedris-declaration`, `c4-onem-checklist`, `contestation-c4-onem`, `outplacement-be-obligatoire-45`) + tous les outils BE futurs (F-213/F-215/F-219 BE en cours).

---

## Comportement attendu

### Cas nominal

1. Bootstrap utilise une heuristique `isBelgianEntry(entry)` : si `toolId` ou `brancheCalculId` matche `-be-` / `-be` (fin) / `onem` / `fedris` / `juridat` / `juportal` → entrée BE
2. Pour les entrées BE → appel `JurisprudenceBeWebSearchClient.fetchArretsByKeyword(motCle, from, to, 5)` (5 arrêts max)
3. Le client construit un prompt structuré → `AnthropicService.analyzeWithWebSearch(systemPrompt, userMessage, 2000 tokens, 5 web_searches max)`
4. Claude interroge JUPORTAL via `web_search` server-tool (multi-turn géré côté Anthropic, réponse unique)
5. Claude retourne un JSON `{"arrets": [{ref, juridiction, date_arret, numero_pourvoi, lien, chapeau}, ...]}`
6. Le client parse → `List<JudilibreArret>` symétrique au pattern FR (réutilise le même type pour Claude evaluator)
7. La suite est inchangée : Claude evaluator → `mappingRepository.save()` → audit log

### Cas d'erreur

| Situation | Comportement |
|-----------|--------------|
| Query vide | Log WARN + retour `List.of()` (no-op) |
| Anthropic API down (HttpServerErrorException) | Retry exponentiel sur 5xx (existant), sinon log WARN + retour vide |
| Réponse Claude sans JSON valide | Parse fail → log WARN + retour vide |
| `arrets: []` (Claude n'a rien trouvé) | Log INFO + retour vide |
| `web_search` désactivé ou indisponible côté API | Anthropic retourne erreur → catché → retour vide |

---

## Critères d'acceptation

- [ ] `AnthropicService.analyzeWithWebSearch(systemPrompt, userMessage, maxTokens, maxWebSearches)` POST `/v1/messages` avec `tools: [{"type": "web_search_20250305", "name": "web_search", "max_uses": N}]`
- [ ] La réponse Anthropic est parsée en concaténant **uniquement** les blocs `content[]` de type `text` (filtrant `server_tool_use` / `web_search_tool_result`)
- [ ] `JurisprudenceBeWebSearchClient.fetchArretsByKeyword(query, from, to, limit)` a la **même signature** que `JudilibreApiClient.fetchArretsByKeyword` pour le routage
- [ ] `JurisprudenceBootstrapService.isBelgianEntry(entry)` détecte BE via patterns `-be-` / fin `-be` / `onem` / `fedris` / `juridat` / `juportal`
- [ ] Routage dans `runBootstrap` : BE → `beWebSearchClient`, FR → `judilibreClient` (inchangé)
- [ ] Anti-régression : 43/43 tests verts sur le module `jurisprudencemapping`
- [ ] Aucune modification du contrat HTTP du bootstrap (frontend non touché)

---

## Hors scope

- **Affichage frontend différencié FR/BE** selon `workspaceCountry` → SF-JU-04-03 séparée
- **Cron veille mensuelle BE** (analogue SF-JU-01-02) → SF-JU-04-04 si signal terrain
- **Migration `pays` dans `tool_jurisprudence_mappings`** → différée (le champ `juridiction` text peut suffire V1)

---

## Technique

| Fichier | Modification |
|---------|--------------|
| `AnthropicService.java` | + méthode `analyzeWithWebSearch` (multi-turn server-tool) |
| `JurisprudenceBeWebSearchClient.java` (nouveau) | Client BE, prompt système ciblé JUPORTAL/Cassation BE, parsing JSON |
| `JurisprudenceBootstrapService.java` | + `isBelgianEntry(entry)` + routage dans la boucle |
| `JurisprudenceBeWebSearchClientTest.java` (nouveau) | 6 UT (empty, parsing valide, empty arrets, garbage, exception, skip ref vide) |
| `JurisprudenceBootstrapServiceTest.java` | + 3 UT (`isBelgianEntry`, routage BE, routage FR) |

Aucune migration DB. Aucun changement de schéma.

---

## Notes

- **Coût LLM modéré** : ~5 web_searches max par entrée BE, ~2000 tokens output. Sur les 7 entrées BE du CSV bootstrap-batch-1.csv, coût estimé ~5-10 € total. Acceptable.
- **Risque robustesse** : si Anthropic retire ou modifie `web_search_20250305`, il faudra basculer sur une nouvelle version (`web_search_20XX`) — changement minime de la chaîne version dans le tool spec.
- **Si JUPORTAL change** : Claude gère le HTML changeant nativement via son interprétation — pas de scraping côté nous.
- **À surveiller** : disponibilité du Central register BE (loi 2022) qui pourrait offrir une vraie API publique 2026/2027. Si ouverture → migration triviale (remplacer le client web_search par un client REST classique).
