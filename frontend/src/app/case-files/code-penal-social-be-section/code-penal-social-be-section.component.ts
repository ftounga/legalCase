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
  CodePenalSocialBeRequest,
  CodePenalSocialBeResponse,
  CodePenalSocialBeTypeInfraction,
  CodePenalSocialBeVerdict,
} from '../../core/models/code-penal-social-be.model';
import {
  CodePenalSocialBeService,
} from '../../core/services/code-penal-social-be.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  CodePenalSocialBeSectionPrefillRules,
} from './code-penal-social-be-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-24b : outil décisionnel « Code pénal social BE — qualification
 * d'infraction sociale + niveau de sanction 1 à 4 » (F-219).
 *
 * <p>BE uniquement — <b>Loi du 06/06/2010</b> introduisant le Code pénal
 * social (M.B. 01/07/2010, e.e.v. 01/07/2011) : art. 101-103 barème
 * uniforme à 4 niveaux ; art. 102 choix amende administrative OU pénale ;
 * art. 103 § 2 multiplication par le nombre de travailleurs concernés ;
 * art. 105 majoration × 5 personne morale ; art. 109 préposé /
 * mandataire ; art. 110 doublement plafond récidive ≤ 1 an ; art. 119
 * à 235 (Livre 2 Titre 2 à 12) types d'infractions qualifiées.
 * Indexation des amendes pénales : art. 1<sup>er</sup> L. 05/03/1952,
 * +0,80 décimes additionnels jusqu'au 31/12/2024, +0,90 décimes
 * additionnels depuis le 01/01/2025 (AR 27/12/2024).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id {@code code-penal-social-be},
 * visibility ALWAYS_ON priority 142 BE / DROIT_DU_TRAVAIL — au-dessus
 * de {@code discrimination-be-handicap-amenagement} (141, SF-219-23b),
 * {@code egalite-femmes-hommes-be} (140, SF-219-22b),
 * {@code eco-cheques-cheques-repas-be} (139, SF-219-21b).</p>
 *
 * <h2>Verdict 5 états (court-circuits backend)</h2>
 * <ul>
 *   <li>{@code SANCTION_NIVEAU_1} (info — admin seule) — amende
 *       administrative 10 € à 100 € (non indexée, art. 101 § 1).</li>
 *   <li>{@code SANCTION_NIVEAU_2} (warn) — amende admin OU pénale
 *       50 € à 500 € (art. 102).</li>
 *   <li>{@code SANCTION_NIVEAU_3} (warn fort) — amende admin OU pénale
 *       100 € à 1 000 € (art. 102).</li>
 *   <li>{@code SANCTION_NIVEAU_4} (critique) — amende admin 300 € à
 *       3 000 € OU amende pénale 600 € à 6 000 € ET / OU emprisonnement
 *       6 mois à 3 ans (art. 103).</li>
 *   <li>{@code A_QUALIFIER} (neutre) — type AUTRE_QUALIFICATION sans
 *       niveau renseigné.</li>
 * </ul>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 / F-219 (type infraction, niveau, date faits, nombre
 * travailleurs, personne morale, récidive, qualité auteur, élément
 * moral non extractibles du dossier salarié — relèvent du
 * procès-verbal d'inspection sociale / dossier auditorat / analyse
 * pénale avocat).</p>
 */
