import {
  Component,
  DestroyRef,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  Type,
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
import { OrdonnanceRequeteSectionComponent } from '../ordonnance-requete-section/ordonnance-requete-section.component';
import { ChangementStatutSectionComponent } from '../changement-statut-section/changement-statut-section.component';
import { NaturalisationSectionComponent } from '../naturalisation-section/naturalisation-section.component';
import { MineursImmigrationSectionComponent } from '../mineurs-immigration-section/mineurs-immigration-section.component';
import { MesuresEloignementSectionComponent } from '../mesures-eloignement-section/mesures-eloignement-section.component';
import { AsileAvanceSectionComponent } from '../asile-avance-section/asile-avance-section.component';
import { PartageJudiciaireSectionComponent } from '../partage-judiciaire-section/partage-judiciaire-section.component';
import { CommunauteUniverselleSectionComponent } from '../communaute-universelle-section/communaute-universelle-section.component';

export interface DecisionToolContext {
  caseFileId: string;
  synthesis: any | null;
  workspaceCountry: string;
  caseFileTitle: string;
  procedureChecks: any[];
  aiQuestions: any[];
}

export interface DecisionToolRegistryEntry {
  component: Type<unknown>;
  inputs: (ctx: DecisionToolContext) => Record<string, unknown>;
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
  ],
  templateUrl: './decisional-tools-panel.component.html',
  styleUrls: ['./decisional-tools-panel.component.scss'],
})
export class DecisionToolsPanelComponent implements OnInit, OnChanges {
  private readonly caseFileService = inject(CaseFileService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);
  private readonly refreshService = inject(CaseDashboardRefreshService, { optional: true });

  @Input({ required: true }) caseFileId!: string;
  @Input() synthesis: any | null = null;
  @Input() workspaceCountry = 'FRANCE';
  @Input() caseFileTitle = '';
  @Input() procedureChecks: any[] = [];
  @Input() aiQuestions: any[] = [];

  readonly loading = signal(false);
  readonly visibility = signal<VisibleToolSet | null>(null);

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
        }),
      }],
      ['F-DT-06-requete-tribunal-travail', {
        component: TribunalTravailFicheSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          caseFileTitle: ctx.caseFileTitle,
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
      // SF-FA-16-02 : communauté universelle FR (art. 1526 + 1527 al. 2 Cciv).
      // 4ᵉ régime matrimonial — 2 dispositifs : VALIDITE_CONVENTION +
      // LIQUIDATION_DECES (avec/sans CAI + action en retranchement).
      // tool_id aligné avec la migration 177.
      ['F-FA-16-communaute-universelle', {
        component: CommunauteUniverselleSectionComponent,
        inputs: (ctx) => ({
          caseFileId: ctx.caseFileId,
          workspaceCountry: ctx.workspaceCountry,
          // Pré-fill IA gracieux (contratNotarieDetected, enfantsNonCommunsDetected,
          // clauseAttributionIntegraleDetected, valeurCommunauteEurDetectee)
          // + validation F-IA-03 (CONTRAT_NOTARIE + ENFANTS_NON_COMMUNS)
          // multi-sources IA / F96 / QUESTION_IA / PIECE_MANQUANTE.
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
    ]);

  ngOnInit(): void {
    if (this.caseFileId) {
      this.loadVisibility(true);
    }
    if (this.refreshService) {
      this.refreshService.refresh$
        .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
        .subscribe(() => {
          if (this.caseFileId) {
            this.loadVisibility(false);
          }
        });
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['caseFileId'] && !changes['caseFileId'].firstChange && this.caseFileId) {
      this.loadVisibility(true);
    }
  }

  private loadVisibility(showSpinner: boolean): void {
    if (showSpinner) this.loading.set(true);
    this.caseFileService.getDecisionToolsVisibility(this.caseFileId).subscribe({
      next: (result) => {
        this.visibility.set(result);
        this.loading.set(false);
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

  isEmpty(): boolean {
    const v = this.visibility();
    if (!v) return false;
    return v.alwaysOn.length === 0 && v.contextual.length === 0;
  }

  componentInputsFor(entry: DecisionToolRegistryEntry): Record<string, unknown> {
    return entry.inputs({
      caseFileId: this.caseFileId,
      synthesis: this.synthesis,
      workspaceCountry: this.workspaceCountry,
      caseFileTitle: this.caseFileTitle,
      procedureChecks: this.procedureChecks,
      aiQuestions: this.aiQuestions,
    });
  }
}
