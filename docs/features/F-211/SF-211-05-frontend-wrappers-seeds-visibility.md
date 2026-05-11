# SF-211-05 — Wrappers Angular + seeds visibility F-211 Famille BE

**Statut** : Done — 2026-05-11
**Type** : Frontend wrappers + seed visibility (dette explicite F-211)

## Objectif

Combler la dette de F-211 (4 calculators backend mergés sans wrappers Angular ni
seeds `decision_tool_visibility_rules`). Sans cette SF, les 4 outils Famille BE
livrés en SF-211-01/02/03/04 sont silencieusement invisibles dans le panel
F-IA-04 (CLAUDE.md `feedback_pre_merge_visibility_seed_check` symétrique).

## Périmètre

4 composants Angular **informationnels (V2)** + 4 entrées `TOOL_REGISTRY` +
4 seeds visibility (1 migration Liquibase 228) + 4 entrées `THEME_BY_TOOL_ID`.

Les composants livrés sont des **placeholders informationnels** : titre + cadre
juridique + note "Composant de saisie complet à livrer dans une SF ultérieure".
Pas d'appel HTTP, pas de formulaire, pas de pré-fill IA. Étiquetage
`PREFILL_COUNT_ALWAYS_ZERO = true` (F-237 SF-237-02) pour exempter du test
parité runtime/static.

## Comportement nominal

- Sur dossier Famille **BE** avec flag pivot adéquat coché (`divorce_dc_envisage`,
  `divorce_ddi_envisage` ou `pacte_successoral_envisage`) → outil apparaît dans
  panel F-IA-04, section catalogue cliquable F-238.
- Outil `tribunal-famille-be-mesures-prov` apparaît toujours sur dossier Famille BE
  (mode ALWAYS_ON — mesures urgentes transversales).
- Clic sur la card → composant placeholder s'ouvre avec message explicite
  "Outil livré : back-end opérationnel. Saisie interactive à venir."

## Cas d'erreur

Aucun (composants pure-info, pas d'appel réseau).

## Critères d'acceptation

- [x] 4 nouveaux dossiers `frontend/src/app/case-files/divorce-dc-be-section/`,
  `divorce-ddi-be-section/`, `tribunal-famille-be-mesures-provisoires-section/`,
  `pacte-successoral-be-2018-section/` créés avec 4 fichiers chacun (`*.ts`/`.html`/`.scss`/`.spec.ts`).
- [x] 4 classes exposent `TOOL_LABEL`, `TOOL_ICON`, `PREFILL_COUNT_ALWAYS_ZERO=true`,
  `static getPrefillCount(): number` retournant 0.
- [x] 4 entrées `TOOL_REGISTRY` ajoutées dans `decisional-tools-panel.component.ts`
  avec `displayLabel` humain (cohérent F-238 SF-238-01).
- [x] 4 mappings `THEME_BY_TOOL_ID` (théme VALIDITE pour les 2 divorces, DELAIS
  pour les mesures provisoires, VALIDITE pour le pacte successoral).
- [x] Migration `228-seed-f211-famille-be-visibility-rules.xml` ajoute 4 INSERT
  dans `decision_tool_visibility_rules`.
- [x] Tests Jest minimum (création / toggle / forceExpanded / TOOL_LABEL+ICON
  statics) sur chaque wrapper — 12 tests verts au total.
- [x] `DecisionToolVisibilityIntegrityIT` + `DecisionToolDisplayLabelIntegrityIT`
  + `prefill-count-integrity.spec.ts` passent.

## Plan de test

- Jest × 4 composants × 3 tests = 12 tests unitaires (rendu, toggle, statics).
- IT backend `DecisionToolVisibilityIntegrityIT` (extraction dynamique, vérifie
  qu'aucun tool_id seedé n'est orphelin frontend).
- IT backend `DecisionToolDisplayLabelIntegrityIT` (vérifie displayLabel non vide
  + non-self-référençant).
- Jest `prefill-count-integrity.spec.ts` (vérifie `getPrefillCount({}) === 0`
  pour les entrées étiquetées `PREFILL_COUNT_ALWAYS_ZERO=true`).
- Build frontend `npm run build` sans warning.

## Tables / endpoints / composants impactés

- **Frontend** : `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts`
  (+ 4 imports + 4 entrées `TOOL_REGISTRY` + 4 entrées `THEME_BY_TOOL_ID`).
- **Backend** : migration Liquibase 228 (4 INSERT dans
  `decision_tool_visibility_rules`).
- **Aucun service Angular** (composants pure-info, pas de HTTP).

## Hors périmètre

- Composants de saisie interactifs (formulaires, pré-fill IA, F-IA-03,
  intégration `*PrefillRules`) — livrés en SF ultérieures (F-211+1 / F-2XX).
- Audit Country × Domain F-166 / parité FR (Belgique-spécifique pure, pas
  d'équivalent FR strict pour pacte successoral 2018 ou DDI 3 voies CC art. 229).

## Analyse de cohérence transversale

| Cible | Statut |
|-------|--------|
| Autres outils décisionnels Famille BE (F-FA-11, F-FA-07) | Pattern aligné — composants info-only ou wrappers minimaux préexistent |
| Pattern wrapper info-only | Référence : `rupture-amiable-info-section` (SF-132-03) — reproduit fidèlement |
| `TOOL_REGISTRY` entries autres outils BE | Cohérence : `inputs: (ctx) => ({ caseFileId })` (pas de sources IA, composant info pur) |
| Garde-fou F-164 visibility | Test extraction dynamique → aucune action hardcodée requise |
| Garde-fou F-238 displayLabel | Labels humains conformes — pas de tool_id dans le label |
| Garde-fou F-237 prefill-count | Étiquette `PREFILL_COUNT_ALWAYS_ZERO=true` (branche exemption SF-237-01) |

## Impact par domaine métier

- **Droit du travail** : non applicable (Famille pur).
- **Immigration** : non applicable (Famille pur).
- **Famille** : applicable, mais **Belgique uniquement** — pas d'équivalent FR
  strict pour ces 4 outils. CJ 1287+ (DC), CC art. 229 §§1/2/3 (DDI), CJ 1280
  (mesures provisoires), Loi 31/07/2017 (pacte successoral 2018) sont des
  sources juridiques nationales BE pures (cf. règle CLAUDE.md "Belgique never
  forget" — couverture exhaustive BE attendue, pas miroir FR).

## Audit F-166 Country × Domain (seeds visibility)

| Cellule | Avant | Après |
|---------|-------|-------|
| BE × DROIT_FAMILLE | F-FA-07 / F-FA-11 / F-152 + outils existants | + `divorce-dc-be` (CONTEXTUAL), `divorce-ddi-3voies-be` (CONTEXTUAL), `tribunal-famille-be-mesures-prov` (ALWAYS_ON), `pacte-successoral-be-2018` (CONTEXTUAL) |
| Autres cellules | inchangé | inchangé |

Aucun conflit de mode (ALWAYS_ON tribunal-famille reflète sa nature
"mesures provisoires urgentes transversales", indépendamment du type de
procédure familiale en cours).

## Migration UUID namespace

`f1a04001-0000-0000-0000-eeee21100XXX` (préfixe `eeee21100` dédié F-211).
