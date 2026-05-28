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
  InterruptionCarriereSoinsParentalForme,
  InterruptionCarriereSoinsParentalModeNotification,
  InterruptionCarriereSoinsParentalRequest,
  InterruptionCarriereSoinsParentalResponse,
  InterruptionCarriereSoinsParentalVerdict,
} from '../../core/models/interruption-carriere-soins-parental.model';
import {
  InterruptionCarriereSoinsParentalService,
} from '../../core/services/interruption-carriere-soins-parental.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  InterruptionCarriereSoinsParentalSectionPrefillRules,
} from './interruption-carriere-soins-parental-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-32b : outil decisionnel Interruption de carriere pour conge
 * parental BE (F-219).
 *
 * <p>BE uniquement — <b>Loi de redressement du 22/01/1985 art. 99 a
 * 107quater</b> (cadre general des interruptions de carriere et
 * protection contre le licenciement art. 101) + <b>AR du 29/10/1997</b>
 * (procedure specifique : conditions d'eligibilite, formes 4 / 8 / 20
 * mois, formalisme notification 2-3 mois) + <b>CCT n 64 du 29/04/1997</b>
 * (rendant le droit obligatoire dans le secteur prive) + <b>AR du
 * 12/08/1991</b> (allocations forfaitaires ONEM) + <b>AR du 12/12/2001</b>
 * (introduction du cinquieme temps 20 mois).</p>
 *
 * <p>Affiche par le panel F-IA-04 (tool_id
 * {@code interruption-carriere-soins-parental}, visibility ALWAYS_ON
 * priority 150 BE / DROIT_DU_TRAVAIL — au-dessus de
 * {@code conge-paternite-naissance-be} (149, SF-219-31b),
 * {@code bien-etre-rps-conseiller-prevention} (148, SF-219-30b),
 * {@code at-mp-rente-capital-be} (147, SF-219-29b)).</p>
 *
 * <h2>Verdict 8 etats</h2>
 * <ul>
 *   <li>{@code ELIGIBLE_COMPLET} (info) — toutes conditions
 *       remplies.</li>
 *   <li>{@code ELIGIBLE_AVEC_RESERVES} (neutral) — eligibilite
 *       confirmee avec points de vigilance.</li>
 *   <li>{@code INELIGIBLE_ANCIENNETE} (critical) — moins de 12 mois.</li>
 *   <li>{@code INELIGIBLE_AGE_ENFANT} (critical) — enfant hors
 *       plafond d'age.</li>
 *   <li>{@code INELIGIBLE_SOLDE_INSUFFISANT} (critical) — solde
 *       individuel epuise.</li>
 *   <li>{@code INELIGIBLE_FORMALISME} (warn) — notification non
 *       conforme.</li>
 *   <li>{@code DIFFERE_EMPLOYEUR} (warn) — differe motive
 *       employeur (6 mois max).</li>
 *   <li>{@code A_QUALIFIER} (neutral) — inputs insuffisants.</li>
 * </ul>
 *
 * <p><b>Pre-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 / F-219 (forme, anciennete (periode reference specifique 15
 * mois), age enfant et handicap, solde ONEM, mode notification, accord
 * et differe employeur, cumul allocation ONEM, dates et remuneration
 * art. 101 non extractibles V1 du dossier salarie generique du pipeline
 * Travail BE — relevent du dossier ONEM, de l'acte de naissance enfant
 * et de la notification employeur).</p>
 *
 * <p>Distinct de F-DT-29 {@code credit-temps-be} (CCT 103 — regime
 * universel sans motif specifique). Cet outil couvre uniquement le
 * conge parental Loi 22/01/1985 et CCT 64.</p>
 */
