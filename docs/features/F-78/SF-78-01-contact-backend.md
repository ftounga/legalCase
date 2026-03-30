# Mini-spec — F-78 / SF-78-01 — Backend endpoint contact

> Statut : `ready`

---

## Identifiant

`F-78 / SF-78-01`

## Feature parente

`F-78` — Page contact — formulaire email

## Statut

`ready`

## Date de création

2026-03-30

## Branche Git

`feat/SF-78-01-contact-backend`

---

## Objectif

Exposer un endpoint public `POST /api/v1/contact` qui reçoit un message de contact et envoie deux emails : un à l'équipe AI LegalCase et un accusé de réception à l'expéditeur.

---

## Comportement attendu

### Cas nominal

1. Le client envoie `POST /api/v1/contact` avec `{ nom, email, telephone, sujet, message }`
2. Le backend valide les champs obligatoires et les formats
3. Un email est envoyé à `ai-legalcase@ng-itconsulting.com` avec tous les détails du message
4. Un email de confirmation est envoyé à l'adresse de l'expéditeur
5. L'endpoint retourne `200 OK` avec `{ "status": "sent" }`
6. Les deux envois sont fail-open : si un email échoue, le `200` est quand même retourné et l'erreur est loggée

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `nom` absent ou vide | 400 Bad Request — message explicite | 400 |
| `email` absent ou format invalide | 400 Bad Request — message explicite | 400 |
| `message` absent ou vide | 400 Bad Request — message explicite | 400 |
| `telephone` format invalide (si fourni) | 400 Bad Request | 400 |
| `sujet` absent ou vide | 400 Bad Request | 400 |
| Envoi email échoue (SMTP down) | 200 retourné, erreur loggée (fail-open) | 200 |

---

## Critères d'acceptation

- [ ] `POST /api/v1/contact` accessible sans authentification (`permitAll`)
- [ ] Retourne 400 si `nom`, `email`, `sujet` ou `message` est absent ou vide après trim
- [ ] Retourne 400 si `email` n'est pas un format email valide
- [ ] Retourne 400 si `telephone` est fourni mais ne correspond pas à `[\d\s\+\-\(\)]{7,20}`
- [ ] Email envoyé à `ai-legalcase@ng-itconsulting.com` avec : nom, email, téléphone, sujet, message
- [ ] Email de confirmation envoyé à l'adresse de l'expéditeur
- [ ] Retourne `200 { "status": "sent" }` si les validations passent, même si l'envoi SMTP échoue
- [ ] Aucune stacktrace exposée dans la réponse en cas d'erreur

---

## Périmètre

### Hors scope (explicite)

- Stockage en base des messages de contact (pas de table `contact_messages`)
- Rate limiting / anti-spam (V2)
- Interface d'administration pour consulter les messages
- Pièces jointes

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Normalisation |
|-------|-------------|-------------|----------------------------|---------------|
| `nom` | Oui | 100 | Texte libre, non vide après trim | trim() |
| `email` | Oui | 255 | Format email valide (`@Email`) | trim(), lowercase |
| `telephone` | Non | 20 | `[\d\s\+\-\(\)]{7,20}` si fourni | trim() |
| `sujet` | Oui | 200 | Texte libre, non vide après trim | trim() |
| `message` | Oui | 3000 | Texte libre, non vide après trim | trim() |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/contact` | Non | `permitAll` |

### Tables impactées

Aucune — pas de persistance.

### Migration Liquibase

- [x] Non applicable

### Composants Angular

N/A — subfeature backend uniquement.

### Nouveaux composants backend

- `ContactController` — reçoit et valide le DTO, délègue à `EmailService`
- `ContactRequest` (DTO) — 5 champs avec annotations de validation Jakarta
- `EmailService#sendContactToTeam()` — email vers `ai-legalcase@ng-itconsulting.com`
- `EmailService#sendContactConfirmation()` — accusé de réception à l'expéditeur

### Configuration

- `app.contact.team-email=ai-legalcase@ng-itconsulting.com` dans `application.yml`

---

## Plan de test

### Tests unitaires

- [ ] `EmailService#sendContactToTeam()` — mail disabled → log + return sans erreur
- [ ] `EmailService#sendContactConfirmation()` — mail disabled → log + return sans erreur

### Tests d'intégration

- [ ] `POST /api/v1/contact` → 200 avec payload valide complet (avec téléphone)
- [ ] `POST /api/v1/contact` → 200 avec payload valide minimal (sans téléphone)
- [ ] `POST /api/v1/contact` → 400 si `nom` absent
- [ ] `POST /api/v1/contact` → 400 si `email` absent
- [ ] `POST /api/v1/contact` → 400 si `email` format invalide
- [ ] `POST /api/v1/contact` → 400 si `sujet` absent
- [ ] `POST /api/v1/contact` → 400 si `message` absent
- [ ] `POST /api/v1/contact` → 400 si `telephone` format invalide
- [ ] `POST /api/v1/contact` → accessible sans token JWT (no auth required)

### Isolation workspace

- [x] Non applicable — endpoint public, aucune donnée workspace

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — endpoint public isolé, sans accès aux données

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — nouvel endpoint public sans impact sur les chemins existants

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Pattern **fail-open** identique aux emails onboarding : une erreur SMTP ne doit pas renvoyer une 500 à l'utilisateur
- L'adresse de destination est externalisée en `@Value` pour permettre de la changer sans recompiler
- `telephone` est optionnel — s'il est absent, il n'apparaît pas dans le corps de l'email
- L'endpoint est ajouté dans la configuration Spring Security `permitAll` (même section que `/api/v1/public/**`)
