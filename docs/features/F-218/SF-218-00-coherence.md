# F-218 — Cadrage cohérence (étape 0)

**Date** : 2026-05-30
**Skill** : `ai-skills/feature-coherence-challenger.md`
**Source workflow** : audit `docs/features/F-191/audit-travail-fr-exhaustif.md` (2026-05-06) + pratique standard avocat travailliste FR.

## Verdict : **GO avec ajustements**

F-218 est fonctionnellement cohérente (aucun trou amont : tous les P1/P2 du Travail FR sont livrés). Mais son périmètre brut (~50 outils P3-P4) doit être **découpé en sous-features thématiques** et **priorisé** : développer en masse les outils que l'audit lui-même range « au signal terrain » / « niche » serait de la sur-production préventive (anti-gadget).

## Intention métier (1 phrase)

Compléter la couverture du droit du travail français avec les outils décisionnels de **spécificité nationale forte** (régimes catégoriels FR-only, IRP/négociation, procédure CPH avancée, temps/congés spécifiques, conformité employeur), une fois les ruptures et les calculs fréquents (P1/P2) déjà couverts.

## Workflow métier réel de l'avocat travailliste FR (source : audit F-191 + pratique standard)

1. Réception du dossier client (salarié ou employeur) + pièces (contrat, bulletins, courriers).
2. Qualification de la situation : rupture ? litige salarial ? IRP ? AT/MP ? régime catégoriel particulier (VRP, CESU, intermittent…) ?
3. Évaluation des délais de prescription / forclusion (P1 — déjà couvert F-DT-03).
4. Analyse de la situation principale (rupture, indemnités, nullité…) — **P1/P2 couverts** par F-DT-08→91 (F-206, F-212).
5. **Situations spécifiques FR** : forfait jours, transfert L.1224-1, CSE, modification du contrat, abandon de poste… — **P2 couverts** (F-212).
6. **Longue traîne** : régime catégoriel (VRP, journalistes, intermittents, CESU, apprentis), conformité employeur (DUER, RI, BDESE, NAO), procédure CPH avancée (appel, exécution/AGS, conciliation), congés spécifiques (parental, proche aidant, événements familiaux) — **= périmètre F-218 (P3-P4)**.
7. Production de la synthèse / fiche prud'homale (F-DT-04 ✅) / conclusions (F-243 ✅).
8. Suivi des échéances (F-69 ✅).

## Cartographie features actuelles ↔ workflow

| Étape métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1. Import dossier + pièces | F-43 / pipeline IA | ✅ Livrée |
| 3. Prescriptions | F-DT-03 | ✅ Livrée |
| 4. Rupture / indemnités / nullité (P1/P2) | F-DT-08→35, F-206, F-212 | ✅ Livrées |
| 5. Spécificités FR fréquentes (P2) | F-DT-36/39-44/46/48-50/56/59?/61/64/65/70-72/75/77/82/84/91 | ✅ Livrées (F-206+F-212) |
| 6. **Longue traîne P3-P4** | **F-218 (challengée)** | 🟡 Backlog |
| 7. Synthèse / fiche CPH / conclusions | F-DT-04, F-243 | ✅ Livrées |
| 8. Échéances | F-69 | ✅ Livrée |

## Position de la nouvelle feature

F-218 s'insère à l'**étape 6 (longue traîne)**, **après** que toutes les étapes amont (1-5) sont couvertes. C'est un backlog d'**enrichissement de couverture**, pas une brique de workflow critique.

## Challenge amont

> Chaque étape AVANT F-218 est-elle couverte ?

**OUI, intégralement.** Vérification factuelle sur `origin/master` (Explore 2026-05-30) :
- Les **2 P1** (F-DT-42 abandon poste, F-DT-75 CP arrêts maladie) → livrés par **F-206**.
- Les **10 du Top 10 audit** → tous livrés (F-206 + F-212).
- **F-212** (19 outils P2) Terminée.
→ **Aucun trou amont.** F-218 ne dépend d'aucune brique manquante.

## Challenge aval

> La sortie des outils F-218 est-elle exploitable ?

**OUI.** Chaque outil F-218 produit une analyse/un calcul qui alimente la **fiche prud'homale** (F-DT-04 ✅), les **conclusions** (F-243 ✅), le **dashboard** et les **échéances** (F-69 ✅). Pas de trou aval.

## STOPs / pré-requis à ajouter au backlog

**Aucun STOP.** Aucun pré-requis fonctionnel manquant.

## Ajustements requis (ce qui fait « GO **avec ajustements** »)

1. **Découper F-218 en 5 sous-features thématiques** (au lieu d'un bloc de ~50 outils) :
   - **F-218a — Procédure CPH avancée** : F-DT-86 (appel CPH, **P2**), F-DT-88 (exécution jugement / AGS, **P2**), F-DT-85, F-DT-87, F-DT-89, F-DT-90.
   - **F-218b — Régimes catégoriels FR-only** : F-DT-104 (VRP, **P2**), F-DT-108 (CESU/particulier employeur, **P2**), F-DT-103, F-DT-105, F-DT-106, F-DT-107, F-DT-109, F-DT-110, F-DT-37, F-DT-38, F-DT-45, F-DT-47.
   - **F-218c — IRP / négociation collective** : F-DT-59 (**P2** harcèlement procédure interne), F-DT-66, F-DT-67, F-DT-68, F-DT-69, F-DT-100, F-DT-102.
   - **F-218d — Temps de travail / congés spécifiques** : F-DT-51, F-DT-52, F-DT-53, F-DT-54, F-DT-55, F-DT-76, F-DT-78, F-DT-79, F-DT-80, F-DT-81, F-DT-83.
   - **F-218e — Sécu sociale / conformité employeur / divers** : F-DT-57, F-DT-58, F-DT-60, F-DT-62, F-DT-63, F-DT-73, F-DT-74, F-DT-92, F-DT-93, F-DT-94, F-DT-95, F-DT-96, F-DT-97, F-DT-98, F-DT-99, F-DT-101.

2. **Prioriser les ~5 P2 résiduels** (F-DT-59, 86, 88, 104, 108) — vraie valeur immédiate, à livrer en premier.

3. **Différer les P4 niche au signal terrain** (F-DT-58 tickets-resto, F-DT-95 retraite progressive…) plutôt que les développer préventivement.

## Invariants anti-gadget pour la mini-spec

- **Un outil = une situation juridique distincte** (règle CLAUDE.md). Ne pas créer d'outil qui doublonne un existant (ex. F-DT-54/55 prime ancienneté/13e mois ≈ paramétrage de F-DT-07/F-DT-20 → à fusionner, pas à créer).
- **Layer CONTEXTUAL par défaut** + flag IA bridé (pattern F-166), jamais ALWAYS_ON sans justification (anti-régression bug E-37 panel de cards blanches).
- **Pré-remplissage IA obligatoire** de tout champ saisissable (F-246) + alerte divergence F-IA-03.
- **Instrumentation jurisprudence F-JU-03** + test `DecisionToolVisibilityIntegrityIT` vert (F-164).
- Chaque outil doit alimenter la **fiche prud'homale / conclusions** (sortie exploitable, pas un cul-de-sac).

## Décision finale

**GO avec ajustements** : F-218 procède, **découpée en F-218a→e**, en commençant par les **P2 résiduels**. Les P4 niche restent au backlog jusqu'à signal terrain. Périmètre exact de cette session à valider avec le PO avant l'étape 0bis / mini-specs.
