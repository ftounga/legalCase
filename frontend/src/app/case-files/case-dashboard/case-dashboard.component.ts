import { Component, DestroyRef, Input, OnInit, Optional, Type, inject, signal, computed } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DecimalPipe } from '@angular/common';
import { debounceTime } from 'rxjs';
import { CaseDashboardService } from '../../core/services/case-dashboard.service';
import { DashboardResponse, DashboardTile as BackendDashboardTile } from '../../core/models/case-dashboard.model';
import { CaseDashboardRefreshService } from './case-dashboard-refresh.service';
import { DashboardTileComponent } from './dashboard-tile/dashboard-tile.component';
import { DecisionToolCardComponent } from '../decisional-tools-panel/decision-tool-card/decision-tool-card.component';
import { DecisionToolModalService } from '../decisional-tools-panel/decision-tool-modal/decision-tool-modal.service';
import { DecisionToolsPanelComponent } from '../decisional-tools-panel/decisional-tools-panel.component';
import { getToolMetadata } from '../decisional-tools-panel/decision-tool.contract';
import { DecisionToolSummary, MetierAlertLevel } from '../decisional-tools-panel/decision-tool-summary.model';
import { LicenciementSectionComponent } from '../licenciement-section/licenciement-section.component';
import { IndemniteComparatifSectionComponent } from '../indemnite-comparatif-section/indemnite-comparatif-section.component';
import { AncienneteSectionComponent } from '../anciennete-section/anciennete-section.component';
import { ImmigrationTitleDecisionSectionComponent } from '../immigration-title-decision-section/immigration-title-decision-section.component';
import { ImmigrationWorkRightSectionComponent } from '../immigration-work-right-section/immigration-work-right-section.component';
import { ImmigrationRecoursSectionComponent } from '../immigration-recours-section/immigration-recours-section.component';
import { PartageImmobilierSectionComponent } from '../partage-immobilier-section/partage-immobilier-section.component';
import { CalendrierGardeSectionComponent } from '../calendrier-garde-section/calendrier-garde-section.component';
import { DivorceChecklistSectionComponent } from '../divorce-checklist-section/divorce-checklist-section.component';

/**
 * F-177 SF-177-09 — Tile dashboard agrégé reproduit via `<app-decision-tool-card>`.
 * Le riskScore est rendu en tile spécial non-cliquable (pas de tool correspondant).
 */
export interface DashboardCardTile {
  toolId: string;
  theme: string;
  title: string;
  icon: string;
  summary: DecisionToolSummary | null;
  metierAlertLevel?: MetierAlertLevel;
  component: Type<unknown> | null;
  componentInputs: Record<string, unknown>;
  disabled: boolean;
}

