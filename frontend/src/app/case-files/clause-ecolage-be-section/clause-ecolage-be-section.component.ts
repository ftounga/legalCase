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
  ClauseEcolageBeMotifDepart,
  ClauseEcolageBeRequest,
  ClauseEcolageBeResponse,
  ClauseEcolageBeTypeFormation,
  ClauseEcolageBeVerdict,
} from '../../core/models/clause-ecolage-be.model';
import {
  ClauseEcolageBeService,
} from '../../core/services/clause-ecolage-be.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  ClauseEcolageBeSectionPrefillRules,
} from './clause-ecolage-be-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-17b : outil décisionnel « clause d'écolage (Belgique) — analyseur
 * de validité » (F-219).
 *
 * <p>BE uniquement — <b>art. 22bis Loi du 03/07/1978</b> sur les contrats
 * de travail, inséré par la <b>Loi du 27/12/2006</b> M.B. 28/12/2006 et
 * modifié par la <b>Loi du 22/12/2017</b> « Pacte de compétitivité ».
 * Conditions cumulatives : (1) forme écrite obligatoire au plus tard à
 * l'entrée en formation (§ 1er al. 1er), (2) formation spécifique
 * (§ 2 al. 1er — non imposée par norme légale ou réglementaire), (3) coût
 * réel &gt; ½ × RMMMG mensuel (§ 2 al. 3 2° — <b>CCT n° 43 du 02/05/1988</b>
 * du CNT), (4) durée d'efficacité ≤ 36 mois (§ 4 al. 1er), (5) dégressivité
 * 100/66/33 % par tiers de la durée d'efficacité (§ 4 al. 2), (6) plafond
 * absolu 80 % du coût réel (§ 4 al. 4), (7) motif de départ actionnable
 * (§ 3 — démission travailleur ou licenciement motif grave imputable au
 * travailleur uniquement). Cadre subsidiaire <b>CCT n° 13 du 02/02/2013</b>
 * du CNT pour les formations qualifiantes sectorielles.</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id {@code clause-ecolage-be},
 * visibility ALWAYS_ON priority 135 BE / DROIT_DU_TRAVAIL — au-dessus de
 * {@code teletravail-be-cct-85-149} (134, SF-219-16b) et
 * {@code interim-be-indemnite-fin-mission} (133, SF-219-15b). Priorité
 * 136 réservée à SF-219-18b (semaine-4-jours-be) en parallèle.</p>
 *
 * <h2>Verdict hiérarchisé 8 états</h2>
 * <ul>
 *   <li>{@code INOPPOSABLE_MOTIF_DEPART} (rouge) — motif art. 22bis § 3
 *       exclu (licenciement sans motif grave employeur / rupture motif grave
 *       employeur / fin CDD) — aucune somme exigible (court-circuite tout).</li>
 *   <li>{@code NULLE_FORME_ECRITE_MANQUANTE} (rouge) — pas d'écrit au plus
 *       tard à l'entrée en formation (§ 1er al. 1er).</li>
 *   <li>{@code NULLE_FORMATION_OBLIGATOIRE} (rouge) — formation imposée par
 *       norme légale ou réglementaire (§ 2 al. 1er).</li>
 *   <li>{@code NULLE_COUT_INSUFFISANT} (rouge) — coût réel ≤ ½ × RMMMG
 *       mensuel (§ 2 al. 3 2°).</li>
 *   <li>{@code NULLE_DUREE_EXCESSIVE} (rouge) — durée d'efficacité &gt; 36
 *       mois (§ 4 al. 1er).</li>
 *   <li>{@code VALIDE_REMBOURSEMENT_DEGRESSIF} (vert) — clause valable, le
 *       remboursement est calculé selon la dégressivité par tiers et
 *       plafonné à 80 % du coût réel.</li>
 *   <li>{@code VALIDE_DUREE_EXPIREE} (vert) — clause valable mais durée
 *       d'efficacité expirée à la date de départ — aucun remboursement.</li>
 *   <li>{@code A_ANALYSER} (gris info) — qualification du type de formation
 *       indéterminée — analyse cas par cas requise.</li>
 * </ul>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 / F-219 (type formation, forme écrite, coût réel, RMMMG mensuel
 * CCT n° 43, durée d'efficacité, dates fin formation / départ,
 * qualification motif art. 22bis § 3 non extractibles du pipeline Travail
 * BE actuel).</p>
 */
