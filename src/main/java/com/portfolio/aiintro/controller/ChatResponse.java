package com.portfolio.aiintro.controller;

/**
 * DTO para devolver a resposta do modelo ao cliente
 */
public record ChatResponse(String response, String model, long tokensUsed) {}
