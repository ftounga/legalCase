# Mini-spec — F-145 / SF-145-01 Identification des pièces (backend)

## Identifiant · `F-145 / SF-145-01`
## Date · `2026-04-21` · Branche · `feat/SF-145-01-document-pieces-backend`

## Objectif
Détecter automatiquement les **pièces distinctes** à l'intérieur d'un document uploadé et les persister. Débloque SF-145-02 (frontend navigable) et F-146 (source précise universelle).

## Contexte
Aujourd'hui un upload = un document, et un document est considéré comme une pièce unique dans tout le pipeline aval (chunking, analyse, source F-IA-03). Réalité terrain : un avocat scanne souvent plusieurs pièces en une passe (contrat + CNI + SMS + attestation…) et l'upload résulte en un seul PDF composite. Sans détection interne, impossible de citer précisément la preuve.

## Comportement nominal
1. Extraction `DONE` → event `ExtractionDoneEvent` émis par `ExtractionService` (déjà en place, utilisé par `ChunkingService`).
2. Nouveau service `DocumentPieceDetectionService` écoute l'event **en parallèle du chunking** (pattern identique).
3. Récupère `documentId`, `extractedText`, `pageCount` (via metadata OCR si dispo, sinon via PDFBox).
4. Appelle Haiku (`anthropicService.analyzeFast`) avec un prompt dédié qui retourne JSON array `[{type, label, pageStart, pageEnd, orderIndex}]`.
5. Parse et persiste dans nouvelle table `document_pieces`.
6. En cas d'échec (Haiku down, JSON invalide) : fail-open — persiste 1 seule pièce "AUTRE" couvrant tout le document (aucun blocage du pipeline aval).

## Prompt Haiku
Système :
```
Tu identifies les pièces juridiques distinctes présentes dans un document
composite. Un document peut contenir plusieurs pièces scannées en une
passe (ex: contrat + CNI + SMS + attestation). Tu dois détecter les
ruptures logiques et lister chaque pièce avec son type parmi cette
liste exacte :
- CONTRAT : contrat de travail, avenant, convention
- PIECE_IDENTITE : CNI, passeport, titre de séjour
- SMS : échanges SMS/messagerie
- EMAIL : emails imprimés
- ATTESTATION : attestation manuscrite, témoignage
- BULLETIN_PAIE : bulletins de paie
- LETTRE : courrier formel (licenciement, mise en demeure…)
- PHOTO : photos non-textuelles
- AUTRE : tout le reste

Pour chaque pièce, fournir :
- type (enum ci-dessus)
- label : description courte contextuelle (ex: "Contrat de travail Dupont",
  "Attestation collègue 1")
- pageStart, pageEnd : pages dans le document (1-indexed)
- orderIndex : ordre séquentiel dans le document (0-indexed)

Si le document est unitaire (une seule pièce), retourner 1 entrée.

Réponds UNIQUEMENT avec un tableau JSON valide, sans texte avant ni après.
Format : [{"type":"...","label":"...","pageStart":N,"pageEnd":N,"orderIndex":N}]
```

Utilisateur : le texte extrait (tronqué à ~50k chars pour rester dans le budget Haiku, headers/footers préservés).

