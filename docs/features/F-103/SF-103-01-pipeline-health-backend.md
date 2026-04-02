# SF-103-01 — Santé pipeline IA — backend

## Objectif

Exposer un endpoint super-admin qui retourne l'état temps réel du pipeline IA :
profondeur des queues RabbitMQ (vue globale multi-pod) + statistiques des jobs
depuis PostgreSQL.

---

## Sources de données

| Donnée | Source | Multi-pod |
|--------|--------|-----------|
| Profondeur queues, consumers, messages en transit | RabbitMQ Management HTTP API | ✅ oui |
| Jobs DONE/FAILED/PROCESSING/PENDING | PostgreSQL `analysis_jobs` | ✅ oui |

---

## Comportement nominal

### Endpoint

`GET /api/v1/super-admin/pipeline-health`
- Accès réservé aux `isSuperAdmin = true`
- Retourne `PipelineHealthResponse`

### PipelineHealthResponse

```json
{
  "queues": [
    {
      "name": "chunk.analysis",
      "messagesReady": 0,
      "messagesUnacknowledged": 5,
      "consumers": 10,
      "available": true
    },
    {
      "name": "document.analysis",
      "messagesReady": 0,
      "messagesUnacknowledged": 2,
      "consumers": 10,
      "available": true
    },
    {
      "name": "case.analysis",
      "messagesReady": 0,
      "messagesUnacknowledged": 1,
      "consumers": 6,
      "available": true
    }
  ],
  "jobs": {
    "last24h": { "done": 48, "failed": 0, "processing": 1, "pending": 0 },
    "last7d":  { "done": 320, "failed": 3, "processing": 1, "pending": 0 }
  },
  "rabbitmqAvailable": true
}
```

### RabbitMQManagementClient

- `RestClient` appelant `GET {rabbitmq.management.url}/api/queues/{vhost}/{queue}`
- Auth HTTP Basic avec les credentials RabbitMQ (`rabbitmq.management.username` / `rabbitmq.management.password`)
- **Fail-open** : si l'API est indisponible → `available: false`, `messagesReady: -1`, `rabbitmqAvailable: false`
- Timeout court : 3 secondes (ne doit pas bloquer la page super-admin)

### Statistiques jobs (PostgreSQL)

Requêtes sur `analysis_jobs` :
- `countByStatusAndUpdatedAtAfter(status, since)` pour chaque statut × chaque fenêtre (24h, 7j)
- Tous types de jobs confondus (CHUNK_ANALYSIS + DOCUMENT_ANALYSIS + CASE_ANALYSIS)

---

## Cas d'erreur

- RabbitMQ Management API indisponible → `rabbitmqAvailable: false`, queues avec `available: false`, jobs stats DB retournés normalement
- Timeout RabbitMQ → même comportement (fail-open)
- Utilisateur non super-admin → 403

---

## Critères d'acceptation

- [ ] `GET /api/v1/super-admin/pipeline-health` retourne 200 avec le bon format
- [ ] Les 3 queues sont présentes dans la réponse
- [ ] `messagesReady`, `messagesUnacknowledged`, `consumers` reflètent l'état global RabbitMQ
- [ ] `rabbitmqAvailable: false` si management API inaccessible (pas d'exception levée)
- [ ] `jobs.last24h` et `jobs.last7d` calculés depuis `analysis_jobs.updated_at`
- [ ] 403 si non super-admin

---

## Plan de test

| ID | Cas | Assertion |
|----|-----|-----------|
| U-01 | RabbitMQ disponible → retourne données queues | messagesReady mappé, available=true |
| U-02 | RabbitMQ indisponible → fail-open | rabbitmqAvailable=false, jobs stats présents |
| U-03 | Jobs stats 24h calculés correctement | countByStatus depuis updated_at |
| IT-01 | GET /pipeline-health non super-admin → 403 | status 403 |
| IT-02 | GET /pipeline-health super-admin → 200 | structure JSON correcte |

---

## Composants impactés

- Nouveau : `PipelineHealthResponse`, `QueueHealth`, `JobStats`, `JobCounts`
- Nouveau : `RabbitMQManagementClient`
- Nouveau : `PipelineHealthService`
- Modifié : `SuperAdminController` (nouveau endpoint)
- Modifié : `AnalysisJobRepository` (nouvelles requêtes)
- Config : `rabbitmq.management.url`, `rabbitmq.management.username`, `rabbitmq.management.password`

## Hors périmètre

- Historique / time-series des métriques
- Alerting automatique
- Frontend (SF-103-02)
