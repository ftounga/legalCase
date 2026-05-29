# Mini-spec — F-JU-03 / SF-JU-03-100 — Instrumentation citations F-DT-75 (congés payés arrêt maladie)

## Identifiant
`F-JU-03 / SF-JU-03-100`

## Date
2026-05-29

## Branche Git
`feat/SF-JU-03-100-instrument-conges-payes-arret-maladie`

## Type
Bugfix / complétion — instrumentation résiduelle d'un outil ajouté au registre **après** la vague d'instrumentation F-JU-03 (clôturée 2026-05-25).

---

## Objectif (1 phrase)
Afficher le bloc « Jurisprudence applicable » (F-JU-01) sous le résultat de l'outil `F-DT-75-conges-payes-arret-maladie`, qui n'avait jamais été instrumenté.

## Contexte
Audit de couverture F-JU-01 du 2026-05-29 : l'outil `F-DT-75-conges-payes-arret-maladie` (calculateur des congés payés acquis pendant un arrêt maladie, jurisprudence Cassation majeure — arrêts du 13/09/2023) n'affiche aucune citation car son composant `conges-payes-arret-maladie-section` **ne contient pas** `<app-tool-jurisprudence-citations>`. De plus le mapping manuel correspondant avait été créé sous une **mauvaise clé** (`conges-payes-arret-maladie` au lieu de `F-DT-75-conges-payes-arret-maladie`) → orphelin.

## Comportement attendu

### Nominal
- Après calcul (résultat présent) et hors `standaloneMode`, le composant affiche le bloc `<app-tool-jurisprudence-citations [toolId]="F-DT-75-conges-payes-arret-maladie" [branchActive]="default">`, identique au pattern des autres outils instrumentés (réf. `conges-payes-section` F-DT-26).
- Le bloc charge les mappings via l'endpoint existant `GET /api/tools/{toolId}/jurisprudence-citations`.

### Cas d'erreur
| Situation | Comportement |
|---|---|
| `standaloneMode = true` (pas de dossier) | bloc non rendu (`@if (!standaloneMode)`) — comme les autres outils |
| Aucun mapping pour ce toolId | le composant citations gère l'état vide (rien affiché) — comportement existant F-JU-01 |

## Critères d'acceptation
- [ ] `conges-payes-arret-maladie-section.component.ts` importe `ToolJurisprudenceCitationsComponent` et l'ajoute à `imports`.
- [ ] Champs `toolIdForJurisprudence = 'F-DT-75-conges-payes-arret-maladie'` et `brancheActiveForJurisprudence = 'default'` ajoutés.
- [ ] Le HTML rend le bloc citations sous le résultat, guardé par `@if (!standaloneMode)`.
- [ ] `toolId` strictement égal à la clé du `TOOL_REGISTRY` (`F-DT-75-conges-payes-arret-maladie`).
- [ ] Tests Jest du composant verts (existants + éventuel ajout).
- [ ] Self-check grep pré-commit : le `toolId` du bloc == clé registre == clé du mapping DB.

## Plan de test
- **Jest** `conges-payes-arret-maladie-section.component.spec.ts` : le composant compile avec le nouvel import ; le bloc citations est présent dans le DOM quand `result()` est rendu et `standaloneMode=false`, absent si `standaloneMode=true`.
- **Isolation workspace** : N/A (le composant citations consomme la table globale `tool_jurisprudence_mappings`, déjà gérée par F-JU-01).

## Composants impactés
- `frontend/.../conges-payes-arret-maladie-section/conges-payes-arret-maladie-section.component.ts` (import + 2 champs)
- `…/conges-payes-arret-maladie-section.component.html` (bloc citations)
- `…/conges-payes-arret-maladie-section.component.spec.ts` (test)
- **Aucun backend, aucune migration.**

## Hors périmètre
- La re-création du mapping à la bonne clé `F-DT-75-conges-payes-arret-maladie` + archivage de l'orphelin `conges-payes-arret-maladie` = **opération data manuelle** (payload fourni hors code, via l'UI super-admin), pas dans cette SF.

## Préoccupations transversales
- **Outil décisionnel métier** : modifie l'output visuel d'un outil (ajout bloc citations). Invariant « un outil = une situation » respecté (aucun outil ajouté). Additif pur, pattern identique aux ~120 outils déjà instrumentés.
- Auth/workspace/navigation : non concernés.
