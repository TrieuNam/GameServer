package com.SouthMillion.box_service.repository;

import com.SouthMillion.box_service.enity.BoxSetting;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoxSettingRepository extends JpaRepository<BoxSetting, Long> {

	@Modifying
	@Transactional
	@Query(value = """
			INSERT IGNORE INTO box_setting (
				role_id,
				equip_eqality,
				open_five_mark,
				equip_cap_mark,
				equip_sell_mark,
				condition_first1,
				condition_first2,
				condition_second1,
				condition_second2,
				condition_first_mark,
				condition_second_mark,
				retain_mark,
				challenge_mark
			) VALUES (
				:roleId,
				0,
				0,
				1,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0,
				0
			)
			""", nativeQuery = true)
	int insertDefaultIfAbsent(@Param("roleId") Long roleId);
}
