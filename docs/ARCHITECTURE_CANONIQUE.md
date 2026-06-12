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
document_pieces  
case_phases

## Analyse IA

chunk_analyses
document_analyses
case_analyses
case_conclusions
case_jurisprudence_citations
tool_jurisprudence_mappings
jurisprudence_watch_flags
jurisprudence_audit_log
style_corpus_documents
ai_questions
ai_question_answers
analysis_documents
analysis_diff_cache
analysis_qa_snapshots

## Exploitation

analysis_jobs  
usage_events  
subscriptions  
dashboard_tile_crashes  
dashboard_audit_runs

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

## user_consent_acceptance

F-240 SF-240-01 — traçabilité des acceptations contractuelles (click-wrap CGU / politique de confidentialité / CGV / DPA). Table d'audit append-only : un consentement est immuable, jamais supprimé ni dédupliqué (valeur de preuve RGPD).

id (UUID PK)
user_id (UUID, non nullable, FK → users)
consent_type (VARCHAR(32), non nullable — SIGNUP_TERMS / PRIVACY_POLICY / PAYMENT_TERMS / DPA_DOWNLOAD ; contrainte CHECK chk_consent_type)
version (VARCHAR(64), non nullable — version du document accepté, format libre côté frontend)
accepted_at (TIMESTAMP, non nullable, défaut CURRENT_TIMESTAMP — tient lieu de timestamp de création)
acceptance_ip (VARCHAR(45), non nullable — IPv4/IPv6, extraite de X-Forwarded-For ou RemoteAddr)
acceptance_user_agent (VARCHAR(500), non nullable)
workspace_id (UUID, nullable, FK → workspaces — NULL si l'acceptation précède la création du workspace, ex. SIGNUP_TERMS)

Index :
idx_consent_user_type (user_id, consent_type)
idx_consent_workspace (workspace_id)

Règles :
- Table append-only : pas de soft-delete, pas de déduplication — chaque acceptation est une ligne distincte (un même utilisateur peut ré-accepter, ex. à chaque souscription de plan).
- `consent_type` en VARCHAR + CHECK applicatif (pas d'enum JPA) — extensible sans migration de schéma.
- Pas de filtre `workspace_id` standard : le consentement précède parfois le workspace. L'endpoint POST n'écrit que pour l'utilisateur authentifié courant — aucune fuite cross-user.
- Aucun endpoint de lecture ni de révocation en V1.
- Migration : 232-create-user-consent-acceptance.xml

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
procedure_jurisdiction  
procedure_stage  
procedure_position

Champs procedure_* (F-243) : stade procédural du dossier — juridiction, stade et position juridique. Nullable. Nomenclature dans `ProcedureStageCatalog` (backend), variable selon domaine × pays.

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

## document_pieces

Pièces juridiques identifiées à l'intérieur d'un document composite (F-145).
Détection automatique via Haiku après extraction DONE, fail-open sur une entrée
AUTRE si l'IA échoue. Consommée par la popup aperçu navigable et future F-146
(source précise universelle).

Champs :

id
document_id
type
label
page_start
page_end
order_index
created_at
updated_at

Valeurs type (enum fermé) : CONTRAT, PIECE_IDENTITE, SMS, EMAIL, ATTESTATION,
BULLETIN_PAIE, LETTRE, PHOTO, AUTRE

Invariant : au moins 1 entrée par document après extraction DONE (fallback AUTRE
couvrant tout le document si la détection échoue).

## case_phases

Progression datée du dossier dans le cycle procédural (F-283 / SF-283-01). Chaque
ligne = une transition datée : le dossier est entré dans la phase à `entered_at`.
La suite ordonnée par `entered_at` forme la frise des phases (Saisine →
Conciliation → Fond → Appel → Cassation…), affichée en tête de l'onglet Suivi.
Isolation workspace via `case_file_id`. Distinct de `contradictoire_rounds`
(échanges contradictoires) et de `case_files.procedure_stage` (libellé courant
statique, F-243).

Champs :

id
case_file_id (FK → case_files, ON DELETE CASCADE)
phase (varchar(30))
label (varchar(200), nullable)
entered_at (date)
note (varchar(2000), nullable)
created_at
updated_at

Index : idx_case_phases_case_file (case_file_id).

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
case_analysis_id (FK — migration 026)  
question_text  
order_index  
status  
answered_at  
critere_code (VARCHAR 50, nullable — code F-DT-08 ou F-DT-09 si la question s'y rattache, migration 069 SF-IA-03-03 / 071 SF-IA-03-05)
expected_value (VARCHAR 50, nullable — valeur confirmée par une réponse "oui" pour critère énuméré, migration 071 SF-IA-03-05)

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

## promo_codes

Table (F-255 SF-255-01 migration 299, F-255 SF-255-04 migration 301) :

id (UUID PK)
code (varchar 64, unique global — trim + uppercase côté Java, regex [A-Z0-9_-]+)
type (varchar 20 — valeurs : TRIAL_EXTENSION, STRIPE_DISCOUNT)
value_days (int, nullable — non null pour TRIAL_EXTENSION, null pour STRIPE_DISCOUNT)
stripe_coupon_id (varchar 255, nullable — renseigné par SF-255-04 pour les codes STRIPE_DISCOUNT)
stripe_promotion_code_id (varchar 255, nullable, INDEX — renseigné par SF-255-04, sert au lookup webhook)
value_off_type (varchar 10, nullable — PERCENT ou AMOUNT, requis si type=STRIPE_DISCOUNT)
value_off_amount (int, nullable — pourcentage 1-100 ou centimes EUR, requis si type=STRIPE_DISCOUNT)
currency (varchar 3, nullable — EUR V1, requis si value_off_type=AMOUNT)
duration (varchar 20, nullable — ONCE | REPEATING_3 | FOREVER, requis si type=STRIPE_DISCOUNT)
partner_label (varchar 100, non null — libellé partenaire libre, ex « ACE »)
max_uses (int, non null)
uses_count (int, non null, défaut 0)
expires_at (timestamptz, non null, > now() à la création)
active (boolean, non null, défaut true)
created_at (timestamptz, non null)
created_by_user_id (UUID FK → users)

Index :

idx_promo_codes_code (unique sur code)
idx_promo_codes_active_expires (sur active, expires_at)
idx_promo_codes_stripe_promotion_code_id (sur stripe_promotion_code_id, pour lookup webhook)

Règles :

- Créés exclusivement par un super-admin via `POST /api/v1/super-admin/promo-codes`
- `uses_count` incrémenté atomiquement via UPDATE conditionnel `WHERE uses_count < max_uses` — 0 rows = épuisé, retourne 409 PROMO_CODE_EXHAUSTED
- `active = false` (désactivation manuelle SUPER_ADMIN) empêche toute nouvelle redemption — pour STRIPE_DISCOUNT, propage l'`active=false` au PromotionCode Stripe (best-effort, log warning si KO)
- **TRIAL_EXTENSION** : aucune interaction Stripe, extension d'essai 100 % locale via UPDATE direct sur `subscriptions.expires_at`. L'utilisateur saisit le code dans `/workspace/billing`
- **STRIPE_DISCOUNT** : Coupon + PromotionCode créés via Stripe API à la création locale, l'utilisateur saisit le code dans Stripe Checkout (champ natif activé via `allow_promotion_codes=true`), webhook `customer.discount.created` synchronise les redemptions localement
- À la redemption sur l'endpoint user `/workspace/billing/promo-codes/redeem`, un code STRIPE_DISCOUNT retourne 409 PROMO_CODE_TYPE_NOT_SUPPORTED_YET (volontaire — conservé après SF-04 pour orienter le user vers Stripe Checkout)

## promo_code_redemptions

Table (F-255 SF-255-01 migration 300, F-255 SF-255-04 migration 302) :

id (UUID PK)
workspace_id (UUID FK → workspaces)
promo_code_id (UUID FK → promo_codes)
code_at_redemption (varchar 64, non null — copie immuable du code au moment T pour audit, même si le code source est renommé en V2)
type (varchar 20 — copie du type au moment T)
value_applied_days (int, nullable — copie de la valeur appliquée pour TRIAL_EXTENSION)
value_applied_amount (int, nullable — centimes EUR de réduction réellement appliquée pour STRIPE_DISCOUNT, renseigné par le webhook)
source_event_id (varchar 255, nullable UNIQUE — event.getId() Stripe pour idempotence webhook, NULL pour les redemptions TRIAL_EXTENSION)
redeemed_at (timestamptz, non null)
applied_by_user_id (UUID FK → users — utilisateur authentifié qui a redeemé ou owner du workspace pour les webhooks Stripe)

Index :

idx_promo_code_redemptions_promo_code_id (sur promo_code_id, pour recalcul uses_count)
ux_promo_code_redemptions_workspace_trial (UNIQUE sur (workspace_id, type) WHERE type='TRIAL_EXTENSION', PostgreSQL uniquement — matérialise l'invariant anti-abus 1 TRIAL_EXTENSION par workspace à vie ; vérif applicative équivalente dans PromoCodeService pour les tests H2)
ux_promo_code_redemptions_source_event_id (UNIQUE sur source_event_id, pour idempotence webhook Stripe — pas de double INSERT si Stripe ré-émet le même event)

Règles :

- Une ligne par redemption, immuable (pas d'UPDATE/DELETE)
- `workspace_id` dérivé du contexte de sécurité (TRIAL_EXTENSION : path utilisateur authentifié ; STRIPE_DISCOUNT : `subscription.workspace_id` retrouvé depuis `customer_id` Stripe)
- `applied_by_user_id` = utilisateur authentifié (TRIAL_EXTENSION) ou owner du workspace (STRIPE_DISCOUNT webhook, fallback `createdByUserId` du code promo si owner null)
- `code_at_redemption` + `type` + `value_applied_*` = copies de sûreté pour réconciliation a posteriori
- `source_event_id` = clé d'idempotence pour les webhooks Stripe (Stripe peut ré-émettre le même event)

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
raison (TEXT, nullable — justification Claude lors d'une requalification, migration 044)
critere_code (VARCHAR 50, nullable — code du critère F-DT-08 ou F-DT-09 si le point s'y rattache, migration 068 SF-IA-03-02 / 071 SF-IA-03-05)
expected_value (VARCHAR 50, nullable — valeur attendue pour critère énuméré type DT09_TYPE_RUPTURE, migration 071 SF-IA-03-05)
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

# 25e — Checklist pièces immigration (F-IM-01)

Table :

immigration_piece_checks

Champs :

id (UUID PK — généré par JPA)
case_file_id (UUID FK → case_files, cascade delete)
titre_type (VARCHAR 50, non nullable — VISA_ETUDIANT | TITRE_SALARIE | REGROUPEMENT_FAMILIAL | NATURALISATION)
country (VARCHAR 20, non nullable — FRANCE | BELGIQUE)
label (VARCHAR 255, non nullable — libellé de la pièce selon le référentiel)
statut (VARCHAR 20, non nullable, défaut INCONNU — PRESENT | ABSENT | INCONNU)
created_at (timestamptz, non nullable)
updated_at (timestamptz, non nullable)

Contrainte :

UNIQUE(case_file_id, titre_type, country, label) — garantit l'idempotence du PUT

Règles :

- Scopé DROIT_IMMIGRATION uniquement — 400 si legalDomain différent
- Accès contrôlé via workspace du dossier (isolation workspace)
- Le référentiel des pièces est statique (ImmigrationPieceReferentiel Java) — pas de table de configuration
- GET fusionne le référentiel et l'historique persisté (pièces sans historique → INCONNU)
- PUT ignore les labels absents du référentiel pour le type/pays demandé
- Migration : 047-create-immigration-piece-checks.xml

Index :

idx_immigration_piece_checks_case_file_id

---

## immigration_title_decisions

Résultat de l'arbre décisionnel type de titre de séjour — 1:1 avec un dossier. Stocke les critères du questionnaire et les titres recommandés sous forme JSON TEXT.

```
immigration_title_decisions
  id                    UUID PK
  case_file_id          UUID FK → case_files(id)  UNIQUE
  country               VARCHAR(20) NOT NULL       -- FRANCE ou BELGIQUE
  nationalite_ue        BOOLEAN NOT NULL
  motif                 VARCHAR(30) NOT NULL       -- TRAVAIL, ETUDES, FAMILLE, ASILE, AUTRE
  duree                 VARCHAR(20) NOT NULL       -- COURT_SEJOUR, LONG_SEJOUR
  situation_familiale   VARCHAR(30)                -- CELIBATAIRE, MARIE, PACS_COHABITATION (nullable)
  recommended_titles    TEXT NOT NULL               -- JSON array des TitleRecommendation
  created_at            TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at            TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

uq_immigration_title_decisions_case_file (case_file_id) — un seul résultat par dossier

Index :

idx_immigration_title_decisions_case_file

---

## immigration_recours

Document de recours immigration généré — 1:1 avec un dossier. Stocke les données du requérant, la décision contestée, l'exposé des faits et le document structuré généré sous forme JSON TEXT.

```
immigration_recours
  id                       UUID PK
  case_file_id             UUID FK → case_files(id)  UNIQUE
  recours_type             VARCHAR(50) NOT NULL       -- code du type de recours
  date_notification        DATE NOT NULL              -- date de notification du refus
  date_limite              DATE NOT NULL              -- date notification + délai légal
  requerant_data           TEXT NOT NULL              -- JSON (nom, prénom, nationalité, adresse)
  decision_contestee_data  TEXT NOT NULL              -- JSON (autorité, date, référence)
  expose_faits             TEXT                       -- texte libre (nullable)
  generated_document       TEXT NOT NULL              -- JSON du document structuré complet
  created_at               TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at               TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

uq_immigration_recours_case_file (case_file_id) — un seul recours par dossier

Index :

idx_immigration_recours_case_file

---

## immigration_work_rights

Analyse du droit au travail par titre de séjour — 1:1 avec un dossier. Stocke le titre analysé et le résultat structuré (droit, conditions, obligations employeur) sous forme JSON TEXT.

```
immigration_work_rights
  id              UUID PK
  case_file_id    UUID FK → case_files(id)  UNIQUE
  titre_type      VARCHAR(50) NOT NULL
  country         VARCHAR(20) NOT NULL
  result_data     TEXT NOT NULL              -- JSON du WorkRightResult
  created_at      TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

uq_immigration_work_rights_case_file (case_file_id) — une seule analyse par dossier

Index :

idx_immigration_work_rights_case_file

---

## regime_communaute_legale_be_analyses

F-217 SF-217-01 — analyse de l'outil décisionnel « régime matrimonial communauté légale BE » (droit de la famille belge), 1:1 avec un dossier. Stocke les entrées de l'avocat et le résultat calculé (qualification des biens et des dettes en propres / communs) sous forme JSON TEXT (compatible H2 + PostgreSQL).

```
regime_communaute_legale_be_analyses
  id              UUID PK
  case_file_id    UUID FK → case_files(id)  UNIQUE
  snapshot_data   TEXT NOT NULL              -- JSON : inputs + résultat calculé
  country         VARCHAR(20) NOT NULL
  created_at      TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

uq_regime_communaute_legale_be_analyses_case_file (case_file_id) — une seule analyse par dossier

Index :

idx_regime_communaute_legale_be_analyses_case_file

Migration : 233-create-regime-communaute-legale-be-analyses.xml

---

## liquidation_partage_be_analyses

F-217 SF-217-02 — analyse de l'outil décisionnel « liquidation-partage BE » (droit de la famille belge), 1:1 avec un dossier. Stocke les entrées de l'avocat et le résultat calculé (liquidation-partage post-divorce, méthode Renard, délais du Code judiciaire) sous forme JSON TEXT (compatible H2 + PostgreSQL).

```
liquidation_partage_be_analyses
  id              UUID PK
  case_file_id    UUID FK → case_files(id)  UNIQUE
  snapshot_data   TEXT NOT NULL              -- JSON : inputs + résultat calculé
  country         VARCHAR(20) NOT NULL
  created_at      TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

uq_liquidation_partage_be_analyses_case_file (case_file_id) — une seule analyse par dossier

Index :

idx_liquidation_partage_be_analyses_case_file

Migration : 234-create-liquidation-partage-be-analyses.xml

---

## autorite_parentale_be_analyses

F-217 SF-217-04 — analyse de l'outil décisionnel « autorité parentale BE » (Code civil belge art. 374-375 — autorité conjointe / exclusive, déchéance), 1:1 avec un dossier. Stocke les entrées de l'avocat et le résultat calculé sous forme JSON TEXT.

```
autorite_parentale_be_analyses
  id              UUID PK
  case_file_id    UUID FK → case_files(id)  UNIQUE
  snapshot_data   TEXT NOT NULL              -- JSON : inputs + résultat calculé
  country         VARCHAR(20) NOT NULL
  created_at      TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

uq_autorite_parentale_be_analyses_case_file (case_file_id) — une seule analyse par dossier

Index :

idx_autorite_parentale_be_analyses_case_file

Migration : 237-create-autorite-parentale-be-analyses.xml

---

## cce_annulation_be_analyses

F-215 SF-215-13 — analyse de l'outil décisionnel « Recours CCE annulation 30 jours BE » (`F-IM-31-cce-annulation-30j-be`, BELGIQUE). Calculateur de délais du recours en annulation devant le **Conseil du Contentieux des Étrangers** (art. 39/2 §2 et 39/82 §4 al. 1 Loi du 15/12/1980 + loi du 15/09/2006) — 30 jours **calendaires** depuis la notification. 1:1 avec un dossier. Stocke les inputs métier dénormalisés (pour requête/index) + le snapshot JSON complet (inputs + résultat calculé) dans `result_data`.

```
cce_annulation_be_analyses
  id                          UUID PK
  case_file_id                UUID FK → case_files(id)  UNIQUE
  date_notification_decision  DATE NOT NULL
  type_decision               VARCHAR(30) NOT NULL  -- enum 7 valeurs (REFUS_TITRE … AUTRE)
  recours_forme               BOOLEAN NOT NULL
  date_recours                DATE                  -- nullable (si recours déjà formé)
  statut                      VARCHAR(20) NOT NULL  -- DISPONIBLE / URGENT / EXPIRE / RECOURS_FORME
  country                     VARCHAR(20) NOT NULL DEFAULT 'BELGIQUE'
  result_data                 TEXT NOT NULL         -- JSON : inputs + résultat calculé
  created_at                  TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

uq_cce_annulation_be_case_file (case_file_id) — une seule analyse par dossier

Index :

idx_cce_annulation_be_case_file

Migration : 439-create-cce-annulation-be-analyses.xml (+ 440-seed-cce-annulation-be-visibility.xml — visibility CONTEXTUAL `recours_cce_envisage=true` priority 206)

---

## cce_extreme_urgence_be_analyses

F-215 SF-215-15 — analyse de l'outil décisionnel « Recours CCE extrême urgence 5 jours BE » (`F-IM-32-cce-extreme-urgence-5j-be`, BELGIQUE). Calculateur de délais du recours en extrême urgence devant le **Conseil du Contentieux des Étrangers** (art. 39/82 §4 al. 2-3 Loi du 15/12/1980) — 5 jours **ouvrables** (réutilise `BelgianBusinessDaysCalculator`). 1:1 avec un dossier.

```
cce_extreme_urgence_be_analyses
  id                    UUID PK
  case_file_id          UUID FK → case_files(id)  UNIQUE
  date_acte_executoire  DATE NOT NULL
  type_acte             VARCHAR(30) NOT NULL  -- enum 5 valeurs (OQT_EXECUTE … AUTRE)
  recours_forme         BOOLEAN NOT NULL
  date_recours          DATE                  -- nullable
  statut                VARCHAR(20) NOT NULL  -- DISPONIBLE / CRITIQUE / EXPIRE / RECOURS_FORME
  country               VARCHAR(20) NOT NULL DEFAULT 'BELGIQUE'
  result_data           TEXT NOT NULL         -- JSON : inputs + résultat (dateLimiteRecours, joursOuvrablesRestants, audienceEstimee, actionImmediate)
  created_at            TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at            TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes : uq_cce_extreme_urgence_be_case_file (case_file_id). Index : idx_cce_extreme_urgence_be_case_file.

Migration : 441-create-cce-extreme-urgence-be-analyses.xml (+ 442-seed-cce-extreme-urgence-be-visibility.xml — visibility CONTEXTUAL `recours_cce_extreme_urgence=true` priority 207)

---

## annexe13quinquies_be_analyses

F-215 SF-215-17 — analyse de l'outil décisionnel « Annexe 13quinquies OQT + interdiction d'entrée BE » (`F-IM-33-annexe13quinquies-ie-be`, BELGIQUE). Calcule la durée de l'interdiction d'entrée (3/5/8 ans selon motif, art. 74/11 §2 Loi du 15/12/1980), la date de levée précoce (2/3 du délai, art. 74/12) et le délai de recours en annulation (30 jours calendaires). 1:1 avec un dossier. Distinct de `annexe13_be_analyses` (Annexe 13 simple sans IE).

```
annexe13quinquies_be_analyses
  id                         UUID PK
  case_file_id               UUID FK → case_files(id)  UNIQUE
  date_notification_annexe   DATE NOT NULL
  motif_interdiction_entree  VARCHAR(30) NOT NULL  -- enum 5 valeurs (SEJOUR_IRREGULIER … DECISION_JUDICIAIRE)
  precedent_sejour           BOOLEAN NOT NULL
  duree_interdiction         INTEGER NOT NULL      -- années (3/5/8)
  recours_forme              BOOLEAN NOT NULL
  date_recours               DATE                  -- nullable
  statut                     VARCHAR(20) NOT NULL  -- DISPONIBLE / URGENT / EXPIRE / FORME
  country                    VARCHAR(20) NOT NULL DEFAULT 'BELGIQUE'
  result_data                TEXT NOT NULL         -- JSON : inputs + résultat (dateFinInterdiction, datePossibleLevePrecoce, dateLimiteRecoursAnnulation, conditionsLevee)
  created_at                 TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at                 TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes : uq_annexe13quinquies_be_case_file (case_file_id). Index : idx_annexe13quinquies_be_case_file.

Migration : 443-create-annexe13quinquies-be-analyses.xml (+ 444-seed-annexe13quinquies-be-visibility.xml — visibility CONTEXTUAL `interdiction_entree_be_detectee=true` priority 208)

---

## protection_temporaire_ukraine_be_analyses

F-215 SF-215-19 — analyse de l'outil décisionnel « Protection temporaire Ukraine BE » (`F-IM-34-protection-temporaire-ukraine-be`, BELGIQUE). Évalue l'éligibilité (directive 2001/55/CE + décision UE 2022/382), la durée de protection restante (`date_fin_protection` paramétrable `protection.temporaire.ukraine.date-fin`, défaut `2027-03-04` à actualiser annuellement) et les droits associés (travail immédiat sans single permit art. 57/29 Loi du 15/12/1980, CPAS, logement, enseignement). 1:1 avec un dossier.

```
protection_temporaire_ukraine_be_analyses
  id                                  UUID PK
  case_file_id                        UUID FK → case_files(id)  UNIQUE
  date_arrivee                        DATE NOT NULL
  nationalite_ukrainienne             BOOLEAN NOT NULL
  residence_ukraine_avant_24fev2022   BOOLEAN NOT NULL
  apatrides_ukraine                   BOOLEAN               -- nullable
  membre_famille_protege              BOOLEAN               -- nullable
  titre_sejour_be                     VARCHAR(30) NOT NULL  -- enum 5 valeurs (AUCUN … TITRE_AUTRE)
  eligible                            BOOLEAN NOT NULL
  date_fin_protection                 DATE NOT NULL
  duree_protection_restante           BIGINT NOT NULL       -- jours restants
  country                             VARCHAR(20) NOT NULL DEFAULT 'BELGIQUE'
  result_data                         TEXT NOT NULL         -- JSON : inputs + résultat (droitsTravail, droitsAides, cheminProcedure, prochainRenouvellement)
  created_at                          TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at                          TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes : uq_protection_temporaire_ukraine_be_case_file (case_file_id). Index : idx_protection_temporaire_ukraine_be_case_file.

Migration : 445-create-protection-temporaire-ukraine-be-analyses.xml (+ 446-seed-protection-temporaire-ukraine-be-visibility.xml — visibility CONTEXTUAL `protection_temporaire_ukraine_detectee=true` priority 209)

---

## contribution_alimentaire_enfants_be_analyses

F-217 SF-217-06 — analyse de l'outil décisionnel « contribution alimentaire pour enfants BE » (méthode Renard, Code civil belge art. 203 / 203bis), 1:1 avec un dossier. Stocke les entrées de l'avocat et le résultat calculé (estimation indicative) sous forme JSON TEXT.

```
contribution_alimentaire_enfants_be_analyses
  id              UUID PK
  case_file_id    UUID FK → case_files(id)  UNIQUE
  snapshot_data   TEXT NOT NULL              -- JSON : inputs + résultat calculé
  country         VARCHAR(20) NOT NULL
  created_at      TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

uq_contribution_alimentaire_enfants_be_analyses_case_file (case_file_id) — une seule analyse par dossier

Index :

idx_contribution_alimentaire_enfants_be_analyses_case_file

Migration : 239-create-contribution-alimentaire-enfants-be-analyses.xml

---

## contribution_conjoint_be_analyses

F-217 SF-217-08 — analyse de l'outil décisionnel « pension alimentaire entre ex-époux BE » (Code civil belge art. 301), 1:1 avec un dossier. Stocke les entrées de l'avocat et le résultat calculé (estimation indicative) sous forme JSON TEXT.

```
contribution_conjoint_be_analyses
  id              UUID PK
  case_file_id    UUID FK → case_files(id)  UNIQUE
  snapshot_data   TEXT NOT NULL              -- JSON : inputs + résultat calculé
  country         VARCHAR(20) NOT NULL
  created_at      TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at      TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

uq_contribution_conjoint_be_analyses_case_file (case_file_id) — une seule analyse par dossier

Index :

idx_contribution_conjoint_be_analyses_case_file

Migration : 241-create-contribution-conjoint-be-analyses.xml

---

## aes_presence_prouvee_analyses

F-214 SF-214-11 — analyse de l'outil décisionnel « AES calcul présence prouvée » (`F-IM-30-aes-presence-prouvee-fr`, FRANCE). Calcule le nombre d'années de présence effective justifiée par pièces recevables (circulaire Valls 28/11/2012) et l'éligibilité aux 4 voies AES (famille 5 ans, humanitaire 10 ans, étudiant 3 ans, métiers en tension 3 ans — L. 435-1 / L. 435-3 CESEDA). 1:1 avec un dossier. Périodes saisies/fusionnées, gaps, éligibilité par voie persistés en JSON dans `result_data`.

```
aes_presence_prouvee_analyses
  id                       UUID PK
  case_file_id             UUID FK → case_files(id)  UNIQUE (inline)
  annees_totales_prouvees  INT NOT NULL
  mois_totaux_prouves      INT NOT NULL
  country                  VARCHAR(20) NOT NULL
  result_data              TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at               TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at               TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_aes_presence_prouvee_case_file) — une seule analyse par dossier

Migration : 456-create-aes-presence-prouvee-analyses.xml (+ 457-seed-aes-presence-prouvee-visibility.xml — visibility CONTEXTUAL priority 30)

---

## aj_cnda_analyses

F-214 SF-214-19 — analyse de l'outil décisionnel « AJ CNDA — éligibilité et délais » (`F-IM-34-aj-cnda-fr`, FRANCE). Vérifie l'éligibilité à l'aide juridictionnelle devant la CNDA sous condition de ressources (loi n° 91-647 du 10/07/1991 ; décret n° 2020-1717) et calcule les délais — recours CNDA (1 mois, 15 j en procédure accélérée, L. 532-4 CESEDA) et demande d'AJ (15 j). 1:1 avec un dossier.

```
aj_cnda_analyses
  id                            UUID PK
  case_file_id                  UUID FK → case_files(id)  UNIQUE (inline)
  date_decision_ofpra           DATE NOT NULL
  ressources_mensuelles_nettes  DOUBLE PRECISION NOT NULL
  procedure_acceleree           BOOLEAN NOT NULL
  demande_aj_deposee            BOOLEAN NOT NULL
  date_depot_aj                 DATE                       -- nullable
  eligible_aj                   BOOLEAN NOT NULL
  statut                        VARCHAR(30) NOT NULL
  country                       VARCHAR(20) NOT NULL
  result_data                   TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at                    TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at                    TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_aj_cnda_case_file) — une seule analyse par dossier

Migration : 464-create-aj-cnda-analyses.xml (+ 465-seed-aj-cnda-visibility.xml — visibility CONTEXTUAL priority 34)

---

## anef_procedure_analyses

F-214 SF-214-25 — analyse de l'outil décisionnel « ANEF procédure / pannes / recours » (`F-IM-37-anef-procedure-fr`, FRANCE). Guide les démarches ANEF (R. 311-2-2 CESEDA ; arrêté 27/04/2021) et les recours en cas de panne du dépôt dématérialisé. Calcule le statut (NORMAL / URGENT si expiration < 30 j / PANNE_EN_COURS / RECOURS_POSSIBLE), les étapes alternatives (preuve panne, LRAR préfecture, dépôt physique) et le délai de recours pour faute (2 ans, L. 114-9 CRPA). 1:1 avec un dossier.

```
anef_procedure_analyses
  id                            UUID PK
  case_file_id                  UUID FK → case_files(id)  UNIQUE (inline)
  type_titre_concerne           VARCHAR(120)               -- nullable
  date_expiration_titre         DATE NOT NULL
  pannee_anef_signalee          BOOLEAN NOT NULL
  date_tentative_depot          DATE                       -- nullable
  demande_adressee_prefecture   BOOLEAN NOT NULL
  statut                        VARCHAR(20) NOT NULL
  country                       VARCHAR(20) NOT NULL
  result_data                   TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at                    TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at                    TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_anef_procedure_case_file) — une seule analyse par dossier

Migration : 470-create-anef-procedure-analyses.xml (+ 471-seed-anef-procedure-visibility.xml — visibility CONTEXTUAL priority 37)

---

## appel_caa_cassation_analyses

F-214 SF-214-33 — analyse de l'outil décisionnel « Appel CAA / cassation CE délais » (`F-IM-41-appel-caa-cassation-ce-fr`, FRANCE). Calcule le délai d'appel devant la CAA (1 mois de droit commun, 15 j en OQTF sans délai de départ) et rappelle le délai de cassation devant le CE (2 mois, R. 821-1 CJA) + filtre d'admission des pourvois OQTF (L. 821-2 CJA). 1:1 avec un dossier.

```
appel_caa_cassation_analyses
  id                  UUID PK
  case_file_id        UUID FK → case_files(id)  UNIQUE (inline)
  date_jugement_ta    DATE NOT NULL
  type_decision_ta    VARCHAR(20) NOT NULL
  type_contentieux    VARCHAR(20) NOT NULL
  delai_special_oqtf  BOOLEAN NOT NULL
  statut              VARCHAR(20) NOT NULL
  country             VARCHAR(20) NOT NULL
  result_data         TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at          TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at          TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_appel_caa_cassation_case_file) — une seule analyse par dossier

Migration : 478-create-appel-caa-cassation-analyses.xml (+ 479-seed-appel-caa-cassation-visibility.xml — visibility CONTEXTUAL priority 41)

---

## assignation_residence_analyses

F-214 SF-214-35 — analyse de l'outil décisionnel « Assignation à résidence L. 731-1 » (`F-IM-42-assignation-residence-fr`, FRANCE). Analyse la validité d'une assignation à résidence (alternative à la rétention), calcule la durée maximale légale (45 j renouvelable 2 fois = 135 j, L. 731-1 CESEDA) et génère les moyens de contestation devant le TA (recours 48 h). 1:1 avec un dossier.

```
assignation_residence_analyses
  id                             UUID PK
  case_file_id                   UUID FK → case_files(id)  UNIQUE (inline)
  date_notification_assignation  DATE NOT NULL
  duree_assignation_jours        INT NOT NULL
  motif_assignation              VARCHAR(40) NOT NULL
  obligations_presentation       TEXT                       -- nullable
  statut                         VARCHAR(20) NOT NULL
  country                        VARCHAR(20) NOT NULL
  result_data                    TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at                     TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at                     TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_assignation_residence_case_file) — une seule analyse par dossier

Migration : 480-create-assignation-residence-analyses.xml (+ 481-seed-assignation-residence-visibility.xml — visibility CONTEXTUAL priority 42)

---

## autorisation_travail_employeur_analyses

F-214 SF-214-43 — analyse de l'outil décisionnel « Autorisation de travail employeur » (`F-IM-46-autorisation-travail-employeur-fr`, FRANCE). Analyse les obligations de l'employeur recrutant un travailleur étranger hors UE (autorisation préalable L. 5221-1 Code du travail), détermine si une autorisation est requise selon la nationalité, le délai d'instruction OFII (2 mois, R. 5221-20) et le délai de recours en cas de refus. 1:1 avec un dossier.

```
autorisation_travail_employeur_analyses
  id                       UUID PK
  case_file_id             UUID FK → case_files(id)  UNIQUE (inline)
  type_contrat             VARCHAR(20) NOT NULL
  poste_proposes           VARCHAR(200) NOT NULL
  nationalite_candidat     VARCHAR(100) NOT NULL
  duree_contrat_mois       INTEGER                    -- nullable
  refus_autorisation       BOOLEAN NOT NULL
  date_refus_autorisation  DATE                       -- nullable
  statut                   VARCHAR(30) NOT NULL
  country                  VARCHAR(20) NOT NULL
  result_data              TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at               TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at               TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_autorisation_travail_employeur_case_file) — une seule analyse par dossier

Migration : 488-create-autorisation-travail-employeur-analyses.xml (+ 489-seed-autorisation-travail-employeur-visibility.xml — visibility ALWAYS_ON priority 46)

---

## carte_resident_analyses

F-214 SF-214-23 — analyse de l'outil décisionnel « Carte de résident 10 ans L. 426-1 » (`F-IM-36-carte-resident-l4261-fr`, FRANCE). Analyse l'éligibilité à la carte de résident 10 ans (≥ 5 ans de séjour régulier, intégration MOYEN/FORT, ressources ≥ SMIC ; L. 426-1 CESEDA ; loi 26/01/2024). Une condamnation pénale grave rend la demande irrecevable. 1:1 avec un dossier.

```
carte_resident_analyses
  id                            UUID PK
  case_file_id                  UUID FK → case_files(id)  UNIQUE (inline)
  duree_sejour_regulier_annees  INT NOT NULL
  types_titres_anterieurs       VARCHAR(500)               -- nullable
  niveau_integration            VARCHAR(20) NOT NULL
  ressources_mensuelles_nettes  DOUBLE NOT NULL
  condamnations_penales_graves  BOOLEAN NOT NULL
  country                       VARCHAR(20) NOT NULL
  result_data                   TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at                    TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at                    TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_carte_resident_case_file) — une seule analyse par dossier

Migration : 468-create-carte-resident-analyses.xml (+ 469-seed-carte-resident-visibility.xml — visibility CONTEXTUAL priority 36)

---

## itf_judiciaire_analyses

F-214 SF-214-37 — analyse de l'outil décisionnel « ITF judiciaire » (`F-IM-43-itf-judiciaire-fr`, FRANCE). Analyse une interdiction du territoire français prononcée par un juge pénal comme peine complémentaire (C. pén. 131-30 à 131-30-2), ses voies de recours (appel 10 j, cassation 5 j) et la requête en relèvement (C. pén. 702-1, 5 ans minimum). Distinct de l'IRTF administrative (F-IM-20). 1:1 avec un dossier.

```
itf_judiciaire_analyses
  id                      UUID PK
  case_file_id            UUID FK → case_files(id)
  date_condamnation       DATE NOT NULL
  duree_itf_annees        INT NOT NULL
  infraction_principale   VARCHAR(200)               -- nullable
  condamnation_definitive BOOLEAN NOT NULL
  statut                  VARCHAR(20) NOT NULL
  country                 VARCHAR(20) NOT NULL
  result_data             TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at              TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at              TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

uq_itf_judiciaire_analyses_case_file (case_file_id) — une seule analyse par dossier

Index :

idx_itf_judiciaire_analyses_case_file

Migration : 482-create-itf-judiciaire-analyses.xml (+ 483-seed-itf-judiciaire-visibility.xml — visibility CONTEXTUAL priority 43)

---

## mna_evaluation_age_analyses

F-214 SF-214-27 — analyse de l'outil décisionnel « MNA évaluation d'âge » (`F-IM-38-mna-evaluation-age-fr`, FRANCE). Guide la procédure d'évaluation d'âge d'un mineur non accompagné refusé par l'ASE, les recours devant le juge des enfants et la contestation des examens osseux. 1:1 avec un dossier.

```
mna_evaluation_age_analyses
  id                      UUID PK
  case_file_id            UUID FK → case_files(id)
  date_naissance_declaree DATE NOT NULL
  evaluation_ase_refusee  BOOLEAN NOT NULL DEFAULT false
  date_refus_ase          DATE                       -- nullable
  examen_osseux_ordonne   BOOLEAN NOT NULL DEFAULT false
  statut                  VARCHAR(40) NOT NULL
  country                 VARCHAR(20) NOT NULL
  result_data             TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at              TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at              TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

uq_mna_evaluation_age_analyses_case_file (case_file_id) — une seule analyse par dossier

Index :

idx_mna_evaluation_age_analyses_case_file

Migration : 472-create-mna-evaluation-age-analyses.xml (+ 473-seed-mna-evaluation-age-visibility.xml — visibility CONTEXTUAL priority 38)

---

## naturalisation_recours_ta_analyses

F-214 SF-214-31 — analyse de l'outil décisionnel « Recours TA Nantes refus de naturalisation par décret » (`F-IM-40-naturalisation-recours-ta-fr`, FRANCE). Calcule le délai de recours pour excès de pouvoir devant le TA de Nantes (compétence exclusive nationale, R. 312-4 CJA) contre un refus de naturalisation par décret — 2 mois (Cciv 21-15 ; L. 213-1 CJA). Procédure ADMINISTRATIVE, distincte du recours TJ (SF-214-29). 1:1 avec un dossier.

```
naturalisation_recours_ta_analyses
  id                 UUID PK
  case_file_id       UUID FK → case_files(id)  UNIQUE (inline)
  date_refus_decret  DATE NOT NULL
  recours_prerequis  BOOLEAN NOT NULL DEFAULT false
  statut             VARCHAR(20) NOT NULL
  country            VARCHAR(20) NOT NULL
  result_data        TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at         TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at         TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_naturalisation_recours_ta_case_file) — une seule analyse par dossier

Migration : 476-create-naturalisation-recours-ta-analyses.xml (+ 477-seed-naturalisation-recours-ta-visibility.xml — visibility CONTEXTUAL priority 40)

---

## naturalisation_recours_tj_analyses

F-214 SF-214-29 — analyse de l'outil décisionnel « Recours TJ refus déclaration de nationalité » (`F-IM-39-naturalisation-recours-tj-fr`, FRANCE). Calcule le délai de recours devant le tribunal judiciaire contre un refus de déclaration de nationalité française — 6 mois (Cciv 26-3 ; CPC 1043). Procédure CIVILE, distincte du recours décret porté devant le TA de Nantes (SF-214-31). 1:1 avec un dossier.

```
naturalisation_recours_tj_analyses
  id                      UUID PK
  case_file_id            UUID FK → case_files(id)  UNIQUE (inline)
  voie_naturalisation     VARCHAR(30) NOT NULL
  date_refus_declaration  DATE NOT NULL
  type_refus              VARCHAR(30) NOT NULL
  statut                  VARCHAR(20) NOT NULL
  country                 VARCHAR(20) NOT NULL
  result_data             TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at              TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at              TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_naturalisation_recours_tj_case_file) — une seule analyse par dossier

Migration : 474-create-naturalisation-recours-tj-analyses.xml (+ 475-seed-naturalisation-recours-tj-visibility.xml — visibility CONTEXTUAL priority 39)

---

## ofpra_introduction_analyses

F-214 SF-214-17 — analyse de l'outil décisionnel « Demande OFPRA introduction (GUDA/ADA) » (`F-IM-33-ofpra-introduction-fr`, FRANCE). Guide la procédure d'introduction de la demande d'asile via le GUDA et l'ADA, calcule le délai d'introduction de 90 jours depuis l'arrivée (R. 521-1 CESEDA), les étapes/pièces requises et le risque de procédure accélérée (pays sûr, L. 531-24). 1:1 avec un dossier.

```
ofpra_introduction_analyses
  id                     UUID PK
  case_file_id           UUID FK → case_files(id)  UNIQUE (inline)
  date_arrivee_en_france DATE NOT NULL
  passage_guda_effectue  BOOLEAN NOT NULL
  date_passage_guda      DATE                       -- nullable
  ada_requise            BOOLEAN NOT NULL
  pays_origine           VARCHAR(120)               -- nullable
  statut_delai           VARCHAR(20) NOT NULL
  country                VARCHAR(20) NOT NULL
  result_data            TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at             TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at             TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_ofpra_introduction_case_file) — une seule analyse par dossier

Migration : 462-create-ofpra-introduction-analyses.xml (+ 463-seed-ofpra-introduction-visibility.xml — visibility CONTEXTUAL priority 33)

---

## oqtf_categories_analyses

F-214 SF-214-09 — analyse de l'outil décisionnel « OQTF catégories L. 611-1 » (`F-IM-29-oqtf-categories-l6111-fr`, FRANCE). Détermine la catégorie d'OQTF parmi les 7 de L. 611-1 (1° à 7°) et les moyens de défense spécifiques. Complémentaire de F-IM-08 (OQTF avec/sans délai). 1:1 avec un dossier.

```
oqtf_categories_analyses
  id                      UUID PK
  case_file_id            UUID FK → case_files(id)
  categorie_l611          VARCHAR(10) NOT NULL
  date_notification_oqtf  DATE NOT NULL
  motif_oqtf              VARCHAR(300)               -- nullable
  country                 VARCHAR(20) NOT NULL
  result_data             TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at              TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at              TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

uq_oqtf_categories_analyses_case_file (case_file_id) — une seule analyse par dossier

Index :

idx_oqtf_categories_analyses_case_file

Migration : 454-create-oqtf-categories-analyses.xml (+ 455-seed-oqtf-categories-visibility.xml — visibility CONTEXTUAL priority 29)

---

## recepisse_attestation_analyses

F-214 SF-214-15 — analyse de l'outil décisionnel « Récépissé vs attestation de prolongation » (`F-IM-32-recepisse-attestation-fr`, FRANCE). Distingue les droits attachés au récépissé (séjour + travail, R. 311-4) vs l'attestation de prolongation d'instruction (séjour seul, PAS de droit au travail, R. 311-6). Confusion source de sanctions employeur (L. 8253-1 Code du travail). 1:1 avec un dossier.

```
recepisse_attestation_analyses
  id                            UUID PK
  case_file_id                  UUID FK → case_files(id)  UNIQUE (inline)
  type_document                 VARCHAR(30) NOT NULL
  date_delivrance               DATE                       -- nullable
  date_expiration               DATE                       -- nullable
  mention_autorisation_travail  BOOLEAN                    -- nullable
  country                       VARCHAR(20) NOT NULL
  result_data                   TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at                    TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at                    TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_recepisse_attestation_case_file) — une seule analyse par dossier

Migration : 460-create-recepisse-attestation-analyses.xml (+ 461-seed-recepisse-attestation-visibility.xml — visibility CONTEXTUAL priority 32)

---

## regroupement_familial_analyses

F-214 SF-214-03 — analyse de l'outil décisionnel « Regroupement familial L. 434-1+ » (`F-IM-26-regroupement-familial-fr`, FRANCE). Analyse l'éligibilité au regroupement familial (durée de séjour régulier ≥ 18 mois R. 434-4, ressources ≥ SMIC net pondéré R. 434-30, surface habitable minimale R. 434-20 ; L. 434-1 à L. 434-13 CESEDA). 1:1 avec un dossier.

```
regroupement_familial_analyses
  id                            UUID PK
  case_file_id                  UUID FK → case_files(id)  UNIQUE (inline)
  duree_sejour_regulier_mois    INT NOT NULL
  ressources_mensuelles_nettes  DOUBLE NOT NULL
  taille_logement_m2            INT NOT NULL
  nombre_personnes_foyer        INT NOT NULL
  type_regroupement             VARCHAR(20) NOT NULL
  membres_famille_a_regrouper   INT NOT NULL
  country                       VARCHAR(20) NOT NULL
  result_data                   TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at                    TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at                    TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_regroupement_familial_case_file) — une seule analyse par dossier

Migration : 448-create-regroupement-familial-analyses.xml (+ 449-seed-regroupement-familial-visibility.xml — visibility CONTEXTUAL priority 26)

---

## renouvellement_delai_analyses

F-214 SF-214-13 — analyse de l'outil décisionnel « Renouvellement — délai de dépôt 2 mois avant » (`F-IM-31-renouvellement-delai-depot-fr`, FRANCE). Calcule le délai optimal de dépôt du renouvellement (2 mois avant expiration, R. 433-1 CESEDA) et le seuil impératif (1 mois avant) ; alerte sur le risque d'interruption des droits. 1:1 avec un dossier.

```
renouvellement_delai_analyses
  id                     UUID PK
  case_file_id           UUID FK → case_files(id)  UNIQUE (inline)
  date_expiration_titre  DATE NOT NULL
  date_depot_dossier     DATE                       -- nullable
  type_titre             VARCHAR(120)               -- nullable
  statut                 VARCHAR(20) NOT NULL
  country                VARCHAR(20) NOT NULL
  result_data            TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at             TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at             TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_renouvellement_delai_case_file) — une seule analyse par dossier

