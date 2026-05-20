# Mini-spec — F-143 / SF-143-02 — Bascule canonique 301 + SEO + désactivation progressive ancien host

## Identifiant

`F-143 / SF-143-02`

## Feature parente

`F-143` — Migration nom de domaine `legalcase.fr`. **Découpage F-143 repensé 2026-05-02** :
- SF-143-01 (DNS + Ingress dual-host + auth multi-host + CORS + frontend-url bascule) — **Mergée** PR #743 + déployée prod 2026-05-02.
- SF-143-02 (bascule canonique 301 ancien→nouveau + SEO meta + désactivation progressive ancien host) — **cette SF**.

Les 3 SF initialement prévues (webhook Stripe, emails transactionnels, rebrand visuel) ont été soit absorbées dans SF-143-01 (webhook + emails via `frontend-url`), soit sorties du périmètre F-143 (rebrand visuel "AI LegalCase" → "LegalCase", décision opérateur 2026-05-01).

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-143-02-bascule-canonique-301`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Basculer canonique sur `legalcase.fr` — l'ancien host `legalcase.ng-itconsulting.com` (et son équivalent staging) renvoie une **301 permanente** vers le nouveau host **en préservant le path et la query string**, les meta SEO (canonical, og:url, JSON-LD, sitemap) pointent toutes vers `legalcase.fr` — sans retirer encore l'ancien host (la désactivation totale reste une action ops post-merge après 2-4 semaines supplémentaires de monitoring sans incident).

---

## Comportement attendu

### Cas nominal — après merge + déploiement

1. Un visiteur tape `https://legalcase.ng-itconsulting.com/dashboard?caseId=42` → Ingress renvoie `301 Moved Permanently` avec `Location: https://legalcase.fr/dashboard?caseId=42`. Le navigateur suit la redirection et arrive sur le nouveau host. **Path et query string préservés.**
2. Idem `https://staging.legalcase.ng-itconsulting.com/...` → `301` vers `https://staging.legalcase.fr/...`.
3. Un visiteur direct sur `https://legalcase.fr/` → serve normal (frontend Angular). Aucune régression.
4. Un crawler Google qui visite l'ancien host suit le 301 et indexe le nouveau host. `<link rel="canonical" href="https://legalcase.fr/">` et `<meta property="og:url" content="https://legalcase.fr/">` dans `index.html` confirment l'URL canonique.
5. `https://legalcase.fr/api/sitemap.xml` liste les URLs en `https://legalcase.fr/...`. `https://legalcase.fr/api/robots.txt` référence `https://legalcase.fr/sitemap.xml`.
6. `frontend/public/robots.txt` (statique servi à la racine) référence `Sitemap: https://legalcase.fr/api/sitemap.xml`. `frontend/public/sitemap.xml` (statique) pointe vers le nouveau host.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Un appel `XHR` ou `fetch` côté frontend depuis un ancien onglet ouvert sur `legalcase.ng-itconsulting.com` vers `/api/...` | Le 301 s'applique aussi sur `/api/...` (l'Ingress redirige toutes les routes) → le navigateur suit la redirection (ou pas, selon le mode `redirect` du fetch). Conséquence : les sessions actuellement ouvertes sur l'ancien host vont passer côté nouveau host au prochain reload. **Pas une régression** — les cookies de session étant attachés à un domaine, l'utilisateur devra se reconnecter sur le nouveau host. Documenté dans la PR comme « comportement attendu ; les onglets ouverts depuis > 1 h auront besoin d'une nouvelle session OAuth ». |
| Webhook Stripe configuré sur ancien host | **OK** : le 301 préserve le path. Stripe suit automatiquement les redirections 301 (vérifié dans la doc Stripe — `Stripe-Signature` est recalculé contre le body, indépendant du host). Néanmoins, **action ops préalable** : déplacer l'endpoint webhook côté Stripe Dashboard vers `https://legalcase.fr/api/v1/stripe/webhook` AVANT le merge, et rotation `STRIPE_WEBHOOK_SECRET` côté Secrets K8s. Voir « Plan opérateur » plus bas. |
| Cert TLS de l'ancien host expire après désactivation totale future | Non concerné dans cette SF — l'ancien host est gardé actif (en mode 301). Le cert reste géré par cert-manager. |
| Revert nécessaire (le 301 casse quelque chose) | `git revert` du commit Ingress + `kubectl apply` ré-installe le serve direct sur l'ancien host. Reverse rollback en < 5 min. |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — *Non applicable* : SF purement infra/SEO/HTTP, ne touche aucun outil décisionnel.
- [x] **Autres pays** — *Non applicable* : redirection HTTP indépendante du pays.
- [x] **Autres domaines** — *Non applicable*.
- [x] **Autres UI patterns** — *Non applicable* : pas de nouveau composant UI.
- [x] **Autres flows transversaux : auth + email + workspace + plans + Stripe** :
  - **Auth OAuth Google** — non touché. `HostAwareSuccessHandler` redirige déjà selon le host d'origine. Si l'avocat démarre OAuth depuis l'ancien host, l'Ingress redirige 301 vers le nouveau host AVANT que Spring Security ne voie la requête → le `Host:` header pour Spring sera celui de la 2e requête (`legalcase.fr`). Côté Google Cloud Console les redirect URIs `https://legalcase.fr/login/oauth2/code/google` sont déjà autorisées (SF-01).
  - **Stripe webhook** — DOIT être migré côté Dashboard AVANT le merge (action ops). Voir plan opérateur.
  - **Emails transactionnels** — non touché (`frontend-url` pointe déjà vers `legalcase.fr` depuis SF-01).
  - **Workspace context** — non touché.
  - **Plans / limites** — non touché.
  - **CORS** — déjà multi-host depuis SF-01. Non touché.

