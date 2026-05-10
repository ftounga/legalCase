import {
  Component,
  Input,
  OnChanges,
  OnInit,
  Optional,
  SimpleChanges,
  computed,
  signal,
} from '@angular/core';
import { CommonModule, DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { AcceptationRenonciationSuccessionService } from '../../core/services/acceptation-renonciation-succession.service';
import {
  AcceptationRenonciationSuccessionRequest,
  AcceptationRenonciationSuccessionResponse,
  IntentionExprimee,
  QualiteHeritier,
  SuccessionOption,
} from '../../core/models/acceptation-renonciation-succession.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherenceAlert } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { AcceptationRenonciationPrefillRules } from './acceptation-renonciation-section-prefill-rules';

export type AcceptationRenonciationAlertField = 'DATE_OUVERTURE' | 'ACTIF_BRUT';
export type AcceptationRenonciationCoherenceAlert =
  CoherenceAlert<AcceptationRenonciationAlertField>;

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;
const ACTIF_DIVERGENCE_RATIO = 0.10;

/**
 * F-210 SF-210-04 : composant Angular standalone pour l'outil "Acceptation /
 * renonciation à succession" (Cciv art. 768+ : 3 options + délais 4+2 mois).
 * FRANCE uniquement, droit famille.
 *
 * Pré-fill IA via {@link FamilleExtractedData} :
 * dateOuvertureSuccessionDetectee, actifBrutSuccessionEurDetecte,
 * passifSuccessionEurDetecte, qualiteHeritierDetectee,
 * actesEquivalentAcceptationDejaPosesDetected, dettesIncertainesDetected.
 *
 * Validation F-IA-03 : alertes sur date d'ouverture et actif brut si l'avocat
 * saisit une valeur divergente.
 */
@Component({
  selector: 'app-acceptation-renonciation-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule, MatSelectModule,
    MatSlideToggleModule, MatProgressSpinnerModule,
    MatTooltipModule,
    LegalCitationsPipe,
  ],
  templateUrl: './acceptation-renonciation-section.component.html',
  styleUrl: './acceptation-renonciation-section.component.scss',
})
export class AcceptationRenonciationSectionComponent implements OnInit, OnChanges {
  static readonly TOOL_LABEL = 'ACCEPTATION / RENONCIATION SUCCESSION (FR)';
  static readonly TOOL_ICON = 'how_to_reg';

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;
  @Input() forceExpanded = false;

  private aiDataSignal = signal<FamilleExtractedData | null | undefined>(undefined);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<AcceptationRenonciationSuccessionResponse | null>(null);

  dateOuvertureSuccession = signal<string | null>(null);
  qualiteHeritier = signal<QualiteHeritier>('PREMIER_RANG');
  actifBrutEur = signal<number | null>(null);
  passifEur = signal<number | null>(null);
  actesEquivalentAcceptation = signal<boolean>(false);
  inventaireRealise = signal<boolean>(false);
  dettesIncertaines = signal<boolean>(false);
  intentionExprimee = signal<IntentionExprimee>('INCERTAIN');

  provenanceDateOuverture = signal<'IA' | null>(null);
  provenanceActifBrut = signal<'IA' | null>(null);
  provenancePassif = signal<'IA' | null>(null);
  provenanceQualite = signal<'IA' | null>(null);
  provenanceActes = signal<'IA' | null>(null);
  provenanceDettesIncertaines = signal<'IA' | null>(null);

  readonly intentionsOptions: { value: IntentionExprimee; label: string }[] = [
    { value: 'INCERTAIN', label: 'Indécis — analyse demandée' },
    { value: 'ACCEPTATION_PURE_SIMPLE', label: 'Acceptation pure et simple' },
    { value: 'ACCEPTATION_CONCURRENCE_ACTIF', label: 'Acceptation à concurrence de l\'actif net' },
    { value: 'RENONCIATION', label: 'Renonciation' },
  ];

