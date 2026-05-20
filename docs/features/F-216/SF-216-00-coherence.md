# Cadrage cohérence — F-216 — P2 Famille FR — ~20 outils fréquence haute (étape 0)

**Date** : 2026-05-20
**Skill appliquée** : `ai-skills/feature-coherence-challenger.md`
**Feature parente** : `F-216` — P2 Famille FR — ~20 outils décisionnels fréquence haute
**Sources** :
- `docs/features/F-191/audit-famille-fr-exhaustif.md` — source de vérité périmètre
- `docs/features/F-246/SF-246-14-audit-prefill-exhaustif.md` — mapping champs F-246
- `docs/features/F-210/SF-210-00-coherence.md` + mini-specs F-210 — modèle canonique
- `docs/PRODUCT_SPEC.md` ligne F-216

---

## Contexte et déclencheur

F-210 (P1 Famille FR ✅ 4/4 SF livrées) a traité la **médiation familiale obligatoire** (outil rang 4 Top-10 audit F-191) et l'**acceptation/renonciation succession** (rang 5). F-200 (F-166 généralisée Famille FR ✅) a posé les flags IA booléens de visibilité CONTEXTUAL. F-246 (pré-remplissage IA ✅ 28/28 SF livrées) a massivement étendu `FamilleExtractedData` avec les sous-objets `succession_detection_v2`, `vie_commune_detection`, `filiation_detection_v2`, `protection_divorce_detection_v2`, `regimes_vie_commune_detection_v2`.

F-216 est la **vague P2 Famille FR** : les ~20 outils décisionnels de **fréquence haute** identifiés par l'audit F-191 et mentionnés explicitement dans la ligne PRODUCT_SPEC F-216. Ces outils représentent des situations que tout avocat en droit de la famille rencontre régulièrement (recouvrement pension, adoption intra, délégation AP, audition mineur, partage successoral notarié vs judiciaire, donation entre époux, etc.).

---

## Workflow métier réel — avocat FR en droit de la famille

Un dossier Famille FR suit 5 grands piliers procéduraux, souvent imbriqués :

1. **Dissolution du couple** : divorce (4 cas), séparation, PACS — les outils de base existent (F-FA-07/08/09/10/20/21/22). Lacunes P2 : scoring divorce CM (DELETE 191), prestation compensatoire (DELETE 191), liquidation communauté (DELETE 191).
2. **Patrimoine du couple** : régimes matrimoniaux, partage immobilier (existants), donation entre époux (manquant P2), changement de régime (P3+).
3. **Enfants** : autorité parentale (A19 existant), calendrier garde (A2 existant), pension alimentaire (DELETE 191 — P2 restauration), ARIPA recouvrement (manquant P2), audition mineur 388-1 (manquant P2), délégation AP (manquant P2), retrait AP (P2), désaccords parentaux (A21 existant).
4. **Filiation** : reconnaissance (A15 existant), contestation paternité (A16 existant avec lacunes pré-fill), adoption — variants intra/internationale/plénière/simple (partiels P2), présomption paternité (manquant P2).
5. **Successions / libéralités** : 7 outils existants (F-FA-24-*), 3 manquants P2 : indignité successorale, recel succession, partage successoral notarié vs judiciaire (découpage déjà existant F-FA-24-partage-successoral mais monomode, à enrichir).

---

## Cartographie features existantes ↔ workflow

| Pilier | Outils existants (extraits audit F-191 tableau A) | Lacunes identifiées P2 |
|--------|---------------------------------------------------|------------------------|
| Dissolution du couple | F-FA-07/08/09/10/20/21/22 | Prestation compensatoire (DELETE 191), liquidation communauté (DELETE 191), scoring CM (DELETE 191) |
| Patrimoine du couple | F-FA-05/11/12/15/16 | Donation entre époux (B5.7), séparation biens créances (P3+) |
| Enfants | F-FA-02 (DELETE 191), F-FA-06/12/19/20/21 | Pension alim. restauration, ARIPA, audition mineur 388-1, délégation AP, retrait AP |
| Filiation | F-FA-18-*/27 | Adoption intra-familiale (B8.5), adoption internationale (B8.4), présomption paternité (B7.1) |
| Successions | F-FA-24-* × 7 | Indignité successorale (B6.9), recel (B6.8), donation-partage (B6.12) |

