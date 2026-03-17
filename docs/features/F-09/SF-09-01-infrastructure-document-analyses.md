# Mini-spec — F-09 / SF-09-01 — Infrastructure table document_analyses

---

## Identifiant

`F-09 / SF-09-01`

## Feature parente

`F-09` — Analyse IA — document

## Statut

`ready`

## Date de création

2026-03-17

## Branche Git

`feat/SF-09-infrastructure-document-analyses`

---

## Objectif

Créer la table `document_analyses`, l'entité JPA et le repository pour persister la synthèse IA d'un document (agrégation des analyses de chunks).

---

## Comportement attendu

### Cas nominal

La migration Liquibase crée la table `document_analyses`. L'entité JPA `DocumentAnalysis` permet de persister et lire les analyses de documents. Le repository expose `findByExtractionId()` et `existsByExtractionIdAndAnalysisStatusIn()`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Migration déjà appliquée | Liquibase ignore (checksum) | N/A |

---

## Critères d'acceptation

- [ ] Table `document_analyses` créée par la migration 011
- [ ] FK `extraction_id` → `document_extractions(id)` présente
- [ ] FK `document_id` → `documents(id)` présente
- [ ] Index sur `extraction_id` présent
- [ ] `DocumentAnalysis` persistable via JPA
- [ ] `findByExtractionId()` opérationnel
- [ ] `existsByExtractionIdAndAnalysisStatusIn()` opérationnel
- [ ] `@PrePersist` / `@PreUpdate` pour `created_at` / `updated_at`

---

## Périmètre

### Hors scope (explicite)

- Logique de déclenchement et d'agrégation (SF-09-02)
- Endpoint API pour consulter les analyses
- Analyse niveau dossier (F-10)

---

## Technique

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| document_analyses | CREATE TABLE | Migration 011 |

### Schéma document_analyses

| Colonne | Type | Notes |
|---------|------|-------|
| id | UUID PK | Généré par JPA |
| document_id | UUID FK → documents(id) | Non nullable |
| extraction_id | UUID FK → document_extractions(id) | Non nullable |
| analysis_status | VARCHAR(20) | Enum : PENDING / PROCESSING / DONE / FAILED |
| analysis_result | TEXT | JSON retourné par Anthropic (nullable jusqu'à DONE) |
| model_used | VARCHAR(100) | Nullable jusqu'à DONE |
| prompt_tokens | INTEGER | Nullable jusqu'à DONE |
| completion_tokens | INTEGER | Nullable jusqu'à DONE |
| created_at | TIMESTAMP WITH TIME ZONE | Auto @PrePersist |
| updated_at | TIMESTAMP WITH TIME ZONE | Auto @PrePersist + @PreUpdate |

### Migration Liquibase

- [ ] `011-create-document-analyses.xml`

---

## Plan de test

### Tests d'intégration

- [ ] Couvert par `DocumentAnalysisServiceIT` (SF-09-02) — persistance vérifiée via le repository

### Isolation workspace

- [ ] Non applicable — pas d'endpoint exposé

---

## Dépendances

### Subfeatures bloquantes

- F-08 / SF-08-01 — table `chunk_analyses` existante — statut : done
- F-07 / SF-07-01 — table `document_chunks` existante — statut : done

---

## Notes et décisions

- Double FK (document_id + extraction_id) : accès direct au document sans jointure via extraction, utile pour les requêtes futures (F-10, F-12)
- `AnalysisStatus` enum réutilisé depuis le package `fr.ailegalcase.analysis` (déjà défini en F-08)
- `existsByExtractionIdAndAnalysisStatusIn()` nécessaire dans SF-09-02 pour éviter de déclencher deux fois l'analyse d'un même document
