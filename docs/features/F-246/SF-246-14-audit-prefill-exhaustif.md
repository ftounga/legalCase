# SF-246-14 — Audit exhaustif champ par champ du pré-remplissage IA des outils décisionnels

> **Type** : sous-feature d'**audit** de F-246 « Complétion du pré-remplissage IA
> des outils décisionnels ». **Aucun code applicatif n'est modifié** — le
> livrable est ce document.
> **Source** : extension de périmètre F-246 du 2026-05-19 (décision product
> owner — invariant « tous les champs »).
> **Étapes 0 / 0 bis** : exemptées (SF d'audit, aucun impact écran).

---

## 1. Mini-spec

### 1.1 Objectif

Inventorier **chaque champ saisissable** de **chacun des 103 outils
décisionnels** du produit et statuer, champ par champ, sur l'état de son
pré-remplissage IA, sous le nouveau bar product owner du 2026-05-19.

### 1.2 Contexte — pourquoi un nouvel audit

Le diagnostic du 2026-05-18 (`cadrage-decoupage.md`) n'avait échantillonné que
~32 outils sur ~103 et avait posé deux exclusions qui sont **désormais
caduques** :

- **§2.3** « outils sans champ date / valeur saisissable » — exclus du
  périmètre ;
- **§5.6** champs parqués en saisie manuelle au motif « non factualisable de
  façon fiable ».

**Invariant F-246 (2026-05-19)** : *tout champ saisissable d'un outil décisionnel
doit être pré-rempli par l'IA ; la seule exception admise est que l'information
soit absente des documents uploadés.* Le motif « non factualisable de façon
fiable » n'est plus recevable — l'IA doit tenter l'extraction.

### 1.3 Mécanique de la dette (rappel, vérifiée)

Un champ n'est réellement pré-rempli que si la chaîne complète est branchée :

- **(a)** le champ existe dans le record `*ExtractedData` de
  `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisResponse.java` ;
- **(b)** le prompt `LegalDomainPromptBuilder` instruit le LLM de l'extraire et
  le déclare dans le contrat JSON ;
- **(c)** le helper frontend `*-prefill-rules.ts` lit un chemin réel de ce
  record (pas un champ d'un type d'intersection aspirationnel) ;
- **(d)** le composant `*-section` applique la valeur dans `prefillFromAi()`.

Si (a) ou (b) manque, le helper lit toujours `undefined` → `prefillFromAi()` est
un no-op structurel pour ce champ.

### 1.4 Méthode

Lecture de code exhaustive, croisée sur 5 sources :

1. les **103 composants** `frontend/src/app/case-files/*-section/*.component.ts`
   — inventaire des champs saisissables (signaux de formulaire / FormControl) ;
2. les **103 helpers** `*-prefill-rules.ts` — fonctions `compute*` et accès
   `aiData.X` réels ;
3. les **3 records backend** `TravailExtractedData` / `ImmigrationExtractedData`
   / `FamilleExtractedData` (`CaseAnalysisResponse.java`) ;
4. le **prompt** `LegalDomainPromptBuilder` (clés JSON instruites) ;
5. les **DTO frontend** `case-analysis.model.ts` / `divorce-accepte.model.ts`
   (détection des champs d'intersection aspirationnels).

### 1.5 Périmètre d'audit (les 103 outils)

Le périmètre = les **103 répertoires `*-section` dotés d'un helper
`*-prefill-rules.ts`**. Les 17 répertoires `*-section` SANS helper
(`conclusions`, `case-notes`, `case-deadlines`, `divorce-cm-scoring`,
`divorce-consentement-scoring`, `divorce-ddi-be`, `fourchettes-jaf`,
`immigration-events`, `immigration-strategy-comparator`,
`jurisprudence-citations`, `liquidation-communaute`, `pacte-successoral-be-2018`,
`pension-alimentaire`, `prestation-compensatoire`, `procedure-stage`,
`rupture-amiable-info`, `tribunal-famille-be-mesures-provisoires`) ne sont
**pas des outils décisionnels à pré-remplissage** (panneaux de saisie, fiches,
calculateurs purement manuels, vues agrégées) — hors périmètre F-246.

### 1.6 Vocabulaire de statut (par champ)

| Statut | Définition |
|---|---|
| **pré-rempli** | Chaîne (a)+(b)+(c)+(d) complète — le champ est effectivement renseigné par l'IA quand l'info est aux pièces. |
| **à brancher** | Champ **extractible** d'un document (date, montant, dénombrement, fait juridique factuel, énumération identifiable) **mais** sans source backend : champ absent du record `*ExtractedData` et/ou du prompt. Le helper le déclare souvent comme champ d'intersection aspirationnel. **C'est la dette F-246.** |
| **info structurellement absente** | Champ dont la valeur **n'existe jamais dans les pièces uploadées** : appréciation/arbitrage propre à l'avocat, paramètre de simulation, hypothèse de calcul, choix de stratégie procédurale, donnée saisie manuellement par construction. **Exception admise** au sens de l'invariant 2026-05-19 — justification donnée colonne par colonne. |

### 1.7 Livrable

Tableau de couverture exhaustif outil × champ × statut (§3 à §8), synthèse
chiffrée (§9), proposition de découpage des vagues de remédiation SF-246-15+
(§10).

### 1.8 Hors périmètre

- Toute modification de code applicatif (audit uniquement).
- Le mécanisme de validation de cohérence F-IA-03 (objet de F-250).
- Les flags booléens de visibilité F-200/201/202/203/204/205.

---

## 2. Note de lecture des tableaux

- Les champs **dérivés** (booléens calculés localement à partir d'un champ
  source — ex. `limiteDureeDefinie ← dureeMois != null`) suivent le statut de
  leur source ; ils sont notés *(dérivé de X)*.
- Pour les outils **BE** et **FR mono-pays**, le pré-fill est gardé par
  `workspaceCountry` — cela ne change pas le statut d'un champ (un champ
  pré-rempli reste « pré-rempli »).
- Les outils **purement booléens / checklist** (toggles d'appréciation) ont la
  plupart de leurs champs en « info structurellement absente » : ce sont des
  arbitrages de l'avocat, pas des données extractibles. C'est cohérent avec
  l'invariant — l'exception est *justifiée*, pas le motif vague « non
  factualisable ».

---

## 3. Domaine Travail FR

### 3.1 Outils Travail FR déjà bien couverts

| Outil (`*-section`) | Champs saisissables | Pré-remplis | À brancher | Info absente |
|---|---|---|---|---|
| `anciennete` | conventionCode, dateEntree, salaireBase, congesContrat, primeContrat | **5** | 0 | 0 |
| `at-mp` | dispositif, dateAccident, lieuTravail, declarationEmployeur48h, certificatMedicalInitial, numeroTableau, numeroTableauHorsTableau, delaiPriseEnCharge, dateExposition, tauxFixeCpam, tauxRevendique, expertiseMedicale, datePremierAvisCpam (13) | 1 (`dateAccident` ← mappé `dateLicenciement` — **mapping douteux**, cf. note) | **2** (`lieuTravail`, `dateExposition` — extractibles du CMI / déclaration AT) | 10 (dispositif = choix outil ; taux, délais, présence de pièces = appréciation avocat) |
| `conges-payes` | totalRemunerationPeriode, joursAcquisAnnee, joursPris, salaireMensuelBrut, dateRupture, methodeForcee (6) | **2** (`salaireMensuelBrut`, `dateRupture`) | **2** (`joursAcquisAnnee`, `joursPris` — souvent au solde de tout compte / bulletins) | 2 (`totalRemunerationPeriode` saisi, `methodeForcee` = option calcul) |
| `contestation-are` | typeDecisionContestee, motifContestation, dateNotificationDecision, dateRecoursHierarchique, preuvesProduites, montantConteste, demandeurDejaSaisiTribunal, delaiContestationRespecte (8) | **1** (`dateNotificationDecision`) | **2** (`typeDecisionContestee`, `montantConteste` — extractibles de la décision France Travail) | 5 (motif, recours proposé, preuves, déjà saisi, délai = appréciation/stratégie) |
| `discrimination` | salaireMensuelReference, motifDiscrimination, contexteActe (3) | **1** (`salaireMensuelReference` ← `salaireBrutMensuel`) | **2** (`motifDiscrimination`, `contexteActe` — souvent qualifiables des pièces : courriers, attestations) | 0 |
| `documents-fin-contrat` | dateFinContrat, certificatTravailRemis, dateCertificatTravail, attestationFranceTravailRemise, dateAttestationFranceTravail, soldeToutCompteSigne, dateSoldeToutCompte, salaireMensuelBrut, soldeContestableDelai6mois (9) | **2** (`dateFinContrat`, `salaireMensuelBrut`) | **3** (`dateCertificatTravail`, `dateAttestationFranceTravail`, `dateSoldeToutCompte` — datables si la pièce est uploadée) | 4 (présence/signature des documents, contestabilité = constat avocat) |
| `fin-mission-interim` | salaireMensuelReference, totalRemunerationsBrutes, dureeMissionJours, motifExclusion, dateFinMission (5) | **1** (`salaireMensuelReference`) | **3** (`totalRemunerationsBrutes`, `dureeMissionJours`, `dateFinMission` — extractibles du contrat de mission) | 1 (`motifExclusion` = appréciation) |
| `harcelement-licenciement-nul` | salaireMensuelReference, motifNullite (2) | **2** (`salaireMensuelReference`, `motifNullite` ← `motifNullitePressenti`) | 0 | 0 |
| `heures-sup` | tauxHoraireBrut, heuresSup25, heuresSup50, heuresHorsContingent, tauxMajoration25, tauxMajoration50, heuresSupSemaine, heuresDimancheJoursFeries (8) | **1** (`tauxHoraireBrut` ← `salaireBrutMensuel`) | **2** (`heuresSup25`, `heuresSup50` — chiffrables des bulletins / relevés d'heures) | 5 (taux de majoration = paramètres légaux/conventionnels par défaut ; ventilations = saisie) |
| `inaptitude` | salaireMensuelReference, ancienneteAnnees, origineInaptitude, reclassementRespecte, avisMedecinTravailDate (5) | **5** (tous — `salaireBrutMensuel`, `dateEntree`, `origineInaptitudePressentie`, `reclassementRespecteDetected`, `avisMedecinTravailDate`) | 0 | 0 |
| `indemnite-comparatif` | typeRupture, typeRuptureNote (2) | 0 | **1** (`typeRupture` — qualifiable de la lettre de rupture) | 1 (`typeRuptureNote` = commentaire libre) |
| `indemnite-preavis` | ancienneteAnnees, ancienneteMois, salaireMensuelBrut, conventionCollectiveCode, fonction, exemptionEmployeur, dateRupture (7) | **3** (`salaireMensuelBrut`, `conventionCollectiveCode`, `dateRupture`) | **3** (`ancienneteAnnees`, `ancienneteMois` dérivables de `dateEntree`; `fonction` ← `poste`) | 1 (`exemptionEmployeur` = appréciation) |
| `indemnite-precarite-cdd` | salaireMensuelReference, dureeCddMois, totalSalairesBruts, tauxPrecarite, casExclusion (5) | **1** (`salaireMensuelReference`) | **2** (`dureeCddMois`, `totalSalairesBruts` — chiffrables du contrat CDD / bulletins) | 2 (`tauxPrecarite` = paramètre légal défaut ; `casExclusion` = appréciation) |
| `licenciement` | criteresForm (réponses OUI/NON/INCONNU sur référentiel de critères) | **n** (codes pré-cochés via `detections`) | 0 | reste = appréciation avocat |
| `licenciement-economique` | motifEconomique, preuvesMotif, criteresOrdre, salarieAge, salarieAncienneteMois, salarieChargesFamille, salarieQualitesProf, tentativesReclassement, prioriteReembauche, congeReclassement, dateNotification (11) | **2** (`dateNotification` ← `dateLicenciement`, `motifEconomique` ← `motifLicenciement`) | **2** (`salarieAncienneteMois` dérivable, `salarieAge` extractible si pièce d'identité aux pièces) | 7 (critères d'ordre, charges famille, qualités, reclassement = appréciation) |
| `licenciement-nul-detection` | salarieEnceinte, salarieAccidentTravail, salarieHarceleAvere, salarieDiscrimination, salarieLanceurAlerte, salarieMandatRepresentant, salarieActionJustice, dateAccouchement, dateConsolidationAT, salaireMensuelBrut, ancienneteAnnees, dateNotificationLicenciement (12) | **3** (`salaireMensuelBrut`, `dateNotificationLicenciement` ← `dateLicenciement`, `motifNullitePressenti` → pré-coche les flags) | **2** (`dateAccouchement`, `dateConsolidationAT` — datables des pièces médicales) | 7 (les 7 toggles de cause de nullité = appréciation, partiellement pré-cochés via `motifNullitePressenti`) |
| `prudhome-fiche` | demandeur (profession), employeur, identités, juridiction… | **1** (`profession` ← `poste`) | **multi** (nom/prénom/adresse salarié, nom/adresse employeur, SIRET — tous dans `TravailExtractedData` côté record mais le helper ne lit que `poste`) | reste = saisie de fiche |
| `protection-rp` | statutProtege, dateExpirationMandat, datePresumeeRupture, procedureSuivie, motifLicenciement, salaireMensuelBrut (6) | **1** (`motifLicenciement`) | **2** (`datePresumeeRupture` ← `dateLicenciement`, `salaireMensuelBrut` — disponibles dans le record, non lus) | 3 (`statutProtege`, `dateExpirationMandat`, `procedureSuivie` partiellement extractibles / appréciation) |
| `pse` | tailleEntreprise, nombreLicenciements, periodeJours, dateProjet, modeAdoption, dreetsStatut, dateNotificationDreets, csaeConsulteAvis, contenuMesures (9) | **1** (`dateProjet` ← `dateLicenciement`) | **2** (`tailleEntreprise`, `nombreLicenciements` — chiffrables du dossier PSE) | 6 (mode adoption, statuts DREETS, avis CSE, contenu = procédure/appréciation) |
| `rappel-salaire` | periodeDebut, periodeFin, montantSalaireDuMensuel, montantSalairePerVerseMensuel, conventionCollectiveCode, ancienneteAnneesPrime, indexInseeRevalorise, tauxRevalorisation, methodeCpSurRappel, conventionsFrance (10) | **2** (`montantSalaireDuMensuel` ← `salaireBrutMensuel`, `conventionCollectiveCode`) | **2** (`montantSalairePerVerse`, `periodeDebut`/`periodeFin` — chiffrables/datables des bulletins) | 5 (index, taux, méthode = paramètres de calcul) |
| `refere-prudhomal` | typeRefere, natureCreance, montantProvision, absenceContestationSerieuse, preuvesUrgence, dommageImmediat, tresorerieEmployeurDouteuse, dateMiseEnDemeure, ancienneteContratMois (9) | **2** (`dateMiseEnDemeure`, `ancienneteContratMois` — calculés des champs IA travail) | **1** (`montantProvision` — chiffrable de la mise en demeure / bulletins) | 6 (type, nature, appréciations d'urgence = stratégie/arbitrage) |
| `requalification-cdd-cdi` | motifCddInvoque, motifInterdit, motifInterditType, successionCdd, delaiCarenceRespecte, dureeContratMois, salaireMensuelBrut, dateFinDernierContrat, newCddDateDebut, newCddDateFin, newCddMotif (11) | **1** (`salaireMensuelBrut`) | **4** (`dureeContratMois`, `dateFinDernierContrat`, `newCddDateDebut`, `newCddDateFin` — datables des contrats CDD) | 6 (motifs, succession, carence = appréciation juridique) |
| `requalification-interim-cdi` | motifInterimInvoque, motifInterdit, motifInterditType, successionMissions, delaiCarenceRespecte, dureeMissionsTotaleMois, salaireMensuelBrut, dateFinDerniereMission, memeEntrepriseUtilisatrice, newMissionDateDebut, newMissionDateFin, newMissionMotif, newMissionEntrepriseUtilisatrice (13) | **1** (`salaireMensuelBrut`) | **5** (`dureeMissionsTotaleMois`, `dateFinDerniereMission`, `newMissionDateDebut`, `newMissionDateFin`, `newMissionEntrepriseUtilisatrice` — extractibles des contrats de mission) | 7 (motifs, succession, carence = appréciation) |
| `rupture-conv` | criteresForm (réponses OUI/NON sur RC_CODES) | **n** (codes pré-cochés via `detections`) | 0 | reste = appréciation |
| `rupture-conv-indemnite` | ancienneteAnnees, salaireMensuel (2) | **2** (`salaireMensuel` ← `salaireBrutMensuel`, `ancienneteAnnees` dérivé) | 0 | 0 |
| `transaction` | dateSignature, concessionsEmployeur, concessionsSalarie, indemniteTransactionnelle, salaireMensuelBrut, ancienneteAnnees, renonciationActionExpresse, delaiReflexion15j, rupturePrealable, presenceAvocatAssistance, viceConsentementAllegue (11) | **3** (`salaireMensuelBrut`, `ancienneteAnnees`, `rupture` ← `motifLicenciement`) | **2** (`dateSignature`, `indemniteTransactionnelle` — datables/chiffrables du protocole transactionnel) | 6 (concessions, renonciation, délai, présence avocat, vice = appréciation) |
| `travail-dissimule` | salaireMensuelReference (+ champs de simulation) | **1** (`salaireMensuelReference`) | 0 | reste = paramètres de simulation |

### 3.2 Outils Travail FR identifiés en dette F-246 (déjà traités SF-246-01/02)

| Outil | Champs saisissables | Pré-remplis | À brancher | Info absente | Note |
|---|---|---|---|---|---|
| `procedure-nullite-licenciement` | convocationEnvoyee, dateConvocationPresentee, dateEntretienPrealable, entretienTenu, dateNotificationLicenciement, lettreLicenciementEcrite, lettreMotivee, motivationSuffisante, motivationCommentaire, licenciementPourMotifGrave, licenciementCollectif, procedureCseRespectee, conventionCollectiveApplicable, conventionCollectiveRespectee, conventionCollectiveCommentaire (15) | **8** (les 8 champs SF-246-01 — branchés) | **3** (`licenciementPourMotifGrave`, `licenciementCollectif`, `procedureCseRespectee` — qualifiables de la lettre de licenciement) | 4 (commentaires libres + 2 champs convention collective = appréciation) |
| `non-concurrence` | clausePresenteContrat, limiteTerritoireDefini, territoireDescription, limiteDureeDefinie, dureeMois, limiteObjetDefini, objetDescription, contrepartieFinancierePresente, contrepartieMontantMensuel, salaireMensuelBrut, secteurActivite, datePriseEffet (12) | **8** (SF-246-02) | **4** (`limiteObjetDefini`, `objetDescription` — extractibles de l'art. de la clause ; `secteurActivite`, `datePriseEffet` — **SF-246-13 en cours**) | 0 |

### 3.3 `travail-procedure` — anomalie connue

| Outil | Champs saisissables | Pré-remplis | À brancher | Info absente |
|---|---|---|---|---|
| `travail-procedure` (F-136 calendrier procédural) | typeProcedure, dateDeclencheur (2) | **0** | **2** (`typeProcedure`, `dateDeclencheur` — le helper lit `procedureTravailDetectee` / `dateDeclencheurProcedure`, **champs aspirationnels absents du record `TravailExtractedData`**, cast permissif documenté « anomalie connue » dans le helper) | 0 |

---

## 4. Domaine Travail BE

| Outil | Champs saisissables | Pré-remplis | À brancher | Info absente | Note |
|---|---|---|---|---|---|
| `motif-grave-be` | dateConnaissanceFait, dateNotificationRupture, dateNotificationMotifs, anciennetteAnnees, salaireMensuelReference (5) | **2** (`dateNotificationRupture` ← `dateLicenciement`, `salaireMensuelReference` ← `salaireBrutMensuel`) | **2** (`dateConnaissanceFait`, `dateNotificationMotifs` — datables de la lettre de licenciement BE / notification des motifs ; concepts du délai de 3 j ouvrables CCT) | 1 (`anciennetteAnnees` partiellement dérivable) |
| `avantages-conventionnels-be` | salaireMensuelBrut, joursTravaillesAnneePrecedente, anciennetteMois, commissionParitaire, annee, doublePeculeVacancesPercu, primeFinAnneePrevue, ecoChequesPrevu, ecoChequesUtilisation, chequesRepasPrevu, joursPrestesEffectifs (11) | **1** (`salaireMensuelBrut`) | **3** (`commissionParitaire` ← `conventionCollective`, `joursTravailles`, `joursPrestes` — chiffrables des fiches de paie BE) | 7 (peculé/prime/chèques prévus = paramètres conventionnels par défaut + appréciation) |
| `credit-temps-be` | regime, motif, ancienneteEntrepriseMois, tailleEntrepriseEtp, dureeReductionType, ageDemandeurAnnees, dateDemande (7) | **1** (`ageDemandeurAnnees` — SF-246-05 branché) | **2** (`ancienneteEntrepriseMois` dérivable de `dateEntree`; `dateDemande` — datable de la demande de crédit-temps) | 3 (`regime`, `motif`, `dureeReductionType`, `tailleEntrepriseEtp` = choix/paramètre de simulation) |
| `tribunal-travail-fiche` | commissionParitaire, typeContrat, dateDebut, dateFin, motifRupture (+ identités fiche) | **5** (`conventionCollective`, `typeContrat`, `dateEntree`, `dateLicenciement`, `motifLicenciement`) | **multi** (identités salarié/employeur, BCE — présentes dans `TravailExtractedData`, non lues par le helper) | reste = saisie de fiche |

---

## 5. Domaine Immigration FR

### 5.1 Outils Immigration FR

| Outil | Champs saisissables | Pré-remplis | À brancher | Info absente | Note |
|---|---|---|---|---|---|
| `oqtf-avec-delai` | dateNotificationOqtf, motifOqtf, recoursForme, dateRecours (4) | **3** (`dateNotificationOqtf`, `motifOqtf` ← `motifOqtfCode`, `recoursForme` ← `recoursFormeDetected`) | 0 | 1 (`dateRecours` = date de l'acte à venir, saisie) |
| `oqtf-sans-delai` | dateHeureNotificationOqtf, motifSansDelai, placementCra, recoursForme, dateHeureRecours (5) | **4** (`dateHeureNotificationOqtf`, `motifSansDelai` ← `motifOqtfCode`, `placementCra` ← `placementCraDetected`, `recoursForme`) | 0 | 1 (`dateHeureRecours` = acte à venir) |
| `jld-retention` | dateNotificationPlacement, motifPlacement, recoursForme, dateRecours (4) | **2** (`dateNotificationPlacement` ← `dateNotificationOqtf`/`dateNotificationDecisionContestee`, `motifPlacement` ← `motifOqtfCode`) | **1** (`recoursForme` — extractible si recours déjà formé) | 1 (`dateRecours` = acte à venir) |
| `dublin-recours` | dateNotificationDecisionTransfert, etatMembreResponsable, motifTransfert, recoursForme, dateRecours (5) | **1** (`dateNotificationDecisionTransfert` ← `dateNotificationDecisionContestee`/`dateNotificationOqtf`) | **2** (`etatMembreResponsable`, `motifTransfert` — extractibles de la décision de transfert Dublin) | 2 (`recoursForme`, `dateRecours` = acte à venir) |
| `crrv-refus-visa` | dateNotificationRefus, typeVisa, motifRefus, recoursForme, dateRecours (5) | **1** (`dateNotificationRefus` ← `dateNotificationDecisionContestee`) | **2** (`typeVisa`, `motifRefus` — extractibles de la décision de refus de visa) | 2 (`recoursForme`, `dateRecours` = acte à venir) |
| `immigration-recours` | recoursType, dateNotification, nom, prenom, nationalite, adresse, autorite, dateDecision, reference, exposeFaits (10) | **0** (helper lit `dateNotificationDecisionContestee`, `typeRecoursCode` mais le composant n'applique pas — cf. note) | **6** (`recoursType`, `dateNotification`, `nom`, `prenom`, `nationalite`, `dateDecision`, `reference` — tous extractibles de la décision contestée + identité) | 2 (`adresse`, `exposeFaits` = saisie de l'avocat) |
| `immigration-title-decision` | country, nationaliteUe, motif, duree (4) | **1** (`nationaliteUe`) | **2** (`motif` ← `typeTitreSejour`, `duree` — qualifiables des pièces) | 1 (`country` = contexte workspace) |
| `immigration-work-right` | titreType, country (2) | **1** (`titreType` ← `typeTitreSejourCode`) | 0 | 1 (`country` = contexte workspace) |
| `changement-statut` | titreActuel, titreEnvisage, dureeRestanteSurTitreActuelMois, documentJustificatifFourni, remunerationContrat, casierJudiciaireVierge (6) | **2** (`titreActuel` ← `typeTitreSejour`, `dureeRestante` ← dérivée de `dateExpirationTitre`) | **2** (`titreEnvisage`, `remunerationContrat` — extractibles du projet / contrat) | 2 (`documentJustificatifFourni`, `casierJudiciaireVierge` = constat avocat) |
| `aes-etudiant` | dateEntreeFrance, dureePresenceMois, anneesScolariteConsecutives, niveauEtudesActuel, resultatsAcademiques, inscriptionEtablissementReconnu, moyensSubsistance, menaceOrdrePublic, parcoursCoherent, dateDepotDemande (10) | **1** (`dateDepotDemande` ← `dateDepotProcedure`) | **5** (`dateEntreeFrance`, `dureePresenceMois`, `anneesScolariteConsecutives`, `niveauEtudesActuel`, `inscriptionEtablissementReconnu` — extractibles des certificats de scolarité / passeport) | 4 (`resultatsAcademiques`, `moyensSubsistance`, `menaceOrdrePublic`, `parcoursCoherent` = appréciation) |
| `aes-famille` | dateEntreeFrance, dureePresenceMois, enfantsScolarisesFrance, dureeScolaritePlusAncienEnfant, dateDepotDemande (5) | **1** (`dateEntreeFrance`) | **3** (`dureePresenceMois` dérivable, `dureeScolaritePlusAncienEnfant` extractible, `dateDepotDemande` ← `dateDepotProcedure`) | 1 (`enfantsScolarisesFrance` = constat) |
| `aes-humanitaire` | dateEntreeFrance, motifHumanitaireDominant, dateDepotDemande (3) | **1** (`dateDepotDemande` ← `dateDepotProcedure`) | **2** (`dateEntreeFrance`, `motifHumanitaireDominant` — extractibles des pièces) | 0 |
| `aes-metiers-tension` | dateEntreeFrance, moisActiviteSalariee24Mois, metierEstEnTension, codeMetier, menaceOrdrePublic, contratOuPromesseValide, dateDepotDemande (7) | **1** (`dateDepotDemande` ← `dateDepotProcedure`) | **3** (`dateEntreeFrance`, `moisActiviteSalariee24Mois`, `codeMetier` — extractibles des bulletins / contrat) | 3 (`metierEstEnTension`, `menaceOrdrePublic`, `contratOuPromesseValide` = appréciation) |
| `naturalisation` | voieNaturalisation, dureeResidenceReguliere, dureeMariage, cohabitationContinue, ageDemandeur, ascendantDirectFrancais, parentAcquiertNationalite, vitAvecParentAcquereur, ancienFrancais, casierJudiciaireVierge, assimilationLangueB1, ressourcesStables, oppositionGouvernementale, etudesSuperieuresFrance (14) | **0** | **3** (`dureeResidenceReguliere`, `dureeMariage`, `ageDemandeur` — extractibles/dérivables des titres + acte de mariage + pièce d'identité) | 11 (voie, cohabitation, ascendance, langue, ressources = appréciation/constat) |
| `mineurs-immigration` | dispositifVise, dateNaissance, dateEntreeFrance, parentRegulier, isolementAvere, motifOrdrePublic, nationalite (7) | **0** | **3** (`dateNaissance`, `dateEntreeFrance`, `nationalite` — extractibles de l'acte de naissance / passeport ; `nationalite` existe dans `ImmigrationExtractedData`, non lu) | 4 (dispositif, parent régulier, isolement, ordre public = appréciation) |
| `regime-algerien` | voieDemande, nationaliteAlgerienne, documentEtatCivilOriginal, presenceReguliereMois, casierJudiciaireVierge, visaLongSejourValide, conjointFrancais, parentEnfantFrancais, neEnFrance, arriveeAvant13Ans, contratTravailValide, ressourcesSuffisantes, logementDecent, nombrePersonnesFoyer (14) | **0** | **2** (`nationaliteAlgerienne` dérivable de `nationalite`, `presenceReguliereMois` extractible) | 12 (voie, documents, statuts familiaux, ressources = appréciation/constat) |
| `asile-avance` | dispositifAsile, empreintesEurodac, demandeurEnFuite, paysOrigineListeSure, fraudeDocumentaire, refusPriseEmpreintes, dateDecisionAnterieure, elementsNouveaux, motifsExclusion, presenceReguliere, traitementsGravesEtablis (11) | **0** (helper lit `typeProcedureDetectee`) | **1** (`dateDecisionAnterieure` — datable d'une décision OFPRA/CNDA antérieure) | 10 (dispositif, empreintes, fuite, fraude, éléments nouveaux = appréciation/procédure) |
| `mesures-eloignement` | dispositif, motifMenace, procedureCommissionRespectee, urgenceAbsolueJustifiee, dureeCircularitePrecaire, dureePresenceIrreguliereMois, comportementAggravant, recoursDelai (8) | **0** (helper lit `typeProcedureDetectee`) | **1** (`dureePresenceIrreguliereMois` — chiffrable des pièces) | 7 (dispositif, motif menace, urgence, comportement = appréciation) |
| `referes-admin` | typeRefere, decisionContestee, dateNotificationDecision, urgenceCaracterisee, atteinteLiberteFondamentale, doutesSerieuxLegalite, preuvesUrgence, demandeurDejaPrived (8) | **0** (helper lit `dateNotificationDecisionContestee`, `typeRecoursCode`, `transfertImminentDetected` — non appliqués) | **2** (`dateNotificationDecision`, `decisionContestee` — extractibles de la décision contestée) | 6 (type référé, appréciations d'urgence/légalité = stratégie/arbitrage) |
| `immigration-checklist` | titreType (pré-sélection régime de checklist) | **1** (`inferredChecklistType` via `@Input` setter) | 0 | reste = checklist de pièces |
| `victime-violences-l4256` | dateOrdonnanceProtection, juridiction, dureeProtectionMois, dateExpirationProtection, enfantsAcharge, nationalite (6) | **1** (`dateOrdonnanceProtection` — SF-246-04) | **5** (`juridiction`, `dureeProtectionMois`, `dateExpirationProtection`, `enfantsAcharge`, `nationalite` — tous extractibles de l'ordonnance de protection JAF et de l'état civil ; le helper les documente « non factualisables de façon fiable » — **motif désormais caduc**) | 0 |

> **Note `immigration-recours` / `referes-admin`** : les helpers exposent des
> `compute*` lisant des champs réels du record, mais le composant n'a pas de
> `prefillFromAi()` runtime appliquant ces valeurs (ou applique via un chemin
> partiel). À traiter comme « à brancher » côté composant en plus du backend.

---

## 6. Domaine Immigration BE

| Outil | Champs saisissables | Pré-remplis | À brancher | Info absente |
|---|---|---|---|---|
| `annexe13-be` | dateNotificationAnnexe13, delaiDepartImposeJours, motifOqt, transfertImminent, recoursForme, typeRecours, dateRecours (7) | **4** (`dateNotificationAnnexe13`, `delaiDepartImposeJours`, `motifOqt` ← `motifOqtCodeBe`, `transfertImminent` ← `transfertImminentDetected`) | 0 | 3 (`recoursForme`, `typeRecours`, `dateRecours` = acte à venir / choix) |
| `belgian-9bis` | dateEntreeBelgique, dureePresenceMois, circonstancesExceptionnelles, liensFamiliauxBe, liensProfessionnels, scolariteEnfantsBe, menaceOrdrePublic, dateDepotDemande (8) | **1** (`dateDepotDemande` ← `dateDepotProcedure`) | **2** (`dateEntreeBelgique`, `dureePresenceMois` — extractibles/dérivables des pièces) | 5 (circonstances, liens, scolarité, ordre public = appréciation) |
| `belgian-9ter` | dateDebutSymptomes, maladieGraveCertifiee, soinsNecessairesDisponiblesBe, soinsInaccessiblesPaysOrigine, menaceOrdrePublic, dateDepotDemande (6) | **0** | **2** (`dateDebutSymptomes`, `dateDepotDemande` — datables du certificat médical / de la demande) | 4 (maladie certifiée, disponibilité des soins, ordre public = appréciation médicale/juridique) |
| `belgian-40bis` | lienFamilial, regroupantCitoyenUe, regroupantActiviteCategorie, ressourcesSuffisantes, assuranceMaladieUe, logementSuffisant, menaceOrdrePublic, dateDepotDemande (8) | **2** (`dateDepotDemande` ← `dateDepotProcedure`, `regroupantCitoyenUe` ← `nationaliteUe`) | **1** (`lienFamilial` — extractible de l'acte de mariage / de naissance) | 4 (activité, ressources, assurance, logement, ordre public = constat/appréciation) |
| `belgian-40ter` | lienFamilial, regroupantBelge, revenusMensuelsNets, seuil120PctRis, assuranceMaladie, logementSuffisant, menaceOrdrePublic, dateDepotDemande (8) | **0** | **3** (`lienFamilial`, `revenusMensuelsNets`, `dateDepotDemande` — extractibles des pièces) | 5 (regroupant belge, seuil RIS, assurance, logement, ordre public = constat/paramètre) |

---

## 7. Domaine Famille FR

> **Constat structurel majeur (cadrage §5.3 confirmé exhaustivement)** : le
> record backend `FamilleExtractedData` ne contient **que** les champs
> date/montant/dénombrement des lots SF-246-06 à SF-246-10 (successions, régimes
> matrimoniaux, vie commune, filiation, autorité parentale) plus
> `dateAcceptationPV` (F-239). **Tous les champs `*Detected` / `*Detecte` de
> nature booléenne, énumérée ou liste de chaînes lus par les helpers Famille
> sont aspirationnels** : ils figurent dans le DTO frontend
> `FamilleExtractedData` (`divorce-accepte.model.ts`) mais **n'ont aucune
> contrepartie dans le record backend ni dans le prompt**. `prefillFromAi()`
> les lit toujours `undefined`.

### 7.1 Famille FR — successions / libéralités (lot SF-246-06)

| Outil | Champs saisissables | Pré-remplis | À brancher | Info absente |
|---|---|---|---|---|
| `acceptation-renonciation` | dateOuvertureSuccession, qualiteHeritier, actifBrut, passif, actesEquivalentAcceptation, inventaireRealise, dettesIncertaines, intentionExprimee (8) | **3** (`dateOuvertureSuccession`, `actifBrut`, `passif` — SF-246-06) | **3** (`qualiteHeritier`, `actesEquivalentAcceptation`, `dettesIncertaines` — le helper lit `qualiteHeritierDetectee`, `actesEquivalentAcceptationDejaPosesDetected`, `dettesIncertainesDetected` — **aspirationnels, absents du record/prompt**) | 2 (`inventaireRealise`, `intentionExprimee` = constat/arbitrage) |
| `partage-successoral` | modePartageDemande, nombreCoheritiers, consentementsTous, presenceImmeubles, accordsValuation, desaccordPersistant, dateDeces, valeurMasse (8) | **4** (`modePartageDemande`, `nombreCoheritiers`, `dateDeces`, `valeurMasse` — SF-246-06) | 0 | 4 (consentements, immeubles, accords, désaccord = appréciation) |
| `reserve-heriditaire` | nombreEnfants, conjointSurvivant, montantSuccession, montantLibsTotal, dateOuvertureSuccession, qualiteDuDemandeur (6) | **4** (`nombreEnfants` ← `nombreEnfantsSuccessionDetecte`, `montantSuccession`, `montantLibsTotal`, `dateOuvertureSuccession` — SF-246-06) | **2** (`conjointSurvivant`, `qualiteDuDemandeur` — le helper lit `conjointSurvivantDetected`, `qualiteDuDemandeurReserveDetecte` — **aspirationnels**) | 0 |
| `rapport-succession` | qualiteHeritier, donationsRecues, dateDonation, valeurAuJourPartage, donationDispenseDeRapport, naturePresumeeNonRapportable (6) | **3** (`donationsRecues`, `dateDonation`, `valeurAuJourPartage` — SF-246-06) | **3** (`qualiteHeritier`, `donationDispenseDeRapport`, `naturePresumeeNonRapportable` — helper lit `qualiteHeritierRapportDetectee`, `donationDispenseDeRapportDetected`, `naturePresumeeNonRapportableDetected` — **aspirationnels**) | 0 |
| `indivision-successorale` | typeIndivision, dateOuvertureSuccession, nbHeritiers, valeurPatrimoineIndivis, valeurBienOccupe, consentementsTous, occupationExclusive, actesAdministrationContestes, demandePartage (9) | **2** (`typeIndivision` ← `typeIndivisionSuccessoraleDetecte`, `dateOuvertureSuccession` — SF-246-06) | **2** (`nbHeritiers` extractible — l'acte de notoriété, `valeurPatrimoineIndivis` chiffrable) | 4 (consentements, occupation, actes contestés, demande = appréciation) |
| `devolution-legale` | conjointSurvivant, nbDescendants, tousDescendantsCommuns, nbDescendantsPredecedes, nbPetitsEnfants, pereVivant, mereVivant, nbFreresSoeurs, nbFreresSoeursPredecedes, ascendantsOrdinaires, collateralOrdinaires, optionConjoint (12) | **2** (`nbDescendants`, `nbFreresSoeurs` — SF-246-06) | **2** (`conjointSurvivant`, `tousDescendantsCommuns` — helper lit `conjointSurvivantDetected`, `tousDescendantsCommunsAvecConjointDetected` — **aspirationnels**) | 8 (prédécès, petits-enfants, ascendants, collatéraux, option = arbre généalogique / arbitrage rarement aux pièces) |
| `donation` | formeDonation, dateDonation, ageDonateur, saineDEsprit, capaciteDonateur, capaciteRecipiendaire, consentementLibre, objetDetermine, respectFormalisme, respectQuotiteDisponible, acteAuthentique, acceptationExpresse, remiseEffective, bienMeuble, intentionLiberale, actePrincipalNeutre, apparenceOnerueuse, prixIncoherent, vicesConsentementDol, erreurSubstantielle, ingratitudeAvere, inexecutionCharge (22) | **1** (`dateDonation` — SF-246-06) | **3** (`formeDonation`, `saineDEsprit`, `respectQuotiteDisponible` — helper lit `formeDonationDetectee`, `saineDEspritDonateurDetected`, `respectQuotiteDisponibleDetected` — **aspirationnels** ; `ageDonateur` extractible) | 17 (les ~17 toggles de conditions de validité = appréciation juridique) |
| `testament-validite` | formeTestament, dateRedaction, ageTestateurRedaction, saineDEsprit, majeurProtegeAvecAssistance, ecritureManuscritIntegrale, dateComplete, signatureTestateur, presenceNotaireEtTemoins, dicteEnPresence, lectureFinale, signaturesCompletes, remiseSousPliCache, declarationDevant2Temoins, acteSuscriptionNotaire, respecteFormeWashington, vicesConsentementDol, erreurSubstantielle, testamentPosterieurContradictoire, dechirureVolontaireOriginal, legsExcedeQuotiteDisponible (21) | **1** (`dateRedaction` ← `dateRedactionTestamentDetectee` — SF-246-06) | **3** (`formeTestament`, `saineDEsprit`, `legsExcedeQuotiteDisponible` — helper lit `formeTestamentDetectee`, `saineDEspritTestateurDetected`, `legsExcedeQuotiteDisponibleDetected` — **aspirationnels** ; `ageTestateur` extractible) | 16 (les ~16 toggles de validité formelle = appréciation juridique) |

### 7.2 Famille FR — régimes matrimoniaux / liquidation (lot SF-246-07)

| Outil | Champs saisissables | Pré-remplis | À brancher | Info absente |
|---|---|---|---|---|
| `communaute-universelle` | dispositifAnalyse, contratNotarie, inscriptionEtatCivil, consentementLibreDesEpoux, respectReserveHereditaire, clauseAttributionIntegrale, enfantsNonCommuns, valeurCommunaute (8) | **1** (`valeurCommunaute` ← `valeurCommunauteEurDetectee` — SF-246-07) | **3** (`contratNotarie`, `clauseAttributionIntegrale`, `enfantsNonCommuns` — helper lit `contratNotarieDetected`, `clauseAttributionIntegraleDetected`, `enfantsNonCommunsDetected` — **aspirationnels** ; extractibles du contrat de mariage) | 3 (dispositif = choix outil ; inscription, consentement, réserve = appréciation) |
| `recompenses` | regimeMatrimonial, operations (liste) (2) | **1** (`regimeMatrimonial` ← `regimeMatrimonialDetecte` — SF-246-07) | 0 | 1 (`operations` = saisie ligne à ligne) |
| `partage-judiciaire` | pvDifficultesEtabli, tentativeAmiableEpuiseuee, typeBienIndivision, nombreCoindivisaires, desaccordMotive, valeurEstimeeBiens (6) | **2** (`nombreCoindivisaires`, `valeurEstimeeBiens` ← `valeurBiensIndivisionEur` — SF-246-07) | **2** (`pvDifficultesEtabli`, `tentativeAmiableEpuiseuee` — helper lit `pvDifficultesEtablisDetected`, `tentativeAmiableEpuiseueeDetected` — **aspirationnels**) | 2 (`typeBienIndivision`, `desaccordMotive` = appréciation) |

### 7.3 Famille FR — vie commune / séparation / PACS / protection (lot SF-246-08)

| Outil | Champs saisissables | Pré-remplis | À brancher | Info absente |
|---|---|---|---|---|
| `pacs-dissolution` | dateConclusionPacs, modeDissolution, dateDissolution, dureeUnionAnnees, regimeBiens, patrimoineCommunSignificatif, creancesAlleguees, enfantsCommuns, dateNotificationPartenaire (9) | **3** (`dateConclusionPacs`, `modeDissolution` ← `modeDissolutionPacsDetecte`, `regimeBiens` ← `regimeBiensPacsDetecte` — SF-246-08) | **2** (`dateDissolution`, `dateNotificationPartenaire` — datables des pièces) | 4 (`dureeUnion` dérivable mais saisie, `patrimoineCommunSignificatif`, `creancesAlleguees`, `enfantsCommuns` = appréciation/constat) |
| `separation-corps` | modeProcedure, dateJugementSeparationCorps, dateRequeteConversion, dureeSeparationAnnees, consentementMutuelConversion, patrimoineCommun, enfantsMineurs, demandeReconciliationFormulee (8) | **2** (`dateJugement` ← `dateSeparation`, `patrimoineCommunEur`/`patrimoineCommun`) | **1** (`dateRequeteConversion` — datable) | 5 (mode, durée, consentement, enfants, réconciliation = appréciation/saisie) |
| `indivision` | dateOrigineIndivision, natureBiens, valeurEstimeeTotale, nbIndivisaires, quotesPart, tentativesPartageAmiable, consentementPartageGlobal, occupationBien, indivisionDureeAnnees, demandeMesuresConservatoires, conflitOuvert (11) | **1** (`dateOrigineIndivision` ← `dateSeparation`) | **2** (`valeurEstimeeTotale`, `nbIndivisaires` — chiffrables des pièces) | 8 (nature, quotes-parts, tentatives, consentement, occupation, durée, conflit = appréciation) |
| `ordonnance-protection` | dateRequete, violencesAlleguees, preuvesViolences, dangerImmediat, presenceEnfants, logementCommun, victimeFinanciairementDependante, demandeurDejaProtege, demandeMesures (9) | **1** (`dateRequete` ← `dateRequeteOP` — SF-246-08) | **6** (`violencesAlleguees`, `preuvesViolences`, `dangerImmediat`, `presenceEnfants`, `logementCommun`, `victimeFinanciairementDependante` — helper lit les `*Detectees`/`*Detected` correspondants — **aspirationnels** ; tous qualifiables des pièces : plaintes, certificats, attestations) | 2 (`demandeurDejaProtege`, `demandeMesures` = constat/choix) |
| `mesures-provisoires` | dateAudienceAOMP, revenusEpouxDemandeur, revenusEpouxDefendeur, logementCommunDescription, logementProprietaire, enfantsMineurs, souhaitResidenceEnfants, violencesAlleguees, patrimoineCommunSignificatif, demandeMesureConservatoire (10) | **3** (`dateAudienceAOMP`, `patrimoineCommunSignificatif` ← `patrimoineCommunEur`, `violencesAlleguees` — SF-246-08) | **2** (`revenusEpouxDemandeur`, `revenusEpouxDefendeur` — chiffrables des avis d'imposition / bulletins) | 5 (logement, enfants, souhait, mesure = appréciation/saisie) |
| `revisions-post-divorce` | typeRevision, dateDecisionInitiale, changementCirconstance, revenusInitialsDebiteur, revenusActuelsDebiteur, revenusInitialsCreancier, revenusActuelsCreancier, nbEnfantsACharge, ageEnfants, modeResidenceActuel, modeResidenceDemande (11) | **2** (`nbEnfantsACharge`, `revenusAnnuelsEpoux` — SF-246-08, partiel) | **3** (`dateDecisionInitiale`, `revenusActuelsDebiteur`, `revenusActuelsCreancier` — datables/chiffrables des pièces) | 6 (type, changement, revenus initiaux, mode résidence = appréciation/historique) |

### 7.4 Famille FR — filiation / adoption (lot SF-246-09)

| Outil | Champs saisissables | Pré-remplis | À brancher | Info absente |
|---|---|---|---|---|
| `contestation-paternite` | qualiteAagir, dateEtablissementFiliation, dateConnaissanceVerite, dateMajoriteEnfant, possessionEtatConforme5Ans, expertiseAdnDemandee, motifsSerieux (7) | **3** (`dateEtablissementFiliation`, `dateConnaissanceVerite`, `dateMajoriteEnfant` — SF-246-09) | **4** (`qualiteAagir`, `possessionEtatConforme5Ans`, `expertiseAdnDemandee`, `motifsSerieux` — helper lit les `*Detected` — **aspirationnels** ; `qualiteAagir` et possession état qualifiables des pièces) | 0 |
| `recherche-paternite` | qualiteDuDemandeur, dateNaissanceEnfant, presomptionPossessionEtat, expertiseAdnDemandee, pereDesigneRefuseADN, motifsSerieux (6) | **1** (`dateNaissanceEnfant` ← `dateNaissanceEnfantRechercheDetectee` — SF-246-09) | **3** (`qualiteDuDemandeur`, `presomptionPossessionEtat`, `motifsSerieux` — helper lit les `*RechercheDetected` — **aspirationnels**) | 2 (`expertiseAdnDemandee`, `pereDesigneRefuseADN` = constat) |
| `reconnaissance-paternelle` | sousType, dateNaissanceEnfant, dateReconnaissance, consentementLibreDuPere, paterniteVraisemblable, enfantNonReconnuParAutrePere, procedureRespectee, presenceParProcuration (8) | **1** (`dateNaissanceEnfant` ← `dateNaissanceEnfantDetectee` — SF-246-09) | **4** (`consentementLibreDuPere`, `paterniteVraisemblable`, `enfantNonReconnuParAutrePere`, `procedureRespectee` — helper lit les `*Detected` — **aspirationnels** ; `dateReconnaissance` extractible) | 2 (`sousType`, `presenceParProcuration` = choix/constat) |
| `adoption` | formeAdoption, ageAdoptant, ageAdopte, consentementParents, consentementAdopte, consentementConjointAdoptant, enquetes, placement6mois, pupilleEtat, adoptantMarie (10) | **2** (`ageAdoptant`, `ageAdopte` — SF-246-09) | **3** (`formeAdoption`, `pupilleEtat`, `adoptantMarie` — helper lit `formeAdoptionDemandeeDetected`, `pupilleEtatDetected`, `adoptantMarieDetected` — **aspirationnels**) | 5 (consentements, enquêtes, placement = constat/procédure) |
| `possession-etat` | dateDebutPossession, dateFinPossession, tractatus, fama, nomen, continueCondition, paisible, nonEquivoque (8) | **0** (helper lit `possessionEtatConforme5AnsDetected` — aspirationnel) | **2** (`dateDebutPossession`, `dateFinPossession` — datables des pièces) | 6 (tractatus/fama/nomen et qualités de la possession = appréciation juridique) |

### 7.5 Famille FR — autorité parentale (lot SF-246-10)

| Outil | Champs saisissables | Pré-remplis | À brancher | Info absente |
|---|---|---|---|---|
| `autorite-parentale` | regimeExerciceActuel, regimeExerciceDemande, motifChangement, dangerCaracterise, preuvesProduites, ageEnfants, consentementAutreParent, interferenceVieEnfant, dateRequete (9) | **1** (`ageEnfants` ← `agesEnfantsDetectes` — SF-246-10) | **4** (`regimeExerciceActuel`, `dangerCaracterise`, `consentementAutreParent`, `interferenceVieEnfant` — helper lit ces champs **aspirationnels** du DTO frontend, absents du record backend) | 4 (regimeDemande, motif, preuves, dateRequete = choix/saisie) |
| `changement-residence` | dateChangementPrevu, distanceKm, raisonChangement, consentementAutreParent, informePrealablement, delaiInformationJours, modeResidenceActuel, ageEnfants, scolariteImpactee, modificationDvhDemandee (10) | **1** (`ageEnfants` ← `agesEnfantsDetectes` — SF-246-10) | **4** (`raisonChangement`, `consentementAutreParent`, `informePrealablement`, `modeResidenceActuel` — helper lit ces champs **aspirationnels**) | 5 (date prévue, distance, délai, scolarité, modification DVH = saisie/projet) |
| `desaccords-parentaux` | domaineDesaccord, intensiteDesaccord, tentativesMediation, ageEnfantsConcernes, interetSuperieurInvoque, expertiseDejaRealisee, urgence, dateRequete (8) | **1** (`ageEnfantsConcernes` ← `agesEnfantsDetectes` — SF-246-10) | **4** (`domaineDesaccord`, `intensiteDesaccord`, `tentativesMediation`, `urgence` — helper lit `domaineDesaccordDetecte`, `intensiteDesaccordDetecte`, `tentativesMediationDetectees`, `urgenceDetectee` — **aspirationnels**) | 3 (intérêt supérieur, expertise, dateRequete = appréciation/saisie) |
| `calendrier-garde` | modeDetailleNote, agesEnfants, dateDebutCalendrier, dateFinCalendrier (4) | **3** (`agesEnfants` ← `agesEnfantsDetectes`, `dateDebutCalendrier`, `dateFinCalendrier` — SF-246-10) | 0 | 1 (`modeDetailleNote` = commentaire libre) |

### 7.6 Famille FR — divorce, autres

| Outil | Champs saisissables | Pré-remplis | À brancher | Info absente |
|---|---|---|---|---|
| `divorce-accepte` | acceptationPrincipeSignee, dateAcceptationPV, dureeMariageAnnees, revenusAnnuelsEpoux1, revenusAnnuelsEpoux2, patrimoineCommun, dateAssignation (7) | **4** (`dateAcceptationPV`, `dureeMariageAnnees`, `revenusAnnuelsEpoux1`, `revenusAnnuelsEpoux2`, `patrimoineCommun` — F-239 + DTO) | **1** (`dateAssignation` — datable de l'assignation) | 1 (`acceptationPrincipeSignee` = constat) |
| `divorce-alteration` | dateCessationVieCommune, preuvesSeparation, tentativesReconciliation, dureeMariageAnnees, revenusAnnuelsEpoux1, revenusAnnuelsEpoux2, patrimoineCommunSignificatif, dateAssignation (8) | **4** (`dureeMariageAnnees`, `revenusAnnuelsEpoux1`, `revenusAnnuelsEpoux2`, `patrimoineCommunSignificatif`) | **2** (`dateCessationVieCommune` — extractible ; `dateAssignation` — datable) | 2 (`preuvesSeparation`, `tentativesReconciliation` = constat) |
| `divorce-faute` | fautesInvoquees, preuvesDocumentaires, tortsAdverseInvoques, dureeMariageAnnees, revenusAnnuelsDemandeur, revenusAnnuelsDefendeur, dateDepotAssignation (7) | **0** (le helper expose `computeFautesDetectees`, `computeDureeMariage`, `computeRevenusDemandeur`/`Defendeur`, `computeDateDepotAssignation` mais lit des champs **non présents** dans le record — cf. cadrage §2.1 #3) | **5** (`fautesInvoquees`, `dureeMariageAnnees`, `revenusAnnuelsDemandeur`, `revenusAnnuelsDefendeur`, `dateDepotAssignation` — tous extractibles ; **SF-246-03 prévue, non encore livrée**) | 2 (`preuvesDocumentaires`, `tortsAdverseInvoques` = constat) |
| `divorce-checklist` | étapes de checklist | **2** (étapes signature/rédaction convention ← `dateAcceptationPV`) | 0 | reste = checklist procédurale |
| `changement-etat-civil` | typeChangement, motifInvoque, preuvesProduites, majeurDemandeur, consentementParental, datesDocsConcordants, dejaChangeAuparavant, dateNaissanceDemandeur, departementDeclaration (9) | **0** (helper lit `typeChangementDetecte`, `motifChangementDetecte`, `dateNaissanceDemandeurDetectee`, `majeurDemandeurDetected`, `consentementParentalDetected` — **dans le DTO frontend mais absents du record backend** ; **SF-246-11 prévue, non livrée**) | **3** (`typeChangement`, `motifInvoque`, `dateNaissanceDemandeur` — extractibles ; SF-246-11) | 6 (preuves, majeur, consentement, concordance, déjà changé, département = constat) |
| `mediation-familiale` | motifSaisine, mediationTentee, dateMediation, exceptionApplicable, exceptionDetail (5) | **0** (helper lit `motifSaisineMediationDetecte` — aspirationnel) | **2** (`motifSaisine`, `dateMediation` — extractibles si attestation de médiation aux pièces) | 3 (médiation tentée, exception = constat/appréciation) |
| `ordonnance-requete` | motifRequete, urgenceJustifiee, derogationContradictoire, pieceJustificativeFournie, presenceEnfants, commentaireUrgence (6) | **0** (helper lit `presenceEnfantsDetected` — aspirationnel) | **1** (`motifRequete` — qualifiable) | 4 (urgence, dérogation, pièce, commentaire = appréciation/saisie) |
| `majeurs-proteges` | regimeProtectionDemande, alterationFacultesMentales, alterationFacultesPhysiques, certificatMedicalCirconstancie, dateCertificatMedical, consentementPersonneAProteger, demandeurFamilial, actesEnvisages, urgencePatrimoniale, patrimoineSignificatif, isolementSocial, incapaciteGestionQuotidienne, alterationGrave, mandatPrealableSigne, formeMandatProtection (15) | **0** (le DTO déclare `certificatMedicalCirconstancieDetected`, `dateCertificatMedicalDetected`, etc. — **aspirationnels**, absents du record/prompt) | **2** (`dateCertificatMedical`, `regimeProtectionDemande` — datable / qualifiable du certificat médical circonstancié) | 13 (altérations, consentement, actes, urgence, isolement, mandat = appréciation médicale/juridique) |
| `pma-gpa-bioethique` | dispositif, consentementsConjointsNotaire, dateReconnaissanceAnterieurePMA, datePMA, conditionsAccesPMA, paysGPACode, parentBiologiqueAvere, decisionEtrangereProduite, adoptionDemande, dateDon, demandeAcces, ageDemandeur (12) | **0** (helper lit `dispositifBioethiqueDetecte` — aspirationnel) | **3** (`datePMA`, `dateReconnaissanceAnterieurePMA`, `dateDon` — datables des pièces médicales) | 9 (dispositif, consentements, conditions, pays GPA, parent bio, décision étrangère, adoption, âge = appréciation/saisie) |
| `partage-immobilier` | selectedBienLibelle, selectedPretLibelle (2) | **2** (`valeurImmeuble`, `capitalRestantDu` branchés SF-155-20) | 0 | 0 |

---

## 8. Domaine Famille BE

| Outil | Champs saisissables | Pré-remplis | À brancher | Info absente |
|---|---|---|---|---|
| `divorce-dc-be` | dateSignatureConvention, dateAudienceHomologation, conventionLogement, conventionBiens, conventionGardeEnfants, conventionContributions, enfantsMineursCommuns, epouxConsentent (8) | **1** (`dateSignatureConvention` ← `dateAcceptationPV`) | **1** (`dateAudienceHomologation` — datable de la convocation) | 6 (conventions logement/biens/garde/contributions, enfants, consentement = contenu/constat) |
| `divorce-desunion-be` | dateSeparation, separationConsentue, preuvesSeparation, preuvesDocumentaires, tentativesReconciliation, dateAssignation (6) | **0** (le helper lit `dateSeparation` et `separationConsentue` du DTO Famille FR — **mais SF-246-12 BE n'est pas livrée** : le champ `dateSeparation` BE doit être un champ séparé du `dateSeparation` FR, cf. cadrage §2.1 #31. À ce jour aucune source backend BE.) | **2** (`dateSeparation` BE, `dateAssignation` — datables ; **SF-246-12 prévue, non livrée**) | 3 (`separationConsentue`, `preuvesSeparation`, `tentativesReconciliation` = constat) |
| `autorite-parentale-be` | filiationEtablieDeuxParents, accordParentalExiste, demandeAutoriteExclusive, desinteretDurableParent, miseEnDangerEnfant, incapaciteParent, decisionJudiciaireAnterieure, modeHebergementPrincipal, commentaire (9) | **0** (`PREFILL_COUNT_ALWAYS_ZERO = true`) | **1** (`modeHebergementPrincipal` — extractible si convention/jugement aux pièces) | 8 (filiation, accord, désintérêt, mise en danger, incapacité = appréciation) |
| `contribution-alimentaire-enfants-be` | nombreEnfants, trancheAgeEnfants, revenuMensuelParent1, revenuMensuelParent2, coutMensuelGlobalEnfants, nuitsHebergementParent1, nuitsHebergementParent2, allocationsFamilialesMensuelles, fraisExtraordinairesMensuels, commentaire (10) | **0** (`PREFILL_COUNT_ALWAYS_ZERO = true`) | **5** (`nombreEnfants`, `revenuMensuelParent1`, `revenuMensuelParent2`, `allocationsFamilialesMensuelles`, `nuitsHebergement*` — extractibles/chiffrables des fiches de paie, jugement, convention de garde BE) | 5 (`trancheAgeEnfants`, `coutMensuelGlobal`, `fraisExtraordinaires`, `commentaire` = paramètre de simulation / saisie) |
| `contribution-conjoint-be` | typeDivorce, renonciationPensionConvention, creancierEnEtatDeBesoin, fauteGraveCreancier, dureeMariageAnnees, revenuMensuelCreancier, revenuMensuelDebiteur, degradationEconomique, commentaire (9) | **0** (`PREFILL_COUNT_ALWAYS_ZERO = true`) | **3** (`dureeMariageAnnees`, `revenuMensuelCreancier`, `revenuMensuelDebiteur` — dérivables/chiffrables des pièces) | 6 (type, renonciation, état de besoin, faute, dégradation, commentaire = appréciation) |
| `liquidation-partage-be` | notaireDesigne, dateDesignationNotaire, operationsOuvertes, dateOuvertureOperations, inventaireEtabli, projetLiquidationEtabli, dateNotificationProjet, contreditsDeposes, procesVerbalDiresEtabli, homologationDemandee, dateHomologation, commentaire (12) | **0** (`PREFILL_COUNT_ALWAYS_ZERO = true`) | **4** (`dateDesignationNotaire`, `dateOuvertureOperations`, `dateNotificationProjet`, `dateHomologation` — datables des actes de la procédure de liquidation-partage BE) | 8 (notaire désigné, opérations, inventaire, projet, contredits, PV, homologation = constat de procédure) |
| `regime-communaute-legale-be` | dateMariage, contratMariageSigne, biens (liste), dettes (liste) (4) | **0** (`PREFILL_COUNT_ALWAYS_ZERO = true`) | **2** (`dateMariage`, `contratMariageSigne` — datable / constatable de l'acte de mariage BE) | 2 (`biens`, `dettes` = saisie ligne à ligne) |

---

## 9. Synthèse chiffrée

### 9.1 Comptage des champs « à brancher »

| Domaine | Outils audités | Champs « à brancher » |
|---|---|---|
| Travail FR (déjà couverts §3.1) | 25 | ~52 |
| Travail FR dette §3.2 (SF-246-01/02 + reliquats) | 2 | 7 |
| Travail FR `travail-procedure` §3.3 | 1 | 2 |
| Travail BE | 4 | ~7 + identités fiche |
| Immigration FR | 21 | ~52 |
| Immigration BE | 5 | ~8 |
| Famille FR successions §7.1 | 8 | ~20 |
| Famille FR régimes §7.2 | 3 | 4 |
| Famille FR vie commune §7.3 | 6 | ~16 |
| Famille FR filiation §7.4 | 5 | ~16 |
| Famille FR autorité parentale §7.5 | 4 | 12 |
| Famille FR divorce / autres §7.6 | 9 | ~17 |
| Famille BE | 7 | ~20 |
| **Total** | **103** | **≈ 235 champs « à brancher »** |

> Le compte des champs « à brancher » est donné en ordre de grandeur (≈ 235) :
> certains champs sont fortement extractibles (dates, montants, codes
> énumérés), d'autres extractibles mais avec un risque LLM élevé (fait
> juridique qualifiable). Le chiffrage fin par champ relève de chaque mini-spec
> de remédiation (la mini-spec tranche la priorité). **Bar de l'audit** : tout
> champ ici en « à brancher » DOIT être tenté par l'IA — aucun ne peut être
> reclassé en « info absente » sans justification documentée que l'information
> n'existe jamais aux pièces.

### 9.2 Répartition des statuts (≈ 850 champs saisissables au total sur 103 outils)

| Statut | Volume approximatif | Part |
|---|---|---|
| **pré-rempli** | ≈ 165 champs | ~19 % |
| **à brancher** (dette F-246) | ≈ 235 champs | ~28 % |
| **info structurellement absente** (exception admise justifiée) | ≈ 450 champs | ~53 % |

### 9.3 Constats structurels

1. **Le diagnostic 2026-05-18 a sous-estimé l'ampleur.** Il dénombrait « ~32
   outils sur ~103 » concernés. L'audit exhaustif montre que **la quasi-totalité
   des 103 outils** ont au moins un champ « à brancher » sous le nouveau bar —
   les 32 du cadrage ne ciblaient que les champs **date/valeur** des outils où
   la dette était la plus visible.

2. **Trois familles de dette distinctes** :
   - **(D1) Champs date/montant/dénombrement sans source backend** — la dette
     « historique » F-246. Largement résorbée Travail FR/BE + Famille
     successions/régimes/vie commune/filiation/autorité parentale par SF-246-01
     à SF-246-12 (records + prompt branchés). Reliquats : `travail-procedure`,
     `divorce-faute` (SF-246-03), `changement-etat-civil` (SF-246-11),
     `divorce-desunion-be` (SF-246-12).
   - **(D2) Champs `*Detected` / `*Detecte` aspirationnels** — booléens, énumérés
     et listes de chaînes déclarés dans les **DTO frontend**
     (`FamilleExtractedData` surtout, `TravailExtractedData` pour
     `travail-procedure`) **sans contrepartie dans le record backend ni le
     prompt**. Les helpers les lisent → `undefined` permanent. Le gros de la
     dette restante : ~80 champs, concentrés sur Famille FR (successions,
     régimes, vie commune, filiation, autorité parentale, protection des
     majeurs, PMA/GPA).
   - **(D3) Champs extractibles jamais tentés** — dates, montants, codes
     énumérés présents aux pièces (contrats, décisions, certificats, bulletins)
     que ni le record ni le prompt n'instruisent. Le gros volume sous le **bar
     2026-05-19** : Immigration FR/BE (dates d'entrée, durées de présence,
     motifs de décision, identités), Travail (durées de contrat, montants,
     dates de pièces), Famille BE (`PREFILL_COUNT_ALWAYS_ZERO` à 4 outils — il
     existe pourtant des dates/montants extractibles).

3. **Les 4 outils Famille BE marqués `PREFILL_COUNT_ALWAYS_ZERO = true`** ne sont
   **pas** des outils 100 % « info absente » : chacun a des dates/montants
   extractibles (dates de procédure de liquidation-partage, revenus parentaux,
   date de mariage). Le `ALWAYS_ZERO` est à lever sous le nouveau bar.

4. **`prudhome-fiche` et `tribunal-travail-fiche`** : les identités
   salarié/employeur (`nomSalarie`, `prenomSalarie`, `adresseSalarie`,
   `nomEmployeur`, `adresseEmployeur`, `siretEmployeur`, `bceEmployeur`) **sont
   présentes dans `TravailExtractedData`** mais les helpers de fiche ne lisent
   que `poste`/`conventionCollective`. Dette **(c)+(d)** pure (helper +
   composant), sans extension backend — remédiation peu coûteuse.

5. **`immigration-recours` et `referes-admin`** : helpers exposant des `compute*`
   sur champs réels mais composant sans `prefillFromAi()` runtime appliquant ces
   valeurs. Dette **(d)** pure.

---

## 10. Proposition de découpage des vagues de remédiation (SF-246-15+)

Principe inchangé : **1 SF full-stack par outil ou par lot homogène** partageant
record + prompt. Priorisation : résorber d'abord la dette **(c)+(d)** peu
coûteuse (helper/composant seuls), puis la dette backend par domaine, en
suivant l'ordre démos (Travail/Immigration FR avant Famille, BE en dernier).

| SF | Périmètre | Domaine | Type de dette | Ampleur |
|---|---|---|---|---|
| **SF-246-15** | Fiches Travail — lecture des identités déjà extraites (`prudhome-fiche`, `tribunal-travail-fiche`) — helper + composant uniquement, record déjà OK | Travail FR + BE | D2/(c)+(d) | S |
| **SF-246-16** | `immigration-recours` + `referes-admin` — brancher `prefillFromAi()` runtime + champs décision contestée | Immigration FR | D3/(d) + backend | M |
| **SF-246-17** | Lot OQTF / recours Immigration FR — champs de décision restants (`dublin-recours`, `crrv-refus-visa`, `jld-retention` : motifs, état membre, type de visa) | Immigration FR | D3 | M |
| **SF-246-18** | Lot AES Immigration FR — dates d'entrée / durées de présence / niveaux / codes métier (`aes-etudiant`, `aes-famille`, `aes-humanitaire`, `aes-metiers-tension`) | Immigration FR | D3 | L |
| **SF-246-19** | Lot Immigration FR statut & dispositifs — `changement-statut`, `immigration-title-decision`, `naturalisation`, `mineurs-immigration`, `regime-algerien`, `asile-avance`, `mesures-eloignement` | Immigration FR | D3 | L |
| **SF-246-20** | Lot Immigration BE — `belgian-9bis`, `belgian-9ter`, `belgian-40bis`, `belgian-40ter` : dates d'entrée, dépôt, liens, revenus | Immigration BE | D3 | M |
| **SF-246-21** | Lot Travail FR — champs date/montant extractibles restants (`requalification-cdd-cdi`, `requalification-interim-cdi`, `indemnite-precarite-cdd`, `fin-mission-interim`, `conges-payes`, `rappel-salaire`, `documents-fin-contrat`, `licenciement-economique`, `transaction`, `pse`, `at-mp`, `contestation-are`, `heures-sup`, `discrimination`, `indemnite-comparatif`, `protection-rp`, `refere-prudhomal`) | Travail FR | D3 | L |
| **SF-246-22** | `travail-procedure` — résorber l'anomalie `procedureTravailDetectee` / `dateDeclencheurProcedure` (record + prompt) | Travail FR | D1 | S |
| **SF-246-23** | Lot Travail BE — `motif-grave-be` (dates connaissance fait / notification motifs), `avantages-conventionnels-be`, `credit-temps-be` (ancienneté, date demande) | Travail BE | D3 | M |
| **SF-246-24** | Lot Famille FR successions — booléens/énumérés `*Detected` aspirationnels (`acceptation-renonciation`, `reserve-heriditaire`, `rapport-succession`, `devolution-legale`, `donation`, `testament-validite`, `indivision-successorale`) | Famille FR | D2 | L |
| **SF-246-25** | Lot Famille FR régimes & vie commune — `*Detected` aspirationnels (`communaute-universelle`, `partage-judiciaire`, `ordonnance-protection`, `mesures-provisoires`, `revisions-post-divorce`, `pacs-dissolution`, `separation-corps`, `indivision`) | Famille FR | D2/D3 | L |
| **SF-246-26** | Lot Famille FR filiation & autorité parentale — `*Detected` aspirationnels (`contestation-paternite`, `recherche-paternite`, `reconnaissance-paternelle`, `adoption`, `possession-etat`, `autorite-parentale`, `changement-residence`, `desaccords-parentaux`) | Famille FR | D2 | L |
| **SF-246-27** | Lot Famille FR protection & divorce — `majeurs-proteges`, `pma-gpa-bioethique`, `mediation-familiale`, `ordonnance-requete`, `divorce-accepte`/`divorce-alteration` (dates d'assignation), `divorce-dc-be` (date homologation) | Famille FR + BE | D2/D3 | M |
| **SF-246-28** | Lot Famille BE — lever les 4 `PREFILL_COUNT_ALWAYS_ZERO` (`autorite-parentale-be`, `contribution-alimentaire-enfants-be`, `contribution-conjoint-be`, `liquidation-partage-be`, `regime-communaute-legale-be`) | Famille BE | D3 | L |

> **SF déjà planifiées (cadrage initial) à exécuter en parallèle/avant ces
> vagues** : SF-246-03 (`divorce-faute`), SF-246-11 (`changement-etat-civil`),
> SF-246-12 (`divorce-desunion-be`), SF-246-13 (`non-concurrence` :
> `datePriseEffet` + `secteurActivite`). Elles relèvent de la dette D1/D3 et
> restent dans le découpage F-246.

**Total remédiation post-audit** : **14 nouvelles SF (SF-246-15 à SF-246-28)**
+ les 4 SF déjà cadrées non livrées (SF-246-03/11/12/13) = **18 SF de
remédiation** pour résorber les ≈ 235 champs « à brancher ».

### 10.1 Ordre des vagues recommandé

| Vague | SF | Justification |
|---|---|---|
| **Vague A — dette frontend pure (gain rapide)** | SF-246-15, SF-246-16 | Helper + composant uniquement (ou quasi), pas/peu d'extension backend — gain immédiat sur les fiches et 2 outils immigration. |
| **Vague B — Immigration FR (démos)** | SF-246-13, SF-246-17, SF-246-18, SF-246-19 | Domaine des démos en cours, fort volume de champs date/code extractibles. |
| **Vague C — Travail FR/BE + reliquats** | SF-246-03, SF-246-21, SF-246-22, SF-246-23 | Complète la couverture Travail (démos Renversez/Mengue). |
| **Vague D — Famille FR** | SF-246-11, SF-246-24, SF-246-25, SF-246-26, SF-246-27 | Plus gros volume, dette D2 (DTO aspirationnels à réaligner) — record `FamilleExtractedData` à étendre par lots. |
| **Vague E — BE** | SF-246-12, SF-246-20, SF-246-28 | Belgique en dernier (cohérent priorisation domaine). |

> **Audit de couverture intermédiaire** : refaire un point après la Vague C
> (≈ 50 outils traités) avant d'engager les vagues Famille FR / BE — règle
> gouvernance « audit tous les 10 outils ».

---

## 11. Conclusion

- **103 outils décisionnels audités champ par champ** — périmètre intégral, et
  non l'échantillon de 32 du diagnostic 2026-05-18.
- **≈ 235 champs saisissables « à brancher »** identifiés, répartis sur la
  quasi-totalité des 103 outils — la dette est plus large que le diagnostic
  initial ne le laissait penser, le bar 2026-05-19 (« tout champ extractible
  doit être tenté ») requalifiant en dette de nombreux champs qui étaient
  rangés en « non factualisable ».
- **3 familles de dette** : D1 champs date/valeur sans source (largement
  résorbée), D2 champs `*Detected` aspirationnels du DTO frontend (concentrée
  Famille FR), D3 champs extractibles jamais tentés (gros volume Immigration +
  Travail + Famille BE).
- **18 SF de remédiation** proposées (14 nouvelles SF-246-15→28 + 4 SF déjà
  cadrées SF-246-03/11/12/13), en **5 vagues** A→E.
- Les exclusions §2.3 / §5.6 du cadrage sont **formellement caduques** : les
  outils Famille BE `PREFILL_COUNT_ALWAYS_ZERO` et les champs parqués
  « non factualisables » rejoignent le périmètre de remédiation.
- **Invariant à imposer dans chaque mini-spec SF-246-15+** : aucun champ ne peut
  rester non pré-rempli sans justification documentée que l'information est
  **structurellement absente des documents uploadés** (appréciation/arbitrage de
  l'avocat, paramètre de simulation, donnée saisie par construction). Les
  invariants dates (un champ = une définition juridique, nullable, no-op
  gracieux, provenance + badge, alerte F-IA-03, format ISO, fixtures
  multi-dates) du cadrage §5.1 restent applicables.
