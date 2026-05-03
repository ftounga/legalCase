# Mini-spec — F-120 / SF-120-05 Scheduler quotidien + circuit breaker + cleanup orphelins

## Identifiant

`F-120 / SF-120-05`

## Statut

`draft`

## Date de création

2026-05-03

## Branche Git

`feat/SF-120-05-scheduler-circuit-breaker`

---

## Objectif

Orchestrer la production automatique d'articles via un `@Scheduled` quotidien
qui appelle SF-120-03 (génération Claude) puis SF-120-04 (image hero), borné par
un **circuit breaker journalier/hebdomadaire** et complété par un **cleanup horaire**
des réservations orphelines (`BlogTopic` `RESERVED > 1h` sans suite).

---

## Comportement attendu

### Cas nominal

1. **Cron quotidien** (`@Scheduled(cron="${blog.scheduler.cron:0 0 8 * * *}")`) — par défaut tous les jours à 8h UTC.
2. **Feature flag** : si `blog.scheduler.enabled=false` → no-op (par défaut désactivé pour prod, démarré manuellement par super-admin).
3. **Circuit breaker** :
   - Si articles publiés/jour ≥ 3 → arrêt jour.
   - Si articles publiés sur 7 jours glissants ≥ 15 → arrêt semaine.
   - Sinon → continue.
4. **Sélection topic** : `TopicSelectorService.selectNext()` (SF-120-02).
5. **Génération article** : `BlogArticleGeneratorService.generateForTopic(topicId)` (SF-120-03).
6. Si statut = `DRAFT` :
   - **Génération hero image** : `BlogHeroImageService.generateForArticle(articleId)` (SF-120-04). Échec → article publié sans hero.
   - **Auto-publication** : `status = PUBLISHED`, `publishedAt = NOW()`.
7. Si statut = `NEEDS_REVIEW` : aucune image générée (cf. `BlogHeroImageService` skip), article **reste en `NEEDS_REVIEW`** pour relecture super-admin (SF-120-08).
8. **Log structuré** : un seul article par run pour pouvoir tracer les coûts.

### Cleanup orphelins

- **Cron horaire** (`@Scheduled(fixedDelayString="${blog.cleanup.fixed-delay-ms:3600000}")`) — toutes les heures.
- Sélectionne tous les `BlogTopic` avec `status=RESERVED` et `used_at < now() - 1h`.
- Pour chacun → `releaseReservation(topicId)` (passe à `AVAILABLE`).
- Log INFO si > 0 libération.

### Alerte email coût (Garde-fou n° 6)

- **Cron quotidien** (`@Scheduled(cron="${blog.cost-alert.cron:0 0 9 * * *}")`) — chaque matin 9h UTC après le scheduler.
- Calcule le coût mensuel cumulé Anthropic + OpenAI projeté en fin de mois (linéaire).
- Si projection > 15 € (Anthropic) ou > 10 € (OpenAI) → email super-admin via `JavaMailSender` existant.
- Si email non configuré → log WARN.

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| Aucun topic AVAILABLE | Log INFO "no topic to process", skip silencieux |
| Génération budget bloquée | Log WARN, run skip — sera retenté demain |
| Génération définitivement échouée | Log WARN, topic libéré (déjà fait par BlogArticleGeneratorService), run skip |
| Hero image échouée | Article publié sans hero (continue) |
| Exception non gérée | Try/catch global au niveau du scheduler — le bug d'un run ne doit pas tuer le bean |

---

## Analyse de cohérence transversale

- [x] **Autres outils métier** — N/A (orchestration blog).
- [x] **Autres pays** — Le scheduler ne fait pas de distinction pays (le quota 70/30 est appliqué côté `TopicSelectorService` SF-120-02).
- [x] **Autres domaines** — Idem (60/25/15 côté SF-120-02).
- [x] **Autres flows transversaux** — Réutilise `JavaMailSender` (alerte email) et le pattern `@Scheduled` Spring déjà utilisé pour `BacklogSyncService`.

### Cas spécifique : nouveau pattern UI ou service partagé

Pas de nouveau pattern — un scheduler trivial + circuit breaker spécifique au blog. `BlogCircuitBreaker` n'a aucun équivalent à harmoniser.

### Décision

- [x] Étendu à toutes les cibles applicables.
- [x] Pas de SF parallèle.

---

## Impact par domaine métier

Transversal aux 3 domaines × 2 pays via SF-120-02. Pas d'asymétrie.

---

## Critères d'acceptation

