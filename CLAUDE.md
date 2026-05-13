# CLAUDE.md — Instructions projet AI LegalCase

## Documents à lire en priorité

Lire ces documents avant toute réponse impliquant du code, une spec ou une décision technique.

### Architecture
1. `docs/ARCHITECTURE_CANONIQUE.md` — source de vérité architecture (obligatoire)
2. `docs/PRODUCT_SPEC.md` — source de vérité fonctionnelle — liste officielle des features (obligatoire)
3. `docs/OPEN_QUESTIONS.md` — registre des sujets non tranchés (obligatoire)
4. `docs/DESIGN_SYSTEM.md` — charte graphique et règles UI (obligatoire pour tout travail frontend)

### Process
5. `project-governance/playbooks/feature-lifecycle.md` — cycle de vie des features
6. `project-governance/playbooks/definition-of-done.md` — critères de complétion
7. `project-governance/playbooks/coding-rules.md` — conventions de code
8. `project-governance/playbooks/review-rules.md` — critères de review
9. `project-governance/playbooks/testing-strategy.md` — stratégie de test

### Checklists
10. `project-governance/checklists/readiness-checklist.md` — avant de démarrer le dev
11. `project-governance/checklists/review-checklist.md` — avant toute PR
12. `project-governance/checklists/release-checklist.md` — avant tout merge

### Gouvernance détaillée
13. `docs/governance/automatic-blockers.md` — tableau complet des 41 blocages automatiques (refus / motivations / cas historiques)
14. `docs/DEVELOPMENT.md` — commandes de développement local (backend/frontend/H2/PostgreSQL)
15. `docs/DEPLOYMENT.md` — commandes de déploiement cloud (staging/prod, kubectl, gh workflow)

---

## Règles impératives

### Architecture
- Ne pas réinventer la stack, l'authentification, le modèle multi-tenant ou le modèle de données sans signaler explicitement une variante
- Le client est un workspace, pas un simple utilisateur
- L'utilisateur est une personne physique rattachée à un workspace
- La V1 cible le droit du travail uniquement
- L'auth V1 repose sur Spring Security + OAuth2/OIDC avec Google et Microsoft — aucun mot de passe local
- Backend : Spring Boot | Frontend : Angular | Base : PostgreSQL | Stockage : Object storage S3-compatible
- Les analyses de dossiers sont asynchrones
- Le pipeline IA fonctionne à 3 niveaux : chunk → document → dossier
- L'IA peut poser des questions interactives à l'avocat

---

## Suggestion de la prochaine feature

Quand l'utilisateur dit "on continue", "qu'est-ce qu'on fait ensuite" ou toute formulation équivalente sans préciser de feature :

1. Lire la section **"Features hors V1 (backlog)"** de `docs/PRODUCT_SPEC.md`
2. Proposer les 3 features les plus pertinentes du backlog, regroupées par thème (UX rapide / infrastructure / nouvelle capacité)
3. Ne jamais inventer une feature absente du backlog — si une idée est nouvelle, la soumettre d'abord à validation avant de l'ajouter au backlog et de la proposer
4. Toute feature choisie doit passer par la séquence obligatoire (mini-spec → readiness → dev → review → push)

**REFUS si** : une feature est implémentée sans être référencée dans `docs/PRODUCT_SPEC.md`.

---

## Séquence obligatoire par subfeature

Ce cycle est non négociable. Chaque étape produit un artefact visible dans la conversation.
**Sans l'artefact de l'étape N, l'étape N+1 est refusée.**

```
[1] Mini-spec → [2] Readiness → [3] Dev → [4] Review → [5] Push + Release checklist + PR (atomique) → [6] Docs post-merge → [7] Sync backlog DB
```

### Étape 1 — Mini-spec (ARTEFACT : document SF-XX rempli)

**Avant d'écrire la moindre ligne de code**, produire le fichier mini-spec en utilisant `project-governance/templates/subfeature-template.md` comme base.

Le fichier doit contenir :
- Objectif en une phrase
- Comportement nominal + cas d'erreur
- Critères d'acceptation vérifiables
- Plan de test minimal (unitaires + intégration + isolation workspace)
- Tables / endpoints / composants impactés
- Ce qui est hors périmètre

