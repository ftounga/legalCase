import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DecisionalHeaderFlagComponent } from './decisional-header-flag.component';

describe('DecisionalHeaderFlagComponent', () => {
  let component: DecisionalHeaderFlagComponent;
  let fixture: ComponentFixture<DecisionalHeaderFlagComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DecisionalHeaderFlagComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(DecisionalHeaderFlagComponent);
    component = fixture.componentInstance;
  });

  it('renders the label', () => {
    component.label = 'CRA';
    fixture.detectChanges();
    const host: HTMLElement = fixture.nativeElement;
    expect(host.textContent).toContain('CRA');
  });

  it('defaults to warning variant when none specified', () => {
    component.label = 'CRA';
    fixture.detectChanges();
    const el = (fixture.nativeElement as HTMLElement).querySelector('.dhf');
    expect(el?.classList).toContain('dhf--warning');
  });

  it('applies the danger variant', () => {
    component.label = 'TRANSFERT IMMINENT';
    component.variant = 'danger';
    fixture.detectChanges();
    const el = (fixture.nativeElement as HTMLElement).querySelector('.dhf');
    expect(el?.classList).toContain('dhf--danger');
    expect(el?.classList).not.toContain('dhf--warning');
  });

  it('applies the info variant', () => {
    component.label = 'INFO';
    component.variant = 'info';
    fixture.detectChanges();
    const el = (fixture.nativeElement as HTMLElement).querySelector('.dhf');
    expect(el?.classList).toContain('dhf--info');
  });

  it('renders the optional icon when provided', () => {
    component.label = 'TRANSFERT IMMINENT';
    component.icon = 'local_shipping';
    fixture.detectChanges();
    const icon = (fixture.nativeElement as HTMLElement).querySelector('mat-icon');
    expect(icon).toBeTruthy();
    expect(icon?.textContent?.trim()).toBe('local_shipping');
  });

  it('omits the icon element when icon is null', () => {
    component.label = 'CRA';
    component.icon = null;
    fixture.detectChanges();
    const icon = (fixture.nativeElement as HTMLElement).querySelector('mat-icon');
    expect(icon).toBeNull();
  });

  it('sets aria-label for screen readers', () => {
    component.label = 'CRA';
    fixture.detectChanges();
    const el = (fixture.nativeElement as HTMLElement).querySelector('.dhf');
    expect(el?.getAttribute('aria-label')).toBe('CRA');
    expect(el?.getAttribute('role')).toBe('status');
  });
});
