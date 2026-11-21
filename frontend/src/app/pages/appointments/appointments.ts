import { Component, OnInit } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Api, Agendamento } from '../../core/api';

@Component({
  selector: 'app-appointments',
  imports: [DatePipe],
  templateUrl: './appointments.html',
  styleUrl: './appointments.scss',
})
export class Appointments implements OnInit {
  agendamentos: Agendamento[] = [];
  loading = true;
  erro = '';
  constructor(private api: Api) {}
  ngOnInit() {
    this.api.meusAgendamentos().subscribe({
      next: itens => { this.agendamentos = itens; this.loading = false; },
      error: () => { this.erro = 'Não foi possível consultar seus agendamentos.'; this.loading = false; }
    });
  }
}
