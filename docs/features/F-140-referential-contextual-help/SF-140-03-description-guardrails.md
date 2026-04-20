# Mini-spec — F-140 / SF-140-03 Garde-fous sur description

## Identifiant · `F-140 / SF-140-03`
## Date · `2026-04-20` · Branche · `feat/SF-140-03-description-guardrails`

## Objectif
Pérenniser le dispositif SF-140-02 pour que **toute nouvelle entry** (migration seed ou création admin via F-110) porte une description — sinon on re-créerait silencieusement le trou qu'on vient de combler.

## 3 garde-fous

### A — Test d'intégrité CI (automatique)
`LegalReferentialDescriptionIntegrityIT` parcourt toutes les entries `is_system=true` et vérifie `description` non null et non blank. Les 7 types à description riche native dans le JSON sont exemptés (liste dans le test). Toute migration oubliant `description` casse la CI.

### B — Champ description dans le dialog F-110 d'édition
`ReferentialEditDialogComponent` reçoit un `FormControl description` commun à tous les types (ajouté via `form.addControl` après `buildForm`). Textarea 4 lignes + compteur 2000 chars. `ReferentialEditDialogResult` inclut `description`. `ReferentialService.updateReferential` passe le champ. Backend `ReferentialUpdateRequest` + `LegalReferentialService.updateReferential` supportent le champ via surcharge rétrocompatible (null ou blank = description non modifiée).

### C — Convention CLAUDE.md
Nouvelle règle de blocage :
> Migration Liquibase qui INSERT une entry legal_referentials avec is_system=true sans la colonne description → REFUS. Exception : les 7 types à description riche native dans value_json.

## Critères d'acceptation
- [x] `LegalReferentialDescriptionIntegrityIT` créé (2 tests, PASS)
- [x] Backend surcharge `updateReferential` rétrocompat avec paramètre description
- [x] Frontend : model + service + dialog + submit passent description
- [x] Règle CLAUDE.md ajoutée
- [x] 961 backend + 1063 frontend verts

## Hors scope
- Re-valider manuellement les ~102 descriptions actuelles (peuvent être raffinées au fil du temps via F-110)
- Étendre le test à d'autres colonnes (source_ref, etc.)
