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
import { CoherenceAlert, CoherenceAlertSource } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

/**
 * SF-155-17 : fields d'alerte F-IA-03 exposés par l'outil F-DT-10
 * (validité rupture conventionnelle). Un par critère auditable.
 */
export type RCAlertField =
  | 'RC_CONSENTEMENT'
  | 'RC_DELAI_RETRACTATION'
  | 'RC_HOMOLOGATION'
  | 'RC_ASSISTANCE'
  | 'RC_INDEMNITE'
  | 'RC_ENTRETIENS';

interface CritereForm {
  code: RCAlertField;
  label: string;
  description: string;
  bloquant: boolean;
}

// SF-155-17 : alias rétro-compat — utilise l'interface générique partagée (SF-155-05).
export type RCAlertSource = CoherenceAlertSource;
export type RCCoherenceAlert = CoherenceAlert<RCAlertField>;

const RC_CODES: ReadonlySet<RCAlertField> = new Set<RCAlertField>([
  'RC_CONSENTEMENT', 'RC_DELAI_RETRACTATION', 'RC_HOMOLOGATION',
  'RC_ASSISTANCE', 'RC_INDEMNITE', 'RC_ENTRETIENS',
]);

const RC_BLOCKERS: ReadonlySet<RCAlertField> = new Set<RCAlertField>([
  'RC_CONSENTEMENT', 'RC_DELAI_RETRACTATION', 'RC_HOMOLOGATION', 'RC_INDEMNITE',
]);

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
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'VALIDITÉ DE LA RUPTURE CONVENTIONNELLE';
  static readonly TOOL_ICON = 'gavel';

  @Input() caseFileId!: string;
  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
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

  // SF-155-17 : provenance IA par critère. 'IA' quand le champ a été pré-rempli
  // par `prefillFromAi()` ; remis à null dès que l'avocat modifie manuellement
  // (handler `onReponseChange`). Pattern canonique harcèlement / immigration.
  provenanceReponses = signal<Record<string, 'IA' | null>>(this.defaultProvenance());

  private aiDataSignal = signal<RuptureConvValidityDetection | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  coherenceAlerts = computed<Partial<Record<RCAlertField, RCCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<RCAlertField, RCCoherenceAlert>> = {};
    const detections = this.aiDataSignal()?.detections;
    const f96Index = this.buildF96Index(this.procedureChecksSignal());
    const questionsIndex = this.buildQuestionsIndex(this.aiQuestionsSignal());
    const piecesIndex = this.buildPiecesIndex(this.piecesManquantesSignal());

    const reponses = this.reponses();
    for (const c of this.criteres) {
      const code = c.code as RCAlertField;
      const reponse = reponses[code];
      if (!reponse || reponse === 'INCONNU') continue;
      const severity = RC_BLOCKERS.has(code) ? 'CRITICAL' : 'WARNING';

      // Hiérarchie F-IA-03 : F96 > QUESTION_IA > IA > PIECE_MANQUANTE.
      // Utilise CoherenceAlertBuilder (SF-155-05) — le premier `expected` gagne,
      // les sources ultérieures avec même expected deviennent `contributors`.
      const f96 = f96Index[code];
      const q = questionsIndex[code];
      const det = detections?.[code];
      const piece = piecesIndex[code];

      const f96Expected = f96 ? (f96.statut === 'VERIFIED' ? 'OUI' : 'NON') : null;
      const questionExpected = q?.expected ?? null;
      const iaExpected: 'OUI' | 'NON' | null =
        det && (det.reponse === 'OUI' || det.reponse === 'NON') ? det.reponse : null;

      // Déterminer la source primaire (celle qui impose l'expected) dans l'ordre
      // de hiérarchie. Si aucune des 3 sources "fortes" n'est en divergence, on
      // tombe en E (pièce manquante seule — warning sur OUI).
      let primaryExpected: 'OUI' | 'NON' | null = null;
      if (f96Expected && f96Expected !== reponse) primaryExpected = f96Expected;
      else if (questionExpected && questionExpected !== reponse) primaryExpected = questionExpected;
      else if (iaExpected && iaExpected !== reponse) primaryExpected = iaExpected;

      if (primaryExpected) {
        const builder = CoherenceAlertBuilder.forField<RCAlertField>(code)
          .withSeverity(severity as 'CRITICAL' | 'WARNING');

        if (f96Expected === primaryExpected) {
          const statut = f96!.statut === 'VERIFIED' ? 'vérifié' : 'non respecté';
          const detail = f96!.raison ? ` (${f96!.raison})` : '';
          builder.addSource('F96', {
            expectedDisplay: primaryExpected,
            reason: `Checklist procédurale : ${statut}${detail}`,
          });
        }
        if (questionExpected === primaryExpected) {
          builder.addSource('QUESTION_IA', {
            expectedDisplay: primaryExpected,
            reason: `Question complémentaire : "${q!.text}" → réponse "${q!.answer}"`,
          });
        }
        if (iaExpected === primaryExpected) {
          const just = det!.justification?.trim() || 'Aucune justification fournie';
          builder.addSource('IA', {
            expectedDisplay: primaryExpected,
            reason: `Analyse du dossier : ${iaExpected}${just ? ' — ' + just : ''}`,
          });
        }
        // PIECE_MANQUANTE : contributor additionnel, seulement pertinent quand
        // l'expected canonique est NON (une pièce manquante pousse vers NON).
        if (piece && primaryExpected === 'NON') {
          builder.addPieceManquante(piece);
        }

        const alert = builder.build();
        if (alert) alerts[code] = alert;
        continue;
      }

      // E — pièce manquante seule : warning si avocat a répondu OUI et aucune
      // des 3 sources fortes n'a été contradictoire par ailleurs.
      if (piece && reponse === 'OUI') {
        const builder = CoherenceAlertBuilder.forField<RCAlertField>(code)
          .withSeverity('WARNING')
          .addSource('PIECE_MANQUANTE', {
            expectedDisplay: 'NON',
            reason: `Pièce manquante : ${piece}`,
            pieceTexte: piece,
          });
        const alert = builder.build();
        if (alert) alerts[code] = alert;
      }
    }
    return alerts;
  });

  alertsSummary = computed(() => {
    const alerts = Object.values(this.coherenceAlerts());
    return {
      total: alerts.length,
      blockers: alerts.filter(a => a!.severity === 'CRITICAL').length,
    };
  });

  // SF-IA-03-15b — map {sourceKey → explanation} pour le popover enrichi
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

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
  explanationFor(critereCode: string): SourceExplanation[] {
    return this.sourceExplanations().get(critereCode) ?? [];
  }

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);

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
        // SF-155-17 : valeurs persistées = saisie avocat — jamais de badge IA.
        this.provenanceReponses.set(this.defaultProvenance());
        this.loading.set(false);
      },
      error: () => {
        this.showForm.set(true);
        this.loading.set(false);
        this.prefillFromAi();
      },
    });
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    if (changes['aiData']) {
      this.aiDataSignal.set(this.aiData);
      // SF-155-17 : ré-appliquer le pré-fill quand `aiData` arrive avant
      // première résolution backend et que l'avocat n'a pas encore répondu.
      if (!changes['aiData'].firstChange && this.showForm() && !this.result()) {
        this.prefillFromAi();
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

  /**
   * SF-155-17 : handler manuel — efface le badge IA dès que l'avocat modifie
   * le choix. Pattern canonique identique à harcelement/immigration sections.
   */
  onReponseChange(code: string, value: Reponse): void {
    this.reponses.update(r => ({ ...r, [code]: value }));
    this.provenanceReponses.update(p => ({ ...p, [code]: null }));
  }

  /**
   * @deprecated SF-155-17 : conservé pour compat tests existants — préférer
   * `onReponseChange` qui efface aussi la provenance IA.
   */
  setReponse(code: string, value: Reponse): void {
    this.onReponseChange(code, value);
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
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
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
    return `${prefix} (${alert.expectedDisplay})`;
  }

  /**
   * SF-155-17 : alias rétro-compat — `level` était 'blocker'|'warning', on
   * mappe depuis `severity` pour les classes CSS existantes.
   */
  alertLevelClass(alert: RCCoherenceAlert): 'blocker' | 'warning' {
    return alert.severity === 'CRITICAL' ? 'blocker' : 'warning';
  }

  private buildF96Index(checks: ProcedureCheck[]): Record<string, { statut: 'VERIFIED' | 'NON_COMPLIANT', raison?: string | null }> {
    const index: Record<string, { statut: 'VERIFIED' | 'NON_COMPLIANT', raison?: string | null }> = {};
    if (!checks || checks.length === 0) return index;
    for (const chk of checks) {
      const code = chk.critereCode?.toUpperCase();
      if (!code || !RC_CODES.has(code as RCAlertField)) continue;
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
      if (!code || !RC_CODES.has(code as RCAlertField)) continue;
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
      if (!code || !RC_CODES.has(code as RCAlertField)) continue;
      if (!index[code]) index[code] = p.texte;
    }
    return index;
  }

  /**
   * SF-155-17 : pré-remplit les réponses depuis `aiData.detections`.
   * N'écrase jamais une saisie avocat existante (provenance null).
   * Marque `provenanceReponses[code] = 'IA'` pour afficher le badge.
   */
  private prefillFromAi(): void {
    const detections = this.aiDataSignal()?.detections;
    if (!detections) return;
    const currentReponses = this.reponses();
    const currentProv = this.provenanceReponses();
    const nextReponses: Record<string, Reponse> = { ...currentReponses };
    const nextProv: Record<string, 'IA' | null> = { ...currentProv };
    for (const code of Object.keys(detections)) {
      if (!RC_CODES.has(code as RCAlertField)) continue;
      const r = detections[code].reponse;
      if (r !== 'OUI' && r !== 'NON' && r !== 'INCONNU') continue;
      // Pré-fill uniquement si la case est INCONNU (vide) OU déjà pré-remplie par IA.
      if (currentReponses[code] === 'INCONNU' || currentProv[code] === 'IA') {
        nextReponses[code] = r;
        nextProv[code] = r === 'INCONNU' ? null : 'IA';
      }
    }
    this.reponses.set(nextReponses);
    this.provenanceReponses.set(nextProv);
  }

  private defaultReponses(): Record<string, Reponse> {
    const r: Record<string, Reponse> = {};
    for (const c of CRITERES_FR) r[c.code] = 'INCONNU';
    return r;
  }

  private defaultProvenance(): Record<string, 'IA' | null> {
    const p: Record<string, 'IA' | null> = {};
    for (const c of CRITERES_FR) p[c.code] = null;
    return p;
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
