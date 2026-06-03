# Mini-spec — F-223 / SF-223-07 — Outil loi applicable en droit de la famille (DIP belge)

## Identifiant
`F-223 / SF-223-07` — tool_id `dip-be-loi-applicable-famille` (Famille BE) — statut `ready` — créé 2026-06-03 — branche `feat/SF-223-07-dip-be-loi-applicable-famille`

## Objectif (1 phrase)
Déterminer la **loi applicable** à une situation familiale présentant un élément d'extranéité (divorce / régime matrimonial / succession) au regard de Rome III (Règl. UE 1259/2010), du Règl. UE 2016/1103 (régimes matrimoniaux), du Règl. UE 650/2012 (successions) et du CDIP belge (loi 16/07/2004) — à vérifier par avocat belge.

## Périmètre / anti-doublon
**Fusion explicite** (cadrage) des items audit `dip-be-loi-applicable-divorce` + `dip-be-loi-applicable-regime-mat` + `dip-be-loi-applicable-succession` + `regime-international-be` + `succession-be-internationale` : la **détermination de la loi applicable EST la situation** (1 outil = 1 situation, multi-vues par matière). Distinct de `dip-be-reconnaissance-decision-etrangere` (SF-223-08 : reconnaissance/exequatur d'une décision déjà rendue ≠ choix de la loi applicable à une situation à instruire). Ne tranche pas la compétence juridictionnelle (Bruxelles II ter — hors périmètre V1).

## Comportement (cas nominal)
- `POST /api/v1/case-files/{caseFileId}/dip-be-loi-applicable-famille-analysis` (+ `GET`).
- Entrées : `matiere` (enum `DIVORCE` / `REGIME_MATRIMONIAL` / `SUCCESSION`), `residenceHabituelleCommune` (String ISO-2 nullable), `nationaliteCommune` (String ISO-2 nullable), `choixLoiParLesParties` (String ISO-2 nullable — professio juris), `dateMariageOuDeces` (date nullable), `lieuSituationBiensImmobiliers` (String ISO-2 nullable).
- Logique verdict par matière :
  - `DIVORCE` → Rome III : 1) loi choisie par les parties (art. 5) ; 2) à défaut, résidence habituelle commune ; 3) dernière résidence commune ; 4) nationalité commune ; 5) loi du for (échelle art. 8).
  - `REGIME_MATRIMONIAL` → Règl. 2016/1103 : loi choisie sinon première résidence commune sinon nationalité commune sinon lien le plus étroit.
  - `SUCCESSION` → Règl. 650/2012 : professio juris (loi nationale) sinon résidence habituelle du défaut au décès.
- Verdict : `loiApplicableDeterminee` (String ISO-2 ou label) + `fondementRattachement` (enum du critère retenu) + `LOI_INDETERMINABLE` si inputs insuffisants + actes/conseils + bases juridiques annotées « (à vérifier par avocat belge — Règlements UE + CDIP + renumérotation CC post-réformes 2017-2019) ».

## Cas d'erreur
| Situation | HTTP |
|---|---|
| Corps absent / `matiere` absent ou invalide | 400 |
| Tout champ ISO-2 non conforme (`^[A-Z]{2}$`) | 400 |
| `dateMariageOuDeces` future / mal formée | 400 |
| Workspace ≠ BELGIQUE | 400 |
| `legalDomain` ≠ DROIT_FAMILLE | 400 |
| Autre workspace / inexistant / GET sans calcul | 404 |

## Champs IA (`FamilleExtractedData`)
- **Flag pivot CONTEXTUAL niveau 2 BE-only** : `dipFamilleBeDetecte` — **nouveau champ** boolean (default false, BE-only — déclenche sur tout élément d'extranéité familial dans un dossier BE). Un seul boolean (pas de sous-record `@JsonUnwrapped`, plafond 255 params [[feedback_travailextracteddata_255_param_limit]]).
- Pré-fill (F-246) : résidences, nationalités, choix de loi, dates pré-remplis si factualisables ; sinon vides.

## Critères d'acceptation
- [ ] Les 3 matières couvertes ; échelle de rattachement Rome III respectée pour le divorce.
- [ ] Choix de loi par les parties (professio juris) prime quand présent.
- [ ] Inputs insuffisants → `LOI_INDETERMINABLE` + message.
- [ ] Le `fondementRattachement` retenu est exposé (traçabilité du raisonnement).
- [ ] Aucune citation jurisprudentielle BE (F-JU-04 parké).
- [ ] Isolation workspace + gate BE-only/DROIT_FAMILLE testés.

## Plan de test
UT Calculator (3 matières × échelle de rattachement + professio juris + indéterminable), IT endpoint (200 + 400 gate/ISO-2/date + 404 isolation), Jest composant.

## Tables / endpoints / composants
- Backend : migration `dip_be_loi_applicable_famille_analyses` (NNN — à pré-assigner) + Calculator/Service/Controller (pattern F-217). Migration record IA addColumn flag — à pré-assigner.
- Frontend : `dip-be-loi-applicable-famille-section.component` (+ artefacts) + `TOOL_REGISTRY` `dip-be-loi-applicable-famille` + `THEME_BY_TOOL` + seed visibility (CONTEXTUAL, trigger `dipFamilleBeDetecte`) + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- IA : ajout flag `dipFamilleBeDetecte` à `FamilleExtractedData` + builder + prompt Famille BE (false hors BE).

## Invariants
CONTEXTUAL jamais ALWAYS_ON ; pré-fill F-246 ; visibility + KNOWN_NO_DASHBOARD ; 1 outil = 1 situation ; BE-only ; pas de citation jurisprudence BE.

## Hors périmètre
Reconnaissance/exequatur d'une décision étrangère (`dip-be-reconnaissance-decision-etrangere` SF-223-08) ; compétence juridictionnelle (Bruxelles II ter) ; reconnaissance d'un mariage étranger valablement célébré (`mariage-etranger-be-reconnaissance` F-217) ; chiffrage du régime / de la succession (`regime-be-separation-biens`, `liquidation-partage-be`, `succession-be-*`).
