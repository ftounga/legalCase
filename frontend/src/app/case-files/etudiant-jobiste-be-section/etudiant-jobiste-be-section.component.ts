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
  EtudiantJobisteBeRequest,
  EtudiantJobisteBeResponse,
  EtudiantJobisteBeStatutEtudiant,
  EtudiantJobisteBeVerdict,
} from '../../core/models/etudiant-jobiste-be.model';
import {
  EtudiantJobisteBeService,
} from '../../core/services/etudiant-jobiste-be.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  EtudiantJobisteBeSectionPrefillRules,
} from './etudiant-jobiste-be-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-13b : outil décisionnel « Étudiant jobiste (Belgique) » (F-219).
 *
 * <p>BE uniquement — <b>Loi du 03/07/1978</b> (Titre VII, art. 120 à
 * 130ter — contrat d'occupation d'étudiants) + <b>Loi-programme du
 * 24/12/2002</b> (cotisations de solidarité réduites) + <b>AR du
 * 14/07/1995</b> (modalités — 5,42 % patronale + 2,71 % personnelle =
 * 8,13 %) + <b>AR du 05/11/2002</b> (Dimona spécifique type STU) +
 * <b>Loi du 18/12/2016</b> (passage 50 jours → 475 heures/an) +
 * <b>Loi-programme du 28/12/2022 + AR du 06/03/2023</b> (relèvement
 * transitoire 600h/an 2023-2024) + <b>Loi-programme du 22/12/2023</b>
 * (M.B. 29/12/2023) pérennisant le quota à 600 heures/an.</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id {@code etudiant-jobiste-be},
 * visibility ALWAYS_ON priority 131 BE / DROIT_DU_TRAVAIL — au-dessus
 * de {@code flexi-job-be} (130, SF-219-12b).</p>
 *
 * <h2>Verdict hiérarchisé 6 états (priorité : statut → vide
 * pédagogique → quota → formalisme → cotisations)</h2>
 * <ul>
 *   <li>{@code ELIGIBLE} (vert) — toutes conditions cumulatives réunies.</li>
 *   <li>{@code FRAGILE_CONTRAT_OU_DIMONA_MANQUANT} (ambre) — formalisme
 *       défaillant, risque requalification ONSS.</li>
 *   <li>{@code FRAGILE_COTISATIONS_NON_REDUITES} (ambre) — barème
 *       AR 14/07/1995 non appliqué, risque redressement ONSS.</li>
 *   <li>{@code INELIGIBLE_STATUT_NON_ETUDIANT} (rouge) — ni étudiant à
 *       titre principal, ni interruption courte ≤ 12 mois.</li>
 *   <li>{@code INELIGIBLE_QUOTA_DEPASSE} (rouge) — heures hors quota
 *       basculent au régime ordinaire (~50 % au lieu de 8,13 %).</li>
 *   <li>{@code A_ANALYSER} (gris info) — vide pédagogique &gt; 12 mois
 *       avec réinscription confirmée, zone grise jurisprudentielle.</li>
 * </ul>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 / F-219 (statut étudiant, compteur Student@work, volume contrat,
 * formalisme contrat / Dimona STU, barème de cotisations non
 * extractibles du pipeline Travail BE actuel).</p>
 */
