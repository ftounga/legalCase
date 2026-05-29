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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar } from '@angular/material/snack-bar';

import { MnaEvaluationAgeService } from '../../core/services/mna-evaluation-age.service';
import {
  MnaEvaluationAgeRequest,
  MnaEvaluationAgeResponse,
  StatutMnaEvaluationAge,
} from '../../core/models/mna-evaluation-age.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { MnaEvaluationAgePrefillRules } from './mna-evaluation-age-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-28 — Outil décisionnel « MNA — évaluation de l'âge / recours JE »
 * (F-IM-38-mna-evaluation-age-fr).
 *
 * FRANCE UNIQUEMENT — l'évaluation de la minorité par l'ASE, la contestation des
 * examens osseux (art. 388 code civil) et le recours devant le juge des enfants
 * relèvent du droit français (CASF / décret 2019-57) et n'ont pas d'équivalent BE.
 * Analyse le statut de la situation, calcule l'échéance de saisine du JE, fournit
 * la procédure ASE (stepper), les arguments de contestation de l'examen osseux et
 * les droits attachés à la qualité de mineur isolé.
 * Pattern miroir : {@link AjCndaSectionComponent} (F-IM-34, bridge échéance F-69).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; chip rouge RECOURS_JE_URGENT,
 *    orange EXAMEN_OSSEUX_CONTESTE, vert PRIS_EN_CHARGE
 *  - pré-fill IA (date de naissance déclarée depuis mineursDateNaissance)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 *
 * <p>Bridge échéance F-69 : si statut RECOURS_JE_URGENT, une échéance
 * `dateEcheanceSaisineJE` est créée via {@link CaseDeadlineService} avec le label
 * « Saisine juge des enfants MNA » (best-effort, n'interrompt pas le flux).
 */
@Component({
  selector: 'app-mna-evaluation-age-section',
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
  templateUrl: './mna-evaluation-age-section.component.html',
  styleUrl: './mna-evaluation-age-section.component.scss',
})
export class MnaEvaluationAgeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-38-mna-evaluation-age-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'MNA évaluation âge (FR)';
  static readonly TOOL_ICON = 'escalator_warning';

  static getPrefillCount(input: PrefillCountInput): number {
    return MnaEvaluationAgePrefillRules.computePrefillCount(input);
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
  result = signal<MnaEvaluationAgeResponse | null>(null);
  deadlineCreated = signal(false);

  dateNaissanceDeclaree = signal<string | null>(null);
  evaluationASERefusee = signal<boolean>(false);
  dateRefusASE = signal<string | null>(null);
  examenOsseuxOrdonne = signal<boolean>(false);
  resultatExamenOsseux = signal<string | null>(null);

  provenanceDateNaissance = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: MnaEvaluationAgeService,
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
    if (!this.dateNaissanceDeclaree()) return false;
    // Si l'ASE a refusé l'évaluation, la date du refus est requise.
    if (this.evaluationASERefusee() && !this.dateRefusASE()) return false;
    return true;
  }

  onDateNaissanceChange(value: string | null): void {
    this.dateNaissanceDeclaree.set(value || null);
    this.provenanceDateNaissance.set(null);
  }

  onEvaluationRefuseeChange(value: boolean): void {
    this.evaluationASERefusee.set(value);
    if (!value) {
      this.dateRefusASE.set(null);
    }
  }

  onDateRefusChange(value: string | null): void {
    this.dateRefusASE.set(value || null);
  }

  onExamenOsseuxOrdonneChange(value: boolean): void {
    this.examenOsseuxOrdonne.set(value);
    if (!value) {
      this.resultatExamenOsseux.set(null);
    }
  }

  onResultatExamenChange(value: string | null): void {
    this.resultatExamenOsseux.set(value || null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: MnaEvaluationAgeRequest = {
      dateNaissanceDeclaree: this.dateNaissanceDeclaree()!,
      evaluationASERefusee: this.evaluationASERefusee(),
      dateRefusASE: this.evaluationASERefusee() ? this.dateRefusASE() : null,
      examenOsseuxOrdonne: this.examenOsseuxOrdonne(),
      resultatExamenOsseux: this.examenOsseuxOrdonne() ? this.resultatExamenOsseux() : null,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse MNA évaluation âge enregistrée', 'OK', { duration: 2500 });
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
   * Bridge échéance F-69 — si le statut est RECOURS_JE_URGENT, on alimente
   * `app-case-deadlines-section` avec l'échéance `dateEcheanceSaisineJE`.
   * Best-effort : un échec n'interrompt pas le flux.
   */
  private maybeCreateDeadline(r: MnaEvaluationAgeResponse): void {
    if (!this.deadlineService || !this.caseFileId) return;
    if (r.statut !== 'RECOURS_JE_URGENT') return;
    if (!r.dateEcheanceSaisineJE) return;
    this.deadlineService.create(this.caseFileId, 'Saisine juge des enfants MNA', r.dateEcheanceSaisineJE).subscribe({
      next: () => {
        this.deadlineCreated.set(true);
        this.cdr.markForCheck();
      },
      error: () => {
        // Best-effort : on ne bloque pas l'utilisateur sur l'échéance.
      },
    });
  }

  statutLabel(statut: StatutMnaEvaluationAge | null | undefined): string {
    switch (statut) {
      case 'EN_ATTENTE_EVALUATION':   return 'En attente d’évaluation';
      case 'RECOURS_JE_URGENT':       return 'Recours JE urgent';
      case 'EXAMEN_OSSEUX_CONTESTE':  return 'Examen osseux contesté';
      case 'PRIS_EN_CHARGE':          return 'Pris en charge';
      default:                        return '';
    }
  }

  bannerClass(statut: StatutMnaEvaluationAge | null | undefined): string {
    switch (statut) {
      case 'PRIS_EN_CHARGE':          return 'mna-banner mna-banner--success';
      case 'EXAMEN_OSSEUX_CONTESTE':  return 'mna-banner mna-banner--warning';
      case 'EN_ATTENTE_EVALUATION':   return 'mna-banner mna-banner--warning';
      case 'RECOURS_JE_URGENT':       return 'mna-banner mna-banner--danger';
      default:                        return 'mna-banner';
    }
  }

  bannerIcon(statut: StatutMnaEvaluationAge | null | undefined): string {
    switch (statut) {
      case 'PRIS_EN_CHARGE':          return 'check_circle';
      case 'EXAMEN_OSSEUX_CONTESTE':  return 'fact_check';
      case 'EN_ATTENTE_EVALUATION':   return 'schedule';
      case 'RECOURS_JE_URGENT':       return 'gavel';
      default:                        return 'info_outline';
    }
  }

  /** Recours JE urgent → échéance de saisine du juge des enfants en rouge. */
  isEcheanceUrgente(r: MnaEvaluationAgeResponse | null | undefined): boolean {
    if (!r) return false;
    return r.statut === 'RECOURS_JE_URGENT';
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const dateNaissance = MnaEvaluationAgePrefillRules.computeDateNaissanceDeclaree(input);
    if (dateNaissance !== null && this.dateNaissanceDeclaree() === null) {
      this.dateNaissanceDeclaree.set(dateNaissance);
      this.provenanceDateNaissance.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateNaissanceDeclaree.set(r.dateNaissanceDeclaree ?? null);
        this.evaluationASERefusee.set(r.evaluationASERefusee ?? false);
        this.dateRefusASE.set(r.dateRefusASE ?? null);
        this.examenOsseuxOrdonne.set(r.examenOsseuxOrdonne ?? false);
        this.resultatExamenOsseux.set(r.resultatExamenOsseux ?? null);
        this.provenanceDateNaissance.set(null);
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
