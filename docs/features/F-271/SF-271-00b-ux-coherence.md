# Étape 0 bis — Cohérence écran — F-278 (confirmation de régénération, couplée F-271)

> Skill : `ai-skills/screen-coherence-challenger.md`. 2026-06-12. Élément visible nouveau = boîte de confirmation avant `generate()` quand une version DONE existe déjà.

## 1. Parcours écran réel

Onglet « Décision » du dossier → section conclusions (`conclusions-section.component`). États : aucune version → bouton « Générer » ; version DONE → acte rendu, sélecteur de versions, badges cycle de vie, boutons « Régénérer » / « Modifier » / export. La régénération est déclenchée par le bouton « Régénérer » (`generate()`).

## 2. Écrans / zones existants sur ce parcours

- `ConfirmDialogComponent` (modale Material) **déjà utilisé** dans cette même section pour la garde d'export (placeholders à compléter) — pattern et style identiques.
- Aucune confirmation n'existe aujourd'hui sur « Régénérer » → clic = perte immédiate (avant F-271) / nouvelle version silencieuse.

## 3. Challenge placement / lisibilité / charge / continuité

- **Placement** : modale au clic « Régénérer », **uniquement** si une version DONE existe (première génération = aucun dialogue, flux direct). Pas d'ajout d'élément permanent à l'écran → **zéro surcharge visuelle**.
- **Lisibilité de la séquence** : clic → modale (Annuler / Régénérer) → polling habituel. Cohérent avec la garde d'export déjà présente (même composant) → l'avocat reconnaît le pattern.
- **Charge** : nulle hors interaction (la modale n'apparaît qu'au clic).
- **Continuité / état final** : après confirmation, comportement de polling/affichage inchangé ; après Annuler, retour à l'état courant sans effet de bord.

## 4. Invariants anti-surcharge

1. Confirmation **conditionnelle** (seulement si version DONE pré-existante) — pas de friction à la première génération.
2. **Réutiliser** `ConfirmDialogComponent`, pas de nouveau composant ni nouvelle dépendance.
3. Message **informatif** aligné F-271 (reprise des éditions), pas alarmiste.
4. Bouton de confirmation couleur `primary` (action normale, non destructive depuis F-271), label « Régénérer ».

## 5. Verdict

**GO** — élément visible minimal, conditionnel, réutilise un pattern déjà présent sur le même écran. Référentiel `docs/business/parcours-ecran-dossier.md` : ajout d'une confirmation informative au clic « Régénérer » quand une version existe déjà (reprise récapitulative des éditions).
