import { Component, DestroyRef, Input, OnInit, Optional, ViewContainerRef, inject, signal, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { debounceTime } from 'rxjs';
import { CaseDashboardService } from '../../core/services/case-dashboard.service';
import { DashboardResponse, DashboardTile as BackendDashboardTile } from '../../core/models/case-dashboard.model';
import { CaseDashboardRefreshService } from './case-dashboard-refresh.service';
import { DashboardTileComponent } from './dashboard-tile/dashboard-tile.component';
import { DecisionToolCardComponent } from '../decisional-tools-panel/decision-tool-card/decision-tool-card.component';
import { DecisionToolModalService } from '../decisional-tools-panel/decision-tool-modal/decision-tool-modal.service';
import { DecisionToolsPanelComponent, ThemeKey } from '../decisional-tools-panel/decisional-tools-panel.component';
import { getToolMetadata } from '../decisional-tools-panel/decision-tool.contract';
import { DecisionToolSummary, MetierAlertLevel } from '../decisional-tools-panel/decision-tool-summary.model';

/**
 * F-167 SF-167-05 — Tile riskScore (non cliquable). Conservée à part car
 * elle ne correspond à aucun outil décisionnel : le score IA global est rendu
 * en tête du tableau de bord, hors thèmes.
 */
export interface RiskScoreTile {
  toolId: 'risk-score';
  title: string;
  icon: string;
  summary: DecisionToolSummary;
  metierAlertLevel: MetierAlertLevel;
}

/**
 * F-167 SF-167-05 — Section thématique du tableau de bord (un thème = une
 * section, ex. "Indemnités & calculs"). Les tiles à l'intérieur sont triées
 * par {@code alertLevel} décroissant (ALERT > WARNING > OK > null).
 */
export interface DashboardThemeSection {
  key: ThemeKey;
  label: string;
  tiles: BackendDashboardTile[];
}

/**
 * F-167 SF-167-05 — Tableau de bord décisionnel agrégé.
 *
 * <p>Pipeline de rendu :
 * <ol>
 *   <li>Tile {@code riskScore} (rendue via {@code <app-decision-tool-card>},
 *   non cliquable, hors thèmes).</li>
 *   <li>Liste {@link BackendDashboardTile} groupée par thème (ordre fixe
 *   {@code DecisionToolsPanelComponent.THEMES_ORDERED}) ; tri intra-thème par
 *   {@code alertLevel} décroissant.</li>
 *   <li>État vide : message centré "Aucun outil exécuté pour ce dossier".</li>
 * </ol></p>
 */
@Component({
  selector: 'app-case-dashboard',
  standalone: true,
  imports: [MatIconModule, MatProgressSpinnerModule, DecisionToolCardComponent, DashboardTileComponent],
  templateUrl: './case-dashboard.component.html',
  styleUrl: './case-dashboard.component.scss'
})
export class CaseDashboardComponent implements OnInit {
  @Input() caseFileId!: string;
  // F-177 SF-177-09 : inputs supplémentaires pour passer aux composants outils
  // ouverts dans le modal (alignés sur le panel F-IA-04 SF-177-11).
  @Input() synthesis: any | null = null;
  @Input() workspaceCountry = 'FRANCE';
  @Input() procedureChecks: any[] = [];
  @Input() aiQuestions: any[] = [];

  /**
   * F-167 SF-167-05 — ordre fixe des sections thématiques dans le dashboard.
   * Réutilise l'ordre canonique de {@code DecisionToolsPanelComponent.THEMES_ORDERED}
   * (F-169 SF-169-01) pour cohérence visuelle avec le panneau outils.
   */
  static readonly THEME_SECTIONS = DecisionToolsPanelComponent.THEMES_ORDERED;

  /**
   * F-167 SF-167-05 — ordre de tri des alertLevel à l'intérieur d'un thème.
   * ALERT en premier, puis WARNING, OK, null/undefined.
   */
  private static readonly ALERT_LEVEL_ORDER: Record<string, number> = {
    ALERT: 0,
    WARNING: 1,
    OK: 2,
  };

  loading = signal(false);
  dashboard = signal<DashboardResponse | null>(null);

  /**
   * F-167 SF-167-05 — riskScore tile rendue isolément (non groupée par thème).
   */
  readonly riskScoreTile = computed<RiskScoreTile | null>(() => {
    const d = this.dashboard();
    if (!d || d.riskScore == null) return null;
    return {
      toolId: 'risk-score',
      title: 'ÉVALUATION DES RISQUES',
      icon: 'speed',
      summary: {
        label: 'Score',
        primaryValue: `${d.riskScore} %`,
        secondaryValue: d.riskLevel ?? undefined,
      },
      metierAlertLevel: this.riskAlertLevel(d.riskScore),
    };
  });

  /**
   * F-167 SF-167-05 — sections thématiques (5 thèmes max). Ordre fixe ;
   * thèmes vides exclus du rendu. Tri intra-thème par alertLevel décroissant.
   */
  readonly themeSections = computed<DashboardThemeSection[]>(() => {
    const d = this.dashboard();
    const tiles = d?.tiles ?? [];
    if (tiles.length === 0) return [];
    return CaseDashboardComponent.THEME_SECTIONS
      .map(({ key, label }) => ({
        key,
        label,
        tiles: this.sortByAlertLevel(tiles.filter((t) => t.theme === key)),
      }))
      .filter((section) => section.tiles.length > 0);
  });

  /**
   * F-167 SF-167-05 — vrai si aucune donnée à afficher (ni riskScore, ni
   * tiles génériques). Déclenche l'état vide ("Aucun outil exécuté").
   */
  readonly isEmpty = computed<boolean>(() => {
    const d = this.dashboard();
    if (!d) return false; // pas encore chargé : pas d'état vide affiché
    return d.riskScore == null && (!d.tiles || d.tiles.length === 0);
  });

  /**
   * F-184 SF-184-01 — Compte total de verdicts disponibles, exposé au parent
   * (`case-file-detail`) via template ref pour alimenter le count badge du
   * wrapper `.decisional-summary-panel`. riskScore + tiles génériques.
   */
  readonly verdictsCount = computed<number>(() => {
    const d = this.dashboard();
    if (!d) return 0;
    const tilesCount = d.tiles?.length ?? 0;
    return (d.riskScore != null ? 1 : 0) + tilesCount;
  });

  private readonly destroyRef = inject(DestroyRef);
  private readonly modalService = inject(DecisionToolModalService);
  // SF-177-14 — propagé au modal pour que les outils héritent de l'injector
  // tree de case-file-detail (CaseDashboardRefreshService notamment).
  private readonly vcr = inject(ViewContainerRef);

  constructor(
    private dashboardService: CaseDashboardService,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    this.reload(true);
    if (this.refreshService) {
      this.refreshService.refresh$
        .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
        .subscribe(() => this.reload(false));
    }
  }

  /**
   * F-167 SF-167-01 / SF-167-05 — Ouvre le modal du composant outil
   * correspondant à une tile générique. Résout le composant via
   * TOOL_REGISTRY (même logique que `<app-decisional-tools-panel>`). Si le
   * toolId est inconnu : no-op + console.warn (cohérent avec resolveEntry).
   */
  openGenericTool(toolId: string): void {
    const entry = DecisionToolsPanelComponent.TOOL_REGISTRY.get(toolId);
    if (!entry) {
      // eslint-disable-next-line no-console
      console.warn(`[case-dashboard] Unknown toolId for generic tile: ${toolId}`);
      return;
    }
    const meta = getToolMetadata(entry.component);
    const inputs = entry.inputs({
      caseFileId: this.caseFileId,
      synthesis: this.synthesis,
      workspaceCountry: this.workspaceCountry,
      caseFileTitle: '',
      procedureChecks: this.procedureChecks,
      aiQuestions: this.aiQuestions,
    });
    this.modalService.open({
      toolId,
      title: meta?.label ?? toolId,
      icon: meta?.icon ?? 'extension',
      component: entry.component,
      inputs: { ...inputs, forceExpanded: true },
      viewContainerRef: this.vcr,
    });
  }

  /**
   * F-167 SF-167-05 — tri par alertLevel décroissant (ALERT > WARNING > OK >
   * null). Tri stable (préserve l'ordre backend pour les tiles de même
   * niveau).
   */
  private sortByAlertLevel(tiles: BackendDashboardTile[]): BackendDashboardTile[] {
    return [...tiles].sort((a, b) => {
      const ra = a.alertLevel ? CaseDashboardComponent.ALERT_LEVEL_ORDER[a.alertLevel] ?? 3 : 3;
      const rb = b.alertLevel ? CaseDashboardComponent.ALERT_LEVEL_ORDER[b.alertLevel] ?? 3 : 3;
      return ra - rb;
    });
  }

  private riskAlertLevel(score: number): MetierAlertLevel {
    if (score < 30) return 'OK';
    if (score < 60) return 'WARNING';
    return 'ALERT';
  }

  private reload(showSpinner: boolean): void {
    if (showSpinner) this.loading.set(true);
    this.dashboardService.get(this.caseFileId).subscribe({
      next: d => { this.dashboard.set(d); this.loading.set(false); },
      error: () => { this.loading.set(false); },
    });
  }
}
