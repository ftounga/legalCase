# Mini-spec — F-148 / SF-148-02 Frontend affichage Claude Vision

## Identifiant · `F-148 / SF-148-02`
## Date · `2026-04-23` · Branche · `feat/SF-148-02-frontend-vision-display`

## Objectif
Afficher dans la popup aperçu d'un document la description visuelle produite par Claude Vision (SF-148-01), avec un badge "Vision" signalant que l'enrichissement a été appliqué à la pièce. L'avocat voit explicitement ce que Claude a observé en plus du texte OCR.

## Contexte
SF-148-01 backend persiste `visual_description` sur `DocumentPiece` et l'expose via `DocumentPieceSummary`. SF-148-02 rend cette donnée visible à l'utilisateur.

## Comportement nominal

### A — Modèle frontend
`DocumentPieceSummary` (frontend) reçoit un champ optionnel `visualDescription?: string | null`.

### B — Badge indicatif
Dans la sidebar des pièces (`DocumentPreviewDialog`), chaque pièce enrichie affiche un petit badge discret `Vision` (mat-icon `visibility`) à droite du libellé. Tooltip : *"Description visuelle Claude Vision disponible"*.

### C — Panneau de description visuelle
Quand la pièce sélectionnée a une `visualDescription`, un encart discret est injecté dans l'onglet "Texte OCR" **au-dessus** du texte OCR extrait. Format :
- entête : icône `visibility` + titre `Description visuelle (Claude Vision)`
- corps : texte `visualDescription` (whitespace preserved)
- fond légèrement coloré (accent), séparateur avec le texte OCR en dessous

### D — Rétrocompat
Pièces sans `visualDescription` → aucun badge, aucun encart (comportement pré-F-148 identique).

## Critères d'acceptation
- [ ] `DocumentPieceSummary` TS exposé avec `visualDescription?: string | null`
- [ ] Badge `Vision` visible dans la sidebar pour les pièces enrichies
- [ ] Panneau "Description visuelle" injecté au-dessus du texte OCR dans l'onglet approprié
- [ ] Pièces non enrichies → UI inchangée
- [ ] Tests unitaires `DocumentPreviewDialog` : badge présent/absent, panneau présent/absent, contenu affiché
- [ ] Suite frontend verte, build vert

## Plan de test minimal
- U-01 : piece avec `visualDescription` → badge `Vision` visible
- U-02 : piece sans `visualDescription` → pas de badge
- U-03 : piece avec `visualDescription` et onglet Texte actif → encart description affiché
- U-04 : piece sans `visualDescription` → pas d'encart
- U-05 : alternance entre une pièce enrichie et non enrichie → badge/encart disparaît correctement

## Tables / endpoints / composants impactés
### Frontend
- `core/models/document.model.ts` : +`visualDescription` sur `DocumentPieceSummary`
- `case-files/document-preview-dialog/document-preview-dialog.component.ts|.html|.scss` : badge + panneau
- Tests associés

### Pas impacté
- Backend (SF-148-01)
- Autres composants du frontend

## Impact par domaine métier (FR + BE × 3 domaines)
| Domaine | Impact | Adaptation |
|---|---|---|
| **Droit du travail** | Affichage pour SMS/ATTESTATION/PHOTO enrichis | Aucune — UI agnostique |
| **Immigration** | Affichage pour PHOTO/PIECE_IDENTITE/TITRE_DE_SEJOUR/PASSEPORT/VISA/RECEPISSE_PREFECTURE enrichis | Aucune |
| **Famille** | Affichage pour PHOTO/SMS/ATTESTATION/ACTE_MARIAGE enrichis | Aucune |

Le composant d'affichage est purement un rendu UI neutre consommant le champ backend. Les règles métier qui déterminent **si** une pièce est enrichie restent côté backend (SF-148-01).

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|---|---|---|
| DocumentPreviewDialog (SF-145-02) | Extension minimale : +badge + encart texte | Intégré |
| Sidebar pièces | Ajoute un slot badge conditionnel à droite du label | Intégré |
| `<app-source-ref>` (SF-146-02) | Non concerné : il travaille sur `AnalysisItem`, pas sur `DocumentPieceSummary` directement | Non applicable |
| Exports DOCX/PDF (SF-146-03) | `visualDescription` n'est pas exporté — hors scope V1 (voir "Hors scope") | Non applicable |

## Préoccupations transversales
- **Auth / Principal** : aucun impact
- **Workspace context** : aucun impact
- **Plans / limites** : aucun impact (feature flag SF-148-01 contrôle l'activation)
- **Navigation / routing** : aucun impact

## Hors scope
- Export DOCX/PDF de la description visuelle — à évaluer si feedback terrain en demande
- Édition manuelle de la description (correction humaine) — pas dans cette SF
- Indicateur coût IA / compteur pages enrichies — à évaluer avec F-16 si volumétrie dérive
- Ré-enrichissement à la demande via un bouton UI — pas dans cette SF
