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
import { MatSnackBar } from '@angular/material/snack-bar';

import { NaturalisationRecoursTjService } from '../../core/services/naturalisation-recours-tj.service';
import {
  NaturalisationRecoursTjRequest,
  NaturalisationRecoursTjResponse,
  StatutNaturalisationRecoursTj,
  TypeRefus,
  VoieNaturalisation,
} from '../../core/models/naturalisation-recours-tj.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { NaturalisationRecoursTjPrefillRules } from './naturalisation-recours-tj-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-30 — Outil décisionnel « Recours TJ naturalisation » (F-IM-39).
 *
 * FRANCE uniquement — recours juridictionnel devant le tribunal judiciaire
 * contre un refus d'enregistrement d'une déclaration de nationalité ou une
 * contestation de nationalité, dans le délai de 6 mois (C. civ. art. 26-3,
 * 26-4 ; CPC art. 1040 et suivants).
 * Calculateur de délai (palette rouge PRESCRIT / orange URGENT / vert
 * RECOURS_POSSIBLE).
 * Pattern miroir : {@link VlsTsValidationSectionComponent} (F-IM-28) et
 * {@link AjCndaSectionComponent} (F-IM-34).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; vert RECOURS_POSSIBLE, orange URGENT,
 *    rouge PRESCRIT
 *  - pré-fill IA (voie de naturalisation + date de refus)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 *
 * <p>Bridge échéance F-69 : si statut RECOURS_POSSIBLE ou URGENT, une échéance
 * `dateEcheanceRecoursJudicaire` est créée via {@link CaseDeadlineService} avec
 * le label « Recours TJ naturalisation » (best-effort, n'interrompt pas le flux).
 */
@Component({
  selector: 'app-naturalisation-recours-tj-section',
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
  templateUrl: './naturalisation-recours-tj-section.component.html',
  styleUrl: './naturalisation-recours-tj-section.component.scss',
})
export class NaturalisationRecoursTjSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-39-naturalisation-recours-tj-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'RECOURS TJ NATURALISATION (FR)';
  static readonly TOOL_ICON = 'gavel';

  static getPrefillCount(input: PrefillCountInput): number {
    return NaturalisationRecoursTjPrefillRules.computePrefillCount(input);
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
  result = signal<NaturalisationRecoursTjResponse | null>(null);
  deadlineCreated = signal(false);

  voieNaturalisation = signal<VoieNaturalisation | null>(null);
  dateRefusDeclaration = signal<string | null>(null);
  typeRefus = signal<TypeRefus | null>(null);

  provenanceVoie = signal<'IA' | null>(null);
  provenanceDateRefus = signal<'IA' | null>(null);

  readonly voieOptions: ReadonlyArray<{ code: VoieNaturalisation; label: string }> = [
    { code: 'MARIAGE', label: 'Déclaration par mariage (art. 21-2)' },
    { code: 'ASCENDANT', label: 'Déclaration ascendant de Français (art. 21-13-1)' },
    { code: 'MINEUR_22_1', label: 'Déclaration mineur né en France (art. 21-11)' },
  ];

  readonly typeRefusOptions: ReadonlyArray<{ code: TypeRefus; label: string }> = [
    { code: 'REFUS_ENREGISTREMENT', label: "Refus d'enregistrement de la déclaration" },
    { code: 'CONTESTATION_NATIONALITE', label: 'Contestation de nationalité' },
  ];

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: NaturalisationRecoursTjService,
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
    if (!this.voieNaturalisation()) return false;
    if (!this.dateRefusDeclaration()) return false;
    if (!this.typeRefus()) return false;
    return true;
  }

  onVoieChange(value: VoieNaturalisation | null): void {
    this.voieNaturalisation.set(value);
    this.provenanceVoie.set(null);
  }

  onDateRefusChange(value: string | null): void {
    this.dateRefusDeclaration.set(value || null);
    this.provenanceDateRefus.set(null);
  }

  onTypeRefusChange(value: TypeRefus | null): void {
    this.typeRefus.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: NaturalisationRecoursTjRequest = {
      voieNaturalisation: this.voieNaturalisation()!,
      dateRefusDeclaration: this.dateRefusDeclaration()!,
      typeRefus: this.typeRefus()!,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse recours TJ naturalisation enregistrée', 'OK', { duration: 2500 });
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
   * Bridge échéance F-69 — si le statut est RECOURS_POSSIBLE ou URGENT, on
   * alimente `app-case-deadlines-section` avec l'échéance
   * `dateEcheanceRecoursJudicaire`. Best-effort : un échec n'interrompt pas
   * le flux (le dashboard refresh affichera l'analyse quoi qu'il arrive).
   */
  private maybeCreateDeadline(r: NaturalisationRecoursTjResponse): void {
    if (!this.deadlineService || !this.caseFileId) return;
    if (r.statut !== 'RECOURS_POSSIBLE' && r.statut !== 'URGENT') return;
    if (!r.dateEcheanceRecoursJudicaire) return;
    this.deadlineService.create(this.caseFileId, 'Recours TJ naturalisation', r.dateEcheanceRecoursJudicaire).subscribe({
      next: () => {
        this.deadlineCreated.set(true);
        this.cdr.markForCheck();
      },
      error: () => {
        // Best-effort : on ne bloque pas l'utilisateur sur l'échéance.
      },
    });
  }

  statutLabel(statut: StatutNaturalisationRecoursTj | null | undefined): string {
    switch (statut) {
      case 'RECOURS_POSSIBLE': return 'Recours judiciaire possible';
      case 'URGENT':           return 'Recours judiciaire urgent';
      case 'PRESCRIT':         return 'Délai de recours prescrit';
      default:                 return '';
    }
  }

  bannerClass(statut: StatutNaturalisationRecoursTj | null | undefined): string {
    switch (statut) {
      case 'RECOURS_POSSIBLE': return 'nrt-banner nrt-banner--success';
      case 'URGENT':           return 'nrt-banner nrt-banner--warning';
      case 'PRESCRIT':         return 'nrt-banner nrt-banner--danger';
      default:                 return 'nrt-banner';
    }
  }

  bannerIcon(statut: StatutNaturalisationRecoursTj | null | undefined): string {
    switch (statut) {
      case 'RECOURS_POSSIBLE': return 'check_circle';
      case 'URGENT':           return 'warning';
      case 'PRESCRIT':         return 'error';
      default:                 return 'info_outline';
    }
  }

  voieLabel(voie: VoieNaturalisation | null | undefined): string {
    switch (voie) {
      case 'MARIAGE':     return 'Déclaration par mariage (art. 21-2)';
      case 'ASCENDANT':   return 'Déclaration ascendant de Français (art. 21-13-1)';
      case 'MINEUR_22_1': return 'Déclaration mineur né en France (art. 21-11)';
      default:            return '';
    }
  }

  typeRefusLabel(type: TypeRefus | null | undefined): string {
    switch (type) {
      case 'REFUS_ENREGISTREMENT':     return "Refus d'enregistrement de la déclaration";
      case 'CONTESTATION_NATIONALITE': return 'Contestation de nationalité';
      default:                         return '';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const voie = NaturalisationRecoursTjPrefillRules.computeVoieNaturalisation(input);
    if (voie !== null && this.voieNaturalisation() === null) {
      this.voieNaturalisation.set(voie);
      this.provenanceVoie.set('IA');
    }

    const dateRefus = NaturalisationRecoursTjPrefillRules.computeDateRefusDeclaration(input);
    if (dateRefus !== null && this.dateRefusDeclaration() === null) {
      this.dateRefusDeclaration.set(dateRefus);
      this.provenanceDateRefus.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.voieNaturalisation.set(r.voieNaturalisation ?? null);
        this.dateRefusDeclaration.set(r.dateRefusDeclaration ?? null);
        this.typeRefus.set(r.typeRefus ?? null);
        this.provenanceVoie.set(null);
        this.provenanceDateRefus.set(null);
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
