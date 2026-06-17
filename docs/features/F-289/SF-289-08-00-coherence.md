# SF-289-08-00 — Cadrage cohérence : indicateur « dossier à ré-synthétiser »

> Étape 0 (skill `feature-coherence-challenger`). Feature : sur la **Vue d'ensemble** (F-289), signaler que le dossier a évolué **depuis la dernière synthèse enrichie** — étendu aux **réponses IA** et aux **nouveaux documents** — pour inciter l'avocat à relancer une synthèse.

## Workflow métier réel de l'avocat
1. L'avocat ouvre le dossier, dépose des pièces, lance l'analyse → **synthèse** (faits, pièces manquantes, risques, questions IA).
2. Il **cure** le dossier : répond aux questions IA, marque des pièces obtenues/non applicables, retient des pistes, ajoute des pièces reçues.
3. Ces décisions ne sont **matérialisées dans la synthèse** (et les outils) qu'au **prochain run de synthèse enrichie** (cohérence F-176/F-194 stricte : le PUT est un acte pur, sans recompute).
4. **Problème** : rien ne signale à l'avocat que sa synthèse affichée est **périmée** par rapport à ce qu'il vient de saisir. Il peut générer des conclusions sur une base obsolète, ou ne pas comprendre pourquoi ses réponses n'apparaissent pas.

## Cartographie des features existantes sur ce workflow
- **F-289 Vue d'ensemble** : expose déjà `pilotage.analysisStale` + un item d'attention `ANALYSE_OBSOLETE`, mais **uniquement** piloté par les **pièces en attente d'analyse** (`pendingPiecesCount` via `PiecesWaveService`). → Couvre les nouveaux documents **non encore analysés**, mais **pas** les réponses IA ni les pièces curées depuis la dernière synthèse.
- **F-194 / F-176 / F-192 / F-193 / F-195** : statuts curés injectés dans le prompt enrichi via `collectForEnrichment` — donc un changement de statut **rend la synthèse périmée**, sans aucun signal.
- **Questions IA** : répondre inline (SF-289-07) ne déclenche aucun signal de péremption.

## Challenge cohérence
- **Amont** (pré-requis existent ?) : ✅ La notion de « synthèse enrichie » et son déclenchement (`/re-analyze`) existent. Le mécanisme `analysisStale` + `ANALYSE_OBSOLETE` existe déjà → on **étend** un signal, on n'en invente pas un.
- **Aval** (sortie exploitable ?) : ✅ L'item d'attention `ANALYSE_OBSOLETE` porte déjà l'action `RELAUNCH_ANALYSIS` (route `/re-analyze`). L'avocat a déjà le bouton pour agir → le signal est directement actionnable.
- **Risque gadget** : faible — le besoin est réel (constaté au test : « montrer qu'on n'est plus à jour, qu'il faut une synthèse enrichie, pareil pour les réponses IA »).

## Verdict : **GO avec ajustements**
Ajustements imposés à la mini-spec :
1. **Réutiliser** le signal existant (`analysisStale` + item `ANALYSE_OBSOLETE`), ne pas créer un 2ᵉ mécanisme concurrent.
2. **Lecture seule, fail-open** strict (F-289 est un agrégateur sans écriture / sans LLM) — aucune table, idéalement aucune migration.
3. Le signal doit **expliquer la raison** (réponses IA / nouveaux documents / pièces en attente) sans noyer l'avocat — rester dans le bloc « attention » + bandeau existants.
4. Ne **rien changer** au déclenchement réel de la synthèse (`/re-analyze`) ni à la matérialisation (invariant : 1 outil = 1 situation, aucune altération d'outil).

## Invariants anti-gadget
- Le signal est **dérivé** (comparaison de timestamps existants), jamais une nouvelle source de vérité.
- Pas de notification push / email — un indicateur **in situ** sur la Vue d'ensemble.
- Si une source de timestamp est indisponible → **fail-open** (pas de faux « périmé » bloquant ; au pire on n'affiche pas le signal).
