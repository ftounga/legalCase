import { Component, Inject } from '@angular/core';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';

export interface DomainPickerData {
  workspaceName: string;
}

interface LegalDomainTile {
  key: string;
  label: string;
  subtitle: string;
  icon: string;
  available: boolean;
}

@Component({
  selector: 'app-domain-picker-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './domain-picker-dialog.component.html',
  styleUrl: './domain-picker-dialog.component.scss'
})
export class DomainPickerDialogComponent {
  readonly domains: LegalDomainTile[] = [
    {
      key: 'DROIT_DU_TRAVAIL',
      label: 'Droit du travail',
      subtitle: 'Licenciement, harcèlement, rupture conventionnelle',
      icon: 'gavel',
      available: true
    },
    {
      key: 'DROIT_IMMIGRATION',
      label: "Droit de l'immigration",
      subtitle: 'Titres de séjour, visas, naturalisation',
      icon: 'flight',
      available: false
    },
    {
      key: 'DROIT_IMMOBILIER',
      label: 'Droit immobilier',
      subtitle: 'Baux, copropriété, transactions',
      icon: 'home',
      available: false
    }
  ];

  selected = 'DROIT_DU_TRAVAIL';

  constructor(
    private dialogRef: MatDialogRef<DomainPickerDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DomainPickerData
  ) {
    this.dialogRef.disableClose = true;
  }

  confirm(): void {
    this.dialogRef.close(this.selected);
  }
}
