import { Component, Input, OnInit, signal, computed } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FormsModule } from '@angular/forms';
import { DivorceChecklistService } from '../../core/services/divorce-checklist.service';
import { DivorceChecklistResponse, DivorceEtapeStatus, DivorcePieceStatus } from '../../core/models/divorce-checklist.model';

@Component({
  selector: 'app-divorce-checklist-section',
  standalone: true,
  imports: [FormsModule, MatButtonModule, MatIconModule, MatSelectModule, MatFormFieldModule, MatProgressSpinnerModule],
  templateUrl: './divorce-checklist-section.component.html',
  styleUrl: './divorce-checklist-section.component.scss'
})
export class DivorceChecklistSectionComponent implements OnInit {
  @Input() caseFileId!: string;

  collapsed = signal(true);
  loading = signal(false);
  saving = signal(false);
  result = signal<DivorceChecklistResponse | null>(null);
  country = signal('FRANCE');

  progress = computed(() => {
    const r = this.result();
    if (!r) return 0;
    const total = r.etapesTotal + r.piecesTotal;
    const done = r.etapesCompletees + r.piecesPresentes;
    return total > 0 ? Math.round((done / total) * 100) : 0;
  });

  constructor(private checklistService: DivorceChecklistService, private snackBar: MatSnackBar) {}

  ngOnInit(): void { this.loadExisting(); }
  toggleCollapsed(): void { this.collapsed.update(v => !v); }

  loadExisting(): void {
    this.loading.set(true);
    this.checklistService.get(this.caseFileId).subscribe({
      next: r => { this.result.set(r); this.country.set(r.country); this.loading.set(false); },
      error: () => { this.loading.set(false); },
    });
  }

  toggleEtape(etape: DivorceEtapeStatus): void {
    etape.statut = etape.statut === 'FAIT' ? 'A_FAIRE' : 'FAIT';
    this.saveChecklist();
  }

  togglePiece(piece: DivorcePieceStatus): void {
    piece.statut = piece.statut === 'PRESENTE' ? 'MANQUANTE' : 'PRESENTE';
    this.saveChecklist();
  }

  initChecklist(): void {
    this.saveChecklist();
  }

  private saveChecklist(): void {
    this.saving.set(true);
    const r = this.result();
    const etapeStatuts: Record<string, string> = {};
    const pieceStatuts: Record<string, string> = {};
    if (r) {
      r.etapes.forEach(e => etapeStatuts[e.code] = e.statut);
      r.pieces.forEach(p => pieceStatuts[p.code] = p.statut);
    }
    this.checklistService.save(this.caseFileId, {
      country: this.country(), etapeStatuts, pieceStatuts,
    }).subscribe({
      next: resp => { this.result.set(resp); this.saving.set(false); },
      error: () => { this.saving.set(false); this.snackBar.open('Erreur', 'Fermer', { duration: 4000 }); },
    });
  }
}
