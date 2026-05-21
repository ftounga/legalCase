import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  computed,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ContestationFiliationBeService } from '../../core/services/contestation-filiation-be.service';
import {
  ContestationFiliationBeRequest,
  ContestationFiliationBeResponse,
  ContestationFiliationBeVerdict,
  DelaiStatutBe,
  NatureActionFiliationBe,
  QualiteDemandeurFiliationBe,
  VoieProceduraleFiliationBe,
} from '../../core/models/contestation-filiation-be.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ContestationFiliationBeSectionPrefillRules } from './contestation-filiation-be-section-prefill-rules';

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

interface NatureOption { value: NatureActionFiliationBe; label: string; }
interface QualiteOption { value: QualiteDemandeurFiliationBe; label: string; }

/**
 * F-217 SF-217-19 : composant Angular standalone pour l'outil décisionnel
 * "Contestation de filiation BE" (`contestation-filiation-be`).
 * BELGIQUE uniquement.
 *
 * Pattern de référence : `succession-be-acceptation-renonciation-section`
 * (F-217 SF-217-13) — composant complet (form + verdict + délai + motifs +
 * actions). Visibilité CATALOG (activation manuelle via F-238 catalogue) —
 * pas d'extraction IA en V1.
 *
 * - Form : nature de l'action (paternité présumée du mari / paternité reconnue
 *   volontairement — `MATERNITE` hors scope V1), qualité du demandeur, date
 *   de naissance de l'enfant, date de connaissance du fait, possession d'état
 *   conforme + durée (conditionnelle), expertise ADN disponible / envisagée,
 *   commentaire.
 * - Verdict 6 niveaux (ACTION_RECEVABLE / ACTION_RECEVABLE_DELAI_CRITIQUE /
 *   IRRECEVABLE_DELAI_DEPASSE / IRRECEVABLE_QUALITE_A_AGIR /
 *   IRRECEVABLE_POSSESSION_ETAT / QUALIFICATION_INCOMPLETE). Rouge réservé
 *   aux verdicts d'irrecevabilité.
 * - Affichage : verdict, voie procédurale, date limite + jours restants +
 *   statut coloré, motifs d'irrecevabilité, actions concrètes, bases juridiques.
 * - Pré-fill IA V1 = 0 champ (`PREFILL_COUNT_ALWAYS_ZERO = true`).
 * - Refresh dashboard F-IA-02 + MatSnackBar erreurs.
 * - Gate `workspaceCountry === 'BELGIQUE'` — bannière info si mismatch.
 * - OnPush + signals + `markForCheck()` dans `next:` / `error:`.
 */
