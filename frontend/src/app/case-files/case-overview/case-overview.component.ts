import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  computed,
  inject,
  signal,
} from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { DatePipe, NgTemplateOutlet } from '@angular/common';
import { OverviewService } from '../../core/services/overview.service';
import { DocumentService } from '../../core/services/document.service';
import { CaseFileService } from '../../core/services/case-file.service';
import {
  AttentionItem,
  OverviewAction,
  OverviewResponse,
  OverviewVoie,
  TimelineEvent,
} from '../../core/models/overview.model';

/**
 * F-289 / SF-289-01 — Onglet « Vue d'ensemble du dossier » (index 0, actif par
 * défaut), poste de pilotage / journal du dossier en 3 registres hiérarchisés :
 *   ① PILOTER     — bandeau d'état + bloc « ce qui requiert ton attention » (≤ 5).
 *   ② PARCOURIR   — fil vertical chronologique, repère Aujourd'hui, accordéons pièces.
 *   ③ APPROFONDIR — raccourcis vers les zones de travail + export.
 *
 * Lecture seule (un GET d'agrégation `fail-open`). Les actions « lourdes »
 * (générer, relancer, valider, répondre) ne s'exécutent pas ici : elles
 * **routent** vers l'onglet/ancre cible via `@Output() navigate`, consommé par
 * le parent `case-file-detail` (mécanisme `selectedTabIndex` + scroll d'ancre).
 *
 * `OnPush` : tout `subscribe()` rappelle `markForCheck()` (convention projet).
 */

/** Filtre de voie sélectionnable (chips). `ALL` = aucun filtre (défaut). */
export type VoieFilter = OverviewVoie | 'ALL';

/** Demande de routage émise vers le parent (onglet + ancre, ou route absolue). */
export interface OverviewNavigation {
  targetTab?: string | null;
  anchor?: string | null;
  route?: string | null;
}

/** Seuil de repli du passé : au-delà, on masque les plus anciens événements. */
export const OVERVIEW_PAST_COLLAPSE_THRESHOLD = 8;

/** Nombre maximum d'items d'attention affichés (le reste → « +N autres »). */
export const OVERVIEW_ATTENTION_MAX = 5;

