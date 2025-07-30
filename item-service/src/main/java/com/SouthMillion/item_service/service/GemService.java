package com.SouthMillion.item_service.service;

import com.SouthMillion.item_service.entity.GemDrawingEntity;
import com.SouthMillion.item_service.entity.GemInlayEntity;
import com.SouthMillion.item_service.repository.GemDrawingRepository;
import com.SouthMillion.item_service.service.client.ConfigFeignClient;
import com.SouthMillion.item_service.service.client.UserResourceFeignClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.SouthMillion.dto.item.gem.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GemService {
    @Autowired
    private ConfigFeignClient configFeignClient;

    @Autowired
    private GemDrawingRepository gemDrawingRepository;


    private static final String GEMSTONE_JSON = "gemstone";
    private static final String GEMSTONE_DRAWING_JSON = "gemstone_drawing";
    private static final String GEM_CFG_JSON = "gem_cfg";

    private UserResourceFeignClient userFeignClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // --- 1. Get All Gems ---

    public List<GemstoneDTO> getAllGems() {
        JsonNode root = configFeignClient.getConfigFile(GEMSTONE_JSON);
        JsonNode arr = root.get("gemstone");
        try {
            return Arrays.asList(objectMapper.treeToValue(arr, GemstoneDTO[].class));
        } catch (Exception e) {
            throw new RuntimeException("Parse gemstone.json failed", e);
        }
    }

    // --- 2. Get Gem By Id ---
    public GemstoneDTO getGemById(Integer id) {
        return getAllGems().stream()
                .filter(gem -> gem.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // --- 3. Get All Drawings ---
    public List<GemstoneDrawingDTO> getAllDrawings() {
        JsonNode root = configFeignClient.getConfigFile(GEMSTONE_DRAWING_JSON);
        JsonNode arr = root.get("gemstone_drawing");
        try {
            return Arrays.asList(objectMapper.treeToValue(arr, GemstoneDrawingDTO[].class));
        } catch (Exception e) {
            throw new RuntimeException("Parse gemstone_drawing.json failed", e);
        }
    }

    // --- 4. Get Drawing By Id ---
    public GemstoneDrawingDTO getDrawingById(Integer id) {
        return getAllDrawings().stream()
                .filter(draw -> draw.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    // --- 5. Get Compound Config ---
    public List<GemCompoundDTO> getAllGemCompounds() {
        JsonNode root = configFeignClient.getConfigFile(GEM_CFG_JSON);
        JsonNode arr = root.get("compound");
        try {
            return Arrays.asList(objectMapper.treeToValue(arr, GemCompoundDTO[].class));
        } catch (Exception e) {
            throw new RuntimeException("Parse gem_cfg.json (compound) failed", e);
        }
    }

    // --- 6. Get DrawingUp Config ---
    public List<GemDrawingUpDTO> getAllDrawingUp() {
        JsonNode root = configFeignClient.getConfigFile(GEM_CFG_JSON);
        JsonNode arr = root.get("drawing_up");
        try {
            // Vì GemDrawingUpDTO có List<GemDrawingUpAttrDTO>, bạn cần custom TypeReference
            return objectMapper.readValue(arr.traverse(), new TypeReference<List<GemDrawingUpDTO>>() {
            });
        } catch (Exception e) {
            throw new RuntimeException("Parse gem_cfg.json (drawing_up) failed", e);
        }
    }

    // --- 7. Demo Logic Mua Gem ---
    public void buyGem(Long userId, List<Integer> itemIds) {
        List<GemstoneDTO> allGems = getAllGems();
        int totalPrice = 0;
        int priceItemId = 0;
        int priceAmount = 0;
        Map<Integer, GemstoneDTO> buyMap = new HashMap<>();

        for (Integer itemId : itemIds) {
            GemstoneDTO gem = allGems.stream()
                    .filter(g -> g.getId().equals(itemId))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Gem not found: " + itemId));
            totalPrice += gem.getSellprice();
            // Đếm từng item (nâng cấp thành nhiều loại gem nếu cần)
            buyMap.put(itemId, gem);
        }
        // ** Giả sử: tất cả gem dùng cùng một loại tiền, ví dụ: Vàng (id=40001) **
        priceItemId = 40001; // Bạn chỉnh đúng ID resource, có thể lấy từ gem.getPriceItemId() nếu phức tạp
        priceAmount = totalPrice;

        // 1. Kiểm tra user có đủ resource không
        boolean enough = userFeignClient.hasEnough(userId, priceItemId, priceAmount);
        if (!enough) {
            throw new RuntimeException("Not enough resource (" + priceAmount + ")");
        }
        // 2. Trừ tài nguyên
        userFeignClient.deductResource(userId, priceItemId, priceAmount);

        // 3. Thêm đá quý về kho cho user (có thể tăng số lượng nếu đã có sẵn)
        for (Map.Entry<Integer, GemstoneDTO> entry : buyMap.entrySet()) {
            userFeignClient.addItem(userId, entry.getKey(), 1); // Mỗi item mua 1 viên, tuỳ logic front-end/truyền lên
        }
    }


    public List<GemDrawingDTO> getAllDrawingsByUser(Long userId) {
        // Lấy toàn bộ bản vẽ gắn đá của user từ repository (hoặc Redis, hoặc DB)
        List<GemDrawingEntity> entities = gemDrawingRepository.findAllByUserId(userId);
        List<GemDrawingDTO> dtos = new ArrayList<>();
        for (GemDrawingEntity entity : entities) {
            GemDrawingDTO dto = new GemDrawingDTO();
            dto.setLevel(entity.getLevel());
            dto.setGemList(mapInlayList(entity.getGemList())); // convert từ entity sang DTO
            dtos.add(dto);
        }
        return dtos;
    }

    private List<GemInlayDTO> mapInlayList(List<GemInlayEntity> inlayEntities) {
        List<GemInlayDTO> result = new ArrayList<>();
        if (inlayEntities != null) {
            for (GemInlayEntity entity : inlayEntities) {
                GemInlayDTO dto = new GemInlayDTO();
                dto.setItemId(entity.getItemId());
                dto.setPos(entity.getPos());
                result.add(dto);
            }
        }
        return result;
    }


    public GemDrawingDTO getDrawingByUser(Long userId, Long drawingId) {
        GemDrawingEntity entity = gemDrawingRepository.findByUserIdAndId(userId, drawingId);
        return entity != null ? mapEntityToDTO(entity) : null;
    }


    public void oneKeyUpgradeGem(Long userId, List<Integer> itemIds) {
        // Ví dụ: Nâng cấp level tất cả bản vẽ của user nếu có các item hợp lệ.
        List<GemDrawingEntity> drawings = gemDrawingRepository.findAllByUserId(userId);
        if (drawings.isEmpty()) return;

        for (GemDrawingEntity drawing : drawings) {
            // Logic: Kiểm tra các itemIds có nằm trong gemList không
            List<GemInlayEntity> gemList = drawing.getGemList();
            boolean canUpgrade = false;

            if (gemList != null && !gemList.isEmpty()) {
                for (GemInlayEntity inlay : gemList) {
                    if (itemIds.contains(inlay.getItemId())) {
                        canUpgrade = true;
                        // Có thể thay thế viên đá ở đây nếu business yêu cầu
                    }
                }
            }
            if (canUpgrade) {
                // Business: Tăng cấp cho bản vẽ này
                int oldLevel = drawing.getLevel() == null ? 1 : drawing.getLevel();
                drawing.setLevel(oldLevel + 1);

                // Ví dụ: Cập nhật một viên đá bất kỳ lên cấp cao hơn
                if (gemList != null && !gemList.isEmpty()) {
                    for (GemInlayEntity inlay : gemList) {
                        if (itemIds.contains(inlay.getItemId())) {
                            // Giả sử: mỗi lần upgrade tăng itemId lên 1 (minh họa)
                            inlay.setItemId(inlay.getItemId() + 1);
                        }
                    }
                }
            }
        }
        gemDrawingRepository.saveAll(drawings);
    }

    // Mapping entity -> dto
    private GemDrawingDTO mapEntityToDTO(GemDrawingEntity entity) {
        GemDrawingDTO dto = new GemDrawingDTO();
        dto.setLevel(entity.getLevel());
        dto.setGemList(mapInlayList(entity.getGemList()));
        return dto;
    }

}
