package services

import javax.inject.Singleton
import models.{ProductTreeBranch, ProductTreeLeaf}


@Singleton
class ProductTreeService {

  private val productTree =
    ProductTreeBranch("root", "Root",
      List(
        ProductTreeBranch("alcohol",
          "Alcohol", List(
            ProductTreeLeaf("beer", "Beer", "ALC/A2/BEER", "alcohol"),
            ProductTreeLeaf("cider", "Cider", "ALC/A1/CIDER", "alcohol"),
            ProductTreeLeaf("sparkling", "Sparkling or fortified wine and other alcoholic drinks up to 22% alcohol by volume", "ALC/A1/U22", "alcohol"),
            ProductTreeLeaf("spirits", "Spirits", "ALC/A1/O22", "alcohol"),
            ProductTreeLeaf("wine", "Wine","ALC/A3/WINE","alcohol")
          )
        ),
        ProductTreeBranch("tobacco",
          "Tobacco", List(
            ProductTreeLeaf("cigarettes", "Cigarettes", "TOB/A1/CIGRT", "cigarettes"),
            ProductTreeLeaf("cigars", "Cigars", "TOB/A1/CIGAR", "cigars"),
            ProductTreeLeaf("cigarillos", "Cigarillos", "TOB/A1/CRILO", "cigars"),
            ProductTreeLeaf("rolling", "Rolling tobacco", "TOB/A1/HAND", "tobacco"),
            ProductTreeLeaf("chewing", "Chewing or pipe tobacco", "TOB/A1/OTHER", "tobacco")
          )
        ),
        ProductTreeBranch("other-goods",
          "Other Goods", List(
            ProductTreeLeaf("antiques", "Antiques and works of art", "OGD/ART", "other-goods"),
            ProductTreeLeaf("books", "Books and publications", "OGD/BKS/MISC", "other-goods"),
            ProductTreeBranch("carpets-cotton-fabric",
              "Carpets, cotton and fabrics", List(
                ProductTreeLeaf("carpets", "Carpets", "OGD/CRPT", "other-goods"),
                ProductTreeLeaf("cotton", "Cotton", "OGD/CLTHS/ADULT", "other-goods"),
                ProductTreeLeaf("fabrics", "Fabrics", "OGD/FBRIC", "other-goods")
              )
            ),
            ProductTreeLeaf("car-seats", "Children’s car seats", "OGD/MOB/MISC", "other-goods"),
            ProductTreeBranch("clothing",
              "Clothing and footwear", List(
                ProductTreeLeaf("childrens", "Children’s clothing", "OGD/CLTHS/CHILD", "other-goods"),
                ProductTreeLeaf("footwear", "Footwear", "OGD/FOOTW", "other-goods"),
                ProductTreeLeaf("other", "All other clothing", "OGD/CLTHS/ADULT", "other-goods")

              )
            ),
            ProductTreeBranch("disability-mobility",
              "Disability equipment and mobility aids", List(
                ProductTreeLeaf("disability", "Disability equipment", "OGD/BKS/MISC", "other-goods"),
                ProductTreeLeaf("mobility", "Mobility aids", "OGD/MOB/MISC", "other-goods")
              )
            ),
            ProductTreeBranch("electronic-devices",
              "Electronic devices", List(
                ProductTreeLeaf("televisions", "Televisions", "OGD/DIGI/TV", "other-goods"),
                ProductTreeLeaf("other", "All other electronic devices", "OGD/DIGI/MISC", "other-goods")
              )
            ),
            ProductTreeLeaf("furniture", "Furniture", "OGD/ORN/MISC", "other-goods"),
            ProductTreeLeaf("games-sports", "Games and sport equipment", "OGD/SPORT", "other-goods"),
            ProductTreeBranch("glassware-ornaments",
              "Glassware and ornaments", List(
                ProductTreeLeaf("glassware", "Glassware", "OGD/GLASS", "other-goods"),
                ProductTreeLeaf("ornaments", "Ornaments", "OGD/ORN/MISC", "other-goods")
              )
            ),
            ProductTreeLeaf("jewellery", "Jewellery","OGD/DIGI/MISC","other-goods"),
            ProductTreeBranch("metals-wood",
              "Metals and wood products", List(
                ProductTreeLeaf("metals", "Metals", "OGD/ORN/MISC", "other-goods"),
                ProductTreeLeaf("wood", "Wood products", "OGD/ORN/MISC", "other-goods")
              )
            ),
            ProductTreeLeaf("perfumes-cosmetics", "Perfumes and cosmetics", "OGD/COSMT", "other-goods"),
            ProductTreeLeaf("protective-helmets", "Protective helmets", "OGD/BKS/MISC", "other-goods"),
            ProductTreeLeaf("sanitary-products", "Sanitary products", "OGD/MOB/MISC", "other-goods"),
            ProductTreeLeaf("stop-smoking", "Stop smoking products", "OGD/MOB/MISC", "other-goods"),
            ProductTreeLeaf("tablewear", "Tableware", "OGD/TABLE", "other-goods"),
            ProductTreeLeaf("watches-clocks", "Watches and clocks", "OGD/ORN/MISC", "other-goods"),
            ProductTreeLeaf("other", "Anything else", "OGD/OTHER", "other-goods")
          )
        )
      )
    )

  def getProducts: ProductTreeBranch = {
    productTree
  }

}
