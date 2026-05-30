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

import { StagiaireGratificationService } from '../../core/services/stagiaire-gratification.service';
import {
  RisqueRequalification,
  StagiaireGratificationRequest,
  StagiaireGratificationResponse,
  VerdictGlobal,
} from '../../core/models/stagiaire-gratification.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { PrefillCountInput } from '../decisional-tools-panel/decision-tool.contract';
import { StagiaireGratificationPrefillRules } from './stagiaire-gratification-section-prefill-rules';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';

/**
 * SF-218-22 — Outil décisionnel « Stagiaire : gratification minimale et
 * requalification en CDI » (F-DT-109-stagiaire-gratification-requalification).
 *
 * FRANCE uniquement — applique les art. L.124-1 et s. du Code de l'éducation :
 * vérification du seuil de déclenchement et calcul de la gratification minimale
 * obligatoire (au-delà de 2 mois / 44 jours de présence — art. L.124-6 +
 * D.124-6 = 15 % du plafond horaire SS), et appréciation du risque de
 * requalification du stage en contrat de travail (CDI) en présence d'indices
 * d'irrégularité (poste de travail permanent, missions hors projet pédagogique,
 * dépassement de la durée maximale de 6 mois — art. L.124-5 et L.124-8).
 *
 * Verdict global (3 états) :
 *  - STAGE_CONFORME (vert) : gratification suffisante + risque non élevé.
 *  - RAPPEL_GRATIFICATION (orange) : gratification obligatoire insuffisamment versée.
 *  - REQUALIFICATION_PROBABLE (rouge) : risque de requalification élevé.
 *
 * Conforme F-IA-04 / ai-skills/frontend-coherence-audit.md :
 *  - standalone OnPush, palette navy/or ; rouge réservé rappel > 0 / risque
 *    ELEVE / REQUALIFICATION_PROBABLE ; orange MODERE / RAPPEL_GRATIFICATION ;
 *    vert FAIBLE / STAGE_CONFORME ; `<input type="date">` ; JetBrains Mono
 *    montants / baseJuridique ; bannière gate FR ; MatSnackBar.
 *  - pré-fill IA (dateDebutStage, dateFinStage) depuis TravailExtractedData +
 *    badge `auto_awesome` de provenance.
 *  - OnPush + ChangeDetectorRef.markForCheck() dans subscribe (next/error)
 *    cf. feedback_onpush_subscribe_markforcheck (mémoire utilisateur).
 *  - F-IA-03 : coherenceAlerts (dates croisées ; gratification versée saisie sur
 *    un stage sous le seuil ; indices d'irrégularité sans poste permanent).
 */
