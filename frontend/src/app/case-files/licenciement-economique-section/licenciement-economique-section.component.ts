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
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { LicenciementEconomiqueService } from '../../core/services/licenciement-economique.service';
import {
  AI_MOTIF_TO_MOTIF_ECONOMIQUE,
  CRITERES_ORDRE,
  CritereOrdre,
  CritereOrdreOption,
  LicenciementEconomiqueResponse,
  MOTIFS_ECONOMIQUES,
  MotifEconomique,
  MotifEconomiqueOption,
  PREUVES_MOTIF,
  PreuveMotif,
  PreuveMotifOption,
  QUALITES_PROF,
  QualitesProf,
  QualitesProfOption,
  TENTATIVES_RECLASSEMENT,
  TentativeReclassement,
  TentativeReclassementOption,
  VerdictRisqueRequalification,
  critereOrdreLabel,
  motifEconomiqueLabel,
  verdictRisqueLabel,
} from '../../core/models/licenciement-economique.model';
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
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import {
  LicenciementEconomiqueSectionPrefillRules,
} from './licenciement-economique-section-prefill-rules';

/**
 * SF-DT-13-02 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-DT-13 (licenciement économique, FR uniquement, art. L.1233-3/4/5/45).
 */
export type LicEcoAlertField = 'MOTIF_ECONOMIQUE' | 'DATE_NOTIFICATION';

// Aliases locaux rétro-compat — utilise l'interface générique partagée.
export type LicEcoAlertSource = CoherenceAlertSource;
export type LicEcoCoherenceAlert = CoherenceAlert<LicEcoAlertField>;

/** Regex ISO date YYYY-MM-DD pour comparaison stricte de la date notification. */
const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/**
 * SF-DT-13-02 : outil décisionnel "Licenciement économique" (FR uniquement,
 * art. L.1233-3 + L.1233-4 + L.1233-5 + L.1233-45 Code du travail).
 * Consomme l'API SF-DT-13-01 (PR #585). Affiché conditionnellement par le
 * panel F-IA-04 (tool_id `F-DT-13-licenciement-economique`).
 *
 * Pattern de référence : `harcelement-licenciement-nul-section` (canon F-IA-03)
 * + `divorce-faute-section` (multi-select + 3 cartes scores).
 *
 * Cf. `ai-skills/frontend-coherence-audit.md` §5.
 */
