# Mini-spec — F-178 / SF-178-02 Sync auto (cron + startup)

## Identifiant

`F-178 / SF-178-02`

## Feature parente

`F-178` — Visualiseur de backlog dans super-admin

## Statut

`ready`

## Date de création

2026-05-02

## Branche Git

`feat/SF-178-02-sync-auto`

---

## Objectif

Automatiser la sync MD→DB via 2 mécanismes : un `@Scheduled` cron toutes les 5 min et un hook de démarrage `ApplicationReadyEvent` — pour que la DB soit toujours alignée sur les fichiers Markdown sans intervention humaine.

---

## Comportement attendu

### Cas nominal

1. Au démarrage de l'application Spring Boot, l'événement `ApplicationReadyEvent` déclenche `BacklogSyncService.sync(SyncTrigger.STARTUP)`. Garantit que la DB est peuplée dès le premier accès à `/super-admin/backlog` après un déploiement.
2. Toutes les 5 minutes (UTC), le cron `BacklogSyncScheduler` appelle `BacklogSyncService.sync(SyncTrigger.SCHEDULED)`. Logge succès (INFO) ou échec (WARN avec message).
3. `BacklogSyncService.sync()` (déjà SF-178-01) parse, upsert idempotent, persiste un `BacklogSyncRunEntity` audit avec `triggered_by = STARTUP | SCHEDULED`.
4. Si `sync()` lève une exception, le scheduler **catch** l'erreur et log WARN — il ne propage **pas**, sinon Spring désactiverait définitivement le job en mode `errorHandler` par défaut.
5. Le run d'audit est persisté quoi qu'il arrive (le service le fait avant de relancer).

### Cas d'erreur

| Situation | Comportement | Trace |
|-----------|--------------|-------|
| Fichier MD introuvable au démarrage | `STARTUP` run en `success=false`, exception loggée WARN, app démarre quand même | log WARN + audit row |
| Fichier MD introuvable au cron | `SCHEDULED` run en `success=false`, le cron continue de tourner | log WARN + audit row |
| Exception parser au cron | Idem | log WARN + audit row |
| 2 cron runs qui se chevauchent | Spring `@Scheduled` mode single-thread par défaut → impossible (un job ne démarre pas avant la fin du précédent) | — |
| App redémarre pendant un cron | Job interrompu, sans audit row terminé. Prochain cron 5 min plus tard reprend. | — |

---

## Analyse de cohérence transversale

- [x] **Autres outils / pays / domaines** : N/A — feature transversale interne.
- [x] **Pattern UI** : N/A.
- [x] **Pattern `@Scheduled`** : déjà utilisé dans le projet (`@EnableScheduling` actif sur `LegalcaseBackendApplication`). Pas de nouveau pattern introduit.
- [x] **Pattern `@EventListener(ApplicationReadyEvent.class)`** : pattern Spring standard. Vérifier qu'aucun autre listener du projet ne fait déjà un parsing lourd au boot pour ne pas allonger le startup.

### Décision

- [x] Étendu à toutes les cibles applicables.

---

## Critères d'acceptation

- [ ] Composant `BacklogSyncScheduler` créé dans `fr.ailegalcase.backlog`
- [ ] Méthode `@Scheduled(cron = "0 */5 * * * *")` appelle `syncService.sync(SyncTrigger.SCHEDULED)` et catch les exceptions sans propager
- [ ] Méthode `@EventListener(ApplicationReadyEvent.class)` appelle `syncService.sync(SyncTrigger.STARTUP)` et catch les exceptions sans propager
- [ ] L'app démarre normalement même si la sync STARTUP échoue (fichier MD absent en test)
- [ ] Test UT vérifiant que le scheduler appelle bien `syncService.sync(SCHEDULED)` (mock du service)
- [ ] Test UT vérifiant qu'une exception du service ne propage pas et est loggée
- [ ] Test UT vérifiant que le startup hook appelle `sync(STARTUP)`
- [ ] Tous les tests existants passent sans régression (3161+)

---

## Périmètre

### Hors scope

- **Configuration de la fréquence** via properties — fixe à 5 min (cohérent avec `staleAfterMinutes=10` dans BacklogProperties qui définit "STALE")
- **Désactivation du cron en dev** — laissé activé en dev (le coût est négligeable et ça permet de tester en local)
- **Lock distribué multi-instance** — l'app tournant en 1 réplica en prod aujourd'hui, pas de risque de double sync. Si un jour on scale > 1 réplica, ajouter Shedlock ou désactiver le cron sur N-1 instances.
- **Métriques Micrometer** — pas de gauge/counter exposé (audit dans `backlog_sync_runs` suffit)

---

## Technique

### Composant

`BacklogSyncScheduler` (`@Component`) :
- Constructor : injection `BacklogSyncService`
- Méthode `syncOnStartup` annotée `@EventListener(ApplicationReadyEvent.class)` 
- Méthode `syncPeriodically` annotée `@Scheduled(cron = "0 */5 * * * *")`
- Logger SLF4J `BacklogSyncScheduler.class`
- Both methods wrap `syncService.sync(...)` in try/catch, log WARN on Exception

### Test config

- `BacklogSyncSchedulerTest` — UT pure Mockito, pas de Spring context :
  - Mock `BacklogSyncService`
  - Test 1 : `syncPeriodically()` appelle `sync(SCHEDULED)`
  - Test 2 : `syncOnStartup()` appelle `sync(STARTUP)`
  - Test 3 : exception du service → WARN log + pas de propagation (vérifier que la méthode retourne normalement)

### Migration Liquibase

Aucune.

---

## Plan de test

### Tests unitaires (3)

- `BacklogSyncSchedulerTest.syncPeriodically_callsSyncWithScheduledTrigger`
- `BacklogSyncSchedulerTest.syncOnStartup_callsSyncWithStartupTrigger`
- `BacklogSyncSchedulerTest.exception_isLoggedAndDoesNotPropagate`

### Tests d'intégration

Aucun — le pattern `@Scheduled` lui-même est testé par Spring. Tester le cron en bootant l'app n'apporte rien (latence + complexité).

### Isolation workspace

N/A — feature super-admin transversale.

---

## Analyse d'impact

- [x] Aucune préoccupation transversale touchée.
- L'application démarrage gagne une étape supplémentaire (~50-100 ms pour parser PRODUCT_SPEC.md + MARKETING_BACKLOG.md). Acceptable.
- Le cron consomme ~50-100 ms toutes les 5 min. Négligeable.

### Composants existants potentiellement impactés

- `LegalcaseBackendApplication` a déjà `@EnableScheduling` (vérifié) — aucune modification.
- `BacklogSyncService` (SF-178-01) : aucune modification.
