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
  TravailNoirBeDimonaElementsSubordination,
  TravailNoirBeDimonaRequest,
  TravailNoirBeDimonaResponse,
  TravailNoirBeDimonaStatutDimona,
  TravailNoirBeDimonaVerdict,
} from '../../core/models/travail-noir-be-dimona.model';
import {
  TravailNoirBeDimonaService,
} from '../../core/services/travail-noir-be-dimona.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  TravailNoirBeDimonaSectionPrefillRules,
} from './travail-noir-be-dimona-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-26b : outil décisionnel « Travail noir BE — DIMONA,
 * requalification et sanctions » (F-219).
 *
 * <p>BE uniquement — <b>Loi-programme du 24/12/2002</b> art. 167-184
 * (M.B. 31/12/2002) + <b>AR du 05/11/2002</b> instituant la déclaration
 * immédiate de l'emploi à l'ONSS (DIMONA) + <b>Code pénal social</b>
 * art. 181 (niveau 4 défaut DIMONA — Cass. ch. néerl. 21/11/2017
 * P.17.0532.N) + art. 328 § 1 (présomption simple de salariat — Cass.
 * 06/12/2010 S.10.0067.F) + <b>Loi 22/04/2003</b> art. 28 (amende ONSS
 * forfaitaire 3 × cotisations dues) + <b>Loi-programme I du
 * 27/12/2006</b> art. 328 et s. (faux indépendants — critères Bart
 * Buysse construction / transport / nettoyage / agriculture / horeca /
 * gardiennage).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id {@code travail-noir-be-dimona},
 * visibility ALWAYS_ON priority 144 BE / DROIT_DU_TRAVAIL — au-dessus
 * de {@code auditorat-travail-be} (143, SF-219-25b),
 * {@code code-penal-social-be} (142, SF-219-24b),
 * {@code discrimination-be-handicap-amenagement} (141, SF-219-23b)).</p>
 *
 * <h2>Verdict 6 états</h2>
 * <ul>
 *   <li>{@code DIMONA_CONFORME} (info) — déclaration avant début
 *       occupation, aucune sanction.</li>
 *   <li>{@code DIMONA_TARDIVE_REGULARISABLE} (warn) — sanction
 *       administrative ONSS atténuée.</li>
 *   <li>{@code ABSENCE_DIMONA_SANCTION_NIVEAU_4} (critique) — niveau 4
 *       art. 181 + cotisations rétroactives + amende forfaitaire 3 ×.</li>
 *   <li>{@code REQUALIFICATION_SALARIE_PRESUMEE} (critique) —
 *       présomption art. 328 déclenchée.</li>
 *   <li>{@code INDEPENDANT_REQUALIFIE_NON_DECLARE} (critique) — faux
 *       indépendant requalifié, cotisations depuis l'origine.</li>
 *   <li>{@code A_QUALIFIER} (neutre) — inputs insuffisants ou ambigus.</li>
 * </ul>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 / F-219 (statut DIMONA, dates, salaire, nombre travailleurs,
 * personne morale, récidive, éléments subordination non extractibles
 * du dossier salarié — relèvent du procès-verbal d'inspection sociale /
 * extrait ONSS / audition travailleur).</p>
 */
