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
  TransactionBeTravailRequest,
  TransactionBeTravailResponse,
  TransactionBeTravailVerdict,
  TransactionBeTravailRaisonInvalidite,
} from '../../core/models/transaction-be-travail.model';
import {
  TransactionBeTravailService,
} from '../../core/services/transaction-be-travail.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  TransactionBeTravailSectionPrefillRules,
} from './transaction-be-travail-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-213-06b : outil décisionnel "Transaction de fin de contrat BE" (F-213).
 *
 * BE uniquement — art. 2044 Code civil belge + Loi du 03/07/1978 art. 6
 * (analyse de validité d'une transaction de fin de contrat de travail belge).
 *
 * <p>Affiché par le panel F-IA-04 (tool_id {@code transaction-be-travail},
 * visibility ALWAYS_ON priority 114 BE / DROIT_DU_TRAVAIL — juste au-dessus
 * de {@code licenciement-be-protection-grossesse} (113). Pattern miroir
 * des autres outils F-213 vague 1-5.</p>
 *
 * <p><b>Verdict 4 états</b> :
 * <ul>
 *   <li>{@code VALIDE} — vert</li>
 *   <li>{@code INVALIDE} — rouge (raison ABSENCE_CONCESSIONS)</li>
 *   <li>{@code INVALIDE_PARTIELLE} — ambre (raison RENONCIATION_ORDRE_PUBLIC)</li>
 *   <li>{@code A_COMPLETER} — gris (raison LITIGE_NON_VISE)</li>
 * </ul></p>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ pré-rempli — l'analyse fine du
 * document de transaction est explicitement hors scope V1 (cf. helper
 * doc). Saisie avocat des 6 champs depuis le document.</p>
 */
@Component({
  selector: 'app-transaction-be-travail-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatCheckboxModule,
    MatListModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './transaction-be-travail-section.component.html',
  styleUrl: './transaction-be-travail-section.component.scss',
})
export class TransactionBeTravailSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'transaction-be-travail';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'TRANSACTION FIN CONTRAT (BE)';
  static readonly TOOL_ICON = 'handshake';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return TransactionBeTravailSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — la France a un outil distinct (F-DT-31-transaction, régime art. 2044 Cciv FR). */
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
  result = signal<TransactionBeTravailResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  montantTransactionBrut = signal<number | null>(null);
  indemniteLegaleEtimee = signal<number | null>(null);
  concessionsEmployeurDescrites = signal<boolean>(false);
  /** Textarea multi-lignes — chaque ligne non vide = une renonciation. */
  renonciationsRaw = signal<string>('');
  renonciationOrdrePublicDetectee = signal<boolean>(false);
  mentionContestation = signal<boolean>(false);

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
    private service: TransactionBeTravailService,
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
    // SF-213-06b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  /** Liste finale des renonciations (lignes non vides du textarea). */
  renonciationsListees(): string[] {
    return this.renonciationsRaw()
      .split(/\r?\n/)
      .map(s => s.trim())
      .filter(s => s.length > 0);
  }

  formValid(): boolean {
    const montant = this.montantTransactionBrut();
    if (montant === null || montant === undefined || Number.isNaN(montant) || montant <= 0) return false;
    // renonciationsListees peut être vide (verdict A_COMPLETER côté backend).
    // indemniteLegaleEtimee est optionnel.
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onMontantChange(value: number | null): void {
    this.montantTransactionBrut.set(value);
  }
  onIndemniteChange(value: number | null): void {
    this.indemniteLegaleEtimee.set(value);
  }
  onConcessionsChange(value: boolean): void {
    this.concessionsEmployeurDescrites.set(value);
  }
  onRenonciationsRawChange(value: string | null): void {
    this.renonciationsRaw.set(value || '');
  }
  onOrdrePublicChange(value: boolean): void {
    this.renonciationOrdrePublicDetectee.set(value);
  }
  onMentionContestationChange(value: boolean): void {
    this.mentionContestation.set(value);
  }

  /** Formate un montant € au format fr-BE. `null` / `NaN` → '—'. */
  formatEuro(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) return '—';
    return this.euroFormatter.format(value);
  }

  /** Formate un ratio en %, JetBrains Mono (gabarit), 1 décimale. `null` → '—'. */
  formatRatio(value: number | null | undefined): string {
    if (value === null || value === undefined || Number.isNaN(value)) return '—';
    return value.toFixed(1) + ' %';
  }

  /**
   * Classe CSS du badge verdict + couleur de la bannière résultat —
   * mapping métier strict :
   *   - VALIDE → verdict-ok (vert)
   *   - INVALIDE → verdict-critical (rouge)
   *   - INVALIDE_PARTIELLE → verdict-warn (ambre)
   *   - A_COMPLETER → verdict-info (gris)
   */
  verdictClass(verdict: TransactionBeTravailVerdict | undefined): string {
    switch (verdict) {
      case 'VALIDE': return 'verdict-ok';
      case 'INVALIDE': return 'verdict-critical';
      case 'INVALIDE_PARTIELLE': return 'verdict-warn';
      case 'A_COMPLETER': return 'verdict-info';
      default: return 'verdict-info';
    }
  }

  /** Libellé humain du verdict (FR). */
  verdictLabel(verdict: TransactionBeTravailVerdict | undefined): string {
    switch (verdict) {
      case 'VALIDE': return 'Transaction valide';
      case 'INVALIDE': return 'Transaction invalide';
      case 'INVALIDE_PARTIELLE': return 'Partiellement invalide';
      case 'A_COMPLETER': return 'À compléter';
      default: return '—';
    }
  }

  /** Icône Material associée au verdict. */
  verdictIcon(verdict: TransactionBeTravailVerdict | undefined): string {
    switch (verdict) {
      case 'VALIDE': return 'check_circle';
      case 'INVALIDE': return 'gpp_bad';
      case 'INVALIDE_PARTIELLE': return 'warning';
      case 'A_COMPLETER': return 'help';
      default: return 'info';
    }
  }

  /** Libellé humain de la raison d'invalidité (null → '—'). */
  raisonLabel(raison: TransactionBeTravailRaisonInvalidite | null | undefined): string {
    switch (raison) {
      case 'ABSENCE_CONCESSIONS': return 'Absence de concessions réciproques de l\'employeur';
      case 'RENONCIATION_ORDRE_PUBLIC': return 'Renonciation à un droit d\'ordre public détectée';
      case 'LITIGE_NON_VISE': return 'Litige non visé ou liste des renonciations vide';
      default: return '—';
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: TransactionBeTravailRequest = {
      montantTransactionBrut: this.montantTransactionBrut()!,
      indemniteLegaleEtimee: this.indemniteLegaleEtimee() ?? null,
      concessionsEmployeurDescrites: this.concessionsEmployeurDescrites(),
      renonciationsListees: this.renonciationsListees(),
      renonciationOrdrePublicDetectee: this.renonciationOrdrePublicDetectee(),
      mentionContestation: this.mentionContestation(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Transaction BE analysée', 'OK', { duration: 2500 });
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
        // GET = saisie avocat persistée → hydrater le textarea pour permettre l'édition.
        this.montantTransactionBrut.set(r.montantTransactionBrut);
        this.indemniteLegaleEtimee.set(r.indemniteLegaleEtimee);
        this.concessionsEmployeurDescrites.set(r.concessionsEmployeurDescrites);
        this.renonciationsRaw.set((r.renonciationsListees ?? []).join('\n'));
        this.renonciationOrdrePublicDetectee.set(r.renonciationOrdrePublicDetectee);
        this.mentionContestation.set(r.mentionContestation);
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
