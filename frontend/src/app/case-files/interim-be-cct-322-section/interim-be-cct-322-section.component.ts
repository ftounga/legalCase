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
  InterimBeCct322MotifMission,
  InterimBeCct322Request,
  InterimBeCct322Response,
  InterimBeCct322Verdict,
} from '../../core/models/interim-be-cct-322.model';
import {
  InterimBeCct322Service,
} from '../../core/services/interim-be-cct-322.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  InterimBeCct322SectionPrefillRules,
} from './interim-be-cct-322-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-14b : outil décisionnel « statut intérim BE — CCT n° 322 » (F-219).
 *
 * <p>BE uniquement — <b>Loi du 24/07/1987</b> relative au travail temporaire,
 * au travail intérimaire et à la mise de travailleurs à la disposition
 * d'utilisateurs (M.B. 20/08/1987) + <b>CCT n° 322 du 14/06/2010</b> conclue
 * au CNT (responsabilité solidaire ETI / utilisateur, parité salariale
 * stricte) + <b>CCT n° 108 du 16/07/2013</b> (insertion en vue d'embauche
 * durable, max 3 tentatives / 9 mois cumulés) + <b>AR du 11/10/1976</b>
 * (liste limitative cas de travail exceptionnel) + <b>Loi-programme du
 * 27/12/2012</b> (sanctions ONSS renforcées en cas de fraude).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id {@code interim-be-cct-322},
 * visibility ALWAYS_ON priority 132 BE / DROIT_DU_TRAVAIL — au-dessus de
 * {@code etudiant-jobiste-be} (131, SF-219-13b parallèle), {@code flexi-job-be}
 * (130, SF-219-12b) et {@code conge-education-paye-region} (129, SF-219-11b).</p>
 *
 * <h2>Verdict hiérarchisé 7 états (priorité : grève/lock-out → motif non
 * autorisé → zone grise → durée → parité → formalisme → éligible)</h2>
 * <ul>
 *   <li>{@code ELIGIBLE_MISSION_REGULIERE} (vert) — toutes conditions
 *       cumulatives réunies.</li>
 *   <li>{@code FRAGILE_CONTRAT_OU_DIMONA_MANQUANT} (ambre) — fond OK mais
 *       formalisme défaillant (requalification automatique).</li>
 *   <li>{@code INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT} (rouge) — art. 19,
 *       interdiction absolue, présomption irréfragable de fraude.</li>
 *   <li>{@code INELIGIBLE_MOTIF_NON_AUTORISE} (rouge) — motif hors liste
 *       limitative art. 1er § 1 / 1bis (requalification CDI utilisateur).</li>
 *   <li>{@code INELIGIBLE_DUREE_MAX_DEPASSEE} (rouge) — durée totale au-delà
 *       du plafond légal applicable au motif.</li>
 *   <li>{@code INELIGIBLE_PARITE_SALARIALE_VIOLEE} (rouge) — salaire
 *       intérimaire sous celui du permanent de référence (art. 10).</li>
 *   <li>{@code A_ANALYSER} (gris info) — motif artistique / flux exceptionnel,
 *       analyse cas par cas requise.</li>
 * </ul>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213 / F-219 (date mission ETI, motif limitatif, durée cumulée, parité
 * salariale utilisateur, formalisme Dimona ETI, paramètres légaux du
 * plafond non extractibles du pipeline Travail BE actuel).</p>
 */
