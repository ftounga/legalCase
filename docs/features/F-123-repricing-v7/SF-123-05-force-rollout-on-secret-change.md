# Mini-spec — F-123 / SF-123-05 Force rollout on secret change (workflow fix)

## Identifiant · `F-123 / SF-123-05`
## Date · `2026-04-21` · Branche · `feat/SF-123-05-force-rollout-on-secret-change`

## Objectif
Corriger le workflow CI/CD pour que les changements de secrets K8s déclenchent automatiquement un rollout du backend. Aujourd'hui, si le code n'a pas changé (même SHA → même image), `kubectl apply -k` ne détecte aucune modification du Deployment et ignore la mise à jour du secret. Résultat : le pod continue à tourner avec les anciennes env vars jusqu'au prochain push.

## Contexte
Incident 2026-04-21 lors de la mise à jour du `STRIPE_PRICE_ID_SOLO_TEST` en staging : le secret a été écrit, le workflow s'est terminé OK, mais le pod tournait encore avec l'ancien price_id. Retry manuel via `kubectl rollout restart`.

## Comportement ciblé
Calculer le SHA256 tronqué du secret `backend-secrets` après sa mise à jour, et l'injecter comme annotation `secrets-hash` sur le pod template. Kubernetes détecte alors un changement de spec et redémarre le pod — même si l'image est inchangée. Symétrique dans les 2 workflows :
- `backend.yml` (staging)
- `deploy-production.yml` (prod)

## Critères d'acceptation
- [ ] `k8s/base/backend/deployment.yaml` : annotation `secrets-hash: SECRETS_HASH_PLACEHOLDER` sous `spec.template.metadata.annotations`
- [ ] `backend.yml` : calcul du hash + sed pour substituer le placeholder avant `kubectl apply -k`
- [ ] `deploy-production.yml` : même logique
- [ ] Après 1er run, le placeholder n'existe plus dans le cluster
- [ ] Changement de secret seul (sans change de code) → pod effectivement redémarré
- [ ] Changement de code seul → 1 seul rollout (pas de double rollout)

## Plan de test minimal
Pas de test unitaire (modifications YAML + shell dans CI). Validation :
1. Merger + CI run avec aucun changement de secret → 1 rollout (image → nouveau SHA → nouveau hash aussi). OK même si double calcul, car Kubernetes regroupe en 1 rollout pour un changement cohérent.
2. Déclencher un `workflow_dispatch` après modification d'un secret sans changer le code → rollout détecté via hash différent.

## Impacts
### Fichiers modifiés
- `k8s/base/backend/deployment.yaml`
- `.github/workflows/backend.yml`
- `.github/workflows/deploy-production.yml`

### Pas d'impact
- Frontend : pas de secrets K8s sensibles dans le même schéma
- RabbitMQ : secret dédié `rabbitmq-secrets`, cas rare (et redémarré manuellement si nécessaire, charge IT négligeable)

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|-------|-----------|------------|
| Autres Deployments avec envFrom secretRef | Backend uniquement en V1 (frontend n'a pas de secret) | Non applicable |
| Pattern CI/CD partagé | Solution isolée aux 2 workflows backend, pas de pattern partagé à généraliser | Intégré |

## Préoccupations transversales
- Aucune : modification purement infra.

## Hors scope
- Réorganiser les workflows (ex. externaliser le calcul du hash dans une action réutilisable) — intéressant si on ajoute d'autres Deployments avec secrets, mais pas aujourd'hui.
- Équivalent pour `rabbitmq-secrets` — hors incident pour l'instant.
