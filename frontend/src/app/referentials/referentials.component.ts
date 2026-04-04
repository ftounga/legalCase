import { Component, OnInit, signal } from '@angular/core';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { JsonPipe } from '@angular/common';
import { ReferentialService } from '../core/services/referential.service';
import { WorkspaceService } from '../core/services/workspace.service';
import { ReferentialEntry, ReferentialResponse } from '../core/models/referential.model';
import { fadeInUp } from '../shared/animations';

interface SectionDisplay {
  type: string;
  title: string;
  entries: ReferentialEntry[];
}

const SECTION_LABELS: Record<string, string> = {
  LITIGATION_TYPE:    'Types de litiges',
  BAREME_MACRON:      'Barème Macron',
  IMMIGRATION_JALONS: 'Jalons procéduraux',
  IMMIGRATION_PIECES: 'Pièces requises',
  PENSION_TAUX:       'Barème pension alimentaire',
  PRESTATION_COEFF:   'Prestation compensatoire',
};

@Component({
  selector: 'app-referentials',
  standalone: true,
  imports: [
    MatExpansionModule, MatIconModule, MatProgressSpinnerModule, MatChipsModule,
    JsonPipe,
  ],
  templateUrl: './referentials.component.html',
  styleUrl: './referentials.component.scss',
  animations: [fadeInUp],
  host: { '[@fadeInUp]': '' },
})
export class ReferentialsComponent implements OnInit {
  loading = signal(true);
  error = signal(false);
  sections = signal<SectionDisplay[]>([]);
  domainLabel = signal('');

  constructor(
    private referentialService: ReferentialService,
    private workspaceService: WorkspaceService
  ) {}

  ngOnInit(): void {
    this.workspaceService.getCurrentWorkspace().subscribe({
      next: ws => {
        const domain = ws.legalDomain;
        this.domainLabel.set(this.formatDomain(domain));
        if (!domain) {
          this.loading.set(false);
          return;
        }
        this.referentialService.getReferentials(domain).subscribe({
          next: (response: ReferentialResponse) => {
            this.sections.set(this.buildSections(response));
            this.loading.set(false);
          },
          error: () => {
            this.error.set(true);
            this.loading.set(false);
          }
        });
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }

  private buildSections(response: ReferentialResponse): SectionDisplay[] {
    return Object.entries(response.sections).map(([type, entries]) => ({
      type,
      title: SECTION_LABELS[type] ?? type,
      entries,
    }));
  }

  private formatDomain(domain?: string): string {
    switch (domain) {
      case 'DROIT_DU_TRAVAIL':  return 'Droit du travail';
      case 'DROIT_FAMILLE':     return 'Droit de la famille';
      case 'DROIT_IMMIGRATION': return 'Droit de l\'immigration';
      default:                  return domain ?? '';
    }
  }

  formatValue(entry: ReferentialEntry, sectionType: string): string {
    try {
      const val = JSON.parse(entry.valueJson);
      switch (sectionType) {
        case 'LITIGATION_TYPE':
          return `${val.years} an${val.years > 1 ? 's' : ''} — ${val.article}`;
        case 'IMMIGRATION_JALONS':
          return (val as { label: string; offsetDays: number }[])
            .map(j => `${j.label} (J+${j.offsetDays})`)
            .join('\n');
        case 'IMMIGRATION_PIECES':
          return (val as string[]).join('\n');
        case 'PRESTATION_COEFF':
          return `Coefficient : ${(val.coeff * 100).toFixed(0)} % — Durée de référence : ${val.dureeReferenceAns} ans`;
        default:
          return JSON.stringify(val, null, 2);
      }
    } catch {
      return entry.valueJson;
    }
  }

  sectionIcon(type: string): string {
    switch (type) {
      case 'LITIGATION_TYPE':    return 'gavel';
      case 'BAREME_MACRON':      return 'calculate';
      case 'IMMIGRATION_JALONS': return 'timeline';
      case 'IMMIGRATION_PIECES': return 'checklist';
      case 'PENSION_TAUX':       return 'child_care';
      case 'PRESTATION_COEFF':   return 'balance';
      default:                   return 'info';
    }
  }
}
