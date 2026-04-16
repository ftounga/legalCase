# Mini-spec — [FEAT-XX / SF-YY] Titre de la subfeature

> Template : copier ce fichier, renommer en `SF-XX-YY-nom.md`, placer dans `docs/features/FEAT-XX/`
> Ce document doit être validé AVANT de démarrer le dev.

---

## Identifiant

`FEAT-XX / SF-YY`

## Feature parente

`FEAT-XX` — [titre de la feature parente]

## Statut

`draft` | `ready` | `in-progress` | `in-review` | `done` | `blocked`

## Date de création

YYYY-MM-DD

## Branche Git

`feat/SF-XX-YY-nom-court`

---

## Objectif

> En une phrase : que fait cette subfeature ?

[À compléter]

---

## Comportement attendu

### Cas nominal

> Description précise du flux principal (entrée → traitement → sortie).

[À compléter]

### Cas d'erreur

> Lister tous les cas d'erreur identifiés.

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| [Champ obligatoire absent] | Message d'erreur explicite | 400 |
| [Ressource inexistante] | [description] | 404 |
| [Workspace différent] | Accès refusé | 403 |
| [...] | [...] | [...] |

---

## Analyse de cohérence transversale

> Avant d'écrire les critères d'acceptation, scanner les cibles où le même mécanisme pourrait s'appliquer.
> Objectif : éviter d'implémenter un mécanisme sur un seul outil / pays / domaine et devoir le redupliquer plus tard.

### Périmètres à scanner

