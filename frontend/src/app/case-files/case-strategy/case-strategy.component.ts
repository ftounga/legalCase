import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  Input,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { marked } from 'marked';
import { CaseStrategyService } from '../../core/services/case-strategy.service';
import { CaseStrategy } from '../../core/models/case-strategy.model';

/**
 * F-286 / SF-286-01 — carte « Stratégie de dossier » (onglet Décision, coiffe de la
 * colonne verdict, au-dessus du tableau de bord décisionnel).
 *
 * <p>Affiche la recommandation stratégique consolidée (couche LLM de synthèse EN LECTURE
 * des verdicts d'outils CALCULÉS + synthèse). N'altère AUCUN outil. Reprend le principe
 * F-258 : tant qu'aucun outil n'est calculé, n'invente rien — affiche un encart honnête.</p>
 *
 * <p>Design : gabarit F-282 (carte « document » étroite, navy/or, Merriweather/Inter/
 * JetBrains Mono, états vide/chargement/erreur soignés, CTA navy).</p>
 */
@Component({
  selector: 'app-case-strategy',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    RouterModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './case-strategy.component.html',
  styleUrl: './case-strategy.component.scss',
})
export class CaseStrategyComponent implements OnInit {
  @Input({ required: true }) caseFileId!: string;

