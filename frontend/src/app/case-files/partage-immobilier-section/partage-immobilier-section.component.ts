import { Component, Input, OnInit, OnChanges, SimpleChanges, Optional, signal, computed } from '@angular/core';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { DecimalPipe } from '@angular/common';
import { PartageImmobilierService } from '../../core/services/partage-immobilier.service';
import { PartageImmobilierResponse } from '../../core/models/partage-immobilier.model';
import { BienItem, LiquidationCommunaute, PieceManquanteEntry } from '../../core/models/case-analysis.model';
import { ProcedureCheck } from '../../core/models/procedure-check.model';
import { AiQuestion } from '../../core/models/ai-question.model';
import { SourceExplanationService } from '../../core/services/source-explanation.service';
import { SourceExplanation } from '../../core/models/source-explanation.model';
import { CoherencePopoverTriggerDirective } from '../../shared/coherence-popover/coherence-popover-trigger.directive';
import { ToolJurisprudenceCitationsComponent } from '../../shared/tool-jurisprudence-citations/tool-jurisprudence-citations.component';
import {
  CoherenceAlert,
  CoherenceAlertSource,
} from '../../shared/coherence-popover/coherence-alert.model';
import { CoherenceAlertBuilder } from '../../shared/coherence-popover/coherence-alert-builder';
import { FamilleExtractedData } from '../../core/models/divorce-accepte.model';
import { ProcedureCheckAlignment } from '../../core/models/procedure-check-alignment.model';
import { ProcedureChecksOutputComponent } from '../decisional-tools-panel/procedure-checks-output/procedure-checks-output.component';
import { computeBadge, ProcedureChecksBadge } from '../decisional-tools-panel/procedure-check-badge.helper';

import {
  IMMO_KEYWORDS,
  PRET_KEYWORDS,
  PartageImmobilierPrefillRules,
  matchesKeyword,
} from './partage-immobilier-section-prefill-rules';

const FA05_VALEUR_CODE = 'FA05_VALEUR_VENALE';
const FA05_CAPITAL_CODE = 'FA05_CAPITAL_RESTANT';
const FA05_CODES = new Set([FA05_VALEUR_CODE, FA05_CAPITAL_CODE]);

/** Seuil divergence relatif (10 %) — aligné sur SF-IA-03-08 + F-DT-09. */
const DIVERGENCE_RATIO = 0.10;

function findBestMatch(value: number, items: BienItem[]): BienItem | null {
  let best: BienItem | null = null;
  let bestDiff = Infinity;
  for (const it of items) {
    if (it.valeur == null || it.valeur <= 0) continue;
    const diff = Math.abs(value - it.valeur);
    if (diff < bestDiff) {
      bestDiff = diff;
      best = it;
    }
  }
  return best;
}

/**
 * SF-155-20 : champs F-IA-03 audités par l'outil F-FA-05 (partage immobilier).
 * Conserve les noms historiques (`VALEUR_VENALE`, `CAPITAL_RESTANT`) pour
 * compat tests SF-IA-03-08 + SF-FA-05-04.
 */
export type PartageAlertField = 'VALEUR_VENALE' | 'CAPITAL_RESTANT';

// SF-155-20 : alias rétro-compat — utilise l'interface partagée.
export type PartageAlertSource = CoherenceAlertSource;
export type PartageCoherenceAlert = CoherenceAlert<PartageAlertField>;

@Component({
  selector: 'app-partage-immobilier-section',
  standalone: true,
  imports: [
    FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule, MatSelectModule,
    MatFormFieldModule, MatInputModule, MatProgressSpinnerModule,
    MatSlideToggleModule, MatTooltipModule,
    CoherencePopoverTriggerDirective,
    ToolJurisprudenceCitationsComponent
  ],
  templateUrl: './partage-immobilier-section.component.html',
  styleUrl: './partage-immobilier-section.component.scss'
})
export class PartageImmobilierSectionComponent implements OnInit, OnChanges {
  // F-JU-03 SF-JU-03-05 — citations jurisprudentielles F-JU-01.
  protected readonly toolIdForJurisprudence = 'F-FA-05-partage-immobilier';
  protected readonly brancheActiveForJurisprudence = 'default';

