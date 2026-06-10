# Mini-spec — F-265 / SF-265-01 — Backend : régénération d'une section avec instruction IA

> Programme Conclusions V2 / F-265 (levier UX n°2). Backend. Étape 0 GO avec ajustements (Option B — régénération de section markdown in-place). **Contrat API figé ci-dessous** → frontend (SF-265-02) parallélisable.

## Identifiant
`F-265 / SF-265-01`

## Statut
`ready` (étape 0 GO ; pas d'étape 0 bis — backend pur, aucun écran nouveau).

## Branche
`feat/SF-265-01-backend-regeneration-section`

## Objectif
> Exposer un endpoint **synchrone** qui régénère le markdown d'**une seule section** d'une version de conclusions, selon une **instruction libre** de l'avocat, en réutilisant le contexte dossier et les gardes du pipeline de génération (F-98), sans toucher au stockage (markdown) ni aux autres sections.

## Contrat API (figé)

```
POST /api/v1/case-files/{caseFileId}/conclusions/versions/{versionId}/sections/regenerate
Body: { "sectionMarkdown": "<markdown de la section sélectionnée>", "instruction": "<consigne libre>" }
200 → { "regeneratedMarkdown": "<markdown régénéré de la section>" }
400 → sectionMarkdown vide/absent OU instruction vide/absente OU instruction > 2000 car.
409 → version non DONE (CONTENT_REQUIRES_DONE) OU version non DRAFT (CONTENT_NOT_EDITABLE)
404 → dossier / version inconnu ou autre workspace
401 → non authentifié
```

- **Synchrone** (une section ≈ court ; pas d'async/RabbitMQ). `maxTokens` borné (2000).
- **Gate coût** : `AiCallContext.userLevel(workspaceId, userId, caseFileId, JobType.CONCLUSION_SECTION_REGEN)` — user-level → gate token user + record usage_events (workspaceId/userId/caseFileId non null).

## Comportement attendu

### Cas nominal
1. L'avocat (frontend) sélectionne une section + saisit une instruction → `POST …/sections/regenerate`.
2. Le service vérifie l'isolation workspace, charge la version, applique les gardes DONE + DRAFT (mêmes que `updateContent`).
3. Il construit le **system prompt de la combinaison** via `CaseConclusionPromptBuilder.buildSystemPrompt(key, styleSignatures)` — **mêmes gardes rédactionnelles** (anti-jargon, chiffres tracés, jurisprudence vérifiée, style cabinet) que la génération complète.
4. Il construit un **user message scopé** ancré sur l'**acte courant** (le `content` markdown déjà persisté de la version) : « Voici l'acte complet en cours (contexte). Régénère UNIQUEMENT la section délimitée ci-dessous selon l'instruction, en respectant toutes les consignes du system prompt (chiffres tracés, jurisprudence vérifiée, pas de jargon interne, markdown valide). Ne réécris pas le reste de l'acte. Acte courant : <content>. Section à régénérer : <sectionMarkdown>. Instruction : <instruction>. Réponds UNIQUEMENT le markdown de la section régénérée, sans préambule ni titre supplémentaire. »
5. Appel `anthropicService.analyze(ctx, systemPrompt, userMessage, 2000)`.

> **Arbitrage RÉVERSIBLE (flaggé)** : le contexte de grounding est l'**acte courant déjà généré** (qui contient déjà faits/pièces/chiffres/jurisprudence intégrés par la génération initiale) + le system prompt porteur des gardes — **PAS** un ré-assemblage des repos analyse/pièces/outils/jurisprudence. Raison : DRY (pas de duplication des ~160 lignes de `prepare`, fragiles), et l'acte courant est une source de grounding suffisante et fidèle pour une retouche de section. Enrichissement possible plus tard (ré-injecter le contexte brut) si signal terrain — réversible, aucun impact stockage/contrat.
6. Réponse → trim → `{ regeneratedMarkdown }`. **Aucune persistance** : le frontend décide d'insérer puis l'avocat enregistre via `PATCH …/content` (round-trip markdown existant).

### Cas d'erreur / bords
| Situation | Comportement |
|---|---|
| `sectionMarkdown` ou `instruction` vide/blank | `400` |
| `instruction` > 2000 caractères | `400` (borne anti-abus) |
| version non `DONE` | `409 CONTENT_REQUIRES_DONE` |
| version `VALIDATED`/`DEPOSITED` (non DRAFT) | `409 CONTENT_NOT_EDITABLE` |
| dossier/version autre workspace | `404` (pas de fuite d'existence) |
| réponse IA vide | `502` (échec génération, message générique) |
| budget token dépassé | propagé par le gate (comportement F-257 existant) |

## Gardes / invariants
- **Markdown-safe** : la consigne impose un markdown valide ; pas de bordereau ni jurisprudence globale réinjectés (hors périmètre section).
- **Pas d'invention** : contexte dossier identique à la génération ; l'instruction oriente le style/l'angle, jamais les faits/chiffres. Gardes SF-98-55 (anti-jargon, chiffres, jurisprudence) embarquées dans la consigne.
- **DONE + DRAFT only** : symétrie stricte avec `updateContent`.
- **Aucune persistance** : l'endpoint est pur (lecture + appel IA). L'enregistrement passe par l'endpoint `content` existant → versions/export/cycle de vie intacts.

## Tables / endpoints / composants impactés
- **Nouveau** : `SectionRegenerationRequest` (record), `SectionRegenerationResponse` (record), endpoint controller, méthode service `regenerateSection`, `JobType.CONCLUSION_SECTION_REGEN`.
- **Refactor léger** : extraire l'assemblage de `ConclusionPromptInput` de `CaseConclusionService.prepare` vers une méthode/clé réutilisable **sans** changer le comportement de génération (l'assembleur reste dans le service `@Profile`, mais la **régénération synchrone** vit dans un service non profilé pour être testable et appelable hors RabbitMQ). Décision : créer `ConclusionSectionRegenerationService` (non profilé) qui réutilise `CaseConclusionPromptBuilder` + repos déjà injectés ailleurs. Le `CaseConclusionService` (worker) reste inchangé.
- **Aucune migration de schéma** (pas de nouvelle table). `JobType` : enum applicatif (pas de contrainte DB sur le nom ; usage_events varchar40 depuis SF-257-02 — `CONCLUSION_SECTION_REGEN` = 27 car., OK).
- **Inchangé** : stockage `content`, export, versions, cycle de vie, bordereau.

## Hors périmètre
- Persistance d'un modèle de bloc (Option A) — backlog.
- Frontend (SF-265-02).
- Régénération multi-sections / réordonnancement / suppression de bloc (la mini-spec UX cadrera ce qui est inclus côté front ; backend ne fait que régénérer une section).

## Plan de test (JUnit + IT)
- **IT** `POST …/sections/regenerate` :
  - nominal `200` + `regeneratedMarkdown` non vide (Anthropic mocké).
  - `400` sectionMarkdown vide / instruction vide / instruction trop longue.
  - `409` version non DONE ; `409` version non DRAFT.
  - `404` version d'un autre workspace (isolation).
  - `401` non authentifié.
- **Unit** service : la consigne embarque le `sectionMarkdown` + `instruction` + gardes ; `AiCallContext` est user-level avec workspace/user/case non null (record usage OK) ; réponse vide → `502`.
- **Non-régression** : aucun test existant de génération/versions/export modifié.

## Analyse d'impact transversal
- **Auth/Principal** : réutilise `CurrentUserResolver` (inchangé).
- **Workspace context** : isolation via `resolveCaseFileInWorkspace` (pattern existant).
- **Plans / limites** : ✅ gate token user via `AiCallContext.userLevel` (déclencheur « coût LLM »). JobType user-level enregistré.
- **Navigation / routing** : N/A.
- **Outil décisionnel** : N/A (génération rédactionnelle, pas un outil décisionnel).
- **Smoke E2E** : non requis (pas de changement auth/workspace/navigation) ; couvert par IT.

## Dépendances
- F-98 (pipeline, prompt builder, contexte), F-257 (gate AiCallContext), SF-98-49 (endpoint content pour l'enregistrement). Tous `done`.
