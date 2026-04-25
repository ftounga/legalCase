# Mini-spec — F-FA-25 / SF-FA-25-04 Backend Tutelle

## Identifiant

`F-FA-25 / SF-FA-25-04`

## Feature parente

`F-FA-25` — Majeurs protégés (sauvegarde, habilitation, curatelle simple/renforcée, **tutelle**, mandat protection future)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-25-04-backend-tutelle`

---

## Objectif

Affiner l'outil décisionnel `MajeursProtegesCalculator` pour qu'il **recommande explicitement et avec un verdict d'éligibilité circonstancié** le régime **TUTELLE** (art. 440 al. 3 Cciv), le plus protecteur des 6 — la personne ne peut **plus pourvoir seule à ses intérêts** en raison d'une **altération grave et durable** de ses facultés et le tuteur la **représente** dans tous les actes de la vie civile.

La SF-FA-25-03 (mergée PR #622) a affiné sauvegarde + habilitation + curatelle simple/renforcée (4/6 régimes). Cette SF complète 1 régime supplémentaire (5/6 après merge — il restera mandat de protection future SF-05).

---

## Comportement attendu

### Cas nominal TUTELLE (art. 440 al. 3 Cciv)

L'avocat saisit un dossier `DROIT_FAMILLE` FRANCE pour une personne dont les facultés mentales/physiques sont **gravement et durablement** altérées au point qu'elle ne peut **plus pourvoir seule à ses intérêts** dans les actes essentiels (pas seulement les actes patrimoniaux exceptionnels).

Critères d'**éligibilité ELEVEE** pour TUTELLE :
- `altertationFacultesMentales` ou `altertationFacultesPhysiques` = true (art. 425)
- `certificatMedicalCirconstancie` = true (obligatoire art. 431)
- **`altertationGrave` = true** (critère pivot tutelle — la personne ne peut plus pourvoir seule à ses intérêts)
- `incapaciteGestionQuotidienne` = true (la personne ne peut pas gérer son budget courant — déjà introduit en SF-03)
- `actesEnvisages` doit inclure **au moins 2 catégories différentes** (représentation continue, pas juste assistance ponctuelle — par ex. `GESTION_PATRIMOINE` + `DECISIONS_LOGEMENT` et idéalement `DECISIONS_SANTE` ou `DECISIONS_FAMILIALES`)

Si tous ces critères sont remplis → `eligible = true` + verdict ELEVEE + recommandation TUTELLE prioritaire.

### Cas dégradés

| Situation | `eligible` | `criteresNonRemplis` |
|-----------|-----------|----------------------|
| Cert médical manquant | false | "Certificat médical circonstancié (art. 431)" |
| Pas d'altération | false | "Altération des facultés (art. 425)" |
| `altertationGrave = false` | false | "Altération grave et durable (art. 440 al. 3) — basculer vers curatelle" |
| `incapaciteGestionQuotidienne = false` | false | "Incapacité de gestion quotidienne (art. 425/440 al. 3)" |
| Moins de 2 catégories d'actes | false | "Au moins 2 catégories d'actes envisagés (représentation continue art. 440 al. 3)" |

### Priorité dans l'arbre de décision

L'arbre ressort : **TUTELLE > CURATELLE_RENFORCEE > CURATELLE_SIMPLE > HABILITATION > SAUVEGARDE**.

- Si `altertationGrave = true` ET autres critères tutelle remplis → TUTELLE prioritaire (sauf urgence patrimoniale → SAUVEGARDE reste première branche)
- Si `altertationGrave = false` mais `incapaciteGestionQuotidienne = true` → CURATELLE_RENFORCEE (logique existante préservée)
- Si `altertationGrave = null` → traité comme false (default backward compat)
- Si urgence patrimoniale → SAUVEGARDE_JUSTICE reste première priorité (mesure provisoire)
- Si famille proche + consentement + altération mentale + non urgent → HABILITATION_FAMILIALE (subsidiarité art. 428 — l'habilitation suppose le consentement, incompatible en pratique avec une altération grave bloquante)

### Délai procédure JAF

Maintenu à 8 mois (DELAI_LONG_MOIS) — la tutelle, comme les curatelles, est une mesure lourde devant le juge des contentieux de la protection. Audition obligatoire art. 432 (sauf dispense médicale).

### Cas d'erreur

Identiques à SF-FA-25-01/03 (régime invalide, demandeur invalide, workspace BE, dossier non famille, cert manquant, etc.).

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** — calculator existant `MajeursProtegesCalculator`, pattern préservé (extension non rupturante de la signature — surcharge `compute()`)
- [x] **Autres pays** — outil **single-country FR**. Équivalent BE = administration provisoire (art. 488 et s. CC BE) — backlog
- [x] **Autres domaines** — DROIT_FAMILLE seul (gate déjà actif)
- [x] **Autres UI patterns** — réutilisation endpoint POST/GET existant `/api/v1/case-files/{id}/majeurs-proteges`
- [x] **Autres flows transversaux** — auth + workspace context inchangés

### Cas spécifique : extension d'outil décisionnel existant

- [x] **Cohérence IA (F-IA-03)** : `altertationGrave` candidat à validation IA (frontend SF future)
- [x] **Refresh dashboard (F-IA-02)** : pas d'impact backend
- [x] **Pré-remplissage IA** : possible (frontend SF future)
- [x] **Persistance des inputs** : nouvelle colonne `altertation_grave` (boolean nullable) + persistance dans `result_data` JSON
- [x] **Masquage conditionnel selon type** : règle visibility F-IA-04 inchangée (UUID `f1a04001-0000-0000-0000-ee00000fa251`)
- [x] **Alertes actives après calcul** : N/A backend
- [x] **Description SF-140-03** : seed `legal_referentials` avec 1 entry `MAJEURS_PROTEGES_REGIMES` `TUTELLE` avec **description obligatoire** en langage avocat

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Mandat protection future FR (art. 477) | Oui | Backlog — SF-FA-25-05 (anticipation par la personne avant altération) |
| Administration provisoire BE | Oui | Backlog — SF jumelle BE (juge de paix) |
| Frontend tutelle | Oui | SF future, contrat API étendu ici |
| Test IT description SF-140-03 | Oui | Lancé par CI sur la nouvelle entry `legal_referentials` |

### Décision

- [x] Étendu à tutelle (1 régime supplémentaire — le plus protecteur)
- [x] Backlog : mandat protection future, administration provisoire BE

---

## Impact par domaine métier

Cette feature **est sensible au domaine** : DROIT_FAMILLE FR exclusivement.

- **DROIT_FAMILLE FR** : extension calculator + validation eligibilité tutelle
- **DROIT_FAMILLE BE** : équivalent = administration provisoire — non couvert (backlog)
- **DROIT_DU_TRAVAIL** : 400 (gate inchangé)
- **DROIT_IMMIGRATION** : 400 (gate inchangé)

---

## Parité des domaines métier

Outil de **niveau 5 (scoring + analyse validité)** — extension d'un outil existant déjà niveau 5. Parité :

- **DROIT_FAMILLE** : F-FA-25 cette SF (tutelle)
- **DROIT_DU_TRAVAIL** : équivalent N/A — la tutelle est exclusivement civile (JCP), un salarié sous tutelle reste salarié sans impact contrat travail (les actes du tuteur sur la rupture peuvent être contestés par le juge mais le concept "tutelle salariée" n'existe pas en tant que régime)
- **DROIT_IMMIGRATION** : équivalent N/A — un étranger sous tutelle suit la même procédure JCP, sans impact migratoire spécifique

→ Pas de feature jumelle au backlog. Concept spécifique au DROIT_FAMILLE.

---

## Critères d'acceptation

- [ ] `POST /api/v1/case-files/{id}/majeurs-proteges` accepte un nouveau champ optionnel `altertationGrave` (Boolean)
- [ ] La réponse expose le nouveau champ `altertationGrave`
- [ ] Le calculator recommande **TUTELLE** quand : altération + cert médical + `altertationGrave = true` + `incapaciteGestionQuotidienne = true` + au moins 2 catégories d'actes (représentation continue), sans urgence ni consentement+habilitation prioritaire
- [ ] Verdict `ELEVEE` quand tous les critères de la tutelle demandée sont remplis
- [ ] `criteresNonRemplis` liste explicite les critères absents si verdict ≠ ELEVEE
- [ ] `eligible = (criteresNonRemplis.isEmpty())` → boolean dérivé
- [ ] Migration Liquibase **160** :
  - ALTER table `majeurs_proteges_analyses` ADD COLUMN `altertation_grave` boolean nullable
  - INSERT 1 entry `legal_referentials` (TUTELLE UUID `f1a04001-0000-0000-0000-000000000161`) avec **description obligatoire** SF-140-03
- [ ] ≥ 15 tests unitaires + ≥ 5 tests d'intégration
- [ ] Backward compatibility : un POST sans `altertationGrave` reste valide (default false)
- [ ] Régression zéro : `MajeursProtegesCalculatorTest` + `MajeursProtegesCalculatorCuratelleTest` + `MajeursProtegesCuratelleIT` + `LegalReferentialDescriptionIntegrityIT` toujours verts

---

## Périmètre

### Hors scope (explicite)

- Mandat de protection future (SF-FA-25-05 future)
- Administration provisoire BE (SF jumelle backlog)
- Frontend tutelle (SF future)
- Génération PDF requête JCP
- Dispense d'audition par certificat médical (art. 432 al. 2)

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `altertationGrave` | null en DB, false dans le calcul | Default si non renseigné |

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs |
|-------|-------------|------------------|
| `altertationGrave` | Non (default false) | Boolean |

Reste inchangé.

---

## Technique

### Endpoints

Inchangé : `POST + GET /api/v1/case-files/{caseFileId}/majeurs-proteges`. **Le contrat est étendu** (champ optionnel ajouté + champ propagé en sortie).

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `majeurs_proteges_analyses` | ALTER | ADD COLUMN `altertation_grave` boolean nullable |
| `legal_referentials` | INSERT | 1 entry `MAJEURS_PROTEGES_REGIMES` (TUTELLE) avec description SF-140-03 |

### Migration Liquibase

`160-add-tutelle-criteres-majeurs-proteges.xml`

UUID `legal_referentials` :
- TUTELLE : `f1a04001-0000-0000-0000-000000000161`

### Composants Angular

N/A (SF frontend future).

---

## Plan de test

### Tests unitaires (≥ 15) — `MajeursProtegesCalculatorTutelleTest`

1. `compute_tutelle_recommandee_ELEVEE` — tous critères remplis → TUTELLE eligible
2. `compute_tutelle_eligible_false_alterationLegere` — `altertationGrave = false` → tutelle non recommandée, curatelle renforcée si autres critères
3. `compute_tutelle_eligible_false_certManquant`
4. `compute_tutelle_eligible_false_incapAbsente`
5. `compute_tutelle_eligible_false_actesInsuffisants` — un seul type d'acte → critère non rempli
6. `compute_tutelle_priorite_surCuratelleRenforcee` — même contexte avec `altertationGrave=true` → tutelle, sans → curatelle renforcée
7. `compute_tutelle_criteresNonRemplis_listeExplicite`
8. `compute_tutelle_msg_contient_440_al3`
9. `compute_altertationGrave_propagee_dansResult`
10. `compute_altertationGrave_null_traiteCommeFalse`
11. `compute_tutelle_eligible_dérivéDeListe`
12. `compute_tutelle_inclutDecisionsMedicales_conserve_eligible`
13. `compute_priorite_arbreDecision` — tutelle > curatelle > habilitation > sauvegarde, urgence reste première
14. `compute_dureeProcedure_tutelle_8a10mois`
15. `compute_auditionObligatoire_tutelle_true`
16. `compute_tutelle_msg_contientArt432_audition`

### Tests d'intégration (≥ 5) — `MajeursProtegesTutelleIT`

1. POST FR tutelle eligible → 200 + eligible=true + verdict ELEVEE
2. POST FR tutelle altertationGrave=false → 200 + eligible=false (mais autre régime peut être recommandé)
3. POST FR upsert : POST sauvegarde puis POST tutelle remplace
4. GET après POST tutelle → 200 + altertationGrave persisté
5. POST FR tutelle sans certificat → 200 + criteresNonRemplis contient certificat

### Description integrity test (CI)

`LegalReferentialDescriptionIntegrityIT` doit passer avec la nouvelle entry TUTELLE (description non-null).

### Isolation workspace

Hérité de SF-FA-25-01.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — pattern inchangé
- [ ] Workspace context — inchangé
- [ ] Plans / limites — N/A
- [ ] Navigation / routing frontend — N/A backend
- [x] **Outil décisionnel métier** — extension du calculator `MajeursProtegesCalculator` pour 1 nouveau régime ; pattern préservé, signature étendue avec un champ optionnel `altertationGrave`
- [x] Aucune autre préoccupation transversale

### Smoke tests E2E concernés

- [x] Aucun — pas d'impact auth/workspace/nav

---

## Dépendances

### Subfeatures bloquantes

- SF-FA-25-01 (mergée PR #605) — table + calculator de base
- SF-FA-25-03 (mergée PR #622) — colonne `incapacite_gestion_quotidienne` + critère pivot art. 472

### Subfeatures parallélisables

- Frontend tutelle (SF future, contrat API figé ici)

### Questions ouvertes impactées

Aucune.

---

## Contrat API (figé)

`POST + GET /api/v1/case-files/{caseFileId}/majeurs-proteges`

**Request body** (POST, champ ajouté en gras) :

```json
{
  "regimeProtectionDemande": "TUTELLE",
  "altertationFacultesMentales": true,
  "altertationFacultesPhysiques": false,
  "certificatMedicalCirconstancie": true,
  "dateCertificatMedical": "2026-04-15",
  "consentementPersonneAProteger": false,
  "demandeurFamilial": "ENFANT_MAJEUR",
  "actesEnvisages": ["GESTION_PATRIMOINE", "DECISIONS_LOGEMENT", "DECISIONS_SANTE"],
  "urgencePatrimoniale": false,
  "patrimoineSignificatif": true,
  "isolementSocial": false,
  "incapaciteGestionQuotidienne": true,
  "altertationGrave": true
}
```

Enums inchangés (régime, demandeur, actes).

**Response** (champ ajouté en gras) :

```json
{
  "caseFileId": "uuid",
  "regimeProtectionDemande": "TUTELLE",
  "altertationFacultesMentales": true,
  "...": "...",
  "incapaciteGestionQuotidienne": true,
  "altertationGrave": true,
  "scoreEligibilite": 70,
  "regimeOptimalRecommande": "TUTELLE",
  "verdictAcceptabiliteJaf": "MOYENNE",
  "delaiProcedureMoisPrevisionnel": 8,
  "auditionPersonneObligatoire": true,
  "expertisePsyComplementaireRecommandee": true,
  "eligible": true,
  "criteresNonRemplis": [],
  "baseJuridique": "Art. 433-441 + 494-1 et s. Cciv",
  "formule": "Score X = acceptabilité ...",
  "messages": ["..."],
  "country": "FRANCE"
}
```

---

## Notes et décisions

- **Backward compatibility** : `altertationGrave` est optionnel (Boolean) ; absence = false. Aucun frontend existant ne casse, et la logique tutelle existante (qui historiquement utilisait `isolementSocial`) reste accessible via le branche fallback.
- **Distinction tutelle vs curatelle renforcée** : la curatelle renforcée art. 472 vise les personnes qui ne peuvent plus gérer leur budget quotidien mais conservent un degré d'autonomie (le curateur **assiste**) ; la tutelle art. 440 al. 3 vise une altération grave et durable où la personne ne peut plus pourvoir seule à ses intérêts dans les actes essentiels (le tuteur **représente**). Le critère pivot `altertationGrave` permet de distinguer.
- **Description SF-140-03** : 1 entry `MAJEURS_PROTEGES_REGIMES` `TUTELLE` avec description riche en langage avocat (référencement art. 440 al. 3, conditions précises, distinctions vs curatelle, cas d'emploi).
- **Migration ID 160** : suit séquentiellement 159 (curatelle SF-FA-25-03). Pas de collision avec 164 (PSE) ni 165 (crédit-temps BE).
- **UUID** : `f1a04001-0000-0000-0000-000000000161`. Pattern aligné avec UUIDs SF-03 (159, 160 pour CURATELLE_SIMPLE/RENFORCEE).
- **Pas de nouvelle table** : la table `majeurs_proteges_analyses` existante couvre tous les régimes via la colonne `regime_protection_demande` ; ALTER ADD COLUMN suffit.
- **Priorité arbre décisionnel** : TUTELLE est insérée juste après SAUVEGARDE_JUSTICE (urgence) et HABILITATION_FAMILIALE (consentement), avant les curatelles. La logique historique TUTELLE par isolement social est préservée comme branche secondaire (fallback) pour ne pas casser SF-FA-25-01.
