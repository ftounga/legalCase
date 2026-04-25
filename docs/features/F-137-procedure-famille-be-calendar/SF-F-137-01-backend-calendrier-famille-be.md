# Mini-spec — F-137 / SF-F-137-01 — Backend `FamilleProcedureReferentiel` (tribunal famille BE + Cour appel + Cassation)

## Identifiant

`F-137 / SF-F-137-01`

## Feature parente

`F-137` — Calendrier procédural JAF / tribunal famille BE (PRODUCT_SPEC.md ligne 397, V7 — backlog, 🔴 critique).

> Attention au homonyme : l'ancienne `F-137 — Refonte UX "Guides & barèmes"` (Terminée le 2026-04-20) occupe le dossier `docs/features/F-137-guides-baremes-ux-refresh/`. La nouvelle F-137 (calendrier procédural famille) ré-utilise le numéro de feature au backlog mais vit dans un dossier dédié `F-137-procedure-famille-be-calendar/`.

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-F-137-01-backend-calendrier-famille-be`

---

## Objectif

Étendre le pattern `TravailProcedureReferentiel` (F-136 SF-136-01) au droit de la famille belge : créer le référentiel statique `FamilleProcedureReferentiel` (jalons + délais + articles) couvrant les 3 procédures clés famille BE (tribunal de la famille, cour d'appel, Cassation), seedé en DB via `legal_referentials` (type `FAMILLE_PROCEDURE_JALONS`) avec descriptions avocat (SF-140-03) et accessible via `LegalReferentialService.getFamilleProcedureJalons(typeProcedure, country)`.

> Périmètre : **Belgique uniquement** dans cette SF (mirror strict du brief). Le calendrier procédural famille FR (JAF + audience MEC + ONC + appel CA + Cass civile) est ouvert au backlog comme SF-F-137-02 jumelle (à scinder).

---

## Comportement attendu

### Cas nominal

1. Un consommateur backend (futur outil F-137 SF-02 endpoint, ou outils décisionnels famille BE comme F-FA-21 séparation de corps, F-FA-12 mesures provisoires) appelle `LegalReferentialService.getFamilleProcedureJalons(typeProcedure, country)` avec `typeProcedure ∈ { TRIBUNAL_FAMILLE_BE, COUR_APPEL_FAMILLE_BE, CASSATION_FAMILLE_BE }` et `country = "BE"`.
2. Le service fait un `findSystemEntryByCountry("DROIT_FAMILLE", "FAMILLE_PROCEDURE_JALONS", typeProcedure, country)`.
3. La DB retourne le `value_json` (liste de `{ label, offsetDays, articleRef }`) seedé par la migration 162.
4. Le service parse et retourne `List<FamilleProcedureReferentiel.ProcedureJalon>` (record).
5. Le consommateur peut générer ses jalons (audience MP, mise en état, fond, prononcé, appel, cassation) à partir d'une date pivot (date dépôt requête, date jugement, date signification).

### Fallback Java

Si la DB est indisponible ou l'entry absente, `LegalReferentialService` log un warning et retourne `FamilleProcedureReferentiel.resolve(typeProcedure, country)` (constantes Java alignées sur le seed DB — pattern strict de `TravailProcedureReferentiel`).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `typeProcedure` null ou inconnu | Retour `List.of()` (pas d'exception) | n/a (interne) |
| `country` null | Retour `List.of()` (toutes les procédures sont country-scoped BE) | n/a (interne) |
| `country` ≠ BE | Retour `List.of()` (FR sera ouvert en SF jumelle) | n/a (interne) |
| DB injoignable | Warning loggé, fallback Java appliqué | n/a (interne) |
| Migration 162 sans `description` ou avec UUID en collision | `LegalReferentialDescriptionIntegrityIT` échoue en CI | n/a (CI) |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : `TravailProcedureReferentiel` (F-136 SF-136-01, pattern source mirroré strictement) et `ImmigrationProcedureReferentiel` (pattern initial). Aucun équivalent existant côté `DROIT_FAMILLE`.
- [x] **Autres pays** : Belgique seul dans cette SF (mirror strict du brief). France (JAF + appel CA + Cass. civile) ouvert au backlog → SF-F-137-02 jumelle à créer (pattern symétrique strict).
- [x] **Autres domaines** : `DROIT_FAMILLE` (cette SF) ; `DROIT_DU_TRAVAIL` déjà couvert (F-136) ; `DROIT_IMMIGRATION` déjà couvert (`IMMIGRATION_JALONS`).
- [x] **Autres UI patterns** : aucun — SF backend pure, pas d'écran. Endpoints HTTP + UI timeline dans SF futures (F-137 SF-02/03).
- [x] **Autres flows transversaux** : pas d'auth / workspace / plans / navigation impactés. Référentiel système (`workspace_id IS NULL`).

### Niveaux de vérification

- [x] **Modèle TypeScript / API exposée** — non applicable (SF backend, pas d'endpoint).
- [x] **Record / DTO backend** — `FamilleProcedureReferentiel.ProcedureJalon(label, offsetDays, articleRef)` (record, miroir strict de `TravailProcedureReferentiel.ProcedureJalon`).
- [x] **Service / logique métier** — `LegalReferentialService.getFamilleProcedureJalons(typeProcedure, country)` ajouté (miroir strict de `getTravailProcedureJalons`).
- [x] **Entité JPA + schéma DB** — table existante `legal_referentials`, type `FAMILLE_PROCEDURE_JALONS`, persistance JSON dans `value_json`, country `BE`.
- [x] **Tests existants** — `TravailProcedureReferentielTest` (pattern de réutilisation), `LegalReferentialDescriptionIntegrityIT` (garde-fou SF-140-03).

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Où le nouveau pattern pourrait-il être réutilisé ?** Aucun nouveau pattern UI introduit. La méthode `getFamilleProcedureJalons` est strictement symétrique à `getTravailProcedureJalons` — elle suit le pattern existant.
- [x] **Y a-t-il des patterns concurrents ?** Non — c'est le 3e domaine à utiliser le pattern de jalons procéduraux. Aligné par construction.
- [x] **Le nouveau service / endpoint peut-il servir à d'autres features ?** Oui — la méthode sera consommée par les outils décisionnels famille BE (F-FA-12 mesures provisoires, F-FA-21 séparation de corps, etc.) qui auront besoin d'évaluer un calendrier procédural à partir d'une date pivot.
- [x] **Le nouveau composant a-t-il un équivalent design qu'il remplace ?** Non — création.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `TravailProcedureReferentiel` (pattern source F-136) | Oui | Référence — mirror strict (record `ProcedureJalon` + Map type → liste, méthode `resolve`). |
| Famille FR (`JAF_FR`, `APPEL_FAMILLE_FR`, `CASSATION_FAMILLE_FR`) | Oui | Backlog — SF-F-137-02 jumelle à créer (parité FR/BE). Hors scope ici (brief explicite). |
| Travail BE / Immigration | Non | Déjà couverts par F-136 et `IMMIGRATION_JALONS`. |
| Endpoint HTTP `GET /api/v1/case-files/{id}/famille-procedure-jalons` | Oui | Backlog — SF-F-137-03 (hors scope ici comme spécifié). |
| Frontend (composant timeline famille) | Oui | Backlog — SF-F-137-04 (hors scope). |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (BE — 3 procédures — fallback Java + DB).
- [x] Subfeature(s) parallèle(s) créée(s) pour les cibles restantes : SF-F-137-02 famille FR (jumelle), SF-F-137-03 endpoint HTTP, SF-F-137-04 UI timeline.
- [x] Backlog VN : néant supplémentaire — tout est tracé.
- [x] Non applicable aux autres cibles (travail = F-136, immigration = déjà couvert).

---

## Impact par domaine métier

- **Droit du travail** : non concerné (couvert par F-136). Aucune modification du code travail.
- **Droit de la famille** : couvert ici pour la Belgique (3 procédures). FR à couvrir dans SF jumelle.
- **Droit de l'immigration** : non concerné (couvert par `IMMIGRATION_JALONS`).

---

## Parité des domaines métier

Cette SF ne livre **pas** d'outil de scoring (niveau 5), comparateur (niveau 6) ou détection d'événement (niveau 7) — c'est un référentiel statique de jalons (niveau 1, infrastructure). La règle "Parité des domaines métier" s'applique néanmoins par symétrie volontaire : les 3 domaines disposeront d'un référentiel `*_PROCEDURE_JALONS` (immigration via `IMMIGRATION_JALONS`, travail via `TRAVAIL_PROCEDURE_JALONS`, famille via `FAMILLE_PROCEDURE_JALONS` introduit ici).

---

## Critères d'acceptation

- [ ] Classe Java `FamilleProcedureReferentiel` créée, miroir strict de `TravailProcedureReferentiel` (record `ProcedureJalon(label, offsetDays, articleRef)` + Map → liste + méthode `resolve(type, country)`).
- [ ] 3 procédures famille BE seedées : `TRIBUNAL_FAMILLE_BE` (5 jalons : dépôt requête, audience MP, mise en état, plaidoiries fond, prononcé), `COUR_APPEL_FAMILLE_BE` (4 jalons : délai appel 1 mois art. 1051 CJ, conclusions, audience, arrêt), `CASSATION_FAMILLE_BE` (4 jalons : délai pourvoi 3 mois art. 1073 CJ, mémoire, audience, arrêt).
- [ ] Migration Liquibase `162-seed-famille-be-procedure-jalons.xml` créée avec INSERTs pour les 3 entries (UUID prefix dédié `f1620000-…`), description SF-140-03 obligatoire en langage avocat pour chaque entry.
- [ ] Méthode `LegalReferentialService.getFamilleProcedureJalons(typeProcedure, country)` ajoutée — DB-first sur `FAMILLE_PROCEDURE_JALONS`, fallback Java sur `FamilleProcedureReferentiel.resolve(...)`.
- [ ] Méthode `resolve` retourne `List.of()` pour `typeProcedure` ou `country` null/inconnu (pas d'exception).
- [ ] Tous les jalons retournés ont un `articleRef` non vide citant un article du Code judiciaire BE (CJ) ou Code civil BE (Cciv).
- [ ] `LegalReferentialDescriptionIntegrityIT` passe (description renseignée pour chaque entry).
- [ ] Pas d'endpoint HTTP exposé dans cette SF (couvert ailleurs).
- [ ] Pas de modification du code F-136 / `TravailProcedureReferentiel` / `ImmigrationProcedureReferentiel` (purement additif).

---

## Périmètre

### Hors scope (explicite)

- Calendrier procédural famille **France** (JAF, audience MEC, ONC, appel CA, Cassation) — SF-F-137-02 jumelle à créer.
- Endpoint REST HTTP — SF-F-137-03 (le brief précise qu'on peut décider en lisant F-136 ; F-136 a séparé `SF-136-01` backend référentiel et `SF-136-02` endpoint, on suit le même découpage).
- Composant frontend Angular timeline famille — SF-F-137-04.
- Génération automatique de `case_deadlines` à partir des jalons — non décidé (cohérent avec F-136 qui a aussi reporté ce sujet).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `legal_domain` | `DROIT_FAMILLE` | Imposé par la migration |
| `referential_type` | `FAMILLE_PROCEDURE_JALONS` | Nouveau type, miroir de `TRAVAIL_PROCEDURE_JALONS` |
| `country` | `BE` | Périmètre brief |
| `is_system` | `true` | Référentiel canonique éditable uniquement par les opérateurs |
| `is_active` | `true` | |
| `workspace_id` | `NULL` | Référentiel système, partagé |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| `typeProcedure` | Oui (sinon `List.of()`) | — | `TRIBUNAL_FAMILLE_BE` / `COUR_APPEL_FAMILLE_BE` / `CASSATION_FAMILLE_BE` | n/a (lecture) | trim().toUpperCase() côté `resolve` |
| `country` | Oui (sinon `List.of()`) | — | `BE` (cette SF) | n/a | trim().toUpperCase() |

---

## Technique

### Endpoint(s)

Aucun — SF backend pure (référentiel + service de lecture).

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `legal_referentials` | INSERT | 3 entries `FAMILLE_PROCEDURE_JALONS` (BE), via migration 162. |

### Migration Liquibase

- [x] Oui — `162-seed-famille-be-procedure-jalons.xml`. UUID prefix `f1620000-…` (pas de collision avec `f1130000-…` F-136 ni autres migrations).

### Composants Angular (si applicable)

Aucun.

---

## Plan de test

### Tests unitaires

- [x] `FamilleProcedureReferentielTest.tribunalFamilleBe_returns5Jalons_avecDelaisOrdonnes` — récupération + ordre + offset days.
- [x] `FamilleProcedureReferentielTest.tribunalFamilleBe_audienceMP_jalonPresent` — vérifie que le jalon "audience MP" est présent et cite un article CJ.
- [x] `FamilleProcedureReferentielTest.courAppelFamilleBe_delaiAppel30Jours_art1051Cj` — délai appel 1 mois.
- [x] `FamilleProcedureReferentielTest.cassationFamilleBe_pourvoi90Jours_art1073Cj` — délai pourvoi 3 mois.
- [x] `FamilleProcedureReferentielTest.unknownType_returnsEmptyList` — robustesse.
- [x] `FamilleProcedureReferentielTest.nullType_returnsEmptyList`, `nullCountry_returnsEmptyList`.
- [x] `FamilleProcedureReferentielTest.mismatchCountryFr_returnsEmptyList` — `BE` types refusés avec `country=FR`.
- [x] `FamilleProcedureReferentielTest.unknownCountry_returnsEmptyList` — `country=DE`.
- [x] `FamilleProcedureReferentielTest.allBeTypesHaveCjArticleRef` — cohérence transversale (chaque jalon cite CJ).
- [x] `FamilleProcedureReferentielTest.allTypesReturnImmutableList` — immutabilité.
- [x] `FamilleProcedureReferentielTest.countryNormalizedToUpperCase` — accepte `be`.

(≥ 8 tests unitaires comme demandé par le brief — on en livre 11.)

### Tests d'intégration

Pas d'endpoint HTTP créé dans cette SF, donc pas de `*ControllerIT` famille à ajouter ici. La vérification d'intégrité DB est couverte par :
- [x] `LegalReferentialDescriptionIntegrityIT` — vérifie automatiquement que chaque entry `is_system=true` a une `description` non vide (passe dès l'écriture de la migration 162 conforme).

### Isolation workspace

- [x] Non applicable — référentiel système (`workspace_id IS NULL`), lecture par tous les workspaces.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée :
  - Aucun `@AuthenticationPrincipal` modifié.
  - Aucun changement workspace context.
  - Aucun changement plans / limites.
  - Aucune route Angular touchée.
  - **Outil décisionnel métier** : non — c'est un référentiel statique, pas un outil décisionnel. Les outils décisionnels famille BE pourront le consommer, mais cette SF ne crée pas d'outil au sens "calculator / analyzer / generator / decision engine".

### Composants / endpoints existants potentiellement impactés

Aucun — purement additif. `LegalReferentialService` reçoit une nouvelle méthode publique sans modifier les signatures existantes.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné (justification : SF backend pure sans endpoint HTTP ni route Angular ; rien à smoke-tester end-to-end).

---

## Dépendances

### Subfeatures bloquantes

- F-136 SF-136-01 (`TravailProcedureReferentiel`) — `done` (mergée). Source du pattern.
- F-140 SF-140-03 (description obligatoire `legal_referentials`) — `done`. Garde-fou test `LegalReferentialDescriptionIntegrityIT`.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Choix du nom de classe** : `FamilleProcedureReferentiel` (analogue à `TravailProcedureReferentiel`), pas `FamilleProcedureReferentielBe` même si périmètre BE — la classe accepte un paramètre `country`, prête à recevoir les types FR en SF-F-137-02 sans renommage.
- **Nommage des codes BE** : suffixe `_BE` strict (`TRIBUNAL_FAMILLE_BE`, `COUR_APPEL_FAMILLE_BE`, `CASSATION_FAMILLE_BE`) — analogue à `TRIBUNAL_TRAVAIL_BE` / `COUR_TRAVAIL_BE` / `CASSATION_BE` (note : F-136 a utilisé `CASSATION_BE` sans le préfixe `TRAVAIL` car c'était le seul à l'époque ; pour la famille on garde `CASSATION_FAMILLE_BE` afin d'éviter une collision conceptuelle si d'autres domaines BE ajoutent une cassation).
- **UUID prefix migration** : `f1620000-0000-0000-0000-00000000000X` (pas de collision avec `f1130000-…` F-136 ni autres prefix observés).
- **Articles légaux référencés** :
  - Tribunal de la famille BE : art. 572bis CJ (compétence), art. 1253ter/2 CJ (audience MP), art. 747 CJ (mise en état), art. 770 CJ (jugement).
  - Cour d'appel famille BE : art. 1051 CJ (délai appel 1 mois), art. 1056 CJ (audience).
  - Cassation BE : art. 1073 CJ (délai pourvoi 3 mois), art. 1080/1095 CJ (mémoire/audience).
- **Délais indicatifs vs stricts** : seul délai d'appel (1 mois) et délai de pourvoi (3 mois) sont **stricts** — les autres délais (audience MP, mise en état, fond, prononcé) sont **indicatifs** (pratique greffe). Documenté dans la `description` de chaque entry comme F-136.
