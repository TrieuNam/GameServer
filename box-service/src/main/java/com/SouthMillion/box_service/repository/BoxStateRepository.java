package com.SouthMillion.box_service.repository;

import com.SouthMillion.box_service.enity.BoxState;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface BoxStateRepository extends JpaRepository<BoxState, Long> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select b from BoxState b where b.roleId = :roleId")
	Optional<BoxState> findByRoleIdForUpdate(@Param("roleId") Long roleId);

	@Modifying
	@Transactional
	@Query(value = """
			INSERT IGNORE INTO box_state (
				role_id,
				box_level,
				box_buy_times,
				level_up_end_epoch,
				level_fetch_flag,
				open_box_total,
				last_open_is_five,
				pending_json,
				shi_zhuang_num,
				arena_item_num,
				daily_ymd,
				last_open_epoch
			) VALUES (
				:roleId,
				1,
				0,
				0,
				0,
				0,
				0,
				NULL,
				0,
				0,
				NULL,
				0
			)
			""", nativeQuery = true)
	int insertDefaultIfAbsent(@Param("roleId") Long roleId);

	@Modifying
	@Transactional
	@Query(value = """
			UPDATE box_state
			SET pending_json = :newPendingJson
			WHERE role_id = :roleId
			  AND (
				(:expectedPendingJson IS NULL AND pending_json IS NULL)
				OR pending_json = :expectedPendingJson
			  )
			""", nativeQuery = true)
	int updatePendingJsonIfMatches(
			@Param("roleId") Long roleId,
			@Param("expectedPendingJson") String expectedPendingJson,
			@Param("newPendingJson") String newPendingJson
	);
}