Le fichier est créé dans `docs/features/F-XX/SF-XX-YY-nom.md` et son contenu est affiché dans la conversation.

**REFUS si** : le dev démarre sans que ce fichier soit produit et visible dans la conversation.

---

### Étape 2 — Readiness checklist (ARTEFACT : checklist passée item par item)

Avant de créer la branche et d'écrire le code, passer `project-governance/checklists/readiness-checklist.md` et afficher le résultat dans la conversation avec un verdict PASS / FAIL explicite.

**REFUS si** : le premier commit est créé sans que la readiness checklist ait été passée dans cette conversation.

---

### Étape 3 — Dev

Travailler sur une branche `feat/SF-XX-YY-nom-court` créée depuis `master` à jour.
Respecter `project-governance/playbooks/coding-rules.md`.
Toute décision technique non prévue dans la mini-spec est documentée dans la PR.

#### Parallélisation backend / frontend (optionnelle)

Quand une feature comporte une subfeature backend et une subfeature frontend indépendantes (le frontend consomme une API exposée par le backend), les deux SF peuvent être développées **en parallèle** à condition que :

1. **Le contrat API est figé avant le dev dans la mini-spec** — chaque SF contient explicitement la section "Contrat API" avec :
   - Méthode HTTP + URL exacte
   - Schema du body de requête (champs, types, validation)
   - Schema de la réponse (tous les champs retournés)
   - Codes d'erreur et messages attendus
   - Codes enum éventuels (valeurs exactes et cas d'emploi)
2. **Chaque SF travaille sur sa propre branche** — `feat/SF-XX-YY-backend` et `feat/SF-XX-YY-frontend`, créées depuis `master` à jour.
3. **Chaque SF produit une PR indépendante** — elles peuvent être mergées dans n'importe quel ordre, mais la frontend ne sera utilisable en production qu'après le merge du backend.
4. **Les tests frontend utilisent un mock du service** — pas besoin d'attendre le backend mergé pour faire passer les tests unitaires Jest. L'intégration réelle (end-to-end) est validée après merge des deux PRs.
5. **Les deux mini-specs se référencent mutuellement** — la SF frontend indique "contrat importé de SF-XX-YY-backend" pour traçabilité.

**REFUS si** : deux SF sont lancées en parallèle sans que le contrat API soit présent explicitement dans la mini-spec backend — risque de divergence = dette de convergence immédiate.

**REFUS si** : deux SF parallèles partagent la même branche Git — elles doivent être strictement isolées.

Cette règle s'applique uniquement quand la parallélisation est **explicitement décidée**. Le mode sequentiel standard (SF backend mergée → SF frontend démarrée) reste le défaut.

---

### Étape 4 — Review checklist (ARTEFACT : checklist passée item par item)

Avant tout `git push`, lire `project-governance/checklists/review-checklist.md` et afficher le résultat dans la conversation avec un verdict PASS / FAIL explicite et les items bloquants identifiés.

Les items bloquants doivent être corrigés avant le push. Un item non bloquant peut être poussé avec une note explicite.

**REFUS si** : `git push` est exécuté sans que la review checklist ait été passée et affichée dans cette conversation.

---

### Étape 5 — Push, Release checklist et PR (étape atomique, non séparable)

Ces trois actions forment un bloc indivisible exécuté dans cet ordre exact :

1. `git push -u origin feat/SF-XX-YY-nom-court`
2. Passer `project-governance/checklists/release-checklist.md` item par item et afficher le résultat avec verdict PASS / FAIL — **ARTEFACT obligatoire**
3. Afficher le template PR rempli dans la conversation (titre, corps, checklist)

L'utilisateur ne voit le template PR qu'après avoir vu la release checklist. Il n'y a pas d'étape 6 séparée.

**REFUS si** : le push est effectué sans que la release checklist soit produite dans la même réponse.

**REFUS si** : une nouvelle subfeature démarre alors que la release checklist de la subfeature précédente n'a pas été passée dans cette conversation.

### Étape 6 — Mise à jour documentation post-merge (ARTEFACT : PRODUCT_SPEC.md à jour)

Dès que l'utilisateur confirme le merge ("mergé", "PR mergée", ou équivalent) :

1. Mettre à jour le statut de la feature parente dans `docs/PRODUCT_SPEC.md` si toutes ses subfeatures sont Done
2. Ajouter une ligne dans l'historique des évolutions de `docs/PRODUCT_SPEC.md`
3. Si une nouvelle table a été créée : vérifier et mettre à jour `docs/ARCHITECTURE_CANONIQUE.md`
4. Commiter ces mises à jour directement sur master

**REFUS si** : la feature parente est complète et PRODUCT_SPEC.md n'a pas été mis à jour avant de démarrer la feature suivante.

---

### Étape 7 — Sync backlog DB (automatique post-merge — F-178)

Toute modification de `docs/PRODUCT_SPEC.md` ou `docs/MARKETING_BACKLOG.md` doit aboutir à une synchronisation des tables `backlog_features`, `backlog_subfeatures`, `backlog_marketing_tasks` consommées par l'écran super-admin `/super-admin/backlog` (F-178).

**Mode normal — automatique** : tâche `@Scheduled` cron 5 min qui parse les 2 fichiers et upsert les tables. Audit dans `backlog_sync_runs` (timestamp, durée, count, success/error). **Aucun artefact obligatoire côté contributeur** — le merge sur master suffit.

**Mode resync manuelle — opérationnel** : si une modification doit être visible immédiatement (démo, présentation, debug), cliquer **"Resync now"** dans l'écran `/super-admin/backlog` (super-admin only). L'écran affiche un indicateur de fraîcheur ("Synchronisé il y a X minutes") visible en haut.

**Si la sync échoue répétitivement** (visible dans `backlog_sync_runs.success = false` sur plusieurs runs consécutifs) : ouvrir un ticket — ne pas éditer la DB à la main (les MD restent la source de vérité, la DB sera ré-écrasée au prochain cron réussi).

**REFUS si** : édition directe des tables `backlog_*` sans passer par l'édition du fichier MD source. Les MD sont la source de vérité (Option A retenue F-178), la DB est un cache de lecture.

---

## Blocages automatiques

41 situations qui déclenchent un refus immédiat — voir `docs/governance/automatic-blockers.md` pour le tableau complet avec motivations et cas historiques.

**Format de refus standard :**
```
REFUS [contexte]
Motif : [raison précise]
Artefact manquant : [ce qui doit être produit]
Référence : [fichier de gouvernance concerné]
```

**Index par thème** (voir `docs/governance/automatic-blockers.md` pour le détail) :
- **Mini-spec incomplète** : 2, 3, 4, 5, 6, 24, 27, 28
- **Découpage / process** : 1, 7, 8, 11, 26
- **Checklists** : 9, 10, 12
- **Architecture / cohérence transversale** : 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 39, 40
- **Référentiels métier** : 29, 30, 31
- **Composants décisionnels frontend** : 32, 33, 34, 35, 36, 37, 38, 41
- **Tests E2E** : 25

---

## Détection des demandes multi-features

Avant tout traitement, analyser si la demande brute couvre une seule feature ou plusieurs.

Une demande doit être considérée comme **potentiellement multi-features** si elle contient :
- plusieurs comportements visibles distincts et indépendants
- plusieurs responsabilités métier séparables
- plusieurs écrans ou endpoints indépendants qui ne partagent pas de flux unique
- plusieurs entités principales impactées de façon indépendante

**Règle :** Une demande multi-features ne doit jamais être traitée comme une feature unique sans arbitrage préalable.

**Action requise si multi-features détectée :**
```
REFUS [contexte]
Motif : La demande couvre plusieurs features distinctes.
Features identifiées : [liste des features détectées]
Action requise : Séparer en features indépendantes et traiter chacune séparément.
Référence : CLAUDE.md — Détection des demandes multi-features
```

---

## Préoccupations transversales — règle anti-régression

Certaines modifications impactent silencieusement des composants existants qui n'ont pas été touchés.
Ces **préoccupations transversales** doivent être traitées explicitement à chaque subfeature.

### Déclencheurs obligatoires

| Préoccupation | Exemples concrets | Action requise |
|--------------|------------------|----------------|
| **Auth / Principal** | Nouveau type d'auth, modification du Principal, changement de session | Lister tous les `@AuthenticationPrincipal` existants. Vérifier que chacun supporte le nouveau type. Ajouter test de non-régression. |
| **Workspace context** | Nouveau moyen de résoudre le workspace, changement de `workspace_id` | Lister tous les composants qui résolvent le workspace. Vérifier leur comportement. |
| **Plans / limites** | Nouveau plan, changement de quota, nouveau gate | Lister tous les appels à `PlanLimitService`. Vérifier les gates. |
| **Navigation / routing** | Nouvelle route, guard modifié, redirection ajoutée | Vérifier tous les chemins de navigation existants. Lancer les smoke tests. |
| **Outil décisionnel métier** | Création, modification ou observation concernant un outil décisionnel (calculator / analyzer / generator / decision engine côté backend ; section composant côté frontend). Inclut tout ajout backlog, toute SF qui touche un outil existant, toute observation de bug qui en mentionne un. | **Lister tous les outils décisionnels** (F-DT-07/08/09/10, F-IM-05/06/07, F-FA-05/06/07, etc.). **Scanner chacun** pour vérifier s'il contient un switch conditionnel sur un type métier, un pays ou un mode qui mélange plusieurs situations distinctes. **Classer** chaque outil : déjà séparé / multi-situations à scinder / paramétrage simple. **Appliquer l'invariant** : un outil décisionnel = une situation métier (pattern F-DT-08/F-DT-10). Si un autre outil présente le même pattern que celui à l'origine de la demande, l'inclure dans le périmètre ou ouvrir une feature jumelle au backlog. |

### Règle de blocage automatique

Si une subfeature coche une préoccupation transversale dans sa mini-spec **sans liste de composants impactés** → BLOCAGE.
Si les smoke tests E2E échouent après l'implémentation → BLOCAGE avant push.

### Suite de smoke tests E2E

Les tests de non-régression automatiques sont dans `e2e/smoke/`.
Lancer avant tout push touchant une préoccupation transversale :

```bash
cd e2e && npm test
```

Les smoke tests couvrent les chemins critiques d'intégration :
- `auth.spec.ts` — login local, login OAuth, logout, redirect non-authentifié
- `workspace.spec.ts` — switch workspace → rechargement des dossiers
- `navigation.spec.ts` — invitation → /login, guards, redirections

---

## Sujets non tranchés

- Toute décision touchant à `docs/OPEN_QUESTIONS.md` doit être explicitement posée avant implémentation
- Ne jamais implémenter silencieusement une solution à un sujet ouvert

## Features — règle d'existence

- Toute feature implémentée doit être référencée dans `docs/PRODUCT_SPEC.md`
- Toute nouvelle feature doit être ajoutée à `docs/PRODUCT_SPEC.md` et validée avant tout dev
- Le statut de chaque feature dans `docs/PRODUCT_SPEC.md` doit être maintenu à jour

---

## Quand tu proposes une modification

1. Rappeler la décision actuelle (architecture ou process)
2. Expliquer la variante proposée et son impact
3. Ne jamais remplacer silencieusement une décision existante
4. Si la modification touche un sujet ouvert, le signaler

---

## Agents et skills disponibles

### Agents
- `ai-agents/orchestrator/delivery-orchestrator.md` — point d'entrée de tout dev
- `ai-agents/backend/backend-agent.md` — implémentation Spring Boot
- `ai-agents/frontend/frontend-agent.md` — implémentation Angular
- `ai-agents/qa/qa-agent.md` — validation qualité
- `ai-agents/review/review-agent.md` — review de code
- `ai-agents/docs/docs-agent.md` — cohérence documentaire

### Skills
- `ai-skills/feature-splitter.md` — découper une feature en subfeatures
- `ai-skills/story-writer.md` — rédiger une mini-spec
- `ai-skills/test-case-generator.md` — générer un plan de test
- `ai-skills/review-checklist-runner.md` — évaluer une PR
- `ai-skills/definition-of-done-checker.md` — valider la complétude
- `ai-skills/feature-autonome.md` — livrer une feature complète **toutes SF d'affilée** sans pause entre SF (à invoquer quand l'utilisateur demande "implémente F-XXX en autonome" / "vas-y jusqu'au bout" / équivalent)

