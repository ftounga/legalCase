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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { RetraitTitreFraudeService } from '../../core/services/retrait-titre-fraude.service';
import {
  RetraitTitreFraudeRequest,
  RetraitTitreFraudeResponse,
  StatutRetraitTitreFraude,
  MotifRetraitTitreFraude,
} from '../../core/models/retrait-titre-fraude.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { RetraitTitreFraudePrefillRules } from './retrait-titre-fraude-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-42 — Outil décisionnel « Retrait titre pour fraude » (F-IM-45).
 *
 * FRANCE uniquement — retrait d'un titre de séjour obtenu par fraude (mariage
 * gris, fausses déclarations, fraude documentaire) ou consécutif à la perte des
 * conditions de délivrance (CESEDA). Analyseur de validité (palette rouge
 * PRESCRIT / orange URGENT / vert RECOURS_POSSIBLE) : statut du recours + vices
 * de procédure éventuels + motifs de contestation + délai du recours devant le
 * TA + base juridique.
 * Pattern miroir : {@link AppelCaaCassationSectionComponent} (F-IM-41).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; vert RECOURS_POSSIBLE, orange URGENT,
 *    rouge PRESCRIT
 *  - pré-fill IA (date du retrait + motif du retrait)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 *
 * <p>Bridge échéance F-69 : si statut RECOURS_POSSIBLE ou URGENT, une échéance
 * `delaiRecoursTA` est créée via {@link CaseDeadlineService} avec le label
 * « Recours TA retrait titre » (best-effort, n'interrompt pas le flux).
 *
 * <p>Section orange « vices de procédure » : si l'analyse relève des vices de
 * procédure (non vide), ils sont mis en avant dans un encadré orange — ce sont
 * souvent les moyens les plus efficaces pour faire annuler la décision.
 */
@Component({
  selector: 'app-retrait-titre-fraude-section',
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
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './retrait-titre-fraude-section.component.html',
  styleUrl: './retrait-titre-fraude-section.component.scss',
})
export class RetraitTitreFraudeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-45-retrait-titre-fraude-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'RETRAIT TITRE POUR FRAUDE (FR)';
  static readonly TOOL_ICON = 'gpp_bad';

  static getPrefillCount(input: PrefillCountInput): number {
    return RetraitTitreFraudePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<RetraitTitreFraudeResponse | null>(null);
  deadlineCreated = signal(false);

  dateRetrait = signal<string | null>(null);
  motifRetrait = signal<MotifRetraitTitreFraude>('FAUSSES_DECLARATIONS');
  miseEnDemeurePrealable = signal<boolean>(false);
  dateMiseEnDemeure = signal<string | null>(null);

  provenanceDateRetrait = signal<'IA' | null>(null);
  provenanceMotifRetrait = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: RetraitTitreFraudeService,
    private readonly cdr: ChangeDetectorRef,
    private readonly snackBar: MatSnackBar,
    @Optional() private readonly dashboardRefresh?: CaseDashboardRefreshService,
    @Optional() private readonly deadlineService?: CaseDeadlineService,
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
    if (!this.dateRetrait()) return false;
    return true;
  }

  onDateRetraitChange(value: string | null): void {
    this.dateRetrait.set(value || null);
    this.provenanceDateRetrait.set(null);
  }

  onMotifRetraitChange(value: MotifRetraitTitreFraude): void {
    this.motifRetrait.set(value);
    this.provenanceMotifRetrait.set(null);
  }

  onMiseEnDemeureChange(value: boolean): void {
    this.miseEnDemeurePrealable.set(value);
    if (!value) {
      this.dateMiseEnDemeure.set(null);
    }
  }

  onDateMiseEnDemeureChange(value: string | null): void {
    this.dateMiseEnDemeure.set(value || null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: RetraitTitreFraudeRequest = {
      dateRetrait: this.dateRetrait()!,
      motifRetrait: this.motifRetrait(),
      miseEnDemeurePrealable: this.miseEnDemeurePrealable(),
      dateMiseEnDemeure: this.dateMiseEnDemeure(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse retrait titre pour fraude enregistrée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) {
          this.maybeCreateDeadline(r);
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

  /**
   * Bridge échéance F-69 — si le statut est RECOURS_POSSIBLE ou URGENT, on
   * alimente `app-case-deadlines-section` avec l'échéance `delaiRecoursTA`.
   * Best-effort : un échec n'interrompt pas le flux (le dashboard refresh
   * affichera l'analyse quoi qu'il arrive).
   */
  private maybeCreateDeadline(r: RetraitTitreFraudeResponse): void {
    if (!this.deadlineService || !this.caseFileId) return;
    if (r.statut !== 'RECOURS_POSSIBLE' && r.statut !== 'URGENT') return;
    if (!r.delaiRecoursTA) return;
    this.deadlineService.create(this.caseFileId, 'Recours TA retrait titre', r.delaiRecoursTA).subscribe({
      next: () => {
        this.deadlineCreated.set(true);
        this.cdr.markForCheck();
      },
      error: () => {
        // Best-effort : on ne bloque pas l'utilisateur sur l'échéance.
      },
    });
  }

  statutLabel(statut: StatutRetraitTitreFraude | null | undefined): string {
    switch (statut) {
      case 'RECOURS_POSSIBLE': return 'Recours possible';
      case 'URGENT':           return 'Recours urgent';
      case 'PRESCRIT':         return 'Délai de recours prescrit';
      default:                 return '';
    }
  }

  motifLabel(motif: MotifRetraitTitreFraude | null | undefined): string {
    switch (motif) {
      case 'MARIAGE_GRIS':         return 'Mariage gris / de complaisance';
      case 'FAUSSES_DECLARATIONS': return 'Fausses déclarations';
      case 'FRAUDE_DOCUMENTAIRE':  return 'Fraude documentaire';
      case 'PERTE_CONDITIONS':     return 'Perte des conditions de délivrance';
      default:                     return '';
    }
  }

  bannerClass(statut: StatutRetraitTitreFraude | null | undefined): string {
    switch (statut) {
      case 'RECOURS_POSSIBLE': return 'acc-banner acc-banner--success';
      case 'URGENT':           return 'acc-banner acc-banner--warning';
      case 'PRESCRIT':         return 'acc-banner acc-banner--danger';
      default:                 return 'acc-banner';
    }
  }

  bannerIcon(statut: StatutRetraitTitreFraude | null | undefined): string {
    switch (statut) {
      case 'RECOURS_POSSIBLE': return 'check_circle';
      case 'URGENT':           return 'warning';
      case 'PRESCRIT':         return 'error';
      default:                 return 'info_outline';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const dateRetrait = RetraitTitreFraudePrefillRules.computeDateRetrait(input);
    if (dateRetrait !== null && this.dateRetrait() === null) {
      this.dateRetrait.set(dateRetrait);
      this.provenanceDateRetrait.set('IA');
    }

    const motif = RetraitTitreFraudePrefillRules.computeMotifRetrait(input);
    if (motif !== null && this.provenanceMotifRetrait() === null) {
      this.motifRetrait.set(motif);
      this.provenanceMotifRetrait.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateRetrait.set(r.dateRetrait ?? null);
        this.motifRetrait.set(r.motifRetrait ?? 'FAUSSES_DECLARATIONS');
        this.miseEnDemeurePrealable.set(r.miseEnDemeurePrealable ?? false);
        this.dateMiseEnDemeure.set(r.dateMiseEnDemeure ?? null);
        this.provenanceDateRetrait.set(null);
        this.provenanceMotifRetrait.set(null);
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
