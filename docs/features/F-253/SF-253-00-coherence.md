# F-253 — Cadrage cohérence (étape 0)

## Verdict : GO

## Intention métier (1 phrase)

Donner un consommateur explicite, hors écran synthèse, au statut `À_CREUSER` des risques de F-195 pour qu'un risque détecté par l'IA et non encore arbitré par l'avocat reste visible dans son champ d'attention (dashboard, cards outils, export PDF) — sans casser l'invariant F-176 strict (PUT statut reste un acte pur).

## Workflow métier réel de l'utilisateur cible (avocat)

**Source** : pratique avocat sourcée — signaux Renversez (démo 13/05/2026, 1ʳᵉ utilisation prod réelle 19/05/2026) + Mengue (démo 11/05/2026) + pattern reconnu lors de la livraison F-195 (Terminée 2026-05-06).

1. Avocat reçoit le dossier client (factuel + premières pièces).
2. Importe les pièces dans LegalCase (F-43).
3. Lance l'analyse IA standard (F-13/F-14, asynchrone — F-185 streaming).
4. Consulte la synthèse : faits, **risques juridiques**, points juridiques, pièces manquantes, pistes stratégiques, timeline.
5. **Curate les risques** détectés par l'IA — décide, par risque, si VALIDÉ (à approfondir) / ÉCARTÉ (non pertinent + raison) / À_CREUSER (par défaut tant que pas arbitré).
6. Lance la **synthèse enrichie** (matérialisation F-195 — recompute `score_risque_avocat`, propagation aux outils).
7. Bascule sur l'**espace décisionnel** (F-244) pour consulter les outils calculator/analyzer/generator pertinents (indemnités, barème, scoring, etc.).
8. Au besoin, retourne sur la synthèse pour vérifier / ajuster un risque.
9. Exporte le PDF récapitulatif (relecture client / argumentation conclusion).
10. Génère les conclusions (F-243 si disponibles dans le domaine).
11. **État terminal** : conclusions remises au client / déposées au tribunal — le dossier sort du flux actif.

## Cartographie features actuelles ↔ workflow

| Étape workflow métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1. Réception dossier | (hors app) | — |
| 2. Import pièces | F-43 Import dossier | ✅ Livrée |
| 3. Analyse IA standard | F-13/F-14 + F-185 streaming | ✅ Livrée |
| 4. Consultation synthèse | F-94 Synthèse + F-162 refonte écran | ✅ Livrée |
| 5. **Curation risques** | F-195 SF-195-01/02/03 | ✅ Livrée 2026-05-06 |
| 6. Synthèse enrichie + matérialisation | F-IA-04 visibility + F-195 hooks | ✅ Livrée |
| 7. Espace décisionnel | F-244 architecture info + outils F-DT-*/F-IM-*/F-FA-* | ✅ Livrée |
| 8. Retour synthèse | F-244 SF-244-02/04 reconnexion bidirectionnelle | ✅ Livrée |
| 9. Export PDF | F-PDF-* / PdfExportService | ✅ Livré |
| 10. Génération conclusions | F-243 | 🟡 Partiellement livrée |
| 11. État terminal | (hors app) | — |

## Position de la nouvelle feature

F-253 s'insère à l'étape 5 (curation risques) **en aval** et aux étapes 2/7/9 **comme rappel visuel** :
- Tile dashboard d'accueil du dossier → rappel à l'étape 2 et 7 (l'avocat voit le dossier sous l'angle « il y a N risques que je n'ai pas encore tranchés »).
- Pill sur cards outils → rappel à l'étape 7 (sur chaque outil potentiellement concerné par un risque à creuser).
- Section PDF → consolidation à l'étape 9 (la liste des risques à creuser apparaît dans le PDF avant les VALIDÉ).

## Challenge amont

**Question : chaque étape AVANT F-253 est-elle couverte ?**

- F-43 Import : ✅ livrée
- F-13/F-14 Analyse IA + risques détectés : ✅ livrée
- F-195 Risques markables + statut `À_CREUSER` par défaut : ✅ livrée 2026-05-06
- `RisqueToolMatcher` (mapping risque → outil) : ✅ livré dans F-195 SF-195-01
- `RisqueAlignmentService.getForLatestAnalysis()` : ✅ livré dans F-195 SF-195-01
- `case_analyses.risques_alignment_json` (déjà retourne `aCreuser` via `collectForEnrichment`) : ✅ livré dans F-195 SF-195-01

**Aucun trou amont**. La brique « identification des risques » et la brique « curation par l'avocat » existent toutes les deux.

## Challenge aval

**Question : la sortie de F-253 est-elle exploitable par les étapes AVAL du workflow ?**

- Étape 6 (synthèse enrichie) : la tile et la pill ne modifient pas la matérialisation, c'est de la lecture pure. Pas de side-effect sur le recompute `score_risque_avocat` (déjà géré par F-195 — VALIDÉ/ÉCARTÉ uniquement).
- Étape 7 (espace décisionnel) : la pill complète le badge VALIDÉ/ÉCARTÉ existant. Cohabite — pas de remplacement.
- Étape 9 (export PDF) : la section « Risques à creuser » s'insère avant « Risques retenus par votre avocat » (ordre : à creuser → validés → écartés — décroissance d'incertitude).

**Aucun trou aval**.

## STOPs / pré-requis à ajouter au backlog

**Aucun**. Tous les pré-requis sont livrés. F-253 est démarrable immédiatement.

## Invariants anti-gadget pour la mini-spec

1. **Invariant F-176 strict préservé** : PUT `/risques/status` reste un acte pur (pas de side-effect F-253). Tous les effets F-253 sont en lecture, sur la base de l'état déjà persisté par F-195.
2. **Tile masquée si compteur = 0** : pas de pollution dashboard quand l'avocat a tout tranché (anti-bruit). Le placement de la tile suit le même pattern que `F-195-risques-summary` (apparait si pertinente).
3. **Pas de double comptage** : la tile F-253 compte UNIQUEMENT `À_CREUSER`, la tile `F-195-risques-summary` compte VALIDÉ/ÉCARTÉ. Pas de risque mentionné dans les deux tiles.
4. **Pill non critique** : palette gris navy `#1A3A5C` ≤ 60 % opacité (DESIGN_SYSTEM.md). Le rouge `#C0392B` reste réservé à `validated_critical` (F-195). Le À_CREUSER est une indécision, pas une alerte.
5. **Aucun side-effect métier** : pas de notification toaster, pas d'email, pas de délai auto type F-194 `case_deadlines` (un risque à creuser n'a pas de logique de « relance dans X jours »).
6. **Pas de migration** : F-253 réutilise strictement la table `risque_status` et la colonne `case_analyses.risques_alignment_json` de F-195. Si une migration est tentée, REFUS — signe que le scope dérive.
7. **Transversalité stricte** : aucune adaptation pays/domaine. Le `RisqueToolMatcher` existant gère déjà Travail FR/BE, Immigration FR/BE, Famille FR/BE, Pénal, Successions BE. Si on ajoute un mapping pays-spécifique → REFUS, ça relève de F-195.

## Décision finale

**GO** — feature démarrable immédiatement, séquence cycle obligatoire complète à partir de l'étape 0 bis.
