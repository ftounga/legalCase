# Mini-spec — F-110 / SF-110-01 Migration référentiels métier Java → DB

## Identifiant
`F-110 / SF-110-01`

## Feature parente
`F-110` — Guides & barèmes métier par domaine

## Statut
`draft`

## Date de création
2026-04-04

## Branche Git
`feat/SF-110-01-migration-referentiels-db`

---

## Objectif

Migrer les 6 référentiels métier hardcodés en Java vers une table `legal_referentials` en base de données, et enrichir le référentiel immigration avec la procédure `REGULARISATION_EXCEPTIONNELLE` (FR + BE).

---

## Comportement attendu

### Cas nominal

Les données actuellement hardcodées dans les classes Java suivantes sont insérées en base via Liquibase (données initiales) :

| Classe Java | Type de données |
|-------------|----------------|
| `LitigationTypeMapper` | 7 types de litiges + délais + articles |
| `CompensationCalculator` | Barème Macron (plafonds min/max) |
| `ImmigrationPieceReferentiel` | 4 procédures × 2 pays → listes de pièces |
| `ImmigrationProcedureReferentiel` | 3 procédures → jalons (label + offset jours) |
| `PensionAlimentaireCalculator` | Tables UNAF (FR) + CGKR (BE) par nb enfants × garde |
| `PrestationCompensatoireCalculator` | Coefficients FR/BE + durée référence |

La procédure `REGULARISATION_EXCEPTIONNELLE` est ajoutée pour FR et BE :
- **Pièces FR** : Passeport, justificatif de résidence 5 ans, contrat de travail ou promesse d'embauche, avis d'imposition 3 ans, justificatif d'hébergement, casier judiciaire, photos d'identité, formulaire CERFA
- **Pièces BE** : Passeport, preuve de séjour ininterrompu 5 ans, justificatif d'intégration sociale, ressources stables, casier judiciaire belge et pays d'origine, assurance maladie, photos
- **Jalons FR** : Instruction préfecture (180j), silence vaut rejet (120j)
- **Jalons BE** : Instruction Office des étrangers (150j), recours CCAT (90j)

Les services existants (`StatutoryDeadlineService`, `PensionAlimentaireCalculator`, etc.) sont mis à jour pour lire depuis la DB. Les classes Java hardcodées sont conservées comme fallback fail-open si la DB ne retourne aucune donnée.

3 endpoints GET sont exposés pour que le frontend puisse afficher les référentiels (SF-110-02) :

- `GET /api/v1/referentials?domain=DROIT_DU_TRAVAIL`
- `GET /api/v1/referentials?domain=DROIT_IMMIGRATION`
- `GET /api/v1/referentials?domain=DROIT_FAMILLE`

Chaque réponse retourne les entrées groupées par `referential_type` pour le domaine demandé + le workspace courant (données système + données custom du workspace).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `domain` absent ou invalide | Message d'erreur explicite | 400 |
| Aucune entrée en base pour le domaine | Fallback Java, réponse 200 | 200 |
| Utilisateur non authentifié | 401 | 401 |

---

## Critères d'acceptation

- [ ] Table `legal_referentials` créée avec migration Liquibase
- [ ] Toutes les données des 6 classes Java insérées via `changeSet` Liquibase
- [ ] `REGULARISATION_EXCEPTIONNELLE` présente pour FR et BE (pièces + jalons)
- [ ] `StatutoryDeadlineService` lit les jalons immigration depuis la DB
- [ ] `PensionAlimentaireCalculator` lit les taux depuis la DB avec fallback Java
- [ ] `PrestationCompensatoireCalculator` lit les coefficients depuis la DB avec fallback Java
- [ ] `LitigationTypeMapper` lit les types de litiges depuis la DB avec fallback Java
- [ ] `GET /api/v1/referentials?domain=X` retourne 200 avec les données groupées
- [ ] Isolation workspace : un workspace voit ses données custom + les données système
- [ ] Tests verts

---

## Périmètre

### Hors scope

- Écran frontend (SF-110-02)
- Modification des barèmes par l'OWNER (SF-110-03)
- Cron IA automatique (SF-110-04)
- Bouton signalement anomalie (SF-110-05)
- Suppression des classes Java hardcodées (conservées comme fallback)

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `workspace_id` | NULL | Données système visibles par tous les workspaces |
| `is_system` | true | Toutes les données migrées depuis Java sont système |
| `is_active` | true | Toutes les entrées actives par défaut |
| `updated_at` | NOW() | Renseigné automatiquement |
| `updated_by` | NULL | Données système sans auteur utilisateur |

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs autorisées |
|-------|-------------|----------------------------|
| `legal_domain` | Oui | `DROIT_DU_TRAVAIL`, `DROIT_IMMIGRATION`, `DROIT_FAMILLE` |
| `referential_type` | Oui | `LITIGATION_TYPE`, `BAREME_MACRON`, `IMMIGRATION_PIECES`, `IMMIGRATION_JALONS`, `PENSION_TAUX`, `PRESTATION_COEFF` |
| `entry_key` | Oui | Non vide |
| `value_json` | Oui | JSON valide |
| `country` | Non | `FRANCE`, `BELGIQUE`, ou NULL si universel |

---

## Technique

### Table `legal_referentials`

