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
import { InaptitudeSectionComponent } from '../inaptitude-section/inaptitude-section.component';
import { HeuresSupSectionComponent } from '../heures-sup-section/heures-sup-section.component';
import { IndemnitePrecariteCddSectionComponent } from '../indemnite-precarite-cdd-section/indemnite-precarite-cdd-section.component';
import { OqtfAvecDelaiSectionComponent } from '../oqtf-avec-delai-section/oqtf-avec-delai-section.component';
import { OqtfSansDelaiSectionComponent } from '../oqtf-sans-delai-section/oqtf-sans-delai-section.component';
import { Annexe13BeSectionComponent } from '../annexe13-be-section/annexe13-be-section.component';

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
