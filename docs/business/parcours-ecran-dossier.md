# Parcours écran — Détail du dossier

> Référentiel d'architecture de l'information construit incrémentalement par la skill `screen-coherence-challenger` (étape 0 bis du cycle de gouvernance). Chaque feature à impact écran sur le détail du dossier enrichit ce document.

**Écran** : `frontend/src/app/case-files/case-file-detail/case-file-detail.component`
**Utilisateur cible** : avocat traitant un dossier

---

## Structure de l'écran — 4 onglets (depuis F-244 SF-244-01)

Depuis F-244, le détail du dossier est organisé en **4 onglets** (`mat-tab-group`), sous un en-tête plein largeur. Les contenus sont rendus en permanence et masqués via `[hidden]` (pas de lazy mount) pour préserver l'état SSE/polling.

| Onglet | Index | Contenu |
|---|---|---|
| **Dossier** | 0 | Métadonnées (identité), stade procédural (F-243), import / liste des pièces |
| **Analyse** | 1 | Pipeline d'analyse IA, accès à la synthèse |
| **Décision** | 2 | Outils décisionnels, tableau de bord décisionnel, **génération de conclusions (F-98)** |
| **Suivi** | 3 | Échéances, notes, calendrier procédural |

---

## Parcours réel de l'avocat (ouverture du dossier → état terminal)

1. L'avocat ouvre un dossier → écran **détail du dossier**, 4 onglets.
2. **En-tête** : titre du dossier, actions (export, clôturer, rouvrir, supprimer) + `app-case-dashboard-stepper`.
3. Onglet **Dossier** : métadonnées (domaine juridique, date, description), **stade procédural** (F-243 — juridiction + stade + position), import et liste des pièces.
4. Onglet **Analyse** : lancement asynchrone du pipeline IA (`app-analysis-pipeline`), accès à la **synthèse** du dossier (faits, timeline, points juridiques, risques, questions ouvertes).
5. L'avocat renseigne le **stade procédural** du dossier (onglet Dossier).
6. Onglet **Décision** : l'avocat remplit les **outils décisionnels** pertinents (`app-decisional-tools-panel`).
7. Chaque outil ouvre dans un **modal F-177** (`app-decision-tool-modal`, MatDialog 90vw/90vh) — l'avocat valide les champs pré-remplis IA, clique Enregistrer, et **lit sous le résultat le bloc « Jurisprudence applicable » F-JU-01** (1-3 arrêts structurants, chapeau officiel Cassation, lien Légifrance, bouton signaler) avant de fermer le modal.
8. L'avocat consulte le **tableau de bord décisionnel** (`app-case-dashboard`) — verdicts agrégés des outils.
9. L'avocat **génère le projet de conclusions** (`app-conclusions-section`, onglet Décision, bas) — consolidation de synthèse + stade + outils + pistes stratégiques + **arrêts F-JU-01** des outils utilisés. ⬅ **F-98**
10. L'avocat relit le projet, le copie, le finalise dans son traitement de texte.
11. Onglet **Suivi** : échéances, notes, calendrier procédural jusqu'à l'audience.
12. **État terminal** : projet de conclusions généré (cf. section dédiée).

## Zones de l'écran (blocs primaires, par onglet)

| Onglet | Zone | Composant / sélecteur | Rôle |
|---|---|---|---|
| (header) | En-tête | `detail-header` | Titre + actions |
| (header) | Stepper | `app-case-dashboard-stepper` | Étapes du dossier |
| Dossier | Métadonnées (identité) | `mat-card.detail-card` | Domaine juridique, date, description, **stade procédural** |
| Dossier | Stats | `mat-card.stats-card` | Compteurs d'usage |
| Dossier | Documents | `section#section-documents`, `docs-card` | Import / liste des pièces |
| Analyse | Pipeline | `app-analysis-pipeline` | Suivi de l'analyse |
| Décision | Outils décisionnels | `app-decisional-tools-panel` | Calculators / analyzers |
| Décision | **Stratégie de dossier** (coiffe du tableau de bord, pas un bloc primaire) | `app-case-strategy` | Reco stratégique LLM en lecture des verdicts calculés + synthèse (F-286) |
| Décision | Tableau de bord décisionnel | `app-case-dashboard` | Verdicts agrégés des outils décisionnels |
| Décision | **Conclusions** | `app-conclusions-section` | Génération du projet de conclusions (F-98) |
| Suivi | Échéances | `app-case-deadlines-section` | Délais |
| Suivi | Notes | `app-case-notes-section` | Notes internes |

