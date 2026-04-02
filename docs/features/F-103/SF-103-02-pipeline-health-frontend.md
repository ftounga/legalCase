# SF-103-02 — Santé pipeline IA — frontend

## Objectif

Afficher dans `/super-admin` une section "Santé pipeline" avec les métriques
de SF-103-01 : 3 cartes queues RabbitMQ + tableau jobs stats, avec code couleur.

---

## Comportement nominal

### Section "Santé pipeline"

Positionnée entre "Métriques plateforme" et "Outils & monitoring".

#### Cartes queues (3 cartes horizontales)

Chaque carte affiche :
- Nom de la queue (ex. `chunk.analysis`)
- `messagesReady` — badge coloré
- `messagesUnacknowledged` — en transit
- `consumers` — nombre de consumers actifs (reflète le nb de pods × concurrence)
- Indicateur de statut global (point coloré)

Code couleur `messagesReady` :
- 0 → vert (`--color-success`)
- 1–10 → orange (`--color-warning`)
- > 10 → rouge (`--color-error`)
- `available: false` → gris + label "Indisponible"

#### Tableau jobs stats

2 lignes (24h / 7j) × 4 colonnes (DONE / PROCESSING / PENDING / FAILED).
Colonne FAILED : 0 → vert, > 0 → orange.

---

## Critères d'acceptation

- [ ] Section visible uniquement si données chargées
- [ ] 3 cartes queues avec code couleur conforme au design system
- [ ] Tableau 2 lignes × 4 colonnes jobs stats
- [ ] `available: false` → carte grisée + "Indisponible"
- [ ] Chargement via `forkJoin` avec les autres appels super-admin existants
- [ ] Couleurs issues exclusivement de `DESIGN_SYSTEM.md`

---

## Plan de test

| ID | Cas | Assertion |
|----|-----|-----------|
| U-01 | Données chargées → section visible | section présente dans le DOM |
| U-02 | messagesReady=0 → couleur verte | classe CSS success présente |
| U-03 | messagesReady=5 → couleur orange | classe CSS warning présente |
| U-04 | messagesReady=15 → couleur rouge | classe CSS error présente |
| U-05 | available=false → label "Indisponible" | texte présent |
| U-06 | failed > 0 → colonne FAILED orange | classe CSS warning présente |

---

## Composants impactés

- Modifié : `super-admin.component.ts` — chargement `pipelineHealth`
- Modifié : `super-admin.component.html` — nouvelle section
- Modifié : `super-admin.component.scss` — styles cartes pipeline
- Modifié : `super-admin.service.ts` — nouvelle méthode `getPipelineHealth()`
- Modifié : `super-admin.model.ts` — nouveaux types `PipelineHealth`, `QueueHealth`, `JobStats`

## Hors périmètre

- Refresh automatique (polling)
- Graphiques historiques
