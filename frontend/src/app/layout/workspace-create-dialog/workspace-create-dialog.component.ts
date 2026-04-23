import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { WorkspaceService } from '../../core/services/workspace.service';
import { Workspace } from '../../core/models/workspace.model';

interface LegalDomainTile {
  key: 'DROIT_DU_TRAVAIL' | 'DROIT_IMMIGRATION' | 'DROIT_FAMILLE';
  label: string;
  subtitle: string;
  icon: string;
  color: string;
  bgColor: string;
}

/**
 * F-154 SF-154-01 : dialog de création d'un nouveau workspace depuis le
 * switcher de la toolbar. Réutilise l'endpoint POST /api/v1/workspaces
 * déjà en place.
 *
 * <p>Résultat : au succès, retourne le Workspace créé au parent (Shell)
 * qui déclenche le switch automatique vers ce nouveau workspace.
 */
@Component({
  selector: 'app-workspace-create-dialog',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatDialogModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatProgressSpinnerModule,
  ],
  templateUrl: './workspace-create-dialog.component.html',
  styleUrl: './workspace-create-dialog.component.scss',
})
export class WorkspaceCreateDialogComponent {
  readonly domains: LegalDomainTile[] = [
    {
      key: 'DROIT_DU_TRAVAIL',
      label: 'Droit du travail',
      subtitle: 'Licenciement, harcèlement, rupture conventionnelle',
      icon: 'gavel',
      color: '#27AE60',
      bgColor: 'rgba(39, 174, 96, 0.16)',
    },
    {
      key: 'DROIT_IMMIGRATION',
      label: "Droit de l'immigration",
      subtitle: 'Titres de séjour, visas, naturalisation',
      icon: 'flight',
      color: '#1A3A5C',
      bgColor: 'rgba(26, 58, 92, 0.16)',
    },
    {
      key: 'DROIT_FAMILLE',
      label: 'Droit de la famille',
      subtitle: "Divorce, garde d'enfants, successions",
      icon: 'family_restroom',
      color: '#C9973A',
      bgColor: 'rgba(201, 120, 20, 0.16)',
    },
  ];

  readonly countries: ReadonlyArray<{ key: 'FRANCE' | 'BELGIQUE'; label: string }> = [
    { key: 'FRANCE', label: 'France' },
    { key: 'BELGIQUE', label: 'Belgique' },
  ];

  name = '';
  selectedDomain: LegalDomainTile['key'] = 'DROIT_DU_TRAVAIL';
  selectedCountry: 'FRANCE' | 'BELGIQUE' = 'FRANCE';

  readonly submitting = signal(false);

  private readonly dialogRef = inject<MatDialogRef<WorkspaceCreateDialogComponent, Workspace | null>>(MatDialogRef);
  private readonly workspaceService = inject(WorkspaceService);
  private readonly snackBar = inject(MatSnackBar);

  get canSubmit(): boolean {
    return (
      !this.submitting() &&
      this.name.trim().length > 0 &&
      !!this.selectedDomain &&
      !!this.selectedCountry
    );
  }

  domainColor(key: LegalDomainTile['key']): string {
    return this.domains.find(d => d.key === key)?.color ?? '#1A3A5C';
  }

  selectDomain(key: LegalDomainTile['key']): void {
    this.selectedDomain = key;
  }

  cancel(): void {
    if (this.submitting()) return;
    this.dialogRef.close(null);
  }

  submit(): void {
    if (!this.canSubmit) return;
    this.submitting.set(true);
    this.workspaceService.createWorkspace(
      this.name.trim(),
      this.selectedDomain,
      this.selectedCountry
    ).subscribe({
      next: (ws) => {
        this.submitting.set(false);
        this.dialogRef.close(ws);
      },
      error: () => {
        this.submitting.set(false);
        this.snackBar.open(
          'Impossible de créer le workspace. Réessayez dans un instant.',
          'Fermer',
          { duration: 5000, panelClass: ['snack-error'] }
        );
      },
    });
  }
}
