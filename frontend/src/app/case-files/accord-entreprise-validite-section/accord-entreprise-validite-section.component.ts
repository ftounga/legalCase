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
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';

import { AccordEntrepriseValiditeService } from '../../core/services/accord-entreprise-validite.service';
import {
  AccordConditionMajorite,
  AccordEntrepriseValiditeRequest,
  AccordEntrepriseValiditeResponse,
  AccordEntrepriseValiditeStatut,
  AccordTypeOperation,
} from '../../core/models/accord-entreprise-validite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { AccordEntrepriseValiditePrefillRules } from './accord-entreprise-validite-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-32 — Outil décisionnel « Accord d'entreprise : validité (conditions de
 * majorité) » (F-DT-67-accord-entreprise-validite).
 *
 * FRANCE uniquement — analyseur de validité d'un accord d'entreprise au regard
 * des conditions de majorité (art. L.2232-12 CT) et, selon l'opération, des
 * conditions de révision (parties habilitées, L.2261-7) ou de dénonciation
 * (préavis 3 mois + survie 12 mois, L.2261-9 à L.2261-11). Distinct de F-DT-66
 * (NAO : conformité de la négociation annuelle obligatoire).
 *
 * Condition de majorité (3 états) :
 *  - MAJORITE_50 (vert) : signataires > 50 % des suffrages exprimés au 1er tour.
 *  - REFERENDUM_30 (orange) : signataires ∈ [30 % ; 50 %[ + référendum approuvé.
 *  - INSUFFISANTE (rouge) : signataires < 30 %, ou ∈ [30 % ; 50 %[ sans référendum.
 *
 * Verdict de validité (3 états) :
 *  - VALIDE (vert) : majorité remplie + item d'opération satisfait.
 *  - VALIDE_SOUS_RESERVE (orange) : majorité atteinte par référendum.
 *  - NON_VALIDE (rouge) : majorité non remplie ou item d'opération non satisfait.
 *
 * Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; rouge réservé INSUFFISANTE / NON_VALIDE ;
 *    orange REFERENDUM_30 / VALIDE_SOUS_RESERVE ; vert MAJORITE_50 / VALIDE ;
 *    JetBrains Mono pourcentage / dateFinSurvie / baseJuridique ; bannière gate FR ;
 *    MatSnackBar (pas de refresh dashboard requis — calcul sans action validée).
 *  - pré-fill IA (pourcentageSuffragesSignataires, typeOperation) depuis
 *    TravailExtractedData + badge `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur).
 *  - F-IA-03 : coherenceAlerts (référendum approuvé sans référendum organisé ;
 *    révision sans parties habilitées ; dénonciation sans date de dénonciation).
 */
@Component({
  selector: 'app-accord-entreprise-validite-section',
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
    MatSelectModule,
    MatProgressSpinnerModule,
    ToolJurisprudenceCitationsComponent,
  ],
  templateUrl: './accord-entreprise-validite-section.component.html',
  styleUrl: './accord-entreprise-validite-section.component.scss',
})
export class AccordEntrepriseValiditeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-67-accord-entreprise-validite';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'ACCORD ENTREPRISE — VALIDITÉ (FR)';
  static readonly TOOL_ICON = 'gavel';

  static getPrefillCount(input: PrefillCountInput): number {
    return AccordEntrepriseValiditePrefillRules.computePrefillCount(input);
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
  result = signal<AccordEntrepriseValiditeResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  pourcentageSuffragesSignataires = signal<number | null>(null);
  referendumOrganise = signal<boolean>(false);
  referendumApprouve = signal<boolean>(false);
  typeOperation = signal<AccordTypeOperation>('CONCLUSION');
  signePartiesHabilitees = signal<boolean>(false);
  preavisDenonciationRespecte = signal<boolean>(true);
  dateDenonciation = signal<string | null>(null);

  provenancePourcentage = signal<'IA' | null>(null);
  provenanceTypeOperation = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');
  isRevision = computed<boolean>(() => this.typeOperation() === 'REVISION');
  isDenonciation = computed<boolean>(() => this.typeOperation() === 'DENONCIATION');

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) :
   *  - référendum approuvé déclaré sans référendum organisé ;
   *  - révision sans parties habilitées cochées ;
   *  - dénonciation sans date de dénonciation renseignée (fin de survie non calculable).
   */
  coherenceAlerts = computed<string[]>(() => {
    const alerts: string[] = [];
    if (this.referendumApprouve() && !this.referendumOrganise()) {
      alerts.push(
        'Le référendum est déclaré approuvé alors qu\'aucun référendum n\'est marqué comme organisé : la validation par référendum suppose qu\'un référendum des salariés ait été organisé (art. L.2232-12 CT).',
      );
    }
    if (this.isRevision() && !this.signePartiesHabilitees()) {
      alerts.push(
        'Opération de révision sans confirmation que l\'avenant est signé par les parties habilitées : la révision d\'un accord ne peut être engagée que par les signataires ou syndicats habilités (art. L.2261-7 CT).',
      );
    }
    if (this.isDenonciation() && !this.dateDenonciation()) {
      alerts.push(
        'Opération de dénonciation sans date de dénonciation : la date est nécessaire pour calculer la fin de survie de l\'accord (préavis 3 mois + survie 12 mois, art. L.2261-9 à L.2261-11 CT).',
      );
    }
    return alerts;
  });

  constructor(
    private readonly service: AccordEntrepriseValiditeService,
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
   * Le formulaire est soumettable dès que le % de suffrages est un nombre dans
   * [0 ; 100].
   */
  formValid(): boolean {
    const p = this.pourcentageSuffragesSignataires();
    if (p === null || !Number.isFinite(p) || p < 0 || p > 100) return false;
    return true;
  }

  onPourcentageChange(value: number | null): void {
    this.pourcentageSuffragesSignataires.set(
      value === null || value === undefined ? null : Number(value));
    this.provenancePourcentage.set(null);
  }

  onTypeOperationChange(value: AccordTypeOperation): void {
    this.typeOperation.set(value);
    this.provenanceTypeOperation.set(null);
  }

  onReferendumOrganiseChange(value: boolean): void {
    this.referendumOrganise.set(value);
  }

  onReferendumApprouveChange(value: boolean): void {
    this.referendumApprouve.set(value);
  }

  onSignePartiesHabiliteesChange(value: boolean): void {
    this.signePartiesHabilitees.set(value);
  }

  onPreavisDenonciationChange(value: boolean): void {
    this.preavisDenonciationRespecte.set(value);
  }

  onDateDenonciationChange(value: string | null): void {
    this.dateDenonciation.set(value ? value : null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: AccordEntrepriseValiditeRequest = {
      pourcentageSuffragesSignataires: this.pourcentageSuffragesSignataires()!,
      referendumOrganise: this.referendumOrganise(),
      referendumApprouve: this.referendumApprouve(),
      typeOperation: this.typeOperation(),
      signePartiesHabilitees: this.signePartiesHabilitees(),
      preavisDenonciationRespecte: this.preavisDenonciationRespecte(),
      dateDenonciation: this.dateDenonciation(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse de validité de l\'accord enregistrée', 'OK', { duration: 2500 });
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

  typeOperationLabel(t: AccordTypeOperation | null | undefined): string {
    switch (t) {
      case 'CONCLUSION':   return 'Conclusion';
      case 'REVISION':     return 'Révision';
      case 'DENONCIATION': return 'Dénonciation';
      default:             return '';
    }
  }

  conditionMajoriteLabel(c: AccordConditionMajorite | null | undefined): string {
    switch (c) {
      case 'MAJORITE_50':   return 'Majorité > 50 %';
      case 'REFERENDUM_30': return 'Référendum (≥ 30 %)';
      case 'INSUFFISANTE':  return 'Majorité insuffisante';
      default:              return '';
    }
  }

  conditionMajoriteChipClass(c: AccordConditionMajorite | null | undefined): string {
    switch (c) {
      case 'MAJORITE_50':   return 'is-chip is-chip--success';
      case 'REFERENDUM_30': return 'is-chip is-chip--warning';
      case 'INSUFFISANTE':  return 'is-chip is-chip--danger';
      default:              return 'is-chip';
    }
  }

  statutLabel(s: AccordEntrepriseValiditeStatut | null | undefined): string {
    switch (s) {
      case 'VALIDE':              return 'Accord valide';
      case 'VALIDE_SOUS_RESERVE': return 'Valide sous réserve';
      case 'NON_VALIDE':          return 'Accord non valide';
      default:                    return '';
    }
  }

  statutChipClass(s: AccordEntrepriseValiditeStatut | null | undefined): string {
    switch (s) {
      case 'VALIDE':              return 'is-chip is-chip--success';
      case 'VALIDE_SOUS_RESERVE': return 'is-chip is-chip--warning';
      case 'NON_VALIDE':          return 'is-chip is-chip--danger';
      default:                    return 'is-chip';
    }
  }

  bannerClass(s: AccordEntrepriseValiditeStatut | null | undefined): string {
    switch (s) {
      case 'VALIDE':              return 'is-banner is-banner--success';
      case 'VALIDE_SOUS_RESERVE': return 'is-banner is-banner--warning';
      case 'NON_VALIDE':          return 'is-banner is-banner--danger';
      default:                    return 'is-banner is-banner--navy';
    }
  }

  bannerIcon(s: AccordEntrepriseValiditeStatut | null | undefined): string {
    switch (s) {
      case 'VALIDE':              return 'verified';
      case 'VALIDE_SOUS_RESERVE': return 'gpp_maybe';
      case 'NON_VALIDE':          return 'gpp_bad';
      default:                    return 'info_outline';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const pct = AccordEntrepriseValiditePrefillRules.computePourcentageSignataires(input);
    if (pct !== null && this.pourcentageSuffragesSignataires() === null) {
      this.pourcentageSuffragesSignataires.set(pct);
      this.provenancePourcentage.set('IA');
    }

    const op = AccordEntrepriseValiditePrefillRules.computeTypeOperation(input);
    if (op !== null && this.provenanceTypeOperation() === null && this.typeOperation() === 'CONCLUSION') {
      this.typeOperation.set(op);
      this.provenanceTypeOperation.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.pourcentageSuffragesSignataires.set(r.pourcentageSuffragesSignataires ?? null);
        this.referendumOrganise.set(r.referendumOrganise ?? false);
        this.referendumApprouve.set(r.referendumApprouve ?? false);
        this.typeOperation.set(r.typeOperation ?? 'CONCLUSION');
        this.dateDenonciation.set(r.dateDenonciation ?? null);
        this.provenancePourcentage.set(null);
        this.provenanceTypeOperation.set(null);
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
