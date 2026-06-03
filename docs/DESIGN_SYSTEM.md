# Design System — AI LegalCase

Source de vérité pour l'identité visuelle et les règles d'interface du projet.

Tout écran produit dans ce projet doit respecter ce document.
Toute divergence doit être explicitement signalée et validée.

---

## 1 — Identité de marque

**Nom produit** : AI LegalCase

**Positionnement visuel** : Outil professionnel pour avocats — sobre, fiable, efficace.
Ni startup flashy, ni logiciel d'entreprise froid. Crédibilité et clarté avant tout.

**Logo** :
- Fichier : `frontend/public/legalcase-logo.png` (1536×1024px, fond blanc)
- Symbole : bouclier bleu marine avec maillet doré en 3D
- Texte : `LEGAL` en bleu marine — `CASE` en or
- Largeur d'affichage standard : 220px dans les cartes / pages d'accueil
- Largeur dans le header : 140px maximum
- Ne pas recadrer, déformer, recolorer ou modifier le logo
- Fond blanc ou très clair uniquement (pas de logo sur fond sombre sans version adaptée)

---

## 2 — Palette de couleurs

| Rôle | Nom | Hex | Usage |
|------|-----|-----|-------|
| **Primary** | Bleu marine | `#1A3A5C` | Boutons principaux, header, éléments actifs |
| **Accent** | Or juridique | `#C9973A` | Accents, badges, highlights |
| **Background** | Gris très clair | `#F5F6FA` | Fond de page |
| **Surface** | Blanc | `#FFFFFF` | Cartes, modales, formulaires |
| **Error** | Rouge sobre | `#C0392B` | Erreurs, alertes destructives |
| **Success** | Vert foncé | `#27AE60` | Validations, statuts positifs |
| **Text principal** | Presque noir | `#1C2B3A` | Corps de texte, titres |
| **Text secondaire** | Gris moyen | `#6B7A8D` | Labels, sous-titres, placeholders |
| **Divider** | Gris clair | `#E0E4EA` | Séparateurs, bordures |

---

## 3 — Typographie

| Usage | Police | Poids | Taille de base |
|-------|--------|-------|----------------|
| Titres h1, h2 | Merriweather | 700 | 32px / 24px |
| Titres h3, h4 | Merriweather | 600 | 20px / 18px |
| Corps de texte | Inter | 400 | 16px |
| Labels, boutons | Inter | 500 | 14px |
| Données, code | JetBrains Mono | 400 | 14px |
| Texte secondaire | Inter | 400 | 12px |

**Import Google Fonts** :
```html
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&family=Merriweather:wght@600;700&family=JetBrains+Mono&display=swap" rel="stylesheet">
```

---

## 4 — Layout général

```
┌─────────────────────────────────────────────┐
│  HEADER  64px fixe                          │
│  [Logo]  [Nav principale]  [Avatar user]    │
├──────────┬──────────────────────────────────┤
│          │                                  │
│  SIDE    │   CONTENU PRINCIPAL              │
│  NAV     │   padding: 24px                  │
│  240px   │                                  │
│          │                                  │
├──────────┴──────────────────────────────────┤
│  FOOTER  48px  [version]  [mentions légales]│
└─────────────────────────────────────────────┘
```

### Header
- Hauteur : 64px, fixe (position sticky)
- Fond : `#1A3A5C` (primary)
- Logo à gauche
- Navigation principale au centre ou à gauche du logo
- Avatar utilisateur + menu à droite
- Ombre portée légère : `box-shadow: 0 2px 8px rgba(0,0,0,0.12)`

### Side navigation
- Largeur : 240px déployée / 64px rétractée (icônes seules)
- Fond : `#FFFFFF`
- Bordure droite : `1px solid #E0E4EA`
- Sections : Dossiers / Documents / Analyses / Paramètres
- Item actif : fond `#EEF2F7`, texte `#1A3A5C`, barre gauche `4px solid #C9973A`

### Contenu principal
- Padding : 24px
- Fond : `#F5F6FA`
- Largeur max : 1280px, centré

