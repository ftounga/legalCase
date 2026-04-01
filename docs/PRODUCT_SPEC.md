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
| F-20 | Droit de l'immigration | V2 — **Terminée** | Domaine DROIT_IMMIGRATION activé dans F-55 : prompts IA dynamiques via LegalDomainPromptBuilder, migration 029, sélection domaine à l'onboarding (DomainPickerDialog). Couvert par SF-55-01 et SF-55-02 mergées 2026-03-26. |
| F-21 | Droit immobilier | ~~V3~~ — **Abandonné** | Remplacé par DROIT_FAMILLE (déjà implémenté dans F-55). Droit immobilier jugé moins rentable pour la cible V2. |

### Pipeline IA & qualité

| ID | Feature | Cible | Notes |
|----|---------|-------|-------|
| F-98 | Génération de courrier / conclusions | V4 — `À spécifier` | Génération d'un premier draft de courrier (lettre de mise en demeure, conclusions, courrier de contestation) basé sur la synthèse du dossier. Brouillon non contractuel avec mention explicite "document généré par IA — à valider par l'avocat". Garde-fous obligatoires : watermark, disclaimer légal, pas d'envoi direct. |
| F-97 | Détection automatique des délais légaux | V3 — `À spécifier` | Extension de F-69 : l'IA détecte et propose les délais légaux applicables depuis les documents (date de licenciement → délai de recours prud'homal, date de naissance → délai de reconnaissance, etc.) sans saisie manuelle. L'avocat valide ou rejette chaque délai proposé avant persistance. |
| F-96 | Checklist procédurale interactive | V3 — **Terminée** | L'IA génère après l'analyse un champ `points_procedure` (étapes légales à vérifier selon le type de dossier). L'avocat coche chaque point : ✅ Vérifié / ❌ Non respecté / ⚠️ À vérifier. Les points "Non respecté" alimentent la prochaine re-synthèse enrichie. Table `procedure_checks`. SF-96-01 mergée 2026-04-01 (prompt + extraction + CRUD statuts, 402 tests), SF-96-02 mergée 2026-04-01 (panneau checklist SynthesisComponent, 3 boutons statut, 416 tests), SF-96-03 mergée 2026-04-01 (injection NON_COMPLIANT dans prompt enrichi, fail-open, 398 tests). |
| F-95 | Export Word (.docx) de la synthèse | V3 — `À spécifier` | Export de la synthèse en fichier .docx éditable (sections structurées : timeline, faits, points juridiques, risques, questions ouvertes, pièces manquantes). Complément au PDF existant (F-40). Les avocats travaillent dans Word. |
| F-94 | Score de risque global du dossier | V3 — **Terminée** | Indicateur synthétique Faible/Moyen/Élevé + score 0-100 calculé depuis les risques identifiés dans la synthèse. Visible sur la liste des dossiers et en haut de la page synthèse. Calculé par le LLM lors de la case analysis. SF-94-01 mergée 2026-04-01 : score_risque dans prompts, migration 041 (risk_level/risk_score), populateRiskScore fail-open, CaseFileResponse exposé, 405 tests. SF-94-02 mergée 2026-04-01 : badge coloré dans CaseFilesListComponent et SynthesisComponent, couleurs design system, 422 tests. |
| F-93 | Traçabilité des sources IA | V3 — **Terminée** | Chaque fait, risque et point juridique cite le document source et l'extrait exact dont il est tiré. Renforce la confiance de l'avocat dans les conclusions IA. Différenciant fort vs Jimini/Ordalie. SF-93-01 mergée 2026-04-01 : AnalysisItem{texte,source,extrait}, parse fail-open, filename dans prompt, 401 tests. SF-93-02 mergée 2026-04-01 : badge source dans SynthesisComponent, extrait en italique, 422 tests. |
| F-92 | Détection de pièces manquantes | V3 — **Terminée** | Section `pieces_manquantes` dans la synthèse IA. SF-92-01 mergée 2026-03-31 : prompt CaseAnalysisService+EnrichedAnalysisService, extraction fail-open, truncation, 389 tests. SF-92-02 mergée 2026-03-31 : panneau conditionnel SynthesisComponent, icône orange, rétrocompat, 411 tests. |
| F-78 | Page contact — formulaire email | **Terminée** | SF-78-01 (backend POST /api/v1/contact, 2 emails fail-open) + SF-78-02 (ContactComponent /contact, 5 champs, lien footer) mergées 2026-03-30. 22 tests backend + 6 tests frontend. |
| F-77 | Google Analytics 4 — tracking + bannière consentement RGPD | **Terminée** | SF-77-01 mergée 2026-03-30 : ConsentService + CookieConsentBannerComponent, injection dynamique GA4, localStorage, gaId G-2JPL8JTXE7, 10 tests. SF-77-02 mergée 2026-03-31 : AnalyticsService fail-open, events analysis_launched/pdf_exported/upgrade_clicked, fix UI barre filtres dossiers (toggles compacts), 406 tests verts. |
| F-75 | SEO technique — meta tags, Open Graph, sitemap, robots.txt | **Terminée** | index.html enrichi (OG + Twitter Card), LandingComponent/LegalPageComponent injectent Title+Meta dynamiquement, robots.txt bloque routes auth, sitemap.xml liste 4 URLs publiques. SF-75-01 mergée 2026-03-30. 328 tests verts. |
| F-74 | Pages légales — mentions légales, CGU, politique de confidentialité | **Terminée** | 3 pages statiques Angular accessibles publiquement (/mentions-legales, /cgu, /privacy). Lien dans le footer de la landing page. |
| F-73 | Séquence email onboarding | Terminée | 5 emails automatiques post-inscription : J+0 bienvenue, J+2 tip analyse (si aucune analyse), J+5 tip partage client, J+12 conversion avant expiration trial, J+15 récupération post-expiration. Table `email_sends` pour éviter les doublons. Brevo comme provider. |
| F-65 | Notifications email d'analyse terminée | V2 — **Terminée** | Email envoyé au créateur du dossier quand analyse STANDARD ou ENRICHED passe DONE. AnalysisNotificationService (@EventListener), EmailService.sendAnalysisDone(), fail-open. SF-65-01 mergée 2026-03-29. |
| F-29 | Limites pipeline IA configurables | V2 — **Terminée** | Externalisation des limites hardcodées de F-28 via `AnalysisLimitsProperties` (`@ConfigurationProperties`). Configurable par domaine juridique dans `application.yml`. `AnalysisJsonTruncator` paramétré par `LevelLimits` (chunk/document/dossier). SF-29-01 mergée 2026-03-28. |
| F-39 | Notifications temps réel | V2 — **Terminée** | SSE : notifier l'avocat quand une analyse se termine. Endpoint `GET /api/v1/case-files/{id}/analysis-status/stream`, `SseEmitterRegistry`, `SseNotificationService`, `AnalysisStatusEvent` afterCommit, `AnalysisSseService` Angular. SF-39-01 mergée 2026-03-25. SF-39-02 mergée 2026-03-26 : `GlobalAnalysisNotificationService` singleton Angular, toast MatSnackBar visible depuis toute page, événements SSE typés par jobType. |
| F-40 | Export PDF de la synthèse | V2 — **Terminée** | Générer un PDF structuré de la synthèse (timeline, faits, points juridiques, risques). Utile pour partager avec un client ou archiver. Implémenté 100% frontend via pdfmake (SF-40-01, mergé 2026-03-24). |

