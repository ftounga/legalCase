import { Component, OnInit, OnDestroy, signal, ViewChild, ElementRef } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe, UpperCasePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { CaseFileService } from '../../core/services/case-file.service';
import { DocumentService } from '../../core/services/document.service';
import { AnalysisJobService } from '../../core/services/analysis-job.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { AiQuestionService } from '../../core/services/ai-question.service';
import { AiQuestionAnswerService } from '../../core/services/ai-question-answer.service';
import { ReAnalysisService } from '../../core/services/re-analysis.service';
import { CaseFile } from '../../core/models/case-file.model';
import { Document } from '../../core/models/document.model';
import { AnalysisJob } from '../../core/models/analysis-job.model';
import { CaseAnalysisResult } from '../../core/models/case-analysis.model';
import { AiQuestion } from '../../core/models/ai-question.model';

@Component({
  selector: 'app-case-file-detail',
  standalone: true,
  imports: [
    RouterLink, DatePipe, UpperCasePipe,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatProgressSpinnerModule, MatProgressBarModule, MatDividerModule
  ],
  templateUrl: './case-file-detail.component.html',
  styleUrl: './case-file-detail.component.scss'
})
export class CaseFileDetailComponent implements OnInit, OnDestroy {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  caseFile = signal<CaseFile | null>(null);
  documents = signal<Document[]>([]);
  analysisJobs = signal<AnalysisJob[]>([]);
  synthesis = signal<CaseAnalysisResult | null>(null);
  questions = signal<AiQuestion[]>([]);
  loading = signal(true);
  uploading = signal(false);
  submittingAnswer = signal<string | null>(null);
  reAnalyzing = signal(false);

  readonly docColumns = ['name', 'type', 'size', 'date', 'actions'];

  private pollingInterval: ReturnType<typeof setInterval> | null = null;

