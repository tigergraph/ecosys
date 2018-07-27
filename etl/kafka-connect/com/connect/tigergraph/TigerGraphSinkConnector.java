/******************************************************************************
 * Copyright (c)  2015-2017, TigerGraph Inc.
 * All rights reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited
 * Proprietary and confidential
 ******************************************************************************/
package com.connect.tigergraph;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.connect.connector.Task;
import org.apache.kafka.connect.sink.SinkConnector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.connect.tigergraph.sink.TigerGraphSinkTask;
import com.connect.tigergraph.sink.TigerGraphSinkConfig;

public final class TigerGraphSinkConnector extends SinkConnector {
  private Map<String, String> configProps;

  public Class<? extends Task> taskClass() {
    return TigerGraphSinkTask.class;
  }

  @Override
  public List<Map<String, String>> taskConfigs(int maxTasks) {
    final List<Map<String, String>> configs = new ArrayList<>(maxTasks);
    for (int i = 0; i < maxTasks; ++i) {
      configs.add(configProps);
    }
    return configs;
  }

  @Override
  public void start(Map<String, String> props) {
    configProps = props;
  }

  @Override
  public void stop() {
  }

  @Override
  public ConfigDef config() {
    return TigerGraphSinkConfig.getconfig();
  }
  @Override
  public String version() {
    return "TigerGraphSinkConnector.V0.1";
  }
}