Migration : 458-create-renouvellement-delai-analyses.xml (+ 459-seed-renouvellement-delai-visibility.xml — visibility ALWAYS_ON priority 31)

---

## retrait_titre_fraude_analyses

F-214 SF-214-41 — analyse de l'outil décisionnel « Retrait de titre pour fraude L. 412-7 » (`F-IM-45-retrait-titre-fraude-fr`, FRANCE). Analyse la validité d'un retrait de titre pour fraude (mariage gris, fausses déclarations, fraude documentaire), vérifie le contradictoire préalable obligatoire (L. 411-5 CESEDA), identifie les vices de procédure et calcule le délai de recours TA (2 mois). 1:1 avec un dossier.

```
retrait_titre_fraude_analyses
  id                         UUID PK
  case_file_id               UUID FK → case_files(id)  UNIQUE (inline)
  date_retrait               DATE NOT NULL
  motif_retrait              VARCHAR(40) NOT NULL
  mise_en_demeure_prealable  BOOLEAN NOT NULL
  date_mise_en_demeure       DATE                       -- nullable
  statut                     VARCHAR(20) NOT NULL
  country                    VARCHAR(20) NOT NULL
  result_data                TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at                 TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at                 TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_retrait_titre_fraude_case_file) — une seule analyse par dossier

Migration : 486-create-retrait-titre-fraude-analyses.xml (+ 487-seed-retrait-titre-fraude-visibility.xml — visibility CONTEXTUAL priority 45)

---

## ue_eee_suisse_sejour_analyses

F-214 SF-214-39 — analyse de l'outil décisionnel « Séjour UE/EEE/Suisse » (`F-IM-44-ue-eee-suisse-sejour-fr`, FRANCE). Analyse le droit au séjour des citoyens UE/EEE/Suisse et de leurs membres de famille (directive 2004/38/CE, L. 233-1+ CESEDA) : séjour automatique 3 mois (art. 6), au-delà sous conditions (art. 7), droit permanent après 60 mois (art. 16), carte « membre de famille de citoyen de l'Union » (art. 10). 1:1 avec un dossier.

```
ue_eee_suisse_sejour_analyses
  id                        UUID PK
  case_file_id              UUID FK → case_files(id)  UNIQUE (inline)
  nationalite               VARCHAR(255)               -- nullable
  est_citoyen_ue            BOOLEAN NOT NULL
  membre_famille_non_ue     BOOLEAN NOT NULL
  duree_sejour_mois         INT NOT NULL
  activite_professionnelle  VARCHAR(50) NOT NULL
  country                   VARCHAR(20) NOT NULL
  result_data               TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at                TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at                TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_ue_eee_suisse_sejour_case_file) — une seule analyse par dossier

