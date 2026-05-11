import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { SimulatorsCatalogService } from '../core/services/simulators-catalog.service';
import { SimulatorsCatalogResponse } from '../core/models/simulators-catalog.model';
import {
  DecisionToolsPanelComponent,
  DecisionToolRegistryEntry,
} from '../case-files/decisional-tools-panel/decisional-tools-panel.component';
import { getToolMetadata } from '../case-files/decisional-tools-panel/decision-tool.contract';
import {
  SimulatorInfoDialogComponent,
  SimulatorInfoDialogData,
} from './simulator-info-dialog.component';

interface DisplayTool {
  toolId: string;
  displayLabel: string;
  shortLabel: string | null;
  icon: string;
  /** Forme normalisée (NFD + lowercase + sans diacritiques) pour le filtre. */
  searchKey: string;
}

const LEGAL_DOMAIN_LABELS: Record<string, string> = {
  DROIT_DU_TRAVAIL: 'Droit du travail',
  DROIT_FAMILLE: 'Droit de la famille',
  DROIT_IMMIGRATION: 'Droit de l’immigration',
};

const COUNTRY_LABELS: Record<string, string> = {
  FRANCE: 'France',
  BELGIQUE: 'Belgique',
};

/**
 * F-163 / SF-163-01 — Page catalogue des simulateurs (outils décisionnels)
 * disponibles pour le workspace courant (domaine + pays). V1 :
 *   - liste filtrée par recherche temps réel (case + accents insensitive),
 *   - clic sur card → dialog pédagogique « Créer un dossier »,
 *   - aucun état persisté (la page se reconstruit à chaque visite).
 */
@Component({
  selector: 'app-simulators-catalog-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './simulators-catalog-page.component.html',
  styleUrls: ['./simulators-catalog-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class SimulatorsCatalogPageComponent implements OnInit {
  private readonly service = inject(SimulatorsCatalogService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);

  readonly loading = signal(true);
  readonly error = signal(false);
  readonly catalog = signal<SimulatorsCatalogResponse | null>(null);
  readonly searchQuery = signal('');

  /** Map TOOL_REGISTRY exposée pour les spécs (lecture seule). */
  static readonly TOOL_REGISTRY = DecisionToolsPanelComponent.TOOL_REGISTRY;

  /** Liste complète des outils du workspace, après croisement avec TOOL_REGISTRY. */
  readonly availableTools = computed<DisplayTool[]>(() => {
    const cat = this.catalog();
    if (!cat) return [];
    const out: DisplayTool[] = [];
    for (const toolId of cat.toolIds) {
      const entry = DecisionToolsPanelComponent.TOOL_REGISTRY.get(toolId) as
        | DecisionToolRegistryEntry
        | undefined;
      if (!entry) {
        // Garde-fou F-164 — skip silencieux + warn (forward-compat).
        // eslint-disable-next-line no-console
        console.warn(
          `[simulators-catalog-page] toolId orphelin (absent de TOOL_REGISTRY) skippé : ${toolId}`,
        );
        continue;
      }
      const meta = getToolMetadata(entry.component);
      out.push({
        toolId,
        displayLabel: entry.displayLabel,
        shortLabel: meta?.label ?? null,
        icon: meta?.icon ?? 'extension',
        searchKey: normalize(entry.displayLabel),
      });
    }
    return out;
  });

  /** Sous-liste après application du filtre `searchQuery`. */
  readonly filteredTools = computed<DisplayTool[]>(() => {
    const all = this.availableTools();
    const q = normalize(this.searchQuery().trim());
    if (!q) return all;
    return all.filter((t) => t.searchKey.includes(q));
  });

  /** Compteur `{ matching, total }` pour l'affichage « N résultats sur M ». */
  readonly resultsCount = computed<{ matching: number; total: number }>(() => ({
    matching: this.filteredTools().length,
    total: this.availableTools().length,
  }));

  readonly legalDomainLabel = computed(() => {
    const code = this.catalog()?.legalDomain;
    if (!code) return null;
    return LEGAL_DOMAIN_LABELS[code] ?? code;
  });

  readonly countryLabel = computed(() => {
    const code = this.catalog()?.country;
    if (!code) return null;
    return COUNTRY_LABELS[code] ?? code;
  });

  /** True quand le workspace n'a pas de legalDomain (bannIère info CTA `/workspace/settings`). */
  readonly isWorkspaceUnconfigured = computed(() => {
    const cat = this.catalog();
    return !!cat && !cat.legalDomain;
  });

  /** True quand la recherche en cours ne matche aucun outil disponible. */
  readonly isSearchEmpty = computed(() => {
    return (
      this.availableTools().length > 0 &&
      this.searchQuery().trim().length > 0 &&
      this.filteredTools().length === 0
    );
  });

  ngOnInit(): void {
    this.loadCatalog();
  }

  loadCatalog(): void {
    this.loading.set(true);
    this.error.set(false);
    this.service.getCatalog().subscribe({
      next: (res) => {
        this.catalog.set(res);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        this.error.set(true);
        this.snackBar.open(
          'Impossible de charger les simulateurs. Réessayez.',
          'Fermer',
          { duration: 5000, panelClass: ['snack-error'] },
        );
      },
    });
  }

  onSearchChange(value: string): void {
    this.searchQuery.set(value ?? '');
  }

  onResetSearch(): void {
    this.searchQuery.set('');
  }

  onCardClick(tool: DisplayTool): void {
    const data: SimulatorInfoDialogData = { displayLabel: tool.displayLabel };
    this.dialog.open(SimulatorInfoDialogComponent, {
      data,
      width: '560px',
      maxWidth: '94vw',
      autoFocus: false,
    });
  }

  /** Test helper — expose la normalisation pour les specs. */
  static normalizeForTest(value: string): string {
    return normalize(value);
  }
}

/**
 * Normalisation de la chaîne de recherche : NFD + suppression des diacritiques
 * (`\\p{Diacritic}`) + lowercase. Permet à `validite` de matcher `Validité`.
 */
function normalize(value: string): string {
  return value
    .normalize('NFD')
    .replace(/\p{Diacritic}/gu, '')
    .toLowerCase();
}
