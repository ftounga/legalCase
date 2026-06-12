# SF-279-00b — Cadrage de cohérence écran (F-279)

**Feature** : F-279 — Feedback de sauvegarde explicite + autosave brouillon
**Skill** : `ai-skills/screen-coherence-challenger.md`
**Date** : 2026-06-12
**Écran cible** : `conclusions-section` (onglet Décision du détail dossier), **mode édition**.

## 1. Parcours écran réel (de l'ouverture à l'état terminal)

Détail dossier → onglet **Décision** → carte **« Projet de conclusions »** → état `DONE` →
bouton **Modifier** → **mode édition** (toggle Édition/Aperçu, co-rédaction IA, barre d'outils
markdown, textarea, aperçu live) → actions **Enregistrer** / **Annuler** → retour lecture.

## 2. Écrans / zones existants sur ce parcours

- En-tête de carte : titre + sélecteur de version.
- Zone d'édition (`cs-edit-split`) : colonne éditeur (co-rédaction + toolbar + textarea) +
  colonne aperçu.
- Barre d'actions d'édition (`cs-actions`) : **Enregistrer** (primary), **Annuler** (stroked).

## 3. Challenge écran

### Placement
- **Indicateur d'état de sauvegarde** : placé **dans la barre d'actions d'édition**, à côté
  des boutons Enregistrer/Annuler — c'est l'endroit où l'œil cherche la confirmation après
  un clic « Enregistrer ». Petit chip discret (`✓ Enregistré` / `Modifié`), pas un bandeau
  pleine largeur (anti-surcharge).
- **Bandeau de restauration de brouillon** : affiché **en haut de la zone d'édition**, à
  l'entrée en édition, **uniquement** si un brouillon local plus récent que le contenu serveur
  existe. Réutilise le style d'alerte douce déjà présent (`cs-placeholder-alert` / `role="status"`),
  deux actions **Restaurer** / **Ignorer**. Disparaît dès le choix.

### Lisibilité de la séquence
- Le chip d'état complète, ne remplace pas, la snackbar ponctuelle de succès (ajoutée par F-279
  pour la confirmation immédiate « Modifications enregistrées »). Pas de double message
  redondant simultané : la snackbar est fugace (3 s), le chip est l'état **persistant**.

### Charge de l'écran cible
- +1 chip (quelques px) dans une barre d'actions déjà présente.
- +1 bandeau conditionnel, **rare** (uniquement après crash/réouverture avec brouillon en attente).
- Aucune colonne ni section nouvelle. Charge visuelle quasi nulle en régime normal.

### État final / continuité
- Après « Enregistrer » : snackbar succès + chip `✓ Enregistré` + retour lecture (continuité
  inchangée). Brouillon local purgé.
- Après « Annuler » : brouillon local purgé, retour lecture.
- Après restauration : le textarea/aperçu reflètent le brouillon restauré, chip `Modifié`.

## 4. Invariants anti-surcharge

1. Indicateur = **chip discret** dans la barre d'actions existante, pas un bloc dédié.
2. Bandeau de restauration **conditionnel et éphémère** (réutilise un style d'alerte existant).
3. **Pas** de barre de progression d'autosave permanente ni d'horodatage verbeux clignotant.
4. Aucun nouvel onglet, aucune nouvelle carte, aucun déplacement d'élément existant.

## 5. Verdict

**GO avec ajustements** — impact écran faible et localisé (chip + bandeau conditionnel) dans
la zone d'édition existante, réutilise les styles d'alerte en place. Respecte la séquence et
n'alourdit pas l'écran en régime normal.

## 6. Enrichissement référentiel

`docs/business/parcours-ecran-conclusions.md` (si présent) : ajouter au mode édition la zone
« état de sauvegarde » (chip) et le bandeau « brouillon récupéré » conditionnel.