Migration : 484-create-ue-eee-suisse-sejour-analyses.xml (+ 485-seed-ue-eee-suisse-sejour-visibility.xml — visibility CONTEXTUAL priority 44)

---

## victime_traite_analyses

F-214 SF-214-21 — analyse de l'outil décisionnel « Victime de la traite des êtres humains L. 425-1 » (`F-IM-35-victime-traite-l4251-fr`, FRANCE). Analyse l'éligibilité au titre L. 425-1 (plainte ou signalement par association agréée + collaboration OCRTEH + identification comme victime) ouvrant une APS 6 mois avec droit au travail. Distinct de L. 425-6 (violences conjugales, F-208). 1:1 avec un dossier.

```
victime_traite_analyses
  id                                  UUID PK
  case_file_id                        UUID FK → case_files(id)  UNIQUE (inline)
  plainte_deposee                     BOOLEAN NOT NULL
  collaboration_ocrteh                BOOLEAN NOT NULL
  date_plainte                        DATE                       -- nullable
  titre_actuel                        VARCHAR(120)               -- nullable
  presence_autorite_refugie_detectee  BOOLEAN NOT NULL
  country                             VARCHAR(20) NOT NULL
  result_data                         TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at                          TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at                          TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_victime_traite_case_file) — une seule analyse par dossier

Migration : 466-create-victime-traite-analyses.xml (+ 467-seed-victime-traite-visibility.xml — visibility CONTEXTUAL priority 35)

---

## vls_ts_validation_analyses

F-214 SF-214-07 — analyse de l'outil décisionnel « Validation VLS-TS OFII 3 mois » (`F-IM-28-vls-ts-validation-ofii-fr`, FRANCE). Calcule le délai de validation du VLS-TS (visa long séjour valant titre de séjour) auprès de l'OFII — 3 mois à compter de l'entrée en France (R. 311-3 CESEDA) — et alerte sur le risque d'irrégularité passé ce délai. 1:1 avec un dossier.

```
vls_ts_validation_analyses
  id                       UUID PK
  case_file_id             UUID FK → case_files(id)  UNIQUE (inline)
  date_entree_france       DATE NOT NULL
  type_vls_ts              VARCHAR(30) NOT NULL
  validation_ofii_effectuee BOOLEAN NOT NULL
  date_validation_ofii     DATE                       -- nullable
  statut                   VARCHAR(20) NOT NULL
  country                  VARCHAR(20) NOT NULL
  result_data              TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at               TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at               TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_vls_ts_validation_case_file) — une seule analyse par dossier

