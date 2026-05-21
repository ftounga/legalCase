# Mini-spec — F-INFRA / SF-INFRA-09 — Retrait total de Sentry + redirection des erreurs frontend vers CloudWatch

> **Étape 0 (cadrage cohérence)** : EXEMPTÉE — SF infrastructure pure, refactor d'observabilité sans nouveau workflow utilisateur visible. La capture d'exception côté frontend devient invisible pour l'avocat (comme l'était Sentry).
> **Étape 0 bis (cohérence écran)** : EXEMPTÉE — pas d'UI nouvelle ni déplacée. Aucun écran impacté.

---

## Identifiant

`F-INFRA / SF-INFRA-09`

## Feature parente

`F-INFRA` — Travaux d'infrastructure AWS (suite directe de SF-INFRA-07b qui a déployé Fluent Bit + métric filter + alarme `legalcase-production-backend-error-rate`).

Note : F-47 « Monitoring & alertes applicatives » (V2 Terminée) avait introduit Sentry. SF-INFRA-09 la remplace fonctionnellement par la chaîne CloudWatch (déjà en place côté backend, étendue désormais aux erreurs frontend).

## Statut

`in-progress`

## Date de création

2026-05-21

## Branche Git

`feat/SF-INFRA-09-retrait-sentry-cloudwatch`

---

## Objectif

Supprimer entièrement Sentry (dep Maven backend + package npm frontend + config K8s/CI/CD) et router les erreurs JavaScript Angular vers CloudWatch via un endpoint `/api/v1/logs/client-error` qui logge en SLF4J, captée ensuite par Fluent Bit + metric filter + alarme `legalcase-production-backend-error-rate` déjà existants.

---

## Contexte

- **Sentry actuel** (F-47, V2 Terminée 2026-03-25) : `io.sentry:sentry-spring-boot-starter-jakarta` côté Java + `@sentry/angular` côté Angular. Capture exceptions backend automatique + appel manuel `Sentry.captureEvent(...)` sur job IA FAILED dans `CaseAnalysisService` et `EnrichedAnalysisService`.
- **CloudWatch déjà en place** (SF-INFRA-07b) : Fluent Bit DaemonSet stream les stdout de tous les pods backend vers le log group `/aws/eks/legalcase-shared/applications`. Un metric filter sur le pattern `"ERROR"` alimente la métrique CloudWatch dont l'alarme `legalcase-production-backend-error-rate` se déclenche au-delà de 10 ERROR / 5 min.
- **Décision** : abandonner Sentry pour réduire le périmètre sous-traitant (souveraineté, futur SOC 2 / ISO 27001 F-134) et unifier l'observabilité sur la stack AWS native. Le coût marginal de la chaîne CloudWatch est nul (déjà payée) ; Sentry libre = ~26 €/mois de licence économisée.
- **Couverture identique** : capture toute exception Angular non gérée (équivalent `Sentry.createErrorHandler()`) + capture des HTTP 5xx renvoyés par le backend qui ne sont pas systématiquement loggés côté navigateur. Le log group CloudWatch couvre les 30 derniers jours (rétention par défaut shared cluster) — équivalent au plan Sentry free actuel (30j rétention).

---

## Comportement attendu

### Cas nominal — Exception JS non gérée côté Angular

1. Une erreur runtime non catchée survient dans un composant Angular en production (`new Error(...)`, accès propriété sur `undefined`, etc.).
2. `GlobalErrorHandler` (extends `ErrorHandler`) reçoit l'exception via `handleError(error)`.
3. Le handler appelle `ClientErrorService.report(...)` qui :
   - Construit un payload `ClientErrorPayload` (message tronqué à 500 chars, stack tronquée à 4000 chars, `location.href`, `navigator.userAgent` tronqué à 200, ISO timestamp, `environment.appVersion` si dispo).
   - Calcule un hash simple `message + stack[0..200]`. Si ce hash a déjà été envoyé dans la session courante (`Set` en mémoire), `report()` est no-op (dédup).
   - Sinon, POST `/api/v1/logs/client-error` via `HttpClient` injecté. `catchError` silencieux pour ne jamais throw depuis le handler.
4. Le `super.handleError(error)` est appelé en suivant — la console navigateur affiche l'erreur en dev/staging comme avant.
5. Côté backend, `ClientErrorLogController` reçoit le POST. Le service `ClientErrorLogService` logue via SLF4J : `log.error("[FRONTEND] {} | url={} | ua={} | stack={}", payload.message(), payload.url(), payload.userAgent(), payload.stack())`.
6. Le log atterrit sur stdout du pod backend → Fluent Bit → log group CloudWatch → metric filter `"ERROR"` (matche `[FRONTEND] ... ERROR ...` car SLF4J ajoute `ERROR` au level) → métrique +1 → alarme `legalcase-production-backend-error-rate` si seuil dépassé.

