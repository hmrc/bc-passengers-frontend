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
            ProductTreeBranch("cider", "Cider",
              List(
                ProductTreeLeaf("non-sparkling-cider", "Non-sparkling cider", "ALC/A1/CIDER", "alcohol"),
                ProductTreeLeaf("sparkling-cider", "Sparkling cider (1.3% to 5.5% alcohol)", "ALC/A1/CIDERU5SP", "alcohol"),
                ProductTreeLeaf("sparkling-cider-up", "Sparkling cider (5.6% to 8.4% alcohol)", "ALC/A1/CIDERU8SP", "alcohol")
              )
            ),
            ProductTreeLeaf("sparkling", "Sparkling wine (such as Champagne or Prosecco)", "ALC/A1/WINESP", "alcohol"),
            ProductTreeLeaf("spirits", "Spirits (including gin, vodka and alcohol above 22%)", "ALC/A1/O22", "alcohol"),
            ProductTreeLeaf("wine", "Wine","ALC/A3/WINE","alcohol"),
            ProductTreeLeaf("other", "All other alcoholic drinks (including port, sherry and alcohol up to 22%)", "ALC/A1/U22", "alcohol")
          )
        ),
        ProductTreeBranch("tobacco",
          "Tobacco", List(
            ProductTreeLeaf("cigarettes", "Cigarettes", "TOB/A1/CIGRT", "cigarettes"),
            ProductTreeLeaf("cigarillos", "Cigarillos", "TOB/A1/CRILO", "cigars"),
            ProductTreeLeaf("cigars", "Cigars", "TOB/A1/CIGAR", "cigars"),
            ProductTreeLeaf("chewing", "Pipe or chewing tobacco", "TOB/A1/OTHER", "tobacco"),
            ProductTreeLeaf("rolling", "Rolling tobacco", "TOB/A1/HAND", "tobacco")
          )
        ),
        ProductTreeBranch("other-goods",
          "Other Goods", List(
            ProductTreeBranch("adult", "Adult clothing and footwear",
              List(
                ProductTreeLeaf("adult-clothing", "Adult clothing", "OGD/CLTHS/ADULT", "other-goods"),
                ProductTreeLeaf("adult-footwear", "Adult footwear", "OGD/FOOTW", "other-goods")
              )
            ),
            ProductTreeLeaf("antiques", "Antiques and works of art", "OGD/ART", "other-goods"),
            ProductTreeLeaf("books", "Books and publications", "OGD/BKS/MISC", "other-goods"),
            ProductTreeBranch("carpets-fabric",
              "Carpets and fabrics", List(
                ProductTreeLeaf("carpets", "Carpets", "OGD/CRPT", "other-goods"),
                ProductTreeLeaf("fabrics", "Fabrics", "OGD/FBRIC", "other-goods")
              )
            ),
            ProductTreeLeaf("car-seats", "Children’s car seats", "OGD/MOB/MISC", "other-goods"),
            ProductTreeBranch("childrens-clothing",
              "Children's clothing and footwear", List(
                ProductTreeLeaf("childrens", "Children’s clothing", "OGD/CLTHS/CHILD", "other-goods"),
                ProductTreeLeaf("footwear", "Children’s Footwear", "OGD/CLTHS/CHILD", "other-goods")

              )
            ),
            ProductTreeLeaf("disability", "Disability equipment", "OGD/BKS/MISC", "other-goods"),
            ProductTreeBranch("electronic-devices",
              "Electronic devices", List(
                ProductTreeLeaf("televisions", "Televisions", "OGD/DIGI/TV", "other-goods"),
                ProductTreeLeaf("other", "All other electronic devices", "OGD/DIGI/MISC", "other-goods")
              )
            ),
            ProductTreeLeaf("furniture", "Furniture", "OGD/ORN/MISC", "other-goods"),
            ProductTreeBranch("glassware-ornaments",
              "Glassware and ornaments", List(
                ProductTreeLeaf("glassware", "Glassware", "OGD/GLASS", "other-goods"),
                ProductTreeLeaf("ornaments", "Ornaments", "OGD/ART", "other-goods")
              )
            ),
            ProductTreeLeaf("jewellery", "Jewellery","OGD/ORN/MISC","other-goods"),
            ProductTreeLeaf("mobility", "Mobility aids", "OGD/MOB/MISC", "other-goods"),
            ProductTreeLeaf("perfumes-cosmetics", "Perfumes and cosmetics", "OGD/COSMT", "other-goods"),
            ProductTreeLeaf("protective-helmets", "Protective helmets", "OGD/BKS/MISC", "other-goods"),
            ProductTreeLeaf("sanitary-products", "Sanitary products", "OGD/MOB/MISC", "other-goods"),
            ProductTreeLeaf("stop-smoking", "Stop smoking products", "OGD/MOB/MISC", "other-goods"),
            ProductTreeLeaf("tableware", "Tableware and kitchenware", "OGD/TABLE", "other-goods"),
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