Migration : 452-create-vls-ts-validation-analyses.xml (+ 453-seed-vls-ts-validation-visibility.xml — visibility ALWAYS_ON priority 28)

---

## vpf_liens_personnels_analyses

F-214 SF-214-05 — analyse de l'outil décisionnel « VPF liens personnels et familiaux L. 423-23 » (`F-IM-27-vpf-liens-personnels-l42323-fr`, FRANCE). Analyse l'éligibilité au titre « vie privée et familiale » (art. 8 CEDH, L. 423-23 CESEDA) en combinant durée de résidence (≥ 5 ans, ou ≥ 3 ans si entrée pendant la minorité) et score d'intensité des liens. 1:1 avec un dossier.

```
vpf_liens_personnels_analyses
  id                              UUID PK
  case_file_id                    UUID FK → case_files(id)  UNIQUE (inline)
  duree_residence_france_mois     INT NOT NULL
  entree_en_france_mineur         BOOLEAN NOT NULL
  enfants_en_france               BOOLEAN NOT NULL
  conjoint_en_france              BOOLEAN NOT NULL
  parents_en_france               BOOLEAN NOT NULL
  situation_familiale_a_letranger VARCHAR(300)               -- nullable
  niveau_integration              VARCHAR(20) NOT NULL
  ancienne_conviction_penale      BOOLEAN NOT NULL
  country                         VARCHAR(20) NOT NULL
  result_data                     TEXT NOT NULL              -- JSON : inputs + résultat calculé
  created_at                      TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes :

contrainte UNIQUE inline sur case_file_id (fk_vpf_liens_personnels_case_file) — une seule analyse par dossier

Migration : 450-create-vpf-liens-personnels-analyses.xml (+ 451-seed-vpf-liens-personnels-visibility.xml — visibility CONTEXTUAL priority 27)

---

## vrp_indemnite_clientele_analyses

F-218 SF-218-11 (F-218b) — analyse de l'outil décisionnel « VRP : statut, préavis et indemnité de clientèle » (`F-DT-104-vrp-indemnite-clientele`, FRANCE). Pour la rupture d'un VRP statutaire (art. L.7311-1 et s. CT) : préavis spécifique (L.7313-9, 1/2/3 mois selon ancienneté), éligibilité à l'indemnité de clientèle (L.7313-13), fourchette estimée (indicative — évaluation souveraine du juge), et comparaison non-cumul avec l'indemnité légale de licenciement (option la plus favorable). 1:1 avec un dossier.

```
vrp_indemnite_clientele_analyses
  id                              UUID PK
  case_file_id                    UUID FK → case_files(id)  UNIQUE
  date_entree                     DATE NOT NULL
  date_rupture                    DATE NOT NULL
  cause_rupture                   VARCHAR(40) NOT NULL  -- enum 6 valeurs (LICENCIEMENT_CAUSE_REELLE … RUPTURE_CONVENTIONNELLE)
  type_vrp                        VARCHAR(20) NOT NULL  -- EXCLUSIF / MULTICARTES
  commissions_annuelles_moyennes  NUMERIC NOT NULL
  salaire_mensuel_moyen           NUMERIC NOT NULL
  clientele_developpee            BOOLEAN NOT NULL
  country                         VARCHAR(20) NOT NULL DEFAULT 'FRANCE'
  result_data                     TEXT NOT NULL         -- JSON : inputs + résultat (dureePreavisMois, eligibiliteClientele, indemniteClienteleMin/Max, indemniteLegaleLicenciement, optionRecommandee, baseJuridique)
  created_at                      TIMESTAMP WITH TIME ZONE NOT NULL
  updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL
