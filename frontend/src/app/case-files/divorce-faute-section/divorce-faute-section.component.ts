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
import { DivorceFauteService } from '../../core/services/divorce-faute.service';
import {
  DivorceFauteResponse,
  FAUTES_FR,
  FauteCode,
  FauteOption,
  fauteLabel,
  verdictProbabiliteLabel,
  verdictTortsLabel,
  VerdictProbabilite,
  VerdictTorts,
} from '../../core/models/divorce-faute.model';
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

/**
 * SF-FA-09-02 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-FA-09 (divorce pour faute, FR uniquement, art. 242 Cciv).
 */
export type DivorceFauteAlertField =
  | 'REVENUS_DEMANDEUR'
  | 'REVENUS_DEFENDEUR'
  | 'DUREE_MARIAGE'
  | 'DATE_DEPOT_ASSIGNATION'
  | 'FAUTES_INVOQUEES';

// SF-155-10 : alias locaux rétro-compat — utilise l'interface générique partagée.
export type DivorceFauteAlertSource = CoherenceAlertSource;
export type DivorceFauteCoherenceAlert = CoherenceAlert<DivorceFauteAlertField>;

/**
 * Seuil d'écart relatif (10 %) au-delà duquel on déclenche une alerte
 * de divergence sur les revenus annuels — aligné F-DT-09.
 */
const REVENUS_DIVERGENCE_RATIO = 0.10;

/** Écart absolu (en années) pour la durée de mariage. */
const DUREE_MARIAGE_DIVERGENCE_YEARS = 1;

/** Regex ISO date YYYY-MM-DD (pour la date dépôt assignation). */
const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

const VALID_FAUTE_CODES: ReadonlySet<string> = new Set<FauteCode>([
  'ADULTERE',
  'VIOLENCES',
  'ABANDON',
  'OUTRAGES',
  'DEVOIR_ASSISTANCE',
  'DEVOIR_FIDELITE',
  'DEVOIR_COMMUNAUTE_VIE',
  'AUTRE',
]);

/**
 * SF-FA-09-02 : outil décisionnel "Divorce pour faute" (FR uniquement).
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `F-FA-09-divorce-faute`). Consomme l'API SF-FA-09-01.
 *
 * SF-155-10 : harmonisation F-IA-03 — interface locale `DivorceFauteCoherenceAlert`
 * remplacée par `CoherenceAlert<DivorceFauteAlertField>` partagée, alertes
 * construites via `CoherenceAlertBuilder` (multi-sources F96 / QUESTION_IA /
 * IA / PIECE_MANQUANTE + popover F-IA-03-15b). Pattern de référence canonique :
 * `harcelement-licenciement-nul-section` (cf. `ai-skills/frontend-coherence-audit.md`
 * §5), miroir `divorce-accepte-section` (F-FA-10).
 */