→ La structure en onglets (F-244) a réparti la charge de l'écran. **Seuil par onglet** : un onglet ne dépasse pas ~3 blocs primaires. L'onglet Décision en porte 3 après F-98 — toute capacité F-98 ultérieure (éditeur, versions) s'intègre **dans** `app-conclusions-section`, pas comme bloc autonome.

## Regroupements logiques

- **Identité du dossier** : en-tête + `detail-card` (onglet Dossier). Domaine juridique, **stade procédural** (F-243).
- **Traitement** : documents (Dossier) → analyse + synthèse (Analyse) → outils décisionnels + tableau de bord (Décision) → **conclusions (Décision)**. C'est la chaîne de production métier.
- **Annexes / suivi** : échéances, notes, calendrier (onglet Suivi).

## État terminal du processus

✅ **Tranché par le cadrage écran F-98 (2026-05-18).** L'état terminal du traitement métier d'un dossier = **« projet de conclusions généré »** (`app-conclusions-section`, onglet Décision). C'est la dernière production substantielle de l'avocat dans le produit. En V1 (SF-98-01), l'avocat copie le texte généré ; l'export Word/PDF (SF-98-50/51) fluidifiera la sortie. Le suivi procédural (onglet Suivi) est un accompagnement post-conclusions. Le statut `OPEN/CLOSED` du dossier reste une action administrative distincte de l'état terminal métier.

## Historique des passages

