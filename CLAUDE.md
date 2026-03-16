# CLAUDE.md — Instructions projet AI LegalCase

## Documents à lire en priorité

Lire ces documents avant toute réponse impliquant du code, une spec ou une décision technique.

### Architecture
1. `docs/ARCHITECTURE_CANONIQUE.md` — source de vérité architecture (obligatoire)
2. `docs/OPEN_QUESTIONS.md` — registre des sujets non tranchés (obligatoire)

### Process
3. `project-governance/playbooks/feature-lifecycle.md` — cycle de vie des features
4. `project-governance/playbooks/definition-of-done.md` — critères de complétion
5. `project-governance/playbooks/coding-rules.md` — conventions de code
6. `project-governance/playbooks/review-rules.md` — critères de review
7. `project-governance/playbooks/testing-strategy.md` — stratégie de test

### Checklists (à appliquer à chaque étape)
8. `project-governance/checklists/readiness-checklist.md` — avant de démarrer le dev
9. `project-governance/checklists/review-checklist.md` — avant toute PR
10. `project-governance/checklists/release-checklist.md` — avant tout merge

### Optionnel
11. `docs/PRODUCT_SPEC.md` si présent

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

### Process de développement — cycle obligatoire

```
Feature → Subfeature → Mini-spec → Dev → Review → Test → Validation → Merge → Itération
```

Ce cycle est non négociable. Chaque étape a ses prérequis. Aucune étape ne peut être sautée.

| Étape | Prérequis obligatoires | Référence |
|-------|------------------------|-----------|
| Subfeature | Feature parente définie | `feature-lifecycle.md` |
| Mini-spec | Subfeature identifiée, périmètre délimité | `subfeature-template.md` |
| Dev | Mini-spec validée, critères d'acceptation définis, plan de test minimal présent | `readiness-checklist.md` |
| Review | QA passée, PR ouverte avec template rempli | `review-checklist.md` |
| Merge | Review approuvée, CI verte, DoD vérifiée | `release-checklist.md` |

### Sujets non tranchés
- Toute décision touchant à `docs/OPEN_QUESTIONS.md` doit être explicitement posée avant implémentation
- Ne jamais implémenter silencieusement une solution à un sujet ouvert

---

## Détection des demandes multi-features

Avant tout traitement, analyser si la demande brute couvre une seule feature ou plusieurs.

Une demande doit être considérée comme **potentiellement multi-features** si elle contient :
- plusieurs comportements visibles distincts et indépendants (ex : "upload ET consultation ET notification")
- plusieurs responsabilités métier séparables (ex : "validation des fichiers ET déclenchement de l'analyse IA")
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

Si la séparation est ambiguë → escalader au delivery-orchestrator avant de continuer.

---

## Blocages automatiques

Ces situations déclenchent un refus immédiat. Répondre avec le format de refus standard.

| Situation | Réponse |
|-----------|---------|
| Demande brute couvrant plusieurs features distinctes | REFUS — séparer les features avant tout découpage |
| Demande de code sans mini-spec | REFUS — demander la mini-spec (`subfeature-template.md`) |
| Demande de code sans critères d'acceptation | REFUS — demander les critères |
| Demande de code sans plan de test minimal | REFUS — demander le plan de test |
| Feature non découpée en subfeatures | REFUS — demander le découpage (`feature-splitter`) |
| Subfeature estimée > 2 jours | REFUS — demander un redécoupage |
| Question ouverte non tranchée et bloquante | BLOCAGE — signaler, ne pas avancer |
| Incohérence avec `ARCHITECTURE_CANONIQUE.md` | BLOCAGE — signaler la divergence |
| Traitement IA demandé de façon synchrone | REFUS — rappeler la règle async |
| Accès données sans filtre `workspace_id` | REFUS — rappeler la règle d'isolation |

**Format de refus standard :**
```
REFUS [contexte]
Motif : [raison précise]
Action requise : [ce qui doit être fourni ou corrigé]
Référence : [fichier de gouvernance concerné]
```

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
```bash
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
- Port : 8080
- Profil dev : H2 en mémoire, console H2 activée, Liquibase appliqué au démarrage

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
