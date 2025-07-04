/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package services

import models.Country
import play.api.libs.json.{JsError, JsSuccess, Json}
import util.BaseSpec

class CountriesServiceSpec extends BaseSpec {

  val expectedCountries: List[Country] = List(
    Country("AF", "title.afghanistan", "AF", isEu = false, isCountry = true, Nil),
    Country("AX", "title.aland_islands", "AX", isEu = false, isCountry = true, List("Aland Islands")),
    Country("AL", "title.albania", "AL", isEu = false, isCountry = true, Nil),
    Country("DZ", "title.algeria", "DZ", isEu = false, isCountry = true, Nil),
    Country("AS", "title.american_samoa", "AS", isEu = false, isCountry = true, Nil),
    Country("AD", "title.andorra", "AD", isEu = false, isCountry = true, Nil),
    Country("AO", "title.angola", "AO", isEu = false, isCountry = true, Nil),
    Country("AI", "title.anguilla", "AI", isEu = false, isCountry = true, Nil),
    Country("AQ", "title.antarctica", "AQ", isEu = false, isCountry = true, List("Antartica")),
    Country("AG", "title.antigua_and_barbuda", "AG", isEu = false, isCountry = true, Nil),
    Country("AR", "title.argentina", "AR", isEu = false, isCountry = true, Nil),
    Country("AM", "title.armenia", "AM", isEu = false, isCountry = true, Nil),
    Country("AW", "title.aruba", "AW", isEu = false, isCountry = true, Nil),
    Country("AU", "title.australia", "AU", isEu = false, isCountry = true, List("Oz")),
    Country("AT", "title.austria", "AT", isEu = true, isCountry = true, Nil),
    Country("AZ", "title.azerbaijan", "AZ", isEu = false, isCountry = true, Nil),
    Country("BS", "title.bahamas", "BS", isEu = false, isCountry = true, Nil),
    Country("BH", "title.bahrain", "BH", isEu = false, isCountry = true, Nil),
    Country("BD", "title.bangladesh", "BD", isEu = false, isCountry = true, Nil),
    Country("BB", "title.barbados", "BB", isEu = false, isCountry = true, Nil),
    Country("BY", "title.belarus", "BY", isEu = false, isCountry = true, Nil),
    Country("BE", "title.belgium", "BE", isEu = true, isCountry = true, Nil),
    Country("BZ", "title.belize", "BZ", isEu = false, isCountry = true, Nil),
    Country("BJ", "title.benin", "BJ", isEu = false, isCountry = true, Nil),
    Country("BM", "title.bermuda", "BM", isEu = false, isCountry = true, Nil),
    Country("BT", "title.bhutan", "BT", isEu = false, isCountry = true, Nil),
    Country("BO", "title.bolivia", "BO", isEu = false, isCountry = true, Nil),
    Country("BQ", "title.bonaire_sint_eustatius_and_saba", "BQ", isEu = false, isCountry = true, Nil),
    Country("BA", "title.bosnia_and_herzegovina", "BA", isEu = false, isCountry = true, Nil),
    Country("BW", "title.botswana", "BW", isEu = false, isCountry = true, Nil),
    Country("BV", "title.bouvet_island", "BV", isEu = false, isCountry = true, Nil),
    Country("BR", "title.brazil", "BR", isEu = false, isCountry = true, Nil),
    Country("IO", "title.british_indian_ocean_territory", "IO", isEu = false, isCountry = true, List("BIOT")),
    Country("BN", "title.brunei", "BN", isEu = false, isCountry = true, Nil),
    Country("BG", "title.bulgaria", "BG", isEu = true, isCountry = true, Nil),
    Country("BF", "title.burkina_faso", "BF", isEu = false, isCountry = true, Nil),
    Country("BI", "title.burundi", "BI", isEu = false, isCountry = true, Nil),
    Country("CV", "title.cape_verde", "CV", isEu = false, isCountry = true, List("Republic of Cabo Verde")),
    Country("KH", "title.cambodia", "KH", isEu = false, isCountry = true, Nil),
    Country("CM", "title.cameroon", "CM", isEu = false, isCountry = true, Nil),
    Country("CA", "title.canada", "CA", isEu = false, isCountry = true, Nil),
    Country("ES2", "title.canary_islands", "ES", isEu = false, isCountry = true, List("Canaries")),
    Country("KY", "title.cayman_islands", "KY", isEu = false, isCountry = true, List("Caymans")),
    Country("CF", "title.central_african_republic", "CF", isEu = false, isCountry = true, List("CAR")),
    Country("TD", "title.chad", "TD", isEu = false, isCountry = true, Nil),
    Country("CL", "title.chile", "CL", isEu = false, isCountry = true, Nil),
    Country(
      "CN",
      "title.china",
      "CN",
      isEu = false,
      isCountry = true,
      List("People's Republic of China", "PRC", "Peoples Republic of China")
    ),
    Country("CX", "title.christmas_island", "CX", isEu = false, isCountry = true, List("Christmas Islands")),
    Country("CC", "title.cocos_keeling_islands", "CC", isEu = false, isCountry = true, Nil),
    Country("CO", "title.colombia", "CO", isEu = false, isCountry = true, List("Columbia")),
    Country("KM", "title.comoros", "KM", isEu = false, isCountry = true, Nil),
    Country(
      "CD",
      "title.democratic_republic_of_the_congo",
      "CD",
      isEu = false,
      isCountry = true,
      List("DR Congo", "DRC")
    ),
    Country(
      "CG",
      "title.republic_of_the_congo",
      "CG",
      isEu = false,
      isCountry = true,
      List("Congo-Brazzaville", "Congo Republic")
    ),
    Country("CK", "title.cook_islands", "CK", isEu = false, isCountry = true, Nil),
    Country("CR", "title.costa_rica", "CR", isEu = false, isCountry = true, Nil),
    Country(
      "CI",
      "title.cote_divoire",
      "CI",
      isEu = false,
      isCountry = true,
      List("Ivory Coast", "Cote d'Ivoire", "Cote dIvoire", "Cote d Ivoire")
    ),
    Country("HR", "title.croatia", "HR", isEu = true, isCountry = true, Nil),
    Country("CU", "title.cuba", "CU", isEu = false, isCountry = true, Nil),
    Country("CW", "title.curacao", "CW", isEu = false, isCountry = true, List("Curacao", "Curacoa")),
    Country("CY0", "title.cyprus", "CY", isEu = true, isCountry = true, Nil),
    Country("CZ", "title.czech_republic", "CZ", isEu = true, isCountry = true, List("Czechoslovakia", "Czechia")),
    Country("DK", "title.denmark", "DK", isEu = true, isCountry = true, List("Danish")),
    Country("DJ", "title.djibouti", "DJ", isEu = false, isCountry = true, Nil),
    Country("DM", "title.dominica", "DM", isEu = false, isCountry = true, Nil),
    Country("DO", "title.dominican_republic", "DO", isEu = false, isCountry = true, Nil),
    Country("EC", "title.ecuador", "EC", isEu = false, isCountry = true, Nil),
    Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil),
    Country("SV", "title.el_salvador", "SV", isEu = false, isCountry = true, Nil),
    Country("GQ", "title.equatorial_guinea", "GQ", isEu = false, isCountry = true, Nil),
    Country("ER", "title.eritrea", "ER", isEu = false, isCountry = true, Nil),
    Country("EE", "title.estonia", "EE", isEu = true, isCountry = true, Nil),
    Country("ET", "title.ethiopia", "ET", isEu = false, isCountry = true, Nil),
    Country("SZ", "title.eswatini", "SZ", isEu = false, isCountry = true, List("Swaziland")),
    Country("FK", "title.falkland_islands", "FK", isEu = false, isCountry = true, List("Falklands")),
    Country("FO", "title.faroe_islands", "FO", isEu = false, isCountry = true, List("Faroes")),
    Country("FJ", "title.fiji", "FJ", isEu = false, isCountry = true, Nil),
    Country("FI", "title.finland", "FI", isEu = true, isCountry = true, Nil),
    Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil),
    Country("GF", "title.french_guiana", "GF", isEu = false, isCountry = true, List("French Guayana")),
    Country("PF", "title.french_polynesia", "PF", isEu = false, isCountry = true, List("Polynesian Islands")),
    Country("TF", "title.french_southern_territories", "TF", isEu = false, isCountry = true, Nil),
    Country("ES3", "title.fuerteventura", "ES", isEu = false, isCountry = true, Nil),
    Country("GA", "title.gabon", "GA", isEu = false, isCountry = true, Nil),
    Country("GM", "title.gambia", "GM", isEu = false, isCountry = true, Nil),
    Country("GE", "title.georgia", "GE", isEu = false, isCountry = true, Nil),
    Country("DE", "title.germany", "DE", isEu = true, isCountry = true, List("Deutschland")),
    Country("GH", "title.ghana", "GH", isEu = false, isCountry = true, Nil),
    Country("GI", "title.gibraltar", "GI", isEu = false, isCountry = true, List("Gibraltar Rock")),
    Country("ES4", "title.gran_canaria", "ES", isEu = false, isCountry = true, List("Grand Canaria")),
    Country("GR", "title.greece", "GR", isEu = true, isCountry = true, Nil),
    Country("GL", "title.greenland", "GL", isEu = false, isCountry = true, Nil),
    Country("GD", "title.grenada", "GD", isEu = false, isCountry = true, Nil),
    Country("GP", "title.guadeloupe", "GP", isEu = false, isCountry = true, Nil),
    Country("GU", "title.guam", "GU", isEu = false, isCountry = true, Nil),
    Country("GT", "title.guatemala", "GT", isEu = false, isCountry = true, Nil),
    Country("GG", "title.guernsey", "GG", isEu = false, isCountry = true, List("Channel Islands")),
    Country("GN", "title.guinea", "GN", isEu = false, isCountry = true, Nil),
    Country("GW", "title.guinea_bissau", "GW", isEu = false, isCountry = true, Nil),
    Country("GY", "title.guyana", "GY", isEu = false, isCountry = true, Nil),
    Country("HT", "title.haiti", "HT", isEu = false, isCountry = true, Nil),
    Country("HM", "title.heard_island_and_mcdonald_islands", "HM", isEu = false, isCountry = true, Nil),
    Country("VA", "title.holy_see", "VA", isEu = false, isCountry = true, List("Vatican City")),
    Country("HN", "title.honduras", "HN", isEu = false, isCountry = true, Nil),
    Country("HK", "title.hong_kong", "HK", isEu = false, isCountry = true, Nil),
    Country("HU", "title.hungary", "HU", isEu = true, isCountry = true, Nil),
    Country("IS", "title.iceland", "IS", isEu = false, isCountry = true, Nil),
    Country("IN", "title.india", "IN", isEu = false, isCountry = true, Nil),
    Country("ID", "title.indonesia", "ID", isEu = false, isCountry = true, Nil),
    Country("IR", "title.iran", "IR", isEu = false, isCountry = true, Nil),
    Country("IQ", "title.iraq", "IQ", isEu = false, isCountry = true, Nil),
    Country("IE", "title.ireland", "IE", isEu = true, isCountry = true, List("Republic of Ireland", "Eire")),
    Country("IM", "title.isle_of_man", "IM", isEu = false, isCountry = true, Nil),
    Country("IL", "title.israel", "IL", isEu = false, isCountry = true, Nil),
    Country("IT", "title.italy", "IT", isEu = true, isCountry = true, Nil),
    Country("JM", "title.jamaica", "JM", isEu = false, isCountry = true, Nil),
    Country("JP", "title.japan", "JP", isEu = false, isCountry = true, Nil),
    Country("JE", "title.jersey", "JE", isEu = false, isCountry = true, List("Channel Islands")),
    Country("JO", "title.jordan", "JO", isEu = false, isCountry = true, Nil),
    Country("KZ", "title.kazakhstan", "KZ", isEu = false, isCountry = true, Nil),
    Country("KE", "title.kenya", "KE", isEu = false, isCountry = true, Nil),
    Country("KI", "title.kiribati", "KI", isEu = false, isCountry = true, Nil),
    Country("KP", "title.north_korea", "KP", isEu = false, isCountry = true, Nil),
    Country("KR", "title.south_korea", "KR", isEu = false, isCountry = true, Nil),
    Country("KO", "title.kosovo", "KO", isEu = false, isCountry = true, Nil),
    Country("KW", "title.kuwait", "KW", isEu = false, isCountry = true, Nil),
    Country("KG", "title.kyrgyzstan", "KG", isEu = false, isCountry = true, Nil),
    Country("ES5", "title.lanzarote", "ES", isEu = false, isCountry = true, List("Lanzarrote")),
    Country("ES6", "title.la_palma", "ES", isEu = false, isCountry = true, List("Las Palma")),
    Country("LA", "title.laos", "LA", isEu = false, isCountry = true, List("Lao Peoples Democratic Republic")),
    Country("LV", "title.latvia", "LV", isEu = true, isCountry = true, Nil),
    Country("LB", "title.lebanon", "LB", isEu = false, isCountry = true, Nil),
    Country("LS", "title.lesotho", "LS", isEu = false, isCountry = true, Nil),
    Country("LR", "title.liberia", "LR", isEu = false, isCountry = true, Nil),
    Country("LY", "title.libya", "LY", isEu = false, isCountry = true, Nil),
    Country("LI", "title.liechtenstein", "LI", isEu = false, isCountry = true, Nil),
    Country("LT", "title.lithuania", "LT", isEu = true, isCountry = true, Nil),
    Country("LU", "title.luxembourg", "LU", isEu = true, isCountry = true, List("Luxemburg")),
    Country("MO", "title.macao", "MO", isEu = false, isCountry = true, Nil),
    Country("MK", "title.north_macedonia", "MK", isEu = false, isCountry = true, Nil),
    Country("MG", "title.madagascar", "MG", isEu = false, isCountry = true, Nil),
    Country("MW", "title.malawi", "MW", isEu = false, isCountry = true, Nil),
    Country("MY", "title.malaysia", "MY", isEu = false, isCountry = true, Nil),
    Country("MV", "title.maldives", "MV", isEu = false, isCountry = true, List("Maldive Islands")),
    Country("ML", "title.mali", "ML", isEu = false, isCountry = true, List("Mali Republic", "Republic of Mali")),
    Country("MT", "title.malta", "MT", isEu = true, isCountry = true, Nil),
    Country("MH", "title.marshall_islands", "MH", isEu = false, isCountry = true, Nil),
    Country("MQ", "title.martinique", "MQ", isEu = false, isCountry = true, Nil),
    Country("MR", "title.mauritania", "MR", isEu = false, isCountry = true, Nil),
    Country("MU", "title.mauritius", "MU", isEu = false, isCountry = true, Nil),
    Country("YT", "title.mayotte", "YT", isEu = false, isCountry = true, Nil),
    Country("MX", "title.mexico", "MX", isEu = false, isCountry = true, Nil),
    Country("FM", "title.micronesia", "FM", isEu = false, isCountry = true, Nil),
    Country("MD", "title.moldova", "MD", isEu = false, isCountry = true, Nil),
    Country("MC", "title.monaco", "MC", isEu = false, isCountry = true, Nil),
    Country("MN", "title.mongolia", "MN", isEu = false, isCountry = true, Nil),
    Country("ME", "title.montenegro", "ME", isEu = false, isCountry = true, Nil),
    Country("MS", "title.montserrat", "MS", isEu = false, isCountry = true, Nil),
    Country("MA", "title.morocco", "MA", isEu = false, isCountry = true, Nil),
    Country("MZ", "title.mozambique", "MZ", isEu = false, isCountry = true, Nil),
    Country("MM", "title.myanmar", "MM", isEu = false, isCountry = true, List("Burma")),
    Country("NA", "title.namibia", "NA", isEu = false, isCountry = true, Nil),
    Country("NR", "title.nauru", "NR", isEu = false, isCountry = true, Nil),
    Country("NP", "title.nepal", "NP", isEu = false, isCountry = true, Nil),
    Country("NL", "title.netherlands", "NL", isEu = true, isCountry = true, List("Holland", "Dutch", "Amsterdam")),
    Country("NC", "title.new_caledonia", "NC", isEu = false, isCountry = true, Nil),
    Country("NZ", "title.new_zealand", "NZ", isEu = false, isCountry = true, Nil),
    Country("NI", "title.nicaragua", "NI", isEu = false, isCountry = true, Nil),
    Country("NE", "title.niger", "NE", isEu = false, isCountry = true, List("Republic of the Niger", "Niger Republic")),
    Country("NG", "title.nigeria", "NG", isEu = false, isCountry = true, Nil),
    Country("NU", "title.niue", "NU", isEu = false, isCountry = true, Nil),
    Country("NF", "title.norfolk_island", "NF", isEu = false, isCountry = true, Nil),
    Country("MP", "title.northern_mariana_islands", "MP", isEu = false, isCountry = true, Nil),
    Country("NO", "title.norway", "NO", isEu = false, isCountry = true, Nil),
    Country("OM", "title.oman", "OM", isEu = false, isCountry = true, Nil),
    Country("PK", "title.pakistan", "PK", isEu = false, isCountry = true, Nil),
    Country("PW", "title.palau", "PW", isEu = false, isCountry = true, Nil),
    Country("PS", "title.palestine_state_of", "PS", isEu = false, isCountry = true, Nil),
    Country("PA", "title.panama", "PA", isEu = false, isCountry = true, Nil),
    Country("PG", "title.papua_new_guinea", "PG", isEu = false, isCountry = true, Nil),
    Country("PY", "title.paraguay", "PY", isEu = false, isCountry = true, Nil),
    Country("PE", "title.peru", "PE", isEu = false, isCountry = true, Nil),
    Country(
      "PH",
      "title.philippines",
      "PH",
      isEu = false,
      isCountry = true,
      List("Philippenes", "Phillipines", "Phillippines", "Philipines")
    ),
    Country("PN", "title.pitcairn", "PN", isEu = false, isCountry = true, Nil),
    Country("PL", "title.poland", "PL", isEu = true, isCountry = true, Nil),
    Country("PT", "title.portugal", "PT", isEu = true, isCountry = true, Nil),
    Country("PR", "title.puerto_rico", "PR", isEu = false, isCountry = true, Nil),
    Country("QA", "title.qatar", "QA", isEu = false, isCountry = true, Nil),
    Country("RE", "title.reunion", "RE", isEu = false, isCountry = true, List("Reunion")),
    Country("RO", "title.romania", "RO", isEu = true, isCountry = true, Nil),
    Country("RU", "title.russia", "RU", isEu = false, isCountry = true, List("USSR", "Soviet Union")),
    Country("RW", "title.rwanda", "RW", isEu = false, isCountry = true, Nil),
    Country("BL", "title.saint_barthelemy", "BL", isEu = false, isCountry = true, List("Barthélemy", "Barthelemy")),
    Country(
      "SH",
      "title.saint_helena_ascension_and_tristan_da_cunha",
      "SH",
      isEu = false,
      isCountry = true,
      List("St Helena")
    ),
    Country("KN", "title.saint_kitts_and_nevis", "KN", isEu = false, isCountry = true, List("St Kitts")),
    Country("LC", "title.saint_lucia", "LC", isEu = false, isCountry = true, List("St Lucia")),
    Country("MF", "title.saint_martin_french_part", "MF", isEu = false, isCountry = true, List("St Martin")),
    Country("PM", "title.saint_pierre_and_miquelon", "PM", isEu = false, isCountry = true, List("St Pierre")),
    Country("VC", "title.saint_vincent_and_the_grenadines", "VC", isEu = false, isCountry = true, List("St Vincent")),
    Country("WS", "title.samoa", "WS", isEu = false, isCountry = true, List("Western Samoa")),
    Country("SM", "title.san_marino", "SM", isEu = false, isCountry = true, Nil),
    Country("ST", "title.sao_tome_and_principe", "ST", isEu = false, isCountry = true, Nil),
    Country("SA", "title.saudi_arabia", "SA", isEu = false, isCountry = true, Nil),
    Country("SN", "title.senegal", "SN", isEu = false, isCountry = true, Nil),
    Country("RS", "title.serbia", "RS", isEu = false, isCountry = true, Nil),
    Country("SC", "title.seychelles", "SC", isEu = false, isCountry = true, Nil),
    Country("SL", "title.sierra_leone", "SL", isEu = false, isCountry = true, Nil),
    Country("SG", "title.singapore", "SG", isEu = false, isCountry = true, Nil),
    Country("SX", "title.sint_maarten_dutch_part", "SX", isEu = false, isCountry = true, Nil),
    Country("SK", "title.slovakia", "SK", isEu = true, isCountry = true, Nil),
    Country("SI", "title.slovenia", "SI", isEu = true, isCountry = true, Nil),
    Country("SB", "title.solomon_islands", "SB", isEu = false, isCountry = true, List("Solomons")),
    Country("SO", "title.somalia", "SO", isEu = false, isCountry = true, Nil),
    Country("ZA", "title.south_africa", "ZA", isEu = false, isCountry = true, List("RSA")),
    Country("GS", "title.south_georgia_and_the_south_sandwich_islands", "GS", isEu = false, isCountry = true, Nil),
    Country("SS", "title.south_sudan", "SS", isEu = false, isCountry = true, Nil),
    Country("ES0", "title.spain", "ES", isEu = true, isCountry = true, Nil),
    Country("LK", "title.sri_lanka", "LK", isEu = false, isCountry = true, Nil),
    Country("SD", "title.sudan", "SD", isEu = false, isCountry = true, Nil),
    Country("SR", "title.suriname", "SR", isEu = false, isCountry = true, Nil),
    Country("SJ", "title.svalbard_and_jan_mayen", "SJ", isEu = false, isCountry = true, Nil),
    Country("SE", "title.sweden", "SE", isEu = true, isCountry = true, Nil),
    Country("CH", "title.switzerland", "CH", isEu = false, isCountry = true, List("Swiss")),
    Country("SY", "title.syria", "SY", isEu = false, isCountry = true, Nil),
    Country("TW", "title.taiwan", "TW", isEu = false, isCountry = true, Nil),
    Country("TJ", "title.tajikistan", "TJ", isEu = false, isCountry = true, Nil),
    Country("TZ", "title.tanzania", "TZ", isEu = false, isCountry = true, Nil),
    Country("ES1", "title.tenerife", "ES", isEu = false, isCountry = true, List("Tennerife")),
    Country("TH", "title.thailand", "TH", isEu = false, isCountry = true, Nil),
    Country("TL", "title.timor_leste", "TL", isEu = false, isCountry = true, Nil),
    Country(
      "TG",
      "title.togo",
      "TG",
      isEu = false,
      isCountry = true,
      List("Togo Republic", "Togolese Republic", "Republic of Togo")
    ),
    Country("TK", "title.tokelau", "TK", isEu = false, isCountry = true, Nil),
    Country("TO", "title.tonga", "TO", isEu = false, isCountry = true, Nil),
    Country("TT", "title.trinidad_and_tobago", "TT", isEu = false, isCountry = true, Nil),
    Country("TN", "title.tunisia", "TN", isEu = false, isCountry = true, Nil),
    Country("TR", "title.turkey", "TR", isEu = false, isCountry = true, Nil),
    Country("TM", "title.turkmenistan", "TM", isEu = false, isCountry = true, Nil),
    Country("TC", "title.turks_and_caicos_islands", "TC", isEu = false, isCountry = true, Nil),
    Country("TV", "title.tuvalu", "TV", isEu = false, isCountry = true, Nil),
    Country("UG", "title.uganda", "UG", isEu = false, isCountry = true, Nil),
    Country("UA", "title.ukraine", "UA", isEu = false, isCountry = true, Nil),
    Country(
      "AE",
      "title.united_arab_emirates",
      "AE",
      isEu = false,
      isCountry = true,
      List("UAE", "emirati", "dubai", "abu dahbi", "abu dhabi")
    ),
    Country(
      "GB",
      "title.united_kingdom",
      "GB",
      isEu = false,
      isCountry = true,
      List("England", "Scotland", "Wales", "Northern Ireland", "GB", "UK")
    ),
    Country("UM", "title.united_states_minor_outlying_islands", "UM", isEu = false, isCountry = true, Nil),
    Country(
      "US",
      "title.united_states_of_america",
      "US",
      isEu = false,
      isCountry = true,
      List("USA", "US", "American")
    ),
    Country("UY", "title.uruguay", "UY", isEu = false, isCountry = true, Nil),
    Country("UZ", "title.uzbekistan", "UZ", isEu = false, isCountry = true, Nil),
    Country("VU", "title.vanuatu", "VU", isEu = false, isCountry = true, Nil),
    Country("VE", "title.venezuela", "VE", isEu = false, isCountry = true, Nil),
    Country("VN", "title.vietnam", "VN", isEu = false, isCountry = true, Nil),
    Country("VG", "title.virgin_islands_british", "VG", isEu = false, isCountry = true, Nil),
    Country("VI", "title.virgin_islands_us", "VI", isEu = false, isCountry = true, Nil),
    Country("WF", "title.wallis_and_futuna", "WF", isEu = false, isCountry = true, Nil),
    Country("EH", "title.western_sahara", "EH", isEu = false, isCountry = true, Nil),
    Country("YE", "title.yemen", "YE", isEu = false, isCountry = true, Nil),
    Country("ZM", "title.zambia", "ZM", isEu = false, isCountry = true, Nil),
    Country("ZW", "title.zimbabwe", "ZW", isEu = false, isCountry = true, Nil)
  )

  val expectedCountriesAndEu: List[Country] = List(
    Country("AF", "title.afghanistan", "AF", isEu = false, isCountry = true, Nil),
    Country("AX", "title.aland_islands", "AX", isEu = false, isCountry = true, List("Aland Islands")),
    Country("AL", "title.albania", "AL", isEu = false, isCountry = true, Nil),
    Country("DZ", "title.algeria", "DZ", isEu = false, isCountry = true, Nil),
    Country("AS", "title.american_samoa", "AS", isEu = false, isCountry = true, Nil),
    Country("AD", "title.andorra", "AD", isEu = false, isCountry = true, Nil),
    Country("AO", "title.angola", "AO", isEu = false, isCountry = true, Nil),
    Country("AI", "title.anguilla", "AI", isEu = false, isCountry = true, Nil),
    Country("AQ", "title.antarctica", "AQ", isEu = false, isCountry = true, List("Antartica")),
    Country("AG", "title.antigua_and_barbuda", "AG", isEu = false, isCountry = true, Nil),
    Country("AR", "title.argentina", "AR", isEu = false, isCountry = true, Nil),
    Country("AM", "title.armenia", "AM", isEu = false, isCountry = true, Nil),
    Country("AW", "title.aruba", "AW", isEu = false, isCountry = true, Nil),
    Country("AU", "title.australia", "AU", isEu = false, isCountry = true, List("Oz")),
    Country("AT", "title.austria", "AT", isEu = true, isCountry = true, Nil),
    Country("AZ", "title.azerbaijan", "AZ", isEu = false, isCountry = true, Nil),
    Country("BS", "title.bahamas", "BS", isEu = false, isCountry = true, Nil),
    Country("BH", "title.bahrain", "BH", isEu = false, isCountry = true, Nil),
    Country("BD", "title.bangladesh", "BD", isEu = false, isCountry = true, Nil),
    Country("BB", "title.barbados", "BB", isEu = false, isCountry = true, Nil),
    Country("BY", "title.belarus", "BY", isEu = false, isCountry = true, Nil),
    Country("BE", "title.belgium", "BE", isEu = true, isCountry = true, Nil),
    Country("BZ", "title.belize", "BZ", isEu = false, isCountry = true, Nil),
    Country("BJ", "title.benin", "BJ", isEu = false, isCountry = true, Nil),
    Country("BM", "title.bermuda", "BM", isEu = false, isCountry = true, Nil),
    Country("BT", "title.bhutan", "BT", isEu = false, isCountry = true, Nil),
    Country("BO", "title.bolivia", "BO", isEu = false, isCountry = true, Nil),
    Country("BQ", "title.bonaire_sint_eustatius_and_saba", "BQ", isEu = false, isCountry = true, Nil),
    Country("BA", "title.bosnia_and_herzegovina", "BA", isEu = false, isCountry = true, Nil),
    Country("BW", "title.botswana", "BW", isEu = false, isCountry = true, Nil),
    Country("BV", "title.bouvet_island", "BV", isEu = false, isCountry = true, Nil),
    Country("BR", "title.brazil", "BR", isEu = false, isCountry = true, Nil),
    Country("IO", "title.british_indian_ocean_territory", "IO", isEu = false, isCountry = true, List("BIOT")),
    Country("BN", "title.brunei", "BN", isEu = false, isCountry = true, Nil),
    Country("BG", "title.bulgaria", "BG", isEu = true, isCountry = true, Nil),
    Country("BF", "title.burkina_faso", "BF", isEu = false, isCountry = true, Nil),
    Country("BI", "title.burundi", "BI", isEu = false, isCountry = true, Nil),
    Country("CV", "title.cape_verde", "CV", isEu = false, isCountry = true, List("Republic of Cabo Verde")),
    Country("KH", "title.cambodia", "KH", isEu = false, isCountry = true, Nil),
    Country("CM", "title.cameroon", "CM", isEu = false, isCountry = true, Nil),
    Country("CA", "title.canada", "CA", isEu = false, isCountry = true, Nil),
    Country("ES2", "title.canary_islands", "ES", isEu = false, isCountry = true, List("Canaries")),
    Country("KY", "title.cayman_islands", "KY", isEu = false, isCountry = true, List("Caymans")),
    Country("CF", "title.central_african_republic", "CF", isEu = false, isCountry = true, List("CAR")),
    Country("TD", "title.chad", "TD", isEu = false, isCountry = true, Nil),
    Country("CL", "title.chile", "CL", isEu = false, isCountry = true, Nil),
    Country(
      "CN",
      "title.china",
      "CN",
      isEu = false,
      isCountry = true,
      List("People's Republic of China", "PRC", "Peoples Republic of China")
    ),
    Country("CX", "title.christmas_island", "CX", isEu = false, isCountry = true, List("Christmas Islands")),
    Country("CC", "title.cocos_keeling_islands", "CC", isEu = false, isCountry = true, Nil),
    Country("CO", "title.colombia", "CO", isEu = false, isCountry = true, List("Columbia")),
    Country("KM", "title.comoros", "KM", isEu = false, isCountry = true, Nil),
    Country(
      "CD",
      "title.democratic_republic_of_the_congo",
      "CD",
      isEu = false,
      isCountry = true,
      List("DR Congo", "DRC")
    ),
    Country(
      "CG",
      "title.republic_of_the_congo",
      "CG",
      isEu = false,
      isCountry = true,
      List("Congo-Brazzaville", "Congo Republic")
    ),
    Country("CK", "title.cook_islands", "CK", isEu = false, isCountry = true, Nil),
    Country("CR", "title.costa_rica", "CR", isEu = false, isCountry = true, Nil),
    Country(
      "CI",
      "title.cote_divoire",
      "CI",
      isEu = false,
      isCountry = true,
      List("Ivory Coast", "Cote d'Ivoire", "Cote dIvoire", "Cote d Ivoire")
    ),
    Country("HR", "title.croatia", "HR", isEu = true, isCountry = true, Nil),
    Country("CU", "title.cuba", "CU", isEu = false, isCountry = true, Nil),
    Country("CW", "title.curacao", "CW", isEu = false, isCountry = true, List("Curacao", "Curacoa")),
    Country("CY0", "title.cyprus", "CY", isEu = true, isCountry = true, Nil),
    Country("CZ", "title.czech_republic", "CZ", isEu = true, isCountry = true, List("Czechoslovakia", "Czechia")),
    Country("DK", "title.denmark", "DK", isEu = true, isCountry = true, List("Danish")),
    Country("DJ", "title.djibouti", "DJ", isEu = false, isCountry = true, Nil),
    Country("DM", "title.dominica", "DM", isEu = false, isCountry = true, Nil),
    Country("DO", "title.dominican_republic", "DO", isEu = false, isCountry = true, Nil),
    Country("EC", "title.ecuador", "EC", isEu = false, isCountry = true, Nil),
    Country("EG", "title.egypt", "EG", isEu = false, isCountry = true, Nil),
    Country("SV", "title.el_salvador", "SV", isEu = false, isCountry = true, Nil),
    Country("GQ", "title.equatorial_guinea", "GQ", isEu = false, isCountry = true, Nil),
    Country("ER", "title.eritrea", "ER", isEu = false, isCountry = true, Nil),
    Country("EE", "title.estonia", "EE", isEu = true, isCountry = true, Nil),
    Country("ET", "title.ethiopia", "ET", isEu = false, isCountry = true, Nil),
    Country("SZ", "title.eswatini", "SZ", isEu = false, isCountry = true, List("Swaziland")),
    Country("FK", "title.falkland_islands", "FK", isEu = false, isCountry = true, List("Falklands")),
    Country("FO", "title.faroe_islands", "FO", isEu = false, isCountry = true, List("Faroes")),
    Country("FJ", "title.fiji", "FJ", isEu = false, isCountry = true, Nil),
    Country("FI", "title.finland", "FI", isEu = true, isCountry = true, Nil),
    Country("FR", "title.france", "FR", isEu = true, isCountry = true, Nil),
    Country("GF", "title.french_guiana", "GF", isEu = false, isCountry = true, List("French Guayana")),
    Country("PF", "title.french_polynesia", "PF", isEu = false, isCountry = true, List("Polynesian Islands")),
    Country("TF", "title.french_southern_territories", "TF", isEu = false, isCountry = true, Nil),
    Country("ES3", "title.fuerteventura", "ES", isEu = false, isCountry = true, Nil),
    Country("GA", "title.gabon", "GA", isEu = false, isCountry = true, Nil),
    Country("GM", "title.gambia", "GM", isEu = false, isCountry = true, Nil),
    Country("GE", "title.georgia", "GE", isEu = false, isCountry = true, Nil),
    Country("DE", "title.germany", "DE", isEu = true, isCountry = true, List("Deutschland")),
    Country("GH", "title.ghana", "GH", isEu = false, isCountry = true, Nil),
    Country("GI", "title.gibraltar", "GI", isEu = false, isCountry = true, List("Gibraltar Rock")),
    Country("ES4", "title.gran_canaria", "ES", isEu = false, isCountry = true, List("Grand Canaria")),
    Country("GR", "title.greece", "GR", isEu = true, isCountry = true, Nil),
    Country("GL", "title.greenland", "GL", isEu = false, isCountry = true, Nil),
    Country("GD", "title.grenada", "GD", isEu = false, isCountry = true, Nil),
    Country("GP", "title.guadeloupe", "GP", isEu = false, isCountry = true, Nil),
    Country("GU", "title.guam", "GU", isEu = false, isCountry = true, Nil),
    Country("GT", "title.guatemala", "GT", isEu = false, isCountry = true, Nil),
    Country("GG", "title.guernsey", "GG", isEu = false, isCountry = true, List("Channel Islands")),
    Country("GN", "title.guinea", "GN", isEu = false, isCountry = true, Nil),
    Country("GW", "title.guinea_bissau", "GW", isEu = false, isCountry = true, Nil),
    Country("GY", "title.guyana", "GY", isEu = false, isCountry = true, Nil),
    Country("HT", "title.haiti", "HT", isEu = false, isCountry = true, Nil),
    Country("HM", "title.heard_island_and_mcdonald_islands", "HM", isEu = false, isCountry = true, Nil),
    Country("VA", "title.holy_see", "VA", isEu = false, isCountry = true, List("Vatican City")),
    Country("HN", "title.honduras", "HN", isEu = false, isCountry = true, Nil),
    Country("HK", "title.hong_kong", "HK", isEu = false, isCountry = true, Nil),
    Country("HU", "title.hungary", "HU", isEu = true, isCountry = true, Nil),
    Country("IS", "title.iceland", "IS", isEu = false, isCountry = true, Nil),
    Country("IN", "title.india", "IN", isEu = false, isCountry = true, Nil),
    Country("ID", "title.indonesia", "ID", isEu = false, isCountry = true, Nil),
    Country("IR", "title.iran", "IR", isEu = false, isCountry = true, Nil),
    Country("IQ", "title.iraq", "IQ", isEu = false, isCountry = true, Nil),
    Country("IE", "title.ireland", "IE", isEu = true, isCountry = true, List("Republic of Ireland", "Eire")),
    Country("IM", "title.isle_of_man", "IM", isEu = false, isCountry = true, Nil),
    Country("IL", "title.israel", "IL", isEu = false, isCountry = true, Nil),
    Country("IT", "title.italy", "IT", isEu = true, isCountry = true, Nil),
    Country("JM", "title.jamaica", "JM", isEu = false, isCountry = true, Nil),
    Country("JP", "title.japan", "JP", isEu = false, isCountry = true, Nil),
    Country("JE", "title.jersey", "JE", isEu = false, isCountry = true, List("Channel Islands")),
    Country("JO", "title.jordan", "JO", isEu = false, isCountry = true, Nil),
    Country("KZ", "title.kazakhstan", "KZ", isEu = false, isCountry = true, Nil),
    Country("KE", "title.kenya", "KE", isEu = false, isCountry = true, Nil),
    Country("KI", "title.kiribati", "KI", isEu = false, isCountry = true, Nil),
    Country("KP", "title.north_korea", "KP", isEu = false, isCountry = true, Nil),
    Country("KR", "title.south_korea", "KR", isEu = false, isCountry = true, Nil),
    Country("KO", "title.kosovo", "KO", isEu = false, isCountry = true, Nil),
    Country("KW", "title.kuwait", "KW", isEu = false, isCountry = true, Nil),
    Country("KG", "title.kyrgyzstan", "KG", isEu = false, isCountry = true, Nil),
    Country("ES5", "title.lanzarote", "ES", isEu = false, isCountry = true, List("Lanzarrote")),
    Country("ES6", "title.la_palma", "ES", isEu = false, isCountry = true, List("Las Palma")),
    Country("LA", "title.laos", "LA", isEu = false, isCountry = true, List("Lao Peoples Democratic Republic")),
    Country("LV", "title.latvia", "LV", isEu = true, isCountry = true, Nil),
    Country("LB", "title.lebanon", "LB", isEu = false, isCountry = true, Nil),
    Country("LS", "title.lesotho", "LS", isEu = false, isCountry = true, Nil),
    Country("LR", "title.liberia", "LR", isEu = false, isCountry = true, Nil),
    Country("LY", "title.libya", "LY", isEu = false, isCountry = true, Nil),
    Country("LI", "title.liechtenstein", "LI", isEu = false, isCountry = true, Nil),
    Country("LT", "title.lithuania", "LT", isEu = true, isCountry = true, Nil),
    Country("LU", "title.luxembourg", "LU", isEu = true, isCountry = true, List("Luxemburg")),
    Country("MO", "title.macao", "MO", isEu = false, isCountry = true, Nil),
    Country("MK", "title.north_macedonia", "MK", isEu = false, isCountry = true, Nil),
    Country("MG", "title.madagascar", "MG", isEu = false, isCountry = true, Nil),
    Country("MW", "title.malawi", "MW", isEu = false, isCountry = true, Nil),
    Country("MY", "title.malaysia", "MY", isEu = false, isCountry = true, Nil),
    Country("MV", "title.maldives", "MV", isEu = false, isCountry = true, List("Maldive Islands")),
    Country("ML", "title.mali", "ML", isEu = false, isCountry = true, List("Mali Republic", "Republic of Mali")),
    Country("MT", "title.malta", "MT", isEu = true, isCountry = true, Nil),
    Country("MH", "title.marshall_islands", "MH", isEu = false, isCountry = true, Nil),
    Country("MQ", "title.martinique", "MQ", isEu = false, isCountry = true, Nil),
    Country("MR", "title.mauritania", "MR", isEu = false, isCountry = true, Nil),
    Country("MU", "title.mauritius", "MU", isEu = false, isCountry = true, Nil),
    Country("YT", "title.mayotte", "YT", isEu = false, isCountry = true, Nil),
    Country("MX", "title.mexico", "MX", isEu = false, isCountry = true, Nil),
    Country("FM", "title.micronesia", "FM", isEu = false, isCountry = true, Nil),
    Country("MD", "title.moldova", "MD", isEu = false, isCountry = true, Nil),
    Country("MC", "title.monaco", "MC", isEu = false, isCountry = true, Nil),
    Country("MN", "title.mongolia", "MN", isEu = false, isCountry = true, Nil),
    Country("ME", "title.montenegro", "ME", isEu = false, isCountry = true, Nil),
    Country("MS", "title.montserrat", "MS", isEu = false, isCountry = true, Nil),
    Country("MA", "title.morocco", "MA", isEu = false, isCountry = true, Nil),
    Country("MZ", "title.mozambique", "MZ", isEu = false, isCountry = true, Nil),
    Country("MM", "title.myanmar", "MM", isEu = false, isCountry = true, List("Burma")),
    Country("NA", "title.namibia", "NA", isEu = false, isCountry = true, Nil),
    Country("NR", "title.nauru", "NR", isEu = false, isCountry = true, Nil),
    Country("NP", "title.nepal", "NP", isEu = false, isCountry = true, Nil),
    Country("NL", "title.netherlands", "NL", isEu = true, isCountry = true, List("Holland", "Dutch", "Amsterdam")),
    Country("NC", "title.new_caledonia", "NC", isEu = false, isCountry = true, Nil),
    Country("NZ", "title.new_zealand", "NZ", isEu = false, isCountry = true, Nil),
    Country("NI", "title.nicaragua", "NI", isEu = false, isCountry = true, Nil),
    Country("NE", "title.niger", "NE", isEu = false, isCountry = true, List("Republic of the Niger", "Niger Republic")),
    Country("NG", "title.nigeria", "NG", isEu = false, isCountry = true, Nil),
    Country("NU", "title.niue", "NU", isEu = false, isCountry = true, Nil),
    Country("NF", "title.norfolk_island", "NF", isEu = false, isCountry = true, Nil),
    Country("MP", "title.northern_mariana_islands", "MP", isEu = false, isCountry = true, Nil),
    Country("NO", "title.norway", "NO", isEu = false, isCountry = true, Nil),
    Country("OM", "title.oman", "OM", isEu = false, isCountry = true, Nil),
    Country("PK", "title.pakistan", "PK", isEu = false, isCountry = true, Nil),
    Country("PW", "title.palau", "PW", isEu = false, isCountry = true, Nil),
    Country("PS", "title.palestine_state_of", "PS", isEu = false, isCountry = true, Nil),
    Country("PA", "title.panama", "PA", isEu = false, isCountry = true, Nil),
    Country("PG", "title.papua_new_guinea", "PG", isEu = false, isCountry = true, Nil),
    Country("PY", "title.paraguay", "PY", isEu = false, isCountry = true, Nil),
    Country("PE", "title.peru", "PE", isEu = false, isCountry = true, Nil),
    Country(
      "PH",
      "title.philippines",
      "PH",
      isEu = false,
      isCountry = true,
      List("Philippenes", "Phillipines", "Phillippines", "Philipines")
    ),
    Country("PN", "title.pitcairn", "PN", isEu = false, isCountry = true, Nil),
    Country("PL", "title.poland", "PL", isEu = true, isCountry = true, Nil),
    Country("PT", "title.portugal", "PT", isEu = true, isCountry = true, Nil),
    Country("PR", "title.puerto_rico", "PR", isEu = false, isCountry = true, Nil),
    Country("QA", "title.qatar", "QA", isEu = false, isCountry = true, Nil),
    Country("RE", "title.reunion", "RE", isEu = false, isCountry = true, List("Reunion")),
    Country("RO", "title.romania", "RO", isEu = true, isCountry = true, Nil),
    Country("RU", "title.russia", "RU", isEu = false, isCountry = true, List("USSR", "Soviet Union")),
    Country("RW", "title.rwanda", "RW", isEu = false, isCountry = true, Nil),
    Country("BL", "title.saint_barthelemy", "BL", isEu = false, isCountry = true, List("Barthélemy", "Barthelemy")),
    Country(
      "SH",
      "title.saint_helena_ascension_and_tristan_da_cunha",
      "SH",
      isEu = false,
      isCountry = true,
      List("St Helena")
    ),
    Country("KN", "title.saint_kitts_and_nevis", "KN", isEu = false, isCountry = true, List("St Kitts")),
    Country("LC", "title.saint_lucia", "LC", isEu = false, isCountry = true, List("St Lucia")),
    Country("MF", "title.saint_martin_french_part", "MF", isEu = false, isCountry = true, List("St Martin")),
    Country("PM", "title.saint_pierre_and_miquelon", "PM", isEu = false, isCountry = true, List("St Pierre")),
    Country("VC", "title.saint_vincent_and_the_grenadines", "VC", isEu = false, isCountry = true, List("St Vincent")),
    Country("WS", "title.samoa", "WS", isEu = false, isCountry = true, List("Western Samoa")),
    Country("SM", "title.san_marino", "SM", isEu = false, isCountry = true, Nil),
    Country("ST", "title.sao_tome_and_principe", "ST", isEu = false, isCountry = true, Nil),
    Country("SA", "title.saudi_arabia", "SA", isEu = false, isCountry = true, Nil),
    Country("SN", "title.senegal", "SN", isEu = false, isCountry = true, Nil),
    Country("RS", "title.serbia", "RS", isEu = false, isCountry = true, Nil),
    Country("SC", "title.seychelles", "SC", isEu = false, isCountry = true, Nil),
    Country("SL", "title.sierra_leone", "SL", isEu = false, isCountry = true, Nil),
    Country("SG", "title.singapore", "SG", isEu = false, isCountry = true, Nil),
    Country("SX", "title.sint_maarten_dutch_part", "SX", isEu = false, isCountry = true, Nil),
    Country("SK", "title.slovakia", "SK", isEu = true, isCountry = true, Nil),
    Country("SI", "title.slovenia", "SI", isEu = true, isCountry = true, Nil),
    Country("SB", "title.solomon_islands", "SB", isEu = false, isCountry = true, List("Solomons")),
    Country("SO", "title.somalia", "SO", isEu = false, isCountry = true, Nil),
    Country("ZA", "title.south_africa", "ZA", isEu = false, isCountry = true, List("RSA")),
    Country("GS", "title.south_georgia_and_the_south_sandwich_islands", "GS", isEu = false, isCountry = true, Nil),
    Country("SS", "title.south_sudan", "SS", isEu = false, isCountry = true, Nil),
    Country("ES0", "title.spain", "ES", isEu = true, isCountry = true, Nil),
    Country("LK", "title.sri_lanka", "LK", isEu = false, isCountry = true, Nil),
    Country("SD", "title.sudan", "SD", isEu = false, isCountry = true, Nil),
    Country("SR", "title.suriname", "SR", isEu = false, isCountry = true, Nil),
    Country("SJ", "title.svalbard_and_jan_mayen", "SJ", isEu = false, isCountry = true, Nil),
    Country("SE", "title.sweden", "SE", isEu = true, isCountry = true, Nil),
    Country("CH", "title.switzerland", "CH", isEu = false, isCountry = true, List("Swiss")),
    Country("SY", "title.syria", "SY", isEu = false, isCountry = true, Nil),
    Country("TW", "title.taiwan", "TW", isEu = false, isCountry = true, Nil),
    Country("TJ", "title.tajikistan", "TJ", isEu = false, isCountry = true, Nil),
    Country("TZ", "title.tanzania", "TZ", isEu = false, isCountry = true, Nil),
    Country("ES1", "title.tenerife", "ES", isEu = false, isCountry = true, List("Tennerife")),
    Country("TH", "title.thailand", "TH", isEu = false, isCountry = true, Nil),
    Country("TL", "title.timor_leste", "TL", isEu = false, isCountry = true, Nil),
    Country(
      "TG",
      "title.togo",
      "TG",
      isEu = false,
      isCountry = true,
      List("Togo Republic", "Togolese Republic", "Republic of Togo")
    ),
    Country("TK", "title.tokelau", "TK", isEu = false, isCountry = true, Nil),
    Country("TO", "title.tonga", "TO", isEu = false, isCountry = true, Nil),
    Country("TT", "title.trinidad_and_tobago", "TT", isEu = false, isCountry = true, Nil),
    Country("TN", "title.tunisia", "TN", isEu = false, isCountry = true, Nil),
    Country("TR", "title.turkey", "TR", isEu = false, isCountry = true, Nil),
    Country("TM", "title.turkmenistan", "TM", isEu = false, isCountry = true, Nil),
    Country("TC", "title.turks_and_caicos_islands", "TC", isEu = false, isCountry = true, Nil),
    Country("TV", "title.tuvalu", "TV", isEu = false, isCountry = true, Nil),
    Country("UG", "title.uganda", "UG", isEu = false, isCountry = true, Nil),
    Country("UA", "title.ukraine", "UA", isEu = false, isCountry = true, Nil),
    Country(
      "AE",
      "title.united_arab_emirates",
      "AE",
      isEu = false,
      isCountry = true,
      List("UAE", "emirati", "dubai", "abu dahbi", "abu dhabi")
    ),
    Country(
      "GB",
      "title.united_kingdom",
      "GB",
      isEu = false,
      isCountry = true,
      List("England", "Scotland", "Wales", "Northern Ireland", "GB", "UK")
    ),
    Country("UM", "title.united_states_minor_outlying_islands", "UM", isEu = false, isCountry = true, Nil),
    Country(
      "US",
      "title.united_states_of_america",
      "US",
      isEu = false,
      isCountry = true,
      List("USA", "US", "American")
    ),
    Country("UY", "title.uruguay", "UY", isEu = false, isCountry = true, Nil),
    Country("UZ", "title.uzbekistan", "UZ", isEu = false, isCountry = true, Nil),
    Country("VU", "title.vanuatu", "VU", isEu = false, isCountry = true, Nil),
    Country("VE", "title.venezuela", "VE", isEu = false, isCountry = true, Nil),
    Country("VN", "title.vietnam", "VN", isEu = false, isCountry = true, Nil),
    Country("VG", "title.virgin_islands_british", "VG", isEu = false, isCountry = true, Nil),
    Country("VI", "title.virgin_islands_us", "VI", isEu = false, isCountry = true, Nil),
    Country("WF", "title.wallis_and_futuna", "WF", isEu = false, isCountry = true, Nil),
    Country("EH", "title.western_sahara", "EH", isEu = false, isCountry = true, Nil),
    Country("YE", "title.yemen", "YE", isEu = false, isCountry = true, Nil),
    Country("ZM", "title.zambia", "ZM", isEu = false, isCountry = true, Nil),
    Country("ZW", "title.zimbabwe", "ZW", isEu = false, isCountry = true, Nil),
    Country("EU", "title.european_union", "EU", isEu = true, isCountry = false, List("EU"))
  )

  "getAllCountriesAndEU" should {

    val countriesService = app.injector.instanceOf[CountriesService] // CountriesService.getAllCountries

    "return the expected countries" in {
      countriesService.getAllCountriesAndEu shouldEqual expectedCountriesAndEu
    }

    "not return 2 countries with the same code" in {

      val grouped = countriesService.getAllCountries.groupBy(_.code)

      val dupes = for (group <- grouped if group._2.size > 1) yield group

      dupes shouldBe empty
    }
  }

  "getAllCountries" should {

    val countriesService = app.injector.instanceOf[CountriesService] // CountriesService.getAllCountries

    "return the expected countries" in {
      countriesService.getAllCountries shouldEqual expectedCountries
    }

  }

  "isInEu" should {

    val countriesService = app.injector.instanceOf[CountriesService]

    "return true for countries in EU" in {
      countriesService.isInEu("ES0") shouldBe true
    }

    "return false for countries not in EU" in {
      countriesService.isInEu("TN") shouldBe false
    }
  }

  "getCountryByCode" should {

    val countriesService = app.injector.instanceOf[CountriesService]

    "get a country by its code" in {
      countriesService.getCountryByCode("ES0") shouldBe Some(
        Country("ES0", "title.spain", "ES", isEu = true, isCountry = true, Nil)
      )
    }
  }

  "Country" should {
    val validCountry = Country(
      code = "GBR",
      countryName = "United Kingdom",
      alphaTwoCode = "GB",
      isEu = false,
      isCountry = true,
      countrySynonyms = List("Britain", "England")
    )

    "serialize to JSON" when {
      "all fields are valid" in {
        Json.toJson(validCountry) shouldBe Json.obj(
          "code"            -> "GBR",
          "countryName"     -> "United Kingdom",
          "alphaTwoCode"    -> "GB",
          "isEu"            -> false,
          "isCountry"       -> true,
          "countrySynonyms" -> Json.arr("Britain", "England")
        )
      }

      "countrySynonyms is empty and ensure no nulls" in {
        val countryWithEmptySynonyms = validCountry.copy(countrySynonyms = List.empty)

        Json.toJson(countryWithEmptySynonyms) shouldBe Json.obj(
          "code"            -> "GBR",
          "countryName"     -> "United Kingdom",
          "alphaTwoCode"    -> "GB",
          "isEu"            -> false,
          "isCountry"       -> true,
          "countrySynonyms" -> Json.arr()
        )
      }
    }

    "deserialize from JSON" when {
      "all fields are valid" in {
        val json = Json.obj(
          "code"            -> "GBR",
          "countryName"     -> "United Kingdom",
          "alphaTwoCode"    -> "GB",
          "isEu"            -> false,
          "isCountry"       -> true,
          "countrySynonyms" -> Json.arr("Britain", "England")
        )

        json.validate[Country] shouldBe JsSuccess(validCountry)
      }

      "countrySynonyms is empty" in {
        val json = Json.obj(
          "code"            -> "GBR",
          "countryName"     -> "United Kingdom",
          "alphaTwoCode"    -> "GB",
          "isEu"            -> false,
          "isCountry"       -> true,
          "countrySynonyms" -> Json.arr()
        )

        json.validate[Country] shouldBe JsSuccess(
          validCountry.copy(countrySynonyms = List.empty)
        )
      }

      "countrySynonyms is missing" in {
        val json = Json.obj(
          "code"         -> "GBR",
          "countryName"  -> "United Kingdom",
          "alphaTwoCode" -> "GB",
          "isEu"         -> false,
          "isCountry"    -> true
        )

        json.validate[Country] shouldBe a[JsError]
      }

      "countrySynonyms array is empty" in {
        val json = Json.obj(
          "code"            -> "GBR",
          "countryName"     -> "United Kingdom",
          "alphaTwoCode"    -> "GB",
          "isEu"            -> false,
          "countrySynonyms" -> Json.arr()
        )

        json.validate[Country] shouldBe a[JsError]
      }

      "alphaTwoCode has special characters" in {
        val json = Json.obj(
          "code"            -> "GBR",
          "countryName"     -> "United Kingdom",
          "alphaTwoCode"    -> "G@",
          "isEu"            -> false,
          "countrySynonyms" -> Json.arr("Britain", "England")
        )

        json.validate[Country] shouldBe a[JsError]
      }

      "special characters in countrySynonyms" in {
        val json = Json.obj(
          "code"            -> "GBR",
          "countryName"     -> "United Kingdom",
          "alphaTwoCode"    -> "GB",
          "isEu"            -> false,
          "countrySynonyms" -> Json.arr("Brit@in", "Eng1and", "#Country!")
        )

        json.validate[Country] shouldBe a[JsError]
      }
    }

    "fail deserialization" when {
      "required fields are missing" in {
        val json = Json.obj(
          "code"        -> "GBR",
          "countryName" -> "United Kingdom"
          // Missing "alphaTwoCode", "isEu", and "countrySynonyms"
        )

        json.validate[Country] shouldBe a[JsError]
      }

      "field types are invalid" in {
        val json = Json.obj(
          "code"            -> "GBR",
          "countryName"     -> "United Kingdom",
          "alphaTwoCode"    -> 123,
          "isEu"            -> "false",
          "countrySynonyms" -> Json.arr("Britain", "England")
        )

        json.validate[Country] shouldBe a[JsError]
      }
      "countrySynonyms is null" in {
        val jsonNull = Json.obj(
          "code"            -> "GBR",
          "countryName"     -> "United Kingdom",
          "alphaTwoCode"    -> "GB",
          "isEu"            -> false,
          "countrySynonyms" -> null
        )
        jsonNull.validate[Country] shouldBe a[JsError]
      }
      "countrySynonyms is a string instead of an array" in {
        val json = Json.obj(
          "code"            -> "GBR",
          "countryName"     -> "United Kingdom",
          "alphaTwoCode"    -> "GB",
          "isEu"            -> false,
          "countrySynonyms" -> "notAnArray"
        )

        json.validate[Country] shouldBe a[JsError]
      }

      "isEu is null" in {
        val json = Json.obj(
          "code"            -> "GBR",
          "countryName"     -> "United Kingdom",
          "alphaTwoCode"    -> "GB",
          "isEu"            -> null,
          "countrySynonyms" -> Json.arr("Britain", "England")
        )

        json.validate[Country] shouldBe a[JsError]
      }
      "code is empty or missing" in {
        val jsonMissingCode = Json.obj(
          "countryName"     -> "United Kingdom",
          "alphaTwoCode"    -> "GB",
          "isEu"            -> false,
          "countrySynonyms" -> Json.arr("Britain", "England")
        )

        jsonMissingCode.validate[Country] shouldBe a[JsError]
      }
      "invalid JSON structure" in {
        val json = Json.arr(
          Json.obj("key" -> "value")
        )
        json.validate[Country] shouldBe a[JsError]
      }
      "an empty JSON object" in {
        val json = Json.obj()
        json.validate[Country] shouldBe a[JsError]
      }

    }

    "support round-trip serialization/deserialization" in {
      val json = Json.toJson(validCountry)
      json.validate[Country] shouldBe JsSuccess(validCountry)
    }
    "support round-trip serialization/deserialization with edge cases" in {
      val edgeCaseCountry = validCountry.copy(
        alphaTwoCode = "X!",
        countryName = "Very Special #Name",
        countrySynonyms = List.empty
      )

      val json = Json.toJson(edgeCaseCountry)
      json.validate[Country] shouldBe JsSuccess(edgeCaseCountry)
    }
  }
}
