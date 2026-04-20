# Mini-spec — F-132 / SF-132-01 Backend outil "Indemnité rupture conventionnelle" (FR)

## Identifiant
`F-132 / SF-132-01`

## Feature parente
`F-132` — Refonte F-DT-09 en outils décisionnels dédiés

## Statut `draft`  · Date `2026-04-20`  · Branche `feat/SF-132-01-rupture-conv-indemnite-backend`

---

## Objectif

Extraire la situation "rupture conventionnelle France" de `IndemniteComparatifCalculator` en un outil décisionnel dédié côté backend (calculator + service + endpoint + entity + migration), sur le pattern F-DT-10 "Validité rupture conventionnelle". Cette SF ne touche **pas** le frontend ni l'ancien calculateur — elle pose la brique backend. SF-132-02 consommera le nouvel endpoint côté UI et retirera la branche rupture conv de l'ancien calculateur.

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/rupture-conv-indemnite` avec `{ ancienneteAnnees, salaireMensuel }` :
1. Résout le `caseFile` et vérifie appartenance workspace (pattern existant `IndemniteComparatifService`)
2. Vérifie que `caseFile.legalDomain == DROIT_DU_TRAVAIL` et `country == FRANCE`
3. Calcule le minimum légal via `RuptureConvIndemniteCalculator.computeMinimumLegal(anciennete, salaire)` — formule art. R1234-2 : `¼ × min(10, anciennete) + ⅓ × max(0, anciennete - 10)` multiplié par le salaire
4. Persiste le résultat dans une entity `RuptureConvIndemniteAnalysis` (upsert 1:1 sur `caseFileId`)
5. Retourne `RuptureConvIndemniteResponse` avec `indemniteLegaleMinimum`, `formule`, `baseJuridique`, `messages`

`GET /api/v1/case-files/{caseFileId}/rupture-conv-indemnite` : lecture de la dernière analyse persistée pour ce dossier.

### Cas d'erreur

| Situation | Comportement | Code |
|---|---|---|
| `caseFile` inexistant ou autre workspace | `Case file not found` | 404 |
| `legalDomain ≠ DROIT_DU_TRAVAIL` | `Ce dossier n'est pas un dossier de droit du travail` | 400 |
| `ancienneteAnnees` null ou négative | `Ancienneté requise et positive` | 400 |
| `salaireMensuel` null, négatif ou zéro | `Salaire mensuel requis et positif` | 400 |
| Aucune analyse persistée (GET) | `Aucune analyse trouvée pour ce dossier` | 404 |

---

## Analyse de cohérence transversale

### Périmètres scannés

| Cible | Applicable ? | Traitement |
|---|---|---|
| Autres outils décisionnels (scan F-132 initial) | Oui — `IndemniteComparatifCalculator` FR/BE, `RecoursGenerator` | F-132 parent traite FR rupture conv (cette SF) et FR licenciement (SF-132-02 cleanup), SF-132-03 pour BE ; F-IM-06 `RecoursGenerator` traité par F-133 |
| Autres pays (Belgique — CCT 109 vs rupture amiable) | Oui | **SF-132-03** (même pattern appliqué à BE) |
| Autres domaines (immigration, famille) | Non applicable | Scan F-132 conclut : F-IM-05/06/07, F-FA-05/06/07 OK ou cas limite acceptable |
| Cohérence IA (F-IA-03) | Oui — indemnité calculée vs réponse avocat éventuelle | Cette SF **ne crée pas de réponses avocat saisies librement** (calcul déterministe à partir de 2 inputs). Pas d'intégration F-IA-03 nécessaire. Note ajoutée |
| Refresh dashboard (F-IA-02) | Oui — la card "Indemnités estimées" consomme F-DT-09 | La card dashboard continue d'afficher le résultat du Macron (F-DT-09). Le nouvel endpoint est consommé en SF-132-02 ; à ce moment, injection `CaseDashboardRefreshService.triggerRefresh()` côté frontend |
| Pré-remplissage IA | Oui — `compensation_data.ancienneteAnnees` et `compensation_data.salaireBrutMensuel` | **Intégré** : la Request accepte ces 2 champs ; pré-remplissage côté frontend viendra en SF-132-02 via le pattern existant (`prefillFromAi`) |
| Persistance inputs | Oui | **Intégré** : colonnes dédiées `anciennete_annees`, `salaire_mensuel` dans la nouvelle table |
| Masquage conditionnel selon type | Oui — outil à afficher uniquement quand `type_rupture == RUPTURE_CONVENTIONNELLE` | **Traité en SF-132-02** (frontend) ; cette SF backend reste inerte tant que personne n'appelle l'endpoint |
| Nouveau pattern UI / service partagé | Non — on applique un pattern existant (F-DT-10) et on reste isolé à un dossier FR rupture conv. Pas de composant/directive réutilisable introduit | Non applicable |

