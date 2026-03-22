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

| ID | Feature | Cible | Notes |
|----|---------|-------|-------|
| F-20 | Droit de l'immigration | V2 | Nouveau domaine juridique — nouveaux prompts LLM |
| F-21 | Droit immobilier | V3 | Nouveau domaine juridique — nouveaux prompts LLM |
| F-22 | SSO entreprise (Azure AD, Google Workspace, SAML) | V2+ | Auth avancée pour cabinets |
| F-23 | Collaboration avancée | V2+ | Partage de dossiers entre membres, commentaires |
| F-24 | Génération d'argumentaire | V2+ | Hors scope V1 — complexité juridique trop élevée |
| F-29 | Limites pipeline IA configurables | V2 | Externaliser les limites hardcodées de F-28 (nb max d'items par champ JSON). Configurable par domaine juridique et/ou par plan (Starter/Pro). Actuellement hardcodé dans `AnalysisJsonTruncator` et les `SYSTEM_PROMPT`. |

---

## Historique des évolutions

| Date | Modification | Validé par |
|------|-------------|------------|
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
