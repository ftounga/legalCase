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
import { MajeursProtegesService } from '../../core/services/majeurs-proteges.service';
import {
  ActeEnvisage,
  ACTES_ENVISAGES_FR,
  ActeEnvisageOption,
  DemandeurFamilial,
  DEMANDEURS_FAMILIAUX_FR,
  DemandeurFamilialOption,
  FormeMandatProtection,
  FORMES_MANDAT_PROTECTION_FR,
  FormeMandatProtectionOption,
  MajeursProtegesRequest,
  MajeursProtegesResponse,
  RegimeProtection,
  REGIMES_PROTECTION_FR,
  RegimeProtectionOption,
  VerdictMajeurs,
  acteEnvisageLabel,
  demandeurFamilialLabel,
  formeMandatProtectionLabel,
  regimeProtectionLabel,
  verdictMajeursLabel,
} from '../../core/models/majeurs-proteges.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/**
 * SF-FA-25-02 : champs F-IA-03 audités par l'outil F-FA-25
 * (majeurs protégés, FRANCE uniquement, art. 425-494 Cciv et 494-1 Cciv).
 */
export type MajeursProtegesAlertField =
  | 'DATE_CERTIFICAT'
  | 'ALT_MENTALES'
  | 'CONSENTEMENT'
  | 'DEMANDEUR_FAMILIAL';

export type MajeursProtegesAlertSource = CoherenceAlertSource;
export type MajeursProtegesCoherenceAlert =
  CoherenceAlert<MajeursProtegesAlertField>;

const ISO_DATE_REGEX = /^\d{4}-\d{2}-\d{2}$/;

const VALID_REGIMES: ReadonlySet<string> = new Set<RegimeProtection>([
  'SAUVEGARDE_JUSTICE',
  'HABILITATION_FAMILIALE',
  'CURATELLE_SIMPLE',
  'CURATELLE_RENFORCEE',
  'TUTELLE',
  'MANDAT_PROTECTION_FUTURE',
]);

const VALID_DEMANDEURS: ReadonlySet<string> = new Set<DemandeurFamilial>([
  'CONJOINT',
  'ENFANT_MAJEUR',
  'PARENT',
  'FRERE_SOEUR',
  'TIERS_PROCHE',
  'MINISTERE_PUBLIC',
]);

const VALID_ACTES: ReadonlySet<string> = new Set<ActeEnvisage>([
  'GESTION_PATRIMOINE',
  'DECISIONS_LOGEMENT',
  'DECISIONS_SANTE',
  'DECISIONS_FAMILIALES',
  'ACTES_ETAT_CIVIL',
  'AUTRE',
]);

const VALID_FORMES_MANDAT: ReadonlySet<string> = new Set<FormeMandatProtection>([
  'NOTARIE',
  'SOUS_SEING_PRIVE',
]);

/**
 * SF-FA-25-02 : composant Angular standalone pour l'outil "Majeurs protégés —
 * choix du régime" (F-FA-25, FRANCE uniquement, art. 425-494 Cciv et
 * 494-1 Cciv pour l'habilitation familiale depuis 2016).
 *
 * Pattern de référence canonique : `ordonnance-protection-section` (art.
 * 515-9 Cciv, FRANCE), miroir le plus proche pour la palette navy/or/rouge
 * classique et la structure du form. Pattern card "recommandée" emprunté à
 * `autorite-parentale-section` (`expertiseRecommandee` + `auditionEnfants
 * Recommandee`).
 *
 * - Form : 1 mat-select RegimeProtection (6) + 4 toggles (alt mentales /
 *   alt physiques / certificat médical circonstancié / consentement) +
 *   1 datepicker `<input type="date">` certificat médical + 1 mat-select
 *   DemandeurFamilial (6) + 1 mat-select multiple ActeEnvisage (6) +
 *   3 toggles contextuels (urgence patrimoniale / patrimoine significatif
 *   / isolement social).
 * - Pré-fill IA via `aiData?: FamilleExtractedData | null` (8 fields).
 *   Provenance signal effacée au changement manuel.
 * - Validation F-IA-03 sur 4 fields via `CoherenceAlertBuilder` (multi-
 *   sources IA + F96 + PIECE_MANQUANTE).
 * - Affichage résultat : bannière verdict (palette navy/or/rouge classique),
 *   carte "Régime optimal recommandé" (peut différer du régime demandé),
 *   2 badges (audition obligatoire + expertise recommandée), délai
 *   procédure (mois), messages, baseJuridique + formule (JetBrains Mono).
 *
 * Palette : navy/or/rouge **classique**. Verdict ELEVEE = strong navy
 * (acceptabilité juridique de la mesure par le juge des contentieux de la
 * protection), MOYENNE = or, FAIBLE = rouge classique. Pas de gradation
 * rouge dominante (pas d'urgence absolue type < 72h — la sauvegarde
 * d'urgence se traite en référé séparé).
 */
