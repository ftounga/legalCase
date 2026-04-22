# Mini-spec — F-148 / SF-148-01 Backend Claude Vision (hybride)

## Identifiant · `F-148 / SF-148-01`
## Date · `2026-04-23` · Branche · `feat/SF-148-01-backend-vision-hybrid`

## Objectif
Enrichir sélectivement les pièces scannées avec une description visuelle générée par Claude Vision (multimodal), selon une stratégie hybride **signaux + config** évitant le coût pour les pièces où l'OCR seul suffit (contrats, bulletins, lettres).

## Contexte
F-145 identifie les pièces (type + pages). F-146 permet de citer précisément. F-148 comble la dernière limite : **l'OCR extrait du texte, pas du contexte visuel**. Pour un SMS on ignore qui est à gauche/à droite ; pour une photo pure, on n'a quasi rien ; pour une attestation manuscrite, l'OCR est souvent dégradé.

La stratégie hybride permet de maîtriser le surcoût (~0,01–0,03 €/page vision vs ~0,002 €/page texte) en ne déclenchant vision que là où elle apporte.

## Comportement nominal

### A — Décision "shouldEnrich"
Pour chaque pièce persistée après SF-145-01 :

**Signal 1 (prioritaire) — OCR pauvre** : si la pièce contient au moins une page dont l'extraction OCR fait moins de `app.vision.min-ocr-chars-per-page` (défaut **40**), vision est déclenchée **quel que soit le type**, même s'il est dans la blacklist. Un PHOTO pur à 0 caractère passe toujours par vision.

**Signal 2 — Whitelist par domaine** : si le type figure dans `app.vision.trigger-types-by-domain.<DROIT_DU_TRAVAIL|DROIT_IMMIGRATION|DROIT_FAMILLE>`, vision est déclenchée même avec OCR correct. Typiquement : `SMS`, `ATTESTATION`, `PHOTO`, `PIECE_IDENTITE`, `TITRE_DE_SEJOUR`.

**Blacklist par domaine** — `app.vision.skip-types-by-domain.<DOMAINE>` : si le type y figure ET signal 1 ne s'applique pas, vision est skippée. Typiquement : `CONTRAT`, `LETTRE`, `BULLETIN_PAIE`, `AVIS_IMPOSITION`, `QUITTANCE_LOYER`.

**Si pas en whitelist ET pas en blacklist** → vision skippée (comportement par défaut conservateur).

### B — Feature flag
`app.vision.enabled` (défaut **false** en dev, **true** en staging/prod). Quand désactivé, `shouldEnrichPiece()` retourne toujours `false`. Permet de rouler progressivement.

### C — Appel Claude Vision
Pour chaque pièce retenue :
1. Rasteriser les pages `pageStart..pageEnd` du PDF en PNG (200 DPI, couleur), via PDFBox (même pattern que `OcrService.callTextractRasterized`).
2. Appel multimodal à Claude (par défaut `claude-haiku-4-5-20251001`) avec :
   - système : `"Tu décris précisément le contenu visuel de pièces juridiques scannées. Sortie : description courte et factuelle (4-8 phrases) mettant en évidence les éléments que l'OCR perd (qui parle, disposition, photo, signature, annotation manuscrite). Pas de reformulation du texte déjà lisible."`
   - user content blocks : pour chaque page `{type: "image", source: base64 PNG}` puis un bloc `text` avec le label + type de pièce.
3. Max tokens configurable (défaut `app.vision.max-tokens = 600`).

### D — Persistance
Nouvelles colonnes sur `document_pieces` :
- `visual_description TEXT NULL`
- `vision_enriched_at TIMESTAMP WITH TIME ZONE NULL`
- `vision_model VARCHAR(80) NULL`

Si l'appel échoue : logguer warning, ne rien écrire, fail-open (la pièce reste utilisable sans enrichissement).

### E — Injection dans les prompts IA
`PiecesPromptContext` (F-146) enrichit chaque ligne quand `visualDescription` est présent :
```
  - SMS « Échanges Fatima / Anne » (p. 4-5) — [Vision : Conversation entre deux numéros, messages à gauche en vert…]
```

