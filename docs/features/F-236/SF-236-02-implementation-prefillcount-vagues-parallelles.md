# Mini-spec — F-236 / SF-236-02 Implémentation `static getPrefillCount` + helper partagé sur 54 composants (3 vagues parallèles)

## Identifiant

`F-236 / SF-236-02`

## Feature parente

`F-236` — Robustesse pré-fill IA outils décisionnels frontend

## Statut

`draft` (à passer en `ready` après livraison de SF-236-01)

## Date de création

2026-05-10

## Branches Git

- `feat/SF-236-02-travail` (vague Travail FR+BE)
- `feat/SF-236-02-immigration` (vague Immigration FR+BE)
- `feat/SF-236-02-famille` (vague Famille FR+BE)

## Worktrees

Chaque vague est exécutée dans un worktree distinct pour respecter la règle "deux SF parallèles ne partagent pas la même branche".

---

## Objectif

Implémenter `static getPrefillCount(input): number` sur les 54 composants décisionnels qui en sont dépourvus, en extrayant systématiquement un helper partagé `<ComponentName>PrefillRules.ts` consommé par `prefillFromAi()` runtime ET le static — la divergence devient impossible par construction.

---

## Comportement attendu

### Cas nominal (par composant)

1. Créer le fichier `<component>-prefill-rules.ts` à côté du composant
2. Y déplacer toutes les constantes de pré-fill (codes, mappings, keywords) et toutes les fonctions pures de calcul
3. Refactorer `prefillFromAi()` runtime pour appeler les fonctions du helper sur l'input et appliquer les résultats aux signals
4. Implémenter `static getPrefillCount(input): number` qui appelle les **mêmes fonctions** du helper et retourne le nombre de champs qui auraient été pré-remplis
5. Ajouter test Jest avec 3 cas obligatoires : (a) 0 champs, (b) M champs partiels, (c) N champs nominal
6. Vérifier que le badge `auto_awesome (+N)` s'affiche bien sur la card du panel F-IA-04

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Le composant n'a pas d'`@Input() aiData?` | SF-236-02 hors scope — l'absence de pré-fill est un bug séparé à traiter en SF-236-04 |
| Le helper de plusieurs composants partage une logique commune | Extraire dans `frontend/src/app/case-files/shared/prefill/` (max 1 par domaine) |
| Un champ IA consommé n'existe pas dans le record `*ExtractedData` | Échec immédiat à porter en SF-236-03 (champ mort) |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : 54 composants à modifier — c'est précisément le scope
- [x] **Autres pays** : FR + BE — chaque vague gère les deux pays de son domaine
- [x] **Autres domaines** : 3 vagues parallèles (1 par domaine)
- [x] **Autres UI patterns** : le pattern helper introduit est utilisable au-delà des outils décisionnels
- [ ] **Autres flows transversaux** : non applicable

### Niveaux de vérification

- [x] Modèle TypeScript / API exposée
- [x] Service / logique métier (helper extrait + runtime + static)
- [ ] Record / DTO backend — vérifier la cohérence des champs consommés (déjà couvert par SF-236-01)
- [ ] Entité JPA + schéma DB — non applicable
- [x] Tests existants — étendus avec les 3 cas obligatoires

### Cas spécifique : nouvelle feature d'outil décisionnel

Non applicable — refactor de l'existant, pas de nouvel outil créé.

### Cas spécifique : nouveau pattern UI ou service partagé

**Applicable** — le helper `<ComponentName>PrefillRules` est un nouveau pattern qui sera adopté par 58 composants. Le contrat est défini par SF-236-01 et déployé par cette SF.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| 54 composants à mettre en conformité | Oui | Implémentation directe par vague |
| 4 composants déjà conformes | Oui | Refactor pour adopter le helper (suppression duplication) |
| Composants `*-section` hors panel | Non | Hors scope F-236 |

### Décision

- [x] Étendu à toutes les cibles applicables
- [ ] Backlog : non applicable
- [ ] Non applicable : 49 composants hors panel

---

## Conformité F-IA-04 (SF frontend décisionnelle)

Cette SF est **explicitement** la SF de mise en conformité F-IA-04 sur les 58 composants. Tous les blocs ci-dessous s'appliquent.

### 1. Cohérence visuelle

- [ ] Aucune modification UI dans cette SF — palette / datepicker / typo / gate / erreurs / refresh préservés à l'identique. Diff = +helper +static, pas de changement de markup.

### 2. Pré-fill IA

- [ ] `@Input() aiData?` déjà typé strictement sur tous les composants (vérifié par SF-236-01)
- [ ] `prefillFromAi()` déjà invoqué dans `ngOnInit()` ET `ngOnChanges()` — sinon anomalie remontée par SF-236-01 et corrigée
- [ ] Signals `provenance<Field>` déjà en place — préservés
- [ ] Badges UI `auto_awesome` à côté des champs pré-remplis — préservés
- [ ] Handlers `onXxxChange()` — préservés

