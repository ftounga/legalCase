# Mini-spec — F-220 / SF-220-04 — Outil PACS VPF (CESEDA L.423-23)

## Identifiant
`F-220 / SF-220-04` — tool_id `F-IM-50-pacs-vpf-fr` (Immigration FR)
- slug : `pacs-vpf`
- statut : `ready`
- date : 2026-06-03
- branche Git : `feat/SF-220-04-pacs-vpf`

## Objectif (1 phrase)
Évaluer l'éligibilité à la carte « vie privée et familiale » (L.423-23) au titre d'un PACS conclu en France, selon les critères jurisprudentiels propres (ancienneté ~1 an + intensité et stabilité de la communauté de vie), distincts du mariage.

## Périmètre / anti-doublon
Distinct de `F-IM-21` (conjoint **marié** de Français) : le PACS n'ouvre pas automatiquement de droit au séjour, il est un **élément** d'appréciation de la vie privée et familiale (L.423-23). Distinct de `F-IM-27-vpf-liens-personnels-l42323-fr` qui traite la voie L.423-23 générale : ici l'angle est **spécifiquement le PACS** (critères d'ancienneté et d'intensité propres dégagés par la jurisprudence). L'outil apprécie le PACS comme faisceau, pas comme droit automatique.

## Comportement (branches, branche `default`)
- **POST** `/api/v1/case-files/{caseFileId}/pacs-vpf-analysis`
- Body `PacsVpfAnalyzeRequest` : `pacsConclu` (bool), `datePacs` (LocalDate, nullable), `partenaireFrancaisOuRegulier` (enum `FRANCAIS` | `ETRANGER_REGULIER` | `AUTRE`), `dureeVieCommuneMois` (int, nullable), `intensiteCommunauteVie` (enum `FORTE` | `MOYENNE` | `FAIBLE` | `NON_ETABLIE`), `autresLiensPrivesFamiliaux` (bool).
- Logique (annotée « à vérifier par avocat ») :
  - PACS récent (< ~1 an) ou intensité faible → le PACS seul est **insuffisant** ; verdict orientant vers consolidation du faisceau.
  - PACS ancien (≥ ~1 an) + intensité forte + partenaire français/régulier → faisceau favorable L.423-23.
  - intégrer `autresLiensPrivesFamiliaux` comme élément renforçant.
- Verdict enum `eligibilite` : `FAISCEAU_FAVORABLE` / `FAISCEAU_INSUFFISANT` / `A_CONSOLIDER` / `NON_ELIGIBLE`.
- Output : `eligibilite` + `elementsFavorables` (string[]) + `elementsManquants` (string[]) + `basesJuridiques` (string[]) + `messages` (string[]). Persisté 1:1 dans `pacs_vpf_analyses`.
- **GET** `/api/v1/case-files/{caseFileId}/pacs-vpf-analysis` → 200 ou 404.

## Cas d'erreur
| Situation | Comportement |
|---|---|
| Gate : `case_file.country` ≠ FRANCE | 400 Bad Request |
| Gate : domaine ≠ DROIT_IMMIGRATION | 400 Bad Request |
| `intensiteCommunauteVie` hors enum | 400 Bad Request (validation) |
| `dureeVieCommuneMois` < 0 | 400 Bad Request |
| GET sans POST / autre workspace | 404 (isolation workspace) |

## Source juridique (à vérifier par avocat)
- **CESEDA L.423-23** (carte VPF, liens privés et familiaux) (à vérifier par avocat).
- Jurisprudence sur la valeur probante du PACS (ancienneté, intensité de la communauté de vie) (à vérifier par avocat).

## Champs IA à extraire (`ImmigrationExtractedData`)
| Champ | Type | Extension |
|---|---|---|
| `pacsConclu` | bool | Extension record + prompt Immigration |
| `pacsDate` | date | Extension record + prompt |
| `pacsDureeVieCommune` | int | Extension record + prompt |
| `pacsIntensiteCommunauteVie` | texte | Extension record + prompt |

**Flag pivot CONTEXTUAL** : `pacs_detecte` (niveau 2, FR-only).

## Critères d'acceptation
- [ ] Les 4 valeurs de `eligibilite` couvertes + `elementsManquants` renseigné si insuffisant / à consolider.
- [ ] PACS seul jamais traité comme droit automatique (différence explicite avec le mariage F-IM-21).
- [ ] Gate 400 si country≠FRANCE ou domaine≠DROIT_IMMIGRATION.
- [ ] Isolation workspace testée (404 cross-workspace).
- [ ] Tous les champs saisissables pré-remplis par l'IA (F-246), sauf non factualisable.
- [ ] `F-IM-50-pacs-vpf-fr` dans `KNOWN_FRONTEND_TOOL_IDS` + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- [ ] Seed : `layer=CONTEXTUAL`, `trigger_field=pacs_detecte` (jamais ALWAYS_ON).

## Plan de test
- **UT** `PacsVpfAnalyzerTest` : ≥ 6 cas (faisceau favorable, insuffisant, à consolider, non éligible, partenaire français vs étranger régulier).
- **IT** `PacsVpfControllerIT` : 200 + 400 gate country + 400 gate domaine + 400 validation + 404 isolation workspace.
- **Jest** `pacs-vpf-section.component.spec` : rendu form + verdict + bouton désactivé si vide + flush jurisprudence-citations + `getPrefillCount` parité (F-237).

## Tables / endpoints / composants
- Backend : migration `pacs_vpf_analyses` (à pré-assigner) + entité + repo + `PacsVpfAnalyzer` + `PacsVpfController`.
- Frontend : `pacs-vpf-section.component` (+ .html/.scss/.spec + prefill-rules) + `TOOL_REGISTRY` `F-IM-50-pacs-vpf-fr` + `decision_tool_visibility_rules` + `KNOWN_NO_DASHBOARD_TILE_IDS` + `KNOWN_FRONTEND_TOOL_IDS`.
- Champs IA (`ImmigrationExtractedData`) : `pacsConclu`, `pacsDate`, `pacsDureeVieCommune`, `pacsIntensiteCommunauteVie` + flag `pacsDetecte` — étendre record + prompt Immigration.

## Invariants
- **CONTEXTUAL** (`pacs_detecte`), jamais ALWAYS_ON.
- **Pré-fill IA F-246** sur tous les champs + F-IA-03 sur le pivot.
- Instrumentation visibility + `KNOWN_NO_DASHBOARD_TILE_IDS` ([[feedback_pre_merge_visibility_seed_check]]).
- **1 outil = 1 situation** : PACS comme faisceau L.423-23, distinct de F-IM-21 (mariage) et F-IM-27 (liens personnels généraux).

## Hors périmètre
- Conjoint marié de Français (F-IM-21).
- Voie L.423-23 liens personnels générale (F-IM-27).
