# Mini-spec — F-JU-01 / SF-JU-01-10 Bootstrap async + polling status

## Identifiant

`F-JU-01 / SF-JU-01-10`

## Feature parente

`F-JU-01` — Citations jurisprudentielles dans les outils décisionnels (FR + BE) — full auto-pilot Claude

## Statut

`draft`

## Date de création

2026-05-27

## Branche Git

`feat/SF-JU-01-10-bootstrap-async`

---

## Objectif

Transformer l'endpoint synchrone `POST /api/admin/jurisprudence/bootstrap` (qui boucle plusieurs minutes côté serveur et fait timeout sur NGINX `proxy-read-timeout: 120s` côté client → toast 50x trompeur) en endpoint **asynchrone** qui retourne immédiatement un `jobId`, plus un endpoint **status polling** GET pour suivre la progression jusqu'au terminal state.

---

## Comportement attendu

### Cas nominal

- L'avocat super-admin clique « Démarrer le bootstrap » avec un CSV chargé.
- `POST /api/admin/jurisprudence/bootstrap` → réponse **immédiate `202 Accepted`** avec body `{ "jobId": "<uuid>", "entriesTotal": N, "startedAt": "<iso>" }`.
- L'exécution démarre en arrière-plan via `@Async` (pool Spring déjà activé par `@EnableAsync` dans `LegalcaseBackendApplication`).
- Une ligne est créée dans la nouvelle table `jurisprudence_bootstrap_jobs` avec `status = 'RUNNING'`, `entries_total = N`, compteurs à 0.
- Au fil du traitement, le job est mis à jour (UPDATE par entrée) : `entries_processed++`, `mappings_created` ou `entries_skipped` selon résultat. Chaque UPDATE est commité dans sa propre transaction (cohérent SF-JU-01-09).
- Le frontend polls `GET /api/admin/jurisprudence/bootstrap/jobs/{jobId}` toutes les **5 secondes** et affiche la progression `X / N entrées traitées, Y créées, Z skipped`.
- En fin de traitement : `status = 'DONE'`, `completed_at`, `duration_ms`. Le polling s'arrête. Le frontend affiche `Bootstrap terminé : X processed, Y created, Z skipped (Tms)`.
- En cas d'exception fatale dans le runner async (NPE, OOM, etc.) : `status = 'FAILED'`, `error_message` rempli, polling s'arrête, frontend affiche un MatSnackBar d'erreur.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Payload invalide (0 entrée, > 200 entries, champs hors regex) | Validation Spring 400 standard, **aucun job créé** | 400 |
| jobId inexistant ou format UUID invalide sur GET status | 404 avec message « Job introuvable » | 404 |
| Exception fatale dans le runner async | Le job est UPDATE en `FAILED` avec `error_message` ; le frontend récupère l'état au prochain poll | n/a (interne) |
| Kill du pod en cours de run | Le job reste `RUNNING` indéfiniment (pas de heartbeat) — V1 accepté. Le frontend continue de poller, l'admin réalise visuellement que le compteur ne bouge plus | n/a |
| Plusieurs onglets / sessions ouvrent le même jobId | Tous voient les mêmes counters (read-only) | 200 |
| Bootstrap relancé pendant qu'un autre tourne | Pas de garde-fou serveur V1 — 2 jobs en parallèle possibles, chacun avec son jobId distinct. Frontend ne bloque pas non plus (cas marginal admin) | 202 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — non applicable, F-JU-01 spécifique
- [x] **Autres pays** — non applicable
- [x] **Autres domaines** — non applicable
- [x] **Autres UI patterns** — le pattern « POST sync long → POST async + polling status » est nouveau pour `/super-admin/jurisprudence-watch`. Pourrait être réutilisé pour d'autres jobs admin longs (export massif, recompute IA, etc.). **Pas de pattern partagé créé V1** — duplication acceptable pour 1 cas, à généraliser si ≥ 3 occurrences (mémoire `feedback_skills_over_governance`).
- [x] **Autres flows transversaux** — pas de modif auth/workspace/plans/navigation.

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Pattern UI** : barre de progression d'un job long. Pas de composant `<app-job-progress>` partagé créé — restreint au composant `jurisprudence-watch.component.ts`. Si un 2e job long apparaît (cron manuel, recompute), extraire alors un shared component.
- [x] **Service** : `JurisprudenceBootstrapJobRepository` est interne à F-JU-01.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern « endpoint async + polling » | Premier usage produit | Spécifique à F-JU-01 V1. À généraliser si ≥ 3 occurrences (note backlog pas créée — observation pour future SF si signal) |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s)
- [ ] Backlog VN
- [ ] Non applicable

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — composant `jurisprudence-watch.component.ts` est un panneau super-admin, pas un outil décisionnel intégré au `TOOL_REGISTRY`. Aucun pré-fill IA, aucune validation F-IA-03, aucun lien dashboard `case-file`.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — la SF ne crée pas d'outil décisionnel à champs saisissables.

---

## Critères d'acceptation

