# SF-IM-05-04 — Pré-remplissage F-IM-05 depuis les événements déclencheurs

## Objectif
Éviter que l'avocat saisisse manuellement le motif et la situation familiale
dans F-IM-05 (titres recommandés) quand l'IA a déjà détecté un événement
déclencheur (F-150) qui détermine la **voie juridique cible**. Le cas
paradigmatique : Chen Wei (pluriannuelle Étudiant-Recherche + mariage avec
Française) doit ouvrir F-IM-05 avec motif **FAMILLE** + situation **MARIÉ**
pour qu'il recommande `CST_VPF_CONJOINT_FR`, pas avec motif TRAVAIL (choix
manuel que l'avocat avait dû faire à tort).

## Comportement nominal
**Au mount / ngOnChanges** — `prefillFromAi()` avec priorités :

1. **Priorité 1 — trigger_events** (décrit la voie cible) :
   - `MARIAGE_RESSORTISSANT_FR` → motif FAMILLE + situation MARIÉ
   - `PACS_RESSORTISSANT_FR` → FAMILLE + PACS_COHABITATION
   - `NAISSANCE_ENFANT_FR`, `REGROUPEMENT_FAMILIAL_AUTORISE`,
     `VIOLENCES_CONJUGALES_CONSTATEES`, `ENFANT_NE_FR_13ANS_PRESENCE` → FAMILLE
   - `CDI_OBTENU_SALARIE`, `DOCTORAT_OBTENU` → TRAVAIL
   - `DEMANDE_ASILE_ACCORDEE_OFPRA` → ASILE
   - `ENTREE_LEGALE_10ANS` : non mappé (ambigu motif)

2. **Priorité 2 — typeTitreSejourCode** (titre actuel) :
   - Nouveaux codes SF-IM-07-04 mappés :
     - `CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE` → ETUDES
     - `CARTE_PLURIANNUELLE_SALARIE`, `_PASSEPORT_TALENT` → TRAVAIL
     - `CARTE_PLURIANNUELLE_VPF`, `CST_VPF_CONJOINT_FR` → FAMILLE
   - Anciens codes inchangés

3. **Priorité 3 — heuristique texte libre** (legacy, fallback)

**Badges "Pré-rempli depuis l'analyse"** (icône `auto_awesome`) affichés
à côté de chaque champ rempli par l'IA : motif, situation familiale,
nationalité UE. Effacés dès que l'avocat modifie le champ.

