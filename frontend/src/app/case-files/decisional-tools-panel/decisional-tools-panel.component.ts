import {
  Component,
  DestroyRef,
  EventEmitter,
  Input,
  OnChanges,
  OnInit,
  Optional,
  Output,
  SimpleChanges,
  Type,
  ViewContainerRef,
  inject,
  signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { debounceTime } from 'rxjs';
import { CaseFileService, VisibleToolSet } from '../../core/services/case-file.service';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { RetainedPisteAlignment } from '../../core/models/retained-piste-alignment.model';
import { PieceManquanteAlignment } from '../../core/models/piece-manquante-alignment.model';
import { RisqueAlignment } from '../../core/models/risque-alignment.model';
import { AiQuestionAlignment } from '../../core/models/ai-question-alignment.model';
import { DecisionToolAlignmentsLoader } from '../decision-tools-shared/decision-tool-alignments.loader';
import { RetainedPistesBadge } from '../immigration-title-decision-section/immigration-title-decision-section.component';
import { DecisionToolCardComponent, PiecesBadgeInput, RisquesBadgeInput } from './decision-tool-card/decision-tool-card.component';
import {
  computePiecesBadge,
  piecesObtenuesFor,
} from './piece-manquante-badge.helper';
import {
  computeRisquesBadge,
  getRisquesACreuserCountFor,
  risquesValidesFor,
} from './risque-badge.helper';
import { DecisionToolModalService } from './decision-tool-modal/decision-tool-modal.service';
// SF-238-02 : service d'activation manuelle d'un outil depuis le catalogue.
import { DecisionToolManualActivationService } from '../../core/services/decision-tool-manual-activation.service';
import { DecisionalToolsProgressBannerComponent } from './decisional-tools-progress-banner.component';
import { DecisionalToolsProgressService } from './decisional-tools-progress.service';
import { getToolMetadata, getToolPrefillCount, PrefillCountInput } from './decision-tool.contract';
import { AncienneteSectionComponent } from '../anciennete-section/anciennete-section.component';
import { LicenciementSectionComponent } from '../licenciement-section/licenciement-section.component';
import { RuptureConvSectionComponent } from '../rupture-conv-section/rupture-conv-section.component';
import { RuptureConvIndemniteSectionComponent } from '../rupture-conv-indemnite-section/rupture-conv-indemnite-section.component';
import { RuptureAmiableInfoSectionComponent } from '../rupture-amiable-info-section/rupture-amiable-info-section.component';
// F-208 — Wrappers informationnels P1 Immigration FR (backend complet PR #915, composants
// de saisie complets à livrer dans une SF ultérieure de F-208).
import { JldRetentionSectionComponent } from '../jld-retention-section/jld-retention-section.component';
import { DublinRecoursSectionComponent } from '../dublin-recours-section/dublin-recours-section.component';
import { CrrvRefusVisaSectionComponent } from '../crrv-refus-visa-section/crrv-refus-visa-section.component';
import { VictimeViolencesL4256SectionComponent } from '../victime-violences-l4256-section/victime-violences-l4256-section.component';
// SF-214-02 : composant complet F-IM-25 étranger malade L.425-9 CESEDA (FR).
import { EtrangerMaladeSectionComponent } from '../etranger-malade-section/etranger-malade-section.component';
// SF-214-04 : composant complet F-IM-26 regroupement familial L.434-1+ CESEDA (FR).
import { RegroupementFamilialSectionComponent } from '../regroupement-familial-section/regroupement-familial-section.component';
// SF-214-06 : composant complet F-IM-27-vpf-liens-personnels-l42323-fr — VPF liens personnels L.423-23 (FR, scoring).
import { VpfLiensPersonnelsSectionComponent } from '../vpf-liens-personnels-section/vpf-liens-personnels-section.component';
// SF-214-08 : composant complet F-IM-28-vls-ts-validation-ofii-fr — validation VLS-TS OFII (FR, calculateur délai 3 mois).
import { VlsTsValidationSectionComponent } from '../vls-ts-validation-section/vls-ts-validation-section.component';
// SF-214-30 : composant complet F-IM-39-naturalisation-recours-tj-fr — recours TJ naturalisation (FR, calculateur délai 6 mois).
import { NaturalisationRecoursTjSectionComponent } from '../naturalisation-recours-tj-section/naturalisation-recours-tj-section.component';
// SF-214-32 : composant complet F-IM-40-naturalisation-recours-ta-fr — recours TA Nantes naturalisation (FR, calculateur délai 2 mois).
import { NaturalisationRecoursTaSectionComponent } from '../naturalisation-recours-ta-section/naturalisation-recours-ta-section.component';
// SF-214-34 : composant complet F-IM-41-appel-caa-cassation-ce-fr — appel CAA / cassation CE (FR, calculateur délai 1 mois / 15 j OQTF + filtre pourvoi CE).
import { AppelCaaCassationSectionComponent } from '../appel-caa-cassation-section/appel-caa-cassation-section.component';
// SF-214-36 : composant complet F-IM-42-assignation-residence-fr — assignation à résidence (FR, analyseur validité/délais + bridge échéance F-69).
import { AssignationResidenceSectionComponent } from '../assignation-residence-section/assignation-residence-section.component';
// SF-214-38 : composant complet F-IM-43-itf-judiciaire-fr — ITF judiciaire (FR, analyseur validité/délais + encadré ITF vs IRTF + bridge échéance F-69).
import { ItfJudiciaireSectionComponent } from '../itf-judiciaire-section/itf-judiciaire-section.component';
// SF-214-40 : composant complet F-IM-44-ue-eee-suisse-sejour-fr — séjour UE/EEE/Suisse (FR, analyseur de droits + encadré membre de famille non-UE).
import { UeEeeSuisseSejourSectionComponent } from '../ue-eee-suisse-sejour-section/ue-eee-suisse-sejour-section.component';
// SF-214-42 : composant complet F-IM-45-retrait-titre-fraude-fr — retrait titre pour fraude (FR, analyseur validité + vices de procédure + bridge échéance F-69).
import { RetraitTitreFraudeSectionComponent } from '../retrait-titre-fraude-section/retrait-titre-fraude-section.component';
// SF-214-44 : composant complet F-IM-46-autorisation-travail-employeur-fr — autorisation travail employeur (FR, checklist procédure OFII + délai instruction + bridge échéance F-69 si refus).
import { AutorisationTravailEmployeurSectionComponent } from '../autorisation-travail-employeur-section/autorisation-travail-employeur-section.component';
// SF-214-14 : composant complet F-IM-31-renouvellement-delai-depot-fr — renouvellement délai dépôt (FR, calculateur délai optimal/impératif).
import { RenouvellementDelaiSectionComponent } from '../renouvellement-delai-section/renouvellement-delai-section.component';
// SF-214-10 : composant complet F-IM-29-oqtf-categories-l6111-fr — OQTF catégories L.611-1 (FR, moyens de défense).
import { OqtfCategoriesSectionComponent } from '../oqtf-categories-section/oqtf-categories-section.component';
// SF-214-16 : composant complet F-IM-32-recepisse-attestation-fr — récépissé vs attestation (FR, analyseur droits séjour/travail + risque employeur).
import { RecepisseAttestationSectionComponent } from '../recepisse-attestation-section/recepisse-attestation-section.component';
// SF-214-18 : composant complet F-IM-33-ofpra-introduction-fr — OFPRA introduction (FR, échéance + stepper 5 étapes + risque procédure accélérée).
import { OfpraIntroductionSectionComponent } from '../ofpra-introduction-section/ofpra-introduction-section.component';
// SF-214-26 : composant complet F-IM-37-anef-procedure-fr — ANEF procédure / pannes (FR, guide pas-à-pas + recours panne téléservice).
import { AnefProcedureSectionComponent } from '../anef-procedure-section/anef-procedure-section.component';
// SF-214-20 : composant complet F-IM-34-aj-cnda-fr — aide juridictionnelle CNDA (FR, ressources + délais, bridge échéance F-69).
import { AjCndaSectionComponent } from '../aj-cnda-section/aj-cnda-section.component';
// SF-214-28 : composant complet F-IM-38-mna-evaluation-age-fr — MNA évaluation âge / recours JE (FR, stepper procédure ASE + contestation osseux, bridge échéance F-69).
import { MnaEvaluationAgeSectionComponent } from '../mna-evaluation-age-section/mna-evaluation-age-section.component';
// SF-214-22 : composant complet F-IM-35-victime-traite-l4251-fr — protection victime de traite L. 425-1 (FR, alerte sécurité si victime en danger).
import { VictimeTraiteSectionComponent } from '../victime-traite-section/victime-traite-section.component';
// SF-214-24 : composant complet F-IM-36-carte-resident-l4261-fr — carte de résident L. 426-1 (FR, checklist critères + atouts).
import { CarteResidentSectionComponent } from '../carte-resident-section/carte-resident-section.component';
// SF-214-12 : composant complet F-IM-30-aes-presence-prouvee-fr — AES présence prouvée (FR, calcul périodes + 4 voies).
import { AesPresenceProuveeSectionComponent } from '../aes-presence-prouvee-section/aes-presence-prouvee-section.component';
// SF-215-02 : composant complet F-IM-25-single-permit-be permis unique BE (travail+séjour, BELGIQUE).
import { SinglePermitBeSectionComponent } from '../single-permit-be-section/single-permit-be-section.component';
// SF-215-04 : composant complet F-IM-26-regroupement-10ter-be (Immigration BE, regroupement familial art. 10ter).
import { Regroupement10terBeSectionComponent } from '../regroupement-10ter-be-section/regroupement-10ter-be-section.component';
// SF-215-14 : composant complet F-IM-31-cce-annulation-30j-be (Immigration BE, recours CCE annulation 30j).
// CCE = Conseil du Contentieux des Étrangers (droit des étrangers belge).
import { CceAnnulationBeSectionComponent } from '../cce-annulation-be-section/cce-annulation-be-section.component';
// SF-215-16 : composant complet F-IM-32-cce-extreme-urgence-5j-be (Immigration BE, recours CCE extrême urgence 5j ouvrables).
import { CceExtremeUrgenceBeSectionComponent } from '../cce-extreme-urgence-be-section/cce-extreme-urgence-be-section.component';
// SF-215-18 : composant complet F-IM-33-annexe13quinquies-ie-be (Immigration BE, annexe 13quinquies OQT + interdiction d'entrée Schengen).
import { Annexe13quinquiesBeSectionComponent } from '../annexe13quinquies-be-section/annexe13quinquies-be-section.component';
// SF-215-20 : composant complet F-IM-34-protection-temporaire-ukraine-be (Immigration BE, protection temporaire Ukraine — décision (UE) 2022/382).
import { ProtectionTemporaireUkraineBeSectionComponent } from '../protection-temporaire-ukraine-be-section/protection-temporaire-ukraine-be-section.component';
// SF-215-06 : composant complet F-IM-27-regroupement-10bis-be (Immigration BE, regroupement familial art. 10bis — séjour LIMITÉ carte A).
import { Regroupement10bisBeSectionComponent } from '../regroupement-10bis-be-section/regroupement-10bis-be-section.component';
// SF-215-08 : composant complet F-IM-28-naturalisation-12bis-be (Immigration BE, naturalisation art. 12bis — voie 5/10 ans).
import { Naturalisation12bisBeSectionComponent } from '../naturalisation-12bis-be-section/naturalisation-12bis-be-section.component';
// SF-215-10 : composant complet F-IM-29-naturalisation-conjoint-belge-be (Immigration BE, naturalisation conjoint Belge art. 16).
import { NaturalisationConjointBelgeBeSectionComponent } from '../naturalisation-conjoint-belge-be-section/naturalisation-conjoint-belge-be-section.component';
// SF-215-12 : composant complet F-IM-30-aesm-mena-be (Immigration BE, AESM + tutelle DGDE MENA — composite 2 volets, CONTEXTUAL).
import { AesmMenaBeSectionComponent } from '../aesm-mena-be-section/aesm-mena-be-section.component';
import { IndemniteComparatifSectionComponent } from '../indemnite-comparatif-section/indemnite-comparatif-section.component';
import { PrudhomeFicheSectionComponent } from '../prudhome-fiche-section/prudhome-fiche-section.component';
import { TribunalTravailFicheSectionComponent } from '../tribunal-travail-fiche-section/tribunal-travail-fiche-section.component';
// SF-207-01b : section décisionnelle prescription Travail BE (BE-only, ALWAYS_ON).
import { PrescriptionBeLitigeTravailSectionComponent } from '../prescription-be-litige-travail-section/prescription-be-litige-travail-section.component';
// SF-207-02b : section décisionnelle checklist C4 ONEM (BE-only, ALWAYS_ON).
import { C4OnemChecklistSectionComponent } from '../c4-onem-checklist-section/c4-onem-checklist-section.component';
// SF-207-03b : section décisionnelle contestation C4 ONEM (BE-only, ALWAYS_ON).
import { ContestationC4OnemSectionComponent } from '../contestation-c4-onem-section/contestation-c4-onem-section.component';
// SF-207-04b : section décisionnelle déclaration AT Fedris (BE-only, ALWAYS_ON).
import { AtFedrisDeclarationSectionComponent } from '../at-fedris-declaration-section/at-fedris-declaration-section.component';
// SF-207-05b : section décisionnelle référé tribunal du travail BE (BE-only, ALWAYS_ON).
import { RefereTribunalTravailBeSectionComponent } from '../refere-tribunal-travail-be-section/refere-tribunal-travail-be-section.component';
// SF-207-06b : section décisionnelle RCC BE conditions d'éligibilité (BE-only, ALWAYS_ON).
import { RccBeConditionsSectionComponent } from '../rcc-be-conditions-section/rcc-be-conditions-section.component';
// SF-207-07b : section décisionnelle RCC BE indemnité complémentaire (BE-only, ALWAYS_ON).
import { RccBeIndemniteComplementaireSectionComponent } from '../rcc-be-indemnite-complementaire-section/rcc-be-indemnite-complementaire-section.component';
// SF-207-08b : section décisionnelle outplacement BE obligatoire 45+ (BE-only, ALWAYS_ON).
import { OutplacementBeObligatoire45SectionComponent } from '../outplacement-be-obligatoire-45-section/outplacement-be-obligatoire-45-section.component';
import { PartageImmobilierSectionComponent } from '../partage-immobilier-section/partage-immobilier-section.component';
import { CalendrierGardeSectionComponent } from '../calendrier-garde-section/calendrier-garde-section.component';
import { DivorceChecklistSectionComponent } from '../divorce-checklist-section/divorce-checklist-section.component';
import { ImmigrationTitleDecisionSectionComponent } from '../immigration-title-decision-section/immigration-title-decision-section.component';
import { ImmigrationRecoursSectionComponent } from '../immigration-recours-section/immigration-recours-section.component';
import { ImmigrationWorkRightSectionComponent } from '../immigration-work-right-section/immigration-work-right-section.component';
import { ImmigrationChecklistSectionComponent } from '../immigration-checklist-section/immigration-checklist-section.component';
import { HarcelementLicenciementNulSectionComponent } from '../harcelement-licenciement-nul-section/harcelement-licenciement-nul-section.component';
import { LicenciementNulDetectionSectionComponent } from '../licenciement-nul-detection-section/licenciement-nul-detection-section.component';
import { ProcedureNulliteLicenciementSectionComponent } from '../procedure-nullite-licenciement-section/procedure-nullite-licenciement-section.component';
import { AbandonPostePresomptionDemissionSectionComponent } from '../abandon-poste-presomption-demission-section/abandon-poste-presomption-demission-section.component';
import { VrpIndemniteClienteleSectionComponent } from '../vrp-indemnite-clientele-section/vrp-indemnite-clientele-section.component';
import { CongesPayesArretMaladieSectionComponent } from '../conges-payes-arret-maladie-section/conges-payes-arret-maladie-section.component';
import { PriseActeRuptureSectionComponent } from '../prise-acte-rupture-section/prise-acte-rupture-section.component';
// SF-212-02 : outil F-DT-36 licenciement pour faute grave / faute lourde (FR uniquement).
import { LicenciementFauteGraveLourdSectionComponent } from '../licenciement-faute-grave-lourd-section/licenciement-faute-grave-lourd-section.component';
// SF-212-04 : outil F-DT-50 forfait jours validité (FR uniquement).
import { ForfaitJoursFrSectionComponent } from '../forfait-jours-fr-section/forfait-jours-fr-section.component';
// SF-212-06 : outil F-DT-72 transfert d'entreprise — L. 1224-1 (FR uniquement).
import { TransfertEntrepriseFrSectionComponent } from '../transfert-entreprise-fr-section/transfert-entreprise-fr-section.component';
// SF-212-08 : outil F-DT-44 CSP/CRP — conformité de la proposition (FR uniquement).
import { CspCrpFrSectionComponent } from '../csp-crp-fr-section/csp-crp-fr-section.component';
// SF-212-10 : outil F-DT-91 faute inexcusable de l'employeur (FR uniquement).
import { FauteInexcusableFrSectionComponent } from '../faute-inexcusable-fr-section/faute-inexcusable-fr-section.component';
// SF-212-26 : outil F-DT-61 protection du lanceur d'alerte (FR uniquement).
import { LanceurAlerteProtectionSectionComponent } from '../lanceur-alerte-protection-section/lanceur-alerte-protection-section.component';
// SF-212-12 : outil F-DT-70 modification du contrat — refus du salarié (FR uniquement).
import { ModificationContratRefusSectionComponent } from '../modification-contrat-refus-section/modification-contrat-refus-section.component';
// SF-212-14 : outil F-DT-71 mutation — validité de la clause de mobilité (FR uniquement).
import { MutationClauseMobiliteSectionComponent } from '../mutation-clause-mobilite-section/mutation-clause-mobilite-section.component';
// SF-212-16 : outil F-DT-82 télétravail — conformité et litige (FR uniquement).
import { TeletravailAccordSectionComponent } from '../teletravail-accord-section/teletravail-accord-section.component';
// SF-212-20 : outil F-DT-48 mise à pied disciplinaire — régularité (FR uniquement).
import { MiseAPiedDisciplinaireSectionComponent } from '../mise-a-pied-disciplinaire-section/mise-a-pied-disciplinaire-section.component';
// SF-212-24 : outil F-DT-56 égalité salariale femmes/hommes (FR uniquement).
import { EgaliteSalarialeFhSectionComponent } from '../egalite-salariale-fh-section/egalite-salariale-fh-section.component';
// SF-212-22 : outil F-DT-41 démission validité équivoque (FR uniquement).
import { DemissionEquivoqueSectionComponent } from '../demission-equivoque-section/demission-equivoque-section.component';
// SF-212-36 : outil F-DT-46 PDV / RCC — conformité (FR uniquement).
import { PdvRccConformiteSectionComponent } from '../pdv-rcc-conformite-section/pdv-rcc-conformite-section.component';
// SF-212-30 : outil F-DT-77 congé maternité / paternité — protection & indemnités (FR uniquement).
import { CongeMaternitePaterniteSectionComponent } from '../conge-maternite-paternite-section/conge-maternite-paternite-section.component';
// SF-212-28 : outil F-DT-64 burn-out — reconnaissance maladie professionnelle hors tableau (FR uniquement).
import { BurnoutReconnaissanceMpSectionComponent } from '../burnout-reconnaissance-mp-section/burnout-reconnaissance-mp-section.component';
// SF-212-32 : outil F-DT-65 élections CSE — conformité procédure (FR uniquement).
import { ElectionsCseConformiteSectionComponent } from '../elections-cse-conformite-section/elections-cse-conformite-section.component';
// SF-212-34 : outil F-DT-49 temps partiel — requalification en temps plein (FR uniquement).
import { TempsPartielRequalificationSectionComponent } from '../temps-partiel-requalification-section/temps-partiel-requalification-section.component';
// SF-212-38 : outil F-DT-84 conciliation CPH — Bureau de Conciliation et d'Orientation (BCO) (FR uniquement). F-212 19/19.
import { ConciliationCphBcaSectionComponent } from '../conciliation-cph-bca-section/conciliation-cph-bca-section.component';
// SF-212-18 : outil F-DT-43 rupture anticipée du CDD (FR uniquement).
import { RuptureAnticipeeCddSectionComponent } from '../rupture-anticipee-cdd-section/rupture-anticipee-cdd-section.component';
import { ResiliationJudiciaireCphSectionComponent } from '../resiliation-judiciaire-cph-section/resiliation-judiciaire-cph-section.component';
import { RupturePeriodeEssaiSectionComponent } from '../rupture-periode-essai-section/rupture-periode-essai-section.component';
import { DiscriminationSectionComponent } from '../discrimination-section/discrimination-section.component';
import { LicenciementEconomiqueSectionComponent } from '../licenciement-economique-section/licenciement-economique-section.component';
import { InaptitudeSectionComponent } from '../inaptitude-section/inaptitude-section.component';
import { HeuresSupSectionComponent } from '../heures-sup-section/heures-sup-section.component';
import { IndemnitePrecariteCddSectionComponent } from '../indemnite-precarite-cdd-section/indemnite-precarite-cdd-section.component';
import { CongesPayesSectionComponent } from '../conges-payes-section/conges-payes-section.component';
import { FinMissionInterimSectionComponent } from '../fin-mission-interim-section/fin-mission-interim-section.component';
import { TravailDissimuleSectionComponent } from '../travail-dissimule-section/travail-dissimule-section.component';
import { IndemnitePreavisSectionComponent } from '../indemnite-preavis-section/indemnite-preavis-section.component';
import { RappelSalaireSectionComponent } from '../rappel-salaire-section/rappel-salaire-section.component';
import { RequalificationCddCdiSectionComponent } from '../requalification-cdd-cdi-section/requalification-cdd-cdi-section.component';
import { RequalificationInterimCdiSectionComponent } from '../requalification-interim-cdi-section/requalification-interim-cdi-section.component';
import { NonConcurrenceSectionComponent } from '../non-concurrence-section/non-concurrence-section.component';
import { OqtfAvecDelaiSectionComponent } from '../oqtf-avec-delai-section/oqtf-avec-delai-section.component';
import { OqtfSansDelaiSectionComponent } from '../oqtf-sans-delai-section/oqtf-sans-delai-section.component';
import { Annexe13BeSectionComponent } from '../annexe13-be-section/annexe13-be-section.component';
import { ReferesAdminSectionComponent } from '../referes-admin-section/referes-admin-section.component';
import { ContestationAreSectionComponent } from '../contestation-are-section/contestation-are-section.component';
import { MotifGraveBeSectionComponent } from '../motif-grave-be-section/motif-grave-be-section.component';
import { ClauseNonConcurrenceBeSectionComponent } from '../clause-non-concurrence-be-section/clause-non-concurrence-be-section.component';
import { RappelSalaireBeSectionComponent } from '../rappel-salaire-be-section/rappel-salaire-be-section.component';
import { LicenciementBeStatutUniquePreavisSectionComponent } from '../licenciement-be-statut-unique-preavis-section/licenciement-be-statut-unique-preavis-section.component';
import { LicenciementBeFormuleClaeysSectionComponent } from '../licenciement-be-formule-claeys-section/licenciement-be-formule-claeys-section.component';
import { LicenciementBeProtectionGrossesseSectionComponent } from '../licenciement-be-protection-grossesse-section/licenciement-be-protection-grossesse-section.component';
import { TransactionBeTravailSectionComponent } from '../transaction-be-travail-section/transaction-be-travail-section.component';
import { HarcelementBeProcedureFormelleSectionComponent } from '../harcelement-be-procedure-formelle-section/harcelement-be-procedure-formelle-section.component';
import { LicenciementBeProtectionDelegueeSectionComponent } from '../licenciement-be-protection-deleguee-section/licenciement-be-protection-deleguee-section.component';
import { LicenciementBeActeEquivalentSectionComponent } from '../licenciement-be-acte-equivalent-section/licenciement-be-acte-equivalent-section.component';
import { LicenciementBeCct109DeraisonnableSectionComponent } from '../licenciement-be-cct109-deraisonnable-section/licenciement-be-cct109-deraisonnable-section.component';
// SF-219-02b : section décisionnelle RCC BE longue carrière (BE-only, ALWAYS_ON).
import { RccBeLongueCarriereSectionComponent } from '../rcc-be-longue-carriere-section/rcc-be-longue-carriere-section.component';
// SF-219-01b : section décisionnelle RCC métiers lourds BE (BE-only, ALWAYS_ON).
import { RccBeMetiersLourdsSectionComponent } from '../rcc-be-metiers-lourds-section/rcc-be-metiers-lourds-section.component';
// SF-219-03b : section décisionnelle RCC BE entreprise en difficulté / restructuration (BE-only, ALWAYS_ON).
import { RccBeEntrepriseDifficulteSectionComponent } from '../rcc-be-entreprise-difficulte-section/rcc-be-entreprise-difficulte-section.component';
// SF-219-04b : section décisionnelle Cumul RCC + allocations (BE-only, ALWAYS_ON).
import { CumulRccAllocationsSectionComponent } from '../cumul-rcc-allocations-section/cumul-rcc-allocations-section.component';
import { OutplacementBeGeneral30semSectionComponent } from '../outplacement-be-general-30sem-section/outplacement-be-general-30sem-section.component';
// SF-219-06b : section décisionnelle Licenciement BE fermeture d'entreprise (BE-only, ALWAYS_ON).
import { LicenciementBeFermetureEntrepriseSectionComponent } from '../licenciement-be-fermeture-entreprise-section/licenciement-be-fermeture-entreprise-section.component';
// SF-219-07b : section décisionnelle Licenciement collectif BE — Loi Renault (BE-only, ALWAYS_ON).
import { LicenciementBeCollectifRenaultSectionComponent } from '../licenciement-be-collectif-renault-section/licenciement-be-collectif-renault-section.component';
// SF-219-08b : section décisionnelle Transfert d'entreprise CCT 32bis (BE-only, ALWAYS_ON).
import { TransfertEntrepriseCct32bisSectionComponent } from '../transfert-entreprise-cct-32bis-section/transfert-entreprise-cct-32bis-section.component';
// SF-219-09b : section décisionnelle Élections sociales BE (BE-only, ALWAYS_ON).
import { ElectionsSocialesBeSectionComponent } from '../elections-sociales-be-section/elections-sociales-be-section.component';
// SF-219-10b : section décisionnelle Statut délégué syndical CCT n° 5 (BE-only, ALWAYS_ON).
import { DelegueSyndicalCct5SectionComponent } from '../delegue-syndical-cct-5-section/delegue-syndical-cct-5-section.component';
// SF-219-11b : section décisionnelle Congé-éducation payé régionalisé (BE-only, ALWAYS_ON).
import { CongeEducationPayeRegionSectionComponent } from '../conge-education-paye-region-section/conge-education-paye-region-section.component';
// SF-219-12b : section décisionnelle Flexi-job BE (BE-only, ALWAYS_ON).
import { FlexiJobBeSectionComponent } from '../flexi-job-be-section/flexi-job-be-section.component';
// SF-219-13b : section décisionnelle Étudiant jobiste BE (BE-only, ALWAYS_ON).
import { EtudiantJobisteBeSectionComponent } from '../etudiant-jobiste-be-section/etudiant-jobiste-be-section.component';
// SF-219-14b : section décisionnelle statut intérim BE — CCT n° 322 (BE-only, ALWAYS_ON).
import { InterimBeCct322SectionComponent } from '../interim-be-cct-322-section/interim-be-cct-322-section.component';
// SF-219-15b : section décisionnelle indemnité fin de mission intérim BE (BE-only, ALWAYS_ON).
import { InterimBeIndemniteFinMissionSectionComponent } from '../interim-be-indemnite-fin-mission-section/interim-be-indemnite-fin-mission-section.component';
// SF-219-16b : section décisionnelle Télétravail BE — CCT n° 85 / CCT n° 149 (BE-only, ALWAYS_ON).
import { TeletravailBeCct85149SectionComponent } from '../teletravail-be-cct-85-149-section/teletravail-be-cct-85-149-section.component';
// SF-219-17b : section décisionnelle clause d'écolage BE — art. 22bis Loi 03/07/1978 (BE-only, ALWAYS_ON).
import { ClauseEcolageBeSectionComponent } from '../clause-ecolage-be-section/clause-ecolage-be-section.component';
// SF-219-18b : section décisionnelle Semaine de 4 jours BE — Loi 03/10/2022 « Deal pour l'emploi » (BE-only, ALWAYS_ON).
import { Semaine4JoursBeSectionComponent } from '../semaine-4-jours-be-section/semaine-4-jours-be-section.component';
// SF-219-19b : section décisionnelle Droit à la déconnexion BE — Loi 03/10/2022 art. 16 + AR 19/02/2023 + CCT 149 (BE-only, ALWAYS_ON).
import { DroitDeconnexionBeSectionComponent } from '../droit-deconnexion-be-section/droit-deconnexion-be-section.component';
// SF-219-20b : section décisionnelle Pécule de vacances BE — Lois 28/06/1971 + AR 30/03/1967 (BE-only, ALWAYS_ON).
import { PeculeVacancesBeSectionComponent } from '../pecule-vacances-be-section/pecule-vacances-be-section.component';
// SF-219-21b : section décisionnelle Éco-chèques + chèques-repas BE — CCT n°98 + Loi 25/04/2014 + AR 03/02/2010 (BE-only, ALWAYS_ON).
import { EcoChequesChequesRepasBeSectionComponent } from '../eco-cheques-cheques-repas-be-section/eco-cheques-cheques-repas-be-section.component';
// SF-219-22b : section décisionnelle Égalité salariale F/H BE — Loi 22/04/2012 + AR 17/08/2013 + AR 25/04/2014 (BE-only, ALWAYS_ON).
import { EgaliteFemmesHommesBeSectionComponent } from '../egalite-femmes-hommes-be-section/egalite-femmes-hommes-be-section.component';
// SF-219-23b : section décisionnelle refus d'aménagements raisonnables handicap BE — Loi 10/05/2007 + CCT n° 95 + Directive 2000/78/CE art. 5 (BE-only, ALWAYS_ON).
import { DiscriminationBeHandicapAmenagementSectionComponent } from '../discrimination-be-handicap-amenagement-section/discrimination-be-handicap-amenagement-section.component';
// SF-219-24b : section décisionnelle Code pénal social BE — Loi 06/06/2010 (BE-only, ALWAYS_ON, qualification d'infraction + niveau de sanction 1 à 4).
import { CodePenalSocialBeSectionComponent } from '../code-penal-social-be-section/code-penal-social-be-section.component';
// SF-219-25b : section décisionnelle Auditorat du travail BE.
import { AuditoratTravailBeSectionComponent } from '../auditorat-travail-be-section/auditorat-travail-be-section.component';
// SF-219-26b : section décisionnelle Travail noir BE DIMONA.
import { TravailNoirBeDimonaSectionComponent } from '../travail-noir-be-dimona-section/travail-noir-be-dimona-section.component';
// SF-219-27b : section décisionnelle INASTI statut travailleur indépendant BE.
import { InastriStatutTravailleurIndependantSectionComponent } from '../inastri-statut-travailleur-independant-section/inastri-statut-travailleur-independant-section.component';
// SF-219-28b : section décisionnelle MP Fedris reconnaissance BE.
import { MpFedrisReconnaissanceSectionComponent } from '../mp-fedris-reconnaissance-section/mp-fedris-reconnaissance-section.component';
// SF-219-29b : section décisionnelle Rente AT/MP vs capitalisation BE.
import { AtMpRenteCapitalBeSectionComponent } from '../at-mp-rente-capital-be-section/at-mp-rente-capital-be-section.component';
// SF-219-30b : section décisionnelle Saisine CPAP BE (RPS) — Loi 04/08/1996 art. 32sexies + AR 10/04/2014.
import { BienEtreRpsConseillerPreventionSectionComponent } from '../bien-etre-rps-conseiller-prevention-section/bien-etre-rps-conseiller-prevention-section.component';
// SF-219-31b : section décisionnelle Conge paternite / naissance BE — Loi 03/07/1978 art. 30 paragr. 2 + Loi 07/04/2023.
import { CongePaterniteNaissanceBeSectionComponent } from '../conge-paternite-naissance-be-section/conge-paternite-naissance-be-section.component';
// SF-219-32b : section décisionnelle Interruption de carriere conge parental BE — Loi 22/01/1985 + AR 29/10/1997 + CCT 64.
import { InterruptionCarriereSoinsParentalSectionComponent } from '../interruption-carriere-soins-parental-section/interruption-carriere-soins-parental-section.component';
import { Belgian40terSectionComponent } from '../belgian-40ter-section/belgian-40ter-section.component';
import { Belgian9bisSectionComponent } from '../belgian-9bis-section/belgian-9bis-section.component';
import { Belgian9terSectionComponent } from '../belgian-9ter-section/belgian-9ter-section.component';
import { BelgianCohabitantUeBeSectionComponent } from '../belgian-40bis-section/belgian-40bis-section.component';
import { AesMetiersTensionSectionComponent } from '../aes-metiers-tension-section/aes-metiers-tension-section.component';
import { AesFamilleSectionComponent } from '../aes-famille-section/aes-famille-section.component';
import { DivorceAlterationSectionComponent } from '../divorce-alteration-section/divorce-alteration-section.component';
import { DivorceFauteSectionComponent } from '../divorce-faute-section/divorce-faute-section.component';
import { DivorceAccepteSectionComponent } from '../divorce-accepte-section/divorce-accepte-section.component';
import { OrdonnanceProtectionSectionComponent } from '../ordonnance-protection-section/ordonnance-protection-section.component';
import { RevisionsPostDivorceSectionComponent } from '../revisions-post-divorce-section/revisions-post-divorce-section.component';
import { TravailProcedureSectionComponent } from '../travail-procedure-section/travail-procedure-section.component';
import { RecompensesSectionComponent } from '../recompenses-section/recompenses-section.component';
import { AutoriteParentaleSectionComponent } from '../autorite-parentale-section/autorite-parentale-section.component';
import { ChangementResidenceSectionComponent } from '../changement-residence-section/changement-residence-section.component';
import { DesaccordsParentauxSectionComponent } from '../desaccords-parentaux-section/desaccords-parentaux-section.component';
import { MesuresProvisoiresSectionComponent } from '../mesures-provisoires-section/mesures-provisoires-section.component';
import { DocumentsFinContratSectionComponent } from '../documents-fin-contrat-section/documents-fin-contrat-section.component';
import { IndivisionSectionComponent } from '../indivision-section/indivision-section.component';
import { PacsDissolutionSectionComponent } from '../pacs-dissolution-section/pacs-dissolution-section.component';
import { MajeursProtegesSectionComponent } from '../majeurs-proteges-section/majeurs-proteges-section.component';
import { ChangementEtatCivilSectionComponent } from '../changement-etat-civil-section/changement-etat-civil-section.component';
import { AvantagesConventionnelsBeSectionComponent } from '../avantages-conventionnels-be-section/avantages-conventionnels-be-section.component';
import { SeparationCorpsSectionComponent } from '../separation-corps-section/separation-corps-section.component';
import { ReferePrudhomalSectionComponent } from '../refere-prudhomal-section/refere-prudhomal-section.component';
import { CreditTempsBeSectionComponent } from '../credit-temps-be-section/credit-temps-be-section.component';
import { PseSectionComponent } from '../pse-section/pse-section.component';
import { ProtectionRpSectionComponent } from '../protection-rp-section/protection-rp-section.component';
import { AtMpSectionComponent } from '../at-mp-section/at-mp-section.component';
import { OrdonnanceRequeteSectionComponent } from '../ordonnance-requete-section/ordonnance-requete-section.component';
import { ChangementStatutSectionComponent } from '../changement-statut-section/changement-statut-section.component';
import { NaturalisationSectionComponent } from '../naturalisation-section/naturalisation-section.component';
import { MineursImmigrationSectionComponent } from '../mineurs-immigration-section/mineurs-immigration-section.component';
import { MesuresEloignementSectionComponent } from '../mesures-eloignement-section/mesures-eloignement-section.component';
import { AsileAvanceSectionComponent } from '../asile-avance-section/asile-avance-section.component';
import { PartageJudiciaireSectionComponent } from '../partage-judiciaire-section/partage-judiciaire-section.component';
import { DevolutionLegaleSectionComponent } from '../devolution-legale-section/devolution-legale-section.component';
import { TestamentValiditeSectionComponent } from '../testament-validite-section/testament-validite-section.component';
import { DonationSectionComponent } from '../donation-section/donation-section.component';
import { ReserveHeriditaireSectionComponent } from '../reserve-heriditaire-section/reserve-heriditaire-section.component';
import { IndivisionSuccessoraleSectionComponent } from '../indivision-successorale-section/indivision-successorale-section.component';
import { PartageSuccessoralSectionComponent } from '../partage-successoral-section/partage-successoral-section.component';
import { RapportSuccessionSectionComponent } from '../rapport-succession-section/rapport-succession-section.component';
import { ReconnaissancePaternelleSectionComponent } from '../reconnaissance-paternelle-section/reconnaissance-paternelle-section.component';
import { ContestationPaterniteSectionComponent } from '../contestation-paternite-section/contestation-paternite-section.component';
import { RecherchePaterniteSectionComponent } from '../recherche-paternite-section/recherche-paternite-section.component';
import { PossessionEtatSectionComponent } from '../possession-etat-section/possession-etat-section.component';
import { AdoptionSectionComponent } from '../adoption-section/adoption-section.component';
import { CommunauteUniverselleSectionComponent } from '../communaute-universelle-section/communaute-universelle-section.component';
import { PmaGpaBioethiqueSectionComponent } from '../pma-gpa-bioethique-section/pma-gpa-bioethique-section.component';
import { RegimeAlgerienSectionComponent } from '../regime-algerien-section/regime-algerien-section.component';
// SF-164-01 : 5 entrées TOOL_REGISTRY manquantes pour des composants existants
// (DB seedait `decision_tool_visibility_rules` sans entrée registry → outils
// silencieusement masqués via `console.warn` dans `resolveEntry()`).
import { CaseDeadlinesSectionComponent } from '../case-deadlines-section/case-deadlines-section.component';
import { TransactionSectionComponent } from '../transaction-section/transaction-section.component';
import { AesEtudiantSectionComponent } from '../aes-etudiant-section/aes-etudiant-section.component';
import { AesHumanitaireSectionComponent } from '../aes-humanitaire-section/aes-humanitaire-section.component';
import { DivorceDesunionBeSectionComponent } from '../divorce-desunion-be-section/divorce-desunion-be-section.component';
// F-198 : rattrapage des 5 outils Famille FR DELETE par migration 191. Wrappers
// présentationnels qui rendent les estimations IA (pension alimentaire, prestation
// compensatoire, liquidation communauté, divorce CM scoring, fourchettes JAF).
import { PrestationCompensatoireSectionComponent } from '../prestation-compensatoire-section/prestation-compensatoire-section.component';
import { LiquidationCommunauteSectionComponent } from '../liquidation-communaute-section/liquidation-communaute-section.component';
// SF-216-08 : composant simulateur ARIPA recouvrement pension impayée (F-FA-ARIPA-RECOUVREMENT).
import { AripaRecouvrementFrSectionComponent } from '../aripa-recouvrement-fr-section/aripa-recouvrement-fr-section.component';
// SF-216-10 : composant simulateur délégation autorité parentale (F-FA-XX-delegation-ap).
import { DelegationApFrSectionComponent } from '../delegation-ap-fr-section/delegation-ap-fr-section.component';
// SF-216-12 : composant simulateur retrait autorité parentale (F-FA-RETRAIT-AP).
import { RetraitApFrSectionComponent } from '../retrait-ap-fr-section/retrait-ap-fr-section.component';
// SF-216-16 : composant simulateur Adoption intra-familiale FR (F-FA-ADOPTION-INTRA).
import { AdoptionIntraFrSectionComponent } from '../adoption-intra-fr-section/adoption-intra-fr-section.component';
// SF-216-18 : composant simulateur Adoption internationale FR (F-FA-ADOPTION-INTERNATIONALE).
import { AdoptionInternationaleFrSectionComponent } from '../adoption-internationale-fr-section/adoption-internationale-fr-section.component';
// SF-216-14 : composant simulateur Audition du mineur FR (F-FA-AUDITION-MINEUR).
import { AuditionMineurFrSectionComponent } from '../audition-mineur-fr-section/audition-mineur-fr-section.component';
// SF-216-20 : composant simulateur Indignité successorale FR (F-FA-INDIGNITE-SUCCESSORALE).
import { IndigniteSuccessoraleFrSectionComponent } from '../indignite-successorale-fr-section/indignite-successorale-fr-section.component';
// SF-216-22 : composant simulateur Recel de succession FR (F-FA-RECEL-SUCCESSION).
import { RecelSuccessionFrSectionComponent } from '../recel-succession-fr-section/recel-succession-fr-section.component';
// SF-216-24 : composant simulateur Donation entre époux FR (F-FA-DONATION-ENTRE-EPOUX).
import { DonationEntreEpouxFrSectionComponent } from '../donation-entre-epoux-fr-section/donation-entre-epoux-fr-section.component';
// SF-216-28 : composant simulateur Partage successoral notarié FR (F-FA-PARTAGE-NOTARIAL).
import { PartageNotarialFrSectionComponent } from '../partage-notarial-fr-section/partage-notarial-fr-section.component';
// SF-216-26 : composant simulateur Présomption de paternité FR (F-FA-PRESOMPTION-PATERNITE).
import { PresomptionPaterniteFrSectionComponent } from '../presomption-paternite-fr-section/presomption-paternite-fr-section.component';
// SF-216-30 : composant simulateur Donation-partage FR (F-FA-DONATION-PARTAGE).
import { DonationPartageFrSectionComponent } from '../donation-partage-fr-section/donation-partage-fr-section.component';
// SF-216-04 : nouveau composant simulateur F-FA-02 (remplace le wrapper
// SF-198-02 `PensionAlimentaireSectionComponent` qui n'est plus référencé).
import { PensionAlimentaireEnfantFrSectionComponent } from '../pension-alimentaire-enfant-fr-section/pension-alimentaire-enfant-fr-section.component';
import { DivorceCmScoringSectionComponent } from '../divorce-cm-scoring-section/divorce-cm-scoring-section.component';
import { FourchettesJafSectionComponent } from '../fourchettes-jaf-section/fourchettes-jaf-section.component';
// F-210 — 2 outils urgences procédurales Famille FR.
import { MediationFamilialeSectionComponent } from '../mediation-familiale-section/mediation-familiale-section.component';
import { AcceptationRenonciationSectionComponent } from '../acceptation-renonciation-section/acceptation-renonciation-section.component';
// F-211 SF-211-05 — 4 wrappers informationnels Famille BE (backend mergé PR #942).
import { DivorceDcBeSectionComponent } from '../divorce-dc-be-section/divorce-dc-be-section.component';
import { DivorceDdiBeSectionComponent } from '../divorce-ddi-be-section/divorce-ddi-be-section.component';
import { TribunalFamilleBeMesuresProvisoiresSectionComponent } from '../tribunal-famille-be-mesures-provisoires-section/tribunal-famille-be-mesures-provisoires-section.component';
import { PacteSuccessoralBe2018SectionComponent } from '../pacte-successoral-be-2018-section/pacte-successoral-be-2018-section.component';
// F-217 SF-217-03 — 2 sections décisionnelles Vague 1 Famille BE (backends mergés PR #983 / #982).
import { RegimeCommunauteLegaleBeSectionComponent } from '../regime-communaute-legale-be-section/regime-communaute-legale-be-section.component';
import { LiquidationPartageBeSectionComponent } from '../liquidation-partage-be-section/liquidation-partage-be-section.component';
// F-217 SF-217-05 / 07 / 09 — sections décisionnelles Vague 2 Famille BE — Enfants (backends mergés PR #993 / #995 / #998).
import { AutoriteParentaleBeSectionComponent } from '../autorite-parentale-be-section/autorite-parentale-be-section.component';
import { ContributionAlimentaireEnfantsBeSectionComponent } from '../contribution-alimentaire-enfants-be-section/contribution-alimentaire-enfants-be-section.component';
import { ContributionConjointBeSectionComponent } from '../contribution-conjoint-be-section/contribution-conjoint-be-section.component';
// F-217 SF-217-13 — 2 sections décisionnelles Vague 3 Famille BE — Successions (backends mergés PR #1180 / #1181).
import { SuccessionBeDevolutionReserveSectionComponent } from '../succession-be-devolution-reserve-section/succession-be-devolution-reserve-section.component';
import { SuccessionBeAcceptationRenonciationSectionComponent } from '../succession-be-acceptation-renonciation-section/succession-be-acceptation-renonciation-section.component';
// F-217 SF-217-17 — section décisionnelle Vague 3 Famille BE — Reconnaissance mariage / divorce étranger (talaq inclus). Backend SF-217-16 bundle.
import { MariageEtrangerBeReconnaissanceSectionComponent } from '../mariage-etranger-be-reconnaissance-section/mariage-etranger-be-reconnaissance-section.component';
// F-217 SF-217-15 — section décisionnelle Vague 3 Famille BE — Protection du majeur (backend SF-217-14).
import { ProtectionMajeurBeSectionComponent } from '../protection-majeur-be-section/protection-majeur-be-section.component';
// F-217 SF-217-19 — section décisionnelle Vague 3 Famille BE — Contestation de filiation (CC art. 318 nouveau).
import { ContestationFiliationBeSectionComponent } from '../contestation-filiation-be-section/contestation-filiation-be-section.component';

export interface DecisionToolContext {
  caseFileId: string;
  synthesis: any | null;
  workspaceCountry: string;
  caseFileTitle: string;
  procedureChecks: any[];
  aiQuestions: any[];
  /**
   * F-197 SF-197-02 — Override avocat single-value du type de litige (Travail
   * FR) ou du type de procédure (Immigration). Lu via
   * {@link TypeLitigeOverrideService} dans le composant parent (`<app-synthesis>`
   * ou `<app-case-dashboard>`) puis transmis au panel via le signal SSE
   * `ENRICHED_ANALYSIS DONE` (cohérence F-176 stricte). Si présent, prend
   * précédence sur la valeur IA brute pour le pré-remplissage des outils
   * décisionnels.
   *
   * <p>Rappel : le PUT statut depuis le dialog override ne déclenche AUCUN
   * refresh côté frontend — la propagation outils se fait au prochain run de
   * Synthèse enrichie.</p>
   */
  typeLitigeOverride?: import('../../core/models/type-litige-override.model').TypeLitigeOverrideResponse | null;
  /**
   * F-192 SF-192-02 — Alignement IA des pistes 🟢 RETAINED (F-176) avec les
   * outils décisionnels. Lu via `RetainedPisteAlignmentService` au montage du
   * dossier ; rafraîchi à la réception de l'event SSE `ENRICHED_ANALYSIS DONE`
   * via `CaseDashboardRefreshService` (cohérence F-176 stricte).
   */
  pistesRetenues?: import('../../core/models/retained-piste-alignment.model').RetainedPisteAlignment[];
  /**
   * F-194 SF-194-02 — Alignement matérialisé pièces manquantes ↔ outils.
   * Pré-filtré côté entry par toolId pour ne passer aux composants outils
   * que la sous-liste des pièces qui les concernent. Pattern miroir
   * {@link RetainedPisteAlignment} (F-192) + {@link ProcedureCheckAlignment}
   * (F-193). Refresh exclusif au run de Synthèse enrichie (PUT statut pièce
   * ne déclenche AUCUN refresh côté frontend — cohérence F-176 stricte).
   */
  piecesAlignment?: import('../../core/models/piece-manquante-alignment.model').PieceManquanteAlignment[];
  /**
   * F-195 SF-195-02 — Alignement matérialisé risques ↔ outils. Pré-filtré
   * côté entry par toolId pour ne passer aux composants outils que la sous-
   * liste des risques qui les concernent. Pattern miroir
   * {@link PieceManquanteAlignment} (F-194). Refresh exclusif au run de
   * Synthèse enrichie (PUT statut risque ne déclenche AUCUN refresh côté
   * frontend — cohérence F-176 stricte).
   */
  risquesAlignment?: import('../../core/models/risque-alignment.model').RisqueAlignment[];
  /**
   * F-196 SF-196-02 / F-228 SF-228-01 — Alignement questions complémentaires
   * (F-94) ↔ outils. Disponible côté ctx pour permettre à un composant outil
   * d'inférer les pièces déduites des réponses de l'avocat (cohérence avec
   * `synthesis.piecesManquantesDetails` enrichies au prochain run de Synthèse
   * enrichie). Pattern miroir
   * {@link RetainedPisteAlignment} (F-192) +
   * {@link PieceManquanteAlignment} (F-194) +
   * {@link RisqueAlignment} (F-195).
   */
  aiQuestionsAlignment?: import('../../core/models/ai-question-alignment.model').AiQuestionAlignment[];
  /**
   * F-163 SF-163-02a — Mode simulateur autonome (hors dossier client).
   * Propagé par `SimulatorRunnerPageComponent` au composant décisionnel via
   * la closure `inputs(ctx)` du registre. Les composants qui ne le
   * consomment pas (~106 outils non encore refactorés) ignorent ce flag.
   * Quand `true` : le composant doit bypass `prefillFromAi()`,
   * `coherenceAlerts`, `triggerRefresh()` et POSTer sur le dispatcher
   * `/api/v1/simulators/{toolId}/calculate` au lieu de
   * `/api/v1/case-files/{id}/...`. Default `false` (mode case-file scoped).
   */
  standaloneMode?: boolean;
}

export interface DecisionToolRegistryEntry {
  component: Type<unknown>;
  inputs: (ctx: DecisionToolContext) => Record<string, unknown>;
  /**
   * SF-238-01 — libellé humain affiché dans le catalogue (chip cliquable).
   * Conventions :
   *   - FR : « Désunion irrémédiable (FR) », « Licenciement — Validité (FR) »
   *   - BE : « Désunion irrémédiable (Belgique) », « Préavis (Belgique) »
   *   - transversal (sans pays) : pas de suffixe
   * Le garde-fou CI `DecisionToolDisplayLabelIntegrityIT` (SF-238-01) interdit :
   *   1. tout `displayLabel` vide,
   *   2. tout `displayLabel` qui contient le `tool_id` (anti copier-coller).
   */
  displayLabel: string;
}

/**
 * F-169 SF-169-01 : thème métier auquel un outil décisionnel appartient.
 * Le panel affiche les outils groupés par thème (au lieu de la distinction
 * technique ALWAYS_ON / CONTEXTUAL) afin d'offrir une lecture par usage métier.
 */
export type ThemeKey = 'INDEMNITES' | 'VALIDITE' | 'DELAIS' | 'DOCUMENTS' | 'DIAGNOSTIC';

export interface ThemeDescriptor {
  key: ThemeKey;
  label: string;
}

@Component({
  selector: 'app-decisional-tools-panel',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatChipsModule,
    MatIconModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
    DecisionToolCardComponent,
    DecisionalToolsProgressBannerComponent,
  ],
  templateUrl: './decisional-tools-panel.component.html',
  styleUrls: ['./decisional-tools-panel.component.scss'],
})
export class DecisionToolsPanelComponent implements OnInit, OnChanges {
  private readonly caseFileService = inject(CaseFileService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  private readonly refreshService = inject(CaseDashboardRefreshService, { optional: true });
  // F-228 SF-228-01 : loader partagé qui charge les 4 alignements en parallèle
  // (forkJoin + fail-open par stream). Remplace les 3 services individuels
  // précédents (RetainedPisteAlignmentService / PieceManquanteAlignmentService /
  // RisqueAlignmentService) — code réutilisé par <app-case-dashboard>.
  private readonly alignmentsLoader = inject(DecisionToolAlignmentsLoader);
  private readonly modalService = inject(DecisionToolModalService);
  // SF-238-02 : activation manuelle d'un outil depuis le catalogue cliquable.
  private readonly manualActivationService = inject(DecisionToolManualActivationService);
  // SF-177-14 — propagé au modal pour que les outils héritent de l'injector
  // tree de case-file-detail (CaseDashboardRefreshService notamment).
  private readonly vcr = inject(ViewContainerRef);
  protected readonly progressService = inject(DecisionalToolsProgressService, { optional: true });

  @Input({ required: true }) caseFileId!: string;
  @Input() synthesis: any | null = null;
  @Input() workspaceCountry = 'FRANCE';
  @Input() caseFileTitle = '';
  @Input() procedureChecks: any[] = [];
  @Input() aiQuestions: any[] = [];
  // F-190 SF-190-03 — compteur "X/7 sections reçues" propagé au banner.
  @Input() streamingSectionsReceived: number | null = null;
  @Input() streamingSectionsExpected = 0;
  /**
   * F-197 SF-197-02 — Override avocat single-value du type de litige
   * (Travail FR) ou type de procédure (Immigration). Si présent, prend
   * précédence sur la valeur IA brute pour le pré-remplissage des outils
   * décisionnels (lecture via le helper {@link #augmentSynthesisWithOverride}).
   * Lu une fois au montage du dossier dans le composant parent ; aucun
   * refresh côté frontend après PUT (cohérence F-176 stricte).
   */
  @Input() typeLitigeOverride: import('../../core/models/type-litige-override.model').TypeLitigeOverrideResponse | null = null;

  /**
   * F-244 SF-244-02 — Total agrégé des champs pré-remplis par l'IA sur
   * l'ensemble des outils visibles (always-on ∪ contextual) = somme des
   * `getPrefillCount()` exposés par chaque composant outil.
   *
   * <p>Émis par le parent `case-file-detail` qui le porte en badge
   * `auto_awesome` sur l'onglet « Décision » du `mat-tab-group` : un onglet
   * fermé ne doit pas masquer le travail de l'IA (sous-règle anti-surcharge,
   * audit `screen-coherence-challenger` 2026-05-15, ajustement 5).</p>
   *
   * <p>Ré-émis à chaque chargement de la visibilité (`loadVisibility`), à
   * chaque émission `CaseDashboardRefreshService.refresh$` (fin de run
   * d'analyse) et à chaque changement de `synthesis` (les compteurs de
   * pré-fill dépendent de la synthèse IA).</p>
   */
  @Output() prefillTotalChange = new EventEmitter<number>();

  readonly loading = signal(false);
  readonly visibility = signal<VisibleToolSet | null>(null);

  /**
   * SF-238-02 — IDs des outils en cours d'activation manuelle. Chip → spinner +
   * disabled tant que le POST est en vol. Vidé à la réception de la réponse
   * (succès ou erreur).
   */
  readonly activatingToolIds = signal<ReadonlySet<string>>(new Set());

  /**
   * F-192 SF-192-02 — Alignement IA des pistes 🟢 RETAINED (F-176) avec les
   * outils décisionnels. Cache local. Refresh : (1) au mount du dossier,
   * (2) à chaque émission `CaseDashboardRefreshService.refresh$` (qui est lui-
   * même déclenché à la fin du run de Synthèse enrichie via les events SSE
   * F-185/F-190 — cohérence F-176 stricte).
   */
  readonly retainedPistes = signal<RetainedPisteAlignment[]>([]);

  /**
   * F-194 SF-194-02 — Alignement pièces manquantes ↔ outils. Cache local.
   * Refresh : (1) au mount du dossier, (2) à chaque émission
   * `CaseDashboardRefreshService.refresh$` (déclenchée à la fin du run de
   * Synthèse enrichie). Le PUT statut pièce ne déclenche PAS de refresh.
   */
  readonly piecesAlignment = signal<PieceManquanteAlignment[]>([]);

  /**
   * F-195 SF-195-02 — Alignement risques ↔ outils. Cache local.
   * Refresh : (1) au mount du dossier, (2) à chaque émission
   * `CaseDashboardRefreshService.refresh$` (déclenchée à la fin du run de
   * Synthèse enrichie). Le PUT statut risque ne déclenche PAS de refresh
   * (cohérence F-176 stricte — la matérialisation risque → outil ne se fait
   * qu'au prochain run de Synthèse enrichie).
   */
  readonly risquesAlignment = signal<RisqueAlignment[]>([]);

  /**
   * F-196 SF-196-02 / F-228 SF-228-01 — Alignement questions complémentaires
   * (F-94) ↔ outils. Cache local. Refresh aligné sur les 3 autres alignements
   * (mount + SSE `ENRICHED_ANALYSIS DONE` via
   * `CaseDashboardRefreshService.refresh$`). Pattern miroir.
   */
  readonly aiQuestionsAlignment = signal<AiQuestionAlignment[]>([]);

  /**
   * Registre des outils décisionnels. Chaque entrée déclare son composant
   * Angular et une closure qui mappe le contexte du dossier vers les inputs
   * exacts que ce composant attend. Les tool_id non présents ici sont
   * skippés avec un warning (forward-compat SF-IA-04-02).
   */
  static readonly TOOL_REGISTRY: ReadonlyMap<string, DecisionToolRegistryEntry> =
    new Map<string, DecisionToolRegistryEntry>([
      ['F-DT-04-fiche-prudhomale', {
        displayLabel: 'Fiche prud\'homale (FR)',
        component: PrudhomeFicheSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          caseFileTitle: ctx.caseFileTitle,
          // SF-173-01 : pré-fill IA + validation F-IA-03 (pattern canonique F-155).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-194 SF-194-02 : libellés des pièces statut OBTENUE alignées sur cet outil.
          piecesObtenues: piecesObtenuesFor(ctx.piecesAlignment, 'F-DT-04-fiche-prudhomale'),
        }),
      }],
      // SF-207-01b : prescription Travail BE — placée en tête de la séquence
      // Travail BE (transversal P1 ALWAYS_ON, cf. SF-207-00b-ux-coherence).
      // L'avocat doit voir d'abord ce délai critique avant de préparer la requête.
      ['prescription-be-litige-travail', {
        displayLabel: 'Prescription — litige travail (BE)',
        component: PrescriptionBeLitigeTravailSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // Pré-fill IA depuis 2 champs TravailExtractedData (Travail BE) :
          // `dateRuptureContrat` + `motifRupture` (mapping → typeCreance).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-207-02b : checklist C4 ONEM Travail BE (BE-only, ALWAYS_ON). Insertion
      // immédiate après `prescription-be-litige-travail` — séquence métier Travail
      // BE imposée par SF-207-00b-ux-coherence (prescription = délai critique
      // d'abord, conformité du C4 ensuite). 10 champs pré-remplis depuis
      // `TravailExtractedData` (extension SF-207-02 / SF-207-02b côté backend).
      ['c4-onem-checklist', {
        displayLabel: 'Checklist C4 ONEM (BE)',
        component: C4OnemChecklistSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-207-03b : contestation C4 ONEM Travail BE (BE-only, ALWAYS_ON).
      // Insertion immédiate après `c4-onem-checklist` — séquence métier :
      // vérifier la conformité du C4 d'abord, puis calculer les délais de
      // contestation (admin 1 mois + tribunal 3 mois). 3 champs pré-remplis
      // depuis `TravailExtractedData` (extension SF-207-03 backend).
      ['contestation-c4-onem', {
        displayLabel: 'Contestation C4 ONEM (BE)',
        component: ContestationC4OnemSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-207-04b : déclaration AT Fedris Travail BE (BE-only, ALWAYS_ON).
      // Délai 8 jours calendaires employeur (Loi 10/04/1971 art. 62). 2 modes
      // (prospectif / rétrospectif), 5 verdicts. 2 champs pré-remplis depuis
      // `TravailExtractedData` (dateAccident, dateConnaissanceAccidentEmployeur).
      ['at-fedris-declaration', {
        displayLabel: 'Déclaration AT Fedris (BE)',
        component: AtFedrisDeclarationSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-207-05b : référé tribunal du travail BE (BE-only, ALWAYS_ON).
      // CJ art. 584 — référé devant le président du tribunal du travail.
      // Verdict 3 états (ELIGIBLE / INCERTAIN / NON_ELIGIBLE) sur score 0-5
      // de 5 conditions cumulatives. 3 champs pré-remplis depuis
      // `TravailExtractedData` (motifUrgenceDetecte, dateFaitGenerateurUrgence,
      // perilImmediatPresume). Génération d'un squelette de requête copiable.
      ['refere-tribunal-travail-be', {
        displayLabel: 'Référé tribunal du travail (BE)',
        component: RefereTribunalTravailBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-207-06b : RCC BE — conditions d'éligibilité (BE-only, ALWAYS_ON).
      // CCT 17 / CCT 17/13 / AR 03/05/2007 — analyseur 4 régimes parallèles
      // (général, métiers lourds, longue carrière, entreprise en difficulté).
      // Verdict 3 états (ELIGIBLE / INCERTAIN / NON_ELIGIBLE) + régime applicable
      // (priorité) + régimes cumulés + calculs annexes (âge + années carrière).
      // 4 champs pré-remplis depuis `TravailExtractedData` (dateNaissanceSalarie,
      // anneesCarriereSalarie, metierLourdDetecte, entrepriseEnDifficulteDetectee).
      ['rcc-be-conditions', {
        displayLabel: 'RCC BE — conditions d\'éligibilité',
        component: RccBeConditionsSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-207-07b : RCC BE — indemnité complémentaire (BE-only, ALWAYS_ON).
      // CCT 17 art. 5 / Loi 03/07/1978 / AR 03/05/2007 — calculateur pur
      // (€/mois + total) sans verdict. Placé directement après l'analyseur
      // d'éligibilité rcc-be-conditions (séquence métier : conditions d'abord,
      // indemnité ensuite). 4 champs pré-remplis depuis `TravailExtractedData`
      // (remunerationNetteReferenceRccDetectee, allocationOnemMensuelleEstimee,
      // dateNaissanceSalarie réutilisé SF-207-06, dateDebutRccEnvisagee).
      ['rcc-be-indemnite-complementaire', {
        displayLabel: 'RCC BE — indemnité complémentaire',
        component: RccBeIndemniteComplementaireSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-207-08b : Outplacement BE obligatoire 45+ (BE-only, ALWAYS_ON).
      // CCT n°82 / CCT n°82 bis / Loi 05/09/2001 art. 13 / AR 30/05/2018 /
      // AR 25/11/1991 art. 154 — verdict 5 états (vert NON_DU /
      // OFFRE_CONFORME, rouge SANCTION_EMPLOYEUR 1 800 €, ambre
      // SANCTION_SALARIE 4-52 sem.). 5 champs pré-remplis depuis
      // `TravailExtractedData` (dateLicenciement, dateNaissanceSalarie réutilisé
      // SF-207-06, ancienneteSalarie + motifLicenciementDetecte +
      // offreOutplacementMentionnee livrés SF-207-08 backend).
      // DERNIÈRE SF de F-207 — 8/8 outils Travail BE livrés (back + front).
      ['outplacement-be-obligatoire-45', {
        displayLabel: 'Outplacement obligatoire 45+ (BE)',
        component: OutplacementBeObligatoire45SectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-DT-06-requete-tribunal-travail', {
        displayLabel: 'Requête tribunal du travail (BE)',
        component: TribunalTravailFicheSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          caseFileTitle: ctx.caseFileTitle,
          // SF-173-02 : pré-fill IA + validation F-IA-03 (pattern canonique F-155).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-194 SF-194-02 : libellés des pièces statut OBTENUE alignées sur cet outil.
          piecesObtenues: piecesObtenuesFor(ctx.piecesAlignment, 'F-DT-06-requete-tribunal-travail'),
        }),
      }],
      ['F-DT-07-anciennete-conges-prime', {
        displayLabel: 'Ancienneté, congés et primes (FR)',
        component: AncienneteSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          aiData: ctx.synthesis?.travailExtractedData,
        }),
      }],
      ['F-DT-08-licenciement-validity', {
        displayLabel: 'Licenciement — validité (FR)',
        component: LicenciementSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.licenciementValidityDetection,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02a — propage le flag standalone (default false) au
          // composant pilote refactoré ; les autres entrées du registre
          // l'ignorent tant que SF-163-02b/c/d ne les a pas refactorées.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-DT-09-comparateur-indemnites', {
        displayLabel: 'Comparateur d\'indemnités (FR)',
        component: IndemniteComparatifSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          synthesis: ctx.synthesis,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          // SF-155-15 : F-145 pièces manquantes pour enrichir l'alerte F-IA-03 sur TYPE_RUPTURE.
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-DT-10-rupture-conv-validity', {
        displayLabel: 'Rupture conventionnelle — validité (FR)',
        component: RuptureConvSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          aiData: ctx.synthesis?.ruptureConvValidityDetection,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-132-rupture-conv-indemnite', {
        displayLabel: 'Rupture conventionnelle — indemnité (FR)',
        component: RuptureConvIndemniteSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          synthesis: ctx.synthesis,
        }),
      }],
      ['F-DT-11-harcelement-licenciement-nul', {
        displayLabel: 'Harcèlement → licenciement nul (FR)',
        component: HarcelementLicenciementNulSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-155-04-A1 : branchement IA (pré-fill + validation F-IA-03).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-195 SF-195-02 : libellés des risques statut VALIDE alignés sur cet outil
          // (ex. "harcèlement moral subi" → flag visuel "risque validé" sur la card).
          risquesValides: risquesValidesFor(ctx.risquesAlignment, 'F-DT-11-harcelement-licenciement-nul'),
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-DT-16-licenciement-nul-detection', {
        displayLabel: 'Détection de licenciement nul (FR)',
        component: LicenciementNulDetectionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-16-02 : pré-fill IA (salaireBrutMensuel + dateLicenciement +
          // motifNullitePressenti → flag protection) + validation F-IA-03
          // (3 champs : SALAIRE, DATE_NOTIFICATION, PROTECTIONS).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-DT-36-procedure-nullite-licenciement', {
        displayLabel: 'Nullité de procédure de licenciement (FR)',
        component: ProcedureNulliteLicenciementSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-246-01 : pré-fill IA réel (8 champs procéduraux extraits dans
          // travailExtractedData.procedure_licenciement_detection, FR uniquement).
          // Validation F-IA-03 sur 3 champs croisables (DATE_ENTRETIEN,
          // MOTIVATION, ENTRETIEN_TENU) via F-96 / questions IA / pièces.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02b — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-DT-42-abandon-poste-presomption-demission', {
        displayLabel: 'Abandon de poste / présomption de démission (FR)',
        component: AbandonPostePresomptionDemissionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-206-02 : pré-fill IA réel (8 champs `abandon_poste_detail` projetés
          // à plat dans travailExtractedData, FR uniquement). Validation F-IA-03
          // sur 4 champs croisables (DATE_MISE_EN_DEMEURE, DELAI_ACCORDE,
          // MENTIONS_MED, MOTIF_LEGITIME) via F-96 / questions IA / pièces.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02b — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-DT-104-vrp-indemnite-clientele', {
        displayLabel: 'VRP : préavis et indemnité de clientèle (FR)',
        component: VrpIndemniteClienteleSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-218-12 : pré-fill IA réel (dateEntree, dateRupture, commissions
          // annuelles moyennes — FR uniquement). Validation F-IA-03 sur les 2
          // dates croisables (DATE_ENTREE, DATE_RUPTURE) via F-96 / questions IA.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          // F-163 SF-163-02b — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-DT-75-conges-payes-arret-maladie', {
        displayLabel: 'Congés payés acquis pendant arrêt maladie (FR)',
        component: CongesPayesArretMaladieSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-206-04 : pré-fill IA réel (5 champs `conges_payes_arret_maladie_detail`
          // projetés à plat dans travailExtractedData + `salaireBrutMensuel` déjà
          // extrait, FR uniquement). Validation F-IA-03 sur 3 champs croisables
          // (TYPE_ARRET, DUREE_ARRET, SALARIE_EN_POSTE) via F-96 / questions IA /
          // pièces.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02b — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-DT-39-prise-acte-rupture', {
        displayLabel: 'Prise d\'acte de la rupture (FR)',
        component: PriseActeRuptureSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-206-06 : pré-fill IA réel (11 champs `prise_acte_detail` projetés
          // à plat dans travailExtractedData, FR uniquement). Validation F-IA-03
          // sur 5 champs croisables (DT39_DEFAUT_PAIEMENT, DT39_HARCELEMENT,
          // DT39_MANQUEMENT_SECURITE, DT39_MODIFICATION_CONTRAT,
          // DT39_GRIEF_IMPOSSIBLE_POURSUITE) via F-96 / questions IA / pièces.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-02 : F-DT-36 licenciement pour faute grave / faute lourde (FR uniquement,
        // L. 1234-1 s. CT ; Cass. soc. 18/06/2013 n°11-14.393 — distinction strictement
        // française). Pré-fill IA réel sur 6 champs `fauteGrave*` (sous-objet
        // `faute_grave_detail`, projeté à plat dans travailExtractedData). Trigger
        // visibilité = `motif_faute_grave_pressenti` côté backend (seed migration 278).
      ['F-DT-36-licenciement-faute-grave-lourde', {
        displayLabel: 'Faute grave / faute lourde (FR)',
        component: LicenciementFauteGraveLourdSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-04 : F-DT-50 forfait jours — validité et rappel HS (FR uniquement,
      // L. 3121-58 à L. 3121-66 CT ; Cass. soc. 29/06/2011 n°09-71.107 — régime
      // strictement français). Pré-fill IA réel sur 5 champs `forfaitJours*`
      // (sous-objet `forfait_jours_detail`, projeté à plat dans
      // travailExtractedData). Trigger visibilité = `forfait_jours_detecte`
      // côté backend (seed migration 316, signal métier déjà extrait par F-205).
      ['F-DT-50-forfait-jours-validite', {
        displayLabel: 'Forfait jours — validité (FR)',
        component: ForfaitJoursFrSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-06 : F-DT-72 transfert d'entreprise — L. 1224-1 (FR uniquement,
      // L. 1224-1 CT ; L. 1224-3 CT ; Directive 2001/23/CE ; Cass. soc.
      // 18/07/2000 n°98-46.071 — entité économique autonome). Pré-fill IA
      // réel sur 5 champs `transfert*` (sous-objet `transfert_entreprise_detail`,
      // projeté à plat dans travailExtractedData). Trigger visibilité =
      // `transfert_entreprise_detecte` côté backend (seed migration 318,
      // flag IA livré par F-205).
      ['F-DT-72-transfert-entreprise-l1224-1', {
        displayLabel: 'Transfert d’entreprise — L. 1224-1 (FR)',
        component: TransfertEntrepriseFrSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-08 : F-DT-44 CSP/CRP — conformité de la proposition (FR).
      ['F-DT-44-csp-crp-conformite', {
        displayLabel: 'CSP/CRP — conformité de la proposition (FR)',
        component: CspCrpFrSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-10 : F-DT-91 faute inexcusable de l'employeur (FR).
      ['F-DT-91-faute-inexcusable-employeur', {
        displayLabel: 'Faute inexcusable de l’employeur (FR)',
        component: FauteInexcusableFrSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-26 : F-DT-61 protection du lanceur d'alerte (FR).
      ['F-DT-61-lanceur-alerte-protection', {
        displayLabel: 'Protection du lanceur d’alerte (FR)',
        component: LanceurAlerteProtectionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-12 : F-DT-70 modification du contrat — refus du salarié (FR).
      ['F-DT-70-modification-contrat-refus', {
        displayLabel: 'Modification du contrat — refus du salarié (FR)',
        component: ModificationContratRefusSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-14 : F-DT-71 mutation — validité de la clause de mobilité (FR).
      ['F-DT-71-mutation-clause-mobilite', {
        displayLabel: 'Mutation — validité de la clause de mobilité (FR)',
        component: MutationClauseMobiliteSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-16 : F-DT-82 télétravail — conformité et litige (FR).
      ['F-DT-82-teletravail-accord', {
        displayLabel: 'Télétravail — conformité et litige (FR)',
        component: TeletravailAccordSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-20 : F-DT-48 mise à pied disciplinaire — régularité (FR).
      ['F-DT-48-mise-a-pied-disciplinaire', {
        displayLabel: 'Mise à pied disciplinaire — régularité (FR)',
        component: MiseAPiedDisciplinaireSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-24 : F-DT-56 égalité salariale femmes/hommes (FR).
      ['F-DT-56-egalite-salariale-femmes-hommes', {
        displayLabel: 'Égalité salariale femmes/hommes (FR)',
        component: EgaliteSalarialeFhSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-36 : F-DT-46 PDV / RCC — conformité (FR).
      ['F-DT-46-pdv-rcc-conformite', {
        displayLabel: 'PDV / RCC — conformité (FR)',
        component: PdvRccConformiteSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-30 : F-DT-77 congé maternité / paternité — protection & indemnités (FR).
      ['F-DT-77-conge-paternite-maternite', {
        displayLabel: 'Congé maternité / paternité — protection & indemnités (FR)',
        component: CongeMaternitePaterniteSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-28 : F-DT-64 burn-out — reconnaissance maladie professionnelle hors tableau (FR).
      ['F-DT-64-burnout-reconnaissance-mp', {
        displayLabel: 'Burn-out — reconnaissance MP (FR)',
        component: BurnoutReconnaissanceMpSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-32 : F-DT-65 élections CSE — conformité procédure (FR).
      ['F-DT-65-elections-cse-conformite', {
        displayLabel: 'Élections CSE — conformité procédure (FR)',
        component: ElectionsCseConformiteSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-34 : F-DT-49 temps partiel — requalification en temps plein (FR).
      ['F-DT-49-temps-partiel-requalification', {
        displayLabel: 'Temps partiel — requalification (FR)',
        component: TempsPartielRequalificationSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-38 : F-DT-84 conciliation CPH BCA — préparation phase BCO (FR). F-212 19/19.
      ['F-DT-84-conciliation-cph-bca', {
        displayLabel: 'Conciliation CPH — BCO (FR)',
        component: ConciliationCphBcaSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-18 : F-DT-43 rupture anticipée du CDD (FR).
      ['F-DT-43-rupture-anticipee-cdd', {
        displayLabel: 'Rupture anticipée du CDD (FR)',
        component: RuptureAnticipeeCddSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-212-22 : F-DT-41 démission validité équivoque (FR).
      ['F-DT-41-demission-validite-equivoque', {
        displayLabel: 'Démission — validité et caractère équivoque (FR)',
        component: DemissionEquivoqueSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-DT-40-resiliation-judiciaire-cph', {
        displayLabel: 'Résiliation judiciaire du contrat (FR)',
        component: ResiliationJudiciaireCphSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-206-08 : pré-fill IA réel (12 champs `resiliation_judiciaire_detail`
          // projetés à plat dans travailExtractedData, FR uniquement). Validation
          // F-IA-03 sur 5 champs croisables (DT40_DEFAUT_PAIEMENT, DT40_HARCELEMENT,
          // DT40_MANQUEMENT_SECURITE, DT40_MODIFICATION_CONTRAT,
          // DT40_MANQUEMENTS_PERSISTANTS) via F-96 / questions IA / pièces.
          // Outil jumeau de F-DT-39 (prise d'acte) — situation distincte : voie
          // sans risque de rupture, salarié reste en poste pendant l'instance.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-DT-38-rupture-periode-essai', {
        displayLabel: 'Rupture de période d\'essai (FR)',
        component: RupturePeriodeEssaiSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-38-02 : pré-fill IA basé sur les champs déjà extraits par
          // le pipeline (9 champs — typeContrat, dateEntree, dateLicenciement,
          // motifLicenciement, motifNullitePressenti, atMpDetecte,
          // conventionCollective, salaireBrutMensuel). Pré-fill exhaustif F-246
          // (sous-objet rupture_periode_essai_detail) différé à une SF dédiée.
          aiData: ctx.synthesis?.travailExtractedData,
          // F-163 SF-163-02b — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-DT-12-discrimination-dommages-interets', {
        displayLabel: 'Discrimination — dommages-intérêts (FR)',
        component: DiscriminationSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-12-02 : pré-fill IA palier 1 (salaire uniquement) +
          // validation F-IA-03.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-195 SF-195-02 : libellés des risques statut VALIDE alignés
          // (ex. "discrimination" → flag visuel "risque validé").
          risquesValides: risquesValidesFor(ctx.risquesAlignment, 'F-DT-12-discrimination-dommages-interets'),
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-DT-13-licenciement-economique', {
        displayLabel: 'Licenciement économique (FR)',
        component: LicenciementEconomiqueSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-13-02 : pré-fill IA (motifLicenciement → motifEconomique,
          // dateLicenciement → dateNotification) + validation F-IA-03 sur
          // MOTIF_ECONOMIQUE + DATE_NOTIFICATION (multi-sources IA / F96 /
          // QUESTION_IA / PIECE_MANQUANTE).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-DT-15-inaptitude', {
        displayLabel: 'Inaptitude — indemnités (FR)',
        component: InaptitudeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-155-04-A2 : sources IA (pré-fill + validation F-IA-03)
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-DT-19-heures-sup', {
        displayLabel: 'Heures supplémentaires (FR)',
        component: HeuresSupSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-155-04-A3 : pré-fill IA + validation F-IA-03.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-DT-17-indemnite-precarite-cdd', {
        displayLabel: 'Indemnité de précarité CDD (FR)',
        component: IndemnitePrecariteCddSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-17-02 : pré-fill IA salaire mensuel + alertes F-IA-03.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-DT-26-conges-payes-indemnite', {
        displayLabel: 'Indemnité congés payés (FR)',
        component: CongesPayesSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-26-02 : pré-fill IA (salaireBrutMensuel + dateLicenciement)
          // + validation F-IA-03 (2 champs : SALAIRE_MENSUEL + DATE_RUPTURE).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-DT-18-fin-mission-interim', {
        displayLabel: 'Fin de mission intérim (FR)',
        component: FinMissionInterimSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-18-02 : pré-fill IA salaire mensuel + alertes F-IA-03.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-DT-32-documents-fin-contrat', {
        displayLabel: 'Documents de fin de contrat (FR)',
        component: DocumentsFinContratSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-32-02 : pré-fill IA (salaireBrutMensuel + dateLicenciement)
          // + validation F-IA-03 (2 alertes : SALAIRE + DATE_FIN_CONTRAT).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-DT-34-refere-prudhomal', {
        displayLabel: 'Référé prud\'homal (FR)',
        component: ReferePrudhomalSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-34-02 : pré-fill IA (dateLicenciement → dateMiseEnDemeure,
          // dateEntree → ancienneteContratMois, heuresSupMentionneesDansDossier
          // → natureCreance=HEURES_SUPPLEMENTAIRES) + validation F-IA-03
          // (2 alertes : DATE_MISE_EN_DEMEURE + ANCIENNETE).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-DT-21-travail-dissimule', {
        displayLabel: 'Travail dissimulé (FR)',
        component: TravailDissimuleSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-21-02 : pré-fill IA (salaireBrutMensuel) + validation F-IA-03.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-DT-25-indemnite-preavis', {
        displayLabel: 'Indemnité de préavis (FR)',
        component: IndemnitePreavisSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-25-02 : pré-fill IA (salaireBrutMensuel + dateLicenciement +
          // conventionCollective) + validation F-IA-03 (3 alertes : SALAIRE,
          // DATE_RUPTURE, CONVENTION).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-DT-20-rappel-salaire', {
        displayLabel: 'Rappel de salaire (FR)',
        component: RappelSalaireSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-20-02 : pré-fill IA (salaireBrutMensuel + conventionCollective)
          // + validation F-IA-03 (2 alertes : SALAIRE + CONVENTION).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-DT-22-requalification-cdd-cdi', {
        displayLabel: 'Requalification CDD en CDI (FR)',
        component: RequalificationCddCdiSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-22-02 : pré-fill IA (salaireBrutMensuel) + validation F-IA-03
          // (1 alerte : SALAIRE_MENSUEL multi-sources IA / F96 / QUESTION_IA /
          // PIECE_MANQUANTE).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-DT-23-requalification-interim-cdi', {
        displayLabel: 'Requalification intérim en CDI (FR)',
        component: RequalificationInterimCdiSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-23-02 : pré-fill IA (salaireBrutMensuel) + validation F-IA-03
          // (1 alerte : SALAIRE_MENSUEL multi-sources IA / F96 / QUESTION_IA /
          // PIECE_MANQUANTE). Jumeau direct F-DT-22 — adapté MissionInterim
          // (entrepriseUtilisatrice + memeEntrepriseUtilisatrice).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-DT-24-non-concurrence', {
        displayLabel: 'Clause de non-concurrence (FR)',
        component: NonConcurrenceSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-24-02 : pré-fill IA (salaireBrutMensuel) + validation F-IA-03
          // (1 alerte : SALAIRE_MENSUEL multi-sources IA / F96 / QUESTION_IA /
          // PIECE_MANQUANTE). Outil scoring 4 critères Cass. soc. 10/07/2002.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-195 SF-195-02 : libellés des risques statut VALIDE alignés
          // (ex. "clause non-concurrence abusive" → flag visuel ; ECARTE
          // → masquage potentiel via F-IA-04).
          risquesValides: risquesValidesFor(ctx.risquesAlignment, 'F-DT-24-non-concurrence'),
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-132-rupture-amiable-info', {
        displayLabel: 'Rupture amiable — informations',
        component: RuptureAmiableInfoSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
        }),
      }],
      // F-208 — 4 wrappers informationnels P1 Immigration FR (délais courts).
      // Backend complet (calculator/analyzer + endpoint POST/GET + table + migration)
      // livré dans la PR #915. Le composant frontend de saisie/visualisation complet
      // sera livré dans une SF ultérieure de F-208 ; ce wrapper rappelle uniquement
      // le cadre juridique pour ne pas laisser le tool_id orphelin (cf. règle
      // SF-164-01 / DecisionToolVisibilityIntegrityIT).
      // SF-208-05 : composant complet F-IM-21 (formulaire + verdict + pre-fill IA + F-IA-03).
      ['F-IM-21-jld-retention-fr', {
        displayLabel: 'JLD rétention administrative (FR)',
        component: JldRetentionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-208-06 : composant complet F-IM-22.
      ['F-IM-22-dublin-recours-fr', {
        displayLabel: 'Dublin — recours (FR)',
        component: DublinRecoursSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-208-07 : composant complet F-IM-23.
      ['F-IM-23-crrv-refus-visa-fr', {
        displayLabel: 'CRRV — refus de visa (FR)',
        component: CrrvRefusVisaSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-208-08 : composant complet F-IM-24 (analyzer scoring 3 verdicts).
      ['F-IM-24-victime-violences-l4256-fr', {
        displayLabel: 'Victime de violences — L.425-6 (FR)',
        component: VictimeViolencesL4256SectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-02 : composant complet F-IM-25 étranger malade L.425-9 CESEDA (FR).
      // Pré-fill IA via static getPrefillCount + EtrangerMaladePrefillRules.
      ['F-IM-25-etranger-malade-l4259-fr', {
        displayLabel: 'Étranger malade — L.425-9 (FR)',
        component: EtrangerMaladeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-04 : composant complet F-IM-26 regroupement familial L.434-1+ CESEDA (FR).
      // FR uniquement. Pré-fill IA 3 champs (durée séjour, ressources, type)
      // via static getPrefillCount + RegroupementFamilialPrefillRules.
      ['F-IM-26-regroupement-familial-fr', {
        displayLabel: 'Regroupement familial (FR)',
        component: RegroupementFamilialSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-06 : composant complet F-IM-27-vpf-liens-personnels-l42323-fr — VPF
      // liens personnels L.423-23 CESEDA (FR). FR uniquement. Scoring 0-100 +
      // 4 verdicts (ELIGIBLE_PROBABLE / ELIGIBLE_SOUS_RESERVE / NON_ELIGIBLE /
      // DOSSIER_A_CONSOLIDER). Pré-fill IA 4 champs (durée résidence, minorité à
      // l'entrée, enfants en France, niveau d'intégration) via static getPrefillCount
      // + VpfLiensPersonnelsPrefillRules.
      ['F-IM-27-vpf-liens-personnels-l42323-fr', {
        displayLabel: 'VPF liens personnels L.423-23 (FR)',
        component: VpfLiensPersonnelsSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-08 : composant complet F-IM-28-vls-ts-validation-ofii-fr — validation
      // VLS-TS OFII (FR). FR uniquement, ALWAYS_ON. Calculateur de délai 3 mois
      // (CESEDA R.431-16+) : statut A_VALIDER/URGENT/EXPIRE/VALIDE + échéance +
      // jours restants. Pré-fill IA 1 champ (date d'entrée en France) via static
      // getPrefillCount + VlsTsValidationPrefillRules. Bridge échéance F-69.
      ['F-IM-28-vls-ts-validation-ofii-fr', {
        displayLabel: 'Validation VLS-TS OFII (FR)',
        component: VlsTsValidationSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-30 : composant complet F-IM-39-naturalisation-recours-tj-fr —
      // recours TJ naturalisation (FR). FR uniquement, ALWAYS_ON. Calculateur de
      // délai 6 mois (C. civ. art. 26-3, 26-4) : statut RECOURS_POSSIBLE/URGENT/
      // PRESCRIT + échéance recours judiciaire + jours restants + tribunal
      // compétent + motifs de recours + bases juridiques. Pré-fill IA 2 champs
      // (voieNaturalisation, dateRefusDeclaration) via static getPrefillCount +
      // NaturalisationRecoursTjPrefillRules. Bridge échéance F-69.
      ['F-IM-39-naturalisation-recours-tj-fr', {
        displayLabel: 'Recours TJ naturalisation (FR)',
        component: NaturalisationRecoursTjSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-32 : composant complet F-IM-40-naturalisation-recours-ta-fr —
      // recours TA Nantes naturalisation (FR). FR uniquement, ALWAYS_ON.
      // Calculateur de délai 2 mois (recours pour excès de pouvoir contre un
      // refus de décret de naturalisation, compétence exclusive TA de Nantes) :
      // statut RECOURS_POSSIBLE/URGENT/PRESCRIT + échéance recours TA + jours
      // restants + tribunal compétent (TA Nantes) + motifs de recours + bases
      // juridiques. Pré-fill IA 1 champ (dateRefusDecret réutilise
      // naturalisationDateRefus) via static getPrefillCount +
      // NaturalisationRecoursTaPrefillRules. Bridge échéance F-69.
      ['F-IM-40-naturalisation-recours-ta-fr', {
        displayLabel: 'Recours TA Nantes naturalisation (FR)',
        component: NaturalisationRecoursTaSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-34 : composant complet F-IM-41-appel-caa-cassation-ce-fr — appel
      // CAA / cassation CE (FR). FR uniquement, ALWAYS_ON. Calculateur de délai
      // d'appel devant la cour administrative d'appel contre un jugement de TA en
      // contentieux des étrangers (délai 1 mois, ou 15 j en OQTF) : statut
      // APPEL_POSSIBLE/URGENT/PRESCRIT + échéance appel CAA + jours restants +
      // CAA compétente + motifs d'appel possibles + filtre des pourvois en
      // cassation (CE, L.821-2 CJA pour l'OQTF) + délai du pourvoi en cassation.
      // Pré-fill IA 1 champ (dateJugementTA depuis recoursDateJugementTA) via
      // static getPrefillCount + AppelCaaCassationPrefillRules. Bridge échéance
      // F-69 (label « Appel CAA contentieux étrangers »).
      ['F-IM-41-appel-caa-cassation-ce-fr', {
        displayLabel: 'Appel CAA / cassation CE (FR)',
        component: AppelCaaCassationSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-36 : composant complet F-IM-42-assignation-residence-fr —
      // assignation à résidence (FR). FR uniquement, ALWAYS_ON. Analyseur de
      // validité / délais (CESEDA) : statut EN_COURS/EXPIRATION_PROCHE/EXPIRE +
      // échéance assignation + durée totale autorisée + renouvellement possible +
      // motifs de contestation + recours TA possible (délai 48 h). Pré-fill IA 1
      // champ (dateNotificationAssignation depuis assignationDateNotification) via
      // static getPrefillCount + AssignationResidencePrefillRules. Bridge échéance
      // F-69 (label « Échéance assignation à résidence », statut EN_COURS).
      ['F-IM-42-assignation-residence-fr', {
        displayLabel: 'Assignation à résidence (FR)',
        component: AssignationResidenceSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-38 : composant complet F-IM-43-itf-judiciaire-fr — ITF judiciaire
      // (FR). FR uniquement, ALWAYS_ON. Analyseur de validité / délais (Code
      // pénal, art. 131-30 et s.) : statut APPEL_POSSIBLE / RELEVE_POSSIBLE /
      // EN_COURS_PURGE / RECOURS_PRESCRIT + échéance du relevé + voies de recours
      // (délais) + conditions du relevé + encadré bleu distinction ITF (judiciaire)
      // vs IRTF (administrative). Pré-fill IA 2 champs (dateCondamnation depuis
      // itfJudiciaireDateCondamnation, dureeITFAnnees depuis itfJudiciaireDureeAnnees)
      // via static getPrefillCount + ItfJudiciairePrefillRules. Bridge échéance
      // F-69 (label « Recours pénal ITF », statut APPEL_POSSIBLE).
      ['F-IM-43-itf-judiciaire-fr', {
        displayLabel: 'ITF judiciaire (FR)',
        component: ItfJudiciaireSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-40 : composant complet F-IM-44-ue-eee-suisse-sejour-fr — séjour
      // UE/EEE/Suisse (FR). FR uniquement, ALWAYS_ON. Analyseur de droits
      // (directive 2004/38/CE, art. L. 233-1 et s. du CESEDA) : droit au séjour
      // automatique de 3 mois + droit au séjour permanent au-delà de 5 ans + titre
      // obtenu (attestation d'enregistrement / carte de séjour membre de famille)
      // + conditions respectées + encadré situation du membre de famille non-UE.
      // Pré-fill IA 3 champs (nationalite depuis nationalite, estCitoyenUE depuis
      // nationaliteUe, dureeSejourMois depuis aesDureePresenceMois) via static
      // getPrefillCount + UeEeeSuisseSejourPrefillRules. Pas de bridge échéance.
      ['F-IM-44-ue-eee-suisse-sejour-fr', {
        displayLabel: 'Séjour UE/EEE/Suisse (FR)',
        component: UeEeeSuisseSejourSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-42 : composant complet F-IM-45-retrait-titre-fraude-fr — retrait
      // de titre pour fraude (FR). FR uniquement, ALWAYS_ON. Analyseur de validité
      // (CESEDA) : statut du recours RECOURS_POSSIBLE/URGENT/PRESCRIT + vices de
      // procédure (encadré orange si non vide) + motifs de contestation + délai du
      // recours devant le TA + base juridique. Pré-fill IA 2 champs (dateRetrait
      // depuis retraitTitreDateRetrait, motifRetrait depuis retraitTitreMotif) via
      // static getPrefillCount + RetraitTitreFraudePrefillRules. Bridge échéance
      // F-69 (label « Recours TA retrait titre », statut RECOURS_POSSIBLE / URGENT).
      ['F-IM-45-retrait-titre-fraude-fr', {
        displayLabel: 'Retrait titre pour fraude (FR)',
        component: RetraitTitreFraudeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-44 : composant complet F-IM-46-autorisation-travail-employeur-fr —
      // autorisation de travail employeur (FR). FR uniquement, ALWAYS_ON. Outil côté
      // employeur, complémentaire à F-IM-07 (côté salarié) : statut AUTORISATION_REQUISE
      // (bleu) / AUTORISATION_NON_REQUISE (vert, dispense UE/EEE/Suisse) / RECOURS_POSSIBLE
      // (orange, refus contestable) / RECOURS_PRESCRIT (rouge) + checklist obligations de la
      // demande (procédure OFII) + délai d'instruction OFII + taxe OFII + délai du recours TA
      // si refus. Pré-fill IA 1 champ (nationaliteCandidat depuis nationalite) via static
      // getPrefillCount + AutorisationTravailEmployeurPrefillRules. Bridge échéance F-69
      // (label « Recours TA autorisation travail », statut RECOURS_POSSIBLE).
      ['F-IM-46-autorisation-travail-employeur-fr', {
        displayLabel: 'Autorisation travail employeur (FR)',
        component: AutorisationTravailEmployeurSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-14 : composant complet F-IM-31-renouvellement-delai-depot-fr —
      // renouvellement délai dépôt (FR). FR uniquement, ALWAYS_ON. Calculateur de
      // délai (CESEDA) : statut EN_AVANCE/A_DEPOSER/A_DEPOSER_URGENT/EXPIRE/DEPOSE
      // + date optimale + date impérative + jours restants + risqueIrruption/
      // alerteRetard. Pré-fill IA 2 champs (dateExpirationTitre, typeTitre depuis
      // typeTitreSejour) via static getPrefillCount + RenouvellementDelaiPrefillRules.
      // Bridge échéance F-69 (label « Dépôt renouvellement titre »).
      ['F-IM-31-renouvellement-delai-depot-fr', {
        displayLabel: 'Renouvellement délai dépôt (FR)',
        component: RenouvellementDelaiSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-10 : composant complet F-IM-29-oqtf-categories-l6111-fr — OQTF
      // catégories L.611-1 (FR). FR uniquement. Pour la catégorie L.611-1 (1° à 7°)
      // choisie : moyens de défense spécifiques + base juridique + délai de recours
      // + renvoi F-IM-22 (Dublin) si CAT_7. Pré-fill IA 2 champs (dateNotificationOqtf,
      // motifOqtf depuis motifOqtfCode) via static getPrefillCount + OqtfCategoriesPrefillRules.
      ['F-IM-29-oqtf-categories-l6111-fr', {
        displayLabel: 'OQTF catégories L.611-1 (FR)',
        component: OqtfCategoriesSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-16 : composant complet F-IM-32-recepisse-attestation-fr — récépissé
      // vs attestation (FR). FR uniquement. Distingue les droits attachés au
      // récépissé de demande de titre de ceux de l'attestation de prolongation
      // d'instruction (droit au séjour / droit au travail / durée de validité) et
      // alerte sur le risque employeur (sanctions L. 8253-1) si attestation.
      // Pré-fill IA 2 champs (dateExpiration depuis dateExpirationTitre,
      // typeDocument depuis recepisseOuAttestationType) via static getPrefillCount
      // + RecepisseAttestationPrefillRules.
      ['F-IM-32-recepisse-attestation-fr', {
        displayLabel: 'Récépissé vs attestation (FR)',
        component: RecepisseAttestationSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-18 : composant complet F-IM-33-ofpra-introduction-fr — OFPRA
      // introduction (FR). FR uniquement. Calcule la date d'échéance d'introduction
      // de la demande d'asile auprès de l'OFPRA (après passage GUDA), affiche un
      // stepper de 5 étapes de procédure + les pièces requises et alerte sur le
      // risque de procédure accélérée. Pré-fill IA 1 champ (dateArriveeEnFrance
      // depuis aesDateEntreeFrance) via static getPrefillCount + OfpraIntroductionPrefillRules.
      ['F-IM-33-ofpra-introduction-fr', {
        displayLabel: 'OFPRA introduction (FR)',
        component: OfpraIntroductionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-26 : composant complet F-IM-37-anef-procedure-fr — ANEF procédure /
      // pannes (FR). FR uniquement. Guide pas-à-pas de la dématérialisation ANEF :
      // détecte une panne du téléservice et le risque d'expiration du titre, affiche
      // soit les étapes standard, soit les étapes alternatives de recours (dépôt
      // papier préfecture, référé) en cas de panne, et donne le délai de recours pour
      // faute. Pré-fill IA 2 champs (dateExpirationTitre + typeTitreConcerne depuis
      // typeTitreSejour) via static getPrefillCount + AnefProcedurePrefillRules.
      ['F-IM-37-anef-procedure-fr', {
        displayLabel: 'ANEF procédure / pannes (FR)',
        component: AnefProcedureSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-20 : composant complet F-IM-34-aj-cnda-fr — aide juridictionnelle
      // CNDA (FR). FR uniquement. Vérifie l'éligibilité ressources, calcule
      // l'échéance du recours CNDA (1 mois, réduit en procédure accélérée) et
      // l'échéance de dépôt de la demande d'AJ + liste les pièces. Statut
      // AJ_A_DEMANDER/AJ_DEPOSEE/HORS_DELAI_AJ/NON_ELIGIBLE_RESSOURCES. Pré-fill
      // IA 1 champ (dateDecisionOFPRA depuis asileDateDecisionAnterieure) via
      // static getPrefillCount + AjCndaPrefillRules. Bridge échéance F-69
      // (« Demande AJ CNDA » si statut AJ_A_DEMANDER).
      ['F-IM-34-aj-cnda-fr', {
        displayLabel: 'AJ CNDA (FR)',
        component: AjCndaSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-28 : composant complet F-IM-38-mna-evaluation-age-fr — MNA
      // évaluation de l'âge / recours juge des enfants (FR). FR uniquement
      // (l'évaluation de la minorité par l'ASE, la contestation des examens
      // osseux art. 388 c. civ. et le recours JE relèvent du droit français).
      // Détermine le statut de la situation, calcule l'échéance de saisine du
      // JE (bridge échéance F-69 si RECOURS_JE_URGENT, label « Saisine juge des
      // enfants MNA »), expose le stepper procédure ASE, les arguments de
      // contestation de l'examen osseux et les droits attachés. Pré-fill IA
      // 1 champ (dateNaissanceDeclaree depuis mineursDateNaissance) via static
      // getPrefillCount + MnaEvaluationAgePrefillRules.
      ['F-IM-38-mna-evaluation-age-fr', {
        displayLabel: 'MNA évaluation âge (FR)',
        component: MnaEvaluationAgeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-22 : composant complet F-IM-35-victime-traite-l4251-fr — protection
      // victime de traite des êtres humains L. 425-1 (FR). FR uniquement (régime
      // distinct en BE). Évalue l'éligibilité à la carte de séjour « vie privée
      // et familiale » L. 425-1 pour la victime ayant porté plainte / témoigné,
      // liste les mesures de protection et alerte par une bannière ROUGE sur le
      // risque immédiat pour la sécurité (risqueVictimeEnDanger). Pré-fill IA
      // 2 champs (plainteDeposee depuis tehPlainteDeposee, datePlainte depuis
      // tehDatePlainte) via static getPrefillCount + VictimeTraitePrefillRules.
      ['F-IM-35-victime-traite-l4251-fr', {
        displayLabel: 'Victime traite L.425-1 (FR)',
        component: VictimeTraiteSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-24 : composant complet F-IM-36-carte-resident-l4261-fr — carte de
      // résident de dix ans de l'article L. 426-1 du CESEDA (FR). FR uniquement
      // (régime distinct en BE). Évalue l'éligibilité (durée de séjour régulier,
      // intégration républicaine, ressources, condamnations pénales graves),
      // affiche la checklist des critères non remplis (chipsCriteresNonRemplis)
      // et la liste des atouts du dossier. Pré-fill IA 2 champs
      // (dureeSejourRegulierAnnees depuis aesDureePresenceMois ÷ 12,
      // ressourcesMensuellesNettes depuis carteResidentRessources) via static
      // getPrefillCount + CarteResidentPrefillRules.
      ['F-IM-36-carte-resident-l4261-fr', {
        displayLabel: 'Carte de résident L.426-1 (FR)',
        component: CarteResidentSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-214-12 : composant complet F-IM-30-aes-presence-prouvee-fr — AES présence
      // prouvée (FR). FR uniquement, CONTEXTUAL via flag aesCalculPresenceDeclenche.
      // Saisie dynamique de périodes {debut, fin, typePiece} → total d'années
      // prouvées + 4 voies AES (famille/humanitaire/étudiant/métiers tension) +
      // gaps + recommandations de pièces. Pré-fill IA 1 ligne (aesDateEntreeFrance
      // → période initiale) via static getPrefillCount + AesPresenceProuveePrefillRules.
      ['F-IM-30-aes-presence-prouvee-fr', {
        displayLabel: 'AES présence prouvée (FR)',
        component: AesPresenceProuveeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-215-02 : composant complet F-IM-25-single-permit-be permis unique BE
      // (travail+séjour). BELGIQUE uniquement, CONTEXTUAL via flag
      // `single_permit_envisage`. Pré-fill IA 5 champs (dates début/fin, région
      // FOREM/VDAB/ACTIRIS, type activité, motif NOUVEAU/RENOUVELLEMENT) via
      // static getPrefillCount + SinglePermitBePrefillRules.
      ['F-IM-25-single-permit-be', {
        displayLabel: 'Permis unique BE (travail+séjour)',
        component: SinglePermitBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-215-04 : composant complet F-IM-26-regroupement-10ter-be — regroupement
      // familial art. 10ter (BE). BELGIQUE uniquement, CONTEXTUAL via flag
      // `regroupement_10ter_detecte`. Pré-fill IA 4 champs (lienFamilial,
      // typeCarteRegroupant, revenusMensuelsNets, dureeSejour) via static
      // getPrefillCount + Regroupement10terBePrefillRules. Scoring 0-100 +
      // 3 verdicts (ELIGIBLE/SOUS_RESERVE/INELIGIBLE) + différentiel signé.
      ['F-IM-26-regroupement-10ter-be', {
        displayLabel: 'Regroupement familial 10ter (BE)',
        component: Regroupement10terBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-215-14 : composant complet F-IM-31-cce-annulation-30j-be — recours en
      // ANNULATION devant le CCE (Conseil du Contentieux des Étrangers). BELGIQUE
      // uniquement, CONTEXTUAL via flag `recours_cce_envisage`. Calculateur de
      // délai (30 jours calendaires, Loi 15/12/1980 art. 39/2 §2 et 39/57 §1er).
      // Pré-fill IA RÉEL 2 champs (dateNotificationDecision, typeDecision) via
      // static getPrefillCount + CceAnnulationBePrefillRules. Les 2 champs
      // recoursForme (checkbox) + dateRecours (date conditionnelle) sont
      // aspirationnels — `PREFILL_COUNT_ALWAYS_ZERO`. Badge statut 4 états
      // (DISPONIBLE vert / URGENT orange / EXPIRE rouge / RECOURS_FORME bleu) +
      // lien croisé vers F-IM-32 (extrême urgence) si URGENT/EXPIRE.
      ['F-IM-31-cce-annulation-30j-be', {
        displayLabel: 'Recours CCE annulation 30j (BE)',
        component: CceAnnulationBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-215-16 : composant complet F-IM-32-cce-extreme-urgence-5j-be — recours en
      // EXTRÊME URGENCE devant le CCE (Conseil du Contentieux des Étrangers). BELGIQUE
      // uniquement, CONTEXTUAL via flag `recours_cce_extreme_urgence`. Calculateur de
      // délai (5 jours OUVRABLES, Loi 15/12/1980 art. 39/82). Cas d'urgence absolue —
      // bandeau rouge proéminent + actionImmediate si CRITIQUE/EXPIRE. Pré-fill IA RÉEL
      // 2 champs (dateActeExecutoire, typeActe) via static getPrefillCount +
      // CceExtremeUrgenceBePrefillRules. Les 2 champs recoursForme (checkbox) +
      // dateRecours (date conditionnelle) sont aspirationnels (jamais comptés).
      ['F-IM-32-cce-extreme-urgence-5j-be', {
        displayLabel: 'Recours CCE extrême urgence 5j (BE)',
        component: CceExtremeUrgenceBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-215-18 : composant complet F-IM-33-annexe13quinquies-ie-be — annexe
      // 13quinquies = OQT assorti d'une interdiction d'entrée (IE) Schengen.
      // BELGIQUE uniquement, CONTEXTUAL via flag `interdiction_entree_be_detectee`.
      // Calculateur (Loi 15/12/1980 art. 74/11) : durée IE 3/5/8 ans (badge coloré),
      // date de fin, date de levée précoce possible, délai du recours en annulation
      // CCE (art. 39/2 §2 — 30j calendaires) avec statut DISPONIBLE/URGENT/EXPIRE/
      // FORME + lien croisé vers F-IM-31 si URGENT. Pré-fill IA RÉEL 2 champs
      // (dateNotificationAnnexe, motifInterdictionEntree) via static getPrefillCount
      // + Annexe13quinquiesBePrefillRules. precedentSejour / recoursForme /
      // dateRecours aspirationnels (jamais comptés). VOIE (a) F-IA-03 : badge inline.
      ['F-IM-33-annexe13quinquies-ie-be', {
        displayLabel: 'Annexe 13quinquies OQT + interdiction entrée (BE)',
        component: Annexe13quinquiesBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-215-20 : composant complet F-IM-34-protection-temporaire-ukraine-be —
      // protection temporaire des personnes déplacées d'Ukraine (décision
      // d'exécution (UE) 2022/382 activant la directive 2001/55/CE). BELGIQUE
      // uniquement, CONTEXTUAL via flag `protection_temporaire_ukraine_detectee`.
      // Checklist + calculateur : éligibilité (badge ELIGIBLE vert / INELIGIBLE
      // rouge), durée de protection restante (X jours — JetBrains Mono), bandeau
      // orange si renouvellement imminent (< 90 j), bloc droits travail proéminent
      // (mention « pas de single permit requis »), droits aux aides + chemin
      // procédural en liste numérotée. Pré-fill IA RÉEL 2 champs (dateArrivee,
      // nationaliteUkrainienne) via static getPrefillCount +
      // ProtectionTemporaireUkraineBePrefillRules. residenceUkraineAvant24Fev2022 /
      // apatridesUkraine / membreFamilleProtege / titreSejourBE aspirationnels
      // (jamais comptés). VOIE (a) F-IA-03 : badge inline sur dateArrivee.
      ['F-IM-34-protection-temporaire-ukraine-be', {
        displayLabel: 'Protection temporaire Ukraine (BE)',
        component: ProtectionTemporaireUkraineBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-215-06 : composant complet F-IM-27-regroupement-10bis-be — regroupement
      // familial art. 10bis (BE). BELGIQUE uniquement, CONTEXTUAL via flag
      // `regroupement_10bis_detecte`. Pré-fill IA 4 champs (lienFamilial,
      // revenusMensuelsNets, dureeSejour, dateFinCarteA) via static
      // getPrefillCount + Regroupement10bisBePrefillRules. À la différence du
      // 10ter, le type de carte est forcé à CARTE_A (séjour LIMITÉ) et le
      // résultat expose `conditionTitreEnCours` (boolean) reflétant la
      // validité du titre A à la date d'analyse.
      ['F-IM-27-regroupement-10bis-be', {
        displayLabel: 'Regroupement familial 10bis (BE)',
        component: Regroupement10bisBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-215-08 : composant complet F-IM-28-naturalisation-12bis-be —
      // naturalisation art. 12bis (BE). BELGIQUE uniquement, CONTEXTUAL via flag
      // `naturalisation_be_envisagee`. Pré-fill IA RÉEL 3 champs (dureeSejour,
      // typeSejour, niveauLangue) via static getPrefillCount +
      // Naturalisation12bisBePrefillRules. Les 4 checkboxes restantes
      // (preuveIntegration, preuveEmploi, menaceOrdrePublic, condamnationPenale)
      // sont aspirationnelles — `PREFILL_COUNT_ALWAYS_ZERO`.
      // Verdict : VOIE_5_ANS / VOIE_10_ANS (vert) ou AUCUNE (rouge) +
      // dureeManquante (mois) si > 0.
      ['F-IM-28-naturalisation-12bis-be', {
        displayLabel: 'Naturalisation 12bis (BE)',
        component: Naturalisation12bisBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-215-10 : composant complet F-IM-29-naturalisation-conjoint-belge-be —
      // naturalisation conjoint Belge art. 16 (Code de la nationalité belge).
      // BELGIQUE uniquement, CONTEXTUAL via flag `naturalisation_be_envisagee`
      // (partagé avec F-IM-28 art. 12bis SF-215-08). Pré-fill IA RÉEL 3 champs
      // (dateMarriage, dureeCohabitationMois, niveauLangue) via static
      // getPrefillCount + NaturalisationConjointBelgeBePrefillRules. Les 4
      // checkboxes restantes (cohabitationLegale, preuveIntegration,
      // menaceOrdrePublic, condamnationPenale) sont aspirationnelles —
      // `PREFILL_COUNT_ALWAYS_ZERO`. Verdict : ELIGIBLE (vert) / INELIGIBLE
      // (rouge) + dureeManquante (mois) si > 0.
      ['F-IM-29-naturalisation-conjoint-belge-be', {
        displayLabel: 'Naturalisation conjoint Belge (art. 16)',
        component: NaturalisationConjointBelgeBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-215-12 : composant complet F-IM-30-aesm-mena-be — outil composite
      // AESM + tutelle DGDE (MENA). BELGIQUE uniquement, CONTEXTUAL via flag
      // `mineur_non_accompagne_be_detecte` (F-203). Pré-fill IA RÉEL 3 champs
      // (menaAge, menaDateArrivee, menaDureeScolaire) via static
      // getPrefillCount + AesmMenaBePrefillRules. Les 5 checkboxes restantes
      // (tuteurDesigne, integrationScolaire, projetVieElabore,
      // perspectiveAutonomie, menaceOrdrePublic) sont aspirationnelles —
      // `PREFILL_COUNT_ALWAYS_ZERO`. Composite 2 volets : tutelle DGDE
      // (Loi 04/05/2007) + AESM scoring (Art. 9bis adapté + Circulaire OE
      // 15/09/2005). Verdict AESM 3 états + bandeau urgence rouge si age ≥ 17.
      ['F-IM-30-aesm-mena-be', {
        displayLabel: 'AESM + tutelle DGDE (MENA)',
        component: AesmMenaBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-216-02 : composant simulateur complet (POST/GET backend SF-216-01).
      // Remplace le wrapper présentationnel SF-198-01. Pré-fill IA branché sur
      // `synthesis.familleExtractedData` (6 champs : durée mariage, revenus 1/2,
      // âges 1/2, avantage matrimonial via clause attribution intégrale).
      ['F-FA-01-prestation-compensatoire', {
        displayLabel: 'Prestation compensatoire (FR)',
        component: PrestationCompensatoireSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-216-04 : composant simulateur complet (POST/GET backend SF-216-03).
      // Remplace le wrapper présentationnel SF-198-02. Pré-fill IA branché sur
      // `synthesis.familleExtractedData` (5 champs : revenus 1/2 mensuels,
      // nombre enfants, âges enfants, mode résidence).
      ['F-FA-02-pension-alimentaire', {
        displayLabel: 'Pension alimentaire enfant (FR)',
        component: PensionAlimentaireEnfantFrSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // F-198 SF-198-03 : restauration de F-FA-04-liquidation-communaute (DELETE
      // par migration 191, restauré par migration 212). Wrapper présentationnel
      // sur synthesis.liquidationCommunaute.
      ['F-FA-04-liquidation-communaute', {
        displayLabel: 'Liquidation de communauté (FR)',
        component: LiquidationCommunauteSectionComponent,
        inputs: (ctx) => ({
          synthesis: ctx.synthesis,
        }),
      }],
      // SF-216-08 : composant simulateur complet (POST/GET backend SF-216-07).
      // Outil P1 famille FR — ARIPA recouvrement pension alimentaire impayée
      // (art. L. 581 CSS). Pré-fill IA branché sur `synthesis.familleExtractedData`
      // (montant pension du titre + titre exécutoire détecté + nb enfants).
      ['F-FA-ARIPA-RECOUVREMENT', {
        displayLabel: 'ARIPA recouvrement (FR)',
        component: AripaRecouvrementFrSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-216-10 : composant simulateur complet (POST/GET backend SF-216-09).
      // Outil P1 famille FR — délégation d'autorité parentale (art. 376-1 Cciv).
      // Pré-fill IA branché sur `synthesis.familleExtractedData` (âge enfant +
      // lien tiers + accord parents).
      ['F-FA-XX-delegation-ap', {
        displayLabel: 'Délégation autorité parentale (FR)',
        component: DelegationApFrSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-216-12 : composant simulateur complet (POST/GET backend SF-216-11).
      // Outil P2 famille FR — Retrait d'autorité parentale (art. 378-381 Cciv +
      // loi n°2022-140 du 7 février 2022 LMVSS + art. 343-1 al. 2 Cciv). Pré-fill
      // IA sur 4 champs (âge enfant, condamnation pénale, danger caractérisé,
      // violences conjugales) — visibility CONTEXTUAL F-IA-04 priority 103.
      ['F-FA-RETRAIT-AP', {
        displayLabel: 'Retrait autorité parentale (FR)',
        component: RetraitApFrSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-216-16 : composant simulateur complet (POST/GET backend SF-216-15).
      // Outil P2 famille FR — Adoption de l'enfant du conjoint (adoption
      // intra-familiale, art. 345-1 Cciv). V1 sans pré-fill IA — flag IA
      // `adoption_intra_detection.envisagee` utilisé uniquement pour
      // l'activation visibility CONTEXTUAL F-IA-04 (priority 105).
      ['F-FA-ADOPTION-INTRA', {
        displayLabel: 'Adoption intra-familiale (FR)',
        component: AdoptionIntraFrSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-216-18 : composant simulateur complet (POST/GET backend SF-216-17).
      // Outil P2 famille FR — Adoption internationale (art. 370-3 à 370-5 Cciv +
      // Convention La Haye 1993 + Loi n°2001-111 / agrément). 3 champs pré-fill
      // IA (pays, agrément, exequatur) — visibility CONTEXTUAL F-IA-04 priority 106.
      ['F-FA-ADOPTION-INTERNATIONALE', {
        displayLabel: 'Adoption internationale (FR)',
        component: AdoptionInternationaleFrSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-216-14 : composant simulateur complet (POST/GET backend SF-216-13).
      // Outil P2 famille FR — Audition du mineur par le JAF (art. 388-1 Cciv
      // + art. 1074-1 à 1074-3 CPC + CIDE art. 12). 2 champs pré-fill IA
      // (âge enfant, demande formalisée) — visibility CONTEXTUAL F-IA-04 priority 104.
      ['F-FA-AUDITION-MINEUR', {
        displayLabel: 'Audition du mineur (FR)',
        component: AuditionMineurFrSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-216-20 : composant simulateur complet (POST/GET backend SF-216-19).
      // Outil P2 famille FR — Indignité successorale (art. 726-729-1 Cciv +
      // Loi n°2022-1617 violences intrafamiliales). 3 champs pré-fill IA
      // (condamnation, pardon, date ouverture) — visibility CONTEXTUAL F-IA-04
      // priority 107.
      ['F-FA-INDIGNITE-SUCCESSORALE', {
        displayLabel: 'Indignité successorale (FR)',
        component: IndigniteSuccessoraleFrSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-216-22 : composant simulateur complet (POST/GET backend SF-216-21).
      // Outil P2 famille FR — Recel de succession (art. 778 Cciv +
      // Cass. 1ère civ., 14/11/2012). 3 champs pré-fill IA
      // (typeRecel, preuveRecel, date ouverture) — visibility CONTEXTUAL F-IA-04
      // priority 108.
      ['F-FA-RECEL-SUCCESSION', {
        displayLabel: 'Recel de succession (FR)',
        component: RecelSuccessionFrSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-216-24 : composant simulateur complet (POST/GET backend SF-216-23).
      // Outil P2 famille FR — Donation entre époux / avantage matrimonial
      // (art. 1091-1100 Cciv + art. 265 al. 2 + art. 1527 al. 2 + art. 912-928).
      // 5 champs pré-fill IA (régime, clause attribution intégrale, enfants
      // non communs, révocabilité, bien donné) — visibility CONTEXTUAL F-IA-04
      // priority 109.
      ['F-FA-DONATION-ENTRE-EPOUX', {
        displayLabel: 'Donation entre époux (FR)',
        component: DonationEntreEpouxFrSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-216-28 : composant simulateur complet (POST/GET backend SF-216-27).
      // Outil P2 famille FR — Partage successoral notarié (art. 816 et s.
      // Cciv + art. 870 Cciv + art. 1592 CGI + art. 641 CGI + art. 840
      // Cciv). 4 champs pré-fill IA (date ouverture, cohéritiers, masse,
      // présence immeuble) — visibility CONTEXTUAL F-IA-04 priority 111.
      ['F-FA-PARTAGE-NOTARIAL', {
        displayLabel: 'Partage successoral notarié (FR)',
        component: PartageNotarialFrSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-216-30 : composant simulateur complet (POST/GET backend SF-216-29).
      // Outil P2 famille FR — Donation-partage (art. 1075 à 1075-5 Cciv +
      // art. 1078 / gel valeur + art. 1078-1 / réincorporation + art. 1080
      // / quasi-usufruit + art. 912-928 / réserve). 4 champs pré-fill IA
      // (descendants, quotité, petits-enfants substitution, conjonctive)
      // — visibility CONTEXTUAL F-IA-04 priority 112.
      ['F-FA-DONATION-PARTAGE', {
        displayLabel: 'Donation-partage (FR)',
        component: DonationPartageFrSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-216-26 : composant simulateur complet (POST/GET backend SF-216-25).
      // Outil P2 famille FR — Présomption de paternité du mari et désaveu
      // (art. 312-315 Cciv + art. 316 al. 2 + art. 333 al. 1 + Cass. 1ère
      // civ., 19/2/2014). 5 champs pré-fill IA (date naissance, possession
      // état conforme, dates mariage / dissolution, désaveu envisagé) —
      // visibility CONTEXTUAL F-IA-04 priority 110.
      ['F-FA-PRESOMPTION-PATERNITE', {
        displayLabel: 'Présomption de paternité (FR)',
        component: PresomptionPaterniteFrSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-FA-05-partage-immobilier', {
        displayLabel: 'Partage immobilier',
        component: PartageImmobilierSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          liquidationCommunaute: ctx.synthesis?.liquidationCommunaute,
          // SF-155-20 : pré-fill IA via FamilleExtractedData (valeurImmeuble + capitalRestantDu).
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02c — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-FA-15-recompenses', {
        displayLabel: 'Récompenses entre époux (FR)',
        component: RecompensesSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-FA-15-02 : pré-fill IA via FamilleExtractedData (regimeMatrimonialDetecte)
          // + validation F-IA-03 sur REGIME_MATRIMONIAL.
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02c — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-FA-06-calendrier-garde', {
        displayLabel: 'Calendrier de garde',
        component: CalendrierGardeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiModeGardeDetaille: ctx.synthesis?.pensionAlimentaireEstimate?.modeGardeDetaille,
          // SF-246-10 : aiData branché sur familleExtractedData pour pré-fill âges + dates.
          aiData: ctx.synthesis?.familleExtractedData,
          synthesis: ctx.synthesis,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-FA-07-checklist-divorce', {
        displayLabel: 'Checklist divorce',
        component: DivorceChecklistSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          // SF-155-19 : pré-fill IA via FamilleExtractedData (dateAcceptationPV).
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-194 SF-194-02 : libellés des pièces statut OBTENUE alignées sur cet outil.
          piecesObtenues: piecesObtenuesFor(ctx.piecesAlignment, 'F-FA-07-checklist-divorce'),
        }),
      }],
      // F-198 SF-198-04 : restauration de F-152-divorce-consentement-scoring
      // (DELETE par migration 191, restauré par migration 212). Wrapper qui
      // délègue au composant F-152 existant (présentationnel pur).
      ['F-152-divorce-consentement-scoring', {
        displayLabel: 'Divorce par consentement mutuel — scoring (FR/BE)',
        component: DivorceCmScoringSectionComponent,
        inputs: (ctx) => ({
          synthesis: ctx.synthesis,
          // F-242 : propage le pays workspace pour adapter les libellés des 7 critères (FR/BE).
          workspaceCountry: ctx.workspaceCountry ?? null,
        }),
      }],
      // F-198 SF-198-05 : restauration de F-153-fourchettes-jaf (DELETE par
      // migration 191, restauré par migration 212). Wrapper qui agrège les
      // jurisprudenceRange p25/p50/p75 livrées par F-153.
      ['F-153-fourchettes-jaf', {
        displayLabel: 'Fourchettes JAF (FR)',
        component: FourchettesJafSectionComponent,
        inputs: (ctx) => ({
          synthesis: ctx.synthesis,
        }),
      }],
      ['F-IM-01-checklist-pieces', {
        displayLabel: 'Checklist pièces immigration',
        component: ImmigrationChecklistSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          caseFileTitle: ctx.caseFileTitle,
          inferredChecklistType: ctx.synthesis?.immigrationExtractedData?.inferredChecklistType ?? null,
        }),
      }],
      ['F-IM-05-arbre-decisionnel-titre', {
        displayLabel: 'Arbre décisionnel titre de séjour (FR)',
        component: ImmigrationTitleDecisionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          aiData: ctx.synthesis?.immigrationExtractedData,
          triggerEvents: ctx.synthesis?.immigrationTriggerEvents,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-192 SF-192-02 : pistes 🟢 RETAINED filtrées sur cet outil.
          pistesRetenues: (ctx.pistesRetenues ?? []).filter(
            p => p.toolIdCible === 'F-IM-05-arbre-decisionnel-titre',
          ),
          // F-194 SF-194-02 : libellés des pièces statut OBTENUE alignées sur cet outil.
          piecesObtenues: piecesObtenuesFor(ctx.piecesAlignment, 'F-IM-05-arbre-decisionnel-titre'),
        }),
      }],
      ['F-IM-06-recours', {
        displayLabel: 'Recours immigration (FR)',
        component: ImmigrationRecoursSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          caseFileTitle: ctx.caseFileTitle,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-192 SF-192-02 : pistes 🟢 RETAINED filtrées sur cet outil.
          pistesRetenues: (ctx.pistesRetenues ?? []).filter(
            p => p.toolIdCible === 'F-IM-06-recours',
          ),
          // F-194 SF-194-02 : libellés des pièces statut OBTENUE alignées sur cet outil.
          piecesObtenues: piecesObtenuesFor(ctx.piecesAlignment, 'F-IM-06-recours'),
        }),
      }],
      ['F-IM-07-droit-au-travail', {
        displayLabel: 'Droit au travail des étrangers (FR)',
        component: ImmigrationWorkRightSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-IM-08-oqtf-avec-delai-fr', {
        displayLabel: 'OQTF avec délai (FR)',
        component: OqtfAvecDelaiSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-155-04-B1 : pré-fill IA + alertes de cohérence F-IA-03.
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-IM-08-oqtf-sans-delai-fr', {
        displayLabel: 'OQTF sans délai (FR)',
        component: OqtfSansDelaiSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-155-04-B2 : pré-fill IA + validation F-IA-03 (urgence 48h).
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-IM-08-annexe13-be', {
        displayLabel: 'Annexe 13 — OQT (Belgique)',
        component: Annexe13BeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-155-04-C : pré-fill IA + validation F-IA-03 (4 champs BE).
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-IM-08-08 : référés administratifs L.521-1 / L.521-2 (FR uniquement).
      ['F-IM-08-referes-admin-fr', {
        displayLabel: 'Référés administratifs OQTF (FR)',
        component: ReferesAdminSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-DT-35-02 : contestation ARE / France Travail (FR uniquement).
      ['F-DT-35-contestation-are-fr', {
        displayLabel: 'Contestation ARE Pôle emploi (FR)',
        component: ContestationAreSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // Pré-fill IA gracieux (dateLicenciement → dateNotificationDecision)
          // + validation F-IA-03 (DATE_NOTIFICATION).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-DT-27-motif-grave-be', {
        displayLabel: 'Motif grave (Belgique)',
        component: MotifGraveBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-27-02 : pré-fill IA (dateLicenciement + salaireBrutMensuel)
          // + validation F-IA-03 (2 champs : DATE_RUPTURE + SALAIRE).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      // SF-213-01b : clause de non-concurrence BE (Loi 03/07/1978 art. 65 +
      // CCT n°13). BE-only, CONTEXTUAL `clause_non_concurrence_presente=true`.
      // 3 champs IA pré-remplis depuis TravailExtractedData
      // (salaireBrutAnnuel, clauseNonConcurrenceDureeMois, clauseNonConcurrenceZone).
      ['clause-non-concurrence-be', {
        displayLabel: 'Clause de non-concurrence (Belgique)',
        component: ClauseNonConcurrenceBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-213-02b : rappel de salaire BE (Loi 12/04/1965 art. 10 — intérêts
      // moratoires 10 % + Loi 03/07/1978 art. 15 al. 1 — prescription 1 an
      // post-rupture / 5 ans pendant contrat). BE-only, ALWAYS_ON.
      // 4 champs IA pré-remplis depuis TravailExtractedData branche BE
      // (montantArrieresSalaireBrut, dateDebutArrieresSalaire,
      // dateFinArrieresSalaire, dateRuptureContrat) + typeArriereEnum dérivé.
      ['rappel-salaire-be', {
        displayLabel: 'Rappel de salaire (Belgique)',
        component: RappelSalaireBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-213-03b : préavis "statut unique" BE (Loi 26/12/2013 + art. 37/2
      // §1er Loi 03/07/1978 — barème par paliers d'ancienneté en semaines).
      // BE-only, ALWAYS_ON (réflexe transversal sur tout licenciement BE
      // post-2014, soit la quasi-totalité des contrats actifs en 2026).
      // 3 champs IA pré-remplis depuis TravailExtractedData : ancienneté
      // (couple années/mois dérivé dateEntree→dateLicenciement), salaire
      // hebdo (annuel/52 ou mensuel×12/52), date notification (= dateLicenciement).
      // Flag UX `partieStatutUniqueSeulement` dérivé de dateEntree >= 2014-01-01
      // (non compté dans le badge — paramètre de calcul, pas champ IA factuel).
      ['licenciement-be-statut-unique-preavis', {
        displayLabel: 'Préavis statut unique (Belgique)',
        component: LicenciementBeStatutUniquePreavisSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-213-04b : préavis Formule Claeys BE (ancien art. 82 §3 Loi
      // 03/07/1978 + Cass. 28/02/2011 RG S.10.0073.F + Loi 26/12/2013
      // art. 67 clause de sauvegarde). BE-only, ALWAYS_ON priority 112
      // (juste au-dessus de statut-unique-preavis = 111). 4 champs IA
      // pré-remplis depuis TravailExtractedData : ancienneté pré-2014
      // (couple années/mois dérivé dateEntree→2014-01-01), rémunération
      // annuelle K€ (salaireBrutAnnuel/1000 ou mensuel×12/1000), ancienneté
      // post-2014 (de 2014-01-01 à dateLicenciement), salaire hebdo brut.
      // Flag UX `appliquerClauseSauvegarde` dérivé de dateEntree pré-2014
      // (non compté badge — paramètre de calcul). Champs post-2014
      // conditionnels masqués + reset si toggle off.
      ['licenciement-be-formule-claeys', {
        displayLabel: 'Préavis Formule Claeys (Belgique)',
        component: LicenciementBeFormuleClaeysSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-213-05b : protection grossesse BE (Loi 16/03/1971 art. 40).
      // Analyseur de la validité d'un licenciement intervenu pendant la
      // grossesse / maternité. Verdict 4 états (HORS_PERIODE_PROTECTION,
      // PROTECTION_APPLICABLE_NON_NOTIFIEE, PROTECTION_APPLICABLE,
      // PROTECTION_PRESUMEE — charge de preuve renversée si notification
      // écrite + licenciement ≤ 10 sem post début grossesse). BE-only,
      // ALWAYS_ON priority 113 (juste au-dessus de formule-claeys = 112).
      // Pré-fill IA gracieux 2 champs (dateLicenciement, rémunération
      // mensuelle brute via salaireBrutMensuel ou salaireBrutAnnuel/12).
      // Les autres champs (grossesse, accouchement, congé, notification,
      // motif) restent en saisie avocat — pas d'extraction IA BE-only
      // grossesse dans cette vague.
      ['licenciement-be-protection-grossesse', {
        displayLabel: 'Protection grossesse (Belgique)',
        component: LicenciementBeProtectionGrossesseSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-213-06b : transaction de fin de contrat BE (art. 2044 Cciv +
      // Loi 03/07/1978 art. 6). Analyseur de validité 4 états (VALIDE,
      // INVALIDE, INVALIDE_PARTIELLE, A_COMPLETER) + checklist des
      // renonciations + ratio transaction/indemnité légale + avertissement
      // de lésion < 50 %. BE-only, ALWAYS_ON priority 114 (juste au-dessus
      // de protection-grossesse = 113). Pré-fill IA V1 : aucun champ
      // (analyse sémantique du document de transaction hors scope V1).
      // Distinct de F-DT-31-transaction (régime FR art. 2044 Cciv FR
      // moins exigeant — les deux coexistent gated par country).
      ['transaction-be-travail', {
        displayLabel: 'Transaction fin de contrat (Belgique)',
        component: TransactionBeTravailSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-213-07b : harcèlement BE — procédure formelle (Loi 04/08/1996
      // art. 32bis-32sexies + AR 10/04/2014). Checklist procédurale 5
      // étapes (avant dépôt, demande informelle, demande formelle,
      // enquête terminée, mesure défavorable post-plainte) + protection
      // représailles 12 mois (art. 32sexies) + délai fatal 90 j d'enquête
      // CPAP. BE-only, ALWAYS_ON priority 115 (juste au-dessus de
      // transaction-be-travail = 114). Pré-fill IA V1 : aucun champ.
      // Distinct de F-DT-11 (nullité licenciement représailles BE,
      // intervient en aval) — cet outil pilote la procédure interne en
      // amont de toute rupture.
      ['harcelement-be-procedure-formelle', {
        displayLabel: 'Harcèlement procédure formelle (Belgique)',
        component: HarcelementBeProcedureFormelleSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-213-08b : licenciement BE — protection délégué syndical
      // (Loi 19/03/1991 + CCT n° 5 du 24/05/1971). Analyseur de validité
      // binaire (INTERDIT_SANS_PROCEDURE / HORS_PROTECTION) + calcul
      // indemnité forfaitaire (rémunération annuelle × 2 ou × 4 selon
      // circonstances aggravantes / récidive) + délai 30 j (art. 14) pour
      // demander la réintégration. BE-only, ALWAYS_ON priority 116 (juste
      // au-dessus de harcelement-be-procedure-formelle = 115). Pré-fill IA
      // V1 : aucun champ (alignement pattern uniforme vagues 6b/7b).
      // Distinct de licenciement-be-protection-grossesse (SF-213-05b —
      // autre population protégée) et de F-DT-30-protection-rp (statut
      // protégé FR L. 2411-1 et s. C. trav. — régime distinct).
      ['licenciement-be-protection-deleguee', {
        displayLabel: 'Licenciement — protection délégué (Belgique)',
        component: LicenciementBeProtectionDelegueeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-213-09b : acte équipollent à rupture BE (Loi 03/07/1978 art. 20
      // + Cass. BE 23/12/1957). Analyseur 4 verdicts (ACTE_EQUIPOLLENT_PROBABLE
      // / PAS_ACTE_EQUIPOLLENT / RISQUE_ACCEPTATION_TACITE / A_ANALYSER) +
      // ICP indicatif (rémunération hebdo × préavis semaines) si verdict
      // probable + délai 30 j de protestation. BE-only, ALWAYS_ON priority
      // 117 (juste au-dessus de licenciement-be-protection-deleguee = 116).
      // Pré-fill IA V1 : aucun champ (alignement pattern uniforme vagues
      // 6b/7b/8b — qualification juridique fine non extractible). Distinct
      // du dispositif FR (prise d'acte L. 1237-19 C. trav. + résiliation
      // judiciaire) — gating par workspaceCountry.
      ['licenciement-be-acte-equivalent', {
        displayLabel: 'Acte équipollent à rupture (Belgique)',
        component: LicenciementBeActeEquivalentSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-213-10b : score CCT n° 109 BE — licenciement manifestement
      // déraisonnable (CCT 12/02/2014 art. 8-9). Échelle 5 niveaux
      // (NON_DERAISONNABLE 0 sem. / 3 sem. / 8 sem. / 12 sem. / 17 sem.)
      // + indemnité = rémunération hebdomadaire × nombre de semaines +
      // cumul ICP systématique (bannière info). BE-only, ALWAYS_ON
      // priority 118 (au-dessus de licenciement-be-acte-equivalent =
      // 117 livré par SF-213-09b). Pré-fill IA V1 : aucun champ
      // (alignement pattern uniforme vagues 6b/7b/8b). Distinct du
      // barème Macron FR (L. 1235-3 C. trav.) et de
      // F-DT-27-motif-grave-be (analyseur de validité du motif grave,
      // intervient en amont).
      ['licenciement-be-cct109-deraisonnable', {
        displayLabel: 'Score CCT 109 — licenciement déraisonnable (Belgique)',
        component: LicenciementBeCct109DeraisonnableSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-01b : RCC métiers lourds BE (CCT 17 + AR 03/05/2007 art. 3).
      // Analyseur d'éligibilité 2 verdicts (ELIGIBLE / INELIGIBLE) avec
      // raison précise (DEMISSION / AGE_INSUFFISANT / CARRIERE_INSUFFISANTE
      // / DUREE_METIER_LOURD_INSUFFISANTE). Conditions cumulatives :
      // licenciement effectif ; âge ≥ 58 ; carrière ≥ 35 ; 5/10 OU 7/15
      // en métier lourd. BE-only, ALWAYS_ON priority 119 (juste au-dessus
      // de licenciement-be-cct109-deraisonnable = 118). Pré-fill IA V1 :
      // aucun champ (alignement pattern uniforme F-213 vagues 6b-10b —
      // qualification métier lourd / carrière non extractible V1). Distinct
      // de rcc-be-conditions (F-207 SF-207-06b — RCC général 60+/40) et de
      // rcc-be-indemnite-complementaire (F-207 SF-207-07b — calcul transverse).
      ['rcc-be-metiers-lourds', {
        displayLabel: 'RCC métiers lourds (Belgique)',
        component: RccBeMetiersLourdsSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-02b : RCC BE — longue carrière (Loi 26/12/2013 + CCT n° 17
      // du 19/12/1974 + AR 03/05/2007 art. 3). Analyseur 4 verdicts
      // (ELIGIBLE_RCC_LONGUE_CARRIERE / INELIGIBLE_DEMISSION /
      // INELIGIBLE_AGE_INSUFFISANT / INELIGIBLE_CARRIERE_INSUFFISANTE)
      // + indemnité complémentaire mensuelle indicative (0,5 × diff. rém.
      // nette / allocation chômage) si éligible et couple financier
      // fourni. BE-only, ALWAYS_ON priority 120 (au-dessus de
      // licenciement-be-cct109-deraisonnable = 118). Pré-fill IA V1 :
      // aucun champ (alignement pattern uniforme vagues 7b-10b — carrière
      // ETP ONSS + dérivation âge + rémunération nette non extractibles).
      // Distinct de rcc-be-conditions (SF-207-06b — analyseur 4 régimes
      // parallèles) et rcc-be-indemnite-complementaire (SF-207-07b —
      // calculateur générique).
      ['rcc-be-longue-carriere', {
        displayLabel: 'RCC BE — longue carrière',
        component: RccBeLongueCarriereSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-03b : RCC BE — entreprise en difficulté / restructuration
      // (Loi 26/12/2013 + CCT n° 17 du 19/12/1974 + AR 03/05/2007 + AR de
      // reconnaissance ministérielle de l'entreprise + CCT sectorielle ad
      // hoc). Analyseur 6 verdicts (ELIGIBLE_RCC_ENTREPRISE_DIFFICULTE /
      // INELIGIBLE_DEMISSION / INELIGIBLE_RECONNAISSANCE_ABSENTE /
      // INELIGIBLE_AGE_INSUFFISANT / INELIGIBLE_CARRIERE_INSUFFISANTE /
      // INELIGIBLE_ANCIENNETE_INSUFFISANTE) + indemnité complémentaire
      // mensuelle indicative (0,5 × diff. rém. nette / allocation chômage)
      // si éligible et couple financier fourni. BE-only, ALWAYS_ON priority
      // 121 (au-dessus de rcc-be-longue-carriere = 120). Pré-fill IA V1 :
      // aucun champ (alignement pattern uniforme vagues 7b-10b/SF-219-01b/
      // SF-219-02b — reconnaissance ministérielle = acte administratif
      // externe, carrière ETP ONSS + ancienneté secteur + rémunération
      // nette non extractibles). Distinct de rcc-be-conditions (SF-207-06b
      // — analyseur 4 régimes parallèles), rcc-be-indemnite-complementaire
      // (SF-207-07b — calculateur générique), rcc-be-metiers-lourds
      // (SF-219-01b — régime 58+/35 + métier lourd) et rcc-be-longue-
      // carriere (SF-219-02b — régime 59+/40).
      ['rcc-be-entreprise-difficulte', {
        displayLabel: 'RCC BE — entreprise en difficulté',
        component: RccBeEntrepriseDifficulteSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-04b : Cumul RCC + allocations chômage / pension (BE) —
      // CCT 17 + AR 25/11/1991 ONEM + AR 03/05/2007 art. 22+.
      // Analyseur transversal 4 verdicts (CUMUL_CONFORME / CUMUL_PLAFOND_DEPASSE
      // / BASCULE_PENSION_LEGALE / INCOMPATIBLE_ACTIVITE_NON_AUTORISEE).
      // Intervient APRÈS l'éligibilité acquise par rcc-be-conditions /
      // rcc-be-metiers-lourds / rcc-be-longue-carriere / rcc-be-entreprise-difficulte :
      // vérifie le plafond de cumul (allocations ONEM + indemnité CCT 17
      // ≤ dernière rémunération nette) + détecte bascule pension légale +
      // compatibilité activité complémentaire. BE-only, ALWAYS_ON priority 122
      // (au-dessus de rcc-be-entreprise-difficulte = 121). Pré-fill IA V1 :
      // aucun champ (alignement pattern uniforme F-213/F-219 — montants nets
      // ONEM, accord CCT 17 employeur, carrière ETP non extractibles).
      ['cumul-rcc-allocations', {
        displayLabel: 'Cumul RCC + allocations (Belgique)',
        component: CumulRccAllocationsSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-05b : Outplacement BE général au titre du régime
      // préavis ≥ 30 semaines — Loi 05/09/2001 art. 11 + AR 21/10/2007.
      // Analyseur de conformité 7 verdicts (CONFORME / NON_DU_DEMISSION /
      // NON_DU_MOTIF_GRAVE / NON_DU_PREAVIS_INSUFFISANT /
      // NON_CONFORME_OFFRE_TARDIVE / NON_CONFORME_DUREE_INSUFFISANTE /
      // NON_CONFORME_FORME) + indemnité forfaitaire sanction art. 11/7.
      // BE-only, ALWAYS_ON priority 123 (au-dessus de cumul-rcc-allocations
      // = 122). Distinct de outplacement-be-obligatoire-45 (F-207 SF-207-08
      // — régime 45+ ans applicable quelle que soit la durée du préavis ;
      // les deux régimes peuvent coexister). Pré-fill IA V1 : aucun champ
      // (alignement pattern uniforme F-213/F-219 — qualifications juridiques
      // licenciement/motif grave/durée/forme non extractibles depuis
      // pipeline Travail BE actuel).
      ['outplacement-be-general-30sem', {
        displayLabel: 'Outplacement BE général (préavis ≥ 30 sem.)',
        component: OutplacementBeGeneral30semSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-06b : Licenciement BE — fermeture d'entreprise (Loi 26/06/2002 +
      // AR 23/03/2007 + CCT n° 9bis). Outil BE-only — Fonds de Fermeture
      // des Entreprises (FFE) géré par l'ONEM. Calcule l'indemnité de
      // fermeture (forfaitaire par année d'ancienneté + supplément ≥ 45 ans)
      // et les créances activables (salaires + pécule + indemnité de
      // rupture impayés) si l'employeur est insolvable. Verdict 6 états
      // (3 éligibles FFE / 3 motifs d'inéligibilité : ancienneté < 1 an /
      // type non qualifiant / effectif < 20 ETP). ALWAYS_ON priority 124
      // (au-dessus de outplacement-be-general-30sem = 123 SF-219-05b).
      // Pré-fill IA V1 : aucun champ (alignement pattern uniforme F-213/F-219 —
      // type fermeture / solvabilité / effectif employeur non extractibles).
      ['licenciement-be-fermeture-entreprise', {
        displayLabel: 'Fermeture d\'entreprise (Belgique)',
        component: LicenciementBeFermetureEntrepriseSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-07b : Licenciement collectif BE — Loi Renault (Loi 13/02/1998 +
      // CCT n° 24 + CCT n° 39 + Directive 98/59/CE). Outil BE-only — pas
      // d'équivalent FR (le PSE français suit une procédure DIRECCTE / DDETS
      // distincte avec validation/homologation, calendrier d'expertise CSE).
      // Checklist procédurale, pas calculateur — vérifie le seuil de
      // déclenchement (10/20/30 lic./60 j selon taille), les 3 phases
      // (information CE/CPPT → consultation → décision + notification
      // autorité régionale Forem/Actiris/VDAB) et le délai d'attente
      // obligatoire de 30 jours après notification (au cours duquel aucun
      // préavis ne peut être notifié sous peine de nullité + indemnité
      // spéciale art. 67). Verdict 6 états : NON_APPLICABLE_SEUIL (info) /
      // CONFORME (vert) / 4× NON_CONFORME_* (rouge : info incomplète,
      // consultation insuffisante, notification autorité manquante, délai
      // 30 j non respecté). ALWAYS_ON priority 125 (au-dessus de
      // licenciement-be-fermeture-entreprise = 124 SF-219-06b).
      // Pré-fill IA V1 : aucun champ (alignement pattern uniforme F-213/F-219
      // — qualifications procédurales / statut effectif employeur / actes
      // procéduraux non extractibles).
      ['licenciement-be-collectif-renault', {
        displayLabel: 'Licenciement collectif — Loi Renault (Belgique)',
        component: LicenciementBeCollectifRenaultSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-08b : Transfert d'entreprise CCT n° 32bis (Conseil National
      // du Travail, 07/06/1985) — transfert conventionnel BE (vente fonds,
      // fusion, scission, apport, démembrement) + Loi 17/03/1965 (info-
      // consultation préalable, sanction pénale) + Directive 2001/23/CE
      // (responsabilité solidaire 1 an). Outil BE-only — l'art. L. 1224-1
      // FR pose un régime parent mais sans les spécificités procédurales.
      // Verdict 5 états (CONFORME / NON_CONFORME_INFO_CONSULT /
      // INELIGIBLE_TYPE_OPERATION / INELIGIBLE_PAS_ENTITE_ECONOMIQUE /
      // A_ANALYSER). ALWAYS_ON priority 126 (au-dessus de
      // licenciement-be-collectif-renault = 125 SF-219-07b). Pré-fill IA V1 :
      // aucun champ (alignement pattern uniforme F-213/F-219 — qualification
      // juridique 9 cas, identité économique, info-consultation et
      // déclarations cessionnaire non extractibles).
      ['transfert-entreprise-cct-32bis', {
        displayLabel: 'Transfert d\'entreprise CCT 32bis (Belgique)',
        component: TransfertEntrepriseCct32bisSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-09b : Élections sociales (Belgique) — Loi du 04/12/2007 + AR
      // du 25/05/2012 + Loi 04/08/1996 art. 49 (CPPT) + Loi 19/03/1991
      // (protection candidats). Outil BE-only — pas d'équivalent FR (CSE
      // français = calendrier libre, seuils 11/50/300, pas de jour Y
      // national imposé). Verdict 4 états (OBLIGATION_CE_ET_CPPT /
      // OBLIGATION_CPPT_SEUL / NON_APPLICABLE_SEUIL / EFFECTIF_A_RECALCULER)
      // + calendrier rebours complet (X-60 / X / X-35 / X+35 / X+40 / Y /
      // Y+6 / Y+45) + fenêtre de protection candidats (Loi 19/03/1991).
      // ALWAYS_ON priority 127 (au-dessus de transfert-entreprise-cct-32bis
      // = 126 SF-219-08b). Pré-fill IA V1 : aucun champ (alignement pattern
      // uniforme F-213/F-219 — cycle AR / dateJourY employeur / effectif
      // ETP RH / UTE paritaire non extractibles).
      ['elections-sociales-be', {
        displayLabel: 'Élections sociales (Belgique)',
        component: ElectionsSocialesBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-10b : Statut du délégué syndical CCT n° 5 (CNT, 24/05/1971)
      // + CCT 5bis/5ter + AR 26/01/1972 + CCT sectorielles. Outil BE-only
      // de qualification + checklist statut DS : éligibilité (champ
      // sectoriel + désignation OS représentative + notification employeur),
      // missions exerçables (art. 3 + art. 24 supplétif CE/CPPT) et durée
      // indicative du mandat (4 ans). Distinct de SF-213-08 (protection
      // licenciement Loi 19/03/1991).
      // Verdict 5 états (STATUT_RECONNU / STATUT_FRAGILE_NOTIFICATION_MANQUANTE /
      // INELIGIBLE_ENTREPRISE_HORS_CHAMP / INELIGIBLE_PAS_DESIGNE_PAR_OS /
      // A_ANALYSER). ALWAYS_ON priority 128 (au-dessus de
      // elections-sociales-be = 127 SF-219-09b). Pré-fill IA V1 : aucun
      // champ (alignement pattern uniforme F-213/F-219 — qualifications
      // syndicales, paramétrage sectoriel, institutionnel CE/CPPT non
      // extractibles).
      ['delegue-syndical-cct-5', {
        displayLabel: 'Délégué syndical — CCT n° 5 (Belgique)',
        component: DelegueSyndicalCct5SectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-11b : Congé-éducation payé régionalisé (Loi 22/01/1985 Section 6,
      // régionalisée 2014 — WBR / FLA VOV / BXL). Outil BE-only de qualification
      // d'éligibilité au congé : région du lieu de travail (gate régional),
      // type de formation (5 cas dont HORS_LISTE_AGREEE), heures demandées,
      // taux d'occupation et public fragilisé (dérogation FLA). L'outil branche
      // en interne sur le régime régional applicable et calcule le plafond
      // d'heures puis le prorata occupation. Verdict 5 états (ELIGIBLE_PLEIN_DROIT /
      // ELIGIBLE_PRORATA / INELIGIBLE_HORS_FORMATION_AGREEE /
      // INELIGIBLE_OCCUPATION_INSUFFISANTE / A_ANALYSER). ALWAYS_ON priority 129
      // (au-dessus de delegue-syndical-cct-5 = 128 SF-219-10b). Pré-fill IA V1 :
      // aucun champ (alignement pattern uniforme F-213/F-219 — région lieu de
      // travail, type formation, agrément régional non extractibles).
      ['conge-education-paye-region', {
        displayLabel: 'Congé-éducation payé — régionalisé (Belgique)',
        component: CongeEducationPayeRegionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-12b : Flexi-job BE (Loi-programme du 26/12/2013 art. 13 à 28 +
      // Loi 25/04/2014 + Loi-programme 16/11/2015 boulangerie/coiffure +
      // Loi-programme 30/10/2018 commerce/agriculture/soins de santé +
      // suppression plafond pensionnés + Loi-programme 28/12/2023 +
      // AR 02/06/2024 extension secteurs publics/sport/culture/garages/
      // transport). Outil BE-only — pas d'équivalent FR (CDD d'usage et
      // CDI intérimaire FR partagent une logique d'occupation atypique
      // mais le régime de cotisations / formalisme Dimona / plafonds
      // annuels est structurellement incomparable). Verdict hiérarchisé
      // 7 états (ELIGIBLE / FRAGILE_CONTRAT_OU_DIMONA_MANQUANT /
      // FRAGILE_PLAFOND_DEPASSE / INELIGIBLE_TRAVAILLEUR_HORS_CONDITION /
      // INELIGIBLE_SECTEUR_NON_ELIGIBLE /
      // INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR / A_ANALYSER) — priorité
      // travailleur → secteur → cumul → formalisme → rémunération +
      // ventilation 5 dimensions cumulatives + montant excédentaire au
      // plafond annuel exonéré. ALWAYS_ON priority 130 (au-dessus de
      // conge-education-paye-region = 129 SF-219-11b et delegue-syndical-
      // cct-5 = 128 SF-219-10b). Pré-fill IA V1 : aucun champ (alignement
      // pattern uniforme F-213/F-219 — date occupation flexi employeur /
      // statut T-3 chez un AUTRE employeur / secteur paritaire flexi /
      // formalisme Dimona FLX / paramètres légaux indexés non
      // extractibles du dossier salarié principal).
      ['flexi-job-be', {
        displayLabel: 'Flexi-job (Belgique)',
        component: FlexiJobBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-13b : Étudiant jobiste BE (Loi du 03/07/1978 Titre VII art. 120
      // à 130ter + Loi-programme du 24/12/2002 cotisations de solidarité
      // réduites + AR du 14/07/1995 modalités 5,42 % patronale + 2,71 %
      // personnelle = 8,13 % + AR du 05/11/2002 Dimona STU + Loi du
      // 18/12/2016 passage 50 jours → 475 heures + Loi-programme du
      // 28/12/2022 + AR du 06/03/2023 relèvement transitoire 600h/an +
      // Loi-programme du 22/12/2023 pérennisant 600h/an). Outil BE-only —
      // pas d'équivalent FR (le job étudiant FR relève du droit commun
      // salarié avec abattements art. L. 241-3-2 CSS mais sans quota
      // horaire annuel ni régime spécifique unifié — aucune transposition
      // mécanique). Verdict hiérarchisé 6 états (ELIGIBLE /
      // FRAGILE_CONTRAT_OU_DIMONA_MANQUANT / FRAGILE_COTISATIONS_NON_REDUITES /
      // INELIGIBLE_STATUT_NON_ETUDIANT / INELIGIBLE_QUOTA_DEPASSE /
      // A_ANALYSER) — priorité statut → vide pédagogique → quota →
      // formalisme → cotisations + ventilation 4 dimensions cumulatives
      // + heures restantes au quota + heures hors quota + redressement
      // estimé (différentiel ~41,87 %). ALWAYS_ON priority 131 (au-dessus
      // de flexi-job-be = 130 SF-219-12b). Pré-fill IA V1 : aucun champ
      // (alignement pattern uniforme F-213/F-219 — statut étudiant,
      // compteur Student@work, volume contrat, formalisme contrat/Dimona
      // STU, barème cotisations non extractibles du pipeline Travail BE).
      ['etudiant-jobiste-be', {
        displayLabel: 'Étudiant jobiste (Belgique)',
        component: EtudiantJobisteBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-14b : statut intérim BE — CCT n° 322 (Loi du 24/07/1987
      // relative au travail temporaire / intérimaire / mise à disposition,
      // M.B. 20/08/1987 + CCT n° 322 du 14/06/2010 conclue au CNT
      // responsabilité solidaire ETI/utilisateur + parité salariale stricte
      // + CCT n° 108 du 16/07/2013 motif insertion en vue d'embauche
      // durable + AR 11/10/1976 liste limitative cas de travail
      // exceptionnel + Loi-programme du 27/12/2012 sanctions ONSS
      // renforcées). Outil BE-only — la France a un régime de travail
      // temporaire structurellement différent (motifs limitatifs,
      // plafonds, formalisme DPAE vs Dimona, parité salariale non
      // mécanique). Verdict hiérarchisé 7 états (ELIGIBLE_MISSION_REGULIERE /
      // FRAGILE_CONTRAT_OU_DIMONA_MANQUANT / INELIGIBLE_MOTIF_INTERDIT_
      // GREVE_LOCKOUT / INELIGIBLE_MOTIF_NON_AUTORISE / INELIGIBLE_DUREE_
      // MAX_DEPASSEE / INELIGIBLE_PARITE_SALARIALE_VIOLEE / A_ANALYSER)
      // — priorité grève/lock-out → motif → zone grise → durée → parité
      // → formalisme + ventilation 4 dimensions cumulatives + jours
      // excédentaires + écart parité salariale. ALWAYS_ON priority 132
      // (au-dessus de etudiant-jobiste-be = 131 SF-219-13b et
      // flexi-job-be = 130 SF-219-12b). Pré-fill IA V1 : aucun champ
      // (alignement pattern uniforme F-213/F-219 — date mission ETI /
      // motif limitatif / durée cumulée / parité salariale utilisateur /
      // formalisme Dimona ETI / paramètres légaux du plafond non
      // extractibles du dossier salarié principal).
      ['interim-be-cct-322', {
        displayLabel: 'Intérim (Belgique — CCT 322)',
        component: InterimBeCct322SectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-15b : Indemnité fin de mission intérim BE (Loi 24/07/1987
      // + CCT n° 322 + CCT n° 322bis du 16/12/2010 pécule vacances 15,38 %
      // FSI + AR 30/03/1967 art. 19 + CCT sectorielles propres à la CP
      // utilisateur prime fin d'année 1/12 brut annuel + jurisprudence
      // Cass. BE 04/02/1991 et 16/12/2002 rupture anticipée injustifiée =
      // rémunérations restant à courir + Cass. BE 03/05/2010 et 23/06/2003
      // refus prime de précarité 10 % style FR + Loi 16/03/1971 art. 29
      // sursalaire heures supplémentaires). Outil BE-only — la France
      // impose une IFM forfaitaire 10 % (art. L. 1251-32) que la
      // jurisprudence Cass. BE refuse explicitement de transposer. Aucune
      // équivalence mécanique. Verdict hiérarchisé 4 états (INDEMNITES_DUES /
      // RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR / AUCUNE_INDEMNITE_DUE /
      // A_ANALYSER_SECTEUR_NON_RECONNU) — priorité rupture anticipée →
      // secteur non reconnu → indemnités dues → aucune indemnité. ALWAYS_ON
      // priority 133 (au-dessus de interim-be-cct-322 = 132 SF-219-14b et
      // etudiant-jobiste-be = 131 SF-219-13b). Pré-fill IA V1 : aucun
      // champ (alignement pattern uniforme F-213/F-219 — dates mission ETI /
      // durées / salaire horaire / relevés d'heures / flag FSI / qualification
      // rupture / CP utilisateur / ancienneté sectorielle non extractibles
      // du dossier salarié principal).
      ['interim-be-indemnite-fin-mission', {
        displayLabel: 'Indemnité fin de mission intérim (Belgique)',
        component: InterimBeIndemniteFinMissionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-16b : télétravail BE — CCT n° 85 du 09/11/2005 (CNT,
      // structurel régulier, rendue obligatoire AR 13/06/2006 M.B.
      // 05/09/2006) + CCT n° 149 du 26/01/2021 (CNT, occasionnel /
      // force majeure / COVID) + Loi du 03/07/1978 art. 17 + Loi du
      // 04/08/1996 + Code du bien-être au travail Livre VIII Titre 1 +
      // Loi du 26/03/2018 art. 16-18 (droit à la déconnexion par
      // concertation collective) + Loi du 03/10/2022 « Deal pour
      // l'emploi » M.B. 10/11/2022 (modalités déconnexion CCT /
      // règlement de travail obligatoires entreprises ≥ 20 travailleurs,
      // e.e.v. 01/04/2023). Outil BE-only — la France a un cadre
      // télétravail structurellement différent (C. trav. L. 1222-9 et s.
      // + ANI 26/11/2020 ; déconnexion encadrée par L. 2242-17
      // négociation annuelle). Verdict hiérarchisé 7 états
      // (CONFORME_CCT_85_STRUCTUREL / CONFORME_CCT_149_OCCASIONNEL /
      // NON_CONFORME_CONVENTION_ECRITE_MANQUANTE /
      // NON_CONFORME_EQUIPEMENT_NON_FOURNI / NON_CONFORME_DROITS_REDUITS /
      // FRAGILE_DECONNEXION_NON_DEFINIE / A_ANALYSER) — priorité
      // structurel conforme → occasionnel conforme → convention écrite →
      // équipement → droits → déconnexion → à analyser + ventilation
      // 4 conformités cumulatives + indemnité excédentaire au plafond
      // ONSS/SPF Finances. ALWAYS_ON priority 134 (au-dessus de
      // interim-be-cct-322 = 132 SF-219-14b ; priorité 133 réservée à
      // SF-219-15b interim-be-indemnite-fin-mission parallèle).
      // Pré-fill IA V1 : aucun champ (alignement pattern uniforme
      // F-213/F-219 — date entrée télétravail / type structurel ou
      // occasionnel / flags art. 4-5-6-9 CCT n° 85 / montant indemnité /
      // plafond ONSS/SPF Finances / modalités déconnexion Loi 03/10/2022 /
      // effectif entreprise non extractibles du dossier salarié principal).
      ['teletravail-be-cct-85-149', {
        displayLabel: 'Télétravail (Belgique — CCT 85 / 149)',
        component: TeletravailBeCct85149SectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-17b : Clause d'écolage BE — art. 22bis Loi 03/07/1978 (BE-only,
      // ALWAYS_ON priority 135 BE / DROIT_DU_TRAVAIL — au-dessus de
      // teletravail-be-cct-85-149 (134, SF-219-16b) et
      // interim-be-indemnite-fin-mission (133, SF-219-15b) ; priorité 136
      // réservée à SF-219-18b semaine-4-jours-be parallèle).
      // Pré-fill IA V1 : aucun champ (alignement pattern uniforme
      // F-213/F-219 — type formation, forme écrite, coût réel, RMMMG mensuel
      // CCT n° 43, durée d'efficacité, dates fin formation / départ,
      // qualification motif art. 22bis § 3 non extractibles du dossier salarié
      // principal).
      ['clause-ecolage-be', {
        displayLabel: 'Clause d\'écolage (Belgique — art. 22bis)',
        component: ClauseEcolageBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-18b : semaine de 4 jours BE — Loi du 03/10/2022 « Deal
      // pour l'emploi » M.B. 10/11/2022, art. 5 (régime à la demande
      // du travailleur à temps plein — compression durée hebdomadaire
      // 38-40 h sur 4 jours, journée plafonnée 9 h 30 ou 10 h CCT,
      // avenant écrit ≤ 6 mois renouvelable, refus motivé écrit dans
      // le mois, protection licenciement = 6 mois rémunération) +
      // art. 6 (procédure simplifiée règlement de travail) ; Loi du
      // 16/03/1971 art. 19 (limites quotidienne 9 h et hebdomadaire
      // 40 h droit commun) ; Loi du 03/07/1978 art. 25 (modification
      // élément essentiel du contrat) ; Loi du 08/04/1965 art. 12
      // (règlements de travail). Outil BE-only — la semaine de 4 jours
      // française (C. trav. L. 3122-2 et s., Loi 13/06/1998 « Aubry I »
      // + Loi 19/01/2000 « Aubry II ») est un aménagement collectif
      // par accord d'entreprise et non une demande individuelle du
      // salarié à temps plein. Aucune transposition mécanique.
      // Verdict hiérarchisé 9 états (CONFORME_REGIME_4_JOURS_VALIDE /
      // LICENCIEMENT_REPRESAILLES_PRESUME / REFUS_EMPLOYEUR_NON_MOTIVE
      // / NON_ELIGIBLE_TEMPS_PARTIEL / NON_CONFORME_DEMANDE_ECRITE
      // / NON_CONFORME_JOURNEE_DEPASSE_9H30 / NON_CONFORME_AVENANT
      // _OU_REGLEMENT_MANQUANT / NON_CONFORME_DUREE_DEPASSE_6_MOIS
      // / A_ANALYSER) — court-circuits backend (statut indéterminé →
      // licenciement représailles → refus non motivé → temps partiel
      // → demande écrite → journée → formalisation → durée → conforme)
      // + ventilation 5 conformités cumulatives. ALWAYS_ON priority
      // 136 (au-dessus de clause-ecolage-be = 135 SF-219-17b et
      // teletravail-be-cct-85-149 = 134 SF-219-16b). Pré-fill IA V1 :
      // aucun champ (alignement pattern uniforme F-213/F-219 — statut
      // demande / temps plein / demande écrite / horaire compressé /
      // avenant / règlement de travail / licenciement / motif objectif
      // non extractibles du dossier salarié principal — déclaratifs
      // RH ou avocat).
      ['semaine-4-jours-be', {
        displayLabel: 'Semaine de 4 jours (Belgique)',
        component: Semaine4JoursBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-19b : Droit à la déconnexion BE — Loi du 03/10/2022
      // « Deal pour l'emploi » M.B. 10/11/2022, art. 16 (obligation
      // employeur ≥ 20 travailleurs de conclure une CCT d'entreprise
      // ou modifier le règlement de travail définissant les modalités
      // du droit à la déconnexion sur 3 thèmes : modalités pratiques,
      // sensibilisation/formation, modalités d'organisation du travail) ;
      // AR du 19/02/2023 (entrée en vigueur 01/04/2023) ; CCT n° 149
      // du Conseil national du travail (cadre supplétif) ; Loi du
      // 08/04/1965 art. 11-12 (procédure règlement de travail) ;
      // C. pén. social art. 137 (sanction de niveau 2). Outil BE-only —
      // le droit à la déconnexion français (C. trav. art. L. 2242-17, 7°
      // issu de la Loi n° 2016-1088 du 08/08/2016 dite « El Khomri »)
      // est un régime juridiquement distinct (obligation de négociation
      // annuelle dans les entreprises ≥ 50 salariés, sans obligation
      // de résultat). Aucune transposition mécanique.
      // Verdict hiérarchisé 6 états (HORS_CHAMP_SEUIL_NON_ATTEINT /
      // CONFORME_ACCORD_COMPLET / NON_CONFORME_CONTENU_INCOMPLET /
      // NON_CONFORME_INSTRUMENT_MANQUANT / MANQUEMENT_GRAVE_AUCUNE_INITIATIVE
      // / A_ANALYSER) — court-circuits backend (seuil < 20 → aucune
      // initiative → instrument manquant → contenu incomplet → conforme
      // / indéterminé) + ventilation 7 conformités cumulatives (seuil,
      // instrument formalisé, 3 thèmes art. 16 § 2, consultation,
      // contenu complet). ALWAYS_ON priority 137 (au-dessus de
      // semaine-4-jours-be = 136 SF-219-18b et clause-ecolage-be =
      // 135 SF-219-17b). Pré-fill IA V1 : aucun champ (alignement
      // pattern uniforme F-213/F-219 — effectif employeur, statut
      // accord, date entrée en vigueur, modalités pratiques /
      // sensibilisation / organisation, consultation organe
      // concertation, manquement signalé CBE/SPF non extractibles
      // du dossier salarié principal — déclaratifs RH ou avocat).
      ['droit-deconnexion-be', {
        displayLabel: 'Droit à la déconnexion (Belgique)',
        component: DroitDeconnexionBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-20b : pécule de vacances BE — Lois coordonnées du
      // 28/06/1971 relatives aux vacances annuelles des travailleurs
      // salariés (M.B. 30/09/1971), art. 3 (durée minimale 4 semaines
      // temps plein), art. 6 (double pécule), art. 7 (pécule de sortie)
      // + AR du 30/03/1967 (M.B. 06/04/1967), art. 9 (vacances jeunes),
      // art. 19 (pécule ouvrier 15,38 % de la rémunération brute
      // annuelle 108 % — liquidé par l'ONVA / Caisse de vacances
      // sectorielle), art. 38 (pécule employé — rémunération maintenue
      // + 85 % du salaire mensuel comme double pécule), art. 46 (pécule
      // de départ employé — 15,34 %) + Loi du 03/07/1978 art. 15
      // (prescription : 1 an post-contrat, 5 ans depuis le fait
      // générateur). Outil BE-only — l'indemnité compensatrice de
      // congés payés française (ICCP, C. trav. art. L. 3141-24 et s. —
      // 10 % de la rémunération brute totale ou maintien du salaire)
      // constitue un régime juridiquement distinct (pas de double
      // pécule, pas de débiteur tiers ONVA). Aucune transposition
      // mécanique. Verdict hiérarchisé 8 états
      // (DU_PECULE_SIMPLE_CALCULE / DU_DOUBLE_PECULE_CALCULE /
      // DU_PECULE_DEPART_CALCULE / NON_DU_OUVRIER_VIA_ONVA /
      // NON_DU_DEJA_PAYE / NON_DU_JOURS_INSUFFISANTS / PRESCRIT /
      // A_ANALYSER) — court-circuits backend (statut indéterminé →
      // déjà payé → ouvrier renvoyé ONVA → prescrit → jours insuffisants
      // → simple / double / départ liquidé) + ventilation des 3
      // montants théoriques. ALWAYS_ON priority 138 (au-dessus de
      // droit-deconnexion-be = 137 SF-219-19b parallèle et
      // semaine-4-jours-be = 136 SF-219-18b). Pré-fill IA V1 : aucun
      // champ (alignement pattern uniforme F-213/F-219 — statut
      // employé/ouvrier/jeune travailleur / type calcul / montants
      // rémunération exercice de vacances / jours conges pris / flag
      // pécule déjà payé / dates contentieuses non extractibles du
      // dossier salarié principal — déclaratifs RH ou avocat).
      ['pecule-vacances-be', {
        displayLabel: 'Pécule de vacances (Belgique)',
        component: PeculeVacancesBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-21b : éco-chèques + chèques-repas BE — CCT n°98 du CNT
      // du 20/02/2009 (M.B. 06/05/2009 — AR 12/04/2009), art. 3 (CCT
      // sectorielle ou d'entreprise ou convention individuelle écrite),
      // art. 4 (interdiction substitution rémunération), art. 6
      // (plafond 250 EUR/an), art. 13 (validité 24 mois) + Loi du
      // 25/04/2014 portant des dispositions diverses (M.B. 07/05/2014)
      // art. 51 (paiement électronique obligatoire chèques-repas depuis
      // 01/01/2016) + AR du 03/02/2010 modifiant l'art. 19bis de l'AR
      // du 28/11/1969 portant exécution de la loi du 27/06/1969 sur
      // la sécurité sociale (M.B. 17/02/2010), conditions cumulatives
      // d'exonération ONSS (1 chèque/jour effectivement presté,
      // nominatif, usage repas/denrées alimentaires uniquement,
      // intervention employeur max 6,91 EUR, contribution travailleur
      // min 1,09 EUR, valeur faciale max 8 EUR, validité 12 mois, pas
      // de cumul frais de bouche) + Cass. 16/10/2017 S.16.0042.F ONSS
      // c/ SA (interdiction substitution à rémunération préexistante —
      // requalification intégrale rétroactive). Outil BE-only — les
      // titres-restaurant français (CGI art. 81 19° ter) reposent sur
      // un mécanisme analogue mais avec plafond d'exonération distinct
      // (7,18 EUR/jour en 2024, contribution employeur 50-60 %) et
      // il n'existe pas d'équivalent direct des éco-chèques en droit
      // français. Aucune transposition mécanique. Verdict hiérarchisé
      // 6 états (CONFORME_EXONERATION_INTEGRALE /
      // CONFORME_PARTIELLEMENT_EXONERE / NON_CONFORME_DEPASSEMENT_PLAFOND
      // / NON_CONFORME_SUBSTITUTION_REMUNERATION /
      // NON_CONFORME_CONDITION_MANQUANTE / A_ANALYSER) — court-circuits
      // backend (type indéterminé → substitution → condition manquante
      // → dépassement plafond / partiellement exonéré → conforme
      // intégrale) + ventilation 4 montants (plafond légal, exonéré,
      // requalifié, cotisations ONSS estimées 25 % indicatif).
      // ALWAYS_ON priority 139 (au-dessus de pecule-vacances-be = 138
      // SF-219-20b et droit-deconnexion-be = 137 SF-219-19b). Pré-fill
      // IA V1 : aucun champ (alignement pattern uniforme F-213/F-219 —
      // type d'avantage / montants / jours prestés / flags juridiques
      // (CCT, convention individuelle, paiement électronique, cumul
      // frais de bouche, substitution rémunération) / date attribution
      // non extractibles du dossier salarié principal — déclaratifs
      // RH / fiches de paie / avocat).
      ['eco-cheques-cheques-repas-be', {
        displayLabel: 'Éco-chèques + chèques-repas (Belgique)',
        component: EcoChequesChequesRepasBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-22b : égalité salariale femmes / hommes BE — Loi du
      // 22/04/2012 visant à lutter contre l'écart salarial entre hommes
      // et femmes (M.B. 28/08/2012), art. 2 (obligation rapport biennal
      // d'analyse de la structure de rémunération H/F pour les
      // employeurs occupant en moyenne ≥ 50 travailleurs ETP, calcul
      // Loi 04/12/2007 art. 7), art. 5 (plan d'action concret avec
      // objectifs chiffrés et calendrier si écart non justifié constaté),
      // art. 12 (interdiction discrimination salariale), art. 14
      // (faculté médiateur dans l'entreprise) ; AR du 17/08/2013 portant
      // exécution de l'art. 2, art. 4 (ventilation art. 4 obligatoire :
      // niveau de fonction, ancienneté, qualification, régime de travail,
      // composants de la rémunération) ; AR du 25/04/2014 fixant les
      // formulaires standardisés (ann. I détaillé ≥ 100 ETP, ann. II
      // simplifié 50-99 ETP, dépôt CE / DS + dépôt central électronique
      // EBES auprès de la BNB) ; CCT n° 25 du 15/10/1975 du CNT sur
      // l'égalité de rémunération H/F (rendue obligatoire AR 09/12/1975,
      // modifiée CCT n° 25bis du 19/12/2001 + 25ter du 09/07/2008 —
      // transpose Directive 75/117/CEE puis 2006/54/CE) ; Loi du
      // 10/05/2007 anti-discrimination + Loi du 12/01/2007 gender
      // mainstreaming ; C. pén. social art. 195/1 (inséré par Loi
      // 22/04/2012 art. 16 — sanction niveau 2 : amende administrative
      // 80 à 800 € ou pénale 200 à 2 000 € par travailleur, plafonnée).
      // Outil BE-only — le régime français d'index égalité
      // professionnelle (C. trav. art. L. 1142-7 et s. ; Décret
      // n° 2019-15 du 08/01/2019) constitue un dispositif juridiquement
      // distinct (notation 100 points, plan de rattrapage si < 75/100,
      // publication obligatoire). Aucune transposition mécanique.
      // Verdict hiérarchisé 6 états (CONFORME_RAPPORT_PLAN_COMPLETS /
      // HORS_CHAMP_SEUIL_NON_ATTEINT / NON_CONFORME_RAPPORT_INCOMPLET /
      // NON_CONFORME_PLAN_ACTION_MANQUANT /
      // MANQUEMENT_GRAVE_RAPPORT_NON_DEPOSE / A_ANALYSER) —
      // court-circuits backend (statut indéterminé / en préparation →
      // seuil non atteint → délai dépassé → ventilation incomplète →
      // plan d'action manquant → conforme) + ventilation 5 conformités
      // cumulatives (seuil, rapport déposé, ventilation 5 sections,
      // plan d'action requis, plan d'action conforme) + indicateur
      // formulaire applicable (ann. I / II / NON_APPLICABLE).
      // ALWAYS_ON priority 140 (au-dessus de
      // eco-cheques-cheques-repas-be = 139 SF-219-21b parallèle,
      // pecule-vacances-be = 138 SF-219-20b et droit-deconnexion-be =
      // 137 SF-219-19b). Pré-fill IA V1 : aucun champ (alignement
      // pattern uniforme F-213 / F-219 — effectif moyen ETP entreprise,
      // statut rapport biennal, dates de dépôt, ventilations art. 4,
      // écart salarial constaté, plan d'action, médiateur, plainte
      // IEFH non extractibles du dossier salarié individuel —
      // déclaratifs RH employeur ou avocat post-instruction).
      ['egalite-femmes-hommes-be', {
        displayLabel: 'Égalité salariale F/H (Belgique)',
        component: EgaliteFemmesHommesBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-23b : refus d'aménagements raisonnables handicap BE — Loi du
      // 10/05/2007 tendant à lutter contre certaines formes de
      // discrimination (M.B. 30/05/2007), art. 4 4° (notion fonctionnelle
      // de handicap transposant la jurisprudence CJUE 11/04/2013 C-335/11
      // et C-337/11 HK Danmark / Ring et Werge), art. 14 (interdiction
      // discrimination indirecte par défaut d'aménagement raisonnable),
      // art. 17 (présomption de représailles renversant la charge de la
      // preuve), art. 28 (sanctions civiles — indemnité forfaitaire 6 mois
      // ou préjudice réel + § 3 renversement charge de la preuve devant
      // le juge) ; CCT n° 95 du 10/10/2008 du CNT (NAR) sur l'égalité
      // de traitement durant toutes les phases de la relation de travail ;
      // Directive 2000/78/CE art. 5 (cadre général égalité de traitement,
      // obligation d'aménagement raisonnable) ; Convention ONU 13/12/2006
      // relative aux droits des personnes handicapées art. 5 (égalité et
      // non-discrimination) et art. 27 (travail et emploi) ; Convention
      // OIT n° 159 sur la réadaptation professionnelle et l'emploi des
      // personnes handicapées (1983). Outil BE-only — le régime français
      // d'OETH (C. trav. art. L. 5213-1 et s. + L. 5213-6 obligation
      // d'aménagement raisonnable + L. 1132-1 prohibition de la
      // discrimination + art. R. 5213-32 et s.) constitue un dispositif
      // juridiquement distinct (taux d'emploi 6 %, contribution AGEFIPH,
      // indemnités L. 1226-14 inaptitude d'origine professionnelle).
      // Aucune transposition mécanique. Verdict hiérarchisé 7 états
      // (CONFORME_AMENAGEMENT_ACCORDE / CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE
      // / DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE / DISCRIMINATION_PRESUMEE_NON_MOTIVATION
      // / REPRESAILLES_PRESUMEES_LICENCIEMENT / FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE
      // / A_ANALYSER) — court-circuits backend (statut handicap indéterminé /
      // contesté → qualification fragile ; sanction post-demande → représailles ;
      // refus sans motivation → présomption ; refus motivé sans subsides ni
      // alternatives → discrimination indirecte ; charge disproportionnée
      // démontrée → conforme refus ; aménagement accordé → conforme) +
      // ventilation 5 conformités cumulatives (qualification handicap,
      // demande formalisée, refus caractérisé, charge disproportionnée
      // démontrée, représailles présumées) + indemnité forfaitaire 6 mois
      // indicative (art. 28 § 2 1°). ALWAYS_ON priority 141 (au-dessus de
      // egalite-femmes-hommes-be = 140 SF-219-22b, eco-cheques-cheques-repas-be
      // = 139 SF-219-21b, pecule-vacances-be = 138 SF-219-20b et
      // droit-deconnexion-be = 137 SF-219-19b). Pré-fill IA V1 : aucun champ
      // (alignement pattern uniforme F-213 / F-219 — statut handicap,
      // demande, type d'aménagement, coût, subsides, effectif, CA, réponse
      // employeur, motivation détaillée, avis SEPP, charge disproportionnée
      // invoquée, devis, mesures alternatives, sanction, salaire, procédure
      // UNIA non extractibles du dossier salarié individuel — déclaratifs
      // RH employeur / médecin du travail SEPP / avocat post-instruction).
      ['discrimination-be-handicap-amenagement', {
        displayLabel: 'Aménagements raisonnables handicap (Belgique)',
        component: DiscriminationBeHandicapAmenagementSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-24b : Code pénal social BE — qualification d'infraction
      // sociale (13 types ou AUTRE_QUALIFICATION) + restitution du
      // niveau de sanction (1 à 4 — art. 101-103 C. pén. soc., Loi du
      // 06/06/2010). Restitue les bornes d'amende administrative et
      // pénale (valeurs de base non indexées), emprisonnement niveau 4
      // (6 mois à 3 ans), majorations × travailleurs (art. 103 § 2),
      // × 5 personne morale (art. 105), × 2 récidive ≤ 1 an (art. 110).
      // ALWAYS_ON priority 142 (au-dessus de
      // discrimination-be-handicap-amenagement = 141 SF-219-23b,
      // egalite-femmes-hommes-be = 140 SF-219-22b,
      // eco-cheques-cheques-repas-be = 139 SF-219-21b). Pré-fill IA V1 :
      // aucun champ (alignement pattern uniforme F-213 / F-219 — type
      // infraction, niveau, date faits, nombre travailleurs, personne
      // morale, récidive, qualité auteur, élément moral non extractibles
      // du dossier salarié individuel — relèvent du procès-verbal
      // d'inspection sociale / dossier auditorat / analyse pénale
      // avocat post-instruction).
      ['code-penal-social-be', {
        displayLabel: 'Code pénal social (Belgique)',
        component: CodePenalSocialBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-25b : outil décisionnel Auditorat du travail BE
      // (Code judiciaire art. 138bis + Code d'instruction criminelle
      // art. 24 + Loi du 03/08/1992 sur le Code judiciaire + Loi du
      // 06/06/2010 introduisant le Code pénal social). Outil
      // d'orientation et de checklist de saisine du parquet
      // spécialisé en droit social pénal. Verdicts :
      // SAISINE_AUDITORAT_RECOMMANDEE (infraction pénale sociale
      // caractérisée, accident grave, harcèlement pénal art. 442bis
      // C. pén., discrimination pénale, entrave inspection),
      // DENONCIATION_INSPECTION_PREALABLE (travail non déclaré
      // suspecté, PV transmis art. 76 C. pén. soc.),
      // SAISINE_NON_PERTINENTE (litige civil pur art. 578 C. jud.
      // ou faits prescrits art. 81 C. pén. soc.), A_QUALIFIER
      // (nature ouverte ou pluri-qualifications à arbitrer).
      // BE uniquement (la France n'a pas d'auditorat du travail
      // équivalent — PV inspection au procureur sur
      // art. L. 8112-1 et s. C. trav. FR).
      // Pré-fill IA V1 : aucun champ (alignement pattern uniforme
      // F-213 / F-219 — nature des faits, mode de saisine,
      // prescription, urgence, recours pénal, qualité employeur non
      // extractibles du dossier salarié individuel — relèvent de
      // l'analyse stratégique avocat et du dossier d'instruction).
      ['auditorat-travail-be', {
        displayLabel: 'Auditorat du travail (Belgique)',
        component: AuditoratTravailBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-26b : Travail noir BE — DIMONA, requalification et
      // sanctions (Loi-programme 24/12/2002 art. 167-184 + AR 05/11/2002
      // + Code pénal social art. 181 niveau 4 + Loi 22/04/2003 art. 28
      // amende ONSS forfaitaire 3 ×). Analyse 6 verdicts (DIMONA_CONFORME
      // / TARDIVE_REGULARISABLE / ABSENCE_NIVEAU_4 / REQUALIFICATION_
      // PRESUMEE / INDEPENDANT_REQUALIFIE / A_QUALIFIER) avec calcul
      // cotisations ONSS rétroactives (employeur ~25 % + travailleur
      // 13,07 %), amende ONSS forfaitaire 3 × cotisations dues, sanction
      // pénale art. 181 niveau 4 (300/600 - 3000/6000 €, emprisonnement
      // 6-36 mois) + présomption salariat art. 328, requalification faux
      // indépendant Loi-programme I 27/12/2006 art. 333. ALWAYS_ON
      // priority 144 (au-dessus de auditorat-travail-be = 143 SF-219-25b,
      // code-penal-social-be = 142 SF-219-24b, discrimination-be-handicap-
      // amenagement = 141 SF-219-23b). Pré-fill IA V1 : aucun champ
      // (alignement pattern uniforme F-213 / F-219 — statut DIMONA,
      // dates, salaire, nombre travailleurs, personne morale, récidive,
      // éléments subordination non extractibles du dossier salarié —
      // relèvent du procès-verbal d'inspection sociale / extrait ONSS /
      // audition travailleur).
      ['travail-noir-be-dimona', {
        displayLabel: 'Travail noir DIMONA (Belgique)',
        component: TravailNoirBeDimonaSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-27b : INASTI statut travailleur indépendant BE — Loi
      // 27/06/1969 + AR n° 38 du 27/07/1967 + AR du 19/12/1967 +
      // Loi-programme I du 27/12/2006 art. 328 à 333 « doctrine Bart
      // Buysse » (4 critères généraux + présomption simple de salariat
      // art. 328 § 1) + art. 337/1 à 337/3 critères sectoriels (Loi
      // 25/08/2012, extension agriculture / horticulture AR 29/10/2013 ;
      // Cour const. arrêt 167/2013 du 19/12/2013). 5 verdicts
      // (INDEPENDANT_CONFIRME / SALARIE_REQUALIFIE / FAUX_INDEPENDANT_
      // PRESUMPTION_SECTORIELLE / PRESUMPTION_GENERALE_SALARIAT /
      // A_QUALIFIER) avec scores critères généraux 4/4 + critères
      // sectoriels 9/9 + flags présomption sectorielle / générale,
      // cohérence DIMONA / statut déclaré et indice mono-client (Cass.
      // BE 23/12/2002 S.02.0021.F). ALWAYS_ON priority 145 (au-dessus
      // de travail-noir-be-dimona = 144 SF-219-26b, auditorat-travail-
      // be = 143 SF-219-25b, code-penal-social-be = 142 SF-219-24b).
      // Pré-fill IA V1 : aucun champ (alignement pattern uniforme F-213
      // / F-219 — volonté des parties, modalités d'exécution, critères
      // de subordination, secteur, statut administratif, présence
      // d'autres clients non extractibles du dossier salarié — relèvent
      // de l'analyse contractuelle in concreto par l'avocat).
      ['inastri-statut-travailleur-independant', {
        displayLabel: 'INASTI — statut travailleur indépendant (Belgique)',
        component: InastriStatutTravailleurIndependantSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-28b : MP Fedris reconnaissance — Lois coordonnees du
      // 03/06/1970 + AR 28/03/1969 liste fermee modifie 06/12/2018 +
      // AR 16/12/1985 systeme ouvert + Loi 11/01/2018 reformant Fedris.
      // Analyse 6 verdicts (MALADIE_LISTE_FERMEE_PRESOMPTION art. 32 /
      // MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE / SYSTEME_OUVERT_
      // CAUSALITE_DIRECTE_DETERMINANTE art. 30bis / SYSTEME_OUVERT_
      // CAUSALITE_INSUFFISANTE / DECLARATION_PRESCRITE prescription
      // triennale art. 31 / A_QUALIFIER) avec calcul de la prescription
      // triennale a compter de la connaissance du caractere
      // professionnel. ALWAYS_ON priority 146 (au-dessus de
      // inastri-statut-travailleur-independant = 145 SF-219-27b,
      // travail-noir-be-dimona = 144 SF-219-26b, auditorat-travail-be =
      // 143 SF-219-25b). Pre-fill IA V1 : aucun champ (alignement
      // pattern uniforme F-213 / F-219 — typeMaladie, codeMaladieListe,
      // libelleMaladie, dates, exposition, causalite non extractibles
      // du dossier salarie generique — relevent du rapport medical
      // specialiste, du CV professionnel, du carnet de surveillance
      // sante AR 28/05/2003, et de la nomenclature AR 28/03/1969).
      ['mp-fedris-reconnaissance', {
        displayLabel: 'MP Fedris reconnaissance (Belgique)',
        component: MpFedrisReconnaissanceSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-29b : Rente AT/MP vs capitalisation BE — Loi du
      // 10/04/1971 art. 24 (capital forfaitaire IPP -19 / rente
      // annuelle viagere IPP +19) + Lois coordonnees du 03/06/1970
      // art. 35 (renvoi MP au regime AT) + AR du 21/12/1971 + AR du
      // 10/12/1987 (conversion partielle 1/3 max apres 3 ans) + AR
      // du 24/02/2005 (bareme et coefficients d'age Table I-bis).
      // Calculateur d'indemnite forfaitaire d'incapacite permanente
      // partielle post-consolidation avec 5 verdicts
      // (CAPITAL_FORFAITAIRE_LT_19 / RENTE_ANNUELLE_GE_19 /
      // INELIGIBLE_NON_RECONNU / IPP_NON_DETERMINE / A_QUALIFIER).
      // ALWAYS_ON priority 147 (au-dessus de mp-fedris-reconnaissance
      // = 146 SF-219-28b — chaine logique : reconnaissance MP en
      // amont via SF-219-28b puis chiffrage des indemnites post-
      // consolidation ici). Pre-fill IA V1 : aucun champ (alignement
      // pattern uniforme F-213 / F-219 — origine AT/MP, statut Fedris,
      // dates medicales, taux IPP, remuneration de base art. 34
      // plafonnee art. 39, date de naissance, conversion partielle
      // non extractibles du dossier salarie generique).
      ['at-mp-rente-capital-be', {
        displayLabel: 'Rente AT/MP vs capitalisation (Belgique)',
        component: AtMpRenteCapitalBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-30b : Saisine du Conseiller en Prevention Aspects
      // Psychosociaux (CPAP) — procedure RPS interne BE. BE-only,
      // ALWAYS_ON priority 148 (au-dessus de at-mp-rente-capital-be
      // = 147 SF-219-29b, mp-fedris-reconnaissance = 146 SF-219-28b).
      // Verdict 8 etats (SAISINE_CONFORME info /
      // SAISINE_INFORMELLE_EN_COURS neutral / SAISINE_FORMELLE_EN_COURS
      // info / AVIS_RENDU_DELAI_RESPECTE info / AVIS_RENDU_DELAI_DEPASSE
      // warn / NON_CONFORME_FORMALITES_MANQUANTES critical /
      // NON_CONFORME_PAS_DE_CONSEILLER critical / A_QUALIFIER neutral)
      // avec calcul des echeances 3 mois enquete art. 22 § 1 / 2 mois
      // mesures employeur art. 32 / protection 12 mois art. 32sexies.
      // Pre-fill IA V1 : aucun champ (alignement pattern uniforme
      // F-213 / F-219 — typeRisque, modeDemande, etapeProcedure, 5
      // formalites booleennes, dates depot et avis non extractibles
      // du dossier salarie generique — relevent du dossier procedural
      // specifique CPAP : compte-rendu entretien prealable, formulaire
      // de demande, accuse de reception CPAP, notification employeur).
      ['bien-etre-rps-conseiller-prevention', {
        displayLabel: 'Saisine CPAP — Conseiller en Prevention RPS (Belgique)',
        component: BienEtreRpsConseillerPreventionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-31b : Conge paternite / naissance BE — Loi du
      // 03/07/1978 art. 30 paragr. 2 (10 / 15 / 20 jours ouvrables
      // selon date de naissance, dans les 4 mois post-naissance) +
      // Loi du 12/08/2000 + Loi du 07/04/2023 Deal pour l'emploi
      // (extension a tous les co-parents, alignement 20 jours depuis
      // 01/01/2023) + AR du 17/10/1994 (modalites d'information et
      // de prise) + AR du 03/07/1996 (indemnisation INAMI : 3
      // premiers jours employeur 100 pour cent, jours restants
      // mutuelle 82 pour cent plafonnee) + protection 5 mois
      // licenciement art. 30 paragr. 4. Verdict 8 etats
      // (ELIGIBLE_CONGE_OUVERT info / CONGE_EN_COURS_PROTECTION_ACTIVE
      // info / CONGE_PRIS_PROTECTION_RESIDUELLE info /
      // INELIGIBLE_STATUT_NON_COUVERT critical /
      // INELIGIBLE_FILIATION_NON_ETABLIE critical /
      // DROIT_PERDU_DELAI_DEPASSE critical /
      // INELIGIBLE_NAISSANCE_FUTURE warn / A_QUALIFIER neutral).
      // ALWAYS_ON priority 149 (au-dessus de
      // bien-etre-rps-conseiller-prevention 148 SF-219-30b,
      // at-mp-rente-capital-be 147 SF-219-29b,
      // mp-fedris-reconnaissance 146 SF-219-28b). Pre-fill IA V1 :
      // aucun champ (alignement pattern uniforme F-213 / F-219 —
      // statut travailleur, lien filiation, etape RH, dates etat-civil
      // et formalites RH non extractibles du dossier salarie
      // generique). THEME VALIDITE (qualification de droit / verdict
      // 8 etats, coherent avec les autres outils de qualification du
      // panel BE : mp-fedris-reconnaissance,
      // bien-etre-rps-conseiller-prevention, protection-grossesse).
      ['conge-paternite-naissance-be', {
        displayLabel: 'Conge paternite / naissance (Belgique)',
        component: CongePaterniteNaissanceBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-219-32b : Interruption de carriere pour conge parental BE —
      // Loi de redressement du 22/01/1985 art. 99 a 107quater (cadre
      // general + protection licenciement art. 101) + AR du 29/10/1997
      // (conditions d'eligibilite, formes 4 / 8 / 20 mois, formalisme
      // notification 2-3 mois) + CCT n 64 du 29/04/1997 (rendant le
      // droit obligatoire dans le secteur prive) + AR du 12/08/1991
      // (allocations forfaitaires ONEM) + AR du 12/12/2001 (cinquieme
      // temps). BE-only, ALWAYS_ON priority 150 (au-dessus de
      // conge-paternite-naissance-be = 149 SF-219-31b,
      // bien-etre-rps-conseiller-prevention = 148 SF-219-30b,
      // at-mp-rente-capital-be = 147 SF-219-29b). Verdict 8 etats
      // (ELIGIBLE_COMPLET info / ELIGIBLE_AVEC_RESERVES neutral /
      // INELIGIBLE_ANCIENNETE critical / INELIGIBLE_AGE_ENFANT critical /
      // INELIGIBLE_SOLDE_INSUFFISANT critical / INELIGIBLE_FORMALISME
      // warn / DIFFERE_EMPLOYEUR warn / A_QUALIFIER neutral) avec calcul
      // de la duree (4 / 8 / 20 mois), de l'allocation ONEM, de la date
      // de fin, de la protection art. 101 et de l'indemnite forfaitaire
      // 6 mois. Distinct de F-DT-29 credit-temps-be (CCT 103 regime
      // universel sans motif specifique) — cet outil couvre uniquement
      // le conge parental Loi 22/01/1985 et CCT 64 (droit individuel
      // par enfant et par parent avec conditions d'age enfant et formes
      // specifiques). Pre-fill IA V1 : aucun champ (alignement pattern
      // uniforme F-213 / F-219 — forme, anciennete (periode reference
      // 15 mois), age enfant, handicap, solde ONEM, mode notification,
      // accord et differe employeur, cumul ONEM, dates et remuneration
      // art. 101 non extractibles du dossier salarie generique).
      ['interruption-carriere-soins-parental', {
        displayLabel: 'Interruption de carriere — Conge parental (Belgique)',
        component: InterruptionCarriereSoinsParentalSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-DT-28-avantages-conventionnels-be', {
        displayLabel: 'Avantages conventionnels (Belgique)',
        component: AvantagesConventionnelsBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-28-02 : pré-fill IA (salaireBrutMensuel) + validation
          // F-IA-03 (1 champ SALAIRE). Autres champs non extraits par
          // le prompt IA travail actuel.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      // SF-DT-29-02 : crédit-temps / interruption de carrière BE
      // (CCT 103 + AR 29/10/1997). BE uniquement.
      ['F-DT-29-credit-temps-be', {
        displayLabel: 'Crédit-temps (Belgique)',
        component: CreditTempsBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // Pré-fill IA (dateEntree → ancienneteEntrepriseMois,
          // ageDemandeurAnnees) + validation F-IA-03 (2 champs :
          // ANCIENNETE, AGE) multi-sources IA / F96 / QUESTION_IA /
          // PIECE_MANQUANTE.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      // SF-DT-14-02 : PSE — critères de validité (FR uniquement,
      // L.1233-24-1 + L.1233-30 + L.1233-57-2 + L.1233-61 + L.1235-7-1).
      // tool_id aligné avec la migration 164.
      ['F-DT-14-pse-validite', {
        displayLabel: 'PSE — validité (FR)',
        component: PseSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // Pré-fill IA gracieux (dateLicenciement → dateProjet)
          // + validation F-IA-03 (1 champ DATE_PROJET multi-sources
          // IA / F96 / QUESTION_IA / PIECE_MANQUANTE).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-DT-30-02 : Protection des représentants du personnel (FR uniquement,
      // art. L.2411-1 + L.2411-3 + L.2411-22 + L.2422-1 + R.2422-1 CT).
      // tool_id aligné avec la migration 166.
      ['F-DT-30-protection-rp', {
        displayLabel: 'Protection représentant du personnel (FR)',
        component: ProtectionRpSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // Pré-fill IA gracieux (motifLicenciement → motifLicenciement
          // mappé vers enum backend) + validation F-IA-03 (1 champ
          // MOTIF_LICENCIEMENT multi-sources IA / F96 / QUESTION_IA /
          // PIECE_MANQUANTE).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      // SF-DT-33-02 : Accident du travail / Maladie professionnelle (FR uniquement,
      // CSS L.411-1 / L.461-1 / L.434-2 + L.142-2 + R.142-1 + R.441-13 + R.461-9).
      // 3 dispositifs : RECONNAISSANCE_AT / RECONNAISSANCE_MP / CONTESTATION_TAUX_IPP.
      // tool_id aligné avec la migration 175.
      ['F-DT-33-at-mp', {
        displayLabel: 'AT/MP — accident et maladie pro (FR)',
        component: AtMpSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // Pré-fill IA gracieux (dateLicenciement → dateAccident proxy
          // RECONNAISSANCE_AT seul — pipeline IA n'extrait pas encore une
          // dateAccident dédiée) + validation F-IA-03 (1 champ DATE_ACCIDENT
          // multi-sources IA / F96 / QUESTION_IA / PIECE_MANQUANTE).
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-IM-14-40ter-familial-belge-be', {
        displayLabel: 'Article 40ter — regroupement familial belge (Belgique)',
        component: Belgian40terSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-IM-14-08 : pré-fill IA gracieux (lienFamilial, regroupantBelge,
          // revenusNetsMensuels, dateDepotDemande) + validation F-IA-03.
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-IM-14-9bis-humanitaire-be', {
        displayLabel: 'Article 9bis — humanitaire (Belgique)',
        component: Belgian9bisSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-IM-14-9ter-medical-be', {
        displayLabel: 'Article 9ter — médical (Belgique)',
        component: Belgian9terSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-IM-14-40bis-cohabitant-ue-be', {
        displayLabel: 'Article 40bis — cohabitant UE (Belgique)',
        component: BelgianCohabitantUeBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-IM-09-aes-metiers-tension', {
        displayLabel: 'AES métiers en tension (FR)',
        component: AesMetiersTensionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-IM-09-aes-famille', {
        displayLabel: 'AES familial (FR)',
        component: AesFamilleSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-FA-08-divorce-alteration', {
        displayLabel: 'Divorce — altération du lien (FR)',
        component: DivorceAlterationSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02c — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // F-236 SF-236-03 : F-FA-09 est un outil **famille** — son `aiData` doit
      // pointer sur `familleExtractedData` (et non `travailExtractedData`,
      // erreur de mapping héritée). Cf. mini-spec F-236 § "F-FA-09 hotfix".
      ['F-FA-09-divorce-faute', {
        displayLabel: 'Divorce pour faute (FR)',
        component: DivorceFauteSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          // F-163 SF-163-02c — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-FA-10-divorce-accepte', {
        displayLabel: 'Divorce accepté (FR)',
        component: DivorceAccepteSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02c — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-FA-12-mesures-provisoires', {
        displayLabel: 'Mesures provisoires (FR)',
        component: MesuresProvisoiresSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-FA-13-revisions-post-divorce', {
        displayLabel: 'Révisions post-divorce (FR)',
        component: RevisionsPostDivorceSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02c — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-FA-14-02 : ordonnance de protection FR (art. 515-9 Cciv).
      ['F-FA-14-ordonnance-protection', {
        displayLabel: 'Ordonnance de protection (FR)',
        component: OrdonnanceProtectionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02c — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-136-travail-procedure', {
        displayLabel: 'Procédure prud\'homale (FR)',
        component: TravailProcedureSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
        }),
      }],
      ['F-FA-19-autorite-parentale', {
        displayLabel: 'Autorité parentale (FR)',
        component: AutoriteParentaleSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02c — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-FA-19-changement-residence', {
        displayLabel: 'Changement de résidence de l\'enfant (FR)',
        component: ChangementResidenceSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02c — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-FA-19-desaccords-parentaux', {
        displayLabel: 'Désaccords parentaux (FR)',
        component: DesaccordsParentauxSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02c — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-FA-22-indivision', {
        displayLabel: 'Indivision (FR)',
        component: IndivisionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-FA-20-pacs-dissolution', {
        displayLabel: 'Dissolution de PACS (FR)',
        component: PacsDissolutionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-25-02 : majeurs protégés FR (art. 425-494 / 494-1 Cciv).
      ['F-FA-25-majeurs-proteges', {
        displayLabel: 'Majeurs protégés (FR)',
        component: MajeursProtegesSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-26-02 : changement d'état civil FR (art. 60 / 61-1 / 61-5 Cciv).
      ['F-FA-26-changement-etat-civil', {
        displayLabel: 'Changement d\'état civil (FR)',
        component: ChangementEtatCivilSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02c — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-FA-21-02 : séparation de corps + conversion divorce FR (art. 296+306 Cciv).
      ['F-FA-21-separation-corps', {
        displayLabel: 'Séparation de corps (FR)',
        component: SeparationCorpsSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02c — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-IM-11-02 : changement de statut CESEDA (FR uniquement,
      // art. L.421+ + R.5221). tool_id aligné migration 170.
      ['F-IM-11-changement-statut', {
        displayLabel: 'Changement de statut (FR)',
        component: ChangementStatutSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-IM-13-02 : naturalisation française FR — 6 voies Cciv 21-15+.
      ['F-IM-13-naturalisation', {
        displayLabel: 'Naturalisation (FR)',
        component: NaturalisationSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-IM-19-02 : mineurs étrangers FR — MNA / L.435-3 / DCEM / TIR.
      ['F-IM-19-mineurs', {
        displayLabel: 'Mineurs immigration (FR)',
        component: MineursImmigrationSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-IM-20-02 : mesures d'éloignement avancées FR — Expulsion + IRTF + IAT.
      ['F-IM-20-mesures-eloignement', {
        displayLabel: 'Mesures d\'éloignement (FR)',
        component: MesuresEloignementSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-IM-12-02 : asile avancé FR — Dublin III / accélérée / réexamen / apatridie / PS.
      ['F-IM-12-asile-avance', {
        displayLabel: 'Asile avancé (FR)',
        component: AsileAvanceSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-FA-17-02 : partage judiciaire FR (art. 840+ Cciv + 1364+ + 1366 CPC).
      // tool_id aligné avec la migration 169.
      ['F-FA-17-partage-judiciaire', {
        displayLabel: 'Partage judiciaire (FR)',
        component: PartageJudiciaireSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-18-02 : reconnaissance paternelle FR (art. 316 + 332-335 + 372 Cciv).
      ['F-FA-18-reconnaissance-paternelle', {
        displayLabel: 'Reconnaissance paternelle (FR)',
        component: ReconnaissancePaternelleSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-18-04 : contestation de paternité FR (art. 332-335 + 311-1 + 321 + 372 Cciv).
      // tool_id aligné avec la migration 181.
      ['F-FA-18-contestation-paternite', {
        displayLabel: 'Contestation de paternité (FR)',
        component: ContestationPaterniteSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-18-06 : action en recherche de paternité FR
      // (art. 327 + 340 + 16-11 + 321 Cciv). tool_id aligné avec la migration 183.
      ['F-FA-18-recherche-paternite', {
        displayLabel: 'Recherche de paternité (FR)',
        component: RecherchePaterniteSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-18-08 : possession d'état FR (art. 311-1 + 311-2 + 317 Cciv).
      // tool_id aligné avec la migration 185.
      ['F-FA-18-possession-etat', {
        displayLabel: 'Possession d\'état (FR)',
        component: PossessionEtatSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // Pré-fill IA gracieux via possessionEtatConforme5AnsDetected
          // (faisceau cardinal "conforme 5 ans") + validation F-IA-03
          // (5 alertes sur tractatus / fama / continue / paisible / nonEquivoque).
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-18-10 : adoption FR (art. 343-370-2 Cciv) — plénière / simple.
      // Scoring niveau 5 (recevabilité) + bascule plénière → simple.
      // FR uniquement (BE = feature jumelle au backlog).
      ['F-FA-18-adoption', {
        displayLabel: 'Adoption (FR)',
        component: AdoptionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // Pré-fill IA via formeAdoptionDemandeeDetected / pupilleEtatDetected
          // / adoptantMarieDetected / ageAdoptantDetecte / ageAdopteDetecte.
          // Validation F-IA-03 sur 5 fields (FORME_ADOPTION / PUPILLE_ETAT /
          // ADOPTANT_MARIE / AGE_ADOPTANT / AGE_ADOPTE).
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-16-02 : communauté universelle FR (art. 1526 + 1527 al. 2 Cciv).
      ['F-FA-16-communaute-universelle', {
        displayLabel: 'Communauté universelle (FR)',
        component: CommunauteUniverselleSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-23-02 : ordonnance sur requête (mesures urgentes familiales)
      // FR + BE actifs (art. 493 + 497 CPC FR / art. 1025 et s. CJ BE).
      ['F-FA-23-ordonnance-requete', {
        displayLabel: 'Ordonnance sur requête',
        component: OrdonnanceRequeteSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-24-02 : dévolution légale successorale FR (art. 731 et s. Cciv).
      // tool_id aligné avec la migration 179 (visibility ALWAYS_ON
      // DROIT_FAMILLE FRANCE priority 88).
      ['F-FA-24-devolution-legale', {
        displayLabel: 'Dévolution légale (FR)',
        component: DevolutionLegaleSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-27-02 : PMA / GPA / bioéthique FR.
      ['F-FA-27-pma-gpa', {
        displayLabel: 'PMA / GPA — bioéthique (FR)',
        component: PmaGpaBioethiqueSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-24-04 : validité testament FR.
      ['F-FA-24-testament-validite', {
        displayLabel: 'Testament — validité (FR)',
        component: TestamentValiditeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-24-06 : donation entre vifs FR.
      ['F-FA-24-donation', {
        displayLabel: 'Donation (FR)',
        component: DonationSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-24-08 : réserve héréditaire FR.
      ['F-FA-24-reserve-heriditaire', {
        displayLabel: 'Réserve héréditaire (FR)',
        component: ReserveHeriditaireSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02c — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-FA-24-10 : partage successoral FR.
      ['F-FA-24-partage-successoral', {
        displayLabel: 'Partage successoral (FR)',
        component: PartageSuccessoralSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-24-12 : indivision successorale FR.
      ['F-FA-24-indivision-successorale', {
        displayLabel: 'Indivision successorale (FR)',
        component: IndivisionSuccessoraleSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-24-14 : rapport à succession FR (art. 843-863 + 919 Cciv).
      ['F-FA-24-rapport-succession', {
        displayLabel: 'Rapport à succession (FR)',
        component: RapportSuccessionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02c — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // SF-IM-17-02 : régime algérien FR.
      ['F-IM-17-regime-algerien', {
        displayLabel: 'Régime algérien (accord franco-algérien)',
        component: RegimeAlgerienSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // ===================================================================
      // SF-164-01 : entrées rétroactives pour composants existants seedés
      // en DB sans entrée registry (régression silencieuse vague 2026-04-24).
      // ===================================================================
      ['F-DT-03-prescription-litige', {
        displayLabel: 'Prescription du litige (FR)',
        component: CaseDeadlinesSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
        }),
      }],
      ['F-DT-31-transaction', {
        displayLabel: 'Transaction (FR)',
        component: TransactionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
                // F-163 SF-163-02b — propage le flag standalone (default false).
        standaloneMode: ctx.standaloneMode ?? false,
}),
      }],
      ['F-IM-09-aes-etudiant', {
        displayLabel: 'AES étudiant (FR)',
        component: AesEtudiantSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-IM-09-aes-humanitaire', {
        displayLabel: 'AES humanitaire (FR)',
        component: AesHumanitaireSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02d — propage le flag standalone.
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      ['F-FA-11-desunion-irremediable-be', {
        displayLabel: 'Désunion irrémédiable (Belgique)',
        component: DivorceDesunionBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          // F-163 SF-163-02c — propage le flag standalone (default false).
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // F-210 SF-210-02 : médiation familiale obligatoire pré-saisine JAF (FR).
      // Migration 218 — CONTEXTUAL trigger `mediation_familiale_pre_saisine_pertinente=true`.
      ['mediation-familiale-pre-saisine', {
        displayLabel: 'Médiation familiale pré-saisine',
        component: MediationFamilialeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // F-210 SF-210-04 : acceptation / renonciation succession (FR — art. 768+).
      // Migration 219 — CONTEXTUAL trigger `succession_envisagee=true` (flag pivot F-200).
      ['acceptation-renonciation-succession', {
        displayLabel: 'Acceptation / renonciation à succession',
        component: AcceptationRenonciationSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // ===================================================================
      // F-211 SF-211-05 : 4 wrappers informationnels Famille BE (V1).
      // Backend mergé PR #942 (SF-211-01/02/03/04) — endpoints opérationnels.
      // Composants de saisie complets à livrer en SF ultérieures.
      // ===================================================================
      // F-211 SF-211-01 : divorce par consentement mutuel BE — CJ 1287+.
      // Migration 228 — CONTEXTUAL trigger `divorce_dc_envisage=true`.
      ['divorce-dc-be', {
        displayLabel: 'Divorce par consentement mutuel (Belgique)',
        component: DivorceDcBeSectionComponent,
        // F-243 : composant complet (form + pré-fill IA + validation F-IA-03).
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // F-211 SF-211-02 : divorce désunion irrémédiable orientation 3 voies BE — CC 229 §§1/2/3.
      // Migration 228 — CONTEXTUAL trigger `divorce_ddi_envisage=true`.
      ['divorce-ddi-3voies-be', {
        displayLabel: 'Divorce — Désunion irrémédiable 3 voies (Belgique)',
        component: DivorceDdiBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
        }),
      }],
      // F-211 SF-211-03 : mesures provisoires Tribunal de la famille BE — CJ 1280.
      // Migration 228 — ALWAYS_ON (mesures urgentes transversales).
      ['tribunal-famille-be-mesures-prov', {
        displayLabel: 'Mesures provisoires Tribunal de la famille (Belgique)',
        component: TribunalFamilleBeMesuresProvisoiresSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
        }),
      }],
      // F-211 SF-211-04 : pacte successoral 2018 BE — Loi 31/07/2017.
      // Migration 228 — CONTEXTUAL trigger `pacte_successoral_envisage=true`.
      ['pacte-successoral-be-2018', {
        displayLabel: 'Pacte successoral 2018 (Belgique)',
        component: PacteSuccessoralBe2018SectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
        }),
      }],
      // F-217 SF-217-03 : régime de communauté légale BE — CC Livre 3, loi 22/07/2018.
      // Migration 235 — ALWAYS_ON (toute analyse d'un dossier de couple marié
      // belge mobilise la qualification du régime). Backend SF-217-01 (PR #983).
      ['regime-mat-be-communaute-legale', {
        displayLabel: 'Régime de communauté légale (Belgique)',
        component: RegimeCommunauteLegaleBeSectionComponent,
        // Composant complet (form + listes dynamiques + validation F-IA-03).
        // Pré-fill IA V1 = 0 champ (PREFILL_COUNT_ALWAYS_ZERO).
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // F-217 SF-217-03 : liquidation-partage post-divorce BE — CJ art. 1207+ / 1218.
      // Migration 235 — ALWAYS_ON (toute dissolution du couple appelle une
      // liquidation-partage du patrimoine). Backend SF-217-02 (PR #982).
      ['liquidation-partage-be', {
        displayLabel: 'Liquidation-partage post-divorce (Belgique)',
        component: LiquidationPartageBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // F-217 SF-217-05 : autorité parentale BE — CC art. 374-375.
      // Migration 238 — ALWAYS_ON (question systématique de tout dossier
      // Famille BE avec enfant). Backend SF-217-04 (PR #993).
      ['autorite-parentale-be', {
        displayLabel: 'Autorité parentale (Belgique)',
        component: AutoriteParentaleBeSectionComponent,
        // Composant complet (form + verdict + voie procédurale).
        // Pré-fill IA V1 = 0 champ (PREFILL_COUNT_ALWAYS_ZERO).
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // F-217 SF-217-07 : contribution alimentaire des enfants BE — méthode
      // Renard, CC art. 203 / 203bis. Migration 240 — ALWAYS_ON (question
      // systématique de tout dossier Famille BE avec enfant). Backend
      // SF-217-06 (PR #995).
      ['contribution-alimentaire-enfants-be', {
        displayLabel: 'Contribution alimentaire des enfants (Belgique)',
        component: ContributionAlimentaireEnfantsBeSectionComponent,
        // Composant complet (form + verdict + montant + détail Renard).
        // Pré-fill IA V1 = 0 champ (PREFILL_COUNT_ALWAYS_ZERO).
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // F-217 SF-217-09 : pension alimentaire entre ex-époux BE — CC art. 301.
      // Migration 242 — ALWAYS_ON (question systématique de tout dossier de
      // divorce Famille BE). Backend SF-217-08 (PR #998).
      ['contribution-conjoint-be', {
        displayLabel: 'Pension alimentaire entre ex-époux (Belgique)',
        component: ContributionConjointBeSectionComponent,
        // Composant complet (form + verdict + durée + montant indicatif).
        // Pré-fill IA V1 = 0 champ (PREFILL_COUNT_ALWAYS_ZERO).
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // F-217 SF-217-13 : succession BE — dévolution légale et réserve
      // héréditaire post-2017 (CC Livre 4 réformé, loi 31/07/2017).
      // Migration 278 — ALWAYS_ON (toute analyse d'un dossier successoral
      // belge mobilise la qualification de la dévolution / réserve). Backend
      // SF-217-11 (PR #1180).
      ['succession-be-devolution-reserve', {
        displayLabel: 'Dévolution et réserve héréditaire (Belgique)',
        component: SuccessionBeDevolutionReserveSectionComponent,
        // Composant complet (form + verdict + héritiers + réserve + quotité).
        // Pré-fill IA V1 = 0 champ (PREFILL_COUNT_ALWAYS_ZERO).
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // F-217 SF-217-13 : succession BE — acceptation / renonciation
      // (CC art. 774+ nouveau — option successorale 4 mois).
      // Migration 278 — ALWAYS_ON (tout héritier appelé doit opter dans les
      // délais). Backend SF-217-12 (PR #1181).
      ['succession-be-acceptation-renonciation', {
        displayLabel: 'Acceptation / renonciation à succession (Belgique)',
        component: SuccessionBeAcceptationRenonciationSectionComponent,
        // Composant complet (form + verdict + option + délai + risques).
        // Pré-fill IA V1 = 0 champ (PREFILL_COUNT_ALWAYS_ZERO).
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // F-217 SF-217-17 : reconnaissance mariage / divorce étranger BE
      // (CDIP art. 21+ / 22+ / 25 / 27 / 46 — incluant le talaq).
      // Migration 281 — CATALOG (situation contextuelle, 20-30 % des
      // dossiers belges selon audit F-191 § 1.4 ; ajout via F-238).
      // Backend SF-217-16 bundle.
      ['mariage-etranger-be-reconnaissance', {
        displayLabel: 'Reconnaissance mariage / divorce étranger (Belgique)',
        component: MariageEtrangerBeReconnaissanceSectionComponent,
        // Composant complet (form + verdict + motifs refus/réserve + actes).
        // Pré-fill IA V1 = 0 champ (PREFILL_COUNT_ALWAYS_ZERO).
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // F-217 SF-217-19 : contestation de filiation BE (CC art. 318 nouveau —
      // qualité à agir + délai 1 an + possession d'état conforme 5 ans).
      // Migration 281 — CONTEXTUAL avec trigger non extrait V1
      // (`contestation_filiation_be_envisagee`) → tombe dans la couche CATALOG
      // (activation manuelle via F-238). Backend SF-217-18.
      ['contestation-filiation-be', {
        displayLabel: 'Contestation de filiation (Belgique)',
        component: ContestationFiliationBeSectionComponent,
        // Composant complet (form + verdict + voie procédurale + motifs
        // d'irrecevabilité). Pré-fill IA V1 = 0 champ
        // (PREFILL_COUNT_ALWAYS_ZERO).
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
      // F-217 SF-217-15 : protection du majeur BE — loi du 17/03/2013
      // (statut unique d'administration : biens / personne / mandat
      // extra-judiciaire / mesure provisoire d'urgence).
      // Migration 281 — ALWAYS_ON (la question de la protection du majeur
      // peut se poser sur tout dossier famille BE comportant un client âgé
      // / vulnérable — situation toujours pertinente, pas à détecter).
      // Backend SF-217-14.
      ['protection-majeur-be', {
        displayLabel: 'Protection du majeur (Belgique)',
        component: ProtectionMajeurBeSectionComponent,
        // Composant complet (form + verdict + mesure + juridiction + actes
        // protégés + actions concrètes + bases juridiques).
        // Pré-fill IA V1 = 0 champ (PREFILL_COUNT_ALWAYS_ZERO).
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          standaloneMode: ctx.standaloneMode ?? false,
        }),
      }],
    ]);

  /**
   * F-169 SF-169-01 : ordre d'affichage des thèmes métier dans le panel.
   * "Indemnités d'abord" → ce qui chiffre une créance pour le client.
   * "Validité" → ce qui sécurise / conteste un acte.
   * "Délais" → temporalité et procédure.
   * "Documents" → livrables.
   * "Diagnostic" → orientation / arbres décisionnels (fallback par défaut).
   */
  static readonly THEMES_ORDERED: readonly ThemeDescriptor[] = [
    { key: 'INDEMNITES', label: 'Indemnités & calculs' },
    { key: 'VALIDITE', label: 'Validité & contestation' },
    { key: 'DELAIS', label: 'Délais & procédure' },
    { key: 'DOCUMENTS', label: 'Documents' },
    { key: 'DIAGNOSTIC', label: 'Diagnostic situation' },
  ];

  /**
   * F-169 SF-169-01 : mapping `tool_id → thème métier`. Couvre l'ensemble
   * des entrées de TOOL_REGISTRY. Tout `tool_id` non mappé tombe sur le
   * fallback `DIAGNOSTIC` avec un `console.warn`.
   */
  static readonly THEME_BY_TOOL_ID: ReadonlyMap<string, ThemeKey> = new Map<string, ThemeKey>([
    // ── Indemnités & calculs ───────────────────────────────────────────
    ['F-DT-07-anciennete-conges-prime', 'INDEMNITES'],
    ['F-DT-09-comparateur-indemnites', 'INDEMNITES'],
    ['F-DT-12-discrimination-dommages-interets', 'INDEMNITES'],
    ['F-DT-15-inaptitude', 'INDEMNITES'],
    ['F-DT-17-indemnite-precarite-cdd', 'INDEMNITES'],
    ['F-DT-18-fin-mission-interim', 'INDEMNITES'],
    ['F-DT-19-heures-sup', 'INDEMNITES'],
    ['F-DT-20-rappel-salaire', 'INDEMNITES'],
    // SF-213-02b : rappel de salaire BE — chiffrage arriérés + intérêts moratoires
    // 10 % + prescription (Loi 12/04/1965 art. 10 + Loi 03/07/1978 art. 15).
    ['rappel-salaire-be', 'INDEMNITES'],
    // SF-213-03b : préavis statut unique BE — chiffrage durée préavis + ICP
    // (Loi 26/12/2013 + Loi 03/07/1978 art. 37/2 §1er). BE-only ALWAYS_ON.
    ['licenciement-be-statut-unique-preavis', 'INDEMNITES'],
    // SF-213-04b : préavis Formule Claeys BE — fraction Claeys pré-2014 +
    // cumul statut unique post-2014 via clause de sauvegarde (Loi 26/12/2013
    // art. 67 + ancien art. 82 §3 Loi 03/07/1978 + Cass. 28/02/2011).
    // BE-only ALWAYS_ON priority 112.
    ['licenciement-be-formule-claeys', 'INDEMNITES'],
    // SF-213-10b : score CCT n° 109 BE — calculateur d'indemnité
    // (3/8/12/17 semaines × rémunération hebdomadaire). Thème
    // INDEMNITES : calculateur d'indemnité, pas analyseur de validité
    // (le motif grave / la conformité procédurale sont des inputs, le
    // résultat est un montant + nombre de semaines). BE-only,
    // ALWAYS_ON priority 118.
    ['licenciement-be-cct109-deraisonnable', 'INDEMNITES'],
    ['F-DT-21-travail-dissimule', 'INDEMNITES'],
    ['F-DT-25-indemnite-preavis', 'INDEMNITES'],
    ['F-DT-26-conges-payes-indemnite', 'INDEMNITES'],
    // SF-206-04 : rappel de CP acquis pendant arrêt maladie (FR, L.3141-5 /
    // L.3141-5-1 CT, loi 22/04/2024). Groupe F-169 « Rappels et indemnités
    // salariales » — c'est un rappel de droits, pas une rupture.
    ['F-DT-75-conges-payes-arret-maladie', 'INDEMNITES'],
    // SF-218-12 : VRP — indemnité de clientèle + préavis spécifique + option
    // la plus favorable. Calculateur de montant d'indemnité (cohérent avec les
    // autres outils INDEMNITES). FR-only (statut VRP statutaire, L.7311-1 s.).
    ['F-DT-104-vrp-indemnite-clientele', 'INDEMNITES'],
    ['F-DT-28-avantages-conventionnels-be', 'INDEMNITES'],
    ['F-DT-31-transaction', 'INDEMNITES'],
    ['F-DT-35-contestation-are-fr', 'INDEMNITES'],
    ['F-132-rupture-conv-indemnite', 'INDEMNITES'],
    // SF-207-07b : RCC BE — indemnité complémentaire (CCT 17 art. 5).
    // Thème INDEMNITES (calculateur de montant, cohérent avec les autres
    // chiffrages d'indemnité).
    ['rcc-be-indemnite-complementaire', 'INDEMNITES'],
    ['F-FA-15-recompenses', 'INDEMNITES'],
    // F-198 SF-198-01/02/05 : rattrapage des outils Famille FR DELETE par migration 191.
    ['F-FA-01-prestation-compensatoire', 'INDEMNITES'],
    ['F-FA-02-pension-alimentaire', 'INDEMNITES'],
    ['F-153-fourchettes-jaf', 'INDEMNITES'],
    // F-217 SF-217-07 : contribution alimentaire des enfants BE — chiffre une créance.
    ['contribution-alimentaire-enfants-be', 'INDEMNITES'],
    // F-217 SF-217-09 : pension alimentaire entre ex-époux BE — chiffre une créance.
    ['contribution-conjoint-be', 'INDEMNITES'],

    // ── Validité & contestation ────────────────────────────────────────
    ['F-DT-08-licenciement-validity', 'VALIDITE'],
    ['F-DT-10-rupture-conv-validity', 'VALIDITE'],
    ['F-DT-11-harcelement-licenciement-nul', 'VALIDITE'],
    ['F-DT-13-licenciement-economique', 'VALIDITE'],
    ['F-DT-14-pse-validite', 'VALIDITE'],
    ['F-DT-16-licenciement-nul-detection', 'VALIDITE'],
    ['F-DT-36-procedure-nullite-licenciement', 'VALIDITE'],
    // SF-206-02 : contestation de la présomption de démission (qualification
    // de la rupture — la présomption peut être renversée).
    ['F-DT-42-abandon-poste-presomption-demission', 'VALIDITE'],
    // F-DT-38 SF-DT-38-02 : qualification rupture période d'essai (FR).
    ['F-DT-38-rupture-periode-essai', 'VALIDITE'],
    ['F-DT-22-requalification-cdd-cdi', 'VALIDITE'],
    ['F-DT-23-requalification-interim-cdi', 'VALIDITE'],
    ['F-DT-24-non-concurrence', 'VALIDITE'],
    ['F-DT-27-motif-grave-be', 'VALIDITE'],
    // SF-213-01b : clause de non-concurrence BE — analyseur 3 verdicts
    // (VALIDE / NULLE / PARTIELLEMENT_NULLE), BE-only.
    ['clause-non-concurrence-be', 'VALIDITE'],
    // SF-213-05b : protection grossesse BE — analyseur de validité du
    // licenciement (verdict 4 états + indemnité forfaitaire 6 mois +
    // charge preuve renversée si présomption). Thème VALIDITE (parité
    // motif-grave-be, F-DT-27 : analyseur de validité de la rupture,
    // pas chiffrage pur). BE-only, ALWAYS_ON priority 113.
    ['licenciement-be-protection-grossesse', 'VALIDITE'],
    // SF-213-08b : licenciement BE — protection délégué syndical
    // (Loi 19/03/1991 + CCT n° 5). Analyseur de validité binaire
    // (LICENCIEMENT_INTERDIT_SANS_PROCEDURE / HORS_PERIODE_PROTECTION).
    // Thème VALIDITE — analyseur de validité de la rupture vis-à-vis
    // de la fenêtre de protection (pattern miroir protection-grossesse).
    // BE-only, ALWAYS_ON priority 116.
    ['licenciement-be-protection-deleguee', 'VALIDITE'],
    // SF-213-09b — analyseur de validité acte équipollent à rupture BE
    // (4 verdicts ACTE_EQUIPOLLENT_PROBABLE / PAS / RISQUE_ACCEPTATION_TACITE
    // / A_ANALYSER). Thème VALIDITE (parité vagues précédentes F-213 BE).
    ['licenciement-be-acte-equivalent', 'VALIDITE'],
    // SF-219-01b — analyseur d'éligibilité RCC métiers lourds BE
    // (2 verdicts ELIGIBLE / INELIGIBLE + 4 raisons). Thème VALIDITE
    // (analyse d'éligibilité, cohérent avec rcc-be-conditions /
    // outplacement-be-obligatoire-45 / autres analyseurs F-207 / F-213).
    ['rcc-be-metiers-lourds', 'VALIDITE'],
    // SF-213-06b : transaction de fin de contrat BE (art. 2044 Cciv +
    // Loi 03/07/1978 art. 6). Analyseur de validité 4 états + checklist
    // renonciations + ratio. Thème VALIDITE — analyseur de validité du
    // protocole transactionnel (pattern motif-grave-be / protection-grossesse).
    // BE-only, ALWAYS_ON priority 114. Distinct de F-DT-31-transaction (FR).
    ['transaction-be-travail', 'VALIDITE'],
    ['F-DT-30-protection-rp', 'VALIDITE'],
    // SF-214-01 : F-IM-25 étranger malade L.425-9 CESEDA (FR) — analyseur d'éligibilité.
    // Thème VALIDITE (analyse d'éligibilité protection médicale, CONTEXTUAL FR).
    ['F-IM-25-etranger-malade-l4259-fr', 'VALIDITE'],
    // SF-214-04 : F-IM-26 regroupement familial L.434-1+ CESEDA (FR) — analyseur
    // d'éligibilité (ressources SMIC + surface habitable). Thème VALIDITE.
    ['F-IM-26-regroupement-familial-fr', 'VALIDITE'],
    // SF-214-06 : F-IM-27-vpf-liens-personnels-l42323-fr VPF liens personnels
    // L.423-23 CESEDA (FR). Thème VALIDITE — analyseur d'éligibilité (scoring
    // 0-100 + 4 verdicts). Aligné sur regroupement-familial-fr / etranger-malade.
    ['F-IM-27-vpf-liens-personnels-l42323-fr', 'VALIDITE'],
    // SF-214-08 : F-IM-28-vls-ts-validation-ofii-fr validation VLS-TS OFII (FR).
    // Thème DELAIS — calculateur de délai (3 mois CESEDA R.431-16+ : date
    // d'échéance + jours restants + statut A_VALIDER/URGENT/EXPIRE/VALIDE +
    // procédure de recours si expiré). FR uniquement, ALWAYS_ON. Aligné sur
    // F-IM-25-single-permit-be et les autres calculateurs de délais Immigration.
    ['F-IM-28-vls-ts-validation-ofii-fr', 'DELAIS'],
    // SF-214-30 : F-IM-39-naturalisation-recours-tj-fr recours TJ naturalisation
    // (FR). Thème DELAIS — calculateur de délai (6 mois C. civ. art. 26-3/26-4 :
    // date du refus → échéance recours judiciaire + jours restants + tribunal
    // compétent). Bridge échéance F-69. Cohérent avec F-IM-28 (DELAIS).
    ['F-IM-39-naturalisation-recours-tj-fr', 'DELAIS'],
    // SF-214-32 : F-IM-40-naturalisation-recours-ta-fr recours TA Nantes
    // naturalisation (FR). Thème DELAIS — calculateur de délai (2 mois recours
    // pour excès de pouvoir : date du refus de décret → échéance recours TA +
    // jours restants + TA de Nantes). Bridge échéance F-69. Cohérent avec
    // F-IM-39 (DELAIS).
    ['F-IM-40-naturalisation-recours-ta-fr', 'DELAIS'],
    // SF-214-34 : F-IM-41-appel-caa-cassation-ce-fr appel CAA / cassation CE (FR).
    // Thème DELAIS — calculateur de délai d'appel (1 mois, ou 15 j en OQTF) :
    // date du jugement TA → échéance appel CAA + jours restants + CAA compétente
    // + filtre pourvoi CE. Bridge échéance F-69. Cohérent avec F-IM-40 (DELAIS).
    ['F-IM-41-appel-caa-cassation-ce-fr', 'DELAIS'],
    // SF-214-36 : F-IM-42-assignation-residence-fr assignation à résidence (FR).
    // Thème VALIDITE — analyseur de validité de la mesure (statut de validité de
    // l'assignation : en cours / expiration proche / expirée) + motifs de
    // contestation + recours TA. Bridge échéance F-69 (statut EN_COURS).
    ['F-IM-42-assignation-residence-fr', 'VALIDITE'],
    // SF-214-38 : F-IM-43-itf-judiciaire-fr ITF judiciaire (FR).
    // Thème VALIDITE — analyseur de validité / voie de recours de la peine d'ITF
    // (statut de la voie de recours pénale + conditions du relevé + distinction
    // ITF judiciaire vs IRTF administrative). Bridge échéance F-69 (statut
    // APPEL_POSSIBLE).
    ['F-IM-43-itf-judiciaire-fr', 'VALIDITE'],
    // SF-214-40 : F-IM-44-ue-eee-suisse-sejour-fr séjour UE/EEE/Suisse (FR).
    // Thème VALIDITE — analyseur de droits au séjour des citoyens UE/EEE/Suisse
    // (directive 2004/38/CE) : droit automatique 3 mois + droit permanent > 5 ans
    // + titre obtenu + encadré membre de famille non-UE. Pas de bridge échéance.
    ['F-IM-44-ue-eee-suisse-sejour-fr', 'VALIDITE'],
    // SF-214-42 : F-IM-45-retrait-titre-fraude-fr retrait titre pour fraude (FR).
    // Thème VALIDITE — analyseur de validité de la décision de retrait (vices de
    // procédure + motifs de contestation au fond) + délai du recours TA. Bridge
    // échéance F-69 (statut RECOURS_POSSIBLE / URGENT). Cohérent avec F-IM-42/43.
    ['F-IM-45-retrait-titre-fraude-fr', 'VALIDITE'],
    // SF-214-44 : F-IM-46-autorisation-travail-employeur-fr autorisation travail
    // employeur (FR). Thème DOCUMENTS — checklist de la procédure de demande
    // d'autorisation de travail auprès de l'OFII (obligations de la demande +
    // délai d'instruction + taxe OFII), côté employeur. Bridge échéance F-69
    // (délai du recours TA si refus, statut RECOURS_POSSIBLE).
    ['F-IM-46-autorisation-travail-employeur-fr', 'DOCUMENTS'],
    // SF-214-14 : F-IM-31-renouvellement-delai-depot-fr renouvellement délai dépôt (FR).
    // Thème DELAIS — calculateur de délai (date optimale + date impérative de dépôt
    // de la demande de renouvellement de titre + statut + jours restants).
    ['F-IM-31-renouvellement-delai-depot-fr', 'DELAIS'],
    // SF-214-10 : F-IM-29-oqtf-categories-l6111-fr OQTF catégories L.611-1 (FR).
    // Thème VALIDITE — analyseur des moyens de défense propres à chaque catégorie
    // L.611-1 (1° à 7°) + renvoi F-IM-22 si CAT_7. ThemeKey VALIDITE (le groupement
    // métier « contentieux OQTF » de la mini-spec se rattache au thème VALIDITE,
    // seul ThemeKey disponible pour les analyseurs de validité/qualification).
    ['F-IM-29-oqtf-categories-l6111-fr', 'VALIDITE'],
    // SF-214-16 : F-IM-32-recepisse-attestation-fr récépissé vs attestation (FR).
    // Thème VALIDITE — analyseur des droits (séjour / travail / durée de validité)
    // attachés au récépissé ou à l'attestation de prolongation d'instruction.
    ['F-IM-32-recepisse-attestation-fr', 'VALIDITE'],
    // SF-214-18 : F-IM-33-ofpra-introduction-fr OFPRA introduction (FR) — checklist
    // procédure (stepper 5 étapes + pièces requises) → thème DOCUMENTS.
    ['F-IM-33-ofpra-introduction-fr', 'DOCUMENTS'],
    // SF-214-26 : F-IM-37-anef-procedure-fr ANEF procédure / pannes (FR) — guide + recours.
    ['F-IM-37-anef-procedure-fr', 'DOCUMENTS'],
    // SF-214-20 : F-IM-34-aj-cnda-fr aide juridictionnelle CNDA (FR) — thème DELAIS,
    // calculateur de délais (recours CNDA 1 mois réduit en accélérée + échéance
    // dépôt AJ) avec bridge échéance F-69. Cohérent avec F-IM-28 (DELAIS).
    ['F-IM-34-aj-cnda-fr', 'DELAIS'],
    // SF-214-28 : F-IM-38-mna-evaluation-age-fr MNA évaluation âge / recours JE (FR)
    // — thème VALIDITE, analyseur de la qualité de mineur isolé (statut + droits
    // attachés + contestation examen osseux + procédure ASE/JE) avec bridge
    // échéance F-69 sur la saisine du juge des enfants si RECOURS_JE_URGENT.
    ['F-IM-38-mna-evaluation-age-fr', 'VALIDITE'],
    // SF-214-22 : F-IM-35-victime-traite-l4251-fr victime de traite L. 425-1 (FR) —
    // thème VALIDITE, analyseur d'éligibilité (scoring) à la carte de séjour
    // « vie privée et familiale » + alerte sécurité si victime en danger.
    ['F-IM-35-victime-traite-l4251-fr', 'VALIDITE'],
    // SF-214-24 : F-IM-36-carte-resident-l4261-fr carte de résident L. 426-1 (FR)
    // — thème VALIDITE, analyseur d'éligibilité (scoring) à la carte de résident
    // de dix ans + checklist critères non remplis + atouts.
    ['F-IM-36-carte-resident-l4261-fr', 'VALIDITE'],
    // SF-214-12 : F-IM-30-aes-presence-prouvee-fr AES présence prouvée (FR).
    // Thème DIAGNOSTIC — l'outil agrège des périodes de présence justifiées par
    // pièce et restitue un diagnostic d'éligibilité aux 4 voies AES (famille 5 ans,
    // humanitaire 10 ans, étudiant 3 ans, métiers en tension 3 ans) + gaps +
    // recommandations de pièces. FR uniquement, CONTEXTUAL (flag aesCalculPresenceDeclenche).
    ['F-IM-30-aes-presence-prouvee-fr', 'DIAGNOSTIC'],
    // SF-215-02 : F-IM-25-single-permit-be permis unique BE (travail+séjour).
    // Thème DELAIS (calcul date limite dépôt = dateFinPermit - 60j + 4 statuts
    // de renouvellement). Aligné sur les autres outils Immigration BE-only
    // CONTEXTUAL (flag `single_permit_envisage`).
    ['F-IM-25-single-permit-be', 'DELAIS'],
    // SF-215-04 : F-IM-26-regroupement-10ter-be regroupement familial 10ter (BE).
    // Thème VALIDITE — analyseur d'éligibilité (scoring 0-100 + 3 verdicts).
    // Aligné sur F-IM-14-40ter-familial-belge-be et les autres scoring BE
    // (visibility CONTEXTUAL via flag `regroupement_10ter_detecte`).
    ['F-IM-26-regroupement-10ter-be', 'VALIDITE'],
    // SF-215-14 : F-IM-31-cce-annulation-30j-be recours CCE annulation (BE).
    // Thème DELAIS — calculateur de délai (30j calendaires, date limite +
    // jours restants + statut DISPONIBLE/URGENT/EXPIRE/RECOURS_FORME). Aligné
    // sur F-IM-08-annexe13-be et les autres calculateurs de délais CCE/CESEDA.
    ['F-IM-31-cce-annulation-30j-be', 'DELAIS'],
    // SF-215-16 : F-IM-32-cce-extreme-urgence-5j-be recours CCE extrême urgence (BE).
    // Thème DELAIS — calculateur de délai (5j OUVRABLES, date limite + jours
    // ouvrables restants + statut DISPONIBLE/CRITIQUE/EXPIRE/RECOURS_FORME +
    // audience estimée). Aligné sur F-IM-31-cce-annulation-30j-be et les autres
    // calculateurs de délais CCE/CESEDA.
    ['F-IM-32-cce-extreme-urgence-5j-be', 'DELAIS'],
    // SF-215-18 : F-IM-33-annexe13quinquies-ie-be annexe 13quinquies OQT + IE (BE).
    // Thème DELAIS — calculateur (durée IE 3/5/8 ans, date de fin, date de levée
    // précoce, délai du recours en annulation CCE 30j calendaires + statut +
    // jours restants). Aligné sur F-IM-31-cce-annulation-30j-be et les autres
    // calculateurs de délais CCE/CESEDA.
    ['F-IM-33-annexe13quinquies-ie-be', 'DELAIS'],
    // SF-215-20 : F-IM-34-protection-temporaire-ukraine-be protection temporaire
    // Ukraine (BE). Thème VALIDITE — analyseur d'éligibilité (badge ELIGIBLE /
    // INELIGIBLE, durée de protection restante, droits travail/aides, chemin
    // procédural). Aligné sur les autres analyseurs d'éligibilité Immigration BE
    // (F-IM-27 regroupement 10bis, F-IM-28 naturalisation 12bis).
    ['F-IM-34-protection-temporaire-ukraine-be', 'VALIDITE'],
    // SF-215-06 : F-IM-27-regroupement-10bis-be regroupement familial 10bis (BE).
    // Thème VALIDITE — analyseur d'éligibilité (scoring 0-100 + 3 verdicts +
    // condition supplémentaire `conditionTitreEnCours` sur validité carte A).
    // Symétrie avec F-IM-26 / F-IM-14 (visibility CONTEXTUAL via flag
    // `regroupement_10bis_detecte`).
    ['F-IM-27-regroupement-10bis-be', 'VALIDITE'],
    // SF-215-08 : F-IM-28-naturalisation-12bis-be naturalisation 12bis (BE).
    // Thème VALIDITE — analyseur d'éligibilité 2 voies (5 ans / 10 ans) +
    // verdict AUCUNE. Symétrie avec F-IM-13-naturalisation FR (visibility
    // CONTEXTUAL via flag `naturalisation_be_envisagee`).
    ['F-IM-28-naturalisation-12bis-be', 'VALIDITE'],
    // SF-215-10 : F-IM-29-naturalisation-conjoint-belge-be naturalisation
    // conjoint Belge art. 16 (Code de la nationalité belge). Thème VALIDITE
    // — analyseur d'éligibilité avec verdict ELIGIBLE/INELIGIBLE + délai
    // manquant. Visibility CONTEXTUAL via flag `naturalisation_be_envisagee`
    // (partagé avec F-IM-28 — la voie 12bis 5 ans et la voie conjoint Belge
    // sont les deux voies « courtes » qui co-existent — l'avocat compare).
    ['F-IM-29-naturalisation-conjoint-belge-be', 'VALIDITE'],
    // SF-215-12 : F-IM-30-aesm-mena-be — AESM + tutelle DGDE (MENA, BE).
    // Thème VALIDITE — analyseur d'éligibilité composite 2 volets
    // (tutelle DGDE Loi 04/05/2007 + scoring AESM Art. 9bis adapté +
    // Circulaire OE 15/09/2005). Verdict 3 états + bandeau urgence
    // si age ≥ 17 + bloc info tutelle conditionnel. Symétrie avec les
    // autres F-IM-XX-be (visibility CONTEXTUAL via flag
    // `mineur_non_accompagne_be_detecte` — F-203).
    ['F-IM-30-aesm-mena-be', 'VALIDITE'],
    // SF-207-06b : RCC BE — conditions d'éligibilité (analyseur 4 régimes).
    // Thème VALIDITE (analyse d'éligibilité, cohérent avec les autres analyseurs).
    ['rcc-be-conditions', 'VALIDITE'],
    // SF-219-02b : RCC BE — longue carrière (AR 03/05/2007 art. 3 — 59+/40).
    // Thème VALIDITE — analyseur d'éligibilité dédié au régime longue
    // carrière (parité rcc-be-conditions). 4 verdicts + indemnité indicative.
    ['rcc-be-longue-carriere', 'VALIDITE'],
    // SF-219-03b : RCC BE — entreprise en difficulté / restructuration
    // (Loi 26/12/2013 + CCT n° 17 + AR 03/05/2007 + AR de reconnaissance
    // ministérielle). Thème VALIDITE — analyseur d'éligibilité dédié au
    // régime entreprise reconnue (parité rcc-be-conditions / rcc-be-
    // metiers-lourds / rcc-be-longue-carriere). 6 verdicts (1 éligible +
    // 5 motifs d'inéligibilité) + indemnité complémentaire indicative.
    ['rcc-be-entreprise-difficulte', 'VALIDITE'],
    // SF-219-04b : Cumul RCC + allocations / pension (BE) — analyseur
    // transversal de conformité (CCT 17 + AR 25/11/1991 ONEM +
    // AR 03/05/2007 art. 22+). Thème VALIDITE — analyseur de validité du
    // cumul mensuel (plafond + régime de disponibilité + bascule pension +
    // compatibilité activité). 4 verdicts. Parité rcc-be-conditions.
    ['cumul-rcc-allocations', 'VALIDITE'],
    // SF-219-05b : Outplacement BE général au titre du régime préavis ≥ 30
    // semaines (Loi 05/09/2001 art. 11 + AR 21/10/2007). Thème VALIDITE —
    // analyseur de conformité (parité outplacement-be-obligatoire-45 +
    // autres analyseurs de conformité légale). 7 verdicts hiérarchiques
    // + indemnité forfaitaire sanction.
    ['outplacement-be-general-30sem', 'VALIDITE'],
    // SF-219-06b : Licenciement BE — fermeture d'entreprise (Loi 26/06/2002
    // + AR 23/03/2007 + CCT 9bis). Thème INDEMNITES — l'outil calcule
    // l'indemnité de fermeture (forfaitaire par année d'ancienneté +
    // supplément ≥ 45 ans) et le montant total des créances FFE reprises
    // (salaires + pécule + indemnité de rupture impayés). Parité avec
    // rcc-be-indemnite-complementaire / formule-claeys / rappel-salaire-be.
    ['licenciement-be-fermeture-entreprise', 'INDEMNITES'],
    // SF-219-07b : Licenciement collectif BE — Loi Renault (Loi 13/02/1998 +
    // CCT n° 24 + CCT n° 39 + Directive 98/59/CE). Thème VALIDITE — l'outil
    // est une checklist procédurale (PAS un calculateur) qui vérifie la
    // conformité des 3 phases + délai d'attente 30 j conditionnant la
    // VALIDITÉ du licenciement collectif (sanction art. 67 + nullité préavis
    // en cas de non-respect). Parité avec outplacement-be-general-30sem /
    // outplacement-be-obligatoire-45 / F-DT-27-motif-grave-be (analyseurs
    // de conformité légale).
    ['licenciement-be-collectif-renault', 'VALIDITE'],
    // SF-219-08b : Transfert d'entreprise CCT n° 32bis (07/06/1985) +
    // Loi 17/03/1965 + Directive 2001/23/CE. Thème VALIDITE — l'outil
    // qualifie le transfert (5 verdicts : 1 conforme / 1 conforme partiel /
    // 2 inéligibles / 1 à analyser) et valide la procédure d'information-
    // consultation préalable. Parité avec les autres analyseurs de validité
    // procédurale BE (rcc-be-conditions, outplacement-be-obligatoire-45).
    ['transfert-entreprise-cct-32bis', 'VALIDITE'],
    // SF-219-09b : Élections sociales BE (Loi 04/12/2007 + AR 25/05/2012 +
    // Loi 04/08/1996 art. 49 + Loi 19/03/1991). Thème VALIDITE — l'outil
    // qualifie l'obligation procédurale (4 verdicts : CE+CPPT / CPPT seul /
    // non applicable / à recalculer) et la conformité de la procédure
    // (calendrier rebours, UTE, fenêtre protection candidats). Parité avec
    // les autres analyseurs de validité procédurale BE
    // (licenciement-be-collectif-renault, transfert-entreprise-cct-32bis).
    ['elections-sociales-be', 'VALIDITE'],
    // SF-219-10b : Statut du délégué syndical CCT n° 5 (CNT, 24/05/1971) +
    // CCT 5bis/5ter + AR 26/01/1972 + CCT sectorielles. Thème VALIDITE —
    // l'outil qualifie le statut du DS (5 verdicts : 1 reconnu / 1 fragile /
    // 2 inéligibles / 1 à analyser) et liste les missions exerçables.
    // Parité avec les autres analyseurs de validité statutaire BE
    // (transfert-entreprise-cct-32bis, licenciement-be-protection-deleguee).
    ['delegue-syndical-cct-5', 'VALIDITE'],
    // SF-219-11b : Congé-éducation payé régionalisé (Loi 22/01/1985 Section 6,
    // régionalisée 2014 — WBR / FLA VOV / BXL). Thème VALIDITE — l'outil qualifie
    // l'éligibilité au congé (5 verdicts : 2 éligibles plein droit/prorata,
    // 2 inéligibles hors liste/occupation insuffisante, 1 à analyser) et
    // calcule le plafond régional applicable. Parité avec les autres
    // analyseurs de validité d'éligibilité BE (cumul-rcc-allocations,
    // outplacement-be-general-30sem, rcc-be-conditions).
    ['conge-education-paye-region', 'VALIDITE'],
    // SF-219-12b : Flexi-job BE (Loi-programme du 26/12/2013 art. 13 à 28 +
    // extensions 2015/2018/2023 + AR 02/06/2024). Thème VALIDITE — l'outil
    // qualifie l'éligibilité d'une occupation flexi-job sur 5 dimensions
    // cumulatives (travailleur / secteur / cumul / formalisme / rémunération)
    // avec verdict hiérarchisé 7 états (1 éligible / 2 fragiles / 3 inéligibles
    // / 1 à analyser). Parité avec les autres analyseurs de validité statutaire
    // BE (delegue-syndical-cct-5, transfert-entreprise-cct-32bis).
    ['flexi-job-be', 'VALIDITE'],
    // SF-219-13b : Étudiant jobiste BE (Loi du 03/07/1978 Titre VII +
    // Loi-programme du 24/12/2002 + AR du 14/07/1995 + Loi-programme du
    // 22/12/2023 pérennisant 600h/an). Thème VALIDITE — l'outil qualifie
    // l'éligibilité d'une occupation étudiant sur 4 dimensions cumulatives
    // (statut / quota / formalisme / cotisations) avec verdict hiérarchisé
    // 6 états (1 éligible / 2 fragiles / 2 inéligibles / 1 à analyser).
    // Parité avec les autres analyseurs de validité statutaire BE
    // (flexi-job-be, delegue-syndical-cct-5).
    ['etudiant-jobiste-be', 'VALIDITE'],
    // SF-219-14b : Statut intérim BE — CCT n° 322 (Loi du 24/07/1987 +
    // CCT n° 322 du 14/06/2010 + CCT n° 108 + AR 11/10/1976 +
    // Loi-programme du 27/12/2012). Thème VALIDITE — l'outil qualifie
    // la régularité d'une mission intérimaire sur 4 dimensions cumulatives
    // (motif / durée / parité salariale / formalisme) avec verdict
    // hiérarchisé 7 états (1 éligible / 1 fragile formalisme / 4 inéligibles
    // / 1 à analyser zone grise artistique-flux). Parité avec les autres
    // analyseurs de validité statutaire BE (flexi-job-be,
    // delegue-syndical-cct-5, transfert-entreprise-cct-32bis).
    ['interim-be-cct-322', 'VALIDITE'],
    // SF-219-15b : Indemnité fin de mission intérim BE — outil de CALCUL
    // d'indemnité (pécule de vacances 15,38 % + prime fin d'année
    // sectorielle + indemnité rupture anticipée Cass. 04/02/1991 +
    // sursalaire heures sup Loi 16/03/1971). Thème INDEMNITES —
    // calculateur de montant cohérent avec les autres calculateurs BE
    // (rappel-salaire-be, licenciement-be-statut-unique-preavis,
    // licenciement-be-formule-claeys, F-DT-28-avantages-conventionnels-be)
    // et avec son homologue FR F-DT-18-fin-mission-interim (IFM 10 %).
    // Pas un analyseur de validité (l'outil 14b interim-be-cct-322
    // couvre déjà la qualification de la mission).
    ['interim-be-indemnite-fin-mission', 'INDEMNITES'],
    // SF-219-16b : Télétravail BE — CCT n° 85 du 09/11/2005 + CCT n° 149
    // du 26/01/2021 + Loi du 03/10/2022 « Deal pour l'emploi ». Thème
    // VALIDITE — l'outil qualifie la conformité d'une formule de télétravail
    // (structurel ou occasionnel) sur 4 conditions cumulatives (convention
    // écrite art. 6 / équipement art. 9 / droits sociaux art. 4 /
    // modalités déconnexion Loi 03/10/2022) avec verdict hiérarchisé
    // 7 états (2 conformes structurel/occasionnel / 3 non-conformes / 1
    // fragile déconnexion / 1 à analyser indéterminé). Parité avec les
    // autres analyseurs de conformité statutaire BE (interim-be-cct-322,
    // flexi-job-be, delegue-syndical-cct-5, transfert-entreprise-cct-32bis).
    ['teletravail-be-cct-85-149', 'VALIDITE'],
    // SF-219-17b : Clause d'écolage BE — analyseur de validité (8 verdicts
    // hiérarchisés : INOPPOSABLE_MOTIF_DEPART / NULLE_FORME_ECRITE_MANQUANTE /
    // NULLE_FORMATION_OBLIGATOIRE / NULLE_COUT_INSUFFISANT /
    // NULLE_DUREE_EXCESSIVE / VALIDE_REMBOURSEMENT_DEGRESSIF /
    // VALIDE_DUREE_EXPIREE / A_ANALYSER). Thème VALIDITE — cohérent avec les
    // autres analyseurs de conformité statutaire BE (interim-be-cct-322,
    // flexi-job-be, teletravail-be-cct-85-149, transfert-entreprise-cct-32bis,
    // delegue-syndical-cct-5).
    ['clause-ecolage-be', 'VALIDITE'],
    // SF-219-18b : Semaine de 4 jours BE — Loi du 03/10/2022 « Deal pour
    // l'emploi » M.B. 10/11/2022, art. 5 (régime à la demande du
    // travailleur à temps plein) + art. 6 (procédure simplifiée
    // règlement de travail). Thème VALIDITE — l'outil qualifie la
    // conformité d'une mise en place de la semaine de 4 jours sur
    // 5 conditions cumulatives (temps plein / demande écrite / journée
    // ≤ 9 h 30 ou 10 h CCT / avenant signé + règlement de travail
    // modifié / durée avenant ≤ 6 mois ou renouvellement) avec verdict
    // hiérarchisé 9 états (1 conforme / 2 court-circuits critiques —
    // licenciement représailles, refus non motivé / 4 non-conformes
    // de fond / 1 non éligible / 1 à analyser indéterminé). Parité
    // avec les autres analyseurs de conformité statutaire BE
    // (interim-be-cct-322, flexi-job-be, delegue-syndical-cct-5,
    // transfert-entreprise-cct-32bis, teletravail-be-cct-85-149).
    ['semaine-4-jours-be', 'VALIDITE'],
    // SF-219-19b : Droit à la déconnexion BE
    ['droit-deconnexion-be', 'VALIDITE'],
    // SF-219-20b : Pécule de vacances BE
    ['pecule-vacances-be', 'INDEMNITES'],
    // SF-219-21b : Éco-chèques + chèques-repas BE — analyseur de
    // conformité statutaire (6 verdicts, ventilation montant exonéré /
    // requalifié / cotisations ONSS). Thème VALIDITE — cohérent avec
    // les autres analyseurs de conformité statutaire BE
    // (semaine-4-jours-be, droit-deconnexion-be, interim-be-cct-322,
    // flexi-job-be, teletravail-be-cct-85-149).
    ['eco-cheques-cheques-repas-be', 'VALIDITE'],
    // SF-219-22b : Égalité salariale F/H BE — analyseur de conformité
    // à l'obligation rapport biennal (art. 2 Loi 22/04/2012) +
    // ventilation art. 4 AR 17/08/2013 + plan d'action art. 5.
    // Thème VALIDITE (parité avec les autres analyseurs de conformité
    // statutaire BE : droit-deconnexion-be, semaine-4-jours-be,
    // delegue-syndical-cct-5, teletravail-be-cct-85-149).
    ['egalite-femmes-hommes-be', 'VALIDITE'],
    // SF-219-23b : Refus aménagements raisonnables handicap BE — analyseur
    // de conformité art. 14 Loi 10/05/2007 + CCT n° 95 + Directive
    // 2000/78/CE art. 5 (7 verdicts hiérarchisés couvrant qualification
    // handicap, refus motivé/non motivé, charge disproportionnée
    // démontrée/non démontrée, représailles présumées). Thème VALIDITE
    // (parité avec les autres analyseurs de conformité statutaire BE :
    // egalite-femmes-hommes-be, droit-deconnexion-be, semaine-4-jours-be,
    // delegue-syndical-cct-5, teletravail-be-cct-85-149).
    ['discrimination-be-handicap-amenagement', 'VALIDITE'],
    // SF-219-24b : Code pénal social BE — qualification d'infraction
    // sociale + restitution du niveau de sanction 1 à 4 (art. 101-103
    // C. pén. soc., Loi 06/06/2010). 5 verdicts couvrant les 4 niveaux
    // de sanction + A_QUALIFIER. Thème VALIDITE (parité avec les autres
    // analyseurs / qualifieurs de conformité pénale-statutaire BE :
    // discrimination-be-handicap-amenagement, egalite-femmes-hommes-be,
    // droit-deconnexion-be, semaine-4-jours-be, delegue-syndical-cct-5,
    // teletravail-be-cct-85-149). Outil de qualification d'infraction et
    // restitution de la peine encourue — pas de chiffrage indemnitaire
    // (distinct des outils INDEMNITES rappel-salaire-be, formule-claeys).
    ['code-penal-social-be', 'VALIDITE'],
    // SF-219-25b : Auditorat du travail BE
    ['auditorat-travail-be', 'VALIDITE'],
    // SF-219-26b : Travail noir BE — DIMONA
    ['travail-noir-be-dimona', 'VALIDITE'],
    // SF-219-27b : INASTI statut travailleur indépendant BE
    ['inastri-statut-travailleur-independant', 'VALIDITE'],
    // SF-219-28b : MP Fedris reconnaissance BE
    ['mp-fedris-reconnaissance', 'VALIDITE'],
    // SF-219-29b : Rente AT/MP vs capitalisation BE (calculateur
    // d'indemnite forfaitaire post-consolidation — capital unique ou
    // rente annuelle viagere selon seuil 19 % IPP). Theme INDEMNITES
    // (calculateur de montant, coherent avec rappel-salaire-be,
    // licenciement-be-formule-claeys, rcc-be-indemnite-complementaire).
    ['at-mp-rente-capital-be', 'INDEMNITES'],
    // SF-219-30b : Saisine CPAP BE (RPS) — conformite procedurale
    ['bien-etre-rps-conseiller-prevention', 'VALIDITE'],
    // SF-219-31b : Conge paternite / naissance BE (qualification de droit
    // + protection 5 mois licenciement, verdict 8 etats, coherent avec
    // les autres outils de qualification du panel BE :
    // mp-fedris-reconnaissance, bien-etre-rps-conseiller-prevention,
    // protection-grossesse).
    ['conge-paternite-naissance-be', 'VALIDITE'],
    // SF-219-32b : Interruption de carriere conge parental BE — analyseur
    // d'eligibilite (Loi 22/01/1985 + AR 29/10/1997 + CCT 64 + AR
    // 12/08/1991 ONEM). Theme VALIDITE — coherent avec les autres
    // analyseurs de conformite legale (mp-fedris-reconnaissance,
    // bien-etre-rps-conseiller-prevention, code-penal-social-be).
    // Distinct de F-DT-29 credit-temps-be (CCT 103, regime universel).
    ['interruption-carriere-soins-parental', 'VALIDITE'],
    // SF-207-08b : Outplacement BE obligatoire 45+ (analyseur de conformité,
    // 5 verdicts). Thème VALIDITE — cohérent avec les autres analyseurs de
    // conformité légale (rcc-be-conditions, F-DT-27-motif-grave-be).
    ['outplacement-be-obligatoire-45', 'VALIDITE'],
    ['F-FA-08-divorce-alteration', 'VALIDITE'],
    ['F-FA-09-divorce-faute', 'VALIDITE'],
    ['F-FA-10-divorce-accepte', 'VALIDITE'],
    ['F-FA-11-desunion-irremediable-be', 'VALIDITE'],
    // F-211 SF-211-05 : 3 outils Famille BE de validité (divorces + pacte successoral).
    ['divorce-dc-be', 'VALIDITE'],
    ['divorce-ddi-3voies-be', 'VALIDITE'],
    ['pacte-successoral-be-2018', 'VALIDITE'],
    // F-217 SF-217-03 : qualification de la composition du patrimoine.
    ['regime-mat-be-communaute-legale', 'VALIDITE'],
    // F-217 SF-217-05 : autorité parentale BE — qualification / orientation procédurale.
    ['autorite-parentale-be', 'VALIDITE'],
    // F-217 SF-217-13 : succession BE — quantification de la dévolution et
    // analyse de validité de la réserve héréditaire (CC Livre 4 BE réformé).
    ['succession-be-devolution-reserve', 'VALIDITE'],
    // F-217 SF-217-17 : reconnaissance mariage / divorce étranger BE — analyse
    // de validité d'ordre public (CDIP art. 21+ / 27 — talaq inclus).
    ['mariage-etranger-be-reconnaissance', 'VALIDITE'],
    // F-217 SF-217-15 : protection du majeur BE — outil d'orientation /
    // qualification de la mesure adéquate (loi 17/03/2013). VALIDITE plutôt
    // que DELAIS malgré l'urgence potentielle (qui est qualifiée à l'audience,
    // pas un délai procédural figé), cf. mini-spec SF-217-15.
    ['protection-majeur-be', 'VALIDITE'],
    ['F-FA-18-contestation-paternite', 'VALIDITE'],
    ['F-FA-18-recherche-paternite', 'VALIDITE'],
    ['F-FA-18-reconnaissance-paternelle', 'VALIDITE'],
    ['F-FA-18-possession-etat', 'VALIDITE'],
    ['F-FA-24-testament-validite', 'VALIDITE'],
    // F-198 SF-198-04 : rattrapage F-152 (DELETE par migration 191).
    ['F-152-divorce-consentement-scoring', 'VALIDITE'],

    // ── Délais & procédure ─────────────────────────────────────────────
    ['F-DT-03-prescription-litige', 'DELAIS'],
    // SF-207-01b : prescription Travail BE (Loi 03/07/1978 art. 15 + CCT 109 art. 11).
    ['prescription-be-litige-travail', 'DELAIS'],
    // SF-207-03b : contestation C4 ONEM — double délai admin (1 mois) + tribunal
    // (3 mois). Thème DELAIS pour cohérence avec la nature du calculateur.
    ['contestation-c4-onem', 'DELAIS'],
    // SF-207-04b : déclaration AT Fedris — délai 8 jours calendaires
    // employeur (Loi 10/04/1971 art. 62). Thème DELAIS.
    ['at-fedris-declaration', 'DELAIS'],
    // SF-213-07b : harcèlement BE — procédure formelle (Loi 04/08/1996
    // art. 32bis-32sexies + AR 10/04/2014). Outil de pilotage de
    // procédure interne (checklist 5 étapes + délai fatal 90 j + fenêtre
    // protection 12 mois). Thème DELAIS — outil de délais & procédure,
    // pas un analyseur de validité (pas de verdict 4 états). BE-only,
    // ALWAYS_ON priority 115.
    ['harcelement-be-procedure-formelle', 'DELAIS'],
    // SF-207-05b : référé tribunal du travail BE (CJ art. 584). Thème DELAIS
    // (procédure d'urgence — pas de thème URGENCES dédié à ce stade, cohérence
    // avec les 4 outils BE Travail F-207 déjà classés DELAIS).
    ['refere-tribunal-travail-be', 'DELAIS'],
    ['F-DT-29-credit-temps-be', 'DELAIS'],
    ['F-DT-33-at-mp', 'DELAIS'],
    ['F-DT-34-refere-prudhomal', 'DELAIS'],
    ['F-FA-12-mesures-provisoires', 'DELAIS'],
    // F-211 SF-211-05 : mesures provisoires tribunal famille BE — délais procéduraux urgents.
    ['tribunal-famille-be-mesures-prov', 'DELAIS'],
    // F-217 SF-217-03 : procédure à délais (délai de contredits CJ art. 1218).
    ['liquidation-partage-be', 'DELAIS'],
    ['F-FA-13-revisions-post-divorce', 'DELAIS'],
    ['F-FA-14-ordonnance-protection', 'DELAIS'],
    ['F-FA-23-ordonnance-requete', 'DELAIS'],
    // F-210 — 2 outils urgences procédurales Famille FR.
    ['mediation-familiale-pre-saisine', 'DELAIS'],
    ['acceptation-renonciation-succession', 'DELAIS'],
    // F-217 SF-217-13 : succession BE — option successorale 4 mois (CC art. 774+).
    ['succession-be-acceptation-renonciation', 'DELAIS'],
    // F-217 SF-217-19 : contestation de filiation BE — délai 1 an + possession
    // d'état conforme 5 ans bloquante (CC art. 318 nouveau).
    ['contestation-filiation-be', 'DELAIS'],
    ['F-IM-06-recours', 'DELAIS'],
    ['F-IM-08-oqtf-avec-delai-fr', 'DELAIS'],
    ['F-IM-08-oqtf-sans-delai-fr', 'DELAIS'],
    ['F-IM-08-referes-admin-fr', 'DELAIS'],
    ['F-IM-08-annexe13-be', 'DELAIS'],
    ['F-IM-20-mesures-eloignement', 'DELAIS'],
    // F-220 / SF-IM-21-XX — outils Immigration FR à délais courts ajoutés
    // post-salve 2 sans alignement THEME_BY_TOOL_ID (cf. test SF-169-01 T-01).
    ['F-IM-21-jld-retention-fr', 'DELAIS'],
    ['F-IM-22-dublin-recours-fr', 'DELAIS'],
    ['F-IM-23-crrv-refus-visa-fr', 'DELAIS'],
    ['F-IM-24-victime-violences-l4256-fr', 'DELAIS'],
    ['F-136-travail-procedure', 'DELAIS'],

    // ── Documents ──────────────────────────────────────────────────────
    ['F-DT-04-fiche-prudhomale', 'DOCUMENTS'],
    ['F-DT-06-requete-tribunal-travail', 'DOCUMENTS'],
    ['F-DT-32-documents-fin-contrat', 'DOCUMENTS'],
    // SF-207-02b : checklist C4 ONEM — outil de vérification documentaire
    // (mentions obligatoires d'un document de fin de contrat employeur BE).
    ['c4-onem-checklist', 'DOCUMENTS'],
    ['F-IM-01-checklist-pieces', 'DOCUMENTS'],
    ['F-FA-07-checklist-divorce', 'DOCUMENTS'],
    ['F-132-rupture-amiable-info', 'DOCUMENTS'],

    // ── Diagnostic situation ───────────────────────────────────────────
    ['F-IM-05-arbre-decisionnel-titre', 'DIAGNOSTIC'],
    ['F-IM-07-droit-au-travail', 'DIAGNOSTIC'],
    ['F-IM-09-aes-etudiant', 'DIAGNOSTIC'],
    ['F-IM-09-aes-famille', 'DIAGNOSTIC'],
    ['F-IM-09-aes-humanitaire', 'DIAGNOSTIC'],
    ['F-IM-09-aes-metiers-tension', 'DIAGNOSTIC'],
    ['F-IM-11-changement-statut', 'DIAGNOSTIC'],
    ['F-IM-12-asile-avance', 'DIAGNOSTIC'],
    ['F-IM-13-naturalisation', 'DIAGNOSTIC'],
    ['F-IM-14-40bis-cohabitant-ue-be', 'DIAGNOSTIC'],
    ['F-IM-14-40ter-familial-belge-be', 'DIAGNOSTIC'],
    ['F-IM-14-9bis-humanitaire-be', 'DIAGNOSTIC'],
    ['F-IM-14-9ter-medical-be', 'DIAGNOSTIC'],
    ['F-IM-17-regime-algerien', 'DIAGNOSTIC'],
    ['F-IM-19-mineurs', 'DIAGNOSTIC'],
    ['F-FA-05-partage-immobilier', 'DIAGNOSTIC'],
    ['F-FA-06-calendrier-garde', 'DIAGNOSTIC'],
    // SF-206-06 : prise d'acte de la rupture aux torts de l'employeur (FR,
    // Cass. soc. 25/06/2003 n°01-42.679). Groupe F-169 « Rupture — initiative
    // salarié / torts employeur » — diagnostic de solidité AVANT notification.
    ['F-DT-39-prise-acte-rupture', 'DIAGNOSTIC'],
    // SF-212-02 : F-DT-36 — qualification de la faute disciplinaire
    // (diagnostic = comprendre la situation et l'impact indemnitaire).
    ['F-DT-36-licenciement-faute-grave-lourde', 'DIAGNOSTIC'],
    // SF-212-04 : F-DT-50 — validité de la convention de forfait jours
    // (diagnostic = vérifier conformité L. 3121-58+ CT et estimer le rappel HS).
    ['F-DT-50-forfait-jours-validite', 'DIAGNOSTIC'],
    // SF-212-06 : F-DT-72 — transfert d'entreprise L. 1224-1 (diagnostic =
    // comprendre l'applicabilité du maintien automatique et détecter les
    // irrégularités pré/post-transfert).
    ['F-DT-72-transfert-entreprise-l1224-1', 'DIAGNOSTIC'],
    // SF-212-08 : F-DT-44 — CSP/CRP conformité de la proposition.
    ['F-DT-44-csp-crp-conformite', 'DIAGNOSTIC'],
    // SF-212-10 : F-DT-91 — faute inexcusable de l'employeur.
    ['F-DT-91-faute-inexcusable-employeur', 'DIAGNOSTIC'],
    // SF-212-26 : F-DT-61 — protection du lanceur d'alerte.
    ['F-DT-61-lanceur-alerte-protection', 'DIAGNOSTIC'],
    // SF-212-12 : F-DT-70 — modification du contrat — refus du salarié.
    ['F-DT-70-modification-contrat-refus', 'DIAGNOSTIC'],
    // SF-212-14 : F-DT-71 — mutation — validité de la clause de mobilité.
    ['F-DT-71-mutation-clause-mobilite', 'DIAGNOSTIC'],
    // SF-212-16 : F-DT-82 — télétravail — conformité et litige.
    ['F-DT-82-teletravail-accord', 'DIAGNOSTIC'],
    // SF-212-20 : F-DT-48 — mise à pied disciplinaire — régularité.
    ['F-DT-48-mise-a-pied-disciplinaire', 'DIAGNOSTIC'],
    // SF-212-24 : F-DT-56 — égalité salariale femmes/hommes.
    ['F-DT-56-egalite-salariale-femmes-hommes', 'DIAGNOSTIC'],
    // SF-212-36 : F-DT-46 — PDV / RCC conformité.
    ['F-DT-46-pdv-rcc-conformite', 'DIAGNOSTIC'],
    // SF-212-30 : F-DT-77 — congé maternité / paternité (protection & indemnités).
    ['F-DT-77-conge-paternite-maternite', 'DIAGNOSTIC'],
    // SF-212-28 : F-DT-64 — burn-out reconnaissance maladie professionnelle.
    ['F-DT-64-burnout-reconnaissance-mp', 'DIAGNOSTIC'],
    // SF-212-32 : F-DT-65 — élections CSE — conformité procédure.
    ['F-DT-65-elections-cse-conformite', 'DIAGNOSTIC'],
    // SF-212-34 : F-DT-49 — temps partiel requalification en temps plein.
    ['F-DT-49-temps-partiel-requalification', 'DIAGNOSTIC'],
    // SF-212-38 : F-DT-84 — conciliation CPH BCA (Bureau de Conciliation et d'Orientation). F-212 19/19.
    ['F-DT-84-conciliation-cph-bca', 'DIAGNOSTIC'],
    // SF-212-18 : F-DT-43 — rupture anticipée du CDD.
    ['F-DT-43-rupture-anticipee-cdd', 'DIAGNOSTIC'],
    // SF-212-22 : F-DT-41 — démission validité équivoque.
    ['F-DT-41-demission-validite-equivoque', 'DIAGNOSTIC'],
    // SF-206-08 : résiliation judiciaire du contrat aux torts de l'employeur
    // (FR, Cass. soc. 16/03/1989 ; Cass. soc. 20/01/1998 ; art. L.1411-1 CT ;
    // art. 1224, 1227-1228 C. civ.). Groupe F-169 « Rupture — initiative
    // salarié / torts employeur » — outil jumeau de F-DT-39 mais voie sans
    // risque (rejet ≠ rupture, salarié reste en poste pendant l'instance).
    ['F-DT-40-resiliation-judiciaire-cph', 'DIAGNOSTIC'],
    // F-198 SF-198-03 : rattrapage F-FA-04 (DELETE par migration 191).
    ['F-FA-04-liquidation-communaute', 'DIAGNOSTIC'],
    // SF-216-08 : ARIPA recouvrement pension alimentaire impayée (FR).
    ['F-FA-ARIPA-RECOUVREMENT', 'DIAGNOSTIC'],
    // SF-216-10 : délégation autorité parentale (FR, art. 376-1 Cciv).
    ['F-FA-XX-delegation-ap', 'DIAGNOSTIC'],
    // SF-216-12 : retrait autorité parentale (FR, art. 378-381 Cciv + loi 2022-140 LMVSS).
    ['F-FA-RETRAIT-AP', 'DIAGNOSTIC'],
    // SF-216-16 : adoption intra-familiale (FR, art. 345-1 Cciv).
    ['F-FA-ADOPTION-INTRA', 'DIAGNOSTIC'],
    // SF-216-18 : adoption internationale (FR, art. 370-3 Cciv + Convention La Haye 1993).
    ['F-FA-ADOPTION-INTERNATIONALE', 'DIAGNOSTIC'],
    // SF-216-14 : audition du mineur par le JAF (FR, art. 388-1 Cciv + art. 1074-1 à 1074-3 CPC).
    ['F-FA-AUDITION-MINEUR', 'DIAGNOSTIC'],
    // SF-216-20 : indignité successorale (FR, art. 726-729-1 Cciv + Loi 2022-1617).
    ['F-FA-INDIGNITE-SUCCESSORALE', 'DIAGNOSTIC'],
    // SF-216-22 : recel de succession (FR, art. 778 Cciv + Cass. 1ère civ. 14/11/2012).
    ['F-FA-RECEL-SUCCESSION', 'DIAGNOSTIC'],
    // SF-216-24 : donation entre époux (FR, art. 1091-1100 Cciv + art. 265 al. 2).
    ['F-FA-DONATION-ENTRE-EPOUX', 'DIAGNOSTIC'],
    // SF-216-28 : partage successoral notarié (FR, art. 816 et s. Cciv + 1592 CGI + 641 CGI).
    ['F-FA-PARTAGE-NOTARIAL', 'DIAGNOSTIC'],
    // SF-216-26 : présomption de paternité du mari et désaveu (FR, art. 312-316 Cciv + art. 333 al. 1).
    ['F-FA-PRESOMPTION-PATERNITE', 'DIAGNOSTIC'],
    // SF-216-30 : donation-partage (FR, art. 1075 à 1075-5 Cciv + art. 1078, 1078-1, 1080 + art. 912-928).
    ['F-FA-DONATION-PARTAGE', 'DIAGNOSTIC'],
    ['F-FA-16-communaute-universelle', 'DIAGNOSTIC'],
    ['F-FA-17-partage-judiciaire', 'DIAGNOSTIC'],
    ['F-FA-18-adoption', 'DIAGNOSTIC'],
    ['F-FA-19-autorite-parentale', 'DIAGNOSTIC'],
    ['F-FA-19-changement-residence', 'DIAGNOSTIC'],
    ['F-FA-19-desaccords-parentaux', 'DIAGNOSTIC'],
    ['F-FA-20-pacs-dissolution', 'DIAGNOSTIC'],
    ['F-FA-21-separation-corps', 'DIAGNOSTIC'],
    ['F-FA-22-indivision', 'DIAGNOSTIC'],
    ['F-FA-24-devolution-legale', 'DIAGNOSTIC'],
    ['F-FA-24-donation', 'DIAGNOSTIC'],
    ['F-FA-24-reserve-heriditaire', 'DIAGNOSTIC'],
    ['F-FA-24-partage-successoral', 'DIAGNOSTIC'],
    ['F-FA-24-indivision-successorale', 'DIAGNOSTIC'],
    ['F-FA-24-rapport-succession', 'DIAGNOSTIC'],
    ['F-FA-25-majeurs-proteges', 'DIAGNOSTIC'],
    ['F-FA-26-changement-etat-civil', 'DIAGNOSTIC'],
    ['F-FA-27-pma-gpa', 'DIAGNOSTIC'],
  ]);

  /** Expose THEMES_ORDERED au template (les statics ne sont pas accessibles directement). */
  readonly themesOrdered = DecisionToolsPanelComponent.THEMES_ORDERED;

  ngOnInit(): void {
    if (this.caseFileId) {
      this.loadVisibility(true);
      this.loadAlignments();
    }
    if (this.refreshService) {
      this.refreshService.refresh$
        .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
        .subscribe(() => {
          if (this.caseFileId) {
            this.loadVisibility(false);
            // F-228 SF-228-01 — re-fetch des 4 alignements à chaque run de
            // Synthèse enrichie (cohérence F-176 stricte). Le PUT statut
            // piste/pièce/risque/réponse F-94 ne déclenche PAS triggerRefresh
            // (cf. CA-12 F-192/F-194/F-195/F-196), donc le re-fetch ici ne
            // s'enclenche que post-analyse — comportement voulu.
            this.loadAlignments();
          }
        });
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['caseFileId'] && !changes['caseFileId'].firstChange && this.caseFileId) {
      this.loadVisibility(true);
      this.loadAlignments();
    }
    // F-244 SF-244-02 — les compteurs de pré-fill dépendent de `synthesis` ;
    // tout changement de cette entrée (post-analyse, override) doit ré-émettre
    // le total agrégé pour rafraîchir le badge de l'onglet « Décision ».
    if (changes['synthesis'] && !changes['synthesis'].firstChange) {
      this.emitPrefillTotal();
    }
  }

  /**
   * F-244 SF-244-02 — Calcule la somme des `getPrefillCount()` de tous les
   * outils visibles (always-on ∪ contextual) et l'émet via
   * `prefillTotalChange`. Les outils non instrumentés (`getPrefillCount`
   * absent) comptent 0 — `prefillCountFor` retourne alors `null`, neutralisé
   * en 0 ici (forward-compat).
   */
  private emitPrefillTotal(): void {
    const all = [...this.resolvedAlwaysOn(), ...this.resolvedContextual()];
    const total = all.reduce((sum, item) => sum + (this.prefillCountFor(item.toolId) ?? 0), 0);
    this.prefillTotalChange.emit(total);
  }

  /**
   * F-228 SF-228-01 — Charge les 4 alignements en parallèle via le loader
   * partagé `DecisionToolAlignmentsLoader` (forkJoin + fail-open par stream).
   * Remplace les 3 méthodes `loadRetainedPistes` / `loadPiecesAlignment` /
   * `loadRisquesAlignment` précédentes + ajoute `aiQuestionsAlignment`.
   *
   * <p>OnPush-safe : mutation via `signal.set()` qui déclenche la CD
   * nativement. Refresh exclusif au run de Synthèse enrichie (PUT statut
   * piste/pièce/risque + PUT réponse F-94 ne déclenchent PAS de refresh —
   * cohérence F-176 stricte).</p>
   */
  private loadAlignments(): void {
    this.alignmentsLoader.loadAll(this.caseFileId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (bundle) => {
          this.retainedPistes.set(bundle.retainedPistes);
          this.piecesAlignment.set(bundle.piecesAlignment);
          this.risquesAlignment.set(bundle.risquesAlignment);
          this.aiQuestionsAlignment.set(bundle.aiQuestionsAlignment);
        },
        error: () => {
          this.retainedPistes.set([]);
          this.piecesAlignment.set([]);
          this.risquesAlignment.set([]);
          this.aiQuestionsAlignment.set([]);
        },
      });
  }

  private loadVisibility(showSpinner: boolean): void {
    if (showSpinner) this.loading.set(true);
    this.caseFileService.getDecisionToolsVisibility(this.caseFileId).subscribe({
      next: (result) => {
        this.visibility.set(result);
        this.loading.set(false);
        this.recordPrefillSnapshot();
        // F-244 SF-244-02 — la liste des outils visibles vient de changer :
        // ré-émettre le total de pré-fill pour le badge de l'onglet « Décision ».
        this.emitPrefillTotal();
      },
      error: () => {
        this.loading.set(false);
        this.visibility.set({ alwaysOn: [], contextual: [], catalog: [] });
        // F-244 SF-244-02 — visibilité vide → aucun outil pré-remplissable.
        this.emitPrefillTotal();
        this.snackBar.open(
          'Impossible de charger les outils du dossier. Réessayez plus tard.',
          'Fermer',
          { duration: 4000 }
        );
      },
    });
  }

  resolveEntry(toolId: string): DecisionToolRegistryEntry | null {
    const entry = DecisionToolsPanelComponent.TOOL_REGISTRY.get(toolId);
    if (!entry) {
      // eslint-disable-next-line no-console
      console.warn(`[decisional-tools-panel] Unknown toolId: ${toolId}`);
      return null;
    }
    return entry;
  }

  /**
   * SF-238-01 — retourne le libellé humain affiché dans le chip du catalogue.
   * Fallback sur le `tool_id` brut si le composant n'est pas (encore) enregistré
   * (forward-compat). Le garde-fou CI `DecisionToolDisplayLabelIntegrityIT`
   * interdit toute entrée TOOL_REGISTRY sans `displayLabel`, donc en pratique
   * le fallback ne sert qu'aux `tool_id` orphelins en DB (qui sont eux-mêmes
   * bloqués par `DecisionToolVisibilityIntegrityIT` SF-164-01).
   */
  resolveDisplayLabel(toolId: string): string {
    return DecisionToolsPanelComponent.TOOL_REGISTRY.get(toolId)?.displayLabel ?? toolId;
  }

  /**
   * SF-238-02 — indique si l'activation manuelle de cet outil est en vol.
   * Utilisé par le template pour afficher le spinner et désactiver le chip.
   */
  isActivating(toolId: string): boolean {
    return this.activatingToolIds().has(toolId);
  }

  /**
   * SF-238-02 — déclenche l'activation manuelle d'un outil du catalogue :
   *   1. ajoute le toolId à `activatingToolIds` (UI : spinner + disabled),
   *   2. POST vers le backend (SF-238-03),
   *   3. sur succès → triggerRefresh() pour re-fetch la visibilité (l'outil
   *      migre catalog → contextual),
   *   4. sur erreur → MatSnackBar (409 = info, autre = erreur générique),
   *   5. retire le toolId de `activatingToolIds` dans tous les cas.
   *
   * Pas de blocage technique sur 409 (idempotent côté UX) : on déclenche un
   * refresh quand même pour s'assurer que la vue reflète bien l'état serveur.
   */
  activateManually(toolId: string): void {
    if (this.isActivating(toolId)) {
      return; // anti double-clic
    }
    this.markActivating(toolId, true);
    this.manualActivationService.activate(this.caseFileId, toolId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.markActivating(toolId, false);
          // Déclenche le re-fetch de la visibilité → l'outil migre dans contextual.
          if (this.refreshService) {
            this.refreshService.triggerRefresh();
          } else {
            // Si pas d'orchestrateur, on relance directement le GET local.
            this.loadVisibility(false);
          }
        },
        error: (err) => {
          this.markActivating(toolId, false);
          if (err?.status === 409) {
            this.snackBar.open(
              'Outil déjà activé pour ce dossier.',
              'Fermer',
              { duration: 3000 }
            );
            // 409 = état déjà à jour côté serveur, on rafraîchit quand même
            // pour cohérence visuelle.
            if (this.refreshService) {
              this.refreshService.triggerRefresh();
            } else {
              this.loadVisibility(false);
            }
            return;
          }
          this.snackBar.open(
            'Activation impossible. Réessayez plus tard.',
            'Fermer',
            { duration: 4000 }
          );
        },
      });
  }

  private markActivating(toolId: string, on: boolean): void {
    const next = new Set(this.activatingToolIds());
    if (on) {
      next.add(toolId);
    } else {
      next.delete(toolId);
    }
    this.activatingToolIds.set(next);
  }

  resolvedAlwaysOn(): { toolId: string; entry: DecisionToolRegistryEntry }[] {
    const v = this.visibility();
    if (!v) return [];
    return v.alwaysOn
      .map((toolId) => ({ toolId, entry: this.resolveEntry(toolId) }))
      .filter((x): x is { toolId: string; entry: DecisionToolRegistryEntry } => x.entry !== null);
  }

  resolvedContextual(): { toolId: string; entry: DecisionToolRegistryEntry }[] {
    const v = this.visibility();
    if (!v) return [];
    return v.contextual
      .map((toolId) => ({ toolId, entry: this.resolveEntry(toolId) }))
      .filter((x): x is { toolId: string; entry: DecisionToolRegistryEntry } => x.entry !== null);
  }

  /**
   * F-169 SF-169-01 : regroupe les outils résolus (always-on + contextual)
   * par thème métier. Un toolId sans entrée dans `THEME_BY_TOOL_ID` tombe
   * sur `DIAGNOSTIC` avec un `console.warn` pour signaler la dette.
   */
  themedTools(): Map<ThemeKey, { toolId: string; entry: DecisionToolRegistryEntry }[]> {
    const all = [...this.resolvedAlwaysOn(), ...this.resolvedContextual()];
    const byTheme = new Map<ThemeKey, { toolId: string; entry: DecisionToolRegistryEntry }[]>();
    for (const item of all) {
      let theme = DecisionToolsPanelComponent.THEME_BY_TOOL_ID.get(item.toolId);
      if (!theme) {
        // eslint-disable-next-line no-console
        console.warn(`[decisional-tools-panel] toolId sans mapping thème : ${item.toolId} → fallback DIAGNOSTIC`);
        theme = 'DIAGNOSTIC';
      }
      const list = byTheme.get(theme) ?? [];
      list.push(item);
      byTheme.set(theme, list);
    }
    return byTheme;
  }

  isEmpty(): boolean {
    const v = this.visibility();
    if (!v) return false;
    return v.alwaysOn.length === 0 && v.contextual.length === 0;
  }

  /**
   * SF-159-02 — capture l'état `prefillCount` + métadonnées pour chaque outil résolu
   * et le passe au service progress. La 1re invocation initialise sans déclencher
   * flash ni toast ; les suivantes calculent le diff et signalent les enrichissements.
   */
  private recordPrefillSnapshot(): void {
    if (!this.progressService) return;
    const snapshot = new Map<string, number>();
    const metadata = new Map<string, { label: string; icon: string }>();
    const all = [...this.resolvedAlwaysOn(), ...this.resolvedContextual()];
    for (const item of all) {
      snapshot.set(item.toolId, this.prefillCountFor(item.toolId) ?? 0);
      metadata.set(item.toolId, this.cardMetadataFor(item.entry, item.toolId));
    }
    this.progressService.recordSnapshot(snapshot, metadata);
  }

  componentInputsFor(entry: DecisionToolRegistryEntry): Record<string, unknown> {
    return entry.inputs({
      caseFileId: this.caseFileId,
      // F-197 SF-197-02 : la synthèse passée aux outils est augmentée avec
      // l'override avocat (single-value typeLitige Travail FR / typeProcedure
      // Immigration). L'override prend précédence sur la valeur IA pour le
      // pré-remplissage. Cohérence F-176 stricte : l'override n'est consommé
      // qu'à l'instant de l'instanciation des outils, le PUT n'est jamais
      // suivi d'un refresh frontend.
      synthesis: this.augmentSynthesisWithOverride(this.synthesis, this.typeLitigeOverride),
      workspaceCountry: this.workspaceCountry,
      caseFileTitle: this.caseFileTitle,
      procedureChecks: this.procedureChecks,
      aiQuestions: this.aiQuestions,
      // F-228 SF-228-01 : 4 alignements chargés en parallèle via
      // DecisionToolAlignmentsLoader (cf. `loadAlignments()`).
      pistesRetenues: this.retainedPistes(),
      piecesAlignment: this.piecesAlignment(),
      risquesAlignment: this.risquesAlignment(),
      aiQuestionsAlignment: this.aiQuestionsAlignment(),
      // F-197 SF-197-02 : exposé brut au cas où un outil veut raisonner sur
      // la présence/absence d'un override (badge "modifié par vous" UI).
      typeLitigeOverride: this.typeLitigeOverride,
    });
  }

  /**
   * F-197 SF-197-02 — Helper : retourne une copie de la synthèse dans
   * laquelle :
   * <ul>
   *   <li>{@code travailExtractedData.typeLitigeAvocatOverride} est posé à la
   *       valeur de l'override Travail FR (si présent).</li>
   *   <li>{@code immigrationExtractedData.typeProcedureAvocatOverride} est
   *       posé à la valeur de l'override Immigration (si présent).</li>
   * </ul>
   *
   * <p>Les outils décisionnels qui veulent privilégier l'override appellent
   * en interne {@code aiData.typeLitigeAvocatOverride ?? aiData.typeLitigeDetecte}.
   * Aucun outil existant n'est modifié par cette SF — seul le contrat de
   * passage est étendu (tools peuvent ignorer le champ s'ils ne l'utilisent
   * pas, no-op gracieux).</p>
   *
   * <p>Si {@code synthesis} est null ou si {@code override} est null, retourne
   * la synthèse telle quelle (no-op).</p>
   */
  private augmentSynthesisWithOverride(
    synthesis: any | null,
    override: import('../../core/models/type-litige-override.model').TypeLitigeOverrideResponse | null,
  ): any | null {
    if (!synthesis || !override) return synthesis;
    const travailOverride = override.typeLitigeAvocat ?? null;
    const immigrationOverride = override.typeProcedureAvocat ?? null;
    if (!travailOverride && !immigrationOverride) return synthesis;
    const augmented: any = { ...synthesis };
    if (travailOverride && synthesis.travailExtractedData) {
      augmented.travailExtractedData = {
        ...synthesis.travailExtractedData,
        typeLitigeAvocatOverride: travailOverride,
      };
    } else if (travailOverride) {
      augmented.travailExtractedData = { typeLitigeAvocatOverride: travailOverride };
    }
    if (immigrationOverride && synthesis.immigrationExtractedData) {
      augmented.immigrationExtractedData = {
        ...synthesis.immigrationExtractedData,
        typeProcedureAvocatOverride: immigrationOverride,
      };
    } else if (immigrationOverride) {
      augmented.immigrationExtractedData = { typeProcedureAvocatOverride: immigrationOverride };
    }
    return augmented;
  }

  /**
   * F-194 SF-194-02 — Calcule le badge pièces (D/O/N) à afficher sur la card
   * du panel pour un toolId. Fait à partir du signal `piecesAlignment` filtré
   * sur `toolIdsCibles`. Pattern miroir `retainedPistesBadgeFor` (F-192).
   * Retourne `null` si aucune pièce n'est mappée à cet outil — la pill est
   * silencieusement masquée par le card via `showPiecesBadge`.
   */
  piecesBadgeFor(toolId: string): PiecesBadgeInput | null {
    const all = this.piecesAlignment();
    const filtered = all.filter(p => Array.isArray(p.toolIdsCibles) && p.toolIdsCibles.includes(toolId));
    const badge = computePiecesBadge(filtered);
    if (badge.kind === 'none') return null;
    return badge;
  }

  /**
   * F-195 SF-195-02 — Calcule le badge risques (V/E) à afficher sur la card
   * du panel pour un toolId. Fait à partir du signal `risquesAlignment`
   * filtré sur `toolIdsCibles`. Pattern miroir `piecesBadgeFor` (F-194).
   * Retourne `null` si aucun risque n'est mappé à cet outil — la pill est
   * silencieusement masquée par le card via `showRisquesBadge`.
   */
  risquesBadgeFor(toolId: string): RisquesBadgeInput | null {
    const all = this.risquesAlignment();
    const filtered = all.filter(r => Array.isArray(r.toolIdsCibles) && r.toolIdsCibles.includes(toolId));
    const badge = computeRisquesBadge(filtered);
    if (badge.kind === 'none') return null;
    return badge;
  }

  /**
   * F-253 SF-253-02 — Calcule le compteur des risques au statut `A_CREUSER`
   * mappés sur un outil donné. Alimente la pill secondaire `🔍 N à creuser`
   * sur la card outil. Retourne {@code null} si aucun à creuser (la pill est
   * masquée par le card via `showRisquesACreuserPill`).
   *
   * <p>Pourquoi {@code null} et pas {@code 0} : le card utilise un check
   * 3-valued (null = composant non instrumenté, 0 = pas de À_CREUSER pour cet
   * outil, N > 0 = afficher). Le panel renvoie toujours un nombre puisqu'il
   * EST instrumenté F-253.</p>
   */
  risquesACreuserCountFor(toolId: string): number {
    return getRisquesACreuserCountFor(this.risquesAlignment(), toolId);
  }

  /**
   * F-192 SF-192-02 — Badge piste 🟢 retenue à afficher sur la card du panel
   * pour un toolId. Lit le static `getRetainedPistesBadge` (s'il existe) du
   * composant outil. Retourne `null` si :
   *   - le composant n'est pas instrumenté SF-192-02 (forward-compat),
   *   - ou aucune piste ne mappe ce toolId,
   *   - ou le static throw (log 1× via try/catch).
   */
  retainedPistesBadgeFor(toolId: string): RetainedPistesBadge | null {
    const entry = DecisionToolsPanelComponent.TOOL_REGISTRY.get(toolId);
    if (!entry) return null;
    const candidate = entry.component as unknown as {
      getRetainedPistesBadge?: (input: { pistesRetenues?: RetainedPisteAlignment[] }) => RetainedPistesBadge;
    };
    if (typeof candidate.getRetainedPistesBadge !== 'function') return null;
    try {
      const badge = candidate.getRetainedPistesBadge({ pistesRetenues: this.retainedPistes() });
      if (!badge || badge.kind === 'none' || badge.count <= 0) return null;
      return badge;
    } catch {
      return null;
    }
  }

  /**
   * F-177 SF-177-11 : metadata d'affichage card pour un toolId.
   * Lit `TOOL_LABEL` + `TOOL_ICON` exposés en statics par le composant outil
   * (instrumentés via SF-177-03/03b/05/07). Fallback sur `extension` + `toolId`
   * si un composant ne les expose pas (forward-compat).
   */
  cardMetadataFor(entry: DecisionToolRegistryEntry, toolId: string): { label: string; icon: string } {
    const meta = getToolMetadata(entry.component);
    return meta ?? { label: toolId, icon: 'extension' };
  }

  /**
   * F-177 SF-177-12 — Calcule le nombre de champs pré-remplis par l'IA pour
   * un toolId. Utilisé par le template via `[prefillCount]` sur la card.
   *
   * Retourne `null` si le composant n'expose pas le static `getPrefillCount`
   * (composant non instrumenté → la card masque le badge silencieusement).
   *
   * Le contexte construit ici reflète les inputs réels passés via
   * `componentInputsFor` (TOOL_REGISTRY) — `aiData` est mappé selon le
   * domaine détecté dans la synthèse, `triggerEvents` et `workspaceCountry`
   * sont exposés en top-level pour les composants qui en dépendent
   * (immigration-title-decision, immigration-work-right).
   */
  prefillCountFor(toolId: string): number | null {
    const entry = DecisionToolsPanelComponent.TOOL_REGISTRY.get(toolId);
    if (!entry) return null;
    const synthesis = this.synthesis;
    const aiData =
      synthesis?.travailExtractedData
      ?? synthesis?.immigrationExtractedData
      ?? synthesis?.familleExtractedData
      ?? null;
    const input: PrefillCountInput = {
      aiData,
      procedureChecks: this.procedureChecks ?? [],
      aiQuestions: this.aiQuestions ?? [],
      piecesManquantes: synthesis?.piecesManquantesDetails ?? [],
      triggerEvents: synthesis?.immigrationTriggerEvents ?? [],
      workspaceCountry: this.workspaceCountry,
      synthesis,
    };
    return getToolPrefillCount(entry.component, input);
  }

  /**
   * F-177 SF-177-11 : ouvre l'outil dans un MatDialog 90vw/90vh.
   * Le composant outil est instancié dans le modal avec `forceExpanded: true`
   * pour qu'il apparaisse déplié immédiatement. Pas de bouton Save dans cette
   * étape (les composants gèrent leur propre persistence) — onSave restera
   * undefined → bouton caché.
   */
  openTool(toolId: string, entry: DecisionToolRegistryEntry): void {
    const meta = this.cardMetadataFor(entry, toolId);
    const inputs = {
      ...this.componentInputsFor(entry),
      forceExpanded: true,
    };
    this.modalService.open({
      toolId,
      title: meta.label,
      icon: meta.icon,
      component: entry.component,
      inputs,
      viewContainerRef: this.vcr,
    });
  }
}
