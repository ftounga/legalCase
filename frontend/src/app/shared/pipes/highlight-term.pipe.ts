import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

@Pipe({ name: 'highlightTerm', standalone: true })
export class HighlightTermPipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer) {}

  transform(text: string, term: string): SafeHtml {
    if (!term || term.length < 2) return text;
    const escaped = term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    const highlighted = text.replace(
      new RegExp(escaped, 'gi'),
      match => `<strong>${match}</strong>`
    );
    return this.sanitizer.bypassSecurityTrustHtml(highlighted);
  }
}