F-246 a pré-branche les champs IA correspondants dans `FamilleExtractedData` (sous-objets `succession_detection_v2`, `vie_commune_detection`, `filiation_detection_v2`, `protection_divorce_detection_v2`, `regimes_vie_commune_detection_v2`) — F-216 **doit les réutiliser au maximum** avant d'étendre le record.

---

## Sélection des ~20 outils P2 Famille FR

La sélection est opérée à partir du tableau de priorité de l'audit F-191 (D.2 Top-10 + items P1 des tableaux B), des outils cités explicitement dans la ligne PRODUCT_SPEC F-216, et des 5 outils DELETE-191 non encore restaurés. Les outils P3+ (mariage formation, séparation biens créances, conventions internationales, outils CASF, TGD, BAR) sont explicitement exclus.

| # SF | tool_id | Situation juridique | Audit F-191 | Priorité | Outils DELETE à restaurer |
|------|---------|--------------------|-----------|-----------|-----------------------|
| SF-216-01/02 | `F-FA-01-prestation-compensatoire` | Prestation compensatoire — capital/rente, critères (art. 270-281 Cciv) | D.2 rang 2 / B3.11 | P0 | **DELETE 191** |
| SF-216-03/04 | `F-FA-02-pension-alimentaire` | Pension alimentaire enfant — calcul, barème INSEE (art. 371-2 Cciv) | D.2 rang 1 / B9.8 | P0 | **DELETE 191** |
| SF-216-05/06 | `F-FA-04-liquidation-communaute` | Liquidation régime communauté légale post-divorce (art. 1467+ Cciv) | D.2 rang 3 / B3.10 | P0 | **DELETE 191** |
| SF-216-07/08 | `F-FA-ARIPA-RECOUVREMENT` | Recouvrement pension impayée ARIPA (L. 581+ CSS) | D.2 rang 6 / B9.9 | P1 | Nouveau |
| SF-216-09/10 | `F-FA-DELEGATION-AP` | Délégation autorité parentale (art. 376-1 Cciv) | D.2 rang 8 / B9.2 | P1 | Nouveau |
| SF-216-11/12 | `F-FA-RETRAIT-AP` | Retrait autorité parentale — violence (art. 378+ Cciv + loi 2/3/2022) | B9.3 / D.4 éclater F-FA-19 | P1 | Nouveau |
| SF-216-13/14 | `F-FA-AUDITION-ENFANT` | Audition mineur JAF art. 388-1 Cciv — conditions, demande, refus | D.2 rang 9 / B9.7 | P1 | Nouveau |
| SF-216-15/16 | `F-FA-ADOPTION-INTRA` | Adoption de l'enfant du conjoint (art. 343-1 al. 2 + 345-1 Cciv) | B8.5 / D.2 rang 7 | P1 | Nouveau |
| SF-216-17/18 | `F-FA-ADOPTION-INTERNATIONALE` | Adoption internationale — Convention La Haye 1993, agrément (art. 370-3+ Cciv) | B8.4 | P1 | Nouveau |
| SF-216-19/20 | `F-FA-INDIGNITE-SUCCESSORALE` | Indignité successorale (art. 726-727 Cciv) | B6.9 | P2 | Nouveau |
| SF-216-21/22 | `F-FA-RECEL-SUCCESSION` | Recel succession — peines civil + pénal (art. 778 Cciv) | B6.8 | P2 | Nouveau |
| SF-216-23/24 | `F-FA-DONATION-ENTRE-EPOUX` | Donation entre époux pendant mariage (art. 1096 Cciv) | B5.7 | P2 | Nouveau |
| SF-216-25/26 | `F-FA-PRESOMPTION-PATERNITE` | Présomption de paternité du mari, désaveu (art. 312+ Cciv) | B7.1 | P2 | Nouveau |
| SF-216-27/28 | `F-FA-PARTAGE-NOTARIAL` | Partage successoral notarié — étapes, calendrier (art. 870+ Cciv) | B6.11 / D.4 éclater F-FA-24-partage-successoral | P2 | Nouveau (découpage) |
| SF-216-29/30 | `F-FA-DONATION-PARTAGE` | Donation-partage (art. 1075-1080 Cciv) | B6.12 | P2 | Nouveau |
| SF-216-31/32 | `F-FA-ACCEPTATION-SUCCESSION` | Acceptation pure et simple / à concurrence actif net / renonciation (art. 768-781 Cciv) | D.2 rang 5 / B6.10 | P0 | **Déjà livré F-210** — exclusion (voir §Exclusions) |
| SF-216-33/34 | `F-FA-MEDIATION-OBLIGATOIRE` | Médiation familiale obligatoire pré-saisine JAF | D.2 rang 4 | P0 | **Déjà livré F-210** — exclusion |

