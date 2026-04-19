# Mini-spec — F-126 / SF-126-01 Landing — galerie vidéo avec miniatures

## Identifiant
`F-126 / SF-126-01`

## Feature parente
`F-126` — Landing : galerie vidéo multiple (remplace la démo unique)

## Statut `draft`  · Date `2026-04-19`  · Branche `feat/SF-126-01-landing-video-gallery`

---

## Objectif

Remplacer la vidéo unique actuelle de la section "Démo" de la landing par un player principal accompagné de 3 miniatures cliquables en dessous. Chaque miniature montre un cas d'usage différent, ce qui augmente la compréhension produit d'un prospect et la probabilité qu'il trouve un angle qui lui parle.

---

## Comportement

### Structure visuelle

```
 [ Section-header : eyebrow "Démo" + titre + sous-titre ]

 [ ───────── PLAYER ACTIF ─────────  ]   ← iframe YouTube 16:9, ombrage doux
 [                                    ]
 [ ───────── (end player) ──────────  ]

 [ [thumb1] [thumb2] [thumb3] ]      ← row de 3 cartes, même largeur
   titre    titre    titre
   sous     sous     sous
```

### Interaction

- Par défaut la vidéo 1 est sélectionnée, le player charge son iframe
- Clic sur une miniature → l'URL de l'iframe change → même lecteur, nouvelle vidéo
- La carte active a un ring de 2 px couleur accent + scale(1.02) léger, transition 200 ms
- Les thumbnails utilisent l'image native YouTube `https://img.youtube.com/vi/{ID}/maxresdefault.jpg` avec fallback `hqdefault.jpg` via `onerror`
- Chaque carte a un overlay "play" (triangle SVG) avec léger blur-backdrop au hover

### Accessibilité

- Les cartes sont des `<button>` (pas des `<div>` + click), focusables clavier
- `aria-pressed="true"` sur la carte active
- `aria-label` descriptif : "Lire : {titre}"

### Cas d'erreur

- Thumbnail YouTube indisponible (404 maxresdefault) → fallback `hqdefault.jpg` automatique
- Iframe YouTube qui échoue → YouTube gère l'UI d'erreur, pas notre souci

---

## Critères d'acceptation

- [ ] La section "Démo" de la landing affiche 3 miniatures cliquables sous le player principal
- [ ] Par défaut la vidéo 1 est jouée
- [ ] Clic sur une miniature change la vidéo du player (fade-in doux) sans rechargement de page
- [ ] La carte active a un style visuel distinct (ring + scale)
- [ ] Responsive : sur mobile (< 768 px), les 3 cartes passent sur 1 colonne empilée
- [ ] Le player reste en 16:9 sur toutes les tailles d'écran
- [ ] Navigation clavier : Tab traverse les 3 cartes, Enter/Space joue la vidéo sélectionnée
- [ ] Aucune régression sur les autres sections de la landing (hero, problem, pricing, FAQ)

---

## Plan de test

### Unitaires frontend
- `landing.component.spec.ts` :
  - `videos` tableau contient bien 3 entrées avec `videoId`, `title`, `subtitle`
  - `selectedVideoId()` retourne le 1er par défaut
  - `selectVideo('I5EemkFR8NE')` met à jour le signal
  - `videoEmbedUrl()` retourne `https://www.youtube.com/embed/{selectedVideoId}`

### Test visuel manuel
- Ouvrir `/` en local et staging, vérifier le rendu sur desktop + mobile (< 768 px)
- Vérifier le focus clavier sur les 3 cartes
- Cliquer chaque miniature et vérifier que le player change

### Isolation workspace
- Non applicable — page publique non authentifiée

---

## Tables / endpoints / composants impactés

### Frontend
- `landing.component.html` — section `.video-showcase` refondue
- `landing.component.ts` — ajout du signal `selectedVideoId` + tableau `videos` + méthode `selectVideo()` + computed `videoEmbedUrl`
- `landing.component.scss` — styles `.video-thumbnails`, `.video-thumb`, `.video-thumb--active`, responsive
- `landing.component.spec.ts` — 4 nouveaux tests

### Backend / Config / Migration
- Aucun changement

---

## Hors périmètre

- Autoplay quand on change de vidéo (comportement anti-UX, YouTube le gère par défaut on-demand)
- Carousel infini ou swipe mobile (pattern explicitement rejeté avec l'utilisateur, option A retenue)
- Gestion de plus de 3 vidéos (si besoin plus tard : ajuster la grille CSS, pas de code JS à changer)
- Préchargement des 3 iframes (coût perf trop élevé — 1 seule iframe active à la fois)

---

## Analyse de cohérence transversale

| Cible | Applicable | Classement |
|---|---|---|
| Autres pages avec vidéo intégrée | Non | La landing est la seule page avec vidéo publique actuellement |
| Pattern vidéo dans l'app authentifiée (tuto onboarding) | **Backlog** — si un jour on ajoute des tutos vidéo intégrés (ex. aide contextuelle), ce composant serait réutilisable. Pour l'instant inexistant, pas d'overlap. |
| Design system — nouveau pattern carte cliquable | Existant | Réutilisation des tokens de couleur DESIGN_SYSTEM (accent pour ring, shadow existante) |

**Analyse d'impact cross-cutting** :
- [ ] Auth / Principal — non touché
- [ ] Workspace context — non touché
- [ ] Plans / limites — non touché
- [ ] Navigation / routing — non touché

Aucun smoke E2E concerné.

---

## Nouveau pattern UI ou service partagé

- [x] **Nouveau pattern "galerie vidéo avec miniatures"** — isolé à `landing.component`. Pas de composant partagé extrait : le cas d'usage est unique à la landing publique. Si un besoin similaire émerge (onboarding, FAQ vidéo, centre d'aide) → refactor en `<app-video-gallery>` réutilisable (→ backlog tracking).
