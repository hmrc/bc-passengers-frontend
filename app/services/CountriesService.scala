package services

import javax.inject.Singleton
import models.Country
import play.api.libs.json.{JsValue, Json}

@Singleton
class CountriesService {

  def getAllCountries: List[Country] = countries

  def isInEu(selectedCountry: String): Boolean = countries.exists(c => c.isEu && c.countryName == selectedCountry)

  def isValidCountryName(selectedCountry: String): Boolean = countries.exists(c => c.countryName == selectedCountry)

  def getCountryByName(countryName: String): Option[Country] = countries.find(_.countryName == countryName)

  private val countries = List(
    Country("Afghanistan", "AF", isEu=false, Nil),
    Country("Åland Islands", "AX", isEu=false, List("Aland Islands")),
    Country("Albania", "AL", isEu=false, Nil),
    Country("Algeria", "DZ", isEu=false, Nil),
    Country("American Samoa", "AS", isEu=false, List("Samoa")),
    Country("Andorra", "AD", isEu=false, Nil),
    Country("Angola", "AO", isEu=false, Nil),
    Country("Anguilla", "AI", isEu=false, Nil),
    Country("Antarctica", "AQ", isEu=false, List("Antartica")),
    Country("Antigua and Barbuda", "AG", isEu=false, Nil),
    Country("Argentina", "AR", isEu=false, Nil),
    Country("Armenia", "AM", isEu=false, Nil),
    Country("Aruba", "AW", isEu=false, Nil),
    Country("Australia", "AU", isEu=false, List("Oz")),
    Country("Austria", "AT", isEu=true, Nil),
    Country("Azerbaijan", "AZ", isEu=false, Nil),
    Country("Bahamas", "BS", isEu=false, Nil),
    Country("Bahrain", "BH", isEu=false, Nil),
    Country("Bangladesh", "BD", isEu=false, Nil),
    Country("Barbados", "BB", isEu=false, Nil),
    Country("Belarus", "BY", isEu=false, Nil),
    Country("Belgium", "BE", isEu=true, Nil),
    Country("Belize", "BZ", isEu=false, Nil),
    Country("Benin", "BJ", isEu=false, Nil),
    Country("Bermuda", "BM", isEu=false, Nil),
    Country("Bhutan", "BT", isEu=false, Nil),
    Country("Bolivia", "BO", isEu=false, Nil),
    Country("Bonaire, Sint Eustatius and Saba", "BQ", isEu=false, Nil),
    Country("Bosnia and Herzegovina", "BA", isEu=false, Nil),
    Country("Botswana", "BW", isEu=false, Nil),
    Country("Bouvet Island", "BV", isEu=false, Nil),
    Country("Brazil", "BR", isEu=false, Nil),
    Country("British Indian Ocean Territory", "IO", isEu=false, List("BIOT")),
    Country("Brunei Darussalam", "BN", isEu=false, Nil),
    Country("Bulgaria", "BG", isEu=true, Nil),
    Country("Burkina Faso", "BF", isEu=false, Nil),
    Country("Burundi", "BI", isEu=false, Nil),
    Country("Cape Verde", "CV", isEu=false, List("Republic of Cabo Verde")),
    Country("Cambodia", "KH", isEu=false, Nil),
    Country("Cameroon", "CM", isEu=false, Nil),
    Country("Canada", "CA", isEu=false, Nil),
    Country("Canary Islands", "ES", isEu=false, List("Canaries")),
    Country("Cayman Islands", "KY", isEu=false, List("Caymans")),
    Country("Central African Republic", "CF", isEu=false, List("CAR")),
    Country("Chad", "TD", isEu=false, Nil),
    Country("Chile", "CL", isEu=false, Nil),
    Country("China", "CN", isEu=false, List("People's Republic of China", "PRC", "Peoples Republic of China")),
    Country("Christmas Island", "CX", isEu=false, List("Christmas Islands")),
    Country("Cocos (Keeling) Islands", "CC", isEu=false, Nil),
    Country("Colombia", "CO", isEu=false, List("Columbia")),
    Country("Comoros", "KM", isEu=false, Nil),
    Country("Democratic Republic of the Congo", "CD", isEu=false, List("DR Congo", "DRC")),
    Country("Republic of the Congo", "CG", isEu=false, List("Congo-Brazzaville", "Congo Republic")),
    Country("Cook Islands", "CK", isEu=false, Nil),
    Country("Costa Rica", "CR", isEu=false, Nil),
    Country("Côte d'Ivoire", "CI", isEu=false, List("Ivory Coast", "Cote d'Ivoire", "Cote dIvoire", "Cote d Ivoire")),
    Country("Croatia", "HR", isEu=true, Nil),
    Country("Cuba", "CU", isEu=false, Nil),
    Country("Curaçao", "CW", isEu=false, List("Curacao", "Curacoa")),
    Country("Cyprus", "CY", isEu=true, Nil),
    Country("Czech Republic", "CZ", isEu=true, List("Czechoslovakia", "Czechia")),
    Country("Denmark", "DK", isEu=true, List("Danish")),
    Country("Djibouti", "DJ", isEu=false, Nil),
    Country("Dominica", "DM", isEu=false, Nil),
    Country("Dominican Republic", "DO", isEu=false, Nil),
    Country("Ecuador", "EC", isEu=false, Nil),
    Country("Egypt", "EG", isEu=false, Nil),
    Country("El Salvador", "SV", isEu=false, Nil),
    Country("Equatorial Guinea", "GQ", isEu=false, Nil),
    Country("Eritrea", "ER", isEu=false, Nil),
    Country("Estonia", "EE", isEu=true, Nil),
    Country("Ethiopia", "ET", isEu=false, Nil),
    Country("Eswatini", "SZ", isEu=false, List("Swaziland")),
    Country("Falkland Islands", "FK", isEu=false, List("Falklands")),
    Country("Faroe Islands", "FO", isEu=false, List("Faroes")),
    Country("Fiji", "FJ", isEu=false, Nil),
    Country("Finland", "FI", isEu=true, Nil),
    Country("France", "FR", isEu=true, Nil),
    Country("French Guiana", "GF", isEu=false, List("French Guayana")),
    Country("French Polynesia", "PF", isEu=false, List("Polynesian Islands")),
    Country("French Southern Territories", "TF", isEu=false, Nil),
    Country("Fuerteventura", "ES", isEu = false, Nil),
    Country("Gabon", "GA", isEu=false, Nil),
    Country("Gambia", "GM", isEu=false, Nil),
    Country("Georgia", "GE", isEu=false, Nil),
    Country("Germany", "DE", isEu=true, List("Deutschland")),
    Country("Ghana", "GH", isEu=false, Nil),
    Country("Gibraltar", "GI", isEu=false, List("Gibraltar Rock")),
    Country("Grand Canaria", "ES", isEu=false, List("Gran Canaria")),
    Country("Greece", "GR", isEu=true, Nil),
    Country("Greenland", "GL", isEu=false, Nil),
    Country("Grenada", "GD", isEu=false, Nil),
    Country("Guadeloupe", "GP", isEu=false, Nil),
    Country("Guam", "GU", isEu=false, Nil),
    Country("Guatemala", "GT", isEu=false, Nil),
    Country("Guernsey", "GG", isEu=false, List("Channel Islands")),
    Country("Guinea", "GN", isEu=false, Nil),
    Country("Guinea-Bissau", "GW", isEu=false, Nil),
    Country("Guyana", "GY", isEu=false, Nil),
    Country("Haiti", "HT", isEu=false, Nil),
    Country("Heard Island and McDonald Islands", "HM", isEu=false, Nil),
    Country("Holy See", "VA", isEu=false, Nil),
    Country("Honduras", "HN", isEu=false, Nil),
    Country("Hong Kong", "HK", isEu=false, Nil),
    Country("Hungary", "HU", isEu=true, Nil),
    Country("Iceland", "IS", isEu=false, Nil),
    Country("India", "IN", isEu=false, Nil),
    Country("Indonesia", "ID", isEu=false, Nil),
    Country("Iran", "IR", isEu=false, Nil),
    Country("Iraq", "IQ", isEu=false, Nil),
    Country("Ireland", "IE", isEu=true, List("Republic of Ireland", "Eire")),
    Country("Isle of Man", "IM", isEu=false, Nil),
    Country("Israel", "IL", isEu=false, Nil),
    Country("Italy", "IT", isEu=true, Nil),
    Country("Jamaica", "JM", isEu=false, Nil),
    Country("Japan", "JP", isEu=false, Nil),
    Country("Jersey", "JE", isEu=false, List("Channel Islands")),
    Country("Jordan", "JO", isEu=false, Nil),
    Country("Kazakhstan", "KZ", isEu=false, Nil),
    Country("Kenya", "KE", isEu=false, Nil),
    Country("Kiribati", "KI", isEu=false, Nil),
    Country("North Korea", "KP", isEu=false, Nil),
    Country("South Korea", "KR", isEu=false, Nil),
    Country("Kuwait", "KW", isEu=false, Nil),
    Country("Kyrgyzstan", "KG", isEu=false, Nil),
    Country("Lanzarote", "ES", isEu=false, List("Lanzarrote")),
    Country("La Palma", "ES", isEu=false, List("Las Palma")),
    Country("Lao People's Democratic Republic", "LA", isEu=false, List("Lao Peoples Democratic Republic")),
    Country("Latvia", "LV", isEu=true, Nil),
    Country("Lebanon", "LB", isEu=false, Nil),
    Country("Lesotho", "LS", isEu=false, Nil),
    Country("Liberia", "LR", isEu=false, Nil),
    Country("Libya", "LY", isEu=false, Nil),
    Country("Liechtenstein", "LI", isEu=false, Nil),
    Country("Lithuania", "LT", isEu=true, Nil),
    Country("Luxembourg", "LU", isEu=true, List("Luxemburg")),
    Country("Macao", "MO", isEu=false, Nil),
    Country("Macedonia", "MK", isEu=false, Nil),
    Country("Madagascar", "MG", isEu=false, Nil),
    Country("Malawi", "MW", isEu=false, Nil),
    Country("Malaysia", "MY", isEu=false, Nil),
    Country("Maldives", "MV", isEu=false, List("Maldive Islands")),
    Country("Mali", "ML", isEu=false, List("Mali Republic", "Republic of Mali")),
    Country("Malta", "MT", isEu=true, Nil),
    Country("Marshall Islands", "MH", isEu=false, Nil),
    Country("Martinique", "MQ", isEu=false, Nil),
    Country("Mauritania", "MR", isEu=false, Nil),
    Country("Mauritius", "MU", isEu=false, Nil),
    Country("Mayotte", "YT", isEu=false, Nil),
    Country("Mexico", "MX", isEu=false, Nil),
    Country("Micronesia", "FM", isEu=false, Nil),
    Country("Moldova", "MD", isEu=false, Nil),
    Country("Monaco", "MC", isEu=false, Nil),
    Country("Mongolia", "MN", isEu=false, Nil),
    Country("Montenegro", "ME", isEu=false, Nil),
    Country("Montserrat", "MS", isEu=false, Nil),
    Country("Morocco", "MA", isEu=false, Nil),
    Country("Mozambique", "MZ", isEu=false, Nil),
    Country("Myanmar", "MM", isEu=false, Nil),
    Country("Namibia", "NA", isEu=false, Nil),
    Country("Nauru", "NR", isEu=false, Nil),
    Country("Nepal", "NP", isEu=false, Nil),
    Country("Netherlands", "NL", isEu=true, List("Holland", "Dutch", "Amsterdam")),
    Country("New Caledonia", "NC", isEu=false, Nil),
    Country("New Zealand", "NZ", isEu=false, Nil),
    Country("Nicaragua", "NI", isEu=false, Nil),
    Country("Niger", "NE", isEu=false, List("Republic of the Niger", "Niger Republic")),
    Country("Nigeria", "NG", isEu=false, Nil),
    Country("Niue", "NU", isEu=false, Nil),
    Country("Norfolk Island", "NF", isEu=false, Nil),
    Country("Northern Mariana Islands", "MP", isEu=false, Nil),
    Country("Norway", "NO", isEu=false, Nil),
    Country("Oman", "OM", isEu=false, Nil),
    Country("Pakistan", "PK", isEu=false, Nil),
    Country("Palau", "PW", isEu=false, Nil),
    Country("Palestine, State of", "PS", isEu=false, Nil),
    Country("Panama", "PA", isEu=false, Nil),
    Country("Papua New Guinea", "PG", isEu=false, Nil),
    Country("Paraguay", "PY", isEu=false, Nil),
    Country("Peru", "PE", isEu=false, Nil),
    Country("Philippines", "PH", isEu=false, List("Philippenes", "Phillipines", "Phillippines", "Philipines")),
    Country("Pitcairn", "PN", isEu=false, Nil),
    Country("Poland", "PL", isEu=true, Nil),
    Country("Portugal", "PT", isEu=true, Nil),
    Country("Puerto Rico", "PR", isEu=false, Nil),
    Country("Qatar", "QA", isEu=false, Nil),
    Country("Réunion", "RE", isEu=false, List("Reunion")),
    Country("Romania", "RO", isEu=true, Nil),
    Country("Russian Federation", "RU", isEu=false, List("USSR", "Soviet Union")),
    Country("Rwanda", "RW", isEu=false, Nil),
    Country("Saint Barthélemy", "BL", isEu=false, List("Barthélemy", "Barthelemy")),
    Country("Saint Helena, Ascension and Tristan da Cunha", "SH", isEu=false, List("St Helena")),
    Country("Saint Kitts and Nevis", "KN", isEu=false, List("St Kitts")),
    Country("Saint Lucia", "LC", isEu=false, List("St Lucia")),
    Country("Saint Martin (French part)", "MF", isEu=false, List("St Martin")),
    Country("Saint Pierre and Miquelon", "PM", isEu=false, List("St Pierre")),
    Country("Saint Vincent and the Grenadines", "VC", isEu=false, List("St Vincent")),
    Country("Samoa", "WS", isEu=false, List("Western Samoa")),
    Country("San Marino", "SM", isEu=false, Nil),
    Country("Sao Tome and Principe", "ST", isEu=false, Nil),
    Country("Saudi Arabia", "SA", isEu=false, Nil),
    Country("Senegal", "SN", isEu=false, Nil),
    Country("Serbia", "RS", isEu=false, Nil),
    Country("Seychelles", "SC", isEu=false, Nil),
    Country("Sierra Leone", "SL", isEu=false, Nil),
    Country("Singapore", "SG", isEu=false, Nil),
    Country("Sint Maarten (Dutch part)", "SX", isEu=false, Nil),
    Country("Slovakia", "SK", isEu=true, Nil),
    Country("Slovenia", "SI", isEu=true, Nil),
    Country("Solomon Islands", "SB", isEu=false, List("Solomons")),
    Country("Somalia", "SO", isEu=false, Nil),
    Country("Republic of South Africa", "ZA", isEu=false, List("RSA")),
    Country("South Georgia and the South Sandwich Islands", "GS", isEu=false, Nil),
    Country("South Sudan", "SS", isEu=false, Nil),
    Country("Spain", "ES", isEu=true, Nil),
    Country("Sri Lanka", "LK", isEu=false, Nil),
    Country("Sudan", "SD", isEu=false, Nil),
    Country("Suriname", "SR", isEu=false, Nil),
    Country("Svalbard and Jan Mayen", "SJ", isEu=false, Nil),
    Country("Sweden", "SE", isEu=true, Nil),
    Country("Switzerland", "CH", isEu=false, List("Swiss")),
    Country("Syrian Arab Republic", "SY", isEu=false, Nil),
    Country("Taiwan (Province of China)", "TW", isEu=false, Nil),
    Country("Tajikistan", "TJ", isEu=false, Nil),
    Country("Tanzania", "TZ", isEu=false, Nil),
    Country("Tenerife", "ES", isEu=false, List("Tennerife")),
    Country("Thailand", "TH", isEu=false, Nil),
    Country("Timor-Leste", "TL", isEu=false, Nil),
    Country("Togo", "TG", isEu=false, List("Togo Republic", "Togolese Republic", "Republic of Togo")),
    Country("Tokelau", "TK", isEu=false, Nil),
    Country("Tonga", "TO", isEu=false, Nil),
    Country("Trinidad and Tobago", "TT", isEu=false, Nil),
    Country("Tunisia", "TN", isEu=false, Nil),
    Country("Turkey", "TR", isEu=false, Nil),
    Country("Turkish Republic of Northern Cyprus", "CY", isEu=false, List("TRNC", "Northern Cyprus")),
    Country("Turkmenistan", "TM", isEu=false, Nil),
    Country("Turks and Caicos Islands", "TC", isEu=false, Nil),
    Country("Tuvalu", "TV", isEu=false, Nil),
    Country("Uganda", "UG", isEu=false, Nil),
    Country("Ukraine", "UA", isEu=false, Nil),
    Country("United Arab Emirates", "AE", isEu=false, List("UAE", "emirati", "dubai", "abu dahbi", "abu dhabi")),
    Country("United Kingdom of Great Britain and Northern Ireland (the)", "GB", isEu=true, List("England", "Scotland", "Wales", "Northern Ireland", "GB", "UK")),
    Country("United States Minor Outlying Islands", "UM", isEu=false, Nil),
    Country("United States of America", "US", isEu=false, List("USA", "US", "American")),
    Country("Uruguay", "UY", isEu=false, Nil),
    Country("Uzbekistan", "UZ", isEu=false, Nil),
    Country("Vanuatu", "VU", isEu=false, Nil),
    Country("Venezuela", "VE", isEu=false, Nil),
    Country("Viet Nam", "VN", isEu=false, Nil),
    Country("Virgin Islands (British)", "VG", isEu=false, Nil),
    Country("Virgin Islands (US)", "VI", isEu=false, Nil),
    Country("Wallis and Futuna", "WF", isEu=false, Nil),
    Country("Western Sahara", "EH", isEu=false, Nil),
    Country("Yemen", "YE", isEu=false, Nil),
    Country("Zambia", "ZM", isEu=false, Nil),
    Country("Zimbabwe", "ZW", isEu=false, Nil)
  )
}
