# Mini-spec — F-129 / SF-129-01 Seed 49 CCN IDCC en DB + dropdown dynamique

## Identifiant
`F-129 / SF-129-01`

## Feature parente
`F-129` — Référentiel conventions collectives — couverture étendue

## Statut `draft`  · Date `2026-04-20`  · Branche `feat/SF-129-01-seed-conventions-idcc`

---

## Objectif

Remplacer le dropdown hardcodé à 5 CCN françaises par un dropdown **dynamique affichant 49 CCN officielles** (top 95% du volume prud'homal par effectif), chargées depuis la DB via le référentiel existant `legal_referentials`. Une CCN correctement détectée par l'IA (ex. Propreté IDCC 3043 sur E24) sera **pré-sélectionnée automatiquement** au lieu de laisser l'avocat choisir manuellement une convention incomplète.

---

## Comportement

### Contexte actuel (problème)

- `anciennete-section.component.ts` a 5 CCN FR + 3 CP BE hardcodées
- Sur le dossier E24 : IA détecte `convention_collective = "NETTOYAGE"` (Propreté IDCC 3043)
- `NETTOYAGE` n'est pas dans les 5 codes → dropdown garde "METALLURGIE" par défaut → aucune pré-sélection
- Infrastructure DB-first existe déjà (`LegalReferentialService.getConventionBareme`) mais seuls les 5 codes hardcodés ont été seedés

### Nouveau comportement

**Seed DB** : migration Liquibase 086 insert 49 CCN FR dans `legal_referentials`
(type `CONVENTION_BAREMES`). Chaque entry :
- `entry_key` = code court déterministe (ex. `IDCC_3043` ou `NETTOYAGE`)
- `label` = libellé officiel depuis kali-data
- `country` = FRANCE
- `value_json` = `{"congesLegauxJours": 25, "congesSupp": [], "primes": []}` (minimums légaux par défaut)
- `source_ref` = "IDCC {numéro} — data.gouv.fr / kali-data"

**Endpoint** : `GET /api/v1/referentials/conventions` renvoie la liste des entries actives (FR + BE). Accessible à tout utilisateur authentifié, workspace-scoped via le pattern existant.

**Frontend** : `anciennete-section.component.ts` supprime les tableaux `conventionsFrance`/`conventionsBelgique` hardcodés. Remplacement par un signal chargé depuis un nouveau `ConventionReferentialService.list()`. Dropdown dynamique.

**Fallback pré-sélection** : si l'IA renvoie un code inconnu (ex. CCN absente du top 49) :
- Dropdown ne force pas une pré-sélection erronée
- Badge informatif affiché : "Convention détectée par l'IA : {libellé IA brut} (non présente dans notre référentiel — veuillez sélectionner la plus proche ou saisir manuellement les barèmes)"

### Cas d'erreur

- DB vide / référentiel non seedé → fallback automatique vers `ConventionBaremeReferentiel.java` statique existant (pattern DB-first existant déjà)
- Endpoint échec réseau → dropdown utilise un fallback statique minimal (5 CCN FR + 3 CP BE comme aujourd'hui)

---

## Critères d'acceptation

- [ ] Migration Liquibase 086 insert 49 CCN FR dans `legal_referentials` avec `referential_type = 'CONVENTION_BAREMES'`
- [ ] Migration préserve les 5 CCN FR + 3 CP BE existantes (via `ConventionBaremeReferentiel.java` fallback)
- [ ] `GET /api/v1/referentials/conventions` renvoie `[{code, label, country}, ...]` avec les 49 FR + 3 BE
- [ ] Dropdown de `anciennete-section` affiche dynamiquement toutes les CCN depuis l'endpoint
- [ ] Sur E24 : relancer l'analyse → convention Propreté pré-sélectionnée
- [ ] `ReferentialCheckService` (scheduler 6 mois) scanne désormais les 49 nouvelles entries automatiquement
- [ ] Admin peut override/update une entry via l'UI existante `PUT /api/v1/referential/{id}`
- [ ] Aucune régression sur les calculs de congés / prime d'ancienneté pour les 5 CCN préexistantes

---

## Plan de test

### Unitaires backend
- `LegalReferentialServiceTest` — nouveau test : `getConventionBareme("NETTOYAGE")` renvoie l'entry DB avec 25j + pas de congés sup
- `LegalReferentialServiceTest` — test existant : `getConventionBareme("METALLURGIE")` renvoie toujours l'entry DB (migration préserve) ou fallback statique
- Nouveau `ConventionReferentialControllerTest` — `GET /api/v1/referentials/conventions` renvoie 200 + JSON attendu avec ≥ 49 entries FR + 3 BE

### Unitaires frontend
- `AncienneteSectionComponent.spec.ts` — test existant : pré-sélection convention hardcodée → convertir vers signal dynamique
- Nouveau test : IA envoie `conventionCollective = "NETTOYAGE"` → dropdown pré-sélectionne `NETTOYAGE` (après chargement depuis service)
- Nouveau test : IA envoie `conventionCollective = "INCONNUE"` → badge informatif affiché, pas de pré-sélection forcée

### Intégration manuelle staging
- Relancer analyse complète sur E24 → convention `NETTOYAGE` / `Propreté (IDCC 3043)` apparaît pré-sélectionnée dans anciennete-section
- Vérifier le dropdown affiche ~50 options (FR + BE)

### Isolation workspace
- N/A : `legal_referentials` est system-wide (workspace_id = NULL pour `is_system = true`)

---

## Tables / endpoints / composants impactés

### Backend
- `db/changelog/migrations/086-seed-conventions-idcc.xml` — NOUVELLE migration (49 INSERT)
- `ConventionReferentialController.java` — NOUVEAU (endpoint GET)
- `LegalReferentialService.java` — possibles ajouts si méthode de listing
- `ConventionBaremeReferentiel.java` — inchangé (fallback statique préservé)

### Frontend
- `anciennete-section.component.ts` — supprime hardcode, ajoute signal + chargement service
- `anciennete-section.component.html` — template dropdown inchangé (utilise le signal)
- `convention-referential.service.ts` — NOUVEAU service HTTP
- Tests unitaires

### Config / DB
- Aucune nouvelle table — utilise la table `legal_referentials` existante (migration 048)

---

## Hors périmètre

- **Barèmes spécifiques** (congés sup + primes ancienneté par tranches) pour les 49 nouvelles CCN : renvoyé à SF-129-02. La SF-129-01 pose les minimums légaux pour déjà débloquer la sélection.
- **CP belges étendues** (+7 CP au-delà des 3 existantes) : renvoyé à SF-129-03.
- **UI admin dédiée** pour édition batch des CCN : utilise l'UI existante `LegalReferential` (édition one-by-one suffit pour V1).
- **Prompt enum constraint** : les 49 codes FR ne sont pas listés dans le prompt. L'IA reste libre de renvoyer n'importe quel code textuel. Le fallback UI gère les codes inconnus. Raison : la liste est trop longue pour l'enum prompt (~500 tokens rien que pour l'énumération) et le fallback couvre le cas.

---

## Analyse de cohérence transversale

| Cible | Applicable | Classement |
|---|---|---|
| Autres pays (Belgique) | Oui | **Intégrée en héritage** — les 3 CP BE existantes restent, ajout des 7 CP BE renvoyé à SF-129-03 (effort de recherche distinct) |
| Autres domaines (immigration/famille) | Non applicable | Les CCN sont un concept spécifique au droit du travail |
| Autres référentiels (immigration_titles, divorce_pieces, etc.) | Non applicable | Suivent déjà le même pattern DB-first via migration 067, pas besoin de refactor |
| Prompt IA enum | **Non applicable V1** — 49 codes trop longs à embedder dans le prompt. Le fallback UI compense. Future amélioration possible via SF-129-04 si besoin. |

**Analyse d'impact cross-cutting** :
- [ ] Auth / Principal — non touché (endpoint hérite des règles génériques)
- [ ] Workspace context — non touché (référentiels system-wide, workspace_id=NULL)
- [ ] Plans / limites — non touché (donnée publique métier)
- [ ] Navigation / routing — non touché

Aucun smoke E2E concerné (dropdown change mais pas de nouveau flow utilisateur).

---

## Nouveau pattern UI ou service partagé

- [x] **`ConventionReferentialService`** (frontend) — nouveau service HTTP. Réutilisable par tout composant ayant besoin de la liste des CCN (futurs outils décisionnels, page settings admin, etc.).
- [x] **Endpoint `GET /api/v1/referentials/conventions`** — nouveau, endpoint de lecture générique. Pattern aligné avec les autres endpoints référentiels existants.
- [x] Pas de nouveau pattern UI fondamental — le dropdown MatSelect existe déjà avec son style.
