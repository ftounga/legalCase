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
import { CoherenceAlert, CoherenceAlertSource } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { ProcedureCheckAlignment } from '../../core/models/procedure-check-alignment.model';
import { ProcedureChecksOutputComponent } from '../decisional-tools-panel/procedure-checks-output/procedure-checks-output.component';
import { computeBadge, ProcedureChecksBadge } from '../decisional-tools-panel/procedure-check-badge.helper';

interface CritereForm {
  code: string;
  label: string;
  description: string;
  bloquant: boolean;
  reponse: string;
}

/**
 * SF-155-16 : Alert field type pour F-DT-08 (validité licenciement).
 *
 * Le code critère est utilisé comme field (chaîne dynamique parmi
 * `CRITERE_CODES`) — pattern similaire à celui des autres composants
 * à N critères uniformes.
 */
export type LicenciementAlertField = string;

// SF-155-16 : alias local — utilise l'interface générique partagée
// (plus de définition locale d'`CoherenceAlert`).
export type AlertSource = CoherenceAlertSource;
export type LicenciementCoherenceAlert = CoherenceAlert<LicenciementAlertField>;

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
    ProcedureChecksOutputComponent,
  ],
  templateUrl: './licenciement-section.component.html',
  styleUrl: './licenciement-section.component.scss'
})
export class LicenciementSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03 : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'VALIDITÉ DU LICENCIEMENT';
  static readonly TOOL_ICON = 'gavel';

  /**
   * F-193 SF-193-02 — Pattern miroir de `getRetainedPistesBadge` (F-192) :
   * permet au panel F-IA-04 d'afficher le pill `🔍 Procédure (V/N/T)` AVANT
   * instanciation du composant. La liste reçue est déjà filtrée par le
   * panel via `inputs(ctx)` sur `toolIdCible === <toolId courant>`.
   */
  static getProcedureChecksBadge(input: {
    proceduresChecksAlignment?: ProcedureCheckAlignment[] | null;
  }): ProcedureChecksBadge {
    return computeBadge(Array.isArray(input.proceduresChecksAlignment) ? input.proceduresChecksAlignment : []);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: string = 'FRANCE';
  @Input() aiData?: LicenciementValidityDetection | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  /**
   * F-193 SF-193-02 — Alignement des checks F-96 sur cet outil (déjà
   * pré-filtré par le panel via TOOL_REGISTRY.inputs(ctx)).
   */
  @Input() proceduresChecksAlignment?: ProcedureCheckAlignment[] | null;
  // F-177 SF-177-03 : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

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

  // SF-155-16 : provenance IA par critère pré-rempli (badge "auto_awesome").
  // Effacé dès que l'avocat modifie manuellement (onReponseChange).
  provenanceByCode = signal<Record<string, 'IA' | null>>({});

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

  /**
   * SF-155-16 : alertes de cohérence F-IA-03 calculées via `CoherenceAlertBuilder`.
   * Hiérarchie préservée (F-96 > QUESTION_IA > IA > PIECE_MANQUANTE) :
   *   - F-96 attendu fixe `expectedDisplay` → autres sources convergeant
   *     sur la même valeur deviennent `contributors` additionnels (source `MULTI`).
   *   - Pièce manquante seule sur critère OUI → alerte warning attendant NON.
   * Gate anti-bug SF-IA-03-12 : aucune alerte si `showForm()=false`.
   */
  coherenceAlerts = computed<Record<string, LicenciementCoherenceAlert>>(() => {
    if (!this.showForm()) return {};
    const detections = this.aiDataSignal()?.detections;
    const f96Index = this.buildF96Index(this.procedureChecksSignal());
    const questionsIndex = this.buildQuestionsIndex(this.aiQuestionsSignal());
    const piecesIndex = this.buildPiecesIndex(this.piecesManquantesSignal());

    const alerts: Record<string, LicenciementCoherenceAlert> = {};
    for (const c of this.criteresForm()) {
      if (c.reponse === 'INCONNU') continue;

      const alert = this.buildCritereAlert(c, f96Index, questionsIndex, detections, piecesIndex);
      if (alert) alerts[c.code] = alert;
    }
    return alerts;
  });

  /**
   * SF-155-16 : construit l'alerte F-IA-03 pour un critère donné via le
   * builder partagé. Première source = F96 NON_COMPLIANT > F96 VERIFIED >
   * QUESTION_IA "oui/non" interprétable > IA détection > PIECE_MANQUANTE
   * (seule si avocat a coché OUI).
   */
  private buildCritereAlert(
    c: CritereForm,
    f96Index: Record<string, { statut: 'VERIFIED' | 'NON_COMPLIANT'; raison?: string | null }>,
    questionsIndex: Record<string, { expected: 'OUI' | 'NON' | null; text: string; answer: string }>,
    detections: Record<string, { reponse: string; justification?: string | null }> | undefined,
    piecesIndex: Record<string, string>,
  ): LicenciementCoherenceAlert | null {
    const builder = CoherenceAlertBuilder.forField<string>(c.code);
    let initiated = false;

    // 1. F-96 — priorité absolue. Si F-96 concorde avec l'avocat, aucune
    //    alerte n'est levée même si IA/QUESTION_IA divergent (règle
    //    historique : F-96 prime sur tout).
    const f96 = f96Index[c.code];
    if (f96) {
      const f96Expected: 'OUI' | 'NON' = f96.statut === 'VERIFIED' ? 'OUI' : 'NON';
      if (c.reponse === f96Expected) {
        return null;
      }
      const statutLabel = f96.statut === 'VERIFIED' ? 'vérifié' : 'non respecté';
      const detail = f96.raison ? ` (${f96.raison})` : '';
      builder
        .withSeverity('CRITICAL')
        .addSource('F96', {
          expectedDisplay: f96Expected,
          reason: `Checklist procédurale : ${statutLabel}${detail}`,
        });
      initiated = true;
    }

    // 2. QUESTION_IA — "oui"/"non" interprétable
    const q = questionsIndex[c.code];
    if (q && q.expected && c.reponse !== q.expected) {
      if (initiated) {
        // Contributor additionnel — consolidé seulement si même expected.
        builder.addSource('QUESTION_IA', {
          expectedDisplay: q.expected,
          reason: `Question complémentaire : "${q.text}" → réponse "${q.answer}"`,
        });
      } else {
        builder
          .withSeverity('CRITICAL')
          .addSource('QUESTION_IA', {
            expectedDisplay: q.expected,
            reason: `Question complémentaire : "${q.text}" → réponse "${q.answer}"`,
          });
        initiated = true;
      }
    }

    // 3. IA — détection dossier
    const detected = detections?.[c.code];
    if (detected && (detected.reponse === 'OUI' || detected.reponse === 'NON')) {
      const iaExpected = detected.reponse;
      if (c.reponse !== iaExpected) {
        const justification = detected.justification?.trim() || 'Aucune justification fournie';
        if (initiated) {
          builder.addSource('IA', {
            expectedDisplay: iaExpected,
            reason: `Analyse du dossier : ${iaExpected} — ${justification}`,
          });
        } else {
          // IA devient source primaire. Sévérité = CRITICAL si critère
          // bloquant, WARNING sinon (l'IA seule fait varier la sévérité
          // selon `bloquant` — F96 et QUESTION_IA sont toujours CRITICAL).
          builder
            .withSeverity(c.bloquant ? 'CRITICAL' : 'WARNING')
            .addSource('IA', {
              expectedDisplay: iaExpected,
              reason: `Analyse du dossier : ${iaExpected} — ${justification}`,
            });
          initiated = true;
        }
      }
    }

    // 4. PIECE_MANQUANTE — contributor additionnel si alerte déjà initiée,
    //    ou alerte seule (WARNING attendant NON) si avocat a coché OUI.
    const piece = piecesIndex[c.code];
    if (piece) {
      if (initiated) {
        builder.addPieceManquante(piece, `Pièce manquante : ${piece}`);
      } else if (c.reponse === 'OUI') {
        builder
          .withSeverity('WARNING')
          .addSource('PIECE_MANQUANTE', {
            expectedDisplay: 'NON',
            reason: `Pièce manquante : ${piece}`,
            pieceTexte: piece,
          });
        initiated = true;
      }
    }

    return builder.build();
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
      // SF-155-16 : blocker = alerte CRITICAL (équivalent historique de
      // `level === 'blocker'`) ; ce sont les divergences sur critères
      // bloquants OU venant de F-96 / QUESTION_IA (toujours critiques).
      blockers: alerts.filter(a => a.severity === 'CRITICAL').length,
    };
  });

  /**
   * SF-155-16 : nombre de critères pré-remplis par l'IA (provenance `IA`
   * encore active). Utilisé pour afficher un badge "Pré-remplissage IA"
   * en haut du formulaire et un indicateur dans le header de section.
   */
  prefillSummary = computed(() => {
    const map = this.provenanceByCode();
    const count = Object.values(map).filter(v => v === 'IA').length;
    return { count };
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
    // F-177 SF-177-03 : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);

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
    // F-177 SF-177-03 : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);

    if (changes['aiData']) {
      this.aiDataSignal.set(this.aiData);
      if (!changes['aiData'].firstChange) {
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

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  alertTooltip(alert: LicenciementCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: LicenciementCoherenceAlert): string {
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
   * SF-155-16 : handler change critère — efface le badge IA dès que
   * l'avocat modifie manuellement (pattern canonique).
   */
  onReponseChange(code: string, value: string): void {
    this.criteresForm.update(list =>
      list.map(c => c.code === code ? { ...c, reponse: value } : c)
    );
    this.provenanceByCode.update(map => ({ ...map, [code]: null }));
  }

  /** SF-155-16 : helper template — true si critère pré-rempli par l'IA. */
  isPrefilledByAi(code: string): boolean {
    return this.provenanceByCode()[code] === 'IA';
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
        this.prefillFromAi();
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
    // SF-155-16 : valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceByCode.set({});
  }

  private buildInitialForm(country: string): CritereForm[] {
    return (this.criteresReferentiel[country] || this.criteresReferentiel['FRANCE'])
      .map(c => ({ ...c, reponse: 'INCONNU' }));
  }

  /**
   * SF-155-16 : pré-remplit les critères depuis l'analyse IA
   * (`aiData.detections`). N'écrase jamais une saisie avocat existante
   * (valeurs non-INCONNU déjà en place). Trace la provenance `IA` par
   * critère pour affichage du badge "Pré-rempli depuis l'analyse".
   *
   * Appelé dans `ngOnInit()` (via `loadExisting` 404 fallback) ET dans
   * `ngOnChanges()` si `aiData` change avant première résolution.
   */
  private prefillFromAi(): void {
    if (this.hasSavedResult) return;
    const detections = this.aiData?.detections;
    if (!detections) return;
    const current = this.criteresForm();
    if (current.length === 0) return;

    const newProvenance: Record<string, 'IA' | null> = { ...this.provenanceByCode() };
    const next = current.map(c => {
      const detected = detections[c.code];
      if (detected && (detected.reponse === 'OUI' || detected.reponse === 'NON')) {
        // Ne pré-rempli QUE si champ encore INCONNU OU déjà marqué IA
        // (préserve les edits avocats faits après un premier pré-fill).
        if (c.reponse === 'INCONNU' || this.provenanceByCode()[c.code] === 'IA') {
          newProvenance[c.code] = 'IA';
          return { ...c, reponse: detected.reponse };
        }
      }
      return c;
    });
    this.criteresForm.set(next);
    this.provenanceByCode.set(newProvenance);
  }
}
