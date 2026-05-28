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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import {
  BienEtreRpsConseillerPreventionEtape,
  BienEtreRpsConseillerPreventionModeDemande,
  BienEtreRpsConseillerPreventionRequest,
  BienEtreRpsConseillerPreventionResponse,
  BienEtreRpsConseillerPreventionTypeRisque,
  BienEtreRpsConseillerPreventionVerdict,
} from '../../core/models/bien-etre-rps-conseiller-prevention.model';
import {
  BienEtreRpsConseillerPreventionService,
} from '../../core/services/bien-etre-rps-conseiller-prevention.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  BienEtreRpsConseillerPreventionSectionPrefillRules,
} from './bien-etre-rps-conseiller-prevention-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-30b : outil decisionnel « Saisine du Conseiller en Prevention
 * Aspects Psychosociaux (CPAP) — procedure RPS interne BE » (F-219).
 *
 * <p>BE uniquement — <b>Loi du 04/08/1996</b> relative au bien-etre des
 * travailleurs lors de l'execution de leur travail (art. 32sexies
 * protection contre les represailles 12 mois, art. 32bis a
 * 32sexiesdecies definition harcelement / violence / autres RPS) +
 * <b>AR du 10/04/2014</b> relatif a la prevention des risques
 * psychosociaux au travail (art. 11 a 32 procedure interne CPAP) +
 * <b>Loi du 28/02/2014</b> introduisant la notion de risques
 * psychosociaux a l'art. 32/1 § 2 + <b>AR du 28/05/2003</b> relatif a
 * la surveillance de la sante des travailleurs (art. 27 carnet de
 * surveillance).</p>
 *
 * <p>Affiche par le panel F-IA-04 (tool_id
 * {@code bien-etre-rps-conseiller-prevention}, visibility ALWAYS_ON
 * priority 148 BE / DROIT_DU_TRAVAIL — au-dessus de
 * {@code mp-fedris-reconnaissance} (146, SF-219-28b),
 * {@code inastri-statut-travailleur-independant} (145, SF-219-27b)).</p>
 *
 * <h2>Verdict 8 etats</h2>
 * <ul>
 *   <li>{@code SAISINE_CONFORME} (info) — formalites prealables
 *       remplies.</li>
 *   <li>{@code SAISINE_INFORMELLE_EN_COURS} (neutral) — mediation
 *       interne, pas de delai legal.</li>
 *   <li>{@code SAISINE_FORMELLE_EN_COURS} (info) — enquete CPAP dans
 *       les 3 mois legaux.</li>
 *   <li>{@code AVIS_RENDU_DELAI_RESPECTE} (info) — avis motive rendu
 *       dans les 3 mois.</li>
 *   <li>{@code AVIS_RENDU_DELAI_DEPASSE} (warn) — avis rendu hors
 *       delai (irregularite).</li>
 *   <li>{@code NON_CONFORME_FORMALITES_MANQUANTES} (critical) —
 *       entretien / signature / AR manquants.</li>
 *   <li>{@code NON_CONFORME_PAS_DE_CONSEILLER} (critical) — aucun CPAP
 *       designe (manquement employeur art. 32sexies § 2).</li>
 *   <li>{@code A_QUALIFIER} (neutral) — inputs insuffisants.</li>
 * </ul>
 *
 * <p><b>Pre-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 / F-219 (typeRisque, modeDemande, etapeProcedure, 5 formalites
 * booleennes, dates depot et avis non extractibles du dossier salarie
 * generique — relevent du dossier procedural specifique CPAP).</p>
 */
