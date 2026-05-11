import {
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
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Belgian40bisService } from '../../core/services/belgian-40bis.service';
import {
  Belgian40bisRequest,
  Belgian40bisResponse,
  LienFamilial,
  LienFamilialOption,
  LIENS_FAMILIAUX,
  RegroupantActiviteCategorie,
  RegroupantActiviteCategorieOption,
  REGROUPANT_ACTIVITES,
  VerdictProbabilite,
} from '../../core/models/belgian-40bis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  ImmigrationExtractedData,
  PieceManquanteEntry,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { Belgian40bisPrefillRules } from './belgian-40bis-section-prefill-rules';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/**
 * SF-IM-14-07 : champs F-IA-03 exposés par l'outil 40bis BE.
 * Aujourd'hui un seul (LIEN_FAMILIAL) — ImmigrationExtractedData
 * ne porte pas encore de champ natif `lienFamilial` IA.
 * Câblage maintenu pour extension future (backlog).
 */
export type Belgian40bisAlertField = 'LIEN_FAMILIAL';

export type Belgian40bisAlertSource = CoherenceAlertSource;
export type Belgian40bisCoherenceAlert = CoherenceAlert<Belgian40bisAlertField>;

/**
 * SF-IM-14-07 : outil décisionnel dédié "Regroupement familial
 * citoyen UE — art. 40bis Loi 15/12/1980" (carte F). BE uniquement.
 *
 * L'équivalent FR (regroupement familial CESEDA) est couvert par
 * F-IM-03 / F-IM-12 ; distinct de l'art. 40ter (regroupant Belge —
 * règles plus strictes, voir SF-IM-14-04 + 08). Si
 * `workspaceCountry !== 'BELGIQUE'`, bannière info (pas de masquage
 * silencieux).
 *
 * Consomme l'API figée en SF-IM-14-03 (backend, PR #510). Affiché
 * conditionnellement par le panel F-IA-04
 * (tool_id 'F-IM-14-40bis-cohabitant-ue-be', règle ALWAYS_ON BE).
 *
 * Patterns de référence :
 * - Template canonique : `harcelement-licenciement-nul-section`.
 * - Pattern IA + provenance : `motif-grave-be-section`.
 * - Pattern gate BE-only + chips critères manquants : `annexe13-be-section`.
 */