@Component({
  selector: 'app-case-dashboard',
  standalone: true,
  imports: [MatIconModule, MatProgressSpinnerModule, DecimalPipe, DecisionToolCardComponent, DashboardTileComponent],
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

  loading = signal(false);
  dashboard = signal<DashboardResponse | null>(null);

  hasAnyData = computed(() => {
    const d = this.dashboard();
    if (!d) return false;
    return d.licenciement || d.indemnites || d.anciennete || d.titleDecision ||
           d.workRight || d.recours || d.partage || d.garde || d.divorce || d.riskScore != null
           // F-167 SF-167-01 : prend également en compte les tiles génériques.
           || (d.tiles && d.tiles.length > 0);
  });

  /**
   * F-167 SF-167-01 — Liste des tiles génériques renvoyées par le backend.
   * Coexiste avec les 9 tiles typées en haut du dashboard pendant la
   * transition (SF-167-01 → SF-167-04). SF-167-05 fusionnera et supprimera
   * les tiles typées.
   */
  readonly genericTiles = computed<BackendDashboardTile[]>(() => {
    return this.dashboard()?.tiles ?? [];
  });

  /**
   * F-177 SF-177-09 — produit la liste de tiles à rendre via `<app-decision-tool-card>`.
   * riskScore en tête (non cliquable), puis 9 tiles ordonnées (Travail → Immigration → Famille).
   * Les champs nuls sont filtrés.
   */
  readonly tiles = computed<DashboardCardTile[]>(() => {
    const d = this.dashboard();
    if (!d) return [];
    return this.tilesFromDashboard(d);
  });

  private readonly destroyRef = inject(DestroyRef);
  private readonly modalService = inject(DecisionToolModalService);

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
   * F-177 SF-177-09 — ouvre le composant outil de la tile dans le modal 90vw/90vh.
   * Tile riskScore : disabled === true → no-op.
   */
  openTile(tile: DashboardCardTile): void {
    if (tile.disabled || !tile.component) return;
    this.modalService.open({
      toolId: tile.toolId,
      title: tile.title,
      icon: tile.icon,
      component: tile.component,
      inputs: { ...tile.componentInputs, forceExpanded: true },
    });
  }

  /**
   * F-167 SF-167-01 — Ouvre le modal du composant outil correspondant à une
   * tile générique. Résout le composant via TOOL_REGISTRY (même logique que
   * `<app-decisional-tools-panel>`). Si le toolId est inconnu, no-op +
   * console.warn (cohérent avec resolveEntry du panel).
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
    });
  }

  tilesFromDashboard(d: DashboardResponse): DashboardCardTile[] {
    const tiles: DashboardCardTile[] = [];

    if (d.riskScore != null) {
      tiles.push({
        toolId: 'risk-score',
        theme: 'DIAGNOSTIC',
        title: 'ÉVALUATION DES RISQUES',
        icon: 'speed',
        summary: {
          label: 'Score',
          primaryValue: `${d.riskScore} %`,
          secondaryValue: d.riskLevel ?? undefined,
        },
        metierAlertLevel: this.riskAlertLevel(d.riskScore),
        component: null,
        componentInputs: {},
        disabled: true,
      });
    }

    if (d.licenciement) {
      tiles.push(this.buildToolTile({
        toolId: 'F-DT-08-licenciement-validity',
        theme: 'VALIDITE',
        component: LicenciementSectionComponent,
        summary: {
          label: 'Validité',
          primaryValue: d.licenciement.verdict,
          secondaryValue: `${d.licenciement.criteresNonConformes}/${d.licenciement.criteresTotal} non conformes`,
        },
        metierAlertLevel: d.licenciement.verdict === 'VALIDE' ? 'OK' : 'ALERT',
      }));
    }

    if (d.indemnites) {
      tiles.push(this.buildToolTile({
        toolId: 'F-DT-09-comparateur-indemnites',
        theme: 'INDEMNITES',
        component: IndemniteComparatifSectionComponent,
        summary: {
          label: 'Indemnités',
          primaryValue: `${this.formatEuros(d.indemnites.fourchetteBasse)} – ${this.formatEuros(d.indemnites.fourhetteHaute)} €`,
          secondaryValue: d.indemnites.baremeSource,
        },
      }));
    }

    if (d.anciennete) {
      tiles.push(this.buildToolTile({
        toolId: 'F-DT-07-anciennete-conges-prime',
        theme: 'DELAIS',
        component: AncienneteSectionComponent,
        summary: {
          label: 'Ancienneté',
          primaryValue: `${d.anciennete.annees} an(s) ${d.anciennete.mois} mois`,
          secondaryValue: `${d.anciennete.congesTotalJours}j congés`,
        },
        metierAlertLevel: d.anciennete.ecartsDetectes > 0 ? 'WARNING' : undefined,
      }));
    }

    if (d.titleDecision) {
      tiles.push(this.buildToolTile({
        toolId: 'F-IM-05-arbre-decisionnel-titre',
        theme: 'DIAGNOSTIC',
        component: ImmigrationTitleDecisionSectionComponent,
        summary: {
          label: 'Titre',
          primaryValue: `${d.titleDecision.nbRecommandations} recommandation(s)`,
          secondaryValue: d.titleDecision.premierTitreLabel ?? undefined,
        },
      }));
    }

    if (d.workRight) {
      tiles.push(this.buildToolTile({
        toolId: 'F-IM-07-droit-au-travail',
        theme: 'DIAGNOSTIC',
        component: ImmigrationWorkRightSectionComponent,
        summary: {
          label: 'Droit au travail',
          primaryValue: d.workRight.droitTravail,
          secondaryValue: d.workRight.titreLabel,
        },
        metierAlertLevel: d.workRight.droitTravail === 'OUI' ? 'OK' : (d.workRight.droitTravail === 'NON' ? 'ALERT' : 'WARNING'),
      }));
    }

    if (d.recours) {
      tiles.push(this.buildToolTile({
        toolId: 'F-IM-06-recours',
        theme: 'DELAIS',
        component: ImmigrationRecoursSectionComponent,
        summary: {
          label: 'Recours',
          primaryValue: d.recours.recoursLabel,
          secondaryValue: d.recours.dateLimiteDepassee
            ? 'DÉLAI DÉPASSÉ'
            : (d.recours.dateLimite ? `Limite : ${d.recours.dateLimite}` : undefined),
        },
        metierAlertLevel: d.recours.dateLimiteDepassee ? 'ALERT' : undefined,
      }));
    }

    if (d.partage) {
      tiles.push(this.buildToolTile({
        toolId: 'F-FA-05-partage-immobilier',
        theme: 'DIAGNOSTIC',
        component: PartageImmobilierSectionComponent,
        summary: {
          label: 'Partage',
          primaryValue: `${this.formatEuros(d.partage.soulte)} €`,
          secondaryValue: `Coût total : ${this.formatEuros(d.partage.coutTotal)} €`,
        },
      }));
    }

    if (d.garde) {
      tiles.push(this.buildToolTile({
        toolId: 'F-FA-06-calendrier-garde',
        theme: 'DIAGNOSTIC',
        component: CalendrierGardeSectionComponent,
        summary: {
          label: 'Garde',
          primaryValue: d.garde.gardeLabel,
          secondaryValue: `${d.garde.joursParentA}j / ${d.garde.joursParentB}j`,
        },
      }));
    }

    if (d.divorce) {
      tiles.push(this.buildToolTile({
        toolId: 'F-FA-07-checklist-divorce',
        theme: 'DOCUMENTS',
        component: DivorceChecklistSectionComponent,
        summary: {
          label: 'Divorce',
          primaryValue: `${d.divorce.progressionPct} %`,
          secondaryValue: `${d.divorce.etapesCompletees}/${d.divorce.etapesTotal} étapes`,
        },
      }));
    }

    return tiles;
  }

  private buildToolTile(args: {
    toolId: string;
    theme: string;
    component: Type<unknown>;
    summary: DecisionToolSummary;
    metierAlertLevel?: MetierAlertLevel;
  }): DashboardCardTile {
    const meta = getToolMetadata(args.component);
    return {
      toolId: args.toolId,
      theme: args.theme,
      title: meta?.label ?? args.toolId,
      icon: meta?.icon ?? 'extension',
      summary: args.summary,
      metierAlertLevel: args.metierAlertLevel,
      component: args.component,
      componentInputs: {
        caseFileId: this.caseFileId,
        workspaceCountry: this.workspaceCountry,
        aiData: this.synthesis?.travailExtractedData
          ?? this.synthesis?.immigrationExtractedData
          ?? this.synthesis?.familleExtractedData
          ?? null,
        procedureChecks: this.procedureChecks,
        aiQuestions: this.aiQuestions,
        piecesManquantes: this.synthesis?.piecesManquantesDetails,
      },
      disabled: false,
    };
  }

  private riskAlertLevel(score: number): MetierAlertLevel {
    if (score < 30) return 'OK';
    if (score < 60) return 'WARNING';
    return 'ALERT';
  }

  private formatEuros(amount: number): string {
    return new Intl.NumberFormat('fr-FR', { maximumFractionDigits: 0 }).format(amount);
  }

  private reload(showSpinner: boolean): void {
    if (showSpinner) this.loading.set(true);
    this.dashboardService.get(this.caseFileId).subscribe({
      next: d => { this.dashboard.set(d); this.loading.set(false); },
      error: () => { this.loading.set(false); },
    });
  }
}
