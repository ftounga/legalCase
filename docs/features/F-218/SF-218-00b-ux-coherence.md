# F-218 — Cadrage cohérence écran (étape 0bis)

**Date** : 2026-05-30
**Skill** : `ai-skills/screen-coherence-challenger.md`
**Référentiel** : `docs/business/parcours-ecran-*.md` + pattern panel décisionnel F-IA-04 (établi F-164/F-166, répliqué F-212/F-214/F-215).

## Verdict : **GO** (pattern écran déjà cadré et stable)

Les 18 outils de F-218a/b/c sont des **outils décisionnels** qui s'ajoutent au **panel décisionnel F-IA-04 existant** (`<app-decisional-tools-panel>` dans l'écran dossier). Aucun nouvel écran, aucune nouvelle route, aucun déplacement d'élément existant. Le pattern d'insertion écran est **identique** à F-212/F-214/F-215 (déjà cadrés et livrés sans incident écran).

## Parcours écran réel de l'avocat (rappel)

1. Ouvre le dossier → onglet « Analyse ».
2. Le pipeline IA détecte les situations → alimente les flags `TravailExtractedData`.
3. Le **panel décisionnel** affiche **uniquement** les outils dont le trigger CONTEXTUAL est satisfait (panel au repos si rien détecté — invariant F-166).
4. L'avocat ouvre une card outil → formulaire pré-rempli IA → verdict/calcul.
5. Synthèse → fiche prud'homale (F-DT-04) / conclusions (F-243).

## Challenge écran

| Critère | Évaluation |
|---|---|
| **Placement** | Cards ajoutées au panel décisionnel existant, groupées par thème (`THEME_BY_TOOL_ID`). Aucun nouvel emplacement. ✅ |
| **Lisibilité de la séquence** | Les 18 outils sont **CONTEXTUAL** : invisibles tant que l'IA n'a pas détecté la situation (VRP, CESU, appel CPH…). Pas de pollution du panel. ✅ |
| **Charge de l'écran cible** | Risque maîtrisé : un dossier réel ne déclenche que les outils pertinents (rarement > 3-4 simultanés). Les régimes catégoriels (VRP/CESU/journalistes/intermittents) sont mutuellement exclusifs en pratique. ✅ |
| **État final / continuité** | Sortie alimente fiche prud'homale + conclusions + échéances (F-69). ✅ |

## Invariants anti-surcharge pour la mini-spec

- **CONTEXTUAL strict** : aucun des 18 outils n'est ALWAYS_ON (anti-régression bug E-37 « panel de cards blanches »).
- **Trigger IA bridé** (niveau 2 sur mention explicite, niveau 3 LLM sur contexte) — pas de faux positifs qui rempliraient le panel.
- **Thème cohérent** (`THEME_BY_TOOL_ID`) : a → `CONTENTIEUX`/`DELAIS`, b → `VALIDITE`, c → `DIAGNOSTIC`.
- **Régimes catégoriels mutuellement exclusifs** : un même dossier ne doit pas afficher VRP + CESU + intermittent simultanément (triggers disjoints).

## Décision finale

**GO** — impact écran nul au-delà du pattern panel décisionnel déjà éprouvé. La mini-spec applique les invariants CONTEXTUAL + trigger bridé.
