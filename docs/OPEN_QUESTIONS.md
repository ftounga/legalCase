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

## Modèle multi-workspace (F-17)
~~Un utilisateur peut-il appartenir à plusieurs workspaces ? Comment est résolu le contexte workspace ?~~
**Tranché le 2026-03-18** — Multi-workspace avec flag `is_primary` sur `workspace_members`.
- À la création de compte : workspace personnel créé, `is_primary = true`
- Lors d'une invitation acceptée : workspace invitant → `is_primary = true`, ancien → `is_primary = false`
- Tous les `findFirstByUser` remplacés par `findByUserAndIsPrimaryTrue`
- Écran de changement de workspace inclus dans F-17 (pas différé en V2)

## Invitation workspace (F-17)
~~L'invitation requiert-elle un compte existant ou un flow email avec token ?~~
**Tranché le 2026-03-18** — Invitation par email avec token (Option B).
- Token généré, email envoyé avec lien vers le frontend (`/invite?token=xxx`)
- Frontend stocke le token, user se connecte via OAuth, frontend appelle `POST /api/v1/workspace/invitations/accept`
- Fonctionne que l'invité ait déjà un compte ou non
- Provider email V1 : à trancher en SF-17-03 (Spring Mail SMTP recommandé)

## Intégration paiement Stripe (F-19)
~~Stripe différé post-V1 ?~~
**Tranché le 2026-03-18** — Stripe intégré en V1 (F-19). Objectif : les utilisateurs peuvent payer dès le déploiement V1.
- Stripe Checkout (hosted) pour FREE→Starter et Starter→Pro
- Webhook Stripe pour mise à jour automatique du `plan_code` en base
- `stripe_customer_id` et `stripe_subscription_id` à ajouter sur la table `subscriptions`
- Clés Stripe test en local, clés prod à configurer avant déploiement

---

## Plan tarifaire — modèle FREE trial (F-16 évolution)
**Tranché le 2026-03-18** — Ajout d'un plan FREE en trial 14 jours.

| Limite | FREE (14j) | Starter | Pro |
|--------|-----------|---------|-----|
| Dossiers actifs | 1 | 3 | 20 |
| Documents par dossier | 3 | 5 | 30 |
| Re-analyse enrichie | Non | Non | Oui |
| Durée | 14 jours | Illimité | Illimité |

- **À la création du workspace** : plan FREE attribué, `expires_at = created_at + 14j`
- **À expiration** : lecture seule des dossiers existants (pas de blocage total), gates sur création/upload/re-analyse
- **Upsell Starter** : proposé à la connexion quand le plan FREE est expiré
- **Upsell Pro** : proposé au moment où l'utilisateur tente de dépasser un quota Starter (sur les 402 actuels)
- **Bannière owner** : affichée une fois par session (localStorage flag), quand le quota est atteint ou proche (≥ 80%)

---

## Conventions de nommage
~~À stabiliser quand le code prend forme~~
**Tranchée le 2026-03-17** — Défini dans `project-governance/playbooks/coding-rules.md` :
- Packages Java : `fr.ailegalcase.{workspace,casefile,document,analysis,auth,billing,shared}`
- Nommage Angular : kebab-case composants, PascalCase classes/services
- Conventions SQL : déjà posées dans ARCHITECTURE_CANONIQUE
