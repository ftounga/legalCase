# Mini-spec — F-IM-01 / SF-IM-01-01 Backend checklist pièces par type de titre

## Identifiant

`F-IM-01 / SF-IM-01-01`

## Feature parente

`F-IM-01` — Checklist pièces par type de titre de séjour

## Statut

`in-progress`

## Date de création

2026-04-04

## Branche Git

`feat/SF-IM-01-01-backend-checklist-pieces`

---

## Objectif

Créer le référentiel statique des pièces requises par type de titre de séjour et par pays, persister l'état de chaque pièce (PRESENT / ABSENT / INCONNU) par dossier dans une table dédiée, et exposer deux endpoints GET + PUT pour consulter et mettre à jour la checklist d'un dossier immigration.

---

## Comportement attendu

### Cas nominal

**GET** `/api/v1/case-files/{caseFileId}/immigration-checklist?titreType=VISA_ETUDIANT&country=FRANCE`

1. Vérification que le dossier appartient au workspace de l'utilisateur connecté.
2. Vérification que `legalDomain = DROIT_IMMIGRATION`.
3. `ImmigrationPieceReferentiel.getPieces(titreType, country)` retourne la liste des pièces attendues pour ce type de titre.
4. Pour chaque pièce du référentiel, chercher un enregistrement existant dans `immigration_piece_checks`.
5. Retourner la liste fusionnée : pièces connues avec leur statut, pièces nouvelles avec statut `INCONNU`.

**PUT** `/api/v1/case-files/{caseFileId}/immigration-checklist`

Body : `{ "titreType": "VISA_ETUDIANT", "country": "FRANCE", "pieces": [{"label": "Passeport en cours de validité", "statut": "PRESENT"}, ...] }`

1. Vérification workspace + legalDomain.
2. Pour chaque pièce : upsert dans `immigration_piece_checks` (insert si nouvelle, update si existante).
3. Retourner la checklist mise à jour.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Dossier inexistant | Not found | 404 |
| Dossier d'un autre workspace | Accès refusé | 403 |
| `legalDomain ≠ DROIT_IMMIGRATION` | Bad request | 400 |
| `titreType` inconnu du référentiel | Bad request — liste des valeurs valides | 400 |
| `country` non supportée | Bad request | 400 |
| `statut` invalide (hors PRESENT/ABSENT/INCONNU) | Bad request | 400 |

---

## Critères d'acceptation

- [ ] `GET` retourne les pièces du référentiel pour `VISA_ETUDIANT` + `FRANCE` avec statut `INCONNU` si aucun enregistrement
- [ ] `GET` retourne les statuts persistés quand ils existent
- [ ] `PUT` persiste les statuts et retourne la checklist à jour
- [ ] `PUT` est idempotent : double appel avec les mêmes données → même résultat
- [ ] 403 si dossier d'un autre workspace
- [ ] 400 si `legalDomain ≠ DROIT_IMMIGRATION`
- [ ] 400 si `titreType` inconnu
- [ ] Référentiel couvre 4 types × 2 pays = 8 combinaisons
- [ ] Pièces spécifiques à la Belgique différentes des pièces France

---

## Périmètre

### Hors scope

- Export PDF (SF-IM-01-03)
- Composant Angular (SF-IM-01-02)
- Détection automatique du type de titre par l'IA (non prévu)
- Ajout de pièces personnalisées hors référentiel
- Suppression d'enregistrements (le PUT remplace)

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| statut | INCONNU | Valeur par défaut si jamais renseigné |
| created_at | now() | @PrePersist |
| updated_at | now() | @PrePersist + @PreUpdate |

---

## Contraintes de validation