- [ ] Migration Liquibase `350-create-jurisprudence-bootstrap-jobs.xml` crée la table avec les colonnes attendues + PK + FK + index sur `status` + `started_at`.
- [ ] `POST /api/admin/jurisprudence/bootstrap` retourne **`202 Accepted`** avec `{ jobId, entriesTotal, startedAt }` en < 500 ms (création job DB + lancement `@Async`, sans attente du runner).
- [ ] `GET /api/admin/jurisprudence/bootstrap/jobs/{jobId}` retourne 200 avec status + counters quand le job existe, 404 sinon.
- [ ] Le runner async met à jour le job **après chaque entrée traitée** dans une transaction dédiée (réutilise `TransactionTemplate` de SF-JU-01-09).
- [ ] Une exception fatale dans le runner async positionne `status = 'FAILED'` + `error_message` (pas de RUNNING figé en DB).
- [ ] Le composant `jurisprudence-watch.component.ts` polls le job toutes les 5s, affiche `X / N entrées traitées, Y créées, Z skipped`, et stoppe le polling à terminal state.
- [ ] Le `MatSnackBar` final affiche soit le message de succès, soit l'erreur si `FAILED`.
- [ ] L'isolation `SUPER_ADMIN` est conservée sur les 2 endpoints (POST + GET status).
- [ ] Test backend unitaire : `JurisprudenceBootstrapServiceTest.startBootstrap_createsRunningJobAndReturnsJobId`.
- [ ] Test backend IT : `JurisprudenceWatchAdminControllerIT` (ou équivalent) qui vérifie la 202 immédiate + la 404 sur job inexistant.
- [ ] Test frontend Jest : polling boucle 3 fois, status passe RUNNING → DONE → polling stoppe.

---

## Périmètre

### Hors scope (explicite)

- **Reprise après kill** : un job en RUNNING tué n'est pas auto-marqué FAILED par un janitor — il reste RUNNING en DB. Accepté V1.
- **Heartbeat / pinger** : pas de mécanisme « le runner ping le job toutes les N secondes ; si pas de ping > X minutes, le job devient STUCK ». À ajouter si signal terrain.
- **Annulation du job en cours** : pas d'endpoint `POST /jobs/{jobId}/cancel`. V1 = on laisse tourner ou on kill le pod.
- **Liste des jobs passés** : pas de tableau « historique des bootstraps ». Visible via audit log existant `jurisprudence_audit_log` (entrées `AUTO_ADD` actor `SUPER_ADMIN`). À ajouter si signal terrain.

---

## Valeurs initiales

À la création d'un job :
| Champ | Valeur initiale | Règle |
|-------|-----------------|-------|
| status | RUNNING | Imposée par le service au démarrage |
| entries_total | `request.entries().size()` | Snapshot au démarrage |
| entries_processed / mappings_created / entries_skipped | 0 | Tous compteurs partent à 0 |
| started_at | `now()` | DB default |
| completed_at | null | Renseigné en fin de run |
| duration_ms | null | Renseigné en fin de run |
| error_message | null | Renseigné si FAILED |

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs autorisées |
|-------|-------------|-----------------------------|
| status | Oui | enum `RUNNING` / `DONE` / `FAILED` (varchar avec CHECK constraint) |
| entries_total | Oui | int, > 0 |
| entries_processed / mappings_created / entries_skipped | Oui | int, ≥ 0, monotone croissant |
| duration_ms | Non (null si RUNNING) | bigint, ≥ 0 |
| error_message | Non (null si pas FAILED) | text |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|--------------|
| POST | `/api/admin/jurisprudence/bootstrap` | Oui | SUPER_ADMIN |
| GET | `/api/admin/jurisprudence/bootstrap/jobs/{jobId}` | Oui | SUPER_ADMIN |

Réponse `POST` (changement) : `JurisprudenceBootstrapJobStarted { jobId: UUID, entriesTotal: int, startedAt: Instant }` au lieu de `JurisprudenceBootstrapResponse` complet.

Réponse `GET` : `JurisprudenceBootstrapJobStatus { jobId, status, entriesTotal, entriesProcessed, mappingsCreated, entriesSkipped, durationMs?, errorMessage?, startedAt, completedAt? }`.

### Tables impactées

| Table | Opération |
|-------|-----------|
| `jurisprudence_bootstrap_jobs` (nouvelle) | INSERT au démarrage, UPDATE à chaque entrée, UPDATE final |

### Migration Liquibase

- [x] Oui — `350-create-jurisprudence-bootstrap-jobs.xml`
- [ ] Non applicable

### Composants Angular

- `jurisprudence-watch.component.ts` — refactor de `runBootstrap()` : remplace la subscribe unique par un polling `interval(5000).pipe(switchMap(() => client.getJobStatus(jobId)))` jusqu'à statut DONE/FAILED.
- `jurisprudence-watch-admin.service.ts` — `triggerBootstrap()` retourne maintenant `Observable<JobStarted>` ; nouvelle méthode `getBootstrapJobStatus(jobId)`.

