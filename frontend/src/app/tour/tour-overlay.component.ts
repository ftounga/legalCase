import {
  Component, OnInit, OnDestroy, inject, DestroyRef, signal, effect, HostListener
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgStyle } from '@angular/common';
import { Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TourService } from '../core/services/tour.service';
import { CaseFileService } from '../core/services/case-file.service';

interface TourStep {
  target: string | null;
  icon: string;
  title: string;
  description: string;
}

const TOUR_STEPS: TourStep[] = [
  {
    target: null,
    icon: 'celebration',
    title: 'Bienvenue sur AI LegalCase !',
    description: 'Votre workspace est prêt. En quelques étapes, découvrez les fonctionnalités clés de votre assistant juridique.'
  },
  {
    target: 'new-dossier-btn',
    icon: 'folder_open',
    title: 'Créez votre premier dossier',
    description: 'Un dossier regroupe tous les documents d\'une affaire. Cliquez sur ce bouton pour commencer.'
  },
  {
    target: 'upload-trigger-btn',
    icon: 'upload_file',
    title: 'Ajoutez vos documents',
    description: 'Uploadez vos contrats, courriers et décisions. PDF, Word et images sont pris en charge.'
  },
  {
    target: 'analyze-btn',
    icon: 'auto_fix_high',
    title: 'Lancez une analyse',
    description: 'En un clic, vos documents sont analysés et une synthèse juridique complète est générée.'
  },
  {
    target: 'synthesis-link',
    icon: 'summarize',
    title: 'Consultez la synthèse',
    description: 'Accédez à la timeline, aux faits clés, aux risques juridiques et aux points importants identifiés.'
  }
];

@Component({
  selector: 'app-tour-overlay',
  standalone: true,
  imports: [NgStyle, MatButtonModule, MatIconModule],
  templateUrl: './tour-overlay.component.html',
  styleUrl: './tour-overlay.component.scss'
})
export class TourOverlayComponent implements OnInit, OnDestroy {
  protected tourService = inject(TourService);
  private router = inject(Router);
  private destroyRef = inject(DestroyRef);
  private caseFileService = inject(CaseFileService);

  cardStyle = signal<Record<string, string>>({ top: '80px', left: '50%', transform: 'translateX(-50%)' });
  readonly steps = TOUR_STEPS;

  private highlightedEl: Element | null = null;

  constructor() {
    effect(() => {
      // Réagit aux changements de step
      this.tourService.currentStep();
      setTimeout(() => this.updatePosition(), 80);
    });
  }

  get step(): TourStep {
    return TOUR_STEPS[this.tourService.currentStep()];
  }

  get isLastStep(): boolean {
    return this.tourService.currentStep() === TOUR_STEPS.length - 1;
  }

  ngOnInit(): void {
    this.router.events
      .pipe(
        filter(e => e instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe(() => setTimeout(() => this.updatePosition(), 100));

    setTimeout(() => this.updatePosition(), 80);
  }

  @HostListener('window:resize')
  onResize(): void {
    this.updatePosition();
  }

  next(): void {
    if (this.tourService.currentStep() === 1) {
      this.caseFileService.list(0, 1).subscribe({
        next: page => this.tourService.advanceToStep2(page.totalElements > 0),
        error: () => this.tourService.advanceToStep2(false)
      });
    } else {
      this.tourService.next();
    }
  }
  skip(): void { this.tourService.skip(); }

  private updatePosition(): void {
    this.clearHighlight();

    const target = this.step.target;
    if (!target) {
      this.cardStyle.set({ top: '80px', left: '50%', transform: 'translateX(-50%)' });
      return;
    }

    const el = document.querySelector(`[data-tour-target="${target}"]`);
    if (!el) {
      this.cardStyle.set({ bottom: '32px', right: '32px', top: 'auto', left: 'auto', transform: 'none' });
      return;
    }

    el.classList.add('tour-spotlight');
    this.highlightedEl = el;

    const rect = el.getBoundingClientRect();
    const cardWidth = 360;
    const cardHeight = 180;
    const margin = 12;

    let top = rect.bottom + margin;
    let left = Math.max(16, Math.min(rect.left, window.innerWidth - cardWidth - 16));

    if (top + cardHeight > window.innerHeight - 16) {
      top = Math.max(16, rect.top - cardHeight - margin);
    }

    this.cardStyle.set({ top: `${top}px`, left: `${left}px`, transform: 'none' });
  }

  private clearHighlight(): void {
    if (this.highlightedEl) {
      this.highlightedEl.classList.remove('tour-spotlight');
      this.highlightedEl = null;
    }
  }

  ngOnDestroy(): void {
    this.clearHighlight();
  }
}
