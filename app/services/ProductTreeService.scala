package services

import javax.inject.Singleton
import models.{OtherGoodsSearchItem, ProductListEntry, ProductPath, ProductTreeBranch, ProductTreeLeaf}


@Singleton
class ProductTreeService {

  val productTree: ProductTreeBranch =
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

  val otherGoodsSearchItems: List[OtherGoodsSearchItem] = List(
    OtherGoodsSearchItem("label.other-goods.adult_belt", ProductPath("other-goods/adult/adult-clothing")),
    OtherGoodsSearchItem("label.other-goods.adult_clothing", ProductPath("other-goods/adult/adult-clothing")),
    OtherGoodsSearchItem("label.other-goods.adult_shoes", ProductPath("other-goods/adult/adult-footwear")),
    OtherGoodsSearchItem("label.other-goods.adult_trainers", ProductPath("other-goods/adult/adult-footwear")),
    OtherGoodsSearchItem("label.other-goods.adult_footwear", ProductPath("other-goods/adult/adult-footwear")),
    OtherGoodsSearchItem("label.other-goods.aftershave", ProductPath("other-goods/perfumes-cosmetics")),
    OtherGoodsSearchItem("label.other-goods.antiques", ProductPath("other-goods/antiques")),
    OtherGoodsSearchItem("label.other-goods.apple_watch", ProductPath("other-goods/watches-clocks")),
    OtherGoodsSearchItem("label.other-goods.bag", ProductPath("other-goods/other")),
    OtherGoodsSearchItem("label.other-goods.briefcase", ProductPath("other-goods/other")),
    OtherGoodsSearchItem("label.other-goods.book", ProductPath("other-goods/books")),
    OtherGoodsSearchItem("label.other-goods.bracelet", ProductPath("other-goods/jewellery")),
    OtherGoodsSearchItem("label.other-goods.camera", ProductPath("other-goods/electronic-devices/other")),
    OtherGoodsSearchItem("label.other-goods.camera_equipment", ProductPath("other-goods/electronic-devices/other")),
    OtherGoodsSearchItem("label.other-goods.childrens_clothing", ProductPath("other-goods/childrens/childrens-clothing")),
    OtherGoodsSearchItem("label.other-goods.childrens_footwear", ProductPath("other-goods/childrens/childrens-footwear")),
    OtherGoodsSearchItem("label.other-goods.china", ProductPath("other-goods/tableware")),
    OtherGoodsSearchItem("label.other-goods.chocolate", ProductPath("other-goods/other")),
    OtherGoodsSearchItem("label.other-goods.cologne", ProductPath("other-goods/perfumes-cosmetics")),
    OtherGoodsSearchItem("label.other-goods.computer", ProductPath("other-goods/electronic-devices/other")),
    OtherGoodsSearchItem("label.other-goods.cosmetics", ProductPath("other-goods/perfumes-cosmetics")),
    OtherGoodsSearchItem("label.other-goods.cuckoo_clock", ProductPath("other-goods/watches-clocks")),
    OtherGoodsSearchItem("label.other-goods.dell_laptop", ProductPath("other-goods/electronic-devices/other")),
    OtherGoodsSearchItem("label.other-goods.earrings", ProductPath("other-goods/jewellery")),
    OtherGoodsSearchItem("label.other-goods.eyewear", ProductPath("other-goods/other")),
    OtherGoodsSearchItem("label.other-goods.food", ProductPath("other-goods/other")),
    OtherGoodsSearchItem("label.other-goods.fabric", ProductPath("other-goods/carpets-fabric/fabrics")),
    OtherGoodsSearchItem("label.other-goods.fragrance", ProductPath("other-goods/perfumes-cosmetics")),
    OtherGoodsSearchItem("label.other-goods.gold", ProductPath("other-goods/jewellery")),
    OtherGoodsSearchItem("label.other-goods.golf_equipment", ProductPath("other-goods/other")),
    OtherGoodsSearchItem("label.other-goods.handbag", ProductPath("other-goods/other")),
    OtherGoodsSearchItem("label.other-goods.iPad", ProductPath("other-goods/electronic-devices/other")),
    OtherGoodsSearchItem("label.other-goods.iPhone", ProductPath("other-goods/electronic-devices/other")),
    OtherGoodsSearchItem("label.other-goods.jeans", ProductPath("other-goods/adult/adult-clothing")),
    OtherGoodsSearchItem("label.other-goods.jewellery", ProductPath("other-goods/jewellery")),
    OtherGoodsSearchItem("label.other-goods.kindle", ProductPath("other-goods/electronic-devices/other")),
    OtherGoodsSearchItem("label.other-goods.laptop", ProductPath("other-goods/electronic-devices/other")),
    OtherGoodsSearchItem("label.other-goods.leather_goods", ProductPath("other-goods/other")),
    OtherGoodsSearchItem("label.other-goods.lingerie", ProductPath("other-goods/adult/adult-clothing")),
    OtherGoodsSearchItem("label.other-goods.macbook", ProductPath("other-goods/electronic-devices/other")),
    OtherGoodsSearchItem("label.other-goods.makeup", ProductPath("other-goods/perfumes-cosmetics")),
    OtherGoodsSearchItem("label.other-goods.mobile_phone", ProductPath("other-goods/electronic-devices/other")),
    OtherGoodsSearchItem("label.other-goods.nicotine_patch", ProductPath("other-goods/stop-smoking-products")),
    OtherGoodsSearchItem("label.other-goods.necklace", ProductPath("other-goods/jewellery")),
    OtherGoodsSearchItem("label.other-goods.ornament", ProductPath("other-goods/other")),
    OtherGoodsSearchItem("label.other-goods.painting", ProductPath("other-goods/antiques")),
    OtherGoodsSearchItem("label.other-goods.perfume", ProductPath("other-goods/perfumes-cosmetics")),
    OtherGoodsSearchItem("label.other-goods.prescription_glasses", ProductPath("other-goods/other")),
    OtherGoodsSearchItem("label.other-goods.porcelain", ProductPath("other-goods/other")),
    OtherGoodsSearchItem("label.other-goods.pottery", ProductPath("other-goods/other")),
    OtherGoodsSearchItem("label.other-goods.ring", ProductPath("other-goods/jewellery")),
    OtherGoodsSearchItem("label.other-goods.samsung_laptop", ProductPath("other-goods/electronic-devices/other")),
    OtherGoodsSearchItem("label.other-goods.samsung_mobile_phone", ProductPath("other-goods/electronic-devices/other")),
    OtherGoodsSearchItem("label.other-goods.soft-drink", ProductPath("other-goods/other")),
    OtherGoodsSearchItem("label.other-goods.sony_laptop", ProductPath("other-goods/electronic-devices/other")),
    OtherGoodsSearchItem("label.other-goods.sunglasses", ProductPath("other-goods/other")),
    OtherGoodsSearchItem("label.other-goods.sweets", ProductPath("other-goods/other")),
    OtherGoodsSearchItem("label.other-goods.tablet", ProductPath("other-goods/electronic-devices/other")),
    OtherGoodsSearchItem("label.other-goods.television", ProductPath("other-goods/electronic-devices/televisions")),
    OtherGoodsSearchItem("label.other-goods.underwear", ProductPath("other-goods/adult/adult-clothing")),
    OtherGoodsSearchItem("label.other-goods.watch", ProductPath("other-goods/watches-clocks")),

    // Additional values
    OtherGoodsSearchItem("label.other-goods.booster_seat", ProductPath("other-goods/car-seats")),
    OtherGoodsSearchItem("label.other-goods.cutlery", ProductPath("other-goods/tableware")),
    OtherGoodsSearchItem("label.other-goods.disability_apparatus", ProductPath("other-goods/disability-equipment")),
    OtherGoodsSearchItem("label.other-goods.glass", ProductPath("other-goods/glassware")),
    OtherGoodsSearchItem("label.other-goods.hygiene_products", ProductPath("other-goods/other")),
    OtherGoodsSearchItem("label.other-goods.heets", ProductPath("other-goods/heated-tobacco")),
    OtherGoodsSearchItem("label.other-goods.helmet", ProductPath("other-goods/protective-helmets")),
    OtherGoodsSearchItem("label.other-goods.iqos", ProductPath("other-goods/heated-tobacco")),
    OtherGoodsSearchItem("label.other-goods.kids_clothes", ProductPath("other-goods/childrens/childrens-clothing")),
    OtherGoodsSearchItem("label.other-goods.kids_shoes", ProductPath("other-goods/childrens/childrens-footwear")),
    OtherGoodsSearchItem("label.other-goods.mans_clothes", ProductPath("other-goods/adult/adult-clothing")),
    OtherGoodsSearchItem("label.other-goods.mans_shoes", ProductPath("other-goods/adult/adult-footwear")),
    OtherGoodsSearchItem("label.other-goods.rug", ProductPath("other-goods/carpets-fabric/carpets")),
    OtherGoodsSearchItem("label.other-goods.scents", ProductPath("other-goods/perfumes-cosmetics")),
    OtherGoodsSearchItem("label.other-goods.sofa", ProductPath("other-goods/furniture")),
    OtherGoodsSearchItem("label.other-goods.textiles", ProductPath("other-goods/carpets-fabric/fabrics")),
    OtherGoodsSearchItem("label.other-goods.tv", ProductPath("other-goods/electronic-devices/televisions")),
    OtherGoodsSearchItem("label.other-goods.womens_clothes", ProductPath("other-goods/adult/adult-clothing")),
    OtherGoodsSearchItem("label.other-goods.womens_shoes", ProductPath("other-goods/adult/adult-footwear"))
  ).sortBy(_.name)
}
