# F-122 / SF-122-13 — Cadrage cohérence (étape 0)

## Verdict : GO

## Intention métier (1 phrase)

Permettre l'analyse des bordereaux de pièces scannés de plus de 11 pages, aujourd'hui rejetés par l'OCR (`OCR_UNSUPPORTED_SIZE`).

## Nature — extension de F-122, pas une nouvelle feature

Conformément à la règle d'analyse d'overlap : il ne s'agit **pas** d'une nouvelle feature mais d'une **subfeature de F-122** (« OCR pour PDF scannés ») — overlap ~100 % avec le périmètre OCR existant. SF-122-13 lève une limite explicitement documentée comme « itération future » dans le code de `OcrService` (« mode async = hors scope V1 »).

## Workflow métier réel de l'utilisateur cible

Source : pratique standard avocat + signal terrain documenté — incident RENVERSEZ 2026-05-19, dossier `stanojevic` : le bordereau de pièces (PDF scanné 3,3 Mo, > 11 pages) a été refusé par l'OCR.

1. L'avocat rassemble les pièces du dossier ; beaucoup arrivent sous forme de **scans** (bordereau de pièces, pièces communiquées par la partie adverse, courriers).
2. Un bordereau de pièces est typiquement un **PDF scanné multi-pages** (15-30 pages, parfois plus).
3. L'avocat verse ces pièces dans LegalCase.
4. LegalCase tente l'extraction texte ; sur un scan, le texte natif est vide → bascule OCR.
5. L'OCR reconnaît le texte du scan.
6. Le texte alimente l'analyse documentaire puis la synthèse.

## Cartographie features actuelles ↔ workflow

| Étape workflow métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1-3. Rassembler / uploader les pièces | F-43 import / upload | ✅ Livrée |
| 4. Détection texte vide → bascule OCR | F-121-01 + F-122-01 | ✅ Livrée |
| 5. OCR du scan — **≤ 11 pages** | F-122-01 (Textract sync direct) | ✅ Livrée |
| 5. OCR du scan — **format PDF refusé par Textract** | F-122-08 (rasterisation page-par-page PDF → PNG) | ✅ Livrée |
| 5. OCR du scan — **> 11 pages** | — | ❌ **manquant — apport SF-122-13** |
| 6. Analyse + synthèse | F-3/4/5 | ✅ Livrée |

## Position de la nouvelle feature

Étape 5 — l'OCR. SF-122-13 ne crée aucune brique technique nouvelle : la voie de **rasterisation page-par-page** (`OcrService.callTextractRasterized`, livrée par SF-122-08) traite déjà les PDF page par page et **n'a aucune limite de pages**. SF-122-13 se borne à **aiguiller** le cas « > 11 pages » vers cette voie au lieu de le rejeter.

## Challenge amont

Chaque étape avant l'OCR multi-pages est-elle couverte ?
- Upload (1-3) : ✅ F-43.
- Détection texte vide → OCR (4) : ✅ F-121-01 / F-122-01.
- Voie technique multi-pages (la rasterisation page-par-page) : ✅ **déjà livrée par SF-122-08** — c'est le point clé : la brique amont existe, SF-122-13 ne fait que la router.

**Aucun trou amont.**

## Challenge aval

Après un OCR réussi, l'extraction passe `DONE` et le pipeline d'analyse standard prend le relais (`OcrResult.successRasterized` est déjà consommé par `ExtractionService` — branche SF-122-08). ✅ **Aucun trou aval.**

## STOPs / pré-requis à ajouter au backlog

Aucun. Le seul « trou » est une limite de routage interne à `OcrService` ; la voie de traitement existe déjà.

## Invariants anti-gadget pour la mini-spec

1. **Réutiliser `callTextractRasterized` (SF-122-08)** — ne pas réimplémenter de chemin OCR multi-pages parallèle.
2. **Borne haute obligatoire** — un cap de pages pour la voie rasterisée (ex. ~200) : OCR-iser page par page un PDF de 1000 pages = coût Textract et latence non bornés. Au-delà du cap → `OCR_UNSUPPORTED_SIZE` reste le comportement.
3. **Gate quota OCR conservé** — `PlanLimitService.isOcrQuotaExceeded` (SF-122-02) reste appliqué : un gros scan consomme son quota page par page, normalement.
4. **Aucune régression de la voie directe** — un PDF ≤ 11 pages reste traité en un seul appel Textract rapide ; la rasterisation n'est empruntée que pour les cas hors limites directes (ou le fallback `UnsupportedDocumentException` existant).
5. **Pas de mode async** — SF-122-13 résout le besoin avec la voie sync/rasterisée existante ; l'API Textract asynchrone reste hors scope (non nécessaire pour des bordereaux de quelques dizaines de pages).

## Décision finale

**GO.** Toutes les briques amont/aval sont livrées — la voie de rasterisation page-par-page (SF-122-08) est en particulier déjà capable de traiter un PDF multi-pages. SF-122-13 est une subfeature de routage interne à `OcrService`. Feature **purement backend, sans impact écran** (le badge « OCR en cours » existant couvre la latence accrue) → **étape 0 bis non applicable**.
