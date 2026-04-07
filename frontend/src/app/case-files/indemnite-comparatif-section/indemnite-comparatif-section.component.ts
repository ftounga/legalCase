import { Component, Input, OnInit, signal, computed } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { IndemniteComparatifService } from '../../core/services/indemnite-comparatif.service';
import { IndemniteComparatifResponse } from '../../core/models/indemnite-comparatif.model';

@Component({
  selector: 'app-indemnite-comparatif-section',
  standalone: true,
  imports: [
    FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule,
    MatInputModule, MatProgressSpinnerModule,
  ],
  templateUrl: './indemnite-comparatif-section.component.html',
  styleUrl: './indemnite-comparatif-section.component.scss'
})
export class IndemniteComparatifSectionComponent implements OnInit {
  @Input() caseFileId!: string;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<IndemniteComparatifResponse | null>(null);

  country = signal('FRANCE');
  ancienneteAnnees = signal(5);
  age = signal(35);
  salaireMensuel = signal(3000);

  barMaxWidth = computed(() => {
    const r = this.result();
    if (!r) return 0;
    return r.baremePlafondMois;
  });

  constructor(
    private comparatifService: IndemniteComparatifService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.loadExisting();
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  loadExisting(): void {
    this.loading.set(true);
    this.comparatifService.get(this.caseFileId).subscribe({
      next: resp => {
        this.result.set(resp);
        this.prefillForm(resp);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.showForm.set(true);
        this.loading.set(false);
      },
    });
  }

  calculate(): void {
    this.calculating.set(true);
    this.comparatifService.calculate(this.caseFileId, {
      country: this.country(),
      ancienneteAnnees: this.ancienneteAnnees(),
      age: this.age(),
      salaireMensuel: this.salaireMensuel(),
    }).subscribe({
      next: resp => {
        this.result.set(resp);
        this.showForm.set(false);
        this.calculating.set(false);
      },
      error: () => {
        this.calculating.set(false);
        this.snackBar.open('Erreur lors du calcul', 'Fermer', { duration: 4000 });
      },
    });
  }

  editForm(): void {
    const r = this.result();
    if (r) this.prefillForm(r);
    this.showForm.set(true);
  }

  barWidth(mois: number): string {
    const max = this.barMaxWidth();
    if (!max || max === 0) return '0%';
    return Math.min(100, (mois / max) * 100) + '%';
  }

  private prefillForm(resp: IndemniteComparatifResponse): void {
    this.country.set(resp.country);
    this.ancienneteAnnees.set(resp.ancienneteAnnees);
    this.age.set(resp.age);
    this.salaireMensuel.set(resp.salaireMensuel);
  }
}
