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

import { CrrvRefusVisaService } from '../../core/services/crrv-refus-visa.service';
import {
  CrrvRefusVisaResponse,
  TYPES_VISA_CRRV,
  TypeVisaCrrv,
  TypeVisaCrrvOption,
  StatutCrrv,
} from '../../core/models/crrv-refus-visa.model';
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
import { CrrvRefusVisaPrefillRules } from './crrv-refus-visa-section-prefill-rules';

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export type CrrvAlertField = 'DATE_NOTIFICATION';
export type CrrvAlertSeverity = CoherenceAlertSeverity;
export type CrrvCoherenceAlert = CoherenceAlert<CrrvAlertField>;

/**
 * SF-208-07 : outil decisionnel "CRRV recours refus de visa (2 mois) - France"
 * (F-IM-23). FR uniquement (CESEDA L.312-1+, D.312-3). Visibility ALWAYS_ON.
 * Pattern de reference : OqtfAvecDelaiSectionComponent.
 */
@Component({
  selector: 'app-crrv-refus-visa-section',
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
  templateUrl: './crrv-refus-visa-section.component.html',
  styleUrl: './crrv-refus-visa-section.component.scss',
})
export class CrrvRefusVisaSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'CRRV RECOURS REFUS DE VISA (FR)';
  static readonly TOOL_ICON = 'mail';
  static readonly PREFILL_COUNT_ALWAYS_ZERO = true;

  static getPrefillCount(input: PrefillCountInput): number {
    return CrrvRefusVisaPrefillRules.computePrefillCount(input);
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
  result = signal<CrrvRefusVisaResponse | null>(null);

  dateNotificationRefus = signal<string | null>(null);
  typeVisa = signal<TypeVisaCrrv | null>(null);
  motifRefus = signal<string | null>(null);
  recoursForme = signal<boolean>(false);
  dateRecours = signal<string | null>(null);

  provenanceDateNotification = signal<'IA' | null>(null);

  readonly typesVisa: TypeVisaCrrvOption[] = TYPES_VISA_CRRV;
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  coherenceAlerts = computed<Partial<Record<CrrvAlertField, CrrvCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<CrrvAlertField, CrrvCoherenceAlert>> = {};
    const a1 = this.buildDateNotificationAlert();
    if (a1) alerts.DATE_NOTIFICATION = a1;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter(a => a?.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: CrrvRefusVisaService,
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
    const d = this.dateNotificationRefus();
    const t = this.typeVisa();
    if (!d || !t) return false;
    if (d > this.todayIso) return false;
    if (this.recoursForme()) {
      const dr = this.dateRecours();
      if (!dr) return false;
      if (dr < d) return false;
    }
    const m = this.motifRefus();
    if (m && m.length > 200) return false;
    return true;
  }

  editMode(): void { this.showForm.set(true); }

  onDateNotificationChange(value: string | null): void {
    this.dateNotificationRefus.set(value || null);
    this.provenanceDateNotification.set(null);
  }

  onRecoursFormeChange(value: boolean): void {
    this.recoursForme.set(value);
    if (!value) this.dateRecours.set(null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request = {
      dateNotificationRefus: this.dateNotificationRefus()!,
      typeVisa: this.typeVisa()!,
      motifRefus: this.motifRefus() || null,
      recoursForme: this.recoursForme(),
      ...(this.recoursForme() ? { dateRecours: this.dateRecours()! } : {}),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Recours CRRV analyse', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || "Erreur lors de l'analyse";
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  bannerClass(statut: StatutCrrv | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'crrv-banner crrv-banner--info';
      case 'URGENT':        return 'crrv-banner crrv-banner--warning';
      case 'EXPIRE':        return 'crrv-banner crrv-banner--danger';
      case 'RECOURS_FORME': return 'crrv-banner crrv-banner--success';
      default:              return 'crrv-banner';
    }
  }

  bannerIcon(statut: StatutCrrv | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'info_outline';
      case 'URGENT':        return 'warning';
      case 'EXPIRE':        return 'error';
      case 'RECOURS_FORME': return 'check_circle';
      default:              return 'info_outline';
    }
  }

  statutLabel(statut: StatutCrrv | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'Delai disponible';
      case 'URGENT':        return 'Delai URGENT (<= 7 j)';
      case 'EXPIRE':        return 'Delai expire';
      case 'RECOURS_FORME': return 'Recours forme';
      default:              return '';
    }
  }

  showJoursRestants(r: CrrvRefusVisaResponse | null): boolean {
    if (!r) return false;
    return r.statut === 'DISPONIBLE' || r.statut === 'URGENT';
  }

  alertBadgeLabel(alert: CrrvCoherenceAlert): string {
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

    const date = CrrvRefusVisaPrefillRules.computeDateNotificationRefus(input);
    if (date !== null && !this.dateNotificationRefus()) {
      this.dateNotificationRefus.set(date);
      this.provenanceDateNotification.set('IA');
    }
  }

  private buildDateNotificationAlert(): CrrvCoherenceAlert | null {
    const user = this.dateNotificationRefus();
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<CrrvAlertField>('DATE_NOTIFICATION')
      .withSeverity('WARNING');

    for (const chk of this.procedureChecksSignal()) {
      const code = chk.critereCode?.toUpperCase();
      if (code !== 'IM23_DATE_NOTIFICATION_REFUS' && code !== 'NOTIFICATION_DECISION') continue;
      const ev = chk.expectedValue;
      if (!ev || !ISO_DATE_RE.test(ev) || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procedurale : date attendue ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const aiDate = this.aiDataSignal()?.dateNotificationDecisionContestee;
    if (typeof aiDate === 'string' && ISO_DATE_RE.test(aiDate) && aiDate !== user) {
      builder.addSource('IA', {
        expectedDisplay: aiDate,
        reason: `L'analyse a detecte une date de notification differente : ${aiDate}`,
      });
    }

    const piece = this.findPieceManquante(['IM23_DATE_NOTIFICATION_REFUS', 'IM23_REFUS_VISA']);
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
        this.dateNotificationRefus.set(r.dateNotificationRefus);
        this.typeVisa.set(r.typeVisa);
        this.motifRefus.set(r.motifRefus);
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
