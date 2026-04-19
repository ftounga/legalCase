import { Component, OnInit, OnDestroy, signal, computed, ViewChild, ElementRef, inject } from '@angular/core';
import { Subscription, forkJoin, of } from 'rxjs';
import { catchError, filter, map, tap } from 'rxjs/operators';
import { HttpEventType, HttpResponse } from '@angular/common/http';
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
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DocumentDeleteDialogComponent } from './document-delete-dialog.component';
import { CaseFileDeleteDialogComponent } from './case-file-delete-dialog.component';
import { FullReanalysisConfirmDialogComponent, FullReanalysisConfirmResult } from './full-reanalysis-confirm-dialog.component';
import { CaseFileEditDialogComponent, CaseFileEditDialogData } from '../case-file-edit-dialog/case-file-edit-dialog.component';
import { ShareDialogComponent, ShareDialogData } from '../share-dialog/share-dialog.component';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseFileStatusService } from '../../core/services/case-file-status.service';
import { DocumentService } from '../../core/services/document.service';
import { AnalysisJobService } from '../../core/services/analysis-job.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { CaseAnalysisCommandService } from '../../core/services/case-analysis-command.service';
import { ReAnalysisService } from '../../core/services/re-analysis.service';
import { GlobalAnalysisNotificationService } from '../../core/services/global-analysis-notification.service';
import { AiQuestionService } from '../../core/services/ai-question.service';
import { ProcedureCheckService } from '../../core/services/procedure-check.service';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AuthService } from '../../core/services/auth.service';
import { WorkspaceMemberService } from '../../core/services/workspace-member.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { AnalyticsService } from '../../core/services/analytics.service';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CaseFile } from '../../core/models/case-file.model';
import { Document, extractionFailureLabel } from '../../core/models/document.model';
import { AnalysisJob } from '../../core/models/analysis-job.model';
import { CaseAnalysisResult } from '../../core/models/case-analysis.model';
import { CaseFileStats } from '../../core/models/case-file-stats.model';
import { CaseFileStatsService } from '../../core/services/case-file-stats.service';
import { CaseNotesSectionComponent } from '../case-notes-section/case-notes-section.component';
import { CaseDeadlinesSectionComponent } from '../case-deadlines-section/case-deadlines-section.component';
import { PrudhomeFicheSectionComponent } from '../prudhome-fiche-section/prudhome-fiche-section.component';
import { TribunalTravailFicheSectionComponent } from '../tribunal-travail-fiche-section/tribunal-travail-fiche-section.component';
import { ImmigrationChecklistSectionComponent } from '../immigration-checklist-section/immigration-checklist-section.component';
import { ImmigrationTitleDecisionSectionComponent } from '../immigration-title-decision-section/immigration-title-decision-section.component';
import { ImmigrationRecoursSectionComponent } from '../immigration-recours-section/immigration-recours-section.component';
import { ImmigrationWorkRightSectionComponent } from '../immigration-work-right-section/immigration-work-right-section.component';
import { AncienneteSectionComponent } from '../anciennete-section/anciennete-section.component';
import { LicenciementSectionComponent } from '../licenciement-section/licenciement-section.component';
import { IndemniteComparatifSectionComponent } from '../indemnite-comparatif-section/indemnite-comparatif-section.component';
import { PartageImmobilierSectionComponent } from '../partage-immobilier-section/partage-immobilier-section.component';
import { CalendrierGardeSectionComponent } from '../calendrier-garde-section/calendrier-garde-section.component';
import { DivorceChecklistSectionComponent } from '../divorce-checklist-section/divorce-checklist-section.component';
import { CaseDashboardComponent } from '../case-dashboard/case-dashboard.component';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { RuptureConvSectionComponent } from '../rupture-conv-section/rupture-conv-section.component';
import { CaseDashboardStepperComponent, DashboardStep } from '../case-dashboard-stepper/case-dashboard-stepper.component';
import { AnalysisPipelineComponent } from '../analysis-pipeline/analysis-pipeline.component';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { CaseDeadline } from '../../core/models/case-deadline.model';
import { OcrRetryService, OcrRetryPreview } from '../../core/services/ocr-retry.service';
import { fadeInUp, listStagger } from '../../shared/animations';
import { TimerWidgetComponent } from '../../shared/timer-widget/timer-widget.component';

