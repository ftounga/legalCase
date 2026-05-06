import {
  Component,
  DestroyRef,
  Input,
  OnChanges,
  OnInit,
  Optional,
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
import { RetainedPisteAlignmentService } from '../../core/services/retained-piste-alignment.service';
import { RetainedPisteAlignment } from '../../core/models/retained-piste-alignment.model';
import { PieceManquanteAlignmentService } from '../../core/services/piece-manquante-alignment.service';
import { PieceManquanteAlignment } from '../../core/models/piece-manquante-alignment.model';
import { RisqueAlignmentService } from '../../core/services/risque-alignment.service';
import { RisqueAlignment } from '../../core/models/risque-alignment.model';
import { RetainedPistesBadge } from '../immigration-title-decision-section/immigration-title-decision-section.component';
import { DecisionToolCardComponent, PiecesBadgeInput, RisquesBadgeInput } from './decision-tool-card/decision-tool-card.component';
import {
  computePiecesBadge,
  piecesObtenuesFor,
} from './piece-manquante-badge.helper';
import {
  computeRisquesBadge,
  risquesValidesFor,
} from './risque-badge.helper';
import { DecisionToolModalService } from './decision-tool-modal/decision-tool-modal.service';
import { DecisionalToolsProgressBannerComponent } from './decisional-tools-progress-banner.component';
import { DecisionalToolsProgressService } from './decisional-tools-progress.service';
import { getToolMetadata, getToolPrefillCount, PrefillCountInput } from './decision-tool.contract';
import { AncienneteSectionComponent } from '../anciennete-section/anciennete-section.component';
import { LicenciementSectionComponent } from '../licenciement-section/licenciement-section.component';
import { RuptureConvSectionComponent } from '../rupture-conv-section/rupture-conv-section.component';
import { RuptureConvIndemniteSectionComponent } from '../rupture-conv-indemnite-section/rupture-conv-indemnite-section.component';
import { RuptureAmiableInfoSectionComponent } from '../rupture-amiable-info-section/rupture-amiable-info-section.component';
import { IndemniteComparatifSectionComponent } from '../indemnite-comparatif-section/indemnite-comparatif-section.component';
import { PrudhomeFicheSectionComponent } from '../prudhome-fiche-section/prudhome-fiche-section.component';
import { TribunalTravailFicheSectionComponent } from '../tribunal-travail-fiche-section/tribunal-travail-fiche-section.component';
import { PartageImmobilierSectionComponent } from '../partage-immobilier-section/partage-immobilier-section.component';
import { CalendrierGardeSectionComponent } from '../calendrier-garde-section/calendrier-garde-section.component';
import { DivorceChecklistSectionComponent } from '../divorce-checklist-section/divorce-checklist-section.component';
import { ImmigrationTitleDecisionSectionComponent } from '../immigration-title-decision-section/immigration-title-decision-section.component';
import { ImmigrationRecoursSectionComponent } from '../immigration-recours-section/immigration-recours-section.component';
import { ImmigrationWorkRightSectionComponent } from '../immigration-work-right-section/immigration-work-right-section.component';
import { ImmigrationChecklistSectionComponent } from '../immigration-checklist-section/immigration-checklist-section.component';
import { HarcelementLicenciementNulSectionComponent } from '../harcelement-licenciement-nul-section/harcelement-licenciement-nul-section.component';
import { LicenciementNulDetectionSectionComponent } from '../licenciement-nul-detection-section/licenciement-nul-detection-section.component';
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

export interface DecisionToolContext {
  caseFileId: string;
  synthesis: any | null;
  workspaceCountry: string;
  caseFileTitle: string;
  procedureChecks: any[];
  aiQuestions: any[];
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
}

export interface DecisionToolRegistryEntry {
  component: Type<unknown>;
  inputs: (ctx: DecisionToolContext) => Record<string, unknown>;
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
  private readonly retainedPistesService = inject(RetainedPisteAlignmentService, { optional: true });
  private readonly piecesAlignmentService = inject(PieceManquanteAlignmentService, { optional: true });
  private readonly risqueAlignmentService = inject(RisqueAlignmentService, { optional: true });
  private readonly modalService = inject(DecisionToolModalService);
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

  readonly loading = signal(false);
  readonly visibility = signal<VisibleToolSet | null>(null);

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
   * Registre des outils décisionnels. Chaque entrée déclare son composant
   * Angular et une closure qui mappe le contexte du dossier vers les inputs
   * exacts que ce composant attend. Les tool_id non présents ici sont
   * skippés avec un warning (forward-compat SF-IA-04-02).
   */
  static readonly TOOL_REGISTRY: ReadonlyMap<string, DecisionToolRegistryEntry> =
    new Map<string, DecisionToolRegistryEntry>([
      ['F-DT-04-fiche-prudhomale', {
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
      ['F-DT-06-requete-tribunal-travail', {
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
        component: AncienneteSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          aiData: ctx.synthesis?.travailExtractedData,
        }),
      }],
      ['F-DT-08-licenciement-validity', {
        component: LicenciementSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.licenciementValidityDetection,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-DT-09-comparateur-indemnites', {
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
        component: RuptureConvSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          aiData: ctx.synthesis?.ruptureConvValidityDetection,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-132-rupture-conv-indemnite', {
        component: RuptureConvIndemniteSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          synthesis: ctx.synthesis,
        }),
      }],
      ['F-DT-11-harcelement-licenciement-nul', {
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
        }),
      }],
      ['F-DT-16-licenciement-nul-detection', {
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
        }),
      }],
      ['F-DT-12-discrimination-dommages-interets', {
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
        }),
      }],
      ['F-DT-13-licenciement-economique', {
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
        }),
      }],
      ['F-DT-15-inaptitude', {
        component: InaptitudeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-155-04-A2 : sources IA (pré-fill + validation F-IA-03)
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-DT-19-heures-sup', {
        component: HeuresSupSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-155-04-A3 : pré-fill IA + validation F-IA-03.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-DT-17-indemnite-precarite-cdd', {
        component: IndemnitePrecariteCddSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-17-02 : pré-fill IA salaire mensuel + alertes F-IA-03.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-DT-26-conges-payes-indemnite', {
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
        }),
      }],
      ['F-DT-18-fin-mission-interim', {
        component: FinMissionInterimSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-18-02 : pré-fill IA salaire mensuel + alertes F-IA-03.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-DT-32-documents-fin-contrat', {
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
        }),
      }],
      ['F-DT-21-travail-dissimule', {
        component: TravailDissimuleSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-DT-21-02 : pré-fill IA (salaireBrutMensuel) + validation F-IA-03.
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-DT-25-indemnite-preavis', {
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
        }),
      }],
      ['F-DT-23-requalification-interim-cdi', {
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
        }),
      }],
      ['F-DT-24-non-concurrence', {
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
        }),
      }],
      ['F-132-rupture-amiable-info', {
        component: RuptureAmiableInfoSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
        }),
      }],
      ['F-FA-05-partage-immobilier', {
        component: PartageImmobilierSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          liquidationCommunaute: ctx.synthesis?.liquidationCommunaute,
          // SF-155-20 : pré-fill IA via FamilleExtractedData (valeurImmeuble + capitalRestantDu).
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-FA-15-recompenses', {
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
        }),
      }],
      ['F-FA-06-calendrier-garde', {
        component: CalendrierGardeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiModeGardeDetaille: ctx.synthesis?.pensionAlimentaireEstimate?.modeGardeDetaille,
          synthesis: ctx.synthesis,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-FA-07-checklist-divorce', {
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
      ['F-IM-01-checklist-pieces', {
        component: ImmigrationChecklistSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          caseFileTitle: ctx.caseFileTitle,
          inferredChecklistType: ctx.synthesis?.immigrationExtractedData?.inferredChecklistType ?? null,
        }),
      }],
      ['F-IM-05-arbre-decisionnel-titre', {
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
        component: OqtfAvecDelaiSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-155-04-B1 : pré-fill IA + alertes de cohérence F-IA-03.
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-IM-08-oqtf-sans-delai-fr', {
        component: OqtfSansDelaiSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-155-04-B2 : pré-fill IA + validation F-IA-03 (urgence 48h).
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-IM-08-annexe13-be', {
        component: Annexe13BeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-155-04-C : pré-fill IA + validation F-IA-03 (4 champs BE).
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-IM-08-08 : référés administratifs L.521-1 / L.521-2 (FR uniquement).
      ['F-IM-08-referes-admin-fr', {
        component: ReferesAdminSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-DT-35-02 : contestation ARE / France Travail (FR uniquement).
      ['F-DT-35-contestation-are-fr', {
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
        }),
      }],
      ['F-DT-27-motif-grave-be', {
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
        }),
      }],
      ['F-DT-28-avantages-conventionnels-be', {
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
        }),
      }],
      // SF-DT-29-02 : crédit-temps / interruption de carrière BE
      // (CCT 103 + AR 29/10/1997). BE uniquement.
      ['F-DT-29-credit-temps-be', {
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
        }),
      }],
      // SF-DT-14-02 : PSE — critères de validité (FR uniquement,
      // L.1233-24-1 + L.1233-30 + L.1233-57-2 + L.1233-61 + L.1235-7-1).
      // tool_id aligné avec la migration 164.
      ['F-DT-14-pse-validite', {
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
        }),
      }],
      // SF-DT-33-02 : Accident du travail / Maladie professionnelle (FR uniquement,
      // CSS L.411-1 / L.461-1 / L.434-2 + L.142-2 + R.142-1 + R.441-13 + R.461-9).
      // 3 dispositifs : RECONNAISSANCE_AT / RECONNAISSANCE_MP / CONTESTATION_TAUX_IPP.
      // tool_id aligné avec la migration 175.
      ['F-DT-33-at-mp', {
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
        }),
      }],
      ['F-IM-14-40ter-familial-belge-be', {
        component: Belgian40terSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // SF-IM-14-08 : pré-fill IA gracieux (lienFamilial, regroupantBelge,
          // revenusNetsMensuels, dateDepotDemande) + validation F-IA-03.
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
        }),
      }],
      ['F-IM-14-9bis-humanitaire-be', {
        component: Belgian9bisSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
        }),
      }],
      ['F-IM-14-9ter-medical-be', {
        component: Belgian9terSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
        }),
      }],
      ['F-IM-14-40bis-cohabitant-ue-be', {
        component: BelgianCohabitantUeBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-IM-09-aes-metiers-tension', {
        component: AesMetiersTensionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-IM-09-aes-famille', {
        component: AesFamilleSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-FA-08-divorce-alteration', {
        component: DivorceAlterationSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-FA-09-divorce-faute', {
        component: DivorceFauteSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
        }),
      }],
      ['F-FA-10-divorce-accepte', {
        component: DivorceAccepteSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-FA-12-mesures-provisoires', {
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
        component: RevisionsPostDivorceSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-14-02 : ordonnance de protection FR (art. 515-9 Cciv).
      ['F-FA-14-ordonnance-protection', {
        component: OrdonnanceProtectionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-136-travail-procedure', {
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
        component: AutoriteParentaleSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-FA-19-changement-residence', {
        component: ChangementResidenceSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-FA-19-desaccords-parentaux', {
        component: DesaccordsParentauxSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-FA-22-indivision', {
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
        component: ChangementEtatCivilSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-21-02 : séparation de corps + conversion divorce FR (art. 296+306 Cciv).
      ['F-FA-21-separation-corps', {
        component: SeparationCorpsSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-IM-11-02 : changement de statut CESEDA (FR uniquement,
      // art. L.421+ + R.5221). tool_id aligné migration 170.
      ['F-IM-11-changement-statut', {
        component: ChangementStatutSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-IM-13-02 : naturalisation française FR — 6 voies Cciv 21-15+.
      ['F-IM-13-naturalisation', {
        component: NaturalisationSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-IM-19-02 : mineurs étrangers FR — MNA / L.435-3 / DCEM / TIR.
      ['F-IM-19-mineurs', {
        component: MineursImmigrationSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-IM-20-02 : mesures d'éloignement avancées FR — Expulsion + IRTF + IAT.
      ['F-IM-20-mesures-eloignement', {
        component: MesuresEloignementSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-IM-12-02 : asile avancé FR — Dublin III / accélérée / réexamen / apatridie / PS.
      ['F-IM-12-asile-avance', {
        component: AsileAvanceSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-17-02 : partage judiciaire FR (art. 840+ Cciv + 1364+ + 1366 CPC).
      // tool_id aligné avec la migration 169.
      ['F-FA-17-partage-judiciaire', {
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
        component: ReserveHeriditaireSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-FA-24-10 : partage successoral FR.
      ['F-FA-24-partage-successoral', {
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
        component: RapportSuccessionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // SF-IM-17-02 : régime algérien FR.
      ['F-IM-17-regime-algerien', {
        component: RegimeAlgerienSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      // ===================================================================
      // SF-164-01 : entrées rétroactives pour composants existants seedés
      // en DB sans entrée registry (régression silencieuse vague 2026-04-24).
      // ===================================================================
      ['F-DT-03-prescription-litige', {
        component: CaseDeadlinesSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
        }),
      }],
      ['F-DT-31-transaction', {
        component: TransactionSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.travailExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-IM-09-aes-etudiant', {
        component: AesEtudiantSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-IM-09-aes-humanitaire', {
        component: AesHumanitaireSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.immigrationExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
        }),
      }],
      ['F-FA-11-desunion-irremediable-be', {
        component: DivorceDesunionBeSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          aiData: ctx.synthesis?.familleExtractedData,
          procedureChecks: ctx.procedureChecks,
          aiQuestions: ctx.aiQuestions,
          piecesManquantes: ctx.synthesis?.piecesManquantesDetails,
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
    ['F-DT-28-avantages-conventionnels-be', 'INDEMNITES'],
    ['F-DT-31-transaction', 'INDEMNITES'],
    ['F-DT-35-contestation-are-fr', 'INDEMNITES'],
    ['F-132-rupture-conv-indemnite', 'INDEMNITES'],
    ['F-FA-15-recompenses', 'INDEMNITES'],

    // ── Validité & contestation ────────────────────────────────────────
    ['F-DT-08-licenciement-validity', 'VALIDITE'],
    ['F-DT-10-rupture-conv-validity', 'VALIDITE'],
    ['F-DT-11-harcelement-licenciement-nul', 'VALIDITE'],
    ['F-DT-13-licenciement-economique', 'VALIDITE'],
    ['F-DT-14-pse-validite', 'VALIDITE'],
    ['F-DT-16-licenciement-nul-detection', 'VALIDITE'],
    ['F-DT-22-requalification-cdd-cdi', 'VALIDITE'],
    ['F-DT-23-requalification-interim-cdi', 'VALIDITE'],
    ['F-DT-24-non-concurrence', 'VALIDITE'],
    ['F-DT-27-motif-grave-be', 'VALIDITE'],
    ['F-DT-30-protection-rp', 'VALIDITE'],
    ['F-FA-08-divorce-alteration', 'VALIDITE'],
    ['F-FA-09-divorce-faute', 'VALIDITE'],
    ['F-FA-10-divorce-accepte', 'VALIDITE'],
    ['F-FA-11-desunion-irremediable-be', 'VALIDITE'],
    ['F-FA-18-contestation-paternite', 'VALIDITE'],
    ['F-FA-18-recherche-paternite', 'VALIDITE'],
    ['F-FA-18-reconnaissance-paternelle', 'VALIDITE'],
    ['F-FA-18-possession-etat', 'VALIDITE'],
    ['F-FA-24-testament-validite', 'VALIDITE'],

    // ── Délais & procédure ─────────────────────────────────────────────
    ['F-DT-03-prescription-litige', 'DELAIS'],
    ['F-DT-29-credit-temps-be', 'DELAIS'],
    ['F-DT-33-at-mp', 'DELAIS'],
    ['F-DT-34-refere-prudhomal', 'DELAIS'],
    ['F-FA-12-mesures-provisoires', 'DELAIS'],
    ['F-FA-13-revisions-post-divorce', 'DELAIS'],
    ['F-FA-14-ordonnance-protection', 'DELAIS'],
    ['F-FA-23-ordonnance-requete', 'DELAIS'],
    ['F-IM-06-recours', 'DELAIS'],
    ['F-IM-08-oqtf-avec-delai-fr', 'DELAIS'],
    ['F-IM-08-oqtf-sans-delai-fr', 'DELAIS'],
    ['F-IM-08-referes-admin-fr', 'DELAIS'],
    ['F-IM-08-annexe13-be', 'DELAIS'],
    ['F-IM-20-mesures-eloignement', 'DELAIS'],
    ['F-136-travail-procedure', 'DELAIS'],

    // ── Documents ──────────────────────────────────────────────────────
    ['F-DT-04-fiche-prudhomale', 'DOCUMENTS'],
    ['F-DT-06-requete-tribunal-travail', 'DOCUMENTS'],
    ['F-DT-32-documents-fin-contrat', 'DOCUMENTS'],
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
      this.loadRetainedPistes();
      this.loadPiecesAlignment();
      this.loadRisquesAlignment();
    }
    if (this.refreshService) {
      this.refreshService.refresh$
        .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
        .subscribe(() => {
          if (this.caseFileId) {
            this.loadVisibility(false);
            // F-192 SF-192-02 — re-fetch alignement à chaque run de Synthèse
            // enrichie (cohérence F-176 stricte). Le PUT statut piste ne
            // déclenche PAS triggerRefresh (cf. CA-12), donc le re-fetch ici
            // ne s'enclenche que post-analyse — comportement voulu.
            this.loadRetainedPistes();
            // F-194 SF-194-02 — idem pour les pièces. PUT statut pièce ne
            // déclenche PAS de refresh, seul le run de Synthèse enrichie le fait.
            this.loadPiecesAlignment();
            // F-195 SF-195-02 — idem pour les risques. PUT statut risque ne
            // déclenche PAS de refresh, seul le run de Synthèse enrichie le fait.
            this.loadRisquesAlignment();
          }
        });
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['caseFileId'] && !changes['caseFileId'].firstChange && this.caseFileId) {
      this.loadVisibility(true);
      this.loadRetainedPistes();
      this.loadPiecesAlignment();
      this.loadRisquesAlignment();
    }
  }

  /**
   * F-192 SF-192-02 — Charge l'alignement persisté côté backend. Fail-open :
   * `[]` si endpoint indisponible (le service log un warn). OnPush-safe :
   * mutation via `signal.set()` qui déclenche la CD nativement.
   */
  private loadRetainedPistes(): void {
    if (!this.retainedPistesService) {
      this.retainedPistes.set([]);
      return;
    }
    this.retainedPistesService.getForCaseFile(this.caseFileId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: list => this.retainedPistes.set(list),
        error: () => this.retainedPistes.set([]),
      });
  }

  /**
   * F-194 SF-194-02 — Charge l'alignement pièces ↔ outils. Fail-open : `[]`
   * si endpoint indisponible (le service log un warn). OnPush-safe :
   * mutation via `signal.set()` qui déclenche la CD nativement.
   */
  private loadPiecesAlignment(): void {
    if (!this.piecesAlignmentService) {
      this.piecesAlignment.set([]);
      return;
    }
    this.piecesAlignmentService.getForCaseFile(this.caseFileId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: list => this.piecesAlignment.set(list),
        error: () => this.piecesAlignment.set([]),
      });
  }

  /**
   * F-195 SF-195-02 — Charge l'alignement risques ↔ outils. Fail-open : `[]`
   * si endpoint indisponible (le service log un warn). OnPush-safe :
   * mutation via `signal.set()` qui déclenche la CD nativement.
   */
  private loadRisquesAlignment(): void {
    if (!this.risqueAlignmentService) {
      this.risquesAlignment.set([]);
      return;
    }
    this.risqueAlignmentService.getForCaseFile(this.caseFileId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: list => this.risquesAlignment.set(list),
        error: () => this.risquesAlignment.set([]),
      });
  }

  private loadVisibility(showSpinner: boolean): void {
    if (showSpinner) this.loading.set(true);
    this.caseFileService.getDecisionToolsVisibility(this.caseFileId).subscribe({
      next: (result) => {
        this.visibility.set(result);
        this.loading.set(false);
        this.recordPrefillSnapshot();
      },
      error: () => {
        this.loading.set(false);
        this.visibility.set({ alwaysOn: [], contextual: [], catalog: [] });
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
      synthesis: this.synthesis,
      workspaceCountry: this.workspaceCountry,
      caseFileTitle: this.caseFileTitle,
      procedureChecks: this.procedureChecks,
      aiQuestions: this.aiQuestions,
      // F-192 SF-192-02 : alignement chargé via RetainedPisteAlignmentService.
      pistesRetenues: this.retainedPistes(),
      // F-194 SF-194-02 : alignement chargé via PieceManquanteAlignmentService.
      piecesAlignment: this.piecesAlignment(),
      // F-195 SF-195-02 : alignement chargé via RisqueAlignmentService.
      risquesAlignment: this.risquesAlignment(),
    });
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