@Component({
  selector: 'app-travail-noir-be-dimona-section',
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
  templateUrl: './travail-noir-be-dimona-section.component.html',
  styleUrl: './travail-noir-be-dimona-section.component.scss',
})
export class TravailNoirBeDimonaSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'travail-noir-be-dimona';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'TRAVAIL NOIR DIMONA (BE)';
  static readonly TOOL_ICON = 'work_off';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return TravailNoirBeDimonaSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR mécanique (DPAE distincte). */
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
  result = signal<TravailNoirBeDimonaResponse | null>(null);

  // -- Champs du form (signals, édités via handlers). --
  statutDimona = signal<TravailNoirBeDimonaStatutDimona | null>(null);
  dateDebutOccupation = signal<string | null>(null);
  dateDimonaEffective = signal<string | null>(null);
  dateControle = signal<string | null>(null);
  salaireBrutMensuel = signal<number | null>(null);
  nombreTravailleursConcernes = signal<number | null>(null);
  personneMorale = signal<boolean | null>(null);
  recidiveDansLAn = signal<boolean | null>(null);
  elementsSubordination = signal<TravailNoirBeDimonaElementsSubordination | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: TravailNoirBeDimonaService,
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
    // SF-219-26b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.statutDimona() === null) return false;
    if (this.dateDebutOccupation() === null || this.dateDebutOccupation() === '') return false;
    if (this.dateControle() === null || this.dateControle() === '') return false;
    if (this.salaireBrutMensuel() === null) return false;
    const sal = this.salaireBrutMensuel() as number;
    if (sal < 0) return false;
    if (this.nombreTravailleursConcernes() === null) return false;
    const nb = this.nombreTravailleursConcernes() as number;
    if (nb < 1 || nb > 1000000) return false;
    if (this.personneMorale() === null) return false;
    if (this.recidiveDansLAn() === null) return false;
    if (this.elementsSubordination() === null) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onStatutDimonaChange(value: TravailNoirBeDimonaStatutDimona): void {
    this.statutDimona.set(value);
  }
  onDateDebutOccupationChange(value: string | null): void {
    this.dateDebutOccupation.set(value);
  }
  onDateDimonaEffectiveChange(value: string | null): void {
    this.dateDimonaEffective.set(value);
  }
  onDateControleChange(value: string | null): void {
    this.dateControle.set(value);
  }
  onSalaireBrutMensuelChange(value: string | number | null): void {
    this.salaireBrutMensuel.set(this.toNumberOrNull(value));
  }
  onNombreTravailleursConcernesChange(value: string | number | null): void {
    this.nombreTravailleursConcernes.set(this.toNumberOrNull(value));
  }
  onPersonneMoraleChange(value: boolean): void {
    this.personneMorale.set(value);
  }
  onRecidiveDansLAnChange(value: boolean): void {
    this.recidiveDansLAn.set(value);
  }
  onElementsSubordinationChange(value: TravailNoirBeDimonaElementsSubordination): void {
    this.elementsSubordination.set(value);
  }

  private toNumberOrNull(value: string | number | null): number | null {
    if (value === null || value === '') return null;
    const n = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(n) ? n : null;
  }

  // -- Libellés humains --
  statutDimonaLabel(value: TravailNoirBeDimonaStatutDimona | null): string {
    switch (value) {
      case 'DECLAREE_AVANT_DEBUT':
        return 'DIMONA déclarée avant début occupation (cas nominal art. 4 AR 05/11/2002)';
      case 'DECLAREE_TARDIVE':
        return 'DIMONA déclarée tardivement (après début mais avant contrôle)';
      case 'ABSENTE':
        return 'DIMONA absente au moment du contrôle (infraction art. 181 niveau 4)';
      case 'INDEPENDANT_FAUSSEMENT_QUALIFIE':
        return 'Relation indépendante faussement qualifiée — à requalifier (faux indépendant)';
      default:
        return '—';
    }
  }

  elementsSubordinationLabel(value: TravailNoirBeDimonaElementsSubordination | null): string {
    switch (value) {
      case 'OUI':
        return 'Subordination caractérisée (horaires imposés, instructions, intégration)';
      case 'NON':
        return 'Aucun élément de subordination (relation indépendante)';
      case 'INDETERMINE':
        return 'Indéterminé — analyse complémentaire avocat requise';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'DIMONA_CONFORME':
        return 'DIMONA conforme — déclaration effectuée avant le début effectif de l\'occupation (art. 4 AR du 05/11/2002). Aucune sanction encourue ; rappel des obligations DRS et DmfA trimestrielle ONSS.';
      case 'DIMONA_TARDIVE_REGULARISABLE':
        return 'DIMONA tardive régularisable — déclaration effectuée après le début effectif mais avant le contrôle inspection sociale. Sanction administrative ONSS atténuée possible, pas de sanction pénale niveau 4 art. 181 C. pén. soc.';
      case 'ABSENCE_DIMONA_SANCTION_NIVEAU_4':
        return 'Absence DIMONA — infraction art. 181 C. pén. soc. niveau 4 : amende administrative 300 € à 3 000 € OU amende pénale 600 € à 6 000 € ET / OU emprisonnement 6 mois à 3 ans, cumulée avec cotisations ONSS rétroactives (employeur ~25 % + travailleur 13,07 %) et amende ONSS forfaitaire 3 × cotisations dues (art. 28 L. 22/04/2003).';
      case 'REQUALIFICATION_SALARIE_PRESUMEE':
        return 'Présomption de salariat art. 328 § 1 C. pén. soc. déclenchée — absence DIMONA combinée à des éléments de subordination caractérisés. Relation requalifiée en salariat avec rappel des cotisations ONSS rétroactives depuis l\'origine + sanction art. 181 niveau 4.';
      case 'INDEPENDANT_REQUALIFIE_NON_DECLARE':
        return 'Faux indépendant requalifié — relation faussement qualifiée d\'indépendante (Loi-programme I 27/12/2006 art. 328 et s., critères Bart Buysse). Obligation DIMONA depuis l\'origine non remplie, sanction art. 181 + cotisations ONSS rétroactives depuis le début de la relation.';
      case 'A_QUALIFIER':
        return 'Qualification ouverte — inputs insuffisants ou ambigus (éléments de subordination INDETERMINE ou cohérence dates/statut à clarifier). L\'avocat doit compléter l\'analyse.';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict. */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'ABSENCE_DIMONA_SANCTION_NIVEAU_4':
      case 'REQUALIFICATION_SALARIE_PRESUMEE':
      case 'INDEPENDANT_REQUALIFIE_NON_DECLARE':
        return 'verdict-critical';
      case 'DIMONA_TARDIVE_REGULARISABLE':
        return 'verdict-warn';
      case 'DIMONA_CONFORME':
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
    if (!r) return 'work_off';
    switch (r.verdict) {
      case 'ABSENCE_DIMONA_SANCTION_NIVEAU_4':
        return 'gavel';
      case 'REQUALIFICATION_SALARIE_PRESUMEE':
        return 'report';
      case 'INDEPENDANT_REQUALIFIE_NON_DECLARE':
        return 'warning';
      case 'DIMONA_TARDIVE_REGULARISABLE':
        return 'schedule';
      case 'DIMONA_CONFORME':
        return 'check_circle';
      case 'A_QUALIFIER':
        return 'help_outline';
      default:
        return 'work_off';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: TravailNoirBeDimonaVerdict): string {
    switch (verdict) {
      case 'DIMONA_CONFORME':
        return 'CONFORME';
      case 'DIMONA_TARDIVE_REGULARISABLE':
        return 'TARDIVE';
      case 'ABSENCE_DIMONA_SANCTION_NIVEAU_4':
        return 'ABSENCE — NIVEAU 4';
      case 'REQUALIFICATION_SALARIE_PRESUMEE':
        return 'PRÉSOMPTION SALARIAT';
      case 'INDEPENDANT_REQUALIFIE_NON_DECLARE':
        return 'FAUX INDÉPENDANT';
      case 'A_QUALIFIER':
        return 'À QUALIFIER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: TravailNoirBeDimonaVerdict): string {
    switch (verdict) {
      case 'ABSENCE_DIMONA_SANCTION_NIVEAU_4':
      case 'REQUALIFICATION_SALARIE_PRESUMEE':
      case 'INDEPENDANT_REQUALIFIE_NON_DECLARE':
        return 'badge-critical';
      case 'DIMONA_TARDIVE_REGULARISABLE':
        return 'badge-warn';
      case 'DIMONA_CONFORME':
        return 'badge-info';
      case 'A_QUALIFIER':
        return 'badge-neutral';
      default:
        return '';
    }
  }

  /** Format euros — décimales 2 (cotisations). */
  formatEuros(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value.toLocaleString('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }) + ' €';
  }

  /** Format euros entiers (bornes barème). */
  formatEurosInt(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value.toLocaleString('fr-FR', {
      maximumFractionDigits: 0,
    }) + ' €';
  }

  /** Boolean → Oui / Non / —. */
  formatBoolean(value: boolean | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value ? 'Oui' : 'Non';
  }

  /** Format mois → "X mois" / "X mois - Y mois". */
  formatMoisRange(min: number, max: number): string {
    if (min === 0 && max === 0) return '—';
    if (min === max) return `${min} mois`;
    return `${min} mois — ${max} mois`;
  }

  /** Format jours. */
  formatJours(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    if (value === 0) return '0 jour';
    if (value === 1) return '1 jour';
    return `${value.toLocaleString('fr-FR', { maximumFractionDigits: 0 })} jours`;
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: TravailNoirBeDimonaRequest = {
      statutDimona: this.statutDimona()!,
      dateDebutOccupation: this.dateDebutOccupation()!,
      dateDimonaEffective: this.dateDimonaEffective(),
      dateControle: this.dateControle()!,
      salaireBrutMensuel: this.salaireBrutMensuel()!,
      nombreTravailleursConcernes: this.nombreTravailleursConcernes()!,
      personneMorale: this.personneMorale()!,
      recidiveDansLAn: this.recidiveDansLAn()!,
      elementsSubordination: this.elementsSubordination()!,
    };
    this.calculating.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse Travail noir DIMONA calculée', 'OK', { duration: 2500 });
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
        this.statutDimona.set(r.statutDimona);
        this.dateDebutOccupation.set(r.dateDebutOccupation);
        this.dateDimonaEffective.set(r.dateDimonaEffective);
        this.dateControle.set(r.dateControle);
        this.salaireBrutMensuel.set(r.salaireBrutMensuel);
        this.nombreTravailleursConcernes.set(r.nombreTravailleursConcernes);
        this.personneMorale.set(r.personneMorale);
        this.recidiveDansLAn.set(r.recidiveDansLAn);
        this.elementsSubordination.set(r.elementsSubordination);
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