@Component({
  selector: 'app-case-file-detail',
  standalone: true,
  imports: [
    RouterLink, DatePipe, DecimalPipe, UpperCasePipe,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatProgressSpinnerModule, MatProgressBarModule,
    MatDialogModule, MatMenuModule, MatTooltipModule, ShareDialogComponent, CaseNotesSectionComponent,
    CaseDeadlinesSectionComponent, CaseDashboardStepperComponent,
    TimerWidgetComponent, PrudhomeFicheSectionComponent, TribunalTravailFicheSectionComponent,
    ImmigrationChecklistSectionComponent, ImmigrationTitleDecisionSectionComponent,
    ImmigrationRecoursSectionComponent, ImmigrationWorkRightSectionComponent,
    AncienneteSectionComponent, LicenciementSectionComponent,
    IndemniteComparatifSectionComponent, PartageImmobilierSectionComponent, CalendrierGardeSectionComponent, DivorceChecklistSectionComponent,
    RuptureConvSectionComponent,
    CaseDashboardComponent, AnalysisPipelineComponent
  ],
  templateUrl: './case-file-detail.component.html',
  styleUrl: './case-file-detail.component.scss',
  animations: [fadeInUp, listStagger],
  host: { '[@fadeInUp]': '' },
  providers: [CaseDashboardRefreshService],
})
export class CaseFileDetailComponent implements OnInit, OnDestroy {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  caseFile = signal<CaseFile | null>(null);
  documents = signal<Document[]>([]);
  analysisJobs = signal<AnalysisJob[]>([]);
  synthesis = signal<CaseAnalysisResult | null>(null);
  procedureChecks = signal<ProcedureCheck[]>([]);
  stats = signal<CaseFileStats | null>(null);
  questions = signal<AiQuestion[]>([]);
  loading = signal(true);
  uploading = signal(false);
  analyzing = signal(false);
  synthesisLoading = signal(false);
  questionsLoading = signal(false);
  questionsLoaded = signal(false);
  currentMemberRole = signal<string | null>(null);
  workspaceCountry = signal<string>('FRANCE');

  // true between upload success and first backend confirmation that new doc analysis started
  private docAnalysisPending = signal(false);

  // true between triggerAnalysis() success and first backend confirmation that CASE_ANALYSIS job exists
  private caseAnalysisPending = signal(false);

  // F-124 : dernière version d'analyse chargée, utilisée pour détecter une nouvelle version
  // (différente de this.synthesis().version qui est reset à null pendant les re-analyses).
  // Permet de ne déclencher refreshService.triggerRefresh() qu'une seule fois par ré-analyse,
  // pas à chaque tick de polling qui re-fetche la même synthèse.
  private lastCompletedSynthesisVersion = signal<number | null>(null);

  // SF-125-01 : transition between enrich/full click and backend confirmation
  reAnalyzing = signal(false);

  // SF-121-02 : exposé au template pour le tooltip du badge "Non analysable"
  readonly extractionFailureLabel = extractionFailureLabel;

  // SF-125-01 : bouton contextuel — ENRICHED si analyse DONE + au moins 1 input avocat
  // (réponse Q&A OU check procédural validé). Le chat n'est pas chargé côté case-file-detail,
  // le backend validera via la condition complète (Q&A + chat + checks).
  readonly hasAnyAnalysis = computed(() => this.synthesis() !== null);
  readonly canEnrichSynthesis = computed(() => {
    if (this.synthesis() === null) return false;
    const hasAnyAnswer = this.questions().some(q => q.answerText !== null);
    const hasAnyValidatedCheck = this.procedureChecks().some(c => c.statut === 'VERIFIED' || c.statut === 'NON_COMPLIANT');
    return hasAnyAnswer || hasAnyValidatedCheck;
  });
  readonly analysisButtonLabel = computed(() => {
    if (!this.hasAnyAnalysis()) return 'Analyser le dossier';
    return this.canEnrichSynthesis() ? 'Enrichir la synthèse' : 'Analyser le dossier';
  });

