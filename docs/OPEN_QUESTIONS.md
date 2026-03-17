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

## Conventions de nommage
~~À stabiliser quand le code prend forme~~
**Tranchée le 2026-03-17** — Défini dans `project-governance/playbooks/coding-rules.md` :
- Packages Java : `fr.ailegalcase.{workspace,casefile,document,analysis,auth,billing,shared}`
- Nommage Angular : kebab-case composants, PascalCase classes/services
- Conventions SQL : déjà posées dans ARCHITECTURE_CANONIQUE
