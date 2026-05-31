# Skill — autonomous-delivery-wave

> **Le « super prompt » de livraison autonome multi-features.**
> Livre une *vague* de N features (cible 10) du backlog `PRODUCT_SPEC.md`, de bout en bout,
> en équipe d'agents coordonnés, en respectant toute la gouvernance CLAUDE.md,
> sur la dev workstation AWS always-up. Ne crée **jamais** de feature nouvelle —
> il consomme uniquement ce qui existe déjà dans la spec.

**Quand l'invoquer** :
- *« Lance une vague autonome de 10 features »*
- *« Vas-y sur le backlog, prends tes décisions, livre-moi le récap au matin »*
- *« Nocturne : enchaîne les features À faire, je lis le récap demain »*

Cette skill **chapeaute** `feature-autonome.md` (autonomie intra-feature) et
`parallel-frontback-delivery.md` (parallélisme back/front). Elle ajoute la couche
**inter-features** qui manquait : file d'attente, classification de risque, boucle de
livraison, plafond budget, récap d'arbitrages.

---

## Régime d'autonomie (décisions PO figées 2026-05-30)

| Dimension | Choix | Conséquence |
|---|---|---|
| **Gate produit** (cohérence STOP, OPEN_QUESTION, nouvelle table, cohérence écran) | **Décider par défaut + flag** | L'agent prend sa meilleure décision, l'implémente, et la **trace dans les arbitrages**. PAS de pause. |
| **HALT dur** (exception au décider-par-défaut) | Irréversible / sécurité critique | Migration destructive (DROP/ALTER perte de données), faille sécurité, suppression de données prod, décision coûteuse non réversible → **STOP + demander**. |
| **Autorité de merge** | **Auto-merge `--admin`** | Checklists vertes → merge + deploy staging sans pause. Gardé par hooks pré-merge. |
| **File des features** | **Audit auto + PRODUCT_SPEC** | Audit couverture obligatoire puis tirage des N prioritaires « À faire ». Zéro saisie. |
| **Mode d'exécution** | **Nocturne `/loop` + plafond token** | Tourne sur la box AWS. Plafond budget token DUR (le Workflow throw au plafond). Récap au matin. |

> ⚙️ **Paramètre à fixer à chaque lancement** : `BUDGET_TOKENS` (plafond dur de la vague).
> Défaut conservateur proposé : **2 000 000 tokens output** (~10 features mécaniques).
> Passé en `+2M` dans la directive de lancement, ou via `args.budget` du Workflow.

---

## Principe directeur

> **Une vague = N features livrées et mergées, OU une raison écrite pour chaque feature non livrée.**
> Le débit n'autorise jamais à sauter un artefact de gouvernance. La vélocité vient du
> parallélisme et de la suppression des pauses de confirmation — **pas** du contournement des gates.

Aucune feature inventée. **REFUS** immédiat si une feature candidate n'est pas dans `PRODUCT_SPEC.md`.

---

## Procédure

### Phase 0 — Bootstrap état (toujours, avant tout)

