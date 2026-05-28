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
  MpFedrisReconnaissanceCausalite,
  MpFedrisReconnaissanceExposition,
  MpFedrisReconnaissanceRequest,
  MpFedrisReconnaissanceResponse,
  MpFedrisReconnaissanceTypeMaladie,
  MpFedrisReconnaissanceVerdict,
} from '../../core/models/mp-fedris-reconnaissance.model';
import {
  MpFedrisReconnaissanceService,
} from '../../core/services/mp-fedris-reconnaissance.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  MpFedrisReconnaissanceSectionPrefillRules,
} from './mp-fedris-reconnaissance-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-28b : outil decisionnel « Reconnaissance d'une maladie
 * professionnelle Fedris BE » (F-219).
 *
 * <p>BE uniquement — <b>Lois coordonnees du 03/06/1970</b> relatives
 * a la prevention des maladies professionnelles et a la reparation
 * des dommages resultant de celles-ci (art. 30 liste fermee, art.
 * 30bis systeme ouvert preuve causalite directe et determinante
 * introduit par la Loi du 29/12/1990, art. 31 prescription triennale,
 * art. 32 presomption juridique de causalite, art. 53 recours
 * tribunal du travail) + <b>AR du 28/03/1969</b> dressant la liste
 * des maladies professionnelles donnant lieu a reparation modifie
 * par l'AR du 06/12/2018 (sections 1.x agents chimiques, 2.x agents
 * physiques notamment 2.1 vibrations, 2.2 bruit, 2.3 amiante, 3.x
 * agents biologiques, 4.x maladies cutanees et respiratoires, 5.x
 * maladies musculosquelettiques, 6.x cancers professionnels) +
 * <b>AR du 16/12/1985</b> systeme ouvert + <b>Loi du 11/01/2018</b>
 * portant l'organisation administrative de Fedris fusion FMP / FAT
 * au 01/01/2017 + jurisprudence Cass. BE 28/05/2008 S.07.0033.F,
 * Cass. BE 04/02/2002 S.01.0095.F, Cass. BE 13/12/1989 Pas. 1990 I
 * 451, Cour const. arret 51/2008 du 13/03/2008.</p>
 *
 * <p>Affiche par le panel F-IA-04 (tool_id {@code mp-fedris-reconnaissance},
 * visibility ALWAYS_ON priority 146 BE / DROIT_DU_TRAVAIL — au-dessus
 * de {@code inastri-statut-travailleur-independant} (145, SF-219-27b),
 * {@code travail-noir-be-dimona} (144, SF-219-26b),
 * {@code auditorat-travail-be} (143, SF-219-25b)).</p>
 *
 * <h2>Verdict 6 etats</h2>
 * <ul>
 *   <li>{@code MALADIE_LISTE_FERMEE_PRESOMPTION} (info) — liste +
 *       exposition averee, presomption art. 32 joue (favorable).</li>
 *   <li>{@code MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE} (warn) —
 *       liste mais exposition non demontree.</li>
 *   <li>{@code SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE} (info) —
 *       hors liste, causalite etablie (art. 30bis, favorable).</li>
 *   <li>{@code SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE} (critique) —
 *       hors liste, causalite insuffisante (refus probable Fedris).</li>
 *   <li>{@code DECLARATION_PRESCRITE} (critique) — prescription
 *       triennale art. 31 depassee.</li>
 *   <li>{@code A_QUALIFIER} (neutre) — inputs ambigus.</li>
 * </ul>
 *
 * <p><b>Pre-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 / F-219 (typeMaladie, codeMaladieListe, libelleMaladie, dates,
 * exposition, causalite non extractibles du dossier salarie generique).</p>
 */
