import { Component, Input, OnChanges, OnInit, Optional, signal, computed, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RuptureConvService } from '../../core/services/rupture-conv.service';
import { RuptureConvResponse } from '../../core/models/rupture-conv.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { RuptureConvValidityDetection, PieceManquanteEntry } from '../../core/models/case-analysis.model';
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
}

export type RCAlertLevel = 'blocker' | 'warning';
export type RCAlertSource = 'F96' | 'QUESTION_IA' | 'IA' | 'PIECE_MANQUANTE' | 'MULTI';

export interface RCCoherenceAlert {
  level: RCAlertLevel;
  source: RCAlertSource;
  expectedReponse: 'OUI' | 'NON';
  contributors: RCAlertSource[];
  f96Statut?: 'VERIFIED' | 'NON_COMPLIANT' | null;
  f96Raison?: string | null;
  questionText?: string | null;
  questionAnswer?: string | null;
  aiReponse?: 'OUI' | 'NON' | null;
  justification?: string | null;
  pieceTexte?: string | null;
}

const RC_CODES = new Set([
  'RC_CONSENTEMENT', 'RC_DELAI_RETRACTATION', 'RC_HOMOLOGATION',
  'RC_ASSISTANCE', 'RC_INDEMNITE', 'RC_ENTRETIENS',
]);

const RC_BLOCKERS = new Set(['RC_CONSENTEMENT', 'RC_DELAI_RETRACTATION', 'RC_HOMOLOGATION', 'RC_INDEMNITE']);

const CRITERES_FR: CritereForm[] = [
  { code: 'RC_CONSENTEMENT', label: 'Consentement libre et éclairé', bloquant: true,
    description: 'Le consentement du salarié est-il exempt de vice (pression, dol, violence morale, erreur) ? '
      + 'Des échanges, pièces ou circonstances laissent-ils présumer une contrainte ?' },
  { code: 'RC_DELAI_RETRACTATION', label: 'Délai de rétractation de 15 jours calendaires respecté', bloquant: true,
    description: 'Un délai de 15 jours calendaires a-t-il été respecté entre la signature de la convention et '
      + 'la demande d\'homologation adressée à l\'administration ?' },
  { code: 'RC_HOMOLOGATION', label: 'Homologation par la DREETS (ex-DIRECCTE)', bloquant: true,
    description: 'La demande d\'homologation a-t-elle été déposée et l\'homologation obtenue '
      + '(ou réputée acquise après 15 jours ouvrables sans réponse) ?' },
  { code: 'RC_ASSISTANCE', label: 'Assistance possible et documentée', bloquant: false,
    description: 'Le salarié a-t-il été informé de sa possibilité de se faire assister (avocat, conseiller du salarié, '
      + 'représentant du personnel) lors des entretiens, et cette assistance est-elle documentée ?' },
  { code: 'RC_INDEMNITE', label: 'Indemnité spécifique supérieure ou égale à l\'indemnité légale', bloquant: true,
    description: 'L\'indemnité spécifique de rupture conventionnelle est-elle au moins égale à l\'indemnité légale '
      + 'de licenciement (¼ de mois par année les 10 premières années + ⅓ au-delà) ?' },
  { code: 'RC_ENTRETIENS', label: 'Au moins un entretien préalable tenu et documenté', bloquant: false,
    description: 'Un entretien préalable a-t-il été organisé entre l\'employeur et le salarié avant signature, '
      + 'et existe-t-il une trace écrite (compte-rendu, correspondance) ?' },
];

type Reponse = 'OUI' | 'NON' | 'INCONNU';