---

## Commandes de développement et de déploiement

Voir :
- `docs/DEVELOPMENT.md` — démarrer backend (H2 / PostgreSQL), frontend, builder
- `docs/DEPLOYMENT.md` — déploiement staging / production via CI/CD, kubectl, monitoring

---

## Tâches marketing — règles de gouvernance

### Règle 1 — Complétion

Toute tâche du `docs/MARKETING_BACKLOG.md` suit cette règle :

**Une tâche marketing n'est marquée `Terminé` que si elle est entièrement opérationnelle en production.**

- Un email rédigé mais non branché dans le code → statut `Rédigé`, pas `Terminé`
- Une page web rédigée mais non déployée → statut `Rédigé`, pas `Terminé`
- Un document produit mais non publié/transmis → statut `Rédigé`, pas `Terminé`

Quand une tâche marketing implique du code (email automatique, tracking, intégration), elle doit passer par la séquence de dev standard (mini-spec → dev → review → push) avant d'être marquée `Terminé`.

**REFUS si** : une tâche marketing est marquée `Terminé` sans que le code correspondant soit implémenté et déployé.

### Règle 2 — Contrôle de cohérence avant tout ajout au backlog marketing

Avant d'ajouter une nouvelle tâche dans `docs/MARKETING_BACKLOG.md`, exécuter et **afficher dans la conversation** le contrôle suivant en 4 points :

