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

import models.PortsOfArrival
import util.BaseSpec

class PortsOfArrivalServiceSpec extends BaseSpec {

  private val expectedPorts: List[PortsOfArrival] = List(
    PortsOfArrival("ABZ", "title.aberdeen_airport", isGB = true, Nil),
    PortsOfArrival("ABD", "title.aberdeen_port", isGB = true, Nil),
    PortsOfArrival("AFK", "title.ashford", isGB = true, Nil),
    PortsOfArrival("AVO", "title.avonmouth", isGB = true, Nil),
    PortsOfArrival("BAR", "title.barrow", isGB = true, Nil),
    PortsOfArrival("BEL", "title.belfast_docks", isGB = false, Nil),
    PortsOfArrival("BFS", "title.belfast_international_airport", isGB = false, List("BFS")),
    PortsOfArrival("BWK", "title.berwick_upon_tweed", isGB = true, Nil),
    PortsOfArrival("BQH", "title.biggin_hill_airport", isGB = true, List("London Biggin Hill Airport", "BQH")),
    PortsOfArrival("BRK", "title.birkenhead", isGB = true, Nil),
    PortsOfArrival("BHX", "title.birmingham_airport", isGB = true, List("BHX")),
    PortsOfArrival("BLK", "title.blackpool_airport", isGB = true, List("BLK")),
    PortsOfArrival("BLY", "title.blyth", isGB = true, Nil),
    PortsOfArrival("BOH", "title.bournemouth_airport", isGB = true, List("BOH")),
    PortsOfArrival("BRS", "title.bristol_airport", isGB = true, List("BRS")),
    PortsOfArrival("BZN", "title.brize_norton", isGB = true, List("RAF Brize Norton", "BZZ")),
    PortsOfArrival("BUC", "title.buckie", isGB = true, Nil),
    PortsOfArrival("BTL", "title.burntisland", isGB = true, Nil),
    PortsOfArrival("CYN", "title.cairnryan", isGB = true, List("Stranraer")),
    PortsOfArrival("CBZ", "title.cambridge_city_airport", isGB = true, List("Cambridge Airport", "CBG")),
    PortsOfArrival("CWL", "title.cardiff_airport", isGB = true, List("CWL")),
    PortsOfArrival("CAX", "title.carlisle_lake_district_airport", isGB = true, List("Carlisle Airport", "CAX")),
    PortsOfArrival(
      "LDY",
      "title.city_of_derry_airport",
      isGB = false,
      List("Londonderry Airport", "LDY", "Derry Airport")
    ),
    PortsOfArrival("CVT", "title.coventry_airport", isGB = true, List("CVT")),
    PortsOfArrival("FYL", "title.derry_port", isGB = false, List("Foyle Port", "Londonderry Port")),
    PortsOfArrival("DSA", "title.doncaster_sheffield_airport", isGB = true, List("Robin Hood Airport", "DSA")),
    PortsOfArrival("DVR", "title.dover", isGB = true, List("Port of Dover")),
    PortsOfArrival("DND", "title.dundee_airport", isGB = true, List("DND")),
    PortsOfArrival("EMA", "title.east_midlands_airport", isGB = true, List("EMA")),
    PortsOfArrival("EBD", "title.ebbsfleet", isGB = true, Nil),
    PortsOfArrival("EDI", "title.edinburgh_airport", isGB = true, List("EDI")),
    PortsOfArrival("EXT", "title.exeter_airport", isGB = true, List("EXT")),
    PortsOfArrival("FAL", "title.falmouth", isGB = true, Nil),
    PortsOfArrival("FAB", "title.farnborough_airport", isGB = true, List("FAB")),
    PortsOfArrival("FXT", "title.felixstowe", isGB = true, Nil),
    PortsOfArrival("FNT", "title.finnart", isGB = true, Nil),
    PortsOfArrival("FIS", "title.fishguard", isGB = true, Nil),
    PortsOfArrival("FLE", "title.fleetwood", isGB = true, Nil),
    PortsOfArrival(
      "FOL",
      "title.folkestone",
      isGB = true,
      List("Cheriton", "Folkestone", "Eurotunnel", "Channel Tunnel")
    ),
    PortsOfArrival("FWM", "title.fort_william_corpach", isGB = true, Nil),
    PortsOfArrival("FRB", "title.fraserburgh", isGB = true, Nil),
    PortsOfArrival("LGW", "title.gatwick_airport", isGB = true, List("London Gatwick Airport", "LGW")),
    PortsOfArrival("BHD", "title.george_best_belfast_city_airport", isGB = false, List("Belfast City Airport", "BHD")),
    PortsOfArrival("GLA", "title.glasgow_airport", isGB = true, List("Glasgow International Airport", "GLA")),
    PortsOfArrival("GLW", "title.glasgow_docks", isGB = true, Nil),
    PortsOfArrival("PIK", "title.glasgow_prestwick_airport", isGB = true, List("Prestwick Airport", "PIK")),
    PortsOfArrival("GSA", "title.glensanda", isGB = true, Nil),
    PortsOfArrival("GRG", "title.grangemouth", isGB = true, Nil),
    PortsOfArrival("GRK", "title.greenock_ocean_terminal", isGB = true, List("Greenock Port")),
    PortsOfArrival("GRI", "title.grimsby", isGB = true, Nil),
    PortsOfArrival("HTP", "title.hartlepool", isGB = true, Nil),
    PortsOfArrival("HRH", "title.harwich", isGB = true, Nil),
    PortsOfArrival("HED", "title.headcorn_aerodrome", isGB = true, Nil),
    PortsOfArrival("LHR", "title.heathrow_airport", isGB = true, List("London Heathrow Airport", "LHR")),
    PortsOfArrival("HEY", "title.heysham", isGB = true, Nil),
    PortsOfArrival("HLD", "title.holyhead", isGB = true, Nil),
    PortsOfArrival("HUL", "title.hull", isGB = true, Nil),
    PortsOfArrival("HUY", "title.humberside_airport", isGB = true, List("HUY")),
    PortsOfArrival("HST", "title.hunterston", isGB = true, Nil),
    PortsOfArrival("IMM", "title.immingham", isGB = true, Nil),
    PortsOfArrival("IVG", "title.invergordon", isGB = true, Nil),
    PortsOfArrival("INK", "title.inverkeithing", isGB = true, Nil),
    PortsOfArrival("INV", "title.inverness_airport", isGB = true, List("INV")),
    PortsOfArrival("IVP", "title.inverness_port", isGB = true, Nil),
    PortsOfArrival("IPS", "title.ipswich", isGB = true, Nil),
    PortsOfArrival("IOM", "title.isle_of_man_airport", isGB = true, List("Ronaldsway Airport", "IOM")),
    PortsOfArrival("DGS", "title.isle_of_man_sea_terminal", isGB = true, List("Douglas")),
    PortsOfArrival("KLN", "title.kings_lynn", isGB = true, Nil),
    PortsOfArrival("KOI", "title.kirkwall_airport", isGB = true, List("KOI")),
    PortsOfArrival("LAR", "title.larne", isGB = false, Nil),
    PortsOfArrival(
      "LBA",
      "title.leeds_bradford_airport",
      isGB = true,
      List("Leeds Airport", "Bradford Airport", "LBA")
    ),
    PortsOfArrival("LEI", "title.leith", isGB = true, Nil),
    PortsOfArrival("LER", "title.lerwick", isGB = true, Nil),
    PortsOfArrival(
      "LPL",
      "title.liverpool_john_lennon_airport",
      isGB = true,
      List("Liverpool Airport", "John Lennon Airport", "LPL")
    ),
    PortsOfArrival("LCY", "title.london_city_airport", isGB = true, List("LCY")),
    PortsOfArrival("LGP", "title.london_gateway", isGB = true, Nil),
    PortsOfArrival("LTN", "title.luton_airport", isGB = true, List("London Luton Airport", "LTN")),
    PortsOfArrival("LYX", "title.lydd_airport", isGB = true, List("London Ashford Airport", "LYX")),
    PortsOfArrival("MAN", "title.manchester_airport", isGB = true, List("MAN")),
    PortsOfArrival("MRY", "title.maryport", isGB = true, Nil),
    PortsOfArrival("MTH", "title.methil", isGB = true, Nil),
    PortsOfArrival("MON", "title.montrose", isGB = true, Nil),
    PortsOfArrival("NCL", "title.newcastle_airport", isGB = true, List("Newcastle International Airport", "NCL")),
    PortsOfArrival("NHV", "title.newhaven", isGB = true, Nil),
    PortsOfArrival("NQY", "title.newquay_airport", isGB = true, List("NQY")),
    PortsOfArrival("NSH", "title.north_shields", isGB = true, Nil),
    PortsOfArrival("NHT", "title.northolt", isGB = true, List("RAF Northolt", "NHT")),
    PortsOfArrival("NWI", "title.norwich_airport", isGB = true, List("NWI")),
    PortsOfArrival("PEL", "title.peel", isGB = true, Nil),
    PortsOfArrival("PED", "title.pembroke_port", isGB = true, Nil),
    PortsOfArrival("PHD", "title.peterhead", isGB = true, Nil),
    PortsOfArrival("PLY", "title.plymouth", isGB = true, Nil),
    PortsOfArrival("POO", "title.poole", isGB = true, Nil),
    PortsOfArrival("DUN", "title.port_of_dundee", isGB = true, List("Dundee Port")),
    PortsOfArrival("LIV", "title.port_of_liverpool", isGB = true, List("Liverpool Port")),
    PortsOfArrival("POS", "title.port_of_southampton", isGB = true, List("Southampton Port")),
    PortsOfArrival("TYN", "title.port_of_tyne", isGB = true, List("Tyne Port")),
    PortsOfArrival("PTM", "title.portsmouth", isGB = true, Nil),
    PortsOfArrival("PUR", "title.purfleet", isGB = true, Nil),
    PortsOfArrival("RSY", "title.ramsey", isGB = true, Nil),
    PortsOfArrival("RMG", "title.ramsgate", isGB = true, Nil),
    PortsOfArrival("RER", "title.redcar", isGB = true, Nil),
    PortsOfArrival("ROS", "title.rosyth", isGB = true, List("Port of Rosyth")),
    PortsOfArrival("SEA", "title.seaham", isGB = true, Nil),
    PortsOfArrival("ESH", "title.shoreham_airport", isGB = true, List("Brighton City Airport", "ESH")),
    PortsOfArrival("SIL", "title.silloth", isGB = true, Nil),
    PortsOfArrival("SOU", "title.southampton_airport", isGB = true, List("SOU")),
    PortsOfArrival("SEN", "title.southend_airport", isGB = true, List("London Southend Airport", "SEN")),
    PortsOfArrival("STP", "title.st_pancras", isGB = true, List("London St Pancras")),
    PortsOfArrival("STN", "title.stansted_airport", isGB = true, List("London Stansted Airport", "STN")),
    PortsOfArrival("STY", "title.stornoway_airport", isGB = true, List("STY")),
    PortsOfArrival("LSI", "title.sumburgh_airport", isGB = true, List("LSI")),
    PortsOfArrival("SUN", "title.sunderland", isGB = true, Nil),
    PortsOfArrival("SWA", "title.swansea", isGB = true, Nil),
    PortsOfArrival(
      "MME",
      "title.teeside_international_airport",
      isGB = true,
      List("Durham Tees Valley Airport", "MME")
    ),
    PortsOfArrival("TEE", "title.teesport", isGB = true, Nil),
    PortsOfArrival("MED", "title.thames_gateway_Sheerness", isGB = true, Nil),
    PortsOfArrival("THP", "title.thames_port", isGB = true, Nil),
    PortsOfArrival("TIL", "title.tilbury", isGB = true, List("Port of Tilbury")),
    PortsOfArrival("UNT", "title.unst", isGB = true, Nil),
    PortsOfArrival("WPT", "title.warrenpoint", isGB = false, Nil),
    PortsOfArrival("WEY", "title.weymouth", isGB = true, Nil),
    PortsOfArrival("WTB", "title.whitby", isGB = true, Nil),
    PortsOfArrival("WHV", "title.whitehaven", isGB = true, Nil),
    PortsOfArrival("WIC", "title.wick", isGB = true, Nil),
    PortsOfArrival("WOR", "title.workington", isGB = true, Nil)
  )

