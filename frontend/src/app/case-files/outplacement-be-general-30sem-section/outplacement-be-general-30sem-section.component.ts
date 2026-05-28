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
import { MatSnackBar } from '@angular/material/snack-bar';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import {
  PieceManquanteEntry,
  TravailExtractedData,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import {
  OutplacementBeGeneral30semRequest,
  OutplacementBeGeneral30semResponse,
  OutplacementBeGeneral30semVerdict,
} from '../../core/models/outplacement-be-general-30sem.model';
import {
  OutplacementBeGeneral30semService,
} from '../../core/services/outplacement-be-general-30sem.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  OutplacementBeGeneral30semSectionPrefillRules,
} from './outplacement-be-general-30sem-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-05b : outil décisionnel « Outplacement BE général — régime
 * préavis ≥ 30 semaines » (F-219).
 *
 * <p>BE uniquement — <b>Loi du 05/09/2001 art. 11</b> (régime
 * d'outplacement général lié à la durée du préavis) + <b>AR du
 * 21/10/2007</b> (programme minimum 60h sur 12 mois). Aucun équivalent
 * strict en droit français.</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id {@code outplacement-be-general-30sem},
 * visibility ALWAYS_ON priority 123 BE / DROIT_DU_TRAVAIL — au-dessus de
 * {@code cumul-rcc-allocations} (122). Pattern miroir des autres outils
 * outplacement / RCC BE de la F-219.</p>
 *
 * <p><b>Verdict 7 états hiérarchiques</b> (la première condition non
 * remplie l'emporte) :
 * <ul>
 *   <li>{@code CONFORME} (vert) — offre d'outplacement régulière.</li>
 *   <li>{@code NON_DU_DEMISSION} (gris info) — pas de licenciement.</li>
 *   <li>{@code NON_DU_MOTIF_GRAVE} (gris info) — exclu du régime.</li>
 *   <li>{@code NON_DU_PREAVIS_INSUFFISANT} (gris info) — &lt; 30 semaines.</li>
 *   <li>{@code NON_CONFORME_OFFRE_TARDIVE} (rouge) — offre &gt; 15 jours.</li>
 *   <li>{@code NON_CONFORME_DUREE_INSUFFISANTE} (rouge) — &lt; 60h ou
 *       &gt; 12 mois.</li>
 *   <li>{@code NON_CONFORME_FORME} (rouge) — non écrite / non individuelle
 *       / contenu minimum absent.</li>
 * </ul></p>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern uniforme
 * F-213/F-219. Qualifications juridiques (licenciement effectif, motif
 * grave, durée préavis, fenêtre 15 jours, forme/contenu) relèvent de
 * l'avocat sur la lettre de rupture, le C4 et la lettre d'offre
 * outplacement (prestataire externe).</p>
 *
 * <p>Distinct de {@code outplacement-be-obligatoire-45} (F-207 SF-207-08 —
 * régime 45+ ans, applicable quelle que soit la durée du préavis). Les
 * deux régimes peuvent coexister sur un même dossier.</p>
 */
@Component({
  selector: 'app-outplacement-be-general-30sem-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSelectModule,
    MatListModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './outplacement-be-general-30sem-section.component.html',
  styleUrl: './outplacement-be-general-30sem-section.component.scss',
})
export class OutplacementBeGeneral30semSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'outplacement-be-general-30sem';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'OUTPLACEMENT BE GÉNÉRAL 30 SEM';
  static readonly TOOL_ICON = 'work_history';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return OutplacementBeGeneral30semSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR. */
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
  result = signal<OutplacementBeGeneral30semResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  licenciementEffectif = signal<boolean | null>(null);
  motifGrave = signal<boolean | null>(null);
  semainesPreavisOuIndemnite = signal<number | null>(null);
  dateFinContrat = signal<string | null>(null);
  offreNotifiee = signal<boolean | null>(null);
  dateNotificationOffre = signal<string | null>(null);
  heuresProgramme = signal<number | null>(null);
  dureeProgrammeMois = signal<number | null>(null);
  offreEcrite = signal<boolean | null>(null);
  offreIndividuelle = signal<boolean | null>(null);
  contenuMinimumRespecte = signal<boolean | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: OutplacementBeGeneral30semService,
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
    // SF-219-05b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.licenciementEffectif() === null) return false;
    if (this.motifGrave() === null) return false;
    if (this.semainesPreavisOuIndemnite() === null) return false;
    if (!this.dateFinContrat()) return false;
    if (this.offreNotifiee() === null) return false;
    if (this.heuresProgramme() === null) return false;
    if (this.dureeProgrammeMois() === null) return false;
    if (this.offreEcrite() === null) return false;
    if (this.offreIndividuelle() === null) return false;
    if (this.contenuMinimumRespecte() === null) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onLicenciementEffectifChange(value: boolean): void {
    this.licenciementEffectif.set(value);
  }
  onMotifGraveChange(value: boolean): void {
    this.motifGrave.set(value);
  }
  onSemainesPreavisOuIndemniteChange(value: number | null): void {
    this.semainesPreavisOuIndemnite.set(value);
  }
  onDateFinContratChange(value: string | null): void {
    this.dateFinContrat.set(value);
  }
  onOffreNotifieeChange(value: boolean): void {
    this.offreNotifiee.set(value);
    if (!value) {
      this.dateNotificationOffre.set(null);
    }
  }
  onDateNotificationOffreChange(value: string | null): void {
    this.dateNotificationOffre.set(value);
  }
  onHeuresProgrammeChange(value: number | null): void {
    this.heuresProgramme.set(value);
  }
  onDureeProgrammeMoisChange(value: number | null): void {
    this.dureeProgrammeMois.set(value);
  }
  onOffreEcriteChange(value: boolean): void {
    this.offreEcrite.set(value);
  }
  onOffreIndividuelleChange(value: boolean): void {
    this.offreIndividuelle.set(value);
  }
  onContenuMinimumRespecteChange(value: boolean): void {
    this.contenuMinimumRespecte.set(value);
  }

  // -- Libellés humains du verdict --
  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'CONFORME':
        return 'Offre d\'outplacement conforme';
      case 'NON_DU_DEMISSION':
        return 'Outplacement non dû — démission';
      case 'NON_DU_MOTIF_GRAVE':
        return 'Outplacement non dû — motif grave';
      case 'NON_DU_PREAVIS_INSUFFISANT':
        return 'Outplacement non dû — préavis < 30 semaines';
      case 'NON_CONFORME_OFFRE_TARDIVE':
        return 'Offre tardive ou absente (> 15 jours)';
      case 'NON_CONFORME_DUREE_INSUFFISANTE':
        return 'Durée du programme insuffisante (< 60h ou > 12 mois)';
      case 'NON_CONFORME_FORME':
        return 'Forme de l\'offre non conforme';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert conforme, rouge anomalie, gris non-dû). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'CONFORME':
        return 'verdict-ok';
      case 'NON_CONFORME_OFFRE_TARDIVE':
      case 'NON_CONFORME_DUREE_INSUFFISANTE':
      case 'NON_CONFORME_FORME':
        return 'verdict-critical';
      case 'NON_DU_DEMISSION':
      case 'NON_DU_MOTIF_GRAVE':
      case 'NON_DU_PREAVIS_INSUFFISANT':
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
      case 'CONFORME':
        return 'verified';
      case 'NON_DU_DEMISSION':
        return 'logout';
      case 'NON_DU_MOTIF_GRAVE':
        return 'gpp_bad';
      case 'NON_DU_PREAVIS_INSUFFISANT':
        return 'timer_off';
      case 'NON_CONFORME_OFFRE_TARDIVE':
        return 'schedule';
      case 'NON_CONFORME_DUREE_INSUFFISANTE':
        return 'trending_down';
      case 'NON_CONFORME_FORME':
        return 'rule';
      default:
        return 'gavel';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: OutplacementBeGeneral30semVerdict): string {
    switch (verdict) {
      case 'CONFORME':
        return 'CONFORME';
      case 'NON_DU_DEMISSION':
        return 'NON DÛ — DÉMISSION';
      case 'NON_DU_MOTIF_GRAVE':
        return 'NON DÛ — MOTIF GRAVE';
      case 'NON_DU_PREAVIS_INSUFFISANT':
        return 'NON DÛ — PRÉAVIS < 30 SEM';
      case 'NON_CONFORME_OFFRE_TARDIVE':
        return 'OFFRE TARDIVE';
      case 'NON_CONFORME_DUREE_INSUFFISANTE':
        return 'DURÉE INSUFFISANTE';
      case 'NON_CONFORME_FORME':
        return 'FORME NON CONFORME';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: OutplacementBeGeneral30semVerdict): string {
    switch (verdict) {
      case 'CONFORME':
        return 'badge-ok';
      case 'NON_CONFORME_OFFRE_TARDIVE':
      case 'NON_CONFORME_DUREE_INSUFFISANTE':
      case 'NON_CONFORME_FORME':
        return 'badge-critical';
      case 'NON_DU_DEMISSION':
      case 'NON_DU_MOTIF_GRAVE':
      case 'NON_DU_PREAVIS_INSUFFISANT':
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
    const request: OutplacementBeGeneral30semRequest = {
      licenciementEffectif: this.licenciementEffectif()!,
      motifGrave: this.motifGrave()!,
      semainesPreavisOuIndemnite: this.semainesPreavisOuIndemnite()!,
      dateFinContrat: this.dateFinContrat()!,
      offreNotifiee: this.offreNotifiee()!,
      dateNotificationOffre: this.dateNotificationOffre(),
      heuresProgramme: this.heuresProgramme()!,
      dureeProgrammeMois: this.dureeProgrammeMois()!,
      offreEcrite: this.offreEcrite()!,
      offreIndividuelle: this.offreIndividuelle()!,
      contenuMinimumRespecte: this.contenuMinimumRespecte()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Outplacement BE analysé', 'OK', { duration: 2500 });
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
        this.licenciementEffectif.set(r.licenciementEffectif);
        this.motifGrave.set(r.motifGrave);
        this.semainesPreavisOuIndemnite.set(r.semainesPreavisOuIndemnite);
        this.dateFinContrat.set(r.dateFinContrat);
        this.offreNotifiee.set(r.offreNotifiee);
        this.dateNotificationOffre.set(r.dateNotificationOffre);
        this.heuresProgramme.set(r.heuresProgramme);
        this.dureeProgrammeMois.set(r.dureeProgrammeMois);
        this.offreEcrite.set(r.offreEcrite);
        this.offreIndividuelle.set(r.offreIndividuelle);
        this.contenuMinimumRespecte.set(r.contenuMinimumRespecte);
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
