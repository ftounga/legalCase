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
import { MatCheckboxModule } from '@angular/material/checkbox';
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
  InastriStatutTravailleurIndependantLiberte,
  InastriStatutTravailleurIndependantRequest,
  InastriStatutTravailleurIndependantResponse,
  InastriStatutTravailleurIndependantSecteur,
  InastriStatutTravailleurIndependantStatutDeclare,
  InastriStatutTravailleurIndependantVerdict,
  InastriStatutTravailleurIndependantVolonte,
} from '../../core/models/inastri-statut-travailleur-independant.model';
import {
  InastriStatutTravailleurIndependantService,
} from '../../core/services/inastri-statut-travailleur-independant.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  InastriStatutTravailleurIndependantSectionPrefillRules,
} from './inastri-statut-travailleur-independant-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-27b : outil décisionnel « INASTI — statut travailleur
 * indépendant et qualification salarié / indépendant » (F-219).
 *
 * <p>BE uniquement — <b>Loi du 27/06/1969</b> révisant l'arrêté-loi
 * du 28/12/1944 (sécurité sociale des travailleurs salariés) + <b>AR
 * n° 38 du 27/07/1967</b> organisant le statut social des travailleurs
 * indépendants + <b>AR du 19/12/1967</b> portant règlement général
 * d'exécution + <b>Loi-programme I du 27/12/2006</b> art. 328 à 333
 * « doctrine Bart Buysse » (4 critères généraux + présomption simple
 * de salariat art. 328 § 1) + art. 337/1 à 337/3 critères sectoriels
 * (Loi 25/08/2012, extension agriculture / horticulture AR
 * 29/10/2013 ; Cour const. arrêt 167/2013 du 19/12/2013).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id
 * {@code inastri-statut-travailleur-independant}, visibility ALWAYS_ON
 * priority 145 BE / DROIT_DU_TRAVAIL — au-dessus de
 * {@code travail-noir-be-dimona} (144, SF-219-26b),
 * {@code auditorat-travail-be} (143, SF-219-25b),
 * {@code code-penal-social-be} (142, SF-219-24b)).</p>
 *
 * <h2>Verdict 5 états</h2>
 * <ul>
 *   <li>{@code INDEPENDANT_CONFIRME} (info) — qualification d'indépendant
 *       confirmée.</li>
 *   <li>{@code SALARIE_REQUALIFIE} (critique) — relation à requalifier
 *       en salariat.</li>
 *   <li>{@code FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE} (critique) —
 *       présomption sectorielle art. 337/2 § 1 déclenchée.</li>
 *   <li>{@code PRESUMPTION_GENERALE_SALARIAT} (critique) — présomption
 *       générale art. 328 § 1 C. pén. soc. mobilisable.</li>
 *   <li>{@code A_QUALIFIER} (neutre) — analyse complémentaire avocat
 *       requise.</li>
 * </ul>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 / F-219 (volonté des parties, modalités d'exécution, critères
 * de subordination, secteur, statut administratif, présence d'autres
 * clients non extractibles du dossier salarié — relèvent de l'analyse
 * contractuelle in concreto par l'avocat).</p>
 */
@Component({
  selector: 'app-inastri-statut-travailleur-independant-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule,
    MatListModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './inastri-statut-travailleur-independant-section.component.html',
  styleUrl: './inastri-statut-travailleur-independant-section.component.scss',
})
export class InastriStatutTravailleurIndependantSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'inastri-statut-travailleur-independant';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'INASTI STATUT INDEPENDANT (BE)';
  static readonly TOOL_ICON = 'badge';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return InastriStatutTravailleurIndependantSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR mécanique (présomption non-salariat C. trav. L. 8221-6 distincte). */
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
  result = signal<InastriStatutTravailleurIndependantResponse | null>(null);

  // -- Champs du form (signals, édités via handlers). --
  volonteParties = signal<InastriStatutTravailleurIndependantVolonte | null>(null);
  liberteOrganisationTemps = signal<InastriStatutTravailleurIndependantLiberte | null>(null);
  liberteOrganisationTravail = signal<InastriStatutTravailleurIndependantLiberte | null>(null);
  controleHierarchique = signal<InastriStatutTravailleurIndependantLiberte | null>(null);
  secteur = signal<InastriStatutTravailleurIndependantSecteur | null>(null);
  nbCriteresSectorielsSalarie = signal<number | null>(null);
  statutDeclare = signal<InastriStatutTravailleurIndependantStatutDeclare | null>(null);
  dimonaPresente = signal<boolean | null>(null);
  autreClientPresent = signal<boolean | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: InastriStatutTravailleurIndependantService,
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
    // SF-219-27b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.volonteParties() === null) return false;
    if (this.liberteOrganisationTemps() === null) return false;
    if (this.liberteOrganisationTravail() === null) return false;
    if (this.controleHierarchique() === null) return false;
    if (this.secteur() === null) return false;
    if (this.nbCriteresSectorielsSalarie() === null) return false;
    const nb = this.nbCriteresSectorielsSalarie() as number;
    if (nb < 0 || nb > 9) return false;
    if (this.statutDeclare() === null) return false;
    if (this.dimonaPresente() === null) return false;
    if (this.autreClientPresent() === null) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onVolontePartiesChange(value: InastriStatutTravailleurIndependantVolonte): void {
    this.volonteParties.set(value);
  }
  onLiberteOrganisationTempsChange(value: InastriStatutTravailleurIndependantLiberte): void {
    this.liberteOrganisationTemps.set(value);
  }
  onLiberteOrganisationTravailChange(value: InastriStatutTravailleurIndependantLiberte): void {
    this.liberteOrganisationTravail.set(value);
  }
  onControleHierarchiqueChange(value: InastriStatutTravailleurIndependantLiberte): void {
    this.controleHierarchique.set(value);
  }
  onSecteurChange(value: InastriStatutTravailleurIndependantSecteur): void {
    this.secteur.set(value);
  }
  onNbCriteresSectorielsSalarieChange(value: string | number | null): void {
    this.nbCriteresSectorielsSalarie.set(this.toNumberOrNull(value));
  }
  onStatutDeclareChange(value: InastriStatutTravailleurIndependantStatutDeclare): void {
    this.statutDeclare.set(value);
  }
  onDimonaPresenteChange(value: boolean): void {
    this.dimonaPresente.set(value);
  }
  onAutreClientPresentChange(value: boolean): void {
    this.autreClientPresent.set(value);
  }

  private toNumberOrNull(value: string | number | null): number | null {
    if (value === null || value === '') return null;
    const n = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(n) ? n : null;
  }

  // -- Libellés humains --
  volontePartiesLabel(value: InastriStatutTravailleurIndependantVolonte | null): string {
    switch (value) {
      case 'INDEPENDANT_EXPLICITE':
        return 'Convention écrite expressément indépendante (contrat d\'entreprise / prestations / INASTI)';
      case 'SALARIE_EXPLICITE':
        return 'Convention écrite expressément salariée (contrat de travail Loi 03/07/1978)';
      case 'IMPLICITE_OU_AMBIGUE':
        return 'Qualification implicite ou ambiguë (déduction des comportements)';
      default:
        return '—';
    }
  }

  liberteLabel(value: InastriStatutTravailleurIndependantLiberte | null): string {
    switch (value) {
      case 'TOTALE_INDEPENDANT':
        return 'Liberté totale ou absence de contrôle (indice indépendant)';
      case 'PARTIELLE':
        return 'Liberté partielle ou contrôle ponctuel (indice neutre)';
      case 'AUCUNE_SALARIE':
        return 'Aucune liberté ou contrôle hiérarchique régulier (indice salarié)';
      default:
        return '—';
    }
  }

  secteurLabel(value: InastriStatutTravailleurIndependantSecteur | null): string {
    switch (value) {
      case 'CONSTRUCTION':
        return 'Construction (CP 124 — AR 07/06/2013, 9 critères sectoriels art. 337/2 § 1, 1°)';
      case 'TRANSPORT_DE_CHOSES':
        return 'Transport de choses pour compte de tiers (art. 337/2 § 1, 2°)';
      case 'GARDIENNAGE':
        return 'Gardiennage et sécurité privée (art. 337/2 § 1, 3°)';
      case 'NETTOYAGE':
        return 'Nettoyage (art. 337/2 § 1, 4°)';
      case 'AGRICULTURE_HORTICULTURE':
        return 'Agriculture / horticulture (art. 337/2 § 1, 5° — AR 29/10/2013)';
      case 'AUTRE':
        return 'Secteur non visé par la présomption sectorielle';
      default:
        return '—';
    }
  }

  statutDeclareLabel(value: InastriStatutTravailleurIndependantStatutDeclare | null): string {
    switch (value) {
      case 'INDEPENDANT_INASTI':
        return 'Déclaré indépendant — affilié caisse d\'assurances sociales (AR n° 38 du 27/07/1967)';
      case 'SALARIE_DIMONA':
        return 'Déclaré salarié — DIMONA ONSS effectuée (Loi-programme 24/12/2002)';
      case 'SANS_DECLARATION':
        return 'Aucune affiliation déclarée (présomption simple de salariat art. 328 § 1 mobilisable)';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'INDEPENDANT_CONFIRME':
        return 'Indépendant confirmé — les 4 critères généraux art. 333 (volonté des parties, liberté d\'organisation du temps, liberté d\'organisation du travail, absence de contrôle hiérarchique) et, le cas échéant, les critères sectoriels art. 337/2 sont compatibles avec une relation d\'indépendance économique autonome. Maintien de l\'affiliation INASTI / caisse d\'assurances sociales.';
      case 'SALARIE_REQUALIFIE':
        return 'Salarié requalifié — qualification d\'indépendant écartée : la majorité des critères généraux pointe vers la subordination juridique. Relation à requalifier en contrat de travail salarié (Loi 03/07/1978) avec rappel rétroactif des cotisations ONSS depuis l\'origine, amende ONSS forfaitaire 3 × (art. 28 L. 22/04/2003) et sanction art. 181 C. pén. soc. niveau 4 si absence DIMONA cumulée.';
      case 'FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE':
        return 'Faux indépendant par présomption sectorielle art. 337/2 § 1 — secteur visé (construction / transport / gardiennage / nettoyage / agriculture-horticulture) et majorité des 9 critères sectoriels indicateurs de salariat. Charge probatoire inversée à charge de la personne qui invoque l\'indépendance (Cour const. arrêt 167/2013 du 19/12/2013).';
      case 'PRESUMPTION_GENERALE_SALARIAT':
        return 'Présomption générale de salariat art. 328 § 1 C. pén. soc. mobilisable — aucune DIMONA n\'a été effectuée et la personne se déclare sans statut clair. Présomption simple renversable par tout moyen de preuve (Cass. BE 06/12/2010 S.10.0067.F).';
      case 'A_QUALIFIER':
        return 'Qualification ouverte — inputs insuffisants, critères contradictoires ou nature de la relation à clarifier par l\'avocat avant prise de position définitive (négociation INASTI, contestation devant tribunal du travail, transaction).';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict. */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'SALARIE_REQUALIFIE':
      case 'FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE':
      case 'PRESUMPTION_GENERALE_SALARIAT':
        return 'verdict-critical';
      case 'INDEPENDANT_CONFIRME':
        return 'verdict-info';
      case 'A_QUALIFIER':
        return 'verdict-neutral';
      default:
        return '';
    }
  }

  /** Icône Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'badge';
    switch (r.verdict) {
      case 'SALARIE_REQUALIFIE':
        return 'gavel';
      case 'FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE':
        return 'report';
      case 'PRESUMPTION_GENERALE_SALARIAT':
        return 'warning';
      case 'INDEPENDANT_CONFIRME':
        return 'check_circle';
      case 'A_QUALIFIER':
        return 'help_outline';
      default:
        return 'badge';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: InastriStatutTravailleurIndependantVerdict): string {
    switch (verdict) {
      case 'INDEPENDANT_CONFIRME':
        return 'INDÉPENDANT';
      case 'SALARIE_REQUALIFIE':
        return 'SALARIÉ REQUALIFIÉ';
      case 'FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE':
        return 'PRÉSOMPTION SECTORIELLE';
      case 'PRESUMPTION_GENERALE_SALARIAT':
        return 'PRÉSOMPTION GÉNÉRALE';
      case 'A_QUALIFIER':
        return 'À QUALIFIER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: InastriStatutTravailleurIndependantVerdict): string {
    switch (verdict) {
      case 'SALARIE_REQUALIFIE':
      case 'FAUX_INDEPENDANT_PRESUMPTION_SECTORIELLE':
      case 'PRESUMPTION_GENERALE_SALARIAT':
        return 'badge-critical';
      case 'INDEPENDANT_CONFIRME':
        return 'badge-info';
      case 'A_QUALIFIER':
        return 'badge-neutral';
      default:
        return '';
    }
  }

  /** Boolean → Oui / Non / —. */
  formatBoolean(value: boolean | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value ? 'Oui' : 'Non';
  }

  /** Format entier. */
  formatInt(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value.toLocaleString('fr-FR', { maximumFractionDigits: 0 });
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: InastriStatutTravailleurIndependantRequest = {
      volonteParties: this.volonteParties()!,
      liberteOrganisationTemps: this.liberteOrganisationTemps()!,
      liberteOrganisationTravail: this.liberteOrganisationTravail()!,
      controleHierarchique: this.controleHierarchique()!,
      secteur: this.secteur()!,
      nbCriteresSectorielsSalarie: this.nbCriteresSectorielsSalarie()!,
      statutDeclare: this.statutDeclare()!,
      dimonaPresente: this.dimonaPresente()!,
      autreClientPresent: this.autreClientPresent()!,
    };
    this.calculating.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse INASTI statut indépendant calculée', 'OK', { duration: 2500 });
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
        this.volonteParties.set(r.volonteParties);
        this.liberteOrganisationTemps.set(r.liberteOrganisationTemps);
        this.liberteOrganisationTravail.set(r.liberteOrganisationTravail);
        this.controleHierarchique.set(r.controleHierarchique);
        this.secteur.set(r.secteur);
        this.nbCriteresSectorielsSalarie.set(r.nbCriteresSectorielsSalarie);
        this.statutDeclare.set(r.statutDeclare);
        this.dimonaPresente.set(r.dimonaPresente);
        this.autreClientPresent.set(r.autreClientPresent);
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
