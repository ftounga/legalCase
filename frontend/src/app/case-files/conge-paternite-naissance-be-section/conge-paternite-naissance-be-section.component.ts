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
  CongePaterniteNaissanceBeEtape,
  CongePaterniteNaissanceBeLienFiliation,
  CongePaterniteNaissanceBeRequest,
  CongePaterniteNaissanceBeResponse,
  CongePaterniteNaissanceBeStatutTravailleur,
  CongePaterniteNaissanceBeVerdict,
} from '../../core/models/conge-paternite-naissance-be.model';
import {
  CongePaterniteNaissanceBeService,
} from '../../core/services/conge-paternite-naissance-be.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  CongePaterniteNaissanceBeSectionPrefillRules,
} from './conge-paternite-naissance-be-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-31b : outil decisionnel Conge paternite / naissance BE
 * (F-219).
 *
 * <p>BE uniquement — <b>Loi du 03/07/1978 sur les contrats de travail
 * art. 30 paragr. 2</b> (droit a 10 / 15 / 20 jours ouvrables selon
 * date de naissance, dans les 4 mois post-naissance) + <b>Loi du
 * 12/08/2000</b> portant des dispositions sociales budgetaires et
 * diverses + <b>Loi du 07/04/2023</b> portant des dispositions en
 * matiere de Deal pour l'emploi 2022 (extension a tous les co-parents,
 * alignement de la duree a 20 jours pour les naissances a partir du
 * 01/01/2023) + <b>AR du 17/10/1994</b> sur les modalites
 * d'information et de prise + <b>AR du 03/07/1996</b> indemnisation
 * INAMI (employeur 100 pour cent les 3 premiers jours puis mutuelle
 * 82 pour cent plafonnee les jours suivants) + protection 5 mois
 * contre le licenciement art. 30 paragr. 4 (de la notification a 5
 * mois apres la prise complete).</p>
 *
 * <p>Affiche par le panel F-IA-04 (tool_id
 * {@code conge-paternite-naissance-be}, visibility ALWAYS_ON priority
 * 149 BE / DROIT_DU_TRAVAIL — au-dessus de
 * {@code bien-etre-rps-conseiller-prevention} 148 SF-219-30b et de
 * {@code at-mp-rente-capital-be} 147 SF-219-29b).</p>
 *
 * <h2>Verdict 8 etats</h2>
 * <ul>
 *   <li>{@code ELIGIBLE_CONGE_OUVERT} (info) — droit ouvert, en attente
 *       de prise ou prise partielle.</li>
 *   <li>{@code CONGE_EN_COURS_PROTECTION_ACTIVE} (info) — prise en
 *       cours, protection 5 mois licenciement active art. 30
 *       paragr. 4.</li>
 *   <li>{@code CONGE_PRIS_PROTECTION_RESIDUELLE} (info) — totalement
 *       pris, protection 5 mois residuelle.</li>
 *   <li>{@code INELIGIBLE_STATUT_NON_COUVERT} (critique) — statut hors
 *       champ Loi 03/07/1978 (orienter vers regime INASTI ou
 *       statutaire).</li>
 *   <li>{@code INELIGIBLE_FILIATION_NON_ETABLIE} (critique) — lien
 *       parental non etabli.</li>
 *   <li>{@code DROIT_PERDU_DELAI_DEPASSE} (critique) — delai 4 mois
 *       depasse sans report legal.</li>
 *   <li>{@code INELIGIBLE_NAISSANCE_FUTURE} (warn) — calcul
 *       indicatif.</li>
 *   <li>{@code A_QUALIFIER} (neutre) — inputs ambigus.</li>
 * </ul>
 *
 * <p><b>Pre-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 / F-219 (statut travailleur, lien filiation, etape RH, dates
 * etat-civil et formalites RH non extractibles V1 du dossier salarie
 * generique du pipeline Travail BE).</p>
 */
