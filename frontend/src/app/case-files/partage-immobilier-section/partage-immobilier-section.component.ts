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

const IMMO_KEYWORDS = ['immobilier', 'maison', 'appartement', 'résidence', 'residence', 'villa', 'studio', 'terrain', 'logement'];
const PRET_KEYWORDS = ['prêt', 'pret', 'emprunt', 'crédit', 'credit', 'hypothèque', 'hypotheque', 'hypothécaire', 'hypothecaire'];

const FA05_CODES = new Set(['FA05_VALEUR_VENALE', 'FA05_CAPITAL_RESTANT']);

function matchesKeyword(libelle: string | null, keywords: string[]): boolean {
  if (!libelle) return false;
  const lower = libelle.toLowerCase();
  return keywords.some(k => lower.includes(k));
}

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

export type PartageAlertField = 'VALEUR_VENALE' | 'CAPITAL_RESTANT';
export type PartageAlertSource = 'F96' | 'QUESTION_IA' | 'IA' | 'PIECE_MANQUANTE' | 'MULTI';

export interface PartageCoherenceAlert {
  field: PartageAlertField;
  iaValue: number;
  iaLibelle?: string;
  source: PartageAlertSource;
  contributors: PartageAlertSource[];
  f96Raison?: string | null;
  questionText?: string | null;
  questionAnswer?: string | null;
  pieceTexte?: string | null;
}

