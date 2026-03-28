# PRODUCT_SPEC.md — AI LegalCase

Source de vérité des fonctionnalités du produit.

Toute nouvelle feature doit être ajoutée ici avant toute implémentation.
Toute évolution d'une feature existante doit être validée et mise à jour ici.
Aucune feature ne peut être implémentée si elle n'est pas référencée dans ce fichier.

---

## Règles de gestion

- Toute feature ajoutée doit avoir un identifiant unique (`F-XX`)
- Les identifiants ne sont jamais réutilisés, même si une feature est supprimée
- Le statut est mis à jour à chaque étape du cycle de développement
- Toute modification de ce fichier doit être explicitement validée par le product owner
- Les features hors V1 sont listées mais ne peuvent pas être implémentées avant décision explicite

---

## Statuts possibles

| Statut | Signification |
|--------|--------------|
| `À spécifier` | Feature identifiée, pas encore découpée en subfeatures |
| `En cours` | Au moins une subfeature en cours d'implémentation |
| `Partielle` | Certaines subfeatures terminées, d'autres non |
| `Terminée` | Toutes les subfeatures DoD vérifiées et mergées |
| `Suspendue` | Mise en attente — décision explicite requise pour reprendre |

---

## Features V1

### Bloc 1 — Fondations

| ID | Feature | Description | Statut |
|----|---------|-------------|--------|
| F-01 | Authentification OAuth2 | Login Google + Microsoft. Aucun mot de passe local. Spring Security + OAuth2/OIDC. | `Terminée` |
| F-02 | Onboarding & workspace | Premier login → création user. Formulaire obligatoire de nom workspace avant accès au dashboard. | `Terminée` |

### Bloc 2 — Gestion des dossiers

| ID | Feature | Description | Statut |
|----|---------|-------------|--------|
| F-03 | Création de dossier | Formulaire de création : titre, domaine juridique (EMPLOYMENT_LAW en V1), description optionnelle. | `Terminée` |
| F-04 | Liste & consultation des dossiers | Dashboard principal. Liste paginée des dossiers du workspace. Navigation vers un dossier. Statuts visibles. | `Terminée` |
| F-27 | Domaine juridique du workspace | Le workspace est lié à un unique domaine juridique, choisi à l'onboarding par l'owner. Renommage EMPLOYMENT_LAW → DROIT_DU_TRAVAIL. Modale de sélection avec 3 catégories (Droit du travail actif, Droit immigration + Droit immobilier en "bientôt disponible"). Les case files héritent du domaine du workspace. | `Terminée` |

### Bloc 3 — Documents

| ID | Feature | Description | Statut |
|----|---------|-------------|--------|
| F-05 | Upload de documents | Ajout de fichiers à un dossier. Validation type et taille. Stockage object storage S3-compatible. | `Terminée` |
| F-52 | Upload multi-documents — sélection batch et soumission différée | L'écran d'upload passe en mode "panier" : le bouton "Ajouter des documents" ouvre le sélecteur avec multi-sélection possible et ajoute les fichiers à une liste locale sans les uploader immédiatement. Un bouton "Uploader les documents" déclenche l'upload réel en parallèle (N appels vers l'endpoint existant). Aucun changement backend. SF-52-01 mergée 2026-03-26. | `Terminée` |
| F-06 | Extraction de texte | Transformation du fichier brut en texte exploitable. Persistance dans `document_extractions`. Déclenchée automatiquement après upload. | `Terminée` |
| F-07 | Chunking | Segmentation du texte extrait en chunks. Persistance dans `document_chunks`. Déclenchée automatiquement après extraction. | `Terminée` |

### Bloc 4 — Pipeline IA

