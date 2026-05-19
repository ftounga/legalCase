import { Component, OnInit, OnDestroy, signal, computed, ViewChild, ElementRef, inject } from '@angular/core';
import { Subscription, forkJoin, of, retry } from 'rxjs';
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
import { MatTabsModule } from '@angular/material/tabs';
import { DocumentDeleteDialogComponent } from './document-delete-dialog.component';
import { CaseFileDeleteDialogComponent } from './case-file-delete-dialog.component';
import { FullReanalysisConfirmDialogComponent, FullReanalysisConfirmResult } from './full-reanalysis-confirm-dialog.component';
import { ConfirmDialogComponent, ConfirmDialogData } from '../../shared/confirm-dialog/confirm-dialog.component';
import { DocumentPreviewDialogComponent, DocumentPreviewDialogData } from '../document-preview-dialog/document-preview-dialog.component';
import { CaseFileEditDialogComponent, CaseFileEditDialogData } from '../case-file-edit-dialog/case-file-edit-dialog.component';
import { ShareDialogComponent, ShareDialogData } from '../share-dialog/share-dialog.component';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseFileStatusService } from '../../core/services/case-file-status.service';
import { DocumentService } from '../../core/services/document.service';
import { AnalysisJobService } from '../../core/services/analysis-job.service';
import { CaseAnalysisService } from '../../core/services/case-analysis.service';
import { TypeLitigeOverrideService } from '../../core/services/type-litige-override.service';
import { TypeLitigeOverrideResponse } from '../../core/models/type-litige-override.model';
import { CaseAnalysisCommandService } from '../../core/services/case-analysis-command.service';
import { ReAnalysisService } from '../../core/services/re-analysis.service';
import { GlobalAnalysisNotificationService } from '../../core/services/global-analysis-notification.service';
import { DecisionalToolsProgressService } from '../decisional-tools-panel/decisional-tools-progress.service';
import { AiQuestionService } from '../../core/services/ai-question.service';
import { ProcedureCheckService } from '../../core/services/procedure-check.service';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AuthService } from '../../core/services/auth.service';
import { WorkspaceMemberService } from '../../core/services/workspace-member.service';
import { WorkspaceService } from '../../core/services/workspace.service';
import { AnalyticsService } from '../../core/services/analytics.service';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CaseFile } from '../../core/models/case-file.model';
import { Document, extractionFailureLabel, extractionRecoveryHint, documentPieceTypeIcon, documentPieceTypeLabel } from '../../core/models/document.model';
import { AnalysisJob } from '../../core/models/analysis-job.model';
import { CaseAnalysisPartialResponse, CaseAnalysisResult } from '../../core/models/case-analysis.model';
import { STREAMING_EXPECTED_SECTIONS } from '../synthesis/streaming-sections';
import { CaseFileStats } from '../../core/models/case-file-stats.model';
import { CaseFileStatsService } from '../../core/services/case-file-stats.service';
import { CaseNotesSectionComponent } from '../case-notes-section/case-notes-section.component';
import { CaseDeadlinesSectionComponent } from '../case-deadlines-section/case-deadlines-section.component';
import { ProcedureStageSectionComponent } from '../procedure-stage-section/procedure-stage-section.component';
import { ConclusionsSectionComponent } from '../conclusions-section/conclusions-section.component';
import { CaseDashboardComponent } from '../case-dashboard/case-dashboard.component';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { DecisionToolsPanelComponent } from '../decisional-tools-panel/decisional-tools-panel.component';
import { CaseDashboardStepperComponent, DashboardStep, StepActivation } from '../case-dashboard-stepper/case-dashboard-stepper.component';
import { AnalysisPipelineComponent } from '../analysis-pipeline/analysis-pipeline.component';
import { CaseDeadlineService } from '../../core/services/case-deadline.service';
import { CaseDeadline } from '../../core/models/case-deadline.model';
import { OcrRetryService, OcrRetryPreview } from '../../core/services/ocr-retry.service';
import { QuotaErrorStateService } from '../../core/services/quota-error-state.service';
import { fadeInUp, listStagger } from '../../shared/animations';
import { TimerWidgetComponent } from '../../shared/timer-widget/timer-widget.component';
import { QuotaErrorBannerComponent } from '../../shared/quota-error-banner/quota-error-banner.component';

/**
 * SF-230-02 — Types image acceptés pour upload natif (validés contentType côté client).
 * HEIC/WebP sont autorisés ; le pipeline backend (SF-230-01) prend ensuite le relais.
 */
export const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/heic', 'image/webp'] as const;

/** SF-230-02 — Extensions image autorisées (fallback si contentType vide ou générique). */
export const ALLOWED_IMAGE_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.heic', '.webp'] as const;

/** SF-230-02 — Extensions documents non-image autorisées historiquement. */
export const ALLOWED_DOCUMENT_EXTENSIONS = ['.pdf', '.doc', '.docx', '.txt'] as const;

/** SF-231-02 — Extensions vidéo autorisées (fallback si contentType vide). */
export const ALLOWED_VIDEO_EXTENSIONS = ['.mp4', '.mov'] as const;

/** SF-230-02 / SF-231-02 — Liste blanche complète d'extensions autorisées (documents + images + vidéos). */
export const ALLOWED_FILE_EXTENSIONS = [
  ...ALLOWED_DOCUMENT_EXTENSIONS,
  ...ALLOWED_IMAGE_EXTENSIONS,
  ...ALLOWED_VIDEO_EXTENSIONS,
] as const;

/**
 * SF-230-02 — Limite de taille spécifique aux images (10 Mo).
 * Plus restrictif que la limite globale 50 Mo car une photo iPhone 4K HEIC ≈ 5-8 Mo.
 */
export const MAX_IMAGE_SIZE_BYTES = 10 * 1024 * 1024;

/** SF-231-02 : MIME types acceptés pour l'upload vidéo. */
export const ALLOWED_VIDEO_TYPES: readonly string[] = ['video/mp4', 'video/quicktime'];
/** SF-231-02 : taille max d'un fichier vidéo (100 Mo). */
export const MAX_VIDEO_SIZE_BYTES = 100 * 1024 * 1024;
/** SF-231-02 : durée max d'un fichier vidéo (60 s). */
export const MAX_VIDEO_DURATION_SECONDS = 60;

