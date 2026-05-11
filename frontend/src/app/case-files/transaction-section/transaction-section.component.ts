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
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TransactionService } from '../../core/services/transaction.service';
import {
  CONCESSIONS_EMPLOYEUR,
  CONCESSIONS_SALARIE,
  ConcessionEmployeur,
  ConcessionEmployeurOption,
  ConcessionSalarie,
  ConcessionSalarieOption,
  RUPTURES_PREALABLE,
  RupturePrealable,
  RupturePrealableOption,
  TransactionResponse,
  VerdictValiditeContrat,
  concessionEmployeurLabel,
  concessionSalarieLabel,
  rupturePrealableLabel,
  verdictValiditeLabel,
} from '../../core/models/transaction.model';
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
import { TransactionPrefillRules } from './transaction-section-prefill-rules';

/**
 * SF-DT-31-02 : champs d'alerte F-IA-03 exposés par l'outil F-DT-31
 * (transaction art. 2044 Cciv, FR uniquement).
 */
export type TransactionAlertField =
  | 'SALAIRE_MENSUEL'
  | 'ANCIENNETE'
  | 'RUPTURE_PREALABLE';

export type TransactionAlertSource = CoherenceAlertSource;
export type TransactionCoherenceAlert = CoherenceAlert<TransactionAlertField>;

/** Seuil divergence salaire 10 % (aligné F-DT-09 / canonique). */
const SALAIRE_DIVERGENCE_RATIO = 0.10;
/** Seuil divergence ancienneté 1 an absolu (aligné F-FA-09 dureeMariage). */
const ANCIENNETE_DIVERGENCE_YEARS = 1;
/** Regex ISO date YYYY-MM-DD. */
const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

/**
 * SF-DT-31-02 : mapping IA `motifLicenciement` (chaîne libre extraite par le
 * pipeline IA) → `RupturePrealable` enum. Tolérant aux variantes de casse et
 * aux libellés humains usuels. Valeurs IA non mappables → pas de pré-fill
 * (graceful, fail-open).
 */
const AI_MOTIF_TO_RUPTURE_PREALABLE: Readonly<Record<string, RupturePrealable>> = {
  LICENCIEMENT_PERSONNEL: 'LICENCIEMENT_PERSONNEL',
  LICENCIEMENT_ECONOMIQUE: 'LICENCIEMENT_ECONOMIQUE',
  RUPTURE_CONVENTIONNELLE: 'RUPTURE_CONVENTIONNELLE',
  DEMISSION: 'DEMISSION',
  FIN_CDD: 'FIN_CDD',
};

/**
 * Renvoie le code RupturePrealable mappé depuis une chaîne libre IA, ou null
 * si aucun mapping n'est applicable. Heuristique : (1) match strict sur enum,
 * (2) keywords français usuels.
 */
function mapAiMotifToRupturePrealable(raw: string | null | undefined): RupturePrealable | null {
  if (!raw || typeof raw !== 'string') return null;
  const upper = raw.trim().toUpperCase();
  if (!upper) return null;
  // 1. Match strict
  if (AI_MOTIF_TO_RUPTURE_PREALABLE[upper]) return AI_MOTIF_TO_RUPTURE_PREALABLE[upper];
  // 2. Keywords français
  const lower = raw.toLowerCase();
  if (lower.includes('économique') || lower.includes('economique')) return 'LICENCIEMENT_ECONOMIQUE';
  if (lower.includes('personnel') || lower.includes('faute')) return 'LICENCIEMENT_PERSONNEL';
  if (lower.includes('conventionnelle')) return 'RUPTURE_CONVENTIONNELLE';
  if (lower.includes('démission') || lower.includes('demission')) return 'DEMISSION';
  if (lower.includes('cdd') || lower.includes('fin de contrat')) return 'FIN_CDD';
  return null;
}

const VALID_RUPTURE_CODES: ReadonlySet<string> = new Set<RupturePrealable>([
  'LICENCIEMENT_PERSONNEL',
  'LICENCIEMENT_ECONOMIQUE',
  'RUPTURE_CONVENTIONNELLE',
  'DEMISSION',
  'FIN_CDD',
  'AUTRE',
]);

