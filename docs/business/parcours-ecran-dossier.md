# Parcours écran — Détail du dossier

> Référentiel d'architecture de l'information construit incrémentalement par la skill `screen-coherence-challenger` (étape 0 bis du cycle de gouvernance). Chaque feature à impact écran sur le détail du dossier enrichit ce document.

**Écran** : `frontend/src/app/case-files/case-file-detail/case-file-detail.component`
**Utilisateur cible** : avocat traitant un dossier

---

## Parcours réel de l'avocat (ouverture du dossier → état terminal)

1. L'avocat ouvre un dossier → écran **détail du dossier** (disposition en **2 colonnes** sous un en-tête plein largeur).
2. **En-tête** : titre du dossier, actions (export, clôturer, rouvrir, supprimer) + `app-case-dashboard-stepper` à 5 étapes (Documents → Analyse → Questions → Délais → Pièces manquantes).
3. **Métadonnées du dossier** (`mat-card.detail-card`, col-left) : domaine juridique, date de création, description. *Zone d'identité du dossier.*
4. **Stats d'usage** (`mat-card.stats-card`, col-left) : compteurs documents / analyses / tokens.
5. **Import des pièces** (`section#section-documents`, col-left) : upload et liste des documents.
6. **Analyse** (`app-analysis-pipeline`, col-right) : lancement asynchrone du pipeline IA.
7. **Synthèse** : ⚠ **pas un bloc de cet écran** — page dédiée `/case-files/:id/synthesis`, atteinte par le lien « Voir la synthèse » (col-right). Faits, timeline, points juridiques, risques, questions ouvertes.
8. **Outils décisionnels** (`app-decisional-tools-panel`, col-left, bas) : saisie des calculators / analyzers métier.
9. **Tableau de bord décisionnel** (`section.decisional-summary-panel`, `app-case-dashboard`, col-right, bas) : verdicts agrégés des outils. *Distinct de la synthèse.*
10. **Échéances et notes** (`app-case-deadlines-section`, `app-case-notes-section`, col-left).
11. **État terminal** : ⚠ non explicite à ce jour.

## Zones de l'écran (blocs primaires)

| Zone | Composant / sélecteur | Rôle |
|---|---|---|
| En-tête | `detail-header` | Titre + actions |
| Stepper | `app-case-dashboard-stepper` | Étapes du dossier |
| Métadonnées (identité) | `mat-card.detail-card` | Domaine juridique, date, description |
| Stats | `mat-card.stats-card` | Compteurs d'usage |
| Documents | `section#section-documents`, `docs-card` | Import / liste des pièces |
| Échéances | `app-case-deadlines-section` | Délais |
| Notes | `app-case-notes-section` | Notes internes |
| Outils décisionnels | `app-decisional-tools-panel` | Calculators / analyzers |
| Pipeline | `app-analysis-pipeline` | Suivi de l'analyse |
| Tableau de bord décisionnel | `section.decisional-summary-panel`, `app-case-dashboard` | Verdicts agrégés des outils décisionnels (col-right, bas) — distinct de la synthèse |

→ **~10 blocs primaires.** Écran dense : toute nouvelle feature à impact écran doit privilégier l'intégration à un bloc existant plutôt que l'ajout d'un bloc autonome (cf. invariant anti-surcharge, cadrage F-243).

## Regroupements logiques

- **Identité du dossier** : en-tête + `detail-card`. Toute métadonnée d'identification (domaine juridique, **stade procédural** — F-243) appartient à ce groupe.
- **Usage** : `stats-card` — compteurs, sans lien avec l'identité.
- **Traitement** : documents → analyse → synthèse (écran dédié) → outils décisionnels → tableau de bord décisionnel. ⚠ Le `app-case-dashboard-stepper` **ne porte pas** cette séquence : son modèle à 5 étapes (Documents/Analyse/Questions/Délais/Pièces manquantes) omet synthèse, outils et tableau de bord (cf. audit 2026-05-15).
- **Annexes** : échéances, notes.

## État terminal du processus

⚠ **Non explicite à ce jour.** Le dossier porte un statut `OPEN`/`CLOSED`, mais le parcours ne matérialise pas *quand* le traitement métier est terminé. Avec F-98 (génération de conclusions), l'état terminal devrait devenir « conclusions générées et exportées ». **À articuler lors du cadrage écran de F-98.**

## Historique des passages

| Date | Feature | Apport au parcours |
|---|---|---|
| 2026-05-15 | F-243 (stade procédural) | Création du référentiel. Stade procédural classé dans le groupe « identité du dossier » (avec `detail-card`). État terminal identifié comme non explicite. |
| 2026-05-15 | Audit outils décisionnels (`screen-coherence-challenger`) | 2ᵉ passage. Verdict GO avec ajustements. Correction : synthèse (écran dédié) ≠ tableau de bord décisionnel (bloc col-right) — étaient fusionnés à tort. Ajout : disposition 2 colonnes. 5 ajustements IA → backlog. Voir `docs/audits/AUDIT-2026-05-15-ux-coherence-detail-dossier.md`. |
