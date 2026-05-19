# F-207 — Cadrage cohérence écran (étape 0 bis)

## Verdict : GO avec ajustements

## Intention métier + comportement visible attendu

8 nouveaux outils décisionnels Travail BE-only s'ajoutent au **panneau outils décisionnels** (onglet Décision du détail dossier) — visibles **uniquement pour les workspaces `country=BELGIUM`**, masqués pour les workspaces FR. Chacun ouvre une `*-section.component` (formulaire + verdict), pré-remplie par l'IA, suivant le pattern canonique `immigration-title-decision-section`.

## Rappel verdict feature-coherence-challenger (étape 0)

**GO** — toutes les briques d'infrastructure amont/aval sont livrées (panneau, gate workspaceCountry, prefill `TravailExtractedData`, validation F-IA-03 / `critereCode`, pattern F-IA-04). Effort sur la substance juridique BE et l'isolation BE-only.

## Parcours écran réel de l'avocat BE

Source : `docs/business/parcours-ecran-dossier.md` + écran réellement codé (`case-file-detail`, 4 onglets depuis F-244) + audit BE travail.

1. L'avocat BE ouvre un dossier de rupture / AT / RCC → écran **détail du dossier**, 4 onglets.
2. Onglet **Dossier** : pièces uploadées (C4 ONEM, contrat, fiches de paie, déclaration Fedris si AT…). Extraction + analyse IA standard.
3. Onglet **Analyse** : pipeline IA, synthèse — la situation BE est détectée (statut salarié, type rupture, AT le cas échéant, âge 45+, etc.).
4. Onglet **Décision** : panneau outils décisionnels (`app-decisional-tools-panel`) filtré par `workspaceCountry=BELGIUM`. Les **8 nouveaux outils F-207** apparaissent ici, dans un ordre suivant la séquence métier.
5. L'avocat ouvre l'outil **prescription-be-litige-travail** en premier (transversal — délai 1 an). Vérifie la non-forclusion avant tout.
6. Selon la situation détectée par l'IA : C4 ONEM → outils 2/3 ; AT → outil 4 ; urgence → outil 5 ; salarié âge → outils 6/7/8.
7. Chaque outil restitue un verdict + un calculateur de délais / sanction / éligibilité.
8. Le **dashboard décisionnel** (F-IA-02) du dossier agrège les verdicts.
9. Onglet **Décision** (suite) : **génération du projet de conclusions** (`app-conclusions-section`, F-98). Les verdicts BE alimentent les conclusions.
10. **État terminal** : projet de conclusions généré (inchangé, cf. référentiel parcours écran).

## État terminal du processus

**Inchangé** — « projet de conclusions généré » (tranché par F-98). F-207 enrichit la chaîne de décision **avant** la génération des conclusions, sans déplacer l'état terminal.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours | Écran / zone | Statut |
|---|---|---|
| 1-3. Upload, analyse, synthèse | onglets Dossier + Analyse | ✅ inchangés |
| 4. Panneau outils décisionnels (filtre BE) | onglet Décision — `app-decisional-tools-panel` | ✅ existant — accueille N entrées via `TOOL_REGISTRY` |
| 5-7. Outils F-207 (8 sections) | onglet Décision — 8 nouvelles `*-section.component` | ❌ **manquant — apport F-207** |
| 8. Dashboard décisionnel | onglet Décision — `app-case-dashboard` | ✅ existant — agrège automatiquement les nouveaux verdicts |
| 9-10. Conclusions | onglet Décision — `app-conclusions-section` (F-98) | ✅ existant |

## Position candidate de la feature

Les 8 outils s'insèrent **à l'intérieur du panneau outils décisionnels** (onglet Décision) — **aucun bloc primaire nouveau d'écran**. Chacun est une entrée `TOOL_REGISTRY` standard (instanciation conditionnelle au `workspaceCountry === 'BELGIUM'`).

## Challenge placement

L'écran cible (onglet **Décision** → `app-decisional-tools-panel`) est le placement standard de tous les outils décisionnels existants (Travail FR, Immigration FR/BE, Famille FR/BE). Cohérent — l'avocat BE y trouve déjà ses outils F-198 / F-204. ✅ Placement juste.

## Challenge lisibilité de la séquence

⚠️ **Ajustement requis** : les 8 outils doivent apparaître dans un **ordre métier lisible**. L'audit fixe l'ordre prioritaire (§4.3 « Top 10 outils prioritaires à livrer en premier ») :
1. `prescription-be-litige-travail` — **P1 transversal** (impacte tous les autres ; vérifié en premier dans toute consultation post-rupture).
2. `c4-onem-checklist` — C4 documents fin de contrat, première étape post-rupture.
3. `contestation-c4-onem` — déclenché si C4 conteste / exclusion ONEM.
4. `at-fedris-declaration` — branche AT séparée (délai 8 jours).
5. `refere-tribunal-travail-be` — urgences procédurales transversales.
6. `rcc-be-conditions` — régime RCC (séquence séparée).
7. `rcc-be-indemnite-complementaire` — suite RCC si éligible.
8. `outplacement-be-obligatoire-45` — branche outplacement (salariés 45+).

