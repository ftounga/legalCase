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
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RecelSuccessionFrService } from '../../core/services/recel-succession-fr.service';
import {
  PreuveRecel,
  ReceleurQualite,
  RecelSuccessionRequest,
  RecelSuccessionResponse,
  TypeRecel,
} from '../../core/models/recel-succession-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { RecelSuccessionFrPrefillRules } from './recel-succession-fr-section-prefill-rules';

/**
 * SF-216-22 : composant Angular standalone pour l'outil décisionnel
 * "Recel de succession" (F-FA-RECEL-SUCCESSION).
 *
 * FRANCE uniquement (art. 778 Cciv + Cass. 1ère civ., 14/11/2012,
 * n° 11-20.582 — faisceau d'indices).
 *
 * Pattern de référence : `indignite-successorale-fr-section` (SF-216-20).
 *
 * Pré-fill IA (3 champs F-246) :
 *  - typeRecel                ← `typeRecelDetecte`
 *  - preuveRecel              ← `preuveRecelDetectee`
 *  - dateOuvertureSuccession  ← `dateOuvertureSuccessionDetectee` (réutilisé)
 *
 * Country gate : `workspaceCountry !== 'FRANCE'` → bannière info + aucun appel.
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-recel-succession-fr-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
  ],
  templateUrl: './recel-succession-fr-section.component.html',
  styleUrl: './recel-succession-fr-section.component.scss',
})
export class RecelSuccessionFrSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'RECEL DE SUCCESSION';
  static readonly TOOL_ICON = 'visibility_off';

  /**
   * SF-216-22 — délégué au helper partagé (parité stricte avec prefillFromAi).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return RecelSuccessionFrPrefillRules.computePrefillCount({
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
  result = signal<RecelSuccessionResponse | null>(null);

  // --- Form fields (6 champs contrat backend SF-216-21) ---
  typeRecel = signal<TypeRecel | null>(null);
  bienCeleValeurEur = signal<number | null>(null);
  preuveRecel = signal<PreuveRecel | null>(null);
  receleurQualite = signal<ReceleurQualite>('HERITIER');
  dateOuvertureSuccession = signal<string | null>(null);
  actionIntentee = signal<boolean>(false);

  /** Options select type de recel. */
  readonly typesRecel: ReadonlyArray<{ value: TypeRecel; label: string }> = [
    { value: 'DISSIMULATION_BIEN', label: 'Dissimulation d\'un bien (art. 778 Cciv)' },
    { value: 'DISSIMULATION_DONATION', label: 'Dissimulation d\'une donation (art. 778 + 850 Cciv)' },
    { value: 'DESTRUCTION_TESTAMENT', label: 'Destruction / recel du testament (art. 778 + 441-8 CP)' },
    { value: 'RECEL_CREANCE', label: 'Recel d\'une créance du défunt' },
    { value: 'AUTRE', label: 'Autre forme de recel (à motiver)' },
  ];

  /** Options select preuve. */
  readonly preuvesRecel: ReadonlyArray<{ value: PreuveRecel; label: string }> = [
    { value: 'AVEUX', label: 'Aveux du receleur (preuve forte)' },
    { value: 'DOCUMENT', label: 'Pièce écrite probante (relevé, acte, courriel)' },
    { value: 'EXPERTISE', label: 'Expertise (notariale, comptable, graphologique)' },
    { value: 'TEMOIGNAGE', label: 'Témoignage (attestation art. 202 CPC)' },
    { value: 'FAISCEAU_INDICES', label: 'Faisceau d\'indices (Cass. 14/11/2012)' },
    { value: 'AUCUNE', label: 'Aucune preuve disponible' },
  ];

  /** Options select qualité receleur. */
  readonly receleursQualite: ReadonlyArray<{ value: ReceleurQualite; label: string }> = [
    { value: 'HERITIER', label: 'Héritier (légal ou réservataire)' },
    { value: 'LEGATAIRE', label: 'Légataire (universel ou à titre universel)' },
    { value: 'DONATAIRE', label: 'Donataire rapportable' },
    { value: 'TIERS_COMPLICITE', label: 'Tiers complice (sanction art. 778 inapplicable)' },
  ];

  constructor(
    private service: RecelSuccessionFrService,
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
   * Form valide : FR + typeRecel + dateOuverture + preuveRecel renseignés.
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (this.typeRecel() === null) return false;
    if (this.preuveRecel() === null) return false;
    const d = this.dateOuvertureSuccession();
    if (!d || d.trim() === '') return false;
    return true;
  }

  // --- Handlers ---

  onTypeChange(value: TypeRecel | null): void {
    this.typeRecel.set(value);
  }

  onPreuveChange(value: PreuveRecel | null): void {
    this.preuveRecel.set(value);
  }

  onQualiteChange(value: ReceleurQualite): void {
    this.receleurQualite.set(value);
  }

  onValeurChange(value: number | string | null): void {
    this.bienCeleValeurEur.set(this.parseNonNegativeInt(value));
  }

  onDateOuvertureChange(value: string | null): void {
    this.dateOuvertureSuccession.set(value && value.trim() !== '' ? value : null);
  }

  onActionIntenteeChange(checked: boolean): void {
    this.actionIntentee.set(checked);
  }

  private parseNonNegativeInt(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
    if (!Number.isFinite(num) || num < 0) return null;
    return num;
  }

  private applyPrefillFromAi(): void {
    const v = RecelSuccessionFrPrefillRules.prefillFromAi({
      aiData: this.aiData ?? null,
      workspaceCountry: this.workspaceCountry,
    });
    if (v.typeRecel !== null && this.typeRecel() === null) {
      this.typeRecel.set(v.typeRecel);
    }
    if (v.preuveRecel !== null && this.preuveRecel() === null) {
      this.preuveRecel.set(v.preuveRecel);
    }
    if (v.dateOuvertureSuccession !== null && this.dateOuvertureSuccession() === null) {
      this.dateOuvertureSuccession.set(v.dateOuvertureSuccession);
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: RecelSuccessionRequest = {
      typeRecel: this.typeRecel(),
      bienCeleValeurEur: this.bienCeleValeurEur(),
      preuveRecel: this.preuveRecel(),
      receleurQualite: this.receleurQualite(),
      dateOuvertureSuccession: this.dateOuvertureSuccession(),
      actionIntentee: this.actionIntentee(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Recel successoral évalué', 'OK', { duration: 2500 });
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

  private applyResult(r: RecelSuccessionResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage
  // ---------------------------------------------------------------------------

  qualificationLabel(q: string): string {
    const labels: Record<string, string> = {
      CARACTERISE: 'Recel caractérisé (art. 778)',
      PROBABLE: 'Recel probable (faisceau / témoignage)',
      INSUFFISANT: 'Preuve insuffisante',
      HORS_PERIMETRE: 'Hors périmètre (tiers complice)',
      PAS_DE_RECEL: 'Pas de recel caractérisable',
    };
    return labels[q] ?? q;
  }

  /** Classe CSS verdict. */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    if (r.delaiForclos) return 'verdict-blocked';
    if (r.qualificationRecel === 'CARACTERISE') return 'verdict-success';
    if (r.qualificationRecel === 'PROBABLE') return 'verdict-warning';
    if (r.qualificationRecel === 'HORS_PERIMETRE') return 'verdict-blocked';
    return 'verdict-warning';
  }
}
