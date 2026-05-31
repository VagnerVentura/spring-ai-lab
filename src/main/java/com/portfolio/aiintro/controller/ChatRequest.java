package com.portfolio.aiintro.controller;

/**
 * ╔══════════════════════════════════════════════════════╗
 * ║  NOVIDADE neste branch: conversationId               ║
 * ╠══════════════════════════════════════════════════════╣
 * ║  O conversationId é a chave que identifica uma       ║
 * ║  sessão de conversa específica.                      ║
 * ║                                                      ║
 * ║  Cada ID diferente = contexto separado na memória.   ║
 * ║  Mesmo ID = modelo lembra tudo que foi dito antes.   ║
 * ║                                                      ║
 * ║  Experimente:                                        ║
 * ║  1. Mande "meu nome é Vagner" com id "sessao-1"     ║
 * ║  2. Mande "qual é o meu nome?" com id "sessao-1"    ║
 * ║  3. Mande "qual é o meu nome?" com id "sessao-2"    ║
 * ║     → resposta 3 não vai saber o nome!               ║
 * ╚══════════════════════════════════════════════════════╝
 *
 * @param message        mensagem do usuário
 * @param conversationId identificador único da sessão (ex: UUID, userId, "sessao-1")
 */
public record ChatRequest(String message, String conversationId) {}