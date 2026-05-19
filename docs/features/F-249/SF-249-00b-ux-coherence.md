# F-249 — Cadrage cohérence écran (étape 0 bis)

**Date** : 2026-05-19
**Skill appliquée** : `ai-skills/screen-coherence-challenger.md`
**Feature** : F-249 — Refonte futuriste du tableau de bord d'accueil

---

## Verdict : GO avec ajustements

Le placement est trivialement correct (refonte in-place de l'écran d'accueil). **Un ajustement structurant** : le hero d'accueil doit *absorber* la barre KPI plutôt que s'y ajouter, pour garder la charge d'écran constante.

## Intention métier + comportement visible attendu

Refonte visuelle du tableau de bord d'accueil `/dashboard`. Visible : un **hero d'accueil** en tête (accueil personnalisé + date + headline « ce qui demande votre attention » + compteurs KPI intégrés), puis les 4 sections existantes (délais urgents, alertes checklist, dossiers ouverts, activité récente) retravaillées avec une couche visuelle « futuriste maîtrisée » (dégradés navy subtils, glassmorphism léger, profondeur, transitions).

## Rappel verdict feature-coherence-challenger (étape 0)

**GO** (`SF-249-00-coherence.md`) — refonte d'écran existant, aucun trou fonctionnel amont ni aval.

## Parcours écran réel de l'avocat

Source : route `/dashboard` + `dashboard.component` réellement codés + `ShellComponent` + pratique avocat (⚠ hypothèse à valider).

1. L'avocat se connecte → redirection automatique vers `/dashboard` (route par défaut, `app.routes.ts`).
2. L'écran s'affiche dans le `ShellComponent` (header sticky + side nav 240px + zone contenu).
3. **Zone 1 — barre KPI** : 4 cartes compteurs (dossiers actifs, délais urgents, alertes checklist, analyses récentes).
4. **Zone 2 — colonne gauche** : section « Délais urgents », section « Alertes checklist ».
5. **Zone 3 — colonne droite** : section « Dossiers ouverts », section « Activité récente ».
6. L'avocat scanne ces zones — aujourd'hui **~5 blocs primaires** (1 barre KPI + 4 sections).
7. Il clique un délai / une alerte / un dossier / une analyse → navigation vers `case-file-detail`.
8. Il quitte le dashboard ; il y revient via la nav latérale (rubrique DOSSIERS → Tableau de bord), plusieurs fois par jour.

## État terminal du processus (explicite)

Un tableau de bord d'accueil **n'a pas d'état terminal** — c'est un hub d'orientation récurrent, pas un traitement qui se clôt. L'état terminal pertinent est celui d'une **visite** : la visite est réussie quand l'avocat est *orienté en quelques secondes* — il a cliqué vers le dossier prioritaire, ou constaté l'absence d'urgence. La refonte F-249 doit donc optimiser la **vitesse d'orientation**, pas seulement l'esthétique.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours | Zone LegalCase | Statut |
|---|---|---|
| Connexion → redirection | `authGuard` + route `/dashboard` | ✅ existant |
| Shell (header / nav / contenu) | `ShellComponent` | ✅ existant |
| Barre KPI (4 compteurs) | `.kpi-bar` (4 × `.kpi-card`) | ✅ existant — à absorber dans le hero |
| **Hero d'accueil** | — | ❌ à créer (F-249) |
| Délais urgents | `section#deadlines` | ✅ existant — à restyler |
| Alertes checklist | `section#checks` | ✅ existant — à restyler |
| Dossiers ouverts | `section#cases` | ✅ existant — à restyler |
| Activité récente | `section#activity` | ✅ existant — à restyler |
| Destination au clic | `case-file-detail` | ✅ existant |

## Position candidate de la feature

Le hero d'accueil en **tête de `/dashboard`**, à la place de la barre KPI actuelle. Le reste de l'écran = les 4 mêmes sections, restylées. Aucune nouvelle route, aucun nouvel écran. Point d'entrée inchangé : redirection post-login + nav latérale.

