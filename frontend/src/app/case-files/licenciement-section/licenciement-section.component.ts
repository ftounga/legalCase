import { Component, Input, OnInit, signal, computed } from '@angular/core';
import { TitleCasePipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { LicenciementService } from '../../core/services/licenciement.service';
import { LicenciementResponse } from '../../core/models/licenciement.model';

interface CritereForm {
  code: string;
  label: string;
  description: string;
  reponse: string;
}

@Component({
  selector: 'app-licenciement-section',
  standalone: true,
  imports: [
    FormsModule, TitleCasePipe,
    MatButtonModule, MatIconModule,
    MatSelectModule, MatFormFieldModule,
    MatProgressSpinnerModule, MatRadioModule,
  ],
  templateUrl: './licenciement-section.component.html',
  styleUrl: './licenciement-section.component.scss'
})
export class LicenciementSectionComponent implements OnInit {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: string = 'FRANCE';

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<LicenciementResponse | null>(null);

  country = signal('FRANCE');
  criteresForm = signal<CritereForm[]>([]);

  readonly criteresReferentiel: Record<string, CritereForm[]> = {
    FRANCE: [
      { code: 'FR_CONVOCATION', label: 'Convocation entretien préalable', description: 'LRAR ou remise en main propre, 5 jours ouvrables', reponse: 'INCONNU' },
      { code: 'FR_ENTRETIEN', label: 'Tenue entretien préalable', description: 'Entretien effectué, possibilité d\'assistance', reponse: 'INCONNU' },
      { code: 'FR_DELAI_NOTIFICATION', label: 'Délai de notification', description: '2 jours ouvrables après entretien (7j cadre)', reponse: 'INCONNU' },
      { code: 'FR_MOTIVATION', label: 'Motivation de la lettre', description: 'Motifs précis et matériellement vérifiables', reponse: 'INCONNU' },
      { code: 'FR_MOTIF_REEL', label: 'Motif réel et sérieux', description: 'Objectif, exact et suffisamment grave', reponse: 'INCONNU' },
      { code: 'FR_PROCEDURE_DISCIPLINAIRE', label: 'Procédure disciplinaire', description: 'Faits < 2 mois, pas de double sanction', reponse: 'INCONNU' },
      { code: 'FR_ORDRE_LICENCIEMENT', label: 'Ordre des licenciements', description: 'Critères ancienneté, charges, qualités (éco)', reponse: 'INCONNU' },
    ],
    BELGIQUE: [
      { code: 'BE_NOTIFICATION', label: 'Notification du licenciement', description: 'LRAR ou exploit d\'huissier', reponse: 'INCONNU' },
      { code: 'BE_PREAVIS', label: 'Délai de préavis', description: 'Selon ancienneté (loi 26/12/2013)', reponse: 'INCONNU' },
      { code: 'BE_MOTIVATION', label: 'Motivation (CCT 109)', description: 'Comportement, aptitude ou nécessité entreprise', reponse: 'INCONNU' },
      { code: 'BE_AUDITION', label: 'Audition préalable', description: 'Recommandée (non obligatoire sauf CCE)', reponse: 'INCONNU' },
      { code: 'BE_NON_DISCRIMINATION', label: 'Absence discrimination', description: 'Pas de critère protégé', reponse: 'INCONNU' },
      { code: 'BE_PROTECTION_SPECIALE', label: 'Absence protection spéciale', description: 'Délégué syndical, grossesse, crédit-temps', reponse: 'INCONNU' },
      { code: 'BE_INDEMNITE_MANIFESTE', label: 'Risque licenciement déraisonnable', description: 'Indemnité 3-17 semaines (CCT 109)', reponse: 'INCONNU' },
    ]
  };

  scoreColor = computed(() => {
    const r = this.result();
    if (!r) return '#6B7A8D';
    if (r.scoreRisque < 15) return '#27AE60';
    if (r.scoreRisque < 40) return '#F59E0B';
    if (r.scoreRisque < 70) return '#E67E22';
    return '#C0392B';
  });

  constructor(
    private licenciementService: LicenciementService,
    private snackBar: MatSnackBar,
  ) {}

  ngOnInit(): void {
    this.country.set(this.workspaceCountry);
    this.criteresForm.set(this.criteresReferentiel[this.country()] || this.criteresReferentiel['FRANCE']);
    this.loadExisting();
  }

  toggleCollapsed(): void {
    this.collapsed.update(v => !v);
  }

  onCountryChange(): void {
    this.criteresForm.set(
      (this.criteresReferentiel[this.country()] || []).map(c => ({ ...c, reponse: 'INCONNU' }))
    );
  }

  loadExisting(): void {
    this.loading.set(true);
    this.licenciementService.get(this.caseFileId).subscribe({
      next: resp => {
        this.result.set(resp);
        this.country.set(resp.country);
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

  analyze(): void {
    this.analyzing.set(true);
    const reponses: Record<string, string> = {};
    for (const c of this.criteresForm()) {
      reponses[c.code] = c.reponse;
    }
    this.licenciementService.analyze(this.caseFileId, {
      country: this.country(),
      reponses,
    }).subscribe({
      next: resp => {
        this.result.set(resp);
        this.showForm.set(false);
        this.analyzing.set(false);
      },
      error: () => {
        this.analyzing.set(false);
        this.snackBar.open('Erreur lors de l\'analyse', 'Fermer', { duration: 4000 });
      },
    });
  }

  editForm(): void {
    const r = this.result();
    if (r) this.prefillForm(r);
    this.showForm.set(true);
  }

  private prefillForm(resp: LicenciementResponse): void {
    const form = (this.criteresReferentiel[resp.country] || []).map(c => {
      const found = resp.criteres.find(rc => rc.code === c.code);
      return { ...c, reponse: found ? found.reponse : 'INCONNU' };
    });
    this.criteresForm.set(form);
  }
}
