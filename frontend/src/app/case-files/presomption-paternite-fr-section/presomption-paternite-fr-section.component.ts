import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PresomptionPaterniteFrService } from '../../core/services/presomption-paternite-fr.service';
import {
  PresomptionPaterniteRequest,
  PresomptionPaterniteResponse,
} from '../../core/models/presomption-paternite-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PresomptionPaterniteFrPrefillRules } from './presomption-paternite-fr-section-prefill-rules';

/**
 * SF-216-26 : composant Angular standalone pour l'outil décisionnel
 * "Présomption de paternité du mari et désaveu" (F-FA-PRESOMPTION-PATERNITE).
 *
 * FRANCE uniquement (art. 312-315 Cciv + art. 316 al. 2 + art. 333 al. 1
 * + Cass. 1ère civ., 19/2/2014 — point de départ délai désaveu).
 *
 * Pattern de référence : `donation-entre-epoux-fr-section` (SF-216-24).
 *
 * Pré-fill IA (5 champs F-246 + SF-216-25) :
 *  - dateNaissanceEnfant            ← `dateNaissanceEnfantDetectee` (filiation_v2)
 *  - possessionEtatConformeDetecte  ← `possessionEtatConforme5AnsDetected` (filiation_v2)
 *  - dateConclusionMariage          ← `dateConclusionMariageDetectee` (SF-216-25)
 *  - dateDissolutionMariage         ← `dateDissolutionMariageDetectee` (SF-216-25)
 *  - desaveuEnvisage                ← `desaveuEnvisage` (SF-216-25)
 *
 * Country gate : `workspaceCountry !== 'FRANCE'` → bannière info + aucun appel.
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-presomption-paternite-fr-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
  ],
  templateUrl: './presomption-paternite-fr-section.component.html',
  styleUrl: './presomption-paternite-fr-section.component.scss',
})
export class PresomptionPaterniteFrSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'PRÉSOMPTION DE PATERNITÉ';
  static readonly TOOL_ICON = 'family_restroom';

  /**
   * SF-216-26 — délégué au helper partagé (parité stricte avec prefillFromAi).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return PresomptionPaterniteFrPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: FamilleExtractedData | null;
  /** Mode simulateur autonome (hors dossier) — coupe le refresh dashboard. */
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<PresomptionPaterniteResponse | null>(null);

  // --- Form fields (9 champs contrat backend SF-216-25) ---
  dateNaissanceEnfant = signal<string | null>(null);
  dateConclusionMariage = signal<string | null>(null);
  dateDissolutionMariage = signal<string | null>(null);
  dateAccouchement = signal<string | null>(null);
  conceptionEn180PremiersMoisMariage = signal<boolean>(false);
  enfantNeApresDisso = signal<boolean>(false);
  desaveuEnvisage = signal<boolean>(false);
  possessionEtatConformeDetecte = signal<boolean>(false);
  dateConnaissanceNaissance = signal<string | null>(null);

  constructor(
    private service: PresomptionPaterniteFrService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    if (this.workspaceCountry === 'FRANCE') {
      this.applyPrefillFromAi();
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData'] && this.workspaceCountry === 'FRANCE') {
      this.applyPrefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * Form valide : FR + dateNaissance + dateConclusionMariage renseignés.
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const dn = this.dateNaissanceEnfant();
    if (!dn || dn.trim() === '') return false;
    const dm = this.dateConclusionMariage();
    if (!dm || dm.trim() === '') return false;
    return true;
  }

  // --- Handlers ---

  onDateNaissanceChange(value: string | null): void {
    this.dateNaissanceEnfant.set(value && value.trim() !== '' ? value : null);
  }

  onDateMariageChange(value: string | null): void {
    this.dateConclusionMariage.set(value && value.trim() !== '' ? value : null);
  }

  onDateDissolutionChange(value: string | null): void {
    this.dateDissolutionMariage.set(value && value.trim() !== '' ? value : null);
  }

  onDateAccouchementChange(value: string | null): void {
    this.dateAccouchement.set(value && value.trim() !== '' ? value : null);
  }

  onDateConnaissanceChange(value: string | null): void {
    this.dateConnaissanceNaissance.set(value && value.trim() !== '' ? value : null);
  }

  onConception180Change(checked: boolean): void {
    this.conceptionEn180PremiersMoisMariage.set(checked);
  }

  onNeApresDissoChange(checked: boolean): void {
    this.enfantNeApresDisso.set(checked);
  }

  onDesaveuChange(checked: boolean): void {
    this.desaveuEnvisage.set(checked);
  }

  onPossessionEtatChange(checked: boolean): void {
    this.possessionEtatConformeDetecte.set(checked);
  }

  private applyPrefillFromAi(): void {
    const v = PresomptionPaterniteFrPrefillRules.prefillFromAi({
      aiData: this.aiData ?? null,
      workspaceCountry: this.workspaceCountry,
    });
    if (v.dateNaissanceEnfant !== null && this.dateNaissanceEnfant() === null) {
      this.dateNaissanceEnfant.set(v.dateNaissanceEnfant);
    }
    if (v.possessionEtatConformeDetecte !== null) {
      this.possessionEtatConformeDetecte.set(v.possessionEtatConformeDetecte);
    }
    if (v.dateConclusionMariage !== null && this.dateConclusionMariage() === null) {
      this.dateConclusionMariage.set(v.dateConclusionMariage);
    }
    if (v.dateDissolutionMariage !== null && this.dateDissolutionMariage() === null) {
      this.dateDissolutionMariage.set(v.dateDissolutionMariage);
    }
    if (v.desaveuEnvisage !== null) {
      this.desaveuEnvisage.set(v.desaveuEnvisage);
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: PresomptionPaterniteRequest = {
      dateNaissanceEnfant: this.dateNaissanceEnfant(),
      dateConclusionMariage: this.dateConclusionMariage(),
      dateDissolutionMariage: this.dateDissolutionMariage(),
      dateAccouchement: this.dateAccouchement(),
      conceptionEn180PremiersMoisMariage: this.conceptionEn180PremiersMoisMariage(),
      enfantNeApresDisso: this.enfantNeApresDisso(),
      desaveuEnvisage: this.desaveuEnvisage(),
      possessionEtatConformeDetecte: this.possessionEtatConformeDetecte(),
      dateConnaissanceNaissance: this.dateConnaissanceNaissance(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Présomption de paternité évaluée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: PresomptionPaterniteResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage
  // ---------------------------------------------------------------------------

  voieDesaveuLabel(v: string): string {
    const labels: Record<string, string> = {
      DESAVEU_RECEVABLE: 'Désaveu recevable (art. 316 al. 2)',
      DESAVEU_DELAI_FORCLOS: 'Délai de désaveu forclos',
      DESAVEU_DIFFICILE_POSSESSION_ETAT: 'Désaveu difficile (possession d\'état)',
      DESAVEU_SANS_OBJET: 'Désaveu sans objet (présomption écartée)',
      INDETERMINE: 'Non envisagé',
    };
    return labels[v] ?? v;
  }

  presomptionLabel(): string {
    const r = this.result();
    if (!r) return '';
    if (!r.presomptionApplicable && r.presomptionRenversee) {
      return 'Présomption écartée (art. 313)';
    }
    if (r.presomptionApplicable && r.presomptionRenversee) {
      return 'Présomption renversable (art. 313 al. 2)';
    }
    if (r.presomptionApplicable) {
      return 'Présomption applicable (art. 312)';
    }
    return 'Non déterminée';
  }

  /** Classe CSS verdict. */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    if (r.voieDesaveu === 'DESAVEU_DELAI_FORCLOS') return 'verdict-blocked';
    if (r.voieDesaveu === 'DESAVEU_DIFFICILE_POSSESSION_ETAT') return 'verdict-warning';
    if (r.voieDesaveu === 'DESAVEU_SANS_OBJET') return 'verdict-warning';
    if (r.presomptionApplicable && !r.presomptionRenversee) return 'verdict-success';
    return 'verdict-warning';
  }
}
