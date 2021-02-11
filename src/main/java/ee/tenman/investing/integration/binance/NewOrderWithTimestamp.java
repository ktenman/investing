package ee.tenman.investing.integration.binance;

import com.binance.api.client.domain.OrderSide;
import com.binance.api.client.domain.OrderType;
import com.binance.api.client.domain.TimeInForce;
import com.binance.api.client.domain.account.NewOrder;

public class NewOrderWithTimestamp extends NewOrder {
    private long timestamp;

    public NewOrderWithTimestamp(String symbol, OrderSide side, OrderType type, TimeInForce timeInForce, String quantity, long timestamp) {
        super(symbol, side, type, timeInForce, quantity);
        this.timestamp = timestamp;
    }

    public NewOrderWithTimestamp(String symbol, OrderSide side, OrderType type, TimeInForce timeInForce, String quantity, String price, long timestamp) {
        super(symbol, side, type, timeInForce, quantity, price);
        this.timestamp = timestamp;
    }

    public static NewOrderWithTimestamp marketBuy(String symbol, String quantity, long timestamp) {
        return new NewOrderWithTimestamp(symbol, OrderSide.BUY, OrderType.MARKET, (TimeInForce) null, quantity, timestamp);
    }

    public static NewOrderWithTimestamp marketSell(String symbol, String quantity, long timestamp) {
        return new NewOrderWithTimestamp(symbol, OrderSide.SELL, OrderType.MARKET, (TimeInForce) null, quantity, timestamp);
    }

    @Override
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

}