### 3. Validation F-IA-03

- [ ] `coherenceAlerts` computed — préservé
- [ ] Hiérarchie F-96 > Question IA > IA > Pièce manquante — préservée
- [ ] Directive `<app-coherence-popover-trigger>` — préservée
- [ ] Helper `CoherenceAlertBuilder` — préservé

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [ ] Entrée TOOL_REGISTRY déjà présente — préservée
- [ ] Constantes `TOOL_LABEL` et `TOOL_ICON` — préservées
- [ ] **Static `getPrefillCount` ajouté** — c'est l'objet principal de cette SF
- [ ] **Parité stricte runtime/static** — garantie par construction via helper partagé
- [ ] Tests Jest 0/M/N — ajoutés pour chaque composant
- [ ] `tool_id` déjà présent dans `KNOWN_FRONTEND_TOOL_IDS` — préservé

### 5. Parité des domaines métier

- [x] **Non applicable** — refactor sans création d'outil de niveau ≥ 5. Aucun nouveau scoring, comparateur ou détection.

---

## Critères d'acceptation

- [ ] Les 54 composants exposent `static getPrefillCount(input): number` avec la signature unifiée
- [ ] Chaque composant a un fichier helper `<component>-prefill-rules.ts` à côté
- [ ] Le runtime `prefillFromAi()` consomme exclusivement les fonctions du helper (pas de constante ou logique en dur dans le composant)
- [ ] Le static `getPrefillCount` consomme les **mêmes** fonctions du helper sur les **mêmes** inputs
- [ ] Tests Jest présents : 3 cas par composant (0/M/N champs) — ~162 tests minimum (54 × 3)
- [ ] `npm run build` passe sur les 3 worktrees
- [ ] `npm test` passe sur les 3 worktrees (pas de régression sur les ~3000 tests existants)
- [ ] Le panel F-IA-04 affiche un badge `auto_awesome (+N)` correct sur les composants modifiés (vérification manuelle sur 1 dossier de chaque domaine)

---

## Périmètre

### Hors scope (explicite)

- Modification de la logique de pré-fill (pas de nouveau champ IA consommé)
- Correction des divergences détectées (couvert par SF-236-03)
- Robustification mono-champ (couvert par SF-236-04)
- Garde-fou CI (couvert par SF-236-05)
- Création de nouveaux outils décisionnels (couvert par F-220+)

---

## Plan de test

### Tests unitaires (Jest, par composant)

- [ ] `<component>.spec.ts` — `getPrefillCount` cas 0 : input vide → 0
- [ ] `<component>.spec.ts` — `getPrefillCount` cas M : input partiel → M
- [ ] `<component>.spec.ts` — `getPrefillCount` cas N : input complet → N
- [ ] `<component>.spec.ts` — runtime `prefillFromAi()` produit le même décompte que `getPrefillCount` (parité observable)

### Tests d'intégration

Non applicable — pas de modification backend.

### Isolation workspace

Non applicable — frontend pur.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] **Auth / Principal** — non
- [ ] **Workspace context** — non
- [ ] **Plans / limites** — non
- [ ] **Navigation / routing frontend** — non
- [x] **Aucune préoccupation transversale critique** — refactor isolé par composant, pas de service partagé impacté

### Composants / endpoints existants potentiellement impactés

Aucun (refactor interne par composant, contrat externe inchangé).

### Smoke tests E2E concernés

- [ ] `e2e/smoke/case-detail.spec.ts` (s'il existe) — vérifier ouverture et utilisation des outils décisionnels
- [x] Aucun smoke test critique — refactor isolé

---

## Dépendances

### Subfeatures bloquantes

- SF-236-01 — doit être `done` (matrice + contrat helper publiés)

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

### Découpage des vagues (calibration finale issue de SF-236-01)

À finaliser après livraison de SF-236-01. Estimation initiale :
- **Vague Travail** : ~25 composants — ~12 h — branche `feat/SF-236-02-travail`
- **Vague Immigration** : ~14 composants — ~7 h — branche `feat/SF-236-02-immigration`
- **Vague Famille** : ~15 composants — ~7 h — branche `feat/SF-236-02-famille`

### Coordination merge

Les 3 PRs sont mergées dans n'importe quel ordre. Aucun fichier partagé entre les vagues (chaque domaine a ses propres composants). Le `decisional-tools-panel.component.ts` n'est touché par aucune vague (le panel consomme déjà `getPrefillCount` quand il existe).

Une 4ᵉ PR de finalisation pourra être nécessaire si les 4 composants déjà conformes (F-FA-05/07, F-IM-05/07) doivent être refactorés pour adopter le helper — décision prise dans SF-236-01.

### Pattern de référence

Voir `docs/features/F-236/contract-prefill-rules.md` (livrable SF-236-01) — exemple canonique sur F-IM-05.
