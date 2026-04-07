import { Component, Input, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { ImmigrationWorkRightService } from '../../core/services/immigration-work-right.service';
import { WorkRightResponse } from '../../core/models/immigration-work-right.model';

@Component({
  selector: 'app-immigration-work-right-section',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './immigration-work-right-section.component.html',
  styleUrl: './immigration-work-right-section.component.scss'
})
export class ImmigrationWorkRightSectionComponent implements OnInit {
  @Input() caseFileId!: string;

  collapsed = signal(true);
  loading = signal(false);
  resolving = signal(false);
  showForm = signal(true);
  result = signal<WorkRightResponse | null>(null);

  titreType = signal('VLS_TS_SALARIE');
  country = signal('FRANCE');

  readonly titresFrance = [
    { value: 'VLS_TS_ETUDIANT', label: 'VLS-TS Étudiant' },
    { value: 'VLS_TS_SALARIE', label: 'VLS-TS Salarié' },
    { value: 'CST_SALARIE', label: 'CST Salarié' },
    { value: 'CARTE_PLURIANNUELLE', label: 'Carte pluriannuelle' },
    { value: 'CARTE_RESIDENT', label: 'Carte de résident' },
    { value: 'APS', label: 'APS' },
    { value: 'CST_VPF', label: 'Carte vie privée et familiale' },
    { value: 'RECEPISSE_ASILE', label: 'Récépissé demande d\'asile' },
  ];

  readonly titresBelgique = [
    { value: 'CARTE_A_TRAVAIL', label: 'Carte A — Travail' },
    { value: 'CARTE_A_ETUDES', label: 'Carte A — Études' },
    { value: 'CARTE_A_FAMILLE', label: 'Carte A — Famille' },
    { value: 'CARTE_B', label: 'Carte B' },
    { value: 'CARTE_C', label: 'Carte C' },
    { value: 'PERMIS_UNIQUE', label: 'Permis unique' },
    { value: 'ANNEXE_15', label: 'Annexe 15' },
    { value: 'ATTESTATION_IMMATRICULATION', label: 'Attestation d\'immatriculation' },
  ];

  readonly countries = [
    { value: 'FRANCE', label: 'France' },
    { value: 'BELGIQUE', label: 'Belgique' },
  ];

  get titresForCountry() {
    return this.country() === 'BELGIQUE' ? this.titresBelgique : this.titresFrance;
  }

  constructor(
    private workRightService: ImmigrationWorkRightService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.loadExisting();
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  onCountryChange(): void {
    this.titreType.set(this.titresForCountry[0].value);
  }

  loadExisting(): void {
    this.loading.set(true);
    this.workRightService.get(this.caseFileId).subscribe({
      next: resp => {
        this.result.set(resp);
        this.country.set(resp.country);
        this.titreType.set(resp.titreType);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.showForm.set(true);
        this.loading.set(false);
      },
    });
  }

  resolve(): void {
    this.resolving.set(true);
    this.workRightService.resolve(this.caseFileId, {
      titreType: this.titreType(),
      country: this.country(),
    }).subscribe({
      next: resp => {
        this.result.set(resp);
        this.showForm.set(false);
        this.resolving.set(false);
      },
      error: () => {
        this.resolving.set(false);
        this.snackBar.open('Erreur lors de l\'analyse', 'Fermer', { duration: 4000 });
      },
    });
  }

  editForm(): void {
    const r = this.result();
    if (r) {
      this.country.set(r.country);
      this.titreType.set(r.titreType);
    }
    this.showForm.set(true);
  }
}