@Component({
  selector: 'app-divorce-faute-section',
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
  templateUrl: './divorce-faute-section.component.html',
  styleUrl: './divorce-faute-section.component.scss',
})
export class DivorceFauteSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'DIVORCE POUR FAUTE (FR) — ART. 242 CCIV';
  static readonly TOOL_ICON = 'gavel';

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // SF-FA-09-02 : pré-fill IA (tous optionnels — null-safe partout).
  @Input() aiData?: TravailExtractedData | null;
  // SF-155-10 : inputs multi-sources F-IA-03 (F96 / QUESTION_IA / PIECE_MANQUANTE).
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal pour que les `computed` réagissent aux changements d'input.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<DivorceFauteResponse | null>(null);

  // Form state (signals pour cohérence avec le pattern canonique).
  fautesInvoquees = signal<FauteCode[]>([]);
  preuvesDocumentaires = signal<boolean>(false);
  tortsAdverseInvoques = signal<boolean>(false);
  dureeMariageAnnees = signal<number | null>(null);
  revenusAnnuelsDemandeurEur = signal<number | null>(null);
  revenusAnnuelsDefendeurEur = signal<number | null>(null);
  dateDepotAssignation = signal<string | null>(null);

  // Provenance IA par champ.
  provenanceFautesInvoquees = signal<'IA' | null>(null);
  provenanceDureeMariage = signal<'IA' | null>(null);
  provenanceRevenusDemandeur = signal<'IA' | null>(null);
  provenanceRevenusDefendeur = signal<'IA' | null>(null);
  provenanceDateDepot = signal<'IA' | null>(null);

  // SF-IA-03-15c : map {sourceKey → explanations} pour le popover F-IA-03.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  readonly fautesOptions: FauteOption[] = FAUTES_FR;

  // SF-FA-09-02 / SF-155-10 : alertes de cohérence calculées dynamiquement via
  // le builder partagé. Gate strict `showForm()` (pattern anti-bug SF-IA-03-12).
  coherenceAlerts = computed<Partial<Record<DivorceFauteAlertField, DivorceFauteCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<DivorceFauteAlertField, DivorceFauteCoherenceAlert>> = {};
    const dem = this.buildRevenusAlert('REVENUS_DEMANDEUR');
    if (dem) alerts.REVENUS_DEMANDEUR = dem;
    const def = this.buildRevenusAlert('REVENUS_DEFENDEUR');
    if (def) alerts.REVENUS_DEFENDEUR = def;
    const duree = this.buildDureeMariageAlert();
    if (duree) alerts.DUREE_MARIAGE = duree;
    const date = this.buildDateDepotAssignationAlert();
    if (date) alerts.DATE_DEPOT_ASSIGNATION = date;
    const fautes = this.buildFautesAlert();
    if (fautes) alerts.FAUTES_INVOQUEES = fautes;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  isFrance = computed(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private service: DivorceFauteService,
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
    // l'avocat a déjà saisi manuellement (provenance null sur un champ rempli)
    // ou si un résultat persisté est présent (form masqué).
    if (changes['aiData'] && !changes['aiData'].firstChange && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * SF-FA-09-02 : pré-remplit le form depuis `aiData`. N'écrase jamais une
   * saisie avocat existante (la garde est faite par champ — provenance IA OK
   * ou champ encore vide).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // 1. Durée mariage (entier > 0).
    const ageMariage = (ai as { dureeMariageAnnees?: number | null }).dureeMariageAnnees;
    if (typeof ageMariage === 'number' && ageMariage > 0) {
      if (this.dureeMariageAnnees() === null || this.provenanceDureeMariage() === 'IA') {
        this.dureeMariageAnnees.set(ageMariage);
        this.provenanceDureeMariage.set('IA');
      }
    }

    // 2. Revenus annuels demandeur (> 0).
    const revDem = (ai as { revenusAnnuelsDemandeurEur?: number | null }).revenusAnnuelsDemandeurEur;
    if (typeof revDem === 'number' && revDem > 0) {
      if (this.revenusAnnuelsDemandeurEur() === null || this.provenanceRevenusDemandeur() === 'IA') {
        this.revenusAnnuelsDemandeurEur.set(revDem);
        this.provenanceRevenusDemandeur.set('IA');
      }
    }

    // 3. Revenus annuels défendeur (> 0).
    const revDef = (ai as { revenusAnnuelsDefendeurEur?: number | null }).revenusAnnuelsDefendeurEur;
    if (typeof revDef === 'number' && revDef > 0) {
      if (this.revenusAnnuelsDefendeurEur() === null || this.provenanceRevenusDefendeur() === 'IA') {
        this.revenusAnnuelsDefendeurEur.set(revDef);
        this.provenanceRevenusDefendeur.set('IA');
      }
    }

    // 4. Date dépôt assignation (ISO YYYY-MM-DD).
    const dateDepot = (ai as { dateDepotAssignation?: string | null }).dateDepotAssignation;
    if (typeof dateDepot === 'string' && dateDepot.length > 0) {
      if (this.dateDepotAssignation() === null || this.provenanceDateDepot() === 'IA') {
        this.dateDepotAssignation.set(dateDepot);
        this.provenanceDateDepot.set('IA');
      }
    }

    // 5. Fautes détectées par pipeline IA — no-op gracieux si absent.
    const fautesIa = ai.fautesDetectees;
    if (Array.isArray(fautesIa) && fautesIa.length > 0) {
      const filtered = fautesIa
        .map((f) => f?.toUpperCase())
        .filter((f): f is FauteCode => !!f && VALID_FAUTE_CODES.has(f));
      if (filtered.length > 0
          && (this.fautesInvoquees().length === 0 || this.provenanceFautesInvoquees() === 'IA')) {
        this.fautesInvoquees.set(filtered);
        this.provenanceFautesInvoquees.set('IA');
      }
    }
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /**
   * SF-IA-03-15c : mapping field → sourceKey pour le popover F-IA-03.
   */
  explanationFor(field: DivorceFauteAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'REVENUS_DEMANDEUR': return 'FA09_REVENUS_DEMANDEUR';
        case 'REVENUS_DEFENDEUR': return 'FA09_REVENUS_DEFENDEUR';
        case 'DUREE_MARIAGE': return 'FA09_DUREE_MARIAGE';
        case 'DATE_DEPOT_ASSIGNATION': return 'FA09_DATE_DEPOT_ASSIGNATION';
        case 'FAUTES_INVOQUEES': return 'FA09_FAUTES_INVOQUEES';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: DivorceFauteCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: DivorceFauteCoherenceAlert): string {
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
  // Builders d'alertes F-IA-03 (un par field) — SF-155-10 via CoherenceAlertBuilder.
  // ---------------------------------------------------------------------------

  /**
   * SF-155-10 : revenus (demandeur/défendeur) — écart relatif > 10 % IA vs user.
   * Enrichissement multi-sources : F96 (critère `FA09_REVENUS_*`), QUESTION_IA
   * (réponse "oui" avec `expectedValue`), PIECE_MANQUANTE (avis d'imposition).
   */
  private buildRevenusAlert(field: 'REVENUS_DEMANDEUR' | 'REVENUS_DEFENDEUR'): DivorceFauteCoherenceAlert | null {
    const ai = this.aiDataSignal() as
      | { revenusAnnuelsDemandeurEur?: number | null; revenusAnnuelsDefendeurEur?: number | null }
      | null
      | undefined;
    const aiValue = field === 'REVENUS_DEMANDEUR'
      ? ai?.revenusAnnuelsDemandeurEur
      : ai?.revenusAnnuelsDefendeurEur;
    const userValue = field === 'REVENUS_DEMANDEUR'
      ? this.revenusAnnuelsDemandeurEur()
      : this.revenusAnnuelsDefendeurEur();
    if (typeof aiValue !== 'number' || aiValue <= 0) return null;
    if (typeof userValue !== 'number' || userValue <= 0) return null;
    const ratio = Math.abs(userValue - aiValue) / aiValue;
    if (ratio <= REVENUS_DIVERGENCE_RATIO) return null;

    const subject = field === 'REVENUS_DEMANDEUR' ? 'demandeur' : 'défendeur';
    const display = `${aiValue.toLocaleString('fr-FR')} €`;
    const critereCode = field === 'REVENUS_DEMANDEUR'
      ? 'FA09_REVENUS_DEMANDEUR'
      : 'FA09_REVENUS_DEFENDEUR';
    const pieceCodes = field === 'REVENUS_DEMANDEUR'
      ? ['FA09_REVENUS_DEMANDEUR', 'AVIS_IMPOSITION_DEMANDEUR']
      : ['FA09_REVENUS_DEFENDEUR', 'AVIS_IMPOSITION_DEFENDEUR'];

    const builder = CoherenceAlertBuilder.forField<DivorceFauteAlertField>(field)
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : revenus annuels ${subject} ~${display}`,
      });

    // F-96 checklist procédurale.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== critereCode) continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : revenus ${subject} attendus ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    // QUESTION_IA — réponse "oui" sur `FA09_REVENUS_*` avec expectedValue.
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== critereCode) continue;
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

    // PIECE_MANQUANTE (avis d'imposition).
    const piece = this.findPieceManquante(pieceCodes);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * SF-155-10 : durée mariage — écart absolu > 1 an IA vs user.
   * Multi-sources : F96 / QUESTION_IA / PIECE_MANQUANTE (acte mariage).
   */
  private buildDureeMariageAlert(): DivorceFauteCoherenceAlert | null {
    const ai = this.aiDataSignal() as { dureeMariageAnnees?: number | null } | null | undefined;
    const aiValue = ai?.dureeMariageAnnees;
    const userValue = this.dureeMariageAnnees();
    if (typeof aiValue !== 'number' || aiValue <= 0) return null;
    if (typeof userValue !== 'number' || userValue < 0) return null;
    if (Math.abs(userValue - aiValue) <= DUREE_MARIAGE_DIVERGENCE_YEARS) return null;

    const display = `${aiValue} an(s)`;
    const builder = CoherenceAlertBuilder.forField<DivorceFauteAlertField>('DUREE_MARIAGE')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : durée du mariage ~${display}`,
      });

    // F-96 checklist.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA09_DUREE_MARIAGE') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : durée mariage attendue ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA09_DUREE_MARIAGE', 'ACTE_MARIAGE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * SF-155-10 : date dépôt assignation — divergence stricte sur la chaîne ISO.
   * Multi-sources : F96 + PIECE_MANQUANTE (copie assignation).
   */
  private buildDateDepotAssignationAlert(): DivorceFauteCoherenceAlert | null {
    const ai = this.aiDataSignal() as { dateDepotAssignation?: string | null } | null | undefined;
    const aiDate = ai?.dateDepotAssignation;
    const user = this.dateDepotAssignation();
    if (!user) return null;
    if (!aiDate || !ISO_DATE_REGEX.test(aiDate)) return null;
    if (user === aiDate) return null;

    const builder = CoherenceAlertBuilder.forField<DivorceFauteAlertField>('DATE_DEPOT_ASSIGNATION')
      .addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : assignation déposée le ${aiDate}`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA09_DATE_DEPOT_ASSIGNATION') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: aiDate,
        reason: `Checklist procédurale : date dépôt assignation attendue ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA09_DATE_DEPOT_ASSIGNATION', 'COPIE_ASSIGNATION']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * SF-155-10 : fautes invoquées — divergence ensembliste IA vs user.
   * Multi-sources : F96 (critère `FA09_FAUTES_INVOQUEES`) + PIECE_MANQUANTE
   * (témoignages, constats huissier, mains courantes).
   */
  private buildFautesAlert(): DivorceFauteCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiList = ai?.fautesDetectees;
    if (!Array.isArray(aiList) || aiList.length === 0) return null;
    const aiSet = new Set(aiList.map((f) => f?.toUpperCase()).filter((f): f is string => !!f));
    if (aiSet.size === 0) return null;
    const userSet = new Set(this.fautesInvoquees());

    // Si avocat n'a sélectionné aucune faute alors que l'IA en a détecté.
    let display: string;
    let iaReason: string;
    if (userSet.size === 0) {
      const sample = Array.from(aiSet).slice(0, 3).join(', ');
      display = sample;
      iaReason = `Analyse du dossier : fautes détectées (${sample})`;
    } else {
      // Divergence symétrique : ensembles différents.
      const same = aiSet.size === userSet.size && Array.from(aiSet).every((f) => userSet.has(f as FauteCode));
      if (same) return null;
      const sample = Array.from(aiSet).slice(0, 3).join(', ');
      display = sample;
      iaReason = `Analyse du dossier : fautes détectées (${sample}) — divergence avec votre saisie`;
    }

    const builder = CoherenceAlertBuilder.forField<DivorceFauteAlertField>('FAUTES_INVOQUEES')
      .addSource('IA', { expectedDisplay: display, reason: iaReason });

    // F-96 checklist.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA09_FAUTES_INVOQUEES') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : fautes attendues ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante([
      'FA09_FAUTES_INVOQUEES',
      'CONSTAT_HUISSIER',
      'TEMOIGNAGES',
      'MAIN_COURANTE',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * SF-155-10 : renvoie le `texte` d'une `PieceManquanteEntry` dont `critereCode`
   * (case-insensitive) appartient à la liste des codes acceptés, ou `null` si
   * aucune pièce ne matche. La première pièce trouvée gagne.
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
    const fautes = this.fautesInvoquees();
    const duree = this.dureeMariageAnnees();
    const revDem = this.revenusAnnuelsDemandeurEur();
    const revDef = this.revenusAnnuelsDefendeurEur();
    return fautes.length > 0
      && duree !== null && duree >= 0
      && revDem !== null && revDem >= 0
      && revDef !== null && revDef >= 0;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onFautesChange(value: FauteCode[]): void {
    this.fautesInvoquees.set(value ?? []);
    this.provenanceFautesInvoquees.set(null);
  }

  onPreuvesChange(value: boolean): void {
    this.preuvesDocumentaires.set(!!value);
  }

  onTortsAdverseChange(value: boolean): void {
    this.tortsAdverseInvoques.set(!!value);
  }

  onDureeMariageChange(value: number | null): void {
    this.dureeMariageAnnees.set(value);
    this.provenanceDureeMariage.set(null);
  }

  onRevenusDemandeurChange(value: number | null): void {
    this.revenusAnnuelsDemandeurEur.set(value);
    this.provenanceRevenusDemandeur.set(null);
  }

  onRevenusDefendeurChange(value: number | null): void {
    this.revenusAnnuelsDefendeurEur.set(value);
    this.provenanceRevenusDefendeur.set(null);
  }

  onDateDepotChange(value: string | null): void {
    this.dateDepotAssignation.set(value && value.length > 0 ? value : null);
    this.provenanceDateDepot.set(null);
  }

  // ---------------------------------------------------------------------------
  // Display helpers
  // ---------------------------------------------------------------------------

  fauteLabel = fauteLabel;
  verdictProbabiliteLabel = verdictProbabiliteLabel;
  verdictTortsLabel = verdictTortsLabel;

  verdictBannerClass(verdict: VerdictProbabilite): string {
    switch (verdict) {
      case 'ELEVEE': return 'df-verdict-banner df-verdict-banner--strong';
      case 'MOYENNE': return 'df-verdict-banner df-verdict-banner--medium';
      case 'FAIBLE': return 'df-verdict-banner df-verdict-banner--weak';
    }
  }

  verdictTortsClass(verdict: VerdictTorts): string {
    switch (verdict) {
      case 'EXCLUSIF_DEFENDEUR': return 'df-tort-card df-tort-card--exclusif';
      case 'PARTAGES': return 'df-tort-card df-tort-card--partages';
      case 'IMPREDICTIBLE': return 'df-tort-card df-tort-card--impredictible';
    }
  }

  // ---------------------------------------------------------------------------
  // HTTP
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request = {
      fautesInvoquees: this.fautesInvoquees(),
      preuvesDocumentaires: this.preuvesDocumentaires(),
      tortsAdverseInvoques: this.tortsAdverseInvoques(),
      dureeMariageAnnees: this.dureeMariageAnnees()!,
      revenusAnnuelsDemandeurEur: this.revenusAnnuelsDemandeurEur()!,
      revenusAnnuelsDefendeurEur: this.revenusAnnuelsDefendeurEur()!,
      dateDepotAssignation: this.dateDepotAssignation(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse divorce pour faute calculée', 'OK', { duration: 2500 });
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

  private applyPersistedResult(r: DivorceFauteResponse): void {
    this.result.set(r);
    this.fautesInvoquees.set(r.fautesInvoquees ?? []);
    this.preuvesDocumentaires.set(!!r.preuvesDocumentaires);
    this.tortsAdverseInvoques.set(!!r.tortsAdverseInvoques);
    this.dureeMariageAnnees.set(r.dureeMariageAnnees);
    this.revenusAnnuelsDemandeurEur.set(r.revenusAnnuelsDemandeurEur);
    this.revenusAnnuelsDefendeurEur.set(r.revenusAnnuelsDefendeurEur);
    this.dateDepotAssignation.set(r.dateDepotAssignation);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceFautesInvoquees.set(null);
    this.provenanceDureeMariage.set(null);
    this.provenanceRevenusDemandeur.set(null);
    this.provenanceRevenusDefendeur.set(null);
    this.provenanceDateDepot.set(null);
    this.showForm.set(false);
  }
}
