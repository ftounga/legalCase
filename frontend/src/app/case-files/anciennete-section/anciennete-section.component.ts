import { Component, Input, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { AncienneteService } from '../../core/services/anciennete.service';
import { AncienneteResponse } from '../../core/models/anciennete.model';

@Component({
  selector: 'app-anciennete-section',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule,
    MatInputModule, MatProgressSpinnerModule,
  ],
  templateUrl: './anciennete-section.component.html',
  styleUrl: './anciennete-section.component.scss'
})
export class AncienneteSectionComponent implements OnInit {
  @Input() caseFileId!: string;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<AncienneteResponse | null>(null);

  conventionCode = signal('METALLURGIE');
  dateEntree = signal('');
  salaireBase = signal(0);
  congesContrat = signal(25);
  primeContrat = signal(0);

  readonly conventionsFrance = [
    { value: 'METALLURGIE', label: 'Métallurgie (IDCC 3248)' },
    { value: 'COMMERCE', label: 'Commerce de détail (IDCC 2216)' },
    { value: 'BTP', label: 'BTP (IDCC 1596)' },
    { value: 'HCR', label: 'Hôtels, cafés, restaurants (IDCC 1979)' },
    { value: 'SYNTEC', label: 'Syntec (IDCC 1486)' },
  ];

  readonly conventionsBelgique = [
    { value: 'CP200', label: 'CP 200 — Employés' },
    { value: 'CP124', label: 'CP 124 — Construction' },
    { value: 'CP302', label: 'CP 302 — Hôtellerie' },
  ];

  get allConventions() {
    return [
      { group: 'France', items: this.conventionsFrance },
      { group: 'Belgique', items: this.conventionsBelgique },
    ];
  }

  constructor(
    private ancienneteService: AncienneteService,
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
    this.ancienneteService.get(this.caseFileId).subscribe({
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
    this.ancienneteService.calculate(this.caseFileId, {
      conventionCode: this.conventionCode(),
      dateEntree: this.dateEntree(),
      salaireBase: this.salaireBase(),
      congesContrat: this.congesContrat(),
      primeContrat: this.primeContrat(),
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

  private prefillForm(resp: AncienneteResponse): void {
    this.conventionCode.set(resp.conventionCode);
  }
}