| Champ | Obligatoire | Valeurs autorisées |
|-------|-------------|-------------------|
| titreType | Oui | VISA_ETUDIANT, TITRE_SALARIE, REGROUPEMENT_FAMILIAL, NATURALISATION |
| country | Oui | FRANCE, BELGIQUE |
| statut | Oui | PRESENT, ABSENT, INCONNU |
| label | Oui | Doit correspondre à une pièce du référentiel pour le titreType + country |

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{caseFileId}/immigration-checklist` | Oui | MEMBER |
| PUT | `/api/v1/case-files/{caseFileId}/immigration-checklist` | Oui | LAWYER |

Paramètres GET : `titreType` (query param, obligatoire), `country` (query param, obligatoire)

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| immigration_piece_checks | INSERT / SELECT / UPDATE | Nouvelle table — migration 047 |
| case_files | SELECT | Vérification legalDomain + workspace |

### Migration Liquibase

`047-create-immigration-piece-checks.xml`

```
immigration_piece_checks
  id UUID PK
  case_file_id UUID FK → case_files (cascade delete)
  titre_type VARCHAR(50) NOT NULL
  country VARCHAR(20) NOT NULL
  label VARCHAR(255) NOT NULL
  statut VARCHAR(20) NOT NULL DEFAULT 'INCONNU'
  created_at TIMESTAMPTZ NOT NULL
  updated_at TIMESTAMPTZ NOT NULL
  UNIQUE(case_file_id, titre_type, country, label)
```

### Nouveau composant Java

- `ImmigrationPieceReferentiel` — map statique : `(titreType, country) → List<String> labels`
- `ImmigrationPieceCheck` — entité JPA
- `ImmigrationPieceCheckRepository` — `findByCaseFileIdAndTitreTypeAndCountry()`
- `ImmigrationChecklistService` — `get()`, `upsert()`
- `ImmigrationChecklistController` — GET + PUT
- `ImmigrationChecklistResponse` — DTO réponse : `{ titreType, country, pieces: [{label, statut}] }`
- `ImmigrationChecklistRequest` — DTO requête PUT

---

## Plan de test

### Tests unitaires

- [ ] `ImmigrationPieceReferentielTest` — `VISA_ETUDIANT` + `FRANCE` → N pièces avec labels corrects
- [ ] `ImmigrationPieceReferentielTest` — `VISA_ETUDIANT` + `BELGIQUE` → liste différente de France
- [ ] `ImmigrationPieceReferentielTest` — tous les types × 2 pays → non vide
- [ ] `ImmigrationChecklistServiceTest` — GET dossier sans historique → toutes pièces INCONNU
- [ ] `ImmigrationChecklistServiceTest` — GET dossier avec historique partiel → statuts fusionnés
- [ ] `ImmigrationChecklistServiceTest` — PUT → statuts persistés, retour correct
- [ ] `ImmigrationChecklistServiceTest` — 404 dossier inexistant
- [ ] `ImmigrationChecklistServiceTest` — 403 workspace différent
- [ ] `ImmigrationChecklistServiceTest` — 400 legalDomain ≠ DROIT_IMMIGRATION

### Tests d'intégration

- [ ] `GET /immigration-checklist` → 200 avec pièces INCONNU (dossier vierge)
- [ ] `PUT` → 200, puis `GET` → statuts mis à jour
- [ ] `PUT` idempotent → double appel → même résultat
- [ ] `GET` dossier autre workspace → 403
- [ ] `GET` dossier non immigration → 400

### Isolation workspace

- [ ] Applicable — un utilisateur workspace A ne peut pas accéder à la checklist d'un dossier workspace B

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Workspace context** — accès aux données filtré par workspace via caseFileId

### Composants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `CaseFileService` | Utilisé pour résoudre le dossier + vérifier workspace | Test IT avec workspace différent |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — nouvel endpoint sans impact sur navigation/routing

---

## Dépendances

### Subfeatures bloquantes

Aucune — première subfeature de F-IM-01.

### Questions ouvertes

Aucune.

---

## Notes et décisions

- Référentiel statique Java (map), pas de table de configuration — même pattern que `ImmigrationProcedureReferentiel`.
- La contrainte UNIQUE `(case_file_id, titre_type, country, label)` garantit l'idempotence du PUT.
- Le `titreType` et `country` sont passés en query params au GET pour permettre l'affichage du référentiel sans historique.
- Le PUT reçoit la liste complète des pièces du type sélectionné — pas de PATCH partiel.