Total outils P2 **retenus** : **14 outils = 28 SF** (backend + frontend pour chaque, sauf outils pur-checklist = 1 SF full-stack).

> **Note sur le découpage** : conformément à la règle CLAUDE.md « un outil décisionnel = une situation métier », les outils `F-FA-ADOPTION-INTRA` et `F-FA-ADOPTION-INTERNATIONALE` sont distincts de `F-FA-18-adoption` existant (plénière + simple générique). Le partage successoral notarié est distinct de `F-FA-24-partage-successoral` (judiciaire existant).

---

## Challenge de cohérence amont — les pré-requis existent-ils ?

| Pré-requis | État | Verdict |
|------------|------|---------|
| Pattern outil décisionnel (Calculator + Section + F-IA-04) | Éprouvé sur 35+ outils Famille FR | ✅ disponible |
| `FamilleExtractedData` (record backend + prompt) | Massivement étendu par F-246 (sous-objets v2) | ✅ disponible — à compléter pour outils P2 non couverts |
| Flags IA CONTEXTUAL Famille FR | F-200 a posé ~30 flags. Plusieurs flags P2 manquent encore (`aripa_recouvrement_envisage`, `delegation_ap_envisagee`, etc.) | ⚠️ ajustement — à créer dans chaque SF backend |
| Seeds `decision_tool_visibility_rules` | Pattern établi, dernière migration utilisée : ~270 | ✅ disponible |
| Numéro de migration disponible | 271+ (libre depuis migration 270 `resiliation-judiciaire-visibility`) | ✅ disponible |
| Outils DELETE-191 (F-FA-01/02/04) | Calculateurs backend probablement partiels ; wrappers frontend manquants | ⚠️ vérification à l'étape Dev de chaque SF |

**Verdict amont : GO avec ajustements** — les ajustements sont : (a) créer les flags IA manquants dans le prompt `FAMILLE_INSTRUCTION` à chaque SF backend, (b) vérifier l'état réel des calculateurs F-FA-01/02/04 avant écriture du code.

---

## Challenge de cohérence aval — la sortie est-elle exploitable ?

Les verdicts des outils décisionnels F-216 alimentent :
- La synthèse décisionnelle du dossier (`app-case-dashboard`) — ✅ chaînage établi par F-FA-05/06/12/19 etc.
- Les pistes stratégiques F-176 — ✅ chaînage établi.
- Le panel F-IA-04 (`app-decisional-tools-panel`) — ✅ existant.

Aucun dead-end. ✅

---

## Exclusions et justifications

