# ARCHITECTURE_CANONIQUE.md
AI LegalCase — Architecture produit et technique de référence

Ce document constitue la source de vérité architecturale du projet AI LegalCase.
Toute implémentation technique, toute proposition d'évolution ou toute génération
de code doit rester cohérente avec ce document.

Toute divergence doit être explicitement signalée.

---

# 1 — Vision du produit

AI LegalCase est un micro-SaaS LegalTech destiné aux **avocats indépendants et petits cabinets**.

Objectif principal :

Permettre l’analyse rapide de **dossiers juridiques multi-documents** via un pipeline
d’intelligence artificielle structuré.

Le système :

1. centralise les documents d’un dossier
2. extrait le texte exploitable
3. segmente le contenu
4. analyse via LLM
5. produit une synthèse juridique
6. génère des questions complémentaires
7. enrichit l’analyse avec les réponses de l’avocat

Le produit réduit drastiquement le temps d’analyse d’un dossier juridique.

---

# 2 — Positionnement produit

## Domaine initial (V1)

Droit du travail

Cas d’usage principaux :

- licenciement abusif
- harcèlement moral
- rupture conventionnelle contestée
- sanctions disciplinaires
- contentieux prud'homal

Documents typiques :

- contrats de travail
- emails
- lettres de licenciement
- avertissements disciplinaires
- bulletins de salaire
- attestations

## Extension progressive

La plateforme évoluera par niches spécialisées :

V1  
Droit du travail

V2  
Droit de l’immigration

V3  
Droit immobilier

Principe fondamental :

Ne jamais construire un assistant juridique généraliste.  
Toujours spécialiser par domaine.

---

# 3 — Modèle SaaS

AI LegalCase est un SaaS multi-tenant.

Une seule plateforme est déployée.

Chaque client possède un **workspace isolé**.

## Concepts fondamentaux

### Client

Organisation utilisant la plateforme.

Exemples :

- avocat indépendant
- cabinet d'avocats

### Workspace

Représentation technique du client dans le système.

Un workspace :

- possède les dossiers
- possède les documents
- possède les analyses
- possède l'abonnement

### Utilisateur

Personne physique accédant à la plateforme.

Un utilisateur peut appartenir à un ou plusieurs workspaces.

---

# 4 — Stack technique

La stack est volontairement simple et maîtrisée.

Frontend
Angular 19

Backend
Spring Boot 3.5 / Java 21

Base de données
PostgreSQL (production) — H2 en mémoire (dev/test)

Migrations de schéma
Liquibase (XML, versionné dans `db/changelog/migrations/`)

Authentification
Spring Security + OAuth2 / OIDC

Providers OAuth V1 :

- Google
- Microsoft

Stockage fichiers

Object storage compatible S3.

Queue asynchrone

RabbitMQ — utilisé à partir de F-08 (appels LLM).
`@Async` Spring conservé pour F-06 (extraction) et F-07 (chunking) — traitements locaux courts.

Intégration IA

Provider : **Anthropic** (Claude).
Appels via API HTTP REST (RestClient Spring).
Modèle V1 : `claude-sonnet-4-6`.

Les traitements IA sont obligatoirement **asynchrones**.

---

# 5 — Architecture système

Architecture logique :

Frontend Angular  
→ interface utilisateur

Backend Spring Boot  
→ API métier et orchestration

PostgreSQL  
→ persistance

Object storage  
→ stockage documents

Provider LLM  
→ analyse IA

Responsabilités du backend :

- gestion utilisateurs
- gestion workspaces
- gestion dossiers
- gestion documents
- pipeline IA
- jobs asynchrones
- suivi usage
- billing

---

# 6 — Sous-systèmes de la plateforme

La plateforme est organisée en 5 sous-systèmes principaux.

## 1 Identité & authentification

Gestion :

- utilisateurs
- comptes OAuth
- workspaces
- membres

## 2 Gestion dossiers & documents

Fonctions :

- création dossier
- upload documents
- extraction texte
- chunking

## 3 Pipeline IA

Analyse multi-niveaux :

chunk → document → dossier

## 4 Interaction utilisateur

Fonctions :

- restitution analyse
- questions IA
- réponses avocat

## 5 Exploitation

Fonctions :

- jobs asynchrones
- usage events
- billing

---

# 7 — Authentification

Authentification via OAuth2 / OIDC.

Aucun mot de passe local.

