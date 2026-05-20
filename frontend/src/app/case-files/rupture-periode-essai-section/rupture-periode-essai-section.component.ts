import {
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RupturePeriodeEssaiService } from '../../core/services/rupture-periode-essai.service';
import {
  AuteurRupture,
  CategorieSocioProfessionnelle,
  DiscriminationMotif,
  RupturePeriodeEssaiRequest,
  RupturePeriodeEssaiResponse,
  RupturePeriodeEssaiVerdict,
  TypeContratEssai,
} from '../../core/models/rupture-periode-essai.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { RupturePeriodeEssaiSectionPrefillRules } from './rupture-periode-essai-section-prefill-rules';

/**
 * SF-DT-38-02 : composant Angular standalone pour l'outil décisionnel
 * "Rupture de période d'essai (qualification régulière / abusive / nulle /
 * illégale)" (F-DT-38). FRANCE uniquement.
 *
 * Pattern de référence : `procedure-nullite-licenciement-section` (F-DT-36).
 *
 * - Form : catégorie socio-pro / type contrat / dates / durée essai /
 *   renouvellement / auteur rupture / prévenance / motif / protections /
 *   convention collective / salaire.
 * - Verdict 4 niveaux : REGULIERE (navy) / RISQUE_ABUSIVE (or) / NULLE (rouge
 *   avec mention "Option principale : réintégration") / ILLEGALE (rouge avec
 *   mention "Barème Macron L.1235-3 applicable — voir F-DT-08").
 * - Indemnité fourchette 1-6 mois × salaire pour RISQUE_ABUSIVE.
 * - Pré-fill IA basé sur champs déjà extraits (9 champs — extension F-246
 *   exhaustive différée à SF F-246 dédiée).
 * - Refresh dashboard F-IA-02 + MatSnackBar erreurs.
 *
 * Source signal terrain : 2ᵉ démo Marjolaine RENVERSEZ 18/05/2026 + mail
 * détaillé 19/05/2026 18:38.
 */
