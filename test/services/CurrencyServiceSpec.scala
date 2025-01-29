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

import models.Currency
import play.api.libs.json.{JsError, JsSuccess, Json}
import util.BaseSpec

class CurrencyServiceSpec extends BaseSpec {

  private val expectedCurrencies: List[Currency] = List(
    Currency(
      "AED",
      "title.united_arab_emirates_dirham_aed",
      Some("AED"),
      List("UAE", "Emirati", "dubai", "abu dahbi", "abu dhabi")
    ),
    Currency("ALL", "title.albanian_lek_all", Some("ALL"), Nil),
    Currency("AMD", "title.armenian_dram_amd", Some("AMD"), Nil),
    Currency("AOA", "title.angolan_kwanza_aoa", Some("AOA"), Nil),
    Currency("ARS", "title.argentinian_peso_ars", Some("ARS"), Nil),
    Currency("AUD", "title.australian_dollars_aud", Some("AUD"), List("Australian", "Oz")),
    Currency("AWG", "title.aruban_florin_awg", Some("AWG"), Nil),
    Currency("AZN", "title.azerbaijani_manat_azn", Some("AZN"), Nil),
    Currency("BAM", "title.bosnia_herzegovinian_marka_bam", Some("BAM"), Nil),
    Currency("BBD", "title.barbados_dollars_bbd", Some("BBD"), Nil),
    Currency("BDT", "title.bangladeshi_taka_bdt", Some("BDT"), Nil),
    Currency("BGN", "title.bulgarian_lev_bgn", Some("BGN"), Nil),
    Currency("BHD", "title.bahrainian_dinar_bhd", Some("BHD"), Nil),
    Currency("BIF", "title.burundi_francs_bif", Some("BIF"), Nil),
    Currency("BMD", "title.bermudan_dollars_bmd", Some("BMD"), Nil),
    Currency("BND", "title.brunei_dollars_bnd", Some("BND"), Nil),
    Currency("BOB", "title.bolivian_boliviano_bob", Some("BOB"), Nil),
    Currency("BRL", "title.brazilian_real_brl", Some("BRL"), Nil),
    Currency("BSD", "title.bahamas_dollars_bsd", Some("BSD"), Nil),
    Currency("BTN", "title.bhutan_ngultrum_btn", Some("BTN"), Nil),
    Currency("BWP", "title.botswanan_pula_bwp", Some("BWP"), Nil),
    Currency("BYN", "title.belarusian_roubles_byn", Some("BYN"), Nil),
    Currency("BZD", "title.belize_dollars_bzd", Some("BZD"), Nil),
    Currency("CAD", "title.canadian_dollars_cad", Some("CAD"), Nil),
    Currency("CDF", "title.democratic_republic_of_congo_francs_cdf", Some("CDF"), Nil),
    Currency("CHF", "title.swiss_francs_chf", Some("CHF"), List("Swiss", "Switzerland")),
    Currency("CLP", "title.chilean_pesos_clp", Some("CLP"), List("Chile")),
    Currency("CNY", "title.chinese_yuan_cny", Some("CNY"), List("China")),
    Currency("COP", "title.colombian_pesos_cop", Some("COP"), List("Columbian")),
    Currency("CRC", "title.costa_rican_colon_crc", Some("CRC"), Nil),
    Currency("CUP", "title.cuban_pesos_cup", Some("CUP"), List("Cuban")),
    Currency("CVE", "title.cape_verde_islands_escudos_cve", Some("CVE"), Nil),
    Currency("CZK", "title.czech_republic_koruna_czk", Some("CZK"), List("Czechoslovakia")),
    Currency("DJF", "title.djibouti_francs_djf", Some("DJF"), Nil),
    Currency("DKK", "title.danish_krone_dkk", Some("DKK"), List("Denmark")),
    Currency("DOP", "title.dominican_republic_pesos_dop", Some("DOP"), Nil),
    Currency("DZD", "title.algerian_dinar_dzd", Some("DZD"), Nil),
    Currency("ECS", "title.ecuadorian_dollars_ecs", Some("ECS"), Nil),
    Currency("EGP", "title.egyptian_pounds_egp", Some("EGP"), List("Egypt")),
    Currency("ERN", "title.eritrean_nakfa_ern", Some("ERN"), Nil),
    Currency("ETB", "title.ethiopian_birr_etb", Some("ETB"), Nil),
    Currency("EUR", "title.euro_eur", Some("EUR"), List("Europe", "European")),
    Currency("FJD", "title.fiji_islands_dollars_fjd", Some("FJD"), Nil),
    Currency("GEL", "title.georgian_lari_gel", Some("GEL"), Nil),
    Currency("GHS", "title.ghanian_cedi_ghs", Some("GHS"), List("Ghana")),
    Currency("GMD", "title.gambian_dalasi_gmd", Some("GMD"), Nil),
    Currency("GNF", "title.guinean_francs_gnf", Some("GNF"), Nil),
    Currency("GTQ", "title.guatemalan_quetzal_gtq", Some("GTQ"), Nil),
    Currency("GYD", "title.guyanan_dollars_gyd", Some("GYD"), Nil),
    Currency("HKD", "title.hong_kong_dollars_hkd", Some("HKD"), Nil),
    Currency("HNL", "title.honduras_lempira_hnl", Some("HNL"), Nil),
    Currency("HRK", "title.croatian_kuna_hrk", Some("HRK"), Nil),
    Currency("HTG", "title.haiti_gourde_htg", Some("HTG"), Nil),
    Currency("HUF", "title.hungarian_forints_huf", Some("HUF"), List("Hungary")),
    Currency("IDR", "title.indonesian_rupiahs_idr", Some("IDR"), Nil),
    Currency("ILS", "title.israeli_shekels_ils", Some("ILS"), Nil),
    Currency("INR", "title.indian_rupees_inr", Some("INR"), List("Indian")),
    Currency("IQD", "title.iraqi_dinar_iqd", Some("IQD"), Nil),
    Currency("ISK", "title.icelandic_krona_isk", Some("ISK"), Nil),
    Currency("JMD", "title.jamaican_dollars_jmd", Some("JMD"), List("Jamaican")),
    Currency("JOD", "title.jordanian_dinar_jod", Some("JOD"), Nil),
    Currency("JPY", "title.japanese_yen_jpy", Some("JPY"), Nil),
    Currency("KES", "title.kenyan_shillings_kes", Some("KES"), List("schilling")),
    Currency("KGS", "title.kyrgyz_republic_som_kgs", Some("KGS"), Nil),
    Currency("KHR", "title.cambodian_riel_khr", Some("KHR"), Nil),
    Currency("KMF", "title.comoros_francs_kmf", Some("KMF"), Nil),
    Currency("KRW", "title.south_korean_won_krw", Some("KRW"), Nil),
    Currency("KWD", "title.kuwaiti_dinar_kwd", Some("KWD"), Nil),
    Currency("KYD", "title.cayman_islands_dollars_kyd", Some("KYD"), Nil),
    Currency("KZT", "title.kazakhstanian_tenge_kzt", Some("KZT"), Nil),
    Currency("LAK", "title.lao_kip_lak", Some("LAK"), Nil),
    Currency("LBP", "title.lebanese_pounds_lbp", Some("LBP"), List("Lebanon")),
    Currency("LKR", "title.sri_lankan_rupees_lkr", Some("LKR"), Nil),
    Currency("LRD", "title.liberian_dollars_lrd", Some("LRD"), Nil),
    Currency("LSL", "title.lesotho_loti_lsl", Some("LSL"), Nil),
    Currency("LYD", "title.libyan_dinar_lyd", Some("LYD"), Nil),
    Currency("MAD", "title.moroccon_dirham_mad", Some("MAD"), Nil),
    Currency("MDL", "title.moldovian_leu_mdl", Some("MDL"), List("Moldova")),
    Currency("MGA", "title.madagascar_malagasy_ariary_mga", Some("MGA"), Nil),
    Currency("MKD", "title.macedonian_denar_mkd", Some("MKD"), Nil),
    Currency("MMK", "title.myanmar_kyat_mmk", Some("MMK"), Nil),
    Currency("MNT", "title.mongolian_tugrik_mnt", Some("MNT"), Nil),
    Currency("MOP", "title.macaon_pataca_mop", Some("MOP"), Nil),
    Currency("MRU", "title.mauritanian_ouguiya_mru", Some("MRU"), Nil),
    Currency("MUR", "title.mauritius_rupees_mur", Some("MUR"), Nil),
    Currency("MVR", "title.maldives_rufiyaa_mvr", Some("MVR"), Nil),
    Currency("MWK", "title.malawian_kwacha_mwk", Some("MWK"), Nil),
    Currency("MXN", "title.mexican_pesos_mxn", Some("MXN"), List("Mexico")),
    Currency("MYR", "title.malaysian_ringgit_myr", Some("MYR"), Nil),
    Currency("MZN", "title.mozambiquan_metical_mzn", Some("MZN"), Nil),
    Currency("NGN", "title.nigerian_naira_ngn", Some("NGN"), Nil),
    Currency("NIO", "title.nicaraguan_cordoba_nio", Some("NIO"), Nil),
    Currency("NOK", "title.norwegian_krone_nok", Some("NOK"), List("Norway")),
    Currency("NPR", "title.nepalese_rupees_npr", Some("NPR"), Nil),
    Currency("NZD", "title.new_zealand_dollars_nzd", Some("NZD"), List("Kiwi")),
    Currency("OMR", "title.oman_rial_omr", Some("OMR"), Nil),
    Currency("PAB", "title.panamanian_balboa_pab", Some("PAB"), List("Panamanian")),
    Currency("PEN", "title.peruvian_sol_pen", Some("PEN"), Nil),
    Currency("PGK", "title.papua_new_guinea_kina_pgk", Some("PGK"), Nil),
    Currency(
      "PHP",
      "title.philippineno_pesos_php",
      Some("PHP"),
      List("Philippenes", "Phillipines", "Phillippines", "Philipines")
    ),
    Currency("PKR", "title.pakistani_rupees_pkr", Some("PKR"), Nil),
    Currency("PLN", "title.polish_zloty_pln", Some("PLN"), List("Poland")),
    Currency("PYG", "title.paraguan_guarani_pyg", Some("PYG"), List("Paraguay")),
    Currency("QAR", "title.qatarian_riyal_qar", Some("QAR"), Nil),
    Currency("RON", "title.romanian_leu_ron", Some("RON"), Nil),
    Currency("RSD", "title.serbian_dinar_rsd", Some("RSD"), Nil),
    Currency("RUB", "title.russian_roubles_rub", Some("RUB"), List("USSR", "Soviet Union")),
    Currency("RWF", "title.rwandan_francs_rwf", Some("RWF"), Nil),
    Currency("SAR", "title.saudi_arabian_riyal_sar", Some("SAR"), Nil),
    Currency("SBD", "title.soloman_islands_dollars_sbd", Some("SBD"), Nil),
    Currency("SCR", "title.seychelles_rupees_scr", Some("SCR"), Nil),
    Currency("SDG", "title.sudan_republic_pounds_sdg", Some("SDG"), Nil),
    Currency("SEK", "title.swedish_krona_sek", Some("SEK"), List("Sweden")),
    Currency("SGD", "title.singapore_dollars_sgd", Some("SGD"), Nil),
    Currency("SLL", "title.sierra_leone_leone_sll", Some("SLL"), Nil),
    Currency("SOS", "title.somali_republic_schillings_sos", Some("SOS"), Nil),
    Currency("SRD", "title.suriname_dollars_srd", Some("SRD"), Nil),
    Currency("STD", "title.sao_tome_and_principe_dobra_std", Some("STD"), Nil),
    Currency("SVC", "title.el_salvadorian_colon_svc", Some("SVC"), Nil),
    Currency("SZL", "title.eswatini_lilangeni_szl", Some("SZL"), List("Swaziland")),
    Currency("THB", "title.thai_baht_thb", Some("THB"), List("Thailand")),
    Currency("TMT", "title.turkmenistanian_manat_tmt", Some("TMT"), Nil),
    Currency("TND", "title.tunisian_dinar_tnd", Some("TND"), Nil),
    Currency("TOP", "title.tongan_paanga_top", Some("TOP"), Nil),
    Currency("TRY", "title.turkish_lira_try", Some("TRY"), List("Turkey")),
    Currency("TTD", "title.trinidad_and_tobago_dollars_ttd", Some("TTD"), Nil),
    Currency("TWD", "title.taiwanese_dollars_twd", Some("TWD"), Nil),
    Currency("TZS", "title.tanzanian_schillings_tzs", Some("TZS"), Nil),
    Currency("UAH", "title.ukrainian_hryvnia_uah", Some("UAH"), List("Ukraine")),
    Currency("UGX", "title.ugandan_schillings_ugx", Some("UGX"), Nil),
    Currency(
      "USD",
      "title.usa_dollars_usd",
      Some("USD"),
      List("USD", "USA", "US", "United States of America", "American")
    ),
    Currency("UYU", "title.uruguan_pesos_uyu", Some("UYU"), List("Urguguay")),
    Currency("UZS", "title.uzbekistanian_sum_uzs", Some("UZS"), Nil),
    Currency("VEF", "title.venezuelan_bolivar_fuerte_vef", Some("VEF"), Nil),
    Currency("VND", "title.vietnamese_dong_vnd", Some("VND"), Nil),
    Currency("VUV", "title.vanuatuan_vatu_vuv", Some("VUV"), Nil),
    Currency("WST", "title.western_samoan_tala_wst", Some("WST"), Nil),
    Currency(
      "XAF",
      "title.central_african_francs_xaf",
      Some("XAF"),
      List("Cameroon", "Chad", "Congo", "Equatorial Guinea", "Gabon")
    ),
    Currency(
      "XCD",
      "title.east_caribbean_dollars_xcd",
      Some("XCD"),
      List(
        "Dominica",
        "Grenada",
        "Montserrat",
        "St Christopher and Anguilla",
        "Saint Christopher",
        "St Lucia",
        "Saint Lucia",
        "St Vincent",
        "Saint Vincent"
      )
    ),
    Currency(
      "XOF",
      "title.west_african_francs_xof",
      Some("XOF"),
      List(
        "Benin",
        "Burkina Faso",
        "Cote d'Ivoire",
        "Ivory Coast",
        "Guinea Bissau",
        "Mali Republic",
        "Niger Republic",
        "Senegal",
        "Cote dIvoire",
        "Cote d Ivoire",
        "Republic of Mali",
        "Republic of the Niger",
        "Togolese Republic",
        "Togo Republic",
        "Republic of Togo"
      )
    ),
    Currency(
      "XPF",
      "title.cfp_francs_xpf",
      Some("XPF"),
      List("Fr. Polynesia", "French Polynesia", "New Caledonia", "Wallis and Futuna Islands")
    ),
    Currency("YER", "title.yemen_rial_yer", Some("YER"), Nil),
    Currency("ZAR", "title.south_african_rand_zar", Some("ZAR"), Nil),
    Currency("ZMW", "title.zambian_kwacha_zmw", Some("ZMW"), Nil),
    Currency("ZWL", "title.zimbabwean_dollars_zwl", Some("ZWL"), Nil),
    Currency(
      "GBP",
      "title.british_pounds_gbp",
      None,
      List("England", "Scotland", "Wales", "Northern Ireland", "British", "sterling", "pound", "GB")
    ),
    Currency("FKP", "title.falkland_island_pounds_fkp", None, List("Falklands")),
    Currency("GIP", "title.gibraltar_pounds_gip", None, Nil),
    Currency("GGP", "title.guernsey_pounds_ggp", None, List("Channel Islands")),
    Currency("IMP", "title.isle_of_man_pounds_imp", None, Nil),
    Currency("JEP", "title.jersey_pounds_jep", None, List("Channel Islands", "St Helier", "Sanint Helier")),
    Currency("SHP", "title.saint_helenian_pounds_shp", None, List("St Helenia"))
  )