| ID | Feature | Description | Statut |
|----|---------|-------------|--------|
| F-08 | Analyse IA — chunk | Analyse de chaque chunk par le LLM. Persistance dans `chunk_analyses`. Asynchrone. | `Terminée` |
| F-09 | Analyse IA — document | Synthèse des chunks d'un document. Persistance dans `document_analyses`. Asynchrone. | `Terminée` |
| F-10 | Analyse IA — dossier | Synthèse globale du dossier. Persistance dans `case_analyses`. Asynchrone. Format JSON : timeline, faits, points_juridiques, risques, questions_ouvertes. | `Terminée` |
| F-11 | Suivi des jobs asynchrones | Suivi de la progression des analyses en temps réel. Table `analysis_jobs`. Affichage statut et pourcentage. | `Terminée` |
| F-28 | Scalabilité pipeline IA — résumés compacts | Les system prompts de DocumentAnalysisService et CaseAnalysisService imposent des contraintes de longueur explicites (nb max d'items par champ JSON) + truncation Java déterministe avant persistance. Garantit que l'input de chaque niveau reste borné quel que soit le nombre de documents uploadés. | `Terminée` |
| F-30 | Parallélisme pipeline IA — concurrence RabbitMQ | Traitement parallèle des chunks via 5 consumers RabbitMQ concurrents. Réduit le temps d'analyse de ~6 min à ~2 min pour 3 documents. | `Terminée` |
| F-32 | Optimisation coût LLM — modèle adaptatif par étape | Chunk + document analysis sur Claude Haiku (tâches simples, ~10x moins cher). Case analysis, question generation et enriched analysis restent sur Claude Sonnet (qualité critique). Réduction estimée ~80% des coûts LLM. Aucun impact fonctionnel visible. | `Terminée` |
| F-51 | Pipeline IA adaptatif — chunking conditionnel | Si le document extrait tient dans la fenêtre de contexte (< 150 000 tokens ≈ 600 000 chars), il est envoyé directement à DocumentAnalysisService sans découpage en chunks. Sinon le pipeline existant (chunking → chunk analysis → document analysis) est conservé. Élimine ~90% des appels Anthropic pour les documents juridiques courants et réduit le temps d'analyse de ~4 min à ~15 sec. | `Terminée` |

### Bloc 5 — Interaction avocat

| ID | Feature | Description | Statut |
|----|---------|-------------|--------|
| F-12 | Restitution de l'analyse | Affichage structuré de la synthèse : timeline, faits, points juridiques, risques, questions ouvertes. | `Terminée` |
| F-31 | Écran dédié synthèse | Page dédiée `/case-files/:id/synthesis` avec sections distinctes (Chronologie, Faits, Points juridiques, Risques, Questions ouvertes), navigation claire et bouton re-analyser. Remplace l'affichage inline dans la page dossier. | `Terminée` |
| F-13 | Questions IA interactives | L'IA génère des questions complémentaires après synthèse. Persistance dans `ai_questions`. | `Terminée` |
| F-14 | Réponses avocat & re-synthèse | L'avocat répond aux questions IA. Persistance dans `ai_question_answers`. Déclenchement d'une nouvelle synthèse enrichie. | `Terminée` |

### Bloc 6 — Exploitation

| ID | Feature | Description | Statut |
|----|---------|-------------|--------|
| F-15 | Suivi consommation LLM | Traçabilité des tokens et coûts LLM par dossier et par user. Table `usage_events`. | `Terminée` |
| F-16 | Gestion des abonnements | Plans Starter et Pro. Table `subscriptions`. Contrôle d'accès selon le plan. | `Terminée` |
| F-33 | Limite de re-analyses par dossier | Gate billing : nombre maximum de re-analyses (ENRICHED_ANALYSIS) par dossier selon le plan (PRO = 5). Protège contre les dérapages de coût LLM sur les gros dossiers. | `Terminée` |
| F-34 | Budget tokens mensuel par workspace | Plafond de tokens consommés par mois et par workspace selon le plan. Alerte super-admin si dépassement, blocage hard si seuil critique atteint. Visibilité en page admin. | `Terminée` |
| F-35 | Chat libre sur dossier | L'avocat pose ses propres questions aux documents du dossier (mode RAG). Modèle adaptatif : Haiku pour questions factuelles, Sonnet pour analyses approfondies. Limite de messages/mois par plan. | `Terminée` |
| F-36 | Déclenchement manuel de l'analyse dossier | L'analyse dossier (case analysis) est déclenchée manuellement via un bouton, pas automatiquement après le dernier document. Gate billing sur le nombre d'analyses par dossier selon le plan (FREE=2, STARTER=5, PRO=illimité). Permet d'uploader tous les documents avant d'analyser, et de re-analyser après ajout de nouveaux documents. | `Terminée` |
| F-37 | Versioning des synthèses | Chaque déclenchement d'analyse produit une nouvelle version numérotée de la synthèse (v1, v2…). La re-synthèse enrichie est une version distincte avec badge « Enrichie ». L'écran Synthèse affiche un sélecteur de version. Chaque version a ses propres questions IA isolées. Le chat libre (RAG) n'est pas lié aux versions. | `Terminée` |
| F-38 | Suppression de documents | L'avocat peut supprimer un document d'un dossier. La synthèse existante est signalée comme périmée avec un message adaptatif (ajouts / suppressions / les deux). Table audit_logs : traçabilité des actions sensibles (suppression, upload…) consultable par owner/admin dans un écran dédié `/workspace/audit-logs` avec recherche et filtre par action. | `Terminée` |

### Bloc 7 — Workspace

| ID | Feature | Description | Statut |
|----|---------|-------------|--------|
| F-17 | Gestion des membres workspace | Invitations, attribution des rôles (OWNER, ADMIN, LAWYER, MEMBER), révocation. Multi-workspace avec is_primary. Invitation par email avec token. Workspace switcher. | `Terminée` |

### Bloc 8 — Administration

| ID | Feature | Description | Statut |
|----|---------|-------------|--------|
| F-18 | Page d'administration | Tableau de bord admin : consommation LLM par dossier/user, gestion des membres, statut du plan. Accès OWNER/ADMIN uniquement. | `Terminée` |

### Bloc 9 — Super-administration plateforme

| ID | Feature | Description | Statut |
|----|---------|-------------|--------|
| F-25 | Super-admin plateforme | Tableau de bord super-admin : vue de tous les workspaces, consommation LLM par workspace, suppression workspace (cascade dossiers + membres exclusifs + annulation Stripe), suppression utilisateur (tous ses workspaces). Accès `is_super_admin` uniquement. Route `/super-admin`, lien header conditionnel. | `Terminée` |

### Bloc 10 — Auth locale

| ID | Feature | Description | Statut |
|----|---------|-------------|--------|
| F-26 | Auth locale (email/mot de passe) | Inscription email/mot de passe (nom, prénom, email, mdp) + validation email 24h. Connexion locale. Fusion automatique si email OAuth existant. Mot de passe oublié (reset token 24h). Coexistence avec OAuth2 Google/Microsoft. Page auth redessinée : onglets Se connecter / S'inscrire, formulaires + OAuth. | `Terminée` |

### Bloc 11 — Paiement

| ID | Feature | Description | Statut |
|----|---------|-------------|--------|
| F-19 | Intégration paiement Stripe | Checkout Stripe pour passage FREE→Starter et Starter→Pro. Webhook Stripe pour mise à jour automatique du plan. Page pricing frontend. Bannière upgrade. Gestion plan FREE trial 14j. | `Terminée` |
| F-58 | Repricing — Plans SOLO/TEAM/PRO | Remplacement des plans STARTER (49€) et PRO (129€) par SOLO (59€), TEAM (119€) et PRO (249€). Différenciation par quotas uniquement — toutes les features sur tous les plans payants. Re-analyse enrichie ouverte à tous les plans payants (quota = 1 en FREE pour démo). Nouveaux quotas : dossiers ouverts, documents/dossier, analyses/dossier, re-analyses enrichies, budget tokens mensuel, messages chat. Migration STARTER→SOLO sur subscriptions existantes. | `Terminée` |

---

## Ordre d'implémentation recommandé

```
F-01 → F-02 → F-03 → F-04 → F-05 → F-06 → F-07
                                              ↓
                                   F-08 → F-09 → F-10 → F-11 → F-12 → F-13 → F-14
                                              ↓
                                   F-15, F-16, F-17, F-18, F-25 (parallélisables)
```

---

## Features hors V1 (backlog)

### Domaines juridiques

| ID | Feature | Cible | Notes |
|----|---------|-------|-------|
| F-20 | Droit de l'immigration | V2 | Nouveau domaine juridique — nouveaux prompts LLM |
| F-21 | Droit immobilier | V3 | Nouveau domaine juridique — nouveaux prompts LLM |

### Pipeline IA & qualité

| ID | Feature | Cible | Notes |
|----|---------|-------|-------|
| F-29 | Limites pipeline IA configurables | V2 | Externaliser les limites hardcodées de F-28 (nb max d'items par champ JSON). Configurable par domaine juridique et/ou par plan (Starter/Pro). Actuellement hardcodé dans `AnalysisJsonTruncator` et les `SYSTEM_PROMPT`. |
| F-39 | Notifications temps réel | V2 — **Terminée** | SSE : notifier l'avocat quand une analyse se termine. Endpoint `GET /api/v1/case-files/{id}/analysis-status/stream`, `SseEmitterRegistry`, `SseNotificationService`, `AnalysisStatusEvent` afterCommit, `AnalysisSseService` Angular. SF-39-01 mergée 2026-03-25. SF-39-02 mergée 2026-03-26 : `GlobalAnalysisNotificationService` singleton Angular, toast MatSnackBar visible depuis toute page, événements SSE typés par jobType. |
| F-40 | Export PDF de la synthèse | V2 — **Terminée** | Générer un PDF structuré de la synthèse (timeline, faits, points juridiques, risques). Utile pour partager avec un client ou archiver. Implémenté 100% frontend via pdfmake (SF-40-01, mergé 2026-03-24). |

### UX & exploitation

| ID | Feature | Cible | Notes |
|----|---------|-------|-------|
| F-60 | Normalisation nom workspace en majuscules | V2 — **Terminée** | Le nom du workspace est converti en majuscules au moment de la saisie et stocké en majuscules en base. Concerne la création et la modification du nom. SF-60-01 mergée 2026-03-28. |
| F-61 | Responsive mobile — Shell & navigation | V2 — **Terminée** | Sidebar rétractable sur mobile (menu hamburger), header adaptatif, navigation accessible sur téléphone. SF-61-01 mergée 2026-03-28. |
| F-62 | Responsive mobile — Écrans principaux | V2 — **Terminée** | Adaptation mobile de case-file-detail, synthesis et liste des dossiers — colonnes empilées, upload accessible, synthèse lisible. SF-62-01 mergée 2026-03-28. |
| F-63 | Responsive mobile — Écrans secondaires | V2 — **Terminée** | Adaptation mobile des écrans secondaires : analysis-diff (sélecteurs empilés), membres (colonnes masquées), admin (colonne email masquée). Login/onboarding/billing déjà responsive. SF-63-01 mergée 2026-03-28. |
| F-41 | Partage dossier lecture seule | V2 | Lien temporaire (token, expiration configurable) permettant à un client de consulter la synthèse d'un dossier sans compte. Accès lecture seule strict. |
| F-49 | Top-up de crédits tokens | V2 | Achat de tokens supplémentaires via Stripe (one-shot, hors abonnement) quand le quota mensuel est atteint. Table `credit_purchases`. Déduction prioritaire sur les crédits avant le quota plan. Webhook Stripe dédié. Visible dans la page Administration. |
| F-42 | Export CSV journal d'actions | V2 | Bouton export dans `/workspace/audit-logs`. Génère un CSV de toutes les entrées (ou des entrées filtrées). |
| F-43 | Filtre par plage de dates — journal d'actions | V2 | Sélecteur de dates (date début / date fin) dans l'écran `/workspace/audit-logs`. Actuellement hors scope de SF-38-04. |
| F-44 | Pagination et tri côté serveur — journal d'actions | V2 | Actuellement le backend renvoie 50 entrées max et le filtre est côté client. À remplacer par pagination serveur + paramètres de tri/filtre sur l'endpoint `GET /api/v1/admin/audit-logs`. |
| F-45 | Pagination côté serveur — liste des dossiers | V2 | La liste des dossiers est actuellement chargée entièrement côté client. À paginer côté serveur pour les workspaces avec de nombreux dossiers. |
| F-48 | Tableau de bord dossier | V2 — **Terminée** | Métriques par dossier : nombre de documents, nombre d'analyses terminées, total tokens consommés (pas de coût en euros, pas de durée). Visible depuis la page dossier. SF-48-01 mergée 2026-03-26 : `GET /api/v1/case-files/{id}/stats`. SF-48-02 mergée 2026-03-26 : bloc métriques Angular dans case-file-detail. |
| F-59 | UX diff — filtres, stats bar et sections collapsibles | Améliorations ergonomiques de l'écran de comparaison inter-analyses : pré-sélection auto des 2 dernières versions, barre de stats sticky (ajouts/suppressions/enrichis/inchangés), chips filtres, toggle inchangés, sections auto-repliées si vides de changements, collapsible au clic, raisons inline. | `Terminée` |
| F-54 | Comparaison inter-analyses (diff) | V2 — **Terminée** | Permettre à l'avocat de comparer deux versions de synthèse (STANDARD ou ENRICHED) sur un même dossier. Diff sémantique item par item sur tous les champs (faits, points juridiques, risques, questions ouvertes, timeline). Choix libre des deux versions à comparer. Ajouts en vert, suppressions en rouge, inchangé en gris. Disponible dès que le dossier a au moins 2 versions d'analyse. SF-54-01 mergée 2026-03-26 : backend (GET /diff, AnalysisDiffService, SectionDiff<T>, 251 tests). SF-54-02 mergée 2026-03-26 : frontend (AnalysisDiffComponent, route /diff, bouton conditionnel, diff coloré 5 sections, 206 tests). |
| F-55 | Multi-domaines et pays workspace | V2 — **Terminée** | Activation des domaines DROIT_IMMIGRATION et DROIT_FAMILLE (DROIT_IMMOBILIER retiré). Ajout du champ `country` (FRANCE / BELGIQUE) au workspace. Couleurs distinctes par domaine. Prompts IA dynamiques selon domaine + pays. Sélection pays à l'onboarding. SF-55-01 mergée 2026-03-26 : migration 029 country, LegalDomainPromptBuilder, 6 services IA dynamiques, 255 tests. SF-55-02 mergée 2026-03-26 : DomainPickerDialog 3 tuiles colorées + sélecteur pays, OnboardingComponent, ShellComponent domainColor(), 31 tests frontend. |
| F-56 | Diff sémantique avec attribution des raisons | V2 — **Terminée** | Amélioration de F-54. Remplacement du diff exact par un diff sémantique via Haiku (4 états : ajouté/supprimé/inchangé/enrichi-bleu). Snapshot des documents et Q&A par analyse. Attribution de la raison par item : document ajouté/supprimé pour STANDARD vs STANDARD, réponse Q&R figée pour STANDARD vs ENRICHIE. Tooltip au survol. Cache du résultat sémantique par paire. 5 appels Haiku parallèles (virtual threads). Garde relance enrichie (409 sans nouvelles réponses). SF-56-01 mergée 2026-03-27 : migration 030, AnalysisDocumentSnapshot, AnalysisDocumentSnapshotService. SF-56-02 mergée 2026-03-27 : SemanticDiffService (Haiku + fallback exact), cache 031, AnalysisDiffResponse 4 états, 17 tests. SF-56-03 mergée 2026-03-27 : frontend enriched bleu, matTooltip reason, totalEnriched/sectionEnrichedCount, 14 tests. SF-56-04 mergée 2026-03-28 : 5 appels Haiku parallèles via virtual threads, fallback par section, 14 tests. SF-56-05 mergée 2026-03-28 : garde relance enrichie 409, 3 nouveaux tests. SF-56-06 mergée 2026-03-28 : migration 032 table `analysis_qa_snapshots`, AnalysisQaSnapshotService, buildContext() utilise snapshot figé avec fallback, 277 tests. |
| F-57 | Modification des réponses Q&A | V2 — **Terminée** | Permettre à l'avocat de modifier une réponse déjà soumise à une question IA. Bouton "Modifier" sur chaque réponse existante. La modification crée une nouvelle entrée dans `ai_question_answers` (historique conservé). Le garde SF-56-05 doit reconnaître une réponse modifiée comme activité suffisante pour relancer une analyse enrichie. |
| F-53 | Gestion du statut des dossiers | V2 — **Terminée** | Clôture, réouverture et suppression (soft delete) des dossiers. Statuts : `OPEN`, `CLOSED`, soft delete via `deleted_at`. Clôture : tous les membres. Réouverture : OWNER/ADMIN uniquement, bloquée si quota atteint. Suppression soft : OWNER uniquement, 409 si analyse en cours. Toutes les actions tracées dans `audit_logs`. SF-53-01 mergée 2026-03-26 : backend (migration 028, CaseFileStatusService, 3 endpoints, 242 tests). SF-53-02 mergée 2026-03-26 : frontend (CaseFileStatusService, boutons Clôturer/Réouvrir/Supprimer, badge CLOSED, dialog confirmation, 50 tests). |

### Auth & collaboration

| ID | Feature | Cible | Notes |
|----|---------|-------|-------|
| F-22 | SSO entreprise (Azure AD, Google Workspace, SAML) | V2+ | Auth avancée pour cabinets |
| F-23 | Collaboration avancée | V2+ | Partage de dossiers entre membres, commentaires |
| F-24 | Génération d'argumentaire | V2+ | Hors scope V1 — complexité juridique trop élevée |

### Infrastructure & qualité

| ID | Feature | Cible | Notes |
|----|---------|-------|-------|
| F-46 | Tests E2E smoke branchés CI | V2 | Les specs existent dans `e2e/smoke/` (auth, workspace, navigation). Brancher dans GitHub Actions pour bloquer les merges sur régression. |
| F-47 | Monitoring & alertes applicatives | V2 — **Terminée** | Sentry backend (`sentry-spring-boot-starter-jakarta`) + frontend (`@sentry/angular`). Capture automatique exceptions + événement manuel sur job IA FAILED. DSN via env var, désactivé en dev. SF-47-01 + SF-47-02 mergées 2026-03-25. |
| F-50 | Déploiement V1 — AWS EKS | V1 — **Terminée** | Infrastructure AWS (Terraform) + Dockerfiles + Kubernetes manifests + CI/CD GitHub Actions. Région eu-west-3 (Paris). Cluster EKS unique avec namespaces staging/production. RDS PostgreSQL, S3 AWS, ECR, RabbitMQ sur EKS. 6 subfeatures toutes terminées : SF-50-01 Dockerfiles ✅, SF-50-02 Terraform infra ✅, SF-50-03 K8s manifests ✅, SF-50-04 CI/CD ✅, SF-50-05 Config prod OAuth2 (Google)/Stripe ✅, SF-50-06 Monitoring Sentry ✅. Premier déploiement production 2026-03-25. |

---

## Historique des évolutions

| Date | Modification | Validé par |
|------|-------------|------------|
| 2026-03-28 | F-63 Terminée — SF-63-01 mergée : responsive CSS diff (sélecteurs empilés), membres (colonnes masquées), admin (email masqué) | Product owner |
| 2026-03-28 | F-63 ajoutée au backlog — Responsive mobile écrans secondaires (onboarding, membres, billing, admin, diff, login) | Product owner |
| 2026-03-28 | F-62 Terminée — SF-62-01 mergée : responsive CSS liste dossiers (masque Domaine/Date), détail dossier (title-row wrap, docs table 2 cols, jobs IA barre séparée), synthèse (header wrap, titre 18px) | Product owner |
| 2026-03-28 | F-61 Terminée — SF-61-01 mergée : sidenav responsive (mode over/side selon viewport), hamburger button mobile, fermeture auto après navigation, 4 nouveaux tests unitaires | Product owner |
| 2026-03-24 | F-40 Terminée — SF-40-01 mergée : export PDF synthèse 100% frontend (pdfmake), page couverture branded, sections colorées, footer paginé, 7 tests unitaires | Product owner |
| 2026-03-24 | F-50 SF-50-04 mergée — CI/CD GitHub Actions backend + frontend : build Docker → ECR, kubectl apply kustomize, secrets K8s, séparation BACKEND_IMAGE_TAG/FRONTEND_IMAGE_TAG, RabbitMQConfig profil prod, Stripe test keys staging, sous-domaine staging.legalcase.ng-itconsulting.com HTTPS | Product owner |
| 2026-03-24 | F-50 SF-50-03 mergée — K8s manifests base + overlays staging/production : backend/frontend/rabbitmq/ingress nginx, HPA CPU 70%, PVC EBS RabbitMQ, Kustomize | Product owner |
| 2026-03-24 | F-50 SF-50-02 — Infrastructure Terraform : VPC, EKS 1.31, RDS PostgreSQL 16, S3, ECR, Secrets Manager, bootstrap S3+DynamoDB, environments staging/production, terraform validate PASS | Product owner |
| 2026-03-24 | F-50 SF-50-01 mergée — Dockerfiles multi-stage backend (JRE21 alpine, 330MB) + frontend (nginx alpine, 68MB), nginx.conf proxy /api /oauth2, fix @angular/animations v19 | Product owner |
| 2026-03-26 | F-55 Terminée — SF-55-02 mergée : DomainPickerDialog 3 tuiles colorées (#27AE60/#1A3A5C/#C9973A), sélecteur pays FRANCE/BELGIQUE, OnboardingComponent consomme {legalDomain,country}, ShellComponent domainColor(), 31 tests frontend | Product owner |
| 2026-03-26 | F-55 SF-55-01 mergée — migration 029 country workspace, 3 domaines (DROIT_DU_TRAVAIL/FAMILLE/IMMIGRATION), 2 pays (FRANCE/BELGIQUE), LegalDomainPromptBuilder, prompts IA dynamiques sur les 6 services, 255 tests | Product owner |
| 2026-03-26 | F-50 marquée Terminée — SF-50-05 (OAuth2 Google + Stripe prod) et SF-50-06 (Sentry monitoring) validées en prod. Microsoft OAuth2 retiré intentionnellement. | Product owner |
| 2026-03-24 | F-38 SF-38-04 mergée — écran dédié `/workspace/audit-logs` (AuditLogScreenComponent), filtre texte libre + filtre action, section journal supprimée de WorkspaceAdminComponent, 13 tests frontend | Product owner |
| 2026-03-24 | F-38 Terminée — SF-38-03 mergée : GET /api/v1/admin/audit-logs, AuditLogAdminService, section journal d'actions dans WorkspaceAdminComponent, 215 tests backend | Product owner |
| 2026-03-24 | F-38 SF-38-02 mergée — bouton suppression document, MatDialog confirmation, message adaptatif synthèse (ajouts/suppressions/les deux), CaseFile.lastDocumentDeletedAt, 162 tests frontend | Product owner |
| 2026-03-23 | F-38 SF-38-01 mergée — DELETE /api/v1/case-files/{id}/documents/{docId}, cascade chunk_analyses→document, last_document_deleted_at sur CaseFile, table audit_logs (DOCUMENT_DELETED), 409 si analyse en cours, migration 027, 212 tests | Product owner |
| 2026-03-23 | F-37 Terminée — SF-37-02 mergée : sélecteur de version dans SynthesisComponent, questions IA isolées par version, badge Enrichie, chat non versionné | Product owner |
| 2026-03-23 | F-37 SF-37-01 mergée — versioning backend : champ version + analysisType sur case_analyses, FK case_analysis_id sur ai_questions, migration 026, endpoints GET /versions et GET /versions/{version}, GET /ai-questions?analysisId | Product owner |
| 2026-03-23 | F-36 SF-36-03 mergée — indicateur synthèse périmée : badge warning + badge "Non inclus" sur documents postérieurs à la dernière analyse | Product owner |
| 2026-03-23 | F-36 fix mergé — reset processedItems, clamping progress, docAnalysisPending UX, visibleJobs, spinners états, quotas plans landing+billing | Product owner |
| 2026-03-23 | F-36 Terminée — SF-36-01 + SF-36-02 mergées : trigger manuel case analysis, endpoint POST /analyze, gate FREE=2/STARTER=5/PRO=illimité, bouton "Analyser le dossier" dans CaseFileDetailComponent, suppression auto-trigger | Product owner |
| 2026-03-22 | F-35 Terminée — SF-35-02 mergée : panneau chat dans SynthesisComponent, bulles question/réponse, checkbox analyse approfondie, gestion 402/424 | Product owner |
| 2026-03-22 | F-34 Terminée — SF-34-02 mergée : section budget mensuel dans page admin, barre de progression colorée, alerte ≥ 80 % | Product owner |
| 2026-03-22 | F-34 SF-34-01 mergée — gate pipeline tokens mensuel (FREE 500K / STARTER 3M / PRO 20M), AnalysisStatus.SKIPPED, fail-open | Product owner |
| 2026-03-22 | F-33 Terminée — SF-33-01 mergée : gate 402 si PRO ≥ 5 re-analyses par dossier, compteur via usage_events | Product owner |
| 2026-03-22 | F-32 Terminée — SF-32-01 mergée : Haiku sur chunk/document, Sonnet sur synthèses, réduction ~80% coûts LLM | Product owner |
| 2026-03-22 | F-32 ajoutée — optimisation coût LLM modèle adaptatif (Haiku chunks/docs, Sonnet synthèses) | Product owner |
| 2026-03-22 | F-33 ajoutée — limite re-analyses par dossier par plan, gate billing | Product owner |
| 2026-03-22 | F-34 ajoutée — budget tokens mensuel par workspace, plafond et alertes | Product owner |
| 2026-03-22 | F-35 SF-35-01 mergée — backend chat : table chat_messages, ChatService/ChatController @Profile local, gates 402/424, Haiku/Sonnet adaptatif, limites FREE=10/STARTER=50/PRO=200 messages/mois | Product owner |
| 2026-03-22 | F-35 ajoutée — chat libre sur dossier (RAG, modèle adaptatif, limites par plan) | Product owner |
| 2026-03-22 | F-31 SF-31-02 mergée — accordéon sur les sections synthèse, questions IA déplacées sur l'écran synthèse, bandeau compact sur page dossier | Product owner |
| 2026-03-22 | F-31 marquée Terminée — SF-31-01 mergée : SynthesisComponent écran dédié `/case-files/:id/synthesis`, 5 sections en cards, badge enrichi/initial, suppression bloc inline dans CaseFileDetailComponent | Product owner |
| 2026-03-22 | F-31 ajoutée — écran dédié synthèse, remplacement du bloc inline trop dense dans la page dossier | Product owner |
| 2026-03-22 | F-30 marquée Terminée — SF-30-01 mergée, concurrency=5 sur ChunkAnalysisService | Product owner |
| 2026-03-22 | F-30 ajoutée — parallélisme pipeline IA, concurrence RabbitMQ pour réduire le temps d'analyse | Product owner |
| 2026-03-22 | F-28 marquée Terminée — SF-28-01 (prompts compacts) + SF-28-02 (truncation Java déterministe) mergées | Product owner |
| 2026-03-22 | F-28 ajoutée — scalabilité pipeline IA, résumés compacts pour éviter l'explosion de l'input avec le nombre de documents | Product owner |
| 2026-03-22 | F-27 marquée Terminée — SF-27-01 (backend) + SF-27-02 (frontend modale onboarding) mergées | Product owner |
| 2026-03-21 | F-27 ajoutée — domaine juridique workspace, renommage DROIT_DU_TRAVAIL, modale onboarding | Product owner |
| 2026-03-17 | Création initiale — 17 features V1 définies | Product owner |
| 2026-03-17 | F-01 marquée Terminée — 5 subfeatures mergées sur master | Product owner |
| 2026-03-17 | F-06 Terminée — SF-06-01 (infrastructure) + SF-06-02 (extraction async) mergées | Product owner |
| 2026-03-17 | F-02 marquée Terminée — 2 subfeatures mergées sur master | Product owner |
| 2026-03-17 | F-03 marquée Terminée — SF-03-01 mergée sur master | Product owner |
| 2026-03-17 | F-04 marquée Terminée — SF-04-01 (liste) + SF-04-02 (get by id) mergées sur master | Product owner |
| 2026-03-17 | F-05 Terminée — SF-05-01 (upload), SF-05-02 (liste), SF-05-03 (download) mergées | Product owner |
| 2026-03-17 | F-07 Terminée — SF-07-01 (infrastructure) + SF-07-02 (chunking async) mergées | Product owner |
| 2026-03-17 | F-08 Terminée — SF-08-01 (infra RabbitMQ/Anthropic) + SF-08-02 (config) + SF-08-03 (ChunkAnalysisService) mergées | Product owner |
| 2026-03-17 | F-09 Terminée — SF-09-01 (infra document_analyses) + SF-09-02 (DocumentAnalysisService) mergées | Product owner |
| 2026-03-18 | F-10 Terminée — SF-10-01 (infra case_analyses) + SF-10-02 (CaseAnalysisService) mergées | Product owner |
| 2026-03-18 | F-10 évolution — SF-10-03 : ajout champ timeline au prompt CaseAnalysis (requis pour F-12) | Product owner |
| 2026-03-18 | F-11 Terminée — SF-11-01 (infra analysis_jobs) + SF-11-02 (API REST) + SF-11-03 (frontend) mergées | Product owner |
| 2026-03-18 | F-12 Terminée — SF-10-03 (timeline prompt) + SF-12-01 (API REST) + SF-12-02 (frontend) mergées | Product owner |
| 2026-03-18 | F-13 Terminée — SF-13-01 (infra ai_questions + génération async) mergée | Product owner |
| 2026-03-18 | F-14 Terminée — SF-14-01 (réponses avocat) + SF-14-02 (re-analyse pipeline) + SF-14-03 (frontend) mergées | Product owner |
| 2026-03-18 | F-15 Terminée — SF-15-01 (infra usage_events) + SF-15-02 (pipeline integration) + SF-15-03 (API REST) mergées. SF-15-04 (affichage frontend) différé en F-18 | Product owner |
| 2026-03-18 | F-16 Terminée — SF-16-01 (infra subscriptions) + SF-16-02 (gate création dossier) + SF-16-03 (gate upload) + SF-16-04 (gate re-analyse) mergées | Product owner |
| 2026-03-18 | F-18 créée (Bloc 8 Administration) — remplace l'ancienne F-18 immigration désormais F-19. Backlog décalé : F-19→F-20 immobilier, F-20→F-21 SSO, F-21→F-22 collaboration, F-22→F-23 argumentaire | Product owner |
| 2026-03-18 | F-17 SF-17-01 mergée — infrastructure multi-workspace (is_primary, workspace_invitations, findByUserAndPrimaryTrue dans 9 services) | Product owner |
| 2026-03-18 | F-17 SF-17-02 mergée — API REST membres et invitations (6 endpoints, token accept, is_primary bascule, 90 tests) | Product owner |
| 2026-03-18 | F-17 SF-17-03 mergée — service email invitations Spring Mail SMTP, fail-open, Brevo en prod (95 tests) | Product owner |
| 2026-03-18 | F-17 SF-17-04 mergée — frontend membres, acceptation invitation (WorkspaceMembersComponent, InviteAcceptComponent, lien sidenav, routes, 87 tests Karma). F-17 marquée Terminée | Product owner |
| 2026-03-18 | F-19 SF-19-01 mergée — SDK stripe-java, colonnes stripe_customer_id/stripe_subscription_id, plan FREE trial 14j, StripeCustomerService fail-open, PlanLimitService FREE (101 tests) | Product owner |
| 2026-03-18 | F-19 SF-19-02 mergée — expiration FREE trial : isExpiredFree(), gates lecture seule (limites = 0), 108 tests | Product owner |
| 2026-03-18 | F-19 SF-19-03 mergée — webhook Stripe : POST /api/v1/stripe/webhook public, vérification signature, checkout.session.completed/subscription.updated/deleted, 115 tests | Product owner |
| 2026-03-18 | F-19 SF-19-04 mergée — endpoint POST /api/v1/stripe/checkout-session, création Checkout Session, retour checkoutUrl, 123 tests | Product owner |
| 2026-03-18 | F-19 SF-19-05 mergée — frontend billing : page pricing, upgrade Stripe Checkout, bannière trial FREE, 101 tests frontend. F-19 marquée Terminée | Product owner |
| 2026-03-19 | F-18 SF-18-01 mergée — API REST GET /api/v1/admin/usage, agrégation par user/dossier, accès OWNER/ADMIN, 13 tests. F-18 marquée En cours | Product owner |
| 2026-03-19 | F-18 SF-18-02 mergée — page admin frontend, tableaux triables/paginés par dossier et user, gestion 403, 7 tests Karma. F-18 marquée Terminée | Product owner |
| 2026-03-19 | F-17 SF-17-05 mergée — fix race condition acceptation invitation : ShellComponent bloque router-outlet (ready signal) jusqu'au reload workspace post-acceptation, fail-open, 4 tests Karma | Product owner |
| 2026-03-20 | F-02 SF-02-03 mergée — nom workspace obligatoire à l'onboarding : suppression auto-création, POST /api/v1/workspaces, authGuard → /onboarding si 404, OnboardingComponent, 22 tests | Product owner |
| 2026-03-20 | F-17 SF-17-06 mergée — workspace switcher : GET /api/v1/workspaces, POST /{id}/switch (403 si non membre), dropdown header si >1 workspace, WorkspaceResponse+primary, 18 tests | Product owner |
| 2026-03-20 | F-18 SF-18-03 mergée — refactoring page admin : suppression tokens/coûts, ajout section Plan (plan, quota, expiry trial) + section Membres (email, rôle, lien /workspace/members), 4 tests Karma | Product owner |
| 2026-03-20 | F-25 ajoutée en V1 (Bloc 9 Super-administration plateforme) — super-admin is_super_admin, tous les workspaces, consommation LLM, suppression workspace/utilisateur, route /super-admin | Product owner |
| 2026-03-20 | F-25 SF-25-01 mergée — colonne is_super_admin sur users (migration 021), SuperAdminService + Controller, GET /api/v1/super-admin/workspaces (memberCount, expiresAt), 6 tests. F-25 marquée En cours | Product owner |
| 2026-03-20 | F-25 SF-25-02 mergée — GET /api/v1/super-admin/usage, agrégation tokens/coûts par workspace via SQL natif, workspace sans usage → 0, conversion UUID H2, 11 tests | Product owner |
| 2026-03-20 | F-25 SF-25-03 mergée — DELETE /api/v1/super-admin/workspaces/{id}, suppression cascade atomique (15 tables), Stripe cancel fail-open, 15 tests | Product owner |
| 2026-03-20 | F-25 SF-25-04 mergée — DELETE /api/v1/super-admin/users/{id}, suppression user de tous ses workspaces, cascade sole-owner, 21 tests | Product owner |
| 2026-03-20 | F-25 SF-25-05 mergée — page /super-admin, lien header conditionnel, GET /api/v1/super-admin/users, isSuperAdmin dans /api/me, 27 tests backend + 14 Karma. F-25 marquée Terminée | Product owner |
| 2026-03-21 | F-26 créée (Bloc 10 Auth locale) — inscription email/mdp, validation email 24h, login local, fusion OAuth, reset mdp 24h, refonte page auth. 5 subfeatures planifiées (SF-26-01 à SF-26-05) | Product owner |
| 2026-03-21 | F-26 SF-26-01 mergée — migration 022 : password_hash + email_verified sur auth_accounts, tables email_verification_tokens + password_reset_tokens, entités et repositories JPA, 7 tests intégration. F-26 marquée En cours | Product owner |
| 2026-03-21 | F-26 SF-26-02 mergée — POST /api/v1/auth/register + GET /api/v1/auth/verify-email (publics), BCrypt, token 24h, email fail-open, 20 tests | Product owner |
| 2026-03-21 | F-26 SF-26-03 mergée — POST /api/v1/auth/login, /api/me LOCAL, fusion OAuth→LOCAL dans CustomOidcUserService, 18 tests | Product owner |
| 2026-03-21 | F-26 SF-26-04 mergée — POST /api/v1/auth/forgot-password (fail-silent) + POST /api/v1/auth/reset-password, BCrypt, token 24h, 16 tests | Product owner |
| 2026-03-21 | F-26 SF-26-05 mergée — refonte page auth : LoginComponent (onglets Se connecter/S'inscrire, OAuth + local), VerifyEmailComponent, ResetPasswordComponent, AuthService étendu, 16 tests Karma. F-26 marquée Terminée | Product owner |
| 2026-03-25 | F-39 SF-39-01 mergée — notifications SSE : endpoint `GET /api/v1/case-files/{id}/analysis-status/stream`, SseEmitterRegistry, SseNotificationService, AnalysisStatusEvent afterCommit, AnalysisSseService Angular, 15 tests | Product owner |
| 2026-03-26 | F-39 SF-39-02 mergée — GlobalAnalysisNotificationService Angular singleton, toast visible depuis toute page, événements SSE typés par jobType (CASE_ANALYSIS_DONE, ENRICHED_ANALYSIS_DONE, DOCUMENT_ANALYSIS_DONE), DocumentAnalysisService publie l'événement SSE quand tous les docs sont analysés. F-39 marquée Terminée | Product owner |
| 2026-03-25 | F-47 SF-47-01+02 mergées — Sentry backend (sentry-spring-boot-starter-jakarta, captureEvent job FAILED, fail-open) + frontend (@sentry/angular, ErrorHandler, environment.prod.ts), K8s SENTRY_ENV par overlay, 4 tests. F-47 marquée Terminée | Product owner |
| 2026-03-25 | F-51 SF-51-01 mergée — pipeline IA adaptatif : documents < 600k chars envoyés directement en analyse sans chunking (directAnalysis flag), seuil configurable, 5 tests unitaires. F-51 marquée Terminée | Product owner |
| 2026-03-26 | F-52 SF-52-01 mergée — upload multi-documents en mode panier : sélection batch, liste de fichiers en attente, upload parallèle, feedback par fichier. F-52 marquée Terminée | Product owner |
| 2026-03-26 | F-48 SF-48-01 mergée — GET /api/v1/case-files/{id}/stats : documentCount, analysisCount (DONE), totalTokens, isolation workspace, 4 tests unitaires + 4 tests intégration | Product owner |
| 2026-03-26 | F-48 SF-48-02 mergée — bloc métriques Angular dans case-file-detail : CaseFileStats model, CaseFileStatsService, signal stats, rafraîchissement SSE DONE, formatage number pipe, styles JetBrains Mono. F-48 marquée Terminée | Product owner |
| 2026-03-26 | F-53 ajoutée au backlog — gestion statut dossiers (ACTIVE/CLOSED/DELETED), clôture tous membres, réouverture OWNER/ADMIN avec gate quota, suppression soft OWNER, audit logs | Product owner |
| 2026-03-26 | F-53 SF-53-01 mergée — backend : migration 028 (deleted_at), CaseFileStatusService (close/reopen/delete), 3 endpoints PATCH/DELETE, findByIdAndDeletedAtIsNull sur 13 services, 9 tests unitaires + 9 IT, 242 tests total | Product owner |
| 2026-03-26 | F-53 SF-53-02 mergée — frontend : CaseFileStatusService Angular, CaseFileDeleteDialogComponent, boutons Clôturer/Réouvrir/Supprimer avec visibilité par rôle, badge CLOSED → "Clôturé", gestion 402/409, navigation post-delete. F-53 marquée Terminée | Product owner |
| 2026-03-26 | F-54 ajoutée au backlog — comparaison inter-analyses (diff sémantique), tous champs, choix libre des 2 versions, STANDARD et ENRICHED comparables | Product owner |
| 2026-03-26 | F-54 SF-54-01 mergée — backend : endpoint GET /diff?fromId&toId, AnalysisDiffService, AnalysisDiffResponse (SectionDiff<T> générique), findByIdAndCaseFileId, 9 tests unitaires + 5 IT, 251 tests total | Product owner |
| 2026-03-26 | F-54 SF-54-02 mergée — frontend : AnalysisDiffComponent (sélecteur versions, diff coloré 5 sections), route /case-files/:id/diff, bouton "Comparer" conditionnel (≥ 2 versions), modèles AnalysisDiff/SectionDiff<T>, getDiff() service, 10 tests, 206 total. F-54 marquée Terminée | Product owner |
| 2026-03-27 | F-56 SF-56-01 mergée — migration 030 table `analysis_documents`, AnalysisDocumentSnapshot entity, AnalysisDocumentSnapshotService (capture snapshot après analyse réussie), bug-fix pipeline guard : DocumentExtractionRepository ajouté à isPipelineActive() | Product owner |
| 2026-03-27 | F-56 SF-56-02 mergée — SemanticDiffService (Haiku via analyzeFast, 4 états, reason ≤ 20 mots, fallback exactDiff), AnalysisDiffResponse redesigné (SectionDiff non-générique, DiffItem avec reason, TimelineSectionDiff, enriched lists), cache permanent `analysis_diff_cache` (migration 031), 17 tests | Product owner |
| 2026-03-27 | F-56 SF-56-03 mergée — frontend : AnalysisDiffComponent enriched bleu (#EBF3FB/#2980B9), icône auto_awesome, matTooltip reason sur added/removed/enriched, chip enrichi barre résumé, badge ~N par section, modèles TypeScript DiffItem/TimelineDiffItem/SectionDiff/TimelineSectionDiff mis à jour, 14 tests. F-56 marquée Terminée | Product owner |
| 2026-03-27 | F-56 fix post-merge (#123) — max_tokens 8096 (troncature JSON Haiku), prompt sans FROM/TO, bouton Comparer explicite, alerte ordre inversé, hint convention, tooltip CSS .mdc-tooltip__surface, espace bas de page via ::after | Product owner |
| 2026-03-28 | F-56 SF-56-04 mergée — diff sémantique parallèle : 5 appels Haiku via virtual threads (un par section, 2048 tokens chacun), fallback exact diff par section, buildContext() partagé, 14 tests | Product owner |
| 2026-03-28 | F-56 SF-56-05 mergée — garde relance enrichie : 409 si aucune AiQuestionAnswer postérieure à la dernière analyse enrichie DONE, premier enrichissement toujours autorisé, snackbar frontend, 3 nouveaux tests (total 276) | Product owner |
| 2026-03-28 | F-56 SF-56-06 mergée — snapshot Q&A par analyse enrichie : migration 032 table `analysis_qa_snapshots`, AnalysisQaSnapshot entity + repository + service, snapshot figé à la création de l'analyse enrichie, buildContext() utilise snapshot avec fallback courant (rétrocompat), 277 tests. F-56 marquée Terminée | Product owner |
| 2026-03-28 | F-57 SF-57-01 mergée — backend ré-answering : confirmation comportement existant + 3 tests (U-04 unitaire, I-07 deux entrées/dernière prioritaire, I-08 garde SF-56-05), fix pré-existant legalDomain manquant dans IT, 278 tests | Product owner |
| 2026-03-28 | F-57 SF-57-02 mergée — frontend : bouton "Modifier" inline sur réponses Q&A, formulaire pré-rempli, signal editingQuestionId, startEdit/cancelEdit/submitEdit, 7 nouveaux tests (T-10→T-16), 232 tests frontend. F-57 marquée Terminée | Product owner |
| 2026-03-28 | F-58 SF-58-01 mergée — backend repricing : PlanLimitService 4 plans (FREE/SOLO/TEAM/PRO) avec 6 dimensions de quotas, StripeWebhookService/CheckoutService SOLO+TEAM+PRO, migration 033 STARTER→SOLO, application.yml price-id-solo/team/pro, CI/CD secrets mis à jour, 72 tests | Product owner |
| 2026-03-28 | F-59 ajoutée — UX diff : filtres chips, stats bar sticky, sections collapsibles, pré-sélection 2 dernières versions, raisons inline | Product owner |
| 2026-03-28 | F-58 SF-58-02 mergée — frontend repricing : workspace-billing 4 plans (SOLO Recommandé, boutons différenciés), landing 4 cartes pricing (re-synthèse enrichie ✅ sur tous plans payants), workspace-admin PLAN_QUOTA, super-admin planLabel, 238 tests. F-58 marquée Terminée | Product owner |
| 2026-03-28 | F-59 SF-59-01 mergée — AnalysisDiffComponent : signals activeFilter/unchangedVisible/collapsedSections, computed totaux, stats bar sticky, chips filtres, toggle inchangés, auto-collapse sections vides, collapsible au clic, raisons inline, 238 tests. F-59 marquée Terminée | Product owner |
| 2026-03-28 | F-58 SF-58-03 mergée — fix affichage re-synthèse enrichie FREE : ❌ disabled → ✅ "(1 essai)" dans workspace-billing et landing, aligné avec quota backend (=1) | Product owner |
| 2026-03-28 | F-60 SF-60-01 mergée — normalisation nom workspace en majuscules : toUpperCase() backend (createWorkspace + createDefaultWorkspace) + transformation live frontend onboarding, 238 tests. F-60 marquée Terminée | Product owner |
