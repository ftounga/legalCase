# SF-200-02 — Migration `decision_tool_visibility_rules` Famille FR

## Objectif
Basculer 30 outils Famille FR de `ALWAYS_ON` à `CONTEXTUAL` avec trigger sur les 30 flags livrés par SF-200-01. Réduction attendue : −91 % (~33 → ~3 cards par défaut sur dossier Famille FR vide).

## Comportement nominal
- Migration Liquibase 216 : DELETE 30 entrées ALWAYS_ON Famille FR (+ ALWAYS_ON F-FA-07 pays NULL transversal), INSERT 30 entrées CONTEXTUAL avec `trigger_field = <flag>` et `trigger_value = 'true'`.
- 3 outils gardés ALWAYS_ON (cliniquement justifiés) :
  - `F-FA-12-mesures-provisoires` (tronc commun divorce judiciaire).
  - `F-FA-19-autorite-parentale` (tronc commun mineurs).
  - `F-FA-05-partage-immobilier`, `F-FA-06-calendrier-garde` (déjà mixed ALWAYS_ON + CONTEXTUAL — préservés).

Note : `F-FA-07-checklist-divorce` perd son ALWAYS_ON (basculer CONTEXTUAL pur per Tableau C C3) ; sa CONTEXTUAL existante (`type_procedure_detectee=DIVORCE_CONSENTEMENT_MUTUEL`) est conservée et complétée par une seconde CONTEXTUAL sur `divorce_consentement_mutuel_envisage=true`.

## Cas d'erreur
- Doublon UUID : namespace `f1a04001-0000-0000-0000-eeee20000XXX` distinct des autres (testé).
- Réversibilité : rollback restaure exactement l'état initial des 30 outils ALWAYS_ON (avec UUID d'origine pour permettre une nouvelle suppression cohérente côté code).

## Critères d'acceptation
1. 30 outils Famille FR basculent CONTEXTUAL via migration 216.
2. UUIDs `eeee20000fa01..fa30` sans collision.
3. `DecisionToolVisibilityIntegrityIT` reste vert.
4. Sur dossier Famille FR sans aucun flag à `true`, le panel F-IA-04 affiche uniquement F-FA-12 + F-FA-19-autorite-parentale + F-FA-05 + F-FA-06 (+ outils transversaux non Famille).
5. Sur dossier avec `divorce_faute_envisage=true`, F-FA-09-divorce-faute réapparaît.

## Plan de test
- IT migration : la 216 s'applique sans erreur sur PostgreSQL réel.
- IT visibility : règle 216 produit la map attendue alwaysOn / contextual.
- Smoke staging post-merge : ouvrir un dossier Famille FR existant, vérifier que les outils contextuels apparaissent en cohérence avec les flags IA.

## Tables / endpoints / composants impactés
- Table `decision_tool_visibility_rules` (DELETE 30 + INSERT 30).
- Aucun changement code Java (les rules sont consommées par DecisionToolVisibilityService déjà existant).
- Aucun changement frontend (les tool_id sont préservés, seul le layer change).

## Audit "Impact F-166 cross-C×D"
- ✅ Famille FR : impacté (objet de cette SF).
- ⚪ Famille BE / Immigration FR / Immigration BE / Travail FR / Travail BE : non impactés (cf. SF-200-01).

## Audit "exhaustivité droit national FR"
Voir SF-200-01.

## Hors périmètre
- Implémentation des 30 flags IA (SF-200-01).
- Frontend (aucune modification — symétrique de F-166 SF-166-02 et F-201 SF-201-02).
- Famille BE : F-202.
