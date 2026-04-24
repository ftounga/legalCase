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
import { OqtfSansDelaiService } from '../../core/services/oqtf-sans-delai.service';
import {
  MOTIFS_SANS_DELAI,
  MotifSansDelai,
  MotifSansDelaiOption,
  OqtfSansDelaiResponse,
  StatutDelaiSd,
} from '../../core/models/oqtf-sans-delai.model';
import {
  ImmigrationExtractedData,
  PieceManquanteEntry,
} from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { LegalCitationsPipe } from '../../shared/pipes/legal-citations.pipe';
import { DecisionalHeaderFlagComponent } from '../decisional-tools-panel/decisional-header-flag/decisional-header-flag.component';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';

/**
 * SF-IM-08-04 : outil décisionnel dédié "OQTF SANS délai de départ
 * volontaire — France" (F-IM-08). FR uniquement. Procédure d'urgence
 * absolue : 48h pour former un recours (contre 30 jours pour l'OQTF
 * avec délai). Invariant "un outil = une situation métier" : séparé
 * de SF-IM-08-02 (OQTF avec délai).
 * Consomme l'API SF-IM-08-03. Affiché conditionnellement par le panel
 * F-IA-04 (tool_id 'F-IM-08-oqtf-sans-delai-fr').
 *
 * SF-155-04-B2 : pré-fill IA depuis `ImmigrationExtractedData` + alertes
 * cohérence F-IA-03 critiques (délai 48h dépassé, placement CRA détecté,
 * divergence date/heure, contradiction recours).
 */
export type OqtfSdAlertField =
  | 'DATE_HEURE_NOTIFICATION'
  | 'PLACEMENT_CRA'
  | 'RECOURS_FORME'
  | 'MOTIF_SANS_DELAI';

export type OqtfSdAlertSeverity = 'CRITICAL' | 'WARNING';

export interface OqtfSdCoherenceAlert {
  field: OqtfSdAlertField;
  severity: OqtfSdAlertSeverity;
  reason: string;
  expectedDisplay: string;
}

const ALLOWED_MOTIFS_SANS_DELAI: ReadonlySet<string> = new Set<string>(
  MOTIFS_SANS_DELAI.map((m) => m.code),
);

const MS_ONE_HOUR = 3_600_000;
const MS_48H = 48 * MS_ONE_HOUR;

