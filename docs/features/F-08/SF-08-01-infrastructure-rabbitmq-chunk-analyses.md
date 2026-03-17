# Mini-spec — F-08 / SF-08-01 — Infrastructure RabbitMQ + table chunk_analyses

---

## Identifiant

`F-08 / SF-08-01`

## Feature parente

`F-08` — Analyse IA — chunk

## Statut

`ready`

## Date de création

2026-03-17

## Branche Git

`feat/SF-08-rabbitmq-chunk-analyses`

---

## Objectif

Mettre en place l'infrastructure nécessaire à l'analyse IA des chunks : service RabbitMQ dans docker-compose, configuration Spring AMQP, migration Liquibase pour la table `chunk_analyses`, entité JPA et repository.

---

## Comportement attendu

### Cas nominal

Au démarrage de l'application avec le profil `local`, RabbitMQ est accessible. Spring AMQP déclare automatiquement la queue `chunk.analysis` et l'exchange associé. La table `chunk_analyses` est créée par la migration 010. L'entité `ChunkAnalysis` est persistable via JPA. Le repository expose `findByChunkId()`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Migration déjà appliquée | Liquibase ignore (checksum) | N/A |
| RabbitMQ indisponible au démarrage (profil `dev`) | Application démarre quand même — AMQP désactivé en profil `dev` | N/A |

---

## Critères d'acceptation

- [ ] Service `rabbitmq` ajouté dans `docker-compose.yml` (image `rabbitmq:3-management`, ports 5672 + 15672)
- [ ] Dépendance `spring-boot-starter-amqp` ajoutée dans `pom.xml`
- [ ] Queue `chunk.analysis` déclarée via `@Bean Queue`
- [ ] Exchange direct `chunk.analysis.exchange` déclaré via `@Bean DirectExchange`
- [ ] Binding queue ↔ exchange avec routing key `chunk.analysis` déclaré via `@Bean Binding`
- [ ] Configuration AMQP absente du profil `dev` (H2) — les tests unitaires et d'intégration ne requièrent pas RabbitMQ
- [ ] Table `chunk_analyses` créée par la migration 010
- [ ] FK `chunk_id` → `document_chunks(id)` présente
- [ ] Index sur `chunk_id` présent
- [ ] `ChunkAnalysis` persistable via JPA
- [ ] `findByChunkId()` opérationnel
- [ ] `@PrePersist` / `@PreUpdate` pour `created_at` / `updated_at`

---

## Périmètre

### Hors scope (explicite)

- Publication de messages dans la queue (SF-08-03)
- Consommation de messages (SF-08-03)
- Appel Anthropic (SF-08-02)
- Endpoint API pour consulter les analyses

---

## Technique

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| chunk_analyses | CREATE TABLE | Migration 010 |

### Schéma chunk_analyses

| Colonne | Type | Notes |
|---------|------|-------|
| id | UUID PK | Généré par JPA |
| chunk_id | UUID FK → document_chunks(id) | Non nullable |
| analysis_status | VARCHAR(20) | Enum : PENDING / PROCESSING / DONE / FAILED |
| analysis_result | TEXT | JSON retourné par Anthropic (nullable jusqu'à DONE) |
| model_used | VARCHAR(100) | Ex: `claude-sonnet-4-6` (nullable jusqu'à DONE) |
| prompt_tokens | INTEGER | Nullable jusqu'à DONE |
| completion_tokens | INTEGER | Nullable jusqu'à DONE |
| created_at | TIMESTAMP WITH TIME ZONE | Auto @PrePersist |
| updated_at | TIMESTAMP WITH TIME ZONE | Auto @PrePersist + @PreUpdate |

### Configuration RabbitMQ (application-local.yml)

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

### Configuration absente du profil `dev`

Le profil `dev` (H2) ne doit pas tenter de se connecter à RabbitMQ. La classe de configuration AMQP sera annotée `@Profile("local")`.

### Migration Liquibase

- [ ] `010-create-chunk-analyses.xml`

---

## Plan de test

### Tests d'intégration

- [ ] Couvert par `ChunkAnalysisServiceIT` (SF-08-03) — persistance vérifiée via le repository

### Isolation workspace

- [ ] Non applicable — pas d'endpoint exposé

---

## Dépendances

### Subfeatures bloquantes

- F-07 / SF-07-01 — table `document_chunks` existante — statut : done

### Questions ouvertes impactées

- Système de queue asynchrone : RabbitMQ — tranché le 2026-03-17

---

## Notes et décisions

- Exchange de type `Direct` (pas Topic) — suffisant pour V1, un seul type de message
- Profil `@Profile("local")` sur la configuration AMQP pour ne pas bloquer les tests avec H2
- `analysis_result` est du JSON libre (pas de modèle fixe en V1) — permet d'évoluer le prompt sans migration
- `prompt_tokens` + `completion_tokens` pré-positionnés pour F-15 (suivi consommation LLM)
