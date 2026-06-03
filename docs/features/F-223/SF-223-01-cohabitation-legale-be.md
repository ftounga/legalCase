# Mini-spec — F-223 / SF-223-01 — Outil cohabitation légale (Belgique)

## Identifiant
`F-223 / SF-223-01` — tool_id `cohabitation-legale-be` (Famille BE) — statut `ready` — créé 2026-06-03 — branche `feat/SF-223-01-cohabitation-legale-be`

## Objectif (1 phrase)
Qualifier le régime de la **cohabitation légale belge** (loi 23/11/1998 ; CC art. 1475-1479 — à vérifier par avocat belge, renumérotation CC post-réformes 2017-2019) : conditions de formation par déclaration à l'officier de l'état civil, effets patrimoniaux (logement commun protégé, contribution aux charges), et modalités de dissolution (unilatérale par déclaration / commune / par mariage / par décès).

## Périmètre / anti-doublon
**BE-only pur, aucun équivalent FR** (≠ PACS FR — F-FA-12 PACS-dissolution est FR-only et structurellement distinct). Outil **multi-vues unique** couvrant formation + effets + dissolution (1 outil = 1 situation « régime de la cohabitation légale » — invariant cadrage). Ne recouvre pas le mariage (`mariage-etranger-be-reconnaissance` F-217) ni la cohabitation de fait (`cohabitation-fait-be-effets` → P4 différé F-224).

## Comportement (cas nominal)
- `POST /api/v1/case-files/{caseFileId}/cohabitation-legale-be-analysis` (et `GET` du dernier résultat).
- Entrées : `vue` (enum `FORMATION` / `EFFETS` / `DISSOLUTION`), `deuxPersonnesNonMariees` (bool), `capaciteJuridique` (bool), `pasDejaLieParMariageOuAutreCohabitation` (bool), `domicileCommun` (bool), `logementFamilialEnJeu` (bool, nullable), `modeDissolutionEnvisage` (enum nullable `DECLARATION_COMMUNE` / `DECLARATION_UNILATERALE` / `MARIAGE` / `DECES`).
- Logique verdict : conditions de formation (CC 1475 — deux personnes capables, non mariées, non déjà liées) ; effets (protection du logement familial CC 1477, contribution aux charges proportionnelle) ; dissolution (CC 1476 — déclaration commune / unilatérale signifiée par huissier / mariage de l'un / décès).
- Verdict 4 niveaux : `FORMATION_VALIDE` / `FORMATION_IMPOSSIBLE` / `EFFETS_QUALIFIES` / `DISSOLUTION_QUALIFIEE` + conditions/motifs + actes à produire (déclaration officier état civil, signification huissier le cas échéant) + bases juridiques annotées « (à vérifier par avocat belge — renumérotation CC post-réformes 2017-2019) ».

## Cas d'erreur
| Situation | HTTP |
|---|---|
| Corps absent / `vue` absente ou invalide | 400 |
| `modeDissolutionEnvisage` absent quand `vue=DISSOLUTION` | 400 |
| Workspace `country` ≠ `BELGIQUE` (outil BE-only) | 400 |
| `legalDomain` ≠ `DROIT_FAMILLE` | 400 |
| Dossier d'un autre workspace / inexistant / GET sans calcul | 404 |

## Champs IA (`FamilleExtractedData` — record Famille partagé FR/BE, cf. `CaseAnalysisResponse.java`)
- **Flag pivot CONTEXTUAL niveau 2 BE-only** : `cohabitationLegaleBeDetectee` — **déjà présent** dans `FamilleExtractedData` (ajouté F-202, ligne ~4238). Réutilisé tel quel comme `trigger_field` de visibilité CONTEXTUAL — pas de nouveau champ à ajouter.
- Pré-fill (F-246) : champs saisissables pré-remplis quand factualisables (mention déclaration cohabitation légale, domicile commun) ; sinon laissés vides. Si aucun champ extractible stable en V1, `PREFILL_COUNT_ALWAYS_ZERO = true` côté section, mais le flag pivot existant alimente la visibilité CONTEXTUAL.

## Critères d'acceptation
- [ ] Les 3 vues (FORMATION / EFFETS / DISSOLUTION) couvertes, 4 verdicts produits.
- [ ] Conditions de formation refusées si une condition CC 1475 manque → `FORMATION_IMPOSSIBLE` + motif.
- [ ] Dissolution : actes à produire dépendent du `modeDissolutionEnvisage` (signification huissier pour unilatérale).
- [ ] Aucune citation jurisprudentielle BE (F-JU-04 parké).
- [ ] Tous champs saisissables pré-remplis par l'IA (sauf non factualisable).
- [ ] Isolation workspace testée ; gate BE-only + DROIT_FAMILLE.

## Plan de test
UT Calculator (3 vues × verdicts + formation impossible + dissolution unilatérale), IT endpoint (200 + 400 gate pays/domaine + 404 isolation), Jest composant (rendu form multi-vues + verdict + flush jurisprudence-citations).

## Tables / endpoints / composants
- Backend : migration `cohabitation_legale_be_analyses` (NNN — à pré-assigner), Calculator static + Input/Result + Request/Response + @Entity + Repository + Service + Controller (pattern F-217).
- Frontend : `cohabitation-legale-be-section.component` (+ .html/.scss/.spec + prefill-rules) + entrée `TOOL_REGISTRY` `cohabitation-legale-be` + `THEME_BY_TOOL` + seed `decision_tool_visibility_rules` (CONTEXTUAL, `DROIT_FAMILLE`/`BELGIQUE`, trigger `cohabitationLegaleBeDetectee`) + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- IA : flag pivot `cohabitationLegaleBeDetectee` **réutilisé** (pas de modif record).

## Invariants
CONTEXTUAL jamais ALWAYS_ON ; pré-fill F-246 ; visibility + KNOWN_NO_DASHBOARD ; 1 outil = 1 situation ; BE-only ; pas de citation jurisprudence BE.

## Hors périmètre
PACS FR (F-FA-12) ; cohabitation de fait (P4 F-224) ; génération de la déclaration à l'officier de l'état civil (outil de génération dédié potentiel, reporté).
