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
- Symbole : balance de justice stylisée + initiales "LC"
- Texte : `AI` en or (`#C9973A`) — `LegalCase` en bleu marine (`#1A3A5C`)
- Police du logotype : Merriweather Bold

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
