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
import { CongesPayesArretMaladieSectionComponent } from '../conges-payes-arret-maladie-section/conges-payes-arret-maladie-section.component';
import { PriseActeRuptureSectionComponent } from '../prise-acte-rupture-section/prise-acte-rupture-section.component';
// SF-212-02 : outil F-DT-36 licenciement pour faute grave / faute lourde (FR uniquement).
import { LicenciementFauteGraveLourdSectionComponent } from '../licenciement-faute-grave-lourd-section/licenciement-faute-grave-lourd-section.component';
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
    ['F-DT-21-travail-dissimule', 'INDEMNITES'],
    ['F-DT-25-indemnite-preavis', 'INDEMNITES'],
    ['F-DT-26-conges-payes-indemnite', 'INDEMNITES'],
    // SF-206-04 : rappel de CP acquis pendant arrêt maladie (FR, L.3141-5 /
    // L.3141-5-1 CT, loi 22/04/2024). Groupe F-169 « Rappels et indemnités
    // salariales » — c'est un rappel de droits, pas une rupture.
    ['F-DT-75-conges-payes-arret-maladie', 'INDEMNITES'],
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
    ['F-DT-30-protection-rp', 'VALIDITE'],
    // SF-214-01 : F-IM-25 étranger malade L.425-9 CESEDA (FR) — analyseur d'éligibilité.
    // Thème VALIDITE (analyse d'éligibilité protection médicale, CONTEXTUAL FR).
    ['F-IM-25-etranger-malade-l4259-fr', 'VALIDITE'],
    // SF-207-06b : RCC BE — conditions d'éligibilité (analyseur 4 régimes).
    // Thème VALIDITE (analyse d'éligibilité, cohérent avec les autres analyseurs).
    ['rcc-be-conditions', 'VALIDITE'],
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
