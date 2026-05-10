# SF-203-02 — Migration `decision_tool_visibility_rules` Immigration BE

## Objectif
Basculer 5 outils Immigration BE de `ALWAYS_ON` à `CONTEXTUAL` avec trigger sur les 5 flags livrés par SF-203-01. Réduction attendue : −56 % (9 → 4 cards par défaut sur dossier Immigration BE).

## Critères d'acceptation
1. Migration Liquibase 214 : DELETE 5 entrées ALWAYS_ON BE (F-IM-14 9bis/9ter/40bis/40ter, F-IM-08 annexe13), INSERT 5 entrées CONTEXTUAL.
2. UUIDs `eeee20300141..144 + 085` sans collision.
3. ALWAYS_ON Immigration BE = 4 outils transversaux gardés (F-IM-01/05/06/07).
4. Garde-fou IT vert.

## Tables / endpoints / composants impactés
- Table `decision_tool_visibility_rules` (DELETE 5 + INSERT 5).
- Aucun code Java/frontend modifié.

## Audit "Impact F-166 cross-C×D"
- ⚪ Immigration FR : F-201 parallèle.
- ✅ Immigration BE : objet de cette SF.
- ⚪ Autres : non impactés.

## Audit "exhaustivité droit national BE"
Voir SF-203-01.

## Hors périmètre
- Implémentation flags (SF-203-01).
- Outils Immigration BE MANQUE : F-209+.