@Component({
  selector: 'app-licenciement-economique-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatChipsModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './licenciement-economique-section.component.html',
  styleUrl: './licenciement-economique-section.component.scss',
})
export class LicenciementEconomiqueSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'LICENCIEMENT ÉCONOMIQUE (FR) — ART. L.1233-3/4/5/45';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: {
    aiData?: any;
    procedureChecks?: any[];
    aiQuestions?: any[];
    piecesManquantes?: any[];
    triggerEvents?: any[];
    workspaceCountry?: string;
  }): number {
    return LicenciementEconomiqueSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
    });
  }

  static readonly TOOL_ICON = 'gavel';

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // Pré-fill IA + multi-sources F-IA-03 (tous optionnels — null-safe partout).
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

  // Snapshots signal pour que `computed` réagisse aux changements d'input.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<LicenciementEconomiqueResponse | null>(null);

  // Form state.
  motifEconomiqueInvoque = signal<MotifEconomique | null>(null);
  preuvesMotif = signal<PreuveMotif[]>([]);
  criteresOrdreAppliques = signal<CritereOrdre[]>([]);
  salarieAge = signal<number | null>(null);
  salarieAncienneteMois = signal<number | null>(null);
  salarieChargesFamille = signal<number | null>(null);
  salarieQualitesProf = signal<QualitesProf | null>(null);
  tentativesReclassement = signal<TentativeReclassement[]>([]);
  prioriteReembauchePropose = signal<boolean>(false);
  congeReclassementPropose = signal<boolean>(false);
  dateNotification = signal<string | null>(null);

  // Provenance IA par champ.
  provenanceMotif = signal<'IA' | null>(null);
  provenanceDateNotification = signal<'IA' | null>(null);

  // SF-IA-03-15c : map {sourceKey → explanations} pour le popover F-IA-03.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  readonly motifsOptions: MotifEconomiqueOption[] = MOTIFS_ECONOMIQUES;
  readonly preuvesOptions: PreuveMotifOption[] = PREUVES_MOTIF;
  readonly criteresOptions: CritereOrdreOption[] = CRITERES_ORDRE;
  readonly qualitesOptions: QualitesProfOption[] = QUALITES_PROF;
  readonly tentativesOptions: TentativeReclassementOption[] = TENTATIVES_RECLASSEMENT;

  isFrance = computed(() => this.workspaceCountry === 'FRANCE');

  // SF-DT-13-02 : alertes de cohérence F-IA-03 calculées dynamiquement via
  // le builder partagé. Gate strict `showForm()` (anti-bug SF-IA-03-12).
  coherenceAlerts = computed<Partial<Record<LicEcoAlertField, LicEcoCoherenceAlert>>>(() => {
    // F-163 SF-163-02b : aucune source IA en standalone.
    if (this.standaloneMode) return {} as any;
    if (!this.showForm()) return {};
    const alerts: Partial<Record<LicEcoAlertField, LicEcoCoherenceAlert>> = {};
    const motifAlert = this.buildMotifAlert();
    if (motifAlert) alerts.MOTIF_ECONOMIQUE = motifAlert;
    const dateAlert = this.buildDateNotificationAlert();
    if (dateAlert) alerts.DATE_NOTIFICATION = dateAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: LicenciementEconomiqueService,
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

    // Ré-appliquer le pré-fill quand `aiData` change après mount, sauf si
    // l'avocat a déjà saisi (provenance null sur champ rempli) ou si un
    // résultat persisté est présent (form masqué).
    if (changes['aiData'] && !changes['aiData'].firstChange && this.showForm() && !this.result()) {
      if (!this.standaloneMode) this.prefillFromAi();
    }
  }

  /**
   * SF-DT-13-02 : pré-remplit le form depuis `aiData`. N'écrase jamais une
   * saisie avocat existante (la garde est par champ — provenance IA OK ou
   * champ encore vide). Mappe :
   * - `motifLicenciement` (chaîne libre) → `motifEconomiqueInvoque` (enum).
   * - `dateLicenciement` (ISO) → `dateNotification` (ISO).
   *
   * Les autres champs (preuves, critères, tentatives, qualités prof, âge,
   * ancienneté, charges famille) ne sont pas pré-remplis car le pipeline IA
   * n'extrait pas ces champs côté `TravailExtractedData` actuel — l'avocat
   * les saisit manuellement.
   */
  private prefillFromAi(): void {
    // F-163 SF-163-02b : aucune source IA en standalone.
    if (this.standaloneMode) return;
    const ai = this.aiDataSignal();
    if (!ai) return;

    // F-236 SF-236-02 : valeurs calculées par le helper partagé (parité static).
    const ruleInput = { aiData: ai };

    // 1. Motif économique : mapping IA chaîne libre → enum UI.
    const mappedMotif = LicenciementEconomiqueSectionPrefillRules.computeMotifEconomique(ruleInput);
    if (mappedMotif && (this.motifEconomiqueInvoque() === null || this.provenanceMotif() === 'IA')) {
      this.motifEconomiqueInvoque.set(mappedMotif);
      this.provenanceMotif.set('IA');
    }

    // 2. Date notification : `dateLicenciement` IA (YYYY-MM-DD).
    const dateNotif = LicenciementEconomiqueSectionPrefillRules.computeDateNotification(ruleInput);
    if (dateNotif) {
      if (this.dateNotification() === null || this.provenanceDateNotification() === 'IA') {
        this.dateNotification.set(dateNotif);
        this.provenanceDateNotification.set('IA');
      }
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

  /** SF-IA-03-15c : mapping field → sourceKey pour le popover F-IA-03. */
  explanationFor(field: LicEcoAlertField): SourceExplanation[] {
    const key = field === 'MOTIF_ECONOMIQUE'
      ? 'DT13_MOTIF_ECONOMIQUE'
      : 'DT13_DATE_NOTIFICATION';
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: LicEcoCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: LicEcoCoherenceAlert): string {
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

  // ---------------------------------------------------------------------------
  // Builders d'alertes F-IA-03 (un par field) — via CoherenceAlertBuilder.
  // ---------------------------------------------------------------------------

  /**
   * SF-DT-13-02 : motif économique — divergence IA `motifLicenciement` (chaîne
   * libre, mappable) vs saisie avocat (enum). Multi-sources : IA + F96
   * (`DT13_MOTIF_ECONOMIQUE`) + QUESTION_IA + PIECE_MANQUANTE.
   */
  private buildMotifAlert(): LicEcoCoherenceAlert | null {
    const userMotif = this.motifEconomiqueInvoque();
    if (!userMotif) return null;

    const ai = this.aiDataSignal();
    const iaMotifRaw = ai?.motifLicenciement?.toUpperCase();
    const iaMapped = iaMotifRaw ? AI_MOTIF_TO_MOTIF_ECONOMIQUE[iaMotifRaw] : undefined;

    const builder = CoherenceAlertBuilder.forField<LicEcoAlertField>('MOTIF_ECONOMIQUE');
    const validCodes = new Set<string>(MOTIFS_ECONOMIQUES.map((m) => m.code));

    // 1. IA — mapping motifLicenciement → enum.
    if (iaMapped && iaMapped !== userMotif) {
      const display = motifEconomiqueLabel(iaMapped);
      builder.addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : motif économique pressenti "${display}"`,
      });
    }

    // 2. F-96 checklist procédurale — critereCode `DT13_MOTIF_ECONOMIQUE`.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'DT13_MOTIF_ECONOMIQUE') continue;
      const ev = chk.expectedValue?.toUpperCase();
      if (!ev || !validCodes.has(ev)) continue;
      if (ev === userMotif) continue;
      const label = motifEconomiqueLabel(ev as MotifEconomique);
      builder.addSource('F96', {
        expectedDisplay: label,
        reason: `Checklist procédurale : motif attendu ${label}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    // 3. QUESTION_IA — réponse "oui" sur DT13_MOTIF_ECONOMIQUE avec expectedValue.
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'DT13_MOTIF_ECONOMIQUE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue?.toUpperCase();
      if (!ev || !validCodes.has(ev)) continue;
      if (ev === userMotif) continue;
      const label = motifEconomiqueLabel(ev as MotifEconomique);
      builder.addSource('QUESTION_IA', {
        expectedDisplay: label,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 4. PIECE_MANQUANTE — contributor additionnel si alerte initiée.
    const piece = this.findPieceManquante(['DT13_MOTIF_ECONOMIQUE', 'COMPTABILITE', 'RAPPORT_EXPERT']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * SF-DT-13-02 : date notification — divergence stricte sur la chaîne ISO IA
   * `dateLicenciement` vs saisie avocat. Multi-sources : IA + F96 + PIECE_MANQUANTE
   * (lettre de licenciement).
   */
  private buildDateNotificationAlert(): LicEcoCoherenceAlert | null {
    const user = this.dateNotification();
    if (!user) return null;
    const ai = this.aiDataSignal();
    const aiDate = ai?.dateLicenciement;

    const builder = CoherenceAlertBuilder.forField<LicEcoAlertField>('DATE_NOTIFICATION');

    // 1. IA — `dateLicenciement` divergent.
    if (aiDate && ISO_DATE_REGEX.test(aiDate) && user !== aiDate) {
      builder.addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : licenciement notifié le ${aiDate}`,
      });
    }

    // 2. F-96 checklist procédurale.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'DT13_DATE_NOTIFICATION') continue;
      const ev = chk.expectedValue;
      if (!ev || !ISO_DATE_REGEX.test(ev)) continue;
      if (ev === user) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : date notification attendue ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    // 3. PIECE_MANQUANTE — lettre de licenciement.
    const piece = this.findPieceManquante(['DT13_DATE_NOTIFICATION', 'LETTRE_LICENCIEMENT']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Renvoie le `texte` d'une `PieceManquanteEntry` dont `critereCode`
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

  // ---------------------------------------------------------------------------
  // Form handlers (avec effacement badge IA si applicable)
  // ---------------------------------------------------------------------------

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const motif = this.motifEconomiqueInvoque();
    const date = this.dateNotification();
    return motif !== null && date !== null && date.length > 0;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onMotifChange(value: MotifEconomique | null): void {
    this.motifEconomiqueInvoque.set(value);
    this.provenanceMotif.set(null);
  }

  onPreuvesChange(value: PreuveMotif[]): void {
    this.preuvesMotif.set(value ?? []);
  }

  onCriteresChange(value: CritereOrdre[]): void {
    this.criteresOrdreAppliques.set(value ?? []);
  }

  onSalarieAgeChange(value: number | null): void {
    this.salarieAge.set(value);
  }

  onSalarieAncienneteChange(value: number | null): void {
    this.salarieAncienneteMois.set(value);
  }

  onSalarieChargesChange(value: number | null): void {
    this.salarieChargesFamille.set(value);
  }

  onQualitesProfChange(value: QualitesProf | null): void {
    this.salarieQualitesProf.set(value);
  }

  onTentativesChange(value: TentativeReclassement[]): void {
    this.tentativesReclassement.set(value ?? []);
  }

  onPrioriteReembaucheChange(value: boolean): void {
    this.prioriteReembauchePropose.set(!!value);
  }

  onCongeReclassementChange(value: boolean): void {
    this.congeReclassementPropose.set(!!value);
  }

  onDateNotificationChange(value: string | null): void {
    this.dateNotification.set(value && value.length > 0 ? value : null);
    this.provenanceDateNotification.set(null);
  }

  // ---------------------------------------------------------------------------
  // Display helpers
  // ---------------------------------------------------------------------------

  motifEconomiqueLabel = motifEconomiqueLabel;
  critereOrdreLabel = critereOrdreLabel;
  verdictRisqueLabel = verdictRisqueLabel;

  /**
   * Convention palette F-IA-04 / DESIGN_SYSTEM.md :
   * - FAIBLE → navy (info_outline)
   * - MOYENNE → or accent (warning)
   * - ELEVEE → rouge danger (gavel/error)
   */
  verdictBannerClass(verdict: VerdictRisqueRequalification): string {
    switch (verdict) {
      case 'FAIBLE': return 'lec-verdict-banner lec-verdict-banner--info';
      case 'MOYENNE': return 'lec-verdict-banner lec-verdict-banner--warning';
      case 'ELEVEE': return 'lec-verdict-banner lec-verdict-banner--danger';
    }
  }

  verdictChipClass(verdict: VerdictRisqueRequalification): string {
    switch (verdict) {
      case 'FAIBLE': return 'lec-chip lec-chip--info';
      case 'MOYENNE': return 'lec-chip lec-chip--warning';
      case 'ELEVEE': return 'lec-chip lec-chip--danger';
    }
  }

  verdictIcon(verdict: VerdictRisqueRequalification): string {
    switch (verdict) {
      case 'FAIBLE': return 'info_outline';
      case 'MOYENNE': return 'warning';
      case 'ELEVEE': return 'gavel';
    }
  }

  // ---------------------------------------------------------------------------
  // HTTP
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request = {
      motifEconomiqueInvoque: this.motifEconomiqueInvoque()!,
      preuvesMotif: this.preuvesMotif(),
      criteresOrdreAppliques: this.criteresOrdreAppliques(),
      salarieAge: this.salarieAge(),
      salarieAncienneteMois: this.salarieAncienneteMois(),
      salarieChargesFamille: this.salarieChargesFamille(),
      salarieQualitesProf: this.salarieQualitesProf(),
      tentativesReclassement: this.tentativesReclassement(),
      prioriteReembauchePropose: this.prioriteReembauchePropose(),
      congeReclassementPropose: this.congeReclassementPropose(),
      dateNotification: this.dateNotification()!,
    };
    this.calculating.set(true);
    (this.standaloneMode
      ? this.service.calculateStandalone(request)
      : this.service.calculate(this.caseFileId, request))
      .subscribe({
      next: (r) => {
        this.applyPersistedResult(r);
        this.calculating.set(false);
        this.snackBar.open('Risque de requalification analysé', 'OK', { duration: 2500 });
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
        this.applyPersistedResult(r);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — reste en mode formulaire.
        if (!this.standaloneMode) this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private applyPersistedResult(r: LicenciementEconomiqueResponse): void {
    this.result.set(r);
    this.motifEconomiqueInvoque.set(r.motifEconomiqueInvoque);
    this.preuvesMotif.set(r.preuvesMotif ?? []);
    this.criteresOrdreAppliques.set(r.criteresOrdreAppliques ?? []);
    this.salarieAge.set(r.salarieAge);
    this.salarieAncienneteMois.set(r.salarieAncienneteMois);
    this.salarieChargesFamille.set(r.salarieChargesFamille);
    this.salarieQualitesProf.set(r.salarieQualitesProf);
    this.tentativesReclassement.set(r.tentativesReclassement ?? []);
    this.prioriteReembauchePropose.set(r.prioriteReembauchePropose);
    this.congeReclassementPropose.set(r.congeReclassementPropose);
    this.dateNotification.set(r.dateNotification);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceMotif.set(null);
    this.provenanceDateNotification.set(null);
    this.showForm.set(false);
  }
}
