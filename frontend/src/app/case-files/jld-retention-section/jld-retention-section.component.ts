import {
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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { JldRetentionService } from '../../core/services/jld-retention.service';
import {
  JldRetentionResponse,
  MOTIFS_PLACEMENT_JLD,
  MotifPlacementJld,
  MotifPlacementJldOption,
  StatutJld,
} from '../../core/models/jld-retention.model';
import {
  ImmigrationExtractedData,
  PieceManquanteEntry,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSeverity,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { DecisionalHeaderFlagComponent } from '../decisional-tools-panel/decisional-header-flag/decisional-header-flag.component';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { JldRetentionPrefillRules } from './jld-retention-section-prefill-rules';

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export type JldAlertField = 'DATE_NOTIFICATION' | 'MOTIF_PLACEMENT';
export type JldAlertSeverity = CoherenceAlertSeverity;
export type JldCoherenceAlert = CoherenceAlert<JldAlertField>;

/**
 * SF-208-05 : outil decisionnel "JLD retention administrative - France"
 * (F-IM-21). FR uniquement (CESEDA L.741+, L.743+).
 *
 * Pattern de reference : OqtfAvecDelaiSectionComponent. Conforme F-IA-04 /
 * ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or, rouge URGENT/EXPIRE
 *  - pre-fill IA (date notification + motif inferable EXECUTION_OQTF)
 *  - validation F-IA-03 (coherenceAlerts + CoherenceAlertBuilder partage)
 *  - dashboardRefreshService.triggerRefresh() post-POST succes
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 */
@Component({
  selector: 'app-jld-retention-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
    DecisionalHeaderFlagComponent,
  ],
  templateUrl: './jld-retention-section.component.html',
  styleUrl: './jld-retention-section.component.scss',
})
export class JldRetentionSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'JLD RETENTION ADMINISTRATIVE (FR)';
  static readonly TOOL_ICON = 'gavel';

  static getPrefillCount(input: PrefillCountInput): number {
    return JldRetentionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  @Input() forceExpanded = false;

  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<JldRetentionResponse | null>(null);

  dateNotificationPlacement = signal<string | null>(null);
  motifPlacement = signal<MotifPlacementJld | null>(null);
  recoursForme = signal<boolean>(false);
  dateRecours = signal<string | null>(null);

  provenanceDateNotification = signal<'IA' | null>(null);
  provenanceMotifPlacement = signal<'IA' | null>(null);

  readonly motifs: MotifPlacementJldOption[] = MOTIFS_PLACEMENT_JLD;
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  coherenceAlerts = computed<Partial<Record<JldAlertField, JldCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<JldAlertField, JldCoherenceAlert>> = {};
    const a1 = this.buildDateNotificationAlert();
    if (a1) alerts.DATE_NOTIFICATION = a1;
    const a2 = this.buildMotifPlacementAlert();
    if (a2) alerts.MOTIF_PLACEMENT = a2;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter(a => a?.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: JldRetentionService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (this.isFrance()) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (changes['aiData'] && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void { this.collapsed.update(v => !v); }

  formValid(): boolean {
    const d = this.dateNotificationPlacement();
    const m = this.motifPlacement();
    if (!d || !m) return false;
    if (d > this.todayIso) return false;
    if (this.recoursForme()) {
      const dr = this.dateRecours();
      if (!dr) return false;
      if (dr < d) return false;
    }
    return true;
  }

  editMode(): void { this.showForm.set(true); }

  onDateNotificationChange(value: string | null): void {
    this.dateNotificationPlacement.set(value || null);
    this.provenanceDateNotification.set(null);
  }

  onMotifPlacementChange(value: MotifPlacementJld | null): void {
    this.motifPlacement.set(value);
    this.provenanceMotifPlacement.set(null);
  }

  onRecoursFormeChange(value: boolean): void {
    this.recoursForme.set(value);
    if (!value) this.dateRecours.set(null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request = {
      dateNotificationPlacement: this.dateNotificationPlacement()!,
      motifPlacement: this.motifPlacement()!,
      recoursForme: this.recoursForme(),
      ...(this.recoursForme() ? { dateRecours: this.dateRecours()! } : {}),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Saisine JLD analysee', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || "Erreur lors de l'analyse";
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  bannerClass(statut: StatutJld | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'jld-banner jld-banner--info';
      case 'URGENT':        return 'jld-banner jld-banner--warning';
      case 'EXPIRE':        return 'jld-banner jld-banner--danger';
      case 'RECOURS_FORME': return 'jld-banner jld-banner--success';
      default:              return 'jld-banner';
    }
  }

  bannerIcon(statut: StatutJld | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'info_outline';
      case 'URGENT':        return 'warning';
      case 'EXPIRE':        return 'error';
      case 'RECOURS_FORME': return 'check_circle';
      default:              return 'info_outline';
    }
  }

  statutLabel(statut: StatutJld | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'Saisine disponible';
      case 'URGENT':        return 'Saisine URGENTE (< 24 h)';
      case 'EXPIRE':        return 'Saisine forclose';
      case 'RECOURS_FORME': return 'Recours forme';
      default:              return '';
    }
  }

  showJoursRestants(r: JldRetentionResponse | null): boolean {
    if (!r) return false;
    return r.statut === 'DISPONIBLE' || r.statut === 'URGENT';
  }

  alertBadgeLabel(alert: JldCoherenceAlert): string {
    const prefix = alert.severity === 'CRITICAL'
      ? "Risque d'irrecevabilite"
      : 'Incoherence detectee';
    return `${prefix} (${alert.expectedDisplay})`;
  }

  private prefillFromAi(): void {
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const date = JldRetentionPrefillRules.computeDateNotificationPlacement(input);
    if (date !== null && !this.dateNotificationPlacement()) {
      this.dateNotificationPlacement.set(date);
      this.provenanceDateNotification.set('IA');
    }

    const motif = JldRetentionPrefillRules.computeMotifPlacement(input);
    if (motif !== null && !this.motifPlacement()) {
      this.motifPlacement.set(motif);
      this.provenanceMotifPlacement.set('IA');
    }
  }

  private buildDateNotificationAlert(): JldCoherenceAlert | null {
    const user = this.dateNotificationPlacement();
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<JldAlertField>('DATE_NOTIFICATION')
      .withSeverity('WARNING');

    for (const chk of this.procedureChecksSignal()) {
      const code = chk.critereCode?.toUpperCase();
      if (code !== 'IM21_DATE_NOTIFICATION_PLACEMENT' && code !== 'NOTIFICATION_DECISION') continue;
      const ev = chk.expectedValue;
      if (!ev || !ISO_DATE_RE.test(ev) || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procedurale : date attendue ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const aiDate = this.aiDataSignal()?.dateNotificationOqtf;
    if (typeof aiDate === 'string' && ISO_DATE_RE.test(aiDate) && aiDate !== user) {
      builder.addSource('IA', {
        expectedDisplay: aiDate,
        reason: `L'analyse a detecte une date de notification differente : ${aiDate}`,
      });
    }

    const piece = this.findPieceManquante(['IM21_DATE_NOTIFICATION_PLACEMENT', 'IM21_PLACEMENT_CRA']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildMotifPlacementAlert(): JldCoherenceAlert | null {
    const user = this.motifPlacement();
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<JldAlertField>('MOTIF_PLACEMENT')
      .withSeverity('WARNING');

    const aiOqtf = this.aiDataSignal()?.motifOqtfCode;
    if (typeof aiOqtf === 'string' && user !== 'EXECUTION_OQTF') {
      builder.addSource('IA', {
        expectedDisplay: "Execution d'une OQTF",
        reason: `L'analyse a detecte une OQTF (${aiOqtf}). Le placement en retention est typiquement pour executer cette OQTF.`,
      });
    }

    const piece = this.findPieceManquante(['IM21_MOTIF_PLACEMENT']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private findPieceManquante(acceptedCodes: string[]): string | null {
    const norm = new Set(acceptedCodes.map(c => c.toUpperCase()));
    for (const p of this.piecesManquantesSignal()) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (norm.has(code)) return p.texte;
    }
    return null;
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateNotificationPlacement.set(r.dateNotificationPlacement);
        this.motifPlacement.set(r.motifPlacement);
        this.recoursForme.set(r.recoursForme);
        this.dateRecours.set(r.dateRecours);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }
}
