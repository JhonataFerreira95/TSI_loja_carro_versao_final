import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ReactiveFormsModule, FormBuilder, Validators, FormGroup } from '@angular/forms';
import { Api, Carro } from '../../core/api';

@Component({
  selector: 'app-test-drive',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './test-drive.html',
  styleUrl: './test-drive.scss',
})
export class TestDrive implements OnInit {
  carro?: Carro;
  form: FormGroup;
  horarios: string[] = [];
  horarioSelecionado = '';
  carregandoHorarios = false;
  loading = false;
  mensagem = '';
  erro = false;
  constructor(private route: ActivatedRoute, private fb: FormBuilder, private api: Api) {
    this.form = this.fb.group({
      data: [this.dataLocal(new Date()), Validators.required],
      observacao: ['', Validators.maxLength(500)]
    });
  }
  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.api.buscarCarro(id).subscribe({
      next: carro => {
        this.carro = carro;
        this.carregarDisponibilidade();
      },
      error: () => { this.erro = true; this.mensagem = 'Veículo não encontrado.'; }
    });
  }

  get dataMinima(): string {
    return this.dataLocal(new Date());
  }

  carregarDisponibilidade() {
    if (!this.carro) return;
    this.horarioSelecionado = '';
    this.horarios = [];
    const data = this.form.controls['data'].value;
    if (!data) return;

    this.carregandoHorarios = true;
    this.api.horariosDisponiveis(this.carro.id, data).subscribe({
      next: horarios => {
        this.horarios = horarios;
        this.carregandoHorarios = false;
      },
      error: resposta => {
        this.erro = true;
        this.mensagem = resposta.error?.erro || 'Não foi possível consultar os horários disponíveis.';
        this.carregandoHorarios = false;
      }
    });
  }

  agendar() {
    if (!this.carro || this.form.invalid || !this.horarioSelecionado) {
      this.form.markAllAsTouched();
      return;
    }
    this.loading = true;
    this.api.criarAgendamento({
      carroId: this.carro.id,
      dataHora: `${this.form.value.data}T${this.horarioSelecionado}`,
      observacao: this.form.value.observacao
    }).subscribe({
      next: () => {
        this.erro = false;
        this.mensagem = 'Test drive agendado com sucesso.';
        this.horarioSelecionado = '';
        this.loading = false;
        this.carregarDisponibilidade();
      },
      error: resposta => {
        this.erro = true;
        this.mensagem = resposta.error?.erro || 'Não foi possível agendar.';
        this.loading = false;
        this.carregarDisponibilidade();
      }
    });
  }

  private dataLocal(data: Date): string {
    const ano = data.getFullYear();
    const mes = String(data.getMonth() + 1).padStart(2, '0');
    const dia = String(data.getDate()).padStart(2, '0');
    return `${ano}-${mes}-${dia}`;
  }
}
