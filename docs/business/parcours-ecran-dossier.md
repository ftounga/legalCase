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
7. L'avocat consulte le **tableau de bord décisionnel** (`app-case-dashboard`) — verdicts agrégés des outils.
8. L'avocat **génère le projet de conclusions** (`app-conclusions-section`, onglet Décision, bas) — consolidation de synthèse + stade + outils + pistes stratégiques. ⬅ **F-98**
9. L'avocat relit le projet, le copie, le finalise dans son traitement de texte.
10. Onglet **Suivi** : échéances, notes, calendrier procédural jusqu'à l'audience.
11. **État terminal** : projet de conclusions généré (cf. section dédiée).

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

## Note — écran synthèse (sous-écran de l'onglet Analyse)

L'onglet **Analyse** du détail du dossier mène à un **écran dédié de synthèse** (`SynthesisComponent`, route `/case-files/:id/synthesis`) : un `mat-accordion` de panneaux conditionnels (chronologie, faits, points juridiques, risques, questions, pièces manquantes, indemnités, pistes stratégiques, checklist). Cet écran absorbe les nouveaux panneaux par conception (accordéon extensible) — il ne suit pas le seuil « 3 blocs primaires » du détail du dossier. Le parcours écran de la synthèse a été documenté lors du cadrage F-179 (`docs/features/F-179/SF-179-00b-ux-coherence.md`).
