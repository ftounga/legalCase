# Mini-spec — F-109 / SF-109-01 Alertes procédurales automatiques

---

## Identifiant

`F-109 / SF-109-01`

## Feature parente

`F-109` — Alertes procédurales automatiques

## Statut

`ready`

## Date de création

2026-04-04

## Branche Git

`feat/SF-109-01-alerte-requalification`

---

## Objectif

Envoyer un email à l'avocat quand une re-synthèse enrichie requalifie un point VERIFIED en NON_COMPLIANT ou TO_CHECK via `checks_a_requalifier`.

---

## Comportement attendu

### Cas nominal

1. Une analyse enrichie se termine (`EnrichedAnalysisService`).
2. `ProcedureCheckService.createChecksWithVerifiedPropagation()` applique les requalifications Claude.
3. Si au moins un check VERIFIED a effectivement changé de statut, un `ProcedureCheckRequalifiedEvent` est publié.
4. `RequalificationAlertService` écoute l'événement et appelle `EmailService.sendRequalificationAlert()`.
5. L'avocat (créateur du dossier) reçoit un email listant les points requalifiés avec leur raison.

### Contenu de l'email

- **Objet** : `Point(s) procédural(aux) réévalué(s) sur "[titre dossier]" — action requise`
- **Corps** : liste des checks requalifiés (description + nouveau statut + raison IA), lien vers le dossier

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Email créateur introuvable | Log warn, pas d'envoi, pas de crash (fail-open) |
| Envoi email échoue (SMTP) | Log warn, pas de crash (fail-open) |
| Aucune requalification effective | Aucun event publié, aucun email |
| Requalification TO_CHECK uniquement (sans NON_COMPLIANT) | Email envoyé quand même — tout changement VERIFIED → autre statut est notable |

---

## Critères d'acceptation

- [ ] Un email est envoyé quand au moins un check passe de VERIFIED à NON_COMPLIANT ou TO_CHECK
- [ ] Aucun email n'est envoyé si aucune requalification effective
- [ ] L'email contient le titre du dossier, la liste des checks requalifiés avec leur raison
- [ ] Le destinataire est le créateur du dossier
- [ ] Fail-open : une erreur SMTP ne fait pas planter la pipeline IA
- [ ] Fail-open : si le créateur est introuvable, l'email est ignoré silencieusement

---

## Périmètre

### Hors scope

- Notifications aux autres membres du workspace (uniquement le créateur)
- Requalification manuelle par l'avocat (uniquement requalification IA)
- Push notification ou SSE (uniquement email)
- Email de test ou preview

---

## Technique

### Endpoint(s)

Aucun — traitement asynchrone interne.

### Nouveaux composants

| Composant | Rôle |
|---|---|
| `ProcedureCheckRequalifiedEvent` | Record Spring — `caseFileId`, `caseFileTitle`, `creatorEmail`, liste de `RequalifiedCheck(description, newStatus, raison)` |
| `RequalificationAlertService` | `@EventListener` → appelle `EmailService.sendRequalificationAlert()`, fail-open |
| `EmailService.sendRequalificationAlert()` | Nouvelle méthode — envoie l'email via `SimpleMailMessage` |

### Composants modifiés

| Composant | Modification |
|---|---|
| `ProcedureCheckService` | Injection `ApplicationEventPublisher` + `CaseFileRepository` — publie l'event après requalifications effectives |

### Tables impactées

Aucune nouvelle table.

### Migration Liquibase

- [x] Non applicable

---

## Plan de test

### Tests unitaires

- [ ] `ProcedureCheckService` — requalifications non vides → event publié avec la bonne liste
- [ ] `ProcedureCheckService` — requalifications vides → aucun event publié
- [ ] `ProcedureCheckService` — requalification listée mais aucun VERIFIED ne correspond → aucun event publié
- [ ] `RequalificationAlertService` — event reçu → `sendRequalificationAlert()` appelé
- [ ] `RequalificationAlertService` — exception SMTP → pas de propagation (fail-open)
- [ ] `EmailService.sendRequalificationAlert()` — construit le bon message (sujet, corps avec liste)

### Tests d'intégration

- [ ] Non applicable — pas d'endpoint HTTP

### Isolation workspace

- [x] Non applicable — email envoyé au créateur déjà résolu depuis le workspace du dossier

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — ajout d'un event + listener + méthode email, aucune modification auth/routing/workspace/plans.

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

- F-65 (notifications email) — statut : **done** (`EmailService` en place)
- SF-96-05 (requalification automatique) — statut : **done** (`createChecksWithVerifiedPropagation()` en place)

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- L'event est construit dans `ProcedureCheckService` après l'étape 2 (application des requalifications), avant la propagation des VERIFIED vers la nouvelle analyse. On publie uniquement si au moins un check a effectivement changé.
- `creatorEmail` est résolu dans `ProcedureCheckService` via `CaseFileRepository.findCreatorEmailById()` (méthode déjà existante) avant de publier l'event — évite d'injecter le repo dans `RequalificationAlertService`.
- Pattern identique à `AnalysisNotificationService` / `AnalysisStatusEvent`.
