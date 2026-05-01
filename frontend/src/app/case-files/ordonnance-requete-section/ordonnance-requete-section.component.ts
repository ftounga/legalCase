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
import { MatRadioModule } from '@angular/material/radio';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { OrdonnanceRequeteService } from '../../core/services/ordonnance-requete.service';
import {
  CRITERE_RECEVABILITE_LABELS,
  CritereRecevabilite,
  MOTIF_REQUETE_LABELS,
  MotifRequete,
  OrdonnanceRequeteRequest,
  OrdonnanceRequeteResponse,
  VerdictAccordeProbabilite,
} from '../../core/models/ordonnance-requete.model';
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

/**
 * SF-FA-23-02 : champs d'alerte F-IA-03 exposés par "Ordonnance sur requête".
 * - PRESENCE_ENFANTS : divergence si IA détecte la présence d'enfants
 *   (`presenceEnfantsDetected=true`) et l'avocat saisit `presenceEnfants=false`
 *   (ou inversement). Multi-sources F96 / QUESTION_IA / IA / PIECE_MANQUANTE.
 */
export type OrdonnanceRequeteAlertField = 'PRESENCE_ENFANTS';

export type OrdonnanceRequeteAlertSource = CoherenceAlertSource;
export type OrdonnanceRequeteCoherenceAlert =
  CoherenceAlert<OrdonnanceRequeteAlertField>;

/**
 * SF-FA-23-02 : outil décisionnel "Ordonnance sur requête" (mesures urgentes
 * familiales — art. 493 + 497 CPC FR / art. 1025 et s. CJ BE).
 *
 * Outil applicable FRANCE et BELGIQUE simultanément — pas de gate pays.
 * Le `baseJuridique` retourné par le backend est adapté selon
 * `workspaceCountry` (art. 493 CPC FR ou art. 1025 CJ BE).
 *
 * Consomme l'API SF-FA-23-01 (mergée PR #637). Affiché conditionnellement
 * par le panel F-IA-04 (tool_id 'F-FA-23-ordonnance-requete').
 *
 * Patterns de référence :
 * - Pattern UI : `protection-rp-section` (SF-DT-30-02, PR #634).
 * - Helper partagé : `CoherenceAlertBuilder` + `CoherenceAlert<F>` (SF-155-05).
 *
 * Différence vs `protection-rp-section` : pas de gate `isFrance()`. La
 * procédure de requête unilatérale existe à l'identique dans les 2 pays.
 */
@Component({
  selector: 'app-ordonnance-requete-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatRadioModule, MatCheckboxModule,
    MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './ordonnance-requete-section.component.html',
  styleUrl: './ordonnance-requete-section.component.scss',
})
export class OrdonnanceRequeteSectionComponent implements OnInit, OnChanges {
  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'ORDONNANCE SUR REQUÊTE (FR/BE)';
  static readonly TOOL_ICON = 'gavel';

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
  result = signal<OrdonnanceRequeteResponse | null>(null);

  // Form fields
  motifRequete = signal<MotifRequete | null>(null);
  urgenceJustifiee = signal<boolean>(false);
  derogationContradictoireJustifiee = signal<boolean>(false);
  pieceJustificativeFournie = signal<boolean>(false);
  presenceEnfants = signal<boolean>(false);
  commentaireUrgence = signal<string>('');

  /** Provenance IA pour les champs pré-remplissables (uniquement presenceEnfants pour V1). */
  provenancePresenceEnfants = signal<'IA' | null>(null);

  /** Listes pour mat-radio. */
  readonly motifRequeteOptions = MOTIF_REQUETE_LABELS;
  readonly critereLabels = CRITERE_RECEVABILITE_LABELS;

  /**
   * URGENT si motif IST OU enfants concernés (déclenche le délai 1-3 j et
   * la palette critique côté UI).
   */
  isUrgent = computed<boolean>(() => {
    const motif = this.motifRequete();
    if (motif === 'ENLEVEMENT_INTERNATIONAL') return true;
    return this.presenceEnfants();
  });

  isUrgentResult = computed<boolean>(() => {
    const r = this.result();
    if (!r) return false;
    return r.motifRequete === 'ENLEVEMENT_INTERNATIONAL'
      || (r.delaiTypiqueJoursMin === 1 && r.delaiTypiqueJoursMax === 3);
  });

