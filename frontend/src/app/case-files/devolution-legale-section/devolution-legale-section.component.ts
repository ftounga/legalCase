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
import { DevolutionLegaleService } from '../../core/services/devolution-legale.service';
import {
  DevolutionLegaleRequest,
  DevolutionLegaleResponse,
  HeritierDesigne,
  ModaliteHeritier,
  MODALITE_HERITIER_LABELS,
  OptionConjoint,
  OrdreActif,
  ORDRE_ACTIF_LABELS,
  QualiteHeritier,
  QUALITE_HERITIER_LABELS,
} from '../../core/models/devolution-legale.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { DevolutionLegalePrefillRules } from './devolution-legale-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-FA-24-02 : champs d'alerte F-IA-03 exposés par l'outil "Dévolution
 * légale successorale".
 * - CONJOINT : divergence sur la présence d'un conjoint survivant.
 * - DESCENDANTS_COMMUNS : divergence sur le caractère commun des
 *   descendants (impacte option ¼ pleine propriété vs usufruit total —
 *   art. 757 Cciv).
 */
export type DevolutionLegaleAlertField = 'CONJOINT' | 'DESCENDANTS_COMMUNS';

export type DevolutionLegaleAlertSource = CoherenceAlertSource;
export type DevolutionLegaleCoherenceAlert =
  CoherenceAlert<DevolutionLegaleAlertField>;

/**
 * SF-FA-24-02 : outil décisionnel "Dévolution légale successorale" (FR —
 * art. 731 et s. Cciv : 4 ordres d'héritiers + spécial conjoint survivant
 * + représentation + fente).
 *
 * FR uniquement (bannière info si dossier BE — équivalent CC BE art. 731+
 * traité dans la feature jumelle backlog F-FA-24-BE).
 *
 * Consomme l'API SF-FA-24-01 (mergée PR #651). Affiché conditionnellement
 * par le panel F-IA-04 (tool_id 'F-FA-24-devolution-legale' — migration 179).
 *
 * Pattern de référence : `partage-judiciaire-section` (PR #638).
 * Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 */
