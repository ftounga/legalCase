# Mini-spec — F-213 / SF-213-01-backend Outil clause non-concurrence BE — analyseur validité + calculateur indemnité

## Identifiant

`F-213 / SF-213-01-backend`

## Feature parente

`F-213` — P2 Travail BE — ~10 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-213-01-backend-clause-non-concurrence-be`

---

## Objectif

Analyser la validité d'une clause de non-concurrence selon le droit belge du travail (**Loi 03/07/1978 art. 65 ; CCT 13 du 24/02/1971 modifiée**) et calculer l'indemnité légale (= ½ rémunération brute × durée de la clause). **BELGIQUE UNIQUEMENT** — régime distinct du FR (FR : montant libre ; BE : indemnité légale obligatoire = ½ salaire).

---

## Source juridique BE

- **Loi du 3 juillet 1978** relative aux contrats de travail, **art. 65** : conditions de validité de la clause.
- **CCT n° 13 du 24 février 1971** (modifiée) concernant les clauses de non-concurrence : seuil de salaire, durée maximale, zone géographique, indemnité légale.
- **Seuils 2024** (à vérifier par avocat BE) : clause valide si rémunération annuelle brute > 73 571 € (indexé annuellement). En deçà : clause nulle de plein droit sauf convention sectorielle plus favorable.
- **Indemnité légale** : ½ rémunération brute correspondant à la durée de la clause (ex. clause 6 mois → indemnité = 3 mois de rémunération brute). Payable dans les 15 jours de la fin du contrat.
- **Durée maximale** : 12 mois. Zone géographique : Belgique uniquement (clause incluant l'étranger = partiellement nulle sauf activité internationale prouvée).
- **Exception sectorielles** : certaines CCT sectorielles prévoient des régimes spécifiques (ex. CP218, CP200) — à vérifier.

---

## Comportement attendu

### Cas nominal

`POST /api/v1/case-files/{caseFileId}/decision-tools/clause-non-concurrence-be`

Inputs (body) :
- `remunerationAnnuelleBrute` (BigDecimal, €) — obligatoire.
- `dureeMois` (int, 1-12) — durée de la clause en mois, obligatoire.
- `zoneGeographique` (enum) — `BELGIQUE_UNIQUEMENT` | `BELGIQUE_ET_ETRANGER` | `NON_SPECIFIEE`.
- `activiteInternationaleProuvee` (boolean, défaut false) — atténue la nullité si zone internationale.
- `salaireAnnuelSeuil` (BigDecimal, optionnel) — seuil d'application en vigueur (défaut : 73 571 €/2024).

Logique (`ClauseNonConcurrenceBeValidator`) :

| Condition | Verdict | Raison |
|---|---|---|
| `remunerationAnnuelleBrute <= salaireAnnuelSeuil` | `NULLE` | Loi 03/07/1978 art. 65 — clause nulle si rémunération insuffisante |
| `dureeMois > 12` | `NULLE` | Durée maximale 12 mois dépassée |
| `zoneGeographique = BELGIQUE_ET_ETRANGER` && !`activiteInternationaleProuvee` | `PARTIELLEMENT_NULLE` | Zone internationale non justifiée — nulle pour la partie étrangère |
| Toutes conditions OK | `VALIDE` | Clause valide |

Calcul indemnité légale :
- `indemniteLegale = (remunerationAnnuelleBrute / 12) * (dureeMois / 2)`
- Arrondi à 2 décimales.
- N'est calculée que si verdict ≠ `NULLE` (ou pour simuler le coût en cas de litige).

Output (`ClauseNonConcurrenceBeResponse`) :
```json
{
  "verdict": "VALIDE" | "NULLE" | "PARTIELLEMENT_NULLE",
  "raisonNullite": null | "REMUNERATION_INSUFFISANTE" | "DUREE_EXCESSIVE" | "ZONE_GEOGRAPHIQUE_NON_JUSTIFIEE",
  "indemniteLegale": 3500.00,
  "indemniteLegaleFormule": "(73571 € / 12) * (6 / 2) = 3678,55 €",
  "baseJuridique": "Loi 03/07/1978 art. 65 ; CCT n°13 du 24/02/1971",
  "avertissement": "Seuil 2024 : 73 571 € — à vérifier selon année de signature du contrat."
}
```

Persistance : table `clause_non_concurrence_be_analyses` — 1 ligne par dossier (unique sur `case_file_id`, mise à jour à chaque POST). Inputs + verdict persistés en JSON (`result_data`).

`GET` du même path renvoie la dernière analyse ou 404.

### Cas d'erreur

| Situation | Code | Comportement |
|---|---|---|
| `workspaceCountry !== 'BELGIQUE'` | 404 | Isolation BE-only |
| `caseFileId` hors workspace | 404 | Isolation workspace standard |
| `remunerationAnnuelleBrute` ≤ 0 | 400 | « Rémunération invalide » |
| `dureeMois` hors [1, 12] | 400 | « Durée invalide (1-12 mois) » |
| `zoneGeographique` manquant | 400 | « zoneGeographique obligatoire » |

---

## Champs IA à extraire — BELGIQUE UNIQUEMENT

Extension `TravailExtractedData` (branche `country=BELGIUM` de `LegalDomainPromptBuilder`) :

| Champ formulaire | Type | Champ `TravailExtractedData` BE (annoté BELGIQUE UNIQUEMENT) | Notes |
|---|---|---|---|
| `remunerationAnnuelleBrute` | BigDecimal | `salaireBrutAnnuel` (existant ou à ajouter) | Priorité : fiche de paie ou contrat |
| `dureeMois` | int | `clauseNonConcurrenceDureeMois` — **BELGIQUE UNIQUEMENT** | Extrait de la clause dans le contrat |
| `zoneGeographique` | enum | `clauseNonConcurrenceZone` — **BELGIQUE UNIQUEMENT** | Extrait texte de la clause |
| `activiteInternationaleProuvee` | boolean | dérivé de `domaineActiviteInternational` — **BELGIQUE UNIQUEMENT** | Défaut false |

`critereCode` émis : `BE_CLAUSE_NC_REMUNERATION`, `BE_CLAUSE_NC_DUREE`, `BE_CLAUSE_NC_ZONE`.

---

## Critères d'acceptation

- [ ] `POST` retourne `NULLE` + `REMUNERATION_INSUFFISANTE` si rémunération ≤ 73 571 €.
- [ ] `POST` retourne `NULLE` + `DUREE_EXCESSIVE` si dureeMois > 12.
- [ ] `POST` retourne `PARTIELLEMENT_NULLE` si zone internationale sans activité prouvée.
- [ ] `POST` retourne `VALIDE` + `indemniteLegale` calculé pour une clause valide.
- [ ] Formule affichée en clair dans `indemniteLegaleFormule`.
- [ ] `POST` workspace France → 404.
- [ ] `GET` renvoie dernière analyse ou 404.
- [ ] `CritereCodeIntegrityIT` reste vert (nouveaux codes BE_CLAUSE_NC_*).

---

## Périmètre

### Hors scope

- Frontend (`clause-non-concurrence-be-section.component`) — SF-213-01b.
- CCT sectorielles spécifiques (CP200, CP218 etc.) — validité générale couverte ; barèmes sectoriels = P4.
- Clause de non-concurrence pour **indépendants** (Loi 27/12/2006 — autre régime).
- Droit applicable en cas de contrat avec clause de choix de loi étranger.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/decision-tools/clause-non-concurrence-be` | OIDC | LAWYER |
| GET  | `/api/v1/case-files/{caseFileId}/decision-tools/clause-non-concurrence-be` | OIDC | MEMBER |

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `clause_non_concurrence_be_analyses` | INSERT / UPDATE / SELECT | Unique sur `case_file_id`. Colonnes : `id` UUID, `case_file_id` FK CASCADE, `result_data` TEXT JSON, `created_at`, `updated_at`. |

