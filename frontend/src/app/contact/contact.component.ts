import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ContactService } from './contact.service';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSnackBarModule
  ],
  templateUrl: './contact.component.html',
  styleUrl: './contact.component.scss'
})
export class ContactComponent {
  private fb = inject(FormBuilder);
  private contactService = inject(ContactService);
  private snackBar = inject(MatSnackBar);

  form = this.fb.group({
    nom:       ['', [Validators.required]],
    email:     ['', [Validators.required, Validators.email]],
    telephone: ['', [Validators.pattern(/^[\d\s\+\-\(\)]{7,20}$/)]],
    sujet:     ['', [Validators.required]],
    message:   ['', [Validators.required, Validators.maxLength(3000)]]
  });

  sending = false;
  sent = false;

  get nom(): AbstractControl { return this.form.get('nom')!; }
  get email(): AbstractControl { return this.form.get('email')!; }
  get telephone(): AbstractControl { return this.form.get('telephone')!; }
  get sujet(): AbstractControl { return this.form.get('sujet')!; }
  get message(): AbstractControl { return this.form.get('message')!; }

  submit(): void {
    if (this.form.invalid) return;
    this.sending = true;

    const value = this.form.value;
    const payload = {
      nom: value.nom!,
      email: value.email!,
      telephone: value.telephone || undefined,
      sujet: value.sujet!,
      message: value.message!
    };

    this.contactService.send(payload).subscribe({
      next: () => {
        this.sending = false;
        this.sent = true;
      },
      error: () => {
        this.sending = false;
        this.snackBar.open('Une erreur est survenue. Veuillez réessayer.', 'Fermer', { duration: 5000 });
      }
    });
  }

  reset(): void {
    this.form.reset();
    this.sent = false;
  }
}
