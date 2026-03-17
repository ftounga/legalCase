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
| F-02 | Onboarding & workspace | Premier login → création automatique user + workspace + rôle OWNER. Redirection dashboard. | `Terminée` |

### Bloc 2 — Gestion des dossiers

| ID | Feature | Description | Statut |
|----|---------|-------------|--------|
| F-03 | Création de dossier | Formulaire de création : titre, domaine juridique (EMPLOYMENT_LAW en V1), description optionnelle. | `Terminée` |
| F-04 | Liste & consultation des dossiers | Dashboard principal. Liste paginée des dossiers du workspace. Navigation vers un dossier. Statuts visibles. | `Terminée` |

### Bloc 3 — Documents

| ID | Feature | Description | Statut |
|----|---------|-------------|--------|
| F-05 | Upload de documents | Ajout de fichiers à un dossier. Validation type et taille. Stockage object storage S3-compatible. | `Terminée` |
| F-06 | Extraction de texte | Transformation du fichier brut en texte exploitable. Persistance dans `document_extractions`. Déclenchée automatiquement après upload. | `À spécifier` |
| F-07 | Chunking | Segmentation du texte extrait en chunks. Persistance dans `document_chunks`. Déclenchée automatiquement après extraction. | `À spécifier` |

### Bloc 4 — Pipeline IA

| ID | Feature | Description | Statut |
|----|---------|-------------|--------|
| F-08 | Analyse IA — chunk | Analyse de chaque chunk par le LLM. Persistance dans `chunk_analyses`. Asynchrone. | `À spécifier` |
| F-09 | Analyse IA — document | Synthèse des chunks d'un document. Persistance dans `document_analyses`. Asynchrone. | `À spécifier` |
| F-10 | Analyse IA — dossier | Synthèse globale du dossier. Persistance dans `case_analyses`. Asynchrone. | `À spécifier` |
| F-11 | Suivi des jobs asynchrones | Suivi de la progression des analyses en temps réel. Table `analysis_jobs`. Affichage statut et pourcentage. | `À spécifier` |

### Bloc 5 — Interaction avocat

| ID | Feature | Description | Statut |
|----|---------|-------------|--------|
| F-12 | Restitution de l'analyse | Affichage structuré de la synthèse : timeline, faits, points juridiques, risques, questions ouvertes. | `À spécifier` |
| F-13 | Questions IA interactives | L'IA génère des questions complémentaires après synthèse. Persistance dans `ai_questions`. | `À spécifier` |
| F-14 | Réponses avocat & re-synthèse | L'avocat répond aux questions IA. Persistance dans `ai_question_answers`. Déclenchement d'une nouvelle synthèse enrichie. | `À spécifier` |

### Bloc 6 — Exploitation

| ID | Feature | Description | Statut |
|----|---------|-------------|--------|
| F-15 | Suivi consommation LLM | Traçabilité des tokens et coûts LLM par dossier et par user. Table `usage_events`. | `À spécifier` |
| F-16 | Gestion des abonnements | Plans Starter et Pro. Table `subscriptions`. Contrôle d'accès selon le plan. | `À spécifier` |

### Bloc 7 — Workspace

| ID | Feature | Description | Statut |
|----|---------|-------------|--------|
| F-17 | Gestion des membres workspace | Invitations, attribution des rôles (OWNER, ADMIN, LAWYER, MEMBER), révocation. | `À spécifier` |

---

## Ordre d'implémentation recommandé

```
F-01 → F-02 → F-03 → F-04 → F-05 → F-06 → F-07
                                              ↓
                                   F-08 → F-09 → F-10 → F-11 → F-12 → F-13 → F-14
                                              ↓
                                   F-15, F-16, F-17 (parallélisables)
```

---

## Features hors V1 (backlog)

| ID | Feature | Cible | Notes |
|----|---------|-------|-------|
| F-18 | Droit de l'immigration | V2 | Nouveau domaine juridique — nouveaux prompts LLM |
| F-19 | Droit immobilier | V3 | Nouveau domaine juridique — nouveaux prompts LLM |
| F-20 | SSO entreprise (Azure AD, Google Workspace, SAML) | V2+ | Auth avancée pour cabinets |
| F-21 | Collaboration avancée | V2+ | Partage de dossiers entre membres, commentaires |
| F-22 | Génération d'argumentaire | V2+ | Hors scope V1 — complexité juridique trop élevée |

---

## Historique des évolutions

| Date | Modification | Validé par |
|------|-------------|------------|
| 2026-03-17 | Création initiale — 17 features V1 définies | Product owner |
| 2026-03-17 | F-01 marquée Terminée — 5 subfeatures mergées sur master | Product owner |
| 2026-03-17 | F-02 marquée Terminée — 2 subfeatures mergées sur master | Product owner |
| 2026-03-17 | F-03 marquée Terminée — SF-03-01 mergée sur master | Product owner |
| 2026-03-17 | F-04 marquée Terminée — SF-04-01 (liste) + SF-04-02 (get by id) mergées sur master | Product owner |
| 2026-03-17 | F-05 Terminée — SF-05-01 (upload), SF-05-02 (liste), SF-05-03 (download) mergées | Product owner |
