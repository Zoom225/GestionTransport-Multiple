import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import {
  VehiculeRequest,
  VehiculeResponse
} from '../../shared/models/agence-vehicule.model';
import { apiUrl } from './api-url';

@Injectable({ providedIn: 'root' })
export class VehiculesApiService {
  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<VehiculeResponse[]> {
    return this.http.get<VehiculeResponse[]>(apiUrl('/vehicules'));
  }

  getById(id: number): Observable<VehiculeResponse> {
    return this.http.get<VehiculeResponse>(apiUrl(`/vehicules/${id}`));
  }

  getByAgence(agenceId: number): Observable<VehiculeResponse[]> {
    return this.http.get<VehiculeResponse[]>(apiUrl(`/vehicules/agence/${agenceId}`));
  }

  create(payload: VehiculeRequest): Observable<VehiculeResponse> {
    return this.http.post<VehiculeResponse>(apiUrl('/vehicules'), payload);
  }

  update(id: number, payload: VehiculeRequest): Observable<VehiculeResponse> {
    return this.http.put<VehiculeResponse>(apiUrl(`/vehicules/${id}`), payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(apiUrl(`/vehicules/${id}`));
  }
}