@Component({
  selector: 'app-rupture-conv-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatRadioModule, MatTooltipModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './rupture-conv-section.component.html',
  styleUrl: './rupture-conv-section.component.scss'
})
export class RuptureConvSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() aiData?: RuptureConvValidityDetection | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  readonly criteres = CRITERES_FR;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  reponses = signal<Record<string, Reponse>>(this.defaultReponses());
  result = signal<RuptureConvResponse | null>(null);

  private aiDataSignal = signal<RuptureConvValidityDetection | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  coherenceAlerts = computed<Record<string, RCCoherenceAlert>>(() => {
    if (!this.showForm()) return {};
    const detections = this.aiDataSignal()?.detections;
    const f96Index = this.buildF96Index(this.procedureChecksSignal());
    const questionsIndex = this.buildQuestionsIndex(this.aiQuestionsSignal());
    const piecesIndex = this.buildPiecesIndex(this.piecesManquantesSignal());

    const alerts: Record<string, RCCoherenceAlert> = {};
    const reponses = this.reponses();
    for (const c of this.criteres) {
      const code = c.code;
      const reponse = reponses[code];
      if (!reponse || reponse === 'INCONNU') continue;
      const level: RCAlertLevel = RC_BLOCKERS.has(code) ? 'blocker' : 'warning';

      // B : F-96 validé
      const f96 = f96Index[code];
      if (f96) {
        const expected: 'OUI' | 'NON' = f96.statut === 'VERIFIED' ? 'OUI' : 'NON';
        if (reponse === expected) continue;
        const extras = this.collectSupportingSources(code, expected, questionsIndex, detections, piecesIndex, 'F96');
        alerts[code] = this.multiOrSingle({
          level,
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
      const q = questionsIndex[code];
      if (q && q.expected) {
        if (reponse === q.expected) continue;
        const extras = this.collectSupportingSources(code, q.expected, null, detections, piecesIndex, 'QUESTION_IA');
        alerts[code] = this.multiOrSingle({
          level,
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
      const detected = detections?.[code];
      if (detected) {
        const aiReponse = detected.reponse;
        if (aiReponse === 'OUI' || aiReponse === 'NON') {
          if (reponse !== aiReponse) {
            const extras = this.collectSupportingSources(code, aiReponse, null, null, piecesIndex, 'IA');
            alerts[code] = this.multiOrSingle({
              level,
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
      const piece = piecesIndex[code];
      if (piece && reponse === 'OUI') {
        alerts[code] = {
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

  alertsSummary = computed(() => {
    const alerts = Object.values(this.coherenceAlerts());
    return {
      total: alerts.length,
      blockers: alerts.filter(a => a.level === 'blocker').length,
    };
  });

  // SF-IA-03-15b — map {sourceKey → explanation} pour le popover enrichi
  sourceExplanations = signal<Map<string, SourceExplanation>>(new Map());

  constructor(
    private rcService: RuptureConvService,
    private sourceExplanationService: SourceExplanationService,
    private snackBar: MatSnackBar,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: map => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15b : lookup par code critère RC_* (même sourceKey côté Haiku). */
  explanationFor(critereCode: string): SourceExplanation | null {
    return this.sourceExplanations().get(critereCode) ?? null;
  }

  ngOnInit(): void {
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.loadSourceExplanations();

    this.loading.set(true);
    this.rcService.get(this.caseFileId).subscribe({
      next: resp => {
        this.result.set(resp);
        this.showForm.set(false);
        this.applyReponsesFromResult(resp);
        this.loading.set(false);
      },
      error: () => {
        this.showForm.set(true);
        this.loading.set(false);
        this.applyAiPrefill();
      },
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aiData']) {
      this.aiDataSignal.set(this.aiData);
      if (!changes['aiData'].firstChange && this.showForm() && !this.result()) {
        this.applyAiPrefill();
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

  toggleCollapsed(): void { this.collapsed.update(v => !v); }

  setReponse(code: string, value: Reponse): void {
    this.reponses.update(r => ({ ...r, [code]: value }));
  }

  analyze(): void {
    this.analyzing.set(true);
    this.rcService.analyze(this.caseFileId, {
      country: 'FRANCE',
      reponses: this.reponses(),
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

  editForm(): void { this.showForm.set(true); }

  verdictColor(verdict: string | undefined): string {
    switch (verdict) {
      case 'VALIDE': return '#27AE60';
      case 'RISQUE_MODERE': return '#F59E0B';
      case 'RISQUE_ELEVE': return '#E67E22';
      case 'INVALIDE': return '#C0392B';
      default: return '#6B7A8D';
    }
  }

  verdictLabel(verdict: string | undefined): string {
    switch (verdict) {
      case 'VALIDE': return 'Valide';
      case 'RISQUE_MODERE': return 'Risque modéré';
      case 'RISQUE_ELEVE': return 'Risque élevé';
      case 'INVALIDE': return 'Invalide';
      default: return '';
    }
  }

  scoreBarWidth(score: number): string {
    return Math.max(0, Math.min(100, score)) + '%';
  }

  alertTooltip(alert: RCCoherenceAlert): string {
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

  alertBadgeLabel(alert: RCCoherenceAlert): string {
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

  private multiOrSingle(args: {
    level: RCAlertLevel;
    primary: RCAlertSource;
    expectedReponse: 'OUI' | 'NON';
    f96Statut?: 'VERIFIED' | 'NON_COMPLIANT' | null;
    f96Raison?: string | null;
    questionText?: string | null;
    questionAnswer?: string | null;
    aiReponse?: 'OUI' | 'NON' | null;
    justification?: string | null;
    pieceTexte?: string | null;
    extraContributors?: RCAlertSource[];
  }): RCCoherenceAlert {
    const contributors: RCAlertSource[] = [args.primary, ...(args.extraContributors ?? [])];
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
    primary: RCAlertSource,
  ): {
    extraContributors: RCAlertSource[];
    questionText?: string | null;
    questionAnswer?: string | null;
    aiReponse?: 'OUI' | 'NON' | null;
    justification?: string | null;
    pieceTexte?: string | null;
  } {
    const extras: RCAlertSource[] = [];
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
      if (!code || !RC_CODES.has(code)) continue;
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
      if (!code || !RC_CODES.has(code)) continue;
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
      if (!code || !RC_CODES.has(code)) continue;
      if (!index[code]) index[code] = p.texte;
    }
    return index;
  }

  private applyAiPrefill(): void {
    const detections = this.aiData?.detections;
    if (!detections) return;
    for (const code of Object.keys(detections)) {
      if (!RC_CODES.has(code)) continue;
      const r = detections[code].reponse;
      if (r === 'OUI' || r === 'NON' || r === 'INCONNU') {
        this.setReponse(code, r);
      }
    }
  }

  private defaultReponses(): Record<string, Reponse> {
    const r: Record<string, Reponse> = {};
    for (const c of CRITERES_FR) r[c.code] = 'INCONNU';
    return r;
  }

  private applyReponsesFromResult(resp: RuptureConvResponse): void {
    const r: Record<string, Reponse> = { ...this.defaultReponses() };
    for (const c of resp.criteres) {
      if (c.reponse === 'OUI' || c.reponse === 'NON' || c.reponse === 'INCONNU') {
        r[c.code] = c.reponse;
      }
    }
    this.reponses.set(r);
  }
}
