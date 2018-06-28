package services

import javax.inject.Singleton
import models.Country


@Singleton
class CountriesService {

  def getAllCountries: List[Country] = countries

  def isInEu(selectedCountry: String): Boolean = countries.exists(c => c.isEu && c.countryName == selectedCountry)



  private val countries = List(
    Country("Afghanistan", "AF", isEu = false, None),
    Country("Åland Islands", "AX", isEu = false, Some("EUR")),
    Country("Albania", "AL", isEu = false, Some("ALL")),
    Country("Algeria", "DZ", isEu = false, Some("DZD")),
    Country("American Samoa", "AS", isEu = false, Some("USD")),
    Country("Andorra", "AD", isEu = false, Some("EUR")),
    Country("Angola", "AO", isEu = false, Some("AOA")),
    Country("Anguilla", "AI", isEu = false, Some("XCD")),
    Country("Antarctica", "AQ", isEu = false, Some("USD")),
    Country("Antigua and Barbuda", "AG", isEu = false, Some("XCD")),
    Country("Argentina", "AR", isEu = false, Some("ARS")),
    Country("Armenia", "AM", isEu = false, Some("AMD")),
    Country("Aruba", "AW", isEu = false, Some("AWG")),
    Country("Australia", "AU", isEu = false, Some("AUD")),
    Country("Austria", "AT", isEu = true, Some("EUR")),
    Country("Azerbaijan", "AZ", isEu = false, Some("AZN")),
    Country("Bahamas (the)", "BS", isEu = false, Some("BSD")),
    Country("Bahrain", "BH", isEu = false, Some("BHD")),
    Country("Bangladesh", "BD", isEu = false, Some("BDT")),
    Country("Barbados", "BB", isEu = false, Some("BBD")),
    Country("Belarus", "BY", isEu = false, Some("BYN")),
    Country("Belgium", "BE", isEu = true, Some("EUR")),
    Country("Belize", "BZ", isEu = false, Some("BZD")),
    Country("Benin", "BJ", isEu = false, Some("XOF")),
    Country("Bermuda", "BM", isEu = false, Some("BMD")),
    Country("Bhutan", "BT", isEu = false, Some("BTN")),
    Country("Bolivia (Plurinational State of)", "BO", isEu = false, Some("BOB")),
    Country("Bonaire, Sint Eustatius and Saba", "BQ", isEu = false, None),
    Country("Bosnia and Herzegovina", "BA", isEu = false, Some("BAM")),
    Country("Botswana", "BW", isEu = false, Some("BWP")),
    Country("Bouvet Island", "BV", isEu = false, Some("NOK")),
    Country("Brazil", "BR", isEu = false, Some("BRL")),
    Country("British Indian Ocean Territory (the)", "IO", isEu = false, Some("USD")),
    Country("Brunei Darussalam", "BN", isEu = false, Some("BND")),
    Country("Bulgaria", "BG", isEu = true, Some("BGN")),
    Country("Burkina Faso", "BF", isEu = false, Some("XOF")),
    Country("Burundi", "BI", isEu = false, Some("BIF")),
    Country("Cabo Verde", "CV", isEu = false, Some("CVE")),
    Country("Cambodia", "KH", isEu = false, Some("KHR")),
    Country("Cameroon", "CM", isEu = false, Some("XAF")),
    Country("Canada", "CA", isEu = false, Some("CAD")),
    Country("Cayman Islands (the)", "KY", isEu = false, Some("KYD")),
    Country("Central African Republic (the)", "CF", isEu = false, Some("XAF")),
    Country("Chad", "TD", isEu = false, Some("XAF")),
    Country("Chile", "CL", isEu = false, Some("CLP")),
    Country("China", "CN", isEu = false, Some("CNY")),
    Country("Christmas Island", "CX", isEu = false, Some("AUD")),
    Country("Cocos (Keeling) Islands (the)", "CC", isEu = false, Some("AUD")),
    Country("Colombia", "CO", isEu = false, Some("COP")),
    Country("Comoros (the)", "KM", isEu = false, Some("KMF")),
    Country("Congo (the Democratic Republic of the)", "CD", isEu = false, Some("CDF")),
    Country("Congo (the)", "CG", isEu = false, Some("XAF")),
    Country("Cook Islands (the)", "CK", isEu = false, Some("NZD")),
    Country("Costa Rica", "CR", isEu = false, Some("CRC")),
    Country("Côte d'Ivoire", "CI", isEu = false, Some("XOF")),
    Country("Croatia", "HR", isEu = true, Some("HRK")),
    Country("Cuba", "CU", isEu = false, Some("CUP")),
    Country("Curaçao", "CW", isEu = false, None),
    Country("Cyprus", "CY", isEu = true, Some("EUR")),
    Country("Czechia", "CZ", isEu = true, Some("CZK")),
    Country("Denmark", "DK", isEu = true, Some("DKK")),
    Country("Djibouti", "DJ", isEu = false, Some("DJF")),
    Country("Dominica", "DM", isEu = false, Some("XCD")),
    Country("Dominican Republic (the)", "DO", isEu = false, Some("DOP")),
    Country("Ecuador", "EC", isEu = false, Some("ECS")),
    Country("Egypt", "EG", isEu = false, Some("EGP")),
    Country("El Salvador", "SV", isEu = false, Some("SVC")),
    Country("Equatorial Guinea", "GQ", isEu = false, Some("XAF")),
    Country("Eritrea", "ER", isEu = false, Some("ERN")),
    Country("Estonia", "EE", isEu = true, Some("EUR")),
    Country("Ethiopia", "ET", isEu = false, Some("ETB")),
    Country("Falkland Islands (the) [Malvinas]", "FK", isEu = false, None),
    Country("Faroe Islands (the)", "FO", isEu = false, Some("DKK")),
    Country("Fiji", "FJ", isEu = false, Some("FJD")),
    Country("Finland", "FI", isEu = true, Some("EUR")),
    Country("France", "FR", isEu = true, Some("EUR")),
    Country("French Guiana", "GF", isEu = false, Some("EUR")),
    Country("French Polynesia", "PF", isEu = false, Some("XPF")),
    Country("French Southern Territories (the)", "TF", isEu = false, Some("EUR")),
    Country("Gabon", "GA", isEu = false, Some("XAF")),
    Country("Gambia (the)", "GM", isEu = false, Some("GMD")),
    Country("Georgia", "GE", isEu = false, Some("GEL")),
    Country("Germany", "DE", isEu = true, Some("EUR")),
    Country("Ghana", "GH", isEu = false, Some("GHS")),
    Country("Gibraltar", "GI", isEu = false, None),
    Country("Greece", "GR", isEu = true, Some("EUR")),
    Country("Greenland", "GL", isEu = false, Some("DKK")),
    Country("Grenada", "GD", isEu = false, Some("XCD")),
    Country("Guadeloupe", "GP", isEu = false, Some("EUR")),
    Country("Guam", "GU", isEu = false, Some("USD")),
    Country("Guatemala", "GT", isEu = false, Some("GTQ")),
    Country("Guernsey", "GG", isEu = false, None),
    Country("Guinea", "GN", isEu = false, Some("GNF")),
    Country("Guinea-Bissau", "GW", isEu = false, Some("XOF")),
    Country("Guyana", "GY", isEu = false, Some("GYD")),
    Country("Haiti", "HT", isEu = false, Some("HTG")),
    Country("Heard Island and McDonald Islands", "HM", isEu = false, Some("AUD")),
    Country("Holy See (the)", "VA", isEu = false, Some("EUR")),
    Country("Honduras", "HN", isEu = false, Some("HNL")),
    Country("Hong Kong", "HK", isEu = false, Some("HKD")),
    Country("Hungary", "HU", isEu = true, Some("HUF")),
    Country("Iceland", "IS", isEu = false, Some("ISK")),
    Country("India", "IN", isEu = false, Some("INR")),
    Country("Indonesia", "ID", isEu = false, Some("IDR")),
    Country("Iran (Islamic Republic of)", "IR", isEu = false, None),
    Country("Iraq", "IQ", isEu = false, Some("IQD")),
    Country("Ireland", "IE", isEu = true, Some("EUR")),
    Country("Isle of Man", "IM", isEu = false, None),
    Country("Israel", "IL", isEu = false, Some("ILS")),
    Country("Italy", "IT", isEu = true, Some("EUR")),
    Country("Jamaica", "JM", isEu = false, Some("JMD")),
    Country("Japan", "JP", isEu = false, Some("JPY")),
    Country("Jersey", "JE", isEu = false, None),
    Country("Jordan", "JO", isEu = false, Some("JOD")),
    Country("Kazakhstan", "KZ", isEu = false, Some("KZT")),
    Country("Kenya", "KE", isEu = false, Some("KES")),
    Country("Kiribati", "KI", isEu = false, Some("AUD")),
    Country("Korea (the Democratic People's Republic of)", "KP", isEu = false, None),
    Country("Korea (the Republic of)", "KR", isEu = false, Some("KRW")),
    Country("Kuwait", "KW", isEu = false, Some("KWD")),
    Country("Kyrgyzstan", "KG", isEu = false, Some("KGS")),
    Country("Lao People's Democratic Republic (the)", "LA", isEu = false, Some("LAK")),
    Country("Latvia", "LV", isEu = true, Some("EUR")),
    Country("Lebanon", "LB", isEu = false, Some("LBP")),
    Country("Lesotho", "LS", isEu = false, Some("LSL")),
    Country("Liberia", "LR", isEu = false, Some("LRD")),
    Country("Libya", "LY", isEu = false, Some("LYD")),
    Country("Liechtenstein", "LI", isEu = false, Some("CHF")),
    Country("Lithuania", "LT", isEu = true, Some("EUR")),
    Country("Luxembourg", "LU", isEu = true, Some("EUR")),
    Country("Macao", "MO", isEu = false, Some("MOP")),
    Country("Macedonia (the former Yugoslav Republic of)", "MK", isEu = false, Some("MKD")),
    Country("Madagascar", "MG", isEu = false, Some("MGA")),
    Country("Malawi", "MW", isEu = false, Some("MWK")),
    Country("Malaysia", "MY", isEu = false, Some("MYR")),
    Country("Maldives", "MV", isEu = false, Some("MVR")),
    Country("Mali", "ML", isEu = false, Some("XOF")),
    Country("Malta", "MT", isEu = true, Some("EUR")),
    Country("Marshall Islands (the)", "MH", isEu = false, Some("USD")),
    Country("Martinique", "MQ", isEu = false, Some("EUR")),
    Country("Mauritania", "MR", isEu = false, Some("MRO")),
    Country("Mauritius", "MU", isEu = false, Some("MUR")),
    Country("Mayotte", "YT", isEu = false, Some("KMF")),
    Country("Mexico", "MX", isEu = false, Some("MXN")),
    Country("Micronesia (Federated States of)", "FM", isEu = false, Some("USD")),
    Country("Moldova (the Republic of)", "MD", isEu = false, Some("MDL")),
    Country("Monaco", "MC", isEu = false, Some("EUR")),
    Country("Mongolia", "MN", isEu = false, Some("MNT")),
    Country("Montenegro", "ME", isEu = false, Some("EUR")),
    Country("Montserrat", "MS", isEu = false, Some("XCD")),
    Country("Morocco", "MA", isEu = false, Some("MAD")),
    Country("Mozambique", "MZ", isEu = false, Some("MZN")),
    Country("Myanmar", "MM", isEu = false, Some("MMK")),
    Country("Namibia", "NA", isEu = false, Some("ZAR")),
    Country("Nauru", "NR", isEu = false, Some("AUD")),
    Country("Nepal", "NP", isEu = false, Some("NPR")),
    Country("Netherlands (the)", "NL", isEu = true, Some("EUR")),
    Country("New Caledonia", "NC", isEu = false, Some("XPF")),
    Country("New Zealand", "NZ", isEu = false, Some("NZD")),
    Country("Nicaragua", "NI", isEu = false, Some("NIO")),
    Country("Niger (the)", "NE", isEu = false, Some("XOF")),
    Country("Nigeria", "NG", isEu = false, Some("NGN")),
    Country("Niue", "NU", isEu = false, Some("NZD")),
    Country("Norfolk Island", "NF", isEu = false, Some("AUD")),
    Country("Northern Mariana Islands (the)", "MP", isEu = false, Some("USD")),
    Country("Norway", "NO", isEu = false, Some("NOK")),
    Country("Oman", "OM", isEu = false, Some("OMR")),
    Country("Pakistan", "PK", isEu = false, Some("PKR")),
    Country("Palau", "PW", isEu = false, Some("USD")),
    Country("Palestine, State of", "PS", isEu = false, None),
    Country("Panama", "PA", isEu = false, Some("PAB")),
    Country("Papua New Guinea", "PG", isEu = false, Some("PGK")),
    Country("Paraguay", "PY", isEu = false, Some("PYG")),
    Country("Peru", "PE", isEu = false, Some("PEN")),
    Country("Philippines (the)", "PH", isEu = false, Some("PHP")),
    Country("Pitcairn", "PN", isEu = false, Some("NZD")),
    Country("Poland", "PL", isEu = true, Some("PLN")),
    Country("Portugal", "PT", isEu = true, Some("EUR")),
    Country("Puerto Rico", "PR", isEu = false, Some("USD")),
    Country("Qatar", "QA", isEu = false, Some("QAR")),
    Country("Réunion", "RE", isEu = false, Some("EUR")),
    Country("Romania", "RO", isEu = true, Some("RON")),
    Country("Russian Federation (the)", "RU", isEu = false, Some("RUB")),
    Country("Rwanda", "RW", isEu = false, Some("RWF")),
    Country("Saint Barthélemy", "BL", isEu = false, Some("EUR")),
    Country("Saint Helena, Ascension and Tristan da Cunha", "SH", isEu = false, None),
    Country("Saint Kitts and Nevis", "KN", isEu = false, Some("XCD")),
    Country("Saint Lucia", "LC", isEu = false, Some("XCD")),
    Country("Saint Martin (French part)", "MF", isEu = false, Some("EUR")),
    Country("Saint Pierre and Miquelon", "PM", isEu = false, Some("EUR")),
    Country("Saint Vincent and the Grenadines", "VC", isEu = false, Some("XCD")),
    Country("Samoa", "WS", isEu = false, Some("WST")),
    Country("San Marino", "SM", isEu = false, Some("EUR")),
    Country("Sao Tome and Principe", "ST", isEu = false, Some("STD")),
    Country("Saudi Arabia", "SA", isEu = false, Some("SAR")),
    Country("Senegal", "SN", isEu = false, Some("XOF")),
    Country("Serbia", "RS", isEu = false, Some("RSD")),
    Country("Seychelles", "SC", isEu = false, Some("SCR")),
    Country("Sierra Leone", "SL", isEu = false, Some("SLL")),
    Country("Singapore", "SG", isEu = false, Some("SGD")),
    Country("Sint Maarten (Dutch part)", "SX", isEu = false, Some("EUR")),
    Country("Slovakia", "SK", isEu = true, Some("EUR")),
    Country("Slovenia", "SI", isEu = true, Some("EUR")),
    Country("Solomon Islands", "SB", isEu = false, Some("SBD")),
    Country("Somalia", "SO", isEu = false, Some("SOS")),
    Country("South Africa", "ZA", isEu = false, Some("ZAR")),
    Country("South Georgia and the South Sandwich Islands", "GS", isEu = false, None),
    Country("South Sudan", "SS", isEu = false, Some("SDG")),
    Country("Spain", "ES", isEu = true, Some("EUR")),
    Country("Sri Lanka", "LK", isEu = false, Some("LKR")),
    Country("Sudan (the)", "SD", isEu = false, Some("SDG")),
    Country("Suriname", "SR", isEu = false, Some("SRD")),
    Country("Svalbard and Jan Mayen", "SJ", isEu = false, Some("NOK")),
    Country("Swaziland", "SZ", isEu = false, Some("SZL")),
    Country("Sweden", "SE", isEu = true, Some("SEK")),
    Country("Switzerland", "CH", isEu = false, Some("CHF")),
    Country("Syrian Arab Republic", "SY", isEu = false, None),
    Country("Taiwan (Province of China)", "TW", isEu = false, Some("TWD")),
    Country("Tajikistan", "TJ", isEu = false, None),
    Country("Tanzania, United Republic of", "TZ", isEu = false, Some("TZS")),
    Country("Thailand", "TH", isEu = false, Some("THB")),
    Country("Timor-Leste", "TL", isEu = false, Some("USD")),
    Country("Togo", "TG", isEu = false, Some("XOF")),
    Country("Tokelau", "TK", isEu = false, Some("NZD")),
    Country("Tonga", "TO", isEu = false, Some("TOP")),
    Country("Trinidad and Tobago", "TT", isEu = false, Some("TTD")),
    Country("Tunisia", "TN", isEu = false, Some("TND")),
    Country("Turkey", "TR", isEu = false, Some("TRY")),
    Country("Turkmenistan", "TM", isEu = false, Some("TMT")),
    Country("Turks and Caicos Islands (the)", "TC", isEu = false, Some("USD")),
    Country("Tuvalu", "TV", isEu = false, Some("AUD")),
    Country("Uganda", "UG", isEu = false, Some("UGX")),
    Country("Ukraine", "UA", isEu = false, Some("UAH")),
    Country("United Arab Emirates (the)", "AE", isEu = false, Some("AED")),
    Country("United Kingdom of Great Britain and Northern Ireland (the)", "GB", isEu = true, None),
    Country("United States Minor Outlying Islands (the)", "UM", isEu = false, Some("USD")),
    Country("United States of America (the)", "US", isEu = false, Some("USD")),
    Country("Uruguay", "UY", isEu = false, Some("UYU")),
    Country("Uzbekistan", "UZ", isEu = false, Some("UZS")),
    Country("Vanuatu", "VU", isEu = false, Some("VUV")),
    Country("Venezuela (Bolivarian Republic of)", "VE", isEu = false, Some("VEF")),
    Country("Viet Nam", "VN", isEu = false, Some("VND")),
    Country("Virgin Islands (British)", "VG", isEu = false, Some("USD")),
    Country("Virgin Islands (U.S.)", "VI", isEu = false, Some("USD")),
    Country("Wallis and Futuna", "WF", isEu = false, Some("XPF")),
    Country("Western Sahara", "EH", isEu = false, Some("MAD")),
    Country("Yemen", "YE", isEu = false, Some("YER")),
    Country("Zambia", "ZM", isEu = false, Some("ZMW")),
    Country("Zimbabwe", "ZW", isEu = false, Some("ZWL"))
  )
}
