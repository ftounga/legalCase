# Mini-spec — F-104 / SF-104-01 Chatbot d'aide produit — backend

## Identifiant
`F-104 / SF-104-01`

## Feature parente
`F-104` — Chatbot d'aide produit intégré

## Statut
`in-progress`

## Date de création
2026-04-02

## Branche Git
`feat/SF-104-01-help-chat-backend`

---

## Objectif

Exposer `POST /api/v1/help/chat` qui répond à une question sur l'utilisation du produit AI LegalCase en s'appuyant sur la documentation produit embarquée dans le classpath (RAG statique via system prompt).

---

## Comportement attendu

### Cas nominal

1. L'utilisateur authentifié envoie `{message: "comment créer un dossier ?"}`.
2. Le backend charge la documentation produit depuis `src/main/resources/help/` (fichiers markdown).
3. Construit un system prompt contenant toute la documentation + instructions de comportement.
4. Appelle Claude Haiku via `AnthropicService.analyzeFast()` (max 512 tokens).
5. Retourne `{answer: "..."}`.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| message absent ou vide | 400 Bad Request | 400 |
| message > 500 chars | 400 Bad Request | 400 |
| Non authentifié | 401 Unauthorized | 401 |
| Anthropic indisponible | 503 avec message générique | 503 |

---

## Critères d'acceptation

- [ ] `POST /api/v1/help/chat` retourne 200 + `{answer}` pour un message valide
- [ ] La réponse se base sur le contenu de la documentation produit
- [ ] message vide → 400
- [ ] message > 500 chars → 400
- [ ] Non authentifié → 401
- [ ] Aucune donnée persistée en base (pas de table, pas de log utilisateur)
- [ ] Utilise `AnthropicService.analyzeFast()` (Haiku)

---

## Périmètre

### Hors scope
- Historique de conversation (chaque appel est indépendant)
- Rate limiting par utilisateur (V1)
- Isolation workspace (la doc produit est publique pour tout utilisateur authentifié)
- Streaming de la réponse

---

## Technique

### Endpoint

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/help/chat` | Oui | Tout utilisateur authentifié |

### Composants

- `HelpDocumentLoader` — charge les fichiers `.md` de `classpath:help/` à l'init, concatène en une string
- `HelpChatService` — construit system prompt + appelle `AnthropicService.analyzeFast()`
- `HelpChatController` — `POST /api/v1/help/chat`
- `HelpChatRequest` — record `{String message}`
- `HelpChatResponse` — record `{String answer}`
- `src/main/resources/help/*.md` — documentation produit

### Tables impactées

Aucune. Pas de migration Liquibase.

### Migration Liquibase
- [x] Non applicable

---

## Plan de test

### Tests unitaires (`HelpChatServiceTest`)

- [ ] U-01 : message valide → `analyzeFast()` appelé avec system prompt contenant la doc
- [ ] U-02 : réponse Anthropic retournée dans `HelpChatResponse.answer`
- [ ] U-03 : Anthropic lance exception → 503 propagé

### Tests d'intégration (`HelpChatControllerIT`)

- [ ] IT-01 : POST sans auth → 401
- [ ] IT-02 : POST message valide super-admin → 200 + champ answer présent
- [ ] IT-03 : POST message vide → 400
- [ ] IT-04 : POST message > 500 chars → 400

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Aucune préoccupation transversale** — nouveau controller/service isolé, aucun composant existant modifié

### Smoke tests E2E concernés
- [x] Aucun smoke test concerné — nouvelle route, pas de modification des chemins existants

---

## Dépendances

### Subfeatures bloquantes
Aucune.

---

## Notes et décisions

- **RAG statique** : la doc produit est petite (~5 pages). On l'inclut entièrement dans le system prompt sans vector DB. Simple, efficace, maintenable.
- **Haiku** : suffisant pour des réponses d'aide produit courtes. Coût négligeable.
- **max_tokens: 512** : les réponses d'aide doivent être concises.
- **Pas d'isolation workspace** : la documentation est la même pour tous les utilisateurs.
