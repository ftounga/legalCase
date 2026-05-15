# Parcours écran — Détail du dossier

> Référentiel d'architecture de l'information construit incrémentalement par la skill `screen-coherence-challenger` (étape 0 bis du cycle de gouvernance). Chaque feature à impact écran sur le détail du dossier enrichit ce document.

**Écran** : `frontend/src/app/case-files/case-file-detail/case-file-detail.component`
**Utilisateur cible** : avocat traitant un dossier

---

## Parcours réel de l'avocat (ouverture du dossier → état terminal)

1. L'avocat ouvre un dossier → écran **détail du dossier**.
2. **En-tête** : titre du dossier, actions (export, fermer, rouvrir, supprimer) + `app-case-dashboard-stepper` qui matérialise les étapes du dossier.
3. **Métadonnées du dossier** (`mat-card.detail-card`) : domaine juridique, date de création, description. *Zone d'identité du dossier.*
4. **Stats d'usage** (`mat-card.stats-card`) : compteurs documents / analyses / tokens.
5. **Import des pièces** (`section#section-documents`, `docs-card`) : upload et liste des documents.
6. **Analyse** (`app-analysis-pipeline`) : lancement asynchrone du pipeline IA.
7. **Synthèse / dashboard décisionnel** (`section.decisional-summary-panel`, `app-case-dashboard`) : résultat de l'analyse.
8. **Outils décisionnels** (`app-decisional-tools-panel`) : calculators / analyzers métier.
9. **Échéances et notes** (`app-case-deadlines-section`, `app-case-notes-section`).
10. **État terminal** : ⚠ non explicite à ce jour.

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
| Synthèse décisionnelle | `section.decisional-summary-panel`, `app-case-dashboard` | Résultat IA |

→ **~10 blocs primaires.** Écran dense : toute nouvelle feature à impact écran doit privilégier l'intégration à un bloc existant plutôt que l'ajout d'un bloc autonome (cf. invariant anti-surcharge, cadrage F-243).

## Regroupements logiques

- **Identité du dossier** : en-tête + `detail-card`. Toute métadonnée d'identification (domaine juridique, **stade procédural** — F-243) appartient à ce groupe.
- **Usage** : `stats-card` — compteurs, sans lien avec l'identité.
- **Traitement** : documents → pipeline → synthèse → outils décisionnels. Séquence portée par le `app-case-dashboard-stepper`.
- **Annexes** : échéances, notes.

## État terminal du processus

⚠ **Non explicite à ce jour.** Le dossier porte un statut `OPEN`/`CLOSED`, mais le parcours ne matérialise pas *quand* le traitement métier est terminé. Avec F-98 (génération de conclusions), l'état terminal devrait devenir « conclusions générées et exportées ». **À articuler lors du cadrage écran de F-98.**

## Historique des passages

| Date | Feature | Apport au parcours |
|---|---|---|
| 2026-05-15 | F-243 (stade procédural) | Création du référentiel. Stade procédural classé dans le groupe « identité du dossier » (avec `detail-card`). État terminal identifié comme non explicite. |
