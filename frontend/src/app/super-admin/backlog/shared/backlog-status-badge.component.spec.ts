import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { BacklogStatusBadgeComponent } from './backlog-status-badge.component';

describe('BacklogStatusBadgeComponent', () => {
  let fixture: ComponentFixture<BacklogStatusBadgeComponent>;
  let component: BacklogStatusBadgeComponent;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [BacklogStatusBadgeComponent] });
    fixture = TestBed.createComponent(BacklogStatusBadgeComponent);
    component = fixture.componentInstance;
  });

  function render(status: string, kind: 'feature' | 'marketing' = 'feature') {
    component.status = status;
    component.kind = kind;
    fixture.detectChanges();
    return fixture.debugElement.query(By.css('span.badge'));
  }

  it('renders feature DONE with done tone and "Terminée" label', () => {
    const span = render('DONE', 'feature');
    expect(span.nativeElement.classList).toContain('badge--done');
    expect(span.nativeElement.textContent.trim()).toBe('Terminée');
  });

  it('renders marketing TERMINE with done tone and "Terminé" label', () => {
    const span = render('TERMINE', 'marketing');
    expect(span.nativeElement.classList).toContain('badge--done');
    expect(span.nativeElement.textContent.trim()).toBe('Terminé');
  });

  it('renders BLOCKED feature with blocked tone and "Bloqué" label', () => {
    const span = render('BLOCKED', 'feature');
    expect(span.nativeElement.classList).toContain('badge--blocked');
    expect(span.nativeElement.textContent.trim()).toBe('Bloqué');
  });

  it('renders BLOQUE marketing with blocked tone and "Bloqué" label', () => {
    const span = render('BLOQUE', 'marketing');
    expect(span.nativeElement.classList).toContain('badge--blocked');
    expect(span.nativeElement.textContent.trim()).toBe('Bloqué');
  });

  it('falls back to unknown tone for unrecognised status', () => {
    const span = render('SOMETHING_NEW', 'feature');
    expect(span.nativeElement.classList).toContain('badge--unknown');
    expect(span.nativeElement.textContent.trim()).toBe('SOMETHING_NEW');
  });
});
