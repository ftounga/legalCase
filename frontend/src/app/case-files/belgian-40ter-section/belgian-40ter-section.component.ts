import {
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  signal,
  computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Belgian40terService } from '../../core/services/belgian-40ter.service';
import {
  Belgian40terRequest,
  Belgian40terResponse,
  LIENS_FAMILIAUX,
  LienFamilial,
  LienFamilialOption,
  SEUIL_120_PCT_RIS_DEFAULT,
  VerdictProbabilite,
} from '../../core/models/belgian-40ter.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/** Whitelist stricte alignée sur Belgian40terCalculator.isLienValide (5 codes). */
const LIENS_FAMILIAUX_WHITELIST = new Set<LienFamilial>(
  LIENS_FAMILIAUX.map((l) => l.code),
);

/** Champs éligibles à la validation F-IA-03. */
export type IM14_40terAlertField =
  | 'LIEN_FAMILIAL'
  | 'REVENUS_MENSUELS'
  | 'DATE_DEPOT';

export type IM14_40terAlertSource = CoherenceAlertSource;
export type IM14_40terCoherenceAlert = CoherenceAlert<IM14_40terAlertField>;

/**
 * SF-IM-14-08 : outil décisionnel "Regroupement familial d'un Belge"
 * (art. 40ter Loi 15/12/1980 — F-IM-14). BE uniquement.
 * Consomme l'API SF-IM-14-04 (backend mergé PR #511).
 *
 * Affiché conditionnellement par le panel F-IA-04 (TOOL_REGISTRY
 * `F-IM-14-40ter-familial-belge-be`, ALWAYS_ON, BELGIQUE, priority 67).
 *
 * Cohérence palette (DESIGN_SYSTEM.md / SF-155-04) : navy/or pour
 * verdicts ELEVEE/MOYENNE, rouge réservé au verdict FAIBLE (alerte
 * critique). `<input type="date">` natif. MatSnackBar pour erreurs.
 * `triggerRefresh()` après POST succès. JetBrains Mono pour
 * `baseJuridique` et `formule`.
 */
