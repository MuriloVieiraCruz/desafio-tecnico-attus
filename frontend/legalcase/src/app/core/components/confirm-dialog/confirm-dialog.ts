import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-confirm-dialog',
  host: { '(document:keydown.escape)': 'onEscape()' },
  template: `
    @if (open()) {
      <div class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4" (click)="cancel.emit()">
        <div role="dialog" aria-modal="true" (click)="$event.stopPropagation()"
             class="w-full max-w-sm rounded-xl bg-surface p-6 shadow-xl">
          <h2 class="text-lg font-semibold text-ink">{{ title() }}</h2>
          <p class="mt-2 text-sm text-ink-muted">{{ message() }}</p>
          <div class="mt-5 flex justify-end gap-2">
            <button type="button" (click)="cancel.emit()"
                    class="rounded-lg border border-line px-4 py-2 text-sm text-ink-muted hover:bg-surface-2">
              {{ cancelLabel() }}
            </button>
            <button type="button" (click)="confirm.emit()"
                    class="rounded-lg px-4 py-2 text-sm font-medium text-white"
                    [class]="danger() ? 'bg-red-600 hover:bg-red-700' : 'bg-primary hover:bg-primary-strong'">
              {{ confirmLabel() }}
            </button>
          </div>
        </div>
      </div>
    }
  `,
})
export class ConfirmDialog {
  readonly open = input(false);
  readonly title = input('Confirmar');
  readonly message = input('');
  readonly confirmLabel = input('Confirmar');
  readonly cancelLabel = input('Cancelar');
  readonly danger = input(false);

  readonly confirm = output<void>();
  readonly cancel = output<void>();

  onEscape(): void {
    if (this.open()) this.cancel.emit();
  }
}