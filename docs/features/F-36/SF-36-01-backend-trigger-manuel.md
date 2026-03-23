# SF-36-01 — Backend : trigger manuel de l'analyse dossier

## Objectif
Remplacer le déclenchement automatique du case analysis (à la fin du dernier document) par un endpoint manuel appelé explicitement par l'utilisateur.

## Comportement nominal
1. L'utilisateur uploade ses documents → chunk/document analysis se déclenchent automatiquement comme avant
2. Quand l'utilisateur clique "Analyser le dossier" → `POST /api/v1/case-files/{id}/analyze` est appelé
3. Le backend vérifie les gates (voir ci-dessous), puis publie un message RabbitMQ sur la queue case analysis
4. Le pipeline case analysis → question generation s'exécute normalement

## Cas d'erreur
- `404` si le dossier n'appartient pas au workspace de l'utilisateur
- `402` si la limite d'analyses par dossier est atteinte (FREE=2, STARTER=5, PRO=illimité)
- `402` si le budget tokens mensuel est dépassé
- `409` si une analyse est déjà en cours (status PENDING ou PROCESSING)
- `422` si aucun document n'est en état DONE (rien à analyser)

## Critères d'acceptation
- [ ] `POST /api/v1/case-files/{id}/analyze` existe et est protégé par auth
- [ ] Le gate `isCaseAnalysisLimitReached` retourne true si FREE ≥ 2, STARTER ≥ 5, PRO jamais
- [ ] Le compteur utilise `UsageEvent` de type `CASE_ANALYSIS` filtrés par `caseFileId`
- [ ] `DocumentAnalysisService.triggerCaseAnalysisIfReady()` est supprimé
- [ ] Un case analysis déjà en cours retourne 409
- [ ] Les tests `PlanLimitServiceTest` couvrent les 3 plans + FREE expiré
- [ ] Le endpoint rejette une demande sans document DONE (422)

## Plan de test
**Unitaires :**
- `PlanLimitServiceTest` : FREE < 2 → false, FREE = 2 → true, STARTER < 5 → false, STARTER = 5 → true, PRO → toujours false
- `CaseAnalysisCommandServiceTest` (nouveau) : 404 dossier inconnu, 402 limite, 402 tokens, 409 en cours, 422 sans doc DONE, 200 nominal

**Intégration :**
- Profil `dev` (H2) : pipeline complet déclenché manuellement, vérifié en base

**Isolation workspace :**
- Un utilisateur ne peut pas déclencher l'analyse d'un dossier appartenant à un autre workspace → 404

## Tables / endpoints impactés
- `usage_events` (lecture — comptage CASE_ANALYSIS par caseFileId)
- Nouveau endpoint : `POST /api/v1/case-files/{id}/analyze`
- Nouveau service : `CaseAnalysisCommandService`
- Nouveau controller : `CaseAnalysisController` (ou ajout dans `CaseFileController`)
- Modifié : `DocumentAnalysisService` — suppression de `triggerCaseAnalysisIfReady()`
- Modifié : `PlanLimitService` — nouveau gate `isCaseAnalysisLimitReached`
- Nouveau dans `PlanLimitService` : constantes FREE_MAX_CASE_ANALYSES=2, STARTER_MAX_CASE_ANALYSES=5

## Hors périmètre
- Modification du pipeline chunk/document (inchangé)
- Frontend (SF-36-02)
- Modification de l'analyse enrichie PRO (inchangée)
- Compteur affiché dans l'UI (SF-36-02)
