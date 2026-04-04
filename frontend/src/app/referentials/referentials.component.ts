import { Component, OnInit, signal } from '@angular/core';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ReferentialService } from '../core/services/referential.service';
import { WorkspaceService } from '../core/services/workspace.service';
import { WorkspaceMemberService } from '../core/services/workspace-member.service';
import { AuthService } from '../core/services/auth.service';
import { ReferentialEntry, ReferentialResponse } from '../core/models/referential.model';
import { fadeInUp } from '../shared/animations';
import {
  ReferentialEditDialogComponent,
  ReferentialEditDialogData,
  ReferentialEditDialogResult,
} from './referential-edit-dialog/referential-edit-dialog.component';

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
    MatExpansionModule, MatIconModule, MatProgressSpinnerModule,
    MatChipsModule, MatButtonModule,
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
  canEdit = signal(false);
  private domain = '';

  constructor(
    private referentialService: ReferentialService,
    private workspaceService: WorkspaceService,
    private workspaceMemberService: WorkspaceMemberService,
    private authService: AuthService,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.workspaceService.getCurrentWorkspace().subscribe({
      next: ws => {
        const domain = ws.legalDomain ?? '';
        this.domain = domain;
        this.domainLabel.set(this.formatDomain(domain));

        // Check role
        this.workspaceMemberService.getMembers().subscribe({
          next: members => {
            const currentUserId = this.authService.currentUser()?.id;
            const me = members.find(m => m.userId === currentUserId);
            this.canEdit.set(me?.memberRole === 'OWNER' || me?.memberRole === 'ADMIN');
          }
        });

        if (!domain) {
          this.loading.set(false);
          return;
        }
        this.loadReferentials();
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }

  private loadReferentials(): void {
    this.referentialService.getReferentials(this.domain).subscribe({
      next: (response: ReferentialResponse) => {
        this.sections.set(this.buildSections(response));
        this.loading.set(false);
      },
      error: () => {
        this.error.set(true);
        this.loading.set(false);
      }
    });
  }

  openEditDialog(entry: ReferentialEntry, sectionType: string): void {
    const dialogRef = this.dialog.open<
      ReferentialEditDialogComponent,
      ReferentialEditDialogData,
      ReferentialEditDialogResult
    >(ReferentialEditDialogComponent, {
      width: '600px',
      data: { entry, sectionType },
    });

    dialogRef.afterClosed().subscribe(result => {
      if (result) {
        this.submitUpdate(entry, result);
      }
    });
  }

  private submitUpdate(entry: ReferentialEntry, result: ReferentialEditDialogResult): void {
    if (!entry.id) return;
    this.referentialService.updateReferential(entry.id, result.label, result.valueJson, result.force)
      .subscribe({
        next: response => {
          if (response.saved) {
            this.snackBar.open('Référentiel mis à jour.', 'Fermer', {
              duration: 4000, panelClass: ['snack-success']
            });
            this.loadReferentials();
          } else if (response.warning) {
            this.showWarningConfirmation(entry, result, response.warning);
          }
        },
        error: () => {
          this.snackBar.open('Erreur lors de la mise à jour.', 'Fermer', {
            duration: 4000, panelClass: ['snack-error']
          });
        }
      });
  }

  private showWarningConfirmation(entry: ReferentialEntry, result: ReferentialEditDialogResult, warning: string): void {
    const confirmed = confirm(
      `L'IA a détecté une possible divergence :\n\n${warning}\n\nSauvegarder quand même ?`
    );
    if (confirmed) {
      this.submitUpdate(entry, { ...result, force: true });
    }
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
