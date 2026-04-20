import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatChipsModule } from '@angular/material/chips';
import { SectionDoc, SECTION_DOCS } from '../section-docs';

/**
 * SF-140-01 : données injectées dans le dialog.
 * - Pour une aide de **type** (section), on reçoit juste `sectionType`.
 * - Pour une aide d'**entrée**, on reçoit en plus `entry` avec sa description
 *   métier déjà extraite du JSON côté parent.
 */
export interface ReferentialHelpDialogData {
  /** Type de référentiel (ex. CONVENTION_BAREMES) — sert à retrouver SECTION_DOCS. */
  sectionType: string;
  /** Si défini, affiche l'aide de l'entrée en complément de la section. */
  entry?: {
    key: string;
    label: string;
    country?: string | null;
    sourceRef?: string | null;
    metierDescription?: string;   // ex. description, conditions, juridiction
    rawJson?: string;             // repli pour power-users
  };
}

@Component({
  selector: 'app-referential-help-dialog',
  standalone: true,
  imports: [CommonModule, MatButtonModule, MatIconModule, MatDialogModule, MatChipsModule],
  templateUrl: './referential-help-dialog.component.html',
  styleUrl: './referential-help-dialog.component.scss',
})
export class ReferentialHelpDialogComponent {
  doc: SectionDoc | null;

  constructor(
    private dialogRef: MatDialogRef<ReferentialHelpDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: ReferentialHelpDialogData,
  ) {
    this.doc = SECTION_DOCS[data.sectionType] ?? null;
  }

  close(): void {
    this.dialogRef.close();
  }

  hasEntryDescription(): boolean {
    return !!this.data.entry?.metierDescription?.trim();
  }
}
