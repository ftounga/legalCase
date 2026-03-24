# Mini-spec — F-39 / SF-39-01 Notifications temps réel via SSE

> Statut : `ready`
> Date de création : 2026-03-24
> Branche Git : `feat/SF-39-01-notifications-sse`

---

## Identifiant

`F-39 / SF-39-01`

## Feature parente

`F-39` — Notifications temps réel

## Statut

`ready`

## Date de création

2026-03-24

## Branche Git

`feat/SF-39-01-notifications-sse`

---

## Objectif

Notifier l'avocat en temps réel (via SSE) quand l'analyse d'un dossier se termine (DONE ou FAILED), sans qu'il ait à surveiller l'écran ou rafraîchir manuellement.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre la page d'un dossier en cours d'analyse (`/case-files/{id}`)
2. Le frontend ouvre une connexion SSE : `GET /api/v1/case-files/{id}/analysis-status/stream`
3. Le backend enregistre un `SseEmitter` pour ce dossier dans le `SseEmitterRegistry`
4. Quand le `CaseAnalysisConsumer` termine l'analyse (DONE ou FAILED), il publie un `AnalysisStatusEvent` via `ApplicationEventPublisher`
5. Le `SseNotificationService` reçoit l'événement et envoie un message SSE à tous les emitters enregistrés pour ce dossier
6. Le frontend reçoit l'événement SSE :
   - Si `DONE` → rechargement automatique de la synthèse + snackbar "Analyse terminée"
   - Si `FAILED` → snackbar d'erreur "L'analyse a échoué"
7. La connexion SSE est fermée côté frontend après réception de l'événement (ou à la destruction du composant)

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Dossier appartenant à un autre workspace | 403 | 403 |
| Dossier inexistant | 404 | 404 |
| Analyse déjà DONE au moment de la connexion SSE | Émission immédiate de l'événement DONE + fermeture | 200 |
| Timeout SSE (connexion inactive > 3 min) | Backend ferme l'emitter proprement (`SseEmitter.complete()`) | — |
| Client se déconnecte (navigation) | Backend retire l'emitter du registre via `onCompletion`/`onTimeout`/`onError` | — |

---

## Critères d'acceptation

- [ ] `GET /api/v1/case-files/{id}/analysis-status/stream` retourne `text/event-stream` avec auth valide
- [ ] Un utilisateur d'un autre workspace reçoit 403
- [ ] Quand l'analyse passe à DONE, le frontend reçoit l'événement SSE sans rafraîchir
- [ ] Quand l'analyse passe à FAILED, le frontend reçoit l'événement SSE avec message d'erreur
- [ ] Si l'analyse est déjà DONE au moment de la connexion, l'événement est émis immédiatement
- [ ] La déconnexion du client est gérée proprement (pas de memory leak dans le registre)
- [ ] Timeout après 3 minutes d'inactivité (emitter nettoyé)
- [ ] Isolation workspace : un emitter n'est jamais notifié pour un dossier d'un autre workspace

---

## Périmètre

### Hors scope (explicite)

- Notifications push navigateur (browser push API)
- Historique de notifications persisté en base
- Notification globale toutes pages (uniquement sur la page du dossier concerné)
- Notification de progression intermédiaire (chunk par chunk) — uniquement DONE/FAILED final
- Support multi-onglets (un seul emitter par connexion, pas de broadcast cross-tab)

---

## Technique

### Endpoint

| Méthode | URL | Auth | Content-Type |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{id}/analysis-status/stream` | Oui (workspace vérifié) | `text/event-stream` |

### Format événement SSE

```
event: ANALYSIS_DONE
data: {"caseFileId": "uuid", "status": "DONE"}

