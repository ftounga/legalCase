import { Component, OnInit, OnDestroy, signal, computed, ViewChild, ElementRef } from '@angular/core';
import { Subscription, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe, DecimalPipe, UpperCasePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { DocumentDeleteDialogComponent } from './document-delete-dialog.component';
import { CaseFileDeleteDialogComponent } from './case-file-delete-dialog.component';
import { ShareDialogComponent, ShareDialogData } from '../share-dialog/share-dialog.component';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseFileStatusService } from '../../core/services/case-file-status.service';
import { DocumentService } from '../../core/services/document.service';
import { AnalysisJobService } from '../../core/services/analysis-job.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { CaseAnalysisCommandService } from '../../core/services/case-analysis-command.service';
import { GlobalAnalysisNotificationService } from '../../core/services/global-analysis-notification.service';
import { AiQuestionService } from '../../core/services/ai-question.service';
import { AuthService } from '../../core/services/auth.service';
import { WorkspaceMemberService } from '../../core/services/workspace-member.service';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CaseFile } from '../../core/models/case-file.model';
import { Document } from '../../core/models/document.model';
import { AnalysisJob } from '../../core/models/analysis-job.model';
import { CaseAnalysisResult } from '../../core/models/case-analysis.model';
import { CaseFileStats } from '../../core/models/case-file-stats.model';
import { CaseFileStatsService } from '../../core/services/case-file-stats.service';

