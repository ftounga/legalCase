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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';

import { Naturalisation12bisBeService } from '../../core/services/naturalisation-12bis-be.service';
import {
  DUREE_SEJOUR_VOIE_10_ANS_MOIS,
  DUREE_SEJOUR_VOIE_5_ANS_MOIS,
  NATURALISATION_12BIS_NIVEAUX_LANGUE,
  NATURALISATION_12BIS_TYPES_SEJOUR,
  Naturalisation12bisBeRequest,
  Naturalisation12bisBeResponse,
  Naturalisation12bisNiveauLangue,
  Naturalisation12bisNiveauLangueOption,
  Naturalisation12bisTypeSejour,
  Naturalisation12bisTypeSejourOption,
  Naturalisation12bisVoie,
} from '../../core/models/naturalisation-12bis-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { Naturalisation12bisBePrefillRules } from './naturalisation-12bis-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

export type IM28_12bisAlertField = 'DUREE_SEJOUR';

export type IM28_12bisAlertSource = CoherenceAlertSource;
export type IM28_12bisCoherenceAlert = CoherenceAlert<IM28_12bisAlertField>;

/**
 * SF-215-08 — Outil décisionnel « Naturalisation art. 12bis (BE) »
 * (F-IM-28-naturalisation-12bis-be).
 *
 * BELGIQUE uniquement — scoring d'éligibilité pour la déclaration de
 * nationalité belge sous l'art. 12bis du Code de la nationalité belge
 * (Loi du 28/06/1984, mod. Loi du 04/12/2012). Deux voies (5 ans et
 * 10 ans), verdict `VOIE_5_ANS` / `VOIE_10_ANS` / `AUCUNE` + durée
 * manquante en mois si aucune voie n'est encore atteinte.
 *
 * Visibilité CONTEXTUAL — flag `naturalisation_be_envisagee`.
 *
 * Pattern miroir : {@link Regroupement10bisBeSectionComponent} (SF-215-06)
 * — 3 pré-fills RÉELS (dureeSejour, typeSejour, niveauLangue) + 4
 * checkboxes ASPIRATIONNELLES (preuveIntegration, preuveEmploi,
 * menaceOrdrePublic, condamnationPenale) que l'IA NE pré-remplit PAS
 * (`PREFILL_COUNT_ALWAYS_ZERO`).
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette : vert VOIE_5_ANS/VOIE_10_ANS, rouge AUCUNE
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi() — max 3
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck
 */
