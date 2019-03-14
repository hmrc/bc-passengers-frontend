package services

import models.Currency
import util.BaseSpec

class CurrencyServiceSpec extends BaseSpec {

  private val expectedCurrencies: List[Currency] = List(
    Currency("AED", "United Arab Emirates dirham (AED)", Some("AED"), List("UAE", "Emirati", "dubai", "abu dahbi", "abu dhabi")),
    Currency("ALL", "Albanian lek (ALL)", Some("ALL"), Nil),
    Currency("AMD", "Armenian dram (AMD)", Some("AMD"), Nil),
    Currency("AOA", "Angolan kwanza (AOA)", Some("AOA"), Nil),
    Currency("ARS", "Argentinian peso (ARS)", Some("ARS"), Nil),
    Currency("AUD", "Australian dollars (AUD)", Some("AUD"), List("Australian", "Oz")),
    Currency("AWG", "Aruban florin (AWG)", Some("AWG"), Nil),
    Currency("AZN", "Azerbaijani manat (AZN)", Some("AZN"), Nil),
    Currency("BAM", "Bosnia-Herzegovinian marka (BAM)", Some("BAM"), Nil),
    Currency("BBD", "Barbados dollars (BBD)", Some("BBD"), Nil),
    Currency("BDT", "Bangladeshi taka (BDT)", Some("BDT"), Nil),
    Currency("BGN", "Bulgarian lev (BGN)", Some("BGN"), Nil),
    Currency("BHD", "Bahrainian dinar (BHD)", Some("BHD"), Nil),
    Currency("BIF", "Burundi francs (BIF)", Some("BIF"), Nil),
    Currency("BMD", "Bermudan dollars (BMD)", Some("BMD"), Nil),
    Currency("BND", "Brunei dollars (BND)", Some("BND"), Nil),
    Currency("BOB", "Bolivian boliviano (BOB)", Some("BOB"), Nil),
    Currency("BRL", "Brazilian real (BRL)", Some("BRL"), Nil),
    Currency("BSD", "Bahamas dollars (BSD)", Some("BSD"), Nil),
    Currency("BTN", "Bhutan ngultrum (BTN)", Some("BTN"), Nil),
    Currency("BWP", "Botswanan pula (BWP)", Some("BWP"), Nil),
    Currency("BYN", "Belarusian roubles (BYN)", Some("BYN"), Nil),
    Currency("BZD", "Belize dollars (BZD)", Some("BZD"), Nil),
    Currency("CAD", "Canadian dollars (CAD)", Some("CAD"), Nil),
    Currency("CDF", "Democratic Republic of Congo francs (CDF)", Some("CDF"), Nil),
    Currency("CHF", "Swiss francs (CHF)", Some("CHF"), List("Swiss", "Switzerland")),
    Currency("CLP", "Chilean pesos (CLP)", Some("CLP"), List("Chile")),
    Currency("CNY", "Chinese yuan (CNY)", Some("CNY"), List("China")),
    Currency("COP", "Colombian pesos (COP)", Some("COP"), List("Columbia")),
    Currency("CRC", "Costa Rican colon (CRC)", Some("CRC"), Nil),
    Currency("CUP", "Cuban pesos (CUP)", Some("CUP"), List("Cuban")),
    Currency("CVE", "Cape Verde Islands escudos (CVE)", Some("CVE"), Nil),
    Currency("CZK", "Czech Republic koruna (CZK)", Some("CZK"), List("Czechoslovakia")),
    Currency("DJF", "Djibouti francs (DJF)", Some("DJF"), Nil),
    Currency("DKK", "Danish krone (DKK)", Some("DKK"), List("Denmark")),
    Currency("DOP", "Dominican Republic pesos (DOP)", Some("DOP"), Nil),
    Currency("DZD", "Algerian dinar (DZD)", Some("DZD"), Nil),
    Currency("ECS", "Ecuadorian dollars (ECS)", Some("ECS"), Nil),
    Currency("EGP", "Egyptian pounds (EGP)", Some("EGP"), List("Egypt")),
    Currency("ERN", "Eritrean nakfa (ERN)", Some("ERN"), Nil),
    Currency("ETB", "Ethiopian birr (ETB)", Some("ETB"), Nil),
    Currency("EUR", "Euro (EUR)", Some("EUR"), List("Europe", "European")),
    Currency("FJD", "Fiji Islands dollars (FJD)", Some("FJD"), Nil),
    Currency("GEL", "Georgian lari (GEL)", Some("GEL"), Nil),
    Currency("GHS", "Ghanian cedi (GHS)", Some("GHS"), List("Ghana")),
    Currency("GMD", "Gambian dalasi (GMD)", Some("GMD"), Nil),
    Currency("GNF", "Guinean francs (GNF)", Some("GNF"), Nil),
    Currency("GTQ", "Guatemalan quetzal (GTQ)", Some("GTQ"), Nil),
    Currency("GYD", "Guyanan dollars (GYD)", Some("GYD"), Nil),
    Currency("HKD", "Hong Kong dollars (HKD)", Some("HKD"), Nil),
    Currency("HNL", "Honduras lempira (HNL)", Some("HNL"), Nil),
    Currency("HRK", "Croatian kuna (HRK)", Some("HRK"), Nil),
    Currency("HTG", "Haiti gourde (HTG)", Some("HTG"), Nil),
    Currency("HUF", "Hungarian forints (HUF)", Some("HUF"), List("Hungary")),
    Currency("IDR", "Indonesian rupiahs (IDR)", Some("IDR"), Nil),
    Currency("ILS", "Israeli shekels (ILS)", Some("ILS"), Nil),
    Currency("INR", "Indian rupees (INR)", Some("INR"), List("Indian")),
    Currency("IQD", "Iraqi dinar (IQD)", Some("IQD"), Nil),
    Currency("ISK", "Icelandic krona (ISK)", Some("ISK"), Nil),
    Currency("JMD", "Jamaican dollars (JMD)", Some("JMD"), List("Jamaican")),
    Currency("JOD", "Jordanian dinar (JOD)", Some("JOD"), Nil),
    Currency("JPY", "Japanese yen (JPY)", Some("JPY"), Nil),
    Currency("KES", "Kenyan shillings (KES)", Some("KES"), List("schilling")),
    Currency("KGS", "Kyrgyz Republic som (KGS)", Some("KGS"), Nil),
    Currency("KHR", "Cambodian riel (KHR)", Some("KHR"), Nil),
    Currency("KMF", "Comoros francs (KMF)", Some("KMF"), Nil),
    Currency("KRW", "South Korean won (KRW)", Some("KRW"), Nil),
    Currency("KWD", "Kuwaiti dinar (KWD)", Some("KWD"), Nil),
    Currency("KYD", "Cayman Islands dollars (KYD)", Some("KYD"), Nil),
    Currency("KZT", "Kazakhstanian tenge (KZT)", Some("KZT"), Nil),
    Currency("LAK", "Lao kip (LAK)", Some("LAK"), Nil),
    Currency("LBP", "Lebanese pounds (LBP)", Some("LBP"), List("Lebanon")),
    Currency("LKR", "Sri Lankan rupees (LKR)", Some("LKR"), Nil),
    Currency("LRD", "Liberian dollars (LRD)", Some("LRD"), Nil),
    Currency("LSL", "Lesotho loti (LSL)", Some("LSL"), Nil),
    Currency("LYD", "Libyan dinar (LYD)", Some("LYD"), Nil),
    Currency("MAD", "Moroccon dirham (MAD)", Some("MAD"), Nil),
    Currency("MDL", "Moldovian leu (MDL)", Some("MDL"), List("Moldova")),
    Currency("MGA", "Madagascar malagasy ariary (MGA)", Some("MGA"), Nil),
    Currency("MKD", "Macedonian denar (MKD)", Some("MKD"), Nil),
    Currency("MMK", "Myanmar kyat (MMK)", Some("MMK"), Nil),
    Currency("MNT", "Mongolian tugrik (MNT)", Some("MNT"), Nil),
    Currency("MOP", "Macaon pataca (MOP)", Some("MOP"), Nil),
    Currency("MRO", "Mauritanian ouguiya (MRO)", Some("MRO"), Nil),
    Currency("MUR", "Mauritius rupees (MUR)", Some("MUR"), Nil),
    Currency("MVR", "Maldives rufiyaa (MVR)", Some("MVR"), Nil),
    Currency("MWK", "Malawian kwacha (MWK)", Some("MWK"), Nil),
    Currency("MXN", "Mexican pesos (MXN)", Some("MXN"), List("Mexico")),
    Currency("MYR", "Malaysian ringgit (MYR)", Some("MYR"), Nil),
    Currency("MZN", "Mozambiquan metical (MZN)", Some("MZN"), Nil),
    Currency("NGN", "Nigerian naira (NGN)", Some("NGN"), Nil),
    Currency("NIO", "Nicaraguan cordoba (NIO)", Some("NIO"), Nil),
    Currency("NOK", "Norwegian krone (NOK)", Some("NOK"), List("Norway")),
    Currency("NPR", "Nepalese rupees (NPR)", Some("NPR"), Nil),
    Currency("NZD", "New Zealand dollars (NZD)", Some("NZD"), List("Kiwi")),
    Currency("OMR", "Oman rial (OMR)", Some("OMR"), Nil),
    Currency("PAB", "Panamanian balboa (PAB)", Some("PAB"), List("Panamanian")),
    Currency("PEN", "Peruvian sol (PEN)", Some("PEN"), Nil),
    Currency("PGK", "Papua New Guinea kina (PGK)", Some("PGK"), Nil),
    Currency("PHP", "Philippineno pesos (PHP)", Some("PHP"), List("Philippenes", "Philipines", "Phillippines", "Philipines")),
    Currency("PKR", "Pakistani rupees (PKR)", Some("PKR"), Nil),
    Currency("PLN", "Polish zloty (PLN)", Some("PLN"), List("Poland")),
    Currency("PYG", "Paraguan guarani (PYG)", Some("PYG"), List("Paraguay")),
    Currency("QAR", "Qatarian riyal (QAR)", Some("QAR"), Nil),
    Currency("RON", "Romanian leu (RON)", Some("RON"), Nil),
    Currency("RSD", "Serbian dinar (RSD)", Some("RSD"), Nil),
    Currency("RUB", "Russian roubles (RUB)", Some("RUB"), List("USSR", "Soviet Union")),
    Currency("RWF", "Rwandan francs RWF", Some("RWF"), Nil),
    Currency("SAR", "Saudi Arabian riyal (SAR)", Some("SAR"), Nil),
    Currency("SBD", "Soloman Islands dollars (SBD)", Some("SBD"), Nil),
    Currency("SCR", "Seychelles rupees (SCR)", Some("SCR"), Nil),
    Currency("SDG", "Sudan Republic pounds (SDG)", Some("SDG"), Nil),
    Currency("SEK", "Swedish krona (SEK)", Some("SEK"), List("Sweden")),
    Currency("SGD", "Singapore dollars (SGD)", Some("SGD"), Nil),
    Currency("SLL", "Sierra Leone leone (SLL)", Some("SLL"), Nil),
    Currency("SOS", "Somali Republic schillings (SOS)", Some("SOS"), Nil),
    Currency("SRD", "Suriname dollars (SRD)", Some("SRD"), Nil),
    Currency("STD", "São Tomé and Príncipe dobra (STD)", Some("STD"), Nil),
    Currency("SVC", "El Salvadorian colon (SVC)", Some("SVC"), Nil),
    Currency("SZL", "Eswatini lilangeni (SZL)", Some("SZL"), List("Swaziland")),
    Currency("THB", "Thai baht (THB)", Some("THB"), List("Thai")),
    Currency("TMT", "Turkmenistanian manat (TMT)", Some("TMT"), Nil),
    Currency("TND", "Tunisian dinar (TND)", Some("TND"), Nil),
    Currency("TOP", "Tongan pa'anga (TOP)", Some("TOP"), Nil),
    Currency("TRY", "Turkish lira (TRY)", Some("TRY"), List("Turkey")),
    Currency("TTD", "Trinidad and Tobago dollars (TTD)", Some("TTD"), Nil),
    Currency("TWD", "Taiwanese dollars (TWD)", Some("TWD"), Nil),
    Currency("TZS", "Tanzanian schillings (TZS)", Some("TZS"), Nil),
    Currency("UAH", "Ukrainian hryvnia (UAH)", Some("UAH"), List("Ukraine")),
    Currency("UGX", "Ugandan schillings (UGX)", Some("UGX"), Nil),
    Currency("USD", "USA dollars (USD)", Some("USD"), List("USD", "USA", "US", "America", "States", "United States", "United", "American")),
    Currency("UYU", "Uruguan pesos (UYU)", Some("UYU"), List("Urguguay")),
    Currency("UZS", "Uzbekistanian sum (UZS)", Some("UZS"), Nil),
    Currency("VEF", "Venezuelan bolivar fuerte (VEF)", Some("VEF"), Nil),
    Currency("VND", "Vietnamese dong (VND)", Some("VND"), Nil),
    Currency("VUV", "Vanuatuan vatu (VUV)", Some("VUV"), Nil),
    Currency("WST", "Western Samoan tala (WST)", Some("WST"), Nil),
    Currency("XAF", "Central African francs (XAF)", Some("XAF"), List("Cameroon", "Chad", "Congo", "Equatorial Guinea", "Gabon")),
    Currency("XCD", "East Caribbean dollars (XCD)", Some("XCD"), List("Dominica", "Grenada", "Montserrat", "St Christopher and Anguilla", "Saint Christopher", "St Lucia", "Saint Lucia", "St Vincent", "Saint Vincent")),
    Currency("XOF", "West African francs (XOF)", Some("XOF"), List("Benin", "Burkina Faso", "Cote d'Ivoire", "Ivory Coast", "Guinea Bissau", "Mali Republic", "Niger Republic", "Senegal", "Cote dIvoire", "Cote d Ivoire", "Republic of Mali", "Republic of the Niger", "Togolese Republic", "Togo Republic", "Republic of Togo")),
    Currency("XPF", "CFP francs (XPF)", Some("XPF"), List("Fr. Polynesia", "French Polynesia", "New Caledonia", "Wallis and Futuna Islands")),
    Currency("YER", "Yemen rial (YER)", Some("YER"), Nil),
    Currency("ZAR", "South African rand (ZAR)", Some("ZAR"), Nil),
    Currency("ZMW", "Zambian kwacha (ZMW)", Some("ZMW"), Nil),
    Currency("ZWL", "Zimbabwean dollars (ZWL)", Some("ZWL"), Nil),
    Currency("GBP", "British pounds (GBP)", None, List("England", "Scotland", "Wales", "Northern Ireland", "British", "sterling", "pound", "GB")),
    Currency("FKP", "Falkland Island pounds (FKP)", None, List("Falklands")),
    Currency("GIP", "Gibraltar pounds (GIP)", None, Nil),
    Currency("GGP", "Guernsey pounds (GGP)", None, List("Channel Islands")),
    Currency("IMP", "Isle of Man pounds (IMP)", None, Nil),
    Currency("JEP", "Jersey pounds (JEP)", None, List("Channel Islands", "St Helier", "Sanint Helier")),
    Currency("SHP", "Saint Helenian pounds (SHP)", None, List("St Helenia"))
  )

  "getAllCurrencies" should {

    val currencyService = app.injector.instanceOf[CurrencyService]

    "return the expected currencies" in {
      currencyService.getAllCurrencies shouldEqual expectedCurrencies
    }
  }
}
