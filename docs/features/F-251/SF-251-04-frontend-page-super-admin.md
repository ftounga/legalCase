# Mini-spec — F-251 / SF-251-04 — Page super-admin « Bootstrap prospect »

## Identifiant

`F-251 / SF-251-04`

## Feature parente

`F-251` — Fiabilisation de la période d'évaluation pour les comptes provisionnés en bypass IHM

## Statut

`ready`

## Date de création

2026-05-25

## Branche Git

`feat/SF-251-04-prospect-bootstrap-frontend`

---

## Objectif

Ajouter une page `/super-admin/prospect-bootstrap` avec un formulaire qui consomme l'endpoint `POST /api/v1/super-admin/prospect-bootstrap` (SF-251-03), permettant à l'opérateur de bootstrap un compte prospect en 1 clic au lieu d'un curl manuel — particulièrement utile en plein milieu d'une démo où chaque seconde compte.

---

## Comportement attendu

### Cas nominal

1. Super-admin connecté navigue vers `/super-admin/prospect-bootstrap` (lien dans la nav super-admin existante).
2. Formulaire affiche 7 champs : firstName, lastName, email, password (input texte visible — pas masqué, l'opérateur doit le dicter par téléphone), country (select FRANCE/BELGIQUE), legalDomain (select 3 valeurs), workspaceName.
3. Validation côté frontend : champs obligatoires, email format, password ≥ 8 caractères. Bouton « Bootstrap le compte » désactivé si formulaire invalide.
4. Sur clic bouton : `ProspectBootstrapService.bootstrap(request)` → `POST /api/v1/super-admin/prospect-bootstrap`.
5. Sur succès (201) : `MatSnackBar` succès « Compte bootstrappé — workspace [nom] (expire le [date]) ». Affiche un panneau résumé en bas du formulaire avec userId, workspaceId, expiresAt + bouton « Voir le workspace » (lien vers `/super-admin/workspaces` pré-filtré sur l'email). Bouton « Bootstrap un autre prospect » qui reset le formulaire.
6. Aucune redirection automatique — l'opérateur reste sur la page pour copier les infos dans son mail de bienvenue.

### Cas d'erreur

| Situation | Comportement |
|-----------|--------------|
| Champ obligatoire vide | Bouton désactivé + message inline `mat-error` |
| Email format invalide | `mat-error` « Format email invalide » |
| Password < 8 caractères | `mat-error` « Minimum 8 caractères » |
| 401 backend | Redirect vers login (intercepté par interceptor existant) |
| 403 backend (pas super-admin — ne devrait pas arriver si la route est gatée) | `MatSnackBar` erreur « Accès refusé — super-admin requis » |
| 409 backend (compte déjà actif) | `MatSnackBar` warning « Ce compte est déjà actif — bootstrap refusé. Voir [/super-admin/workspaces?email=...] » avec lien direct |
| 400 backend (validation backend) | `MatSnackBar` erreur affichant le `message` backend |
| Erreur réseau | `MatSnackBar` erreur « Erreur réseau — réessayer » |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : non applicable (page admin)
- [x] **Autres pays** : `country` sélectionnable, 2 valeurs
- [x] **Autres domaines** : `legalDomain` sélectionnable, 3 valeurs
- [x] **Autres UI patterns** : page super-admin → réutiliser conventions des pages existantes (`/super-admin/workspaces`, `/super-admin/dashboard-audit`, `/super-admin/promo-codes`, `/super-admin/backlog`, `/super-admin/traction-onepager`, `/super-admin/linkedin-queue`, `/super-admin/blog`) — Material design, layout cohérent
- [x] **Autres flows transversaux** : **Navigation / routing** scanné — nouvelle route lazy-loaded sous `/super-admin/` ; **Auth / Principal** — route gatée par `superAdminGuard` existant

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `/super-admin/workspaces` (page existante) | Oui | Pattern de référence pour layout + table optionnelle ; lien sortant depuis succès bootstrap |
| `/super-admin/promo-codes` (page existante) | Oui | Pattern de référence pour formulaire création (input + select + bouton submit + snackbar succès) |
| Nav super-admin (existante) | Oui | Ajout d'un lien « Bootstrap prospect » dans la liste des sections super-admin |
| Skill `prospect-account-bootstrap.md` | Oui | Mise à jour étape 4 dans SF-251-03 (le mode UI sera mentionné comme préféré, le curl reste pour cas exceptionnel) |

### Décision

- [x] Étendu : nouvelle page suit le pattern des 7 pages super-admin existantes (Material design, structure cohérente)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — page admin interne, pas un outil décisionnel, pas d'intégration `TOOL_REGISTRY`, pas de pré-fill IA, pas de cross-check F-IA-03.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — formulaire admin de provisionnement, données saisies manuellement par l'opérateur depuis la démo prospect.

---

## Critères d'acceptation

- [x] Nouvelle route `/super-admin/prospect-bootstrap` lazy-loaded, gatée par `superAdminGuard`.
- [x] Composant `ProspectBootstrapPageComponent` (standalone) avec formulaire réactif `FormGroup` 7 champs.
- [x] Service `ProspectBootstrapService` injectable avec méthode `bootstrap(request): Observable<ProspectBootstrapResponse>`.
- [x] DTOs TypeScript `ProspectBootstrapRequest`, `ProspectBootstrapResponse` strictement alignés sur le contrat API SF-251-03.
- [x] Validation Angular Reactive Forms (required, email, minLength) — bouton submit désactivé tant qu'invalide.
- [x] Snackbar succès affichant `workspaceName` + `expiresAt` formatée (jj/mm/aaaa).
- [x] Panneau résumé post-succès avec userId, workspaceId, expiresAt + 2 boutons (« Voir le workspace » lien vers `/super-admin/workspaces`, « Bootstrap un autre » reset).
- [x] Gestion erreurs 400 / 409 (panneau ou snackbar selon code) avec message backend remonté.
- [x] Lien « Bootstrap prospect » ajouté dans la nav super-admin (page existante hub `/super-admin`).
- [x] Tests Jest ≥ 6 cas (rendu initial, validation form, soumission OK, soumission 409, soumission 400, reset après succès).
- [x] `ng build` 0 erreur, `npm test` 100 % vert, pas de régression sur tests existants.

---

## Périmètre

### Hors scope (explicite)

- Génération automatique du mot de passe côté frontend (l'opérateur le choisit selon le pattern skill — mot français simple à dicter au téléphone).
- Historique des bootstraps réalisés (audit log) — pas nécessaire V1, le `/super-admin/workspaces` existant suffit pour retrouver les comptes.
- Envoi automatique du mail de bienvenue depuis le frontend (skill étape 6 reste manuelle).
- Modification d'un bootstrap existant (cas B : compte déjà actif) — refusé côté backend, le frontend ne propose pas d'override.
- Validation côté frontend du format `workspaceName` (uppercasing) — le backend normalise déjà.
- Internationalisation (i18n) — page super-admin française, pas exposée aux prospects.

---

## Valeurs initiales

| Champ | Valeur initiale formulaire | Règle |
|-------|---------------------------|-------|
| `firstName` | `''` | Champ obligatoire — saisie opérateur |
| `lastName` | `''` | idem |
| `email` | `''` | idem |
| `password` | `''` | idem (input visible, pas masqué) |
| `country` | `'FRANCE'` | Valeur par défaut — adaptable |
| `legalDomain` | `'DROIT_DU_TRAVAIL'` | Valeur par défaut (V1 majoritaire) |
| `workspaceName` | `''` | idem |

---

## Contraintes de validation

| Champ | Obligatoire | Validation Angular | Notes |
|-------|-------------|--------------------|-------|
| firstName | Oui | `Validators.required`, `maxLength(100)` | |
| lastName | Oui | `Validators.required`, `maxLength(100)` | |
| email | Oui | `Validators.required`, `Validators.email`, `maxLength(320)` | |
| password | Oui | `Validators.required`, `Validators.minLength(8)`, `maxLength(100)` | Input type=text (pas password) — opérateur dicte |
| country | Oui | `Validators.required` | select FRANCE/BELGIQUE |
| legalDomain | Oui | `Validators.required` | select 3 valeurs |
| workspaceName | Oui | `Validators.required`, `maxLength(100)` | |

---

## Technique

### Endpoint(s) consommé(s)

| Méthode | URL | Notes |
|---------|-----|-------|
| POST | `/api/v1/super-admin/prospect-bootstrap` | SF-251-03 |

### Tables impactées

Aucune (frontend).

### Migration Liquibase

- [x] Non applicable.

### Composants Angular

- `ProspectBootstrapPageComponent` (standalone, route `/super-admin/prospect-bootstrap`)
- `ProspectBootstrapService` (`core/services/` ou équivalent — selon pattern existant)
- Interfaces `ProspectBootstrapRequest` / `ProspectBootstrapResponse`
- Modif `super-admin.routes.ts` (ajout lazy route)
- Modif `super-admin-hub.component.html` (ajout lien dans la liste — le nom exact du composant hub à confirmer en dev, suivre pattern `/super-admin/promo-codes`)

---

## Plan de test

### Tests unitaires (Jest)

- [x] `ProspectBootstrapPageComponent.shouldRenderForm` — rendu initial avec 7 champs
- [x] `ProspectBootstrapPageComponent.shouldDisableSubmitWhenFormInvalid` — bouton désactivé tant que validations Angular non passées
- [x] `ProspectBootstrapPageComponent.shouldSubmitValidFormAndDisplaySuccess` — succès 201, snackbar + panneau résumé
- [x] `ProspectBootstrapPageComponent.shouldDisplay409ConflictWarning` — backend 409, message backend remonté
- [x] `ProspectBootstrapPageComponent.shouldDisplay400ValidationError` — backend 400, snackbar erreur
- [x] `ProspectBootstrapPageComponent.shouldResetFormAfterSuccess` — bouton « Bootstrap un autre » remet à l'état initial

### Tests d'intégration

- [x] Non applicable côté frontend (pas de Cypress E2E pour cette page — page admin interne, smoke E2E couvert par la chaîne complète backend IT).

### Isolation workspace

- [x] **Non applicable** — page super-admin cross-workspace.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing frontend** — nouvelle route lazy sous `/super-admin/`, vérifier que `superAdminGuard` existant la couvre
- [ ] Auth / Principal — aucun changement (consomme session existante)
- [ ] Workspace context — non applicable (cross-workspace)
- [ ] Plans / limites — non applicable

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `super-admin.routes.ts` | Ajout de route — vérifier pas de collision URL | Jest existant `super-admin.routes.spec.ts` (si présent) reste vert |
| `super-admin-hub.component` | Ajout d'un lien dans la liste — vérifier layout | Jest hub component reste vert |
| `superAdminGuard` | Réutilisé tel quel | Pas de changement |

### Smoke tests E2E concernés

- [x] **Aucun smoke test concerné** — page admin interne, accessible uniquement super-admin (non testée par les smoke E2E avocat).

---

## Dépendances

### Subfeatures bloquantes

- `SF-251-03` — backend endpoint avec contrat API figé. **Parallélisable** : frontend peut être développé sur la base du contrat figé dans la mini-spec SF-251-03, le test d'intégration final via dev-server local nécessite que SF-251-03 soit mergée.

### Subfeatures parallèles

- `SF-251-03` (backend) — contrat API stable, parallélisation OK.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Input password en `type=text` visible** : choix délibéré — l'opérateur doit pouvoir le dicter au téléphone au prospect. Masquer = friction inutile (pas de protection visuelle puisqu'il faut le partager).
- **Aucune redirection automatique post-succès** : l'opérateur a besoin de copier les infos (workspaceId, userId, expiresAt) dans son mail de bienvenue avant de quitter la page.
- **Pas de gestion de l'audit log V1** : si besoin d'historique des bootstraps émerge (signal opérateur), créer F-XXX en backlog. Pour l'instant `users.created_at` + `workspaces.created_at` suffisent à retracer.
- **Pas de gestion offline / retry** : page admin interne, on suppose connectivité stable. Erreur réseau = snackbar + l'opérateur réessaie manuellement.
- **Pattern d'affichage erreurs 400** : le backend renvoie `{message, field}` — on affiche le `message` global en snackbar ET on highlight le champ via `mat-error` quand `field` est renseigné.
- **Hub super-admin** : à confirmer en dev le nom exact du composant hub (`SuperAdminComponent` ou `SuperAdminHubComponent`) et l'emplacement de la liste des liens. Le pattern d'ajout est trivial (1 entrée de plus dans le tableau de liens existant).
