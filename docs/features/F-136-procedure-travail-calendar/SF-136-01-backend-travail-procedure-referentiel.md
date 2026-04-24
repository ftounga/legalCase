# Mini-spec — F-136 / SF-136-01 — Backend `TravailProcedureReferentiel` (prud'hommes FR + tribunal travail BE)

## Identifiant

`F-136 / SF-136-01`

## Feature parente

`F-136` — Calendrier procédural prud'hommes / tribunal travail BE (PRODUCT_SPEC.md ligne 388, V7 — backlog, 🔴 critique).

> Attention au homonyme : l'ancienne `F-136 — Enrichissement massif des référentiels` (Terminée le 2026-04-20) occupe le dossier `docs/features/F-136-massive-referentials-enrichment/`. La nouvelle F-136 (calendrier procédural travail) ré-utilise le numéro de feature au backlog mais vit dans un dossier dédié `F-136-procedure-travail-calendar/`.

## Statut

`ready`

## Date de création

2026-04-24

## Branche Git

`feat/SF-136-01-backend-travail-procedure-referentiel`

---

## Objectif

Généraliser le pattern `ImmigrationProcedureReferentiel` au droit du travail : créer un référentiel statique `TravailProcedureReferentiel` (jalons + délais + articles) couvrant les 6 procédures clés FR (prud'hommes, appel CA sociale, cassation sociale) et BE (tribunal du travail, cour du travail, cassation BE), seedé en DB via `legal_referentials` (type `TRAVAIL_PROCEDURE_JALONS`) avec descriptions avocat (SF-140-03) et accessible via `LegalReferentialService.getTravailProcedureJalons(typeProcedure, country)`.

---

## Comportement attendu

### Cas nominal

1. Un consommateur backend (ex. `StatutoryDeadlineService` ou futur outil F-136 SF-02) appelle `LegalReferentialService.getTravailProcedureJalons(typeProcedure, country)` avec `typeProcedure ∈ { PRUDHOMMES_FR, APPEL_CA_SOCIALE_FR, CASSATION_SOCIALE_FR, TRIBUNAL_TRAVAIL_BE, COUR_TRAVAIL_BE, CASSATION_BE }`.
2. Le service fait un `findSystemEntryByCountry("DROIT_DU_TRAVAIL", "TRAVAIL_PROCEDURE_JALONS", typeProcedure, country)`.
3. La DB retourne le `value_json` (liste de `{ label, offsetDays, articleRef }`) seedé par la migration 130.
4. Le service parse la liste et la retourne sous forme `List<TravailProcedureReferentiel.ProcedureJalon>` (record).
5. Le consommateur peut générer ses jalons d'audience / délais d'appel à partir de la date pivot (date saisine, date jugement, date signification…).

### Fallback Java

Si la DB est indisponible ou l'entry absente, `LegalReferentialService` log un warning et retourne `TravailProcedureReferentiel.resolve(typeProcedure, country)` (constantes Java alignées sur le seed DB — pattern strict de `ImmigrationProcedureReferentiel`).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `typeProcedure` null ou inconnu | Retour `List.of()` (pas d'exception) | n/a (interne) |
| `country` null | Retour `List.of()` (toutes les procédures sont country-scoped) | n/a (interne) |
| DB injoignable | Warning loggé, fallback Java appliqué | n/a (interne) |
| Migration 130 sans `description` ou avec UUID en collision | `LegalReferentialDescriptionIntegrityIT` échoue en CI | n/a (CI) |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : `ImmigrationProcedureReferentiel` (pattern de référence — 10 procédures FR seedées migrations 049 + 108) ; aucun équivalent existant côté `DROIT_DU_TRAVAIL` ni `DROIT_FAMILLE` (F-137 ouvrira ce dernier).
- [x] **Autres pays** : France (prud'hommes / CA sociale / Cass. sociale) + Belgique (tribunal du travail / cour du travail / Cass. BE). Couverts en parité dans la même SF.
- [x] **Autres domaines** : `DROIT_DU_TRAVAIL` (cette SF) ; `DROIT_IMMIGRATION` déjà couvert (`IMMIGRATION_JALONS`) ; `DROIT_FAMILLE` à couvrir par F-137 (calendrier JAF / tribunal famille BE) — feature jumelle déjà au backlog.
- [x] **Autres UI patterns** : aucun — SF backend pure, pas d'écran. SF-136-02/03 ouvriront l'endpoint HTTP + UI.
- [x] **Autres flows transversaux** : pas d'auth / workspace / plans / navigation impactés. Référentiel système (`workspace_id IS NULL`).

### Niveaux de vérification

- [x] **Modèle TypeScript / API exposée** — non applicable (SF backend, pas d'endpoint).
- [x] **Record / DTO backend** — `TravailProcedureReferentiel.ProcedureJalon(label, offsetDays, articleRef)` (record).
- [x] **Service / logique métier** — `LegalReferentialService.getTravailProcedureJalons(typeProcedure, country)`.
- [x] **Entité JPA + schéma DB** — table existante `legal_referentials`, type `TRAVAIL_PROCEDURE_JALONS`, persistance JSON dans `value_json`.
- [x] **Tests existants** — `ImmigrationProcedureReferentielTest` (pattern de réutilisation), `LegalReferentialDescriptionIntegrityIT` (garde-fou SF-140-03).

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Où le nouveau pattern pourrait-il être réutilisé ?** Aucun nouveau pattern UI introduit. La méthode `getTravailProcedureJalons` est strictement symétrique à `getImmigrationJalons` — elle suit le pattern existant.
- [x] **Y a-t-il des patterns concurrents ?** Non — c'est le 2e domaine à utiliser le pattern de jalons procéduraux. F-137 sera le 3e (DROIT_FAMILLE) et utilisera le même pattern (`FAMILLE_PROCEDURE_JALONS`).
- [x] **Le nouveau service / endpoint peut-il servir à d'autres features ?** Oui — la méthode sera consommée par SF-136-02 (endpoint HTTP) puis par les outils décisionnels d'un futur "calendrier procédural travail" (mêmes appellants que `StatutoryDeadlineService` côté immigration).
- [x] **Le nouveau composant a-t-il un équivalent design qu'il remplace ?** Non — création.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `ImmigrationProcedureReferentiel` (pattern source) | Oui | Référence — copié strictement (record `ProcedureJalon` + Map type → liste, méthode `resolve`). |
| Famille (`FAMILLE_PROCEDURE_JALONS`) | Oui | Backlog — F-137 déjà au backlog (PRODUCT_SPEC ligne 389). Hors scope SF-136-01. |
| FR + BE en parité | Oui | Intégré — les 6 procédures sont livrées dans la même SF. |
| `StatutoryDeadlineService` (consommateur immigration existant) | Oui | Non applicable à cette SF — la création des `case_deadlines` automatiques pour le travail relèvera d'une SF future (SF-136-02 ou F-136 SF-04). Pas de consommation auto déclenchée ici. |
| Endpoint HTTP `GET /api/v1/travail/procedures/{type}` | Oui | Backlog — SF-136-02 (hors scope ici comme spécifié). |
| Frontend (composant timeline) | Oui | Backlog — SF-136-03 (hors scope). |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (FR + BE, 6 procédures, fallback Java + DB).
- [x] Subfeature(s) parallèle(s) créée(s) pour les cibles restantes : SF-136-02 (endpoint HTTP), SF-136-03 (UI timeline), F-137 (jumeau famille — déjà au backlog).
- [x] Backlog VN pour les cibles non prioritaires : `StatutoryDeadlineService` automatisation prud'hommes (à ouvrir si besoin métier réel ; pour l'instant l'avocat saisit la date d'audience à la main — réutilisable depuis SF-136-02).
- [x] Non applicable aux autres cibles (famille = F-137, immigration = déjà couvert).

---

## Critères d'acceptation

- [ ] Classe `fr.ailegalcase.casefile.TravailProcedureReferentiel` créée, finale, avec record interne `ProcedureJalon(String label, int offsetDays, String articleRef)`, méthode statique `resolve(String typeProcedure, String country)` qui retourne `List<ProcedureJalon>` (immuable, vide si inconnu).
- [ ] Constantes des 6 types : `PRUDHOMMES_FR`, `APPEL_CA_SOCIALE_FR`, `CASSATION_SOCIALE_FR`, `TRIBUNAL_TRAVAIL_BE`, `COUR_TRAVAIL_BE`, `CASSATION_BE` (Strings publiques).
- [ ] Migration Liquibase `130-seed-travail-procedure-jalons.xml` qui INSERT 6 entries `is_system=true` dans `legal_referentials` (legal_domain `DROIT_DU_TRAVAIL`, referential_type `TRAVAIL_PROCEDURE_JALONS`, country `FR` ou `BE`, `value_json` cohérent avec la liste Java, `description` avocat obligatoire, `source_ref` article principal).
- [ ] Méthode `LegalReferentialService.getTravailProcedureJalons(String typeProcedure, String country)` créée, DB-first avec fallback `TravailProcedureReferentiel.resolve(...)` strictement symétrique à `getImmigrationJalons`.
- [ ] Tests unitaires `TravailProcedureReferentielTest` couvrant : (a) lookup `PRUDHOMMES_FR` retourne ≥3 jalons cohérents, (b) lookup `TRIBUNAL_TRAVAIL_BE` retourne ≥2 jalons, (c) lookup type inconnu retourne liste vide, (d) lookup null retourne liste vide, (e) chaque jalon FR a un `articleRef` qui démarre par `Code travail` ou `CPC` ou `R.`/`L.`, chaque jalon BE par `CJ` ou article du Code judiciaire belge.
- [ ] Test d'intégration `LegalReferentialDescriptionIntegrityIT` continue à passer (description obligatoire sur les 6 nouvelles entries).
- [ ] `./mvnw test` full suite verte.

---

## Périmètre

### Hors scope (explicite)

- Aucun endpoint HTTP créé (vient en SF-136-02).
- Aucune modification frontend (vient en SF-136-03).
- Aucune génération automatique de `case_deadlines` (le pattern existe pour immigration via `StatutoryDeadlineService`, mais brancher prud'hommes nécessite de définir la date pivot — date de saisine ? date d'audience ? — décision produit à prendre dans une SF future).
- Pas de jumeau famille (F-137 séparée).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `is_system` | `true` | Entries seedées par migration, pas modifiables sans override workspace. |
| `is_active` | `true` | Visibles immédiatement. |
| `workspace_id` | `NULL` | Référentiel système. |
| `legal_domain` | `DROIT_DU_TRAVAIL` | Domaine cible. |
| `referential_type` | `TRAVAIL_PROCEDURE_JALONS` | Nouveau type, symétrique à `IMMIGRATION_JALONS`. |
| `country` | `FR` ou `BE` | Pays-scoped (jamais NULL — chaque procédure est nationale). |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `entry_key` | Oui | 200 | `PRUDHOMMES_FR`, `APPEL_CA_SOCIALE_FR`, `CASSATION_SOCIALE_FR`, `TRIBUNAL_TRAVAIL_BE`, `COUR_TRAVAIL_BE`, `CASSATION_BE` | (legal_domain, referential_type, entry_key, country, workspace_id NULL) unique | UPPER, trim |
| `value_json` | Oui | TEXT | JSON `[{ label, offsetDays:int, articleRef }]` | — | — |
| `description` | Oui | TEXT | Langage avocat — quand utiliser, que représente la procédure, jalons clés | — | — |
| `country` | Oui | 20 | `FR` ou `BE` | — | UPPER |
| `source_ref` | Oui | 200 | Référence légale principale (ex. `Code travail Art. R.1454-1` ou `CJ Art. 578`) | — | — |

---

## Procédures et jalons (contrat seed)

### France

| Type | Pays | Jalons | Source légale |
|------|------|--------|---------------|
| `PRUDHOMMES_FR` | FR | (1) Convocation au bureau de conciliation et d'orientation `+45j` (art. R.1452-3 Code travail) — (2) Audience BCO `+90j` (art. L.1454-1) — (3) Renvoi bureau de jugement si échec conciliation `+180j` — (4) Audience bureau de jugement `+270j` — (5) Délibéré et notification `+330j` (art. R.1454-25) | Code travail R.1452-1 à R.1454-26 + L.1454-1 |
| `APPEL_CA_SOCIALE_FR` | FR | (1) Délai d'appel `+30j` à compter de la notification (art. R.1461-1 Code travail) — (2) Audience chambre sociale CA `+270j` (délai indicatif) — (3) Arrêt et notification `+330j` | Code travail R.1461-1 + CPC 538 |
| `CASSATION_SOCIALE_FR` | FR | (1) Délai pourvoi `+60j` à dater de la signification de l'arrêt (art. 612 CPC) — (2) Mémoire ampliatif `+150j` (art. 978 CPC) — (3) Audience chambre sociale Cass. `+540j` (délai indicatif) — (4) Arrêt `+630j` | CPC art. 612, 978, 1009 |

### Belgique

| Type | Pays | Jalons | Source légale |
|------|------|--------|---------------|
| `TRIBUNAL_TRAVAIL_BE` | BE | (1) Citation et 1ʳᵉ audience d'introduction `+30j` (art. 700 CJ) — (2) Mise en état (échange conclusions) `+120j` (art. 747 CJ) — (3) Audience de plaidoiries `+240j` — (4) Jugement `+300j` (art. 770 CJ) | CJ art. 578, 700, 747, 770 |
| `COUR_TRAVAIL_BE` | BE | (1) Délai d'appel `+30j` à dater de la signification (art. 1051 CJ) — (2) Mise en état appel `+150j` — (3) Audience cour du travail `+300j` — (4) Arrêt `+360j` | CJ art. 1050, 1051, 1056 |
| `CASSATION_BE` | BE | (1) Délai pourvoi `+90j` (art. 1073 CJ) — (2) Mémoire `+180j` (art. 1080 CJ) — (3) Audience Cass. BE `+450j` — (4) Arrêt `+540j` | CJ art. 1073, 1080, 1095 |

> Délais stricts (recours / pourvoi) = articles cités, sources directes. Délais d'audience / délibéré (durée moyenne) = indicatifs basés sur les pratiques du greffe — documentés comme tels dans la `description` avocat.

---

## Technique

### Endpoint(s)

Aucun (SF-136-02 plus tard).

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `legal_referentials` | INSERT × 6 | 6 entries `is_system=true`, type `TRAVAIL_PROCEDURE_JALONS`. |

### Migration Liquibase

- [x] Oui — `130-seed-travail-procedure-jalons.xml` (UUID prefix `f1130000-…` pour éviter collisions).
- [ ] Non applicable

### Composants Angular (si applicable)

Aucun.

---

## Plan de test

### Tests unitaires

- [ ] `TravailProcedureReferentielTest.prudhommesFr_returns5Jalons` — `resolve("PRUDHOMMES_FR","FR")` retourne 5 jalons aux offsets 45/90/180/270/330.
- [ ] `TravailProcedureReferentielTest.appelCaSocialeFr_delaiAppel30Jours` — premier jalon `APPEL_CA_SOCIALE_FR` à 30j et label contient "appel".
- [ ] `TravailProcedureReferentielTest.cassationSocialeFr_pourvoi60Jours` — premier jalon à 60j (art. 612 CPC).
- [ ] `TravailProcedureReferentielTest.tribunalTravailBe_returnsAtLeast4Jalons` — `resolve("TRIBUNAL_TRAVAIL_BE","BE")` retourne 4 jalons.
- [ ] `TravailProcedureReferentielTest.courTravailBe_delaiAppel30Jours` — premier jalon à 30j (art. 1051 CJ).
- [ ] `TravailProcedureReferentielTest.cassationBe_pourvoi90Jours` — premier jalon à 90j (art. 1073 CJ).
- [ ] `TravailProcedureReferentielTest.unknownType_returnsEmptyList`.
- [ ] `TravailProcedureReferentielTest.nullType_returnsEmptyList`.
- [ ] `TravailProcedureReferentielTest.allFrTypesHaveCodeTravailOrCpcArticleRef` — chaque jalon FR a `articleRef` non vide.
- [ ] `TravailProcedureReferentielTest.allBeTypesHaveCjArticleRef` — chaque jalon BE a `articleRef` qui contient `CJ` ou `Art.`.

### Tests d'intégration

- [ ] `LegalReferentialDescriptionIntegrityIT` — toujours vert (description sur les 6 nouvelles entries).
- [ ] `./mvnw test` full backend reste vert.

### Isolation workspace

- [x] Non applicable — référentiel système (`workspace_id IS NULL`), accessible à tous workspaces.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité à la création d'un référentiel statique. Pas d'auth / workspace context / plans / navigation touchés.

### Impact par domaine métier

- **DROIT_DU_TRAVAIL** : oui — création directe du référentiel pour FR + BE en parité.
- **DROIT_IMMIGRATION** : non — déjà couvert via `IMMIGRATION_JALONS`. Pas de modification.
- **DROIT_FAMILLE** : non concerné par cette SF — F-137 est la feature jumelle famille, déjà au backlog.

> Couverture FR + BE en parité dans la même SF (alignement règle "Parité des domaines métier" — niveau 1 checklist, pas un scoring/comparateur, donc règle non strictement déclenchée mais respectée par symétrie).

### Composants existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|------------------|------------------------------|
| `LegalReferentialService` | Nouvelle méthode `getTravailProcedureJalons` ajoutée — pas de modification des méthodes existantes | Tests unitaires existants (`LegalReferentialServiceTest`) restent verts |
| `ImmigrationProcedureReferentiel` | Aucun — code distinct | n/a |
| `LegalReferentialDescriptionIntegrityIT` | 6 nouvelles entries doivent passer le check description | IT tournée |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — backend pur, aucun écran, aucun endpoint.

---

## Dépendances

### Subfeatures bloquantes

- Aucune.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Choix nom du type DB** : `TRAVAIL_PROCEDURE_JALONS` (et non `PRUDHOMMES_JALONS`) pour englober prud'hommes FR + tribunal travail BE + appels + cassations dans un même référentiel — symétrique à `IMMIGRATION_JALONS` qui couvre OFPRA/CNDA/préfecture/TA d'un seul referential_type.
- **Choix structure record** : ajout du champ `articleRef` (absent du record immigration `ProcedureJalon(label, offsetDays)`) parce que la traçabilité légale est plus critique pour le contentieux travail (un avocat doit pouvoir citer l'article exact dans ses conclusions). On ne casse pas le pattern immigration : c'est un enrichissement local au record `TravailProcedureReferentiel.ProcedureJalon`, indépendant du record immigration.
- **UUID prefix** : `f1130000-0000-0000-0000-00000000000X` (préfixe migration 130) pour éviter collisions avec migrations 049 (`f1049000-…`) et 108 (`f1108000-…`).
- **Délais indicatifs vs stricts** : les délais procéduraux légaux (recours/pourvoi) sont fermes ; les délais d'audience et délibéré sont indicatifs (pratiques greffe). La distinction est portée dans la `description` avocat de chaque entry.
- **Pas de génération auto de `case_deadlines`** : ce serait piège (date pivot ambiguë : date de saisine ? date jugement ?). Renvoyé à une SF future si besoin métier.
