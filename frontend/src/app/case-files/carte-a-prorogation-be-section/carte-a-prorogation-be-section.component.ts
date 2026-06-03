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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { CarteAProrogationBeService } from '../../core/services/carte-a-prorogation-be.service';
import {
  CarteAProrogationBeRequest,
  CarteAProrogationBeResponse,
  CarteAProrogationVerdict,
} from '../../core/models/carte-a-prorogation-be.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { CarteAProrogationBePrefillRules } from './carte-a-prorogation-be-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export type IM53_CapAlertField = 'DATE_EXPIRATION';
export type IM53_CapAlertSource = CoherenceAlertSource;
export type IM53_CapCoherenceAlert = CoherenceAlert<IM53_CapAlertField>;

/**
 * SF-221-01 — Outil décisionnel « Prorogation de la carte A (séjour temporaire BE) »
 * (F-IM-53-carte-a-prorogation-be).
 *
 * BELGIQUE uniquement — calculateur du délai de dépôt (30-45 j avant expiration)
 * et des conditions de prorogation de la carte A (séjour temporaire / limité),
 * instruite par la commune (Loi 15/12/1980 art. 13 + AR 08/10/1981 art. 33 —
 * à vérifier par avocat). Visibilité CONTEXTUAL — flag `carte_a_prorogation_detecte`.
 *
 * Pattern miroir : {@link CceAnnulationBeSectionComponent}.
 *
 * <p>Conforme F-IA-04 :
 *  - standalone OnPush, palette : vert PROROGEABLE, orange A_DEPOSER_URGENT,
 *    rouge CONDITIONS_NON_REUNIES / EXPIREE, bleu info DEMANDE_DEPOSEE
 *  - pré-fill IA RÉEL 3 champs (dateExpirationCarteA, motifSejourPersiste,
 *    conditionsInitialesToujoursReunies) ; demandeDeposee + dateDemande
 *    aspirationnels → `PREFILL_COUNT_ALWAYS_ZERO`
 *  - dashboardRefreshService.triggerRefresh() post-POST succès
 *  - static getPrefillCount miroir du runtime prefillFromAi()
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 */
@Component({
  selector: 'app-carte-a-prorogation-be-section',
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
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './carte-a-prorogation-be-section.component.html',
  styleUrl: './carte-a-prorogation-be-section.component.scss',
})
export class CarteAProrogationBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-IM-53-carte-a-prorogation-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'PROROGATION CARTE A (BE)';
  static readonly TOOL_ICON = 'event_repeat';

  static getPrefillCount(input: PrefillCountInput): number {
    return CarteAProrogationBePrefillRules.computePrefillCount(input);
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
  result = signal<CarteAProrogationBeResponse | null>(null);

  dateExpirationCarteA = signal<string | null>(null);
  motifSejourPersiste = signal<boolean>(false);
  conditionsInitialesToujoursReunies = signal<boolean>(false);
  demandeDeposee = signal<boolean>(false);
  dateDemande = signal<string | null>(null);

  provenanceDateExpiration = signal<'IA' | null>(null);
  provenanceMotifPersiste = signal<'IA' | null>(null);
  provenanceConditionsReunies = signal<'IA' | null>(null);

  isBelgique = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /** Alertes de cohérence F-IA-03 — sur la date d'expiration uniquement. */
  coherenceAlerts = computed<Partial<Record<IM53_CapAlertField, IM53_CapCoherenceAlert>>>(() => {
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IM53_CapAlertField, IM53_CapCoherenceAlert>> = {};
    const dateAlert = this.buildDateExpirationAlert();
    if (dateAlert) alerts.DATE_EXPIRATION = dateAlert;
    return alerts;
  });

  constructor(
    private readonly service: CarteAProrogationBeService,
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
    const exp = this.dateExpirationCarteA();
    if (!exp || !ISO_DATE_RE.test(exp)) return false;
    if (this.demandeDeposee()) {
      const dd = this.dateDemande();
      if (!dd || !ISO_DATE_RE.test(dd)) return false;
    }
    return true;
  }

  onDateExpirationChange(value: string | null): void {
    this.dateExpirationCarteA.set(value || null);
    this.provenanceDateExpiration.set(null);
  }

  onMotifPersisteChange(value: boolean): void {
    this.motifSejourPersiste.set(value);
    this.provenanceMotifPersiste.set(null);
  }

  onConditionsReuniesChange(value: boolean): void {
    this.conditionsInitialesToujoursReunies.set(value);
    this.provenanceConditionsReunies.set(null);
  }

  onDemandeDeposeeChange(value: boolean): void {
    this.demandeDeposee.set(value);
    if (!value) {
      this.dateDemande.set(null);
    }
  }

  onDateDemandeChange(value: string | null): void {
    this.dateDemande.set(value || null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: CarteAProrogationBeRequest = {
      dateExpirationCarteA: this.dateExpirationCarteA()!,
      motifSejourPersiste: this.motifSejourPersiste(),
      conditionsInitialesToujoursReunies: this.conditionsInitialesToujoursReunies(),
      demandeDeposee: this.demandeDeposee(),
      dateDemande: this.demandeDeposee() ? this.dateDemande() : null,
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse prorogation carte A enregistrée', 'OK', { duration: 2500 });
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

  verdictBannerClass(verdict: CarteAProrogationVerdict | null | undefined): string {
    switch (verdict) {
      case 'PROROGEABLE':            return 'cap-banner cap-banner--success';
      case 'A_DEPOSER_URGENT':       return 'cap-banner cap-banner--warning';
      case 'CONDITIONS_NON_REUNIES': return 'cap-banner cap-banner--danger';
      case 'EXPIREE':                return 'cap-banner cap-banner--danger';
      case 'DEMANDE_DEPOSEE':        return 'cap-banner cap-banner--info';
      default:                       return 'cap-banner';
    }
  }

  verdictChipClass(verdict: CarteAProrogationVerdict | null | undefined): string {
    switch (verdict) {
      case 'PROROGEABLE':            return 'cap-chip cap-chip--success';
      case 'A_DEPOSER_URGENT':       return 'cap-chip cap-chip--warning';
      case 'CONDITIONS_NON_REUNIES': return 'cap-chip cap-chip--danger';
      case 'EXPIREE':                return 'cap-chip cap-chip--danger';
      case 'DEMANDE_DEPOSEE':        return 'cap-chip cap-chip--info';
      default:                       return 'cap-chip';
    }
  }

  verdictIcon(verdict: CarteAProrogationVerdict | null | undefined): string {
    switch (verdict) {
      case 'PROROGEABLE':            return 'event_available';
      case 'A_DEPOSER_URGENT':       return 'schedule';
      case 'CONDITIONS_NON_REUNIES': return 'block';
      case 'EXPIREE':                return 'event_busy';
      case 'DEMANDE_DEPOSEE':        return 'task_alt';
      default:                       return 'info_outline';
    }
  }

  verdictLabel(verdict: CarteAProrogationVerdict | null | undefined): string {
    switch (verdict) {
      case 'PROROGEABLE':            return 'Prorogeable';
      case 'A_DEPOSER_URGENT':       return 'À déposer en urgence';
      case 'CONDITIONS_NON_REUNIES': return 'Conditions non réunies';
      case 'EXPIREE':                return 'Carte expirée';
      case 'DEMANDE_DEPOSEE':        return 'Demande déposée';
      default:                       return '';
    }
  }

  /** Format JJ/MM/YYYY depuis une date ISO yyyy-MM-dd. */
  formatDateFr(iso: string | null | undefined): string {
    if (!iso || !ISO_DATE_RE.test(iso)) return '—';
    const [y, m, d] = iso.split('-');
    return `${d}/${m}/${y}`;
  }

  /** Classe CSS des jours restants — rouge si négatif (carte expirée). */
  joursClass(jours: number | null | undefined): string {
    if (jours === null || jours === undefined) return 'cap-jours';
    return jours < 0 ? 'cap-jours cap-jours--negative' : 'cap-jours';
  }

  alertBadgeLabel(alert: IM53_CapCoherenceAlert): string {
    if (alert.severity === 'CRITICAL') return `Alerte critique (${alert.expectedDisplay})`;
    return `Incohérence détectée (${alert.expectedDisplay})`;
  }

  private buildDateExpirationAlert(): IM53_CapCoherenceAlert | null {
    const userDate = this.dateExpirationCarteA();
    if (!userDate) return null;
    const ai = this.aiData;
    const aiDate = ai?.carteAProrogationDateExpiration;
    const builder = CoherenceAlertBuilder.forField<IM53_CapAlertField>('DATE_EXPIRATION')
      .withSeverity('WARNING');
    if (
      typeof aiDate === 'string' &&
      ISO_DATE_RE.test(aiDate) &&
      aiDate !== userDate
    ) {
      builder.addSource('IA', {
        expectedDisplay: this.formatDateFr(aiDate),
        reason: `Analyse du dossier : expiration de la carte A le ${this.formatDateFr(aiDate)}`,
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

    const dateExp = CarteAProrogationBePrefillRules.computeDateExpiration(input);
    if (dateExp !== null && !this.dateExpirationCarteA()) {
      this.dateExpirationCarteA.set(dateExp);
      this.provenanceDateExpiration.set('IA');
    }

    const motif = CarteAProrogationBePrefillRules.computeMotifPersiste(input);
    if (motif !== null) {
      this.motifSejourPersiste.set(motif);
      this.provenanceMotifPersiste.set('IA');
    }

    const conditions = CarteAProrogationBePrefillRules.computeConditionsReunies(input);
    if (conditions !== null) {
      this.conditionsInitialesToujoursReunies.set(conditions);
      this.provenanceConditionsReunies.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateExpirationCarteA.set(r.dateExpirationCarteA ?? null);
        this.motifSejourPersiste.set(r.motifSejourPersiste ?? false);
        this.conditionsInitialesToujoursReunies.set(r.conditionsInitialesToujoursReunies ?? false);
        this.demandeDeposee.set(r.demandeDeposee ?? false);
        this.dateDemande.set(r.dateDemande ?? null);
        this.provenanceDateExpiration.set(null);
        this.provenanceMotifPersiste.set(null);
        this.provenanceConditionsReunies.set(null);
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
