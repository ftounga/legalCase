import {
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
import { ProtectionMajeurBeService } from '../../core/services/protection-majeur-be.service';
import {
  ActeProtegeBeCode,
  GraviteIncapaciteBe,
  JuridictionProtectionBe,
  MesureProtectionBe,
  ModeSaisineProtectionBe,
  NatureAlterationBe,
  NecessiteActeBe,
  NiveauUrgenceProtectionBe,
  ProtectionMajeurBeRequest,
  ProtectionMajeurBeResponse,
  ProtectionMajeurBeVerdict,
} from '../../core/models/protection-majeur-be.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import { ProtectionMajeurBeSectionPrefillRules } from './protection-majeur-be-section-prefill-rules';

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

interface NatureOption { value: NatureAlterationBe; label: string; }
interface GraviteOption { value: GraviteIncapaciteBe; label: string; }
interface UrgenceOption { value: NiveauUrgenceProtectionBe; label: string; }
interface SaisineOption { value: ModeSaisineProtectionBe; label: string; }

/**
 * F-217 SF-217-15 : composant Angular standalone pour l'outil décisionnel
 * "Protection du majeur (Belgique)" (`protection-majeur-be`). BELGIQUE
 * uniquement.
 *
 * Pattern de référence : `succession-be-acceptation-renonciation-section`
 * (F-217 SF-217-13).
 *
 * - Form : nature de l'altération, gravité de l'incapacité, mandat
 *   extra-judiciaire (avec date conditionnelle), déclaration anticipée,
 *   environnement familial protecteur, niveau d'urgence, mode de saisine,
 *   commentaire.
 * - Verdict 4 niveaux (MANDAT_EXTRA_JUDICIAIRE_VALABLE /
 *   MESURE_PROTECTION_RECOMMANDEE / URGENCE_MESURE_PROVISOIRE /
 *   QUALIFICATION_INCOMPLETE). Rouge réservé à URGENCE_MESURE_PROVISOIRE.
 * - Affichage : verdict, mesure recommandée, juridiction, fondement
 *   procédural, liste des actes protégés colorée par nécessité, actions
 *   concrètes, bases juridiques.
 * - Pré-fill IA V1 = 0 champ (`PREFILL_COUNT_ALWAYS_ZERO = true`).
 * - Refresh dashboard F-IA-02 + MatSnackBar erreurs.
 * - Gate `workspaceCountry === 'BELGIQUE'` — bannière info si mismatch.
 */
@Component({
  selector: 'app-protection-majeur-be-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    ToolJurisprudenceCitationsComponent
  ],
  templateUrl: './protection-majeur-be-section.component.html',
  styleUrl: './protection-majeur-be-section.component.scss',
})
export class ProtectionMajeurBeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'protection-majeur-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = "PROTECTION DU MAJEUR — LOI 17/03/2013 BE";
  static readonly TOOL_ICON = 'shield_person';

  /**
   * F-217 SF-217-15 — V1 : aucun champ pré-rempli par l'IA. Pattern documenté
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
    return ProtectionMajeurBeSectionPrefillRules.computePrefillCount(input);
  }

  static readonly NATURE_OPTIONS: readonly NatureOption[] = [
    { value: 'MEDICALE_DURABLE', label: 'Médicale durable (démence, Alzheimer, AVC durable)' },
    { value: 'MEDICALE_TEMPORAIRE', label: 'Médicale temporaire (coma, accident transitoire)' },
    { value: 'PRODIGALITE', label: 'Prodigalité' },
    { value: 'AUTRE', label: 'Autre' },
  ];
  readonly natureOptions = ProtectionMajeurBeSectionComponent.NATURE_OPTIONS;

  static readonly GRAVITE_OPTIONS: readonly GraviteOption[] = [
    { value: 'INCAPACITE_GERER_BIENS', label: 'Incapacité de gérer les biens uniquement' },
    { value: 'INCAPACITE_GERER_PERSONNE', label: 'Incapacité de gérer la personne uniquement (lieu de vie, soins)' },
    { value: 'LES_DEUX', label: 'Les deux (biens ET personne)' },
    { value: 'INCAPACITE_PARTIELLE', label: 'Incapacité partielle (capacité résiduelle significative)' },
  ];
  readonly graviteOptions = ProtectionMajeurBeSectionComponent.GRAVITE_OPTIONS;

  static readonly URGENCE_OPTIONS: readonly UrgenceOption[] = [
    { value: 'URGENCE_VITALE', label: 'Urgence vitale (mesure provisoire envisageable)' },
    { value: 'URGENCE_PATRIMONIALE', label: 'Urgence patrimoniale (actes urgents à protéger)' },
    { value: 'AUCUNE_URGENCE', label: 'Aucune urgence' },
  ];
  readonly urgenceOptions = ProtectionMajeurBeSectionComponent.URGENCE_OPTIONS;

  static readonly SAISINE_OPTIONS: readonly SaisineOption[] = [
    { value: 'REQUETE_JP', label: 'Requête à la Justice de paix' },
    { value: 'INDETERMINE', label: 'Indéterminé' },
  ];
  readonly saisineOptions = ProtectionMajeurBeSectionComponent.SAISINE_OPTIONS;

  @Input() caseFileId!: string;
  /** F-177 — force l'expansion (mode modal). */
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<ProtectionMajeurBeResponse | null>(null);

  // --- Form fields ---
  natureAlteration = signal<NatureAlterationBe | null>(null);
  graviteIncapacite = signal<GraviteIncapaciteBe | null>(null);
  mandatExtraJudiciaireSigne = signal<boolean>(false);
  mandatExtraJudiciaireDateSignature = signal<string | null>(null);
  declarationAnticipeeExiste = signal<boolean>(false);
  environnementFamilialProtecteur = signal<boolean>(false);
  niveauUrgence = signal<NiveauUrgenceProtectionBe | null>(null);
  modeSaisineEnvisage = signal<ModeSaisineProtectionBe | null>(null);
  commentaire = signal<string | null>(null);

  alertsSummary = computed(() => ({ total: 0 }));

  constructor(
    private service: ProtectionMajeurBeService,
    private snackBar: MatSnackBar,
    private cdr: ChangeDetectorRef,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.prefillFromAi();
    if (this.workspaceCountry === 'BELGIQUE') {
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

  /** V1 : no-op — aucun signal IA dédié à la protection du majeur BE. */
  private prefillFromAi(): void {
    this.cdr.markForCheck();
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onMandatChange(value: boolean): void {
    this.mandatExtraJudiciaireSigne.set(value);
    if (!value) {
      this.mandatExtraJudiciaireDateSignature.set(null);
    }
  }

  /**
   * Form valide si workspace BE, nature/gravité/urgence/mode de saisine
   * renseignés. Si mandat extra-judiciaire signé est coché, la date associée
   * doit être au format ISO.
   */
  formValid(): boolean {
    if (this.workspaceCountry !== 'BELGIQUE') return false;
    if (!this.natureAlteration()) return false;
    if (!this.graviteIncapacite()) return false;
    if (!this.niveauUrgence()) return false;
    if (!this.modeSaisineEnvisage()) return false;
    if (this.mandatExtraJudiciaireSigne()) {
      const d = this.mandatExtraJudiciaireDateSignature();
      if (!d || !ISO_DATE_REGEX.test(d)) return false;
    }
    return true;
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: ProtectionMajeurBeRequest = {
      natureAlteration: this.natureAlteration()!,
      graviteIncapacite: this.graviteIncapacite()!,
      mandatExtraJudiciaireSigne: this.mandatExtraJudiciaireSigne(),
      mandatExtraJudiciaireDateSignature: this.mandatExtraJudiciaireSigne()
        ? this.mandatExtraJudiciaireDateSignature()
        : null,
      declarationAnticipeeExiste: this.declarationAnticipeeExiste(),
      environnementFamilialProtecteur: this.environnementFamilialProtecteur(),
      niveauUrgence: this.niveauUrgence()!,
      modeSaisineEnvisage: this.modeSaisineEnvisage()!,
      commentaire: this.commentaire()?.trim() || null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Analyse de protection du majeur calculée', 'OK', { duration: 2500 });
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

  private applyResult(r: ProtectionMajeurBeResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  private hydrateForm(r: ProtectionMajeurBeResponse): void {
    this.natureAlteration.set(r.natureAlteration);
    this.graviteIncapacite.set(r.graviteIncapacite);
    this.mandatExtraJudiciaireSigne.set(!!r.mandatExtraJudiciaireSigne);
    this.mandatExtraJudiciaireDateSignature.set(r.mandatExtraJudiciaireDateSignature);
    this.declarationAnticipeeExiste.set(!!r.declarationAnticipeeExiste);
    this.environnementFamilialProtecteur.set(!!r.environnementFamilialProtecteur);
    this.niveauUrgence.set(r.niveauUrgence);
    this.modeSaisineEnvisage.set(r.modeSaisineEnvisage);
    this.commentaire.set(r.commentaire);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  /**
   * Rouge réservé à URGENCE_MESURE_PROVISOIRE. Or pour
   * MESURE_PROTECTION_RECOMMANDEE. Vert pour MANDAT_EXTRA_JUDICIAIRE_VALABLE.
   * Navy info pour QUALIFICATION_INCOMPLETE.
   */
  verdictBannerClass(verdict: ProtectionMajeurBeVerdict): string {
    switch (verdict) {
      case 'MANDAT_EXTRA_JUDICIAIRE_VALABLE':
        return 'pmb-verdict-banner pmb-verdict-banner--ok';
      case 'MESURE_PROTECTION_RECOMMANDEE':
        return 'pmb-verdict-banner pmb-verdict-banner--medium';
      case 'URGENCE_MESURE_PROVISOIRE':
        return 'pmb-verdict-banner pmb-verdict-banner--danger';
      case 'QUALIFICATION_INCOMPLETE':
        return 'pmb-verdict-banner pmb-verdict-banner--info';
    }
  }

  verdictBannerLabel(verdict: ProtectionMajeurBeVerdict): string {
    switch (verdict) {
      case 'MANDAT_EXTRA_JUDICIAIRE_VALABLE': return 'Mandat extra-judiciaire valable';
      case 'MESURE_PROTECTION_RECOMMANDEE': return 'Mesure de protection recommandée';
      case 'URGENCE_MESURE_PROVISOIRE': return 'Urgence — mesure provisoire';
      case 'QUALIFICATION_INCOMPLETE': return 'Qualification incomplète';
    }
  }

  verdictBannerIcon(verdict: ProtectionMajeurBeVerdict): string {
    switch (verdict) {
      case 'MANDAT_EXTRA_JUDICIAIRE_VALABLE': return 'check_circle';
      case 'MESURE_PROTECTION_RECOMMANDEE': return 'warning';
      case 'URGENCE_MESURE_PROVISOIRE': return 'error';
      case 'QUALIFICATION_INCOMPLETE': return 'info';
    }
  }

  mesureRecommandeeLabel(m: MesureProtectionBe): string {
    switch (m) {
      case 'MANDAT_EXTRA_JUDICIAIRE': return 'Mandat extra-judiciaire (voie privée)';
      case 'ADMINISTRATION_BIENS': return 'Administration des biens';
      case 'ADMINISTRATION_PERSONNE': return 'Administration de la personne';
      case 'ADMINISTRATION_BIENS_ET_PERSONNE': return 'Administration des biens ET de la personne';
      case 'MESURE_PROVISOIRE_URGENCE': return 'Mesure provisoire d\'urgence';
      case 'AUCUNE_RECOMMANDATION': return 'Aucune recommandation';
    }
  }

  juridictionLabel(j: JuridictionProtectionBe | null): string {
    if (j === null) return '—';
    switch (j) {
      case 'JUSTICE_PAIX': return 'Justice de paix';
      case 'AUTRE_JURIDICTION': return 'Autre juridiction';
    }
  }

  /**
   * Couleur de la nécessité d'un acte protégé.
   * AUTORISATION_JP_PREALABLE = orange, ADMINISTRATEUR_SEUL = navy,
   * AUTONOMIE_PRESERVEE = vert, INTERDICTION_ABSOLUE = rouge.
   */
  necessiteClass(necessite: NecessiteActeBe): string {
    switch (necessite) {
      case 'AUTORISATION_JP_PREALABLE': return 'pmb-necessite pmb-necessite--orange';
      case 'ADMINISTRATEUR_SEUL': return 'pmb-necessite pmb-necessite--navy';
      case 'AUTONOMIE_PRESERVEE': return 'pmb-necessite pmb-necessite--green';
      case 'INTERDICTION_ABSOLUE': return 'pmb-necessite pmb-necessite--red';
    }
  }

  necessiteLabel(necessite: NecessiteActeBe): string {
    switch (necessite) {
      case 'AUTORISATION_JP_PREALABLE': return 'Autorisation JP préalable';
      case 'ADMINISTRATEUR_SEUL': return 'Administrateur seul';
      case 'AUTONOMIE_PRESERVEE': return 'Autonomie préservée';
      case 'INTERDICTION_ABSOLUE': return 'Interdiction absolue';
    }
  }

  acteCodeLabel(code: ActeProtegeBeCode): string {
    switch (code) {
      case 'DISPOSITION_BIENS_IMMOBILIERS': return 'Bien immobilier';
      case 'DISPOSITION_BIENS_MOBILIERS_VALEURS': return 'Valeurs mobilières';
      case 'OPERATION_BANCAIRE': return 'Opérations bancaires';
      case 'ACTES_PATRIMONIAUX_COURANTS': return 'Patrimoine courant';
      case 'ACTES_PERSONNELS_LIEU_VIE': return 'Lieu de vie';
      case 'ACTES_PERSONNELS_SOINS': return 'Soins médicaux';
      case 'MARIAGE_PACS_TESTAMENT': return 'Mariage / testament';
      case 'ACTES_QUOTIDIENS': return 'Vie quotidienne';
    }
  }

  natureLabel(n: NatureAlterationBe): string {
    const opt = this.natureOptions.find(o => o.value === n);
    return opt?.label ?? n;
  }

  graviteLabel(g: GraviteIncapaciteBe): string {
    const opt = this.graviteOptions.find(o => o.value === g);
    return opt?.label ?? g;
  }

  urgenceLabel(u: NiveauUrgenceProtectionBe): string {
    const opt = this.urgenceOptions.find(o => o.value === u);
    return opt?.label ?? u;
  }
}
