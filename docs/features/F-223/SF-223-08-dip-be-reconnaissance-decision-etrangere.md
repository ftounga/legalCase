# Mini-spec — F-223 / SF-223-08 — Outil reconnaissance / exequatur d'une décision familiale étrangère (Belgique)

## Identifiant
`F-223 / SF-223-08` — tool_id `dip-be-reconnaissance-decision-etrangere` (Famille BE) — statut `ready` — créé 2026-06-03 — branche `feat/SF-223-08-dip-be-reconnaissance-decision-etrangere`

## Objectif (1 phrase)
Qualifier la **reconnaissance ou l'exequatur en Belgique d'une décision familiale étrangère** (jugement hors UE ; mariage religieux non précédé d'un mariage civil) au regard du CDIP belge art. 22-27 (loi 16/07/2004 — à vérifier par avocat belge) : conditions de reconnaissance de plein droit, motifs de refus (ordre public, droits de la défense, fraude), et nécessité d'une procédure d'exequatur.

## Périmètre / anti-doublon
Distinct de `dip-be-loi-applicable-famille` (SF-223-07 : *quelle loi* régit une situation à instruire ≠ *reconnaître une décision déjà rendue*). Distinct de `mariage-etranger-be-reconnaissance` (F-217 : reconnaissance d'un **mariage/divorce valablement célébré à l'étranger**) : cet outil traite la **décision juridictionnelle étrangère** et le **mariage religieux non précédé d'un civil** (art. 21 Constitution / CC art. 161 — défaut de mariage civil préalable, ≠ mariage étranger valable). Cadrage net imposé par l'invariant cadrage (« mariage religieux non-civil ≠ reconnaissance mariage étranger »).

## Comportement (cas nominal)
- `POST /api/v1/case-files/{caseFileId}/dip-be-reconnaissance-decision-etrangere-analysis` (+ `GET`).
- Entrées : `natureDecision` (enum `JUGEMENT_ETRANGER_HORS_UE` / `MARIAGE_RELIGIEUX_NON_CIVIL`), `paysOrigine` (String ISO-2), `dateDecision` (date), `decisionDefinitive` (bool nullable), `droitsDefenseRespectes` (bool nullable), `conformiteOrdrePublicBelge` (bool), `absenceFraude` (bool nullable), `mariageCivilPrealable` (bool nullable — propre au cas religieux).
- Logique verdict :
  - `JUGEMENT_ETRANGER_HORS_UE` → CDIP art. 22-25 : reconnaissance de plein droit si décision définitive + droits de la défense respectés + non contraire à l'ordre public + absence de fraude ; sinon `EXEQUATUR_REQUIS` ou refus.
  - `MARIAGE_RELIGIEUX_NON_CIVIL` → art. 21 Constitution / CC art. 161 : un mariage religieux non précédé du mariage civil n'a aucun effet civil en Belgique → `RECONNAISSANCE_REFUSEE` (motif défaut de civil préalable).
- Verdict 4 niveaux : `RECONNAISSANCE_DE_PLEIN_DROIT` / `EXEQUATUR_REQUIS` / `RECONNAISSANCE_REFUSEE` / `QUALIFICATION_INCOMPLETE` + motifs (ordre public / défense / fraude / défaut de civil) + actes à produire (légalisation/apostille, requête en exequatur TF) + bases juridiques annotées « (à vérifier par avocat belge — CDIP art. 22-27 + renumérotation CC post-réformes 2017-2019) ».

## Cas d'erreur
| Situation | HTTP |
|---|---|
| Corps absent / `natureDecision` absent ou invalide | 400 |
| `paysOrigine` non ISO-2 (`^[A-Z]{2}$`) | 400 |
| `dateDecision` future / mal formée | 400 |
| Champs propres au jugement absents quand `natureDecision=JUGEMENT_ETRANGER_HORS_UE` | 400 |
| Workspace ≠ BELGIQUE | 400 |
| `legalDomain` ≠ DROIT_FAMILLE | 400 |
| Autre workspace / inexistant / GET sans calcul | 404 |

## Champs IA (`FamilleExtractedData`)
- **Flag pivot CONTEXTUAL niveau 2 BE-only** : `dipReconnaissanceDecisionBeDetectee` — **nouveau champ** boolean (default false, BE-only). Un seul boolean (pas de sous-record `@JsonUnwrapped`, plafond 255 params [[feedback_travailextracteddata_255_param_limit]]).
- Pré-fill (F-246) : nature de la décision, pays, date pré-remplis si factualisables ; sinon vides.

## Critères d'acceptation
- [ ] Jugement hors UE conforme (définitif + défense + ordre public + pas de fraude) → `RECONNAISSANCE_DE_PLEIN_DROIT`.
- [ ] Jugement hors UE avec réserve → `EXEQUATUR_REQUIS` ou refus + motif.
- [ ] Mariage religieux sans civil préalable → `RECONNAISSANCE_REFUSEE` + motif défaut de civil (art. 21 Constit. / CC 161).
- [ ] Distinction nette vs `mariage-etranger-be-reconnaissance` (mariage étranger valable) documentée dans le verdict/messages.
- [ ] Aucune citation jurisprudentielle BE (F-JU-04 parké).
- [ ] Isolation workspace + gate BE-only/DROIT_FAMILLE testés.

## Plan de test
UT Calculator (jugement reconnu / exequatur / refus + mariage religieux non-civil refusé), IT endpoint (200 + 400 gate/ISO-2/date/champs jugement manquants + 404 isolation), Jest composant.

## Tables / endpoints / composants
- Backend : migration `dip_be_reconnaissance_decision_etrangere_analyses` (NNN — à pré-assigner) + Calculator/Service/Controller (pattern F-217). Migration record IA addColumn flag — à pré-assigner.
- Frontend : `dip-be-reconnaissance-decision-etrangere-section.component` (+ artefacts) + `TOOL_REGISTRY` `dip-be-reconnaissance-decision-etrangere` + `THEME_BY_TOOL` + seed visibility (CONTEXTUAL, trigger `dipReconnaissanceDecisionBeDetectee`) + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- IA : ajout flag `dipReconnaissanceDecisionBeDetectee` à `FamilleExtractedData` + builder + prompt Famille BE (false hors BE).

## Invariants
CONTEXTUAL jamais ALWAYS_ON ; pré-fill F-246 ; visibility + KNOWN_NO_DASHBOARD ; 1 outil = 1 situation ; BE-only ; pas de citation jurisprudence BE.

## Hors périmètre
Loi applicable (`dip-be-loi-applicable-famille` SF-223-07) ; reconnaissance d'un mariage/divorce étranger valablement célébré dont talaq (`mariage-etranger-be-reconnaissance` F-217) ; reconnaissance des décisions UE (Bruxelles II ter — de plein droit, hors périmètre) ; génération de la requête en exequatur.
