import {
  ChangeDetectionStrategy,
  ChangeDetectorRef,
  Component,
  EventEmitter,
  Input,
  OnInit,
  Output,
  computed,
  inject,
  signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PiecesWaveService } from '../../core/services/pieces-wave.service';
import { PendingPiece, PiecesWave } from '../../core/models/pieces-wave.model';

const MAX_VISIBLE_PIECES = 5;

/**
 * F-283 / SF-283-02 — carte « vague de pièces » (onglet Dossier, en tête).
 * Rend lisible l'ajout incrémental : « N pièce(s) ajoutée(s) depuis votre
 * dernière analyse ». Visible uniquement si `pendingCount > 0` (sinon le DOM
 * est vide — silence > placeholder criard). Le CTA « Relancer l'analyse »
 * émet `analyzeRequested` : le parent route vers l'action d'analyse existante
 * (aucune nouvelle pipeline introduite, cf. mini-spec SF-283-02).
 */
@Component({
  selector: 'app-pieces-wave-card',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule],
  templateUrl: './pieces-wave-card.component.html',
  styleUrl: './pieces-wave-card.component.scss',
})
export class PiecesWaveCardComponent implements OnInit {
  @Input({ required: true }) caseFileId!: string;
  /** Au clic « Relancer l'analyse » : demande au parent de lancer l'analyse existante. */
  @Output() analyzeRequested = new EventEmitter<void>();

  private readonly service = inject(PiecesWaveService);
  private readonly cdr = inject(ChangeDetectorRef);

  readonly wave = signal<PiecesWave | null>(null);
  readonly loading = signal(true);

  readonly pendingCount = computed(() => this.wave()?.pendingCount ?? 0);
  readonly visible = computed<PendingPiece[]>(() =>
    (this.wave()?.pendingPieces ?? []).slice(0, MAX_VISIBLE_PIECES),
  );
  readonly overflow = computed(() => Math.max(0, this.pendingCount() - MAX_VISIBLE_PIECES));
  readonly analyzedAt = computed(() => this.wave()?.analyzedAt ?? null);

  ngOnInit(): void {
    this.reload();
  }

  /** Rechargement (à appeler par le parent après upload / analyse). */
  reload(): void {
    this.loading.set(true);
    this.service.wave(this.caseFileId).subscribe({
      next: (w) => {
        this.wave.set(w);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
      error: () => {
        this.wave.set(null);
        this.loading.set(false);
        this.cdr.markForCheck();
      },
    });
  }

  onAnalyze(): void {
    this.analyzeRequested.emit();
  }
}
