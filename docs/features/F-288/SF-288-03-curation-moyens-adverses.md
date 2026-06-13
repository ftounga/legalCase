# Mini-spec — F-288 / SF-288-03 — Composition : dimension « Moyens adverses »

> Vague 3 de F-288. Cadrages : `SF-288-00-coherence.md`, `SF-288-00b-ux-coherence.md`, `SF-288-03-00-coherence.md` (GO via pré-requis).
> **Dépend de SF-261-05** (persistance des moyens — ID stable). Réutilise la table générique `conclusion_composition_exclusion` et le modal `conclusion-composition-dialog` de SF-288-01.

## Identifiant
`F-288 / SF-288-03`

## Feature parente
`F-288` — Écran de composition des conclusions avant génération.

## Statut
`draft`

## Branche Git
`feat/SF-288-03-curation-moyens-adverses`

---

## Objectif
> Ajouter au modal de composition une dimension **« Moyens adverses »** : l'avocat décoche les moyens adverses qu'il **ne souhaite pas réfuter** ; le choix est **durable** et filtre la section « MOYENS ADVERSES À RÉFUTER » du prompt.

## Comportement attendu
### Cas nominal
1. Au clic « Générer »/« Régénérer », le modal de composition (SF-288-01) charge `GET …/conclusions/composition`.
2. La réponse contient **2 dimensions** quand des moyens **déjà persistés** existent : `DECISION_TOOL` (vague 1) **et** `ADVERSE_MOYEN` (items = moyens persistés SF-261-05 lus en **lecture seule** via `findPersisted` ; `key` = id du moyen ; `label` = thèse tronquée ; `included = !exclu`). **Aucune extraction LLM synchrone dans le GET** (invariant « aucune IA synchrone ») : les moyens sont extraits/persistés par le **worker de génération** (`prepare()`), donc la dimension `ADVERSE_MOYEN` **apparaît dès qu'une première génération a figé le set** (omise avant).
3. L'avocat décoche des moyens, « Confirmer & générer » → `PUT` (exclusions des 2 dimensions) puis génération.
4. `CaseConclusionService.prepare` **filtre** les moyens exclus (`ADVERSE_MOYEN`) avant injection dans la section « MOYENS ADVERSES À RÉFUTER ».

### Cas d'erreur
| Situation | Comportement | Code |
|---|---|---|
| Aucun moyen persisté (0 doc adverse) | Dimension `ADVERSE_MOYEN` **absente** de la réponse (modal n'affiche que les outils, ou rien si 0 outil aussi) | 200 |
| `itemKey` (moyen id) inconnu | Toléré, no-op | 200 |
| Workspace différent | 404 non-leak (comme la famille) | 404 |
| Tous les moyens décochés | Génération sans section « moyens à réfuter », non bloquant | 200 |

## Critères d'acceptation
- [ ] **C1** — Quand des moyens persistés existent, `GET composition` renvoie la dimension `ADVERSE_MOYEN` (items = moyens, included par défaut).
- [ ] **C2** — Décocher un moyen + générer → ce moyen **n'apparaît pas** dans la section « MOYENS ADVERSES À RÉFUTER » du prompt (test `prepare`).
- [ ] **C3** — Durable : à la régénération, le modal pré-coche selon les exclusions persistées (l'`id` du moyen est stable, cf. SF-261-05).
- [ ] **C4** — Non-régressif : aucune exclusion `ADVERSE_MOYEN` → tous les moyens persistés injectés (= comportement SF-261-05).
- [ ] **C5** — 0 moyen persisté → pas de dimension `ADVERSE_MOYEN` (pas de section vide dans le modal).
- [ ] **C6** — Isolation workspace (404 non-leak) — hérité de SF-288-01.
- [ ] **C7** — Le modal affiche correctement **2 dimensions** (outils + moyens) sans régression de la vague 1.

## Périmètre
### Hors scope
- La persistance/extraction des moyens (= SF-261-05, pré-requis).
- L'auto-refresh du set de moyens (limite MVP SF-261-05).
- Les dimensions chefs de demande (vague 2 — STOP/gadget).

## Technique
### Endpoints
Aucune nouvelle route. `GET`/`PUT …/conclusions/composition` (SF-288-01) **étendus** : la dimension `ADVERSE_MOYEN` est ajoutée à la réponse GET ; le PUT accepte `dimension="ADVERSE_MOYEN"`.

### Tables impactées
| Table | Opération | Notes |
|---|---|---|
| `conclusion_composition_exclusion` | INSERT/DELETE/SELECT | **Existante** (SF-288-01). Nouveau `dimension="ADVERSE_MOYEN"`, `item_key` = id du moyen persisté. **Aucune migration.** |
| `adverse_moyen` | SELECT | Lecture (SF-261-05). |

### Migration Liquibase
- [x] **Non applicable** (table générique déjà créée en SF-288-01).

### Backend
- `ConclusionCompositionService.getComposition` : ajouter la dimension `ADVERSE_MOYEN` (items depuis `AdverseMoyenPersistenceService.findPersisted` — **lecture seule, pas d'extraction LLM**, label = thèse tronquée). Si 0 moyen persisté → dimension omise. `getComposition` reste `@Transactional(readOnly = true)`.
- `putComposition` : autoriser `ADVERSE_MOYEN` (lever la validation « DECISION_TOOL only »).
- `CaseConclusionService.prepare` : après chargement des moyens persistés (SF-261-05), **filtrer** ceux dont l'id est exclu (`ADVERSE_MOYEN`), avant injection.

### Frontend
- `conclusion-composition-dialog` : **vérifier** qu'il rend déjà N dimensions (construit générique en SF-288-01). Si oui, **aucun changement** sauf tests. Sinon, généraliser le rendu par dimension.
- Aucun changement du flux d'interception (`conclusions-section`).

## Plan de test
- [ ] Backend service : GET renvoie 2 dimensions quand moyens présents ; ADVERSE_MOYEN omise si 0 moyen.
- [ ] `putComposition` accepte ADVERSE_MOYEN ; persistance vérifiée.
- [ ] `prepare` : moyen exclu absent de la section ; exclusions vides = non-régression (C4).
- [ ] IT : GET/PUT 200 avec ADVERSE_MOYEN ; workspace 404.
- [ ] Frontend Jest : modal rend 2 sections ; décocher un moyen → payload PUT ; régression vague 1 nulle.

## Analyse d'impact
- [x] **Aucune préoccupation transversale** (réutilise endpoints/route/table existants ; isolation héritée). Dépend de SF-261-05 (mergée d'abord).

## Dépendances
- **SF-261-05** (persistance des moyens) — **bloquante**, à merger avant.