@Component({
  selector: 'app-bien-etre-rps-conseiller-prevention-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule,
    MatListModule,
    MatProgressSpinnerModule,
    MatCheckboxModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './bien-etre-rps-conseiller-prevention-section.component.html',
  styleUrl: './bien-etre-rps-conseiller-prevention-section.component.scss',
})
export class BienEtreRpsConseillerPreventionSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'bien-etre-rps-conseiller-prevention';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommee par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'BIEN-ETRE RPS CONSEILLER PREVENTION (BE)';
  static readonly TOOL_ICON = 'psychology';

  /** F-177 SF-177-12 / F-236 SF-236-02 — delegue au helper partage (parite runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return BienEtreRpsConseillerPreventionSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'equivalent FR mecanique (regime distinct, pas de CPAP). */
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
  result = signal<BienEtreRpsConseillerPreventionResponse | null>(null);

  // -- Champs du form (signals, edites via handlers). --
  typeRisque = signal<BienEtreRpsConseillerPreventionTypeRisque | null>(null);
  modeDemande = signal<BienEtreRpsConseillerPreventionModeDemande | null>(null);
  etapeProcedure = signal<BienEtreRpsConseillerPreventionEtape | null>(null);
  conseillerIdentifie = signal<boolean>(false);
  entretienPrealableRealise = signal<boolean>(false);
  demandeEcriteSignee = signal<boolean>(false);
  accuseReceptionRecu = signal<boolean>(false);
  notificationEmployeurFaite = signal<boolean>(false);
  dateDepotDemande = signal<string | null>(null);
  dateAvisRendu = signal<string | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: BienEtreRpsConseillerPreventionService,
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
    // SF-219-30b V1 : pas de pre-fill IA (cf. helper doc) — pas de reaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  /**
   * Calcule joursDepuisDepot a partir de la date de depot et de la
   * date du jour Europe/Brussels (approximation locale au navigateur,
   * le backend recalcule de toute facon).
   */
  private computeJoursDepuisDepot(): number | null {
    const depot = this.dateDepotDemande();
    if (depot === null || depot === '') return null;
    const depotMs = Date.parse(depot);
    if (Number.isNaN(depotMs)) return null;
    const now = Date.now();
    const diffMs = now - depotMs;
    return Math.max(0, Math.floor(diffMs / (1000 * 60 * 60 * 24)));
  }

  formValid(): boolean {
    if (this.typeRisque() === null) return false;
    if (this.modeDemande() === null) return false;
    if (this.etapeProcedure() === null) return false;

    // Coherence date avis : doit etre renseignee si etape AVIS_RENDU
    // ou MESURES_PRISES_PAR_EMPLOYEUR (parite backend).
    const etape = this.etapeProcedure();
    const dAvis = this.dateAvisRendu();
    if ((etape === 'AVIS_RENDU' || etape === 'MESURES_PRISES_PAR_EMPLOYEUR')
        && (dAvis === null || dAvis === '')) {
      return false;
    }

    // Coherence dates : avis >= depot.
    const dDepot = this.dateDepotDemande();
    if (dDepot && dAvis && dDepot !== '' && dAvis !== '' && dAvis < dDepot) {
      return false;
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onTypeRisqueChange(value: BienEtreRpsConseillerPreventionTypeRisque): void {
    this.typeRisque.set(value);
  }
  onModeDemandeChange(value: BienEtreRpsConseillerPreventionModeDemande): void {
    this.modeDemande.set(value);
  }
  onEtapeProcedureChange(value: BienEtreRpsConseillerPreventionEtape): void {
    this.etapeProcedure.set(value);
  }
  onConseillerIdentifieChange(value: boolean): void {
    this.conseillerIdentifie.set(value);
  }
  onEntretienPrealableRealiseChange(value: boolean): void {
    this.entretienPrealableRealise.set(value);
  }
  onDemandeEcriteSigneeChange(value: boolean): void {
    this.demandeEcriteSignee.set(value);
  }
  onAccuseReceptionRecuChange(value: boolean): void {
    this.accuseReceptionRecu.set(value);
  }
  onNotificationEmployeurFaiteChange(value: boolean): void {
    this.notificationEmployeurFaite.set(value);
  }
  onDateDepotDemandeChange(value: string | null): void {
    this.dateDepotDemande.set(value);
  }
  onDateAvisRenduChange(value: string | null): void {
    this.dateAvisRendu.set(value);
  }

  // -- Libelles humains --
  typeRisqueLabel(value: BienEtreRpsConseillerPreventionTypeRisque | null): string {
    switch (value) {
      case 'HARCELEMENT_MORAL':
        return 'Harcelement moral — Loi 04/08/1996 art. 32bis (conduite abusive et repetee)';
      case 'HARCELEMENT_SEXUEL':
        return 'Harcelement sexuel — Loi 04/08/1996 art. 32bis al. 3';
      case 'VIOLENCE_AU_TRAVAIL':
        return 'Violence au travail — Loi 04/08/1996 art. 32bis al. 1';
      case 'AUTRES_RPS':
        return 'Autres RPS (stress, burn-out, conflits) — art. 32/1 § 2';
      default:
        return '—';
    }
  }

  modeDemandeLabel(value: BienEtreRpsConseillerPreventionModeDemande | null): string {
    switch (value) {
      case 'INFORMELLE':
        return 'Demande informelle — mediation interne confidentielle (art. 14 a 16)';
      case 'FORMELLE_INDIVIDUELLE':
        return 'Demande formelle individuelle — enquete ecrite avec rapport employeur (art. 16 a 22)';
      case 'FORMELLE_COLLECTIVE':
        return 'Demande formelle collective — analyse organisationnelle (art. 16 § 2)';
      default:
        return '—';
    }
  }

  etapeProcedureLabel(value: BienEtreRpsConseillerPreventionEtape | null): string {
    switch (value) {
      case 'INTENTION_DEMANDE':
        return 'Etape 1 — Intention, consultation personne de confiance (art. 31)';
      case 'ENTRETIEN_PREALABLE_REALISE':
        return 'Etape 2 — Entretien prealable obligatoire CPAP realise (art. 16 § 1, delai 10 jours)';
      case 'DEMANDE_INFORMELLE_EN_COURS':
        return 'Etape 3 — Demande informelle en cours (mediation interne)';
      case 'DEMANDE_FORMELLE_DEPOSEE':
        return 'Etape 4 — Demande formelle deposee, AR remis (3 mois enquete art. 22 § 1)';
      case 'AVIS_RENDU':
        return 'Etape 5 — Avis motive rendu par CPAP (2 mois employeur art. 32)';
      case 'MESURES_PRISES_PAR_EMPLOYEUR':
        return 'Etape 6 — Mesures notifiees, procedure interne cloturee';
      default:
        return '—';
    }
  }

  /** Verdict en francais lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'SAISINE_CONFORME':
        return 'Saisine conforme — toutes les formalites prealables sont remplies (CPAP identifie, entretien prealable realise dans le delai 10 jours art. 16 § 1, demande ecrite et signee par le travailleur art. 16 § 2 si formelle, accuse de reception remis art. 16 § 4, notification employeur art. 17 § 1). La protection 12 mois contre les represailles art. 32sexies court a partir du depot de la demande. Etapes : suivre le delai 3 mois enquete art. 22 § 1 (prolongation motivee 6 mois max) ; en cas d\'avis rendu, notifier les mesures dans les 2 mois art. 32.';
      case 'SAISINE_INFORMELLE_EN_COURS':
        return 'Demande informelle en cours — mediation interne confidentielle (art. 14 a 16 AR 10/04/2014), pas de delai legal strict, pas de rapport au employeur. Le travailleur peut basculer vers la formelle a tout moment. Conseil : verifier que le CPAP documente la mediation (notes, propositions, refus) — utile en cas de bascule formelle ulterieure et pour evaluer la mauvaise foi employeur.';
      case 'SAISINE_FORMELLE_EN_COURS':
        return 'Demande formelle deposee, enquete CPAP en cours dans les 3 mois legaux (art. 22 § 1 AR 10/04/2014). La protection 12 mois contre les represailles art. 32sexies est active. Conseil : suivre la duree de l\'enquete ; si depassement injustifie au-dela de 3 mois (sans prolongation motivee notifiee art. 22 § 2, 6 mois max), saisir l\'Inspection Controle du bien-etre au travail (CBE) pour denoncer l\'irregularite.';
      case 'AVIS_RENDU_DELAI_RESPECTE':
        return 'Avis motive CPAP rendu dans les 3 mois legaux (art. 22 § 1 AR 10/04/2014) ou dans la prolongation motivee de 6 mois max (art. 22 § 2). L\'employeur dispose de 2 mois (art. 32) pour notifier les mesures prises. La protection 12 mois art. 32sexies court a partir du depot. Conseil : suivre la notification des mesures employeur ; en cas de non-conformite ou d\'inaction, saisir l\'Inspection CBE (Controle du bien-etre au travail) puis le tribunal du travail art. 578 C. jud.';
      case 'AVIS_RENDU_DELAI_DEPASSE':
        return 'Avis motive rendu hors delai 3 mois legaux (art. 22 § 1 AR 10/04/2014) — irregularite procedurale du CPAP, mais l\'avis reste valide et exploitable. La protection 12 mois art. 32sexies reste active a compter du depot. Conseil : documenter le depassement (date depot vs date avis), saisir l\'Inspection CBE pour denoncer le retard, et exploiter l\'avis dans les voies externes (tribunal du travail art. 578 C. jud.) en mentionnant l\'irregularite comme element de mauvaise foi de l\'employeur ou du CPAP designe.';
      case 'NON_CONFORME_FORMALITES_MANQUANTES':
        return 'Saisine non conforme — une ou plusieurs formalites prealables sont manquantes : entretien prealable obligatoire avant demande formelle non realise (art. 16 § 1 AR 10/04/2014, delai 10 jours), ou demande formelle non ecrite / non signee par le travailleur (art. 16 § 2), ou accuse de reception manquant (art. 16 § 4). Risque : irrecevabilite procedurale de la demande formelle. Conseil : regulariser les formalites par lettre recommandee ou e-mail trace au CPAP avec demande de re-instruire la demande, OU basculer en informelle (qui ne suit pas ces formalites), OU saisir directement l\'Inspection CBE qui peut requalifier la procedure.';
      case 'NON_CONFORME_PAS_DE_CONSEILLER':
        return 'Aucun CPAP identifie — manquement employeur a l\'obligation legale art. 32sexies § 2 Loi 04/08/1996 (employeur doit designer un CPAP interne ou recourir a un SEPP — Service Externe de Prevention et de Protection au travail). L\'employeur commet une infraction Code penal social BE niveau 3 (art. 122 et s. Code penal social, articulation outil code-penal-social-be). Conseil : mettre l\'employeur en demeure de designer un CPAP par lettre recommandee, saisir l\'Inspection CBE qui peut sanctionner et imposer un CPAP externe, et en cas d\'urgence saisir le tribunal du travail en refere art. 584 C. jud. pour ordonner la designation.';
      case 'A_QUALIFIER':
        return 'Qualification ouverte — inputs insuffisants ou contradictoires (etape AVIS_RENDU declaree mais date avis non renseignee, ou incoherences entre etape et formalites). L\'avocat doit completer les elements (compte-rendu entretien prealable date, formulaire de demande, AR du CPAP, notification employeur, avis motive si rendu, mesures employeur si notifiees) avant de relancer l\'analyse.';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict. */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'NON_CONFORME_FORMALITES_MANQUANTES':
      case 'NON_CONFORME_PAS_DE_CONSEILLER':
        return 'verdict-critical';
      case 'AVIS_RENDU_DELAI_DEPASSE':
        return 'verdict-warn';
      case 'SAISINE_CONFORME':
      case 'SAISINE_FORMELLE_EN_COURS':
      case 'AVIS_RENDU_DELAI_RESPECTE':
        return 'verdict-info';
      case 'SAISINE_INFORMELLE_EN_COURS':
      case 'A_QUALIFIER':
        return 'verdict-neutral';
      default:
        return '';
    }
  }

  /** Icone Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'psychology';
    switch (r.verdict) {
      case 'SAISINE_CONFORME':
        return 'check_circle';
      case 'SAISINE_INFORMELLE_EN_COURS':
        return 'forum';
      case 'SAISINE_FORMELLE_EN_COURS':
        return 'pending_actions';
      case 'AVIS_RENDU_DELAI_RESPECTE':
        return 'verified';
      case 'AVIS_RENDU_DELAI_DEPASSE':
        return 'schedule';
      case 'NON_CONFORME_FORMALITES_MANQUANTES':
        return 'report';
      case 'NON_CONFORME_PAS_DE_CONSEILLER':
        return 'person_off';
      case 'A_QUALIFIER':
        return 'help_outline';
      default:
        return 'psychology';
    }
  }

  /** Libelle bref pour le badge du header. */
  verdictBadge(verdict: BienEtreRpsConseillerPreventionVerdict): string {
    switch (verdict) {
      case 'SAISINE_CONFORME':
        return 'CONFORME';
      case 'SAISINE_INFORMELLE_EN_COURS':
        return 'INFORMELLE EN COURS';
      case 'SAISINE_FORMELLE_EN_COURS':
        return 'FORMELLE EN COURS';
      case 'AVIS_RENDU_DELAI_RESPECTE':
        return 'AVIS DELAI OK';
      case 'AVIS_RENDU_DELAI_DEPASSE':
        return 'AVIS HORS DELAI';
      case 'NON_CONFORME_FORMALITES_MANQUANTES':
        return 'FORMALITES KO';
      case 'NON_CONFORME_PAS_DE_CONSEILLER':
        return 'PAS DE CPAP';
      case 'A_QUALIFIER':
        return 'A QUALIFIER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: BienEtreRpsConseillerPreventionVerdict): string {
    switch (verdict) {
      case 'NON_CONFORME_FORMALITES_MANQUANTES':
      case 'NON_CONFORME_PAS_DE_CONSEILLER':
        return 'badge-critical';
      case 'AVIS_RENDU_DELAI_DEPASSE':
        return 'badge-warn';
      case 'SAISINE_CONFORME':
      case 'SAISINE_FORMELLE_EN_COURS':
      case 'AVIS_RENDU_DELAI_RESPECTE':
        return 'badge-info';
      case 'SAISINE_INFORMELLE_EN_COURS':
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

  /** Format jours -> libelle francais. */
  formatJours(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    if (value === 0) return '0 jour (jour meme)';
    const unit = value === 1 || value === -1 ? 'jour' : 'jours';
    return `${value.toLocaleString('fr-FR')} ${unit}`;
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: BienEtreRpsConseillerPreventionRequest = {
      typeRisque: this.typeRisque()!,
      modeDemande: this.modeDemande()!,
      etapeProcedure: this.etapeProcedure()!,
      conseillerIdentifie: this.conseillerIdentifie(),
      entretienPrealableRealise: this.entretienPrealableRealise(),
      demandeEcriteSignee: this.demandeEcriteSignee(),
      accuseReceptionRecu: this.accuseReceptionRecu(),
      notificationEmployeurFaite: this.notificationEmployeurFaite(),
      dateDepotDemande: this.dateDepotDemande(),
      dateAvisRendu: this.dateAvisRendu(),
      joursDepuisDepot: this.computeJoursDepuisDepot(),
    };
    this.calculating.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse saisine CPAP calculee', 'OK', { duration: 2500 });
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
        this.typeRisque.set(r.typeRisque);
        this.modeDemande.set(r.modeDemande);
        this.etapeProcedure.set(r.etapeProcedure);
        this.conseillerIdentifie.set(r.conseillerIdentifie);
        this.entretienPrealableRealise.set(r.entretienPrealableRealise);
        this.demandeEcriteSignee.set(r.demandeEcriteSignee);
        this.accuseReceptionRecu.set(r.accuseReceptionRecu);
        this.notificationEmployeurFaite.set(r.notificationEmployeurFaite);
        this.dateDepotDemande.set(r.dateDepotDemande);
        this.dateAvisRendu.set(r.dateAvisRendu);
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