1. **Synchroniser la vérité git** : `git fetch origin` puis raisonner sur **`origin/master`**, jamais sur le working tree courant (mémoire `feedback_verifier_master_pas_branche_courante`, `feedback_recheck_master_par_phase`). Repo multi-session : master bouge pendant le travail.
2. **Détecter le travail en cours non terminé** — produire un état avant de démarrer du neuf :
   - ⚠️ **NE PAS se fier à `git branch --no-merged`** : le repo merge en **squash** (`--squash`), donc le tip d'une branche livrée n'est jamais ancêtre de master → toutes les branches squash-mergées apparaissent faussement « non mergées ». **Incident dry-run 2026-05-30** : l'agent de bootstrap a pris ~45 branches squash-mergées (F-213, F-219, **toutes TERMINÉES** dans PRODUCT_SPEC) pour du travail à finir. En mode auto-merge, ça aurait regénéré du travail déjà livré.
   - **Source de vérité du « en cours » = PRODUCT_SPEC.md + PR**, pas les branches locales :
     - `gh pr list --state open` → vraies PR en cours (vérifier qu'elles ne sont pas *superseded* par une PR déjà mergée du même outil).
     - Statut dans `PRODUCT_SPEC.md` : une feature « 🎉 Terminée » est **finie**, quelles que soient les branches locales qui traînent.
     - Pour une branche suspecte, tester le **contenu** pas le tip : `git cherry origin/master feat/SF-X` (sortie vide ⇒ déjà dans master) ou grep des fichiers clés de la SF dans `origin/master`.
     - Dossiers `docs/features/F-*/` avec mini-spec mais feature **non** « Terminée » dans `PRODUCT_SPEC.md` (vraie SF spec-only jamais livrée).
     - SF mergées mais **docs post-merge manquantes** (PRODUCT_SPEC pas à jour).
   - ⚠️ **Détecter les sessions parallèles actives** : `git reflog -10` (pulls/rebases récents non émis par cette session) + présence de `/tmp/parallel-session-untracked/` (une autre session a déplacé des untracked pour rebaser). Si une session parallèle travaille → **coordonner / décaler** (incident 2026-05-30 : F-222 édité par une autre session pendant ce setup). Commit early & commit often, worktrees isolés (mémoires `feedback_parallel_session_branch_name_collision`, `feedback_parallel_agents_isolated_worktree`).
   - **Hygiène** : signaler (ne pas supprimer en auto) les branches locales mortes (squash-mergées) à purger.
   - **Décision** : toute SF **réellement** en cours est **reprise et finie en priorité** avant d'entamer une feature neuve. Une branche morte n'est PAS du travail en cours.
3. **Lire la mémoire** : `MEMORY.md` + les feedbacks pertinents (rate-limit, worktrees, collisions branches, docs→redeploy, Liquibase, pré-merge checks). Ces feedbacks sont des **contraintes dures**, pas des suggestions.
4. **Vérifier les credentials AWS / l'état de la box** si run sur la workstation (cf. mémoire `project_dev_workstation_ec2`).

### Phase 1 — Audit de couverture + file d'attente (obligatoire)

1. **Audit de couverture** des 3 domaines métier (règle mémoire `feedback_audit_obligatoire_avant_vague` + `project_coverage_audit_every_10_features`). Spawner un agent `Explore` qui mappe le couvert/non-couvert FR **et** BE (mémoire `feedback_belgique_never_forget` : couverture exhaustive BE, pas miroir FR).
2. **Construire la file** des N features candidates depuis `PRODUCT_SPEC.md` statut « À faire » (jamais « Backlog » sans GO étape 0, jamais « Bloqué »). Ordonner par : dépendances → valeur produit → effort (lecture rapide du code cible, cf. `feature-autonome` §1).
3. **Annoncer la file** dans le 1ᵉʳ message : *« Vague = [F-x, F-y, … N features]. Ordre : … J'enchaîne sans pause, récap unique à la fin. »* — engagement explicite (mémoire `feedback_finir_signifie_finir` : lister TOUTES les SF au départ, ne pas réduire le scope silencieusement).

### Phase 2 — Classification de risque (label, pas gate)

Pour chaque feature, étiqueter **🟢 vert / 🟠 orange / 🔴 rouge** (sert au récap et au choix du chemin) :

| Label | Critère | Chemin |
|---|---|---|
| 🟢 vert | Spec claire, pattern réutilisable, FR-only ou parité connue, pas de nouvelle table, pas d'OPEN_QUESTION | Livraison full-auto, back/front //. |
| 🟠 orange | Touche un gate produit *réversible* (cohérence écran, choix UX, nouvelle table simple) | **Décider par défaut + flag** : meilleure décision, implémentée, tracée dans arbitrages. |
| 🔴 rouge | Touche l'irréversible / sécurité / coûteux non réversible / OPEN_QUESTION structurante | **HALT** : ne pas livrer en aveugle. Documenter la question, passer à la feature suivante, lister en tête du récap. |

> La classe 🔴 est la **seule** entorse au « décider par défaut » — par sécurité, pas par confort.

### Phase 3 — Boucle de livraison (le Workflow déterministe)

Exécutée par le script `autonomous-delivery-wave.js` (substrat = outil **Workflow**, pas un prompt qui « espère tenir »). Pour chaque feature de la file :

1. **Étapes 0 / 0bis / 1 / 2** de CLAUDE.md : cadrage cohérence (le challenger rend un verdict ; sur 🟠 on prend la reco par défaut), cohérence écran si impact écran, mini-spec(s) SF, readiness checklist — **artefacts produits dans `docs/features/F-XX/`**.
2. **Dev** : `pipeline()` des SF. Back + front **en parallèle** quand le contrat API est figé (`parallel-frontback-delivery`), sinon séquentiel. Chaque agent dans un **worktree isolé** (`isolation: "worktree"` — mémoires `feedback_parallel_agents_isolated_worktree`, `feedback_parallel_session_branch_name_collision`). Pré-assigner UUIDs Liquibase + numéros migration dans chaque brief (mémoire `feedback_parallel_agents_uuid_collision`).
   - **Concurrence (`CONCURRENCY`, défaut 6)** : la limite n'est **pas** le CPU de la box mais le **rate-limit Anthropic** (TPM/RPM du tier, partagé par tous les agents). La box puissante parallélise le **local** (mvn/npm/worktrees), pas les appels API. `Workflow` plafonne nativement à `min(16, cores-2)` = 6 ici. **Calibrer empiriquement** : monter `CONCURRENCY` jusqu'à voir des 429, puis redescendre d'un cran. Le vieux « max 2-3 » (mémoire `feedback_max_2_parallel_agents`) était un plancher observé sur l'ancien spawn `claude` CLI — avec `Workflow` (file d'attente intégrée, excédent mis en queue) on peut viser plus haut tant que le tier suit.
3. **Self-check grep pré-commit** dans chaque brief d'agent front décisionnel (mémoire `feedback_self_check_grep_pre_commit`).
4. **Compile + tests verts** : `mvn -pl backend test` + `npm run build && npm test`. Smoke E2E si préoccupation transversale touchée (`cd e2e && npm test`).
5. **Review checklist + Release checklist** passées et **affichées** (artefacts CLAUDE.md §4/§5). Gardées par hooks (cf. §Hooks).
6. **PR + merge** : `gh pr create` puis `gh pr merge --squash --delete-branch --admin`. **Backend AVANT frontend** (évite 404 runtime). Vérifs pré-merge : endpoints back existants + visibility seed (mémoires `feedback_pre_merge_endpoint_check`, `feedback_pre_merge_visibility_seed_check`).
7. **Master-red post-merge** → `fix/SF-XX-YY` dans le même flow (feature-autonome §3). Si non résolu en **2 tentatives / 30 min** → revert le merge, parker la feature, continuer (décider-par-défaut : ne pas bloquer la vague sur une feature).
8. **Itérer** : feature suivante, sans pause.

> ⚠️ **Couplage CI** : `docs/PRODUCT_SPEC.md` est dans les paths-trigger de `backend.yml` → chaque commit docs déclenche un rolling update qui **tue les jobs async** (mémoires `feedback_docs_change_triggers_backend_redeploy`, `feedback_never_rerun_ci_with_active_async_job`). **Donc : aucun commit docs en cours de vague.** Tous les docs post-merge sont **groupés et poussés à la toute fin** (Phase 4), et le **déploiement staging est unique, en fin de vague**.

### Phase 4 — Docs post-merge groupées + staging unique

1. **1 seul commit docs** `docs/wave-YYYY-MM-DD-complete` (date passée en `args`, jamais `Date.now()`) : statuts `Terminée` dans `PRODUCT_SPEC.md`, 1 entrée historique **par SF**, MAJ `ARCHITECTURE_CANONIQUE.md` si nouvelles tables, MAJ `MARKETING_BACKLOG.md` si une feature couvre une tâche M-XX.
2. **Déploiement staging unique** : `gh workflow run backend.yml --ref master` + front auto, healthcheck `curl https://staging.legalcase.ng-itconsulting.com/api/actuator/health`. Suivi en background, pas de pause.
3. Sync backlog DB = automatique (cron 5 min, F-178) — aucun artefact.

### Phase 5 — Récap unique d'arbitrages (le livrable du matin)

UN seul récap final structuré (schéma imposé pour fiabilité, cf. Workflow) :

- **Features livrées** : par feature → PR mergées + commits + runs CI verts.
- **Liste des ARBITRAGES** : pour chaque décision-par-défaut prise sur un gate 🟠, *quoi / pourquoi / alternative écartée / réversibilité*. C'est le cœur de la transparence promise.
- **Features 🔴 HALT** parkées : feature + question exacte à trancher + ce qui bloquait.
- **Features non atteintes** (budget/échec) : laquelle + raison + état (branche/PR).
- **État staging** : URL + healthcheck + runs.
- **Risques résiduels** à surveiller (ex. nouveau job async qui consomme des tokens, nouvelle table à indexer).
- **Couverture** : ce que la vague a fait avancer FR/BE, prochains trous.

---

## Garde-fous déterministes — Hooks (recommandé, rend la gouvernance non-contournable)

La gouvernance CLAUDE.md repose aujourd'hui sur la discipline du modèle, qui **dérive sur 10 features**. Les hooks la rendent dure (voir `settings.json`) :

| Hook | Matcher | Effet |
|---|---|---|
| `PreToolUse` | `Bash` `git push` | Bloque si aucun artefact review-checklist du jour dans la conversation/run. |
| `PreToolUse` | `Bash` `gh pr merge` | Vérifie endpoints back + visibility seed avant merge. |
| `PreToolUse` | `Bash` édition `backlog_*` | Refuse l'édition directe DB (source = MD). |
| `PostToolUse` | `Edit` front décisionnel | Lance le self-check grep automatiquement. |
| `SubagentStop` / `Stop` | — | Smoke E2E si préoccupation transversale touchée. |

> Ces hooks ne remplacent pas la skill : ils l'**assurent**. Sans eux, la vague reste possible mais repose sur la vigilance du modèle.

---

## Anti-patterns interdits (hérités + nouveaux)

| ❌ Interdit | ✅ Faire |
|---|---|
| Inventer une feature absente de PRODUCT_SPEC | REFUS — consommer la spec uniquement |
| Réduire silencieusement la file (« les 6 plus faciles ») | Lister les N au départ, livrer ou justifier chacune |
| Commit docs en cours de vague | Docs groupées en fin (couplage CI redeploy) |
| Déployer staging par feature | 1 déploiement staging unique en fin |
| Pause de confirmation entre features | Enchaîner ; récap unique à la fin |
| Décider en aveugle sur de l'irréversible | HALT 🔴 + parker + tracer |
| > 3 agents claude concurrents | Plafonner à ~2-3 (rate-limit) |
| Partager une branche entre 2 agents // | 1 worktree isolé par agent |
| Ignorer le plafond budget | Le Workflow throw au plafond — c'est voulu |

---

## Exécution nocturne sur la box AWS

```
# Sur la dev workstation (legalcase start si éteinte), dans tmux :
cd ~/dev/legalCase && claude
> /loop  (puis cette skill, ou directement)
> Lance une vague autonome de 10 features, +2M tokens, récap au matin
```

Le `/loop` self-pace les itérations ; le plafond budget borne le coût ; le récap final
est lu au réveil. Resume possible (`resumeFromRunId`) si la box redémarre.
