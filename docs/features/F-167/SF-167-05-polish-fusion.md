# Mini-spec — F-167 / SF-167-05 Polish dashboard — groupement thème + tri alertLevel + fusion records typés legacy

> Dernière SF de la séquence F-167. Polish + refactoring final pour fusionner
> les 9 records typés legacy (`LicenciementSummary`, `IndemniteSummary`, etc.)
> dans le nouveau pattern générique `DashboardTile`.

---

## Identifiant `F-167 / SF-167-05`
## Branche Git `feat/SF-167-05-polish-fusion`
## Date `2026-05-02`
## Statut `draft`

---

## Objectif

3 axes :
1. **Frontend** : grouper les tiles par thème métier (`INDEMNITES` / `VALIDITE` / `DELAIS` / `DOCUMENTS` / `DIAGNOSTIC`) avec un titre de section par thème (réutilise `THEME_BY_TOOL_ID` + libellés F-169).
2. **Frontend** : tri par `alertLevel` décroissant (ALERT > WARNING > OK > null) à l'intérieur de chaque thème.
3. **Backend + Frontend** : supprimer les 9 records typés legacy (`LicenciementSummary`, `IndemniteSummary`, `AncienneteSummary`, `TitleDecisionSummary`, `WorkRightSummary`, `RecoursSummary`, `PartageSummary`, `GardeSummary`, `DivorceSummary`) du `CaseFileDashboardResponse` car remplacés par les 9 tiles génériques équivalentes (assemblées par SF-167-01). Refactor frontend `case-dashboard.component` qui les lisait.
4. **Frontend** : état vide propre (message "Aucun outil exécuté pour ce dossier" si `tiles.length === 0`).

## Critères d'acceptation

- [ ] Les tiles sont groupées par 5 sections thématiques avec titre.
- [ ] Au sein d'un thème, les tiles `ALERT` apparaissent en premier, puis `WARNING`, puis `OK`, puis sans alertLevel.
- [ ] Les 9 records typés legacy supprimés du backend ; le frontend ne les lit plus.
- [ ] L'endpoint `GET /api/v1/case-files/{id}/dashboard` reste rétro-compatible (le champ `tiles` couvre tout ce que les 9 records typés couvraient).
- [ ] État vide s'affiche correctement.
- [ ] Tests : suite Jest + IT verts, aucune régression.

## Hors scope

- Drag-and-drop entre tiles
- Filtre/recherche sur les tiles
- Export PDF du dashboard

## Cohérence transversale

- `THEME_BY_TOOL_ID` (F-169) : peut nécessiter une migration vers un référentiel partagé front+back (à évaluer ; pour l'instant duplication acceptable).
- Suppression des 9 records typés : risque de régression sur les consommateurs API tiers (vérifier qu'aucun n'existe — endpoint interne uniquement).

## Préoccupations transversales

Aucune.

## Dépendances

- SF-167-01/02/03/04 mergées — toutes les `*Analysis` doivent être couvertes par des mappers `DashboardTile` AVANT de supprimer les records typés.