  readonly canReopen = computed(() => {
    const role = this.currentMemberRole();
    return role === 'OWNER' || role === 'ADMIN';
  });

  readonly canDelete = computed(() => this.currentMemberRole() === 'OWNER');

  // F-DT-10 / SF-DT-10-04 — orchestration UX bloc validité selon type_rupture IA
  private static readonly LICENCIEMENT_TYPES = new Set([
    'LICENCIEMENT', 'LICENCIEMENT_ECONOMIQUE',
    'LICENCIEMENT_ORDINAIRE', 'LICENCIEMENT_MANIFESTEMENT_DERAISONNABLE',
  ]);

  readonly showValiditeLicenciement = computed(() => {
    const type = this.synthesis()?.compensationEstimate?.typeRupture;
    // Defaut permissif : si l'IA n'a pas identifié le type (dossier legacy
    // ou analyse non encore lancee), on garde F-DT-08 visible par retrocompat.
    if (!type) return true;
    return CaseFileDetailComponent.LICENCIEMENT_TYPES.has(type);
  });

  readonly showValiditeRuptureConv = computed(() => {
    const type = this.synthesis()?.compensationEstimate?.typeRupture;
    return type === 'RUPTURE_CONVENTIONNELLE' && this.workspaceCountry() === 'FRANCE';
  });

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

  deadlines = signal<CaseDeadline[]>([]);

  readonly dashboardSteps = computed((): DashboardStep[] => {
    const docs = this.documents();
    const syn = this.synthesis();
    const qs = this.questions();
    const dl = this.deadlines();

    const pendingQuestions = qs.filter(q => q.answerText === null).length;
    const pendingAiDeadlines = dl.filter(d => d.source === 'AI' && d.aiStatus === 'PENDING').length;
    const piecesCount = syn?.piecesManquantes?.length ?? 0;

    return [
      {
        id: 'documents',
        label: 'Documents',
        status: docs.length > 0 ? 'done' : 'pending',
        detail: docs.length > 0 ? `${docs.length} document${docs.length > 1 ? 's' : ''}` : null,
        anchorId: 'section-documents',
      },
      {
        id: 'analyse',
        label: 'Analyse du dossier',
        status: this.fullAnalysisRunning() ? 'in_progress' : syn !== null ? 'done' : 'pending',
        detail: this.fullAnalysisRunning() ? 'En cours…' : syn !== null ? 'Terminée' : null,
        anchorId: 'section-analyse',
      },
      {
        id: 'questions',
        label: 'Questions complémentaires',
        status: syn === null ? 'pending' : (qs.length > 0 && pendingQuestions === 0) ? 'done' : 'pending',
        detail: syn !== null && pendingQuestions > 0 ? `${pendingQuestions} en attente` : null,
        anchorId: null,
      },
      {
        id: 'delais',
        label: 'Délais légaux',
        status: syn === null ? 'pending' : pendingAiDeadlines > 0 ? 'in_progress' : 'done',
        detail: pendingAiDeadlines > 0 ? `${pendingAiDeadlines} proposition${pendingAiDeadlines > 1 ? 's' : ''} IA en attente` : null,
        anchorId: 'section-deadlines',
      },
      {
        id: 'pieces',
        label: 'Pièces manquantes',
        status: syn === null ? 'pending' : piecesCount === 0 ? 'done' : 'pending',
        detail: piecesCount > 0 ? `${piecesCount} identifiée${piecesCount > 1 ? 's' : ''}` : null,
        anchorId: null,
      },
    ];
  });