### Admin & pilotage

| ID | Feature | Cible | Notes |
|----|---------|-------|-------|
| F-76 | Tableau de bord super-admin — métriques produit | V3 — **Terminée** | SF-76-01 mergée 2026-03-31 : GET /api/v1/super-admin/metrics, 9 métriques agrégées (totalWorkspaces, activeWorkspaces30d, inactiveWorkspaces30d, trialWorkspaces, paidWorkspaces, conversionRatePct, analysesLast7Days, analysesLast30Days, newWorkspacesLast30Days), 368 tests verts. SF-76-02 mergée 2026-03-31 : section métriques en haut de /super-admin, 9 cartes en grille 3 colonnes responsive, design system, 346 tests verts. |
| F-79 | Pagination super-admin — workspaces et utilisateurs | V3 — **Terminée** | SF-79-01 mergée 2026-03-31 : GET /workspaces et /users retournent Page<T>, @PageableDefault, 29 tests verts. SF-79-02 mergée 2026-03-31 : MatPaginator sous chaque tableau, PageResponse<T>, appels API ciblés, 10 tests verts. |

### UX & exploitation

| ID | Feature | Cible | Notes |
|----|---------|-------|-------|
| F-91 | Hub opérationnel super-admin — liens plateformes tierces | V3 — **Terminée** | SF-91-01 mergée 2026-03-31 : section "Outils & monitoring" dans /super-admin, 7 liens (GA4, Sentry, Stripe, Brevo, n8n, AWS, RabbitMQ), ingress RabbitMQ staging + prod, rabbitmqUrl par environment.ts, 409 tests verts. |
| F-89 | Refonte UX de la comparaison d'analyses | V3 — **Terminée** | SF-89-01 mergée 2026-03-31 : auto-trigger effect(), compteurs 26px, callout Raison IA, border-left section cards, empty state guidant, 19 tests. SF-89-02 mergée 2026-03-31 : 5 colonnes nullable (faits/points/risques/questionsOuvertes/timeline count) sur case_analyses, populateCounts() fail-open, VersionSummary étendu, version cards affichent les stats si non-null, 9 tests backend + 35 tests frontend. |
| F-88 | Tour d'onboarding avec dossier de démonstration | V3 — **Terminée** | SF-88-01 mergée 2026-03-31 : advanceToStep2() crée un dossier "Dossier de démonstration" si workspace vide, stocke l'ID en mémoire, navigue dans le dossier, le supprime silencieusement à la fin/skip via cleanup(). Effet spotlight : tour-backdrop z-index 9000 + .tour-spotlight (box-shadow 9999px) z-index 9001 remplace l'outline doré. 18 tests verts. |
| F-87 | Export complet d'un dossier (ZIP) | V3 — **Terminée** | SF-87-01 mergée 2026-03-31 : GET /api/v1/case-files/{id}/export, ZIP en mémoire (dossier.json, documents.csv, notes.txt, delais.txt, synthese.json optionnel), Content-Disposition, bouton Exporter dans case-file-detail, 6 unit + 4 IT + 3 tests frontend. |
| F-86 | Renommage et modification d'un dossier | V3 — **Terminée** | SF-86-01 mergée 2026-03-31 : PATCH /api/v1/case-files/{id}, CaseFileUpdateRequest, audit log CASE_FILE_UPDATED, CaseFileEditDialogComponent pré-rempli, bouton Modifier dans case-file-detail, 4 IT + 3 unit + 5 tests frontend. |
| F-85 | Barre de progression globale | V3 — **Terminée** | SF-85-01 mergée 2026-03-31 : LoadingService (signal-based, compteur requêtes actives), loadingInterceptor, MatProgressBarModule dans AppComponent, barre fixée top:0 z-index:9999, 4 tests verts. |
| F-84 | Filtres sur la liste des dossiers | V3 — **Terminée** | SF-84-01 mergée 2026-03-31 : sélecteurs Statut (Ouvert/Clôturé) et Domaine (Travail/Immigration/Famille) dans /case-files, filtrage client-side cumulable avec F-80 et F-82, reset au changement workspace, 5 tests verts (23 total). |
| F-83 | Page d'erreur 404 — route inconnue | V3 — **Terminée** | SF-83-01 mergée 2026-03-31 : NotFoundComponent, route wildcard **, lien retour /case-files, 3 tests verts. |
| F-82 | Tri de la liste des dossiers | V3 — **Terminée** | SF-82-01 mergée 2026-03-31 : MatSortModule, 4 colonnes triables, compatible filtre F-80, 18 tests verts. |
| F-81 | Gestion expiration de session — returnUrl + snackbar | V3 — **Terminée** | SF-81-01 mergée 2026-03-31 : intercepteur 401 snackbar + sessionStorage returnUrl, login local navigue returnUrl, authGuard OAuth2 redirect returnUrl. 19 tests verts. |
| F-80 | Recherche dans la liste des dossiers | V3 — **Terminée** | SF-80-01 mergée 2026-03-31 : barre de recherche dans /case-files, filtre client par titre (insensible casse), bouton ×, message différencié, responsive mobile, 14 tests verts. |
| F-64 | Recherche full-text dans les synthèses | V2 — **Terminée** | Rechercher un mot-clé dans toutes les synthèses du workspace (faits, points juridiques, risques, timeline). Résultats groupés par dossier avec extrait contextuel et terme surligné. SF-64-01 mergée 2026-03-29 (backend GET /api/v1/search). SF-64-02 mergée 2026-03-29 (frontend /search, debounce, HighlightTermPipe). |
| F-66 | Modèles de questions Q&A réutilisables | ~~Abandonné~~ | L'IA génère les questions pertinentes depuis les documents — des modèles prédéfinis n'apportent pas de valeur différenciante. Arbitrage 2026-03-29. |
| F-69 | Suivi des délais légaux | V2 — **Terminée** | Associer des échéances à un dossier (prescription, délai d'appel, délai de réponse). Alertes J-15 et J-7. Affichage dans la page dossier. Scoped au workspace. SF-69-01 mergée 2026-03-29 (backend CRUD, migration 037). SF-69-02 mergée 2026-03-29 (frontend CaseDeadlinesSectionComponent, indicateur J-X). SF-69-03 mergée 2026-03-29 (DeadlineAlertService @Scheduled 8h, fail-open). |
| F-71 | Sections repliables — délais et notes | V2 — **Terminée** | Rendre les sections Délais légaux et Notes internes de la page dossier repliables/dépliables via un clic sur le header. Badge compteur affiché quand la section est repliée. État géré par signal Angular. SF-71-01 mergée 2026-03-29. |
| F-72 | Animations UI | V2 — **Terminée** | Animations légères sur les écrans principaux : fade-in sur transitions de route (200ms), stagger CSS sur lignes de tableau (30ms/ligne), @fadeInUp Angular sur login/headers/sections, @listStagger sur listes de synthèse (faits/risques/points/questions/timeline), animation fadeInUp sur tour overlay. Fichier `shared/animations.ts` pour réutilisation. SF-72 mergée 2026-03-29. |
| F-70 | Notes internes sur un dossier | V2 — **Terminée** | Permettre à l'avocat d'ajouter des annotations libres sur un dossier, séparées de l'analyse IA. Non visibles par le client. Scoped au workspace. SF-70-01 mergée 2026-03-29 (backend CRUD, migration 036). SF-70-02 mergée 2026-03-29 (frontend CaseNotesSectionComponent). |
| F-67 | Wizard d'onboarding guidé | V2 — **Terminée** | Product tour interactif 5 étapes : carte flottante non-bloquante positionnée via getBoundingClientRect(), surlignage tour-highlight, cross-navigation (NavigationEnd + resize). SF-67-01 mergée 2026-03-29 (dialog). SF-67-02 mergée 2026-03-29 (TourService, TourOverlayComponent, 4 data-tour-target, 15 tests + E2E). |
| F-60 | Normalisation nom workspace en majuscules | V2 — **Terminée** | Le nom du workspace est converti en majuscules au moment de la saisie et stocké en majuscules en base. Concerne la création et la modification du nom. SF-60-01 mergée 2026-03-28. |
| F-61 | Responsive mobile — Shell & navigation | V2 — **Terminée** | Sidebar rétractable sur mobile (menu hamburger), header adaptatif, navigation accessible sur téléphone. SF-61-01 mergée 2026-03-28. |
| F-62 | Responsive mobile — Écrans principaux | V2 — **Terminée** | Adaptation mobile de case-file-detail, synthesis et liste des dossiers — colonnes empilées, upload accessible, synthèse lisible. SF-62-01 mergée 2026-03-28. |
| F-63 | Responsive mobile — Écrans secondaires | V2 — **Terminée** | Adaptation mobile des écrans secondaires : analysis-diff (sélecteurs empilés), membres (colonnes masquées), admin (colonne email masquée). Login/onboarding/billing déjà responsive. SF-63-01 mergée 2026-03-28. |
| F-41 | Partage dossier lecture seule | V2 — **Terminée** | Lien temporaire (token, expiration configurable) permettant à un client de consulter la synthèse d'un dossier sans compte. Accès lecture seule strict. SF-41-01 mergée 2026-03-28 (backend), SF-41-02 mergée 2026-03-28 (frontend). |
| F-49 | Top-up de crédits tokens | V2 — **Terminée** | SF-49-01 mergée 2026-03-28 : table `credit_purchases`, `TokenPack` enum (1M/5M/20M), `POST /api/v1/stripe/topup-session`, webhook mode=payment, `PlanLimitService` intègre les crédits restants. SF-49-02 mergée 2026-03-28 : section top-up dans billing (3 cartes 1M/5M/20M), `createTopupSession()`, gestion `?topup=success/canceled`. |
| F-42 | Export CSV journal d'actions | V2 — **Terminée** | Bouton export dans `/workspace/audit-logs`. Génère un CSV de toutes les entrées (ou des entrées filtrées). SF-42-01 mergée 2026-03-28 (backend), SF-42-02 mergée 2026-03-28 (frontend). |
| F-43 | Filtre par plage de dates — journal d'actions | V2 — **Terminée** | Sélecteur de dates (date début / date fin) dans l'écran `/workspace/audit-logs`. SF-43-01 mergée 2026-03-28 (backend), SF-43-02 mergée 2026-03-28 (frontend). |
| F-44 | Pagination et tri côté serveur — journal d'actions | V2 — **Terminée** | `GET /api/v1/admin/audit-logs` retourne `Page<AuditLogResponse>` avec `@PageableDefault(size=20, sort=createdAt DESC)`. Filtres `from`, `to`, `action` via `Specification<AuditLog>`. Frontend : `MatPaginator` (10/20/50), `onPageChange(PageEvent)`, remise à 0 au changement de date. SF-44-01 mergée 2026-03-28 (backend), SF-44-02 mergée 2026-03-28 (frontend). |
| F-45 | Pagination côté serveur — liste des dossiers | V2 — **Terminée** | La liste des dossiers est paginée côté serveur. Backend : `GET /api/v1/case-files?page=X&size=Y` avec `@PageableDefault` Spring. Frontend : `MatPaginatorModule`, `totalElements`, `onPageChange(PageEvent)`. Déjà en production. |
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
| F-23 | Collaboration avancée | ~~V2+~~ — **Abandonné** | Partage de dossiers déjà couvert par le modèle workspace (tous les membres voient tous les dossiers). Notes internes couvertes par F-70. Arbitrage 2026-03-31. |
| F-24 | Génération d'argumentaire | V2+ | Hors scope V1 — complexité juridique trop élevée |
| F-90 | Chat comme contexte de l'analyse enrichie | V3 — **Terminée** | SF-90-01 mergée 2026-03-31 : garde étendu hasNewAnswers||hasNewChatMessages, buildChatSummary() Haiku fail-open (max 512 tokens), section [Échanges libres] injectée dans prompt enrichi, 11 tests unitaires + 8 IT. |

### Infrastructure & qualité

| ID | Feature | Cible | Notes |
|----|---------|-------|-------|
| F-99 | Migration tests frontend Karma → Jest | V3 — **Terminée** | Karma + Chrome Headless remplacés par Jest + jest-preset-angular@14. 422/422 tests verts. CI `npx jest --ci`. Shim Jasmine (spyOn, createSpyObj), polyfills jsdom (URL.createObjectURL, IntersectionObserver), restoreMocks. SF-99-01 mergée 2026-04-01. |
| F-46 | Tests E2E smoke branchés CI | V2 — **Terminée** | Workflow `smoke.yml` déclenché après `Backend CI/CD` et `Frontend CI/CD` (workflow_run, conclusion success) + workflow_dispatch. Playwright chromium, Node 22, secrets E2E_LOCAL_EMAIL/PASSWORD, rapport artifact sur échec. Compte e2e créé en base staging. SF-46-01 mergée 2026-03-28. |
| F-47 | Monitoring & alertes applicatives | V2 — **Terminée** | Sentry backend (`sentry-spring-boot-starter-jakarta`) + frontend (`@sentry/angular`). Capture automatique exceptions + événement manuel sur job IA FAILED. DSN via env var, désactivé en dev. SF-47-01 + SF-47-02 mergées 2026-03-25. |
| F-50 | Déploiement V1 — AWS EKS | V1 — **Terminée** | Infrastructure AWS (Terraform) + Dockerfiles + Kubernetes manifests + CI/CD GitHub Actions. Région eu-west-3 (Paris). Cluster EKS unique avec namespaces staging/production. RDS PostgreSQL, S3 AWS, ECR, RabbitMQ sur EKS. 6 subfeatures toutes terminées : SF-50-01 Dockerfiles ✅, SF-50-02 Terraform infra ✅, SF-50-03 K8s manifests ✅, SF-50-04 CI/CD ✅, SF-50-05 Config prod OAuth2 (Google)/Stripe ✅, SF-50-06 Monitoring Sentry ✅. Premier déploiement production 2026-03-25. |

---

## Historique des évolutions

| Date | Modification | Validé par |
|------|-------------|------------|
| 2026-04-01 | F-93 Terminée — SF-93-02 mergée : badge source + extrait dans SynthesisComponent, 422 tests | Product owner |
| 2026-04-01 | F-94 Terminée — SF-94-02 mergée : badge score de risque dans liste et synthèse, couleurs design system, 422 tests | Product owner |
| 2026-04-01 | F-99 Terminée — SF-99-01 mergée : Karma → Jest, 422/422 verts, CI migrée, shim Jasmine + polyfills jsdom | Product owner |
| 2026-04-01 | F-99 ajoutée — Migration tests frontend Karma → Jest (infrastructure & qualité, V3) | Product owner |
| 2026-04-01 | F-94 SF-94-01 mergée — backend score de risque : prompts, migration 041, populateRiskScore fail-open, CaseFileResponse enrichi, 405 tests | Product owner |
| 2026-04-01 | F-93 SF-93-01 mergée — backend sources IA : AnalysisItem{texte,source,extrait}, filename dans prompt, SemanticDiff adapté, 401 tests | Product owner |
| 2026-04-01 | F-96 Terminée — SF-96-03 mergée : injection NON_COMPLIANT dans prompt enrichi, fail-open, 398 tests | Product owner |
| 2026-04-01 | F-96 SF-96-02 mergée — frontend checklist procédurale : panneau SynthesisComponent, 3 boutons statut, spinner par ligne, rétrocompat, 416 tests | Product owner |
| 2026-04-01 | F-96 SF-96-01 mergée — backend checklist procédurale : prompt points_procedure + extraction fail-open + CRUD statuts + isolation workspace, 402 tests | Product owner |
| 2026-04-01 | F-97/F-98 ajoutées — Délais légaux auto-détectés (extension F-69) + Génération de courrier V4 avec garde-fous | Product owner |
| 2026-04-01 | F-96 ajoutée — Checklist procédurale interactive niveau 2 (statuts + intégration re-synthèse) | Product owner |
| 2026-04-01 | F-93/F-94/F-95 ajoutées au backlog — Traçabilité sources IA, Score de risque, Export Word (.docx) | Product owner |
| 2026-03-31 | F-92 Terminée — SF-92-02 mergée : panneau Pièces manquantes SynthesisComponent, icône orange, 411 tests | Product owner |
| 2026-03-31 | F-92 ajoutée + SF-92-01 mergée — backend pieces_manquantes : prompt + extraction fail-open + truncation, 389 tests | Product owner |
| 2026-03-31 | F-91 Terminée — SF-91-01 : hub outils super-admin (7 liens, ingress RabbitMQ staging+prod, env-based URLs, 409 tests) | Product owner |
| 2026-03-31 | F-90 Terminée — SF-90-01 (garde étendu Q&A||chat, buildChatSummary Haiku fail-open, section [Échanges libres] conditionnelle, 11 unit + 8 IT) | Product owner |
| 2026-03-31 | F-90 ajoutée au backlog — Chat comme contexte analyse enrichie (résumé Haiku, garde étendu Q&A||chat) | Product owner |
| 2026-03-31 | F-23 Abandonnée — partage couvert par modèle workspace, notes couvertes par F-70 | Product owner |
| 2026-03-31 | F-89 Terminée — SF-89-02 (5 colonnes nullable case_analyses, populateCounts fail-open, version cards stats, migration 039, 9 tests backend + 35 frontend) | Product owner |
| 2026-03-31 | F-89 SF-89-01 mergée — auto-trigger effect(), compteurs 26px, callout Raison IA, border-left section cards, empty state guidant, 19 tests | Product owner |
| 2026-03-31 | F-89 ajoutée au backlog — Refonte UX comparaison d'analyses (2 subfeatures prévues : frontend pur + stats par version backend) | Product owner |
| 2026-03-31 | F-88 Terminée — SF-88-01 (advanceToStep2 dossier demo, cleanup silencieux, effet spotlight backdrop+box-shadow, 18 tests) | Product owner |
| 2026-03-31 | F-87 Terminée — SF-87-01 (GET /export ZIP, 5 fichiers, CaseFileExportService, bouton Exporter, 6 unit + 4 IT + 3 frontend) | Product owner |
| 2026-03-31 | F-86 Terminée — SF-86-01 (PATCH /case-files/{id}, audit CASE_FILE_UPDATED, CaseFileEditDialogComponent, 4 IT + 3 unit + 5 frontend) | Product owner |
| 2026-03-31 | F-85 Terminée — SF-85-01 (LoadingService signal-based, loadingInterceptor, MatProgressBarModule, barre fixe top:0, 4 tests) | Product owner |
| 2026-03-31 | F-84 Terminée — SF-84-01 (sélecteurs Statut+Domaine /case-files, filtrage client-side, reset workspace, 5 nouveaux tests, 23 total) | Product owner |
| 2026-03-31 | F-83 Terminée — SF-83-01 (NotFoundComponent, route **, lien retour /case-files, 3 tests) | Product owner |
| 2026-03-31 | F-82 Terminée — SF-82-01 (MatSortModule, 4 colonnes triables, compatible recherche F-80, 18 tests) | Product owner |
| 2026-03-31 | F-81 Terminée — SF-81-01 (intercepteur 401 snackbar + sessionStorage returnUrl, login local + authGuard OAuth2 redirect, 19 tests) | Product owner |
| 2026-03-31 | F-80 Terminée — SF-80-01 (barre recherche /case-files, filtre client par titre, bouton ×, responsive, 14 tests) | Product owner |
| 2026-03-31 | F-79 Terminée — SF-79-01 (Page<T> workspaces+users, @PageableDefault, 29 tests) + SF-79-02 (MatPaginator, PageResponse<T>, appels ciblés, 10 tests) | Product owner |
| 2026-03-31 | F-76 Terminée — SF-76-01 (GET /api/v1/super-admin/metrics, 9 métriques, 368 tests) + SF-76-02 (section métriques /super-admin, 9 cartes responsive, 346 tests) | Product owner |
| 2026-03-30 | F-78 Terminée — SF-78-01 (POST /api/v1/contact, 2 emails, 22 tests) + SF-78-02 (ContactComponent /contact, 5 champs, footer link, 6 tests) | Product owner |
| 2026-03-31 | F-77 Terminée — SF-77-02 mergée : AnalyticsService fail-open (analysis_launched, pdf_exported, upgrade_clicked), redesign barre filtres dossiers (toggles compacts), 406 tests verts | Product owner |
| 2026-03-30 | F-77 Terminée — SF-77-01 mergée : ConsentService (injection GA4 dynamique, localStorage), CookieConsentBannerComponent (responsive, design system), gaId G-2JPL8JTXE7, 10 tests verts | Product owner |
| 2026-03-30 | F-75 Terminée — SF-75-01 mergée : index.html OG + Twitter Card, LandingComponent Title+Meta dynamiques, LegalPageComponent title depuis route data, robots.txt, sitemap.xml, 328 tests verts | Product owner |
| 2026-03-29 | F-74 Terminée — SF-74-01 mergée : LegalPageComponent standalone, 3 routes publiques (/mentions-legales /privacy /cgu), footer landing mis à jour, 324 tests verts | Product owner |
| 2026-03-29 | F-73 Terminée — SF-73-01 mergée : table email_sends (migration 038), EmailSend entity+repo, 5 méthodes sendOnboarding* fail-open, hook J+0 dans createDefaultWorkspace(), OnboardingEmailScheduler cron 8h J+2/J+5/J+12/J+15, 366 tests verts. Landing page V2 + backlog marketing + règle gouvernance marketing inclus. | Product owner |
| 2026-03-29 | F-64 Terminée — SF-64-02 mergée : SearchComponent (/search), debounce 400ms, switchMap, HighlightTermPipe, 282 tests Angular | Product owner |
| 2026-03-29 | F-64 SF-64-01 mergée — endpoint GET /api/v1/search?q= (ILIKE workspace-scoped, max 50 résultats, max 3 extraits, 14 tests) | Product owner |
| 2026-03-29 | F-65 Terminée — SF-65-01 mergée : AnalysisNotificationService, sendAnalysisDone(), 10 tests, 349/349 | Product owner |
| 2026-03-29 | F-70 Terminée — SF-70-01 (backend CRUD, migration 036, 9 IT) + SF-70-02 (CaseNotesSectionComponent, 9 tests, 291/291) | Product owner |
| 2026-03-29 | F-69 SF-69-01 mergée — backend CRUD délais, migration 037 table case_deadlines, 9 ITs (tri ASC, isolation workspace) | Product owner |
| 2026-03-29 | F-69 SF-69-02 mergée — CaseDeadlinesSectionComponent, indicateur J-X coloré, 9 tests, 300/300 | Product owner |
| 2026-03-29 | F-69 Terminée — SF-69-03 mergée : DeadlineAlertService @Scheduled 8h, sendDeadlineAlert(), fail-open par membre, 4 tests Mockito | Product owner |
| 2026-03-29 | F-69/F-70 ajoutées au backlog — Suivi des délais légaux, Notes internes sur un dossier | Product owner |
| 2026-03-29 | F-64/F-65/F-66 ajoutées au backlog — Recherche full-text synthèses, Notifications email analyse, Modèles Q&A réutilisables | Product owner |
| 2026-03-29 | fix — Budget tokens admin : getMonthlyTokenBudgetForWorkspace() incluait pas les crédits top-up. Extraction computeCreditsRemaining() partagée. 356 tests verts. | Product owner |
| 2026-03-29 | F-72 fix — Amplification animations (20px/380ms/cubic-bezier) + correction router transition via host:[@fadeInUp] sur 12 composants routés. 314 tests verts. | Product owner |
| 2026-03-29 | F-72 Terminée — Animations UI : router fade-in, stagger CSS mat-row, @fadeInUp login/détail/synthèse, @listStagger listes synthèse, tour overlay CSS. 314 tests verts. | Product owner |
| 2026-03-29 | F-67 Terminée — SF-67-02 mergée : TourService (signals isActive/currentStep), TourOverlayComponent (carte floating, getBoundingClientRect, NavigationEnd+resize), 4 data-tour-target, tour-highlight CSS, 15 tests unitaires/contrat + E2E tour.spec.ts | Product owner |
| 2026-03-29 | F-67 Terminée — SF-67-01 mergée : OnboardingWizardService (localStorage), OnboardingWizardDialogComponent (4 étapes, signal currentStep), trigger CaseFilesListComponent, 26 tests | Product owner |
| 2026-03-29 | F-67 ajoutée au backlog — Wizard d'onboarding guidé 4 étapes, skippable, localStorage | Product owner |
| 2026-03-29 | F-29 Terminée (statut manquant corrigé) — SF-29-01 mergée 2026-03-28 : AnalysisLimitsProperties, LevelLimits, AnalysisJsonTruncator paramétré, configurable par domaine | Product owner |
| 2026-03-28 | F-49 Terminée — SF-49-02 mergée : section top-up billing (3 cartes), createTopupSession(), ?topup=success/canceled, 14 tests Angular | Product owner |
| 2026-03-28 | F-49 SF-49-01 mergée — table credit_purchases, TokenPack 1M/5M/20M, POST /topup-session, webhook mode=payment, PlanLimitService crédits globaux, 331 tests | Product owner |
| 2026-03-28 | F-46 Terminée — SF-46-01 mergée : smoke.yml workflow_run après Backend/Frontend CI, workflow_dispatch, Playwright chromium Node 22, secrets E2E, compte e2e créé staging | Product owner |
| 2026-03-28 | F-44 Terminée — SF-44-01 (backend) + SF-44-02 (frontend) mergées : Page<AuditLogResponse>, JpaSpecificationExecutor, MatPaginator 10/20/50, 10 tests Java + 251 tests Angular | Product owner |
| 2026-03-28 | F-63 Terminée — SF-63-01 mergée : responsive CSS diff (sélecteurs empilés), membres (colonnes masquées), admin (email masqué) | Product owner |
| 2026-03-28 | F-43 Terminée — SF-43-02 mergée : champs date Du/Au, loadLogs() centralisé, snackbar 400, 256/256 tests | Product owner |
| 2026-03-28 | F-43 SF-43-01 mergée — params ?from/to optionnels, 4 repo methods, from>to→400, 9/9 tests | Product owner |
| 2026-03-28 | F-45 Terminée — déjà implémentée : backend Pageable Spring + frontend MatPaginatorModule, PageEvent, totalElements. Marquée Terminée rétroactivement. | Product owner |
| 2026-03-28 | F-42 Terminée — SF-42-02 mergée : bouton "Exporter CSV" dans audit-logs, AuditLogService.exportCsv() Blob, signal exporting, 253/253 tests | Product owner |
| 2026-03-28 | F-42 SF-42-01 mergée — backend GET /export.csv, BOM UTF-8, RFC 4180, sans limite 50 lignes, 6/6 tests | Product owner |
| 2026-03-28 | F-41 Terminée — SF-41-02 mergée : ShareDialogComponent (génération/copie/révocation lien), PublicShareComponent (/share/:token hors shell), CaseFileShareService, 250/250 tests | Product owner |
| 2026-03-28 | F-41 SF-41-01 mergée — backend partage lecture seule : migration 034 case_file_shares, token SecureRandom 64 chars, 4 endpoints (POST/GET/DELETE auth + GET public permitAll), isolation workspace, 310 tests | Product owner |
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
