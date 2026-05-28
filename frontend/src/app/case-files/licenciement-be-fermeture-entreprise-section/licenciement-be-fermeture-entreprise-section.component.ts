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
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatListModule } from '@angular/material/list';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import {
  LicenciementBeFermetureEntrepriseRequest,
  LicenciementBeFermetureEntrepriseResponse,
  LicenciementBeFermetureEntrepriseStatutEmployeur,
  LicenciementBeFermetureEntrepriseTypeFermeture,
  LicenciementBeFermetureEntrepriseVerdict,
} from '../../core/models/licenciement-be-fermeture-entreprise.model';
import {
  LicenciementBeFermetureEntrepriseService,
} from '../../core/services/licenciement-be-fermeture-entreprise.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  LicenciementBeFermetureEntrepriseSectionPrefillRules,
} from './licenciement-be-fermeture-entreprise-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-06b : outil décisionnel « Licenciement BE — fermeture d'entreprise »
 * (F-219).
 *
 * <p>BE uniquement — <b>Loi du 26/06/2002</b> relative aux fermetures
 * d'entreprises + <b>AR du 23/03/2007</b> d'exécution + <b>CCT n° 9bis</b>
 * (information / consultation préalable du CE). Indemnités prises en
 * charge par le <b>Fonds de Fermeture des Entreprises</b> (FFE) géré par
 * l'ONEM.</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id
 * {@code licenciement-be-fermeture-entreprise}, visibility ALWAYS_ON
 * priority 124 BE / DROIT_DU_TRAVAIL — au-dessus de
 * {@code outplacement-be-general-30sem} (123, SF-219-05b).</p>
 *
 * <p><b>Verdict 6 états</b> :
 * <ul>
 *   <li>{@code ELIGIBLE_FFE_COMPLET} (vert) — fermeture qualifiée + insolvable :
 *       FFE indemnité + reprise créances.</li>
 *   <li>{@code ELIGIBLE_FFE_REPRISE_CREANCES} (vert) — fermeture qualifiée +
 *       insolvable : FFE reprend créances impayées + indemnité due.</li>
 *   <li>{@code ELIGIBLE_INDEMNITE_SEULE} (vert) — fermeture qualifiée +
 *       solvable : indemnité de fermeture seule.</li>
 *   <li>{@code INELIGIBLE_ANCIENNETE_INSUFFISANTE} (rouge) — &lt; 1 an
 *       ancienneté à la date de fermeture.</li>
 *   <li>{@code INELIGIBLE_TYPE_FERMETURE} (rouge) — cessation partielle /
 *       transfert / fusion / temporaire.</li>
 *   <li>{@code INELIGIBLE_SEUIL_EFFECTIF} (rouge) — &lt; 20 ETP (régime
 *       général V1).</li>
 * </ul></p>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213/F-219 (qualification fermeture / type / solvabilité / effectif
 * employeur non extractibles depuis la pipeline Travail BE actuelle ;
 * saisie avocat depuis contrat + jugement faillite + attestation
 * employeur + déclaration ONEM-FFE).</p>
 */
