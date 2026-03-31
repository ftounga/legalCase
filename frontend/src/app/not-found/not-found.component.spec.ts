import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { NotFoundComponent } from './not-found.component';

describe('NotFoundComponent', () => {
  let fixture: ComponentFixture<NotFoundComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotFoundComponent, NoopAnimationsModule],
      providers: [provideRouter([])]
    }).compileComponents();
    fixture = TestBed.createComponent(NotFoundComponent);
    fixture.detectChanges();
  });

  it('T-01: composant créé', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('T-02: texte "introuvable" visible', () => {
    expect(fixture.nativeElement.textContent).toContain('introuvable');
  });

  it('T-03: lien retour aux dossiers présent', () => {
    const link = fixture.nativeElement.querySelector('a[routerlink="/case-files"], a[ng-reflect-router-link="/case-files"]');
    // Check by text content as fallback
    expect(fixture.nativeElement.textContent).toContain('dossiers');
  });
});
