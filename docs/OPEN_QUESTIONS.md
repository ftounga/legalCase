# OPEN_QUESTIONS.md — Décisions architecturales en suspens

Ces questions sont à trancher au fil du développement, au moment où chaque sujet devient concret.

---

## Provider LLM
~~Quel provider LLM est ciblé ?~~
**Tranché le 2026-03-17** — Anthropic (Claude). SDK Java officiel (`anthropic-sdk-java`).

---

## Système de queue / jobs asynchrones
~~Quel mécanisme pour orchestrer les jobs asynchrones ?~~
**Tranché le 2026-03-17** — RabbitMQ à partir de F-08 (appels LLM).
- `@Async` Spring conservé pour F-06 (extraction) et F-07 (chunking) — traitement local, durée < 5s, pas d'API externe.
- RabbitMQ pour F-08+ — appels Anthropic : latence variable, rate limit, retry nécessaire, concurrence à contrôler.
- Ligne de partage : traitement local rapide → `@Async` / appel LLM externe → RabbitMQ.

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

## Provider de stockage objet (production)

Quel provider S3-compatible sera utilisé en production ?
- **Option A — Scaleway Object Storage** (hébergeur français, RGPD natif, ~0,01€/Go/mois) ← recommandé pour cabinets FR
- **Option B — OVH Object Storage** (hébergeur français, RGPD natif, tarif similaire)
- **Option C — AWS S3** (pay-per-use, région EU possible)
- **Option D — MinIO auto-hébergé** (souveraineté totale, on-premise)

> En développement : MinIO Docker (décidé le 2026-03-17).
> Le code est identique quelle que soit l'option prod — seules les variables d'environnement changent.
> À trancher avant la mise en production.

---

## Déclenchement de la re-synthèse enrichie (F-14)
~~Quand la re-synthèse est-elle déclenchée après les réponses avocat ?~~
**Tranché le 2026-03-18** — Option C : re-synthèse à la demande via bouton "Re-analyser".
- L'avocat répond aux questions à son rythme (sans contrainte de toutes répondre)
- Un bouton "Re-analyser" déclenche manuellement la re-synthèse enrichie
- POST `/api/v1/case-files/{id}/re-analyze` → message RabbitMQ → nouveau job ENRICHED_ANALYSIS

---

## Conventions de nommage
~~À stabiliser quand le code prend forme~~
**Tranchée le 2026-03-17** — Défini dans `project-governance/playbooks/coding-rules.md` :
- Packages Java : `fr.ailegalcase.{workspace,casefile,document,analysis,auth,billing,shared}`
- Nommage Angular : kebab-case composants, PascalCase classes/services
- Conventions SQL : déjà posées dans ARCHITECTURE_CANONIQUE
