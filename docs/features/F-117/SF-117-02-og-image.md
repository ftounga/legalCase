# Mini-spec — F-117 / SF-117-02 Image OG 1200x630

---

## Identifiant

`F-117 / SF-117-02`

## Feature parente

`F-117` — SEO — Mots-clés métier dans meta tags + structured data JSON-LD

## Statut

`draft`

## Date de création

2026-04-06

## Branche Git

`feat/SF-117-02-og-image`

---

## Objectif

Remplacer le logo PNG utilisé comme image OG par une vraie image 1200x630 avec branding, tagline et charte graphique pour un rendu optimal sur LinkedIn, Twitter et Slack.

---

## Comportement attendu

### Cas nominal

1. Un fichier `og-image.png` (1200x630) est ajouté dans `frontend/public/`
2. Les meta `og:image` et `twitter:image` pointent vers `og-image.png` au lieu de `legalcase-logo.png`
3. Les dimensions sont déclarées via `og:image:width` et `og:image:height`

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Image non trouvée | LinkedIn/Twitter affichent un placeholder — pas d'impact sur le site |

---

## Critères d'acceptation

- [ ] `og-image.png` existe dans `frontend/public/`, 1200x630px
- [ ] `og:image` pointe vers `og-image.png`
- [ ] `twitter:image` pointe vers `og-image.png`
- [ ] `og:image:width` et `og:image:height` sont déclarés
- [ ] Build prod OK, tests verts

---

## Périmètre

### Hors scope

- Changement de contenu de la landing
- Modification du JSON-LD

---

## Technique

### Fichiers impactés

| Fichier | Modification |
|---------|-------------|
| `frontend/public/og-image.png` | Nouveau — image 1200x630 |
| `frontend/src/index.html` | og:image, twitter:image, og:image:width/height |

---

## Plan de test

- [ ] Build prod OK
- [ ] Tests existants verts

## Analyse d'impact

- [x] **Aucune préoccupation transversale**
