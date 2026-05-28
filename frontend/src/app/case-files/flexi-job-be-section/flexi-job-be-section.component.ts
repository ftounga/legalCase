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
  FlexiJobBeRequest,
  FlexiJobBeResponse,
  FlexiJobBeSecteurEmployeur,
  FlexiJobBeStatutTravailleur,
  FlexiJobBeVerdict,
} from '../../core/models/flexi-job-be.model';
import {
  FlexiJobBeService,
} from '../../core/services/flexi-job-be.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  FlexiJobBeSectionPrefillRules,
} from './flexi-job-be-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-12b : outil décisionnel « Flexi-job (Belgique) » (F-219).
 *
 * <p>BE uniquement — <b>Loi-programme du 26/12/2013</b> (art. 13 à 28,
 * régime initial HoReCa CP 302) + <b>Loi 25/04/2014</b> + <b>Loi-programme
 * du 16/11/2015</b> (extension boulangerie / coiffure) + <b>Loi-programme
 * du 30/10/2018</b> (extension commerce de détail / agriculture /
 * soins de santé + suppression du plafond pensionnés) + <b>Loi-programme
 * du 28/12/2023</b> + <b>AR 02/06/2024</b> (extension secteurs publics
 * limités / sport / culture / événementiel / garages / transport autocar +
 * indexation flexi-salaire et plafond).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id {@code flexi-job-be},
 * visibility ALWAYS_ON priority 130 BE / DROIT_DU_TRAVAIL — au-dessus
 * de {@code conge-education-paye-region} (129, SF-219-11b) et
 * {@code delegue-syndical-cct-5} (128, SF-219-10b).</p>
 *
 * <h2>Verdict hiérarchisé 7 états (priorité : travailleur → secteur →
 * cumul → formalisme → rémunération)</h2>
 * <ul>
 *   <li>{@code ELIGIBLE} (vert) — toutes conditions cumulatives réunies.</li>
 *   <li>{@code FRAGILE_CONTRAT_OU_DIMONA_MANQUANT} (ambre) — formalisme
 *       défaillant, risque requalification ONSS.</li>
 *   <li>{@code FRAGILE_PLAFOND_DEPASSE} (ambre) — salaire sous minimum
 *       OU plafond annuel dépassé.</li>
 *   <li>{@code INELIGIBLE_TRAVAILLEUR_HORS_CONDITION} (rouge) — ni
 *       pensionné, ni 4/5 ETP T-3.</li>
 *   <li>{@code INELIGIBLE_SECTEUR_NON_ELIGIBLE} (rouge) — secteur non
 *       listé par les lois-programmes.</li>
 *   <li>{@code INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR} (rouge) — déjà
 *       occupé chez le même employeur (anti-substitution).</li>
 *   <li>{@code A_ANALYSER} (gris info) — secteur en zone grise.</li>
 * </ul>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 / F-219 (date occupation flexi employeur, statut T-3 chez un
 * AUTRE employeur, secteur paritaire flexi, formalisme Dimona FLX,
 * paramètres légaux indexés non extractibles du pipeline Travail BE
 * actuel).</p>
 */
