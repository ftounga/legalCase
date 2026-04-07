# Mini-spec — F-IM-05 / SF-IM-05-02 Endpoint questionnaire et résolution du titre

---

## Identifiant

`F-IM-05 / SF-IM-05-02`

## Feature parente

`F-IM-05` — Arbre décisionnel type de titre

## Statut

`draft`

## Date de création

2026-04-07

## Branche Git

`feat/SF-IM-05-02-endpoint-title-decision`

---

## Objectif

Exposer un endpoint REST qui reçoit les critères du questionnaire (pays, nationalité UE, motif, durée, situation familiale), exécute l'arbre de décision, persiste le résultat et retourne la liste des titres recommandés. Plus un endpoint GET pour récupérer une décision existante.

---

## Comportement attendu

### Cas nominal

1. `POST /api/v1/case-files/{caseFileId}/immigration/title-decision` avec body :
   ```json
   {
     "country": "FRANCE",
     "nationaliteUe": false,
     "motif": "TRAVAIL",
     "duree": "LONG_SEJOUR",
     "situationFamiliale": null
   }
   ```
2. Le service vérifie : utilisateur authentifié, membre du workspace du dossier, domaine = DROIT_IMMIGRATION
3. L'arbre de décision est exécuté via `ImmigrationTitleDecisionEngine.resolve()`
4. Le résultat est persisté en table `immigration_title_decisions` (upsert : si une décision existe déjà pour ce dossier, elle est mise à jour)
5. Réponse 200 :
   ```json
   {
     "caseFileId": "...",
     "country": "FRANCE",
     "nationaliteUe": false,
     "motif": "TRAVAIL",
     "duree": "LONG_SEJOUR",
     "situationFamiliale": null,
     "recommendations": [
       {
         "code": "VLS_TS_SALARIE",
         "label": "Visa long séjour valant titre de séjour — Salarié",
         "country": "FRANCE",
         "motif": "TRAVAIL",
         "conditions": "...",
         "pieces": ["..."],
         "delaiMoyenJours": 120
       }
     ]
   }
   ```

6. `GET /api/v1/case-files/{caseFileId}/immigration/title-decision` retourne la dernière décision persistée, ou 404 si aucune décision n'existe.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Utilisateur non authentifié | Redirection login | 401 |
| Dossier inexistant ou autre workspace | "Case file not found" | 404 |
| Domaine != DROIT_IMMIGRATION | "Ce dossier n'est pas un dossier de droit de l'immigration" | 400 |
| Pays non supporté | "Pays non supporté" | 400 |
| Motif inconnu | "Motif inconnu" | 400 |
| Durée invalide | "Durée invalide" | 400 |
| Aucune décision existante (GET) | "Aucune décision trouvée" | 404 |

---

## Critères d'acceptation

- [ ] POST crée/met à jour une décision et retourne les titres recommandés
- [ ] GET retourne la dernière décision persistée
- [ ] L'isolation workspace est respectée (403/404 si workspace différent)
- [ ] Le domaine juridique est vérifié (DROIT_IMMIGRATION uniquement)
- [ ] Les critères invalides (pays, motif, durée) retournent 400
- [ ] L'upsert fonctionne : un second POST remplace la décision précédente
- [ ] Les données sont configurées pour FRANCE et BELGIQUE

---

## Périmètre

### Hors scope (explicite)

- Interface frontend (SF-IM-05-03)
- Export PDF
- Intégration avec le pipeline IA

---

## Contraintes de validation

| Champ | Obligatoire | Valeurs autorisées | Normalisation |
|-------|-------------|-------------------|---------------|
| country | Oui | FRANCE, BELGIQUE | — |
| nationaliteUe | Oui | true, false | — |
| motif | Oui | TRAVAIL, ETUDES, FAMILLE, ASILE, AUTRE | — |
| duree | Oui | COURT_SEJOUR, LONG_SEJOUR | — |
| situationFamiliale | Non | CELIBATAIRE, MARIE, PACS_COHABITATION, null | — |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/immigration/title-decision` | Oui | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/immigration/title-decision` | Oui | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `immigration_title_decisions` | INSERT / UPDATE / SELECT | Upsert par case_file_id |
| `case_files` | SELECT | Vérification domaine + workspace |

### Migration Liquibase

- [x] Non applicable — table créée dans SF-IM-05-01

### Classes Java à créer

| Classe | Rôle |
|--------|------|
| `ImmigrationTitleDecisionService` | Service : validation, résolution, persistance, lecture |
| `ImmigrationTitleDecisionController` | Controller REST |
| `ImmigrationTitleDecisionRequest` | Record requête POST |
| `ImmigrationTitleDecisionResponse` | Record réponse |

---

## Plan de test

### Tests unitaires

- [ ] Service — POST cas nominal : critères valides → décision persistée + recommandations retournées
- [ ] Service — POST upsert : second appel remplace la décision
- [ ] Service — POST pays invalide → 400
- [ ] Service — POST motif invalide → 400
- [ ] Service — POST durée invalide → 400
- [ ] Service — GET décision existante → réponse complète
- [ ] Service — GET sans décision → 404

### Tests d'intégration

- [ ] POST `/api/v1/case-files/{id}/immigration/title-decision` → 200 avec payload valide
- [ ] POST → 400 avec pays invalide
- [ ] POST → 400 avec domaine != DROIT_IMMIGRATION
- [ ] POST → 404 avec dossier d'un autre workspace
- [ ] GET → 200 après un POST
- [ ] GET → 404 sans décision préalable

### Isolation workspace

- [x] Applicable — test : un utilisateur du workspace A ne peut pas accéder aux décisions du workspace B

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée, réutilise le pattern existant (ImmigrationChecklistController)

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné (nouveau endpoint, aucun existant modifié)

---

## Dépendances

### Subfeatures bloquantes

- SF-IM-05-01 — statut : done

### Questions ouvertes impactées

- [ ] Aucune

---

## Notes et décisions

- Le pattern controller/service/DTOs suit exactement `ImmigrationChecklistController` / `ImmigrationChecklistService`
- L'upsert utilise `findByCaseFileId()` puis save (même pattern que `PrudhomeFicheService`)
- Le JSON des recommandations est sérialisé avec Jackson (ObjectMapper) pour le champ TEXT
- Le path est `/immigration/title-decision` (sous-ressource du dossier) pour rester cohérent avec `/immigration-checklist`
