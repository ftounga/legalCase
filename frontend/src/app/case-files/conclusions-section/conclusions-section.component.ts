import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output,
  ViewChild,
  computed,
  inject,
  signal,
} from '@angular/core';
import { forkJoin } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import {
  ConclusionDocumentComponent,
  PieceRef,
} from '../conclusion-document/conclusion-document.component';
import { DocumentService } from '../../core/services/document.service';
import { documentPieceTypeLabel } from '../../core/models/document.model';
import {
  MarkdownAction,
  MarkdownToolbarComponent,
} from '../../shared/markdown-toolbar/markdown-toolbar.component';
import { ConclusionsService } from '../../core/services/conclusions.service';
import { CaseFileService } from '../../core/services/case-file.service';
import { CaseDashboardService } from '../../core/services/case-dashboard.service';
import { JurisprudenceCheckService } from '../../core/services/jurisprudence-check.service';
import { DocxExportService } from '../../core/services/docx-export.service';
import { PdfExportService } from '../../core/services/pdf-export.service';
import {
  ConclusionLifecycleStatus,
  ConclusionResponse,
  ConclusionStatus,
  ConclusionVersionSummary,
} from '../../core/models/conclusion.model';

/**
 * Intervalle de polling de l'état des conclusions (ms).
 * Mini-spec SF-98-01 : « polling GET …/conclusions toutes les 3 s ».
 */
const POLL_INTERVAL_MS = 3000;

/** Libellés FR des états du cycle de vie d'une version (SF-98-52). */
const LIFECYCLE_LABELS: Record<ConclusionLifecycleStatus, string> = {
  DRAFT: 'Brouillon',
  VALIDATED: 'Validé',
  DEPOSITED: 'Déposé',
};

/** Ordre des états proposés dans le contrôle de cycle de vie. */
const LIFECYCLE_ORDER: readonly ConclusionLifecycleStatus[] = [
  'DRAFT',
  'VALIDATED',
  'DEPOSITED',
];

