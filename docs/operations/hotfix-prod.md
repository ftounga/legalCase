# Hotfix prod — Tableau de bord

**Dernière analyse** : 2026-05-27T17:50:00Z (skill `prod-health-check`)

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

### HF-2026-05-27-01 — Fausses alarmes `legalcase-production-backend-error-rate` (metric filter trop large)

- **Détecté** : 2026-05-27T17:50:00Z (alarme `legalcase-production-backend-error-rate`, 2 flap en 45 min)
- **Première occurrence** : 2026-05-27T16:55:21Z (1er flap)
- **Dernière occurrence** : 2026-05-27T17:39:21Z (retour OK, alarme stable depuis)
- **Occurrences 24h** : 2 cycles ALARM→OK (16:55→17:04, puis 17:25→17:39)
- **Total observé** : 2 (depuis ce matin)
- **Signature** : `hash:metric-filter-cross-namespace`
- **Logs sample** :
  ```
  ALARM: Threshold Crossed: 1 datapoint [12.0 (27/05/26 17:20:00)] was greater than the threshold (10.0)
  Metric filter "legalcase-shared-backend-errors" pattern: "ERROR"  ← matche TOUT le log group
  ```
- **Commit suspect** : aucun (problème structurel : metric filter non scopé par namespace depuis SF-INFRA-XX d'origine)
- **Hypothèse** : la métrique `BackendErrors` est alimentée par le filter pattern `"ERROR"` qui matche tous les events du log group `/aws/eks/legalcase-shared/applications`, incluant le namespace `staging`. Les 200+ erreurs staging F-JU-01 (cf. HF-2026-05-27-03) franchissent régulièrement le seuil 10/5min → fausse alarme prod. Aucune erreur réelle prod corrélée dans la fenêtre 17:15-17:25 UTC (vérifié par `filter-log-events` namespace=production).
- **Status** : `À TRIER`
- **Notes** : 2 fix possibles — (a) raffiner le filter pattern en JSON `{ $.kubernetes.namespace_name = "production" && $.log = "*ERROR*" }`, (b) créer une alarme staging séparée. À couper de HF-2026-05-27-03 pour ne pas bloquer.

### HF-2026-05-27-02 — `value too long for type character varying(255)` sur `in_app_notifications` (PROD)

- **Détecté** : 2026-05-27T17:50:00Z (filter logs prod ERROR 24h)
- **Première occurrence** : 2026-05-27T08:00:06Z (1er event indexé sur la fenêtre 24h)
- **Dernière occurrence** : 2026-05-27T08:00:09Z (les 2 inserts ont échoué à 3s d'écart, scheduled task)
- **Occurrences 24h** : 8 events (= 2 insertions échouées × 4 lignes de stack chacune)
- **Total observé** : 8 (premier audit qui le détecte)
- **Signature** : `hash:in-app-notifications-varchar-overflow`
- **Logs sample** :
  ```
  ERROR --- [scheduling-1] o.h.engine.jdbc.spi.SqlExceptionHelper : ERROR: value too long for type character varying(255)
  DataIntegrityViolationException: insert into in_app_notifications (created_at,is_read,link,message,read_at,title,type,user_id,workspace_id,id) values (...)
  Caused by: org.postgresql.util.PSQLException: ERROR: value too long for type character varying(255)
  ```
- **Commit suspect** : aucun récent à l'horizon 7 jours (migration `052-create-in-app-notifications.xml` historique)
- **Hypothèse** : la colonne `title VARCHAR(255)` (migration 052) est saturée par un titre généré dynamiquement par le scheduled task de notification (probablement `AnalysisNotificationService` ou similaire). Hypothèse : nom de dossier client long → titre type `"Analyse de <NOM_LONG_DOSSIER> terminée"` > 255 chars. La notification est perdue côté utilisateur final (pas critique mais visible = absence de notif).
- **Status** : `À TRIER`
- **Notes** : 2 fixes possibles — (a) élargir `title` à VARCHAR(500) (`ALTER COLUMN`), (b) tronquer le titre en code à 255 - ellipsis. La (b) est préférable (plus défensif). Identifier le scheduled task source via `[scheduling-1]` thread + grep `InAppNotification.*save` dans le backend.

### HF-2026-05-27-03 — F-JU-01 bootstrap re-insère sans ON CONFLICT (STAGING, 200+/24h)

- **Détecté** : 2026-05-27T17:50:00Z (filter logs ERROR 24h)
- **Première occurrence** : ≥ 2026-05-26T17:50:00Z (déjà présent sur toute la fenêtre 24h)
- **Dernière occurrence** : 2026-05-27T17:24:55Z (dernier event dans la fenêtre alarme)
- **Occurrences 24h** : ≥ 200 (max-items atteint, volume réel probablement plus élevé)
- **Total observé** : ≥ 200 sur 24h
- **Signature** : `hash:jurisprudence-bootstrap-duplicate-key`
- **Logs sample** :
  ```
  ERROR --- [task-1] o.h.engine.jdbc.spi.SqlExceptionHelper : ERROR: duplicate key value violates unique constraint "uq_tool_jurisprudence_mappi"
  WARN  --- [task-1] f.a.j.JurisprudenceBootstrapService : F-JU-01 — Bootstrap persist failed for rcc-be-indemnite-complementaire:default
  ```
- **Commit suspect** : feature F-JU-01 (JurisprudenceBootstrapService) — chercher dernier merge touchant ce service
- **Hypothèse** : le `JurisprudenceBootstrapService` insère les mappings tool→jurisprudence à chaque exécution sans gérer le cas où ils existent déjà (pas de `ON CONFLICT DO NOTHING` ni de `existsBy...()` pré-insert). Chaque outil mappe genère une UniqueConstraintViolation. Effet de bord : déclenche `HF-2026-05-27-01` (fausses alarmes prod).
- **Status** : `À TRIER`
- **Notes** : staging only — impact limité à du bruit logs + cause indirecte du flap alarme prod. Fix simple : `existsByToolIdAndKey()` avant `save()` OU passer en `INSERT ... ON CONFLICT DO NOTHING` (JPA `@SQLInsert` ou JDBC natif).

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
- **Notes** : pas réobservé sur les 7 jours suivants. À ré-évaluer si pattern récurrent matinal.

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

- **RDS prod** : **`db.t4g.small`** (✅ upgrade SF-INFRA-01 confirmé via `describe-db-instances`), storage 50 GB, status `available`. Métriques `DatabaseConnections` / `CPUUtilization` / `FreeableMemory` affichent "no datapoints received" depuis le 2026-05-20T07:57 — à investiguer (probablement re-config CW à faire post-upgrade RDS).
- **RDS staging** : `db.t3.micro`, storage 20 GB. Stable.
- **Pods K8s** : 0 pod en non-Running, 0 restart sur tous (prod backend up depuis 12+ jours, prod frontend up depuis 2 jours). Backend prod ~640 Mi mémoire (calme).
- **Cost Anomaly 7j** : RDS $1.40 au 2026-05-25 (corrélé au switch t3.micro→t4g.small, attendu) + Secrets Manager $0.04 (négligeable).
- **Alarmes CloudWatch** : 7 configurées, 1 a flap aujourd'hui (cf. HF-2026-05-27-01), 0 actuellement en ALARM.
- **DaemonSet Fluent Bit** : 3/3 Running depuis 7+ jours, 0 restart, logs shippés correctement (cf. HF-2026-05-21-01 résolu).

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
