# Mini-spec — F-56 / SF-56-01 Snapshot documents par analyse

> Ce document doit être validé AVANT de démarrer le dev.

---

## Identifiant

`F-56 / SF-56-01`

## Feature parente

`F-56` — Diff sémantique avec attribution des raisons

## Statut

`ready`

## Date de création

2026-03-26

## Branche Git

`feat/SF-56-01-snapshot-documents-analyse`

---

## Objectif

Enregistrer un snapshot des documents présents dans un dossier au moment du déclenchement de chaque analyse (STANDARD ou ENRICHIE), afin de permettre ultérieurement l'attribution des raisons de changement dans le diff sémantique.

---

## Comportement attendu

### Cas nominal

Au moment où une analyse dossier est déclenchée (`CaseAnalysisCommandService` ou `ReAnalysisCommandService`), après création de l'entité `CaseAnalysis`, on insère dans `analysis_documents` une ligne par document existant (non supprimé) dans le dossier. Chaque ligne contient : l'`analysis_id`, le `document_id` et le `document_name` (snapshot du `originalFilename` à cet instant).

Le snapshot est immuable : les ajouts/suppressions de documents ultérieurs n'affectent pas les snapshots passés.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Aucun document dans le dossier au moment de l'analyse | Snapshot vide — aucune erreur, l'analyse continue normalement | — |
| Échec de l'insertion du snapshot | Rollback de la transaction (le snapshot fait partie de la même transaction que la création de l'analyse) | 500 |

---

## Critères d'acceptation

- [ ] La table `analysis_documents` est créée via migration Liquibase 030
- [ ] À chaque déclenchement d'une analyse STANDARD, les documents du dossier sont snapshotés
- [ ] À chaque déclenchement d'une analyse ENRICHIE, les documents du dossier sont snapshotés
- [ ] Le snapshot contient `analysis_id`, `document_id`, `document_name` et `created_at`
- [ ] Si un document est supprimé après l'analyse, son entrée dans `analysis_documents` est conservée
- [ ] Le snapshot d'une analyse A n'est pas affecté par les actions sur d'autres analyses du même dossier
- [ ] Isolation workspace : un utilisateur ne peut pas accéder aux snapshots d'un autre workspace (via la FK analysis_id → case_analyses → case_file → workspace)

---

## Périmètre

### Hors scope (explicite)

- Aucune modification de l'API REST existante (pas de nouvel endpoint)
- Pas d'utilisation du snapshot dans le diff (SF-56-02)
- Pas d'affichage frontend (SF-56-03)
- Pas de migration de l'historique : les analyses existantes auront un snapshot vide — comportement accepté

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| created_at | now() | Renseigné automatiquement à l'insert |
| document_name | originalFilename du document | Snapshot immuable — ne suit pas les renommages futurs |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format | Unicité |
|-------|-------------|-------------|--------|---------|
| analysis_id | Oui | — | UUID FK → case_analyses | Non |
| document_id | Oui | — | UUID (peut ne plus exister en base) | Non |
| document_name | Oui | 500 | Texte libre (nom de fichier) | Non |

Notes :
- `document_id` est volontairement **sans FK** vers `documents` — le document peut être supprimé après le snapshot, la référence doit rester valide
- La paire `(analysis_id, document_id)` est unique (contrainte d'unicité en base)

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `analysis_documents` | CREATE + INSERT | Nouvelle table |
| `case_analyses` | SELECT | Lecture de l'analysis_id après création |
| `documents` | SELECT | Lecture des documents du dossier au moment du snapshot |

### Migration Liquibase

- [x] Oui — `030-create-analysis-documents.xml`

```sql
CREATE TABLE analysis_documents (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  analysis_id     UUID NOT NULL REFERENCES case_analyses(id) ON DELETE CASCADE,
  document_id     UUID NOT NULL,
  document_name   VARCHAR(500) NOT NULL,
  created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
  CONSTRAINT uq_analysis_document UNIQUE (analysis_id, document_id)
);

CREATE INDEX idx_analysis_documents_analysis_id ON analysis_documents(analysis_id);
```

### Composants Angular

Non applicable.

---

## Plan de test

### Tests unitaires

- [ ] `AnalysisDocumentSnapshotService` — cas nominal : 3 documents → 3 lignes insérées avec les bons noms
- [ ] `AnalysisDocumentSnapshotService` — dossier vide : 0 document → snapshot vide, pas d'erreur
- [ ] `CaseAnalysisCommandService` — vérifie que `snapshotDocuments()` est appelé après création de l'analyse
- [ ] `ReAnalysisCommandService` — même vérification pour l'analyse enrichie

### Tests d'intégration

- [ ] Déclencher une analyse → vérifier que `analysis_documents` contient les bons documents
- [ ] Supprimer un document puis vérifier que le snapshot de l'analyse précédente est intact
- [ ] Deux analyses successives sur le même dossier → deux snapshots indépendants

### Isolation workspace

- [ ] Applicable — le snapshot est accessible uniquement via `analysis_id → case_analyses → case_file → workspace_id` du membre connecté

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — modification interne au pipeline, pas de surface exposée

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de changement de comportement visible utilisateur

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- `document_id` sans FK intentionnel : les documents peuvent être supprimés (hard delete), le snapshot doit survivre à la suppression.
- Le snapshot est créé dans la même transaction que la création de `CaseAnalysis` → cohérence garantie.
- Les analyses existantes (avant cette migration) auront un snapshot vide — acceptable, le diff sémantique (SF-56-02) gèrera ce cas en dégradant gracieusement (pas de raison documentaire disponible).
