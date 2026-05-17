# Mini-spec — F-98 / SF-98-50 — Export Word (.docx) des conclusions

> Cadrages amont : `SF-98-00-coherence.md` (étape 0, invariant 9 « export réutilisable » + ajustement aval « étendre l'export aux conclusions ») + `SF-98-00b-ux-coherence.md` (étape 0 bis — invariant 3 : « tout output a un point de sortie explicite : Copier en V1, export Word en SF-98-50 »). Pas de nouveau cadrage écran.

## Identifiant
`F-98 / SF-98-50`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-50-frontend-export-word` — **SF frontend pure** (pas de volet backend).

---

## Objectif
Permettre à l'avocat de **télécharger** une version de conclusions au format Word `.docx`.

---

## Comportement attendu

### Cas nominal
1. Dans la section « Conclusions », quand la version affichée est au statut `DONE`, un bouton **« Télécharger en Word »** est disponible (à côté de « Copier »).
2. Clic → le frontend génère un fichier `.docx` à partir du `content` de la version et déclenche le téléchargement (blob + `a.download`).
3. Le `.docx` structure le texte : les lignes d'en-tête de section en **MAJUSCULES** (`POUR`, `CONTRE`, `FAITS ET PROCÉDURE`, `DISCUSSION`, `PAR CES MOTIFS`…) sont rendues en **titres** ; le reste en paragraphes.
4. Nom du fichier : `{slug-du-dossier}-conclusions-v{versionNumber}.docx`.

### Cas d'erreur
| Situation | Comportement |
|---|---|
| Version non `DONE` (pas de contenu) | Bouton « Télécharger en Word » absent |
| Échec de la génération du `.docx` | `MatSnackBar` d'erreur, pas de téléchargement |

---

## Analyse de cohérence transversale
- [x] **Pattern existant** : F-95 exporte la **synthèse** en `.docx` **côté client** via `frontend/src/app/core/services/docx-export.service.ts` (librairie npm `docx`). SF-98-50 **réutilise ce mécanisme** pour les conclusions — même librairie, même pattern de téléchargement blob.
- [x] **Backend** : aucun — le `content` est déjà disponible côté frontend via `GET .../conclusions`. Pas d'endpoint d'export.
- [x] **Autres domaines / pays** : l'export est transversal à F-98 — bénéficie à toutes les cellules.

### Décision
- [x] Étendu à la seule cible applicable (la section conclusions) ; réutilise le service docx existant — pas de pattern concurrent créé.

## Conformité F-IA-04
- [x] **Non applicable** — générateur de document, pas un outil décisionnel.

---

## Critères d'acceptation
- [ ] **CA1** — Un bouton « Télécharger en Word » est visible dans la section quand la version est `DONE`.
- [ ] **CA2** — Le clic produit un fichier `.docx` valide téléchargé, nommé `{slug}-conclusions-v{N}.docx`.
- [ ] **CA3** — Le `.docx` contient le texte des conclusions, les en-têtes de section rendus en titres.
- [ ] **CA4** — Le bouton est absent pour une version non `DONE`.
- [ ] **CA5** — Un échec de génération affiche une erreur `MatSnackBar`, sans page cassée.

---

## Périmètre
### Hors scope
- **En-tête cabinet / logo** dans le `.docx` — relève d'un paramétrage cabinet, hors V1.
- **Export PDF** — SF-98-51.
- Mise en forme barreau avancée (interligne réglementaire, numérotation automatique des pièces, pagination spécifique).
- Export Word côté backend — non retenu : le pattern projet (F-95) est l'export client-side.

---

## Technique

### Endpoints
Aucun — **SF frontend pure**, aucun appel réseau supplémentaire (le `content` provient du `GET .../conclusions` déjà consommé par la section).

### Composants Angular
- `docx-export.service.ts` (existant, librairie `docx`) — **ajouter** une méthode `exportConclusion(content, caseTitle, versionNumber)` qui construit le `Document` `docx` et déclenche le téléchargement. Réutiliser le helper de nommage de fichier existant.
- `ConclusionsSectionComponent` — bouton « Télécharger en Word » (visible si `status === 'DONE'`), handler appelant le service.

---

## Plan de test
### Frontend (Jest)
- [ ] `docx-export.service.spec.ts` — `exportConclusion` : déclenche la construction du document et le download ; nom de fichier conforme `{slug}-conclusions-v{N}.docx`.
- [ ] `conclusions-section.component.spec.ts` — bouton visible si `DONE` / absent sinon ; clic → appelle le service ; échec → `MatSnackBar`.
### Isolation workspace
- [x] Non applicable — SF frontend pure, aucune donnée serveur nouvelle, aucun endpoint.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — SF additive, frontend pur, pas d'impact auth/workspace/plans/navigation.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- `F-98 / SF-98-52` (versions) — **done** : l'export porte sur une version (`versionNumber` dans le nom de fichier).
- F-95 — service `docx-export.service.ts` existant, réutilisé.

## Notes et décisions
- **SF frontend pure** : pas de branche backend, pas de PR backend.
- Détection des en-têtes de section : une ligne entièrement en majuscules (hors ponctuation) et courte est traitée comme un titre `docx` ; sinon paragraphe. Arbitrage documenté — robuste pour la structure produite par `CaseConclusionPromptBuilder`.
