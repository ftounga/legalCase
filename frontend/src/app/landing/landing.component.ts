import { Component, AfterViewInit, ViewEncapsulation, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Title, Meta } from '@angular/platform-browser';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './landing.component.html',
  styleUrl: './landing.component.scss',
  encapsulation: ViewEncapsulation.None
})
export class LandingComponent implements OnInit, AfterViewInit {
  private title = inject(Title);
  private meta = inject(Meta);

  ngOnInit(): void {
    this.title.setTitle('AI LegalCase — L\'IA au service de vos dossiers juridiques');
    this.meta.updateTag({ name: 'description', content: 'Analysez automatiquement vos dossiers juridiques en quelques minutes. Faits clés, risques, timeline, points de droit. Essai gratuit 14 jours.' });
    this.meta.updateTag({ property: 'og:title', content: 'AI LegalCase — L\'IA au service de vos dossiers juridiques' });
    this.meta.updateTag({ property: 'og:description', content: 'Analysez automatiquement vos dossiers juridiques en quelques minutes. Faits clés, risques, timeline, points de droit. Essai gratuit 14 jours.' });
    this.meta.updateTag({ property: 'og:url', content: 'https://legalcase.ng-itconsulting.com/' });
  }

  ngAfterViewInit(): void {
    // Scroll animations
    const observer = new IntersectionObserver(entries => {
      entries.forEach(entry => {
        if (entry.isIntersecting) {
          entry.target.classList.add('visible');
        }
      });
    }, { threshold: 0.12, rootMargin: '0px 0px -40px 0px' });

    document.querySelectorAll('.fade-in').forEach(el => observer.observe(el));

    // Header scroll effect
    const header = document.querySelector('header');
    if (header) {
      window.addEventListener('scroll', () => {
        header.style.boxShadow = window.scrollY > 20
          ? '0 4px 24px rgba(26,58,92,0.1)'
          : 'none';
      });
    }
  }
}