@Component({
  selector: 'app-contestation-filiation-be-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
  ],
  templateUrl: './contestation-filiation-be-section.component.html',
  styleUrl: './contestation-filiation-be-section.component.scss',
})
export class ContestationFiliationBeSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'CONTESTATION DE FILIATION — CC art. 318 BE';
  static readonly TOOL_ICON = 'family_restroom';

  /**
   * F-217 SF-217-19 — V1 : aucun champ pré-rempli par l'IA. Pattern documenté
   * `PREFILL_COUNT_ALWAYS_ZERO = true` (cf. `SF-217-00-coherence.md`).
   */
  static readonly PREFILL_COUNT_ALWAYS_ZERO = true;
  static getPrefillCount(input: {
    aiData?: unknown;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return ContestationFiliationBeSectionPrefillRules.computePrefillCount(input);
  }

  /**
   * Natures supportées V1 — `MATERNITE` est exposée dans le model TypeScript
   * pour évolutivité, mais NON sélectionnable dans le formulaire (hors scope
   * V1). Si l'avocat soumet `MATERNITE` via API directe → backend 400.
   */
  static readonly NATURE_OPTIONS: readonly NatureOption[] = [
    { value: 'PATERNITE_PRESUMEE_MARI', label: 'Paternité présumée du mari' },
    { value: 'PATERNITE_RECONNUE_VOLONTAIRE', label: 'Paternité reconnue volontairement' },
  ];
  readonly natureOptions = ContestationFiliationBeSectionComponent.NATURE_OPTIONS;

  static readonly QUALITE_OPTIONS: readonly QualiteOption[] = [
    { value: 'ENFANT_MAJEUR', label: 'Enfant majeur' },
    { value: 'ENFANT_MINEUR_REPRESENTE', label: 'Enfant mineur représenté' },
    { value: 'MERE', label: 'Mère' },
    { value: 'PERE_PRESUME_OU_RECONNAISSANT', label: 'Père présumé ou reconnaissant' },
    { value: 'TIERS_PRETENDANT_PERE_BIOLOGIQUE', label: 'Tiers prétendant être le père biologique' },
    { value: 'MINISTERE_PUBLIC', label: 'Ministère public' },
  ];
  readonly qualiteOptions = ContestationFiliationBeSectionComponent.QUALITE_OPTIONS;

  @Input() caseFileId!: string;
  /** F-177 — force l'expansion (mode modal). */
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: unknown[] | null;
  @Input() aiQuestions?: unknown[] | null;
  @Input() piecesManquantes?: unknown[] | null;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<ContestationFiliationBeResponse | null>(null);

  // --- Form fields ---
  natureActionFiliation = signal<NatureActionFiliationBe | null>(null);
  qualiteDemandeur = signal<QualiteDemandeurFiliationBe | null>(null);
  dateNaissanceEnfant = signal<string | null>(null);
  dateConnaissanceFaitContestation = signal<string | null>(null);
  possessionEtatConforme = signal<boolean>(false);
  dureePossessionEtatAnnees = signal<number>(0);
  expertiseAdnDisponible = signal<boolean>(false);
  demandeExpertiseAdnEnvisagee = signal<boolean>(false);
  commentaire = signal<string | null>(null);

  /** Bloc durée affiché uniquement si possession d'état cochée. */
  showDureePossession = computed(() => this.possessionEtatConforme());

  constructor(
    private service: ContestationFiliationBeService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    if (this.workspaceCountry === 'BELGIQUE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onPossessionEtatChange(value: boolean): void {
    this.possessionEtatConforme.set(value);
    if (!value) {
      this.dureePossessionEtatAnnees.set(0);
    }
  }

  /**
   * Form valide si workspace BE, nature de l'action, qualité, dates ISO,
   * cohérence connaissance ≥ naissance.
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    if (!this.natureActionFiliation()) return false;
    if (!this.qualiteDemandeur()) return false;
    const naissance = this.dateNaissanceEnfant();
    if (!naissance || !ISO_DATE_REGEX.test(naissance)) return false;
    const connaissance = this.dateConnaissanceFaitContestation();
    if (!connaissance || !ISO_DATE_REGEX.test(connaissance)) return false;
    if (connaissance < naissance) return false;
    if (this.possessionEtatConforme()) {
      const duree = this.dureePossessionEtatAnnees();
      if (duree < 0 || duree > 100) return false;
    }
    return true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: ContestationFiliationBeRequest = {
      natureActionFiliation: this.natureActionFiliation()!,
      qualiteDemandeur: this.qualiteDemandeur()!,
      dateNaissanceEnfant: this.dateNaissanceEnfant()!,
      dateConnaissanceFaitContestation: this.dateConnaissanceFaitContestation()!,
      possessionEtatConforme: this.possessionEtatConforme(),
      dureePossessionEtatAnnees: this.possessionEtatConforme()
        ? this.dureePossessionEtatAnnees() : 0,
      expertiseAdnDisponible: this.expertiseAdnDisponible(),
      demandeExpertiseAdnEnvisagee: this.demandeExpertiseAdnEnvisagee(),
      commentaire: this.commentaire()?.trim() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de la contestation de filiation calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: ContestationFiliationBeResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  private hydrateForm(r: ContestationFiliationBeResponse): void {
    this.natureActionFiliation.set(r.natureActionFiliation);
    this.qualiteDemandeur.set(r.qualiteDemandeur);
    this.dateNaissanceEnfant.set(r.dateNaissanceEnfant);
    this.dateConnaissanceFaitContestation.set(r.dateConnaissanceFaitContestation);
    this.possessionEtatConforme.set(!!r.possessionEtatConforme);
    this.dureePossessionEtatAnnees.set(r.dureePossessionEtatAnnees ?? 0);
    this.expertiseAdnDisponible.set(!!r.expertiseAdnDisponible);
    this.demandeExpertiseAdnEnvisagee.set(!!r.demandeExpertiseAdnEnvisagee);
    this.commentaire.set(r.commentaire);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Rouge réservé aux verdicts d'irrecevabilité : IRRECEVABLE_DELAI_DEPASSE,
   * IRRECEVABLE_QUALITE_A_AGIR, IRRECEVABLE_POSSESSION_ETAT. Orange pour
   * ACTION_RECEVABLE_DELAI_CRITIQUE. Vert pour ACTION_RECEVABLE. Navy info
   * pour QUALIFICATION_INCOMPLETE.
   */
  verdictBannerClass(verdict: ContestationFiliationBeVerdict): string {
    switch (verdict) {
      case 'ACTION_RECEVABLE':
        return 'cfb-verdict-banner cfb-verdict-banner--ok';
      case 'ACTION_RECEVABLE_DELAI_CRITIQUE':
        return 'cfb-verdict-banner cfb-verdict-banner--medium';
      case 'IRRECEVABLE_DELAI_DEPASSE':
      case 'IRRECEVABLE_QUALITE_A_AGIR':
      case 'IRRECEVABLE_POSSESSION_ETAT':
        return 'cfb-verdict-banner cfb-verdict-banner--danger';
      case 'QUALIFICATION_INCOMPLETE':
        return 'cfb-verdict-banner cfb-verdict-banner--info';
    }
  }

  verdictBannerLabel(verdict: ContestationFiliationBeVerdict): string {
    switch (verdict) {
      case 'ACTION_RECEVABLE': return 'Action recevable';
      case 'ACTION_RECEVABLE_DELAI_CRITIQUE': return 'Action recevable — délai critique';
      case 'IRRECEVABLE_DELAI_DEPASSE': return 'Action irrecevable — délai dépassé';
      case 'IRRECEVABLE_QUALITE_A_AGIR': return 'Action irrecevable — qualité à agir';
      case 'IRRECEVABLE_POSSESSION_ETAT': return 'Action irrecevable — possession d\'état';
      case 'QUALIFICATION_INCOMPLETE': return 'Qualification incomplète';
    }
  }

  verdictBannerIcon(verdict: ContestationFiliationBeVerdict): string {
    switch (verdict) {
      case 'ACTION_RECEVABLE': return 'check_circle';
      case 'ACTION_RECEVABLE_DELAI_CRITIQUE': return 'warning';
      case 'IRRECEVABLE_DELAI_DEPASSE':
      case 'IRRECEVABLE_QUALITE_A_AGIR':
      case 'IRRECEVABLE_POSSESSION_ETAT':
        return 'error';
      case 'QUALIFICATION_INCOMPLETE': return 'info';
    }
  }

  /** Couleur du statut du délai. Rouge pour CRITIQUE / DEPASSE. */
  delaiStatutClass(statut: DelaiStatutBe | null | undefined): string {
    if (!statut) return 'cfb-delai-statut';
    switch (statut) {
      case 'OK': return 'cfb-delai-statut cfb-delai-statut--ok';
      case 'CRITIQUE':
      case 'DEPASSE':
        return 'cfb-delai-statut cfb-delai-statut--danger';
    }
  }

  delaiStatutLabel(statut: DelaiStatutBe | null | undefined): string {
    if (!statut) return '—';
    switch (statut) {
      case 'OK': return 'Délai OK';
      case 'CRITIQUE': return 'Délai critique';
      case 'DEPASSE': return 'Délai dépassé';
    }
  }

  voieProceduraleLabel(voie: VoieProceduraleFiliationBe): string {
    switch (voie) {
      case 'REQUETE_TRIBUNAL_FAMILLE':
        return 'Requête contradictoire au Tribunal de la famille';
      case 'AUCUNE_ACTION_RECEVABLE':
        return 'Aucune action recevable en l\'état';
    }
  }

  natureLabel(nature: NatureActionFiliationBe): string {
    const opt = this.natureOptions.find(o => o.value === nature);
    if (opt) return opt.label;
    if (nature === 'MATERNITE') return 'Maternité';
    return nature;
  }

  qualiteLabel(q: QualiteDemandeurFiliationBe): string {
    const opt = this.qualiteOptions.find(o => o.value === q);
    return opt?.label ?? q;
  }
}
