---
id: SF-61-01
feature: F-61
title: Responsive mobile — Shell & navigation
status: In Progress
---

## Objectif

Rendre le shell (toolbar + sidenav) utilisable sur mobile (< 768 px) via un hamburger menu et une sidenav en mode `over`.

## Comportement nominal

- **Desktop (≥ 768 px)** : sidenav toujours ouverte, `mode="side"`, comportement actuel inchangé
- **Mobile (< 768 px)** : sidenav fermée par défaut, `mode="over"`, hamburger button dans la toolbar ouvre/ferme la sidenav
- Clic sur un lien de navigation ferme automatiquement la sidenav sur mobile

## Cas d'erreur

Aucun — `BreakpointObserver` est synchrone ; en cas d'échec il reste en mode desktop.

## Critères d'acceptation

1. Sur viewport < 768 px : aucune sidenav visible au chargement
2. Sur viewport < 768 px : bouton hamburger visible dans la toolbar, absent sur desktop
3. Clic hamburger → sidenav s'ouvre par-dessus le contenu (mode `over`)
4. Clic sur n'importe quel lien de navigation → sidenav se ferme (mobile uniquement)
5. Rotation portrait↔paysage adapte immédiatement le mode sans rechargement

## Plan de test

- **T-01** : `isMobile` signal = true si BreakpointObserver émet `(max-width: 767px)`
- **T-02** : `sidenavOpen` signal = false au init quand `isMobile` = true
- **T-03** : `onNavClick()` ferme la sidenav uniquement si `isMobile` = true

## Composants impactés

- `frontend/src/app/layout/shell/shell.component.ts`
- `frontend/src/app/layout/shell/shell.component.html`
- `frontend/src/app/layout/shell/shell.component.scss`

## Hors périmètre

- Écrans internes (case-files, détail dossier, synthèse) → F-62
- Aucune modification du routing ou des guards
