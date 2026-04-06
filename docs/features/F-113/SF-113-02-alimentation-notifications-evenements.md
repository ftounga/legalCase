# Mini-spec — F-113 / SF-113-02 Alimentation des notifications depuis les événements

---

## Identifiant

`F-113 / SF-113-02`

## Feature parente

`F-113` — Centre de notifications in-app

## Statut

`draft`

## Date de création

2026-04-06

## Branche Git

`feat/SF-113-02-notifications-events`

---

## Objectif

Brancher la création de notifications in-app sur les événements existants (analyse terminée/échouée, requalification procédurale, délai approchant, alerte référentiel) pour que les notifications soient alimentées automatiquement.

---

## Comportement attendu

### Cas nominal

1. **Analyse terminée (DONE)** : quand une analyse STANDARD ou ENRICHED est terminée, le créateur du dossier reçoit une notification ANALYSIS_DONE
2. **Analyse échouée (FAILED)** : quand une analyse échoue, le créateur reçoit une notification ANALYSIS_FAILED
3. **Requalification procédurale** : quand l'IA requalifie des points de procédure, le créateur reçoit une notification PROCEDURE_REQUALIFIED
4. **Délai approchant** : quand un délai est à J-7 ou J-15, tous les membres du workspace reçoivent une notification DEADLINE_APPROACHING
5. **Alerte référentiel** : quand le cron détecte une incohérence, tous les OWNERs reçoivent une notification REFERENTIAL_ALERT

### Services modifiés

| Service | Événement | Destinataire | Type |
|---------|-----------|-------------|------|
| `AnalysisNotificationService` | AnalysisStatusEvent (DONE) | Créateur du dossier | ANALYSIS_DONE |
| `AnalysisNotificationService` | AnalysisStatusEvent (FAILED) | Créateur du dossier | ANALYSIS_FAILED |
| `RequalificationAlertService` | ProcedureCheckRequalifiedEvent | Créateur du dossier | PROCEDURE_REQUALIFIED |
| `DeadlineAlertService` | Cron J-7/J-15 | Tous membres workspace | DEADLINE_APPROACHING |
| `ReferentialCheckService` | Cron 6 mois | Tous OWNERs | REFERENTIAL_ALERT |

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| userId ou workspaceId non résolvable | Fail-open : log warn, notification non créée, email envoyé normalement |
| Exception lors de la création de la notification | Fail-open : log warn, ne bloque pas l'envoi d'email |

---

## Critères d'acceptation

- [ ] AnalysisNotificationService crée une notification ANALYSIS_DONE quand status=DONE
- [ ] AnalysisNotificationService crée une notification ANALYSIS_FAILED quand status=FAILED
- [ ] RequalificationAlertService crée une notification PROCEDURE_REQUALIFIED
- [ ] DeadlineAlertService crée une notification DEADLINE_APPROACHING pour chaque membre
- [ ] ReferentialCheckService crée une notification REFERENTIAL_ALERT pour chaque OWNER
- [ ] Toutes les notifications sont fail-open (ne bloquent pas les emails existants)
- [ ] Tests unitaires sur chaque service modifié
- [ ] Tous les tests existants restent verts

---

## Périmètre

### Hors scope

- Frontend (SF-113-03)
- Nouveaux types de notifications
- Déduplication des notifications (si le même événement crée 2 notifications, c'est OK)

---

## Technique

### Fichiers modifiés

| Fichier | Modification |
|---------|-------------|
| `AnalysisNotificationService.java` | Injecter InAppNotificationService, créer notification DONE + FAILED |
| `RequalificationAlertService.java` | Injecter InAppNotificationService + CaseFileRepository, créer notification |
| `DeadlineAlertService.java` | Injecter InAppNotificationService, créer notification dans notifyMembers() |
| `ReferentialCheckService.java` | Injecter InAppNotificationService, créer notification dans notifyOwners() |

---

## Plan de test

### Tests unitaires

- [ ] AnalysisNotificationService — crée notification ANALYSIS_DONE
- [ ] AnalysisNotificationService — crée notification ANALYSIS_FAILED
- [ ] RequalificationAlertService — crée notification PROCEDURE_REQUALIFIED
- [ ] DeadlineAlertService — crée notification DEADLINE_APPROACHING
- [ ] ReferentialCheckService — crée notification REFERENTIAL_ALERT

### Isolation workspace

- [ ] Non applicable — les services existants gèrent déjà l'isolation

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — ajout d'un appel dans des services existants, pas de modification du comportement existant

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné (justification : ajout fail-open dans des services backend, aucun changement de comportement visible)
