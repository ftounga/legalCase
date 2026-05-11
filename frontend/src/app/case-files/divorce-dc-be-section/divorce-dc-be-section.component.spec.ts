import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { DivorceDcBeSectionComponent } from './divorce-dc-be-section.component';

describe('DivorceDcBeSectionComponent', () => {
  let component: DivorceDcBeSectionComponent;
  let fixture: ComponentFixture<DivorceDcBeSectionComponent>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DivorceDcBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
    }).compileComponents();
    fixture = TestBed.createComponent(DivorceDcBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    httpMock = TestBed.inject(HttpTestingController);
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  it('rendu nominal : cadre juridique CJ 1287 et notes visibles', () => {
    const html = fixture.nativeElement as HTMLElement;
    expect(html.textContent).toContain('1287');
    expect(html.textContent).toContain('Backend opérationnel');
  });

  it('toggle collapsed cache puis réaffiche le contenu', () => {
    expect(component.collapsed()).toBe(false);
    component.toggleCollapse();
    fixture.detectChanges();
    expect(component.collapsed()).toBe(true);
    expect((fixture.nativeElement as HTMLElement).querySelector('#dcbe-body')).toBeNull();
    component.toggleCollapse();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('#dcbe-body')).not.toBeNull();
  });

  it('forceExpanded=true au mount → collapsed reste false', () => {
    const f = TestBed.createComponent(DivorceDcBeSectionComponent);
    f.componentInstance.caseFileId = 'case-1';
    f.componentInstance.collapsed.set(true);
    f.componentRef.setInput('forceExpanded', true);
    f.componentInstance.ngOnInit();
    expect(f.componentInstance.collapsed()).toBe(false);
  });

  it('expose statics TOOL_LABEL, TOOL_ICON, PREFILL_COUNT_ALWAYS_ZERO, getPrefillCount', () => {
    expect(DivorceDcBeSectionComponent.TOOL_LABEL).toBe('DIVORCE CONSENTEMENT MUTUEL (BE)');
    expect(DivorceDcBeSectionComponent.TOOL_ICON).toBe('handshake');
    expect(DivorceDcBeSectionComponent.PREFILL_COUNT_ALWAYS_ZERO).toBe(true);
    expect(DivorceDcBeSectionComponent.getPrefillCount()).toBe(0);
  });
});
