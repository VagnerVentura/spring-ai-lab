package com.portfolio.aiintro.controller;


/**
 * Adicionado conversationId na resposta para o cliente
 * saber qual ID usar na próxima mensagem da mesma sessão.
 */
public record ChatResponse(
        String response,
        String model,
        long tokensUsed,
        String conversationId   // ← NOVO: devolvemos o ID para o cliente rastrear
) {}