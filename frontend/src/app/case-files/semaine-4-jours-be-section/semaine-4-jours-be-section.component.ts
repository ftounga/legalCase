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
  Semaine4JoursBeRequest,
  Semaine4JoursBeResponse,
  Semaine4JoursBeStatutDemande,
  Semaine4JoursBeVerdict,
} from '../../core/models/semaine-4-jours-be.model';
import {
  Semaine4JoursBeService,
} from '../../core/services/semaine-4-jours-be.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  Semaine4JoursBeSectionPrefillRules,
} from './semaine-4-jours-be-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-18b : outil décisionnel « semaine de 4 jours BE » (F-219).
 *
 * <p>BE uniquement — <b>Loi du 03/10/2022</b> portant des dispositions
 * diverses en matière de travail dite « Deal pour l'emploi » (M.B.
 * 10/11/2022), <b>art. 5</b> (régime à la demande du travailleur à
 * temps plein — compression durée hebdomadaire 38-40 h sur 4 jours,
 * journée plafonnée 9 h 30 ou 10 h CCT, avenant écrit ≤ 6 mois
 * renouvelable, refus motivé écrit dans le mois, protection
 * licenciement = 6 mois rémunération) et <b>art. 6</b> (procédure
 * simplifiée de modification du règlement de travail) ; <b>Loi du
 * 16/03/1971</b> art. 19 (limites quotidienne 9 h et hebdomadaire
 * 40 h de droit commun) ; <b>Loi du 03/07/1978</b> art. 25
 * (modification d'un élément essentiel du contrat) ; <b>Loi du
 * 08/04/1965</b> art. 12 (règlements de travail).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id {@code semaine-4-jours-be},
 * visibility ALWAYS_ON priority 136 BE / DROIT_DU_TRAVAIL — au-dessus
 * de {@code clause-ecolage-be} (135, SF-219-17b),
 * {@code teletravail-be-cct-85-149} (134, SF-219-16b),
 * {@code interim-be-indemnite-fin-mission} (133, SF-219-15b).</p>
 *
 * <h2>Verdict hiérarchisé 9 états (court-circuits backend)</h2>
 * <ul>
 *   <li>{@code CONFORME_REGIME_4_JOURS_VALIDE} (vert) — toutes
 *       conditions cumulatives remplies, régime opposable.</li>
 *   <li>{@code LICENCIEMENT_REPRESAILLES_PRESUME} (rouge) —
 *       licenciement post-demande sans motif objectif établi
 *       (art. 5 § 6) : indemnité forfaitaire 6 mois rémunération.</li>
 *   <li>{@code REFUS_EMPLOYEUR_NON_MOTIVE} (rouge) — refus sans
 *       motivation écrite ou hors délai (art. 5 § 5) : exposition
 *       action en abus de droit.</li>
 *   <li>{@code NON_ELIGIBLE_TEMPS_PARTIEL} (gris info) — temps
 *       partiel exclu du régime (art. 5 § 1).</li>
 *   <li>{@code NON_CONFORME_DEMANDE_ECRITE_MANQUANTE} (ambre) — pas
 *       de demande écrite (art. 5 § 2) : mise en place irrégulière.</li>
 *   <li>{@code NON_CONFORME_JOURNEE_DEPASSE_9H30} (ambre) — journée
 *       &gt; 9 h 30 sans CCT 10 h (art. 5 § 3) : sursalaire +
 *       action SPF ETCS.</li>
 *   <li>{@code NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT} (ambre) —
 *       avenant ou règlement de travail manquant
 *       (art. 5 § 4 + art. 6).</li>
 *   <li>{@code NON_CONFORME_DUREE_DEPASSE_6_MOIS} (ambre) — durée
 *       avenant &gt; 6 mois sans renouvellement (art. 5 § 4).</li>
 *   <li>{@code A_ANALYSER} (gris info) — statut indéterminé, en
 *       attente employeur, refus motivé opposable ou motif objectif
 *       à vérifier : analyse cas par cas.</li>
 * </ul>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern
 * uniforme F-213 / F-219 (statut demande, temps plein, demande
 * écrite, horaire compressé, avenant, règlement de travail,
 * licenciement, motif objectif non extractibles du pipeline Travail
 * BE actuel — déclaratifs RH ou avocat).</p>
 */
@Component({
  selector: 'app-semaine-4-jours-be-section',
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
  templateUrl: './semaine-4-jours-be-section.component.html',
  styleUrl: './semaine-4-jours-be-section.component.scss',
})
export class Semaine4JoursBeSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'semaine-4-jours-be';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'SEMAINE DE 4 JOURS (BE)';
  static readonly TOOL_ICON = 'view_week';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return Semaine4JoursBeSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR mécanique (Loi 13/06/1998 / 19/01/2000 = régime collectif). */
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
  result = signal<Semaine4JoursBeResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  statutDemande = signal<Semaine4JoursBeStatutDemande | null>(null);
  travailleurTempsPlein = signal<boolean | null>(null);
  demandeEcriteTravailleur = signal<boolean | null>(null);
  dateDemande = signal<string | null>(null);
  dureeHebdomadaireHeures = signal<number | null>(null);
  journeeMaximaleHeures = signal<number | null>(null);
  cctAutorise10h = signal<boolean | null>(null);
  avenantEcritSigne = signal<boolean | null>(null);
  reglementTravailModifie = signal<boolean | null>(null);
  dureeAvenantMois = signal<number | null>(null);
  avenantRenouvele = signal<boolean | null>(null);
  refusMotiveParEcrit = signal<boolean | null>(null);
  dateLicenciement = signal<string | null>(null);
  motifLicenciementObjectifEtabli = signal<boolean | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  /** dateLicenciement requise uniquement si statut LICENCIE_APRES_DEMANDE. */
  isLicencieScenario = computed(
    () => this.statutDemande() === 'LICENCIE_APRES_DEMANDE');

  constructor(
    private service: Semaine4JoursBeService,
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
    // SF-219-18b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.statutDemande() === null) return false;
    if (this.travailleurTempsPlein() === null) return false;
    if (this.demandeEcriteTravailleur() === null) return false;
    if (!this.dateDemande()) return false;
    if (this.dureeHebdomadaireHeures() === null) return false;
    const dureeHebdo = this.dureeHebdomadaireHeures() as number;
    if (dureeHebdo <= 0 || dureeHebdo > 60) return false;
    if (this.journeeMaximaleHeures() === null) return false;
    const journee = this.journeeMaximaleHeures() as number;
    if (journee <= 0 || journee > 16) return false;
    if (this.cctAutorise10h() === null) return false;
    if (this.avenantEcritSigne() === null) return false;
    if (this.reglementTravailModifie() === null) return false;
    if (this.dureeAvenantMois() === null) return false;
    const dureeAv = this.dureeAvenantMois() as number;
    if (dureeAv < 0 || dureeAv > 120) return false;
    if (this.avenantRenouvele() === null) return false;
    if (this.refusMotiveParEcrit() === null) return false;
    if (this.motifLicenciementObjectifEtabli() === null) return false;
    // dateLicenciement requise si LICENCIE_APRES_DEMANDE, sinon facultative.
    if (this.isLicencieScenario() && !this.dateLicenciement()) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onStatutDemandeChange(value: Semaine4JoursBeStatutDemande): void {
    this.statutDemande.set(value);
  }
  onTravailleurTempsPleinChange(value: boolean): void {
    this.travailleurTempsPlein.set(value);
  }
  onDemandeEcriteTravailleurChange(value: boolean): void {
    this.demandeEcriteTravailleur.set(value);
  }
  onDateDemandeChange(value: string | null): void {
    this.dateDemande.set(value);
  }
  onDureeHebdomadaireHeuresChange(value: string | number | null): void {
    this.dureeHebdomadaireHeures.set(this.toNumberOrNull(value));
  }
  onJourneeMaximaleHeuresChange(value: string | number | null): void {
    this.journeeMaximaleHeures.set(this.toNumberOrNull(value));
  }
  onCctAutorise10hChange(value: boolean): void {
    this.cctAutorise10h.set(value);
  }
  onAvenantEcritSigneChange(value: boolean): void {
    this.avenantEcritSigne.set(value);
  }
  onReglementTravailModifieChange(value: boolean): void {
    this.reglementTravailModifie.set(value);
  }
  onDureeAvenantMoisChange(value: string | number | null): void {
    this.dureeAvenantMois.set(this.toNumberOrNull(value));
  }
  onAvenantRenouveleChange(value: boolean): void {
    this.avenantRenouvele.set(value);
  }
  onRefusMotiveParEcritChange(value: boolean): void {
    this.refusMotiveParEcrit.set(value);
  }
  onDateLicenciementChange(value: string | null): void {
    this.dateLicenciement.set(value);
  }
  onMotifLicenciementObjectifEtabliChange(value: boolean): void {
    this.motifLicenciementObjectifEtabli.set(value);
  }

  private toNumberOrNull(value: string | number | null): number | null {
    if (value === null || value === '') return null;
    const n = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(n) ? n : null;
  }

  // -- Libellés humains --
  statutDemandeLabel(value: Semaine4JoursBeStatutDemande | null): string {
    switch (value) {
      case 'ACCORDE_AVENANT_SIGNE':
        return 'Accord employeur + avenant signé';
      case 'REFUSE_MOTIVE_PAR_ECRIT':
        return 'Refus motivé par écrit dans le délai d\'1 mois';
      case 'REFUSE_SANS_MOTIVATION_ECRITE':
        return 'Refus sans motivation écrite ou hors délai';
      case 'EN_ATTENTE_REPONSE_EMPLOYEUR':
        return 'En attente de réponse de l\'employeur (délai 1 mois)';
      case 'LICENCIE_APRES_DEMANDE':
        return 'Licenciement intervenu après la demande';
      case 'INDETERMINE':
        return 'Statut indéterminé (analyse cas par cas)';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'CONFORME_REGIME_4_JOURS_VALIDE':
        return 'Régime de la semaine de 4 jours valablement mis en place — toutes conditions cumulatives remplies (art. 5 Loi 03/10/2022)';
      case 'LICENCIEMENT_REPRESAILLES_PRESUME':
        return 'Licenciement présumé représailles — indemnité forfaitaire de 6 mois de rémunération due (art. 5 § 6)';
      case 'REFUS_EMPLOYEUR_NON_MOTIVE':
        return 'Refus employeur non motivé par écrit ou hors délai d\'1 mois — exposition à l\'action en abus de droit (art. 5 § 5)';
      case 'NON_ELIGIBLE_TEMPS_PARTIEL':
        return 'Travailleur à temps partiel — régime non ouvert (art. 5 § 1)';
      case 'NON_CONFORME_DEMANDE_ECRITE_MANQUANTE':
        return 'Demande écrite du travailleur manquante (art. 5 § 2) — mise en place irrégulière, requalifiable en horaire imposé';
      case 'NON_CONFORME_JOURNEE_DEPASSE_9H30':
        return 'Journée quotidienne > plafond autorisé (art. 5 § 3) — sursalaire + risque action SPF ETCS';
      case 'NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT':
        return 'Avenant écrit au contrat ou règlement de travail manquant (art. 5 § 4 + art. 6) — mise en place irrégulière';
      case 'NON_CONFORME_DUREE_DEPASSE_6_MOIS':
        return 'Durée de l\'avenant > 6 mois sans renouvellement explicite (art. 5 § 4) — fragilité contractuelle';
      case 'A_ANALYSER':
        return 'Configuration ambigüe — analyse cas par cas requise';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert / ambre / rouge / gris info). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'CONFORME_REGIME_4_JOURS_VALIDE':
        return 'verdict-ok';
      case 'LICENCIEMENT_REPRESAILLES_PRESUME':
      case 'REFUS_EMPLOYEUR_NON_MOTIVE':
        return 'verdict-critical';
      case 'NON_CONFORME_DEMANDE_ECRITE_MANQUANTE':
      case 'NON_CONFORME_JOURNEE_DEPASSE_9H30':
      case 'NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT':
      case 'NON_CONFORME_DUREE_DEPASSE_6_MOIS':
        return 'verdict-warn';
      case 'NON_ELIGIBLE_TEMPS_PARTIEL':
      case 'A_ANALYSER':
        return 'verdict-neutral';
      default:
        return '';
    }
  }

  /** Icône Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'view_week';
    switch (r.verdict) {
      case 'CONFORME_REGIME_4_JOURS_VALIDE':
        return 'check_circle';
      case 'LICENCIEMENT_REPRESAILLES_PRESUME':
        return 'gavel';
      case 'REFUS_EMPLOYEUR_NON_MOTIVE':
        return 'block';
      case 'NON_ELIGIBLE_TEMPS_PARTIEL':
        return 'do_not_disturb_on';
      case 'NON_CONFORME_DEMANDE_ECRITE_MANQUANTE':
        return 'description';
      case 'NON_CONFORME_JOURNEE_DEPASSE_9H30':
        return 'schedule';
      case 'NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT':
        return 'edit_document';
      case 'NON_CONFORME_DUREE_DEPASSE_6_MOIS':
        return 'hourglass_top';
      case 'A_ANALYSER':
        return 'help_outline';
      default:
        return 'view_week';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: Semaine4JoursBeVerdict): string {
    switch (verdict) {
      case 'CONFORME_REGIME_4_JOURS_VALIDE':
        return 'CONFORME';
      case 'LICENCIEMENT_REPRESAILLES_PRESUME':
        return 'REPRÉSAILLES';
      case 'REFUS_EMPLOYEUR_NON_MOTIVE':
        return 'REFUS NON MOTIVÉ';
      case 'NON_ELIGIBLE_TEMPS_PARTIEL':
        return 'NON ÉLIGIBLE';
      case 'NON_CONFORME_DEMANDE_ECRITE_MANQUANTE':
        return 'DEMANDE MANQUANTE';
      case 'NON_CONFORME_JOURNEE_DEPASSE_9H30':
        return 'JOURNÉE > PLAFOND';
      case 'NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT':
        return 'FORMALISATION';
      case 'NON_CONFORME_DUREE_DEPASSE_6_MOIS':
        return 'DURÉE > 6 MOIS';
      case 'A_ANALYSER':
        return 'À ANALYSER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: Semaine4JoursBeVerdict): string {
    switch (verdict) {
      case 'CONFORME_REGIME_4_JOURS_VALIDE':
        return 'badge-ok';
      case 'LICENCIEMENT_REPRESAILLES_PRESUME':
      case 'REFUS_EMPLOYEUR_NON_MOTIVE':
        return 'badge-critical';
      case 'NON_CONFORME_DEMANDE_ECRITE_MANQUANTE':
      case 'NON_CONFORME_JOURNEE_DEPASSE_9H30':
      case 'NON_CONFORME_AVENANT_OU_REGLEMENT_MANQUANT':
      case 'NON_CONFORME_DUREE_DEPASSE_6_MOIS':
        return 'badge-warn';
      case 'NON_ELIGIBLE_TEMPS_PARTIEL':
      case 'A_ANALYSER':
        return 'badge-neutral';
      default:
        return '';
    }
  }

  /** Format heures arrondi à 2 décimales (locale FR). */
  formatHeures(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value.toLocaleString('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }) + ' h';
  }

  /** Boolean → Oui / Non / —. */
  formatBoolean(value: boolean | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value ? 'Oui' : 'Non';
  }

  calculate(): void {
    if (!this.formValid()) return;
    if (this.workspaceCountry !== 'BELGIQUE') {
      this.snackBar.open('Outil indisponible pour ce workspace', 'Fermer',
        { duration: 4000, panelClass: 'snack-error' });
      return;
    }
    const request: Semaine4JoursBeRequest = {
      statutDemande: this.statutDemande()!,
      travailleurTempsPlein: this.travailleurTempsPlein()!,
      demandeEcriteTravailleur: this.demandeEcriteTravailleur()!,
      dateDemande: this.dateDemande()!,
      dureeHebdomadaireHeures: this.dureeHebdomadaireHeures()!,
      journeeMaximaleHeures: this.journeeMaximaleHeures()!,
      cctAutorise10h: this.cctAutorise10h()!,
      avenantEcritSigne: this.avenantEcritSigne()!,
      reglementTravailModifie: this.reglementTravailModifie()!,
      dureeAvenantMois: this.dureeAvenantMois()!,
      avenantRenouvele: this.avenantRenouvele()!,
      refusMotiveParEcrit: this.refusMotiveParEcrit()!,
      dateLicenciement: this.dateLicenciement(),
      motifLicenciementObjectifEtabli: this.motifLicenciementObjectifEtabli()!,
    };
    this.calculating.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Conformité semaine de 4 jours analysée', 'OK', { duration: 2500 });
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
        this.statutDemande.set(r.statutDemande);
        this.travailleurTempsPlein.set(r.travailleurTempsPlein);
        this.demandeEcriteTravailleur.set(r.demandeEcriteTravailleur);
        this.dateDemande.set(r.dateDemande);
        this.dureeHebdomadaireHeures.set(r.dureeHebdomadaireHeures);
        this.journeeMaximaleHeures.set(r.journeeMaximaleHeures);
        this.cctAutorise10h.set(r.cctAutorise10h);
        this.avenantEcritSigne.set(r.avenantEcritSigne);
        this.reglementTravailModifie.set(r.reglementTravailModifie);
        this.dureeAvenantMois.set(r.dureeAvenantMois);
        this.avenantRenouvele.set(r.avenantRenouvele);
        this.refusMotiveParEcrit.set(r.refusMotiveParEcrit);
        this.dateLicenciement.set(r.dateLicenciement);
        this.motifLicenciementObjectifEtabli.set(r.motifLicenciementObjectifEtabli);
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
