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
import { MatCheckboxModule } from '@angular/material/checkbox';
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
  AmpleurModification,
  LicenciementBeActeEquipollentVerdict,
  LicenciementBeActeEquivalentRequest,
  LicenciementBeActeEquivalentResponse,
  TypeModificationUnilaterale,
} from '../../core/models/licenciement-be-acte-equivalent.model';
import {
  LicenciementBeActeEquivalentService,
} from '../../core/services/licenciement-be-acte-equivalent.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  LicenciementBeActeEquivalentSectionPrefillRules,
} from './licenciement-be-acte-equivalent-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-213-09b : outil décisionnel « Acte équipollent à rupture (Belgique) »
 * (F-213).
 *
 * <p>BE uniquement — <b>Loi du 03/07/1978 art. 20</b> + jurisprudence
 * <b>Cass. BE 23/12/1957</b> (modification unilatérale substantielle d'un
 * élément essentiel = rupture imputable à l'employeur).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id
 * {@code licenciement-be-acte-equivalent}, visibility ALWAYS_ON
 * priority 117 BE / DROIT_DU_TRAVAIL — juste au-dessus de
 * {@code licenciement-be-protection-deleguee} (116). Pattern miroir des
 * autres outils F-213 vagues 1-8.</p>
 *
 * <p><b>Verdict 4 états</b> :
 * <ul>
 *   <li>{@code ACTE_EQUIPOLLENT_PROBABLE} (rouge) — modification substantielle
 *       d'un élément essentiel : rupture imputable à l'employeur ;
 *       affiche l'ICP indicatif si données suffisantes.</li>
 *   <li>{@code PAS_ACTE_EQUIPOLLENT} (vert) — modification mineure
 *       (Ius Variandi).</li>
 *   <li>{@code RISQUE_ACCEPTATION_TACITE} (rouge) — silence du salarié
 *       > 30 j sur modification potentiellement substantielle.</li>
 *   <li>{@code A_ANALYSER} (orange) — analyse approfondie requise.</li>
 * </ul></p>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 vagues 6b/7b/8b ; l'extraction IA Travail BE actuelle ne couvre
 * pas la qualification de la modification unilatérale.</p>
 *
 * <p>Distinct du dispositif FR (prise d'acte L. 1237-19 C. trav. +
 * résiliation judiciaire) — gating par {@code workspaceCountry}.</p>
 */
@Component({
  selector: 'app-licenciement-be-acte-equivalent-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatCheckboxModule, MatSelectModule,
    MatListModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './licenciement-be-acte-equivalent-section.component.html',
  styleUrl: './licenciement-be-acte-equivalent-section.component.scss',
})
export class LicenciementBeActeEquivalentSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'licenciement-be-acte-equivalent';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'ACTE ÉQUIPOLLENT À RUPTURE (BE)';
  static readonly TOOL_ICON = 'compare_arrows';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return LicenciementBeActeEquivalentSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR (prise d'acte FR est un dispositif distinct). */
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
  result = signal<LicenciementBeActeEquivalentResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  typeModification = signal<TypeModificationUnilaterale | null>(null);
  ampleurModification = signal<AmpleurModification | null>(null);
  elementEssentielDuContrat = signal<boolean>(false);
  dateModification = signal<string | null>(null);
  salarieAProteste = signal<boolean>(false);
  delaiDepuisModificationJours = signal<number | null>(null);
  remunerationHebdomadaireBrute = signal<number | null>(null);
  dureePreavisCalculeeSemaines = signal<number | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: LicenciementBeActeEquivalentService,
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
    // SF-213-09b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.typeModification() === null) return false;
    if (this.ampleurModification() === null) return false;
    if (!this.dateModification()) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onTypeModificationChange(value: TypeModificationUnilaterale): void {
    this.typeModification.set(value);
  }
  onAmpleurModificationChange(value: AmpleurModification): void {
    this.ampleurModification.set(value);
  }
  onElementEssentielDuContratChange(value: boolean): void {
    this.elementEssentielDuContrat.set(value);
  }
  onDateModificationChange(value: string | null): void {
    this.dateModification.set(value || null);
  }
  onSalarieAProtesteChange(value: boolean): void {
    this.salarieAProteste.set(value);
  }
  onDelaiDepuisModificationJoursChange(value: number | null): void {
    this.delaiDepuisModificationJours.set(value);
  }
  onRemunerationHebdomadaireBruteChange(value: number | null): void {
    this.remunerationHebdomadaireBrute.set(value);
  }
  onDureePreavisCalculeeSemainesChange(value: number | null): void {
    this.dureePreavisCalculeeSemaines.set(value);
  }

  // -- Libellés humains --
  typeModificationLabel(value: TypeModificationUnilaterale | null): string {
    switch (value) {
      case 'SALAIRE': return 'Salaire (taux, structure, primes)';
      case 'LIEU_TRAVAIL': return 'Lieu de travail (hors clause mobilité)';
      case 'FONCTION': return 'Fonction (responsabilités, qualification)';
      case 'HORAIRE': return 'Horaire de travail';
      case 'AUTRE': return 'Autre élément du contrat';
      default: return '—';
    }
  }

  ampleurModificationLabel(value: AmpleurModification | null): string {
    switch (value) {
      case 'MINEURE': return 'Mineure (Ius Variandi)';
      case 'SUBSTANTIELLE': return 'Substantielle';
      case 'INDETERMINEES': return 'Indéterminée (analyse requise)';
      default: return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'ACTE_EQUIPOLLENT_PROBABLE':
        return 'Acte équipollent à rupture probable';
      case 'PAS_ACTE_EQUIPOLLENT':
        return 'Pas d\'acte équipollent — Ius Variandi';
      case 'RISQUE_ACCEPTATION_TACITE':
        return 'Risque d\'acceptation tacite';
      case 'A_ANALYSER':
        return 'Analyse approfondie requise';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (rouge probable/risque, vert pas, orange à analyser). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'ACTE_EQUIPOLLENT_PROBABLE':
      case 'RISQUE_ACCEPTATION_TACITE':
        return 'verdict-critical';
      case 'PAS_ACTE_EQUIPOLLENT':
        return 'verdict-ok';
      case 'A_ANALYSER':
        return 'verdict-warn';
      default:
        return '';
    }
  }

  /** Icône Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'gavel';
    switch (r.verdict) {
      case 'ACTE_EQUIPOLLENT_PROBABLE':
        return 'gpp_bad';
      case 'RISQUE_ACCEPTATION_TACITE':
        return 'warning';
      case 'PAS_ACTE_EQUIPOLLENT':
        return 'verified';
      case 'A_ANALYSER':
        return 'help_outline';
      default:
        return 'gavel';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: LicenciementBeActeEquipollentVerdict): string {
    switch (verdict) {
      case 'ACTE_EQUIPOLLENT_PROBABLE': return 'ACTE ÉQUIPOLLENT';
      case 'RISQUE_ACCEPTATION_TACITE': return 'RISQUE TACITE';
      case 'PAS_ACTE_EQUIPOLLENT': return 'PAS D\'ACTE';
      case 'A_ANALYSER': return 'À ANALYSER';
      default: return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: LicenciementBeActeEquipollentVerdict): string {
    switch (verdict) {
      case 'ACTE_EQUIPOLLENT_PROBABLE':
      case 'RISQUE_ACCEPTATION_TACITE':
        return 'badge-critical';
      case 'PAS_ACTE_EQUIPOLLENT':
        return 'badge-ok';
      case 'A_ANALYSER':
        return 'badge-warn';
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
    const request: LicenciementBeActeEquivalentRequest = {
      typeModification: this.typeModification()!,
      ampleurModification: this.ampleurModification()!,
      dateModification: this.dateModification()!,
      elementEssentielDuContrat: this.elementEssentielDuContrat(),
      salarieAProteste: this.salarieAProteste(),
      delaiDepuisModificationJours: this.delaiDepuisModificationJours(),
      remunerationHebdomadaireBrute: this.remunerationHebdomadaireBrute(),
      dureePreavisCalculeeSemaines: this.dureePreavisCalculeeSemaines(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Acte équipollent analysé', 'OK', { duration: 2500 });
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
        this.typeModification.set(r.typeModification);
        this.ampleurModification.set(r.ampleurModification);
        this.elementEssentielDuContrat.set(r.elementEssentielDuContrat);
        this.dateModification.set(r.dateModification);
        this.salarieAProteste.set(r.salarieAProteste);
        this.delaiDepuisModificationJours.set(r.delaiDepuisModificationJours);
        this.remunerationHebdomadaireBrute.set(r.remunerationHebdomadaireBrute);
        this.dureePreavisCalculeeSemaines.set(r.dureePreavisCalculeeSemaines);
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
