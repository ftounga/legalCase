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
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar } from '@angular/material/snack-bar';

import { VlsTsValidationService } from '../../core/services/vls-ts-validation.service';
import {
  StatutVlsTsValidation,
  TypeVlsTs,
  VlsTsValidationRequest,
  VlsTsValidationResponse,
} from '../../core/models/vls-ts-validation.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { VlsTsValidationPrefillRules } from './vls-ts-validation-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-08 — Outil décisionnel « Validation VLS-TS OFII » (F-IM-28).
 *
 * FR uniquement (CESEDA R.431-16+ — le titulaire d'un VLS-TS doit le faire
 * valider auprès de l'OFII dans les 3 mois suivant son entrée en France).
 * Calculateur de délai (palette rouge URGENT/EXPIRE, similaire F-208 JLD).
 * Pattern miroir : {@link RegroupementFamilialSectionComponent} (F-IM-26).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; vert VALIDE, orange URGENT,
 *    rouge EXPIRE, neutre A_VALIDER
 *  - pré-fill IA (date d'entrée en France)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 *
 * <p>Bridge échéance F-69 : si statut A_VALIDER ou URGENT, une échéance
 * `dateEcheanceValidation` est créée via {@link CaseDeadlineService} avec le
 * label « Validation VLS-TS OFII » (best-effort, n'interrompt pas le flux).
 */
@Component({
  selector: 'app-vls-ts-validation-section',
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
  templateUrl: './vls-ts-validation-section.component.html',
  styleUrl: './vls-ts-validation-section.component.scss',
})
export class VlsTsValidationSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-28-vls-ts-validation-ofii-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'VALIDATION VLS-TS OFII (FR)';
  static readonly TOOL_ICON = 'event_available';

  static getPrefillCount(input: PrefillCountInput): number {
    return VlsTsValidationPrefillRules.computePrefillCount(input);
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
  result = signal<VlsTsValidationResponse | null>(null);
  deadlineCreated = signal(false);

  dateEntreeFrance = signal<string | null>(null);
  typeVlsTs = signal<TypeVlsTs | null>(null);
  validationOFIIEffectuee = signal<boolean>(false);
  dateValidationOFII = signal<string | null>(null);

  provenanceDateEntree = signal<'IA' | null>(null);

  readonly typeOptions: ReadonlyArray<{ code: TypeVlsTs; label: string }> = [
    { code: 'ETUDIANT', label: 'Étudiant' },
    { code: 'SALARIE', label: 'Salarié' },
    { code: 'VISITEUR', label: 'Visiteur' },
    { code: 'CONJOINT_FRANCAIS', label: 'Conjoint de Français' },
    { code: 'AUTRE', label: 'Autre' },
  ];

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: VlsTsValidationService,
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
    if (!this.dateEntreeFrance()) return false;
    if (!this.typeVlsTs()) return false;
    // Si la validation a été effectuée, sa date est requise.
    if (this.validationOFIIEffectuee() && !this.dateValidationOFII()) return false;
    return true;
  }

  onDateEntreeChange(value: string | null): void {
    this.dateEntreeFrance.set(value || null);
    this.provenanceDateEntree.set(null);
  }

  onTypeChange(value: TypeVlsTs | null): void {
    this.typeVlsTs.set(value);
  }

  onValidationEffectueeChange(value: boolean): void {
    this.validationOFIIEffectuee.set(value);
    if (!value) {
      this.dateValidationOFII.set(null);
    }
  }

  onDateValidationChange(value: string | null): void {
    this.dateValidationOFII.set(value || null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: VlsTsValidationRequest = {
      dateEntreeFrance: this.dateEntreeFrance()!,
      typeVlsTs: this.typeVlsTs()!,
      validationOFIIEffectuee: this.validationOFIIEffectuee(),
      dateValidationOFII: this.validationOFIIEffectuee() ? this.dateValidationOFII() : null,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse validation VLS-TS OFII enregistrée', 'OK', { duration: 2500 });
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
   * Bridge échéance F-69 — si le statut est A_VALIDER ou URGENT, on alimente
   * `app-case-deadlines-section` avec une échéance `dateEcheanceValidation`.
   * Best-effort : un échec n'interrompt pas le flux (le dashboard refresh
   * affichera l'analyse quoi qu'il arrive).
   */
  private maybeCreateDeadline(r: VlsTsValidationResponse): void {
    if (!this.deadlineService || !this.caseFileId) return;
    if (r.statut !== 'A_VALIDER' && r.statut !== 'URGENT') return;
    if (!r.dateEcheanceValidation) return;
    this.deadlineService.create(this.caseFileId, 'Validation VLS-TS OFII', r.dateEcheanceValidation).subscribe({
      next: () => {
        this.deadlineCreated.set(true);
        this.cdr.markForCheck();
      },
      error: () => {
        // Best-effort : on ne bloque pas l'utilisateur sur l'échéance.
      },
    });
  }

  statutLabel(statut: StatutVlsTsValidation | null | undefined): string {
    switch (statut) {
      case 'VALIDE':    return 'Validation OFII effectuée';
      case 'A_VALIDER': return 'À valider auprès de l’OFII';
      case 'URGENT':    return 'Validation OFII urgente';
      case 'EXPIRE':    return 'Délai de validation expiré';
      default:          return '';
    }
  }

  bannerClass(statut: StatutVlsTsValidation | null | undefined): string {
    switch (statut) {
      case 'VALIDE':    return 'vls-banner vls-banner--success';
      case 'A_VALIDER': return 'vls-banner vls-banner--info';
      case 'URGENT':    return 'vls-banner vls-banner--warning';
      case 'EXPIRE':    return 'vls-banner vls-banner--danger';
      default:          return 'vls-banner';
    }
  }

  bannerIcon(statut: StatutVlsTsValidation | null | undefined): string {
    switch (statut) {
      case 'VALIDE':    return 'check_circle';
      case 'A_VALIDER': return 'info_outline';
      case 'URGENT':    return 'warning';
      case 'EXPIRE':    return 'error';
      default:          return 'info_outline';
    }
  }

  typeLabel(type: TypeVlsTs | null | undefined): string {
    switch (type) {
      case 'ETUDIANT':          return 'Étudiant';
      case 'SALARIE':           return 'Salarié';
      case 'VISITEUR':          return 'Visiteur';
      case 'CONJOINT_FRANCAIS': return 'Conjoint de Français';
      case 'AUTRE':             return 'Autre';
      default:                  return '';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const dateEntree = VlsTsValidationPrefillRules.computeDateEntreeFrance(input);
    if (dateEntree !== null && this.dateEntreeFrance() === null) {
      this.dateEntreeFrance.set(dateEntree);
      this.provenanceDateEntree.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateEntreeFrance.set(r.dateEntreeFrance ?? null);
        this.typeVlsTs.set(r.typeVlsTs ?? null);
        this.validationOFIIEffectuee.set(r.validationOFIIEffectuee ?? false);
        this.dateValidationOFII.set(r.dateValidationOFII ?? null);
        this.provenanceDateEntree.set(null);
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