### F — Hook pipeline
Après `DocumentPieceDetectionService.persistAll`, publier un événement `PiecesPersistedEvent(documentId, pieces, legalDomain)`. `VisionEnrichmentService` écoute l'événement **hors transaction** (`@TransactionalEventListener AFTER_COMMIT`) et dispatche sur `applicationTaskExecutor`. Non bloquant pour le pipeline principal.

## Critères d'acceptation
- [ ] Migration Liquibase `099-add-vision-to-document-pieces.xml` (3 colonnes nullable)
- [ ] Entity `DocumentPiece` : +3 champs (`visualDescription`, `visionEnrichedAt`, `visionModel`)
- [ ] `DocumentPieceSummary` DTO : +`visualDescription` (nullable)
- [ ] `VisionProperties` (`@ConfigurationProperties("app.vision")`) avec `enabled`, `minOcrCharsPerPage`, `model`, `maxTokens`, `triggerTypesByDomain`, `skipTypesByDomain`
- [ ] `VisionEnrichmentService.shouldEnrichPiece(piece, legalDomain, textByPage)` implémente signal 1 + 2 + blacklist
- [ ] `VisionEnrichmentService.enrichPiece(piece, pdfBytes, legalDomain)` rasterise + appelle Claude Vision + persiste
- [ ] `AnthropicService.analyzeWithImages(systemPrompt, userText, images, model, maxTokens)` (nouvelle méthode multimodale)
- [ ] `PiecesPersistedEvent` + `VisionEnrichmentService.onPiecesPersisted` listener async
- [ ] `PiecesPromptContext` appent `— [Vision : …]` quand `visualDescription` présent
- [ ] Tests unitaires : shouldEnrichPiece couvre signal 1, whitelist, blacklist, feature flag off, pas de match → skip
- [ ] Tests : `PiecesPromptContext` rend la ligne avec et sans visual
- [ ] Full backend tests verts

## Plan de test minimal
- U-01 : signal 1 (page OCR < min-chars) → `shouldEnrich = true` même si type blacklisté
- U-02 : signal 2 (type whitelist + OCR OK) → `shouldEnrich = true`
- U-03 : blacklist + OCR OK → `shouldEnrich = false`
- U-04 : pas whitelist, pas blacklist, OCR OK → `shouldEnrich = false` (défaut conservateur)
- U-05 : `enabled = false` → toujours `false` même si whitelist
- U-06 : domain inconnu → utilise config par défaut ou skippe si pas configuré
- U-07 : `PiecesPromptContext` + visualDescription → ligne append `— [Vision : …]`
- U-08 : `PiecesPromptContext` sans visualDescription → format F-146 inchangé (rétrocompat)
- U-09 : fail-open — si l'appel Claude échoue, pas d'exception propagée, `visualDescription` reste null
- I-01 : migration 099 applique 3 colonnes nullable sur H2 sans casser les pièces existantes

## Tables / endpoints / composants impactés
### Backend
- Migration : `099-add-vision-to-document-pieces.xml`
- Entité : `DocumentPiece` (3 nouveaux champs)
- Service : `VisionEnrichmentService` (nouveau)
- Service : `AnthropicService.analyzeWithImages` (nouvelle méthode)
- Config : `VisionProperties` (nouveau `@ConfigurationProperties`)
- Event : `PiecesPersistedEvent` (nouveau)
- `DocumentPieceDetectionService` : publie l'event après `persistAll`
- `PiecesPromptContext` (F-146) : injecte la description visuelle
- `DocumentDto.DocumentPieceSummary` (pour frontend) : +`visualDescription`
- `application.yml` : nouvelles propriétés avec valeurs par défaut

### Pas impacté
- Frontend → SF-148-02
- OCR / extraction pipeline (vision est purement additive, n'influe pas sur l'extraction de texte)

