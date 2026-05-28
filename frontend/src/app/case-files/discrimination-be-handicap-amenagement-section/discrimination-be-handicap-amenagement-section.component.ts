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
  DiscriminationBeHandicapAmenagementRequest,
  DiscriminationBeHandicapAmenagementResponse,
  DiscriminationBeHandicapAmenagementReponseEmployeur,
  DiscriminationBeHandicapAmenagementSanctionSubie,
  DiscriminationBeHandicapAmenagementStatutHandicap,
  DiscriminationBeHandicapAmenagementVerdict,
} from '../../core/models/discrimination-be-handicap-amenagement.model';
import {
  DiscriminationBeHandicapAmenagementService,
} from '../../core/services/discrimination-be-handicap-amenagement.service';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import {
  DiscriminationBeHandicapAmenagementSectionPrefillRules,
} from './discrimination-be-handicap-amenagement-section-prefill-rules';
import {
  ToolJurisprudenceCitationsComponent,
} from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-219-23b : outil décisionnel « refus d'aménagements raisonnables
 * handicap BE » (F-219).
 *
 * <p>BE uniquement — <b>Loi du 10/05/2007</b> tendant à lutter contre
 * certaines formes de discrimination (M.B. 30/05/2007), art. 4 4° (notion
 * fonctionnelle de handicap, transposition CJUE Ring/Werge), art. 14
 * (interdiction discrimination indirecte par défaut d'aménagement
 * raisonnable), art. 17 (présomption de représailles), art. 28
 * (sanctions civiles — indemnité forfaitaire 6 mois ou préjudice réel +
 * § 3 renversement charge de la preuve) ; <b>CCT n° 95 du 10/10/2008</b>
 * du CNT (NAR) sur l'égalité de traitement durant toutes les phases de
 * la relation de travail ; <b>Directive 2000/78/CE</b> art. 5 (cadre
 * général égalité de traitement, obligation aménagement raisonnable) ;
 * <b>Convention ONU 13/12/2006</b> relative aux droits des personnes
 * handicapées art. 5 (égalité et non-discrimination) et art. 27
 * (travail et emploi) ; <b>Convention OIT n° 159</b> sur la
 * réadaptation professionnelle et l'emploi des personnes handicapées
 * (1983).</p>
 *
 * <p>Affiché par le panel F-IA-04 (tool_id
 * {@code discrimination-be-handicap-amenagement}, visibility ALWAYS_ON
 * priority 141 BE / DROIT_DU_TRAVAIL — au-dessus de
 * {@code egalite-femmes-hommes-be} (140, SF-219-22b),
 * {@code eco-cheques-cheques-repas-be} (139, SF-219-21b),
 * {@code pecule-vacances-be} (138, SF-219-20b),
 * {@code droit-deconnexion-be} (137, SF-219-19b).</p>
 *
 * <h2>Verdict hiérarchisé 7 états (court-circuits backend)</h2>
 * <ul>
 *   <li>{@code A_ANALYSER} (gris info) — configuration ambigüe (procédure
 *       en cours, attente avis SEPP, articulation reclassement).</li>
 *   <li>{@code FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE} (ambre) —
 *       statut handicap contestable, préalable à trancher.</li>
 *   <li>{@code REPRESAILLES_PRESUMEES_LICENCIEMENT} (rouge) — sanction
 *       après demande, présomption art. 17 + indemnité forfaitaire 6 mois.</li>
 *   <li>{@code DISCRIMINATION_PRESUMEE_NON_MOTIVATION} (rouge) — refus
 *       sans motivation détaillée écrite (art. 28 § 3 charge de la preuve).</li>
 *   <li>{@code DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE} (rouge) —
 *       refus motivé mais charge disproportionnée non démontrée.</li>
 *   <li>{@code CONFORME_AMENAGEMENT_ACCORDE} (vert) — aménagement
 *       accordé ou contre-proposition acceptable.</li>
 *   <li>{@code CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE} (vert) —
 *       refus fondé sur charge disproportionnée objectivement démontrée.</li>
 * </ul>
 *
 * <p><b>Pré-fill IA V1</b> : aucun champ — alignement pattern
 * uniforme F-213 / F-219 (statut handicap, demande, type d'aménagement,
 * coût, subsides, effectif, CA, réponse employeur, motivation, avis
 * SEPP, charge disproportionnée invoquée, devis, mesures alternatives,
 * sanction, salaire, procédure UNIA non extractibles du dossier
 * salarié individuel — déclaratifs RH employeur / médecin du travail /
 * avocat post-instruction).</p>
 */
@Component({
  selector: 'app-discrimination-be-handicap-amenagement-section',
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
  templateUrl: './discrimination-be-handicap-amenagement-section.component.html',
  styleUrl: './discrimination-be-handicap-amenagement-section.component.scss',
})
export class DiscriminationBeHandicapAmenagementSectionComponent
  implements OnInit, OnChanges {

  // F-JU-03 — citations jurisprudentielles F-JU-01 (BE).
  protected readonly toolIdForJurisprudence = 'discrimination-be-handicap-amenagement';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'AMÉNAGEMENTS RAISONNABLES HANDICAP (BE)';
  static readonly TOOL_ICON = 'accessible';

  /** F-177 SF-177-12 / F-236 SF-236-02 — délègue au helper partagé (parité runtime). */
  static getPrefillCount(input: PrefillCountInput): number {
    return DiscriminationBeHandicapAmenagementSectionPrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  /** Gate strict BE — pas d'équivalent FR mécanique (OETH = régime distinct). */
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
  result = signal<DiscriminationBeHandicapAmenagementResponse | null>(null);

  // ── Champs du form (signals — édités via handlers). ────────────────────
  statutHandicap = signal<DiscriminationBeHandicapAmenagementStatutHandicap | null>(null);
  dateDemandeAmenagement = signal<string | null>(null);
  typeAmenagementDemande = signal<string | null>(null);
  coutEstimeAmenagement = signal<number | null>(null);
  subsidesDemandes = signal<boolean | null>(null);
  effectifEntreprise = signal<number | null>(null);
  chiffreAffairesAnnuel = signal<number | null>(null);
  reponseEmployeur = signal<DiscriminationBeHandicapAmenagementReponseEmployeur | null>(null);
  motivationDetailleeFournie = signal<boolean | null>(null);
  avisSeppFavorable = signal<boolean | null>(null);
  chargeDisproportionneeInvoquee = signal<boolean | null>(null);
  devisExterneFourni = signal<boolean | null>(null);
  mesuresAlternativesProposees = signal<boolean | null>(null);
  sanctionSubie = signal<DiscriminationBeHandicapAmenagementSanctionSubie | null>(null);
  dateSanction = signal<string | null>(null);
  salaireMensuelBrut = signal<number | null>(null);
  procedureUniaSaisie = signal<boolean | null>(null);

  /** Gate strict workspace BE. */
  isAvailable = computed(() => this.workspaceCountry === 'BELGIQUE');

  constructor(
    private service: DiscriminationBeHandicapAmenagementService,
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
    // SF-219-23b V1 : pas de pré-fill IA (cf. helper doc) — pas de réaction aiData.
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    if (this.statutHandicap() === null) return false;
    if (this.subsidesDemandes() === null) return false;
    if (this.effectifEntreprise() === null) return false;
    const eff = this.effectifEntreprise() as number;
    if (eff < 0 || eff > 1000000) return false;
    if (this.reponseEmployeur() === null) return false;
    if (this.motivationDetailleeFournie() === null) return false;
    if (this.avisSeppFavorable() === null) return false;
    if (this.chargeDisproportionneeInvoquee() === null) return false;
    if (this.devisExterneFourni() === null) return false;
    if (this.mesuresAlternativesProposees() === null) return false;
    if (this.sanctionSubie() === null) return false;
    if (this.procedureUniaSaisie() === null) return false;
    const cout = this.coutEstimeAmenagement();
    if (cout !== null && cout < 0) return false;
    const ca = this.chiffreAffairesAnnuel();
    if (ca !== null && ca < 0) return false;
    const sal = this.salaireMensuelBrut();
    if (sal !== null && sal < 0) return false;
    const desc = this.typeAmenagementDemande();
    if (desc !== null && desc.length > 500) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // -- Handlers --
  onStatutHandicapChange(value: DiscriminationBeHandicapAmenagementStatutHandicap): void {
    this.statutHandicap.set(value);
  }
  onDateDemandeAmenagementChange(value: string | null): void {
    this.dateDemandeAmenagement.set(value);
  }
  onTypeAmenagementDemandeChange(value: string | null): void {
    this.typeAmenagementDemande.set(value);
  }
  onCoutEstimeAmenagementChange(value: string | number | null): void {
    this.coutEstimeAmenagement.set(this.toNumberOrNull(value));
  }
  onSubsidesDemandesChange(value: boolean): void {
    this.subsidesDemandes.set(value);
  }
  onEffectifEntrepriseChange(value: string | number | null): void {
    this.effectifEntreprise.set(this.toNumberOrNull(value));
  }
  onChiffreAffairesAnnuelChange(value: string | number | null): void {
    this.chiffreAffairesAnnuel.set(this.toNumberOrNull(value));
  }
  onReponseEmployeurChange(value: DiscriminationBeHandicapAmenagementReponseEmployeur): void {
    this.reponseEmployeur.set(value);
  }
  onMotivationDetailleeFournieChange(value: boolean): void {
    this.motivationDetailleeFournie.set(value);
  }
  onAvisSeppFavorableChange(value: boolean): void {
    this.avisSeppFavorable.set(value);
  }
  onChargeDisproportionneeInvoqueeChange(value: boolean): void {
    this.chargeDisproportionneeInvoquee.set(value);
  }
  onDevisExterneFourniChange(value: boolean): void {
    this.devisExterneFourni.set(value);
  }
  onMesuresAlternativesProposeesChange(value: boolean): void {
    this.mesuresAlternativesProposees.set(value);
  }
  onSanctionSubieChange(value: DiscriminationBeHandicapAmenagementSanctionSubie): void {
    this.sanctionSubie.set(value);
  }
  onDateSanctionChange(value: string | null): void {
    this.dateSanction.set(value);
  }
  onSalaireMensuelBrutChange(value: string | number | null): void {
    this.salaireMensuelBrut.set(this.toNumberOrNull(value));
  }
  onProcedureUniaSaisieChange(value: boolean): void {
    this.procedureUniaSaisie.set(value);
  }

  private toNumberOrNull(value: string | number | null): number | null {
    if (value === null || value === '') return null;
    const n = typeof value === 'number' ? value : Number(value);
    return Number.isFinite(n) ? n : null;
  }

  // -- Libellés humains --
  statutHandicapLabel(value: DiscriminationBeHandicapAmenagementStatutHandicap | null): string {
    switch (value) {
      case 'RECONNU_OFFICIEL':
        return 'Reconnaissance officielle (AVIQ / VAPH / PHARE / DGPH)';
      case 'MEDECIN_TRAVAIL_AVIS':
        return 'Avis SEPP médecin du travail constatant la limitation';
      case 'INVALIDITE_MUTUELLE':
        return 'Invalidité reconnue par la mutualité (> 12 mois)';
      case 'DURABLE_FACTUEL':
        return 'Situation factuelle durable sans reconnaissance officielle';
      case 'CONTESTE':
        return 'Qualification handicap contestée par l\'employeur';
      case 'INDETERMINE':
        return 'Statut non encore déterminé';
      default:
        return '—';
    }
  }

  reponseEmployeurLabel(value: DiscriminationBeHandicapAmenagementReponseEmployeur | null): string {
    switch (value) {
      case 'ACCORDE':
        return 'Aménagement accordé intégralement';
      case 'CONTRE_PROPOSITION':
        return 'Contre-proposition employeur';
      case 'REFUSE_MOTIVE_DETAILLE':
        return 'Refus avec motivation détaillée écrite';
      case 'REFUSE_NON_MOTIVE':
        return 'Refus sans motivation détaillée';
      case 'NON_REPONDU_RAISONNABLE':
        return 'Silence employeur au-delà du délai raisonnable';
      case 'EN_ATTENTE':
        return 'Délai raisonnable encore en cours';
      case 'INDETERMINE':
        return 'Statut indéterminé';
      default:
        return '—';
    }
  }

  sanctionSubieLabel(value: DiscriminationBeHandicapAmenagementSanctionSubie | null): string {
    switch (value) {
      case 'LICENCIEMENT':
        return 'Licenciement post-demande (présomption représailles)';
      case 'REGRESSION_CONDITIONS_TRAVAIL':
        return 'Régression conditions de travail (présomption représailles)';
      case 'HARCELEMENT_REPRESAILLE':
        return 'Harcèlement représaille (articulation Loi 04/08/1996)';
      case 'AUCUNE':
        return 'Aucune sanction subie';
      default:
        return '—';
    }
  }

  /** Verdict en français lisible. */
  verdictLabel(): string {
    const r = this.result();
    if (!r) return '—';
    switch (r.verdict) {
      case 'CONFORME_AMENAGEMENT_ACCORDE':
        return 'Conformité — aménagement raisonnable accordé ou contre-proposition acceptable (art. 14 Loi 10/05/2007 + CCT n° 95)';
      case 'CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE':
        return 'Conformité — refus fondé sur charge disproportionnée objectivement démontrée (devis externe + subsides refusés motivés + effectif insuffisant)';
      case 'DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE':
        return 'Discrimination indirecte — refus motivé mais charge disproportionnée non démontrée (subsides non demandés ou alternatives non explorées)';
      case 'DISCRIMINATION_PRESUMEE_NON_MOTIVATION':
        return 'Présomption de discrimination — refus sans motivation détaillée écrite, renversement de la charge de la preuve (art. 28 § 3 Loi 10/05/2007 + CJUE Coleman)';
      case 'REPRESAILLES_PRESUMEES_LICENCIEMENT':
        return 'Représailles présumées — sanction post-demande d\'aménagement, indemnité forfaitaire 6 mois (art. 28 § 2 1°) cumulable avec indemnité de rupture (art. 17 Loi 10/05/2007)';
      case 'FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE':
        return 'Qualification handicap incertaine — préalable à trancher avant toute analyse discrimination (avis médecin du travail SEPP à solliciter)';
      case 'A_ANALYSER':
        return 'Configuration ambigüe — procédure en cours / attente avis SEPP / articulation reclassement médical — analyse cas par cas';
      default:
        return '—';
    }
  }

  /** Classe CSS verdict (vert / rouge / ambre / gris info). */
  verdictClass(): string {
    const r = this.result();
    if (!r) return '';
    switch (r.verdict) {
      case 'CONFORME_AMENAGEMENT_ACCORDE':
      case 'CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE':
        return 'verdict-ok';
      case 'DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE':
      case 'DISCRIMINATION_PRESUMEE_NON_MOTIVATION':
      case 'REPRESAILLES_PRESUMEES_LICENCIEMENT':
        return 'verdict-critical';
      case 'FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE':
        return 'verdict-warn';
      case 'A_ANALYSER':
        return 'verdict-neutral';
      default:
        return '';
    }
  }

  /** Icône Material du verdict. */
  verdictIcon(): string {
    const r = this.result();
    if (!r) return 'accessible';
    switch (r.verdict) {
      case 'CONFORME_AMENAGEMENT_ACCORDE':
        return 'verified';
      case 'CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE':
        return 'check_circle';
      case 'DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE':
        return 'rule_folder';
      case 'DISCRIMINATION_PRESUMEE_NON_MOTIVATION':
        return 'report';
      case 'REPRESAILLES_PRESUMEES_LICENCIEMENT':
        return 'gavel';
      case 'FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE':
        return 'warning';
      case 'A_ANALYSER':
        return 'help_outline';
      default:
        return 'accessible';
    }
  }

  /** Libellé bref pour le badge du header. */
  verdictBadge(verdict: DiscriminationBeHandicapAmenagementVerdict): string {
    switch (verdict) {
      case 'CONFORME_AMENAGEMENT_ACCORDE':
        return 'ACCORDÉ';
      case 'CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE':
        return 'CONFORME';
      case 'DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE':
        return 'REFUS INJUSTIFIÉ';
      case 'DISCRIMINATION_PRESUMEE_NON_MOTIVATION':
        return 'NON MOTIVÉ';
      case 'REPRESAILLES_PRESUMEES_LICENCIEMENT':
        return 'REPRÉSAILLES';
      case 'FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE':
        return 'À QUALIFIER';
      case 'A_ANALYSER':
        return 'À ANALYSER';
      default:
        return '';
    }
  }

  /** Classe CSS badge du header. */
  verdictBadgeClass(verdict: DiscriminationBeHandicapAmenagementVerdict): string {
    switch (verdict) {
      case 'CONFORME_AMENAGEMENT_ACCORDE':
      case 'CONFORME_CHARGE_DISPROPORTIONNEE_DEMONTREE':
        return 'badge-ok';
      case 'DISCRIMINATION_INDIRECTE_REFUS_INJUSTIFIE':
      case 'DISCRIMINATION_PRESUMEE_NON_MOTIVATION':
      case 'REPRESAILLES_PRESUMEES_LICENCIEMENT':
        return 'badge-critical';
      case 'FRAGILE_QUALIFICATION_HANDICAP_INCERTAINE':
        return 'badge-warn';
      case 'A_ANALYSER':
        return 'badge-neutral';
      default:
        return '';
    }
  }

  /** Format euros — 2 décimales, locale FR. */
  formatEuros(value: number | null | undefined): string {
    if (value === null || value === undefined) return '—';
    return value.toLocaleString('fr-FR', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }) + ' €';
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
    const request: DiscriminationBeHandicapAmenagementRequest = {
      statutHandicap: this.statutHandicap()!,
      dateDemandeAmenagement: this.dateDemandeAmenagement(),
      typeAmenagementDemande: this.typeAmenagementDemande(),
      coutEstimeAmenagement: this.coutEstimeAmenagement(),
      subsidesDemandes: this.subsidesDemandes()!,
      effectifEntreprise: this.effectifEntreprise()!,
      chiffreAffairesAnnuel: this.chiffreAffairesAnnuel(),
      reponseEmployeur: this.reponseEmployeur()!,
      motivationDetailleeFournie: this.motivationDetailleeFournie()!,
      avisSeppFavorable: this.avisSeppFavorable()!,
      chargeDisproportionneeInvoquee: this.chargeDisproportionneeInvoquee()!,
      devisExterneFourni: this.devisExterneFourni()!,
      mesuresAlternativesProposees: this.mesuresAlternativesProposees()!,
      sanctionSubie: this.sanctionSubie()!,
      dateSanction: this.dateSanction(),
      salaireMensuelBrut: this.salaireMensuelBrut(),
      procedureUniaSaisie: this.procedureUniaSaisie()!,
    };
    this.calculating.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse aménagements raisonnables calculée', 'OK', { duration: 2500 });
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
        this.statutHandicap.set(r.statutHandicap);
        this.dateDemandeAmenagement.set(r.dateDemandeAmenagement);
        this.typeAmenagementDemande.set(r.typeAmenagementDemande);
        this.coutEstimeAmenagement.set(r.coutEstimeAmenagement);
        this.subsidesDemandes.set(r.subsidesDemandes);
        this.effectifEntreprise.set(r.effectifEntreprise);
        this.chiffreAffairesAnnuel.set(r.chiffreAffairesAnnuel);
        this.reponseEmployeur.set(r.reponseEmployeur);
        this.motivationDetailleeFournie.set(r.motivationDetailleeFournie);
        this.avisSeppFavorable.set(r.avisSeppFavorable);
        this.chargeDisproportionneeInvoquee.set(r.chargeDisproportionneeInvoquee);
        this.devisExterneFourni.set(r.devisExterneFourni);
        this.mesuresAlternativesProposees.set(r.mesuresAlternativesProposees);
        this.sanctionSubie.set(r.sanctionSubie);
        this.dateSanction.set(r.dateSanction);
        this.salaireMensuelBrut.set(r.salaireMensuelBrut);
        this.procedureUniaSaisie.set(r.procedureUniaSaisie);
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
