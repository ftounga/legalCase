import { Component, Input, OnInit, OnChanges, SimpleChanges, signal } from '@angular/core';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { FormsModule } from '@angular/forms';
import { ImmigrationTitleDecisionService } from '../../core/services/immigration-title-decision.service';
import { TitleDecisionResponse, TitleRecommendation } from '../../core/models/immigration-title-decision.model';

const CODE_TO_MOTIF: Record<string, string> = {
  VLS_TS_ETUDIANT: 'ETUDES',
  CARTE_A_ETUDES: 'ETUDES',
  VLS_TS_SALARIE: 'TRAVAIL',
  CST_SALARIE: 'TRAVAIL',
  CARTE_PLURIANNUELLE: 'TRAVAIL',
  APS: 'TRAVAIL',
  CARTE_A_TRAVAIL: 'TRAVAIL',
  PERMIS_UNIQUE: 'TRAVAIL',
  CST_VPF: 'FAMILLE',
  CARTE_A_FAMILLE: 'FAMILLE',
  RECEPISSE_ASILE: 'ASILE',
  ATTESTATION_IMMATRICULATION: 'ASILE',
  ANNEXE_15: 'ASILE',
  // CARTE_RESIDENT, CARTE_B, CARTE_C : titres génériques stables, pas de mapping motif
};

@Component({
  selector: 'app-immigration-title-decision-section',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule,
    MatProgressSpinnerModule, MatSlideToggleModule,
  ],
  templateUrl: './immigration-title-decision-section.component.html',
  styleUrl: './immigration-title-decision-section.component.scss'
})
export class ImmigrationTitleDecisionSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() aiData?: ImmigrationExtractedData | null;

  collapsed = signal(true);
  loading = signal(false);
  resolving = signal(false);
  showForm = signal(true);
  decision = signal<TitleDecisionResponse | null>(null);

  country = signal('FRANCE');
  nationaliteUe = signal(false);
  motif = signal('TRAVAIL');
  duree = signal('LONG_SEJOUR');
  situationFamiliale = signal<string | null>(null);

  readonly countries = [
    { value: 'FRANCE', label: 'France' },
    { value: 'BELGIQUE', label: 'Belgique' },
  ];

  readonly motifs = [
    { value: 'TRAVAIL', label: 'Travail' },
    { value: 'ETUDES', label: 'Études' },
    { value: 'FAMILLE', label: 'Famille' },
    { value: 'ASILE', label: 'Asile' },
    { value: 'AUTRE', label: 'Autre' },
  ];

  readonly durees = [
    { value: 'COURT_SEJOUR', label: 'Court séjour (< 1 an)' },
    { value: 'LONG_SEJOUR', label: 'Long séjour (≥ 1 an)' },
  ];

  readonly situations = [
    { value: 'CELIBATAIRE', label: 'Célibataire' },
    { value: 'MARIE', label: 'Marié(e)' },
    { value: 'PACS_COHABITATION', label: 'PACS / Cohabitation légale' },
  ];

  constructor(
    private decisionService: ImmigrationTitleDecisionService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.loadExisting();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aiData'] && this.showForm() && !this.decision()) {
      this.prefillFromAi();
    }
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  onMotifChange(): void {
    if (this.motif() !== 'FAMILLE') {
      this.situationFamiliale.set(null);
    }
  }

  loadExisting(): void {
    this.loading.set(true);
    this.decisionService.get(this.caseFileId).subscribe({
      next: resp => {
        this.decision.set(resp);
        this.prefillForm(resp);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.prefillFromAi();
        this.showForm.set(true);
        this.loading.set(false);
      },
    });
  }

  private prefillFromAi(): void {
    if (!this.aiData) return;

    // 1. Nationalité UE depuis l'IA
    if (typeof this.aiData.nationaliteUe === 'boolean') {
      this.nationaliteUe.set(this.aiData.nationaliteUe);
    }

    // 2. Motif : priorité au code normalisé, fallback heuristique texte libre
    const code = this.aiData.typeTitreSejourCode?.toUpperCase();
    if (code && CODE_TO_MOTIF[code]) {
      this.motif.set(CODE_TO_MOTIF[code]);
      return;
    }
    if (this.aiData.typeTitreSejour) {
      const type = this.aiData.typeTitreSejour
        .toUpperCase()
        .normalize('NFD').replace(/[\u0300-\u036f]/g, ''); // strip accents
      if (type.includes('ETUDIANT') || type.includes('STUDENT')) this.motif.set('ETUDES');
      else if (type.includes('SALARIE') || type.includes('TRAVAIL')) this.motif.set('TRAVAIL');
      else if (type.includes('FAMILLE') || type.includes('VPF')) this.motif.set('FAMILLE');
      else if (type.includes('ASILE') || type.includes('REFUGIE')) this.motif.set('ASILE');
    }
  }

  resolve(): void {
    this.resolving.set(true);
    this.decisionService.resolve(this.caseFileId, {
      country: this.country(),
      nationaliteUe: this.nationaliteUe(),
      motif: this.motif(),
      duree: this.duree(),
      situationFamiliale: this.motif() === 'FAMILLE' ? this.situationFamiliale() : null,
    }).subscribe({
      next: resp => {
        this.decision.set(resp);
        this.showForm.set(false);
        this.resolving.set(false);
      },
      error: () => {
        this.resolving.set(false);
        this.snackBar.open('Erreur lors de l\'analyse', 'Fermer', { duration: 4000 });
      },
    });
  }

  editCriteria(): void {
    const d = this.decision();
    if (d) this.prefillForm(d);
    this.showForm.set(true);
  }

  private prefillForm(resp: TitleDecisionResponse): void {
    this.country.set(resp.country);
    this.nationaliteUe.set(resp.nationaliteUe);
    this.motif.set(resp.motif);
    this.duree.set(resp.duree);
    this.situationFamiliale.set(resp.situationFamiliale);
  }
}
