import { ChangeDetectorRef, Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { forkJoin, of, Subscription } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { DatePipe, LowerCasePipe, NgTemplateOutlet } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
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
import { StrategicOptionService } from '../../core/services/strategic-option.service';
import { RetainedPisteAlignmentService } from '../../core/services/retained-piste-alignment.service';
import { RetainedPisteAlignment } from '../../core/models/retained-piste-alignment.model';
import { ProcedureCheckAlignmentService } from '../../core/services/procedure-check-alignment.service';
import { ProcedureCheckAlignment } from '../../core/models/procedure-check-alignment.model';
import { PieceManquanteStatusService } from '../../core/services/piece-manquante-status.service';
import { PIECE_DESTINATAIRES, PieceManquanteStatutValue } from '../../core/models/piece-manquante-status.model';
import { PieceManquanteAlignmentService } from '../../core/services/piece-manquante-alignment.service';
import { PieceManquanteAlignment } from '../../core/models/piece-manquante-alignment.model';
import { RisqueStatusService } from '../../core/services/risque-status.service';
import { RisqueStatutValue } from '../../core/models/risque-status.model';
import { RisqueAlignmentService } from '../../core/services/risque-alignment.service';
import { RisqueAlignment } from '../../core/models/risque-alignment.model';
import { AiQuestionAlignmentService } from '../../core/services/ai-question-alignment.service';
import { AiQuestionAlignment } from '../../core/models/ai-question-alignment.model';
import { TypeLitigeOverrideService } from '../../core/services/type-litige-override.service';
import {
  TypeLitigeOverrideResponse,
  TypeLitigeOverrideDomain,
  TYPE_LITIGE_TRAVAIL_FR_LABELS,
  TYPE_PROCEDURE_IMMIGRATION_LABELS,
  resolveOverrideDomain,
} from '../../core/models/type-litige-override.model';
import {
  TypeLitigeOverrideDialogComponent,
  TypeLitigeOverrideDialogData,
} from './type-litige-override-dialog/type-litige-override-dialog.component';
import { DecisionToolsPanelComponent } from '../decisional-tools-panel/decisional-tools-panel.component';
import { getToolMetadata } from '../decisional-tools-panel/decision-tool.contract';
import { StrategicOption, StrategicOptionStatus } from '../../core/models/strategic-option.model';
import { DiscardReasonDialogComponent, DiscardReasonDialogData } from './discard-reason-dialog.component';
import { SynthesisShortBlockDialogComponent, SynthesisShortBlockDialogData } from '../synthesis-short-block-dialog/synthesis-short-block-dialog.component';
import { BadgeKey, BadgeNavigationService } from '../synthesis-badges/badge-navigation.service';
import { CaseFile } from '../../core/models/case-file.model';
import { fadeInUp, listStagger } from '../../shared/animations';
import { SourceRefComponent } from '../../shared/source-ref/source-ref.component';
import { QuotaErrorBannerComponent } from '../../shared/quota-error-banner/quota-error-banner.component';
import { ImmigrationEventsSectionComponent } from '../immigration-events-section/immigration-events-section.component';
import { ImmigrationStrategyComparatorSectionComponent } from '../immigration-strategy-comparator-section/immigration-strategy-comparator-section.component';
import { DivorceConsentementScoringSectionComponent } from '../divorce-consentement-scoring-section/divorce-consentement-scoring-section.component';
import { DivorceConsentementScoring, DivorceConsentementValidityDetection, ImmigrationStrategyScenario, ImmigrationTriggerEvent } from '../../core/models/case-analysis.model';
import { CaseAnalysisPartialResponse, CaseAnalysisPartialSections, CaseAnalysisResult, CaseAnalysisVersionSummary, CompensationEstimate, PensionAlimentaireEstimate, PrestationCompensatoireEstimate, LiquidationCommunaute } from '../../core/models/case-analysis.model';
import { STREAMING_EXPECTED_SECTIONS, StreamingSection } from './streaming-sections';
import { AiQuestion } from '../../core/models/ai-question.model';
import { ChatMessage } from '../../core/models/chat-message.model';
import { ProcedureCheck, ProcedureCheckStatus } from '../../core/models/procedure-check.model';
import { TimeService } from '../../core/services/time.service';
import { TimeEntryResponse } from '../../core/models/time-tracking.models';

/**
 * F-162 SF-162-01 — descripteur d'un badge de la grille de synthèse.
 * SF-162-02..05 ajoutent un `route` pour les pages dédiées (blocs riches).
 * SF-162-06 ajoute un `popup` pour les blocs courts (pièces manquantes,
 * questions ouvertes). Précédence : `popup` > `route` > `anchor`.
 */
interface SynthesisBadge {
  id: string;
  label: string;
  icon: string;
  iconClass: string;
  count: number;
  sublabel?: string | null;
  anchor: string;
  route?: string[];
  popup?: 'pieces' | 'questions-ouvertes';
  /**
   * F-197 SF-197-02 — pour le badge "Type de litige" : valeur affichée
   * (libellé FR du type retenu). Si {@code overridden} est `true`, le badge
   * affiche le badge or "modifié par vous" et l'icône `edit_note`.
   */
  valueLabel?: string | null;
  overridden?: boolean;
  /**
   * F-197 SF-197-02 — déclencheur d'ouverture du dialog override. Distinct
   * de {@code popup} (pour les blocs courts) et de {@code route} (pages
   * dédiées) — précédence : dialog > popup > route > anchor.
   */
  dialog?: 'type-litige-override';
}

@Component({
  selector: 'app-synthesis',
  standalone: true,
  imports: [
    RouterLink, DatePipe, LowerCasePipe, NgTemplateOutlet, FormsModule,
    MatCardModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatProgressBarModule, MatExpansionModule,
    MatCheckboxModule, MatTooltipModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    SourceRefComponent,
    QuotaErrorBannerComponent,
    ImmigrationEventsSectionComponent,
    ImmigrationStrategyComparatorSectionComponent,
    DivorceConsentementScoringSectionComponent
  ],
  templateUrl: './synthesis.component.html',
  styleUrl: './synthesis.component.scss',
  animations: [fadeInUp, listStagger],
  host: { '[@fadeInUp]': '' },
})
export class SynthesisComponent implements OnInit, OnDestroy {
  caseFile = signal<CaseFile | null>(null);
  synthesis = signal<CaseAnalysisResult | null>(null);
  /**
   * F-185 SF-185-01 — la synthèse affichée est un état partiel (streaming en cours).
   * Toutes les sections ne sont pas encore arrivées ; un bandeau le signale et la
   * synthèse se complète au fil des événements SSE `CASE_ANALYSIS_PARTIAL`.
   */
  isStreaming = signal(false);
  /**
   * F-190 SF-190-01 — dernier état partiel reçu (raw, pas le projeté `synthesis()`).
   * Permet de distinguer "section pas encore arrivée" d'une "section arrivée mais
   * vide" (le projeté met `[]` dans les deux cas via `?? []`).
   */
  lastPartial = signal<CaseAnalysisPartialResponse | null>(null);
  timeEntries = signal<TimeEntryResponse[]>([]);
  private sseSubscription?: Subscription;

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

  /**
   * F-194 SF-194-02 — Cache des statuts pièces taggés localement par l'avocat.
   * Clé = libellé original de la pièce (issu de l'IA), valeur = statut.
   * Utilisé pour la mise en évidence du bouton actif. Optimistic update :
   * `set` au clic, rollback (delete) si le PUT remonte une erreur.
   *
   * <p>Ce signal NE déclenche PAS de re-fetch alignement post-PUT — cohérence
   * F-176 stricte : la matérialisation pièce → outil ne se fait qu'au prochain
   * run de Synthèse enrichie via SSE `ENRICHED_ANALYSIS DONE`.</p>
   */
  pieceStatuses = signal<Record<string, PieceManquanteStatutValue>>({});
  /** Saisie locale `raisonNonApp` par pièce (clé = libellé). Pas persistée tant que blur. */
  pieceRaisons = signal<Record<string, string>>({});
  /** Saisie locale `destinataire` par pièce. */
  pieceDestinataires = signal<Record<string, string>>({});
  /** Pièce en cours de PUT (libellé) — affiche un spinner sur la ligne. */
  updatingPieceLibelle = signal<string | null>(null);

  /** Liste des destinataires usuels exposés au template (cf. model). */
  readonly pieceDestinataireOptions = PIECE_DESTINATAIRES;
  /** F-194 SF-194-02 — utilitaire OnPush : forcer la CD après mutation signal. */
  private readonly cdr = inject(ChangeDetectorRef);

  /**
   * F-195 SF-195-02 — Cache des statuts risques taggés localement par
   * l'avocat. Clé = libellé original du risque (issu de l'IA), valeur =
   * statut. Utilisé pour la mise en évidence du bouton actif. Optimistic
   * update : `set` au clic, rollback (delete) si le PUT remonte une erreur.
   *
   * <p>Ce signal NE déclenche PAS de re-fetch alignement post-PUT — cohérence
   * F-176 stricte : la matérialisation risque → outil ne se fait qu'au
   * prochain run de Synthèse enrichie via SSE `ENRICHED_ANALYSIS DONE`.</p>
   */
  risqueStatuses = signal<Record<string, RisqueStatutValue>>({});
  /** Saisie locale `raisonEcarte` par risque (clé = libellé). PUT à blur. */
  risqueRaisons = signal<Record<string, string>>({});
  /** Risque en cours de PUT (libellé) — affiche un spinner sur la ligne. */
  updatingRisqueLibelle = signal<string | null>(null);

  /**
   * F-197 SF-197-02 — Override avocat single-value du type de litige (Travail
   * FR) ou du type de procédure (Immigration). Lu une fois au montage du
   * dossier via {@link TypeLitigeOverrideService}, mis à jour localement
   * après PUT depuis le dialog. Cohérence F-176 stricte : aucun re-fetch
   * synthèse, aucun refresh outils déclenché par le PUT — la propagation
   * outils se fait au prochain run de Synthèse enrichie via
   * {@code ENRICHED_ANALYSIS DONE}.
   */
  readonly typeLitigeOverride = signal<TypeLitigeOverrideResponse | null>(null);

  // F-160 SF-160-02 — paginators indépendants par bloc (checklist + questions)
  // Numéro de version actuellement affiché par bloc (peut diverger de la version
  // synthèse globale pour comparer la checklist d'une itération avec les questions
  // d'une autre).
  currentChecksVersion = signal<number | null>(null);
  currentQuestionsVersion = signal<number | null>(null);

  strategicOptions = signal<StrategicOption[]>([]);
  updatingOptionId = signal<string | null>(null);

  readonly optionsToStudy = computed(() =>
    this.strategicOptions().filter(o => o.statut === 'TO_STUDY').sort((a, b) => a.ordre - b.ordre)
  );
  readonly optionsRetained = computed(() =>
    this.strategicOptions().filter(o => o.statut === 'RETAINED').sort((a, b) => a.ordre - b.ordre)
  );
  readonly optionsDiscarded = computed(() =>
    this.strategicOptions().filter(o => o.statut === 'DISCARDED').sort((a, b) => a.ordre - b.ordre)
  );

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

  /**
   * F-162 SF-162-01 — grille de badges synthétiques en tête de page.
   * Retourne les blocs non vides dans l'ordre canonique avec compteur, libellé, icône
   * et ancre cible. Les badges vides (compteur = 0) sont filtrés.
   * Ordre canonique : Timeline → Faits → Points juridiques → Risques → Pistes
   * stratégiques → Checklist → Questions complémentaires → Pièces manquantes →
   * Questions ouvertes.
   */
  readonly synthesisBadges = computed<SynthesisBadge[]>(() => {
    const syn = this.synthesis();
    if (!syn) return [];
    const cfId = this.caseFile()?.id ?? null;
    const checksCount = this.procedureChecks().length;
    const questionsCount = this.questions().length;
    const optionsCount = this.strategicOptions().length;
    const optionsRetainedCount = this.optionsRetained().length;
    const risquesCount = syn.risques?.length ?? 0;
    const riskLevel = syn.riskLevel ?? null;

    // F-197 SF-197-02 — badge "Type de litige" : visible uniquement si une
    // valeur est connue (IA détectée OU override avocat) et si le domaine
    // supporte l'override (TRAVAIL_FR / IMMIGRATION).
    const typeLitigeLabel = this.currentTypeLitigeLabel();
    const typeLitigeOverridden = this.typeLitigeIsOverridden();
    const typeLitigeDomain = this.typeLitigeOverrideDomain();
    const typeLitigeBadge: SynthesisBadge | null = (typeLitigeLabel && typeLitigeDomain) ? {
      id: 'type-litige',
      label: 'Type de litige',
      icon: typeLitigeOverridden ? 'edit_note' : 'fact_check',
      iconClass: typeLitigeOverridden ? 'panel-icon--type-litige-override' : 'panel-icon--type-litige',
      count: 1,
      sublabel: typeLitigeOverridden ? 'modifié par vous' : null,
      anchor: 'section-type-litige',
      valueLabel: typeLitigeLabel,
      overridden: typeLitigeOverridden,
      dialog: 'type-litige-override',
    } : null;

    const all: SynthesisBadge[] = [
      {
        id: 'timeline',
        label: 'Timeline',
        icon: 'timeline',
        iconClass: 'panel-icon--timeline',
        count: syn.timeline?.length ?? 0,
        anchor: 'section-timeline',
        route: cfId ? ['/case-files', cfId, 'synthesis', 'timeline'] : undefined,
      },
      {
        id: 'faits',
        label: 'Faits',
        icon: 'gavel',
        iconClass: 'panel-icon--faits',
        count: syn.faits?.length ?? 0,
        anchor: 'section-faits',
        route: cfId ? ['/case-files', cfId, 'synthesis', 'faits'] : undefined,
      },
      {
        id: 'points-juridiques',
        label: 'Points juridiques',
        icon: 'balance',
        iconClass: 'panel-icon--juridique',
        count: syn.pointsJuridiques?.length ?? 0,
        anchor: 'section-points-juridiques',
        route: cfId ? ['/case-files', cfId, 'synthesis', 'points-juridiques'] : undefined,
      },
      {
        id: 'risques',
        label: 'Risques',
        icon: 'warning_amber',
        iconClass: 'panel-icon--risques',
        count: risquesCount,
        sublabel: riskLevel === 'ELEVE' && risquesCount > 0 ? 'gravité élevée' : null,
        anchor: 'section-risques',
        route: cfId ? ['/case-files', cfId, 'synthesis', 'risques'] : undefined,
      },
      {
        id: 'pistes',
        label: 'Pistes stratégiques',
        icon: 'lightbulb',
        iconClass: 'panel-icon--pistes',
        count: optionsCount,
        sublabel: optionsRetainedCount > 0 ? `${optionsRetainedCount} retenue(s)` : null,
        anchor: 'section-pistes',
      },
      {
        id: 'checklist',
        label: 'Checklist',
        icon: 'checklist',
        iconClass: 'panel-icon--checklist',
        count: checksCount,
        anchor: 'section-checklist',
      },
      {
        id: 'questions',
        label: 'Questions complémentaires',
        icon: 'quiz',
        iconClass: 'panel-icon--questions',
        count: questionsCount,
        anchor: 'section-questions',
      },
      {
        id: 'pieces',
        label: 'Pièces manquantes',
        icon: 'report_problem',
        iconClass: 'panel-icon--manquantes',
        count: syn.piecesManquantes?.length ?? 0,
        anchor: 'section-pieces',
        popup: 'pieces',
      },
      {
        id: 'questions-ouvertes',
        label: 'Questions ouvertes',
        icon: 'help_outline',
        iconClass: 'panel-icon--questions',
        count: syn.questionsOuvertes?.length ?? 0,
        anchor: 'section-questions-ouvertes',
        popup: 'questions-ouvertes',
      },
    ];
    const filtered = all.filter(b => b.count > 0);
    // F-197 SF-197-02 — préfixer le badge "Type de litige" en tête de grille
    // (saillant : c'est l'action la plus impactante de F-197 — change la
    // visibilité des outils + le pre-fill).
    return typeLitigeBadge ? [typeLitigeBadge, ...filtered] : filtered;
  });

  /**
   * F-162 SF-162-01 — scroll fluide vers un bloc + ouverture si nécessaire.
   * Les panels Material sont rendus `expanded` par défaut dans le template, donc
   * un simple `scrollIntoView` suffit. Le retry est conservé par symétrie avec
   * SF-IA-03-19 (données async pour `procedureChecks` / `questions`).
   */
  scrollToBlock(anchorId: string, attempt = 0): void {
    const el = document.getElementById(anchorId);
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'start' });
      el.classList.add('source-highlight');
      setTimeout(() => el.classList.remove('source-highlight'), 2100);
    } else if (attempt < 5) {
      setTimeout(() => this.scrollToBlock(anchorId, attempt + 1), 200);
    }
  }

  /**
   * F-197 SF-197-02 — Récupère l'override courant. Fail-open silencieux :
   * en cas d'erreur (timeout, 5xx), l'override reste à `null` et le badge
   * "Type de litige" affiche la valeur IA brute (CA-09).
   */
  private loadTypeLitigeOverride(caseFileId: string): void {
    this.typeLitigeOverrideService.getForCaseFile(caseFileId).subscribe({
      next: response => this.typeLitigeOverride.set(response ?? null),
      error: () => this.typeLitigeOverride.set(null),
    });
  }

  /**
   * F-197 SF-197-02 — Code du type de litige actuellement retenu (override
   * avocat si présent, sinon valeur IA détectée). Utilisé pour le badge
   * grille F-162 et passé en `iaDetectedCode` au dialog override.
   */
  readonly currentTypeLitigeCode = computed<string | null>(() => {
    const override = this.typeLitigeOverride();
    if (override) {
      const code = override.typeLitigeAvocat ?? override.typeProcedureAvocat ?? null;
      if (code) return code;
    }
    return this.synthesis()?.typeLitigeDetecte ?? null;
  });

  /**
   * F-197 SF-197-02 — `true` si un override avocat est posé (badge or, icône
   * `edit_note` "modifié par vous").
   */
  readonly typeLitigeIsOverridden = computed<boolean>(() => {
    const o = this.typeLitigeOverride();
    return !!o && (!!o.typeLitigeAvocat || !!o.typeProcedureAvocat);
  });

  /**
   * F-197 SF-197-02 — Libellé FR avocat-friendly du code retenu (lookup dans
   * les 2 maps Travail FR / Immigration). Si le code n'existe pas dans les
   * libellés (rétro-compat ou code IA exotique), retourne le code tel quel.
   */
  readonly currentTypeLitigeLabel = computed<string | null>(() => {
    const code = this.currentTypeLitigeCode();
    if (!code) return null;
    const t = TYPE_LITIGE_TRAVAIL_FR_LABELS as Record<string, string>;
    if (t[code]) return t[code];
    const i = TYPE_PROCEDURE_IMMIGRATION_LABELS as Record<string, string>;
    if (i[code]) return i[code];
    return code;
  });

  /**
   * F-197 SF-197-02 — Domaine cible pour le dialog override (TRAVAIL_FR ou
   * IMMIGRATION) résolu depuis {@link CaseFile#legalDomain}. Null si le
   * domaine ne supporte pas l'override (V1 : famille exclu).
   */
  readonly typeLitigeOverrideDomain = computed<TypeLitigeOverrideDomain | null>(() => {
    return resolveOverrideDomain(this.caseFile()?.legalDomain);
  });

  /**
   * F-197 SF-197-02 — Ouvre le dialog override. Au close avec une réponse
   * non-undefined, met à jour le signal local {@link #typeLitigeOverride}.
   * Cohérence F-176 stricte : aucun re-fetch synthèse, aucun refresh outils.
   */
  openTypeLitigeOverrideDialog(): void {
    const cfId = this.caseFile()?.id;
    const domain = this.typeLitigeOverrideDomain();
    if (!cfId || !domain) return;
    const data: TypeLitigeOverrideDialogData = {
      caseFileId: cfId,
      domain,
      current: this.typeLitigeOverride(),
      iaDetectedCode: this.synthesis()?.typeLitigeDetecte ?? null,
    };
    const ref = this.dialog.open<
      TypeLitigeOverrideDialogComponent,
      TypeLitigeOverrideDialogData,
      TypeLitigeOverrideResponse
    >(TypeLitigeOverrideDialogComponent, { data, width: '520px', maxWidth: '92vw' });
    ref.afterClosed().subscribe(result => {
      if (result === undefined) return;
      this.typeLitigeOverride.set(result);
      this.cdr.markForCheck();
    });
  }

  /**
   * F-229 SF-229-01 — résout l'identifiant d'un badge F-162 vers la clé
   * {@link BadgeKey} correspondante dans le service de navigation partagé.
   * Renvoie `null` pour les badges qui ne sont pas exposés au service
   * (`type-litige` — dialog override hors scope F-229 ; `checklist` — anchor
   * scroll local sans tile dashboard équivalente ; `questions-ouvertes` —
   * popup couvert par {@link openPopup} mais pas de tile dashboard).
   */
  private mapBadgeIdToKey(badgeId: string): BadgeKey | null {
    switch (badgeId) {
      case 'pistes':
      case 'pieces':
      case 'risques':
      case 'questions':
      case 'timeline':
      case 'faits':
      case 'points-juridiques':
        return badgeId;
      default:
        return null;
    }
  }

  /**
   * F-229 SF-229-01 — handler unifié de clic sur un badge F-162. Délègue à
   * {@link BadgeNavigationService} pour les clés exposées (assure l'alignement
   * strict avec les tiles "résumé" du dashboard décisionnel). Fallback sur
   * les handlers locaux pour `type-litige` (dialog override F-197),
   * `checklist` (anchor scroll local) et `questions-ouvertes` (popup local —
   * pas de tile dashboard équivalente, on garde {@link openPopup}).
   */
  onBadgeClick(badge: SynthesisBadge): void {
    if (badge.dialog === 'type-litige-override') {
      this.openTypeLitigeOverrideDialog();
      return;
    }
    const key = this.mapBadgeIdToKey(badge.id);
    const cfId = this.caseFile()?.id ?? null;
    if (key && cfId) {
      const ctx = this.buildBadgeContext(badge);
      this.badgeNavigation.go(key, cfId, ctx);
      return;
    }
    if (badge.popup) {
      this.openPopup(badge.popup);
      return;
    }
    this.scrollToBlock(badge.anchor);
  }

  /**
   * F-229 SF-229-01 — construit les données contextuelles pour les
   * destinations popup. Pour `pieces` : items = `synthesis().piecesManquantes`.
   * Pour les autres destinations, retourne `undefined` (ignoré par le service).
   */
  private buildBadgeContext(badge: SynthesisBadge) {
    if (badge.id === 'pieces') {
      return {
        popupItems: this.synthesis()?.piecesManquantes ?? [],
        popupTitle: 'Pièces manquantes',
        popupIcon: 'report_problem',
      };
    }
    return undefined;
  }

  /** F-162 SF-162-06 — ouvre un dialog modal pour un bloc court. */
  openPopup(kind: 'pieces' | 'questions-ouvertes'): void {
    const syn = this.synthesis();
    if (!syn) return;
    const config: Record<string, SynthesisShortBlockDialogData> = {
      'pieces': {
        title: 'Pièces manquantes',
        icon: 'report_problem',
        items: syn.piecesManquantes ?? [],
      },
      'questions-ouvertes': {
        title: 'Questions ouvertes',
        icon: 'help_outline',
        items: syn.questionsOuvertes ?? [],
      },
    };
    this.dialog.open(SynthesisShortBlockDialogComponent, {
      data: config[kind],
      width: '520px',
      maxWidth: '92vw',
    });
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
    private strategicOptionService: StrategicOptionService,
    private timeService: TimeService,
    private dialog: MatDialog,
    private retainedPisteAlignmentService: RetainedPisteAlignmentService,
    private procedureCheckAlignmentService: ProcedureCheckAlignmentService,
    private pieceManquanteStatusService: PieceManquanteStatusService,
    private pieceManquanteAlignmentService: PieceManquanteAlignmentService,
    private risqueStatusService: RisqueStatusService,
    private risqueAlignmentService: RisqueAlignmentService,
    private aiQuestionAlignmentService: AiQuestionAlignmentService,
    private typeLitigeOverrideService: TypeLitigeOverrideService,
    private badgeNavigation: BadgeNavigationService,
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    this.caseFileService.getById(id).subscribe({
      next: cf => {
        this.caseFile.set(cf);
        this.loadVersions(id);
        this.loadChatHistory(id);
        // F-197 SF-197-02 — fail-open silencieux (CA-09) : si le GET échoue,
        // on garde l'override à null et le badge "Type de litige" affiche
        // simplement la valeur IA détectée.
        this.loadTypeLitigeOverride(id);
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

    // F-229 SF-229-01 — CA-04 : scroll automatique vers le bloc cible quand
    // l'URL contient un fragment (ex. /case-files/:id/synthesis#section-pistes
    // navigué depuis une tile dashboard via {@link BadgeNavigationService}).
    // Le scrollToBlock existant a un retry 5×200 ms qui couvre le cas où la
    // synthesis() n'est pas encore signalée au moment de l'émission du
    // fragment. À chaque émission non-vide on relance le scroll — utile si
    // le user clique deux fois la même tile (force le rescroll).
    this.route.fragment?.subscribe(fragment => {
      if (fragment) {
        this.scrollToBlock(fragment);
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

  ngOnDestroy(): void {
    this.sseSubscription?.unsubscribe();
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
          this.currentChecksVersion.set(versions[0].version);
          this.currentQuestionsVersion.set(versions[0].version);
          this.loadSynthesisForVersion(caseFileId, versions[0].version);
          this.loadQuestionsForVersion(caseFileId, versions[0].id);
          this.loadChecksForVersion(caseFileId, versions[0].id);
          this.loadStrategicOptionsForVersion(caseFileId, versions[0].id);
          // F-185 SF-185-01 — si une nouvelle analyse a démarré (re-analyse), on tombera
          // également sur l'endpoint partial via l'événement SSE PARTIAL ; pas de pré-chargement
          // ici car la version DONE existante reste affichée tant que la nouvelle n'a pas terminé.
          this.subscribeToPartialEvents(caseFileId);
        } else {
          // F-185 SF-185-01 — pas encore de version DONE : essayer de charger l'état partiel
          // d'une analyse en cours pour afficher progressivement les sections au fil de l'eau.
          this.tryLoadPartial(caseFileId);
          this.subscribeToPartialEvents(caseFileId);
        }
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  /**
   * F-185 SF-185-01 — récupère l'état partiel courant et l'affiche en synthèse.
   * 404 = pas d'analyse en cours, on bascule sur l'écran "lance une analyse" standard.
   */
  private tryLoadPartial(caseFileId: string): void {
    this.caseAnalysisService.getPartial(caseFileId).subscribe({
      next: partial => {
        this.applyPartial(partial);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      }
    });
  }

  private subscribeToPartialEvents(caseFileId: string): void {
    this.sseSubscription?.unsubscribe();
    this.globalNotificationService.track(caseFileId);
    this.sseSubscription = this.globalNotificationService.events$.subscribe(event => {
      if (event.caseFileId !== caseFileId) return;
      // F-190 SF-190-02 — CASE_ANALYSIS (1ère synthèse) et ENRICHED_ANALYSIS (re-analyse)
      // empruntent désormais le streaming SSE via le même endpoint partial.
      if (event.jobType === 'CASE_ANALYSIS' || event.jobType === 'ENRICHED_ANALYSIS') {
        if (event.status === 'PARTIAL') {
          this.caseAnalysisService.getPartial(caseFileId).subscribe({
            next: partial => this.applyPartial(partial),
            error: () => {}
          });
        } else if (event.status === 'DONE') {
          this.isStreaming.set(false);
          this.loadVersions(caseFileId);
        } else if (event.status === 'FAILED') {
          this.isStreaming.set(false);
        }
      } else if (event.jobType === 'QUESTION_GENERATION' && event.status === 'DONE') {
        // F-185 SF-185-02 — questions Q&A async prêtes : recharger le panneau questions.
        const currentVersion = this.versions()[0];
        if (currentVersion) {
          this.loadQuestionsForVersion(caseFileId, currentVersion.id);
        }
      }
    });
  }

  /**
   * F-185 SF-185-01 — projette les sections JSON snake_case (telles que produites
   * par Sonnet) sur la forme camelCase de {@link CaseAnalysisResult} pour réutiliser
   * tel quel le template existant. Sections absentes = arrays vides ou null.
   */
  private applyPartial(partial: CaseAnalysisPartialResponse): void {
    this.isStreaming.set(true);
    // F-190 SF-190-01 — conserve le partial brut pour calculer la progression
    // granulaire (le projeté `synthesis()` met `[]` partout via `?? []`, ce qui
    // empêche de distinguer "section pas encore arrivée" d'une "section vide").
    this.lastPartial.set(partial);
    const s: Partial<CaseAnalysisPartialSections> = partial.sections ?? {};
    const result: CaseAnalysisResult = {
      id: partial.analysisId,
      version: partial.version,
      // F-190 SF-190-02 — l'analyse en cours peut être STANDARD ou ENRICHED ;
      // le backend nous indique son type, défaut STANDARD pour rétrocompat.
      analysisType: partial.analysisType ?? 'STANDARD',
      status: partial.status,
      timeline: (s.timeline as any) ?? [],
      faits: (s.faits as any) ?? [],
      pointsJuridiques: (s.points_juridiques as any) ?? [],
      risques: (s.risques as any) ?? [],
      questionsOuvertes: (s.questions_ouvertes as any) ?? [],
      piecesManquantes: (s.pieces_manquantes as any) ?? [],
      riskLevel: (s.risk_level as any) ?? null,
      riskScore: (s.risk_score as any) ?? null,
      modelUsed: null,
      updatedAt: partial.updatedAt,
      analysisDocuments: [],
      piecesManquantesDetails: (s.pieces_manquantes_details as any) ?? null,
    };
    this.synthesis.set(result);
  }

  /**
   * F-190 SF-190-01 — sections déjà reçues du stream, dans l'ordre canonique
   * (pas l'ordre d'arrivée — l'avocat veut un repère stable). Une section est
   * considérée reçue si la clé est présente dans `partial.sections` ET que la
   * valeur est non-null/non-undefined (les arrays vides comptent : Sonnet a
   * fini de générer la section et confirmé qu'il n'y a rien).
   */
  readonly streamingSectionsReceived = computed<readonly StreamingSection[]>(() => {
    const partial = this.lastPartial();
    if (!partial?.sections) return [];
    const sections = partial.sections;
    return STREAMING_EXPECTED_SECTIONS.filter(desc => sections[desc.id] != null);
  });

  /**
   * F-190 SF-190-01 — progression numérique (pour la `<mat-progress-bar>`).
   * `expected` est constant (7 sections), `received` est dérivé du dernier
   * partial reçu, `percent` est arrondi à l'entier.
   */
  readonly streamingProgress = computed(() => {
    const received = this.streamingSectionsReceived().length;
    const expected = STREAMING_EXPECTED_SECTIONS.length;
    const percent = expected === 0 ? 0 : Math.round((received / expected) * 100);
    return { received, expected, percent };
  });

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

  // F-160 SF-160-02 — paginators par bloc.
  readonly versionsAsc = computed(() =>
    [...this.versions()].sort((a, b) => a.version - b.version)
  );

  readonly checksIterationIndex = computed(() => {
    const v = this.currentChecksVersion();
    if (v == null) return 0;
    const i = this.versionsAsc().findIndex(x => x.version === v);
    return i < 0 ? 0 : i;
  });

  readonly questionsIterationIndex = computed(() => {
    const v = this.currentQuestionsVersion();
    if (v == null) return 0;
    const i = this.versionsAsc().findIndex(x => x.version === v);
    return i < 0 ? 0 : i;
  });

  readonly canChecksPrev = computed(() => this.checksIterationIndex() > 0);
  readonly canChecksNext = computed(() =>
    this.checksIterationIndex() < this.versionsAsc().length - 1
  );
  readonly canQuestionsPrev = computed(() => this.questionsIterationIndex() > 0);
  readonly canQuestionsNext = computed(() =>
    this.questionsIterationIndex() < this.versionsAsc().length - 1
  );

  navigateChecks(direction: -1 | 1): void {
    const list = this.versionsAsc();
    if (list.length < 2) return;
    const next = this.checksIterationIndex() + direction;
    if (next < 0 || next >= list.length) return;
    const target = list[next];
    const caseFileId = this.caseFile()?.id;
    if (!caseFileId) return;
    this.currentChecksVersion.set(target.version);
    this.loadChecksForVersion(caseFileId, target.id);
  }

  navigateQuestions(direction: -1 | 1): void {
    const list = this.versionsAsc();
    if (list.length < 2) return;
    const next = this.questionsIterationIndex() + direction;
    if (next < 0 || next >= list.length) return;
    const target = list[next];
    const caseFileId = this.caseFile()?.id;
    if (!caseFileId) return;
    this.currentQuestionsVersion.set(target.version);
    this.loadQuestionsForVersion(caseFileId, target.id);
  }

  loadStrategicOptionsForVersion(caseFileId: string, analysisId: string): void {
    this.strategicOptionService.list(caseFileId, analysisId).subscribe({
      next: options => this.strategicOptions.set(options),
      error: () => this.strategicOptions.set([])
    });
  }

  updateOptionStatus(option: StrategicOption, statut: StrategicOptionStatus): void {
    if (this.updatingOptionId() === option.id) return;
    if (statut === 'DISCARDED') {
      this.openDiscardDialog(option);
      return;
    }
    this.applyOptionStatus(option.id, statut, undefined);
  }

  editDiscardReason(option: StrategicOption): void {
    this.openDiscardDialog(option);
  }

  private openDiscardDialog(option: StrategicOption): void {
    const data: DiscardReasonDialogData = {
      initialReason: option.raisonDiscard,
      pisteTexte: option.texte
    };
    const ref = this.dialog.open(DiscardReasonDialogComponent, { data, width: '480px' });
    ref.afterClosed().subscribe(result => {
      if (result === undefined) return;
      this.applyOptionStatus(option.id, 'DISCARDED', result);
    });
  }

  private applyOptionStatus(
    optionId: string,
    statut: StrategicOptionStatus,
    raisonDiscard: string | null | undefined
  ): void {
    this.updatingOptionId.set(optionId);
    this.strategicOptionService.updateStatus(optionId, statut, raisonDiscard).subscribe({
      next: updated => {
        this.strategicOptions.update(list =>
          list.map(o => o.id === updated.id ? updated : o)
        );
        this.updatingOptionId.set(null);
      },
      error: () => {
        this.updatingOptionId.set(null);
        this.snackBar.open('Erreur lors de la mise à jour de la piste', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }

  optionStatusLabel(statut: StrategicOptionStatus): string {
    switch (statut) {
      case 'RETAINED': return 'Retenue';
      case 'DISCARDED': return 'Écartée';
      default: return 'À étudier';
    }
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

  /**
   * F-194 SF-194-02 — Récupère le statut courant d'une pièce manquante.
   * Tant que l'avocat n'a pas encore tagué : statut implicite `A_DEMANDER`.
   * Cohérent avec la sémantique backend (l'absence d'entrée DB = pièce à
   * demander).
   */
  pieceStatusFor(libelle: string): PieceManquanteStatutValue {
    return this.pieceStatuses()[libelle] ?? 'A_DEMANDER';
  }

  /** F-194 SF-194-02 — bouton actif si statut courant correspond. */
  isPieceStatusActive(libelle: string, statut: PieceManquanteStatutValue): boolean {
    return this.pieceStatusFor(libelle) === statut;
  }

  pieceRaisonFor(libelle: string): string {
    return this.pieceRaisons()[libelle] ?? '';
  }

  pieceDestinataireFor(libelle: string): string {
    return this.pieceDestinataires()[libelle] ?? '';
  }

  /**
   * F-194 SF-194-02 — Tag d'une pièce avec un nouveau statut. PUT optimiste :
   * UI mise à jour immédiatement, rollback si erreur. AUCUN refresh
   * (pas de `triggerRefresh`, pas de `loadAlignment`) — cohérence F-176 stricte.
   * La matérialisation pièce → outil arrive seule au prochain run Synthèse
   * enrichie (via SSE `ENRICHED_ANALYSIS DONE`).
   */
  updatePieceStatus(libelle: string, statut: PieceManquanteStatutValue): void {
    if (this.updatingPieceLibelle() === libelle) return;
    const cf = this.caseFile();
    if (!cf) return;
    const previous = this.pieceStatuses()[libelle];
    // Optimistic update.
    this.pieceStatuses.update(map => ({ ...map, [libelle]: statut }));
    this.updatingPieceLibelle.set(libelle);
    this.cdr.markForCheck();

    const raisonNonApp = statut === 'NON_APPLICABLE'
      ? (this.pieceRaisons()[libelle] ?? null) || null
      : null;
    const destinataire = statut === 'A_DEMANDER'
      ? (this.pieceDestinataires()[libelle] ?? null) || null
      : null;

    this.pieceManquanteStatusService
      .updateStatus(cf.id, libelle, statut, { raisonNonApp, destinataire })
      .subscribe({
        next: response => {
          this.updatingPieceLibelle.set(null);
          // Aligne le cache local sur la réponse autoritative backend (raison /
          // destinataire éventuellement normalisés).
          if (response?.raisonNonApp != null) {
            this.pieceRaisons.update(m => ({ ...m, [libelle]: response.raisonNonApp ?? '' }));
          }
          if (response?.destinataire != null) {
            this.pieceDestinataires.update(m => ({ ...m, [libelle]: response.destinataire ?? '' }));
          }
          this.cdr.markForCheck();
        },
        error: () => {
          this.updatingPieceLibelle.set(null);
          // Rollback statut.
          this.pieceStatuses.update(map => {
            const next = { ...map };
            if (previous !== undefined) {
              next[libelle] = previous;
            } else {
              delete next[libelle];
            }
            return next;
          });
          this.cdr.markForCheck();
          this.snackBar.open(
            'Erreur lors de la mise à jour de la pièce',
            'Fermer',
            { duration: 4000, panelClass: ['snack-error'] },
          );
        },
      });
  }

  /**
   * F-194 SF-194-02 — Capture la saisie raison NON_APPLICABLE (input local).
   * Le PUT est déclenché à blur via `onPieceRaisonBlur` — évite un PUT à
   * chaque caractère.
   */
  onPieceRaisonInput(libelle: string, value: string): void {
    this.pieceRaisons.update(m => ({ ...m, [libelle]: value }));
  }

  onPieceRaisonBlur(libelle: string): void {
    if (this.pieceStatusFor(libelle) !== 'NON_APPLICABLE') return;
    // Re-déclenche un PUT avec la raison saisie (no-op côté UI : statut déjà
    // affiché, on ne change que le payload côté backend).
    this.updatePieceStatus(libelle, 'NON_APPLICABLE');
  }

  /**
   * F-194 SF-194-02 — Saisie destinataire (MatSelect). Déclenche un PUT
   * immédiat car la sélection est ponctuelle (pas de blur à attendre).
   */
  onPieceDestinataireChange(libelle: string, value: string): void {
    this.pieceDestinataires.update(m => ({ ...m, [libelle]: value }));
    if (this.pieceStatusFor(libelle) !== 'A_DEMANDER') return;
    this.updatePieceStatus(libelle, 'A_DEMANDER');
  }

  /**
   * F-195 SF-195-02 — Récupère le statut courant d'un risque (clé = libellé
   * original). Tant que l'avocat n'a pas tagué : statut implicite
   * `A_CREUSER`. Cohérent avec la sémantique backend (l'absence d'entrée DB
   * = risque à creuser).
   */
  risqueStatusFor(libelle: string): RisqueStatutValue {
    return this.risqueStatuses()[libelle] ?? 'A_CREUSER';
  }

  /** F-195 SF-195-02 — bouton actif si statut courant correspond. */
  isRisqueStatusActive(libelle: string, statut: RisqueStatutValue): boolean {
    return this.risqueStatusFor(libelle) === statut;
  }

  risqueRaisonFor(libelle: string): string {
    return this.risqueRaisons()[libelle] ?? '';
  }

  /**
   * F-195 SF-195-02 — Tag d'un risque avec un nouveau statut. PUT optimiste :
   * UI mise à jour immédiatement, rollback si erreur. AUCUN refresh
   * (pas de `triggerRefresh`, pas de `loadAlignment`) — cohérence F-176
   * stricte. La matérialisation risque → outil arrive seule au prochain run
   * Synthèse enrichie (via SSE `ENRICHED_ANALYSIS DONE`).
   */
  updateRisqueStatus(libelle: string, statut: RisqueStatutValue): void {
    if (this.updatingRisqueLibelle() === libelle) return;
    const cf = this.caseFile();
    if (!cf) return;
    const previous = this.risqueStatuses()[libelle];
    // Optimistic update.
    this.risqueStatuses.update(map => ({ ...map, [libelle]: statut }));
    this.updatingRisqueLibelle.set(libelle);
    this.cdr.markForCheck();

    const raisonEcarte = statut === 'ECARTE'
      ? (this.risqueRaisons()[libelle] ?? null) || null
      : null;

    this.risqueStatusService
      .updateStatus(cf.id, libelle, statut, { raisonEcarte })
      .subscribe({
        next: response => {
          this.updatingRisqueLibelle.set(null);
          if (response?.raisonEcarte != null) {
            this.risqueRaisons.update(m => ({ ...m, [libelle]: response.raisonEcarte ?? '' }));
          }
          this.cdr.markForCheck();
        },
        error: () => {
          this.updatingRisqueLibelle.set(null);
          // Rollback statut.
          this.risqueStatuses.update(map => {
            const next = { ...map };
            if (previous !== undefined) {
              next[libelle] = previous;
            } else {
              delete next[libelle];
            }
            return next;
          });
          this.cdr.markForCheck();
          this.snackBar.open(
            'Erreur lors de la mise à jour du risque',
            'Fermer',
            { duration: 4000, panelClass: ['snack-error'] },
          );
        },
      });
  }

  /**
   * F-195 SF-195-02 — Capture la saisie raison ECARTE (input local). Le PUT
   * est déclenché à blur via `onRisqueRaisonBlur` — évite un PUT à chaque
   * caractère.
   */
  onRisqueRaisonInput(libelle: string, value: string): void {
    this.risqueRaisons.update(m => ({ ...m, [libelle]: value }));
  }

  onRisqueRaisonBlur(libelle: string): void {
    if (this.risqueStatusFor(libelle) !== 'ECARTE') return;
    // Re-déclenche un PUT avec la raison saisie.
    this.updateRisqueStatus(libelle, 'ECARTE');
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
    this.strategicOptions.set([]);
    this.editingQuestionId.set(null);
    // F-160 SF-160-02 — resync paginators sur la version globale.
    this.currentChecksVersion.set(selected.version);
    this.currentQuestionsVersion.set(selected.version);
    this.loadSynthesisForVersion(caseFileId, selected.version);
    this.loadQuestionsForVersion(caseFileId, selected.id);
    this.loadChecksForVersion(caseFileId, selected.id);
    this.loadStrategicOptionsForVersion(caseFileId, selected.id);
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

  /** F-150 : événements déclencheurs immigration détectés. Liste vide hors immigration. */
  get immigrationTriggerEvents(): ImmigrationTriggerEvent[] {
    return this.synthesis()?.immigrationTriggerEvents ?? [];
  }

  /** F-151 : scenarii stratégiques immigration comparés. Liste vide si aucun choix stratégique ouvert. */
  get immigrationStrategyScenarios(): ImmigrationStrategyScenario[] {
    return this.synthesis()?.immigrationStrategyScenarios ?? [];
  }

  /** F-152 : détection validité divorce consentement mutuel. Null hors famille. */
  get divorceConsentementValidityDetection(): DivorceConsentementValidityDetection | null {
    return this.synthesis()?.divorceConsentementValidityDetection ?? null;
  }

  /** F-152 : scoring divorce consentement mutuel. Null si détection absente. */
  get divorceConsentementScoring(): DivorceConsentementScoring | null {
    return this.synthesis()?.divorceConsentementScoring ?? null;
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
        // SF-171-02 : 402 (CHAT_MESSAGE_LIMIT_EXCEEDED / TOKEN_BUDGET_EXCEEDED) géré par paymentRequiredInterceptor + QuotaErrorState.
        if (err.status === 402) {
          // no-op
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
        // SF-171-02 : 402 géré par paymentRequiredInterceptor + QuotaErrorState (bandeau persistant).
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
    // F-192 SF-192-03 — pistes 🟢 Retenue + alignement outil cible.
    // F-193 SF-193-03 — checks F-96 + alignement outil cible.
    // F-194 SF-194-03 — pièces manquantes markables + alignement outil cible.
    // F-195 SF-195-03 — risques markables + alignement outil cible.
    // F-196 SF-196-03 — questions complémentaires + déduction pièces.
    // Les 5 services sont chargés EN PARALLÈLE via forkJoin. Fail-open
    // INDÉPENDANT : si l'un échoue, les autres sont utilisés quand même
    // (catchError local par stream). L'export PDF n'est jamais bloqué.
    forkJoin({
      retainedPistes: this.retainedPisteAlignmentService.getForCaseFile(cf.id).pipe(
        catchError(() => of([] as RetainedPisteAlignment[])),
      ),
      procedureChecksAlignment: this.procedureCheckAlignmentService.getForCaseFile(cf.id).pipe(
        catchError(() => of([] as ProcedureCheckAlignment[])),
      ),
      piecesAlignment: this.pieceManquanteAlignmentService.getForCaseFile(cf.id).pipe(
        catchError(() => of([] as PieceManquanteAlignment[])),
      ),
      risquesAlignment: this.risqueAlignmentService.getForCaseFile(cf.id).pipe(
        catchError(() => of([] as RisqueAlignment[])),
      ),
      aiQuestionsAlignment: this.aiQuestionAlignmentService.getForCaseFile(cf.id).pipe(
        catchError(() => of([] as AiQuestionAlignment[])),
      ),
    }).subscribe({
      next: ({ retainedPistes, procedureChecksAlignment, piecesAlignment, risquesAlignment, aiQuestionsAlignment }) =>
        this.runPdfExport(cf, syn, retainedPistes, procedureChecksAlignment, piecesAlignment, risquesAlignment, aiQuestionsAlignment),
      error: () => this.runPdfExport(cf, syn, [], [], [], [], []),
    });
  }

  private runPdfExport(
    cf: CaseFile,
    syn: CaseAnalysisResult,
    retainedPistes: RetainedPisteAlignment[],
    procedureChecksAlignment: ProcedureCheckAlignment[],
    piecesAlignment: PieceManquanteAlignment[],
    risquesAlignment: RisqueAlignment[],
    aiQuestionsAlignment: AiQuestionAlignment[],
  ): void {
    try {
      this.pdfExportService.export(
        cf,
        syn,
        retainedPistes,
        (toolId) => this.resolveToolLabel(toolId),
        procedureChecksAlignment,
        piecesAlignment,
        risquesAlignment,
        aiQuestionsAlignment,
      );
      this.analyticsService.trackEvent('pdf_exported');
    } catch {
      this.snackBar.open('Erreur lors de la génération du PDF', 'Fermer', {
        duration: 4000, panelClass: ['snack-error']
      });
    }
  }

  /**
   * F-192 SF-192-03 — résolution `toolId → TOOL_LABEL` via TOOL_REGISTRY du
   * panel F-IA-04. Retourne `null` si le toolId n'est pas connu (defensive).
   */
  private resolveToolLabel(toolId: string): string | null {
    if (!toolId) return null;
    const entry = DecisionToolsPanelComponent.TOOL_REGISTRY.get(toolId);
    if (!entry) return null;
    const meta = getToolMetadata(entry.component);
    return meta?.label ?? null;
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
        // SF-171-02 : 402 géré par paymentRequiredInterceptor + QuotaErrorState (bandeau persistant).
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
