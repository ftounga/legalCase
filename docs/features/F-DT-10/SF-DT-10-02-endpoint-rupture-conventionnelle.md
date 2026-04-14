# Mini-spec — F-DT-10 / SF-DT-10-02 Endpoint POST/GET rupture conventionnelle

## Identifiant

`F-DT-10 / SF-DT-10-02`

## Feature parente

`F-DT-10` — Analyse de validité de la rupture conventionnelle

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-DT-10-02-endpoint-rupture-conventionnelle`

---

## Objectif

Exposer les endpoints REST qui permettent au frontend d'enregistrer l'analyse de validité d'une rupture conventionnelle pour un dossier donné (POST) et de récupérer l'analyse existante (GET). Respect de l'isolation workspace et rejet des dossiers dont le domaine n'est pas le droit du travail. Miroir fidèle de l'architecture SF-DT-08-02.

---

## Comportement attendu

### Endpoints

| Méthode | URL | Description |
|---------|-----|-------------|
| POST | `/api/v1/case-files/{caseFileId}/rupture-conv` | Enregistre ou met à jour l'analyse (upsert) |
| GET | `/api/v1/case-files/{caseFileId}/rupture-conv` | Lit l'analyse existante |

### POST — body

```json
{
  "country": "FRANCE",
  "reponses": {
    "RC_CONSENTEMENT": "OUI",
    "RC_DELAI_RETRACTATION": "OUI",
    "RC_HOMOLOGATION": "INCONNU",
    "RC_ASSISTANCE": "OUI",
    "RC_INDEMNITE": "OUI",
    "RC_ENTRETIENS": "OUI"
  }
}
```

- `country` obligatoire. Seule valeur acceptée : `FRANCE` (rupture conventionnelle non applicable à la Belgique). `BELGIQUE` ou autre → 400 Bad Request.
- `reponses` optionnelle (map vide acceptée). Les clés non-reconnues sont ignorées silencieusement. Les valeurs non-reconnues sont normalisées en `INCONNU` (cf. SF-DT-10-01).

### Réponse (POST et GET)

```json
{
  "caseFileId": "…",
  "country": "FRANCE",
  "scoreRisque": 10,
  "verdict": "VALIDE",
  "criteres": [
    {"code": "RC_CONSENTEMENT", "label": "…", "reponse": "OUI",
     "pointsRisque": 0, "bloquant": true, "commentaire": "Conforme — …"}
  ]
}
```

### Règles métier

1. **Upsert strict 1:1** : si une analyse existe déjà pour le `caseFileId`, elle est mise à jour (même entité, `updated_at` rafraîchi). Sinon création.
2. **Isolation workspace** : le user courant doit être membre primaire du workspace du dossier. Sinon 404 (pas 403, pour éviter la révélation d'existence).
3. **Domaine requis** : `case_file.legal_domain == DROIT_DU_TRAVAIL`. Sinon 400 "Ce dossier n'est pas un dossier de droit du travail".
4. **Case file supprimé** : `deleted_at IS NOT NULL` → 404.
5. **GET sans analyse existante** : 404 "Aucune analyse de rupture conventionnelle trouvée pour ce dossier".
6. **Analyzer invoqué à chaque POST** : réponses persistées + résultat re-calculé et persisté (cohérence stricte entre `reponses_data` et `result_data`).
7. **Authentification OIDC** : `@AuthenticationPrincipal OidcUser` + `Principal` pour résoudre `provider` et `User` (pattern identique à `LicenciementService`).

### Cas d'erreur

| Situation | Code HTTP | Message |
|-----------|-----------|---------|
| `country` null ou absent | 400 | "Pays non supporté : null" |
| `country` = "BELGIQUE" | 400 | "Pays non supporté : BELGIQUE" |
| `caseFileId` inconnu ou soft-deleted | 404 | "Case file not found" |
| User non membre du workspace | 404 | "Case file not found" |
| Domaine ≠ DROIT_DU_TRAVAIL | 400 | "Ce dossier n'est pas un dossier de droit du travail" |
| GET sans analyse existante | 404 | "Aucune analyse de rupture conventionnelle trouvée pour ce dossier" |
| JSON malformé dans `reponses_data` stocké (corruption) | 500 | "Erreur de désérialisation" |

---

## Critères d'acceptation

- [ ] `RuptureConvRequest(String country, Map<String, String> reponses)` record.
- [ ] `RuptureConvResponse(UUID caseFileId, String country, int scoreRisque, String verdict, List<CritereData> criteres)` record, avec `CritereData(code, label, reponse, pointsRisque, bloquant, commentaire)`.
- [ ] `RuptureConvService` avec `analyze(caseFileId, request, oidcUser, principal)` et `get(caseFileId, oidcUser, principal)`.
- [ ] `RuptureConvController` avec POST et GET mappés sur `/api/v1/case-files/{caseFileId}/rupture-conv`.
- [ ] Validation pays (FRANCE uniquement) rejettée proprement en 400.
- [ ] Isolation workspace testée (autre user → 404).
- [ ] Domaine ≠ DROIT_DU_TRAVAIL → 400.
- [ ] POST upsert : deuxième POST met à jour au lieu de créer (pas de doublon).
- [ ] Sérialisation/désérialisation JSON via `ObjectMapper` Jackson (pattern existant).
- [ ] 8 tests d'intégration (4 POST succès/erreurs + 2 GET succès/404 + 2 isolation).

---

## Périmètre

### Hors scope (explicite)

- Composant Angular (→ SF-DT-10-03).
- Orchestration UX (→ SF-DT-10-04).
- Exposition du référentiel via un endpoint GET `/rupture-conv/criteres` — le frontend embarquera une copie statique du catalogue (comme F-DT-08). Ajouter l'endpoint si un vrai besoin de paramétrage dynamique émerge.
- Historique des analyses — unicité 1:1 confirmée.
- Suppression explicite (DELETE) — hors scope, cascade via `case_files.deleted_at`.

---

## Valeurs initiales

Pas de seed, création à la demande.

---

## Contraintes de validation

| Champ | Obligatoire | Format |
|-------|-------------|--------|
| `country` | Oui | enum { FRANCE } en V2 initial |
| `reponses` | Non | map{string → string} ; null ou absent = map vide |
| Valeurs dans `reponses` | Non | normalisation fail-open (cf. SF-DT-10-01) |

---

## Technique

### Endpoints

| Méthode | URL | Rôle | Statut |
|---------|-----|------|--------|
| POST | `/api/v1/case-files/{caseFileId}/rupture-conv` | Upsert analyse | 200 OK |
| GET | `/api/v1/case-files/{caseFileId}/rupture-conv` | Lecture analyse | 200 OK / 404 |

### Tables impactées

- `rupture_conv_analyses` — écriture par POST, lecture par GET. Pas de modif de schéma.

### Migration Liquibase

Non applicable.

### Composants backend

- `RuptureConvRequest` (record)
- `RuptureConvResponse` (record + `CritereData` interne)
- `RuptureConvService` (`@Service`, `@Transactional`)
- `RuptureConvController` (`@RestController`)

### Composants frontend

Aucun dans cette SF (SF-DT-10-03 consommera l'endpoint).

### Sécurité

- Pas de nouveau rôle.
- Isolation workspace via `WorkspaceMemberRepository.findByUserAndPrimaryTrue` (pattern existant), rejet 404 si non membre.

---

## Plan de test

### Tests d'intégration (MockMvc + H2)

- [ ] `POST rupture-conv FRANCE + reponses partielles → 200, analyse persistée, response avec score calculé`
- [ ] `POST rupture-conv BELGIQUE → 400`
- [ ] `POST rupture-conv caseFileId inconnu → 404`
- [ ] `POST rupture-conv depuis user d'un autre workspace → 404 (isolation)`
- [ ] `POST rupture-conv sur dossier DROIT_IMMIGRATION → 400 (domaine)`
- [ ] `GET rupture-conv après POST → 200, contenu cohérent avec dernière POST`
- [ ] `GET rupture-conv sans POST préalable → 404`
- [ ] `POST deux fois sur le même dossier → une seule ligne en base (upsert)`

