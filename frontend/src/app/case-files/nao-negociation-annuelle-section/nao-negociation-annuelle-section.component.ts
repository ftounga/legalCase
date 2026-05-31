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

import { NaoNegociationAnnuelleService } from '../../core/services/nao-negociation-annuelle.service';
import {
  NaoNegociationAnnuelleRequest,
  NaoNegociationAnnuelleResponse,
  RisqueEntrave,
  StatutEcheanceNao,
  StatutNao,
} from '../../core/models/nao-negociation-annuelle.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { NaoNegociationAnnuellePrefillRules } from './nao-negociation-annuelle-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-30 — Outil décisionnel « NAO : négociation annuelle obligatoire »
 * (F-DT-66-nao-negociation-annuelle).
 *
 * FRANCE uniquement — conformité EMPLOYEUR + calculateur de délai de la
 * négociation annuelle obligatoire dans les entreprises pourvues d'un délégué
 * syndical (art. L.2242-1 à L.2242-8 CT) : déclenchement de la négociation sur
 * les blocs obligatoires (rémunération / temps de travail / partage de la valeur
 * L.2242-15 ; égalité professionnelle F/H et QVT L.2242-17), respect de la
 * périodicité (annuelle, ou jusqu'à 4 ans par accord de méthode L.2242-11),
 * établissement d'un PV de désaccord, exposition aux sanctions (pénalité égalité
 * F/H, délit d'entrave L.2243-2). Distinct de F-DT-67 (validité de l'accord issu
 * de la NAO).
 *
 * Verdict de conformité (3 états) :
 *  - CONFORME (vert) : applicable + tous items obligatoires conformes + échéance
 *    non dépassée → risque FAIBLE.
 *  - NON_CONFORME (orange) : item obligatoire non conforme ou échéance dépassée →
 *    risque MODERE, ou ELEVE si blocs non négociés (délit d'entrave).
 *  - NON_APPLICABLE (gris) : pas de délégué syndical → pas d'obligation, checklist
 *    masquée.
 *
 * Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; rouge réservé NON_CONFORME / échéance
 *    DEPASSEE / risque ELEVE ; orange ECHEANCE_PROCHE / risque MODERE ; gris
 *    NON_APPLICABLE ; vert CONFORME / A_JOUR / risque FAIBLE ; JetBrains Mono
 *    échéance / baseJuridique ; bannière gate FR ; MatSnackBar.
 *  - pré-fill IA (effectif, delegueSyndicalPresent) depuis TravailExtractedData
 *    + badge `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur).
 *  - F-IA-03 : coherenceAlerts (DS présent sans bloc négocié ; périodicité > 12
 *    sans accord de méthode ; négociation non aboutie sans PV de désaccord).
 */
@Component({
  selector: 'app-nao-negociation-annuelle-section',
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
  templateUrl: './nao-negociation-annuelle-section.component.html',
  styleUrl: './nao-negociation-annuelle-section.component.scss',
})
export class NaoNegociationAnnuelleSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-66-nao-negociation-annuelle';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'NAO NÉGOCIATION ANNUELLE (FR)';
  static readonly TOOL_ICON = 'handshake';

  static getPrefillCount(input: PrefillCountInput): number {
    return NaoNegociationAnnuellePrefillRules.computePrefillCount(input);
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
  result = signal<NaoNegociationAnnuelleResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  effectif = signal<number | null>(null);
  delegueSyndicalPresent = signal<boolean>(false);
  blocRemunerationNegocie = signal<boolean>(false);
  blocEgaliteQvtNegocie = signal<boolean>(false);
  accordMethodePeriodicite = signal<boolean>(false);
  dateDerniereNegociation = signal<string | null>(null);
  periodiciteMois = signal<number | null>(12);
  pvDesaccordEtabli = signal<boolean>(false);
  negociationAboutie = signal<boolean>(false);

  provenanceEffectif = signal<'IA' | null>(null);
  provenanceDelegueSyndical = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) :
   *  - délégué syndical présent mais aucun bloc obligatoire négocié ;
   *  - périodicité > 12 mois sans accord de méthode ;
   *  - négociation non aboutie sans PV de désaccord établi.
   */
  coherenceAlerts = computed<string[]>(() => {
    const alerts: string[] = [];
    const ds = this.delegueSyndicalPresent();
    if (ds && !this.blocRemunerationNegocie() && !this.blocEgaliteQvtNegocie()) {
      alerts.push(
        'Un délégué syndical est présent mais aucun bloc obligatoire de la NAO n\'est négocié : l\'employeur doit engager chaque année la négociation (art. L.2242-1 CT), à défaut délit d\'entrave (L.2243-2).',
      );
    }
    const p = this.periodiciteMois();
    if (p !== null && p > 12 && !this.accordMethodePeriodicite()) {
      alerts.push(
        'La périodicité retenue dépasse 12 mois sans accord de méthode : sans accord, la négociation doit être engagée chaque année (art. L.2242-11 CT).',
      );
    }
    if (ds && !this.negociationAboutie() && !this.pvDesaccordEtabli()) {
      alerts.push(
        'La négociation n\'a pas abouti à un accord et aucun PV de désaccord n\'est établi : un PV de désaccord doit consigner les propositions des parties (art. L.2242-5 CT).',
      );
    }
    return alerts;
  });

  constructor(
    private readonly service: NaoNegociationAnnuelleService,
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

  /**
   * Le formulaire est soumettable dès que l'effectif est un entier > 0 et que la
   * périodicité est un entier dans [1 ; 48].
   */
  formValid(): boolean {
    const eff = this.effectif();
    if (eff === null || !Number.isFinite(eff) || eff <= 0) return false;
    const p = this.periodiciteMois();
    if (p === null || !Number.isFinite(p) || p < 1 || p > 48) return false;
    return true;
  }

  onEffectifChange(value: number | null): void {
    this.effectif.set(value === null || value === undefined ? null : Number(value));
    this.provenanceEffectif.set(null);
  }

  onDelegueSyndicalChange(value: boolean): void {
    this.delegueSyndicalPresent.set(value);
    this.provenanceDelegueSyndical.set(null);
  }

  onBlocRemunerationChange(value: boolean): void {
    this.blocRemunerationNegocie.set(value);
  }

  onBlocEgaliteQvtChange(value: boolean): void {
    this.blocEgaliteQvtNegocie.set(value);
  }

  onAccordMethodeChange(value: boolean): void {
    this.accordMethodePeriodicite.set(value);
  }

  onDateDerniereNegociationChange(value: string | null): void {
    this.dateDerniereNegociation.set(value ? value : null);
  }

  onPeriodiciteMoisChange(value: number | null): void {
    this.periodiciteMois.set(value === null || value === undefined ? null : Number(value));
  }

  onPvDesaccordChange(value: boolean): void {
    this.pvDesaccordEtabli.set(value);
  }

  onNegociationAboutieChange(value: boolean): void {
    this.negociationAboutie.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: NaoNegociationAnnuelleRequest = {
      effectif: this.effectif()!,
      delegueSyndicalPresent: this.delegueSyndicalPresent(),
      blocRemunerationNegocie: this.blocRemunerationNegocie(),
      blocEgaliteQvtNegocie: this.blocEgaliteQvtNegocie(),
      accordMethodePeriodicite: this.accordMethodePeriodicite(),
      dateDerniereNegociation: this.dateDerniereNegociation(),
      periodiciteMois: this.periodiciteMois()!,
      pvDesaccordEtabli: this.pvDesaccordEtabli(),
      negociationAboutie: this.negociationAboutie(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse NAO enregistrée', 'OK', { duration: 2500 });
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

  statutLabel(s: StatutNao | null | undefined): string {
    switch (s) {
      case 'CONFORME':       return 'NAO conforme';
      case 'NON_CONFORME':   return 'NAO non conforme';
      case 'NON_APPLICABLE': return 'NAO non applicable';
      default:               return '';
    }
  }

  statutChipClass(s: StatutNao | null | undefined): string {
    switch (s) {
      case 'CONFORME':       return 'is-chip is-chip--success';
      case 'NON_CONFORME':   return 'is-chip is-chip--danger';
      case 'NON_APPLICABLE': return 'is-chip is-chip--neutral';
      default:               return 'is-chip';
    }
  }

  bannerClass(s: StatutNao | null | undefined): string {
    switch (s) {
      case 'CONFORME':       return 'is-banner is-banner--success';
      case 'NON_CONFORME':   return 'is-banner is-banner--danger';
      case 'NON_APPLICABLE': return 'is-banner is-banner--navy';
      default:               return 'is-banner is-banner--navy';
    }
  }

  bannerIcon(s: StatutNao | null | undefined): string {
    switch (s) {
      case 'CONFORME':       return 'verified';
      case 'NON_CONFORME':   return 'gpp_bad';
      case 'NON_APPLICABLE': return 'info_outline';
      default:               return 'info_outline';
    }
  }

  echeanceLabel(e: StatutEcheanceNao | null | undefined): string {
    switch (e) {
      case 'A_JOUR':          return 'À jour';
      case 'ECHEANCE_PROCHE': return 'Échéance proche';
      case 'DEPASSEE':        return 'Échéance dépassée';
      default:                return '';
    }
  }

  echeanceChipClass(e: StatutEcheanceNao | null | undefined): string {
    switch (e) {
      case 'A_JOUR':          return 'is-chip is-chip--success';
      case 'ECHEANCE_PROCHE': return 'is-chip is-chip--warning';
      case 'DEPASSEE':        return 'is-chip is-chip--danger';
      default:                return 'is-chip';
    }
  }

  risqueLabel(r: RisqueEntrave | null | undefined): string {
    switch (r) {
      case 'FAIBLE': return 'Risque faible';
      case 'MODERE': return 'Risque modéré';
      case 'ELEVE':  return 'Risque élevé';
      default:       return '';
    }
  }

  risqueChipClass(r: RisqueEntrave | null | undefined): string {
    switch (r) {
      case 'FAIBLE': return 'is-chip is-chip--success';
      case 'MODERE': return 'is-chip is-chip--warning';
      case 'ELEVE':  return 'is-chip is-chip--danger';
      default:       return 'is-chip';
    }
  }

  /** Note explicative — délit d'entrave + pénalité égalité F/H. */
  risqueNote(r: RisqueEntrave | null | undefined): string {
    switch (r) {
      case 'FAIBLE':
        return 'La négociation annuelle obligatoire est conforme (blocs négociés, périodicité respectée, échéance non dépassée) : le risque de sanction est faible.';
      case 'MODERE':
        return 'Un ou plusieurs items obligatoires ne sont pas conformes, ou l\'échéance de négociation est dépassée : l\'employeur s\'expose à un risque de mise en cause au titre de l\'obligation de négocier (art. L.2242-1 CT).';
      case 'ELEVE':
        return 'Les blocs obligatoires de la NAO n\'ont pas été engagés alors qu\'un délégué syndical est présent : délit d\'entrave (art. L.2243-2 CT) et pénalité financière en matière d\'égalité professionnelle F/H pouvant atteindre 1 % de la masse salariale.';
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

    const eff = NaoNegociationAnnuellePrefillRules.computeEffectif(input);
    if (eff !== null && this.effectif() === null) {
      this.effectif.set(eff);
      this.provenanceEffectif.set('IA');
    }

    const ds = NaoNegociationAnnuellePrefillRules.computeDelegueSyndicalPresent(input);
    if (ds !== null && this.provenanceDelegueSyndical() === null && !this.delegueSyndicalPresent()) {
      this.delegueSyndicalPresent.set(ds);
      this.provenanceDelegueSyndical.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.effectif.set(r.effectif ?? null);
        this.delegueSyndicalPresent.set(r.delegueSyndicalPresent ?? false);
        this.blocRemunerationNegocie.set(r.blocRemunerationNegocie ?? false);
        this.blocEgaliteQvtNegocie.set(r.blocEgaliteQvtNegocie ?? false);
        this.accordMethodePeriodicite.set(r.accordMethodePeriodicite ?? false);
        this.dateDerniereNegociation.set(r.dateDerniereNegociation ?? null);
        this.periodiciteMois.set(r.periodiciteMois ?? 12);
        this.pvDesaccordEtabli.set(r.pvDesaccordEtabli ?? false);
        this.negociationAboutie.set(r.negociationAboutie ?? false);
        this.provenanceEffectif.set(null);
        this.provenanceDelegueSyndical.set(null);
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
