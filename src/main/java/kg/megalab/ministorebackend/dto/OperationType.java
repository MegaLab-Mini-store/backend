package kg.megalab.ministorebackend.dto;

public enum OperationType {
    DELIVERY("Приёмка"),
    SALE("Продажа"),
    SALE_VOID("Аннулирование продажи"),
    CORRECTION("Корректировка");

    private final String label;

    OperationType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** Подпись по строковому коду из БД; неизвестный код возвращается как есть. */
    public static String label(String code) {
        for (OperationType t : values()) {
            if (t.name().equals(code)) {
                return t.label;
            }
        }
        return code;
    }
}