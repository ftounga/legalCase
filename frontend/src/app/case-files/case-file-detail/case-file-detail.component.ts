import { Component, OnInit, OnDestroy, signal, computed, ViewChild, ElementRef } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe, UpperCasePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseFileService } from '../../core/services/case-file.service';
import { DocumentService } from '../../core/services/document.service';
import { AnalysisJobService } from '../../core/services/analysis-job.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { CaseAnalysisCommandService } from '../../core/services/case-analysis-command.service';
import { AiQuestionService } from '../../core/services/ai-question.service';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CaseFile } from '../../core/models/case-file.model';
import { Document } from '../../core/models/document.model';
import { AnalysisJob } from '../../core/models/analysis-job.model';
import { CaseAnalysisResult } from '../../core/models/case-analysis.model';

@Component({
  selector: 'app-case-file-detail',
  standalone: true,
  imports: [
    RouterLink, DatePipe, UpperCasePipe,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatProgressSpinnerModule, MatProgressBarModule
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
  analyzing = signal(false);
  synthesisLoading = signal(false);
  questionsLoading = signal(false);

  // true between upload success and first backend confirmation that new doc analysis started
  private docAnalysisPending = signal(false);

  readonly docColumns = ['name', 'type', 'size', 'date', 'actions'];
  readonly visibleJobs = computed(() => this.analysisJobs().filter(j => j.jobType !== 'CHUNK_ANALYSIS'));

  // documents uploaded after the last synthesis — not covered by the current synthesis
  readonly outdatedDocuments = computed(() => {
    const syn = this.synthesis();
    if (!syn?.updatedAt) return [];
    const synDate = new Date(syn.updatedAt);
    return this.documents().filter(d => new Date(d.createdAt) > synDate);
  });

  // true from "Analyser" click until both synthesis and questions are loaded
  readonly fullAnalysisRunning = computed(() => {
    if (this.analyzing()) return true;
    const jobs = this.analysisJobs();
    const caseJob = jobs.find(j => j.jobType === 'CASE_ANALYSIS');
    if (!caseJob) return false;
    if (caseJob.status === 'PENDING' || caseJob.status === 'PROCESSING') return true;
    if (caseJob.status === 'DONE' && !this.synthesis()) return true;
    const questionJob = jobs.find(j => j.jobType === 'QUESTION_GENERATION');
    if (!questionJob) return false;
    if (questionJob.status === 'PENDING' || questionJob.status === 'PROCESSING') return true;
    if (questionJob.status === 'DONE' && this.questions().length === 0) return true;
    return false;
  });

  private pollingInterval: ReturnType<typeof setInterval> | null = null;

  constructor(
    private route: ActivatedRoute,
    private caseFileService: CaseFileService,
    private documentService: DocumentService,
    private analysisJobService: AnalysisJobService,
    private caseAnalysisService: CaseAnalysisService,
    private caseAnalysisCommandService: CaseAnalysisCommandService,
    private aiQuestionService: AiQuestionService,
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

  loadAnalysisJobs(caseFileId: string, forceStart = false): void {
    this.analysisJobService.getJobs(caseFileId).subscribe({
      next: jobs => {
        // Don't overwrite the placeholder while waiting for the backend to pick up the new upload
        if (jobs.length > 0 && !this.docAnalysisPending()) {
          this.analysisJobs.set(jobs);
        }
        this.managePolling(caseFileId, jobs, forceStart);
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

  private managePolling(caseFileId: string, jobs: AnalysisJob[], forceStart = false): void {
    const hasPendingOrProcessing = jobs.some(
      j => j.status === 'PENDING' || j.status === 'PROCESSING'
    );

    if ((hasPendingOrProcessing || forceStart || this.docAnalysisPending()) && !this.pollingInterval) {
      this.pollingInterval = setInterval(() => {
        this.analysisJobService.getJobs(caseFileId).subscribe({
          next: updated => {
            if (updated.length === 0) return; // pipeline not started yet — keep placeholders

            // If we're waiting for the backend to pick up a new upload, check if it did.
            // Confirmed only when totalItems reflects the current document count —
            // this prevents false positives from stale PROCESSING responses in flight.
            if (this.docAnalysisPending()) {
              const docJob = updated.find(j => j.jobType === 'DOCUMENT_ANALYSIS');
              const backendPickedUp = docJob && docJob.totalItems >= this.documents().length;
              if (!backendPickedUp) {
                return; // stale response — keep placeholder, keep polling
              }
              this.docAnalysisPending.set(false);
            }

            this.analysisJobs.set(updated);
            const stillRunning = updated.some(
              j => j.status === 'PENDING' || j.status === 'PROCESSING'
            );
            const caseAnalysisDone = updated.some(j => j.jobType === 'CASE_ANALYSIS' && j.status === 'DONE');
            const questionsDone = updated.some(j => j.jobType === 'QUESTION_GENERATION' && (j.status === 'DONE' || j.status === 'FAILED'));
            const waitingForQuestions = caseAnalysisDone && !questionsDone;
            if (!stillRunning && !waitingForQuestions) {
              this.stopPolling();
            }
          }
        });
      }, 3000);
    } else if (!hasPendingOrProcessing && !forceStart && !this.docAnalysisPending()) {
      this.stopPolling();
    }
  }

  canAnalyze(): boolean {
    if (this.uploading()) return false;
    if (this.docAnalysisPending()) return false;
    if (this.fullAnalysisRunning()) return false;
    const jobs = this.analysisJobs();
    return jobs.some(j => j.jobType === 'DOCUMENT_ANALYSIS' && j.status === 'DONE');
  }

  caseAnalysisRunning(): boolean {
    return this.analysisJobs().some(
      j => j.jobType === 'CASE_ANALYSIS' && (j.status === 'PENDING' || j.status === 'PROCESSING')
    );
  }

  questionGenerationRunning(): boolean {
    return this.analysisJobs().some(
      j => j.jobType === 'QUESTION_GENERATION' && (j.status === 'PENDING' || j.status === 'PROCESSING')
    );
  }

  docAnalysisRunning(): boolean {
    return this.analysisJobs().some(
      j => j.jobType === 'DOCUMENT_ANALYSIS' && (j.status === 'PENDING' || j.status === 'PROCESSING')
    );
  }

  isJobActive(job: AnalysisJob): boolean {
    return job.status === 'PENDING' || job.status === 'PROCESSING';
  }

  triggerAnalysis(): void {
    const id = this.caseFile()?.id;
    if (!id) return;
    this.analyzing.set(true);

    // Inject placeholders immediately on click — both bars appear before server responds
    const pending = (type: AnalysisJob['jobType']): AnalysisJob =>
      ({ jobType: type, status: 'PENDING', totalItems: 0, processedItems: 0, progressPercentage: 0 });
    this.synthesis.set(null);
    this.questions.set([]);
    this.analysisJobs.update(jobs => [
      ...jobs.filter(j => j.jobType !== 'CASE_ANALYSIS' && j.jobType !== 'QUESTION_GENERATION'),
      pending('CASE_ANALYSIS'),
      pending('QUESTION_GENERATION')
    ]);

    this.caseAnalysisCommandService.triggerAnalysis(id).subscribe({
      next: () => {
        this.analyzing.set(false);
        this.loadAnalysisJobs(id);
      },
      error: (err: any) => {
        this.analyzing.set(false);
        // Revert placeholders — reload actual state from server
        this.loadAnalysisJobs(id);
        this.loadSynthesis(id);
        this.loadQuestions(id);
        if (err.status === 402) {
          this.snackBar.open("Limite d'analyses atteinte pour ce dossier. Passez au plan supérieur.", 'Fermer', {
            duration: 5000, panelClass: ['snack-error']
          });
        } else if (err.status === 409) {
          this.snackBar.open('Une analyse est déjà en cours.', 'Fermer', { duration: 4000 });
        } else if (err.status === 422) {
          this.snackBar.open('Aucun document analysé disponible.', 'Fermer', { duration: 4000 });
        } else {
          this.snackBar.open("Erreur lors du déclenchement de l'analyse.", 'Fermer', {
            duration: 4000, panelClass: ['snack-error']
          });
        }
      }
    });
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
    this.questionsLoading.set(true);
    this.aiQuestionService.getQuestions(caseFileId).subscribe({
      next: qs => { this.questions.set(qs); this.questionsLoading.set(false); },
      error: () => { this.questionsLoading.set(false); }
    });
  }

  pendingQuestionsCount(): number {
    return this.questions().filter(q => q.answerText === null).length;
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
        this.docAnalysisPending.set(true);

        const pending = (type: AnalysisJob['jobType']): AnalysisJob =>
          ({ jobType: type, status: 'PENDING', totalItems: 0, processedItems: 0, progressPercentage: 0 });
        this.analysisJobs.update(jobs => [
          ...jobs.filter(j => j.jobType !== 'CHUNK_ANALYSIS' && j.jobType !== 'DOCUMENT_ANALYSIS'),
          pending('DOCUMENT_ANALYSIS')
        ]);
        this.loadAnalysisJobs(caseFileId, true);
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

  clampProgress(p: number): number {
    return Math.min(100, Math.max(0, p));
  }

  statusLabel(status: string): string {
    return status === 'OPEN' ? 'Ouvert' : status;
  }
}
