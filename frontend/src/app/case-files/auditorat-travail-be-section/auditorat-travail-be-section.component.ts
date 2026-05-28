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
  AuditoratTravailBeModeSaisine,
  AuditoratTravailBeNatureFait,
  AuditoratTravailBeRequest,
  AuditoratTravailBeResponse,
  AuditoratTravailBeVerdict,
} from '../../core/models/auditorat-travail-be.model';
import {
  AuditoratTravailBeService,
} from '../../core/services/auditorat-travail-be.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  AuditoratTravailBeSectionPrefillRules,
} from './auditorat-travail-be-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-25b : outil décisionnel « Auditorat du travail BE —
 * orientation et checklist de saisine du parquet spécialisé en droit
 * social pénal » (F-219).
 *
 * <p>BE uniquement — <b>Code judiciaire art. 138bis</b> (compétence
 * de l'auditorat du travail) + <b>Code d'instruction criminelle
 * art. 24</b> (plainte et dénonciation) + <b>Loi du 03/08/1992</b> sur
 * le Code judiciaire + <b>Loi du 06/06/2010</b> introduisant le Code
 * pénal social (M.B. 01/07/2010, e.e.v. 01/07/2011) art. 73-78
 * (règle « Una Via »), art. 76 (transmission PV inspection), art. 81
 * (prescription), art. 105 (× 5 personne morale).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id {@code auditorat-travail-be},
 * visibility ALWAYS_ON priority 143 BE / DROIT_DU_TRAVAIL — au-dessus
 * de {@code code-penal-social-be} (142, SF-219-24b),
 * {@code discrimination-be-handicap-amenagement} (141, SF-219-23b),
 * {@code egalite-femmes-hommes-be} (140, SF-219-22b).</p>
 *
 * <h2>Verdict 4 états (court-circuits backend)</h2>
 * <ul>
 *   <li>{@code SAISINE_AUDITORAT_RECOMMANDEE} (critique) — infraction
 *       pénale sociale caractérisée, accident grave non déclaré,
 *       harcèlement pénal art. 442bis C. pén., discrimination pénale,
 *       entrave inspection. Voie directe par plainte/dénonciation
 *       art. 24 CIC + art. 138bis C. jud.</li>
 *   <li>{@code DENONCIATION_INSPECTION_PREALABLE} (warn) — travail
 *       non déclaré suspecté nécessitant objectivation terrain,
 *       PV inspection transmis art. 76 C. pén. soc.</li>
 *   <li>{@code SAISINE_NON_PERTINENTE} (info) — litige civil pur
 *       (compétence tribunal travail art. 578 C. jud.) ou faits
 *       prescrits (art. 81 C. pén. soc.).</li>
 *   <li>{@code A_QUALIFIER} (neutre) — qualification ouverte (nature
 *       AUTRE / litige civil + recours pénal envisagé) examen avocat
 *       préalable requis.</li>
 * </ul>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 / F-219 (nature des faits, mode de saisine, prescription,
 * urgence, recours pénal, qualité employeur non extractibles du
 * dossier salarié — relèvent de l'analyse stratégique avocat et du
 * dossier d'instruction).</p>
 */
@Component({
  selector: 'app-auditorat-travail-be-section',
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
  templateUrl: './auditorat-travail-be-section.component.html',
  styleUrl: './auditorat-travail-be-section.component.scss',
})
export class AuditoratTravailBeSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'auditorat-travail-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'AUDITORAT DU TRAVAIL (BE)';
  static readonly TOOL_ICON = 'account_balance';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return AuditoratTravailBeSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR mécanique (pas d'auditorat du travail FR). */
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
  result = signal<AuditoratTravailBeResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  natureFait = signal<AuditoratTravailBeNatureFait | null>(null);
  modeSaisineEnvisage = signal<AuditoratTravailBeModeSaisine | null>(null);
  dateFaits = signal<string | null>(null);
  faitsPrescrits = signal<boolean | null>(null);
  inspectionDejaSaisie = signal<boolean | null>(null);
  plainteCivileDeposee = signal<boolean | null>(null);
  urgenceSecuritePersonnes = signal<boolean | null>(null);
  recoursPenalEnvisage = signal<boolean | null>(null);
  employeurPersonneMorale = signal<boolean | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: AuditoratTravailBeService,
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
    // SF-219-25b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.natureFait() === null) return false;
    if (this.modeSaisineEnvisage() === null) return false;
    if (this.faitsPrescrits() === null) return false;
    if (this.inspectionDejaSaisie() === null) return false;
    if (this.plainteCivileDeposee() === null) return false;
    if (this.urgenceSecuritePersonnes() === null) return false;
    if (this.recoursPenalEnvisage() === null) return false;
    if (this.employeurPersonneMorale() === null) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onNatureFaitChange(value: AuditoratTravailBeNatureFait): void {
    this.natureFait.set(value);
  }
  onModeSaisineEnvisageChange(value: AuditoratTravailBeModeSaisine): void {
    this.modeSaisineEnvisage.set(value);
  }
  onDateFaitsChange(value: string | null): void {
    this.dateFaits.set(value);
  }
  onFaitsPrescritsChange(value: boolean): void {
    this.faitsPrescrits.set(value);
  }
  onInspectionDejaSaisieChange(value: boolean): void {
    this.inspectionDejaSaisie.set(value);
  }
  onPlainteCivileDeposeeChange(value: boolean): void {
    this.plainteCivileDeposee.set(value);
  }
  onUrgenceSecuritePersonnesChange(value: boolean): void {
    this.urgenceSecuritePersonnes.set(value);
  }
  onRecoursPenalEnvisageChange(value: boolean): void {
    this.recoursPenalEnvisage.set(value);
  }
  onEmployeurPersonneMoraleChange(value: boolean): void {
    this.employeurPersonneMorale.set(value);
  }

  // -- Libellés humains --
  natureFaitLabel(value: AuditoratTravailBeNatureFait | null): string {
    switch (value) {
      case 'INFRACTION_PENALE_SOCIALE':
        return 'Infraction Code pénal social (art. 119-235 — Livre 2)';
      case 'ACCIDENT_TRAVAIL_GRAVE':
        return 'Accident du travail grave (art. 26 Loi 04/08/1996 + art. 124 C. pén. soc.)';
      case 'TRAVAIL_NON_DECLARE_SUSPECTE':
        return 'Travail non déclaré suspecté (art. 181 C. pén. soc.)';
      case 'DISCRIMINATION_PENALE':
        return 'Discrimination pénale (art. 22-24 Loi 10/05/2007)';
      case 'HARCELEMENT_PENAL':
        return 'Harcèlement moral / sexuel pénal (art. 442bis C. pén.)';
      case 'ENTRAVE_INSPECTION':
        return 'Entrave à l\'inspection sociale (art. 209-212 C. pén. soc.)';
      case 'LITIGE_CIVIL_PUR':
        return 'Litige civil pur (compétence tribunal travail — art. 578 C. jud.)';
      case 'AUTRE':
        return 'Autre nature — qualification ouverte';
      default:
        return '—';
    }
  }

  modeSaisineLabel(value: AuditoratTravailBeModeSaisine | null): string {
    switch (value) {
      case 'PLAINTE_DIRECTE':
        return 'Plainte directe avec constitution partie civile (art. 63 CIC + art. 138bis C. jud.)';
      case 'DENONCIATION_SIMPLE':
        return 'Dénonciation simple sans constitution partie civile (art. 29 CIC)';
      case 'SIGNALEMENT_INSPECTION_SOCIALE':
        return 'Signalement inspection sociale (PV transmis art. 76 C. pén. soc.)';
      case 'VOIE_CIVILE_PARALLELE':
        return 'Voie civile tribunal travail (art. 578 C. jud.) en parallèle';
      case 'INDETERMINE':
        return 'Mode non encore choisi';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'SAISINE_AUDITORAT_RECOMMANDEE':
        return 'Saisine de l\'auditorat du travail recommandée — voie directe par plainte ou dénonciation (art. 24 CIC + art. 138bis C. jud.). L\'auditeur arbitrera entre voie pénale et voie administrative (règle « Una Via » art. 73-78 C. pén. soc.).';
      case 'DENONCIATION_INSPECTION_PREALABLE':
        return 'Orientation vers l\'inspection sociale recommandée préalablement — PV de constatation art. 100 C. pén. soc. transmis à l\'auditeur art. 76. Voie économe pour objectiver les faits avant qualification pénale.';
      case 'SAISINE_NON_PERTINENTE':
        return 'Saisine non pertinente — compétence du tribunal du travail (art. 578 C. jud. — litige civil pur) ou faits prescrits (art. 81 C. pén. soc.). Pas de détour par l\'auditorat.';
      case 'A_QUALIFIER':
        return 'Qualification ouverte — examen avocat préalable requis (nature des faits incertaine ou pluri-qualifications pénale/civile à arbitrer).';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict. */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'SAISINE_AUDITORAT_RECOMMANDEE':
        return 'verdict-critical';
      case 'DENONCIATION_INSPECTION_PREALABLE':
        return 'verdict-warn';
      case 'SAISINE_NON_PERTINENTE':
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
    if (!r) return 'account_balance';
    switch (r.verdict) {
      case 'SAISINE_AUDITORAT_RECOMMANDEE':
        return 'gavel';
      case 'DENONCIATION_INSPECTION_PREALABLE':
        return 'fact_check';
      case 'SAISINE_NON_PERTINENTE':
        return 'info';
      case 'A_QUALIFIER':
        return 'help_outline';
      default:
        return 'account_balance';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: AuditoratTravailBeVerdict): string {
    switch (verdict) {
      case 'SAISINE_AUDITORAT_RECOMMANDEE':
        return 'SAISIR AUDITORAT';
      case 'DENONCIATION_INSPECTION_PREALABLE':
        return 'INSPECTION PRÉALABLE';
      case 'SAISINE_NON_PERTINENTE':
        return 'NON PERTINENT';
      case 'A_QUALIFIER':
        return 'À QUALIFIER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: AuditoratTravailBeVerdict): string {
    switch (verdict) {
      case 'SAISINE_AUDITORAT_RECOMMANDEE':
        return 'badge-critical';
      case 'DENONCIATION_INSPECTION_PREALABLE':
        return 'badge-warn';
      case 'SAISINE_NON_PERTINENTE':
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

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: AuditoratTravailBeRequest = {
      natureFait: this.natureFait()!,
      modeSaisineEnvisage: this.modeSaisineEnvisage()!,
      dateFaits: this.dateFaits(),
      faitsPrescrits: this.faitsPrescrits()!,
      inspectionDejaSaisie: this.inspectionDejaSaisie()!,
      plainteCivileDeposee: this.plainteCivileDeposee()!,
      urgenceSecuritePersonnes: this.urgenceSecuritePersonnes()!,
      recoursPenalEnvisage: this.recoursPenalEnvisage()!,
      employeurPersonneMorale: this.employeurPersonneMorale()!,
    };
    this.calculating.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse auditorat du travail calculée', 'OK', { duration: 2500 });
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
        this.natureFait.set(r.natureFait);
        this.modeSaisineEnvisage.set(r.modeSaisineEnvisage);
        this.dateFaits.set(r.dateFaits);
        this.faitsPrescrits.set(r.faitsPrescrits);
        this.inspectionDejaSaisie.set(r.inspectionDejaSaisie);
        this.plainteCivileDeposee.set(r.plainteCivileDeposee);
        this.urgenceSecuritePersonnes.set(r.urgenceSecuritePersonnes);
        this.recoursPenalEnvisage.set(r.recoursPenalEnvisage);
        this.employeurPersonneMorale.set(r.employeurPersonneMorale);
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