  private val expectedNIPorts: List[PortsOfArrival] = List(
    PortsOfArrival("BEL", "title.belfast_docks", isGB = false, Nil),
    PortsOfArrival("BFS", "title.belfast_international_airport", isGB = false, List("BFS")),
    PortsOfArrival(
      "LDY",
      "title.city_of_derry_airport",
      isGB = false,
      List("Londonderry Airport", "LDY", "Derry Airport")
    ),
    PortsOfArrival("FYL", "title.derry_port", isGB = false, List("Foyle Port", "Londonderry Port")),
    PortsOfArrival("BHD", "title.george_best_belfast_city_airport", isGB = false, List("Belfast City Airport", "BHD")),
    PortsOfArrival("LAR", "title.larne", isGB = false, Nil),
    PortsOfArrival("WPT", "title.warrenpoint", isGB = false, Nil)
  )

  "getAllPorts" should {

    val portsService = app.injector.instanceOf[PortsOfArrivalService]

    "return the expected ports" in {
      portsService.getAllPorts shouldEqual expectedPorts
    }
  }

  "getAllPortsNI" should {

    val portsService = app.injector.instanceOf[PortsOfArrivalService]

    "return the expected ports" in {
      portsService.getAllPortsNI shouldEqual expectedNIPorts
    }

  }