@Component({
  selector: 'app-etudiant-jobiste-be-section',
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
  templateUrl: './etudiant-jobiste-be-section.component.html',
  styleUrl: './etudiant-jobiste-be-section.component.scss',
})
export class EtudiantJobisteBeSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'etudiant-jobiste-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'ÉTUDIANT JOBISTE (BE)';
  static readonly TOOL_ICON = 'school';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return EtudiantJobisteBeSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR (job étudiant FR sans quota horaire unifié). */
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
  result = signal<EtudiantJobisteBeResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  dateDebutOccupation = signal<string | null>(null);
  statutEtudiant = signal<EtudiantJobisteBeStatutEtudiant | null>(null);
  heuresDejaPresteesDansAnnee = signal<number | null>(null);
  heuresContratEnCours = signal<number | null>(null);
  quotaAnnuelHeures = signal<number | null>(600);
  contratEcritSigne = signal<boolean | null>(null);
  dimonaStuDeclaree = signal<boolean | null>(null);
  cotisationsReduitesAppliquees = signal<boolean | null>(null);
  remunerationBruteHoraire = signal<number | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: EtudiantJobisteBeService,
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
    // SF-219-13b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (!this.dateDebutOccupation()) return false;
    if (this.statutEtudiant() === null) return false;
    if (this.heuresDejaPresteesDansAnnee() === null) return false;
    if ((this.heuresDejaPresteesDansAnnee() as number) < 0) return false;
    if (this.heuresContratEnCours() === null) return false;
    if ((this.heuresContratEnCours() as number) <= 0) return false;
    if (this.quotaAnnuelHeures() === null) return false;
    if ((this.quotaAnnuelHeures() as number) <= 0) return false;
    if (this.contratEcritSigne() === null) return false;
    if (this.dimonaStuDeclaree() === null) return false;
    if (this.cotisationsReduitesAppliquees() === null) return false;
    if (this.remunerationBruteHoraire() === null) return false;
    if ((this.remunerationBruteHoraire() as number) < 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onDateDebutOccupationChange(value: string | null): void {
    this.dateDebutOccupation.set(value);
  }
  onStatutEtudiantChange(value: EtudiantJobisteBeStatutEtudiant): void {
    this.statutEtudiant.set(value);
  }
  onHeuresDejaPresteesDansAnneeChange(value: string | number | null): void {
    this.heuresDejaPresteesDansAnnee.set(this.toNumberOrNull(value));
  }
  onHeuresContratEnCoursChange(value: string | number | null): void {
    this.heuresContratEnCours.set(this.toNumberOrNull(value));
  }
  onQuotaAnnuelHeuresChange(value: string | number | null): void {
    this.quotaAnnuelHeures.set(this.toNumberOrNull(value));
  }
  onContratEcritSigneChange(value: boolean): void {
    this.contratEcritSigne.set(value);
  }
  onDimonaStuDeclareeChange(value: boolean): void {
    this.dimonaStuDeclaree.set(value);
  }
  onCotisationsReduitesAppliqueesChange(value: boolean): void {
    this.cotisationsReduitesAppliquees.set(value);
  }
  onRemunerationBruteHoraireChange(value: string | number | null): void {
    this.remunerationBruteHoraire.set(this.toNumberOrNull(value));
  }

  private toNumberOrNull(value: string | number | null): number | null {
    if (value === null || value === '') return null;
    const n = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(n) ? n : null;
  }

  // -- Libellés humains --
  statutEtudiantLabel(value: EtudiantJobisteBeStatutEtudiant | null): string {
    switch (value) {
      case 'ETUDIANT_PRINCIPAL':
        return 'Étudiant à titre principal (inscription régulière en cours)';
      case 'INTERRUPTION_COURTE_MOINS_12_MOIS':
        return 'Interruption courte ≤ 12 mois entre cycles (statut maintenu)';
      case 'VIDE_PEDAGOGIQUE_PROLONGE':
        return 'Vide pédagogique > 12 mois avec réinscription confirmée (zone grise)';
      case 'NON_ETUDIANT':
        return 'Non étudiant — pas d\'inscription régulière, pas d\'interruption assimilable';
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
        return 'Occupation étudiant pleinement régulière — 4 conditions cumulatives réunies';
      case 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT':
        return 'Formalisme défaillant — contrat écrit ou Dimona STU manquant (risque requalification ONSS)';
      case 'FRAGILE_COTISATIONS_NON_REDUITES':
        return 'Cotisations hors barème AR 14/07/1995 — risque redressement ONSS pour calcul erroné';
      case 'INELIGIBLE_STATUT_NON_ETUDIANT':
        return 'Statut non étudiant — pas d\'inscription régulière, pas d\'interruption assimilable';
      case 'INELIGIBLE_QUOTA_DEPASSE':
        return 'Quota annuel 600h dépassé — heures hors quota basculent au régime ordinaire (~50 %)';
      case 'A_ANALYSER':
        return 'Zone grise — vide pédagogique > 12 mois, analyse juridique requise (jurisprudence partagée)';
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
      case 'FRAGILE_COTISATIONS_NON_REDUITES':
        return 'verdict-warn';
      case 'INELIGIBLE_STATUT_NON_ETUDIANT':
      case 'INELIGIBLE_QUOTA_DEPASSE':
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
      case 'ELIGIBLE':
        return 'verified';
      case 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT':
      case 'FRAGILE_COTISATIONS_NON_REDUITES':
        return 'warning_amber';
      case 'INELIGIBLE_STATUT_NON_ETUDIANT':
      case 'INELIGIBLE_QUOTA_DEPASSE':
        return 'cancel';
      case 'A_ANALYSER':
        return 'help_outline';
      default:
        return 'school';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: EtudiantJobisteBeVerdict): string {
    switch (verdict) {
      case 'ELIGIBLE':
        return 'ÉLIGIBLE';
      case 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT':
        return 'FORMALISME FRAGILE';
      case 'FRAGILE_COTISATIONS_NON_REDUITES':
        return 'COTISATIONS HORS BARÈME';
      case 'INELIGIBLE_STATUT_NON_ETUDIANT':
        return 'INÉLIGIBLE (STATUT)';
      case 'INELIGIBLE_QUOTA_DEPASSE':
        return 'QUOTA DÉPASSÉ';
      case 'A_ANALYSER':
        return 'À ANALYSER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: EtudiantJobisteBeVerdict): string {
    switch (verdict) {
      case 'ELIGIBLE':
        return 'badge-ok';
      case 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT':
      case 'FRAGILE_COTISATIONS_NON_REDUITES':
        return 'badge-warn';
      case 'INELIGIBLE_STATUT_NON_ETUDIANT':
      case 'INELIGIBLE_QUOTA_DEPASSE':
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
    const request: EtudiantJobisteBeRequest = {
      dateDebutOccupation: this.dateDebutOccupation()!,
      statutEtudiant: this.statutEtudiant()!,
      heuresDejaPresteesDansAnnee: this.heuresDejaPresteesDansAnnee()!,
      heuresContratEnCours: this.heuresContratEnCours()!,
      quotaAnnuelHeures: this.quotaAnnuelHeures()!,
      contratEcritSigne: this.contratEcritSigne()!,
      dimonaStuDeclaree: this.dimonaStuDeclaree()!,
      cotisationsReduitesAppliquees: this.cotisationsReduitesAppliquees()!,
      remunerationBruteHoraire: this.remunerationBruteHoraire()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Occupation étudiant analysée', 'OK', { duration: 2500 });
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
        this.dateDebutOccupation.set(r.dateDebutOccupation);
        this.statutEtudiant.set(r.statutEtudiant);
        this.heuresDejaPresteesDansAnnee.set(r.heuresDejaPresteesDansAnnee);
        this.heuresContratEnCours.set(r.heuresContratEnCours);
        this.quotaAnnuelHeures.set(r.quotaAnnuelHeures);
        this.contratEcritSigne.set(r.contratEcritSigne);
        this.dimonaStuDeclaree.set(r.dimonaStuDeclaree);
        this.cotisationsReduitesAppliquees.set(r.cotisationsReduitesAppliquees);
        this.remunerationBruteHoraire.set(r.remunerationBruteHoraire);
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
