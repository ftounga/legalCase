import { Component, Input, OnInit, OnChanges, SimpleChanges, Optional, signal, computed } from '@angular/core';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { DivorceChecklistService } from '../../core/services/divorce-checklist.service';
import { DivorceChecklistResponse, DivorceEtapeStatus, DivorcePieceStatus } from '../../core/models/divorce-checklist.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert, CoherenceAlertSource } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { DivorceChecklistPrefillRules, SIGNATURE_STEP_CODES } from './divorce-checklist-section-prefill-rules';

/**
 * SF-155-19 : champs d'alerte = codes étape / code pièce.
 * On garde `string` car le set de codes est dynamique (FR + BE — 13 étapes + 17 pièces).
 */
export type DivorceChecklistAlertField = string;

// SF-155-19 : alias rétro-compat pour l'usage interne (avant migration). Utilise
// le pattern partagé `CoherenceAlert` (SF-155-05) au lieu d'une interface locale.
export type DivorceAlertSource = CoherenceAlertSource;
export type DivorceCoherenceAlert = CoherenceAlert<DivorceChecklistAlertField>;

const STEP_CODES = new Set([
  'FR_CHOIX_AVOCATS', 'FR_REDACTION_CONVENTION', 'FR_ENVOI_LRAR', 'FR_DELAI_REFLEXION',
  'FR_SIGNATURE_CONVENTION', 'FR_DEPOT_NOTAIRE', 'FR_ENREGISTREMENT',
  'BE_CHOIX_AVOCAT', 'BE_REDACTION_CONVENTION', 'BE_REQUETE_CONJOINTE',
  'BE_COMPARUTION', 'BE_JUGEMENT', 'BE_TRANSCRIPTION',
]);

const PIECE_CODES = new Set([
  'FR_ACTE_MARIAGE', 'FR_ACTE_NAISSANCE_EPOUX', 'FR_ACTE_NAISSANCE_ENFANTS', 'FR_LIVRET_FAMILLE',
  'FR_JUSTIF_DOMICILE', 'FR_CONTRAT_MARIAGE', 'FR_ETAT_PATRIMOINE', 'FR_JUSTIF_REVENUS', 'FR_PIECE_IDENTITE',
  'BE_ACTE_MARIAGE', 'BE_ACTE_NAISSANCE_EPOUX', 'BE_ACTE_NAISSANCE_ENFANTS', 'BE_COMPOSITION_MENAGE',
  'BE_CONTRAT_MARIAGE', 'BE_CONVENTION_PREALABLE', 'BE_JUSTIF_REVENUS', 'BE_PIECE_IDENTITE',
]);

// SF-155-19 : codes étape "signature de la convention" (FR + BE) — désormais
// déclarés dans le helper partagé `divorce-checklist-section-prefill-rules.ts`
// (F-236 SF-236-03). Re-importé ci-dessus pour usage runtime.

