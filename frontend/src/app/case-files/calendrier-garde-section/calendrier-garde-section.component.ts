import { Component, Input, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { CalendrierGardeService } from '../../core/services/calendrier-garde.service';
import { CalendrierGardeResponse } from '../../core/models/calendrier-garde.model';

@Component({
  selector: 'app-calendrier-garde-section',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatIconModule, MatSelectModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule],
  templateUrl: './calendrier-garde-section.component.html',
  styleUrl: './calendrier-garde-section.component.scss'
})
export class CalendrierGardeSectionComponent implements OnInit {
  @Input() caseFileId!: string;

  collapsed = signal(true);
  loading = signal(false);
  generating = signal(false);
  showForm = signal(true);
  result = signal<CalendrierGardeResponse | null>(null);

  gardeCode = signal('ALTERNEE_FR');
  parentANom = signal('');
  parentBNom = signal('');

  readonly modes = [
    { group: 'France', items: [
      { value: 'ALTERNEE_FR', label: 'Résidence alternée' },
      { value: 'DVH_CLASSIQUE_FR', label: 'DVH classique' },
      { value: 'DVH_ELARGI_FR', label: 'DVH élargi' },
    ]},
    { group: 'Belgique', items: [
      { value: 'ALTERNEE_BE', label: 'Hébergement égalitaire' },
      { value: 'SECONDAIRE_BE', label: 'Hébergement secondaire' },
      { value: 'SECONDAIRE_ELARGI_BE', label: 'Hébergement secondaire élargi' },
    ]},
  ];

  constructor(private gardeService: CalendrierGardeService, private snackBar: MatSnackBar) {}

  ngOnInit(): void { this.loadExisting(); }
  toggleCollapsed(): void { this.collapsed.update(v => !v); }

  loadExisting(): void {
    this.loading.set(true);
    this.gardeService.get(this.caseFileId).subscribe({
      next: r => { this.result.set(r); this.showForm.set(false); this.loading.set(false); },
      error: () => { this.showForm.set(true); this.loading.set(false); },
    });
  }

  generate(): void {
    this.generating.set(true);
    this.gardeService.generate(this.caseFileId, {
      gardeCode: this.gardeCode(), parentANom: this.parentANom(), parentBNom: this.parentBNom(),
    }).subscribe({
      next: r => { this.result.set(r); this.showForm.set(false); this.generating.set(false); },
      error: () => { this.generating.set(false); this.snackBar.open('Erreur', 'Fermer', { duration: 4000 }); },
    });
  }

  editForm(): void { this.showForm.set(true); }
}
