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
  TransfertEntrepriseCct32bisIdentiteEconomique,
  TransfertEntrepriseCct32bisRequest,
  TransfertEntrepriseCct32bisResponse,
  TransfertEntrepriseCct32bisTypeOperation,
  TransfertEntrepriseCct32bisVerdict,
} from '../../core/models/transfert-entreprise-cct-32bis.model';
import {
  TransfertEntrepriseCct32bisService,
} from '../../core/services/transfert-entreprise-cct-32bis.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  TransfertEntrepriseCct32bisSectionPrefillRules,
} from './transfert-entreprise-cct-32bis-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-08b : outil décisionnel « Transfert d'entreprise — CCT n° 32bis
 * du 07/06/1985 » (F-219).
 *
 * <p>BE uniquement — <b>CCT n° 32bis du 07/06/1985</b> (transfert
 * conventionnel) + <b>Loi du 17/03/1965</b> sur les transferts
 * d'entreprises + <b>Directive 2001/23/CE</b>. Jurisprudence : CJUE
 * Süzen (11/03/1997), Abler (20/11/2003) ; Cass. BE 17/05/1999,
 * Cass. BE 14/02/2011.</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id
 * {@code transfert-entreprise-cct-32bis}, visibility ALWAYS_ON priority
 * 126 BE / DROIT_DU_TRAVAIL — au-dessus de
 * {@code licenciement-be-collectif-renault} (125, SF-219-07b).</p>
 *
 * <p><b>Verdict 5 états</b> :
 * <ul>
 *   <li>{@code TRANSFERT_CONFORME} (vert) — transfert qualifiant +
 *       info-consult respectée + maintien complet des droits.</li>
 *   <li>{@code TRANSFERT_NON_CONFORME_INFO_CONSULT} (ambre) — transfert
 *       qualifiant mais procédure info-consult préalable non respectée
 *       (sanction pénale Loi 17/03/1965 + DI, maintien des contrats
 *       inchangé — ordre public).</li>
 *   <li>{@code INELIGIBLE_TYPE_OPERATION} (rouge) — faillite /
 *       liquidation / cession actions / intra-groupe : régime CCT 32bis
 *       écarté.</li>
 *   <li>{@code INELIGIBLE_PAS_ENTITE_ECONOMIQUE} (rouge) — pas d'entité
 *       économique préservée (cession actifs isolés).</li>
 *   <li>{@code A_ANALYSER} (gris) — qualification d'identité ambiguë,
 *       intervention avocat BE requise.</li>
 * </ul></p>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213/F-219 (date juridique du transfert ≠ date de rupture extraite ;
 * qualifications juridiques 9 cas type opération / 3 cas identité
 * économique + déclarations cessionnaire post-transfert non extractibles
 * depuis pipeline Travail BE actuel).</p>
 */