  "getAllCurrencies" should {

    val currencyService = app.injector.instanceOf[CurrencyService]

    "return the expected currencies" in {
      currencyService.getAllCurrencies shouldEqual expectedCurrencies
    }
  }

  "Currency" should {
    val currency = Currency("EUR", "title.euro_eur", Some("EUR"), List("Europe", "European"))

    "Currency" should {

      "serialize to JSON" when {
        "the Email value is valid" in {
          Json.toJson(currency) shouldBe Json.obj(
            "code"               -> "EUR",
            "displayName"        -> "title.euro_eur",
            "currencySynonyms"   -> Json.arr("Europe", "European"),
            "valueForConversion" -> "EUR"
          )
        }
        "currencySynonyms is empty" in {
          Json.toJson(Currency("EUR", "title.euro_eur", Some("EUR"), List())) shouldBe Json.obj(
            "code"               -> "EUR",
            "displayName"        -> "title.euro_eur",
            "currencySynonyms"   -> Json.arr(),
            "valueForConversion" -> "EUR"
          )
        }
        "valueForConversion is not present" in {
          Json.toJson(Currency("EUR", "title.euro_eur", None, List("Europe", "European"))) shouldBe Json.obj(
            "code"             -> "EUR",
            "displayName"      -> "title.euro_eur",
            "currencySynonyms" -> Json.arr("Europe", "European")
          )
        }
      }

      "deserialize from JSON" when {
        "the JSON contains a valid Email value" in {
          val json = Json.obj(
            "code"               -> "EUR",
            "displayName"        -> "title.euro_eur",
            "currencySynonyms"   -> Json.arr("Europe", "European"),
            "valueForConversion" -> "EUR"
          )
          json.validate[Currency] shouldBe JsSuccess(currency)
        }
        "an extra field is present" in {
          val json = Json.obj(
            "extra"              -> 0,
            "code"               -> "EUR",
            "displayName"        -> "title.euro_eur",
            "currencySynonyms"   -> Json.arr("Europe", "European"),
            "valueForConversion" -> "EUR"
          )
          json.validate[Currency] shouldBe JsSuccess(currency)
        }
        "currencySynonyms is empty" in {
          val json = Json.obj(
            "code"               -> "EUR",
            "displayName"        -> "title.euro_eur",
            "currencySynonyms"   -> Json.arr(),
            "valueForConversion" -> "EUR"
          )
          json.validate[Currency] shouldBe JsSuccess(Currency("EUR", "title.euro_eur", Some("EUR"), List()))
        }
        "valueForConversion is not present" in {
          val json = Json.obj(
            "code"             -> "EUR",
            "displayName"      -> "title.euro_eur",
            "currencySynonyms" -> Json.arr("Europe", "European")
          )
          json.validate[Currency] shouldBe JsSuccess(
            Currency("EUR", "title.euro_eur", None, List("Europe", "European"))
          )
        }
      }
      "fail to deserialize" when {
        "invalid JSON structure" in {
          val json = Json.arr(
            Json.obj("key" -> "value")
          )
          json.validate[Currency] shouldBe a[JsError]
        }
        "an empty JSON object" in {
          val json = Json.obj()
          json.validate[Currency] shouldBe a[JsError]
        }
      }
    }
  }
}
