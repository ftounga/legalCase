# Mini-spec — F-165 / SF-165-FIX Restauration visibilité F-DT-08 & F-DT-09 (Travail FR)

## Identifiant
`F-165 / SF-165-FIX-fdt08-09-visibility-fr`

## Feature parente
`F-165` (F-IA-04) — système de visibilité des outils décisionnels par couches (ALWAYS_ON / CONTEXTUAL / catalog).

## Statut
`ready`

## Date de création
2026-06-03

## Branche Git
`fix/fdt08-09-visibility-fr`

## Type
**Bugfix de régression en production** (exempté étapes 0 / 0 bis — aucun élément d'écran nouveau : on rétablit un affichage existant).

---

## Contexte / diagnostic

Bug confirmé 2026-06-03 (révélé en testant la jurisprudence sur `dossier-jurisprudence-lemaire`, licenciement FR). Les outils **F-DT-08-licenciement-validity** et **F-DT-09-comparateur-indemnites** n'apparaissent **nulle part** (ni `alwaysOn`, ni `contextual`, ni `catalog`) sur tout dossier de **licenciement Travail FR**, en staging et en prod.

**Cause racine** (confirmée par audit déterministe H2 + simulateur, 0 divergence) :
- Migration **106** : `DELETE … tool_id IN (F-DT-08, F-DT-09) AND layer='CONTEXTUAL'` → supprime leurs règles CONTEXTUAL `type_rupture`.
- Migration **194** : supprime leurs règles `ALWAYS_ON` FR, sur l'hypothèse **fausse** que « les CONTEXTUAL prennent le relais » (déjà supprimés par 106).
- Résultat : **0 règle de visibilité FR** pour ces 2 outils. (La Belgique conserve un ALWAYS_ON → non touchée. L'extraction IA est hors de cause : `type_rupture=LICENCIEMENT` est correctement détecté.)

Mécanique : `DecisionToolVisibilityService.buildResponse()` ne place dans `catalog` que les outils ayant ≥1 règle CONTEXTUAL → un outil sans aucune règle est totalement invisible.

---

## Objectif (une phrase)
Rétablir les règles de visibilité CONTEXTUAL `type_rupture` de F-DT-08 et F-DT-09 en Travail FR pour qu'ils réapparaissent sur les dossiers de licenciement, et empêcher toute régression future du même type par un test d'intégrité.

---

## Comportement nominal
Sur un dossier `DROIT_DU_TRAVAIL / FRANCE` dont l'analyse IA a détecté `type_rupture ∈ {LICENCIEMENT, LICENCIEMENT_ECONOMIQUE}` :
- **F-DT-08** (validité du licenciement) et **F-DT-09** (comparateur d'indemnités) apparaissent dans la couche `contextual` du panneau d'outils décisionnels.
- Sur un dossier où `type_rupture` n'est pas un licenciement, ils restent dans `catalog` (ouvrables manuellement) — comportement F-165 attendu, pas de sur-affichage.

## Cas d'erreur / bords
- Dossier sans `type_rupture` détecté → F-DT-08/09 dans `catalog` (visible manuellement), pas en `contextual`.
- Belgique : inchangé (ALWAYS_ON BE conservé).
- Idempotence migration : les UUID des nouvelles règles sont neufs et uniques (pas de collision avec l'existant).

---

## Solution technique

### 1. Migration `544-restore-fdt08-fdt09-contextual-travail-fr.xml`
Réinsère, pour `legal_domain='DROIT_DU_TRAVAIL'`, `country='FRANCE'`, `layer='CONTEXTUAL'`, `trigger_field='type_rupture'` (priorités identiques au seed initial 105) :

| tool_id | trigger_value | priority |
|---------|---------------|----------|
| F-DT-08-licenciement-validity | LICENCIEMENT | 10 |
| F-DT-08-licenciement-validity | LICENCIEMENT_ECONOMIQUE | 10 |
| F-DT-09-comparateur-indemnites | LICENCIEMENT | 20 |
| F-DT-09-comparateur-indemnites | LICENCIEMENT_ECONOMIQUE | 20 |

- Colonnes exactes vérifiées sur le `createTable` amont : `id, legal_domain, country, tool_id, layer, trigger_field, trigger_value, priority` (cf. [[feedback-liquibase-insert-column-check]] — `layer`, pas `visibility`).
- UUID neufs sous le namespace existant `f1a04001-0000-0000-0000-…` (préfixe dédié `fd08`/`fd09`), vérifiés absents.
- Bloc `<rollback>` qui DELETE ces 4 UUID.
- **On ne réintroduit PAS** `RUPTURE_CONVENTIONNELLE` pour F-DT-09 (retrait volontaire par migration 196).

### 2. Garde-fou anti-régression (test d'intégration)
Étendre/compléter `DecisionToolVisibilityIntegrityIT` : pour le scope `(DROIT_DU_TRAVAIL, FRANCE)`, asserter que **F-DT-08 et F-DT-09 ont ≥ 1 règle de visibilité** (ALWAYS_ON ou CONTEXTUAL). Le test existant couvre le sens « règle → entrée frontend » ; on ajoute le sens manquant « outil licenciement core → a bien une règle ».

---

## Critères d'acceptation (vérifiables)
1. Après migration, `SELECT count(*) FROM decision_tool_visibility_rules WHERE tool_id IN ('F-DT-08-licenciement-validity','F-DT-09-comparateur-indemnites') AND legal_domain='DROIT_DU_TRAVAIL' AND country='FRANCE'` = **4**.
2. `GET /api/v1/case-files/{id}/decision-tools-visibility` sur un dossier licenciement FR (type_rupture=LICENCIEMENT) renvoie **F-DT-08 et F-DT-09 dans `contextual`**.
3. Le nouveau test d'intégrité échoue si l'une des 4 règles est absente (preuve par suppression locale).
4. Belgique inchangée ; aucun autre outil affecté.
5. Build backend vert (`mvn test`).

## Plan de test minimal
- **Migration** : test Liquibase / IT qui boot le contexte et vérifie le count = 4 (critère 1).
- **Intégration** : `DecisionToolVisibilityServiceIT` — un dossier simulé avec `compensation_data.type_rupture=LICENCIEMENT` → F-DT-08/09 dans `contextual`.
- **Garde-fou** : `DecisionToolVisibilityIntegrityIT` — assertion ≥1 règle pour F-DT-08/09 FR.
- **Isolation workspace** : sans objet (donnée de référence globale, pas de `workspace_id`).
- **Manuel staging** : rejouer TEST 2 du plan jurisprudence sur `dossier-jurisprudence-lemaire` → F-DT-08/09 visibles, bloc « Jurisprudence applicable » présent.

---

## Tables / endpoints / composants impactés
- **Table** : `decision_tool_visibility_rules` (INSERT de 4 lignes via migration).
- **Endpoint** : `GET /api/v1/case-files/{id}/decision-tools-visibility` (comportement restauré, code inchangé).
- **Backend** : `DecisionToolVisibilityService` (inchangé) ; `DecisionToolVisibilityIntegrityIT` (test étendu).
- **Frontend** : aucun changement — F-DT-08/09 ont déjà leur entrée `TOOL_REGISTRY` (cf. [[feedback-pre-merge-visibility-seed-check]], vérifié : présents dans `decisional-tools-panel.component.ts`).

### Préoccupation transversale cochée : **Outil décisionnel métier**
Composants impactés listés ci-dessus. Pas d'impact Auth/Principal, Workspace, Plans/limites, Navigation/routing. Donc smoke E2E auth/nav non requis ; validation manuelle staging du panneau d'outils suffisante.

---

## Hors périmètre
- Famille BE (F-153, F-FA-01/02/04) : re-scoping FR-only volontaire (équivalents BE dédiés existent) — **non touché**.
- Outils décommissionnés (F-DT-01, F-DT-05) : sans composant frontend — **non touché**.
- Refonte du mécanisme de visibilité ou des migrations historiques (106/194/196) : on corrige par ajout, on ne réécrit pas l'historique Liquibase.
