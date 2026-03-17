# Mini-spec — F-07 / SF-07-01 — Infrastructure document_chunks

---

## Identifiant

`F-07 / SF-07-01`

## Feature parente

`F-07` — Chunking

## Statut

`ready`

## Date de création

2026-03-17

## Branche Git

`feat/SF-07-chunks`

---

## Objectif

Créer la table `document_chunks`, l'entité JPA et le repository pour persister les segments de texte issus du chunking.

---

## Comportement attendu

### Cas nominal

La migration Liquibase crée la table `document_chunks`. L'entité JPA `DocumentChunk` permet de persister et lire les chunks. Le repository expose `findByExtractionOrderByChunkIndex()`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Migration déjà appliquée | Liquibase ignore (checksum) | N/A |

---

## Critères d'acceptation

- [ ] Table `document_chunks` créée par la migration 009
- [ ] FK `extraction_id` → `document_extractions(id)` présente
- [ ] Index sur `extraction_id` présent
- [ ] `DocumentChunk` persistable via JPA
- [ ] `findByExtractionOrderByChunkIndex()` opérationnel
- [ ] `@PrePersist` pour `created_at`

---

## Périmètre

### Hors scope (explicite)

- Logique de chunking (SF-07-02)
- Endpoint API pour consulter les chunks
- Vectorisation / embeddings (F-08+)

---

## Technique

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| document_chunks | CREATE TABLE | Migration 009 |

### Schéma

| Colonne | Type | Notes |
|---------|------|-------|
| id | UUID PK | Généré par JPA |
| extraction_id | UUID FK → document_extractions(id) | Non nullable |
| chunk_index | INTEGER | Ordre du chunk dans le document |
| chunk_text | TEXT | Contenu textuel du segment |
| token_count | INTEGER | Nombre approximatif de tokens |
| chunk_metadata | TEXT | JSON optionnel (startChar, endChar) |
| created_at | TIMESTAMP WITH TIME ZONE | Auto @PrePersist |

### Migration Liquibase

- [ ] `009-create-document-chunks.xml`

---

## Plan de test

### Tests d'intégration

- [ ] Couvert par ChunkingServiceIT — persistance vérifiée via le repository

### Isolation workspace

- [ ] Non applicable — pas d'endpoint exposé

---

## Dépendances

### Subfeatures bloquantes

- F-06 / SF-06-01 — table `document_extractions` existante — statut : done

---

## Notes et décisions

- La FK est sur `extraction_id` (et non `document_id`) — un chunk appartient à une extraction, pas directement au document
- `token_count` approximé à `chunk_text.length() / 4` (1 token ≈ 4 caractères) — suffisant pour V1, pas de tokenizer externe requis
- `columnDefinition = "TEXT"` évité pour compatibilité H2 — `length = Integer.MAX_VALUE` utilisé
