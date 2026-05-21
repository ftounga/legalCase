# Hotfix prod — Tableau de bord

**Dernière analyse** : 2026-05-21T08:35:00Z (skill `prod-health-check`)

> Ce fichier est **généré et maintenu** par la skill `ai-skills/prod-health-check.md`.
> Il liste les problèmes détectés en production que **l'humain** doit ensuite trier et corriger.
> La skill ne corrige rien — elle observe et répertorie.
>
> Pour lancer un audit : invoque la skill `/prod-health-check` ou demande *"lance le prod health check"*.

---

## 🔴 P0 — Production cassée (urgent)

### HF-2026-05-21-01 — Chaîne logs CloudWatch cassée — Fluent Bit ne parvient pas à enrichir les events

- **Détecté** : 2026-05-21T08:35:00Z (baseline analysis — diag direct DaemonSet)
- **Première occurrence** : 2026-05-20T09:04:01Z (démarrage du DaemonSet)
- **Dernière occurrence** : 2026-05-21T08:35:00Z (toujours actif)
- **Occurrences 24h** : permanent depuis 23h
- **Total observé** : ~24h ininterrompu
- **Signature** : `hash:filter-kubernetes-pod-meta-fail`
- **Logs sample** :
  ```
  [2026/05/20 09:04:01] [filter:kubernetes:kubernetes.0] could not get meta for POD ip-10-0-11-17.eu-west-3.compute.internal
  → Conséquence : le champ kubernetes.namespace_name n'est jamais peuplé
  → Le filter [grep] suivant exige $kubernetes['namespace_name'] ^(production|staging)$
  → Aucun event ne passe → log group /aws/eks/legalcase-shared/applications reste à 0 bytes
  ```
- **Commit suspect** : `286cac01` (SF-INFRA-07b, PR #1176 mergée 2026-05-20T09:03Z) — le DaemonSet a démarré 1 minute après le merge
- **Hypothèse** :
  - Fluent Bit DaemonSet utilise `Use_Kubelet On` + `Kubelet_Port 10250` mais ne peut pas joindre le Kubelet local pour récupérer les métadonnées des pods.
  - Cause probable : permissions (RBAC ou réseau) manquantes pour le ServiceAccount `fluent-bit` (namespace `amazon-cloudwatch`) pour appeler l'endpoint Kubelet `/pods`.
  - Alternative : SDN/NetworkPolicy bloque le port 10250 entre le pod Fluent Bit et le node Kubelet.
- **Impact business** :
  - 🔴 **Toute la chaîne d'alerting est aveugle** : 0 log dans CloudWatch → metric filter ne compte rien → alarme `legalcase-production-backend-error-rate` ne peut jamais déclencher.
  - 🔴 **Cohérent avec SF-INFRA-09 mergé hier** : on a retiré Sentry en pariant sur CloudWatch, mais CloudWatch ne reçoit aucun log applicatif. **On est aveugle sur les erreurs prod et frontend depuis le merge SF-INFRA-09.**
- **Status** : `À TRIER`
- **Notes** : à corriger en priorité (probablement SF-INFRA-07b-fix dans repo legalcase-infra si IRSA, ou simple patch DaemonSet si RBAC kubelet manquant)

---

## 🟠 P1 — Dégradation significative

_(aucun)_

---

## 🟡 P2 — Nuisance / bruit

### HF-2026-05-21-02 — Spike CPU staging RDS éphémère 2026-05-20 08:31 (19s)

- **Détecté** : 2026-05-21T08:35:00Z (baseline analysis — alarm history)
- **Première occurrence** : 2026-05-20T08:31:18Z
- **Dernière occurrence** : 2026-05-20T08:31:37Z (résolu en 19s)
- **Occurrences 24h** : 0 (incident passé, état OK depuis)
- **Total observé** : 1 spike
- **Signature** : `hash:rds-cpu-spike-transient`
- **Logs sample** :
  ```
  legalcase-staging-rds-cpu-high : ALARM → OK en 19 secondes
  ```
- **Commit suspect** : aucun (probablement spike normal au démarrage matin)
- **Hypothèse** : démarrage matinal du pod backend staging déclenche une vague de queries (Liquibase boot, cache warm-up, OAuth reconnect). Transitoire.
- **Status** : `IGNORÉ`
- **Notes** : à ré-évaluer si le pattern se répète quotidiennement. Pour le moment, faux positif transitoire acceptable.

---

## ✅ Terminés (7 derniers jours)

_(aucun — baseline initiale)_

---

## 🗄️ Archive

Les items terminés depuis plus de 30 jours sont déplacés dans `docs/operations/hotfix-prod-archive.md`.

---

## 📊 Observations complémentaires (non hotfix)

Ces points ne sont pas des hotfix à corriger, mais utiles pour le contexte au prochain audit :

- **RDS prod** : `db.t3.micro`, 40 connections max sur 24h, 7.9 % CPU max. **Upgrade vers `db.t4g.small` planifié lun 25/05 ~05:00 Paris** (SF-INFRA-01 — fenêtre de maintenance).
- **RDS staging** : `db.t3.micro`, 50 connections, 8.1 % CPU max. Calme.
- **Pods K8s** : 4 pods backend (2 prod + 2 staging) + 2 frontend + 2 RabbitMQ tous Running, 0 restart, CPU < 10m, mémoire < 640 Mi.
- **Cost Anomaly 7j** : 2 anomalies à \$0.72 et \$0.74 (négligeable).
- **Alarmes CloudWatch** : 7 configurées, 0 en ALARM actuellement.
- **DaemonSet Fluent Bit** : 3/3 pods Running depuis 23h, 0 restart — mais cassé fonctionnellement (cf. HF-2026-05-21-01).

---

## 📋 Légende des statuts

| Statut | Signification |
|---|---|
| `À TRIER` | Nouveau, jamais examiné par l'humain |
| `À FAIRE` | Triés, hotfix à implémenter (priorisé) |
| `EN COURS` | Quelqu'un est en train de fixer (annoter avec qui + PR si dispo) |
| `IGNORÉ` | Examiné, bruit accepté ou faux positif (la skill ne le re-listera pas) |
| `✅ TERMINÉ` | Fixé. Mentionner `Fixed by #PR` permet à la skill de migrer auto |

## 📋 Légende des severities

| Severity | Critère typique |
|---|---|
| **P0** | Production cassée — pod en `CrashLoopBackOff` OU > 100 erreurs/h ininterrompues |
| **P1** | Dégradation significative — alarme prod en `ALARM` OU pattern récurrent > 20/24h |
| **P2** | Nuisance / bruit — alarme staging OU pattern < 20/24h OU dégradation < 25 % |

## 🔗 Voir aussi

- Skill : `ai-skills/prod-health-check.md`
- Patterns à ignorer (bruit accepté) : `docs/operations/hotfix-prod-noise-patterns.md`
- Archive : `docs/operations/hotfix-prod-archive.md`