@Component({
  selector: 'app-licenciement-be-fermeture-entreprise-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule,
    MatListModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './licenciement-be-fermeture-entreprise-section.component.html',
  styleUrl: './licenciement-be-fermeture-entreprise-section.component.scss',
})
export class LicenciementBeFermetureEntrepriseSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'licenciement-be-fermeture-entreprise';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'FERMETURE ENTREPRISE (BE)';
  static readonly TOOL_ICON = 'business';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return LicenciementBeFermetureEntrepriseSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR. */
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private cdr = inject(ChangeDetectorRef);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<LicenciementBeFermetureEntrepriseResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  dateNaissance = signal<string | null>(null);
  dateDebutContrat = signal<string | null>(null);
  dateFermeture = signal<string | null>(null);
  remunerationMensuelleBrute = signal<number | null>(null);
  typeFermeture = signal<LicenciementBeFermetureEntrepriseTypeFermeture | null>(null);
  statutEmployeur = signal<LicenciementBeFermetureEntrepriseStatutEmployeur | null>(null);
  effectifEtp = signal<number | null>(null);
  salairesImpayes = signal<number | null>(null);
  peculeVacancesImpaye = signal<number | null>(null);
  indemniteRuptureImpayee = signal<number | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: LicenciementBeFermetureEntrepriseService,
    private snackBar: MatSnackBar,
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
    // SF-219-06b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (!this.dateNaissance()) return false;
    if (!this.dateDebutContrat()) return false;
    if (!this.dateFermeture()) return false;
    if (this.remunerationMensuelleBrute() === null) return false;
    if (this.typeFermeture() === null) return false;
    if (this.statutEmployeur() === null) return false;
    if (this.effectifEtp() === null) return false;
    if (this.salairesImpayes() === null) return false;
    if (this.peculeVacancesImpaye() === null) return false;
    if (this.indemniteRuptureImpayee() === null) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onDateNaissanceChange(value: string | null): void {
    this.dateNaissance.set(value);
  }
  onDateDebutContratChange(value: string | null): void {
    this.dateDebutContrat.set(value);
  }
  onDateFermetureChange(value: string | null): void {
    this.dateFermeture.set(value);
  }
  onRemunerationMensuelleBruteChange(value: number | null): void {
    this.remunerationMensuelleBrute.set(value);
  }
  onTypeFermetureChange(value: LicenciementBeFermetureEntrepriseTypeFermeture): void {
    this.typeFermeture.set(value);
  }
  onStatutEmployeurChange(value: LicenciementBeFermetureEntrepriseStatutEmployeur): void {
    this.statutEmployeur.set(value);
  }
  onEffectifEtpChange(value: number | null): void {
    this.effectifEtp.set(value);
  }
  onSalairesImpayesChange(value: number | null): void {
    this.salairesImpayes.set(value);
  }
  onPeculeVacancesImpayeChange(value: number | null): void {
    this.peculeVacancesImpaye.set(value);
  }
  onIndemniteRuptureImpayeeChange(value: number | null): void {
    this.indemniteRuptureImpayee.set(value);
  }

  // -- Libellés humains --
  typeFermetureLabel(
    value: LicenciementBeFermetureEntrepriseTypeFermeture | null,
  ): string {
    switch (value) {
      case 'CESSATION_DEFINITIVE':
        return 'Cessation définitive d\'activité';
      case 'FAILLITE_LIQUIDATION_JUDICIAIRE':
        return 'Faillite / liquidation judiciaire';
      case 'CESSATION_PARTIELLE':
        return 'Cessation partielle';
      case 'TRANSFERT_CCT_32BIS':
        return 'Transfert d\'entreprise (CCT 32bis)';
      case 'FUSION_SANS_SUPPRESSION':
        return 'Fusion par absorption sans suppression';
      case 'FERMETURE_TEMPORAIRE':
        return 'Fermeture temporaire (chômage technique)';
      default:
        return '—';
    }
  }

  statutEmployeurLabel(
    value: LicenciementBeFermetureEntrepriseStatutEmployeur | null,
  ): string {
    switch (value) {
      case 'SOLVABLE':
        return 'Solvable — créances payées';
      case 'INSOLVABLE_AVERE':
        return 'Insolvable (avéré par procédure)';
      case 'FAILLITE_DECLAREE':
        return 'Faillite déclarée Tribunal';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'ELIGIBLE_FFE_COMPLET':
        return 'FFE complet — indemnité + reprise créances';
      case 'ELIGIBLE_FFE_REPRISE_CREANCES':
        return 'FFE — reprise des créances impayées';
      case 'ELIGIBLE_INDEMNITE_SEULE':
        return 'Indemnité de fermeture seule (employeur solvable)';
      case 'INELIGIBLE_ANCIENNETE_INSUFFISANTE':
        return 'Inéligible — ancienneté < 1 an';
      case 'INELIGIBLE_TYPE_FERMETURE':
        return 'Inéligible — type de fermeture non qualifiant';
      case 'INELIGIBLE_SEUIL_EFFECTIF':
        return 'Inéligible — effectif < 20 ETP';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert éligible, rouge inéligible). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'ELIGIBLE_FFE_COMPLET':
      case 'ELIGIBLE_FFE_REPRISE_CREANCES':
      case 'ELIGIBLE_INDEMNITE_SEULE':
        return 'verdict-ok';
      case 'INELIGIBLE_ANCIENNETE_INSUFFISANTE':
      case 'INELIGIBLE_TYPE_FERMETURE':
      case 'INELIGIBLE_SEUIL_EFFECTIF':
        return 'verdict-critical';
      default:
        return '';
    }
  }

  /** Icône Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'gavel';
    switch (r.verdict) {
      case 'ELIGIBLE_FFE_COMPLET':
        return 'verified';
      case 'ELIGIBLE_FFE_REPRISE_CREANCES':
        return 'savings';
      case 'ELIGIBLE_INDEMNITE_SEULE':
        return 'paid';
      case 'INELIGIBLE_ANCIENNETE_INSUFFISANTE':
      case 'INELIGIBLE_TYPE_FERMETURE':
      case 'INELIGIBLE_SEUIL_EFFECTIF':
        return 'block';
      default:
        return 'gavel';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: LicenciementBeFermetureEntrepriseVerdict): string {
    switch (verdict) {
      case 'ELIGIBLE_FFE_COMPLET':
        return 'FFE COMPLET';
      case 'ELIGIBLE_FFE_REPRISE_CREANCES':
        return 'FFE CRÉANCES';
      case 'ELIGIBLE_INDEMNITE_SEULE':
        return 'INDEMNITÉ SEULE';
      case 'INELIGIBLE_ANCIENNETE_INSUFFISANTE':
        return 'ANCIENNETÉ INSUFF.';
      case 'INELIGIBLE_TYPE_FERMETURE':
        return 'TYPE NON QUALIFIANT';
      case 'INELIGIBLE_SEUIL_EFFECTIF':
        return 'EFFECTIF < 20';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: LicenciementBeFermetureEntrepriseVerdict): string {
    switch (verdict) {
      case 'ELIGIBLE_FFE_COMPLET':
      case 'ELIGIBLE_FFE_REPRISE_CREANCES':
      case 'ELIGIBLE_INDEMNITE_SEULE':
        return 'badge-ok';
      case 'INELIGIBLE_ANCIENNETE_INSUFFISANTE':
      case 'INELIGIBLE_TYPE_FERMETURE':
      case 'INELIGIBLE_SEUIL_EFFECTIF':
        return 'badge-critical';
      default:
        return '';
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: LicenciementBeFermetureEntrepriseRequest = {
      dateNaissance: this.dateNaissance()!,
      dateDebutContrat: this.dateDebutContrat()!,
      dateFermeture: this.dateFermeture()!,
      remunerationMensuelleBrute: this.remunerationMensuelleBrute()!,
      typeFermeture: this.typeFermeture()!,
      statutEmployeur: this.statutEmployeur()!,
      effectifEtp: this.effectifEtp()!,
      salairesImpayes: this.salairesImpayes()!,
      peculeVacancesImpaye: this.peculeVacancesImpaye()!,
      indemniteRuptureImpayee: this.indemniteRuptureImpayee()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Fermeture entreprise analysée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.calculating.set(false);
        const status = err?.status;
        let msg: string;
        if (status === 404) {
          msg = 'Dossier introuvable';
        } else if (status === 400) {
          msg = err?.error?.message || err?.error || 'Données saisies invalides';
        } else {
          msg = 'Une erreur est survenue. Veuillez réessayer.';
        }
        this.snackBar.open(String(msg), 'Fermer',
          { duration: 5000, panelClass: 'snack-error' });
        this.cdr.markForCheck();
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.loading.set(false);
        // Rehydratation form pour permettre l'édition.
        this.dateNaissance.set(r.dateNaissance);
        this.dateDebutContrat.set(r.dateDebutContrat);
        this.dateFermeture.set(r.dateFermeture);
        this.remunerationMensuelleBrute.set(r.remunerationMensuelleBrute);
        this.typeFermeture.set(r.typeFermeture);
        this.statutEmployeur.set(r.statutEmployeur);
        this.effectifEtp.set(r.effectifEtp);
        this.salairesImpayes.set(r.salairesImpayes);
        this.peculeVacancesImpaye.set(r.peculeVacancesImpaye);
        this.indemniteRuptureImpayee.set(r.indemniteRuptureImpayee);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — mode formulaire vierge.
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
