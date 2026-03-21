# Mini-spec — F-26 / SF-26-05 Frontend refonte page auth

## Identifiant

`F-26 / SF-26-05`

## Feature parente

`F-26` — Auth locale (email/mot de passe)

## Statut

`draft`

## Date de création

2026-03-21

## Branche Git

`feat/SF-26-05-frontend-auth`

---

## Objectif

Refondre la page `/login` en une page à deux onglets ("Se connecter" / "S'inscrire") intégrant
les boutons OAuth existants et les nouveaux formulaires de connexion locale et d'inscription,
plus deux nouvelles pages `/verify-email` et `/reset-password`.

---

## Comportement attendu

### Onglet "Se connecter"

- Boutons Google et Microsoft (comportement inchangé)
- Séparateur "ou"
- Formulaire : email + mot de passe
- Lien "Mot de passe oublié ?" → ouvre une petite section en dessous (email + bouton envoyer)
- Soumission → `POST /api/v1/auth/login`
  - Succès : `AuthService.loadCurrentUser()` puis navigate vers `/case-files`
  - 401 : message d'erreur "Identifiants invalides."
  - 403 : message "Veuillez valider votre email avant de vous connecter."

### Onglet "S'inscrire"

- Boutons Google et Microsoft (comportement inchangé)
- Séparateur "ou"
- Formulaire : prénom, nom, email, mot de passe (min 8 chars)
- Soumission → `POST /api/v1/auth/register`
  - Succès : message "Un email de validation a été envoyé. Vérifiez votre boîte mail." (pas de redirection automatique)
  - 409 : message "Cet email est déjà utilisé."
  - 400 : affichage des erreurs de validation

### Page `/verify-email`

- Route publique : `/verify-email?token=xxx`
- Au chargement : appelle `GET /api/v1/auth/verify-email?token=xxx`
  - Succès : message "Email validé ! Vous pouvez maintenant vous connecter." + lien /login
  - 400 : message d'erreur adapté (token inconnu / expiré / déjà utilisé)
  - Loading state pendant l'appel

### Page `/reset-password`

- Route publique : `/reset-password?token=xxx`
- Formulaire : nouveau mot de passe (min 8 chars) + confirmation
- Soumission → `POST /api/v1/auth/reset-password`
  - Succès : message "Mot de passe réinitialisé ! Connectez-vous avec votre nouveau mot de passe." + lien /login
  - 400 : message d'erreur
- Validation côté client : les deux mots de passe doivent correspondre

---

## Critères d'acceptation

- [ ] Page `/login` : deux onglets "Se connecter" / "S'inscrire" via `MatTabGroup`
- [ ] Onglet connexion : boutons OAuth + formulaire email/mdp + lien mot de passe oublié
- [ ] Onglet inscription : boutons OAuth + formulaire prénom/nom/email/mdp
- [ ] Login nominal → rechargement user + navigation `/case-files`
- [ ] Login 401 → message "Identifiants invalides."
- [ ] Login 403 → message "Veuillez valider votre email avant de vous connecter."
- [ ] Inscription nominale → message de confirmation (pas de redirection)
- [ ] Inscription 409 → message "Cet email est déjà utilisé."
- [ ] Mot de passe oublié → appel `POST /api/v1/auth/forgot-password` + message confirmation
- [ ] Page `/verify-email` : loading → succès/erreur selon réponse API
- [ ] Page `/reset-password` : formulaire + validation concordance mdp + appel API
- [ ] Les deux nouvelles pages sont en routes publiques (pas de authGuard)
- [ ] Conformité Design System : couleurs, polices, Angular Material

---

## Périmètre

### Hors scope

- Modification du layout shell (header/sidenav) — déjà correct
- Renvoi de l'email de validation — non prévu V1

---

## Technique

### Composants Angular

| Composant | Route | Notes |
|-----------|-------|-------|
| `LoginComponent` | `/login` | Refonte — deux onglets Mat |
| `VerifyEmailComponent` | `/verify-email` | Nouveau |
| `ResetPasswordComponent` | `/reset-password` | Nouveau |

### Services

- `AuthService` — ajout `loginLocal()`, `register()`, `forgotPassword()`, `verifyEmail()`, `resetPassword()`

### Routes Angular

- `/verify-email` et `/reset-password` ajoutées sans `authGuard` dans `app.routes.ts`

### Modules Angular Material

- `MatTabsModule` — onglets
- `MatFormFieldModule`, `MatInputModule` — formulaires
- `MatProgressSpinnerModule` — loading
- (déjà présents : MatButtonModule, MatCardModule, MatIconModule, MatDividerModule)

### Migration Liquibase

- [ ] Non applicable

---

## Plan de test

### Tests unitaires

Aucun — logique métier dans les services backend (déjà testés SF-26-02/03/04).

### Tests Karma (composants)

#### LoginComponent

- [ ] T-01 : composant créé
- [ ] T-02 : onglet "Se connecter" visible par défaut
- [ ] T-03 : onglet "S'inscrire" visible
- [ ] T-04 : clic Google → `loginWithGoogle()`
- [ ] T-05 : clic Microsoft → `loginWithMicrosoft()`
- [ ] T-06 : soumission login valide → `loginLocal()` appelé
- [ ] T-07 : erreur 401 → message "Identifiants invalides." affiché
- [ ] T-08 : erreur 403 → message email non vérifié affiché
- [ ] T-09 : soumission inscription → `register()` appelé
- [ ] T-10 : erreur 409 → message "Cet email est déjà utilisé." affiché

#### VerifyEmailComponent

- [ ] T-11 : au chargement → `verifyEmail()` appelé avec le token de l'URL
- [ ] T-12 : succès → message de succès affiché
- [ ] T-13 : erreur → message d'erreur affiché

#### ResetPasswordComponent

- [ ] T-14 : soumission avec mdp concordants → `resetPassword()` appelé
- [ ] T-15 : mdp ne concordent pas → erreur côté client, pas d'appel API
- [ ] T-16 : succès → message de succès affiché

### Isolation workspace

- [ ] Non applicable

---

## Dépendances

### Subfeatures bloquantes

- SF-26-02 — statut : done ✓
- SF-26-03 — statut : done ✓
- SF-26-04 — statut : done ✓

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- `MatTabGroup` pour les onglets — conforme Design System.
- Mot de passe oublié : section inline dans l'onglet connexion (pas de page dédiée), affichée/cachée par toggle.
- Inscription : pas de redirection automatique après succès — l'utilisateur doit valider son email avant de pouvoir se connecter.
- `VerifyEmailComponent` et `ResetPasswordComponent` : routes sans `authGuard` (utilisateurs non connectés).
- `AuthService` : méthodes retournant `Observable` pour que les composants gèrent les états loading/erreur.
