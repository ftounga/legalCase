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
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import {
  DelegueSyndicalCct5Request,
  DelegueSyndicalCct5Response,
  DelegueSyndicalCct5StatutDesignation,
  DelegueSyndicalCct5Verdict,
} from '../../core/models/delegue-syndical-cct-5.model';
import {
  DelegueSyndicalCct5Service,
} from '../../core/services/delegue-syndical-cct-5.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  DelegueSyndicalCct5SectionPrefillRules,
} from './delegue-syndical-cct-5-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-10b : outil décisionnel « Statut du délégué syndical — CCT n° 5 »
 * (F-219).
 *
 * <p>BE uniquement — <b>CCT n° 5 du 24/05/1971</b> (CNT — statut des
 * délégations syndicales du personnel des entreprises) + <b>CCT n° 5bis
 * et 5ter</b> (extensions) + <b>AR 26/01/1972</b> + <b>CCT
 * sectorielles</b> (seuil de déclenchement par secteur d'activité,
 * durée du mandat, crédit d'heures).</p>
 *
 * <p>Outil <b>qualification + checklist statut</b>, pas calculateur :
 * vérifie l'éligibilité d'un travailleur au statut DS (champ
 * d'application sectoriel + désignation par OS représentative +
 * notification employeur), liste les missions exerçables (art. 3 CCT 5 :
 * négociation CCT d'entreprise, plaintes individuelles et collectives,
 * information personnel ; art. 24 : suppléance des missions CE / CPPT
 * s'ils n'existent pas) et la durée indicative du mandat (4 ans). Le
 * volet « protection contre le licenciement » (Loi 19/03/1991) est
 * couvert par un outil distinct (SF-213-08).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id
 * {@code delegue-syndical-cct-5}, visibility ALWAYS_ON priority 128
 * BE / DROIT_DU_TRAVAIL — au-dessus de {@code elections-sociales-be}
 * (127, SF-219-09b).</p>
 *
 * <p><b>Verdict 5 états</b> :
 * <ul>
 *   <li>{@code STATUT_RECONNU} (vert) — entreprise dans le champ
 *       (seuil sectoriel + présence OS), désignation régulière OS +
 *       notification employeur faite — statut pleinement reconnu.</li>
 *   <li>{@code STATUT_FRAGILE_NOTIFICATION_MANQUANTE} (ambre) —
 *       désignation régulière OS mais notification employeur absente —
 *       à régulariser.</li>
 *   <li>{@code INELIGIBLE_ENTREPRISE_HORS_CHAMP} (rouge) — entreprise
 *       sous seuil sectoriel ou sans présence OS — CCT n° 5 non
 *       applicable.</li>
 *   <li>{@code INELIGIBLE_PAS_DESIGNE_PAR_OS} (rouge) — pas de
 *       désignation par OS représentative — pas de statut CCT 5.</li>
 *   <li>{@code A_ANALYSER} (info) — désignation contestée ou ambiguïté
 *       sectorielle — analyse juridique requise.</li>
 * </ul></p>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213/F-219 (qualifications syndicales, paramétrage sectoriel,
 * institutionnel CE/CPPT non extractibles depuis la pipeline Travail BE
 * actuelle ; saisie avocat depuis procès-verbal de désignation OS,
 * courrier de notification employeur, procès-verbal d'élection CE/CPPT,
 * ROI).</p>
 */
@Component({
  selector: 'app-delegue-syndical-cct-5-section',
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
  templateUrl: './delegue-syndical-cct-5-section.component.html',
  styleUrl: './delegue-syndical-cct-5-section.component.scss',
})
export class DelegueSyndicalCct5SectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'delegue-syndical-cct-5';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'DÉLÉGUÉ SYNDICAL (BE / CCT 5)';
  static readonly TOOL_ICON = 'how_to_reg';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return DelegueSyndicalCct5SectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR strict (C. trav. L. 2143-1 et s. distinct). */
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
  result = signal<DelegueSyndicalCct5Response | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  dateDesignation = signal<string | null>(null);
  effectifEntreprise = signal<number | null>(null);
  /** Défaut métier 50 (CCT sectorielle médiane — paramétrable). */
  seuilSectorielRequis = signal<number>(50);
  presenceOsRepresentative = signal<boolean>(false);
  statutDesignation = signal<DelegueSyndicalCct5StatutDesignation | null>(null);
  ceExistant = signal<boolean>(false);
  cpptExistant = signal<boolean>(false);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: DelegueSyndicalCct5Service,
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
    // SF-219-10b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (!this.dateDesignation()) return false;
    if (this.effectifEntreprise() === null) return false;
    if (this.seuilSectorielRequis() === null || this.seuilSectorielRequis() < 1) return false;
    if (this.statutDesignation() === null) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onDateDesignationChange(value: string | null): void {
    this.dateDesignation.set(value);
  }
  onEffectifEntrepriseChange(value: number | null): void {
    this.effectifEntreprise.set(value);
  }
  onSeuilSectorielRequisChange(value: number | null): void {
    this.seuilSectorielRequis.set(value ?? 50);
  }
  onPresenceOsRepresentativeChange(value: boolean): void {
    this.presenceOsRepresentative.set(value);
  }
  onStatutDesignationChange(value: DelegueSyndicalCct5StatutDesignation): void {
    this.statutDesignation.set(value);
  }
  onCeExistantChange(value: boolean): void {
    this.ceExistant.set(value);
  }
  onCpptExistantChange(value: boolean): void {
    this.cpptExistant.set(value);
  }

  // -- Libellés humains --
  statutDesignationLabel(
    value: DelegueSyndicalCct5StatutDesignation | null,
  ): string {
    switch (value) {
      case 'DESIGNE_PAR_OS_REPRESENTATIVE':
        return 'Désigné par OS représentative + notification employeur';
      case 'NOTIFICATION_EMPLOYEUR_MANQUANTE':
        return 'Désigné par OS mais notification employeur manquante';
      case 'PAS_DESIGNE_PAR_OS':
        return 'Pas de désignation par OS (auto-proclamation / élection interne)';
      case 'DESIGNATION_CONTESTEE':
        return 'Désignation contestée (conflit inter-syndical / représentativité)';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'STATUT_RECONNU':
        return 'Statut DS pleinement reconnu — missions CCT 5 exerçables';
      case 'STATUT_FRAGILE_NOTIFICATION_MANQUANTE':
        return 'Statut fragile — notification employeur manquante à régulariser';
      case 'INELIGIBLE_ENTREPRISE_HORS_CHAMP':
        return 'Inéligible — entreprise hors champ sectoriel CCT 5';
      case 'INELIGIBLE_PAS_DESIGNE_PAR_OS':
        return 'Inéligible — pas de désignation par OS représentative';
      case 'A_ANALYSER':
        return 'À analyser — désignation contestée ou ambiguïté sectorielle';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert ok, ambre warn, rouge critical, gris info). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'STATUT_RECONNU':
        return 'verdict-ok';
      case 'STATUT_FRAGILE_NOTIFICATION_MANQUANTE':
        return 'verdict-warn';
      case 'INELIGIBLE_ENTREPRISE_HORS_CHAMP':
      case 'INELIGIBLE_PAS_DESIGNE_PAR_OS':
        return 'verdict-critical';
      case 'A_ANALYSER':
        return 'verdict-info';
      default:
        return '';
    }
  }

  /** Icône Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'gavel';
    switch (r.verdict) {
      case 'STATUT_RECONNU':
        return 'verified';
      case 'STATUT_FRAGILE_NOTIFICATION_MANQUANTE':
        return 'warning';
      case 'INELIGIBLE_ENTREPRISE_HORS_CHAMP':
      case 'INELIGIBLE_PAS_DESIGNE_PAR_OS':
        return 'block';
      case 'A_ANALYSER':
        return 'help';
      default:
        return 'gavel';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: DelegueSyndicalCct5Verdict): string {
    switch (verdict) {
      case 'STATUT_RECONNU':
        return 'STATUT RECONNU';
      case 'STATUT_FRAGILE_NOTIFICATION_MANQUANTE':
        return 'STATUT FRAGILE';
      case 'INELIGIBLE_ENTREPRISE_HORS_CHAMP':
        return 'HORS CHAMP';
      case 'INELIGIBLE_PAS_DESIGNE_PAR_OS':
        return 'PAS DÉSIGNÉ OS';
      case 'A_ANALYSER':
        return 'À ANALYSER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: DelegueSyndicalCct5Verdict): string {
    switch (verdict) {
      case 'STATUT_RECONNU':
        return 'badge-ok';
      case 'STATUT_FRAGILE_NOTIFICATION_MANQUANTE':
        return 'badge-warn';
      case 'INELIGIBLE_ENTREPRISE_HORS_CHAMP':
      case 'INELIGIBLE_PAS_DESIGNE_PAR_OS':
        return 'badge-critical';
      case 'A_ANALYSER':
        return 'badge-info';
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
    const request: DelegueSyndicalCct5Request = {
      dateDesignation: this.dateDesignation()!,
      effectifEntreprise: this.effectifEntreprise()!,
      seuilSectorielRequis: this.seuilSectorielRequis(),
      presenceOsRepresentative: this.presenceOsRepresentative(),
      statutDesignation: this.statutDesignation()!,
      ceExistant: this.ceExistant(),
      cpptExistant: this.cpptExistant(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Statut délégué syndical analysé', 'OK', { duration: 2500 });
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
        this.dateDesignation.set(r.dateDesignation);
        this.effectifEntreprise.set(r.effectifEntreprise);
        this.seuilSectorielRequis.set(r.seuilSectorielRequis);
        this.presenceOsRepresentative.set(r.presenceOsRepresentative);
        this.statutDesignation.set(r.statutDesignation);
        this.ceExistant.set(r.ceExistant);
        this.cpptExistant.set(r.cpptExistant);
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