  constructor(
    private route: ActivatedRoute,
    private caseFileService: CaseFileService,
    private documentService: DocumentService,
    private analysisJobService: AnalysisJobService,
    private caseAnalysisService: CaseAnalysisService,
    private aiQuestionService: AiQuestionService,
    private aiQuestionAnswerService: AiQuestionAnswerService,
    private reAnalysisService: ReAnalysisService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.caseFileService.getById(id).subscribe({
      next: cf => {
        this.caseFile.set(cf);
        this.loading.set(false);
        this.loadDocuments(id);
        this.loadAnalysisJobs(id);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Dossier introuvable', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  loadDocuments(caseFileId: string): void {
    this.documentService.list(caseFileId).subscribe({
      next: docs => this.documents.set(docs),
      error: () => this.snackBar.open('Erreur lors du chargement des documents', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      })
    });
  }

  loadAnalysisJobs(caseFileId: string): void {
    this.analysisJobService.getJobs(caseFileId).subscribe({
      next: jobs => {
        this.analysisJobs.set(jobs);
        this.managePolling(caseFileId, jobs);
        if (jobs.some(j => j.jobType === 'CASE_ANALYSIS' && j.status === 'DONE')) {
          this.loadSynthesis(caseFileId);
        }
        if (jobs.some(j => j.jobType === 'QUESTION_GENERATION' && j.status === 'DONE')) {
          this.loadQuestions(caseFileId);
        }
      },
      error: () => {
        // Silencieux — pas de section Analyse IA affichée
      }
    });
  }

  private managePolling(caseFileId: string, jobs: AnalysisJob[]): void {
    const hasPendingOrProcessing = jobs.some(
      j => j.status === 'PENDING' || j.status === 'PROCESSING'
    );
    const analysisStarting = jobs.length === 0 && this.documents().length > 0 && !this.synthesis();

    if ((hasPendingOrProcessing || analysisStarting) && !this.pollingInterval) {
      this.pollingInterval = setInterval(() => {
        this.analysisJobService.getJobs(caseFileId).subscribe({
          next: updated => {
            this.analysisJobs.set(updated);
            const stillRunning = updated.some(
              j => j.status === 'PENDING' || j.status === 'PROCESSING'
            );
            const stillWaiting = updated.length === 0 && this.documents().length > 0 && !this.synthesis();
            if (!stillRunning && !stillWaiting) {
              this.stopPolling();
            }
          }
        });
      }, 3000);
    } else if (!hasPendingOrProcessing && !analysisStarting) {
      this.stopPolling();
    }
  }

  private stopPolling(): void {
    if (this.pollingInterval) {
      clearInterval(this.pollingInterval);
      this.pollingInterval = null;
      const caseFileId = this.caseFile()?.id;
      if (caseFileId) {
        const caseAnalysisDone = this.analysisJobs().some(
          j => j.jobType === 'CASE_ANALYSIS' && j.status === 'DONE'
        );
        if (caseAnalysisDone) {
          this.loadSynthesis(caseFileId);
        }
        const questionGenerationDone = this.analysisJobs().some(
          j => j.jobType === 'QUESTION_GENERATION' && j.status === 'DONE'
        );
        if (questionGenerationDone) {
          this.loadQuestions(caseFileId);
        }
        const enrichedAnalysisDone = this.analysisJobs().some(
          j => j.jobType === 'ENRICHED_ANALYSIS' && j.status === 'DONE'
        );
        if (enrichedAnalysisDone) {
          this.loadSynthesis(caseFileId);
          this.loadQuestions(caseFileId);
        }
      }
    }
  }

  loadQuestions(caseFileId: string): void {
    this.aiQuestionService.getQuestions(caseFileId).subscribe({
      next: qs => this.questions.set(qs),
      error: () => { /* silencieux */ }
    });
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

  reAnalyze(): void {
    const caseFileId = this.caseFile()?.id;
    if (!caseFileId) return;
    this.reAnalyzing.set(true);
    this.reAnalysisService.reAnalyze(caseFileId).subscribe({
      next: () => {
        this.reAnalyzing.set(false);
        this.loadAnalysisJobs(caseFileId);
      },
      error: (err: any) => {
        this.reAnalyzing.set(false);
        if (err.status === 402) return;
        this.snackBar.open('Erreur lors du déclenchement de la re-analyse', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }

  hasAnsweredQuestions(): boolean {
    return this.questions().some(q => q.answerText !== null);
  }

  loadSynthesis(caseFileId: string): void {
    this.caseAnalysisService.getAnalysis(caseFileId).subscribe({
      next: result => this.synthesis.set(result),
      error: () => { /* silencieux */ }
    });
  }

  jobTypeLabel(jobType: string): string {
    const labels: Record<string, string> = {
      CHUNK_ANALYSIS: 'Analyse des segments',
      DOCUMENT_ANALYSIS: 'Analyse des documents',
      CASE_ANALYSIS: 'Synthèse du dossier',
      QUESTION_GENERATION: 'Génération des questions',
      ENRICHED_ANALYSIS: 'Re-synthèse enrichie'
    };
    return labels[jobType] ?? jobType;
  }

  jobStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PENDING: 'En attente',
      PROCESSING: 'En cours',
      DONE: 'Terminé',
      FAILED: 'Erreur'
    };
    return labels[status] ?? status;
  }

  jobStatusClass(status: string): string {
    const classes: Record<string, string> = {
      PENDING: 'badge--warning',
      PROCESSING: 'badge--success',
      DONE: 'badge--success',
      FAILED: 'badge--error'
    };
    return classes[status] ?? '';
  }

  triggerUpload(): void {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    const caseFileId = this.caseFile()!.id;
    this.uploading.set(true);
    this.documentService.upload(caseFileId, file).subscribe({
      next: doc => {
        this.documents.update(docs => [doc, ...docs]);
        this.uploading.set(false);
        this.snackBar.open('Document ajouté', 'Fermer', {
          duration: 3000, panelClass: ['snack-success']
        });
        input.value = '';
        this.loadAnalysisJobs(caseFileId);
      },
      error: (err: any) => {
        this.uploading.set(false);
        input.value = '';
        if (err.status === 402) return;
        this.snackBar.open("Erreur lors de l'upload. Vérifiez le type et la taille du fichier (max 50 Mo).", 'Fermer', {
          duration: 5000, panelClass: ['snack-error']
        });
      }
    });
  }

  downloadUrl(doc: Document): string {
    return this.documentService.downloadUrl(this.caseFile()!.id, doc.id);
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} o`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`;
  }

  statusLabel(status: string): string {
    return status === 'OPEN' ? 'Ouvert' : status;
  }
}