| Outil candidat | Exclusion | Motif |
|----------------|-----------|-------|
| `F-FA-ACCEPTATION-SUCCESSION` | Exclu F-216 | **Déjà livré par F-210** (SF-210-03/04, PR mergés) |
| `F-FA-MEDIATION-OBLIGATOIRE` | Exclu F-216 | **Déjà livré par F-210** (SF-210-01/02, PR mergés) |
| `F-FA-152-divorce-consentement-scoring` | Repoussé F-222+ | Wrapper frontend pur (pas de nouveau calculator) — P1 mais hors périmètre P2 |
| `F-FA-153-fourchettes-jaf` | Repoussé F-222+ | Outil présentationnel pur (comparateur) — hors outils décisionnels au sens strict |
| Séparation biens créances (B5.4) | P3+ / F-222 | Fréquence modérée, couvert indirectement par F-FA-04 liquidation |
| Participation aux acquêts (B5.5) | P3+ / F-222 | Régime peu fréquent |
| Changement de régime matrimonial (B5.6) | P3+ / F-222 | Procédure notariale hors urgence |
| Restitution maternelle / accouchement sous X (B7.10) | P3+ | Fréquence très basse |
| Présomption paternité B7.1 | Inclus F-216 | Oui, suffisamment fréquent (désaveu, réfutation) |
| Successions internationales B6.16 | P3+ / F-222 | Convention UE 650/2012 — complexité internationale |
| TGD / BAR / AED / AEMO / OPP | F-222 (P3) | Outils FR-only spécifiques — explicitement dans PRODUCT_SPEC F-222 |
| Mariage / annulation / contrat mariage | P3+ | Branche B.1 = 0% couverture actuelle — hors P2 fréquence haute |
| Concubinage rupture (B2.4) | P3+ | Fréquence réelle mais droit très limité |
| Médiation / procédure participative (B13.4/B13.5) | F-222+ | Procédure transverse, non décisionnel pur |

---

## Invariants anti-gadget (à respecter dans toutes les mini-specs F-216)

1. **Un outil = une situation métier** : adoption intra ≠ adoption internationale ≠ adoption plénière/simple. Ne pas réunir sous un seul outil.
2. **Maximiser la réutilisation des champs F-246** : consulter le mapping §Tableau récapitulatif ci-dessous avant tout ajout de nouveau champ au record.
3. **Flags IA pivot** : chaque outil CONTEXTUAL doit avoir son flag IA dans `FamilleExtractedData` + prompt. Documenter le flag dans la mini-spec.
4. **FR-only strict** : aucun outil P2 ci-dessus n'est mutualisable FR+BE sans analyse distincte — la loi belge est différente sur tous ces points. Gate `country=FRANCE` requis dans chaque controller.
5. **Outils DELETE-191** : avant d'écrire le code F-FA-01/02/04, inventorier l'existant (classe Calculator si partielle, table `_analyses` si existante) pour éviter de recréer l'existant.

---

## Verdict

**GO.** Les pré-requis existent. Les ajustements (flags IA manquants + vérification DELETE-191) sont documentés. Le périmètre des 14 outils retenus est cohérent avec le workflow avocat et l'audit F-191. Les exclusions sont justifiées.

---

## Tableau récapitulatif — mapping outils F-216 / champs F-246

