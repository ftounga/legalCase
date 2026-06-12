# SF-280-00b — Cadrage cohérence écran (F-280 : diff de versions)

> Skill `ai-skills/screen-coherence-challenger.md` — étape 0 bis (feature à impact écran).

## 1. Parcours écran réel

Écran cible : `conclusions-section` (carte « Projet de conclusions ») dans la page conclusions du dossier. État terminal du traitement : génération `DONE` → l'avocat lit / édite / compare / valide / exporte.

Séquence actuelle de la carte (état `DONE`, lecture seule) :
1. Header : titre + **sélecteur de version** (mat-select).
2. Corps : rendu « acte » du content de la version sélectionnée (`ConclusionDocumentComponent`).
3. En-tête cabinet (opt-in export), alerte placeholders.
4. Barre d'actions : Modifier / Copier / Word / PDF / Régénérer.

## 2. Cartographie des zones existantes

| Zone | Rôle | Charge |
|------|------|--------|
| Header sélecteur version | choisir LA version affichée | faible |
| Corps acte | lire le content | dense (texte long) |
| Barre d'actions | agir sur la version | moyenne |

Le diff a besoin : (a) d'un déclencheur, (b) d'une zone d'affichage du résultat. Il ne doit pas alourdir la lecture par défaut.

## 3. Challenge placement / lisibilité / charge / état final

- **Placement du déclencheur** : un bouton « Comparer les versions » dans la **barre d'actions** (état `DONE`, lecture), à côté de Régénérer. Cohérent : il agit sur les versions, comme le sélecteur. Conditionné à `versions().length >= 2` (sinon masqué → pas de bouton mort).
- **Zone d'affichage** : le diff s'affiche **en mode plein-carte** (remplace le rendu acte pendant la comparaison), avec un sélecteur des 2 bornes (« Comparer Vx → Vy ») + un bouton « Fermer » qui rend le rendu acte. Évite la surcharge : on ne montre jamais acte + diff simultanément.
- **Lisibilité de la séquence** : entrer en mode diff est explicite (bouton), sortir aussi (Fermer). Le sélecteur de version du header est **désactivé** pendant le diff (le diff a ses propres bornes), évitant une double commande contradictoire.
- **Charge écran** : neutre — le diff réutilise l'espace du corps. Deux bornes (selects) + une liste de lignes colorées. Légende compacte (vert = ajouté, rouge = supprimé).
- **État final / continuité** : Fermer ramène exactement à l'état lecture précédent (version sélectionnée inchangée). Aucune action du diff ne mute l'état.

## 4. Invariants anti-surcharge

1. Le diff **remplace** le rendu acte (jamais en plus) — un seul contenu dense à la fois.
2. Le déclencheur n'apparaît que si ≥ 2 versions ET état `DONE` lecture (pas en édition, pas pendant génération).
3. Légende couleur sobre, tokens DS uniquement.
4. Sortie = retour à l'identique de l'état lecture (aucune perte de contexte).
5. Pas d'ouverture automatique : mode opt-in.

## 5. Verdict

**GO.** Le diff s'insère sans surcharge (mode plein-carte exclusif du rendu acte), déclencheur logique dans la barre d'actions, conditionné, réversible. Référentiel `docs/business/parcours-ecran-dossier.md` à enrichir (post-merge, groupé orchestrateur).
