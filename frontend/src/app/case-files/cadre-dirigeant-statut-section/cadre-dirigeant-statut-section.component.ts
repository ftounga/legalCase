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

import { CadreDirigeantStatutService } from '../../core/services/cadre-dirigeant-statut.service';
import {
  CadreDirigeantStatutRequest,
  CadreDirigeantStatutResponse,
  ExclusionDureeTravail,
  Qualification,
  RisqueRappelHeuresSupp,
} from '../../core/models/cadre-dirigeant-statut.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { CadreDirigeantStatutPrefillRules } from './cadre-dirigeant-statut-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-20 — Outil décisionnel « Cadre dirigeant : qualification (3 critères
 * cumulatifs) » (F-DT-107-cadre-dirigeant-statut).
 *
 * FRANCE uniquement — applique l'art. L.3111-2 CT et la jurisprudence de la
 * Cour de cassation : qualification de cadre dirigeant subordonnée à trois
 * critères cumulatifs (grande indépendance dans l'organisation de l'emploi du
 * temps ; habilitation à prendre des décisions de façon largement autonome ;
 * rémunération parmi les plus élevées de l'entreprise), renforcée par l'indice
 * de participation effective à la direction (Cass. soc. post-2012) → verdict de
 * qualification, exclusion (ou non) des règles de durée du travail et niveau de
 * risque de rappel d'heures supplémentaires en cas de requalification.
 *
 * Qualification (3 états) :
 *  - CADRE_DIRIGEANT (vert) : 3 critères + participation → exclusion confirmée.
 *  - CADRE_DIRIGEANT_FRAGILE (orange) : 3 critères sans participation effective.
 *  - NON_CADRE_DIRIGEANT (rouge) : < 3 critères → soumis à la durée du travail.
 *
 * Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; rouge réservé NON_CADRE_DIRIGEANT /
 *    risque ELEVE ; orange FRAGILE / risque MODERE ; vert CADRE_DIRIGEANT /
 *    risque FAIBLE ; JetBrains Mono critères (x/3) / baseJuridique ; bannière
 *    gate FR ; MatSnackBar.
 *  - pré-fill IA (participationDirectionEntreprise, niveauRemunerationConstate)
 *    depuis TravailExtractedData + badge `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur).
 *  - F-IA-03 : coherenceAlerts (3 critères remplis sans participation effective ;
 *    niveau de rémunération saisi sans cocher le critère 3).
 */
@Component({
  selector: 'app-cadre-dirigeant-statut-section',
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
  templateUrl: './cadre-dirigeant-statut-section.component.html',
  styleUrl: './cadre-dirigeant-statut-section.component.scss',
})
export class CadreDirigeantStatutSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-107-cadre-dirigeant-statut';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'STATUT CADRE DIRIGEANT (FR)';
  static readonly TOOL_ICON = 'workspace_premium';

  static getPrefillCount(input: PrefillCountInput): number {
    return CadreDirigeantStatutPrefillRules.computePrefillCount(input);
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
  result = signal<CadreDirigeantStatutResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  independanceEmploiDuTemps = signal<boolean>(false);
  autonomieDecision = signal<boolean>(false);
  remunerationParmiPlusElevees = signal<boolean>(false);
  participationDirectionEntreprise = signal<boolean>(false);
  niveauRemunerationConstate = signal<number | null>(null);

  provenanceParticipation = signal<'IA' | null>(null);
  provenanceNiveauRemuneration = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Nombre de critères cumulatifs cochés (estimation locale pour la jauge). */
  criteresRemplisLocal = computed<number>(() => {
    let n = 0;
    if (this.independanceEmploiDuTemps()) n++;
    if (this.autonomieDecision()) n++;
    if (this.remunerationParmiPlusElevees()) n++;
    return n;
  });

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) : qualification fragile
   * (3 critères sans participation effective) ; niveau de rémunération saisi
   * alors que le critère 3 n'est pas coché.
   */
  coherenceAlerts = computed<string[]>(() => {
    const alerts: string[] = [];
    if (this.criteresRemplisLocal() === 3 && !this.participationDirectionEntreprise()) {
      alerts.push(
        'Les 3 critères légaux sont réunis mais la participation effective à la direction n\'est pas cochée : la jurisprudence post-2012 l\'exige → qualification fragile, risque de requalification.',
      );
    }
    if ((this.niveauRemunerationConstate() ?? 0) > 0 && !this.remunerationParmiPlusElevees()) {
      alerts.push(
        'Un niveau de rémunération est saisi alors que le critère « rémunération parmi les plus élevées de l\'entreprise » n\'est pas coché : à vérifier au regard des systèmes de rémunération de l\'entreprise.',
      );
    }
    return alerts;
  });

  constructor(
    private readonly service: CadreDirigeantStatutService,
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

  /** Le formulaire est toujours soumettable : les 3 critères + participation sont booléens. */
  formValid(): boolean {
    if ((this.niveauRemunerationConstate() ?? 0) < 0) return false;
    return true;
  }

  onIndependanceChange(value: boolean): void {
    this.independanceEmploiDuTemps.set(value);
  }

  onAutonomieChange(value: boolean): void {
    this.autonomieDecision.set(value);
  }

  onRemunerationParmiPlusEleveesChange(value: boolean): void {
    this.remunerationParmiPlusElevees.set(value);
  }

  onParticipationChange(value: boolean): void {
    this.participationDirectionEntreprise.set(value);
    this.provenanceParticipation.set(null);
  }

  onNiveauRemunerationChange(value: number | null): void {
    this.niveauRemunerationConstate.set(value === null || value === undefined ? null : Number(value));
    this.provenanceNiveauRemuneration.set(null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: CadreDirigeantStatutRequest = {
      independanceEmploiDuTemps: this.independanceEmploiDuTemps(),
      autonomieDecision: this.autonomieDecision(),
      remunerationParmiPlusElevees: this.remunerationParmiPlusElevees(),
      participationDirectionEntreprise: this.participationDirectionEntreprise(),
      niveauRemunerationConstate: this.niveauRemunerationConstate(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse statut cadre dirigeant enregistrée', 'OK', { duration: 2500 });
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

  qualificationLabel(q: Qualification | null | undefined): string {
    switch (q) {
      case 'CADRE_DIRIGEANT':         return 'Cadre dirigeant';
      case 'CADRE_DIRIGEANT_FRAGILE': return 'Cadre dirigeant (fragile)';
      case 'NON_CADRE_DIRIGEANT':     return 'Non cadre dirigeant';
      default:                        return '';
    }
  }

  qualificationChipClass(q: Qualification | null | undefined): string {
    switch (q) {
      case 'CADRE_DIRIGEANT':         return 'is-chip is-chip--success';
      case 'CADRE_DIRIGEANT_FRAGILE': return 'is-chip is-chip--warning';
      case 'NON_CADRE_DIRIGEANT':     return 'is-chip is-chip--danger';
      default:                        return 'is-chip';
    }
  }

  bannerClass(q: Qualification | null | undefined): string {
    switch (q) {
      case 'CADRE_DIRIGEANT':         return 'is-banner is-banner--success';
      case 'CADRE_DIRIGEANT_FRAGILE': return 'is-banner is-banner--warning';
      case 'NON_CADRE_DIRIGEANT':     return 'is-banner is-banner--danger';
      default:                        return 'is-banner is-banner--navy';
    }
  }

  bannerIcon(q: Qualification | null | undefined): string {
    switch (q) {
      case 'CADRE_DIRIGEANT':         return 'verified';
      case 'CADRE_DIRIGEANT_FRAGILE': return 'warning_amber';
      case 'NON_CADRE_DIRIGEANT':     return 'block';
      default:                        return 'info_outline';
    }
  }

  exclusionLabel(e: ExclusionDureeTravail | null | undefined): string {
    switch (e) {
      case 'EXCLU':     return 'Exclu des règles de durée du travail';
      case 'NON_EXCLU': return 'Soumis aux règles de durée du travail';
      default:          return '';
    }
  }

  exclusionChipClass(e: ExclusionDureeTravail | null | undefined): string {
    switch (e) {
      case 'EXCLU':     return 'is-chip is-chip--success';
      case 'NON_EXCLU': return 'is-chip is-chip--danger';
      default:          return 'is-chip';
    }
  }

  risqueLabel(r: RisqueRappelHeuresSupp | null | undefined): string {
    switch (r) {
      case 'FAIBLE': return 'Risque faible';
      case 'MODERE': return 'Risque modéré';
      case 'ELEVE':  return 'Risque élevé';
      default:       return '';
    }
  }

  risqueChipClass(r: RisqueRappelHeuresSupp | null | undefined): string {
    switch (r) {
      case 'FAIBLE': return 'is-chip is-chip--success';
      case 'MODERE': return 'is-chip is-chip--warning';
      case 'ELEVE':  return 'is-chip is-chip--danger';
      default:       return 'is-chip';
    }
  }

  /** Note explicative du risque de rappel d'heures supplémentaires. */
  risqueNote(r: RisqueRappelHeuresSupp | null | undefined): string {
    switch (r) {
      case 'FAIBLE':
        return 'Le salarié étant qualifié de cadre dirigeant, il est exclu des dispositions sur la durée du travail : le risque de rappel d\'heures supplémentaires est faible.';
      case 'MODERE':
        return 'La qualification est fragile (participation effective à la direction non établie) : en cas de requalification, un rappel d\'heures supplémentaires peut être réclamé.';
      case 'ELEVE':
        return 'Le salarié n\'est pas cadre dirigeant : il reste soumis aux règles de durée du travail et un rappel d\'heures supplémentaires est possible.';
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

    const participation = CadreDirigeantStatutPrefillRules.computeParticipationDirection(input);
    if (participation !== null && this.provenanceParticipation() === null && !this.participationDirectionEntreprise()) {
      this.participationDirectionEntreprise.set(participation);
      this.provenanceParticipation.set('IA');
    }

    const niveau = CadreDirigeantStatutPrefillRules.computeNiveauRemuneration(input);
    if (niveau !== null && this.niveauRemunerationConstate() === null) {
      this.niveauRemunerationConstate.set(niveau);
      this.provenanceNiveauRemuneration.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.independanceEmploiDuTemps.set(r.independanceEmploiDuTemps ?? false);
        this.autonomieDecision.set(r.autonomieDecision ?? false);
        this.remunerationParmiPlusElevees.set(r.remunerationParmiPlusElevees ?? false);
        this.participationDirectionEntreprise.set(r.participationDirectionEntreprise ?? false);
        this.niveauRemunerationConstate.set(r.niveauRemunerationConstate ?? null);
        this.provenanceParticipation.set(null);
        this.provenanceNiveauRemuneration.set(null);
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
