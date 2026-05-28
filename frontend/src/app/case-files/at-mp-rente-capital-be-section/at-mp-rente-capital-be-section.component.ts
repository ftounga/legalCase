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
import { MatCheckboxModule } from '@angular/material/checkbox';
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
  AtMpRenteCapitalBeOrigine,
  AtMpRenteCapitalBeRequest,
  AtMpRenteCapitalBeResponse,
  AtMpRenteCapitalBeStatutReconnaissance,
  AtMpRenteCapitalBeVerdict,
} from '../../core/models/at-mp-rente-capital-be.model';
import {
  AtMpRenteCapitalBeService,
} from '../../core/services/at-mp-rente-capital-be.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  AtMpRenteCapitalBeSectionPrefillRules,
} from './at-mp-rente-capital-be-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-29b : outil decisionnel Rente AT/MP vs capitalisation BE
 * (F-219).
 *
 * <p>BE uniquement — <b>Loi du 10/04/1971 sur les accidents du
 * travail art. 24</b> (capital forfaitaire IPP &lt; 19 pour cent vs
 * rente annuelle viagere IPP &gt;= 19 pour cent) + <b>Lois coordonnees
 * du 03/06/1970 art. 35</b> (renvoi MP au regime AT pour le mode de
 * versement) + <b>AR du 21/12/1971</b> portant execution de la Loi
 * 10/04/1971 (bareme de capitalisation et coefficients d'age) + <b>AR
 * du 10/12/1987</b> fixant les regles de conversion partielle de la
 * rente en capital (1/3 max apres 3 ans) + <b>AR du 24/02/2005</b>
 * actualisant le bareme (Table I-bis coefficients d'age).</p>
 *
 * <p>Affiche par le panel F-IA-04 (tool_id {@code at-mp-rente-capital-be},
 * visibility ALWAYS_ON priority 147 BE / DROIT_DU_TRAVAIL — au-dessus
 * de {@code mp-fedris-reconnaissance} (146, SF-219-28b) en complement
 * de l'outil de reconnaissance pour le chiffrage des indemnites
 * forfaitaires apres consolidation).</p>
 *
 * <h2>Verdict 5 etats</h2>
 * <ul>
 *   <li>{@code CAPITAL_FORFAITAIRE_LT_19} (info) — IPP &lt; 19 pour cent :
 *       capital unique forfaitaire (capitalisation d'office art. 24
 *       al. 2 Loi 10/04/1971).</li>
 *   <li>{@code RENTE_ANNUELLE_GE_19} (info) — IPP &gt;= 19 pour cent :
 *       rente annuelle viagere (art. 24 al. 1).</li>
 *   <li>{@code INELIGIBLE_NON_RECONNU} (critique) — AT ou MP non
 *       reconnu (ni rente ni capital).</li>
 *   <li>{@code IPP_NON_DETERMINE} (warn) — taux IPP pas encore arrete
 *       (calcul premature).</li>
 *   <li>{@code A_QUALIFIER} (neutre) — inputs ambigus.</li>
 * </ul>
 *
 * <p><b>Pre-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 / F-219 (origine AT/MP, statut Fedris, dates medicales, taux
 * IPP, remuneration de base art. 34 plafonnee art. 39, date de
 * naissance, demande de conversion partielle non extractibles V1 du
 * dossier salarie generique du pipeline Travail BE).</p>
 */
@Component({
  selector: 'app-at-mp-rente-capital-be-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatListModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './at-mp-rente-capital-be-section.component.html',
  styleUrl: './at-mp-rente-capital-be-section.component.scss',
})
export class AtMpRenteCapitalBeSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'at-mp-rente-capital-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommee par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RENTE AT/MP VS CAPITALISATION (BE)';
  static readonly TOOL_ICON = 'account_balance';

  /** F-177 SF-177-12 / F-236 SF-236-02 — delegue au helper partage (parite runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return AtMpRenteCapitalBeSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'equivalent FR mecanique (CPAM distincte, seuils differents). */
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
  result = signal<AtMpRenteCapitalBeResponse | null>(null);

  // -- Champs du form (signals, edites via handlers). --
  origine = signal<AtMpRenteCapitalBeOrigine | null>(null);
  statutReconnaissance = signal<AtMpRenteCapitalBeStatutReconnaissance | null>(null);
  dateConsolidation = signal<string | null>(null);
  tauxIpp = signal<number | null>(null);
  remunerationBaseAnnuelle = signal<number | null>(null);
  dateNaissance = signal<string | null>(null);
  demandeConversionPartielle = signal<boolean>(false);
  dateDemandeConversion = signal<string | null>(null);
  ageConsolidationOverride = signal<number | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  // Formateur EUR (fr-BE) instancie une fois.
  private readonly euroFormatter = new Intl.NumberFormat('fr-BE', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

  constructor(
    private service: AtMpRenteCapitalBeService,
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
    // SF-219-29b V1 : pas de pre-fill IA (cf. helper doc) — pas de reaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.origine() === null) return false;
    if (this.statutReconnaissance() === null) return false;
    const remu = this.remunerationBaseAnnuelle();
    if (remu === null || remu <= 0) return false;
    if (this.dateNaissance() === null || this.dateNaissance() === '') return false;
    // Si statut RECONNU, taux IPP et date consolidation requis.
    if (this.statutReconnaissance() === 'RECONNU') {
      const ipp = this.tauxIpp();
      if (ipp === null) return false;
      if (ipp < 0 || ipp > 100) return false;
      const dc = this.dateConsolidation();
      if (dc === null || dc === '') return false;
    }
    // Override age coherent.
    const ageOv = this.ageConsolidationOverride();
    if (ageOv !== null && (ageOv < 0 || ageOv > 120)) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onOrigineChange(value: AtMpRenteCapitalBeOrigine): void {
    this.origine.set(value);
  }
  onStatutReconnaissanceChange(value: AtMpRenteCapitalBeStatutReconnaissance): void {
    this.statutReconnaissance.set(value);
  }
  onDateConsolidationChange(value: string | null): void {
    this.dateConsolidation.set(value === '' ? null : value);
  }
  onTauxIppChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.tauxIpp.set(null);
      return;
    }
    const num = typeof value === 'number' ? value : Number(value);
    this.tauxIpp.set(Number.isNaN(num) ? null : num);
  }
  onRemunerationBaseAnnuelleChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.remunerationBaseAnnuelle.set(null);
      return;
    }
    const num = typeof value === 'number' ? value : Number(value);
    this.remunerationBaseAnnuelle.set(Number.isNaN(num) ? null : num);
  }
  onDateNaissanceChange(value: string | null): void {
    this.dateNaissance.set(value === '' ? null : value);
  }
  onDemandeConversionPartielleChange(value: boolean): void {
    this.demandeConversionPartielle.set(value);
    if (!value) {
      this.dateDemandeConversion.set(null);
    }
  }
  onDateDemandeConversionChange(value: string | null): void {
    this.dateDemandeConversion.set(value === '' ? null : value);
  }
  onAgeConsolidationOverrideChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.ageConsolidationOverride.set(null);
      return;
    }
    const num = typeof value === 'number' ? value : Number(value);
    this.ageConsolidationOverride.set(Number.isNaN(num) ? null : num);
  }

  // -- Libelles humains --
  origineLabel(value: AtMpRenteCapitalBeOrigine | null): string {
    switch (value) {
      case 'ACCIDENT_TRAVAIL':
        return 'Accident du travail (art. 7-8 Loi 10/04/1971)';
      case 'MALADIE_PROFESSIONNELLE':
        return 'Maladie professionnelle (Lois coordonnees 03/06/1970)';
      default:
        return '—';
    }
  }

  statutReconnaissanceLabel(value: AtMpRenteCapitalBeStatutReconnaissance | null): string {
    switch (value) {
      case 'RECONNU':
        return 'Reconnu — decision Fedris / assureur-loi acquise';
      case 'CONTESTE':
        return 'Conteste — refus / taux conteste (recours tribunal du travail)';
      case 'EN_COURS':
        return 'En cours — dossier en instruction Fedris / assureur-loi';
      default:
        return '—';
    }
  }

  /** Verdict en francais lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'CAPITAL_FORFAITAIRE_LT_19':
        return 'Taux IPP inferieur a 19 pour cent — l\'indemnite est versee en CAPITAL UNIQUE par capitalisation d\'office (art. 24 al. 2 Loi 10/04/1971 + art. 4 AR 10/12/1987). Le montant est calcule selon le bareme AR 21/12/1971 actualise AR 24/02/2005 : remuneration de base annuelle (plafonnee art. 39 Loi 10/04/1971 — 49 853,86 EUR au 01/01/2024 indexe) x taux IPP x coefficient d\'age Table I-bis (decroissant du quasi-13 a 1 an au quasi-0 a 75 ans). Aucune conversion partielle possible (versement deja en capital). Etapes : verification de l\'expertise medicale Fedris / assureur-loi, calcul du capital, allocation par tranches le cas echeant.';
      case 'RENTE_ANNUELLE_GE_19':
        return 'Taux IPP superieur ou egal a 19 pour cent — la reparation est versee en RENTE ANNUELLE viagere (art. 24 al. 1 Loi 10/04/1971). Base = remuneration de base x taux IPP. Indexation annuelle Loi 02/04/1965 art. 2. Conversion partielle possible : 1/3 maximum de la rente en capital art. 45ter Loi 10/04/1971 + AR 10/12/1987, possible uniquement apres 3 ans depuis la consolidation (delai d\'epreuve art. 24 paragr. 3 AR 03/07/1996). Si demandee : capital = (1/3 rente) x coefficient d\'age Table I-bis + rente residuelle = 2/3 rente. Strategie : envisager la conversion pour besoins de tresorerie (remboursement credit, investissement immobilier) en arbitrant avec la perte d\'indexation et de revenu garanti viager.';
      case 'INELIGIBLE_NON_RECONNU':
        return 'AT ou MP non reconnu par Fedris / assureur-loi — ni rente ni capital ne sont dus tant que la reconnaissance n\'est pas acquise. Voies de recours : recours tribunal du travail art. 580 1 C. jud. (delai 1 an art. 64 Loi 10/04/1971 / art. 23 lois coordonnees 03/06/1970), expertise medicale judiciaire art. 962-991 C. jud. en cas de contestation du taux IPP. Strategie : verifier la decision administrative (date de notification, motivation), preparer le recours dans le delai d\'1 an, solliciter une expertise medicale contradictoire. Articulation possible avec l\'outil SF-219-28b mp-fedris-reconnaissance pour la qualification de la MP en amont.';
      case 'IPP_NON_DETERMINE':
        return 'Taux d\'incapacite permanente partielle pas encore arrete par l\'expertise medicale Fedris ou assureur-loi (consolidation non acquise, expertise en cours art. 24 paragr. 2 AR 03/07/1996). Calcul premature — attendre la decision de consolidation. Pendant l\'incapacite temporaire : indemnite journaliere 90 pour cent de la remuneration moyenne (art. 22 Loi 10/04/1971 AT / art. 34 lois coordonnees 03/06/1970 MP). Strategie : suivre l\'instruction du dossier, prevoir un examen medical contradictoire en cas de divergence d\'appreciation, anticiper le calcul prospectif des le projet de decision de consolidation.';
      case 'A_QUALIFIER':
        return 'Inputs ambigus — analyse complementaire avocat requise. Verifier la remuneration de base art. 34 Loi 10/04/1971 (12 mois precedents, plafonnee art. 39 — 49 853,86 EUR au 01/01/2024), l\'age de la victime (intervalle plausible 16-75 ans pour le coefficient Table I-bis AR 24/02/2005), le statut de reconnaissance et la coherence des dates (naissance, consolidation, demande de conversion).';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict. */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'INELIGIBLE_NON_RECONNU':
        return 'verdict-critical';
      case 'IPP_NON_DETERMINE':
        return 'verdict-warn';
      case 'CAPITAL_FORFAITAIRE_LT_19':
      case 'RENTE_ANNUELLE_GE_19':
        return 'verdict-info';
      case 'A_QUALIFIER':
        return 'verdict-neutral';
      default:
        return '';
    }
  }

  /** Icone Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'account_balance';
    switch (r.verdict) {
      case 'CAPITAL_FORFAITAIRE_LT_19':
        return 'savings';
      case 'RENTE_ANNUELLE_GE_19':
        return 'payments';
      case 'INELIGIBLE_NON_RECONNU':
        return 'block';
      case 'IPP_NON_DETERMINE':
        return 'hourglass_empty';
      case 'A_QUALIFIER':
        return 'help_outline';
      default:
        return 'account_balance';
    }
  }

  /** Libelle bref pour le badge du header. */
  verdictBadge(verdict: AtMpRenteCapitalBeVerdict): string {
    switch (verdict) {
      case 'CAPITAL_FORFAITAIRE_LT_19':
        return 'CAPITAL IPP -19';
      case 'RENTE_ANNUELLE_GE_19':
        return 'RENTE IPP +19';
      case 'INELIGIBLE_NON_RECONNU':
        return 'NON RECONNU';
      case 'IPP_NON_DETERMINE':
        return 'IPP NON FIXE';
      case 'A_QUALIFIER':
        return 'A QUALIFIER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: AtMpRenteCapitalBeVerdict): string {
    switch (verdict) {
      case 'INELIGIBLE_NON_RECONNU':
        return 'badge-critical';
      case 'IPP_NON_DETERMINE':
        return 'badge-warn';
      case 'CAPITAL_FORFAITAIRE_LT_19':
      case 'RENTE_ANNUELLE_GE_19':
        return 'badge-info';
      case 'A_QUALIFIER':
        return 'badge-neutral';
      default:
        return '';
    }
  }

  /** Format date ISO yyyy-MM-dd -> jj/mm/aaaa. */
  formatDate(value: string | null | undefined): string {
    if (value === null || value === undefined || value === '') return '—';
    const parts = value.split('-');
    if (parts.length !== 3) return value;
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  }

  /** Boolean -> Oui / Non / —. */
  formatBoolean(value: boolean | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value ? 'Oui' : 'Non';
  }

  /** Format montant EUR (fr-BE). */
  formatMontant(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return this.euroFormatter.format(value);
  }

  /** Format pourcentage (taux IPP, coefficient). */
  formatPourcentage(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return `${value.toLocaleString('fr-BE', { minimumFractionDigits: 0, maximumFractionDigits: 2 })} %`;
  }

  /** Format coefficient d'age. */
  formatCoefficient(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value.toLocaleString('fr-BE', { minimumFractionDigits: 0, maximumFractionDigits: 4 });
  }

  /** Format age. */
  formatAge(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return `${value} ans`;
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: AtMpRenteCapitalBeRequest = {
      origine: this.origine()!,
      statutReconnaissance: this.statutReconnaissance()!,
      dateConsolidation: this.dateConsolidation(),
      tauxIpp: this.tauxIpp(),
      remunerationBaseAnnuelle: this.remunerationBaseAnnuelle()!,
      dateNaissance: this.dateNaissance()!,
      demandeConversionPartielle: this.demandeConversionPartielle(),
      dateDemandeConversion: this.dateDemandeConversion(),
      ageConsolidationOverride: this.ageConsolidationOverride(),
    };
    this.calculating.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse rente AT/MP calculee', 'OK', { duration: 2500 });
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
          msg = err?.error?.message || err?.error || 'Donnees saisies invalides';
        } else {
          msg = 'Une erreur est survenue. Veuillez reessayer.';
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
        // Rehydratation form pour permettre l'edition.
        this.origine.set(r.origine);
        this.statutReconnaissance.set(r.statutReconnaissance);
        this.dateConsolidation.set(r.dateConsolidation);
        this.tauxIpp.set(r.tauxIpp);
        this.remunerationBaseAnnuelle.set(r.remunerationBaseAnnuelle);
        this.dateNaissance.set(r.dateNaissance);
        this.demandeConversionPartielle.set(r.demandeConversionPartielle);
        this.dateDemandeConversion.set(r.dateDemandeConversion);
        this.ageConsolidationOverride.set(r.ageConsolidationOverride);
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
