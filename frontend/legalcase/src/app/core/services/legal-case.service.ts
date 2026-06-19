import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CreateLegalCaseRequest,
  LegalCase,
  LegalCaseFilter,
  UpdateLegalCaseRequest,
  UpdateStatusRequest,
} from '../models/legal-case';
import { PagedModel } from '../models/page';

@Injectable({ providedIn: 'root' })
export class LegalCaseService {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/legal-cases`;

  create(request: CreateLegalCaseRequest): Observable<LegalCase> {
    return this.http.post<LegalCase>(this.baseUrl, request);
  }

  findById(id: number): Observable<LegalCase> {
    return this.http.get<LegalCase>(`${this.baseUrl}/${id}`);
  }

  search(
    filter: LegalCaseFilter,
    page: number,
    size: number,
    sort?: string,
  ): Observable<PagedModel<LegalCase>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (sort) params = params.set('sort', sort);
    if (filter.status) params = params.set('status', filter.status);
    if (filter.party) params = params.set('party', filter.party);
    if (filter.court) params = params.set('court', filter.court);
    if (filter.filingDateFrom) params = params.set('filingDateFrom', filter.filingDateFrom);
    if (filter.filingDateTo) params = params.set('filingDateTo', filter.filingDateTo);
    return this.http.get<PagedModel<LegalCase>>(this.baseUrl, { params });
  }

  update(id: number, request: UpdateLegalCaseRequest): Observable<LegalCase> {
    return this.http.put<LegalCase>(`${this.baseUrl}/${id}`, request);
  }

  changeStatus(id: number, request: UpdateStatusRequest): Observable<LegalCase> {
    return this.http.patch<LegalCase>(`${this.baseUrl}/${id}/status`, request);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }
}