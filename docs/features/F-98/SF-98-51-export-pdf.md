# Mini-spec — F-98 / SF-98-51 — Export PDF des conclusions

> Cadrages amont : `SF-98-00-coherence.md` (étape 0, ajustement aval « export ») + `SF-98-00b-ux-coherence.md` (étape 0 bis, invariant 3 « tout output a un point de sortie »). SF analogue à SF-98-50 (export Word) — pas de nouveau cadrage écran.

## Identifiant
`F-98 / SF-98-51`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`done` — livrée 2026-05-18 (PR #1009).

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-51-frontend-export-pdf` — **SF frontend pure** (pas de backend).

---

## Objectif
Permettre à l'avocat de **télécharger** une version de conclusions au format **PDF**.

---

## Comportement attendu

### Cas nominal
1. Dans la section « Conclusions », quand la version affichée est au statut `DONE`, un bouton **« Télécharger en PDF »** est disponible à côté de « Télécharger en Word ».
2. Clic → le frontend génère un fichier `.pdf` à partir du `content` de la version (génération client-side) et déclenche le téléchargement.
3. Le PDF structure le texte : les en-têtes de section en MAJUSCULES (`POUR`, `CONTRE`, `FAITS ET PROCÉDURE`, `DISCUSSION`, `PAR CES MOTIFS`…) en titres, le reste en paragraphes.
4. Nom du fichier : `{slug-du-dossier}-conclusions-v{versionNumber}.pdf`.

### Cas d'erreur
| Situation | Comportement |
|---|---|
| Version non `DONE` (pas de contenu) | Bouton « Télécharger en PDF » absent |
| Échec de la génération du `.pdf` | `MatSnackBar` d'erreur, pas de téléchargement |

---

## Analyse de cohérence transversale
- [x] **Pattern existant** : le frontend dispose déjà de la librairie `pdfmake` et d'un `pdf-export.service.ts`. SF-98-51 réutilise ce mécanisme pour les conclusions — symétrique de SF-98-50 (export Word via `docx-export.service.ts`).
- [x] **Backend** : aucun — le `content` est déjà disponible côté frontend.
- [x] **Détection des en-têtes de section** : réutilise la même heuristique que SF-98-50 (`isSectionHeading`) — factoriser si possible.

### Décision
- [x] Étendu à la section conclusions ; réutilise le service PDF existant — pas de pattern concurrent.

## Conformité F-IA-04
- [x] **Non applicable** — générateur de document, pas un outil décisionnel.

---

## Critères d'acceptation
- [ ] **CA1** — Un bouton « Télécharger en PDF » est visible dans la section quand la version est `DONE`.
- [ ] **CA2** — Le clic produit un fichier `.pdf` valide téléchargé, nommé `{slug}-conclusions-v{N}.pdf`.
- [ ] **CA3** — Le PDF contient le texte des conclusions, en-têtes de section en titres.
- [ ] **CA4** — Le bouton est absent pour une version non `DONE`.
- [ ] **CA5** — Un échec de génération affiche une erreur `MatSnackBar`, sans page cassée.

---

## Périmètre
### Hors scope
- En-tête cabinet / logo dans le PDF.
- Mise en forme barreau avancée (pagination réglementaire, interligne).
- Export Word — SF-98-50 (livré).

---

## Technique
### Endpoints
Aucun — **SF frontend pure**.

### Composants Angular
- `pdf-export.service.ts` (existant, `pdfmake`) — ajouter une méthode `exportConclusion(content, caseTitle, versionNumber)` qui construit le document `pdfmake` et déclenche le téléchargement.
- `ConclusionsSectionComponent` — bouton « Télécharger en PDF » (visible si `status === 'DONE'`), handler appelant le service.
- Réutiliser/partager le helper de détection d'en-tête de section de SF-98-50 (`isSectionHeading`) et le helper de nommage de fichier.

---

## Plan de test
### Frontend (Jest)
- [ ] `pdf-export.service.spec.ts` — `exportConclusion` : construction du document + download ; nom `{slug}-conclusions-v{N}.pdf`.
- [ ] `conclusions-section.component.spec.ts` — bouton visible si `DONE` / absent sinon ; clic → service ; échec → `MatSnackBar`.
### Isolation workspace
- [x] Non applicable — SF frontend pure, aucun endpoint.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — SF additive, frontend pur.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- SF-98-52 (versions) — done : l'export porte sur une version (`versionNumber`).
- SF-98-50 (export Word) — done : pattern + heuristique d'en-tête réutilisés.

## Notes et décisions
- **SF frontend pure** — pas de branche backend.
- Si `pdf-export.service.ts` n'existe pas exactement sous ce nom, réutiliser le service `pdfmake` existant repéré dans le frontend ; documenter le choix.
