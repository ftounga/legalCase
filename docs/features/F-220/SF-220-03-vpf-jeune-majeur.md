# Mini-spec — F-220 / SF-220-03 — Outil VPF jeune majeur (CESEDA L.423-22)

## Identifiant
`F-220 / SF-220-03` — tool_id `F-IM-49-vpf-jeune-majeur-l42322-fr` (Immigration FR)
- slug : `vpf-jeune-majeur`
- statut : `ready`
- date : 2026-06-03
- branche Git : `feat/SF-220-03-vpf-jeune-majeur`

## Objectif (1 phrase)
Évaluer l'éligibilité d'un jeune majeur (16-21 ans, entré mineur, scolarisé / pris en charge) à la carte « vie privée et familiale » de l'art. L.423-22 CESEDA (transition à la majorité / sortie ASE).

## Périmètre / anti-doublon
Distinct de `F-IM-27-vpf-liens-personnels-l42323-fr` (L.423-23, liens personnels et familiaux) : ici la voie est L.423-22, propre au **jeune majeur entré mineur**. Distinct de `F-IM-19-mineurs` (s'arrête à la majorité) et de `F-IM-38-mna-evaluation-age-fr` (évaluation de l'âge). L'outil couvre le **trou de la transition majorité** (sortie ASE), non couvert ailleurs.

## Comportement (branches, branche `default`)
- **POST** `/api/v1/case-files/{caseFileId}/vpf-jeune-majeur-analysis`
- Body `VpfJeuneMajeurAnalyzeRequest` : `age` (int), `entreMineur` (bool), `dateEntreeFrance` (LocalDate, nullable), `priseEnChargeAse` (bool), `dateDebutPriseEnCharge` (LocalDate, nullable), `scolariseOuFormation` (bool), `caractereReelEtSerieuxFormation` (bool).
- Logique (annotée « à vérifier par avocat ») :
  - éligibilité L.423-22 si `entreMineur=true` ET `age` ∈ [16,21] ET prise en charge ASE (ancienneté propre selon âge d'entrée) ET formation réelle et sérieuse.
  - apprécier l'**ancienneté de prise en charge** requise (variable selon âge d'entrée < 16 ans / entre 16 et 18 ans) → différence de fondement (L.423-22 vs admission exceptionnelle).
  - condition d'avis de la structure d'accueil + absence de lien avec la famille restée au pays.
- Verdict enum `eligibilite` : `ELIGIBLE_L42322` / `ELIGIBLE_SOUS_RESERVE` (un critère à confirmer) / `NON_ELIGIBLE` / `ORIENTER_AES` (renvoi voie admission exceptionnelle si hors critères stricts).
- Output : `eligibilite` + `criteresManquants` (string[]) + `ancienneteRequiseMois` (int) + `basesJuridiques` (string[]) + `messages` (string[]). Persisté 1:1 dans `vpf_jeune_majeur_analyses`.
- **GET** `/api/v1/case-files/{caseFileId}/vpf-jeune-majeur-analysis` → 200 ou 404.

## Cas d'erreur
| Situation | Comportement |
|---|---|
| Gate : `case_file.country` ≠ FRANCE | 400 Bad Request |
| Gate : domaine ≠ DROIT_IMMIGRATION | 400 Bad Request |
| `age` ≤ 0 ou > 30 | 400 Bad Request (validation) |
| GET sans POST / autre workspace | 404 (isolation workspace) |

## Source juridique (à vérifier par avocat)
- **CESEDA L.423-22** (ancien L.313-11 2°bis — VPF jeune majeur entré mineur) (à vérifier par avocat).
- Articulation avec L.435-3 (admission exceptionnelle jeune majeur confié ASE) (à vérifier par avocat).

## Champs IA à extraire (`ImmigrationExtractedData`)
| Champ | Type | Extension |
|---|---|---|
| `jeuneMajeurAge` (proxy `age`) | int | Extension record + prompt Immigration |
| `jeuneMajeurEntreMineur` | bool | Extension record + prompt |
| `jeuneMajeurPriseEnChargeAse` | bool | Extension record + prompt |
| `jeuneMajeurScolarise` | bool | Extension record + prompt |

**Flag pivot CONTEXTUAL** : `jeune_majeur_ex_mna_detecte` (niveau 2, FR-only).

## Critères d'acceptation
- [ ] Les 4 valeurs de `eligibilite` couvertes + `criteresManquants` renseigné en cas de réserve / non-éligibilité.
- [ ] `ancienneteRequiseMois` varie selon âge d'entrée (< 16 / 16-18).
- [ ] `NON_ELIGIBLE` hors critères stricts ⇒ `ORIENTER_AES` + message renvoi L.435-3.
- [ ] Gate 400 si country≠FRANCE ou domaine≠DROIT_IMMIGRATION.
- [ ] Isolation workspace testée (404 cross-workspace).
- [ ] Tous les champs saisissables pré-remplis par l'IA (F-246), sauf non factualisable.
- [ ] `F-IM-49-vpf-jeune-majeur-l42322-fr` dans `KNOWN_FRONTEND_TOOL_IDS` + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- [ ] Seed : `layer=CONTEXTUAL`, `trigger_field=jeune_majeur_ex_mna_detecte` (jamais ALWAYS_ON).

## Plan de test
- **UT** `VpfJeuneMajeurAnalyzerTest` : ≥ 6 cas (éligible, sous réserve, non éligible, orienter AES, ancienneté selon âge d'entrée).
- **IT** `VpfJeuneMajeurControllerIT` : 200 + 400 gate country + 400 gate domaine + 400 validation + 404 isolation workspace.
- **Jest** `vpf-jeune-majeur-section.component.spec` : rendu form + verdict + bouton désactivé si vide + flush jurisprudence-citations + `getPrefillCount` parité (F-237).

## Tables / endpoints / composants
- Backend : migration `vpf_jeune_majeur_analyses` (à pré-assigner) + entité + repo + `VpfJeuneMajeurAnalyzer` + `VpfJeuneMajeurController`.
- Frontend : `vpf-jeune-majeur-section.component` (+ .html/.scss/.spec + prefill-rules) + `TOOL_REGISTRY` `F-IM-49-vpf-jeune-majeur-l42322-fr` + `decision_tool_visibility_rules` + `KNOWN_NO_DASHBOARD_TILE_IDS` + `KNOWN_FRONTEND_TOOL_IDS`.
- Champs IA (`ImmigrationExtractedData`) : `jeuneMajeurAge`, `jeuneMajeurEntreMineur`, `jeuneMajeurPriseEnChargeAse`, `jeuneMajeurScolarise` + flag `jeuneMajeurExMnaDetecte` — étendre record + prompt Immigration.

## Invariants
- **CONTEXTUAL** (`jeune_majeur_ex_mna_detecte`), jamais ALWAYS_ON.
- **Pré-fill IA F-246** sur tous les champs + F-IA-03 sur le pivot.
- Instrumentation visibility + `KNOWN_NO_DASHBOARD_TILE_IDS` ([[feedback_pre_merge_visibility_seed_check]]).
- **1 outil = 1 situation** : VPF jeune majeur L.423-22, distinct de F-IM-19 / F-IM-27 / F-IM-38.

## Hors périmètre
- Évaluation de l'âge MNA (F-IM-38).
- VPF liens personnels L.423-23 (F-IM-27).
- Tutelle MNA juge des enfants (différée signal terrain).
