package com.SouthMillion.activity_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
@Entity
@Table(name = "ra_fill_blank")
public class FillBlank {

    @Id
    @Column(name = "role_id")
    private Long roleId;

    /** Current puzzle index (0-based) */
    @Column(name = "puzzle_index", nullable = false)
    private Integer puzzleIndex = 0;

    /** Bitmask of correctly filled blanks in current puzzle */
    @Column(name = "filled_mask", nullable = false)
    private Long filledMask = 0L;

    /** Total puzzles completed */
    @Column(name = "completed_count", nullable = false)
    private Integer completedCount = 0;

    /** fetch_flag bitmask for milestone rewards */
    @Column(name = "fetch_flag", nullable = false)
    private Long fetchFlag = 0L;

    /** Daily hint tickets remaining */
    @Column(name = "hint_count", nullable = false)
    private Integer hintCount = 3;
}
