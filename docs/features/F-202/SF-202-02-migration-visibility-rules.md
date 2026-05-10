# SF-202-02 — Migration `decision_tool_visibility_rules` Famille BE

## Objectif
Basculer le seul outil Famille BELGIQUE existant (`F-FA-11-desunion-irremediable-be`) de `ALWAYS_ON` à `CONTEXTUAL` avec trigger sur le flag `divorce_ddi_envisage` livré par SF-202-01. Préparer le namespace UUID `eeee20200XXX` pour les futurs outils Famille BE livrés en F-211/F-217+ (cohabitation légale, pacte successoral, kafala, etc.).

## Comportement nominal
- Migration Liquibase 217 : DELETE 1 entrée ALWAYS_ON Famille BE (F-FA-11), INSERT 1 entrée CONTEXTUAL avec `trigger_field = 'divorce_ddi_envisage'` et `trigger_value = 'true'`.
- L'outil bipays `F-FA-23-ordonnance-requete` reste ALWAYS_ON BE (situation procédurale d'urgence, voie unilatérale toujours pertinente — bascule possible ultérieurement avec un flag d'urgence familiale).
- Les 4 autres flags BE livrés par SF-202-01 (`divorce_dc_envisage`, `cohabitation_legale_be_detectee`, `pacte_successoral_envisage`, `kafala_recueil_detecte`) sont déjà extraits par l'IA mais sans rule CONTEXTUAL active : ils servent les outils MANQUE Famille BE futurs (F-211/F-217+).

## Cas d'erreur
- Doublon UUID : namespace `f1a04001-0000-0000-0000-eeee20200XXX` distinct des autres (F-166 `eeee20000`, F-201 `eeee20100`, F-203 `eeee20300`, F-204 `eeee20400`).
- Réversibilité : rollback restaure exactement l'entrée ALWAYS_ON F-FA-11 initiale (priority 73, valeur précédente).

## Critères d'acceptation
1. L'outil Famille BE `F-FA-11-desunion-irremediable-be` bascule CONTEXTUAL via migration 217.
2. UUID `eeee20200111` sans collision (vérifié par recherche dans toutes les migrations existantes).
3. `DecisionToolVisibilityIntegrityIT` reste vert (le `tool_id` était déjà dans `KNOWN_FRONTEND_TOOL_IDS`).
4. Sur dossier Famille BE sans flag `divorce_ddi_envisage=true`, le panel F-IA-04 n'affiche plus F-FA-11 par défaut (conserve uniquement F-FA-05 / F-FA-06 / F-FA-07 transversaux + F-FA-23 bipays + tout ce qui est BE-friendly).
5. Sur dossier Famille BE avec `divorce_ddi_envisage=true`, F-FA-11 réapparaît.

## Plan de test
- IT migration : la 217 s'applique sans erreur sur PostgreSQL réel et sur H2 (profile `dev`).
- IT visibility : règle 217 produit la map attendue `alwaysOn` / `contextual` quand le dossier a / n'a pas le flag.
- Manuel staging post-merge : ouvrir un dossier Famille BE existant, vérifier que F-FA-11 disparaît du panel quand le dossier ne mentionne pas le DDI, réapparaît quand le flag est true.

## Tables / endpoints / composants impactés
- Table `decision_tool_visibility_rules` (DELETE 1 ALWAYS_ON + INSERT 1 CONTEXTUAL).
- Aucun changement code Java (les rules sont consommées par `DecisionToolVisibilityService` déjà existant).
- Aucun changement frontend (le `tool_id` est préservé, seul le layer change).

## Audit "Impact F-166 cross-C×D"
Toute modification de `decision_tool_visibility_rules` est analysée croisée Country × Domain (cf. garde-fou F-199 SF-199-02).

| Cellule C×D | Impact F-202 SF-202-02 | Justification |
|---|---|---|
| FR × Travail | ⚪ Non impacté | F-166 migration 199 livrée. |
| BE × Travail | ⚪ Non impacté | F-204 migration 215 livrée. |
| FR × Immigration | ⚪ Non impacté | F-201 migration 213 livrée. |
| BE × Immigration | ⚪ Non impacté | F-203 migration 214 livrée. |
| FR × Famille | ⚪ Non impacté | F-200 parallèle (migration 216 attendue). Notre migration 217 ne touche que `country='BELGIQUE'`, donc orthogonale. |
| **BE × Famille** | ✅ **Impacté (objet de cette SF)** | 1 outil ALWAYS_ON → CONTEXTUAL (`F-FA-11-desunion-irremediable-be` → trigger `divorce_ddi_envisage`). Effet panel F-IA-04 : sur dossier Famille BE sans flag, le panel par défaut affiche les outils transversaux (F-FA-05/06/07) + F-FA-23 ALWAYS_ON. F-FA-11 apparaît quand l'IA détecte une voie DDI. |

## Audit "exhaustivité droit national BE"
Le seul outil Famille BE existant aujourd'hui dans `decision_tool_visibility_rules` (avec `country='BELGIQUE'` et `legal_domain='DROIT_FAMILLE'`) est `F-FA-11-desunion-irremediable-be` (priority 73, ALWAYS_ON, livré par migration 131). Le flag jumeau `divorce_ddi_envisage` (livré SF-202-01) est aligné sur la base juridique CC art. 229 § 1 + § 3 et CJ art. 1255 § 1 + § 2 (Loi 27/04/2007 réformant le divorce).

Équivalent FR : F-FA-11 est BE-only (la France utilise `F-FA-08-divorce-alteration` / `F-FA-09-divorce-faute` / `F-FA-10-divorce-accepte` qui sont déjà CONTEXTUAL via le flag `regime_matrimonial` ou seedés FR-only). Pas d'asymétrie introduite : on ne fait que basculer un outil BE-only existant en CONTEXTUAL — aucune entrée FR créée.

Les 4 autres flags Famille BE livrés par SF-202-01 (`divorce_dc_envisage`, `cohabitation_legale_be_detectee`, `pacte_successoral_envisage`, `kafala_recueil_detecte`) ne déclenchent pas encore de bascule CONTEXTUAL faute d'outil seedé ; ils sont prêts pour les features futures F-211/F-217+ qui livreront les outils MANQUE (audit F-191 §3 et §4).

## Impact par domaine métier
- **Famille BE** : impacté (objet de cette SF).
- **Famille FR** : non impacté (la migration ne touche que `country='BELGIQUE'`).
- **Immigration FR/BE, Travail FR/BE** : non impacté (legal_domain différent).

## Hors périmètre
- Création des outils MANQUE Famille BE (cohabitation légale, pacte successoral, kafala, DDI 1an/faits, etc.) — F-211/F-217+ (audit F-191 §4 Top 10 manquants).
- Bascule CONTEXTUAL des outils transversaux F-FA-05/06/07 ou bipays F-FA-23 — décidée hors périmètre par audit F-191 §5.3 (priorité P3 acceptable ALWAYS_ON pour les transversaux).
- Implémentation des 5 flags IA (couverte par SF-202-01).
- Frontend Angular (les `tool_id` sont préservés, seul le layer change — `TOOL_REGISTRY` reste valide).
