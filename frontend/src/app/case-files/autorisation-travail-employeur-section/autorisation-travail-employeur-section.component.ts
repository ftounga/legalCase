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

import { AutorisationTravailEmployeurService } from '../../core/services/autorisation-travail-employeur.service';
import {
  AutorisationTravailEmployeurRequest,
  AutorisationTravailEmployeurResponse,
  StatutAutorisationTravailEmployeur,
  TypeContratAutorisationTravail,
} from '../../core/models/autorisation-travail-employeur.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { AutorisationTravailEmployeurPrefillRules } from './autorisation-travail-employeur-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-44 — Outil décisionnel « Autorisation travail employeur » (F-IM-46).
 *
 * FRANCE uniquement — obligations de l'employeur qui souhaite embaucher un
 * ressortissant étranger (procédure de demande d'autorisation de travail
 * auprès de l'OFII / plateforme dédiée). Outil côté employeur, complémentaire à
 * F-IM-07 (côté salarié).
 *
 * Niveau outil : 2 (checklist procédure employeur — obligations de la demande,
 * délai d'instruction OFII, taxe OFII) + 3 (calculateur de délai du recours
 * devant le TA en cas de refus).
 * Pattern miroir : {@link RetraitTitreFraudeSectionComponent} (F-IM-45).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; vert AUTORISATION_NON_REQUISE, bleu
 *    AUTORISATION_REQUISE, orange RECOURS_POSSIBLE, rouge RECOURS_PRESCRIT
 *  - pré-fill IA (nationalité du candidat) — réutilise `aiData.nationalite`
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 *
 * <p>Bridge échéance F-69 : si statut RECOURS_POSSIBLE, une échéance
 * `delaiRecoursTa` est créée via {@link CaseDeadlineService} avec le label
 * « Recours TA autorisation travail » (best-effort, n'interrompt pas le flux).
 */
@Component({
  selector: 'app-autorisation-travail-employeur-section',
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
  templateUrl: './autorisation-travail-employeur-section.component.html',
  styleUrl: './autorisation-travail-employeur-section.component.scss',
})
export class AutorisationTravailEmployeurSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-46-autorisation-travail-employeur-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'AUTORISATION TRAVAIL EMPLOYEUR (FR)';
  static readonly TOOL_ICON = 'badge';

  static getPrefillCount(input: PrefillCountInput): number {
    return AutorisationTravailEmployeurPrefillRules.computePrefillCount(input);
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
  result = signal<AutorisationTravailEmployeurResponse | null>(null);
  deadlineCreated = signal(false);

  typeContrat = signal<TypeContratAutorisationTravail>('CDI');
  posteProposes = signal<string>('');
  nationaliteCandidat = signal<string>('');
  dureeContratMois = signal<number | null>(null);
  refusAutorisation = signal<boolean>(false);
  dateRefusAutorisation = signal<string | null>(null);

  provenanceNationalite = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: AutorisationTravailEmployeurService,
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
    if (this.posteProposes().trim().length === 0) return false;
    if (this.nationaliteCandidat().trim().length === 0) return false;
    return true;
  }

  onTypeContratChange(value: TypeContratAutorisationTravail): void {
    this.typeContrat.set(value);
  }

  onPosteProposesChange(value: string): void {
    this.posteProposes.set(value ?? '');
  }

  onNationaliteCandidatChange(value: string): void {
    this.nationaliteCandidat.set(value ?? '');
    this.provenanceNationalite.set(null);
  }

  onDureeContratMoisChange(value: number | null): void {
    this.dureeContratMois.set(value ?? null);
  }

  onRefusAutorisationChange(value: boolean): void {
    this.refusAutorisation.set(value);
    if (!value) {
      this.dateRefusAutorisation.set(null);
    }
  }

  onDateRefusAutorisationChange(value: string | null): void {
    this.dateRefusAutorisation.set(value || null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: AutorisationTravailEmployeurRequest = {
      typeContrat: this.typeContrat(),
      posteProposes: this.posteProposes().trim(),
      nationaliteCandidat: this.nationaliteCandidat().trim(),
      dureeContratMois: this.dureeContratMois(),
      refusAutorisation: this.refusAutorisation(),
      dateRefusAutorisation: this.dateRefusAutorisation(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse autorisation travail employeur enregistrée', 'OK', { duration: 2500 });
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
   * Bridge échéance F-69 — si le statut est RECOURS_POSSIBLE (refus
   * d'autorisation contestable), on alimente `app-case-deadlines-section` avec
   * l'échéance `delaiRecoursTa` (délai TA 2 mois).
   * Best-effort : un échec n'interrompt pas le flux (le dashboard refresh
   * affichera l'analyse quoi qu'il arrive).
   */
  private maybeCreateDeadline(r: AutorisationTravailEmployeurResponse): void {
    if (!this.deadlineService || !this.caseFileId) return;
    if (r.statut !== 'RECOURS_POSSIBLE') return;
    if (!r.delaiRecoursTa) return;
    this.deadlineService.create(this.caseFileId, 'Recours TA autorisation travail', r.delaiRecoursTa).subscribe({
      next: () => {
        this.deadlineCreated.set(true);
        this.cdr.markForCheck();
      },
      error: () => {
        // Best-effort : on ne bloque pas l'utilisateur sur l'échéance.
      },
    });
  }

  statutLabel(statut: StatutAutorisationTravailEmployeur | null | undefined): string {
    switch (statut) {
      case 'AUTORISATION_NON_REQUISE': return 'Autorisation non requise';
      case 'AUTORISATION_REQUISE':     return 'Autorisation requise';
      case 'RECOURS_POSSIBLE':         return 'Recours possible';
      case 'RECOURS_PRESCRIT':         return 'Délai de recours prescrit';
      default:                         return '';
    }
  }

  typeContratLabel(type: TypeContratAutorisationTravail | null | undefined): string {
    switch (type) {
      case 'CDI':     return 'CDI';
      case 'CDD':     return 'CDD';
      case 'INTERIM': return 'Intérim';
      default:        return '';
    }
  }

  bannerClass(statut: StatutAutorisationTravailEmployeur | null | undefined): string {
    switch (statut) {
      case 'AUTORISATION_NON_REQUISE': return 'acc-banner acc-banner--success';
      case 'AUTORISATION_REQUISE':     return 'acc-banner acc-banner--info';
      case 'RECOURS_POSSIBLE':         return 'acc-banner acc-banner--warning';
      case 'RECOURS_PRESCRIT':         return 'acc-banner acc-banner--danger';
      default:                         return 'acc-banner';
    }
  }

  bannerIcon(statut: StatutAutorisationTravailEmployeur | null | undefined): string {
    switch (statut) {
      case 'AUTORISATION_NON_REQUISE': return 'check_circle';
      case 'AUTORISATION_REQUISE':     return 'assignment';
      case 'RECOURS_POSSIBLE':         return 'gavel';
      case 'RECOURS_PRESCRIT':         return 'error';
      default:                         return 'info_outline';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const nationalite = AutorisationTravailEmployeurPrefillRules.computeNationaliteCandidat(input);
    if (nationalite !== null && this.nationaliteCandidat().trim().length === 0) {
      this.nationaliteCandidat.set(nationalite);
      this.provenanceNationalite.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.typeContrat.set(r.typeContrat ?? 'CDI');
        this.posteProposes.set(r.posteProposes ?? '');
        this.nationaliteCandidat.set(r.nationaliteCandidat ?? '');
        this.dureeContratMois.set(r.dureeContratMois ?? null);
        this.refusAutorisation.set(r.refusAutorisation ?? false);
        this.dateRefusAutorisation.set(r.dateRefusAutorisation ?? null);
        this.provenanceNationalite.set(null);
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
