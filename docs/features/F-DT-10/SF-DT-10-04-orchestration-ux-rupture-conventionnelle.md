# Mini-spec — F-DT-10 / SF-DT-10-04 Orchestration UX : masquage F-DT-08 / affichage F-DT-10 selon type de rupture

## Identifiant

`F-DT-10 / SF-DT-10-04`

## Feature parente

`F-DT-10` — Analyse de validité de la rupture conventionnelle

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-DT-10-04-orchestration-ux-rupture-conventionnelle`

---

## Objectif

Masquer le bloc **F-DT-08 Validité du licenciement** quand le type de rupture détecté par l'IA n'est pas un licenciement, et afficher à la place **F-DT-10 Validité de la rupture conventionnelle** quand le type est `RUPTURE_CONVENTIONNELLE`. Mettre à jour le plan de test (Test 2 Martin BTP) pour refléter la nouvelle UX.

Résout la friction UX observée sur staging : le bloc "VALIDITÉ DU LICENCIEMENT" s'affichait sur un dossier de rupture conventionnelle avec des critères non applicables.

---

## Comportement attendu

### Cas nominal

Dans `case-file-detail.component.html`, la section droit du travail affiche :

1. **F-DT-08 `<app-licenciement-section>`** — visible **uniquement** si `compensation_data.type_rupture` ∈ licenciement-types **ou** inconnu (défaut permissif) :
   - Types licenciement FR : `LICENCIEMENT`, `LICENCIEMENT_ECONOMIQUE`
   - Types licenciement BE : `LICENCIEMENT_ORDINAIRE`, `LICENCIEMENT_MANIFESTEMENT_DERAISONNABLE`
   - Type inconnu (pas d'IA, pas de `compensationEstimate`) : **visible** par défaut (rétro-compatibilité, dossiers existants)
2. **F-DT-10 `<app-rupture-conv-section>`** — visible **uniquement** si `compensation_data.type_rupture === 'RUPTURE_CONVENTIONNELLE'` **ET** workspace country = `FRANCE` (F-DT-10 FR-only).
3. **Autres types** (`DEMISSION`, `PRISE_ACTE`, `RESILIATION_JUDICIAIRE`, `RUPTURE_AMIABLE`) : les deux blocs sont masqués. Pas de bloc de remplacement — l'outil d'analyse n'existe pas encore pour ces cas.

### Matrice de visibilité

| `type_rupture` | `country` | F-DT-08 | F-DT-10 |
|----------------|-----------|---------|---------|
| `LICENCIEMENT` | FRANCE | ✅ | ❌ |
| `LICENCIEMENT_ECONOMIQUE` | FRANCE | ✅ | ❌ |
| `RUPTURE_CONVENTIONNELLE` | FRANCE | ❌ | ✅ |
| `DEMISSION` / `PRISE_ACTE` / `RESILIATION_JUDICIAIRE` | FRANCE | ❌ | ❌ |
| `LICENCIEMENT_ORDINAIRE` / `LICENCIEMENT_MANIFESTEMENT_DERAISONNABLE` | BELGIQUE | ✅ | ❌ |
| `RUPTURE_AMIABLE` | BELGIQUE | ❌ | ❌ |
| `null` / `undefined` (IA sans extraction ou dossier legacy) | quelconque | ✅ | ❌ |

### Implémentation

Dans `CaseFileDetailComponent` :
- Nouveau `computed` signal `licenciementTypes = computed<Set<string>>()` (immutable, défini au niveau classe).
- Nouveau `computed` signal `showValiditeLicenciement = computed(() => …)` qui retourne `true` si `type_rupture` est dans l'enum licenciement **ou** null/undefined.
- Nouveau `computed` signal `showValiditeRuptureConv = computed(() => …)` qui retourne `true` si `type_rupture === 'RUPTURE_CONVENTIONNELLE'` **ET** workspaceCountry = `FRANCE`.
- Dans le template, `@if` sur chacun de ces signals, en complément du `@if (caseFile()!.legalDomain === 'DROIT_DU_TRAVAIL')` existant.
- Le composant `RuptureConvSectionComponent` est ajouté dans `imports: []`.

### Mise à jour Test 2

Le fichier `test-data/TEST_PLAN.md` Test 2 (Martin, BTP, rupture conventionnelle FR) est réécrit pour :
1. Retirer la mention du bloc "Validité du licenciement" (n'apparaîtra plus).
2. Ajouter le bloc **Validité de la rupture conventionnelle (F-DT-10)** avec ses 6 critères : cocher tous `OUI` sauf `RC_INDEMNITE = NON` → vérifier verdict `INVALIDE` (bloquant), score ≥ 15.
3. Vérifier que la card "Validité du licenciement" du dashboard F-IA-02 ne s'affiche pas (ou reste vide) pour ce dossier.

### Cas d'erreur

| Situation | Comportement |
|-----------|--------------|
| `synthesis()` est null (analyse pas encore faite) | F-DT-08 visible (défaut permissif, rétro-compat) |
| `compensationEstimate` est null dans synthesis | F-DT-08 visible, F-DT-10 masqué |
| `type_rupture` est une valeur inconnue hors de tous les enum | F-DT-08 visible par défaut, F-DT-10 masqué |
| Workspace BE + `type_rupture = RUPTURE_CONVENTIONNELLE` (impossible théoriquement) | F-DT-08 masqué, F-DT-10 masqué (car FR only) — cas exotique |

---

## Critères d'acceptation

- [ ] `CaseFileDetailComponent` déclare `showValiditeLicenciement` et `showValiditeRuptureConv` en `computed` signals.
- [ ] Template HTML : `@if (showValiditeLicenciement())` autour de `<app-licenciement-section>`, `@if (showValiditeRuptureConv())` autour de `<app-rupture-conv-section>`.
- [ ] `RuptureConvSectionComponent` ajouté aux imports du `CaseFileDetailComponent`.
- [ ] Aucune autre modification du template (ordres, autres sections).
- [ ] Tests Jest unitaires sur les computed (rétro-compat null, 4 licenciement types, RUPTURE_CONVENTIONNELLE FR, DEMISSION, RUPTURE_AMIABLE BE).
- [ ] `test-data/TEST_PLAN.md` Test 2 mis à jour : étapes F-DT-10 ajoutées, mention F-DT-08 retirée, vérification visuelle de non-affichage.
- [ ] 924+ tests frontend verts (régression zéro).
- [ ] Build Angular OK.

---

## Périmètre

### Hors scope (explicite)

- Affichage d'un message neutre ("Cet outil ne s'applique pas à ce type de rupture") à la place de F-DT-08 lorsqu'il est masqué — décidé non : masquage simple sans placeholder pour ne pas encombrer la page.
- Pré-remplissage IA des critères F-DT-10 (hors scope V2 initial).
- Card F-IA-02 "Rupture conventionnelle" dans le dashboard (backlog).
- Migration de dossiers historiques pour re-catégoriser `type_rupture`.
- Ordre des blocs (F-DT-08 et F-DT-10 sont mutuellement exclusifs, placés au même endroit).

---

## Valeurs initiales

Sans objet — uniquement conditionnels d'affichage.

---

## Contraintes de validation

Sans objet — pas de saisie utilisateur ici.

---

## Technique

### Endpoints

Aucun.

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

### Composants Angular

- `CaseFileDetailComponent` :
  - Imports : ajout `RuptureConvSectionComponent`.
  - Nouveaux `computed` signals `showValiditeLicenciement`, `showValiditeRuptureConv`.
  - Constante privée `LICENCIEMENT_TYPES = new Set(['LICENCIEMENT', 'LICENCIEMENT_ECONOMIQUE', 'LICENCIEMENT_ORDINAIRE', 'LICENCIEMENT_MANIFESTEMENT_DERAISONNABLE'])`.
- Template `case-file-detail.component.html` : 2 `@if` ajoutés, sans réécriture.
- Tests : nouveau spec ou extension de l'existant sur `CaseFileDetailComponent`, ciblant les computed.

### Design system

- Aucun impact — masquage = absence d'affichage, pas de nouvelle UI.

### Refresh dashboard

- Aucun impact direct — F-DT-10 (monté par cette SF) branchera lui-même son refresh (déjà fait en SF-DT-10-03).

---

## Plan de test

### Tests unitaires Jest

- [ ] `CaseFileDetailComponent` : `showValiditeLicenciement` = true si synthesis null (legacy).
- [ ] `CaseFileDetailComponent` : `showValiditeLicenciement` = true pour `type_rupture = LICENCIEMENT`.
- [ ] `CaseFileDetailComponent` : `showValiditeLicenciement` = true pour `type_rupture = LICENCIEMENT_ORDINAIRE` (BE).
- [ ] `CaseFileDetailComponent` : `showValiditeLicenciement` = false pour `type_rupture = RUPTURE_CONVENTIONNELLE`.
- [ ] `CaseFileDetailComponent` : `showValiditeLicenciement` = false pour `type_rupture = DEMISSION`.
- [ ] `CaseFileDetailComponent` : `showValiditeRuptureConv` = true pour `RUPTURE_CONVENTIONNELLE` + FRANCE.
- [ ] `CaseFileDetailComponent` : `showValiditeRuptureConv` = false pour `RUPTURE_CONVENTIONNELLE` + BELGIQUE (théorique).
- [ ] `CaseFileDetailComponent` : `showValiditeRuptureConv` = false pour `LICENCIEMENT`.

### Tests d'intégration

- [ ] Non applicable (frontend pur, pas d'endpoint touché).

### Validation manuelle

- [ ] Staging, dossier Martin (rupture conventionnelle) : F-DT-10 visible, F-DT-08 masqué.
- [ ] Staging, dossier Dupont (licenciement) : F-DT-08 visible, F-DT-10 masqué.

### Isolation workspace

- [x] N/A — logique frontend pure, pas d'accès données.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [x] Navigation / routing frontend — **indirect** : le contenu de la page fiche dossier change selon `type_rupture`, pas le routing lui-même. Pas de guard impacté, pas de route modifiée.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|-----------|--------|-----------------------|
| `CaseFileDetailComponent` | Ajout imports + 2 computed + 2 `@if` | Specs existants restent verts |
| `LicenciementSectionComponent` | Aucun changement interne — juste conditionné par le parent | Tests existants |
| `RuptureConvSectionComponent` | Consommé pour la première fois | Tests SF-DT-10-03 |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/navigation.spec.ts` — la fiche dossier doit rester navigable sans régression. À relancer.

