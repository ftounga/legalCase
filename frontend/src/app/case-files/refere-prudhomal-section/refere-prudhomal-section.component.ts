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
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ReferePrudhomalService } from '../../core/services/refere-prudhomal.service';
import {
  NATURES_CREANCE,
  NatureCreance,
  NatureCreanceOption,
  PREUVES_URGENCE,
  PreuveUrgence,
  PreuveUrgenceOption,
  ReferePrudhomalRequest,
  ReferePrudhomalResponse,
  TYPES_REFERE,
  TypeRefere,
  TypeRefereOption,
  VERDICT_LABELS,
  VerdictRefereDt,
} from '../../core/models/refere-prudhomal.model';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import {
  ReferePrudhomalSectionPrefillRules,
} from './refere-prudhomal-section-prefill-rules';

/** Regex ISO strict YYYY-MM-DD. */
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

/**
 * SF-DT-34-02 : champs alertables F-IA-03 exposés par l'outil F-DT-34
 * (référé prud'homal). Seuls 2 champs sont alimentés par l'IA travail :
 * `dateMiseEnDemeure` (mappé sur `dateLicenciement`) et `ANCIENNETE`
 * (calculée depuis `dateEntree`).
 */
export type RpAlertField = 'DATE_MISE_EN_DEMEURE' | 'ANCIENNETE';

export type RpAlertSource = CoherenceAlertSource;
export type RpCoherenceAlert = CoherenceAlert<RpAlertField>;

/** Seuil d'écart en mois au-delà duquel on déclenche une alerte ancienneté. */
const ANCIENNETE_DIVERGENCE_MOIS = 2;

/**
 * SF-DT-34-02 : outil décisionnel "Référé prud'homal" (F-DT-34, art. R.1454-1
 * Code du travail). FRANCE uniquement (BE = feature jumelle backlog
 * Tribunal du travail BE).
 *
 * Consomme l'API SF-DT-34-01 (parallèle, contrat figé).
 *
 * Affiché par le panel F-IA-04 (tool_id `F-DT-34-refere-prudhomal`,
 * règle ALWAYS_ON FR + travail).
 *
 * Patterns de référence :
 * - Template canonique : `harcelement-licenciement-nul-section` (F-DT-11-02).
 * - Pattern référés (form + 2 cards parallèles) : `referes-admin-section`
 *   (F-IM-08-08).
 * - Pattern multi-cards : `documents-fin-contrat-section` (F-DT-32-02).
 * - Pattern IA + F-IA-03 : `harcelement-licenciement-nul-section` +
 *   `immigration-title-decision-section`.
 * - Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 *
 * Palette : standard (navy/or). Pas de gradation rouge dominant — délai
 * audience typ. 15+ jours, pas d'urgence < 72h.
 */
