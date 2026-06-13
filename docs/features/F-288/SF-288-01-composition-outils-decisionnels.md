# Mini-spec — F-288 / SF-288-01 — Écran de composition : dimension « Outils décisionnels »

> Étape 1 du cycle. Cadrages amont : `SF-288-00-coherence.md` (GO) + `SF-288-00b-ux-coherence.md` (GO avec ajustements).

## Identifiant
`F-288 / SF-288-01`

## Feature parente
`F-288` — Conclusions : écran de composition avant génération (outils, chefs de demande, moyens adverses).

## Statut
`draft`

## Date de création
2026-06-13

## Branche Git
`feat/SF-288-01-composition-outils`

---

## Objectif

> Au clic « Générer »/« Régénérer », ouvrir un **modal de composition** où l'avocat **décoche les outils décisionnels calculés** qu'il ne veut pas verser dans l'acte ; le choix est **durable** (survit aux régénérations) et **filtre réellement** le prompt de génération.

---

## Comportement attendu

### Cas nominal

1. L'avocat est sur la page Conclusions (`/case-files/:id/conclusions`), état `NOT_GENERATED` (ou `GENERATED` stale).
2. Il clique **« Générer le projet de conclusions »** (ou **« Régénérer »**).
3. Le front appelle `GET …/conclusions/composition`. **Si 0 outil calculé** → pas de modal, on lance la génération directement (comportement actuel). **Sinon** → ouverture du **modal de composition**.
4. Le modal affiche la section **« Outils décisionnels »** : une ligne par outil calculé (libellé court), **case cochée** par défaut, **décochée** si l'outil est dans l'ensemble d'exclusions persisté. Actions « Tout cocher / Tout décocher ».
5. L'avocat ajuste, puis clique **« Confirmer & générer »**.
6. Le front envoie `PUT …/conclusions/composition` (l'ensemble des outils **exclus**), **puis** appelle `POST …/conclusions/generate?fromScratch=…` (sémantique inchangée).
7. Le worker (`CaseConclusionService.prepare`) **lit l'ensemble d'exclusions** et **filtre** : les outils exclus ne sont injectés ni dans les **verdicts d'outils** (`loadDecisionToolTiles`) ni dans la **jurisprudence dérivée de ces outils** (chemin `ToolUsageAggregator` → `ConclusionsJurisprudenceContext`). L'acte se génère sans eux.
8. À la **régénération** suivante, le modal se ré-ouvre **pré-coché selon l'ensemble persisté** (jamais de reset).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|---|---|---|
| Dossier inexistant (GET/PUT) | Erreur ressource | 404 |
| Dossier d'un autre workspace (GET/PUT) | **404 non-leak** (cohérent avec la famille `/conclusions`) | 404 |
| Body `PUT` malformé (dimension inconnue) | Message explicite | 400 |
| `PUT` avec un `itemKey` (toolId) inconnu / non calculé | Toléré (no-op à la génération, fail-open) | 200 |
| Échec de lecture des exclusions au worker | Fail-open : génération sans filtre (= comportement actuel), journalisé | — |
| Tous les outils décochés | Génération autorisée, avertissement non bloquant dans le modal | 200 |

---

## Analyse de cohérence transversale

### Résultat du scan

| Cible | Applicable ? | Traitement |
|---|---|---|
| Dimension **chefs de demande** | Oui | **Vague 2 (SF-288-02)** — réutilise la table + le modal de cette SF (nouvelle dimension `HEAD_OF_CLAIM`, source `heads-of-claim` F-262) |
| Dimension **moyens adverses** | Oui | **Vague 3 (SF-288-03)** — nouvelle dimension `ADVERSE_MOYEN`, source F-261 |
| 5 ingrédients déjà curés (pièces, stratégies, jurisprudence d'appui/adverse, synthèse) | Non | Hors périmètre (déjà pilotés ailleurs — cf. étape 0). |
| Autres domaines (immigration / famille) / pays (FR / BE) | Oui, **automatiquement** | Le mécanisme est **agnostique au domaine** : il filtre « les outils calculés », quels qu'ils soient. Aucune déclinaison par domaine requise. |
| Génération from-scratch vs actualisation (F-271) | Oui | L'ensemble d'exclusions s'applique aux **deux** modes (`fromScratch` true/false) — testé. |

### Décision
- [x] Étendu à toutes les cibles applicables **dans la limite de la vague 1** (outils, tous domaines/pays).
- [x] Subfeatures parallèles créées pour les cibles restantes : SF-288-02 (chefs), SF-288-03 (moyens) — déjà tracées au PRODUCT_SPEC F-288.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : le composant livré est un **modal de composition** (`conclusion-composition-dialog`), **pas** un `<app-XXX-section>` outil décisionnel consommant un endpoint POST décisionnel ni intégré au `TOOL_REGISTRY` du panel F-IA-04. Aucun champ saisissable, aucun calcul, aucun pré-remplissage IA. Il **lit** la liste des outils calculés et persiste un ensemble d'exclusions.

## Champs IA à extraire (pré-remplissage)
- [x] **Aucun pré-remplissage** — justification : pas d'outil décisionnel à champs saisissables ; cette SF ne crée aucun formulaire métier.

---

## Critères d'acceptation

- [ ] **C1 — Modal au déclenchement** : cliquer « Générer » (état `NOT_GENERATED`) ou « Régénérer » (stale) ouvre le modal de composition listant les outils calculés, tous cochés par défaut.
- [ ] **C2 — 0 outil → pas de friction** : si le dossier n'a aucun outil calculé, aucun modal, la génération se lance directement (comportement actuel préservé).
- [ ] **C3 — Filtre réel sur le prompt** : un outil **décoché** puis « Confirmer & générer » → son verdict **n'apparaît pas** dans le prompt de génération (test backend sur `prepare()` : tile absente).
- [ ] **C4 — Cohérence jurisprudence** : la jurisprudence dérivée d'un outil exclu (chemin `ToolUsageAggregator`) est également absente du prompt.
- [ ] **C5 — Durable / régénération** : après une exclusion, une **régénération** ré-ouvre le modal **pré-coché** selon l'ensemble persisté, et la génération respecte l'exclusion **sans re-saisie**.
- [ ] **C6 — Non-régressif** : ensemble d'exclusions vide (cas par défaut, dossier existant) → **tous** les outils calculés sont injectés, identique au comportement actuel.
- [ ] **C7 — Tout décoché** : génération autorisée, avertissement non bloquant affiché dans le modal.
- [ ] **C8 — Isolation workspace** : un utilisateur du workspace A ne peut ni lire ni écrire la composition d'un dossier du workspace B (**404 non-leak**, cohérent avec la famille `/conclusions`).
- [ ] **C9 — Distinct de F-258** : l'encart « outils non calculés » (F-258) reste affiché en amont dans l'état `NOT_GENERATED`, inchangé ; le modal agit sur les **calculés**.

---

## Périmètre

### Hors scope (explicite)
- Dimensions **chefs de demande** (SF-288-02) et **moyens adverses** (SF-288-03).
- Les 5 ingrédients déjà curés ailleurs (pièces, stratégies RETAINED, jurisprudence d'appui F-242, jurisprudence adverse SF-98-56, synthèse).
- Pré-remplissage IA, calcul d'outil, modification de `decision_tool_visibility_rules` (aucun outil n'est masqué/déplacé — invariant « 1 outil = 1 situation »).
- Rafraîchissement temps réel de la liste pendant que le modal est ouvert.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|---|---|---|
| (ensemble d'exclusions) | **vide** | À la création d'un dossier, aucune exclusion → tous les outils calculés sont versés (non-régressif). |

Comportements : `created_at` auto ; rattachement workspace **via `case_file_id`** (le dossier porte le workspace — même modèle d'accès que les autres données de conclusions).

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Valeurs autorisées | Unicité | Normalisation |
|---|---|---|---|---|---|
| `dimension` | Oui | 40 | `DECISION_TOOL` (vague 1) | — | upper |
| `item_key` | Oui | 120 | toolId (texte) | unique avec (case_file_id, dimension) | trim |

---

## Technique

### Endpoint(s)
Base existante : `/api/v1/case-files/{caseFileId}/conclusions`.

| Méthode | URL | Auth | Rôle min | Rôle |
|---|---|---|---|---|
| GET | `…/conclusions/composition` | Oui | MEMBER | Liste des dimensions curables + items (`{key,label,included}`). Vague 1 : dimension `DECISION_TOOL`, items = outils **calculés** (`assembleDecisionToolTiles` → `toolId`+`title`), `included = !exclu`. |
| PUT | `…/conclusions/composition` | Oui | LAWYER | Remplace l'ensemble d'exclusions des dimensions fournies. Body `{ exclusions: [{ dimension, itemKey }] }`. |

> Pas de changement au contrat de `POST …/generate` : le worker lit la composition persistée. Le front appelle PUT **avant** generate.

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `conclusion_composition_exclusion` | CREATE / SELECT / DELETE+INSERT | **Nouvelle.** `(id, case_file_id, dimension, item_key, created_at)`, unique `(case_file_id, dimension, item_key)`, index `case_file_id`. Stocke uniquement les **exclus**. |

### Migration Liquibase
- [x] Oui — `601-create-conclusion-composition-exclusion.xml` (createTable + uniqueConstraint + index ; pas de FK dure si la convention du repo l'évite, sinon FK `case_file_id`).

### Composants Angular
- `conclusion-composition-dialog` (nouveau, `case-files/conclusions-section/…` ou `shared/`) — modal MatDialog : sections par dimension, cases à cocher, « Tout cocher/décocher », « Annuler » / « Confirmer & générer », avertissement « tout décoché ».
- `conclusions-section` (modifié) — **interception** des actions Générer/Régénérer : `GET composition` → 0 item ⇒ generate direct ; sinon ouvrir le dialog ; sur confirm ⇒ `PUT composition` puis generate. Service `ConclusionCompositionService` (GET/PUT).

### Backend
- `ConclusionCompositionService` (lecture liste curable + persistance exclusions, isolation workspace via résolution du dossier comme les autres services conclusions).
- `CaseConclusionService.prepare` (worker) : charger l'ensemble d'exclusions `DECISION_TOOL`, filtrer `loadDecisionToolTiles` (drop toolId exclu) **et** le chemin jurisprudence dérivée d'outils (filtrer les `ToolUsage` dont le toolId est exclu, dans `ConclusionsJurisprudenceContext` / `ToolUsageAggregator` côté conclusions).

---

## Plan de test

### Tests unitaires (backend)
- [ ] `ConclusionCompositionService` — PUT puis GET : l'item exclu revient `included=false`, les autres `true`.
- [ ] GET sans exclusion → tous `included=true` (non-régressif).
- [ ] `prepare()` — un toolId exclu n'apparaît pas dans les tiles injectées (C3).
- [ ] `prepare()` — la jurisprudence dérivée du toolId exclu est filtrée (C4).
- [ ] `prepare()` — exclusions vides → ensemble de tiles identique à l'existant (C6).
- [ ] Fail-open : exception de lecture des exclusions → génération sans filtre, journalisée.

### Tests d'intégration (backend)
- [ ] `GET …/composition` → 200, liste des outils calculés.
- [ ] `PUT …/composition` → 200, persistance vérifiée.
- [ ] `PUT` body malformé (dimension inconnue) → 400.
- [ ] `GET`/`PUT` workspace différent → 404 non-leak (C8).
- [ ] Régénération après exclusion : l'exclusion est toujours appliquée (C5), modes `fromScratch` true/false.

### Tests frontend (Jest)
- [ ] Générer avec items>0 → ouvre le dialog (C1).
- [ ] Générer avec 0 item → pas de dialog, generate direct (C2).
- [ ] Décocher un outil → payload PUT contient l'exclusion ; « Confirmer & générer » appelle generate (C3 côté contrat).
- [ ] Ré-ouverture pré-cochée selon GET (C5).
- [ ] Tout décoché → avertissement affiché, bouton « Confirmer & générer » actif (C7).

### Isolation workspace
- [x] Applicable — un utilisateur du workspace A ne peut accéder à la composition d'un dossier du workspace B (404 non-leak).

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Aucune préoccupation transversale** — pas de changement Auth/Principal, pas de nouvelle résolution de workspace (réutilise l'accès dossier existant), pas de plan/quota, **pas de nouvelle route** (modal par-dessus `/conclusions`). Le seul point sensible = isolation workspace, couverte par C8.

### Smoke tests E2E concernés
- [ ] Aucun smoke E2E bloquant (pas de changement auth/workspace/navigation). Vérifier néanmoins la non-régression de la génération de conclusions si un smoke « conclusions » existe.

---

## Dépendances

### Subfeatures bloquantes
- Aucune (s'appuie sur l'existant : `assembleDecisionToolTiles`, `CaseConclusionService.prepare`, page conclusions F-267, génération F-98/F-287).

### Questions ouvertes impactées
- [ ] Aucune entrée `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

- **Stocker les exclus, pas les inclus** : garantit le non-régressif (table vide = comportement actuel) et évite d'avoir à initialiser une ligne par outil.
- **Filtrage au worker, pas à l'écriture** : la composition est une **préférence** ; la génération l'applique à la lecture. Un outil re-calculé plus tard reste exclu tant qu'il l'est (cohérent avec « durable »).
- **Cohérence outil↔jurisprudence** : exclure un outil retire **aussi** sa jurisprudence dérivée (C4) — sinon l'acte citerait la jurisprudence d'un argument non plaidé (incohérent). La jurisprudence d'appui F-242 (par point juridique, déjà curée) **n'est pas** touchée.
- **Table générique** (`dimension` + `item_key`) **dès la vague 1** pour accueillir `HEAD_OF_CLAIM` (vague 2) et `ADVERSE_MOYEN` (vague 3) sans migration supplémentaire.
