import { Component, Inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface ReferentialWarningDialogData {
  warning: string;
}

@Component({
  selector: 'app-referential-warning-dialog',
  standalone: true,
  imports: [MatDialogModule, MatButtonModule, MatIconModule],
  templateUrl: './referential-warning-dialog.component.html',
  styleUrl: './referential-warning-dialog.component.scss',
})
export class ReferentialWarningDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ReferentialWarningDialogComponent, boolean>,
    @Inject(MAT_DIALOG_DATA) public data: ReferentialWarningDialogData
  ) {}

  confirm(): void { this.dialogRef.close(true); }
  cancel(): void  { this.dialogRef.close(false); }
}
