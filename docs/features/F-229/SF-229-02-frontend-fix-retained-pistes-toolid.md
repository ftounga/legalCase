# Mini-spec — F-229 / SF-229-02 Frontend — Fix toolId tile « Pistes stratégiques retenues » : aligner frontend sur backend

## Identifiant

`F-229 / SF-229-02`

## Statut

`draft` — 2026-05-09

## Branche Git

`feat/SF-229-02-frontend-fix-retained-pistes-toolid`

## Pattern de référence

Aucun — fix ciblé d'une incohérence backend/frontend héritée de SF-192-02.

---

## Objectif

Aligner le toolId checké côté frontend (`'RETAINED_PISTES_SUMMARY'`) sur le toolId réellement envoyé par le backend (`'F-192-retained-pistes-summary'`) pour que la tile « Pistes stratégiques retenues » du dashboard décisionnel soit à nouveau cliquable.

---

## Comportement attendu

### Avant (bug observé staging Immigration Chen 17, 2026-05-09)

1. Avocat clique sur la tile « Pistes stratégiques retenues » du dashboard
2. `case-dashboard.openGenericTool('F-192-retained-pistes-summary')` (toolId envoyé par le backend)
3. Aucun `if` ne match (le code check `'RETAINED_PISTES_SUMMARY'`)
4. Fallback `TOOL_REGISTRY.get(toolId)` → `undefined` (pas un outil instanciable)
5. `console.warn('[case-dashboard] Unknown toolId for generic tile: F-192-retained-pistes-summary')` + `return` → rien ne se passe

### Après fix

1-2 : identique
3. Le `if` match `'F-192-retained-pistes-summary'`
4. `BadgeNavigationService.go('pistes', caseFileId)` invoqué (CA-02 F-229 SF-229-01)
5. Navigation `/case-files/:id/synthesis` + fragment `section-pistes`
6. `SynthesisComponent` souscrit à `route.fragment` → `scrollToBlock('section-pistes')` au mount

---

## Critères d'acceptation

- [ ] **CA-01** : `case-dashboard.component.ts:259` — string littéral `'RETAINED_PISTES_SUMMARY'` remplacé par `'F-192-retained-pistes-summary'`. Commentaire ligne 253 mis à jour pour refléter le nouvel ID.
- [ ] **CA-02** : `case-dashboard.component.spec.ts` — 5 occurrences de `'RETAINED_PISTES_SUMMARY'` remplacées par `'F-192-retained-pistes-summary'` (lignes 421, 423, 437, 470, 475, 481, 483, 949). **Important** : ces tests passaient avec un toolId mocké qui ne correspondait PAS à ce que le backend envoie — le bug n'était pas détectable en CI.
- [ ] **CA-03** : nouveau test régression — `case-dashboard.component.spec.ts` vérifie explicitement que le toolId du test correspond à la valeur backend. Idéalement, importer la constante depuis le backend (DTO partagé) ; à défaut, ajouter un commentaire explicit sur la source du string.
- [ ] **CA-04** : aucune régression sur les tests existants `case-dashboard.component.spec.ts` (58/58 verts attendus) ni `badge-navigation.service.spec.ts` (8/8) ni `synthesis.component.spec.ts` (150/150).
- [ ] **CA-05** : test manuel staging post-deploy — clic sur la tile « Pistes stratégiques retenues » → navigation `/synthesis` + scroll fluide vers le bloc Pistes (comportement F-229 SF-229-01 attendu).

---

## Périmètre

### Hors scope V1

- (a) Audit exhaustif de tous les toolIds frontend vs backend (autres tiles peuvent avoir le même problème mais elles fonctionnent visiblement → si ça marche, c'est qu'elles match). Audit complet en V2 si signal terrain.
- (b) Refactor pour partager les constantes toolId entre backend et frontend (DTO TypeScript généré, etc.) — V2.

---

## Technique

### Fichiers à modifier

1. `frontend/src/app/case-files/case-dashboard/case-dashboard.component.ts` :
   - Ligne 253 commentaire mis à jour
   - Ligne 259 string littéral corrigé

2. `frontend/src/app/case-files/case-dashboard/case-dashboard.component.spec.ts` :
   - 8 occurrences de `'RETAINED_PISTES_SUMMARY'` remplacées par `'F-192-retained-pistes-summary'`

### Aucune migration backend, aucun nouvel endpoint

Le backend `CaseFileDashboardService.java:525` reste inchangé — c'est lui la source de vérité, le frontend s'aligne.

---

## Plan de test

### Tests Jest

- `case-dashboard.component.spec.ts` : 8 tests adaptés (string remplacé), 1 nouveau test régression qui assert le toolId
- Lancer `npx jest --testPathPattern="case-dashboard"` → tous verts
- Lancer `npm run build` → BUILD SUCCESS

### Test manuel staging

1. Dossier Immigration Chen 17
2. Clic tile « Pistes stratégiques retenues » → URL change vers `/case-files/:id/synthesis#section-pistes` + scroll automatique vers le bloc Pistes

---

## Dépendances

- F-229 SF-229-01 ✅ (`BadgeNavigationService.go('pistes')`)
- F-192 SF-192-01 ✅ (backend tile `F-192-retained-pistes-summary`)

---

## Impact par domaine métier

Transversal — UI navigation, aucune adaptation par domaine.

---

## Analyse de cohérence transversale

- **Auth/Principal** : N/A (frontend).
- **Workspace context** : N/A.
- **Plans/limites** : N/A.
- **Navigation/routing** : N/A — pas de modif de route, juste un alignement de string.
- **Outil décisionnel métier** : N/A.
- **Pattern partagé** : N/A — pas de nouveau pattern.

---

## Risques

- **Régression nulle attendue** : seul le toolId checké change. Le comportement (navigation `pistes` via `BadgeNavigationService`) reste celui défini par F-229 SF-229-01.
- **Couverture de tests historique fausse** : les tests utilisaient un mock incorrect. Ce fix corrige le mock + la check. À l'avenir, idéalement, les constantes toolId devraient venir d'une source partagée (V2).

---

## Notes

- **Décision 2026-05-09** : alignement frontend sur backend (cohérent avec F-194/195/196 qui utilisent déjà le pattern `F-XXX-...`). Le backend n'est pas modifié.
- **Origine** : bug staging Immigration Chen 17 rapporté 2026-05-09 par utilisateur via console navigateur — `[case-dashboard] Unknown toolId for generic tile: F-192-retained-pistes-summary`. Préoccupation 2 du test Chen 17 du 2026-05-08 ("tile pistes retenues clic = rien") faussement présumée résolue par F-229 SF-229-01 qui n'a corrigé que le handler, pas le toolId.