@Component({
  selector: 'app-case-file-detail',
  standalone: true,
  imports: [
    RouterLink, DatePipe, DecimalPipe, UpperCasePipe,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatProgressSpinnerModule, MatProgressBarModule,
    MatDialogModule, ShareDialogComponent
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
  stats = signal<CaseFileStats | null>(null);
  questions = signal<AiQuestion[]>([]);
  loading = signal(true);
  uploading = signal(false);
  analyzing = signal(false);
  synthesisLoading = signal(false);
  questionsLoading = signal(false);
  questionsLoaded = signal(false);
  currentMemberRole = signal<string | null>(null);

  // true between upload success and first backend confirmation that new doc analysis started
  private docAnalysisPending = signal(false);

  // true between triggerAnalysis() success and first backend confirmation that CASE_ANALYSIS job exists
  private caseAnalysisPending = signal(false);

  readonly canReopen = computed(() => {
    const role = this.currentMemberRole();
    return role === 'OWNER' || role === 'ADMIN';
  });

  readonly canDelete = computed(() => this.currentMemberRole() === 'OWNER');

  readonly docColumns = ['name', 'type', 'size', 'date', 'actions'];
  readonly visibleJobs = computed(() => this.analysisJobs().filter(j => j.jobType !== 'CHUNK_ANALYSIS'));

  // documents uploaded after the last synthesis — not covered by the current synthesis
  readonly outdatedDocuments = computed(() => {
    const syn = this.synthesis();
    if (!syn?.updatedAt) return [];
    const synDate = new Date(syn.updatedAt);
    return this.documents().filter(d => new Date(d.createdAt) > synDate);
  });

  // true if a document was deleted after the last synthesis
  readonly deletedSinceLastAnalysis = computed(() => {
    const deletedAt = this.caseFile()?.lastDocumentDeletedAt;
    const synUpdatedAt = this.synthesis()?.updatedAt;
    if (!deletedAt || !synUpdatedAt) return false;
    return new Date(deletedAt) > new Date(synUpdatedAt);
  });

  canCompare = signal(false);
  deletingDocId = signal<string | null>(null);
  pendingFiles = signal<File[]>([]);

  // true from "Analyser" click until both synthesis and questions are loaded
  readonly fullAnalysisRunning = computed(() => {
    if (this.analyzing()) return true;
    if (this.caseAnalysisPending()) return true;
    const jobs = this.analysisJobs();
    const caseJob = jobs.find(j => j.jobType === 'CASE_ANALYSIS');
    if (!caseJob) return false;
    if (caseJob.status === 'PENDING' || caseJob.status === 'PROCESSING') return true;
    if (caseJob.status === 'DONE' && !this.synthesis()) return true;
    const questionJob = jobs.find(j => j.jobType === 'QUESTION_GENERATION');
    if (!questionJob) return false;
    if (questionJob.status === 'PENDING' || questionJob.status === 'PROCESSING') return true;
    if (questionJob.status === 'DONE' && !this.questionsLoaded()) return true;
    return false;
  });

  private pollingInterval: ReturnType<typeof setInterval> | null = null;
  private eventsSub: Subscription | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private caseFileService: CaseFileService,
    private caseFileStatusService: CaseFileStatusService,
    private documentService: DocumentService,
    private analysisJobService: AnalysisJobService,
    private caseAnalysisService: CaseAnalysisService,
    private caseAnalysisCommandService: CaseAnalysisCommandService,
    private globalNotificationService: GlobalAnalysisNotificationService,
    private caseFileStatsService: CaseFileStatsService,
    private aiQuestionService: AiQuestionService,
    private authService: AuthService,
    private workspaceMemberService: WorkspaceMemberService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.eventsSub = this.globalNotificationService.events$
      .subscribe(event => {
        if (event.caseFileId !== id) return;
        if (event.status === 'DONE') {
          this.stopPolling();
          this.loadAnalysisJobs(id);
          this.loadStats(id);
          if (event.jobType === 'CASE_ANALYSIS' || event.jobType === 'ENRICHED_ANALYSIS') {
            this.loadSynthesis(id);
          }
        } else {
          this.loadAnalysisJobs(id);
        }
      });
    this.caseFileService.getById(id).subscribe({
      next: cf => {
        this.caseFile.set(cf);
        this.loading.set(false);
        this.loadDocuments(id);
        this.loadAnalysisJobs(id);
        this.loadStats(id);
      },
      error: () => {
        this.loading.set(false);
        this.snackBar.open('Dossier introuvable', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
    this.loadCurrentMemberRole();
  }

  ngOnDestroy(): void {
    this.stopPolling();
    this.eventsSub?.unsubscribe();
  }

  loadStats(caseFileId: string): void {
    this.caseFileStatsService.getStats(caseFileId).subscribe({
      next: s => this.stats.set(s),
      error: () => { /* silencieux */ }
    });
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
        // Don't overwrite placeholders while waiting for backend to pick up upload or analysis trigger
        if (jobs.length > 0 && !this.docAnalysisPending() && !this.caseAnalysisPending()) {
          this.analysisJobs.set(jobs);
        }
        this.managePolling(caseFileId, jobs, forceStart);
        if (jobs.some(j => j.jobType === 'CASE_ANALYSIS' && j.status === 'DONE')) {
          this.loadSynthesis(caseFileId);
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

    if ((hasPendingOrProcessing || forceStart || this.docAnalysisPending() || this.caseAnalysisPending()) && !this.pollingInterval) {
      this.pollingInterval = setInterval(() => {
        this.analysisJobService.getJobs(caseFileId).subscribe({
          next: updated => {
            if (updated.length === 0) return; // pipeline not started yet — keep placeholders

            // Wait for backend to confirm DOCUMENT_ANALYSIS picked up the new upload
            if (this.docAnalysisPending()) {
              const docJob = updated.find(j => j.jobType === 'DOCUMENT_ANALYSIS');
              const backendPickedUp = docJob && docJob.totalItems >= this.documents().length;
              if (!backendPickedUp) {
                return; // stale response — keep placeholder, keep polling
              }
              this.docAnalysisPending.set(false);
            }

            // Wait for backend to confirm CASE_ANALYSIS job was created
            if (this.caseAnalysisPending()) {
              const caseJob = updated.find(j => j.jobType === 'CASE_ANALYSIS');
              if (!caseJob) {
                return; // job not yet created — keep placeholder, keep polling
              }
              this.caseAnalysisPending.set(false);
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
    } else if (!hasPendingOrProcessing && !forceStart && !this.docAnalysisPending() && !this.caseAnalysisPending()) {
      this.stopPolling();
    }
  }

  canAnalyze(): boolean {
    if (this.uploading()) return false;
    if (this.docAnalysisPending()) return false;
    if (this.caseAnalysisPending()) return false;
    if (this.fullAnalysisRunning()) return false;
    if (this.enrichedAnalysisRunning()) return false;
    const jobs = this.analysisJobs();
    return jobs.some(j => j.jobType === 'DOCUMENT_ANALYSIS' && j.status === 'DONE');
  }

  enrichedAnalysisRunning(): boolean {
    return this.analysisJobs().some(
      j => j.jobType === 'ENRICHED_ANALYSIS' && (j.status === 'PENDING' || j.status === 'PROCESSING')
    );
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
    this.questionsLoaded.set(false);
    this.analysisJobs.update(jobs => [
      ...jobs.filter(j => j.jobType !== 'CASE_ANALYSIS' && j.jobType !== 'QUESTION_GENERATION'),
      pending('CASE_ANALYSIS'),
      pending('QUESTION_GENERATION')
    ]);

    this.caseAnalysisPending.set(true);

    this.caseAnalysisCommandService.triggerAnalysis(id).subscribe({
      next: () => {
        this.analyzing.set(false);
        this.loadAnalysisJobs(id, true);
        this.globalNotificationService.track(id);
      },
      error: (err: any) => {
        this.analyzing.set(false);
        this.caseAnalysisPending.set(false);
        // Revert placeholders — reload actual state from server
        this.loadAnalysisJobs(id);
        this.loadSynthesis(id);
        // Questions will be reloaded inside loadSynthesis once synthesis id is known
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
        const enrichedAnalysisDone = this.analysisJobs().some(
          j => j.jobType === 'ENRICHED_ANALYSIS' && j.status === 'DONE'
        );
        if (enrichedAnalysisDone) {
          this.loadSynthesis(caseFileId);
          // Questions will be reloaded inside loadSynthesis once synthesis id is known
        }
      }
    }
  }

  loadQuestions(caseFileId: string, analysisId?: string): void {
    this.questionsLoading.set(true);
    const obs = analysisId
      ? this.aiQuestionService.getQuestionsByAnalysisId(caseFileId, analysisId)
      : this.aiQuestionService.getQuestions(caseFileId);
    obs.subscribe({
      next: qs => { this.questions.set(qs); this.questionsLoading.set(false); this.questionsLoaded.set(true); },
      error: () => { this.questionsLoading.set(false); }
    });
  }

  pendingQuestionsCount(): number {
    return this.questions().filter(q => q.answerText === null).length;
  }

  loadSynthesis(caseFileId: string): void {
    this.caseAnalysisService.getAnalysis(caseFileId).subscribe({
      next: result => {
        this.synthesis.set(result);
        // Reload questions for this specific version once synthesis id is known
        const questionsDone = this.analysisJobs().some(
          j => j.jobType === 'QUESTION_GENERATION' && j.status === 'DONE'
        );
        if (questionsDone && result?.id) {
          this.loadQuestions(caseFileId, result.id);
        }
        this.loadVersionsCount(caseFileId);
      },
      error: () => { /* silencieux */ }
    });
  }

  private loadVersionsCount(caseFileId: string): void {
    this.caseAnalysisService.getVersions(caseFileId).subscribe({
      next: versions => this.canCompare.set(versions.length >= 2),
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
    const files = Array.from(input.files ?? []);
    input.value = '';
    if (!files.length) return;

    const MAX_SIZE = 50 * 1024 * 1024;
    const oversized = files.filter(f => f.size > MAX_SIZE);
    const valid = files.filter(f => f.size <= MAX_SIZE);

    if (oversized.length > 0) {
      this.snackBar.open(
        `${oversized.length} fichier(s) rejeté(s) : taille max 50 Mo.`,
        'Fermer', { duration: 5000, panelClass: ['snack-error'] }
      );
    }

    if (valid.length > 0) {
      this.pendingFiles.update(current => [...current, ...valid]);
    }
  }

  removePendingFile(file: File): void {
    this.pendingFiles.update(files => files.filter(f => f !== file));
  }

  uploadPendingFiles(): void {
    const files = this.pendingFiles();
    if (!files.length) return;
    const caseFileId = this.caseFile()!.id;
    this.uploading.set(true);

    const uploads = files.map(f =>
      this.documentService.upload(caseFileId, f).pipe(
        catchError(err => of({ error: err }))
      )
    );

    forkJoin(uploads).subscribe(results => {
      const succeeded = results.filter(r => !('error' in r)) as Document[];
      const failed = results.filter(r => 'error' in r) as { error: any }[];

      this.uploading.set(false);
      this.pendingFiles.set([]);

      if (succeeded.length > 0) {
        this.documents.update(docs => [...succeeded, ...docs]);
        this.docAnalysisPending.set(true);
        const pending = (type: AnalysisJob['jobType']): AnalysisJob =>
          ({ jobType: type, status: 'PENDING', totalItems: 0, processedItems: 0, progressPercentage: 0 });
        this.analysisJobs.update(jobs => [
          ...jobs.filter(j => j.jobType !== 'CHUNK_ANALYSIS' && j.jobType !== 'DOCUMENT_ANALYSIS'),
          pending('DOCUMENT_ANALYSIS')
        ]);
        this.loadAnalysisJobs(caseFileId, true);
        this.globalNotificationService.track(caseFileId);
      }

      if (failed.length === 0) {
        this.snackBar.open(`${succeeded.length} document(s) ajouté(s)`, 'Fermer', {
          duration: 3000, panelClass: ['snack-success']
        });
      } else if (succeeded.length > 0) {
        this.snackBar.open(
          `${succeeded.length} document(s) ajouté(s), ${failed.length} en erreur.`,
          'Fermer', { duration: 5000, panelClass: ['snack-error'] }
        );
      } else {
        const is402 = failed.some((f: any) => f.error?.status === 402);
        if (is402) {
          this.snackBar.open("Limite de documents atteinte. Passez au plan supérieur.", 'Fermer', {
            duration: 5000, panelClass: ['snack-error']
          });
        } else {
          this.snackBar.open("Erreur lors de l'upload. Vérifiez le type et la taille des fichiers (max 50 Mo).", 'Fermer', {
            duration: 5000, panelClass: ['snack-error']
          });
        }
      }
    });
  }

  canUpload(): boolean {
    return !this.uploading() && !this.fullAnalysisRunning() && !this.enrichedAnalysisRunning();
  }

  canSubmitUpload(): boolean {
    return this.pendingFiles().length > 0 && this.canUpload();
  }

  canDeleteDocument(): boolean {
    return !this.fullAnalysisRunning() && !this.enrichedAnalysisRunning() && !this.docAnalysisRunning();
  }

  deleteDocument(doc: Document): void {
    const ref = this.dialog.open(DocumentDeleteDialogComponent, {
      data: { documentName: doc.originalFilename },
      width: '400px'
    });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      const caseFileId = this.caseFile()!.id;
      this.deletingDocId.set(doc.id);
      this.documentService.delete(caseFileId, doc.id).subscribe({
        next: () => {
          this.documents.update(docs => docs.filter(d => d.id !== doc.id));
          this.caseFile.update(cf => cf ? { ...cf, lastDocumentDeletedAt: new Date().toISOString() } : cf);
          this.deletingDocId.set(null);
          this.snackBar.open('Document supprimé', 'Fermer', { duration: 3000, panelClass: ['snack-success'] });
        },
        error: (err: any) => {
          this.deletingDocId.set(null);
          if (err.status === 409) {
            this.snackBar.open('Suppression impossible : une analyse est en cours.', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
          } else if (err.status === 404) {
            this.snackBar.open('Document introuvable.', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
          } else {
            this.snackBar.open('Erreur lors de la suppression.', 'Fermer', { duration: 4000, panelClass: ['snack-error'] });
          }
        }
      });
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
    if (status === 'OPEN') return 'Ouvert';
    if (status === 'CLOSED') return 'Clôturé';
    return status;
  }

  statusClass(status: string): string {
    return status === 'OPEN' ? 'badge--success' : 'badge--neutral';
  }

  private loadCurrentMemberRole(): void {
    this.workspaceMemberService.getMembers().subscribe({
      next: members => {
        const currentUserId = this.authService.currentUser()?.id;
        const member = members.find(m => m.userId === currentUserId);
        this.currentMemberRole.set(member?.memberRole ?? null);
      },
      error: () => { /* silencieux */ }
    });
  }

  closeCaseFile(): void {
    const id = this.caseFile()?.id;
    if (!id) return;
    this.caseFileStatusService.close(id).subscribe({
      next: updated => {
        this.caseFile.set(updated);
        this.snackBar.open('Dossier clôturé', 'Fermer', { duration: 3000, panelClass: ['snack-success'] });
      },
      error: (err: any) => {
        const msg = err?.status === 409
          ? 'Une analyse est en cours. Attendez la fin avant de clôturer le dossier.'
          : 'Une erreur est survenue. Veuillez réessayer.';
        this.snackBar.open(msg, 'Fermer', { duration: 5000, panelClass: ['snack-error'] });
      }
    });
  }

  reopenCaseFile(): void {
    const id = this.caseFile()?.id;
    if (!id) return;
    this.caseFileStatusService.reopen(id).subscribe({
      next: updated => {
        this.caseFile.set(updated);
        this.snackBar.open('Dossier réouvert', 'Fermer', { duration: 3000, panelClass: ['snack-success'] });
      },
      error: (err: any) => {
        if (err.status === 402) {
          this.snackBar.open('Limite de dossiers actifs atteinte. Passez à un plan supérieur.', 'Fermer', {
            duration: 5000, panelClass: ['snack-error']
          });
        } else {
          this.snackBar.open('Une erreur est survenue. Veuillez réessayer.', 'Fermer', {
            duration: 4000, panelClass: ['snack-error']
          });
        }
      }
    });
  }

  openShareDialog(): void {
    const cf = this.caseFile();
    if (!cf) return;
    this.dialog.open(ShareDialogComponent, {
      data: { caseFileId: cf.id, caseFileTitle: cf.title } satisfies ShareDialogData,
      width: '500px',
      maxWidth: '95vw'
    });
  }

  deleteCaseFile(): void {
    const cf = this.caseFile();
    if (!cf) return;
    const ref = this.dialog.open(CaseFileDeleteDialogComponent, { width: '400px' });
    ref.afterClosed().subscribe(confirmed => {
      if (!confirmed) return;
      this.caseFileStatusService.delete(cf.id).subscribe({
        next: () => {
          this.router.navigate(['/case-files']);
        },
        error: (err: any) => {
          if (err.status === 409) {
            this.snackBar.open('Impossible de supprimer un dossier avec une analyse en cours.', 'Fermer', {
              duration: 5000, panelClass: ['snack-error']
            });
          } else {
            this.snackBar.open('Une erreur est survenue. Veuillez réessayer.', 'Fermer', {
              duration: 4000, panelClass: ['snack-error']
            });
          }
        }
      });
    });
  }
}
