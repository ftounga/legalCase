import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import {
  MarkdownAction,
  MarkdownToolbarComponent,
} from './markdown-toolbar.component';

describe('MarkdownToolbarComponent', () => {
  let component: MarkdownToolbarComponent;
  let fixture: ComponentFixture<MarkdownToolbarComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MarkdownToolbarComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(MarkdownToolbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  function click(testid: string): void {
    const btn: HTMLButtonElement = fixture.nativeElement.querySelector(
      `[data-testid="${testid}"]`,
    );
    btn.click();
  }

  const CASES: ReadonlyArray<{ testid: string; action: MarkdownAction }> = [
    { testid: 'md-h2', action: 'h2' },
    { testid: 'md-h3', action: 'h3' },
    { testid: 'md-bold', action: 'bold' },
    { testid: 'md-italic', action: 'italic' },
    { testid: 'md-list', action: 'list' },
    { testid: 'md-quote', action: 'quote' },
  ];

  it('chaque bouton émet l\'action attendue', () => {
    const emitted: MarkdownAction[] = [];
    component.action.subscribe((a) => emitted.push(a));

    CASES.forEach(({ testid }) => click(testid));

    expect(emitted).toEqual(CASES.map((c) => c.action));
  });

  it('les 6 boutons de mise en forme sont rendus', () => {
    CASES.forEach(({ testid }) => {
      expect(
        fixture.nativeElement.querySelector(`[data-testid="${testid}"]`),
      ).not.toBeNull();
    });
  });

  it('disabled → aucun événement émis au clic', () => {
    component.disabled = true;
    fixture.detectChanges();
    const spy = jasmine.createSpy('action');
    component.action.subscribe(spy);

    click('md-bold');

    expect(spy).not.toHaveBeenCalled();
  });
});
