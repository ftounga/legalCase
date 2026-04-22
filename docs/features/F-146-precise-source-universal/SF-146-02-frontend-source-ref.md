# Mini-spec — F-146 / SF-146-02 Frontend `<source-ref>` + intégration synthèse

## Identifiant · `F-146 / SF-146-02`
## Date · `2026-04-20` · Branche · `feat/SF-146-02-frontend-source-ref`

## Objectif
Afficher les références de source précises (document + pièce + pages) produites par SF-146-01 dans les sections de synthèse (Faits, Points juridiques, Risques, Chronologie), avec un badge cliquable qui ouvre le `DocumentPreviewDialog` directement sur la bonne pièce/page.

## Contexte
SF-146-01 a enrichi le pipeline IA : chaque `AnalysisItem` du backend expose désormais un champ optionnel `sourceRef = { documentName, pieceType, pieceLabel, pageStart, pageEnd }`. Aujourd'hui, le template synthèse n'affiche qu'`item.source` (string), ce qui masque cette nouvelle richesse.

## Comportement nominal

### A — Modèle front enrichi
`AnalysisItem` (TS) reçoit un champ optionnel `sourceRef?: SourceRef | null` aligné sur le DTO backend.

### B — Composant partagé `<app-source-ref>`
Nouveau composant standalone `frontend/src/app/shared/source-ref/`. Inputs : `sourceRef`, `legacySource`, `extrait`, `caseFileId`. Comportement :
- Si `sourceRef` présent → affiche `<pieceTypeIcon> <pieceTypeLabel> « pieceLabel » · doc.pdf · p. X[-Y]` cliquable.
- Sinon fallback legacy → `<icon description> source.pdf` non cliquable (comportement pré-F-146 conservé).
- Au clic : ouvre `DocumentPreviewDialog` avec `initialPieceId` résolu en matchant `(pieceType, pieceLabel, pageStart)` dans la liste des pièces du document ciblé.

### C — Résolution `documentName` → `documentId`
Le `sourceRef` ne transporte que le nom de fichier. Le composant appelle `DocumentService.list(caseFileId)` (cache local via signal partagé dans la page synthèse) et retrouve l'ID par matching exact sur `originalFilename`. Si aucun document trouvé → affichage textuel uniquement (non cliquable), sans erreur.

### D — Intégration dans `synthesis.component.html`
Remplacer les blocs `<span class="source-badge">` dans :
- Faits, Points juridiques, Risques

Par `<app-source-ref [sourceRef]="item.sourceRef" [legacySource]="item.source" [extrait]="item.extrait" [caseFileId]="caseFile()!.id" />`.