@Component({
  selector: 'app-stagiaire-gratification-section',
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
  templateUrl: './stagiaire-gratification-section.component.html',
  styleUrl: './stagiaire-gratification-section.component.scss',
})
export class StagiaireGratificationSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-99c — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-DT-109-stagiaire-gratification-requalification';
  protected readonly brancheActiveForJurisprudence = 'default';

  static readonly TOOL_LABEL = 'STAGIAIRE GRATIFICATION (FR)';
  static readonly TOOL_ICON = 'school';

  static getPrefillCount(input: PrefillCountInput): number {
    return StagiaireGratificationPrefillRules.computePrefillCount(input);
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
  result = signal<StagiaireGratificationResponse | null>(null);

  // --- formulaire -----------------------------------------------------------
  dateDebutStage = signal<string | null>(null);
  dateFinStage = signal<string | null>(null);
  nombreJoursPresence = signal<number | null>(null);
  gratificationMensuelleVersee = signal<number | null>(null);
  tauxHoraireConventionnel = signal<number | null>(null);
  missionsHorsProjetPedagogique = signal<boolean>(false);
  posteTravailPermanent = signal<boolean>(false);

  provenanceDateDebut = signal<'IA' | null>(null);
  provenanceDateFin = signal<'IA' | null>(null);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /** Nombre d'indices locaux de requalification cochés (estimation jauge). */
  indicesRequalificationLocal = computed<number>(() => {
    let n = 0;
    if (this.missionsHorsProjetPedagogique()) n++;
    if (this.posteTravailPermanent()) n++;
    return n;
  });

  /**
   * F-IA-03 — alertes de cohérence (non bloquantes) : dates croisées ; missions
   * hors projet pédagogique cochées sans poste permanent (à vérifier) ;
   * gratification versée saisie alors que la date de fin est antérieure au début.
   */
  coherenceAlerts = computed<string[]>(() => {
    const alerts: string[] = [];
    const debut = this.dateDebutStage();
    const fin = this.dateFinStage();
    if (debut && fin && fin < debut) {
      alerts.push(
        'La date de fin de stage est antérieure à la date de début : vérifiez les dates de la convention de stage.',
      );
    }
    if (this.missionsHorsProjetPedagogique() && !this.posteTravailPermanent()) {
      alerts.push(
        'Des missions hors projet pédagogique sont signalées sans occupation d\'un poste de travail permanent : à recouper, l\'exécution de tâches régulières est un indice fort de requalification (art. L.124-8).',
      );
    }
    if ((this.gratificationMensuelleVersee() ?? 0) > 0 && (this.nombreJoursPresence() ?? 0) > 0 && (this.nombreJoursPresence() ?? 0) <= 44) {
      alerts.push(
        'Une gratification est versée alors que la présence (≤ 44 jours) reste sous le seuil de gratification obligatoire : à vérifier au regard de la convention de stage.',
      );
    }
    return alerts;
  });

  constructor(
    private readonly service: StagiaireGratificationService,
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

  /** Dates requises + nombre de jours ≥ 0 + montants ≥ 0. */
  formValid(): boolean {
    if (!this.dateDebutStage() || !this.dateFinStage()) return false;
    if ((this.nombreJoursPresence() ?? -1) < 0) return false;
    if ((this.gratificationMensuelleVersee() ?? 0) < 0) return false;
    if ((this.tauxHoraireConventionnel() ?? 0) < 0) return false;
    return true;
  }

  onDateDebutChange(value: string | null): void {
    this.dateDebutStage.set(value || null);
    this.provenanceDateDebut.set(null);
  }

  onDateFinChange(value: string | null): void {
    this.dateFinStage.set(value || null);
    this.provenanceDateFin.set(null);
  }

  onNombreJoursChange(value: number | null): void {
    this.nombreJoursPresence.set(value === null || value === undefined ? null : Number(value));
  }

  onGratificationVerseeChange(value: number | null): void {
    this.gratificationMensuelleVersee.set(value === null || value === undefined ? null : Number(value));
  }

  onTauxConventionnelChange(value: number | null): void {
    this.tauxHoraireConventionnel.set(value === null || value === undefined ? null : Number(value));
  }

  onMissionsHorsProjetChange(value: boolean): void {
    this.missionsHorsProjetPedagogique.set(value);
  }

  onPosteTravailPermanentChange(value: boolean): void {
    this.posteTravailPermanent.set(value);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request: StagiaireGratificationRequest = {
      dateDebutStage: this.dateDebutStage()!,
      dateFinStage: this.dateFinStage()!,
      nombreJoursPresence: this.nombreJoursPresence() ?? 0,
      gratificationMensuelleVersee: this.gratificationMensuelleVersee(),
      tauxHoraireConventionnel: this.tauxHoraireConventionnel(),
      missionsHorsProjetPedagogique: this.missionsHorsProjetPedagogique(),
      posteTravailPermanent: this.posteTravailPermanent(),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse gratification stagiaire enregistrée', 'OK', { duration: 2500 });
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

  verdictLabel(v: VerdictGlobal | null | undefined): string {
    switch (v) {
      case 'STAGE_CONFORME':           return 'Stage conforme';
      case 'RAPPEL_GRATIFICATION':     return 'Rappel de gratification dû';
      case 'REQUALIFICATION_PROBABLE': return 'Requalification probable';
      default:                         return '';
    }
  }

  verdictChipClass(v: VerdictGlobal | null | undefined): string {
    switch (v) {
      case 'STAGE_CONFORME':           return 'is-chip is-chip--success';
      case 'RAPPEL_GRATIFICATION':     return 'is-chip is-chip--warning';
      case 'REQUALIFICATION_PROBABLE': return 'is-chip is-chip--danger';
      default:                         return 'is-chip';
    }
  }

  bannerClass(v: VerdictGlobal | null | undefined): string {
    switch (v) {
      case 'STAGE_CONFORME':           return 'is-banner is-banner--success';
      case 'RAPPEL_GRATIFICATION':     return 'is-banner is-banner--warning';
      case 'REQUALIFICATION_PROBABLE': return 'is-banner is-banner--danger';
      default:                         return 'is-banner is-banner--navy';
    }
  }

  bannerIcon(v: VerdictGlobal | null | undefined): string {
    switch (v) {
      case 'STAGE_CONFORME':           return 'verified';
      case 'RAPPEL_GRATIFICATION':     return 'payments';
      case 'REQUALIFICATION_PROBABLE': return 'gavel';
      default:                         return 'info_outline';
    }
  }

  risqueLabel(r: RisqueRequalification | null | undefined): string {
    switch (r) {
      case 'FAIBLE': return 'Risque faible';
      case 'MODERE': return 'Risque modéré';
      case 'ELEVE':  return 'Risque élevé';
      default:       return '';
    }
  }

  risqueChipClass(r: RisqueRequalification | null | undefined): string {
    switch (r) {
      case 'FAIBLE': return 'is-chip is-chip--success';
      case 'MODERE': return 'is-chip is-chip--warning';
      case 'ELEVE':  return 'is-chip is-chip--danger';
      default:       return 'is-chip';
    }
  }

  gratificationChipClass(obligatoire: boolean | null | undefined): string {
    return obligatoire ? 'is-chip is-chip--warning' : 'is-chip is-chip--neutral';
  }

  /** Le rappel de gratification est-il dû (> 0) → rouge. */
  rappelDu(): boolean {
    return (this.result()?.rappelGratification ?? 0) > 0;
  }

  private prefillFromAi(): void {
    if (this.standaloneMode) return;
    const input: PrefillCountInput = {
      aiData: this.aiData,
      workspaceCountry: this.workspaceCountry,
    };

    const debut = StagiaireGratificationPrefillRules.computeDateDebutStage(input);
    if (debut !== null && this.dateDebutStage() === null) {
      this.dateDebutStage.set(debut);
      this.provenanceDateDebut.set('IA');
    }

    const fin = StagiaireGratificationPrefillRules.computeDateFinStage(input);
    if (fin !== null && this.dateFinStage() === null) {
      this.dateFinStage.set(fin);
      this.provenanceDateFin.set('IA');
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateDebutStage.set(r.dateDebutStage ?? null);
        this.dateFinStage.set(r.dateFinStage ?? null);
        this.nombreJoursPresence.set(r.nombreJoursPresence ?? null);
        this.gratificationMensuelleVersee.set(r.gratificationMensuelleVersee ?? null);
        this.tauxHoraireConventionnel.set(r.tauxHoraireConventionnel ?? null);
        this.missionsHorsProjetPedagogique.set(r.missionsHorsProjetPedagogique ?? false);
        this.posteTravailPermanent.set(r.posteTravailPermanent ?? false);
        this.provenanceDateDebut.set(null);
        this.provenanceDateFin.set(null);
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
