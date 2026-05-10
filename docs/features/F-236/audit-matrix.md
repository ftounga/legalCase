# F-236 — Matrice d'audit `static getPrefillCount` × `prefillFromAi` (SF-236-01)

> Source : scan exhaustif des entrées de `TOOL_REGISTRY`
> (`frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts`)
> et lecture du source de chaque composant `*-section.component.ts` cible.
> Date du scan : 2026-05-10.

---

## Légende

- **getPrefillCount** : `OUI` si le composant expose un `static getPrefillCount(input)` ; `NON` sinon.
- **prefillFromAi** : `OUI` si le composant possède une méthode `prefillFromAi()` runtime ; `NON` sinon.
- **Domaine** : déduit du préfixe tool_id (`F-DT-*`, `F-IM-*`, `F-FA-*`) ou du `aiData` consommé (`travailExtractedData` / `immigrationExtractedData` / `familleExtractedData`).
- **Pays** : `FR` (default), `BE` (suffixe `-be` ou champs BE-only), `Both` (ambiguïté géographique gérée via `workspaceCountry`).
- **Max théo. N** : nombre de champs distincts qu'un `getPrefillCount` correctement implémenté DOIT pouvoir retourner si toutes les sources alimentent.
- **Anomalies** :
  - `(A)` `static getPrefillCount` manquant alors que `prefillFromAi` runtime existe (P0)
  - `(B)` Divergence runtime / static (le static existe mais ne couvre pas tous les champs runtime — P0)
  - `(C)` Ancrage mono-champ fragile (`N=1`, perdu si l'unique champ est absent en pratique — P1)
  - `(D)` Fallback `synthesis.*` manquant (la donnée est exposée en input via `TOOL_REGISTRY` mais pas consommée par le runtime — P1)
  - `(E)` Gating pays manquant ou incomplet (composants Immigration BE/FR — P2)
  - `(*)` Wrapper informationnel pur — count attendu = 0, pas d'anomalie

---

## Récapitulatif compteur

- **Travail (F-DT-* / F-132-* / F-136-*)** : 35 outils
- **Immigration (F-IM-*)** : 26 outils
- **Famille (F-FA-* / F-152-* / F-153-* / mediation- / acceptation-)** : 42 outils
- **Total** : **103 outils** (mini-spec attendait ~58 — l'écart est documenté ci-dessous, le panel s'est densifié depuis fin avril 2026)

---

## Domaine TRAVAIL (35 outils)

| tool_id | composant | pays | getPrefillCount | prefillFromAi | Champs IA consommés | Max théo. N | Anomalies |
|---------|-----------|------|-----------------|---------------|---------------------|-------------|-----------|
| F-DT-03-prescription-litige | `case-deadlines-section` | Both | NON | NON | (lit `caseDeadlinesService` interne, pas `aiData`) | 0 | (*) wrapper informationnel |
| F-DT-04-fiche-prudhomale | `prudhome-fiche-section` | FR | NON | OUI | `aiData.poste` | 1 | (A)(C) |
| F-DT-06-requete-tribunal-travail | `tribunal-travail-fiche-section` | BE | NON | OUI | `aiData.{conventionCollective, dateEntree, dateLicenciement, motifLicenciement, typeContrat}` | 5 | (A) |
| F-DT-07-anciennete-conges-prime | `anciennete-section` | FR | NON | OUI | `aiData.{salaireBrutMensuel, dateEntree, conventionCollective, primeAncienneteContractuelle, congesContractuels}` | 5 | (A) |
| F-DT-08-licenciement-validity | `licenciement-section` | Both | NON | OUI | `aiData.detections` (consume LicenciementValidityDetection record) | ~3-5 | (A) — payload riche, à compter par sub-detection |
| F-DT-09-comparateur-indemnites | `indemnite-comparatif-section` | Both | NON | OUI | `aiData.salaireBrutMensuel` (`travailExtractedData`) — `synthesis` exposé en input mais non consommé en fallback | 1 | (A)(C)(D) — F-DT-08 detections accessibles via `synthesis.licenciementValidityDetection` non lues |
| F-DT-10-rupture-conv-validity | `rupture-conv-section` | FR | NON | OUI | `aiData.detections` (RuptureConvValidityDetection record — détections binaires) | ~3 | (A) |
| F-132-rupture-conv-indemnite | `rupture-conv-indemnite-section` | FR | NON | OUI | `aiData.salaireBrutMensuel` (depuis `synthesis.travailExtractedData` exposé via input `synthesis`) | 1 | (A)(C)(D) — input arrive via `synthesis`, lecture détournée |
| F-DT-11-harcelement-licenciement-nul | `harcelement-licenciement-nul-section` | FR | NON | OUI | `aiData.{salaireBrutMensuel, motifNullitePressenti}` | 2 | (A) |
| F-DT-16-licenciement-nul-detection | `licenciement-nul-detection-section` | FR | NON | OUI | `aiData.{salaireBrutMensuel, dateLicenciement, motifNullitePressenti}` | 3 | (A) |
| F-DT-12-discrimination-dommages-interets | `discrimination-section` | Both | NON | OUI | `aiData.salaireBrutMensuel` | 1 | (A)(C) |
| F-DT-13-licenciement-economique | `licenciement-economique-section` | Both | NON | OUI | `aiData.{motifLicenciement, dateLicenciement}` | 2 | (A) |
| F-DT-15-inaptitude | `inaptitude-section` | Both | NON | OUI | `aiData.{salaireBrutMensuel, dateEntree, avisMedecinTravailDate, origineInaptitudePressentie, reclassementRespecteDetected}` | 5 | (A) |
| F-DT-19-heures-sup | `heures-sup-section` | Both | NON | OUI | `aiData.{salaireBrutMensuel, heuresSupMentionneesDansDossier}` | 2 | (A) |
| F-DT-17-indemnite-precarite-cdd | `indemnite-precarite-cdd-section` | Both | NON | OUI | `aiData.salaireBrutMensuel` | 1 | (A)(C) |
| F-DT-26-conges-payes-indemnite | `conges-payes-section` | Both | NON | OUI | `aiData.{salaireBrutMensuel, dateLicenciement}` | 2 | (A) |
| F-DT-18-fin-mission-interim | `fin-mission-interim-section` | Both | NON | OUI | `aiData.salaireBrutMensuel` | 1 | (A)(C) |
| F-DT-32-documents-fin-contrat | `documents-fin-contrat-section` | Both | NON | OUI | `aiData.{salaireBrutMensuel, dateLicenciement}` | 2 | (A) |
| F-DT-34-refere-prudhomal | `refere-prudhomal-section` | Both | NON | OUI | `aiData.{dateLicenciement, dateEntree, heuresSupMentionneesDansDossier}` | 3 | (A) |
| F-DT-21-travail-dissimule | `travail-dissimule-section` | Both | NON | OUI | `aiData.salaireBrutMensuel` | 1 | (A)(C) |
| F-DT-25-indemnite-preavis | `indemnite-preavis-section` | Both | NON | OUI | `aiData.{salaireBrutMensuel, dateLicenciement}` (+ `conventionCollective` annoncé en commentaire SF-DT-25-02 mais à confirmer) | 2-3 | (A) — vérifier `conventionCollective` |
| F-DT-20-rappel-salaire | `rappel-salaire-section` | Both | NON | OUI | `aiData.salaireBrutMensuel` (le commentaire mentionne `conventionCollective` mais runtime ne lit que salaire) | 1 | (A)(C) — divergence commentaire/runtime |
| F-DT-22-requalification-cdd-cdi | `requalification-cdd-cdi-section` | Both | NON | OUI | `aiData.salaireBrutMensuel` | 1 | (A)(C) |
| F-DT-23-requalification-interim-cdi | `requalification-interim-cdi-section` | Both | NON | OUI | `aiData.salaireBrutMensuel` | 1 | (A)(C) |
| F-DT-24-non-concurrence | `non-concurrence-section` | Both | NON | OUI | `aiData.salaireBrutMensuel` | 1 | (A)(C) |
| F-132-rupture-amiable-info | `rupture-amiable-info-section` | FR | NON | NON | (wrapper informationnel pur) | 0 | (*) |
| F-DT-35-contestation-are-fr | `contestation-are-section` | FR | NON | OUI | `aiData.dateLicenciement` | 1 | (A)(C) |
| F-DT-27-motif-grave-be | `motif-grave-be-section` | BE | NON | OUI | `aiData.{salaireBrutMensuel, dateLicenciement}` | 2 | (A) |
| F-DT-28-avantages-conventionnels-be | `avantages-conventionnels-be-section` | BE | NON | OUI | `aiData.salaireBrutMensuel` | 1 | (A)(C) |
| F-DT-29-credit-temps-be | `credit-temps-be-section` | BE | NON | OUI | `aiData.{ageDemandeurAnnees, dateEntree}` (mappé vers `ancienneteEntrepriseMois`) | 2 | (A) |
| F-DT-14-pse-validite | `pse-section` | FR | NON | OUI | `aiData.dateLicenciement` | 1 | (A)(C) |
| F-DT-30-protection-rp | `protection-rp-section` | FR | NON | OUI | `aiData.motifLicenciement` (mappé vers enum backend) | 1 | (A)(C) |
| F-DT-33-at-mp | `at-mp-section` | FR | NON | OUI | `aiData.dateLicenciement` (proxy `dateAccident`) | 1 | (A)(C) |
| F-DT-31-transaction | `transaction-section` | Both | NON | OUI | `aiData.{salaireBrutMensuel, motifLicenciement, dateLicenciement, dateEntree}` | 4 | (A) |
| F-136-travail-procedure | `travail-procedure-section` | Both | NON | OUI | `aiData.{procedureTravailDetectee, dateDeclencheurProcedure}` | 2 | (A) |

**Total Travail** : 35 outils — **30 anomalies (A)**, dont **12 ancrages mono-champ (C)**, **3 cas de fallback `synthesis.*` manquant (D)** (F-DT-09, F-132-rupture-conv-indemnite, F-DT-08).

---

## Domaine IMMIGRATION (26 outils)

| tool_id | composant | pays | getPrefillCount | prefillFromAi | Champs IA consommés | Max théo. N | Anomalies |
|---------|-----------|------|-----------------|---------------|---------------------|-------------|-----------|
| F-IM-21-jld-retention-fr | `jld-retention-section` | FR | NON | NON | (wrapper informationnel — F-208) | 0 | (*) |
| F-IM-22-dublin-recours-fr | `dublin-recours-section` | FR | NON | NON | (wrapper informationnel — F-208) | 0 | (*) |
| F-IM-23-crrv-refus-visa-fr | `crrv-refus-visa-section` | FR | NON | NON | (wrapper informationnel — F-208) | 0 | (*) |
| F-IM-24-victime-violences-l4256-fr | `victime-violences-l4256-section` | FR | NON | NON | (wrapper informationnel — F-208) | 0 | (*) |
| F-IM-01-checklist-pieces | `immigration-checklist-section` | Both | OUI | OUI | `aiData.inferredChecklistType` (validé contre `KNOWN_TITRE_TYPES`) | 1 | aucune |
| F-IM-05-arbre-decisionnel-titre | `immigration-title-decision-section` | Both | OUI | OUI | `aiData.{nationaliteUe, typeTitreSejour, typeTitreSejourCode}` + `triggerEvents[0].eventCode` (TRIGGER_TO_CRITERIA) | 3 | aucune (référence canonique) |
| F-IM-06-recours | `immigration-recours-section` | Both | NON | OUI | `aiData.{typeRecoursCode, dateNotificationDecisionContestee}` | 2 | (A) |
| F-IM-07-droit-au-travail | `immigration-work-right-section` | Both | OUI | OUI | `aiData.typeTitreSejourCode` + gating `workspaceCountry` (FR/BE) | 1 | (C) — N=1 mais gating pays correct |
| F-IM-08-oqtf-avec-delai-fr | `oqtf-avec-delai-section` | FR | NON | OUI | `aiData.{dateNotificationOqtf, motifOqtfCode, recoursFormeDetected}` | 3 | (A) |
| F-IM-08-oqtf-sans-delai-fr | `oqtf-sans-delai-section` | FR | NON | OUI | `aiData.{dateHeureNotificationOqtfSansDelai, motifOqtfCode, recoursFormeDetected, placementCraDetected}` | 4 | (A) |
| F-IM-08-annexe13-be | `annexe13-be-section` | BE | NON | OUI | `aiData.{dateNotificationAnnexe13, motifOqtCodeBe, delaiDepartImposeJours, transfertImminentDetected}` | 4 | (A)(E) — gating pays absent (le composant lit `workspaceCountry` mais ne le garde pas dans `prefillFromAi`) |
| F-IM-08-referes-admin-fr | `referes-admin-section` | FR | NON | OUI | `aiData.{typeRecoursCode, dateNotificationDecisionContestee, transfertImminentDetected}` | 3 | (A) |
| F-IM-14-40ter-familial-belge-be | `belgian-40ter-section` | BE | NON | OUI | `aiData.dateDepotProcedure` (autres champs `lienFamilial`, `regroupantBelge`, `revenusNetsMensuels` annoncés en commentaire mais à vérifier) | 1-4 | (A)(C)(E) — soit le commentaire est erroné soit le runtime est incomplet |
| F-IM-14-9bis-humanitaire-be | `belgian-9bis-section` | BE | NON | OUI | `aiData.dateDepotProcedure` | 1 | (A)(C)(E) |
| F-IM-14-9ter-medical-be | `belgian-9ter-section` | BE | NON | OUI | `aiData.{dateDebutSymptomes, dateDepotDemande, maladieGrave, maladieGraveCertifiee, menaceOrdrePublic, soinsInaccessiblesPaysOrigine, soinsNecessairesDisponiblesBe}` | 7 | (A)(E) |
| F-IM-14-40bis-cohabitant-ue-be | `belgian-40bis-section` | BE | NON | OUI | `aiData.{dateDepotProcedure, nationaliteUe}` | 2 | (A)(E) |
| F-IM-09-aes-metiers-tension | `aes-metiers-tension-section` | FR | NON | OUI | `aiData.dateDepotProcedure` | 1 | (A)(C) |
| F-IM-09-aes-famille | `aes-famille-section` | FR | NON | OUI | (lecture conditionnée sur autres detections — runtime n'extrait pas de champ direct via `ai.X`) | ~1 | (A) — payload faible, à confirmer |
| F-IM-09-aes-etudiant | `aes-etudiant-section` | FR | NON | OUI | `aiData.dateDepotProcedure` | 1 | (A)(C) |
| F-IM-09-aes-humanitaire | `aes-humanitaire-section` | FR | NON | OUI | `aiData.dateDepotProcedure` | 1 | (A)(C) |
| F-IM-11-changement-statut | `changement-statut-section` | FR | OUI | OUI | `aiData.{typeTitreSejourCode, typeTitreSejour, dateExpirationTitre}` | 2 | aucune |
| F-IM-13-naturalisation | `naturalisation-section` | FR | NON | OUI | (lecture indirecte — pas de champs `ai.X` directement, runtime probablement basé sur scoring déduit) | ~1 | (A) — à confirmer source IA |
| F-IM-19-mineurs | `mineurs-immigration-section` | FR | NON | OUI | `aiData.{dateNaissance, dateEntreeFrance, nationalite}` | 3 | (A) |
| F-IM-20-mesures-eloignement | `mesures-eloignement-section` | FR | NON | OUI | `aiData.typeProcedureDetectee` | 1 | (A)(C) |
| F-IM-12-asile-avance | `asile-avance-section` | FR | NON | OUI | `aiData.typeProcedureDetectee` | 1 | (A)(C) |
| F-IM-17-regime-algerien | `regime-algerien-section` | FR | NON | OUI | `aiData.typeProcedureDetectee` | 1 | (A)(C) |

**Total Immigration** : 26 outils — **18 anomalies (A)**, dont **9 ancrages mono-champ (C)**, **5 composants BE sans gating pays effectif (E)**, et **3 conformes** (F-IM-01, F-IM-05, F-IM-07, F-IM-11).

---

## Domaine FAMILLE (42 outils)

| tool_id | composant | pays | getPrefillCount | prefillFromAi | Champs IA consommés | Max théo. N | Anomalies |
|---------|-----------|------|-----------------|---------------|---------------------|-------------|-----------|
| F-FA-01-prestation-compensatoire | `prestation-compensatoire-section` | FR | OUI (=0) | NON | (présentationnel pur sur `synthesis.prestationCompensatoireEstimate`) | 0 | (*) wrapper informationnel |
| F-FA-02-pension-alimentaire | `pension-alimentaire-section` | FR | OUI (=0) | NON | (présentationnel pur sur `synthesis.pensionAlimentaireEstimate`) | 0 | (*) wrapper informationnel |
| F-FA-04-liquidation-communaute | `liquidation-communaute-section` | FR | OUI (=0) | NON | (présentationnel pur sur `synthesis.liquidationCommunaute`) | 0 | (*) wrapper informationnel |
| F-FA-05-partage-immobilier | `partage-immobilier-section` | FR | OUI | OUI | `aiData.{valeurImmeuble, capitalRestantDu}` + fallback `synthesis.liquidationCommunaute.{actifCommun, passifCommun}` (matching `IMMO_KEYWORDS`/`PRET_KEYWORDS`) | 2 | aucune (pattern de référence multi-source) |
| F-FA-15-recompenses | `recompenses-section` | FR | NON | OUI | `aiData.regimeMatrimonialDetecte` | 1 | (A)(C) |
| F-FA-06-calendrier-garde | `calendrier-garde-section` | Both | OUI | OUI (via `aiModeGardeDetaille` input) | `synthesis.pensionAlimentaireEstimate.modeGardeDetaille` (NOT `aiData`) + gating `workspaceCountry` (MODES_FR/MODES_BE) | 1 | aucune (pattern fallback synthèse exclusive) |
| F-FA-07-checklist-divorce | `divorce-checklist-section` | FR | OUI | OUI | `aiData.dateAcceptationPV` | 1 | aucune (mais (C) — N=1) |
| F-152-divorce-consentement-scoring | `divorce-cm-scoring-section` | FR | OUI (=0) | NON | (présentationnel pur sur `synthesis.divorceCmScoring`) | 0 | (*) wrapper informationnel |
| F-153-fourchettes-jaf | `fourchettes-jaf-section` | FR | OUI (=0) | NON | (présentationnel pur sur `synthesis.jurisprudenceRange`) | 0 | (*) wrapper informationnel |
| F-FA-08-divorce-alteration | `divorce-alteration-section` | FR | NON | OUI | `aiData.{dureeMariageAnnees, dateCessationVieCommune, patrimoineCommunSignificatif, revenusAnnuelsEpoux1Eur, revenusAnnuelsEpoux2Eur}` | 5 | (A) |
| F-FA-09-divorce-faute | `divorce-faute-section` | FR | NON | OUI | `aiData.fautesDetectees` (array) — mais utilise `travailExtractedData` au lieu de `familleExtractedData` (cf. ligne 1062 — bug TOOL_REGISTRY ?) | ~1 | (A) + bug `aiData` source erronée à investiguer |
| F-FA-10-divorce-accepte | `divorce-accepte-section` | FR | NON | OUI | `aiData.{dateAcceptationPV, dureeMariageAnnees, patrimoineCommun, revenusAnnuelsEpoux1Eur, revenusAnnuelsEpoux2Eur}` | 5 | (A) |
| F-FA-12-mesures-provisoires | `mesures-provisoires-section` | FR | NON | OUI | `aiData.{dateAudienceAOMP, patrimoineCommunSignificatif, violencesAlleguees}` | 3 | (A) |
| F-FA-13-revisions-post-divorce | `revisions-post-divorce-section` | FR | NON | OUI | `aiData.{revenusAnnuelsEpoux1Eur, revenusAnnuelsEpoux2Eur}` | 2 | (A) |
| F-FA-14-ordonnance-protection | `ordonnance-protection-section` | FR | NON | OUI | `aiData.{dangerImmediatDetected, dateRequeteOP, demandeurDejaProtegeDetected, logementCommunDetected, presenceEnfantsDetected, preuvesViolencesDetectees, victimeFinanciairementDependanteDetected, violencesAllegueesDetectees}` | 8 | (A) |
| F-FA-19-autorite-parentale | `autorite-parentale-section` | FR | NON | OUI | `aiData.{ageEnfants, consentementAutreParent, dangerCaracterise, interferenceVieEnfant, regimeExerciceActuel}` | 5 | (A) |
| F-FA-19-changement-residence | `changement-residence-section` | FR | NON | OUI | `aiData.{ageEnfants, consentementAutreParent, informePrealablement, modeResidenceActuel, raisonChangementDetectee}` | 5 | (A) |
| F-FA-19-desaccords-parentaux | `desaccords-parentaux-section` | FR | NON | OUI | `aiData.{ageEnfants, domaineDesaccordDetecte, intensiteDesaccordDetecte, tentativesMediationDetectees, urgenceDetectee}` | 5 | (A) |
| F-FA-22-indivision | `indivision-section` | FR | NON | OUI | `aiData.{dateSeparation, logementCommunDetected}` | 2 | (A) |
| F-FA-20-pacs-dissolution | `pacs-dissolution-section` | FR | NON | OUI | `aiData.{dateConclusionPacs, modeDissolutionPacsDetecte, regimeBiensPacsDetecte, patrimoineCommunSignificatifDetecte, creancesAllegueesDetectees}` | 5 | (A) |
| F-FA-25-majeurs-proteges | `majeurs-proteges-section` | FR | NON | OUI | `aiData.{altertationFacultesMentales, altertationFacultesPhysiques, altertationGraveDetected, certificatMedicalCirconstancieDetected, consentementPersonneAProtegerDetected, dateCertificatMedicalDetected, demandeurFamilialDetected, formeMandatProtectionDetected, incapaciteGestionQuotidienneDetected, mandatPrealableSigneDetected, regimeProtectionDemande, actesEnvisagesDetected}` | 12 | (A) — payload XL |
| F-FA-26-changement-etat-civil | `changement-etat-civil-section` | FR | NON | OUI | `aiData.{consentementParentalDetected, dateNaissanceDemandeurDetectee, majeurDemandeurDetected, motifChangementDetecte, typeChangementDetecte}` | 5 | (A) |
| F-FA-21-separation-corps | `separation-corps-section` | FR | NON | OUI | `aiData.{dateSeparation, patrimoineCommun}` | 2 | (A) |
| F-FA-17-partage-judiciaire | `partage-judiciaire-section` | FR | NON | OUI | `aiData.{nombreCoindivisairesDetecte, pvDifficultesEtablisDetected, tentativeAmiableEpuiseueeDetected, valeurBiensIndivisionEur}` | 4 | (A) |
| F-FA-18-reconnaissance-paternelle | `reconnaissance-paternelle-section` | FR | NON | OUI | `aiData.{consentementLibreDuPereDetected, dateNaissanceEnfantDetectee, enfantNonReconnuParAutrePereDetected, paterniteVraisemblableDetected, procedureRespecteeReconnaissanceDetected}` | 5 | (A) |
| F-FA-18-contestation-paternite | `contestation-paternite-section` | FR | NON | OUI | `aiData.{dateConnaissanceVeriteDetectee, dateEtablissementFiliationDetectee, dateMajoriteEnfantDetectee, expertiseAdnDemandeeDetected, motifsSerieuxDetected, possessionEtatConforme5AnsDetected, qualiteAagirContestationDetected}` | 7 | (A) |
| F-FA-18-recherche-paternite | `recherche-paternite-section` | FR | NON | OUI | `aiData.{dateNaissanceEnfantRechercheDetectee, expertiseAdnDemandeeRechercheDetected, motifsSerieuxRechercheDetected, pereDesigneRefuseADNDetected, presomptionPossessionEtatRechercheDetected, qualiteDuDemandeurRechercheDetected}` | 6 | (A) |
| F-FA-18-possession-etat | `possession-etat-section` | FR | NON | OUI | `aiData.possessionEtatConforme5AnsDetected` (faisceau cardinal) | 1 | (A)(C) |
| F-FA-18-adoption | `adoption-section` | FR | NON | OUI | `aiData.{adoptantMarieDetected, ageAdoptantDetecte, ageAdopteDetecte, formeAdoptionDemandeeDetected, pupilleEtatDetected}` | 5 | (A) |
| F-FA-16-communaute-universelle | `communaute-universelle-section` | FR | NON | OUI | `aiData.{clauseAttributionIntegraleDetected, contratNotarieDetected, enfantsNonCommunsDetected, valeurCommunauteEurDetectee}` | 4 | (A) |
| F-FA-23-ordonnance-requete | `ordonnance-requete-section` | Both | NON | OUI | `aiData.presenceEnfantsDetected` | 1 | (A)(C) |
| F-FA-24-devolution-legale | `devolution-legale-section` | FR | NON | OUI | `aiData.{conjointSurvivantDetected, nbDescendantsDetecte, nbFreresSoeursDetecte, tousDescendantsCommunsAvecConjointDetected}` | 4 | (A) |
| F-FA-27-pma-gpa | `pma-gpa-bioethique-section` | FR | NON | OUI | `aiData.dispositifBioethiqueDetecte` | 1 | (A)(C) |
| F-FA-24-testament-validite | `testament-validite-section` | FR | NON | OUI | `aiData.{dateRedactionTestamentDetectee, formeTestamentDetectee, legsExcedeQuotiteDisponibleDetected, saineDEspritTestateurDetected}` | 4 | (A) |
| F-FA-24-donation | `donation-section` | FR | NON | OUI | `aiData.{dateDonationDetectee, formeDonationDetectee, respectQuotiteDisponibleDetected, saineDEspritDonateurDetected}` | 4 | (A) |
| F-FA-24-reserve-heriditaire | `reserve-heriditaire-section` | FR | NON | OUI | `aiData.{conjointSurvivantDetected, dateOuvertureSuccessionDetectee, montantLibsTotalEurDetecte, montantSuccessionEurDetecte, nbDescendantsDetecte, nombreEnfantsSuccessionDetecte, qualiteDuDemandeurReserveDetecte}` | 7 | (A) |
| F-FA-24-partage-successoral | `partage-successoral-section` | FR | NON | OUI | `aiData.{dateDecesDetectee, dateOuvertureSuccessionDetectee, modePartageDemandeDetecte, nombreCoheritiersDetecte}` | 4 | (A) |
| F-FA-24-indivision-successorale | `indivision-successorale-section` | FR | NON | OUI | `aiData.{dateOuvertureSuccessionDetectee, typeIndivisionSuccessoraleDetecte}` | 2 | (A) |
| F-FA-24-rapport-succession | `rapport-succession-section` | FR | NON | OUI | `aiData.{dateDonationDetectee, donationDispenseDeRapportDetected, montantDonationsRecuesEurDetecte, naturePresumeeNonRapportableDetected, qualiteHeritierRapportDetectee, valeurDonationAuJourPartageEurDetectee}` | 6 | (A) |
| F-FA-11-desunion-irremediable-be | `divorce-desunion-be-section` | BE | NON | OUI | `aiData.{dateSeparation, separationConsentue}` | 2 | (A)(E) |
| mediation-familiale-pre-saisine | `mediation-familiale-section` | FR | OUI | OUI | `aiData.motifSaisineMediationDetecte` | 1 | aucune (mais (C) — N=1) |
| acceptation-renonciation-succession | `acceptation-renonciation-section` | FR | OUI | OUI | `aiData.{dateOuvertureSuccessionDetectee, actifBrutSuccessionEurDetecte, passifSuccessionEurDetecte, qualiteHeritierDetectee, actesEquivalentAcceptationDejaPosesDetected, dettesIncertainesDetected}` | 6 | aucune |

**Total Famille** : 42 outils — **31 anomalies (A)**, dont **5 ancrages mono-champ (C)**, **6 conformes complets** (F-FA-05, F-FA-06, F-FA-07, mediation-familiale, acceptation-renonciation, F-FA-15 voir below), **5 wrappers informationnels (count=0)**, et **1 bug source `aiData` à investiguer (F-FA-09)**.

---

## Listes récapitulatives par anomalie (pour piloter SF-236-02 à 04)

### Anomalie (A) — `static getPrefillCount` manquant — 79 composants — P0 SF-236-02

#### Vague Travail (29 composants)

`prudhome-fiche`, `tribunal-travail-fiche`, `anciennete`, `licenciement`, `indemnite-comparatif`, `rupture-conv`, `rupture-conv-indemnite`, `harcelement-licenciement-nul`, `licenciement-nul-detection`, `discrimination`, `licenciement-economique`, `inaptitude`, `heures-sup`, `indemnite-precarite-cdd`, `conges-payes`, `fin-mission-interim`, `documents-fin-contrat`, `refere-prudhomal`, `travail-dissimule`, `indemnite-preavis`, `rappel-salaire`, `requalification-cdd-cdi`, `requalification-interim-cdi`, `non-concurrence`, `contestation-are`, `motif-grave-be`, `avantages-conventionnels-be`, `credit-temps-be`, `pse`, `protection-rp`, `at-mp`, `transaction`, `travail-procedure`.

(35 - 2 wrappers F-DT-03/F-132-rupture-amiable-info - 0 conformes = 33 — mais 4 sont déjà à count=0 logique [wrappers] donc 29 réels à instrumenter.)

#### Vague Immigration (18 composants)

`immigration-recours`, `oqtf-avec-delai`, `oqtf-sans-delai`, `annexe13-be`, `referes-admin`, `belgian-40ter`, `belgian-9bis`, `belgian-9ter`, `belgian-40bis`, `aes-metiers-tension`, `aes-famille`, `aes-etudiant`, `aes-humanitaire`, `naturalisation`, `mineurs-immigration`, `mesures-eloignement`, `asile-avance`, `regime-algerien`.

(26 - 4 wrappers F-208 - 4 conformes = 18.)

#### Vague Famille (32 composants)

`recompenses`, `divorce-alteration`, `divorce-faute`, `divorce-accepte`, `mesures-provisoires`, `revisions-post-divorce`, `ordonnance-protection`, `autorite-parentale`, `changement-residence`, `desaccords-parentaux`, `indivision`, `pacs-dissolution`, `majeurs-proteges`, `changement-etat-civil`, `separation-corps`, `partage-judiciaire`, `reconnaissance-paternelle`, `contestation-paternite`, `recherche-paternite`, `possession-etat`, `adoption`, `communaute-universelle`, `ordonnance-requete`, `devolution-legale`, `pma-gpa-bioethique`, `testament-validite`, `donation`, `reserve-heriditaire`, `partage-successoral`, `indivision-successorale`, `rapport-succession`, `divorce-desunion-be`.

(42 - 5 wrappers - 5 conformes = 32.)

### Anomalie (B) — Divergence runtime / static — 0 composant détecté à l'audit

Aucune divergence détectée au scan automatique. À reconfirmer manuellement dans SF-236-03 sur les 8 composants conformes (count > 0) en re-scannant ligne à ligne :
- `immigration-title-decision-section` (ref canonique)
- `immigration-work-right-section`
- `immigration-checklist-section`
- `changement-statut-section`
- `divorce-checklist-section`
- `partage-immobilier-section`
- `mediation-familiale-section`
- `acceptation-renonciation-section`
- `calendrier-garde-section`

### Anomalie (C) — Ancrage mono-champ fragile (N=1) — 26 composants — P1 SF-236-04

Tous les composants où `Max théo. N = 1`. Travail (12) : `prudhome-fiche`, `discrimination`, `indemnite-precarite-cdd`, `fin-mission-interim`, `travail-dissimule`, `rappel-salaire`, `requalification-cdd-cdi`, `requalification-interim-cdi`, `non-concurrence`, `avantages-conventionnels-be`, `pse`, `protection-rp`, `at-mp`, `contestation-are-fr`. Immigration (9) : `belgian-40ter`, `belgian-9bis`, `aes-metiers-tension`, `aes-etudiant`, `aes-humanitaire`, `mesures-eloignement`, `asile-avance`, `regime-algerien`, `immigration-work-right` (acceptable car gating pays). Famille (5) : `recompenses`, `possession-etat`, `ordonnance-requete`, `pma-gpa-bioethique`, `mediation-familiale`, `divorce-checklist`.

**Action** : pour chacun, identifier 1-2 champs IA additionnels exploitables (ex : ajouter `dateLicenciement`/`dateEntree` à tout outil Travail mono-`salaireBrutMensuel`).

### Anomalie (D) — Fallback `synthesis.*` manquant — 4 composants — P1 SF-236-04

| Composant | Champ exposé non lu | Source candidate |
|-----------|---------------------|------------------|
| `indemnite-comparatif-section` (F-DT-09) | `synthesis` exposé en input | Lire `synthesis.licenciementValidityDetection.detections` pour TYPE_RUPTURE pré-fill |
| `rupture-conv-indemnite-section` (F-132) | `synthesis.travailExtractedData.salaireBrutMensuel` | Salaire lu via la mauvaise propriété — refacto |
| `licenciement-section` (F-DT-08) | `synthesis.licenciementValidityDetection.detections` | Le runtime lit `aiData.detections` mais n'a pas de fallback robuste |
| `rappel-salaire` (F-DT-20) | `aiData.conventionCollective` annoncé mais non lu | Ajouter au runtime |

### Anomalie (E) — Gating pays Immigration manquant — 5 composants BE — P2 SF-236-04

| Composant | Pays cible | Risque |
|-----------|------------|--------|
| `annexe13-be-section` (F-IM-08-annexe13-be) | BE only | Pré-fill activé même si `workspaceCountry === 'FRANCE'` |
| `belgian-40ter-section` | BE only | Pré-fill activé même si FR |
| `belgian-9bis-section` | BE only | Idem |
| `belgian-9ter-section` | BE only | Idem |
| `belgian-40bis-section` | BE only | Idem |
| `divorce-desunion-be-section` (F-FA-11-be) | BE only | Idem |

**Action SF-236-04** : ajouter un guard `if (workspaceCountry !== 'BELGIQUE') return 0;` dans le static helper de chacun.

---

## Composants conformes (référence pour SF-236-02/03)

| Composant | tool_id | Pattern |
|-----------|---------|---------|
| `immigration-title-decision-section` | F-IM-05 | Référence canonique : 3 fields, fallback en cascade triggerEvents → CODE_TO_MOTIF → heuristique texte |
| `immigration-work-right-section` | F-IM-07 | Pattern gating pays clean (FR_TITRE_CODES / BE_TITRE_CODES) |
| `immigration-checklist-section` | F-IM-01 | Pattern N=1 + validation enum (`KNOWN_TITRE_TYPES`) |
| `changement-statut-section` | F-IM-11 | Pattern N=2 avec mapper helper (`mapTitreSejourFromIa`) |
| `divorce-checklist-section` | F-FA-07 | Pattern N=1 simple ISO date validation |
| `partage-immobilier-section` | F-FA-05 | Pattern multi-source (aiData + synthesis.liquidationCommunaute) |
| `mediation-familiale-section` | mediation-familiale-pre-saisine | Pattern N=1 string non-vide |
| `acceptation-renonciation-section` | acceptation-renonciation-succession | Pattern N=6, mix string/number/boolean/enum |
| `calendrier-garde-section` | F-FA-06 | Pattern fallback synthèse exclusive + gating pays (MODES_FR/MODES_BE) |

Wrappers `count=0` informationnels (à laisser tels quels) : `prestation-compensatoire`, `pension-alimentaire`, `liquidation-communaute`, `divorce-cm-scoring`, `fourchettes-jaf`, `case-deadlines` (F-DT-03), `rupture-amiable-info` (F-132), 4 wrappers F-208 IM (jld-retention, dublin-recours, crrv-refus-visa, victime-violences-l4256).

---

## 5 fichiers à amorcer en SF-236-02 (les plus simples par domaine)

Sélection volontairement triviale (1-2 fields, copy/paste direct du pattern F-IM-05) pour calibrer le contrat helper avant de scaler :

1. **Travail** — `pse-section.component.ts` (F-DT-14) : 1 field `dateLicenciement` mappé `dateProjet`
2. **Travail** — `at-mp-section.component.ts` (F-DT-33) : 1 field `dateLicenciement` (proxy `dateAccident`)
3. **Immigration** — `aes-metiers-tension-section.component.ts` (F-IM-09) : 1 field `dateDepotProcedure`
4. **Famille** — `recompenses-section.component.ts` (F-FA-15) : 1 field `regimeMatrimonialDetecte`
5. **Famille** — `possession-etat-section.component.ts` (F-FA-18-possession-etat) : 1 field `possessionEtatConforme5AnsDetected`

Ces 5 amorces servent de smoke test du contrat helper avant déploiement sur les 79 composants.

---

## Découpage final des vagues SF-236-02

| Vague | Domaine | Branche | Composants à instrumenter |
|-------|---------|---------|---------------------------|
| Wave A | Travail FR + BE | `feat/SF-236-02-travail` | 29 composants |
| Wave B | Immigration FR + BE | `feat/SF-236-02-immigration` | 18 composants |
| Wave C | Famille FR + BE | `feat/SF-236-02-famille` | 32 composants |

Wrappers (15 composants count=0) sans modification.
Conformes (9 composants) refactorés pour adopter le helper dans la vague de leur domaine respectif.

---

## Bugs / écarts à investiguer hors scope SF-236-01

- **F-FA-09 divorce-faute** : `TOOL_REGISTRY` ligne 1062 mappe `aiData: ctx.synthesis?.travailExtractedData` au lieu de `familleExtractedData` — *bug très probable* à confirmer en SF-236-02 / 04.
- **F-DT-25 indemnite-preavis** : commentaire mentionne `conventionCollective` en pré-fill mais le runtime ne semble pas l'utiliser. À vérifier.
- **F-DT-20 rappel-salaire** : commentaire mentionne `conventionCollective` mais runtime ne lit que `salaireBrutMensuel`. À aligner.
- **F-IM-14-40ter belgian-40ter** : commentaire annonce `lienFamilial`, `regroupantBelge`, `revenusNetsMensuels` mais runtime ne lit que `dateDepotProcedure`. Soit le commentaire est en avance soit le code est incomplet.