@Component({
  selector: 'app-partage-immobilier-section',
  standalone: true,
  imports: [
    FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule, MatSelectModule,
    MatFormFieldModule, MatInputModule, MatProgressSpinnerModule,
    MatSlideToggleModule, MatTooltipModule,
    CoherencePopoverTriggerDirective,
  ],
  templateUrl: './partage-immobilier-section.component.html',
  styleUrl: './partage-immobilier-section.component.scss'
})
export class PartageImmobilierSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() liquidationCommunaute?: LiquidationCommunaute | null;
  @Input() procedureChecks?: ProcedureCheck[] | null;
  @Input() aiQuestions?: AiQuestion[] | null;
  @Input() piecesManquantes?: PieceManquanteEntry[] | null;

  private liquidationSignal = signal<LiquidationCommunaute | null | undefined>(undefined);
  private procedureChecksSignal = signal<ProcedureCheck[]>([]);
  private aiQuestionsSignal = signal<AiQuestion[]>([]);
  private piecesManquantesSignal = signal<PieceManquanteEntry[]>([]);

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

  // Import panel state
  showImportPanel = signal(false);
  selectedBienLibelle = signal<string | null>(null);
  selectedPretLibelle = signal<string | null>(null); // null = "Aucun prêt"
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

  // Import reference values (for SF-IA-03-08 "best match" override)
  private importedValeurVenale = signal<number | null>(null);
  private importedCapital = signal<number | null>(null);

  coherenceAlerts = computed<Partial<Record<PartageAlertField, PartageCoherenceAlert>>>(() => {
    if (!this.showForm()) return {};
    const alerts: Partial<Record<PartageAlertField, PartageCoherenceAlert>> = {};
    const f96 = this.buildF96Index();
    const questions = this.buildQuestionsIndex();
    const pieces = this.buildPiecesIndex();

    this.buildAlertForField(
      'FA05_VALEUR_VENALE', 'VALEUR_VENALE',
      this.valeurVenale(), this.importedValeurVenale(), this.biensImmobiliersFiltres(),
      f96, questions, pieces, alerts,
    );
    this.buildAlertForField(
      'FA05_CAPITAL_RESTANT', 'CAPITAL_RESTANT',
      this.capitalRestantDu(), this.importedCapital(), this.pretsFiltres(),
      f96, questions, pieces, alerts,
    );

    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  private findBienByValeur(valeur: number, list: BienItem[]): BienItem | null {
    return list.find(b => b.valeur === valeur) ?? null;
  }

  private parseNumeric(raw: string | null | undefined): number | null {
    if (raw == null) return null;
    const n = parseFloat(String(raw).trim().replace(',', '.'));
    return Number.isFinite(n) ? n : null;
  }

  private within10Percent(a: number, b: number): boolean {
    const base = Math.max(Math.abs(b), 1);
    return Math.abs(a - b) / base < 0.10;
  }

  private buildF96Index(): Record<string, { expectedNumeric: number; raison: string | null }> {
    const out: Record<string, { expectedNumeric: number; raison: string | null }> = {};
    for (const chk of this.procedureChecksSignal()) {
      const code = chk.critereCode?.toUpperCase();
      if (!code || !FA05_CODES.has(code)) continue;
      if (chk.statut !== 'VERIFIED') continue;
      const n = this.parseNumeric(chk.expectedValue);
      if (n == null) continue;
      if (!out[code]) out[code] = { expectedNumeric: n, raison: chk.raison ?? null };
    }
    return out;
  }

  private buildQuestionsIndex(): Record<string, { expectedNumeric: number; text: string; answer: string }> {
    const out: Record<string, { expectedNumeric: number; text: string; answer: string }> = {};
    for (const q of this.aiQuestionsSignal()) {
      const code = q.critereCode?.toUpperCase();
      if (!code || !FA05_CODES.has(code)) continue;
      const answer = q.answerText?.trim().toLowerCase();
      if (!answer) continue;
      const isOui = answer === 'oui' || answer.startsWith('oui ') || answer.startsWith('oui,') || answer.startsWith('oui.');
      if (!isOui) continue;
      const n = this.parseNumeric(q.expectedValue);
      if (n == null) continue;
      if (!out[code]) out[code] = { expectedNumeric: n, text: q.questionText, answer: q.answerText ?? '' };
    }
    return out;
  }

  private buildPiecesIndex(): Record<string, string> {
    const out: Record<string, string> = {};
    for (const p of this.piecesManquantesSignal()) {
      const code = p.critereCode?.toUpperCase();
      if (!code || !FA05_CODES.has(code)) continue;
      if (!out[code]) out[code] = p.texte;
    }
    return out;
  }

  private buildAlertForField(
    code: string,
    field: PartageAlertField,
    userValue: number,
    importedValue: number | null,
    biens: BienItem[],
    f96: Record<string, { expectedNumeric: number; raison: string | null }>,
    questions: Record<string, { expectedNumeric: number; text: string; answer: string }>,
    pieces: Record<string, string>,
    out: Partial<Record<PartageAlertField, PartageCoherenceAlert>>,
  ): void {
    if (userValue <= 0) return;
    const contributors: PartageAlertSource[] = [];
    let expected: number | null = null;
    let iaLibelle: string | undefined;
    let f96Raison: string | null = null;
    let questionText: string | null = null;
    let questionAnswer: string | null = null;
    let pieceTexte: string | null = null;
    let primary: PartageAlertSource | null = null;

    // Source F96
    const f96Entry = f96[code];
    if (f96Entry && !this.within10Percent(userValue, f96Entry.expectedNumeric)) {
      expected = f96Entry.expectedNumeric;
      f96Raison = f96Entry.raison;
      primary = 'F96';
      contributors.push('F96');
    }

    // Source Question IA
    const qEntry = questions[code];
    if (qEntry && !this.within10Percent(userValue, qEntry.expectedNumeric)) {
      if (expected == null) {
        expected = qEntry.expectedNumeric;
        primary = 'QUESTION_IA';
        questionText = qEntry.text;
        questionAnswer = qEntry.answer;
        contributors.push('QUESTION_IA');
      } else if (this.within10Percent(qEntry.expectedNumeric, expected)) {
        if ((primary as PartageAlertSource | null) !== 'QUESTION_IA') contributors.push('QUESTION_IA');
        if (!questionText) { questionText = qEntry.text; questionAnswer = qEntry.answer; }
      }
    }

    // Source IA best-match existante
    const iaRef = importedValue != null
      ? this.findBienByValeur(importedValue, biens)
      : findBestMatch(userValue, biens);
    if (iaRef && iaRef.valeur != null && iaRef.valeur > 0
        && !this.within10Percent(userValue, iaRef.valeur)) {
      if (expected == null) {
        expected = iaRef.valeur;
        iaLibelle = iaRef.libelle;
        primary = 'IA';
        contributors.push('IA');
      } else if (this.within10Percent(iaRef.valeur, expected)) {
        if ((primary as PartageAlertSource | null) !== 'IA') {
          contributors.push('IA');
          if (!iaLibelle) iaLibelle = iaRef.libelle;
        }
      }
    }

    // Contributor pièce manquante
    const p = pieces[code];
    if (p && expected != null) {
      contributors.push('PIECE_MANQUANTE');
      pieceTexte = p;
    }

    if (expected == null || primary == null) return;
    // Reorder: primary en tête
    const allContribs = [primary, ...contributors.filter(c => c !== primary)];
    const uniqueContribs = Array.from(new Set(allContribs));
    const source: PartageAlertSource = uniqueContribs.length > 1 ? 'MULTI' : primary;

    out[field] = {
      field, iaValue: expected, iaLibelle, source, contributors: uniqueContribs,
      f96Raison, questionText, questionAnswer, pieceTexte,
    };
  }

  alertTooltip(alert: PartageCoherenceAlert): string {
    const parts: string[] = [];
    for (const src of alert.contributors) {
      if (src === 'F96') {
        parts.push(`Checklist procédurale : ${alert.iaValue} €${alert.f96Raison ? ' (' + alert.f96Raison + ')' : ''}`);
      } else if (src === 'QUESTION_IA') {
        parts.push(`Question complémentaire : "${alert.questionText}" → "${alert.questionAnswer}"`);
      } else if (src === 'IA') {
        parts.push(`Analyse du dossier : ${alert.iaValue} €${alert.iaLibelle ? ' (' + alert.iaLibelle + ')' : ''}`);
      } else if (src === 'PIECE_MANQUANTE') {
        parts.push(`Pièce manquante : ${alert.pieceTexte}`);
      }
    }
    return parts.length > 1 ? `Contredit ${parts.join(' ET ')}` : (parts[0] ?? '');
  }

  alertBadgeLabel(alert: PartageCoherenceAlert): string {
    const prefix = alert.source === 'F96' ? 'Incohérence Checklist procédurale'
      : alert.source === 'QUESTION_IA' ? 'Incohérence Question complémentaire'
      : alert.source === 'IA' ? 'Incohérence détectée'
      : alert.source === 'PIECE_MANQUANTE' ? 'Pièce manquante'
      : 'Incohérence multiple';
    return `${prefix} (${alert.iaValue} €)`;
  }

  // SF-IA-03-15b — map {sourceKey → explanation} pour le popover enrichi
  sourceExplanations = signal<Map<string, SourceExplanation[]>>(new Map());

  constructor(
    private partageService: PartageImmobilierService,
    private sourceExplanationService: SourceExplanationService,
    private snackBar: MatSnackBar,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  private loadSourceExplanations(): void {
    if (!this.caseFileId) return;
    this.sourceExplanationService.getForCaseFile(this.caseFileId).subscribe({
      next: map => this.sourceExplanations.set(map),
      error: () => { /* fail-open */ },
    });
  }

  /** SF-IA-03-15b : mapping vers sourceKey F96 FA05_*. */
  explanationFor(field: 'VALEUR_VENALE' | 'CAPITAL_RESTANT'): SourceExplanation[] {
    const key = field === 'VALEUR_VENALE' ? 'FA05_VALEUR_VENALE' : 'FA05_CAPITAL_RESTANT';
    return this.sourceExplanations().get(key) ?? [];
  }

  ngOnInit(): void {
    this.liquidationSignal.set(this.liquidationCommunaute);
    this.procedureChecksSignal.set(this.procedureChecks ?? []);
    this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    this.loadExisting();
    this.loadSourceExplanations();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['liquidationCommunaute']) {
      this.liquidationSignal.set(this.liquidationCommunaute);
    }
    if (changes['procedureChecks']) {
      this.procedureChecksSignal.set(this.procedureChecks ?? []);
    }
    if (changes['aiQuestions']) {
      this.aiQuestionsSignal.set(this.aiQuestions ?? []);
    }
    if (changes['piecesManquantes']) {
      this.piecesManquantesSignal.set(this.piecesManquantes ?? []);
    }
  }

  toggleCollapsed(): void { this.collapsed.update(v => !v); }

  loadExisting(): void {
    this.loading.set(true);
    this.partageService.get(this.caseFileId).subscribe({
      next: r => { this.result.set(r); this.prefillForm(r); this.showForm.set(false); this.loading.set(false); },
      error: () => { this.showForm.set(true); this.loading.set(false); },
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
  }

  calculate(): void {
    this.calculating.set(true);
    this.partageService.calculate(this.caseFileId, {
      country: this.country(),
      valeurVenale: this.valeurVenale(),
      capitalRestantDu: this.capitalRestantDu(),
      quotePartAttributaire: this.quotePartAttributaire() / 100,
      isDivorce: this.isDivorce(),
    }).subscribe({
      next: r => { this.result.set(r); this.showForm.set(false); this.calculating.set(false); this.refreshService?.triggerRefresh(); },
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

  onValeurVenaleChange(): void { this.provenanceValeur.set(null); }
  onCapitalChange(): void { this.provenancePret.set(null); }
}
