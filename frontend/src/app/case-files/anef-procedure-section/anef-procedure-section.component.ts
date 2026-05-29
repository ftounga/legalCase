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

import { AnefProcedureService } from '../../core/services/anef-procedure.service';
import {
  AnefProcedureRequest,
  AnefProcedureResponse,
  StatutAnefProcedure,
} from '../../core/models/anef-procedure.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { AnefProcedurePrefillRules } from './anef-procedure-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-214-26 — Outil décisionnel « ANEF procédure / pannes » (F-IM-37).
 *
 * FR uniquement. Guide pas-à-pas de la dématérialisation ANEF (Administration
 * Numérique des Étrangers en France) : détecte une panne ANEF signalée et le
 * risque d'expiration du titre, propose soit les étapes standard de dépôt, soit
 * les étapes alternatives de recours (dépôt papier en préfecture, référé
 * suspension) lorsqu'une panne est en cours, et donne le délai de recours pour
 * faute de l'administration. Pattern miroir : {@link OfpraIntroductionSectionComponent}.
 *
 * <p>Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or
 *  - gate FRANCE + bannière BE
 *  - pré-fill IA (dateExpirationTitre, typeTitreConcerne depuis typeTitreSejour)
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur)
 */
@Component({
  selector: 'app-anef-procedure-section',
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
  templateUrl: './anef-procedure-section.component.html',
  styleUrl: './anef-procedure-section.component.scss',
})
export class AnefProcedureSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-37-anef-procedure-fr';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'ANEF PROCÉDURE / PANNES (FR)';
  static readonly TOOL_ICON = 'devices';

  static getPrefillCount(input: PrefillCountInput): number {
    return AnefProcedurePrefillRules.computePrefillCount(input);
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
  result = signal<AnefProcedureResponse | null>(null);

  typeTitreConcerne = signal<string | null>(null);
  dateExpirationTitre = signal<string | null>(null);
  panneeANEFSignalee = signal<boolean>(false);
  dateTentativeDepot = signal<string | null>(null);
  demandeAdresseePrefecture = signal<boolean>(false);

  provenanceTypeTitre = signal<'IA' | null>(null);
  provenanceExpiration = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** PANNE_EN_COURS / RECOURS_POSSIBLE → on affiche les étapes alternatives. */
  isPanne = computed<boolean>(() => {
    const s = this.result()?.statut;
    return s === 'PANNE_EN_COURS' || s === 'RECOURS_POSSIBLE';
  });

  private static readonly STATUT_LABELS: Readonly<Record<StatutAnefProcedure, string>> = {
    NORMAL: 'Procédure normale',
    URGENT: 'Urgent',
    PANNE_EN_COURS: 'Panne en cours',
    RECOURS_POSSIBLE: 'Recours possible',
  };

  constructor(
    private readonly service: AnefProcedureService,
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
    const type = this.typeTitreConcerne();
    if (!type || type.trim().length === 0) return false;
    const expiration = this.dateExpirationTitre();
    if (!expiration || !AnefProcedurePrefillRules.ISO_DATE_RE.test(expiration)) return false;
    const tentative = this.dateTentativeDepot();
    if (tentative && !AnefProcedurePrefillRules.ISO_DATE_RE.test(tentative)) return false;
    return true;
  }

  onTypeTitreChange(value: string | null): void {
    this.typeTitreConcerne.set(value);
    this.provenanceTypeTitre.set(null);
  }

  onExpirationChange(value: string | null): void {
    this.dateExpirationTitre.set(value);
    this.provenanceExpiration.set(null);
  }

  onPanneChange(value: boolean): void {
    this.panneeANEFSignalee.set(value);
    if (!value) {
      this.dateTentativeDepot.set(null);
    }
  }

  onTentativeDepotChange(value: string | null): void {
    this.dateTentativeDepot.set(value);
  }

  onDemandePrefectureChange(value: boolean): void {
    this.demandeAdresseePrefecture.set(value);
  }

  statutLabel(statut: StatutAnefProcedure | null | undefined): string {
    return statut ? AnefProcedureSectionComponent.STATUT_LABELS[statut] : '';
  }

  analyze(): void {
    if (!this.formValid()) return;
    const tentative = this.panneeANEFSignalee() ? this.dateTentativeDepot() : null;
    const request: AnefProcedureRequest = {
      typeTitreConcerne: this.typeTitreConcerne()!.trim(),
      dateExpirationTitre: this.dateExpirationTitre()!,
      panneeANEFSignalee: this.panneeANEFSignalee(),
      dateTentativeDepot: tentative,
      demandeAdresseePrefecture: this.demandeAdresseePrefecture(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse ANEF procédure enregistrée', 'OK', { duration: 2500 });
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

    const type = AnefProcedurePrefillRules.computeTypeTitreConcerne(input);
    if (type !== null && this.typeTitreConcerne() === null) {
      this.typeTitreConcerne.set(type);
      this.provenanceTypeTitre.set('IA');
    }

    const expiration = AnefProcedurePrefillRules.computeDateExpirationTitre(input);
    if (expiration !== null && this.dateExpirationTitre() === null) {
      this.dateExpirationTitre.set(expiration);
      this.provenanceExpiration.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.typeTitreConcerne.set(r.typeTitreConcerne ?? null);
        this.dateExpirationTitre.set(r.dateExpirationTitre ?? null);
        this.panneeANEFSignalee.set(r.panneeANEFSignalee ?? false);
        this.dateTentativeDepot.set(r.dateTentativeDepot ?? null);
        this.demandeAdresseePrefecture.set(r.demandeAdresseePrefecture ?? false);
        this.provenanceTypeTitre.set(null);
        this.provenanceExpiration.set(null);
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