- [ ] `BlogPublicationScheduler` avec `@Scheduled(cron=...)` configurable.
- [ ] Feature flag `blog.scheduler.enabled` (default `false`) — no-op si désactivé.
- [ ] `BlogCircuitBreaker` : `canGenerateNow(): CircuitBreakerVerdict` retourne `OK / DAILY_LIMIT / WEEKLY_LIMIT`.
- [ ] Limites journalières et hebdomadaires configurables (`blog.scheduler.daily-cap=3`, `blog.scheduler.weekly-cap=15`).
- [ ] Auto-publication : article `DRAFT` → `PUBLISHED` avec `publishedAt=now()`.
- [ ] `NEEDS_REVIEW` reste en l'état (jamais auto-publié).
- [ ] `OrphanReservationCleaner` libère les RESERVED > 1h (delay configurable).
- [ ] Alerte coût quotidienne avec seuils configurables (Anthropic 15 €, OpenAI 10 €).
- [ ] Try/catch global dans le scheduler pour résilience.
- [ ] Logs structurés (un run = un INFO de bilan).

---

## Périmètre

### Hors scope

- Endpoints super-admin (couverts SF-120-08).
- Frontend (couvert SF-120-06).
- Notification user final (le blog est public, pas de notification).
- Métriques Prometheus / Grafana (V2).
- Système de tags / catégories (déjà en place via SF-120-01).

---

## Technique

### Configuration `application.yml`

```yaml
blog:
  scheduler:
    enabled: ${BLOG_SCHEDULER_ENABLED:false}
    cron: ${BLOG_SCHEDULER_CRON:0 0 8 * * *}
    daily-cap: ${BLOG_SCHEDULER_DAILY_CAP:3}
    weekly-cap: ${BLOG_SCHEDULER_WEEKLY_CAP:15}
  cleanup:
    fixed-delay-ms: ${BLOG_CLEANUP_FIXED_DELAY_MS:3600000}
    reservation-stale-after-ms: ${BLOG_RESERVATION_STALE_AFTER_MS:3600000}
  cost-alert:
    cron: ${BLOG_COST_ALERT_CRON:0 0 9 * * *}
    anthropic-eur: ${BLOG_COST_ALERT_ANTHROPIC_EUR:15}
    openai-eur: ${BLOG_COST_ALERT_OPENAI_EUR:10}
    super-admin-email: ${BLOG_SUPER_ADMIN_EMAIL:ntounga@gmail.com}
```

### Classes Spring à créer

| Classe | Package | Rôle |
|--------|---------|------|
| `BlogPublicationScheduler` | `fr.ailegalcase.blog.scheduler` | `@Scheduled` cron quotidien |
| `BlogCircuitBreaker` | `fr.ailegalcase.blog.scheduler` | Vérifie limites journalière + hebdomadaire |
| `OrphanReservationCleaner` | `fr.ailegalcase.blog.scheduler` | `@Scheduled` horaire, libère RESERVED orphelins |
| `BlogCostAlertJob` | `fr.ailegalcase.blog.scheduler` | `@Scheduled` quotidien, alerte si projection > seuil |
| `CircuitBreakerVerdict` (sealed) | `fr.ailegalcase.blog.scheduler` | `Ok / DailyLimit / WeeklyLimit` |

### Repository à étendre

`BlogTopicRepository` :
```java
List<BlogTopic> findAllByStatusAndUsedAtBefore(BlogTopicStatus status, Instant before);
```

`BlogArticleRepository` :
```java
long countByStatusAndPublishedAtAfter(BlogArticleStatus status, Instant after);
```

---

## Plan de test

### Tests unitaires

- [ ] `BlogCircuitBreaker.canGenerateNow()` : 0 article → OK
- [ ] `BlogCircuitBreaker` : 3 publiés aujourd'hui → DAILY_LIMIT
- [ ] `BlogCircuitBreaker` : 14 publiés cette semaine → OK ; 15 → WEEKLY_LIMIT
- [ ] `OrphanReservationCleaner` : libère les RESERVED > 1h, ignore les < 1h
- [ ] `BlogPublicationScheduler` : feature flag off → no-op
- [ ] `BlogPublicationScheduler` : circuit breaker DAILY_LIMIT → skip silencieux
- [ ] `BlogPublicationScheduler` : aucun topic AVAILABLE → skip silencieux
- [ ] `BlogPublicationScheduler` : DRAFT → hero image OK → auto-publie en PUBLISHED
- [ ] `BlogPublicationScheduler` : DRAFT → hero image échoue → publie sans hero (PUBLISHED, heroUrl=null)
- [ ] `BlogPublicationScheduler` : NEEDS_REVIEW → ne déclenche pas hero image, reste NEEDS_REVIEW
- [ ] `BlogPublicationScheduler` : exception générique → catché, ne crash pas
- [ ] `BlogCostAlertJob` : projection sous seuil → pas d'email
- [ ] `BlogCostAlertJob` : projection > seuil → email envoyé

### Tests d'intégration

- [ ] Déploiement Spring : les 3 cron-tasks apparaissent dans le scheduler context
- [ ] Feature flag `blog.scheduler.enabled=false` (default) → aucun run quand le timer tick

---

## Dépendances

- SF-120-01 ✅ done
- SF-120-02 ✅ done
- SF-120-03 (PR #787) ✅ done
- SF-120-04 (PR #789) ✅ done
