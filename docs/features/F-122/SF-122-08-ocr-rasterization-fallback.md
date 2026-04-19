# Mini-spec — F-122 / SF-122-08 Fallback rasterisation PDF → PNG

## Identifiant `F-122 / SF-122-08`  · Statut `draft`  · Date `2026-04-19`
## Branche `feat/SF-122-08-ocr-rasterization-fallback`

---

## Objectif

Rendre l'OCR robuste aux PDFs que **AWS Textract refuse** avec
`UnsupportedDocumentException` (scans Xerox/Canon avec compression CCITT
Group 4, rotation metadata non-standard, PDF linéarisés exotiques, etc.).

Observé 2026-04-19 sur dossier MEA (staging) : 5/9 PDFs rejetés en
`OCR_FAILED` malgré un fichier humainement lisible. Diagnostic sur P8.pdf :
4 pages, rotation 270°, images CCITT 3507x2480 gray.

La solution : quand Textract rejette le PDF brut, on **rasterise localement**
chaque page en PNG via PDFBox (formats PDF opaques → pixels bruts), puis on
renvoie chaque PNG à Textract qui les accepte toujours (format universel).

---

## Comportement

### Fallback séquentiel

```
┌─────────────────────┐
│  PDF scanné         │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ Textract direct PDF │  ← tentative 1 (1 call)
└──────────┬──────────┘
           │
      ┌────┴─────┐
      │          │
  succès       UnsupportedDocumentException
      │          │
      ▼          ▼
  ✅ DONE   ┌─────────────────────┐
            │ PDFBox renders N    │  ← fallback
            │ pages → N PNG       │
            └──────────┬──────────┘
                       │
                       ▼
            ┌─────────────────────┐
            │ Textract on N PNGs  │  ← N calls, sequential
            └──────────┬──────────┘
                       │
                       ▼
                 ✅ DONE ou ❌ FAILED
```

### Quand rasteriser

Uniquement sur `UnsupportedDocumentException` du call direct PDF. Pas en
préventif — les PDFs bien formés restent sur la voie rapide 1 call.

### Paramètres de rasterisation

- **DPI** : 200 (recommandation AWS pour OCR — équilibre qualité/taille)
- **ImageType** : GRAY (scans de documents, couleurs inutiles, réduit taille)
- **Format sortie** : PNG (lossless, accepté par Textract jusqu'à 10 Mo/image)
- **Une call Textract par page** : agrégation du texte (ordre des pages préservé)

### Quota

Chaque page rasterisée = 1 call Textract = 1 page dans le compteur quota.
Rétrocompat avec SF-122-02 : `pageCount` retourné reste le nombre de pages
du PDF original, que ce soit sync direct (1 call, tous blocks) ou raster
(N calls). Pas de surfacturation du mode fallback.

### Cas limite

| Situation | Comportement |
|---|---|
| PDF > 11 pages | Bloqué en amont (`OCR_UNSUPPORTED_SIZE`) — rasterisation pas tentée |
| PDF > 5 Mo mais < 11 pages | Bloqué en amont — même si rasterisation passerait, limite conservée pour V1 (coherent avec bloc de garde existant) |
| Rasterisation d'une page timeout/échec | Log + skip page (on agrège ce qui a marché). Si zero page → `OCR_FAILED` |
| Textract refuse aussi le PNG (rare) | Log + skip page. Continue sur les pages suivantes |
| PDFBox échoue à rendre une page (PDF corrompu) | Exception → failure `OCR_FAILED` pour le doc entier |
| Mode FORMS activé + rasterisation | Chaque PNG envoyé avec `featureTypes(TABLES, FORMS)` — même coût que mode TABLES côté multiplier ×3 du quota |

---

## Critères d'acceptation

- [ ] `OcrService.tryOcr` tente le call direct PDF d'abord (comportement actuel)
- [ ] Sur `UnsupportedDocumentException`, rasterise via PDFBox et appelle Textract par page
- [ ] Agrégation du texte de toutes les pages avec séparateurs `\n\n`
- [ ] Logs clairs : "falling back to rasterization" + "rendered page N of M"
- [ ] `pageCount` retourné = nb de pages du PDF original
- [ ] Metadata extraction inclut `"extractor":"textract-rasterized"` pour traçabilité
- [ ] Tests unitaires : succès direct / fallback success / fallback partiel / fallback échec complet
- [ ] Build backend vert, tests existants verts

## Hors scope

- Rasterisation en parallèle (optimisation future)
- Support PDF > 11 pages via API Textract async (SF séparée éventuelle)
- Choix DPI configurable par workspace (V2)
- Gestion pages multi-colonnes / rotation page-par-page (relied on PDFBox default)

---

## Technique

| Fichier | Opération |
|---|---|
| `backend/src/main/java/fr/ailegalcase/ocr/OcrService.java` | Ajoute `callTextractRasterized` + catch `UnsupportedDocumentException` |
| `backend/src/test/java/fr/ailegalcase/ocr/OcrServiceTest.java` | 3 TU nouveaux (fallback triggered / success multi-page / both direct and raster fail) |

### PDFBox API utilisée

```java
try (PDDocument doc = Loader.loadPDF(fileBytes)) {
    PDFRenderer renderer = new PDFRenderer(doc);
    for (int i = 0; i < doc.getNumberOfPages(); i++) {
        BufferedImage image = renderer.renderImageWithDPI(i, 200, ImageType.GRAY);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        byte[] pngBytes = baos.toByteArray();
        // ... Textract call with pngBytes
    }
}
```

PDFBox respecte la `Page.rotation` metadata lors du rendering — la page
est rendue dans l'orientation visible à l'écran.

### Coût estimé

- Rasterisation : ~500 ms par page à 200 DPI GRAY
- Call Textract : ~1 s par page
- Total pour 11 pages : ~16 s (sync, bloquant dans un thread async Spring)

Acceptable car déclenché uniquement quand le call direct échoue, et
l'extraction reste asynchrone côté UI (polling 3s).

---

## Analyse d'impact

- [ ] Auth / workspace / navigation : non touchés
- [x] Plans / limites : quota inchangé (1 page = 1 page, même en raster).
  `incrementOcrUsage` appelé avec `pageCount` correct (nb pages PDF réel).
- [ ] Migration DB : aucune.

Aucun smoke E2E concerné.
