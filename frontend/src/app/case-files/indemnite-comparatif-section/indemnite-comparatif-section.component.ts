import { Component, Input, OnInit, OnChanges, SimpleChanges, signal, computed } from '@angular/core';
import { CaseAnalysisResult, TravailExtractedData } from '../../core/models/case-analysis.model';
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

interface TypeRuptureOption {
  value: string;
  label: string;
}

const TYPES_FR: TypeRuptureOption[] = [
  { value: 'LICENCIEMENT', label: 'Licenciement (cause réelle et sérieuse)' },
  { value: 'LICENCIEMENT_ECONOMIQUE', label: 'Licenciement économique' },
  { value: 'RUPTURE_CONVENTIONNELLE', label: 'Rupture conventionnelle homologuée' },
];
const TYPES_BE: TypeRuptureOption[] = [
  { value: 'LICENCIEMENT_ORDINAIRE', label: 'Licenciement ordinaire' },
  { value: 'RUPTURE_AMIABLE', label: 'Rupture amiable' },
];

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
export class IndemniteComparatifSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() aiData?: TravailExtractedData | null;
  @Input() synthesis?: CaseAnalysisResult | null;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<IndemniteComparatifResponse | null>(null);

  country = signal('FRANCE');
  typeRupture = signal<string>('LICENCIEMENT');
  typeRuptureNote = signal<string | null>(null);
  ancienneteAnnees = signal(5);
  age = signal(35);
  salaireMensuel = signal(3000);

  typeRuptureOptions = computed<TypeRuptureOption[]>(() =>
    this.country() === 'BELGIQUE' ? TYPES_BE : TYPES_FR
  );

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

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['aiData'] || changes['synthesis']) && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  onCountryChange(): void {
    // Reset type if incompatible with new country
    const allowed = this.typeRuptureOptions().map(o => o.value);
    if (!allowed.includes(this.typeRupture())) {
      this.typeRupture.set(allowed[0]);
    }
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
        this.prefillFromAi();
        this.showForm.set(true);
        this.loading.set(false);
      },
    });
  }

  calculate(): void {
    this.calculating.set(true);
    this.comparatifService.calculate(this.caseFileId, {
      country: this.country(),
      typeRupture: this.typeRupture(),
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
    if (resp.typeRupture) {
      this.typeRupture.set(resp.typeRupture);
    } else {
      // Legacy result sans type — fallback par défaut selon pays
      this.typeRupture.set(resp.country === 'BELGIQUE' ? 'LICENCIEMENT_ORDINAIRE' : 'LICENCIEMENT');
    }
  }

  private prefillFromAi(): void {
    if (this.aiData?.salaireBrutMensuel) {
      this.salaireMensuel.set(this.aiData.salaireBrutMensuel);
    }
    this.applyTypeRupturePrefill();
  }

  private applyTypeRupturePrefill(): void {
    const iaType = this.synthesis?.compensationEstimate?.typeRupture;
    if (!iaType) {
      this.typeRuptureNote.set(null);
      return;
    }
    const allowed = this.typeRuptureOptions().map(o => o.value);
    if (allowed.includes(iaType)) {
      this.typeRupture.set(iaType);
      this.typeRuptureNote.set(null);
    } else {
      // IA détecte autre chose (démission, prise d'acte, ou type de l'autre pays)
      this.typeRupture.set(allowed[0]);
      this.typeRuptureNote.set(
        `L'IA a détecté un type "${iaType}" non couvert par cet outil. Vérifier que le comparateur est adapté.`
      );
    }
  }
}
