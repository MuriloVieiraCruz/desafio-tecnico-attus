import { Component, computed, input, output } from '@angular/core';
import { StatusBadge } from '../status-badge/status-badge';
import { ALLOWED_TRANSITIONS, CASE_STATUS_LABELS, CaseStatus } from '../../models/legal-case';

@Component({
  selector: 'app-change-status-dialog',
  imports: [StatusBadge],
  host: { '(document:keydown.escape)': 'onEscape()' },
  template: `
    @if (open() && current()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" (click)="cancel.emit()">
        <div role="dialog" aria-modal="true" (click)="$event.stopPropagation()"
             class="w-full max-w-sm rounded-xl bg-surface p-6 shadow-xl">
          <h2 class="text-lg font-semibold text-ink">Alterar status</h2>
          <p class="mt-2 flex items-center gap-2 text-sm text-ink-muted">
            Atual: <app-status-badge [status]="current()!" />
          </p>

          @if (options().length === 0) {
            <p class="mt-4 text-sm text-ink-muted">Estado final — não há transições disponíveis.</p>
          } @else {
            <p class="mt-4 mb-2 text-sm text-ink-muted">Mover para:</p>
            <div class="flex flex-wrap gap-2">
              @for (s of options(); track s) {
                <button type="button" (click)="confirm.emit(s)"
                        class="rounded-lg border border-line px-3 py-2 text-sm text-ink hover:border-primary hover:bg-surface-2">
                  {{ labels[s] }}
                </button>
              }
            </div>
          }

          <div class="mt-5 flex justify-end">
            <button type="button" (click)="cancel.emit()"
                    class="rounded-lg border border-line px-4 py-2 text-sm text-ink-muted hover:bg-surface-2">Fechar</button>
          </div>
        </div>
      </div>
    }
  `,
})

export class ChangeStatusDialog {
  readonly open = input(false);
  readonly current = input<CaseStatus | null>(null);
  readonly options = computed(() => (this.current() ? ALLOWED_TRANSITIONS[this.current()!] : []));
  readonly labels = CASE_STATUS_LABELS;

  readonly confirm = output<CaseStatus>();
  readonly cancel = output<void>();

  onEscape(): void {
    if (this.open()) this.cancel.emit();
  }
}