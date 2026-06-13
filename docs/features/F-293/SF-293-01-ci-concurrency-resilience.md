# SF-293-01 — Groupes de concurrence CI séparés + dispatch manuel frontend

> Feature parente : **F-293** (résilience des déploiements staging). Tâche **infra/CI** → exempte étape 0 / 0 bis (aucun workflow utilisateur, aucun impact écran).

## Objectif (une phrase)

Empêcher qu'un déploiement staging légitime soit annulé par un autre push, en **séparant les groupes de concurrence** frontend/backend, et permettre un re-déclenchement **manuel** du frontend.

## Constat (pourquoi)

`frontend.yml` et `backend.yml` partagent `concurrency.group: staging-deploy` (`cancel-in-progress: false`). Quand ≥ 3 runs s'empilent dans ce groupe partagé, GitHub annule le run **pending intermédiaire**. Lors de la livraison F-292, le push docs étape-6 (qui déclenche `backend.yml`) est arrivé derrière le déploiement frontend en file → **le déploiement frontend a été annulé** (rattrapé à la main par `gh run rerun`).

## Comportement nominal (après fix)

- `frontend.yml` : `concurrency.group: staging-deploy-frontend` + ajout de `workflow_dispatch:` dans `on:`.
- `backend.yml` : `concurrency.group: staging-deploy-backend`.
- `cancel-in-progress: false` **conservé** dans les deux (sérialisation par service préservée : deux pushs frontend rapprochés continuent de se mettre en file, le plus récent l'emporte).
- Conséquence : un push backend (y compris un push docs) **ne peut plus** évincer un déploiement frontend en file, et inversement.

## Cas d'erreur / limites

1. **Deux pushs frontend rapprochés** : comportement inchangé (file d'attente au sein de `staging-deploy-frontend`, GitHub garde le dernier pending) — c'est l'intention.
2. **Déploiements frontend ET backend simultanés** : désormais **autorisés** (groupes distincts). Sans risque : services, déploiements k8s et dépôts ECR distincts (`legalcase-frontend` vs `legalcase-backend`), aucune ressource partagée mutée en parallèle.
3. **YAML invalide** → le workflow ne se charge pas. Mitigation : validation de la syntaxe YAML avant push.

## Critères d'acceptation vérifiables

- [ ] `frontend.yml` : `concurrency.group = staging-deploy-frontend` **et** `on.workflow_dispatch` présent.
- [ ] `backend.yml` : `concurrency.group = staging-deploy-backend` (workflow_dispatch déjà présent, conservé).
- [ ] `cancel-in-progress: false` conservé dans les deux.
- [ ] Les deux fichiers restent du **YAML valide** (parse sans erreur).
- [ ] Aucune autre clé des workflows modifiée (triggers de paths, jobs, steps inchangés).

## Plan de test minimal

- **Validation syntaxe** : `python3 -c "import yaml; yaml.safe_load(open(f))"` sur les deux fichiers → pas d'exception.
- **Vérification ciblée** (grep) : `group:` distinct par fichier, `workflow_dispatch` présent dans frontend.yml, `cancel-in-progress: false` conservé.
- **Validation fonctionnelle au merge** : le merge modifie `.github/workflows/{frontend,backend}.yml` → déclenche les deux workflows, désormais dans des groupes séparés (ne s'évincent plus) → les deux runs vont au bout.
- **Isolation workspace** : N/A (CI, aucune donnée applicative).

## Composants impactés

- `.github/workflows/frontend.yml` (group + workflow_dispatch).
- `.github/workflows/backend.yml` (group).

**Aucun** : code applicatif, endpoint, migration, modèle, frontend.

## Hors périmètre

- Changer `cancel-in-progress` (intention de sérialisation conservée).
- Refondre les étapes de build/deploy, les triggers de paths, ou la stratégie de déploiement.
- Toute optimisation des temps de CI.

## Analyse transversale

- **Auth / workspace / plans / navigation / outil décisionnel** : aucun (CI pur, zéro code applicatif).
- **Smoke E2E** : N/A (pas d'impact auth/workspace/navigation applicative).
- **Préoccupation transversale CI** : la modif touche la **mécanique de déploiement** elle-même → vérifiée au merge (les deux déploiements doivent aboutir, sans éviction mutuelle).
