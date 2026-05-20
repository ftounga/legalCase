# Mini-spec — [F-246 / SF-246-21] Lot Travail FR — champs date/montant extractibles restants (17 outils)

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Référence d'audit : `docs/features/F-246/SF-246-14-audit-prefill-exhaustif.md` §3.1 ligne SF-246-21.
> Référence découpage : `docs/features/F-246/cadrage-decoupage.md` §10 vague C.
> **Modèle de référence** : SF-246-22 (commit `94144dea`).

---

## Identifiant

`F-246 / SF-246-21`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-246-21-lot-travail-fr`

---

## Objectif

Brancher sur des sources backend réelles les champs date/montant/code extractibles
des **17 outils Travail FR** qui disposent déjà d'un pré-fill partiel mais dont les
champs restants sont aspirationnels (source backend manquante dans le record
`TravailExtractedData` et/ou dans le prompt `TRAVAIL_INSTRUCTION`).

---

## Regroupement en sous-objets JSON (stratégie groupée)

Pour éviter un prompt obèse à 17 sous-objets indépendants, les nouveaux champs
sont regroupés en **5 sous-objets thématiques cohérents** dans le prompt et dans
l'extracteur. Chaque sous-objet est emis uniquement si des données pertinentes
sont détectées (null sinon — no-op gracieux).

| Sous-objet JSON | Outils couverts | Justification du regroupement |
|---|---|---|
| `requalification_detection` | `requalification-cdd-cdi`, `requalification-interim-cdi`, `indemnite-precarite-cdd`, `fin-mission-interim` | 4 outils partagent la notion de durée contractuelle + date de fin de mission/contrat. Concepts extraits des mêmes pièces (contrats CDD/intérim, bulletins). |
| `paie_detection` | `conges-payes`, `rappel-salaire`, `heures-sup` (déjà partiellement couvert — complétion), `indemnite-comparatif` | 4 outils autour des montants de paie, jours acquis/pris, périodes de référence. Pièces communes : bulletins de paie, solde de tout compte. |
| `rupture_collective_detection` | `licenciement-economique`, `pse`, `transaction` | 3 outils autour de la rupture collective/négociée : effectifs concernés, dates de projet/signature, montants. |
| `sante_discrimination_detection` | `at-mp` (correction mapping + nouveaux champs), `contestation-are`, `discrimination`, `protection-rp` | 4 outils autour des droits liés à l'état de santé, aux situations protégées, aux décisions administratives. Pièces communes : déclarations AT, décisions France Travail, attestations. |
| `procedure_details_detection` | `refere-prudhomal`, `documents-fin-contrat` | 2 outils autour des aspects procéduraux/documentaires : dates des documents de fin de contrat, montant de provision. |

---

## Champs IA à extraire (pré-remplissage) — tableau outil × champs ajoutés

Légende : **nouveau champ backend** = champ absent du record `TravailExtractedData` et/ou du prompt à ce jour.
Les champs déjà branchés (source backend existante) ne sont pas listés — ils restent inchangés.

### Sous-objet `requalification_detection`

| Outil | Champ formulaire | Champ record (nouveau) | Clé JSON prompt | Type | Nullable | Note |
|---|---|---|---|---|---|---|
| `requalification-cdd-cdi` | `dureeContratMois` | `cddDureeMois` | `cdd_duree_mois` | `Integer` | oui | Durée du dernier CDD en mois — extractible du contrat CDD. Borné [0, 120]. |
| `requalification-cdd-cdi` | `dateFinDernierContrat` | `cddDateFinDernierContrat` | `cdd_date_fin_dernier_contrat` | `String` | oui | Date de fin du dernier CDD (ISO YYYY-MM-DD). NE PAS confondre avec la date de rupture anticipée. |
| `requalification-cdd-cdi` | `newCddDateDebut` | `cddNouveauDateDebut` | `cdd_nouveau_date_debut` | `String` | oui | Date de début du CDD suivant (si succession — ISO). |
| `requalification-cdd-cdi` | `newCddDateFin` | `cddNouveauDateFin` | `cdd_nouveau_date_fin` | `String` | oui | Date de fin du CDD suivant (ISO). |
| `requalification-interim-cdi` | `dureeMissionsTotaleMois` | `interimDureeTotaleMois` | `interim_duree_totale_mois` | `Integer` | oui | Durée totale cumulée des missions d'intérim en mois. Borné [0, 120]. |
| `requalification-interim-cdi` | `dateFinDerniereMission` | `interimDateFinDerniereMission` | `interim_date_fin_derniere_mission` | `String` | oui | Date de fin de la dernière mission d'intérim (ISO). |
| `requalification-interim-cdi` | `newMissionDateDebut` | `interimNouvellesMissionDateDebut` | `interim_nouvelle_mission_date_debut` | `String` | oui | Date de début d'une nouvelle mission (si succession — ISO). |
| `requalification-interim-cdi` | `newMissionDateFin` | `interimNouvellesMissionDateFin` | `interim_nouvelle_mission_date_fin` | `String` | oui | Date de fin d'une nouvelle mission (ISO). |
| `requalification-interim-cdi` | `newMissionEntrepriseUtilisatrice` | `interimEntrepriseUtilisatrice` | `interim_entreprise_utilisatrice` | `String` | oui | Nom ou SIRET de l'entreprise utilisatrice (≤ 200 car.). |
| `indemnite-precarite-cdd` | `dureeCddMois` | `cddDureeMois` | *(mutualisé)* | `Integer` | oui | Réutilise `cddDureeMois` du sous-objet. |
| `indemnite-precarite-cdd` | `totalSalairesBruts` | `cddTotalSalairesBruts` | `cdd_total_salaires_bruts` | `Double` | oui | Total des salaires bruts perçus sur la durée du CDD (€, > 0). Extractible des bulletins. |
| `fin-mission-interim` | `totalRemunerationsBrutes` | `interimTotalRemunerationsBrutes` | `interim_total_remunerations_brutes` | `Double` | oui | Total des rémunérations brutes sur toutes missions (€, > 0). Extractible des bulletins. |
| `fin-mission-interim` | `dureeMissionJours` | `interimDureeMissionJours` | `interim_duree_mission_jours` | `Integer` | oui | Durée de la mission en jours calendaires. Borné [0, 3650]. |
| `fin-mission-interim` | `dateFinMission` | `interimDateFinDerniereMission` | *(mutualisé)* | `String` | oui | Réutilise `interimDateFinDerniereMission`. |

### Sous-objet `paie_detection`

| Outil | Champ formulaire | Champ record (nouveau) | Clé JSON prompt | Type | Nullable | Note |
|---|---|---|---|---|---|---|
| `conges-payes` | `joursAcquisAnnee` | `congesJoursAcquis` | `conges_jours_acquis` | `Integer` | oui | Jours de congés payés acquis sur la période de référence (typiquement bulletins / solde de tout compte). Borné [0, 50]. |
| `conges-payes` | `joursPris` | `congesJoursPris` | `conges_jours_pris` | `Integer` | oui | Jours de congés pris (distincts des acquis). Borné [0, 50]. |
| `rappel-salaire` | `montantSalairePerVerseMensuel` | `rappelSalaireMontantPerverseMensuel` | `rappel_salaire_montant_perverse_mensuel` | `Double` | oui | Montant du salaire effectivement versé (€/mois, > 0). Distinct du montant dû. |
| `rappel-salaire` | `periodeDebut` | `rappelSalairePeriodeDebut` | `rappel_salaire_periode_debut` | `String` | oui | Date de début de la période de rappel (ISO YYYY-MM-DD, premier mois impayé). |
| `rappel-salaire` | `periodeFin` | `rappelSalairePeriodeFin` | `rappel_salaire_periode_fin` | `String` | oui | Date de fin de la période de rappel (ISO, dernier mois impayé). NE PAS confondre avec `dateLicenciement`. |
| `indemnite-comparatif` | `typeRupture` | *(pas de nouveau champ backend — lecture de `motifLicenciement`)* | *(clé existante)* | — | — | Réutilise le mapping `motifLicenciement` → `typeRupture` via whitelist (`LICENCIEMENT`, `LICENCIEMENT_ECONOMIQUE` pour FR ; `LICENCIEMENT_ORDINAIRE` pour BE). Aucun nouveau champ backend nécessaire. |

### Sous-objet `rupture_collective_detection`

| Outil | Champ formulaire | Champ record (nouveau) | Clé JSON prompt | Type | Nullable | Note |
|---|---|---|---|---|---|---|
| `licenciement-economique` | `salarieAncienneteMois` | `ancienneteMoisCalculee` | *(dérivé de `dateEntree` + `dateLicenciement`)* | `Integer` | oui | Ancienneté en mois — dérivée de `dateEntree` et `dateLicenciement` existants, pas de nouveau champ backend. |
| `licenciement-economique` | `salarieAge` | `salarieAgeAnnees` | `salarie_age_annees` | `Integer` | oui | Âge du salarié en années, extractible si pièce d'identité aux pièces. Borné [16, 80]. |
| `pse` | `tailleEntrepriseSalaries` | `pseNombreSalaries` | `pse_nombre_salaries` | `Integer` | oui | Effectif de l'entreprise en nombre de salariés. Borné [0, 100 000]. |
| `pse` | `nombreLicenciementsEnvisages` | `pseNombreLicenciements` | `pse_nombre_licenciements` | `Integer` | oui | Nombre de licenciements envisagés. Borné [0, 100 000]. |
| `transaction` | `dateSignature` | `transactionDateSignature` | `transaction_date_signature` | `String` | oui | Date de signature du protocole transactionnel (ISO). NE PAS confondre avec la date de rupture ou de licenciement. |
| `transaction` | `indemniteTransactionnelleEur` | `transactionIndemniteMontantEur` | `transaction_indemnite_montant_eur` | `Double` | oui | Montant de l'indemnité transactionnelle (€, > 0). |

### Sous-objet `sante_discrimination_detection`

| Outil | Champ formulaire | Champ record (nouveau) | Clé JSON prompt | Type | Nullable | Note |
|---|---|---|---|---|---|---|
| `at-mp` | `dateAccident` | *(correction mapping)* | *(existant `dateLicenciement`)* | — | — | Correction du mapping douteux (note audit). La date accident AT doit être extraite indépendamment de `dateLicenciement`. Nouveau champ `atDateAccident` si présent. |
| `at-mp` | `dateExposition` | `atDateExposition` | `at_date_exposition` | `String` | oui | Date de première exposition au risque (maladie professionnelle, ISO). NE PAS confondre avec la date d'accident. |
| `contestation-are` | `typeDecisionContestee` | `areTypeDecision` | `are_type_decision` | `String` | oui | Type de décision France Travail contestée — whitelist (voir §Whitelist). |
| `contestation-are` | `montantContesteEur` | `areMontantConteste` | `are_montant_conteste` | `Double` | oui | Montant contesté (€, > 0) — extractible de la décision France Travail. |
| `discrimination` | `motifDiscrimination` | `discriminationMotif` | `discrimination_motif` | `String` | oui | Motif de discrimination détecté — whitelist (voir §Whitelist). |
| `discrimination` | `contexteActe` | `discriminationContexte` | `discrimination_contexte` | `String` | oui | Contexte de l'acte discriminatoire — whitelist (voir §Whitelist). |
| `protection-rp` | `datePresumeeRupture` | *(dérivé de `dateLicenciement`)* | *(existant)* | — | — | Réutilise `dateLicenciement` existant. Pas de nouveau champ backend. |
| `protection-rp` | `salaireMensuelBrutEur` | *(existant `salaireBrutMensuel`)* | *(existant)* | — | — | Réutilise `salaireBrutMensuel` existant. Pas de nouveau champ backend. |

### Sous-objet `procedure_details_detection`

| Outil | Champ formulaire | Champ record (nouveau) | Clé JSON prompt | Type | Nullable | Note |
|---|---|---|---|---|---|---|
| `refere-prudhomal` | `montantProvisionDemandeeEur` | `refereMontantProvision` | `refere_montant_provision` | `Double` | oui | Montant de la provision demandée (€, > 0) — chiffrable de la mise en demeure ou des bulletins. |
| `documents-fin-contrat` | `dateCertificatTravail` | `documentsDateCertificatTravail` | `documents_date_certificat_travail` | `String` | oui | Date du certificat de travail (ISO). |
| `documents-fin-contrat` | `dateAttestationFranceTravail` | `documentsDateAttestationFranceTravail` | `documents_date_attestation_france_travail` | `String` | oui | Date de l'attestation France Travail (ISO). NE PAS confondre avec la date de rupture. |
| `documents-fin-contrat` | `dateSoldeToutCompte` | `documentsDateSoldeToutCompte` | `documents_date_solde_tout_compte` | `String` | oui | Date du solde de tout compte (ISO). |

---

## Whitelists de codes

### `areTypeDecision` (contestation-are)

Codes admis (enum `TypeDecisionContestee` frontend) :
`REFUS_INSCRIPTION`, `RADIATION`, `SUPPRESSION_ARE`, `REDUCTION_ARE`, `EXCLUSION_TEMPORAIRE`, `AUTRE`

### `discriminationMotif` (discrimination)

Codes admis (enum `MotifDiscrimination` frontend) :
`SEXE`, `AGE`, `ORIGINE`, `HANDICAP`, `RELIGION`, `ORIENTATION_SEXUELLE`, `GROSSESSE`, `ACTIVITES_SYNDICALES`, `AUTRE`

### `discriminationContexte` (discrimination)

Codes admis (enum `ContexteActe` frontend) :
`REFUS_EMBAUCHE`, `LICENCIEMENT`, `MUTATION`, `SANCTION_DISCIPLINAIRE`, `PROMOTION_REFUSEE`, `REMUNERATION_INFERIEURE`, `HARCELEMENT`, `AUTRE`

---

## Récapitulatif des nouveaux champs backend

| Champ record backend (`TravailExtractedData`) | Type Java | Clé JSON extracteur |
|---|---|---|
| `cddDureeMois` | `Integer` | sous-objet `requalification_detection` |
| `cddDateFinDernierContrat` | `String` | sous-objet `requalification_detection` |
| `cddNouveauDateDebut` | `String` | sous-objet `requalification_detection` |
| `cddNouveauDateFin` | `String` | sous-objet `requalification_detection` |
| `cddTotalSalairesBruts` | `Double` | sous-objet `requalification_detection` |
| `interimDureeTotaleMois` | `Integer` | sous-objet `requalification_detection` |
| `interimDateFinDerniereMission` | `String` | sous-objet `requalification_detection` |
| `interimNouvellesMissionDateDebut` | `String` | sous-objet `requalification_detection` |
| `interimNouvellesMissionDateFin` | `String` | sous-objet `requalification_detection` |
| `interimEntrepriseUtilisatrice` | `String` | sous-objet `requalification_detection` |
| `interimTotalRemunerationsBrutes` | `Double` | sous-objet `requalification_detection` |
| `interimDureeMissionJours` | `Integer` | sous-objet `requalification_detection` |
| `congesJoursAcquis` | `Integer` | sous-objet `paie_detection` |
| `congesJoursPris` | `Integer` | sous-objet `paie_detection` |
| `rappelSalaireMontantPerverseMensuel` | `Double` | sous-objet `paie_detection` |
| `rappelSalairePeriodeDebut` | `String` | sous-objet `paie_detection` |
| `rappelSalairePeriodeFin` | `String` | sous-objet `paie_detection` |
| `salarieAgeAnnees` | `Integer` | sous-objet `rupture_collective_detection` |
| `pseNombreSalaries` | `Integer` | sous-objet `rupture_collective_detection` |
| `pseNombreLicenciements` | `Integer` | sous-objet `rupture_collective_detection` |
| `transactionDateSignature` | `String` | sous-objet `rupture_collective_detection` |
| `transactionIndemniteMontantEur` | `Double` | sous-objet `rupture_collective_detection` |
| `atDateAccident` | `String` | sous-objet `sante_discrimination_detection` |
| `atDateExposition` | `String` | sous-objet `sante_discrimination_detection` |
| `areTypeDecision` | `String` | sous-objet `sante_discrimination_detection` |
| `areMontantConteste` | `Double` | sous-objet `sante_discrimination_detection` |
| `discriminationMotif` | `String` | sous-objet `sante_discrimination_detection` |
| `discriminationContexte` | `String` | sous-objet `sante_discrimination_detection` |
| `refereMontantProvision` | `Double` | sous-objet `procedure_details_detection` |
| `documentsDateCertificatTravail` | `String` | sous-objet `procedure_details_detection` |
| `documentsDateAttestationFranceTravail` | `String` | sous-objet `procedure_details_detection` |
| `documentsDateSoldeToutCompte` | `String` | sous-objet `procedure_details_detection` |

**Total : 32 nouveaux champs backend** (hors réutilisations de champs existants).

Champs réutilisés depuis l'existant (0 nouveau champ backend nécessaire) :
- `protection-rp.datePresumeeRupture` ← `dateLicenciement` (existant)
- `protection-rp.salaireMensuelBrutEur` ← `salaireBrutMensuel` (existant)
- `licenciement-economique.salarieAncienneteMois` ← dérivé `dateEntree`+`dateLicenciement` (existants)
- `indemnite-comparatif.typeRupture` ← mapping `motifLicenciement` (existant)

---

## Comportement attendu

### Cas nominal

1. L'avocat lance une analyse IA d'un dossier de droit du travail FR.
2. Le prompt `TRAVAIL_INSTRUCTION` instruit le LLM d'extraire 5 sous-objets thématiques
   dans `travail_extracted_data` : `requalification_detection`, `paie_detection`,
   `rupture_collective_detection`, `sante_discrimination_detection`, `procedure_details_detection`.
3. Chaque sous-objet est `null` si non applicable (dossier sans CDD, sans AT/MP, etc.).
4. L'extracteur `extractTravailData()` parse les 5 sous-objets, valide chaque champ
   (dates ISO, montants positifs, entiers bornés, whitelists de codes), retourne les
   champs dans le record `TravailExtractedData` (null si invalid).
5. Le DTO frontend `TravailExtractedData` (case-analysis.model.ts) expose les 32 nouveaux champs.
6. Les 17 composants `*-section` lisent les nouveaux champs via leurs helpers
   `*-prefill-rules.ts` mis à jour. `prefillFromAi()` est réel — pas un no-op.
7. Badges `auto_awesome` visibles sur chaque champ pré-rempli. Remise à null au changement manuel.
8. Alertes de cohérence F-IA-03 exposées via `coherenceAlerts` pour :
   - `transaction` : date de signature postérieure à aujourd'hui → avertissement
   - `rappel-salaire` : `montantSalairePerVerse > montantSalaireDu` → incohérence
   - `documents-fin-contrat` : dates de documents antérieures à la date de fin contrat → avertissement

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Sous-objet thématique absent du JSON IA | Champs du sous-objet null ; pré-fill no-op gracieux pour les outils concernés |
| Date non ISO YYYY-MM-DD | Champ null (`isoDateOrNull`) |
| Montant ≤ 0 ou non numérique | Champ null (guard `positiveDoubleOrNull`) |
| Entier hors borne (ex. durée > 120 mois) | Champ null (`boundedIntOrNull`) |
| Code hors whitelist (enum discrimination, ARE) | Champ null (whitelist guard) |
| `rappelSalairePeriodeFin` < `rappelSalairePeriodeDebut` | Champs acceptés (les deux) ; alerte F-IA-03 exposée |
| `transactionDateSignature` > aujourd'hui | Champ accepté ; alerte F-IA-03 exposée |
| Dossier Travail BE | Tous les champs FR-only null (aucun garde-fou global — chaque champ documenté FR uniquement dans le prompt) |

---

## Analyse de cohérence transversale

### Préoccupation — outil décisionnel métier

Cette SF touche 17 outils décisionnels distincts dans le domaine Travail FR.

Invariant mémoire projet : **un outil décisionnel = une situation métier**.
Scan préalable effectué — aucun des 5 sous-objets ne crée de doublon entre outils
(les champs CDD/intérim sont propres à `requalification-*` et `indemnite-precarite-cdd`/`fin-mission-interim`
respectivement, sans overlap avec les outils PSE/transaction).

**Composants impactés (17)** :
`requalification-cdd-cdi-section`, `requalification-interim-cdi-section`,
`indemnite-precarite-cdd-section`, `fin-mission-interim-section`,
`conges-payes-section`, `rappel-salaire-section`, `heures-sup-section`,
`indemnite-comparatif-section`, `licenciement-economique-section`,
`pse-section`, `transaction-section`, `at-mp-section`,
`contestation-are-section`, `discrimination-section`, `protection-rp-section`,
`refere-prudhomal-section`, `documents-fin-contrat-section`.

### Autres préoccupations transversales

- **Auth / Principal** : non impacté.
- **Workspace context** : gate `FRANCE` maintenu outil par outil (cohérent avec l'existant).
- **Plans / limites** : non impacté.
- **Navigation / routing** : non impacté.
- **TOOL_REGISTRY** : 17 outils déjà enregistrés — aucun ajout, aucun renommage.

---

## Critères d'acceptation

| # | Critère | Vérifiable |
|---|---|---|
| 1 | Le record `TravailExtractedData` contient les 32 nouveaux champs (tous nullables, tous non cassants via Builder) | Compilation Java + test `CaseAnalysisResponseTest` |
| 2 | Le prompt `TRAVAIL_INSTRUCTION` documente les 5 sous-objets avec définitions sans ambiguïté + règles anti-confusion dates | Lecture prompt + test `LegalDomainPromptBuilderTest` |
| 3 | `extractTravailData()` parse correctement chaque sous-objet + applique les guards (ISO, positif, borné, whitelist) | `CaseAnalysisResponseTest` — fixture multi-champs + cas invalides |
| 4 | Le DTO frontend `TravailExtractedData` expose les 32 champs réels | Compilation Angular |
| 5 | Les 17 helpers `*-prefill-rules.ts` lisent les champs réels (zéro `as any`, zéro type d'intersection aspirationnel) | `grep -r "as any" src/app/case-files/*-section/*-prefill-rules.ts` |
| 6 | `prefillFromAi()` de chaque composant délègue au helper + gère provenance + badge `auto_awesome` | Jest + inspection composant |
| 7 | Aucun champ pré-rempli ne s'écrase sur une saisie manuelle (`provenance !== 'IA'` → skip) | Jest — scénario "saisie puis aiData change" |
| 8 | Alertes de cohérence F-IA-03 exposées pour transaction/rappel-salaire/documents-fin-contrat | Jest `coherenceAlerts` |
| 9 | Tests backend : ≥ 5 scénarios par sous-objet (nominal, champ absent, invalide, multi-dates concurrent) | `CaseAnalysisResponseTest` |
| 10 | Tests frontend Jest : ≥ 3 scénarios par outil (null, valide, changement manuel) | Jest |
| 11 | Self-check grep pré-commit : zéro champ non câblé bout-en-bout | grep `newChamp` dans helper + composant + record |
| 12 | Smoke E2E `cd e2e && npm test` : ≤ 27 nouveaux échecs (préexistants tolérés) | `npm test` |

---

## Plan de test minimal

### Tests backend (JUnit 5 — `CaseAnalysisResponseTest.java`)

1. **Nominal complet par sous-objet** : JSON IA avec toutes les clés valides → record rempli correctement.
2. **Sous-objet absent** : JSON sans `requalification_detection` → champs CDD/intérim null.
3. **Date invalide** : `cdd_date_fin_dernier_contrat: "2024/13/01"` → `null`.
4. **Montant ≤ 0** : `cdd_total_salaires_bruts: -100` → `null`.
5. **Entier hors borne** : `cdd_duree_mois: 150` → `null`.
6. **Code hors whitelist** : `are_type_decision: "INCONNU"` → `null`.
7. **Multi-dates concurrentes** : dossier avec `rappel_salaire_periode_debut` + `rappel_salaire_periode_fin` + `dateLicenciement` → vérifier que chaque champ est correctement isolé (date de période ≠ dateLicenciement).
8. **Prompt builder** : `LegalDomainPromptBuilderTest` — `buildTravailInstruction()` doit contenir les 5 sous-objets + règles anti-confusion dates.

### Tests frontend (Jest)

Pour chaque outil (17 specs) :
1. `computeXxx({ aiData: null })` → `null`
2. `computeXxx({ aiData: { nouveauChamp: valeurValide } })` → valeur attendue
3. Scénario "provenance" : si `provenance !== 'IA'` → pas d'écrasement
4. Scénario "whitelist" (pour codes enum) : code invalide → `null`

---

## Tables / endpoints / composants impactés

### Backend

- `CaseAnalysisResponse.java` → record `TravailExtractedData` : +32 champs + Builder
- `LegalDomainPromptBuilder.java` → `TRAVAIL_INSTRUCTION` : +5 sous-objets avec règles
- `CaseAnalysisResponse.java` → `extractTravailData()` : 5 méthodes d'extraction + guards

### Frontend

- `case-analysis.model.ts` → interface `TravailExtractedData` : +32 champs
- 17 helpers `*-prefill-rules.ts` : nouveaux `compute*` (interface + fonction + compteur)
- 17 composants `*-section.component.ts` : `prefillFromAi()` + provenance signals

### Aucune modification

- Tables DB (le record est sérialisé en JSON dans `case_analysis_response`)
- Endpoints API (la réponse `CaseAnalysisResponse` est déjà exposée)
- Modèles de calcul (F-DT-XX calculators — les valeurs sont proposées, pas forcées)
- TOOL_REGISTRY (17 outils déjà enregistrés)

---

## Hors périmètre

- Nouveaux outils décisionnels (hors des 17 listés)
- Domaine Famille, Immigration, Travail BE (SF dédiées)
- Refonte du pipeline IA ou des formules de calcul
- Flags de visibilité F-200/201/202/203/204/205
- Validation F-IA-03 générale (F-250)

---

## Invariants anti-gadget (cadrage §5)

1. **Un champ = une définition juridique sans ambiguïté** dans le prompt (ex. "date de fin du dernier CDD" ≠ "date de rupture anticipée").
2. **Nullable + no-op gracieux** : valeur absente/invalide → null, jamais approximatif.
3. **Provenance + badge `auto_awesome`** obligatoires sur chaque champ pré-rempli.
4. **Alerte F-IA-03** pour les incohérences inter-champs détectables (date signature transaction future, montant versé > montant dû).
5. **Format ISO `YYYY-MM-DD` strict** pour toutes les dates — `isoDateOrNull` utilisé.
6. **Fixtures multi-dates** : au moins un test par sous-objet avec plusieurs dates concurrentes.
