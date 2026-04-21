# Mini-spec — F-145 / SF-145-02 Chips pièces + popup aperçu navigable (frontend)

## Identifiant · `F-145 / SF-145-02`
## Date · `2026-04-22` · Branche · `feat/SF-145-02-document-pieces-frontend`

## Objectif
Rendre visibles et explorables les pièces détectées par SF-145-01 côté UX.

## Comportement nominal

### A — Chips pièces sous le nom du doc (table des docs)
Dans la colonne **Nom** de la table `docs-table`, sous le nom du fichier, afficher la liste compacte des pièces (`DocumentPieceSummary`) si `doc.pieces.length > 1` (docs unitaires = 1 seule pièce → pas de bruit visuel). Format :
- Petits chips cliquables avec **icône par type** (contrat = description, CNI = badge, SMS = chat, email = mail, attestation = edit_note, etc.)
- Tooltip : `{label}` au survol
- Click : ouvre la popup preview avec la pièce pré-sélectionnée

### B — Popup aperçu refondue en explorateur navigable
La popup `document-preview-dialog` actuelle montre 2 onglets : "Texte extrait" et "Aperçu PDF". Elle est **refondue en layout 2 colonnes** :
- **Sidebar gauche** (240px) : liste verticale des pièces
  - Pièce sélectionnée mise en évidence (bordure navy gauche, fond navy 6%)
  - Icône + type + label + plage de pages `p. X-Y`
  - Collapsible automatiquement si 1 seule pièce (UX identique à avant)
- **Zone droite** (flex) : conserve les 2 onglets "Texte extrait" / "Aperçu PDF"
  - Bandeau en haut : `{icône type} {label} · p. {pageStart}–{pageEnd}` de la pièce sélectionnée
  - Texte extrait : le **texte complet** du document reste affiché (pas de découpage précis par pièce — cf. hors scope), mais un bandeau info signale "Texte complet du document (segmentation fine par pièce à venir)"
  - Aperçu PDF : rend la **page `pageStart` de la pièce sélectionnée** (au lieu de toujours la page 1). Changer de pièce dans la sidebar re-rend l'aperçu

### C — État par défaut
- Première pièce sélectionnée à l'ouverture
- Si ouverture via click sur un chip dans la table → la pièce correspondante est pré-sélectionnée (data passée via `DocumentPreviewDialogData.initialPieceId`)

## Critères d'acceptation
- [ ] Modèle TS `Document` enrichi de `pieces?: DocumentPieceSummary[]` + interface `DocumentPieceSummary`
- [ ] Modèle TS `DocumentPreview` inchangé (on passe les pièces via le parent, pas via le preview endpoint)
- [ ] Dans `case-file-detail.component.html`, sous le nom du doc : `@if (doc.pieces && doc.pieces.length > 1)` → liste de chips cliquables
- [ ] Chaque chip appelle `openPreview(doc, piece.id)` qui ouvre le dialog avec `initialPieceId`
- [ ] `DocumentPreviewDialogData` inclut `pieces: DocumentPieceSummary[]` + `initialPieceId?: string`
- [ ] Layout popup refondu : sidebar à gauche, contenu à droite
- [ ] Sidebar listant toutes les pièces avec type + label + pages
- [ ] Signal `selectedPieceId` qui alimente le bandeau et le rendu PDF
- [ ] `renderPdfFirstPage()` → renommé `renderPdfPage(pageIndex)` et appelé avec `selectedPiece.pageStart - 1`
- [ ] Sidebar auto-collapse si `pieces.length <= 1` (comportement actuel préservé)
- [ ] Tests frontend U-01 : avec 3 pièces → 3 chips rendus dans la table, 3 entrées dans la sidebar, piece 1 sélectionnée par défaut
- [ ] Tests frontend U-02 : click chip pièce 2 → dialog ouvert, sidebar marque pièce 2 active
- [ ] Tests frontend U-03 : si 1 seule pièce → pas de chips dans la table, pas de sidebar

## Tables / endpoints / composants impactés
### Backend
- Aucun — tout est côté frontend, la donnée vient déjà de SF-145-01 via `DocumentResponse.pieces`

### Frontend
- `core/models/document.model.ts` : +`pieces`, +`DocumentPieceSummary` interface, +`DocumentPieceType` union type
- `case-file-detail/case-file-detail.component.html` : +chips pièces sous le nom
- `case-file-detail/case-file-detail.component.scss` : +styles chips par type (9 couleurs subtiles navy/gold dérivées)
- `case-file-detail/case-file-detail.component.ts` : +`openPreview(doc, pieceId?)` handler (ou update existant)
- `document-preview-dialog/document-preview-dialog.component.ts` : +sidebar, +`selectedPieceId`, +`renderPdfPage(index)`
- `document-preview-dialog/document-preview-dialog.component.html` : layout 2 colonnes
- `document-preview-dialog/document-preview-dialog.component.scss` : styles sidebar
- Specs respectifs

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|-------|-----------|------------|
| F-127 (document preview existant) | On refond sans casser le contract backend (le endpoint preview reste identique) | Intégré |
| F-121 badge "Non analysable" + F-144 chip `OCR` | Cohabitent avec les nouveaux chips pièces — empilage vertical sous le nom | Intégré |
| Design System | Icônes Material (type mapping), palette navy/gold, typo Inter, espacements 4px | Intégré |
| F-146 source précise | Les pièces posées par cette SF seront consommées par F-146 pour citer `doc · pièce · page` | SF parallèle future |

### Nouveau composant partagé
- Aucun — le mapping type→icône et type→couleur est scoped au contexte docs. Si F-146 en a besoin pour afficher la source précise, on externalisera dans un helper `documentPieceDisplay.ts` à ce moment-là (pas d'over-engineering).

## Préoccupations transversales
- **Auth / Principal** : aucun impact
- **Workspace context** : aucun impact (pieces héritent du document déjà isolé)
- **Plans / limites** : aucun impact
- **Navigation / routing** : aucun impact (popup overlay, pas de route)

## Hors scope
- **Découpage fin du texte extrait par pièce** : le backend expose aujourd'hui le texte complet. Segmenter en "texte de la pièce 1 / texte de la pièce 2" demande soit un stockage dédié par pièce, soit un parsing heuristique basé sur pages. Non traité ici — on affiche le texte complet avec un bandeau d'information "segmentation fine à venir". **Sera traité soit dans F-146, soit dans une SF dédiée SF-145-03 si le retour terrain le justifie.**
- **Édition manuelle des pièces** par l'avocat (correction si l'IA s'est trompée) — backlog futur
- **Backfill** des docs antérieurs à F-145 : les anciens docs n'ont pas de pièces détectées (`pieces: []`). Dans ce cas la popup fonctionne comme avant (pas de sidebar)
