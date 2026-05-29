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

import { AjCndaService } from '../../core/services/aj-cnda.service';
import { AjCndaRequest, AjCndaResponse, StatutAjCnda } from '../../core/models/aj-cnda.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { AjCndaPrefillRules } from './aj-cnda-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-20 — Outil décisionnel « Aide juridictionnelle CNDA » (F-IM-34).
 *
 * FRANCE UNIQUEMENT — l'AJ devant la Cour nationale du droit d'asile est
 * spécifique au droit français. Vérification ressources + calcul des délais
 * critiques (recours CNDA 1 mois, réduit en procédure accélérée) + échéance
 * de dépôt de la demande d'AJ + liste des pièces.
 * Pattern miroir : {@link VlsTsValidationSectionComponent} (F-IM-28).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; chip rouge NON_ELIGIBLE_RESSOURCES /
 *    HORS_DELAI_AJ, vert AJ_DEPOSEE, orange AJ_A_DEMANDER
 *  - pré-fill IA (date décision OFPRA depuis asileDateDecisionAnterieure)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 *
 * <p>Bridge échéance F-69 : si statut AJ_A_DEMANDER, une échéance
 * `dateEcheanceDemandeAJ` est créée via {@link CaseDeadlineService} avec le
 * label « Demande AJ CNDA » (best-effort, n'interrompt pas le flux).
 */
@Component({
  selector: 'app-aj-cnda-section',
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
  templateUrl: './aj-cnda-section.component.html',
  styleUrl: './aj-cnda-section.component.scss',
})
export class AjCndaSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-34-aj-cnda-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'AJ CNDA (FR)';
  static readonly TOOL_ICON = 'balance';

  static getPrefillCount(input: PrefillCountInput): number {
    return AjCndaPrefillRules.computePrefillCount(input);
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
  result = signal<AjCndaResponse | null>(null);
  deadlineCreated = signal(false);

  dateDecisionOFPRA = signal<string | null>(null);
  ressourcesMensuellesNettes = signal<number | null>(null);
  procedureAcceleree = signal<boolean>(false);
  demandeAJDeposee = signal<boolean>(false);
  dateDepotAJ = signal<string | null>(null);

  provenanceDateDecision = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: AjCndaService,
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
    if (!this.dateDecisionOFPRA()) return false;
    if (this.ressourcesMensuellesNettes() === null) return false;
    if ((this.ressourcesMensuellesNettes() ?? -1) < 0) return false;
    // Si une demande d'AJ a déjà été déposée, sa date est requise.
    if (this.demandeAJDeposee() && !this.dateDepotAJ()) return false;
    return true;
  }

  onDateDecisionChange(value: string | null): void {
    this.dateDecisionOFPRA.set(value || null);
    this.provenanceDateDecision.set(null);
  }

  onRessourcesChange(value: number | string | null): void {
    if (value === null || value === '') {
      this.ressourcesMensuellesNettes.set(null);
      return;
    }
    const num = typeof value === 'number' ? value : Number(value);
    this.ressourcesMensuellesNettes.set(Number.isNaN(num) ? null : num);
  }

  onProcedureAccelereeChange(value: boolean): void {
    this.procedureAcceleree.set(value);
  }

  onDemandeDeposeeChange(value: boolean): void {
    this.demandeAJDeposee.set(value);
    if (!value) {
      this.dateDepotAJ.set(null);
    }
  }

  onDateDepotChange(value: string | null): void {
    this.dateDepotAJ.set(value || null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: AjCndaRequest = {
      dateDecisionOFPRA: this.dateDecisionOFPRA()!,
      ressourcesMensuellesNettes: this.ressourcesMensuellesNettes()!,
      procedureAcceleree: this.procedureAcceleree(),
      demandeAJDeposee: this.demandeAJDeposee(),
      dateDepotAJ: this.demandeAJDeposee() ? this.dateDepotAJ() : null,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse AJ CNDA enregistrée', 'OK', { duration: 2500 });
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
   * Bridge échéance F-69 — si le statut est AJ_A_DEMANDER, on alimente
   * `app-case-deadlines-section` avec une échéance `dateEcheanceDemandeAJ`.
   * Best-effort : un échec n'interrompt pas le flux.
   */
  private maybeCreateDeadline(r: AjCndaResponse): void {
    if (!this.deadlineService || !this.caseFileId) return;
    if (r.statut !== 'AJ_A_DEMANDER') return;
    if (!r.dateEcheanceDemandeAJ) return;
    this.deadlineService.create(this.caseFileId, 'Demande AJ CNDA', r.dateEcheanceDemandeAJ).subscribe({
      next: () => {
        this.deadlineCreated.set(true);
        this.cdr.markForCheck();
      },
      error: () => {
        // Best-effort : on ne bloque pas l'utilisateur sur l'échéance.
      },
    });
  }

  statutLabel(statut: StatutAjCnda | null | undefined): string {
    switch (statut) {
      case 'AJ_A_DEMANDER':           return 'AJ à demander';
      case 'AJ_DEPOSEE':              return 'Demande AJ déposée';
      case 'HORS_DELAI_AJ':           return "Hors délai pour l'AJ";
      case 'NON_ELIGIBLE_RESSOURCES': return 'Non éligible (ressources)';
      default:                        return '';
    }
  }

  bannerClass(statut: StatutAjCnda | null | undefined): string {
    switch (statut) {
      case 'AJ_DEPOSEE':              return 'aj-banner aj-banner--success';
      case 'AJ_A_DEMANDER':           return 'aj-banner aj-banner--warning';
      case 'HORS_DELAI_AJ':           return 'aj-banner aj-banner--danger';
      case 'NON_ELIGIBLE_RESSOURCES': return 'aj-banner aj-banner--danger';
      default:                        return 'aj-banner';
    }
  }

  bannerIcon(statut: StatutAjCnda | null | undefined): string {
    switch (statut) {
      case 'AJ_DEPOSEE':              return 'check_circle';
      case 'AJ_A_DEMANDER':           return 'schedule';
      case 'HORS_DELAI_AJ':           return 'error';
      case 'NON_ELIGIBLE_RESSOURCES': return 'block';
      default:                        return 'info_outline';
    }
  }

  /** Échéance de demande d'AJ urgente / dépassée → mise en évidence rouge. */
  isEcheanceUrgente(r: AjCndaResponse | null | undefined): boolean {
    if (!r) return false;
    return r.statut === 'AJ_A_DEMANDER' || r.statut === 'HORS_DELAI_AJ';
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const dateDecision = AjCndaPrefillRules.computeDateDecisionOFPRA(input);
    if (dateDecision !== null && this.dateDecisionOFPRA() === null) {
      this.dateDecisionOFPRA.set(dateDecision);
      this.provenanceDateDecision.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateDecisionOFPRA.set(r.dateDecisionOFPRA ?? null);
        this.ressourcesMensuellesNettes.set(r.ressourcesMensuellesNettes ?? null);
        this.procedureAcceleree.set(r.procedureAcceleree ?? false);
        this.demandeAJDeposee.set(r.demandeAJDeposee ?? false);
        this.dateDepotAJ.set(r.dateDepotAJ ?? null);
        this.provenanceDateDecision.set(null);
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
