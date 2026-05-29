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
import { MatSnackBar } from '@angular/material/snack-bar';

import { ItfJudiciaireService } from '../../core/services/itf-judiciaire.service';
import {
  ItfJudiciaireRequest,
  ItfJudiciaireResponse,
  StatutItfJudiciaire,
} from '../../core/models/itf-judiciaire.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { ItfJudiciairePrefillRules } from './itf-judiciaire-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-38 — Outil décisionnel « ITF judiciaire » (F-IM-43).
 *
 * FRANCE uniquement — interdiction du territoire français (ITF) prononcée par le
 * juge pénal à titre de peine principale ou complémentaire (art. 131-30 et s. du
 * Code pénal). À distinguer de l'IRTF (interdiction de retour, mesure
 * administrative préfectorale, CESEDA) — c'est l'objet de l'encadré bleu
 * explicatif `distinctionItfVsIrtf`.
 *
 * Analyseur de validité / délais : statut de la voie de recours pénale
 * (APPEL_POSSIBLE / RECOURS_PRESCRIT / RELEVE_POSSIBLE / EN_COURS_PURGE) +
 * échéance du relevé + voies de recours ouvertes (délais) + conditions du relevé.
 * Pattern miroir : {@link AssignationResidenceSectionComponent} (F-IM-42).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or
 *  - pré-fill IA (date de condamnation + durée de l'ITF)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 *
 * <p>Bridge échéance F-69 : si statut APPEL_POSSIBLE, une échéance de recours
 * pénal est créée via {@link CaseDeadlineService} avec le label
 * « Recours pénal ITF » (best-effort, n'interrompt pas le flux). La date retenue
 * est `dateEcheanceRecoursPenal` (saisie) si disponible, sinon `dateEcheanceReleve`.
 */
@Component({
  selector: 'app-itf-judiciaire-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './itf-judiciaire-section.component.html',
  styleUrl: './itf-judiciaire-section.component.scss',
})
export class ItfJudiciaireSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-43-itf-judiciaire-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'ITF JUDICIAIRE (FR)';
  static readonly TOOL_ICON = 'gavel';

  static getPrefillCount(input: PrefillCountInput): number {
    return ItfJudiciairePrefillRules.computePrefillCount(input);
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
  result = signal<ItfJudiciaireResponse | null>(null);
  deadlineCreated = signal(false);

  dateCondamnation = signal<string | null>(null);
  dureeITFAnnees = signal<number | null>(null);
  infractionPrincipale = signal<string | null>(null);
  condamnationDefinitive = signal<boolean>(false);
  dateEcheanceRecoursPenal = signal<string | null>(null);

  provenanceDateCondamnation = signal<'IA' | null>(null);
  provenanceDuree = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: ItfJudiciaireService,
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
    if (!this.dateCondamnation()) return false;
    const duree = this.dureeITFAnnees();
    if (duree === null || duree <= 0) return false;
    const infraction = this.infractionPrincipale();
    if (!infraction || infraction.trim().length === 0) return false;
    if (infraction.length > 200) return false;
    return true;
  }

  onDateCondamnationChange(value: string | null): void {
    this.dateCondamnation.set(value || null);
    this.provenanceDateCondamnation.set(null);
  }

  onDureeChange(value: number | null): void {
    this.dureeITFAnnees.set(value ?? null);
    this.provenanceDuree.set(null);
  }

  onInfractionChange(value: string | null): void {
    this.infractionPrincipale.set(value || null);
  }

  onCondamnationDefinitiveChange(value: boolean): void {
    this.condamnationDefinitive.set(value);
  }

  onDateEcheanceRecoursChange(value: string | null): void {
    this.dateEcheanceRecoursPenal.set(value || null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: ItfJudiciaireRequest = {
      dateCondamnation: this.dateCondamnation()!,
      dureeITFAnnees: this.dureeITFAnnees()!,
      infractionPrincipale: this.infractionPrincipale()!,
      condamnationDefinitive: this.condamnationDefinitive(),
      dateEcheanceRecoursPenal: this.dateEcheanceRecoursPenal(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse ITF judiciaire enregistrée', 'OK', { duration: 2500 });
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
   * Bridge échéance F-69 — si le statut est APPEL_POSSIBLE, on alimente
   * `app-case-deadlines-section` avec l'échéance du recours pénal. La date
   * retenue est la date d'échéance du recours pénal saisie si disponible, sinon
   * la date d'échéance du relevé renvoyée par le backend.
   * Best-effort : un échec n'interrompt pas le flux.
   */
  private maybeCreateDeadline(r: ItfJudiciaireResponse): void {
    if (!this.deadlineService || !this.caseFileId) return;
    if (r.statut !== 'APPEL_POSSIBLE') return;
    const dueDate = this.dateEcheanceRecoursPenal() ?? r.dateEcheanceReleve;
    if (!dueDate) return;
    this.deadlineService.create(this.caseFileId, 'Recours pénal ITF', dueDate).subscribe({
      next: () => {
        this.deadlineCreated.set(true);
        this.cdr.markForCheck();
      },
      error: () => {
        // Best-effort : on ne bloque pas l'utilisateur sur l'échéance.
      },
    });
  }

  statutLabel(statut: StatutItfJudiciaire | null | undefined): string {
    switch (statut) {
      case 'APPEL_POSSIBLE':  return 'Appel possible';
      case 'RELEVE_POSSIBLE': return 'Relevé possible';
      case 'EN_COURS_PURGE':  return "En cours d'exécution";
      case 'RECOURS_PRESCRIT': return 'Recours prescrit';
      default:                return '';
    }
  }

  bannerClass(statut: StatutItfJudiciaire | null | undefined): string {
    switch (statut) {
      case 'APPEL_POSSIBLE':   return 'acc-banner acc-banner--success';
      case 'RELEVE_POSSIBLE':  return 'acc-banner acc-banner--info';
      case 'EN_COURS_PURGE':   return 'acc-banner acc-banner--warning';
      case 'RECOURS_PRESCRIT': return 'acc-banner acc-banner--danger';
      default:                 return 'acc-banner';
    }
  }

  bannerIcon(statut: StatutItfJudiciaire | null | undefined): string {
    switch (statut) {
      case 'APPEL_POSSIBLE':   return 'check_circle';
      case 'RELEVE_POSSIBLE':  return 'info_outline';
      case 'EN_COURS_PURGE':   return 'hourglass_top';
      case 'RECOURS_PRESCRIT': return 'error';
      default:                 return 'info_outline';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const dateCond = ItfJudiciairePrefillRules.computeDateCondamnation(input);
    if (dateCond !== null && this.dateCondamnation() === null) {
      this.dateCondamnation.set(dateCond);
      this.provenanceDateCondamnation.set('IA');
    }

    const duree = ItfJudiciairePrefillRules.computeDureeAnnees(input);
    if (duree !== null && this.dureeITFAnnees() === null) {
      this.dureeITFAnnees.set(duree);
      this.provenanceDuree.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateCondamnation.set(r.dateCondamnation ?? null);
        this.dureeITFAnnees.set(r.dureeITFAnnees ?? null);
        this.infractionPrincipale.set(r.infractionPrincipale ?? null);
        this.condamnationDefinitive.set(r.condamnationDefinitive ?? false);
        this.dateEcheanceRecoursPenal.set(r.dateEcheanceRecoursPenal ?? null);
        this.provenanceDateCondamnation.set(null);
        this.provenanceDuree.set(null);
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
