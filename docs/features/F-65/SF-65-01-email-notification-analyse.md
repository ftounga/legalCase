# SF-65-01 — Email de notification analyse terminée

## Objectif
Envoyer un email à l'avocat (créateur du dossier) quand une analyse STANDARD ou ENRICHED passe au statut DONE, en complément des notifications SSE existantes pour les sessions fermées.

---

## Comportement nominal

1. L'analyse se termine (`AnalysisStatus.DONE`, `JobType` = `CASE_ANALYSIS` ou `ENRICHED_ANALYSIS`)
2. L'event `AnalysisStatusEvent` est déjà publié après commit par `CaseAnalysisService` et `EnrichedAnalysisService`
3. `AnalysisNotificationService` écoute cet event via `@EventListener`
4. Il récupère le titre du dossier et l'email du créateur via deux queries JPQL sur `CaseFileRepository`
5. Il appelle `EmailService.sendAnalysisDone(email, title, type)` — ajout d'une nouvelle méthode
6. L'email est envoyé via `JavaMailSender` (même canal que les emails existants)
7. Si `app.mail.enabled=false` (dev/test) : log debug, aucun envoi

---

## Cas d'erreur

| Cas | Comportement attendu |
|-----|---------------------|
| `mailEnabled = false` | Log debug, retour silencieux |
| CaseFile introuvable | Log warn, aucun envoi |
| Email introuvable (créateur sans email) | Log warn, aucun envoi |
| `MailException` à l'envoi | Log warn, exception avalée (non bloquante) |
| Status ≠ DONE | Aucune action |
| JobType ≠ CASE_ANALYSIS / ENRICHED_ANALYSIS | Aucune action |

---

## Critères d'acceptation

- [ ] Email envoyé quand `JobType.CASE_ANALYSIS` + `AnalysisStatus.DONE`
- [ ] Email envoyé quand `JobType.ENRICHED_ANALYSIS` + `AnalysisStatus.DONE`
- [ ] Pas d'email si `AnalysisStatus.FAILED`
- [ ] Sujet : `Analyse terminée — [titre du dossier] — AI LegalCase`
- [ ] Corps : titre du dossier, type d'analyse, lien vers le dossier (`{frontendUrl}/case-files/{id}`)
- [ ] Si `mailEnabled=false` : aucun envoi, log debug
- [ ] Exception mail avalée — ne bloque pas le pipeline IA

---

## Plan de test

### Unitaires (AnalysisNotificationServiceTest)
- U-01 : DONE + CASE_ANALYSIS → `sendAnalysisDone` appelé
- U-02 : DONE + ENRICHED_ANALYSIS → `sendAnalysisDone` appelé
- U-03 : FAILED → `sendAnalysisDone` non appelé
- U-04 : DONE + DOCUMENT_ANALYSIS → `sendAnalysisDone` non appelé
- U-05 : CaseFile introuvable → aucun envoi, pas d'exception propagée

### Unitaires (EmailServiceTest — méthode sendAnalysisDone)
- U-06 : `mailEnabled=true` → message envoyé avec bon sujet + lien
- U-07 : `mailEnabled=false` → `mailSender.send()` non appelé

---

## Composants / fichiers impactés

| Fichier | Action |
|---------|--------|
| `analysis/AnalysisNotificationService.java` | Nouveau |
| `analysis/AnalysisNotificationServiceTest.java` | Nouveau |
| `workspace/EmailService.java` | Ajout `sendAnalysisDone()` |
| `workspace/EmailServiceTest.java` | Nouveau (ou ajout si existant) |
| `casefile/CaseFileRepository.java` | Ajout `findTitleById` + `findCreatorEmailById` |

---

## Hors périmètre

- Opt-out par utilisateur (préférences de notification) — V3
- Email aux membres du workspace autres que le créateur — V3
- Template HTML riche — V3 (texte brut suffisant)
- Notification pour `DOCUMENT_ANALYSIS` (trop granulaire, bruit)
