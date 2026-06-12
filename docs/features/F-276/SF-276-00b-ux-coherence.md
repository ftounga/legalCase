# SF-276-00b — Cadrage cohérence écran (F-276 : Sommaire / navigation de l'acte)

> Skill : `ai-skills/screen-coherence-challenger.md`. Feature à impact écran (ajout d'un élément visible).

## 1. Parcours écran réel de l'avocat

`/case-files/:id/conclusions` (page dédiée F-267) :
en-tête de page (retour dossier + fil d'Ariane + titre) → corps centré « feuille » →
`<app-conclusions-section>` qui, en statut `DONE`, affiche :
- **mode lecture** : la « feuille » `<app-conclusion-document>` (titres, paragraphes) + en-tête cabinet
  (opt-in) + alerte placeholders + barre d'actions (Modifier / Copier / Word / PDF / Régénérer) ;
- **mode édition** (F-264) : 2 colonnes — éditeur markdown (gauche) / aperçu `<app-conclusion-document>`
  (droite) — + actions Enregistrer / Annuler.

L'acte fait 5–15 pages → l'avocat scrolle pour atteindre une section.

## 2. Écrans / zones existants sur ce parcours

| Zone | Composant | Rôle |
|------|-----------|------|
| Feuille de l'acte (lecture + aperçu d'édition) | `conclusion-document` | rendu markdown→HTML |
| Sélecteur de section (édition, co-rédaction) | `conclusions-section` (F-265) | choisir la section à régénérer |
| Barre d'actions | `conclusions-section` | copier/exporter/régénérer/éditer |

La **feuille `conclusion-document` est le seul élément commun aux deux modes** → c'est le bon hôte du sommaire
(portée uniforme exigée par F-276).

## 3. Challenge écran

### Placement
- **Option A (retenue)** : sommaire **en tête de la feuille**, à l'intérieur de `conclusion-document`, sous
  forme de bloc « Sommaire » **repliable** (disclosure `<details>`-like). Centré comme la feuille, n'élargit
  pas la page, responsive natif (empilement vertical), apparaît identiquement en lecture et en aperçu d'édition.
- Option B (panneau latéral sticky) : rejetée — fragile en responsive (la feuille est déjà à largeur de
  lecture centrée ; un panneau latéral casse le centrage et exige une colonne supplémentaire en mode édition
  déjà à 2 colonnes). Plus de lift, plus de risque, pour un bénéfice marginal. **Différé / non retenu.**

### Lisibilité de la séquence
Le sommaire en tête de feuille suit l'ordre de lecture naturel (« table des matières avant le corps »).
Familier (mêmes codes qu'un document Word avec sommaire).

### Charge de l'écran cible
- Replié par défaut **n'est pas** retenu : un sommaire utile doit être visible d'emblée. **Décision** : déplié
  par défaut **mais affiché uniquement si ≥ 2 sections** (sinon il n'apporte rien et alourdit). Liste compacte,
  une ligne par section, indentée pour les `###` — empreinte visuelle faible.

### État final / continuité
- Le clic scrolle la feuille jusqu'au titre (`scrollIntoView` smooth/start). Pas de changement d'URL, pas de
  rechargement, aucun effet sur l'export ou les versions. État terminal inchangé.

## 4. Verdict

**GO avec ajustements** :
- placement = bloc Sommaire en tête de feuille (Option A), pas de panneau latéral ;
- affiché seulement si ≥ 2 sections ;
- liste compacte, indentation `##` vs `###`, saut `scrollIntoView` smooth/start.

## 5. Invariants anti-surcharge

1. Sommaire masqué si < 2 sections.
2. Hauteur bornée / liste compacte (pas de second corps de texte).
3. N'élargit pas la feuille (reste dans la largeur de lecture), pas de panneau latéral.
4. Aucun nouveau bouton dans la barre d'actions déjà chargée.
5. Identique en lecture et en aperçu d'édition (un seul hôte : `conclusion-document`).

## 6. Référentiel parcours-écran

À compléter post-merge dans `docs/business/parcours-ecran-*.md` (groupé par l'orchestrateur — pas de commit
docs par feature).
