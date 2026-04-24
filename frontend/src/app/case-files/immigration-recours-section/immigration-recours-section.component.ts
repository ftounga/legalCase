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
import { ImmigrationRecoursService } from '../../core/services/immigration-recours.service';
import { RecoursResponse } from '../../core/models/immigration-recours.model';
import { PdfExportService } from '../../core/services/pdf-export.service';
import { ImmigrationExtractedData, PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert, CoherenceAlertSource } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';

const VALID_RECOURS_CODES = new Set([
  'RECOURS_GRACIEUX_PREFET', 'RECOURS_CONTENTIEUX_TA', 'RECOURS_CNDA',
  'RECOURS_CGRA', 'RECOURS_CCE', 'RECOURS_CE_BELGIQUE',
]);

export type IM06AlertField = 'RECOURS_TYPE' | 'DATE_NOTIFICATION';
// SF-155-13 : alias rétro-compat — utilise l'interface générique partagée.
export type IM06AlertSource = CoherenceAlertSource;
export type IM06CoherenceAlert = CoherenceAlert<IM06AlertField>;

@Component({
  selector: 'app-immigration-recours-section',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule,
    MatInputModule, MatProgressSpinnerModule,
    MatTooltipModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './immigration-recours-section.component.html',
  styleUrl: './immigration-recours-section.component.scss'
})
export class ImmigrationRecoursSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() caseFileTitle: string = '';
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private aiDataSignal = signal<ImmigrationExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  collapsed = signal(true);
  loading = signal(false);
  generating = signal(false);
  showForm = signal(true);
  recours = signal<RecoursResponse | null>(null);

  // Form fields
  recoursType = signal('RECOURS_GRACIEUX_PREFET');
  dateNotification = signal('');

  // Provenance notes (SF-IM-06-04)
  provenanceRecoursType = signal<'IA' | null>(null);
  provenanceDateNotification = signal<'IA' | null>(null);
  nom = signal('');
  prenom = signal('');
  nationalite = signal('');
  adresse = signal('');
  autorite = signal('');
  dateDecision = signal('');
  reference = signal('');
  exposeFaits = signal('');

  readonly recoursTypes = [
    { value: 'RECOURS_GRACIEUX_PREFET', label: 'Recours gracieux (Préfet)', country: 'FR' },
    { value: 'RECOURS_CONTENTIEUX_TA', label: 'Recours contentieux (TA)', country: 'FR' },
    { value: 'RECOURS_CNDA', label: 'Recours CNDA (asile)', country: 'FR' },
    { value: 'RECOURS_CGRA', label: 'Recours CGRA', country: 'BE' },
    { value: 'RECOURS_CCE', label: 'Recours CCE', country: 'BE' },
    { value: 'RECOURS_CE_BELGIQUE', label: 'Recours Conseil d\'État', country: 'BE' },
  ];

  // SF-IA-03-15c — map {sourceKey → explanation}
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private recoursService: ImmigrationRecoursService,
    private sourceExplanationService: SourceExplanationService,
    private snackBar: MatSnackBar,
    private pdfExportService: PdfExportService,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: map => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15c : mapping vers sourceKey F96 + générique. */
  explanationFor(field: IM06AlertField): SourceExplanation[] {
    const key = field === 'RECOURS_TYPE' ? 'IM06_RECOURS_TYPE' : 'date_notification_decision_contestee';
    return this.sourceExplanations().get(key) ?? [];
  }

  coherenceAlerts = computed<Partial<Record<IM06AlertField, IM06CoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<IM06AlertField, IM06CoherenceAlert>> = {};
    const typeAlert = this.buildRecoursTypeAlert();
    if (typeAlert) alerts.RECOURS_TYPE = typeAlert;
    const dateAlert = this.buildDateAlert();
    if (dateAlert) alerts.DATE_NOTIFICATION = dateAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  ngOnInit(): void {
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.loadExisting();
    this.loadSourceExplanations();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (changes['aiData'] && this.showForm() && !this.recours()) {
      this.prefillFromAi();
    }
  }

  private buildPiecesIndex(pieces: PieceManquanteEntry[]): Record<string, string> {
    const index: Record<string, string> = {};
    if (!pieces) return index;
    for (const p of pieces) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (code !== 'IM06_RECOURS_TYPE') continue;
      if (!index[code]) index[code] = p.texte;
    }
    return index;
  }

  alertTooltip(alert: IM06CoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: IM06CoherenceAlert): string {
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
   * SF-155-13 : migration vers CoherenceAlertBuilder partagé.
   * Logique métier inchangée : scan F96 → QUESTION_IA → IA, la 1re source fixe
   * `expectedDisplay`, les suivantes consolident en `MULTI` si même valeur ;
   * la pièce manquante éventuelle est attachée en contributor additionnel.
   */
  private buildRecoursTypeAlert(): IM06CoherenceAlert | null {
    const user = this.recoursType();
    if (!user) return null;
    const piecesIndex = this.buildPiecesIndex(this.piecesManquantesSignal());
    const pieceTexte = piecesIndex['IM06_RECOURS_TYPE'] ?? null;

    const builder = CoherenceAlertBuilder.forField<IM06AlertField>('RECOURS_TYPE');

    // F-96 VERIFIED
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'IM06_RECOURS_TYPE') continue;
      if (chk.statut !== 'VERIFIED') continue;
      const ev = chk.expectedValue?.toUpperCase();
      if (!ev || !VALID_RECOURS_CODES.has(ev)) continue;
      if (ev !== user) {
        builder.addSource('F96', {
          expectedDisplay: ev,
          reason: `Checklist procédurale : ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
        });
      }
      break;
    }

    // Question IA "oui"
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== 'IM06_RECOURS_TYPE') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ') || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue?.toUpperCase();
      if (!ev || !VALID_RECOURS_CODES.has(ev)) continue;
      if (ev === user) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // IA typeRecoursCode
    const iaCode = this.aiDataSignal()?.typeRecoursCode?.toUpperCase();
    if (iaCode && VALID_RECOURS_CODES.has(iaCode) && iaCode !== user) {
      builder.addSource('IA', {
        expectedDisplay: iaCode,
        reason: `Analyse du dossier : ${iaCode}`,
      });
    }

    // PIECE_MANQUANTE — contributor uniquement si alerte déjà initiée.
    if (pieceTexte) builder.addPieceManquante(pieceTexte);

    return builder.build();
  }

  /**
   * SF-155-13 : migration vers CoherenceAlertBuilder partagé.
   * Logique métier inchangée : alerte IA si écart > 7 jours entre date saisie
   * et date IA. Source unique `IA` — pas de consolidation multi-sources ici.
   */
  private buildDateAlert(): IM06CoherenceAlert | null {
    const userDate = this.dateNotification();
    const iaDate = this.aiDataSignal()?.dateNotificationDecisionContestee;
    if (!userDate || !iaDate) return null;
    const tUser = Date.parse(userDate);
    const tIa = Date.parse(iaDate);
    if (Number.isNaN(tUser) || Number.isNaN(tIa)) return null;
    const diffDays = Math.abs(tUser - tIa) / 86400000;
    if (diffDays <= 7) return null;
    return CoherenceAlertBuilder.forField<IM06AlertField>('DATE_NOTIFICATION')
      .addSource('IA', {
        expectedDisplay: iaDate,
        reason: `Analyse du dossier : date de notification détectée le ${iaDate} (écart de ${Math.round(diffDays)} jours)`,
      })
      .build();
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  loadExisting(): void {
    this.loading.set(true);
    this.recoursService.get(this.caseFileId).subscribe({
      next: resp => {
        this.recours.set(resp);
        this.prefillForm(resp);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.showForm.set(true);
        this.loading.set(false);
        this.prefillFromAi();
      },
    });
  }

  private prefillFromAi(): void {
    if (!this.aiData) return;
    const code = this.aiData.typeRecoursCode?.toUpperCase();
    if (code && VALID_RECOURS_CODES.has(code)) {
      this.recoursType.set(code);
      this.provenanceRecoursType.set('IA');
    }
    const date = this.aiData.dateNotificationDecisionContestee;
    if (date && /^\d{4}-\d{2}-\d{2}$/.test(date)) {
      this.dateNotification.set(date);
      this.provenanceDateNotification.set('IA');
    }
  }

  onRecoursTypeChange(): void { this.provenanceRecoursType.set(null); }
  onDateNotificationChange(): void { this.provenanceDateNotification.set(null); }

  generate(): void {
    this.generating.set(true);
    this.recoursService.generate(this.caseFileId, {
      recoursType: this.recoursType(),
      dateNotification: this.dateNotification(),
      requerant: {
        nom: this.nom(),
        prenom: this.prenom(),
        nationalite: this.nationalite(),
        adresse: this.adresse(),
      },
      decisionContestee: {
        autorite: this.autorite(),
        date: this.dateDecision(),
        reference: this.reference() || null,
      },
      exposeFaits: this.exposeFaits() || null,
    }).subscribe({
      next: resp => {
        this.recours.set(resp);
        this.showForm.set(false);
        this.generating.set(false);
        this.refreshService?.triggerRefresh();
      },
      error: () => {
        this.generating.set(false);
        this.snackBar.open('Erreur lors de la génération du recours', 'Fermer', { duration: 4000 });
      },
    });
  }

  editForm(): void {
    const r = this.recours();
    if (r) this.prefillForm(r);
    this.showForm.set(true);
  }

  exportPdf(): void {
    const r = this.recours();
    if (!r) return;
    this.pdfExportService.exportRecoursImmigration(r, this.caseFileTitle);
  }

  private prefillForm(resp: RecoursResponse): void {
    this.recoursType.set(resp.recoursType);
    this.dateNotification.set(resp.dateNotification);
    this.nom.set(resp.requerant.nom);
    this.prenom.set(resp.requerant.prenom);
    this.nationalite.set(resp.requerant.nationalite);
    this.adresse.set(resp.requerant.adresse);
    this.autorite.set(resp.decisionContestee.autorite);
    this.dateDecision.set(resp.decisionContestee.date);
    this.reference.set(resp.decisionContestee.reference ?? '');
    this.exposeFaits.set(resp.exposeFaits ?? '');
  }
}