/**
 * F-98 / SF-98-01 + SF-98-52 — Section « Conclusions » du détail dossier.
 *
 * Placée dans l'onglet Décision, juste après le tableau de bord décisionnel.
 * Permet à l'avocat de générer un projet de conclusions juridiques à partir
 * de la synthèse, du stade procédural, des outils décisionnels et des pistes
 * stratégiques du dossier.
 *
 * SF-98-52 : chaque génération crée une nouvelle version ; l'avocat dispose
 * d'un sélecteur de version, d'un badge de cycle de vie (brouillon / validé /
 * déposé) et d'un contrôle pour faire évoluer ce cycle de vie.
 *
 * Ce n'est PAS un outil décisionnel : pas de `TOOL_REGISTRY`, pas de panel
 * F-IA-04, pas de gate F-IA-03. C'est un générateur de document.
 *
 * Standalone, OnPush + signals. L'état étant muté dans des `subscribe()`, un
 * `ChangeDetectorRef.markForCheck()` est appelé dans chaque `next` ET `error`
 * (cf. mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-conclusions-section',
  standalone: true,
  imports: [
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    RouterLink,
    ConclusionDocumentComponent,
    MarkdownToolbarComponent,
  ],
  templateUrl: './conclusions-section.component.html',
  styleUrl: './conclusions-section.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ConclusionsSectionComponent implements OnInit, OnDestroy {
  /** Dossier dont on génère les conclusions (obligatoire). */
  @Input() caseFileId!: string;

  /**
   * Vrai si le stade procédural du dossier est entièrement renseigné.
   * Optionnel : si `undefined`, on laisse le backend (`409 STAGE_NOT_SET`)
   * piloter le message guidant.
   */
  @Input() procedureStageComplete?: boolean;

  /**
   * Vrai si le dossier a au moins une analyse terminée.
   * Optionnel : si `undefined`, on laisse le backend
   * (`409 ANALYSIS_NOT_READY`) piloter le message guidant.
   */
  @Input() hasCompletedAnalysis?: boolean;

  /**
   * SF-98-50 — Titre du dossier, utilisé pour nommer le fichier `.docx`
   * exporté (`{slug}-conclusions-v{N}.docx`). Optionnel : si absent, le
   * service retombe sur `conclusions-v{N}.docx`.
   */
  @Input() caseTitle?: string;

  /**
   * F-258 / SF-258-01 — Demande de focalisation du panneau d'outils
   * décisionnels. Émis quand l'avocat clique « Voir les outils à calculer »
   * dans l'encart d'avertissement. Le parent (`case-file-detail`) fait défiler
   * la page vers `#section-outils-decisionnels`.
   */
  @Output() viewToolsRequested = new EventEmitter<void>();

  /** Contenu de la version actuellement affichée. */
  readonly conclusion = signal<ConclusionResponse | null>(null);
  /** Historique des versions du dossier (tri version décroissante). */
  readonly versions = signal<ConclusionVersionSummary[]>([]);
  /** Id de la version sélectionnée dans le sélecteur (null si aucune). */
  readonly selectedVersionId = signal<string | null>(null);
  /** Chargement initial (GET au montage). */
  readonly loading = signal(true);
  /** Vrai si le GET initial a échoué — section indisponible. */
  readonly unavailable = signal(false);
  /** Déclenchement de génération en cours (POST). */
  readonly generating = signal(false);
  /** Copie du texte dans le presse-papier en cours. */
  readonly copying = signal(false);
  /** Changement de cycle de vie en cours (PATCH). */
  readonly updatingLifecycle = signal(false);
  /** SF-98-49 — Vrai quand l'avocat est en mode édition du texte. */
  readonly editing = signal(false);
  /** SF-98-49 — Texte en cours d'édition (lié au `textarea`). */
  readonly draftContent = signal('');
  /** SF-98-49 — Enregistrement du texte édité en cours (PATCH). */
  readonly savingContent = signal(false);

  /** F-265 / SF-265-02 — Régénération d'une section en cours (POST). */
  readonly regenerating = signal(false);
  /** F-265 / SF-265-02 — Titre de la section sélectionnée pour la régénération. */
  readonly selectedSectionStart = signal<number | null>(null);
  /** F-265 / SF-265-02 — Instruction libre de l'avocat (« renforce la prescription »). */
  readonly regenInstruction = signal('');

  /**
   * F-265 / SF-265-02 — Sections détectées dans le brouillon courant, par
   * parsing **déterministe** des titres markdown (`##`/`###`). Chaque section
   * couvre `[début du titre, début du titre suivant de niveau ≤[`. Aucune
   * requête réseau. Recalculée à chaque changement de `draftContent`.
   */
  readonly sections = computed<ConclusionSection[]>(() =>
    parseMarkdownSections(this.draftContent()),
  );

  /** F-265 / SF-265-02 — Section actuellement sélectionnée (ou `null`). */
  readonly selectedSection = computed<ConclusionSection | null>(() => {
    const start = this.selectedSectionStart();
    if (start === null) {
      return null;
    }
    return this.sections().find((s) => s.start === start) ?? null;
  });

  /** F-265 / SF-265-02 — Vrai si la régénération peut être déclenchée. */
  readonly canRegenerate = computed<boolean>(
    () =>
      !this.regenerating() &&
      !this.savingContent() &&
      this.selectedSection() !== null &&
      this.regenInstruction().trim().length > 0,
  );

  /**
   * SF-264-01 — Vue active du mode édition sur écran étroit : `edit`
   * (éditeur + barre d'outils) ou `preview` (aperçu « acte » formaté). Sur
   * large écran les deux colonnes sont affichées côte à côte (CSS) et ce
   * signal est sans effet visuel. Réinitialisé à `edit` à chaque entrée en
   * édition.
   */
  readonly editorView = signal<'edit' | 'preview'>('edit');

  /**
   * SF-264-01 — Référence au `<textarea>` d'édition markdown. Utilisée par la
   * barre d'outils pour insérer les marqueurs à la position du curseur / sur la
   * sélection. Absente tant que le mode édition n'est pas rendu.
   */
  @ViewChild('editor') editorRef?: ElementRef<HTMLTextAreaElement>;

  /**
   * F-258 / SF-258-01 — Nombre d'outils décisionnels **proposés** (pré-remplis)
   * mais **non encore calculés** pour ce dossier. Calculé au montage à partir de
   * la visibilité (`alwaysOn + contextual`) et du tableau de bord (`tiles`).
   * `0` ⇒ aucun encart (cas par défaut, et en cas d'échec de l'un des appels).
   */
  readonly missingToolsCount = signal(0);

  /**
   * F-266 / SF-266-01 — Pièces numérotées du dossier (numéro persistant F-260),
   * passées à l'aperçu « acte » pour la traçabilité fait → pièce au survol.
   * Chargées au montage depuis `DocumentService.list`. En cas d'échec → liste
   * vide (aucune décoration, dégradation propre).
   */
  readonly pieceRefs = signal<PieceRef[]>([]);

  /**
   * F-98 / SF-98-56 — Nombre de citations adverses marquées **et** réfutables
   * (statut SUSPECT / NOT_FOUND ET `markedAdverse = true`) du dossier. Calculé
   * au montage à partir des jurisprudence-checks. `0` ⇒ aucune mention affichée
   * (pas de rubrique vide). En cas d'échec du GET, on retombe à `0`.
   */
  readonly adverseMarkedCount = signal(0);

  /** Libellés des états de cycle de vie, exposés au template. */
  readonly lifecycleLabels = LIFECYCLE_LABELS;
  /** Ordre des états de cycle de vie, exposé au template. */
  readonly lifecycleOptions = LIFECYCLE_ORDER;

  private readonly conclusionsService = inject(ConclusionsService);
  private readonly caseFileService = inject(CaseFileService);
  private readonly caseDashboardService = inject(CaseDashboardService);
  private readonly documentService = inject(DocumentService);
  private readonly jurisprudenceCheckService = inject(JurisprudenceCheckService);
  private readonly docxExportService = inject(DocxExportService);
  private readonly pdfExportService = inject(PdfExportService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly cdr = inject(ChangeDetectorRef);

  /** Handle du polling actif (null = aucun polling en cours). */
  private pollHandle: ReturnType<typeof setInterval> | null = null;

  /** Statut courant, par défaut `NOT_GENERATED` tant que rien n'est chargé. */
  readonly status = computed<ConclusionStatus>(
    () => this.conclusion()?.status ?? 'NOT_GENERATED',
  );

  /** Cycle de vie de la version affichée (null tant qu'aucune version). */
  readonly lifecycleStatus = computed<ConclusionLifecycleStatus | null>(
    () => this.conclusion()?.lifecycleStatus ?? null,
  );

  /** Vrai s'il existe au moins une version générée pour le dossier. */
  readonly hasVersions = computed<boolean>(() => this.versions().length > 0);

  /**
   * SF-98-48 (ajustement b2) — Vrai si la version affichée a été générée en
   * appliquant le corpus de style du cabinet. `styleApplied` est un champ
   * optionnel fourni par SF-98-47 : absent ou `false` ⇒ pas d'indicateur
   * (dégradation propre tant que SF-98-47 n'est pas déployée).
   */
  readonly styleApplied = computed<boolean>(
    () => this.conclusion()?.styleApplied === true,
  );

  /**
   * SF-98-53 — Vrai si la version affichée est potentiellement périmée :
   * l'analyse du dossier a évolué depuis sa génération. `stale` est calculé
   * à la lecture par le backend ; absent ou `false` ⇒ rien à signaler
   * (dégradation propre tant que SF-98-53 backend n'est pas déployé). Le
   * bandeau d'avertissement n'est rendu que dans le cas `DONE` (cf. template).
   */
  readonly stale = computed<boolean>(() => this.conclusion()?.stale === true);

  /**
   * SF-98-49 — Vrai si la version affichée est éditable : génération `DONE`
   * ET cycle de vie `DRAFT`. Une version `VALIDATED`/`DEPOSITED` est figée.
   */
  readonly editable = computed<boolean>(() => {
    const c = this.conclusion();
    return !!c && c.status === 'DONE' && c.lifecycleStatus === 'DRAFT';
  });

  /** Vrai si tous les pré-requis fonctionnels connus sont satisfaits. */
  readonly prerequisitesMet = computed<boolean>(
    () =>
      this.procedureStageComplete !== false &&
      this.hasCompletedAnalysis !== false,
  );

  /**
   * Message guidant à afficher quand un pré-requis manque.
   * Le stade procédural est cité en premier (onglet Dossier).
   */
  readonly missingPrerequisiteMessage = computed<string | null>(() => {
    if (this.procedureStageComplete === false) {
      return 'Renseignez le stade procédural du dossier (onglet Dossier) avant de générer les conclusions.';
    }
    if (this.hasCompletedAnalysis === false) {
      return 'Lancez et terminez l\'analyse du dossier avant de générer les conclusions.';
    }
    return null;
  });

  ngOnInit(): void {
    this.refreshMissingTools();
    this.refreshAdverseMarkedCount();
    this.refreshPieceRefs();
    this.conclusionsService.getConclusion(this.caseFileId).subscribe({
      next: (res) => {
        this.conclusion.set(res);
        this.selectedVersionId.set(res.id);
        this.loading.set(false);
        if (res.status === 'PENDING' || res.status === 'PROCESSING') {
          this.startPolling();
        }
        this.cdr.markForCheck();
        this.refreshVersions();
      },
      error: () => {
        this.loading.set(false);
        this.unavailable.set(true);
        this.snackBar.open(
          'Conclusions indisponibles — impossible de charger l\'état du dossier.',
          'Fermer',
          { duration: 4000, panelClass: ['snack-error'] },
        );
        this.cdr.markForCheck();
      },
    });
  }

  ngOnDestroy(): void {
    this.stopPolling();
  }

  /**
   * Déclenche (ou relance) la génération du projet de conclusions.
   * SF-98-52 : crée une nouvelle version. Sur succès `202`, démarre le
   * polling et sélectionne la nouvelle version.
   */
  generate(): void {
    if (this.generating()) {
      return;
    }
    this.generating.set(true);
    this.conclusionsService.generate(this.caseFileId).subscribe({
      next: (res) => {
        this.generating.set(false);
        // Reflète immédiatement l'état PENDING de la nouvelle version en
        // attendant le 1er poll. `id` est inconnu : on cible la dernière
        // version via getConclusion (polling) qui renvoie le max.
        this.conclusion.set({
          id: null,
          caseFileId: this.caseFileId,
          status: 'PENDING',
          versionNumber: res.versionNumber,
          lifecycleStatus: 'DRAFT',
          content: null,
          jurisdictionLabel: null,
          stageLabel: null,
          positionLabel: null,
          modelUsed: null,
          generatedAt: null,
          errorMessage: null,
          createdAt: null,
          updatedAt: null,
        });
        // La nouvelle version devient la version suivie par défaut.
        this.selectedVersionId.set(null);
        this.startPolling();
        this.cdr.markForCheck();
        this.refreshVersions();
      },
      error: (err) => {
        this.generating.set(false);
        const msg =
          err?.error?.message ||
          'Impossible de lancer la génération des conclusions.';
        this.snackBar.open(msg, 'Fermer', {
          duration: 6000,
          panelClass: ['snack-error'],
        });
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * SF-98-52 — Change la version affichée.
   * Recharge le contenu via `getVersion`. Pendant un polling actif
   * (génération en cours), le changement de version est ignoré pour ne pas
   * détourner le suivi de la version en cours de génération.
   */
  selectVersion(versionId: string): void {
    if (versionId === this.selectedVersionId() || this.pollHandle !== null) {
      return;
    }
    // Changer de version sort du mode édition sans appel serveur.
    this.editing.set(false);
    this.selectedVersionId.set(versionId);
    this.conclusionsService.getVersion(this.caseFileId, versionId).subscribe({
      next: (res) => {
        this.conclusion.set(res);
        this.cdr.markForCheck();
      },
      error: () => {
        this.snackBar.open(
          'Impossible de charger cette version des conclusions.',
          'Fermer',
          { duration: 4000, panelClass: ['snack-error'] },
        );
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * SF-98-52 — Fait évoluer le cycle de vie de la version affichée.
   * Sur `409`/`400`, affiche le message backend via la snackbar.
   */
  changeLifecycle(lifecycleStatus: ConclusionLifecycleStatus): void {
    const current = this.conclusion();
    if (
      !current?.id ||
      this.updatingLifecycle() ||
      lifecycleStatus === current.lifecycleStatus
    ) {
      return;
    }
    const versionId = current.id;
    this.updatingLifecycle.set(true);
    this.conclusionsService
      .updateLifecycle(this.caseFileId, versionId, lifecycleStatus)
      .subscribe({
        next: (res) => {
          this.updatingLifecycle.set(false);
          // Sortir d'un brouillon (validation/dépôt) ferme le mode édition.
          this.editing.set(false);
          this.conclusion.set(res);
          this.cdr.markForCheck();
          this.refreshVersions();
        },
        error: (err) => {
          this.updatingLifecycle.set(false);
          const msg =
            err?.error?.message ||
            'Impossible de modifier le cycle de vie de cette version.';
          this.snackBar.open(msg, 'Fermer', {
            duration: 6000,
            panelClass: ['snack-error'],
          });
          this.cdr.markForCheck();
        },
      });
  }

  /** Copie le texte des conclusions dans le presse-papier. */
  copy(): void {
    const content = this.conclusion()?.content;
    if (!content || this.copying()) {
      return;
    }
    this.copying.set(true);
    navigator.clipboard.writeText(content).then(
      () => {
        this.copying.set(false);
        this.snackBar.open(
          'Projet de conclusions copié dans le presse-papier.',
          'Fermer',
          { duration: 3000, panelClass: ['snack-success'] },
        );
        this.cdr.markForCheck();
      },
      () => {
        this.copying.set(false);
        this.snackBar.open(
          'Impossible de copier le texte dans le presse-papier.',
          'Fermer',
          { duration: 4000, panelClass: ['snack-error'] },
        );
        this.cdr.markForCheck();
      },
    );
  }

  /**
   * SF-98-50 — Télécharge la version affichée au format Word `.docx`.
   *
   * Disponible uniquement quand la version est `DONE` (donc avec un `content`).
   * Délègue la construction du document et le déclenchement du téléchargement
   * au `DocxExportService` (réutilise le pattern client-side F-95). En cas
   * d'échec de génération, le service affiche lui-même une `MatSnackBar`
   * d'erreur ; une erreur synchrone inattendue est captée ici par sécurité.
   */
  downloadWord(): void {
    const current = this.conclusion();
    if (current?.status !== 'DONE' || !current.content) {
      return;
    }
    try {
      this.docxExportService.exportConclusion(
        current.content,
        this.caseTitle ?? '',
        current.versionNumber ?? 0,
      );
    } catch {
      this.snackBar.open(
        'Erreur lors de la génération du document Word.',
        'Fermer',
        { duration: 4000, panelClass: ['snack-error'] },
      );
    }
  }

  /**
   * SF-98-51 — Télécharge la version affichée au format PDF.
   *
   * Disponible uniquement quand la version est `DONE` (donc avec un `content`).
   * Délègue la construction du document et le déclenchement du téléchargement
   * au `PdfExportService` (réutilise le pattern client-side `pdfmake`). En cas
   * d'échec de génération, le service affiche lui-même une `MatSnackBar`
   * d'erreur ; une erreur synchrone inattendue est captée ici par sécurité.
   */
  downloadPdf(): void {
    const current = this.conclusion();
    if (current?.status !== 'DONE' || !current.content) {
      return;
    }
    try {
      this.pdfExportService.exportConclusion(
        current.content,
        this.caseTitle ?? '',
        current.versionNumber ?? 0,
      );
    } catch {
      this.snackBar.open(
        'Erreur lors de la génération du document PDF.',
        'Fermer',
        { duration: 4000, panelClass: ['snack-error'] },
      );
    }
  }

  /**
   * SF-98-49 — Passe la version affichée en mode édition.
   * Pré-remplit le brouillon avec le texte courant. Ignoré si la version
   * n'est pas éditable (`DONE` + `DRAFT`).
   */
  startEditing(): void {
    if (!this.editable() || this.editing()) {
      return;
    }
    this.draftContent.set(this.conclusion()?.content ?? '');
    this.editorView.set('edit');
    this.editing.set(true);
    // F-265 — réinitialise l'état de co-rédaction à chaque entrée en édition.
    this.selectedSectionStart.set(null);
    this.regenInstruction.set('');
  }

  /** SF-264-01 — Bascule la vue éditeur/aperçu sur écran étroit. */
  setEditorView(view: 'edit' | 'preview'): void {
    this.editorView.set(view);
  }

  /**
   * SF-264-01 — Applique une action de la barre d'outils markdown sur le
   * `<textarea>` d'édition, à la position du curseur ou sur la sélection.
   *
   * - `bold` / `italic` : **enveloppent** la sélection (`**…**` / `*…*`). Sans
   *   sélection, insèrent les marqueurs vides et replacent le curseur entre eux.
   * - `h2` / `h3` / `list` / `quote` : **préfixent** chaque ligne de la
   *   sélection (ou la ligne courante) par le marqueur (`## `, `### `, `- `,
   *   `> `).
   *
   * Le `content` reste du markdown pur (round-trip garanti). On met à jour le
   * signal `draftContent` puis on restaure le focus et la sélection sur le
   * textarea pour enchaîner les saisies.
   */
  applyMarkdown(action: MarkdownAction): void {
    if (this.savingContent()) {
      return;
    }
    const textarea = this.editorRef?.nativeElement;
    const value = this.draftContent();
    // Position de sélection : on retombe sur la fin du texte si le textarea
    // n'est pas disponible (ex. vue aperçu active sur étroit).
    const start = textarea?.selectionStart ?? value.length;
    const end = textarea?.selectionEnd ?? value.length;

    const result =
      action === 'bold' || action === 'italic'
        ? this.wrapSelection(value, start, end, action === 'bold' ? '**' : '*')
        : this.prefixLines(value, start, end, this.linePrefix(action));

    this.draftContent.set(result.value);

    // Restaure le focus et la sélection après le re-rendu (textarea recréé via
    // le binding `[value]`). Pas de mutation hors zone Angular ⇒ markForCheck
    // implicite par le signal ; on force juste le focus/curseur.
    queueMicrotask(() => {
      const el = this.editorRef?.nativeElement;
      if (el) {
        el.focus();
        el.setSelectionRange(result.selectionStart, result.selectionEnd);
      }
    });
  }

  /** Préfixe de ligne associé à une action de bloc. */
  private linePrefix(action: MarkdownAction): string {
    switch (action) {
      case 'h2':
        return '## ';
      case 'h3':
        return '### ';
      case 'list':
        return '- ';
      case 'quote':
        return '> ';
      default:
        return '';
    }
  }

  /**
   * Enveloppe la sélection `[start, end[` de `value` par `marker` de part et
   * d'autre. Sans sélection, insère `marker``marker` et place le curseur au
   * milieu. Retourne le nouveau texte et la sélection à restaurer.
   */
  private wrapSelection(
    value: string,
    start: number,
    end: number,
    marker: string,
  ): { value: string; selectionStart: number; selectionEnd: number } {
    const before = value.slice(0, start);
    const selected = value.slice(start, end);
    const after = value.slice(end);
    const composed = `${before}${marker}${selected}${marker}${after}`;
    if (selected.length === 0) {
      const caret = start + marker.length;
      return { value: composed, selectionStart: caret, selectionEnd: caret };
    }
    return {
      value: composed,
      selectionStart: start + marker.length,
      selectionEnd: end + marker.length,
    };
  }

  /**
   * Préfixe par `prefix` chaque ligne touchée par la sélection `[start, end[`
   * (ou la ligne courante si la sélection est vide). Retourne le nouveau texte
   * et la sélection couvrant les lignes modifiées.
   */
  private prefixLines(
    value: string,
    start: number,
    end: number,
    prefix: string,
  ): { value: string; selectionStart: number; selectionEnd: number } {
    // Étend la sélection au début de la première ligne et à la fin de la
    // dernière ligne touchées.
    const lineStart = value.lastIndexOf('\n', start - 1) + 1;
    let lineEnd = value.indexOf('\n', end);
    if (lineEnd === -1) {
      lineEnd = value.length;
    }
    const before = value.slice(0, lineStart);
    const block = value.slice(lineStart, lineEnd);
    const after = value.slice(lineEnd);
    const prefixed = block
      .split('\n')
      .map((line) => `${prefix}${line}`)
      .join('\n');
    const composed = `${before}${prefixed}${after}`;
    return {
      value: composed,
      selectionStart: lineStart,
      selectionEnd: lineStart + prefixed.length,
    };
  }

  /**
   * SF-98-49 — Annule l'édition : ferme le mode édition et oublie le
   * brouillon. Aucun appel serveur — le texte affiché reste inchangé.
   */
  cancelEditing(): void {
    this.editing.set(false);
    this.draftContent.set('');
    this.selectedSectionStart.set(null);
    this.regenInstruction.set('');
  }

  /** SF-98-49 — Met à jour le brouillon depuis le `textarea`. */
  onDraftInput(value: string): void {
    this.draftContent.set(value);
  }

  /**
   * SF-98-49 — Enregistre le texte édité via `PATCH .../content`.
   * Sur succès, met à jour la version affichée, repasse en lecture et
   * rafraîchit l'historique des versions. Les erreurs `409`/`400` du backend
   * sont remontées via la snackbar.
   */
  saveContent(): void {
    const current = this.conclusion();
    if (!current?.id || this.savingContent()) {
      return;
    }
    const versionId = current.id;
    const content = this.draftContent();
    this.savingContent.set(true);
    this.conclusionsService
      .updateContent(this.caseFileId, versionId, content)
      .subscribe({
        next: (res) => {
          this.savingContent.set(false);
          this.editing.set(false);
          this.draftContent.set('');
          this.conclusion.set(res);
          this.cdr.markForCheck();
          this.refreshVersions();
        },
        error: (err) => {
          this.savingContent.set(false);
          const msg =
            err?.error?.message ||
            'Impossible d\'enregistrer les modifications.';
          this.snackBar.open(msg, 'Fermer', {
            duration: 6000,
            panelClass: ['snack-error'],
          });
          this.cdr.markForCheck();
        },
      });
  }

  /**
   * F-265 / SF-265-02 — Mémorise la section sélectionnée dans le menu déroulant.
   * La valeur transmise est l'index de début du bloc (clé stable et unique).
   */
  onSelectSection(start: string): void {
    const parsed = Number(start);
    this.selectedSectionStart.set(Number.isNaN(parsed) ? null : parsed);
  }

  /** F-265 / SF-265-02 — Met à jour l'instruction de co-rédaction. */
  onInstructionInput(value: string): void {
    this.regenInstruction.set(value);
  }

  /**
   * F-265 / SF-265-02 — Régénère la section sélectionnée via l'IA selon
   * l'instruction de l'avocat, puis **remplace en place** le bloc dans le
   * brouillon (round-trip markdown). Aucune sauvegarde automatique : l'avocat
   * relit puis enregistre via « Enregistrer ». Les erreurs sont remontées en
   * snackbar et le brouillon reste inchangé.
   */
  regenerateSection(): void {
    const current = this.conclusion();
    const section = this.selectedSection();
    const instruction = this.regenInstruction().trim();
    if (!current?.id || !section || instruction.length === 0 || this.regenerating()) {
      return;
    }
    const versionId = current.id;
    const sectionMarkdown = section.markdown;
    this.regenerating.set(true);
    this.conclusionsService
      .regenerateSection(this.caseFileId, versionId, sectionMarkdown, instruction)
      .subscribe({
        next: (res) => {
          this.regenerating.set(false);
          const replaced = replaceSectionInDraft(
            this.draftContent(),
            sectionMarkdown,
            res.regeneratedMarkdown,
          );
          if (replaced === null) {
            // La section n'est plus retrouvable (édition manuelle entre-temps).
            this.snackBar.open(
              'Section introuvable dans le brouillon — resélectionnez-la.',
              'Fermer',
              { duration: 6000, panelClass: ['snack-error'] },
            );
            this.cdr.markForCheck();
            return;
          }
          this.draftContent.set(replaced);
          this.selectedSectionStart.set(null);
          this.regenInstruction.set('');
          this.snackBar.open(
            'Section régénérée — relisez puis enregistrez.',
            'Fermer',
            { duration: 4000 },
          );
          this.cdr.markForCheck();
        },
        error: (err) => {
          this.regenerating.set(false);
          const msg =
            err?.error?.message ||
            'Impossible de régénérer la section. Réessayez.';
          this.snackBar.open(msg, 'Fermer', {
            duration: 6000,
            panelClass: ['snack-error'],
          });
          this.cdr.markForCheck();
        },
      });
  }

  /** Libellé FR d'un cycle de vie (utilitaire template). */
  lifecycleLabel(value: ConclusionLifecycleStatus | null): string {
    return value ? LIFECYCLE_LABELS[value] : '';
  }

  /**
   * F-258 / SF-258-01 — Recalcule le nombre d'outils proposés non calculés.
   *
   * `N = (alwaysOn + contextual) − {toolId des tiles}`. Le `catalog` (outils
   * non proposés sur ce dossier) n'est **jamais** compté — sinon faux positifs
   * massifs (invariant cadrage étape 0). En cas d'échec de l'un des deux appels,
   * on retombe à `0` (pas d'encart, pas de blocage) : on n'invente pas de N.
   */
  private refreshMissingTools(): void {
    forkJoin({
      visibility: this.caseFileService.getDecisionToolsVisibility(this.caseFileId),
      dashboard: this.caseDashboardService.get(this.caseFileId),
    }).subscribe({
      next: ({ visibility, dashboard }) => {
        const proposed = new Set([
          ...visibility.alwaysOn,
          ...visibility.contextual,
        ]);
        const calculated = new Set(
          (dashboard.tiles ?? []).map((tile) => tile.toolId),
        );
        let missing = 0;
        proposed.forEach((toolId) => {
          if (!calculated.has(toolId)) {
            missing += 1;
          }
        });
        this.missingToolsCount.set(missing);
        this.cdr.markForCheck();
      },
      error: () => {
        // Dégradation silencieuse : sans données fiables, pas d'encart.
        this.missingToolsCount.set(0);
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * F-266 / SF-266-01 — Charge les pièces numérotées du dossier et les met à
   * plat en {@link PieceRef} (numéro persistant F-260 + libellé + type lisible
   * anti-jargon). Alimente la traçabilité fait → pièce au survol de l'aperçu.
   * Une pièce sans `pieceNumber` (très anciennes, non backfillées) est ignorée
   * (pas de numéro fiable → pas de décoration). En cas d'échec → liste vide.
   */
  private refreshPieceRefs(): void {
    this.documentService.list(this.caseFileId).subscribe({
      next: (documents) => {
        const refs: PieceRef[] = [];
        for (const doc of documents) {
          for (const piece of doc.pieces ?? []) {
            if (piece.pieceNumber != null) {
              refs.push({
                number: piece.pieceNumber,
                label: piece.label,
                typeLabel: documentPieceTypeLabel(piece.type),
              });
            }
          }
        }
        this.pieceRefs.set(refs);
        this.cdr.markForCheck();
      },
      error: () => {
        // Dégradation propre : sans pièces, aucune décoration de survol.
        this.pieceRefs.set([]);
        this.cdr.markForCheck();
      },
    });
  }

  /**
   * F-98 / SF-98-56 — Recalcule le nombre de citations adverses marquées et
   * réfutables (statut SUSPECT/NOT_FOUND ET `markedAdverse = true`) du dossier.
   *
   * <p>Ce nombre alimente la mention factuelle affichée après une génération
   * réussie. En cas d'échec du GET, on retombe à `0` (aucune mention) : on
   * n'invente pas de N.</p>
   */
  private refreshAdverseMarkedCount(): void {
    this.jurisprudenceCheckService.getChecks(this.caseFileId).subscribe({
      next: (res) => {
        const count = (res.checks ?? []).filter(
          (c) =>
            c.markedAdverse === true &&
            (c.statut === 'SUSPECT' || c.statut === 'NOT_FOUND'),
        ).length;
        this.adverseMarkedCount.set(count);
        this.cdr.markForCheck();
      },
      error: () => {
        // Dégradation silencieuse : sans données fiables, pas de mention.
        this.adverseMarkedCount.set(0);
        this.cdr.markForCheck();
      },
    });
  }

  /** Recharge l'historique des versions (best-effort, sans bloquer l'UI). */
  private refreshVersions(): void {
    this.conclusionsService.listVersions(this.caseFileId).subscribe({
      next: (list) => {
        this.versions.set(list);
        // Tant qu'aucune version n'est explicitement sélectionnée, la plus
        // récente (max version_number, tête de liste) est suivie par défaut.
        if (this.selectedVersionId() === null && list.length > 0) {
          this.selectedVersionId.set(list[0].id);
        }
        this.cdr.markForCheck();
      },
      error: () => {
        // L'historique est secondaire : un échec ne rend pas la section
        // indisponible, on garde la version courante affichée.
        this.cdr.markForCheck();
      },
    });
  }

  /** Démarre le polling de l'état (idempotent). */
  private startPolling(): void {
    if (this.pollHandle !== null) {
      return;
    }
    this.pollHandle = setInterval(() => this.pollOnce(), POLL_INTERVAL_MS);
  }

  /** Arrête le polling s'il est actif. */
  private stopPolling(): void {
    if (this.pollHandle !== null) {
      clearInterval(this.pollHandle);
      this.pollHandle = null;
    }
  }

  /** Un tour de polling : recharge l'état, s'arrête sur DONE / FAILED. */
  private pollOnce(): void {
    this.conclusionsService.getConclusion(this.caseFileId).subscribe({
      next: (res) => {
        this.conclusion.set(res);
        if (res.status === 'DONE' || res.status === 'FAILED') {
          this.selectedVersionId.set(res.id);
          this.stopPolling();
          this.refreshVersions();
          // SF-98-56 — la génération vient d'aboutir : rafraîchit le nombre de
          // citations adverses marquées prises en compte (mention factuelle).
          this.refreshAdverseMarkedCount();
        }
        this.cdr.markForCheck();
      },
      error: () => {
        // On arrête le polling sur erreur réseau pour ne pas boucler ;
        // l'avocat peut recharger la page ou relancer.
        this.stopPolling();
        this.cdr.markForCheck();
      },
    });
  }
}

/**
 * F-265 / SF-265-02 — Section de l'acte détectée par parsing markdown.
 *
 * @property title    texte du titre (sans les `#`), pour le libellé du menu
 * @property start    index de début du bloc dans le `content` (clé stable)
 * @property markdown bloc markdown complet de la section (titre + corps,
 *                    jusqu'au titre suivant de niveau ≤ ou la fin du document)
 */
export interface ConclusionSection {
  title: string;
  start: number;
  markdown: string;
}

/**
 * F-265 / SF-265-02 — Découpe un markdown en sections délimitées par les titres
 * `##` / `###`. Déterministe, sans dépendance. Une section couvre du début de
 * son titre jusqu'au début du **titre suivant de niveau inférieur ou égal**
 * (un `###` enfant reste DANS la section `##` parente). Le préambule éventuel
 * avant le premier titre n'est pas une section régénérable (on cible les
 * moyens/parties titrés).
 */
export function parseMarkdownSections(content: string): ConclusionSection[] {
  if (!content) {
    return [];
  }
  const lines = content.split('\n');
  // Repère chaque titre : offset de début de ligne + niveau (2 ou 3).
  const headings: { start: number; level: number; title: string }[] = [];
  let offset = 0;
  for (const line of lines) {
    const match = /^(#{2,3})\s+(.*)$/.exec(line);
    if (match) {
      headings.push({
        start: offset,
        level: match[1].length,
        title: match[2].trim(),
      });
    }
    offset += line.length + 1; // +1 pour le '\n' retiré par split
  }
  const sections: ConclusionSection[] = [];
  for (let i = 0; i < headings.length; i++) {
    const h = headings[i];
    // Fin = début du prochain titre de niveau ≤ au courant, sinon fin du texte.
    let end = content.length;
    for (let j = i + 1; j < headings.length; j++) {
      if (headings[j].level <= h.level) {
        end = headings[j].start;
        break;
      }
    }
    sections.push({
      title: h.title,
      start: h.start,
      markdown: content.slice(h.start, end).replace(/\n+$/, ''),
    });
  }
  return sections;
}

/**
 * F-265 / SF-265-02 — Remplace **en place** le bloc `originalSection` par
 * `regenerated` dans `draft`. Round-trip markdown : seul ce bloc change, le
 * reste est byte-identique. Retourne `null` si le bloc original n'est pas
 * retrouvé tel quel (édition manuelle entre la sélection et la régénération) —
 * on n'effectue alors aucun remplacement hasardeux.
 */
export function replaceSectionInDraft(
  draft: string,
  originalSection: string,
  regenerated: string,
): string | null {
  const index = draft.indexOf(originalSection);
  if (index === -1) {
    return null;
  }
  return (
    draft.slice(0, index) +
    regenerated.replace(/\n+$/, '') +
    draft.slice(index + originalSection.length)
  );
}