/** SF-230-02 — Détection MIME type d'une image autorisée. */
function isImageContentType(contentType: string | null | undefined): boolean {
  if (!contentType) return false;
  return (ALLOWED_IMAGE_TYPES as readonly string[]).includes(contentType.toLowerCase());
}

/** SF-230-02 — Extension fichier (utile pour HEIC qui n'a parfois pas de MIME du navigateur). */
function fileExtension(filename: string): string {
  const idx = filename.lastIndexOf('.');
  return idx >= 0 ? filename.substring(idx).toLowerCase() : '';
}

/**
 * SF-230-02 — Détecte si un fichier est une image autorisée (par contentType ou extension).
 * Tolère le cas HEIC où certains navigateurs ne renseignent pas le MIME type.
 */
function isAllowedImageFile(file: File): boolean {
  if (isImageContentType(file.type)) return true;
  const ext = fileExtension(file.name);
  return (ALLOWED_IMAGE_EXTENSIONS as readonly string[]).includes(ext);
}

/**
 * SF-230-02 — Détecte si un fichier est un HEIC (les navigateurs ne décodent pas
 * HEIC nativement → on affiche un placeholder plutôt qu'une thumbnail.)
 */
function isHeicFile(file: File): boolean {
  if (file.type?.toLowerCase() === 'image/heic') return true;
  return fileExtension(file.name) === '.heic';
}

/**
 * SF-230-02 / SF-231-02 — Le fichier est-il un format supporté par l'app (extension présente
 * dans la liste blanche) ? Permet de rejeter `.svg`/`.gif` même si la dialog OS
 * "tous fichiers" les a laissés passer.
 */
function isSupportedFileExtension(file: File): boolean {
  return (ALLOWED_FILE_EXTENSIONS as readonly string[]).includes(fileExtension(file.name));
}

/**
 * F-244 SF-244-01 — index des 4 onglets (`mat-tab-group`) du détail dossier.
 * Onglets en état UI (signal `selectedTabIndex`), non routés. Le `case-dashboard-stepper`
 * et le scroll par query param `?section=` s'appuient sur ces constantes pour basculer
 * sur le bon onglet avant de scroller vers une ancre.
 */
export const TAB_DOSSIER = 0;
export const TAB_ANALYSE = 1;
export const TAB_DECISION = 2;
export const TAB_SUIVI = 3;