### Détail des modifications backend

| Fichier | Modification |
|---------|--------------|
| `db/changelog/migrations/350-create-jurisprudence-bootstrap-jobs.xml` (nouveau) | Création de la table + 2 index |
| `JurisprudenceBootstrapJob.java` (nouveau) | Entity JPA |
| `JurisprudenceBootstrapJobStatus.java` (nouveau) | Enum RUNNING / DONE / FAILED |
| `JurisprudenceBootstrapJobRepository.java` (nouveau) | Spring Data Repository |
| `JurisprudenceBootstrapService.java` | Ajout `startBootstrap(request, user)` qui INSERT job + lance `@Async runBootstrapAsync(jobId, request, user)`. Refactor de la boucle pour UPDATE le job au fil de l'eau. |
| `JurisprudenceBootstrapJobStarted.java` (nouveau record) | DTO réponse 202 |
| `JurisprudenceBootstrapJobStatusResponse.java` (nouveau record) | DTO réponse GET |
| `JurisprudenceWatchAdminController.java` | (1) POST `/bootstrap` retourne 202 + JobStarted ; (2) Nouveau GET `/bootstrap/jobs/{jobId}` |

---

## Plan de test

### Tests unitaires

- [ ] `JurisprudenceBootstrapServiceTest.startBootstrap_createsRunningJobAndReturnsJobId` — vérifie INSERT en `RUNNING` + jobId UUID non null + retour rapide (mock async runner).
- [ ] `JurisprudenceBootstrapServiceTest.runBootstrapAsync_updatesJobAfterEachEntry` — 2 entrées ADD → 2 UPDATE du job (processed = 2, created = 2, status = DONE final).
- [ ] `JurisprudenceBootstrapServiceTest.runBootstrapAsync_setsFailedOnFatalException` — Claude évaluator lève une exception non-DB → status = FAILED, error_message rempli.

### Tests d'intégration

- [ ] `JurisprudenceWatchAdminControllerIT.bootstrap_returns202WithJobId` — POST avec payload valide retourne 202 + jobId présent.
- [ ] `JurisprudenceWatchAdminControllerIT.getJobStatus_unknownJobId_returns404` — GET sur UUID random → 404.
- [ ] `JurisprudenceWatchAdminControllerIT.bootstrap_requiresSuperAdmin` — POST sans SUPER_ADMIN → 403 (anti-régression).

### Tests frontend Jest

- [ ] `jurisprudence-watch.component.spec.ts.shouldPollUntilTerminalStatus` — mock client retourne RUNNING × 2 puis DONE → polling stoppe + snackbar succès affiché.
- [ ] `jurisprudence-watch.component.spec.ts.shouldStopPollingOnFailedStatus` — mock retourne FAILED → polling stoppe + snackbar erreur affichée.

### Isolation workspace

- [x] Non applicable — table `jurisprudence_bootstrap_jobs` globale, accès SUPER_ADMIN uniquement.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — pas d'impact sur auth (rôle SUPER_ADMIN existant), workspace (table globale), plans, navigation.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact | Test de non-régression |
|----------------------|--------|------------------------|
| Frontend `runBootstrap()` (SF-JU-01-06/07) | **Breaking** : la réponse change de `JurisprudenceBootstrapResponse` à `JurisprudenceBootstrapJobStarted`. Le composant doit être refactoré dans la même SF — pas de backward compat HTTP. | Tests Jest mis à jour |
| Cron `JurisprudenceWatchService` (SF-02) / `JurisprudenceDriftService` (SF-03) | Aucun — n'utilisent pas la table jobs ni le POST bootstrap | N/A |

### Smoke tests E2E concernés

- [x] Aucun smoke test couvre actuellement le panneau super-admin. Pas de smoke à ajouter V1 (panneau admin, parcours rare).

---

## Dépendances

### Subfeatures bloquantes

- `SF-JU-01-08` ✅ done
- `SF-JU-01-09` ✅ done (réutilise `TransactionTemplate` du service)

### Questions ouvertes impactées

- [x] Aucune.

---

## Notes et décisions

- **Choix `@Async` vs `CompletableFuture.runAsync`** : on retient `@Async` parce que `@EnableAsync` est déjà actif (`LegalcaseBackendApplication.java`) et un précédent existe (`ExtractionService.onDocumentUploaded`). Cohérence > nouveauté.
- **Pas de pattern shared `<app-job-progress>`** : on attend ≥ 3 occurrences pour généraliser (mémoire `feedback_skills_over_governance`).
- **Idempotence relancée** : héritée du comportement V1 — l'admin maîtrise. Pas de garde-fou « 1 bootstrap à la fois » côté serveur (cas marginal SUPER_ADMIN).
- **Reprise après kill** : volontairement hors scope. Un job RUNNING qui ne bouge plus = signal visuel pour l'admin (compteurs figés). Si signal terrain → ajouter un janitor cron qui marque FAILED les jobs RUNNING sans update depuis > 30 min.
