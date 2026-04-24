import { Component, Input, OnInit, OnChanges, SimpleChanges, Optional, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { OqtfAvecDelaiService } from '../../core/services/oqtf-avec-delai.service';
import {
  MOTIFS_OQTF,
  MotifOqtf,
  MotifOqtfOption,
  OqtfAvecDelaiResponse,
  StatutDelai,
} from '../../core/models/oqtf-avec-delai.model';
import { ImmigrationExtractedData, PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';

/** Enum valeurs valides pour pré-fill / alertes motif. */
const MOTIFS_OQTF_SET: ReadonlySet<MotifOqtf> = new Set<MotifOqtf>([
  'REFUS_TITRE', 'EXPIRATION_TITRE', 'SEJOUR_IRREGULIER', 'RETRAIT_TITRE', 'AUTRE',
]);

/** Regex ISO strict YYYY-MM-DD. */
const ISO_DATE_RE = /^\d{4}-\d{2}-\d{2}$/;

export type OqtfAlertField = 'DATE_NOTIFICATION' | 'MOTIF_OQTF' | 'RECOURS_FORME';
export type OqtfAlertSeverity = 'WARNING' | 'CRITICAL';

export interface OqtfCoherenceAlert {
  field: OqtfAlertField;
  severity: OqtfAlertSeverity;
  expectedDisplay: string;
  reason: string;
}

/**
 * SF-IM-08-02 : outil décisionnel dédié "OQTF avec délai de départ
 * volontaire — France" (F-IM-08). FR uniquement — OQT belge (annexe 13)
 * traitée dans SF-IM-08-05 / SF-IM-08-06 (procédure juridiquement
 * distincte, invariant "un outil = une situation métier"). Consomme
 * l'API SF-IM-08-01. Affiché conditionnellement par le panel F-IA-04
 * (tool_id 'F-IM-08-oqtf-avec-delai-fr').
 *
 * SF-155-04-B1 (2026-04-24) : pré-fill IA depuis `ImmigrationExtractedData`
 * FR + validation F-IA-03 (3 alertes de cohérence dont 1 critique pour
 * détecter un recours déjà formé oublié par l'avocat).
 */
@Component({
  selector: 'app-oqtf-avec-delai-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatChipsModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './oqtf-avec-delai-section.component.html',
  styleUrl: './oqtf-avec-delai-section.component.scss',
})
export class OqtfAvecDelaiSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // SF-155-04-B1 : sources IA pour pré-fill + alertes de cohérence.
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<OqtfAvecDelaiResponse | null>(null);

  dateNotificationOqtf = signal<string | null>(null);
  motifOqtf = signal<MotifOqtf | null>(null);
  recoursForme = signal<boolean>(false);
  dateRecours = signal<string | null>(null);

  // SF-155-04-B1 : provenance IA par champ (badge "Pré-rempli depuis l'analyse").
  // Effacé dès que l'avocat modifie manuellement un champ (onXxxChange).
  provenanceDateNotification = signal<'IA' | null>(null);
  provenanceMotifOqtf = signal<'IA' | null>(null);
  provenanceRecoursForme = signal<'IA' | null>(null);

  readonly motifs: MotifOqtfOption[] = MOTIFS_OQTF;

  /** Aujourd'hui en YYYY-MM-DD pour l'attribut max des inputs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  // SF-155-04-B1 : alertes de cohérence F-IA-03 sur 3 champs.
  coherenceAlerts = computed<Partial<Record<OqtfAlertField, OqtfCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<OqtfAlertField, OqtfCoherenceAlert>> = {};
    const a1 = this.buildDateNotificationAlert();
    if (a1) alerts.DATE_NOTIFICATION = a1;
    const a2 = this.buildMotifOqtfAlert();
    if (a2) alerts.MOTIF_OQTF = a2;
    const a3 = this.buildRecoursFormeAlert();
    if (a3) alerts.RECOURS_FORME = a3;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter(a => a?.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: OqtfAvecDelaiService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    if (this.isFrance()) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    // Re-prefill si aiData arrive après le mount et qu'aucune analyse persistée
    // n'est présente ni en cours de saisie avancée. On évite d'écraser :
    // - l'analyse déjà chargée (result !== null),
    // - une valeur déjà saisie par l'avocat (on ne ré-applique que si le champ
    //   est encore vide — c'est-à-dire que le formulaire n'a pas été touché).
    if (changes['aiData'] && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const d = this.dateNotificationOqtf();
    const m = this.motifOqtf();
    if (!d || !m) return false;
    if (d > this.todayIso) return false; // pas dans le futur
    if (this.recoursForme()) {
      const dr = this.dateRecours();
      if (!dr) return false;
      if (dr < d) return false; // recours >= notification
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  /** SF-155-04-B1 : handlers de modification manuelle — effacement du badge IA. */
  onDateNotificationChange(value: string | null): void {
    this.dateNotificationOqtf.set(value || null);
    this.provenanceDateNotification.set(null);
  }

  onMotifOqtfChange(value: MotifOqtf | null): void {
    this.motifOqtf.set(value);
    this.provenanceMotifOqtf.set(null);
  }

  onRecoursFormeChange(value: boolean): void {
    this.recoursForme.set(value);
    this.provenanceRecoursForme.set(null);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request = {
      dateNotificationOqtf: this.dateNotificationOqtf()!,
      motifOqtf: this.motifOqtf()!,
      recoursForme: this.recoursForme(),
      ...(this.recoursForme()
        ? { dateRecours: this.dateRecours()! }
        : {}),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('OQTF analysée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  /**
   * Classe CSS du bandeau de résultat selon le statut calculé.
   * Rouge réservé au seul statut EXPIRE (alerte critique —
   * DESIGN_SYSTEM.md autorise la couleur rouge pour les statuts
   * d'urgence uniquement).
   */
  bannerClass(statut: StatutDelai | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':   return 'oqtf-banner oqtf-banner--info';
      case 'URGENT':       return 'oqtf-banner oqtf-banner--warning';
      case 'EXPIRE':       return 'oqtf-banner oqtf-banner--danger';
      case 'RECOURS_FORME':return 'oqtf-banner oqtf-banner--success';
      default:             return 'oqtf-banner';
    }
  }

  bannerIcon(statut: StatutDelai | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'info_outline';
      case 'URGENT':        return 'warning';
      case 'EXPIRE':        return 'error';
      case 'RECOURS_FORME': return 'check_circle';
      default:              return 'info_outline';
    }
  }

  statutLabel(statut: StatutDelai | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'Délai disponible';
      case 'URGENT':        return 'Délai urgent';
      case 'EXPIRE':        return 'Délai expiré';
      case 'RECOURS_FORME': return 'Recours formé';
      default:              return '';
    }
  }

  /** Affiche le compteur jours restants si pertinent (hors EXPIRE/RECOURS_FORME). */
  showJoursRestants(r: OqtfAvecDelaiResponse | null): boolean {
    if (!r) return false;
    return r.statutDelaiRecours === 'DISPONIBLE' || r.statutDelaiRecours === 'URGENT';
  }

  /**
   * SF-155-04-B1 : libellé du badge d'alerte côté template (tooltip fallback).
   */
  alertBadgeLabel(alert: OqtfCoherenceAlert): string {
    const prefix = alert.severity === 'CRITICAL'
      ? 'Risque d\'irrecevabilité'
      : 'Incohérence détectée';
    return `${prefix} (${alert.expectedDisplay})`;
  }

  /**
   * SF-155-04-B1 : pré-remplissage des 3 champs clés depuis l'analyse IA.
   * Pas d'écrasement des valeurs déjà saisies.
   */
  private prefillFromAi(): void {
    const ai = this.aiData;
    if (!ai) return;

    // 1. Date de notification OQTF — format YYYY-MM-DD strict, non futur.
    const date = ai.dateNotificationOqtf;
    if (typeof date === 'string' && ISO_DATE_RE.test(date) && date <= this.todayIso) {
      if (!this.dateNotificationOqtf()) {
        this.dateNotificationOqtf.set(date);
        this.provenanceDateNotification.set('IA');
      }
    }

    // 2. Motif OQTF — seulement si valeur dans l'enum front (garde-fou).
    const code = ai.motifOqtfCode;
    if (code && MOTIFS_OQTF_SET.has(code as MotifOqtf)) {
      if (!this.motifOqtf()) {
        this.motifOqtf.set(code as MotifOqtf);
        this.provenanceMotifOqtf.set('IA');
      }
    }

    // 3. Recours formé — dérivé de DetectedAnswer {OUI/NON/INCONNU}.
    //    INCONNU = pas de signal, on reste à la valeur par défaut (false).
    const detected = ai.recoursFormeDetected;
    if (detected && (detected.reponse === 'OUI' || detected.reponse === 'NON')) {
      // On ne pré-fill que si l'avocat n'a pas déjà manipulé le toggle
      // (signal à 'false' défaut → inchangé si détecté NON, mais badge sans
      // effet visible ; pour OUI on change la valeur).
      if (this.provenanceRecoursForme() === null && !this.recoursForme()) {
        this.recoursForme.set(detected.reponse === 'OUI');
        this.provenanceRecoursForme.set('IA');
      } else if (this.provenanceRecoursForme() === null && this.recoursForme() && detected.reponse === 'OUI') {
        // Déjà true par défaut impossible ici (défaut false) — no-op défensif
        this.provenanceRecoursForme.set('IA');
      }
    }
  }

  private buildDateNotificationAlert(): OqtfCoherenceAlert | null {
    const ai = this.aiData?.dateNotificationOqtf;
    const user = this.dateNotificationOqtf();
    if (!user) return null;
    if (typeof ai !== 'string' || !ISO_DATE_RE.test(ai)) return null;
    if (ai === user) return null;
    return {
      field: 'DATE_NOTIFICATION',
      severity: 'WARNING',
      expectedDisplay: ai,
      reason: `L'analyse a détecté une date de notification différente : ${ai}`,
    };
  }

  private buildMotifOqtfAlert(): OqtfCoherenceAlert | null {
    const aiCode = this.aiData?.motifOqtfCode;
    const user = this.motifOqtf();
    if (!user) return null;
    if (!aiCode || !MOTIFS_OQTF_SET.has(aiCode as MotifOqtf)) return null;
    if (aiCode === user) return null;
    const label = MOTIFS_OQTF.find(m => m.code === aiCode)?.label ?? aiCode;
    return {
      field: 'MOTIF_OQTF',
      severity: 'WARNING',
      expectedDisplay: label,
      reason: `L'analyse a détecté un motif différent : ${label}`,
    };
  }

  /**
   * Alerte critique : l'IA a détecté un recours déjà formé mais l'avocat a
   * laissé le toggle à "non formé" — risque majeur d'oubli (irrecevabilité
   * potentielle ou action en doublon).
   */
  private buildRecoursFormeAlert(): OqtfCoherenceAlert | null {
    const detected = this.aiData?.recoursFormeDetected;
    if (!detected || detected.reponse !== 'OUI') return null;
    if (this.recoursForme()) return null; // avocat en phase avec la détection
    return {
      field: 'RECOURS_FORME',
      severity: 'CRITICAL',
      expectedDisplay: 'Recours déjà formé détecté',
      reason: `L'analyse a détecté qu'un recours a déjà été formé${detected.justification ? ' — ' + detected.justification : ''}. Vérifiez avant d'introduire une nouvelle action (risque d'irrecevabilité).`,
    };
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateNotificationOqtf.set(r.dateNotificationOqtf);
        this.motifOqtf.set(r.motifOqtf);
        this.recoursForme.set(r.recoursForme);
        this.dateRecours.set(r.dateRecours);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire et on
        // tente le pré-fill depuis l'IA (si aiData disponible au mount).
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }
}
