import { Component, Input, OnInit, OnChanges, SimpleChanges, Optional, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RuptureConvIndemniteService } from '../../core/services/rupture-conv-indemnite.service';
import { RuptureConvIndemniteResponse } from '../../core/models/rupture-conv-indemnite.model';
import { CaseAnalysisResult } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

/**
 * SF-132-02 : outil décisionnel dédié "Indemnité rupture conventionnelle" (FR).
 * Affiché conditionnellement par case-file-detail quand
 * compensationEstimate.typeRupture == RUPTURE_CONVENTIONNELLE && country == FRANCE.
 */
@Component({
  selector: 'app-rupture-conv-indemnite-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './rupture-conv-indemnite-section.component.html',
  styleUrl: './rupture-conv-indemnite-section.component.scss'
})
export class RuptureConvIndemniteSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() synthesis?: CaseAnalysisResult | null;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<RuptureConvIndemniteResponse | null>(null);

  ancienneteAnnees = signal<number | null>(null);
  salaireMensuel = signal<number | null>(null);

  private prefilled = false;

  constructor(
    private service: RuptureConvIndemniteService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['synthesis'] && !this.prefilled) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const a = this.ancienneteAnnees();
    const s = this.salaireMensuel();
    return a !== null && a >= 0 && s !== null && s > 0;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request = {
      ancienneteAnnees: this.ancienneteAnnees()!,
      salaireMensuel: this.salaireMensuel()!,
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
      }
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.ancienneteAnnees.set(r.ancienneteAnnees);
        this.salaireMensuel.set(r.salaireMensuel);
        this.showForm.set(false);
        this.loading.set(false);
        this.prefilled = true;
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
        this.prefillFromAi();
      }
    });
  }

  private prefillFromAi(): void {
    if (this.prefilled) return;
    const estim = this.synthesis?.compensationEstimate;
    if (!estim) return;
    if (estim.ancienneteAnnees != null && this.ancienneteAnnees() == null) {
      this.ancienneteAnnees.set(estim.ancienneteAnnees);
    }
    if (estim.salaireReference != null && estim.salaireReference > 0 && this.salaireMensuel() == null) {
      this.salaireMensuel.set(estim.salaireReference);
    }
    this.prefilled = true;
  }
}