## Pourquoi
- F-IM-07 avait déjà le pattern (SF-IM-07-05), F-IM-05 restait manuel
  → incohérence de parcours. L'avocat devait deviner que pour Chen c'est
  motif FAMILLE (non évident vu qu'il est étudiant).
- Les trigger_events **pointent déjà vers la voie cible**, pas le titre
  courant — c'est le signal le plus pertinent pour ce simulateur.
- Les scenarii stratégiques F-151 affichent la voie recommandée dans la
  synthèse, mais l'outil décisionnel F-IM-05 ne l'exploitait pas.

## Cas d'erreur
- **Aucun trigger_event** : fallback sur le code titre actuel (comportement
  legacy préservé, test U-06 le garantit).
- **trigger_event inconnu de la map** (ex: `ENTREE_LEGALE_10ANS`) :
  ignoré silencieusement, fallback sur code titre.
- **Décision F-IM-05 déjà persistée** : `loadExisting()` repose la
  décision, le prefill ne s'applique pas (condition `!this.decision()`).

## Critères d'acceptation
- [x] Nouveau @Input `triggerEvents: ImmigrationTriggerEvent[] | null`
- [x] Parent `case-file-detail.component.html` passe `synthesis()?.immigrationTriggerEvents`
- [x] Map `TRIGGER_TO_CRITERIA` des 9 events mappables
- [x] Map `CODE_TO_MOTIF` étendue aux 5 nouveaux sous-types SF-IM-07-04
- [x] `prefillFromAi()` priorise triggers sur code titre
- [x] 3 nouveaux signals `provenanceMotif/SituationFamiliale/NationaliteUe`
- [x] 3 badges `auto_awesome` rendus conditionnellement dans le HTML
- [x] Handlers `onMotifChange`, `onSituationFamilialeChange`,
  `onNationaliteUeChange` effacent les badges
- [x] `ngOnInit` appelle aussi `prefillFromAi()` (garde-fou au mount)
- [x] 36 tests verts (27 existants + 9 nouveaux)

## Plan de test
- **Unit frontend** (9 nouveaux, suite `SF-IM-05-04`) :
  - MARIAGE → FAMILLE + MARIÉ (cas Chen)
  - PACS → FAMILLE + PACS_COHABITATION
  - NAISSANCE_ENFANT_FR → FAMILLE (sans situation)
  - CDI → TRAVAIL
  - trigger prioritaire sur code titre (Chen)
  - pas de trigger → fallback code titre
  - onMotifChange clears badge + reset situation
  - Mappings sous-types nouveaux
- **Intégration (staging — Chen Wei)** :
  1. Ouvrir F-IM-05 "Titres de séjour recommandés"
  2. Champs pré-remplis : motif **FAMILLE**, situation **MARIÉ**,
     nationalité UE **non** — avec badges "Pré-rempli depuis l'analyse"
  3. Cliquer Analyser → recommandation `CST_VPF_CONJOINT_FR` (L.423-1)
  4. Modifier motif → TRAVAIL → badge disparaît, situation familiale
     réinitialisée → Analyser → recommandations TRAVAIL (pour simulation)

## Hors périmètre
- **Changement d'algorithme** de recommandation : on n'a que pré-rempli
  les critères d'entrée, l'arbre décisionnel `ImmigrationTitleDecisionEngine`
  est inchangé (pas besoin).
- **Multiple triggers** : le code ne regarde que le 1er trigger. Si
  plusieurs événements sont détectés (rare), le 1er prime. Amélioration
  future possible si besoin.
- **F-151 strategy_scenarios** : pas consommés directement — F-151 offre
  déjà une vue stratégique dans la synthèse, F-IM-05 reste un simulateur
  générique alimenté par les trigger_events bruts.

## Composants impactés
- `immigration-title-decision-section.component.ts` : +Input, +map,
  +provenance signals, +handlers, prefillFromAi modifié
- `immigration-title-decision-section.component.html` : +3 badges +
  handlers (click/change) sur les inputs
- `immigration-title-decision-section.component.scss` : +`.provenance-note`
- `case-file-detail.component.html` : +passage `triggerEvents`
- Tests : +9 tests SF-IM-05-04

## Impact par domaine métier
**DROIT_IMMIGRATION** uniquement (les trigger_events n'existent que pour
ce domaine). France et Belgique également concernées — la map
`TRIGGER_TO_CRITERIA` est volontairement pays-agnostique car les
motifs (FAMILLE / TRAVAIL / ETUDES / ASILE) sont communs.

## Analyse de cohérence transversale
- **Pattern identique à SF-IM-07-05** (pré-remplissage + badge IA +
  reset manuel). Les 2 outils consomment maintenant l'IA de façon
  cohérente.
- **`coherence-alert` existant** : préservé — si l'avocat modifie le
  motif, l'alerte de divergence avec les signaux IA se déclenche comme
  avant.
- **Pas d'impact** sur F-IM-01 (checklist), F-IM-06 (recours), F-IM-07
  (droit au travail). Ils lisent les mêmes signaux IA mais via d'autres
  champs.

## Nouveau pattern UI ou service partagé
Le pattern "pré-remplissage depuis trigger_events avec badge IA + reset
manuel" est désormais utilisé par **2 outils** (F-IM-05, F-IM-07). Si
un 3e outil décisionnel immigration le nécessite (F-IM-06 recours ?),
on extraira `prefillFromAi` + `TRIGGER_TO_CRITERIA` dans un service
partagé. Pour l'instant, duplication acceptable (règle 3-same-lines).
