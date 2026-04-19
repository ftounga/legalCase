# Mini-spec — F-122 / SF-122-06 Corriger la logique du bandeau retry OCR

## Identifiant `F-122 / SF-122-06`  · Statut `draft`  · Date `2026-04-19`
## Branche `feat/SF-122-06-fix-retry-banner-logic`

---

## Objectif

Corriger l'incohérence UX pointée pendant la validation staging : après
l'activation de Textract (SF-122-01→05), **un nouvel upload dont l'OCR
a déjà échoué avec `EMPTY_TEXT` déclenche quand même le bandeau "Relancer
avec OCR"** — ce qui est futile (Textract va ré-extraire 0 blocks).

Le bandeau doit rester visible **uniquement** pour les documents où un
retry OCR a une vraie chance de succès.

---

## Comportement corrigé

| État du doc | Motif | OCR déjà tenté ? | Bandeau retry ? |
|---|---|---|---|
| Legacy MEA (avant activation) | `EMPTY_TEXT` | Non | ✅ oui (1ère tentative OCR) |
| Nouveau upload, OCR planté transient | `OCR_FAILED` | Oui | ✅ oui (retry légitime — AWS peut être down) |
| Nouveau upload, OCR a renvoyé 0 blocks | `EMPTY_TEXT` | Oui | ❌ non (sera même résultat) |
| Doc > 5 Mo / > 11 pages | `OCR_UNSUPPORTED_SIZE` | N/A | ❌ non (déjà exclu, à traiter en SF-122-08 async) |
| Quota épuisé | `OCR_QUOTA_EXCEEDED` | Non | ❌ non (déjà exclu, à acheter pack) |

## Détection "OCR déjà tenté"

Utilise le champ existant `extractionMetadata` — `ExtractionService` écrit
déjà `"extractor":"internal+textract"` quand OCR a été appelé (que ce
soit un succès ou non). Aucune migration nécessaire.

Filtre côté `OcrRetryService` :
- Pour motif `EMPTY_TEXT` : retry éligible **uniquement si**
  `extractionMetadata` ne contient pas `"textract"`
- Pour motif `OCR_FAILED` : toujours éligible (transient AWS plausible)

---

## Critères d'acceptation

- [ ] `OcrRetryService.preview` et `.retry` excluent les docs `EMPTY_TEXT`
  dont `extractionMetadata` contient `"textract"`
- [ ] Les docs `EMPTY_TEXT` legacy (metadata = `"internal"`) restent éligibles
- [ ] Les docs `OCR_FAILED` restent toujours éligibles
- [ ] Tests unitaires sur le nouveau filtre (3 cas au minimum)
- [ ] Build backend vert, tests existants verts

## Hors scope

- Changement de libellés frontend (bandeau reste "Relancer avec OCR")
- Gestion séparée de `OCR_QUOTA_EXCEEDED` vers billing (SF-122-04-UI à venir)
- Gestion de `OCR_UNSUPPORTED_SIZE` (SF-122-08 async)

---

## Technique

| Fichier | Opération |
|---|---|
| `backend/src/main/java/fr/ailegalcase/ocr/OcrRetryService.java` | Filtre Java sur les résultats de `findRetryableByCaseFile` |
| `backend/src/test/java/fr/ailegalcase/ocr/OcrRetryServiceTest.java` | NOUVEAU — tests du filtre |

Pas de migration, pas de modif frontend, pas d'endpoint nouveau.