  "isInGB" should {
    val portsService = app.injector.instanceOf[PortsOfArrivalService]

    "return true for ports in GB" in {
      portsService.isInGB("ABZ") shouldBe true
    }

    "return false for ports in NI" in {
      portsService.isInGB("BFS") shouldBe false
    }
  }

  "getPortsByCode" should {

    val portsService = app.injector.instanceOf[PortsOfArrivalService]

    "get a port by its code" in {
      portsService.getPortByCode("ABZ") shouldBe Some(PortsOfArrival("ABZ", "title.aberdeen_airport", isGB = true, Nil))
    }

    "get no value for invalid code" in {
      portsService.getPortByCode("XYZ") shouldBe None
    }
  }

  "getDisplayNameByCode" should {

    val portsService = app.injector.instanceOf[PortsOfArrivalService]

    "get a port name by its code" in {
      portsService.getDisplayNameByCode("ABZ") shouldBe Some("title.aberdeen_airport")
    }

    "get no value when invalid code passed" in {
      portsService.getDisplayNameByCode("XYZ") shouldBe None
    }
  }

  "isValidPortCode" should {

    val portsService = app.injector.instanceOf[PortsOfArrivalService]

    "get true for valid code" in {
      portsService.isValidPortCode("ABZ") shouldBe true
    }

    "get false for invalid code" in {
      portsService.isValidPortCode("XYZ") shouldBe false
    }
  }

  "getCodeByDisplayName" should {

    val portsService = app.injector.instanceOf[PortsOfArrivalService]

    "get code for valid port name" in {
      portsService.getCodeByDisplayName("title.aberdeen_airport") shouldBe Some("ABZ")
    }

    "get false for invalid code" in {
      portsService.getCodeByDisplayName("dummy") shouldBe None
    }
  }
}
