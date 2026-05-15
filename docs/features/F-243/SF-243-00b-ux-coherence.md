# F-243 — Cadrage cohérence écran (étape 0 bis)

**Date** : 2026-05-15
**Skill appliquée** : `ai-skills/screen-coherence-challenger.md` (premier usage réel)
**Note** : produit en rattrapage — l'étape 0 bis a été ajoutée au cycle CLAUDE.md pendant le dev de F-243. Le dev frontend SF-243-02 est livré mais **non mergé** ; ce cadrage est appliqué avant la PR pour corriger le placement si nécessaire.

---

## Verdict : GO avec ajustements

Placement globalement cohérent (l'encart vit bien dans l'écran détail du dossier), mais **2 ajustements de positionnement** requis avant merge.

## Intention métier + comportement visible attendu

L'avocat renseigne sur son dossier le stade procédural (juridiction + stade + position). Visible : un encart dans l'écran détail du dossier, en mode affichage (libellés humains) ou édition (3 sélecteurs en cascade).

## Rappel verdict feature-coherence-challenger (étape 0)

GO (`SF-243-00-coherence.md`) — feature de saisie pure, aucun trou fonctionnel amont.

## Parcours écran réel de l'avocat

Source : écran `case-file-detail.component.html` réellement codé + pratique avocat (⚠ parcours métier à valider).

1. L'avocat ouvre un dossier → écran **détail du dossier**
2. En-tête : titre, actions (export, fermer, supprimer) + `app-case-dashboard-stepper` (étapes du dossier)
3. **Métadonnées du dossier** : `mat-card.detail-card` — domaine juridique, date de création, description
4. Stats : `mat-card.stats-card` — compteurs (documents, analyses, tokens)
5. Import des pièces : `section#section-documents`
6. Lancement de l'analyse : `app-analysis-pipeline` (asynchrone)
7. Consultation de la synthèse / dashboard décisionnel : `section.decisional-summary-panel` + `app-case-dashboard`
8. Consultation des outils décisionnels : `app-decisional-tools-panel`
9. Gestion des échéances et notes : `app-case-deadlines-section`, `app-case-notes-section`
10. État terminal = **⚠ non explicite** (voir ci-dessous)

## État terminal du processus (explicite)

**Constat à signaler** : l'écran détail du dossier n'a pas d'état terminal articulé. Le dossier porte un statut `OPEN`/`CLOSED`, mais rien dans le parcours ne dit *quand* le traitement métier est « fini ». Avec F-98 (génération de conclusions) à venir, l'état terminal deviendra « conclusions générées + exportées ». **Ce trou est hérité, pas créé par F-243** — F-243 est une métadonnée, pas une étape de traitement. À traiter dans le cadrage écran de F-98.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours | Zone LegalCase | Statut |
|---|---|---|
| Ouverture / en-tête / stepper | `detail-header`, `app-case-dashboard-stepper` | ✅ existant |
| Métadonnées du dossier | `mat-card.detail-card` | ✅ existant |
| **Stade procédural** | **F-243 — à placer** | — |
| Stats d'usage | `mat-card.stats-card` | ✅ existant |
| Import pièces | `section#section-documents` | ✅ existant |
| Analyse | `app-analysis-pipeline` | ✅ existant |
| Synthèse / dashboard | `decisional-summary-panel`, `app-case-dashboard` | ✅ existant |
| Outils décisionnels | `app-decisional-tools-panel` | ✅ existant |
| Échéances / notes | `app-case-deadlines-section`, `app-case-notes-section` | ✅ existant |
| État terminal | — | ❌ non explicite (hérité) |

## Position candidate de la feature

Le dev SF-243-02 a inséré `<app-procedure-stage-section>` comme `mat-card` **après la `stats-card`**. Points d'entrée : visible directement à l'ouverture du dossier, édition via bouton « Modifier » de l'encart.

## Challenge placement

❌ **Ajustement requis.** Le stade procédural est une **métadonnée d'identification du dossier** — au même rang que le domaine juridique, qui vit dans la `detail-card`. Le poser après la `stats-card` (compteurs d'usage : documents, analyses, tokens) le déconnecte du groupe logique « identité du dossier » et l'intercale dans des données d'usage sans rapport. **Bon placement : regroupé avec les métadonnées du dossier** — intégré à la `detail-card`, ou en `mat-card` immédiatement après la `detail-card` et **avant** la `stats-card`.

## Challenge lisibilité de la séquence

✅ OK. Le stade procédural n'est pas une étape de traitement — c'est une donnée d'identité. Placé avec les métadonnées du dossier (`detail-card`), il se lit naturellement comme « caractéristique du dossier », sans rompre la séquence import → analyse → synthèse → outils que porte le `app-case-dashboard-stepper`.

## Challenge charge écran

⚠ **Ajustement requis.** L'écran `case-file-detail` porte déjà ~10 blocs primaires (en-tête, stepper, detail-card, stats-card, documents, docs-card, deadlines, notes, decisional-tools-panel, analysis-pipeline, decisional-summary-panel). Ajouter une `mat-card` autonome = +1 bloc primaire sur un écran déjà dense. **Préférer l'intégration du stade procédural à la `detail-card` existante** (champ supplémentaire à côté du domaine juridique) plutôt qu'une carte séparée — 0 bloc primaire ajouté.

## Challenge état final / continuité

✅ OK pour F-243. Le stade procédural est une métadonnée saisie tôt, sans output de traitement ; il sera consommé par F-98 (conclusions). Pas de dead-end ni de ping-pong. Le trou « état terminal du dossier » existe mais est hérité (cf. section État terminal).

## Ajustements IA requis

1. **Repositionner** l'encart stade procédural : le sortir d'« après la `stats-card` » pour le **regrouper avec les métadonnées du dossier** — idéalement intégré à la `detail-card` (à côté du domaine juridique), à défaut en carte immédiatement après la `detail-card` et avant la `stats-card`.
2. **Ne pas ajouter de bloc primaire autonome** : privilégier l'intégration à la `detail-card` pour ne pas alourdir un écran déjà à ~10 blocs.

## Invariants anti-surcharge pour la mini-spec

1. Le stade procédural est regroupé visuellement avec les métadonnées d'identité du dossier (domaine juridique), jamais isolé dans une zone d'usage/stats.
2. Mode affichage = forme compacte (une ligne de libellés), pas un bloc volumineux permanent.
3. Le mode édition (3 sélecteurs en cascade) ne s'ouvre qu'à la demande de l'avocat (bouton), il n'est pas affiché en permanence.
4. Aucun nouveau bloc primaire autonome ajouté à l'écran `case-file-detail` — intégration à un bloc existant.

## Décision finale

**GO avec ajustements.** Le dev SF-243-02 est repris pour appliquer l'ajustement 1 (repositionnement vers la `detail-card`) et l'ajustement 2 (pas de carte autonome). Le reste du dev (service, cascade, tests) est conforme et conservé.

## MAJ apportée au parcours écran de référence

Parcours écran « détail du dossier » reconstruit et versionné dans `docs/business/parcours-ecran-dossier.md` (créé à ce passage). État terminal identifié comme non explicite — à articuler lors du cadrage écran de F-98.

---

## Liens
- `ai-skills/screen-coherence-challenger.md` — skill appliquée
- `docs/features/F-243/SF-243-00-coherence.md` — cadrage fonctionnel (étape 0)
- `docs/features/F-243/SF-243-02-frontend-stade-procedural.md` — mini-spec frontend (à ajuster)
- `docs/business/parcours-ecran-dossier.md` — référentiel parcours écran