## Challenge placement

✅ **Correct.** C'est l'écran d'accueil lui-même qu'on refond — on ne déplace rien. Un hero en tête d'une page d'accueil est le placement canonique d'un point focal. L'avocat y est déjà à l'étape 2 du parcours.

## Challenge lisibilité de la séquence

⚠ **Ajustement requis.** Le dashboard n'est pas un workflow séquentiel (étape 1 → 2 → 3) mais un **scan par priorité**. La « séquence » à rendre lisible est l'**ordre de criticité** : l'œil doit tomber d'abord sur le vital (délais urgents = risque de forclusion), puis sur le reste. La couche futuriste ne doit pas aplatir cette hiérarchie en traitant toutes les zones avec la même intensité visuelle. Le hero doit pointer vers l'urgence n°1.

## Challenge charge écran

⚠ **Ajustement requis.** L'écran porte aujourd'hui **~5 blocs primaires** (barre KPI + 4 sections). Ajouter un hero comme **6ᵉ bloc empilé** alourdirait un écran déjà dense et repousserait les délais urgents sous la ligne de flottaison. Le hero doit **absorber la barre KPI** : accueil + date + headline + les 4 compteurs intégrés dans une seule zone hero. Bilan : on reste à **~5 blocs primaires** (hero + 4 sections), charge constante.

## Challenge état final / continuité

✅ **OK.** Chaque zone du dashboard mène à un dossier — continuité claire, pas de dead-end. Seul invariant : les nouveaux éléments (hero, headline, indicateur de tendance) doivent eux aussi être cliquables vers leur zone, sinon ils deviennent des zones mortes décoratives (déjà couvert par l'invariant anti-gadget de l'étape 0).

## Ajustements IA requis

1. Le hero **absorbe** la barre KPI au lieu de s'y ajouter — pas de 6ᵉ bloc primaire.
2. La hiérarchie de criticité (délais urgents = information la plus saillante) est préservée : la couche futuriste module son intensité selon la criticité, elle ne l'uniformise pas.
3. Tout élément du hero (headline, KPI, tendance) est cliquable et défile/navigue vers sa zone — aucune zone morte.
4. Les délais urgents restent visibles **sans scroll** sur un écran ≥ 768px (résolution minimale supportée, DESIGN_SYSTEM §8).

## Invariants anti-surcharge pour la mini-spec

1. Nombre de blocs primaires de `/dashboard` **constant** (~5) — le hero remplace la barre KPI, il ne s'y ajoute pas.
2. L'information critique (délais urgents) reste **au-dessus de la ligne de flottaison** à 768px.
3. **Aucune nouvelle route, aucun nouvel écran** — refonte in-place de `/dashboard`.
4. La couche futuriste est une **couche de rendu** (profondeur, dégradés, transitions), pas une couche de densité : elle n'ajoute pas de contenu UI à scanner.

## Décision finale

**GO avec ajustements.** Le placement est trivialement correct (écran d'accueil refondu in-place). Ajustement structurant : le hero absorbe la barre KPI pour garder la charge d'écran constante. Les 4 ajustements IA sont à intégrer dans la mini-spec frontend.

## MAJ apportée au parcours écran de référence

Section « Parcours écran — Tableau de bord d'accueil » ajoutée à `docs/business/parcours-ecran-cabinet.md`, avec articulation explicite de l'état terminal (orientation réussie d'une visite). Ligne ajoutée à l'historique des passages.

---

## Liens

- `ai-skills/screen-coherence-challenger.md` — skill appliquée
- `docs/features/F-249/SF-249-00-coherence.md` — cadrage cohérence fonctionnelle (étape 0)
- `docs/business/parcours-ecran-cabinet.md` — référentiel parcours écran
- `docs/DESIGN_SYSTEM.md` — couche futuriste dashboard (style — en aval de cette skill)
