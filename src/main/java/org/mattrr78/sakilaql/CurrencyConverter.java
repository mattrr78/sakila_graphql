package org.mattrr78.sakilaql;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CurrencyConverter {
    public BigDecimal convertFromUSD(BigDecimal price, Currency toCurrency)  {
        if (price == null || BigDecimal.ZERO.equals(price))  {
            return BigDecimal.ZERO;
        } else {
            BigDecimal usdMultiplicand = BigDecimal.valueOf(calculateUSDMultiplicand(toCurrency));
            BigDecimal convertedAmount = price.multiply(usdMultiplicand);

            // We don't want to return half a Yen or quarter of a Peso
            int scale = usdMultiplicand.intValue() < 10 ? 2 : 0;
            return convertedAmount.setScale(scale, RoundingMode.HALF_UP);
        }
    }

    public double calculateUSDMultiplicand(Currency currency)  {
        switch (currency)  {
            case EUR: return 0.9d;
            case GBP: return 0.77d;
            case CAD: return 1.31d;
            case MXN: return 18.95d;
            case INR: return 71.12d;
            case AUD: return 1.45d;
            case JPY: return 108.1d;
            default: return 1d;
        }
    }

}
