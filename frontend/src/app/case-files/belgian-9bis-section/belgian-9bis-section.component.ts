import {
  Component, Input, OnChanges, OnInit, Optional, SimpleChanges,
  signal, computed,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Belgian9bisService } from '../../core/services/belgian-9bis.service';
import {
  Belgian9bisResponse,
  Verdict9bis,
} from '../../core/models/belgian-9bis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import {
  ImmigrationExtractedData,
  PieceManquanteEntry,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { CoherenceAlert, CoherenceAlertSource } from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';

/**
 * SF-155-08 : champs d'alerte de cohérence F-IA-03 exposés par l'outil
 * F-IM-14 (9bis humanitaire BE). Un par field pré-remplissable ou sensible
 * au croisement IA / F-96 / QUESTION_IA / PIECE_MANQUANTE.
 */
export type B9bisAlertField =
  | 'DATE_ENTREE_BELGIQUE'
  | 'DUREE_PRESENCE_MOIS'
  | 'CIRCONSTANCES_EXCEPTIONNELLES'
  | 'LIENS_FAMILIAUX_BE'
  | 'LIENS_PROFESSIONNELS'
  | 'SCOLARITE_ENFANTS_BE'
  | 'MENACE_ORDRE_PUBLIC'
  | 'DATE_DEPOT_DEMANDE';

// SF-155-08 : alias locaux rétro-compat — utilise l'interface générique partagée (SF-155-05).
export type B9bisAlertSource = CoherenceAlertSource;
export type B9bisCoherenceAlert = CoherenceAlert<B9bisAlertField>;

/**
 * SF-155-08 : mapping field → critereCode F-96 / QUESTION_IA / PIECE_MANQUANTE.
 * Permet de scanner procedureChecks/aiQuestions/piecesManquantes sans
 * dupliquer la logique dans chaque `build*Alert()`.
 */
const FIELD_CRITERE_CODES: Readonly<Record<B9bisAlertField, readonly string[]>> = {
  DATE_ENTREE_BELGIQUE: ['B9BIS_DATE_ENTREE_BELGIQUE'],
  DUREE_PRESENCE_MOIS: ['B9BIS_DUREE_PRESENCE', 'B9BIS_DUREE_PRESENCE_MOIS'],
  CIRCONSTANCES_EXCEPTIONNELLES: ['B9BIS_CIRCONSTANCES_EXCEPTIONNELLES'],
  LIENS_FAMILIAUX_BE: ['B9BIS_LIENS_FAMILIAUX_BE', 'B9BIS_LIENS_FAMILIAUX'],
  LIENS_PROFESSIONNELS: ['B9BIS_LIENS_PROFESSIONNELS'],
  SCOLARITE_ENFANTS_BE: ['B9BIS_SCOLARITE_ENFANTS_BE', 'B9BIS_SCOLARITE_ENFANTS'],
  MENACE_ORDRE_PUBLIC: ['B9BIS_MENACE_ORDRE_PUBLIC'],
  DATE_DEPOT_DEMANDE: ['B9BIS_DATE_DEPOT_DEMANDE', 'B9BIS_DATE_DEPOT'],
};

/**
 * SF-IM-14-05 : outil décisionnel "9bis humanitaire BE"
 * (Loi 15/12/1980 art. 9bis + AR 17/05/2007). BE uniquement.
 *
 * Consomme l'API SF-IM-14-01 (Belgian9bisController). Affiché conditionnellement
 * par le panel F-IA-04 via tool_id `F-IM-14-9bis-humanitaire-be`.
 *
 * SF-155-08 : composant mis en conformité avec le template canonique IA-compliant
 * (pré-fill IA + validation F-IA-03 — pattern `immigration-title-decision-section`
 * et `harcelement-licenciement-nul-section`). Régression vague 10 (PR #538) corrigée.
 *
 * Pré-fill IA depuis ImmigrationExtractedData (no-op gracieux si champ IA absent) :
 * - dateDepotProcedure → dateDepotDemande (provenance IA)
 * - Les autres fields n'ont pas de correspondance IA native dans
 *   ImmigrationExtractedData aujourd'hui — le mécanisme est en place (signal
 *   + handler) pour un no-op gracieux et pour accueillir de futures
 *   extractions sans nouvelle régression structurelle.
 */
