# Guide de configuration — Déploiement EKS

Ce document liste toutes les actions manuelles à effectuer **une seule fois** avant que les pipelines CI/CD puissent tourner.

---

## 1. Appliquer l'infrastructure Terraform

```bash
# Bootstrap (une seule fois — crée le bucket S3 state + DynamoDB)
cd ~/dev/legalcase-infra/bootstrap
terraform init
terraform apply

# Staging
cd ~/dev/legalcase-infra/environments/staging
terraform init
terraform plan -out=tfplan
terraform apply tfplan

# Production
cd ~/dev/legalcase-infra/environments/production
terraform init
terraform plan -out=tfplan
terraform apply tfplan
```

Après apply, noter les outputs :
- `eks_cluster_name` → secret GitHub `EKS_CLUSTER_NAME`
- `rds_endpoint` → construire `DB_URL` = `jdbc:postgresql://<endpoint>:5432/legalcasedb`
- `s3_bucket_name` → secret GitHub `S3_BUCKET`
- `ecr_backend_url` / `ecr_frontend_url`
- `irsa_backend_role_arn` → secret GitHub `IRSA_BACKEND_ROLE_ARN`

---

## 2. Configurer les secrets GitHub Actions

Dans le repo `legalCase` → **Settings → Secrets and variables → Actions → New repository secret** :

| Secret | Valeur |
|--------|--------|
| `AWS_ACCESS_KEY_ID` | Clé IAM `legalcase-terraform` |
| `AWS_SECRET_ACCESS_KEY` | Secret IAM `legalcase-terraform` |
| `AWS_ACCOUNT_ID` | ID compte AWS (12 chiffres) |
| `EKS_CLUSTER_NAME` | Output Terraform `eks_cluster_name` |
| `DB_URL` | `jdbc:postgresql://<rds_endpoint>:5432/legalcasedb` |
| `DB_PASSWORD` | Mot de passe RDS (généré par Terraform, récupérable dans Secrets Manager) |
| `S3_BUCKET` | Output Terraform `s3_bucket_name` |
| `S3_ACCESS_KEY` | Clé IAM dédiée S3 (ou vide si IRSA) |
| `S3_SECRET_KEY` | Secret IAM S3 (ou vide si IRSA) |
| `IRSA_BACKEND_ROLE_ARN` | Output Terraform `irsa_backend_role_arn` |
| `ANTHROPIC_API_KEY` | Clé API Claude (console.anthropic.com) |
| `GOOGLE_CLIENT_ID` | Console Google Cloud → OAuth2 |
| `GOOGLE_CLIENT_SECRET` | Console Google Cloud → OAuth2 |
| `MICROSOFT_CLIENT_ID` | Azure Portal → App Registration |
| `MICROSOFT_CLIENT_SECRET` | Azure Portal → App Registration |
| `STRIPE_SECRET_KEY` | Dashboard Stripe → Developers → API keys |
| `STRIPE_WEBHOOK_SECRET` | Dashboard Stripe → Webhooks (après création) |
| `STRIPE_PRICE_ID_STARTER` | Dashboard Stripe → Products |
| `STRIPE_PRICE_ID_PRO` | Dashboard Stripe → Products |
| `MAIL_HOST` | `smtp-relay.brevo.com` |
| `MAIL_USERNAME` | Email Brevo |
| `MAIL_PASSWORD` | Clé SMTP Brevo |
| `RABBITMQ_USER` | Au choix (ex: `legalcase`) |
| `RABBITMQ_PASSWORD` | Mot de passe fort généré |

---

## 3. Configurer OAuth2 en production

### Google
1. Console Google Cloud → APIs & Services → Credentials
2. Éditer le client OAuth2 existant
3. Ajouter dans **Authorized redirect URIs** :
   - `https://<votre-domaine>/login/oauth2/code/google`

### Microsoft
1. Azure Portal → App Registration → Authentication
2. Ajouter redirect URI :
   - `https://<votre-domaine>/login/oauth2/code/microsoft`

---

## 4. Configurer le webhook Stripe

1. Dashboard Stripe → Developers → Webhooks → Add endpoint
2. URL : `https://<votre-domaine>/api/v1/stripe/webhook`
3. Events : `checkout.session.completed`, `customer.subscription.updated`, `customer.subscription.deleted`
4. Copier le **Signing secret** → secret GitHub `STRIPE_WEBHOOK_SECRET`

---

## 5. Créer le profil Spring prod

Créer `backend/src/main/resources/application-prod.yml` avec la config production (datasource, rabbitmq, mail activé, stripe activé).

---

## 6. Déploiement initial

```bash
# Installer nginx ingress controller sur le cluster
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.9.4/deploy/static/provider/aws/deploy.yaml

# Vérifier
kubectl get pods -n ingress-nginx

# Le pipeline se déclenche automatiquement au prochain push sur master
```

---

## 7. Déployer en production (après validation staging)

Depuis GitHub → Actions → **Deploy to Production** → Run workflow :
- `backend_tag` : le SHA court du commit à déployer (ex: `a1b2c3d`)
- `frontend_tag` : idem
- `confirm` : taper `PRODUCTION`