@Component({
  selector: 'app-interim-be-cct-322-section',
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
  templateUrl: './interim-be-cct-322-section.component.html',
  styleUrl: './interim-be-cct-322-section.component.scss',
})
export class InterimBeCct322SectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'interim-be-cct-322';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'INTÉRIM (BE — CCT 322)';
  static readonly TOOL_ICON = 'engineering';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return InterimBeCct322SectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR mécanique (régime intérimaire FR distinct). */
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
  result = signal<InterimBeCct322Response | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  dateDebutMission = signal<string | null>(null);
  motifMission = signal<InterimBeCct322MotifMission | null>(null);
  remplacementGreveOuLockout = signal<boolean | null>(null);
  dureeTotaleMissionJours = signal<number | null>(null);
  dureeMaxLegaleJours = signal<number | null>(null);
  salaireHoraireIntimaireBrut = signal<number | null>(null);
  salaireHoraireReferenceBrut = signal<number | null>(null);
  contratEcritSigne = signal<boolean | null>(null);
  dimonaDeclareeParEti = signal<boolean | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: InterimBeCct322Service,
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
    // SF-219-14b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (!this.dateDebutMission()) return false;
    if (this.motifMission() === null) return false;
    if (this.remplacementGreveOuLockout() === null) return false;
    if (this.dureeTotaleMissionJours() === null) return false;
    if ((this.dureeTotaleMissionJours() as number) < 0) return false;
    if (this.dureeMaxLegaleJours() === null) return false;
    if ((this.dureeMaxLegaleJours() as number) <= 0) return false;
    if (this.salaireHoraireIntimaireBrut() === null) return false;
    if ((this.salaireHoraireIntimaireBrut() as number) < 0) return false;
    if (this.salaireHoraireReferenceBrut() === null) return false;
    if ((this.salaireHoraireReferenceBrut() as number) <= 0) return false;
    if (this.contratEcritSigne() === null) return false;
    if (this.dimonaDeclareeParEti() === null) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onDateDebutMissionChange(value: string | null): void {
    this.dateDebutMission.set(value);
  }
  onMotifMissionChange(value: InterimBeCct322MotifMission): void {
    this.motifMission.set(value);
  }
  onRemplacementGreveOuLockoutChange(value: boolean): void {
    this.remplacementGreveOuLockout.set(value);
  }
  onDureeTotaleMissionJoursChange(value: string | number | null): void {
    this.dureeTotaleMissionJours.set(this.toNumberOrNull(value));
  }
  onDureeMaxLegaleJoursChange(value: string | number | null): void {
    this.dureeMaxLegaleJours.set(this.toNumberOrNull(value));
  }
  onSalaireHoraireIntimaireBrutChange(value: string | number | null): void {
    this.salaireHoraireIntimaireBrut.set(this.toNumberOrNull(value));
  }
  onSalaireHoraireReferenceBrutChange(value: string | number | null): void {
    this.salaireHoraireReferenceBrut.set(this.toNumberOrNull(value));
  }
  onContratEcritSigneChange(value: boolean): void {
    this.contratEcritSigne.set(value);
  }
  onDimonaDeclareeParEtiChange(value: boolean): void {
    this.dimonaDeclareeParEti.set(value);
  }

  private toNumberOrNull(value: string | number | null): number | null {
    if (value === null || value === '') return null;
    const n = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(n) ? n : null;
  }

  // -- Libellés humains --
  motifMissionLabel(value: InterimBeCct322MotifMission | null): string {
    switch (value) {
      case 'REMPLACEMENT_TRAVAILLEUR_PERMANENT':
        return 'Remplacement d\'un travailleur permanent (art. 1er § 1, 1°)';
      case 'SURCROIT_TEMPORAIRE_DE_TRAVAIL':
        return 'Surcroît temporaire de travail (art. 1er § 1, 2°)';
      case 'EXECUTION_TRAVAIL_EXCEPTIONNEL':
        return 'Travail exceptionnel — AR 11/10/1976 (art. 1er § 1, 3°)';
      case 'INSERTION_EMBAUCHE_DURABLE':
        return 'Insertion en vue d\'embauche durable (art. 1bis + CCT 108)';
      case 'MOTIF_ARTISTIQUE':
        return 'Motif artistique (art. 1bis § 4)';
      case 'FLUX_TRAVAUX_EXCEPTIONNEL_TRAJET_SCOLAIRE':
        return 'Flux exceptionnels / trajet scolaire (AR sectoriels)';
      case 'AUTRE_NON_AUTORISE':
        return 'Autre motif (non listé par la loi) — recours illégal';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'ELIGIBLE_MISSION_REGULIERE':
        return 'Mission intérimaire régulière — toutes conditions cumulatives réunies';
      case 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT':
        return 'Formalisme défaillant — contrat écrit ou Dimona ETI manquant (requalification automatique)';
      case 'INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT':
        return 'Recours interdit — remplacement grève / lock-out (art. 19, présomption irréfragable de fraude)';
      case 'INELIGIBLE_MOTIF_NON_AUTORISE':
        return 'Motif non autorisé — hors liste limitative art. 1er § 1 / 1bis (requalification CDI utilisateur)';
      case 'INELIGIBLE_DUREE_MAX_DEPASSEE':
        return 'Durée maximale dépassée — la mission excède le plafond légal applicable au motif';
      case 'INELIGIBLE_PARITE_SALARIALE_VIOLEE':
        return 'Parité salariale violée — salaire intérimaire sous celui du permanent de référence (art. 10)';
      case 'A_ANALYSER':
        return 'Zone grise — motif artistique ou flux exceptionnel, analyse cas par cas requise';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert / ambre / rouge / gris info). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'ELIGIBLE_MISSION_REGULIERE':
        return 'verdict-ok';
      case 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT':
        return 'verdict-warn';
      case 'INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT':
      case 'INELIGIBLE_MOTIF_NON_AUTORISE':
      case 'INELIGIBLE_DUREE_MAX_DEPASSEE':
      case 'INELIGIBLE_PARITE_SALARIALE_VIOLEE':
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
    if (!r) return 'engineering';
    switch (r.verdict) {
      case 'ELIGIBLE_MISSION_REGULIERE':
        return 'verified';
      case 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT':
        return 'warning_amber';
      case 'INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT':
      case 'INELIGIBLE_MOTIF_NON_AUTORISE':
      case 'INELIGIBLE_DUREE_MAX_DEPASSEE':
      case 'INELIGIBLE_PARITE_SALARIALE_VIOLEE':
        return 'cancel';
      case 'A_ANALYSER':
        return 'help_outline';
      default:
        return 'engineering';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: InterimBeCct322Verdict): string {
    switch (verdict) {
      case 'ELIGIBLE_MISSION_REGULIERE':
        return 'ÉLIGIBLE';
      case 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT':
        return 'FORMALISME FRAGILE';
      case 'INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT':
        return 'GRÈVE / LOCK-OUT';
      case 'INELIGIBLE_MOTIF_NON_AUTORISE':
        return 'MOTIF INTERDIT';
      case 'INELIGIBLE_DUREE_MAX_DEPASSEE':
        return 'DURÉE DÉPASSÉE';
      case 'INELIGIBLE_PARITE_SALARIALE_VIOLEE':
        return 'PARITÉ VIOLÉE';
      case 'A_ANALYSER':
        return 'À ANALYSER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: InterimBeCct322Verdict): string {
    switch (verdict) {
      case 'ELIGIBLE_MISSION_REGULIERE':
        return 'badge-ok';
      case 'FRAGILE_CONTRAT_OU_DIMONA_MANQUANT':
        return 'badge-warn';
      case 'INELIGIBLE_MOTIF_INTERDIT_GREVE_LOCKOUT':
      case 'INELIGIBLE_MOTIF_NON_AUTORISE':
      case 'INELIGIBLE_DUREE_MAX_DEPASSEE':
      case 'INELIGIBLE_PARITE_SALARIALE_VIOLEE':
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
    const request: InterimBeCct322Request = {
      dateDebutMission: this.dateDebutMission()!,
      motifMission: this.motifMission()!,
      remplacementGreveOuLockout: this.remplacementGreveOuLockout()!,
      dureeTotaleMissionJours: this.dureeTotaleMissionJours()!,
      dureeMaxLegaleJours: this.dureeMaxLegaleJours()!,
      salaireHoraireIntimaireBrut: this.salaireHoraireIntimaireBrut()!,
      salaireHoraireReferenceBrut: this.salaireHoraireReferenceBrut()!,
      contratEcritSigne: this.contratEcritSigne()!,
      dimonaDeclareeParEti: this.dimonaDeclareeParEti()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Statut intérim analysé', 'OK', { duration: 2500 });
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
        this.dateDebutMission.set(r.dateDebutMission);
        this.motifMission.set(r.motifMission);
        this.remplacementGreveOuLockout.set(r.remplacementGreveOuLockout);
        this.dureeTotaleMissionJours.set(r.dureeTotaleMissionJours);
        this.dureeMaxLegaleJours.set(r.dureeMaxLegaleJours);
        this.salaireHoraireIntimaireBrut.set(r.salaireHoraireIntimaireBrut);
        this.salaireHoraireReferenceBrut.set(r.salaireHoraireReferenceBrut);
        this.contratEcritSigne.set(r.contratEcritSigne);
        this.dimonaDeclareeParEti.set(r.dimonaDeclareeParEti);
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