```sql
CREATE TABLE legal_referentials (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id     UUID         REFERENCES workspaces(id) ON DELETE CASCADE,
    -- NULL = donnée système visible par tous les workspaces
    legal_domain     VARCHAR(50)  NOT NULL,
    -- DROIT_DU_TRAVAIL | DROIT_IMMIGRATION | DROIT_FAMILLE
    referential_type VARCHAR(100) NOT NULL,
    -- LITIGATION_TYPE | BAREME_MACRON | IMMIGRATION_PIECES
    -- | IMMIGRATION_JALONS | PENSION_TAUX | PRESTATION_COEFF
    entry_key        VARCHAR(200) NOT NULL,
    -- ex: LICENCIEMENT_SANS_CAUSE_REELLE, VISA_ETUDIANT_FRANCE
    value_json       TEXT         NOT NULL,
    -- JSON structuré selon referential_type
    label            VARCHAR(500),
    country          VARCHAR(20),
    -- FRANCE | BELGIQUE | NULL si universel
    is_system        BOOLEAN      NOT NULL DEFAULT true,
    -- false = ajouté par le workspace
    is_active        BOOLEAN      NOT NULL DEFAULT true,
    source_ref       VARCHAR(200),
    -- ex: Art. L1471-1, Barème UNAF 2025
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_by       UUID         REFERENCES users(id)
);

CREATE INDEX idx_legal_referentials_domain
    ON legal_referentials(legal_domain, referential_type);
CREATE INDEX idx_legal_referentials_workspace
    ON legal_referentials(workspace_id);
```

### Endpoints

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/referentials` | Oui | MEMBER |

Paramètre query : `domain` (obligatoire).

Réponse — exemple DROIT_DU_TRAVAIL :
```json
{
  "domain": "DROIT_DU_TRAVAIL",
  "sections": {
    "LITIGATION_TYPE": [
      { "key": "LICENCIEMENT_SANS_CAUSE_REELLE", "label": "Licenciement sans cause réelle et sérieuse", "valueJson": "{\"years\":1,\"article\":\"Art. L1471-1\"}", "isSystem": true }
    ],
    "BAREME_MACRON": [
      { "key": "0_1_AN", "label": "0 à 1 an d'ancienneté", "valueJson": "{\"plafondMinMois\":0,\"plafondMaxMois\":1}", "isSystem": true }
    ]
  }
}
```

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `legal_referentials` | CREATE + INSERT | Nouvelle table + données initiales |

### Migrations Liquibase

- [x] `V047__create_legal_referentials.sql` — table + index
- [x] `V048__insert_legal_referentials_data.sql` — données initiales (6 sources + REGULARISATION_EXCEPTIONNELLE)

### Nouveaux composants backend

- `LegalReferential` — entité JPA
- `LegalReferentialRepository` — JPA repository
- `LegalReferentialService` — lecture DB + fallback Java
- `ReferentialController` — GET /api/v1/referentials
- `ReferentialResponse` — DTO groupé par type

---

## Plan de test

### Tests unitaires

- [ ] `LegalReferentialService` — retourne données DB si présentes
- [ ] `LegalReferentialService` — fallback Java si DB vide (fail-open)
- [ ] `LegalReferentialService` — fusion données système + custom workspace
- [ ] `PensionAlimentaireCalculator` — utilise les taux DB
- [ ] `PrestationCompensatoireCalculator` — utilise les coefficients DB
- [ ] `LitigationTypeMapper` — utilise les types DB

### Tests d'intégration

- [ ] `GET /api/v1/referentials?domain=DROIT_DU_TRAVAIL` → 200 avec 7 types litiges
- [ ] `GET /api/v1/referentials?domain=DROIT_IMMIGRATION` → 200 contient `REGULARISATION_EXCEPTIONNELLE` FR + BE
- [ ] `GET /api/v1/referentials?domain=DROIT_FAMILLE` → 200 avec taux pension + coefficients prestation
- [ ] `GET /api/v1/referentials` sans domain → 400
- [ ] `GET /api/v1/referentials?domain=INVALIDE` → 400
- [ ] `GET /api/v1/referentials?domain=DROIT_DU_TRAVAIL` non authentifié → 401

### Isolation workspace

- [ ] Applicable — workspace A ne voit pas les données custom du workspace B
- [ ] Les données système (`workspace_id` NULL) sont visibles par tous

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [x] **Workspace context** — résolution workspace pour filtrer les données custom
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [ ] Aucune préoccupation transversale

### Composants existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `StatutoryDeadlineService` | Lit les jalons depuis DB au lieu de Java | IT existants maintenus + fallback testé |
| `PensionAlimentaireCalculator` | Lit les taux depuis DB | Tests unitaires calculateur |
| `PrestationCompensatoireCalculator` | Lit les coefficients depuis DB | Tests unitaires calculateur |
| `LitigationTypeMapper` | Lit les types depuis DB | Tests unitaires mapper |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — pas de modification de navigation ou d'auth

---

## Dépendances

### Subfeatures bloquantes
- Aucune — SF-110-01 est le prérequis de toutes les autres SF de F-110

### Questions ouvertes
- Aucune question ouverte bloquante

---

## Notes et décisions

- **Fallback Java conservé** : si la table est vide (ex. première mise en production avant migration), les calculateurs retombent sur les constantes Java — aucune régression possible
- **`workspace_id NULL`** = donnée système visible par tous les workspaces, sans duplication
- Les classes Java hardcodées ne sont pas supprimées dans cette SF — elles seront dépréciées dans une SF ultérieure une fois la stabilité DB confirmée en production
- Le numéro de migration Liquibase V047/V048 est à confirmer par rapport au dernier numéro utilisé avant dev