  // F-177 SF-177-03b : metadata statique consommée par le panel pour rendre la card.
  static readonly TOOL_LABEL = 'PARTAGE IMMOBILIER';
  static readonly TOOL_ICON = 'home';

  @Input() caseFileId!: string;
  /**
   * SF-FA-05-04 : liquidation communauté détectée — alimente le panneau
   * "Importer depuis l'analyse" (sélecteurs bien + prêt). Conservé en
   * complément de `aiData` (sources non substituables : la liquidation
   * porte la liste détaillée des biens, `aiData` porte les valeurs canoniques).
   */
  @Input() liquidationCommunaute?: LiquidationCommunaute | null;
  /**
   * SF-155-20 : pré-fill IA — `valeurImmeuble` + `capitalRestantDu` extraits
   * par le pipeline IA (FamilleExtractedData). Null-safe partout.
   */
  @Input() aiData?: FamilleExtractedData | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  // Snapshots signal des inputs pour réactivité des `computed` signals.
  private liquidationSignal = signal<LiquidationCommunaute | null | undefined>(undefined);
  private aiDataSignal = signal<FamilleExtractedData | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

  // F-177 SF-177-03b : force l'expansion (mode modal F-177).
  @Input() forceExpanded = false;
  /**
   * F-163 SF-163-02c — Mode simulateur autonome (hors dossier client).
   *
   * Quand `true` :
   *  - Bannière « 🧪 Mode simulateur » affichée en haut.
   *  - `prefillFromAi()` court-circuité.
   *  - `coherenceAlerts` retourne `{}`.
   *  - `loadExisting()` court-circuité.
   *  - `calculate()` POSTe sur `/api/v1/simulators/F-FA-05-partage-immobilier/calculate`.
   *  - `triggerRefresh()` jamais invoqué.
   *
   * Default `false` — mode case-file scoped inchangé.
   */
  @Input() standaloneMode: boolean = false;

  collapsed = signal(true);
  loading = signal(false);
  calculating = signal(false);
  showForm = signal(true);
  result = signal<PartageImmobilierResponse | null>(null);

  country = signal('FRANCE');
  valeurVenale = signal(0);
  capitalRestantDu = signal(0);
  quotePartAttributaire = signal(50);
  isDivorce = signal(true);

  // Import panel state (SF-FA-05-04).
  showImportPanel = signal(false);
  selectedBienLibelle = signal<string | null>(null);
  selectedPretLibelle = signal<string | null>(null); // null = "Aucun prêt"

  // SF-155-20 : provenance IA par champ (badge "Pré-rempli depuis l'analyse").
  // Effacé dès que l'avocat modifie manuellement (onXxxChange).
  // Sources possibles : 'IA' (pré-fill direct depuis aiData OU import depuis
  // liquidationCommunaute — les deux sont des données issues de l'analyse IA).
  provenanceValeur = signal<'IA' | null>(null);
  provenancePret = signal<'IA' | null>(null);

  biensImmobiliersFiltres = computed<BienItem[]>(() => {
    const actifs = this.liquidationSignal()?.actifCommun ?? [];
    return actifs.filter(b => matchesKeyword(b.libelle, IMMO_KEYWORDS));
  });

  pretsFiltres = computed<BienItem[]>(() => {
    const passifs = this.liquidationSignal()?.passifCommun ?? [];
    return passifs.filter(p => matchesKeyword(p.libelle, PRET_KEYWORDS));
  });

  canImport = computed(() => this.biensImmobiliersFiltres().some(b => b.valeur != null && b.valeur > 0));

