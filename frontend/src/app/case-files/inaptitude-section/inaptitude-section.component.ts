import { Component, Input, OnInit, Optional, signal, computed } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { InaptitudeService } from '../../core/services/inaptitude.service';
import {
  InaptitudeResponse,
  OrigineInaptitude,
  OrigineInaptitudeOption,
  ORIGINES_BE,
  ORIGINES_FR,
} from '../../core/models/inaptitude.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';

/**
 * SF-DT-15-02 : outil décisionnel dédié "Licenciement pour inaptitude"
 * (F-DT-15). FR + BE. Consomme l'API SF-DT-15-01. Affiché conditionnellement
 * par le panel F-IA-04 (tool_id 'F-DT-15-inaptitude').
 */
@Component({
  selector: 'app-inaptitude-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
  ],
  templateUrl: './inaptitude-section.component.html',
  styleUrl: './inaptitude-section.component.scss',
})
export class InaptitudeSectionComponent implements OnInit {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<InaptitudeResponse | null>(null);

  salaireMensuelReference = signal<number | null>(null);
  ancienneteAnnees = signal<number | null>(null);
  origineInaptitude = signal<OrigineInaptitude | null>(null);
  reclassementRespecte = signal<boolean>(false);
  avisMedecinTravailDate = signal<string | null>(null);

  originesDisponibles = computed<OrigineInaptitudeOption[]>(() =>
    this.workspaceCountry === 'BELGIQUE' ? ORIGINES_BE : ORIGINES_FR
  );

  constructor(
    private service: InaptitudeService,
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
    const a = this.ancienneteAnnees();
    const o = this.origineInaptitude();
    return s !== null && s > 0
      && a !== null && a >= 0 && Number.isInteger(a)
      && o !== null;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request = {
      salaireMensuelReference: this.salaireMensuelReference()!,
      ancienneteAnnees: this.ancienneteAnnees()!,
      origineInaptitude: this.origineInaptitude()!,
      reclassementRespecte: this.reclassementRespecte(),
      ...(this.avisMedecinTravailDate()
        ? { avisMedecinTravailDate: this.avisMedecinTravailDate()! }
        : {}),
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
        this.ancienneteAnnees.set(r.ancienneteAnnees);
        this.origineInaptitude.set(r.origineInaptitude);
        this.reclassementRespecte.set(r.reclassementRespecte);
        this.avisMedecinTravailDate.set(r.avisMedecinTravailDate);
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
