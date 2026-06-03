# Mini-spec — F-223 / SF-223-09 — Outil modification de l'état civil (Belgique)

## Identifiant
`F-223 / SF-223-09` — tool_id `etat-civil-be-modification` (Famille BE) — statut `ready` — créé 2026-06-03 — branche `feat/SF-223-09-etat-civil-be-modification`

## Objectif (1 phrase)
Qualifier les conditions et la procédure d'une **modification de l'état civil en Belgique** (changement de nom / de prénom : loi 18/06/2018 ; changement de sexe : loi 25/06/2017, auto-déclaration administrative — à vérifier par avocat belge) via l'officier de l'état civil compétent.

## Périmètre / anti-doublon
**BE-only fort** : l'auto-déclaration du changement de sexe (loi 25/06/2017, procédure administrative ≠ procédure judiciaire FR) est une spécificité belge marquée. Outil **multi-vues unique** (changement de nom / prénom / sexe = branches de la même situation « modification de l'état civil » — fusion explicite cadrage, 1 outil = 1 situation). Distinct de la rectification d'état civil (cas occasionnels → P4 différé F-224).

## Comportement (cas nominal)
- `POST /api/v1/case-files/{caseFileId}/etat-civil-be-modification-analysis` (+ `GET`).
- Entrées : `typeModification` (enum `CHANGEMENT_NOM` / `CHANGEMENT_PRENOM` / `CHANGEMENT_SEXE`), `personneMajeure` (bool), `nationaliteBelgeOuResident` (bool), `motifLegitime` (bool nullable — nom), `secondeDemandePrenom` (bool nullable — prénom, gratuité de la 1re), `declarationSexeReiteree` (bool nullable — sexe, délai de réflexion + 2e déclaration), `consentementRepresentantsSiMineur` (bool nullable).
- Logique verdict :
  - `CHANGEMENT_PRENOM` → loi 18/06/2018 : compétence officier état civil, 1re demande à tarif réduit/gratuit, conditions allégées.
  - `CHANGEMENT_NOM` → procédure officier état civil réformée (motif, absence de confusion/atteinte aux tiers).
  - `CHANGEMENT_SEXE` → loi 25/06/2017 : auto-déclaration administrative, délai de réflexion + seconde déclaration confirmative, conditions de majorité (mineur = régime spécifique).
- Verdict 4 niveaux : `MODIFICATION_RECEVABLE` / `MODIFICATION_RECEVABLE_SOUS_CONDITIONS` / `MODIFICATION_IRRECEVABLE` / `QUALIFICATION_INCOMPLETE` + procédure (officier état civil compétent, pièces, délais) + bases juridiques annotées « (à vérifier par avocat belge — lois 18/06/2018 + 25/06/2017 + renumérotation CC post-réformes 2017-2019) ».

## Cas d'erreur
| Situation | HTTP |
|---|---|
| Corps absent / `typeModification` absent ou invalide | 400 |
| Workspace ≠ BELGIQUE | 400 |
| `legalDomain` ≠ DROIT_FAMILLE | 400 |
| Autre workspace / inexistant / GET sans calcul | 404 |

## Champs IA (`FamilleExtractedData`)
- **Flag pivot CONTEXTUAL niveau 2 BE-only** : `etatCivilModificationBeDetectee` — **nouveau champ** boolean (default false, BE-only). Un seul boolean (pas de sous-record `@JsonUnwrapped`, plafond 255 params [[feedback_travailextracteddata_255_param_limit]]). Distinct de `changement_etat_civil_detection` (sous-objet FR mentionné ligne ~4334 du record — FR-only, ne pas réutiliser).
- Pré-fill (F-246) : type de modification, majorité, nationalité/résidence pré-remplis si factualisables ; sinon vides.

## Critères d'acceptation
- [ ] Les 3 types de modification couverts ; 4 verdicts produits.
- [ ] Changement de sexe → procédure d'auto-déclaration + seconde déclaration confirmative exposée (spécificité BE).
- [ ] 1re demande de prénom → gratuité/tarif réduit signalé.
- [ ] Mineur → consentement des représentants requis (sinon `MODIFICATION_RECEVABLE_SOUS_CONDITIONS` ou irrecevable).
- [ ] Aucune citation jurisprudentielle BE (F-JU-04 parké).
- [ ] Isolation workspace + gate BE-only/DROIT_FAMILLE testés.

## Plan de test
UT Calculator (3 types × verdicts + auto-déclaration sexe + gratuité 1re demande prénom + mineur), IT endpoint (200 + 400 gate + 404 isolation), Jest composant.

## Tables / endpoints / composants
- Backend : migration `etat_civil_be_modification_analyses` (NNN — à pré-assigner) + Calculator/Service/Controller (pattern F-217). Migration record IA addColumn flag — à pré-assigner.
- Frontend : `etat-civil-be-modification-section.component` (+ artefacts) + `TOOL_REGISTRY` `etat-civil-be-modification` + `THEME_BY_TOOL` + seed visibility (CONTEXTUAL, trigger `etatCivilModificationBeDetectee`) + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- IA : ajout flag `etatCivilModificationBeDetectee` à `FamilleExtractedData` + builder + prompt Famille BE (false hors BE).

## Invariants
CONTEXTUAL jamais ALWAYS_ON ; pré-fill F-246 ; visibility + KNOWN_NO_DASHBOARD ; 1 outil = 1 situation ; BE-only ; pas de citation jurisprudence BE.

## Hors périmètre
Rectification d'état civil (P4 différé F-224) ; changement d'état civil FR (sous-objet `changement_etat_civil_detection` FR-only) ; génération de la déclaration à l'officier de l'état civil.
