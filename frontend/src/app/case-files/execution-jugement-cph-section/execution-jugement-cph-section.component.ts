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

import { ExecutionJugementCphService } from '../../core/services/execution-jugement-cph.service';
import {
  ExecutionJugementCphRequest,
  ExecutionJugementCphResponse,
  ExecutionJugementCphSituationEmployeur,
  ExecutionJugementCphVerdict,
} from '../../core/models/execution-jugement-cph.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { ExecutionJugementCphPrefillRules } from './execution-jugement-cph-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-04 — Outil décisionnel « Exécution du jugement CPH / AGS » (F-DT-88).
 *
 * FRANCE uniquement — exécution forcée d'un jugement du Conseil de prud'hommes
 * (art. 514 CPC : exécution provisoire de droit de première instance ; R. 1454-28
 * CPC : exécution provisoire de droit des créances salariales) et relais de la
 * garantie AGS lorsque l'employeur est en redressement ou en liquidation judiciaire
 * (L. 3253-6 à L. 3253-21 Code travail). C'est l'outil frère post-jugement de
 * l'appel CPH (F-DT-86, SF-218-02) — même pattern de section.
 *
 * Verdict d'orientation (3 états) :
 *  - EXECUTION_DIRECTE (vert) : employeur in bonis → exécution forcée de droit commun.
 *  - RELAIS_AGS (navy) : employeur en procédure collective → relais garantie AGS / CGEA.
 *  - BLOQUE_INFO_MANQUANTE (or) : procédure collective sans date d'ouverture.
 *
 * Quand l'employeur est en procédure collective, un bloc AGS affiche les plafonds
 * de la garantie (JetBrains Mono, mention « barème à actualiser annuellement ») et
 * les démarches (déclaration de créance au mandataire, saisine CGEA).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; vert EXECUTION_DIRECTE, navy RELAIS_AGS,
 *    or BLOQUE_INFO_MANQUANTE ; rouge réservé aux items bloquants de la checklist
 *  - pré-fill IA (montantCondamnation, situationEmployeur) depuis TravailExtractedData
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 *  - F-IA-03 : coherenceAlerts croise situationEmployeur / dateOuvertureProcedureCollective.
 */