```

Contraintes : uq_vrp_indemnite_clientele_case_file (case_file_id) — une seule analyse par dossier.

Migration : 492-create-vrp-indemnite-clientele-analyses.xml (+ 493-seed-vrp-indemnite-clientele-visibility.xml — visibility CONTEXTUAL `vrp_statut_detecte=true` priority 113)

---

## jurisprudence_checks

F-179 — vérifications de la jurisprudence citée dans les documents uploadés d'un dossier (typiquement les conclusions adverses). Une ligne = une référence jurisprudentielle détectée, vérifiée par Claude Sonnet quant à son existence réelle et à la fidélité de la position alléguée. Produite en post-traitement de `CaseAnalysisService`.

```
jurisprudence_checks
  id                 UUID PK
  case_file_id       UUID NOT NULL FK → case_files(id)
  case_analysis_id   UUID NOT NULL FK → case_analyses(id)
  workspace_id       UUID NOT NULL FK → workspaces(id)   -- isolation workspace
  document_name      VARCHAR(500) NOT NULL
  reference          VARCHAR(500) NOT NULL    -- ex. « Cass. soc. 12/10/2022 n°21-12345 »
  statut             VARCHAR(20) NOT NULL     -- VERIFIED / SUSPECT / NOT_FOUND / UNCERTAIN
  explication        TEXT
  position_alleguee  TEXT
  source_url         VARCHAR(1000)
  claude_confidence  VARCHAR(10)
  web_search_used    BOOLEAN NOT NULL DEFAULT false
  created_at         TIMESTAMP WITH TIME ZONE NOT NULL
