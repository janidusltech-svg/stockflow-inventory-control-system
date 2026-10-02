package com.stockflow.shared.reporting;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class CsvReportSupport {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void row(StringBuilder csv, Object... values) {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                csv.append(',');
            }
            csv.append(escape(values[index]));
        }
        csv.append("\r\n");
    }

    public byte[] utf8(StringBuilder csv) {
        return ("\uFEFF" + csv).getBytes(StandardCharsets.UTF_8);
    }

    public String readable(String enumName) {
        String lower = enumName.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    public String format(LocalDate date) {
        return date == null ? "" : DATE.format(date);
    }

    public String format(LocalDateTime dateTime) {
        return dateTime == null ? "" : DATE_TIME.format(dateTime);
    }

    public String money(BigDecimal amount) {
        return amount == null
                ? "0.00"
                : amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String escape(Object raw) {
        String value = raw == null ? "" : raw.toString();
        if (!value.isEmpty() && "=+-@".indexOf(value.charAt(0)) >= 0) {
            value = "'" + value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
