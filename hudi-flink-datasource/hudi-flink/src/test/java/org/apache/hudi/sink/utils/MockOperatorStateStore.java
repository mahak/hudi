/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hudi.sink.utils;

import org.apache.hudi.adapter.OperatorStateStoreAdapter;

import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.state.OperatorStateStore;
import org.apache.flink.streaming.api.functions.sink.filesystem.TestUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * An {@link OperatorStateStore} for testing purpose.
 */
@SuppressWarnings("rawtypes")
public class MockOperatorStateStore implements OperatorStateStoreAdapter {
  private Map<String, TestUtils.MockListState> listStateMap;

  private final Map<Long, Map<String, TestUtils.MockListState>> historyStateMap;
  private Map<String, TestUtils.MockListState> lastSuccessStateMap;

  public MockOperatorStateStore() {
    this.listStateMap = new HashMap<>();
    this.historyStateMap = new HashMap<>();
    this.lastSuccessStateMap = new HashMap<>();
  }

  @Override
  public <K, V> BroadcastState<K, V> getBroadcastState(MapStateDescriptor<K, V> stateDescriptor) throws Exception {
    throw new UnsupportedOperationException("getBroadcastState is not supported yet");
  }

  @Override
  @SuppressWarnings("unchecked")
  public <S> ListState<S> getListState(ListStateDescriptor<S> stateDescriptor) throws Exception {
    String name = stateDescriptor.getName();
    listStateMap.putIfAbsent(name, new TestUtils.MockListState());
    return listStateMap.get(name);
  }

  @Override
  public <S> ListState<S> getUnionListState(ListStateDescriptor<S> stateDescriptor) throws Exception {
    throw new UnsupportedOperationException("getUnionListState is not supported yet");
  }

  @Override
  public Set<String> getRegisteredStateNames() {
    throw new UnsupportedOperationException("getRegisteredStateNames is not supported yet");
  }

  @Override
  public Set<String> getRegisteredBroadcastStateNames() {
    throw new UnsupportedOperationException("getRegisteredBroadcastStateNames is not supported yet");
  }

  public void checkpointBegin(long checkpointId) {
    Map<String, TestUtils.MockListState> copiedStates = Collections.unmodifiableMap(copyStates(listStateMap));
    historyStateMap.put(checkpointId, copiedStates);
  }

  public void checkpointSuccess(long checkpointId) {
    lastSuccessStateMap = historyStateMap.get(checkpointId);
  }

  public void rollBackToLastSuccessCheckpoint() {
    this.listStateMap = copyStates(lastSuccessStateMap);
  }

  @SuppressWarnings("unchecked")
  private Map<String, TestUtils.MockListState> copyStates(Map<String, TestUtils.MockListState> stateMap) {
    Map<String, TestUtils.MockListState> copiedStates = new HashMap<>();
    for (Map.Entry<String, TestUtils.MockListState> entry : stateMap.entrySet()) {
      TestUtils.MockListState copiedState = new TestUtils.MockListState();
      copiedState.addAll(entry.getValue().getBackingList());
      copiedStates.put(entry.getKey(), copiedState);
    }
    return copiedStates;
  }
}