@Component({
  selector: 'app-oqtf-sans-delai-section',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSlideToggleModule, MatChipsModule, MatProgressSpinnerModule,
    LegalCitationsPipe,
    DecisionalHeaderFlagComponent,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './oqtf-sans-delai-section.component.html',
  styleUrl: './oqtf-sans-delai-section.component.scss',
})
export class OqtfSansDelaiSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;

  // SF-155-04-B2 fix : setter-backed signal pour que `isFrance` (computed)
  // réagisse aux assignations @Input de workspaceCountry, y compris quand
  // les tests le changent via `component.workspaceCountry = 'BELGIQUE';`
  // sans passer par le cycle fixture/ngOnChanges.
  private readonly _workspaceCountry = signal<'FRANCE' | 'BELGIQUE'>('FRANCE');
  @Input()
  set workspaceCountry(v: 'FRANCE' | 'BELGIQUE') { this._workspaceCountry.set(v); }
  get workspaceCountry(): 'FRANCE' | 'BELGIQUE' { return this._workspaceCountry(); }

  // SF-155-04-B2 : Inputs IA (synthèse dossier + checklist F-96 + questions F-IA-02 + pièces F-145).
  @Input() aiData?: ImmigrationExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  collapsed = signal(true);
  loading = signal(false);
  analyzing = signal(false);
  showForm = signal(true);
  result = signal<OqtfSansDelaiResponse | null>(null);

  dateHeureNotificationOqtf = signal<string | null>(null);
  motifSansDelai = signal<MotifSansDelai | null>(null);
  placementCra = signal<boolean>(false);
  recoursForme = signal<boolean>(false);
  dateHeureRecours = signal<string | null>(null);

  // SF-155-04-B2 : signals de provenance IA (badge "Pré-rempli depuis l'analyse").
  // Effacés dès que l'avocat modifie manuellement un champ (onXxxChange).
  provenanceDateHeure = signal<'IA' | null>(null);
  provenanceMotifSansDelai = signal<'IA' | null>(null);
  provenancePlacementCra = signal<'IA' | null>(null);
  provenanceRecoursForme = signal<'IA' | null>(null);

  // SF-155-04-B2 : signal d'override de l'horloge courante (pour tests déterministes).
  // En prod : utilise toujours Date.now().
  private nowMsOverride: number | null = null;

  readonly motifs: MotifSansDelaiOption[] = MOTIFS_SANS_DELAI;

  /** Maintenant au format ISO local (YYYY-MM-DDTHH:mm) pour `max` des inputs datetime-local. */
  readonly nowLocalIso: string = this.buildNowLocalIso();

  isFrance = computed<boolean>(() => this._workspaceCountry() === 'FRANCE');

  // SF-155-04-B2 : alertes cohérence F-IA-03. Gate strict : uniquement
  // quand le formulaire est affiché (règle SF-IA-03-12 : ne pas réafficher
  // après calcul, seul le verdict importe).
  coherenceAlerts = computed<Partial<Record<OqtfSdAlertField, OqtfSdCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    if (!this.isFrance()) return {};
    const alerts: Partial<Record<OqtfSdAlertField, OqtfSdCoherenceAlert>> = {};
    const ai = this.aiData;
    if (!ai) return alerts;

    // ALERTE CRITIQUE 48h : notif IA > 48h + recoursForme=false (recours hors délai probable).
    const critical48h = this.buildAlert48hExpired(ai);
    if (critical48h) {
      alerts.RECOURS_FORME = critical48h;
    } else {
      // Contradiction recours formé (non critique 48h) : IA dit OUI/NON et avocat diverge.
      const recoursContradiction = this.buildRecoursContradictionAlert(ai);
      if (recoursContradiction) alerts.RECOURS_FORME = recoursContradiction;
    }

    // ALERTE CRITIQUE CRA : IA a détecté CRA, avocat dit NON.
    const criticalCra = this.buildCraAlert(ai);
    if (criticalCra) alerts.PLACEMENT_CRA = criticalCra;

    // Divergence date/heure saisie vs IA > 1h.
    const dateAlert = this.buildDateHeureDivergenceAlert(ai);
    if (dateAlert) alerts.DATE_HEURE_NOTIFICATION = dateAlert;

    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    const blockers = values.filter((a) => a && a.severity === 'CRITICAL').length;
    return { total: values.length, blockers };
  });

  constructor(
    private service: OqtfSansDelaiService,
    private snackBar: MatSnackBar,
    @Optional() private dashboardRefresh: CaseDashboardRefreshService,
  ) {}

  ngOnInit(): void {
    if (this.isFrance()) {
      // SF-155-04-B2 : pré-fill IA au mount si aiData est déjà disponible.
      // Ne préempte pas le load() existant — prefillFromAi ne touche que
      // les signals encore à leur valeur défaut.
      this.prefillFromAi();
      this.load();
    }
  }

  ngOnChanges(changes: SimpleChanges): void {
    // SF-155-04-B2 : re-applique le pré-fill dès que aiData change avant la
    // première résolution (cas : pipeline IA termine après l'ouverture du
    // dossier). Si le form n'est plus affiché (result() présent), le
    // pré-fill serait contre-productif — on s'abstient.
    if (
      changes['aiData'] &&
      !changes['aiData'].firstChange &&
      this.isFrance() &&
      this.showForm() &&
      !this.result()
    ) {
      this.prefillFromAi();
    }
  }

  toggleCollapse(): void {
    this.collapsed.update((v) => !v);
  }

  formValid(): boolean {
    const d = this.dateHeureNotificationOqtf();
    const m = this.motifSansDelai();
    if (!d || !m) return false;
    if (d > this.nowLocalIso) return false; // pas dans le futur
    if (this.recoursForme()) {
      const dr = this.dateHeureRecours();
      if (!dr) return false;
      if (dr < d) return false; // recours >= notification
    }
    return true;
  }

  editMode(): void {
    this.showForm.set(true);
  }

  analyze(): void {
    if (!this.formValid()) return;
    const request = {
      dateHeureNotificationOqtf: this.dateHeureNotificationOqtf()!,
      motifSansDelai: this.motifSansDelai()!,
      placementCra: this.placementCra(),
      recoursForme: this.recoursForme(),
      ...(this.recoursForme()
        ? { dateHeureRecours: this.dateHeureRecours()! }
        : {}),
    };
    this.analyzing.set(true);
    this.service.analyze(this.caseFileId, request).subscribe({
      next: (r) => {
        this.result.set(r);
        this.showForm.set(false);
        this.analyzing.set(false);
        this.snackBar.open('OQTF sans délai analysée', 'OK', { duration: 2500 });
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
   * Urgence absolue 48h : palette rouge dominante — DISPONIBLE et
   * URGENT sont déjà en rouge (contrairement à SF-IM-08-02 où DISPONIBLE
   * est bleu / URGENT est or). Seul RECOURS_FORME bascule en vert.
   * Autorisé par DESIGN_SYSTEM.md pour les situations d'urgence critique.
   */
  bannerClass(statut: StatutDelaiSd | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'oqtf-sd-banner oqtf-sd-banner--danger-medium';
      case 'URGENT':        return 'oqtf-sd-banner oqtf-sd-banner--danger-strong';
      case 'EXPIRE':        return 'oqtf-sd-banner oqtf-sd-banner--danger-dark';
      case 'RECOURS_FORME': return 'oqtf-sd-banner oqtf-sd-banner--success';
      default:              return 'oqtf-sd-banner';
    }
  }

  bannerIcon(statut: StatutDelaiSd | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'warning';
      case 'URGENT':        return 'error';
      case 'EXPIRE':        return 'error';
      case 'RECOURS_FORME': return 'check_circle';
      default:              return 'warning';
    }
  }

  statutLabel(statut: StatutDelaiSd | null | undefined): string {
    switch (statut) {
      case 'DISPONIBLE':    return 'Délai 48h disponible';
      case 'URGENT':        return 'Délai 48h urgent';
      case 'EXPIRE':        return 'Délai 48h expiré';
      case 'RECOURS_FORME': return 'Recours formé';
      default:              return '';
    }
  }

  /** Affiche le compteur d'heures restantes si pertinent (hors EXPIRE/RECOURS_FORME). */
  showHeuresRestantes(r: OqtfSansDelaiResponse | null): boolean {
    if (!r) return false;
    return r.statutDelaiRecours === 'DISPONIBLE' || r.statutDelaiRecours === 'URGENT';
  }

  // -----------------------------------------------------------------
  // SF-155-04-B2 — Handlers manuels : effacent la provenance IA.
  // -----------------------------------------------------------------

  onDateHeureNotificationChange(value: string | null): void {
    this.dateHeureNotificationOqtf.set(value || null);
    this.provenanceDateHeure.set(null);
  }

  onMotifSansDelaiChange(value: MotifSansDelai | null): void {
    this.motifSansDelai.set(value);
    this.provenanceMotifSansDelai.set(null);
  }

  onPlacementCraChange(value: boolean): void {
    this.placementCra.set(value);
    this.provenancePlacementCra.set(null);
  }

  onRecoursFormeChange(value: boolean): void {
    this.recoursForme.set(value);
    this.provenanceRecoursForme.set(null);
  }

  // -----------------------------------------------------------------
  // SF-155-04-B2 — Helpers alertes cohérence.
  // -----------------------------------------------------------------

  alertTooltip(alert: OqtfSdCoherenceAlert): string {
    return alert.reason;
  }

  alertBadgeLabel(alert: OqtfSdCoherenceAlert): string {
    return alert.severity === 'CRITICAL'
      ? `Alerte critique : ${alert.expectedDisplay}`
      : `Incohérence détectée : ${alert.expectedDisplay}`;
  }

  /**
   * Test hook — permet aux specs de figer l'horloge courante pour tester
   * les seuils 48h de façon déterministe. Ne modifie pas le comportement
   * en production (Date.now() reste la référence quand override est null).
   */
  setNowMsOverride(ms: number | null): void {
    this.nowMsOverride = ms;
  }

  private nowMs(): number {
    return this.nowMsOverride ?? Date.now();
  }

  /** SF-155-04-B2 : alerte critique 48h — notification > 48h ET recours non formé. */
  private buildAlert48hExpired(ai: ImmigrationExtractedData): OqtfSdCoherenceAlert | null {
    const iso = ai.dateHeureNotificationOqtfSansDelai;
    if (!iso) return null;
    if (this.recoursForme()) return null;
    const notifMs = this.parseIsoToMs(iso);
    if (notifMs === null) return null;
    const deltaMs = this.nowMs() - notifMs;
    if (deltaMs <= MS_48H) return null;
    const hours = Math.round(deltaMs / MS_ONE_HOUR);
    return {
      field: 'RECOURS_FORME',
      severity: 'CRITICAL',
      reason: `Notification OQTF il y a environ ${hours} h (IA : ${iso}). Délai 48h probablement dépassé — aucun recours formé dans le dossier.`,
      expectedDisplay: 'Recours hors délai probable',
    };
  }

  /**
   * Contradiction non critique : IA a détecté que le recours est (ou n'est
   * pas) formé, avocat dit l'inverse. Pas déclenchée si le CRITIQUE 48h
   * est déjà actif (prime sur cet alerte).
   */
  private buildRecoursContradictionAlert(ai: ImmigrationExtractedData): OqtfSdCoherenceAlert | null {
    const reponse = ai.recoursFormeDetected?.reponse;
    if (reponse !== 'OUI' && reponse !== 'NON') return null;
    const iaRecours = reponse === 'OUI';
    if (iaRecours === this.recoursForme()) return null;
    const just = ai.recoursFormeDetected?.justification;
    const justSuffix = just ? ` (${just})` : '';
    return {
      field: 'RECOURS_FORME',
      severity: 'WARNING',
      reason: `Analyse du dossier : recours ${iaRecours ? 'déjà formé' : 'non formé'}${justSuffix}`,
      expectedDisplay: iaRecours ? 'Recours déjà formé' : 'Aucun recours formé',
    };
  }

  /** SF-155-04-B2 : alerte critique CRA détecté par IA, avocat coche non. */
  private buildCraAlert(ai: ImmigrationExtractedData): OqtfSdCoherenceAlert | null {
    if (ai.placementCraDetected !== true) return null;
    if (this.placementCra() === true) return null;
    return {
      field: 'PLACEMENT_CRA',
      severity: 'CRITICAL',
      reason: 'L\'analyse du dossier a détecté une mention de placement en CRA — vérifier les pièces.',
      expectedDisplay: 'Placement CRA détecté',
    };
  }

  /** Divergence date/heure saisie vs IA > 1h — alerte warning. */
  private buildDateHeureDivergenceAlert(ai: ImmigrationExtractedData): OqtfSdCoherenceAlert | null {
    const iso = ai.dateHeureNotificationOqtfSansDelai;
    if (!iso) return null;
    const saisie = this.dateHeureNotificationOqtf();
    if (!saisie) return null;
    const iaMs = this.parseIsoToMs(iso);
    const saisieMs = this.parseIsoToMs(saisie);
    if (iaMs === null || saisieMs === null) return null;
    if (Math.abs(iaMs - saisieMs) <= MS_ONE_HOUR) return null;
    return {
      field: 'DATE_HEURE_NOTIFICATION',
      severity: 'WARNING',
      reason: `Analyse du dossier : ${iso} — écart > 1 h avec la saisie (${saisie}).`,
      expectedDisplay: 'Date IA divergente',
    };
  }

  /**
   * Parse ISO partiel (YYYY-MM-DDTHH:mm[:ss]) en ms. Null si format invalide.
   * Utilise `new Date()` qui interprète la chaîne en timezone LOCALE quand il
   * n'y a ni Z ni offset — cohérent avec les valeurs saisies par l'avocat
   * via `<input type="datetime-local">`.
   */
  private parseIsoToMs(iso: string): number | null {
    if (!iso) return null;
    if (!/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2})?$/.test(iso)) return null;
    const t = new Date(iso).getTime();
    if (Number.isNaN(t)) return null;
    return t;
  }

  // -----------------------------------------------------------------
  // SF-155-04-B2 — Pré-fill IA.
  // -----------------------------------------------------------------

  private prefillFromAi(): void {
    const ai = this.aiData;
    if (!ai) return;
    if (!this.isFrance()) return;
    // Si le form n'est plus affiché (analyse déjà faite), ne pas écraser.
    if (!this.showForm()) return;

    // 1) dateHeureNotificationOqtf
    if (ai.dateHeureNotificationOqtfSansDelai && !this.dateHeureNotificationOqtf()) {
      const normalized = this.normalizeDatetimeLocalInput(ai.dateHeureNotificationOqtfSansDelai);
      if (normalized) {
        this.dateHeureNotificationOqtf.set(normalized);
        this.provenanceDateHeure.set('IA');
      }
    }

    // 2) motifSansDelai — on n'utilise que les valeurs qui croisent
    // l'enum front MotifSansDelai (en pratique : seul 'AUTRE' est
    // dans les deux enums au 2026-04-24). Les autres valeurs backend
    // sont ignorées silencieusement.
    const rawMotif = ai.motifOqtfCode;
    if (rawMotif && ALLOWED_MOTIFS_SANS_DELAI.has(rawMotif) && !this.motifSansDelai()) {
      this.motifSansDelai.set(rawMotif as MotifSansDelai);
      this.provenanceMotifSansDelai.set('IA');
    }

    // 3) placementCra (boolean strict — null / undefined = skip).
    if (ai.placementCraDetected === true || ai.placementCraDetected === false) {
      // Pré-fill uniquement si l'avocat n'a pas déjà interagi (valeur défaut false,
      // mais sans provenance) : pour rester non destructif, on ne pré-remplit que
      // si le signal provenance est encore null ET que la valeur courante est la
      // valeur défaut (false). Cas simple : initial mount.
      if (this.provenancePlacementCra() === null && this.placementCra() === false) {
        this.placementCra.set(ai.placementCraDetected);
        this.provenancePlacementCra.set('IA');
      }
    }

    // 4) recoursForme (OUI/NON/INCONNU — INCONNU skip).
    const reponse = ai.recoursFormeDetected?.reponse;
    if (reponse === 'OUI' || reponse === 'NON') {
      if (this.provenanceRecoursForme() === null && this.recoursForme() === false) {
        this.recoursForme.set(reponse === 'OUI');
        this.provenanceRecoursForme.set('IA');
      }
    }
  }

  /**
   * Convertit un ISO partiel (YYYY-MM-DDTHH:mm[:ss]) en chaîne compatible
   * `<input type="datetime-local">` (YYYY-MM-DDTHH:mm, sans secondes).
   * Retourne null si la chaîne ne matche pas le format attendu.
   */
  private normalizeDatetimeLocalInput(iso: string): string | null {
    if (!iso) return null;
    const m = iso.match(/^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2})(:\d{2})?$/);
    if (!m) return null;
    return m[1];
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (r) => {
        this.result.set(r);
        this.dateHeureNotificationOqtf.set(this.toLocalInputValue(r.dateHeureNotificationOqtf));
        this.motifSansDelai.set(r.motifSansDelai);
        this.placementCra.set(r.placementCra);
        this.recoursForme.set(r.recoursForme);
        this.dateHeureRecours.set(r.dateHeureRecours ? this.toLocalInputValue(r.dateHeureRecours) : null);
        this.showForm.set(false);
        // SF-155-04-B2 : reset provenance IA (les valeurs viennent maintenant
        // du backend, plus de l'analyse IA — badge "Pré-rempli depuis l'analyse"
        // ne doit pas s'afficher).
        this.provenanceDateHeure.set(null);
        this.provenanceMotifSansDelai.set(null);
        this.provenancePlacementCra.set(null);
        this.provenanceRecoursForme.set(null);
        this.loading.set(false);
      },
      error: () => {
        // 404 attendu si aucune analyse — on reste en mode formulaire.
        this.loading.set(false);
      },
    });
  }

  /**
   * Convertit une ISO string backend (avec ou sans offset) en chaîne
   * compatible avec `<input type="datetime-local">` (YYYY-MM-DDTHH:mm).
   * Tolérant aux valeurs déjà au bon format.
   */
  private toLocalInputValue(iso: string): string {
    if (!iso) return '';
    // Déjà au format datetime-local (pas de Z / pas d'offset)
    if (/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}$/.test(iso)) return iso;
    const d = new Date(iso);
    if (isNaN(d.getTime())) return iso;
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  private buildNowLocalIso(): string {
    const d = new Date();
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }
}