Providers V1 :

- Google
- Microsoft

Option possible :

Magic link email.

SSO entreprise possible en V2+ :

- Azure AD
- Google Workspace
- SAML / OIDC entreprise

---

# 8 — Flux d’onboarding

Lors du premier login :

1 Login OAuth
2 Récupération identité
3 Création utilisateur
4 Création workspace
5 Assignation rôle OWNER
6 Redirection dashboard

Cas avocat indépendant :

Le workspace correspond à son cabinet.

Cas cabinet :

Premier utilisateur crée le workspace puis invite les autres membres.

---

# 9 — Modèle de données global

Le modèle de données comporte quatre zones.

## Identité

users
auth_accounts
email_verification_tokens
password_reset_tokens
workspaces
workspace_members

## Métier juridique

case_files  
documents  
document_extractions  
document_chunks

## Analyse IA

chunk_analyses
document_analyses
case_analyses
ai_questions
ai_question_answers
analysis_documents
analysis_diff_cache
analysis_qa_snapshots

## Exploitation

analysis_jobs  
usage_events  
subscriptions

---

# 10 — Tables identité

## users

Représente une personne physique.

Champs :

id
email
first_name
last_name
status
is_super_admin (boolean, NOT NULL, DEFAULT FALSE)
created_at
updated_at

## auth_accounts

Identité externe OAuth ou compte LOCAL.

Champs :

