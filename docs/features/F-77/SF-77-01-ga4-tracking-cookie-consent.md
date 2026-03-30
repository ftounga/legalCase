# Mini-spec — F-77 / SF-77-01 — GA4 tracking + bannière consentement RGPD

> Statut : `ready`

---

## Identifiant

`F-77 / SF-77-01`

## Feature parente

`F-77` — Google Analytics 4 — tracking + bannière consentement RGPD

## Statut

`ready`

## Date de création

2026-03-30

## Branche Git

`feat/SF-77-01-ga4-cookie-consent`

---

## Objectif

Intégrer GA4 sur la landing page et les pages légales en respectant le RGPD : aucun cookie déposé sans consentement, bannière accept/refuser affichée à la première visite.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur arrive sur la landing page pour la première fois
2. Une bannière apparaît en bas de page : "Nous utilisons des cookies analytiques pour mesurer l'audience. Accepter / Refuser"
3. **Si l'utilisateur accepte** : choix stocké dans `localStorage` (`ga_consent = granted`), le script GA4 est injecté dynamiquement, les pages vues commencent à être trackées
4. **Si l'utilisateur refuse** : choix stocké dans `localStorage` (`ga_consent = denied`), aucun script GA4 chargé, bannière disparaît
5. Aux visites suivantes : `localStorage` consulté au démarrage → bannière non réaffichée, consentement appliqué silencieusement
6. Les routes authentifiées (`/case-files`, `/workspace`, etc.) ne sont **pas** trackées (GA4 n'est chargé que sur les pages publiques, mais le script de consentement s'applique globalement)

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `localStorage` indisponible (navigation privée restrictive) | Bannière affichée à chaque visite, GA4 non chargé par défaut |
| GA4 bloqué par un ad-blocker | Silencieux — aucune erreur visible pour l'utilisateur |
| Measurement ID absent de l'environnement | GA4 non initialisé, aucune erreur console |

---

## Critères d'acceptation

- [ ] La bannière est visible à la première visite sur la landing page si aucun consentement stocké
- [ ] La bannière ne réapparaît pas après un choix (accept ou refuse)
- [ ] Aucun cookie GA4 (`_ga`, `_gid`) n'est déposé avant acceptation
- [ ] Après acceptation, `gtag('config', 'G-XXXXXXXXXX')` est appelé et les page views remontent dans GA4
- [ ] Après refus, aucun script `gtag.js` n'est chargé dans le DOM
- [ ] Le Measurement ID est lu depuis `environment.ts` (jamais hardcodé)
- [ ] La bannière est responsive (mobile + desktop)
- [ ] Les pages authentifiées ne déclenchent pas de tracking supplémentaire

---

## Périmètre

### Hors scope (explicite)

- Tracking d'événements custom (clics CTA, signup, etc.) — possible en V2 de cette feature
- Bouton "Gérer mes préférences" / granularité par catégorie de cookies
- Intégration Tag Manager
- Tracking des pages authentifiées

---

## Technique

### Endpoint(s)

Aucun — feature 100% frontend.

### Tables impactées

Aucune — aucune migration.

### Migration Liquibase

- [x] Non applicable

### Composants Angular

- `CookieConsentBannerComponent` — bannière standalone, vérifie `localStorage` au `ngOnInit`, émet les actions accept/refuse, s'auto-masque après choix
- `ConsentService` — service singleton injectable, gère lecture/écriture `localStorage`, charge dynamiquement le script GA4, expose `hasConsent()` et `grantConsent()` / `denyConsent()`
- `AppComponent` — inclut `<app-cookie-consent-banner>` dans le template root

### Fichiers impactés

| Fichier | Modification |
|---------|-------------|
| `src/environments/environment.ts` | Ajout `gaId: ''` |
| `src/environments/environment.prod.ts` | Ajout `gaId: 'G-XXXXXXXXXX'` (valeur fournie par le product owner) |
| `src/app/app.component.ts` | Import `CookieConsentBannerComponent` |
| `src/app/app.component.html` | `<app-cookie-consent-banner />` |

---

## Plan de test

### Tests unitaires

- [ ] `ConsentService` — `hasConsent()` retourne `false` si `localStorage` vide
- [ ] `ConsentService` — `grantConsent()` écrit `ga_consent=granted` dans `localStorage` et injecte le script GA4
- [ ] `ConsentService` — `denyConsent()` écrit `ga_consent=denied`, aucun script injecté
- [ ] `ConsentService` — appel avec `gaId` vide → aucun script injecté
- [ ] `CookieConsentBannerComponent` — affichée si pas de consentement stocké
- [ ] `CookieConsentBannerComponent` — masquée si `ga_consent` déjà présent en `localStorage`
- [ ] `CookieConsentBannerComponent` — clic "Accepter" appelle `grantConsent()` et masque la bannière
- [ ] `CookieConsentBannerComponent` — clic "Refuser" appelle `denyConsent()` et masque la bannière

### Tests d'intégration

Non applicable — aucun endpoint backend.

### Isolation workspace

- [x] Non applicable — feature publique (landing + pages légales), aucun workspace impliqué

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [x] **Navigation / routing frontend** — `AppComponent` modifié (ajout du composant bannière)
- [ ] Aucune préoccupation transversale

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `AppComponent` | Ajout d'un composant enfant dans le template root | Tests existants AppComponent non cassés |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/navigation.spec.ts` — vérifier que la bannière n'interfère pas avec les redirections existantes
- [ ] Aucun autre smoke test concerné

---

## Dépendances

### Dépendances externes (bloquantes)

- **Measurement ID GA4** (`G-XXXXXXXXXX`) — à fournir par le product owner avant le dev. Requiert la création d'une propriété GA4 sur analytics.google.com pour le domaine `legalcase.ng-itconsulting.com`.

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Le script GA4 est **injecté dynamiquement via `document.createElement('script')`** après consentement — il n'est jamais présent dans `index.html`. Cela garantit qu'aucune requête vers Google n'est faite avant accord.
- `localStorage` est utilisé (pas de cookie de consentement) — cohérent avec l'approche RGPD "pas de cookie sans consentement".
- Le `gaId` vide en `environment.ts` permet de développer et tester sans déclencher de tracking réel.
- Design de la bannière : sobre, conforme au design system (bleu #1A3A5C, or #C9973A), positionnée en bas de page, non-bloquante (l'utilisateur peut scroller sans accepter).
