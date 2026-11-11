import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { IndisponibiliteServiceRequest, IndisponibiliteServiceResponse } from '../../shared/models/agence-vehicule.model';
import { apiUrl } from './api-url';

@Injectable({ providedIn: 'root' })
export class IndisponibilitesServiceApiService {
  constructor(private readonly http: HttpClient) {}

  getAll(): Observable<IndisponibiliteServiceResponse[]> {
    return this.http.get<IndisponibiliteServiceResponse[]>(apiUrl('/indisponibilites-service'));
  }

  getGlobal(): Observable<IndisponibiliteServiceResponse[]> {
    return this.http.get<IndisponibiliteServiceResponse[]>(apiUrl('/indisponibilites-service/global'));
  }

  getByAgence(agenceId: number): Observable<IndisponibiliteServiceResponse[]> {
    return this.http.get<IndisponibiliteServiceResponse[]>(apiUrl(`/indisponibilites-service/agence/${agenceId}`));
  }

  create(payload: IndisponibiliteServiceRequest): Observable<IndisponibiliteServiceResponse> {
    return this.http.post<IndisponibiliteServiceResponse>(apiUrl('/indisponibilites-service'), payload);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(apiUrl(`/indisponibilites-service/${id}`));
  }
}

