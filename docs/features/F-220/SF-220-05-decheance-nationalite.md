# Mini-spec — F-220 / SF-220-05 — Outil déchéance de nationalité (Cciv 25 / 25-1)

## Identifiant
`F-220 / SF-220-05` — tool_id `F-IM-51-decheance-nationalite-fr` (Immigration FR)
- slug : `decheance-nationalite`
- statut : `ready`
- date : 2026-06-03
- branche Git : `feat/SF-220-05-decheance-nationalite`

## Objectif (1 phrase)
Analyser la validité d'une mesure (envisagée ou prononcée) de déchéance de la nationalité française (Cciv 25 et 25-1 : motifs terrorisme / atteinte aux intérêts de la Nation / fraude), apprécier les conditions légales et identifier les voies de recours.

## Périmètre / anti-doublon
Distinct de `F-IM-13` (acquisition de la nationalité) : ici l'objet est la **perte par déchéance**, situation et juridiction propres. Distinct des recours naturalisation `F-IM-39` (TJ) / `F-IM-40` (TA Nantes) qui portent sur le refus d'acquisition. Distinct de l'apatridie (`F-IM-12`, écartée au cadrage). Outil rare mais à forte valeur (terrorisme, fraude).

## Comportement (branches, branche `default`)
- **POST** `/api/v1/case-files/{caseFileId}/decheance-nationalite-analysis`
- Body `DecheanceNationaliteAnalyzeRequest` : `motif` (enum `TERRORISME` | `ATTEINTE_INTERETS_NATION` | `FRAUDE_ACQUISITION` | `AUTRE`), `binational` (bool), `dateAcquisitionNationalite` (LocalDate, nullable), `dateFaits` (LocalDate, nullable), `mesurePrononcee` (bool), `dateDecret` (LocalDate, nullable).
- Logique (annotée « à vérifier par avocat ») :
  - condition d'**absence d'apatridie** : la déchéance suppose `binational=true` (ne peut rendre apatride) → si `binational=false`, verdict `MESURE_IRREGULIERE` (impossible).
  - condition de **délai** entre acquisition et faits (Cciv 25-1, délais selon motif) → si hors délai, `MESURE_IRREGULIERE`.
  - si `mesurePrononcee=true` → calcul du **délai de recours** (REP devant le Conseil d'État, 2 mois) à partir de `dateDecret`.
  - proportionnalité de la mesure (élément d'appréciation).
- Verdict enum `validite` : `CONDITIONS_REUNIES` / `MESURE_CONTESTABLE` (un critère fragile) / `MESURE_IRREGULIERE` (apatridie ou hors délai) / `INDETERMINE`.
- Output : `validite` + `conditionsManquantes` (string[]) + `voiesRecours` (string[]) + `delaiRecoursJours` (int, nullable) + `basesJuridiques` (string[]) + `messages` (string[]). Persisté 1:1 dans `decheance_nationalite_analyses`.
- **GET** `/api/v1/case-files/{caseFileId}/decheance-nationalite-analysis` → 200 ou 404.

## Cas d'erreur
| Situation | Comportement |
|---|---|
| Gate : `case_file.country` ≠ FRANCE | 400 Bad Request |
| Gate : domaine ≠ DROIT_IMMIGRATION | 400 Bad Request |
| `motif` hors enum | 400 Bad Request (validation) |
| GET sans POST / autre workspace | 404 (isolation workspace) |

## Source juridique (à vérifier par avocat)
- **Code civil art. 25** (motifs de déchéance) (à vérifier par avocat).
- **Code civil art. 25-1** (délais entre acquisition et faits) (à vérifier par avocat).
- Recours : recours pour excès de pouvoir contre le décret devant le **Conseil d'État** (délai 2 mois) (à vérifier par avocat).

## Champs IA à extraire (`ImmigrationExtractedData`)
| Champ | Type | Extension |
|---|---|---|
| `decheanceMotif` (proxy `motif`) | texte | Extension record + prompt Immigration |
| `decheanceBinational` | bool | Extension record + prompt |
| `decheanceMesurePrononcee` | bool | Extension record + prompt |
| `decheanceDateDecret` | date | Extension record + prompt |

**Flag pivot CONTEXTUAL** : `decheance_nationalite_detectee` (niveau 2, FR-only).

## Critères d'acceptation
- [ ] Les 4 valeurs de `validite` couvertes.
- [ ] `binational=false` ⇒ `MESURE_IRREGULIERE` (interdiction d'apatridie) + message.
- [ ] Hors délai Cciv 25-1 ⇒ `MESURE_IRREGULIERE`.
- [ ] `mesurePrononcee=true` ⇒ `delaiRecoursJours` calculé + voie REP Conseil d'État.
- [ ] Gate 400 si country≠FRANCE ou domaine≠DROIT_IMMIGRATION.
- [ ] Isolation workspace testée (404 cross-workspace).
- [ ] Tous les champs saisissables pré-remplis par l'IA (F-246), sauf non factualisable.
- [ ] `F-IM-51-decheance-nationalite-fr` dans `KNOWN_FRONTEND_TOOL_IDS` + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- [ ] Seed : `layer=CONTEXTUAL`, `trigger_field=decheance_nationalite_detectee` (jamais ALWAYS_ON).

## Plan de test
- **UT** `DecheanceNationaliteAnalyzerTest` : ≥ 6 cas (conditions réunies, contestable, irrégulière apatridie, irrégulière hors délai, calcul délai recours, par motif).
- **IT** `DecheanceNationaliteControllerIT` : 200 + 400 gate country + 400 gate domaine + 400 validation + 404 isolation workspace.
- **Jest** `decheance-nationalite-section.component.spec` : rendu form + verdict + bouton désactivé si vide + flush jurisprudence-citations + `getPrefillCount` parité (F-237).

## Tables / endpoints / composants
- Backend : migration `decheance_nationalite_analyses` (à pré-assigner) + entité + repo + `DecheanceNationaliteAnalyzer` + `DecheanceNationaliteController`.
- Frontend : `decheance-nationalite-section.component` (+ .html/.scss/.spec + prefill-rules) + `TOOL_REGISTRY` `F-IM-51-decheance-nationalite-fr` + `decision_tool_visibility_rules` + `KNOWN_NO_DASHBOARD_TILE_IDS` + `KNOWN_FRONTEND_TOOL_IDS`.
- Champs IA (`ImmigrationExtractedData`) : `decheanceMotif`, `decheanceBinational`, `decheanceMesurePrononcee`, `decheanceDateDecret` + flag `decheanceNationaliteDetectee` — étendre record + prompt Immigration.

## Invariants
- **CONTEXTUAL** (`decheance_nationalite_detectee`), jamais ALWAYS_ON.
- **Pré-fill IA F-246** sur tous les champs + F-IA-03 sur le pivot.
- Instrumentation visibility + `KNOWN_NO_DASHBOARD_TILE_IDS` ([[feedback_pre_merge_visibility_seed_check]]).
- **1 outil = 1 situation** : validité de la déchéance, distinct de F-IM-13 (acquisition) et F-IM-39/40 (recours naturalisation).

## Hors périmètre
- Acquisition de la nationalité (F-IM-13).
- Recours refus naturalisation TJ / TA Nantes (F-IM-39 / F-IM-40).
- Apatridie (F-IM-12).
- Rédaction du REP (générateur d'actes F-IM-06 en aval).