@Component({
  selector: 'app-conge-paternite-naissance-be-section',
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
  templateUrl: './conge-paternite-naissance-be-section.component.html',
  styleUrl: './conge-paternite-naissance-be-section.component.scss',
})
export class CongePaterniteNaissanceBeSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'conge-paternite-naissance-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommee par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CONGE PATERNITE / NAISSANCE (BE)';
  static readonly TOOL_ICON = 'child_care';

  /** F-177 SF-177-12 / F-236 SF-236-02 — delegue au helper partage (parite runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return CongePaterniteNaissanceBeSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — regime FR distinct (25 jours calendaires LFSS 2021, IJSS CPAM). */
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
  result = signal<CongePaterniteNaissanceBeResponse | null>(null);

  // -- Champs du form (signals, edites via handlers). --
  statutTravailleur = signal<CongePaterniteNaissanceBeStatutTravailleur | null>(null);
  lienFiliation = signal<CongePaterniteNaissanceBeLienFiliation | null>(null);
  etapeProcedure = signal<CongePaterniteNaissanceBeEtape | null>(null);
  filiationEtablie = signal<boolean>(false);
  contratTravailEnCours = signal<boolean>(false);
  notificationEmployeurFaite = signal<boolean>(false);
  dateNaissance = signal<string | null>(null);
  dateNotificationEmployeur = signal<string | null>(null);
  joursDejaPrisOuvrables = signal<number | null>(null);
  dateFinPriseEffective = signal<string | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: CongePaterniteNaissanceBeService,
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
    // SF-219-31b V1 : pas de pre-fill IA (cf. helper doc) — pas de reaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.statutTravailleur() === null) return false;
    if (this.lienFiliation() === null) return false;
    if (this.etapeProcedure() === null) return false;
    const j = this.joursDejaPrisOuvrables();
    if (j !== null && j < 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onStatutTravailleurChange(value: CongePaterniteNaissanceBeStatutTravailleur): void {
    this.statutTravailleur.set(value);
  }
  onLienFiliationChange(value: CongePaterniteNaissanceBeLienFiliation): void {
    this.lienFiliation.set(value);
  }
  onEtapeProcedureChange(value: CongePaterniteNaissanceBeEtape): void {
    this.etapeProcedure.set(value);
  }
  onFiliationEtablieChange(value: boolean): void {
    this.filiationEtablie.set(value);
  }
  onContratTravailEnCoursChange(value: boolean): void {
    this.contratTravailEnCours.set(value);
  }
  onNotificationEmployeurFaiteChange(value: boolean): void {
    this.notificationEmployeurFaite.set(value);
  }
  onDateNaissanceChange(value: string | null): void {
    this.dateNaissance.set(value === '' ? null : value);
  }
  onDateNotificationEmployeurChange(value: string | null): void {
    this.dateNotificationEmployeur.set(value === '' ? null : value);
  }
  onJoursDejaPrisOuvrablesChange(value: number | string | null): void {
    if (value === null || value === '' || value === undefined) {
      this.joursDejaPrisOuvrables.set(null);
      return;
    }
    const num = typeof value === 'number' ? value : Number(value);
    this.joursDejaPrisOuvrables.set(Number.isNaN(num) ? null : num);
  }
  onDateFinPriseEffectiveChange(value: string | null): void {
    this.dateFinPriseEffective.set(value === '' ? null : value);
  }

  // -- Libelles humains --
  statutTravailleurLabel(value: CongePaterniteNaissanceBeStatutTravailleur | null): string {
    switch (value) {
      case 'SALARIE':
        return 'Salarie (Loi 03/07/1978 art. 30 paragr. 2)';
      case 'INDEPENDANT':
        return 'Independant (regime distinct INASTI AR 23/05/2019 — 20 jours forfaitaires)';
      case 'AGENT_PUBLIC_STATUTAIRE':
        return 'Agent public statutaire (AR 19/11/1998 art. 23 et s.)';
      case 'AUTRE':
        return 'Autre statut a qualifier';
      default:
        return '—';
    }
  }

  lienFiliationLabel(value: CongePaterniteNaissanceBeLienFiliation | null): string {
    switch (value) {
      case 'PERE_BIOLOGIQUE':
        return 'Pere biologique (presomption art. 315 ou reconnaissance art. 327 C. civ.)';
      case 'COPARENT_COMATERNITE':
        return 'Co-parent par comaternite (art. 325/2 C. civ.)';
      case 'COPARENT_RECONNAISSANCE':
        return 'Co-parent par reconnaissance (art. 327/1 C. civ.)';
      case 'COHABITANT_LEGAL_OU_MARIE':
        return 'Cohabitant legal ou conjoint sans filiation propre (Loi 07/04/2023)';
      case 'AUTRE':
        return 'Autre situation a qualifier';
      default:
        return '—';
    }
  }

  etapeProcedureLabel(value: CongePaterniteNaissanceBeEtape | null): string {
    switch (value) {
      case 'AVANT_NAISSANCE':
        return 'Avant la naissance — droit en formation';
      case 'NAISSANCE_SURVENUE':
        return 'Naissance survenue — delai 4 mois ouvert';
      case 'NOTIFICATION_EMPLOYEUR_FAITE':
        return 'Notification a l\'employeur effectuee (AR 17/10/1994)';
      case 'CONGE_EN_COURS_DE_PRISE':
        return 'Conge en cours de prise — protection 5 mois active';
      case 'CONGE_PRIS_ENTIEREMENT':
        return 'Conge totalement pris — protection 5 mois residuelle';
      case 'DELAI_QUATRE_MOIS_DEPASSE':
        return 'Delai 4 mois post-naissance depasse';
      default:
        return '—';
    }
  }

  /** Verdict en francais lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'ELIGIBLE_CONGE_OUVERT':
        return 'Droit au conge de naissance OUVERT (art. 30 paragr. 2 Loi 03/07/1978). Duree applicable selon la date de naissance : 20 jours ouvrables pour naissances a partir du 01/01/2023 (Loi 07/04/2023), 15 jours du 01/01/2021 au 31/12/2022, 10 jours avant le 01/01/2021. Conge a prendre dans les 4 mois suivant la naissance (al. 2). Indemnisation : 3 premiers jours a 100 pour cent par l\'employeur, jours restants a 82 pour cent par la mutuelle (INAMI) avec plafond AR 03/07/1996. Etapes : notifier l\'employeur AR 17/10/1994 avec justificatif (acte de naissance), planifier la prise, conserver les attestations.';
      case 'CONGE_EN_COURS_PROTECTION_ACTIVE':
        return 'Conge en cours de prise — la protection contre le licenciement de 5 mois est ACTIVE (art. 30 paragr. 4 Loi 03/07/1978). Le licenciement durant la periode protegee est annulable a defaut de motif etranger au conge (Cass. BE 16/03/2015 S.13.0094.F). Strategie : verifier la regularite de toute notification de licenciement intervenue depuis la notification de prise et jusqu\'a 5 mois apres le dernier jour pris ; conserver les preuves (planning prise, attestations RH). Si licenciement notifie : action en nullite + indemnite complementaire forfaitaire 6 mois de remuneration (jurisprudence constante).';
      case 'CONGE_PRIS_PROTECTION_RESIDUELLE':
        return 'Conge entierement pris — protection contre le licenciement 5 mois residuelle (art. 30 paragr. 4 Loi 03/07/1978). La protection court jusqu\'a 5 mois apres la fin de la prise effective. Strategie : surveiller toute notification de rupture (formelle ou indirecte par acte equivalent rupture) jusqu\'a l\'echeance ; conserver les attestations de fin de prise. Indemnite complementaire 6 mois de remuneration en cas de licenciement non fonde sur un motif etranger au conge.';
      case 'INELIGIBLE_STATUT_NON_COUVERT':
        return 'Statut hors champ Loi 03/07/1978 — orienter vers le regime applicable : INDEPENDANT releve du regime INASTI Loi 13/04/2019 + AR 23/05/2019 (20 jours forfaitaires indemnises a 100,69 EUR / jour au 01/01/2024, indexe) ; AGENT_PUBLIC_STATUTAIRE releve d\'AR 19/11/1998 art. 23 et s. (fonction publique federale, modalites sectorielles regionales / communales). Action : qualifier le statut avec precision, basculer vers l\'outil de calcul correspondant si livre, sinon directives administratives applicables.';
      case 'INELIGIBLE_FILIATION_NON_ETABLIE':
        return 'Lien parental non etabli — le conge de naissance suppose une filiation juridique (acte de naissance, reconnaissance art. 327 C. civ., comaternite art. 325/2, cohabitation legale ou mariage Loi 07/04/2023). Action : faire etablir le lien juridique (reconnaissance prenatale art. 327/1 C. civ., comaternite art. 325/2, mariage ou cohabitation legale art. 1475 et s. C. civ.) prealablement a la demande, sous peine de refus d\'octroi. Le droit redevient ouvert des l\'etablissement du lien si la fenetre 4 mois n\'est pas encore depassee.';
      case 'DROIT_PERDU_DELAI_DEPASSE':
        return 'Delai legal de 4 mois post-naissance DEPASSE sans prise complete et sans cas de report (art. 30 paragr. 2 al. 2-4 Loi 03/07/1978). Reports legaux exceptionnels : incapacite de travail de la mere durant la prise, hospitalisation de l\'enfant a la naissance (al. 4). Hors ces cas, le droit est perdu. Action : verifier si un report legal est applicable (preuve a constituer), sinon constat de perte. Strategie : si litige avec l\'employeur sur la possibilite de prise, action tribunal du travail art. 578 1 C. jud. pour faire constater le droit.';
      case 'INELIGIBLE_NAISSANCE_FUTURE':
        return 'Naissance future — calcul indicatif. Le droit s\'ouvrira au jour de la naissance (point de depart du delai de 4 mois al. 2 art. 30 paragr. 2 Loi 03/07/1978). Action : anticiper la notification a l\'employeur (AR 17/10/1994) en preparant un dossier RH complet (date prevue d\'accouchement, modalites de prise, justificatifs prets). Verifier la coherence du statut SALARIE (contrat en cours), l\'etablissement de la filiation juridique (reconnaissance prenatale conseillee).';
      case 'A_QUALIFIER':
        return 'Inputs ambigus — analyse complementaire avocat requise. Verifier la coherence entre statut professionnel (SALARIE / INDEPENDANT / AGENT_PUBLIC_STATUTAIRE / AUTRE), lien de filiation (etabli juridiquement), etape procedurale (avancement de la prise), et les dates (naissance, notification, fin de prise effective). Donnees minimales : date de naissance pour determiner la duree applicable (10 / 15 / 20 jours ouvrables selon le seuil legal de 2021 et 2023), date de notification a l\'employeur pour le point de depart de la protection 5 mois.';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict. */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'INELIGIBLE_STATUT_NON_COUVERT':
      case 'INELIGIBLE_FILIATION_NON_ETABLIE':
      case 'DROIT_PERDU_DELAI_DEPASSE':
        return 'verdict-critical';
      case 'INELIGIBLE_NAISSANCE_FUTURE':
        return 'verdict-warn';
      case 'ELIGIBLE_CONGE_OUVERT':
      case 'CONGE_EN_COURS_PROTECTION_ACTIVE':
      case 'CONGE_PRIS_PROTECTION_RESIDUELLE':
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
    if (!r) return 'child_care';
    switch (r.verdict) {
      case 'ELIGIBLE_CONGE_OUVERT':
        return 'check_circle';
      case 'CONGE_EN_COURS_PROTECTION_ACTIVE':
        return 'shield';
      case 'CONGE_PRIS_PROTECTION_RESIDUELLE':
        return 'verified';
      case 'INELIGIBLE_STATUT_NON_COUVERT':
        return 'block';
      case 'INELIGIBLE_FILIATION_NON_ETABLIE':
        return 'family_restroom';
      case 'DROIT_PERDU_DELAI_DEPASSE':
        return 'event_busy';
      case 'INELIGIBLE_NAISSANCE_FUTURE':
        return 'hourglass_empty';
      case 'A_QUALIFIER':
        return 'help_outline';
      default:
        return 'child_care';
    }
  }

  /** Libelle bref pour le badge du header. */
  verdictBadge(verdict: CongePaterniteNaissanceBeVerdict): string {
    switch (verdict) {
      case 'ELIGIBLE_CONGE_OUVERT':
        return 'CONGE OUVERT';
      case 'CONGE_EN_COURS_PROTECTION_ACTIVE':
        return 'PRISE EN COURS';
      case 'CONGE_PRIS_PROTECTION_RESIDUELLE':
        return 'CONGE PRIS';
      case 'INELIGIBLE_STATUT_NON_COUVERT':
        return 'STATUT HORS CHAMP';
      case 'INELIGIBLE_FILIATION_NON_ETABLIE':
        return 'FILIATION ABSENTE';
      case 'DROIT_PERDU_DELAI_DEPASSE':
        return 'DELAI DEPASSE';
      case 'INELIGIBLE_NAISSANCE_FUTURE':
        return 'NAISSANCE FUTURE';
      case 'A_QUALIFIER':
        return 'A QUALIFIER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: CongePaterniteNaissanceBeVerdict): string {
    switch (verdict) {
      case 'INELIGIBLE_STATUT_NON_COUVERT':
      case 'INELIGIBLE_FILIATION_NON_ETABLIE':
      case 'DROIT_PERDU_DELAI_DEPASSE':
        return 'badge-critical';
      case 'INELIGIBLE_NAISSANCE_FUTURE':
        return 'badge-warn';
      case 'ELIGIBLE_CONGE_OUVERT':
      case 'CONGE_EN_COURS_PROTECTION_ACTIVE':
      case 'CONGE_PRIS_PROTECTION_RESIDUELLE':
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

  /** Format nombre de jours ouvrables. */
  formatJours(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return `${value} jour${value > 1 ? 's' : ''} ouvrable${value > 1 ? 's' : ''}`;
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: CongePaterniteNaissanceBeRequest = {
      statutTravailleur: this.statutTravailleur()!,
      lienFiliation: this.lienFiliation()!,
      etapeProcedure: this.etapeProcedure()!,
      filiationEtablie: this.filiationEtablie(),
      contratTravailEnCours: this.contratTravailEnCours(),
      notificationEmployeurFaite: this.notificationEmployeurFaite(),
      dateNaissance: this.dateNaissance(),
      dateNotificationEmployeur: this.dateNotificationEmployeur(),
      joursDejaPrisOuvrables: this.joursDejaPrisOuvrables(),
      dateFinPriseEffective: this.dateFinPriseEffective(),
    };
    this.calculating.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse conge paternite / naissance calculee', 'OK', { duration: 2500 });
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
        this.statutTravailleur.set(r.statutTravailleur);
        this.lienFiliation.set(r.lienFiliation);
        this.etapeProcedure.set(r.etapeProcedure);
        this.filiationEtablie.set(r.filiationEtablie);
        this.contratTravailEnCours.set(r.contratTravailEnCours);
        this.notificationEmployeurFaite.set(r.notificationEmployeurFaite);
        this.dateNaissance.set(r.dateNaissance);
        this.dateNotificationEmployeur.set(r.dateNotificationEmployeur);
        this.joursDejaPrisOuvrables.set(r.joursDejaPrisOuvrables);
        this.dateFinPriseEffective.set(r.dateFinPriseEffective);
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
