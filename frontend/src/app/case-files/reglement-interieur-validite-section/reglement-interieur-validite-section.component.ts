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

import { ReglementInterieurValiditeService } from '../../core/services/reglement-interieur-validite.service';
import {
  ReglementInterieurChecklistType,
  ReglementInterieurOpposabilite,
  ReglementInterieurValiditeRequest,
  ReglementInterieurValiditeResponse,
  ReglementInterieurValiditeStatut,
} from '../../core/models/reglement-interieur-validite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { ReglementInterieurValiditePrefillRules } from './reglement-interieur-validite-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-36 — Outil décisionnel « Règlement intérieur : validité »
 * (F-DT-100-reglement-interieur-validite).
 *
 * FRANCE uniquement — analyseur de conformité et d'opposabilité d'un règlement
 * intérieur (art. L.1311-1 à L.1322-4, L.1321-1 et s. CT) : contrôle du contenu
 * obligatoire (hygiène/sécurité, discipline, droits de la défense, harcèlement —
 * L.1321-1 / L.1321-2), de l'absence de clauses interdites (atteinte aux libertés
 * non justifiée/proportionnée, sanction pécuniaire — L.1321-3 / L.1331-2) et du
 * respect de la procédure de mise en place (consultation CSE, transmission
 * inspection, dépôt greffe CPH — L.1321-4) conditionnant l'opposabilité aux
 * salariés. Distinct de la validité d'une sanction disciplinaire fondée sur le RI
 * et du lanceur d'alerte (F-DT-61).
 *
 * Verdict de validité (4 états) :
 *  - NON_REQUIS (gris) : effectif < 50 et aucun RI existant → RI facultatif.
 *  - CONFORME (vert) : contenu obligatoire présent, aucune clause interdite,
 *    procédure de mise en place respectée → OPPOSABLE.
 *  - NON_CONFORME (orange) : un contenu obligatoire manque ou une clause interdite
 *    est présente.
 *  - INOPPOSABLE (rouge) : procédure de mise en place non respectée → RI
 *    inopposable aux salariés.
 *
 * Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; rouge réservé statut INOPPOSABLE /
 *    opposabilité INOPPOSABLE ; orange NON_CONFORME ; gris NON_REQUIS ; vert
 *    CONFORME / OPPOSABLE ; JetBrains Mono compteurs / baseJuridique ; bannière
 *    gate FR ; MatSnackBar.
 *  - pré-fill IA (effectif, reglementExiste) depuis TravailExtractedData + badge
 *    `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur).
 *  - F-IA-03 : coherenceAlerts (RI existant sans consultation CSE ; effectif ≥ 50
 *    sans RI ; clause interdite stipulée).
 */
@Component({
  selector: 'app-reglement-interieur-validite-section',
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
  templateUrl: './reglement-interieur-validite-section.component.html',
  styleUrl: './reglement-interieur-validite-section.component.scss',
})
export class ReglementInterieurValiditeSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-100-reglement-interieur-validite';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'RÈGLEMENT INTÉRIEUR — VALIDITÉ (FR)';
  static readonly TOOL_ICON = 'rule';

  static getPrefillCount(input: PrefillCountInput): number {
    return ReglementInterieurValiditePrefillRules.computePrefillCount(input);
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
  result = signal<ReglementInterieurValiditeResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  effectif = signal<number | null>(null);
  reglementExiste = signal<boolean>(false);
  contenuHygieneSecurite = signal<boolean>(false);
  contenuDiscipline = signal<boolean>(false);
  contenuDroitsDefense = signal<boolean>(false);
  contenuHarcelementAgissements = signal<boolean>(false);
  clauseAtteinteLibertesNonJustifiee = signal<boolean>(false);
  clauseSanctionPecuniaire = signal<boolean>(false);
  consultationCseRealisee = signal<boolean>(false);
  transmissionInspectionTravail = signal<boolean>(false);
  depotGreffeCph = signal<boolean>(false);

  provenanceEffectif = signal<'IA' | null>(null);
  provenanceReglementExiste = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) :
   *  - effectif ≥ 50 mais aucun règlement intérieur (obligation L.1311-2) ;
   *  - RI existant mais consultation du CSE non réalisée (procédure L.1321-4) ;
   *  - une clause interdite est stipulée (L.1321-3 / L.1331-2).
   */
  coherenceAlerts = computed<string[]>(() => {
    const alerts: string[] = [];
    const eff = this.effectif();
    const exists = this.reglementExiste();
    if (eff !== null && eff >= 50 && !exists) {
      alerts.push(
        'L\'effectif atteint 50 salariés mais aucun règlement intérieur n\'est en place : l\'établissement d\'un RI est obligatoire (art. L.1311-2 CT).',
      );
    }
    if (exists && !this.consultationCseRealisee()) {
      alerts.push(
        'Un règlement intérieur existe mais le CSE n\'a pas été consulté : à défaut, le RI est inopposable aux salariés (art. L.1321-4 CT).',
      );
    }
    if (this.clauseAtteinteLibertesNonJustifiee() || this.clauseSanctionPecuniaire()) {
      alerts.push(
        'Une clause interdite est stipulée (atteinte aux libertés non justifiée ou sanction pécuniaire) : elle est réputée non écrite (art. L.1321-3 et L.1331-2 CT).',
      );
    }
    return alerts;
  });

  constructor(
    private readonly service: ReglementInterieurValiditeService,
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

  onReglementExisteChange(value: boolean): void {
    this.reglementExiste.set(value);
    this.provenanceReglementExiste.set(null);
  }

  onContenuHygieneSecuriteChange(value: boolean): void { this.contenuHygieneSecurite.set(value); }
  onContenuDisciplineChange(value: boolean): void { this.contenuDiscipline.set(value); }
  onContenuDroitsDefenseChange(value: boolean): void { this.contenuDroitsDefense.set(value); }
  onContenuHarcelementAgissementsChange(value: boolean): void { this.contenuHarcelementAgissements.set(value); }
  onClauseAtteinteLibertesChange(value: boolean): void { this.clauseAtteinteLibertesNonJustifiee.set(value); }
  onClauseSanctionPecuniaireChange(value: boolean): void { this.clauseSanctionPecuniaire.set(value); }
  onConsultationCseChange(value: boolean): void { this.consultationCseRealisee.set(value); }
  onTransmissionInspectionChange(value: boolean): void { this.transmissionInspectionTravail.set(value); }
  onDepotGreffeChange(value: boolean): void { this.depotGreffeCph.set(value); }

  analyze(): void {
    if (!this.formValid()) return;
    const request: ReglementInterieurValiditeRequest = {
      effectif: this.effectif()!,
      reglementExiste: this.reglementExiste(),
      contenuHygieneSecurite: this.contenuHygieneSecurite(),
      contenuDiscipline: this.contenuDiscipline(),
      contenuDroitsDefense: this.contenuDroitsDefense(),
      contenuHarcelementAgissements: this.contenuHarcelementAgissements(),
      clauseAtteinteLibertesNonJustifiee: this.clauseAtteinteLibertesNonJustifiee(),
      clauseSanctionPecuniaire: this.clauseSanctionPecuniaire(),
      consultationCseRealisee: this.consultationCseRealisee(),
      transmissionInspectionTravail: this.transmissionInspectionTravail(),
      depotGreffeCph: this.depotGreffeCph(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse règlement intérieur enregistrée', 'OK', { duration: 2500 });
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

  statutLabel(s: ReglementInterieurValiditeStatut | null | undefined): string {
    switch (s) {
      case 'CONFORME':     return 'Règlement intérieur conforme';
      case 'NON_CONFORME': return 'Règlement intérieur non conforme';
      case 'INOPPOSABLE':  return 'Règlement intérieur inopposable';
      case 'NON_REQUIS':   return 'Règlement intérieur non requis';
      default:             return '';
    }
  }

  statutChipClass(s: ReglementInterieurValiditeStatut | null | undefined): string {
    switch (s) {
      case 'CONFORME':     return 'is-chip is-chip--success';
      case 'NON_CONFORME': return 'is-chip is-chip--warning';
      case 'INOPPOSABLE':  return 'is-chip is-chip--danger';
      case 'NON_REQUIS':   return 'is-chip is-chip--neutral';
      default:             return 'is-chip';
    }
  }

  bannerClass(s: ReglementInterieurValiditeStatut | null | undefined): string {
    switch (s) {
      case 'CONFORME':     return 'is-banner is-banner--success';
      case 'NON_CONFORME': return 'is-banner is-banner--warning';
      case 'INOPPOSABLE':  return 'is-banner is-banner--danger';
      case 'NON_REQUIS':   return 'is-banner is-banner--neutral';
      default:             return 'is-banner is-banner--navy';
    }
  }

  bannerIcon(s: ReglementInterieurValiditeStatut | null | undefined): string {
    switch (s) {
      case 'CONFORME':     return 'verified';
      case 'NON_CONFORME': return 'gpp_maybe';
      case 'INOPPOSABLE':  return 'gpp_bad';
      case 'NON_REQUIS':   return 'info_outline';
      default:             return 'info_outline';
    }
  }

  opposabiliteLabel(o: ReglementInterieurOpposabilite | null | undefined): string {
    switch (o) {
      case 'OPPOSABLE':   return 'Opposable aux salariés';
      case 'INOPPOSABLE': return 'Inopposable aux salariés';
      default:            return '';
    }
  }

  opposabiliteChipClass(o: ReglementInterieurOpposabilite | null | undefined): string {
    switch (o) {
      case 'OPPOSABLE':   return 'is-chip is-chip--success';
      case 'INOPPOSABLE': return 'is-chip is-chip--danger';
      default:            return 'is-chip';
    }
  }

  /** Note explicative sur l'opposabilité (procédure de mise en place L.1321-4). */
  opposabiliteNote(o: ReglementInterieurOpposabilite | null | undefined): string {
    switch (o) {
      case 'OPPOSABLE':
        return 'La procédure de mise en place est respectée (consultation du CSE, transmission à l\'inspection du travail, dépôt au greffe du conseil de prud\'hommes) : le règlement intérieur est opposable aux salariés (art. L.1321-4 CT).';
      case 'INOPPOSABLE':
        return 'Une formalité de la procédure de mise en place manque (consultation du CSE, transmission à l\'inspection du travail ou dépôt au greffe du conseil de prud\'hommes) : le règlement intérieur est inopposable aux salariés (art. L.1321-4 CT).';
      default:
        return '';
    }
  }

  typeBadgeLabel(t: ReglementInterieurChecklistType | null | undefined): string {
    switch (t) {
      case 'OBLIGATOIRE': return 'OBLIGATOIRE';
      case 'INTERDIT':    return 'INTERDIT';
      case 'PROCEDURE':   return 'PROCEDURE';
      default:            return '';
    }
  }

  typeBadgeClass(t: ReglementInterieurChecklistType | null | undefined): string {
    switch (t) {
      case 'OBLIGATOIRE': return 'is-badge-type is-badge-type--obligatoire';
      case 'INTERDIT':    return 'is-badge-type is-badge-type--interdit';
      case 'PROCEDURE':   return 'is-badge-type is-badge-type--procedure';
      default:            return 'is-badge-type';
    }
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const eff = ReglementInterieurValiditePrefillRules.computeEffectif(input);
    if (eff !== null && this.effectif() === null) {
      this.effectif.set(eff);
      this.provenanceEffectif.set('IA');
    }

    const exists = ReglementInterieurValiditePrefillRules.computeReglementExiste(input);
    if (exists !== null && this.provenanceReglementExiste() === null && !this.reglementExiste()) {
      this.reglementExiste.set(exists);
      this.provenanceReglementExiste.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.effectif.set(r.effectif ?? null);
        this.reglementExiste.set(r.reglementExiste ?? false);
        this.provenanceEffectif.set(null);
        this.provenanceReglementExiste.set(null);
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
