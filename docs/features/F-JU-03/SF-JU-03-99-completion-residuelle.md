# Mini-spec — F-JU-03 / SF-JU-03-99 Complétion résiduelle audit 2026-05-24

## Identifiant
`F-JU-03 / SF-JU-03-99`

## Feature parente
`F-JU-03` — Instrumentation des outils décisionnels pour F-JU-01 + F-JU-02

## Statut
`En cours` — correctif post-audit, fait suite aux 4 PRs frontend mergées 2026-05-23 (#1286/#1287/#1288/#1289)

## Date de création
2026-05-24

## Branche Git
`feat/SF-JU-03-99-completion-residuelle`

---

## Objectif

Combler en une seule PR les 6 trous détectés par l'audit cross-référence
`TOOL_REGISTRY × backend × frontend` du 2026-05-24, sans toucher au reste
de l'instrumentation déjà livrée :

1. **Typo `lourd`/`lourde`** dans `licenciement-faute-grave-lourd-section.component.ts:71` —
   le frontend instrumente `F-DT-36-licenciement-faute-grave-lourd` (sans `e`)
   alors que le `TOOL_REGISTRY` et le backend déclarent `F-DT-36-licenciement-faute-grave-lourde` (avec `e`).
   Conséquence : l'outil est totalement orphelin côté F-JU-01 — aucune citation
   ne s'affichera jamais sous son résultat.
2. **5 outils backend-only** (`ToolBranchRegistry` + `ToolUsageContributor` présents,
   composant frontend pas encore enrichi) : ajout du composant
   `<app-tool-jurisprudence-citations>` + 2 propriétés + 1 import dans :
   - `csp-crp-fr-section` (`F-DT-44-csp-crp-conformite`)
   - `transfert-entreprise-fr-section` (`F-DT-72-transfert-entreprise-l1224-1`)
   - `faute-inexcusable-fr-section` (`F-DT-91-faute-inexcusable-employeur`)
   - `belgian-40bis-section` (`F-IM-14-40bis-cohabitant-ue-be`)
   - `licenciement-faute-grave-lourd-section` (`F-DT-36-licenciement-faute-grave-lourde`)
     — déjà partiellement instrumenté mais avec le mauvais toolId (point 1).

Pattern de référence : `docs/features/F-JU-03/SF-JU-03-01-vague-travail-fr-pilote.md`
section « 3. Frontend — Composant section ». Reproduit verbatim sur les 5 sections.

---

## Périmètre

### En scope
- 1 fix typo + 4 ajouts du composant frontend dans les 4 sections back-only (sans backend, déjà présent)
- Mise à jour de `PRODUCT_SPEC.md` post-merge (étape 6)

### Hors scope
- Les ~33 outils éligibles non instrumentés du tout (à arbitrer outil par outil dans une vague suivante)
- Toute modification backend (les 5 `ToolBranchRegistry` et `ToolUsageContributor` existent déjà)
- Logique de branche fine (V2 — V1 utilise `default`)

---

## Critères d'acceptation
- [ ] `licenciement-faute-grave-lourd-section.component.ts:71` : `toolIdForJurisprudence` corrigé en `F-DT-36-licenciement-faute-grave-lourde` (avec `e`)
- [ ] 4 sections frontend supplémentaires importent `ToolJurisprudenceCitationsComponent`, le déclarent dans `imports: []`, exposent les 2 propriétés `toolIdForJurisprudence` + `brancheActiveForJurisprudence` avec les bons toolIds, et insèrent le composant dans le HTML après le bloc résultat
- [ ] Frontend Angular compile sans erreur
- [ ] Tests Jest existants des 5 composants restent verts (aucune régression)
- [ ] Audit cross-référence post-PR : 47 outils ENTIÈREMENT instrumentés (back + front), 0 outil backend-only, 0 typo

## Analyse de cohérence transversale
- [x] **Préoccupations transversales** : aucune (correctif additif, pas de modif backend, pas de modif d'API)
- [x] **Composant partagé** : `<app-tool-jurisprudence-citations>` (déjà livré SF-JU-01-04)
- [x] **Outil décisionnel métier** : l'invariant CLAUDE.md « un outil décisionnel = une situation métier » est respecté — pas d'ajout d'outil, juste correctif d'instrumentation

## Conformité F-IA-04 / Pré-fill IA
- [x] Non applicable (correctif d'instrumentation sur outils existants)

## Plan de test
- Frontend : suites Jest existantes (5 composants × leurs tests propres) doivent rester vertes
- Audit Python `TOOL_REGISTRY × backend × frontend` post-PR doit reporter 0 outil backend-only et 0 frontend-orphelin

## Notes
1. Tâche atomique : 5 fichiers `.ts` + 5 fichiers `.html` modifiés, ~5 lignes ajoutées par paire.
2. Visible en prod uniquement après bootstrap d'un mapping `tool_jurisprudence_mappings` pour ces toolIds (geste ops via dashboard).
3. Le contexte de l'écart est documenté dans la ligne 2026-05-24 du journal `PRODUCT_SPEC.md` (commit `f6a88d37`).

### Coût estimé
- ~15 min dev (édition pure mécanique sur pattern figé)
- ~5 min tests
- ~5 min PR
