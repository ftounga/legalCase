# F-206 — Cadrage cohérence écran (étape 0 bis)

> Produit par la skill `ai-skills/screen-coherence-challenger.md`. Étape 0 bis du cycle de gouvernance, entre l'étape 0 (cohérence fonctionnelle) et l'étape 1 (mini-spec).
> Feature : **F-206 — P1 Travail FR — 4 outils d'urgences procédurales**.
> Date : 2026-05-19.

## Verdict : 🟢 GO avec ajustements

Les 4 outils s'insèrent dans une zone déjà prévue pour les accueillir — le panneau d'outils décisionnels (`app-decisional-tools-panel`) de l'onglet **Décision** du détail du dossier. Ils ne créent **aucun bloc primaire nouveau** : ce sont des sous-sections du panneau, affichées conditionnellement. Ajustements à intégrer à la mini-spec : affichage `CONTEXTUAL` réel, groupement thématique (F-169), matérialisation non orpheline des échéances dans l'onglet Suivi.

## Intention métier + comportement visible attendu

L'avocat, à l'étape d'évaluation décisionnelle d'un dossier de rupture/contentieux du contrat de travail, voit apparaître dans le panneau d'outils décisionnels — **uniquement quand l'IA a détecté la situation** — la (les) section(s) correspondant à : abandon de poste / présomption de démission, congés payés acquis pendant arrêt maladie, prise d'acte de la rupture, résiliation judiciaire. Chaque section affiche des champs pré-remplis par l'IA, un verdict tranché et alimente le tableau de bord décisionnel.

## Rappel verdict feature-coherence-challenger (étape 0)

🟢 **GO avec ajustements** (`SF-206-00-coherence.md`, 2026-05-19). Toutes les briques amont/aval sont livrées ; ajustement = confirmer la provenance des 4 flags de détection (F-205 vs F-206). Verdict fonctionnel acquis — la présente skill ne le re-challenge pas.

## Parcours écran réel de l'avocat (ouverture du dossier → état terminal)

**Source** : référentiel `docs/business/parcours-ecran-dossier.md` (écran réellement codé `case-file-detail.component`, structure 4 onglets depuis F-244).

1. L'avocat ouvre un dossier → écran **détail du dossier**, 4 onglets (Dossier / Analyse / Décision / Suivi).
2. **En-tête** : titre, actions, `app-case-dashboard-stepper`.
3. Onglet **Dossier** : métadonnées, stade procédural (F-243), import et liste des pièces.
4. Onglet **Analyse** : lancement du pipeline IA, accès à la **synthèse** (faits, timeline, points juridiques, risques, questions ouvertes).
5. L'avocat renseigne le stade procédural (onglet Dossier).
6. Onglet **Décision** : l'avocat remplit les **outils décisionnels** pertinents (`app-decisional-tools-panel`) — affichés conditionnellement selon la détection IA. ⬅ **F-206 s'insère ici**
7. L'avocat consulte le **tableau de bord décisionnel** (`app-case-dashboard`) — verdicts agrégés.
8. L'avocat **génère le projet de conclusions** (`app-conclusions-section`, onglet Décision, bas — F-98).
9. L'avocat relit, copie, finalise dans son traitement de texte.
10. Onglet **Suivi** : échéances, notes, calendrier procédural jusqu'à l'audience.
11. **État terminal** : projet de conclusions généré.

## État terminal du processus (explicite)

✅ Déjà tranché par le cadrage écran F-98 (2026-05-18) : l'état terminal du traitement métier d'un dossier = **« projet de conclusions généré »** (`app-conclusions-section`, onglet Décision). **F-206 ne déplace pas cet état terminal** : les 4 outils sont en amont des conclusions (étape 6 du parcours), ils alimentent la matière que la génération de conclusions consolide. L'état terminal reste inchangé.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours écran | Écran / zone LegalCase | Statut |
|---|---|---|
| 1-3. Ouverture, en-tête, onglet Dossier | `case-file-detail.component` — onglet Dossier | ✅ existant |
| 4. Onglet Analyse, synthèse | `app-analysis-pipeline`, `SynthesisComponent` | ✅ existant |
| 5. Stade procédural | `detail-card` (onglet Dossier) — F-243 | ✅ existant |
| 6. **Outils décisionnels** | `app-decisional-tools-panel` (onglet Décision) — **zone d'accueil de F-206** | ✅ existant |
| 7. Tableau de bord décisionnel | `app-case-dashboard` (onglet Décision) — F-167 | ✅ existant |
| 8. Génération de conclusions | `app-conclusions-section` (onglet Décision) — F-98 | ✅ existant |
| 10. Échéances / suivi | `app-case-deadlines-section` (onglet Suivi) — F-69 | ✅ existant |

Toutes les zones du parcours touchées par F-206 existent. **Aucune zone à créer.**

## Position candidate de la feature

- **Écran** : détail du dossier (`case-file-detail.component`).
- **Onglet** : **Décision** (index 2).
- **Zone** : `app-decisional-tools-panel` — les 4 outils sont 4 composants `*-section` enfants du panneau (pattern canonique des ~103 outils décisionnels existants).
- **Points d'entrée** : aucun point d'entrée navigationnel — affichage **conditionnel** piloté par F-IA-04 sur détection IA de la situation (flags `abandon_poste_detecte`, `arret_maladie_long_detecte`, `prise_acte_envisagee`, `resiliation_judiciaire_envisagee`). L'avocat ne « va pas » vers l'outil : l'outil apparaît quand le dossier le justifie.

## Challenge placement