## Impact par domaine métier (FR + BE × 3 domaines)
| Domaine | Types whitelistés par défaut | Rationale |
|---|---|---|
| **Droit du travail** (FR + BE) | `SMS`, `ATTESTATION`, `PHOTO` | SMS : attribution de qui parle, ATTESTATION : signature manuscrite + cachet, PHOTO : preuve non textuelle (lieu de travail, harcèlement visuel). Blacklist : CONTRAT, LETTRE, BULLETIN_PAIE — texte suffit. |
| **Immigration** (FR + BE) | `PHOTO`, `PIECE_IDENTITE`, `TITRE_DE_SEJOUR`, `PASSEPORT`, `VISA`, `RECEPISSE_PREFECTURE` | Documents officiels photographiés : visas, tampons, cachets ont une valeur visuelle (date, signature, macaron). Blacklist : AVIS_IMPOSITION, QUITTANCE_LOYER, ATTESTATION_HEBERGEMENT — texte structuré lisible. |
| **Famille** (FR + BE) | `PHOTO`, `SMS`, `ATTESTATION`, `ACTE_MARIAGE` | Dossiers divorce : SMS pour attribuer les propos, PHOTO pour logement/enfants, ATTESTATION scannée manuscrite, ACTE_MARIAGE pour cachets officiels. Blacklist : JUGEMENT_DIVORCE, LIVRET_FAMILLE, JUSTIFICATIF_REVENUS — textuels. |

Le signal 1 (OCR pauvre) s'applique **à tous les domaines** et à **tous les types** — c'est le filet de sécurité pour les pages photo pure quel que soit le type détecté par Sonnet.

La **distinction FR/BE** n'influe pas sur la whitelist/blacklist dans cette SF : les mêmes types sont pertinents dans les deux pays. Le pays intervient plus tard si besoin (p. ex. spécificités cartes de séjour belges), mais pas au niveau de la stratégie de déclenchement vision.

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|---|---|---|
| F-145 pièces | Source des pièces à enrichir — événement publié après `persistAll` | Intégré |
| F-146 `PiecesPromptContext` | Étendu pour injecter la description visuelle | Intégré |
| F-IA-03 sourceExplanations outils décisionnels | Non touché — indépendant | Non applicable |
| OCR / Textract | Cohabitation — vision est additive, ne remplace pas l'OCR | Intégré (pas de conflit) |
| Coûts IA (F-16 plans/limites) | Coût marginal couvert par l'existant, mesurable via `vision_model` + `vision_enriched_at`. Si volumétrie dérive, sujet à un gate dédié dans une feature future. | Non applicable dans cette SF |
| `AnthropicService` | Nouvelle méthode `analyzeWithImages` — pattern distinct du `analyze` textuel. Composant partagé. | Intégré — nouveau pattern API |

### Nouveau pattern partagé
`AnthropicService.analyzeWithImages` est la première méthode multimodale. Zones de réutilisation potentielles (hors scope SF-148-01) : analyse directe d'un PDF d'acte notarié sans extraction OCR préalable, vérification visuelle d'un diplôme ou sceau, etc. Pattern isolé en une méthode unique pour éviter la duplication future.

## Préoccupations transversales
- **Auth / Principal** : aucun impact — service backend interne.
- **Workspace context** : la pièce est reliée au document via `caseFile → workspace` (isolation préservée en amont par F-145).
- **Plans / limites** : pas de gate pour l'instant (feature activée sans contrôle plan). Si la consommation explose, ajouter un gate F-16 dans une itération ultérieure.
- **Navigation / routing** : aucun impact.

## Hors scope
- UI frontend → **SF-148-02** (badge visible + affichage description dans popup aperçu).
- Gate plan (Pro/Enterprise only) — non tranché, couvrir en V9+ si besoin.
- Ré-enrichissement manuel après ré-analyse — non traité ici (la pièce garde sa `visualDescription` tant qu'elle n'est pas re-détectée par F-145).
- Extraction de tableaux / formulaires complexes (rôle de Textract FORMS).
- Traitement page par page séparé dans la persistance — on stocke une description unique par pièce (concaténant plusieurs pages si nécessaire).
