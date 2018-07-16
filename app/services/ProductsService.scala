package services

object ProductsService {

  sealed trait Node {
    val name: String
    val token: String
  }

  case class Branch(token: String, name: String, children: List[Node]) extends Node {


    def getDescendant(path: Seq[String]): Option[Node] = {
      val child = children.find(_.token==path.head)

      child match {
        case None => None
        case Some(c: Leaf) => Some(c)
        case Some(c: Branch) => path.tail match {
          case Nil => Some(c)
          case _ => c.getDescendant(path.tail)
        }
      }
    }
  }

  case class Leaf(token: String, name: String, rateID: String, templateID: String) extends Node

  private val productTree =
    Branch("root", "Root",
      List(
        Branch("alcohol",
          "Alcohol", List(
            Leaf("beer", "Beer", "ALC/A2/BEER", "alcohol"),
            Leaf("cider", "Cider", "ALC/A1/CIDER", "alcohol"),
            Leaf("other", "Sparkling or fortified wine and other alcoholic drinks up to 22% alcohol by volume", "ALC/A1/U22", "alcohol"),
            Leaf("spirits", "Spirits", "ALC/A1/O22", "alcohol"),
            Leaf("wine", "Wine","ALC/A3/WINE","alcohol")
          )
        ),
        Branch("tobacco",
          "Tobacco", List(
            Leaf("cigarettes", "Cigarettes", "TOB/A1/CIGRT", "cigarettes"),
            Leaf("cigars", "Cigars", "TOB/A1/CIGAR", "cigars"),
            Leaf("cigarillos", "Cigarillos", "TOB/A1/CRILO", "tobacco"),
            Leaf("rolling", "Rolling tobacco", "TOB/A1/HAND", "tobacco"),
            Leaf("chewing", "Chewing or pipe tobacco", "TOB/A1/OTHER", "tobacco")
          )
        ),
        Branch("other-goods",
          "Other Goods", List(
            Leaf("antiques", "Antiques and works of art", "OGD/ART", "other-goods"),
            Leaf("books", "Books and publications", "OGD/BKS/MISC", "other-goods"),
            Branch("carpets-cotton-fabric",
              "Carpets, cotton and fabrics", List(
                Leaf("carpets", "Carpets", "OGD/CRPT", "other-goods"),
                Leaf("cotton", "Cotton", "OGD/CLTHS/ADULT", "other-goods"),
                Leaf("fabrics", "Fabrics", "OGD/FBRIC", "other-goods")
              )
            ),
            Leaf("car-seats", "Children's car seats", "OGD/MOB/MISC", "other-goods"),
            Branch("clothing",
              "Clothing and footwear", List(
                Leaf("childrens", "Children's clothing", "OGD/CLTHS/CHILD", "other-goods"),
                Leaf("footwear", "Footwear", "OGD/FOOTW", "other-goods"),
                Leaf("other", "All other clothing", "OGD/CLTHS/ADULT", "other-goods")

              )
            ),
            Branch("disability-mobility",
              "Disability equipment and mobility aids", List(
                Leaf("disability", "Disability equipment", "OGD/BKS/MISC", "other-goods"),
                Leaf("mobility", "Mobility aids", "OGD/MOB/MISC", "other-goods")
              )
            ),
            Branch("electronic-devices",
              "Electronic devices", List(
                Leaf("televisions", "Televisions", "OGD/DIGI/TV", "other-goods"),
                Leaf("other", "All other electronic devices", "OGD/DIGI/MISC", "other-goods")
              )
            ),
            Leaf("furniture", "Furniture", "ODG/ORN/MISC", "other-goods"),
            Leaf("games-sports", "Games and sport equipment", "OGD/SPORT", "other-goods"),
            Branch("glassware-ornaments",
              "Glassware and ornaments", List(
                Leaf("glassware", "Glassware", "OGD/GLASS", "other-goods"),
                Leaf("ornaments", "Ornaments", "OGD/ORN/MISC", "other-goods")
              )
            ),
            Leaf("jewellery", "Jewellery","OGD/DIGI/MISC","other-goods"),
            Branch("metals-wood",
              "Metals and wood products", List(
                Leaf("metals", "Metals", "OGD/ORN/MISC", "other-goods"),
                Leaf("wood", "Wood products", "OGD/ORN/MISC", "other-goods")
              )
            ),
            Leaf("perfumes-cosmetics", "Perfumes and cosmetics", "OGD/COSMT", "other-goods"),
            Leaf("protective-helmets", "Protective helmets", "OGD/BKS/MISC", "other-goods"),
            Leaf("sanitary-products", "Sanitary products", "OGD/MOB/MISC", "other-goods"),
            Leaf("stop-smoking", "Stop smoking products", "OGD/MOB/MISC", "other-goods"),
            Leaf("tablewear", "Tableware", "OGD/TABLE", "other-goods"),
            Leaf("watches-clocks", "Watches and clocks", "OGD/ORN/MISC", "other-goods"),
            Leaf("other", "Anything else", "OGD/OTHER", "other-goods")
          )
        )
      )
    )

  def getProducts: Branch = {
    productTree
  }

}
