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
  mensagem = '';
  cancelando: number | null = null;
  constructor(private api: Api) {}
  ngOnInit() {
    this.listar();
  }
  listar() {
    this.loading = true;
    this.api.meusAgendamentos().subscribe({
      next: itens => { this.agendamentos = itens; this.loading = false; },
      error: () => { this.erro = 'Não foi possível consultar seus agendamentos.'; this.loading = false; }
    });
  }

  podeCancelar(item: Agendamento): boolean {
    return (item.status === 'CONFIRMADO' || item.status === 'PENDENTE')
      && new Date(item.dataHora).getTime() > Date.now();
  }

  cancelar(item: Agendamento) {
    this.cancelando = item.id;
    this.api.cancelarAgendamento(item.id).subscribe({
      next: () => {
        this.erro = '';
        this.mensagem = 'Agendamento cancelado. O horário está disponível novamente.';
        this.cancelando = null;
        this.listar();
      },
      error: resposta => {
        this.erro = resposta.error?.erro || 'Não foi possível cancelar o agendamento.';
        this.cancelando = null;
      }
    });
  }
}
