# Skill — parallel-frontback-delivery

**Quand l'invoquer** : une feature comporte 2 (ou plus) SF indépendantes dont les contrats sont ou peuvent être figés avant le dev — typiquement 1 backend (endpoint API) + 1 frontend (UI consommatrice). Exemples :
- *"Implémente F-XXX en parallèle back/front"*
- *"Lance les 2 agents en parallèle"*
- *"Vas-y avec la skill parallèle"*

Cette skill **complète** `feature-autonome.md` : feature-autonome enchaîne les SF d'une feature séquentiellement, parallel-frontback-delivery les lance simultanément quand les contrats sont figés.

## Règle d'or

> **Contrat figé = parallélisation possible.** Si le contrat API est noir-sur-blanc dans la mini-spec backend (méthode, URL, body, response, erreurs, codes enum), backend et frontend peuvent partir en parallèle dans 2 worktrees isolés sans risque de divergence.

## Pré-requis non négociables

| # | Item | Conséquence si absent |
|---|------|---------------------|
| 1 | Mini-spec backend rédigée AVEC section "Contrat API" complète | REFUS — risque de divergence = dette de convergence immédiate (CLAUDE.md règle §3) |
| 2 | Mini-spec frontend rédigée référençant explicitement le contrat backend (`"contrat importé de SF-XXX-YY-backend"`) | REFUS — frontend perd la traçabilité |
| 3 | 2 branches Git distinctes (`feat/SF-XXX-YY-backend` + `feat/SF-XXX-YY-frontend`) depuis master à jour | REFUS — partage de branche interdit (CLAUDE.md règle §3) |
| 4 | 2 worktrees isolés (Agent tool avec `isolation: "worktree"`) | Pas obligatoire mais fortement recommandé — évite les conflits de fichiers générés (`node_modules`, `.mvn`) |
| 5 | Tests frontend basés sur un mock du service (ne dépendent pas du backend mergé) | REFUS — frontend bloqué par backend = pas de parallélisation effective |

## Procédure obligatoire

### Étape 0 — Annoncer le plan

Dans le 1ᵉʳ message, lister explicitement :
- *"F-XXX = SF-XXX-01-backend + SF-XXX-02-frontend en parallèle"*
- Les **contrats API figés** (méthode/URL/body/response)
- Le **temps estimé** par SF (lecture du code cible AVANT, cf. feature-autonome §1)
- L'engagement : *"Je lance les 2 agents en parallèle, je merge les 2 PR, je déploie staging, je mets à jour les docs — sans pause."*

### Étape 1 — Vérifier les artefacts pré-dev

