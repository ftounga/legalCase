# Mini-spec — F-119 / SF-119-01 — Tracking conversion Google Ads

---

## Identifiant

`F-119 / SF-119-01`

## Feature parente

`F-119` — Tracking conversion Google Ads

## Statut

`draft`

## Date de création

2026-04-07

## Branche Git

`feat/SF-119-01-tracking-conversion-ads`

---

## Objectif

Brancher le tag de conversion Google Ads et la capture des paramètres UTM/gclid pour que chaque inscription soit remontée comme conversion dans Google Ads.

---

## Comportement attendu

### Cas nominal

1. **Capture UTM/gclid à l'arrivée** — Quand un visiteur arrive sur la landing page avec des paramètres UTM (`utm_source`, `utm_medium`, `utm_campaign`, `utm_term`, `utm_content`) ou `gclid`, ces paramètres sont stockés en `sessionStorage`.

2. **Événement de conversion à l'inscription** — Quand `submitRegister()` réussit dans `LoginComponent`, un événement `gtag('event', 'conversion', { send_to: 'AW-XXXXXXXXX/YYYYYY' })` est déclenché. L'événement inclut les UTM capturés.

3. **Événement de conversion à l'onboarding OAuth** — Quand `OnboardingComponent.submit()` réussit (premier workspace = nouvel utilisateur), le même événement conversion est déclenché.

4. **Respect du consentement** — L'événement n'est envoyé que si `ConsentService.hasConsent() === true`. Sinon, rien n'est émis (fail-open, pas de blocage du flux).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Pas de paramètres UTM dans l'URL | Aucun stockage, conversion envoyée sans UTM |
| Consentement refusé | Événement conversion non envoyé, inscription normale |
| gtag non chargé (bloqueur pub) | Fail-open, inscription normale |
| Conversion ID Google Ads non configuré | Événement ignoré silencieusement |

---

## Critères d'acceptation

- [ ] Les paramètres UTM et gclid sont capturés depuis l'URL et stockés en sessionStorage à l'arrivée sur le site
- [ ] Un événement `conversion` gtag est émis après inscription locale réussie (submitRegister success)
- [ ] Un événement `conversion` gtag est émis après création de workspace réussie (onboarding OAuth)
- [ ] L'événement conversion inclut les UTM capturés (si présents)
- [ ] L'événement n'est envoyé que si le consentement GA est accordé
- [ ] Le Conversion ID Google Ads est configurable via `environment.ts` (vide en dev)
- [ ] Fail-open : aucune erreur de tracking ne bloque le flux utilisateur
- [ ] Les tests existants passent sans régression

---

## Périmètre

### Hors scope (explicite)

- Configuration de la campagne Google Ads elle-même (M-56)
- Enhanced conversions (envoi email hashé) — à évaluer plus tard
- Tracking des conversions backend (server-side) — pas nécessaire en V1
- Suivi des micro-conversions (analyse lancée, PDF exporté) — déjà couvert par F-77

---

## Technique

### Composants / services impactés

| Composant / Service | Modification |
|---------------------|-------------|
| `AnalyticsService` | Ajouter `trackConversion(conversionId, params)` et `captureUtmParams()` |
| `ConsentService` | Aucune modification — utilisé tel quel |
| `LoginComponent` | Appeler `trackConversion()` dans le `next` de `submitRegister()` |
| `OnboardingComponent` | Appeler `trackConversion()` dans le `next` de `submit()` |
| `LandingComponent` ou `AppComponent` | Appeler `captureUtmParams()` à l'init |
| `environment.ts` / `environment.prod.ts` | Ajouter `googleAdsConversionId: ''` / `'AW-XXXXXXXXX/YYYYYY'` |

### Tables impactées

Aucune — pure frontend.

### Migration Liquibase

- [x] Non applicable

### Endpoints

Aucun — pure frontend.

---

## Plan de test

### Tests unitaires

- [ ] `AnalyticsService` — `captureUtmParams()` stocke les paramètres UTM en sessionStorage
- [ ] `AnalyticsService` — `captureUtmParams()` ne stocke rien si aucun paramètre UTM
- [ ] `AnalyticsService` — `trackConversion()` appelle gtag avec le bon conversion ID et les UTM
- [ ] `AnalyticsService` — `trackConversion()` ne fait rien si gtag absent (fail-open)
- [ ] `AnalyticsService` — `trackConversion()` ne fait rien si consent non accordé
- [ ] `LoginComponent` — `submitRegister()` success → appelle `trackConversion()`
- [ ] `OnboardingComponent` — `submit()` success → appelle `trackConversion()`

### Tests d'intégration

- [x] Non applicable — pure frontend

### Isolation workspace

- [x] Non applicable — tracking côté navigateur, pas de données serveur

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité à son périmètre

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné (justification : ajout d'un appel analytics fail-open, aucun impact sur le flux fonctionnel)

---

## Notes et décisions

- Le Conversion ID Google Ads (`AW-XXXXXXXXX/YYYYYY`) sera renseigné dans `environment.prod.ts` une fois la campagne créée dans Google Ads. En attendant, la valeur reste vide et aucun événement n'est émis.
- On utilise `sessionStorage` (pas `localStorage`) pour les UTM car ils sont liés à la visite en cours, pas à l'utilisateur.
- Le gclid est capturé comme les UTM — Google Ads le réconcilie automatiquement côté serveur.