## Critères d'acceptation
- [ ] Migration Liquibase 097 : table `document_pieces` avec colonnes `id UUID PK`, `document_id UUID FK`, `type VARCHAR(30) NOT NULL`, `label VARCHAR(500)`, `page_start INT NOT NULL`, `page_end INT NOT NULL`, `order_index INT NOT NULL`, `created_at`, `updated_at`, index sur `document_id`
- [ ] Entity `DocumentPiece` + repository `DocumentPieceRepository` (findByDocumentIdOrderByOrderIndex)
- [ ] Enum `DocumentPieceType` (9 valeurs)
- [ ] `DocumentPieceDetectionService` : `@EventListener` sur `ExtractionDoneEvent`, appelle Haiku, parse, persiste
- [ ] Prompt dédié `DOCUMENT_PIECES_SYSTEM_PROMPT` dans la classe
- [ ] Fail-open : exception Haiku ou JSON invalide → 1 pièce `AUTRE` pageStart=1, pageEnd=pageCount, label="Document complet"
- [ ] Dédup idempotente : si déjà des pièces pour ce document (ex: rejeu), on les supprime avant d'en réinsérer
- [ ] DTO `DocumentResponse` enrichi : `pieces: List<DocumentPieceSummary>` (type, label, pageStart, pageEnd, orderIndex)
- [ ] Tests U-01 : parseur JSON Haiku avec 2 pièces → 2 entrées persistées
- [ ] Tests U-02 : Haiku jette exception → 1 entrée fallback `AUTRE`
- [ ] Tests U-03 : JSON invalide → 1 entrée fallback
- [ ] Tests U-04 : rejeu sur un document déjà traité → pas de doublons (delete before insert)
- [ ] Tests IT : après extraction DONE, la table `document_pieces` contient au moins 1 entrée

## Tables / endpoints / composants impactés
### Backend
- Migration 097 (nouvelle table)
- `DocumentPiece.java` (entity)
- `DocumentPieceRepository.java`
- `DocumentPieceType.java` (enum)
- `DocumentPieceSummary.java` (DTO readonly)
- `DocumentPieceDetectionService.java`
- `DocumentResponse.java` (+ field `pieces`)
- `DocumentService.toResponse` (alimente le champ)

### Pas impacté
- `ExtractionService` : inchangé, continue d'émettre `ExtractionDoneEvent`
- `ChunkingService` : inchangé, écoute le même event
- `AnthropicService` : inchangé, méthode `analyzeFast` réutilisée

## Analyse de cohérence transversale
| Cible | Évaluation | Classement |
|-------|-----------|------------|
| F-122 OCR | Les pièces sont détectées après extraction réussie, qu'elle vienne de PDFBox ou Textract | Intégré |
| F-121 polling extractions | Les pièces apparaissent asynchrone après `DONE` — le frontend pollera à nouveau pour les récupérer | Intégré |
| F-IA-03 source précise | Les pièces sont une couche supplémentaire préalable à l'enrichissement de la source (F-146) | SF parallèle (F-146 future) |
| F-55/F-56 pipeline IA | Le chunking ne dépend pas des pièces, il reste granulaire au niveau texte | Non applicable |
| Design System | Aucun impact backend | N/A |

### Cas spécifique : nouveau pattern `EventListener` pour détection async
- Pattern déjà utilisé par `ChunkingService.onExtractionDone`. Pas de nouveau pattern transversal introduit.
- `DocumentPieceSummary` est un DTO scoped F-145, pas réutilisé ailleurs pour l'instant. F-146 pourra le consommer sans modification.

## Préoccupations transversales
- **Auth / Principal** : aucun impact (traitement async sans contexte utilisateur, accès DB direct)
- **Workspace context** : les pieces héritent du workspace via `Document.caseFile.workspace` (isolation existante préservée)
- **Plans / limites** : **à évaluer** — la détection des pièces ajoute 1 appel Haiku par extraction DONE. Coût : ~0,002 €/appel × N uploads/mois. Pour un avocat à 50 docs/mois = 0,10 €/mois. **Pas de gate ajoutée** dans cette SF (fail-open et coût marginal).
- **Navigation / routing** : aucun impact

## Hors scope
- Frontend (popup aperçu navigable, chips pièces) → SF-145-02
- Extension de la source F-IA-03 pour citer les pièces → F-146
- Édition manuelle des pièces par l'avocat si l'IA s'est trompée → backlog futur (F-145-03 éventuel retour terrain)
- Retraitement automatique des documents déjà extraits avant cette SF — les nouvelles pièces n'apparaîtront que sur les futurs uploads (les anciens gardent leur comportement actuel = pas de pièces détectées, `pieces: []`)
- Détection fine intra-pièce (ex: distinguer 2 SMS dans une même capture) — scope réduit à la détection de blocs distincts
