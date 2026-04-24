import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

/**
 * SF-155-03 — composant partagé pour afficher un flag contextuel dans le header
 * d'un composant décisionnel (ex. "CRA" pour oqtf-sans-delai, "TRANSFERT IMMINENT"
 * pour annexe13-be).
 *
 * Factorise le pattern "chip additionnel dans le header" observé sur plusieurs
 * outils décisionnels (évite la dette de convergence identifiée dans l'audit
 * F-155 / D8 : 2 mécanismes ad-hoc pour le même besoin).
 *
 * Inputs :
 *  - `label`   : texte affiché
 *  - `icon`    : nom d'icône Material optionnel (ex. 'warning', 'local_shipping')
 *  - `variant` : severity — 'warning' (or, usage par défaut), 'danger' (rouge,
 *                réservé aux alertes critiques documentées), 'info' (navy).
 */
@Component({
  selector: 'app-decisional-header-flag',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <span class="dhf" [class.dhf--warning]="variant === 'warning'"
          [class.dhf--danger]="variant === 'danger'"
          [class.dhf--info]="variant === 'info'"
          role="status" [attr.aria-label]="label">
      @if (icon) {
        <mat-icon class="dhf-icon" aria-hidden="true">{{ icon }}</mat-icon>
      }
      <span class="dhf-label">{{ label }}</span>
    </span>
  `,
  styles: [`
    .dhf {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      padding: 4px 10px;
      border-radius: 12px;
      font-weight: 700;
      font-size: 11px;
      letter-spacing: 0.05em;
      text-transform: uppercase;
      white-space: nowrap;
      border: 1px solid transparent;
    }

    .dhf-icon {
      font-size: 14px;
      width: 14px;
      height: 14px;
      line-height: 1;
    }

    .dhf-label {
      line-height: 1;
    }

    // Variante "warning" — or juridique (cas par défaut, ex. CRA actif).
    .dhf--warning {
      background: rgba(201, 165, 75, 0.28);
      color: #8a6a16;
      border-color: var(--ds-accent-gold, #c9a54b);
    }

    // Variante "danger" — rouge (réservé aux alertes critiques documentées,
    // ex. transfert imminent, urgence absolue).
    .dhf--danger {
      background: rgba(192, 36, 36, 0.14);
      color: #8a1616;
      border-color: #c02424;
    }

    // Variante "info" — navy (contexte informatif).
    .dhf--info {
      background: rgba(26, 35, 48, 0.08);
      color: #1a2330;
      border-color: rgba(26, 35, 48, 0.2);
    }
  `],
})
export class DecisionalHeaderFlagComponent {
  @Input({ required: true }) label!: string;
  @Input() icon: string | null = null;
  @Input() variant: 'warning' | 'danger' | 'info' = 'warning';
}
