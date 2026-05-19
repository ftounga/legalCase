# Mini-spec — F-248 / SF-248-01 — Backend désabonnement des emails non-transactionnels

## Identifiant

`F-248 / SF-248-01`

## Feature parente

`F-248` — Désabonnement des emails non-transactionnels (conformité RGPD / LCEN)

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-248-01-backend-email-unsubscribe`

---

## Objectif

Permettre à un utilisateur de se désinscrire (et se réinscrire) des emails non-transactionnels via un endpoint public token-based, et faire respecter cet opt-out par l'envoi des emails d'onboarding et de la newsletter.

---

## Comportement attendu

### Cas nominal

1. **Lien dans les emails** — chaque email non-transactionnel (5 emails d'onboarding F-73 + newsletter mensuelle) se termine par un pied de page : un texte d'explication + l'URL `{frontendUrl}/unsubscribe?token={marketingUnsubscribeToken}`.
2. **Génération du token** — avant de construire le pied de page, le service s'assure que l'utilisateur a un `marketingUnsubscribeToken` : si `null`, il en génère un (`UUID.randomUUID()`) et le persiste. Token stable ensuite.
3. **Désinscription** — `POST /api/v1/public/email/unsubscribe` body `{token}` : le service recherche l'utilisateur par token, positionne `marketingEmailsOptedOut = true`, renvoie `200 {optedOut: true}`. Idempotent (déjà désinscrit → `200` quand même).
4. **Réinscription** — `POST /api/v1/public/email/resubscribe` body `{token}` : positionne `marketingEmailsOptedOut = false`, renvoie `200 {optedOut: false}`.
5. **État** — `GET /api/v1/public/email/subscription-status?token={token}` renvoie `200 {optedOut: boolean}` pour permettre à la page frontend d'être bidirectionnelle.
6. **Respect de l'opt-out** — `EmailService.sendOnboarding(...)` (funnel des 5 emails d'onboarding) et `sendMonthlyNewsletter(...)` court-circuitent l'envoi si `user.marketingEmailsOptedOut == true` (log + return, comme le court-circuit `mailEnabled` existant).
7. **Emails transactionnels inchangés** — `sendEmailVerification`, `sendPasswordReset`, `sendAnalysisDone`, `sendExtractionFailed`, `sendRequalificationAlert`, `sendDeadlineAlert`, `sendInvitation`, `sendContactConfirmation` : ni garde d'opt-out, ni pied de page de désinscription.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|---|---|---|
| `token` absent ou vide dans le body | « Token requis » | 400 |
| `token` mal formé (pas un UUID) | « Token invalide » | 400 |
| `token` bien formé mais inconnu en base | « Lien de désinscription invalide » | 404 |
| Déjà désinscrit lors d'un `unsubscribe` | Succès idempotent | 200 |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier / pays / domaines** — non applicable : feature d'infrastructure email, transversale, aucune logique par domaine ou pays.
- [x] **Autres UI patterns** — backend pur.
- [x] **Autres flows transversaux** — **Navigation** : nouvelle URL publique côté backend sous `/api/v1/public/**`, déjà couverte par la whitelist Spring Security existante (`requestMatchers("/api/v1/public/**").permitAll()`) — **aucune modification de `SecurityConfig`**. **Auth** : endpoint public sans `Principal`, le token est l'autorisation.
- [x] **Tous les emails de `EmailService`** scannés (cf. tableau ci-dessous) pour classer chaque type transactionnel / non-transactionnel.

### Résultat du scan — classification des emails

| Email (`EmailService`) | Catégorie | Garde opt-out | Pied de désinscription |
|---|---|---|---|
| `sendOnboardingWelcome/TipAnalysis/TipShare/BeforeExpiry/Expired` (via `sendOnboarding`) | Non-transactionnel | **Oui** | **Oui** |
| `sendMonthlyNewsletter` | Non-transactionnel | **Oui** | **Oui** |
| `sendEmailVerification`, `sendPasswordReset` | Transactionnel | Non | Non |
| `sendAnalysisDone`, `sendExtractionFailed`, `sendRequalificationAlert`, `sendDeadlineAlert` | Transactionnel | Non | Non |
| `sendInvitation`, `sendContactToTeam`, `sendContactConfirmation` | Transactionnel | Non | Non |
| `sendReferentialAlert/Report/AlertReminder` (alertes internes super-admin) | Transactionnel/opérationnel | Non | Non |

### Décision

- [x] Étendu à toutes les cibles applicables : les 2 funnels non-transactionnels (`sendOnboarding`, `sendMonthlyNewsletter`) reçoivent garde + pied de page ; les transactionnels sont explicitement exclus.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF backend pure, aucun composant décisionnel.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : feature d'infrastructure email, aucun outil décisionnel.

---

## Critères d'acceptation

- [ ] Les 5 emails d'onboarding et la newsletter contiennent en pied de page l'URL `{frontendUrl}/unsubscribe?token=...`.
- [ ] Aucun email transactionnel ne contient de lien de désinscription.
- [ ] `POST /api/v1/public/email/unsubscribe` avec un token valide positionne `marketingEmailsOptedOut=true` et renvoie `200 {optedOut:true}`, sans authentification.
- [ ] Après désinscription, `sendOnboarding*` et `sendMonthlyNewsletter` ne sont plus envoyés à cet utilisateur (court-circuit + log).
- [ ] Après désinscription, un email transactionnel (ex. `sendAnalysisDone`) est **toujours** envoyé.
- [ ] `POST /api/v1/public/email/resubscribe` repositionne `marketingEmailsOptedOut=false`.
- [ ] `GET /api/v1/public/email/subscription-status?token=...` renvoie l'état courant.
- [ ] Un token absent → `400` ; un token inconnu → `404`.
- [ ] Le token est généré une seule fois par utilisateur (stable entre deux emails).

---

## Périmètre

### Hors scope (explicite)

- Frontend (SF-248-02).
- Granularité par type d'email (désabonnement global uniquement).
- Conversion des emails en HTML (restent en texte brut `SimpleMailMessage`).
- Double opt-in à l'inscription, gestion des bounces Brevo.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|---|---|---|
| `marketing_emails_opted_out` | `false` | tout utilisateur reçoit les emails non-transactionnels par défaut |
| `marketing_unsubscribe_token` | `NULL` | généré paresseusement (Java) au premier envoi d'email non-transactionnel |

---

## Contraintes de validation

| Champ | Obligatoire | Format | Unicité | Notes |
|---|---|---|---|---|
| `marketing_emails_opted_out` | Oui | booléen `NOT NULL DEFAULT false` | Non | — |
| `marketing_unsubscribe_token` | Non | UUID | Oui (index unique) | nullable tant que non généré |
| `token` (body endpoint) | Oui | UUID | — | `400` si absent/mal formé |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---|---|---|---|
| POST | `/api/v1/public/email/unsubscribe` | Non (public) | — (token) |
| POST | `/api/v1/public/email/resubscribe` | Non (public) | — (token) |
| GET | `/api/v1/public/email/subscription-status` | Non (public) | — (token) |

**Contrat de réponse (figé pour parallélisation SF-248-02)** :
`200` → `{"optedOut": boolean}` · `400` token absent/mal formé · `404` token inconnu — corps `{"message": string}`.
Pattern endpoint public POST : aligné sur `/api/v1/contact` (déjà public, déjà whitelisté).

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `users` | ALTER (ajout `marketing_emails_opted_out`, `marketing_unsubscribe_token`) | colonnes nouvelles |

### Migration Liquibase

- [x] Oui — `250-add-user-marketing-email-prefs.xml` : ajout `marketing_emails_opted_out` BOOLEAN NOT NULL DEFAULT false + `marketing_unsubscribe_token` UUID nullable + index unique. Réversible (drop columns). Pas de backfill SQL du token (génération paresseuse Java — évite la dépendance à `gen_random_uuid()`/`RANDOM_UUID()` non portable H2↔PostgreSQL).

### Classes impactées

- `User` (entité) — 2 champs `marketingEmailsOptedOut`, `marketingUnsubscribeToken`.
- `EmailService` — garde opt-out dans `sendOnboarding` (privé, funnel) + `sendMonthlyNewsletter` ; helper `unsubscribeFooter(User)` qui assure le token et construit le texte ; dépendance `UserRepository` (ou `UnsubscribeTokenService` dédié) pour persister le token généré.
- `EmailController` (nouveau, `/api/v1/public/email`) — 3 endpoints.
- `EmailSubscriptionService` (nouveau) — recherche par token, bascule opt-out.
- `UserRepository` — méthode `findByMarketingUnsubscribeToken(UUID)`.

---

## Plan de test

### Tests unitaires

- [ ] `EmailSubscriptionService` — unsubscribe token valide → `optedOut=true`.
- [ ] `EmailSubscriptionService` — resubscribe → `optedOut=false`.
- [ ] `EmailSubscriptionService` — token inconnu → 404 ; token absent → 400.
- [ ] `EmailSubscriptionService` — unsubscribe idempotent (2 appels → toujours 200).
- [ ] `EmailService` — `sendOnboardingWelcome` court-circuité si `optedOut=true` (aucun `mailSender.send`, aucun `EmailSend` persisté).
- [ ] `EmailService` — `sendMonthlyNewsletter` court-circuité si `optedOut=true`.
- [ ] `EmailService` — un email transactionnel (`sendAnalysisDone`) **envoyé** même si `optedOut=true`.
- [ ] `EmailService` — le pied de page contient l'URL `/unsubscribe?token=` et le token est généré si absent.

### Tests d'intégration

- [ ] `POST /api/v1/public/email/unsubscribe` → 200 sans authentification.
- [ ] `POST /api/v1/public/email/unsubscribe` → 400 (token absent) / 404 (token inconnu).
- [ ] `POST /api/v1/public/email/resubscribe` → 200.
- [ ] `GET /api/v1/public/email/subscription-status?token=` → 200 `{optedOut}`.

### Isolation workspace

- [x] Non applicable — la préférence est attachée à `user`, pas à un workspace ; le token isole l'accès (un token ne désinscrit que son porteur).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing** — nouvelle famille d'URL `/api/v1/public/email/**` ; couverte par la whitelist `permitAll` existante `/api/v1/public/**` — vérifier qu'aucun guard global ne l'intercepte (non — pattern identique à `/api/v1/contact`).
- [x] **Auth / Principal** — endpoint public sans `@AuthenticationPrincipal` ; le token fait foi. Aucun endpoint authentifié existant modifié.
- [ ] Workspace context — non.
- [ ] Plans / limites — non.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression |
|---|---|---|
| `EmailService` (tous les `send*`) | ajout d'une garde sur 2 funnels — risque de couper un email à tort | UT : transactionnels toujours envoyés ; non-transactionnels coupés si opt-out |
| `OnboardingEmailScheduler`, `MonthlyNewsletterScheduler` | appellent `EmailService` — comportement inchangé hors opt-out | UT EmailService couvre le court-circuit |
| `SecurityConfig` | **non modifié** — `/api/v1/public/**` déjà whitelisté | — |

### Smoke tests E2E concernés

- [ ] `cd e2e && npm test` — préoccupations Navigation + Auth cochées → smoke tests obligatoires avant push.

---

## Dépendances

### Subfeatures bloquantes

- Aucune. SF-248-02 (frontend) parallélisable — contrat d'API figé ci-dessus.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- Endpoint sous `/api/v1/public/**` → réutilise la whitelist Security existante, zéro modification de `SecurityConfig`.
- Génération paresseuse du token côté Java → migration sans backfill SQL, portable H2/PostgreSQL.
- Emails en texte brut (`SimpleMailMessage`) — le pied de page est du texte, pas du HTML.
- Conformité visée : RGPD art. 21 (droit d'opposition), LCEN art. L34-5 (opt-out prospection électronique).
