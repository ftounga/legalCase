# SF-201-02 — Migration `decision_tool_visibility_rules` Immigration FR

## Objectif
Basculer 9 outils Immigration FR de `ALWAYS_ON` à `CONTEXTUAL` avec trigger sur les 9 flags livrés par SF-201-01. Réduction attendue : −71 % (14 → 4 cards par défaut sur dossier Immigration FR).

## Comportement nominal
- Migration Liquibase 213 : DELETE 9 entrées ALWAYS_ON, INSERT 9 entrées CONTEXTUAL avec `trigger_field = <flag>` et `trigger_value = 'true'`.
- 4 outils transversaux gardés ALWAYS_ON : F-IM-01 checklist pièces, F-IM-05 arbre titre, F-IM-06 générateur recours, F-IM-07 droit au travail.
- F-IM-17-regime-algerien : reste ALWAYS_ON faute de flag dédié (à traiter ultérieurement).

## Cas d'erreur
- Doublon UUID : namespace `f1a04001-0000-0000-0000-eeee20100XXX` distinct des autres (testé).
- Réversibilité : rollback restaure exactement l'état initial des 9 outils ALWAYS_ON.

## Critères d'acceptation
1. 9 outils Immigration FR basculent CONTEXTUAL via migration 213.
2. UUIDs `eeee20100091..201` sans collision.
3. `DecisionToolVisibilityIntegrityIT` reste vert.
4. Sur dossier Immigration FR sans aucun flag à `true`, le panel F-IA-04 affiche uniquement F-IM-01/05/06/07 + F-IM-17 (au lieu de 14 outils).
5. Sur dossier avec `aes_metiers_tension_eligible_detecte=true`, F-IM-09-aes-metiers-tension réapparaît.

## Plan de test
- IT migration : la 213 s'applique sans erreur sur PostgreSQL réel.
- IT visibility : règle 213 produit la map attendue alwaysOn / contextual.
- Smoke staging post-merge : ouvrir un dossier Immigration FR existant, vérifier que les outils contextuels apparaissent en cohérence avec les flags IA.

## Tables / endpoints / composants impactés
- Table `decision_tool_visibility_rules` (DELETE 9 + INSERT 9).
- Aucun changement code Java (les rules sont consommées par DecisionToolVisibilityService déjà existant).
- Aucun changement frontend (les tool_id sont préservés, seul le layer change).

## Audit "Impact F-166 cross-C×D"
- ✅ Immigration FR : impacté (objet de cette SF).
- ⚪ Immigration BE / Travail FR / Travail BE / Famille FR / Famille BE : non impactés (cf. SF-201-01).

## Audit "exhaustivité droit national FR"
Voir SF-201-01.

## Hors périmètre
- Implémentation des 9 flags IA (SF-201-01).
- Frontend (aucune modification — symétrique de F-166 SF-166-02).
- F-IM-17-regime-algerien : reste ALWAYS_ON.
