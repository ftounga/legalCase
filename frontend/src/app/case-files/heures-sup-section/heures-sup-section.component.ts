import { Component, Input, OnInit, Optional, signal } from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { HeuresSupService } from '../../core/services/heures-sup.service';
import {
  HeuresSupRequest,
  HeuresSupResponse,
} from '../../core/models/heures-sup.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

/**
 * SF-DT-19-02 : outil décisionnel dédié "Calculateur heures supplémentaires"
 * (F-DT-19). FR + BE. Consomme l'API SF-DT-19-01.
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * 'F-DT-19-heures-sup').
 */
@Component({
  selector: 'app-heures-sup-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './heures-sup-section.component.html',
  styleUrl: './heures-sup-section.component.scss',
})
export class HeuresSupSectionComponent implements OnInit {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<HeuresSupResponse | null>(null);

  tauxHoraireBrut = signal<number | null>(null);

  // FR
  heuresSupDeclarees25pct = signal<number | null>(null);
  heuresSupDeclarees50pct = signal<number | null>(null);
  heuresHorsContingent = signal<number | null>(null);
  tauxMajoration25 = signal<number | null>(25);
  tauxMajoration50 = signal<number | null>(50);

  // BE
  heuresSupSemaine = signal<number | null>(null);
  heuresDimancheJoursFeries = signal<number | null>(null);

  constructor(
    private service: HeuresSupService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    this.load();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  formValid(): boolean {
    const taux = this.tauxHoraireBrut();
    if (taux === null || taux <= 0) return false;
    if (this.workspaceCountry === 'FRANCE') {
      const h25 = this.heuresSupDeclarees25pct() ?? 0;
      const h50 = this.heuresSupDeclarees50pct() ?? 0;
      const hHc = this.heuresHorsContingent() ?? 0;
      if (h25 < 0 || h50 < 0 || hHc < 0) return false;
      if (h25 + h50 + hHc <= 0) return false;
      const tx25 = this.tauxMajoration25();
      const tx50 = this.tauxMajoration50();
      if (tx25 === null || tx25 < 10 || tx25 > 50) return false;
      if (tx50 === null || tx50 < 10 || tx50 > 50) return false;
      return true;
    }
    // BELGIQUE
    const hSem = this.heuresSupSemaine() ?? 0;
    const hDim = this.heuresDimancheJoursFeries() ?? 0;
    if (hSem < 0 || hDim < 0) return false;
    if (hSem + hDim <= 0) return false;
    return true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: HeuresSupRequest = {
      tauxHoraireBrut: this.tauxHoraireBrut()!,
    };
    if (this.workspaceCountry === 'FRANCE') {
      request.heuresSupDeclarees25pct = this.heuresSupDeclarees25pct() ?? 0;
      request.heuresSupDeclarees50pct = this.heuresSupDeclarees50pct() ?? 0;
      request.heuresHorsContingent = this.heuresHorsContingent() ?? 0;
      request.tauxMajoration25 = this.tauxMajoration25() ?? 25;
      request.tauxMajoration50 = this.tauxMajoration50() ?? 50;
    } else {
      request.heuresSupSemaine = this.heuresSupSemaine() ?? 0;
      request.heuresDimancheJoursFeries = this.heuresDimancheJoursFeries() ?? 0;
    }
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Rappel heures sup calculé', 'OK', { duration: 2500 });
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
        this.tauxHoraireBrut.set(r.tauxHoraireBrut);
        this.heuresSupDeclarees25pct.set(r.heuresSupDeclarees25pct);
        this.heuresSupDeclarees50pct.set(r.heuresSupDeclarees50pct);
        this.heuresHorsContingent.set(r.heuresHorsContingent);
        this.tauxMajoration25.set(r.tauxMajoration25 ?? 25);
        this.tauxMajoration50.set(r.tauxMajoration50 ?? 50);
        this.heuresSupSemaine.set(r.heuresSupSemaine);
        this.heuresDimancheJoursFeries.set(r.heuresDimancheJoursFeries);
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
