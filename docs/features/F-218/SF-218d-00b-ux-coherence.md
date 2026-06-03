# F-218d — Cadrage cohérence écran (étape 0bis) — extension Temps de travail / congés

**Date** : 2026-06-03
**Skill** : `ai-skills/screen-coherence-challenger.md`
**Étend** : `SF-218-00b-ux-coherence.md` (verdict GO, pattern panel décisionnel F-IA-04) au lot **F-218d (9 outils Temps de travail / congés spécifiques FR)**.
**Contexte** : F-218d était différé « au signal terrain » par décision PO du 2026-05-30 ; **gel levé par le PO le 2026-06-03** (choix d'avancer sur la couverture). Périmètre net = 9 outils (F-DT-51, 52, 53, 76, 78, 79, 80, 81, 83) ; doublons F-DT-54/55 maintenus exclus.

## Verdict : **GO** (pattern écran déjà cadré et stable, identique à F-218a/b/c)

Les 9 outils F-218d sont des **outils décisionnels CONTEXTUAL** qui s'ajoutent au **panel décisionnel F-IA-04 existant** (`<app-decisional-tools-panel>` dans l'écran dossier). Aucun nouvel écran, aucune nouvelle route, aucun déplacement d'élément existant. Le pattern d'insertion est **identique** à F-212/F-218a/b/c (déjà livrés sans incident écran).

## Parcours écran (rappel)

1. Avocat ouvre le dossier → onglet « Analyse ».
2. Pipeline IA détecte les situations → alimente les flags `Sf218dDetail` (sous-record `TravailExtractedData`).
3. Le panel décisionnel affiche **uniquement** les outils dont le trigger CONTEXTUAL est satisfait (panel au repos si rien détecté — invariant F-166).
4. Card outil → formulaire pré-rempli IA → verdict/calcul.
5. Synthèse → fiche prud'homale (F-DT-04) / conclusions (F-243).

## Challenge écran

| Critère | Évaluation |
|---|---|
| **Placement** | Cards ajoutées au panel décisionnel existant, groupées par thème (`THEME_BY_TOOL_ID`). Aucun nouvel emplacement. ✅ |
| **Lisibilité de la séquence** | Les 9 outils sont **CONTEXTUAL** : invisibles tant que l'IA n'a pas détecté la situation (RTT, PPV, congé parental…). Pas de pollution du panel. ✅ |
| **Charge de l'écran cible** | Risque maîtrisé : un dossier réel ne déclenche que les outils pertinents. Les situations « rémunération » (RTT/PPV/intéressement) et « congés » (évts familiaux/parental/proche aidant) sont rarement toutes présentes simultanément. ✅ |
| **État final / continuité** | Sortie alimente fiche prud'homale + conclusions + échéances (F-69). ✅ |

## Invariants anti-surcharge pour la mini-spec

- **CONTEXTUAL strict** : aucun des 9 outils n'est ALWAYS_ON (anti-régression bug E-37 « panel de cards blanches »).
- **Trigger IA bridé** (niveau 2 sur mention explicite). Pas de faux positifs qui rempliraient le panel.
- **Thème cohérent** (`THEME_BY_TOOL_ID`) : rémunération/indemnités → `INDEMNITES` (F-DT-51/52/76/79/80/81) ; conformité/diagnostic → `DIAGNOSTIC` (F-DT-53/78/83).
- **Pas de doublon de card** : F-DT-80 (jours RTT acquis selon accord) est distinct de F-DT-19 (heures supplémentaires) ; triggers disjoints. F-DT-54/55 restent exclus (paramétrage de F-DT-07/F-DT-20).

## Décision finale

**GO** — impact écran nul au-delà du pattern panel décisionnel déjà éprouvé sur 18 outils F-218a/b/c. La mini-spec applique les invariants CONTEXTUAL + trigger bridé + sous-record IA consolidé `Sf218dDetail`.
