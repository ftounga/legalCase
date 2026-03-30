import { Component, inject, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { RouterModule } from '@angular/router';
import { Title } from '@angular/platform-browser';

@Component({
  selector: 'app-legal-page',
  standalone: true,
  imports: [RouterModule],
  templateUrl: './legal-page.component.html',
  styleUrl: './legal-page.component.scss'
})
export class LegalPageComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private titleService = inject(Title);

  ngOnInit(): void {
    const pageTitle = this.route.snapshot.data['title'];
    if (pageTitle) {
      this.titleService.setTitle(`${pageTitle} — AI LegalCase`);
    }
  }

  get title(): string { return this.route.snapshot.data['title'] ?? ''; }
  get sections(): LegalSection[] { return this.route.snapshot.data['sections'] ?? []; }
}

export interface LegalSection {
  heading?: string;
  content: string;
}
