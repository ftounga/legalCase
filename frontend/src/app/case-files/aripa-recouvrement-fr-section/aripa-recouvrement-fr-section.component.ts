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
import { AripaRecouvrementFrService } from '../../core/services/aripa-recouvrement-fr.service';
import {
  AripaRecouvrementFrRequest,
  AripaRecouvrementFrResponse,
  SituationAripa,
  VoieRecouvrementAripa,
} from '../../core/models/aripa-recouvrement-fr.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { AripaRecouvrementFrPrefillRules } from './aripa-recouvrement-fr-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-216-08 : composant Angular standalone pour l'outil décisionnel "ARIPA
 * recouvrement pension alimentaire impayée" (F-FA-ARIPA-RECOUVREMENT).
 *
 * FRANCE uniquement (art. L. 581-1 à L. 581-14 CSS — recouvrement public via
 * ARIPA ; art. L. 582-1 CSS — saisie sur rémunération ; art. L. 523-1 CSS —
 * ASF complémentaire ; Convention La Haye 2007 + Règlement UE 4/2009 —
 * recouvrement international).
 *
 * Pattern de référence : `liquidation-communaute-fr-section` (SF-216-06) et
 * `prestation-compensatoire-section` (SF-216-02).
 *
 * Formulaire (8 champs) :
 *  - montantPensionMensuelleEur (entier ≥ 0, € / mois)
 *  - nombreMoisImpayes (entier ≥ 1)
 *  - situationCreancier (enum SituationAripa)
 *  - situationDebiteur (enum SituationAripa)
 *  - titreExecutoire (checkbox)
 *  - debiteurEnFrance (checkbox)
 *  - debiteurEmployeurPublic (checkbox)
 *  - nombreEnfantsACharge (entier ≥ 0)
 *
 * Verdict affiché :
 *  - voieRecommandee (5 voies — TITRE_REQUIS / SDR_ARIPA / SAISIE_SUR_SALAIRE /
 *    SATD / CONVENTION_INTERNATIONALE)
 *  - montantArrieres (pension × mois impayés)
 *  - montantAsfEligibleMensuelEur (complément ASF différentiel)
 *  - delaiEstimeJours
 *  - etapes (timeline)
 *  - baseLegale, messages, alertes
 *
 * Country gate : `workspaceCountry !== 'FRANCE'` → bannière info + aucun appel.
 *
 * OnPush + ChangeDetectorRef.markForCheck() dans next/error des subscribe
 * (mémoire `feedback_onpush_subscribe_markforcheck`).
 */