/**
 * SF-DT-31-02 : outil décisionnel "Transaction art. 2044 Cciv" (FR uniquement).
 * Affiché conditionnellement par le panel F-IA-04 (tool_id `F-DT-31-transaction`).
 * Consomme l'API SF-DT-31-01.
 *
 * Pattern de référence canonique : `harcelement-licenciement-nul-section`
 * (HLN, F-DT-11) — template IA-compliant (cf. `ai-skills/frontend-coherence-audit.md`
 * §5). Multi-select inspiré de `divorce-faute-section` (F-FA-09). Verdict 3
 * niveaux navy/or/rouge classique inspiré de `licenciement-nul-detection-section`.
 */
@Component({
  selector: 'app-transaction-section',
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
  templateUrl: './transaction-section.component.html',
  styleUrl: './transaction-section.component.scss',
})
export class TransactionSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'TRANSACTION (FR) — ART. 2044 CCIV';
  static readonly TOOL_ICON = 'handshake';

  /**
   * F-237 SF-237-02 : static parité runtime/static via helper partagé
   * `TransactionPrefillRules`. Délégation triviale — la logique est dans
   * `transaction-section-prefill-rules.ts` (module pur testé Jest 3-cas).
   *
   * Compte les champs que {@link prefillFromAi} poserait — 3 champs max
   * (salaire brut mensuel, ancienneté dérivée de `dateEntree`, rupture
   * préalable mappée depuis `motifLicenciement`).
   */
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  static getPrefillCount(input: { aiData?: any }): number {
    return TransactionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // Pré-fill IA + validation F-IA-03 (tous optionnels, null-safe).
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal des inputs pour réactivité computed.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<TransactionResponse | null>(null);

  // Form state.
  dateSignature = signal<string | null>(null);
  concessionsEmployeur = signal<ConcessionEmployeur[]>([]);
  concessionsSalarie = signal<ConcessionSalarie[]>([]);
  indemniteTransactionnelleEur = signal<number | null>(null);
  salaireMensuelBrutEur = signal<number | null>(null);
  ancienneteAnnees = signal<number | null>(null);
  renonciationActionExpresse = signal<boolean>(false);
  delaiReflexion15jOk = signal<boolean>(false);
  rupturePrealable = signal<RupturePrealable | null>(null);
  presenceAvocatAssistance = signal<boolean>(false);
  // Note : on garde la propriété backend `viceConsentementAllégué` strictement
  // identique côté request payload (voir `calculate()`), mais on utilise un
  // identifiant ASCII-safe en interne pour éviter les pièges de templating.
  viceConsentementAllegue = signal<boolean>(false);

  // Provenance IA par champ pré-rempli.
  provenanceSalaire = signal<'IA' | null>(null);
  provenanceAnciennete = signal<'IA' | null>(null);
  provenanceRupturePrealable = signal<'IA' | null>(null);

  // Source explanations pour popover F-IA-03-15c.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  readonly concessionsEmployeurOptions: ConcessionEmployeurOption[] = CONCESSIONS_EMPLOYEUR;
  readonly concessionsSalarieOptions: ConcessionSalarieOption[] = CONCESSIONS_SALARIE;
  readonly rupturesPrealableOptions: RupturePrealableOption[] = RUPTURES_PREALABLE;

  isFrance = computed(() => this.workspaceCountry === 'FRANCE');

  // Alertes F-IA-03 calculées dynamiquement (gate strict showForm()).
  coherenceAlerts = computed<Partial<Record<TransactionAlertField, TransactionCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<TransactionAlertField, TransactionCoherenceAlert>> = {};
    const salaire = this.buildSalaireAlert();
    if (salaire) alerts.SALAIRE_MENSUEL = salaire;
    const anciennete = this.buildAncienneteAlert();
    if (anciennete) alerts.ANCIENNETE = anciennete;
    const rupture = this.buildRuptureAlert();
    if (rupture) alerts.RUPTURE_PREALABLE = rupture;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: TransactionService,
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
    // l'avocat a déjà saisi manuellement ou si un résultat persisté est présent.
    if (changes['aiData'] && !changes['aiData'].firstChange && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * F-237 SF-237-02 : pré-remplit le form depuis `aiData` via le helper
   * partagé `TransactionPrefillRules`. N'écrase jamais une saisie avocat
   * existante (la garde est faite par champ — provenance IA OK ou champ vide).
   *
   * Le helper est l'unique source de vérité — toute modification doit y
   * être faite, le static `getPrefillCount` en bénéficie automatiquement.
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    const helperInput = { aiData: ai };

    // 1. Salaire brut mensuel.
    const salaire = TransactionPrefillRules.computeSalaire(helperInput);
    if (salaire !== null) {
      if (this.salaireMensuelBrutEur() === null || this.provenanceSalaire() === 'IA') {
        this.salaireMensuelBrutEur.set(salaire);
        this.provenanceSalaire.set('IA');
      }
    }

    // 2. Ancienneté dérivée depuis dateEntree (et dateLicenciement si présente,
    //    sinon aujourd'hui — fallback identique au helper).
    const anciennete = TransactionPrefillRules.computeAnciennete(helperInput);
    if (anciennete !== null) {
      if (this.ancienneteAnnees() === null || this.provenanceAnciennete() === 'IA') {
        this.ancienneteAnnees.set(anciennete);
        this.provenanceAnciennete.set('IA');
      }
    }

    // 3. Rupture préalable : mapping motifLicenciement → enum.
    const mapped = TransactionPrefillRules.computeRupture(helperInput);
    if (mapped !== null) {
      if (this.rupturePrealable() === null || this.provenanceRupturePrealable() === 'IA') {
        this.rupturePrealable.set(mapped);
        this.provenanceRupturePrealable.set('IA');
      }
    }
  }

  /**
   * Calcule l'ancienneté en années entières (arrondi inférieur) entre
   * `dateEntree` et la date de référence (`dateRef` ou aujourd'hui par défaut).
   * Retourne null si dates invalides ou ordre inversé.
   */
  private computeAncienneteAnnees(dateEntree: string, dateRef: string | null): number | null {
    const entree = new Date(dateEntree);
    if (isNaN(entree.getTime())) return null;
    const ref = dateRef ? new Date(dateRef) : new Date();
    if (dateRef && isNaN(ref.getTime())) return null;
    if (entree > ref) return null;
    const diffMs = ref.getTime() - entree.getTime();
    const years = Math.floor(diffMs / (365.25 * 24 * 60 * 60 * 1000));
    return years >= 0 ? years : null;
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /**
   * SF-IA-03-15c : mapping field → sourceKey pour le popover.
   */
  explanationFor(field: TransactionAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'SALAIRE_MENSUEL': return 'salaire_brut_mensuel';
        case 'ANCIENNETE': return 'DT31_ANCIENNETE';
        case 'RUPTURE_PREALABLE': return 'DT31_RUPTURE_PREALABLE';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: TransactionCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: TransactionCoherenceAlert): string {
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
  // Builders d'alertes F-IA-03 (multi-sources via CoherenceAlertBuilder).
  // ---------------------------------------------------------------------------

  /**
   * Salaire mensuel — divergence > 10 % IA vs user.
   * Multi-sources : IA + F96 (`DT31_SALAIRE_MENSUEL`) + QUESTION_IA + PIECE_MANQUANTE.
   */
  private buildSalaireAlert(): TransactionCoherenceAlert | null {
    const aiSalaire = this.aiDataSignal()?.salaireBrutMensuel;
    const userSalaire = this.salaireMensuelBrutEur();
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    if (typeof userSalaire !== 'number' || userSalaire <= 0) return null;
    const ratio = Math.abs(userSalaire - aiSalaire) / aiSalaire;
    if (ratio <= SALAIRE_DIVERGENCE_RATIO) return null;

    const display = `${aiSalaire.toLocaleString('fr-FR')} €`;
    const builder = CoherenceAlertBuilder.forField<TransactionAlertField>('SALAIRE_MENSUEL')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : salaire brut mensuel ~${display}`,
      });

    // F-96 checklist.
    for (const chk of this.procedureChecksSignal()) {
      const code = chk.critereCode?.toUpperCase();
      if (code !== 'DT31_SALAIRE_MENSUEL' && code !== 'SALAIRE_BRUT_MENSUEL') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : salaire attendu ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    // QUESTION_IA — réponse "oui" sur DT31_SALAIRE_MENSUEL avec expectedValue.
    for (const q of this.aiQuestionsSignal()) {
      const code = q.critereCode?.toUpperCase();
      if (code !== 'DT31_SALAIRE_MENSUEL' && code !== 'SALAIRE_BRUT_MENSUEL') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      if (!q.expectedValue) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: display,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    const piece = this.findPieceManquante([
      'DT31_SALAIRE_MENSUEL',
      'SALAIRE_BRUT_MENSUEL',
      'FICHE_PAIE',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Ancienneté — divergence > 1 an IA (calculée depuis dateEntree) vs user.
   * Multi-sources : IA + F96 (`DT31_ANCIENNETE`) + PIECE_MANQUANTE (CONTRAT_TRAVAIL).
   */
  private buildAncienneteAlert(): TransactionCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const dateEntree = ai?.dateEntree;
    if (!dateEntree || !ISO_DATE_REGEX.test(dateEntree)) return null;
    const refDate = ai?.dateLicenciement && ISO_DATE_REGEX.test(ai.dateLicenciement)
      ? ai.dateLicenciement
      : null;
    const aiAnciennete = this.computeAncienneteAnnees(dateEntree, refDate);
    const userAnciennete = this.ancienneteAnnees();
    if (aiAnciennete === null || aiAnciennete < 0) return null;
    if (typeof userAnciennete !== 'number' || userAnciennete < 0) return null;
    if (Math.abs(userAnciennete - aiAnciennete) <= ANCIENNETE_DIVERGENCE_YEARS) return null;

    const display = `${aiAnciennete} an(s)`;
    const builder = CoherenceAlertBuilder.forField<TransactionAlertField>('ANCIENNETE')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : ancienneté ~${display} (calculée depuis ${dateEntree})`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'DT31_ANCIENNETE') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : ancienneté attendue ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['DT31_ANCIENNETE', 'CONTRAT_TRAVAIL']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Rupture préalable — divergence si IA mappe un autre code que la saisie.
   * Source IA uniquement (mapping interne motifLicenciement).
   */
  private buildRuptureAlert(): TransactionCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const userRupture = this.rupturePrealable();
    if (!userRupture) return null;
    if (!VALID_RUPTURE_CODES.has(userRupture)) return null;
    const mapped = mapAiMotifToRupturePrealable(ai?.motifLicenciement);
    if (!mapped) return null;
    if (mapped === userRupture) return null;

    const display = rupturePrealableLabel(mapped);
    return CoherenceAlertBuilder.forField<TransactionAlertField>('RUPTURE_PREALABLE')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : rupture préalable pressentie "${display}"`,
      })
      .build();
  }

  /**
   * Renvoie le `texte` d'une `PieceManquanteEntry` dont `critereCode`
   * (case-insensitive) appartient à la liste des codes acceptés, ou `null` si
   * aucune pièce ne matche.
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
  // Form handlers
  // ---------------------------------------------------------------------------

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const date = this.dateSignature();
    const ce = this.concessionsEmployeur();
    const cs = this.concessionsSalarie();
    const indem = this.indemniteTransactionnelleEur();
    const sal = this.salaireMensuelBrutEur();
    const anc = this.ancienneteAnnees();
    const rup = this.rupturePrealable();
    return !!date
      && ce.length > 0
      && cs.length > 0
      && typeof indem === 'number' && indem >= 0
      && typeof sal === 'number' && sal > 0
      && typeof anc === 'number' && anc >= 0
      && rup !== null;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onDateSignatureChange(value: string | null): void {
    this.dateSignature.set(value && value.length > 0 ? value : null);
  }

  onConcessionsEmployeurChange(value: ConcessionEmployeur[]): void {
    this.concessionsEmployeur.set(value ?? []);
  }

  onConcessionsSalarieChange(value: ConcessionSalarie[]): void {
    this.concessionsSalarie.set(value ?? []);
  }

  onIndemniteChange(value: number | null): void {
    this.indemniteTransactionnelleEur.set(value);
  }

  onSalaireChange(value: number | null): void {
    this.salaireMensuelBrutEur.set(value);
    this.provenanceSalaire.set(null);
  }

  onAncienneteChange(value: number | null): void {
    this.ancienneteAnnees.set(value);
    this.provenanceAnciennete.set(null);
  }

  onRenonciationChange(value: boolean): void {
    this.renonciationActionExpresse.set(!!value);
  }

  onDelaiReflexionChange(value: boolean): void {
    this.delaiReflexion15jOk.set(!!value);
  }

  onRupturePrealableChange(value: RupturePrealable | null): void {
    this.rupturePrealable.set(value);
    this.provenanceRupturePrealable.set(null);
  }

  onPresenceAvocatChange(value: boolean): void {
    this.presenceAvocatAssistance.set(!!value);
  }

  onViceConsentementChange(value: boolean): void {
    this.viceConsentementAllegue.set(!!value);
  }

  // ---------------------------------------------------------------------------
  // Display helpers
  // ---------------------------------------------------------------------------

  concessionEmployeurLabel = concessionEmployeurLabel;
  concessionSalarieLabel = concessionSalarieLabel;
  rupturePrealableLabel = rupturePrealableLabel;
  verdictValiditeLabel = verdictValiditeLabel;

  /**
   * Bannière verdict — palette navy/or/rouge classique. Rouge réservé
   * `NULLE` (alerte critique uniquement, règle DESIGN_SYSTEM).
   */
  verdictBannerClass(verdict: VerdictValiditeContrat): string {
    switch (verdict) {
      case 'VALIDE': return 'tx-verdict-banner tx-verdict-banner--valid';
      case 'CONTESTABLE': return 'tx-verdict-banner tx-verdict-banner--medium';
      case 'NULLE': return 'tx-verdict-banner tx-verdict-banner--danger';
    }
  }

  verdictIconName(verdict: VerdictValiditeContrat): string {
    switch (verdict) {
      case 'VALIDE': return 'verified';
      case 'CONTESTABLE': return 'gavel';
      case 'NULLE': return 'error';
    }
  }

  verdictChipClass(verdict: VerdictValiditeContrat): string {
    switch (verdict) {
      case 'VALIDE': return 'tx-chip tx-chip--valid';
      case 'CONTESTABLE': return 'tx-chip tx-chip--medium';
      case 'NULLE': return 'tx-chip tx-chip--danger';
    }
  }

  ratioPct(value: number): number {
    // backend renvoie un ratio 0-1 (ex 0.42), on affiche en %.
    if (typeof value !== 'number' || isNaN(value)) return 0;
    return value * 100;
  }

  // ---------------------------------------------------------------------------
  // HTTP
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request = {
      dateSignature: this.dateSignature()!,
      concessionsEmployeur: this.concessionsEmployeur(),
      concessionsSalarie: this.concessionsSalarie(),
      indemniteTransactionnelleEur: this.indemniteTransactionnelleEur()!,
      salaireMensuelBrutEur: this.salaireMensuelBrutEur()!,
      ancienneteAnnees: this.ancienneteAnnees()!,
      renonciationActionExpresse: this.renonciationActionExpresse(),
      delaiReflexion15jOk: this.delaiReflexion15jOk(),
      rupturePrealable: this.rupturePrealable()!,
      presenceAvocatAssistance: this.presenceAvocatAssistance(),
      // Le backend attend strictement la clé avec l'accent (contrat figé
      // SF-DT-31-01) — on garde tel quel dans le payload.
      viceConsentementAllégué: this.viceConsentementAllegue(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Validité de la transaction calculée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
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
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.applyPersistedResult(r);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private applyPersistedResult(r: TransactionResponse): void {
    this.result.set(r);
    this.dateSignature.set(r.dateSignature);
    this.concessionsEmployeur.set(r.concessionsEmployeur ?? []);
    this.concessionsSalarie.set(r.concessionsSalarie ?? []);
    this.indemniteTransactionnelleEur.set(r.indemniteTransactionnelleEur);
    this.salaireMensuelBrutEur.set(r.salaireMensuelBrutEur);
    this.ancienneteAnnees.set(r.ancienneteAnnees);
    this.renonciationActionExpresse.set(!!r.renonciationActionExpresse);
    this.delaiReflexion15jOk.set(!!r.delaiReflexion15jOk);
    this.rupturePrealable.set(r.rupturePrealable);
    this.presenceAvocatAssistance.set(!!r.presenceAvocatAssistance);
    this.viceConsentementAllegue.set(!!r.viceConsentementAllégué);
    // Valeurs persistées = saisie avocat — pas de badge IA.
    this.provenanceSalaire.set(null);
    this.provenanceAnciennete.set(null);
    this.provenanceRupturePrealable.set(null);
    this.showForm.set(false);
  }
}
