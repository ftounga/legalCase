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
created_at  
updated_at

## auth_accounts

Identité externe OAuth.

Champs :

id
user_id
provider
provider_user_id
provider_email
access_scope
created_at

## workspaces

Client SaaS.

Champs :

id  
name  
slug  
billing_email  
owner_user_id  
plan_code  
status

## workspace_members

Relation utilisateur workspace.

Champs :

workspace_id  
user_id  
member_role

Roles possibles :

OWNER  
ADMIN  
LAWYER  
MEMBER

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
plan_code (varchar 20 — valeurs : STARTER, PRO)
status (varchar 20 — valeur initiale : ACTIVE)
started_at (timestamptz, non nullable)
expires_at (timestamptz, nullable — null = pas d'expiration)

Plans V1 :

STARTER — 3 dossiers actifs max, 5 documents/dossier, re-analyse enrichie non disponible
PRO — 20 dossiers actifs max, 30 documents/dossier, re-analyse enrichie disponible

Règles :

- Un workspace a au plus une subscription (contrainte unique sur workspace_id)
- Créée automatiquement au plan STARTER lors de la création du workspace (WorkspaceService)
- Fail open : absence de subscription = accès autorisé (Integer.MAX_VALUE)

---

# 18 — Pipeline documentaire

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

# 26 — Principe directeur

AI LegalCase doit rester :

- spécialisé
- structuré
- multi-tenant
- traçable
- extensible

Toute évolution doit respecter ces principes.

Ce document constitue la référence technique du projet.