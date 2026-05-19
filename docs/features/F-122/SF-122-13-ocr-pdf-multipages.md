# Mini-spec — F-122 / SF-122-13 OCR des PDF scannés multi-pages volumineux

## Identifiant

`F-122 / SF-122-13`

## Feature parente

`F-122` — OCR pour PDF scannés (AWS Textract)

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-122-13-ocr-pdf-multipages`

---

## Contexte

Incident RENVERSEZ 2026-05-19 (dossier `stanojevic`) : un bordereau de pièces scanné (PDF 3,3 Mo, > 11 pages) est refusé par l'OCR avec le motif `OCR_UNSUPPORTED_SIZE`. `OcrService.tryOcr` rejette tout PDF de plus de 11 pages **avant** tout appel Textract — la voie directe Textract sync ne traite que les documents ≤ 11 pages.

Or `OcrService` possède déjà, depuis SF-122-08, une voie `callTextractRasterized` qui rend chaque page du PDF en PNG via PDFBox et OCR-ise chaque page séparément — **sans aucune limite de pages**. Elle n'est aujourd'hui empruntée que sur `UnsupportedDocumentException`. SF-122-13 l'aiguille aussi sur le cas « trop de pages / trop volumineux ».

---

## Objectif

Router les PDF scannés dépassant les limites de la voie Textract directe vers la voie de rasterisation page-par-page existante (SF-122-08), au lieu de les rejeter — avec une borne haute pour ne pas traiter des PDF arbitrairement gros.

---

## Comportement attendu

### Cas nominal

`OcrService.tryOcr` — logique de routage révisée. Deux nouveaux seuils dans `OcrProperties`, dédiés à la voie rasterisée :
- `maxRasterizedPages` (défaut **200**)
- `maxRasterizedSizeMb` (défaut **50**)

Ordre des vérifications :
1. Toggle `aws.textract.enabled` — inchangé.
2. `sizeMb >= maxRasterizedSizeMb` → `OCR_UNSUPPORTED_SIZE` (réellement trop volumineux).
3. `pdfPages > maxRasterizedPages` → `OCR_UNSUPPORTED_SIZE` (trop de pages même pour la voie rasterisée).
4. Gate quota OCR (`PlanLimitService.isOcrQuotaExceeded`, `pdfPages`, ×3 si forms) — inchangé.
5. **Routage** :
   - si `sizeMb >= maxSizeMb` (5) **ou** `pdfPages > maxPages` (11) → `callTextractRasterized` directement (la voie directe échouerait) ;
   - sinon → `callTextractDirect`, qui conserve son fallback existant `UnsupportedDocumentException` → `callTextractRasterized`.

La voie `callTextractRasterized` et le type `OcrResult.successRasterized` sont déjà consommés par `ExtractionService` (branche SF-122-08) — aucune modification en aval.

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| `sizeMb >= maxRasterizedSizeMb` | `OCR_UNSUPPORTED_SIZE` |
| `pdfPages > maxRasterizedPages` | `OCR_UNSUPPORTED_SIZE` |
| Quota OCR mensuel / cap journalier dépassé | `OCR_QUOTA_EXCEEDED` (inchangé) |
| Rasterisation : toutes les pages échouent chez Textract | `OCR_FAILED` (inchangé — comportement existant de `callTextractRasterized`) |
| PDF non parsable par PDFBox | `countPdfPages` renvoie 1 → voie directe → échec Textract éventuel → `OCR_FAILED` (inchangé) |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** — non applicable : brique d'extraction documentaire, pas un outil décisionnel.
- [x] **Autres pays / domaines** — non applicable : aucune logique pays/domaine.
- [x] **Autres UI patterns** — non applicable : SF backend pure, aucun changement frontend.
- [x] **Autres flows transversaux** — **Plans / limites** : SF-122-13 conserve le gate quota OCR existant (`isOcrQuotaExceeded`) sans le modifier ; un PDF multi-pages consomme `pdfPages` pages de quota, page par page, comme tout scan. Pas de nouveau gate, pas de changement de quota.

### Décision

- [x] Étendu à la seule cible applicable (routage `OcrService`). Le chemin image (`extractFromImage`) n'est pas concerné — une image native est mono-page. Aucune cible orpheline.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — SF backend pure, aucun composant frontend.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — SF backend pure, ne crée ni ne modifie aucun outil décisionnel.

---

## Critères d'acceptation

- [ ] Un PDF de N pages, `11 < N ≤ maxRasterizedPages`, est routé vers `callTextractRasterized` — plus de `OCR_UNSUPPORTED_SIZE` immédiat.
- [ ] Un PDF de `N > maxRasterizedPages` pages → `OCR_UNSUPPORTED_SIZE`.
- [ ] Un PDF de taille `≥ maxRasterizedSizeMb` → `OCR_UNSUPPORTED_SIZE`.
- [ ] Un PDF `≤ maxPages` (11) et `< maxSizeMb` (5 Mo) → `callTextractDirect` (voie directe, non-régression).
- [ ] Un PDF `> maxSizeMb` mais `< maxRasterizedSizeMb` et `≤ maxRasterizedPages` → routé vers `callTextractRasterized`.
- [ ] Le gate quota OCR reste appliqué : un scan multi-pages qui dépasse le quota → `OCR_QUOTA_EXCEEDED`, sans appel Textract.
- [ ] Le fallback `UnsupportedDocumentException` → `callTextractRasterized` de la voie directe (SF-122-08) reste fonctionnel.
- [ ] `OcrProperties` expose `maxRasterizedPages` (défaut 200) et `maxRasterizedSizeMb` (défaut 50) avec valeurs de repli si non configurées.

---

## Périmètre

### Hors scope (explicite)

- API Textract **asynchrone** (`StartDocumentAnalysis` / `GetDocumentAnalysis`) — la voie rasterisée sync couvre le besoin (bordereaux de quelques dizaines de pages).
- Chemin OCR des **images natives** (`extractFromImage`) — inchangé.
- `ExtractionService` — inchangé (consomme déjà `OcrResult.successRasterized`).
- Frontend — inchangé (le badge « OCR en cours » couvre déjà la latence accrue).
- Packs / quota / affichage OCR (SF-122-10/11/12) — inchangés.

---

## Contraintes de validation

Aucun champ utilisateur. SF-122-13 modifie une logique de routage interne. Les deux nouveaux seuils de configuration :

| Propriété | Défaut | Repli si ≤ 0 | Rôle |
|---|---|---|---|
| `maxRasterizedPages` | 200 | 200 | Borne haute du nombre de pages traitables par rasterisation |
| `maxRasterizedSizeMb` | 50 | 50 | Borne haute de taille du PDF traitable par rasterisation |

---

## Technique

### Endpoint(s)

Aucun.

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable.

### Composants impactés

- `OcrProperties` (record `@ConfigurationProperties`) — ajout de `maxRasterizedPages` et `maxRasterizedSizeMb` + valeurs de repli dans le constructeur compact.
- `OcrService.tryOcr` — logique de routage révisée (seuils rasterisés + aiguillage direct/rasterisé). `callTextractRasterized`, `callTextractDirect`, `countPdfPages`, `callTextractThrottled` — inchangés.

### Composants Angular

Aucun.

---

## Plan de test

### Tests unitaires (`OcrServiceTest`)

- [ ] PDF `11 < N ≤ maxRasterizedPages` (seuils de test réduits) → la voie rasterisée est empruntée (Textract appelé page par page), résultat `successRasterized`.
- [ ] PDF `N > maxRasterizedPages` → `OCR_UNSUPPORTED_SIZE`, aucun appel Textract.
- [ ] PDF de taille `≥ maxRasterizedSizeMb` → `OCR_UNSUPPORTED_SIZE`, aucun appel Textract.
- [ ] PDF `≤ maxPages` et `< maxSizeMb` → voie directe (`callTextractDirect`), `OcrResult.success` non rasterisé — non-régression.
- [ ] Quota OCR dépassé sur un PDF multi-pages → `OCR_QUOTA_EXCEEDED`.
- [ ] `OcrProperties` — valeurs de repli appliquées quand `maxRasterizedPages`/`maxRasterizedSizeMb` ≤ 0.
- [ ] Non-régression : fallback `UnsupportedDocumentException` de la voie directe → rasterisée toujours opérant.

### Tests d'intégration

- [x] Non applicable — `OcrService` est un service interne sans endpoint ; couverture par tests unitaires avec `TextractClient` mocké et fixtures PDF (réutilise le harnais de test SF-122-08).

### Isolation workspace

- [x] Non applicable — `tryOcr` reçoit un `workspaceId` déjà résolu (utilisé tel quel pour le gate quota) ; aucun nouvel accès cross-workspace.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Plans / limites** — le gate quota OCR (`isOcrQuotaExceeded`) est **conservé sans modification**. Un PDF multi-pages consomme `pdfPages` pages de quota (×3 en mode forms), exactement comme un scan court aujourd'hui. Pas de nouveau gate, pas de changement de barème. Aucun autre composant `PlanLimitService` touché.
- [x] Auth / Principal, Workspace context, Navigation / routing — non touchés.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|---|---|---|
| `OcrService.tryOcr` — voie directe | Le routage révisé ne doit pas dévier un PDF court de la voie directe | Test : PDF ≤ 11 pages < 5 Mo → `callTextractDirect`, résultat non rasterisé |
| `ExtractionService` (consommateur de `tryOcr`) | Consomme déjà `successRasterized` (SF-122-08) — aucune modification | Couverture indirecte par les tests `OcrService` |

### Smoke tests E2E concernés

- [x] Aucun — pas de route, pas d'auth, pas de workspace context modifié. Périmètre worker backend isolé.

---

## Dépendances

### Subfeatures bloquantes

- SF-122-08 (rasterisation page-par-page) — `done`. SF-122-13 réutilise sa voie `callTextractRasterized`.

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

- **Réutilisation, pas réinvention** : la voie de rasterisation existe (SF-122-08) et est déjà capable du multi-pages. SF-122-13 est essentiellement du routage + 2 seuils de configuration. C'est l'invariant anti-gadget central de l'étape 0.
- **Pas d'API async** : le commentaire de classe de `OcrService` évoquait l'async (`StartDocumentAnalysis`) comme « itération future ». SF-122-13 rend cette itération inutile pour le besoin courant — la voie rasterisée sync, bornée à `maxRasterizedPages`, couvre les bordereaux de pièces réels. L'async resterait pertinent uniquement pour des PDF de plusieurs centaines de pages, hors besoin avéré.
- **Borne haute** : `maxRasterizedPages = 200` borne le coût Textract et la latence (200 pages ≈ 200 appels sync throttlés à 3 simultanés). Au-delà, `OCR_UNSUPPORTED_SIZE` reste rendu — l'avocat voit alors le message F-121-06 « divisez le fichier ».
