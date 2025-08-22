package com.SouthMillion.config_service.core;

import org.SouthMillion.dto.config.ConfigFileData;

import java.util.List;
import java.util.Optional;

public interface ConfigStore {
    Optional<ConfigFileData> getFileByKey(String pathKey);

    Optional<ConfigFileData> getByRelativePath(String relativePath); // e.g. "gameworld/battlemonstermanager.xml"

    String currentRevision();

    // indexers for listing
    List<String> listItems();

    List<String> listLogic();         // include "randactivity/xxx"

    List<String> listDrops();         // ids

    List<String> listGlobal();

    List<String> listSkill();

    List<String> listMonster();

    List<String> listServerConfig();  // names (no extension)

    void reload(); // rescan & recompute revision
}