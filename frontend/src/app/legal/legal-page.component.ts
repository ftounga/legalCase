import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-legal-page',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './legal-page.component.html',
  styleUrl: './legal-page.component.scss'
})
export class LegalPageComponent {
  private route = inject(ActivatedRoute);

  get title(): string { return this.route.snapshot.data['title'] ?? ''; }
  get sections(): LegalSection[] { return this.route.snapshot.data['sections'] ?? []; }
}

export interface LegalSection {
  heading?: string;
  content: string;
}