**Avant tout `Agent tool`** :
1. ✅ `docs/PRODUCT_SPEC.md` contient F-XXX (sinon REFUS — ajouter la feature d'abord)
2. ✅ `docs/features/F-XXX/SF-XXX-01-backend.md` existe avec contrat API complet
3. ✅ `docs/features/F-XXX/SF-XXX-02-frontend.md` existe et référence le contrat
4. ✅ Readiness checklist passée (PASS) pour les 2 SF — affichée dans la conversation
5. ✅ `master` à jour localement (`git fetch && git pull`)

### Étape 2 — Lancer les 2 agents en parallèle

**Un seul message Agent tool avec 2 invocations parallèles** :

```
Agent backend (subagent_type=general-purpose, isolation=worktree):
  - Lire mini-spec SF-XXX-01-backend.md
  - Créer branche feat/SF-XXX-01-backend depuis origin/master
  - Implémenter endpoint + service + tests unitaires + tests intégration
  - Compiler + tests verts (mvn -pl backend test)
  - Commit + push + gh pr create
  - Retourner : URL PR + résumé arbitrages techniques

Agent frontend (subagent_type=general-purpose, isolation=worktree):
  - Lire mini-spec SF-XXX-02-frontend.md (avec contrat API)
  - Créer branche feat/SF-XXX-02-frontend depuis origin/master
  - Implémenter composant + service avec MOCK du backend + tests Jest
  - Build + tests verts (npm run build && npm test -- --watchAll=false)
  - Commit + push + gh pr create
  - Retourner : URL PR + résumé arbitrages techniques
```

**Briefer les agents avec self-check grep pré-commit** (cf. memory `feedback_self_check_grep_pre_commit.md`) : avant le commit final, l'agent vérifie qu'il n'a pas oublié les patterns canoniques (handlers manquants, imports orphelins, etc.).

### Étape 3 — Vérifications avant merge

**Avant tout `gh pr merge`** :

1. **Endpoint check** (cf. memory `feedback_pre_merge_endpoint_check.md`) : pour la PR frontend, vérifier que les endpoints consommés (`HttpClient.post/get/...` sur `/api/v1/...`) existent côté backend (sur master OU dans la PR backend ouverte).
2. **Visibility seed check** (cf. memory `feedback_pre_merge_visibility_seed_check.md`) : si le backend INSERT/UPDATE dans `decision_tool_visibility_rules`, vérifier `TOOL_REGISTRY` frontend + `KNOWN_FRONTEND_TOOL_IDS`. (Hors-sujet pour la plupart des features non décisionnelles.)
3. **CI verte** : `gh pr checks <PR#>` → tous les checks ✅.
4. **Review checklist** passée et affichée pour chaque PR (CLAUDE.md règle §4).

### Étape 4 — Merger les 2 PR

**Ordre recommandé** : backend AVANT frontend, pour éviter le runtime 404 si auto-deploy.

```bash
gh pr merge <PR-backend> --squash --delete-branch --admin
gh pr merge <PR-frontend> --squash --delete-branch --admin
```

Si la CI échoue post-merge sur l'une des branches, créer un `fix/SF-XXX-YY-name` et merger dans le même flow (cf. feature-autonome §3).

### Étape 5 — Déploiement staging

- Backend : `gh workflow run backend.yml --ref master`
- Frontend : `gh workflow run frontend.yml --ref master` (ou auto-trigger selon config)
- Health check : `curl https://staging.legalcase.ng-itconsulting.com/api/actuator/health`
- Suivre les 2 runs en parallèle via `gh run watch <run-id>` lancé en background.

### Étape 6 — Docs post-merge groupées

**1 seul commit** `docs/F-XXX-complete-YYYY-MM-DD` qui :
- Marque F-XXX `Terminée` dans `PRODUCT_SPEC.md` (les 2 SF sont mergées)
- Ajoute 2 entrées dans l'historique des évolutions (1 par SF — chaque SF mérite sa traçabilité)
- Mentionne les arbitrages techniques pour chaque SF
- Si nouvelle table : met à jour `ARCHITECTURE_CANONIQUE.md`
- Si feature couvre une tâche marketing M-XX : marquer M-XX comme `Couvert par F-XXX` dans `MARKETING_BACKLOG.md`

### Étape 7 — Récap unique à la toute fin

Un seul récap qui contient :
- Les 2 PR mergées + URLs commits
- Les 2 runs CI verts
- L'état staging (URL + healthcheck)
- Les arbitrages techniques pris dans chaque SF
- Les risques résiduels à surveiller
- Le statut M-XX (si couvert)

## Anti-patterns interdits

| ❌ Interdit | ✅ Faire |
|------------|---------|
| "Tu veux multi-agent ou séquentiel ?" | Lancer en parallèle directement (mode normal du projet) |
| Lancer 2 agents sans contrat API figé | Figer le contrat dans la mini-spec backend AVANT |
| Brief agent sans mention du self-check grep pré-commit | Inclure systématiquement (memory feedback_self_check_grep_pre_commit) |
| Merger frontend AVANT backend | Backend toujours d'abord (sinon 404 runtime sur prod) |
| Récaps intermédiaires entre les 2 SF | 1 récap unique à la TOUTE fin |
| Demander confirmation entre merge backend et merge frontend | Enchaîner directement (les checks sont déjà passés) |
| Docs post-merge en 2 PR séparées | 1 seul commit groupé qui couvre les 2 SF |

## Cas d'arrêt légitime

Stopper et demander uniquement si :
- Le contrat API doit être modifié en cours de dev (cas rare, mais réel) — **stopper les 2 agents**, mettre à jour la mini-spec, puis relancer.
- Un agent révèle un risque sécurité critique (faille endpoint, leak workspace_id…)
- Une PR a un conflit de fichier non trivial avec une autre branche en cours (édition concurrente)
- L'utilisateur dit explicitement "stop"

Dans tous les autres cas, continuer.

## Combinaison avec d'autres skills / mémoires

- `feature-autonome.md` : si F-XXX comporte > 2 SF dont certaines en parallèle et d'autres séquentielles, alterner les modes — parallèle quand contrats figés, séquentiel sinon.
- `frontend-coherence-audit.md` : pour les SF frontend décisionnelles (panel F-IA-04), invoquer aussi cette skill côté agent frontend.
- `feedback_autodeploy.md` : merger + deploy staging dès que green sans demander.
- `feedback_audit_obligatoire_avant_vague.md` : si la SF est dans une vague d'outils décisionnels, audit de couverture AVANT le lancement parallèle.

## Origine

Cette skill formalise un pattern utilisé en pratique sur les vagues 10 à 19 du projet (entre 2026-04-22 et 2026-05-02), où plusieurs SF backend et frontend ont été livrées en parallèle via Agent tool avec worktrees isolés. La gouvernance descriptive (CLAUDE.md §"Parallélisation backend / frontend") + le feedback mémoire (`feedback_parallel_frontback_default.md`) restent valides — cette skill est leur version exécutable.
