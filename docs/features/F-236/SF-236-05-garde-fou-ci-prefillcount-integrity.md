# Mini-spec — F-236 / SF-236-05 Garde-fou CI `DecisionToolPrefillCountIntegrityIT`

## Identifiant

`F-236 / SF-236-05`

## Feature parente

`F-236` — Robustesse pré-fill IA outils décisionnels frontend

## Statut

`draft`

## Date de création

2026-05-10

## Branche Git

`feat/SF-236-05-garde-fou-ci-prefillcount`

---

## Objectif

Empêcher toute régression future de l'invariant "tout composant décisionnel intégré au TOOL_REGISTRY expose `static getPrefillCount`" via un test d'intégrité CI qui échoue si la règle est violée, et formaliser la règle dans CLAUDE.md (blocage automatique).

---

## Comportement attendu

### Cas nominal

1. Créer un test Jest `decisional-tools-panel/prefill-count-integrity.spec.ts` qui :
   - Itère sur toutes les entrées de `TOOL_REGISTRY`
   - Pour chaque entrée, importe dynamiquement le composant cible
   - Vérifie l'existence de la méthode statique `getPrefillCount` de signature `(input: object) => number`
   - Vérifie qu'elle retourne `0` sur un input vide (`{}`)
   - Vérifie qu'elle retourne un nombre fini (pas NaN, pas Infinity, pas négatif)
2. Si un composant n'expose pas la méthode → le test échoue avec un message clair indiquant le `tool_id` concerné
3. Ajouter une règle de blocage automatique dans CLAUDE.md (section "Blocages automatiques")
4. Ajouter une référence à cette règle dans le skill `ai-skills/frontend-coherence-audit.md`

### Cas d'erreur

| Situation | Comportement |
|-----------|--------------|
| Composant absent (mauvais import) | Erreur claire pointant le tool_id et la classe attendue |
| `getPrefillCount` retourne NaN ou Infinity | Erreur — guard manquant dans le helper |
| `getPrefillCount` retourne un négatif | Erreur — bug logique |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : tous les composants TOOL_REGISTRY (le test est exhaustif par construction)
- [ ] **Autres pays** : non applicable
- [ ] **Autres domaines** : non applicable
- [x] **Autres UI patterns** : pattern miroir de `KNOWN_FRONTEND_TOOL_IDS` (F-164) et `KNOWN_FRONTEND_REFERENTIAL_TYPES` (F-225) — cohérence avec les garde-fous existants
- [ ] **Autres flows transversaux** : non applicable

### Niveaux de vérification

- [ ] Modèle TypeScript / API exposée — non applicable
- [ ] Service / logique métier — non applicable
- [x] Tests existants — pattern à reprendre pour la cohérence (mêmes conventions de nommage, même style de message d'erreur)

### Cas spécifique : nouvelle feature d'outil décisionnel

Non applicable — meta-test.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Tous les outils TOOL_REGISTRY | Oui | Test exhaustif par construction |
| Garde-fous existants (F-164, F-225) | Oui (cohérence) | Mêmes conventions adoptées |

### Décision

- [x] Étendu à toutes les cibles applicables

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : SF d'infrastructure/CI sans composant Angular décisionnel modifié. Le test d'intégrité **vérifie** la conformité F-IA-04 mais ne la modifie pas.

---

## Critères d'acceptation

- [ ] Le test `prefill-count-integrity.spec.ts` est créé et **PASSE** sur le master post-SF-236-02 (les 58 outils sont conformes après les vagues SF-236-02)
- [ ] Le test ÉCHOUE de manière reproductible si on retire `static getPrefillCount` d'un composant aléatoire (vérifié manuellement avec un commit jetable)
- [ ] La règle CLAUDE.md "Composant décisionnel sans `static getPrefillCount` → REFUS" est ajoutée à la section "Blocages automatiques"
- [ ] Le skill `ai-skills/frontend-coherence-audit.md` référence cette règle
- [ ] `npm test` passe en CI sur master post-merge

---

## Périmètre

### Hors scope (explicite)

- Test de parité runtime/static (couvert par les tests unitaires de SF-236-02 par composant)
- Test de cohérence des champs IA consommés (couvert par SF-236-01 + SF-236-02)
- Application du test côté backend (test JS suffit, pas besoin de double couverture)

---

## Plan de test

### Tests unitaires

- [ ] Test du test : commit jetable retirant `static getPrefillCount` d'un composant → CI ÉCHOUE → revert → CI PASSE

### Tests d'intégration

Non applicable.

### Isolation workspace

Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] **Auth / Principal** — non
- [ ] **Workspace context** — non
- [ ] **Plans / limites** — non
- [ ] **Navigation / routing frontend** — non
- [x] **Aucune préoccupation transversale**

### Composants / endpoints existants potentiellement impactés

Aucun (test additif).

### Smoke tests E2E concernés

- [x] Aucun

---

## Dépendances

### Subfeatures bloquantes

- SF-236-02 — doit être `done` (sinon le test échoue immédiatement sur les 54 composants en infraction)

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

### Pattern de référence

`DecisionToolVisibilityIntegrityIT` (F-164 SF-164-01) — test backend qui vérifie que tous les `tool_id` du seed `decision_tool_visibility_rules` ont un équivalent dans `KNOWN_FRONTEND_TOOL_IDS` extrait du frontend.

`LegalReferentialDescriptionIntegrityIT` (F-225 SF-225-03) — test backend qui vérifie l'intégrité des descriptions et de l'intégration UX des `legal_referentials`.

Notre test est côté Jest (frontend) car la cible est un import TypeScript dynamique — le pattern est le même mais le moteur est différent.

### Texte de la règle CLAUDE.md à ajouter

```markdown
| Composant Angular décisionnel frontend (entrée `TOOL_REGISTRY`) sans `static getPrefillCount(input): number` exposé OU avec `getPrefillCount` retournant NaN/Infinity/négatif | REFUS — l'avocat ne verra pas le badge `auto_awesome (+N)` du panel F-IA-04, ce qui invalide la promesse UX "outils décisionnels assistés par l'IA". **Vérifier avant tout merge** : (1) la méthode statique est exposée ; (2) elle reproduit fidèlement la logique de `prefillFromAi()` runtime via le helper partagé `<ComponentName>PrefillRules` ; (3) elle est testée Jest dans 3 cas (0/M/N champs). Le test `prefill-count-integrity.spec.ts` échoue automatiquement en CI si la règle est violée. **Motivation** : audit 2026-05-10 — 54 composants sur 58 (93 %) en infraction, badge silencieusement absent partout sauf 4 outils. Garde-fou F-236 SF-236-05. |
```
