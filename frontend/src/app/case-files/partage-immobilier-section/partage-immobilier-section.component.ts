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
import { BienItem, LiquidationCommunaute } from '../../core/models/case-analysis.model';

const IMMO_KEYWORDS = ['immobilier', 'maison', 'appartement', 'résidence', 'residence', 'villa', 'studio', 'terrain', 'logement'];
const PRET_KEYWORDS = ['prêt', 'pret', 'emprunt', 'crédit', 'credit', 'hypothèque', 'hypotheque', 'hypothécaire', 'hypothecaire'];

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

export interface PartageCoherenceAlert {
  field: PartageAlertField;
  iaValue: number;
  iaLibelle: string;
}

@Component({
  selector: 'app-partage-immobilier-section',
  standalone: true,
  imports: [
    FormsModule, DecimalPipe,
    MatButtonModule, MatIconModule, MatSelectModule,
    MatFormFieldModule, MatInputModule, MatProgressSpinnerModule,
    MatSlideToggleModule, MatTooltipModule,
  ],
  templateUrl: './partage-immobilier-section.component.html',
  styleUrl: './partage-immobilier-section.component.scss'
})
export class PartageImmobilierSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() liquidationCommunaute?: LiquidationCommunaute | null;

  private liquidationSignal = signal<LiquidationCommunaute | null | undefined>(undefined);

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

    const userValeur = this.valeurVenale();
    if (userValeur > 0) {
      const iaRef = this.importedValeurVenale() != null
        ? this.findBienByValeur(this.importedValeurVenale()!, this.biensImmobiliersFiltres())
        : findBestMatch(userValeur, this.biensImmobiliersFiltres());
      if (iaRef && iaRef.valeur != null && iaRef.valeur > 0) {
        const rel = Math.abs(userValeur - iaRef.valeur) / Math.max(Math.abs(iaRef.valeur), 1);
        if (rel >= 0.10) {
          alerts.VALEUR_VENALE = { field: 'VALEUR_VENALE', iaValue: iaRef.valeur, iaLibelle: iaRef.libelle };
        }
      }
    }

    const userCapital = this.capitalRestantDu();
    if (userCapital > 0) {
      const iaRef = this.importedCapital() != null
        ? this.findBienByValeur(this.importedCapital()!, this.pretsFiltres())
        : findBestMatch(userCapital, this.pretsFiltres());
      if (iaRef && iaRef.valeur != null && iaRef.valeur > 0) {
        const rel = Math.abs(userCapital - iaRef.valeur) / Math.max(Math.abs(iaRef.valeur), 1);
        if (rel >= 0.10) {
          alerts.CAPITAL_RESTANT = { field: 'CAPITAL_RESTANT', iaValue: iaRef.valeur, iaLibelle: iaRef.libelle };
        }
      }
    }

    return alerts;
  });

  alertsSummary = computed(() => {
    const values = Object.values(this.coherenceAlerts());
    return { total: values.length, blockers: 0 };
  });

  private findBienByValeur(valeur: number, list: BienItem[]): BienItem | null {
    return list.find(b => b.valeur === valeur) ?? null;
  }

  alertTooltip(alert: PartageCoherenceAlert): string {
    return `L'IA a détecté : ${alert.iaLibelle} — ${alert.iaValue.toLocaleString('fr-FR')} €`;
  }

  constructor(
    private partageService: PartageImmobilierService,
    private snackBar: MatSnackBar,
    @Optional() private refreshService: CaseDashboardRefreshService | null,
  ) {}

  ngOnInit(): void {
    this.liquidationSignal.set(this.liquidationCommunaute);
    this.loadExisting();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['liquidationCommunaute']) {
      this.liquidationSignal.set(this.liquidationCommunaute);
    }
  }

  toggleCollapsed(): void { this.collapsed.update(v => !v); }

  loadExisting(): void {
    this.loading.set(true);
    this.partageService.get(this.caseFileId).subscribe({
      next: r => { this.result.set(r); this.showForm.set(false); this.loading.set(false); },
      error: () => { this.showForm.set(true); this.loading.set(false); },
    });
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

  editForm(): void { this.showForm.set(true); }

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
