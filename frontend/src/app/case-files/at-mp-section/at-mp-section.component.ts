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
import { MatRadioModule } from '@angular/material/radio';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AtMpService } from '../../core/services/at-mp.service';
import {
  ATMP_COMPETENCE_LABELS,
  ATMP_DISPOSITIF_LABELS,
  ATMP_VERDICT_LABELS,
  AtMpCompetence,
  AtMpDispositif,
  AtMpRequest,
  AtMpResponse,
  AtMpVerdictRecevabilite,
  NUMERO_TABLEAU_HORS_TABLEAU,
} from '../../core/models/at-mp.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import {
  AtMpSectionPrefillRules,
  computeDateAccident as computeDateAccidentRule,
} from './at-mp-section-prefill-rules';

/**
 * SF-DT-33-02 : champs d'alerte F-IA-03 exposés par l'outil "AT/MP".
 * - DATE_ACCIDENT : divergence si IA (proxy `dateLicenciement`) suggère une
 *   date différente de la `dateAccident` saisie par l'avocat sur
 *   RECONNAISSANCE_AT (le pipeline IA n'extrait pas encore une `dateAccident`
 *   dédiée — pré-fill gracieux).
 */
export type AtMpAlertField = 'DATE_ACCIDENT';

export type AtMpAlertSource = CoherenceAlertSource;
export type AtMpCoherenceAlert = CoherenceAlert<AtMpAlertField>;