@Component({
  selector: 'app-divorce-checklist-section',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatIconModule, MatSelectModule, MatFormFieldModule, MatProgressSpinnerModule, MatTooltipModule, CoherencePopoverTriggerDirective],
  templateUrl: './divorce-checklist-section.component.html',
  styleUrl: './divorce-checklist-section.component.scss'
})
export class DivorceChecklistSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CHECKLIST DIVORCE';
  static readonly TOOL_ICON = 'checklist';

  @Input() caseFileId!: string;
  // SF-155-19 : inputs IA (tous optionnels — null-safe partout). aiData = pré-fill ;
  // les 3 autres alimentent F-IA-03.
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  /**
   * F-194 SF-194-02 — Libellés des pièces taggées « OBTENUE » alignées
   * sur cet outil. V1 = passif (signal d'aide visuel forward-compat).
   */
  @Input() piecesObtenues?: string[] | null;

  private aiDataSignal = signal<FamilleExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  collapsed = signal(true);
  loading = signal(false);
  saving = signal(false);
  result = signal<DivorceChecklistResponse | null>(null);
  country = signal('FRANCE');

  // SF-155-19 : provenance IA par code étape/pièce. 'IA' quand le statut a été
  // pré-rempli par `prefillFromAi()` ; remis à null dès que l'avocat re-toggle.
  provenanceByCode = signal<Record<string, 'IA' | null>>({});

  progress = computed(() => {
    const r = this.result();
    if (!r) return 0;
    const total = r.etapesTotal + r.piecesTotal;
    const done = r.etapesCompletees + r.piecesPresentes;
    return total > 0 ? Math.round((done / total) * 100) : 0;
  });

  /**
   * SF-155-19 : alertes de cohérence F-IA-03 calculées dynamiquement à partir
   * des 4 sources (aiData / procedureChecks F-96 / aiQuestions / piecesManquantes).
   * Migration vers `CoherenceAlertBuilder` partagé (SF-155-05).
   */
  coherenceAlerts = computed<Record<string, DivorceCoherenceAlert>>(() => {
    const r = this.result();
    if (!r) return {};
    const alerts: Record<string, DivorceCoherenceAlert> = {};
    for (const etape of r.etapes) {
      const a = this.buildStepAlert(etape);
      if (a) alerts[etape.code] = a;
    }
    for (const piece of r.pieces) {
      const a = this.buildPieceAlert(piece);
      if (a) alerts[piece.code] = a;
    }
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return {
      total: values.length,
      blockers: values.filter(a => a.severity === 'CRITICAL').length,
    };
  });

  // SF-IA-03-15b — map {sourceKey → explanation}
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private checklistService: DivorceChecklistService,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
    private snackBar: MatSnackBar,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    if (!this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: map => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15b : lookup par code étape/pièce (sourceKey direct). */
  explanationFor(code: string): SourceExplanation[] {
    return this.sourceExplanations().get(code) ?? [];
  }

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.loadExisting();
    this.loadSourceExplanations();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // SF-155-19 : ré-applique le prefill quand aiData change avant première
    // résolution backend (le résultat n'est pas encore chargé).
    if (changes['aiData'] && !changes['aiData'].firstChange && this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapsed(): void { this.collapsed.update(v => !v); }

  loadExisting(): void {
    this.loading.set(true);
    this.checklistService.get(this.caseFileId).subscribe({
      next: r => {
        this.result.set(r);
        this.country.set(r.country);
        this.prefillFromAi();
        this.loading.set(false);
      },
      error: () => { this.loading.set(false); },
    });
  }

  /**
   * SF-155-19 : pré-remplit la checklist depuis `aiData`. Pour le divorce
   * par consentement mutuel, le seul signal exploitable au pré-fill est
   * `dateAcceptationPV` (date de signature de la convention) — on marque
   * alors les étapes "signature/rédaction de la convention" comme FAIT.
   *
   * F-236 SF-236-03 : délègue à `DivorceChecklistPrefillRules.computeDateAcceptationPV`
   * pour la validation ISO + la sélection des codes étape, garantissant la
   * parité runtime/static (badge = 2 ↔ 2 étapes pré-cochées en mémoire).
   *
   * Idempotent : ne marque IA que les étapes encore A_FAIRE (n'écrase pas
   * une saisie avocat manuelle existante) ; ne déclenche pas de save HTTP
   * (pas de side-effect réseau).
   */
  private prefillFromAi(): void {
    const r = this.result();
    const ai = this.aiDataSignal();
    if (!r) return;
    const input: PrefillCountInput = { aiData: ai ?? null };
    if (DivorceChecklistPrefillRules.computeDateAcceptationPV(input) === null) return;

    const next = { ...this.provenanceByCode() };
    let changed = false;
    for (const etape of r.etapes) {
      if (!(SIGNATURE_STEP_CODES as readonly string[]).includes(etape.code)) continue;
      // Ne pas écraser un statut déjà saisi par l'avocat.
      if (etape.statut === 'FAIT') continue;
      etape.statut = 'FAIT';
      next[etape.code] = 'IA';
      changed = true;
    }
    if (changed) {
      this.provenanceByCode.set(next);
      // Snapshot en place — on remonte le compteur pour cohérence d'affichage.
      r.etapesCompletees = r.etapes.filter(e => e.statut === 'FAIT').length;
      this.result.set({ ...r });
    }
  }

  /**
   * SF-155-19 : retourne true si le statut courant de la ligne a été
   * pré-rempli depuis l'IA (pour afficher le badge `auto_awesome`).
   */
  isPrefilledFromAi(code: string): boolean {
    return this.provenanceByCode()[code] === 'IA';
  }

  /**
   * SF-155-19 : handler partagé étape/pièce — efface la provenance IA dès que
   * l'avocat re-toggle manuellement (pattern canonique `onXxxChange`).
   */
  onEtapeStatutChange(etape: DivorceEtapeStatus): void {
    if (this.provenanceByCode()[etape.code] === 'IA') {
      const next = { ...this.provenanceByCode() };
      delete next[etape.code];
      this.provenanceByCode.set(next);
    }
    this.toggleEtapeInternal(etape);
  }

  onPieceStatutChange(piece: DivorcePieceStatus): void {
    if (this.provenanceByCode()[piece.code] === 'IA') {
      const next = { ...this.provenanceByCode() };
      delete next[piece.code];
      this.provenanceByCode.set(next);
    }
    this.togglePieceInternal(piece);
  }

  toggleEtape(etape: DivorceEtapeStatus): void {
    this.onEtapeStatutChange(etape);
  }

  togglePiece(piece: DivorcePieceStatus): void {
    this.onPieceStatutChange(piece);
  }

  private toggleEtapeInternal(etape: DivorceEtapeStatus): void {
    etape.statut = etape.statut === 'FAIT' ? 'A_FAIRE' : 'FAIT';
    this.saveChecklist();
  }

  private togglePieceInternal(piece: DivorcePieceStatus): void {
    piece.statut = piece.statut === 'PRESENTE' ? 'MANQUANTE' : 'PRESENTE';
    this.saveChecklist();
  }

  initChecklist(): void {
    this.saveChecklist();
  }

  alertIcon(alert: DivorceCoherenceAlert): string {
    return alert.severity === 'CRITICAL' ? 'error' : 'warning';
  }

  /** SF-155-19 : true si l'alerte indique que le statut affiché contredit les sources IA. */
  alertIsBlocker(alert: DivorceCoherenceAlert): boolean {
    return alert.severity === 'CRITICAL';
  }

  /**
   * SF-155-19 : construit l'alerte de cohérence pour une étape via le builder
   * partagé. Sources scannées : F-96 (procedureChecks) + Question IA (aiQuestions).
   * - Étape FAIT + source dit "non fait" → CRITICAL.
   * - Étape A_FAIRE + source dit "fait" → WARNING.
   */
  private buildStepAlert(etape: DivorceEtapeStatus): DivorceCoherenceAlert | null {
    if (!STEP_CODES.has(etape.code)) return null;

    let severityHint: 'CRITICAL' | 'WARNING' | null = null;
    const builder = CoherenceAlertBuilder.forField<DivorceChecklistAlertField>(etape.code);

    // F-96 check — incohérence entre statut affiché et résultat checklist procédurale.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== etape.code) continue;
      if (etape.statut === 'FAIT' && chk.statut === 'NON_COMPLIANT') {
        builder.addSource('F96', {
          expectedDisplay: 'À faire',
          reason: `Checklist procédurale : étape non respectée${chk.raison ? ' (' + chk.raison + ')' : ''}`,
        });
        severityHint = 'CRITICAL';
      } else if (etape.statut === 'A_FAIRE' && chk.statut === 'VERIFIED') {
        builder.addSource('F96', {
          expectedDisplay: 'Fait',
          reason: `Checklist procédurale : étape validée${chk.raison ? ' (' + chk.raison + ')' : ''}`,
        });
        severityHint = severityHint ?? 'WARNING';
      }
      break;
    }

    // Question IA : réponse "oui"/"non" sur le code étape.
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== etape.code) continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ') || answer.startsWith('oui,') || answer.startsWith('oui.');
      const isNon = answer === 'non' || answer.startsWith('non ') || answer.startsWith('non,') || answer.startsWith('non.');
      if (etape.statut === 'FAIT' && isNon) {
        builder.addSource('QUESTION_IA', {
          expectedDisplay: 'À faire',
          reason: `Question complémentaire : "${q.questionText}" → réponse "${q.answerText}"`,
        });
        severityHint = 'CRITICAL';
      } else if (etape.statut === 'A_FAIRE' && isOui) {
        builder.addSource('QUESTION_IA', {
          expectedDisplay: 'Fait',
          reason: `Question complémentaire : "${q.questionText}" → réponse "${q.answerText}"`,
        });
        severityHint = severityHint ?? 'WARNING';
      }
      break;
    }

    if (severityHint) builder.withSeverity(severityHint);
    return builder.build();
  }

  /**
   * SF-155-19 : construit l'alerte de cohérence pour une pièce via le builder
   * partagé. Sources scannées : Pièce IA (piecesManquantes) + F-96 (procedureChecks).
   * - Pièce PRESENTE + IA signale manquante → WARNING (pas CRITICAL — divergence
   *   non bloquante, l'avocat peut avoir reçu la pièce après l'analyse).
   * - Pièce PRESENTE + F-96 NON_COMPLIANT → CRITICAL.
   * - Pièce MANQUANTE + F-96 VERIFIED → WARNING.
   */
  private buildPieceAlert(piece: DivorcePieceStatus): DivorceCoherenceAlert | null {
    if (!PIECE_CODES.has(piece.code)) return null;

    let severityHint: 'CRITICAL' | 'WARNING' | null = null;
    const builder = CoherenceAlertBuilder.forField<DivorceChecklistAlertField>(piece.code);

    // Pièce IA : si l'IA signale la pièce manquante alors que l'avocat dit PRESENTE.
    for (const p of this.piecesManquantesSignal()) {
      if (p.critereCode?.toUpperCase() !== piece.code) continue;
      if (piece.statut === 'PRESENTE') {
        builder.addSource('PIECE_MANQUANTE', {
          expectedDisplay: 'Manquante',
          reason: `Pièce manquante signalée : ${p.texte}`,
          pieceTexte: p.texte,
        });
        severityHint = 'WARNING';
      }
      break;
    }

    // F-96 sur code pièce.
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== piece.code) continue;
      if (piece.statut === 'PRESENTE' && chk.statut === 'NON_COMPLIANT') {
        builder.addSource('F96', {
          expectedDisplay: 'Manquante',
          reason: `Checklist procédurale : pièce absente${chk.raison ? ' (' + chk.raison + ')' : ''}`,
        });
        severityHint = 'CRITICAL';
      } else if (piece.statut === 'MANQUANTE' && chk.statut === 'VERIFIED') {
        builder.addSource('F96', {
          expectedDisplay: 'Présente',
          reason: `Checklist procédurale : pièce validée${chk.raison ? ' (' + chk.raison + ')' : ''}`,
        });
        severityHint = severityHint ?? 'WARNING';
      }
      break;
    }

    if (severityHint) builder.withSeverity(severityHint);
    return builder.build();
  }

  private saveChecklist(): void {
    this.saving.set(true);
    const r = this.result();
    const etapeStatuts: Record<string, string> = {};
    const pieceStatuts: Record<string, string> = {};
    if (r) {
      r.etapes.forEach(e => etapeStatuts[e.code] = e.statut);
      r.pieces.forEach(p => pieceStatuts[p.code] = p.statut);
    }
    this.checklistService.save(this.caseFileId, {
      country: this.country(), etapeStatuts, pieceStatuts,
    }).subscribe({
      next: resp => { this.result.set(resp); this.saving.set(false); this.refreshService?.triggerRefresh(); },
      error: () => { this.saving.set(false); this.snackBar.open('Erreur', 'Fermer', { duration: 4000 }); },
    });
  }

  /**
   * F-IA-04 / F-177 SF-177-12 — compteur de champs pré-remplis affiché
   * en badge sur la card avant ouverture.
   *
   * F-236 SF-236-03 : délègue intégralement à
   * `DivorceChecklistPrefillRules.computePrefillCount`. Quand
   * `dateAcceptationPV` est ISO valide, 2 étapes signature/rédaction sont
   * pré-cochées (`FR_SIGNATURE_CONVENTION` + `BE_REDACTION_CONVENTION`).
   * Le badge reflète l'expérience UX réelle de l'avocat — corrigé via
   * audit SF-236-01 (badge précédent = 1, divergent du runtime).
   */
  static getPrefillCount(input: PrefillCountInput): number {
    return DivorceChecklistPrefillRules.computePrefillCount(input);
  }
}