@Component({
  selector: 'app-clause-ecolage-be-section',
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
  templateUrl: './clause-ecolage-be-section.component.html',
  styleUrl: './clause-ecolage-be-section.component.scss',
})
export class ClauseEcolageBeSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'clause-ecolage-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'CLAUSE D\'ÉCOLAGE (BE — art. 22bis)';
  static readonly TOOL_ICON = 'school';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return ClauseEcolageBeSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR mécanique (régime jurisprudentiel français distinct). */
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
  result = signal<ClauseEcolageBeResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  typeFormation = signal<ClauseEcolageBeTypeFormation | null>(null);
  clauseEcriteAvantEntreeFormation = signal<boolean | null>(null);
  coutReelFormationEuros = signal<number | null>(null);
  rmmmgMensuelEuros = signal<number | null>(null);
  dureeEfficaciteMois = signal<number | null>(null);
  dateFinFormation = signal<string | null>(null);
  dateDepartTravailleur = signal<string | null>(null);
  motifDepart = signal<ClauseEcolageBeMotifDepart | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: ClauseEcolageBeService,
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
    // SF-219-17b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.typeFormation() === null) return false;
    if (this.clauseEcriteAvantEntreeFormation() === null) return false;
    if (this.coutReelFormationEuros() === null) return false;
    if ((this.coutReelFormationEuros() as number) < 0) return false;
    if (this.rmmmgMensuelEuros() === null) return false;
    if ((this.rmmmgMensuelEuros() as number) <= 0) return false;
    if (this.dureeEfficaciteMois() === null) return false;
    if ((this.dureeEfficaciteMois() as number) < 1) return false;
    if (!this.dateFinFormation()) return false;
    if (!this.dateDepartTravailleur()) return false;
    if (this.motifDepart() === null) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onTypeFormationChange(value: ClauseEcolageBeTypeFormation): void {
    this.typeFormation.set(value);
  }
  onClauseEcriteAvantEntreeFormationChange(value: boolean): void {
    this.clauseEcriteAvantEntreeFormation.set(value);
  }
  onCoutReelFormationEurosChange(value: string | number | null): void {
    this.coutReelFormationEuros.set(this.toNumberOrNull(value));
  }
  onRmmmgMensuelEurosChange(value: string | number | null): void {
    this.rmmmgMensuelEuros.set(this.toNumberOrNull(value));
  }
  onDureeEfficaciteMoisChange(value: string | number | null): void {
    this.dureeEfficaciteMois.set(this.toNumberOrNull(value));
  }
  onDateFinFormationChange(value: string | null): void {
    this.dateFinFormation.set(value);
  }
  onDateDepartTravailleurChange(value: string | null): void {
    this.dateDepartTravailleur.set(value);
  }
  onMotifDepartChange(value: ClauseEcolageBeMotifDepart): void {
    this.motifDepart.set(value);
  }

  private toNumberOrNull(value: string | number | null): number | null {
    if (value === null || value === '') return null;
    const n = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(n) ? n : null;
  }

  // -- Libellés humains --
  typeFormationLabel(value: ClauseEcolageBeTypeFormation | null): string {
    switch (value) {
      case 'SPECIFIQUE':
        return 'Spécifique — art. 22bis § 2 (clause valable possible)';
      case 'OBLIGATOIRE_LEGALE':
        return 'Obligatoire légale — KB/AR/CCT sectorielle (clause nulle)';
      case 'INDETERMINE':
        return 'Indéterminé — qualification à opérer';
      default:
        return '—';
    }
  }

  motifDepartLabel(value: ClauseEcolageBeMotifDepart | null): string {
    switch (value) {
      case 'DEMISSION_TRAVAILLEUR':
        return 'Démission volontaire du travailleur — clause actionnable';
      case 'LICENCIEMENT_EMPLOYEUR_SANS_MOTIF_GRAVE':
        return 'Licenciement sans motif grave (employeur) — clause inopposable';
      case 'LICENCIEMENT_MOTIF_GRAVE_TRAVAILLEUR':
        return 'Licenciement motif grave (imputable travailleur) — clause actionnable';
      case 'RUPTURE_MOTIF_GRAVE_EMPLOYEUR':
        return 'Rupture motif grave (imputable employeur) — clause inopposable';
      case 'FIN_CDD_NORMALE':
        return 'Expiration normale d\'un CDD — clause inopposable';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'INOPPOSABLE_MOTIF_DEPART':
        return 'Clause inopposable — motif de départ exclu (art. 22bis § 3)';
      case 'NULLE_FORME_ECRITE_MANQUANTE':
        return 'Clause nulle — pas d\'écrit à l\'entrée en formation (art. 22bis § 1er al. 1er)';
      case 'NULLE_FORMATION_OBLIGATOIRE':
        return 'Clause nulle — formation imposée par norme légale ou réglementaire (art. 22bis § 2 al. 1er)';
      case 'NULLE_COUT_INSUFFISANT':
        return 'Clause nulle — coût réel ≤ ½ × RMMMG mensuel (art. 22bis § 2 al. 3 2°)';
      case 'NULLE_DUREE_EXCESSIVE':
        return 'Clause nulle — durée d\'efficacité > 36 mois (art. 22bis § 4 al. 1er)';
      case 'VALIDE_REMBOURSEMENT_DEGRESSIF':
        return 'Clause valable — remboursement dégressif (art. 22bis § 4 al. 2), plafonné à 80 % (al. 4)';
      case 'VALIDE_DUREE_EXPIREE':
        return 'Clause valable mais durée d\'efficacité expirée — aucun remboursement exigible';
      case 'A_ANALYSER':
        return 'Type de formation indéterminé — analyse cas par cas requise';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert / rouge / gris info). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'VALIDE_REMBOURSEMENT_DEGRESSIF':
      case 'VALIDE_DUREE_EXPIREE':
        return 'verdict-ok';
      case 'INOPPOSABLE_MOTIF_DEPART':
      case 'NULLE_FORME_ECRITE_MANQUANTE':
      case 'NULLE_FORMATION_OBLIGATOIRE':
      case 'NULLE_COUT_INSUFFISANT':
      case 'NULLE_DUREE_EXCESSIVE':
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
    if (!r) return 'school';
    switch (r.verdict) {
      case 'VALIDE_REMBOURSEMENT_DEGRESSIF':
      case 'VALIDE_DUREE_EXPIREE':
        return 'verified';
      case 'INOPPOSABLE_MOTIF_DEPART':
      case 'NULLE_FORME_ECRITE_MANQUANTE':
      case 'NULLE_FORMATION_OBLIGATOIRE':
      case 'NULLE_COUT_INSUFFISANT':
      case 'NULLE_DUREE_EXCESSIVE':
        return 'cancel';
      case 'A_ANALYSER':
        return 'help_outline';
      default:
        return 'school';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: ClauseEcolageBeVerdict): string {
    switch (verdict) {
      case 'VALIDE_REMBOURSEMENT_DEGRESSIF':
        return 'VALABLE — REMBOURSEMENT DÛ';
      case 'VALIDE_DUREE_EXPIREE':
        return 'VALABLE — DURÉE EXPIRÉE';
      case 'INOPPOSABLE_MOTIF_DEPART':
        return 'INOPPOSABLE';
      case 'NULLE_FORME_ECRITE_MANQUANTE':
        return 'NULLE — ÉCRIT MANQUANT';
      case 'NULLE_FORMATION_OBLIGATOIRE':
        return 'NULLE — FORMATION OBLIGATOIRE';
      case 'NULLE_COUT_INSUFFISANT':
        return 'NULLE — COÛT INSUFFISANT';
      case 'NULLE_DUREE_EXCESSIVE':
        return 'NULLE — DURÉE EXCESSIVE';
      case 'A_ANALYSER':
        return 'À ANALYSER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: ClauseEcolageBeVerdict): string {
    switch (verdict) {
      case 'VALIDE_REMBOURSEMENT_DEGRESSIF':
      case 'VALIDE_DUREE_EXPIREE':
        return 'badge-ok';
      case 'INOPPOSABLE_MOTIF_DEPART':
      case 'NULLE_FORME_ECRITE_MANQUANTE':
      case 'NULLE_FORMATION_OBLIGATOIRE':
      case 'NULLE_COUT_INSUFFISANT':
      case 'NULLE_DUREE_EXCESSIVE':
        return 'badge-critical';
      case 'A_ANALYSER':
        return 'badge-neutral';
      default:
        return '';
    }
  }

  /** Libellé du tiers atteint (1/2/3 ou 0 = au-delà durée d'efficacité). */
  tierDureeLabel(tier: number): string {
    switch (tier) {
      case 1: return '1er tiers (100 % dû)';
      case 2: return '2e tiers (66 % dû)';
      case 3: return '3e tiers (33 % dû)';
      case 0: return 'au-delà de la durée d\'efficacité (0 % dû)';
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
    const request: ClauseEcolageBeRequest = {
      typeFormation: this.typeFormation()!,
      clauseEcriteAvantEntreeFormation: this.clauseEcriteAvantEntreeFormation()!,
      coutReelFormationEuros: this.coutReelFormationEuros()!,
      rmmmgMensuelEuros: this.rmmmgMensuelEuros()!,
      dureeEfficaciteMois: this.dureeEfficaciteMois()!,
      dateFinFormation: this.dateFinFormation()!,
      dateDepartTravailleur: this.dateDepartTravailleur()!,
      motifDepart: this.motifDepart()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Validité clause d\'écolage analysée', 'OK', { duration: 2500 });
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
        this.typeFormation.set(r.typeFormation);
        this.clauseEcriteAvantEntreeFormation.set(r.clauseEcriteAvantEntreeFormation);
        this.coutReelFormationEuros.set(r.coutReelFormationEuros);
        this.rmmmgMensuelEuros.set(r.rmmmgMensuelEuros);
        this.dureeEfficaciteMois.set(r.dureeEfficaciteMois);
        this.dateFinFormation.set(r.dateFinFormation);
        this.dateDepartTravailleur.set(r.dateDepartTravailleur);
        this.motifDepart.set(r.motifDepart);
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