@Component({
  selector: 'app-aripa-recouvrement-fr-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './aripa-recouvrement-fr-section.component.html',
  styleUrl: './aripa-recouvrement-fr-section.component.scss',
})
export class AripaRecouvrementFrSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99e v4 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-ARIPA-RECOUVREMENT';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'ARIPA RECOUVREMENT';
  static readonly TOOL_ICON = 'payments';

  /**
   * SF-216-08 — délégué au helper partagé (parité stricte avec `prefillFromAi`).
   * Retourne le nombre de champs pré-remplissables FR (max 3 — pension, titre,
   * nb enfants).
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    workspaceCountry?: string;
  }): number {
    return AripaRecouvrementFrPrefillRules.computePrefillCount({
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
  result = signal<AripaRecouvrementFrResponse | null>(null);

  // --- Form fields (8 champs du contrat backend SF-216-07) ---
  montantPensionMensuelleEur = signal<number | null>(null);
  nombreMoisImpayes = signal<number | null>(null);
  situationCreancier = signal<SituationAripa | null>(null);
  situationDebiteur = signal<SituationAripa | null>(null);
  titreExecutoire = signal<boolean>(false);
  debiteurEnFrance = signal<boolean>(true);
  debiteurEmployeurPublic = signal<boolean>(false);
  nombreEnfantsACharge = signal<number | null>(null);

  // Provenance IA par champ (badge dans l'UI tant que valeur non modifiée).
  provenanceMontantPension = signal<'IA' | null>(null);
  provenanceTitreExecutoire = signal<'IA' | null>(null);
  provenanceNombreEnfants = signal<'IA' | null>(null);

  /** Options select situation (4 valeurs pour le créancier, 5 pour le débiteur). */
  readonly situationsCreancier: ReadonlyArray<{ value: SituationAripa; label: string }> = [
    { value: 'SALARIE', label: 'Salarié(e)' },
    { value: 'CHOMEUR', label: 'Demandeur d\'emploi' },
    { value: 'INDEPENDANT', label: 'Indépendant(e)' },
    { value: 'SANS_ACTIVITE', label: 'Sans activité' },
  ];
  readonly situationsDebiteur: ReadonlyArray<{ value: SituationAripa; label: string }> = [
    ...this.situationsCreancier,
    { value: 'INCONNU', label: 'Inconnue' },
  ];

  constructor(
    private service: AripaRecouvrementFrService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.prefillFromAi();
    if (this.workspaceCountry === 'FRANCE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * Pré-fill IA SF-216-08 — renseigne les 3 champs détectables depuis
   * `familleExtractedData` (montant pension, titre exécutoire, nb enfants).
   * No-op gracieux si `aiData` absent ou dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;

    const rules = AripaRecouvrementFrPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const pension = rules.computeMontantPensionMensuelle(ruleInput);
    if (pension !== null) {
      this.montantPensionMensuelleEur.set(pension);
      this.provenanceMontantPension.set('IA');
    }
    const titre = rules.computeTitreExecutoire(ruleInput);
    if (titre !== null) {
      this.titreExecutoire.set(titre);
      this.provenanceTitreExecutoire.set('IA');
    }
    const nbEnfants = rules.computeNombreEnfantsACharge(ruleInput);
    if (nbEnfants !== null) {
      this.nombreEnfantsACharge.set(nbEnfants);
      this.provenanceNombreEnfants.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /**
   * Form valide : FR + tous les champs requis renseignés + valeurs cohérentes.
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    const pension = this.montantPensionMensuelleEur();
    if (pension === null || !Number.isFinite(pension) || pension < 0) return false;
    const mois = this.nombreMoisImpayes();
    if (mois === null || !Number.isFinite(mois) || mois < 1) return false;
    if (this.situationCreancier() === null) return false;
    if (this.situationDebiteur() === null) return false;
    const nbEnfants = this.nombreEnfantsACharge();
    if (nbEnfants === null || !Number.isFinite(nbEnfants) || nbEnfants < 0) return false;
    return true;
  }

  // --- Handlers — toute modification manuelle efface le badge IA ---

  onMontantPensionChange(value: number | string | null): void {
    this.montantPensionMensuelleEur.set(this.parseNonNegativeInt(value));
    this.provenanceMontantPension.set(null);
  }

  onNombreMoisChange(value: number | string | null): void {
    this.nombreMoisImpayes.set(this.parseNonNegativeInt(value));
  }

  onSituationCreancierChange(value: SituationAripa | null): void {
    this.situationCreancier.set(value);
  }

  onSituationDebiteurChange(value: SituationAripa | null): void {
    this.situationDebiteur.set(value);
  }

  onTitreExecutoireChange(checked: boolean): void {
    this.titreExecutoire.set(checked);
    this.provenanceTitreExecutoire.set(null);
  }

  onDebiteurEnFranceChange(checked: boolean): void {
    this.debiteurEnFrance.set(checked);
  }

  onDebiteurEmployeurPublicChange(checked: boolean): void {
    this.debiteurEmployeurPublic.set(checked);
  }

  onNombreEnfantsChange(value: number | string | null): void {
    this.nombreEnfantsACharge.set(this.parseNonNegativeInt(value));
    this.provenanceNombreEnfants.set(null);
  }

  private parseNonNegativeInt(value: number | string | null | undefined): number | null {
    if (value === null || value === undefined || value === '') return null;
    const num = typeof value === 'number' ? value : Number.parseInt(String(value), 10);
    if (!Number.isFinite(num) || num < 0) return null;
    return num;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: AripaRecouvrementFrRequest = {
      montantPensionMensuelleEur: this.montantPensionMensuelleEur(),
      nombreMoisImpayes: this.nombreMoisImpayes(),
      situationCreancier: this.situationCreancier(),
      situationDebiteur: this.situationDebiteur(),
      titreExecutoire: this.titreExecutoire(),
      debiteurEnFrance: this.debiteurEnFrance(),
      debiteurEmployeurPublic: this.debiteurEmployeurPublic(),
      nombreEnfantsACharge: this.nombreEnfantsACharge(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Voie de recouvrement calculée', 'OK', { duration: 2500 });
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
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  private applyResult(r: AripaRecouvrementFrResponse): void {
    this.result.set(r);
    this.showForm.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  formatEuros(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
    }).format(amount);
  }

  /** Libellé humain pour la voie recommandée. */
  voieLabel(voie: VoieRecouvrementAripa): string {
    const labels: Record<VoieRecouvrementAripa, string> = {
      TITRE_REQUIS: 'Titre exécutoire requis',
      SDR_ARIPA: 'SDR ARIPA (CAF / MSA)',
      SAISIE_SUR_SALAIRE: 'Saisie sur salaire',
      SATD: 'SATD (employeur public)',
      CONVENTION_INTERNATIONALE: 'Convention internationale',
    };
    return labels[voie];
  }

  /** Couleur Material par voie pour la pastille verdict. */
  voieClass(voie: VoieRecouvrementAripa): string {
    const cls: Record<VoieRecouvrementAripa, string> = {
      TITRE_REQUIS: 'voie-warning',
      SDR_ARIPA: 'voie-primary',
      SAISIE_SUR_SALAIRE: 'voie-success',
      SATD: 'voie-info',
      CONVENTION_INTERNATIONALE: 'voie-info',
    };
    return cls[voie];
  }
}
