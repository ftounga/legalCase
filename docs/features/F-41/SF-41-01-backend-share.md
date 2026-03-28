---
id: SF-41-01
feature: F-41
title: Backend — table case_file_shares + endpoints partage
status: In Progress
---

## Objectif

Permettre à un avocat de générer un lien temporaire signé donnant accès en lecture seule à la synthèse d'un dossier, sans authentification côté visiteur.

## Table case_file_shares (migration 034)

```
id           UUID PK
case_file_id UUID FK case_files NOT NULL
token        VARCHAR(64) UNIQUE NOT NULL  -- SecureRandom hex 64 chars
expires_at   TIMESTAMP WITH TIME ZONE NOT NULL
created_by   UUID FK users NOT NULL
created_at   TIMESTAMP WITH TIME ZONE NOT NULL
revoked_at   TIMESTAMP WITH TIME ZONE NULL
```

## Endpoints

| Méthode | URL | Auth | Description |
|---------|-----|------|-------------|
| POST | /api/v1/case-files/{id}/shares | Membre workspace | Crée un lien. Body: { "expiresInDays": 7 } |
| GET | /api/v1/case-files/{id}/shares | Membre workspace | Liste les liens actifs |
| DELETE | /api/v1/case-files/{id}/shares/{shareId} | Membre workspace | Révoque un lien |
| GET | /api/v1/public/shares/{token} | Aucune (permitAll) | Retourne titre + domaine + dernière synthèse DONE |

## Comportement nominal

- Token = 32 octets SecureRandom → hex 64 chars
- expiresInDays entre 1 et 30 (@Min(1) @Max(30))
- Endpoint public permitAll() dans SecurityConfig
- Endpoint public retourne la synthèse la plus récente DONE (STANDARD ou ENRICHED)
- Isolation workspace : POST/GET/DELETE vérifient appartenance au workspace du membre

## Cas d'erreur

- Token expiré ou révoqué → 404
- expiresInDays hors bornes → 400
- Dossier supprimé (deletedAt non null) → 404 sur endpoint public

## Critères d'acceptation

1. POST crée un share avec token unique 64 chars et expiration correcte
2. GET liste uniquement les shares actifs (non expirés ET non révoqués)
3. DELETE pose revoked_at, token inactif immédiatement
4. Endpoint public 200 + synthèse si token valide, 404 sinon
5. Endpoint public accessible sans JWT
6. Isolation workspace : membre workspace B ne peut pas partager dossier workspace A

## Plan de test

- T-01 : POST → token 64 chars hex, expiresAt = now + expiresInDays
- T-02 : POST expiresInDays=0 → 400
- T-03 : GET public token valide → 200 + titre + synthèse
- T-04 : GET public token expiré → 404
- T-05 : GET public token révoqué → 404
- T-06 : DELETE → revoked_at set, GET public → 404
- T-07 : GET /case-files/{id}/shares → liste uniquement actifs
- T-08 : Isolation workspace

## Composants impactés

- Nouvelle entité : CaseFileShare
- Nouveau repository : CaseFileShareRepository
- Nouveau service : CaseFileShareService
- Nouveau controller : CaseFileShareController
- SecurityConfig : permitAll sur /api/v1/public/shares/**
- Migration 034

## Hors périmètre

- UI (SF-41-02)
- Notifications d'expiration
- Stats de consultation
