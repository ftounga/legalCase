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
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RecherchePaterniteService } from '../../core/services/recherche-paternite.service';
import {
  QUALITE_DEMANDEUR_RECHERCHE_LABELS,
  QualiteDuDemandeurRecherche,
  RecherchePaterniteRequest,
  RecherchePaterniteResponse,
  VerdictRecevabiliteRecherche,
} from '../../core/models/recherche-paternite.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { RecherchePaternitePrefillRules } from './recherche-paternite-section-prefill-rules';

/**
 * SF-FA-18-06 : champs d'alerte F-IA-03 exposés par l'outil
 * "Action en recherche de paternité" (art. 327 + 340 + 16-11 + 321 Cciv).
 */
export type RecherchePaterniteAlertField =
  | 'QUALITE_DEMANDEUR'
  | 'POSSESSION_ETAT'
  | 'EXPERTISE_ADN'
  | 'REFUS_ADN'
  | 'MOTIFS_SERIEUX';

export type RecherchePaterniteAlertSource = CoherenceAlertSource;
export type RecherchePaterniteCoherenceAlert =
  CoherenceAlert<RecherchePaterniteAlertField>;

/**
 * SF-FA-18-06 : outil décisionnel "Action en recherche de paternité"
 * (FR — art. 327 + 340 + 16-11 + 321 Cciv).
 *
 * Symétrique inverse de la contestation (SF-04) : la contestation annule un
 * lien existant, la recherche fait *créer* un lien inexistant. 3 qualités
 * du demandeur (ENFANT_MAJEUR / REPRESENTANT_LEGAL_MINEUR / MERE), avec
 * délai de prescription 10 ans à compter de la majorité (art. 321), et
 * orientation systématique vers l'expertise ADN (art. 16-11 +
 * Cass. 1ère civ. 28/03/2000) — présomption en cas de refus du défendeur.
 *
 * FR uniquement (bannière info si dossier BE — équivalent CC art. 322 et s.
 * au backlog jumeau).
 *
 * Consomme l'API SF-FA-18-05 (mergée PR #664). Affiché conditionnellement par
 * le panel F-IA-04 (tool_id 'F-FA-18-recherche-paternite' — migration 183).
 *
 * Pattern de référence : `contestation-paternite-section` (PR #663 —
 * chantier F-FA-18 jumeau).
 */
