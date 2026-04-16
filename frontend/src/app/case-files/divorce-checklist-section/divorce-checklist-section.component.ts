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
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';

export type DivorceAlertSource = 'F96' | 'QUESTION_IA' | 'PIECE_IA' | 'MULTI';
export type DivorceAlertLevel = 'blocker' | 'warning';

export interface DivorceCoherenceAlert {
  level: DivorceAlertLevel;
  source: DivorceAlertSource;
  contributors: DivorceAlertSource[];
  reason: string;
}

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

@Component({
  selector: 'app-divorce-checklist-section',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatIconModule, MatSelectModule, MatFormFieldModule, MatProgressSpinnerModule, MatTooltipModule, CoherencePopoverTriggerDirective],
  templateUrl: './divorce-checklist-section.component.html',
  styleUrl: './divorce-checklist-section.component.scss'
})
export class DivorceChecklistSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  saving = signal(false);
  result = signal<DivorceChecklistResponse | null>(null);
  country = signal('FRANCE');

  progress = computed(() => {
    const r = this.result();
    if (!r) return 0;
    const total = r.etapesTotal + r.piecesTotal;
    const done = r.etapesCompletees + r.piecesPresentes;
    return total > 0 ? Math.round((done / total) * 100) : 0;
  });

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
      blockers: values.filter(a => a.level === 'blocker').length,
    };
  });

  // SF-IA-03-15b — map {sourceKey → explanation}
  sourceExplanations = signal<Map<string, SourceExplanation>>(new Map());

  constructor(private checklistService: DivorceChecklistService, private sourceExplanationService: SourceExplanationService, private snackBar: MatSnackBar, @Optional() private refreshService: CaseDashboardRefreshService | null) {}

  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: map => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15b : lookup par code étape/pièce (sourceKey direct). */
  explanationFor(code: string): SourceExplanation | null {
    return this.sourceExplanations().get(code) ?? null;
  }

  ngOnInit(): void {
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.loadExisting();
    this.loadSourceExplanations();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
  }

  toggleCollapsed(): void { this.collapsed.update(v => !v); }

  loadExisting(): void {
    this.loading.set(true);
    this.checklistService.get(this.caseFileId).subscribe({
      next: r => { this.result.set(r); this.country.set(r.country); this.loading.set(false); },
      error: () => { this.loading.set(false); },
    });
  }

  toggleEtape(etape: DivorceEtapeStatus): void {
    etape.statut = etape.statut === 'FAIT' ? 'A_FAIRE' : 'FAIT';
    this.saveChecklist();
  }

  togglePiece(piece: DivorcePieceStatus): void {
    piece.statut = piece.statut === 'PRESENTE' ? 'MANQUANTE' : 'PRESENTE';
    this.saveChecklist();
  }

  initChecklist(): void {
    this.saveChecklist();
  }

  alertIcon(alert: DivorceCoherenceAlert): string {
    return alert.level === 'blocker' ? 'error' : 'warning';
  }

  private buildStepAlert(etape: DivorceEtapeStatus): DivorceCoherenceAlert | null {
    if (!STEP_CODES.has(etape.code)) return null;
    const contributors: DivorceAlertSource[] = [];
    const reasons: string[] = [];
    let level: DivorceAlertLevel | null = null;

    // F-96 check
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== etape.code) continue;
      if (etape.statut === 'FAIT' && chk.statut === 'NON_COMPLIANT') {
        contributors.push('F96');
        reasons.push(`Checklist procédurale : étape non respectée${chk.raison ? ' (' + chk.raison + ')' : ''}`);
        level = 'blocker';
      } else if (etape.statut === 'A_FAIRE' && chk.statut === 'VERIFIED') {
        contributors.push('F96');
        reasons.push(`Checklist procédurale : étape validée${chk.raison ? ' (' + chk.raison + ')' : ''}`);
        level = level ?? 'warning';
      }
      break;
    }

    // Question IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== etape.code) continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ') || answer.startsWith('oui,') || answer.startsWith('oui.');
      const isNon = answer === 'non' || answer.startsWith('non ') || answer.startsWith('non,') || answer.startsWith('non.');
      if (etape.statut === 'FAIT' && isNon) {
        contributors.push('QUESTION_IA');
        reasons.push(`Question complémentaire : "${q.questionText}" → réponse "${q.answerText}"`);
        level = 'blocker';
      } else if (etape.statut === 'A_FAIRE' && isOui) {
        contributors.push('QUESTION_IA');
        reasons.push(`Question complémentaire : "${q.questionText}" → réponse "${q.answerText}"`);
        level = level ?? 'warning';
      }
      break;
    }

    if (contributors.length === 0 || !level) return null;
    return {
      level,
      source: contributors.length > 1 ? 'MULTI' : contributors[0],
      contributors,
      reason: reasons.join(' ET '),
    };
  }

  private buildPieceAlert(piece: DivorcePieceStatus): DivorceCoherenceAlert | null {
    if (!PIECE_CODES.has(piece.code)) return null;
    const contributors: DivorceAlertSource[] = [];
    const reasons: string[] = [];
    let level: DivorceAlertLevel | null = null;

    // Pièce IA (pieces_manquantes)
    for (const p of this.piecesManquantesSignal()) {
      if (p.critereCode?.toUpperCase() !== piece.code) continue;
      if (piece.statut === 'PRESENTE') {
        contributors.push('PIECE_IA');
        reasons.push(`Pièce manquante signalée : ${p.texte}`);
        level = 'warning';
      }
      break;
    }

    // F-96 sur code pièce
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== piece.code) continue;
      if (piece.statut === 'PRESENTE' && chk.statut === 'NON_COMPLIANT') {
        contributors.push('F96');
        reasons.push(`Checklist procédurale : pièce absente${chk.raison ? ' (' + chk.raison + ')' : ''}`);
        level = 'blocker';
      } else if (piece.statut === 'MANQUANTE' && chk.statut === 'VERIFIED') {
        contributors.push('F96');
        reasons.push(`Checklist procédurale : pièce validée${chk.raison ? ' (' + chk.raison + ')' : ''}`);
        level = level ?? 'warning';
      }
      break;
    }

    if (contributors.length === 0 || !level) return null;
    return {
      level,
      source: contributors.length > 1 ? 'MULTI' : contributors[0],
      contributors,
      reason: reasons.join(' ET '),
    };
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
}
