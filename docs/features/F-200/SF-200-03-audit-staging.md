# SF-200-03 — Audit visuel staging Famille FR (post-merge)

## Objectif
Vérifier en staging que la bascule ALWAYS_ON → CONTEXTUAL Famille FR (SF-200-01 + SF-200-02) produit l'effet attendu : panel F-IA-04 réduit à ~3 cards par défaut, +N cards quand l'IA détecte un flag.

## Comportement nominal
- Sur un dossier Famille FR vide (sans pièces ou avec pièces ne déclenchant aucun flag) : `F-FA-12-mesures-provisoires` + `F-FA-19-autorite-parentale` + tronc commun mixte (F-FA-05 + F-FA-06 + F-FA-07 si CM détecté via type_procedure_detectee).
- Sur un dossier Famille FR avec pièces évoquant divorce faute, succession, PMA : les 3 outils correspondants (F-FA-09 + F-FA-24-* + F-FA-27-pma-gpa) apparaissent.

## Périmètre staging
- Hors scope code (cette SF est une checklist d'observation).
- Smoke test E2E manuel après déploiement staging.
- Si une régression visible : ouvrir une SF correctrice (ex. flag IA non émis correctement).

## Critères d'acceptation
1. Panel F-IA-04 sur dossier Famille FR vide : 2-3 cards visibles par défaut (au lieu de ~33 avant).
2. Au moins 3 dossiers Famille FR existants en staging vérifiés.
3. Aucune erreur frontend / 404 / outil silencieusement masqué.

## Hors périmètre
- Famille BE (F-202).
- Outils manquants Top 10 (audit-famille-fr-exhaustif.md D.2).
