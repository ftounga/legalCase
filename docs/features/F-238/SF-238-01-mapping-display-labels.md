# Mini-spec — F-238 / SF-238-01 Mapping labels humains + garde-fou CI dynamique

## Identifiant

`F-238 / SF-238-01`

## Feature parente

`F-238` — Catalogue d'outils décisionnels cliquable + labels humains

## Statut

`ready`

## Date de création

2026-05-11

## Branche Git

`feat/F-238-catalogue-cliquable-labels` (mono-branche groupée)

---

## Objectif

Ajouter un `displayLabel` humain (FR/BE/transversal) sur chaque entrée `TOOL_REGISTRY` et un garde-fou CI dynamique qui interdit la régression « tool_id brut affiché dans le catalogue ».

---

## Comportement attendu

### Cas nominal

1. Chaque entrée de `TOOL_REGISTRY` (DecisionToolsPanelComponent) expose un champ `displayLabel: string` non vide.
2. Le template du panel rend `resolveDisplayLabel(toolId)` qui lit `TOOL_REGISTRY.get(toolId)?.displayLabel ?? toolId`.
3. Conventions :
   - **FR** : « Désunion irrémédiable (FR) », « Licenciement — Validité (FR) »
   - **BE** : « Désunion irrémédiable (Belgique) », « Préavis (Belgique) »
   - **transversal** (sans pays) : pas de suffixe
4. Un test d'intégration backend (`DecisionToolDisplayLabelIntegrityIT`) extrait dynamiquement TOOL_REGISTRY depuis le source frontend et :
   - vérifie que chaque entrée a un `displayLabel` non vide,
   - vérifie qu'aucun `displayLabel` ne contient le `tool_id` (interdit le copier-coller paresseux).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Entrée TOOL_REGISTRY sans `displayLabel` | CI rouge (assert TS strict + IT échoue) | n/a |
| `displayLabel` qui contient le `tool_id` | IT échoue avec liste des coupables | n/a |
| `tool_id` inconnu rendu par le template | Affiche `tool_id` brut (fallback préservé) | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] Autres outils métier : couverture exhaustive — tous les 103 outils de TOOL_REGISTRY traités dans une même SF.
- [x] Autres pays : FR + BE + transversal — convention de suffixe documentée ci-dessus.
- [x] Autres domaines : Travail / Famille / Immigration — chaque domaine relit chaque outil pour vérifier la qualité du libellé.
- [x] Autres UI patterns : un seul mécanisme (`displayLabel` sur l'entrée registry) — pas de doublon avec `TOOL_LABEL` (qui sert au modal de l'outil instancié, pas au chip catalogue).
- [x] Autres flows transversaux : aucun — pur changement de rendu chip + ajout métadonnée.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Tous les outils TOOL_REGISTRY | Oui | Intégré (tous les 103) |
| Composant `decision-tool-card` (modal) | Non | Utilise déjà `TOOL_LABEL` via `getToolMetadata()` (SF-177-11) |
| Garde-fou CI | Oui | Nouveau test `DecisionToolDisplayLabelIntegrityIT` |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — SF de métadonnée pure sur registre + IT backend. Pas de composant décisionnel créé/modifié. Pas de pré-fill IA, pas de F-IA-03 (rien n'est saisi).

---

## Impact par domaine métier

Cette SF est **transversale (infrastructure)** : elle ajoute une métadonnée d'affichage uniforme sur 103 outils (Travail FR/BE, Famille FR/BE, Immigration FR/BE). Aucune adaptation par domaine — convention de nommage identique : titre métier + suffixe `(FR)` / `(Belgique)` / aucun si transversal.

---

## Parité des domaines métier (niveau ≥ 5)

- [x] Niveau du tool décisionnel livré : **non applicable, justifier** — SF infrastructure (métadonnée d'affichage), pas un outil décisionnel.

---

## Critères d'acceptation

- [ ] L'interface `DecisionToolRegistryEntry` expose un champ `displayLabel: string` (obligatoire, non optionnel).
- [ ] Les 103 entrées de `TOOL_REGISTRY` ont chacune un `displayLabel` non vide et différent du `tool_id` (audit grep en self-check pré-commit).
- [ ] Le template `decisional-tools-panel.component.html` ligne 61 rend `{{ resolveDisplayLabel(toolId) }}` au lieu de `{{ toolId }}`.
- [ ] La méthode `resolveDisplayLabel(toolId: string): string` retourne `TOOL_REGISTRY.get(toolId)?.displayLabel ?? toolId`.
- [ ] Le test `DecisionToolDisplayLabelIntegrityIT` est créé et vert (extraction dynamique + 2 asserts).
- [ ] Convention de naming respectée (FR, BE, transversal sans suffixe).
- [ ] `npm run build` frontend OK ; `./mvnw test -Dtest=DecisionToolDisplayLabelIntegrityIT` OK.

---

## Périmètre

### Hors scope

- Changement de comportement du chip (cliquable, activation) → SF-238-02.
- Endpoint backend d'activation manuelle → SF-238-03.

---

## Plan de test

### Unitaires / Jest

- 1 test : `resolveDisplayLabel` retourne le libellé attendu pour 1 entrée connue.
- 1 test : `resolveDisplayLabel` retourne le `toolId` brut si entrée inconnue (forward-compat).

### Intégration backend

- `DecisionToolDisplayLabelIntegrityIT` :
  - assert chaque entrée a un `displayLabel` non vide,
  - assert aucun `displayLabel` ne contient le `tool_id`,
  - extraction dynamique du source frontend via regex (pattern miroir `DecisionToolVisibilityIntegrityIT` post-refactor PR #918).

### Isolation workspace

- N/A (pas de donnée utilisateur, métadonnée pure côté code source).

---

## Tables / endpoints / composants impactés

- **Composants** : `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts` (interface + 103 entries + méthode), `.html` (1 ligne).
- **Tests** : `backend/src/test/java/fr/ailegalcase/casefile/DecisionToolDisplayLabelIntegrityIT.java` (nouveau).
- **Endpoints** : aucun.
- **Tables** : aucune.
