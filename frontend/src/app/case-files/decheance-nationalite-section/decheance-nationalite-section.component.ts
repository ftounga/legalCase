import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  computed,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar } from '@angular/material/snack-bar';

import { DecheanceNationaliteService } from '../../core/services/decheance-nationalite.service';
import {
  DecheanceNationaliteRequest,
  DecheanceNationaliteResponse,
  ValiditeDecheance,
  MotifDecheance,
} from '../../core/models/decheance-nationalite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { DecheanceNationalitePrefillRules } from './decheance-nationalite-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-220-05 — Outil décisionnel « validité d'une mesure de déchéance de
 * nationalité » (F-IM-51, Cciv 25 / 25-1).
 *
 * FR uniquement. Apprécie la régularité d'une mesure (envisagée ou prononcée)
 * de déchéance de la nationalité française : interdiction d'apatridie
 * (binational requis), délai Cciv 25-1, et calcul du délai de recours (REP
 * Conseil d'État, 2 mois). Distinct de F-IM-13 (acquisition de la nationalité)
 * et de F-IM-39/40 (recours refus naturalisation). Pattern miroir :
 * {@link PacsVpfSectionComponent}.
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette navy/or Immigration
 *  - pré-fill IA (motif, binational, mesure prononcée, date du décret)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)</p>
 */