Les **questionsOuvertes** restent des `string[]` (pas d'`AnalysisItem`) — hors scope.

La **chronologie** (`TimelineEntry = { date, evenement }`) n'a pas de `sourceRef` aujourd'hui — hors scope (attendre extension backend du timeline DTO dans une SF future).

### E — Rétrocompat
- Analyses legacy (sans `sourceRef`) → affichage fallback identique à avant (icône `description` + nom du doc + extrait en italique).
- Dossiers sans pièces détectées (pré-F-145) → `sourceRef` null → comportement legacy.

## Critères d'acceptation
- [ ] `SourceRef` exposé dans `case-analysis.model.ts`
- [ ] `AnalysisItem.sourceRef?` ajouté
- [ ] Composant `<app-source-ref>` créé sous `frontend/src/app/shared/source-ref/`
- [ ] Composant intégré dans les sections Faits / Points juridiques / Risques
- [ ] Clic ouvre `DocumentPreviewDialog` pré-positionné sur la bonne pièce
- [ ] Fallback legacy OK (affichage string simple sans clic)
- [ ] Tests unitaires composant : sourceRef complet, legacy fallback, sourceRef partiel, clic ouvre dialog
- [ ] Pas de régression visuelle sur la synthèse (icônes, alignement, couleurs DESIGN_SYSTEM)
- [ ] Full build frontend vert, tous les tests existants passent

## Plan de test minimal
- U-01 : rend `"CONTRAT « Contrat Dupont » · dossier.pdf · p. 1-2"` quand sourceRef complet
- U-02 : rend `"dossier.pdf"` quand sourceRef absent, `legacySource = "dossier.pdf"`
- U-03 : rend sans label si pieceLabel vide
- U-04 : rend `"p. 3"` au lieu de `"p. 3-3"` quand pageStart === pageEnd
- U-05 : clic → `MatDialog.open` appelé avec `DocumentPreviewDialogComponent` + data correcte
- U-06 : clic sans match documentName → pas d'erreur, pas d'ouverture de dialog (no-op silencieux)

## Tables / endpoints / composants impactés
### Frontend
- `core/models/case-analysis.model.ts` : +`SourceRef`, +`sourceRef?` sur `AnalysisItem`
- `shared/source-ref/source-ref.component.ts|.html|.scss|.spec.ts` (nouveau)
- `case-files/synthesis/synthesis.component.ts|.html` : intégration + chargement documents
- Aucun nouvel endpoint

### Pas impacté
- Backend (déjà livré dans SF-146-01)
- DocumentPreviewDialog (utilisé tel quel)
- Outils décisionnels (F-DT/F-IM/F-FA) → SF-146-03

## Impact par domaine métier (FR + BE × 3 domaines)
| Domaine | Impact | Adaptation |
|---|---|---|
| **Droit du travail** (FR + BE) | Les faits/points/risques affichent doc·pièce·page. Clic = navigation directe à la preuve. Gain immédiat sur les dossiers composites prud'homal. | Aucune — le composant est neutre |
| **Immigration** (FR + BE) | Idem pour les dossiers OQTF / recours / renouvellement titre. Pièces référencées : TITRE_DE_SEJOUR, RECEPISSE_PREFECTURE, DECISION_OQTF, etc. | Aucune — les types de pièces sont déjà supportés par `documentPieceTypeLabel/Icon` |
| **Famille** (FR + BE) | Idem pour divorce / garde / liquidation. Pièces : JUGEMENT_DIVORCE, ACTE_MARIAGE, LIVRET_FAMILLE, JUSTIFICATIF_REVENUS. | Aucune |

Le composant `<app-source-ref>` est entièrement agnostique au domaine (il ne fait que formater le `sourceRef` et ouvrir le dialog). Les `documentPieceTypeLabel` / `documentPieceTypeIcon` existants (SF-145-09) couvrent déjà les 25 types.

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|---|---|---|
| F-IA-03 popovers de sources | F-IA-03 traite les sources des outils décisionnels via `source_explanations` (mécanisme différent, popovers avec QA/chat/check). Cohabitation — F-146 s'applique aux listes de faits/points/risques. Pas de conflit. | Intégré |
| DocumentPreviewDialog (SF-145-02) | Réutilisé tel quel via `initialPieceId`. Pas de modification. | Intégré |
| Sections legacy sans `sourceRef` | Fallback automatique sur `legacySource` string. Pas de rupture. | Intégré |
| Outils décisionnels (checklist, ancienneté, indemnités…) | Traités dans SF-146-03. Ici on reste sur les 3 sections de synthèse principales. | SF parallèle (SF-146-03) |
| Chronologie | `TimelineEntry` n'a pas de `sourceRef` aujourd'hui. Extension non prévue dans F-146. | Non applicable (dehors scope SF) |

## Préoccupations transversales
- **Auth / Principal** : aucun impact.
- **Workspace context** : le composant consomme `caseFileId` du parent, qui passe déjà par le filtre workspace standard.
- **Plans / limites** : aucun impact.
- **Navigation / routing** : aucun nouveau chemin. `DocumentPreviewDialog` reste modal.

## Hors scope
- Chronologie (nécessite extension DTO backend timeline)
- Outils décisionnels (SF-146-03)
- Diff analyses (`analysis-diff.component`) — continue à utiliser le rendu legacy
- Highlight page dans le canvas PDF (déjà géré par `initialPieceId` du dialog)
- Backfill des analyses legacy