### Niveaux de vérification couverts

- [x] **K8s Ingress (base + production)** : ajout annotation 301 `nginx.ingress.kubernetes.io/permanent-redirect` (ou équivalent `configuration-snippet` selon ce qui colle au mieux) sur les rules de l'ancien host UNIQUEMENT, sans toucher au serve du nouveau host.
- [x] **Frontend `index.html`** : `<link rel="canonical">`, `<meta property="og:url">`, `<meta property="og:image">`, `<meta name="twitter:image">` → `legalcase.fr`.
- [x] **Frontend `landing.component.ts`** : `url` constante (canonical + og:url) + JSON-LD `url` (Organization/SoftwareApplication) → `legalcase.fr`.
- [x] **Frontend `blog/blog-article-page/blog-article-page.component.ts`** : JSON-LD `publisher.url` → `legalcase.fr`.
- [x] **Frontend `legal/legal-content.ts`** : URL du « Service » dans le bloc CGU article 1 → `legalcase.fr` (continuité juridique : le service désigné par les CGU change de URL canonique).
- [x] **Frontend `super-admin/traction-onepager/`** : constante `DEFAULT_CONTACT_URL` + spec correspondante → `legalcase.fr` (outil interne mais cohérence du référentiel onepager).
- [x] **Frontend `public/robots.txt`** : URL du `Sitemap:` → `legalcase.fr`.
- [x] **Frontend `public/sitemap.xml`** : URL du `<loc>` → `legalcase.fr`.
- [x] **Backend `SitemapController`** : default `${blog.public-base-url:https://legalcase.fr}` (au lieu de l'ancien host) + valeur explicite dans application.yml.
- [x] **K8s overlays** : ajout `BLOG_PUBLIC_BASE_URL=https://legalcase.fr` (prod) et `https://staging.legalcase.fr` (staging).
- [x] **Tests frontend** : `landing.component.spec.ts` (test canonical) mis à jour pour pointer `legalcase.fr` ; `traction-onepager.component.spec.ts` idem.
- [x] **Docs** : `PRODUCT_SPEC.md` (ligne F-143 corrigée pour refléter le découpage 2 SF + historique 2026-05-20), `CLAUDE.md` (aucun URL trouvé → non touché), `DEPLOYMENT.md` (curl healthcheck commandes — laissées en l'état car l'ancien host reste actif en 301, mais ajout d'une note).

### Cas spécifique : nouveau pattern UI ou service partagé

> Non applicable — pas de nouveau composant partagé ni service applicatif. Modification YAML/HTML/TS sur des emplacements déjà existants.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|--------------|------------|
| `k8s/base/ingress/ingress.yaml` (staging) | Oui | Intégré (rule `staging.legalcase.ng-itconsulting.com` annotée 301) |
| `k8s/overlays/production/ingress-patch.yaml` (prod) | Oui | Intégré (rule `legalcase.ng-itconsulting.com` annotée 301) |
| `frontend/src/index.html` | Oui | Intégré (canonical, og:url, og:image, twitter:image → `legalcase.fr`) |
| `frontend/src/app/landing/landing.component.ts` | Oui | Intégré (url + JSON-LD url → `legalcase.fr`) |
| `frontend/src/app/blog/blog-article-page/blog-article-page.component.ts` | Oui | Intégré (publisher.url JSON-LD → `legalcase.fr`) |
| `frontend/src/app/legal/legal-content.ts` (CGU article 1) | Oui | Intégré (URL du Service → `legalcase.fr`) |
| `frontend/src/app/super-admin/traction-onepager/` | Oui | Intégré (DEFAULT_CONTACT_URL → `legalcase.fr`) |
| `frontend/public/robots.txt` | Oui | Intégré (Sitemap: → `legalcase.fr`) |
| `frontend/public/sitemap.xml` | Oui | Intégré (`<loc>` → `legalcase.fr`) |
| `backend/SitemapController.java` | Oui | Intégré (default `blog.public-base-url` → `https://legalcase.fr`) |
| `k8s/overlays/{staging,production}/kustomization.yaml` | Oui | Intégré (ajout `BLOG_PUBLIC_BASE_URL`) |
| `frontend/src/environments/environment.*.ts` (rabbitmqUrl) | **Non** | Pointe vers `rabbitmq.*.ng-itconsulting.com` — admin RabbitMQ interne, hors rebrand (cf. SF-01). |
| Email de contact `ai-legalcase@ng-itconsulting.com` (legal-content.ts ×6, application.yml, EmailService.java, marketing docs) | **Non** | Adresses email, hors web rebrand (DNS MX + boîte mail = feature séparée). |
| `super-admin.component.html` lien `n8n.ng-itconsulting.com` | **Non** | Outil admin interne hors rebrand. |
| `docs/marketing/*` (cgu.md, mentions-legales.md, politique-confidentialite.md, m32, m42, m44, n8n workflow) | **Non** dans cette SF | Documents marketing publics versionnés. Mise à jour à effectuer dans le post-merge docs (étape 6) **uniquement pour les URLs web** (pas les emails). Ces fichiers ne sont pas servis par l'app — la version officielle des CGU/PolConf/MentionsLégales servie aux utilisateurs est dans `legal-content.ts` (frontend), qui est dans le périmètre. |
| Webhook Stripe URL (Stripe Dashboard) | Oui — **action ops** | Action ops pré-merge (voir plan opérateur). |

### Décision

- [x] **Étendu à toutes les cibles applicables web** dans cette subfeature.
- [x] **Non applicable** justifié explicitement pour : RabbitMQ admin, n8n admin, adresses email, environnement.ts rabbitmqUrl, docs marketing externes.
- [x] **Action ops séparée** : webhook Stripe (pré-merge), désactivation totale ancien host (post-merge + 2-4 semaines).

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — SF infrastructure + SEO + HTTP redirect. Pas de composant frontend décisionnel touché. Aucune entrée `TOOL_REGISTRY`, aucun outil décisionnel ajouté ou modifié.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — SF n'introduit aucun champ saisissable. Modifications limitées à : meta SEO, JSON-LD, URL constantes, annotations Ingress, robots.txt, sitemap.xml.

---

## Critères d'acceptation

### Infrastructure / HTTP

- [ ] **C1** — `curl -sI https://legalcase.ng-itconsulting.com/` → `HTTP/2 301` avec `Location: https://legalcase.fr/`.
- [ ] **C2** — `curl -sI https://legalcase.ng-itconsulting.com/dashboard?caseId=42` → `HTTP/2 301` avec `Location: https://legalcase.fr/dashboard?caseId=42` (path et query string préservés).
- [ ] **C3** — `curl -sI https://staging.legalcase.ng-itconsulting.com/` → `HTTP/2 301` avec `Location: https://staging.legalcase.fr/`.
- [ ] **C4** — `curl -sI https://legalcase.fr/` → `HTTP/2 200` (serve normal, pas de boucle de redirection).
- [ ] **C5** — `curl -sI https://staging.legalcase.fr/` → `HTTP/2 200`.
- [ ] **C6** — `curl -sI https://legalcase.fr/api/actuator/health` → `HTTP/2 200` (backend OK).
- [ ] **C7** — `curl -sI https://legalcase.ng-itconsulting.com/api/v1/stripe/webhook -X POST -d "x"` → `HTTP/2 308 ou 301` avec `Location: https://legalcase.fr/api/v1/stripe/webhook` (méthode POST préservée par `308 Permanent Redirect` ou par `nginx.ingress.kubernetes.io/permanent-redirect` qui produit 301 mais Stripe est documenté pour suivre les 301 sur webhook).

### SEO / meta

- [ ] **C8** — `curl -s https://legalcase.fr/ | grep -E 'canonical|og:url'` → URLs en `https://legalcase.fr/` (et plus aucune `ng-itconsulting`).
- [ ] **C9** — `curl -s https://legalcase.fr/api/sitemap.xml | grep loc` → toutes les `<loc>` en `https://legalcase.fr/`.
- [ ] **C10** — `curl -s https://legalcase.fr/api/robots.txt | grep Sitemap` → `Sitemap: https://legalcase.fr/sitemap.xml`.
- [ ] **C11** — `curl -s https://legalcase.fr/robots.txt | grep Sitemap` (robots.txt statique servi par frontend) → URL en `legalcase.fr`.
- [ ] **C12** — `curl -s https://legalcase.fr/sitemap.xml | grep loc` (sitemap statique frontend) → URL en `legalcase.fr`.

### Frontend

- [ ] **C13** — `grep -rn "ng-itconsulting" frontend/src/index.html` → aucune occurrence (zéro hit).
- [ ] **C14** — `grep -rn "legalcase.ng-itconsulting" frontend/src/app/landing/landing.component.ts` → aucune occurrence (zéro hit).
- [ ] **C15** — `grep -rn "legalcase.ng-itconsulting" frontend/src/app/blog/blog-article-page/blog-article-page.component.ts` → aucune occurrence.
- [ ] **C16** — Test Jest `landing.component.spec.ts` PASS avec `legalcase.fr`.
- [ ] **C17** — Test Jest `traction-onepager.component.spec.ts` PASS avec `legalcase.fr`.
- [ ] **C18** — `grep -rn "legalcase.ng-itconsulting" frontend/src/app/legal/legal-content.ts` → uniquement adresses email (`ai-legalcase@ng-itconsulting.com`), aucun lien web `https://legalcase.ng-itconsulting.com`.

### Backend

- [ ] **C19** — `SitemapControllerTest` PASS avec le nouveau default `https://legalcase.fr`.

### Documentation

- [ ] **C20** — `docs/PRODUCT_SPEC.md` ligne F-143 décrit le découpage réel **2 SF** (et non 5 SF). Une ligne historique 2026-05-20 documente la bascule canonique.
- [ ] **C21** — `docs/DEPLOYMENT.md` note ajoutée précisant que les healthchecks sur `*.ng-itconsulting.com` renvoient 301 et que les commandes officielles utilisent `legalcase.fr`.

---

## Périmètre

### Hors scope (explicite)

- **Retrait total de l'ancien host du Ingress.** Le 301 garde l'ancien host actif. La désactivation totale (retrait du host des `tls.hosts` et `rules`) est une action ops post-merge à effectuer **après 2-4 semaines supplémentaires de monitoring du 301 sans incident**.
- **Migration emails de contact** `ai-legalcase@ng-itconsulting.com` → `contact@legalcase.fr` : nécessite DNS MX + boîte mail dédiée, hors F-143.
- **Migration n8n / RabbitMQ admin** : outils internes hors rebrand.
- **Migration docs marketing (`docs/marketing/*`)** : update web-URL dans `cgu.md`, `mentions-legales.md`, `politique-confidentialite.md`, `m32`, `m42`, `m44`, `n8n-workflow.json` → étape 6 post-merge docs, scope sed simple (à condition de ne PAS toucher les adresses email). Pas dans le périmètre code de cette SF.
- **Rebrand visuel "AI LegalCase" → "LegalCase"** : hors F-143 (décision opérateur 2026-05-01).
- **GA4 / tracking** : pas de modification — GA4 traite les sous-domaines automatiquement et le 301 transmet la session.

---

## Valeurs initiales

> Non applicable — aucune entité métier créée.

---

## Contraintes de validation

| Champ / config | Règle |
|---------------|-------|
| Annotation `nginx.ingress.kubernetes.io/permanent-redirect` | Doit valoir `https://legalcase.fr$request_uri` (staging : `https://staging.legalcase.fr$request_uri`) — `$request_uri` préserve path + query. |
| `BLOG_PUBLIC_BASE_URL` (env K8s overlays) | `https://legalcase.fr` (prod) / `https://staging.legalcase.fr` (staging). Sans trailing slash. |
| `blog.public-base-url` (application.yml default) | `https://legalcase.fr`. |

---

## Technique

### Endpoints

> Aucun nouvel endpoint applicatif. `SitemapController` voit sa configuration par défaut changer.

### Tables impactées

> Aucune.

### Migration Liquibase

- [ ] Oui
- [x] **Non applicable**.

### Composants Angular (si applicable)

> Aucun nouveau composant. Modifications de meta tags et de constantes URL.

### Fichiers modifiés

#### K8s

| Fichier | Modification |
|---------|--------------|
| `k8s/base/ingress/ingress.yaml` | **Découpage en 2 Ingress** : un Ingress `legalcase-ingress` qui sert `staging.legalcase.fr` (serve normal), un Ingress `legalcase-ingress-legacy` qui couvre `staging.legalcase.ng-itconsulting.com` avec l'annotation `nginx.ingress.kubernetes.io/permanent-redirect: https://staging.legalcase.fr$request_uri`. **Justification du découpage** : l'annotation `permanent-redirect` est globale à l'Ingress et redirigerait aussi le nouveau host, ce qui créerait une boucle. Solution propre = 2 Ingress séparés. |
| `k8s/overlays/production/ingress-patch.yaml` | Idem : remplace par 2 Ingress (`legalcase-ingress` pour `legalcase.fr`, `legalcase-ingress-legacy` pour `legalcase.ng-itconsulting.com` avec annotation 301). |
| `k8s/overlays/staging/kustomization.yaml` | Ajout `BLOG_PUBLIC_BASE_URL=https://staging.legalcase.fr` dans `configMapGenerator`. |
| `k8s/overlays/production/kustomization.yaml` | Ajout `BLOG_PUBLIC_BASE_URL=https://legalcase.fr`. |

#### Backend Spring

| Fichier | Modification |
|---------|--------------|
| `backend/src/main/java/fr/ailegalcase/blog/controller/SitemapController.java` | Default `${blog.public-base-url:https://legalcase.fr}` au lieu de `https://legalcase.ng-itconsulting.com`. |
| `backend/src/main/resources/application.yml` | Ajout explicite `blog.public-base-url: ${BLOG_PUBLIC_BASE_URL:https://legalcase.fr}`. |
| `backend/src/test/java/.../SitemapControllerTest.java` | Mise à jour des assertions de host vers `https://legalcase.fr`. |

#### Frontend

| Fichier | Modification |
|---------|--------------|
| `frontend/src/index.html` | canonical, og:url, og:image, twitter:image → `https://legalcase.fr/...` |
| `frontend/src/app/landing/landing.component.ts` | `url` constante (canonical + og:url) + JSON-LD `url` → `https://legalcase.fr/` |
| `frontend/src/app/landing/landing.component.spec.ts` | `expect(link?.href).toContain('legalcase.fr')` |
| `frontend/src/app/blog/blog-article-page/blog-article-page.component.ts` | JSON-LD `publisher.url` → `https://legalcase.fr` |
| `frontend/src/app/legal/legal-content.ts` | CGU article 1 : URL du Service → `https://legalcase.fr` (texte ET href). |
| `frontend/src/app/super-admin/traction-onepager/traction-onepager.component.ts` | `DEFAULT_CONTACT_URL = 'https://legalcase.fr'` |
| `frontend/src/app/super-admin/traction-onepager/traction-onepager.component.spec.ts` | Assertion correspondante → `legalcase.fr` |
| `frontend/public/robots.txt` | `Sitemap: https://legalcase.fr/api/sitemap.xml` |
| `frontend/public/sitemap.xml` | `<loc>https://legalcase.fr/api/sitemap.xml</loc>` |

#### Docs

| Fichier | Modification |
|---------|--------------|
| `docs/PRODUCT_SPEC.md` | Ligne F-143 corrigée (5 SF → 2 SF, statut « 🟢 Terminée 2/2 SF » à mettre lors de l'étape 6 post-merge). Ligne historique 2026-05-20 ajoutée. |
| `docs/DEPLOYMENT.md` | Note sur les healthchecks (l'ancien host renvoie 301 — utiliser `legalcase.fr` dans les commandes officielles). |
| `docs/features/F-143/SF-143-02-bascule-canonique-301.md` | Cette mini-spec. |

### Décisions techniques

1. **2 Ingress séparés** (et pas annotation `configuration-snippet` sur un seul Ingress) : la directive `permanent-redirect` de nginx-ingress agit sur tout l'Ingress. Pour ne rediriger que l'ancien host, il faut un Ingress dédié à l'ancien host avec l'annotation, et garder l'Ingress principal pour le nouveau host. **Revert facile** : `kubectl delete ingress legalcase-ingress-legacy -n <ns>` + `git revert`.
2. **`$request_uri`** dans la cible 301 : préserve path **et** query string en une seule variable (équivalent `$uri$is_args$args`).
3. **301 (Moved Permanently) plutôt que 302 (Found)** : indique aux moteurs de recherche que la migration est permanente — transfert de PageRank. Les navigateurs cachent agressivement le 301, ce qui est OK ici (l'ancien host n'a pas vocation à revenir).
4. **POST/webhook préservés** : les RFC 7231 et 7538 autorisent le navigateur à changer POST en GET sur un 301 (legacy comportement). Stripe documente suivre les 301 sur webhooks **en préservant la méthode**. En cas de doute, l'action ops pré-merge est de migrer l'URL côté Stripe Dashboard (voir plan opérateur).
5. **Pas de robots.txt « Disallow: / » sur l'ancien host** : le 301 fait le boulot pour Google (consolide le ranking sur le nouveau host). Un Disallow pourrait au contraire désindexer trop vite avant que Google n'ait crawlé les 301.
6. **`legal-content.ts` CGU** : la phrase de l'article 1 désigne le service par son URL canonique. Le rebrand canonique implique la mise à jour de cette URL (continuité juridique : le « Service » désigné par les CGU reste accessible, juste sous une nouvelle URL — l'ancienne URL renvoie une 301 vers la nouvelle, donc l'accord juridique n'est pas rompu).

---

## Plan opérateur (actions hors code)

> **AVANT le merge** :

### Étape 1 — Stripe Dashboard (migration webhook)

1. Dashboard Stripe → Developers → Webhooks → l'endpoint actuel `https://legalcase.ng-itconsulting.com/api/v1/stripe/webhook`.
2. Éditer l'URL → `https://legalcase.fr/api/v1/stripe/webhook`. **Garder l'événement set inchangé.**
3. Récupérer le nouveau `Signing secret` (whsec_...).
4. Mettre à jour le Secret K8s `legalcase-secrets` dans `staging` et `production` :
   ```bash
   kubectl -n staging set env deployment/legalcase-backend STRIPE_WEBHOOK_SECRET=<nouveau>
   # ou (préférable) via le Secret existant + restart
   ```
5. Tester l'envoi d'un webhook test depuis Stripe Dashboard → vérifier 200 OK côté backend.

> **Note** : si la migration Stripe préalable est trop risquée à effectuer avant le merge, alternative = laisser le webhook sur l'ancien host et compter sur le 301 pour le rediriger. Stripe suit les 301 (documenté). À surveiller dans les logs après merge.

### Étape 2 — Déploiement

1. Merger SF-143-02 sur `master`.
2. Workflow CI/CD → staging.
3. Vérifier rollout : `kubectl rollout status deployment/legalcase-backend -n staging` + `kubectl get ingress -n staging`.
4. Exécuter les tests manuels :
   - `curl -sI https://staging.legalcase.ng-itconsulting.com/ | grep -E "HTTP|Location"` → `301` + `Location: https://staging.legalcase.fr/`.
   - `curl -sI https://staging.legalcase.fr/` → `200`.
   - Login OAuth bout-en-bout sur `staging.legalcase.fr`.
   - Webhook Stripe test (depuis Stripe Dashboard).
5. Si OK staging → déploiement production via `deploy-production.yml`.

### Étape 3 — Post-merge (J+0 à J+14)

1. Surveiller logs nginx-ingress (recherche `301` sur l'ancien host).
2. Surveiller Sentry sur `legalcase.fr` — détecter toute régression silencieuse (CORS, OAuth callback URL côté Google si jamais).
3. Google Search Console — vérifier que le 301 est détecté et que `legalcase.fr` commence à indexer les pages.
4. **Action ops à J+14 à J+28** : si le 301 fonctionne sans incident pendant 2-4 semaines, créer une SF future ou une simple action ops pour retirer l'host `*.ng-itconsulting.com` du Ingress et libérer le cert TLS (action 5 min).

### Revert

Si le 301 casse quelque chose en production :
```bash
git revert <commit_ingress>
git push
# CI/CD redéploie l'Ingress, l'ancien host re-sert directement.
```

---

## Plan de test

### Tests unitaires

- [ ] `SitemapControllerTest` — assertion du host du sitemap : `assertThat(body).contains("https://legalcase.fr/")`.
- [ ] `LandingComponentSpec` — `expect(link?.href).toContain('legalcase.fr')`.
- [ ] `TractionOnepagerComponentSpec` — `expect(form.get('contact.url')?.value).toBe('https://legalcase.fr')`.

### Tests d'intégration / E2E

- [ ] **Non testable en unit / IT classique** : le 301 est une annotation nginx-ingress, validée au déploiement staging.
- [ ] Smoke E2E `e2e/smoke/auth.spec.ts`, `workspace.spec.ts`, `navigation.spec.ts` continuent à passer sur `staging.legalcase.fr` (baseURL à laisser ou ajuster).

### Isolation workspace

- [x] **Non applicable** — SF infra/SEO, ne touche pas l'isolation workspace.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — Non touché (mais voir note ci-dessous).
- [ ] Workspace context — Non touché.
- [ ] Plans / limites — Non touché.
- [x] **Navigation / routing frontend** — Non touché côté Angular router (les routes restent les mêmes). **Côté HTTP** : tout le trafic ancien host est redirigé. Les utilisateurs avec un onglet ouvert sur l'ancien host devront se reconnecter sur le nouveau host (cookies par domaine).
- [ ] Aucune préoccupation transversale — La SF a un impact HTTP transversal mais limité à la couche infra Ingress.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| Webhook Stripe (`POST /api/v1/stripe/webhook`) | 301 sur ancien URL — Stripe suit le 301 mais la doc préfère URL directe | **Action ops pré-merge** : migration de l'URL côté Stripe Dashboard + rotation du `STRIPE_WEBHOOK_SECRET`. Tester avec un webhook test post-merge. |
| OAuth Google callback | Si callback démarré depuis l'ancien host → 301 vers le nouveau host avant que Spring ne reçoive le code → callback final sur `legalcase.fr`. Redirect URIs Google déjà autorisées (SF-01). | Login bout-en-bout sur `legalcase.fr` post-merge. |
| Sessions actives ancienne host | Cookies session par domaine → utilisateurs devront reconnecter sur le nouveau host | Documenté dans PR. Pas de mitigation technique (le re-login OAuth Google est de toute façon transparent ~3 sec). |
| Frontend ouvert sur ancien host (avec hot-reload XHR) | Les XHR `/api/...` reçoivent 301 — selon le mode `redirect` du fetch (`follow` par défaut) le navigateur suit | Documenté ; pas de mitigation. |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/auth.spec.ts` — login OAuth sur `legalcase.fr` post-deploy staging.
- [ ] `e2e/smoke/navigation.spec.ts` — navigation entre routes sur `legalcase.fr`.

---

## Dépendances

### Subfeatures bloquantes

- **SF-143-01** — statut : **done** (mergée PR #743, déployée prod 2026-05-02, validation bout-en-bout opérateur OK). **18 jours de monitoring sans incident** (fenêtre 2-4 semaines respectée).

### Questions ouvertes impactées

- Aucune.

---

## Notes et décisions

- **F-143 redécoupée à 2 SF (2026-05-02)** : SF-143-01 a absorbé webhook Stripe + emails transactionnels + frontend-url ; rebrand visuel sorti du périmètre F-143 (décision opérateur). Cette SF (SF-143-02) clôt F-143.
- **Désactivation totale ancien host = action ops** post-merge à J+14 à J+28. **Pas une SF** (action `kubectl` 5 min).
- **Mémoires impératives appliquées** :
  - `feedback_prod_url_canonical` — URL prod = `https://legalcase.fr` ✅
  - `feedback_no_overclaim_negatives` — chaque assertion `curl` testable via plan opérateur staging ✅
  - `feedback_verifier_master_pas_branche_courante` — branche créée depuis `origin/master` fraîchement fetched ✅
  - `feedback_recheck_master_par_phase` — re-vérifier master AVANT push ✅
  - `feedback_autodeploy` — **PAS auto-merge** ici (production-impacting), l'opérateur valide ✅

---