```

Index :

idx_jurisprudence_checks_case_analysis (case_analysis_id)
idx_jurisprudence_checks_case_file (case_file_id)

Règles :
- 4 statuts : `VERIFIED` (existence + position fidèles), `SUSPECT` (arrêt réel mais position alléguée incohérente — mauvaise foi adverse), `NOT_FOUND` (introuvable même après web search), `UNCERTAIN` (knowledge gap Claude + web search en échec — vérification manuelle requise).
- Isolation workspace stricte via `workspace_id`. La lecture filtre sur la dernière `case_analyses` DONE du dossier.

Migration : 245-create-jurisprudence-checks.xml

---

## tool_jurisprudence_mappings

F-JU-01 / SF-JU-01-01 — mappings curatés entre une branche de calcul d'un outil décisionnel et les 1 à 3 arrêts structurants qui fondent juridiquement le calcul. Une ligne = un arrêt mappé à une branche d'un outil. Alimentée par bootstrap automatique Claude (SF-JU-01-05) puis maintenue par le cron mensuel full auto-pilot (SF-JU-01-02). `chapeau_officiel` = texte brut publié par la juridiction (Cour de cassation, Conseil d'État, Cour const. BE, Cass. BE) — pas de reformulation Claude, zéro déformation possible.

```
tool_jurisprudence_mappings
  id                  UUID PK
  tool_id             VARCHAR(100) NOT NULL    -- TOOL_REGISTRY (~131 outils, ~80-90 éligibles)
  branche_calcul_id   VARCHAR(100) NOT NULL    -- identifiant libre d'une branche du calculator
  arret_ref           VARCHAR(200) NOT NULL    -- ex. « Cass. soc. 8 janv. 2025, n° 23-12.345 »
  juridiction         VARCHAR(50)  NOT NULL    -- ex. « Cour de cassation, chambre sociale »
  date_arret          DATE         NOT NULL
  numero_pourvoi      VARCHAR(50)  NOT NULL
  lien_legifrance     VARCHAR(500) NOT NULL    -- URL Légifrance / juridat.be / const-court.be
  chapeau_officiel    VARCHAR(2000) NOT NULL   -- texte brut officiel, pas une reformulation
  last_verified_at    TIMESTAMP WITH TIME ZONE NOT NULL
  confidence_score    DECIMAL(3,2) NOT NULL    -- 0.00 à 1.00 — seuil min 0.60 (sinon silence)
  archived            BOOLEAN NOT NULL DEFAULT false