### Migration Liquibase

`XXX-create-clause-non-concurrence-be-analyses.xml` — reversible (`<rollback><dropTable /></rollback>`).

### Composants backend

- `ClauseNonConcurrenceBeAnalysis.java` — entité JPA
- `ClauseNonConcurrenceBeRepository.java`
- `ClauseNonConcurrenceBeZoneEnum.java` — enum 3 valeurs
- `ClauseNonConcurrenceBeRequest.java` — DTO POST
- `ClauseNonConcurrenceBeResult.java` — record verdict
- `ClauseNonConcurrenceBeResponse.java` — DTO GET
- `ClauseNonConcurrenceBeService.java`
- `ClauseNonConcurrenceBeValidator.java` — fonction pure
- `ClauseNonConcurrenceBeController.java`
- Extension `TravailExtractedData` + `LegalDomainPromptBuilder` branche BE

---

## Plan de test

### Unitaires (`ClauseNonConcurrenceBeValidatorTest`)

- [ ] Rémunération 73 000 € → `NULLE` / `REMUNERATION_INSUFFISANTE`.
- [ ] Rémunération 100 000 €, durée 6 mois, Belgique uniquement → `VALIDE`, indemniteLegale = 25 000 €.
- [ ] Durée 13 mois → `NULLE` / `DUREE_EXCESSIVE`.
- [ ] Zone internationale sans activité prouvée → `PARTIELLEMENT_NULLE`.
- [ ] Zone internationale avec activité prouvée → `VALIDE`.
- [ ] Formule indemnité correcte (arrondi 2 décimales).

### Intégration (`ClauseNonConcurrenceBeControllerIT`)

- [ ] `POST` workspace BE → 200 + persistance.
- [ ] `POST` workspace FR → 404.
- [ ] `POST` `caseFileId` autre workspace → 404.
- [ ] `GET` après POST → 200.
- [ ] `GET` sans POST → 404.
- [ ] Validation Bean : `remunerationAnnuelleBrute` négatif → 400.

### Isolation workspace

- [x] Applicable — standard.

---

## Analyse d'impact

### Préoccupations transversales

- [x] **Workspace context** — gate `workspaceCountry=BELGIQUE` strict.
- [x] **Outil décisionnel métier** — création d'un outil, invariant un outil = une situation métier respecté (≠ F-DT-24 FR qui est la version française).
- Auth / Plans / Navigation — non touchés.

### Composants impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `LegalDomainPromptBuilder` branche BE | Ajout `critereCode BE_CLAUSE_NC_*` + champs extractés | `LegalDomainPromptBuilderTest` BE |
| `TravailExtractedData` | Ajout champs `clauseNonConcurrenceDureeMois`, `clauseNonConcurrenceZone` | `CaseAnalysisResponseTest` rétrocompatibilité |
| `CritereCodeIntegrityIT` | Nouveaux codes → `KNOWN_FRONTEND_CRITERE_CODES` | MAJ frontend dans SF-213-01b |

---

## Dépendances

- Aucune SF F-213 bloquante. Peut démarrer indépendamment.
