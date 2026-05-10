import {
  Component,
  Input,
  OnInit,
  OnChanges,
  SimpleChanges,
  Optional,
  computed,
  signal,
} from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RuptureConvIndemniteService } from '../../core/services/rupture-conv-indemnite.service';
import { RuptureConvIndemniteResponse } from '../../core/models/rupture-conv-indemnite.model';
import {
  CaseAnalysisResult,
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import {
  RuptureConvIndemniteSectionPrefillRules,
} from './rupture-conv-indemnite-section-prefill-rules';

/**
 * SF-155-12 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-132 (indemnité rupture conventionnelle). Un par field pré-remplissable.
 */
export type RCIAlertField = 'SALAIRE' | 'ANCIENNETE';

// SF-155-12 : alias locaux rétro-compat — utilise l'interface générique partagée.
export type RCIAlertSource = CoherenceAlertSource;
export type RCICoherenceAlert = CoherenceAlert<RCIAlertField>;

/**
 * SF-155-12 : seuil d'écart relatif au-delà duquel on affiche une alerte
 * de cohérence entre la valeur IA et la saisie de l'avocat.
 * Aligné avec HLN (F-DT-11) et F-DT-09.
 */
const SALAIRE_DIVERGENCE_RATIO = 0.10;
const ANCIENNETE_DIVERGENCE_YEARS = 1; // tolérance ≥ 1 an pour déclencher alerte

/**
 * SF-132-02 : outil décisionnel dédié "Indemnité rupture conventionnelle" (FR).
 * Affiché conditionnellement par case-file-detail quand
 * compensationEstimate.typeRupture == RUPTURE_CONVENTIONNELLE && country == FRANCE.
 *
 * SF-155-12 : mise en conformité CLAUDE.md — pré-fill IA (badges
 * "Pré-rempli depuis l'analyse") + validation F-IA-03 (popover 4-sources
 * IA / F96 / Question IA / Pièce manquante). Pattern emprunté à
 * `harcelement-licenciement-nul-section` (F-DT-11 SF-155-04-A1).
 */
@Component({
  selector: 'app-rupture-conv-indemnite-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatProgressSpinnerModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './rupture-conv-indemnite-section.component.html',
  styleUrl: './rupture-conv-indemnite-section.component.scss'
})
export class RuptureConvIndemniteSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'INDEMNITÉ RUPTURE CONVENTIONNELLE';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: {
    aiData?: any;
    synthesis?: any;
    procedureChecks?: any[];
    aiQuestions?: any[];
    piecesManquantes?: any[];
    triggerEvents?: any[];
    workspaceCountry?: string;
  }): number {
    return RuptureConvIndemniteSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      synthesis: input.synthesis,
    });
  }

  static readonly TOOL_ICON = 'euro_symbol';

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  /** Rétro-compat SF-132-02 : fallback `compensationEstimate.ancienneteAnnees/salaireReference`. */
  @Input() synthesis?: CaseAnalysisResult | null;
  // SF-155-12 : inputs IA (tous optionnels — null-safe partout).
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // SF-155-12 : snapshots signal des inputs IA pour que `computed` réagisse.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);
  private synthesisSignal = signal<CaseAnalysisResult | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<RuptureConvIndemniteResponse | null>(null);

  ancienneteAnnees = signal<number | null>(null);
  salaireMensuel = signal<number | null>(null);

  // SF-155-12 : provenance IA par champ. 'IA' quand le champ a été pré-rempli
  // par `prefillFromAi()` ; remis à null dès que l'avocat modifie (onXxxChange).
  provenanceAncienneteAnnees = signal<'IA' | null>(null);
  provenanceSalaireMensuel = signal<'IA' | null>(null);

  // SF-IA-03-15c : map {sourceKey → explanations} pour le popover.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  // SF-155-12 : alertes de cohérence F-IA-03 calculées dynamiquement.
  // Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12).
  coherenceAlerts = computed<Partial<Record<RCIAlertField, RCICoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<RCIAlertField, RCICoherenceAlert>> = {};
    const salaireAlert = this.buildSalaireAlert();
    if (salaireAlert) alerts.SALAIRE = salaireAlert;
    const ancienneteAlert = this.buildAncienneteAlert();
    if (ancienneteAlert) alerts.ANCIENNETE = ancienneteAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: RuptureConvIndemniteService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);

    // SF-155-12 : push inputs vers signals avant toute évaluation computed.
    this.aiDataSignal.set(this.aiData);
    this.synthesisSignal.set(this.synthesis);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    // SF-155-12 : pré-fill au mount si aiData/synthesis déjà disponibles
    // (pipeline IA déjà tourné avant ouverture du dossier).
    this.prefillFromAi();
    this.load();
    this.loadSourceExplanations();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    // SF-155-12 : ré-hydrater les signals à chaque changement d'input.
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['synthesis']) this.synthesisSignal.set(this.synthesis);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // SF-155-12 : ré-appliquer le pré-fill quand `aiData` ou `synthesis` change
    // avant première résolution backend. Ne PAS écraser si l'avocat a déjà saisi
    // manuellement (provenance devenue null) OU si un résultat persisté est
    // présent (form masqué).
    const sourceChanged = (changes['aiData'] && !changes['aiData'].firstChange)
      || (changes['synthesis'] && !changes['synthesis'].firstChange);
    if (sourceChanged && this.showForm() && !this.result()) {
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

  /**
   * SF-155-12 : pré-remplit le form depuis `aiData` (salaire brut mensuel)
   * et `synthesis.compensationEstimate` (ancienneté + salaire référence en
   * fallback). N'écrase jamais une saisie avocat existante.
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 : valeurs calculées par le helper partagé (parité static).
    const ruleInput = {
      aiData: this.aiDataSignal(),
      synthesis: this.synthesisSignal(),
    };

    const salaireSource = RuptureConvIndemniteSectionPrefillRules.computeSalaireMensuel(ruleInput);
    if (salaireSource !== null
        && (this.salaireMensuel() === null || this.provenanceSalaireMensuel() === 'IA')) {
      this.salaireMensuel.set(salaireSource);
      this.provenanceSalaireMensuel.set('IA');
    }

    const annees = RuptureConvIndemniteSectionPrefillRules.computeAncienneteAnnees(ruleInput);
    if (annees !== null
        && (this.ancienneteAnnees() === null || this.provenanceAncienneteAnnees() === 'IA')) {
      this.ancienneteAnnees.set(annees);
      this.provenanceAncienneteAnnees.set('IA');
    }
  }

  /**
   * SF-IA-03-15c : mapping field → sourceKey pour le popover.
   */
  explanationFor(field: RCIAlertField): SourceExplanation[] {
    const key = field === 'SALAIRE' ? 'salaire_brut_mensuel' : 'RCI_ANCIENNETE';
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: RCICoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: RCICoherenceAlert): string {
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
   * SF-155-12 : divergence salaire — écart relatif > 10 %
   * (SALAIRE_DIVERGENCE_RATIO) entre valeur IA et saisie avocat. Agrège
   * F96 (critereCode `RCI_SALAIRE`), QUESTION_IA et PIECE_MANQUANTE.
   */
  private buildSalaireAlert(): RCICoherenceAlert | null {
    const aiSalaire = this.aiDataSignal()?.salaireBrutMensuel;
    const userSalaire = this.salaireMensuel();
    if (typeof aiSalaire !== 'number' || aiSalaire <= 0) return null;
    if (typeof userSalaire !== 'number' || userSalaire <= 0) return null;
    const ratio = Math.abs(userSalaire - aiSalaire) / aiSalaire;
    if (ratio <= SALAIRE_DIVERGENCE_RATIO) return null;

    const builder = CoherenceAlertBuilder.forField<RCIAlertField>('SALAIRE');

    // 1. F-96 VERIFIED/NON_COMPLIANT — critereCode `RCI_SALAIRE`.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'RCI_SALAIRE') continue;
      const ev = this.parseNumber(chk.expectedValue);
      if (ev === null || ev <= 0) continue;
      if (Math.abs(ev - userSalaire) / Math.max(ev, 1) <= SALAIRE_DIVERGENCE_RATIO) continue;
      builder.addSource('F96', {
        expectedDisplay: `${ev.toLocaleString('fr-FR')} €`,
        reason: `Checklist procédurale : salaire ${ev.toLocaleString('fr-FR')} €${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    // 2. QUESTION_IA — réponse "oui" sur `RCI_SALAIRE` avec `expectedValue`.
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'RCI_SALAIRE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = this.parseNumber(q.expectedValue);
      if (ev === null || ev <= 0) continue;
      if (Math.abs(ev - userSalaire) / Math.max(ev, 1) <= SALAIRE_DIVERGENCE_RATIO) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: `${ev.toLocaleString('fr-FR')} €`,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — salaire brut mensuel détecté (divergence déjà qualifiée au-dessus).
    builder.addSource('IA', {
      expectedDisplay: `${aiSalaire.toLocaleString('fr-FR')} €`,
      reason: `Analyse du dossier : salaire brut mensuel ~${aiSalaire.toLocaleString('fr-FR')} €`,
    });

    // 4. PIECE_MANQUANTE (fiche de paie).
    const piece = this.findPieceManquante(['RCI_SALAIRE', 'SALAIRE_BRUT_MENSUEL']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * SF-155-12 : divergence ancienneté — écart ≥ 1 an entre la valeur IA
   * (compensationEstimate.ancienneteAnnees) et la saisie avocat. Agrège
   * F96 (critereCode `RCI_ANCIENNETE`), QUESTION_IA et PIECE_MANQUANTE.
   */
  private buildAncienneteAlert(): RCICoherenceAlert | null {
    const iaAnciennete = this.synthesisSignal()?.compensationEstimate?.ancienneteAnnees;
    const userAnciennete = this.ancienneteAnnees();
    if (typeof iaAnciennete !== 'number' || iaAnciennete < 0) return null;
    if (typeof userAnciennete !== 'number' || userAnciennete < 0) return null;
    if (Math.abs(userAnciennete - iaAnciennete) < ANCIENNETE_DIVERGENCE_YEARS) return null;

    const builder = CoherenceAlertBuilder.forField<RCIAlertField>('ANCIENNETE');

    // 1. F-96 VERIFIED/NON_COMPLIANT — critereCode `RCI_ANCIENNETE`.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'RCI_ANCIENNETE') continue;
      const ev = this.parseNumber(chk.expectedValue);
      if (ev === null || ev < 0) continue;
      if (Math.abs(ev - userAnciennete) < ANCIENNETE_DIVERGENCE_YEARS) continue;
      builder.addSource('F96', {
        expectedDisplay: this.formatAnciennete(ev),
        reason: `Checklist procédurale : ancienneté ${this.formatAnciennete(ev)}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    // 2. QUESTION_IA — réponse "oui" sur `RCI_ANCIENNETE`.
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'RCI_ANCIENNETE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = this.parseNumber(q.expectedValue);
      if (ev === null || ev < 0) continue;
      if (Math.abs(ev - userAnciennete) < ANCIENNETE_DIVERGENCE_YEARS) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: this.formatAnciennete(ev),
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — compensationEstimate.
    builder.addSource('IA', {
      expectedDisplay: this.formatAnciennete(iaAnciennete),
      reason: `Analyse du dossier : ancienneté ${this.formatAnciennete(iaAnciennete)}`,
    });

    // 4. PIECE_MANQUANTE (contrat / attestation employeur).
    const piece = this.findPieceManquante(['RCI_ANCIENNETE', 'ANCIENNETE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * SF-155-12 : parse un nombre depuis une chaîne (virgule ou point), renvoie
   * `null` si parsing impossible. Utilisé pour F96/QUESTION_IA `expectedValue`.
   */
  private parseNumber(raw: string | null | undefined): number | null {
    if (!raw) return null;
    const normalized = raw.replace(',', '.').replace(/\s/g, '');
    const n = Number(normalized);
    return Number.isFinite(n) ? n : null;
  }

  private formatAnciennete(n: number): string {
    return n === 1 ? '1 an' : `${n} ans`;
  }

  /**
   * SF-155-12 : renvoie le `texte` d'une `PieceManquanteEntry` dont
   * `critereCode` appartient à la liste acceptée (case-insensitive),
   * ou `null` si aucune pièce ne matche. La première pièce trouvée gagne.
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
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const a = this.ancienneteAnnees();
    const s = this.salaireMensuel();
    return a !== null && a >= 0 && s !== null && s > 0;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * SF-155-12 : handler change ancienneté — efface le badge IA dès que
   * l'avocat modifie manuellement.
   */
  onAncienneteAnneesChange(value: number | null): void {
    this.ancienneteAnnees.set(value);
    this.provenanceAncienneteAnnees.set(null);
  }

  /**
   * SF-155-12 : handler change salaire — efface le badge IA.
   */
  onSalaireMensuelChange(value: number | null): void {
    this.salaireMensuel.set(value);
    this.provenanceSalaireMensuel.set(null);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request = {
      ancienneteAnnees: this.ancienneteAnnees()!,
      salaireMensuel: this.salaireMensuel()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Indemnité calculée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      }
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.ancienneteAnnees.set(r.ancienneteAnnees);
        this.salaireMensuel.set(r.salaireMensuel);
        // SF-155-12 : valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceAncienneteAnnees.set(null);
        this.provenanceSalaireMensuel.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        // SF-155-12 : fallback pré-fill IA uniquement ici (pas si GET 200).
        this.loading.set(false);
        this.prefillFromAi();
      }
    });
  }
}