@Component({
  selector: 'app-refere-prudhomal-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './refere-prudhomal-section.component.html',
  styleUrl: './refere-prudhomal-section.component.scss',
})
export class ReferePrudhomalSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RÉFÉRÉ PRUD\'HOMAL R.1454-1 (FR)';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: {
    aiData?: any;
    procedureChecks?: any[];
    aiQuestions?: any[];
    piecesManquantes?: any[];
    triggerEvents?: any[];
    workspaceCountry?: string;
  }): number {
    return ReferePrudhomalSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  static readonly TOOL_ICON = 'gavel';

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';

  // Sources IA (pré-fill + alertes F-IA-03).
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  /**
   * F-163 SF-163-02b — Mode simulateur autonome (hors dossier client).
   * Quand `true` : bannière simulateur affichée, prefillFromAi() / coherenceAlerts /
   * loadExisting() / triggerRefresh() court-circuités, POST routé vers le dispatcher
   * générique /api/v1/simulators/{toolId}/calculate (SF-163-03).
   */
  @Input() standaloneMode: boolean = false;

  // Signals miroirs des inputs IA pour réactivité computed.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<ReferePrudhomalResponse | null>(null);

  // Champs form.
  typeRefere = signal<TypeRefere | null>(null);
  natureCreance = signal<NatureCreance | null>(null);
  montantProvisionDemandeeEur = signal<number | null>(null);
  absenceContestationSerieuse = signal<boolean>(false);
  preuvesUrgenceProduites = signal<PreuveUrgence[]>([]);
  dommageImmediatCarac = signal<boolean>(false);
  tresorerieEmployeurDouteuse = signal<boolean>(false);
  dateMiseEnDemeure = signal<string | null>(null);
  ancienneteContratMois = signal<number | null>(null);

  // Provenance IA par champ pré-remplissable.
  provenanceTypeRefere = signal<'IA' | null>(null);
  provenanceNatureCreance = signal<'IA' | null>(null);
  provenanceDateMiseEnDemeure = signal<'IA' | null>(null);
  provenanceAnciennete = signal<'IA' | null>(null);

  readonly typesRefere: TypeRefereOption[] = TYPES_REFERE;
  readonly naturesCreance: NatureCreanceOption[] = NATURES_CREANCE;
  readonly preuvesUrgenceOptions: PreuveUrgenceOption[] = PREUVES_URGENCE;
  readonly verdictLabels = VERDICT_LABELS;

  /** Aujourd'hui en YYYY-MM-DD pour l'attribut max des inputs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  // Source explanations (SF-IA-03-15c).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /** Alertes F-IA-03 — gate strict : seulement quand le formulaire est affiché. */
  coherenceAlerts = computed<Partial<Record<RpAlertField, RpCoherenceAlert>>>(() => {
    // F-163 SF-163-02b : aucune source IA en standalone.
    if (this.standaloneMode) return {} as any;
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<RpAlertField, RpCoherenceAlert>> = {};
    const a1 = this.buildDateMiseEnDemeureAlert();
    if (a1) alerts.DATE_MISE_EN_DEMEURE = a1;
    const a2 = this.buildAncienneteAlert();
    if (a2) alerts.ANCIENNETE = a2;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    // Outil scoring — alertes informatives, jamais bloquantes.
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: ReferePrudhomalService,
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
    if (this.isFrance()) {
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
    // Re-prefill si aiData arrive après mount avant la première résolution.
    if (
      changes['aiData'] &&
      !changes['aiData'].firstChange &&
      this.isFrance() &&
      this.showForm() &&
      !this.result()
    ) {
      if (!this.standaloneMode) this.prefillFromAi();
    }
  }

  private loadSourceExplanations(): void {
    // F-163 SF-163-02b : pas de dossier en standalone.
    if (this.standaloneMode) return;
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** Mapping field → sourceKey. Convention `DT34_<FIELD>` ou clé IA brute. */
  explanationFor(field: RpAlertField): SourceExplanation[] {
    const key = field === 'DATE_MISE_EN_DEMEURE' ? 'date_licenciement'
              : field === 'ANCIENNETE' ? 'date_entree'
              : '';
    return this.sourceExplanations().get(key) ?? [];
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  /** Calcule le nombre de mois entiers entre 2 dates ISO YYYY-MM-DD. */
  private monthsBetween(startIso: string, endIso: string): number | null {
    if (!ISO_DATE_RE.test(startIso) || !ISO_DATE_RE.test(endIso)) return null;
    const start = new Date(startIso);
    const end = new Date(endIso);
    if (isNaN(start.getTime()) || isNaN(end.getTime())) return null;
    if (end < start) return 0;
    const years = end.getFullYear() - start.getFullYear();
    const months = end.getMonth() - start.getMonth();
    let total = years * 12 + months;
    if (end.getDate() < start.getDate()) total -= 1;
    return Math.max(total, 0);
  }

  formValid(): boolean {
    const tr = this.typeRefere();
    const nc = this.natureCreance();
    const date = this.dateMiseEnDemeure();
    const montant = this.montantProvisionDemandeeEur();
    const anc = this.ancienneteContratMois();
    if (!tr || !nc || !date) return false;
    if (date > this.todayIso) return false;
    if (montant === null || montant < 0) return false;
    if (anc === null || anc < 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // ----- Handlers manuels (effacent provenance IA). -----

  onTypeRefereChange(value: TypeRefere | null): void {
    this.typeRefere.set(value);
    this.provenanceTypeRefere.set(null);
  }

  onNatureCreanceChange(value: NatureCreance | null): void {
    this.natureCreance.set(value);
    this.provenanceNatureCreance.set(null);
  }

  onMontantProvisionChange(value: number | null): void {
    this.montantProvisionDemandeeEur.set(value);
  }

  onAbsenceContestationChange(value: boolean): void {
    this.absenceContestationSerieuse.set(value);
  }

  onPreuvesUrgenceChange(value: PreuveUrgence[] | null): void {
    this.preuvesUrgenceProduites.set(value ?? []);
  }

  onDommageImmediatChange(value: boolean): void {
    this.dommageImmediatCarac.set(value);
  }

  onTresorerieDouteuseChange(value: boolean): void {
    this.tresorerieEmployeurDouteuse.set(value);
  }

  onDateMiseEnDemeureChange(value: string | null): void {
    this.dateMiseEnDemeure.set(value || null);
    this.provenanceDateMiseEnDemeure.set(null);
  }

  onAncienneteChange(value: number | null): void {
    this.ancienneteContratMois.set(value);
    this.provenanceAnciennete.set(null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: ReferePrudhomalRequest = {
      typeRefere: this.typeRefere()!,
      natureCreance: this.natureCreance()!,
      montantProvisionDemandeeEur: this.montantProvisionDemandeeEur()!,
      absenceContestationSerieuse: this.absenceContestationSerieuse(),
      preuvesUrgenceProduites: this.preuvesUrgenceProduites(),
      dommageImmediatCarac: this.dommageImmediatCarac(),
      trésorerieEmployeurDouteuse: this.tresorerieEmployeurDouteuse(),
      dateMiseEnDemeure: this.dateMiseEnDemeure()!,
      ancienneteContratMois: this.ancienneteContratMois()!,
    };
    this.analyzing.set(true);
    (this.standaloneMode
      ? this.service.analyzeStandalone(request)
      : this.service.analyze(this.caseFileId, request))
      .subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Référé prud\'homal analysé', 'OK', { duration: 2500 });
        // F-163 SF-163-02b : pas de dashboard à rafraîchir en standalone.

        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  // ----- Affichage verdict / scores -----

  verdictLabel(verdict: VerdictRefereDt | null | undefined): string {
    if (!verdict) return '';
    return this.verdictLabels[verdict];
  }

  /**
   * Palette standard (navy / or) — pas de rouge dominant.
   * - PROVISION_PROBABLE / EXPERTISE_RECOMMANDEE → or (action recommandée)
   * - INSUFFISAMMENT_FONDE → navy clair (info négative)
   * - AUTRE_VOIE_RECOMMANDEE → navy clair (info)
   */
  verdictBannerClass(verdict: VerdictRefereDt | null | undefined): string {
    switch (verdict) {
      case 'PROVISION_PROBABLE':       return 'rp-banner rp-banner--success';
      case 'EXPERTISE_RECOMMANDEE':    return 'rp-banner rp-banner--accent';
      case 'INSUFFISAMMENT_FONDE':     return 'rp-banner rp-banner--info';
      case 'AUTRE_VOIE_RECOMMANDEE':   return 'rp-banner rp-banner--info';
      default:                         return 'rp-banner';
    }
  }

  verdictBannerIcon(verdict: VerdictRefereDt | null | undefined): string {
    switch (verdict) {
      case 'PROVISION_PROBABLE':       return 'check_circle';
      case 'EXPERTISE_RECOMMANDEE':    return 'science';
      case 'INSUFFISAMMENT_FONDE':     return 'info_outline';
      case 'AUTRE_VOIE_RECOMMANDEE':   return 'alt_route';
      default:                         return 'info_outline';
    }
  }

  /** Classe CSS d'un score selon sa valeur. */
  scoreClass(score: number): string {
    if (score >= 70) return 'rp-score--high';
    if (score >= 40) return 'rp-score--medium';
    return 'rp-score--low';
  }

  alertBadgeLabel(alert: RpCoherenceAlert): string {
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

  alertTooltip(alert: RpCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  // ----- Pré-fill IA -----

  /**
   * SF-DT-34-02 : pré-fill depuis `aiData` (TravailExtractedData).
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - chaque champ indépendant (prefill partiel OK)
   * - ne pré-remplit QUE si le champ est encore vide ou déjà 'IA'
   * - n'écrase jamais une saisie manuelle de l'avocat
   */
  private prefillFromAi(): void {
    // F-163 SF-163-02b : aucune source IA en standalone.
    if (this.standaloneMode) return;
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (!this.isFrance()) return;
    if (!this.showForm()) return;

    // F-236 SF-236-02 : valeurs calculées par le helper partagé (parité static).
    const ruleInput = {
      aiData: ai, workspaceCountry: this.workspaceCountry, todayIso: this.todayIso,
    };

    // 1) dateMiseEnDemeure — depuis dateLicenciement IA.
    const dateMiseEnDemeure = ReferePrudhomalSectionPrefillRules.computeDateMiseEnDemeure(ruleInput);
    if (dateMiseEnDemeure !== null) {
      if (this.dateMiseEnDemeure() === null || this.provenanceDateMiseEnDemeure() === 'IA') {
        this.dateMiseEnDemeure.set(dateMiseEnDemeure);
        this.provenanceDateMiseEnDemeure.set('IA');
      }
    }

    // 2) ancienneteContratMois — calculée depuis dateEntree IA.
    const anciennete = ReferePrudhomalSectionPrefillRules.computeAncienneteContratMois(ruleInput);
    if (anciennete !== null) {
      if (this.ancienneteContratMois() === null || this.provenanceAnciennete() === 'IA') {
        this.ancienneteContratMois.set(anciennete);
        this.provenanceAnciennete.set('IA');
      }
    }

    // 3) natureCreance — pré-fill HEURES_SUPPLEMENTAIRES si l'IA a détecté des heures sup.
    const natureCreance = ReferePrudhomalSectionPrefillRules.computeNatureCreance(ruleInput);
    if (natureCreance !== null) {
      if (this.natureCreance() === null || this.provenanceNatureCreance() === 'IA') {
        this.natureCreance.set(natureCreance as 'HEURES_SUPPLEMENTAIRES');
        this.provenanceNatureCreance.set('IA');
      }
    }

    // SF-246-21 : montant de la provision demandée.
    const montant = ReferePrudhomalSectionPrefillRules.computeMontantProvision(ruleInput);
    if (montant !== null && this.montantProvisionDemandeeEur() === null) {
      this.montantProvisionDemandeeEur.set(montant);
    }
  }

  // ----- Builders alertes F-IA-03 -----

  /**
   * Divergence date IA (`dateLicenciement`) vs saisie avocat
   * (`dateMiseEnDemeure`). La mise en demeure est typiquement contemporaine
   * de la rupture — divergence > 60 jours suspecte.
   */
  private buildDateMiseEnDemeureAlert(): RpCoherenceAlert | null {
    const userDate = this.dateMiseEnDemeure();
    if (!userDate) return null;
    const ai = this.aiDataSignal();
    const aiDate = typeof ai?.dateLicenciement === 'string' ? ai.dateLicenciement : null;
    if (!aiDate || !ISO_DATE_RE.test(aiDate)) return null;
    if (aiDate === userDate) return null;
    const builder = CoherenceAlertBuilder.forField<RpAlertField>('DATE_MISE_EN_DEMEURE')
      .withSeverity('WARNING')
      .addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : date de rupture ${aiDate} (proxy mise en demeure)`,
      });
    const piece = this.findPieceManquante(['MISE_EN_DEMEURE', 'DT34_DATE_MISE_EN_DEMEURE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * Divergence ancienneté saisie vs ancienneté calculée depuis dateEntree IA.
   * Seuil : 2 mois (jurisprudentiellement la fenêtre normale entre fin de
   * contrat et saisine du Conseil des prud'hommes).
   */
  private buildAncienneteAlert(): RpCoherenceAlert | null {
    const userAnc = this.ancienneteContratMois();
    if (userAnc === null) return null;
    const ai = this.aiDataSignal();
    const dateEntree = typeof ai?.dateEntree === 'string' ? ai.dateEntree : null;
    if (!dateEntree || !ISO_DATE_RE.test(dateEntree)) return null;
    const dateLic = typeof ai?.dateLicenciement === 'string' ? ai.dateLicenciement : null;
    const endRef = (dateLic && ISO_DATE_RE.test(dateLic)) ? dateLic : this.todayIso;
    const aiAnc = this.monthsBetween(dateEntree, endRef);
    if (aiAnc === null) return null;
    if (Math.abs(userAnc - aiAnc) <= ANCIENNETE_DIVERGENCE_MOIS) return null;
    const builder = CoherenceAlertBuilder.forField<RpAlertField>('ANCIENNETE')
      .withSeverity('WARNING')
      .addSource('IA', {
        expectedDisplay: `${aiAnc} mois`,
        reason: `Analyse du dossier : ancienneté calculée ~${aiAnc} mois (depuis ${dateEntree})`,
      });
    const piece = this.findPieceManquante(['DATE_ENTREE', 'CONTRAT', 'DT34_ANCIENNETE']);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * Renvoie le `texte` d'une PieceManquanteEntry dont `critereCode`
   * (case-insensitive) appartient à la liste des codes acceptés, ou `null`.
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

  private load(): void {
    this.loading.set(true);
    // F-163 SF-163-02b : en standalone, pas de dossier à interroger.

    if (this.standaloneMode) {

      this.loading.set(false);

      this.showForm.set(true);

      if (this.collapsed && typeof (this.collapsed as any).set === 'function') this.collapsed.set(false);

      return;

    }

    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.typeRefere.set(r.typeRefere);
        this.natureCreance.set(r.natureCreance);
        this.montantProvisionDemandeeEur.set(r.montantProvisionDemandeeEur);
        this.absenceContestationSerieuse.set(r.absenceContestationSerieuse);
        this.preuvesUrgenceProduites.set(r.preuvesUrgenceProduites ?? []);
        this.dommageImmediatCarac.set(r.dommageImmediatCarac);
        this.tresorerieEmployeurDouteuse.set(r.trésorerieEmployeurDouteuse);
        this.dateMiseEnDemeure.set(r.dateMiseEnDemeure);
        this.ancienneteContratMois.set(r.ancienneteContratMois);
        this.showForm.set(false);
        // Reset provenance — les valeurs viennent du backend, plus de l'IA.
        this.provenanceTypeRefere.set(null);
        this.provenanceNatureCreance.set(null);
        this.provenanceDateMiseEnDemeure.set(null);
        this.provenanceAnciennete.set(null);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on tente le pré-fill IA.
        if (!this.standaloneMode) this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }
}
