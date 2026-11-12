import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { afterEach, expect, it, vi } from 'vitest';
import { TrajetSearchComponent } from './trajet-search.component';
import { TrajetCardComponent } from './trajet-card.component';
import { TrajetsApiService } from '../../core/api/trajets-api.service';
import { TrajetResponse } from '../models/trajet.model';

afterEach(() => TestBed.resetTestingModule());

it('envoie les codes des villes et la date du formulaire de recherche', async () => {
  await TestBed.configureTestingModule({imports:[TrajetSearchComponent],providers:[provideNoopAnimations(),
    {provide:TrajetsApiService,useValue:{getVilles:()=>of([{code:'MONS',libelle:'Mons'},{code:'BRUXELLES',libelle:'Bruxelles'}])}}]}).compileComponents();
  const fixture = TestBed.createComponent(TrajetSearchComponent);
  const resultat = vi.fn();
  fixture.componentInstance.recherche.subscribe(resultat);
  fixture.componentInstance.form.patchValue({depart:'MONS',arrivee:'BRUXELLES',date:'2027-01-12'});
  fixture.detectChanges();
  fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit', {bubbles:true,cancelable:true}));
  expect(resultat).toHaveBeenCalledWith({depart:'MONS',arrivee:'BRUXELLES',date:'2027-01-12'});
});

const trajet: TrajetResponse = {id:1,vehiculeId:1,vehiculeNom:'Voiture test',agenceNom:'Agence test',organisateurId:2,
  organisateurNom:'Alice Test',chauffeurNom:'Alice Test',villeDepart:'MONS',villeArrivee:'BRUXELLES',date:'2027-01-12',heureDebut:'10:00',heureFin:'11:30',
  typeTrajet:'PUBLIC',statut:'PLANIFIE',nbPassagersActuels:0,placesRestantes:4,placesMaximum:4,prixParPassager:25,dateConversionPublic:null};

it('affiche les quatre places clients sans compter le chauffeur', async () => {
  await TestBed.configureTestingModule({imports:[TrajetCardComponent],providers:[provideNoopAnimations()]}).compileComponents();
  const fixture = TestBed.createComponent(TrajetCardComponent);
  fixture.componentRef.setInput('trajet',trajet); fixture.detectChanges();
  expect(fixture.nativeElement.textContent).toContain('4 place(s) restante(s) sur 4');
  expect(fixture.nativeElement.textContent).toContain('Alice Test');
  expect(fixture.nativeElement.querySelector('button').disabled).toBe(false);
});

it('désactive la réservation et indique complet à zéro place restante', async () => {
  await TestBed.configureTestingModule({imports:[TrajetCardComponent],providers:[provideNoopAnimations()]}).compileComponents();
  const fixture = TestBed.createComponent(TrajetCardComponent);
  fixture.componentRef.setInput('trajet',{...trajet,statut:'COMPLET',placesRestantes:0,nbPassagersActuels:4});fixture.detectChanges();
  expect(fixture.nativeElement.querySelector('button').disabled).toBe(true);
  expect(fixture.nativeElement.querySelector('button').textContent).toContain('Complet');
});
