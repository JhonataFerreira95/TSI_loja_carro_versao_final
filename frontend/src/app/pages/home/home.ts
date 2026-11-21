import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { Api, Carro } from '../../core/api';

@Component({
  selector: 'app-home',
  imports: [RouterLink, CurrencyPipe],
  templateUrl: './home.html',
  styleUrl: './home.scss',
})
export class Home implements OnInit {
  carros: Carro[] = [];
  loading = true;
  constructor(private api: Api) {}
  ngOnInit() {
    this.api.listarCarros().subscribe({
      next: carros => { this.carros = carros.slice(0, 3); this.loading = false; },
      error: () => this.loading = false
    });
  }
}