✅ **Correct.** L'onglet Décision / `app-decisional-tools-panel` est exactement l'étape 6 du parcours — le moment où l'avocat remplit les outils décisionnels. Les 4 outils de F-206 sont des outils décisionnels de même nature que les ~103 déjà présents dans ce panneau (F-DT, F-IM, F-FA). Aucun emplacement alternatif n'aurait de sens : ce sont des analyseurs/calculateurs, pas des écrans autonomes.

## Challenge lisibilité de la séquence

✅ **Globalement correct, 1 point dur.** L'ordre synthèse → outils décisionnels est rendu lisible par la structure en onglets (Analyse précède Décision) et par `app-case-dashboard-stepper`. Les 4 outils héritent de cette séquence.

⚠ **Point dur — décalage inter-onglets sur les échéances.** L'outil « abandon de poste » produit une **échéance datée** (date butoir des 15 jours après la mise en demeure) ; l'outil « congés payés / arrêt maladie » produit une **date de prescription** de l'action. Ces échéances vivent dans l'onglet **Suivi** (F-69), alors que l'outil est dans l'onglet **Décision**. C'est le même motif que la branche « échec d'extraction » SF-121-06 : un signal ne doit jamais être orphelin de son onglet d'action. → Ajustement requis (cf. ci-dessous).

## Challenge charge écran

✅ **Maîtrisée.** Les 4 outils ne sont **pas** des blocs primaires : ce sont des sous-sections de `app-decisional-tools-panel`, lui-même un seul bloc primaire. L'onglet Décision conserve ses **3 blocs primaires** (panneau d'outils décisionnels · tableau de bord décisionnel · conclusions) — le seuil « ~3 blocs primaires par onglet » du référentiel est respecté.

La densité **interne** du panneau est maîtrisée par deux mécanismes déjà livrés :
- **F-IA-04** — affichage conditionnel : le panneau n'affiche un outil que si l'IA a détecté sa situation. Sur un dossier d'abandon de poste, seul cet outil-là apparaît, pas les 102 autres.
- **F-169** — grille 2 colonnes + groupement par thème métier : les outils sont rangés par famille, pas en liste plate.

## Challenge état final / continuité

✅ **Continuité assurée.** Après le calcul de chaque outil :
- son verdict alimente le **tableau de bord décisionnel** (`app-case-dashboard`, F-167, même onglet) via un mapper `DashboardTile` ;
- sa matière est consolidée par la **génération de conclusions** (`app-conclusions-section`, F-98, même onglet) ;
- ses échéances alimentent l'onglet **Suivi** (F-69).

Aucun dead-end, aucun ping-pong subi. L'avocat poursuit naturellement vers le tableau de bord puis les conclusions. L'état terminal (« projet de conclusions généré ») est inchangé.

## Ajustements IA requis

1. **Affichage `CONTEXTUAL`** — les 4 outils sont déclarés `CONTEXTUAL` dans les règles de visibilité F-IA-04, jamais `ALWAYS_ON` (sinon pollution du panneau, régression F-165). Chaque outil n'apparaît que sur détection IA de sa situation.
2. **Groupement thématique (F-169)** — ranger les outils dans des familles cohérentes : abandon de poste, prise d'acte et résiliation judiciaire dans la famille **« Rupture du contrat — initiative salarié / torts employeur »** ; congés payés pendant arrêt maladie dans la famille **« Rappels et indemnités salariales »** (ce n'est pas une rupture mais un rappel de droits — ne pas le ranger avec les ruptures).
3. **Échéance non orpheline** — toute échéance produite par un outil (date butoir 15 j abandon de poste ; date de prescription rappel de congés payés) est matérialisée dans l'onglet **Suivi** via F-69 ; la section de l'outil (onglet Décision) affiche l'échéance et **nomme l'onglet Suivi** comme lieu de suivi de l'échéance (invariant inter-onglets SF-121-06).
4. **Tile de dashboard (F-167)** — chaque outil livre son mapper `DashboardTile` pour apparaître dans le tableau de bord décisionnel agrégé ; sinon outil dormant (régression F-180).

## Invariants anti-surcharge pour la mini-spec

1. **Pas de bloc primaire nouveau** — les 4 outils sont des sous-sections de `app-decisional-tools-panel`. L'onglet Décision reste à 3 blocs primaires. Interdiction d'ajouter un onglet ou un bloc autonome.
2. **Affichage conditionnel obligatoire** — `CONTEXTUAL` via F-IA-04 ; le panneau ne montre que les outils dont la situation est détectée.
3. **Toute sortie a un point de continuité explicite** — verdict → `DashboardTile` (F-167) + matière → conclusions (F-98). Aucun outil ne se termine en dead-end.
4. **Échéance jamais orpheline de son onglet** — une échéance produite par un outil de l'onglet Décision est matérialisée dans l'onglet Suivi (F-69) et la section de l'outil le signale explicitement.
5. **Groupement thématique respecté** — chaque outil rejoint une famille F-169 cohérente ; ne pas mélanger « rupture » et « rappel de droits ».

## Décision finale

🟢 **GO avec ajustements.** F-206 s'insère dans une zone conçue pour les outils décisionnels (`app-decisional-tools-panel`, onglet Décision), à l'étape exacte du parcours où l'avocat en a besoin, sans créer de bloc primaire ni surcharger l'écran (F-IA-04 + F-169 absorbent la densité). Les 4 ajustements IA ci-dessus sont à intégrer à la mini-spec. Étape suivante : 1 — mini-specs des SF de F-206.

## MAJ apportée au parcours écran de référence

Ajout d'une ligne à l'historique des passages de `docs/business/parcours-ecran-dossier.md` (6ᵉ passage, F-206) et d'une note sur l'invariant « échéance non orpheline » pour les outils décisionnels produisant un délai daté.
