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
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { HarcelementProcedureInterneService } from '../../core/services/harcelement-procedure-interne.service';
import {
  DelaiRaisonnableHarcelement,
  HarcelementProcedureInterneRequest,
  HarcelementProcedureInterneResponse,
  RisqueResponsabiliteEmployeur,
  StatutConformiteHarcelement,
} from '../../core/models/harcelement-procedure-interne.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { HarcelementProcedureInternePrefillRules } from './harcelement-procedure-interne-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-28 — Outil décisionnel « Harcèlement : procédure interne de traitement
 * d'un signalement » (F-DT-59-harcelement-procedure-interne).
 *
 * FRANCE uniquement — checklist de conformité EMPLOYEUR de la procédure interne
 * de traitement d'un signalement de harcèlement (art. L.1153-5-1, L.2314-1,
 * L.1152-4 CT) : désignation d'un référent harcèlement sexuel au CSE (≥ 11
 * salariés) et d'un référent employeur (≥ 250 salariés), information / affichage
 * des salariés, déclenchement d'une enquête interne contradictoire dans un délai
 * raisonnable, mesures conservatoires → verdict de conformité, délai de réaction
 * et risque de responsabilité de l'employeur (obligation de sécurité L.4121-1).
 * Distinct de F-DT-11 (nullité du licenciement consécutif au harcèlement).
 *
 * Verdict de conformité (3 états) :
 *  - CONFORME (vert) : tous les items obligatoires conformes → risque FAIBLE.
 *  - NON_CONFORME (orange) : item obligatoire non conforme (hors enquête) →
 *    risque MODERE.
 *  - CARENCE_GRAVE (rouge) : signalement reçu et enquête non déclenchée / non
 *    contradictoire / délai NON → risque ELEVE (manquement L.4121-1).
 *
 * Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; rouge réservé CARENCE_GRAVE / délai
 *    NON / risque ELEVE ; orange NON_CONFORME / délai LIMITE / risque MODERE ;
 *    vert CONFORME / délai OUI / risque FAIBLE ; JetBrains Mono délai /
 *    itemsObligatoiresManquants / baseJuridique ; bannière gate FR ; MatSnackBar.
 *  - pré-fill IA (effectif, signalementRecu) depuis TravailExtractedData + badge
 *    `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur).
 *  - F-IA-03 : coherenceAlerts (effectif ≥ 11 sans référent CSE ; signalement
 *    reçu sans date d'ouverture d'enquête ; date d'ouverture antérieure au
 *    signalement).
 */
@Component({
  selector: 'app-harcelement-procedure-interne-section',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './harcelement-procedure-interne-section.component.html',
  styleUrl: './harcelement-procedure-interne-section.component.scss',
})
export class HarcelementProcedureInterneSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-59-harcelement-procedure-interne';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'HARCÈLEMENT PROCÉDURE INTERNE (FR)';
  static readonly TOOL_ICON = 'report_problem';

  static getPrefillCount(input: PrefillCountInput): number {
    return HarcelementProcedureInternePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: TravailExtractedData | null;
  @Input() forceExpanded = false;
  @Input() standaloneMode = false;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<HarcelementProcedureInterneResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  effectif = signal<number | null>(null);
  referentCseDesigne = signal<boolean>(false);
  referentEmployeurDesigne = signal<boolean>(false);
  informationAffichageRealisee = signal<boolean>(false);
  signalementRecu = signal<boolean>(false);
  dateSignalement = signal<string | null>(null);
  dateOuvertureEnquete = signal<string | null>(null);
  enqueteContradictoire = signal<boolean>(false);
  mesuresConservatoiresPrises = signal<boolean>(false);

  provenanceEffectif = signal<'IA' | null>(null);
  provenanceSignalement = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) :
   *  - effectif ≥ 11 sans référent CSE harcèlement sexuel ;
   *  - signalement reçu mais aucune date d'ouverture d'enquête renseignée ;
   *  - date d'ouverture de l'enquête antérieure à la date du signalement.
   */
  coherenceAlerts = computed<string[]>(() => {
    const alerts: string[] = [];
    const eff = this.effectif() ?? 0;
    if (eff >= 11 && !this.referentCseDesigne()) {
      alerts.push(
        'L\'effectif est d\'au moins 11 salariés : la désignation d\'un référent harcèlement sexuel au CSE est obligatoire (art. L.2314-1 CT). Il n\'est pas coché.',
      );
    }
    if (this.signalementRecu() && !this.dateOuvertureEnquete()) {
      alerts.push(
        'Un signalement a été reçu mais aucune date d\'ouverture d\'enquête n\'est renseignée : l\'employeur doit diligenter une enquête à réception du signalement (obligation de sécurité L.4121-1).',
      );
    }
    const ds = this.dateSignalement();
    const doe = this.dateOuvertureEnquete();
    if (ds && doe && doe < ds) {
      alerts.push(
        'La date d\'ouverture de l\'enquête est antérieure à la date du signalement : à vérifier (incohérence de chronologie).',
      );
    }
    return alerts;
  });

  constructor(
    private readonly service: HarcelementProcedureInterneService,
    private readonly cdr: ChangeDetectorRef,
    private readonly snackBar: MatSnackBar,
    @Optional() private readonly dashboardRefresh?: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    if (this.standaloneMode) {
      this.collapsed.set(false);
      this.loading.set(false);
      this.showForm.set(true);
      return;
    }
    if (this.isFrance() && this.caseFileId) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) {
      this.collapsed.set(false);
    }
    if (changes['aiData'] && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** Le formulaire est soumettable dès que l'effectif est un entier > 0. */
  formValid(): boolean {
    const eff = this.effectif();
    return eff !== null && Number.isFinite(eff) && eff > 0;
  }

  onEffectifChange(value: number | null): void {
    this.effectif.set(value === null || value === undefined ? null : Number(value));
    this.provenanceEffectif.set(null);
  }

  onReferentCseChange(value: boolean): void {
    this.referentCseDesigne.set(value);
  }

  onReferentEmployeurChange(value: boolean): void {
    this.referentEmployeurDesigne.set(value);
  }

  onInformationAffichageChange(value: boolean): void {
    this.informationAffichageRealisee.set(value);
  }

  onSignalementRecuChange(value: boolean): void {
    this.signalementRecu.set(value);
    this.provenanceSignalement.set(null);
  }

  onDateSignalementChange(value: string | null): void {
    this.dateSignalement.set(value ? value : null);
  }

  onDateOuvertureEnqueteChange(value: string | null): void {
    this.dateOuvertureEnquete.set(value ? value : null);
  }

  onEnqueteContradictoireChange(value: boolean): void {
    this.enqueteContradictoire.set(value);
  }

  onMesuresConservatoiresChange(value: boolean): void {
    this.mesuresConservatoiresPrises.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: HarcelementProcedureInterneRequest = {
      effectif: this.effectif()!,
      referentCseDesigne: this.referentCseDesigne(),
      referentEmployeurDesigne: this.referentEmployeurDesigne(),
      informationAffichageRealisee: this.informationAffichageRealisee(),
      signalementRecu: this.signalementRecu(),
      dateSignalement: this.dateSignalement(),
      dateOuvertureEnquete: this.dateOuvertureEnquete(),
      enqueteContradictoire: this.enqueteContradictoire(),
      mesuresConservatoiresPrises: this.mesuresConservatoiresPrises(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse procédure interne harcèlement enregistrée', 'OK', { duration: 2500 });
        if (!this.standaloneMode) {
          this.dashboardRefresh?.triggerRefresh();
        }
        this.cdr.markForCheck();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || "Erreur lors de l'analyse";
        this.snackBar.open(String(msg), 'Fermer', {
          duration: 5000,
          panelClass: 'snack-error',
        });
        this.cdr.markForCheck();
      },
    });
  }

  // --- labels / helpers -----------------------------------------------------

  statutLabel(s: StatutConformiteHarcelement | null | undefined): string {
    switch (s) {
      case 'CONFORME':      return 'Procédure conforme';
      case 'NON_CONFORME':  return 'Procédure non conforme';
      case 'CARENCE_GRAVE': return 'Carence grave';
      default:              return '';
    }
  }

  statutChipClass(s: StatutConformiteHarcelement | null | undefined): string {
    switch (s) {
      case 'CONFORME':      return 'is-chip is-chip--success';
      case 'NON_CONFORME':  return 'is-chip is-chip--warning';
      case 'CARENCE_GRAVE': return 'is-chip is-chip--danger';
      default:              return 'is-chip';
    }
  }

  bannerClass(s: StatutConformiteHarcelement | null | undefined): string {
    switch (s) {
      case 'CONFORME':      return 'is-banner is-banner--success';
      case 'NON_CONFORME':  return 'is-banner is-banner--warning';
      case 'CARENCE_GRAVE': return 'is-banner is-banner--danger';
      default:              return 'is-banner is-banner--navy';
    }
  }

  bannerIcon(s: StatutConformiteHarcelement | null | undefined): string {
    switch (s) {
      case 'CONFORME':      return 'verified';
      case 'NON_CONFORME':  return 'warning_amber';
      case 'CARENCE_GRAVE': return 'gpp_bad';
      default:              return 'info_outline';
    }
  }

  delaiLabel(d: DelaiRaisonnableHarcelement | null | undefined): string {
    switch (d) {
      case 'OUI':    return 'Délai raisonnable';
      case 'LIMITE': return 'Délai limite';
      case 'NON':    return 'Délai non raisonnable';
      default:       return '';
    }
  }

  delaiChipClass(d: DelaiRaisonnableHarcelement | null | undefined): string {
    switch (d) {
      case 'OUI':    return 'is-chip is-chip--success';
      case 'LIMITE': return 'is-chip is-chip--warning';
      case 'NON':    return 'is-chip is-chip--danger';
      default:       return 'is-chip';
    }
  }

  risqueLabel(r: RisqueResponsabiliteEmployeur | null | undefined): string {
    switch (r) {
      case 'FAIBLE': return 'Risque faible';
      case 'MODERE': return 'Risque modéré';
      case 'ELEVE':  return 'Risque élevé';
      default:       return '';
    }
  }

  risqueChipClass(r: RisqueResponsabiliteEmployeur | null | undefined): string {
    switch (r) {
      case 'FAIBLE': return 'is-chip is-chip--success';
      case 'MODERE': return 'is-chip is-chip--warning';
      case 'ELEVE':  return 'is-chip is-chip--danger';
      default:       return 'is-chip';
    }
  }

  /** Note explicative — obligation de sécurité de l'employeur (L.4121-1). */
  risqueNote(r: RisqueResponsabiliteEmployeur | null | undefined): string {
    switch (r) {
      case 'FAIBLE':
        return 'La procédure interne respecte les obligations de l\'employeur (référents, information, enquête contradictoire) : le risque de mise en cause de sa responsabilité au titre de l\'obligation de sécurité (art. L.4121-1 CT) est faible.';
      case 'MODERE':
        return 'Un ou plusieurs items obligatoires ne sont pas conformes : l\'employeur s\'expose à un risque de mise en cause de sa responsabilité au titre de l\'obligation de prévention (art. L.1152-4, L.4121-1 CT).';
      case 'ELEVE':
        return 'Un signalement a été reçu sans enquête diligentée / contradictoire ou dans un délai déraisonnable : manquement à l\'obligation de sécurité (art. L.4121-1 CT) susceptible d\'engager la responsabilité de l\'employeur.';
      default:
        return '';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const eff = HarcelementProcedureInternePrefillRules.computeEffectif(input);
    if (eff !== null && this.effectif() === null) {
      this.effectif.set(eff);
      this.provenanceEffectif.set('IA');
    }

    const signalement = HarcelementProcedureInternePrefillRules.computeSignalementRecu(input);
    if (signalement !== null && this.provenanceSignalement() === null && !this.signalementRecu()) {
      this.signalementRecu.set(signalement);
      this.provenanceSignalement.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.effectif.set(r.effectif ?? null);
        this.referentCseDesigne.set(r.referentCseDesigne ?? false);
        this.referentEmployeurDesigne.set(r.referentEmployeurDesigne ?? false);
        this.informationAffichageRealisee.set(r.informationAffichageRealisee ?? false);
        this.signalementRecu.set(r.signalementRecu ?? false);
        this.dateSignalement.set(r.dateSignalement ?? null);
        this.dateOuvertureEnquete.set(r.dateOuvertureEnquete ?? null);
        this.enqueteContradictoire.set(r.enqueteContradictoire ?? false);
        this.mesuresConservatoiresPrises.set(r.mesuresConservatoiresPrises ?? false);
        this.provenanceEffectif.set(null);
        this.provenanceSignalement.set(null);
        this.showForm.set(false);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        // 404 = pas encore d'analyse, on tente le pré-fill IA.
        this.prefillFromAi();
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }
}
