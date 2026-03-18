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

## Séquence obligatoire par subfeature

Ce cycle est non négociable. Chaque étape produit un artefact visible dans la conversation.
**Sans l'artefact de l'étape N, l'étape N+1 est refusée.**

```
[1] Mini-spec → [2] Readiness → [3] Dev → [4] Review → [5] Push → [6] Merge
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

---

### Étape 4 — Review checklist (ARTEFACT : checklist passée item par item)

Avant tout `git push`, lire `project-governance/checklists/review-checklist.md` et afficher le résultat dans la conversation avec un verdict PASS / FAIL explicite et les items bloquants identifiés.

Les items bloquants doivent être corrigés avant le push. Un item non bloquant peut être poussé avec une note explicite.

**REFUS si** : `git push` est exécuté sans que la review checklist ait été passée et affichée dans cette conversation.

---

### Étape 5 — Push et PR

Après review PASS :
- `git push -u origin feat/SF-XX-YY-nom-court`
- Afficher le template PR rempli dans la conversation (titre, corps, checklist)

---

### Étape 6 — Release checklist (ARTEFACT : checklist passée avant merge)

Avant de merger, passer `project-governance/checklists/release-checklist.md` et afficher le résultat.

**REFUS si** : le merge est effectué sans que la release checklist ait été passée dans cette conversation.

---

## Blocages automatiques

Ces situations déclenchent un refus immédiat. Répondre avec le format de refus standard.

| Situation | Réponse |
|-----------|---------|
| Demande brute couvrant plusieurs features distinctes | REFUS — séparer les features avant tout découpage |
| Demande de code sans mini-spec produite dans la conversation | REFUS — produire la mini-spec d'abord (`subfeature-template.md`) |
| Demande de code sans critères d'acceptation dans la mini-spec | REFUS — compléter la mini-spec |
| Demande de code sans plan de test dans la mini-spec | REFUS — compléter la mini-spec |
| Feature non découpée en subfeatures | REFUS — demander le découpage (`feature-splitter`) |
| Subfeature estimée > 2 jours | REFUS — demander un redécoupage |
| `git push` sans review checklist passée dans la conversation | REFUS — passer la review checklist d'abord |
| Merge sans release checklist passée dans la conversation | REFUS — passer la release checklist d'abord |
| Question ouverte non tranchée et bloquante | BLOCAGE — signaler, ne pas avancer |
| Incohérence avec `ARCHITECTURE_CANONIQUE.md` | BLOCAGE — signaler la divergence |
| Feature non référencée dans `PRODUCT_SPEC.md` | REFUS — ajouter la feature au PRODUCT_SPEC avant tout dev |
| Traitement IA demandé de façon synchrone | REFUS — rappeler la règle async |
| Accès données sans filtre `workspace_id` | REFUS — rappeler la règle d'isolation |
| Composant frontend utilisant couleurs/polices hors `DESIGN_SYSTEM.md` | BLOCAGE — signaler la divergence |
| Ecran produit sans header/layout conforme au design system | BLOCAGE — signaler la divergence |
| Feature avec écran utilisateur marquée `Terminée` sans composant Angular implémenté | REFUS — implémenter les écrans manquants avant de marquer Terminée |
| Subfeature backend mergée sans subfeature frontend planifiée (si la feature a une UI) | BLOCAGE — planifier et créer la subfeature frontend correspondante avant de continuer |

**Format de refus standard :**
```
REFUS [contexte]
Motif : [raison précise]
Artefact manquant : [ce qui doit être produit]
Référence : [fichier de gouvernance concerné]
```

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

---

## Commandes de développement

### Démarrer le backend

**Profil `dev` (H2 en mémoire — pas besoin de Docker)**
```bash
source .env.local
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
- Port : 8080 | Base : H2 en mémoire (données perdues à chaque redémarrage)
- Console H2 : http://localhost:8080/h2-console

**Profil `local` (PostgreSQL + MinIO via docker compose)**
```bash
source .env.local
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```
- Port : 8080 | Base : PostgreSQL (données persistantes)
- Requiert : `docker compose up -d`

### Démarrer le frontend
```bash
source ~/.nvm/nvm.sh && nvm use 22
cd frontend && npm start
```
- Port : 4200
- Node 22 requis (géré via nvm)

### Démarrer PostgreSQL (prod locale)
```bash
docker compose up -d
```
- Port : 5432
- DB : `legalcasedb` / User : `legalcase` / Password : `legalcase`

### Accès base de données H2 (dev uniquement)
- URL : http://localhost:8080/h2-console
- JDBC URL : `jdbc:h2:mem:legalcasedb`
- Utilisateur : `sa` / Mot de passe : (vide)

### Builder le backend sans tests
```bash
cd backend && ./mvnw clean package -DskipTests
```

### Builder le frontend
```bash
source ~/.nvm/nvm.sh && nvm use 22
cd frontend && npm run build
```

---

## Priorité

```
Cohérence architecture > nouveauté
Process > vitesse
Testabilité > complétude
Refuser explicitement > laisser passer silencieusement
```