@Component({
  selector: 'app-case-overview',
  standalone: true,
  imports: [
    DatePipe,
    NgTemplateOutlet,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
  ],
  templateUrl: './case-overview.component.html',
  styleUrl: './case-overview.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CaseOverviewComponent implements OnInit {
  @Input({ required: true }) caseFileId!: string;

  /** Routage inter-onglets demandé par une action (le parent exécute). */
  @Output() navigate = new EventEmitter<OverviewNavigation>();

  private readonly overviewService = inject(OverviewService);
  private readonly documentService = inject(DocumentService);
  private readonly caseFileService = inject(CaseFileService);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly snackHost = inject(CaseOverviewSnack, { optional: true });

  readonly overview = signal<OverviewResponse | null>(null);
  readonly loading = signal(true);
  readonly loadError = signal(false);

  /** Filtre de voie actif (chips). Défaut = ALL (aucun masquage implicite). */
  readonly voieFilter = signal<VoieFilter>('ALL');

  /** Repli du passé lointain ouvert/fermé (déplié au clic « historique »). */
  readonly pastExpanded = signal(false);

  /** Bloc attention déplié au-delà des 5 premiers items. */
  readonly attentionExpanded = signal(false);

  /** Ids des événements dont l'accordéon de pièces est ouvert. */
  private readonly expandedEvents = signal<Set<string>>(new Set());

  /** Constantes exposées au template. */
  readonly ATTENTION_MAX = OVERVIEW_ATTENTION_MAX;
  readonly PAST_THRESHOLD = OVERVIEW_PAST_COLLAPSE_THRESHOLD;

  /** Chips de filtre proposés (ordre de lecture). */
  readonly voieChips: { value: VoieFilter; label: string }[] = [
    { value: 'ALL', label: 'Tout' },
    { value: 'PROCEDURE', label: 'Procédure' },
    { value: 'ECHANGE', label: 'Échanges' },
    { value: 'PIECES', label: 'Pièces' },
    { value: 'PRODUCTION', label: 'Production' },
    { value: 'ECHEANCE', label: 'Échéance' },
  ];

  /** Dossier « neuf / vide » : pilotage minimal ET fil vide ET aucune attention. */
  readonly isEmpty = computed(() => {
    const ov = this.overview();
    if (!ov) return false;
    const p = ov.pilotage;
    const hasState =
      !!p.currentPhaseLabel ||
      !!p.nextDeadline ||
      p.pendingPiecesCount > 0 ||
      p.toolsCalculated > 0;
    return ov.fil.length === 0 && ov.attention.length === 0 && !hasState;
  });

  /** Items d'attention visibles (tronqués à 5 sauf si déplié). */
  readonly visibleAttention = computed<AttentionItem[]>(() => {
    const items = this.overview()?.attention ?? [];
    if (this.attentionExpanded()) return items;
    return items.slice(0, OVERVIEW_ATTENTION_MAX);
  });

  /** Nombre d'items d'attention masqués (« +N autres »). */
  readonly hiddenAttentionCount = computed(() => {
    const ov = this.overview();
    if (!ov) return 0;
    const total = ov.attentionTotal ?? ov.attention.length;
    return Math.max(0, total - OVERVIEW_ATTENTION_MAX);
  });

  /** Fil filtré par voie (sans toucher au tri ni au temps). */
  private readonly filteredFil = computed<TimelineEvent[]>(() => {
    const fil = this.overview()?.fil ?? [];
    const f = this.voieFilter();
    return f === 'ALL' ? fil : fil.filter(e => e.voie === f);
  });

  /** Événements passés (PASSE), triés par date asc tels que servis. */
  readonly pastEvents = computed<TimelineEvent[]>(() =>
    this.filteredFil().filter(e => e.temps === 'PASSE'),
  );

  /** Événements futurs (AUJOURDHUI + FUTUR), sous le repère doré. */
  readonly futureEvents = computed<TimelineEvent[]>(() =>
    this.filteredFil().filter(e => e.temps !== 'PASSE'),
  );

  /** Passé effectivement affiché (repli au-delà du seuil si non déplié). */
  readonly visiblePastEvents = computed<TimelineEvent[]>(() => {
    const past = this.pastEvents();
    if (this.pastExpanded() || past.length <= OVERVIEW_PAST_COLLAPSE_THRESHOLD) {
      return past;
    }
    // Garde les plus récents (fin de liste, tri asc) — masque les plus anciens.
    return past.slice(past.length - OVERVIEW_PAST_COLLAPSE_THRESHOLD);
  });

  /** Nombre d'événements passés masqués par le repli. */
  readonly hiddenPastCount = computed(() => {
    const past = this.pastEvents();
    if (this.pastExpanded()) return 0;
    return Math.max(0, past.length - OVERVIEW_PAST_COLLAPSE_THRESHOLD);
  });

  ngOnInit(): void {
    this.loadOverview();
  }

  loadOverview(): void {
    this.loading.set(true);
    this.loadError.set(false);
    this.overviewService.getOverview(this.caseFileId).subscribe({
      next: ov => {
        this.overview.set(ov);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loadError.set(true);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  // ── Filtres / repli ─────────────────────────────────────────────

  selectVoie(value: VoieFilter): void {
    this.voieFilter.set(value);
  }

  togglePast(): void {
    this.pastExpanded.set(!this.pastExpanded());
  }

  toggleAttention(): void {
    this.attentionExpanded.set(!this.attentionExpanded());
  }

  // ── Accordéon pièces ────────────────────────────────────────────

  /** Identifiant stable d'un événement (pas d'id serveur → date+titre). */
  eventKey(event: TimelineEvent): string {
    return `${event.date}|${event.voie}|${event.titre}`;
  }

  isEventExpanded(event: TimelineEvent): boolean {
    return this.expandedEvents().has(this.eventKey(event));
  }

  toggleEvent(event: TimelineEvent): void {
    if (!event.attachments?.length) return;
    const key = this.eventKey(event);
    const next = new Set(this.expandedEvents());
    if (next.has(key)) {
      next.delete(key);
    } else {
      next.add(key);
    }
    this.expandedEvents.set(next);
  }

  // ── Ouverture de pièces (DocumentService) ───────────────────────

  /** URL de téléchargement direct d'une pièce. */
  downloadHref(documentId: string): string {
    return this.documentService.downloadUrl(this.caseFileId, documentId);
  }

  /**
   * Aperçu d'une pièce : vérifie d'abord sa disponibilité via le payload
   * `/preview` (un document supprimé entre-temps renvoie une erreur → ligne
   * dégradée, pas d'onglet blanc), puis ouvre le flux `/download` que le
   * navigateur rend inline (PDF / image). Fail-open sur l'aperçu.
   */
  previewDocument(documentId: string): void {
    this.documentService.preview(this.caseFileId, documentId).subscribe({
      next: () => {
        window.open(this.downloadHref(documentId), '_blank', 'noopener');
        this.cdr.markForCheck();
      },
      error: () => {
        // Document supprimé / indisponible : on n'ouvre rien, on signale léger.
        this.snackHost?.error("Cette pièce n'est plus disponible.");
        this.cdr.markForCheck();
      },
    });
  }

  // ── Actions (routage vers l'écran dédié — V1 ne fait pas l'action inline) ──

  /** Libellé de bouton dérivé de la nature d'une action. */
  actionLabel(action: OverviewAction | null | undefined): string {
    switch (action?.kind) {
      case 'GENERATE_REPLY':
        return 'Générer ma réplique';
      case 'RELAUNCH_ANALYSIS':
        return "Relancer l'analyse";
      case 'VALIDATE_DEADLINE':
        return 'Marquer fait';
      case 'ANSWER_QUESTION':
        return 'Répondre';
      case 'MARK_PIECE':
        return 'Marquer obtenue';
      case 'OPEN_DOC':
        return 'Ouvrir';
      case 'NAVIGATE':
      default:
        return 'Voir';
    }
  }

  /** Icône de bouton dérivée de la nature d'une action. */
  actionIcon(action: OverviewAction | null | undefined): string {
    switch (action?.kind) {
      case 'GENERATE_REPLY':
        return 'edit_note';
      case 'RELAUNCH_ANALYSIS':
        return 'refresh';
      case 'VALIDATE_DEADLINE':
        return 'check_circle';
      case 'ANSWER_QUESTION':
        return 'question_answer';
      case 'MARK_PIECE':
        return 'inventory';
      case 'OPEN_DOC':
        return 'visibility';
      case 'NAVIGATE':
      default:
        return 'arrow_forward';
    }
  }

  /** Exécute une action : route vers l'onglet/ancre ou la route dédiée. */
  runAction(action: OverviewAction | null | undefined): void {
    if (!action) return;
    this.navigate.emit({
      targetTab: action.targetTab ?? null,
      anchor: action.anchor ?? null,
      route: action.route ?? null,
    });
  }

  // ── Raccourcis « approfondir » ──────────────────────────────────

  goToShortcut(targetTab: string, anchor: string | null = null): void {
    this.navigate.emit({ targetTab, anchor, route: null });
  }

  /** Exporte le dossier (ZIP) — réutilise le service existant. */
  exportZip(): void {
    this.caseFileService.exportZip(this.caseFileId).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'dossier.zip';
        a.click();
        URL.revokeObjectURL(url);
        this.cdr.markForCheck();
      },
      error: () => {
        this.snackHost?.error("Erreur lors de l'export du dossier.");
        this.cdr.markForCheck();
      },
    });
  }

  // ── Helpers de présentation ─────────────────────────────────────

  /** Classe de couleur d'urgence (driver visuel : navy / or / rouge). */
  urgencyClass(urgency: string | null | undefined): string {
    switch ((urgency ?? '').toUpperCase()) {
      case 'CRITICAL':
      case 'OVERDUE':
      case 'HIGH':
        return 'urg-high';
      case 'MEDIUM':
      case 'SOON':
        return 'urg-medium';
      default:
        return 'urg-low';
    }
  }

  /** Classe d'acteur pour la coloration des nœuds du fil (nous=navy / adverse=or). */
  acteurClass(event: TimelineEvent): string {
    if (event.acteur === 'OURS') return 'node--ours';
    if (event.acteur === 'ADVERSE') return 'node--adverse';
    return 'node--system';
  }

  /** Icône Material d'une voie. */
  voieIcon(voie: OverviewVoie): string {
    switch (voie) {
      case 'PROCEDURE':
        return 'gavel';
      case 'ECHANGE':
        return 'forum';
      case 'PIECES':
        return 'folder';
      case 'PRODUCTION':
        return 'auto_awesome';
      case 'ECHEANCE':
        return 'event';
      default:
        return 'circle';
    }
  }
}

/**
 * Hôte de notification optionnel — permet au parent (qui possède un MatSnackBar)
 * d'injecter un afficheur d'erreur léger sans coupler le composant au MatSnackBar
 * (qui n'est pas forcément fourni dans tous les contextes de test). Optionnel :
 * absent → l'erreur est silencieuse côté composant.
 */
export abstract class CaseOverviewSnack {
  abstract error(message: string): void;
}