- [ ] **Autres outils métier** : F-DT-07 Ancienneté, F-DT-08 Validité licenciement, F-DT-09 Comparateur indemnités, F-DT-10 Validité rupture conventionnelle, F-FA-05 Partage immobilier, F-FA-06 Calendrier garde, F-FA-07 Checklist divorce, F-IM-05 Titre séjour, F-IM-06 Recours, F-IM-07 Droit au travail
- [ ] **Autres pays** : France / Belgique (si le mécanisme dépend d'un pays)
- [ ] **Autres domaines** : DROIT_DU_TRAVAIL / DROIT_FAMILLE / DROIT_IMMIGRATION
- [ ] **Autres UI patterns** : formulaires réactifs, dialogues de confirmation, exports PDF, alertes de cohérence F-IA-03, refresh dashboard F-IA-02, pré-remplissage IA, masquage conditionnel
- [ ] **Autres flows transversaux** : auth / workspace context / plans / navigation

### Niveaux de vérification à couvrir

Pour chaque cible applicable, ne pas se limiter à la surface visible. Descendre la chaîne autant que nécessaire selon le mécanisme :

- [ ] **Modèle TypeScript / API exposée** (surface frontend)
- [ ] **Record / DTO backend** (structure de réponse)
- [ ] **Service / logique métier** (quelles données passent réellement)
- [ ] **Entité JPA + schéma DB** (ce qui est persisté effectivement, via colonnes dédiées ou JSON)
- [ ] **Tests existants** (quelle partie est déjà couverte et comment)

> Exemple concret : un fix "pré-remplissage après reload" qui ne vérifie que la présence des champs dans la Response est incomplet — il faut aussi contrôler que les champs sont bien persistés (colonne dédiée ou JSON dans `result_data`). Un outil peut exposer un champ dans la Response tout en ne le stockant pas, et le bug réapparaît au reload.

### Cas spécifique : nouvelle feature d'outil décisionnel

Si la subfeature crée ou modifie un outil décisionnel (avec UI, formulaire, résultat persisté par dossier), répondre explicitement aux questions suivantes :

- [ ] **Cohérence IA (F-IA-03)** : l'outil présente-t-il des champs ou des critères susceptibles d'être croisés avec l'analyse IA, les réponses aux questions IA ou la checklist procédurale F-96 ? Si oui, intégrer la couverture F-IA-03 dans cette subfeature ou créer une subfeature jumelle SF-IA-03-XX.
- [ ] **Refresh dashboard (F-IA-02)** : l'outil déclenche-t-il une action validée qui impacte les cards du tableau de bord ? Si oui, injecter `CaseDashboardRefreshService` et appeler `triggerRefresh()` dans `next:` (pattern SF-IA-02-03).
- [ ] **Pré-remplissage IA** : le formulaire peut-il être pré-rempli depuis la synthèse IA ? Si oui, `prefillFromAi()` + provenance notes (pattern F-DT-09 / F-IM-05).
- [ ] **Persistance des inputs** : tous les champs saisis sont-ils persistés en base (colonnes dédiées ou JSON `result_data`) pour survivre au reload ? (pattern SF-DT-07-04).
- [ ] **Masquage conditionnel selon type** : l'outil ne s'applique qu'à certains types de dossier (rupture, domaine, pays) ? Si oui, orchestration dans `case-file-detail` avec `@if` sur un `computed` signal.
- [ ] **Alertes actives après calcul** : le gate du `coherenceAlerts` computed n'inclut-il pas `|| this.result()` (bug SF-IA-03-12) ? Seul `!this.showForm()` doit gater.

> Ces questions reflètent les écueils récurrents observés. Cocher chaque case explicitement force la vérification. Si l'équipe découvre un nouveau pattern récurrent, l'ajouter ici.

### Cas spécifique : nouveau pattern UI ou service partagé

Si la subfeature introduit un composant partagé (`shared/`), un service applicatif (`core/services/`), un endpoint transversal, une directive, ou un record/DTO réutilisable, répondre explicitement aux questions suivantes :

- [ ] **Où le nouveau pattern UI pourrait-il être réutilisé ?** Scanner au-delà de la cible directe : badges inline, tooltips, popovers, panneaux, indicateurs de statut, barres de progression, cartes info, etc. existants dans l'app.
- [ ] **Y a-t-il des patterns concurrents (tooltips maison, badges maison, popovers ad hoc) que ce nouveau pattern remplace ?** Si oui, lister et classer (harmonisation immédiate / backlog).
- [ ] **Le nouveau service / endpoint peut-il servir à d'autres features ?** Scanner F-69, F-92, F-93, F-94, F-96 et les autres zones porteuses d'info IA/métadonnées.
- [ ] **Le nouveau composant a-t-il un équivalent design que ce design remplace ?** Si oui, planifier la migration progressive et éviter la coexistence durable.

> Un pattern introduit sans ce scan crée une *dette de convergence* : deux mécanismes similaires qui divergent dans le temps. Ce scan oblige à classer chaque cible concurrente dès la naissance du pattern, même si le traitement est différé en backlog.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| [Cible 1] | [Oui / Non] | [Intégré dans cette SF / SF parallèle SF-XX-YY / backlog / Non applicable — raison] |
| [...] | | |

### Décision

- [ ] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) créée(s) pour les cibles restantes : [liste]
- [ ] Backlog VN (référence ligne PRODUCT_SPEC.md) pour les cibles non prioritaires : [liste + raison]
- [ ] Non applicable aux autres cibles (justification explicite)

> Si aucune case n'est cochée → la subfeature n'est pas `ready` (cf. readiness-checklist.md).

---

## Critères d'acceptation

> Chaque critère est vérifiable. Pas d'ambiguïté.
> Ces critères sont reviewés dans la PR.

