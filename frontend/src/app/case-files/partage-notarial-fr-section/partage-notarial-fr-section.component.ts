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
import { PartageNotarialFrService } from '../../core/services/partage-notarial-fr.service';
import {
  PartageNotarialRequest,
  PartageNotarialResponse,
} from '../../core/models/partage-notarial-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PartageNotarialFrPrefillRules } from './partage-notarial-fr-section-prefill-rules';

/**
 * SF-216-28 : composant Angular standalone pour l'outil décisionnel
 * "Partage successoral notarié" (F-FA-PARTAGE-NOTARIAL).
 *
 * FRANCE uniquement (art. 816 et s. Cciv + art. 870 Cciv + art. 1592 CGI
 * + art. 641 CGI + art. 840 Cciv).
 *
 * Pattern de référence : `donation-entre-epoux-fr-section` (SF-216-24).
 *
 * Pré-fill IA (4 champs F-246 + SF-216-27) :
 *  - dateOuvertureSuccession    ← `dateOuvertureSuccessionDetectee`
 *  - nombreCoheritiers          ← `nombreCoheritiersDetecte`
 *  - valeurMasseSuccessoraleEur ← `montantSuccessionEurDetecte`
 *  - presenceImmeuble           ← `presenceImmeubleSuccessionDetecte`
 *
 * Country gate : `workspaceCountry !== 'FRANCE'` → bannière info + aucun appel.
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-partage-notarial-fr-section',
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
  templateUrl: './partage-notarial-fr-section.component.html',
  styleUrl: './partage-notarial-fr-section.component.scss',
})
export class PartageNotarialFrSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'PARTAGE SUCCESSORAL NOTARIÉ';
  static readonly TOOL_ICON = 'gavel';

  /**
   * SF-216-28 — délégué au helper partagé (parité stricte avec prefillFromAi).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return PartageNotarialFrPrefillRules.computePrefillCount({
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
  result = signal<PartageNotarialResponse | null>(null);

  // --- Form fields (8 champs contrat backend SF-216-27) ---
  dateOuvertureSuccession = signal<string | null>(null);
  nombreCoheritiers = signal<number | null>(null);
  consentementsTousDetecte = signal<boolean>(false);
  presenceImmeuble = signal<boolean>(false);
  desaccordPersistant = signal<boolean>(false);
  valeurMasseSuccessoraleEur = signal<number | null>(null);
  notaireDesigne = signal<boolean>(false);
  declarationSuccessionEcheance = signal<string | null>(null);

  constructor(
    private service: PartageNotarialFrService,
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

  /** Form valide : FR + dateOuverture + nombreCoheritiers >= 1. */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (this.dateOuvertureSuccession() === null) return false;
    const n = this.nombreCoheritiers();
    if (n === null || n < 1) return false;
    return true;
  }

  // --- Handlers ---

  onDateOuvertureChange(value: string | null): void {
    this.dateOuvertureSuccession.set(value && value.trim() !== '' ? value : null);
  }

  onNombreChange(value: number | string | null): void {
    this.nombreCoheritiers.set(this.parsePositiveInt(value));
  }

  onConsentementsChange(checked: boolean): void {
    this.consentementsTousDetecte.set(checked);
  }

  onPresenceImmeubleChange(checked: boolean): void {
    this.presenceImmeuble.set(checked);
  }

  onDesaccordChange(checked: boolean): void {
    this.desaccordPersistant.set(checked);
  }

  onValeurChange(value: number | string | null): void {
    this.valeurMasseSuccessoraleEur.set(this.parseNonNegativeInt(value));
  }

  onNotaireDesigneChange(checked: boolean): void {
    this.notaireDesigne.set(checked);
  }

  onEcheanceChange(value: string | null): void {
    this.declarationSuccessionEcheance.set(value && value.trim() !== '' ? value : null);
  }

  private parsePositiveInt(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
    if (!Number.isFinite(num) || num < 1 || !Number.isInteger(num)) return null;
    return num;
  }

  private parseNonNegativeInt(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
    if (!Number.isFinite(num) || num < 0) return null;
    return num;
  }

  private applyPrefillFromAi(): void {
    const v = PartageNotarialFrPrefillRules.prefillFromAi({
      aiData: this.aiData ?? null,
      workspaceCountry: this.workspaceCountry,
    });
    if (v.dateOuvertureSuccession !== null && this.dateOuvertureSuccession() === null) {
      this.dateOuvertureSuccession.set(v.dateOuvertureSuccession);
    }
    if (v.nombreCoheritiers !== null && this.nombreCoheritiers() === null) {
      this.nombreCoheritiers.set(v.nombreCoheritiers);
    }
    if (v.valeurMasseSuccessoraleEur !== null && this.valeurMasseSuccessoraleEur() === null) {
      this.valeurMasseSuccessoraleEur.set(v.valeurMasseSuccessoraleEur);
    }
    if (v.presenceImmeuble !== null && this.presenceImmeuble() === false) {
      // Conserver l'éventuelle case déjà cochée par l'utilisateur ;
      // sinon pré-cocher avec le verdict IA.
      this.presenceImmeuble.set(v.presenceImmeuble);
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: PartageNotarialRequest = {
      dateOuvertureSuccession: this.dateOuvertureSuccession(),
      nombreCoheritiers: this.nombreCoheritiers(),
      consentementsTousDetecte: this.consentementsTousDetecte(),
      presenceImmeuble: this.presenceImmeuble(),
      desaccordPersistant: this.desaccordPersistant(),
      valeurMasseSuccessoraleEur: this.valeurMasseSuccessoraleEur(),
      notaireDesigne: this.notaireDesigne(),
      declarationSuccessionEcheance: this.declarationSuccessionEcheance(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Partage notarial évalué', 'OK', { duration: 2500 });
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

  private applyResult(r: PartageNotarialResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage
  // ---------------------------------------------------------------------------

  /** Classe CSS verdict. */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    if (r.orientationJudiciaire) return 'verdict-blocked';
    if (r.alerteDelai) return 'verdict-warning';
    return 'verdict-success';
  }

  verdictLabel(): string {
    const r = this.result();
    if (!r) return '';
    if (r.orientationJudiciaire) {
      return 'Bascule vers partage judiciaire requise (art. 840 Cciv)';
    }
    if (r.notaireObligatoire) {
      return 'Notaire obligatoire — partage amiable possible (art. 1592 CGI)';
    }
    return 'Partage amiable possible — notaire recommandé';
  }
}