### Cas nominal — HTTP 5xx interceptée

1. Une requête HTTP du frontend reçoit en réponse un code 500/502/503/504.
2. `clientErrorHttpInterceptor` intercepte la réponse via `catchError` du stream.
3. Construit un payload `{ message: "HTTP {status} on {method} {url}", stack: response.error?.message, url: request.url, userAgent, timestamp, appVersion }`.
4. POST `/api/v1/logs/client-error` (même dédup `ClientErrorService`).
5. **Re-throw l'erreur** pour ne pas masquer le 5xx aux callers : `throwError(() => error)` à la fin du `catchError`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `message` absent ou vide | 400 Bad Request avec violation JSR-303 | 400 |
| `message` > 500 chars | 400 Bad Request | 400 |
| `stack` > 4000 chars | 400 Bad Request | 400 |
| Rate limit user authentifié (> 10/min) | 429 Too Many Requests (silencieux côté UI, juste loggé en INFO côté backend) | 429 |
| Rate limit IP anonyme (> 5/min) | 429 Too Many Requests | 429 |
| Endpoint POST `/api/v1/logs/client-error` indisponible | Côté frontend : `catchError` silencieux, l'avocat ne voit rien | n/a |
| `ClientErrorLogService` throw une exception | `log.warn(...)` interne, 500 retourné (sans détails fuités) | 500 |

---

## Critères d'acceptation