  /**
   * Alertes de cohérence F-IA-03 par field.
   * Gate : uniquement en mode formulaire.
   */
  coherenceAlerts = computed<Partial<Record<OrdonnanceRequeteAlertField, OrdonnanceRequeteCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<OrdonnanceRequeteAlertField, OrdonnanceRequeteCoherenceAlert>> = {};
    const a = this.buildPresenceEnfantsAlert();
    if (a) alerts.PRESENCE_ENFANTS = a;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: OrdonnanceRequeteService,
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
    // Pas de gate pays — l'outil tourne sur FR + BE.
    this.load();
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
        && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  alertTooltip(alert: OrdonnanceRequeteCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: OrdonnanceRequeteCoherenceAlert): string {
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
   * Divergence présence d'enfants — IA détecte présence d'enfants et
   * l'avocat saisit l'inverse. Multi-sources F96 / QUESTION_IA / IA /
   * PIECE_MANQUANTE.
   */
  private buildPresenceEnfantsAlert(): OrdonnanceRequeteCoherenceAlert | null {
    const userVal = this.presenceEnfants();
    const builder = CoherenceAlertBuilder
      .forField<OrdonnanceRequeteAlertField>('PRESENCE_ENFANTS');

    // 1. F-96
    for (const chk of this.procedureChecksSignal()) {
      const code = chk.critereCode?.toUpperCase();
      if (code !== 'PRESENCE_ENFANTS' && code !== 'ORDONNANCE_REQUETE_ENFANTS') continue;
      const ev = chk.expectedValue;
      if (ev === null || ev === undefined) continue;
      const expected = this.parseBooleanLoose(ev);
      if (expected === null || expected === userVal) continue;
      builder.addSource('F96', {
        expectedDisplay: this.boolDisplay(expected),
        reason: `Checklist procédurale : présence d'enfants attendue ${this.boolDisplay(expected)}`
          + (chk.raison ? ` (${chk.raison})` : ''),
      });
      break;
    }

    // 2. QUESTION_IA
    for (const q of this.aiQuestionsSignal()) {
      const code = q.critereCode?.toUpperCase();
      if (code !== 'PRESENCE_ENFANTS' && code !== 'ORDONNANCE_REQUETE_ENFANTS') continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      const isNon = answer === 'non' || answer.startsWith('non ')
        || answer.startsWith('non,') || answer.startsWith('non.');
      if (!isOui && !isNon) continue;
      const expected = isOui;
      if (expected === userVal) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: this.boolDisplay(expected),
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }

    // 3. IA — `aiData.presenceEnfantsDetected`.
    const iaVal = this.aiDataSignal()?.presenceEnfantsDetected;
    if (iaVal !== null && iaVal !== undefined && iaVal !== userVal) {
      builder.addSource('IA', {
        expectedDisplay: this.boolDisplay(iaVal),
        reason: `Analyse du dossier : présence d'enfants détectée "${this.boolDisplay(iaVal)}"`,
      });
    }

    // 4. PIECE_MANQUANTE — contributor enrichissant.
    const piece = this.findPieceManquante([
      'PRESENCE_ENFANTS', 'ORDONNANCE_REQUETE_ENFANTS',
    ]);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  private parseBooleanLoose(value: unknown): boolean | null {
    if (typeof value === 'boolean') return value;
    if (typeof value !== 'string') return null;
    const v = value.trim().toLowerCase();
    if (v === 'true' || v === 'oui' || v === 'yes' || v === '1') return true;
    if (v === 'false' || v === 'non' || v === 'no' || v === '0') return false;
    return null;
  }

  private boolDisplay(v: boolean): string {
    return v ? 'Oui' : 'Non';
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
    if (!this.motifRequete()) return false;
    // Les 3 critères de recevabilité sont booléens — valides par défaut
    // (l'avocat coche pour confirmer "oui"). On exige seulement le motif.
    const com = this.commentaireUrgence();
    if (com !== null && com !== undefined && com.length > 2000) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  // Handlers onChange — effacent la provenance IA au 1er changement manuel.
  onMotifRequeteChange(value: MotifRequete | null): void {
    this.motifRequete.set(value);
  }

  onUrgenceJustifieeChange(value: boolean): void {
    this.urgenceJustifiee.set(value);
  }

  onDerogationContradictoireChange(value: boolean): void {
    this.derogationContradictoireJustifiee.set(value);
  }

  onPieceJustificativeChange(value: boolean): void {
    this.pieceJustificativeFournie.set(value);
  }

  onPresenceEnfantsChange(value: boolean): void {
    this.presenceEnfants.set(value);
    this.provenancePresenceEnfants.set(null);
  }

  onCommentaireChange(value: string): void {
    this.commentaireUrgence.set(value ?? '');
  }

  /**
   * SF-FA-23-02 : pré-fill depuis `aiData` (FamilleExtractedData).
   * Règles (fail-open) :
   * - passe silencieusement si aiData absent
   * - ne pré-remplit QUE si le champ est encore vide (false par défaut +
   *   provenance null = pas encore touché par l'avocat)
   * - n'écrase jamais si provenance !== 'IA'
   *
   * Seul `presenceEnfants` est pré-fillable. Le motif n'est pas pré-rempli
   * (pas de signal IA fiable pour les 5 motifs spécifiques — l'avocat
   * choisit explicitement).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // presenceEnfants ← aiData.presenceEnfantsDetected
    const iaPresence = ai.presenceEnfantsDetected;
    if (iaPresence === true || iaPresence === false) {
      // Pré-fill uniquement si l'avocat n'a pas encore touché ce champ
      // (provenance null + valeur par défaut false). Si l'avocat a déjà
      // saisi, provenance != 'IA' et on n'écrase pas.
      if (this.provenancePresenceEnfants() === null
          && this.presenceEnfants() === false) {
        this.presenceEnfants.set(iaPresence);
        if (iaPresence) {
          // On ne pose le badge IA que si la valeur change de défaut.
          this.provenancePresenceEnfants.set('IA');
        }
      } else if (this.provenancePresenceEnfants() === 'IA') {
        // Re-prefill si déjà tagué IA (cas changement aiData).
        this.presenceEnfants.set(iaPresence);
      }
    }
  }

  calculate(): void {
    if (!this.formValid()) return;
    const request: OrdonnanceRequeteRequest = {
      motifRequete: this.motifRequete()!,
      urgenceJustifiee: this.urgenceJustifiee(),
      derogationContradictoireJustifiee: this.derogationContradictoireJustifiee(),
      pieceJustificativeFournie: this.pieceJustificativeFournie(),
      presenceEnfants: this.presenceEnfants(),
    };
    const com = this.commentaireUrgence()?.trim();
    if (com) {
      request.commentaireUrgence = com;
    }
    this.calculating.set(true);
    this.service.calculate(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        this.snackBar.open('Ordonnance sur requête analysée', 'OK', { duration: 2500 });
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
        this.provenancePresenceEnfants.set(null);
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
   * - ELEVEE → navy/info (probabilité d'obtention élevée).
   * - MOYENNE → or/warning.
   * - FAIBLE → rouge/critical (palette urgence — référé contradictoire probable).
   */
  bannerClass(verdict: VerdictAccordeProbabilite | string | undefined | null): string {
    switch (verdict) {
      case 'ELEVEE': return 'ord-requete-banner ord-requete-banner--info';
      case 'MOYENNE': return 'ord-requete-banner ord-requete-banner--warning';
      case 'FAIBLE': return 'ord-requete-banner ord-requete-banner--critical';
      default: return 'ord-requete-banner ord-requete-banner--info';
    }
  }

  /** Libellé humain d'un critère de recevabilité. */
  critereLabel(code: CritereRecevabilite | string | null | undefined): string {
    if (!code) return '';
    const label = (this.critereLabels as Record<string, string>)[code as string];
    return label ?? code;
  }

  /** Libellé humain d'un motif. */
  motifLabel(code: MotifRequete | string | null | undefined): string {
    if (!code) return '';
    const opt = MOTIF_REQUETE_LABELS.find((o) => o.code === code);
    return opt?.label ?? code;
  }
}
