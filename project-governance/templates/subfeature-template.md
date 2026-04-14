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
