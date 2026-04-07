import { Component, Input, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { PartageImmobilierService } from '../../core/services/partage-immobilier.service';
import { PartageImmobilierResponse } from '../../core/models/partage-immobilier.model';

@Component({
  selector: 'app-partage-immobilier-section',
  standalone: true,
  imports: [
    FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule, MatSelectModule,
    MatFormFieldModule, MatInputModule, MatProgressSpinnerModule,
    MatSlideToggleModule,
  ],
  templateUrl: './partage-immobilier-section.component.html',
  styleUrl: './partage-immobilier-section.component.scss'
})
export class PartageImmobilierSectionComponent implements OnInit {
  @Input() caseFileId!: string;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<PartageImmobilierResponse | null>(null);

  country = signal('FRANCE');
  valeurVenale = signal(0);
  capitalRestantDu = signal(0);
  quotePartAttributaire = signal(50);
  isDivorce = signal(true);

  constructor(
    private partageService: PartageImmobilierService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void { this.loadExisting(); }

  toggleCollapsed(): void { this.collapsed.update(v => !v); }

  loadExisting(): void {
    this.loading.set(true);
    this.partageService.get(this.caseFileId).subscribe({
      next: r => { this.result.set(r); this.showForm.set(false); this.loading.set(false); },
      error: () => { this.showForm.set(true); this.loading.set(false); },
    });
  }

  calculate(): void {
    this.calculating.set(true);
    this.partageService.calculate(this.caseFileId, {
      country: this.country(),
      valeurVenale: this.valeurVenale(),
      capitalRestantDu: this.capitalRestantDu(),
      quotePartAttributaire: this.quotePartAttributaire() / 100,
      isDivorce: this.isDivorce(),
    }).subscribe({
      next: r => { this.result.set(r); this.showForm.set(false); this.calculating.set(false); },
      error: () => { this.calculating.set(false); this.snackBar.open('Erreur lors du calcul', 'Fermer', { duration: 4000 }); },
    });
  }

  editForm(): void { this.showForm.set(true); }
}