1. L'endpoint `POST /api/v1/logs/client-error` accepte un body JSON valide (auth optionnelle) et retourne `200 OK` avec body `{"status":"logged"}`.
2. Pour chaque requête acceptée, exactement une ligne `ERROR ... [FRONTEND]` est émise sur stdout du pod backend (vérifiable par capture du logger SLF4J en test).
3. La dépendance Maven `io.sentry:sentry-spring-boot-starter-jakarta` est absente de `backend/pom.xml`.
4. Aucun `import io.sentry.*` ni appel `Sentry.*` ne subsiste dans `backend/src/main/java/**`.
5. La section `sentry:` est absente de `backend/src/main/resources/application.yml`.
6. Le package `@sentry/angular` est absent de `frontend/package.json` (deps + devDeps).
7. Aucun `import * as Sentry` dans `frontend/src/**`.
8. La clé `sentryDsn` est absente des 3 environnements Angular (`environment.ts`, `environment.staging.ts`, `environment.prod.ts`).
9. Les configMaps `k8s/overlays/{staging,production}/kustomization.yaml` ne contiennent plus `SENTRY_ENV=...`.
10. Les workflows `.github/workflows/{backend,deploy-production}.yml` ne référencent plus `SENTRY_DSN`.
11. Backend compile sans warnings inattendus (`mvn -DskipTests compile` PASS).
12. Test backend `ClientErrorLogControllerTest` (unit) + `ClientErrorLogControllerIT` (integration) PASS.
13. Le test legacy `SentryJobReportingTest.java` est supprimé (devient obsolète) ; un test équivalent `JobFailureLoggingTest.java` vérifie qu'un job IA FAILED produit une ligne `log.error(...)` SLF4J avec les bons champs (caseFileId, jobType).
14. Frontend : `npx tsc --noEmit` PASS et `npx jest src/app/core/observability` PASS (≥ 3 spec files, ≥ 9 tests verts).
15. Cible alarme `legalcase-production-backend-error-rate` (déjà déployée) couvre désormais les erreurs frontend (documenté en commentaire dans la PR).

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : N/A — pas de modification métier.
- [x] **Autres pays** : N/A — observabilité indépendante du pays.
- [x] **Autres domaines** : N/A.
- [x] **Autres UI patterns** : N/A — pas de nouvel UI pattern.
- [x] **Autres flows transversaux** : auth (l'endpoint accepte anonyme + authentifié), navigation (interceptor HTTP global), erreurs (handler global Angular).

### Décisions

- [x] Mécanisme intégré directement dans la SF (point unique d'envoi `ClientErrorService` consommé par 2 sources : `GlobalErrorHandler` + `clientErrorHttpInterceptor`).
- [x] Aucun fork pays/domaine — chaîne unique.

### Cas spécifique : pattern réutilisable

- `ClientErrorService` est un service partagé `provideIn: 'root'` injectable n'importe où si on veut un jour reporter manuellement un événement (équivalent `Sentry.captureMessage`). Le contrat est public ; pas d'autre consommateur prévu en V1.

---

## Analyse d'impact — préoccupations transversales

- **Auth / Principal** : impacté. L'endpoint `/api/v1/logs/client-error` est `permitAll()` (auth optionnelle). Le contrôleur résout l'utilisateur via `@AuthenticationPrincipal OAuth2User principal` (nullable) pour le rate limit (10/min user / 5/min IP). Aucun autre endpoint Spring Security n'est modifié.
- **Workspace context** : non impacté. Pas de lecture/écriture en base, pas de `workspace_id`.
- **Plans / limites** : non impacté.
- **Navigation / routing** : non impacté.
- **Outil décisionnel métier** : non impacté.

Smoke E2E concernés : `e2e/auth.spec.ts` et `e2e/navigation.spec.ts` — l'endpoint nouveau ne doit pas bloquer les flows existants. Vérification du `noAuth_isPermitted` couvert en IT backend.

---

## Plan de test

### Backend — `ClientErrorLogControllerTest` (unit, Mockito)

| ID | Cas | Attendu |
|----|-----|---------|
| U-01 | Payload valide minimal (message uniquement) | service.report() appelé, 200 |
| U-02 | Payload complet | service.report() appelé avec tous champs, 200 |
| U-03 | Message null | violation @NotBlank, 400 |
| U-04 | Message > 500 chars | violation @Size, 400 |
| U-05 | Stack > 4000 chars | violation @Size, 400 |
| U-06 | Service throw exception | 500 sans fuite |

### Backend — `ClientErrorLogServiceRateLimitTest` (unit)

| ID | Cas | Attendu |
|----|-----|---------|
| R-01 | 10 appels même user en < 60s | 10 PASS |
| R-02 | 11ᵉ appel même user en < 60s | rate limited (return false) |
| R-03 | 5 appels même IP anonyme en < 60s | 5 PASS |
| R-04 | 6ᵉ appel même IP anonyme en < 60s | rate limited |
| R-05 | Sliding window : après 60s, le compteur se réinitialise | PASS |

### Backend — `ClientErrorLogControllerIT` (integration, `@SpringBootTest`)

| ID | Cas | Attendu |
|----|-----|---------|
| I-01 | POST valide anonyme | 200 + `{"status":"logged"}` |
| I-02 | POST valide minimal | 200 |
| I-03 | Message absent | 400 |
| I-04 | Message trop long | 400 |
| I-05 | Endpoint accessible sans authentification | 200 (permitAll vérifié) |
| I-06 | Plusieurs requêtes successives | log SLF4J capté (via `OutputCaptureExtension`) contient `[FRONTEND]` |

### Backend — `JobFailureLoggingTest` (remplace `SentryJobReportingTest`)

| ID | Cas | Attendu |
|----|-----|---------|
| J-01 | CaseAnalysis FAILED | `log.error(...)` SLF4J émis avec caseFileId + jobType=CASE_ANALYSIS |
| J-02 | CaseAnalysis DONE | aucun `log.error` job-failure |
| J-03 | EnrichedAnalysis FAILED | `log.error(...)` avec caseFileId + jobType=ENRICHED_ANALYSIS |

### Frontend — `global-error-handler.spec.ts`

| ID | Cas | Attendu |
|----|-----|---------|
| H-01 | `handleError(new Error('boom'))` | `ClientErrorService.report` appelé 1× |
| H-02 | `handleError(error)` puis re-appel avec même error | dédup, report appelé 1× |
| H-03 | `super.handleError` est appelé (console.error en dev) | spy console.error appelé |

### Frontend — `client-error-http.interceptor.spec.ts`

| ID | Cas | Attendu |
|----|-----|---------|
| X-01 | HTTP 500 réponse | report appelé + erreur re-thrown |
| X-02 | HTTP 404 réponse | report NON appelé (4xx out-of-scope) |
| X-03 | HTTP 200 réponse | report NON appelé |
| X-04 | URL `/api/v1/logs/client-error` elle-même | report NON appelé (anti-boucle) |

### Frontend — `client-error.service.spec.ts`

| ID | Cas | Attendu |
|----|-----|---------|
| S-01 | `report({...})` | POST `/api/v1/logs/client-error` émis |
| S-02 | `report(same)` 2× | POST émis 1× (dédup hash session) |
| S-03 | Backend renvoie 500 | service swallow l'erreur (catchError silencieux) |

---

## Composants impactés

### Backend — créés
- `backend/src/main/java/fr/ailegalcase/observability/ClientErrorLogController.java`
- `backend/src/main/java/fr/ailegalcase/observability/ClientErrorLogService.java`
- `backend/src/main/java/fr/ailegalcase/observability/ClientErrorPayload.java`
- `backend/src/test/java/fr/ailegalcase/observability/ClientErrorLogControllerTest.java`
- `backend/src/test/java/fr/ailegalcase/observability/ClientErrorLogControllerIT.java`
- `backend/src/test/java/fr/ailegalcase/observability/ClientErrorLogServiceRateLimitTest.java`
- `backend/src/test/java/fr/ailegalcase/analysis/JobFailureLoggingTest.java`

### Backend — modifiés
- `backend/pom.xml` — retrait dep Sentry
- `backend/src/main/resources/application.yml` — retrait section `sentry:`
- `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisService.java` — `reportJobFailureToSentry` → `logJobFailure` SLF4J
- `backend/src/main/java/fr/ailegalcase/analysis/EnrichedAnalysisService.java` — idem
- `backend/src/main/java/fr/ailegalcase/auth/SecurityConfig.java` — ajout `.requestMatchers(HttpMethod.POST, "/api/v1/logs/client-error").permitAll()`

### Backend — supprimés
- `backend/src/test/java/fr/ailegalcase/analysis/SentryJobReportingTest.java`

### Frontend — créés
- `frontend/src/app/core/observability/global-error-handler.ts`
- `frontend/src/app/core/observability/client-error-http.interceptor.ts`
- `frontend/src/app/core/observability/client-error.service.ts`
- `frontend/src/app/core/observability/client-error-payload.model.ts`
- `frontend/src/app/core/observability/global-error-handler.spec.ts`
- `frontend/src/app/core/observability/client-error-http.interceptor.spec.ts`
- `frontend/src/app/core/observability/client-error.service.spec.ts`

### Frontend — modifiés
- `frontend/package.json` — retrait `@sentry/angular`
- `frontend/src/main.ts` — retrait `Sentry.init(...)`
- `frontend/src/app/app.config.ts` — remplace les 3 providers Sentry par `{ provide: ErrorHandler, useClass: GlobalErrorHandler }` + ajout interceptor
- `frontend/src/environments/environment.ts` — retrait `sentryDsn`
- `frontend/src/environments/environment.staging.ts` — retrait `sentryDsn`
- `frontend/src/environments/environment.prod.ts` — retrait `sentryDsn`

### K8s / CI/CD — modifiés
- `k8s/overlays/staging/kustomization.yaml` — retrait `SENTRY_ENV=staging`
- `k8s/overlays/production/kustomization.yaml` — retrait `SENTRY_ENV=production`
- `.github/workflows/backend.yml` — retrait `SENTRY_DSN` env + `--from-literal=SENTRY_DSN`
- `.github/workflows/deploy-production.yml` — idem

---

## Hors périmètre

- **Suppression du compte Sentry sentry.io** : geste manuel post-merge côté admin, hors code.
- **Retrait du lien Sentry dans `/super-admin` hub** (super-admin.component.html ligne 145) : laissé en place tant que le compte Sentry existe (lecture de l'historique). Sera retiré dans une SF dédiée après suppression du compte.
- **Modification de l'alarme `legalcase-production-backend-error-rate`** : aucune nécessité — le pattern `"ERROR"` capte déjà `[FRONTEND] ERROR ...` automatiquement.
- **Rétention CloudWatch > 30 j** : sujet F-141 (futur Prometheus/Grafana) ou nouvelle SF dédiée si besoin.
- **Sampling / level filtering côté frontend** : V2 si l'alarme se met à crier à cause d'un bug récurrent — pour l'instant, dédup session-scope suffit.

---

## Contraintes de validation

| Champ | Règle |
|-------|-------|
| `ClientErrorPayload.message` | `@NotBlank @Size(max=500)` |
| `ClientErrorPayload.stack` | `@Size(max=4000)` nullable |
| `ClientErrorPayload.url` | `@Size(max=500)` nullable |
| `ClientErrorPayload.userAgent` | `@Size(max=200)` nullable |
| `ClientErrorPayload.timestamp` | ISO-8601 string, nullable (backend remplit si absent) |
| `ClientErrorPayload.appVersion` | `@Size(max=50)` nullable |
| Rate limit user | 10 events / 60s sliding window (in-memory `ConcurrentHashMap<String, Deque<Long>>`) |
| Rate limit IP anonyme | 5 events / 60s sliding window |

---

## Notes implémentation

- **Choix du package** : `fr.ailegalcase.observability` (nouveau, propre — pas de mélange avec `analysis` ou `audit`).
- **Rate limit** : `ConcurrentHashMap<String, Deque<Long>>` avec sliding window 60s, suffisant pour le volume attendu (< 100 events/min total). Caffeine pas nécessaire à ce stade.
- **Dédup frontend** : `Set<string>` scoped à l'instance `ClientErrorService` (donc reset au reload). Pas de stockage localStorage car on veut justement re-signaler si l'utilisateur recharge la page sur l'erreur.
- **Format log SLF4J** : `log.error("[FRONTEND] {} | url={} | ua={} | stack={}", message, url, userAgent, stack)`. Le mot `ERROR` est généré automatiquement par SLF4J en début de ligne, donc le pattern metric filter `"ERROR"` matche.
- **Anti-boucle interceptor** : si la requête est elle-même un POST vers `/api/v1/logs/client-error` qui rate, on ne re-report pas (cas X-04).
