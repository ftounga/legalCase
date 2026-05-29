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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CceAnnulationBeService } from '../../core/services/cce-annulation-be.service';
import {
  CCE_ANNULATION_DELAI_JOURS,
  CCE_ANNULATION_TYPES_DECISION,
  CceAnnulationBeRequest,
  CceAnnulationBeResponse,
  CceAnnulationStatut,
  CceAnnulationTypeDecision,
  CceAnnulationTypeDecisionOption,
} from '../../core/models/cce-annulation-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { CceAnnulationBePrefillRules } from './cce-annulation-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export type IM31_CceAlertField = 'DATE_NOTIFICATION';
export type IM31_CceAlertSource = CoherenceAlertSource;
export type IM31_CceCoherenceAlert = CoherenceAlert<IM31_CceAlertField>;

/**
 * SF-215-14 — Outil décisionnel « Recours CCE annulation 30j (BE) »
 * (F-IM-31-cce-annulation-30j-be).
 *
 * ⚠️ « CCE » = Conseil du Contentieux des Étrangers (juridiction administrative
 * belge en droit des étrangers) — AUCUN lien avec le crédit / la banque.
 *
 * BELGIQUE uniquement — calculateur du délai du recours en ANNULATION devant le
 * CCE : 30 jours CALENDAIRES à compter de la notification de la décision
 * attaquée (Loi 15/12/1980 art. 39/2 §2 et 39/57 §1er). Visibilité CONTEXTUAL —
 * flag `recours_cce_envisage`.
 *
 * Pattern miroir : {@link Regroupement10terBeSectionComponent}
 * (F-IM-26-regroupement-10ter-be, SF-215-04).
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette : vert DISPONIBLE, orange URGENT, rouge EXPIRE,
 *    bleu info RECOURS_FORME
 *  - pré-fill IA RÉEL 2 champs (dateNotificationDecision, typeDecision) ;
 *    recoursForme + dateRecours aspirationnels → `PREFILL_COUNT_ALWAYS_ZERO`
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck
 *  - lien croisé vers F-IM-32 dans la recommandation si URGENT/EXPIRE
 */
