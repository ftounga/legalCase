# Mini-spec — F-223 / SF-223-04 — Outil situation contentieuse post-GPA (Belgique)

## Identifiant
`F-223 / SF-223-04` — tool_id `gpa-be-situation-contentieuse` (Famille BE) — statut `ready` — créé 2026-06-03 — branche `feat/SF-223-04-gpa-be-situation-contentieuse`

## Objectif (1 phrase)
Fournir l'arbre décisionnel de l'**établissement de la filiation après une gestation pour autrui (GPA)** en Belgique, dans un contexte de **vide juridique** (absence de loi spécifique GPA — à vérifier par avocat belge) : la convention de GPA n'est pas opposable, et la filiation s'établit par les voies de droit commun (reconnaissance, adoption après naissance, action en établissement).

## Périmètre / anti-doublon
**BE-only pur** : le vide juridique belge (ni autorisée ni pénalement interdite) est une situation distincte de l'interdiction FR. Outil de **cadrage contentieux** (≠ outil de calcul). Distinct de `adoption-be` (l'adoption peut être *une voie* du verdict, mais la situation cadrée est la filiation post-GPA) et de `contestation-filiation-be` (F-217). Ne tranche pas l'aspect international du parcours GPA (renvoi `dip-be-loi-applicable-famille` / `dip-be-reconnaissance-decision-etrangere` si GPA à l'étranger).

## Comportement (cas nominal)
- `POST /api/v1/case-files/{caseFileId}/gpa-be-situation-contentieuse-analysis` (+ `GET`).
- Entrées : `gpaRealiseeEnBelgiqueOuEtranger` (enum `BELGIQUE` / `ETRANGER`), `lienGenetiqueParentIntentionnel` (enum `PERE_INTENTIONNEL` / `MERE_INTENTIONNELLE` / `AUCUN` / `LES_DEUX`), `acteNaissanceEtrangerEtabli` (bool nullable), `merePorteuseDesignee` (bool), `consentementMerePorteuse` (bool nullable), `coupleIntentionnelMarieOuCohabitant` (bool).
- Logique verdict (arbre, pas calcul) : (a) convention GPA non opposable → la mère qui accouche est la mère en droit belge (CC mater semper certa — à vérifier) ; (b) voie du père intentionnel avec lien génétique → reconnaissance possible ; (c) parent intentionnel sans lien génétique → adoption après naissance (renvoi `adoption-be`) ; (d) GPA à l'étranger avec acte étranger → renvoi reconnaissance/loi applicable (DIP).
- Verdict 4 niveaux : `FILIATION_PAR_RECONNAISSANCE` / `FILIATION_PAR_ADOPTION_POST_NAISSANCE` / `RECONNAISSANCE_ACTE_ETRANGER_A_INSTRUIRE` / `QUALIFICATION_INCOMPLETE` + chemin contentieux recommandé + risques (convention inopposable) + bases juridiques annotées « (à vérifier par avocat belge — vide juridique GPA + renumérotation CC post-réformes 2017-2019) ».

## Cas d'erreur
| Situation | HTTP |
|---|---|
| Corps absent / `gpaRealiseeEnBelgiqueOuEtranger` ou `lienGenetiqueParentIntentionnel` absent/invalide | 400 |
| Workspace ≠ BELGIQUE | 400 |
| `legalDomain` ≠ DROIT_FAMILLE | 400 |
| Autre workspace / inexistant / GET sans calcul | 404 |

## Champs IA (`FamilleExtractedData`)
- **Flag pivot CONTEXTUAL niveau 2 BE-only** : `gpaBeSituationContentieuseDetectee` — **nouveau champ** boolean (default false, BE-only). Ajout d'un seul boolean (pas de sous-record `@JsonUnwrapped`, plafond 255 params [[feedback_travailextracteddata_255_param_limit]]).
- Pré-fill (F-246) : lieu de GPA et lien génétique pré-remplis si factualisables ; sinon vides.

## Critères d'acceptation
- [ ] Père intentionnel avec lien génétique → `FILIATION_PAR_RECONNAISSANCE`.
- [ ] Parent intentionnel sans lien génétique → `FILIATION_PAR_ADOPTION_POST_NAISSANCE` (renvoi `adoption-be`).
- [ ] GPA étranger + acte étranger → `RECONNAISSANCE_ACTE_ETRANGER_A_INSTRUIRE` (renvoi DIP).
- [ ] Message explicite : convention de GPA non opposable + mère porteuse = mère en droit (mater semper certa).
- [ ] Aucune citation jurisprudentielle BE (F-JU-04 parké).
- [ ] Isolation workspace + gate BE-only/DROIT_FAMILLE testés.

## Plan de test
UT Calculator (4 branches de filiation + message inopposabilité), IT endpoint (200 + 400 gate + 404 isolation), Jest composant.

## Tables / endpoints / composants
- Backend : migration `gpa_be_situation_contentieuse_analyses` (NNN — à pré-assigner) + Calculator/Service/Controller (pattern F-217). Migration record IA addColumn flag — à pré-assigner.
- Frontend : `gpa-be-situation-contentieuse-section.component` (+ artefacts) + `TOOL_REGISTRY` `gpa-be-situation-contentieuse` + `THEME_BY_TOOL` + seed visibility (CONTEXTUAL, trigger `gpaBeSituationContentieuseDetectee`) + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- IA : ajout flag `gpaBeSituationContentieuseDetectee` à `FamilleExtractedData` + builder + prompt Famille BE (false hors BE).

## Invariants
CONTEXTUAL jamais ALWAYS_ON ; pré-fill F-246 ; visibility + KNOWN_NO_DASHBOARD ; 1 outil = 1 situation ; BE-only ; pas de citation jurisprudence BE (silence > erreur — d'autant plus prudent que la matière est en vide juridique).

## Hors périmètre
Adoption (`adoption-be`) ; reconnaissance/loi applicable de l'acte étranger (DIP) ; contestation de filiation (F-217) ; aspects pénaux éventuels.
