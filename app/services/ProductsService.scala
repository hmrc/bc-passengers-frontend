package services

object ProductsService {

  sealed trait Node {
    val name: String
  }

  case class Branch(name: String, children: List[Node]) extends Node {


    def getDescendant(path: List[String]): Option[Node] = {
      val child = children.find(_.name==path.head)

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

  case class Leaf(name: String, rateID: String, templateID: String) extends Node

  private val productTree =
    Branch("Root",
      List(
        Branch(
          "Tobacco", List(
            Leaf("Cigarettes", "TOB/A1/CIGRT", "cigarettes"),
            Leaf("Rolling Tobacco", "TOB/A1/HAND", "tobacco"),
            Leaf("Chewing or Pipe Tobacco", "TOB/A1/OTHER", "tobacco"),
            Leaf("Cigars", "TOB/A1/CIGAR", "cigars"),
            Leaf("Cigarillos", "TOB/A1/CRILO", "tobacco")
          )
        ),
        Branch(
          "Alcohol", List(
            Leaf("Cider", "ALC/A1/CIDER", "alcohol"),
            Leaf("Spirits", "ALC/A1/O22", "alcohol"),
            Leaf("Beer", "ALC/A2/BEER", "alcohol"),
            Leaf("Wine","ALC/A3/WINE","alcohol"),
            Leaf("Sparkling or fortified wine and other alcoholic drinks" +
              "up to 22% alcohol by volume", "ALC/A1/U22", "alcohol")
          )
        ),
        Branch(
          "Other Goods", List(
            Leaf("Antiques and works of Art", "OGD/ART", "other-goods"),
            Leaf("Books and publications", "OGD/BKS/MISC", "other-goods"),
            Branch(
              "Carpets, cotton and fabrics", List(
                Leaf("Carpets", "OGD/CRPT", "other-goods"),
                Leaf("Cotton", "OGD/CLTHS/ADULT", "other-goods"),
                Leaf("Fabrics", "OGD/FBRIC", "other-goods")
              )
            ),
            Leaf("Children's car seats", "OGD/MOB/MISC", "other-goods"),
            Branch(
              "Clothing and footwear", List(
                Leaf("Children's clothing", "OGD/CLTHS/CHILD", "other-goods"),
                Leaf("Footwear", "OGD/FOOTW", "other-goods"),
                Leaf("All other clothing", "OGD/CLTHS/ADULT", "other-goods")

              )
            ),
            Branch(
              "Disability equipment and mobility aids", List(
                Leaf("Disability equipment", "OGD/BKS/MISC", "other-goods"),
                Leaf("Mobility aids", "OGD/MOB/MISC", "other-goods")
              )
            ),
            Branch(
              "Electronic devices", List(
                Leaf("Televisions", "OGD/DIGI/TV", "other-goods"),
                Leaf("All other electronic devices", "OGD/DIGI/MISC", "other-goods")
              )
            ),
            Leaf("Furniture", "ODG/ORN/MISC", "other-goods"),
            Leaf("Games and sport equipment", "OGD/SPORT", "other-goods"),
            Branch(
              "Glassware and ornaments", List(
                Leaf("Glassware", "OGD/GLASS", "other-goods"),
                Leaf("Ornaments", "OGD/ORN/MISC", "other-goods")
              )
            ),
            Leaf("Jewellery","OGD/DIGI/MISC","other-goods"),
            Branch(
              "Metals and wood products", List(
                Leaf("Metals", "OGD/ORN/MISC", "other-goods"),
                Leaf("Wood products", "OGD/ORN/MISC", "other-goods")
              )
            ),
            Leaf("Perfumes and cosmetics", "OGD/COSMT", "other-goods"),
            Leaf("Protective helmets", "OGD/BKS/MISC", "other-goods"),
            Leaf("Sanitary products", "OGD/MOB/MISC", "other-goods"),
            Leaf("Stop smoking products", "OGD/MOB/MISC", "other-goods"),
            Leaf("Tableware", "OGD/TABLE", "other-goods"),
            Leaf("Watches and clocks", "OGD/ORN/MISC", "other-goods"),
            Leaf("Anything else", "OGD/OTHER", "other-goods")
          )
        )
      )
    )

  def getProducts: Branch = {
    productTree
  }
}
