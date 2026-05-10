# SF-204-02 — Migration `decision_tool_visibility_rules` Travail BE

## Objectif
Basculer 5 outils Travail BE de `ALWAYS_ON` à `CONTEXTUAL` avec trigger sur les 5 flags livrés par SF-204-01.

## Critères d'acceptation
1. Migration Liquibase 215 : DELETE 5 entrées ALWAYS_ON BE (F-DT-11/12/15/19/27), INSERT 5 entrées CONTEXTUAL.
2. UUIDs `eeee20400111..271` sans collision.
3. ALWAYS_ON Travail BE = 7 outils essentiels gardés (F-DT-05 préavis, F-DT-06 requête, F-DT-07 ancienneté/CP, F-DT-08 validity, F-DT-09 comparateur, F-DT-28 avantages CCT, F-DT-29 crédit-temps).
4. Garde-fou IT vert.
5. Les entrées country=FRANCE des mêmes tool_id (F-DT-11/12/15/19) sont **non touchées** — restent dans leur état défini par F-166/F-205.

## Tables / endpoints / composants impactés
- Table `decision_tool_visibility_rules` (DELETE 5 + INSERT 5).
- Aucun code Java/frontend modifié.

## Audit "Impact F-166 cross-C×D"
- ⚪ Travail FR : F-166 déjà livré, non impacté ici.
- ✅ Travail BE : objet de cette SF.
- ⚪ Immigration / Famille : non impactés.

## Audit "exhaustivité droit national BE"
Voir SF-204-01.

## Hors périmètre
- Implémentation flags (SF-204-01).
- Outils Travail BE MANQUE : F-213 P2.
- Entrées country=FRANCE pour F-DT-11/12/15/19/27 : non touchées (F-166/F-205).
