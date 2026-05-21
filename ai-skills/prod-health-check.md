# Skill — prod-health-check

**Quand l'invoquer** : l'utilisateur demande un audit de santé de la production et veut une liste de hotfix candidats à examiner, p. ex. :
- *"Lance le prod health check"*
- *"Vérifie l'état de la prod et liste les hotfix à faire"*
- *"On audite la prod ce matin ?"*
- Via `/prod-health-check`

## Règle d'or

> **Cette skill REPÉRE et REPERTORIE — elle ne corrige RIEN.** L'humain garde la main sur le tri et l'implémentation des hotfix. Le seul artefact produit est `docs/operations/hotfix-prod.md` mis à jour + commit + push direct sur master.

## Pré-requis runtime

| Pré-requis | Commande de vérification | Action si KO |
|---|---|---|
| AWS CLI authentifié | `AWS_PROFILE=legalcase-terraform aws sts get-caller-identity` | Demander à l'utilisateur de renouveler ses credentials (`aws sso login` ou `aws configure`) |
| kubectl context `legalcase-shared` | `kubectl config current-context` | `aws eks update-kubeconfig --region eu-west-3 --name legalcase-shared --profile legalcase-terraform` |
| Repo `legalCase` clean working tree | `git status --short` | Stash ou demander quoi faire |

