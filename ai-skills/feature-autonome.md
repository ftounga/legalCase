# Skill — feature-autonome

**Quand l'invoquer** : l'utilisateur demande d'implémenter une feature complète **en autonomie sans pause**, p. ex. :
- *"Implémente F-XXX totalement en autonome. Si tu vois des points d'arbitrage, fais au mieux"*
- *"Pars du principe que je suis tes recos"*
- *"Vas-y jusqu'au bout"*
- *"Toutes les SF d'affilée"*

## Règle d'or

> **Une feature ≠ une SF.** "Implémenter la feature en autonome" = livrer **toutes les SF** de la feature jusqu'au déploiement staging, **sans demander de confirmation entre chaque SF**, et présenter un récap unique à la toute fin.

## Procédure obligatoire

### Étape 0 — Vérifier la définition de la feature avant de partir
1. Localiser la feature parente dans `docs/PRODUCT_SPEC.md`.
2. Lister explicitement **toutes les SF** prévues (ex. F-185 = SF-185-01 + SF-185-02 + SF-185-03).
3. Annoncer à l'utilisateur dans le 1ᵉʳ message : *"F-XXX = N SF prévues. J'enchaîne SF-XXX-01 → SF-XXX-N + déploiement staging + docs post-merge sans pause."* — c'est un engagement explicite, pas une option.

### Étape 1 — Recalibrer les estimés AVANT de commencer chaque SF
Les mini-specs donnent des bornes hautes ("2-3 j", "5-7 j") **calibrées sur un humain qui découvre le code**. **Avant chaque SF, lire le code des fichiers cibles** (5-15 min) pour estimer le **vrai** effort. Cas courants :
- L'infrastructure existe déjà (ex. SF-185-02 : Q&A async existait déjà via RabbitMQ depuis le départ ; il manquait juste l'event SSE → 30 min vs 2-3 j estimés).
- Un pattern parent peut être réutilisé tel quel (ex. SF-185-03 : pattern `provisional` flag miroir d'`analysisType STANDARD/ENRICHED` → quelques colonnes + 1 helper).
- Le code à modifier est sur les rails de SF déjà livrées (réutilise migration, entité, SSE, etc).

**Si l'effort estimé après lecture du code > 4h**, signaler à l'utilisateur **avant de coder** ("Cette SF est plus grosse que prévu : X. Je reste en autonome ou tu veux qu'on s'arrête ?") — mais sinon, ne pas demander.

### Étape 2 — Pour chaque SF, dérouler la séquence CLAUDE.md sans pause
Chaque SF traverse :
1. **Mini-spec** dans `docs/features/F-XXX/SF-XXX-YY-nom.md`
2. **Branche** `feat/SF-XXX-YY-nom` depuis master à jour
3. **Code** + tests unitaires
4. **Compile + tests verts** (`mvn compile` + tests cibles + `npm run build`)
5. **Commit** avec message explicite (Co-Authored-By: Claude)
6. **Push + PR** via `gh pr create`
7. **Merge** via `gh pr merge --squash --delete-branch --admin`
8. **Sync local** : checkout d'une nouvelle branche depuis `origin/master` (le worktree principal est verrouillé)
9. **Passer à la SF suivante** sans demander

### Étape 3 — Hotfix CI éventuel = nouvelle SF dans le même flow
Si la CI échoue après merge (ex. test pré-existant qui dépendait d'un mock non couvert) :
- Créer une branche `fix/SF-XXX-YY-name`, fixer, merger via la même séquence.
- **Ne pas s'interrompre** pour signaler — c'est un coût de la livraison, pas une mauvaise nouvelle.

### Étape 4 — Docs post-merge groupé après la dernière SF
Au lieu de pousser 1 docs PR par SF, attendre la fin de **toutes** les SF puis pousser **1 seul** `docs/F-XXX-complete-YYYY-MM-DD` qui :
- Marque la feature parente Terminée dans `PRODUCT_SPEC.md` si toutes les SF sont done
- Ajoute 1 entrée par SF dans l'historique (pas 1 entrée groupée — chaque SF mérite sa traçabilité)
- Mentionne les arbitrages techniques pris pour chaque SF

### Étape 5 — Déploiement staging final
- Backend : `gh workflow run backend.yml --ref master`
- Frontend : se déclenche automatiquement au push master
- Health check final : `curl https://staging.legalcase.ng-itconsulting.com/api/actuator/health`
- Attendre les 2 deploys via `Bash run_in_background`, **ne pas demander entre temps "tu veux que je continue ?"**

### Étape 6 — Récap unique à la fin
**À la TOUTE fin** (toutes les SF mergées + déployées staging + docs à jour), produire UN récap qui contient :
- Les N PR mergées + commits
- L'état staging (URLs, runs CI verts)
- La liste des **arbitrages techniques** pris (pour transparence — c'est ce que la consigne autorise)
- Les risques résiduels à surveiller (ex. "auto-trigger consomme tokens à chaque DocumentAnalysis — monitor")
- Les SF de feature suivante éventuellement débloquées

## Anti-patterns interdits

| ❌ Interdit | ✅ Faire |
|------------|---------|
| "SF-XXX-01 livrée. Tu veux que je continue sur SF-XXX-02 ?" | Enchaîner directement sans demander |
| "L'estimé spec dit 5-7 j, c'est trop pour cette session" | Lire le code, ré-estimer (souvent 5-10× moins), continuer |
| Récap intermédiaire après chaque SF | Récap unique à la TOUTE fin |
| "Je m'arrête pour valider sur staging avant SF suivante" | Continuer ; staging final = validation utilisateur |
| Docs post-merge en N PR (1 par SF) | Docs post-merge groupé en 1 PR |
| Nouvelle SF démarre sans avoir mergé la précédente | Toujours merger avant de passer à la suivante (sinon la séquence du CLAUDE.md est cassée) |

## Exception légitime au mode autonome

**Stopper et demander** uniquement si :
- Une SF nécessite une **décision produit** non écrite dans la mini-spec ni dans `PRODUCT_SPEC.md` (ex. "le bandeau doit être or ou rouge ?" — répondre "je prends or, dis-moi si tu préfères rouge dans le récap final" — pas de pause)
- Une SF révèle un **conflit avec une feature en cours** non anticipé (ex. autre session édite les mêmes fichiers)
- Une SF révèle un **risque sécurité critique** non documenté
- Le **build casse de façon non triviale** et la résolution demande > 30 min de debug → signaler avant de partir loin

Dans tous les autres cas, continuer.

## Anti-cas concret 2026-05-03

Lors de F-185, j'ai stoppé après SF-185-01 et demandé "go SF-185-02 ?" malgré la consigne explicite d'autonomie. **3 erreurs cumulées** :
1. Lecture trop respectueuse des estimés mini-spec ("2-3 j pour SF-02") sans lire le code (j'aurais vu que c'était 30 min).
2. Réflexe défensif "résumer puis demander confirmation" hérité d'autres patterns de collaboration.
3. Mauvaise calibration du contexte restant — préfère "livrer propre 1 SF" que "tenter 3 SF avec risque", alors que la consigne autorisait explicitement le risque.

Cette skill existe pour empêcher ce pattern de récidiver.