@Component({
  selector: 'app-belgian-40ter-section',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatChipsModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './belgian-40ter-section.component.html',
  styleUrl: './belgian-40ter-section.component.scss',
})
export class Belgian40terSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = '40TER FAMILIAL BELGE (BE)';
  static readonly TOOL_ICON = 'family_restroom';

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<Belgian40terResponse | null>(null);

  lienFamilial = signal<LienFamilial | null>(null);
  regroupantBelge = signal<boolean>(true);
  revenusMensuelsNetsEur = signal<number | null>(null);
  seuil120PctRisEur = signal<number>(SEUIL_120_PCT_RIS_DEFAULT);
  assuranceMaladie = signal<boolean>(false);
  logementSuffisant = signal<boolean>(false);
  menaceOrdrePublic = signal<boolean>(false);
  dateDepotDemande = signal<string | null>(null);

  // Provenance IA par champ (badge "Pré-rempli depuis l'analyse").
  // Effacée dès que l'avocat modifie manuellement le champ (onXxxChange).
  provenanceLienFamilial = signal<'IA' | null>(null);
  provenanceRegroupantBelge = signal<'IA' | null>(null);
  provenanceRevenusMensuels = signal<'IA' | null>(null);
  provenanceDateDepot = signal<'IA' | null>(null);

  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);

  readonly liensFamiliaux: LienFamilialOption[] = LIENS_FAMILIAUX;
  /** Aujourd'hui en YYYY-MM-DD pour l'attribut max des inputs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isBelgium = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * Alertes de cohérence F-IA-03 exposées par field.
   * Pattern `CoherenceAlertBuilder` aligné sur annexe13-be / immigration-title-decision.
   */
  coherenceAlerts = computed<Partial<Record<IM14_40terAlertField, IM14_40terCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IM14_40terAlertField, IM14_40terCoherenceAlert>> = {};

    const lien = this.buildLienFamilialAlert();
    if (lien) alerts.LIEN_FAMILIAL = lien;
    const revenus = this.buildRevenusMensuelsAlert();
    if (revenus) alerts.REVENUS_MENSUELS = revenus;
    const dateDepot = this.buildDateDepotAlert();
    if (dateDepot) alerts.DATE_DEPOT = dateDepot;

    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: Belgian40terService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (this.isBelgium()) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    // Re-applique le prefill si aiData change, seulement si l'on est encore
    // en mode formulaire ET qu'aucun résultat persisté n'a été chargé
    // (évite d'écraser la saisie avocat ou un résultat backend existant).
    if (
      changes['aiData'] &&
      !changes['aiData'].firstChange &&
      this.isBelgium() &&
      this.showForm() &&
      !this.result()
    ) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    if (!this.lienFamilial()) return false;
    const revenus = this.revenusMensuelsNetsEur();
    if (revenus === null || revenus === undefined || isNaN(revenus) || revenus <= 0) return false;
    const seuil = this.seuil120PctRisEur();
    if (seuil === null || seuil === undefined || isNaN(seuil) || seuil <= 0) return false;
    const dateDepot = this.dateDepotDemande();
    if (dateDepot && dateDepot > this.todayIso) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const dateDepot = this.dateDepotDemande();
    const request: Belgian40terRequest = {
      lienFamilial: this.lienFamilial()!,
      regroupantBelge: this.regroupantBelge(),
      revenusMensuelsNetsEur: this.revenusMensuelsNetsEur()!,
      seuil120PctRisEur: this.seuil120PctRisEur(),
      assuranceMaladie: this.assuranceMaladie(),
      logementSuffisant: this.logementSuffisant(),
      menaceOrdrePublic: this.menaceOrdrePublic(),
      ...(dateDepot ? { dateDepotDemande: dateDepot } : {}),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('40ter familial Belge analysé', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', {
          duration: 5000,
          panelClass: 'snack-error',
        });
      },
    });
  }

  // Handlers onChange — effacent la provenance IA au 1er changement manuel.
  onLienFamilialChange(value: LienFamilial | null): void {
    this.lienFamilial.set(value);
    this.provenanceLienFamilial.set(null);
  }

  onRegroupantBelgeChange(value: boolean): void {
    this.regroupantBelge.set(value);
    this.provenanceRegroupantBelge.set(null);
  }

  onRevenusMensuelsChange(value: number | null | undefined): void {
    this.revenusMensuelsNetsEur.set(value ?? null);
    this.provenanceRevenusMensuels.set(null);
  }

  onSeuilChange(value: number | null | undefined): void {
    this.seuil120PctRisEur.set(value ?? SEUIL_120_PCT_RIS_DEFAULT);
  }

  onDateDepotChange(value: string | null): void {
    this.dateDepotDemande.set(value || null);
    this.provenanceDateDepot.set(null);
  }

  /**
   * Pré-fill IA depuis aiData. Règles fail-open (graceful) :
   * - passe silencieusement si aiData absent
   * - chaque champ est indépendant (prefill partiel OK)
   * - lienFamilial hors-whitelist → skip
   * - revenus négatifs / non numériques → skip
   * - date dans le futur → skip
   * - aucun champ revenusNetsMensuels n'existe encore dans
   *   ImmigrationExtractedData : lookup générique via cast (évite
   *   d'ajouter une dépendance backend pour cette SF).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    const aiAny = ai as Record<string, unknown>;

    const aiLien = aiAny['lienFamilialBe'] ?? aiAny['lienFamilial'];
    if (typeof aiLien === 'string' && LIENS_FAMILIAUX_WHITELIST.has(aiLien as LienFamilial)) {
      this.lienFamilial.set(aiLien as LienFamilial);
      this.provenanceLienFamilial.set('IA');
    }

    const aiRegroupant = aiAny['regroupantBelge'];
    if (typeof aiRegroupant === 'boolean') {
      this.regroupantBelge.set(aiRegroupant);
      this.provenanceRegroupantBelge.set('IA');
    }

    const aiRevenus = aiAny['revenusNetsMensuels'] ?? aiAny['revenusMensuelsNets'];
    if (typeof aiRevenus === 'number' && !isNaN(aiRevenus) && aiRevenus > 0) {
      this.revenusMensuelsNetsEur.set(aiRevenus);
      this.provenanceRevenusMensuels.set('IA');
    }

    const aiDate = aiAny['dateDepotDemande'] ?? ai.dateDepotProcedure;
    if (typeof aiDate === 'string' && aiDate.length > 0 && aiDate <= this.todayIso) {
      this.dateDepotDemande.set(aiDate);
      this.provenanceDateDepot.set('IA');
    }
  }

  private buildLienFamilialAlert(): IM14_40terCoherenceAlert | null {
    const userLien = this.lienFamilial();
    if (!userLien) return null;
    const builder = CoherenceAlertBuilder.forField<IM14_40terAlertField>('LIEN_FAMILIAL')
      .withSeverity('WARNING');
    const ai = this.aiDataSignal();
    const aiAny = (ai ?? {}) as Record<string, unknown>;
    const aiLienRaw = aiAny['lienFamilialBe'] ?? aiAny['lienFamilial'];
    if (
      typeof aiLienRaw === 'string' &&
      LIENS_FAMILIAUX_WHITELIST.has(aiLienRaw as LienFamilial) &&
      aiLienRaw !== userLien
    ) {
      const label = LIENS_FAMILIAUX.find((l) => l.code === aiLienRaw)?.label ?? aiLienRaw;
      builder.addSource('IA', {
        expectedDisplay: label,
        reason: `Analyse du dossier : lien familial ${label}`,
      });
    }
    return builder.build();
  }

  private buildRevenusMensuelsAlert(): IM14_40terCoherenceAlert | null {
    const userRevenus = this.revenusMensuelsNetsEur();
    if (userRevenus === null || userRevenus === undefined) return null;
    const builder = CoherenceAlertBuilder.forField<IM14_40terAlertField>('REVENUS_MENSUELS')
      .withSeverity('WARNING');
    const ai = this.aiDataSignal();
    const aiAny = (ai ?? {}) as Record<string, unknown>;
    const aiRevenus = aiAny['revenusNetsMensuels'] ?? aiAny['revenusMensuelsNets'];
    if (
      typeof aiRevenus === 'number' &&
      !isNaN(aiRevenus) &&
      aiRevenus > 0 &&
      aiRevenus !== userRevenus
    ) {
      builder.addSource('IA', {
        expectedDisplay: `${aiRevenus} €/mois`,
        reason: `Analyse du dossier : revenus nets mensuels = ${aiRevenus} €`,
      });
    }
    return builder.build();
  }

  private buildDateDepotAlert(): IM14_40terCoherenceAlert | null {
    const userDate = this.dateDepotDemande();
    if (!userDate) return null;
    const builder = CoherenceAlertBuilder.forField<IM14_40terAlertField>('DATE_DEPOT')
      .withSeverity('WARNING');
    const ai = this.aiDataSignal();
    const aiAny = (ai ?? {}) as Record<string, unknown>;
    const aiDateRaw = aiAny['dateDepotDemande'] ?? ai?.dateDepotProcedure;
    if (typeof aiDateRaw === 'string' && aiDateRaw.length > 0 && aiDateRaw !== userDate) {
      builder.addSource('IA', {
        expectedDisplay: aiDateRaw,
        reason: `Analyse du dossier : date de dépôt ${aiDateRaw}`,
      });
    }
    return builder.build();
  }

  /**
   * Classe CSS du bandeau de résultat selon le verdict calculé.
   * Rouge réservé au verdict FAIBLE (alerte critique — DESIGN_SYSTEM.md
   * autorise la couleur rouge pour les statuts d'urgence uniquement).
   */
  bannerClass(verdict: VerdictProbabilite | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE': return 'belgian40ter-banner belgian40ter-banner--success';
      case 'MOYENNE': return 'belgian40ter-banner belgian40ter-banner--warning';
      case 'FAIBLE': return 'belgian40ter-banner belgian40ter-banner--danger';
      default: return 'belgian40ter-banner';
    }
  }

  bannerIcon(verdict: VerdictProbabilite | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE': return 'check_circle';
      case 'MOYENNE': return 'warning';
      case 'FAIBLE': return 'error';
      default: return 'info_outline';
    }
  }

  verdictLabel(verdict: VerdictProbabilite | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE': return 'Probabilité d\'acceptation élevée';
      case 'MOYENNE': return 'Probabilité d\'acceptation moyenne';
      case 'FAIBLE': return 'Probabilité d\'acceptation faible';
      default: return '';
    }
  }

  lienFamilialLabel(code: LienFamilial | null | undefined): string {
    if (!code) return '';
    return LIENS_FAMILIAUX.find((l) => l.code === code)?.label ?? code;
  }

  /** Classe du badge différentiel — vert si ≥ 0, rouge si négatif. */
  differentielBadgeClass(diff: number | null | undefined): string {
    if (diff === null || diff === undefined) return 'belgian40ter-diff-badge';
    return diff >= 0
      ? 'belgian40ter-diff-badge belgian40ter-diff-badge--positive'
      : 'belgian40ter-diff-badge belgian40ter-diff-badge--negative';
  }

  /** Texte humain du différentiel (signé, en €). */
  differentielText(diff: number | null | undefined, seuil: number | null | undefined): string {
    if (diff === null || diff === undefined) return '';
    const sign = diff >= 0 ? '+' : '';
    if (diff >= 0) {
      return `${sign}${diff} € > seuil 120 % RIS`;
    }
    return `${diff} € sous seuil`;
  }

  alertTooltip(alert: IM14_40terCoherenceAlert): string {
    return alert.reason;
  }

  alertBadgeLabel(alert: IM14_40terCoherenceAlert): string {
    if (alert.severity === 'CRITICAL') return `Alerte critique (${alert.expectedDisplay})`;
    return `Incohérence détectée (${alert.expectedDisplay})`;
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.lienFamilial.set(r.lienFamilial);
        this.regroupantBelge.set(r.regroupantBelge);
        this.revenusMensuelsNetsEur.set(r.revenusMensuelsNetsEur);
        this.seuil120PctRisEur.set(r.seuil120PctRisEur);
        this.assuranceMaladie.set(r.assuranceMaladie);
        this.logementSuffisant.set(r.logementSuffisant);
        this.menaceOrdrePublic.set(r.menaceOrdrePublic);
        this.dateDepotDemande.set(r.dateDepotDemande);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire
        // et on applique le pré-fill IA si aiData disponible.
        this.loading.set(false);
        this.prefillFromAi();
      },
    });
  }
}
