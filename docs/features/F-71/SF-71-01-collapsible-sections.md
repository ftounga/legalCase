# Mini-spec — F-71 / SF-71-01 Sections repliables — délais et notes

---

## Identifiant

`F-71 / SF-71-01`

## Feature parente

`F-71` — Sections repliables — délais et notes

## Statut

`ready`

## Date de création

2026-03-29

## Branche Git

`feat/SF-71-01-collapsible-sections`

---

## Objectif

Rendre les sections "Délais légaux" et "Notes internes" de la page dossier repliables/dépliables au clic sur leur header, avec un badge compteur visible quand la section est repliée.

---

## Comportement attendu

### Cas nominal

- Chaque section (`CaseDeadlinesSectionComponent`, `CaseNotesSectionComponent`) dispose d'un signal `collapsed = signal(false)` — dépliée par défaut
- Le header est cliquable (bouton ou zone cliquable) et appelle `toggleCollapsed()`
- Quand `collapsed()` est `true` :
  - Le contenu (liste + formulaire d'ajout) est masqué via `@if (!collapsed())`
  - Un badge affiche le nombre d'items : `[N délai(s)]` / `[N note(s)]`
  - Le chevron pointe vers le bas (▶) ou vers la droite selon la convention du design system
- Quand `collapsed()` est `false` :
  - Le contenu est affiché normalement
  - Le badge est masqué (le compteur est implicite dans la liste)
  - Le chevron pointe vers le bas (▼)
- L'état replié/déplié est local au composant (pas de persistance localStorage en V1)

### Cas d'erreur / limites

| Situation | Comportement attendu |
|-----------|---------------------|
| Section vide (0 items) | Badge affiche `[0]`, section toujours repliable |
| Chargement en cours | La section reste dans son état courant — pas d'interaction avec `loading` |

---

## Critères d'acceptation

- [ ] `CaseDeadlinesSectionComponent` : header cliquable, contenu masqué quand replié, badge `[N]` visible quand replié
- [ ] `CaseNotesSectionComponent` : même comportement
- [ ] Dépliée par défaut au chargement de la page
- [ ] Le chevron change d'orientation selon l'état (▶/▼ ou équivalent Material)
- [ ] Aucun appel HTTP supplémentaire déclenché par le toggle

---

## Périmètre

### Hors scope (explicite)

- Persistance de l'état en localStorage
- Animation CSS de transition
- Autres sections de la page dossier (métriques, documents, synthèse)

---

## Technique

### Composants modifiés

| Composant | Fichier | Modification |
|-----------|---------|-------------|
| `CaseDeadlinesSectionComponent` | `case-deadlines-section.component.ts` | Ajout signal `collapsed`, méthode `toggleCollapsed()` |
| `CaseDeadlinesSectionComponent` | `case-deadlines-section.component.html` | Header cliquable, `@if (!collapsed())` sur le contenu, badge |
| `CaseNotesSectionComponent` | `case-notes-section.component.ts` | Idem |
| `CaseNotesSectionComponent` | `case-notes-section.component.html` | Idem |

### Migration Liquibase

- [ ] Non applicable — purement frontend

---

## Plan de test

### Tests unitaires — `CaseDeadlinesSectionComponentSpec`

- [ ] U-01 : section dépliée par défaut — contenu visible
- [ ] U-02 : après `toggleCollapsed()` — contenu masqué, badge visible avec compteur correct
- [ ] U-03 : double toggle — retour à l'état initial (déplié)

### Tests unitaires — `CaseNotesSectionComponentSpec`

- [ ] U-04 : section dépliée par défaut — contenu visible
- [ ] U-05 : après `toggleCollapsed()` — contenu masqué, badge visible avec compteur correct
- [ ] U-06 : double toggle — retour à l'état initial (déplié)

### Isolation workspace

- [ ] Non applicable — pas de contexte utilisateur ou workspace impliqué

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — modification purement locale à deux composants existants, aucun service partagé modifié

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

- SF-69-02 (done) — `CaseDeadlinesSectionComponent` disponible
- SF-70-02 (done) — `CaseNotesSectionComponent` disponible

---

## Notes et décisions

- Signal `collapsed = signal(false)` — même pattern que les autres composants du projet
- Badge : `{{ deadlines().length }} délai{{ deadlines().length > 1 ? 's' : '' }}` / `{{ notes().length }} note{{ notes().length > 1 ? 's' : '' }}`
- Chevron : `mat-icon` avec `expand_more` (déplié) / `chevron_right` (replié)