@Component({
  selector: 'app-belgian-40bis-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatSlideToggleModule, MatChipsModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './belgian-40bis-section.component.html',
  styleUrl: './belgian-40bis-section.component.scss',
})
export class BelgianCohabitantUeBeSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'REGROUPEMENT FAMILIAL — CITOYEN UE (ART. 40BIS)';
  static readonly TOOL_ICON = 'family_restroom';

  /** F-236 SF-236-02 — Délègue intégralement au helper pur partagé. */
  static getPrefillCount(input: PrefillCountInput): number {
    return Belgian40bisPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  // Inputs IA optionnels (pré-fill best-effort + alertes F-IA-03 — SF-155-04 pattern).
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;


  /**
   * F-163 SF-163-02d — Mode simulateur autonome (hors dossier client).
   *
   * Quand `true` :
   *  - Bannière « 🧪 Mode simulateur » affichée en haut.
   *  - `prefillFromAi()` court-circuité.
   *  - `coherenceAlerts` retourne `{}`.
   *  - `loadExisting()` / `load()` court-circuité.
   *  - `calculate()` POSTe sur `/api/v1/simulators/F-IM-14-40bis-cohabitant-ue-be/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;
  // Snapshots signal des inputs IA (computed-friendly).
  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  submitting = signal(false);
  showForm = signal(true);
  result = signal<Belgian40bisResponse | null>(null);

  // Form fields.
  lienFamilial = signal<LienFamilial | null>(null);
  regroupantCitoyenUe = signal<boolean>(true);
  regroupantActiviteCategorie = signal<RegroupantActiviteCategorie | null>(null);
  ressourcesSuffisantes = signal<boolean>(false);
  assuranceMaladieUe = signal<boolean>(false);
  logementSuffisant = signal<boolean>(false);
  menaceOrdrePublic = signal<boolean>(false);
  dateDepotDemande = signal<string | null>(null);

  // Provenance IA — seules 2 valeurs réellement extraites aujourd'hui
  // (best-effort : `dateDepotProcedure` + `nationaliteUe` côté regroupant).
  provenanceDateDepot = signal<'IA' | null>(null);
  provenanceRegroupantCitoyenUe = signal<'IA' | null>(null);

  // Map sourceKey → explanations (popover F-IA-03-15c).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  readonly liensFamiliaux: LienFamilialOption[] = LIENS_FAMILIAUX;
  readonly categoriesActivite: RegroupantActiviteCategorieOption[] = REGROUPANT_ACTIVITES;

  /** Aujourd'hui en YYYY-MM-DD pour l'attribut max de l'input date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isBelgium = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  /**
   * Alertes F-IA-03 par field. Gate `!showForm()` uniquement
   * (anti-bug SF-IA-03-12 : pas de `|| this.result()`).
   */
  coherenceAlerts = computed<Partial<Record<Belgian40bisAlertField, Belgian40bisCoherenceAlert>>>(() => {
    // F-163 SF-163-02d : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<Belgian40bisAlertField, Belgian40bisCoherenceAlert>> = {};
    const lienAlert = this.buildLienFamilialAlert();
    if (lienAlert) alerts.LIEN_FAMILIAL = lienAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    // Qualification juridique — pas d'urgence temporelle, jamais de CRITICAL.
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: Belgian40bisService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (this.isBelgium()) {
      if (this.standaloneMode) {

        // F-163 SF-163-02d : pas de dossier à interroger en standalone.

        this.collapsed.set(false);

        this.loading.set(false);

        this.showForm.set(true);

        return;

      }
      this.load();
      this.loadSourceExplanations();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // Re-prefill si aiData change avant 1ère résolution (form encore vierge,
    // pas de résultat backend persistant).
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isBelgium() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** Map field → sourceKey (fallback générique 40bis). */
  explanationFor(field: Belgian40bisAlertField): SourceExplanation[] {
    const key = field === 'LIEN_FAMILIAL' ? 'IM14_LIEN_FAMILIAL' : 'IM14_40BIS';
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: Belgian40bisCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: Belgian40bisCoherenceAlert): string {
    const prefix = (() => {
      switch (alert.source) {
        case 'F96': return 'Incohérence Checklist procédurale';
        case 'QUESTION_IA': return 'Incohérence Question complémentaire';
        case 'IA': return 'Incohérence détectée';
        case 'PIECE_MANQUANTE': return 'Pièce manquante';
        case 'MULTI': return 'Incohérence multiple';
      }
    })();
    return `${prefix} (${alert.expectedDisplay})`;
  }

  /**
   * Alerte LIEN_FAMILIAL — câblage prêt mais aucune divergence
   * détectable aujourd'hui car `ImmigrationExtractedData` ne porte
   * pas de champ natif `lienFamilial` IA. Retourne `null` en
   * permanence — extension future via une SF dédiée IA.
   */
  private buildLienFamilialAlert(): Belgian40bisCoherenceAlert | null {
    const userLien = this.lienFamilial();
    if (!userLien) return null;
    const builder = CoherenceAlertBuilder.forField<Belgian40bisAlertField>('LIEN_FAMILIAL')
      .withSeverity('WARNING');
    const piece = this.findPieceManquante(['IM14_LIEN_FAMILIAL', 'IM14_40BIS_LIEN']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * Recherche une `PieceManquanteEntry` dont `critereCode`
   * (case-insensitive) appartient à la whitelist. Retourne le 1er
   * `texte`, ou `null`.
   */
  private findPieceManquante(acceptedCodes: string[]): string | null {
    const norm = new Set(acceptedCodes.map((c) => c.toUpperCase()));
    for (const p of this.piecesManquantesSignal()) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (norm.has(code)) return p.texte;
    }
    return null;
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    if (!this.lienFamilial()) return false;
    if (!this.regroupantActiviteCategorie()) return false;
    const dDepot = this.dateDepotDemande();
    if (dDepot && dDepot > this.todayIso) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers onChange — effacent la provenance IA au 1er changement manuel.
  onLienFamilialChange(value: LienFamilial | null): void {
    this.lienFamilial.set(value);
  }

  onCategorieActiviteChange(value: RegroupantActiviteCategorie | null): void {
    this.regroupantActiviteCategorie.set(value);
  }

  onRegroupantCitoyenUeChange(value: boolean): void {
    this.regroupantCitoyenUe.set(value);
    this.provenanceRegroupantCitoyenUe.set(null);
  }

  onRessourcesSuffisantesChange(value: boolean): void {
    this.ressourcesSuffisantes.set(value);
  }

  onAssuranceMaladieUeChange(value: boolean): void {
    this.assuranceMaladieUe.set(value);
  }

  onLogementSuffisantChange(value: boolean): void {
    this.logementSuffisant.set(value);
  }

  onMenaceOrdrePublicChange(value: boolean): void {
    this.menaceOrdrePublic.set(value);
  }

  onDateDepotDemandeChange(value: string | null): void {
    this.dateDepotDemande.set(value || null);
    this.provenanceDateDepot.set(null);
  }

  /**
   * Pré-fill best-effort depuis `aiData` (ImmigrationExtractedData).
   * Aujourd'hui 2 champs no-op gracieux :
   * - `dateDepotProcedure` (générique) → `dateDepotDemande`
   * - `nationaliteUe` (du contexte) → `regroupantCitoyenUe`
   * Les autres (lienFamilial, catégorie activité) ne sont pas
   * extraits par l'IA actuelle — backlog.
   *
   * Règles fail-open :
   * - silence si aiData absent
   * - chaque champ indépendant
   * - n'écrase jamais une saisie manuelle (provenance !== 'IA')
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 : délègue au helper pur partagé.
    // F-236 SF-236-04 : passe workspaceCountry pour gating BE-only.
    const input: PrefillCountInput = {
      aiData: this.aiDataSignal(),
      workspaceCountry: this.workspaceCountry,
    };

    const date = Belgian40bisPrefillRules.computeDateDepotDemande(input);
    if (date !== null
        && (this.dateDepotDemande() === null || this.provenanceDateDepot() === 'IA')) {
      this.dateDepotDemande.set(date);
      this.provenanceDateDepot.set('IA');
    }

    const ue = Belgian40bisPrefillRules.computeRegroupantCitoyenUe(input);
    if (ue !== null
        && (this.provenanceRegroupantCitoyenUe() !== null
            || ue !== this.regroupantCitoyenUe())) {
      this.regroupantCitoyenUe.set(ue);
      this.provenanceRegroupantCitoyenUe.set('IA');
    }
  }

  submit(): void {
    if (!this.formValid()) return;
    const request: Belgian40bisRequest = {
      lienFamilial: this.lienFamilial()!,
      regroupantCitoyenUe: this.regroupantCitoyenUe(),
      regroupantActiviteCategorie: this.regroupantActiviteCategorie()!,
      ressourcesSuffisantes: this.ressourcesSuffisantes(),
      assuranceMaladieUe: this.assuranceMaladieUe(),
      logementSuffisant: this.logementSuffisant(),
      menaceOrdrePublic: this.menaceOrdrePublic(),
      dateDepotDemande: this.dateDepotDemande(),
    };
    this.submitting.set(true);
    // F-163 SF-163-02d : en standalone, POST sur le dispatcher générique.

    const request$ = this.standaloneMode

      ? this.service.calculateStandalone(request)

      : this.service.calculate(this.caseFileId, request);

    request$.subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.submitting.set(false);
        this.snackBar.open('Regroupement 40bis analysé', 'OK', { duration: 2500 });
        // F-163 SF-163-02d : pas de dashboard à rafraîchir en standalone.

        if (!this.standaloneMode) { this.dashboardRefresh?.triggerRefresh(); }
      },
      error: (err) => {
        this.submitting.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.hydrateFromResult(r);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — reste en mode formulaire + prefill IA.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private hydrateFromResult(r: Belgian40bisResponse): void {
    this.result.set(r);
    this.lienFamilial.set(r.lienFamilial);
    this.regroupantCitoyenUe.set(r.regroupantCitoyenUe);
    this.regroupantActiviteCategorie.set(r.regroupantActiviteCategorie);
    this.ressourcesSuffisantes.set(r.ressourcesSuffisantes);
    this.assuranceMaladieUe.set(r.assuranceMaladieUe);
    this.logementSuffisant.set(r.logementSuffisant);
    this.menaceOrdrePublic.set(r.menaceOrdrePublic);
    this.dateDepotDemande.set(r.dateDepotDemande);
    // Valeurs persistées = saisie avocat — pas de badge IA.
    this.provenanceDateDepot.set(null);
    this.provenanceRegroupantCitoyenUe.set(null);
    this.showForm.set(false);
  }

  /**
   * Classe CSS de la bannière de verdict.
   * - ELEVEE → navy (info_outline)
   * - MOYENNE → or (warning)
   * - FAIBLE → rouge (error) — palette "verdict défavorable",
   *   alignée avec `oqtf-sans-delai-section`. Le verdict FAIBLE
   *   matérialise un risque de refus juridique qualifié — utilisation
   *   du rouge conforme DESIGN_SYSTEM.md.
   */
  bannerClass(verdict: VerdictProbabilite | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE':  return 'b40bis-banner b40bis-banner--info';
      case 'MOYENNE': return 'b40bis-banner b40bis-banner--warning';
      case 'FAIBLE':  return 'b40bis-banner b40bis-banner--danger';
      default:        return 'b40bis-banner';
    }
  }

  bannerIcon(verdict: VerdictProbabilite | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE':  return 'check_circle';
      case 'MOYENNE': return 'warning';
      case 'FAIBLE':  return 'error';
      default:        return 'info_outline';
    }
  }

  verdictLabel(verdict: VerdictProbabilite | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE':  return 'Probabilité élevée';
      case 'MOYENNE': return 'Probabilité moyenne';
      case 'FAIBLE':  return 'Probabilité faible';
      default:        return '';
    }
  }

  lienFamilialLabel(code: LienFamilial | null | undefined): string {
    return LIENS_FAMILIAUX.find((o) => o.code === code)?.label ?? '';
  }

  categorieActiviteLabel(code: RegroupantActiviteCategorie | null | undefined): string {
    return REGROUPANT_ACTIVITES.find((o) => o.code === code)?.label ?? '';
  }
}
