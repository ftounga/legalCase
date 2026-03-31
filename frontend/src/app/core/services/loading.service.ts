import { Injectable, signal, computed } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class LoadingService {
  private pendingRequests = signal(0);
  readonly isLoading = computed(() => this.pendingRequests() > 0);

  increment(): void {
    this.pendingRequests.update(n => n + 1);
  }

  decrement(): void {
    this.pendingRequests.update(n => Math.max(0, n - 1));
  }
}
