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
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DivorceFauteService } from '../../core/services/divorce-faute.service';
import {
  DivorceFauteResponse,
  FAUTES_FR,
  FauteCode,
  FauteOption,
  fauteLabel,
  verdictProbabiliteLabel,
  verdictTortsLabel,
  VerdictProbabilite,
  VerdictTorts,
} from '../../core/models/divorce-faute.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

/**
 * SF-FA-09-02 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-FA-09 (divorce pour faute, FR uniquement, art. 242 Cciv).
 */
export type DivorceFauteAlertField =
  | 'REVENUS_DEMANDEUR'
  | 'REVENUS_DEFENDEUR'
  | 'DUREE_MARIAGE'
  | 'FAUTES_INVOQUEES';

export interface DivorceFauteCoherenceAlert {
  field: DivorceFauteAlertField;
  expectedDisplay: string;
  reason: string;
}

/**
 * Seuil d'écart relatif (10 %) au-delà duquel on déclenche une alerte
 * de divergence sur les revenus annuels — aligné F-DT-09.
 */
const REVENUS_DIVERGENCE_RATIO = 0.10;

/** Écart absolu (en années) pour la durée de mariage. */
const DUREE_MARIAGE_DIVERGENCE_YEARS = 1;

const VALID_FAUTE_CODES: ReadonlySet<string> = new Set<FauteCode>([
  'ADULTERE',
  'VIOLENCES',
  'ABANDON',
  'OUTRAGES',
  'DEVOIR_ASSISTANCE',
  'DEVOIR_FIDELITE',
  'DEVOIR_COMMUNAUTE_VIE',
  'AUTRE',
]);

/**
 * SF-FA-09-02 : outil décisionnel "Divorce pour faute" (FR uniquement).
 * Affiché conditionnellement par le panel F-IA-04 (tool_id
 * `F-FA-09-divorce-faute`). Consomme l'API SF-FA-09-01.
 *
 * Pattern canonique emprunté à `harcelement-licenciement-nul-section`
 * (HLN, F-DT-11) — cf. `ai-skills/frontend-coherence-audit.md`.
 */