@Component({
  selector: 'app-devolution-legale-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './devolution-legale-section.component.html',
  styleUrl: './devolution-legale-section.component.scss',
})
export class DevolutionLegaleSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99d — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-24-devolution-legale';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'DÉVOLUTION LÉGALE SUCCESSORALE (FR)';
  static readonly TOOL_ICON = 'family_restroom';

  /** F-236 SF-236-02 — compteur miroir prefillFromAi via helper. */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return DevolutionLegalePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private aiDataSignal = signal<FamilleExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<DevolutionLegaleResponse | null>(null);

  // Form fields — composition familiale
  conjointSurvivant = signal<boolean | null>(null);
  nbDescendants = signal<number | null>(null);
  tousDescendantsCommunsAvecConjoint = signal<boolean | null>(null);
  nbDescendantsPredecedes = signal<number | null>(0);
  nbPetitsEnfantsParRepresentation = signal<number | null>(0);
  pereVivant = signal<boolean | null>(null);
  mereVivant = signal<boolean | null>(null);
  nbFreresSoeurs = signal<number | null>(null);
  nbFreresSoeursPredecedes = signal<number | null>(0);
  ascendantsOrdinaires = signal<boolean | null>(null);
  collateralOrdinaires = signal<boolean | null>(null);
  optionConjoint = signal<OptionConjoint | null>(null);

  /** Provenance IA des champs pré-remplis. */
  provenanceConjoint = signal<'IA' | null>(null);
  provenanceNbDescendants = signal<'IA' | null>(null);
  provenanceTousCommuns = signal<'IA' | null>(null);
  provenanceNbFreresSoeurs = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Option conjoint requise uniquement en présence de descendants tous communs. */
  optionConjointRequired = computed<boolean>(() =>
    this.conjointSurvivant() === true
    && (this.nbDescendants() ?? 0) > 0
    && this.tousDescendantsCommunsAvecConjoint() === true,
  );

  /** Champ tousCommuns requis uniquement si conjoint + descendants. */
  tousCommunsRequired = computed<boolean>(() =>
    this.conjointSurvivant() === true && (this.nbDescendants() ?? 0) > 0,
  );

  coherenceAlerts = computed<Partial<Record<DevolutionLegaleAlertField, DevolutionLegaleCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<DevolutionLegaleAlertField, DevolutionLegaleCoherenceAlert>> = {};
    const conj = this.buildConjointAlert();
    if (conj) alerts.CONJOINT = conj;
    const dc = this.buildDescendantsCommunsAlert();
    if (dc) alerts.DESCENDANTS_COMMUNS = dc;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  readonly ordreActifOptions = ORDRE_ACTIF_LABELS;

  constructor(
    private service: DevolutionLegaleService,
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

    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  alertTooltip(alert: DevolutionLegaleCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: DevolutionLegaleCoherenceAlert): string {
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
   * Divergence sur la présence du conjoint survivant.
   */
  private buildConjointAlert(): DevolutionLegaleCoherenceAlert | null {
    const userVal = this.conjointSurvivant();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder.forField<DevolutionLegaleAlertField>('CONJOINT');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'DEVOLUTION_LEGALE_CONJOINT') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = this.parseBoolFromIa(ev);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? 'Conjoint survivant' : 'Pas de conjoint',
        reason: `Checklist procédurale : ${expected ? 'conjoint survivant détecté' : 'aucun conjoint survivant'}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'DEVOLUTION_LEGALE_CONJOINT') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue;
      if (!ev) continue;
      const expected = this.parseBoolFromIa(ev);
      if (expected === null || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? 'Conjoint survivant' : 'Pas de conjoint',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.conjointSurvivantDetected`.
    const iaConj = this.aiDataSignal()?.conjointSurvivantDetected;
    if (iaConj !== null && iaConj !== undefined && iaConj !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaConj ? 'Conjoint survivant' : 'Pas de conjoint',
        reason: `Analyse du dossier : ${iaConj ? 'conjoint survivant détecté' : 'aucun conjoint survivant'}`,
      });
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante(['DEVOLUTION_LEGALE_CONJOINT', 'CONJOINT_SURVIVANT']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Divergence sur le caractère "tous communs" des descendants
   * — impacte directement l'option du conjoint (¼ vs usufruit, art. 757).
   */
  private buildDescendantsCommunsAlert(): DevolutionLegaleCoherenceAlert | null {
    const userVal = this.tousDescendantsCommunsAvecConjoint();
    if (userVal === null) return null;

    const builder = CoherenceAlertBuilder.forField<DevolutionLegaleAlertField>('DESCENDANTS_COMMUNS');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'DEVOLUTION_LEGALE_DESCENDANTS_COMMUNS') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = this.parseBoolFromIa(ev);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? 'Descendants tous communs' : 'Famille recomposée',
        reason: `Checklist procédurale : ${expected ? 'descendants tous communs au conjoint' : 'descendants non tous communs (recomposée)'}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'DEVOLUTION_LEGALE_DESCENDANTS_COMMUNS') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue;
      if (!ev) continue;
      const expected = this.parseBoolFromIa(ev);
      if (expected === null || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? 'Descendants tous communs' : 'Famille recomposée',
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.tousDescendantsCommunsAvecConjointDetected`.
    const iaDc = this.aiDataSignal()?.tousDescendantsCommunsAvecConjointDetected;
    if (iaDc !== null && iaDc !== undefined && iaDc !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaDc ? 'Descendants tous communs' : 'Famille recomposée',
        reason: `Analyse du dossier : ${iaDc ? 'descendants tous communs détectés' : 'famille recomposée détectée'}`,
      });
    }

    // 4. PIECE_MANQUANTE
    const piece = this.findPieceManquante([
      'DEVOLUTION_LEGALE_DESCENDANTS_COMMUNS',
      'DESCENDANTS_COMMUNS',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private parseBoolFromIa(value: string | null | undefined): boolean | null {
    if (value === null || value === undefined) return null;
    const v = value.toString().trim().toLowerCase();
    if (!v) return null;
    if (v === 'oui' || v === 'true' || v === 'vrai' || v === '1' || v === 'yes') return true;
    if (v === 'non' || v === 'false' || v === 'faux' || v === '0' || v === 'no') return false;
    return null;
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
    if (this.conjointSurvivant() === null) return false;
    if (this.pereVivant() === null) return false;
    if (this.mereVivant() === null) return false;
    if (this.ascendantsOrdinaires() === null) return false;
    if (this.collateralOrdinaires() === null) return false;

    const nd = this.nbDescendants();
    if (nd === null || nd === undefined || nd < 0) return false;

    const ndp = this.nbDescendantsPredecedes();
    if (ndp === null || ndp === undefined || ndp < 0) return false;

    const npe = this.nbPetitsEnfantsParRepresentation();
    if (npe === null || npe === undefined || npe < 0) return false;

    const nfs = this.nbFreresSoeurs();
    if (nfs === null || nfs === undefined || nfs < 0) return false;

    const nfsp = this.nbFreresSoeursPredecedes();
    if (nfsp === null || nfsp === undefined || nfsp < 0) return false;
    if (nfsp > nfs) return false;

    // Si conjoint + descendants → tousCommuns requis
    if (this.tousCommunsRequired() && this.tousDescendantsCommunsAvecConjoint() === null) {
      return false;
    }

    // Si conjoint + descendants tous communs → optionConjoint requise
    if (this.optionConjointRequired() && this.optionConjoint() === null) {
      return false;
    }

    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers — effacent provenance IA au 1er changement manuel.
  onConjointSurvivantChange(value: boolean | null): void {
    this.conjointSurvivant.set(value);
    this.provenanceConjoint.set(null);
    // Si l'avocat retire le conjoint, on reset les options qui en dépendent.
    if (value !== true) {
      this.tousDescendantsCommunsAvecConjoint.set(null);
      this.optionConjoint.set(null);
      this.provenanceTousCommuns.set(null);
    }
  }

  onNbDescendantsChange(value: number | null): void {
    this.nbDescendants.set(value === null || value === undefined ? null : value);
    this.provenanceNbDescendants.set(null);
    if ((value ?? 0) === 0) {
      this.tousDescendantsCommunsAvecConjoint.set(null);
      this.optionConjoint.set(null);
      this.provenanceTousCommuns.set(null);
      this.nbDescendantsPredecedes.set(0);
      this.nbPetitsEnfantsParRepresentation.set(0);
    }
  }

  onTousCommunsChange(value: boolean | null): void {
    this.tousDescendantsCommunsAvecConjoint.set(value);
    this.provenanceTousCommuns.set(null);
    if (value !== true) {
      this.optionConjoint.set(null);
    }
  }

  onNbDescendantsPredecedesChange(value: number | null): void {
    this.nbDescendantsPredecedes.set(value === null || value === undefined ? null : value);
  }

  onNbPetitsEnfantsParRepresentationChange(value: number | null): void {
    this.nbPetitsEnfantsParRepresentation.set(value === null || value === undefined ? null : value);
  }

  onPereVivantChange(value: boolean | null): void {
    this.pereVivant.set(value);
  }

  onMereVivanteChange(value: boolean | null): void {
    this.mereVivant.set(value);
  }

  onNbFreresSoeursChange(value: number | null): void {
    this.nbFreresSoeurs.set(value === null || value === undefined ? null : value);
    this.provenanceNbFreresSoeurs.set(null);
    if ((value ?? 0) === 0) {
      this.nbFreresSoeursPredecedes.set(0);
    }
  }

  onNbFreresSoeursPredecedesChange(value: number | null): void {
    this.nbFreresSoeursPredecedes.set(value === null || value === undefined ? null : value);
  }

  onAscendantsOrdinairesChange(value: boolean | null): void {
    this.ascendantsOrdinaires.set(value);
  }

  onCollateralOrdinairesChange(value: boolean | null): void {
    this.collateralOrdinaires.set(value);
  }

  onOptionConjointChange(value: OptionConjoint | null): void {
    this.optionConjoint.set(value);
  }

  /**
   * Pré-fill depuis `aiData` (FamilleExtractedData).
   * Règles :
   * - silencieux si aiData absent
   * - ne pré-remplit que si le champ est encore vide (`null`) ou marqué IA
   * - n'écrase jamais une saisie avocat (provenance !== 'IA')
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé.
    const h = { aiData: this.aiDataSignal() };
    const cj = DevolutionLegalePrefillRules.computeConjointSurvivant(h);
    if (cj !== null && (this.conjointSurvivant() === null || this.provenanceConjoint() === 'IA')) {
      this.conjointSurvivant.set(cj);
      this.provenanceConjoint.set('IA');
    }
    const nd = DevolutionLegalePrefillRules.computeNbDescendants(h);
    if (nd !== null && (this.nbDescendants() === null || this.provenanceNbDescendants() === 'IA')) {
      this.nbDescendants.set(nd);
      this.provenanceNbDescendants.set('IA');
    }
    const dc = DevolutionLegalePrefillRules.computeTousDescendantsCommuns(h);
    if (dc !== null && (this.tousDescendantsCommunsAvecConjoint() === null || this.provenanceTousCommuns() === 'IA')) {
      this.tousDescendantsCommunsAvecConjoint.set(dc);
      this.provenanceTousCommuns.set('IA');
    }
    const fs = DevolutionLegalePrefillRules.computeNbFreresSoeurs(h);
    if (fs !== null && (this.nbFreresSoeurs() === null || this.provenanceNbFreresSoeurs() === 'IA')) {
      this.nbFreresSoeurs.set(fs);
      this.provenanceNbFreresSoeurs.set('IA');
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: DevolutionLegaleRequest = {
      conjointSurvivant: this.conjointSurvivant()!,
      nbDescendants: this.nbDescendants()!,
      tousDescendantsCommunsAvecConjoint: this.tousDescendantsCommunsAvecConjoint(),
      nbDescendantsPredecedes: this.nbDescendantsPredecedes() ?? 0,
      nbPetitsEnfantsParRepresentation: this.nbPetitsEnfantsParRepresentation() ?? 0,
      pereVivant: this.pereVivant()!,
      mereVivant: this.mereVivant()!,
      nbFreresSoeurs: this.nbFreresSoeurs()!,
      nbFreresSoeursPredecedes: this.nbFreresSoeursPredecedes() ?? 0,
      ascendantsOrdinaires: this.ascendantsOrdinaires()!,
      collateralOrdinaires: this.collateralOrdinaires()!,
      optionConjoint: this.optionConjoint(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Dévolution légale analysée', 'OK', { duration: 2500 });
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
        this.result.set(r);
        // Provenance reset — valeurs persistées = saisie avocat.
        this.provenanceConjoint.set(null);
        this.provenanceNbDescendants.set(null);
        this.provenanceTousCommuns.set(null);
        this.provenanceNbFreresSoeurs.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  /** Libellé humain de l'ordre actif. */
  ordreLabel(code: OrdreActif | null | undefined): string {
    if (!code) return '';
    const opt = ORDRE_ACTIF_LABELS.find((o) => o.code === code);
    return opt?.label ?? code;
  }

  /** Numéro d'ordre pour le chip indicateur graphique. */
  ordreNumero(code: OrdreActif | null | undefined): string {
    if (!code) return '?';
    const opt = ORDRE_ACTIF_LABELS.find((o) => o.code === code);
    return opt?.numero ?? '?';
  }

  /** Libellé humain d'une qualité d'héritier. */
  qualiteLabel(code: QualiteHeritier): string {
    return QUALITE_HERITIER_LABELS[code] ?? code;
  }

  /** Libellé humain d'une modalité. */
  modaliteLabel(code: ModaliteHeritier | null | undefined): string {
    if (!code) return '';
    return MODALITE_HERITIER_LABELS[code] ?? code;
  }

  /** Classe CSS du chip d'ordre actif (déshérence = critique). */
  ordreChipClass(code: OrdreActif | null | undefined): string {
    if (code === 'DESHERENCE') return 'devolution-leg-ordre-chip devolution-leg-ordre-chip--critical';
    return 'devolution-leg-ordre-chip devolution-leg-ordre-chip--info';
  }

  /** TrackBy pour la liste des héritiers. */
  trackHeritier(index: number, h: HeritierDesigne): string {
    return `${h.qualite}-${index}-${h.quotePartPct}`;
  }
}