@Component({
  selector: 'app-recherche-paternite-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './recherche-paternite-section.component.html',
  styleUrl: './recherche-paternite-section.component.scss',
})
export class RecherchePaterniteSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'RECHERCHE DE PATERNITÉ (FR)';
  static readonly TOOL_ICON = 'family_restroom';

  /** F-236 SF-236-02 — compteur miroir prefillFromAi via helper. */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    procedureChecks?: unknown[];
    aiQuestions?: unknown[];
    piecesManquantes?: unknown[];
    triggerEvents?: unknown[];
    workspaceCountry?: string;
  }): number {
    return RecherchePaternitePrefillRules.computePrefillCount(input);
  }

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal des inputs IA pour que `computed` réagisse.
  private aiDataSignal = signal<FamilleExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<RecherchePaterniteResponse | null>(null);

  // Form fields
  qualiteDuDemandeur = signal<QualiteDuDemandeurRecherche | null>(null);
  /** ISO YYYY-MM-DD via input type="date". */
  dateNaissanceEnfant = signal<string | null>(null);
  presomptionPossessionEtat = signal<boolean>(false);
  expertiseAdnDemandee = signal<boolean>(false);
  pereDesigneRefuseADN = signal<boolean>(false);
  motifsSerieux = signal<boolean>(false);

  /** Provenance IA pour les champs pré-remplissables. */
  provenanceQualite = signal<'IA' | null>(null);
  provenanceDateNaissance = signal<'IA' | null>(null);
  provenancePossessionEtat = signal<'IA' | null>(null);
  provenanceExpertiseAdn = signal<'IA' | null>(null);
  provenanceRefusAdn = signal<'IA' | null>(null);
  provenanceMotifsSerieux = signal<'IA' | null>(null);

  /** Listes pour mat-radio. */
  readonly qualiteOptions = QUALITE_DEMANDEUR_RECHERCHE_LABELS;

  isFrance = computed<boolean>(() => this.workspaceCountry === 'FRANCE');

  /**
   * Alertes de cohérence F-IA-03 par field. Gate : uniquement en mode formulaire.
   */
  coherenceAlerts = computed<Partial<Record<RecherchePaterniteAlertField,
      RecherchePaterniteCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<RecherchePaterniteAlertField,
        RecherchePaterniteCoherenceAlert>> = {};
    const qa = this.buildQualiteAlert();
    if (qa) alerts.QUALITE_DEMANDEUR = qa;
    const pe = this.buildPossessionEtatAlert();
    if (pe) alerts.POSSESSION_ETAT = pe;
    const ea = this.buildExpertiseAdnAlert();
    if (ea) alerts.EXPERTISE_ADN = ea;
    const ra = this.buildRefusAdnAlert();
    if (ra) alerts.REFUS_ADN = ra;
    const ms = this.buildMotifsSerieuxAlert();
    if (ms) alerts.MOTIFS_SERIEUX = ms;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  /**
   * Chip alerte délai de prescription :
   * - rouge / critical si ≤ 6 mois ou prescrit (négatif).
   * - or / warning si > 6 et < 12 mois.
   * - null sinon (pas de chip).
   */
  delaiPrescriptionAlert = computed<'critical' | 'warning' | null>(() => {
    const r = this.result();
    if (!r) return null;
    const restant = r.delaiPrescriptionRestantMois;
    if (restant <= 6) return 'critical';
    if (restant < 12) return 'warning';
    return null;
  });

  constructor(
    private service: RecherchePaterniteService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    if (this.isFrance()) {
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // Ré-applique le prefill si aiData change post-mount, en mode formulaire
    // ET sans résultat persisté chargé.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isFrance() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  alertTooltip(alert: RecherchePaterniteCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: RecherchePaterniteCoherenceAlert): string {
    const prefix = (() => {
      switch (alert.source) {
        case 'F96': return 'Incohérence Checklist procédurale';
        case 'QUESTION_IA': return 'Incohérence Question complémentaire';
        case 'IA': return 'Incohérence détectée';
        case 'PIECE_MANQUANTE': return 'Pièce manquante';
        case 'MULTI': return 'Incohérence multiple';
      }
    })();
    return `${prefix} (${alert.expectedDisplay})`;
  }

  /**
   * Helper interne factorisé pour construire une alerte multi-sources sur un
   * champ booléen.
   */
  private buildBooleanAlert(
    field: RecherchePaterniteAlertField,
    userVal: boolean,
    critereCode: string,
    iaVal: boolean | null | undefined,
    trueLabel: string,
    falseLabel: string,
    fieldLabel: string,
  ): RecherchePaterniteCoherenceAlert | null {
    const builder = CoherenceAlertBuilder
        .forField<RecherchePaterniteAlertField>(field);

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== critereCode) continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      const expected = this.parseBoolFromIa(ev);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: expected ? trueLabel : falseLabel,
        reason: `Checklist procédurale : ${fieldLabel} ${expected ? 'vérifié' : 'non vérifié'}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== critereCode) continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue;
      if (!ev) continue;
      const expected = this.parseBoolFromIa(ev);
      if (expected === null || expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: expected ? trueLabel : falseLabel,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.*Detected`.
    if (iaVal !== null && iaVal !== undefined && iaVal !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: iaVal ? trueLabel : falseLabel,
        reason: `Analyse du dossier : ${fieldLabel} ${iaVal ? 'vérifié' : 'non vérifié'}`,
      });
    }

    // 4. PIECE_MANQUANTE — contributor enrichissant.
    const piece = this.findPieceManquante([critereCode]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private buildQualiteAlert(): RecherchePaterniteCoherenceAlert | null {
    const userVal = this.qualiteDuDemandeur();
    if (userVal === null) return null;
    const iaVal = this.aiDataSignal()?.qualiteDuDemandeurRechercheDetected;
    if (!iaVal || iaVal === userVal) return null;

    const labelOf = (q: QualiteDuDemandeurRecherche) =>
      QUALITE_DEMANDEUR_RECHERCHE_LABELS.find((o) => o.code === q)?.label ?? q;

    const builder = CoherenceAlertBuilder
        .forField<RecherchePaterniteAlertField>('QUALITE_DEMANDEUR');
    builder.addSource('IA', {
      expectedDisplay: labelOf(iaVal),
      reason: `Analyse du dossier : qualité du demandeur = ${labelOf(iaVal)}`,
    });
    return builder.build();
  }

  private buildPossessionEtatAlert(): RecherchePaterniteCoherenceAlert | null {
    return this.buildBooleanAlert(
      'POSSESSION_ETAT',
      this.presomptionPossessionEtat(),
      'RECHERCHE_PATERNITE_POSSESSION_ETAT',
      this.aiDataSignal()?.presomptionPossessionEtatRechercheDetected,
      'Possession d\'état présumée',
      'Pas de possession d\'état',
      'présomption de possession d\'état',
    );
  }

  private buildExpertiseAdnAlert(): RecherchePaterniteCoherenceAlert | null {
    return this.buildBooleanAlert(
      'EXPERTISE_ADN',
      this.expertiseAdnDemandee(),
      'RECHERCHE_PATERNITE_EXPERTISE_ADN',
      this.aiDataSignal()?.expertiseAdnDemandeeRechercheDetected,
      'Expertise ADN demandée',
      'Pas d\'expertise ADN',
      'expertise ADN demandée',
    );
  }

  private buildRefusAdnAlert(): RecherchePaterniteCoherenceAlert | null {
    return this.buildBooleanAlert(
      'REFUS_ADN',
      this.pereDesigneRefuseADN(),
      'RECHERCHE_PATERNITE_REFUS_ADN',
      this.aiDataSignal()?.pereDesigneRefuseADNDetected,
      'Refus d\'ADN du père désigné',
      'Pas de refus ADN',
      'refus d\'ADN du père désigné',
    );
  }

  private buildMotifsSerieuxAlert(): RecherchePaterniteCoherenceAlert | null {
    return this.buildBooleanAlert(
      'MOTIFS_SERIEUX',
      this.motifsSerieux(),
      'RECHERCHE_PATERNITE_MOTIFS_SERIEUX',
      this.aiDataSignal()?.motifsSerieuxRechercheDetected,
      'Motifs sérieux',
      'Pas de motifs sérieux',
      'motifs sérieux',
    );
  }

  private parseBoolFromIa(value: string | null | undefined): boolean | null {
    if (value === null || value === undefined) return null;
    const v = value.toString().trim().toLowerCase();
    if (!v) return null;
    if (v === 'oui' || v === 'true' || v === 'vrai' || v === '1' || v === 'yes') return true;
    if (v === 'non' || v === 'false' || v === 'faux' || v === '0' || v === 'no') return false;
    return null;
  }

  private findPieceManquante(acceptedCodes: string[]): string | null {
    const norm = new Set(acceptedCodes.map((c) => c.toUpperCase()));
    for (const p of this.piecesManquantesSignal()) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (norm.has(code)) return p.texte;
    }
    return null;
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    if (!this.qualiteDuDemandeur()) return false;
    if (!this.dateNaissanceEnfant()) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers onChange — effacent la provenance IA au 1er changement manuel.
  onQualiteChange(value: QualiteDuDemandeurRecherche | null): void {
    this.qualiteDuDemandeur.set(value);
    this.provenanceQualite.set(null);
  }

  onDateNaissanceChange(value: string | null): void {
    this.dateNaissanceEnfant.set(value || null);
    this.provenanceDateNaissance.set(null);
  }

  onPossessionEtatChange(value: boolean): void {
    this.presomptionPossessionEtat.set(value);
    this.provenancePossessionEtat.set(null);
  }

  onExpertiseAdnChange(value: boolean): void {
    this.expertiseAdnDemandee.set(value);
    this.provenanceExpertiseAdn.set(null);
  }

  onRefusAdnChange(value: boolean): void {
    this.pereDesigneRefuseADN.set(value);
    this.provenanceRefusAdn.set(null);
  }

  onMotifsSerieuxChange(value: boolean): void {
    this.motifsSerieux.set(value);
    this.provenanceMotifsSerieux.set(null);
  }

  /**
   * SF-FA-18-06 : pré-fill depuis `aiData` (FamilleExtractedData).
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le champ est encore vide / `null` (préserve les edits)
   * - n'écrase jamais si provenance !== 'IA'
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé.
    const h = { aiData: this.aiDataSignal() };
    const iaQ = RecherchePaternitePrefillRules.computeQualite(h);
    if (iaQ !== null && (this.qualiteDuDemandeur() === null || this.provenanceQualite() === 'IA')) {
      this.qualiteDuDemandeur.set(iaQ as any);
      this.provenanceQualite.set('IA');
    }
    const iaDN = RecherchePaternitePrefillRules.computeDateNaissance(h);
    if (iaDN !== null && (this.dateNaissanceEnfant() === null || this.provenanceDateNaissance() === 'IA')) {
      this.dateNaissanceEnfant.set(iaDN);
      this.provenanceDateNaissance.set('IA');
    }
    const iaPE = RecherchePaternitePrefillRules.computePresomptionPossessionEtat(h);
    if (iaPE !== null && (!this.presomptionPossessionEtat() || this.provenancePossessionEtat() === 'IA')) {
      this.presomptionPossessionEtat.set(iaPE);
      this.provenancePossessionEtat.set('IA');
    }
    const iaEA = RecherchePaternitePrefillRules.computeExpertiseAdn(h);
    if (iaEA !== null && (!this.expertiseAdnDemandee() || this.provenanceExpertiseAdn() === 'IA')) {
      this.expertiseAdnDemandee.set(iaEA);
      this.provenanceExpertiseAdn.set('IA');
    }
    const iaRA = RecherchePaternitePrefillRules.computeRefusAdn(h);
    if (iaRA !== null && (!this.pereDesigneRefuseADN() || this.provenanceRefusAdn() === 'IA')) {
      this.pereDesigneRefuseADN.set(iaRA);
      this.provenanceRefusAdn.set('IA');
    }
    const iaMS = RecherchePaternitePrefillRules.computeMotifsSerieux(h);
    if (iaMS !== null && (!this.motifsSerieux() || this.provenanceMotifsSerieux() === 'IA')) {
      this.motifsSerieux.set(iaMS);
      this.provenanceMotifsSerieux.set('IA');
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: RecherchePaterniteRequest = {
      qualiteDuDemandeur: this.qualiteDuDemandeur()!,
      dateNaissanceEnfant: this.dateNaissanceEnfant()!,
      presomptionPossessionEtat: this.presomptionPossessionEtat(),
      expertiseAdnDemandee: this.expertiseAdnDemandee(),
      pereDesigneRefuseADN: this.pereDesigneRefuseADN(),
      motifsSerieux: this.motifsSerieux(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Recherche de paternité analysée', 'OK', { duration: 2500 });
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
        this.result.set(r);
        // Provenance reset — valeurs persistées = saisie avocat.
        this.provenanceQualite.set(null);
        this.provenanceDateNaissance.set(null);
        this.provenancePossessionEtat.set(null);
        this.provenanceExpertiseAdn.set(null);
        this.provenanceRefusAdn.set(null);
        this.provenanceMotifsSerieux.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        // Fallback pré-fill IA uniquement ici (pas si GET 200).
        this.prefillFromAi();
        this.loading.set(false);
      },
    });
  }

  /**
   * Classe CSS du bandeau verdict.
   * - ELEVEE → navy/info (recevabilité forte).
   * - MOYENNE → or/warning (faisceau d'indices partiel).
   * - FAIBLE → rouge/critical (prescription, absence d'éléments).
   */
  bannerClass(verdict: VerdictRecevabiliteRecherche | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'rch-pat-banner rch-pat-banner--info';
      case 'MOYENNE': return 'rch-pat-banner rch-pat-banner--warning';
      case 'FAIBLE': return 'rch-pat-banner rch-pat-banner--critical';
      default: return 'rch-pat-banner rch-pat-banner--info';
    }
  }

  /** Libellé humain d'une qualité du demandeur. */
  qualiteLabel(code: QualiteDuDemandeurRecherche | null | undefined): string {
    if (!code) return '';
    const opt = QUALITE_DEMANDEUR_RECHERCHE_LABELS.find((o) => o.code === code);
    return opt?.label ?? code;
  }
}