### Footer
- Hauteur : 48px
- Fond : `#FFFFFF`
- Bordure haute : `1px solid #E0E4EA`
- Texte : version de l'app + liens légaux, couleur `#6B7A8D`

---

## 5 — Composants Angular Material

### Boutons

| Type | Composant | Usage |
|------|-----------|-------|
| Action principale | `mat-flat-button color="primary"` | Créer, Sauvegarder, Confirmer |
| Action secondaire | `mat-stroked-button` | Annuler, Retour |
| Action destructive | `mat-flat-button color="warn"` | Supprimer, Archiver |
| Action tertiaire | `mat-button` | Liens, actions mineures |
| Icône seule | `mat-icon-button` | Actions dans les tables, toolbars |

### Cartes

- Composant : `mat-card`
- Border-radius : `8px`
- Ombre : `box-shadow: 0 2px 8px rgba(0,0,0,0.08)`
- Padding interne : `24px`
- Pas de fond coloré — toujours `#FFFFFF`

### Formulaires

- Apparence : `outline` sur tous les `mat-form-field`
- Labels toujours au-dessus du champ (floating)
- Messages d'erreur via `mat-error` — jamais de texte libre sous le champ
- Champs obligatoires marqués `*` via `required`

### Tables

- Composant : `mat-table`
- Tri activé sur les colonnes pertinentes (`matSort`)
- Pagination systématique (`mat-paginator`) — jamais de liste infinie sans contrôle
- Ligne hover : fond `#F5F6FA`
- Colonne d'actions toujours à droite

### Notifications

| Situation | Composant | Couleur |
|-----------|-----------|---------|
| Succès | `MatSnackBar` | Fond `#27AE60`, texte blanc |
| Erreur | `MatSnackBar` | Fond `#C0392B`, texte blanc |
| Info | `MatSnackBar` | Fond `#1A3A5C`, texte blanc |
| Confirmation destructive | `MatDialog` | — |

Durée par défaut : 4 secondes. Jamais `window.alert()` ou `window.confirm()`.

### Modales (MatDialog)

- Titre : `mat-dialog-title` — police Merriweather
- Corps : `mat-dialog-content`
- Actions : `mat-dialog-actions` — boutons alignés à droite
- Largeur par défaut : `480px`
- Jamais de modale empilée sur une autre modale

### Badges et statuts

| Statut | Couleur fond | Couleur texte |
|--------|-------------|---------------|
| Actif / En cours | `#E8F5E9` | `#27AE60` |
| En attente | `#FFF8E1` | `#F9A825` |
| Erreur / Rejeté | `#FFEBEE` | `#C0392B` |
| Archivé / Inactif | `#F5F5F5` | `#6B7A8D` |

Border-radius des badges : `4px`, padding : `4px 8px`, police Inter 500 12px.

---

## 6 — Règles d'espacement

- Unité de base : `8px`
- Espacements autorisés : `4px`, `8px`, `16px`, `24px`, `32px`, `48px`, `64px`
- Pas de valeurs arbitraires (ex: `13px`, `22px`)
- Gouttières entre cartes : `16px`
- Marges de section : `32px`

---

## 7 — Icônes

- Bibliothèque : **Material Icons** (déjà inclus avec Angular Material)
- Style : `outlined` en priorité, `filled` pour les états actifs
- Taille standard : `24px`
- Couleur : hériter du texte environnant sauf exception

---

## 8 — Règles de responsive

- Breakpoints Angular Material : `xs` (<600px), `sm` (600-960px), `md` (960-1280px), `lg` (>1280px)
- Mobile : side nav masquée par défaut, accessible via burger menu
- Minimum supporté : 768px (tablette) — pas d'optimisation mobile en V1
- Utiliser `fxLayout` ou CSS Grid/Flexbox, jamais de positions absolues pour le layout

---

## 9 — Ce qui est interdit

- Couleurs hors palette sans validation explicite
- Polices autres que Inter, Merriweather, JetBrains Mono
- `window.alert()`, `window.confirm()`, `window.prompt()`
- Espacements non multiples de 4px
- Tables sans pagination
- Formulaires sans `mat-error` pour les erreurs de validation
- Icônes hors Material Icons sans validation
- Fond coloré sur les cartes (toujours blanc)

