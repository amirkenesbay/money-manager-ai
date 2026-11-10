package ai.moneymanager.domain.model

enum class Currency(val symbol: String) {
    KZT("₸"),
    USD("$"),
    RUB("₽"),
    EUR("€"),
    KGS("с"),
    BYN("Br"),
    UZS("сўм"),
    UAH("₴"),
    GEL("₾"),
    AMD("֏"),
    AZN("₼"),
    TJS("смн"),
    TMT("m"),
    MDL("L"),
    TRY("₺"),
    AED("Dh"),
    GBP("£"),
    CNY("CN¥"),
    JPY("¥"),
    KRW("₩"),
    THB("฿"),
    INR("₹"),
    IDR("Rp"),
    VND("₫"),
    MNT("₮"),
    PLN("zł"),
    CZK("Kč"),
    CHF("Fr"),
    CAD("C$"),
    AUD("A$"),
    ILS("₪"),
    EGP("E£"),
    SAR("SR"),
    QAR("QR");

    companion object {
        val DEFAULT: Currency = KZT
    }
}
