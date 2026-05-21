# Hotfix prod — Tableau de bord

**Dernière analyse** : 2026-05-21T08:45:00Z (skill `prod-health-check` + fix manuel HF-2026-05-21-01)

> Ce fichier est **généré et maintenu** par la skill `ai-skills/prod-health-check.md`.
> Il liste les problèmes détectés en production que **l'humain** doit ensuite trier et corriger.
> La skill ne corrige rien — elle observe et répertorie.
>
> Pour lancer un audit : invoque la skill `/prod-health-check` ou demande *"lance le prod health check"*.

---

## 🔴 P0 — Production cassée (urgent)

_(aucun)_

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

### HF-2026-05-21-01 — Chaîne logs CloudWatch cassée — Fluent Bit Use_Kubelet ✅ TERMINÉ

- **Détecté** : 2026-05-21T08:35:00Z (baseline analysis — diag direct DaemonSet)
- **Première occurrence** : 2026-05-20T09:04:01Z (démarrage du DaemonSet, post-merge SF-INFRA-07b)
- **Dernière occurrence** : 2026-05-21T08:40:47Z (résolu après rollout)
- **Durée totale d'incident** : ~24h (entièrement en pré-prod réelle — 0 client payant impacté)
- **Signature** : `hash:filter-kubernetes-pod-meta-fail`
- **Commit suspect (confirmé)** : `286cac01` (SF-INFRA-07b, PR #1176)
- **Root cause** : filter `kubernetes` configuré avec `Use_Kubelet On` + `Kubelet_Port 10250`, mais le ServiceAccount `fluent-bit` (namespace `amazon-cloudwatch`) n'a que les permissions `pods`, `namespaces`, `pods/logs` — il lui manque `nodes/proxy` pour appeler l'API Kubelet. Résultat : enrichissement K8s échoue → `namespace_name` absent → filter `grep ^(production|staging)$` rejette tous les events → 0 log shippé.
- **Fix** : `Use_Kubelet Off` dans `k8s/system/fluent-bit.yaml`. Le filter utilise désormais l'API K8s server standard (`kubernetes.default.svc:443`), pour laquelle le SA a déjà les bonnes permissions. Fixed by **PR #1212** (commit `ccd1f7ee`).
- **Validation** :
  - `kubectl edit configmap fluent-bit-config` appliqué en immédiat (avant la PR) → rollout daemonset
  - Dans les 30 secondes : 3 streams `from-fluent-bit-application.*` apparaissent dans CloudWatch
  - Logs Fluent Bit montrent maintenant `[output:cloudwatch_logs] Creating log stream …` (vs warning meta avant)
- **Status** : `✅ TERMINÉ` (Fixed by #1212)
- **Leçon retenue** : impossible d'attraper en SF-INFRA-07b car le test était uniquement déploiement K8s. Manque un smoke test "logs arrivent vraiment dans CloudWatch" en post-deploy. À ajouter dans une future SF-INFRA-XX (script validation 5 min après chaque merge SF-INFRA-07b).

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
