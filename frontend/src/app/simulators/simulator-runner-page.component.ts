import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import {
  DecisionToolsPanelComponent,
  DecisionToolRegistryEntry,
  DecisionToolContext,
} from '../case-files/decisional-tools-panel/decisional-tools-panel.component';
import { SimulatorsCatalogService } from '../core/services/simulators-catalog.service';
import { SimulatorsCatalogResponse } from '../core/models/simulators-catalog.model';
import { STANDALONE_READY_TOOL_IDS } from './standalone-ready-tools';

/**
 * F-163 / SF-163-02a — Runner du mode simulateur autonome.
 *
 * Route : `/simulators/:toolId` (lazy, protégée par `AuthGuard` au niveau
 * parent dans `app.routes.ts`).
 *
 * Comportement :
 *   1. Lit `:toolId` depuis `ActivatedRoute.paramMap`.
 *   2. Croise avec `DecisionToolsPanelComponent.TOOL_REGISTRY` pour récupérer
 *      la `DecisionToolRegistryEntry`. Si introuvable → page 404.
 *   3. Vérifie que le `toolId` est dans `STANDALONE_READY_TOOL_IDS`. Si non →
 *      message « Disponible bientôt » (refactor du composant prévu dans
 *      SF-163-02b/c/d).
 *   4. Charge le catalogue workspace (`GET /api/v1/simulators`) pour résoudre
 *      `workspaceCountry` (réutilise le mécanisme de SF-163-01 — pas de
 *      nouveau service). Fallback `FRANCE` si la résolution échoue.
 *   5. Instancie dynamiquement le composant via `*ngComponentOutlet` avec
 *      `inputs = entry.inputs(ctx)` où `ctx.standaloneMode = true` et
 *      `ctx.caseFileId = null` (le composant pilote détectera et basculera
 *      en mode simulateur autonome).
 *
 * La responsabilité de l'UX standalone (bannière 🧪, bypass prefill /
 * coherence / triggerRefresh, switch endpoint) reste dans le composant
 * décisionnel lui-même — le runner ne fait qu'orchestrer le câblage.
 */
@Component({
  selector: 'app-simulator-runner-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './simulator-runner-page.component.html',
  styleUrls: ['./simulator-runner-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SimulatorRunnerPageComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly catalogService = inject(SimulatorsCatalogService);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly toolId = signal<string | null>(null);
  readonly workspaceCountry = signal<string>('FRANCE');

  /** Résolution sync de l'entrée TOOL_REGISTRY (null si toolId inconnu). */
  readonly entry = computed<DecisionToolRegistryEntry | null>(() => {
    const id = this.toolId();
    if (!id) return null;
    const e = DecisionToolsPanelComponent.TOOL_REGISTRY.get(id);
    return e ?? null;
  });

  /** True si le toolId est valide et fait partie de la whitelist standalone. */
  readonly isStandaloneReady = computed<boolean>(() => {
    const id = this.toolId();
    return !!id && STANDALONE_READY_TOOL_IDS.has(id);
  });

  /** True si toolId fourni mais absent de TOOL_REGISTRY → page 404. */
  readonly isNotFound = computed<boolean>(() => {
    const id = this.toolId();
    return !!id && !this.entry();
  });

  /** Inputs propagés au composant via *ngComponentOutlet. */
  readonly componentInputs = computed<Record<string, unknown> | null>(() => {
    const entry = this.entry();
    if (!entry || !this.isStandaloneReady()) return null;
    const ctx = this.buildContext();
    return entry.inputs(ctx);
  });

  /** Construit le contexte minimal pour TOOL_REGISTRY.inputs(ctx). */
  private buildContext(): DecisionToolContext {
    return {
      // SF-163-02a : en mode standalone, pas de dossier client.
      caseFileId: null as unknown as string,
      synthesis: null,
      workspaceCountry: this.workspaceCountry(),
      caseFileTitle: '',
      procedureChecks: [],
      aiQuestions: [],
      typeLitigeOverride: null,
      pistesRetenues: [],
      piecesAlignment: [],
      risquesAlignment: [],
      aiQuestionsAlignment: [],
      // SF-163-02a : flag standalone propagé via TOOL_REGISTRY.inputs(ctx).
      standaloneMode: true,
    };
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe((params) => {
      const id = params.get('toolId');
      this.toolId.set(id);
      this.loadWorkspaceCountry();
    });
  }

  /**
   * Résout le pays du workspace en réutilisant `GET /api/v1/simulators`
   * (déjà appelé par la page catalogue — pas de nouveau service).
   * Fallback `FRANCE` en cas d'erreur — le composant pilote affichera
   * éventuellement une bannière mismatch (gate cohérence visuelle).
   */
  private loadWorkspaceCountry(): void {
    this.loading.set(true);
    this.catalogService.getCatalog().subscribe({
      next: (cat: SimulatorsCatalogResponse) => {
        this.workspaceCountry.set(cat.country ?? 'FRANCE');
        this.loading.set(false);
      },
      error: () => {
        // Fail-open : on garde FRANCE par défaut, l'avocat peut quand même
        // utiliser le simulateur (le mode standalone ne nécessite pas le
        // pays exact, juste un fallback raisonnable).
        this.loading.set(false);
        this.snackBar.open(
          'Impossible de résoudre le pays du workspace — fallback France.',
          'Fermer',
          { duration: 4000 },
        );
      },
    });
  }
}
