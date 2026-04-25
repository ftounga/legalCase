# Mini-spec — F-DT-20 / SF-DT-20-01 Backend Calculateur Rappel de salaire FR

## Identifiant
`F-DT-20 / SF-DT-20-01`

## Feature parente
`F-DT-20` — Calculateur rappel de salaire FR (art. L.3242-1 + L.3245-1 + L.3141-26 Code du travail + CCN)

## Statut `draft` · Date `2026-04-25` · Branche `feat/SF-DT-20-01-rappel-salaire-backend`

---

## Objectif

Fournir un outil décisionnel dédié au calcul du rappel de salaire dû à un salarié, en application de l'art. L.3242-1 (paiement du salaire) et L.3245-1 (prescription triennale), incluant :
- la **revalorisation** du différentiel selon un index INSEE (taux saisi par l'avocat) ;
- l'application de la **prime d'ancienneté** prévue par la convention collective si applicable (consultée via `LegalReferentialService.getConventionBareme(...)`) ;
- les **congés payés sur rappel** (art. L.3141-26) selon trois méthodes possibles : règle du dixième, maintien du salaire, ou aucune (cas CDD spécifiques).

---

## Comportement attendu

### Cas nominal

**Calcul du différentiel mensuel et du rappel brut hors revalorisation :**
- `differentielMensuelEur = montantSalaireDuMensuelEur - montantSalairePerVerseMensuelEur`.
- `nbMoisPeriode` = nombre de mois entiers entre `periodeDebut` et `periodeFin` (inclus).
- `totalRappelBrutHorsRevalorisationEur = differentielMensuelEur × nbMoisPeriode`.

**Revalorisation INSEE :**
- Si `indexInseeRevalorise=true` : `montantRevalorisationEur = (tauxRevalorisationPct / 100) × totalRappelBrutHorsRevalorisationEur` (HALF_UP 2 décimales).
- Sinon : `0`.

**Prime d'ancienneté CCN :**
- Si `conventionCollectiveCode` fourni et présent en DB (`CONVENTION_BAREMES`) : applique la tranche `primesAnciennete` correspondant à `ancienneteAnneesPrime`.
- `primeAncienneteEur = totalRappelBrutHorsRevalorisationEur × pourcentage / 100` (HALF_UP).
- Si CCN inconnue ou pas de tranche correspondante : `primeAncienneteEur = 0`.

**Total brut :**
- `totalRappelBrutEur = totalRappelBrutHorsRevalorisationEur + montantRevalorisationEur + primeAncienneteEur`.

**Congés payés sur rappel :**
- `DIX_POURCENT` : `congesPayesSurRappelEur = totalRappelBrutEur × 0.10`.
- `MAINTIEN` : approximation `totalRappelBrutEur / 12` (équivalent maintien moyen sur 1 mois de CP/an).
- `AUCUN` : `0` (typiquement CDD avec ICCP versée séparément).

**Total final :** `totalAvecCpEur = totalRappelBrutEur + congesPayesSurRappelEur`.

**Sortie** : tous les champs ci-dessus + `baseJuridique` + `formule` + `messages` + `country=FRANCE`.

### Cas d'erreur
| Situation | Comportement | Code HTTP |
|---|---|---|
| `periodeDebut` ou `periodeFin` null | 400 | 400 |
| `periodeFin < periodeDebut` | 400 "Période invalide" | 400 |
| `montantSalaireDuMensuelEur` ≤ 0 | 400 | 400 |
| `montantSalairePerVerseMensuelEur` < 0 | 400 | 400 |
| `montantSalaireDuMensuelEur ≤ montantSalairePerVerseMensuelEur` | 400 "Aucun différentiel à réclamer" | 400 |
| `tauxRevalorisationPct < 0` ou > 100 | 400 | 400 |
| `ancienneteAnneesPrime < 0` | 400 | 400 |
| `methodeCpSurRappel` null | 400 | 400 |
| Dossier d'un autre domaine | 400 | 400 |
| Dossier workspace BELGIQUE | 400 "FR only" | 400 |
| Workspace différent | 404 | 404 |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier travail FR** :
  - F-DT-25 indemnité préavis : utilise déjà `LegalReferentialService.getConventionPreavis(...)` (durée préavis CCN) — pattern voisin.
  - F-DT-26 indemnité congés payés : utilise les mêmes méthodes 10% / maintien sur l'indemnité finale (méthode L.3141-24). Ici la méthode s'applique au rappel et non à la rupture — concept distinct mais formules cohérentes.
  - F-DT-15 inaptitude / F-DT-17 indemnité précarité CDD / F-DT-09 comparateur Macron : aucun ne calcule de rappel de salaire — pas de chevauchement.
  - **Aucun outil existant ne calcule le rappel de salaire.**
- [x] **Autres pays** : Belgique — concept similaire (arriéré de rémunération + prescription 5 ans art. 2262bis CC ; CP différents = pécule de vacances). Feature jumelle BE à ouvrir au backlog si besoin (`F-DT-20-BE`). Sources juridiques distinctes → SF séparée justifiée.
- [x] **Autres domaines** : non applicable — concept exclusif au droit du travail.
- [x] **Référentiel CCN** : la SF consomme le type existant `CONVENTION_BAREMES.primes` (pourcentage par ancienneté). Pas de nouveau type créé. Pas de nouveau seed nécessaire — réutilise les CCN seedées par F-129.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Calculator FR | Oui | Intégré (`RappelSalaireCalculator`) |
| Référentiel CCN | Oui (lecture) | `LegalReferentialService.getConventionBareme(...)` (existant) |
| Visibility rule F-IA-04 | Oui | ALWAYS_ON DROIT_DU_TRAVAIL FRANCE priority 57 |
| Frontend Angular | Reporté | SF-DT-20-02 (vague suivante, contrat figé ci-dessous) |
| Préavis BE | Hors périmètre | F-DT-20-BE à ouvrir au besoin |

### Décision

- [x] Backend intégré dans cette SF (calculator + entity + endpoint + migration + visibility rule)
- [x] Frontend reporté à SF-DT-20-02 (parallélisable — contrat figé ci-dessous)

---

## Nouveau pattern UI ou service partagé

Pas de nouveau pattern. La SF consomme une méthode existante `LegalReferentialService.getConventionBareme(...)` et applique le même pattern de visibility rule F-IA-04 ALWAYS_ON FRANCE DROIT_DU_TRAVAIL que les autres calculateurs (F-DT-15/17/25/26).

---

## Impact par domaine métier

**Sensible au domaine** : spécifique DROIT_DU_TRAVAIL FRANCE.
- **Droit du travail FR** : cœur de la SF.
- **Droit du travail BE** : non applicable (sources juridiques distinctes — pécule de vacances, prescription 5 ans). Feature jumelle BE à ouvrir si besoin futur.
- **Droit immigration / famille** : non applicable.

---

## Parité des domaines métier

Outil de **niveau 3** (calculateur). Règle de parité ≥5 ne s'applique pas. Pas d'équivalent en immigration / famille. La parité FR/BE est volontairement différée (SF backlog `F-DT-20-BE` si besoin).

---

## Critères d'acceptation

- [ ] **C1** : `compute(periode 24 mois, dû 2500, versé 2200, IDCC_2120, 8 ans, indexInsee=true, taux 3.5, DIX_POURCENT)` retourne `totalRappelBrutHorsRevalorisationEur=7200`, `montantRevalorisationEur=252`, `primeAncienneteEur` selon CCN, `congesPayesSurRappelEur=10% du brut`, `totalAvecCpEur` cohérent.
- [ ] **C2** : `indexInseeRevalorise=false` → `montantRevalorisationEur=0`.
- [ ] **C3** : `methodeCpSurRappel=AUCUN` → `congesPayesSurRappelEur=0`.
- [ ] **C4** : `methodeCpSurRappel=MAINTIEN` → `congesPayesSurRappelEur ≈ totalRappelBrutEur/12`.
- [ ] **C5** : CCN inconnue → `primeAncienneteEur=0` + message.
- [ ] **C6** : `dû ≤ versé` → `IllegalArgumentException`.
- [ ] **C7** : `tauxRevalorisationPct < 0` → `IllegalArgumentException`.
- [ ] **C8** : `periodeFin < periodeDebut` → `IllegalArgumentException`.
- [ ] **C9** : Migration 143 crée la table + visibility rule (UUID `f1a04001-0000-0000-0000-ee0000000201`, priority 57).
- [ ] **C10** : POST endpoint nominal → 200 + persiste.
- [ ] **C11** : POST autre workspace → 404.
- [ ] **C12** : POST dossier IMMIGRATION → 400.
- [ ] **C13** : POST workspace BELGIQUE → 400.
- [ ] **C14** : GET idempotent retourne le résultat persisté.
- [ ] **C15** : Message "Prescription action 3 ans (art. L.3245-1)" présent.

---

## Périmètre

### Hors scope (explicite)
- **Frontend Angular** — reporté à SF-DT-20-02 (vague suivante).
- **Rappel de salaire BE** — feature jumelle à ouvrir au backlog (`F-DT-20-BE`).
- **Calcul automatique du taux INSEE** — l'avocat saisit lui-même le taux. La connexion à l'API INSEE est hors périmètre.
- **Calcul des cotisations sociales / nettes** — la SF retourne un brut.

---

## Technique

### Endpoints
| Méthode | URL | Auth | Rôle minimum |
|---|---|---|---|
| POST | `/api/v1/case-files/{caseFileId}/rappel-salaire` | OAuth2 | MEMBER |
| GET | `/api/v1/case-files/{caseFileId}/rappel-salaire` | OAuth2 | MEMBER |

### Contrat API (figé pour SF-DT-20-02 frontend)

**Request body (POST)** :
```json
{
  "periodeDebut": "2023-01-01",
  "periodeFin": "2024-12-31",
  "montantSalaireDuMensuelEur": 2500.00,
  "montantSalairePerVerseMensuelEur": 2200.00,
  "conventionCollectiveCode": "IDCC_2120",
  "ancienneteAnneesPrime": 8,
  "indexInseeRevalorise": true,
  "tauxRevalorisationPct": 3.5,
  "methodeCpSurRappel": "DIX_POURCENT"
}
```

- `methodeCpSurRappel` enum : `DIX_POURCENT` | `MAINTIEN` | `AUCUN`.
- `conventionCollectiveCode` nullable.
- `tauxRevalorisationPct` nullable si `indexInseeRevalorise=false`.

**Response** :
```json
{
  "caseFileId": "uuid",
  "periodeDebut": "2023-01-01",
  "periodeFin": "2024-12-31",
  "montantSalaireDuMensuelEur": 2500.00,
  "montantSalairePerVerseMensuelEur": 2200.00,
  "conventionCollectiveCode": "IDCC_2120",
  "ancienneteAnneesPrime": 8,
  "indexInseeRevalorise": true,
  "tauxRevalorisationPct": 3.5,
  "methodeCpSurRappel": "DIX_POURCENT",
  "nbMoisPeriode": 24,
  "differentielMensuelEur": 300.00,
  "totalRappelBrutHorsRevalorisationEur": 7200.00,
  "montantRevalorisationEur": 252.00,
  "primeAncienneteEur": 0.00,
  "totalRappelBrutEur": 7452.00,
  "congesPayesSurRappelEur": 745.20,
  "totalAvecCpEur": 8197.20,
  "baseJuridique": "Art. L.3242-1 (revalorisation) + L.3141-26 (CP sur rappel) + CCN IDCC 2120 (prime ancienneté)",
  "formule": "Différentiel 300,00 €/mois × 24 mois + revalorisation 3.5 % + CP 10 % = 8 197,20 €",
  "messages": ["Prescription action 3 ans (art. L.3245-1)", "..."],
  "country": "FRANCE"
}
```

### Tables impactées
| Table | Opération | Notes |
|-------|-----------|-------|
| `rappel_salaire_analyses` | CREATE | nouvelle table 1:1 avec `case_files` |
| `decision_tool_visibility_rules` | INSERT 1 ligne | tool_id `F-DT-20-rappel-salaire`, ALWAYS_ON, DROIT_DU_TRAVAIL, FRANCE, priority 57 |

### Migration Liquibase
- [x] Oui — `143-create-rappel-salaire-analyses.xml` (table + visibility rule).
- Rollback : DROP TABLE + DELETE rule.

### Composants modifiés / créés
- Backend :
  - `RappelSalaireCalculator.java`
  - `RappelSalaireMethodeCpSurRappel.java` (enum)
  - `RappelSalaireAnalysis.java` (entity)
  - `RappelSalaireRepository.java`
  - `RappelSalaireRequest.java` / `RappelSalaireResponse.java` / `RappelSalaireResult.java`
  - `RappelSalaireService.java`
  - `RappelSalaireController.java`
  - Migration `143-create-rappel-salaire-analyses.xml`
- Tests :
  - `RappelSalaireCalculatorTest.java` (≥ 14 UT)
  - `RappelSalaireControllerIT.java` (≥ 8 IT)

### Contraintes de validation

| Champ | Obligatoire | Format |
|-------|-------------|--------|
| `periodeDebut` / `periodeFin` | Oui | LocalDate, fin ≥ debut |
| `montantSalaireDuMensuelEur` | Oui | BigDecimal > 0 |
| `montantSalairePerVerseMensuelEur` | Oui | BigDecimal ≥ 0 |
| `conventionCollectiveCode` | Non | string normalisé |
| `ancienneteAnneesPrime` | Oui | int ≥ 0 |
| `indexInseeRevalorise` | Oui | boolean |
| `tauxRevalorisationPct` | Conditionnel | BigDecimal [0,100] |
| `methodeCpSurRappel` | Oui | enum DIX_POURCENT/MAINTIEN/AUCUN |
| `case_file_id` | — | Unique (1:1) |

---

## Plan de test

### Tests unitaires (`RappelSalaireCalculatorTest`) — ≥ 14

- [ ] `compute_nominal_24mois_300diff_avecRevalorisationEt10Pct`
- [ ] `compute_indexInseeFalse_pasDeRevalorisation`
- [ ] `compute_methodeAucun_pasDeCp`
- [ ] `compute_methodeMaintien_environ1_12`
- [ ] `compute_ccnInconnue_primeZero`
- [ ] `compute_ccnConnueAvecPrime_appliquePourcentage`
- [ ] `compute_periodeFinAvantDebut_throwsIllegalArgument`
- [ ] `compute_duInferieurAVerse_throwsIllegalArgument`
- [ ] `compute_duEgalVerse_throwsIllegalArgument`
- [ ] `compute_montantDuZero_throwsIllegalArgument`
- [ ] `compute_versNegatif_throwsIllegalArgument`
- [ ] `compute_tauxNegatif_throwsIllegalArgument`
- [ ] `compute_methodeNull_throwsIllegalArgument`
- [ ] `compute_arrondiCentimeHALFUP`
- [ ] `compute_messagesContiennentPrescription3Ans`

### Tests d'intégration (`RappelSalaireControllerIT`) — ≥ 8

- [ ] `POST_nominal_persists_and_returns200`
- [ ] `POST_indexInseeFalse`
- [ ] `POST_methodeAucun`
- [ ] `POST_upsertReplacesExistingAnalysis`
- [ ] `POST_otherWorkspace_returns404`
- [ ] `POST_immigrationCaseFile_returns400`
- [ ] `POST_belgiumWorkspace_returns400`
- [ ] `GET_afterPost_returnsPersistedAnalysis`
- [ ] `GET_withoutPost_returns404`

### Isolation workspace

- [x] Applicable — couvert par `POST_otherWorkspace_returns404`.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context (lecture seule, pattern standard)
- [ ] Plans / limites
- [ ] Navigation / routing
- [x] **F-IA-04 visibility rule** — ajout 1 règle (déclaratif).
- [x] **Outil décisionnel métier** — création nouveau calculateur, scan effectué (cf. Analyse de cohérence ci-dessus).

### Composants / endpoints impactés

- Aucun composant existant n'est modifié.

### Smoke tests E2E concernés

- [x] Aucun — endpoint isolé.

---

## Dépendances

### Subfeatures bloquantes
- SF-IA-04-01 (moteur visibility) — done.
- F-129 (CCN seed `CONVENTION_BAREMES`) — la SF lit ces entrées, mais reste fonctionnelle si la CCN est inconnue (prime=0).

### Questions ouvertes impactées
Aucune.

---

## Notes et décisions

- **Décision 1** : la méthode `MAINTIEN` est implémentée comme `totalRappelBrutEur / 12` (équivalent à 1 mois de CP par an, soit ~8.33 %). C'est une approximation suffisante pour un outil décisionnel ; la méthode 10 % reste préférable légalement (jurisprudence constante).
- **Décision 2** : la prime d'ancienneté est appliquée sur le **rappel brut hors revalorisation** (pas sur la revalorisation elle-même). C'est la pratique majoritaire — la prime suit le différentiel salarial nominal.
- **Décision 3** : l'avocat saisit lui-même le `tauxRevalorisationPct`. Pas d'appel à l'API INSEE (hors périmètre, F-DT-20 V8+).
- **Décision 4** : le calcul `nbMoisPeriode` utilise `Period.between(periodeDebut, periodeFin.plusDays(1))` pour inclure le mois final (24 mois pour 2023-01-01 → 2024-12-31).
- **Décision 5** : message systématique "Prescription action 3 ans (art. L.3245-1)" pour rappeler à l'avocat le délai de prescription de l'action en paiement de salaire.