### Décision

- [x] Étendu aux cibles applicables dans cette SF (persistance inputs, intégration du pré-remplissage via Request) 
- [x] SFs parallèles créées pour les cibles restantes : SF-132-02 (frontend + retrait branche rupture conv de l'ancien calc), SF-132-03 (Belgique), F-133 (RecoursGenerator)
- [x] Non applicable aux domaines immigration / famille (scan F-132 explicite)

---

## Critères d'acceptation

- [ ] Nouveau calculateur `RuptureConvIndemniteCalculator` avec méthode `computeMinimumLegal(int anciennete, BigDecimal salaire)` retournant un record `RuptureConvIndemniteResult` contenant `indemniteLegaleMinimum`, `formule` (texte explicatif ex. `"¼ × 4 ans × 2 979 €"`), `baseJuridique` (`"Art. R1234-2 Code du travail"`), `messages` (liste : rappel L1237-13, vérifier convention collective si plus favorable)
- [ ] Pour `anciennete < 1` ou `salaire <= 0` : retour `indemniteLegaleMinimum = 0` avec un message explicite (seuil 1 an conforme à l'approximation du calc actuel `IndemniteComparatifCalculator.computeIndemniteLegaleLicenciement`)
- [ ] Formule appliquée identique à `IndemniteComparatifCalculator.computeIndemniteLegaleLicenciement` : `¼ × min(10, ancienneteAnnees) × salaire + ⅓ × max(0, ancienneteAnnees − 10) × salaire`, arrondi `HALF_UP` à 2 décimales
- [ ] Nouvelle entity JPA `RuptureConvIndemniteAnalysis` 1:1 sur `caseFileId`, colonnes `anciennete_annees`, `salaire_mensuel`, `result_data` (JSON), `created_at`, `updated_at`
- [ ] Migration Liquibase `088-rupture-conv-indemnite-analysis.xml` créant la table avec FK sur `case_files(id)` et index unique sur `case_file_id`
- [ ] Service `RuptureConvIndemniteService` avec méthodes `calculate(caseFileId, request, user)` et `get(caseFileId, user)` — isolation workspace via `WorkspaceMemberRepository.findByUserAndPrimaryTrue` (pattern `AncienneteService`/`IndemniteComparatifService`)
- [ ] Controller `RuptureConvIndemniteController` exposant `POST` et `GET /api/v1/case-files/{caseFileId}/rupture-conv-indemnite`
- [ ] Service valide que le domaine du dossier = `DROIT_DU_TRAVAIL` (l'outil est implicitement FR-only — CaseFile n'ayant pas de colonne `country`, le masquage FR se fait côté frontend en SF-132-02, pattern F-DT-10)
- [ ] Tous les 5 cas d'erreur listés ci-dessus retournent le bon code HTTP et le bon message
- [ ] Aucun changement dans `IndemniteComparatifCalculator`, `IndemniteComparatifService` ou leurs tests (cohabitation, cleanup en SF-132-02)

---

## Périmètre

### Hors scope

- Frontend : composant Angular, routing, masquage conditionnel F-DT-09 (**SF-132-02**)
- Retrait de la branche `RUPTURE_CONVENTIONNELLE` dans `IndemniteComparatifCalculator.calculateFrance()` et de `RUPTURE_CONVENTIONNELLE` dans `TYPES_RUPTURE_FR` (**SF-132-02** après bascule du consommateur)
- Dossier Belgique (CCT 109 vs rupture amiable) → **SF-132-03**
- Migration des `IndemniteComparatif` existants marqués rupture conv vers la nouvelle table (**SF-132-02** si nécessaire — étude à faire sur le volume)
- Integration F-IA-03 (pas de réponse avocat libre dans cet outil)

---

## Contraintes de validation

| Champ | Obligatoire | Format | Notes |
|---|---|---|---|
| `ancienneteAnnees` | Oui | entier ≥ 0 | Zéro accepté → indemnité 0 |
| `salaireMensuel` | Oui | `BigDecimal` > 0 | Rejeté si null ou ≤ 0 |
| `caseFileId` (path) | Oui | UUID valide | Isolation workspace obligatoire |

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/rupture-conv-indemnite` | Oui | MEMBER du workspace |
| GET | `/api/v1/case-files/{caseFileId}/rupture-conv-indemnite` | Oui | MEMBER du workspace |

### Tables impactées

| Table | Opération |
|---|---|
| `rupture_conv_indemnite_analysis` (nouvelle) | CREATE (migration 088), INSERT/UPDATE/SELECT |
| `case_files` | SELECT (FK existante) |

### Migration Liquibase

- [x] Oui — `088-rupture-conv-indemnite-analysis.xml`

---

## Plan de test

### Tests unitaires

- `RuptureConvIndemniteCalculatorTest` :
  - 0 an → indemnité 0
  - < 1 an (ex. 10 mois) → indemnité 0
  - 4 ans plein, salaire 2 979 € → 2 979 € (réplique du cas E28)
  - 10 ans pile → 10 × ¼ × salaire
  - 15 ans → 10 × ¼ × salaire + 5 × ⅓ × salaire
  - salaire null → `IllegalArgumentException`
  - salaire négatif ou zéro → retour 0 avec message
  - Messages retournés : L1237-13 et vérification convention collective présents

### Tests d'intégration

- `POST` avec payload valide → 200 + body conforme
- `POST` avec `ancienneteAnnees` null → 400
- `POST` avec `salaireMensuel` null → 400
- `POST` sur un `caseFile` autre workspace → 404
- `POST` sur un `caseFile` `legalDomain != DROIT_DU_TRAVAIL` → 400
- `GET` sans analyse préalable → 404
- `GET` après `POST` → renvoie l'analyse persistée
- Isolation workspace : user du workspace A ne peut pas lire celui de B → 404

### Context load

- `LegalcaseBackendApplicationTests.contextLoads` PASS avec migration 088

---

## Analyse d'impact

### Préoccupations transversales

- [ ] Auth / Principal : non touché
- [ ] Workspace context : réutilise `WorkspaceMemberRepository.findByUserAndPrimaryTrue` (pattern existant)
- [ ] Plans / limites : non touché (aucune gate ajoutée — pattern F-DT-07/F-DT-10 qui ne gatent pas)
- [ ] Navigation / routing : non touché (SF-132-01 backend pur)
- [x] **Outil décisionnel métier** : scan F-132 complet (10 outils) effectué, cibles jumelles tracées en SF-132-02, SF-132-03 et F-133. Pas de régression sur l'existant.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `IndemniteComparatifCalculator` / `Service` / `Controller` | **Aucun** dans cette SF (cohabitation) | Tests existants doivent rester verts |
| `CaseFileDashboardService.buildIndemniteSummary` | **Aucun** (continue de lire F-DT-09 jusqu'à SF-132-02) | Tests existants doivent rester verts |

### Smoke tests E2E

- Aucun — la SF est backend pure, aucun flow utilisateur touché. Vérifié via tests IT.

---

## Dépendances

### Subfeatures bloquantes

- Aucune (F-DT-09 en place, F-DT-10 en place comme pattern de référence)

### Questions ouvertes

- Aucune

---

## Notes et décisions

- **Pourquoi une table dédiée et non une colonne dans `case_analyses` ou `indemnite_comparatif_analysis`** : respect du pattern F-DT-07/F-DT-08/F-DT-10 — chaque outil décisionnel a son entity 1:1 par dossier. Permet GET indépendant, cohabitation propre avec `IndemniteComparatifAnalysis` pendant la transition, suppression simple si un jour l'outil est retiré.
- **Pourquoi pas de pré-remplissage IA dans cette SF** : le pré-remplissage se fait côté frontend via `prefillFromAi` à l'ouverture du formulaire — donc SF-132-02. Le backend expose juste l'endpoint, il n'initie pas le calcul automatiquement.
- **Pourquoi seuil 1 an** : alignement strict sur l'implémentation actuelle `IndemniteComparatifCalculator.computeIndemniteLegaleLicenciement` (ligne 141). Le seuil légal réel est 8 mois continus (art. L1234-9) — différence volontairement conservée pour ne pas introduire de divergence comportementale dans cette SF. Correction possible en feature séparée si besoin.