  coherenceAlerts = computed<Partial<Record<AcceptationRenonciationAlertField, AcceptationRenonciationCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<AcceptationRenonciationAlertField, AcceptationRenonciationCoherenceAlert>> = {};
    const d = this.buildDateOuvertureAlert();
    if (d) alerts.DATE_OUVERTURE = d;
    const a = this.buildActifBrutAlert();
    if (a) alerts.ACTIF_BRUT = a;
    return alerts;
  });

  alertsSummary = computed(() => ({
    total: Object.values(this.coherenceAlerts()).length,
  }));

  constructor(
    private service: AcceptationRenonciationSuccessionService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    if (this.workspaceCountry === 'FRANCE') {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  isFrance(): boolean {
    return this.workspaceCountry === 'FRANCE';
  }

  formValid(): boolean {
    return this.dateOuvertureSuccession() !== null
      && this.actifBrutEur() !== null && this.actifBrutEur()! >= 0
      && this.passifEur() !== null && this.passifEur()! >= 0;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onDateOuvertureChange(value: string | null): void {
    this.dateOuvertureSuccession.set(value || null);
    this.provenanceDateOuverture.set(null);
  }
  onQualiteChange(value: QualiteHeritier): void {
    this.qualiteHeritier.set(value);
    this.provenanceQualite.set(null);
  }
  onActifChange(value: number | null): void {
    this.actifBrutEur.set(value);
    this.provenanceActifBrut.set(null);
  }
  onPassifChange(value: number | null): void {
    this.passifEur.set(value);
    this.provenancePassif.set(null);
  }
  onActesChange(value: boolean): void {
    this.actesEquivalentAcceptation.set(value);
    this.provenanceActes.set(null);
  }
  onInventaireChange(value: boolean): void {
    this.inventaireRealise.set(value);
  }
  onDettesIncertainesChange(value: boolean): void {
    this.dettesIncertaines.set(value);
    this.provenanceDettesIncertaines.set(null);
  }
  onIntentionChange(value: IntentionExprimee): void {
    this.intentionExprimee.set(value);
  }

  calculate(): void {
    if (!this.formValid() || !this.dateOuvertureSuccession()) return;
    const request: AcceptationRenonciationSuccessionRequest = {
      dateOuvertureSuccession: this.dateOuvertureSuccession()!,
      qualiteHeritier: this.qualiteHeritier(),
      actifBrutEur: this.actifBrutEur()!,
      passifEur: this.passifEur()!,
      actesEquivalentAcceptationDejaPosesDetected: this.actesEquivalentAcceptation(),
      inventaireRealise: this.inventaireRealise(),
      dettesIncertainesDetected: this.dettesIncertaines(),
      intentionExprimee: this.intentionExprimee(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse d\'option successorale calculée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
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
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private applyResult(r: AcceptationRenonciationSuccessionResponse): void {
    this.result.set(r);
    this.dateOuvertureSuccession.set(r.dateOuvertureSuccession);
    this.qualiteHeritier.set(r.qualiteHeritier);
    this.actifBrutEur.set(r.actifBrutEur);
    this.passifEur.set(r.passifEur);
    this.actesEquivalentAcceptation.set(r.actesEquivalentAcceptationDejaPosesDetected);
    this.inventaireRealise.set(r.inventaireRealise);
    this.dettesIncertaines.set(r.dettesIncertainesDetected);
    const intent = (r.intentionExprimee as IntentionExprimee);
    this.intentionExprimee.set(intent || 'INCERTAIN');
    this.provenanceDateOuverture.set(null);
    this.provenanceActifBrut.set(null);
    this.provenancePassif.set(null);
    this.provenanceQualite.set(null);
    this.provenanceActes.set(null);
    this.provenanceDettesIncertaines.set(null);
    this.showForm.set(false);
  }

  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé.
    const h = { aiData: this.aiDataSignal() };

    const dateOpen = AcceptationRenonciationPrefillRules.computeDateOuverture(h);
    if (dateOpen !== null
        && (this.dateOuvertureSuccession() === null || this.provenanceDateOuverture() === 'IA')) {
      this.dateOuvertureSuccession.set(dateOpen);
      this.provenanceDateOuverture.set('IA');
    }
    const ab = AcceptationRenonciationPrefillRules.computeActifBrut(h);
    if (ab !== null && (this.actifBrutEur() === null || this.provenanceActifBrut() === 'IA')) {
      this.actifBrutEur.set(ab);
      this.provenanceActifBrut.set('IA');
    }
    const ps = AcceptationRenonciationPrefillRules.computePassif(h);
    if (ps !== null && (this.passifEur() === null || this.provenancePassif() === 'IA')) {
      this.passifEur.set(ps);
      this.provenancePassif.set('IA');
    }
    const qual = AcceptationRenonciationPrefillRules.computeQualiteHeritier(h);
    if (qual !== null
        && (this.provenanceQualite() === 'IA' || this.qualiteHeritier() === 'PREMIER_RANG')) {
      this.qualiteHeritier.set(qual);
      this.provenanceQualite.set('IA');
    }
    const actes = AcceptationRenonciationPrefillRules.computeActesEquivalentAcceptation(h);
    if (actes !== null
        && (this.provenanceActes() === 'IA' || this.actesEquivalentAcceptation() === false)) {
      this.actesEquivalentAcceptation.set(actes);
      this.provenanceActes.set('IA');
    }
    const dettes = AcceptationRenonciationPrefillRules.computeDettesIncertaines(h);
    if (dettes !== null
        && (this.provenanceDettesIncertaines() === 'IA' || this.dettesIncertaines() === false)) {
      this.dettesIncertaines.set(dettes);
      this.provenanceDettesIncertaines.set('IA');
    }
  }

  private buildDateOuvertureAlert(): AcceptationRenonciationCoherenceAlert | null {
    const iaDate = this.aiDataSignal()?.dateOuvertureSuccessionDetectee;
    const user = this.dateOuvertureSuccession();
    if (!user || !iaDate || !ISO_DATE_REGEX.test(iaDate)) return null;
    if (user === iaDate) return null;
    if (this.provenanceDateOuverture() === 'IA') return null;
    return CoherenceAlertBuilder.forField<AcceptationRenonciationAlertField>('DATE_OUVERTURE')
      .addSource('IA', {
        expectedDisplay: iaDate,
        reason: `Analyse du dossier : succession ouverte le ${iaDate}`,
      })
      .build();
  }

  private buildActifBrutAlert(): AcceptationRenonciationCoherenceAlert | null {
    const iaVal = this.aiDataSignal()?.actifBrutSuccessionEurDetecte;
    const user = this.actifBrutEur();
    if (typeof iaVal !== 'number' || iaVal <= 0) return null;
    if (user === null || user <= 0) return null;
    const ratio = Math.abs(user - iaVal) / iaVal;
    if (ratio <= ACTIF_DIVERGENCE_RATIO) return null;
    if (this.provenanceActifBrut() === 'IA') return null;
    return CoherenceAlertBuilder.forField<AcceptationRenonciationAlertField>('ACTIF_BRUT')
      .addSource('IA', {
        expectedDisplay: `${iaVal.toLocaleString('fr-FR')} €`,
        reason: `Analyse du dossier : actif brut succession ~${iaVal.toLocaleString('fr-FR')} € (écart > 10 %)`,
      })
      .build();
  }

  alertTooltip(alert: AcceptationRenonciationCoherenceAlert): string {
    return alert.reason;
  }
  alertBadgeLabel(alert: AcceptationRenonciationCoherenceAlert): string {
    return `Incohérence détectée (${alert.expectedDisplay})`;
  }

  delaiBadgeClass(jours: number): string {
    if (jours < 0) return 'ars-delai-badge ars-delai-badge--danger';
    if (jours <= 30) return 'ars-delai-badge ars-delai-badge--warn';
    return 'ars-delai-badge ars-delai-badge--ok';
  }

  delaiBadgeLabel(jours: number): string {
    if (jours < 0) return `Délai dépassé (${Math.abs(jours)} jours)`;
    if (jours === 0) return 'Délai expirant aujourd\'hui';
    return `${jours} jours restants`;
  }

  optionLabel(o: SuccessionOption): string {
    switch (o) {
      case 'ACCEPTATION_PURE_SIMPLE': return 'Acceptation pure et simple';
      case 'ACCEPTATION_CONCURRENCE_ACTIF': return 'Acceptation à concurrence de l\'actif net';
      case 'RENONCIATION': return 'Renonciation';
    }
  }

  /**
   * F-210 SF-210-04 : pré-fill count miroir de prefillFromAi().
   */
  static getPrefillCount(input: { aiData?: FamilleExtractedData | null }): number {
    // F-236 SF-236-02 — délégation au helper partagé.
    return AcceptationRenonciationPrefillRules.computePrefillCount(input);
  }
}
