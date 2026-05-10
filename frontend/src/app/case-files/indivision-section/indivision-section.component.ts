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
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { IndivisionService } from '../../core/services/indivision.service';
import {
  IndivisionRequest,
  IndivisionResponse,
  NATURE_BIEN_OPTIONS,
  NatureBien,
  TENTATIVE_PARTAGE_OPTIONS,
  TentativePartage,
  VerdictIndivision,
  natureBienLabel,
  tentativePartageLabel,
  verdictIndivisionLabel,
} from '../../core/models/indivision.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { IndivisionPrefillRules } from './indivision-section-prefill-rules';

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

const VALID_NATURES: ReadonlySet<string> = new Set<NatureBien>([
  'IMMOBILIER',
  'MOBILIER',
  'COMPTES_BANCAIRES',
  'TITRES_FINANCIERS',
  'FONDS_COMMERCE',
  'AUTRE',
]);

const VALID_TENTATIVES: ReadonlySet<string> = new Set<TentativePartage>([
  'PROPOSITION_NOTAIRE',
  'MEDIATION',
  'EXPERTISE_VALORISATION',
  'LICITATION_AMIABLE',
  'AUCUNE',
]);

/**
 * SF-FA-22-02 : champs F-IA-03 audités par l'outil Indivision (art. 815 Cciv).
 * - `DATE_ORIGINE_INDIVISION` : divergence sur la date d'origine de l'indivision
 *   (post-communautaire — souvent date de séparation des époux ou date de
 *   dissolution du régime).
 * - `OCCUPATION_BIEN` : alerte structurelle quand l'IA détecte un logement
 *   commun et l'avocat n'a pas coché l'occupation par un indivisaire (qui
 *   ouvre droit à l'indemnité d'occupation art. 815-9 al. 2).
 */
export type IndivisionAlertField = 'DATE_ORIGINE_INDIVISION' | 'OCCUPATION_BIEN';

export type IndivisionAlertSource = CoherenceAlertSource;
export type IndivisionCoherenceAlert = CoherenceAlert<IndivisionAlertField>;

/**
 * Outil décisionnel Indivision post-communautaire (art. 815 et s. Cciv) —
 * single-country FR.
 *
 * Pattern de référence :
 * - `harcelement-licenciement-nul-section` (template canonique 2026-04-24)
 *   pour le pré-fill IA + validation F-IA-03.
 * - `desaccords-parentaux-section` (F-FA-19-06) pour le pattern multi-select
 *   + CSV liste (quotesPart).
 * - `recompenses-section` (F-FA-15-02) pour la palette navy/or famille.
 *
 * Affiché conditionnellement par le panel F-IA-04 (tool_id `F-FA-22-indivision`).
 */
