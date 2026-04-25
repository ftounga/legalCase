# Mini-spec — F-FA-25 / SF-FA-25-05 Backend Mandat de protection future

## Identifiant

`F-FA-25 / SF-FA-25-05`

## Feature parente

`F-FA-25` — Majeurs protégés (sauvegarde, habilitation, curatelle simple/renforcée, tutelle, **mandat de protection future**)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-25-05-backend-mandat-protection-future`

---

## Objectif

Affiner l'outil décisionnel `MajeursProtegesCalculator` pour qu'il **recommande explicitement et avec un verdict d'éligibilité circonstancié** le régime **MANDAT_PROTECTION_FUTURE** (art. 477-494 Cciv) — mécanisme **anticipatif** par lequel la personne, encore capable, désigne à l'avance un mandataire qui pourra agir pour elle si ses facultés se trouvent altérées. À la différence des autres 5 régimes (prononcés par le juge), le mandat est **contractuel** : il échappe au contrôle continu du juge dès lors qu'il prend la forme notariée. Cette SF clôture **F-FA-25 (6/6 régimes après merge)**.

La SF-FA-25-04 (mergée PR #628) a affiné la tutelle (5/6 régimes). Cette SF complète le 6e et dernier régime — débloque par ailleurs le merge de PR #629 SF-FA-25-06 frontend qui expose déjà les champs `mandatPrealableSigne` + `formeMandatProtection`.

---

## Comportement attendu

### Cas nominal MANDAT_PROTECTION_FUTURE (art. 477-494 Cciv)

L'avocat saisit un dossier `DROIT_FAMILLE` FRANCE pour une personne ayant **antérieurement signé** un mandat de protection future (alors qu'elle était capable) et dont les facultés sont **désormais altérées**, justifiant la mise en œuvre du mandat (art. 481).

Critères d'**éligibilité ELEVEE** pour MANDAT_PROTECTION_FUTURE :
- `mandatPrealableSigne = true` (PIVOT — la personne avait signé un mandat AVANT l'altération, art. 477)
- `formeMandatProtection ∈ {NOTARIE, SOUS_SEING_PRIVE}` (art. 489 / 492)
- `altertationFacultesMentales` ou `altertationFacultesPhysiques` = true (le mandat n'entre en vigueur qu'avec une altération constatée — art. 481)
- `certificatMedicalCirconstancie` = true (médecin inscrit liste procureur, mise en œuvre — art. 481)
- Si `formeMandatProtection = SOUS_SEING_PRIVE` ET `actesEnvisages` contient un acte grave (`GESTION_PATRIMOINE` ou `DECISIONS_LOGEMENT`) → critère "Forme notariée requise pour actes graves" (art. 493 — le mandat sous seing privé est limité à la gestion patrimoniale courante, pas les actes graves)

Si tous ces critères sont remplis ET la forme est NOTARIE → `eligible = true` + verdict ELEVEE + recommandation MANDAT_PROTECTION_FUTURE prioritaire.

### Cas dégradés

| Situation | `eligible` | `criteresNonRemplis` |
|-----------|-----------|----------------------|
| `mandatPrealableSigne = false` | false | "Mandat préalable signé requis (art. 477) — basculer vers un autre régime" |
| Cert médical manquant | false | "Certificat médical circonstancié (art. 431/481)" |
| Pas d'altération | false | "Altération des facultés (art. 425) — mandat n'entre en vigueur qu'avec altération constatée art. 481" |
| `formeMandatProtection` non renseignée | false | "Forme du mandat requise (NOTARIE ou SOUS_SEING_PRIVE — art. 489/492)" |
| `formeMandatProtection = SOUS_SEING_PRIVE` ET acte grave | false ou MOYENNE | "Forme notariée requise pour actes graves (art. 493) — mandat sous seing privé limité à la gestion patrimoniale courante" |

### Verdict

- **ELEVEE** : tous critères remplis + forme NOTARIE
- **MOYENNE** : mandat existe mais forme inadaptée (sous seing privé pour actes graves)
- **FAIBLE** : `mandatPrealableSigne = false` → recommander un autre régime

### Priorité dans l'arbre de décision

Le mandat de protection future est **prioritaire sur tous les autres régimes** dès lors que :
- `mandatPrealableSigne = true`
- ET `altertationFacultesMentales` ou `altertationFacultesPhysiques` = true
- ET `certificatMedicalCirconstancie = true`

Justification : le mandat est l'**expression anticipée** de la volonté de la personne — la subsidiarité (art. 428) commande de le respecter avant toute mesure judiciaire imposée. Les régimes judiciaires (sauvegarde, habilitation, curatelle, tutelle) ne sont mobilisés que si le mandat n'existe pas ou s'avère insuffisant.

Exception : si `urgencePatrimoniale = true` ET le mandat est sous seing privé, la SAUVEGARDE_JUSTICE peut rester prioritaire (mesure provisoire conservatoire en attendant l'activation du mandat).

L'arbre de décision actualisé devient :
1. **MANDAT_PROTECTION_FUTURE** (mandat préalable + altération + cert) → prioritaire
2. urgence patrimoniale + altération → SAUVEGARDE_JUSTICE
3. famille proche + consentement + altération mentale + non urgent → HABILITATION_FAMILIALE
4. altertationGrave + cert + incap quotidienne + ≥ 2 catégories d'actes + sans consentement → TUTELLE
5. incapacité quotidienne + altération + cert + sans consentement → CURATELLE_RENFORCEE
6. gestion patrimoine + patrimoine significatif + sans consentement → CURATELLE_RENFORCEE (compat SF-01)
7. altération + cert + acte patrimonial + pas d'incap + sans consentement + sans isolement → CURATELLE_SIMPLE
8. altération mentale + isolement + sans consentement → TUTELLE (compat SF-01 fallback)
9. fallback → régime demandé

### Délai procédure

Le mandat de protection future entre en vigueur **sans audience JAF** dès que le certificat médical constate l'altération (art. 481). Le délai prévisionnel est donc **court** = `DELAI_COURT_MOIS` (4 mois — temps d'obtention du certificat + visa du greffier pour la version sous seing privé).

### Cas d'erreur

Identiques à SF-FA-25-01/03/04 (régime invalide, demandeur invalide, workspace BE, dossier non famille, cert manquant, etc.).

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** — calculator existant `MajeursProtegesCalculator`, pattern préservé (extension non rupturante de la signature — surcharge `compute()` 13-args → 15-args)
- [x] **Autres pays** — outil **single-country FR**. Équivalent BE = mandat extra-judiciaire (art. 489 et s. CC BE) — backlog
- [x] **Autres domaines** — DROIT_FAMILLE seul (gate déjà actif)
- [x] **Autres UI patterns** — réutilisation endpoint POST/GET existant `/api/v1/case-files/{id}/majeurs-proteges`
- [x] **Autres flows transversaux** — auth + workspace context inchangés

### Cas spécifique : extension d'outil décisionnel existant

- [x] **Cohérence IA (F-IA-03)** : `mandatPrealableSigne` + `formeMandatProtection` candidats à validation IA (frontend SF-06 mergée — coherence builder déjà en place)
- [x] **Refresh dashboard (F-IA-02)** : pas d'impact backend
- [x] **Pré-remplissage IA** : 4 nouveaux champs `*Detected` côté `FamilleExtractedData` (PR #629)
- [x] **Persistance des inputs** : nouvelles colonnes `mandat_prealable_signe` boolean nullable + `forme_mandat_protection` varchar(30) nullable + persistance dans `result_data` JSON
- [x] **Masquage conditionnel selon type** : règle visibility F-IA-04 inchangée (UUID `f1a04001-0000-0000-0000-ee00000fa251`)
- [x] **Alertes actives après calcul** : N/A backend
- [x] **Description SF-140-03** : seed `legal_referentials` avec 1 entry `MAJEURS_PROTEGES_REGIMES` `MANDAT_PROTECTION_FUTURE` avec **description obligatoire** en langage avocat

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Mandat extra-judiciaire BE (art. 489 CC BE) | Oui | Backlog — SF jumelle BE (juge de paix) |
| Frontend mandat de protection future | Oui | **Déjà couvert** par PR #629 SF-FA-25-06 (en attente de ce backend) |
| Test IT description SF-140-03 | Oui | Lancé par CI sur la nouvelle entry `legal_referentials` |

### Décision

- [x] Étendu à mandat de protection future (1 régime supplémentaire — clôture F-FA-25 6/6)
- [x] Backlog : mandat extra-judiciaire BE

---

## Impact par domaine métier

Cette feature **est sensible au domaine** : DROIT_FAMILLE FR exclusivement.

- **DROIT_FAMILLE FR** : extension calculator + validation eligibilité mandat de protection future
- **DROIT_FAMILLE BE** : équivalent = mandat extra-judiciaire — non couvert (backlog)
- **DROIT_DU_TRAVAIL** : 400 (gate inchangé)
- **DROIT_IMMIGRATION** : 400 (gate inchangé)

---

## Parité des domaines métier

Outil de **niveau 5 (scoring + analyse validité)** — extension d'un outil existant déjà niveau 5. Parité :

- **DROIT_FAMILLE** : F-FA-25 cette SF (mandat de protection future)
- **DROIT_DU_TRAVAIL** : équivalent N/A — le mandat de protection future est exclusivement un mécanisme civil (Cciv 477) sans transposition en droit du travail
- **DROIT_IMMIGRATION** : équivalent N/A — un étranger sous mandat suit la même logique civile, sans impact migratoire spécifique

→ Pas de feature jumelle au backlog. Concept spécifique au DROIT_FAMILLE FR (équivalent BE = backlog).

---

## Critères d'acceptation

- [ ] `POST /api/v1/case-files/{id}/majeurs-proteges` accepte 2 nouveaux champs optionnels : `mandatPrealableSigne` (Boolean) + `formeMandatProtection` (String — NOTARIE ou SOUS_SEING_PRIVE)
- [ ] La réponse expose les 2 nouveaux champs `mandatPrealableSigne` + `formeMandatProtection`
- [ ] Le calculator recommande **MANDAT_PROTECTION_FUTURE** quand : `mandatPrealableSigne = true` + altération + cert médical, en priorité sur les autres régimes (sauf urgence + sous seing privé)
- [ ] Verdict `ELEVEE` quand tous les critères mandat remplis + forme NOTARIE
- [ ] Verdict `MOYENNE` quand forme SOUS_SEING_PRIVE pour actes graves (`GESTION_PATRIMOINE` ou `DECISIONS_LOGEMENT`)
- [ ] `criteresNonRemplis` liste explicite les critères absents si verdict ≠ ELEVEE
- [ ] `eligible = (criteresNonRemplis.isEmpty())` → boolean dérivé
- [ ] Migration Liquibase **161** :
  - ALTER table `majeurs_proteges_analyses` ADD COLUMN `mandat_prealable_signe` boolean nullable
  - ALTER table `majeurs_proteges_analyses` ADD COLUMN `forme_mandat_protection` varchar(30) nullable
  - INSERT 1 entry `legal_referentials` (MANDAT_PROTECTION_FUTURE UUID `f1a04001-0000-0000-0000-000000000162`) avec **description obligatoire** SF-140-03
- [ ] ≥ 15 tests unitaires + ≥ 5 tests d'intégration
- [ ] Backward compatibility : un POST sans `mandatPrealableSigne`/`formeMandatProtection` reste valide (default false / null)
- [ ] Régression zéro : `MajeursProtegesCalculatorTest` + `MajeursProtegesCalculatorCuratelleTest` + `MajeursProtegesCalculatorTutelleTest` + `MajeursProtegesCuratelleIT` + `MajeursProtegesTutelleIT` + `LegalReferentialDescriptionIntegrityIT` toujours verts

---

## Périmètre

### Hors scope (explicite)

- Mandat extra-judiciaire BE (SF jumelle backlog)
- Génération de l'acte de mandat (notarié ou sous seing privé)
- Visa du greffier pour le mandat sous seing privé (art. 492)
- Frontend mandat (déjà couvert par PR #629 SF-FA-25-06)
- Articulation avec la sauvegarde de justice (art. 491 — révocation par le juge)

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `mandatPrealableSigne` | null en DB, false dans le calcul | Default si non renseigné |
| `formeMandatProtection` | null en DB, null dans le calcul | Default si non renseigné — verdict aligné |

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs |
|-------|-------------|------------------|
| `mandatPrealableSigne` | Non (default false) | Boolean |
| `formeMandatProtection` | Non (default null) | Enum String : NOTARIE / SOUS_SEING_PRIVE |

Reste inchangé.

---

## Technique

### Endpoints

Inchangé : `POST + GET /api/v1/case-files/{caseFileId}/majeurs-proteges`. **Le contrat est étendu** (2 champs optionnels ajoutés en entrée + 2 champs propagés en sortie).

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `majeurs_proteges_analyses` | ALTER | ADD COLUMN `mandat_prealable_signe` boolean nullable + ADD COLUMN `forme_mandat_protection` varchar(30) nullable |
| `legal_referentials` | INSERT | 1 entry `MAJEURS_PROTEGES_REGIMES` (MANDAT_PROTECTION_FUTURE) avec description SF-140-03 |

### Migration Liquibase

`161-add-mandat-protection-future-criteres-majeurs-proteges.xml`

UUID `legal_referentials` :
- MANDAT_PROTECTION_FUTURE : `f1a04001-0000-0000-0000-000000000162`

### Composants Angular

N/A — déjà couvert par PR #629 SF-FA-25-06 (frontend en parallèle).

---

## Plan de test

### Tests unitaires (≥ 15) — `MajeursProtegesCalculatorMandatProtectionTest`

1. `mandat_recommandee_ELEVEE_notarie` — tous critères + NOTARIE → MANDAT eligible
2. `mandat_recommandee_ELEVEE_sousSeingPrive_actesNonGraves` — sous seing privé sans actes graves → eligible
3. `mandat_eligible_false_mandatPrealableAbsent` — sans mandat préalable → non éligible, autre régime recommandé
4. `mandat_eligible_false_certManquant` — cert manquant → non éligible
5. `mandat_eligible_false_alterationAbsente` — altération absente → non éligible
6. `mandat_MOYENNE_sousSeingPrive_actesGraves` — sous seing privé pour gestion patrimoine → MOYENNE
7. `mandat_priorite_surTutelle` — avec mandat → MANDAT, sans → TUTELLE (même contexte par ailleurs)
8. `mandat_criteresNonRemplis_listeExplicite` — multiple critères manquants tous listés
9. `mandat_msg_contient_477` — message évoque art. 477
10. `mandat_msg_contient_489_si_notarie` — message notarié évoque art. 489
11. `mandat_propage_dansResult` — champs propagés
12. `mandat_null_traiteCommeFalse` — surcharge 13-args (sans mandat) → backward compat
13. `forme_null_traiteCommeNonRenseigne` — formeMandatProtection null + mandatPrealableSigne true → critère manquant
14. `arbreDecision_mandat_existant_remplaceToutAutreRegime` — sauf urgence sous seing privé
15. `mandat_eligible_dérivéDeListe` — eligible = criteresNonRemplis.isEmpty()
16. `mandat_dureeProcedure_courte` — DELAI_COURT_MOIS (pas d'audience JAF — exécution directe art. 481)
17. `backwardCompat_surcharge13Args_traitéCommeFalse` — appel ancienne signature
18. `mandat_priorite_sauf_urgence_sousSeingPrive` — urgence + sous seing privé → SAUVEGARDE prioritaire

### Tests d'intégration (≥ 5) — `MajeursProtegesMandatProtectionIT`

1. POST FR mandat NOTARIE eligible → 200 + verdict ELEVEE
2. POST FR mandat sans préalable → 200 + criteresNonRemplis "Mandat préalable requis"
3. POST FR mandat sous seing privé pour actes graves → 200 + MOYENNE
4. POST FR upsert : POST tutelle puis POST mandat remplace
5. GET après POST mandat → 200 + champs persistés

### Description integrity test (CI)

`LegalReferentialDescriptionIntegrityIT` doit passer avec la nouvelle entry MANDAT_PROTECTION_FUTURE (description non-null).

### Isolation workspace

Hérité de SF-FA-25-01.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — pattern inchangé
- [ ] Workspace context — inchangé
- [ ] Plans / limites — N/A
- [ ] Navigation / routing frontend — N/A backend
- [x] **Outil décisionnel métier** — extension du calculator `MajeursProtegesCalculator` pour le 6e et dernier régime ; pattern préservé, signature étendue avec 2 champs optionnels `mandatPrealableSigne` + `formeMandatProtection`
- [x] Aucune autre préoccupation transversale

### Smoke tests E2E concernés

- [x] Aucun — pas d'impact auth/workspace/nav

---

## Dépendances

### Subfeatures bloquantes

- SF-FA-25-01 (mergée PR #605) — table + calculator de base
- SF-FA-25-03 (mergée PR #622) — colonne `incapacite_gestion_quotidienne`
- SF-FA-25-04 (mergée PR #628) — colonne `altertation_grave`

### Subfeatures parallélisables

- Frontend mandat de protection future (PR #629 SF-FA-25-06 en attente de ce backend pour merge)

### Questions ouvertes impactées

Aucune.

---

## Contrat API (figé)

`POST + GET /api/v1/case-files/{caseFileId}/majeurs-proteges`

**Request body** (POST, 2 champs ajoutés en gras) :

```json
{
  "regimeProtectionDemande": "MANDAT_PROTECTION_FUTURE",
  "altertationFacultesMentales": true,
  "altertationFacultesPhysiques": false,
  "certificatMedicalCirconstancie": true,
  "dateCertificatMedical": "2026-04-15",
  "consentementPersonneAProteger": false,
  "demandeurFamilial": "ENFANT_MAJEUR",
  "actesEnvisages": ["GESTION_PATRIMOINE"],
  "urgencePatrimoniale": false,
  "patrimoineSignificatif": true,
  "isolementSocial": false,
  "incapaciteGestionQuotidienne": false,
  "altertationGrave": false,
  "mandatPrealableSigne": true,
  "formeMandatProtection": "NOTARIE"
}
```

Enums étendus : `formeMandatProtection ∈ {NOTARIE, SOUS_SEING_PRIVE}`.

**Response** (2 champs ajoutés en gras) :

```json
{
  "caseFileId": "uuid",
  "regimeProtectionDemande": "MANDAT_PROTECTION_FUTURE",
  "altertationFacultesMentales": true,
  "...": "...",
  "incapaciteGestionQuotidienne": false,
  "altertationGrave": false,
  "mandatPrealableSigne": true,
  "formeMandatProtection": "NOTARIE",
  "scoreEligibilite": 70,
  "regimeOptimalRecommande": "MANDAT_PROTECTION_FUTURE",
  "verdictAcceptabiliteJaf": "ELEVEE",
  "delaiProcedureMoisPrevisionnel": 4,
  "auditionPersonneObligatoire": true,
  "expertisePsyComplementaireRecommandee": false,
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

- **Backward compatibility** : `mandatPrealableSigne` (Boolean) + `formeMandatProtection` (String) sont optionnels ; absence = false / null. Aucun frontend existant ne casse, et la logique des autres régimes reste préservée. Surcharge `compute()` 13-args inchangée et délègue à la version 15-args avec defaults.
- **Spécificité du mandat** : régime contractuel et anticipatif (pas judiciaire). Le mandat est l'expression de la volonté de la personne — la subsidiarité (art. 428) commande de le respecter avant toute mesure judiciaire imposée. **Le mandat est donc prioritaire dans l'arbre de décision** dès lors que les conditions de mise en vigueur sont réunies (art. 481).
- **Distinction notarié vs sous seing privé** : le mandat notarié (art. 489) couvre la gestion patrimoniale ET la protection de la personne, avec force exécutoire. Le mandat sous seing privé (art. 492) est limité à la **gestion patrimoniale courante** ; les actes graves (vente immobilière, emprunt) requièrent une autorisation spéciale du juge ou la forme notariée. C'est ce qui motive le verdict MOYENNE en cas de forme inadaptée.
- **Description SF-140-03** : 1 entry `MAJEURS_PROTEGES_REGIMES` `MANDAT_PROTECTION_FUTURE` avec description riche en langage avocat (référencement art. 477-494, conditions précises, distinctions notarié/sous seing privé, cas d'emploi).
- **Migration ID 161** : suit séquentiellement 160 (tutelle SF-FA-25-04). Pas de collision avec 164 (PSE) ni 165 (crédit-temps BE).
- **UUID** : `f1a04001-0000-0000-0000-000000000162`. Pattern aligné avec UUIDs SF-03/04 (159 → CURATELLE_SIMPLE/RENFORCEE, 160 → TUTELLE).
- **Pas de nouvelle table** : la table `majeurs_proteges_analyses` existante couvre tous les régimes ; ALTER ADD COLUMN x2 suffit.
- **Délai court** : le mandat n'implique pas d'audience JAF. Mise en œuvre dès certificat médical (art. 481). DELAI_COURT_MOIS = 4.
- **Audition** : conservée à `true` (cohérence des messages avec art. 432) bien que techniquement la mise en vigueur du mandat ne nécessite pas d'audition formelle.