@Component({
  selector: 'app-transfert-entreprise-cct-32bis-section',
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
  templateUrl: './transfert-entreprise-cct-32bis-section.component.html',
  styleUrl: './transfert-entreprise-cct-32bis-section.component.scss',
})
export class TransfertEntrepriseCct32bisSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'transfert-entreprise-cct-32bis';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'TRANSFERT CCT 32BIS (BE)';
  static readonly TOOL_ICON = 'swap_horiz';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return TransfertEntrepriseCct32bisSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR complet (L. 1224-1 sans spécificités CCT 32bis). */
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
  result = signal<TransfertEntrepriseCct32bisResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  dateTransfert = signal<string | null>(null);
  typeOperation = signal<TransfertEntrepriseCct32bisTypeOperation | null>(null);
  identiteEconomique = signal<TransfertEntrepriseCct32bisIdentiteEconomique | null>(null);
  infoConsultPrealableRespectee = signal<boolean | null>(null);
  conseilEntrepriseConsulte = signal<boolean | null>(null);
  maintienContratsAutomatique = signal<boolean | null>(null);
  maintienAncienneteRespecte = signal<boolean | null>(null);
  maintienRemunerationRespecte = signal<boolean | null>(null);
  conventionsCollectivesMaintenues = signal<boolean | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: TransfertEntrepriseCct32bisService,
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
    // SF-219-08b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (!this.dateTransfert()) return false;
    if (this.typeOperation() === null) return false;
    if (this.identiteEconomique() === null) return false;
    if (this.infoConsultPrealableRespectee() === null) return false;
    if (this.conseilEntrepriseConsulte() === null) return false;
    if (this.maintienContratsAutomatique() === null) return false;
    if (this.maintienAncienneteRespecte() === null) return false;
    if (this.maintienRemunerationRespecte() === null) return false;
    if (this.conventionsCollectivesMaintenues() === null) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onDateTransfertChange(value: string | null): void {
    this.dateTransfert.set(value);
  }
  onTypeOperationChange(value: TransfertEntrepriseCct32bisTypeOperation): void {
    this.typeOperation.set(value);
  }
  onIdentiteEconomiqueChange(value: TransfertEntrepriseCct32bisIdentiteEconomique): void {
    this.identiteEconomique.set(value);
  }
  onInfoConsultPrealableRespecteeChange(value: boolean): void {
    this.infoConsultPrealableRespectee.set(value);
  }
  onConseilEntrepriseConsulteChange(value: boolean): void {
    this.conseilEntrepriseConsulte.set(value);
  }
  onMaintienContratsAutomatiqueChange(value: boolean): void {
    this.maintienContratsAutomatique.set(value);
  }
  onMaintienAncienneteRespecteChange(value: boolean): void {
    this.maintienAncienneteRespecte.set(value);
  }
  onMaintienRemunerationRespecteChange(value: boolean): void {
    this.maintienRemunerationRespecte.set(value);
  }
  onConventionsCollectivesMaintenuesChange(value: boolean): void {
    this.conventionsCollectivesMaintenues.set(value);
  }

  // -- Libellés humains --
  typeOperationLabel(
    value: TransfertEntrepriseCct32bisTypeOperation | null,
  ): string {
    switch (value) {
      case 'VENTE_FONDS_COMMERCE':
        return 'Vente du fonds de commerce / universalité';
      case 'FUSION_ABSORPTION':
        return 'Fusion par absorption (CSA)';
      case 'SCISSION':
        return 'Scission totale ou partielle';
      case 'APPORT_UNIVERSALITE':
        return 'Apport d\'une branche d\'activité / universalité';
      case 'DEMEMBREMENT':
        return 'Démembrement / réorganisation conventionnelle';
      case 'FAILLITE':
        return 'Faillite judiciaire';
      case 'LIQUIDATION_JUDICIAIRE':
        return 'Liquidation judiciaire';
      case 'CESSION_ACTIONS':
        return 'Cession d\'actions / parts sociales';
      case 'TRANSFERT_INTRA_GROUPE_SANS_CESSION':
        return 'Transfert intra-groupe sans cession véritable';
      default:
        return '—';
    }
  }

  identiteEconomiqueLabel(
    value: TransfertEntrepriseCct32bisIdentiteEconomique | null,
  ): string {
    switch (value) {
      case 'PRESERVEE':
        return 'Identité économique préservée (continuité d\'activité)';
      case 'NON_PRESERVEE':
        return 'Identité non préservée (simple cession d\'actifs)';
      case 'A_ANALYSER':
        return 'Appréciation ambiguë — à analyser';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'TRANSFERT_CONFORME':
        return 'Transfert conforme CCT 32bis — droits maintenus';
      case 'TRANSFERT_NON_CONFORME_INFO_CONSULT':
        return 'Transfert qualifiant — info-consultation préalable non respectée';
      case 'INELIGIBLE_TYPE_OPERATION':
        return 'Inéligible — opération non qualifiante (faillite / cession actions / intra-groupe)';
      case 'INELIGIBLE_PAS_ENTITE_ECONOMIQUE':
        return 'Inéligible — pas d\'entité économique préservée';
      case 'A_ANALYSER':
        return 'À analyser — qualification d\'identité économique ambiguë';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert conforme, ambre partiel, rouge inéligible, gris à analyser). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'TRANSFERT_CONFORME':
        return 'verdict-ok';
      case 'TRANSFERT_NON_CONFORME_INFO_CONSULT':
        return 'verdict-warn';
      case 'INELIGIBLE_TYPE_OPERATION':
      case 'INELIGIBLE_PAS_ENTITE_ECONOMIQUE':
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
    if (!r) return 'gavel';
    switch (r.verdict) {
      case 'TRANSFERT_CONFORME':
        return 'verified';
      case 'TRANSFERT_NON_CONFORME_INFO_CONSULT':
        return 'warning_amber';
      case 'INELIGIBLE_TYPE_OPERATION':
      case 'INELIGIBLE_PAS_ENTITE_ECONOMIQUE':
        return 'block';
      case 'A_ANALYSER':
        return 'help';
      default:
        return 'gavel';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: TransfertEntrepriseCct32bisVerdict): string {
    switch (verdict) {
      case 'TRANSFERT_CONFORME':
        return 'CONFORME';
      case 'TRANSFERT_NON_CONFORME_INFO_CONSULT':
        return 'INFO-CONSULT KO';
      case 'INELIGIBLE_TYPE_OPERATION':
        return 'TYPE NON QUALIFIANT';
      case 'INELIGIBLE_PAS_ENTITE_ECONOMIQUE':
        return 'PAS D\'ENTITÉ';
      case 'A_ANALYSER':
        return 'À ANALYSER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: TransfertEntrepriseCct32bisVerdict): string {
    switch (verdict) {
      case 'TRANSFERT_CONFORME':
        return 'badge-ok';
      case 'TRANSFERT_NON_CONFORME_INFO_CONSULT':
        return 'badge-warn';
      case 'INELIGIBLE_TYPE_OPERATION':
      case 'INELIGIBLE_PAS_ENTITE_ECONOMIQUE':
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
    const request: TransfertEntrepriseCct32bisRequest = {
      dateTransfert: this.dateTransfert()!,
      typeOperation: this.typeOperation()!,
      identiteEconomique: this.identiteEconomique()!,
      infoConsultPrealableRespectee: this.infoConsultPrealableRespectee()!,
      conseilEntrepriseConsulte: this.conseilEntrepriseConsulte()!,
      maintienContratsAutomatique: this.maintienContratsAutomatique()!,
      maintienAncienneteRespecte: this.maintienAncienneteRespecte()!,
      maintienRemunerationRespecte: this.maintienRemunerationRespecte()!,
      conventionsCollectivesMaintenues: this.conventionsCollectivesMaintenues()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Transfert CCT 32bis analysé', 'OK', { duration: 2500 });
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
        this.dateTransfert.set(r.dateTransfert);
        this.typeOperation.set(r.typeOperation);
        this.identiteEconomique.set(r.identiteEconomique);
        this.infoConsultPrealableRespectee.set(r.infoConsultPrealableRespectee);
        this.conseilEntrepriseConsulte.set(r.conseilEntrepriseConsulte);
        this.maintienContratsAutomatique.set(r.maintienContratsAutomatique);
        this.maintienAncienneteRespecte.set(r.maintienAncienneteRespecte);
        this.maintienRemunerationRespecte.set(r.maintienRemunerationRespecte);
        this.conventionsCollectivesMaintenues.set(r.conventionsCollectivesMaintenues);
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
