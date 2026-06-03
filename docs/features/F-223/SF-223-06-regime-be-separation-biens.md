# Mini-spec — F-223 / SF-223-06 — Outil régime de séparation de biens (Belgique)

## Identifiant
`F-223 / SF-223-06` — tool_id `regime-be-separation-biens` (Famille BE) — statut `ready` — créé 2026-06-03 — branche `feat/SF-223-06-regime-be-separation-biens`

## Objectif (1 phrase)
Qualifier le **régime matrimonial de séparation de biens belge** (Livre 3 CC ; loi 22/07/2018 — à vérifier par avocat belge, renumérotation CC post-réformes 2017-2019) : séparation pure, adjonction d'une société d'acquêts, et correctifs équitables introduits en 2018 (clause de participation aux acquêts, correction judiciaire en équité).

## Périmètre / anti-doublon
**Régime distinct** de la communauté légale déjà livrée (`regime-mat-be-communaute-legale` F-217) et de la liquidation-partage (`liquidation-partage-be` F-217). Outil **unique** couvrant les variantes du régime de séparation (la communauté universelle et la participation aux acquêts pures sont des variantes de régime → branches/comparateur ici, conformément au cadrage : pas d'outils séparés). Ne calcule pas le partage notarial chiffré (renvoi `liquidation-partage-be`).

## Comportement (cas nominal)
- `POST /api/v1/case-files/{caseFileId}/regime-be-separation-biens-analysis` (+ `GET`).
- Entrées : `varianteRegime` (enum `SEPARATION_PURE` / `SEPARATION_AVEC_SOCIETE_ACQUETS` / `SEPARATION_AVEC_PARTICIPATION_ACQUETS`), `contratMariageNotarie` (bool), `clauseParticipationPrevue` (bool nullable), `disproportionPatrimonialeAllegee` (bool — correctif équitable 2018), `dateContrat` (date nullable), `patrimoinePropreEpoux1Eur` (Double nullable), `patrimoinePropreEpoux2Eur` (Double nullable).
- Logique verdict : (a) séparation pure → chaque époux conserve la propriété et la gestion de ses biens ; pas de masse commune ; (b) société d'acquêts adjointe → masse limitée d'acquêts à partager ; (c) clause de participation aux acquêts → créance de participation calculable à la dissolution (loi 2018) ; (d) correctif équitable 2018 → si disproportion alléguée, possibilité de correction judiciaire en équité (signaler, ne pas chiffrer).
- Verdict 4 niveaux : `SEPARATION_PURE_QUALIFIEE` / `SOCIETE_ACQUETS_QUALIFIEE` / `PARTICIPATION_ACQUETS_QUALIFIEE` / `QUALIFICATION_INCOMPLETE` + effets patrimoniaux + indication créance de participation/correctif équitable + bases juridiques annotées « (à vérifier par avocat belge — loi 22/07/2018 + renumérotation CC post-réformes 2017-2019) ».

## Cas d'erreur
| Situation | HTTP |
|---|---|
| Corps absent / `varianteRegime` absent ou invalide | 400 |
| `dateContrat` future / mal formée | 400 |
| Montants patrimoine < 0 | 400 |
| `clauseParticipationPrevue` absent quand `varianteRegime=SEPARATION_AVEC_PARTICIPATION_ACQUETS` | 400 |
| Workspace ≠ BELGIQUE | 400 |
| `legalDomain` ≠ DROIT_FAMILLE | 400 |
| Autre workspace / inexistant / GET sans calcul | 404 |

## Champs IA (`FamilleExtractedData`)
- **Flag pivot CONTEXTUAL niveau 2 BE-only** : `regimeSeparationBiensBeDetecte` — **nouveau champ** boolean (default false, BE-only). Un seul boolean (pas de sous-record `@JsonUnwrapped`, plafond 255 params [[feedback_travailextracteddata_255_param_limit]]). Ne pas confondre avec `regimeMatrimonialDetecte` (FR string, SF-246-07).
- Pré-fill (F-246) : variante du régime, date du contrat, patrimoines pré-remplis si factualisables ; sinon vides.

## Critères d'acceptation
- [ ] Les 3 variantes couvertes ; 4 verdicts produits.
- [ ] Participation aux acquêts → indication d'une créance de participation à liquider (renvoi `liquidation-partage-be`).
- [ ] Disproportion alléguée → mention du correctif équitable 2018 (sans chiffrage).
- [ ] Aucune citation jurisprudentielle BE (F-JU-04 parké).
- [ ] Champs pré-remplis IA (sauf non factualisable) ; flag pivot BE-only niveau 2.
- [ ] Isolation workspace + gate BE-only/DROIT_FAMILLE testés.

## Plan de test
UT Calculator (3 variantes + créance de participation + correctif équitable), IT endpoint (200 + 400 gate/date/montant/clause manquante + 404 isolation), Jest composant.

## Tables / endpoints / composants
- Backend : migration `regime_be_separation_biens_analyses` (NNN — à pré-assigner) + Calculator/Service/Controller (pattern F-217). Migration record IA addColumn flag — à pré-assigner.
- Frontend : `regime-be-separation-biens-section.component` (+ artefacts) + `TOOL_REGISTRY` `regime-be-separation-biens` + `THEME_BY_TOOL` + seed visibility (CONTEXTUAL, trigger `regimeSeparationBiensBeDetecte`) + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- IA : ajout flag `regimeSeparationBiensBeDetecte` à `FamilleExtractedData` + builder + prompt Famille BE (false hors BE).

## Invariants
CONTEXTUAL jamais ALWAYS_ON ; pré-fill F-246 ; visibility + KNOWN_NO_DASHBOARD ; 1 outil = 1 situation ; BE-only ; pas de citation jurisprudence BE.

## Hors périmètre
Communauté légale (`regime-mat-be-communaute-legale` F-217) ; partage notarial chiffré (`liquidation-partage-be` F-217) ; loi applicable au régime en présence d'extranéité (`dip-be-loi-applicable-famille`).