  canCompare = signal(false);
  deletingDocId = signal<string | null>(null);
  pendingFiles = signal<File[]>([]);
  /** SF-122-07 : si coché (défaut), un PDF scanné déclenche l'OCR Textract en
   * fallback. Si décoché, le scan reste non analysable (aucun appel AWS, quota
   * préservé). L'avocat pourra toujours réactiver l'OCR a posteriori via le
   * bouton "Relancer avec OCR" (bandeau F-122-05). Batch-level, reset après upload. */
  ocrEnabled = signal(true);

  /** SF-122-03 : si coché, les PDF uploadés bénéficient de l'OCR Textract approfondi
   * (FORMS + TABLES). Compte ×3 dans le quota OCR. Grisé si ocrEnabled=false. */
  ocrFormsMode = signal(false);

  /** SF-122-07 : tooltip de la 2ème checkbox, contextuel selon l'état de ocrEnabled. */
  readonly ocrFormsTooltip = computed(() => {
    if (!this.ocrEnabled()) {
      return "Activez d'abord l'OCR pour choisir le mode approfondi";
    }
    return "Active l'OCR approfondi (détection des champs de formulaire) pour CERFA, déclarations préfecture, URSSAF. Compte ×3 dans le quota.";
  });

  /** SF-122-05 : preview du retry OCR — chargée à l'ouverture du dossier si ≥ 1 doc FAILED éligible. */
  ocrRetryPreview = signal<OcrRetryPreview | null>(null);
  ocrRetryInProgress = signal(false);

  /** true si ≥ 1 doc FAILED avec motif EMPTY_TEXT ou OCR_FAILED. Déclenche le bandeau. */
  readonly hasRetryableFailedDocs = computed(() =>
    this.documents().some(d => d.extractionStatus === 'FAILED'
        && (d.failureReason === 'EMPTY_TEXT' || d.failureReason === 'OCR_FAILED')));
  fileUploadProgresses = signal<Map<string, number>>(new Map());

  readonly overallUploadProgress = computed(() => {
    const m = this.fileUploadProgresses();
    if (m.size === 0) return 0;
    const values = Array.from(m.values());
    return Math.round(values.reduce((a, b) => a + b, 0) / values.length);
  });

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
    private reAnalysisService: ReAnalysisService,
    private globalNotificationService: GlobalAnalysisNotificationService,
    private caseFileStatsService: CaseFileStatsService,
    private aiQuestionService: AiQuestionService,
    protected authService: AuthService,
    private workspaceMemberService: WorkspaceMemberService,
    private workspaceService: WorkspaceService,
    private snackBar: MatSnackBar,
    private dialog: MatDialog,
    private analyticsService: AnalyticsService,
    private caseDeadlineService: CaseDeadlineService,
    private procedureCheckService: ProcedureCheckService,
    private dashboardRefreshService: CaseDashboardRefreshService,
    private ocrRetryService: OcrRetryService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;

