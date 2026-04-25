# Mini-spec — F-FA-25 / SF-FA-25-03 Backend Curatelle simple + Curatelle renforcée

## Identifiant

`F-FA-25 / SF-FA-25-03`

## Feature parente

`F-FA-25` — Majeurs protégés (sauvegarde, habilitation, **curatelle simple/renforcée**, tutelle, mandat protection future)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-FA-25-03-backend-curatelle`

---

## Objectif

Affiner l'outil décisionnel `MajeursProtegesCalculator` pour qu'il **recommande explicitement et avec un verdict d'éligibilité circonstancié** les régimes **CURATELLE_SIMPLE** (art. 440 al. 1 Cciv) et **CURATELLE_RENFORCEE** (art. 472 Cciv), en exposant la liste des critères non remplis et un drapeau `eligible` par dossier.

La SF-FA-25-01 a posé le squelette pour 6 régimes mais n'a affiné le scoring que pour sauvegarde + habilitation. Cette SF complète 2 régimes supplémentaires (4/6 régimes après merge — il restera tutelle + mandat protection future).

---

## Comportement attendu

### Cas nominal CURATELLE_SIMPLE (art. 440 al. 1 Cciv)

L'avocat saisit un dossier `DROIT_FAMILLE` FRANCE pour une personne dont les facultés mentales/physiques sont altérées de manière à requérir une **assistance pour les actes patrimoniaux importants** (mais pas une représentation continue).

Critères d'**éligibilité ELEVEE** pour CURATELLE_SIMPLE :
- `altertationFacultesMentales` ou `altertationFacultesPhysiques` = true (altération significative requise art. 425 Cciv)
- `certificatMedicalCirconstancie` = true (obligatoire art. 431)
- `actesEnvisages` contient au moins un acte patrimonial important (`GESTION_PATRIMOINE` ou `DECISIONS_LOGEMENT`)
- la personne **n'a pas besoin d'être représentée** pour ses revenus quotidiens (`incapaciteGestionQuotidienne` = false ou non renseigné)

Si tous ces critères sont remplis → `eligible = true` + verdict ELEVEE.

### Cas nominal CURATELLE_RENFORCEE (art. 472 Cciv)

L'avocat saisit pour un dossier où la personne **ne peut pas gérer son budget quotidien** (revenus + dépenses courantes), nécessitant un curateur qui perçoit les revenus et règle les dépenses.

Critères d'**éligibilité ELEVEE** pour CURATELLE_RENFORCEE :
- tous les critères de la curatelle simple
- + `incapaciteGestionQuotidienne` = true (critère pivot art. 472)

Si tous remplis → `eligible = true` + verdict ELEVEE + recommandation prioritaire.

### Cas dégradés

| Situation | `eligible` | `criteresNonRemplis` |
|-----------|-----------|----------------------|
| Cert médical manquant | false | "Certificat médical circonstancié (art. 431)" |
| Pas d'altération | false | "Altération des facultés (art. 425)" |
| Aucun acte patrimonial important | false | "Acte patrimonial important (gestion patrimoine ou logement)" |
| CURATELLE_RENFORCEE demandée mais `incapaciteGestionQuotidienne` = false | false | "Incapacité de gestion quotidienne (art. 472)" |

### Cas d'erreur

Identiques à SF-FA-25-01 (régime invalide, demandeur invalide, workspace BE, dossier non famille, etc.).

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** — calculator existant `MajeursProtegesCalculator`, pattern préservé (extension non rupturante de la signature)
- [x] **Autres pays** — outil **single-country FR**. Équivalent BE = administration provisoire (art. 488 et s. CC BE) — backlog
- [x] **Autres domaines** — DROIT_FAMILLE seul (gate déjà actif)
- [x] **Autres UI patterns** — réutilisation endpoint POST/GET existant `/api/v1/case-files/{id}/majeurs-proteges`
- [x] **Autres flows transversaux** — auth + workspace context inchangés

### Cas spécifique : extension d'outil décisionnel existant

- [x] **Cohérence IA (F-IA-03)** : `incapaciteGestionQuotidienne` candidat à validation IA (frontend SF future)
- [x] **Refresh dashboard (F-IA-02)** : pas d'impact backend
- [x] **Pré-remplissage IA** : possible (frontend SF future)
- [x] **Persistance des inputs** : nouvelle colonne `incapacite_gestion_quotidienne` (boolean nullable) + persistance dans `result_data` JSON
- [x] **Masquage conditionnel selon type** : règle visibility F-IA-04 inchangée (UUID `f1a04001-0000-0000-0000-ee00000fa251`)
- [x] **Alertes actives après calcul** : N/A backend
- [x] **Description SF-140-03** : seed `legal_referentials` avec 2 entries `MAJEURS_PROTEGES_REGIMES` (CURATELLE_SIMPLE + CURATELLE_RENFORCEE) avec **description obligatoire** en langage avocat

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Tutelle FR (art. 440 al. 3) | Oui | Backlog — SF-FA-25-04 (régime distinct, représentation continue) |
| Mandat protection future FR (art. 477) | Oui | Backlog — SF-FA-25-05 (anticipation par la personne avant altération) |
| Administration provisoire BE | Oui | Backlog — SF jumelle BE (juge de paix) |
| Frontend curatelle | Oui | SF-FA-25-04-frontend (SF future, contrat API étendu ici) |
| Test IT description SF-140-03 | Oui | Lancé par CI sur les 2 nouvelles entries `legal_referentials` |

### Décision

- [x] Étendu à curatelle simple + curatelle renforcée (2 régimes supplémentaires les plus demandés en pratique)
- [x] Backlog : tutelle, mandat protection future, administration provisoire BE

---

## Impact par domaine métier

Cette feature **est sensible au domaine** : DROIT_FAMILLE FR exclusivement.

- **DROIT_FAMILLE FR** : extension calculator + validation eligibilité curatelle
- **DROIT_FAMILLE BE** : équivalent = administration provisoire — non couvert (backlog)
- **DROIT_DU_TRAVAIL** : 400 (gate inchangé)
- **DROIT_IMMIGRATION** : 400 (gate inchangé)

---

## Parité des domaines métier

Outil de **niveau 5 (scoring + analyse validité)** — extension d'un outil existant déjà niveau 5. Parité :

- **DROIT_FAMILLE** : F-FA-25 cette SF (curatelle simple/renforcée)
- **DROIT_DU_TRAVAIL** : équivalent N/A — la curatelle est exclusivement civile (JCP), un salarié sous curatelle reste salarié sans impact contrat travail (les actes du curateur sur la rupture peuvent être contestés par le juge mais le concept "curatelle salariée" n'existe pas en tant que régime)
- **DROIT_IMMIGRATION** : équivalent N/A — un étranger sous curatelle suit la même procédure JCP, sans impact migratoire spécifique

→ Pas de feature jumelle au backlog. Concept spécifique au DROIT_FAMILLE.

---

## Critères d'acceptation

- [ ] `POST /api/v1/case-files/{id}/majeurs-proteges` accepte un nouveau champ optionnel `incapaciteGestionQuotidienne` (Boolean)
- [ ] La réponse expose 2 nouveaux champs : `eligible` (boolean) + `criteresNonRemplis` (List<String>)
- [ ] Le calculator recommande **CURATELLE_SIMPLE** quand : altération + cert médical + acte patrimonial important + pas d'incapacité quotidienne + pas d'urgence + pas de famille proche/consentement
- [ ] Le calculator recommande **CURATELLE_RENFORCEE** quand : altération + cert médical + `incapaciteGestionQuotidienne = true` (critère pivot art. 472), priorité sur curatelle simple
- [ ] Verdict `ELEVEE` quand tous les critères de la curatelle demandée sont remplis
- [ ] `criteresNonRemplis` liste explicite les critères absents si verdict ≠ ELEVEE
- [ ] `eligible = (criteresNonRemplis.isEmpty())` → boolean dérivé
- [ ] Migration Liquibase **159** :
  - ALTER table `majeurs_proteges_analyses` ADD COLUMN `incapacite_gestion_quotidienne` boolean nullable
  - INSERT 2 entries `legal_referentials` (CURATELLE_SIMPLE UUID `f1a04001-0000-0000-0000-000000000159`, CURATELLE_RENFORCEE UUID `f1a04001-0000-0000-0000-000000000160`) avec **description obligatoire** SF-140-03
- [ ] ≥ 15 tests unitaires + ≥ 5 tests d'intégration (time-box Maven 5 min)
- [ ] Backward compatibility : un POST sans `incapaciteGestionQuotidienne` reste valide (default false)

---

## Périmètre

### Hors scope (explicite)

- Tutelle (SF-FA-25-04 future)
- Mandat de protection future (SF-FA-25-05 future)
- Administration provisoire BE (SF jumelle backlog)
- Frontend curatelle (SF future)
- Génération PDF requête JCP

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `incapaciteGestionQuotidienne` | null en DB, false dans le calcul | Default si non renseigné |

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs |
|-------|-------------|------------------|
| `incapaciteGestionQuotidienne` | Non (default false) | Boolean |

Reste inchangé.

---

## Technique

### Endpoints

Inchangé : `POST + GET /api/v1/case-files/{caseFileId}/majeurs-proteges`. **Le contrat est étendu** (champ optionnel ajouté + 2 nouveaux champs en sortie).

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `majeurs_proteges_analyses` | ALTER | ADD COLUMN `incapacite_gestion_quotidienne` boolean nullable |
| `legal_referentials` | INSERT | 2 entries `MAJEURS_PROTEGES_REGIMES` avec description SF-140-03 |

### Migration Liquibase

`159-add-curatelle-criteres-majeurs-proteges.xml`

UUIDs `legal_referentials` :
- CURATELLE_SIMPLE : `f1a04001-0000-0000-0000-000000000159`
- CURATELLE_RENFORCEE : `f1a04001-0000-0000-0000-000000000160`

### Composants Angular

N/A (SF frontend future).

---

## Plan de test

### Tests unitaires (≥ 15)

1. compute_curatelleSimple_recommandee_ELEVEE — altération + cert + acte patrimoine + sans urgence + sans isolement + sans incap quotidienne → CURATELLE_SIMPLE eligible
2. compute_curatelleSimple_eligible_false_certManquant
3. compute_curatelleSimple_eligible_false_alterationAbsente
4. compute_curatelleSimple_eligible_false_aucunActePatrimonial
5. compute_curatelleRenforcee_recommandee_ELEVEE — incap quotidienne true → CURATELLE_RENFORCEE prioritaire
6. compute_curatelleRenforcee_eligible_false_incapAbsente
7. compute_curatelleRenforcee_priorite_surSimple — même contexte avec/sans incap quotidienne → renforcée si incap, simple sinon
8. compute_curatelleSimple_criteresNonRemplis_listeExplicite
9. compute_curatelleRenforcee_criteresNonRemplis_inclutIncapacite
10. compute_curatelle_eligible_dérivéDeListe — eligible = criteresNonRemplis.isEmpty()
11. compute_incapaciteGestionQuotidienne_propagee_dansResult
12. compute_incapaciteGestionQuotidienne_null_traiteCommeFalse
13. compute_curatelleRenforcee_msg_contient_472
14. compute_curatelleSimple_msg_contient_440
15. compute_priorite_arbreDecision_inchangee — sauvegarde + habilitation toujours prioritaires sur curatelle si critères remplis
16. compute_actesPatrimoniaux_logement_compteCommePat — DECISIONS_LOGEMENT seul suffit pour curatelle simple

### Tests d'intégration (≥ 5)

1. POST FR curatelle simple eligible → 200 + eligible=true + criteresNonRemplis vide
2. POST FR curatelle renforcée incapaciteGestionQuotidienne=true → 200 + regimeOptimalRecommande=CURATELLE_RENFORCEE
3. POST FR curatelle renforcée sans incap → 200 + eligible=false + criteresNonRemplis contient incapacite
4. POST FR upsert : POST sauvegarde puis POST curatelle remplace
5. GET après POST curatelle → 200 + données persistées avec incapaciteGestionQuotidienne

### Description integrity test (CI)

`LegalReferentialDescriptionIntegrityIT` doit passer avec les 2 nouvelles entries (description non-null).

### Isolation workspace

Hérité de SF-FA-25-01.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — pattern inchangé
- [ ] Workspace context — inchangé
- [ ] Plans / limites — N/A
- [ ] Navigation / routing frontend — N/A backend
- [x] **Outil décisionnel métier** — extension du calculator `MajeursProtegesCalculator` pour 2 nouveaux régimes ; pattern préservé, signature étendue avec un champ optionnel
- [x] Aucune autre préoccupation transversale

### Smoke tests E2E concernés

- [x] Aucun — pas d'impact auth/workspace/nav

---

## Dépendances

### Subfeatures bloquantes

- SF-FA-25-01 (mergée PR #605) — table + calculator de base

### Subfeatures parallélisables

- Frontend curatelle (SF future, contrat API figé ici)

### Questions ouvertes impactées

Aucune.

---

## Contrat API (figé)

`POST + GET /api/v1/case-files/{caseFileId}/majeurs-proteges`

**Request body** (POST, champ ajouté en gras) :

```json
{
  "regimeProtectionDemande": "CURATELLE_RENFORCEE",
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
  "incapaciteGestionQuotidienne": true
}
```

Enums inchangés (régime, demandeur, actes).

**Response** (champs ajoutés en gras) :

```json
{
  "caseFileId": "uuid",
  "regimeProtectionDemande": "CURATELLE_RENFORCEE",
  "altertationFacultesMentales": true,
  "...": "...",
  "incapaciteGestionQuotidienne": true,
  "scoreEligibilite": 80,
  "regimeOptimalRecommande": "CURATELLE_RENFORCEE",
  "verdictAcceptabiliteJaf": "ELEVEE",
  "delaiProcedureMoisPrevisionnel": 8,
  "auditionPersonneObligatoire": true,
  "expertisePsyComplementaireRecommandee": false,
  "eligible": true,
  "criteresNonRemplis": [],
  "baseJuridique": "Art. 433-441 + 494-1 et s. Cciv",
  "formule": "Score 80 = acceptabilité ELEVEE...",
  "messages": ["..."],
  "country": "FRANCE"
}
```

---

## Notes et décisions

- **Backward compatibility** : `incapaciteGestionQuotidienne` est optionnel (Boolean) ; absence = false. Aucun frontend existant ne casse.
- **Description SF-140-03** : 2 entries `MAJEURS_PROTEGES_REGIMES` avec description riche en langage avocat (référencement art. 440-441-472, conditions précises, cas d'emploi, articulation avec autres régimes).
- **Migration ID 159** : suit séquentiellement 158 (contestation ARE).
- **UUIDs** : `f1a04001-0000-0000-0000-000000000159` / `…000000000160`. Pattern aligné avec UUID visibility 153 (`f1a04001-0000-0000-0000-ee00000fa251`).
- **Pas de nouvelle table** : la table `majeurs_proteges_analyses` existante couvre tous les régimes via la colonne `regime_protection_demande` ; ALTER ADD COLUMN suffit.
