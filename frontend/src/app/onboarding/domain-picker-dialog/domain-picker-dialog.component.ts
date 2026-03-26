import { Component, Inject } from '@angular/core';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { CommonModule } from '@angular/common';

export interface DomainPickerData {
  workspaceName: string;
}

export interface DomainPickerResult {
  legalDomain: string;
  country: string;
}

interface LegalDomainTile {
  key: string;
  label: string;
  subtitle: string;
  icon: string;
  color: string;
  bgColor: string;
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
      color: '#27AE60',
      bgColor: 'rgba(39, 174, 96, 0.16)'
    },
    {
      key: 'DROIT_IMMIGRATION',
      label: "Droit de l'immigration",
      subtitle: 'Titres de séjour, visas, naturalisation',
      icon: 'flight',
      color: '#1A3A5C',
      bgColor: 'rgba(26, 58, 92, 0.16)'
    },
    {
      key: 'DROIT_FAMILLE',
      label: 'Droit de la famille',
      subtitle: "Divorce, garde d'enfants, successions",
      icon: 'family_restroom',
      color: '#C9973A',
      bgColor: 'rgba(201, 120, 20, 0.16)'
    }
  ];

  readonly countries = [
    { key: 'FRANCE', label: 'France' },
    { key: 'BELGIQUE', label: 'Belgique' }
  ];

  selected = 'DROIT_DU_TRAVAIL';
  selectedCountry = 'FRANCE';

  get selectedDomainColor(): string {
    return this.domains.find(d => d.key === this.selected)?.color ?? '#1A3A5C';
  }

  get selectedDomainBgColor(): string {
    return this.domains.find(d => d.key === this.selected)?.bgColor ?? 'rgba(26,58,92,0.16)';
  }

  constructor(
    private dialogRef: MatDialogRef<DomainPickerDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: DomainPickerData
  ) {
    this.dialogRef.disableClose = true;
  }

  get canConfirm(): boolean {
    return !!this.selected && !!this.selectedCountry;
  }

  domainColor(key: string): string {
    return this.domains.find(d => d.key === key)?.color ?? '#1A3A5C';
  }

  confirm(): void {
    if (!this.canConfirm) return;
    this.dialogRef.close({ legalDomain: this.selected, country: this.selectedCountry } as DomainPickerResult);
  }
}