### Isolation workspace

- [x] Test dédié dans la suite d'intégration.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal : **oui (indirect)** — réutilise le pattern existant `@AuthenticationPrincipal OidcUser + Principal + CurrentUserResolver`. Aucun changement du Principal ou de la résolution.
- [ ] Workspace context : **oui (indirect)** — réutilise `WorkspaceMemberRepository.findByUserAndPrimaryTrue`. Aucun changement de la logique.
- [ ] Plans / limites : non
- [ ] Navigation / routing frontend : non

> Préoccupations cochées en "indirect" : aucun changement du mécanisme, réutilisation à l'identique. Test d'isolation workspace inclus dans le plan de test.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|-----------|--------|-----------------------|
| Endpoints existants | Aucun — nouveau chemin `/rupture-conv` | Tests IT F-DT-08 conservés |
| `CaseFile` entity | Aucun (lecture uniquement) | — |

### Smoke tests E2E concernés

- [ ] Aucun — feature nouvelle, aucun chemin E2E ne l'inclut à ce stade.

---

## Dépendances

### Subfeatures bloquantes

- `SF-DT-10-01` **mergée** — fournit `RuptureConvAnalyzer`, référentiel, entité, repository.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi pas d'endpoint GET `/criteres`** : le référentiel est figé côté backend (6 critères, base juridique claire). Le frontend SF-DT-10-03 dupliquera le catalogue en dur comme F-DT-08, économisant un aller-retour HTTP et évitant un couplage frontend/backend sur le cache référentiel.
- **Pourquoi copie exacte du pattern F-DT-08** : la logique métier est identique (upsert 1:1 par dossier + isolation workspace + domaine requis). Toute divergence serait accidentelle.
- **Pourquoi pas de DELETE** : aucun besoin exprimé, suppression passée par soft-delete du dossier parent.
- **Pourquoi 404 au lieu de 403 sur isolation** : pattern du codebase — on ne révèle pas l'existence d'un dossier qu'on ne peut pas voir.