@Component({
  selector: 'app-mp-fedris-reconnaissance-section',
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
  templateUrl: './mp-fedris-reconnaissance-section.component.html',
  styleUrl: './mp-fedris-reconnaissance-section.component.scss',
})
export class MpFedrisReconnaissanceSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'mp-fedris-reconnaissance';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommee par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'MP FEDRIS RECONNAISSANCE (BE)';
  static readonly TOOL_ICON = 'medical_services';

  /** F-177 SF-177-12 / F-236 SF-236-02 — delegue au helper partage (parite runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return MpFedrisReconnaissanceSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'equivalent FR mecanique (CPAM distincte). */
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
  result = signal<MpFedrisReconnaissanceResponse | null>(null);

  // -- Champs du form (signals, edites via handlers). --
  typeMaladie = signal<MpFedrisReconnaissanceTypeMaladie | null>(null);
  codeMaladieListe = signal<string | null>(null);
  libelleMaladie = signal<string | null>(null);
  dateConstatationMedicale = signal<string | null>(null);
  dateConnaissanceProfessionnel = signal<string | null>(null);
  dateDeclarationEnvisagee = signal<string | null>(null);
  exposition = signal<MpFedrisReconnaissanceExposition | null>(null);
  causalite = signal<MpFedrisReconnaissanceCausalite | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: MpFedrisReconnaissanceService,
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
    // SF-219-28b V1 : pas de pre-fill IA (cf. helper doc) — pas de reaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.typeMaladie() === null) return false;
    const lib = this.libelleMaladie();
    if (lib === null || lib.trim() === '') return false;
    if (lib.length > 255) return false;
    if (this.dateConstatationMedicale() === null
        || this.dateConstatationMedicale() === '') return false;
    if (this.exposition() === null) return false;
    if (this.causalite() === null) return false;
    // Si LISTE_FERMEE, codeMaladieListe est requis.
    if (this.typeMaladie() === 'LISTE_FERMEE') {
      const code = this.codeMaladieListe();
      if (code === null || code.trim() === '') return false;
      if (code.length > 255) return false;
    }
    // Coherence dates : connaissance professionnel >= constatation
    // medicale (parite backend).
    const dCM = this.dateConstatationMedicale();
    const dCP = this.dateConnaissanceProfessionnel();
    if (dCM && dCP && dCP !== '' && dCP < dCM) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onTypeMaladieChange(value: MpFedrisReconnaissanceTypeMaladie): void {
    this.typeMaladie.set(value);
    // Si HORS_LISTE, on remet causalite=NON_APPLICABLE seulement quand
    // l'utilisateur l'a deja positionnee — sinon on laisse l'avocat
    // choisir. Pas d'auto-reset systematique pour ne pas effacer.
  }
  onCodeMaladieListeChange(value: string | null): void {
    this.codeMaladieListe.set(value === '' ? null : value);
  }
  onLibelleMaladieChange(value: string | null): void {
    this.libelleMaladie.set(value === '' ? null : value);
  }
  onDateConstatationMedicaleChange(value: string | null): void {
    this.dateConstatationMedicale.set(value);
  }
  onDateConnaissanceProfessionnelChange(value: string | null): void {
    this.dateConnaissanceProfessionnel.set(value);
  }
  onDateDeclarationEnvisageeChange(value: string | null): void {
    this.dateDeclarationEnvisagee.set(value);
  }
  onExpositionChange(value: MpFedrisReconnaissanceExposition): void {
    this.exposition.set(value);
  }
  onCausaliteChange(value: MpFedrisReconnaissanceCausalite): void {
    this.causalite.set(value);
  }

  // -- Libelles humains --
  typeMaladieLabel(value: MpFedrisReconnaissanceTypeMaladie | null): string {
    switch (value) {
      case 'LISTE_FERMEE':
        return 'Liste fermee AR 28/03/1969 (presomption art. 32 lois coordonnees)';
      case 'HORS_LISTE':
        return 'Hors liste — systeme ouvert art. 30bis (preuve causalite directe et determinante)';
      default:
        return '—';
    }
  }

  expositionLabel(value: MpFedrisReconnaissanceExposition | null): string {
    switch (value) {
      case 'AVEREE':
        return 'Averee — exposition documentee et conforme aux conditions de la section';
      case 'INSUFFISANTE':
        return 'Insuffisante — exposition partielle ou hors champ de la liste';
      case 'INDETERMINEE':
        return 'Indeterminee — caracterisation a etablir avant declaration';
      default:
        return '—';
    }
  }

  causaliteLabel(value: MpFedrisReconnaissanceCausalite | null): string {
    switch (value) {
      case 'DIRECTE_DETERMINANTE':
        return 'Directe et determinante — exposition cause directe et essentielle';
      case 'INSUFFISANTE':
        return 'Insuffisante — lien causal possible mais non demontre comme direct et determinant';
      case 'INDETERMINEE':
        return 'Indeterminee — expertise medicale Fedris a demander';
      case 'NON_APPLICABLE':
        return 'Non applicable (liste fermee — presomption art. 32 dispense la preuve)';
      default:
        return '—';
    }
  }

  /** Verdict en francais lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'MALADIE_LISTE_FERMEE_PRESOMPTION':
        return 'Maladie liste fermee + exposition averee — presomption juridique de causalite art. 32 lois coordonnees 03/06/1970 applicable (Cass. BE 13/12/1989). Fedris est tenu de reconnaitre la maladie sauf preuve contraire. Etapes : formulaire 501 victime + 503 employeur + rapports medicaux + CV professionnel + attestations employeurs successifs ; declaration immediate art. 24 ; examen medical Fedris ; calcul indemnites art. 34-37.';
      case 'MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE':
        return 'Maladie inscrite sur la liste fermee AR 28/03/1969 mais exposition professionnelle non suffisamment documentee au regard des conditions de duree minimale et d\'intensite de la section concernee. La presomption art. 32 ne joue pas. Pistes : completer la documentation (CV professionnel, attestations employeurs, fiches de poste, mesures d\'ambiance, registre amiante art. 23 AR 16/03/2006) ; basculer en systeme ouvert art. 30bis ; ou provoquer le refus pour saisir le tribunal du travail art. 580 1° C. jud.';
      case 'SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE':
        return 'Maladie hors liste mais causalite directe et determinante etablie au standard Cass. BE 28/05/2008 S.07.0033.F (art. 30bis lois coordonnees 03/06/1970, systeme ouvert introduit par la Loi du 29/12/1990). Pas de presomption art. 32 — la charge de la preuve incombe a la victime mais reste supportable avec expertise medicale concordante. Etapes : dossier complet + declaration Fedris + expertise medicale contradictoire ; en cas de refus, recours tribunal du travail.';
      case 'SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE':
        return 'Maladie hors liste et lien causal direct et determinant non etabli au standard Cass. BE 28/05/2008 S.07.0033.F / Cass. BE 04/02/2002 S.01.0095.F (simple facteur favorisant insuffisant ; caractere direct et essentiel exige). Pronostic Fedris : refus probable. Pistes : renforcer l\'expertise medicale (specialiste medecin du travail INAMI, pneumologue, rhumatologue, neurologue) ; documenter precisement l\'exposition ; exclure les facteurs personnels predominants ; saisir le tribunal du travail avec expertise judiciaire art. 962-991 C. jud.';
      case 'DECLARATION_PRESCRITE':
        return 'Delai de prescription triennale de l\'action en reparation art. 31 lois coordonnees 03/06/1970 depasse — l\'action Fedris est en principe eteinte. L\'avocat doit verifier : (i) la date exacte de connaissance du caractere professionnel (point de depart) ; (ii) les causes d\'interruption art. 2244 anc. / 2.5.5 nouv. C. civ. ; (iii) les causes de suspension art. 2251 anc. C. civ. ; (iv) la theorie de la decouverte progressive de l\'aggravation (Cass. BE 24/04/1995) ; (v) la voie residuelle responsabilite civile art. 1382 anc. / 5.83 nouv. C. civ. (prescription 5 ans art. 2262bis / 5.149).';
      case 'A_QUALIFIER':
        return 'Qualification ouverte — inputs ambigus (exposition INDETERMINEE en liste fermee, ou causalite INDETERMINEE / NON_APPLICABLE en systeme ouvert). L\'avocat doit reunir les elements (rapport medical specialiste, analyse d\'exposition quantitative, bibliographie scientifique etablissant la relation dose-effet) avant de declencher la declaration Fedris.';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict. */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE':
      case 'DECLARATION_PRESCRITE':
        return 'verdict-critical';
      case 'MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE':
        return 'verdict-warn';
      case 'MALADIE_LISTE_FERMEE_PRESOMPTION':
      case 'SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE':
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
    if (!r) return 'medical_services';
    switch (r.verdict) {
      case 'MALADIE_LISTE_FERMEE_PRESOMPTION':
        return 'check_circle';
      case 'MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE':
        return 'description';
      case 'SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE':
        return 'verified';
      case 'SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE':
        return 'report';
      case 'DECLARATION_PRESCRITE':
        return 'event_busy';
      case 'A_QUALIFIER':
        return 'help_outline';
      default:
        return 'medical_services';
    }
  }

  /** Libelle bref pour le badge du header. */
  verdictBadge(verdict: MpFedrisReconnaissanceVerdict): string {
    switch (verdict) {
      case 'MALADIE_LISTE_FERMEE_PRESOMPTION':
        return 'PRESOMPTION ART. 32';
      case 'MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE':
        return 'EXPOSITION INSUFFISANTE';
      case 'SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE':
        return 'CAUSALITE ETABLIE';
      case 'SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE':
        return 'CAUSALITE INSUFFISANTE';
      case 'DECLARATION_PRESCRITE':
        return 'PRESCRITE';
      case 'A_QUALIFIER':
        return 'A QUALIFIER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: MpFedrisReconnaissanceVerdict): string {
    switch (verdict) {
      case 'SYSTEME_OUVERT_CAUSALITE_INSUFFISANTE':
      case 'DECLARATION_PRESCRITE':
        return 'badge-critical';
      case 'MALADIE_LISTE_FERMEE_EXPOSITION_INSUFFISANTE':
        return 'badge-warn';
      case 'MALADIE_LISTE_FERMEE_PRESOMPTION':
      case 'SYSTEME_OUVERT_CAUSALITE_DIRECTE_DETERMINANTE':
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

  /** Format jours avant prescription (signe + libelle). */
  formatJoursPrescription(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    if (value === 0) return '0 jour (echeance jour meme)';
    if (value > 0) {
      const unit = value === 1 ? 'jour' : 'jours';
      return `${value.toLocaleString('fr-FR')} ${unit} restant(s) avant prescription`;
    }
    const depasses = Math.abs(value);
    const unit = depasses === 1 ? 'jour' : 'jours';
    return `${depasses.toLocaleString('fr-FR')} ${unit} ecoule(s) depuis l\'expiration`;
  }

  /** Boolean -> Oui / Non / —. */
  formatBoolean(value: boolean | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value ? 'Oui' : 'Non';
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: MpFedrisReconnaissanceRequest = {
      typeMaladie: this.typeMaladie()!,
      codeMaladieListe: this.codeMaladieListe(),
      libelleMaladie: this.libelleMaladie()!,
      dateConstatationMedicale: this.dateConstatationMedicale()!,
      dateConnaissanceProfessionnel: this.dateConnaissanceProfessionnel(),
      dateDeclarationEnvisagee: this.dateDeclarationEnvisagee(),
      exposition: this.exposition()!,
      causalite: this.causalite()!,
    };
    this.calculating.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse MP Fedris calculee', 'OK', { duration: 2500 });
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
        this.typeMaladie.set(r.typeMaladie);
        this.codeMaladieListe.set(r.codeMaladieListe);
        this.libelleMaladie.set(r.libelleMaladie);
        this.dateConstatationMedicale.set(r.dateConstatationMedicale);
        this.dateConnaissanceProfessionnel.set(r.dateConnaissanceProfessionnel);
        this.dateDeclarationEnvisagee.set(r.dateDeclarationEnvisagee);
        this.exposition.set(r.exposition);
        this.causalite.set(r.causalite);
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