@Component({
  selector: 'app-decheance-nationalite-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './decheance-nationalite-section.component.html',
  styleUrl: './decheance-nationalite-section.component.scss',
})
export class DecheanceNationaliteSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-51-decheance-nationalite-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'VALIDITÉ DÉCHÉANCE DE NATIONALITÉ (Cciv 25 / 25-1) (FR)';
  static readonly TOOL_ICON = 'gavel';

  static getPrefillCount(input: PrefillCountInput): number {
    return DecheanceNationalitePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  readonly motifs: MotifDecheance[] = [
    'TERRORISME',
    'ATTEINTE_INTERETS_NATION',
    'FRAUDE_ACQUISITION',
    'AUTRE',
  ];

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<DecheanceNationaliteResponse | null>(null);

  motif = signal<MotifDecheance | null>(null);
  binational = signal<boolean | null>(null);
  dateAcquisitionNationalite = signal<string | null>(null);
  dateFaits = signal<string | null>(null);
  mesurePrononcee = signal<boolean>(false);
  dateDecret = signal<string | null>(null);

  provenanceMotif = signal<'IA' | null>(null);
  provenanceBinational = signal<'IA' | null>(null);
  provenanceMesurePrononcee = signal<'IA' | null>(null);
  provenanceDateDecret = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: DecheanceNationaliteService,
    private readonly cdr: ChangeDetectorRef,
    private readonly snackBar: MatSnackBar,
    @Optional() private readonly dashboardRefresh?: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    if (this.standaloneMode) {
      this.collapsed.set(false);
      this.loading.set(false);
      this.showForm.set(true);
      return;
    }
    if (this.isFrance() && this.caseFileId) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) {
      this.collapsed.set(false);
    }
    if (changes['aiData'] && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  formValid(): boolean {
    // Le motif est l'élément structurant de l'outil — l'analyse exige au moins
    // un motif sélectionné. Les autres champs alimentent l'appréciation.
    return this.motif() !== null;
  }

  onMotifChange(value: MotifDecheance): void {
    this.motif.set(value);
    this.provenanceMotif.set(null);
  }

  onBinationalChange(value: boolean | null): void {
    this.binational.set(value);
    this.provenanceBinational.set(null);
  }

  onMesurePrononceeChange(value: boolean): void {
    this.mesurePrononcee.set(value);
    this.provenanceMesurePrononcee.set(null);
  }

  onDateDecretChange(value: string | null): void {
    this.dateDecret.set(value);
    this.provenanceDateDecret.set(null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: DecheanceNationaliteRequest = {
      motif: this.motif(),
      binational: this.binational(),
      dateAcquisitionNationalite: this.dateAcquisitionNationalite(),
      dateFaits: this.dateFaits(),
      mesurePrononcee: this.mesurePrononcee(),
      dateDecret: this.dateDecret(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse de déchéance de nationalité enregistrée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) {
          this.dashboardRefresh?.triggerRefresh();
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || "Erreur lors de l'analyse";
        this.snackBar.open(String(msg), 'Fermer', {
          duration: 5000,
          panelClass: 'snack-error',
        });
        this.cdr.markForCheck();
      },
    });
  }

  bannerClass(result: DecheanceNationaliteResponse | null | undefined): string {
    if (!result) return 'dech-banner';
    switch (result.validite) {
      case 'CONDITIONS_REUNIES':
        return 'dech-banner dech-banner--success';
      case 'MESURE_CONTESTABLE':
        return 'dech-banner dech-banner--warning';
      case 'MESURE_IRREGULIERE':
        return 'dech-banner dech-banner--danger';
      case 'INDETERMINE':
      default:
        return 'dech-banner dech-banner--neutral';
    }
  }

  bannerIcon(result: DecheanceNationaliteResponse | null | undefined): string {
    if (!result) return 'info_outline';
    switch (result.validite) {
      case 'CONDITIONS_REUNIES':
        return 'verified';
      case 'MESURE_CONTESTABLE':
        return 'report_problem';
      case 'MESURE_IRREGULIERE':
        return 'cancel';
      case 'INDETERMINE':
      default:
        return 'help_outline';
    }
  }

  validiteLabel(v: ValiditeDecheance | null | undefined): string {
    switch (v) {
      case 'CONDITIONS_REUNIES':
        return 'Conditions légales réunies (sous contrôle de proportionnalité)';
      case 'MESURE_CONTESTABLE':
        return 'Mesure contestable (élément(s) à factualiser ou fragile(s))';
      case 'MESURE_IRREGULIERE':
        return 'Mesure irrégulière (apatridie ou hors délai Cciv 25-1)';
      case 'INDETERMINE':
        return 'Situation indéterminée';
      default:
        return '';
    }
  }

  motifLabel(m: MotifDecheance | null | undefined): string {
    switch (m) {
      case 'TERRORISME':
        return 'Terrorisme (Cciv 25, 1°)';
      case 'ATTEINTE_INTERETS_NATION':
        return 'Atteinte aux intérêts fondamentaux de la Nation';
      case 'FRAUDE_ACQUISITION':
        return 'Fraude lors de l\'acquisition de la nationalité';
      case 'AUTRE':
        return 'Autre motif';
      default:
        return '';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const motif = DecheanceNationalitePrefillRules.computeMotif(input);
    if (motif !== null && this.motif() === null) {
      this.motif.set(motif);
      this.provenanceMotif.set('IA');
    }

    const binational = DecheanceNationalitePrefillRules.computeBinational(input);
    if (binational !== null && this.binational() === null) {
      this.binational.set(binational);
      this.provenanceBinational.set('IA');
    }

    const mesurePrononcee = DecheanceNationalitePrefillRules.computeMesurePrononcee(input);
    if (mesurePrononcee !== null && !this.mesurePrononcee()) {
      this.mesurePrononcee.set(mesurePrononcee);
      this.provenanceMesurePrononcee.set('IA');
    }

    const dateDecret = DecheanceNationalitePrefillRules.computeDateDecret(input);
    if (dateDecret !== null && this.dateDecret() === null) {
      this.dateDecret.set(dateDecret);
      this.provenanceDateDecret.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.motif.set(r.motif ?? null);
        this.binational.set(r.binational ?? null);
        this.dateAcquisitionNationalite.set(r.dateAcquisitionNationalite ?? null);
        this.dateFaits.set(r.dateFaits ?? null);
        this.mesurePrononcee.set(r.mesurePrononcee ?? false);
        this.dateDecret.set(r.dateDecret ?? null);
        this.provenanceMotif.set(null);
        this.provenanceBinational.set(null);
        this.provenanceMesurePrononcee.set(null);
        this.provenanceDateDecret.set(null);
        this.showForm.set(false);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 = pas encore d'analyse, on tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
