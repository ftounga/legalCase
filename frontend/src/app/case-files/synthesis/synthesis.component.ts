import { Component, OnInit, computed, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { AiQuestionService } from '../../core/services/ai-question.service';
import { AiQuestionAnswerService } from '../../core/services/ai-question-answer.service';
import { ReAnalysisService } from '../../core/services/re-analysis.service';
import { GlobalAnalysisNotificationService } from '../../core/services/global-analysis-notification.service';
import { ChatService } from '../../core/services/chat.service';
import { AnalyticsService } from '../../core/services/analytics.service';
import { PdfExportService } from '../../core/services/pdf-export.service';
import { DocxExportService } from '../../core/services/docx-export.service';
import { ProcedureCheckService } from '../../core/services/procedure-check.service';
import { CaseFile } from '../../core/models/case-file.model';
import { fadeInUp, listStagger } from '../../shared/animations';
import { SourceRefComponent } from '../../shared/source-ref/source-ref.component';
import { CaseAnalysisResult, CaseAnalysisVersionSummary, CompensationEstimate, PensionAlimentaireEstimate, PrestationCompensatoireEstimate, LiquidationCommunaute } from '../../core/models/case-analysis.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { ChatMessage } from '../../core/models/chat-message.model';
import { ProcedureCheck, ProcedureCheckStatus } from '../../core/models/procedure-check.model';
import { TimeService } from '../../core/services/time.service';
import { TimeEntryResponse } from '../../core/models/time-tracking.models';

@Component({
  selector: 'app-synthesis',
  standalone: true,
  imports: [
    RouterLink, DatePipe, LowerCasePipe, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatExpansionModule,
    MatCheckboxModule, MatTooltipModule,
    SourceRefComponent
  ],
  templateUrl: './synthesis.component.html',
  styleUrl: './synthesis.component.scss',
  animations: [fadeInUp, listStagger],
  host: { '[@fadeInUp]': '' },
})
export class SynthesisComponent implements OnInit {
  caseFile = signal<CaseFile | null>(null);
  synthesis = signal<CaseAnalysisResult | null>(null);
  timeEntries = signal<TimeEntryResponse[]>([]);

  readonly totalBilledSeconds = computed(() =>
    this.timeEntries()
      .filter(e => e.durationSeconds != null)
      .reduce((acc, e) => acc + e.durationSeconds!, 0)
  );

  readonly showInsight = computed(() =>
    this.synthesis()?.riskLevel != null && this.totalBilledSeconds() > 0
  );

  readonly insightText = computed(() => {
    const level = this.synthesis()?.riskLevel;
    if (!level || this.totalBilledSeconds() === 0) return '';
    const labels: Record<string, string> = { FAIBLE: 'Faible', MOYEN: 'Moyen', ELEVE: 'Élevé' };
    const levelLabel = labels[level] ?? level;
    return `Ce dossier représente ${this.formatInsightDuration(this.totalBilledSeconds())} de travail enregistré — risque ${levelLabel}. Pensez à vérifier votre honoraire.`;
  });
  versions = signal<CaseAnalysisVersionSummary[]>([]);
  questions = signal<AiQuestion[]>([]);
  loading = signal(true);
  reAnalyzing = signal(false);
  submittingAnswer = signal<string | null>(null);
  editingQuestionId = signal<string | null>(null);
  submittingEdit = signal<string | null>(null);

  procedureChecks = signal<ProcedureCheck[]>([]);
  updatingCheckId = signal<string | null>(null);

  chatMessages = signal<ChatMessage[]>([]);
  chatLoading = signal(false);
  chatDisabled = signal(false);
  chatQuestion = '';
  useEnriched = false;

  sourceMap = computed(() => {
    const map = new Map<string, string>();
    const docs = this.synthesis()?.analysisDocuments;
    if (!docs) return map;
    for (const doc of docs) {
      map.set(`Document ${doc.index}`, doc.name);
    }
    return map;
  });

  resolveSource(source: string | null): string | null {
    if (!source) return null;
    if (/^Document \d+$/i.test(source)) {
      return this.sourceMap().get(source) ?? source;
    }
    return source;
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private caseFileService: CaseFileService,
    private caseAnalysisService: CaseAnalysisService,
    private aiQuestionService: AiQuestionService,
    private aiQuestionAnswerService: AiQuestionAnswerService,
    private reAnalysisService: ReAnalysisService,
    private globalNotificationService: GlobalAnalysisNotificationService,
    private chatService: ChatService,
    private snackBar: MatSnackBar,
    private pdfExportService: PdfExportService,
    private docxExportService: DocxExportService,
    private analyticsService: AnalyticsService,
    private procedureCheckService: ProcedureCheckService,
    private timeService: TimeService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.caseFileService.getById(id).subscribe({
      next: cf => {
        this.caseFile.set(cf);
        this.loadVersions(id);
        this.loadChatHistory(id);
        this.timeService.loadEntries(id).subscribe({
          next: () => this.timeEntries.set(this.timeService.entries()),
          error: () => {}
        });
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Dossier introuvable', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
      }
    });

    // SF-IA-03-19 : lecture des query params pour scroll + highlight vers la source cliquée depuis un popover.
    this.route.queryParamMap?.subscribe(params => {
      const qa = params.get('qa');
      const check = params.get('check');
      const piece = params.get('piece');
      const chat = params.get('chat');
      const section = params.get('section');
      let anchorId: string | null = null;
      if (qa) anchorId = 'qa-' + qa;
      else if (check) anchorId = 'check-' + check;
      else if (piece !== null) anchorId = 'piece-' + piece;
      else if (chat || section === 'chat') anchorId = 'section-chat';
      else if (section === 'questions') anchorId = 'section-questions';
      else if (section === 'checklist') anchorId = 'section-checklist';
      else if (section === 'pieces') anchorId = 'section-pieces';
      if (anchorId) this.scrollAndHighlight(anchorId);
    });
  }

  /** SF-IA-03-19 : scroll vers l'ancre + highlight pulse 2s. Retry 3× car les données chargent async. */
  private scrollAndHighlight(anchorId: string, attempt = 0): void {
    const el = document.getElementById(anchorId);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el.classList.add('source-highlight');
      setTimeout(() => el.classList.remove('source-highlight'), 2100);
    } else if (attempt < 10) {
      // Les données sont async (loadVersions), retry avec backoff
      setTimeout(() => this.scrollAndHighlight(anchorId, attempt + 1), 300);
    }
  }

  private loadVersions(caseFileId: string): void {
    this.caseAnalysisService.getVersions(caseFileId).subscribe({
      next: versions => {
        this.versions.set(versions);
        if (versions.length > 0) {
          this.loadSynthesisForVersion(caseFileId, versions[0].version);
          this.loadQuestionsForVersion(caseFileId, versions[0].id);
          this.loadChecksForVersion(caseFileId, versions[0].id);
        } else {
          this.loading.set(false);
        }
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  loadSynthesisForVersion(caseFileId: string, version: number): void {
    this.caseAnalysisService.getByVersion(caseFileId, version).subscribe({
      next: result => {
        this.synthesis.set(result);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Erreur lors du chargement de la version', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
      }
    });
  }

  loadQuestionsForVersion(caseFileId: string, analysisId: string): void {
    this.aiQuestionService.getQuestionsByAnalysisId(caseFileId, analysisId).subscribe({
      next: qs => this.questions.set(qs),
      error: () => {}
    });
  }

  loadChecksForVersion(caseFileId: string, analysisId: string): void {
    this.procedureCheckService.list(caseFileId, analysisId).subscribe({
      next: checks => this.procedureChecks.set(checks),
      error: () => this.procedureChecks.set([])
    });
  }

  updateCheckStatus(check: ProcedureCheck, statut: ProcedureCheckStatus): void {
    if (this.updatingCheckId() === check.id) return;
    this.updatingCheckId.set(check.id);
    this.procedureCheckService.updateStatus(check.id, statut).subscribe({
      next: updated => {
        this.procedureChecks.update(list =>
          list.map(c => c.id === updated.id ? updated : c)
        );
        this.updatingCheckId.set(null);
      },
      error: () => {
        this.updatingCheckId.set(null);
        this.snackBar.open('Erreur lors de la mise à jour du statut', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }

  checkStatusLabel(statut: ProcedureCheckStatus): string {
    switch (statut) {
      case 'VERIFIED': return 'Vérifié';
      case 'NON_COMPLIANT': return 'Non conforme';
      default: return 'À vérifier';
    }
  }

  checkStatusIcon(statut: ProcedureCheckStatus): string {
    switch (statut) {
      case 'VERIFIED': return 'check_circle';
      case 'NON_COMPLIANT': return 'cancel';
      default: return 'help_outline';
    }
  }

  onVersionChange(versionNumber: number): void {
    const caseFileId = this.caseFile()?.id;
    if (!caseFileId) return;
    const selected = this.versions().find(v => v.version === versionNumber);
    if (!selected) return;
    this.synthesis.set(null);
    this.questions.set([]);
    this.procedureChecks.set([]);
    this.editingQuestionId.set(null);
    this.loadSynthesisForVersion(caseFileId, selected.version);
    this.loadQuestionsForVersion(caseFileId, selected.id);
    this.loadChecksForVersion(caseFileId, selected.id);
  }

  startEdit(question: AiQuestion): void {
    this.editingQuestionId.set(question.id);
  }

  cancelEdit(): void {
    this.editingQuestionId.set(null);
  }

  submitEdit(question: AiQuestion, newText: string): void {
    if (!newText.trim()) return;
    this.submittingEdit.set(question.id);
    this.aiQuestionAnswerService.submitAnswer(question.id, newText.trim()).subscribe({
      next: () => {
        this.questions.update(qs => qs.map(q =>
          q.id === question.id ? { ...q, answerText: newText.trim() } : q
        ));
        this.submittingEdit.set(null);
        this.editingQuestionId.set(null);
      },
      error: () => {
        this.submittingEdit.set(null);
        this.snackBar.open('Erreur lors de la modification de la réponse', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }

  versionLabel(v: CaseAnalysisVersionSummary): string {
    return v.analysisType === 'ENRICHED' ? `v${v.version} — Enrichie` : `v${v.version}`;
  }

  isEnriched(): boolean {
    return this.synthesis()?.analysisType === 'ENRICHED';
  }

  formatInsightDuration(seconds: number): string {
    if (seconds < 60) return '< 1min';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (h > 0) return `${h}h ${String(m).padStart(2, '0')}min`;
    return `${m}min`;
  }

  riskLabel(synthesis: CaseAnalysisResult): string {
    const labels: Record<string, string> = { FAIBLE: 'Faible', MOYEN: 'Moyen', ELEVE: 'Élevé' };
    const label = labels[synthesis.riskLevel!] ?? synthesis.riskLevel!;
    return synthesis.riskScore != null ? `${label} (${synthesis.riskScore})` : label;
  }

  riskClass(riskLevel: string | null): string {
    if (riskLevel === 'FAIBLE') return 'risk-badge risk-badge--faible';
    if (riskLevel === 'MOYEN') return 'risk-badge risk-badge--moyen';
    if (riskLevel === 'ELEVE') return 'risk-badge risk-badge--eleve';
    return '';
  }

  get compensationEstimate(): CompensationEstimate | null {
    return this.synthesis()?.compensationEstimate ?? null;
  }

  get belgianCompensationEstimate(): any | null {
    return this.synthesis()?.belgianCompensationEstimate ?? null;
  }

  /**
   * Affiche le panneau Macron FR uniquement si compensationEstimate existe ET
   * que belgianCompensationEstimate est absent. Depuis le fix F-DT-09-BE,
   * compensationEstimate est aussi renseigné pour les dossiers BE (pour
   * alimenter les alertes F-IA-03 du comparateur d'indemnités), mais ses
   * valeurs Macron ne doivent pas s'afficher dans la synthèse d'un workspace BE.
   */
  get showMacronPanel(): boolean {
    return !!this.compensationEstimate && !this.belgianCompensationEstimate;
  }

  get pensionAlimentaireEstimate(): PensionAlimentaireEstimate | null {
    return this.synthesis()?.pensionAlimentaireEstimate ?? null;
  }

  get prestationCompensatoireEstimate(): PrestationCompensatoireEstimate | null {
    return this.synthesis()?.prestationCompensatoireEstimate ?? null;
  }

  get liquidationCommunaute(): LiquidationCommunaute | null {
    return this.synthesis()?.liquidationCommunaute ?? null;
  }

  formatRegime(regime: string | null): string {
    const labels: Record<string, string> = {
      COMMUNAUTE_LEGALE:     'Communauté légale',
      SEPARATION_BIENS:      'Séparation de biens',
      PARTICIPATION_ACQUETS: 'Participation aux acquêts',
    };
    return regime ? (labels[regime] ?? regime) : 'Non détecté';
  }

  formatTypeRupture(type: string): string {
    const labels: Record<string, string> = {
      LICENCIEMENT: 'Licenciement',
      LICENCIEMENT_ECONOMIQUE: 'Licenciement économique',
      RUPTURE_CONVENTIONNELLE: 'Rupture conventionnelle',
    };
    return labels[type] ?? type;
  }

  formatAnciennete(annees: number, mois: number): string {
    if (annees === 0 && mois === 0) return 'moins d\'1 an';
    const parts: string[] = [];
    if (annees > 0) parts.push(`${annees} an${annees > 1 ? 's' : ''}`);
    if (mois > 0)   parts.push(`${mois} mois`);
    return parts.join(' ');
  }

  formatEuros(amount: number): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(amount);
  }

  formatPlafond(mois: number, salaire: number): string {
    return this.formatEuros(Math.round(mois * salaire));
  }

  private loadChatHistory(id: string): void {
    this.chatService.getHistory(id).subscribe({
      next: msgs => this.chatMessages.set(msgs),
      error: (err: any) => {
        if (err.status === 424) {
          this.chatDisabled.set(true);
        }
      }
    });
  }

  sendChatMessage(): void {
    const question = this.chatQuestion.trim();
    if (!question || this.chatLoading()) return;
    const id = this.caseFile()!.id;
    this.chatLoading.set(true);
    this.chatService.sendMessage(id, { question, useEnriched: this.useEnriched }).subscribe({
      next: msg => {
        this.chatMessages.update(msgs => [...msgs, msg]);
        this.chatQuestion = '';
        this.chatLoading.set(false);
        this.analyticsService.trackEvent('chat_message_sent', { enriched: this.useEnriched });
      },
      error: (err: any) => {
        this.chatLoading.set(false);
        if (err.status === 402) {
          this.snackBar.open('Limite de messages atteinte pour ce mois', 'Fermer', {
            duration: 4000, panelClass: ['snack-error']
          });
        } else if (err.status === 424) {
          this.chatDisabled.set(true);
        } else {
          this.snackBar.open("Erreur lors de l'envoi du message", 'Fermer', {
            duration: 4000, panelClass: ['snack-error']
          });
        }
      }
    });
  }

  hasAnsweredQuestions(): boolean {
    return this.questions().some(q => q.answerText !== null);
  }

  submitAnswer(question: AiQuestion, answerText: string): void {
    if (!answerText.trim()) return;
    this.submittingAnswer.set(question.id);
    this.aiQuestionAnswerService.submitAnswer(question.id, answerText.trim()).subscribe({
      next: () => {
        this.questions.update(qs => qs.map(q =>
          q.id === question.id ? { ...q, answerText: answerText.trim() } : q
        ));
        this.submittingAnswer.set(null);
      },
      error: (err: any) => {
        this.submittingAnswer.set(null);
        if (err.status === 402) return;
        this.snackBar.open('Erreur lors de la soumission de la réponse', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }

  exportPdf(): void {
    const cf = this.caseFile();
    const syn = this.synthesis();
    if (!cf || !syn) return;
    try {
      this.pdfExportService.export(cf, syn);
      this.analyticsService.trackEvent('pdf_exported');
    } catch {
      this.snackBar.open('Erreur lors de la génération du PDF', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      });
    }
  }

  exportDocx(): void {
    const cf = this.caseFile();
    const syn = this.synthesis();
    if (!cf || !syn) return;
    this.docxExportService.export(cf, syn);
    this.analyticsService.trackEvent('docx_exported');
  }

  exportChecklistPdf(): void {
    const cf = this.caseFile();
    if (!cf) return;
    this.pdfExportService.exportChecklist(cf, this.procedureChecks());
  }

  reAnalyze(): void {
    const id = this.caseFile()?.id;
    if (!id) return;
    this.reAnalyzing.set(true);
    this.reAnalysisService.reAnalyze(id).subscribe({
      next: () => {
        this.analyticsService.trackEvent('analysis_launched', { type: 'ENRICHED' });
        this.reAnalyzing.set(false);
        this.globalNotificationService.track(id);
        this.router.navigate(['/case-files', id]);
      },
      error: (err: any) => {
        this.reAnalyzing.set(false);
        if (err.status === 402) return;
        if (err.status === 409) {
          this.snackBar.open(
            'Aucune nouvelle réponse depuis la dernière analyse enrichie.',
            'Fermer', { duration: 6000, panelClass: ['snack-error'] }
          );
          return;
        }
        this.snackBar.open('Erreur lors du déclenchement de la re-analyse', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }
}
