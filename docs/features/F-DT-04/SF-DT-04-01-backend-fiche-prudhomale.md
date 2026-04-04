# Mini-spec — F-DT-04 / SF-DT-04-01 Backend fiche prud'homale

---

## Identifiant

`F-DT-04 / SF-DT-04-01`

## Feature parente

`F-DT-04` — Génération fiche prud'homale

## Statut

`ready`

## Date de création

2026-04-04

## Branche Git

`feat/SF-DT-04-01-backend-fiche-prudhomale`

---

## Objectif

Créer le modèle de données et l'API permettant de persister et de récupérer une fiche prud'homale associée à un dossier, avec pré-remplissage automatique depuis la dernière analyse disponible.

---

## Comportement attendu

### Cas nominal — GET (premier accès, aucune fiche sauvegardée)

1. L'avocat accède à la fiche d'un dossier pour la première fois.
2. `GET /api/v1/case-files/{id}/prudhome-fiche` → 200 avec un objet pré-rempli depuis la dernière analyse `DONE` :
   - `faitsTexte` : faits de la synthèse joints en texte libre
   - `moyensDroitTexte` : points juridiques joints en texte libre
   - `demandes` : liste pré-remplie avec les indemnités F-DT-01 si disponibles, sinon liste vide
   - `demandeur`, `defendeur` : objets vides (à remplir par l'avocat)
   - `piecesList` : liste des documents du dossier avec numérotation auto
3. L'objet retourné a `id = null` (pas encore persisté).

### Cas nominal — GET (fiche déjà sauvegardée)

1. `GET /api/v1/case-files/{id}/prudhome-fiche` → 200 avec les données persistées.

### Cas nominal — PUT (création / mise à jour)

1. L'avocat soumet le formulaire.
2. `PUT /api/v1/case-files/{id}/prudhome-fiche` → 200 avec la fiche persistée (upsert : une seule fiche par dossier).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Dossier introuvable ou hors workspace | 404 | 404 |
| Aucune analyse DONE disponible (pré-remplissage) | Objet pré-rempli avec champs vides — pas d'erreur | 200 |
| `demandeur.nom` vide au PUT | 400 | 400 |
| Montant demande < 0 | 400 | 400 |

---

## Critères d'acceptation

- [ ] `GET` retourne 200 avec pré-remplissage depuis la dernière analyse si aucune fiche persistée
- [ ] `GET` retourne 200 avec les données persistées si la fiche existe
- [ ] `PUT` crée la fiche si elle n'existe pas (upsert)
- [ ] `PUT` met à jour la fiche si elle existe déjà
- [ ] `demandeur.nom` est obligatoire au PUT (400 si absent)
- [ ] Isolation workspace : un utilisateur hors workspace → 404
- [ ] `piecesList` dans la réponse GET liste les documents du dossier avec leur numéro d'ordre
- [ ] Les indemnités F-DT-01 pré-remplissent `demandes` si `compensationEstimate` disponible dans la dernière analyse

---

## Périmètre

### Hors scope

- Export PDF/Word (SF-DT-04-03)
- Interface formulaire Angular (SF-DT-04-02)
- Versioning de la fiche (une seule version par dossier)
- Partage ou envoi de la fiche par email

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `id` | null (objet pré-rempli) | UUID généré à la première persistance |
| `demandeur` | `{}` (objet vide) | Toujours renseigné par l'avocat |
| `defendeur` | `{}` (objet vide) | Toujours renseigné par l'avocat |
| `demandes` | `[]` ou pré-rempli F-DT-01 | Calculé depuis compensationEstimate |
| `faitsTexte` | Faits de la synthèse joints | Pré-rempli, éditable |
| `moyensDroitTexte` | Points juridiques joints | Pré-rempli, éditable |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{caseFileId}/prudhome-fiche` | Oui | MEMBER |
| PUT | `/api/v1/case-files/{caseFileId}/prudhome-fiche` | Oui | LAWYER |

### Nouveaux composants

| Composant | Rôle |
|---|---|
| `PrudhomeFiche` | Entité JPA — `case_file_id`, `demandeur` (JSONB), `defendeur` (JSONB), `demandes` (JSONB), `faits_texte`, `moyens_droit_texte` |
| `PrudhomeFicheRepository` | JPA — `findByCaseFileId` |
| `PrudhomeFicheRequest` | DTO PUT — demandeur, défendeur, demandes, faitsTexte, moyensDroitTexte |
| `PrudhomeFicheResponse` | DTO GET — id (nullable), + tous les champs + piecesList |
| `PrudhomeFicheService` | `get()` (préfill ou persisté) + `upsert()` |
| `PrudhomeFicheController` | GET + PUT |

### Modèle JSON `demandeur` / `defendeur`

```json
// demandeur
{
  "nom": "Jean Dupont",
  "prenom": "Jean",
  "adresse": "12 rue de la Paix, 75001 Paris",
  "telephone": "0612345678",
  "email": "jean.dupont@email.fr",
  "profession": "Technicien"
}

// defendeur
{
  "nom": "SA Renault",
  "adresse": "13 quai Le Gallo, 92100 Boulogne-Billancourt",
  "siret": "78000000000000",
  "representant": "Jean Martin, DRH"
}
```

### Modèle JSON `demandes`

```json
[
  {"label": "Indemnité légale de licenciement", "montant": 8050.0},
  {"label": "Dommages-intérêts (barème Macron)", "montant": null},
  {"label": "Indemnité compensatrice de préavis", "montant": null}
]
```

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `prudhome_fiches` | CREATE + INSERT + UPDATE | Nouvelle table, 1 ligne max par `case_file_id` |

### Migration Liquibase

- [x] Oui — `045-create-prudhome-fiches.xml`

---

## Plan de test

### Tests unitaires

- [ ] `PrudhomeFicheService.get()` — aucune fiche persistée → objet pré-rempli avec faits de la synthèse
- [ ] `PrudhomeFicheService.get()` — fiche existante → données persistées retournées
- [ ] `PrudhomeFicheService.get()` — aucune analyse disponible → objet vide sans erreur
- [ ] `PrudhomeFicheService.get()` — compensationEstimate disponible → demandes pré-remplies
- [ ] `PrudhomeFicheService.upsert()` — création si inexistante
- [ ] `PrudhomeFicheService.upsert()` — mise à jour si existante

### Tests d'intégration

- [ ] `GET /api/v1/case-files/{id}/prudhome-fiche` → 200 (dossier valide, membre du workspace)
- [ ] `GET /api/v1/case-files/{id}/prudhome-fiche` → 404 (dossier hors workspace)
- [ ] `PUT /api/v1/case-files/{id}/prudhome-fiche` → 200 avec payload valide
- [ ] `PUT /api/v1/case-files/{id}/prudhome-fiche` → 400 si `demandeur.nom` absent
- [ ] `PUT /api/v1/case-files/{id}/prudhome-fiche` → 404 (dossier hors workspace)

### Isolation workspace

- [ ] Applicable — `GET` et `PUT` vérifient que le dossier appartient au workspace de l'utilisateur connecté

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — nouveaux endpoints sur un nouveau sous-resource, aucune modification auth/routing/workspace/plans.

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné

---

## Dépendances

### Subfeatures bloquantes

- SF-DT-04-02 (frontend) — dépend de cette subfeature
- SF-DT-04-03 (export) — dépend de cette subfeature
- F-DT-01 (compensationEstimate) — statut : **done** (utilisé pour pré-remplir les demandes)

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Upsert sur `case_file_id` (contrainte UNIQUE) : un seul enregistrement par dossier, PUT crée ou met à jour.
- `demandeur` et `defendeur` stockés en JSONB — structure souple pour V1, évite des colonnes nullables nombreuses.
- `piecesList` dans la réponse GET est calculée à la volée depuis `DocumentRepository` — non persistée.
- Pré-remplissage `faitsTexte` : `analysis.faits.map(f → f.texte).join('\n')` (max 5000 chars).
- Pré-remplissage `moyensDroitTexte` : `analysis.pointsJuridiques.map(p → p.texte).join('\n')`.
- `compensationEstimate` null → `demandes = []`; non null → 2 demandes pré-remplies : indemnité légale (avec montant) + D&I barème Macron (montant null, à saisir).
