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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterModule } from '@angular/router';

import { OfpraIntroductionService } from '../../core/services/ofpra-introduction.service';
import {
  OfpraIntroductionRequest,
  OfpraIntroductionResponse,
  StatutDelaiOfpra,
} from '../../core/models/ofpra-introduction.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { OfpraIntroductionPrefillRules } from './ofpra-introduction-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-18 — Outil décisionnel « OFPRA introduction » (F-IM-33).
 *
 * FR uniquement. Calcule la date d'échéance d'introduction de la demande
 * d'asile auprès de l'OFPRA (délai de 21 jours après remise de l'attestation
 * de demande d'asile au GUDA), liste les étapes ordonnées de la procédure
 * (stepper 5 étapes) et les pièces requises, et alerte sur le risque de
 * procédure accélérée. Pattern miroir : {@link RecepisseAttestationSectionComponent}
 * (F-IM-32).
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or
 *  - gate FRANCE + bannière BE
 *  - pré-fill IA (dateArriveeEnFrance depuis aesDateEntreeFrance)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 */
@Component({
  selector: 'app-ofpra-introduction-section',
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
    RouterModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './ofpra-introduction-section.component.html',
  styleUrl: './ofpra-introduction-section.component.scss',
})
export class OfpraIntroductionSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-33-ofpra-introduction-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'OFPRA INTRODUCTION (FR)';
  static readonly TOOL_ICON = 'assignment_turned_in';

  static getPrefillCount(input: PrefillCountInput): number {
    return OfpraIntroductionPrefillRules.computePrefillCount(input);
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
  result = signal<OfpraIntroductionResponse | null>(null);

  dateArriveeEnFrance = signal<string | null>(null);
  passageGudaEffectue = signal<boolean>(false);
  datePassageGuda = signal<string | null>(null);
  adaRequise = signal<boolean>(false);

  provenanceArrivee = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  private static readonly STATUT_LABELS: Readonly<Record<StatutDelaiOfpra, string>> = {
    A_DEPOSER: 'À déposer',
    URGENT: 'Urgent',
    EXPIRE: 'Délai expiré',
  };

  constructor(
    private readonly service: OfpraIntroductionService,
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
    const arrivee = this.dateArriveeEnFrance();
    if (!arrivee || !OfpraIntroductionPrefillRules.ISO_DATE_RE.test(arrivee)) return false;
    const guda = this.datePassageGuda();
    if (guda) {
      if (!OfpraIntroductionPrefillRules.ISO_DATE_RE.test(guda)) return false;
      if (guda < arrivee) return false;
    }
    return true;
  }

  onArriveeChange(value: string | null): void {
    this.dateArriveeEnFrance.set(value);
    this.provenanceArrivee.set(null);
  }

  onGudaPassageChange(value: boolean): void {
    this.passageGudaEffectue.set(value);
    if (!value) {
      this.datePassageGuda.set(null);
    }
  }

  onGudaDateChange(value: string | null): void {
    this.datePassageGuda.set(value);
  }

  onAdaChange(value: boolean): void {
    this.adaRequise.set(value);
  }

  statutLabel(statut: StatutDelaiOfpra | null | undefined): string {
    return statut ? OfpraIntroductionSectionComponent.STATUT_LABELS[statut] : '';
  }

  analyze(): void {
    if (!this.formValid()) return;
    const guda = this.passageGudaEffectue() ? this.datePassageGuda() : null;
    const request: OfpraIntroductionRequest = {
      dateArriveeEnFrance: this.dateArriveeEnFrance()!,
      passageGudaEffectue: this.passageGudaEffectue(),
      datePassageGuda: guda,
      adaRequise: this.adaRequise(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse OFPRA introduction enregistrée', 'OK', { duration: 2500 });
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

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const arrivee = OfpraIntroductionPrefillRules.computeDateArriveeEnFrance(input);
    if (arrivee !== null && this.dateArriveeEnFrance() === null) {
      this.dateArriveeEnFrance.set(arrivee);
      this.provenanceArrivee.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateArriveeEnFrance.set(r.dateArriveeEnFrance ?? null);
        this.passageGudaEffectue.set(r.passageGudaEffectue ?? false);
        this.datePassageGuda.set(r.datePassageGuda ?? null);
        this.adaRequise.set(r.adaRequise ?? false);
        this.provenanceArrivee.set(null);
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
