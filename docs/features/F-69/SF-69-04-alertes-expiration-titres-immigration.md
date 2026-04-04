# Mini-spec — F-69 / SF-69-04 — Alertes expiration titres de séjour (DROIT_IMMIGRATION)

---

## Identifiant

`F-69 / SF-69-04`

## Feature parente

`F-69` — Suivi des délais légaux

## Statut

`ready`

## Date de création

2026-04-04

## Branche Git

`feat/SF-69-04-alertes-expiration-titres-immigration`

---

## Objectif

Étendre `case_deadlines` avec des alertes multi-seuils (J-90/J-30/J-7) et un type de document, scopés DROIT_IMMIGRATION, pour notifier l'avocat avant l'expiration d'un titre de séjour.

---

## Comportement attendu

### Cas nominal

**Pipeline IA :**
- Le prompt DROIT_IMMIGRATION est étendu pour extraire `date_expiration_titre` + `type_titre_sejour`
- `StatutoryDeadlineService` (ou un service dédié) crée un `CaseDeadline` avec :
  - `source = "STATUTORY"`, `label = "Expiration {type}"`, `dueDate = date_expiration`
  - `alertThresholds = "90,30,7"`, `documentType = "TITRE_SEJOUR"`
- Uniquement si `caseFile.legalDomain == DROIT_IMMIGRATION`

**Scheduler :**
- Le cron daily `DeadlineAlertService` est étendu : pour les délais ayant `alertThresholds` non null, il calcule si aujourd'hui = dueDate - seuil (90, 30 ou 7 jours)
- Pour chaque seuil atteint : envoie l'email si pas déjà envoyé (déduplication via `deadline_alert_sends`)
- Les délais sans `alertThresholds` conservent le comportement actuel (J-15/J-7)

**Déduplication :**
- Table `deadline_alert_sends` (deadline_id, threshold_days, sent_at) — UNIQUE(deadline_id, threshold_days)
- Si la ligne existe → skip silencieux

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `date_expiration_titre` absente du JSON IA | Aucun délai créé, pas d'exception |
| Email introuvable pour le membre | Log warn, skip (comportement actuel) |
| Erreur envoi email | Log warn, continue les autres membres |

---

## Critères d'acceptation

- [ ] Deux nouvelles colonnes nullable sur `case_deadlines` : `alert_thresholds` VARCHAR(50), `document_type` VARCHAR(50)
- [ ] Table `deadline_alert_sends` créée (migration 046)
- [ ] Extraction IA uniquement pour DROIT_IMMIGRATION
- [ ] CaseDeadline créé avec `alertThresholds="90,30,7"` et `documentType="TITRE_SEJOUR"`
- [ ] Scheduler envoie l'email au seuil J-90, J-30, J-7
- [ ] Pas de doublon d'envoi (déduplication)
- [ ] Délais sans `alertThresholds` : comportement inchangé

---

## Périmètre

### Hors scope (explicite)

- UI (badge d'urgence dans la section délais) — prévu SF-69-05
- F-IM-03 (calendrier préfectoral) — subfeature séparée
- Alertes pour DROIT_DU_TRAVAIL ou DROIT_FAMILLE

---

## Contraintes de validation

| Champ | Règle |
|-------|-------|
| `alertThresholds` | Format CSV entiers positifs ex: "90,30,7" — nullable |
| `documentType` | VARCHAR(50) nullable |
| UNIQUE `deadline_alert_sends` | (deadline_id, threshold_days) |

---

## Technique

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `case_deadlines` | ALTER — ajout 2 colonnes nullable | `alert_thresholds` VARCHAR(50), `document_type` VARCHAR(50) |
| `deadline_alert_sends` | CREATE | id UUID PK, deadline_id FK, threshold_days INT, sent_at TIMESTAMPTZ |

### Migration Liquibase

- `046-extend-case-deadlines-alert-thresholds.xml`

### Composants backend

- `CaseDeadline.java` — 2 nouveaux champs
- `CaseDeadlineRepository` — requête pour deadlines avec alertThresholds non null
- `DeadlineAlertService` — logique multi-seuils + déduplication
- `DeadlineAlertSend.java` + `DeadlineAlertSendRepository.java` — entité déduplication
- `LegalDomainPromptBuilder` — extraction `date_expiration_titre` + `type_titre_sejour` pour DROIT_IMMIGRATION
- `StatutoryDeadlineService` — création du CaseDeadline immigration si domaine = IMMIGRATION

---

## Plan de test

### Tests unitaires

- [ ] `DeadlineAlertService` — délai avec alertThresholds → email envoyé au seuil J-90
- [ ] `DeadlineAlertService` — seuil déjà envoyé → skip (déduplication)
- [ ] `DeadlineAlertService` — délai sans alertThresholds → comportement J-15/J-7 inchangé
- [ ] `StatutoryDeadlineService` — dossier DROIT_IMMIGRATION avec date_expiration → CaseDeadline créé avec alertThresholds
- [ ] `StatutoryDeadlineService` — dossier DROIT_DU_TRAVAIL → pas de CaseDeadline expiration

### Isolation workspace

- Applicable — vérifiée par le workspace du CaseFile (comportement hérité de F-69)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Auth / Principal** — aucune modification du Principal
- [x] **Aucune préoccupation transversale** — extension du scheduler existant, pas de nouvelle route

### Smoke tests E2E concernés

- Aucun (pas de modification de navigation ni de guards)

---

## Dépendances

### Subfeatures bloquantes

- SF-69-01/02/03 — statut : done

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- `alertThresholds` stocké en VARCHAR CSV ("90,30,7") pour rester simple sans nouvelle table de jonction
- Déduplication par table `deadline_alert_sends` plutôt que `email_sends` : sémantique différente (seuil vs type d'email onboarding)
- Le scheduler existant (J-15/J-7) est conservé sans modification pour les délais sans `alertThresholds`
- Scopé DROIT_IMMIGRATION dans le prompt et dans la création du délai
