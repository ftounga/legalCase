---
description: Audite la production (alarmes CloudWatch, logs, pods K8s, coûts AWS) et met à jour le tableau de bord docs/operations/hotfix-prod.md
---

# /prod-health-check

Tu dois exécuter la skill **prod-health-check** documentée dans le repo.

## Étape 1 — Charger les instructions

Lis intégralement le fichier `ai-skills/prod-health-check.md` et suis sa procédure obligatoire (étapes 1 à 6).

## Étape 2 — Charger l'état courant du tableau de bord

Lis `docs/operations/hotfix-prod.md` (état actuel des hotfix candidats) et `docs/operations/hotfix-prod-noise-patterns.md` (patterns d'erreur à ignorer) avant de commencer l'audit.

## Étape 3 — Exécuter l'audit complet

Audite les **4 dimensions** :
- A. Alarmes CloudWatch (`describe-alarms` + `describe-alarm-history`)
- B. Patterns logs ERROR sur 24h (`filter-log-events` + grouping par hash signature)
- C. Santé pods K8s (`kubectl get pods`, `kubectl top`, restart count)
- D. Coûts / capacité AWS (Cost Anomaly + métriques RDS vs J-7)

**Toutes les commandes AWS doivent préfixer `AWS_PROFILE=legalcase-terraform`** (le profil par défaut n'est pas authentifié).

## Étape 4 — Mettre à jour hotfix-prod.md

Applique les règles de fusion documentées dans la skill (nouveaux items, déduplication par signature, auto-resolve si plus observé, auto-archive >30j).

## Étape 5 — Commit + push direct master

Skill purement diagnostique, pas de séquence CLAUDE.md complète. Commit + push direct.

## Étape 6 — Résumé pour l'utilisateur

Affiche le résumé court (compteurs par severity + lien commit) à la fin.

**Non-objectifs** : ne corrige RIEN, n'implémente AUCUN hotfix. Le seul artefact produit est la mise à jour du tableau de bord.