id
user_id
provider (GOOGLE, MICROSOFT, LOCAL)
provider_user_id (pour LOCAL : email de l'utilisateur)
provider_email
access_scope
password_hash (VARCHAR 255, nullable — rempli uniquement pour provider=LOCAL)
email_verified (boolean, NOT NULL, DEFAULT TRUE — false pour les comptes LOCAL non encore vérifiés)
created_at

## email_verification_tokens

Token de validation d'email pour les comptes LOCAL (expiration 24h).

Champs :

id (UUID PK)
user_id (UUID, FK → users.id, NOT NULL)
token (VARCHAR 255, UNIQUE, NOT NULL)
expires_at (timestamptz, NOT NULL)
used_at (timestamptz, nullable — rempli après usage pour audit)
created_at (timestamptz, NOT NULL)

## password_reset_tokens

Token de réinitialisation de mot de passe (expiration 24h).

Champs :

id (UUID PK)
user_id (UUID, FK → users.id, NOT NULL)
token (VARCHAR 255, UNIQUE, NOT NULL)
expires_at (timestamptz, NOT NULL)
used_at (timestamptz, nullable — rempli après usage pour audit)
created_at (timestamptz, NOT NULL)

## workspaces

Client SaaS.

Champs :

id
name
slug
billing_email
owner_user_id
legal_domain (varchar 50, NOT NULL, DEFAULT 'DROIT_DU_TRAVAIL' — valeurs : DROIT_DU_TRAVAIL, DROIT_IMMIGRATION, DROIT_IMMOBILIER)
plan_code
status

## workspace_members

Relation utilisateur workspace.

Champs :

workspace_id (PK, FK → workspaces)
user_id (PK, FK → users)
member_role (varchar 50)
is_primary (boolean, NOT NULL, DEFAULT TRUE)
created_at (timestamptz)

Roles possibles :

OWNER
ADMIN
LAWYER
MEMBER

Règle `is_primary` :
- Un utilisateur peut appartenir à plusieurs workspaces.
- `is_primary = true` désigne le workspace actif par défaut (résolu à chaque requête).
- Le premier workspace créé lors de l'onboarding a `is_primary = true`.

## workspace_invitations

Invitation à rejoindre un workspace par email.

Champs :

id (UUID PK)
workspace_id (UUID, FK → workspaces)
invited_by_user_id (UUID, FK → users)
email (varchar 255)
role (varchar 50)
token (varchar 255, UNIQUE)
status (varchar 20 — valeurs : PENDING, ACCEPTED, EXPIRED, REVOKED)
expires_at (timestamptz)
created_at (timestamptz)

Index :

idx_workspace_invitations_token
idx_workspace_invitations_workspace_id

Règles :
- Une seule invitation PENDING par (workspace_id, email).
- Le token est généré côté backend (UUID aléatoire), passé en query param lors du login OAuth.
- Après acceptation, `status → ACCEPTED` et l'utilisateur est ajouté en tant que member.

---

# 11 — Tables métier

## case_files

Dossier juridique.

Champs :

id  
workspace_id  
created_by_user_id  
title  
legal_domain  
status  
description

Valeurs legal_domain :

EMPLOYMENT_LAW  
IMMIGRATION_LAW  
REAL_ESTATE_LAW

V1 active uniquement :

EMPLOYMENT_LAW

---

## documents

Document uploadé.

Champs :

id
case_file_id
uploaded_by_user_id
original_filename
content_type
file_size
storage_key
created_at

Types acceptés (V1) :

application/pdf
application/msword
application/vnd.openxmlformats-officedocument.wordprocessingml.document
text/plain

Taille maximale : 50 Mo
Format storage_key : {workspaceId}/{caseFileId}/{UUID}/{sanitized_filename}

---

# 12 — Traitement documentaire

## document_extractions

Texte extrait du document.

Champs :

id
document_id
extraction_status
extracted_text
extraction_metadata
created_at
updated_at

Valeurs extraction_status : PENDING, PROCESSING, DONE, FAILED

---

## document_chunks

Segments textuels issus du chunking.

Champs :

id
extraction_id
chunk_index
chunk_text
token_count
chunk_metadata
created_at

Paramètres V1 : taille 1000 caractères, overlap 200 caractères
token_count : approximation chunk_text.length() / 4

---

# 13 — Analyse IA

Le pipeline IA comporte trois niveaux.

## Niveau 1 — chunk

Analyse de chaque segment par le LLM.

Table :

chunk_analyses

Champs :

id
chunk_id (FK → document_chunks)
analysis_status (PENDING, PROCESSING, DONE, FAILED)
analysis_result (TEXT — JSON retourné par le LLM)
model_used
prompt_tokens
completion_tokens
created_at
updated_at

Déclenchement : message RabbitMQ publié après chunking — consumer @RabbitListener appelle Anthropic API.

---

## Niveau 2 — document

Synthèse du document. Déclenchée quand toutes les chunk_analyses du document sont DONE.

Table :

document_analyses

Champs :

id
document_id (FK → documents)
extraction_id (FK → document_extractions)
analysis_status (PENDING, PROCESSING, DONE, FAILED)
analysis_result (TEXT — JSON retourné par le LLM)
model_used
prompt_tokens
completion_tokens
created_at
updated_at

Idempotence : existsByExtractionIdAndAnalysisStatusIn(PENDING, PROCESSING, DONE) avant déclenchement.

---

## Niveau 3 — dossier

Synthèse globale. Déclenchée quand toutes les document_analyses du dossier sont DONE.

Table :

case_analyses

Champs :

id
case_file_id (FK → case_files)
analysis_status (PENDING, PROCESSING, DONE, FAILED)
analysis_result (TEXT — JSON retourné par le LLM)
model_used
prompt_tokens
completion_tokens
created_at
updated_at

Idempotence : existsByCaseFileIdAndAnalysisStatusIn(PENDING, PROCESSING, DONE) avant déclenchement.

---

# 14 — Interactivité IA

## ai_questions

Questions générées par l’IA.

Champs :

case_file_id  
question_text  
question_category  
status  
answered_at

## ai_question_answers

Réponses avocat.

Champs :

ai_question_id  
answered_by_user_id  
answer_text  
created_at

Les réponses déclenchent une **nouvelle synthèse enrichie**.

---

# 15 — Gestion des jobs

Les analyses sont asynchrones. Chaque niveau du pipeline (chunk, document, dossier) dispose d'un job de suivi associé à un dossier.

Table :

analysis_jobs

Champs :

id (UUID PK)
case_file_id (FK → case_files)
job_type (CHUNK_ANALYSIS | DOCUMENT_ANALYSIS | CASE_ANALYSIS)
status (PENDING | PROCESSING | DONE | FAILED)
total_items (nombre total d'items à traiter)
processed_items (nombre d'items traités)
error_message (nullable — renseigné en cas de FAILED)
created_at
updated_at

Unicité : (case_file_id, job_type) — un seul job actif par case et par niveau.

Cycle de vie :
- PENDING → créé au déclenchement du niveau (onChunkingDone, consumeDocumentAnalysis, consumeCaseAnalysis)
- PROCESSING → mis à jour au démarrage du traitement
- DONE → tous les items traités avec succès
- FAILED → erreur non récupérée

Notes :
- processed_items est recalculé depuis la base (count of DONE items) pour garantir la cohérence en cas de retry
- Pour CHUNK_ANALYSIS : totalItems est cumulé par extraction (plusieurs documents = plusieurs onChunkingDone)

---

# 16 — Suivi usage IA

Table :

usage_events

Objectif :

suivi consommation LLM.

Champs :

id (UUID PK)
user_id (FK → users)
case_file_id (FK → case_files)
event_type (varchar 30 — valeurs : CHUNK_ANALYSIS, DOCUMENT_ANALYSIS, CASE_ANALYSIS, ENRICHED_ANALYSIS)
tokens_input (int)
tokens_output (int)
estimated_cost (decimal 12,6)
created_at (timestamptz)

Index :

idx_usage_events_case_file_id
idx_usage_events_user_id

---

# 17 — Billing

Table :

subscriptions

Champs :

id (UUID PK)
workspace_id (UUID FK → workspaces, unique)
plan_code (varchar 20 — valeurs : FREE, STARTER, PRO)
status (varchar 20 — valeur initiale : ACTIVE)
started_at (timestamptz, non nullable)
expires_at (timestamptz, nullable — null = pas d'expiration)
stripe_customer_id (varchar 255, nullable)
stripe_subscription_id (varchar 255, nullable)

Plans V1 :

FREE — 1 dossier actif max, 3 documents/dossier, re-analyse enrichie non disponible, durée 14 jours
STARTER — 3 dossiers actifs max, 5 documents/dossier, re-analyse enrichie non disponible
PRO — 20 dossiers actifs max, 30 documents/dossier, re-analyse enrichie disponible

Règles :

- Un workspace a au plus une subscription (contrainte unique sur workspace_id)
- Créée automatiquement au plan FREE lors de la création du workspace (WorkspaceService), expires_at = now() + 14j
- Fail open : absence de subscription = accès autorisé (Integer.MAX_VALUE)
- stripe_customer_id : créé via Stripe API à la création du workspace, fail-open (null si Stripe indisponible)
- stripe_subscription_id : rempli par le webhook Stripe lors du paiement (SF-19-03)

## credit_purchases

Table :

credit_purchases

Champs :

id (UUID PK)
workspace_id (UUID FK → workspaces)
tokens_bought (bigint, NOT NULL)
amount_cents (int, NOT NULL)
stripe_session_id (varchar 255, UNIQUE, NOT NULL)
created_at (timestamptz, NOT NULL)

Index :

idx_credit_purchases_workspace_id

Règles :

- Un achat est idempotent : le service vérifie l'unicité de stripe_session_id avant d'insérer
- Crédits déduits globalement (all-time) dans PlanLimitService.isMonthlyTokenBudgetExceeded()
- 3 packs disponibles : TOKENS_1M (990 cents), TOKENS_5M (3990 cents), TOKENS_20M (12990 cents)
- Migration : 035-create-credit-purchases.xml

---

# 18 — Chat

Table :

chat_messages

Champs :

id (UUID PK — généré par JPA)
case_file_id (UUID FK → case_files, cascade delete)
user_id (UUID FK → users)
question (TEXT, non nullable)
answer (TEXT, nullable)
model_used (varchar 100, nullable)
use_enriched (boolean, non nullable, défaut false)
created_at (timestamptz, non nullable)

Règles :

- Accès contrôlé : user doit appartenir au workspace propriétaire du dossier
- Gate plan : isChatMessageLimitReached → 402 si limite mensuelle dépassée
- Synthèse DONE requise → 424 si absente
- Modèle : analyzeFast (Haiku) par défaut, analyze (Sonnet) si useEnriched=true et plan PRO
- Limites mensuelles : FREE=10, STARTER=50, PRO=200 messages/mois
- @Profile("local") — service et contrôleur inactifs en profil dev (H2)

Index :

idx_chat_messages_case_file_id

---

# 18b — Snapshots documents par analyse

Table :

analysis_documents

Objectif : associer les documents présents au moment d'une analyse à cette analyse, pour permettre l'attribution des raisons dans le diff sémantique.

Champs :

id (UUID PK — généré par JPA)
analysis_id (UUID FK → case_analyses, cascade delete)
document_id (UUID, sans FK — intentionnel : hard deletes sur documents)
document_name (varchar 500, non nullable)
created_at (timestamptz, non nullable)

Contraintes :

UNIQUE (analysis_id, document_id)

Index :

idx_analysis_documents_analysis_id

Règles :

- Remplie au moment de la création de chaque CaseAnalysis (STANDARD et ENRICHED) par AnalysisDocumentSnapshotService
- document_id sans FK car les documents peuvent être supprimés définitivement (hard delete)
- Lecture seule après insertion : pas de mise à jour

---

# 18c — Cache de diff sémantique

Table :

analysis_diff_cache

Objectif : éviter d'appeler Haiku plusieurs fois pour la même paire d'analyses. Les analyses historiques étant immuables, le cache est permanent (pas d'invalidation).

Champs :

id (UUID PK — généré par JPA)
from_id (UUID, non nullable — référence analysis source)
to_id (UUID, non nullable — référence analysis cible)
result_json (TEXT, non nullable — sérialisation JSON de AnalysisDiffResponse)
created_at (timestamptz, non nullable)

Contraintes :

UNIQUE (from_id, to_id)

Index :

idx_analysis_diff_cache_from_to (from_id, to_id)

Règles :

- Écrit par AnalysisDiffService après chaque appel à SemanticDiffService
- Lu avant tout appel Haiku (cache hit → retour direct, < 50ms)
- Entrée corrompue → supprimée et recalculée automatiquement
- Pas de TTL : validité permanente

---

## Table analysis_qa_snapshots

Table :

analysis_qa_snapshots

Objectif : figer l'état des Q&A au moment de la création d'une analyse enrichie, pour que le diff sémantique utilise le contexte Q&R exact de la version TO et non l'état courant au moment du diff.

Colonnes :

- id UUID PK
- analysis_id UUID NOT NULL — FK → case_analyses(id)
- order_index INT NOT NULL
- question_text TEXT NOT NULL
- answer_text TEXT NOT NULL
- created_at TIMESTAMPTZ NOT NULL DEFAULT now()

Index :

idx_analysis_qa_snapshots_analysis_id

Règles :

- Écrit par AnalysisQaSnapshotService.snapshot() dans prepareEnrichedAnalysis(), après le snapshot documents
- Lu par SemanticDiffService.buildContext() via buildQaContext(toAnalysisId)
- Si absent (analyses créées avant cette migration) → fallback sur l'état courant des Q&A du dossier
- Immuable après création : représente la photo figée à l'instant T de l'analyse

---

# 19 — Pipeline documentaire

Étapes :

1 création dossier
2 upload documents
3 stockage fichiers
4 extraction texte
5 chunking

Chaque étape est persistée.

---

# 19 — Pipeline IA

Étapes :

1 analyse chunk
2 synthèse document
3 synthèse dossier
4 génération questions IA
5 réponses avocat
6 nouvelle synthèse enrichie

---

# 20 — Architecture prompts

Prompts spécialisés par domaine.

Structure :

employment-law / chunk-analysis  
employment-law / document-summary  
employment-law / case-analysis

V2 :

immigration-law / chunk-analysis

V3 :

real-estate-law / chunk-analysis

---

# 21 — Sécurité

Principes V1 :

- OAuth2 obligatoire
- aucun mot de passe local
- isolation stricte par workspace
- journalisation minimale
- audit des analyses

---

# 21b — Audit Log

Table :

audit_logs

Champs :

id (UUID PK)
workspace_id (UUID FK → workspaces, non nullable)
user_id (UUID FK → users, non nullable)
case_file_id (UUID FK → case_files, nullable)
action (varchar 50, non nullable — ex. DOCUMENT_DELETED)
metadata (CLOB, nullable — JSON contextuel)
created_at (timestamptz, non nullable)

Règles :

- Créé par les services métier lors d'actions traçables (suppression de documents, etc.)
- Lecture réservée aux rôles OWNER et ADMIN du workspace
- Non modifiable, non supprimable par l'API

Index :

idx_audit_logs_workspace_id
idx_audit_logs_case_file_id

---

# 22 — Observabilité

Éléments à tracer :

statut upload  
statut extraction  
statut jobs  
temps traitement  
consommation tokens  
coûts LLM

---

# 23 — Scalabilité

Choix structurants :

- pipeline asynchrone
- chunking systématique
- stockage objet
- séparation frontend backend IA
- isolation workspace

Évolutions possibles :

workers IA  
queue jobs  
optimisation LLM  
caching analyses

---

# 24 — Limites V1

Pas de :

- jurisprudence automatique
- génération argumentaire
- préparation audience
- SSO entreprise
- collaboration avancée
- OCR complexe

---

# 25 — Roadmap

V1

Droit du travail

V2

Droit immigration

V3

Droit immobilier

---

# 25b — Notes internes (F-70)

Table :

case_notes

Champs :

id (UUID PK — généré par JPA)
case_file_id (UUID FK → case_files, cascade delete)
created_by_user_id (UUID FK → users)
content (TEXT, non nullable)
created_at (timestamptz, non nullable)
updated_at (timestamptz, non nullable)

Règles :

- Accès contrôlé : user doit appartenir au workspace propriétaire du dossier
- Création/modification/suppression réservées à l'auteur (created_by_user_id = user courant)
- Liste retournée triée par created_at DESC
- Migration : 036-create-case-notes.xml

Index :

idx_case_notes_case_file_id

---

# 25c — Délais légaux (F-69)

Table :

case_deadlines

Champs :

id (UUID PK — généré par JPA)
case_file_id (UUID FK → case_files, cascade delete)
label (varchar 255, non nullable)
due_date (DATE, non nullable — LocalDate, sans timezone)
source (varchar 10, non nullable — "MANUAL" | "STATUTORY")
ai_status (varchar 50, nullable)
alert_thresholds (varchar 50, nullable — CSV ex: "90,30,7" pour délais multi-seuils DROIT_IMMIGRATION)
document_type (varchar 50, nullable — ex: "TITRE_SEJOUR", "CARTE_RESIDENT")
created_at (timestamptz, non nullable)
updated_at (timestamptz, non nullable)

Règles :

- Accès contrôlé : user doit appartenir au workspace propriétaire du dossier
- CRUD accessible à tout membre du workspace (pas de restriction auteur — données d'équipe)
- Liste retournée triée par due_date ASC
- alert_thresholds non null → parcours multi-threshold dans DeadlineAlertService (déduplication via deadline_alert_sends)
- alert_thresholds null → parcours standard J-15/J-7
- Migration : 037-create-case-deadlines.xml + 046-extend-case-deadlines-alert-thresholds.xml

Index :

idx_case_deadlines_case_file_id

Table de déduplication :

deadline_alert_sends

Champs :

id (UUID PK — généré par JPA)
deadline_id (UUID FK → case_deadlines, cascade delete)
threshold_days (INT, non nullable)
sent_at (timestamptz, non nullable — @PrePersist)

Contrainte :

UNIQUE(deadline_id, threshold_days) — garantit qu'un seuil n'est envoyé qu'une fois par délai

Règle : enregistrement créé par DeadlineAlertService après chaque envoi multi-threshold réussi

---

# 25d — Checklist procédurale (F-96 / SF-96-01)

Table :

procedure_checks

Champs :

id (UUID PK — généré par JPA)
case_analysis_id (UUID FK → case_analyses, cascade delete)
workspace_id (UUID FK → workspaces, cascade delete — dénormalisé pour isolation)
ordre (INT, non nullable — index dans le tableau points_procedure du JSON LLM)
description (TEXT, non nullable — libellé du point procédural)
statut (VARCHAR 20, non nullable, DEFAULT 'TO_CHECK' — TO_CHECK | VERIFIED | NON_COMPLIANT)
created_at (timestamptz, non nullable)
updated_at (timestamptz, non nullable)

Règles :

- Créés automatiquement par ProcedureCheckService.createChecks() à la fin de chaque analyse (STANDARD et ENRICHED)
- Extraction fail-open : si points_procedure absent ou JSON invalide → aucun check créé, pas d'exception
- Les checks sont recréés à chaque nouvelle version d'analyse (les anciens de la même analyse sont supprimés)
- workspace_id dénormalisé pour simplifier les requêtes d'isolation (même pattern que case_deadlines)
- GET : isolation vérifiée via case_file.workspace_id
- PATCH : isolation vérifiée via procedure_check.workspace_id
- Migration : 040-create-procedure-checks.xml

Endpoints :

GET  /api/v1/case-files/{caseFileId}/analyses/{analysisId}/procedure-checks — liste ordonnée par ordre ASC
PATCH /api/v1/procedure-checks/{checkId} — body {statut: "VERIFIED"} — met à jour le statut

Index :

idx_procedure_checks_case_analysis_id
idx_procedure_checks_workspace_id

---

## prudhome_fiches

Fiche prud'homale — document procédural 1:1 avec un dossier. Stocke les parties, faits, demandes et moyens de droit sous forme JSON TEXT (compatible H2 + PostgreSQL).

```
prudhome_fiches
  id                UUID PK
  case_file_id      UUID FK → case_files(id) ON DELETE CASCADE  UNIQUE
  demandeur         TEXT NOT NULL DEFAULT '{}'   -- JSON sérialisé (nom, prénom, adresse, téléphone, email, profession)
  defendeur         TEXT NOT NULL DEFAULT '{}'   -- JSON sérialisé (nom, adresse, siret, représentant)
  demandes          TEXT NOT NULL DEFAULT '[]'   -- JSON sérialisé (label, montant)
  faits_texte       TEXT
  moyens_droit_texte TEXT
  created_at        TIMESTAMPTZ NOT NULL
  updated_at        TIMESTAMPTZ NOT NULL
  UNIQUE uq_prudhome_fiches_case_file_id (case_file_id)
```

Notes :
- Relation 1:1 stricte avec case_files (UNIQUE constraint sur case_file_id)
- Upsert via findByCaseFileId + save (pas de requête native)
- Champs JSON stockés en TEXT pour compatibilité H2 (profil dev) et PostgreSQL
- La liste des pièces est générée dynamiquement à la lecture depuis la table documents (non persistée)
- Pré-remplissage des demandes depuis compensationEstimate si une analyse DONE est disponible

Index :

idx_prudhome_fiches_case_file_id

---

## email_sends

Déduplication des emails automatiques (onboarding, etc.) — évite les doublons d'envoi.

```
email_sends
  id               UUID PK
  user_id          UUID FK → users(id) ON DELETE CASCADE
  email_type       VARCHAR(100) NOT NULL  -- enum : ONBOARDING_WELCOME, ONBOARDING_TIP_ANALYSIS, ...
  sent_at          TIMESTAMPTZ NOT NULL
  UNIQUE (user_id, email_type)
```

- Migration : 038-create-email-sends.xml

Index :

idx_email_sends_user_id

---

# 25b — Tables time tracking (F-106 — SF-106-01)

## user_billing_rates

Taux horaire facturable par utilisateur et par workspace. Historique conservé — chaque modification insère une nouvelle ligne.

id (UUID PK)
user_id (UUID FK → users, non nullable)
workspace_id (UUID FK → workspaces, non nullable)
rate_per_hour (NUMERIC(10,2), non nullable, CHECK > 0)
effective_from (DATE, non nullable)
created_at (TIMESTAMP WITH TIME ZONE, non nullable)

Index : idx_billing_rates_user_workspace (user_id, workspace_id)

Règles :
- Le taux actif est la ligne dont `effective_from` est la plus récente (≤ date courante).
- Le taux est résolu par utilisateur — pas par workspace global.
- Un utilisateur sans taux configuré produit un montant `null` dans le rapport.

---

## time_entries

Sessions de temps facturable par dossier, utilisateur et workspace.

id (UUID PK)
case_file_id (UUID FK → case_files, non nullable)
workspace_id (UUID FK → workspaces, non nullable)
user_id (UUID FK → users, non nullable)
started_at (TIMESTAMP WITH TIME ZONE, non nullable)
stopped_at (TIMESTAMP WITH TIME ZONE, nullable — null si timer actif)
duration_seconds (INTEGER, nullable — calculé à l'arrêt du timer)
created_at (TIMESTAMP WITH TIME ZONE, non nullable)

Index :
idx_time_entries_case_file (case_file_id)
idx_time_entries_workspace_month (workspace_id, started_at)
idx_time_entries_workspace (workspace_id)

Règles :
- Un utilisateur ne peut avoir qu'un seul timer actif (`stopped_at IS NULL`) à la fois.
- Les entrées sans `stopped_at` sont exclues du rapport mensuel.
- `duration_seconds = EXTRACT(EPOCH FROM stopped_at - started_at)` calculé côté applicatif à l'arrêt.
- Isolation multi-tenant : toutes les requêtes filtrent par `workspace_id`.

---

# 26 — Principe directeur

AI LegalCase doit rester :

- spécialisé
- structuré
- multi-tenant
- traçable
- extensible

Toute évolution doit respecter ces principes.

Ce document constitue la référence technique du projet.