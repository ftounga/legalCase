# Mini-spec — F-261 / SF-261-05 — Persistance des moyens adverses extraits

> Pré-requis de F-288 vague 3 (curation des moyens) — cf. `docs/features/F-288/SF-288-03-00-coherence.md`.
> SF backend de refactor + nouvelle table : étapes 0/0bis non applicables (aucun workflow ni écran nouveau ; comportement de génération préservé).

## Identifiant
`F-261 / SF-261-05`

## Feature parente
`F-261` — Conclusions en réponse (réfutation des moyens adverses). Item backlog « persistance/affichage des moyens extraits ».

## Statut
`draft`

## Branche Git
`feat/SF-261-05-persistance-moyens-adverses`

---

## Objectif

> Persister les moyens adverses extraits (entité + table, **ID stable**) et faire lire à la génération le **set persisté** au lieu de ré-extraire à chaque fois — pour donner un identifiant stable réutilisable (curation F-288 vague 3) et garantir que modal et génération lisent les **mêmes** moyens.

---

## Comportement attendu

### Cas nominal
1. Aujourd'hui : `CaseConclusionService.prepare` appelle `loadAdverseMoyens` qui **ré-extrait** par LLM (`AdverseMoyensExtractor`) à chaque génération (record éphémère, pas d'ID).
2. Après : un service `AdverseMoyenPersistenceService.loadOrExtract(caseFileId, domain, country)` :
   - si un **set persisté existe** pour le dossier → le retourne (aucun appel LLM) ;
   - sinon, si des documents « écritures adverses » avec texte existent → **extrait (LLM) puis persiste** (replace set : `delete` + `insert` ordonné) → retourne le set avec **ID stable** ;
   - aucun document adverse → liste vide (aucun appel LLM).
3. `prepare` consomme ce set persisté (mappé vers l'intrant `AdverseMoyen` du prompt, **inchangé** côté contenu).

### Cas d'erreur
| Situation | Comportement | 
|---|---|
| Aucun document adverse | Liste vide, pas d'appel LLM |
| Échec extraction / persistance | **Fail-open** : génération sans section moyens, journalisé (jamais propagé) |
| Set persisté présent mais nouvelle écriture adverse ajoutée depuis | **Limite MVP assumée** : pas d'auto-refresh ; ré-extraction sur déclencheur explicite (`refresh`) ou si set absent. Tracé. |

---

## Critères d'acceptation
- [ ] **C1** — Une table `adverse_moyen` persiste chaque moyen (these, fondements, pièces, ordre) avec un **ID stable** par dossier.
- [ ] **C2** — 1ʳᵉ génération avec écritures adverses : extraction LLM **+ persistance** ; le set persisté correspond aux moyens injectés au prompt.
- [ ] **C3** — Génération suivante (set présent) : **aucun nouvel appel LLM** d'extraction ; mêmes moyens (mêmes ID).
- [ ] **C4** — Contenu du prompt « MOYENS ADVERSES À RÉFUTER » **identique** à l'existant pour un même jeu de moyens (non-régression F-261).
- [ ] **C5** — Aucun document adverse → 0 ligne, 0 appel LLM.
- [ ] **C6** — Fail-open : échec extraction/persistance → génération sans section, pas d'exception propagée.
- [ ] **C7** — Isolation : moyens rattachés au dossier (workspace via `case_file_id`).

## Périmètre
### Hors scope
- La **curation** (sélection/exclusion) des moyens = SF-288-03.
- L'**affichage** des moyens dans une UI dédiée (hors modal de composition).
- L'auto-refresh du set à l'ajout d'une écriture adverse (limite MVP).
- Domaines : l'extraction reste celle de l'existant (travail FR principal ; im/fa selon `AdverseMoyensExtractor`). Pas de changement de prompt.

## Technique
### Tables impactées
| Table | Opération | Notes |
|---|---|---|
| `adverse_moyen` | CREATE / SELECT / DELETE+INSERT | **Nouvelle.** `id` uuid PK, `case_file_id` uuid NOT NULL (FK CASCADE), `these` text NOT NULL, `fondements` text (JSON array), `pieces_invoquees` text (JSON array), `ordre` int, `created_at` timestamptz. Index `case_file_id`. |

### Migration Liquibase
- [x] `606-create-adverse-moyen.xml` (la dernière est 605).

### Backend
- Entité `AdverseMoyenEntity` + `AdverseMoyenRepository` (`findByCaseFileIdOrderByOrdreAsc`, `existsByCaseFileId`, `deleteByCaseFileId`).
- `AdverseMoyenPersistenceService` : `loadOrExtract(caseFileId, domain, country)` (+ `refresh(...)` pour le futur). Réutilise `AdverseMoyensExtractor` (gating `AiCallContext` inchangé). Mapping entité ⇄ record `AdverseMoyen` (le record reste l'intrant du prompt ; on lui adjoint un id via un nouveau type/porteur si nécessaire pour la curation — sinon la curation lira le repo directement).
- `CaseConclusionService.prepare` : remplacer `loadAdverseMoyens(...)` par `adverseMoyenPersistenceService.loadOrExtract(...)`. Conserver le mapping vers l'intrant prompt **inchangé**.

## Plan de test
- [ ] `AdverseMoyenPersistenceService` — extraction+persistance au 1ᵉʳ appel ; 2ᵉ appel ne ré-extrait pas (vérifier 0 interaction extracteur via mock).
- [ ] Aucun doc adverse → vide, extracteur jamais appelé.
- [ ] Fail-open : extracteur lève → liste vide, pas d'exception.
- [ ] `prepare` consomme le set persisté (intrant prompt inchangé pour un même jeu).
- [ ] Repository : ordre stable, delete+insert idempotent.
- [ ] Isolation workspace via case_file_id (les moyens d'un dossier ne fuient pas).

## Analyse d'impact
- [x] **Aucune préoccupation transversale** (pas d'auth/route/plan ; backend interne au pipeline conclusions). Changement de comportement = la génération lit le persisté (couvert par C3/C4).

## Notes
- **Replace-set** (pas d'upsert fin) : simple et suffisant. Les ID changent uniquement sur `refresh` explicite → exclusions F-288-03 stables entre générations.
- Le record `AdverseMoyen` (intrant prompt) reste ; pour exposer l'`id` à la curation, soit on enrichit le porteur, soit la curation lit `AdverseMoyenRepository`. Tranché en dev (préférence : la curation lit le repo, le prompt garde le record nu).
