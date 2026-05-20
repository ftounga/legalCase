# F-212 — Cadrage cohérence écran (étape 0 bis)

> Produit par la skill `ai-skills/screen-coherence-challenger.md`. Étape 0 bis du cycle de gouvernance.
> Feature : **F-212 — P2 Travail FR — ~22 outils fréquence haute**.
> Date : 2026-05-20.

## Verdict : 🟢 GO avec ajustements

Les 22 outils P2 s'insèrent dans le panneau d'outils décisionnels (`app-decisional-tools-panel`) de l'onglet **Décision** — exactement la même zone que F-206 et les ~28 outils décisionnels Travail FR existants. Aucun nouveau bloc primaire ni nouvel onglet. Trois ajustements à intégrer à chaque mini-spec : affichage `CONTEXTUAL` obligatoire, groupement thématique F-169, tile `DashboardTile` F-167.

## Intention métier + comportement visible attendu

L'avocat, dans l'onglet **Décision** d'un dossier Travail FR, voit apparaître — **uniquement quand l'IA a détecté la situation** — la section décisionnelle correspondant à la situation P2 identifiée dans les pièces. Chaque section affiche des champs pré-remplis par l'IA, un verdict tranché et alimente le tableau de bord décisionnel. Sur un dossier sans aucune situation P2 détectée, aucun des 22 outils n'est visible.

## Rappel verdict feature-coherence-challenger (étape 0)

🟢 **GO avec ajustements** (`SF-212-00-coherence.md`, 2026-05-20). Aucun trou amont/aval bloquant. Ajustements = 9 invariants anti-gadget à respecter dans chaque mini-spec. Verdict fonctionnel acquis.

## Parcours écran réel de l'avocat (ouverture du dossier → état terminal)

**Source** : référentiel `docs/business/parcours-ecran-dossier.md` (structure 4 onglets depuis F-244).

1. L'avocat ouvre un dossier → écran **détail du dossier** (4 onglets : Dossier / Analyse / Décision / Suivi).
2. En-tête : titre, actions, `app-case-dashboard-stepper`.
3. Onglet **Dossier** : métadonnées, stade procédural (F-243), import et liste des pièces.
4. Onglet **Analyse** : pipeline IA, synthèse (faits, timeline, points juridiques, risques, questions).
5. L'avocat renseigne le stade procédural (onglet Dossier).
6. Onglet **Décision** : `app-decisional-tools-panel` — affichage conditionnel selon détection IA. ⬅ **F-212 s'insère ici**
7. L'avocat consulte le **tableau de bord décisionnel** (`app-case-dashboard`) — verdicts agrégés.
8. L'avocat **génère le projet de conclusions** (`app-conclusions-section`, F-98).
9. L'avocat relit, copie, finalise dans son traitement de texte.
10. Onglet **Suivi** : échéances, notes, calendrier procédural jusqu'à l'audience.
11. **État terminal** : projet de conclusions généré.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours écran | Écran / zone LegalCase | Statut |
|---|---|---|
| 1-3. Ouverture, en-tête, onglet Dossier | `case-file-detail.component` — onglet Dossier | ✅ existant |
| 4. Onglet Analyse, synthèse | `app-analysis-pipeline`, `SynthesisComponent` | ✅ existant |
| 5. Stade procédural | `detail-card` (onglet Dossier) — F-243 | ✅ existant |
| 6. **Outils décisionnels** | `app-decisional-tools-panel` (onglet Décision) — **zone d'accueil F-212** | ✅ existant |
| 7. Tableau de bord décisionnel | `app-case-dashboard` (onglet Décision) — F-167 | ✅ existant |
| 8. Génération de conclusions | `app-conclusions-section` (onglet Décision) — F-98 | ✅ existant |
| 10. Échéances / suivi | `app-case-deadlines-section` (onglet Suivi) — F-69 | ✅ existant |

**Toutes les zones sont existantes. Aucune zone à créer.**

## État terminal du processus

✅ Inchangé : **« projet de conclusions généré »** (`app-conclusions-section`, F-98). F-212 se place à l'étape 6 du parcours, en amont des conclusions, sans déplacer cet état terminal.

## Position candidate de la feature

- **Écran** : détail du dossier (`case-file-detail.component`).
- **Onglet** : **Décision** (index 2).
- **Zone** : `app-decisional-tools-panel` — les 22 outils sont 22 composants `*-section` enfants du panneau (pattern canonique des ~28 outils décisionnels Travail FR existants).
- **Points d'entrée** : aucun point d'entrée navigationnel — affichage **conditionnel** piloté par F-IA-04 sur détection IA.

## Challenge placement

✅ **Correct.** L'onglet Décision / `app-decisional-tools-panel` est exactement l'étape 6 du parcours. Les 22 outils P2 sont des outils décisionnels de même nature que les 28 déjà présents. Aucun emplacement alternatif cohérent.

## Challenge lisibilité de la séquence

✅ **Globalement correct.** L'ordre synthèse → outils décisionnels est rendu lisible par la structure en onglets (Analyse précède Décision) et par `app-case-dashboard-stepper`. Les 22 outils héritent de cette séquence.

