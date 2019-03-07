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
            ProductTreeLeaf("beer", "Beer", "ALC/A2/BEER", "alcohol", List("L-BEER")),
            ProductTreeBranch("cider", "Cider",
              List(
                ProductTreeLeaf("non-sparkling-cider", "Non-sparkling cider", "ALC/A1/CIDER", "alcohol", List("L-ALCOTH")),
                ProductTreeLeaf("sparkling-cider", "Sparkling cider (1.3% to 5.5% alcohol)", "ALC/A1/CIDERU5SP", "alcohol", List("L-ALCOTH")),
                ProductTreeLeaf("sparkling-cider-up", "Sparkling cider (5.6% to 8.4% alcohol)", "ALC/A1/CIDERU8SP", "alcohol", List("L-ALCOTH"))
              )
            ),
            ProductTreeLeaf("sparkling-wine", "Sparkling wine (such as Champagne or Prosecco)", "ALC/A1/WINESP", "alcohol", List("L-WINE", "L-WINESP")),
            ProductTreeLeaf("spirits", "Spirits (including gin, vodka and alcohol above 22%)", "ALC/A1/O22", "alcohol", List("L-SPIRIT")),
            ProductTreeLeaf("wine", "Wine","ALC/A3/WINE","alcohol", List("L-WINE")),
            ProductTreeLeaf("other", "All other alcoholic drinks (including port, sherry and alcohol up to 22%)", "ALC/A1/U22", "alcohol", List("L-ALCOTH"))
          )
        ),
        ProductTreeBranch("tobacco",
          "Tobacco", List(
            ProductTreeLeaf("cigarettes", "Cigarettes", "TOB/A1/CIGRT", "cigarettes", List("L-CIGRT")),
            ProductTreeLeaf("cigarillos", "Cigarillos", "TOB/A1/CRILO", "cigars", List("L-CRILO")),
            ProductTreeLeaf("cigars", "Cigars", "TOB/A1/CIGAR", "cigars", List("L-CIGAR")),
            ProductTreeLeaf("chewing-tobacco", "Pipe or chewing tobacco", "TOB/A1/OTHER", "tobacco", List("L-LOOSE")),
            ProductTreeLeaf("rolling-tobacco", "Rolling tobacco", "TOB/A1/HAND", "tobacco", List("L-LOOSE"))
          )
        ),
        ProductTreeBranch("other-goods",
          "Other Goods", List(
            ProductTreeBranch("adult", "Adult clothing and footwear",
              List(
                ProductTreeLeaf("adult-clothing", "Adult clothing", "OGD/CLTHS/ADULT", "other-goods", Nil),
                ProductTreeLeaf("adult-footwear", "Adult footwear", "OGD/FOOTW", "other-goods", Nil)
              )
            ),
            ProductTreeLeaf("antiques", "Antiques, collector’s pieces and works of art", "OGD/ART", "other-goods", Nil),
            ProductTreeLeaf("books", "Books and publications", "OGD/BKS/MISC", "other-goods", Nil),
            ProductTreeBranch("carpets-fabric",
              "Carpets and fabrics", List(
                ProductTreeLeaf("carpets", "Carpets", "OGD/CRPT", "other-goods", Nil),
                ProductTreeLeaf("fabrics", "Fabrics", "OGD/FBRIC", "other-goods", Nil)
              )
            ),
            ProductTreeLeaf("car-seats", "Children’s car seats", "OGD/MOB/MISC", "other-goods", Nil),
            ProductTreeBranch("childrens",
              "Children's clothing and footwear", List(
                ProductTreeLeaf("childrens-clothing", "Children’s clothing", "OGD/CLTHS/CHILD", "other-goods", Nil),
                ProductTreeLeaf("childrens-footwear", "Children’s Footwear", "OGD/CLTHS/CHILD", "other-goods", Nil)

              )
            ),
            ProductTreeLeaf("disability-equipment", "Disability equipment", "OGD/BKS/MISC", "other-goods", Nil),
            ProductTreeBranch("electronic-devices",
              "Electronic devices", List(
                ProductTreeLeaf("televisions", "Televisions", "OGD/DIGI/TV", "other-goods", Nil),
                ProductTreeLeaf("other", "All other electronic devices", "OGD/DIGI/MISC", "other-goods", Nil)
              )
            ),
            ProductTreeLeaf("furniture", "Furniture", "OGD/ORN/MISC", "other-goods", Nil),
            ProductTreeLeaf("glassware", "Glassware", "OGD/GLASS", "other-goods", Nil),
            ProductTreeLeaf("jewellery", "Jewellery","OGD/ORN/MISC","other-goods", Nil),
            ProductTreeLeaf("mobility-aids", "Mobility aids", "OGD/MOB/MISC", "other-goods", Nil),
            ProductTreeLeaf("perfumes-cosmetics", "Perfumes and cosmetics", "OGD/COSMT", "other-goods", Nil),
            ProductTreeLeaf("protective-helmets", "Protective helmets", "OGD/BKS/MISC", "other-goods", Nil),
            ProductTreeLeaf("sanitary-products", "Sanitary products", "OGD/MOB/MISC", "other-goods", Nil),
            ProductTreeLeaf("stop-smoking-products", "Stop smoking products", "OGD/MOB/MISC", "other-goods", Nil),
            ProductTreeLeaf("tableware", "Tableware and kitchenware", "OGD/TABLE", "other-goods", Nil),
            ProductTreeLeaf("watches-clocks", "Watches and clocks", "OGD/ORN/MISC", "other-goods", Nil),
            ProductTreeLeaf("other", "Anything else", "OGD/OTHER", "other-goods", Nil)
          )
        )
      )
    )

  def getProducts: ProductTreeBranch = {
    productTree
  }

}
