# Mini-spec — F-143 / SF-143-01 — Activation complète du domaine `legalcase.fr` (dual-host fonctionnel)

## Identifiant

`F-143 / SF-143-01`

## Feature parente

`F-143` — Migration nom de domaine `legalcase.fr` (rebrand de marque "AI LegalCase" → "LegalCase" hors périmètre F-143 actuel — décision opérateur 2026-05-01).

## Statut

`Terminée` (mergée PR #743 + déployée prod 2026-05-02, validation bout-en-bout opérateur OK)

## Date de création

2026-05-02

## Branche Git

`feat/SF-143-01-domaine-fonctionnel`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Rendre **`legalcase.fr` (production)** et **`staging.legalcase.fr` (staging)** **100 % fonctionnels en parallèle** des hosts existants `*.ng-itconsulting.com` : login OAuth Google, paiement Stripe, emails transactionnels, dashboard, partage de dossier, contact — **tout marche sur les 4 hosts simultanément**, sans aucune coupure de service ni redirection canonique.

> La bascule canonique (301 ancien→nouveau, désactivation ancien host, SEO, CGU) est traitée dans la SF suivante **SF-143-02**.

---

## Comportement attendu

### Cas nominal — après merge de cette SF

1. Un visiteur tape `https://legalcase.fr` → frontend Angular production se charge avec certificat Let's Encrypt valide.
2. Il clique « Se connecter avec Google » → redirection Google → consentement → callback `https://legalcase.fr/login/oauth2/code/google` → Spring Security crée la session → redirection vers `https://legalcase.fr/dashboard` (le **host d'origine**, pas un host hardcodé).
3. Il accède à `/billing/checkout` → Stripe Checkout ouvre dans un nouvel onglet, success URL = `https://legalcase.fr/billing/success` → retour OK.
4. Stripe envoie un webhook → reçu par l'**ancien** endpoint `https://legalcase.ng-itconsulting.com/api/v1/stripe/webhook` (inchangé dans cette SF) → signature validée → événement traité. La migration du webhook vers le nouveau host est traitée en SF-143-02 (rotation simple).
5. L'utilisateur invite un collègue → email transactionnel envoyé → lien dans l'email pointe vers `https://legalcase.fr/accept-invitation/...` → l'invité clique → accepte → arrive sur le nouveau host.
6. **Symétriquement**, un visiteur qui arrive sur `https://legalcase.ng-itconsulting.com` continue à pouvoir tout faire : login, Stripe, accept-invitation. Cohérent à 100 %.
7. Les CORS acceptent les **4 hosts** (`legalcase.fr`, `staging.legalcase.fr`, `legalcase.ng-itconsulting.com`, `staging.legalcase.ng-itconsulting.com`).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| DNS non propagé au moment du déploiement Ingress | cert-manager retry HTTP-01, l'ancien host continue de servir. Documenter dans la PR : ne pas paniquer, attendre la propagation. |
| Cert Let's Encrypt en `False` après 5 min | `kubectl describe certificate -n staging` → identifier le challenge bloqué ; vérifier que le DNS pointe bien vers le LB du cluster. |
| Webhook Stripe sur l'ancien host (inchangé dans cette SF) | **OK** : le webhook Stripe reste configuré sur `legalcase.ng-itconsulting.com/api/v1/stripe/webhook`. L'ancien host reste actif → réception OK. La migration du webhook est traitée dans SF-143-02 (rotation simple `STRIPE_WEBHOOK_SECRET`, zéro code Java). |
| Login OAuth depuis `legalcase.fr` mais Google Console pas mis à jour | `redirect_uri_mismatch` côté Google — **bloquant**. La SF n'est pas mergeable tant que les redirect URIs Google ne sont pas ajoutées. |
| Visiteur de `legalcase.ng-itconsulting.com` clique sur un lien dans un nouvel email (qui pointe vers `legalcase.fr`) | OK fonctionnel : le nouveau domaine répond. Comportement attendu (le `frontend-url` bascule définitivement vers `legalcase.fr` dans cette SF). |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — *Non applicable* : SF infra/auth/billing/email, ne touche aucun outil décisionnel.
- [x] **Autres pays** — *Non applicable* : DNS/Ingress/OAuth sans dépendance pays.
- [x] **Autres domaines** — *Non applicable* : sans dépendance domaine métier.
- [x] **Autres UI patterns** — *Non applicable* : aucune modification UI dans cette SF (le rebrand visuel "AI LegalCase" → "LegalCase" est hors périmètre).
- [x] **Flows transversaux : auth + email + workspace + plans + Stripe** :
  - Auth OAuth Google → redirect URIs ajoutés côté Google + `defaultSuccessUrl` remplacé par `HostAwareSuccessHandler` côté Spring.
  - Stripe webhook → **inchangé dans cette SF** (reste sur l'ancien host, l'ancien host reste actif). Migration différée à SF-143-02 (rotation simple, zéro code Java). Stripe Checkout success/cancel URLs deviennent dynamiques via `frontend-url` (côté backend) — succès cohérent quel que soit le host d'origine.
  - Email → `frontend-url` bascule vers `legalcase.fr` → tous les liens emails (invitation, partage, top-up, référentiel) pointent vers le nouveau domaine.
  - Workspace context — non touché.
  - Plans / limites — non touché.
  - CORS — étendu aux 4 hosts via `WebConfig`.

### Niveaux de vérification couverts

- [x] **K8s Ingress** : ajout des 2 nouveaux hosts dans `tls.hosts` + `rules`.
- [x] **K8s ConfigMap** : `APP_FRONTEND_URL` mis à jour dans staging + production overlays.
- [x] **Spring `application.yml`** : ajout `app.allowed-frontend-urls` (CSV des hosts autorisés).
- [x] **Spring `WebConfig`** : CORS multi-origin (allowlist 4 hosts).
- [x] **Spring `SecurityConfig`** : `defaultSuccessUrl` remplacé par un `AuthenticationSuccessHandler` qui redirige vers le host de la requête.
- [x] **Spring `StripeWebhookController`** : **inchangé** dans cette SF — la migration webhook est SF-143-02.
- [x] **Frontend `index.html`, `landing.component.ts`, `legal-content.ts`, `super-admin.component.html`** : **non modifiés** dans cette SF (canonical/og:url/CGU/n8n link → traités en SF-143-02 lors de la bascule canonique 301, pour transférer le PageRank proprement).
- [x] **Frontend `environment.*.ts`** : aucune URL frontend-relative n'est hardcodée pour le backend (les appels API utilisent des paths `/api/...`). À vérifier en review.

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Aucun nouveau pattern UI** introduit.
- [x] **Pattern technique introduit** : `HostAwareSuccessHandler` (Spring `AuthenticationSuccessHandler` qui redirige vers le host de la requête). Limité au flow OAuth login — un seul flow OAuth dans l'app. Pas de service partagé créé.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|--------------|------------|
| `k8s/base/ingress/ingress.yaml` | Oui | Intégré (ajout `staging.legalcase.fr`) |
| `k8s/overlays/production/ingress-patch.yaml` | Oui | Intégré (ajout `legalcase.fr`) |
| `k8s/overlays/staging/kustomization.yaml` (`APP_FRONTEND_URL`) | Oui | Intégré (bascule vers `https://staging.legalcase.fr`) |
| `k8s/overlays/production/kustomization.yaml` (`APP_FRONTEND_URL`) | Oui | Intégré (bascule vers `https://legalcase.fr`) |
| Spring `WebConfig` (CORS) | Oui | Intégré (allowlist 4 hosts via `app.allowed-frontend-urls`) |
| Spring `SecurityConfig` (`defaultSuccessUrl`) | Oui | Intégré (`HostAwareSuccessHandler` custom) |
| Spring `StripeWebhookController` | **Non** dans cette SF | Webhook reste configuré sur ancien host. **SF-143-02** : update URL côté Stripe Dashboard + rotation simple `STRIPE_WEBHOOK_SECRET` (zéro code Java). |
| Spring `application.yml` (`app.allowed-frontend-urls`) | Oui | Intégré |
| Google Cloud Console (redirect URIs + JS origins) | Oui | **Action opérateur intégrée** (étape 2 du plan opérateur) |
| Stripe Dashboard | **Non** dans cette SF | Migration webhook URL → SF-143-02 |
| Microsoft OAuth | **Non** | Pas configuré actuellement dans `application.yml` (seul Google présent) — hors scope. À ajouter dans une feature dédiée si Microsoft est activé plus tard. |
| Frontend `index.html` SEO meta (canonical, og:url, og:image, twitter:image) | Oui — **différé** | **SF-143-02** : reportée à la bascule canonique 301 pour transférer le PageRank proprement. |
| Frontend `landing.component.ts` (og:url, JSON-LD `url`) | Oui — **différé** | **SF-143-02** |
| Frontend `legal-content.ts` (URL service dans CGU) | Oui — **différé** | **SF-143-02** (bascule juridique : le « service » désigné par les CGU change de URL) |
| Frontend `super-admin.component.html` (lien `n8n.ng-itconsulting.com`) | **Non** | Outil interne hors rebrand (n8n reste sur le sous-domaine `ng-itconsulting`). |
| `app.contact.team-email` (`ai-legalcase@ng-itconsulting.com`) | **Non** | Adresse email de contact (≠ web). Migration vers `contact@legalcase.fr` requiert config DNS MX + boîte mail — feature séparée hors F-143. |
| `MAIL_FROM` (variable env) | **Non** | Idem — adresse expéditeur email, hors web. |
| `rabbitmq.legalcase.ng-itconsulting.com` (Ingress admin interne) | **Non** | Admin RabbitMQ, non client-facing, hors rebrand. |
| `environment.*.ts` (`rabbitmqUrl`) | **Non** | Pointage admin RabbitMQ, hors rebrand. |

### Décision

- [x] **Étendu à toutes les cibles applicables dans cette subfeature** (tout ce qui est nécessaire au fonctionnement bout-en-bout sur les 2 nouveaux hosts).
- [x] **Subfeature parallèle créée pour les cibles différées** : SF-143-02 (bascule canonique 301 + SEO + CGU + désactivation ancien host).
- [x] **Non applicable** justifié explicitement pour : Microsoft OAuth, n8n admin, emails de contact, RabbitMQ admin.

---

## Critères d'acceptation

### Infrastructure

- [ ] **C1** — `kubectl get certificate -n staging` → `legalcase-tls` couvre `staging.legalcase.fr` ET `staging.legalcase.ng-itconsulting.com`, status `True`.
- [ ] **C2** — `kubectl get certificate -n production` → `legalcase-tls-prod` couvre `legalcase.fr` ET `legalcase.ng-itconsulting.com`, status `True`.
- [ ] **C3** — Les 4 hosts répondent `HTTP/2 200` sur `/` (frontend) et `/api/actuator/health` (backend).

### Auth (OAuth Google)

- [ ] **C4** — Login Google démarré depuis `https://staging.legalcase.fr/login` → redirection `accounts.google.com` OK → callback `staging.legalcase.fr/login/oauth2/code/google` → session créée → redirection finale vers `https://staging.legalcase.fr/dashboard` (PAS vers `staging.legalcase.ng-itconsulting.com`).
- [ ] **C5** — Login Google démarré depuis `https://staging.legalcase.ng-itconsulting.com/login` → redirection finale vers `https://staging.legalcase.ng-itconsulting.com/dashboard` (non-régression : ancien host conserve son comportement).
- [ ] **C6** — Logout sur `staging.legalcase.fr` → cookie session invalidé → tentative d'accès `/dashboard` → redirigé vers `/login`.

### Billing (Stripe)

> Note : le **webhook Stripe est inchangé dans cette SF** (continue à pointer sur l'ancien host, qui reste actif). Migration webhook → SF-143-02.

- [ ] **C7** — Checkout Stripe démarré depuis `staging.legalcase.fr/billing` → success URL pointe vers le **host d'origine** (`https://staging.legalcase.fr/billing/success`) car les services `StripeCheckoutService` / `TopupCheckoutService` utilisent `frontend-url`. **Pré-requis** : `frontend-url` côté staging = `https://staging.legalcase.fr` après bascule ConfigMap (cette SF). L'utilisateur revient sur le bon host après paiement.
- [ ] **C8** — Webhook Stripe TEST envoyé sur l'ancien endpoint `staging.legalcase.ng-itconsulting.com/api/v1/stripe/webhook` (URL configurée côté Stripe, inchangée) → événement traité avec succès (non-régression).
- [ ] **C9** — Vérifier qu'il n'y a **PAS** de tentative d'envoi de webhook vers `legalcase.fr` (l'URL côté Stripe Dashboard reste l'ancien host). Sinon documenter dans la PR comme bug à investiguer.

### Emails transactionnels

- [ ] **C11** — Invitation envoyée depuis `staging.legalcase.fr` → email reçu → lien `accept-invitation` pointe vers `https://staging.legalcase.fr/accept-invitation/...`.
- [ ] **C12** — Invitation envoyée depuis `staging.legalcase.ng-itconsulting.com` → lien dans l'email pointe vers `https://staging.legalcase.fr/accept-invitation/...` aussi (car `frontend-url` est désormais le NOUVEAU host pour tout le monde) — **comportement attendu et documenté**.
- [ ] **C13** — Lien d'un ancien email (envoyé avant cette SF, pointe vers ancien host) → l'ancien host répond toujours (dual-host actif) → l'invité accepte sans erreur.

### CORS

- [ ] **C14** — Requête `XMLHttpRequest` depuis `https://legalcase.fr` vers `https://legalcase.fr/api/...` → succès (Access-Control-Allow-Origin OK).
- [ ] **C15** — Requête depuis `https://legalcase.ng-itconsulting.com` vers `/api/...` → succès (non-régression).
- [ ] **C16** — Requête depuis `https://random-domain.com` vers `/api/...` → bloquée CORS (sécurité préservée).

### Tests automatisés

- [ ] **C17** — Tests unitaires : `WebConfig` allowlist multi-hosts (3 cas : origine autorisée, origine non autorisée, localhost dev), `HostAwareSuccessHandler` redirige selon le `Host:` de la requête sauvegardée (5 cas : 4 hosts autorisés + 1 host hostile fallback).
- [ ] **C18** — Tests d'intégration : OAuth login flow complet (mock Google) sur les 4 hosts → URL finale différente selon le host d'origine.
- [ ] **C19** — Smoke E2E `e2e/smoke/auth.spec.ts`, `workspace.spec.ts`, `navigation.spec.ts` continuent à passer sur `staging.legalcase.ng-itconsulting.com` (baseURL inchangée).

---

## Périmètre

### Hors scope (explicite)

- **Migration webhook Stripe** vers `legalcase.fr/api/v1/stripe/webhook` → **SF-143-02** (rotation simple URL Dashboard + `STRIPE_WEBHOOK_SECRET`).
- **Bascule canonique 301** ancien → nouveau host → **SF-143-02**.
- **SEO meta** (canonical, og:url, og:image, twitter:image dans `index.html` et `landing.component.ts`) → **SF-143-02**.
- **CGU / mentions légales** (URL du service dans `legal-content.ts`) → **SF-143-02**.
- **Update docs** (`PRODUCT_SPEC.md`, `CLAUDE.md`, `README.md`, marketing) → **SF-143-02**.
- **Externes** (LinkedIn page, Google Business) → **SF-143-02**.
- **Désactivation progressive ancien host** (après 2-4 semaines de validation) → **SF-143-02**.
- **Rebrand visuel "AI LegalCase" → "LegalCase"** → reporté hors F-143 sur décision opérateur 2026-05-01.
- **Microsoft OAuth** : non configuré dans `application.yml`, hors scope.
- **Migration emails** (`contact@legalcase.fr`, `noreply@legalcase.fr`) → hors scope (DNS MX + boîte mail dédiée).
- **Migration `n8n.ng-itconsulting.com`** : outil admin interne, hors rebrand.
- **Migration `rabbitmq.*.ng-itconsulting.com`** : Ingress admin interne, hors rebrand.

---

## Impact par domaine métier

> **Transversale infrastructure / auth / billing / email**, aucune adaptation par domaine métier (DROIT_DU_TRAVAIL / DROIT_IMMIGRATION / DROIT_FAMILLE) ni par pays (France / Belgique). Le code applicatif métier (pipeline IA, outils décisionnels, référentiels) n'est pas touché.

---

## Valeurs initiales

> Non applicable — pas de création d'entité métier, pas de migration Liquibase.

---

## Contraintes de validation

| Champ / config | Règle |
|---------------|-------|
| `app.allowed-frontend-urls` | Liste d'URLs `https://...` séparées par virgule. Au moins une valeur. Pas de wildcard. Consommée par CORS (`WebConfig`) et par `HostAwareSuccessHandler` (allowlist anti-open-redirect). |
| `app.frontend-url` | URL canonique du host **principal** (utilisée pour les liens dans les emails et pour le fallback `HostAwareSuccessHandler`). Doit faire partie de `app.allowed-frontend-urls`. |

---

## Technique

### Endpoints

> Aucun nouvel endpoint applicatif. Les endpoints existants deviennent accessibles via 2 hosts supplémentaires.

### Tables impactées

> Aucune.

### Migration Liquibase

- [ ] Oui
- [x] **Non applicable**.

### Composants Angular (si applicable)

> Aucun.

### Fichiers modifiés

#### K8s

| Fichier | Modification |
|---------|--------------|
| `k8s/base/ingress/ingress.yaml` | Ajouter `staging.legalcase.fr` dans `tls.hosts` + 2e bloc `- host: staging.legalcase.fr` dans `rules` (4 paths : `/api`, `/oauth2`, `/login/oauth2`, `/`). |
| `k8s/overlays/production/ingress-patch.yaml` | Ajouter `legalcase.fr` dans `tls.hosts` + 2e bloc `- host: legalcase.fr` dans `rules` (4 paths). |
| `k8s/overlays/staging/kustomization.yaml` | `APP_FRONTEND_URL=https://staging.legalcase.fr` (bascule). Ajouter `APP_ALLOWED_FRONTEND_URLS=https://staging.legalcase.fr,https://staging.legalcase.ng-itconsulting.com`. |
| `k8s/overlays/production/kustomization.yaml` | `APP_FRONTEND_URL=https://legalcase.fr` (bascule). Ajouter `APP_ALLOWED_FRONTEND_URLS=https://legalcase.fr,https://legalcase.ng-itconsulting.com`. |

#### Backend Spring

| Fichier | Modification |
|---------|--------------|
| `backend/src/main/resources/application.yml` | Ajouter `app.allowed-frontend-urls: ${APP_ALLOWED_FRONTEND_URLS:${app.frontend-url}}`. |
| `backend/src/main/java/fr/ailegalcase/shared/config/WebConfig.java` | Remplacer `allowedOrigins(frontendUrl, "http://localhost:4200")` par parsing de `${app.allowed-frontend-urls}` (split CSV) + ajout `http://localhost:4200` côté dev. |
| `backend/src/main/java/fr/ailegalcase/auth/SecurityConfig.java` | Remplacer `defaultSuccessUrl(frontendUrl + "/dashboard", true)` par `successHandler(new HostAwareSuccessHandler(allowedFrontendUrls, frontendUrl))`. |
| `backend/src/main/java/fr/ailegalcase/auth/HostAwareSuccessHandler.java` (**nouveau**) | `AuthenticationSuccessHandler` qui calcule la redirection à partir du `Host` header de la `SavedRequest` (stockée dans `HttpSession` par Spring Security lors du redirect vers Google) ; vérifie que le host extrait fait partie de `allowed-frontend-urls` (allowlist stricte anti-open-redirect) ; fallback `frontend-url` sinon. |

#### Frontend

> **Aucune modification frontend dans cette SF**. SEO/canonical/CGU/landing → SF-143-02. À vérifier en review : `grep "ng-itconsulting" frontend/src/` ne doit pas faire apparaître d'URL bloquante (liens internes API utilisent des paths relatifs, OK).

### Décisions techniques

1. **`HostAwareSuccessHandler`** : Spring Security stocke la `SavedRequest` (qui inclut le host d'origine) dans `HttpSession` avant la redirection vers Google. Le handler la récupère via `HttpSessionRequestCache` ; si le host extrait fait partie de `allowed-frontend-urls`, redirection vers `<host>/dashboard` ; sinon fallback vers `frontend-url`.
2. **Stripe webhook inchangé** : reste configuré sur `legalcase.ng-itconsulting.com` côté Dashboard. L'ancien host reste actif → réception OK. Migration différée à SF-143-02 (rotation simple URL Dashboard + `STRIPE_WEBHOOK_SECRET`, zéro code Java).
3. **`frontend-url` bascule immédiate** : tous les emails partants (invitations, partage, top-up, référentiel) pointent dès le merge vers `legalcase.fr`. Les anciens emails restent fonctionnels (ancien host actif). Les Stripe Checkout success/cancel URLs (générées via `frontend-url` côté backend) pointent aussi vers le nouveau host.
4. **CORS multi-origin** : `app.allowed-frontend-urls` consommé par `WebConfig` ET `HostAwareSuccessHandler` (cohérence : seuls les hosts CORS-autorisés peuvent recevoir une redirection après login).

---

## Plan opérateur (actions hors code)

> **À exécuter dans l'ordre exact ci-dessous, AVANT le déploiement staging.**

### Étape 1 — DNS (registrar OVH/Gandi)

1. Récupérer le hostname du LB ingress-nginx du cluster :
   ```bash
   aws eks update-kubeconfig --region eu-west-3 --name legalcase-shared
   kubectl get svc -n ingress-nginx ingress-nginx-controller \
     -o jsonpath='{.status.loadBalancer.ingress[0].hostname}'
   ```
2. Créer chez le registrar :
   - **CNAME** `staging.legalcase.fr` → `<hostname-LB-AWS>`
   - **ALIAS** ou **A records** (IPs résolues du hostname AWS) pour `legalcase.fr` (apex) selon ce que supporte le registrar.
3. Vérifier propagation : `dig staging.legalcase.fr +short` et `dig legalcase.fr +short`. Attendre que ce soit OK avant l'étape suivante.

### Étape 2 — Google Cloud Console (OAuth)

1. APIs & Services → Credentials → OAuth 2.0 Client IDs → édition du client `legalcase-prod` (et `legalcase-staging` si client distinct).
2. **Authorized redirect URIs** — ajouter (sans retirer les anciennes) :
   - `https://legalcase.fr/login/oauth2/code/google`
   - `https://staging.legalcase.fr/login/oauth2/code/google`
3. **Authorized JavaScript origins** — ajouter :
   - `https://legalcase.fr`
   - `https://staging.legalcase.fr`
4. Save. La propagation Google prend ~5 min.

### Étape 3 — Stripe Dashboard (PAS de changement dans cette SF)

> **Aucune action Stripe requise pour SF-143-01.** Le webhook reste configuré sur `legalcase.ng-itconsulting.com` côté Stripe Dashboard. L'ancien host continue à recevoir les webhooks normalement.
>
> La migration du webhook Stripe est traitée dans **SF-143-02** : update URL côté Dashboard + rotation `STRIPE_WEBHOOK_SECRET` côté Secrets K8s (zéro code Java).

### Étape 4 — Déploiement

1. Merger SF-143-01 sur `master`.
2. Workflow CI/CD `backend.yml` + `frontend.yml` → staging.
3. Vérifier rollout : `kubectl rollout status deployment/legalcase-backend -n staging`.
4. Exécuter les tests manuels T1-T8 (voir Plan de test).
5. Si OK staging → déploiement production via `deploy-production.yml` avec confirmation `PRODUCTION`.

---

## Plan de test

### Tests unitaires (backend)

- [ ] `WebConfigTest` — `addCorsMappings` autorise les origines configurées (parsing CSV) + localhost dev (3 cas : origine listée OK, origine non listée KO, localhost dev OK).
- [ ] `HostAwareSuccessHandlerTest` :
  - cas 1 : `Host: staging.legalcase.fr` → redirection `https://staging.legalcase.fr/dashboard`
  - cas 2 : `Host: staging.legalcase.ng-itconsulting.com` → redirection `https://staging.legalcase.ng-itconsulting.com/dashboard`
  - cas 3 : `Host: legalcase.fr` → redirection `https://legalcase.fr/dashboard`
  - cas 4 : `Host: legalcase.ng-itconsulting.com` → redirection `https://legalcase.ng-itconsulting.com/dashboard`
  - cas 5 : `Host: malicious.example.com` → fallback `frontend-url/dashboard` (pas de redirect ouvert)

### Tests d'intégration (backend)

- [ ] `OAuth2LoginIntegrationTest` (mock Google) — 4 cas hosts → URL finale différente.

### Tests manuels post-déploiement

#### Staging (obligatoire avant production)

- [ ] **T1** — `curl -I https://staging.legalcase.fr/` → HTTP/2 200, cert Let's Encrypt valide.
- [ ] **T2** — `curl -I https://staging.legalcase.fr/api/actuator/health` → 200, body `{"status":"UP"}`.
- [ ] **T3** — Browser : login Google sur `staging.legalcase.fr/login` → flow complet → arrivée sur `staging.legalcase.fr/dashboard`.
- [ ] **T4** — Browser : login Google sur `staging.legalcase.ng-itconsulting.com/login` → arrivée sur `staging.legalcase.ng-itconsulting.com/dashboard` (non-régression).
- [ ] **T5** — Stripe Dashboard "Send test event" → l'événement arrive sur l'ancien endpoint (URL inchangée côté Stripe) → 200 dans les logs backend (non-régression).
- [ ] **T6** — Inviter un collaborateur depuis `staging.legalcase.fr` → email reçu → lien pointe vers `staging.legalcase.fr/accept-invitation/...`.
- [ ] **T7** — `kubectl get certificate -n staging` → status `True` pour les 2 hosts.
- [ ] **T8** — Démarrer un Stripe Checkout depuis `staging.legalcase.fr/billing` → success URL retourne sur `staging.legalcase.fr/billing/success` (pas sur ancien host).

#### Production

- [ ] **T9-T16** — Mêmes tests T1-T8 transposés sur `legalcase.fr` / `legalcase.ng-itconsulting.com`.

### Smoke E2E

- [ ] `cd e2e && npm test` → `auth.spec.ts`, `workspace.spec.ts`, `navigation.spec.ts` continuent à passer (baseURL inchangée = ancien host).

### Isolation workspace

- [x] **Non applicable** — SF infra/auth/billing/email, aucune donnée client manipulée.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Auth / Principal** — `defaultSuccessUrl` remplacé par `HostAwareSuccessHandler`. Liste explicite des composants impactés ci-dessous.
- [x] **Workspace context** — non touché.
- [x] **Plans / limites** — non touché.
- [x] **Navigation / routing frontend** — non touché côté Angular (paths relatifs). Côté Spring : la redirection après login change d'origine.
- [ ] **Aucune préoccupation transversale** — non, plusieurs sont touchées.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact | Test de non-régression |
|----------------------|--------|------------------------|
| `WebConfig` (CORS) | Allowlist passe de 1 à 4 origines | `WebConfigTest` + smoke E2E sur ancien host |
| `SecurityConfig.oauth2Login` | `defaultSuccessUrl` → `successHandler` custom | `HostAwareSuccessHandlerTest` 5 cas + IT login flow sur 4 hosts |
| `EmailService` (et 5 autres consommateurs `frontend-url`) | URL des liens emails change vers `legalcase.fr` | T6 + vérification staging d'un email réel |
| `TopupCheckoutService`, `StripeCheckoutService` | Success/cancel URLs basculent vers `legalcase.fr` | T8 (test manuel checkout Stripe staging) |
| `CaseFileShareService` | URL de partage bascule | Manuel : créer un share, vérifier URL générée |
| `ReferentialCheckService`, `ReferentialReminderScheduler` | Liens dans les rappels référentiel | Manuel ou IT |
| `StripeWebhookController` | **Inchangé** — webhook reste sur ancien host | T5 (non-régression) |

### Smoke tests E2E concernés

- [x] `e2e/smoke/auth.spec.ts` — login OAuth → **doit continuer à passer sur l'ancien host** (baseURL Playwright inchangée).
- [x] `e2e/smoke/workspace.spec.ts` — switch workspace → idem.
- [x] `e2e/smoke/navigation.spec.ts` — guards/redirections → idem.

---

## Dépendances

### Subfeatures bloquantes

> Aucune (SF d'amorce de F-143).

### Action opérateur préalable bloquante

- [x] Réservation `legalcase.fr` confirmée (2026-05-01).
- [ ] Configuration DNS (étape 1 du plan opérateur) **avant** déploiement staging.
- [ ] Google Cloud Console (étape 2) **avant** déploiement staging.
- Pas d'action Stripe ni Secrets K8s requise dans cette SF.

### Subfeatures dépendant de celle-ci

- **SF-143-02** (bascule canonique 301 + SEO + CGU + désactivation ancien host) — bloquée tant que SF-143-01 n'est pas mergée + 2-4 semaines de validation passées.

### Questions ouvertes impactées

- [x] Aucune question ouverte (`docs/OPEN_QUESTIONS.md`) impactée.

---

## Estimation effort

- **Code** : ~0,5 j (3 fichiers Spring : 1 nouveau handler + 2 modifs + tests + 4 fichiers K8s + `application.yml`).
- **Actions opérateur** : ~0,3 j (DNS propagation incluse, Google Console).
- **Tests manuels staging + prod** : ~0,5 j.
- **Total** : ~1,5 j. **Conforme à la limite 2 j de la séquence obligatoire.**

---

## Notes et décisions

- **2026-05-01** — Décision opérateur : périmètre F-143 réduit à la migration domaine. Rebrand de marque "AI LegalCase" → "LegalCase" reporté hors F-143.
- **2026-05-02** — Décision opérateur : la SF doit livrer un domaine **100 % fonctionnel** dès le merge (pas de bascule progressive avec OAuth cassé temporairement). Conséquence : SF-143-01 agrège DNS + OAuth (Google Console + `HostAwareSuccessHandler`) + emails (bascule `frontend-url`) + CORS multi-origin. Le découpage final F-143 = 2 SF (vs 5 initiales).
- **2026-05-02** — Décision technique : Stripe webhook **inchangé** dans SF-143-01 (reste sur ancien host). Justification : Stripe est server-to-server, pas besoin de toucher au controller (point critique paiements). Migration simple en SF-143-02 (rotation URL Dashboard + `STRIPE_WEBHOOK_SECRET`). Évite le pattern multi-secrets sur-engineering.
- **Décision technique** : `frontend-url` bascule **immédiate** vers `legalcase.fr`. Conséquence : tous les nouveaux emails et les Stripe Checkout success/cancel URLs pointent vers le nouveau domaine. Les anciens emails restent fonctionnels (ancien host actif jusqu'à SF-143-02).
- **Décision technique** : `HostAwareSuccessHandler` lit le `Host` header de la requête sauvegardée par Spring Security (`HttpSessionRequestCache.getRequest()`). Si l'attaquant force un Host différent, le handler vérifie qu'il fait partie de `allowed-frontend-urls` (allowlist stricte) ; sinon fallback `frontend-url`. Pas de **open redirect**.
- **Décision technique** : Microsoft OAuth absent du code actuel — non traité dans cette SF. Si activé plus tard, son ajout sera trivial (configurer 4 redirect URIs côté Microsoft Azure AD).
- **Décision technique** : SEO/canonical/CGU **délibérément différés** à SF-143-02 → la 301 transfère le PageRank proprement. Mettre les meta canonical sur `legalcase.fr` dès maintenant (alors que le contenu sert sur les 2 hosts) brouillerait le signal Google.
