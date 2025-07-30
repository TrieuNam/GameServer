package com.SouthMillion.item_service.service;

import com.SouthMillion.item_service.entity.MysteryShopStateEntity;
import com.SouthMillion.item_service.entity.ShopPurchaseEntity;
import com.SouthMillion.item_service.mapper.ShopMapper;
import com.SouthMillion.item_service.repository.MysteryShopStateRepository;
import com.SouthMillion.item_service.repository.ShopPurchaseRepository;
import com.SouthMillion.item_service.service.client.ConfigFeignClient;
import com.SouthMillion.item_service.service.client.UserResourceFeignClient;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.SouthMillion.dto.item.shop.ClothShopItemDTO;
import org.SouthMillion.dto.item.shop.MysteryShopItemDTO;
import org.SouthMillion.dto.item.shop.ShopItemDTO;
import org.SouthMillion.proto.Msgother.Msgother;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ShopService {
    @Autowired
    private ShopPurchaseRepository shopRepo;
    @Autowired
    private MysteryShopStateRepository mysteryRepo;

    @Autowired
    private ConfigFeignClient configFeign;

    @Autowired
    private UserResourceFeignClient resourceClient;

    private Map<Integer, ShopItemDTO> shopIndexMap;
    private Map<Integer, ClothShopItemDTO> clothSeqMap;
    private Map<Integer, MysteryShopItemDTO> mysteryIndexMap;
    private List<MysteryShopItemDTO> mysteryPool;

    @PostConstruct
    public void init() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

        // 1. shop_cfg.json
        JsonNode shopCfg = configFeign.getConfigFile("shop_cfg");
        shopIndexMap = new HashMap<>();
        if (shopCfg != null && shopCfg.has("shop")) {
            for (JsonNode node : shopCfg.get("shop")) {
                try {
                    ShopItemDTO dto = mapper.treeToValue(node, ShopItemDTO.class);
                    shopIndexMap.put(dto.getIndex(), dto);
                } catch (Exception e) {
                    // Có thể log lỗi chi tiết DTO bị fail
                    System.err.println("Load shop_cfg.json fail at node: " + node + ", error: " + e.getMessage());
                }
            }
        } else {
            throw new RuntimeException("shop_cfg.json missing or invalid");
        }

        // 2. cloth_shop.json
        JsonNode clothShop = configFeign.getConfigFile("cloth_shop");
        clothSeqMap = new HashMap<>();
        if (clothShop != null && clothShop.has("shop")) {
            for (JsonNode node : clothShop.get("shop")) {
                try {
                    ClothShopItemDTO dto = mapper.treeToValue(node, ClothShopItemDTO.class);
                    clothSeqMap.put(dto.getSeq(), dto);
                } catch (Exception e) {
                    System.err.println("Load cloth_shop.json fail at node: " + node + ", error: " + e.getMessage());
                }
            }
        } else {
            throw new RuntimeException("cloth_shop.json missing or invalid");
        }

        // 3. shop_shenmi.json
        JsonNode shopShenmi = configFeign.getConfigFile("shop_shenmi");
        List<MysteryShopItemDTO> mysteryList = new ArrayList<>();
        if (shopShenmi != null && shopShenmi.has("shop")) {
            for (JsonNode node : shopShenmi.get("shop")) {
                try {
                    MysteryShopItemDTO dto = mapper.treeToValue(node, MysteryShopItemDTO.class);
                    mysteryList.add(dto);
                } catch (Exception e) {
                    System.err.println("Load shop_shenmi.json fail at node: " + node + ", error: " + e.getMessage());
                }
            }
        } else {
            throw new RuntimeException("shop_shenmi.json missing or invalid");
        }
        mysteryIndexMap = mysteryList.stream()
                .collect(Collectors.toMap(MysteryShopItemDTO::getIndex, Function.identity()));
        mysteryPool = mysteryList;
    }



    public void buyItem(Long userId, int index, int num) {
        ShopItemDTO config = shopIndexMap.get(index);
        if (config == null) throw new RuntimeException("Invalid shop index");

        // Kiểm tra đủ tiền
        Integer costItemId = config.getExchangeItemId();
        Integer costNum = config.getExchangeItemNum() * num;
        if (!resourceClient.hasEnough(userId, costItemId, costNum))
            throw new RuntimeException("Not enough currency!");

        // Trừ tiền và cộng item
        resourceClient.deductResource(userId, costItemId, costNum);
        resourceClient.addItem(userId, config.getItemId(), config.getItemNum() * num);

        // Lưu lượt mua
        ShopPurchaseEntity purchase = shopRepo.findByUserIdAndShopIndex(userId, index)
                .orElseGet(() -> {
                    ShopPurchaseEntity e = new ShopPurchaseEntity();
                    e.setUserId(userId);
                    e.setShopIndex(index);
                    e.setBuyNum(0);
                    return e;
                });
        purchase.setBuyNum(purchase.getBuyNum() + num);
        purchase.setLastBuyTime(System.currentTimeMillis());
        shopRepo.save(purchase);
    }


    public Msgother.PB_SCShopInfo getShopInfo(Long userId) {
        List<ShopPurchaseEntity> list = shopRepo.findByUserId(userId);
        Msgother.PB_SCShopInfo.Builder builder = Msgother.PB_SCShopInfo.newBuilder();

        for (ShopPurchaseEntity e : list) {
            builder.addDataList(Msgother.PB_ShopData.newBuilder()
                    .setIndex(e.getShopIndex())
                    .setBuyNum(e.getBuyNum())
                    .build());
        }

        return builder.build();
    }


    public void buyClothItem(Long userId, int seq, int num) {
        ClothShopItemDTO config = clothSeqMap.get(seq);
        if (config == null) throw new RuntimeException("Invalid cloth shop seq");

        Integer costItemId = config.getBuyItem();
        Integer costNum = config.getBuyItemNum() * num;
        if (!resourceClient.hasEnough(userId, costItemId, costNum))
            throw new RuntimeException("Not enough currency!");

        resourceClient.deductResource(userId, costItemId, costNum);
        resourceClient.addItem(userId, config.getItemId(), config.getItemNum() * num);
    }


    public void operateMystery(Long userId, int opType, int param) {
        MysteryShopStateEntity state = mysteryRepo.findById(userId).orElseGet(() -> {
            MysteryShopStateEntity e = new MysteryShopStateEntity();
            e.setUserId(userId);
            e.setBuyFlag(0);
            e.setIndexList("");
            return mysteryRepo.save(e);
        });

        if (opType == 0) {
            // Làm mới
            List<Integer> selected = randomMysteryItems(mysteryPool, 4);
            state.setIndexList(selected.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",")));
            state.setBuyFlag(0);
        } else if (opType == 1) {
            // Mua
            int bit = 1 << param;
            state.setBuyFlag(state.getBuyFlag() | bit);

            List<Integer> indexList = parseIndexList(state.getIndexList());
            if (param >= indexList.size()) throw new RuntimeException("Invalid param");

            int index = indexList.get(param);
            MysteryShopItemDTO item = mysteryIndexMap.get(index);
            if (item == null) throw new RuntimeException("Mystery item not found");

            Integer costItemId = item.getExchangeItemId();
            Integer costNum = item.getExchangeItemNum();
            if (!resourceClient.hasEnough(userId, costItemId, costNum))
                throw new RuntimeException("Not enough currency!");

            resourceClient.deductResource(userId, costItemId, costNum);
            resourceClient.addItem(userId, item.getItemId(), item.getItemNum());
        }

        mysteryRepo.save(state);
    }


    public Msgother.PB_SCMysteryShopInfo getMysteryShopInfo(Long userId) {
        MysteryShopStateEntity e = mysteryRepo.findById(userId).orElseGet(() -> {
            MysteryShopStateEntity s = new MysteryShopStateEntity();
            s.setUserId(userId);
            s.setBuyFlag(0);
            s.setIndexList("0,1,2,3");
            return mysteryRepo.save(s);
        });

        Msgother.PB_SCMysteryShopInfo.Builder builder = Msgother.PB_SCMysteryShopInfo.newBuilder()
                .setBuyFlag(e.getBuyFlag());

        builder.addAllIndexList(parseIndexList(e.getIndexList()));
        return builder.build();
    }

    private List<Integer> parseIndexList(String s) {
        if (s == null || s.isEmpty()) return Collections.emptyList();
        return Arrays.stream(s.split(",")).map(Integer::parseInt).collect(Collectors.toList());
    }

    private List<Integer> randomMysteryItems(List<MysteryShopItemDTO> pool, int count) {
        List<Integer> result = new ArrayList<>();
        Random rand = new Random();
        Set<Integer> used = new HashSet<>();
        while (result.size() < count && used.size() < pool.size()) {
            int i = rand.nextInt(pool.size());
            if (!used.contains(i)) {
                used.add(i);
                result.add(pool.get(i).getIndex());
            }
        }
        return result;
    }

}