  // Import reference values (pour SF-IA-03-08 "best match" override).
  private importedValeurVenale = signal<number | null>(null);
  private importedCapital = signal<number | null>(null);

  /**
   * SF-155-20 : alertes de cohérence F-IA-03 — gate `showForm()` strict
   * (pattern anti-bug SF-IA-03-12 : pas d'alertes affichées après calcul).
   *
   * 4 sources consolidées : `aiData` (FamilleExtractedData), F-96 (procedureChecks
   * VERIFIED), QUESTION_IA (aiQuestions answered "oui"), PIECE_MANQUANTE.
   */
  coherenceAlerts = computed<Partial<Record<PartageAlertField, PartageCoherenceAlert>>>(() => {
    // F-163 SF-163-02c : aucune source IA en standalone.
    if (this.standaloneMode) return {};
    if (!this.showForm()) return {};
    const alerts: Partial<Record<PartageAlertField, PartageCoherenceAlert>> = {};
    const valeurAlert = this.buildValeurVenaleAlert();
    if (valeurAlert) alerts.VALEUR_VENALE = valeurAlert;
    const capitalAlert = this.buildCapitalRestantAlert();
    if (capitalAlert) alerts.CAPITAL_RESTANT = capitalAlert;
    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  // SF-IA-03-15b — map {sourceKey → explanation} pour le popover enrichi.
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private partageService: PartageImmobilierService,
    @Optional() private sourceExplanationService: SourceExplanationService | null,
    private snackBar: MatSnackBar,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  private loadSourceExplanations(): void {
    if (!this.caseFileId || !this.sourceExplanationService) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: map => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15b : mapping vers sourceKey F96 FA05_*. */
  explanationFor(field: PartageAlertField): SourceExplanation[] {
    const key = field === 'VALEUR_VENALE' ? FA05_VALEUR_CODE : FA05_CAPITAL_CODE;
    return this.sourceExplanations().get(key) ?? [];
  }

  ngOnInit(): void {
    // F-177 SF-177-03b : appliqué dès le mount pour le mode modal.
    if (this.forceExpanded) this.collapsed.set(false);
    this.liquidationSignal.set(this.liquidationCommunaute);
    this.aiDataSignal.set(this.aiData);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);

    if (this.standaloneMode) {
      // F-163 SF-163-02c : pas de dossier à interroger, pas de pré-fill IA,
      // pas d'explications de sources à charger.
      this.collapsed.set(false);
      this.loading.set(false);
      this.showForm.set(true);
      return;
    }

    // SF-155-20 : pré-fill au mount si aiData déjà disponible (cas où
    // l'analyse IA est terminée avant l'ouverture de l'outil).
    this.prefillFromAi();
    this.loadExisting();
    this.loadSourceExplanations();
  }

  ngOnChanges(changes: SimpleChanges): void {
    // F-177 SF-177-03b : applique le forceExpanded quand il passe à true en cours de vie.
    if (changes['forceExpanded'] && this.forceExpanded) this.collapsed.set(false);
    if (changes['liquidationCommunaute']) {
      this.liquidationSignal.set(this.liquidationCommunaute);
    }
    if (changes['aiData']) this.aiDataSignal.set(this.aiData);
    if (changes['procedureChecks']) {
      this.procedureChecksSignal.set(this.procedureChecks ?? []);
    }
    if (changes['aiQuestions']) {
      this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    }
    if (changes['piecesManquantes']) {
      this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    }
    // SF-155-20 : ré-appliquer le pré-fill quand `aiData` arrive après mount
    // tant que l'avocat n'a pas saisi manuellement et qu'aucun résultat n'est
    // affiché (form encore visible). F-163 SF-163-02c : bypass en standalone.
    if (changes['aiData'] && !changes['aiData'].firstChange
        && this.showForm() && !this.result() && !this.standaloneMode) {
      this.prefillFromAi();
    }
  }

  /**
   * SF-155-20 + F-220 (BE) : pré-remplit `valeurVenale` et `capitalRestantDu`
   * depuis 2 sources prioritaires :
   * 1. `aiData.valeurImmeuble` / `aiData.capitalRestantDu` (extraction Famille
   *    directe — actuellement renseigné par le pipeline IA pour FR uniquement,
   *    pas pour BE en l'état des prompts post-salve 2).
   * 2. **Fallback** `liquidationCommunaute.actifCommun` / `passifCommun` —
   *    le pipeline IA renseigne toujours ces objets quand il détecte les
   *    biens dans les pièces (FR + BE). On filtre par mots-clés (IMMO_KEYWORDS
   *    / PRET_KEYWORDS) pour identifier le bien immobilier et le prêt
   *    hypothécaire pertinents. Cas réel BE : convention préalable Vermeersch
   *    où `liquidationCommunaute.actifCommun[0].valeur = 222000` (appartement
   *    rue du Bailli, valeur nette) et `passifCommun[0].valeur = 198000`
   *    (prêt BNP Paribas Fortis).
   *
   * N'écrase pas une saisie avocat (provenance === null indique une
   * modification manuelle). Convention : `valeurVenale` à 0 = "non saisi"
   * (signal initial).
   */
  private prefillFromAi(): void {
    // F-236 SF-236-02 — délégation au helper partagé `PartageImmobilierPrefillRules`.
    // Note : le fallback liquidation ne s'applique que s'il n'y a **qu'un seul**
    // bien immo / prêt détecté (pas d'ambiguïté). Si plusieurs biens, l'avocat
    // choisit via le bouton "Importer depuis l'analyse" (panel SF-FA-05-04).
    const helperInput = {
      aiData: this.aiDataSignal(),
      liquidationCommunaute: this.liquidationSignal() ?? null,
    };
    const valeurImmeuble = PartageImmobilierPrefillRules.computeValeurImmeuble(helperInput);
    const capitalRestantDu = PartageImmobilierPrefillRules.computeCapitalRestantDu(helperInput);

    if (valeurImmeuble !== null) {
      if (this.valeurVenale() === 0 || this.provenanceValeur() === 'IA') {
        this.valeurVenale.set(valeurImmeuble);
        this.provenanceValeur.set('IA');
        this.importedValeurVenale.set(valeurImmeuble);
      }
    }

    if (capitalRestantDu !== null) {
      if (this.capitalRestantDu() === 0 || this.provenancePret() === 'IA') {
        this.capitalRestantDu.set(capitalRestantDu);
        if (capitalRestantDu > 0) {
          this.provenancePret.set('IA');
          this.importedCapital.set(capitalRestantDu);
        }
      }
    }
  }

  // ---------------------------------------------------------------------------
  // Builders d'alertes F-IA-03 (un par field) — via CoherenceAlertBuilder
  // ---------------------------------------------------------------------------

  alertTooltip(alert: PartageCoherenceAlert): string {
    return alert.contributors.length > 1 ? `Contredit ${alert.reason}` : alert.reason;
  }

  alertBadgeLabel(alert: PartageCoherenceAlert): string {
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

  private parseNumeric(raw: string | null | undefined): number | null {
    if (raw == null) return null;
    const n = parseFloat(String(raw).trim().replace(',', '.'));
    return Number.isFinite(n) ? n : null;
  }

  private within10Percent(a: number, b: number): boolean {
    const base = Math.max(Math.abs(b), 1);
    return Math.abs(a - b) / base < DIVERGENCE_RATIO;
  }

  private formatEuros(n: number): string {
    return `${n.toLocaleString('fr-FR')} €`;
  }

  private findPieceManquante(code: string): string | null {
    for (const p of this.piecesManquantesSignal()) {
      if (p.critereCode?.toUpperCase() === code) return p.texte;
    }
    return null;
  }

  /**
   * Pré-collecte les sources F-96 + QUESTION_IA + IA pour un field numérique
   * (VALEUR_VENALE / CAPITAL_RESTANT). Permet d'aligner l'`expectedDisplay`
   * des sources convergentes (within 10 %) sur la valeur "primaire" (la
   * première source détectée), pour que `CoherenceAlertBuilder` les
   * consolide correctement (le builder partagé matche par chaîne exacte).
   *
   * Hiérarchie de "primaire" : F-96 > QUESTION_IA > IA. La première source
   * détectée fixe l'`expectedDisplay` canonique ; les sources suivantes
   * convergentes (à ±10 %) sont alignées dessus.
   */
  private collectNumericSources(
    user: number,
    code: string,
    iaExpected: number | null,
    iaLibelle: string | undefined,
  ): { source: 'F96' | 'QUESTION_IA' | 'IA'; expectedDisplay: string; reason: string }[] {
    const out: { source: 'F96' | 'QUESTION_IA' | 'IA'; expectedDisplay: string; reason: string; rawValue: number }[] = [];

    // 1. F-96 VERIFIED
    for (const chk of this.procedureChecksSignal()) {
      if (chk.critereCode?.toUpperCase() !== code) continue;
      if (chk.statut !== 'VERIFIED') continue;
      const expected = this.parseNumeric(chk.expectedValue);
      if (expected == null) continue;
      if (this.within10Percent(user, expected)) break;
      out.push({
        source: 'F96',
        expectedDisplay: this.formatEuros(expected),
        reason: `Checklist procédurale : ${this.formatEuros(expected)}${chk.raison ? ' (' + chk.raison + ')' : ''}`,
        rawValue: expected,
      });
      break;
    }

    // 2. QUESTION_IA "oui"
    for (const q of this.aiQuestionsSignal()) {
      if (q.critereCode?.toUpperCase() !== code) continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ')
        || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const expected = this.parseNumeric(q.expectedValue);
      if (expected == null) continue;
      if (this.within10Percent(user, expected)) break;
      out.push({
        source: 'QUESTION_IA',
        expectedDisplay: this.formatEuros(expected),
        reason: `Question complémentaire : "${q.questionText}" → "${q.answerText}"`,
        rawValue: expected,
      });
      break;
    }

    // 3. IA (best-match parmi les biens / prêts / aiData)
    if (iaExpected != null && !this.within10Percent(user, iaExpected)) {
      out.push({
        source: 'IA',
        expectedDisplay: this.formatEuros(iaExpected),
        reason: `Analyse du dossier : ${this.formatEuros(iaExpected)}${iaLibelle ? ' (' + iaLibelle + ')' : ''}`,
        rawValue: iaExpected,
      });
    }

    if (out.length === 0) return [];

    // Aligne l'expectedDisplay des sources convergentes (within 10 %)
    // sur la première source ("primaire" — F-96 > QUESTION_IA > IA).
    const primary = out[0];
    return out
      .filter((s, i) => i === 0 || this.within10Percent(s.rawValue, primary.rawValue))
      .map((s, i) => ({
        source: s.source,
        expectedDisplay: i === 0 ? primary.expectedDisplay : primary.expectedDisplay,
        reason: s.reason,
      }));
  }

  /**
   * Construit l'alerte VALEUR_VENALE en consolidant 4 sources :
   *  1. F-96 procedureCheck VERIFIED `FA05_VALEUR_VENALE`
   *  2. QUESTION_IA réponse "oui" sur `FA05_VALEUR_VENALE`
   *  3. IA — best-match dans biens immobiliers ou aiData.valeurImmeuble
   *  4. PIECE_MANQUANTE `FA05_VALEUR_VENALE` (contributor uniquement)
   */
  private buildValeurVenaleAlert(): PartageCoherenceAlert | null {
    const user = this.valeurVenale();
    if (user <= 0) return null;
    const builder = CoherenceAlertBuilder.forField<PartageAlertField>('VALEUR_VENALE');

    // IA — référence : valeur importée si disponible, sinon best-match
    // parmi les biens immobiliers, sinon aiData.valeurImmeuble.
    const biens = this.biensImmobiliersFiltres();
    const importedRef = this.importedValeurVenale();
    let iaRef: BienItem | null = null;
    if (importedRef != null) {
      iaRef = biens.find(b => b.valeur === importedRef) ?? null;
    } else {
      iaRef = findBestMatch(user, biens);
    }
    let iaExpected: number | null = null;
    let iaLibelle: string | undefined;
    if (iaRef && iaRef.valeur != null && iaRef.valeur > 0) {
      iaExpected = iaRef.valeur;
      iaLibelle = iaRef.libelle;
    } else if (typeof this.aiDataSignal()?.valeurImmeuble === 'number'
               && (this.aiDataSignal()!.valeurImmeuble as number) > 0) {
      iaExpected = this.aiDataSignal()!.valeurImmeuble as number;
    }

    const sources = this.collectNumericSources(user, FA05_VALEUR_CODE, iaExpected, iaLibelle);
    for (const s of sources) {
      builder.addSource(s.source, { expectedDisplay: s.expectedDisplay, reason: s.reason });
    }

    // PIECE_MANQUANTE (contributor si une alerte est déjà initiée).
    const piece = this.findPieceManquante(FA05_VALEUR_CODE);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  /**
   * Construit l'alerte CAPITAL_RESTANT en consolidant 4 sources analogues :
   * F-96 / QUESTION_IA / IA (best-match prêts ou aiData.capitalRestantDu) /
   * PIECE_MANQUANTE.
   */
  private buildCapitalRestantAlert(): PartageCoherenceAlert | null {
    const user = this.capitalRestantDu();
    if (user <= 0) return null;
    const builder = CoherenceAlertBuilder.forField<PartageAlertField>('CAPITAL_RESTANT');

    // IA — référence : valeur importée si disponible, sinon best-match
    // parmi les prêts détectés, sinon aiData.capitalRestantDu.
    const prets = this.pretsFiltres();
    const importedRef = this.importedCapital();
    let iaRef: BienItem | null = null;
    if (importedRef != null) {
      iaRef = prets.find(p => p.valeur === importedRef) ?? null;
    } else {
      iaRef = findBestMatch(user, prets);
    }
    let iaExpected: number | null = null;
    let iaLibelle: string | undefined;
    if (iaRef && iaRef.valeur != null && iaRef.valeur > 0) {
      iaExpected = iaRef.valeur;
      iaLibelle = iaRef.libelle;
    } else if (typeof this.aiDataSignal()?.capitalRestantDu === 'number'
               && (this.aiDataSignal()!.capitalRestantDu as number) > 0) {
      iaExpected = this.aiDataSignal()!.capitalRestantDu as number;
    }

    const sources = this.collectNumericSources(user, FA05_CAPITAL_CODE, iaExpected, iaLibelle);
    for (const s of sources) {
      builder.addSource(s.source, { expectedDisplay: s.expectedDisplay, reason: s.reason });
    }

    const piece = this.findPieceManquante(FA05_CAPITAL_CODE);
    if (piece) builder.addPieceManquante(piece);

    return builder.build();
  }

  toggleCollapsed(): void { this.collapsed.update(v => !v); }

  loadExisting(): void {
    this.loading.set(true);
    this.partageService.get(this.caseFileId).subscribe({
      next: r => {
        this.result.set(r);
        this.prefillForm(r);
        this.showForm.set(false);
        this.loading.set(false);
      },
      error: () => {
        this.showForm.set(true);
        this.loading.set(false);
      },
    });
  }

  private prefillForm(resp: PartageImmobilierResponse): void {
    this.country.set(resp.country);
    if (resp.valeurVenale != null) this.valeurVenale.set(resp.valeurVenale);
    if (resp.capitalRestantDu != null) this.capitalRestantDu.set(resp.capitalRestantDu);
    if (resp.quotePartAttributaire != null) {
      // Backend stocke la décimale (0.5) — le signal est en pourcentage (50)
      this.quotePartAttributaire.set(resp.quotePartAttributaire * 100);
    }
    // SF-155-20 : valeurs persistées = saisie avocat — jamais de badge IA.
    this.provenanceValeur.set(null);
    this.provenancePret.set(null);
  }

  calculate(): void {
    this.calculating.set(true);
    const payload = {
      country: this.country(),
      valeurVenale: this.valeurVenale(),
      capitalRestantDu: this.capitalRestantDu(),
      quotePartAttributaire: this.quotePartAttributaire() / 100,
      isDivorce: this.isDivorce(),
    };
    // F-163 SF-163-02c : en standalone, POST sur le dispatcher générique sans dossier.
    const request$ = this.standaloneMode
      ? this.partageService.calculateStandalone(payload)
      : this.partageService.calculate(this.caseFileId, payload);
    request$.subscribe({
      next: r => {
        this.result.set(r);
        this.showForm.set(false);
        this.calculating.set(false);
        // F-163 SF-163-02c : pas de dashboard à rafraîchir en standalone.
        if (!this.standaloneMode) {
          this.refreshService?.triggerRefresh();
        }
      },
      error: () => { this.calculating.set(false); this.snackBar.open('Erreur lors du calcul', 'Fermer', { duration: 4000 }); },
    });
  }

  editForm(): void {
    const r = this.result();
    if (r) this.prefillForm(r);
    this.showForm.set(true);
  }

  toggleImportPanel(): void {
    if (!this.canImport()) return;
    this.showImportPanel.update(v => !v);
  }

  applyImport(): void {
    const bien = this.biensImmobiliersFiltres().find(b => b.libelle === this.selectedBienLibelle());
    if (!bien || bien.valeur == null || bien.valeur <= 0) return;
    this.valeurVenale.set(bien.valeur);
    this.provenanceValeur.set('IA');
    this.importedValeurVenale.set(bien.valeur);

    const pretLib = this.selectedPretLibelle();
    if (pretLib) {
      const pret = this.pretsFiltres().find(p => p.libelle === pretLib);
      if (pret && pret.valeur != null && pret.valeur > 0) {
        this.capitalRestantDu.set(pret.valeur);
        this.provenancePret.set('IA');
        this.importedCapital.set(pret.valeur);
      }
    }
    this.showImportPanel.set(false);
  }

  // SF-155-20 : handlers manuels — effacent les badges IA correspondants.
  onValeurVenaleChange(): void {
    this.provenanceValeur.set(null);
  }

  onCapitalChange(): void {
    this.provenancePret.set(null);
  }

  /**
   * F-IA-04 / F-177 SF-177-12 + F-220 (BE) — pattern miroir TOOL_LABEL :
   * compteur de champs pré-remplis affiché en badge sur la card avant
   * ouverture du composant. Stricte parité avec `prefillFromAi()` ci-dessus.
   *
   * 2 sources prioritaires (mêmes règles que le runtime) :
   * 1. `aiData.valeurImmeuble` / `aiData.capitalRestantDu` (extraction Famille
   *    directe — préféré).
   * 2. Fallback `synthesis.liquidationCommunaute.actifCommun` / `passifCommun`
   *    filtré par mots-clés (IMMO_KEYWORDS / PRET_KEYWORDS).
   *
   * Total possible : 0, 1 ou 2.
   */
  static getPrefillCount(input: {
    aiData?: FamilleExtractedData | null;
    synthesis?: { liquidationCommunaute?: LiquidationCommunaute | null } | null;
    liquidationCommunaute?: LiquidationCommunaute | null;
  }): number {
    // F-236 SF-236-02 — délégation au helper partagé (parité runtime/static).
    return PartageImmobilierPrefillRules.computePrefillCount(input);
  }
}
