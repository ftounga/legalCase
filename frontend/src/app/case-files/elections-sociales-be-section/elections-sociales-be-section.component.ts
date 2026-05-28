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
  ElectionsSocialesBeCycle,
  ElectionsSocialesBeRequest,
  ElectionsSocialesBeResponse,
  ElectionsSocialesBeVerdict,
} from '../../core/models/elections-sociales-be.model';
import {
  ElectionsSocialesBeService,
} from '../../core/services/elections-sociales-be.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  ElectionsSocialesBeSectionPrefillRules,
} from './elections-sociales-be-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-09b : outil décisionnel « Élections sociales (Belgique) »
 * (F-219).
 *
 * <p>BE uniquement — <b>Loi du 04/12/2007</b> (élections sociales) +
 * <b>AR du 25/05/2012</b> (modalités d'organisation) + <b>Loi du
 * 04/08/1996 art. 49</b> (CPPT) + <b>Loi du 19/03/1991</b> (protection
 * candidats contre licenciement).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id {@code elections-sociales-be},
 * visibility ALWAYS_ON priority 127 BE / DROIT_DU_TRAVAIL — au-dessus
 * de {@code transfert-entreprise-cct-32bis} (126, SF-219-08b).</p>
 *
 * <p><b>Verdict 4 états</b> :
 * <ul>
 *   <li>{@code OBLIGATION_CE_ET_CPPT} (vert) — ≥ 100 ETP : double scrutin
 *       CE + CPPT à organiser dans la fenêtre Y imposée par AR.</li>
 *   <li>{@code OBLIGATION_CPPT_SEUL} (ambre clair) — 50-99 ETP : scrutin
 *       CPPT seul.</li>
 *   <li>{@code NON_APPLICABLE_SEUIL} (gris info) — &lt; 50 ETP : pas
 *       d'obligation Loi 04/12/2007.</li>
 *   <li>{@code EFFECTIF_A_RECALCULER} (ambre) — effectif limite (49-51
 *       ou 99-101 ETP) — recalcul ETP requis (sanction pénale art. 32
 *       si manquement).</li>
 * </ul></p>
 *
 * <p><b>Calendrier rebours</b> calculé à partir de Y (X = Y-90,
 * X-60, X-35, X+35, X+40, Y+6, Y+45) — toujours affiché même si
 * obligation absente (conseil prévisionnel).</p>
 *
 * <p><b>Fenêtre de protection candidats</b> (Loi 19/03/1991) — début
 * occulte rétroactif X+10 (≈ 30 j avant affichage), fin = affichage +
 * 2 ans (non-élus). Affichée si obligation existe.</p>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213/F-219 (cycle 4 ans imposé par AR, dateJourY employeur,
 * effectifMoyenEtp RH employeur, UTE paritaire — aucun champ
 * extractible depuis pipeline Travail BE actuel).</p>
 */
@Component({
  selector: 'app-elections-sociales-be-section',
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
  templateUrl: './elections-sociales-be-section.component.html',
  styleUrl: './elections-sociales-be-section.component.scss',
})
export class ElectionsSocialesBeSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'elections-sociales-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'ÉLECTIONS SOCIALES (BE)';
  static readonly TOOL_ICON = 'how_to_vote';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return ElectionsSocialesBeSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR (CSE = calendrier libre, seuils 11/50/300). */
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
  result = signal<ElectionsSocialesBeResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  cycleElectoral = signal<ElectionsSocialesBeCycle | null>(null);
  dateJourY = signal<string | null>(null);
  effectifMoyenEtp = signal<number | null>(null);
  uniteTechniqueExploitationConfirmee = signal<boolean | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: ElectionsSocialesBeService,
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
    // SF-219-09b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.cycleElectoral() === null) return false;
    if (!this.dateJourY()) return false;
    if (this.effectifMoyenEtp() === null) return false;
    if ((this.effectifMoyenEtp() as number) < 0) return false;
    if (this.uniteTechniqueExploitationConfirmee() === null) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onCycleElectoralChange(value: ElectionsSocialesBeCycle): void {
    this.cycleElectoral.set(value);
  }
  onDateJourYChange(value: string | null): void {
    this.dateJourY.set(value);
  }
  onEffectifMoyenEtpChange(value: string | number | null): void {
    if (value === null || value === '') {
      this.effectifMoyenEtp.set(null);
      return;
    }
    const n = typeof value === 'number' ? value : Number(value);
    this.effectifMoyenEtp.set(Number.isFinite(n) ? n : null);
  }
  onUniteTechniqueExploitationConfirmeeChange(value: boolean): void {
    this.uniteTechniqueExploitationConfirmee.set(value);
  }

  // -- Libellés humains --
  cycleLabel(value: ElectionsSocialesBeCycle | null): string {
    switch (value) {
      case 'CYCLE_2024':
        return 'Cycle 2024 — fenêtre Y : 13-26 mai 2024 (AR 14/07/2022)';
      case 'CYCLE_2028':
        return 'Cycle 2028 — fenêtre Y : 11-24 mai 2028 (annoncé)';
      case 'CYCLE_2032':
        return 'Cycle 2032 — fenêtre Y : mai 2032 (prévisionnel)';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'OBLIGATION_CE_ET_CPPT':
        return 'Élections obligatoires — Conseil d\'entreprise + CPPT';
      case 'OBLIGATION_CPPT_SEUL':
        return 'Élections obligatoires — CPPT seul';
      case 'NON_APPLICABLE_SEUIL':
        return 'Pas d\'élections sociales obligatoires (effectif < 50 ETP)';
      case 'EFFECTIF_A_RECALCULER':
        return 'Effectif limite — recalcul ETP requis avant déclenchement';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert obligation, ambre limite/cppt, gris non applicable). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'OBLIGATION_CE_ET_CPPT':
        return 'verdict-ok';
      case 'OBLIGATION_CPPT_SEUL':
        return 'verdict-ok';
      case 'EFFECTIF_A_RECALCULER':
        return 'verdict-warn';
      case 'NON_APPLICABLE_SEUIL':
        return 'verdict-neutral';
      default:
        return '';
    }
  }

  /** Icône Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'how_to_vote';
    switch (r.verdict) {
      case 'OBLIGATION_CE_ET_CPPT':
      case 'OBLIGATION_CPPT_SEUL':
        return 'verified';
      case 'EFFECTIF_A_RECALCULER':
        return 'warning_amber';
      case 'NON_APPLICABLE_SEUIL':
        return 'info';
      default:
        return 'how_to_vote';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: ElectionsSocialesBeVerdict): string {
    switch (verdict) {
      case 'OBLIGATION_CE_ET_CPPT':
        return 'CE + CPPT';
      case 'OBLIGATION_CPPT_SEUL':
        return 'CPPT SEUL';
      case 'NON_APPLICABLE_SEUIL':
        return 'NON APPLICABLE';
      case 'EFFECTIF_A_RECALCULER':
        return 'À RECALCULER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: ElectionsSocialesBeVerdict): string {
    switch (verdict) {
      case 'OBLIGATION_CE_ET_CPPT':
      case 'OBLIGATION_CPPT_SEUL':
        return 'badge-ok';
      case 'EFFECTIF_A_RECALCULER':
        return 'badge-warn';
      case 'NON_APPLICABLE_SEUIL':
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
    const request: ElectionsSocialesBeRequest = {
      cycleElectoral: this.cycleElectoral()!,
      dateJourY: this.dateJourY()!,
      effectifMoyenEtp: this.effectifMoyenEtp()!,
      uniteTechniqueExploitationConfirmee: this.uniteTechniqueExploitationConfirmee()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Élections sociales analysées', 'OK', { duration: 2500 });
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
        this.cycleElectoral.set(r.cycleElectoral);
        this.dateJourY.set(r.dateJourY);
        this.effectifMoyenEtp.set(r.effectifMoyenEtp);
        this.uniteTechniqueExploitationConfirmee.set(r.uniteTechniqueExploitationConfirmee);
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
