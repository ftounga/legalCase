import { Component, OnInit, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DatePipe } from '@angular/common';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { CaseFileShareService } from '../core/services/case-file-share.service';
import { PublicShareResponse } from '../core/models/share.model';

@Component({
  selector: 'app-public-share',
  standalone: true,
  imports: [DatePipe, MatProgressSpinnerModule, MatIconModule],
  templateUrl: './public-share.component.html',
  styleUrl: './public-share.component.scss'
})
export class PublicShareComponent implements OnInit {
  loading = signal(true);
  error = signal<'not_found' | 'expired' | 'generic' | null>(null);
  share = signal<PublicShareResponse | null>(null);

  constructor(
    private route: ActivatedRoute,
    private shareService: CaseFileShareService
  ) {}

  ngOnInit(): void {
    const token = this.route.snapshot.paramMap.get('token')!;
    this.shareService.getPublicShare(token).subscribe({
      next: data => {
        this.share.set(data);
        this.loading.set(false);
      },
      error: (err: any) => {
        if (err.status === 404) {
          this.error.set('not_found');
        } else if (err.status === 410) {
          this.error.set('expired');
        } else {
          this.error.set('generic');
        }
        this.loading.set(false);
      }
    });
  }
}
