import { Component, Input, OnInit, Optional, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HarcelementNulliteService } from '../../core/services/harcelement-nullite.service';
import {
  HarcelementNulliteResponse,
  MotifNullite,
  MotifOption,
  MOTIFS_BE,
  MOTIFS_FR,
} from '../../core/models/harcelement-nullite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';

/**
 * SF-DT-11-02 : outil décisionnel dédié "Indemnité minimum licenciement nul
 * — harcèlement" (F-DT-11). FR + BE. Consomme l'API SF-DT-11-01.
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * 'F-DT-11-harcelement-licenciement-nul').
 */
@Component({
  selector: 'app-harcelement-licenciement-nul-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
  ],
  templateUrl: './harcelement-licenciement-nul-section.component.html',
  styleUrl: './harcelement-licenciement-nul-section.component.scss',
})
export class HarcelementLicenciementNulSectionComponent implements OnInit {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<HarcelementNulliteResponse | null>(null);

  salaireMensuelReference = signal<number | null>(null);
  motifNullite = signal<MotifNullite | null>(null);

  motifsDisponibles = computed<MotifOption[]>(() =>
    this.workspaceCountry === 'BELGIQUE' ? MOTIFS_BE : MOTIFS_FR
  );

  constructor(
    private service: HarcelementNulliteService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const s = this.salaireMensuelReference();
    const m = this.motifNullite();
    return s !== null && s > 0 && m !== null;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request = {
      salaireMensuelReference: this.salaireMensuelReference()!,
      motifNullite: this.motifNullite()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Indemnité calculée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.salaireMensuelReference.set(r.salaireMensuelReference);
        this.motifNullite.set(r.motifNullite);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
      },
    });
  }
}