```

Index + contraintes :

idx_tool_jurisprudence_mappings_lookup (tool_id, branche_calcul_id, archived)
uq_tool_jurisprudence_mappings_active UNIQUE (tool_id, branche_calcul_id, arret_ref)

Règles :
- **Table globale** (pas d'isolation workspace) — la jurisprudence française et belge est identique pour tous les avocats.
- Aucun endpoint d'écriture exposé en SF-JU-01-01 ; mutations réservées au cron full auto-pilot (SF-JU-01-02) et au dashboard SUPER_ADMIN (SF-JU-01-05).
- Limite stricte de **3 résultats** appliquée côté service (`findTop3...OrderByConfidenceScoreDescDateArretDesc`). Tri secondaire par `date_arret DESC` à confidence égale.
- Use case **orthogonal** à `case_jurisprudence_citations` (F-242, table workspace-scoped per-point juridique, saisie manuelle avocat) — pas de fusion possible. Convention de nommage `Tool*` pour les classes Java F-JU-01 (ex. `ToolJurisprudenceMapping`, `ToolJurisprudenceCitationResponse`) afin d'éviter la collision avec `JurisprudenceCitation` de F-242.

Migration : 282-create-tool-jurisprudence-mappings.xml

---

## jurisprudence_watch_flags

F-JU-01 / SF-JU-01-01 — flags de veille jurisprudentielle à arbitrer. Une ligne = un événement de veille (nouvel arrêt entrant détecté par le cron mensuel à confiance Claude entre 0.60 et 0.85, OU signalement utilisateur via le bouton « Signaler un problème »). Le statut passe de `PENDING` à `REVIEWED` (avec décision `REPLACE` / `ADD`) ou `IGNORED` lors du clic admin dans le dashboard `/super-admin/jurisprudence-watch` (SF-JU-01-05).

```
jurisprudence_watch_flags
  id                   UUID PK
  tool_id              VARCHAR(100) NOT NULL
  branche_calcul_id    VARCHAR(100) NOT NULL
  arret_entrant_ref    VARCHAR(200) NOT NULL
  mapping_actuel_id    UUID         FK → tool_jurisprudence_mappings(id)   -- NULL si nouveau
  source               VARCHAR(20)  NOT NULL    -- CRON / USER_SIGNAL
  confidence_score     DECIMAL(3,2)             -- NULL si source=USER_SIGNAL
  explication          VARCHAR(2000)
  statut               VARCHAR(20)  NOT NULL DEFAULT 'PENDING'   -- PENDING / REVIEWED / IGNORED
  created_at           TIMESTAMP WITH TIME ZONE NOT NULL
  reviewed_at          TIMESTAMP WITH TIME ZONE
  reviewed_by_user_id  UUID         FK → users(id)
  decision             VARCHAR(20)              -- REPLACE / ADD / IGNORE (NULL si PENDING)
  comment_user         VARCHAR(2000)
```

Index :

idx_jurisprudence_watch_flags_pending (statut, created_at)

Règles :
- Table globale (pas d'isolation workspace).
- Table créée en SF-JU-01-01 mais **NON alimentée** par cette SF. Premier INSERT par SF-JU-01-02 (cron mensuel) ou SF-JU-01-04 (bouton signaler côté avocat utilisateur).
- Mode opérationnel par défaut full auto-pilot : la plupart des arrêts entrants à confiance > 0.85 sont actionnés directement (pas de flag créé). Seuls les cas ambigus (confiance entre 0.60 et 0.85) ou les signalements utilisateurs génèrent des flags.

Migration : 283-create-jurisprudence-watch-flags.xml

---

## jurisprudence_audit_log

F-JU-01 / SF-JU-01-01 — audit log rejouable de toutes les actions sur les mappings jurisprudentiels (cron auto-pilot OU manuel SUPER_ADMIN). Trace les confirmations mensuelles (`AUTO_CONFIRM`), ajouts/remplacements/archivages automatiques (`AUTO_ADD`/`AUTO_REPLACE`/`AUTO_ARCHIVE`), et les arbitrages manuels (`MANUAL_REPLACE`/`MANUAL_ADD`/`MANUAL_IGNORE`).

```
jurisprudence_audit_log
  id                 UUID PK
  mapping_id         UUID NOT NULL FK → tool_jurisprudence_mappings(id)
  action             VARCHAR(30) NOT NULL    -- 7 valeurs (cf. ci-dessus)
  actor              VARCHAR(20) NOT NULL    -- CRON / SUPER_ADMIN
  actor_user_id      UUID        FK → users(id)   -- NULL si actor=CRON
  claude_confidence  DECIMAL(3,2)            -- NULL si actor=SUPER_ADMIN
  claude_reason      VARCHAR(2000)           -- NULL si actor=SUPER_ADMIN
  created_at         TIMESTAMP WITH TIME ZONE NOT NULL