@Component({
  selector: 'app-rupture-periode-essai-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    MatSelectModule,
    LegalCitationsPipe,
  ],
  templateUrl: './rupture-periode-essai-section.component.html',
  styleUrl: './rupture-periode-essai-section.component.scss',
})
export class RupturePeriodeEssaiSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour la card.
  static readonly TOOL_LABEL = 'RUPTURE DE PÉRIODE D\'ESSAI';
  static readonly TOOL_ICON = 'gavel';

  /**
   * F-236 SF-236-01 / F-237 SF-237-01 — délègue au helper partagé.
   * Parité stricte avec `prefillFromAi()` : mêmes mappings, même gate FR.
   */
  static getPrefillCount(input: {
    aiData?: TravailExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return RupturePeriodeEssaiSectionPrefillRules.computePrefillCount({
      aiData: input.aiData,
      workspaceCountry: input.workspaceCountry,
    });
  }

  @Input() caseFileId!: string;
  @Input() forceExpanded = false;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<RupturePeriodeEssaiResponse | null>(null);

  // --- Form fields ---
  categorieSocioProfessionnelle = signal<CategorieSocioProfessionnelle>('CADRE');
  typeContrat = signal<TypeContratEssai>('CDI');
  dureeCddMois = signal<number | null>(null);
  dateDebutContrat = signal<string | null>(null);
  dateRupture = signal<string | null>(null);
  dureePeriodeEssaiContractuelleMois = signal<number>(4);
  renouvellementInvoque = signal<boolean>(false);
  accordBrancheRenouvellement = signal<boolean>(false);
  accordEcritSalarieRenouvellement = signal<boolean>(false);
  auteurRupture = signal<AuteurRupture>('EMPLOYEUR');
  delaiPrevenanceJoursAppliques = signal<number | null>(null);
  motifInvoque = signal<string | null>(null);
  motifLieAuxCompetencesProfessionnelles = signal<boolean>(true);
  motifEconomiqueOuOrganisationnel = signal<boolean>(false);
  discriminationInvoquee = signal<DiscriminationMotif | null>(null);
  grossesseAuMomentRupture = signal<boolean>(false);
  arretAccidentTravailEnCours = signal<boolean>(false);
  atteinteLiberteFondamentale = signal<string | null>(null);
  lettreRuptureMotivee = signal<boolean>(false);
  motifsAveresParPieces = signal<boolean>(false);
  conventionCollectiveApplicable = signal<boolean>(false);
  conventionCollectivePlusFavorableRespectee = signal<boolean>(true);
  salaireMensuelBrut = signal<number | null>(null);

  // Provenance signals (badges IA) — `'IA'` tant que la valeur vient de l'IA,
  // `null` après modification manuelle.
  provenanceTypeContrat = signal<'IA' | null>(null);
  provenanceDateDebutContrat = signal<'IA' | null>(null);
  provenanceDateRupture = signal<'IA' | null>(null);
  provenanceMotifInvoque = signal<'IA' | null>(null);
  provenanceDiscrimination = signal<'IA' | null>(null);
  provenanceGrossesse = signal<'IA' | null>(null);
  provenanceArretAt = signal<'IA' | null>(null);
  provenanceConventionApplicable = signal<'IA' | null>(null);
  provenanceSalaire = signal<'IA' | null>(null);

  // Options listes
  readonly categorieOptions: { value: CategorieSocioProfessionnelle; label: string }[] = [
    { value: 'OUVRIER_EMPLOYE', label: 'Ouvrier / employé' },
    { value: 'AGENT_MAITRISE_TECHNICIEN', label: 'Agent de maîtrise / technicien' },
    { value: 'CADRE', label: 'Cadre' },
  ];
  readonly typeContratOptions: { value: TypeContratEssai; label: string }[] = [
    { value: 'CDI', label: 'CDI' },
    { value: 'CDD', label: 'CDD' },
    { value: 'INTERIM', label: 'Intérim' },
  ];
  readonly auteurOptions: { value: AuteurRupture; label: string }[] = [
    { value: 'EMPLOYEUR', label: 'Employeur' },
    { value: 'SALARIE', label: 'Salarié' },
  ];
  readonly discriminationOptions: { value: DiscriminationMotif | 'AUCUNE'; label: string }[] = [
    { value: 'AUCUNE', label: 'Aucune discrimination invoquée' },
    { value: 'RACE_ORIGINE', label: 'Race / origine' },
    { value: 'SEXE', label: 'Sexe' },
    { value: 'GROSSESSE', label: 'Grossesse / maternité' },
    { value: 'SANTE', label: 'État de santé / handicap' },
    { value: 'SYNDICAL', label: 'Activité syndicale / RP' },
    { value: 'AUTRE', label: 'Autre motif discriminatoire L.1132-1' },
  ];

  constructor(
    private service: RupturePeriodeEssaiService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.prefillFromAi();
    if (this.workspaceCountry === 'FRANCE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * Pré-fill IA (SF-DT-38-02) — délègue au helper partagé.
   * 9 champs pré-remplissables depuis l'existant ; pré-fill exhaustif
   * (12 champs supplémentaires) reporté à une SF F-246 dédiée.
   * No-op gracieux si `aiData` absent ou dossier BE.
   */
  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const ai = this.aiData;
    if (!ai) return;

    const rules = RupturePeriodeEssaiSectionPrefillRules;
    const ruleInput = { aiData: ai, workspaceCountry: this.workspaceCountry };

    const tc = rules.computeTypeContrat(ruleInput);
    if (tc && (this.typeContrat() === 'CDI' || this.provenanceTypeContrat() === 'IA')) {
      this.typeContrat.set(tc);
      this.provenanceTypeContrat.set('IA');
    }

    const dd = rules.computeDateDebutContrat(ruleInput);
    if (dd && (this.dateDebutContrat() === null || this.provenanceDateDebutContrat() === 'IA')) {
      this.dateDebutContrat.set(dd);
      this.provenanceDateDebutContrat.set('IA');
    }

    const dr = rules.computeDateRupture(ruleInput);
    if (dr && (this.dateRupture() === null || this.provenanceDateRupture() === 'IA')) {
      this.dateRupture.set(dr);
      this.provenanceDateRupture.set('IA');
    }

    const mi = rules.computeMotifInvoque(ruleInput);
    if (mi && (this.motifInvoque() === null || this.provenanceMotifInvoque() === 'IA')) {
      this.motifInvoque.set(mi);
      this.provenanceMotifInvoque.set('IA');
    }

    const disc = rules.computeDiscriminationInvoquee(ruleInput);
    if (disc && (this.discriminationInvoquee() === null || this.provenanceDiscrimination() === 'IA')) {
      this.discriminationInvoquee.set(disc);
      this.provenanceDiscrimination.set('IA');
    }

    const gr = rules.computeGrossesseAuMomentRupture(ruleInput);
    if (gr !== null) {
      this.grossesseAuMomentRupture.set(gr);
      this.provenanceGrossesse.set('IA');
    }

    const at = rules.computeArretAccidentTravail(ruleInput);
    if (at !== null) {
      this.arretAccidentTravailEnCours.set(at);
      this.provenanceArretAt.set('IA');
    }

    const cc = rules.computeConventionApplicable(ruleInput);
    if (cc !== null) {
      this.conventionCollectiveApplicable.set(cc);
      this.provenanceConventionApplicable.set('IA');
    }

    const sal = rules.computeSalaireMensuelBrut(ruleInput);
    if (sal && (this.salaireMensuelBrut() === null || this.provenanceSalaire() === 'IA')) {
      this.salaireMensuelBrut.set(sal);
      this.provenanceSalaire.set('IA');
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  formValid(): boolean {
    if (this.workspaceCountry !== 'FRANCE') return false;
    if (!this.dateDebutContrat() || !this.dateRupture()) return false;
    if (this.dureePeriodeEssaiContractuelleMois() <= 0) return false;
    return true;
  }

  // --- Handlers : toute modification efface le badge IA ---

  onCategorieChange(v: CategorieSocioProfessionnelle): void { this.categorieSocioProfessionnelle.set(v); }
  onTypeContratChange(v: TypeContratEssai): void {
    this.typeContrat.set(v);
    this.provenanceTypeContrat.set(null);
  }
  onDureeCddChange(v: string | null): void {
    const n = v == null || v === '' ? null : Number(v);
    this.dureeCddMois.set(Number.isFinite(n) ? n as number : null);
  }
  onDateDebutChange(v: string | null): void {
    this.dateDebutContrat.set(v || null);
    this.provenanceDateDebutContrat.set(null);
  }
  onDateRuptureChange(v: string | null): void {
    this.dateRupture.set(v || null);
    this.provenanceDateRupture.set(null);
  }
  onDureeEssaiChange(v: string | null): void {
    const n = v == null || v === '' ? 0 : Number(v);
    this.dureePeriodeEssaiContractuelleMois.set(Number.isFinite(n) ? n as number : 0);
  }
  onRenouvellementChange(v: boolean): void {
    this.renouvellementInvoque.set(v);
    if (!v) {
      this.accordBrancheRenouvellement.set(false);
      this.accordEcritSalarieRenouvellement.set(false);
    }
  }
  onAuteurChange(v: AuteurRupture): void { this.auteurRupture.set(v); }
  onPrevenanceChange(v: string | null): void {
    const n = v == null || v === '' ? null : Number(v);
    this.delaiPrevenanceJoursAppliques.set(Number.isFinite(n) ? n as number : null);
  }
  onMotifInvoqueChange(v: string | null): void {
    this.motifInvoque.set(v || null);
    this.provenanceMotifInvoque.set(null);
  }
  onDiscriminationChange(v: DiscriminationMotif | 'AUCUNE'): void {
    this.discriminationInvoquee.set(v === 'AUCUNE' ? null : v);
    this.provenanceDiscrimination.set(null);
  }
  onGrossesseChange(v: boolean): void {
    this.grossesseAuMomentRupture.set(v);
    this.provenanceGrossesse.set(null);
  }
  onArretAtChange(v: boolean): void {
    this.arretAccidentTravailEnCours.set(v);
    this.provenanceArretAt.set(null);
  }
  onAtteinteLiberteChange(v: string | null): void { this.atteinteLiberteFondamentale.set(v || null); }
  onLettreMotiveeChange(v: boolean): void {
    this.lettreRuptureMotivee.set(v);
    if (!v) this.motifsAveresParPieces.set(false);
  }
  onMotifsAveresChange(v: boolean): void { this.motifsAveresParPieces.set(v); }
  onConventionApplicableChange(v: boolean): void {
    this.conventionCollectiveApplicable.set(v);
    this.provenanceConventionApplicable.set(null);
    if (!v) this.conventionCollectivePlusFavorableRespectee.set(true);
  }
  onConventionRespecteeChange(v: boolean): void { this.conventionCollectivePlusFavorableRespectee.set(v); }
  onSalaireChange(v: string | null): void {
    const n = v == null || v === '' ? null : Number(v);
    this.salaireMensuelBrut.set(Number.isFinite(n) ? n as number : null);
    this.provenanceSalaire.set(null);
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: RupturePeriodeEssaiRequest = {
      categorieSocioProfessionnelle: this.categorieSocioProfessionnelle(),
      typeContrat: this.typeContrat(),
      dureeCddMois: this.dureeCddMois(),
      dateDebutContrat: this.dateDebutContrat()!,
      dateRupture: this.dateRupture()!,
      dureePeriodeEssaiContractuelleMois: this.dureePeriodeEssaiContractuelleMois(),
      renouvellementInvoque: this.renouvellementInvoque(),
      accordBrancheRenouvellement: this.renouvellementInvoque() ? this.accordBrancheRenouvellement() : null,
      accordEcritSalarieRenouvellement: this.renouvellementInvoque() ? this.accordEcritSalarieRenouvellement() : null,
      auteurRupture: this.auteurRupture(),
      delaiPrevenanceJoursAppliques: this.delaiPrevenanceJoursAppliques(),
      motifInvoque: this.motifInvoque()?.trim() || null,
      motifLieAuxCompetencesProfessionnelles: this.motifLieAuxCompetencesProfessionnelles(),
      motifEconomiqueOuOrganisationnel: this.motifEconomiqueOuOrganisationnel(),
      discriminationInvoquee: this.discriminationInvoquee(),
      grossesseAuMomentRupture: this.grossesseAuMomentRupture(),
      arretAccidentTravailEnCours: this.arretAccidentTravailEnCours(),
      atteinteLiberteFondamentale: this.atteinteLiberteFondamentale()?.trim() || null,
      lettreRuptureMotivee: this.lettreRuptureMotivee(),
      motifsAveresParPieces: this.lettreRuptureMotivee() ? this.motifsAveresParPieces() : false,
      conventionCollectiveApplicable: this.conventionCollectiveApplicable(),
      conventionCollectivePlusFavorableRespectee: this.conventionCollectiveApplicable()
        ? this.conventionCollectivePlusFavorableRespectee() : null,
      salaireMensuelBrut: this.salaireMensuelBrut(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.calculating.set(false);
        this.snackBar.open('Qualification de la rupture d\'essai calculée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.calculating.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors du calcul';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.applyResult(r);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
      },
    });
  }

  private applyResult(r: RupturePeriodeEssaiResponse): void {
    this.result.set(r);
    this.hydrateForm(r);
    this.showForm.set(false);
  }

  /** Ré-injecte le snapshot d'inputs de la réponse dans les champs. */
  private hydrateForm(r: RupturePeriodeEssaiResponse): void {
    this.categorieSocioProfessionnelle.set(r.categorieSocioProfessionnelle);
    this.typeContrat.set(r.typeContrat);
    this.dureeCddMois.set(r.dureeCddMois);
    this.dateDebutContrat.set(r.dateDebutContrat);
    this.dateRupture.set(r.dateRupture);
    this.dureePeriodeEssaiContractuelleMois.set(r.dureePeriodeEssaiContractuelleMois);
    this.renouvellementInvoque.set(r.renouvellementInvoque ?? false);
    this.accordBrancheRenouvellement.set(r.accordBrancheRenouvellement ?? false);
    this.accordEcritSalarieRenouvellement.set(r.accordEcritSalarieRenouvellement ?? false);
    this.auteurRupture.set(r.auteurRupture);
    this.delaiPrevenanceJoursAppliques.set(r.delaiPrevenanceJoursAppliques);
    this.motifInvoque.set(r.motifInvoque);
    this.motifLieAuxCompetencesProfessionnelles.set(r.motifLieAuxCompetencesProfessionnelles ?? true);
    this.motifEconomiqueOuOrganisationnel.set(r.motifEconomiqueOuOrganisationnel ?? false);
    this.discriminationInvoquee.set(r.discriminationInvoquee);
    this.grossesseAuMomentRupture.set(r.grossesseAuMomentRupture ?? false);
    this.arretAccidentTravailEnCours.set(r.arretAccidentTravailEnCours ?? false);
    this.atteinteLiberteFondamentale.set(r.atteinteLiberteFondamentale);
    this.lettreRuptureMotivee.set(r.lettreRuptureMotivee ?? false);
    this.motifsAveresParPieces.set(r.motifsAveresParPieces ?? false);
    this.conventionCollectiveApplicable.set(r.conventionCollectiveApplicable ?? false);
    this.conventionCollectivePlusFavorableRespectee.set(r.conventionCollectivePlusFavorableRespectee ?? true);
    this.salaireMensuelBrut.set(r.salaireMensuelBrut);
    // Reset provenance — valeurs persistées = saisie avocat.
    this.provenanceTypeContrat.set(null);
    this.provenanceDateDebutContrat.set(null);
    this.provenanceDateRupture.set(null);
    this.provenanceMotifInvoque.set(null);
    this.provenanceDiscrimination.set(null);
    this.provenanceGrossesse.set(null);
    this.provenanceArretAt.set(null);
    this.provenanceConventionApplicable.set(null);
    this.provenanceSalaire.set(null);
  }

  // ---------------------------------------------------------------------------
  // Helpers d'affichage du résultat
  // ---------------------------------------------------------------------------

  verdictBannerClass(verdict: RupturePeriodeEssaiVerdict): string {
    switch (verdict) {
      case 'REGULIERE': return 'rpe-verdict-banner rpe-verdict-banner--available';
      case 'RISQUE_ABUSIVE': return 'rpe-verdict-banner rpe-verdict-banner--medium';
      case 'NULLE': return 'rpe-verdict-banner rpe-verdict-banner--danger';
      case 'ILLEGALE_REQUALIF_LICENCIEMENT': return 'rpe-verdict-banner rpe-verdict-banner--danger';
    }
  }

  verdictBannerLabel(verdict: RupturePeriodeEssaiVerdict): string {
    switch (verdict) {
      case 'REGULIERE': return 'Rupture régulière';
      case 'RISQUE_ABUSIVE': return 'Risque d\'abus — dommages et intérêts';
      case 'NULLE': return 'Rupture nulle — option réintégration';
      case 'ILLEGALE_REQUALIF_LICENCIEMENT': return 'Rupture illégale — requalification licenciement';
    }
  }

  verdictBannerIcon(verdict: RupturePeriodeEssaiVerdict): string {
    switch (verdict) {
      case 'REGULIERE': return 'check_circle';
      case 'RISQUE_ABUSIVE': return 'warning';
      case 'NULLE': return 'error';
      case 'ILLEGALE_REQUALIF_LICENCIEMENT': return 'error';
    }
  }

  /** Classe CSS du chip gravité d'une anomalie. */
  anomalieGraviteClass(gravite: 'AVERE' | 'PROBABLE'): string {
    return gravite === 'AVERE' ? 'rpe-gravite rpe-gravite--avere' : 'rpe-gravite rpe-gravite--probable';
  }

  anomalieGraviteLabel(gravite: 'AVERE' | 'PROBABLE'): string {
    return gravite === 'AVERE' ? 'Avéré' : 'Probable';
  }

  formatEuros(v: number | null): string {
    if (v == null) return '—';
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'EUR', maximumFractionDigits: 0 }).format(v);
  }
}