---

## 10 — Couche d'accueil — dashboard (dérogation encadrée, F-249)

> Ajoutée par **F-249** (refonte du tableau de bord d'accueil). **Dérogation strictement limitée à l'écran `/dashboard`** (`frontend/src/app/dashboard/`). Le reste du produit reste régi sans exception par les sections 1 à 9.
>
> **Amendement SF-249-02 (2026-06-03, retour PO).** Le hero « futuriste » livré par SF-249-01 (gros dégradé navy + halo doré + KPI glassmorphism) a été jugé **trop massif et grossier**. La dérogation est **réduite** : le bandeau d'accueil devient sobre (bande navy fine portant uniquement salutation + date), le **halo / glow doré** et le **glassmorphism** sont **supprimés**, et les **KPI repassent en cartes blanches standard** (section 5) posées sous le bandeau, hors du bleu.
>
> **Amendement SF-249-03 (2026-06-04, retour PO) — dérogation close.** Le bandeau navy, même affiné, restait indésirable. **Tout aplat / dégradé bleu de l'en-tête d'accueil est supprimé** : « Bonjour Maître X » est un simple titre `#1A3A5C` (Merriweather) sur le fond clair de page, la date en gris (`--muted`). L'écran `/dashboard` ne comporte plus **aucune dérogation** : il est désormais entièrement régi par les sections 1 à 9 (en-tête texte + cartes blanches). Cette section 10 est conservée pour mémoire.

Le tableau de bord d'accueil est la page d'accueil de l'application — la première chose que voit l'avocat à chaque session. Pour lui donner un caractère soigné et une salutation personnalisée, une **bande d'accueil** discrète est autorisée sur ce seul écran. Elle n'introduit **aucune couleur ni police nouvelle** — uniquement de nouvelles *façons d'utiliser* la palette existante.

### Autorisé sur `/dashboard` uniquement

| Élément | Règle |
|---|---|
| ~~**Dégradé navy (bande d'accueil)**~~ | **Obsolète (SF-249-03).** Plus aucun fond/dégradé bleu sur l'en-tête d'accueil. La salutation est un titre texte `#1A3A5C` sur fond clair (section 5/2), la date en `--muted`. |
| ~~**Halo / glow doré**~~ | **Obsolète (SF-249-02).** Supprimé du hero et des cartes. Ne plus réintroduire de lueur dorée en aplat ou en accent. |
| ~~**Glassmorphism**~~ | **Obsolète (SF-249-02).** Plus de surfaces translucides ni de `backdrop-filter`. Les compteurs KPI sont des **cartes blanches opaques** standard (section 5). |
| **Compteurs animés** | Animation *count-up* des valeurs KPI à l'entrée de page, **une seule fois**, durée ≤ 800 ms. Jamais en boucle. |
| **Profondeur** | Ombres standard de la section 5 sur les cartes KPI / sparkline (`$shadow-card`, `$shadow-hover` au survol). Plus d'ombre « hero » renforcée. |
| **Transitions** | Entrée en cascade des blocs (stagger), `cubic-bezier(0.22, 1, 0.36, 1)`. Transitions de survol ≤ 240 ms. |
| **Sparkline** | Mini-graphe de tendance d'activité (7 jours) tracé en or, en **carte blanche discrète** sous le bandeau (et non plus sur le dégradé navy). Décoratif léger, sans axes. |

### Interdit, même sur `/dashboard`

- Toute couleur hors palette (navy / or / sémantiques succès-erreur-avertissement).
- Le mode sombre, le néon, les dégradés multicolores.
- Fond dégradé ou coloré sur les **cartes de section** — elles restent blanches (section 5).
- Toute animation en boucle / permanente (l'écran est consulté plusieurs fois par jour).
- Réduire la lisibilité de l'information critique (délais urgents) sous les effets visuels — la hiérarchie urgences > reste prime sur l'esthétique.

### Polices et espacements

Inchangés : Inter / Merriweather / JetBrains Mono (section 3), espacements multiples de 4px (section 6).