```

Index :

idx_jurisprudence_audit_log_mapping (mapping_id, created_at)

Règles :
- Table globale (pas d'isolation workspace).
- Table créée en SF-JU-01-01 mais **NON alimentée** par cette SF. Premier INSERT par SF-JU-01-02 (cron mensuel) ou SF-JU-01-05 (dashboard admin).
- Sert à la review semestrielle facultative (~30 min/an) — audit log des remplacements et alertes massives.
- Sert également à l'email mensuel récap (compteur par type d'action) envoyé au fondateur en mode full auto-pilot.

Migration : 284-create-jurisprudence-audit-log.xml

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

# 25f — Blog SEO (F-120 — SF-120-01)

Tables de contenu public pour le blog SEO automatisé.
Isolées du modèle multi-tenant : aucun `workspace_id` (contenu public mutualisé).

## blog_topics

Backlog de sujets d'articles, alimenté manuellement puis consommé par le générateur (SF-120-02).

id (UUID PK)
slug (VARCHAR(200), non nullable, unique — uq_blog_topics_slug)
title (VARCHAR(500), non nullable)
description (TEXT)
category (VARCHAR(30), non nullable — DROIT_DU_TRAVAIL / DROIT_IMMIGRATION / DROIT_FAMILLE)
country_scope (VARCHAR(10), non nullable — FRANCE / BELGIQUE)
status (VARCHAR(20), non nullable — PENDING / IN_PROGRESS / USED / REJECTED)
article_id (UUID — lien vers blog_articles si USED)
used_at (TIMESTAMP WITH TIME ZONE)
created_at (TIMESTAMP WITH TIME ZONE, non nullable)

Index :
idx_blog_topics_status (status)
idx_blog_topics_category_country (category, country_scope)

Règles :
- Un topic USED référence son article généré via `article_id`.
- Le quota glissant 4 semaines (60/25/15 % domaines V1) s'appuie sur category + country_scope + used_at.
- Migration : 078-create-blog-topic.xml

---

## blog_articles

Articles publiés ou en brouillon. URL publique `/blog/fr/<slug>` ou `/blog/be/<slug>`.

id (UUID PK)
slug (VARCHAR(200), non nullable, unique — uq_blog_articles_slug)
title (VARCHAR(500), non nullable)
subtitle (VARCHAR(1000))
body_markdown (TEXT, non nullable — corps en Markdown, rendu côté frontend)
hero_image_url (VARCHAR(500) — S3 WebP, fallback JPEG)
hero_image_alt (VARCHAR(500))
country (VARCHAR(10), non nullable — FRANCE / BELGIQUE)
legal_domain (VARCHAR(30), non nullable — DROIT_DU_TRAVAIL / DROIT_IMMIGRATION / DROIT_FAMILLE)
author_name (VARCHAR(200), non nullable — par défaut "Franck Tounga")
author_url (VARCHAR(500))
meta_title (VARCHAR(70), non nullable — SEO title tag)
meta_description (VARCHAR(160), non nullable — SEO meta description)
reading_time_minutes (INTEGER, non nullable — calculé à la génération)
status (VARCHAR(20), non nullable — DRAFT / PUBLISHED / UNPUBLISHED)
topic_id (UUID FK → blog_topics, non nullable — fk_blog_articles_topic)
published_at (TIMESTAMP WITH TIME ZONE — renseigné au passage PUBLISHED)
created_at (TIMESTAMP WITH TIME ZONE, non nullable)
updated_at (TIMESTAMP WITH TIME ZONE, non nullable)

Index :
idx_blog_articles_status_published_at (status, published_at) — listing public trié par date de publication
idx_blog_articles_country (country)
idx_blog_articles_legal_domain (legal_domain)

Règles :
- `slug` unique globalement — déterminé à la génération.
- `meta_title` ≤ 70 caractères, `meta_description` ≤ 160 caractères (contraintes SEO).
- Status lifecycle : DRAFT → PUBLISHED → UNPUBLISHED (bouton dépublier 1-clic).
- Seuls les articles `PUBLISHED` sont exposés par `GET /api/v1/blog/articles`.
- Endpoints publics sans auth (whitelistés dans `SecurityConfig`).
- Migration : 079-create-blog-article.xml

## decision_tool_visibility_rules

Configuration déclarative du moteur F-IA-04 d'affichage conditionnel des outils décisionnels sur le dashboard dossier. Alimentée exclusivement par migrations Liquibase (pas d'endpoint d'écriture en V1).

id (UUID PK)
legal_domain (VARCHAR(50), non nullable — DROIT_DU_TRAVAIL / DROIT_IMMIGRATION / DROIT_FAMILLE)
country (VARCHAR(20), nullable — FRANCE / BELGIQUE / NULL = règle transversale au domaine)
tool_id (VARCHAR(100), non nullable — identifiant stable de l'outil, ex. "F-DT-10-rupture-conv-validity")
layer (VARCHAR(20), non nullable — ALWAYS_ON / CONTEXTUAL)
trigger_field (VARCHAR(100), nullable — nom du champ IA à matcher, ex. "type_rupture")
trigger_value (VARCHAR(100), nullable — valeur du champ IA qui active la règle, ex. "RUPTURE_CONVENTIONNELLE")
priority (INTEGER, non nullable, défaut 0 — ordre d'affichage dans la couche)
created_at (TIMESTAMP WITH TIME ZONE, non nullable)

Index :
idx_dtvr_domain_country (legal_domain, country) — requête principale du service

Contrainte CHECK ck_dtvr_layer_trigger :
- Si layer = ALWAYS_ON → trigger_field IS NULL AND trigger_value IS NULL
- Si layer = CONTEXTUAL → trigger_field IS NOT NULL AND trigger_value IS NOT NULL

Règles :
- Un `tool_id` peut apparaître dans plusieurs règles (un outil activé par plusieurs valeurs enum différentes).
- Aucune FK vers case_files ou workspaces : cette table est une configuration domaine, pas une donnée par dossier.
- `DecisionToolVisibilityService.resolveVisibleTools(caseFileId, user)` lit les règles du couple `(legalDomain, country)` + les règles `country IS NULL` du même domaine, puis croise avec les codes de situation extraits de `case_analyses.analysis_result` (dernière analyse DONE) pour produire 3 listes : `alwaysOn` / `contextual` / `catalog`.
- Seedée initialement par migration 105 avec 53 règles couvrant les 23 outils décisionnels existants.

---

## backlog_features

F-178 SF-178-01 — cache de lecture du visualiseur de backlog super-admin.
**Source de vérité = `docs/PRODUCT_SPEC.md`** (Option A F-178). Cette table est alimentée par `BacklogSyncService` qui parse le Markdown ; elle ne doit **jamais** être éditée manuellement (cf. règle Étape 7 CLAUDE.md).

Colonnes :
- id (UUID, PK)
- code (VARCHAR(32), unique, ex: `F-DT-08`, `F-178`)
- title (VARCHAR(500), non nullable)
- target_version (VARCHAR(32), nullable, ex: `V1`, `V8+`)
- status (VARCHAR(32), enum `BacklogStatus` : PLANNED, READY, IN_PROGRESS, BLOCKED, DONE, PARTIAL, ABSORBED, UNKNOWN)
- description (TEXT, brut markdown des notes)
- domain (VARCHAR(32), enum `BacklogDomain` : WORK, IMMIGRATION, FAMILY, CROSS, nullable)
- priority (VARCHAR(16), enum `BacklogPriority` : HIGH, MEDIUM, LOW, nullable)
- source_file (VARCHAR(255), non nullable, ex: `docs/PRODUCT_SPEC.md`)
- source_line (INT, ligne dans le MD au moment du parse)
- parsed_at (TIMESTAMP WITH TIME ZONE)
- is_orphaned (BOOLEAN, défaut false — true si code disparaît du MD entre 2 syncs)
- created_at, updated_at (TIMESTAMP WITH TIME ZONE)

Index : idx_backlog_features_status, idx_backlog_features_domain.

## backlog_subfeatures

F-178 SF-178-01 — découpage SF parsé depuis les notes de PRODUCT_SPEC.md.

Colonnes :
- id (UUID, PK)
- code (VARCHAR(64), unique, ex: `SF-DT-08-01`, `SF-178-01`)
- parent_feature_id (UUID, FK `backlog_features(id)` ON DELETE CASCADE)
- title (VARCHAR(500), nullable — souvent extrait de contexte court)
- status (VARCHAR(32), enum `BacklogStatus`)
- description (TEXT)
- source_file, source_line, parsed_at, is_orphaned, created_at, updated_at (idem features)

Index : idx_backlog_subfeatures_parent (parent_feature_id), idx_backlog_subfeatures_status.

## backlog_marketing_tasks

F-178 SF-178-01 — cache de lecture de `docs/MARKETING_BACKLOG.md`.

Colonnes :
- id (UUID, PK)
- code (VARCHAR(32), unique, ex: `M-71`)
- title (VARCHAR(500), non nullable)
- status (VARCHAR(32), enum `BacklogMarketingStatus` : TODO, DRAFTED, IN_PROGRESS, DONE, BLOCKED, UNKNOWN)
- description (TEXT)
- category (VARCHAR(64), nullable — détectée depuis section Markdown ex: `Site web`, `Vidéo`)
- source_file, source_line, parsed_at, is_orphaned, created_at, updated_at

Index : idx_backlog_marketing_status.

## backlog_sync_runs

F-178 SF-178-01 — audit des runs de sync MD → DB.

Colonnes :
- id (UUID, PK)
- started_at, finished_at (TIMESTAMP WITH TIME ZONE)
- duration_ms (BIGINT, nullable)
- success (BOOLEAN, défaut true)
- features_count, subfeatures_count, marketing_count, orphans_marked (INT, défauts 0)
- triggered_by (VARCHAR(32), enum `SyncTrigger` : MANUAL, SCHEDULED, STARTUP)
- error_message (TEXT, rempli si success=false)
- created_at (TIMESTAMP WITH TIME ZONE)

Index : idx_backlog_sync_runs_started (started_at DESC), idx_backlog_sync_runs_success.

Règles :
- Aucune FK vers workspaces — feature super-admin transversale.
- Tous les endpoints `/api/v1/super-admin/backlog/*` gated par `SuperAdminService.assertSuperAdmin`.
- La sync est idempotente : `BacklogSyncService.sync()` appelé 2× consécutifs ne crée pas de doublons (upsert par code).
- Suppressions : aucune. Si un code disparaît du MD, on positionne `is_orphaned=true` (conservation historique).

---

## dashboard_tile_crashes

F-180 SF-180-01 — crashes runtime des mappers `DashboardTile` de F-167. Une row = une exception réelle jetée en production par un mapper `tileFromXxx()` de `CaseFileDashboardService` et catchée par `addSafely()`. Complément **runtime** du garde-fou **statique** `DashboardTileToolIdIntegrityIT` (SF-DT-36-03, CI build-time) : ici on persiste les crashes d'exécution, là on empêche les désynchros structurelles avant merge.

Colonnes :
- id (UUID, PK)
- tool_id (VARCHAR(100), non nullable — identifiant TOOL_REGISTRY du mapper)
- case_file_id (UUID, nullable — dossier en cours d'assemblage ; **jamais exposé par l'API**, PII)
- exception_class (VARCHAR(255), non nullable)
- exception_message (VARCHAR(2000), nullable — tronqué à 2000 caractères côté service)
- occurred_at (TIMESTAMP WITH TIME ZONE, non nullable)

Index : idx_dashboard_tile_crashes_occurred_at, idx_dashboard_tile_crashes_tool_id.

Règles :
- Aucun `workspace_id` — observabilité produit globale (même pattern que les tables `backlog_*`).
- INSERT par `DashboardTileCrashRecorder` (transaction `REQUIRES_NEW`, fail-open du fail-open : un échec d'INSERT ne dégrade jamais le dashboard de l'avocat).
- Rétention 30j : les rows plus anciennes sont purgées à chaque `DashboardAuditService.runAudit()`.
- Persistance en DB plutôt que grep logs JVM : robuste au redémarrage d'instance.
- Migration : 251-create-dashboard-audit-tables.xml

## dashboard_audit_runs

F-180 SF-180-01 — snapshots historisés des runs d'audit dashboard. Une row = un audit produit soit par le `@Scheduled` hebdomadaire (lundi 8h UTC), soit par le bouton « Relancer maintenant » du super-admin.

Colonnes :
- id (UUID, PK)
- ran_at (TIMESTAMP WITH TIME ZONE, non nullable)
- crashed_json (TEXT, non nullable — JSON sérialisé du panel 🔴 mappers en erreur 168h)
- dormant_json (TEXT, non nullable — JSON du panel 🟡 tables `*_analyses` à 0 row)
- active_json (TEXT, non nullable — JSON du panel 🟢 tables `*_analyses` à ≥ 1 row, triées par count desc)
- created_at (TIMESTAMP WITH TIME ZONE, non nullable)

Index : idx_dashboard_audit_runs_ran_at.

Règles :
- Aucun `workspace_id` — feature super-admin transversale.
- Colonnes JSON en `TEXT` (pas `jsonb`) pour compatibilité H2 profil dev.
- `GET /api/v1/super-admin/dashboard-audit/latest` lit la dernière row par `ran_at DESC` — il ne recalcule pas (un run = 95+ `count(*)`). Si aucune row n'existe, un run est déclenché à la volée.
- Endpoints `/api/v1/super-admin/dashboard-audit/*` gated par `SuperAdminService.assertSuperAdmin`.
- Migration : 251-create-dashboard-audit-tables.xml

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