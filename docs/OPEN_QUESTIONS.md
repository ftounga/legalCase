# OPEN_QUESTIONS.md — Décisions architecturales en suspens

Ces questions sont à trancher au fil du développement, au moment où chaque sujet devient concret.

---

## Provider LLM
Quel provider LLM est ciblé ?
- OpenAI
- Anthropic (Claude)
- Azure OpenAI
- Autre

---

## Système de queue / jobs asynchrones
Quel mécanisme pour orchestrer les jobs asynchrones ?
- Spring Batch
- RabbitMQ
- Redis (avec Spring Data Redis ou Redisson)
- Autre

---

## Notification de progression (UI)
Comment le frontend Angular est-il notifié de l'avancement des analyses ?
- Polling HTTP
- WebSocket
- SSE (Server-Sent Events)

---

## Isolation multi-tenant SQL
L'isolation par workspace est-elle assurée par :
- Row-Level Security PostgreSQL
- Filtre applicatif Spring (ex: filtre automatique sur `workspace_id`)
- Les deux combinés

---

## Conventions de nommage
À stabiliser quand le code prend forme :
- Structure des packages Java (ex: `fr.ailegalcase.workspace`, `fr.ailegalcase.casefile`...)
- Nommage des composants Angular
- Conventions SQL (déjà posées dans ARCHITECTURE_CANONIQUE)