1. **Cohérence budgétaire** — la tâche entre-t-elle dans l'enveloppe marketing en vigueur (cadrage actif `docs/marketing/m71-budget-cadrage-2026h2.md` ou successeur) ? Si l'enveloppe doit être dépassée, l'ajout est conditionné à une décision d'arbitrage budgétaire explicite, et la tâche prérequise correspondante (cadrage / révision d'enveloppe) doit être créée d'abord.
2. **Doublon avec une feature produit** — la capacité technique demandée n'est-elle pas déjà couverte par une feature livrée du `PRODUCT_SPEC.md` ? Exemples : ne pas demander Google Analytics si F-77 est `Terminée`, ne pas demander un tracking conversion Google Ads si F-119 est `Terminée`.
3. **Doublon backlog (overlap > 30 %)** — scanner les sections M-XX existantes par thème (Site, Vidéo, Email, LinkedIn, Vente, Belgique, Stratégie acquisition, Mesure, Cadrage stratégique) pour vérifier qu'aucune tâche existante ne couvre déjà l'intention. Si oui, étendre la tâche existante plutôt qu'en créer une nouvelle (cf. règle "feedback_backlog_overlap_analysis").
4. **Séquence stratégique** — la tâche met-elle la charrue avant les bœufs (par ex. engager un stand événementiel à 5 k € avant d'avoir tranché l'enveloppe globale et l'arbitrage entre canaux SEO/SEA/SDR/événements) ? Si oui, créer d'abord la tâche prérequise (cadrage budget, validation traction, etc.).

Le contrôle doit produire un tableau visible dans la réponse (tâche proposée → verdict 4 points → action). Les tâches qui sortent du contrôle peuvent être marquées `Bloqué` si elles dépendent d'un arbitrage non encore tranché.

**REFUS si** : une nouvelle tâche est ajoutée à `MARKETING_BACKLOG.md` sans que ce contrôle ait été affiché dans la conversation.

**REFUS si** : une tâche événementielle ou un canal d'acquisition payant > 1 000 € est ajoutée au backlog alors que le cadrage budget marketing en vigueur (M-71 ou successeur) n'a pas tranché l'enveloppe correspondante.

---

## Priorité

```
Cohérence architecture > nouveauté
Process > vitesse
Testabilité > complétude
Refuser explicitement > laisser passer silencieusement
```
