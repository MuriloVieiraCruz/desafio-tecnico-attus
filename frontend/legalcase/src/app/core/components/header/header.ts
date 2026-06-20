import { Component } from '@angular/core';

@Component({
  selector: 'app-header',
  template: `
    <header class="bg-brand-dark text-white">
      <div class="mx-auto flex max-w-7xl items-center gap-3 px-4 py-3">
        <!-- placeholder do logo: troque pelo logo da Attus quando tiver -->
        <div class="flex h-9 w-9 items-center justify-center rounded-lg
                    bg-linear-to-br from-accent to-primary text-sm font-bold">A</div>
        <div class="leading-tight">
          <div class="text-base font-semibold">Attus</div>
          <div class="text-xs text-white/60">Procuradoria Digital</div>
        </div>
      </div>
      <div class="h-0.5 bg-linear-to-r from-accent to-primary"></div>
    </header>
  `,
})
export class AppHeader {}