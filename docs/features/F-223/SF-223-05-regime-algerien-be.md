# Mini-spec — F-223 / SF-223-05 — Outil régime familial algérien en Belgique

## Identifiant
`F-223 / SF-223-05` — tool_id `regime-algerien-be` (Famille BE) — statut `ready` — créé 2026-06-03 — branche `feat/SF-223-05-regime-algerien-be`

## Objectif (1 phrase)
Qualifier le sort en Belgique d'un **mariage, d'un talaq ou d'une dot relevant du droit algérien** (CDIP — loi 16/07/2004 ; Convention algéro-belge — à vérifier par avocat belge) : conditions de consentement, conformité à l'ordre public belge, et effets reconnus (mariage, dot/mahr, répudiation).

## Périmètre / anti-doublon
**BE-only pur** (reporté explicitement à F-223 par le PRODUCT_SPEC). Outil **dédié au corridor algérien** (Convention bilatérale algéro-belge spécifique). Distinct de `mariage-etranger-be-reconnaissance` (F-217), qui traite le talaq **généralement** : ici le périmètre est la **spécificité algérienne** (Code de la famille algérien, dot/mahr, dispositions de la Convention algéro-belge). ⚠️ Risque de doublon avec F-217 à cadrer nettement : l'outil F-223 **n'instruit que** les éléments propres au régime algérien (dot/mahr, conditions du Code algérien, jeu de la Convention bilatérale) ; il renvoie vers `mariage-etranger-be-reconnaissance` pour la mécanique générale CDIP de reconnaissance du talaq. Si en review l'overlap dépasse l'invariant « 1 outil = 1 situation », fusionner dans F-217 comme branche corridor (à trancher en review).

## Comportement (cas nominal)
- `POST /api/v1/case-files/{caseFileId}/regime-algerien-be-analysis` (+ `GET`).
- Entrées : `natureActe` (enum `MARIAGE_ALGERIEN` / `TALAQ_ALGERIEN` / `DOT_MAHR`), `dateActe` (date), `consentementEpouxEpouse` (bool), `dotMahrPrevue` (bool nullable), `montantDotConnu` (Double nullable), `conventionAlgeroBelgeInvoquee` (bool), `lienRattachementBelgique` (enum `RESIDENCE` / `NATIONALITE` / `AUCUN`).
- Logique verdict : (a) mariage algérien → conformité fond (Code de la famille algérien — capacité, consentement, absence d'empêchement) + ordre public belge ; (b) talaq algérien → renvoi méthode CDIP générale (F-217) + spécificités Convention algéro-belge ; (c) dot/mahr → qualification de l'obligation (effet patrimonial reconnu vs contraire à l'ordre public) ; (d) jeu de la Convention bilatérale si invoquée.
- Verdict 4 niveaux : `RECONNAISSANCE_DE_PLEIN_DROIT` / `RECONNAISSANCE_SOUS_CONDITIONS` / `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC` / `QUALIFICATION_INCOMPLETE` + motifs + effets de la dot + bases juridiques annotées « (à vérifier par avocat belge — Convention algéro-belge + renumérotation CC post-réformes 2017-2019) ».

## Cas d'erreur
| Situation | HTTP |
|---|---|
| Corps absent / `natureActe` ou `lienRattachementBelgique` absent/invalide | 400 |
| `dateActe` future / mal formée | 400 |
| `montantDotConnu` < 0 | 400 |
| Workspace ≠ BELGIQUE | 400 |
| `legalDomain` ≠ DROIT_FAMILLE | 400 |
| Autre workspace / inexistant / GET sans calcul | 404 |

## Champs IA (`FamilleExtractedData`)
- **Flag pivot CONTEXTUAL niveau 2 BE-only** : `regimeAlgerienBeDetecte` — **nouveau champ** boolean (default false, BE-only). Un seul boolean (pas de sous-record `@JsonUnwrapped`, plafond 255 params [[feedback_travailextracteddata_255_param_limit]]).
- Pré-fill (F-246) : nature de l'acte, date, montant de la dot pré-remplis si factualisables ; sinon vides.

## Critères d'acceptation
- [ ] Mariage algérien conforme fond + ordre public → `RECONNAISSANCE_DE_PLEIN_DROIT`.
- [ ] Atteinte ordre public (mariage forcé, polygamie) → `RECONNAISSANCE_REFUSEE_ORDRE_PUBLIC` + motif.
- [ ] Dot/mahr → qualification de l'effet patrimonial.
- [ ] Talaq → renvoi explicite `mariage-etranger-be-reconnaissance` (F-217) pour la mécanique CDIP générale.
- [ ] Aucune citation jurisprudentielle BE (F-JU-04 parké).
- [ ] Isolation workspace + gate BE-only/DROIT_FAMILLE testés.

## Plan de test
UT Calculator (mariage reconnu + refus ordre public + dot + renvoi talaq F-217), IT endpoint (200 + 400 gate/date/montant + 404 isolation), Jest composant.

## Tables / endpoints / composants
- Backend : migration `regime_algerien_be_analyses` (NNN — à pré-assigner) + Calculator/Service/Controller (pattern F-217). Migration record IA addColumn flag — à pré-assigner.
- Frontend : `regime-algerien-be-section.component` (+ artefacts) + `TOOL_REGISTRY` `regime-algerien-be` + `THEME_BY_TOOL` + seed visibility (CONTEXTUAL, trigger `regimeAlgerienBeDetecte`) + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- IA : ajout flag `regimeAlgerienBeDetecte` à `FamilleExtractedData` + builder + prompt Famille BE (false hors BE).

## Invariants
CONTEXTUAL jamais ALWAYS_ON ; pré-fill F-246 ; visibility + KNOWN_NO_DASHBOARD ; 1 outil = 1 situation ; BE-only ; pas de citation jurisprudence BE.

## Hors périmètre
Mécanique CDIP générale du talaq (`mariage-etranger-be-reconnaissance` F-217) ; séjour (immigration) ; succession algérienne (`dip-be-loi-applicable-famille`). Si l'overlap avec F-217 dépasse l'invariant en review → fusion en branche corridor de F-217 (décision review).
