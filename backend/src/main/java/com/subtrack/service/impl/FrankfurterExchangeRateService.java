package com.subtrack.service.impl;

import com.subtrack.dto.response.CurrencyConversionResponse;
import com.subtrack.dto.response.ExchangeRatesResponse;
import com.subtrack.service.ExchangeRateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FrankfurterExchangeRateService implements ExchangeRateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrankfurterExchangeRateService.class);
    private static final String FRANKFURTER_URL = "https://api.frankfurter.dev/v1/latest";
    private static final String CACHE_KEY_PREFIX = "exchange-rates:";
    private static final Pattern CURRENCY_CODE = Pattern.compile("[A-Z]{3}");

    private final RestClient.Builder restClientBuilder;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.exchange-rates.cache-ttl:6h}")
    private Duration cacheTtl;

    @Override
    public ExchangeRatesResponse getRates(String baseCurrency) {
        String base = normalizeCurrency(baseCurrency);
        CachedRates cached = getCachedRates(base);
        return new ExchangeRatesResponse(base, cached.rateDate(), cached.rates());
    }

    @Override
    public CurrencyConversionResponse convert(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (amount == null || amount.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be zero or greater");
        }
        String from = normalizeCurrency(fromCurrency);
        String to = normalizeCurrency(toCurrency);
        if (from.equals(to)) {
            return new CurrencyConversionResponse(amount, from, amount, to, BigDecimal.ONE, null);
        }

        CachedRates rates = getCachedRates(from);
        Double rate = rates.rates().get(to);
        if (rate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No exchange rate available from " + from + " to " + to);
        }
        BigDecimal exchangeRate = BigDecimal.valueOf(rate);
        BigDecimal converted = amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
        return new CurrencyConversionResponse(amount, from, converted, to, exchangeRate, rates.rateDate());
    }

    private CachedRates getCachedRates(String baseCurrency) {
        String cacheKey = CACHE_KEY_PREFIX + baseCurrency;
        String cachedJson = null;
        try {
            cachedJson = redisTemplate.opsForValue().get(cacheKey);
        } catch (RedisConnectionFailureException exception) {
            LOGGER.warn("Redis unavailable while reading exchange rates for {}", baseCurrency, exception);
        }
        if (cachedJson != null) {
            try {
                CachedRates cachedRates = objectMapper.readValue(cachedJson, CachedRates.class);
                if (cachedRates.rates() != null && cachedRates.rateDate() != null) {
                    return cachedRates;
                }
                LOGGER.warn("Ignoring invalid cached exchange rates for {}", baseCurrency);
            } catch (JsonProcessingException exception) {
                LOGGER.warn("Ignoring malformed cached exchange rates for {}", baseCurrency, exception);
            }
        }

        try {
            FrankfurterResponse response = restClientBuilder.build()
                    .get()
                    .uri(FRANKFURTER_URL + "?base={base}", baseCurrency)
                    .retrieve()
                    .body(FrankfurterResponse.class);
            if (response == null || response.rates() == null || response.date() == null
                    || !baseCurrency.equals(response.base())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY, "Exchange rate provider returned invalid data");
            }
            CachedRates fresh = new CachedRates(response.date(), Map.copyOf(response.rates()));
            try {
                redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(fresh), cacheTtl);
            } catch (JsonProcessingException exception) {
                LOGGER.warn("Could not serialize exchange rates for Redis: {}", baseCurrency, exception);
            } catch (RedisConnectionFailureException exception) {
                LOGGER.warn("Redis unavailable while caching exchange rates for {}", baseCurrency, exception);
            }
            return fresh;
        } catch (RestClientException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Exchange rate provider is unavailable", exception);
        }
    }

    private String normalizeCurrency(String currency) {
        if (currency == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency is required");
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        if (!CURRENCY_CODE.matcher(normalized).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Currency must be a three-letter code");
        }
        return normalized;
    }

    private record FrankfurterResponse(String base, LocalDate date, Map<String, Double> rates) {
    }

    private record CachedRates(LocalDate rateDate, Map<String, Double> rates) {
    }
}
