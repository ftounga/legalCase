# Mini-spec — F-35 / SF-35-01 Chat libre — backend

## Identifiant
`F-35 / SF-35-01`

## Feature parente
`F-35` — Chat libre sur dossier

## Statut
`ready`

## Date de création
2026-03-22

## Branche Git
`feat/SF-35-01-chat-backend`

---

## Objectif

Permettre à l'avocat de poser des questions libres sur un dossier et obtenir
une réponse Anthropic basée sur la synthèse du dossier (context = CaseAnalysis).

---

## Comportement attendu

### Modèle adaptatif
- `useEnriched = false` (défaut) → Haiku (rapide, factuel)
- `useEnriched = true` → Sonnet (plus nuancé)
- Si `useEnriched = true` mais plan non PRO → fallback Haiku silencieux

### Contexte transmis à Anthropic
- `CaseAnalysis.analysisResult` (synthèse JSON du dossier, statut DONE)
- Si pas de synthèse DONE → HTTP 424

### Limites mensuelles par plan
| Plan | Messages/mois |
|------|--------------|
| FREE | 10 |
| STARTER | 50 |
| PRO | 200 |
| Sans souscription | Illimité (fail open) |

### Cas d'erreur
| Situation | HTTP | Message |
|-----------|------|---------|
| Limite atteinte | 402 | "Limite de messages chat atteinte." |
| Pas de synthèse DONE | 424 | "Aucune synthèse disponible pour ce dossier." |
| Dossier non trouvé / hors workspace | 404 | — |

---

## Critères d'acceptation

- [ ] POST /api/v1/case-files/:id/chat crée un message et retourne la réponse
- [ ] GET /api/v1/case-files/:id/chat retourne l'historique du dossier (tri createdAt DESC)
- [ ] Modèle : Haiku par défaut, Sonnet si useEnriched=true ET plan PRO
- [ ] Limite mensuelle vérifiée avant appel Anthropic (402)
- [ ] Usage enregistré dans usage_events (JobType.CHAT_MESSAGE)
- [ ] Accès vérifié : user membre du workspace du dossier

---

## Technique

### Migration Liquibase
- [x] Applicable — nouvelle table `chat_messages`

```sql
CREATE TABLE chat_messages (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  case_file_id UUID NOT NULL REFERENCES case_files(id) ON DELETE CASCADE,
  user_id UUID NOT NULL REFERENCES users(id),
  question TEXT NOT NULL,
  answer TEXT,
  model_used VARCHAR(100),
  use_enriched BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_chat_messages_case_file_id ON chat_messages(case_file_id);
```

### Composants backend nouveaux / modifiés
- `JobType` — ajout `CHAT_MESSAGE`
- `ChatMessage` entity + `ChatMessageRepository`
- `ChatMessageRequest` DTO (question, useEnriched)
- `ChatMessageResponse` DTO (id, question, answer, modelUsed, useEnriched, createdAt)
- `ChatService` — logique métier (gate, contexte, Anthropic, save, usage)
- `ChatController` — POST /api/v1/case-files/:id/chat + GET
- `PlanLimitService` — `isChatMessageLimitReached(UUID workspaceId)` + constantes FREE/STARTER/PRO

### Note architecture — synchronisme
Le chat est **synchrone** (réponse directe dans le corps HTTP).
Exception justifiée à la règle async du pipeline : le chat est conversationnel,
pas un pipeline de traitement documentaire. Le timeout HTTP est acceptable (< 30s Haiku, < 60s Sonnet).

---

## Plan de test

- [ ] U-01 : limite non atteinte → message créé, réponse retournée
- [ ] U-02 : limite atteinte → 402
- [ ] U-03 : pas de synthèse → 424
- [ ] U-04 : useEnriched=true + PRO → modèle Sonnet
- [ ] U-05 : useEnriched=true + STARTER → modèle Haiku (fallback silencieux)
- [ ] U-06 : sans souscription → autorisé (fail open)

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Plans / limites** — nouveau gate dans ChatService + PlanLimitService

### Composants / endpoints existants potentiellement impactés
| Composant | Impact | Test |
|-----------|--------|------|
| `PlanLimitService` | Nouvelles constantes + méthode | U-01/U-02/U-06 |
| `JobType` | Nouvelle valeur CHAT_MESSAGE | Usage recording |

---

## Dépendances
- SF-34-01 — done ✓