---

## Dépendances

### Subfeatures bloquantes

- `SF-DT-10-01` Done — backend référentiel/analyzer.
- `SF-DT-10-02` Done — endpoints.
- `SF-DT-10-03` Done — composant Angular.
- `SF-DT-09-05` Done — garantit que `compensation_data.type_rupture` est peuplé de manière fiable (sinon le masquage serait erratique).

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi défaut permissif sur F-DT-08** : les dossiers historiques ou en cours d'analyse n'ont pas encore `compensationEstimate.typeRupture`. Masquer F-DT-08 dans ces cas serait une régression. On garde la visibilité par défaut, le masquage ne se déclenche que si l'IA a formellement identifié un non-licenciement.
- **Pourquoi masquage simple sans message neutre** : encombrer la page avec un bloc "cet outil ne s'applique pas" ajoute du bruit. L'absence est la meilleure UX quand le dossier a un type clair.
- **Pourquoi F-DT-10 FR only** : la rupture amiable belge n'a pas les mêmes critères juridiques (pas d'homologation, pas de délai de rétractation légal). Séparer si et quand le besoin sera confirmé.
- **Pourquoi pas d'ajout dans F-IA-02 dashboard** : hors scope (backlog). Cette SF se concentre sur la correction UX immédiate pour le test 2.
- **Pourquoi combiner TEST_PLAN.md dans la même subfeature** : le plan de test est indissociable du changement UX. Le mettre dans la même PR évite un fichier orphelin.
