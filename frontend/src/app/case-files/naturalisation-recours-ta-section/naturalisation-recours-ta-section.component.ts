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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { NaturalisationRecoursTaService } from '../../core/services/naturalisation-recours-ta.service';
import {
  NaturalisationRecoursTaRequest,
  NaturalisationRecoursTaResponse,
  StatutNaturalisationRecoursTa,
} from '../../core/models/naturalisation-recours-ta.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { NaturalisationRecoursTaPrefillRules } from './naturalisation-recours-ta-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-32 — Outil décisionnel « Recours TA Nantes naturalisation » (F-IM-40).
 *
 * FRANCE uniquement — recours pour excès de pouvoir contre un refus de décret
 * de naturalisation, dans le délai de 2 mois, porté devant le tribunal
 * administratif de Nantes (compétence exclusive en matière de naturalisation).
 * Calculateur de délai (palette rouge PRESCRIT / orange URGENT / vert
 * RECOURS_POSSIBLE).
 * Pattern miroir : {@link NaturalisationRecoursTjSectionComponent} (F-IM-39).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; vert RECOURS_POSSIBLE, orange URGENT,
 *    rouge PRESCRIT
 *  - pré-fill IA (date du refus de décret)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 *
 * <p>Bridge échéance F-69 : si statut RECOURS_POSSIBLE ou URGENT, une échéance
 * `dateEcheanceRecoursTa` est créée via {@link CaseDeadlineService} avec le
 * label « Recours TA Nantes naturalisation » (best-effort, n'interrompt pas le
 * flux).
 */
@Component({
  selector: 'app-naturalisation-recours-ta-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './naturalisation-recours-ta-section.component.html',
  styleUrl: './naturalisation-recours-ta-section.component.scss',
})
export class NaturalisationRecoursTaSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-40-naturalisation-recours-ta-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'RECOURS TA NANTES NATURALISATION (FR)';
  static readonly TOOL_ICON = 'gavel';

  static getPrefillCount(input: PrefillCountInput): number {
    return NaturalisationRecoursTaPrefillRules.computePrefillCount(input);
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
  result = signal<NaturalisationRecoursTaResponse | null>(null);
  deadlineCreated = signal(false);

  dateRefusDecret = signal<string | null>(null);
  motivationRefus = signal<string | null>(null);
  recoursPrerequis = signal<boolean>(false);

  provenanceDateRefus = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: NaturalisationRecoursTaService,
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
    if (!this.dateRefusDecret()) return false;
    return true;
  }

  onDateRefusChange(value: string | null): void {
    this.dateRefusDecret.set(value || null);
    this.provenanceDateRefus.set(null);
  }

  onMotivationChange(value: string | null): void {
    this.motivationRefus.set(value || null);
  }

  onRecoursPrerequisChange(value: boolean): void {
    this.recoursPrerequis.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: NaturalisationRecoursTaRequest = {
      dateRefusDecret: this.dateRefusDecret()!,
      motivationRefus: this.motivationRefus(),
      recoursPrerequis: this.recoursPrerequis(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse recours TA Nantes naturalisation enregistrée', 'OK', { duration: 2500 });
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
   * alimente `app-case-deadlines-section` avec l'échéance `dateEcheanceRecoursTa`.
   * Best-effort : un échec n'interrompt pas le flux (le dashboard refresh
   * affichera l'analyse quoi qu'il arrive).
   */
  private maybeCreateDeadline(r: NaturalisationRecoursTaResponse): void {
    if (!this.deadlineService || !this.caseFileId) return;
    if (r.statut !== 'RECOURS_POSSIBLE' && r.statut !== 'URGENT') return;
    if (!r.dateEcheanceRecoursTa) return;
    this.deadlineService.create(this.caseFileId, 'Recours TA Nantes naturalisation', r.dateEcheanceRecoursTa).subscribe({
      next: () => {
        this.deadlineCreated.set(true);
        this.cdr.markForCheck();
      },
      error: () => {
        // Best-effort : on ne bloque pas l'utilisateur sur l'échéance.
      },
    });
  }

  statutLabel(statut: StatutNaturalisationRecoursTa | null | undefined): string {
    switch (statut) {
      case 'RECOURS_POSSIBLE': return 'Recours TA possible';
      case 'URGENT':           return 'Recours TA urgent';
      case 'PRESCRIT':         return 'Délai de recours prescrit';
      default:                 return '';
    }
  }

  bannerClass(statut: StatutNaturalisationRecoursTa | null | undefined): string {
    switch (statut) {
      case 'RECOURS_POSSIBLE': return 'nrt-banner nrt-banner--success';
      case 'URGENT':           return 'nrt-banner nrt-banner--warning';
      case 'PRESCRIT':         return 'nrt-banner nrt-banner--danger';
      default:                 return 'nrt-banner';
    }
  }

  bannerIcon(statut: StatutNaturalisationRecoursTa | null | undefined): string {
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

    const dateRefus = NaturalisationRecoursTaPrefillRules.computeDateRefusDecret(input);
    if (dateRefus !== null && this.dateRefusDecret() === null) {
      this.dateRefusDecret.set(dateRefus);
      this.provenanceDateRefus.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateRefusDecret.set(r.dateRefusDecret ?? null);
        this.motivationRefus.set(r.motivationRefus ?? null);
        this.recoursPrerequis.set(r.recoursPrerequis ?? false);
        this.provenanceDateRefus.set(null);
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