| Date | Feature | Apport au parcours |
|---|---|---|
| 2026-05-15 | F-243 (stade procédural) | Création du référentiel. Stade procédural classé dans le groupe « identité du dossier ». État terminal identifié comme non explicite. |
| 2026-05-15 | Audit outils décisionnels (`screen-coherence-challenger`) | 2ᵉ passage. Verdict GO avec ajustements. Synthèse (écran/onglet) ≠ tableau de bord décisionnel. |
| 2026-05-18 | F-98 (cadrage écran SF-98-00b) | 3ᵉ passage. Intégration de la structure en 4 onglets (F-244). Ajout de l'étape 8 « génération conclusions » dans l'onglet Décision (`app-conclusions-section`, bas). **État terminal tranché : « projet de conclusions généré »** — n'est plus non explicite. Verdict GO avec ajustements. |
| 2026-05-18 | F-179 (vérification jurisprudence citée) | Verdict cohérence écran GO avec ajustements. F-179 vit dans l'**écran synthèse** (`SynthesisComponent`, route `/case-files/:id/synthesis`), sous-écran atteint depuis l'onglet Analyse — pas dans le détail du dossier. Le parcours détaillé de l'écran synthèse est documenté dans `docs/features/F-179/SF-179-00b-ux-coherence.md`. Aucune modification du parcours du détail du dossier. |
| 2026-05-18 | F-242 (citation jurisprudence d'appui) | 4ᵉ passage. Verdict GO avec ajustements. F-242 enrichit le panneau **« Points juridiques »** de l'écran synthèse en per-item (champ « Jurisprudence à l'appui ») — aucun bloc primaire nouveau. Acte le **« retour » de la chaîne jurisprudence** : F-241 = aller (deeplink vers l'éditeur), F-242 = retour (l'avocat ramène les références). Coexistence assumée de 3 briques jurisprudence sur la synthèse — F-179 (vérification des arrêts cités dans les documents), F-241 (aller), F-242 (retour) — à libellés distincts, non fusionnées. |
| 2026-05-19 | F-121 / SF-121-06 (échec d'extraction actionnable) | 5ᵉ passage. Verdict GO avec ajustements. Ajout de la **branche « échec d'extraction » du parcours** : quand une pièce échoue (étapes 3-8), l'avocat doit pouvoir réessayer/corriger ou retirer la pièce. Aucun bloc primaire nouveau — enrichissement interne du bloc Documents (onglet Dossier) et de la step 2 du pipeline (onglet Analyse). **Point dur explicité : décalage inter-onglets** — le signal d'échec le plus visible (step 2 rouge) est sur l'onglet Analyse, l'action de récupération (corbeille / ré-upload) sur l'onglet Dossier ; un signal d'échec ne doit jamais être orphelin de son action → la step 2 en échec renvoie explicitement vers l'onglet Dossier. |
| 2026-05-19 | F-206 (cadrage écran SF-206-00b — 4 outils d'urgences procédurales Travail FR) | 6ᵉ passage. Verdict GO avec ajustements. Les 4 outils (abandon de poste / présomption de démission, congés payés pendant arrêt maladie, prise d'acte de la rupture, résiliation judiciaire) sont des sous-sections de `app-decisional-tools-panel` (onglet Décision) — **aucun bloc primaire nouveau**, l'onglet Décision reste à 3 blocs primaires. Affichage `CONTEXTUAL` via F-IA-04, groupement thématique F-169. **Point dur explicité : échéance inter-onglets** — un outil décisionnel qui produit un délai daté (butoir 15 j de l'abandon de poste, prescription du rappel de congés payés) le matérialise dans l'onglet Suivi (F-69) ; l'échéance ne doit jamais être orpheline de son onglet d'action. |
| 2026-05-20 | F-214 (cadrage écran SF-214-00b — 22 outils P2 Immigration FR) | **7ᵉ passage**. Verdict GO avec ajustements. Les 22 outils P2 s'insèrent dans `app-decisional-tools-panel` (onglet Décision) — **aucun bloc primaire nouveau**, l'onglet Décision reste à 3 blocs primaires. 19 outils CONTEXTUAL (flags IA F-201/F-214) + 3 ALWAYS_ON (renouvellement délai, VLS-TS, autorisation travail employeur). **Point dur ajouté : tout outil P2 produisant un délai daté** (renouvellement, VLS-TS, appel CAA/CE, assignation résidence, MNA/JE, naturalisation recours, retrait titre, AJ CNDA) **crée une deadline dans l'onglet Suivi** (F-69) — invariant anti-orphelin d'échéance. Groupement thématique : DELAIS / STATUT_SEJOUR / AES / SANTE / ASILE / VICTIMES / CONTENTIEUX / MINEURS / ADMINISTRATIF. |
| 2026-05-21 | F-JU-01 (cadrage écran SF-JU-01-00b — Citations jurisprudentielles dans outils décisionnels) | **8ᵉ passage**. Verdict GO avec ajustements. Insertion d'une **étape 7 bis** du parcours : lecture des arrêts dans le **modal F-177 d'un outil**, **sous le résultat du calcul**. Composant `<app-tool-jurisprudence-citations [toolId] [branchActive]>` standalone, interface `ToolJurisprudenceCitable` exposée par les composants outils. **Aucun bloc primaire nouveau** — l'onglet Décision reste à 3 blocs primaires (`app-decisional-tools-panel`, `app-case-dashboard`, `app-conclusions-section`) ; F-JU-01 enrichit l'**intérieur** des composants outils ouverts en modal. **Point dur ajouté : pas de bloc primaire jurisprudence sur l'onglet Décision** — tout placement extérieur au modal d'outil est refusé. **Chaîne jurisprudence complétée à 4 briques distinctes** (libellés non confondables) : F-179 (vérification arrêts adverses, écran synthèse), F-241 (deeplinks vers éditeurs tiers, écran synthèse + V2 modal outil), F-242 (citation manuelle, écran synthèse), **F-JU-01 (citations proactives sur calcul, modal outil)**. Continuité vers F-98 conclusions à clarifier en mini-spec (la génération de conclusions doit pouvoir puiser dans `tool_jurisprudence_mappings`). |
| 2026-06-09 | F-98 / SF-98-56 (cadrage écran — réfutation jurisprudence adverse) | **9ᵉ passage**. Verdict GO avec ajustements. La section **F-179 « Jurisprudences citées »** (écran synthèse) gagne une **action de marquage « adverse à réfuter »** *inline par citation*, visible **uniquement sur les statuts SUSPECT/NOT_FOUND** — aucun bloc primaire nouveau (l'accordéon synthèse absorbe l'action locale). Les citations marquées alimentent une **réfutation** dans le projet de conclusions (onglet Décision, `app-conclusions-section`). **Point dur explicité : lisibilité de la séquence inter-écrans** — le marquage vit sur la Synthèse, son effet sur les conclusions vit sur l'onglet Décision → 2 mentions légères corrigent le maillon : (a) sous F-179 « les citations marquées alimenteront la réfutation des conclusions » (si ≥ 1 éligible), (b) après génération, signalement factuel de N citations adverses prises en compte (rien si N = 0). État terminal « projet de conclusions généré » inchangé, **enrichi de la réfutation**. Décision PO : Option A (sélection avocat) — cf. `docs/features/F-98/SF-98-56-00-coherence.md` + `SF-98-56-00b-ux-coherence.md`. |
| 2026-06-09 | F-260 (cadrage écran — numérotation persistante & ordre des pièces) | **10ᵉ passage**. Verdict GO avec ajustements. La **table des documents** (onglet Dossier, `case-file-detail` `#section-documents`) gagne une **colonne « N° »** (numéro de pièce persistant) et un **réordonnancement** par ligne (flèches haut/bas retenues plutôt que drag mat-table — plus robuste/accessible) — **aucun bloc primaire nouveau**. **Point dur explicité : l'ordre pilote la numérotation citée dans les actes** → hint « L'ordre des pièces détermine leur numérotation dans les conclusions et la fiche prud'homale » + tooltips flèches « renumérote les pièces dans les actes ». Le numéro devient **stable** (append à l'ajout, trou conservé à la suppression ; seul le réordonnancement explicite renumérote) et **source unique** lue par F-98, fiche prud'homale et fiche tribunal travail. Cf. `docs/features/F-260/SF-260-00-coherence.md` + `SF-260-00b-ux-coherence.md`. |
| 2026-06-10 | F-261 / SF-261-01 (cadrage écran — tag « écritures adverses ») | **11ᵉ passage**. Verdict GO avec ajustements. La **table des documents** (onglet Dossier, `case-file-detail` `#section-documents`) gagne une **action « Écritures adverses »** (bouton `gavel` inline) par document — **aucun bloc primaire nouveau** (s'ajoute à N° + réordonnancement F-260). Marque le document des conclusions adverses → ses **moyens** seront extraits (SF-261-02) puis réfutés (SF-261-03) dans les conclusions. **Point dur explicité : distinction vs SF-98-56** — le marquage des **citations** adverses vit sur l'écran **Synthèse** (« Marquer comme adverse à réfuter ») ; ce marquage-ci est au niveau **document**, onglet Dossier (« Écritures adverses ») → libellés distincts pour ne pas confondre citation et document. Finalité signalée sobrement (tooltip). Cf. `docs/features/F-261/SF-261-00-coherence.md` + `SF-261-01-00b-ux-coherence.md`. |
| 2026-06-10 | F-264 / SF-264-01 (cadrage écran — éditeur document natif) | **12ᵉ passage**. Verdict GO avec ajustements (Option A : markdown enrichi + aperçu live, sans dépendance). Le **mode édition** des conclusions (onglet Décision, `conclusions-section`) passe du textarea brut seul à un layout **barre d'outils markdown + aperçu formaté « acte » live** (réutilise `ConclusionDocumentComponent`). Côte à côte (large) / bascule Édition-Aperçu (étroit). **Point dur : round-trip** — le `content` reste **markdown** (export Word/PDF + versions intacts), markdown-safe only (pas de couleur/taille). Aucun nouvel écran ni bloc primaire ; enrichissement du mode édition existant. Cf. `docs/features/F-264/SF-264-00-coherence.md` + `SF-264-00b-ux-coherence.md`. |
| 2026-06-12 | F-286 / SF-286-01 (cadrage écran — stratégie de dossier unifiée) | **13ᵉ passage**. Verdict GO. La **colonne verdict** de l'onglet **Décision** (`decision-space__verdict`) gagne une **carte de coiffe « Stratégie de dossier »** (`app-case-strategy`) placée **au-dessus** du tableau de bord décisionnel — **aucun 4ᵉ bloc primaire** : c'est la lecture de plus haute altitude de la **même brique « consolidation des verdicts »** que le tableau de bord. Couche **LLM de synthèse en LECTURE** (décision PO) : un appel dédié lit les **verdicts des outils CALCULÉS** (`assembleDecisionToolTiles`) + la **synthèse** `DONE` et produit une reco lisible (voie référé/fond, posture concilier/plaider, priorisation des chefs, séquencement). **Point dur (reprise F-258) : honnêteté du vide** — 0 outil calculé → encart « Calculez vos outils décisionnels… » + compteur d'outils à calculer, **aucune stratégie inventée**. **Invariant 1 outil = 1 situation préservé** : pur ajout en lecture, F-286 n'écrit sur aucune table `*_analysis` ni `decision_tool_visibility_rules`, ne déplace/masque aucun outil. Reco regénérable (le dossier vit), date en JetBrains Mono, lien léger vers les conclusions. Cf. `docs/features/F-286/SF-286-00-coherence.md` + `SF-286-00b-ux-coherence.md`. |

## Note — branche « échec d'extraction » du parcours (depuis SF-121-06, 2026-05-19)

Quand une ou plusieurs pièces échouent à l'extraction, le parcours bifurque : le signal apparaît sur **deux onglets** — la ligne du document dans l'onglet **Dossier** (badge « Non analysable » + message spécifique au motif d'échec) et la step 2 du pipeline dans l'onglet **Analyse** (compteur « N non analysables / M »). L'**action** de récupération (corbeille, ré-upload) vit dans l'onglet **Dossier**. Invariant : la step 2 en échec (onglet Analyse) **oriente toujours** vers l'onglet Dossier — le signal nomme l'onglet de l'action. Le message d'échec est **spécifique au `failureReason`** (`OCR_UNSUPPORTED_SIZE` → découper le fichier ; `EMPTY_TEXT`/`OCR_FAILED` → retry OCR F-122-05 ; `CORRUPTED`/`UNSUPPORTED_FORMAT` → remplacer le fichier).

## Note — écran synthèse (sous-écran de l'onglet Analyse)

L'onglet **Analyse** du détail du dossier mène à un **écran dédié de synthèse** (`SynthesisComponent`, route `/case-files/:id/synthesis`) : un `mat-accordion` de panneaux conditionnels (chronologie, faits, points juridiques, risques, questions, pièces manquantes, indemnités, pistes stratégiques, checklist). Cet écran absorbe les nouveaux panneaux par conception (accordéon extensible) — il ne suit pas le seuil « 3 blocs primaires » du détail du dossier. Le parcours écran de la synthèse a été documenté lors du cadrage F-179 (`docs/features/F-179/SF-179-00b-ux-coherence.md`).

**Chaîne jurisprudence sur l'écran synthèse** (depuis F-242, 2026-05-18) — le panneau « Points juridiques » porte, par point juridique : le bouton deeplink **F-241** (ouvrir une recherche pré-remplie chez l'éditeur — l'« aller ») puis le champ **F-242** « Jurisprudence à l'appui » (saisir la référence retenue — le « retour »), citation reprise dans les conclusions générées (F-98). La section **F-179** « Jurisprudences citées » reste distincte : elle vérifie les arrêts cités dans les *documents* du dossier, ce n'est pas une saisie de l'avocat.

## Note — bloc « Jurisprudence applicable » dans le modal d'outil (depuis F-JU-01, 2026-05-21)

Quand l'avocat ouvre un outil décisionnel via le modal F-177 (`app-decision-tool-modal`, MatDialog 90vw/90vh) et que le résultat du calcul s'affiche, un bloc **« Jurisprudence applicable »** apparaît automatiquement **sous ce résultat** : 1 à 3 arrêts structurants (chapeau officiel Cassation cité textuellement, lien Légifrance en nouvel onglet, date de dernière vérification, bouton « Signaler un problème »). Composant standalone `<app-tool-jurisprudence-citations [toolId] [branchActive]>`, alimenté par la table globale `tool_jurisprudence_mappings`.

**Invariants** :
- Le bloc citations vit **dans** le composant outil ouvert en modal, jamais comme bloc primaire de l'onglet Décision (qui reste à 3 blocs : `app-decisional-tools-panel`, `app-case-dashboard`, `app-conclusions-section`).
- Si Claude < 60 % confiant sur la pertinence (ou outil non éligible), le bloc est **absent du DOM** (silence > erreur > placeholder).
- Le bloc se met à jour automatiquement quand la saisie avocat change la branche de calcul active (`Observable branchActive$`).
- Liens externes (Légifrance, Doctrine V2 via F-241) s'ouvrent toujours en nouvel onglet — pas de navigation interne perdante.
- Le bouton « Signaler » utilise un prompt inline ou un toast — pas de MatDialog imbriqué dans le modal d'outil.

**Chaîne jurisprudence à 4 briques distinctes** (depuis F-JU-01, 2026-05-21) :

| Brique | Écran | Sens |
|---|---|---|
| F-179 « Jurisprudences citées » | Synthèse | Vérification des arrêts cités dans les documents uploadés (conclusions adverses, défense) |
| F-241 deeplinks éditeurs | Synthèse (V2 : modal outil) | Aller — bouton « Ouvrir dans Doctrine / Lexis Plus / Lextenso » avec query pré-remplie |
| F-242 « Jurisprudence à l'appui » | Synthèse, per-item | Retour — saisie manuelle par l'avocat de la référence retenue |
| **F-JU-01 « Jurisprudence applicable »** | **Modal outil**, sous le résultat | **Citation proactive automatique — l'arrêt qui fonde le calcul** |

Libellés non confondables. F-JU-01 est **autoportant** (l'avocat n'a pas besoin de Doctrine pour valider le calcul), les 3 autres briques restent complémentaires.

## Note — invariant « échéance non orpheline » des outils décisionnels (depuis F-206, 2026-05-19)

Un outil décisionnel de l'onglet **Décision** (`app-decisional-tools-panel`) peut produire un **délai procédural daté** : c'est le cas des outils d'urgences procédurales F-206 (date butoir des 15 jours après mise en demeure pour l'abandon de poste ; date de prescription de l'action en rappel de congés payés). Ce délai se matérialise dans l'onglet **Suivi** (`app-case-deadlines-section`, F-69) — un onglet différent de celui où l'outil est rempli.

**Invariant** : une échéance produite par un outil ne doit jamais être orpheline de son onglet d'action. La section de l'outil (onglet Décision) **affiche l'échéance** et **nomme l'onglet Suivi** comme lieu de son suivi. Même logique que la branche « échec d'extraction » (SF-121-06) : un signal porté par un onglet pointe explicitement vers l'onglet qui porte l'action ou le suivi associé.