@Component({
  selector: 'app-interruption-carriere-soins-parental-section',
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
  templateUrl: './interruption-carriere-soins-parental-section.component.html',
  styleUrl: './interruption-carriere-soins-parental-section.component.scss',
})
export class InterruptionCarriereSoinsParentalSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'interruption-carriere-soins-parental';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommee par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'INTERRUPTION CARRIERE CONGE PARENTAL (BE)';
  static readonly TOOL_ICON = 'family_restroom';

  /** F-177 SF-177-12 / F-236 SF-236-02 — delegue au helper partage (parite runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return InterruptionCarriereSoinsParentalSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'equivalent FR mecanique (CAF distincte, duree distincte). */
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
  result = signal<InterruptionCarriereSoinsParentalResponse | null>(null);

  // -- Champs du form (signals, edites via handlers). --
  forme = signal<InterruptionCarriereSoinsParentalForme | null>(null);
  ancienneteMois = signal<number | null>(null);
  ageEnfantMois = signal<number | null>(null);
  enfantHandicap = signal<boolean>(false);
  soldeRestantMoisEtp = signal<number | null>(null);
  modeNotification = signal<InterruptionCarriereSoinsParentalModeNotification | null>(null);
  employeurAccepte = signal<boolean>(false);
  employeurAdiffere = signal<boolean>(false);
  cumulAllocationOnemDemande = signal<boolean>(false);
  dateDebutInterruption = signal<string | null>(null);
  dateNotification = signal<string | null>(null);
  remunerationMensuelleBrute = signal<number | null>(null);

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
    private service: InterruptionCarriereSoinsParentalService,
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
    // SF-219-32b V1 : pas de pre-fill IA (cf. helper doc) — pas de reaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.forme() === null) return false;
    if (this.modeNotification() === null) return false;
    const anc = this.ancienneteMois();
    if (anc === null || anc < 0) return false;
    const age = this.ageEnfantMois();
    if (age === null || age < 0) return false;
    const solde = this.soldeRestantMoisEtp();
    if (solde === null || solde < 0) return false;
    // Coherence dates : notification <= debut si les 2 sont renseignees.
    const dNotif = this.dateNotification();
    const dDebut = this.dateDebutInterruption();
    if (dNotif && dDebut && dNotif !== '' && dDebut !== '' && dNotif > dDebut) {
      return false;
    }
    // Remuneration positive si renseignee.
    const remu = this.remunerationMensuelleBrute();
    if (remu !== null && remu < 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onFormeChange(value: InterruptionCarriereSoinsParentalForme): void {
    this.forme.set(value);
  }
  onAncienneteMoisChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.ancienneteMois.set(null);
      return;
    }
    const num = typeof value === 'number' ? value : Number(value);
    this.ancienneteMois.set(Number.isNaN(num) ? null : num);
  }
  onAgeEnfantMoisChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.ageEnfantMois.set(null);
      return;
    }
    const num = typeof value === 'number' ? value : Number(value);
    this.ageEnfantMois.set(Number.isNaN(num) ? null : num);
  }
  onEnfantHandicapChange(value: boolean): void {
    this.enfantHandicap.set(value);
  }
  onSoldeRestantMoisEtpChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.soldeRestantMoisEtp.set(null);
      return;
    }
    const num = typeof value === 'number' ? value : Number(value);
    this.soldeRestantMoisEtp.set(Number.isNaN(num) ? null : num);
  }
  onModeNotificationChange(value: InterruptionCarriereSoinsParentalModeNotification): void {
    this.modeNotification.set(value);
  }
  onEmployeurAccepteChange(value: boolean): void {
    this.employeurAccepte.set(value);
  }
  onEmployeurAdiffereChange(value: boolean): void {
    this.employeurAdiffere.set(value);
  }
  onCumulAllocationOnemDemandeChange(value: boolean): void {
    this.cumulAllocationOnemDemande.set(value);
  }
  onDateDebutInterruptionChange(value: string | null): void {
    this.dateDebutInterruption.set(value === '' ? null : value);
  }
  onDateNotificationChange(value: string | null): void {
    this.dateNotification.set(value === '' ? null : value);
  }
  onRemunerationMensuelleBruteChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.remunerationMensuelleBrute.set(null);
      return;
    }
    const num = typeof value === 'number' ? value : Number(value);
    this.remunerationMensuelleBrute.set(Number.isNaN(num) ? null : num);
  }

  // -- Libelles humains --
  formeLabel(value: InterruptionCarriereSoinsParentalForme | null): string {
    switch (value) {
      case 'TEMPS_PLEIN':
        return 'Temps plein — 4 mois (AR 29/10/1997 art. 3)';
      case 'MI_TEMPS':
        return 'Mi-temps — 8 mois (AR 29/10/1997 art. 3)';
      case 'CINQUIEME_TEMPS':
        return 'Cinquieme temps — 20 mois (AR 12/12/2001 art. 1)';
      default:
        return '—';
    }
  }

  modeNotificationLabel(value: InterruptionCarriereSoinsParentalModeNotification | null): string {
    switch (value) {
      case 'LETTRE_RECOMMANDEE':
        return 'Lettre recommandee (art. 6 AR 29/10/1997)';
      case 'REMISE_EN_MAIN':
        return 'Remise en main propre contre accuse date / signe';
      case 'AUTRE':
        return 'Autre — mode non conforme art. 6 AR 29/10/1997';
      default:
        return '—';
    }
  }

  /** Verdict en francais lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'ELIGIBLE_COMPLET':
        return 'Eligibilite complete — toutes les conditions du conge parental sont remplies (anciennete >= 12 mois chez l\'employeur dans la periode 15 mois precedant la demande art. 5 AR 29/10/1997, age enfant dans le plafond legal 12 ans / 21 ans handicap art. 4, solde individuel du droit 4 mois ETP par enfant et par parent suffisant art. 2, formalisme respecte mode + delai 2 a 3 mois art. 6, accord employeur). La protection contre le licenciement art. 101 Loi 22/01/1985 court de la notification jusqu\'a 3 mois apres la fin de l\'interruption. Indemnite forfaitaire 6 mois de remuneration en cas de licenciement injustifie. Conseil : suivre le depot du formulaire C61 / C62 ONEM pour l\'allocation forfaitaire mensuelle (AR 12/08/1991 art. 4).';
      case 'ELIGIBLE_AVEC_RESERVES':
        return 'Eligibilite confirmee mais points de vigilance — la demande est recevable mais expose a un risque procedural (delai notification limite proche de 2 mois, solde tendu, formalisme limite mais admis). La protection contre le licenciement art. 101 Loi 22/01/1985 reste active. Conseil : conserver une trace ecrite robuste (accuse de reception lettre recommandee, copie du formulaire date), preparer une reserve sur le respect du delai dans la reponse employeur, et anticiper une eventuelle reorganisation du planning. Suivre le depot ONEM (formulaire C61 / C62) dans les delais utiles.';
      case 'INELIGIBLE_ANCIENNETE':
        return 'Eligibilite refusee — anciennete insuffisante. Le travailleur n\'a pas accompli au moins 12 mois aupres du meme employeur dans la periode des 15 mois precedant la demande (art. 5 AR 29/10/1997). Strategie : verifier le decompte exact (contrats successifs, periodes assimilees art. 5 § 2, eventuels temps partiels imputes en jours d\'occupation), envisager un report de la demande au moment ou la condition sera remplie, et examiner les regimes alternatifs (credit-temps CCT 103 sans condition similaire — voir F-DT-29 credit-temps-be).';
      case 'INELIGIBLE_AGE_ENFANT':
        return 'Eligibilite refusee — age de l\'enfant hors plafond legal. L\'enfant a atteint ou depasse 12 ans (sans handicap) ou 21 ans (handicap >= 66 pour cent) a la date du debut de l\'interruption (art. 4 AR 29/10/1997, plafond porte a 21 ans pour handicap art. 4 al. 2). Strategie : verifier l\'age effectif a la date de debut (et non a la date de notification), confirmer le statut handicap (attestation Vierge Noire / DG Personnes handicapees ou medicale) si applicable, et examiner les regimes alternatifs (credit-temps CCT 103 — voir F-DT-29).';
      case 'INELIGIBLE_SOLDE_INSUFFISANT':
        return 'Eligibilite refusee — solde individuel insuffisant. Le droit individuel de 4 mois ETP par enfant et par parent est epuise ou la demande depasse le solde restant (art. 2 AR 29/10/1997). Strategie : verifier le decompte ONEM (compteur individuel par enfant et par parent), examiner s\'il existe d\'autres enfants ouvrant droit a un nouveau quota, et envisager le credit-temps CCT 103 ou un conge sans solde negocie avec l\'employeur (voir F-DT-29).';
      case 'INELIGIBLE_FORMALISME':
        return 'Eligibilite refusee pour vice de formalisme — la notification a l\'employeur n\'est pas conforme : mode autre que lettre recommandee ou remise en main propre contre accuse date / signe (art. 6 AR 29/10/1997), ou delai 2 a 3 mois avant le debut souhaite non respecte. Strategie : regulariser par une nouvelle notification conforme (recommandee de preference), reporter la date de debut pour respecter le delai legal, et conserver la trace ecrite (accuse de reception postal, accuse signe employeur). En cas de refus employeur sur ce motif, denoncer le refus a l\'Inspection des lois sociales.';
      case 'DIFFERE_EMPLOYEUR':
        return 'Employeur a notifie un differe motive — l\'employeur dispose, dans le mois de la notification, du droit de differer le debut de l\'interruption pour raisons organisationnelles serieuses (continuite du service, remplacement difficile) pour une duree maximum de 6 mois a compter de la date initialement souhaitee (art. 7 AR 29/10/1997). Strategie : verifier la motivation ecrite du differe (raisons serieuses imperatives), contester si motivation insuffisante via l\'Inspection des lois sociales ou le tribunal du travail art. 578 C. jud., et negocier une date de debut acceptable dans le delai maximum de 6 mois.';
      case 'A_QUALIFIER':
        return 'Qualification ouverte — inputs insuffisants ou contradictoires. Verifier l\'anciennete exacte (15 mois precedant la demande, art. 5 AR 29/10/1997), l\'age precis de l\'enfant en mois a la date prevue du debut, le statut handicap (attestation officielle si invoque), le solde individuel restant aupres de l\'ONEM, la date et le mode de notification effectifs (accuse postal ou signe), et l\'eventuelle reponse formelle de l\'employeur (accord ou differe motive). L\'avocat doit completer ces elements avant de relancer l\'analyse.';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict. */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'INELIGIBLE_ANCIENNETE':
      case 'INELIGIBLE_AGE_ENFANT':
      case 'INELIGIBLE_SOLDE_INSUFFISANT':
        return 'verdict-critical';
      case 'INELIGIBLE_FORMALISME':
      case 'DIFFERE_EMPLOYEUR':
        return 'verdict-warn';
      case 'ELIGIBLE_COMPLET':
        return 'verdict-info';
      case 'ELIGIBLE_AVEC_RESERVES':
      case 'A_QUALIFIER':
        return 'verdict-neutral';
      default:
        return '';
    }
  }

  /** Icone Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'family_restroom';
    switch (r.verdict) {
      case 'ELIGIBLE_COMPLET':
        return 'check_circle';
      case 'ELIGIBLE_AVEC_RESERVES':
        return 'verified';
      case 'INELIGIBLE_ANCIENNETE':
        return 'work_history';
      case 'INELIGIBLE_AGE_ENFANT':
        return 'child_care';
      case 'INELIGIBLE_SOLDE_INSUFFISANT':
        return 'production_quantity_limits';
      case 'INELIGIBLE_FORMALISME':
        return 'report';
      case 'DIFFERE_EMPLOYEUR':
        return 'schedule';
      case 'A_QUALIFIER':
        return 'help_outline';
      default:
        return 'family_restroom';
    }
  }

  /** Libelle bref pour le badge du header. */
  verdictBadge(verdict: InterruptionCarriereSoinsParentalVerdict): string {
    switch (verdict) {
      case 'ELIGIBLE_COMPLET':
        return 'ELIGIBLE';
      case 'ELIGIBLE_AVEC_RESERVES':
        return 'ELIGIBLE RESERVES';
      case 'INELIGIBLE_ANCIENNETE':
        return 'ANCIENNETE KO';
      case 'INELIGIBLE_AGE_ENFANT':
        return 'AGE ENFANT KO';
      case 'INELIGIBLE_SOLDE_INSUFFISANT':
        return 'SOLDE KO';
      case 'INELIGIBLE_FORMALISME':
        return 'FORMALISME KO';
      case 'DIFFERE_EMPLOYEUR':
        return 'DIFFERE EMPLOYEUR';
      case 'A_QUALIFIER':
        return 'A QUALIFIER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: InterruptionCarriereSoinsParentalVerdict): string {
    switch (verdict) {
      case 'INELIGIBLE_ANCIENNETE':
      case 'INELIGIBLE_AGE_ENFANT':
      case 'INELIGIBLE_SOLDE_INSUFFISANT':
        return 'badge-critical';
      case 'INELIGIBLE_FORMALISME':
      case 'DIFFERE_EMPLOYEUR':
        return 'badge-warn';
      case 'ELIGIBLE_COMPLET':
        return 'badge-info';
      case 'ELIGIBLE_AVEC_RESERVES':
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

  /** Format mois -> libelle francais. */
  formatMois(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    const unit = value === 1 || value === -1 ? 'mois' : 'mois';
    return `${value.toLocaleString('fr-BE')} ${unit}`;
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: InterruptionCarriereSoinsParentalRequest = {
      forme: this.forme()!,
      ancienneteMois: this.ancienneteMois()!,
      ageEnfantMois: this.ageEnfantMois()!,
      enfantHandicap: this.enfantHandicap(),
      soldeRestantMoisEtp: this.soldeRestantMoisEtp()!,
      modeNotification: this.modeNotification()!,
      employeurAccepte: this.employeurAccepte(),
      employeurAdiffere: this.employeurAdiffere(),
      cumulAllocationOnemDemande: this.cumulAllocationOnemDemande(),
      dateDebutInterruption: this.dateDebutInterruption(),
      dateNotification: this.dateNotification(),
      remunerationMensuelleBrute: this.remunerationMensuelleBrute(),
    };
    this.calculating.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse conge parental calculee', 'OK', { duration: 2500 });
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
        this.forme.set(r.forme);
        this.ancienneteMois.set(r.ancienneteMois);
        this.ageEnfantMois.set(r.ageEnfantMois);
        this.enfantHandicap.set(r.enfantHandicap);
        this.soldeRestantMoisEtp.set(r.soldeRestantMoisEtp);
        this.modeNotification.set(r.modeNotification);
        this.employeurAccepte.set(r.employeurAccepte);
        this.employeurAdiffere.set(r.employeurAdiffere);
        this.cumulAllocationOnemDemande.set(r.cumulAllocationOnemDemande);
        this.dateDebutInterruption.set(r.dateDebutInterruption);
        this.dateNotification.set(r.dateNotification);
        this.remunerationMensuelleBrute.set(r.remunerationMensuelleBrute);
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
