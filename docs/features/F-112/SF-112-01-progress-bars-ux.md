# Mini-spec — F-112 / SF-112-01 : Amélioration UX barres de progression

## Identifiant
`F-112 / SF-112-01`

## Feature parente
`F-112` — Amélioration UX barres de progression

## Statut
`in-progress`

## Date de création
`2026-04-04`

## Branche Git
`feat/SF-112-01-progress-bars-ux`

---

## Objectif
Remplacer les indicateurs de progression génériques de la page dossier par des indicateurs riches et adaptatifs : vrai % à l'upload, modes de barre selon le statut, compteurs humains, pipeline visuel linéaire et temps écoulé sur le job actif.

---

## Comportement attendu

### 1 — Upload avec vrai pourcentage
- Lors du `uploadPendingFiles()`, utiliser `HttpClient` avec `{ reportProgress: true, observe: 'events' }` pour chaque fichier.
- Chaque fichier dans la liste "pending" affiche une `mat-progress-bar` avec son `%` réel.
- Une barre globale affiche la progression totale (bytes envoyés / bytes totaux).
- Le bouton "Uploader" reste disponible avec un spinner ; les fichiers individuels montrent leur avancement.

### 2 — Modes de barre adaptatifs par statut
| Statut job | Mode `mat-progress-bar` | Couleur |
|---|---|---|
| `PENDING` | `buffer` | ambre (#C9973A) |
| `PROCESSING` | `determinate` | bleu primaire |
| `DONE` | `determinate` à 100% | vert (#27AE60) |
| `FAILED` | `determinate` à 100% | rouge (warn) |

### 3 — Compteurs humains
- Sous chaque barre afficher `{{ job.processedItems }} / {{ job.totalItems }}` quand `totalItems > 0`.
- Exemple : "8 / 19 documents" pour DOCUMENT_ANALYSIS.
- Masqué quand `totalItems === 0` (état initial PENDING sans données encore chargées).

### 4 — Pipeline visuel linéaire
- Remplacer la liste de `analysis-job-row` par un `AnalysisPipelineComponent` standalone.
- 5 étapes : Upload → Analyse docs → Synthèse → Questions → Synthèse enrichie.
- Chaque étape : icône + label + barre + compteur.
- État d'une étape : `done` (vert), `active` (bleu + animation shimmer), `waiting` (gris), `failed` (rouge).
- L'étape Upload se base sur `uploading()` et le % d'upload ; les autres sur `AnalysisJob`.
- Étapes sans job correspondant (ENRICHED_ANALYSIS non démarré) → `waiting`.

### 5 — Temps écoulé sur le job actif
- Pour chaque job `PROCESSING`, afficher un compteur "depuis X min Y s" incrémenté chaque seconde via `setInterval`.
- L'horodatage de départ = moment où le statut passe à `PROCESSING` (détecté par le polling).
- Réinitialisé à chaque nouveau cycle (nouveau job).
- Masqué pour les statuts PENDING / DONE / FAILED.

### 6 — Amélioration visuelle des spinners (optionnel)
- Remplacer les `mat-spinner` inline par des indicateurs CSS animés (pulse ou shimmer sur la barre elle-même).
- Garder `mat-spinner` uniquement pour les états d'attente de réponse serveur (ex: bouton Analyser).

---

## Cas d'erreur
- Upload partiel (certains fichiers échouent) : barre individuelle en rouge + message existant dans le snackbar.
- Job `FAILED` : étape en rouge dans le pipeline, pas de % affiché.
- `totalItems === 0` : afficher une barre indeterminate ou masquer le compteur (pas de division par zéro).

---

## Critères d'acceptation

- [ ] Upload : chaque fichier pending affiche son % réel pendant l'upload
- [ ] Upload : une barre globale agrège la progression totale
- [ ] Barre PENDING en mode `buffer` (ambre)
- [ ] Barre PROCESSING en mode `determinate` (couleur primaire)
- [ ] Barre DONE en vert à 100%
- [ ] Barre FAILED en rouge
- [ ] Compteur "X / Y" affiché quand `totalItems > 0`
- [ ] Pipeline visuel : 5 étapes visibles, état correct pour chaque étape
- [ ] Temps écoulé affiché sur le job `PROCESSING` actif
- [ ] Aucune régression sur les comportements existants (triggerAnalysis, polling, placeholder)

---

## Composants impactés

| Composant | Type de modification |
|---|---|
| `case-file-detail.component.ts` | Refacto upload (reportProgress), ajout elapsed time |
| `case-file-detail.component.html` | Suppression `analysis-job-row`, insertion `app-analysis-pipeline` |
| `case-file-detail.component.scss` | Suppression styles job-row |
| `document.service.ts` | Ajout méthode `uploadWithProgress()` retournant `Observable<HttpEvent<Document>>` |
| Nouveau : `analysis-pipeline/analysis-pipeline.component.ts` | Pipeline visuel standalone |
| Nouveau : `analysis-pipeline/analysis-pipeline.component.html` | Template pipeline |
| Nouveau : `analysis-pipeline/analysis-pipeline.component.scss` | Styles pipeline + animations |

---

## Plan de test

### Unitaires (PROG)
- PROG-01 : `AnalysisPipelineComponent` — job PENDING → étape `waiting` avec barre `buffer`
- PROG-02 : `AnalysisPipelineComponent` — job PROCESSING 50% → étape `active`, compteur "5 / 10"
- PROG-03 : `AnalysisPipelineComponent` — job DONE → étape `done`, barre verte 100%
- PROG-04 : `AnalysisPipelineComponent` — job FAILED → étape `failed`, barre rouge
- PROG-05 : `AnalysisPipelineComponent` — jobs null → étape Upload `active` si `uploading = true`
- PROG-06 : `AnalysisPipelineComponent` — `totalItems === 0` → compteur masqué
- PROG-07 : `document.service.ts` — `uploadWithProgress()` émet des `UploadProgressEvent`

### Intégration
- Aucune (pure frontend, pas de nouveau endpoint)

### Isolation workspace
- Non applicable (affichage uniquement)

---

## Hors périmètre
- Nouveau endpoint backend
- Notifications push / SSE pour la progression
- Historique des jobs
- Barre de progression dans la liste des dossiers
