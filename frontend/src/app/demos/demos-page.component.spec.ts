import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { DemosPageComponent } from './demos-page.component';
import { DEMO_VIDEOS } from '../shared/demos/demo-videos.data';

describe('DemosPageComponent', () => {
  let component: DemosPageComponent;
  let fixture: ComponentFixture<DemosPageComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DemosPageComponent],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(DemosPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('rend la grille avec autant de cards que DEMO_VIDEOS', () => {
    const cards = fixture.nativeElement.querySelectorAll('li.demos-card');
    expect(cards.length).toBe(DEMO_VIDEOS.length);
  });

  it('aucune vidéo sélectionnée au rendu initial', () => {
    expect(component.selectedVideoId()).toBeNull();
    const players = fixture.nativeElement.querySelectorAll('.demos-card-player');
    expect(players.length).toBe(0);
  });

  it('expose le compteur de vidéos', () => {
    expect(component.count()).toBe(DEMO_VIDEOS.length);
    const subtitle = fixture.nativeElement.querySelector('.demos-subtitle');
    expect(subtitle.textContent).toContain(String(DEMO_VIDEOS.length));
  });

  it('clic sur une card sélectionne la vidéo et affiche le player', () => {
    const videoId = DEMO_VIDEOS[0].videoId;
    component.selectVideo(videoId);
    fixture.detectChanges();

    expect(component.selectedVideoId()).toBe(videoId);
    expect(component.isSelected(videoId)).toBe(true);

    const players = fixture.nativeElement.querySelectorAll('.demos-card-player');
    expect(players.length).toBe(1);
    const iframe = players[0].querySelector('iframe');
    expect(iframe).toBeTruthy();
    expect(iframe.getAttribute('title')).toBe(DEMO_VIDEOS[0].title);
  });

  it('clic sur une autre card change la vidéo sélectionnée (un seul player actif)', () => {
    component.selectVideo(DEMO_VIDEOS[0].videoId);
    fixture.detectChanges();
    component.selectVideo(DEMO_VIDEOS[1].videoId);
    fixture.detectChanges();

    expect(component.selectedVideoId()).toBe(DEMO_VIDEOS[1].videoId);
    const players = fixture.nativeElement.querySelectorAll('.demos-card-player');
    expect(players.length).toBe(1);
  });

  it('closeVideo() réinitialise la sélection', () => {
    component.selectVideo(DEMO_VIDEOS[0].videoId);
    expect(component.selectedVideoId()).not.toBeNull();
    component.closeVideo();
    expect(component.selectedVideoId()).toBeNull();
  });

  it('navigation clavier : Enter sélectionne, Escape ferme', () => {
    const videoId = DEMO_VIDEOS[2].videoId;
    const enterEvt = new KeyboardEvent('keydown', { key: 'Enter' });
    component.onCardKeydown(enterEvt, videoId);
    expect(component.selectedVideoId()).toBe(videoId);

    const escEvt = new KeyboardEvent('keydown', { key: 'Escape' });
    component.onCardKeydown(escEvt, videoId);
    expect(component.selectedVideoId()).toBeNull();
  });

  it('thumbnailUrl() renvoie l\'URL YouTube hqdefault', () => {
    const url = component.thumbnailUrl('ABC123');
    expect(url).toBe('https://img.youtube.com/vi/ABC123/hqdefault.jpg');
  });

  it('chaque card non-active a des labels ARIA pour l\'accessibilité', () => {
    const buttons = fixture.nativeElement.querySelectorAll('button.demos-card-thumb');
    expect(buttons.length).toBe(DEMO_VIDEOS.length);
    buttons.forEach((btn: HTMLButtonElement, i: number) => {
      expect(btn.getAttribute('aria-label')).toBe('Lire : ' + DEMO_VIDEOS[i].title);
    });
  });

  it('expose les CTAs Essai gratuit (/login) et Calendly (target=_blank)', () => {
    const cta = fixture.nativeElement.querySelector('.demos-cta');
    expect(cta).toBeTruthy();
    const primary = cta.querySelector('.demos-cta-btn--primary');
    expect(primary.getAttribute('ng-reflect-router-link')).toBe('/login');

    const secondary = cta.querySelector('.demos-cta-btn--secondary');
    expect(secondary.getAttribute('href')).toContain('calendly.com');
    expect(secondary.getAttribute('target')).toBe('_blank');
    expect(secondary.getAttribute('rel')).toContain('noopener');
  });

  it('injecte un bloc JSON-LD WebPage dans le head', () => {
    const scripts = document.head.querySelectorAll('script[type="application/ld+json"]');
    const matching = Array.from(scripts).find(s => {
      const json = JSON.parse(s.textContent || '{}');
      return json['@type'] === 'WebPage' && json.url === 'https://legalcase.fr/demos';
    });
    expect(matching).toBeDefined();
  });

  it('définit la balise canonical vers https://legalcase.fr/demos', () => {
    const canonical = document.head.querySelector('link[rel="canonical"]') as HTMLLinkElement;
    expect(canonical).toBeTruthy();
    expect(canonical.href).toBe('https://legalcase.fr/demos');
  });

  it('met à jour le title de la page', () => {
    expect(document.title).toContain('Démonstrations');
  });

  it('nettoie le JSON-LD au ngOnDestroy', () => {
    fixture.destroy();
    const remaining = Array.from(
      document.head.querySelectorAll('script[type="application/ld+json"]'),
    ).filter(s => {
      const json = JSON.parse(s.textContent || '{}');
      return json['@type'] === 'WebPage' && json.url === 'https://legalcase.fr/demos';
    });
    expect(remaining.length).toBe(0);
  });
});
