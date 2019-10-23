package services

import javax.inject.Singleton
import models.{ProductTreeBranch, ProductTreeLeaf}


@Singleton
class ProductTreeService {

  private val productTree =
    ProductTreeBranch("root", "Root",
      List(
        ProductTreeBranch("alcohol",
          "label.alcohol", List(
            ProductTreeLeaf("beer", "label.alcohol.beer", "ALC/A2/BEER", "alcohol", List("L-BEER")),
            ProductTreeBranch("cider", "label.alcohol.cider",
              List(
                ProductTreeLeaf("non-sparkling-cider", "label.alcohol.cider.non-sparkling-cider", "ALC/A1/CIDER", "alcohol", List("L-ALCOTH")),
                ProductTreeLeaf("sparkling-cider", "label.alcohol.cider.sparkling-cider", "ALC/A1/CIDERU5SP", "alcohol", List("L-ALCOTH")),
                ProductTreeLeaf("sparkling-cider-up", "label.alcohol.cider.sparkling-cider-up", "ALC/A1/CIDERU8SP", "alcohol", List("L-ALCOTH"))
              )
            ),
            ProductTreeLeaf("sparkling-wine", "label.alcohol.sparkling-wine", "ALC/A1/WINESP", "alcohol", List("L-WINE", "L-WINESP")),
            ProductTreeLeaf("spirits", "label.alcohol.spirits", "ALC/A1/O22", "alcohol", List("L-SPIRIT")),
            ProductTreeLeaf("wine", "label.alcohol.wine","ALC/A3/WINE","alcohol", List("L-WINE")),
            ProductTreeLeaf("other", "label.alcohol.other", "ALC/A1/U22", "alcohol", List("L-ALCOTH"))
          )
        ),
        ProductTreeBranch("tobacco",
          "label.tobacco", List(
            ProductTreeLeaf("cigarettes", "label.tobacco.cigarettes", "TOB/A1/CIGRT", "cigarettes", List("L-CIGRT")),
            ProductTreeLeaf("cigarillos", "label.tobacco.cigarillos", "TOB/A1/CRILO", "cigars", List("L-CRILO")),
            ProductTreeLeaf("cigars", "label.tobacco.cigars", "TOB/A1/CIGAR", "cigars", List("L-CIGAR")),
            ProductTreeLeaf("chewing-tobacco", "label.tobacco.chewing-tobacco", "TOB/A1/OTHER", "tobacco", List("L-LOOSE")),
            ProductTreeLeaf("rolling-tobacco", "label.tobacco.rolling-tobacco", "TOB/A1/HAND", "tobacco", List("L-LOOSE"))
          )
        ),
        ProductTreeBranch("other-goods",
          "label.other-goods", List(
            ProductTreeBranch("adult", "label.other-goods.adult",
              List(
                ProductTreeLeaf("adult-clothing", "label.other-goods.adult.adult-clothing", "OGD/CLTHS/ADULT", "other-goods", Nil),
                ProductTreeLeaf("adult-footwear", "label.other-goods.adult.adult-footwear", "OGD/FOOTW", "other-goods", Nil)
              )
            ),
            ProductTreeLeaf("antiques", "label.other-goods.antiques", "OGD/ART", "other-goods", Nil),
            ProductTreeLeaf("books", "label.other-goods.books", "OGD/BKS/MISC", "other-goods", Nil),
            ProductTreeBranch("carpets-fabric",
              "label.other-goods.carpets-fabric", List(
                ProductTreeLeaf("carpets", "label.other-goods.carpets-fabric.carpets", "OGD/CRPT", "other-goods", Nil),
                ProductTreeLeaf("fabrics", "label.other-goods.carpets-fabric.fabrics", "OGD/FBRIC", "other-goods", Nil)
              )
            ),
            ProductTreeLeaf("car-seats", "label.other-goods.car-seats", "OGD/MOB/MISC", "other-goods", Nil),
            ProductTreeBranch("childrens",
              "label.other-goods.childrens", List(
                ProductTreeLeaf("childrens-clothing", "label.other-goods.childrens.childrens-clothing", "OGD/CLTHS/CHILD", "other-goods", Nil),
                ProductTreeLeaf("childrens-footwear", "label.other-goods.childrens.childrens-footwear", "OGD/CLTHS/CHILD", "other-goods", Nil)
              )
            ),
            ProductTreeLeaf("disability-equipment", "label.other-goods.disability-equipment", "OGD/BKS/MISC", "other-goods", Nil),
            ProductTreeBranch("electronic-devices",
              "label.other-goods.electronic-devices", List(
                ProductTreeLeaf("televisions", "label.other-goods.electronic-devices.televisions", "OGD/DIGI/TV", "other-goods", Nil),
                ProductTreeLeaf("other", "label.other-goods.electronic-devices.other", "OGD/DIGI/MISC", "other-goods", Nil)
              )
            ),
            ProductTreeLeaf("furniture", "label.other-goods.furniture", "OGD/ORN/MISC", "other-goods", Nil),
            ProductTreeLeaf("glassware", "label.other-goods.glassware", "OGD/GLASS", "other-goods", Nil),
            ProductTreeLeaf("heated-tobacco", "label.other-goods.heated-tobacco","OGD/HTB","other-goods", Nil),
            ProductTreeLeaf("jewellery", "label.other-goods.jewellery","OGD/ORN/MISC","other-goods", Nil),
            ProductTreeLeaf("mobility-aids", "label.other-goods.mobility-aids", "OGD/MOB/MISC", "other-goods", Nil),
            ProductTreeLeaf("perfumes-cosmetics", "label.other-goods.perfumes-cosmetics", "OGD/COSMT", "other-goods", Nil),
            ProductTreeLeaf("protective-helmets", "label.other-goods.protective-helmets", "OGD/BKS/MISC", "other-goods", Nil),
            ProductTreeLeaf("sanitary-products", "label.other-goods.sanitary-products", "OGD/MOB/MISC", "other-goods", Nil),
            ProductTreeLeaf("stop-smoking-products", "label.other-goods.stop-smoking-products", "OGD/MOB/MISC", "other-goods", Nil),
            ProductTreeLeaf("tableware", "label.other-goods.tableware", "OGD/TABLE", "other-goods", Nil),
            ProductTreeLeaf("watches-clocks", "label.other-goods.watches-clocks", "OGD/ORN/MISC", "other-goods", Nil),
            ProductTreeLeaf("other", "label.other-goods.other", "OGD/OTHER", "other-goods", Nil)
          )
        )
      )
    )

  def getProducts: ProductTreeBranch = {
    productTree
  }

}
