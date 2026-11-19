package com.subtrack.controller;

import com.subtrack.dto.response.CurrencyConversionResponse;
import com.subtrack.dto.response.ExchangeRatesResponse;
import com.subtrack.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/currencies")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CurrencyController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping("/rates")
    public ResponseEntity<ExchangeRatesResponse> getRates(
            @RequestParam(defaultValue = "EUR") String base) {
        return ResponseEntity.ok(exchangeRateService.getRates(base));
    }

    @GetMapping("/convert")
    public ResponseEntity<CurrencyConversionResponse> convert(
            @RequestParam BigDecimal amount,
            @RequestParam String from,
            @RequestParam String to) {
        return ResponseEntity.ok(exchangeRateService.convert(amount, from, to));
    }
}
