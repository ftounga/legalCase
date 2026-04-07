# Mini-spec — F-IM-05 / SF-IM-05-01 Référentiel des titres de séjour et arbre de décision (backend)

---

## Identifiant

`F-IM-05 / SF-IM-05-01`

## Feature parente

`F-IM-05` — Arbre décisionnel type de titre

## Statut

`draft`

## Date de création

2026-04-07

## Branche Git

`feat/SF-IM-05-01-referentiel-titres`

---

## Objectif

Créer le référentiel statique des titres de séjour (France + Belgique) et l'arbre de décision qui, à partir de critères (nationalité, motif, durée, situation familiale), recommande le titre adapté. Persister le résultat par dossier.

---

## Comportement attendu

### Cas nominal

1. Le référentiel contient la liste exhaustive des titres de séjour pour la France et la Belgique, chacun associé à :
   - un code unique (ex: `VLS_TS_ETUDIANT`, `CARTE_B_TRAVAIL`)
   - un libellé
   - un pays (`FRANCE` / `BELGIQUE`)
   - une catégorie de motif (`TRAVAIL`, `ETUDES`, `FAMILLE`, `ASILE`, `AUTRE`)
   - les conditions principales (texte structuré)
   - les pièces typiques à fournir (liste de labels)
   - le délai moyen de traitement (en jours, indicatif)

2. L'arbre de décision reçoit un ensemble de critères :
   - `country` : `FRANCE` ou `BELGIQUE`
   - `nationaliteUE` : `true` / `false`
   - `motif` : `TRAVAIL` / `ETUDES` / `FAMILLE` / `ASILE` / `AUTRE`
   - `duree` : `COURT_SEJOUR` (< 1 an) / `LONG_SEJOUR` (>= 1 an)
   - `situationFamiliale` : `CELIBATAIRE` / `MARIE` / `PACS_COHABITATION` (optionnel, utilisé pour FAMILLE)

3. L'arbre retourne une liste ordonnée de 1 à 3 titres recommandés (le plus probable en premier), chacun avec ses conditions et pièces.

4. Le résultat est persisté en table `immigration_title_decisions` (1:1 avec le dossier) pour être récupéré ultérieurement.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Pays non supporté (ni FRANCE ni BELGIQUE) | Message d'erreur explicite | 400 |
| Motif inconnu | Message d'erreur explicite | 400 |
| Combinaison de critères sans titre applicable | Retourne liste vide + message "Aucun titre standard identifié" | 200 |
| Dossier inexistant ou workspace différent | Accès refusé | 404 |
| Domaine juridique != DROIT_IMMIGRATION | Message d'erreur | 400 |

---

## Critères d'acceptation

