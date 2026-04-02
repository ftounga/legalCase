# Mini-spec — F-105 / SF-105-01 Refonte page de login — split-screen branding

## Identifiant
`F-105 / SF-105-01`

## Feature parente
`F-105` — Refonte page de login — split-screen branding juridique

## Statut
`in-progress`

## Date de création
2026-04-02

## Branche Git
`feat/SF-105-01-login-redesign`

---

## Objectif

Remplacer la page de login (carte centrée générique) par un layout split-screen professionnel : colonne gauche branding bleu marine, colonne droite formulaire — renforçant la première impression pour un outil juridique B2B.

---

## Comportement attendu

### Cas nominal

**Layout split-screen (≥ 768px)**
- Colonne gauche (fond `#1A3A5C`, 45%) :
  - Logo blanc en haut
  - Tagline : "Analysez vos dossiers juridiques en quelques minutes"
  - 3 bullet points valeur avec icônes Material
  - Mention "Sécurisé · Confidentiel · RGPD" en bas
- Colonne droite (fond blanc, 55%) :
  - Heading "Bienvenue"
  - Bouton Google SSO (principal)
  - Séparateur "ou"
  - Formulaire email + mot de passe
  - Lien "Mot de passe oublié ?"
  - Lien discret "Pas encore de compte ? S'inscrire" qui bascule vers le formulaire d'inscription

**Vue inscription**
- Même layout split-screen
- Colonne droite : formulaire prénom/nom/email/mot de passe + bouton "Créer mon compte"
- Lien retour "Déjà un compte ? Se connecter"
- Bouton Google SSO toujours présent en haut

**Mobile (< 768px)**
- Colonne gauche masquée
- Colonne droite plein écran avec logo en haut

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Email/password invalide | Message d'erreur inline identique à l'actuel |
| Inscription email déjà utilisé | Message d'erreur inline identique à l'actuel |
| Mot de passe oublié | Formulaire inline identique à l'actuel |

---

## Critères d'acceptation

- [ ] Layout split-screen affiché sur ≥ 768px
- [ ] Colonne gauche : logo blanc, tagline, 3 bullets, mention sécurité
- [ ] Colonne droite : Google SSO en premier, formulaire email/password
- [ ] Vue login par défaut (pas de tabs)
- [ ] Clic "Pas encore de compte ?" → vue inscription (même page, pas de navigation)
- [ ] Clic "Déjà un compte ?" → retour vue login
- [ ] Mobile (< 768px) → colonne gauche masquée, colonne droite plein écran avec logo
- [ ] Toutes les fonctionnalités existantes préservées (forgot password, register, errors)
- [ ] Couleurs conformes au design system : `#1A3A5C`, `#FFFFFF`, `#C9973A`
- [ ] Pas de scrollbar sur la page

---

## Périmètre

### Hors scope
- Bouton Microsoft SSO (backend non configuré)
- Animation de transition entre les vues login/inscription
- Page de login responsive mobile avancée (< 768px simplifié suffit)

---

## Technique

### Composants Angular
- `LoginComponent` — modification uniquement (html + scss + ts)

### Tables impactées
Aucune.

### Migration Liquibase
- [x] Non applicable

---

## Plan de test

### Tests unitaires (LoginComponent spec)

- [ ] T-01 : vue login affichée par défaut (`showRegister = false`)
- [ ] T-02 : clic "Pas encore de compte ?" → `showRegister = true`
- [ ] T-03 : clic "Déjà un compte ?" → `showRegister = false`
- [ ] T-04 : soumission login → `loginLocal()` appelé (test existant préservé)
- [ ] T-05 : soumission inscription → `register()` appelé (test existant préservé)
- [ ] T-06 : erreur login → message affiché
- [ ] T-07 : erreur inscription email déjà utilisé → message affiché
- [ ] T-08 : mot de passe oublié → toggleForgot fonctionne toujours
- [ ] T-09 : loginWithGoogle() appelé au clic Google

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Navigation / routing frontend** — la page /login est modifiée structurellement

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `FrontendRedirectIT` (backend) | Redirige vers /login après OAuth2 — URL inchangée | N/A (URL non modifiée) |
| `e2e/smoke/auth.spec.ts` | Tests de navigation vers /login | Smoke tests à vérifier |

### Smoke tests E2E concernés
- [ ] `e2e/smoke/auth.spec.ts` — vérifier que les chemins de login restent fonctionnels

---

## Dépendances

### Subfeatures bloquantes
Aucune.

---

## Notes et décisions

- **Suppression des tabs** : `mat-tab-group` remplacé par un signal `showRegister` — plus sobre, plus professionnel.
- **Google SSO toujours visible dans les deux vues** : logique pour un outil B2B.
- **Pas de Microsoft** : non configuré backend — feature séparée si besoin.
- **Colonne gauche statique** : pas d'image, pas de carousel — texte + couleur suffisent pour le positionnement juridique sobre.
