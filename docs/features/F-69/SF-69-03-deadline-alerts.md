# Mini-spec — F-69 / SF-69-03 Alertes J-15/J-7 par email

---

## Identifiant

`F-69 / SF-69-03`

## Feature parente

`F-69` — Suivi des délais légaux

## Statut

`ready`

## Date de création

2026-03-29

## Branche Git

`feat/SF-69-03-deadline-alerts`

---

## Objectif

Envoyer un email à tous les membres du workspace quand un délai atteint J-15 ou J-7, via un job `@Scheduled` quotidien à 8h.

---

## Comportement attendu

### Cas nominal

- `DeadlineAlertService` est annoté `@Scheduled(cron = "0 0 8 * * *")` — s'exécute chaque jour à 8h00
- Calcule `today + 15` et `today + 7`
- Interroge `case_deadlines` pour trouver tous les délais dont `due_date IN (today+15, today+7)`
- Pour chaque délai trouvé :
  - Charge les membres du workspace du dossier via `WorkspaceMemberRepository.findByWorkspace_Id`
  - Envoie un email à chaque membre (email de l'utilisateur)
  - Fail-open : si l'envoi échoue pour un membre, on log warn et on continue
- L'email contient : label du délai, date, nombre de jours restants, lien vers le dossier

### Cas d'erreur / limites

| Situation | Comportement attendu |
|-----------|---------------------|
| Mail désactivé (`app.mail.enabled=false`) | Email non envoyé (log debug) — fail-open |
| Dossier supprimé (soft-deleted) | Non retourné par la requête (filtre `deletedAt IS NULL`) |
| Membre sans email | Skippé (log warn) |
| Exception mail | Log warn, traitement des autres membres continue |
| Exception globale du job | Attrapée au niveau du job, log error — le scheduler continue |

---

## Critères d'acceptation

- [ ] Le job s'exécute à 8h chaque jour (`cron = "0 0 8 * * *"`)
- [ ] Les délais à J-15 ET J-7 sont couverts dans le même job
- [ ] Un email est envoyé à chaque membre du workspace
- [ ] L'email contient le label, la date, le nombre de jours et le lien vers le dossier
- [ ] Fail-open : une erreur d'envoi pour un membre n'interrompt pas le traitement des autres
- [ ] Les dossiers supprimés (soft-delete) sont exclus
- [ ] Le job ne renvoie pas si le délai a déjà été traité le même jour (idempotence via `due_date = today+N` — pas besoin de table d'historique en V1)

---

## Périmètre

### Hors scope (explicite)

- Table d'historique des alertes envoyées (V2 si nécessaire)
- Déduplication sur plusieurs exécutions le même jour
- Opt-out email par membre
- Alertes pour d'autres seuils que J-15 et J-7

---

## Technique

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| case_deadlines | SELECT | filtre `due_date IN (today+15, today+7)` |
| case_files | SELECT via JOIN | filtre `deleted_at IS NULL` |
| workspace_members | SELECT | tous les membres du workspace |
| users | SELECT via JOIN | récupération de l'email |

### Migration Liquibase

- [ ] Non applicable — pas de nouveau schéma

### Composants créés

- `DeadlineAlertService` dans `fr.ailegalcase.casefile`
- `sendDeadlineAlert()` ajoutée à `EmailService`
- Requête `findByDueDateInAndCaseFileDeletedAtIsNull(Collection<LocalDate>)` dans `CaseDeadlineRepository`

---

## Plan de test

### Tests unitaires (`DeadlineAlertServiceTest`)

- [ ] U-01 : délais à J-15 et J-7 → emails envoyés à tous les membres
- [ ] U-02 : aucun délai à ces dates → aucun email
- [ ] U-03 : exception mail pour un membre → log warn, autres membres traités
- [ ] U-04 : délai avec dossier d'un workspace à 2 membres → 2 emails envoyés

### Isolation workspace

- [ ] Non applicable — le job tourne côté serveur, pas de contexte utilisateur

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — nouveau service isolé, pas de modification de flux existant

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

- SF-69-01 (done) — table `case_deadlines` disponible
- SF-65-01 (done) — `EmailService.sendAnalysisDone` — pattern fail-open réutilisé

---

## Notes et décisions

- `@EnableScheduling` déjà présent ou à ajouter sur la classe principale / une config
- Le job est en profil `!test` pour ne pas s'exécuter lors des ITs
- `LocalDate.now()` + `plusDays(15)` et `plusDays(7)` — timezone serveur = Europe/Paris (configuré via `TZ` env var)
- Requête JPA : `findByDueDateInAndCaseFileDeletedAtIsNull` — filtre soft-delete inclus dans la requête
