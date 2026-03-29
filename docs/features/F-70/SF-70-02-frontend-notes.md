# SF-70-02 — Frontend notes internes sur un dossier

## Objectif
Afficher et gérer les notes internes d'un dossier depuis la page case-file-detail.

## Comportement nominal
- Section "Notes internes" en bas de la page dossier
- Liste notes ordre décroissant : auteur, date, contenu
- Champ textarea + bouton "Ajouter" pour créer une note
- Icônes éditer/supprimer uniquement sur les notes du user courant
- Inline editing : clic éditer → textarea pré-rempli, bouton Sauvegarder/Annuler
- Snackbar succès/erreur sur chaque action

## Cas d'erreur
| Cas | Comportement |
|-----|-------------|
| Contenu vide à la création | Bouton désactivé |
| Erreur HTTP | Snackbar erreur |

## Critères d'acceptation
- [ ] Notes chargées au montage du composant
- [ ] Création → note apparaît en tête de liste
- [ ] Édition inline → contenu mis à jour
- [ ] Suppression → note disparaît de la liste
- [ ] Notes des autres users : pas de boutons éditer/supprimer
- [ ] Conformité design system

## Plan de test
- U-01 : `loadNotes()` → service appelé, signal `notes` peuplé
- U-02 : `addNote()` contenu vide → service non appelé
- U-03 : `addNote()` valide → note ajoutée, champ réinitialisé
- U-04 : `deleteNote()` → note retirée du signal
- U-05 : erreur HTTP → snackbar affiché

## Composants impactés
| Fichier | Action |
|---------|--------|
| `core/services/case-note.service.ts` | Nouveau |
| `core/services/case-note.service.spec.ts` | Nouveau |
| `core/models/case-note.model.ts` | Nouveau |
| `case-files/case-notes-section/case-notes-section.component.ts` | Nouveau |
| `case-files/case-notes-section/case-notes-section.component.html` | Nouveau |
| `case-files/case-notes-section/case-notes-section.component.scss` | Nouveau |
| `case-files/case-notes-section/case-notes-section.component.spec.ts` | Nouveau |
| `case-files/case-file-detail/case-file-detail.component.html` | Modifié (ajout section) |
| `case-files/case-file-detail/case-file-detail.component.ts` | Modifié (import) |

## Hors périmètre
- Notes visibles dans le share public client
- Mentions @utilisateur
