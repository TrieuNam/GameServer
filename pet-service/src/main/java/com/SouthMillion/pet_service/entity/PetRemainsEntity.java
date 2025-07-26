package com.SouthMillion.pet_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.SouthMillion.dto.pet.PetRemainsDTO;

@Entity
@Table(name = "pet_remains")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetRemainsEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    private Integer seq;      // Thứ tự di vật (relic sequence)
    private Integer grade;    // Phẩm chất
    private Integer level;    // Cấp độ
    private Integer exp;      // Kinh nghiệm
    private Integer clothId;  // ID của trang phục (nếu có)
    private Integer index;    // Index trong bag hoặc slot

    // Nếu cần, thêm createdAt, updatedAt...

    public static PetRemainsDTO fromEntity(PetRemainsEntity entity) {
        return PetRemainsDTO.builder()
                .seq(entity.getSeq())
                .grade(entity.getGrade())
                .level(entity.getLevel())
                .exp(entity.getExp())
                .clothId(entity.getClothId())
                .index(entity.getIndex())
                .build();
    }

    public static PetRemainsEntity toEntity(PetRemainsDTO dto, Long userId) {
        return PetRemainsEntity.builder()
                .userId(userId)
                .seq(dto.getSeq())
                .grade(dto.getGrade())
                .level(dto.getLevel())
                .exp(dto.getExp())
                .clothId(dto.getClothId())
                .index(dto.getIndex())
                .build();
    }
}