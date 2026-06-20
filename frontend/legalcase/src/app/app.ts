import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { Toast } from './core/components/toast/toast';
import { AppHeader } from './core/components/header/header';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, Toast, AppHeader],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('legalcase');
}
