import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../core/services/auth.service';
import { HelpService } from '../core/services/help.service';

export const SUGGESTED_QUESTIONS = [
  'Comment créer un dossier ?',
  'Comment ajouter un document ?',
  'Comment lancer une analyse IA ?',
];

@Component({
  selector: 'app-help-chat-widget',
  standalone: true,
  imports: [
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './help-chat-widget.component.html',
  styleUrl: './help-chat-widget.component.scss',
})
export class HelpChatWidgetComponent {
  protected authService = inject(AuthService);
  private helpService = inject(HelpService);

  readonly suggestedQuestions = SUGGESTED_QUESTIONS;

  panelOpen = signal(false);
  isLoading = signal(false);
  answer = signal<string | null>(null);
  errorMessage = signal<string | null>(null);

  message = '';

  get canSend(): boolean {
    return this.message.trim().length > 0 && this.message.length <= 500 && !this.isLoading();
  }

  togglePanel(): void {
    this.panelOpen.update(v => !v);
  }

  closePanel(): void {
    this.panelOpen.set(false);
  }

  sendSuggestion(question: string): void {
    this.message = question;
    this.send();
  }

  send(): void {
    if (!this.canSend) return;

    this.isLoading.set(true);
    this.answer.set(null);
    this.errorMessage.set(null);

    this.helpService.chat(this.message).subscribe({
      next: response => {
        this.answer.set(response.answer);
        this.message = '';
        this.isLoading.set(false);
      },
      error: () => {
        this.errorMessage.set('Service temporairement indisponible. Veuillez réessayer dans quelques instants.');
        this.isLoading.set(false);
      },
    });
  }
}
