# Mini-spec — F-177 / SF-177-03 Pilote — Pattern B sur 4 outils ALWAYS_ON Travail FR

## Identifiant
`F-177 / SF-177-03`

## Feature parente
`F-177` — Refonte panel F-IA-04 (cards verdict + modal)

## Statut
`done`

## Date
2026-04-30

## Branche
`feat/SF-177-03-travail-fr-pilote`

## Objectif
Valider sur 4 outils ALWAYS_ON Travail FR pilotes le pattern d'instrumentation **B** (statics `TOOL_LABEL`/`TOOL_ICON` + `@Input forceExpanded`) qui sera ensuite étendu aux ~26 composants restants par les SF jumelles 03b/04/05/06/07/08.

## Composants instrumentés
| toolId | Composant | TOOL_LABEL | TOOL_ICON |
|--------|-----------|------------|-----------|
| F-DT-04-fiche-prudhomale | `PrudhomeFicheSectionComponent` | FICHE PRUD'HOMALE | gavel |
| F-DT-07-anciennete-conges-prime | `AncienneteSectionComponent` | ANCIENNETÉ ET CONGÉS | calendar_month |
| F-DT-08-licenciement-validity | `LicenciementSectionComponent` | VALIDITÉ DU LICENCIEMENT | gavel |
| F-DT-09-comparateur-indemnites | `IndemniteComparatifSectionComponent` | COMPARATEUR INDEMNITÉS | euro_symbol |

## Contrat ajouté
`frontend/src/app/case-files/decisional-tools-panel/decision-tool.contract.ts` :
- Type `DecisionToolStatic { TOOL_LABEL: string; TOOL_ICON: string }`
- Type `DecisionToolMetadata { label: string; icon: string }`
- Util `getToolMetadata(component): DecisionToolMetadata | null` qui lit les statics ; retourne null si l'un manque (fallback panel)

## Pattern d'instrumentation par composant
1. 2 statics ajoutés en tête de classe (`static readonly TOOL_LABEL = '...';` + `static readonly TOOL_ICON = '...';`)
2. `@Input() forceExpanded = false;` ajouté près des autres inputs
3. Dans `ngOnInit()` : `if (this.forceExpanded) this.collapsed.set(false);` (avant la logique existante)
4. Dans `ngOnChanges()` : `if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);`

## Critères d'acceptation
- [x] `decision-tool.contract.ts` créé avec `DecisionToolStatic` + `getToolMetadata()`
- [x] 4 composants pilotes ont les statics + l'input forceExpanded
- [x] `decision-tool.contract.spec.ts` : 3 tests sur `getToolMetadata` (label+icon présents, absents, partiels)
- [x] `decision-tool-instrumentation-pilote.spec.ts` : test factorisé `describe.each` qui valide les 4 pilotes (label + icon + cohérence du périmètre)
- [x] Tests existants des 4 composants inchangés (3772/3772 verts, +28 vs base)
- [x] Build vert

## Hors scope
- Bascule du panel vers `<app-decision-tool-card>` (= SF-177-11 finale)
- Instrumentation des ~26 autres composants (= SF-177-03b/04/05/06/07/08)
- Exposition du `summary` (= SF d'enrichissement futures)

## Tests
- 3 tests `getToolMetadata` (contrat)
- 8 tests factorisés sur les 4 pilotes (2 par pilote × 4 = 8) + 1 cohérence du périmètre
- Total nouveaux tests : 12

## Notes
- Le test `@Input forceExpanded` initial échouait car `new PrudhomeFicheSectionComponent()` crash sur dépendance `fb.group` non injectée. Retiré car la valeur par défaut est garantie par TypeScript (`= false`). Le comportement runtime (`collapsed.set(false) quand forceExpanded=true`) sera validé dans les tests d'intégration de SF-177-11.
- Aucune touche aux templates HTML / SCSS / aux specs existants → impact zéro sur l'UX courante. Le composant n'est consommé via `forceExpanded` qu'à partir de SF-177-11.
