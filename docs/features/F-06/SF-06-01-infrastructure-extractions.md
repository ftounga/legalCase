# Mini-spec — F-06 / SF-06-01 — Infrastructure document_extractions

> Statut : done (rétroactif — implémenté le 2026-03-17)

---

## Identifiant

`F-06 / SF-06-01`

## Feature parente

`F-06` — Extraction de texte

## Statut

`done`

## Date de création

2026-03-17 (rétroactif)

## Branche Git

`feat/SF-06-01-infrastructure-extractions` *(non créée — commit direct sur master, écart de gouvernance corrigé rétroactivement)*

---

## Objectif

Créer la table `document_extractions`, l'entité JPA associée et le repository pour persister les résultats d'extraction de texte.

---

## Comportement attendu

### Cas nominal

La migration Liquibase crée la table `document_extractions` avec les colonnes nécessaires. L'entité JPA `DocumentExtraction` permet de persister et lire les extractions. Le repository expose `findByDocumentId()`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Migration déjà appliquée | Liquibase ignore (checksum) | N/A |

---

## Critères d'acceptation

- [x] Table `document_extractions` créée par la migration 008
- [x] FK `document_id` → `documents(id)` présente
- [x] Index sur `document_id` présent
- [x] Enum `ExtractionStatus` : PENDING, PROCESSING, DONE, FAILED
- [x] `DocumentExtraction` persistable via JPA
- [x] `findByDocumentId()` opérationnel
- [x] `@PrePersist` / `@PreUpdate` pour created_at / updated_at

---

## Périmètre

### Hors scope (explicite)

- Logique d'extraction (SF-06-02)
- Endpoint API pour consulter l'extraction
- Chunking (F-07)

---

## Technique

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| document_extractions | CREATE TABLE | Migration 008 |

### Migration Liquibase

- [x] Oui — `008-create-document-extractions.xml`

---

## Plan de test

### Tests d'intégration

- [x] Couvert par ExtractionServiceIT — les tests vérifient la persistance via le repository

### Isolation workspace

- [ ] Non applicable — pas d'endpoint exposé dans cette subfeature

---

## Dépendances

### Subfeatures bloquantes

- F-05 / SF-05-01 — table documents existante — statut : done

---

## Notes et décisions

- `columnDefinition = "TEXT"` évité pour la compatibilité H2 (tests) — `length = Integer.MAX_VALUE` utilisé à la place
- `gen_random_uuid()` et `now()` retirés des defaults Liquibase (incompatibles H2) — gestion par JPA (`@PrePersist`, `@GeneratedValue`)
