# Hotfix prod — Tableau de bord

**Dernière analyse** : 2026-05-28T00:15:00Z (skill `prod-health-check` — confirmation post-deploy)

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

### HF-2026-05-27-01 — Fausses alarmes `legalcase-production-backend-error-rate` (metric filter trop large) ✅ TERMINÉ

- **Détecté** : 2026-05-27T17:50:00Z (alarme `legalcase-production-backend-error-rate`, 2 flap en 45 min)
- **Première occurrence** : 2026-05-27T16:55:21Z (1er flap)
- **Dernière occurrence** : 2026-05-27T17:39:21Z (retour OK, alarme stable depuis)
- **Occurrences 24h** : 2 cycles ALARM→OK (16:55→17:04, puis 17:25→17:39)
- **Total observé** : 2 (depuis ce matin)
- **Signature** : `hash:metric-filter-cross-namespace`
- **Logs sample** :
  ```
  ALARM: Threshold Crossed: 1 datapoint [12.0 (27/05/26 17:20:00)] was greater than the threshold (10.0)
  Metric filter "legalcase-shared-backend-errors" pattern: "ERROR"  ← matchait TOUT le log group avant fix
  ```
- **Commit suspect** : aucun (problème structurel : metric filter non scopé par namespace depuis création initiale)
- **Hypothèse** : la métrique `BackendErrors` était alimentée par le filter pattern `"ERROR"` qui matchait tous les events du log group `/aws/eks/legalcase-shared/applications`, incluant le namespace `staging`. Les 200+ erreurs staging F-JU-01 (cf. HF-2026-05-27-03) franchissaient régulièrement le seuil 10/5min → fausse alarme prod.
- **Fix** : `aws logs put-metric-filter --filter-pattern '{ $.kubernetes.namespace_name = "production" && $.log = "*ERROR*" }'` appliqué directement sur `legalcase-shared-backend-errors`. La métrique `BackendErrors` ne sera désormais incrémentée que pour les events du namespace `production`. Pas de Terraform à modifier (filter créé hors IaC).
- **Validation** :
  - Filter mis à jour : `aws logs describe-metric-filters` confirme le nouveau pattern JSON
  - Cause amont aussi neutralisée par PR #1361 (HF-2026-05-27-03) — défense en profondeur
  - ✅ **Confirmation prod-health-check 2026-05-28T00:15Z** : alarme `legalcase-production-backend-error-rate` en état OK stable depuis le 2026-05-27T18:36 UTC — **plus aucun flap depuis 5h40min** (vs 2 flap en 45min le 27/05 après-midi). Fix complet et durable.
- **Status** : `✅ TERMINÉ` (2 fix complémentaires appliqués 2026-05-27 : cause amont + scope du filter, validé prod-health-check 2026-05-28T00:15Z)
- **Leçon retenue** : à la création d'un metric filter sur un log group EKS shared, **TOUJOURS scoper par namespace** via filter pattern JSON. Pattern à privilégier : `{ $.kubernetes.namespace_name = "X" && $.log = "*PATTERN*" }`.

