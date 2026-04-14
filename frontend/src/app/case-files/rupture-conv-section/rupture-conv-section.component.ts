import { Component, Input, OnInit, Optional, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RuptureConvService } from '../../core/services/rupture-conv.service';
import { RuptureConvResponse } from '../../core/models/rupture-conv.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

interface CritereForm {
  code: string;
  label: string;
  description: string;
  bloquant: boolean;
}

const CRITERES_FR: CritereForm[] = [
  { code: 'RC_CONSENTEMENT', label: 'Consentement libre et éclairé', bloquant: true,
    description: 'Le consentement du salarié est-il exempt de vice (pression, dol, violence morale, erreur) ? '
      + 'Des échanges, pièces ou circonstances laissent-ils présumer une contrainte ?' },
  { code: 'RC_DELAI_RETRACTATION', label: 'Délai de rétractation de 15 jours calendaires respecté', bloquant: true,
    description: 'Un délai de 15 jours calendaires a-t-il été respecté entre la signature de la convention et '
      + 'la demande d\'homologation adressée à l\'administration ?' },
  { code: 'RC_HOMOLOGATION', label: 'Homologation par la DREETS (ex-DIRECCTE)', bloquant: true,
    description: 'La demande d\'homologation a-t-elle été déposée et l\'homologation obtenue '
      + '(ou réputée acquise après 15 jours ouvrables sans réponse) ?' },
  { code: 'RC_ASSISTANCE', label: 'Assistance possible et documentée', bloquant: false,
    description: 'Le salarié a-t-il été informé de sa possibilité de se faire assister (avocat, conseiller du salarié, '
      + 'représentant du personnel) lors des entretiens, et cette assistance est-elle documentée ?' },
  { code: 'RC_INDEMNITE', label: 'Indemnité spécifique supérieure ou égale à l\'indemnité légale', bloquant: true,
    description: 'L\'indemnité spécifique de rupture conventionnelle est-elle au moins égale à l\'indemnité légale '
      + 'de licenciement (¼ de mois par année les 10 premières années + ⅓ au-delà) ?' },
  { code: 'RC_ENTRETIENS', label: 'Au moins un entretien préalable tenu et documenté', bloquant: false,
    description: 'Un entretien préalable a-t-il été organisé entre l\'employeur et le salarié avant signature, '
      + 'et existe-t-il une trace écrite (compte-rendu, correspondance) ?' },
];

type Reponse = 'OUI' | 'NON' | 'INCONNU';

@Component({
  selector: 'app-rupture-conv-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatRadioModule, MatTooltipModule,
  ],
  templateUrl: './rupture-conv-section.component.html',
  styleUrl: './rupture-conv-section.component.scss'
})
export class RuptureConvSectionComponent implements OnInit {
  @Input() caseFileId!: string;

  readonly criteres = CRITERES_FR;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  reponses = signal<Record<string, Reponse>>(this.defaultReponses());
  result = signal<RuptureConvResponse | null>(null);

  constructor(
    private rcService: RuptureConvService,
    private snackBar: MatSnackBar,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    this.loading.set(true);
    this.rcService.get(this.caseFileId).subscribe({
      next: resp => {
        this.result.set(resp);
        this.showForm.set(false);
        this.applyReponsesFromResult(resp);
        this.loading.set(false);
      },
      error: () => {
        this.showForm.set(true);
        this.loading.set(false);
      },
    });
  }

  toggleCollapsed(): void { this.collapsed.update(v => !v); }

  setReponse(code: string, value: Reponse): void {
    this.reponses.update(r => ({ ...r, [code]: value }));
  }

  analyze(): void {
    this.analyzing.set(true);
    this.rcService.analyze(this.caseFileId, {
      country: 'FRANCE',
      reponses: this.reponses(),
    }).subscribe({
      next: resp => {
        this.result.set(resp);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.refreshService?.triggerRefresh();
      },
      error: () => {
        this.analyzing.set(false);
        this.snackBar.open('Erreur lors de l\'analyse', 'Fermer', { duration: 4000 });
      },
    });
  }

  editForm(): void { this.showForm.set(true); }

  verdictColor(verdict: string | undefined): string {
    switch (verdict) {
      case 'VALIDE': return '#27AE60';
      case 'RISQUE_MODERE': return '#F59E0B';
      case 'RISQUE_ELEVE': return '#E67E22';
      case 'INVALIDE': return '#C0392B';
      default: return '#6B7A8D';
    }
  }

  verdictLabel(verdict: string | undefined): string {
    switch (verdict) {
      case 'VALIDE': return 'Valide';
      case 'RISQUE_MODERE': return 'Risque modéré';
      case 'RISQUE_ELEVE': return 'Risque élevé';
      case 'INVALIDE': return 'Invalide';
      default: return '';
    }
  }

  scoreBarWidth(score: number): string {
    return Math.max(0, Math.min(100, score)) + '%';
  }

  private defaultReponses(): Record<string, Reponse> {
    const r: Record<string, Reponse> = {};
    for (const c of CRITERES_FR) r[c.code] = 'INCONNU';
    return r;
  }

  private applyReponsesFromResult(resp: RuptureConvResponse): void {
    const r: Record<string, Reponse> = { ...this.defaultReponses() };
    for (const c of resp.criteres) {
      if (c.reponse === 'OUI' || c.reponse === 'NON' || c.reponse === 'INCONNU') {
        r[c.code] = c.reponse;
      }
    }
    this.reponses.set(r);
  }
}
