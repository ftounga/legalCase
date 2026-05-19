import { Component, OnInit, signal, computed, inject, PLATFORM_ID, WritableSignal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe, isPlatformBrowser } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { DashboardService } from '../core/services/dashboard.service';
import { DashboardSummary } from '../core/models/dashboard.model';

/** Niveau de criticité de la headline d'orientation du hero. */
type HeadlineLevel = 'critical' | 'warn' | 'ok';

interface Headline {
  level: HeadlineLevel;
  icon: string;
  text: string;
  anchor: string | null;
}

interface Sparkline {
  line: string;
  area: string;
  last: { x: number; y: number };
  total: number;
  width: number;
  height: number;
}

/**
 * F-249 — Tableau de bord d'accueil (page d'accueil cabinet).
 *
 * Hub d'orientation : répond en un coup d'œil à « qu'est-ce qui demande mon
 * attention aujourd'hui ? ». Le hero (accueil personnalisé + date + headline
 * + KPI animés + sparkline) absorbe la barre KPI — charge d'écran constante.
 */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, DatePipe, MatIconModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly platformId = inject(PLATFORM_ID);

  loading = signal(true);
  error = signal(false);
  summary = signal<DashboardSummary | null>(null);

  /** Valeurs KPI affichées — animées en count-up une seule fois à l'entrée. */
  kpiCases = signal(0);
  kpiDeadlines = signal(0);
  kpiChecks = signal(0);
  kpiAnalyses = signal(0);

  /** Date du jour en toutes lettres (« lundi 19 mai 2026 »). */
  readonly today = this.formatToday();

  /** Accueil personnalisé — « Bonjour Maître X » ou « Bonjour » si prénom absent. */
  greeting = computed<string>(() => {
    const name = this.summary()?.userFirstName?.trim();
    return name ? `Bonjour Maître ${name}` : 'Bonjour';
  });

  /** Phrase d'orientation : ce qui demande l'attention de l'avocat aujourd'hui. */
  headline = computed<Headline | null>(() => {
    const data = this.summary();
    if (!data) {
      return null;
    }
    const s = (n: number) => (n > 1 ? 's' : '');
    const critical = data.urgentDeadlines.filter(d => this.daysUntil(d.dueDate) <= 3).length;
    const urgent = data.urgentDeadlines.length;
    const checks = data.staleChecks.length;

    if (critical > 0) {
      return {
        level: 'critical', icon: 'warning', anchor: 'deadlines',
        text: `${urgent} délai${s(urgent)} urgent${s(urgent)}, dont ${critical} critique${s(critical)}`,
      };
    }
    if (urgent > 0) {
      return {
        level: 'warn', icon: 'schedule', anchor: 'deadlines',
        text: `${urgent} délai${s(urgent)} urgent${s(urgent)} dans les 7 prochains jours`,
      };
    }
    if (checks > 0) {
      return {
        level: 'warn', icon: 'report_problem', anchor: 'checks',
        text: `${checks} dossier${s(checks)} avec des alertes de checklist`,
      };
    }
    return { level: 'ok', icon: 'check_circle', anchor: null, text: 'Tout est à jour — aucune urgence' };
  });

  /** Tracé SVG du sparkline « votre semaine » à partir de weeklyActivity. */
  sparkline = computed<Sparkline | null>(() => {
    const week = this.summary()?.weeklyActivity ?? [];
    if (week.length === 0) {
      return null;
    }
    const counts = week.map(d => d.analysesCount);
    const total = counts.reduce((a, b) => a + b, 0);
    const max = Math.max(1, ...counts);
    const width = 132;
    const height = 36;
    const points = counts.map((c, i) => ({
      x: counts.length > 1 ? (i / (counts.length - 1)) * width : width / 2,
      y: height - 3 - (c / max) * (height - 8),
    }));
    const line = points.map((p, i) => `${i ? 'L' : 'M'}${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' ');
    return {
      line,
      area: `${line} L${width} ${height} L0 ${height} Z`,
      last: points[points.length - 1],
      total,
      width,
      height,
    };
  });

  ngOnInit(): void {
    this.load();
  }

  retry(): void {
    this.error.set(false);
    this.loading.set(true);
    this.load();
  }

  /** Défile en douceur vers une section ancrée (headline / KPI cliquables). */
  scrollTo(sectionId: string | null): void {
    if (!sectionId || !isPlatformBrowser(this.platformId)) {
      return;
    }
    document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  daysUntil(dueDateStr: string): number {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const due = new Date(dueDateStr);
    due.setHours(0, 0, 0, 0);
    return Math.round((due.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
  }

  domainColor(domain: string): string {
    switch (domain) {
      case 'DROIT_DU_TRAVAIL':  return '#27AE60';
      case 'DROIT_FAMILLE':     return '#C9973A';
      case 'DROIT_IMMIGRATION': return '#1A3A5C';
      default: return '#1A3A5C';
    }
  }

  domainLabel(domain: string): string {
    switch (domain) {
      case 'DROIT_DU_TRAVAIL':  return 'Travail';
      case 'DROIT_FAMILLE':     return 'Famille';
      case 'DROIT_IMMIGRATION': return 'Immigration';
      default: return domain;
    }
  }

  analysisTypeLabel(type: string): string {
    return type === 'ENRICHED' ? 'Enrichie' : 'Standard';
  }

  private load(): void {
    this.dashboardService.getSummary().subscribe({
      next: data => {
        this.summary.set(data);
        this.loading.set(false);
        this.animateCounts(data);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      },
    });
  }

  /** Count-up des 4 KPI — joué une seule fois à l'entrée (DESIGN_SYSTEM §10). */
  private animateCounts(data: DashboardSummary): void {
    const targets: Array<[WritableSignal<number>, number]> = [
      [this.kpiCases, data.openCasesCount],
      [this.kpiDeadlines, data.urgentDeadlines.length],
      [this.kpiChecks, data.staleChecks.length],
      [this.kpiAnalyses, data.recentAnalyses.length],
    ];
    if (!isPlatformBrowser(this.platformId) || typeof requestAnimationFrame === 'undefined') {
      targets.forEach(([sig, target]) => sig.set(target));
      return;
    }
    const duration = 700;
    const start = performance.now();
    const step = (now: number): void => {
      const progress = Math.min(1, (now - start) / duration);
      const eased = 1 - Math.pow(1 - progress, 3);
      targets.forEach(([sig, target]) => sig.set(Math.round(target * eased)));
      if (progress < 1) {
        requestAnimationFrame(step);
      }
    };
    requestAnimationFrame(step);
  }

  private formatToday(): string {
    return new Intl.DateTimeFormat('fr-FR', {
      weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    }).format(new Date());
  }
}
