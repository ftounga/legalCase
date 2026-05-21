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
import { DecisionToolsPanelComponent, ThemeKey, DecisionToolContext } from '../decisional-tools-panel/decisional-tools-panel.component';
import { getToolMetadata } from '../decisional-tools-panel/decision-tool.contract';
import { DecisionToolSummary, MetierAlertLevel } from '../decisional-tools-panel/decision-tool-summary.model';
import { DecisionToolAlignmentsLoader } from '../decision-tools-shared/decision-tool-alignments.loader';
import { RetainedPisteAlignment } from '../../core/models/retained-piste-alignment.model';
import { PieceManquanteAlignment } from '../../core/models/piece-manquante-alignment.model';
import { RisqueAlignment } from '../../core/models/risque-alignment.model';
import { AiQuestionAlignment } from '../../core/models/ai-question-alignment.model';
import { BadgeNavigationService } from '../synthesis-badges/badge-navigation.service';

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
   * F-228 SF-228-01 — Cache local des 4 alignements IA ↔ outils consommés par
   * le ctx de `entry.inputs(ctx)` lors de l'ouverture d'un outil décisionnel
   * depuis le dashboard. Pattern miroir
   * `<app-decisional-tools-panel>` (F-IA-04). Refresh : (1) au mount du
   * dossier, (2) à chaque émission `CaseDashboardRefreshService.refresh$`
   * (déclenchée à la fin du run de Synthèse enrichie via les events SSE
   * F-185/F-190). Le PUT statut piste/pièce/risque + PUT réponse F-94 ne
   * déclenchent PAS de refresh — cohérence F-176 stricte.
   */
  readonly retainedPistes = signal<RetainedPisteAlignment[]>([]);
  readonly piecesAlignment = signal<PieceManquanteAlignment[]>([]);
  readonly risquesAlignment = signal<RisqueAlignment[]>([]);
  readonly aiQuestionsAlignment = signal<AiQuestionAlignment[]>([]);

  /**
   * F-167 SF-167-05 — riskScore tile rendue isolément (non groupée par thème).
   *
   * <p>F-195 SF-195-02 — Quand `scoreRisqueAvocat` est présent (l'avocat a
   * écarté au moins un risque), on affiche les 2 valeurs côte à côte :
   * <code>Score IA brut : X · Score validé avocat : Y</code>. La couleur de
   * la bordure suit le score validé avocat (qui prime visuellement — c'est
   * la décision finale). Sinon comportement original (1 score).</p>
   */
  readonly riskScoreTile = computed<RiskScoreTile | null>(() => {
    const d = this.dashboard();
    if (!d || d.riskScore == null) return null;
    const scoreAvocat = d.scoreRisqueAvocat ?? null;
    const hasDualScore = scoreAvocat != null;
    const primary = hasDualScore
      ? `IA brut : ${d.riskScore} % · Validé avocat : ${scoreAvocat} %`
      : `${d.riskScore} %`;
    // Quand le score avocat existe, il prime pour la couleur de la card —
    // c'est la décision finale (l'avocat a écarté certains risques).
    const colorScore = hasDualScore ? scoreAvocat! : d.riskScore;
    return {
      toolId: 'risk-score',
      title: 'ÉVALUATION DES RISQUES',
      icon: 'speed',
      summary: {
        label: 'Score',
        primaryValue: primary,
        secondaryValue: d.riskLevel ?? undefined,
      },
      metierAlertLevel: this.riskAlertLevel(colorScore),
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
  // F-228 SF-228-01 — loader partagé qui charge les 4 alignements en parallèle
  // (forkJoin + fail-open par stream). Symétrie avec `<app-decisional-tools-panel>`.
  private readonly alignmentsLoader = inject(DecisionToolAlignmentsLoader);
  // F-229 SF-229-01 — résout les tiles "résumé" du dashboard sur la cible
  // canonique du badge F-162 équivalent (popup pour pieces, sous-page pour
  // risques, anchor scroll pour pistes/questions).
  private readonly badgeNavigation = inject(BadgeNavigationService);

  constructor(
    private dashboardService: CaseDashboardService,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    this.reload(true);
    this.loadAlignments();
    if (this.refreshService) {
      this.refreshService.refresh$
        .pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef))
        .subscribe(() => {
          this.reload(false);
          // F-228 SF-228-01 — re-fetch des 4 alignements à chaque run de
          // Synthèse enrichie (cohérence F-176 stricte). Le PUT statut
          // piste/pièce/risque + PUT réponse F-94 ne déclenchent PAS
          // triggerRefresh, donc le re-fetch ici ne s'enclenche que post-
          // analyse — comportement voulu.
          this.loadAlignments();
        });
    }
  }

  /**
   * F-228 SF-228-01 — Charge les 4 alignements en parallèle via le loader
   * partagé. Fail-open par stream (cf. {@link DecisionToolAlignmentsLoader}).
   * OnPush-safe : mutation via `signal.set()` qui déclenche la CD nativement.
   */
  private loadAlignments(): void {
    if (!this.caseFileId) return;
    this.alignmentsLoader.loadAll(this.caseFileId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (bundle) => {
          this.retainedPistes.set(bundle.retainedPistes);
          this.piecesAlignment.set(bundle.piecesAlignment);
          this.risquesAlignment.set(bundle.risquesAlignment);
          this.aiQuestionsAlignment.set(bundle.aiQuestionsAlignment);
        },
        error: () => {
          this.retainedPistes.set([]);
          this.piecesAlignment.set([]);
          this.risquesAlignment.set([]);
          this.aiQuestionsAlignment.set([]);
        },
      });
  }

  /**
   * F-167 SF-167-01 / SF-167-05 — Ouvre le modal du composant outil
   * correspondant à une tile générique. Résout le composant via
   * TOOL_REGISTRY (même logique que `<app-decisional-tools-panel>`). Si le
   * toolId est inconnu : no-op + console.warn (cohérent avec resolveEntry).
   */
  openGenericTool(toolId: string): void {
    // F-229 SF-229-01 — les 5 tiles "résumé" du dashboard décisionnel sont
    // désormais alignées sur les cibles canoniques de la grille de badges
    // F-162 via {@link BadgeNavigationService}. Mismatches corrigés :
    //   - F-192-retained-pistes-summary  → anchor `section-pistes` (scroll inline)
    //   - F-193-procedure-checks-summary → anchor `section-checklist` (scroll inline)
    //   - F-194-pieces-summary           → popup `SynthesisShortBlockDialogComponent`
    //   - F-195-risques-summary          → sous-page `/synthesis/risques`
    //   - F-196-questions-summary        → anchor `section-questions` (scroll inline)
    // Audit visuel CA-09 : clic d'une tile résumé doit ouvrir EXACTEMENT
    // la même chose que le badge F-162 équivalent.
    // F-229 SF-229-02 (2026-05-09) — alignement avec le toolId backend
    // `CaseFileDashboardService.java:525` ("F-192-retained-pistes-summary").
    // L'ancien check `'RETAINED_PISTES_SUMMARY'` (UPPERCASE snake) ne matchait
    // pas, le clic était silencieux (console.warn + return). Bug staging
    // Immigration Chen 17 du 2026-05-09.
    // F-229 SF-229-03 (2026-05-09) — handler F-193-procedure-checks-summary
    // ajouté (orphelin sinon → console.warn + return). Symétrie avec les 4
    // autres tiles résumé.
    if (toolId === 'F-192-retained-pistes-summary') {
      this.badgeNavigation.go('pistes', this.caseFileId);
      return;
    }
    // F-229 SF-229-03 — tile résumé F-193 SF-193-01 procedure checks F-96
    // matérialisés. Renvoie vers anchor `section-checklist` (cohérent badge
    // F-162 `checklist`).
    if (toolId === 'F-193-procedure-checks-summary') {
      this.badgeNavigation.go('checklist', this.caseFileId);
      return;
    }
    if (toolId === 'F-194-pieces-summary') {
      this.badgeNavigation.go('pieces', this.caseFileId, {
        popupItems: this.synthesis?.piecesManquantes ?? [],
        popupTitle: 'Pièces manquantes',
        popupIcon: 'report_problem',
      });
      return;
    }
    if (toolId === 'F-195-risques-summary') {
      this.badgeNavigation.go('risques', this.caseFileId);
      return;
    }
    // F-253 SF-253-02 — tile dédiée À_CREUSER : route vers la même cible
    // canonique que F-195-risques-summary (sous-page synthèse / section
    // risques). L'avocat arrive sur la section où il peut arbitrer.
    if (toolId === 'F-253-risques-a-creuser') {
      this.badgeNavigation.go('risques', this.caseFileId);
      return;
    }
    if (toolId === 'F-196-questions-summary') {
      this.badgeNavigation.go('questions', this.caseFileId);
      return;
    }
    const entry = DecisionToolsPanelComponent.TOOL_REGISTRY.get(toolId);
    if (!entry) {
      // eslint-disable-next-line no-console
      console.warn(`[case-dashboard] Unknown toolId for generic tile: ${toolId}`);
      return;
    }
    const meta = getToolMetadata(entry.component);
    // F-228 SF-228-01 — ctx symétrique au panel F-IA-04 (cf.
    // `decisional-tools-panel.component.ts:1758+ componentInputsFor`).
    // Sans ces 4 alignements, les outils ouverts depuis le dashboard
    // recevaient un ctx amputé → bug staging Immigration Chen 17 du
    // 2026-05-08 (popup `<app-immigration-title-decision-section>` perdait
    // ses pistes stratégiques entre le 1er et le 2nd clic).
    const ctx: DecisionToolContext = {
      caseFileId: this.caseFileId,
      synthesis: this.synthesis,
      workspaceCountry: this.workspaceCountry,
      caseFileTitle: '',
      procedureChecks: this.procedureChecks,
      aiQuestions: this.aiQuestions,
      pistesRetenues: this.retainedPistes(),
      piecesAlignment: this.piecesAlignment(),
      risquesAlignment: this.risquesAlignment(),
      aiQuestionsAlignment: this.aiQuestionsAlignment(),
    };
    const inputs = entry.inputs(ctx);
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
