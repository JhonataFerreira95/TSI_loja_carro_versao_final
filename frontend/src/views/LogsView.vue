<script setup>
import { onMounted, ref } from 'vue'
import { api, lerErro } from '../api'

const logs = ref([])
const mensagem = ref('')

async function listar() {
  const resposta = await api('/log')
  if (resposta.ok) logs.value = await resposta.json()
  else mensagem.value = await lerErro(resposta)
}

function formatarData(data) {
  return data ? new Date(data).toLocaleString('pt-BR') : ''
}

onMounted(listar)
</script>

<template>
  <section class="card">
    <h1>Logs do Sistema</h1>
    <button type="button" @click="listar">Atualizar</button>
    <p v-if="mensagem" class="mensagem erro">{{ mensagem }}</p>
    <div class="tabela-wrapper">
      <table>
        <thead><tr><th>ID</th><th>Data/Hora</th><th>Usuário</th><th>Ação</th><th>Entidade</th><th>Descrição</th></tr></thead>
        <tbody>
          <tr v-for="log in logs" :key="log.id">
            <td>{{ log.id }}</td><td>{{ formatarData(log.dataHora) }}</td><td>{{ log.usuario }}</td><td>{{ log.acao }}</td><td>{{ log.entidade }}</td><td>{{ log.descricao }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>
