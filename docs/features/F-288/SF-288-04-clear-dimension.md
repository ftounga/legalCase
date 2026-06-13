# Mini-spec — F-288 / SF-288-04 — Correctif : « tout recoché » ne ré-incluait pas (composition non vidable)

> **Bugfix** (exempt étapes 0 / 0 bis). Régression trouvée à la **validation staging 2026-06-13** de F-288.

## Identifiant
`F-288 / SF-288-04`

## Statut
`draft`

## Branche
`fix/SF-288-04-clear-dimension`

## Bug constaté (staging, reproduit)
Sur `staging.legalcase.fr`, dossier e2e, outil `F-DT-07-anciennete-conges-prime` calculé :
1. `PUT composition {exclusions:[{DECISION_TOOL, F-DT-07}]}` → `included=false` ✅
2. « tout recoché » → le frontend envoie `PUT {exclusions:[]}` → l'outil **reste `included=false`** ❌

**Cause** : `putComposition` ne remettait à plat (delete+insert) que les dimensions **présentes dans les entrées `exclusions`**. Un body `exclusions: []` ne « portait » aucune dimension → aucun delete → l'exclusion précédente restait collée. L'avocat ne pouvait plus **ré-inclure** un élément une fois qu'il devenait le dernier exclu de sa dimension.

## Correctif
Le client **déclare les dimensions qu'il gère** dans le PUT, pour pouvoir les vider même sans exclusion.
- **Contrat étendu** : `PUT …/composition` body = `{ "dimensions": ["DECISION_TOOL", "ADVERSE_MOYEN"], "exclusions": [ … ] }`. `dimensions` = clés des dimensions affichées par le modal. Le backend remet à plat **chaque dimension déclarée** (delete) puis insère les exclusions ; rétro-compatible (`dimensions` absent → ancien comportement « dimensions présentes dans exclusions »).
- **Backend** : `CompositionUpdateRequest` gagne `List<String> dimensions` (constructeur de compat. conservé pour SF-288-01) ; `putComposition` unionne dimensions déclarées + dimensions des entrées, valide (400 si inconnue), delete chacune, insert.
- **Frontend** : `saveComposition(caseFileId, exclusions, dimensions)` envoie `dimensions` = `composition.dimensions.map(d => d.key)` ; `conclusions-section` passe les clés affichées.

## Critères d'acceptation
- [ ] Après exclusion d'un outil puis « tout recoché » (PUT `dimensions:[DECISION_TOOL], exclusions:[]`), l'outil redevient `included=true`.
- [ ] Indépendance des dimensions préservée (un PUT déclarant `DECISION_TOOL` n'efface pas les exclusions `ADVERSE_MOYEN`).
- [ ] Dimension déclarée inconnue → 400.
- [ ] Rétro-compat : body sans `dimensions` (ancien client) → comportement SF-288-01 inchangé.

## Tests
- Back : `put_declaredDimensionWithEmptyExclusions_clearsThatDimension`, `put_declaredUnknownDimension_throws400` (+ existants verts).
- Front : assertion du body PUT `{ dimensions:['DECISION_TOOL'], exclusions:[…] }`.

## Hors scope
- Tout autre comportement de F-288 (inchangé).

## Validation staging
À re-dérouler après déploiement : exclure → « tout recoché » → `included=true` ; puis nettoyer l'exclusion résiduelle laissée par le bug sur le dossier e2e.
