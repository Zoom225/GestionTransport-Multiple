import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { LanguageService } from './core/i18n/language.service';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/pages/landing.page').then((m) => m.LandingPage),
    title: () => `GestionTransport - ${inject(LanguageService).t('nav.home')}`
  },
  {
    path: 'voyageur',
    loadChildren: () => import('./features/voyageur/voyageur.routes').then((m) => m.VOYAGEUR_ROUTES)
  },
  {
    path: 'admin',
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
