# DEPLOYMENT.md — Commandes de déploiement cloud

Référencé depuis `CLAUDE.md`.

## Référence rapide — toutes les commandes

| Intention | Commande naturelle |
|-----------|-------------------|
| Redémarrer l'appli en local (dev) | "Redémarre l'application en local" |
| Redémarrer l'appli en local (local/postgres) | "Redémarre l'application en local avec PostgreSQL" |
| Déployer en staging via CI/CD | "Lance le workflow de déploiement staging" |
| Déployer en production via CI/CD | "Lance le workflow de déploiement production" |
| Déployer manuellement en staging | "Déploie l'application en staging" |
| Déployer manuellement en production | "Déploie l'application en production" |
| Voir les pods en staging | "Montre l'état des pods en staging" |
| Voir les pods en production | "Montre l'état des pods en production" |
| Voir les logs du backend staging | "Montre les logs du backend en staging" |
| Redémarrer le backend en staging | "Redémarre le backend en staging" |
| Redémarrer le backend en production | "Redémarre le backend en production" |

---

## Déployer en staging — via CI/CD (recommandé)

Déclenche le workflow GitHub Actions backend ou frontend sur master :

```bash
# Déployer le backend en staging
gh workflow run backend.yml --ref master

# Déployer le frontend en staging
gh workflow run frontend.yml --ref master

# Suivre le déploiement en cours
gh run list --workflow=backend.yml --limit=1
gh run watch $(gh run list --workflow=backend.yml --limit=1 --json databaseId -q '.[0].databaseId')
```

---

## Déployer en production — via CI/CD (workflow dédié)

Le déploiement production est `workflow_dispatch` uniquement — jamais automatique.
Il requiert les tags exacts des images à déployer et la confirmation manuelle `PRODUCTION`.

```bash
# 1. Récupérer les SHAs actuellement en staging
kubectl get deployment legalcase-backend -n staging \
  -o jsonpath='{.spec.template.spec.containers[0].image}' | cut -d: -f2
kubectl get deployment legalcase-frontend -n staging \
  -o jsonpath='{.spec.template.spec.containers[0].image}' | cut -d: -f2

# 2. Lancer le déploiement production avec les tags récupérés
gh workflow run deploy-production.yml \
  --ref master \
  --field backend_tag=<SHA_BACKEND> \
  --field frontend_tag=<SHA_FRONTEND> \
  --field confirm=PRODUCTION

# 3. Suivre le déploiement
gh run list --workflow=deploy-production.yml --limit=1
gh run watch $(gh run list --workflow=deploy-production.yml --limit=1 --json databaseId -q '.[0].databaseId')
```

> Ne jamais déployer en production sans avoir validé en staging d'abord.

---

## Déployer manuellement en staging (sans CI/CD)

Pour un correctif urgent sans passer par le pipeline :

```bash
# Mettre à jour kubeconfig
aws eks update-kubeconfig --region eu-west-3 --name legalcase-shared

# Appliquer les manifests kustomize
kubectl apply -k k8s/overlays/staging/

# Vérifier le rollout
kubectl rollout status deployment/legalcase-backend -n staging --timeout=120s
kubectl rollout status deployment/legalcase-frontend -n staging --timeout=60s
```

---

## Surveiller les pods

```bash
# État de tous les pods staging
kubectl get pods -n staging

# État de tous les pods production
kubectl get pods -n production

# Logs backend staging (30 dernières lignes)
kubectl logs -n staging deployment/legalcase-backend --tail=30

# Logs backend production
kubectl logs -n production deployment/legalcase-backend --tail=30

# Logs en temps réel
kubectl logs -n staging deployment/legalcase-backend -f
```

---

## Redémarrer un service sur le cluster

```bash
# Redémarrer le backend en staging
kubectl rollout restart deployment/legalcase-backend -n staging

# Redémarrer le frontend en staging
kubectl rollout restart deployment/legalcase-frontend -n staging

# Redémarrer le backend en production
kubectl rollout restart deployment/legalcase-backend -n production

# Redémarrer tous les services en staging
kubectl rollout restart deployment/legalcase-backend deployment/legalcase-frontend deployment/rabbitmq -n staging
```

---

## Vérifier la santé de l'application

> Note F-143 SF-143-02 — Les anciens hosts `*.ng-itconsulting.com` renvoient maintenant
> `301 Moved Permanently` vers `legalcase.fr` (resp. `staging.legalcase.fr`).
> Utiliser les hosts canoniques `legalcase.fr` dans les commandes officielles.
> `curl -L` suit le 301 ; `curl -sI` montre la redirection (utile pour valider la bascule).

```bash
# Health check staging
curl -s https://staging.legalcase.fr/api/actuator/health

# Health check production
curl -s https://legalcase.fr/api/actuator/health

# Validation de la bascule 301 (doit renvoyer 301 + Location vers legalcase.fr)
curl -sI https://staging.legalcase.ng-itconsulting.com/ | grep -E "HTTP|Location"
curl -sI https://legalcase.ng-itconsulting.com/ | grep -E "HTTP|Location"

# Certificats TLS
kubectl get certificate -n staging
kubectl get certificate -n production
```
