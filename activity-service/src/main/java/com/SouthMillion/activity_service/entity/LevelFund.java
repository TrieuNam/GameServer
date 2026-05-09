package com.SouthMillion.activity_service.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "level_fund")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LevelFund {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "role_id", nullable = false, unique = true)
	private Long roleId;

	@Enumerated(EnumType.STRING)
	@Column(name = "phase_buy_flag", nullable = false)
	private FundState phaseBuyFlag;

	@Column(name = "common_fetch_flag", nullable = false)
	private Long commonFetchFlag;

	@Column(name = "senior_fetch_flag", nullable = false)
	private Long seniorFetchFlag;

	@UpdateTimestamp
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	public boolean isRewardClaimable(Map<String, Integer> levelThresholds) {
		return levelThresholds != null && !levelThresholds.isEmpty();
	}

	public void unlock() {
		if (this.phaseBuyFlag != FundState.UNLOCKED) {
			this.phaseBuyFlag = FundState.UNLOCKED;
		} else {
			throw new IllegalStateException("Already unlocked.");
		}
	}

	public void claim() {
		if (this.phaseBuyFlag == FundState.UNLOCKED) {
			this.phaseBuyFlag = FundState.CLAIMED;
		} else {
			throw new IllegalStateException("Invalid transition to CLAIMED.");
		}
	}

	public void complete() {
		if (this.phaseBuyFlag == FundState.CLAIMED) {
			this.phaseBuyFlag = FundState.COMPLETED;
		} else {
			throw new IllegalStateException("Invalid transition to COMPLETED.");
		}
	}
}