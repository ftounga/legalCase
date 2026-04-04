import { Component, Inject } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { ReferentialEntry } from '../../core/models/referential.model';

export interface ReferentialEditDialogData {
  entry: ReferentialEntry;
  sectionType: string;
}

export interface ReferentialEditDialogResult {
  label: string;
  valueJson: string;
  force: boolean;
}

@Component({
  selector: 'app-referential-edit-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule, MatFormFieldModule, MatInputModule, MatButtonModule,
  ],
  templateUrl: './referential-edit-dialog.component.html',
})
export class ReferentialEditDialogComponent {
  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<ReferentialEditDialogComponent, ReferentialEditDialogResult>,
    @Inject(MAT_DIALOG_DATA) public data: ReferentialEditDialogData
  ) {
    this.form = this.fb.group({
      label: [data.entry.label, [Validators.required, Validators.maxLength(500)]],
      valueJson: [data.entry.valueJson, [Validators.required, this.jsonValidator]],
    });
  }

  private jsonValidator(control: { value: string }) {
    try {
      JSON.parse(control.value);
      return null;
    } catch {
      return { invalidJson: true };
    }
  }

  submit(): void {
    if (this.form.invalid) return;
    this.dialogRef.close({
      label: this.form.value.label,
      valueJson: this.form.value.valueJson,
      force: false,
    });
  }

  cancel(): void {
    this.dialogRef.close();
  }
}
