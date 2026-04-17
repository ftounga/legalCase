import { Component, Input, OnChanges, OnInit, SimpleChanges, Optional, signal, computed } from '@angular/core';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TitleCasePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { FormsModule } from '@angular/forms';
import { LicenciementService } from '../../core/services/licenciement.service';
import { LicenciementResponse } from '../../core/models/licenciement.model';
import { LicenciementValidityDetection, PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';

interface CritereForm {
  code: string;
  label: string;
  description: string;
  bloquant: boolean;
  reponse: string;
}

type AlertLevel = 'blocker' | 'warning';
export type AlertSource = 'F96' | 'QUESTION_IA' | 'IA' | 'PIECE_MANQUANTE' | 'MULTI';

export interface CoherenceAlert {
  level: AlertLevel;
  source: AlertSource;
  expectedReponse: 'OUI' | 'NON';
  contributors: AlertSource[];
  aiReponse?: 'OUI' | 'NON' | null;
  f96Statut?: 'VERIFIED' | 'NON_COMPLIANT' | null;
  f96Raison?: string | null;
  questionText?: string | null;
  questionAnswer?: string | null;
  pieceTexte?: string | null;
  justification?: string | null;
}

const CRITERE_CODES = new Set([
  'FR_CONVOCATION', 'FR_ENTRETIEN', 'FR_DELAI_NOTIFICATION', 'FR_MOTIVATION',
  'FR_MOTIF_REEL', 'FR_PROCEDURE_DISCIPLINAIRE', 'FR_ORDRE_LICENCIEMENT',
  'BE_NOTIFICATION', 'BE_PREAVIS', 'BE_MOTIVATION', 'BE_AUDITION',
  'BE_NON_DISCRIMINATION', 'BE_PROTECTION_SPECIALE', 'BE_INDEMNITE_MANIFESTE',
]);

@Component({
  selector: 'app-licenciement-section',
  standalone: true,
  imports: [
    FormsModule, TitleCasePipe,
    MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule, MatInputModule,
    MatProgressSpinnerModule, MatRadioModule,
    MatTooltipModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './licenciement-section.component.html',
  styleUrl: './licenciement-section.component.scss'
})
export class LicenciementSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: string = 'FRANCE';
  @Input() aiData?: LicenciementValidityDetection | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private hasSavedResult = false;
  private aiDataSignal = signal<LicenciementValidityDetection | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<LicenciementResponse | null>(null);

  country = signal('FRANCE');
  criteresForm = signal<CritereForm[]>([]);

  readonly criteresReferentiel: Record<string, CritereForm[]> = {
    FRANCE: [
      { code: 'FR_CONVOCATION', label: 'Convocation entretien préalable', description: 'LRAR ou remise en main propre, 5 jours ouvrables', bloquant: true, reponse: 'INCONNU' },
      { code: 'FR_ENTRETIEN', label: 'Tenue entretien préalable', description: 'Entretien effectué, possibilité d\'assistance', bloquant: true, reponse: 'INCONNU' },
      { code: 'FR_DELAI_NOTIFICATION', label: 'Délai de notification', description: '2 jours ouvrables après entretien (7j cadre)', bloquant: false, reponse: 'INCONNU' },
      { code: 'FR_MOTIVATION', label: 'Motivation de la lettre', description: 'Motifs précis et matériellement vérifiables', bloquant: true, reponse: 'INCONNU' },
      { code: 'FR_MOTIF_REEL', label: 'Motif réel et sérieux', description: 'Objectif, exact et suffisamment grave', bloquant: true, reponse: 'INCONNU' },
      { code: 'FR_PROCEDURE_DISCIPLINAIRE', label: 'Procédure disciplinaire', description: 'Faits < 2 mois, pas de double sanction', bloquant: false, reponse: 'INCONNU' },
      { code: 'FR_ORDRE_LICENCIEMENT', label: 'Ordre des licenciements', description: 'Critères ancienneté, charges, qualités (éco)', bloquant: false, reponse: 'INCONNU' },
    ],
    BELGIQUE: [
      { code: 'BE_NOTIFICATION', label: 'Notification du licenciement', description: 'LRAR ou exploit d\'huissier', bloquant: true, reponse: 'INCONNU' },
      { code: 'BE_PREAVIS', label: 'Délai de préavis', description: 'Selon ancienneté (loi 26/12/2013)', bloquant: true, reponse: 'INCONNU' },
      { code: 'BE_MOTIVATION', label: 'Motivation (CCT 109)', description: 'Comportement, aptitude ou nécessité entreprise', bloquant: true, reponse: 'INCONNU' },
      { code: 'BE_AUDITION', label: 'Audition préalable', description: 'Recommandée (non obligatoire sauf CCE)', bloquant: false, reponse: 'INCONNU' },
      { code: 'BE_NON_DISCRIMINATION', label: 'Absence discrimination', description: 'Pas de critère protégé', bloquant: true, reponse: 'INCONNU' },
      { code: 'BE_PROTECTION_SPECIALE', label: 'Absence protection spéciale', description: 'Délégué syndical, grossesse, crédit-temps', bloquant: true, reponse: 'INCONNU' },
      { code: 'BE_INDEMNITE_MANIFESTE', label: 'Risque licenciement déraisonnable', description: 'Indemnité 3-17 semaines (CCT 109)', bloquant: false, reponse: 'INCONNU' },
    ]
  };

  scoreColor = computed(() => {
    const r = this.result();
    if (!r) return '#6B7A8D';
    if (r.scoreRisque < 15) return '#27AE60';
    if (r.scoreRisque < 40) return '#F59E0B';
    if (r.scoreRisque < 70) return '#E67E22';
    return '#C0392B';
  });

  coherenceAlerts = computed<Record<string, CoherenceAlert>>(() => {
    if (!this.showForm()) return {};
    const detections = this.aiDataSignal()?.detections;
    const f96Index = this.buildF96Index(this.procedureChecksSignal());
    const questionsIndex = this.buildQuestionsIndex(this.aiQuestionsSignal());
    const piecesIndex = this.buildPiecesIndex(this.piecesManquantesSignal());

    const alerts: Record<string, CoherenceAlert> = {};
    for (const c of this.criteresForm()) {
      if (c.reponse === 'INCONNU') continue;

      // B : F-96 validé
      const f96 = f96Index[c.code];
      if (f96) {
        const expected: 'OUI' | 'NON' = f96.statut === 'VERIFIED' ? 'OUI' : 'NON';
        if (c.reponse === expected) continue;
        const extras = this.collectSupportingSources(c.code, expected, questionsIndex, detections, piecesIndex, 'F96');
        alerts[c.code] = this.multiOrSingle({
          level: 'blocker',
          primary: 'F96',
          expectedReponse: expected,
          f96Statut: f96.statut,
          f96Raison: f96.raison ?? null,
          extraContributors: extras.extraContributors,
          questionText: extras.questionText,
          questionAnswer: extras.questionAnswer,
          aiReponse: extras.aiReponse,
          justification: extras.justification,
          pieceTexte: extras.pieceTexte,
        });
        continue;
      }

      // C : question IA répondue interprétable
      const q = questionsIndex[c.code];
      if (q && q.expected) {
        if (c.reponse === q.expected) continue;
        const extras = this.collectSupportingSources(c.code, q.expected, null, detections, piecesIndex, 'QUESTION_IA');
        alerts[c.code] = this.multiOrSingle({
          level: 'blocker',
          primary: 'QUESTION_IA',
          expectedReponse: q.expected,
          questionText: q.text,
          questionAnswer: q.answer,
          extraContributors: extras.extraContributors,
          aiReponse: extras.aiReponse,
          justification: extras.justification,
          pieceTexte: extras.pieceTexte,
        });
        continue;
      }

      // D : détection IA
      const detected = detections?.[c.code];
      if (detected) {
        const aiReponse = detected.reponse;
        if (aiReponse === 'OUI' || aiReponse === 'NON') {
          if (c.reponse !== aiReponse) {
            const extras = this.collectSupportingSources(c.code, aiReponse, null, null, piecesIndex, 'IA');
            alerts[c.code] = this.multiOrSingle({
              level: c.bloquant ? 'blocker' : 'warning',
              primary: 'IA',
              expectedReponse: aiReponse,
              aiReponse,
              justification: detected.justification?.trim() || 'Aucune justification fournie',
              extraContributors: extras.extraContributors,
              pieceTexte: extras.pieceTexte,
            });
            continue;
          }
        }
      }

      // E : pièce manquante taggée — warning si avocat a coché OUI
      const piece = piecesIndex[c.code];
      if (piece && c.reponse === 'OUI') {
        alerts[c.code] = {
          level: 'warning',
          source: 'PIECE_MANQUANTE',
          expectedReponse: 'NON',
          pieceTexte: piece,
          contributors: ['PIECE_MANQUANTE'],
        };
      }
    }
    return alerts;
  });

  private multiOrSingle(args: {
    level: AlertLevel;
    primary: AlertSource;
    expectedReponse: 'OUI' | 'NON';
    f96Statut?: 'VERIFIED' | 'NON_COMPLIANT' | null;
    f96Raison?: string | null;
    questionText?: string | null;
    questionAnswer?: string | null;
    aiReponse?: 'OUI' | 'NON' | null;
    justification?: string | null;
    pieceTexte?: string | null;
    extraContributors?: AlertSource[];
  }): CoherenceAlert {
    const contributors: AlertSource[] = [args.primary, ...(args.extraContributors ?? [])];
    const isMulti = contributors.length > 1;
    return {
      level: args.level,
      source: isMulti ? 'MULTI' : args.primary,
      expectedReponse: args.expectedReponse,
      contributors,
      f96Statut: args.f96Statut ?? null,
      f96Raison: args.f96Raison ?? null,
      questionText: args.questionText ?? null,
      questionAnswer: args.questionAnswer ?? null,
      aiReponse: args.aiReponse ?? null,
      justification: args.justification ?? null,
      pieceTexte: args.pieceTexte ?? null,
    };
  }

  private collectSupportingSources(
    code: string,
    expected: 'OUI' | 'NON',
    questionsIndex: Record<string, { expected: 'OUI' | 'NON' | null; text: string; answer: string }> | null,
    detections: Record<string, { reponse: string; justification?: string | null }> | null | undefined,
    piecesIndex: Record<string, string> | null,
    primary: AlertSource,
  ): {
    extraContributors: AlertSource[];
    questionText?: string | null;
    questionAnswer?: string | null;
    aiReponse?: 'OUI' | 'NON' | null;
    justification?: string | null;
    pieceTexte?: string | null;
  } {
    const extras: AlertSource[] = [];
    let questionText: string | null = null, questionAnswer: string | null = null;
    let aiReponse: 'OUI' | 'NON' | null = null, justification: string | null = null;
    let pieceTexte: string | null = null;

    if (primary !== 'QUESTION_IA' && questionsIndex) {
      const q = questionsIndex[code];
      if (q && q.expected === expected) {
        extras.push('QUESTION_IA');
        questionText = q.text;
        questionAnswer = q.answer;
      }
    }
    if (primary !== 'IA' && detections) {
      const d = detections[code];
      if (d && (d.reponse === 'OUI' || d.reponse === 'NON') && d.reponse === expected) {
        extras.push('IA');
        aiReponse = d.reponse;
        justification = d.justification?.trim() || null;
      }
    }
    if (primary !== 'PIECE_MANQUANTE' && piecesIndex && expected === 'NON') {
      const p = piecesIndex[code];
      if (p) {
        extras.push('PIECE_MANQUANTE');
        pieceTexte = p;
      }
    }

    return { extraContributors: extras, questionText, questionAnswer, aiReponse, justification, pieceTexte };
  }

  private buildF96Index(checks: ProcedureCheck[]): Record<string, { statut: 'VERIFIED' | 'NON_COMPLIANT', raison?: string | null }> {
    const index: Record<string, { statut: 'VERIFIED' | 'NON_COMPLIANT', raison?: string | null }> = {};
    if (!checks || checks.length === 0) return index;
    for (const chk of checks) {
      const code = chk.critereCode?.toUpperCase();
      if (!code || !CRITERE_CODES.has(code)) continue;
      if (chk.statut !== 'VERIFIED' && chk.statut !== 'NON_COMPLIANT') continue;
      const existing = index[code];
      if (!existing || (existing.statut === 'VERIFIED' && chk.statut === 'NON_COMPLIANT')) {
        index[code] = { statut: chk.statut, raison: chk.raison };
      }
    }
    return index;
  }

  private buildQuestionsIndex(questions: AiQuestion[]): Record<string, { expected: 'OUI' | 'NON' | null; text: string; answer: string }> {
    const perCode: Record<string, { expected: 'OUI' | 'NON' | null; text: string; answer: string; conflict: boolean }> = {};
    if (!questions) return {};
    for (const q of questions) {
      const code = q.critereCode?.toUpperCase();
      if (!code || !CRITERE_CODES.has(code)) continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      let expected: 'OUI' | 'NON' | null = null;
      if (answer === 'oui' || answer.startsWith('oui ') || answer.startsWith('oui,') || answer.startsWith('oui.')) expected = 'OUI';
      else if (answer === 'non' || answer.startsWith('non ') || answer.startsWith('non,') || answer.startsWith('non.')) expected = 'NON';
      if (!expected) continue;
      const existing = perCode[code];
      if (!existing) {
        perCode[code] = { expected, text: q.questionText, answer: q.answerText ?? '', conflict: false };
      } else if (!existing.conflict && existing.expected !== expected) {
        existing.conflict = true;
      }
    }
    const out: Record<string, { expected: 'OUI' | 'NON' | null; text: string; answer: string }> = {};
    for (const [code, v] of Object.entries(perCode)) {
      if (v.conflict) continue;
      out[code] = { expected: v.expected, text: v.text, answer: v.answer };
    }
    return out;
  }

  private buildPiecesIndex(pieces: PieceManquanteEntry[]): Record<string, string> {
    const index: Record<string, string> = {};
    if (!pieces) return index;
    for (const p of pieces) {
      const code = p.critereCode?.toUpperCase();
      if (!code || !CRITERE_CODES.has(code)) continue;
      if (!index[code]) index[code] = p.texte;
    }
    return index;
  }

  alertsSummary = computed(() => {
    const alerts = Object.values(this.coherenceAlerts());
    return {
      total: alerts.length,
      blockers: alerts.filter(a => a.level === 'blocker').length,
    };
  });

  // SF-IA-03-15b — map {sourceKey → explanation} pour le popover enrichi
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private licenciementService: LicenciementService,
    private sourceExplanationService: SourceExplanationService,
    private snackBar: MatSnackBar,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    this.country.set(this.workspaceCountry);
    this.criteresForm.set(this.buildInitialForm(this.country()));
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.loadExisting();
    this.loadSourceExplanations();
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: map => this.sourceExplanations.set(map),
      error: () => { /* fail-open : popover tombe en fallback template */ },
    });
  }

  /** SF-IA-03-15b : lookup explanation par code critère F96 (ex. FR_CONVOCATION). */
  explanationFor(critereCode: string): SourceExplanation[] {
    return this.sourceExplanations().get(critereCode) ?? [];
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aiData']) {
      this.aiDataSignal.set(this.aiData);
      if (!changes['aiData'].firstChange) {
        this.applyAiPrefillIfPossible();
      }
    }
    if (changes['procedureChecks']) {
      this.procedureChecksSignal.set(this.procedureChecks ?? []);
    }
    if (changes['aiQuestions']) {
      this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    }
    if (changes['piecesManquantes']) {
      this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    }
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  alertTooltip(alert: CoherenceAlert): string {
    const parts: string[] = [];
    for (const src of alert.contributors) {
      if (src === 'F96') {
        const statut = alert.f96Statut === 'VERIFIED' ? 'vérifié' : 'non respecté';
        const detail = alert.f96Raison ? ` (${alert.f96Raison})` : '';
        parts.push(`Checklist procédurale : ${statut}${detail}`);
      } else if (src === 'QUESTION_IA') {
        parts.push(`Question complémentaire : "${alert.questionText}" → réponse "${alert.questionAnswer}"`);
      } else if (src === 'IA') {
        const just = alert.justification ? ` — ${alert.justification}` : '';
        parts.push(`Analyse du dossier : ${alert.aiReponse}${just}`);
      } else if (src === 'PIECE_MANQUANTE') {
        parts.push(`Pièce manquante : ${alert.pieceTexte}`);
      }
    }
    return parts.length > 1 ? `Contredit ${parts.join(' ET ')}` : parts[0] ?? '';
  }

  alertBadgeLabel(alert: CoherenceAlert): string {
    const prefix = (() => {
      switch (alert.source) {
        case 'F96': return 'Incohérence Checklist procédurale';
        case 'QUESTION_IA': return 'Incohérence Question complémentaire';
        case 'IA': return 'Incohérence détectée';
        case 'PIECE_MANQUANTE': return 'Pièce manquante';
        case 'MULTI': return 'Incohérence multiple';
      }
    })();
    return `${prefix} (${alert.expectedReponse})`;
  }

  onReponseChange(code: string, value: string): void {
    this.criteresForm.update(list =>
      list.map(c => c.code === code ? { ...c, reponse: value } : c)
    );
  }

  loadExisting(): void {
    this.loading.set(true);
    this.licenciementService.get(this.caseFileId).subscribe({
      next: resp => {
        this.hasSavedResult = true;
        this.result.set(resp);
        this.country.set(resp.country);
        this.prefillForm(resp);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.hasSavedResult = false;
        this.showForm.set(true);
        this.loading.set(false);
        this.applyAiPrefillIfPossible();
      },
    });
  }

  analyze(): void {
    this.analyzing.set(true);
    const reponses: Record<string, string> = {};
    for (const c of this.criteresForm()) {
      reponses[c.code] = c.reponse;
    }
    this.licenciementService.analyze(this.caseFileId, {
      country: this.country(),
      reponses,
    }).subscribe({
      next: resp => {
        this.result.set(resp);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.refreshService?.triggerRefresh();
      },
      error: () => {
        this.analyzing.set(false);
        this.snackBar.open('Erreur lors de l\'analyse', 'Fermer', { duration: 4000 });
      },
    });
  }

  editForm(): void {
    const r = this.result();
    if (r) this.prefillForm(r);
    this.showForm.set(true);
  }

  private prefillForm(resp: LicenciementResponse): void {
    const form = (this.criteresReferentiel[resp.country] || []).map(c => {
      const found = resp.criteres.find(rc => rc.code === c.code);
      return { ...c, reponse: found ? found.reponse : 'INCONNU' };
    });
    this.criteresForm.set(form);
  }

  private buildInitialForm(country: string): CritereForm[] {
    return (this.criteresReferentiel[country] || this.criteresReferentiel['FRANCE'])
      .map(c => ({ ...c, reponse: 'INCONNU' }));
  }

  private applyAiPrefillIfPossible(): void {
    if (this.hasSavedResult) return;
    const detections = this.aiData?.detections;
    if (!detections) return;
    const current = this.criteresForm();
    if (current.length === 0) return;
    const next = current.map(c => {
      const detected = detections[c.code];
      if (detected && (detected.reponse === 'OUI' || detected.reponse === 'NON')) {
        return { ...c, reponse: detected.reponse };
      }
      return c;
    });
    this.criteresForm.set(next);
  }
}
