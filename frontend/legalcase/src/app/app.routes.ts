import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', redirectTo: 'legal-cases', pathMatch: 'full' },
  {
    path: 'legal-cases',
    loadComponent: () =>
      import('./features/legal-cases/list/legal-case-list').then(m => m.LegalCaseList),
  },
  {
  path: 'legal-cases/new',
  loadComponent: () =>
    import('./features/legal-cases/form/legal-case-form').then(m => m.LegalCaseForm),
  },
  {
    path: 'legal-cases/:id/edit',
    loadComponent: () =>
      import('./features/legal-cases/form/legal-case-form').then(m => m.LegalCaseForm),
  },
  {
    path: '**', redirectTo: 'legal-cases'
  },
];
