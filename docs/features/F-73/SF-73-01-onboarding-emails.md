# SF-73-01 — Séquence email onboarding automatique

## Objectif
Envoyer automatiquement 5 emails de bienvenue/activation aux nouveaux inscrits sur 15 jours (J+0 à J+15).

## Comportement nominal

| Délai | Type | Déclencheur |
|-------|------|-------------|
| J+0 | Bienvenue | Immédiat à la création du workspace |
| J+2 | Tip — Analyser un dossier | Scheduler nightly 8h |
| J+5 | Tip — Partager avec un client | Scheduler nightly 8h |
| J+12 | Avant expiration trial | Scheduler nightly 8h |
| J+15 | Trial expiré | Scheduler nightly 8h |

Chaque email est envoyé **une seule fois par utilisateur** (déduplication via table `email_sends`).

## Cas d'erreur
- Mail désactivé (`app.mail.enabled=false`) → log debug, pas d'envoi, pas d'insertion dans `email_sends`
- Echec SMTP → log warn, pas de crash (fail-open), pas d'insertion (retry possible au prochain run)
- `email_sends` violation contrainte unique → log info, skip silencieux

## Critères d'acceptation
1. À la création d'un workspace, `ONBOARDING_WELCOME` est inséré dans `email_sends` et l'email J+0 est envoyé
2. Le scheduler J+2 ne renvoie pas l'email si `email_sends` contient déjà `(user_id, ONBOARDING_TIP_ANALYSIS)`
3. Le scheduler s'exécute à 8h chaque jour (`cron = "0 0 8 * * *"`)
4. Aucun email n'est envoyé si `app.mail.enabled=false`
5. Un utilisateur créé le jour J reçoit J+2 le jour J+2 (basé sur `users.created_at`)

## Plan de test

### Unitaires (`OnboardingEmailSchedulerTest`)
- T1 : sendJ2Tips — envoie mail si utilisateur créé il y a exactement 2 jours, pas d'entrée existante
- T2 : sendJ2Tips — skip si entrée existante dans email_sends
- T3 : sendJ5Tips — même logique pour 5 jours
- T4 : sendJ12BeforeExpiry — 12 jours
- T5 : sendJ15Expired — 15 jours

### Unitaires (`EmailServiceOnboardingTest`)
- T6 : sendOnboardingWelcome — vérifie sujet et corps email
- T7 : sendOnboardingTipAnalysis — vérifie sujet et corps
- T8 : sendOnboardingTipShare — vérifie sujet et corps
- T9 : sendOnboardingBeforeExpiry — vérifie sujet et corps
- T10 : sendOnboardingExpired — vérifie sujet et corps

### Isolation workspace
- Chaque entrée `email_sends` est liée à `user_id` — pas de fuite inter-workspace

## Tables / endpoints / composants impactés
- **Nouvelle table** : `email_sends (id, user_id, email_type, sent_at)` — unique `(user_id, email_type)`
- **Nouveau bean** : `EmailSend` (JPA entity), `EmailSendRepository`
- **Modifié** : `EmailService` — 5 nouvelles méthodes `sendOnboarding*`
- **Modifié** : `WorkspaceService.createDefaultWorkspace()` — hook J+0
- **Nouveau bean** : `OnboardingEmailScheduler` (`@Scheduled`)

## Hors périmètre
- Emails HTML / templates Thymeleaf — plain text uniquement en V1
- Unsubscribe / opt-out — V2
- Tracking ouverture / clic — V2
- Internationalisation — français uniquement