- [ ] [Critère 1 : nominal — description précise]
- [ ] [Critère 2 : nominal — description précise]
- [ ] [Critère 3 : cas d'erreur — description précise]
- [ ] [Critère 4 : sécurité — isolation workspace vérifiée]
- [ ] [...]

---

## Périmètre

### Hors scope (explicite)

- [Ce qui n'est pas fait dans cette subfeature]
- [...]

---

## Valeurs initiales
> À remplir si la subfeature crée une entité ou modifie l'état initial d'une ressource.

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| status | [ex: DRAFT] | [imposée par le métier / toujours à cette valeur à la création] |
| [flag] | [true / false] | [description de la règle] |
| [champ] | [valeur] | [description] |

Comportements à la création :
- [Ex : created_at est renseigné automatiquement par la base]
- [Ex : created_by_user_id = utilisateur connecté]
- [Ex : workspace_id = workspace du contexte de sécurité]

---

## Contraintes de validation

> À remplir pour tout champ soumis à une règle de format, présence, taille ou unicité.
> Ces contraintes sont implémentées dans le service et testées explicitement.

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| [champ1] | Oui / Non | [ex: 255] | [ex: non vide, sans HTML] | Non | [ex: trim()] |
| [champ2] | Oui / Non | — | [ex: EMPLOYMENT_LAW, IMMIGRATION_LAW] | Non | — |
| [champ3] | Non | [ex: 2000] | [texte libre] | Non | — |

Notes :
- [Ex : legal_domain en V1 n'accepte que EMPLOYMENT_LAW]
- [Ex : le titre ne peut pas être une chaîne vide après trim()]
- [Ex : description est optionnelle mais limitée à 2000 caractères si fournie]

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/[resource]` | Oui | LAWYER |
| GET | `/api/v1/[resource]/{id}` | Oui | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| [table] | INSERT / SELECT / UPDATE | [précision] |

### Migration Liquibase

- [ ] Oui — `V{version}__[description].sql`
- [ ] Non applicable

### Composants Angular (si applicable)

- [ComponentName] — [description de ce qu'il fait]

---

## Plan de test

### Tests unitaires

- [ ] [Service] — cas nominal : [description]
- [ ] [Service] — cas d'erreur : [description]
- [ ] [...]

### Tests d'intégration

- [ ] `POST /api/v1/[resource]` → 201 avec payload valide
- [ ] `POST /api/v1/[resource]` → 400 avec champ manquant
- [ ] `GET /api/v1/[resource]/{id}` → 403 si workspace différent
- [ ] [...]

### Isolation workspace

- [ ] Applicable — test : un utilisateur du workspace A ne peut pas accéder aux données du workspace B
- [ ] Non applicable — raison : [...]

---

## Analyse d'impact

### Préoccupations transversales touchées

> Cocher chaque préoccupation que cette subfeature modifie ou étend.
> Si au moins une case est cochée → remplir obligatoirement le tableau ci-dessous.

- [ ] **Auth / Principal** — touche `@AuthenticationPrincipal`, le contexte de sécurité, ou l'identité de l'utilisateur
- [ ] **Workspace context** — touche la résolution du workspace courant, `workspace_id`, ou les membres
- [ ] **Plans / limites** — touche `PlanLimitService`, les gates, ou les quotas
- [ ] **Navigation / routing frontend** — touche les routes Angular, les guards, ou les redirections
- [ ] **Aucune préoccupation transversale** — subfeature isolée, impact limité à son périmètre

### Composants / endpoints existants potentiellement impactés

> À remplir si au moins une case est cochée ci-dessus.
> Lister explicitement ce qui pourrait casser chez les consommateurs existants.

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| [ex: WorkspaceController] | [ex: utilise OidcUser — à vérifier avec le nouveau type d'auth] | [ex: IT avec le nouveau provider] |
| [ex: CaseFilesListComponent] | [ex: réagit au contexte workspace — à vérifier après changement] | [ex: smoke test workspace switch] |

### Smoke tests E2E concernés

> Lister les smoke tests de `e2e/smoke/` qui doivent passer **sans régression** après cette subfeature.
> Si un smoke test échoue après merge → la subfeature est bloquante.

- [ ] `e2e/smoke/[fichier]` — `[nom du test]` — [raison]
- [ ] Aucun smoke test concerné (justification : [raison])

---

## Dépendances

### Subfeatures bloquantes

- [SF-XX-YY] — statut : [done / in-progress]
- [...]

### Questions ouvertes impactées

- [ ] [Question de `docs/OPEN_QUESTIONS.md`] — tranchée le YYYY-MM-DD / non encore tranchée
- [ ] [...]

---

## Notes et décisions

> Décisions techniques prises lors de la spécification. À compléter au fil du dev si nécessaire.

[À compléter]
