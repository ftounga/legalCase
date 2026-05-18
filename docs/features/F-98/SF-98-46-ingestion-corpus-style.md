# Mini-spec — F-98 / SF-98-46 — Ingestion du corpus de style

> Cadrages amont : `SF-98-46-00-coherence.md` (étape 0, GO avec ajustements — minimisation RGPD) + `SF-98-46-00b-ux-coherence.md` (étape 0 bis, GO avec ajustements — écran cabinet dédié).

## Identifiant
`F-98 / SF-98-46`

## Feature parente
`F-98` — Génération de courrier / conclusions (bloc style learning)

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-46-backend-corpus-style` — **SF backend pure** (le frontend de l'écran est livré par SF-98-48).

---

## Objectif
Permettre à l'avocat de téléverser ses anciennes conclusions au niveau de son cabinet ; le backend en extrait un **profil de style** rédactionnel (sans données client) et **purge le contenu source**.

---

## Comportement attendu

### Cas nominal
1. L'avocat téléverse un document (conclusion de référence) via `POST .../style-corpus/documents`.
2. Le backend crée une ligne `style_corpus_documents` (statut `PENDING`), stocke temporairement le fichier (S3), publie un message RabbitMQ.
3. Le worker asynchrone : extrait le texte (réutilise `ExtractionService`), appelle l'IA pour produire une **signature de style** — une description réutilisable du style rédactionnel **sans aucun fait, nom, montant ni donnée de dossier** — puis :
   - persiste la `style_signature` + statut `DONE` ;
   - **supprime le fichier S3 et ne persiste jamais le texte brut extrait** (minimisation RGPD, invariant 1 du cadrage).
4. Le document est `active = true` par défaut : il contribue au profil de style du cabinet.
5. L'avocat peut lister, désactiver/réactiver, supprimer ses documents de corpus.

### Cas d'erreur
| Situation | Comportement | Code |
|---|---|---|
| Fichier absent / type non supporté (hors `.pdf/.doc/.docx/.txt`) | Rejet | 400 |
| Fichier trop volumineux (> 50 Mo) | Rejet | 400 |
| `workspaceId` ≠ workspace de l'utilisateur | Accès refusé | 404 |
| Document inexistant (DELETE/PATCH) ou autre workspace | Accès refusé | 404 |
| Échec extraction texte ou appel IA | Statut `FAILED` + `errorMessage` ; le fichier S3 est tout de même purgé | 202 au déclenchement |
| Non authentifié | Rejet | 401 |

---

## Analyse de cohérence transversale
- [x] **Pattern asynchrone IA** : réutilise le pattern RabbitMQ + worker de SF-98-01 (`CaseConclusionService`) et `AnthropicService`.
- [x] **Upload + extraction** : réutilise `ExtractionService` (lecture .docx/.pdf/.txt) et `StorageService` (S3). Différence : upload **au niveau workspace**, pas dossier.
- [x] **Entité de niveau workspace** : modèle `UserBillingRate` (FK workspace) comme référence.
- [x] **Consommée par** : SF-98-47 (lecture des signatures actives) et SF-98-48 (écran). Contrat API figé ci-dessous.

### Décision
- [x] Périmètre limité à l'ingestion backend ; SF-98-47 et SF-98-48 sont des SF distinctes planifiées.

## Conformité F-IA-04
- [x] **Non applicable** — SF backend pure, pas de composant décisionnel.

---

## Critères d'acceptation
- [ ] **CA1** — `POST .../style-corpus/documents` (multipart) crée une ligne `style_corpus_documents` au statut `PENDING`, `active=true`, et déclenche le traitement asynchrone → `202`.
- [ ] **CA2** — Le worker extrait le texte, appelle l'IA, persiste une `style_signature` non vide, statut `DONE`.
- [ ] **CA3** — Après traitement (succès **ou** échec), le fichier S3 est supprimé et aucun texte brut n'est persisté (seule la `style_signature` subsiste). Vérifiable par test.
- [ ] **CA4** — `GET .../style-corpus/documents` liste les documents du workspace (filename, statut, active, date, errorMessage).
- [ ] **CA5** — `PATCH .../style-corpus/documents/{id}` `{active}` active/désactive un document.
- [ ] **CA6** — `DELETE .../style-corpus/documents/{id}` supprime la ligne → `204`.
- [ ] **CA7** — Échec d'extraction/IA → statut `FAILED` + `errorMessage` ; fichier S3 tout de même purgé (CA3).
- [ ] **CA8** — Isolation workspace : `404` sur les 4 routes pour un `workspaceId`/document d'un autre workspace.
- [ ] **CA9** — Validation : type de fichier non supporté ou taille > 50 Mo → `400`.

---

## Périmètre
### Hors scope
- Injection du style dans la génération — **SF-98-47**.
- Écran de gestion du corpus (frontend) — **SF-98-48**.
- Anonymisation fine du texte source (pseudonymisation par NER) — non retenu : la signature de style est produite *sans* reprendre les données, l'extraction-puis-purge suffit à la minimisation.
- Versionnement / historique des signatures.

---

## Valeurs initiales
| Champ | Valeur initiale | Règle |
|---|---|---|
| `status` | `PENDING` | à la création |
| `active` | `true` | un document ingéré contribue au profil par défaut |
| `style_signature`, `error_message` | `null` | renseignés par le worker |

---

## Technique

### Contrat API (FIGÉ — parallélisation avec SF-98-48)
| Méthode | URL | Réponses |
|---|---|---|
| POST | `/api/v1/workspaces/{workspaceId}/style-corpus/documents` (multipart `file`) | `202 {"id":UUID,"status":"PENDING"}` ; `400` ; `404` ; `401` |
| GET | `/api/v1/workspaces/{workspaceId}/style-corpus/documents` | `200 StyleCorpusDocumentSummary[]` ; `404` ; `401` |
| PATCH | `/api/v1/workspaces/{workspaceId}/style-corpus/documents/{id}` body `{"active":bool}` | `200 StyleCorpusDocumentSummary` ; `404` ; `401` |
| DELETE | `/api/v1/workspaces/{workspaceId}/style-corpus/documents/{id}` | `204` ; `404` ; `401` |

`StyleCorpusDocumentSummary` : `{ id, originalFilename, status (PENDING|PROCESSING|DONE|FAILED), active, createdAt, errorMessage }`. **La `style_signature` n'est jamais exposée par l'API** (usage interne SF-98-47).

### Tables impactées
| Table | Opération |
|---|---|
| `style_corpus_documents` | CREATE (nouvelle table) — `id`, `workspace_id` FK, `uploaded_by_user_id` FK, `original_filename`, `content_type`, `file_size`, `status`, `style_signature` TEXT, `active` BOOLEAN, `error_message` TEXT, `created_at`, `updated_at` |

### Migration Liquibase
- [x] Oui — `{NNN}-create-style-corpus-documents.xml` — **vérifier le prochain numéro libre sur `origin/master`** au moment du dev (≥ 237). Rollback : drop table.

### Composants Backend
Package `fr.ailegalcase.stylelearning` : `StyleCorpusDocument` (entité), `StyleCorpusDocumentStatus` (enum), `StyleCorpusRepository`, `StyleCorpusController`, `StyleCorpusCommandService` (upload + validation + RabbitMQ + list/patch/delete, isolation workspace), `StyleCorpusExtractionService` (`@RabbitListener` — extraction texte + appel IA + persistance signature + purge), `StyleCorpusMessage`, `StyleSignaturePromptBuilder`, `StyleCorpusDocumentSummary` DTO, config RabbitMQ queue `style.corpus`.

### Prompt d'extraction de style (`StyleSignaturePromptBuilder`)
System : « Tu reçois une conclusion juridique rédigée par un avocat. Décris uniquement son **style rédactionnel** — structure d'argumentation, formules de transition récurrentes, registre de langue, longueur et rythme des phrases, ton. **Ne reprends aucun fait, aucun nom, aucune date, aucun montant, aucune donnée propre au dossier.** Produis une description réutilisable pour guider la rédaction d'autres conclusions. »

---

## Plan de test
### Backend (UT + IT)
- [ ] `StyleCorpusCommandServiceTest` : upload nominal → ligne `PENDING` + message ; `400` type/taille ; `404` workspace ; patch active ; delete.
- [ ] `StyleCorpusExtractionServiceTest` : worker succès → `DONE` + `style_signature` + **purge S3 vérifiée** ; échec IA → `FAILED` + purge S3.
- [ ] `StyleSignaturePromptBuilderTest` : le prompt système interdit explicitement la reprise des données.
- [ ] `StyleCorpusControllerIT` : les 4 endpoints, `404` isolation workspace, `401`.
### Isolation workspace
- [x] Applicable — testée dans `StyleCorpusControllerIT`.

---

## Analyse d'impact
- [x] **Workspace context** coché — la SF crée des routes `/api/v1/workspaces/{workspaceId}/...`. Composants impactés : aucun existant modifié (routes nouvelles) ; le `workspaceId` de l'URL est contrôlé contre le workspace de l'utilisateur (pattern `WorkspaceController`). Test de non-régression : `StyleCorpusControllerIT` isolation `404`.
- [x] Aucun smoke test E2E concerné (pas de modification d'auth/navigation).

## Dépendances
- SF-98-01 — done (pattern worker IA réutilisé).

## Notes et décisions
- **Minimisation RGPD** (invariant 1 du cadrage) : le fichier S3 et le texte brut sont supprimés après extraction ; seule la `style_signature` (description de style, sans donnée client) est conservée. La purge a lieu **même en cas d'échec**.
- `style_signature` non exposée par l'API : usage strictement interne (SF-98-47).
