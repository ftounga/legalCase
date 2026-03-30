# Mini-spec — F-78 / SF-78-02 — Frontend page contact

> Statut : `ready`

---

## Identifiant

`F-78 / SF-78-02`

## Feature parente

`F-78` — Page contact — formulaire email

## Statut

`ready`

## Date de création

2026-03-30

## Branche Git

`feat/SF-78-02-contact-frontend`

---

## Objectif

Créer la page publique `/contact` avec un formulaire 5 champs (nom, email, téléphone, sujet, message), appeler `POST /api/v1/contact`, afficher un message de succès ou d'erreur, et ajouter un lien "Contact" dans le footer de la landing page.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur navigue sur `/contact`
2. Il remplit le formulaire : nom*, email*, téléphone (optionnel), sujet*, message*
3. Il clique "Envoyer"
4. Un spinner s'affiche pendant l'appel API
5. En cas de succès : le formulaire est remplacé par un message "Votre message a bien été envoyé. Nous vous répondrons dans les plus brefs délais."
6. Un bouton "Envoyer un autre message" réinitialise le formulaire

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Champ obligatoire vide à la soumission | `mat-error` sous le champ concerné |
| Email format invalide | `mat-error` sous le champ email |
| Téléphone format invalide | `mat-error` sous le champ téléphone |
| Erreur 400 retournée par l'API | Message d'erreur global via `MatSnackBar` |
| Erreur 500 / réseau | `MatSnackBar` : "Une erreur est survenue. Veuillez réessayer." |

---

## Critères d'acceptation

- [ ] Route `/contact` accessible publiquement (pas d'`AuthGuard`)
- [ ] Formulaire avec 5 champs : nom, email, téléphone (optionnel), sujet, message
- [ ] Validation côté client : champs obligatoires, format email, format téléphone
- [ ] Bouton "Envoyer" désactivé si formulaire invalide
- [ ] Spinner pendant l'appel API
- [ ] Message de succès affiché après envoi réussi (formulaire masqué)
- [ ] Bouton "Envoyer un autre message" réinitialise l'état
- [ ] Erreurs API affichées via `MatSnackBar`
- [ ] Lien "Contact" ajouté dans le footer de `LandingComponent`
- [ ] Page responsive (mobile + desktop)
- [ ] Couleurs et polices conformes au design system

---

## Périmètre

### Hors scope (explicite)

- Captcha / protection anti-spam
- Confirmation par email visible dans l'UI (géré côté backend)
- Historique des messages envoyés

---

## Contraintes de validation (côté client)

| Champ | Obligatoire | Validation Angular |
|-------|-------------|-------------------|
| `nom` | Oui | `Validators.required` |
| `email` | Oui | `Validators.required`, `Validators.email` |
| `telephone` | Non | Pattern `[\d\s\+\-\(\)]{7,20}` si non vide |
| `sujet` | Oui | `Validators.required` |
| `message` | Oui | `Validators.required`, `maxLength(3000)` |

---

## Technique

### Endpoint(s) consommés

| Méthode | URL | Auth |
|---------|-----|------|
| POST | `/api/v1/contact` | Non |

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable

### Composants Angular

- `ContactComponent` — page standalone, formulaire réactif, gestion succès/erreur
- `ContactService` — appel HTTP `POST /api/v1/contact`, retourne `Observable`

### Fichiers impactés

| Fichier | Modification |
|---------|-------------|
| `src/app/app.routes.ts` | Ajout route `{ path: 'contact', component: ContactComponent }` |
| `src/app/landing/landing.component.html` | Ajout lien "Contact" dans le footer |

---

## Plan de test

### Tests unitaires

- [ ] `ContactService#send()` — émet `POST /api/v1/contact` avec le bon payload
- [ ] `ContactComponent` — formulaire invalide si champ obligatoire vide
- [ ] `ContactComponent` — bouton désactivé si formulaire invalide
- [ ] `ContactComponent` — appelle `ContactService#send()` à la soumission
- [ ] `ContactComponent` — affiche le message de succès après réponse 200
- [ ] `ContactComponent` — affiche `MatSnackBar` en cas d'erreur API

### Tests d'intégration

Non applicable — composant frontend, pas d'endpoint à tester ici.

### Isolation workspace

- [x] Non applicable — page publique, aucun workspace

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [x] **Navigation / routing frontend** — nouvelle route `/contact` ajoutée dans `app.routes.ts`
- [ ] Aucune préoccupation transversale

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `app.routes.ts` | Ajout d'une route publique | Smoke test navigation — routes protégées non affectées |
| `LandingComponent` | Ajout d'un lien footer | Test existant du composant non cassé |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/navigation.spec.ts` — vérifier que les routes protégées existantes ne sont pas affectées

---

## Dépendances

### Subfeatures bloquantes

- **SF-78-01** — doit être mergée (ou développée en parallèle avec mock) avant le déploiement end-to-end

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- `ContactComponent` placé dans `src/app/contact/` (même structure que `landing/`, `legal/`)
- Pas d'`AuthGuard` — la route est publique comme `/mentions-legales`, `/cgu`, `/privacy`
- Le formulaire utilise `ReactiveFormsModule` + `mat-form-field appearance="outline"` conformément au design system
- En cas de succès, le formulaire est **masqué** (pas redirigé) pour permettre "Envoyer un autre message"
