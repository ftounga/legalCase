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

import { RenouvellementDelaiService } from '../../core/services/renouvellement-delai.service';
import {
  RenouvellementDelaiRequest,
  RenouvellementDelaiResponse,
  StatutRenouvellementDelai,
} from '../../core/models/renouvellement-delai.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { RenouvellementDelaiPrefillRules } from './renouvellement-delai-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-14 — Outil décisionnel « Renouvellement délai dépôt » (F-IM-31).
 *
 * FR uniquement (CESEDA — la demande de renouvellement d'un titre de séjour doit
 * être déposée dans les 2 mois précédant l'expiration ; passé l'expiration, le
 * titulaire tombe en situation irrégulière). Calculateur de délai (palette rouge
 * EXPIRE/URGENT, orange A_DEPOSER, vert EN_AVANCE/DEPOSE).
 * Pattern miroir : {@link VlsTsValidationSectionComponent} (F-IM-28).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; vert EN_AVANCE/DEPOSE, orange A_DEPOSER,
 *    orange-rouge A_DEPOSER_URGENT, rouge EXPIRE
 *  - pré-fill IA (date d'expiration du titre + type de titre)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 *
 * <p>Bridge échéance F-69 : si statut A_DEPOSER ou A_DEPOSER_URGENT, une échéance
 * `dateOptimalDepot` est créée via {@link CaseDeadlineService} avec le label
 * « Dépôt renouvellement titre » (best-effort, n'interrompt pas le flux).
 */
@Component({
  selector: 'app-renouvellement-delai-section',
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
  templateUrl: './renouvellement-delai-section.component.html',
  styleUrl: './renouvellement-delai-section.component.scss',
})
export class RenouvellementDelaiSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-31-renouvellement-delai-depot-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'RENOUVELLEMENT DÉLAI DÉPÔT (FR)';
  static readonly TOOL_ICON = 'event_repeat';

  static getPrefillCount(input: PrefillCountInput): number {
    return RenouvellementDelaiPrefillRules.computePrefillCount(input);
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
  result = signal<RenouvellementDelaiResponse | null>(null);
  deadlineCreated = signal(false);

  dateExpirationTitre = signal<string | null>(null);
  dateDepotDossier = signal<string | null>(null);
  typeTitre = signal<string | null>(null);

  provenanceDateExpiration = signal<'IA' | null>(null);
  provenanceTypeTitre = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: RenouvellementDelaiService,
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
    return !!this.dateExpirationTitre();
  }

  onDateExpirationChange(value: string | null): void {
    this.dateExpirationTitre.set(value || null);
    this.provenanceDateExpiration.set(null);
  }

  onDateDepotChange(value: string | null): void {
    this.dateDepotDossier.set(value || null);
  }

  onTypeTitreChange(value: string | null): void {
    this.typeTitre.set(value || null);
    this.provenanceTypeTitre.set(null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: RenouvellementDelaiRequest = {
      dateExpirationTitre: this.dateExpirationTitre()!,
      dateDepotDossier: this.dateDepotDossier(),
      typeTitre: this.typeTitre(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse renouvellement délai dépôt enregistrée', 'OK', { duration: 2500 });
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
   * Bridge échéance F-69 — si le statut est A_DEPOSER ou A_DEPOSER_URGENT, on
   * alimente `app-case-deadlines-section` avec une échéance `dateOptimalDepot`.
   * Best-effort : un échec n'interrompt pas le flux (le dashboard refresh
   * affichera l'analyse quoi qu'il arrive).
   */
  private maybeCreateDeadline(r: RenouvellementDelaiResponse): void {
    if (!this.deadlineService || !this.caseFileId) return;
    if (r.statut !== 'A_DEPOSER' && r.statut !== 'A_DEPOSER_URGENT') return;
    if (!r.dateOptimalDepot) return;
    this.deadlineService.create(this.caseFileId, 'Dépôt renouvellement titre', r.dateOptimalDepot).subscribe({
      next: () => {
        this.deadlineCreated.set(true);
        this.cdr.markForCheck();
      },
      error: () => {
        // Best-effort : on ne bloque pas l'utilisateur sur l'échéance.
      },
    });
  }

  statutLabel(statut: StatutRenouvellementDelai | null | undefined): string {
    switch (statut) {
      case 'EN_AVANCE':        return 'Délai de dépôt confortable';
      case 'A_DEPOSER':        return 'À déposer';
      case 'A_DEPOSER_URGENT': return 'À déposer en urgence';
      case 'EXPIRE':           return 'Titre expiré';
      case 'DEPOSE':           return 'Demande déjà déposée';
      default:                 return '';
    }
  }

  bannerClass(statut: StatutRenouvellementDelai | null | undefined): string {
    switch (statut) {
      case 'EN_AVANCE':        return 'rdd-banner rdd-banner--success';
      case 'A_DEPOSER':        return 'rdd-banner rdd-banner--info';
      case 'A_DEPOSER_URGENT': return 'rdd-banner rdd-banner--warning';
      case 'EXPIRE':           return 'rdd-banner rdd-banner--danger';
      case 'DEPOSE':           return 'rdd-banner rdd-banner--success';
      default:                 return 'rdd-banner';
    }
  }

  bannerIcon(statut: StatutRenouvellementDelai | null | undefined): string {
    switch (statut) {
      case 'EN_AVANCE':        return 'check_circle';
      case 'A_DEPOSER':        return 'schedule';
      case 'A_DEPOSER_URGENT': return 'warning';
      case 'EXPIRE':           return 'error';
      case 'DEPOSE':           return 'check_circle';
      default:                 return 'info_outline';
    }
  }

  chipClass(statut: StatutRenouvellementDelai | null | undefined): string {
    switch (statut) {
      case 'EN_AVANCE':        return 'rdd-chip--success';
      case 'A_DEPOSER':        return 'rdd-chip--info';
      case 'A_DEPOSER_URGENT': return 'rdd-chip--warning';
      case 'EXPIRE':           return 'rdd-chip--danger';
      case 'DEPOSE':           return 'rdd-chip--success';
      default:                 return '';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const dateExpiration = RenouvellementDelaiPrefillRules.computeDateExpirationTitre(input);
    if (dateExpiration !== null && this.dateExpirationTitre() === null) {
      this.dateExpirationTitre.set(dateExpiration);
      this.provenanceDateExpiration.set('IA');
    }

    const typeTitre = RenouvellementDelaiPrefillRules.computeTypeTitre(input);
    if (typeTitre !== null && this.typeTitre() === null) {
      this.typeTitre.set(typeTitre);
      this.provenanceTypeTitre.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateExpirationTitre.set(r.dateExpirationTitre ?? null);
        this.dateDepotDossier.set(r.dateDepotDossier ?? null);
        this.typeTitre.set(r.typeTitre ?? null);
        this.provenanceDateExpiration.set(null);
        this.provenanceTypeTitre.set(null);
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
