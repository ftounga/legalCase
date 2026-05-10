# SF-233-01 — Fix timeout kubectl rollout dans les workflows CI

## Objectif
Augmenter à `10m` les timeouts `kubectl rollout status` dans les workflows GitHub Actions afin de
laisser le temps aux migrations Liquibase + démarrage Spring Boot de remonter `Ready`, et éviter le
faux échec CI alors que le rollout applicatif a réussi.

## Contexte
4 runs consécutifs cassés 2026-05-09/10 sur `backend.yml` :
`error: timed out waiting for the condition` après ~2 min sur
`kubectl rollout status deployment/legalcase-backend -n staging --timeout=120s`,
alors que `https://staging.legalcase.ng-itconsulting.com/api/actuator/health` répond UP peu après.
Cause : timeout `120s` trop court face aux migrations Liquibase + readinessProbe (Spring Boot
startup ~90-150s + délai readiness probe initial).

## Comportement nominal
- `kubectl rollout status deployment/legalcase-backend -n staging --timeout=10m` réussit dès que
  le rollout est terminé (souvent < 4 min) et n'attend pas inutilement.
- Si le rollout échoue réellement (crashloop, image inexistante), kubectl sort en erreur dès que
  le `progressDeadlineSeconds` du Deployment est dépassé — pas besoin d'attendre les 10 min.
- Cohérence staging / production / k8s-deploy : tous les rollouts ont `--timeout=10m`.

## Cas d'erreur couverts
- Migration Liquibase longue (> 2 min) : OK, on attend jusqu'à 10 min.
- Déploiement bloqué (crashloop) : kubectl rollout status sort sur le `progressDeadlineSeconds`
  du Deployment (généralement 600s par défaut, soit aligné).
- Le timeout 10 min reste un garde-fou contre un workflow qui resterait suspendu indéfiniment.

## Critères d'acceptation
- [x] Tous les `kubectl rollout status` des 4 workflows ont `--timeout=10m`.
- [x] Aucun `kubectl wait ...` introduit (aucun n'existe aujourd'hui).
- [x] Cohérence backend ↔ frontend ↔ production ↔ k8s-deploy.
- [x] YAML valide (visuel ou yamllint).
- [x] Le workflow `backend.yml` réussit sur le run suivant.

## Plan de test
- **Validation YAML** : lecture visuelle + `yamllint` si dispo. Aucun test unitaire applicable —
  il s'agit de fichiers de pipeline.
- **Validation runtime** : relancer `gh workflow run backend.yml --ref master` après merge,
  vérifier que `Deploy to staging` passe en succès en moins de 10 min.

## Fichiers impactés
- `.github/workflows/backend.yml`
- `.github/workflows/frontend.yml`
- `.github/workflows/deploy-production.yml`
- `.github/workflows/k8s-deploy.yml`

## Hors périmètre
- Optimisation du temps de démarrage du backend (migrations Liquibase, JVM warmup) — autre feature.
- Ajustement de `progressDeadlineSeconds` sur les Deployments K8s — non nécessaire (default 600s).
- Refonte des healthchecks Spring Boot — hors scope.

## Impact par domaine métier
Transversale (infrastructure CI/CD). Aucune adaptation par domaine (Travail / Immigration / Famille)
ni par pays (FR / BE) — c'est un correctif de pipeline.