@Component({
  selector: 'app-belgian-9bis-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule,
    MatSlideToggleModule, MatChipsModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './belgian-9bis-section.component.html',
  styleUrl: './belgian-9bis-section.component.scss',
})
export class Belgian9bisSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() workspaceCountry: 'FRANCE' | 'BELGIQUE' = 'BELGIQUE';
  /** SF-IM-14-05 : pré-fill IA depuis ImmigrationExtractedData (BE). */
  @Input() aiData?: Partial<ImmigrationExtractedData> | null;
  // SF-155-08 : inputs IA transversaux pour validation F-IA-03 — tous optionnels,
  // null-safe partout (si le panel F-IA-04 ne les passe pas, l'outil reste
  // fonctionnel, l'alerte ne s'affiche simplement pas).
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<Belgian9bisResponse | null>(null);

  // Form state
  dateEntreeBelgique = signal<string | null>(null);
  dureePresenceMois = signal<number>(0);
  circonstancesExceptionnelles = signal<boolean>(false);
  liensFamiliauxBe = signal<boolean>(false);
  liensProfessionnels = signal<boolean>(false);
  scolariteEnfantsBe = signal<boolean>(false);
  menaceOrdrePublic = signal<boolean>(false);
  dateDepotDemande = signal<string | null>(null);

  // SF-155-08 : provenance IA par champ (badge "Pré-rempli depuis l'analyse").
  // Pattern canonique : un signal par field pré-remplissable. Effacé dès que
  // l'avocat modifie manuellement (onXxxChange).
  provenanceDateEntreeBelgique = signal<'IA' | null>(null);
  provenanceDureePresenceMois = signal<'IA' | null>(null);
  provenanceCirconstancesExceptionnelles = signal<'IA' | null>(null);
  provenanceLiensFamiliauxBe = signal<'IA' | null>(null);
  provenanceLiensProfessionnels = signal<'IA' | null>(null);
  provenanceScolariteEnfantsBe = signal<'IA' | null>(null);
  provenanceMenaceOrdrePublic = signal<'IA' | null>(null);
  provenanceDateDepot = signal<'IA' | null>(null);

  // SF-155-08 : snapshots signal des inputs IA pour que `computed` réagisse.
  // Même pattern qu'`immigration-title-decision-section` / `harcelement-licenciement-nul-section`.
  private aiDataSignal = signal<Partial<ImmigrationExtractedData> | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // SF-IA-03-15c : map {sourceKey → explanations} pour le popover enrichi.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  /** Aujourd'hui en YYYY-MM-DD pour [max] des champs date. */
  readonly todayIso: string = new Date().toISOString().slice(0, 10);

  isBelgium = computed<boolean>(() => this.workspaceCountry === 'BELGIQUE');

  // SF-155-08 : alertes de cohérence F-IA-03 calculées dynamiquement.
  // Gate : uniquement en mode formulaire (pattern anti-bug — ne pas afficher
  // d'alertes après que le calcul est rendu).
  coherenceAlerts = computed<Partial<Record<B9bisAlertField, B9bisCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<B9bisAlertField, B9bisCoherenceAlert>> = {};
    const dateDepotAlert = this.buildDateDepotAlert();
    if (dateDepotAlert) alerts.DATE_DEPOT_DEMANDE = dateDepotAlert;
    const dateEntreeAlert = this.buildBooleanOrDateAlert('DATE_ENTREE_BELGIQUE', this.dateEntreeBelgique());
    if (dateEntreeAlert) alerts.DATE_ENTREE_BELGIQUE = dateEntreeAlert;
    const dureeAlert = this.buildDureePresenceAlert();
    if (dureeAlert) alerts.DUREE_PRESENCE_MOIS = dureeAlert;
    const circAlert = this.buildBooleanOrDateAlert('CIRCONSTANCES_EXCEPTIONNELLES', this.circonstancesExceptionnelles());
    if (circAlert) alerts.CIRCONSTANCES_EXCEPTIONNELLES = circAlert;
    const liensFamAlert = this.buildBooleanOrDateAlert('LIENS_FAMILIAUX_BE', this.liensFamiliauxBe());
    if (liensFamAlert) alerts.LIENS_FAMILIAUX_BE = liensFamAlert;
    const liensProAlert = this.buildBooleanOrDateAlert('LIENS_PROFESSIONNELS', this.liensProfessionnels());
    if (liensProAlert) alerts.LIENS_PROFESSIONNELS = liensProAlert;
    const scolAlert = this.buildBooleanOrDateAlert('SCOLARITE_ENFANTS_BE', this.scolariteEnfantsBe());
    if (scolAlert) alerts.SCOLARITE_ENFANTS_BE = scolAlert;
    const menaceAlert = this.buildBooleanOrDateAlert('MENACE_ORDRE_PUBLIC', this.menaceOrdrePublic());
    if (menaceAlert) alerts.MENACE_ORDRE_PUBLIC = menaceAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  constructor(
    private service: Belgian9bisService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService | null,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
  ) {}

  ngOnInit(): void {
    // SF-155-08 : push inputs vers signals avant toute évaluation computed.
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    // SF-155-08 : garde-fou — pré-remplit au mount si aiData est déjà
    // disponible (cas : pipeline déjà tourné avant ouverture du dossier).
    this.prefillFromAi();
    if (this.isBelgium()) {
      this.load();
    }
    this.loadSourceExplanations();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // SF-155-08 : ré-hydrater les signals à chaque changement d'input.
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) this.procedureChecksSignal.set(this.procedureChecks ?? []);
    if (changes['aiQuestions']) this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    if (changes['piecesManquantes']) this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    // SF-155-08 : ré-applique le prefill dès que aiData change avant
    // première résolution backend. Ne PAS écraser si un résultat persisté
    // est déjà présent (form masqué).
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.isBelgium() && this.showForm() && !this.result()) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update(v => !v);
  }

  formValid(): boolean {
    const d = this.dateEntreeBelgique();
    const duree = this.dureePresenceMois();
    if (!d) return false;
    if (d > this.todayIso) return false;
    if (duree === null || duree === undefined) return false;
    if (!Number.isFinite(duree) || duree < 0) return false;
    const dDepot = this.dateDepotDemande();
    if (dDepot && dDepot < d) return false; // dépôt >= entrée
    if (dDepot && dDepot > this.todayIso) return false;
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const dDepot = this.dateDepotDemande();
    const request = {
      dateEntreeBelgique: this.dateEntreeBelgique()!,
      dureePresenceMois: this.dureePresenceMois(),
      circonstancesExceptionnelles: this.circonstancesExceptionnelles(),
      liensFamiliauxBe: this.liensFamiliauxBe(),
      liensProfessionnels: this.liensProfessionnels(),
      scolariteEnfantsBe: this.scolariteEnfantsBe(),
      menaceOrdrePublic: this.menaceOrdrePublic(),
      ...(dDepot ? { dateDepotDemande: dDepot } : {}),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('Analyse 9bis humanitaire enregistrée', 'OK', { duration: 2500 });
        this.dashboardRefresh?.triggerRefresh();
      },
      error: (err) => {
        this.analyzing.set(false);
        const msg = err?.error?.message || err?.error || 'Erreur lors de l\'analyse';
        this.snackBar.open(String(msg), 'Fermer', { duration: 5000, panelClass: 'snack-error' });
      },
    });
  }

  // ---------------------------------------------------------------------------
  // SF-155-08 : handlers — effacent la provenance IA au 1er changement manuel.
  // ---------------------------------------------------------------------------

  onDateEntreeChange(value: string | null): void {
    this.dateEntreeBelgique.set(value || null);
    this.provenanceDateEntreeBelgique.set(null);
  }

  onDureePresenceChange(value: number | null | undefined): void {
    this.dureePresenceMois.set(value ?? 0);
    this.provenanceDureePresenceMois.set(null);
  }

  onCirconstancesExceptionnellesChange(value: boolean): void {
    this.circonstancesExceptionnelles.set(value);
    this.provenanceCirconstancesExceptionnelles.set(null);
  }

  onLiensFamiliauxBeChange(value: boolean): void {
    this.liensFamiliauxBe.set(value);
    this.provenanceLiensFamiliauxBe.set(null);
  }

  onLiensProfessionnelsChange(value: boolean): void {
    this.liensProfessionnels.set(value);
    this.provenanceLiensProfessionnels.set(null);
  }

  onScolariteEnfantsBeChange(value: boolean): void {
    this.scolariteEnfantsBe.set(value);
    this.provenanceScolariteEnfantsBe.set(null);
  }

  onMenaceOrdrePublicChange(value: boolean): void {
    this.menaceOrdrePublic.set(value);
    this.provenanceMenaceOrdrePublic.set(null);
  }

  onDateDepotChange(value: string | null): void {
    this.dateDepotDemande.set(value || null);
    this.provenanceDateDepot.set(null);
  }

  /**
   * SF-155-08 : pré-fill IA depuis aiData. Règles fail-open :
   * - aiData absent → no-op (l'avocat saisit manuellement)
   * - dateDepotProcedure (string) → dateDepotDemande + provenance IA
   * - Les autres fields n'ont pas de correspondance IA native dans
   *   `ImmigrationExtractedData` aujourd'hui. Le mécanisme est en place
   *   (signal `provenance*` + handler `on*Change`) pour une intégration
   *   future sans nouvelle régression structurelle — cf. règle CLAUDE.md
   *   ligne 190 (RÈGLE FONDAMENTALE A).
   */
  private prefillFromAi(): void {
    const ai = this.aiDataSignal();
    if (!ai) return;

    // 1. dateDepotProcedure → dateDepotDemande (ne pré-remplit QUE si vide
    //    ou déjà marqué IA — ne jamais écraser une saisie avocat).
    const dDepot = ai.dateDepotProcedure;
    if (typeof dDepot === 'string' && dDepot.length > 0
        && (this.dateDepotDemande() === null || this.provenanceDateDepot() === 'IA')) {
      this.dateDepotDemande.set(dDepot);
      this.provenanceDateDepot.set('IA');
    }

    // 2. Autres fields : pas de mapping IA natif aujourd'hui. No-op gracieux.
    //    Crochet prêt pour des extractions futures (ex. dateEntreeBelgique
    //    depuis un Annexe 26 détecté).
    //    Cast `as any` volontaire — préfigure l'évolution du modèle sans
    //    forcer la main à la spec actuelle. SF future pourra brancher.
    const extendedAi = ai as any;
    if (typeof extendedAi.dateEntreeBelgique === 'string'
        && extendedAi.dateEntreeBelgique.length > 0
        && (this.dateEntreeBelgique() === null
            || this.provenanceDateEntreeBelgique() === 'IA')) {
      this.dateEntreeBelgique.set(extendedAi.dateEntreeBelgique);
      this.provenanceDateEntreeBelgique.set('IA');
    }
  }

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: (map) => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /**
   * SF-IA-03-15c : mapping field → sourceKey pour le popover enrichi.
   * Renvoie les explications chargées pour le critère, ou `[]` si aucune.
   */
  explanationFor(field: B9bisAlertField): SourceExplanation[] {
    const codes = FIELD_CRITERE_CODES[field] ?? [];
    const map = this.sourceExplanations();
    for (const code of codes) {
      const hit = map.get(code);
      if (hit && hit.length) return hit;
    }
    return [];
  }

  alertTooltip(alert: B9bisCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: B9bisCoherenceAlert): string {
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
  // SF-155-08 : builders d'alertes F-IA-03 via CoherenceAlertBuilder partagé.
  // ---------------------------------------------------------------------------

  /**
   * SF-155-08 : alerte sur `dateDepotDemande`. Divergence IA si
   * `aiData.dateDepotProcedure` est renseignée ET diffère de la saisie avocat.
   * Scan additionnel F-96 / QUESTION_IA / PIECE_MANQUANTE sur critère
   * `B9BIS_DATE_DEPOT_DEMANDE` pour enrichir en MULTI-source.
   */
  private buildDateDepotAlert(): B9bisCoherenceAlert | null {
    const user = this.dateDepotDemande();
    const ai = this.aiDataSignal()?.dateDepotProcedure;
    const builder = CoherenceAlertBuilder.forField<B9bisAlertField>('DATE_DEPOT_DEMANDE');

    // F-96 VERIFIED
    this.scanProcedureChecks(builder, 'DATE_DEPOT_DEMANDE', user);
    // QUESTION_IA "oui"
    this.scanAiQuestions(builder, 'DATE_DEPOT_DEMANDE', user);

    // IA : aiData.dateDepotProcedure vs saisie
    if (typeof ai === 'string' && ai.length > 0 && user !== null && user !== ai) {
      builder.addSource('IA', {
        expectedDisplay: ai,
        reason: `Analyse du dossier : date de dépôt ${ai}`,
      });
    }

    // PIECE_MANQUANTE (contributor additionnel si alerte déjà initiée).
    const piece = this.findPieceManquante(FIELD_CRITERE_CODES.DATE_DEPOT_DEMANDE);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * SF-155-08 : alerte sur `dureePresenceMois`. Pas de mapping IA natif
   * aujourd'hui — l'alerte dépend uniquement des sources F-96 / QUESTION_IA /
   * PIECE_MANQUANTE. No-op gracieux si aucune source ne cible le critère.
   */
  private buildDureePresenceAlert(): B9bisCoherenceAlert | null {
    const user = String(this.dureePresenceMois());
    const builder = CoherenceAlertBuilder.forField<B9bisAlertField>('DUREE_PRESENCE_MOIS');
    this.scanProcedureChecks(builder, 'DUREE_PRESENCE_MOIS', user);
    this.scanAiQuestions(builder, 'DUREE_PRESENCE_MOIS', user);
    const piece = this.findPieceManquante(FIELD_CRITERE_CODES.DUREE_PRESENCE_MOIS);
    if (piece) builder.addPieceManquante(piece);
    return builder.build();
  }

  /**
   * SF-155-08 : builder générique pour les fields booleans (ou date simple)
   * pilotés uniquement par les sources externes F-96 / QUESTION_IA /
   * PIECE_MANQUANTE. Normalise la valeur user en string pour comparaison.
   */
  private buildBooleanOrDateAlert(
    field: B9bisAlertField,
    userValue: boolean | string | null,
  ): B9bisCoherenceAlert | null {
    const userStr = this.normalize(userValue);
    const builder = CoherenceAlertBuilder.forField<B9bisAlertField>(field);
    this.scanProcedureChecks(builder, field, userStr);
    this.scanAiQuestions(builder, field, userStr);
    const codes = FIELD_CRITERE_CODES[field];
    if (codes) {
      const piece = this.findPieceManquante(codes);
      if (piece) builder.addPieceManquante(piece);
    }
    return builder.build();
  }

  private normalize(value: boolean | string | null): string | null {
    if (value === null || value === undefined) return null;
    if (typeof value === 'boolean') return value ? 'true' : 'false';
    return value;
  }

  private scanProcedureChecks(
    builder: CoherenceAlertBuilder<B9bisAlertField>,
    field: B9bisAlertField,
    userValue: string | null,
  ): void {
    const codes = new Set(FIELD_CRITERE_CODES[field].map((c) => c.toUpperCase()));
    for (const chk of this.procedureChecksSignal()) {
      const chkCode = chk.critereCode?.toUpperCase();
      if (!chkCode || !codes.has(chkCode)) continue;
      if (chk.statut !== 'VERIFIED') continue;
      const ev = chk.expectedValue;
      if (!ev) continue;
      if (userValue !== null && userValue.toLowerCase() === ev.toLowerCase()) continue;
      builder.addSource('F96', {
        expectedDisplay: ev,
        reason: `Checklist procédurale : ${ev}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
      });
      break;
    }
  }

  private scanAiQuestions(
    builder: CoherenceAlertBuilder<B9bisAlertField>,
    field: B9bisAlertField,
    userValue: string | null,
  ): void {
    const codes = new Set(FIELD_CRITERE_CODES[field].map((c) => c.toUpperCase()));
    for (const q of this.aiQuestionsSignal()) {
      const qCode = q.critereCode?.toUpperCase();
      if (!qCode || !codes.has(qCode)) continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const ev = q.expectedValue;
      if (!ev) continue;
      if (userValue !== null && userValue.toLowerCase() === ev.toLowerCase()) continue;
      builder.addSource('QUESTION_IA', {
        expectedDisplay: ev,
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
      });
      break;
    }
  }

  /**
   * SF-155-08 : renvoie le `texte` d'une `PieceManquanteEntry` dont
   * `critereCode` (case-insensitive) appartient à la liste des codes acceptés,
   * ou `null` si aucune pièce ne matche. La première pièce trouvée gagne.
   */
  private findPieceManquante(acceptedCodes: readonly string[]): string | null {
    const norm = new Set(acceptedCodes.map((c) => c.toUpperCase()));
    for (const p of this.piecesManquantesSignal()) {
      const code = p.critereCode?.toUpperCase();
      if (!code) continue;
      if (norm.has(code)) return p.texte;
    }
    return null;
  }

  // ---------------------------------------------------------------------------
  // UI helpers (inchangés, conservés tels quels).
  // ---------------------------------------------------------------------------

  /**
   * Classe CSS du bandeau verdict.
   * Palette navy/or — pas de rouge dominant (pas d'urgence 48h ici).
   */
  bannerClass(verdict: Verdict9bis | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE':  return 'b9bis-banner b9bis-banner--success';
      case 'MOYENNE': return 'b9bis-banner b9bis-banner--info';
      case 'FAIBLE':  return 'b9bis-banner b9bis-banner--warning';
      default:        return 'b9bis-banner';
    }
  }

  bannerIcon(verdict: Verdict9bis | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE':  return 'check_circle';
      case 'MOYENNE': return 'info_outline';
      case 'FAIBLE':  return 'warning';
      default:        return 'info_outline';
    }
  }

  verdictLabel(verdict: Verdict9bis | null | undefined): string {
    switch (verdict) {
      case 'ELEVEE':  return 'Probabilité élevée';
      case 'MOYENNE': return 'Probabilité moyenne';
      case 'FAIBLE':  return 'Probabilité faible';
      default:        return '';
    }
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateEntreeBelgique.set(r.dateEntreeBelgique);
        this.dureePresenceMois.set(r.dureePresenceMois);
        this.circonstancesExceptionnelles.set(r.circonstancesExceptionnelles);
        this.liensFamiliauxBe.set(r.liensFamiliauxBe);
        this.liensProfessionnels.set(r.liensProfessionnels);
        this.scolariteEnfantsBe.set(r.scolariteEnfantsBe);
        this.menaceOrdrePublic.set(r.menaceOrdrePublic);
        this.dateDepotDemande.set(r.dateDepotDemande);
        // SF-155-08 : valeurs persistées = saisie avocat historique — jamais de badge IA.
        this.provenanceDateEntreeBelgique.set(null);
        this.provenanceDureePresenceMois.set(null);
        this.provenanceCirconstancesExceptionnelles.set(null);
        this.provenanceLiensFamiliauxBe.set(null);
        this.provenanceLiensProfessionnels.set(null);
        this.provenanceScolariteEnfantsBe.set(null);
        this.provenanceMenaceOrdrePublic.set(null);
        this.provenanceDateDepot.set(null);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en formulaire et on
        // applique le pré-fill IA si aiData disponible.
        this.loading.set(false);
        this.prefillFromAi();
      },
    });
  }
}
