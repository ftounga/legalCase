import {
  Component,
  Input,
  OnInit,
  OnDestroy,
  signal,
  computed
} from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSnackBar } from '@angular/material/snack-bar';
import { TimeService } from '../../core/services/time.service';
import { AuthService } from '../../core/services/auth.service';
import { TimeEntryResponse } from '../../core/models/time-tracking.models';

@Component({
  selector: 'app-timer-widget',
  standalone: true,
  imports: [MatButtonModule, MatIconModule, MatTooltipModule],
  templateUrl: './timer-widget.component.html',
  styleUrl: './timer-widget.component.scss'
})
export class TimerWidgetComponent implements OnInit, OnDestroy {
  @Input() caseFileId!: string;

  elapsedSeconds = signal(0);
  /** true si un timer est actif sur un AUTRE dossier (409 reçu) */
  blockedByOtherTimer = signal(false);
  noRateConfigured = signal(false);

  private intervalRef: ReturnType<typeof setInterval> | null = null;

  /** Entrée active appartenant à l'utilisateur courant uniquement */
  readonly activeEntry = computed(() => {
    const entry = this.timeService.activeEntry();
    const currentUserId = this.authService.currentUser()?.id;
    if (!entry || !currentUserId || entry.userId !== currentUserId) return null;
    return entry;
  });

  readonly totalCompletedSeconds = computed(() =>
    this.timeService.entries()
      .filter(e => e.durationSeconds != null)
      .reduce((acc, e) => acc + e.durationSeconds!, 0)
  );

  readonly totalSeconds = computed(() =>
    this.totalCompletedSeconds() + (this.activeEntry() ? this.elapsedSeconds() : 0)
  );

  constructor(
    readonly timeService: TimeService,
    readonly authService: AuthService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    this.timeService.getBillingRate().subscribe({
      next: rate => { this.noRateConfigured.set(rate == null); },
      error: () => {}
    });

    this.timeService.loadEntries(this.caseFileId).subscribe({
      next: () => {
        const active = this.activeEntry();
        if (active) {
          this.resumeTimer(active);
        }
      },
      error: () => {}
    });
  }

  ngOnDestroy(): void {
    this.clearInterval();
  }

  onStart(): void {
    if (this.blockedByOtherTimer() || this.noRateConfigured()) return;
    this.timeService.startTimer(this.caseFileId).subscribe({
      next: entry => {
        this.elapsedSeconds.set(0);
        this.startInterval();
        this.snackBar.open('Chrono démarré', 'Fermer', {
          duration: 2000, panelClass: ['snack-success']
        });
      },
      error: (err: any) => {
        if (err?.status === 409) {
          this.blockedByOtherTimer.set(true);
          this.snackBar.open(
            'Un timer est déjà actif sur un autre dossier. Arrêtez-le d\'abord.',
            'Fermer',
            { duration: 5000, panelClass: ['snack-error'] }
          );
        } else {
          this.snackBar.open('Erreur lors du démarrage du chrono.', 'Fermer', {
            duration: 4000, panelClass: ['snack-error']
          });
        }
      }
    });
  }

  onStop(): void {
    const entry = this.timeService.activeEntry();
    if (!entry) return;
    this.timeService.stopTimer(entry.id).subscribe({
      next: () => {
        this.clearInterval();
        this.elapsedSeconds.set(0);
        this.blockedByOtherTimer.set(false);
        this.snackBar.open('Chrono arrêté', 'Fermer', {
          duration: 2000, panelClass: ['snack-success']
        });
      },
      error: () => {
        this.snackBar.open('Erreur lors de l\'arrêt du chrono.', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });
  }

  get formattedElapsed(): string {
    const s = this.elapsedSeconds();
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    return [
      String(h).padStart(2, '0'),
      String(m).padStart(2, '0'),
      String(sec).padStart(2, '0')
    ].join(':');
  }

  formatDuration(seconds: number): string {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    if (h > 0) return `${h}h ${String(m).padStart(2, '0')}min`;
    if (m > 0) return `${m}min`;
    return `${seconds}s`;
  }

  private resumeTimer(entry: TimeEntryResponse): void {
    const startedMs = new Date(entry.startedAt).getTime();
    const nowMs = Date.now();
    const elapsed = Math.floor((nowMs - startedMs) / 1000);
    // SF-106-06 : si le timer est stale (> 2h), notifier l'utilisateur.
    const TWO_HOURS = 2 * 3600;
    if (elapsed > TWO_HOURS) {
      const hours = Math.floor(elapsed / 3600);
      const minutes = Math.floor((elapsed % 3600) / 60);
      this.snackBar.open(
        `Le chrono était actif depuis ${hours}h${minutes > 0 ? minutes + 'min' : ''}. Pensez à l'arrêter si vous ne travaillez plus sur ce dossier.`,
        'Fermer',
        { duration: 10000, panelClass: ['snack-warning'] }
      );
    }
    this.elapsedSeconds.set(Math.max(0, elapsed));
    this.startInterval();
  }

  private startInterval(): void {
    this.clearInterval();
    this.intervalRef = setInterval(() => {
      this.elapsedSeconds.update(v => v + 1);
    }, 1000);
  }

  private clearInterval(): void {
    if (this.intervalRef !== null) {
      clearInterval(this.intervalRef);
      this.intervalRef = null;
    }
  }
}
