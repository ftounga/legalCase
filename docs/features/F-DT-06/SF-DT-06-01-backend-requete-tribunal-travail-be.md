# Mini-spec — F-DT-06 / SF-DT-06-01 Backend CRUD requête tribunal du travail belge

---

## Identifiant

`F-DT-06 / SF-DT-06-01`

## Feature parente

`F-DT-06` — Requête contradictoire tribunal du travail belge

## Statut

`draft`

## Date de création

2026-04-06

## Branche Git

`feat/SF-DT-06-01-requete-tribunal-be`

---

## Objectif

Créer la table, l'entity, le service et les endpoints CRUD pour la requête contradictoire devant le tribunal du travail belge (équivalent de la fiche prud'homale F-DT-04).

---

## Comportement attendu

### Cas nominal

1. `GET /api/v1/case-files/{id}/tribunal-travail-fiche` retourne la fiche si elle existe (ou 404 avec structure vide créée)
2. `PUT /api/v1/case-files/{id}/tribunal-travail-fiche` crée ou met à jour la fiche (upsert)
3. La fiche est liée à un dossier (1 fiche max par dossier)
4. Les champs sont stockés en JSON (même pattern que PrudhomeFiche)
5. L'API auto-remplit certains champs depuis la synthèse IA si disponible (ancienneté, salaire, type rupture)

### Champs

**Requérant** : nom, prénom, domicile, registreNational
**Défendeur** : nom, siegeSocial, numeroBce, representant
**Procédure** : tribunal (arrondissement + division), langue (FR/NL/DE), commissionParitaire
**Contrat** : typeContrat (EMPLOYE/OUVRIER), dateDebut, dateFin, motifRupture
**Demande** : demandes (liste label+montant), exposeDesMoyens
**Pièces** : inventaire auto-généré depuis les documents du dossier

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Dossier inexistant | Not found | 404 |
| Dossier d'un autre workspace | Accès refusé | 403 |
| Dossier non BELGIQUE ou non DROIT_DU_TRAVAIL | Fiche non applicable | 400 |

---

## Critères d'acceptation

- [ ] Migration 055 crée la table `tribunal_travail_fiches`
- [ ] Entity `TribunalTravailFiche` suit le même pattern que `PrudhomeFiche`
- [ ] `GET /api/v1/case-files/{id}/tribunal-travail-fiche` → 200 avec fiche
- [ ] `PUT /api/v1/case-files/{id}/tribunal-travail-fiche` → 200 upsert
- [ ] Isolation workspace vérifiée
- [ ] La fiche n'est disponible que pour les dossiers DROIT_DU_TRAVAIL + country BELGIQUE
- [ ] Les données sont configurées pour FRANCE (prud'homale) et BELGIQUE (tribunal travail)
- [ ] Tests d'intégration sur les endpoints
- [ ] Tous les tests existants restent verts

---

## Périmètre

### Hors scope

- Formulaire Angular (SF-DT-06-02)
- Export PDF (SF-DT-06-03)

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle |
|---------|-----|------|------|
| GET | `/api/v1/case-files/{id}/tribunal-travail-fiche` | Oui | MEMBER |
| PUT | `/api/v1/case-files/{id}/tribunal-travail-fiche` | Oui | MEMBER |

### Tables

| Table | Opération |
|-------|-----------|
| `tribunal_travail_fiches` | CREATE (DDL) + SELECT + INSERT + UPDATE |

### Migration Liquibase

- [x] Oui — `055-create-tribunal-travail-fiches.xml`

---

## Plan de test

### Tests d'intégration

- [ ] GET fiche → 200
- [ ] PUT upsert → 200
- [ ] GET fiche autre workspace → 404
- [ ] PUT dossier non belge → 400

### Isolation workspace

- [x] Applicable

---

## Analyse d'impact

- [x] **Aucune préoccupation transversale** — nouveau CRUD isolé
