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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { VictimeViolencesL4256Service } from '../../core/services/victime-violences-l4256.service';
import {
  EligibiliteScoreL4256,
  VictimeViolencesL4256Response,
} from '../../core/models/victime-violences-l4256.model';
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
import { VictimeViolencesL4256PrefillRules } from './victime-violences-l4256-section-prefill-rules';

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export type VVL4256AlertField = 'DATE_ORDONNANCE';
export type VVL4256AlertSeverity = CoherenceAlertSeverity;
export type VVL4256CoherenceAlert = CoherenceAlert<VVL4256AlertField>;

/**
 * SF-208-08 : outil decisionnel "Victime de violences L.425-6 - France"
 * (F-IM-24). FR uniquement (CESEDA L.425-6+, Cciv 515-9+).
 * Niveau 5 (scoring 3 verdicts). Visibility ALWAYS_ON.
 * Pattern de reference : OqtfAvecDelaiSectionComponent.
 */
@Component({
  selector: 'app-victime-violences-l4256-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
    DecisionalHeaderFlagComponent,
  ],
  templateUrl: './victime-violences-l4256-section.component.html',
  styleUrl: './victime-violences-l4256-section.component.scss',
})
export class VictimeViolencesL4256SectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'VICTIME VIOLENCES L.425-6 (FR)';
  static readonly TOOL_ICON = 'shield';
  static readonly PREFILL_COUNT_ALWAYS_ZERO = true;

  static getPrefillCount(input: PrefillCountInput): number {
    return VictimeViolencesL4256PrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  @Input() forceExpanded = false;


  /**
   * F-163 SF-163-02d — Mode simulateur autonome (hors dossier client).
   *
   * Quand `true` :
   *  - Bannière « 🧪 Mode simulateur » affichée en haut.
   *  - `prefillFromAi()` court-circuité.
   *  - `coherenceAlerts` retourne `{}`.
   *  - `loadExisting()` / `load()` court-circuité.
   *  - `analyze()` POSTe sur `/api/v1/simulators/F-IM-24-victime-violences-l4256-fr/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;
  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<VictimeViolencesL4256Response | null>(null);

  dateOrdonnanceProtection = signal<string | null>(null);
  juridiction = signal<string | null>(null);
  dureeProtectionMois = signal<number>(6);
  dateExpirationProtection = signal<string | null>(null);
  enfantsAcharge = signal<number>(0);
  nationalite = signal<string | null>(null);

  provenanceDateOrdonnance = signal<'IA' | null>(null);

  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  coherenceAlerts = computed<Partial<Record<VVL4256AlertField, VVL4256CoherenceAlert>>>(() => {
    // F-163 SF-163-02d : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<VVL4256AlertField, VVL4256CoherenceAlert>> = {};
    const a1 = this.buildDateOrdonnanceAlert();
    if (a1) alerts.DATE_ORDONNANCE = a1;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter(a => a?.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: VictimeViolencesL4256Service,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (this.standaloneMode) {

      // F-163 SF-163-02d : pas de dossier à interroger en standalone.

      this.collapsed.set(false);

      this.loading.set(false);

      this.showForm.set(true);

      return;

    }
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
    const d = this.dateOrdonnanceProtection();
    const j = this.juridiction();
    const duree = this.dureeProtectionMois();
    const ec = this.enfantsAcharge();
    if (!d || !j) return false;
    if (d > this.todayIso) return false;
    if (!j.trim()) return false;
    if (duree <= 0) return false;
    if (ec < 0) return false;
    return true;
  }

  editMode(): void { this.showForm.set(true); }

  onDateOrdonnanceChange(value: string | null): void {
    this.dateOrdonnanceProtection.set(value || null);
    this.provenanceDateOrdonnance.set(null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request = {
      dateOrdonnanceProtection: this.dateOrdonnanceProtection()!,
      juridiction: this.juridiction()!.trim(),
      dureeProtectionMois: this.dureeProtectionMois(),
      dateExpirationProtection: this.dateExpirationProtection() || null,
      enfantsAcharge: this.enfantsAcharge(),
      nationalite: this.nationalite() || null,
    };
    this.analyzing.set(true);
    // F-163 SF-163-02d : en standalone, POST sur le dispatcher générique.

    const request$ = this.standaloneMode

      ? this.service.analyzeStandalone(request)

      : this.service.analyze(this.caseFileId, request);

    request$.subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Eligibilite L.425-6 analysee', 'OK', { duration: 2500 });
        // F-163 SF-163-02d : pas de dashboard à rafraîchir en standalone.

        if (!this.standaloneMode) { this.dashboardRefresh?.triggerRefresh(); }
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || "Erreur lors de l'analyse";
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  verdictClass(verdict: EligibiliteScoreL4256 | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE_PLEIN_DROIT':    return 'vvl-banner vvl-banner--success';
      case 'ELIGIBLE_SOUS_RESERVE':   return 'vvl-banner vvl-banner--warning';
      case 'NON_ELIGIBLE':            return 'vvl-banner vvl-banner--danger';
      default:                        return 'vvl-banner';
    }
  }

  verdictIcon(verdict: EligibiliteScoreL4256 | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE_PLEIN_DROIT':  return 'verified';
      case 'ELIGIBLE_SOUS_RESERVE': return 'warning';
      case 'NON_ELIGIBLE':          return 'error';
      default:                      return 'info_outline';
    }
  }

  verdictLabel(verdict: EligibiliteScoreL4256 | null | undefined): string {
    switch (verdict) {
      case 'ELIGIBLE_PLEIN_DROIT':  return 'Eligible de plein droit (L.425-6)';
      case 'ELIGIBLE_SOUS_RESERVE': return 'Eligible sous reserve';
      case 'NON_ELIGIBLE':          return 'Non eligible sur le fondement L.425-6';
      default:                      return '';
    }
  }

  alertBadgeLabel(alert: VVL4256CoherenceAlert): string {
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

    const date = VictimeViolencesL4256PrefillRules.computeDateOrdonnanceProtection(input);
    if (date !== null && !this.dateOrdonnanceProtection()) {
      this.dateOrdonnanceProtection.set(date);
      this.provenanceDateOrdonnance.set('IA');
    }
  }

  private buildDateOrdonnanceAlert(): VVL4256CoherenceAlert | null {
    const user = this.dateOrdonnanceProtection();
    if (!user) return null;
    const builder = CoherenceAlertBuilder.forField<VVL4256AlertField>('DATE_ORDONNANCE')
      .withSeverity('WARNING');

    for (const chk of this.procedureChecksSignal()) {
      const code = chk.critereCode?.toUpperCase();
      if (code !== 'IM24_DATE_ORDONNANCE_PROTECTION' && code !== 'ORDONNANCE_JAF') continue;
      const ev = chk.expectedValue;
      if (!ev || !ISO_DATE_RE.test(ev) || ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procedurale : date ordonnance attendue ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante([
      'IM24_DATE_ORDONNANCE_PROTECTION',
      'IM24_ORDONNANCE_JAF',
      'ORDONNANCE_PROTECTION',
    ]);
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
        this.dateOrdonnanceProtection.set(r.dateOrdonnanceProtection);
        this.juridiction.set(r.juridiction);
        this.dureeProtectionMois.set(r.dureeProtectionMois);
        this.dateExpirationProtection.set(r.dateExpirationProtectionEffective);
        this.enfantsAcharge.set(r.enfantsAcharge);
        this.nationalite.set(r.nationalite);
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