@Component({
  selector: 'app-indivision-section',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DecimalPipe,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatSlideToggleModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './indivision-section.component.html',
  styleUrl: './indivision-section.component.scss',
})
export class IndivisionSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'INDIVISION POST-COMMUNAUTAIRE (ART. 815 CCIV)';
  static readonly TOOL_ICON = 'apartment';

  /** F-236 SF-236-02 — compteur miroir prefillFromAi via helper. */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return IndivisionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';

  // SF-FA-22-02 : inputs IA (tous optionnels — null-safe partout, no-op gracieux).
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal — réactivité des `computed`.
  private aiDataSignal = signal<FamilleExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // États UI.
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<IndivisionResponse | null>(null);

  // Form fields.
  dateOrigineIndivision = signal<string | null>(null);
  natureBiens = signal<NatureBien[]>([]);
  valeurEstimeeTotaleEur = signal<number>(0);
  nbIndivisaires = signal<number>(2);
  /** Saisi via input texte CSV (parsé). */
  quotesPartRaw = signal<string>('');
  quotesPart = signal<number[]>([]);
  tentativesPartageAmiable = signal<TentativePartage[]>([]);
  consentementPartageGlobal = signal<boolean>(false);
  occupationBienParUnIndivisaire = signal<boolean>(false);
  indivisionDureeAnnees = signal<number>(0);
  demandeMesuresConservatoires = signal<boolean>(false);
  conflitOuvertEntreIndivisaires = signal<boolean>(false);

  // SF-FA-22-02 : provenance IA — un signal par champ pré-remplissable.
  provenanceDateOrigine = signal<'IA' | null>(null);
  provenanceOccupation = signal<'IA' | null>(null);

  // Options statiques exposées au template.
  readonly natureBienOptions = NATURE_BIEN_OPTIONS;
  readonly tentativePartageOptions = TENTATIVE_PARTAGE_OPTIONS;

  // Display helpers.
  natureBienLabel = natureBienLabel;
  tentativePartageLabel = tentativePartageLabel;
  verdictLabel = verdictIndivisionLabel;

  /**
   * Alertes F-IA-03 — gate `showForm()` strict (anti-bug SF-IA-03-12).
   */
  coherenceAlerts = computed<Partial<Record<IndivisionAlertField, IndivisionCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IndivisionAlertField, IndivisionCoherenceAlert>> = {};
    const dateAlert = this.buildDateOrigineAlert();
    if (dateAlert) alerts.DATE_ORIGINE_INDIVISION = dateAlert;
    const occupationAlert = this.buildOccupationAlert();
    if (occupationAlert) alerts.OCCUPATION_BIEN = occupationAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  isFrance = computed(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private service: IndivisionService,
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

    // Re-prefill quand `aiData` arrive après mount, tant que l'avocat n'a
    // pas saisi manuellement et qu'aucun résultat n'est affiché.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  // ---------------------------------------------------------------------------
  // Pré-fill IA + provenance
  // ---------------------------------------------------------------------------

  /**
   * SF-FA-22-02 : pré-remplit `dateOrigineIndivision` (depuis `dateSeparation`)
   * et `occupationBienParUnIndivisaire` (depuis `logementCommunDetected`).
   * No-op gracieux si `aiData` absent ou champs vides.
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé.
    const h = { aiData: this.aiDataSignal() };

    const date = IndivisionPrefillRules.computeDateOrigine(h);
    if (date !== null
        && (this.dateOrigineIndivision() === null || this.provenanceDateOrigine() === 'IA')) {
      this.dateOrigineIndivision.set(date);
      this.provenanceDateOrigine.set('IA');
    }

    if (IndivisionPrefillRules.isOccupationBien(h)
        && (this.occupationBienParUnIndivisaire() === false
            || this.provenanceOccupation() === 'IA')) {
      this.occupationBienParUnIndivisaire.set(true);
      this.provenanceOccupation.set('IA');
    }
  }

  // ---------------------------------------------------------------------------
  // Builders d'alertes F-IA-03
  // ---------------------------------------------------------------------------

  /**
   * Alerte cohérence sur la date d'origine de l'indivision.
   * Sources : IA (`aiData.dateSeparation`), F96 (`FA22_DATE_ORIGINE`),
   * QUESTION_IA, PIECE_MANQUANTE.
   */
  private buildDateOrigineAlert(): IndivisionCoherenceAlert | null {
    const userDate = this.dateOrigineIndivision();
    if (!userDate || !ISO_DATE_REGEX.test(userDate)) return null;

    const ai = this.aiDataSignal();
    const aiDate = ai?.dateSeparation;
    if (!aiDate || !ISO_DATE_REGEX.test(aiDate)) return null;
    if (aiDate === userDate) return null;

    const builder = CoherenceAlertBuilder.forField<IndivisionAlertField>('DATE_ORIGINE_INDIVISION')
      .addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : date de séparation détectée ${aiDate}`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA22_DATE_ORIGINE') continue;
      if (chk.statut !== 'VERIFIED') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: aiDate,
        reason: `Checklist procédurale : date attendue ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'FA22_DATE_ORIGINE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      if (!q.expectedValue) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: aiDate,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA22_DATE_ORIGINE', 'JUGEMENT_DIVORCE', 'PV_NON_CONCILIATION']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Alerte cohérence sur l'occupation du bien — IA détecte un logement
   * commun mais l'avocat n'a pas coché. Impact direct sur l'indemnité
   * d'occupation art. 815-9 al. 2.
   */
  private buildOccupationAlert(): IndivisionCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (typeof ai?.logementCommunDetected !== 'boolean') return null;
    if (ai.logementCommunDetected === false) return null;
    if (this.occupationBienParUnIndivisaire() === true) return null;

    const display = 'Occupation détectée';
    const builder = CoherenceAlertBuilder.forField<IndivisionAlertField>('OCCUPATION_BIEN')
      .addSource('IA', {
        expectedDisplay: display,
        reason: 'Analyse du dossier : logement commun détecté — occupation par un indivisaire probable (indemnité art. 815-9 al. 2)',
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA22_OCCUPATION') continue;
      if (chk.statut !== 'VERIFIED') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA22_OCCUPATION', 'ATTESTATION_OCCUPATION']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private findPieceManquante(acceptedCodes: string[]): string | null {
    const norm = new Set(acceptedCodes.map(c => c.toUpperCase()));
    for (const p of this.piecesManquantesSignal()) {
      const code = p.critereCode?.toUpperCase();
      if (code && norm.has(code)) return p.texte;
    }
    return null;
  }

  alertTooltip(alert: IndivisionCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: IndivisionCoherenceAlert): string {
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

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  // ---------------------------------------------------------------------------
  // Form helpers
  // ---------------------------------------------------------------------------

  /**
   * Parse la chaîne CSV des quotes-part — accepte espaces / virgules /
   * point-virgules. Filtre les valeurs non numériques ou hors range [0, 100].
   */
  parseQuotesPart(raw: string): number[] {
    if (!raw) return [];
    const tokens = raw.split(/[,;\s]+/).filter((t) => t.length > 0);
    const out: number[] = [];
    for (const t of tokens) {
      const n = Number(t.replace(',', '.'));
      if (Number.isFinite(n) && n >= 0 && n <= 100) {
        out.push(n);
      }
    }
    return out;
  }

  formValid(): boolean {
    const date = this.dateOrigineIndivision();
    const natures = this.natureBiens();
    const nb = this.nbIndivisaires();
    const quotes = this.quotesPart();
    const valeur = this.valeurEstimeeTotaleEur();
    return date !== null && ISO_DATE_REGEX.test(date)
      && natures.length > 0
      && nb >= 2
      && quotes.length > 0
      && valeur >= 0;
  }

  /**
   * Indique si la somme des quotes-part diverge significativement de 100 %
   * (tolérance ±0.5). Permet d'afficher un hint dans l'UI sans bloquer
   * le submit (le backend valide formellement).
   */
  quotesPartSumWarning(): boolean {
    const sum = this.quotesPart().reduce((a, b) => a + b, 0);
    return this.quotesPart().length > 0 && Math.abs(sum - 100) > 0.5;
  }

  // Handlers manuels — effacent le badge IA correspondant + state form.
  onDateOrigineChange(value: string | null): void {
    this.dateOrigineIndivision.set(value && value.length > 0 ? value : null);
    this.provenanceDateOrigine.set(null);
  }

  onNatureBiensChange(value: NatureBien[]): void {
    this.natureBiens.set(value ?? []);
  }

  onValeurEstimeeChange(value: number | null): void {
    this.valeurEstimeeTotaleEur.set(value ?? 0);
  }

  onNbIndivisairesChange(value: number | null): void {
    this.nbIndivisaires.set(value ?? 2);
  }

  onQuotesPartRawChange(value: string): void {
    this.quotesPartRaw.set(value ?? '');
    this.quotesPart.set(this.parseQuotesPart(value ?? ''));
  }

  onTentativesChange(value: TentativePartage[]): void {
    this.tentativesPartageAmiable.set(value ?? []);
  }

  onConsentementChange(value: boolean): void {
    this.consentementPartageGlobal.set(!!value);
  }

  onOccupationChange(value: boolean): void {
    this.occupationBienParUnIndivisaire.set(!!value);
    this.provenanceOccupation.set(null);
  }

  onIndivisionDureeChange(value: number | null): void {
    this.indivisionDureeAnnees.set(value ?? 0);
  }

  onMesuresConservatoiresChange(value: boolean): void {
    this.demandeMesuresConservatoires.set(!!value);
  }

  onConflitOuvertChange(value: boolean): void {
    this.conflitOuvertEntreIndivisaires.set(!!value);
  }

  // ---------------------------------------------------------------------------
  // HTTP : load + analyser
  // ---------------------------------------------------------------------------

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: r => {
        this.applyPersistedResult(r);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — reste en mode formulaire et
        // tente le pré-fill IA pour gagner du temps à l'avocat.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private applyPersistedResult(r: IndivisionResponse): void {
    this.result.set(r);
    this.dateOrigineIndivision.set(r.dateOrigineIndivision);
    this.natureBiens.set(r.natureBiens ?? []);
    this.valeurEstimeeTotaleEur.set(r.valeurEstimeeTotaleEur);
    this.nbIndivisaires.set(r.nbIndivisaires);
    this.quotesPart.set(r.quotesPart ?? []);
    this.quotesPartRaw.set((r.quotesPart ?? []).map(q => q.toString()).join(', '));
    this.tentativesPartageAmiable.set(r.tentativesPartageAmiable ?? []);
    this.consentementPartageGlobal.set(r.consentementPartageGlobal);
    this.occupationBienParUnIndivisaire.set(r.occupationBienParUnIndivisaire);
    this.indivisionDureeAnnees.set(r.indivisionDureeAnnees);
    this.demandeMesuresConservatoires.set(r.demandeMesuresConservatoires);
    this.conflitOuvertEntreIndivisaires.set(r.conflitOuvertEntreIndivisaires);
    // Valeurs persistées = saisie avocat — pas de badge IA.
    this.provenanceDateOrigine.set(null);
    this.provenanceOccupation.set(null);
    this.showForm.set(false);
  }

  analyser(): void {
    if (!this.formValid()) return;
    const request: IndivisionRequest = {
      dateOrigineIndivision: this.dateOrigineIndivision()!,
      natureBiens: this.natureBiens(),
      valeurEstimeeTotaleEur: this.valeurEstimeeTotaleEur(),
      nbIndivisaires: this.nbIndivisaires(),
      quotesPart: this.quotesPart(),
      tentativesPartageAmiable: this.tentativesPartageAmiable(),
      consentementPartageGlobal: this.consentementPartageGlobal(),
      occupationBienParUnIndivisaire: this.occupationBienParUnIndivisaire(),
      indivisionDureeAnnees: this.indivisionDureeAnnees(),
      demandeMesuresConservatoires: this.demandeMesuresConservatoires(),
      conflitOuvertEntreIndivisaires: this.conflitOuvertEntreIndivisaires(),
    };
    this.calculating.set(true);
    this.service.analyser(this.caseFileId, request).subscribe({
      next: r => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse indivision effectuée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: err => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * Classe CSS de la bannière de verdict — palette navy/or/rouge selon
   * l'urgence procédurale recommandée.
   */
  verdictBannerClass(verdict: VerdictIndivision): string {
    switch (verdict) {
      case 'PARTAGE_AMIABLE_POSSIBLE': return 'indivision-verdict-banner indivision-verdict-banner--success';
      case 'MEDIATION_PREALABLE': return 'indivision-verdict-banner indivision-verdict-banner--info';
      case 'PARTAGE_JUDICIAIRE_RECOMMANDE': return 'indivision-verdict-banner indivision-verdict-banner--warning';
      case 'LICITATION_REQUISE': return 'indivision-verdict-banner indivision-verdict-banner--danger';
    }
  }
}
