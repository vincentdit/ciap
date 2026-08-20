import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ApiResponse, ProjectResponse, CreateProjectRequest, CreateTaskRequest,
  CreateMilestoneRequest, TaskResponse, MilestoneResponse, TaskStatus
} from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class ProjectApi {
  private readonly base = `${environment.apiBaseUrl}/projects`;

  constructor(private http: HttpClient) {}

  createForRequest(requestId: number, body: CreateProjectRequest): Observable<ApiResponse<ProjectResponse>> {
    return this.http.post<ApiResponse<ProjectResponse>>(`${this.base}/from-request/${requestId}`, body);
  }

  getForRequest(requestId: number): Observable<ApiResponse<ProjectResponse>> {
    return this.http.get<ApiResponse<ProjectResponse>>(`${this.base}/request/${requestId}`);
  }

  get(id: number): Observable<ApiResponse<ProjectResponse>> {
    return this.http.get<ApiResponse<ProjectResponse>>(`${this.base}/${id}`);
  }

  start(id: number)    { return this.http.post<ApiResponse<ProjectResponse>>(`${this.base}/${id}/start`, {}); }
  hold(id: number)     { return this.http.post<ApiResponse<ProjectResponse>>(`${this.base}/${id}/hold`, {}); }
  complete(id: number) { return this.http.post<ApiResponse<ProjectResponse>>(`${this.base}/${id}/complete`, {}); }
  cancel(id: number)   { return this.http.post<ApiResponse<ProjectResponse>>(`${this.base}/${id}/cancel`, {}); }

  addTask(id: number, body: CreateTaskRequest): Observable<ApiResponse<TaskResponse>> {
    return this.http.post<ApiResponse<TaskResponse>>(`${this.base}/${id}/tasks`, body);
  }

  updateTaskStatus(id: number, taskId: number, status: TaskStatus): Observable<ApiResponse<ProjectResponse>> {
    return this.http.patch<ApiResponse<ProjectResponse>>(`${this.base}/${id}/tasks/${taskId}/status`, { status });
  }

  addMilestone(id: number, body: CreateMilestoneRequest): Observable<ApiResponse<MilestoneResponse>> {
    return this.http.post<ApiResponse<MilestoneResponse>>(`${this.base}/${id}/milestones`, body);
  }

  achieveMilestone(id: number, milestoneId: number): Observable<ApiResponse<MilestoneResponse>> {
    return this.http.post<ApiResponse<MilestoneResponse>>(`${this.base}/${id}/milestones/${milestoneId}/achieve`, {});
  }
}