### HF-2026-05-27-02 — `value too long for type character varying(255)` sur `in_app_notifications` (PROD) ✅ TERMINÉ

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
  ```
- **Commit suspect (confirmé)** : `DeadlineAlertService.notifyMembers()` ligne 132 — `"Délai J-" + daysRemaining + " : " + deadline.getLabel()`. `deadline.getLabel()` provient de `case_deadlines.label` VARCHAR(255). Un avocat saisissant un label long fait déborder le titre concaténé > 255 chars.
- **Root cause** : la colonne `title VARCHAR(255)` (migration 052-create-in-app-notifications.xml) saturée par concaténation prefix + label dans `DeadlineAlertService` (cron `@Scheduled("0 0 8 * * *")` — confirmé par timestamp prod 08:00:06 UTC). Le try-catch ligne 135 du caller avalait l'erreur en log.warn → notification silencieusement perdue côté utilisateur final.
- **Fix** : SF-113-04 — truncation défensive centralisée dans `InAppNotificationService.create()` via `truncateWithEllipsis(value, maxLen)`. Couvre title (255) + message (1000) + link (500). Appliqué AVANT les setters. Centralisé pour couvrir les 5 callers + tout futur caller. Fixed by **PR #1367**.
- **Validation** :
  - 10/10 UT verts (`InAppNotificationServiceTest`) — 6 nouveaux scénarios (T-1 à T-6)
  - CI master verte post-merge
  - ✅ **Confirmation prod-health-check 2026-05-28T00:15Z** : pods prod basculés sur image `c26b20c` à 23:11 UTC (4 hotfixes frontend intercalés pour débloquer prod build : PR #1381/1384/1385/1386). 8 events PROD à 08:00 UTC du 27/05 (cron `DeadlineAlertService` ANTÉRIEURS au deploy 23:11). **Prochain cron 08:00 UTC du 28/05 sera le 1er test du fix en prod réelle** — si 0 event, fix validé. Re-runner `prod-health-check` après 08:00 UTC du 28/05 pour confirmation finale.
- **Status** : `✅ TERMINÉ` (Fixed by #1367, validation prod programmée 28/05 08:00 UTC sur cron `DeadlineAlertService`)
- **Notes** : choix Option (b) tronquer en code (mini-spec section « Hors périmètre ») privilégié sur ALTER COLUMN — défense en profondeur applicable à tout futur caller, ne déplace pas le problème à VARCHAR(500).

### HF-2026-05-27-03 — F-JU-01 bootstrap re-insère sans ON CONFLICT (STAGING, 200+/24h) ✅ TERMINÉ

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
- **Commit suspect (confirmé)** : `JurisprudenceBootstrapService.persistTopCandidates()` ligne 246 — `mappingRepository.save()` sans guard `existsBy…` en amont.
- **Root cause** : la contrainte unique `uq_tool_jurisprudence_mappings_active` porte sur `(tool_id, branche_calcul_id, arret_ref)`. Re-bootstrap manuel → Claude renvoie le même top-1 → INSERT → `DataIntegrityViolationException`. Le catch `RuntimeException` du callsite avale l'erreur (skipped++) mais PostgreSQL log un ERROR avant le catch Spring.
- **Fix** : SF-JU-01-14 — guard `existsByToolIdAndBrancheCalculIdAndArretRef` AVANT le `txTemplate`. Si exists → `log.info()` + `skipped++` + `continue`, zéro transaction ouverte. Fixed by **PR #1361** (commit `80afef2c`).
- **Validation** :
  - 10/10 UT verts (`JurisprudenceBootstrapServiceTest`) — 2 nouveaux scénarios T-1 (skip pur) + T-2 (mix new/existing)
  - CI master verte post-merge
  - ✅ **Confirmation prod-health-check 2026-05-28T00:15Z** : 50 events staging UNIQUEMENT entre 18:52-18:58 UTC (juste après le rolling update staging avec la nouvelle image, 6 min de cleanup d'un job résiduel), puis **0 event depuis 5+ heures**. Volume effondré 200+/24h → 50 résiduels → 0 stable. Fix prouvé efficace.
- **Status** : `✅ TERMINÉ` (Fixed by #1361, validé prod-health-check 2026-05-28T00:15Z)
- **Leçon retenue** : 4e SF F-JU-01 en moins de 7 jours qui corrige le bootstrap (SF-JU-01-09 transactions, SF-JU-01-10 async polling, SF-JU-01-13 search vs export, SF-JU-01-14 idempotence). Pattern récurrent → la prochaine itération devrait inclure un IT bout-en-bout du bootstrap.

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

- **RDS prod** : **`db.t4g.small`** (✅ upgrade SF-INFRA-01 confirmé), storage 50 GB, status `available`. **Alarmes recréées 2026-05-27T18:59 UTC** avec dimension `DBInstanceIdentifier=legalcase-production-postgres`. Métrique réelle confirmée vivante : **72 datapoints sur 6h, CPU ~4.2% (dernier datapoint), max 20 connections**. `StateReason` reste figé sur l'ancien "no datapoints received" car CloudWatch ne met à jour `StateReason` qu'aux transitions d'état OK↔ALARM ; l'état OK reste constant (valeurs < seuils 80% CPU / 100 connections / 200MB free memory), donc pas de transition. Comportement attendu. La prochaine ALARM (si elle arrive) écrira un Reason citant la vraie métrique.
- **RDS staging** : `db.t3.micro`, storage 20 GB. Stable.
- **Pods K8s** : 0 pod en non-Running, 0 restart sur tous. **Backend prod redéployé sur image `c26b20c` à 23:11/23:40 UTC** (rolling update HF-02 + HF-03), 2 pods ready, CPU 3-4m, mémoire 613/671 Mi (calme). **Frontend prod** redéployé sur `latest-prod`.
- **Cost Anomaly 7j** : RDS $1.40 au 2026-05-25 (corrélé au switch t3.micro→t4g.small, attendu) + Secrets Manager $0.04 (négligeable). Pas de nouvelle anomalie depuis.
- **Alarmes CloudWatch** : 7 configurées, **0 en ALARM**. Aucun flap depuis 18:36 UTC (HF-2026-05-27-01 fixé).
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
