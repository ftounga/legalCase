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
  InterimBeCommissionParitaire,
  InterimBeIndemniteFinMissionRequest,
  InterimBeIndemniteFinMissionResponse,
  InterimBeIndemniteFinMissionVerdict,
} from '../../core/models/interim-be-indemnite-fin-mission.model';
import {
  InterimBeIndemniteFinMissionService,
} from '../../core/services/interim-be-indemnite-fin-mission.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  InterimBeIndemniteFinMissionSectionPrefillRules,
} from './interim-be-indemnite-fin-mission-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-15b : outil décisionnel « indemnité de fin de mission
 * intérim BE » (F-219).
 *
 * <p>BE uniquement — <b>Loi du 24/07/1987</b> relative au travail
 * temporaire (M.B. 20/08/1987) + <b>CCT n° 322 du 14/06/2010</b>
 * (statut intérim) + <b>CCT n° 322bis du 16/12/2010</b> (pécule de
 * vacances intérim 15,38 % payé par le FSI) + <b>AR du 30/03/1967</b>
 * art. 19 (vacances annuelles) + <b>CCT sectorielles</b> propres à
 * la CP de l'utilisateur (prime fin d'année 1/12 brut annuel —
 * variable selon CP) + <b>jurisprudence Cass. BE 04/02/1991 et
 * 16/12/2002</b> (rupture anticipée injustifiée = rémunérations
 * restant à courir) + <b>Cass. BE 03/05/2010 et 23/06/2003</b>
 * (refus prime de précarité 10 % style FR) + <b>Loi 16/03/1971</b>
 * art. 29 (sursalaire heures supplémentaires).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id
 * {@code interim-be-indemnite-fin-mission}, visibility ALWAYS_ON
 * priority 133 BE / DROIT_DU_TRAVAIL — au-dessus de
 * {@code interim-be-cct-322} (132, SF-219-14b),
 * {@code etudiant-jobiste-be} (131, SF-219-13b),
 * {@code flexi-job-be} (130, SF-219-12b).</p>
 *
 * <h2>Verdict hiérarchisé 4 états</h2>
 * <ul>
 *   <li>{@code RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR} (rouge) —
 *       indemnité spécifique substituée aux composantes ordinaires.</li>
 *   <li>{@code A_ANALYSER_SECTEUR_NON_RECONNU} (gris info) — CP
 *       utilisateur non recensée, renvoi avocat CCT sectorielle.</li>
 *   <li>{@code INDEMNITES_DUES} (vert) — au moins une composante
 *       (pécule / prime fin d'année / heures sup).</li>
 *   <li>{@code AUCUNE_INDEMNITE_DUE} (gris info) — fin au terme,
 *       pécule FSI déjà payé, pas de prime sectorielle, pas d'heures
 *       sup. <b>Pas de prime de précarité 10 % style FR en BE.</b></li>
 * </ul>
 *
 * <p><b>Avertissement structurel</b> : la prime de précarité
 * forfaitaire 10 % de l'art. L. 1251-32 C. trav. fr. n'a aucun
 * équivalent en droit belge — l'outil affiche systématiquement
 * un avertissement pour prévenir toute transposition mécanique
 * du droit FR (cf. Cass. 03/05/2010, S.09.0086.F).</p>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern
 * uniforme F-213 / F-219 (dates mission ETI, durées, salaire
 * horaire, relevés d'heures, flag pécule FSI, qualification rupture,
 * CP utilisateur, ancienneté sectorielle non extractibles du
 * pipeline Travail BE actuel).</p>
 */
@Component({
  selector: 'app-interim-be-indemnite-fin-mission-section',
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
  templateUrl: './interim-be-indemnite-fin-mission-section.component.html',
  styleUrl: './interim-be-indemnite-fin-mission-section.component.scss',
})
export class InterimBeIndemniteFinMissionSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'interim-be-indemnite-fin-mission';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'INDEMNITÉ FIN MISSION INTÉRIM (BE)';
  static readonly TOOL_ICON = 'request_quote';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return InterimBeIndemniteFinMissionSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR mécanique (IFM forfaitaire 10 % refusée par Cass. BE). */
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
  result = signal<InterimBeIndemniteFinMissionResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  dateDebutMission = signal<string | null>(null);
  dateFinPrevue = signal<string | null>(null);
  dateFinReelle = signal<string | null>(null);
  dureeReellePrestationJours = signal<number | null>(null);
  dureePrevueJours = signal<number | null>(null);
  salaireHoraireBrut = signal<number | null>(null);
  heuresPrestees = signal<number | null>(null);
  heuresSupplementairesSemaine = signal<number | null>(0);
  heuresSupplementairesDimancheFerie = signal<number | null>(0);
  peculeDejaVerseParFsi = signal<boolean | null>(null);
  ruptureAnticipeeParEtiSansMotifGrave = signal<boolean | null>(null);
  commissionParitaireUtilisateur = signal<InterimBeCommissionParitaire | null>(null);
  ancienneteSectorielleJours = signal<number | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: InterimBeIndemniteFinMissionService,
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
    // SF-219-15b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (!this.dateDebutMission()) return false;
    if (!this.dateFinPrevue()) return false;
    if (!this.dateFinReelle()) return false;
    if (this.dureeReellePrestationJours() === null) return false;
    if ((this.dureeReellePrestationJours() as number) < 0) return false;
    if (this.dureePrevueJours() === null) return false;
    if ((this.dureePrevueJours() as number) <= 0) return false;
    if (this.salaireHoraireBrut() === null) return false;
    if ((this.salaireHoraireBrut() as number) <= 0) return false;
    if (this.heuresPrestees() === null) return false;
    if ((this.heuresPrestees() as number) < 0) return false;
    if (this.heuresSupplementairesSemaine() === null) return false;
    if ((this.heuresSupplementairesSemaine() as number) < 0) return false;
    if (this.heuresSupplementairesDimancheFerie() === null) return false;
    if ((this.heuresSupplementairesDimancheFerie() as number) < 0) return false;
    if (this.peculeDejaVerseParFsi() === null) return false;
    if (this.ruptureAnticipeeParEtiSansMotifGrave() === null) return false;
    if (this.commissionParitaireUtilisateur() === null) return false;
    if (this.ancienneteSectorielleJours() === null) return false;
    if ((this.ancienneteSectorielleJours() as number) < 0) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onDateDebutMissionChange(value: string | null): void {
    this.dateDebutMission.set(value);
  }
  onDateFinPrevueChange(value: string | null): void {
    this.dateFinPrevue.set(value);
  }
  onDateFinReelleChange(value: string | null): void {
    this.dateFinReelle.set(value);
  }
  onDureeReellePrestationJoursChange(value: string | number | null): void {
    this.dureeReellePrestationJours.set(this.toNumberOrNull(value));
  }
  onDureePrevueJoursChange(value: string | number | null): void {
    this.dureePrevueJours.set(this.toNumberOrNull(value));
  }
  onSalaireHoraireBrutChange(value: string | number | null): void {
    this.salaireHoraireBrut.set(this.toNumberOrNull(value));
  }
  onHeuresPresteesChange(value: string | number | null): void {
    this.heuresPrestees.set(this.toNumberOrNull(value));
  }
  onHeuresSupplementairesSemaineChange(value: string | number | null): void {
    this.heuresSupplementairesSemaine.set(this.toNumberOrNull(value));
  }
  onHeuresSupplementairesDimancheFerieChange(value: string | number | null): void {
    this.heuresSupplementairesDimancheFerie.set(this.toNumberOrNull(value));
  }
  onPeculeDejaVerseParFsiChange(value: boolean): void {
    this.peculeDejaVerseParFsi.set(value);
  }
  onRuptureAnticipeeParEtiSansMotifGraveChange(value: boolean): void {
    this.ruptureAnticipeeParEtiSansMotifGrave.set(value);
  }
  onCommissionParitaireUtilisateurChange(value: InterimBeCommissionParitaire): void {
    this.commissionParitaireUtilisateur.set(value);
  }
  onAncienneteSectorielleJoursChange(value: string | number | null): void {
    this.ancienneteSectorielleJours.set(this.toNumberOrNull(value));
  }

  private toNumberOrNull(value: string | number | null): number | null {
    if (value === null || value === '') return null;
    const n = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(n) ? n : null;
  }

  // -- Libellés humains --
  commissionParitaireLabel(value: InterimBeCommissionParitaire | null): string {
    switch (value) {
      case 'CP_124_CONSTRUCTION':
        return 'CP 124 — Construction (prime 9,12 % via Constructiv)';
      case 'CP_200_AUXILIAIRE_EMPLOYES':
        return 'CP 200 — Auxiliaire pour employés (prime 1/12, ancienneté 6 mois)';
      case 'CP_218_NON_MARCHAND':
        return 'CP 218 — Employés du non-marchand (CCT sectorielle interne)';
      case 'CP_322_INTERIM':
        return 'CP 322 — Travail intérimaire (renvoi à la CP utilisateur)';
      case 'CP_140_TRANSPORT_LOGISTIQUE':
        return 'CP 140 — Transport et logistique (prime 8,33 % via Fonds Social Transport)';
      case 'CP_209_METAUX_FABRICATIONS_METALLIQUES':
        return 'CP 209 — Métaux et fabrications métalliques';
      case 'AUTRE_NON_RECENSE':
        return 'Autre CP — non recensée par l\'outil V1 (analyse manuelle requise)';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'INDEMNITES_DUES':
        return 'Indemnités dues — au moins une composante (pécule vacances / prime fin d\'année / sursalaire heures sup) à verser';
      case 'RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR':
        return 'Rupture anticipée injustifiée — rémunérations restant à courir jusqu\'au terme prévu (Cass. 04/02/1991)';
      case 'AUCUNE_INDEMNITE_DUE':
        return 'Aucune indemnité de fin de mission due — fin au terme prévu + pécule FSI versé + pas de prime sectorielle';
      case 'A_ANALYSER_SECTEUR_NON_RECONNU':
        return 'Commission paritaire utilisateur non recensée — prime fin d\'année à analyser manuellement (CCT sectorielle propre)';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert / ambre / rouge / gris info). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'INDEMNITES_DUES':
        return 'verdict-ok';
      case 'RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR':
        return 'verdict-critical';
      case 'AUCUNE_INDEMNITE_DUE':
        return 'verdict-neutral';
      case 'A_ANALYSER_SECTEUR_NON_RECONNU':
        return 'verdict-neutral';
      default:
        return '';
    }
  }

  /** Icône Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'request_quote';
    switch (r.verdict) {
      case 'INDEMNITES_DUES':
        return 'paid';
      case 'RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR':
        return 'gavel';
      case 'AUCUNE_INDEMNITE_DUE':
        return 'do_not_disturb_on';
      case 'A_ANALYSER_SECTEUR_NON_RECONNU':
        return 'help_outline';
      default:
        return 'request_quote';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: InterimBeIndemniteFinMissionVerdict): string {
    switch (verdict) {
      case 'INDEMNITES_DUES':
        return 'INDEMNITÉS DUES';
      case 'RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR':
        return 'RUPTURE ANTICIPÉE';
      case 'AUCUNE_INDEMNITE_DUE':
        return 'AUCUNE INDEMNITÉ';
      case 'A_ANALYSER_SECTEUR_NON_RECONNU':
        return 'À ANALYSER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: InterimBeIndemniteFinMissionVerdict): string {
    switch (verdict) {
      case 'INDEMNITES_DUES':
        return 'badge-ok';
      case 'RUPTURE_ANTICIPEE_INDEMNITE_RESTE_A_COURIR':
        return 'badge-critical';
      case 'AUCUNE_INDEMNITE_DUE':
        return 'badge-neutral';
      case 'A_ANALYSER_SECTEUR_NON_RECONNU':
        return 'badge-neutral';
      default:
        return '';
    }
  }

  /** Format € arrondi à 2 décimales (locale FR pour cohérence avec autres outils). */
  formatEuro(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value.toLocaleString('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }) + ' €';
  }

  /** Format pourcentage (taux décimal → %). */
  formatPourcentage(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return (value * 100).toLocaleString('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }) + ' %';
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: InterimBeIndemniteFinMissionRequest = {
      dateDebutMission: this.dateDebutMission()!,
      dateFinPrevue: this.dateFinPrevue()!,
      dateFinReelle: this.dateFinReelle()!,
      dureeReellePrestationJours: this.dureeReellePrestationJours()!,
      dureePrevueJours: this.dureePrevueJours()!,
      salaireHoraireBrut: this.salaireHoraireBrut()!,
      heuresPrestees: this.heuresPrestees()!,
      heuresSupplementairesSemaine: this.heuresSupplementairesSemaine()!,
      heuresSupplementairesDimancheFerie: this.heuresSupplementairesDimancheFerie()!,
      peculeDejaVerseParFsi: this.peculeDejaVerseParFsi()!,
      ruptureAnticipeeParEtiSansMotifGrave: this.ruptureAnticipeeParEtiSansMotifGrave()!,
      commissionParitaireUtilisateur: this.commissionParitaireUtilisateur()!,
      ancienneteSectorielleJours: this.ancienneteSectorielleJours()!,
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Indemnité fin de mission calculée', 'OK', { duration: 2500 });
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
        this.dateFinPrevue.set(r.dateFinPrevue);
        this.dateFinReelle.set(r.dateFinReelle);
        this.dureeReellePrestationJours.set(r.dureeReellePrestationJours);
        this.dureePrevueJours.set(r.dureePrevueJours);
        this.salaireHoraireBrut.set(r.salaireHoraireBrut);
        this.heuresPrestees.set(r.heuresPrestees);
        this.heuresSupplementairesSemaine.set(r.heuresSupplementairesSemaine);
        this.heuresSupplementairesDimancheFerie.set(r.heuresSupplementairesDimancheFerie);
        this.peculeDejaVerseParFsi.set(r.peculeDejaVerseParFsi);
        this.ruptureAnticipeeParEtiSansMotifGrave.set(r.ruptureAnticipeeParEtiSansMotifGrave);
        this.commissionParitaireUtilisateur.set(r.commissionParitaireUtilisateur);
        this.ancienneteSectorielleJours.set(r.ancienneteSectorielleJours);
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