⚠ **Point dur — groupement thématique.** 22 nouveaux outils s'ajoutent aux 28 existants dans le panneau. Sans groupement thématique clair, la liste devient illisible. **Ajustement requis** : chaque outil P2 doit être rangé dans la bonne famille F-169.

**Mapping familles F-169 pour les 22 outils P2 :**

| Outil | Famille F-169 |
|---|---|
| Faute grave / lourde | Rupture du contrat — motif disciplinaire |
| Démission équivoque | Rupture du contrat — initiative salarié |
| Rupture anticipée CDD | Rupture du contrat — CDD |
| Modification contrat refus | Modification / mobilité du contrat |
| Mutation clause mobilité | Modification / mobilité du contrat |
| Transfert entreprise L. 1224-1 | Modification / mobilité du contrat |
| CSP/CRP conformité | Licenciement économique |
| PDV / RCC conformité | Licenciement économique |
| Forfait jours validité | Temps de travail |
| Temps partiel requalification | Temps de travail |
| Télétravail accord | Temps de travail |
| Mise à pied disciplinaire | Sanctions disciplinaires |
| Égalité salariale F/H | Discrimination / harcèlement |
| Lanceur d'alerte protection | Discrimination / harcèlement |
| Burn-out reconnaissance MP | Sécurité sociale / AT-MP |
| Faute inexcusable employeur | Sécurité sociale / AT-MP |
| Congé maternité / paternité | Congés et absences |
| Élections CSE conformité | IRP / Négociation collective |
| Conciliation CPH (BCO/BCA) | Procédure CPH |
| Exécution jugement CPH (AGS) | Procédure CPH |
| VRP statut + indemnité clientèle | Régimes catégoriels |
| Particulier employeur (CESU) | Régimes catégoriels |

## Challenge charge écran

✅ **Maîtrisée par les mécanismes existants.**

- **F-IA-04** : affichage conditionnel — sur un dossier standard, seuls les outils dont la situation est détectée sont visibles. Un dossier de licenciement économique PME verra typiquement 2-3 outils P2 (CSP/CRP, faute grave éventuelle, forfait jours si cadre), pas les 22.
- **F-169** : groupement thématique 2 colonnes — les familles sont visuellement séparées, l'avocat scanne par famille, pas outil par outil.

Les 3 blocs primaires de l'onglet Décision restent inchangés (panneau outils · tableau de bord · conclusions).

## Challenge état final / continuité

✅ **Continuité assurée.** Pour chaque outil :
- son verdict alimente le **tableau de bord décisionnel** (`app-case-dashboard`, F-167) via mapper `DashboardTile` ;
- sa matière est consolidée par la **génération de conclusions** (`app-conclusions-section`, F-98) ;
- ses éventuelles échéances datées (ex. prescription CRRMP, délai conciliation) alimentent l'onglet **Suivi** (F-69).

Aucun dead-end. L'état terminal est inchangé.

## Ajustements requis

1. **Affichage `CONTEXTUAL`** — tous les 22 outils déclarés `CONTEXTUAL` dans les règles de visibilité F-IA-04. Jamais `ALWAYS_ON`.
2. **Groupement thématique (F-169)** — chaque outil est rangé dans sa famille selon le mapping ci-dessus.
3. **Tile de dashboard (F-167)** — chaque outil livre son mapper `DashboardTile` ; outil dormant sans tile (régression F-180).
4. **Pré-remplissage IA total (invariant F-246)** — badge `auto_awesome` par champ pré-rempli dans chaque section.
5. **Gate `country=FRANCE`** — bannière info côté frontend si workspace ≠ FRANCE (pas de masquage silencieux).

## Invariants anti-surcharge pour la mini-spec

1. **Pas de bloc primaire nouveau** — 22 outils = 22 sous-sections de `app-decisional-tools-panel`. L'onglet Décision reste à 3 blocs primaires.
2. **Affichage conditionnel obligatoire** — `CONTEXTUAL` via F-IA-04.
3. **Toute sortie a un point de continuité explicite** — verdict → `DashboardTile` (F-167) + matière → conclusions (F-98).
4. **Groupement thématique respecté** — chaque outil rejoint la famille F-169 identifiée ci-dessus.
5. **Pas de coexistence ALWAYS_ON** — aucun des 22 outils ne s'affiche sur un dossier vide.

## Décision finale

🟢 **GO avec ajustements.** F-212 s'insère dans la zone conçue pour les outils décisionnels, sans créer de bloc primaire ni surcharger l'écran (F-IA-04 + F-169 absorbent la densité des 22 outils). Les 5 ajustements ci-dessus sont à intégrer à chaque mini-spec. Étape suivante : 1 — mini-specs des 44 SF de F-212.

## MAJ apportée au parcours écran de référence

Ajout d'une ligne à l'historique des passages de `docs/business/parcours-ecran-dossier.md` (7e passage, F-212) : les 22 outils P2 rejoignent `app-decisional-tools-panel` dans les familles thématiques F-169 ; pas de nouveau bloc primaire.