@Component({
  selector: 'app-cce-annulation-be-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './cce-annulation-be-section.component.html',
  styleUrl: './cce-annulation-be-section.component.scss',
})
export class CceAnnulationBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-31-cce-annulation-30j-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'RECOURS CCE ANNULATION 30J (BE)';
  static readonly TOOL_ICON = 'gavel';

  static getPrefillCount(input: PrefillCountInput): number {
    return CceAnnulationBePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  readonly delaiJours = CCE_ANNULATION_DELAI_JOURS;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<CceAnnulationBeResponse | null>(null);

  dateNotificationDecision = signal<string | null>(null);
  typeDecision = signal<CceAnnulationTypeDecision | null>(null);
  recoursForme = signal<boolean>(false);
  dateRecours = signal<string | null>(null);

  provenanceDateNotification = signal<'IA' | null>(null);
  provenanceTypeDecision = signal<'IA' | null>(null);

  readonly typesDecision: ReadonlyArray<CceAnnulationTypeDecisionOption> =
    CCE_ANNULATION_TYPES_DECISION;

  isBelgique = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * Alertes de cohérence F-IA-03 — uniquement sur la date de notification
   * (le type de décision est un enum whitelisté strictement pré-rempli).
   */
  coherenceAlerts = computed<Partial<Record<IM31_CceAlertField, IM31_CceCoherenceAlert>>>(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IM31_CceAlertField, IM31_CceCoherenceAlert>> = {};
    const dateAlert = this.buildDateNotificationAlert();
    if (dateAlert) alerts.DATE_NOTIFICATION = dateAlert;
    return alerts;
  });

  constructor(
    private readonly service: CceAnnulationBeService,
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
    if (this.isBelgique() && this.caseFileId) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) {
      this.collapsed.set(false);
    }
    if (changes['aiData'] && this.isBelgique() && this.showForm() && !this.result()) {
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
    const dateNotif = this.dateNotificationDecision();
    if (!dateNotif || !ISO_DATE_RE.test(dateNotif)) return false;
    if (!this.typeDecision()) return false;
    if (this.recoursForme()) {
      const dr = this.dateRecours();
      if (!dr || !ISO_DATE_RE.test(dr)) return false;
    }
    return true;
  }

  onDateNotificationChange(value: string | null): void {
    this.dateNotificationDecision.set(value || null);
    this.provenanceDateNotification.set(null);
  }

  onTypeDecisionChange(value: CceAnnulationTypeDecision | null): void {
    this.typeDecision.set(value);
    this.provenanceTypeDecision.set(null);
  }

  onRecoursFormeChange(value: boolean): void {
    this.recoursForme.set(value);
    if (!value) {
      this.dateRecours.set(null);
    }
  }

  onDateRecoursChange(value: string | null): void {
    this.dateRecours.set(value || null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: CceAnnulationBeRequest = {
      dateNotificationDecision: this.dateNotificationDecision()!,
      typeDecision: this.typeDecision()!,
      recoursForme: this.recoursForme(),
      dateRecours: this.recoursForme() ? this.dateRecours() : null,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse recours CCE annulation enregistrée', 'OK', { duration: 2500 });
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

  statutBannerClass(statut: CceAnnulationStatut | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'cce-banner cce-banner--success';
      case 'URGENT':        return 'cce-banner cce-banner--warning';
      case 'EXPIRE':        return 'cce-banner cce-banner--danger';
      case 'RECOURS_FORME': return 'cce-banner cce-banner--info';
      default:              return 'cce-banner';
    }
  }

  statutChipClass(statut: CceAnnulationStatut | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'cce-chip cce-chip--success';
      case 'URGENT':        return 'cce-chip cce-chip--warning';
      case 'EXPIRE':        return 'cce-chip cce-chip--danger';
      case 'RECOURS_FORME': return 'cce-chip cce-chip--info';
      default:              return 'cce-chip';
    }
  }

  statutIcon(statut: CceAnnulationStatut | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'event_available';
      case 'URGENT':        return 'schedule';
      case 'EXPIRE':        return 'event_busy';
      case 'RECOURS_FORME': return 'task_alt';
      default:              return 'info_outline';
    }
  }

  statutLabel(statut: CceAnnulationStatut | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'Délai disponible';
      case 'URGENT':        return 'Délai urgent';
      case 'EXPIRE':        return 'Délai expiré';
      case 'RECOURS_FORME': return 'Recours déjà formé';
      default:              return '';
    }
  }

  /** True si la recommandation/lien F-IM-32 doit être affichée (URGENT ou EXPIRE). */
  showRecommandation(statut: CceAnnulationStatut | null | undefined): boolean {
    return statut === 'URGENT' || statut === 'EXPIRE';
  }

  typeDecisionLabel(code: CceAnnulationTypeDecision | string | null | undefined): string {
    if (!code) return '';
    return this.typesDecision.find((t) => t.code === code)?.label ?? String(code);
  }

  /** Format JJ/MM/YYYY depuis une date ISO yyyy-MM-dd (typographie JetBrains Mono côté template). */
  formatDateFr(iso: string | null | undefined): string {
    if (!iso || !ISO_DATE_RE.test(iso)) return '—';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }

  /** Classe CSS des jours restants — rouge si négatif (délai dépassé). */
  joursRestantsClass(jours: number | null | undefined): string {
    if (jours === null || jours === undefined) return 'cce-jours';
    return jours < 0 ? 'cce-jours cce-jours--negative' : 'cce-jours';
  }

  alertTooltip(alert: IM31_CceCoherenceAlert): string {
    return alert.reason;
  }

  alertBadgeLabel(alert: IM31_CceCoherenceAlert): string {
    if (alert.severity === 'CRITICAL') return `Alerte critique (${alert.expectedDisplay})`;
    return `Incohérence détectée (${alert.expectedDisplay})`;
  }

  private buildDateNotificationAlert(): IM31_CceCoherenceAlert | null {
    const userDate = this.dateNotificationDecision();
    if (!userDate) return null;
    const ai = this.aiData;
    const aiDate = ai?.recoursCceDateNotification;
    const builder = CoherenceAlertBuilder.forField<IM31_CceAlertField>('DATE_NOTIFICATION')
      .withSeverity('WARNING');
    if (
      typeof aiDate === 'string' &&
      ISO_DATE_RE.test(aiDate) &&
      aiDate !== userDate
    ) {
      builder.addSource('IA', {
        expectedDisplay: this.formatDateFr(aiDate),
        reason: `Analyse du dossier : notification de la décision le ${this.formatDateFr(aiDate)}`,
      });
    }
    return builder.build();
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const dateNotif = CceAnnulationBePrefillRules.computeDateNotificationDecision(input);
    if (dateNotif !== null && !this.dateNotificationDecision()) {
      this.dateNotificationDecision.set(dateNotif);
      this.provenanceDateNotification.set('IA');
    }

    const type = CceAnnulationBePrefillRules.computeTypeDecision(input);
    if (type !== null && !this.typeDecision()) {
      this.typeDecision.set(type);
      this.provenanceTypeDecision.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateNotificationDecision.set(r.dateNotificationDecision ?? null);
        this.typeDecision.set((r.typeDecision as CceAnnulationTypeDecision) ?? null);
        this.recoursForme.set(r.recoursForme ?? false);
        this.dateRecours.set(r.dateRecours ?? null);
        this.provenanceDateNotification.set(null);
        this.provenanceTypeDecision.set(null);
        this.showForm.set(false);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — on tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