@Component({
  selector: 'app-naturalisation-12bis-be-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatRadioModule,
    MatSelectModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './naturalisation-12bis-be-section.component.html',
  styleUrl: './naturalisation-12bis-be-section.component.scss',
})
export class Naturalisation12bisBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-28-naturalisation-12bis-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'NATURALISATION 12BIS (BE)';
  static readonly TOOL_ICON = 'flag';

  static getPrefillCount(input: PrefillCountInput): number {
    return Naturalisation12bisBePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<Naturalisation12bisBeResponse | null>(null);

  dureeSejour = signal<number | null>(null);
  typeSejour = signal<Naturalisation12bisTypeSejour | null>(null);
  niveauLangue = signal<Naturalisation12bisNiveauLangue | null>(null);
  preuveIntegration = signal<boolean>(false);
  preuveEmploi = signal<boolean>(false);
  menaceOrdrePublic = signal<boolean>(false);
  condamnationPenale = signal<boolean>(false);

  // Provenance — 3 réels (les 4 checkboxes restent saisie avocat —
  // `PREFILL_COUNT_ALWAYS_ZERO`).
  provenanceDureeSejour = signal<'IA' | null>(null);
  provenanceTypeSejour = signal<'IA' | null>(null);
  provenanceNiveauLangue = signal<'IA' | null>(null);

  readonly typesSejour: ReadonlyArray<Naturalisation12bisTypeSejourOption> =
    NATURALISATION_12BIS_TYPES_SEJOUR;
  readonly niveauxLangue: ReadonlyArray<Naturalisation12bisNiveauLangueOption> =
    NATURALISATION_12BIS_NIVEAUX_LANGUE;

  readonly dureeVoie5Ans = DUREE_SEJOUR_VOIE_5_ANS_MOIS;
  readonly dureeVoie10Ans = DUREE_SEJOUR_VOIE_10_ANS_MOIS;

  isBelgique = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * Alertes de cohérence F-IA-03 — uniquement sur `dureeSejour` (entier
   * numérique). Pas d'alerte sur `typeSejour` / `niveauLangue` qui sont
   * des enums whitelistés (un seul match possible).
   */
  coherenceAlerts = computed<Partial<Record<IM28_12bisAlertField, IM28_12bisCoherenceAlert>>>(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IM28_12bisAlertField, IM28_12bisCoherenceAlert>> = {};
    const duree = this.buildDureeSejourAlert();
    if (duree) alerts.DUREE_SEJOUR = duree;
    return alerts;
  });

  constructor(
    private readonly service: Naturalisation12bisBeService,
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
    if (this.isBelgique() && this.caseFileId) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) {
      this.collapsed.set(false);
    }
    if (changes['aiData'] && this.isBelgique() && this.showForm() && !this.result()) {
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
    const duree = this.dureeSejour();
    if (duree === null || duree === undefined || isNaN(duree) || duree < 0 || duree > 600) return false;
    if (!this.typeSejour()) return false;
    if (!this.niveauLangue()) return false;
    return true;
  }

  onDureeSejourChange(value: number | null | undefined): void {
    this.dureeSejour.set(value ?? null);
    this.provenanceDureeSejour.set(null);
  }

  onTypeSejourChange(value: Naturalisation12bisTypeSejour | null): void {
    this.typeSejour.set(value);
    this.provenanceTypeSejour.set(null);
  }

  onNiveauLangueChange(value: Naturalisation12bisNiveauLangue | null): void {
    this.niveauLangue.set(value);
    this.provenanceNiveauLangue.set(null);
  }

  onPreuveIntegrationChange(value: boolean): void {
    this.preuveIntegration.set(value);
  }

  onPreuveEmploiChange(value: boolean): void {
    this.preuveEmploi.set(value);
  }

  onMenaceOrdrePublicChange(value: boolean): void {
    this.menaceOrdrePublic.set(value);
  }

  onCondamnationPenaleChange(value: boolean): void {
    this.condamnationPenale.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: Naturalisation12bisBeRequest = {
      dureeSejour: this.dureeSejour()!,
      typeSejour: this.typeSejour()!,
      niveauLangue: this.niveauLangue()!,
      preuveIntegration: this.preuveIntegration(),
      preuveEmploi: this.preuveEmploi(),
      menaceOrdrePublic: this.menaceOrdrePublic(),
      condamnationPenale: this.condamnationPenale(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse naturalisation 12bis BE enregistrée', 'OK', { duration: 2500 });
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

  voieBannerClass(voie: Naturalisation12bisVoie | null | undefined): string {
    switch (voie) {
      case 'VOIE_5_ANS':  return 'nat12bis-banner nat12bis-banner--success';
      case 'VOIE_10_ANS': return 'nat12bis-banner nat12bis-banner--success';
      case 'AUCUNE':      return 'nat12bis-banner nat12bis-banner--danger';
      default:            return 'nat12bis-banner';
    }
  }

  voieChipClass(voie: Naturalisation12bisVoie | null | undefined): string {
    switch (voie) {
      case 'VOIE_5_ANS':  return 'nat12bis-chip nat12bis-chip--success';
      case 'VOIE_10_ANS': return 'nat12bis-chip nat12bis-chip--success';
      case 'AUCUNE':      return 'nat12bis-chip nat12bis-chip--danger';
      default:            return 'nat12bis-chip';
    }
  }

  voieIcon(voie: Naturalisation12bisVoie | null | undefined): string {
    switch (voie) {
      case 'VOIE_5_ANS':  return 'check_circle';
      case 'VOIE_10_ANS': return 'check_circle';
      case 'AUCUNE':      return 'block';
      default:            return 'info_outline';
    }
  }

  voieLabel(voie: Naturalisation12bisVoie | null | undefined): string {
    switch (voie) {
      case 'VOIE_5_ANS':  return 'Voie 5 ans';
      case 'VOIE_10_ANS': return 'Voie 10 ans';
      case 'AUCUNE':      return 'Aucune voie éligible';
      default:            return '';
    }
  }

  typeSejourLabel(code: Naturalisation12bisTypeSejour | string | null | undefined): string {
    if (!code) return '';
    return this.typesSejour.find((o) => o.code === code)?.label ?? String(code);
  }

  niveauLangueLabel(code: Naturalisation12bisNiveauLangue | string | null | undefined): string {
    if (!code) return '';
    return this.niveauxLangue.find((o) => o.code === code)?.label ?? String(code);
  }

  alertTooltip(alert: IM28_12bisCoherenceAlert): string {
    return alert.reason;
  }

  alertBadgeLabel(alert: IM28_12bisCoherenceAlert): string {
    if (alert.severity === 'CRITICAL') return `Alerte critique (${alert.expectedDisplay})`;
    return `Incohérence détectée (${alert.expectedDisplay})`;
  }

  private buildDureeSejourAlert(): IM28_12bisCoherenceAlert | null {
    const userDuree = this.dureeSejour();
    if (userDuree === null || userDuree === undefined) return null;
    const builder = CoherenceAlertBuilder.forField<IM28_12bisAlertField>('DUREE_SEJOUR')
      .withSeverity('WARNING');
    const aiDuree = this.aiData?.naturalisationBeDureeSejour;
    if (
      typeof aiDuree === 'number' &&
      !isNaN(aiDuree) &&
      aiDuree >= 0 &&
      aiDuree !== userDuree
    ) {
      builder.addSource('IA', {
        expectedDisplay: `${aiDuree} mois`,
        reason: `Analyse du dossier : durée du séjour légal = ${aiDuree} mois`,
      });
    }
    return builder.build();
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const duree = Naturalisation12bisBePrefillRules.computeDureeSejour(input);
    if (duree !== null && this.dureeSejour() === null) {
      this.dureeSejour.set(duree);
      this.provenanceDureeSejour.set('IA');
    }

    const type = Naturalisation12bisBePrefillRules.computeTypeSejour(input);
    if (type !== null && !this.typeSejour()) {
      this.typeSejour.set(type);
      this.provenanceTypeSejour.set('IA');
    }

    const niveau = Naturalisation12bisBePrefillRules.computeNiveauLangue(input);
    if (niveau !== null && !this.niveauLangue()) {
      this.niveauLangue.set(niveau);
      this.provenanceNiveauLangue.set('IA');
    }
    // Les 4 checkboxes (preuveIntegration, preuveEmploi, menaceOrdrePublic,
    // condamnationPenale) ne sont JAMAIS pré-remplies (PREFILL_COUNT_ALWAYS_ZERO).
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dureeSejour.set(r.dureeSejour ?? null);
        this.typeSejour.set((r.typeSejour as Naturalisation12bisTypeSejour) ?? null);
        this.niveauLangue.set((r.niveauLangue as Naturalisation12bisNiveauLangue) ?? null);
        this.preuveIntegration.set(r.preuveIntegration ?? false);
        this.preuveEmploi.set(r.preuveEmploi ?? false);
        this.menaceOrdrePublic.set(r.menaceOrdrePublic ?? false);
        this.condamnationPenale.set(r.condamnationPenale ?? false);
        this.provenanceDureeSejour.set(null);
        this.provenanceTypeSejour.set(null);
        this.provenanceNiveauLangue.set(null);
        this.showForm.set(false);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — on tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
