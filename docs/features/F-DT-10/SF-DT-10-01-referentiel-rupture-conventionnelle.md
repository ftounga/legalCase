# Mini-spec — F-DT-10 / SF-DT-10-01 Référentiel de critères + analyseur de validité de la rupture conventionnelle

## Identifiant

`F-DT-10 / SF-DT-10-01`

## Feature parente

`F-DT-10` — Analyse de validité de la rupture conventionnelle

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-DT-10-01-referentiel-rupture-conventionnelle`

---

## Objectif

Fournir le socle backend pour analyser la validité d'une rupture conventionnelle individuelle française : référentiel statique Java des critères juridiques, analyseur de scoring 0-100 avec verdict, entité JPA + migration Liquibase pour persister le résultat par dossier. Structure miroir de F-DT-08 (licenciement) pour garder une cohérence de code.

Scope V2 initial : France uniquement (art. L1237-11 et suivants du Code du travail). Belgique (rupture amiable) hors périmètre — cadre juridique très différent, peu de contraintes légales, traité dans une feature séparée si nécessité confirmée.

---

## Comportement attendu

### Cas nominal

1. `RuptureConvCritereReferentiel.getByCountry("FRANCE")` retourne la liste immuable des 6 critères FR.
2. `RuptureConvAnalyzer.analyze("FRANCE", reponses)` où `reponses` est une `Map<String, String>` code → "OUI" / "NON" / "INCONNU" retourne un `RuptureConvAnalysisResult` contenant : country, score 0-100, verdict ∈ {VALIDE, RISQUE_MODERE, RISQUE_ELEVE, INVALIDE}, liste d'évaluations par critère.
3. Un critère sans réponse dans la map est traité comme "INCONNU".
4. `reponses` peut contenir des clés non présentes dans le référentiel (ignorées silencieusement) — fail-open pour rétrocompat future.

### Critères FR (6)

| Code | Libellé | Poids | Bloquant | Base juridique |
|------|---------|-------|----------|----------------|
| `RC_CONSENTEMENT` | Consentement libre et éclairé | 25 | Oui | Art. L1237-11, 1er alinéa + Cass. soc. 29 janv. 2020, n° 18-24.558 |
| `RC_DELAI_RETRACTATION` | Délai de rétractation de 15 jours calendaires respecté | 20 | Oui | Art. L1237-13, 2ᵉ alinéa |
| `RC_HOMOLOGATION` | Homologation par la DREETS (ex-DIRECCTE) | 25 | Oui | Art. L1237-14 |
| `RC_ASSISTANCE` | Assistance possible et documentée (avocat / conseiller du salarié / représentant du personnel) | 10 | Non | Art. L1237-12, dernier alinéa |
| `RC_INDEMNITE` | Indemnité spécifique ≥ indemnité légale de licenciement | 15 | Oui | Art. L1237-13, 1er alinéa |
| `RC_ENTRETIENS` | Au moins un entretien préalable tenu et documenté | 5 | Non | Art. L1237-12, 1er alinéa |

Somme des poids = 100 → le score est calculé directement sans normalisation.

### Règles de scoring

- Réponse `OUI` (conforme) → 0 point ajouté au risque.
- Réponse `NON` (non-conforme) → poids complet ajouté au risque.
- Réponse `INCONNU` (à vérifier) → poids ÷ 2 ajouté au risque.
- Score final = `min(100, totalRisque)` (puisque somme des poids = 100, l'addition donne déjà un score 0-100).

### Règles de verdict

| Condition | Verdict |
|-----------|---------|
| Au moins un critère bloquant répondu `NON` | `INVALIDE` |
| Score ≥ 70 sans bloquant en NON | `INVALIDE` (risque majeur d'annulation par le CPH) |
| 40 ≤ score < 70 | `RISQUE_ELEVE` |
| 15 ≤ score < 40 | `RISQUE_MODERE` |
| Score < 15 | `VALIDE` |

### Persistance

- Table `rupture_conv_analyses` (migration 072).
- Unicité stricte 1:1 par `case_file_id` : une seule analyse par dossier (mise à jour en place).
- Colonnes : `id uuid PK`, `case_file_id uuid FK case_files NOT NULL UNIQUE`, `country varchar(20) NOT NULL`, `reponses_data text NOT NULL` (JSON), `result_data text NOT NULL` (JSON), `created_at / updated_at timestamptz NOT NULL`.
- Index sur `case_file_id`.

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| `country` ≠ "FRANCE" | `IllegalArgumentException("Pays non supporté")` — Belgique hors scope SF-DT-10-01 |
| `country` null | `IllegalArgumentException` |
| `reponses` null | Traité comme map vide → tous critères INCONNU |
| Réponse non reconnue (ex. "peut-être") | Traitée comme "INCONNU" (fail-open) |
| Critère dans `reponses` absent du référentiel | Ignoré silencieusement |

---

## Critères d'acceptation

- [ ] `RuptureConvCritere` record créé (miroir `LicenciementCritere`).
- [ ] `RuptureConvCritereReferentiel` avec les 6 critères FR, méthodes `getByCountry`, `getByCode`, `getAll`, `isCountryValid` (FRANCE uniquement pour l'instant, mais l'API est ouverte).
- [ ] Somme des poids FR = 100 (assertion testée).
- [ ] `RuptureConvAnalysisResult` record (country, score, verdict, List<CritereEvaluation>) + constantes VALIDE/RISQUE_MODERE/RISQUE_ELEVE/INVALIDE.
- [ ] `RuptureConvAnalyzer` avec `analyze(country, reponses)` et overload testable `analyze(country, reponses, criteres)`.
- [ ] Entité JPA `RuptureConvAnalysis` avec mapping table `rupture_conv_analyses`, stockage JSON via `text` (même pattern que F-DT-08).
- [ ] Repository Spring Data `RuptureConvAnalysisRepository` avec `findByCaseFileId(UUID)`.
- [ ] Migration `072-create-rupture-conv-analyses.xml` appliquée et réversible.
- [ ] Tests unitaires : référentiel (6 critères, poids somme 100), analyzer (verdicts pour chaque seuil, bloquant OUI/NON/INCONNU, pays invalide, reponses null).
- [ ] Aucun endpoint ajouté dans cette subfeature (sera SF-DT-10-02).
- [ ] Aucun composant frontend ajouté dans cette subfeature (sera SF-DT-10-03).

---

## Périmètre

### Hors scope (explicite)

- Endpoint REST (→ SF-DT-10-02).
- Composant Angular (→ SF-DT-10-03).
- Orchestration UX (masquage F-DT-08 selon type_rupture) (→ SF-DT-10-04).
- Belgique — rupture amiable belge est un cadre juridique distinct, couvrable séparément.
- Pré-remplissage depuis l'IA (extraction automatique des critères depuis les documents) — l'avocat remplit manuellement en V2 initiale, extraction IA à évaluer après validation de la feature de base.
- Recalcul automatique de l'indemnité légale dans `RC_INDEMNITE` — l'avocat évalue lui-même ; le calcul automatique peut venir plus tard (lien avec F-DT-09 envisageable).
- Export PDF / génération de note de synthèse — hors périmètre V2 initial.
- Multi-ruptures ou historique — 1:1 strict par dossier comme F-DT-08.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `rupture_conv_analyses.reponses_data` | JSON `{}` si insertion sans réponse | défini par la requête |
| `rupture_conv_analyses.result_data` | JSON contenant score 0 + verdict `VALIDE` si aucune réponse | calculé par l'analyzer |

---

## Contraintes de validation

| Champ | Obligatoire | Valeurs autorisées | Normalisation |
|-------|-------------|-------------------|--------------|
| `country` | Oui | `FRANCE` uniquement en SF-DT-10-01 | upper-case avant comparaison |
| code critère dans map | Non | parmi 6 codes FR ; autres ignorés | upper-case avant comparaison |
| réponse à un critère | Non | `OUI`, `NON`, `INCONNU` ; autres → `INCONNU` | upper-case + trim |

---

## Technique

### Endpoints

Aucun.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `rupture_conv_analyses` | CREATE | migration 072, 1:1 avec case_files |

### Migration Liquibase

- [x] Oui — `072-create-rupture-conv-analyses.xml`, réversible (`DROP TABLE`).

### Composants backend

- `fr.ailegalcase.casefile.RuptureConvCritere` (record).
- `fr.ailegalcase.casefile.RuptureConvCritereReferentiel` (final class).
- `fr.ailegalcase.casefile.RuptureConvAnalysisResult` (record) + constantes.
- `fr.ailegalcase.casefile.RuptureConvAnalyzer` (final class).
- `fr.ailegalcase.casefile.RuptureConvAnalysis` (entité JPA).
- `fr.ailegalcase.casefile.RuptureConvAnalysisRepository` (interface).

### Composants frontend

Aucun.

---

## Plan de test

### Tests unitaires backend

- [ ] `RuptureConvCritereReferentielTest` : 6 critères FR, somme poids = 100, `getByCode` trouve + `getByCode` inexistant → null, `isCountryValid` FR/BE/FOO.
- [ ] `RuptureConvAnalyzerTest` : tous OUI → score 0, verdict VALIDE.
- [ ] `RuptureConvAnalyzerTest` : tous NON → score 100, verdict INVALIDE.
- [ ] `RuptureConvAnalyzerTest` : tous INCONNU → score 50 (somme poids/2), verdict RISQUE_ELEVE.
- [ ] `RuptureConvAnalyzerTest` : un bloquant NON, reste OUI → verdict INVALIDE (même si score < 70).
- [ ] `RuptureConvAnalyzerTest` : tous OUI sauf RC_ASSISTANCE (non-bloquant) = NON → score 10, verdict VALIDE (score < 15).
- [ ] `RuptureConvAnalyzerTest` : pays non supporté (ex. BELGIQUE) → `IllegalArgumentException`.
- [ ] `RuptureConvAnalyzerTest` : reponses null → tous INCONNU → score 50.
- [ ] `RuptureConvAnalyzerTest` : réponse non reconnue ("peut-être") → traitée comme INCONNU.
- [ ] `RuptureConvAnalyzerTest` : critère dans reponses absent du référentiel → ignoré sans erreur.

### Tests d'intégration

- [ ] Aucun à ce stade — pas d'endpoint. Les repository tests arriveront via SF-DT-10-02.

### Isolation workspace

- [x] Préservée via `case_file_id` — pas d'accès direct au workspace dans ce layer.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune** — layer modèle/service pur, aucune route, aucun endpoint.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|-----------|--------|-----------------------|
| `licenciement_analyses` (F-DT-08) | Aucun — table distincte | Tests F-DT-08 existants |
| Schéma global | Ajout d'une table isolée | Migration Liquibase appliquée en local + verify |

### Smoke tests E2E concernés

- [ ] Aucun — couche backend isolée.

---

## Dépendances

### Subfeatures bloquantes

- Aucune. `F-DT-08` sert de modèle mais n'est pas une dépendance de code.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi une table distincte et pas une colonne `type_analyse` dans `licenciement_analyses`** : cadre juridique différent, critères différents, base juridique différente, verdicts différents. Fusionner créerait plus de complexité qu'elle n'en simplifie. Le coût d'une table dédiée est négligeable.
- **Pourquoi seulement France** : la rupture amiable belge (cadre Loi du 3 juillet 1978 + négociation privée) n'a presque aucune contrainte légale — l'analogue V2 n'a pas la même valeur métier. À cibler séparément si la demande apparaît.
- **Pourquoi 6 critères et pas plus** : ce sont les 6 conditions **substantielles** de validité de l'art. L1237-11 s. ; le reste (forme rédactionnelle, mentions obligatoires, signature) est inclus implicitement dans `RC_CONSENTEMENT` et `RC_HOMOLOGATION`. Un élargissement est possible plus tard sans casser l'API.
- **Pourquoi somme poids = 100** : évite la division de normalisation, garde le code proche de la mini-spec.
- **Pourquoi pas de pré-remplissage IA dans cette subfeature** : mieux séparer la pose du socle de l'enrichissement IA. Si validation métier positive, un SF-DT-10-0X ajouterait une extraction spécifique (délai entre signature et DIRECCTE depuis les pièces, montant de l'indemnité depuis la convention).
- **Pourquoi pas d'auto-calcul `RC_INDEMNITE`** : éviter les couplages entre outils dans la première passe. F-DT-09 calcule l'indemnité légale ; F-DT-10 peut demander à l'avocat si elle est respectée. Fusion envisageable plus tard.