    // SF-IA-03-19 : scroll vers la section cible quand on arrive via un popover d'incohérence.
    this.route.queryParamMap?.subscribe(params => {
      const section = params.get('section');
      const doc = params.get('doc');
      // Priorité au document précis si présent (highlight ligne), sinon la section.
      if (doc) {
        this.scrollAndHighlight('doc-' + doc);
        return;
      }
      if (!section) return;
      const anchorId = section === 'documents' ? 'section-documents'
                     : section === 'analyse' ? 'section-analyse'
                     : section === 'deadlines' ? 'section-deadlines'
                     : null;
      if (anchorId) this.scrollAndHighlight(anchorId);
    });

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
    this.workspaceService.getCurrentWorkspace().subscribe({
      next: ws => this.workspaceCountry.set(ws.country ?? 'FRANCE'),
      error: () => {}
    });
    this.caseFileService.getById(id).subscribe({
      next: cf => {
        this.caseFile.set(cf);
        this.loading.set(false);
        this.loadDocuments(id);
        this.loadAnalysisJobs(id);
        this.loadStats(id);
        this.loadDeadlines(id);
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

  /** SF-IA-03-19 : scroll + highlight pulse 2s sur une section. Retry 3× car le rendu est async. */
  private scrollAndHighlight(anchorId: string, attempt = 0): void {
    const el = document.getElementById(anchorId);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' });
      el.classList.add('source-highlight');
      setTimeout(() => el.classList.remove('source-highlight'), 2100);
    } else if (attempt < 10) {
      setTimeout(() => this.scrollAndHighlight(anchorId, attempt + 1), 300);
    }
  }

  loadDeadlines(caseFileId: string): void {
    this.caseDeadlineService.list(caseFileId).subscribe({
      next: dl => this.deadlines.set(dl),
      error: () => { /* fail-open — stepper étape 4 reste pending */ }
    });
  }

  loadStats(caseFileId: string): void {
    this.caseFileStatsService.getStats(caseFileId).subscribe({
      next: s => this.stats.set(s),
      error: () => { /* silencieux */ }
    });
  }

  loadDocuments(caseFileId: string): void {
    this.documentService.list(caseFileId).subscribe({
      next: docs => {
        this.documents.set(docs);
        // SF-121-04 : après fetch docs, ré-applique l'override FAILED si pertinent.
        this.applyExtractionFailedOverride();
        // SF-122-05 : si au moins un doc FAILED éligible, charger le preview retry.
        if (this.hasRetryableFailedDocs()) {
          this.loadOcrRetryPreview(caseFileId);
        } else {
          this.ocrRetryPreview.set(null);
        }
      },
      error: () => this.snackBar.open('Erreur lors du chargement des documents', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      })
    });
  }

  /** SF-122-05 : charge le preview de retry OCR (nb docs éligibles, quota restant). */
  private loadOcrRetryPreview(caseFileId: string): void {
    this.ocrRetryService.preview(caseFileId).subscribe({
      next: p => this.ocrRetryPreview.set(p),
      error: () => this.ocrRetryPreview.set(null),
    });
  }

  /** SF-122-05 : handler bouton "Relancer avec OCR" du bandeau. */
  triggerOcrRetry(): void {
    const id = this.caseFile()?.id;
    const preview = this.ocrRetryPreview();
    if (!id || !preview || !preview.canRetry) return;

    const message = `Relancer l'OCR sur ${preview.failedDocsCount} document(s) ? ` +
        `Cela va consommer ~${preview.estimatedPages} pages de votre quota OCR ` +
        `(restantes : ${preview.monthlyRemaining + preview.packsRemaining}).`;
    if (!confirm(message)) return;

    this.ocrRetryInProgress.set(true);
    // SF-122-07 fix : remplace le virtuel FAILED par un virtuel PROCESSING le
    // temps du retry, pour que la step 2 "Analyse des documents" passe en bleu
    // progression (au lieu de rester en rouge figé). Le polling mettra à jour
    // quand les extractions aboutiront.
    this.analysisJobs.update(jobs => [
      ...jobs.filter(j => j.jobType !== 'DOCUMENT_ANALYSIS' && j.jobType !== 'CHUNK_ANALYSIS'),
      {
        jobType: 'DOCUMENT_ANALYSIS',
        status: 'PROCESSING',
        totalItems: preview.failedDocsCount,
        processedItems: 0,
        progressPercentage: 0,
      },
    ]);
    this.docAnalysisPending.set(true);
    this.ocrRetryPreview.set(null); // masque le bandeau pendant le retry

    this.ocrRetryService.retry(id).subscribe({
      next: res => {
        this.ocrRetryInProgress.set(false);
        this.snackBar.open(
            `${res.retryedCount} document(s) en cours de re-extraction…`,
            'Fermer', { duration: 4000, panelClass: ['snack-success'] });
        // Relance polling en forçant le démarrage, et recharge docs après 3s
        this.loadAnalysisJobs(id, true);
        setTimeout(() => this.loadDocuments(id), 3000);
      },
      error: err => {
        this.ocrRetryInProgress.set(false);
        // Restaure l'affichage original en cas d'erreur (le polling re-sync sur état réel)
        this.docAnalysisPending.set(false);
        this.applyExtractionFailedOverride();
        this.loadOcrRetryPreview(id);
        const msg = err?.status === 429
            ? 'Un retry OCR a déjà été lancé récemment. Réessayez dans quelques minutes.'
            : 'Erreur lors du lancement du retry OCR.';
        this.snackBar.open(msg, 'Fermer', { duration: 4500, panelClass: ['snack-error'] });
      }
    });
  }

  loadAnalysisJobs(caseFileId: string, forceStart = false): void {
    this.analysisJobService.getJobs(caseFileId).subscribe({
      next: jobs => {
        // Don't overwrite placeholders while waiting for backend to pick up upload or analysis trigger
        if (jobs.length > 0 && !this.docAnalysisPending() && !this.caseAnalysisPending()) {
          this.analysisJobs.set(jobs);
        }
        // SF-121-04 : après application des jobs backend, ré-applique l'override FAILED.
        this.applyExtractionFailedOverride();
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
        // SF-121-04 : re-fetch docs pour détecter l'état d'extraction courant,
        // puis re-applique l'override FAILED si ≥ 1 doc FAILED.
        this.refreshDocumentsAndApplyOverride(caseFileId);
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
            // SF-121-04 : après avoir écrasé avec les jobs backend, ré-applique l'override FAILED.
            this.applyExtractionFailedOverride();
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

  /**
   * SF-121-04 : règle "any FAILED". Si ≥ 1 document du dossier a
   * `extractionStatus === 'FAILED'` et que toutes les extractions sont dans
   * un état stable (FAILED ou DONE), remplace/pose un job virtuel
   * DOCUMENT_ANALYSIS en FAILED avec compteur "N non analysables / M".
   *
   * Why : SF-121-01 empêche le pipeline IA de tourner sur des extractions vides,
   * et même en cas d'échec partiel (3 FAILED + 6 DONE) la synthèse IA sur les
   * 6 docs lisibles peut être trompeuse si des pièces clés sont dans les 3 FAILED.
   * Faire remonter l'échec en step 2 alerte l'avocat avant qu'il ne consomme une
   * synthèse incomplète. Idempotent — ne repose le virtuel que si données diffèrent.
   */
  private applyExtractionFailedOverride(): void {
    const docs = this.documents();
    if (docs.length === 0) return;

    const failedCount = docs.filter(d => d.extractionStatus === 'FAILED').length;
    if (failedCount === 0) return;

    // Attendre que toutes les extractions se stabilisent (PENDING/PROCESSING → FAILED ou DONE)
    // avant de figer l'affichage en rouge. Les docs sans `extractionStatus` (legacy) sont
    // considérés stables (pas d'extraction en cours).
    const anyUnstable = docs.some(d =>
      d.extractionStatus === 'PENDING' || d.extractionStatus === 'PROCESSING'
    );
    if (anyUnstable) return;

    this.analysisJobs.update(jobs => {
      const existing = jobs.find(j => j.jobType === 'DOCUMENT_ANALYSIS');
      if (existing
          && existing.status === 'FAILED'
          && existing.processedItems === failedCount
          && existing.totalItems === docs.length) {
        return jobs; // idempotent — rien à changer
      }
      const virtualFailed: AnalysisJob = {
        jobType: 'DOCUMENT_ANALYSIS',
        status: 'FAILED',
        totalItems: docs.length,
        processedItems: failedCount,
        progressPercentage: 100,
      };
      return [
        ...jobs.filter(j => j.jobType !== 'DOCUMENT_ANALYSIS' && j.jobType !== 'CHUNK_ANALYSIS'),
        virtualFailed,
      ];
    });
    this.docAnalysisPending.set(false);
    // stopPolling géré par managePolling en fonction des autres jobs (ex. CASE_ANALYSIS
    // peut être en cours sur les docs lisibles — ne pas couper le polling prématurément).
  }

  private refreshDocumentsAndApplyOverride(caseFileId: string): void {
    this.documentService.list(caseFileId).subscribe({
      next: docs => {
        // SF-122-07 fix anti-flicker : n'update le signal que si le contenu a
        // vraiment changé (id + extractionStatus + failureReason). Sinon toutes
        // les 3s la liste clignotait à cause du re-diff Angular sur la nouvelle
        // référence tableau.
        if (!this.documentsContentEqual(this.documents(), docs)) {
          this.documents.set(docs);
        }
        this.applyExtractionFailedOverride();
        if (this.hasRetryableFailedDocs()) {
          this.loadOcrRetryPreview(caseFileId);
        } else {
          this.ocrRetryPreview.set(null);
        }
      },
    });
  }

  /** Compare 2 listes de Documents sur les champs qui comptent pour le rendu. */
  private documentsContentEqual(a: Document[], b: Document[]): boolean {
    if (a.length !== b.length) return false;
    for (let i = 0; i < a.length; i++) {
      const x = a[i], y = b[i];
      if (x.id !== y.id
          || x.extractionStatus !== y.extractionStatus
          || x.failureReason !== y.failureReason) {
        return false;
      }
    }
    return true;
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
        this.analyticsService.trackEvent('analysis_launched', { type: 'STANDARD' });
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

  /**
   * SF-125-01 — Bouton principal contextuel : dispatch STANDARD ou ENRICHED
   * selon l'état du dossier (présence d'une analyse DONE et de réponses Q&A).
   */
  onAnalysisButtonClick(): void {
    if (this.canEnrichSynthesis()) {
      this.triggerEnrichedAnalysis();
    } else {
      this.triggerAnalysis();
    }
  }

  /**
   * SF-125-01 — Option du menu secondaire ⋮ "Nouvelle analyse complète depuis zéro".
   * Ouvre une dialog de confirmation qui ré-oriente vers l'enrichissement par défaut.
   */
  onFullReanalysisClick(): void {
    const ref = this.dialog.open(FullReanalysisConfirmDialogComponent, {
      width: '520px',
      autoFocus: false,
    });
    ref.afterClosed().subscribe((result: FullReanalysisConfirmResult | undefined) => {
      this.handleFullReanalysisResult(result);
    });
  }

  /**
   * SF-125-01 — Dispatch selon le résultat de la dialog de confirmation.
   * Méthode extraite pour testabilité (indépendante du cycle MatDialog).
   */
  handleFullReanalysisResult(result: FullReanalysisConfirmResult | undefined): void {
    if (result === 'ENRICH') {
      this.triggerEnrichedAnalysis();
    } else if (result === 'FULL') {
      this.triggerAnalysis();
    }
    // CANCEL ou undefined → no-op
  }

  /**
   * SF-125-01 — Déclenche une analyse ENRICHED (réutilise le service partagé).
   * Gère 402 (limite plan) et 409 (aucune nouvelle réponse depuis dernière enrichie).
   */
  private triggerEnrichedAnalysis(): void {
    const id = this.caseFile()?.id;
    if (!id) return;
    this.reAnalyzing.set(true);

    this.reAnalysisService.reAnalyze(id).subscribe({
      next: () => {
        this.analyticsService.trackEvent('analysis_launched', { type: 'ENRICHED' });
        this.reAnalyzing.set(false);
        this.loadAnalysisJobs(id, true);
        this.globalNotificationService.track(id);
      },
      error: (err: any) => {
        this.reAnalyzing.set(false);
        if (err.status === 402) {
          this.snackBar.open("Limite d'analyses atteinte pour ce dossier. Passez au plan supérieur.", 'Fermer', {
            duration: 5000, panelClass: ['snack-error']
          });
        } else if (err.status === 409) {
          this.snackBar.open(
            'Aucune nouvelle réponse, message chat ou action sur la checklist procédurale depuis la dernière analyse enrichie.',
            'Fermer', { duration: 6000, panelClass: ['snack-error'] }
          );
        } else {
          this.snackBar.open("Erreur lors du déclenchement de l'enrichissement.", 'Fermer', {
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
        // F-124 : détecter une nouvelle version de synthèse pour rafraîchir le dashboard décisionnel.
        // On compare avec lastCompletedSynthesisVersion (signal qui survit au reset synthesis=null
        // déclenché par triggerAnalysis). Trigger uniquement si la version a réellement changé —
        // le polling appelle loadSynthesis à chaque tick DONE, on évite le spam.
        const previousVersion = this.lastCompletedSynthesisVersion();
        this.synthesis.set(result);
        if (result?.version !== undefined && result?.version !== null) {
          if (previousVersion !== null && result.version !== previousVersion) {
            this.dashboardRefreshService.triggerRefresh();
          }
          this.lastCompletedSynthesisVersion.set(result.version);
        }
        // Reload questions for this specific version once synthesis id is known
        const questionsDone = this.analysisJobs().some(
          j => j.jobType === 'QUESTION_GENERATION' && j.status === 'DONE'
        );
        if (questionsDone && result?.id) {
          this.loadQuestions(caseFileId, result.id);
        }
        if (result?.id) {
          this.loadProcedureChecks(caseFileId, result.id);
        } else {
          this.procedureChecks.set([]);
        }
        this.loadVersionsCount(caseFileId);
      },
      error: () => { /* silencieux */ }
    });
  }

  private loadProcedureChecks(caseFileId: string, analysisId: string): void {
    this.procedureCheckService.list(caseFileId, analysisId).subscribe({
      next: checks => this.procedureChecks.set(checks),
      error: () => this.procedureChecks.set([]),
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

    const initialProgresses = new Map<string, number>(files.map(f => [f.name, 0]));
    this.fileUploadProgresses.set(initialProgresses);

    const formsMode = this.ocrFormsMode();
    const ocrEnabled = this.ocrEnabled();
    // Cohérence : si OCR désactivé, forms mode ignoré (checkbox grisée côté UI)
    const effectiveFormsMode = ocrEnabled && formsMode;
    const uploads = files.map(f =>
      this.documentService.uploadWithProgress(caseFileId, f, effectiveFormsMode, ocrEnabled).pipe(
        tap(event => {
          if (event.type === HttpEventType.UploadProgress && event.total) {
            const pct = Math.round((event.loaded / event.total) * 100);
            this.fileUploadProgresses.update(m => new Map(m).set(f.name, pct));
          }
        }),
        filter(event => event.type === HttpEventType.Response),
        map(event => (event as HttpResponse<Document>).body as Document),
        catchError(err => of({ error: err }))
      )
    );

    forkJoin(uploads).subscribe(results => {
      const succeeded = results.filter(r => !('error' in r)) as Document[];
      const failed = results.filter(r => 'error' in r) as { error: any }[];

      this.uploading.set(false);
      this.fileUploadProgresses.set(new Map());
      this.pendingFiles.set([]);
      this.ocrFormsMode.set(false); // SF-122-03 : reset après chaque batch
      this.ocrEnabled.set(true);    // SF-122-07 : reset après chaque batch

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

  exportZip(): void {
    const id = this.caseFile()?.id;
    if (!id) return;
    this.caseFileService.exportZip(id).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'dossier.zip';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.snackBar.open("Erreur lors de l'export", 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }

  openEditDialog(): void {
    const cf = this.caseFile();
    if (!cf) return;
    const ref = this.dialog.open(CaseFileEditDialogComponent, {
      data: { id: cf.id, title: cf.title, description: cf.description } satisfies CaseFileEditDialogData,
      width: '500px',
      maxWidth: '95vw'
    });
    ref.afterClosed().subscribe(updated => {
      if (!updated) return;
      this.caseFile.update(current => current ? { ...current, title: updated.title, description: updated.description } : current);
      this.snackBar.open('Dossier modifié avec succès', 'Fermer', { duration: 3000, panelClass: ['snack-success'] });
    });
  }
}