  private readonly service = inject(CaseStrategyService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly strategy = signal<CaseStrategy | null>(null);
  readonly loading = signal(true);
  readonly generating = signal(false);

  readonly status = computed(() => this.strategy()?.status ?? null);
  readonly hasReco = computed(() => this.status() === 'READY' && !!this.strategy()?.content);
  /** Génération demandée mais 0 outil calculé : rien d'inventé (reprise F-258). */
  readonly emptyInput = computed(() => this.status() === 'EMPTY_INPUT');
  readonly toolsConsidered = computed(() => this.strategy()?.toolsConsidered ?? 0);
  readonly generatedAt = computed(() => this.strategy()?.generatedAt ?? null);

  /** HTML rendu (sanitizé par Angular au binding [innerHTML]) du markdown de la reco. */
  readonly contentHtml = computed<string>(() => {
    const content = this.strategy()?.content;
    if (!content) return '';
    return (marked.parse(content, { async: false }) as string).trim();
  });

  /**
   * F-295 / SF-295-01 — carte compacte repliable.
   *
   * <p>État de repli local (INV-3 : replié par défaut). Le markdown complet
   * (`contentHtml`) n'est rendu que lorsque `expanded()` est vrai (CA1/CA3).</p>
   */
  readonly expanded = signal(false);

  /**
   * Découpe le markdown brut de la reco par les 4 titres `##` contractuels du prompt
   * F-286 (`CaseStrategyPromptBuilder`). Renvoie {@code null} si le format n'est pas
   * reconnu (LLM qui dévie / contenu legacy) → fallback troncature (CA5 / INV-1).
   */
  readonly strategySections = computed<StrategySections | null>(() => {
    const content = this.strategy()?.content;
    if (!content) return null;
    return parseStrategySections(content);
  });

  /** Le parsing par titres `##` a réussi (CA5). */
  readonly parseOk = computed(() => this.strategySections() !== null);

  /**
   * Lignes du résumé compact dérivées de `strategySections()` (CA2 / INV-6) :
   * 1ʳᵉ phrase de « Voie procédurale », 1ʳᵉ phrase de « Posture », aperçu de la
   * priorisation des chefs de demande. Une ligne dont la section est vide est omise.
   */
  readonly summary = computed<StrategySummaryLine[]>(() => {
    const s = this.strategySections();
    if (!s) return [];
    const lines: StrategySummaryLine[] = [];

    const voie = firstSentence(s.voie);
    if (voie) lines.push({ icon: 'route', label: 'Voie procédurale', text: voie });

    const posture = firstSentence(s.posture);
    if (posture) lines.push({ icon: 'balance', label: 'Posture', text: posture });

    const priorisation = prioritisationPreview(s.priorisationItems);
    if (priorisation) {
      lines.push({ icon: 'low_priority', label: 'Priorisation', text: priorisation });
    }

    return lines;
  });

  ngOnInit(): void {
    this.load();
  }

  /** Bascule l'affichage du détail (markdown complet) — OnPush : `markForCheck` requis. */
  toggleDetail(): void {
    this.expanded.update((v) => !v);
    this.cdr.markForCheck();
  }

  private load(): void {
    this.loading.set(true);
    this.service.get(this.caseFileId).subscribe({
      next: (s) => {
        this.strategy.set(s);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  generate(): void {
    if (this.generating()) return;
    this.generating.set(true);
    this.cdr.markForCheck();
    this.service.generate(this.caseFileId).subscribe({
      next: (s) => {
        this.strategy.set(s);
        this.generating.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.generating.set(false);
        this.snackBar.open('Échec de la génération de la stratégie.', 'Fermer', {
          duration: 5000,
          panelClass: ['snack-error'],
        });
        this.cdr.markForCheck();
      },
    });
  }
}

/** F-295 — sections de la reco découpées par les 4 titres `##` du prompt F-286. */
export interface StrategySections {
  voie: string;
  posture: string;
  priorisationItems: string[];
  sequencement: string;
}

/** F-295 — une ligne du résumé compact (icône + libellé de section + texte condensé). */
export interface StrategySummaryLine {
  icon: string;
  label: string;
  text: string;
}

/** Les 4 titres `##` contractuels du prompt F-286 (ordre imposé), normalisés. */
const SECTION_TITLES = {
  voie: 'voie procédurale',
  posture: 'posture',
  priorisation: 'priorisation des chefs de demande',
  sequencement: 'séquencement',
} as const;

function normalizeTitle(raw: string): string {
  return raw
    .trim()
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '');
}

const NORMALIZED_TITLES = {
  voie: normalizeTitle(SECTION_TITLES.voie),
  posture: normalizeTitle(SECTION_TITLES.posture),
  priorisation: normalizeTitle(SECTION_TITLES.priorisation),
  sequencement: normalizeTitle(SECTION_TITLES.sequencement),
};

/**
 * Découpe le markdown brut par titres `##`. Renvoie {@code null} si les 4 titres
 * attendus ne sont pas tous reconnus → fallback troncature côté template (CA5).
 */
export function parseStrategySections(content: string): StrategySections | null {
  const lines = content.split('\n');
  const blocks: { title: string; body: string[] }[] = [];
  let current: { title: string; body: string[] } | null = null;

  for (const line of lines) {
    const heading = /^\s*##\s+(.+?)\s*$/.exec(line);
    if (heading && !/^#/.test(heading[1])) {
      current = { title: normalizeTitle(heading[1]), body: [] };
      blocks.push(current);
    } else if (current) {
      current.body.push(line);
    }
  }

  const find = (normalized: string) => blocks.find((b) => b.title === normalized);
  const voieB = find(NORMALIZED_TITLES.voie);
  const postureB = find(NORMALIZED_TITLES.posture);
  const prioB = find(NORMALIZED_TITLES.priorisation);
  const seqB = find(NORMALIZED_TITLES.sequencement);

  if (!voieB || !postureB || !prioB || !seqB) return null;

  return {
    voie: bodyToText(voieB.body),
    posture: bodyToText(postureB.body),
    priorisationItems: bodyToListItems(prioB.body),
    sequencement: bodyToText(seqB.body),
  };
}

/** Concatène les lignes de corps (hors items de liste) en un paragraphe nettoyé. */
function bodyToText(body: string[]): string {
  return body
    .map((l) => l.trim())
    .filter((l) => l.length > 0)
    .join(' ')
    .replace(/\s+/g, ' ')
    .trim();
}

/** Extrait les items de liste (`- ` / `* `) du corps d'une section. */
function bodyToListItems(body: string[]): string[] {
  return body
    .map((l) => /^\s*[-*]\s+(.+?)\s*$/.exec(l))
    .filter((m): m is RegExpExecArray => m !== null)
    .map((m) => m[1].trim());
}

/** 1ʳᵉ phrase d'un texte : jusqu'au premier `. ` (ou point final), sinon tout le texte. */
export function firstSentence(text: string): string {
  const t = (text ?? '').trim();
  if (!t) return '';
  const match = /^(.*?[.!?])(\s|$)/.exec(t);
  return (match ? match[1] : t).trim();
}

/** Aperçu de la priorisation : « N chefs de demande priorisés », sinon le 1er item. */
export function prioritisationPreview(items: string[]): string {
  if (!items || items.length === 0) return '';
  if (items.length === 1) return items[0];
  return `${items.length} chefs de demande priorisés`;
}
