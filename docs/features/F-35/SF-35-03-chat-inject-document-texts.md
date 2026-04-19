# Mini-spec — F-35 / SF-35-03 Chat — injecter le texte brut des documents dans le contexte

## Identifiant
`F-35 / SF-35-03`

## Feature parente
`F-35` — Chat libre sur dossier

## Statut `draft`  · Date `2026-04-20`  · Branche `feat/SF-35-03-chat-inject-document-texts`

---

## Objectif

Permettre au chat libre de répondre sur des détails présents dans les documents (chiffres, dates, noms, extraits précis) qui ne remontent pas forcément dans la synthèse. Aujourd'hui le chat ne voit que la synthèse du dossier → il répond "je ne vois rien" sur des infos réellement extraites par OCR/PDFBox.

---

## Comportement

### Contexte actuel (bug)

`ChatService.sendMessage` construit un prompt qui contient uniquement la synthèse :

```java
String userMessage = "Dossier :\n" + caseAnalysis.getAnalysisResult()
    + "\n\nQuestion : " + request.question();
```

Résultat : une question sur "804 €" (présent dans P3 après OCR) renvoie "je ne vois rien".

### Nouveau comportement

Injecter le texte brut extrait de tous les documents DONE du dossier, en plus de la synthèse, dans le prompt Claude.

**Format du prompt** :

```
Synthèse du dossier :
<caseAnalysis.analysisResult>

Documents du dossier :
=== P1 — contrat.pdf (extraction classique, 12 345 car.) ===
<texte extrait>

=== P3 — scan.pdf (OCR, 45 678 car.) ===
<texte extrait>

...

Question : <user question>
```

### Budget tokens et troncature

- **Budget total** : 150 000 caractères (≈ 37 500 tokens pour Claude, marge confortable sur 200K context)
- **Tri** : documents les plus récents en premier (par `uploadedAt` DESC) — hypothèse que les documents récents sont souvent les plus pertinents pour la question du moment
- **Troncature** : si dépassement, couper au document en cours et ajouter "… [N autres documents non inclus par limite de taille]"
- **Exclusion** : documents FAILED, PENDING, PROCESSING ignorés (pas de texte utile)

### Cas limite

- Dossier sans aucun document DONE → comportement actuel (seulement synthèse)
- Document avec `extractedText = null` ou vide → ignoré silencieusement
- Question posée pendant qu'un document est encore en cours d'extraction → seul les DONE disponibles sont injectés, message chat normal

---

## Critères d'acceptation

- [ ] Une question sur un chiffre/date/nom présent uniquement dans le texte brut d'un document (hors synthèse) reçoit une réponse correcte qui cite ce document
- [ ] Le prompt envoyé à Claude contient explicitement le texte des documents, avec nom de fichier et méthode d'extraction
- [ ] Budget total tronqué à 150 000 caractères, message ajouté si des docs sont exclus
- [ ] Documents FAILED / non-DONE exclus du contexte
- [ ] Aucune régression sur les cas existants (question globale sur la synthèse reste précise)
- [ ] Tokens input × ~10 vs comportement actuel, mais dans le budget 200K Claude → pas d'erreur API

---

## Plan de test

### Unitaires backend
- `ChatServiceTest` :
  - Nouveau test — message inclut texte d'un document (mock `caseFileRepository` + `documentRepository` + `extractionRepository`) → `AnthropicService` reçoit un prompt contenant le texte du doc
  - Nouveau test — documents FAILED exclus du prompt
  - Nouveau test — budget 150K respecté avec troncature et message explicite
  - Nouveau test — cas aucun document DONE → fallback sur synthèse seule (rétrocompat)

### Intégration manuelle staging
- Uploader un doc scanné avec un chiffre précis (ex. 804 €)
- Lancer une analyse dossier
- Une fois DONE, poser la question "Est-ce que le montant 804 te parle dans les documents ?"
- Vérifier que Claude répond en citant le document

### Isolation workspace
- Déjà couverte par le check workspace existant dans `ChatService.sendMessage`

---

## Tables / endpoints / composants impactés

### Backend
- `ChatService.java` — méthode `buildUserMessage(caseAnalysis, caseFileId)` nouvelle, injecte le texte brut
- `DocumentRepository` — nouvelle méthode `findByCaseFileIdOrderByCreatedAtDesc` si pas existante
- `DocumentExtractionRepository` — déjà `findByDocumentIdIn` existe
- `ChatServiceTest` — 3-4 nouveaux tests

### Frontend
- Aucun changement — le contrat API reste identique

### DB / migration
- Aucun changement

---

## Hors périmètre

- RAG / embeddings (option B rejetée par l'utilisateur pour V1)
- Sélection intelligente de documents pertinents — simple tri par date DESC + troncature
- Indication UI que des documents ont été tronqués — le message apparaît dans la réponse Claude si besoin
- Cache du texte concaténé par dossier — pas d'optimisation précoce, recomputé à chaque message

---

## Analyse de cohérence transversale

| Cible | Applicable | Classement |
|---|---|---|
| Autres pays (Belgique) | Oui | **Intégrée** — aucune spécificité pays |
| Autres domaines | Oui | **Intégrée** — texte extrait workspace-level |
| Autres endpoints chat | Non applicable | un seul endpoint chat case-file |
| Chat d'aide produit (F-104) | Non applicable | Utilise un corpus statique `.md`, pas des docs user |

**Analyse d'impact cross-cutting** :
- [ ] Auth / Principal — non touché (endpoint existant)
- [ ] Workspace context — non touché (check existant dans sendMessage)
- [x] **Plans / limites** — côté quota tokens : le coût par message chat va augmenter ~10×. Impact direct sur le compteur `monthlyTokensUsed`. Les plans incluent déjà un budget tokens mensuel qui absorbe ce surcoût — aucun changement de limite nécessaire, mais à monitorer après release (Sentry + métriques). Composants concernés : aucun, le tracking tokens existant est générique.
- [ ] Navigation / routing — non touché

Aucun smoke E2E concerné.

---

## Nouveau pattern UI ou service partagé

- [x] Pas de nouveau pattern UI
- [x] Pas de service partagé nouveau — modification locale à `ChatService`