@Component({
  selector: 'app-majeurs-proteges-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatChipsModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './majeurs-proteges-section.component.html',
  styleUrl: './majeurs-proteges-section.component.scss',
})
export class MajeursProtegesSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'MAJEURS PROTÉGÉS (FR) — ART. 425-494 / 494-1 CCIV';
  static readonly TOOL_ICON = 'supervisor_account';

  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';
  // Pré-fill IA + sources F-IA-03 (tous optionnels, null-safe partout).
  @Input() aiData?: Partial<FamilleExtractedData> | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal pour réactivité des `computed` F-IA-03.
  private aiDataSignal = signal<Partial<FamilleExtractedData> | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<MajeursProtegesResponse | null>(null);

  // Form fields.
  regimeProtectionDemande = signal<RegimeProtection | null>(null);
  altertationFacultesMentales = signal<boolean>(false);
  altertationFacultesPhysiques = signal<boolean>(false);
  certificatMedicalCirconstancie = signal<boolean>(false);
  dateCertificatMedical = signal<string | null>(null);
  consentementPersonneAProteger = signal<boolean>(false);
  demandeurFamilial = signal<DemandeurFamilial | null>(null);
  actesEnvisages = signal<ActeEnvisage[]>([]);
  urgencePatrimoniale = signal<boolean>(false);
  patrimoineSignificatif = signal<boolean>(false);
  isolementSocial = signal<boolean>(false);
  // SF-FA-25-06 : 4 nouveaux champs conditionnels selon régime.
  incapaciteGestionQuotidienne = signal<boolean>(false);
  altertationGrave = signal<boolean>(false);
  mandatPrealableSigne = signal<boolean>(false);
  formeMandatProtection = signal<FormeMandatProtection | null>(null);

  // Provenance IA par champ pré-remplissable (8 fields IA initiaux + 4 SF-06).
  provenanceRegimeProtectionDemande = signal<'IA' | null>(null);
  provenanceAltertationFacultesMentales = signal<'IA' | null>(null);
  provenanceAltertationFacultesPhysiques = signal<'IA' | null>(null);
  provenanceCertificatMedicalCirconstancie = signal<'IA' | null>(null);
  provenanceDateCertificatMedical = signal<'IA' | null>(null);
  provenanceConsentementPersonneAProteger = signal<'IA' | null>(null);
  provenanceDemandeurFamilial = signal<'IA' | null>(null);
  provenanceActesEnvisages = signal<'IA' | null>(null);
  // SF-FA-25-06 : provenance des 4 nouveaux champs.
  provenanceIncapaciteGestionQuotidienne = signal<'IA' | null>(null);
  provenanceAltertationGrave = signal<'IA' | null>(null);
  provenanceMandatPrealableSigne = signal<'IA' | null>(null);
  provenanceFormeMandatProtection = signal<'IA' | null>(null);

  // Map {sourceKey → explanations} pour le popover F-IA-03-15c (fail-open).
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  readonly regimesOptions: RegimeProtectionOption[] = REGIMES_PROTECTION_FR;
  readonly demandeursOptions: DemandeurFamilialOption[] = DEMANDEURS_FAMILIAUX_FR;
  readonly actesOptions: ActeEnvisageOption[] = ACTES_ENVISAGES_FR;
  readonly formesMandatOptions: FormeMandatProtectionOption[] = FORMES_MANDAT_PROTECTION_FR;

  // ---------------------------------------------------------------------------
  // SF-FA-25-06 : helpers de visibilité conditionnelle des nouveaux champs
  // selon le régime sélectionné. UX progressive disclosure.
  // ---------------------------------------------------------------------------

  /** Visible pour CURATELLE_RENFORCEE et TUTELLE (art. 472 + 440 al. 3). */
  showIncapaciteGestionQuotidienne = computed<boolean>(() => {
    const r = this.regimeProtectionDemande();
    return r === 'CURATELLE_RENFORCEE' || r === 'TUTELLE';
  });

  /** Visible pour TUTELLE seule (art. 440 al. 3 — altération grave). */
  showAltertationGrave = computed<boolean>(() => {
    return this.regimeProtectionDemande() === 'TUTELLE';
  });

  /** Visible pour MANDAT_PROTECTION_FUTURE (art. 477 — mandat préalable). */
  showMandatPrealableSigne = computed<boolean>(() => {
    return this.regimeProtectionDemande() === 'MANDAT_PROTECTION_FUTURE';
  });

  /** Visible pour MANDAT_PROTECTION_FUTURE ssi `mandatPrealableSigne === true`. */
  showFormeMandatProtection = computed<boolean>(() => {
    return this.regimeProtectionDemande() === 'MANDAT_PROTECTION_FUTURE'
      && this.mandatPrealableSigne() === true;
  });

  /**
   * Coherence alerts F-IA-03 — gate strict `showForm()` (pattern anti-bug
   * SF-IA-03-12 : pas de fallback `|| result()`).
   */
  coherenceAlerts = computed<Partial<Record<MajeursProtegesAlertField, MajeursProtegesCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<MajeursProtegesAlertField, MajeursProtegesCoherenceAlert>> = {};
    const dateAlert = this.buildDateCertificatAlert();
    if (dateAlert) alerts.DATE_CERTIFICAT = dateAlert;
    const altAlert = this.buildAltMentalesAlert();
    if (altAlert) alerts.ALT_MENTALES = altAlert;
    const consentAlert = this.buildConsentementAlert();
    if (consentAlert) alerts.CONSENTEMENT = consentAlert;
    const demAlert = this.buildDemandeurAlert();
    if (demAlert) alerts.DEMANDEUR_FAMILIAL = demAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  isFrance = computed(() => this.workspaceCountry === 'FRANCE');

  constructor(
    private service: MajeursProtegesService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
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
      this.loadSourceExplanations();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // Re-prefill quand aiData arrive après mount, tant que l'avocat n'a pas
    // saisi manuellement et qu'aucun résultat n'est affiché.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15c : mapping field → sourceKey pour le popover F-IA-03. */
  explanationFor(field: MajeursProtegesAlertField): SourceExplanation[] {
    const key = (() => {
      switch (field) {
        case 'DATE_CERTIFICAT': return 'FA25_DATE_CERTIFICAT';
        case 'ALT_MENTALES': return 'FA25_ALT_MENTALES';
        case 'CONSENTEMENT': return 'FA25_CONSENTEMENT';
        case 'DEMANDEUR_FAMILIAL': return 'FA25_DEMANDEUR_FAMILIAL';
      }
    })();
    return this.sourceExplanations().get(key) ?? [];
  }

  alertTooltip(alert: MajeursProtegesCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: MajeursProtegesCoherenceAlert): string {
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

  // ---------------------------------------------------------------------------
  // Form helpers / handlers
  // ---------------------------------------------------------------------------

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const reg = this.regimeProtectionDemande();
    const dem = this.demandeurFamilial();
    const date = this.dateCertificatMedical();
    const baseOk = reg !== null
      && dem !== null
      && this.actesEnvisages().length > 0
      && date !== null && ISO_DATE_REGEX.test(date)
      && (this.altertationFacultesMentales() || this.altertationFacultesPhysiques());
    if (!baseOk) return false;
    // SF-FA-25-06 : si mandat préalable signé est exigé (régime MANDAT_PROTECTION_FUTURE
    // avec toggle on), la forme du mandat est requise.
    if (this.showFormeMandatProtection() && this.formeMandatProtection() === null) {
      return false;
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  onRegimeChange(value: RegimeProtection | null): void {
    this.regimeProtectionDemande.set(value);
    this.provenanceRegimeProtectionDemande.set(null);
    // SF-FA-25-06 : reset des champs conditionnels qui ne sont plus visibles
    // pour éviter d'envoyer au backend des valeurs incohérentes avec le régime.
    if (value !== 'CURATELLE_RENFORCEE' && value !== 'TUTELLE') {
      this.incapaciteGestionQuotidienne.set(false);
      this.provenanceIncapaciteGestionQuotidienne.set(null);
    }
    if (value !== 'TUTELLE') {
      this.altertationGrave.set(false);
      this.provenanceAltertationGrave.set(null);
    }
    if (value !== 'MANDAT_PROTECTION_FUTURE') {
      this.mandatPrealableSigne.set(false);
      this.formeMandatProtection.set(null);
      this.provenanceMandatPrealableSigne.set(null);
      this.provenanceFormeMandatProtection.set(null);
    }
  }

  onAltMentalesChange(value: boolean): void {
    this.altertationFacultesMentales.set(!!value);
    this.provenanceAltertationFacultesMentales.set(null);
  }

  onAltPhysiquesChange(value: boolean): void {
    this.altertationFacultesPhysiques.set(!!value);
    this.provenanceAltertationFacultesPhysiques.set(null);
  }

  onCertificatChange(value: boolean): void {
    this.certificatMedicalCirconstancie.set(!!value);
    this.provenanceCertificatMedicalCirconstancie.set(null);
  }

  onDateCertificatChange(value: string | null): void {
    this.dateCertificatMedical.set(value && value.length > 0 ? value : null);
    this.provenanceDateCertificatMedical.set(null);
  }

  onConsentementChange(value: boolean): void {
    this.consentementPersonneAProteger.set(!!value);
    this.provenanceConsentementPersonneAProteger.set(null);
  }

  onDemandeurChange(value: DemandeurFamilial | null): void {
    this.demandeurFamilial.set(value);
    this.provenanceDemandeurFamilial.set(null);
  }

  onActesChange(value: ActeEnvisage[]): void {
    this.actesEnvisages.set(value ?? []);
    this.provenanceActesEnvisages.set(null);
  }

  onUrgencePatrimonialeChange(value: boolean): void {
    this.urgencePatrimoniale.set(!!value);
  }

  onPatrimoineSignificatifChange(value: boolean): void {
    this.patrimoineSignificatif.set(!!value);
  }

  onIsolementSocialChange(value: boolean): void {
    this.isolementSocial.set(!!value);
  }

  // SF-FA-25-06 : handlers des 4 nouveaux champs.

  onIncapaciteGestionQuotidienneChange(value: boolean): void {
    this.incapaciteGestionQuotidienne.set(!!value);
    this.provenanceIncapaciteGestionQuotidienne.set(null);
  }

  onAltertationGraveChange(value: boolean): void {
    this.altertationGrave.set(!!value);
    this.provenanceAltertationGrave.set(null);
  }

  onMandatPrealableSigneChange(value: boolean): void {
    this.mandatPrealableSigne.set(!!value);
    this.provenanceMandatPrealableSigne.set(null);
    // Si l'avocat décoche le mandat préalable, la forme n'a plus de sens —
    // on reset pour rester cohérent avec le contrat backend (formeMandatProtection
    // ne peut être renseignée que si mandatPrealableSigne === true).
    if (!value) {
      this.formeMandatProtection.set(null);
      this.provenanceFormeMandatProtection.set(null);
    }
  }

  onFormeMandatProtectionChange(value: FormeMandatProtection | null): void {
    this.formeMandatProtection.set(value);
    this.provenanceFormeMandatProtection.set(null);
  }

  // ---------------------------------------------------------------------------
  // HTTP : load + calculate
  // ---------------------------------------------------------------------------

  calculate(): void {
    if (!this.formValid()) return;
    const request: MajeursProtegesRequest = {
      regimeProtectionDemande: this.regimeProtectionDemande()!,
      altertationFacultesMentales: this.altertationFacultesMentales(),
      altertationFacultesPhysiques: this.altertationFacultesPhysiques(),
      certificatMedicalCirconstancie: this.certificatMedicalCirconstancie(),
      dateCertificatMedical: this.dateCertificatMedical()!,
      consentementPersonneAProteger: this.consentementPersonneAProteger(),
      demandeurFamilial: this.demandeurFamilial()!,
      actesEnvisages: this.actesEnvisages(),
      urgencePatrimoniale: this.urgencePatrimoniale(),
      patrimoineSignificatif: this.patrimoineSignificatif(),
      isolementSocial: this.isolementSocial(),
      // SF-FA-25-06 : 4 nouveaux champs envoyés tels quels (le backend
      // accepte des null/false pour les régimes auxquels ils ne s'appliquent
      // pas — backward-compatible).
      incapaciteGestionQuotidienne: this.incapaciteGestionQuotidienne(),
      altertationGrave: this.altertationGrave(),
      mandatPrealableSigne: this.mandatPrealableSigne(),
      formeMandatProtection: this.formeMandatProtection(),
    };
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Analyse majeurs protégés calculée', 'OK', { duration: 2500 });
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

  private applyPersistedResult(r: MajeursProtegesResponse): void {
    this.result.set(r);
    this.regimeProtectionDemande.set(r.regimeProtectionDemande);
    this.altertationFacultesMentales.set(!!r.altertationFacultesMentales);
    this.altertationFacultesPhysiques.set(!!r.altertationFacultesPhysiques);
    this.certificatMedicalCirconstancie.set(!!r.certificatMedicalCirconstancie);
    this.dateCertificatMedical.set(r.dateCertificatMedical);
    this.consentementPersonneAProteger.set(!!r.consentementPersonneAProteger);
    this.demandeurFamilial.set(r.demandeurFamilial);
    this.actesEnvisages.set(r.actesEnvisages ?? []);
    this.urgencePatrimoniale.set(!!r.urgencePatrimoniale);
    this.patrimoineSignificatif.set(!!r.patrimoineSignificatif);
    this.isolementSocial.set(!!r.isolementSocial);
    // SF-FA-25-06 : reprise des 4 nouveaux champs persistés.
    this.incapaciteGestionQuotidienne.set(!!r.incapaciteGestionQuotidienne);
    this.altertationGrave.set(!!r.altertationGrave);
    this.mandatPrealableSigne.set(!!r.mandatPrealableSigne);
    const persistedForme = r.formeMandatProtection;
    this.formeMandatProtection.set(
      persistedForme === 'NOTARIE' || persistedForme === 'SOUS_SEING_PRIVE'
        ? persistedForme
        : null,
    );
    // Valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceRegimeProtectionDemande.set(null);
    this.provenanceAltertationFacultesMentales.set(null);
    this.provenanceAltertationFacultesPhysiques.set(null);
    this.provenanceCertificatMedicalCirconstancie.set(null);
    this.provenanceDateCertificatMedical.set(null);
    this.provenanceConsentementPersonneAProteger.set(null);
    this.provenanceDemandeurFamilial.set(null);
    this.provenanceActesEnvisages.set(null);
    this.provenanceIncapaciteGestionQuotidienne.set(null);
    this.provenanceAltertationGrave.set(null);
    this.provenanceMandatPrealableSigne.set(null);
    this.provenanceFormeMandatProtection.set(null);
    this.showForm.set(false);
  }

  /**
   * Pré-fill 8 champs IA depuis `aiData`. No-op gracieux si champ absent.
   * N'écrase jamais une saisie avocat (la garde est par champ : provenance
   * IA OK, valeur null, ou booléen encore false par défaut).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // 1. Régime protection demandé.
    const regAi = ai.regimeProtectionDemande;
    if (typeof regAi === 'string' && regAi.length > 0) {
      const upper = regAi.toUpperCase();
      if (VALID_REGIMES.has(upper)) {
        if (this.regimeProtectionDemande() === null
            || this.provenanceRegimeProtectionDemande() === 'IA') {
          this.regimeProtectionDemande.set(upper as RegimeProtection);
          this.provenanceRegimeProtectionDemande.set('IA');
        }
      }
    }

    // 2. Altération facultés mentales.
    if (typeof ai.altertationFacultesMentales === 'boolean') {
      if (this.provenanceAltertationFacultesMentales() === 'IA'
          || this.altertationFacultesMentales() === false) {
        this.altertationFacultesMentales.set(ai.altertationFacultesMentales);
        if (ai.altertationFacultesMentales) {
          this.provenanceAltertationFacultesMentales.set('IA');
        }
      }
    }

    // 3. Altération facultés physiques.
    if (typeof ai.altertationFacultesPhysiques === 'boolean') {
      if (this.provenanceAltertationFacultesPhysiques() === 'IA'
          || this.altertationFacultesPhysiques() === false) {
        this.altertationFacultesPhysiques.set(ai.altertationFacultesPhysiques);
        if (ai.altertationFacultesPhysiques) {
          this.provenanceAltertationFacultesPhysiques.set('IA');
        }
      }
    }

    // 4. Certificat médical circonstancié.
    if (typeof ai.certificatMedicalCirconstancieDetected === 'boolean') {
      if (this.provenanceCertificatMedicalCirconstancie() === 'IA'
          || this.certificatMedicalCirconstancie() === false) {
        this.certificatMedicalCirconstancie.set(ai.certificatMedicalCirconstancieDetected);
        if (ai.certificatMedicalCirconstancieDetected) {
          this.provenanceCertificatMedicalCirconstancie.set('IA');
        }
      }
    }

    // 5. Date certificat médical (ISO).
    if (typeof ai.dateCertificatMedicalDetected === 'string'
        && ISO_DATE_REGEX.test(ai.dateCertificatMedicalDetected)) {
      if (this.dateCertificatMedical() === null
          || this.provenanceDateCertificatMedical() === 'IA') {
        this.dateCertificatMedical.set(ai.dateCertificatMedicalDetected);
        this.provenanceDateCertificatMedical.set('IA');
      }
    }

    // 6. Consentement personne à protéger (boolean — important pour
    // distinguer habilitation familiale vs tutelle).
    if (typeof ai.consentementPersonneAProtegerDetected === 'boolean') {
      if (this.provenanceConsentementPersonneAProteger() === 'IA'
          || this.consentementPersonneAProteger() === false) {
        this.consentementPersonneAProteger.set(ai.consentementPersonneAProtegerDetected);
        if (ai.consentementPersonneAProtegerDetected) {
          this.provenanceConsentementPersonneAProteger.set('IA');
        }
      }
    }

    // 7. Demandeur familial.
    const demAi = ai.demandeurFamilialDetected;
    if (typeof demAi === 'string' && demAi.length > 0) {
      const upper = demAi.toUpperCase();
      if (VALID_DEMANDEURS.has(upper)) {
        if (this.demandeurFamilial() === null
            || this.provenanceDemandeurFamilial() === 'IA') {
          this.demandeurFamilial.set(upper as DemandeurFamilial);
          this.provenanceDemandeurFamilial.set('IA');
        }
      }
    }

    // 8. Actes envisagés (multi).
    if (Array.isArray(ai.actesEnvisagesDetected) && ai.actesEnvisagesDetected.length > 0) {
      const filtered = ai.actesEnvisagesDetected
        .map((a) => a?.toUpperCase())
        .filter((a): a is ActeEnvisage => !!a && VALID_ACTES.has(a));
      if (filtered.length > 0
          && (this.actesEnvisages().length === 0
              || this.provenanceActesEnvisages() === 'IA')) {
        this.actesEnvisages.set(filtered);
        this.provenanceActesEnvisages.set('IA');
      }
    }

    // SF-FA-25-06 : 4 champs spécifiques aux régimes 3-6.

    // 9. Incapacité de gestion quotidienne (CURATELLE_RENFORCEE / TUTELLE).
    if (typeof ai.incapaciteGestionQuotidienneDetected === 'boolean') {
      if (this.provenanceIncapaciteGestionQuotidienne() === 'IA'
          || this.incapaciteGestionQuotidienne() === false) {
        this.incapaciteGestionQuotidienne.set(ai.incapaciteGestionQuotidienneDetected);
        if (ai.incapaciteGestionQuotidienneDetected) {
          this.provenanceIncapaciteGestionQuotidienne.set('IA');
        }
      }
    }

    // 10. Altération grave (TUTELLE).
    if (typeof ai.altertationGraveDetected === 'boolean') {
      if (this.provenanceAltertationGrave() === 'IA'
          || this.altertationGrave() === false) {
        this.altertationGrave.set(ai.altertationGraveDetected);
        if (ai.altertationGraveDetected) {
          this.provenanceAltertationGrave.set('IA');
        }
      }
    }

    // 11. Mandat préalable signé (MANDAT_PROTECTION_FUTURE).
    if (typeof ai.mandatPrealableSigneDetected === 'boolean') {
      if (this.provenanceMandatPrealableSigne() === 'IA'
          || this.mandatPrealableSigne() === false) {
        this.mandatPrealableSigne.set(ai.mandatPrealableSigneDetected);
        if (ai.mandatPrealableSigneDetected) {
          this.provenanceMandatPrealableSigne.set('IA');
        }
      }
    }

    // 12. Forme mandat protection (NOTARIE / SOUS_SEING_PRIVE).
    const formeAi = ai.formeMandatProtectionDetected;
    if (typeof formeAi === 'string' && formeAi.length > 0) {
      const upper = formeAi.toUpperCase();
      if (VALID_FORMES_MANDAT.has(upper)) {
        if (this.formeMandatProtection() === null
            || this.provenanceFormeMandatProtection() === 'IA') {
          this.formeMandatProtection.set(upper as FormeMandatProtection);
          this.provenanceFormeMandatProtection.set('IA');
        }
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Builders d'alertes F-IA-03 (un par field) — via CoherenceAlertBuilder.
  // ---------------------------------------------------------------------------

  /**
   * Date certificat médical — divergence ISO stricte entre IA et user.
   * Multi-sources : IA + F96 + PIECE_MANQUANTE (pour le certificat médical).
   */
  private buildDateCertificatAlert(): MajeursProtegesCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiDate = ai?.dateCertificatMedicalDetected;
    const user = this.dateCertificatMedical();
    if (!user) return null;
    if (!aiDate || !ISO_DATE_REGEX.test(aiDate)) return null;
    if (user === aiDate) return null;

    const builder = CoherenceAlertBuilder.forField<MajeursProtegesAlertField>('DATE_CERTIFICAT')
      .addSource('IA', {
        expectedDisplay: aiDate,
        reason: `Analyse du dossier : certificat médical daté ${aiDate}`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA25_DATE_CERTIFICAT') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: aiDate,
        reason: `Checklist procédurale : date attendue ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA25_DATE_CERTIFICAT', 'CERTIFICAT_MEDICAL_CIRCONSTANCIE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Altération facultés mentales — alerte si IA dit `true` et user a coché
   * `false` (sens critique : sous-évaluation par l'avocat alors que le
   * dossier comporte des indices médicaux).
   */
  private buildAltMentalesAlert(): MajeursProtegesCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (typeof ai?.altertationFacultesMentales !== 'boolean') return null;
    if (ai.altertationFacultesMentales === false) return null;
    if (this.altertationFacultesMentales() === true) return null;

    const display = 'Altération facultés mentales détectée';
    const builder = CoherenceAlertBuilder.forField<MajeursProtegesAlertField>('ALT_MENTALES')
      .addSource('IA', {
        expectedDisplay: display,
        reason: 'Analyse du dossier : altération des facultés mentales détectée',
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA25_ALT_MENTALES') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante([
      'FA25_ALT_MENTALES',
      'CERTIFICAT_MEDICAL_CIRCONSTANCIE',
      'EXPERTISE_PSYCHIATRIQUE',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Consentement personne à protéger — divergence boolean (sens critique :
   * détermine si une habilitation familiale est possible vs imposer une
   * tutelle).
   */
  private buildConsentementAlert(): MajeursProtegesCoherenceAlert | null {
    const ai = this.aiDataSignal();
    if (typeof ai?.consentementPersonneAProtegerDetected !== 'boolean') return null;
    if (this.consentementPersonneAProteger() === ai.consentementPersonneAProtegerDetected) return null;

    const display = ai.consentementPersonneAProtegerDetected
      ? 'Consentement détecté'
      : 'Pas de consentement détecté';
    const builder = CoherenceAlertBuilder.forField<MajeursProtegesAlertField>('CONSENTEMENT')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : ${display.toLowerCase()}`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA25_CONSENTEMENT') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : ${chk.expectedValue}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante([
      'FA25_CONSENTEMENT',
      'AUDITION_PERSONNE_PROTEGEE',
      'CONSENTEMENT_ECRIT',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Demandeur familial — divergence enum stricte (6 valeurs).
   */
  private buildDemandeurAlert(): MajeursProtegesCoherenceAlert | null {
    const ai = this.aiDataSignal();
    const aiRaw = ai?.demandeurFamilialDetected;
    if (typeof aiRaw !== 'string' || aiRaw.length === 0) return null;
    const aiDem = aiRaw.toUpperCase();
    if (!VALID_DEMANDEURS.has(aiDem)) return null;
    const userDem = this.demandeurFamilial();
    if (userDem === null) return null;
    if (userDem === aiDem) return null;

    const display = demandeurFamilialLabel(aiDem as DemandeurFamilial);
    const builder = CoherenceAlertBuilder.forField<MajeursProtegesAlertField>('DEMANDEUR_FAMILIAL')
      .addSource('IA', {
        expectedDisplay: display,
        reason: `Analyse du dossier : demandeur "${display}"`,
      });

    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== 'FA25_DEMANDEUR_FAMILIAL') continue;
      if (!chk.expectedValue) continue;
      builder.addSource('F96', {
        expectedDisplay: display,
        reason: `Checklist procédurale : demandeur attendu "${chk.expectedValue}"${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }

    const piece = this.findPieceManquante(['FA25_DEMANDEUR_FAMILIAL', 'JUSTIFICATIF_LIEN_PARENTE']);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /** Retourne le `texte` d'une `PieceManquanteEntry` matchant un code. */
  private findPieceManquante(acceptedCodes: string[]): string | null {
    const norm = new Set(acceptedCodes.map((c) => c.toUpperCase()));
    for (const p of this.piecesManquantesSignal()) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (norm.has(code)) return p.texte;
    }
    return null;
  }

  // ---------------------------------------------------------------------------
  // Display helpers
  // ---------------------------------------------------------------------------

  regimeProtectionLabel = regimeProtectionLabel;
  demandeurFamilialLabel = demandeurFamilialLabel;
  acteEnvisageLabel = acteEnvisageLabel;
  verdictMajeursLabel = verdictMajeursLabel;
  formeMandatProtectionLabel = formeMandatProtectionLabel;

  /**
   * Palette navy/or/rouge classique. Verdict ELEVEE = strong navy (acceptabi
   * lité juridique élevée), MOYENNE = or, FAIBLE = rouge classique. Pas de
   * gradation rouge dominante (urgence absolue type < 72h non applicable —
   * sauvegarde d'urgence se traite via art. 433 référé séparément).
   */
  verdictBannerClass(verdict: VerdictMajeurs): string {
    switch (verdict) {
      case 'ELEVEE': return 'mp-verdict-banner mp-verdict-banner--strong';
      case 'MOYENNE': return 'mp-verdict-banner mp-verdict-banner--medium';
      case 'FAIBLE': return 'mp-verdict-banner mp-verdict-banner--weak';
    }
  }

  /**
   * Vrai si le régime optimal recommandé diffère du régime demandé par
   * l'avocat — utilisé pour mettre en avant la carte "Régime optimal
   * recommandé" (point différenciant de l'outil).
   */
  isRegimeRecommandationDifferente(): boolean {
    const r = this.result();
    if (!r) return false;
    return r.regimeOptimalRecommande !== r.regimeProtectionDemande;
  }
}
