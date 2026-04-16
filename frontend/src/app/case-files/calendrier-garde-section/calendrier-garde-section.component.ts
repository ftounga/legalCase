import { Component, Input, OnInit, OnChanges, SimpleChanges, Optional, signal, computed } from '@angular/core';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { CalendrierGardeService } from '../../core/services/calendrier-garde.service';
import { CalendrierGardeResponse } from '../../core/models/calendrier-garde.model';
import { CaseAnalysisResult, PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';

const MODES_FR = new Set(['ALTERNEE_FR', 'DVH_CLASSIQUE_FR', 'DVH_ELARGI_FR']);
const MODES_BE = new Set(['ALTERNEE_BE', 'SECONDAIRE_BE', 'SECONDAIRE_ELARGI_BE']);
const ALL_MODES = new Set([...MODES_FR, ...MODES_BE]);
const ALTERNEE_MODES = new Set(['ALTERNEE_FR', 'ALTERNEE_BE']);

export type GardeAlertSource = 'F96' | 'QUESTION_IA' | 'IA' | 'IA_COARSE' | 'PIECE_MANQUANTE' | 'MULTI';
export type GardeAlertLevel = 'blocker' | 'warning';

export interface GardeCoherenceAlert {
  level: GardeAlertLevel;
  source: GardeAlertSource;
  contributors: GardeAlertSource[];
  expectedDisplay: string;
  reason: string;
  pieceTexte?: string | null;
}

@Component({
  selector: 'app-calendrier-garde-section',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatIconModule, MatSelectModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, MatTooltipModule, CoherencePopoverTriggerDirective],
  templateUrl: './calendrier-garde-section.component.html',
  styleUrl: './calendrier-garde-section.component.scss'
})
export class CalendrierGardeSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() aiModeGardeDetaille?: string | null;
  @Input() workspaceCountry: string = 'FRANCE';
  @Input() synthesis?: CaseAnalysisResult | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private synthesisSignal = signal<CaseAnalysisResult | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  generating = signal(false);
  showForm = signal(true);
  result = signal<CalendrierGardeResponse | null>(null);

  gardeCode = signal('ALTERNEE_FR');
  parentANom = signal('');
  parentBNom = signal('');
  modeDetailleNote = signal<string | null>(null);

  readonly modes = [
    { group: 'France', items: [
      { value: 'ALTERNEE_FR', label: 'Résidence alternée' },
      { value: 'DVH_CLASSIQUE_FR', label: 'DVH classique' },
      { value: 'DVH_ELARGI_FR', label: 'DVH élargi' },
    ]},
    { group: 'Belgique', items: [
      { value: 'ALTERNEE_BE', label: 'Hébergement égalitaire' },
      { value: 'SECONDAIRE_BE', label: 'Hébergement secondaire' },
      { value: 'SECONDAIRE_ELARGI_BE', label: 'Hébergement secondaire élargi' },
    ]},
  ];

  // SF-IA-03-15b — map {sourceKey → explanation}
  sourceExplanations = signal<Map<string, SourceExplanation>>(new Map());

  constructor(private gardeService: CalendrierGardeService, private sourceExplanationService: SourceExplanationService, private snackBar: MatSnackBar, @Optional() private refreshService: CaseDashboardRefreshService | null) {}

  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: map => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  explanationFor(): SourceExplanation | null {
    return this.sourceExplanations().get('FA06_MODE_GARDE') ?? null;
  }

  coherenceAlert = computed<GardeCoherenceAlert | null>(() => {
    if (!this.showForm()) return null;
    const user = this.gardeCode();
    if (!user || !ALL_MODES.has(user)) return null;

    const piecesIndex = this.buildPiecesIndex(this.piecesManquantesSignal());
    const pieceTexte = piecesIndex['FA06_MODE_GARDE'] ?? null;
    const contributors: GardeAlertSource[] = [];
    const reasons: string[] = [];
    let expected: string | null = null;
    let level: GardeAlertLevel | null = null;

    // B — F-96 VERIFIED + expected_value
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA06_MODE_GARDE') continue;
      if (chk.statut !== 'VERIFIED') continue;
      const ev = chk.expectedValue?.toUpperCase();
      if (!ev || !ALL_MODES.has(ev)) continue;
      if (ev !== user && !expected) {
        expected = ev;
        contributors.push('F96');
        reasons.push(`Checklist procédurale : ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`);
        level = 'blocker';
      }
      break;
    }

    // C — Question IA "oui" + expected_value
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'FA06_MODE_GARDE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ') || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue?.toUpperCase();
      if (!ev || !ALL_MODES.has(ev)) continue;
      if (ev === user) continue;
      if (!expected) {
        expected = ev;
        contributors.push('QUESTION_IA');
        reasons.push(`Question complémentaire : "${q.questionText}" → "${q.answerText}"`);
        level = 'blocker';
      } else if (ev === expected) {
        contributors.push('QUESTION_IA');
        reasons.push(`Question complémentaire : "${q.questionText}" → "${q.answerText}"`);
      }
      break;
    }

    // D — IA modeGardeDetaille
    const iaDetaille = this.synthesisSignal()?.pensionAlimentaireEstimate?.modeGardeDetaille?.toUpperCase();
    if (iaDetaille && ALL_MODES.has(iaDetaille) && iaDetaille !== user) {
      if (!expected) {
        expected = iaDetaille;
        contributors.push('IA');
        reasons.push(`Analyse du dossier : ${iaDetaille}`);
        level = 'blocker';
      } else if (iaDetaille === expected) {
        contributors.push('IA');
        reasons.push(`Analyse du dossier : ${iaDetaille}`);
      }
    }

    // E — IA coarse (ALTERNEE/EXCLUSIVE)
    if (!expected) {
      const iaCoarse = this.synthesisSignal()?.pensionAlimentaireEstimate?.modeGarde;
      if (iaCoarse === 'ALTERNEE' || iaCoarse === 'EXCLUSIVE') {
        const userIsAlternee = ALTERNEE_MODES.has(user);
        const iaIsAlternee = iaCoarse === 'ALTERNEE';
        if (userIsAlternee !== iaIsAlternee) {
          expected = iaCoarse === 'ALTERNEE' ? 'mode alterné' : 'mode non alterné';
          contributors.push('IA_COARSE');
          reasons.push(`Analyse du dossier (catégorie) : ${iaCoarse}`);
          level = 'warning';
        }
      }
    }

    if (!level || !expected) return null;
    if (pieceTexte) {
      contributors.push('PIECE_MANQUANTE');
      reasons.push(`Pièce manquante : ${pieceTexte}`);
    }
    const source: GardeAlertSource = contributors.length > 1 ? 'MULTI' : contributors[0];
    return { level, source, contributors, expectedDisplay: expected, reason: reasons.join(' ET '), pieceTexte };
  });

  alertsSummary = computed(() => {
    const a = this.coherenceAlert();
    return { total: a ? 1 : 0, blockers: a?.level === 'blocker' ? 1 : 0 };
  });

  ngOnInit(): void {
    this.synthesisSignal.set(this.synthesis);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.loadExisting();
    this.loadSourceExplanations();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['synthesis']) this.synthesisSignal.set(this.synthesis);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (changes['aiModeGardeDetaille'] && this.showForm() && !this.result()) {
      this.applyAiPrefill();
    }
  }

  private buildPiecesIndex(pieces: PieceManquanteEntry[]): Record<string, string> {
    const index: Record<string, string> = {};
    if (!pieces) return index;
    for (const p of pieces) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (code !== 'FA06_MODE_GARDE') continue;
      if (!index[code]) index[code] = p.texte;
    }
    return index;
  }

  alertTooltip(alert: GardeCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: GardeCoherenceAlert): string {
    switch (alert.source) {
      case 'F96': return `Incohérence Checklist procédurale (${alert.expectedDisplay})`;
      case 'QUESTION_IA': return `Incohérence Question complémentaire (${alert.expectedDisplay})`;
      case 'IA': return `Incohérence détectée (${alert.expectedDisplay})`;
      case 'IA_COARSE': return `Incohérence catégorie IA`;
      case 'PIECE_MANQUANTE': return `Pièce manquante (${alert.expectedDisplay})`;
      case 'MULTI': return `Incohérence multiple (${alert.expectedDisplay})`;
    }
  }

  toggleCollapsed(): void { this.collapsed.update(v => !v); }

  loadExisting(): void {
    this.loading.set(true);
    this.gardeService.get(this.caseFileId).subscribe({
      next: r => { this.result.set(r); this.prefillForm(r); this.showForm.set(false); this.loading.set(false); },
      error: () => { this.showForm.set(true); this.loading.set(false); this.applyAiPrefill(); },
    });
  }

  private prefillForm(resp: CalendrierGardeResponse): void {
    if (resp.gardeCode) this.gardeCode.set(resp.gardeCode);
    if (resp.parentANom != null) this.parentANom.set(resp.parentANom);
    if (resp.parentBNom != null) this.parentBNom.set(resp.parentBNom);
  }

  generate(): void {
    this.generating.set(true);
    this.gardeService.generate(this.caseFileId, {
      gardeCode: this.gardeCode(), parentANom: this.parentANom(), parentBNom: this.parentBNom(),
    }).subscribe({
      next: r => { this.result.set(r); this.showForm.set(false); this.generating.set(false); this.refreshService?.triggerRefresh(); },
      error: () => { this.generating.set(false); this.snackBar.open('Erreur', 'Fermer', { duration: 4000 }); },
    });
  }

  editForm(): void {
    const r = this.result();
    if (r) this.prefillForm(r);
    this.showForm.set(true);
  }

  onGardeCodeChange(): void {
    // L'avocat a modifié le mode — on efface la note de pré-remplissage.
    this.modeDetailleNote.set(null);
  }

  private applyAiPrefill(): void {
    const ai = this.aiModeGardeDetaille?.toUpperCase();
    if (!ai) { this.modeDetailleNote.set(null); return; }
    const wsFR = this.workspaceCountry === 'FRANCE';
    const isFR = MODES_FR.has(ai);
    const isBE = MODES_BE.has(ai);
    if (!isFR && !isBE) { this.modeDetailleNote.set(null); return; }
    if ((wsFR && isFR) || (!wsFR && isBE)) {
      this.gardeCode.set(ai);
      this.modeDetailleNote.set(null);
    } else {
      this.modeDetailleNote.set(
        `L'IA a détecté le mode "${ai}" (autre pays). Vérifier que ce dossier est adapté.`
      );
    }
  }
}
