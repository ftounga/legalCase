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

import { AppelCaaCassationService } from '../../core/services/appel-caa-cassation.service';
import {
  AppelCaaCassationRequest,
  AppelCaaCassationResponse,
  StatutAppelCaaCassation,
  TypeContentieuxAppel,
  TypeDecisionTaAppel,
} from '../../core/models/appel-caa-cassation.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { AppelCaaCassationPrefillRules } from './appel-caa-cassation-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-34 — Outil décisionnel « Appel CAA / cassation CE » (F-IM-41).
 *
 * FRANCE uniquement — délais d'appel devant la cour administrative d'appel (CAA)
 * contre un jugement de TA en contentieux des étrangers, et pourvoi en cassation
 * devant le Conseil d'État. Le délai d'appel de droit commun est d'un mois ; un
 * délai spécial de 15 jours s'applique en matière d'OQTF (CESEDA / CJA).
 * Calculateur de délai (palette rouge PRESCRIT / orange URGENT / vert
 * APPEL_POSSIBLE).
 * Pattern miroir : {@link NaturalisationRecoursTaSectionComponent} (F-IM-40).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; vert APPEL_POSSIBLE, orange URGENT,
 *    rouge PRESCRIT
 *  - pré-fill IA (date du jugement du TA)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 *
 * <p>Bridge échéance F-69 : si statut APPEL_POSSIBLE ou URGENT, une échéance
 * `dateEcheanceAppelCaa` est créée via {@link CaseDeadlineService} avec le label
 * « Appel CAA contentieux étrangers » (best-effort, n'interrompt pas le flux).
 *
 * <p>Filtre des pourvois en cassation : en matière d'OQTF, le pourvoi en
 * cassation devant le Conseil d'État est soumis à la procédure préalable
 * d'admission (L.821-2 CJA) — une bannière d'information le signale.
 */
@Component({
  selector: 'app-appel-caa-cassation-section',
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
  templateUrl: './appel-caa-cassation-section.component.html',
  styleUrl: './appel-caa-cassation-section.component.scss',
})
export class AppelCaaCassationSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-41-appel-caa-cassation-ce-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'APPEL CAA / CASSATION CE (FR)';
  static readonly TOOL_ICON = 'gavel';

  static getPrefillCount(input: PrefillCountInput): number {
    return AppelCaaCassationPrefillRules.computePrefillCount(input);
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
  result = signal<AppelCaaCassationResponse | null>(null);
  deadlineCreated = signal(false);

  dateJugementTA = signal<string | null>(null);
  typeDecisionTA = signal<TypeDecisionTaAppel>('REJET');
  typeContentieux = signal<TypeContentieuxAppel>('OQTF');
  delaiSpecialOQTF = signal<boolean>(true);

  provenanceDateJugement = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private readonly service: AppelCaaCassationService,
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
    if (!this.dateJugementTA()) return false;
    return true;
  }

  onDateJugementChange(value: string | null): void {
    this.dateJugementTA.set(value || null);
    this.provenanceDateJugement.set(null);
  }

  onTypeDecisionChange(value: TypeDecisionTaAppel): void {
    this.typeDecisionTA.set(value);
  }

  onTypeContentieuxChange(value: TypeContentieuxAppel): void {
    this.typeContentieux.set(value);
    // L'OQTF appelle par défaut le délai spécial de 15 j ; on laisse l'avocat
    // décocher si une situation particulière l'exige (pas d'override forcé).
    if (value === 'OQTF') {
      this.delaiSpecialOQTF.set(true);
    } else {
      this.delaiSpecialOQTF.set(false);
    }
  }

  onDelaiSpecialChange(value: boolean): void {
    this.delaiSpecialOQTF.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: AppelCaaCassationRequest = {
      dateJugementTA: this.dateJugementTA()!,
      typeDecisionTA: this.typeDecisionTA(),
      typeContentieux: this.typeContentieux(),
      delaiSpecialOQTF: this.delaiSpecialOQTF(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse appel CAA / cassation CE enregistrée', 'OK', { duration: 2500 });
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
   * Bridge échéance F-69 — si le statut est APPEL_POSSIBLE ou URGENT, on
   * alimente `app-case-deadlines-section` avec l'échéance `dateEcheanceAppelCaa`.
   * Best-effort : un échec n'interrompt pas le flux (le dashboard refresh
   * affichera l'analyse quoi qu'il arrive).
   */
  private maybeCreateDeadline(r: AppelCaaCassationResponse): void {
    if (!this.deadlineService || !this.caseFileId) return;
    if (r.statut !== 'APPEL_POSSIBLE' && r.statut !== 'URGENT') return;
    if (!r.dateEcheanceAppelCaa) return;
    this.deadlineService.create(this.caseFileId, 'Appel CAA contentieux étrangers', r.dateEcheanceAppelCaa).subscribe({
      next: () => {
        this.deadlineCreated.set(true);
        this.cdr.markForCheck();
      },
      error: () => {
        // Best-effort : on ne bloque pas l'utilisateur sur l'échéance.
      },
    });
  }

  statutLabel(statut: StatutAppelCaaCassation | null | undefined): string {
    switch (statut) {
      case 'APPEL_POSSIBLE': return 'Appel CAA possible';
      case 'URGENT':         return 'Appel CAA urgent';
      case 'PRESCRIT':       return "Délai d'appel prescrit";
      default:               return '';
    }
  }

  typeDecisionLabel(type: TypeDecisionTaAppel | null | undefined): string {
    switch (type) {
      case 'REJET':      return 'Rejet de la requête';
      case 'ANNULATION': return 'Annulation';
      default:           return '';
    }
  }

  typeContentieuxLabel(type: TypeContentieuxAppel | null | undefined): string {
    switch (type) {
      case 'OQTF':        return 'OQTF';
      case 'REFUS_TITRE': return 'Refus de titre de séjour';
      case 'EXPULSION':   return 'Expulsion';
      case 'AUTRE':       return 'Autre contentieux';
      default:            return '';
    }
  }

  bannerClass(statut: StatutAppelCaaCassation | null | undefined): string {
    switch (statut) {
      case 'APPEL_POSSIBLE': return 'acc-banner acc-banner--success';
      case 'URGENT':         return 'acc-banner acc-banner--warning';
      case 'PRESCRIT':       return 'acc-banner acc-banner--danger';
      default:               return 'acc-banner';
    }
  }

  bannerIcon(statut: StatutAppelCaaCassation | null | undefined): string {
    switch (statut) {
      case 'APPEL_POSSIBLE': return 'check_circle';
      case 'URGENT':         return 'warning';
      case 'PRESCRIT':       return 'error';
      default:               return 'info_outline';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const dateJugement = AppelCaaCassationPrefillRules.computeDateJugementTA(input);
    if (dateJugement !== null && this.dateJugementTA() === null) {
      this.dateJugementTA.set(dateJugement);
      this.provenanceDateJugement.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateJugementTA.set(r.dateJugementTA ?? null);
        this.typeDecisionTA.set(r.typeDecisionTA ?? 'REJET');
        this.typeContentieux.set(r.typeContentieux ?? 'OQTF');
        this.delaiSpecialOQTF.set(r.delaiSpecialOQTF ?? false);
        this.provenanceDateJugement.set(null);
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
