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
  LicenciementBeProtectionGrossesseRequest,
  LicenciementBeProtectionGrossesseResponse,
  LicenciementBeProtectionGrossesseVerdict,
} from '../../core/models/licenciement-be-protection-grossesse.model';
import {
  LicenciementBeProtectionGrossesseService,
} from '../../core/services/licenciement-be-protection-grossesse.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  LicenciementBeProtectionGrossesseSectionPrefillRules,
} from './licenciement-be-protection-grossesse-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-213-05b : outil décisionnel "Protection grossesse BE" (F-213).
 *
 * BE uniquement — Loi du 16 mars 1971 sur la protection de la maternité,
 * art. 40 (interdiction de licenciement pendant la période protégée).
 *
 * <p>Affiché par le panel F-IA-04 (tool_id `licenciement-be-protection-grossesse`,
 * visibility ALWAYS_ON priority 113 BE / DROIT_DU_TRAVAIL — priorité juste
 * au-dessus de `licenciement-be-formule-claeys` (112). Pattern miroir des
 * autres outils F-213 vague 3-4.</p>
 *
 * <p><b>Verdict 4 états</b> :
 * <ul>
 *   <li>{@code HORS_PERIODE_PROTECTION} — vert</li>
 *   <li>{@code PROTECTION_APPLICABLE_NON_NOTIFIEE} — ambre</li>
 *   <li>{@code PROTECTION_APPLICABLE} — rouge</li>
 *   <li>{@code PROTECTION_PRESUMEE} — rouge foncé (charge preuve renversée)</li>
 * </ul></p>
 */
@Component({
  selector: 'app-licenciement-be-protection-grossesse-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatCheckboxModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './licenciement-be-protection-grossesse-section.component.html',
  styleUrl: './licenciement-be-protection-grossesse-section.component.scss',
})
export class LicenciementBeProtectionGrossesseSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'licenciement-be-protection-grossesse';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'PROTECTION GROSSESSE (BE)';
  static readonly TOOL_ICON = 'pregnant_woman';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return LicenciementBeProtectionGrossesseSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — la France a un régime distinct (L.1225-4 C. trav.) hors scope SF-213-05. */
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private cdr = inject(ChangeDetectorRef);

  // Snapshot signal de aiData pour réactivité (parité pattern).
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<LicenciementBeProtectionGrossesseResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  dateDebutGrossesse = signal<string | null>(null);
  dateAccouchement = signal<string | null>(null);
  dateCongeMaterniteDebut = signal<string | null>(null);
  dateCongeMaterniteFinale = signal<string | null>(null);
  dateLicenciement = signal<string | null>(null);
  grossesseNotifieeParEcrit = signal<boolean>(false);
  remunerationMensuelleBrute = signal<number | null>(null);
  motifInvoqueParEmployeur = signal<string | null>(null);

  // Provenance IA par champ pré-rempli (les autres sont saisie avocat).
  provenanceDateLicenciement = signal<'IA' | null>(null);
  provenanceRemuneration = signal<'IA' | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  // Formateur € (fr-BE) instancié une fois.
  private readonly euroFormatter = new Intl.NumberFormat('fr-BE', {
    style: 'currency',
    currency: 'EUR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  });

  constructor(
    private service: LicenciementBeProtectionGrossesseService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    if (this.workspaceCountry === 'BELGIQUE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);

    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()
        && this.workspaceCountry === 'BELGIQUE') {
      this.prefillFromAi();
    }
  }

  /**
   * SF-213-05b — Pré-remplit le form depuis `aiData` via le helper partagé.
   * Parité runtime / static `getPrefillCount` garantie par construction.
   *
   * <p>2 champs pré-remplis depuis le pipeline IA Travail BE existant :
   *   - dateLicenciement (dateLicenciement)
   *   - remunerationMensuelleBrute (salaireBrutMensuel ou salaireBrutAnnuel/12)
   *
   * <p>Les autres champs (grossesse, accouchement, congé maternité,
   * notification, motif) restent en saisie avocat — pas d'extraction IA
   * BE-only dédiée dans cette vague (cf. helper doc).</p>
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;
    if (this.workspaceCountry !== 'BELGIQUE') return;

    const ruleInput: PrefillCountInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    // 1. dateLicenciement.
    const dLic = LicenciementBeProtectionGrossesseSectionPrefillRules.computeDateLicenciement(ruleInput);
    if (dLic !== null
        && (this.dateLicenciement() === null || this.provenanceDateLicenciement() === 'IA')) {
      this.dateLicenciement.set(dLic);
      this.provenanceDateLicenciement.set('IA');
    }

    // 2. rémunération mensuelle brute.
    const remu = LicenciementBeProtectionGrossesseSectionPrefillRules.computeRemunerationMensuelleBrute(ruleInput);
    if (remu !== null
        && (this.remunerationMensuelleBrute() === null || this.provenanceRemuneration() === 'IA')) {
      this.remunerationMensuelleBrute.set(remu);
      this.provenanceRemuneration.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    // 3 champs obligatoires : dateDebutGrossesse, dateLicenciement, remunerationMensuelleBrute.
    if (!this.dateDebutGrossesse()) return false;
    if (!this.dateLicenciement()) return false;
    const remu = this.remunerationMensuelleBrute();
    if (remu === null || remu === undefined || Number.isNaN(remu) || remu <= 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers — effacent la provenance IA au 1er changement manuel. --
  onDateDebutGrossesseChange(value: string | null): void {
    this.dateDebutGrossesse.set(value || null);
  }
  onDateAccouchementChange(value: string | null): void {
    this.dateAccouchement.set(value || null);
  }
  onDateCongeMaterniteDebutChange(value: string | null): void {
    this.dateCongeMaterniteDebut.set(value || null);
  }
  onDateCongeMaterniteFinaleChange(value: string | null): void {
    this.dateCongeMaterniteFinale.set(value || null);
  }
  onDateLicenciementChange(value: string | null): void {
    this.dateLicenciement.set(value || null);
    this.provenanceDateLicenciement.set(null);
  }
  onGrossesseNotifieeChange(value: boolean): void {
    this.grossesseNotifieeParEcrit.set(value);
  }
  onRemunerationChange(value: number | null): void {
    this.remunerationMensuelleBrute.set(value);
    this.provenanceRemuneration.set(null);
  }
  onMotifChange(value: string | null): void {
    this.motifInvoqueParEmployeur.set(value || null);
  }

  /** Formate un montant € au format fr-BE. `null` / `NaN` → '—'. */
  formatEuro(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) return '—';
    return this.euroFormatter.format(value);
  }

  /** Formate une date ISO en JJ/MM/AAAA fr-BE. `null` → '—'. */
  formatDate(value: string | null | undefined): string {
    if (!value) return '—';
    const d = new Date(value);
    if (Number.isNaN(d.getTime())) return '—';
    return d.toLocaleDateString('fr-BE', { day: '2-digit', month: '2-digit', year: 'numeric' });
  }

  /**
   * Classe CSS du badge verdict + couleur de la bannière résultat —
   * mapping métier strict :
   *   - HORS_PERIODE_PROTECTION → verdict-ok (vert)
   *   - PROTECTION_APPLICABLE_NON_NOTIFIEE → verdict-warn (ambre)
   *   - PROTECTION_APPLICABLE → verdict-critical (rouge)
   *   - PROTECTION_PRESUMEE → verdict-severe (rouge foncé)
   */
  verdictClass(verdict: LicenciementBeProtectionGrossesseVerdict | undefined): string {
    switch (verdict) {
      case 'HORS_PERIODE_PROTECTION': return 'verdict-ok';
      case 'PROTECTION_APPLICABLE_NON_NOTIFIEE': return 'verdict-warn';
      case 'PROTECTION_APPLICABLE': return 'verdict-critical';
      case 'PROTECTION_PRESUMEE': return 'verdict-severe';
      default: return 'verdict-info';
    }
  }

  /** Libellé humain du verdict (FR). */
  verdictLabel(verdict: LicenciementBeProtectionGrossesseVerdict | undefined): string {
    switch (verdict) {
      case 'HORS_PERIODE_PROTECTION': return 'Hors période de protection';
      case 'PROTECTION_APPLICABLE_NON_NOTIFIEE': return 'Protection applicable — grossesse non notifiée';
      case 'PROTECTION_APPLICABLE': return 'Protection applicable';
      case 'PROTECTION_PRESUMEE': return 'Protection présumée — charge de preuve renversée';
      default: return '—';
    }
  }

  /** Icône Material associée au verdict. */
  verdictIcon(verdict: LicenciementBeProtectionGrossesseVerdict | undefined): string {
    switch (verdict) {
      case 'HORS_PERIODE_PROTECTION': return 'check_circle';
      case 'PROTECTION_APPLICABLE_NON_NOTIFIEE': return 'warning';
      case 'PROTECTION_APPLICABLE': return 'gpp_bad';
      case 'PROTECTION_PRESUMEE': return 'gavel';
      default: return 'info';
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: LicenciementBeProtectionGrossesseRequest = {
      dateDebutGrossesse: this.dateDebutGrossesse()!,
      dateAccouchement: this.dateAccouchement() ?? null,
      dateCongeMaterniteDebut: this.dateCongeMaterniteDebut() ?? null,
      dateCongeMaterniteFinale: this.dateCongeMaterniteFinale() ?? null,
      dateLicenciement: this.dateLicenciement()!,
      grossesseNotifieeParEcrit: this.grossesseNotifieeParEcrit(),
      remunerationMensuelleBrute: this.remunerationMensuelleBrute()!,
      motifInvoqueParEmployeur: this.motifInvoqueParEmployeur() ?? null,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Protection grossesse BE analysée', 'OK', { duration: 2500 });
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
        // GET = saisie avocat persistée → reset provenance IA.
        this.provenanceDateLicenciement.set(null);
        this.provenanceRemuneration.set(null);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 attendu si aucune analyse — mode formulaire + pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
