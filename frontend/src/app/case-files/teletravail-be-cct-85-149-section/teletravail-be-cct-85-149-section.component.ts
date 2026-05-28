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
  TeletravailBeCct85149Request,
  TeletravailBeCct85149Response,
  TeletravailBeCct85149TypeTeletravail,
  TeletravailBeCct85149Verdict,
} from '../../core/models/teletravail-be-cct-85-149.model';
import {
  TeletravailBeCct85149Service,
} from '../../core/services/teletravail-be-cct-85-149.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  TeletravailBeCct85149SectionPrefillRules,
} from './teletravail-be-cct-85-149-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-16b : outil décisionnel « télétravail (Belgique) — CCT n° 85 /
 * CCT n° 149 » (F-219).
 *
 * <p>BE uniquement — <b>CCT n° 85 du 09/11/2005</b> (CNT) concernant le
 * télétravail régulier et structurel (rendue obligatoire AR 13/06/2006
 * M.B. 05/09/2006) + <b>CCT n° 149 du 26/01/2021</b> (CNT) relative au
 * télétravail occasionnel / force majeure / COVID + <b>Loi du 03/07/1978</b>
 * art. 17 (obligations du travailleur) + <b>Loi du 04/08/1996</b> + <b>Code
 * du bien-être au travail Livre VIII Titre 1</b> + <b>Loi du 26/03/2018</b>
 * art. 16-18 (droit à la déconnexion par concertation collective) + <b>Loi
 * du 03/10/2022</b> « Deal pour l'emploi » M.B. 10/11/2022 (modalités de
 * déconnexion par CCT / règlement de travail obligatoires pour les
 * entreprises ≥ 20 travailleurs, e.e.v. 01/04/2023).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id {@code teletravail-be-cct-85-149},
 * visibility ALWAYS_ON priority 134 BE / DROIT_DU_TRAVAIL — au-dessus de
 * {@code interim-be-cct-322} (132, SF-219-14b), {@code etudiant-jobiste-be}
 * (131, SF-219-13b) et {@code flexi-job-be} (130, SF-219-12b). Priorité 133
 * réservée à SF-219-15b (interim-be-indemnite-fin-mission) en parallèle.</p>
 *
 * <h2>Verdict hiérarchisé 7 états</h2>
 * <ul>
 *   <li>{@code CONFORME_CCT_85_STRUCTUREL} (vert) — télétravail structurel
 *       régulier toutes conditions cumulatives CCT n° 85 réunies.</li>
 *   <li>{@code CONFORME_CCT_149_OCCASIONNEL} (vert) — télétravail occasionnel
 *       relevant de la CCT n° 149, cadre minimal respecté.</li>
 *   <li>{@code NON_CONFORME_CONVENTION_ECRITE_MANQUANTE} (rouge) — télétravail
 *       structurel sans convention écrite individuelle (art. 6 CCT n° 85).</li>
 *   <li>{@code NON_CONFORME_EQUIPEMENT_NON_FOURNI} (rouge) — équipement non
 *       fourni / non indemnisé par l'employeur (art. 9 CCT n° 85).</li>
 *   <li>{@code NON_CONFORME_DROITS_REDUITS} (rouge) — droits sociaux réduits
 *       par rapport aux présentiels — discrimination art. 4 CCT n° 85.</li>
 *   <li>{@code FRAGILE_DECONNEXION_NON_DEFINIE} (ambre) — entreprise ≥ 20
 *       travailleurs sans accord collectif / règlement de travail fixant
 *       les modalités de déconnexion (Loi 03/10/2022).</li>
 *   <li>{@code A_ANALYSER} (gris info) — type indéterminé ou configuration
 *       mixte (structurel + occasionnel) — analyse cas par cas requise.</li>
 * </ul>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 / F-219 (date entrée télétravail, type structurel/occasionnel,
 * flags art. 4-5-6-9 CCT n° 85, montant indemnité, plafond ONSS/SPF
 * Finances, modalités déconnexion, effectif entreprise non extractibles
 * du pipeline Travail BE actuel).</p>
 */