- [ ] Le référentiel couvre au minimum 8 titres France (VLS-TS étudiant, VLS-TS salarié, carte séjour temporaire salarié, carte pluriannuelle, carte résident, APS, carte séjour vie privée et familiale, récépissé) et 8 titres Belgique (carte A travail, carte A études, carte A famille, carte B, carte C, permis unique, annexe 15, attestation d'immatriculation)
- [ ] Les données sont configurées pour FRANCE et BELGIQUE
- [ ] L'arbre de décision retourne le bon titre pour chaque combinaison pays × nationalité × motif × durée testée
- [ ] Les résultats sont persistés en DB avec FK vers case_file et contrainte unique
- [ ] L'isolation workspace est respectée : un utilisateur du workspace A ne peut pas accéder aux décisions du workspace B
- [ ] Le domaine juridique du dossier est vérifié (DROIT_IMMIGRATION uniquement)
- [ ] Le service suit le pattern existant (`ImmigrationPieceReferentiel` / `PrudhomeFiche`)

---

## Périmètre

### Hors scope (explicite)

- Endpoint REST (SF-IM-05-02)
- Interface frontend (SF-IM-05-03)
- Export PDF de la fiche récapitulative
- Intégration avec le pipeline IA (le questionnaire est rempli manuellement par l'avocat)
- Modification de la checklist F-IM-01 en fonction du résultat

---

## Technique

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `immigration_title_decisions` | CREATE | Nouvelle table |
| `case_files` | SELECT | Lecture pour vérification domaine + workspace |

### Migration Liquibase

- [x] Oui — `V058__create-immigration-title-decisions.xml`

**Structure de la table `immigration_title_decisions` :**

| Colonne | Type | Contrainte |
|---------|------|-----------|
| `id` | UUID | PK |
| `case_file_id` | UUID FK | UNIQUE, NOT NULL |
| `country` | VARCHAR(20) | NOT NULL |
| `nationalite_ue` | BOOLEAN | NOT NULL |
| `motif` | VARCHAR(30) | NOT NULL |
| `duree` | VARCHAR(20) | NOT NULL |
| `situation_familiale` | VARCHAR(30) | nullable |
| `recommended_titles` | TEXT | NOT NULL — JSON array des titres recommandés |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

### Classes Java à créer

| Classe | Rôle |
|--------|------|
| `ImmigrationTitleReferentiel` | Référentiel statique : titres FR + BE avec conditions, pièces, délais |
| `ImmigrationTitleDecisionEngine` | Arbre de décision : critères → liste de titres recommandés |
| `ImmigrationTitleDecision` | Entity JPA (table `immigration_title_decisions`) |
| `ImmigrationTitleDecisionRepository` | Repository JPA |
| `TitleRecommendation` | Record : code, label, country, conditions, pièces, délai |

### Composants Angular (si applicable)

- Non applicable (SF-IM-05-01 = backend uniquement)

---

## Plan de test

### Tests unitaires

- [ ] `ImmigrationTitleReferentiel` — couvre au moins 8 titres FR et 8 titres BE
- [ ] `ImmigrationTitleReferentiel` — `getTitles(country)` retourne uniquement les titres du pays demandé
- [ ] `ImmigrationTitleDecisionEngine` — nationalité UE + travail + France → carte séjour UE
- [ ] `ImmigrationTitleDecisionEngine` — hors UE + études + France → VLS-TS étudiant
- [ ] `ImmigrationTitleDecisionEngine` — hors UE + travail + Belgique → permis unique
- [ ] `ImmigrationTitleDecisionEngine` — hors UE + famille + Belgique → carte A regroupement familial
- [ ] `ImmigrationTitleDecisionEngine` — hors UE + asile + France → récépissé demande d'asile
- [ ] `ImmigrationTitleDecisionEngine` — combinaison sans titre → liste vide
- [ ] `ImmigrationTitleDecisionEngine` — UE + court séjour → pas de titre nécessaire (libre circulation)

### Tests d'intégration

- [ ] Persistance d'une décision → relecture OK avec mêmes données
- [ ] Upsert : deuxième décision sur le même dossier remplace la première (contrainte unique)
- [ ] Dossier avec domaine != DROIT_IMMIGRATION → rejet 400
- [ ] Dossier d'un autre workspace → rejet 404

### Isolation workspace

- [x] Applicable — test : un utilisateur du workspace A ne peut pas accéder aux décisions du workspace B

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] **Auth / Principal** — pas de modification
- [ ] **Workspace context** — lecture seule, pattern existant réutilisé
- [ ] **Plans / limites** — pas impacté
- [ ] **Navigation / routing frontend** — pas impacté (backend only)
- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité à son périmètre

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné (justification : nouvelle feature backend, aucun composant existant modifié)

---

## Dépendances

### Subfeatures bloquantes

- Aucune — SF-IM-05-01 est la première du découpage

### Questions ouvertes impactées

- [ ] Aucune question ouverte de `docs/OPEN_QUESTIONS.md` n'est impactée

---

## Notes et décisions

- Le référentiel est statique en Java (comme `ImmigrationPieceReferentiel`), pas en base. Permet d'itérer rapidement sans migration à chaque ajout de titre.
- Le champ `recommended_titles` est stocké en JSON TEXT (comme `PrudhomeFiche.demandeur`), parsé côté service. Évite une table de jointure complexe.
- L'arbre de décision est un service pur (pas d'accès DB) — facilite les tests unitaires.
- La relation avec `case_files` est 1:1 (contrainte UNIQUE sur `case_file_id`), même pattern que `PrudhomeFiche`.
