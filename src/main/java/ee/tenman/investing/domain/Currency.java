package ee.tenman.investing.domain;

public enum Currency {
    EUR("Euro"),
    GBX("Pound sterling"),
    GBP("Pound sterling"),
    USD("United States Dollar");

    private String value;

    Currency(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