@Component({
  selector: 'app-divorce-faute-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatChipsModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
  ],
  templateUrl: './divorce-faute-section.component.html',
  styleUrl: './divorce-faute-section.component.scss',
})
export class DivorceFauteSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // SF-FA-09-02 : pré-fill IA (tous optionnels — null-safe partout).
  @Input() aiData?: TravailExtractedData | null;

  // Snapshots signal pour que les `computed` réagissent aux changements d'input.
  private aiDataSignal = signal<TravailExtractedData | null | undefined>(undefined);

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<DivorceFauteResponse | null>(null);

  // Form state (signals pour cohérence avec le pattern canonique).
  fautesInvoquees = signal<FauteCode[]>([]);
  preuvesDocumentaires = signal<boolean>(false);
  tortsAdverseInvoques = signal<boolean>(false);
  dureeMariageAnnees = signal<number | null>(null);
  revenusAnnuelsDemandeurEur = signal<number | null>(null);
  revenusAnnuelsDefendeurEur = signal<number | null>(null);
  dateDepotAssignation = signal<string | null>(null);

  // Provenance IA par champ.
  provenanceFautesInvoquees = signal<'IA' | null>(null);
  provenanceDureeMariage = signal<'IA' | null>(null);
  provenanceRevenusDemandeur = signal<'IA' | null>(null);
  provenanceRevenusDefendeur = signal<'IA' | null>(null);
  provenanceDateDepot = signal<'IA' | null>(null);

  readonly fautesOptions: FauteOption[] = FAUTES_FR;

  // SF-FA-09-02 : alertes de cohérence calculées dynamiquement.
  // Gate : uniquement en mode formulaire (pattern anti-bug SF-IA-03-12).
  coherenceAlerts = computed<Partial<Record<DivorceFauteAlertField, DivorceFauteCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<DivorceFauteAlertField, DivorceFauteCoherenceAlert>> = {};
    const dem = this.buildRevenusAlert('REVENUS_DEMANDEUR');
    if (dem) alerts.REVENUS_DEMANDEUR = dem;
    const def = this.buildRevenusAlert('REVENUS_DEFENDEUR');
    if (def) alerts.REVENUS_DEFENDEUR = def;
    const duree = this.buildDureeMariageAlert();
    if (duree) alerts.DUREE_MARIAGE = duree;
    const fautes = this.buildFautesAlert();
    if (fautes) alerts.FAUTES_INVOQUEES = fautes;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length };
  });

  isFrance = computed(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private service: DivorceFauteService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    this.aiDataSignal.set(this.aiData);
    if (this.isFrance()) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);

    // Ré-appliquer le pré-fill quand `aiData` change après mount, sauf si
    // l'avocat a déjà saisi manuellement (provenance null sur un champ rempli)
    // ou si un résultat persisté est présent (form masqué).
    if (changes['aiData'] && !changes['aiData'].firstChange && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  /**
   * SF-FA-09-02 : pré-remplit le form depuis `aiData`. N'écrase jamais une
   * saisie avocat existante (la garde est faite par champ — provenance IA OK
   * ou champ encore vide).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // 1. Durée mariage (entier > 0).
    const ageMariage = (ai as { dureeMariageAnnees?: number | null }).dureeMariageAnnees;
    if (typeof ageMariage === 'number' && ageMariage > 0) {
      if (this.dureeMariageAnnees() === null || this.provenanceDureeMariage() === 'IA') {
        this.dureeMariageAnnees.set(ageMariage);
        this.provenanceDureeMariage.set('IA');
      }
    }

    // 2. Revenus annuels demandeur (> 0).
    const revDem = (ai as { revenusAnnuelsDemandeurEur?: number | null }).revenusAnnuelsDemandeurEur;
    if (typeof revDem === 'number' && revDem > 0) {
      if (this.revenusAnnuelsDemandeurEur() === null || this.provenanceRevenusDemandeur() === 'IA') {
        this.revenusAnnuelsDemandeurEur.set(revDem);
        this.provenanceRevenusDemandeur.set('IA');
      }
    }

    // 3. Revenus annuels défendeur (> 0).
    const revDef = (ai as { revenusAnnuelsDefendeurEur?: number | null }).revenusAnnuelsDefendeurEur;
    if (typeof revDef === 'number' && revDef > 0) {
      if (this.revenusAnnuelsDefendeurEur() === null || this.provenanceRevenusDefendeur() === 'IA') {
        this.revenusAnnuelsDefendeurEur.set(revDef);
        this.provenanceRevenusDefendeur.set('IA');
      }
    }

    // 4. Date dépôt assignation (ISO YYYY-MM-DD).
    const dateDepot = (ai as { dateDepotAssignation?: string | null }).dateDepotAssignation;
    if (typeof dateDepot === 'string' && dateDepot.length > 0) {
      if (this.dateDepotAssignation() === null || this.provenanceDateDepot() === 'IA') {
        this.dateDepotAssignation.set(dateDepot);
        this.provenanceDateDepot.set('IA');
      }
    }

    // 5. Fautes détectées par pipeline IA — no-op gracieux si absent.
    const fautesIa = ai.fautesDetectees;
    if (Array.isArray(fautesIa) && fautesIa.length > 0) {
      const filtered = fautesIa
        .map((f) => f?.toUpperCase())
        .filter((f): f is FauteCode => !!f && VALID_FAUTE_CODES.has(f));
      if (filtered.length > 0
          && (this.fautesInvoquees().length === 0 || this.provenanceFautesInvoquees() === 'IA')) {
        this.fautesInvoquees.set(filtered);
        this.provenanceFautesInvoquees.set('IA');
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Coherence alerts
  // ---------------------------------------------------------------------------

  private buildRevenusAlert(field: 'REVENUS_DEMANDEUR' | 'REVENUS_DEFENDEUR'): DivorceFauteCoherenceAlert | null {
    const ai = this.aiDataSignal() as
      | { revenusAnnuelsDemandeurEur?: number | null; revenusAnnuelsDefendeurEur?: number | null }
      | null
      | undefined;
    const aiValue = field === 'REVENUS_DEMANDEUR'
      ? ai?.revenusAnnuelsDemandeurEur
      : ai?.revenusAnnuelsDefendeurEur;
    const userValue = field === 'REVENUS_DEMANDEUR'
      ? this.revenusAnnuelsDemandeurEur()
      : this.revenusAnnuelsDefendeurEur();
    if (typeof aiValue !== 'number' || aiValue <= 0) return null;
    if (typeof userValue !== 'number' || userValue <= 0) return null;
    const ratio = Math.abs(userValue - aiValue) / aiValue;
    if (ratio <= REVENUS_DIVERGENCE_RATIO) return null;
    const display = `${aiValue.toLocaleString('fr-FR')} €`;
    const subject = field === 'REVENUS_DEMANDEUR' ? 'demandeur' : 'défendeur';
    return {
      field,
      expectedDisplay: display,
      reason: `Analyse du dossier : revenus annuels ${subject} ~${display}`,
    };
  }

  private buildDureeMariageAlert(): DivorceFauteCoherenceAlert | null {
    const ai = this.aiDataSignal() as { dureeMariageAnnees?: number | null } | null | undefined;
    const aiValue = ai?.dureeMariageAnnees;
    const userValue = this.dureeMariageAnnees();
    if (typeof aiValue !== 'number' || aiValue <= 0) return null;
    if (typeof userValue !== 'number' || userValue < 0) return null;
    if (Math.abs(userValue - aiValue) <= DUREE_MARIAGE_DIVERGENCE_YEARS) return null;
    return {
      field: 'DUREE_MARIAGE',
      expectedDisplay: `${aiValue} an(s)`,
      reason: `Analyse du dossier : durée du mariage ~${aiValue} an(s)`,
    };
  }

  private buildFautesAlert(): DivorceFauteCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiList = ai?.fautesDetectees;
    if (!Array.isArray(aiList) || aiList.length === 0) return null;
    const aiSet = new Set(aiList.map((f) => f?.toUpperCase()).filter(Boolean));
    const userSet = new Set(this.fautesInvoquees());
    if (aiSet.size === 0) return null;
    // Si avocat n'a sélectionné aucune faute alors que l'IA en a détecté.
    if (userSet.size === 0) {
      const sample = Array.from(aiSet).slice(0, 3).join(', ');
      return {
        field: 'FAUTES_INVOQUEES',
        expectedDisplay: sample,
        reason: `Analyse du dossier : fautes détectées (${sample})`,
      };
    }
    // Divergence symétrique : ensembles différents.
    const same = aiSet.size === userSet.size && Array.from(aiSet).every((f) => userSet.has(f as FauteCode));
    if (same) return null;
    const sample = Array.from(aiSet).slice(0, 3).join(', ');
    return {
      field: 'FAUTES_INVOQUEES',
      expectedDisplay: sample,
      reason: `Analyse du dossier : fautes détectées (${sample}) — divergence avec votre saisie`,
    };
  }

  // ---------------------------------------------------------------------------
  // Form handlers
  // ---------------------------------------------------------------------------

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const fautes = this.fautesInvoquees();
    const duree = this.dureeMariageAnnees();
    const revDem = this.revenusAnnuelsDemandeurEur();
    const revDef = this.revenusAnnuelsDefendeurEur();
    return fautes.length > 0
      && duree !== null && duree >= 0
      && revDem !== null && revDem >= 0
      && revDef !== null && revDef >= 0;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onFautesChange(value: FauteCode[]): void {
    this.fautesInvoquees.set(value ?? []);
    this.provenanceFautesInvoquees.set(null);
  }

  onPreuvesChange(value: boolean): void {
    this.preuvesDocumentaires.set(!!value);
  }

  onTortsAdverseChange(value: boolean): void {
    this.tortsAdverseInvoques.set(!!value);
  }

  onDureeMariageChange(value: number | null): void {
    this.dureeMariageAnnees.set(value);
    this.provenanceDureeMariage.set(null);
  }

  onRevenusDemandeurChange(value: number | null): void {
    this.revenusAnnuelsDemandeurEur.set(value);
    this.provenanceRevenusDemandeur.set(null);
  }

  onRevenusDefendeurChange(value: number | null): void {
    this.revenusAnnuelsDefendeurEur.set(value);
    this.provenanceRevenusDefendeur.set(null);
  }

  onDateDepotChange(value: string | null): void {
    this.dateDepotAssignation.set(value && value.length > 0 ? value : null);
    this.provenanceDateDepot.set(null);
  }

  // ---------------------------------------------------------------------------
  // Display helpers
  // ---------------------------------------------------------------------------

  fauteLabel = fauteLabel;
  verdictProbabiliteLabel = verdictProbabiliteLabel;
  verdictTortsLabel = verdictTortsLabel;

  verdictBannerClass(verdict: VerdictProbabilite): string {
    switch (verdict) {
      case 'ELEVEE': return 'df-verdict-banner df-verdict-banner--strong';
      case 'MOYENNE': return 'df-verdict-banner df-verdict-banner--medium';
      case 'FAIBLE': return 'df-verdict-banner df-verdict-banner--weak';
    }
  }

  verdictTortsClass(verdict: VerdictTorts): string {
    switch (verdict) {
      case 'EXCLUSIF_DEFENDEUR': return 'df-tort-card df-tort-card--exclusif';
      case 'PARTAGES': return 'df-tort-card df-tort-card--partages';
      case 'IMPREDICTIBLE': return 'df-tort-card df-tort-card--impredictible';
    }
  }

  alertBadgeLabel(alert: DivorceFauteCoherenceAlert): string {
    return `Incohérence détectée (${alert.expectedDisplay})`;
  }

  alertTooltip(alert: DivorceFauteCoherenceAlert): string {
    return alert.reason;
  }

  // ---------------------------------------------------------------------------
  // HTTP
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request = {
      fautesInvoquees: this.fautesInvoquees(),
      preuvesDocumentaires: this.preuvesDocumentaires(),
      tortsAdverseInvoques: this.tortsAdverseInvoques(),
      dureeMariageAnnees: this.dureeMariageAnnees()!,
      revenusAnnuelsDemandeurEur: this.revenusAnnuelsDemandeurEur()!,
      revenusAnnuelsDefendeurEur: this.revenusAnnuelsDefendeurEur()!,
      dateDepotAssignation: this.dateDepotAssignation(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse divorce pour faute calculée', 'OK', { duration: 2500 });
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
        this.applyPersistedResult(r);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  private applyPersistedResult(r: DivorceFauteResponse): void {
    this.result.set(r);
    this.fautesInvoquees.set(r.fautesInvoquees ?? []);
    this.preuvesDocumentaires.set(!!r.preuvesDocumentaires);
    this.tortsAdverseInvoques.set(!!r.tortsAdverseInvoques);
    this.dureeMariageAnnees.set(r.dureeMariageAnnees);
    this.revenusAnnuelsDemandeurEur.set(r.revenusAnnuelsDemandeurEur);
    this.revenusAnnuelsDefendeurEur.set(r.revenusAnnuelsDefendeurEur);
    this.dateDepotAssignation.set(r.dateDepotAssignation);
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceFautesInvoquees.set(null);
    this.provenanceDureeMariage.set(null);
    this.provenanceRevenusDemandeur.set(null);
    this.provenanceRevenusDefendeur.set(null);
    this.provenanceDateDepot.set(null);
    this.showForm.set(false);
  }
}