/**
 * SF-DT-33-02 : outil décisionnel "Accident du travail / Maladie
 * professionnelle" — recevabilité d'une procédure AT/MP française
 * (CSS L.411-1 / L.461-1 / L.434-2). 3 dispositifs : RECONNAISSANCE_AT,
 * RECONNAISSANCE_MP, CONTESTATION_TAUX_IPP.
 *
 * FR uniquement (bannière info BE — équivalent FEDRIS = backlog jumeau).
 *
 * Consomme l'API SF-DT-33-01 (mergée PR #649). Affiché conditionnellement
 * par le panel F-IA-04 (tool_id 'F-DT-33-at-mp' — migration 175).
 *
 * Patterns de référence :
 * - Pattern jumeau FR-only verdict 3-niveaux : `protection-rp-section`
 *   (F-DT-30, PR #634).
 * - Pattern multi-dispositifs radio + champs conditionnels :
 *   `mesures-eloignement-section` (F-IM-20).
 * - Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-at-mp-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './at-mp-section.component.html',
  styleUrl: './at-mp-section.component.scss',
})
export class AtMpSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'ACCIDENT DU TRAVAIL / MALADIE PROFESSIONNELLE (FR)';
  static readonly TOOL_ICON = 'healing';

  /**
   * F-177 SF-177-12 / F-236 SF-236-02 — Compte les champs que `prefillFromAi()`
   * poserait potentiellement, sans instancier le composant. Délègue au helper
   * partagé `AtMpSectionPrefillRules` qui est aussi consommé par le runtime
   * ci-dessous — divergence impossible par construction.
   */
  static getPrefillCount(input: {
    aiData?: any;
    procedureChecks?: any[];
    aiQuestions?: any[];
    piecesManquantes?: any[];
    triggerEvents?: any[];
    workspaceCountry?: string;
  }): number {
    return AtMpSectionPrefillRules.computePrefillCount({ aiData: input.aiData });
  }

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
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

  // Snapshots signal des inputs IA pour que `computed` réagisse.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<AtMpResponse | null>(null);

  // Form fields — dispositif
  dispositif = signal<AtMpDispositif | null>(null);

  // RECONNAISSANCE_AT
  dateAccident = signal<string | null>(null);
  lieuTravail = signal<boolean>(false);
  declarationEmployeurDansLes48h = signal<boolean>(false);
  certificatMedicalInitial = signal<boolean>(false);

  // RECONNAISSANCE_MP
  numeroTableau = signal<string | null>(null);
  numeroTableauIsHorsTableau = signal<boolean>(false);
  delaiPriseEnChargeRespecte = signal<boolean>(true);
  dateExposition = signal<string | null>(null);

  // CONTESTATION_TAUX_IPP
  tauxFixeParCpam = signal<number | null>(null);
  tauxRevendique = signal<number | null>(null);
  expertiseMedicaleProduite = signal<boolean>(false);
  datePremierAvisCpam = signal<string | null>(null);

  /** Provenance IA pour les champs pré-remplissables (uniquement dateAccident pour V1). */
  provenanceDateAccident = signal<'IA' | null>(null);

  /** Listes pour mat-radio. */
  readonly dispositifOptions = ATMP_DISPOSITIF_LABELS;
  readonly verdictLabels = ATMP_VERDICT_LABELS;
  readonly competenceLabels = ATMP_COMPETENCE_LABELS;

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');
  isAt = computed<boolean>(() => this.dispositif() === 'RECONNAISSANCE_AT');
  isMp = computed<boolean>(() => this.dispositif() === 'RECONNAISSANCE_MP');
  isIpp = computed<boolean>(() => this.dispositif() === 'CONTESTATION_TAUX_IPP');

  /**
   * Alertes de cohérence F-IA-03 par field.
   * Gate : uniquement en mode formulaire ET dispositif AT.
   */
  coherenceAlerts = computed<Partial<Record<AtMpAlertField, AtMpCoherenceAlert>>>(() => {
    // F-163 SF-163-02b : aucune source IA en standalone.
    if (this.standaloneMode) return {} as any;
    if (!this.showForm()) return {};
    if (this.dispositif() !== 'RECONNAISSANCE_AT') return {};
    const alerts: Partial<Record<AtMpAlertField, AtMpCoherenceAlert>> = {};
    const a = this.buildDateAccidentAlert();
    if (a) alerts.DATE_ACCIDENT = a;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: AtMpService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
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
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // Ré-applique le prefill si aiData change post-mount, en mode formulaire
    // ET sans résultat persisté chargé.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isFrance() && this.showForm() && !this.result()) {
      if (!this.standaloneMode) this.prefillFromAi();
    }
  }

  alertTooltip(alert: AtMpCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: AtMpCoherenceAlert): string {
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
   * Divergence date d'accident — IA (proxy `dateLicenciement`) suggère une
   * date différente de celle saisie par l'avocat. Multi-sources F96 /
   * QUESTION_IA / IA / PIECE_MANQUANTE.
   */
  private buildDateAccidentAlert(): AtMpCoherenceAlert | null {
    const userDate = this.dateAccident();
    if (!userDate) return null;

    const builder = CoherenceAlertBuilder.forField<AtMpAlertField>('DATE_ACCIDENT');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      const code = chk.critereCode?.toUpperCase();
      if (code !== 'AT_MP_DATE_ACCIDENT' && code !== 'DATE_ACCIDENT') continue;
      const ev = chk.expectedValue;
      if (!ev || ev === userDate) continue;
      builder.addSource('F96', {
        expectedDisplay: this.formatDate(ev),
        reason: `Checklist procédurale : date attendue ${this.formatDate(ev)}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      const code = q.critereCode?.toUpperCase();
      if (code !== 'AT_MP_DATE_ACCIDENT' && code !== 'DATE_ACCIDENT') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue;
      if (!ev || ev === userDate) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: this.formatDate(ev),
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.dateLicenciement` (proxy gracieux).
    const iaDate = this.aiDataSignal()?.dateLicenciement;
    if (iaDate && iaDate !== userDate) {
      builder.addSource('IA', {
        expectedDisplay: this.formatDate(iaDate),
        reason: `Analyse du dossier : date détectée ${this.formatDate(iaDate)}`,
      });
    }

    // 4. PIECE_MANQUANTE — contributor enrichissant.
    const piece = this.findPieceManquante(['AT_MP_DATE_ACCIDENT', 'DATE_ACCIDENT']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

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
    const d = this.dispositif();
    if (!d) return false;
    switch (d) {
      case 'RECONNAISSANCE_AT':
        return !!this.dateAccident();
      case 'RECONNAISSANCE_MP':
        if (!this.dateExposition()) return false;
        if (this.numeroTableauIsHorsTableau()) return true;
        return !!this.numeroTableau() && this.numeroTableau()!.trim().length > 0;
      case 'CONTESTATION_TAUX_IPP': {
        const tFixe = this.tauxFixeParCpam();
        const tRev = this.tauxRevendique();
        if (tFixe === null || tFixe === undefined) return false;
        if (tRev === null || tRev === undefined) return false;
        if (tFixe < 0 || tFixe > 100) return false;
        if (tRev < 0 || tRev > 100) return false;
        if (tRev <= tFixe) return false;
        if (!this.datePremierAvisCpam()) return false;
        return true;
      }
      default:
        return false;
    }
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers onChange — effacent la provenance IA au 1er changement manuel.
  onDispositifChange(value: AtMpDispositif | null): void {
    this.dispositif.set(value);
  }

  onDateAccidentChange(value: string | null): void {
    this.dateAccident.set(value || null);
    this.provenanceDateAccident.set(null);
  }

  onLieuTravailChange(value: boolean): void {
    this.lieuTravail.set(value);
  }

  onDeclarationEmployeurChange(value: boolean): void {
    this.declarationEmployeurDansLes48h.set(value);
  }

  onCertificatMedicalChange(value: boolean): void {
    this.certificatMedicalInitial.set(value);
  }

  onNumeroTableauChange(value: string | null): void {
    this.numeroTableau.set(value || null);
  }

  onHorsTableauChange(value: boolean): void {
    this.numeroTableauIsHorsTableau.set(value);
    if (value) {
      this.numeroTableau.set(NUMERO_TABLEAU_HORS_TABLEAU);
    } else if (this.numeroTableau() === NUMERO_TABLEAU_HORS_TABLEAU) {
      this.numeroTableau.set(null);
    }
  }

  onDelaiPriseEnChargeRespecteChange(value: boolean): void {
    this.delaiPriseEnChargeRespecte.set(value);
  }

  onDateExpositionChange(value: string | null): void {
    this.dateExposition.set(value || null);
  }

  onTauxFixeChange(value: number | null): void {
    this.tauxFixeParCpam.set(value === null || value === undefined ? null : value);
  }

  onTauxRevendiqueChange(value: number | null): void {
    this.tauxRevendique.set(value === null || value === undefined ? null : value);
  }

  onExpertiseMedicaleChange(value: boolean): void {
    this.expertiseMedicaleProduite.set(value);
  }

  onDatePremierAvisCpamChange(value: string | null): void {
    this.datePremierAvisCpam.set(value || null);
  }

  /**
   * SF-DT-33-02 : pré-fill depuis `aiData` (TravailExtractedData).
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le champ est encore vide (préserve les edits)
   * - n'écrase jamais si provenance !== 'IA'
   * - pré-fill `dateAccident` UNIQUEMENT pour le dispositif AT (proxy
   *   `aiData.dateLicenciement` — le pipeline IA n'extrait pas encore
   *   une `dateAccident` dédiée).
   */
  private prefillFromAi(): void {
    // F-163 SF-163-02b : aucune source IA en standalone.
    if (this.standaloneMode) return;
    const ai = this.aiDataSignal();
    if (!ai) return;

    // F-236 SF-236-02 : la valeur est calculée par le helper partagé. Le gating
    // runtime (dispositif === RECONNAISSANCE_AT) reste local au composant car
    // il dépend de l'état UI signal — pas pré-calculable côté static.
    const iaDate = computeDateAccidentRule({ aiData: ai });
    if (iaDate && this.dispositif() === 'RECONNAISSANCE_AT') {
      if (this.dateAccident() === null
          || this.provenanceDateAccident() === 'IA') {
        this.dateAccident.set(iaDate);
        this.provenanceDateAccident.set('IA');
      }
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const d = this.dispositif()!;
    const request: AtMpRequest = { dispositif: d };
    switch (d) {
      case 'RECONNAISSANCE_AT':
        request.dateAccident = this.dateAccident();
        request.lieuTravail = this.lieuTravail();
        request.declarationEmployeurDansLes48h = this.declarationEmployeurDansLes48h();
        request.certificatMedicalInitial = this.certificatMedicalInitial();
        break;
      case 'RECONNAISSANCE_MP':
        request.numeroTableau = this.numeroTableau();
        request.delaiPriseEnChargeRespecte = this.delaiPriseEnChargeRespecte();
        request.dateExposition = this.dateExposition();
        request.certificatMedicalInitial = this.certificatMedicalInitial();
        break;
      case 'CONTESTATION_TAUX_IPP':
        request.tauxFixeParCpam = this.tauxFixeParCpam();
        request.tauxRevendique = this.tauxRevendique();
        request.expertiseMedicaleProduite = this.expertiseMedicaleProduite();
        request.datePremierAvisCpam = this.datePremierAvisCpam();
        break;
    }
    this.calculating.set(true);
    (this.standaloneMode
      ? this.service.calculateStandalone(request)
      : this.service.calculate(this.caseFileId, request))
      .subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Recevabilité AT/MP analysée', 'OK', { duration: 2500 });
        // F-163 SF-163-02b : pas de dashboard à rafraîchir en standalone.

        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
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
        // Provenance reset — valeurs persistées = saisie avocat.
        this.provenanceDateAccident.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        // Fallback pré-fill IA uniquement ici (pas si GET 200).
        if (!this.standaloneMode) this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  /**
   * Classe CSS du bandeau verdict.
   * - ELEVEE → navy/info.
   * - MOYENNE → or/warning.
   * - FAIBLE → or/warning (palette warning, pas critical — recevabilité
   *   faible n'est pas un vice de procédure absolu type L.2422-1).
   */
  bannerClass(verdict: AtMpVerdictRecevabilite | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'at-mp-banner at-mp-banner--info';
      case 'MOYENNE': return 'at-mp-banner at-mp-banner--warning';
      case 'FAIBLE': return 'at-mp-banner at-mp-banner--warning';
      default: return 'at-mp-banner at-mp-banner--info';
    }
  }

  /** Libellé humain d'un dispositif. */
  dispositifLabel(code: AtMpDispositif | string | null | undefined): string {
    if (!code) return '';
    const opt = ATMP_DISPOSITIF_LABELS.find((o) => o.code === code);
    return opt?.label ?? String(code);
  }

  /** Libellé humain d'un verdict. */
  verdictLabel(code: AtMpVerdictRecevabilite | string | null | undefined): string {
    if (!code) return '';
    const label = (this.verdictLabels as Record<string, string>)[code as string];
    return label ?? String(code);
  }

  /** Libellé humain d'une compétence. */
  competenceLabel(code: AtMpCompetence | string | null | undefined): string {
    if (!code) return '';
    const label = (this.competenceLabels as Record<string, string>)[code as string];
    return label ?? String(code);
  }

  /** Formate une date YYYY-MM-DD en JJ/MM/YYYY pour l'affichage humain. */
  private formatDate(iso: string): string {
    if (!iso || iso.length < 10) return iso;
    const y = iso.slice(0, 4);
    const m = iso.slice(5, 7);
    const d = iso.slice(8, 10);
    return `${d}/${m}/${y}`;
  }
}