@Component({
  selector: 'app-case-file-detail',
  standalone: true,
  imports: [
    RouterLink, DatePipe, DecimalPipe, UpperCasePipe,
    MatCardModule, MatButtonModule, MatIconModule,
    MatTableModule, MatProgressSpinnerModule, MatProgressBarModule,
    MatDialogModule, MatMenuModule, MatTooltipModule, MatTabsModule, ShareDialogComponent, CaseNotesSectionComponent,
    CaseDeadlinesSectionComponent, CaseDashboardStepperComponent,
    ProcedureStageSectionComponent,
    ConclusionsSectionComponent,
    TimerWidgetComponent,
    CaseDashboardComponent, AnalysisPipelineComponent,
    DecisionToolsPanelComponent,
    QuotaErrorBannerComponent
  ],
  templateUrl: './case-file-detail.component.html',
  styleUrl: './case-file-detail.component.scss',
  animations: [fadeInUp, listStagger],
  host: { '[@fadeInUp]': '' },
  providers: [CaseDashboardRefreshService, DecisionalToolsProgressService],
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
  // SF-98-54 : passe à true dès que le 1er chargement des jobs d'analyse réussit.
  analysisJobsLoaded = signal(false);
  // SF-170-02 : section Documents repliable en accordéon, dépliée par défaut, sans persistance.
  // Le toggle manuel reste possible intra-session pour gagner de l'espace vertical sur dossiers riches,
  // mais aucun état n'est sauvegardé entre rechargements.
  readonly docsCollapsed = signal(false);
  /**
   * F-244 SF-244-01 — onglet actif du `mat-tab-group` (Dossier / Analyse /
   * Décision / Suivi). État UI pur, non routé : réinitialisé à `TAB_DOSSIER`
   * à chaque ouverture de dossier. Piloté par le clic d'étape du
   * `case-dashboard-stepper` et par le query param `?section=`.
   */
  readonly selectedTabIndex = signal(TAB_DOSSIER);
  /**
   * F-244 SF-244-02 — Total agrégé des champs pré-remplis par l'IA sur les
   * outils visibles de l'onglet « Décision ». Alimenté par l'`@Output`
   * `prefillTotalChange` du `decisional-tools-panel`. Porté en badge
   * `auto_awesome` sur l'onglet « Décision » du `mat-tab-group` : un onglet
   * fermé ne masque pas le travail de l'IA (sous-règle anti-surcharge de
   * l'audit `screen-coherence-challenger` 2026-05-15). Badge masqué quand 0.
   */
  readonly decisionPrefillTotal = signal(0);
  currentMemberRole = signal<string | null>(null);
  workspaceCountry = signal<string>('FRANCE');
  /**
   * F-197 SF-197-02 — Override avocat single-value du type de litige (Travail
   * FR) ou type de procédure (Immigration). Lu une fois au montage du
   * dossier, propagé via `[typeLitigeOverride]` au panel
   * `<app-decisional-tools-panel>` pour pré-fill outils. Cohérence F-176
   * stricte : aucun re-fetch, aucun refresh outils déclenché par le PUT
   * depuis la synthèse.
   */
  typeLitigeOverride = signal<TypeLitigeOverrideResponse | null>(null);

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

  // SF-159-04 : instrumentation diagnostique — mémorise le dernier statut connu
  // par jobType pour détecter les transitions `* → FAILED` et logger leur contexte.
  // Réinitialisé au montage du composant (pas de persistance cross-instance).
  private previousJobStatuses = new Map<string, string>();

  // SF-121-02 : exposé au template pour le tooltip du badge "Non analysable"
  readonly extractionFailureLabel = extractionFailureLabel;
  /** SF-121-06 : message de récupération actionnable affiché sous un doc FAILED. */
  readonly extractionRecoveryHint = extractionRecoveryHint;
  readonly documentPieceTypeIcon = documentPieceTypeIcon;
  readonly documentPieceTypeLabel = documentPieceTypeLabel;

  // SF-190-03 : dernier état partiel reçu pendant le streaming d'une analyse standard
  // ou enrichie. Permet d'afficher "X/7 sections reçues" dans le bandeau de progression
  // (parité avec le bandeau de la page synthèse, F-190 SF-190-01).
  lastPartial = signal<CaseAnalysisPartialResponse | null>(null);

  /**
   * SF-190-03 — progression numérique du streaming des sections de synthèse.
   * `received` = nombre de sections présentes dans le dernier partial reçu
   * (0 si aucun partial encore arrivé) ; `expected` = nombre fixe de sections
   * attendues. Le banner masque la sous-ligne quand aucun job CASE_ANALYSIS /
   * ENRICHED_ANALYSIS n'est actif, donc la valeur "0/7" ne fuite pas hors
   * streaming.
   */
  readonly streamingProgress = computed<{ received: number; expected: number }>(() => {
    const partial = this.lastPartial();
    const sections = partial?.sections;
    const received = sections
      ? STREAMING_EXPECTED_SECTIONS.filter(desc => sections[desc.id] != null).length
      : 0;
    return { received, expected: STREAMING_EXPECTED_SECTIONS.length };
  });

  // SF-125-01 : bouton contextuel — ENRICHED si analyse DONE + au moins 1 input avocat
  // (réponse Q&A OU check procédural validé). Le chat n'est pas chargé côté case-file-detail,
  // le backend validera via la condition complète (Q&A + chat + checks).
  readonly hasAnyAnalysis = computed(() => this.synthesis() !== null);

  /**
   * SF-98-54 — Pré-requis « analyse terminée » de la section Conclusions, en
   * tri-état. Vaut `undefined` tant que l'état des jobs d'analyse n'est pas
   * connu (jobs pas encore chargés, ou chargement en échec) : le composant
   * `conclusions-section` laisse alors le backend trancher (409
   * ANALYSIS_NOT_READY). Une fois les jobs chargés, booléen réel selon la
   * présence d'un job CASE_ANALYSIS DONE. Évite que le bouton « Générer les
   * conclusions » soit grisé à tort pendant le chargement (notamment au
   * retour de navigation, où le composant est recréé et la synthèse rechargée).
   */
  readonly hasCompletedAnalysis = computed<boolean | undefined>(() => {
    if (!this.analysisJobsLoaded()) {
      return undefined;
    }
    return this.analysisJobs().some(
      j => j.jobType === 'CASE_ANALYSIS' && j.status === 'DONE',
    );
  });

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

  readonly docColumns = ['name', 'type', 'size', 'date', 'preview', 'actions'];
  readonly visibleJobs = computed(() => this.analysisJobs().filter(j => j.jobType !== 'CHUNK_ANALYSIS'));

  // documents uploaded after the last synthesis — not covered by the current synthesis
  readonly outdatedDocuments = computed(() => {
    const syn = this.synthesis();
    if (!syn?.updatedAt) return [];
    const synDate = new Date(syn.updatedAt);
    return this.documents().filter(d => new Date(d.createdAt) > synDate);
  });

  /** SF-148-04 : total pièces en enrichissement vision sur le dossier. */
  readonly visionPendingCount = computed(() => {
    let n = 0;
    for (const doc of this.documents()) {
      for (const p of doc.pieces ?? []) {
        if (p.visionStatus === 'PENDING') n++;
      }
    }
    return n;
  });

  /** SF-148-04 : total pièces déjà enrichies (visionStatus DONE) sur le dossier. */
  readonly visionDoneCount = computed(() => {
    let n = 0;
    for (const doc of this.documents()) {
      for (const p of doc.pieces ?? []) {
        if (p.visionStatus === 'DONE') n++;
      }
    }
    return n;
  });

  /**
   * SF-148-04 : état vision agrégé par document.
   * - `PENDING` : au moins une pièce en cours → badge "Vision ⏳"
   * - `DONE`    : au moins une pièce enrichie, aucune PENDING → badge "Vision ✓"
   * - `NONE`    : rien à afficher
   */
  documentVisionState(doc: Document): 'PENDING' | 'DONE' | 'NONE' {
    const pieces = doc.pieces ?? [];
    if (pieces.some(p => p.visionStatus === 'PENDING')) return 'PENDING';
    if (pieces.some(p => p.visionStatus === 'DONE')) return 'DONE';
    return 'NONE';
  }

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

    // F-244 SF-244-01 — chaque étape déclare l'onglet contenant son bloc cible
    // (cf. TAB_*). `tabIndex: null` → étape navigant vers la route synthèse.
    // F-244 SF-244-03 — le stepper couvre désormais la séquence complète du
    // parcours : Documents → Analyse → Synthèse → Questions → Outils
    // décisionnels → Tableau de bord → Délais → Pièces manquantes (l'audit
    // pointait l'omission de Synthèse / Outils / Tableau de bord).
    const prefillTotal = this.decisionPrefillTotal();
    return [
      {
        id: 'documents',
        label: 'Documents',
        status: docs.length > 0 ? 'done' : 'pending',
        detail: docs.length > 0 ? `${docs.length} document${docs.length > 1 ? 's' : ''}` : null,
        anchorId: 'section-documents',
        tabIndex: TAB_DOSSIER,
      },
      {
        id: 'analyse',
        label: 'Analyse du dossier',
        status: this.fullAnalysisRunning() ? 'in_progress' : syn !== null ? 'done' : 'pending',
        detail: this.fullAnalysisRunning() ? 'En cours…' : syn !== null ? 'Terminée' : null,
        anchorId: 'section-analyse',
        tabIndex: TAB_ANALYSE,
      },
      {
        // F-244 SF-244-03 — étape Synthèse : navigue vers la route synthèse
        // dédiée (tabIndex + anchorId null → navigation gérée par le stepper).
        id: 'synthese',
        label: 'Synthèse',
        status: this.fullAnalysisRunning() ? 'in_progress' : syn !== null ? 'done' : 'pending',
        detail: syn !== null ? 'Disponible' : null,
        anchorId: null,
        tabIndex: null,
      },
      {
        id: 'questions',
        label: 'Questions complémentaires',
        status: syn === null ? 'pending' : (qs.length > 0 && pendingQuestions === 0) ? 'done' : 'pending',
        detail: syn !== null && pendingQuestions > 0 ? `${pendingQuestions} en attente` : null,
        anchorId: null,
        tabIndex: null,
      },
      {
        // F-244 SF-244-03 — étape Outils décisionnels : point d'entrée navigable
        // vers l'onglet Décision. Reste `pending` (donc cliquable) une fois la
        // synthèse disponible — pas de critère objectif de complétion. Porte le
        // badge prefill agrégé (decisionPrefillTotal, partagé avec SF-244-02).
        id: 'outils',
        label: 'Outils décisionnels',
        status: 'pending',
        detail: prefillTotal > 0
          ? `${prefillTotal} champ${prefillTotal > 1 ? 's' : ''} pré-rempli${prefillTotal > 1 ? 's' : ''}`
          : null,
        anchorId: 'section-outils-decisionnels',
        tabIndex: TAB_DECISION,
        prefillCount: prefillTotal,
      },
      {
        // F-244 SF-244-03 — étape Tableau de bord : point d'entrée navigable
        // vers la carte tableau de bord de l'onglet Décision.
        id: 'tableau-bord',
        label: 'Tableau de bord décisionnel',
        status: 'pending',
        detail: null,
        anchorId: 'section-tableau-bord',
        tabIndex: TAB_DECISION,
      },
      {
        id: 'delais',
        label: 'Délais légaux',
        status: syn === null ? 'pending' : pendingAiDeadlines > 0 ? 'in_progress' : 'done',
        detail: pendingAiDeadlines > 0 ? `${pendingAiDeadlines} proposition${pendingAiDeadlines > 1 ? 's' : ''} IA en attente` : null,
        anchorId: 'section-deadlines',
        tabIndex: TAB_SUIVI,
      },
      {
        id: 'pieces',
        label: 'Pièces manquantes',
        status: syn === null ? 'pending' : piecesCount === 0 ? 'done' : 'pending',
        detail: piecesCount > 0 ? `${piecesCount} identifiée${piecesCount > 1 ? 's' : ''}` : null,
        anchorId: null,
        tabIndex: null,
      },
    ];
  });

  canCompare = signal(false);
  deletingDocId = signal<string | null>(null);
  pendingFiles = signal<File[]>([]);
  /**
   * SF-230-02 — Cache des URLs `blob:` générées via `URL.createObjectURL` pour
   * prévisualiser les images sélectionnées (clé = nom du fichier).
   * Les URLs sont systématiquement révoquées après upload, retrait du panier
   * ou destruction du composant pour éviter toute fuite mémoire.
   */
  pendingThumbnails = signal<Map<string, string>>(new Map());
  /**
   * SF-231-02 : durée arrondie en secondes par fichier vidéo en attente
   * (clé = `file.name`). Renseignée par `onFileSelected` après lecture
   * asynchrone de la durée HTML5, consommée par `uploadPendingFiles` pour
   * envoyer le header `X-Video-Duration-Seconds` requis par le backend.
   */
  videoDurationsByName = signal<Map<string, number>>(new Map());
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
  private progressSynced = false;
  // SF-171-02 : nettoyage du state quota au switch de workspace.
  private workspaceSwitchSub: Subscription | null = null;

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
    private progressService: DecisionalToolsProgressService,
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
    private ocrRetryService: OcrRetryService,
    protected quotaErrorState: QuotaErrorStateService,
    private typeLitigeOverrideService: TypeLitigeOverrideService,
  ) {}

  // SF-171-02 : signal des codes 402 qui doivent désactiver les boutons d'analyse.
  readonly analysisQuotaBlocked = computed(() => {
    const code = this.quotaErrorState.error()?.code;
    return code === 'TOKEN_BUDGET_EXCEEDED' || code === 'CASE_ANALYSIS_LIMIT_EXCEEDED';
  });

  // SF-171-02 : tooltip standard pour boutons en disabled-quota.
  readonly analysisQuotaTooltip = 'Quota mensuel atteint — passez au plan supérieur';

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    // SF-159-04 : reset diagnostique à chaque montage.
    this.previousJobStatuses.clear();

    // SF-IA-03-19 : scroll vers la section cible quand on arrive via un popover d'incohérence.
    // SF-171-02 : ?upgraded=success → vide le state quota (retour Stripe checkout success).
    this.route.queryParamMap?.subscribe(params => {
      if (params.get('upgraded') === 'success') {
        this.quotaErrorState.clear();
      }
      const section = params.get('section');
      const doc = params.get('doc');
      // Priorité au document précis si présent (highlight ligne), sinon la section.
      // F-244 SF-244-01 — les sections étant désormais réparties en onglets,
      // on bascule d'abord sur le bon onglet avant de scroller vers l'ancre.
      if (doc) {
        this.selectedTabIndex.set(TAB_DOSSIER);
        setTimeout(() => this.scrollAndHighlight('doc-' + doc), 0);
        return;
      }
      if (!section) return;
      // F-244 SF-244-04 — `?section=decision` ouvre le détail dossier sur
      // l'onglet « Décision » scrollé sur le panel d'outils : point d'entrée
      // depuis l'écran synthèse (reconnexion synthèse → outils).
      const target = section === 'documents' ? { tab: TAB_DOSSIER, anchor: 'section-documents' }
                   : section === 'analyse' ? { tab: TAB_ANALYSE, anchor: 'section-analyse' }
                   : section === 'decision' ? { tab: TAB_DECISION, anchor: 'section-outils-decisionnels' }
                   : section === 'deadlines' ? { tab: TAB_SUIVI, anchor: 'section-deadlines' }
                   : null;
      if (target) {
        this.selectedTabIndex.set(target.tab);
        setTimeout(() => this.scrollAndHighlight(target.anchor), 0);
      }
    });

    // SF-171-02 : reset du state quota au switch de workspace.
    this.workspaceSwitchSub = this.workspaceService.workspaceSwitched$.subscribe(() => {
      this.quotaErrorState.clear();
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
            // SF-190-03 — fin du streaming → reset du compteur sections.
            this.lastPartial.set(null);
          }
        } else if (event.status === 'PARTIAL'
                   && (event.jobType === 'CASE_ANALYSIS' || event.jobType === 'ENRICHED_ANALYSIS')) {
          // SF-190-03 — récupère l'état partiel courant pour mettre à jour le compteur
          // "X/7 sections reçues" dans le bandeau de progression. Pas de loadAnalysisJobs
          // ici : les jobs ne changent pas pendant le streaming, seul le partial évolue.
          this.caseAnalysisService.getPartial(id).subscribe({
            next: partial => this.lastPartial.set(partial),
            error: () => { /* silencieux : pas de partial → compteur masqué */ }
          });
        } else if (event.status === 'FAILED') {
          this.loadAnalysisJobs(id);
          // SF-190-03 — analyse échouée → reset du compteur.
          if (event.jobType === 'CASE_ANALYSIS' || event.jobType === 'ENRICHED_ANALYSIS') {
            this.lastPartial.set(null);
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
        // F-197 SF-197-02 — fail-open silencieux (CA-09) : si le GET échoue,
        // l'override reste à null, le panel utilise la valeur IA brute.
        this.typeLitigeOverrideService.getForCaseFile(id).subscribe({
          next: response => this.typeLitigeOverride.set(response ?? null),
          error: () => this.typeLitigeOverride.set(null),
        });
        this.setupVisibilityRefetch(id);
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
    this.workspaceSwitchSub?.unsubscribe();
    if (this.visibilityHandler) {
      document.removeEventListener('visibilitychange', this.visibilityHandler);
      this.visibilityHandler = null;
    }
    // SF-230-02 : nettoie les URLs `blob:` restantes au cas où l'utilisateur
    // quitte la page sans avoir uploadé les fichiers en attente.
    this.revokeAllThumbnails();
  }

  // SF-186-01 — refetch défensif au retour de visibilité (focus tab, retour sur
  // la page après mise en veille, etc.) pour pallier un éventuel SSE perdu.
  // Le fix principal est dans `analysis-sse.service.ts` (auto-reconnect), mais
  // ce hook secondaire garantit qu'on ne reste jamais avec un état stale.
  private visibilityHandler: (() => void) | null = null;
  private setupVisibilityRefetch(caseFileId: string): void {
    this.visibilityHandler = () => {
      if (document.visibilityState === 'visible') {
        this.loadAnalysisJobs(caseFileId);
        this.loadDocuments(caseFileId);
      }
    };
    document.addEventListener('visibilitychange', this.visibilityHandler);
  }

  // ── SF-170-02 : accordéon section Documents (toggle in-session, pas de persistance) ──

  toggleDocsCollapsed(): void {
    this.docsCollapsed.set(!this.docsCollapsed());
  }

  /**
   * F-244 SF-244-01 — handler du clic d'étape du `case-dashboard-stepper`.
   * Bascule d'abord sur l'onglet contenant le bloc cible, puis scrolle vers
   * l'ancre une fois le contenu de l'onglet visible (le scroll d'ancre seul ne
   * suffit plus depuis le passage en onglets). Le scroll est différé d'un tick
   * pour laisser le `mat-tab-group` afficher le panneau cible.
   */
  onStepActivated(activation: StepActivation): void {
    this.selectedTabIndex.set(activation.tabIndex);
    if (activation.anchorId) {
      const anchorId = activation.anchorId;
      setTimeout(() => this.scrollAndHighlight(anchorId), 0);
    }
  }

  /**
   * SF-121-06 — handler de l'`@Output` `manageFailedDocuments` de
   * `analysis-pipeline`. La step « Analyse des documents » en échec (onglet
   * Analyse) renvoie vers l'onglet Dossier, où vit l'action de récupération
   * (supprimer / ré-uploader la pièce). Réutilise le pont inter-onglets
   * `selectedTabIndex` + `scrollAndHighlight`. Bascule idempotente.
   */
  onManageFailedDocuments(): void {
    this.selectedTabIndex.set(TAB_DOSSIER);
    setTimeout(() => this.scrollAndHighlight('section-documents'), 0);
  }

  /**
   * F-244 SF-244-02 — handler de l'`@Output` `prefillTotalChange` du
   * `decisional-tools-panel`. Met à jour le compteur agrégé porté en badge
   * `auto_awesome` sur l'onglet « Décision ». `case-file-detail` n'est pas en
   * `ChangeDetectionStrategy.OnPush` : la mutation du signal suffit à
   * déclencher la CD, pas besoin de `markForCheck()`.
   */
  onDecisionPrefillTotalChange(total: number): void {
    this.decisionPrefillTotal.set(total);
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
        // SF-148-04 : si l'utilisateur ouvre le dossier alors que l'enrichissement
        // vision est encore en cours (async), démarrer le polling pour rafraîchir
        // les pièces et basculer les badges PENDING → DONE sans recharger la page.
        if (this.visionPendingCount() > 0) {
          this.managePolling(caseFileId, this.analysisJobs(), true);
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

    const data: ConfirmDialogData = {
      title: 'Relancer avec OCR',
      message: `Relancer l'OCR sur ${preview.failedDocsCount} document(s) ? ` +
          `Cela va consommer ~${preview.estimatedPages} pages de votre quota OCR ` +
          `(restantes : ${preview.monthlyRemaining + preview.packsRemaining}).`,
      confirmLabel: 'Relancer',
      confirmColor: 'primary',
    };
    this.dialog.open(ConfirmDialogComponent, { data, width: '480px' })
        .afterClosed().subscribe(ok => {
          if (ok) this.runOcrRetry(id);
        });
  }

  private runOcrRetry(id: string): void {
    const preview = this.ocrRetryPreview();
    if (!preview) return;
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
        // SF-98-54 : l'état des jobs d'analyse est désormais connu (succès) —
        // débloque le tri-état `hasCompletedAnalysis`.
        this.analysisJobsLoaded.set(true);
        // SF-159-04 : log diagnostique de toute transition vers FAILED.
        this.detectAndLogFailedTransition(caseFileId, jobs, 'loadAnalysisJobs');
        // Don't overwrite placeholders while waiting for backend to pick up upload or analysis trigger
        if (jobs.length > 0 && !this.docAnalysisPending() && !this.caseAnalysisPending()) {
          this.analysisJobs.set(jobs);
        }
        if (!this.progressSynced) {
          // SF-159-03 — initialisation autoritaire au montage (réapparition correcte
          // de la bannière sur reload pendant analyse en cours).
          this.progressService.initFromJobs(jobs);
          this.progressSynced = true;
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
    const visionPending = this.visionPendingCount() > 0;

    if ((hasPendingOrProcessing || forceStart || this.docAnalysisPending() || this.caseAnalysisPending() || visionPending) && !this.pollingInterval) {
      this.pollingInterval = setInterval(() => {
        // SF-121-04 : re-fetch docs pour détecter l'état d'extraction courant,
        // puis re-applique l'override FAILED si ≥ 1 doc FAILED.
        this.refreshDocumentsAndApplyOverride(caseFileId);
        this.analysisJobService.getJobs(caseFileId).subscribe({
          next: updated => {
            // SF-159-04 : log diagnostique de toute transition vers FAILED.
            this.detectAndLogFailedTransition(caseFileId, updated, 'pollingTick');
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
            // SF-159-03 — défense en profondeur : sync "remove only" du progress service
            // pour clear la bannière même si l'event SSE de fin n'a pas été reçu.
            this.progressService.syncFromJobs(updated);
            const stillRunning = updated.some(
              j => j.status === 'PENDING' || j.status === 'PROCESSING'
            );
            const caseAnalysisDone = updated.some(j => j.jobType === 'CASE_ANALYSIS' && j.status === 'DONE');
            const enrichedAnalysisDone = updated.some(j => j.jobType === 'ENRICHED_ANALYSIS' && j.status === 'DONE');
            const questionsDone = updated.some(j => j.jobType === 'QUESTION_GENERATION' && (j.status === 'DONE' || j.status === 'FAILED'));
            const waitingForQuestions = caseAnalysisDone && !questionsDone;
            // F-227 SF-227-01 — fallback si SSE DONE event missed during navigation
            // (re-track sur SseNotificationService ne replay pas les événements). Le
            // polling tick recharge la synthèse à la transition PROCESSING→DONE.
            // Idempotent grâce à `lastCompletedSynthesisVersion` dans loadSynthesis.
            if (caseAnalysisDone || enrichedAnalysisDone) {
              this.loadSynthesis(caseFileId);
            }
            // SF-148-04 : continuer le polling tant qu'au moins une pièce est
            // en enrichissement visuel (LegalCase Vision, async post-pipeline).
            const stillVision = this.visionPendingCount() > 0;
            if (!stillRunning && !waitingForQuestions && !stillVision) {
              this.stopPolling();
            }
          }
        });
      }, 3000);
    } else if (!hasPendingOrProcessing && !forceStart && !this.docAnalysisPending() && !this.caseAnalysisPending() && !visionPending) {
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
  /**
   * SF-159-04 — Instrumentation diagnostique : détecte les transitions
   * `* → FAILED` côté frontend et écrit un `console.warn` enrichi pour permettre
   * l'analyse post-mortem du bug intermittent observé sur Chen 13 (2026-05-05),
   * où la pipeline tile affichait CASE_ANALYSIS en rouge alors que le backend
   * était encore PROCESSING. Idempotent : un même statut FAILED reçu plusieurs
   * fois consécutivement n'est loggé qu'une seule fois. Aucun envoi Sentry V1.
   */
  private detectAndLogFailedTransition(caseFileId: string, jobs: AnalysisJob[], source: string): void {
    for (const job of jobs) {
      const previousStatus = this.previousJobStatuses.get(job.jobType);
      if (job.status === 'FAILED' && previousStatus !== 'FAILED') {
        // eslint-disable-next-line no-console
        console.warn('[SF-159-04] AnalysisJob transition to FAILED', {
          caseFileId,
          jobType: job.jobType,
          previousStatus: previousStatus ?? '(none)',
          newJob: job,
          allJobs: jobs,
          source,
          timestamp: new Date().toISOString(),
        });
      }
      this.previousJobStatuses.set(job.jobType, job.status);
    }
  }

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
          || x.failureReason !== y.failureReason
          || x.ocrRunning !== y.ocrRunning
          || x.ocrExtracted !== y.ocrExtracted) {
        return false;
      }
      // SF-148-04 : les pièces (F-145) et leur statut vision (F-148) sont
      // produits de façon asynchrone après l'upload. Si ce diff les ignore,
      // les chips et badges Vision ne se rafraîchissent pas pendant le polling.
      if (!this.piecesEqual(x.pieces, y.pieces)) {
        return false;
      }
    }
    return true;
  }

  private piecesEqual(a?: Document['pieces'], b?: Document['pieces']): boolean {
    const la = a?.length ?? 0;
    const lb = b?.length ?? 0;
    if (la !== lb) return false;
    if (la === 0) return true;
    for (let i = 0; i < la; i++) {
      const p = a![i], q = b![i];
      if (p.id !== q.id
          || p.type !== q.type
          || p.label !== q.label
          || p.visionStatus !== q.visionStatus
          || (p.visualDescription ?? null) !== (q.visualDescription ?? null)) {
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
        this.progressService.start('CASE_ANALYSIS');
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
        // SF-171-02 : 402 géré par paymentRequiredInterceptor + QuotaErrorState (bandeau persistant).
        if (err.status === 402) {
          // no-op
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
        this.progressService.start('ENRICHED_ANALYSIS');
        this.loadAnalysisJobs(id, true);
        this.globalNotificationService.track(id);
      },
      error: (err: any) => {
        this.reAnalyzing.set(false);
        // SF-171-02 : 402 géré par paymentRequiredInterceptor + QuotaErrorState (bandeau persistant).
        if (err.status === 402) {
          // no-op
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
    // SF-98-54 : retry sur échec transitoire — un getAnalysis échoué ne doit
    // pas laisser la synthèse durablement absente sans recours.
    this.caseAnalysisService.getAnalysis(caseFileId)
      .pipe(retry({ count: 2, delay: 1000 }))
      .subscribe({
      next: result => {
        // F-124 : détecter une nouvelle version de synthèse pour rafraîchir le dashboard décisionnel.
        // On compare avec lastCompletedSynthesisVersion (signal qui survit au reset synthesis=null
        // déclenché par triggerAnalysis). Trigger uniquement si la version a réellement changé —
        // le polling appelle loadSynthesis à chaque tick DONE, on évite le spam.
        const previousVersion = this.lastCompletedSynthesisVersion();
        this.synthesis.set(result);
        if (result?.version !== undefined && result?.version !== null) {
          // Déclencher refresh dashboard à toute nouvelle version, y compris la
          // première (previousVersion === null). Le panel décisionnel a besoin
          // de recharger sa visibilité dès que les flags IA (divorceDcEnvisage,
          // etc.) sont extraits — sans quoi les outils CONTEXTUAL restent
          // invisibles jusqu'au refresh manuel (cas Vermeersch BE 2026-05-11).
          if (result.version !== previousVersion) {
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

    // SF-231-02 : sépare d'abord les vidéos — elles ont leur propre limite de taille
    // (100 Mo) et un contrôle asynchrone de la durée géré par `validateAndAddVideo`.
    const videos = files.filter(f => this.isVideoFile(f));
    const nonVideos = files.filter(f => !this.isVideoFile(f));

    // SF-230-02 : 3 niveaux de validation côté client (avant tout appel serveur).
    //   1) Extension dans la liste blanche (rejette `.svg`, `.gif` etc. même si
    //      la dialog OS "tous fichiers" les a laissés passer).
    //   2) Taille image plus restrictive (10 Mo) que documents (50 Mo).
    //   3) Taille globale (50 Mo) pour les documents.
    const unsupported: File[] = [];
    const oversizedImages: File[] = [];
    const oversized: File[] = [];
    const valid: File[] = [];

    for (const f of nonVideos) {
      if (!isSupportedFileExtension(f)) {
        unsupported.push(f);
        continue;
      }
      const isImage = isAllowedImageFile(f);
      if (isImage && f.size > MAX_IMAGE_SIZE_BYTES) {
        oversizedImages.push(f);
        continue;
      }
      if (!isImage && f.size > MAX_SIZE) {
        oversized.push(f);
        continue;
      }
      valid.push(f);
    }

    if (unsupported.length > 0) {
      this.snackBar.open(
        `${unsupported.length} fichier(s) au format non supporté ignoré(s). Formats acceptés : PDF, DOC, DOCX, TXT, JPG, PNG, HEIC, WebP, MP4, MOV.`,
        'Fermer', { duration: 5000, panelClass: ['snack-error'] }
      );
    }
    if (oversizedImages.length > 0) {
      this.snackBar.open(
        `${oversizedImages.length} image(s) rejetée(s) : taille max 10 Mo par image.`,
        'Fermer', { duration: 5000, panelClass: ['snack-error'] }
      );
    }
    if (oversized.length > 0) {
      this.snackBar.open(
        `${oversized.length} fichier(s) rejeté(s) : taille max 50 Mo.`,
        'Fermer', { duration: 5000, panelClass: ['snack-error'] }
      );
    }

    if (valid.length > 0) {
      // SF-230-02 : génère une URL `blob:` pour les images non-HEIC.
      // HEIC : pas de thumbnail (placeholder textuel côté template).
      this.pendingThumbnails.update(map => {
        const next = new Map(map);
        for (const f of valid) {
          if (next.has(f.name)) continue;
          if (isAllowedImageFile(f) && !isHeicFile(f)) {
            next.set(f.name, URL.createObjectURL(f));
          }
        }
        return next;
      });
      this.pendingFiles.update(current => [...current, ...valid]);
      // SF-170-02 : auto-déplie la section Documents. Si l'avocat avait replié manuellement,
      // la liste pendingFiles et le bouton "Uploader les documents" doivent redevenir visibles.
      this.docsCollapsed.set(false);
    }

    // SF-231-02 : validation taille + durée asynchrone pour chaque vidéo.
    for (const video of videos) {
      this.validateAndAddVideo(video);
    }
  }

  /** SF-231-02 : true si le fichier est une vidéo MP4 ou MOV. */
  private isVideoFile(file: File): boolean {
    return ALLOWED_VIDEO_TYPES.includes(file.type);
  }

  /**
   * SF-231-02 : valide une vidéo (taille + durée) et l'ajoute au panier si OK.
   * - Taille > 100 Mo → snackbar erreur, pas d'ajout.
   * - Lit la durée via HTML5 `<video>.onloadedmetadata`.
   * - Durée > 60 s → snackbar erreur, pas d'ajout.
   * - Sinon : ajoute au panier + stocke la durée arrondie pour l'upload.
   */
  private validateAndAddVideo(file: File): void {
    if (file.size > MAX_VIDEO_SIZE_BYTES) {
      this.snackBar.open(
        `Vidéo « ${file.name} » trop volumineuse : 100 Mo max.`,
        'Fermer', { duration: 5000, panelClass: ['snack-error'] }
      );
      return;
    }

    const video = document.createElement('video');
    video.preload = 'metadata';
    const objectUrl = URL.createObjectURL(file);
    video.src = objectUrl;
    video.onloadedmetadata = () => {
      const rawDuration = video.duration;
      URL.revokeObjectURL(objectUrl);
      if (!isFinite(rawDuration) || rawDuration <= 0) {
        this.snackBar.open(
          `Vidéo « ${file.name} » illisible : durée indéterminée.`,
          'Fermer', { duration: 5000, panelClass: ['snack-error'] }
        );
        return;
      }
      const duration = Math.ceil(rawDuration);
      if (duration > MAX_VIDEO_DURATION_SECONDS) {
        this.snackBar.open(
          `Vidéo « ${file.name} » trop longue : ${MAX_VIDEO_DURATION_SECONDS} s max (durée détectée : ${duration} s).`,
          'Fermer', { duration: 5000, panelClass: ['snack-error'] }
        );
        return;
      }
      // OK : on enregistre la durée et on ajoute au panier.
      this.videoDurationsByName.update(m => new Map(m).set(file.name, duration));
      this.pendingFiles.update(current => [...current, file]);
      this.docsCollapsed.set(false);
    };
    video.onerror = () => {
      URL.revokeObjectURL(objectUrl);
      this.snackBar.open(
        `Vidéo « ${file.name} » illisible ou format non supporté.`,
        'Fermer', { duration: 5000, panelClass: ['snack-error'] }
      );
    };
  }

  removePendingFile(file: File): void {
    this.revokeThumbnailFor(file.name);
    this.pendingFiles.update(files => files.filter(f => f !== file));
    // SF-231-02 : nettoie la durée vidéo associée pour ne pas envoyer un
    // header X-Video-Duration-Seconds périmé sur un futur fichier homonyme.
    if (this.isVideoFile(file)) {
      this.videoDurationsByName.update(m => {
        const next = new Map(m);
        next.delete(file.name);
        return next;
      });
    }
  }

  /** SF-230-02 — Le fichier en attente est-il un HEIC ? Utilisé par le template
   *  pour afficher un placeholder textuel à la place de la thumbnail. */
  isHeicPendingFile(file: File): boolean {
    return isHeicFile(file);
  }

  /** SF-230-02 — Révoque l'URL `blob:` associée à un fichier en attente
   *  et la retire du cache. No-op si pas d'URL connue. */
  private revokeThumbnailFor(filename: string): void {
    const map = this.pendingThumbnails();
    const url = map.get(filename);
    if (!url) return;
    URL.revokeObjectURL(url);
    const next = new Map(map);
    next.delete(filename);
    this.pendingThumbnails.set(next);
  }

  /** SF-230-02 — Révoque toutes les URLs `blob:` connues (appelé après upload
   *  ou destruction du composant). */
  private revokeAllThumbnails(): void {
    const map = this.pendingThumbnails();
    for (const url of map.values()) {
      URL.revokeObjectURL(url);
    }
    if (map.size > 0) {
      this.pendingThumbnails.set(new Map());
    }
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
    const durations = this.videoDurationsByName();
    const uploads = files.map(f => {
      // SF-231-02 : pour une vidéo, on transmet le header X-Video-Duration-Seconds
      // (validation backend SF-231-01 — rejet 400 sinon).
      const videoDurationSeconds = this.isVideoFile(f) ? durations.get(f.name) : undefined;
      return this.documentService.uploadWithProgress(
        caseFileId, f, effectiveFormsMode, ocrEnabled,
        { videoDurationSeconds }
      ).pipe(
        tap(event => {
          if (event.type === HttpEventType.UploadProgress && event.total) {
            const pct = Math.round((event.loaded / event.total) * 100);
            this.fileUploadProgresses.update(m => new Map(m).set(f.name, pct));
          }
        }),
        filter(event => event.type === HttpEventType.Response),
        map(event => (event as HttpResponse<Document>).body as Document),
        catchError(err => of({ error: err }))
      );
    });

    forkJoin(uploads).subscribe(results => {
      const succeeded = results.filter(r => !('error' in r)) as Document[];
      const failed = results.filter(r => 'error' in r) as { error: any }[];

      this.uploading.set(false);
      this.fileUploadProgresses.set(new Map());
      this.pendingFiles.set([]);
      // SF-230-02 : révoque toutes les URLs `blob:` générées pour les thumbnails
      // images après upload. Évite la fuite mémoire (chaque blob retenu = ~taille
      // image × N fichiers).
      this.revokeAllThumbnails();
      this.videoDurationsByName.set(new Map()); // SF-231-02 : reset après batch
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
        this.progressService.start('DOCUMENT_ANALYSIS');
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
        // SF-171-02 : 402 (DOCUMENT_LIMIT_EXCEEDED / OCR_QUOTA_EXCEEDED) géré par paymentRequiredInterceptor + QuotaErrorState.
        const is402 = failed.some((f: any) => f.error?.status === 402);
        if (!is402) {
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

  /** SF-127-01 : ouvre le dialog d'aperçu du document (texte extrait + 1re page PDF). */
  canPreviewDocument(doc: Document): boolean {
    const status = doc.extractionStatus;
    return status === 'DONE' || status === 'FAILED';
  }

  openPreview(doc: Document, initialPieceId?: string): void {
    if (!this.canPreviewDocument(doc)) return;
    const data: DocumentPreviewDialogData = {
      caseFileId: this.caseFile()!.id,
      documentId: doc.id,
      pieces: doc.pieces ?? [],
      initialPieceId,
      // SF-145-11 : permet au dialog de filtrer les types autorisés dans le
      // dialog de reclassification selon le domaine du dossier.
      legalDomain: this.caseFile()?.legalDomain ?? undefined,
    };
    this.dialog.open(DocumentPreviewDialogComponent, {
      data,
      width: (doc.pieces?.length ?? 0) > 0 ? '960px' : '720px',
      maxWidth: '95vw',
    });
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
        // SF-171-02 : 402 (CASE_FILE_OPEN_LIMIT_EXCEEDED) géré par paymentRequiredInterceptor + QuotaErrorState.
        if (err.status !== 402) {
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
