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
import { CaseAnalysisResult } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';

const MODES_FR = new Set(['ALTERNEE_FR', 'DVH_CLASSIQUE_FR', 'DVH_ELARGI_FR']);
const MODES_BE = new Set(['ALTERNEE_BE', 'SECONDAIRE_BE', 'SECONDAIRE_ELARGI_BE']);
const ALL_MODES = new Set([...MODES_FR, ...MODES_BE]);
const ALTERNEE_MODES = new Set(['ALTERNEE_FR', 'ALTERNEE_BE']);

export type GardeAlertSource = 'F96' | 'QUESTION_IA' | 'IA' | 'IA_COARSE' | 'MULTI';
export type GardeAlertLevel = 'blocker' | 'warning';

export interface GardeCoherenceAlert {
  level: GardeAlertLevel;
  source: GardeAlertSource;
  contributors: GardeAlertSource[];
  expectedDisplay: string;
  reason: string;
}

@Component({
  selector: 'app-calendrier-garde-section',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatIconModule, MatSelectModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, MatTooltipModule],
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

  private synthesisSignal = signal<CaseAnalysisResult | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);

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

  constructor(private gardeService: CalendrierGardeService, private snackBar: MatSnackBar, @Optional() private refreshService: CaseDashboardRefreshService | null) {}

  coherenceAlert = computed<GardeCoherenceAlert | null>(() => {
    if (!this.showForm() || this.result()) return null;
    const user = this.gardeCode();
    if (!user || !ALL_MODES.has(user)) return null;

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
        reasons.push(`Question IA : "${q.questionText}" → "${q.answerText}"`);
        level = 'blocker';
      } else if (ev === expected) {
        contributors.push('QUESTION_IA');
        reasons.push(`Question IA : "${q.questionText}" → "${q.answerText}"`);
      }
      break;
    }

    // D — IA modeGardeDetaille
    const iaDetaille = this.synthesisSignal()?.pensionAlimentaireEstimate?.modeGardeDetaille?.toUpperCase();
    if (iaDetaille && ALL_MODES.has(iaDetaille) && iaDetaille !== user) {
      if (!expected) {
        expected = iaDetaille;
        contributors.push('IA');
        reasons.push(`Analyse IA : ${iaDetaille}`);
        level = 'blocker';
      } else if (iaDetaille === expected) {
        contributors.push('IA');
        reasons.push(`Analyse IA : ${iaDetaille}`);
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
          reasons.push(`Analyse IA (catégorie) : ${iaCoarse}`);
          level = 'warning';
        }
      }
    }

    if (!level || !expected) return null;
    const source: GardeAlertSource = contributors.length > 1 ? 'MULTI' : contributors[0];
    return { level, source, contributors, expectedDisplay: expected, reason: reasons.join(' ET ') };
  });

  alertsSummary = computed(() => {
    const a = this.coherenceAlert();
    return { total: a ? 1 : 0, blockers: a?.level === 'blocker' ? 1 : 0 };
  });

  ngOnInit(): void {
    this.synthesisSignal.set(this.synthesis);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.loadExisting();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['synthesis']) this.synthesisSignal.set(this.synthesis);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['aiModeGardeDetaille'] && this.showForm() && !this.result()) {
      this.applyAiPrefill();
    }
  }

  alertTooltip(alert: GardeCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: GardeCoherenceAlert): string {
    switch (alert.source) {
      case 'F96': return `Incohérence F-96 (${alert.expectedDisplay})`;
      case 'QUESTION_IA': return `Incohérence Question IA (${alert.expectedDisplay})`;
      case 'IA': return `Incohérence IA (${alert.expectedDisplay})`;
      case 'IA_COARSE': return `Incohérence catégorie IA`;
      case 'MULTI': return `Incohérence multiple (${alert.expectedDisplay})`;
    }
  }

  toggleCollapsed(): void { this.collapsed.update(v => !v); }

  loadExisting(): void {
    this.loading.set(true);
    this.gardeService.get(this.caseFileId).subscribe({
      next: r => { this.result.set(r); this.showForm.set(false); this.loading.set(false); },
      error: () => { this.showForm.set(true); this.loading.set(false); this.applyAiPrefill(); },
    });
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

  editForm(): void { this.showForm.set(true); }

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
