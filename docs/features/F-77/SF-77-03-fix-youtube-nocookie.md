# Mini-spec — F-77 / SF-77-03 — Fix fuite RGPD YouTube embed (mode no-cookie)

> Statut : `ready`

---

## Identifiant

`F-77 / SF-77-03`

## Feature parente

`F-77` — Google Analytics 4 — tracking + bannière consentement RGPD

## Date de création

2026-05-06

## Branche Git

`feat/SF-77-03-fix-youtube-nocookie`

---

## Objectif

Corriger une fuite RGPD identifiée le 2026-05-06 lors de la vérification de la config tracking : l'iframe YouTube de la landing page charge des cookies tracking DoubleClick **avant** consentement utilisateur.

---

## Comportement attendu

### Cas nominal

1. Visiteur arrive sur la landing page
2. L'iframe vidéo de démo se rend dans le DOM
3. **Aucune requête vers `googleads.g.doubleclick.net` ou `static.doubleclick.net` n'est émise** tant que l'utilisateur n'appuie pas sur Play
4. Lecture de la vidéo : YouTube en mode no-cookie (cookies de personnalisation/pub désactivés, seuls les cookies fonctionnels strictement nécessaires sont déposés)

### Diagnostic du bug existant

`landing.component.ts:70` charge l'iframe via `https://www.youtube.com/embed/...` — domaine standard YouTube qui inclut des trackers DoubleClick au rendering.

### Fix

Remplacer le domaine par `https://www.youtube-nocookie.com/embed/...` — endpoint officiel YouTube en mode "Privacy-Enhanced" qui ne dépose pas de cookies de tracking publicitaire avant interaction utilisateur. URL fonctionnellement identique pour le visiteur (player YouTube standard, qualité identique).

---

## Critères d'acceptation

- [ ] L'URL d'embed produite par `videoEmbedUrl()` utilise `youtube-nocookie.com` au lieu de `youtube.com`
- [ ] Le test unitaire vérifie le domaine no-cookie
- [ ] Validation manuelle Playwright : 0 requête vers `doubleclick` avant consentement (vs 6 actuellement)
- [ ] La vidéo se joue normalement (pas de régression fonctionnelle)

---

## Périmètre

### Hors scope (explicite)

- Lazy-load de l'iframe (chargement post-clic placeholder) — alternative plus stricte mais change l'UX, hors périmètre de ce fix
- Audit RGPD complet d'autres iframes/embeds éventuels — ce fix concerne uniquement le YouTube embed identifié

---

## Technique

### Fichiers impactés

| Fichier | Modification |
|---------|-------------|
| `frontend/src/app/landing/landing.component.ts` | URL domaine : `youtube.com` → `youtube-nocookie.com` |
| `frontend/src/app/landing/landing.component.spec.ts` | Mise à jour test d'URL embed si applicable |

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable.

### Endpoints

Aucun.

---

## Plan de test

### Tests unitaires

- [ ] `landing.component.spec.ts` — `videoEmbedUrl` produit une URL `youtube-nocookie.com/embed/...`

### Tests d'intégration

- [x] Non applicable.

### Validation manuelle

- [ ] Playwright sur prod après déploiement : 0 requête `doubleclick` avant consent (re-run du script de vérification utilisé pour identifier le bug)

### Isolation workspace

- [x] Non applicable — feature publique.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune** — modification ciblée d'une seule URL d'embed.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `LandingComponent` | URL d'embed modifiée | Test unitaire `videoEmbedUrl` |

### Smoke tests E2E concernés

- [ ] Aucun.

---

## Analyse de cohérence transversale

- **Autres iframes YouTube dans le codebase** : grep effectué — uniquement dans `landing.component.ts` et son spec. Pas d'autre endroit à corriger.
- **Autres providers vidéo** (Vimeo, Wistia, etc.) : non utilisés.
- **Autres trackers tiers tertiaires** (Calendly embed, Stripe Checkout iframe en page publique, etc.) : Calendly non embedded (lien externe seulement), Stripe pas en landing publique.

### Nouveau pattern UI ou service partagé

Non applicable — fix ponctuel d'une URL existante.

---

## Impact par domaine métier

Transversal — feature de tracking/RGPD sur landing publique. Pas de spécificité par domaine métier (Travail / Immigration / Famille) ni par pays (FR / BE).

---

## Notes et décisions

- **Pourquoi `youtube-nocookie.com` plutôt que lazy-load post-clic placeholder ?** Le mode no-cookie est l'approche standard CNIL/RGPD recommandée par YouTube lui-même, fonctionnellement transparente pour l'utilisateur, zéro régression UX. Le lazy-load placeholder serait plus strict (aucune requête vers Google avant clic Play) mais change le rendu visuel — à envisager en SF-77-04 si la CNIL durcit sa doctrine.
- **Diagnostic d'origine** : verification manuelle 2026-05-06 via Playwright sur `https://legalcase.fr/` — 6 requêtes `doubleclick` capturées avant clic Accepter, toutes initiées par l'iframe `youtube.com/embed/NGTRMWQKPEA`.
