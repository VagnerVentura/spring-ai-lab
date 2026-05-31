package com.portfolio.aiintro.controller;

/**
 * DTO para receber a mensagem do usuário via POST /chat
 */
public record ChatRequest(String message) {}
