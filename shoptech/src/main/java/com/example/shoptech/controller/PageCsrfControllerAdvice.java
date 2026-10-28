package com.example.shoptech.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Ensure the CSRF cookie exists before any server-rendered form or notification action is used. */
@ControllerAdvice
public class PageCsrfControllerAdvice {
    @ModelAttribute
    public void initializeCsrfToken(CsrfToken csrfToken) { csrfToken.getToken(); }
}
