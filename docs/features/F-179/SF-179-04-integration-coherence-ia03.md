# Mini-spec — [F-179 / SF-179-04] Frontend — alerte cohérence sur arrêt SUSPECT cité par l'adverse

> Mini-spec produite via `ai-skills/story-writer.md`. À valider avant dev.

---

## Identifiant

`F-179 / SF-179-04`

## Feature parente

`F-179` — Vérification de jurisprudence citée dans les documents uploadés (FR + BE)

## Statut

`ready`

## Date de création

2026-05-18

## Branche Git

`feat/SF-179-04-integration-coherence-ia03`

---

## Objectif

Mettre en évidence, dans la section « Jurisprudences citées » de la synthèse, les arrêts au statut `SUSPECT` cités par la partie adverse via une **alerte de cohérence visuelle** (badge + popover explicatif), dans l'esprit F-IA-03 — pour que l'avocat repère immédiatement une citation potentiellement de mauvaise foi.

---

## Comportement attendu

### Cas nominal

1. Dans `app-jurisprudence-citations-section` (livré par SF-179-03), chaque référence au statut `SUSPECT` est signalée par une **alerte de cohérence** : un badge d'alerte distinct (icône `warning` / `error_outline`, palette rouge réservée `SUSPECT` — DESIGN_SYSTEM) à côté de la référence.
2. Au survol / clic du badge, un **popover** affiche le détail : la `positionAlleguee` (ce que la partie adverse prétend que dit l'arrêt) confrontée à l'`explication` du backend (la réalité de l'arrêt selon la vérification), avec un libellé explicite du type « Position alléguée incohérente avec le contenu réel de l'arrêt ».
3. Le popover réutilise le **composant de popover de cohérence existant** (`CoherencePopoverComponent` / directive `appCoherencePopover` du package `shared/coherence-popover`) si son contrat le permet pour un usage hors-formulaire ; sinon un popover léger dédié à la section est utilisé (décision tranchée au dev — voir Notes).
4. Un **compteur d'alertes** est affiché dans l'en-tête de la section et dans la `mat-panel-description` du panneau (ex. « 4 références — ⚠️ 1 citation suspecte »).
5. Les statuts `VERIFIED` / `NOT_FOUND` / `UNCERTAIN` ne déclenchent **pas** d'alerte de cohérence — seul `SUSPECT` (arrêt réel + position détournée) est traité comme un signal de cohérence, car c'est le cas « mauvaise foi adverse » que F-IA-03 vise.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Aucun check `SUSPECT` | Aucune alerte, compteur d'alertes à 0 (non affiché) | — |
| `positionAlleguee` absente sur un `SUSPECT` | Le popover affiche uniquement l'`explication` backend, sans la ligne « position alléguée » | — |
| `explication` absente | Le popover affiche un libellé générique « Arrêt réel mais position alléguée à vérifier » | — |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : F-179 n'est pas un outil décisionnel. L'alerte de cohérence F-IA-03 classique (`CoherenceAlert<F>`) est rattachée à un **field éditable d'un formulaire d'outil décisionnel** — F-179 n'a pas de formulaire. SF-179-04 fait une **adaptation du concept** F-IA-03 (signaler une incohérence) à une zone d'affichage, sans réutiliser mécaniquement le `CoherenceAlertBuilder` (qui suppose un `field` enum). Scan ci-dessous.
- [x] **Autres pays** : FR + BE — l'alerte est agnostique du pays (un arrêt FR ou BE `SUSPECT` déclenche la même alerte).
- [x] **Autres domaines** : transversal — l'alerte s'affiche pour tout domaine dès qu'un `SUSPECT` existe.
- [x] **Autres UI patterns** : popover de cohérence (`shared/coherence-popover`). Scan ci-dessous.
- [x] **Autres flows transversaux** : aucun (auth/workspace/plan/navigation inchangés).

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Réutilisation du popover existant** : le package `shared/coherence-popover` fournit `CoherencePopoverComponent` + `CoherencePopoverTriggerDirective`. Au dev, vérifier si la directive accepte un usage hors-formulaire (elle prend des `SourceExplanation[]` + un `reason` — pas strictement liée à un `field`). Si oui → réutilisation directe. Si la directive est trop couplée à `SourceExplanation`, un popover léger dédié à la section est créé, **réutilisant le style visuel** du popover de cohérence (pas de pattern concurrent divergent).
- [x] **`CoherenceAlert<F>` / `CoherenceAlertBuilder`** : **non réutilisé** — ces types supposent un `field` enum d'un formulaire d'outil décisionnel. F-179 n'a pas de formulaire. Réutiliser ce type forcerait un faux `field`. SF-179-04 traite l'alerte au niveau de la **donnée check** (`statut === 'SUSPECT'`), pas d'un field. Justifié et tracé.
- [x] **Patterns concurrents** : aucun pattern d'alerte concurrent introduit. Le style visuel (icône + couleur rouge `SUSPECT` + popover) est cohérent avec le design F-IA-03 existant.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `app-jurisprudence-citations-section` | Oui | Étendu : badge d'alerte + popover sur les `SUSPECT`. |
| `shared/coherence-popover` (directive) | Oui — si contrat compatible | Réutilisé si possible ; sinon popover dédié au style cohérent (décision dev). |
| `CoherenceAlertBuilder` / `CoherenceAlert<F>` | Non | Non réutilisé (couplé aux formulaires d'outils décisionnels — pas de `field` ici). Justifié. |
| Outils décisionnels | Non | F-179 n'est pas un outil décisionnel. |

### Décision

- [x] Étendu à la cible applicable (`app-jurisprudence-citations-section`).
- [x] Non applicable au `CoherenceAlertBuilder` — justification explicite ci-dessus (pas de `field` de formulaire).

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : `app-jurisprudence-citations-section` n'est pas un composant décisionnel (pas de formulaire, pas de POST décisionnel, pas de `TOOL_REGISTRY`, pas de pré-fill). SF-179-04 ajoute une mise en évidence visuelle sur une zone d'affichage en lecture seule. Les 5 blocs F-IA-04 ne s'appliquent pas.

---

## Critères d'acceptation

- [ ] Une référence au statut `SUSPECT` affiche un badge d'alerte distinct (icône + palette rouge DESIGN_SYSTEM) dans la section « Jurisprudences citées ».
- [ ] Au survol / clic du badge, un popover affiche la confrontation `positionAlleguee` ↔ `explication`.
- [ ] Les statuts `VERIFIED` / `NOT_FOUND` / `UNCERTAIN` ne déclenchent aucune alerte de cohérence.
- [ ] L'en-tête de la section et la `mat-panel-description` affichent un compteur de citations suspectes quand il y en a au moins une.
- [ ] Quand `positionAlleguee` est absente, le popover affiche uniquement l'`explication` sans planter.
- [ ] Quand aucun `SUSPECT` n'existe, aucune alerte ni compteur n'est affiché.

---

## Périmètre

### Hors scope (explicite)

- Alerte de cohérence sur les outils décisionnels (F-IA-03 « classique » sur un field de formulaire) — sans objet, F-179 n'a pas de formulaire.
- Alimentation automatique de la génération de conclusions F-98 par les `SUSPECT` — hors scope F-179 (cf. cadrage étape 0).
- Statut markable avocat sur les alertes — hors V1.
- Notification / email sur détection d'un `SUSPECT` — hors scope.

---

## Valeurs initiales

Aucune entité créée — mise en évidence visuelle uniquement.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `statut` (déclencheur) | — | — | alerte déclenchée **uniquement** si `statut === 'SUSPECT'` | — | — |

---

## Technique

### Endpoint(s)

Aucun — SF-179-04 consomme la même donnée que SF-179-03 (`jurisprudence_checks`). Pas de nouvel appel API.

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable.

### Composants Angular

- `JurisprudenceCitationsSectionComponent` (livré par SF-179-03) — étendu : computed `suspectCount`, rendu du badge d'alerte + popover sur les lignes `SUSPECT`.
- `shared/coherence-popover` — réutilisé si compatible, sinon popover léger dédié.
- `SynthesisComponent` — éventuellement : compteur de citations suspectes dans la `mat-panel-description` (déjà couvert par SF-179-03 si le compteur y est générique ; SF-179-04 le précise).

> **Garde-fou OnPush** : `suspectCount` est un `computed` dérivé de l'`@Input() checks` → recalculé automatiquement, pas de `subscribe()` interne. Pas de risque de vue figée.

---

## Plan de test

### Tests unitaires (Jest)

- [ ] `JurisprudenceCitationsSectionComponent` — un check `SUSPECT` → badge d'alerte rendu.
- [ ] `JurisprudenceCitationsSectionComponent` — checks `VERIFIED`/`NOT_FOUND`/`UNCERTAIN` → aucun badge d'alerte.
- [ ] `JurisprudenceCitationsSectionComponent` — `suspectCount` = nombre de `SUSPECT`.
- [ ] `JurisprudenceCitationsSectionComponent` — popover affiche `positionAlleguee` + `explication` quand présentes.
- [ ] `JurisprudenceCitationsSectionComponent` — `positionAlleguee` absente → popover affiche uniquement `explication`.

### Tests d'intégration

- [x] Non applicable (frontend) — couvert par Jest + build prod.

### Isolation workspace

- [x] Non applicable — l'isolation est portée par le backend (SF-179-01).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — extension visuelle d'un composant existant.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `JurisprudenceCitationsSectionComponent` (SF-179-03) | Extension : badge + popover sur `SUSPECT`. | Tests Jest SF-179-03 doivent rester verts. |
| `shared/coherence-popover` | Réutilisation potentielle de la directive. Aucune modification de la directive — usage en lecture. | Tests Jest existants du popover non modifiés. |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — pas de route ni guard modifié.

---

## Dépendances

### Subfeatures bloquantes

- `SF-179-01` — fournit la donnée (`positionAlleguee`, `statut`).
- `SF-179-03` — fournit le composant `JurisprudenceCitationsSectionComponent` étendu ici. SF-179-04 démarre après le merge de SF-179-03 (extension du même composant — pas de parallélisation possible avec SF-179-03, même fichier).

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

- **Réutilisation du popover** : à trancher au dev. La directive `appCoherencePopover` prend `SourceExplanation[]` — si elle peut afficher un contenu libre `positionAlleguee` ↔ `explication`, on la réutilise ; sinon popover dédié au style cohérent. Décision documentée dans la PR.
- `CoherenceAlert<F>` **non réutilisé** : ce type est couplé aux formulaires d'outils décisionnels (`field` enum). F-179 traite l'alerte au niveau de la donnée `check`, pas d'un field. Réutiliser le type aurait créé une dette de convergence inverse (forcer un faux `field`).
- SF-179-04 est volontairement **petite** (taille S) : c'est une extension visuelle de SF-179-03 ; aucune logique backend, aucun nouvel appel API.