@Component({
  selector: 'app-execution-jugement-cph-section',
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
  templateUrl: './execution-jugement-cph-section.component.html',
  styleUrl: './execution-jugement-cph-section.component.scss',
})
export class ExecutionJugementCphSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-88-execution-jugement-cph';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'EXÉCUTION JUGEMENT CPH / AGS (FR)';
  static readonly TOOL_ICON = 'gavel';

  static getPrefillCount(input: PrefillCountInput): number {
    return ExecutionJugementCphPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<ExecutionJugementCphResponse | null>(null);

  dateJugement = signal<string | null>(null);
  montantCondamnation = signal<number | null>(null);
  executionProvisoireOrdonnee = signal<boolean>(true);
  situationEmployeur = signal<ExecutionJugementCphSituationEmployeur>('IN_BONIS');
  dateOuvertureProcedureCollective = signal<string | null>(null);
  creancesSuperPrivilegiees = signal<number | null>(null);

  provenanceMontant = signal<'IA' | null>(null);
  provenanceSituationEmployeur = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** True quand l'employeur est en procédure collective (redressement / liquidation). */
  procedureCollective = computed<boolean>(() =>
    this.situationEmployeur() === 'REDRESSEMENT' || this.situationEmployeur() === 'LIQUIDATION');

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) croisant la saisie avec
   * l'analyse IA et la cohérence interne du formulaire. Affichées sous le formulaire.
   */
  coherenceAlerts = computed<string[]>(() => {
    const alerts: string[] = [];
    // Procédure collective sélectionnée mais aucune date d'ouverture → analyse bloquée côté backend.
    if (this.procedureCollective() && !this.dateOuvertureProcedureCollective()) {
      alerts.push(
        "Employeur en procédure collective : renseignez la date d'ouverture du redressement / de la liquidation (requise pour le relais AGS).",
      );
    }
    // Divergence entre la situation détectée par l'IA et la saisie.
    const aiSituation = this.aiData?.situationEmployeurDetectee;
    if (aiSituation && aiSituation !== this.situationEmployeur()) {
      alerts.push(
        `L'analyse a détecté un employeur « ${this.situationEmployeurLabel(aiSituation)} » : la situation saisie diffère, vérifiez la source.`,
      );
    }
    return alerts;
  });

  constructor(
    private readonly service: ExecutionJugementCphService,
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
    if (!this.dateJugement()) return false;
    if (this.montantCondamnation() === null || (this.montantCondamnation() ?? 0) <= 0) return false;
    if (this.procedureCollective() && !this.dateOuvertureProcedureCollective()) return false;
    return true;
  }

  onDateJugementChange(value: string | null): void {
    this.dateJugement.set(value || null);
  }

  onMontantChange(value: number | null): void {
    this.montantCondamnation.set(value === null || value === undefined ? null : Number(value));
    this.provenanceMontant.set(null);
  }

  onExecutionProvisoireChange(value: boolean): void {
    this.executionProvisoireOrdonnee.set(value);
  }

  onSituationEmployeurChange(value: ExecutionJugementCphSituationEmployeur): void {
    this.situationEmployeur.set(value);
    this.provenanceSituationEmployeur.set(null);
    // Quitter la procédure collective efface la date d'ouverture devenue sans objet.
    if (!this.procedureCollective()) {
      this.dateOuvertureProcedureCollective.set(null);
    }
  }

  onDateOuvertureChange(value: string | null): void {
    this.dateOuvertureProcedureCollective.set(value || null);
  }

  onCreancesSuperPrivilegieesChange(value: number | null): void {
    this.creancesSuperPrivilegiees.set(value === null || value === undefined ? null : Number(value));
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: ExecutionJugementCphRequest = {
      dateJugement: this.dateJugement()!,
      montantCondamnation: this.montantCondamnation()!,
      executionProvisoireOrdonnee: this.executionProvisoireOrdonnee(),
      situationEmployeur: this.situationEmployeur(),
      dateOuvertureProcedureCollective: this.procedureCollective()
        ? this.dateOuvertureProcedureCollective()
        : null,
      creancesSuperPrivilegiees: this.creancesSuperPrivilegiees(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse exécution jugement CPH enregistrée', 'OK', { duration: 2500 });
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

  verdictLabel(verdict: ExecutionJugementCphVerdict | null | undefined): string {
    switch (verdict) {
      case 'EXECUTION_DIRECTE':     return 'Exécution directe contre l\'employeur';
      case 'RELAIS_AGS':            return 'Relais garantie AGS';
      case 'BLOQUE_INFO_MANQUANTE': return 'Information manquante';
      default:                      return '';
    }
  }

  situationEmployeurLabel(s: ExecutionJugementCphSituationEmployeur | null | undefined): string {
    switch (s) {
      case 'IN_BONIS':     return 'In bonis';
      case 'REDRESSEMENT': return 'Redressement judiciaire';
      case 'LIQUIDATION':  return 'Liquidation judiciaire';
      default:             return '';
    }
  }

  bannerClass(verdict: ExecutionJugementCphVerdict | null | undefined): string {
    switch (verdict) {
      case 'EXECUTION_DIRECTE':     return 'ejc-banner ejc-banner--success';
      case 'RELAIS_AGS':            return 'ejc-banner ejc-banner--info';
      case 'BLOQUE_INFO_MANQUANTE': return 'ejc-banner ejc-banner--warning';
      default:                      return 'ejc-banner';
    }
  }

  bannerIcon(verdict: ExecutionJugementCphVerdict | null | undefined): string {
    switch (verdict) {
      case 'EXECUTION_DIRECTE':     return 'check_circle';
      case 'RELAIS_AGS':            return 'account_balance';
      case 'BLOQUE_INFO_MANQUANTE': return 'help_outline';
      default:                      return 'info_outline';
    }
  }

  /** Le bloc AGS n'a de sens que lorsque la garantie est mobilisable (procédure collective). */
  showAgsBlock(r: ExecutionJugementCphResponse | null | undefined): boolean {
    return !!r && r.agsEligible && !!r.agsInfo;
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const montant = ExecutionJugementCphPrefillRules.computeMontantCondamnation(input);
    if (montant !== null && this.montantCondamnation() === null) {
      this.montantCondamnation.set(montant);
      this.provenanceMontant.set('IA');
    }

    const situation = ExecutionJugementCphPrefillRules.computeSituationEmployeur(input);
    if (situation !== null && this.situationEmployeur() === 'IN_BONIS' && this.provenanceSituationEmployeur() === null) {
      this.situationEmployeur.set(situation);
      this.provenanceSituationEmployeur.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateJugement.set(r.dateJugement ?? null);
        this.montantCondamnation.set(r.montantCondamnation ?? null);
        this.executionProvisoireOrdonnee.set(r.executionProvisoireOrdonnee ?? true);
        this.situationEmployeur.set(r.situationEmployeur ?? 'IN_BONIS');
        this.dateOuvertureProcedureCollective.set(r.dateOuvertureProcedureCollective ?? null);
        this.creancesSuperPrivilegiees.set(r.creancesSuperPrivilegiees ?? null);
        this.provenanceMontant.set(null);
        this.provenanceSituationEmployeur.set(null);
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
