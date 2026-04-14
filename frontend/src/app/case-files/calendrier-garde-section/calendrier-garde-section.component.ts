import { Component, Input, OnInit, OnChanges, SimpleChanges, signal } from '@angular/core';
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

const MODES_FR = new Set(['ALTERNEE_FR', 'DVH_CLASSIQUE_FR', 'DVH_ELARGI_FR']);
const MODES_BE = new Set(['ALTERNEE_BE', 'SECONDAIRE_BE', 'SECONDAIRE_ELARGI_BE']);

@Component({
  selector: 'app-calendrier-garde-section',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatIconModule, MatSelectModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule],
  templateUrl: './calendrier-garde-section.component.html',
  styleUrl: './calendrier-garde-section.component.scss'
})
export class CalendrierGardeSectionComponent implements OnInit, OnChanges {
  @Input() caseFileId!: string;
  @Input() aiModeGardeDetaille?: string | null;
  @Input() workspaceCountry: string = 'FRANCE';

  collapsed = signal(true);
  loading = signal(false);
  generating = signal(false);
  showForm = signal(true);
  result = signal<CalendrierGardeResponse | null>(null);

  gardeCode = signal('ALTERNEE_FR');
  parentANom = signal('');
  parentBNom = signal('');
  modeDetailleNote = signal<string | null>(null);

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

  ngOnInit(): void {
    this.loadExisting();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['aiModeGardeDetaille'] && this.showForm() && !this.result()) {
      this.applyAiPrefill();
    }
  }

  toggleCollapsed(): void { this.collapsed.update(v => !v); }

  loadExisting(): void {
    this.loading.set(true);
    this.gardeService.get(this.caseFileId).subscribe({
      next: r => { this.result.set(r); this.showForm.set(false); this.loading.set(false); },
      error: () => { this.showForm.set(true); this.loading.set(false); this.applyAiPrefill(); },
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

  onGardeCodeChange(): void {
    // L'avocat a modifié le mode — on efface la note de pré-remplissage.
    this.modeDetailleNote.set(null);
  }

  private applyAiPrefill(): void {
    const ai = this.aiModeGardeDetaille?.toUpperCase();
    if (!ai) { this.modeDetailleNote.set(null); return; }
    const wsFR = this.workspaceCountry === 'FRANCE';
    const isFR = MODES_FR.has(ai);
    const isBE = MODES_BE.has(ai);
    if (!isFR && !isBE) { this.modeDetailleNote.set(null); return; }
    if ((wsFR && isFR) || (!wsFR && isBE)) {
      this.gardeCode.set(ai);
      this.modeDetailleNote.set(null);
    } else {
      this.modeDetailleNote.set(
        `L'IA a détecté le mode "${ai}" (autre pays). Vérifier que ce dossier est adapté.`
      );
    }
  }
}