@Component({
  selector: 'app-flexi-job-be-section',
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
  templateUrl: './flexi-job-be-section.component.html',
  styleUrl: './flexi-job-be-section.component.scss',
})
export class FlexiJobBeSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'flexi-job-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'FLEXI-JOB (BE)';
  static readonly TOOL_ICON = 'restaurant_menu';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return FlexiJobBeSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR (CDD d'usage et CDI intérimaire incomparables). */
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
  result = signal<FlexiJobBeResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  datePremiereOccupationFlexi = signal<string | null>(null);
  statutTravailleur = signal<FlexiJobBeStatutTravailleur | null>(null);
  secteurEmployeur = signal<FlexiJobBeSecteurEmployeur | null>(null);
  dejaOccupeChezMemeEmployeur = signal<boolean | null>(null);
  contratCadreSigne = signal<boolean | null>(null);
  dimonaFlxDeclaree = signal<boolean | null>(null);
  flexiSalaireHoraireBrut = signal<number | null>(null);
  flexiSalaireMinimumApplicable = signal<number | null>(12.33);
  revenuAnnuelFlexiCumule = signal<number | null>(null);
  plafondAnnuelExonere = signal<number | null>(12000);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: FlexiJobBeService,
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
    // SF-219-12b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (!this.datePremiereOccupationFlexi()) return false;
    if (this.statutTravailleur() === null) return false;
    if (this.secteurEmployeur() === null) return false;
    if (this.dejaOccupeChezMemeEmployeur() === null) return false;
    if (this.contratCadreSigne() === null) return false;
    if (this.dimonaFlxDeclaree() === null) return false;
    if (this.flexiSalaireHoraireBrut() === null) return false;
    if ((this.flexiSalaireHoraireBrut() as number) < 0) return false;
    if (this.flexiSalaireMinimumApplicable() === null) return false;
    if ((this.flexiSalaireMinimumApplicable() as number) <= 0) return false;
    if (this.revenuAnnuelFlexiCumule() === null) return false;
    if ((this.revenuAnnuelFlexiCumule() as number) < 0) return false;
    if (this.plafondAnnuelExonere() === null) return false;
    if ((this.plafondAnnuelExonere() as number) <= 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onDatePremiereOccupationFlexiChange(value: string | null): void {
    this.datePremiereOccupationFlexi.set(value);
  }
  onStatutTravailleurChange(value: FlexiJobBeStatutTravailleur): void {
    this.statutTravailleur.set(value);
  }
  onSecteurEmployeurChange(value: FlexiJobBeSecteurEmployeur): void {
    this.secteurEmployeur.set(value);
  }
  onDejaOccupeChezMemeEmployeurChange(value: boolean): void {
    this.dejaOccupeChezMemeEmployeur.set(value);
  }
  onContratCadreSigneChange(value: boolean): void {
    this.contratCadreSigne.set(value);
  }
  onDimonaFlxDeclareeChange(value: boolean): void {
    this.dimonaFlxDeclaree.set(value);
  }
  onFlexiSalaireHoraireBrutChange(value: string | number | null): void {
    this.flexiSalaireHoraireBrut.set(this.toNumberOrNull(value));
  }
  onFlexiSalaireMinimumApplicableChange(value: string | number | null): void {
    this.flexiSalaireMinimumApplicable.set(this.toNumberOrNull(value));
  }
  onRevenuAnnuelFlexiCumuleChange(value: string | number | null): void {
    this.revenuAnnuelFlexiCumule.set(this.toNumberOrNull(value));
  }
  onPlafondAnnuelExonereChange(value: string | number | null): void {
    this.plafondAnnuelExonere.set(this.toNumberOrNull(value));
  }

  private toNumberOrNull(value: string | number | null): number | null {
    if (value === null || value === '') return null;
    const n = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(n) ? n : null;
  }

  // -- Libellés humains --
  statutTravailleurLabel(value: FlexiJobBeStatutTravailleur | null): string {
    switch (value) {
      case 'PENSIONNE':
        return 'Pensionné (retraite ou survie) — cumul sans plafond depuis 2018';
      case 'SALARIE_4_5_TEMPS_T_MOINS_3':
        return 'Salarié ≥ 4/5 ETP au T-3 chez un AUTRE employeur (DmfA ONSS)';
      case 'AUTRE':
        return 'Autre situation (chômeur, étudiant, etc.) — pas de statut flexi';
      default:
        return '—';
    }
  }

  secteurEmployeurLabel(value: FlexiJobBeSecteurEmployeur | null): string {
    switch (value) {
      case 'HORECA_302':
        return 'HoReCa (CP 302) — régime historique 2013';
      case 'BOULANGERIE_COIFFURE':
        return 'Boulangerie / coiffure (CP 119 / CP 314) — extension 2015';
      case 'COMMERCE_DETAIL':
        return 'Commerce de détail (CP 201/202/311/312) — extension 2018';
      case 'AGRICULTURE':
        return 'Agriculture / horticulture (CP 132/144/145) — extension 2018';
      case 'SOINS_DE_SANTE':
        return 'Soins de santé (périmètre limité) — extension 2018';
      case 'SECTEUR_PUBLIC':
        return 'Secteur public (périmètre limité) — extension 2023';
      case 'SPORT_CULTURE_EVENEMENTIEL':
        return 'Sport / culture / événementiel / garages / transport — extension 2023';
      case 'AUTRE_NON_ELIGIBLE':
        return 'Secteur non listé par les lois-programmes (non éligible)';
      case 'ZONE_GRISE_EXTENSION_RECENTE':
        return 'Extension récente sans AR d\'application — analyse requise';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'ELIGIBLE':
        return 'Occupation flexi-job régulière — toutes conditions cumulatives réunies';
      case 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT':
        return 'Formalisme défaillant — contrat-cadre ou Dimona FLX manquant (risque requalification ONSS)';
      case 'FRAGILE_PLAFOND_DEPASSE':
        return 'Rémunération non conforme — salaire sous minimum ou plafond annuel dépassé';
      case 'INELIGIBLE_TRAVAILLEUR_HORS_CONDITION':
        return 'Travailleur hors condition — ni pensionné, ni 4/5 ETP T-3 chez un autre employeur';
      case 'INELIGIBLE_SECTEUR_NON_ELIGIBLE':
        return 'Secteur non éligible — pas listé par les lois-programmes 2013/2015/2018/2023';
      case 'INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR':
        return 'Cumul interdit — déjà occupé chez le même employeur (anti-substitution)';
      case 'A_ANALYSER':
        return 'Zone grise — extension récente sans AR d\'application connu, analyse juridique requise';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert / ambre / rouge / gris info). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'ELIGIBLE':
        return 'verdict-ok';
      case 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT':
      case 'FRAGILE_PLAFOND_DEPASSE':
        return 'verdict-warn';
      case 'INELIGIBLE_TRAVAILLEUR_HORS_CONDITION':
      case 'INELIGIBLE_SECTEUR_NON_ELIGIBLE':
      case 'INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR':
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
    if (!r) return 'restaurant_menu';
    switch (r.verdict) {
      case 'ELIGIBLE':
        return 'verified';
      case 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT':
      case 'FRAGILE_PLAFOND_DEPASSE':
        return 'warning_amber';
      case 'INELIGIBLE_TRAVAILLEUR_HORS_CONDITION':
      case 'INELIGIBLE_SECTEUR_NON_ELIGIBLE':
      case 'INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR':
        return 'cancel';
      case 'A_ANALYSER':
        return 'help_outline';
      default:
        return 'restaurant_menu';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: FlexiJobBeVerdict): string {
    switch (verdict) {
      case 'ELIGIBLE':
        return 'ÉLIGIBLE';
      case 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT':
        return 'FORMALISME FRAGILE';
      case 'FRAGILE_PLAFOND_DEPASSE':
        return 'PLAFOND DÉPASSÉ';
      case 'INELIGIBLE_TRAVAILLEUR_HORS_CONDITION':
        return 'INÉLIGIBLE (TRAVAILLEUR)';
      case 'INELIGIBLE_SECTEUR_NON_ELIGIBLE':
        return 'INÉLIGIBLE (SECTEUR)';
      case 'INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR':
        return 'CUMUL INTERDIT';
      case 'A_ANALYSER':
        return 'À ANALYSER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: FlexiJobBeVerdict): string {
    switch (verdict) {
      case 'ELIGIBLE':
        return 'badge-ok';
      case 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT':
      case 'FRAGILE_PLAFOND_DEPASSE':
        return 'badge-warn';
      case 'INELIGIBLE_TRAVAILLEUR_HORS_CONDITION':
      case 'INELIGIBLE_SECTEUR_NON_ELIGIBLE':
      case 'INELIGIBLE_CUMUL_INTERDIT_MEME_EMPLOYEUR':
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
    const request: FlexiJobBeRequest = {
      datePremiereOccupationFlexi: this.datePremiereOccupationFlexi()!,
      statutTravailleur: this.statutTravailleur()!,
      secteurEmployeur: this.secteurEmployeur()!,
      dejaOccupeChezMemeEmployeur: this.dejaOccupeChezMemeEmployeur()!,
      contratCadreSigne: this.contratCadreSigne()!,
      dimonaFlxDeclaree: this.dimonaFlxDeclaree()!,
      flexiSalaireHoraireBrut: this.flexiSalaireHoraireBrut()!,
      flexiSalaireMinimumApplicable: this.flexiSalaireMinimumApplicable()!,
      revenuAnnuelFlexiCumule: this.revenuAnnuelFlexiCumule()!,
      plafondAnnuelExonere: this.plafondAnnuelExonere()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Flexi-job analysé', 'OK', { duration: 2500 });
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
        this.datePremiereOccupationFlexi.set(r.datePremiereOccupationFlexi);
        this.statutTravailleur.set(r.statutTravailleur);
        this.secteurEmployeur.set(r.secteurEmployeur);
        this.dejaOccupeChezMemeEmployeur.set(r.dejaOccupeChezMemeEmployeur);
        this.contratCadreSigne.set(r.contratCadreSigne);
        this.dimonaFlxDeclaree.set(r.dimonaFlxDeclaree);
        this.flexiSalaireHoraireBrut.set(r.flexiSalaireHoraireBrut);
        this.flexiSalaireMinimumApplicable.set(r.flexiSalaireMinimumApplicable);
        this.revenuAnnuelFlexiCumule.set(r.revenuAnnuelFlexiCumule);
        this.plafondAnnuelExonere.set(r.plafondAnnuelExonere);
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