@Component({
  selector: 'app-teletravail-be-cct-85-149-section',
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
  templateUrl: './teletravail-be-cct-85-149-section.component.html',
  styleUrl: './teletravail-be-cct-85-149-section.component.scss',
})
export class TeletravailBeCct85149SectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'teletravail-be-cct-85-149';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'TÉLÉTRAVAIL (BE — CCT 85 / 149)';
  static readonly TOOL_ICON = 'home_work';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return TeletravailBeCct85149SectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR mécanique (cadre télétravail FR distinct C. trav. L. 1222-9). */
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
  result = signal<TeletravailBeCct85149Response | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  dateDebutTeletravail = signal<string | null>(null);
  typeTeletravail = signal<TeletravailBeCct85149TypeTeletravail | null>(null);
  volontariatReciproque = signal<boolean | null>(null);
  conventionEcriteIndividuelle = signal<boolean | null>(null);
  equipementFourniOuIndemnise = signal<boolean | null>(null);
  indemniteForfaitaireMensuelleEuros = signal<number | null>(null);
  plafondIndemniteMensuelleEuros = signal<number | null>(null);
  droitsSociauxMaintenus = signal<boolean | null>(null);
  deconnexionDefinie = signal<boolean | null>(null);
  effectifEntreprise = signal<number | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: TeletravailBeCct85149Service,
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
    // SF-219-16b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (!this.dateDebutTeletravail()) return false;
    if (this.typeTeletravail() === null) return false;
    if (this.volontariatReciproque() === null) return false;
    if (this.conventionEcriteIndividuelle() === null) return false;
    if (this.equipementFourniOuIndemnise() === null) return false;
    if (this.indemniteForfaitaireMensuelleEuros() === null) return false;
    if ((this.indemniteForfaitaireMensuelleEuros() as number) < 0) return false;
    if (this.plafondIndemniteMensuelleEuros() === null) return false;
    if ((this.plafondIndemniteMensuelleEuros() as number) <= 0) return false;
    if (this.droitsSociauxMaintenus() === null) return false;
    if (this.deconnexionDefinie() === null) return false;
    if (this.effectifEntreprise() === null) return false;
    if ((this.effectifEntreprise() as number) < 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onDateDebutTeletravailChange(value: string | null): void {
    this.dateDebutTeletravail.set(value);
  }
  onTypeTeletravailChange(value: TeletravailBeCct85149TypeTeletravail): void {
    this.typeTeletravail.set(value);
  }
  onVolontariatReciproqueChange(value: boolean): void {
    this.volontariatReciproque.set(value);
  }
  onConventionEcriteIndividuelleChange(value: boolean): void {
    this.conventionEcriteIndividuelle.set(value);
  }
  onEquipementFourniOuIndemniseChange(value: boolean): void {
    this.equipementFourniOuIndemnise.set(value);
  }
  onIndemniteForfaitaireMensuelleEurosChange(value: string | number | null): void {
    this.indemniteForfaitaireMensuelleEuros.set(this.toNumberOrNull(value));
  }
  onPlafondIndemniteMensuelleEurosChange(value: string | number | null): void {
    this.plafondIndemniteMensuelleEuros.set(this.toNumberOrNull(value));
  }
  onDroitsSociauxMaintenusChange(value: boolean): void {
    this.droitsSociauxMaintenus.set(value);
  }
  onDeconnexionDefinieChange(value: boolean): void {
    this.deconnexionDefinie.set(value);
  }
  onEffectifEntrepriseChange(value: string | number | null): void {
    this.effectifEntreprise.set(this.toNumberOrNull(value));
  }

  private toNumberOrNull(value: string | number | null): number | null {
    if (value === null || value === '') return null;
    const n = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(n) ? n : null;
  }

  // -- Libellés humains --
  typeTeletravailLabel(value: TeletravailBeCct85149TypeTeletravail | null): string {
    switch (value) {
      case 'STRUCTUREL_CCT_85':
        return 'Structurel — CCT n° 85 du 09/11/2005 (régulier)';
      case 'OCCASIONNEL_CCT_149':
        return 'Occasionnel — CCT n° 149 du 26/01/2021 (force majeure / personnel)';
      case 'INDETERMINE':
        return 'Indéterminé — qualification à opérer';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'CONFORME_CCT_85_STRUCTUREL':
        return 'Télétravail structurel conforme — toutes conditions cumulatives CCT n° 85 réunies';
      case 'CONFORME_CCT_149_OCCASIONNEL':
        return 'Télétravail occasionnel conforme — cadre CCT n° 149 respecté';
      case 'NON_CONFORME_CONVENTION_ECRITE_MANQUANTE':
        return 'Convention écrite individuelle manquante — non-conformité art. 6 CCT n° 85';
      case 'NON_CONFORME_EQUIPEMENT_NON_FOURNI':
        return 'Équipement non fourni / non indemnisé — non-conformité art. 9 CCT n° 85';
      case 'NON_CONFORME_DROITS_REDUITS':
        return 'Droits réduits par rapport aux présentiels — non-conformité art. 4 CCT n° 85';
      case 'FRAGILE_DECONNEXION_NON_DEFINIE':
        return 'Modalités de déconnexion non définies — manquement Loi 03/10/2022 (≥ 20 travailleurs)';
      case 'A_ANALYSER':
        return 'Type de télétravail indéterminé ou configuration mixte — analyse cas par cas requise';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert / ambre / rouge / gris info). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'CONFORME_CCT_85_STRUCTUREL':
      case 'CONFORME_CCT_149_OCCASIONNEL':
        return 'verdict-ok';
      case 'FRAGILE_DECONNEXION_NON_DEFINIE':
        return 'verdict-warn';
      case 'NON_CONFORME_CONVENTION_ECRITE_MANQUANTE':
      case 'NON_CONFORME_EQUIPEMENT_NON_FOURNI':
      case 'NON_CONFORME_DROITS_REDUITS':
        return 'verdict-critical';
      case 'A_ANALYSER':
        return 'verdict-neutral';
      default:
        return '';
    }
  }

  /** Icône Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'home_work';
    switch (r.verdict) {
      case 'CONFORME_CCT_85_STRUCTUREL':
      case 'CONFORME_CCT_149_OCCASIONNEL':
        return 'verified';
      case 'FRAGILE_DECONNEXION_NON_DEFINIE':
        return 'warning_amber';
      case 'NON_CONFORME_CONVENTION_ECRITE_MANQUANTE':
      case 'NON_CONFORME_EQUIPEMENT_NON_FOURNI':
      case 'NON_CONFORME_DROITS_REDUITS':
        return 'cancel';
      case 'A_ANALYSER':
        return 'help_outline';
      default:
        return 'home_work';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: TeletravailBeCct85149Verdict): string {
    switch (verdict) {
      case 'CONFORME_CCT_85_STRUCTUREL':
        return 'CONFORME CCT 85';
      case 'CONFORME_CCT_149_OCCASIONNEL':
        return 'CONFORME CCT 149';
      case 'NON_CONFORME_CONVENTION_ECRITE_MANQUANTE':
        return 'CONVENTION MANQUANTE';
      case 'NON_CONFORME_EQUIPEMENT_NON_FOURNI':
        return 'ÉQUIPEMENT NON FOURNI';
      case 'NON_CONFORME_DROITS_REDUITS':
        return 'DROITS RÉDUITS';
      case 'FRAGILE_DECONNEXION_NON_DEFINIE':
        return 'DÉCONNEXION FRAGILE';
      case 'A_ANALYSER':
        return 'À ANALYSER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: TeletravailBeCct85149Verdict): string {
    switch (verdict) {
      case 'CONFORME_CCT_85_STRUCTUREL':
      case 'CONFORME_CCT_149_OCCASIONNEL':
        return 'badge-ok';
      case 'FRAGILE_DECONNEXION_NON_DEFINIE':
        return 'badge-warn';
      case 'NON_CONFORME_CONVENTION_ECRITE_MANQUANTE':
      case 'NON_CONFORME_EQUIPEMENT_NON_FOURNI':
      case 'NON_CONFORME_DROITS_REDUITS':
        return 'badge-critical';
      case 'A_ANALYSER':
        return 'badge-neutral';
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
    const request: TeletravailBeCct85149Request = {
      dateDebutTeletravail: this.dateDebutTeletravail()!,
      typeTeletravail: this.typeTeletravail()!,
      volontariatReciproque: this.volontariatReciproque()!,
      conventionEcriteIndividuelle: this.conventionEcriteIndividuelle()!,
      equipementFourniOuIndemnise: this.equipementFourniOuIndemnise()!,
      indemniteForfaitaireMensuelleEuros: this.indemniteForfaitaireMensuelleEuros()!,
      plafondIndemniteMensuelleEuros: this.plafondIndemniteMensuelleEuros()!,
      droitsSociauxMaintenus: this.droitsSociauxMaintenus()!,
      deconnexionDefinie: this.deconnexionDefinie()!,
      effectifEntreprise: this.effectifEntreprise()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Conformité télétravail analysée', 'OK', { duration: 2500 });
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
        this.dateDebutTeletravail.set(r.dateDebutTeletravail);
        this.typeTeletravail.set(r.typeTeletravail);
        this.volontariatReciproque.set(r.volontariatReciproque);
        this.conventionEcriteIndividuelle.set(r.conventionEcriteIndividuelle);
        this.equipementFourniOuIndemnise.set(r.equipementFourniOuIndemnise);
        this.indemniteForfaitaireMensuelleEuros.set(r.indemniteForfaitaireMensuelleEuros);
        this.plafondIndemniteMensuelleEuros.set(r.plafondIndemniteMensuelleEuros);
        this.droitsSociauxMaintenus.set(r.droitsSociauxMaintenus);
        this.deconnexionDefinie.set(r.deconnexionDefinie);
        this.effectifEntreprise.set(r.effectifEntreprise);
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