event: ANALYSIS_FAILED
data: {"caseFileId": "uuid", "status": "FAILED", "message": "Erreur pipeline IA"}
```

### Composants backend

| Composant | Rôle |
|-----------|------|
| `SseEmitterRegistry` | Map in-memory `caseFileId → List<SseEmitter>`. Thread-safe (`ConcurrentHashMap`). |
| `AnalysisStatusEvent` | Événement Spring interne (`record`) : `caseFileId`, `status` (DONE/FAILED), `errorMessage` |
| `SseNotificationService` | `@EventListener` sur `AnalysisStatusEvent` → pousse le message SSE aux emitters enregistrés |
| `AnalysisStatusStreamController` | `@GetMapping` → crée `SseEmitter`, l'enregistre, gère timeout/completion/error |

### Déclencheur backend existant modifié

- `CaseAnalysisConsumer` : après `markAsDone()` ou `markAsFailed()` → publier `AnalysisStatusEvent` via `ApplicationEventPublisher`

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_files` | SELECT | Vérification workspace_id avant ouverture du stream |
| `case_analyses` | SELECT | Vérification statut existant (déjà DONE ?) |

### Migration Liquibase

- [x] Non applicable — aucune nouvelle table

### Composants Angular

| Composant / Service | Rôle |
|---------------------|------|
| `AnalysisSseService` | Ouvre `EventSource`, expose un `Observable<AnalysisStatusEvent>`, gère la fermeture |
| `CaseFileDetailComponent` (existant) | S'abonne au SSE si analyse en cours, recharge à la réception de DONE/FAILED |

---

## Plan de test

### Tests unitaires

- [ ] `SseEmitterRegistry` — `register` / `remove` / `getEmitters` fonctionne correctement
- [ ] `SseEmitterRegistry` — registry vide après `remove` du dernier emitter
- [ ] `SseNotificationService` — `onAnalysisStatusEvent` appelle `send()` sur les emitters enregistrés
- [ ] `SseNotificationService` — emitter en erreur est retiré du registre sans crasher

### Tests d'intégration

- [ ] `GET /api/v1/case-files/{id}/analysis-status/stream` → 200 `text/event-stream` avec auth valide
- [ ] `GET /api/v1/case-files/{id}/analysis-status/stream` → 403 si workspace différent
- [ ] `GET /api/v1/case-files/{id}/analysis-status/stream` → 404 si dossier inexistant
- [ ] Analyse déjà DONE → événement émis immédiatement à la connexion

### Isolation workspace

- [ ] Applicable — test : un utilisateur workspace A ne reçoit pas les événements du workspace B

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Auth / Principal** — le stream vérifie `@AuthenticationPrincipal` + `workspace_id`
- [ ] **Workspace context** — résolution workspace standard via `caseFileService.getById` (existant)
- [ ] **Plans / limites** — non applicable
- [ ] **Navigation / routing frontend** — non applicable

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `CaseAnalysisConsumer` | Ajout d'un `publishEvent` après `markAsDone`/`markAsFailed` | Test IT existant du consumer à rejouer |
| `CaseFileDetailComponent` | Ajout d'abonnement SSE conditionnel | Smoke test navigation |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/navigation.spec.ts` — navigation vers un dossier — vérifier pas de régression
- [ ] `e2e/smoke/auth.spec.ts` — login OAuth — vérifier pas de régression

---

## Dépendances

### Subfeatures bloquantes

- Aucune

### Questions ouvertes impactées

- [x] Notification de progression (UI) — **tranchée le 2026-03-24** : SSE

---

## Notes et décisions

- `SseEmitter` avec timeout de 180 000 ms (3 min) — suffisant pour les analyses longues
- Registry in-memory : acceptable pour V1 (instance unique). En cas de scale-out (plusieurs pods), il faudra un bus partagé (RabbitMQ ou Redis Pub/Sub) — noté comme dette technique V2.
- Si l'analyse est déjà DONE au moment de la connexion SSE, le contrôleur émet immédiatement l'événement et ferme le stream — évite un polling de fallback.
- Angular `EventSource` ne supporte pas les headers d'auth custom → l'auth SSE repose sur le cookie de session Spring Security (JSESSIONID), identique aux autres appels API.