@Component({
  selector: 'app-code-penal-social-be-section',
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
  templateUrl: './code-penal-social-be-section.component.html',
  styleUrl: './code-penal-social-be-section.component.scss',
})
export class CodePenalSocialBeSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'code-penal-social-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CODE PÉNAL SOCIAL (BE)';
  static readonly TOOL_ICON = 'gavel';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return CodePenalSocialBeSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR mécanique (C. trav. art. L. 8221-1 distinct). */
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
  result = signal<CodePenalSocialBeResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  typeInfraction = signal<CodePenalSocialBeTypeInfraction | null>(null);
  niveauPropose = signal<number | null>(null);
  dateFaits = signal<string | null>(null);
  nombreTravailleursConcernes = signal<number | null>(null);
  personneMorale = signal<boolean | null>(null);
  recidiveDansLAn = signal<boolean | null>(null);
  prevenuPreposeOuMandataire = signal<boolean | null>(null);
  elementMoralIntentionnel = signal<boolean | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: CodePenalSocialBeService,
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
    // SF-219-24b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.typeInfraction() === null) return false;
    if (this.nombreTravailleursConcernes() === null) return false;
    const nb = this.nombreTravailleursConcernes() as number;
    if (nb < 0 || nb > 1000000) return false;
    if (this.personneMorale() === null) return false;
    if (this.recidiveDansLAn() === null) return false;
    if (this.prevenuPreposeOuMandataire() === null) return false;
    if (this.elementMoralIntentionnel() === null) return false;
    const niv = this.niveauPropose();
    if (niv !== null && (niv < 1 || niv > 4)) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onTypeInfractionChange(value: CodePenalSocialBeTypeInfraction): void {
    this.typeInfraction.set(value);
  }
  onNiveauProposeChange(value: string | number | null): void {
    this.niveauPropose.set(this.toNumberOrNull(value));
  }
  onDateFaitsChange(value: string | null): void {
    this.dateFaits.set(value);
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
  onPrevenuPreposeOuMandataireChange(value: boolean): void {
    this.prevenuPreposeOuMandataire.set(value);
  }
  onElementMoralIntentionnelChange(value: boolean): void {
    this.elementMoralIntentionnel.set(value);
  }

  private toNumberOrNull(value: string | number | null): number | null {
    if (value === null || value === '') return null;
    const n = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(n) ? n : null;
  }

  // -- Libellés humains --
  typeInfractionLabel(value: CodePenalSocialBeTypeInfraction | null): string {
    switch (value) {
      case 'TRAVAIL_NON_DECLARE':
        return 'Travail non déclaré (art. 181 — niveau 4)';
      case 'ABSENCE_DIMONA':
        return 'Absence Dimona (art. 181 — niveau 4)';
      case 'ABSENCE_DMFA':
        return 'Absence DmfA ONSS (art. 223 — niveau 4 si intentionnel, sinon niveau 2)';
      case 'NON_PAIEMENT_REMUNERATION':
        return 'Non-paiement de la rémunération (art. 162-167 — niveau 2)';
      case 'DEPASSEMENT_TEMPS_TRAVAIL':
        return 'Dépassement temps de travail (art. 137-138 — niveau 3)';
      case 'INFRACTION_BIEN_ETRE':
        return 'Manquement bien-être au travail (art. 119-132 — niveau 3)';
      case 'ACCIDENT_GRAVE_NON_DECLARE':
        return 'Accident grave non déclaré (art. 124 — niveau 4)';
      case 'DISCRIMINATION_EMPLOI':
        return 'Discrimination à l\'emploi (art. 145 — niveau 2)';
      case 'ENTRAVE_INSPECTION_SOCIALE':
        return 'Entrave à l\'inspection sociale (art. 209-212 — niveau 3)';
      case 'FAUX_SOCIAL':
        return 'Faux en écritures sociales (art. 232-235 — niveau 4)';
      case 'ABSENCE_REGISTRE_PERSONNEL':
        return 'Absence registre du personnel (art. 185 — niveau 3)';
      case 'INFRACTION_INTERIM':
        return 'Infraction travail intérimaire (art. 178 — niveau 3)';
      case 'INFRACTION_DETACHEMENT':
        return 'Infraction détachement / LIMOSA (art. 182-184 — niveau 3 ou 4)';
      case 'AUTRE_QUALIFICATION':
        return 'Autre qualification — niveau à préciser';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'SANCTION_NIVEAU_1':
        return 'Sanction de niveau 1 — amende administrative 10 € à 100 € (art. 101 § 1 C. pén. soc., montant non indexé, pas d\'inscription au casier judiciaire)';
      case 'SANCTION_NIVEAU_2':
        return 'Sanction de niveau 2 — amende administrative 50 € à 500 € OU amende pénale 50 € à 500 € au choix de l\'auditorat (art. 102 + 73-78 C. pén. soc.)';
      case 'SANCTION_NIVEAU_3':
        return 'Sanction de niveau 3 — amende administrative 100 € à 1 000 € OU amende pénale 100 € à 1 000 € (art. 102 C. pén. soc., pas d\'emprisonnement)';
      case 'SANCTION_NIVEAU_4':
        return 'Sanction de niveau 4 — amende administrative 300 € à 3 000 € OU amende pénale 600 € à 6 000 € ET / OU emprisonnement 6 mois à 3 ans (art. 103 C. pén. soc., infractions les plus graves)';
      case 'A_QUALIFIER':
        return 'Qualification ouverte — type d\'infraction incertain (AUTRE_QUALIFICATION sans niveau renseigné), l\'avocat doit établir la qualification précise';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (rouge critique / ambre warn / gris info / neutre). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'SANCTION_NIVEAU_4':
        return 'verdict-critical';
      case 'SANCTION_NIVEAU_3':
      case 'SANCTION_NIVEAU_2':
        return 'verdict-warn';
      case 'SANCTION_NIVEAU_1':
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
    if (!r) return 'gavel';
    switch (r.verdict) {
      case 'SANCTION_NIVEAU_4':
        return 'gavel';
      case 'SANCTION_NIVEAU_3':
        return 'report';
      case 'SANCTION_NIVEAU_2':
        return 'warning';
      case 'SANCTION_NIVEAU_1':
        return 'info';
      case 'A_QUALIFIER':
        return 'help_outline';
      default:
        return 'gavel';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: CodePenalSocialBeVerdict): string {
    switch (verdict) {
      case 'SANCTION_NIVEAU_1':
        return 'NIVEAU 1';
      case 'SANCTION_NIVEAU_2':
        return 'NIVEAU 2';
      case 'SANCTION_NIVEAU_3':
        return 'NIVEAU 3';
      case 'SANCTION_NIVEAU_4':
        return 'NIVEAU 4';
      case 'A_QUALIFIER':
        return 'À QUALIFIER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: CodePenalSocialBeVerdict): string {
    switch (verdict) {
      case 'SANCTION_NIVEAU_4':
        return 'badge-critical';
      case 'SANCTION_NIVEAU_3':
      case 'SANCTION_NIVEAU_2':
        return 'badge-warn';
      case 'SANCTION_NIVEAU_1':
        return 'badge-info';
      case 'A_QUALIFIER':
        return 'badge-neutral';
      default:
        return '';
    }
  }

  /** Format euros — entiers (valeurs de base C. pén. soc.). */
  formatEuros(value: number | null | undefined): string {
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

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: CodePenalSocialBeRequest = {
      typeInfraction: this.typeInfraction()!,
      niveauPropose: this.niveauPropose(),
      dateFaits: this.dateFaits(),
      nombreTravailleursConcernes: this.nombreTravailleursConcernes()!,
      personneMorale: this.personneMorale()!,
      recidiveDansLAn: this.recidiveDansLAn()!,
      prevenuPreposeOuMandataire: this.prevenuPreposeOuMandataire()!,
      elementMoralIntentionnel: this.elementMoralIntentionnel()!,
    };
    this.calculating.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse Code pénal social calculée', 'OK', { duration: 2500 });
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
        this.typeInfraction.set(r.typeInfraction);
        this.niveauPropose.set(r.niveauPropose);
        this.dateFaits.set(r.dateFaits);
        this.nombreTravailleursConcernes.set(r.nombreTravailleursConcernes);
        this.personneMorale.set(r.personneMorale);
        this.recidiveDansLAn.set(r.recidiveDansLAn);
        this.prevenuPreposeOuMandataire.set(r.prevenuPreposeOuMandataire);
        this.elementMoralIntentionnel.set(r.elementMoralIntentionnel);
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
