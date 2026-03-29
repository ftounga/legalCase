import { Component, signal, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, filter, switchMap } from 'rxjs/operators';
import { SynthesisSearchService } from '../core/services/synthesis-search.service';
import { SynthesisSearchResult } from '../core/models/search.model';
import { HighlightTermPipe } from '../shared/pipes/highlight-term.pipe';
import { fadeInUp } from '../shared/animations';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule, MatInputModule, MatIconModule,
    MatButtonModule, MatCardModule, MatProgressSpinnerModule,
    MatChipsModule,
    HighlightTermPipe
  ],
  templateUrl: './search.component.html',
  styleUrl: './search.component.scss',
  animations: [fadeInUp],
  host: { '[@fadeInUp]': '' },
})
export class SearchComponent implements OnInit, OnDestroy {
  queryControl = new FormControl('');
  results = signal<SynthesisSearchResult[]>([]);
  loading = signal(false);
  searched = signal(false);
  currentQuery = signal('');

  private subscription = new Subscription();

  constructor(
    private searchService: SynthesisSearchService,
    private snackBar: MatSnackBar,
    private router: Router
  ) {}

  ngOnInit(): void {
    const sub = this.queryControl.valueChanges.pipe(
      debounceTime(400),
      distinctUntilChanged(),
      filter(q => typeof q === 'string' && q.trim().length >= 2)
    ).pipe(
      switchMap(q => {
        this.loading.set(true);
        this.searched.set(false);
        this.currentQuery.set(q!.trim());
        return this.searchService.search(q!.trim());
      })
    ).subscribe({
      next: response => {
        this.results.set(response.results);
        this.loading.set(false);
        this.searched.set(true);
      },
      error: () => {
        this.loading.set(false);
        this.searched.set(true);
        this.snackBar.open('Erreur lors de la recherche, réessayez.', 'Fermer', {
          duration: 4000, panelClass: ['snack-error']
        });
      }
    });

    const clearSub = this.queryControl.valueChanges.pipe(
      filter(q => !q || q.trim().length < 2)
    ).subscribe(() => {
      this.results.set([]);
      this.searched.set(false);
      this.loading.set(false);
    });

    this.subscription.add(sub);
    this.subscription.add(clearSub);
  }

  ngOnDestroy(): void {
    this.subscription.unsubscribe();
  }

  goToCaseFile(id: string): void {
    this.router.navigate(['/case-files', id]);
  }

  domainLabel(domain: string): string {
    switch (domain) {
      case 'DROIT_DU_TRAVAIL': return 'Droit du travail';
      case 'DROIT_FAMILLE': return 'Droit de la famille';
      case 'DROIT_IMMIGRATION': return 'Droit de l\'immigration';
      default: return domain;
    }
  }

  analysisTypeLabel(type: string): string {
    return type === 'STANDARD' ? 'Standard' : type;
  }
}