**Toutes les commandes AWS doivent préfixer `AWS_PROFILE=legalcase-terraform`** (le profil par défaut n'est pas authentifié).

## Procédure obligatoire

### Étape 1 — Audit 4 dimensions (en parallèle si possible)

**Dimension A — Alarmes CloudWatch**

```bash
AWS_PROFILE=legalcase-terraform aws cloudwatch describe-alarms \
  --state-value ALARM \
  --output json
```

Pour chaque alarme en `ALARM` :
- Récupérer son nom, métrique, seuil, datapoints récents (`describe-alarm-history` sur 24h)
- Identifier le contexte (prod vs staging, backend vs infra)

Lister aussi les alarmes en `OK` qui ont eu un basculement récent (≤ 7 jours) via `describe-alarm-history` — utile pour détecter les patterns intermittents.

**Dimension B — Patterns logs ERROR**

Sur la fenêtre des 24 dernières heures, dans le log group `/aws/eks/legalcase-shared/applications` :

```bash
AWS_PROFILE=legalcase-terraform aws logs filter-log-events \
  --log-group-name "/aws/eks/legalcase-shared/applications" \
  --start-time $(($(date +%s) - 86400))000 \
  --filter-pattern "ERROR" \
  --max-items 200
```

Puis grouper par signature :
- **Hash le 1er frame du stack trace** (1ère ligne de `at fr.ailegalcase.*` ou équivalent) pour dédupliquer
- Compter les occurrences par groupe
- Identifier les **nouveaux groupes** (apparus depuis le dernier run de cette skill — voir Étape 3 pour la persistance)

Ignorer les patterns classés comme bruit dans le fichier `docs/operations/hotfix-prod-noise-patterns.md` (créer ce fichier vide au 1er run, l'humain l'enrichira pour exclure les faux positifs récurrents type "OAuth2 invalid_grant" benins).

**Dimension C — Santé pods K8s**

```bash
kubectl get pods --all-namespaces \
  --field-selector=status.phase!=Running,status.phase!=Succeeded \
  -o json
kubectl top pods --all-namespaces 2>&1 | head -30
kubectl get pods --all-namespaces -o json | jq '.items[] | select(.status.containerStatuses[]?.restartCount > 3) | {ns: .metadata.namespace, name: .metadata.name, restarts: .status.containerStatuses[0].restartCount}'
```

Détecter :
- `CrashLoopBackOff` (pod redémarre en boucle)
- `OOMKilled` (mémoire saturée)
- Restart count > 3 sur les dernières 24h
- CPU > 80% ou Memory > 80% sur les pods backend

**Dimension D — Coûts / capacité AWS**

```bash
AWS_PROFILE=legalcase-terraform aws ce get-anomalies \
  --date-interval StartDate=$(date -d '7 days ago' +%Y-%m-%d),EndDate=$(date +%Y-%m-%d) \
  --output json
```

Et comparer les métriques RDS clés vs J-7 :
- `AWS/RDS/DatabaseConnections` (max sur 24h)
- `AWS/RDS/CPUUtilization` (avg sur 24h)
- `AWS/RDS/FreeableMemory` (min sur 24h)
- `AWS/RDS/FreeStorageSpace` (min — tendance disque)

Détecter une dégradation significative (> 25 % de variation vs semaine précédente).

### Étape 2 — Classification par severity

| Severity | Critère |
|---|---|
| **P0 — Prod down** | Alarme prod en `ALARM` + pod backend en `CrashLoopBackOff` OU > 100 erreurs/h en cours |
| **P1 — Dégradation significative** | Alarme prod en `ALARM` (autres cas) OU pattern d'erreur récurrent > 20 occurrences/24h OU restart count pod > 10 |
| **P2 — Nuisance / bruit** | Pattern d'erreur < 20 occurrences/24h OU alarme staging en `ALARM` OU dégradation < 25 % vs J-7 |

### Étape 3 — Cross-référence deploy/commit

Pour chaque issue détectée, identifier le commit potentiellement responsable :

```bash
# Dernier deploy prod
git log --oneline origin/master --since="7 days ago" | head -10
```

Si une issue est apparue dans les 24-48h qui suivent un commit master, ajouter dans le hotfix un champ `commit_suspect` avec le SHA.

Si l'humain a déjà annoté un hotfix existant avec `Fixed by #1234`, vérifier que la PR #1234 est mergée — si oui, déplacer l'entrée vers la section "Terminés".

### Étape 4 — Mise à jour de `docs/operations/hotfix-prod.md`

**Lire l'existant** puis appliquer ces règles de fusion :

1. **Nouveaux items** : ajouter sous le bon header severity avec status `À TRIER`
2. **Items déjà présents** (matching par hash signature) :
   - Si toujours actif → incrémenter `occurrences_24h` et `last_seen`
   - Si annoté `Fixed by #PR` et la PR mergée → déplacer vers "✅ Terminés"
   - Si annoté `Ignored` → ne pas re-lister (l'humain a tranché que c'est du bruit accepté)
3. **Items obsolètes** : si un item en "À TRIER" ou "À FAIRE" n'apparaît plus depuis 7 jours → déplacer vers "✅ Terminés" avec note `auto-resolved (plus observé)`
4. **Archive** : items en "✅ Terminés" depuis > 30 jours → déplacer dans `docs/operations/hotfix-prod-archive.md` (créer si absent)

Format de chaque entrée (voir gabarit dans `docs/operations/hotfix-prod.md` initial) :

```markdown
### HF-YYYY-MM-DD-NN — Titre court factuel
- **Détecté** : YYYY-MM-DDTHH:MM:SSZ (alarme `nom-alarme` ou pattern logs)
- **Première occurrence** : YYYY-MM-DD
- **Dernière occurrence** : YYYY-MM-DD
- **Occurrences 24h** : N
- **Total observé** : N
- **Signature** : `hash:abc123…` (1er frame stack ou message clé)
- **Logs sample** : 2-3 lignes représentatives (tronquer à 200 chars)
- **Commit suspect** : `<sha>` (ou `aucun`)
- **Hypothèse** : 1-2 phrases d'analyse rapide
- **Status** : `À TRIER` | `À FAIRE` | `EN COURS` | `IGNORÉ` | `✅ TERMINÉ`
- **Notes** : annotations humaines libres (vide au départ)
```

### Étape 5 — Commit + push direct master

Vu que cette skill est purement diagnostique (pas de feature code), bypass la séquence CLAUDE.md complète :

```bash
git add docs/operations/hotfix-prod.md docs/operations/hotfix-prod-archive.md 2>/dev/null
git commit -m "ops(prod-health-check): scan $(date -u +%Y-%m-%dT%H:%MZ) — N nouveaux items, M déplacés en Terminés"
git push origin master
```

Si la branche locale master n'est pas à jour : `git pull --ff-only origin master` avant.

### Étape 6 — Résumé pour l'utilisateur

Afficher en fin de skill un résumé court :

```
✅ Scan prod terminé (YYYY-MM-DDTHH:MM:SSZ)

🔴 P0 : 0 item(s)
🟠 P1 : N item(s) — dont X nouveaux
🟡 P2 : N item(s) — dont X nouveaux
✅ Terminés (auto) : N item(s)
🗄️ Archivés : N item(s)

Fichier mis à jour : docs/operations/hotfix-prod.md (commit <sha>, push master).
```

**Si P0 détecté** : annoncer explicitement *"⚠️ P0 détecté — production cassée ou dégradée. Tu veux que je te montre le détail tout de suite ?"*. Mais ne PAS implémenter le hotfix.

## Cas d'erreur

| Cas | Action |
|---|---|
| AWS CLI non authentifié | Stopper, demander renouvellement credentials |
| kubectl context absent | Stopper, suggérer la commande `update-kubeconfig` |
| Working tree sale sur master | Stopper, demander quoi faire (l'utilisateur peut être en train de bosser sur autre chose) |
| Aucun changement détecté (rien à mettre à jour) | Ne pas commit/push, juste afficher *"État stable — aucun nouvel item, dernière analyse il y a Xh"* |
| `hotfix-prod.md` introuvable | Le créer avec le squelette du template |

## Non-objectifs

- ❌ **Implémenter** les hotfix (l'humain s'en charge)
- ❌ **Décider** quelle issue est prioritaire (l'humain trie, le `À TRIER` est juste un défaut)
- ❌ **Notifier** l'utilisateur par email/Slack (l'utilisateur consulte le fichier quand il veut)
- ❌ **Modifier** le code applicatif (juste de la doc)

## Itération future

Si l'utilisateur veut automatiser :
- `/schedule "0 8 * * *" /prod-health-check` (Claude Code Schedule, ~10€/mois)
- Ou GitHub Actions cron + Anthropic SDK direct (~3€/mois, plus de setup)

Si l'utilisateur veut enrichir :
- Ajouter détection de `dependabot` PRs non mergées depuis > 30j (dette de sécurité)
- Ajouter scan SonarCloud / coverage drop
- Ajouter scan Stripe disputed payments / chargebacks