**Mise en œuvre** : `TOOL_REGISTRY` BE ordonné selon cette séquence. ALWAYS_ON pour les transversaux (prescription, référé) ; CONTEXTUAL pour les autres avec `trigger_field` détecté par l'IA (`c4_recu`, `at_declare`, `rcc_envisage`, `age_45plus_licencie`), à arbitrer par mini-spec.

## Challenge charge écran

Onglet **Décision** porte 3 blocs primaires (`app-decisional-tools-panel`, `app-case-dashboard`, `app-conclusions-section`) — seuil ~3 respecté (cf. référentiel parcours-écran). F-207 enrichit **le contenu interne** du bloc panneau, **pas de nouveau bloc primaire**. Le panneau lui-même est conçu pour absorber N outils (CONTEXTUAL trigger F-166 / F-IA-04). ✅ Charge écran respectée.

Pour un dossier rupture BE typique : prescription (ALWAYS_ON) + 1-3 outils contextuels (C4, contestation, référé) — densité raisonnable. Pour un dossier AT BE : prescription + AT Fedris + référé. Pour un dossier RCC : prescription + RCC conditions + RCC indemnité + outplacement éventuel.

## Challenge état final / continuité

Après le verdict de chaque outil :
- Refresh dashboard décisionnel (F-IA-02) — `triggerRefresh()` dans le `next:` du POST de validation (pattern SF-IA-02-03).
- Le verdict enrichit le projet de conclusions (F-98).

✅ Continuité préservée — chaque outil mène l'avocat vers la suite du parcours (dashboard + conclusions).

## Ajustements IA requis

1. **Ordre TOOL_REGISTRY BE** — séquence métier ci-dessus (prescription en premier).
2. **Workspace gate BE-only strict** — chaque entrée `TOOL_REGISTRY` filtrée par `workspaceCountry === 'BELGIUM'`. Test d'isolation `country=FRANCE` → outil masqué, **obligatoire par SF frontend**.
3. **ALWAYS_ON vs CONTEXTUAL** à arbitrer par outil :
   - `prescription-be-litige-travail` → **ALWAYS_ON BE** (transversal).
   - `refere-tribunal-travail-be` → ALWAYS_ON BE (urgence transversale, à confirmer en mini-spec).
   - Les 6 autres → CONTEXTUAL avec `trigger_field` IA (à cadrer par mini-spec).
4. **Pas d'agrégation visuelle** RCC conditions + RCC indemnité — deux entrées TOOL_REGISTRY distinctes (mémoire `feedback_decision_tools_one_per_situation`).
5. **Pré-remplissage IA obligatoire** sur tous les champs saisissables — chaque section frontend implémente `prefillFromAi()` + provenance + `getPrefillCount(input)` (pattern canonique F-IA-04 / F-246).

## Invariants anti-surcharge pour les mini-specs

- **Zéro bloc primaire nouveau** — enrichissement du contenu interne du panneau Décision uniquement.
- **`workspaceCountry === 'BELGIUM'` strict** — pas de fuite FR.
- **Ordre du panneau respecte la séquence métier** (prescription en premier).
- **Détection contextuelle (CONTEXTUAL trigger_field) cohérente avec les flags IA détectés** — pas de trigger orphelin (test d'intégrité visibilité doit rester vert : `decision_tool_visibility_rules` ↔ `TOOL_REGISTRY` ↔ extracteur IA).
- **Critères F-IA-03 préfixés `BE_*`** distincts des codes FR équivalents (jumeaux F-DT-33/34/35 émettent leurs propres codes FR).

## Décision finale

**GO avec ajustements.** Placement correct (panneau Décision standard, BE-only). Charge écran nulle (aucun bloc primaire nouveau, panneau extensible by design). Lisibilité séquence requise : `TOOL_REGISTRY` BE ordonné selon la séquence métier (prescription en tête, AT et RCC en branches séparées). Les 5 ajustements IA ci-dessus sont à intégrer dans chaque mini-spec.

## MAJ apportée au parcours écran de référence

`docs/business/parcours-ecran-dossier.md` enrichi : 6ᵉ passage — ajout du flux outils décisionnels Travail BE-only (sequence prescription → C4 → contestation → AT → urgences → RCC → outplacement), avec invariant « workspaceCountry BE-only strict » et « ordre lisible du panneau respecte la séquence métier ».
