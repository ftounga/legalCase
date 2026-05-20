import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  Optional,
  SimpleChanges,
  computed,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EtrangerMaladeService } from '../../core/services/etranger-malade.service';
import {
  EtrangerMaladeRequest,
  EtrangerMaladeResponse,
} from '../../core/models/etranger-malade.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

/**
 * SF-214-01 stub — entrée TOOL_REGISTRY pour F-IM-25-etranger-malade-l4259-fr.
 * Composant complet livré en SF-214-02.
 */
@Component({
  selector: 'app-etranger-malade-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatChipsModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatCheckboxModule,
  ],
  template: `
    <div class="tool-section">
      <p class="tool-stub-notice">Outil Étranger malade L.425-9 — à implémenter (SF-214-02).</p>
    </div>
  `,
  styles: [`
    .tool-section { padding: 16px; }
    .tool-stub-notice { color: #888; font-style: italic; }
  `],
})
export class EtrangerMaladeSectionComponent implements OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry?: string;
  @Input() aiData?: ImmigrationExtractedData;
  @Input() standaloneMode = false;

  result = signal<EtrangerMaladeResponse | null>(null);
  loading = signal(false);

  constructor(
    private readonly service: EtrangerMaladeService,
    private readonly cdr: ChangeDetectorRef,
    private readonly snackBar: MatSnackBar,
    @Optional() private readonly dashboardRefresh?: CaseDashboardRefreshService,
  ) {}

  ngOnChanges(_: SimpleChanges): void {
    if (this.caseFileId) this.loadExisting();
  }

  private loadExisting(): void {
    this.service.get(this.caseFileId).subscribe({
      next: (r) => { this.result.set(r); this.cdr.markForCheck(); },
      error: () => {},
    });
  }
}
