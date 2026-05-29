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
import { MatSnackBar } from '@angular/material/snack-bar';

import { AssignationResidenceService } from '../../core/services/assignation-residence.service';
import {
  AssignationResidenceRequest,
  AssignationResidenceResponse,
  MotifAssignation,
  StatutAssignationResidence,
} from '../../core/models/assignation-residence.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { AssignationResidencePrefillRules } from './assignation-residence-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-36 — Outil décisionnel « Assignation à résidence » (F-IM-42).
 *
 * FRANCE uniquement — assignation à résidence prononcée en vue de l'exécution
 * d'une mesure d'éloignement (OQTF) ou comme mesure de surveillance (CESEDA).
 * Analyseur de validité / délais : statut de l'assignation (EXPIRE rouge /
 * EXPIRATION_PROCHE orange / EN_COURS vert) + date d'échéance + durée totale
 * autorisée + possibilité de renouvellement + motifs de contestation + recours
 * possible devant le TA (délai 48 h).
 * Pattern miroir : {@link AppelCaaCassationSectionComponent} (F-IM-41).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; vert EN_COURS, orange
 *    EXPIRATION_PROCHE, rouge EXPIRE
 *  - pré-fill IA (date de notification de l'assignation)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 *
 * <p>Bridge échéance F-69 : si statut EN_COURS, une échéance
 * `dateEcheanceAssignation` est créée via {@link CaseDeadlineService} avec le
 * label « Échéance assignation à résidence » (best-effort, n'interrompt pas le
 * flux).
 */
@Component({
  selector: 'app-assignation-residence-section',
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
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './assignation-residence-section.component.html',
  styleUrl: './assignation-residence-section.component.scss',
})
export class AssignationResidenceSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-42-assignation-residence-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'ASSIGNATION À RÉSIDENCE (FR)';
  static readonly TOOL_ICON = 'home_pin';

  static getPrefillCount(input: PrefillCountInput): number {
    return AssignationResidencePrefillRules.computePrefillCount(input);
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
  result = signal<AssignationResidenceResponse | null>(null);
  deadlineCreated = signal(false);

  dateNotificationAssignation = signal<string | null>(null);
  dureeAssignationJours = signal<number | null>(null);
  motifAssignation = signal<MotifAssignation>('EXECUTION_OQTF');
  obligationsPresentation = signal<string | null>(null);

  provenanceDateNotification = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: AssignationResidenceService,
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
    if (!this.dateNotificationAssignation()) return false;
    const duree = this.dureeAssignationJours();
    if (duree === null || duree <= 0) return false;
    return true;
  }

  onDateNotificationChange(value: string | null): void {
    this.dateNotificationAssignation.set(value || null);
    this.provenanceDateNotification.set(null);
  }

  onDureeChange(value: number | null): void {
    this.dureeAssignationJours.set(value ?? null);
  }

  onMotifChange(value: MotifAssignation): void {
    this.motifAssignation.set(value);
  }

  onObligationsChange(value: string | null): void {
    this.obligationsPresentation.set(value || null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: AssignationResidenceRequest = {
      dateNotificationAssignation: this.dateNotificationAssignation()!,
      dureeAssignationJours: this.dureeAssignationJours()!,
      motifAssignation: this.motifAssignation(),
      obligationsPresentation: this.obligationsPresentation(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse assignation à résidence enregistrée', 'OK', { duration: 2500 });
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
   * Bridge échéance F-69 — si le statut est EN_COURS, on alimente
   * `app-case-deadlines-section` avec l'échéance `dateEcheanceAssignation`.
   * Best-effort : un échec n'interrompt pas le flux (le dashboard refresh
   * affichera l'analyse quoi qu'il arrive).
   */
  private maybeCreateDeadline(r: AssignationResidenceResponse): void {
    if (!this.deadlineService || !this.caseFileId) return;
    if (r.statut !== 'EN_COURS') return;
    if (!r.dateEcheanceAssignation) return;
    this.deadlineService.create(this.caseFileId, 'Échéance assignation à résidence', r.dateEcheanceAssignation).subscribe({
      next: () => {
        this.deadlineCreated.set(true);
        this.cdr.markForCheck();
      },
      error: () => {
        // Best-effort : on ne bloque pas l'utilisateur sur l'échéance.
      },
    });
  }

  statutLabel(statut: StatutAssignationResidence | null | undefined): string {
    switch (statut) {
      case 'EN_COURS':          return 'Assignation en cours';
      case 'EXPIRATION_PROCHE': return 'Expiration proche';
      case 'EXPIRE':            return 'Assignation expirée';
      default:                  return '';
    }
  }

  motifLabel(motif: MotifAssignation | null | undefined): string {
    switch (motif) {
      case 'EXECUTION_OQTF':                  return "Exécution d'une OQTF";
      case 'SURVEILLANCE_MESURE_ELOIGNEMENT': return "Surveillance d'une mesure d'éloignement";
      case 'AUTRE':                           return 'Autre motif';
      default:                                return '';
    }
  }

  bannerClass(statut: StatutAssignationResidence | null | undefined): string {
    switch (statut) {
      case 'EN_COURS':          return 'acc-banner acc-banner--success';
      case 'EXPIRATION_PROCHE': return 'acc-banner acc-banner--warning';
      case 'EXPIRE':            return 'acc-banner acc-banner--danger';
      default:                  return 'acc-banner';
    }
  }

  bannerIcon(statut: StatutAssignationResidence | null | undefined): string {
    switch (statut) {
      case 'EN_COURS':          return 'check_circle';
      case 'EXPIRATION_PROCHE': return 'warning';
      case 'EXPIRE':            return 'error';
      default:                  return 'info_outline';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const dateNotif = AssignationResidencePrefillRules.computeDateNotification(input);
    if (dateNotif !== null && this.dateNotificationAssignation() === null) {
      this.dateNotificationAssignation.set(dateNotif);
      this.provenanceDateNotification.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateNotificationAssignation.set(r.dateNotificationAssignation ?? null);
        this.dureeAssignationJours.set(r.dureeAssignationJours ?? null);
        this.motifAssignation.set(r.motifAssignation ?? 'EXECUTION_OQTF');
        this.obligationsPresentation.set(r.obligationsPresentation ?? null);
        this.provenanceDateNotification.set(null);
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