| Outil F-216 | SF backend | SF frontend | Flag F-200 existant | Sous-objet F-246 réutilisé | Champs F-246 réutilisés | Nouveaux champs requis | Source juridique principale |
|-------------|-----------|------------|--------------------|--------------------------|-----------------------|----------------------|----------------------------|
| `F-FA-01-prestation-compensatoire` | SF-216-01 | SF-216-02 | `prestation_compensatoire_envisagee` (à créer) | `vie_commune_detection` | `dureeMariageAnnees`, `revenusAnnuelsEpoux1`, `revenusAnnuelsEpoux2`, `patrimoineCommunEur` | `ageEpoux1`, `ageEpoux2`, `formePrestationDemandee`, `prestationCompensatoireEnvisagee` | art. 270-281 Cciv |
| `F-FA-02-pension-alimentaire` | SF-216-03 | SF-216-04 | `pension_alimentaire_envisagee` (à créer) | `vie_commune_detection`, `filiation_detection_v2` | `revenusAnnuelsEpoux1`, `revenusAnnuelsEpoux2`, `agesEnfantsDetectes`, `nombreEnfantsDetecte` | `pensionAlimentaireEnvisagee`, `pensionAlimentaireBareme` | art. 371-2 Cciv + barème indicatif Cass. |
| `F-FA-04-liquidation-communaute` | SF-216-05 | SF-216-06 | `liquidation_communaute_envisagee` (à créer) | `regimes_vie_commune_detection_v2` | `regimeMatrimonialDetecte`, `valeurCommunauteEurDetectee`, `valeurImmeubleEur`, `capitalRestantDuEur` | `liquidationCommunauteEnvisagee`, `masseCommuneEur`, `soulteEur` | art. 1467-1517 Cciv |
| `F-FA-ARIPA-RECOUVREMENT` | SF-216-07 | SF-216-08 | `aripa_recouvrement_envisage` (à créer) | `vie_commune_detection`, `filiation_detection_v2` | `revenusAnnuelsEpoux1`, `nombreEnfantsDetecte` | `aripaRecouvrement`, `pensionImpayeeMontant`, `dureeImpayeMois`, `voieRecouvrementEnvisagee` | art. L. 581+ CSS / ARIPA |
| `F-FA-DELEGATION-AP` | SF-216-09 | SF-216-10 | `delegation_ap_envisagee` (à créer) | `filiation_detection_v2` | `agesEnfantsDetectes`, `nombreEnfantsDetecte` | `delegationApEnvisagee`, `tiersDesigneLienFamilial`, `motivationDelegation`, `accordParentsPresent` | art. 376-1 Cciv |
| `F-FA-RETRAIT-AP` | SF-216-11 | SF-216-12 | `retrait_ap_envisage` (à créer) | `protection_divorce_detection_v2`, `filiation_detection_v2` | `violencesAllegueesDetectees`, `dangerCaracteriseDetecte`, `agesEnfantsDetectes` | `retraitApEnvisage`, `typeRetraitAp`, `motifRetraitAp`, `decisionsJudiciairesPrecedentesDetectees` | art. 378-381 Cciv + loi 2/3/2022 |
| `F-FA-AUDITION-ENFANT` | SF-216-13 | SF-216-14 | `audition_mineur_envisagee` (à créer) | `filiation_detection_v2` | `agesEnfantsDetectes`, `nombreEnfantsDetecte` | `auditionMineurEnvisagee`, `ageMoinsDe13Ans`, `demandeFormaliseeDetectee`, `refusMotive` | art. 388-1 Cciv + CPC art. 1074-1 |
| `F-FA-ADOPTION-INTRA` | SF-216-15 | SF-216-16 | `adoption_intra_envisagee` (à créer) | `filiation_detection_v2` | `agesEnfantsDetectes`, `adoptantMarieDetected` | `adoptionIntraEnvisagee`, `ageAdoptantIntra`, `ageAdopteIntra`, `consentementEnfantAdopte`, `filiationOrigineModifiee` | art. 343-1 al. 2 + 345-1 Cciv + réforme 21/2/2022 |
| `F-FA-ADOPTION-INTERNATIONALE` | SF-216-17 | SF-216-18 | `adoption_internationale_envisagee` (à créer) | `filiation_detection_v2` | `agesEnfantsDetectes`, `adoptantMarieDetected`, `pupilleEtatDetected` | `adoptionInternationaleEnvisagee`, `agrement2025`, `paysOrigineAdopte`, `conventionLaHayeApplicable`, `exequaturRequis` | art. 370-3+ Cciv + Convention La Haye 29/5/1993 |
| `F-FA-INDIGNITE-SUCCESSORALE` | SF-216-19 | SF-216-20 | `indignite_successorale_envisagee` (à créer) | `succession_detection_v2` | `dateOuvertureSuccessionDetectee`, `conjointSurvivantDetected`, `nbDescendantsDetecte` | `indigniteSuccessoraleEnvisagee`, `motifIndignite`, `indignitePrononceePrecedemment`, `pardonTestamentaireDetecte` | art. 726-727 Cciv |
| `F-FA-RECEL-SUCCESSION` | SF-216-21 | SF-216-22 | `recel_succession_detecte` (à créer) | `succession_detection_v2` | `dateOuvertureSuccessionDetectee`, `montantSuccessionEur`, `montantDonationsRecuesEur` | `recelSuccessionDetecte`, `typeRecelSuspect`, `bienCeleDeclare`, `preuveRecelDetectee` | art. 778 Cciv |
| `F-FA-DONATION-ENTRE-EPOUX` | SF-216-23 | SF-216-24 | `donation_entre_epoux_envisagee` (à créer) | `regimes_vie_commune_detection_v2`, `succession_detection_v2` | `regimeMatrimonialDetecte`, `contratNotarieDetected`, `clauseAttributionIntegraleDetected` | `donationEntreEpouxEnvisagee`, `avantageMatrimonialType`, `revocabiliteDetectee`, `bienDonneType` | art. 1096 Cciv |
| `F-FA-PRESOMPTION-PATERNITE` | SF-216-25 | SF-216-26 | `desaveu_paternite_envisage` (à créer) | `filiation_detection_v2` | `dateNaissanceEnfantDetectee`, `paterniteVraisemblableDetected`, `consentementLibreDuPereDetected` | `desaveuEnvisage`, `dateConclusionMariage`, `dateConceptionEnfant`, `desaveuDelaiRespecte` | art. 312-318 Cciv |
| `F-FA-PARTAGE-NOTARIAL` | SF-216-27 | SF-216-28 | `partage_notarial_envisage` (à créer) | `succession_detection_v2` | `dateOuvertureSuccessionDetectee`, `nombreCoheritiersDetecte`, `montantSuccessionEur`, `typeIndivisionSuccessoraleDetecte` | `partageNotarialEnvisage`, `desaccordCoindivisaires`, `notaireDesigne`, `calendrierPartageEtapes` | art. 870+ Cciv + 816+ Cciv |
| `F-FA-DONATION-PARTAGE` | SF-216-29 | SF-216-30 | `donation_partage_envisagee` (à créer) | `succession_detection_v2`, `regimes_vie_commune_detection_v2` | `nbDescendantsDetecte`, `montantDonationsRecuesEur`, `respectQuotiteDisponibleDetected` | `donationPartageEnvisagee`, `presencePetitsEnfantsParSubstitution`, `donationPartageConjonctiveDetectee`, `valeurPartageTotal` | art. 1075-1080 Cciv |

> **Note** : les champs marqués « à créer » dans la colonne « Nouveaux champs requis » doivent être ajoutés à `FamilleExtractedData` (backend `CaseAnalysisResponse.java`) ET au prompt `FAMILLE_INSTRUCTION`. Les champs F-246 « réutilisés » sont déjà présents dans le record — il suffit que le helper frontend et le calculator les lisent. **Ratio de réutilisation estimé : ~60% des champs par outil proviennent de F-246.**

---

## Périmètre de chaque SF

Conformément au critère de la CLAUDE.md :

- **≥ 1 nouveau champ IA dans `FamilleExtractedData` → 2 SF** (backend + frontend séparées).
- Tous les 14 outils P2 nécessitent au moins 1 nouveau champ IA → **28 SF au total** (SF-216-01 à SF-216-28).
- Exception possible : outil pur-checklist sans champ IA nouveau → SF full-stack unique (à statuer par outil lors des mini-specs individuelles).

---

## Liens

- `docs/features/F-191/audit-famille-fr-exhaustif.md`
- `docs/features/F-246/SF-246-14-audit-prefill-exhaustif.md`
- `docs/features/F-210/SF-210-00-coherence.md`
- `docs/PRODUCT_SPEC.md` ligne F-216
