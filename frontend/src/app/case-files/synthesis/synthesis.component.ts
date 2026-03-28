import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
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
import { PdfExportService } from '../../core/services/pdf-export.service';
import { CaseFile } from '../../core/models/case-file.model';
import { CaseAnalysisResult, CaseAnalysisVersionSummary } from '../../core/models/case-analysis.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { ChatMessage } from '../../core/models/chat-message.model';

@Component({
  selector: 'app-synthesis',
  standalone: true,
  imports: [
    RouterLink, DatePipe, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatExpansionModule,
    MatCheckboxModule, MatTooltipModule
  ],
  templateUrl: './synthesis.component.html',
  styleUrl: './synthesis.component.scss'
})
export class SynthesisComponent implements OnInit {
  caseFile = signal<CaseFile | null>(null);
  synthesis = signal<CaseAnalysisResult | null>(null);
  versions = signal<CaseAnalysisVersionSummary[]>([]);
  questions = signal<AiQuestion[]>([]);
  loading = signal(true);
  reAnalyzing = signal(false);
  submittingAnswer = signal<string | null>(null);
  editingQuestionId = signal<string | null>(null);
  submittingEdit = signal<string | null>(null);

  chatMessages = signal<ChatMessage[]>([]);
  chatLoading = signal(false);
  chatDisabled = signal(false);
  chatQuestion = '';
  useEnriched = false;

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
    private pdfExportService: PdfExportService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.caseFileService.getById(id).subscribe({
      next: cf => {
        this.caseFile.set(cf);
        this.loadVersions(id);
        this.loadChatHistory(id);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Dossier introuvable', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
      }
    });
  }

  private loadVersions(caseFileId: string): void {
    this.caseAnalysisService.getVersions(caseFileId).subscribe({
      next: versions => {
        this.versions.set(versions);
        if (versions.length > 0) {
          this.loadSynthesisForVersion(caseFileId, versions[0].version);
          this.loadQuestionsForVersion(caseFileId, versions[0].id);
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

  onVersionChange(versionNumber: number): void {
    const caseFileId = this.caseFile()?.id;
    if (!caseFileId) return;
    const selected = this.versions().find(v => v.version === versionNumber);
    if (!selected) return;
    this.synthesis.set(null);
    this.questions.set([]);
    this.editingQuestionId.set(null);
    this.loadSynthesisForVersion(caseFileId, selected.version);
    this.loadQuestionsForVersion(caseFileId, selected.id);
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
    } catch {
      this.snackBar.open('Erreur lors de la génération du PDF', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      });
    }
  }

  reAnalyze(): void {
    const id = this.caseFile()?.id;
    if (!id) return;
    this.reAnalyzing.set(true);
    this.reAnalysisService.reAnalyze(id).subscribe({
      next: () => {
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
