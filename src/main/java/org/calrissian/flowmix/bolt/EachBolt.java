/*
 * Copyright (C) 2014 The Calrissian Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.calrissian.flowmix.bolt;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import org.calrissian.flowmix.FlowmixFactory;
import org.calrissian.flowmix.model.EachOp;
import org.calrissian.flowmix.model.Event;
import org.calrissian.flowmix.model.Flow;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.calrissian.flowmix.Constants.*;
import static org.calrissian.flowmix.spout.MockFlowLoaderSpout.FLOW_LOADER_STREAM;

/**
 * A each allows an event to be processed, mutated, split, or transformed.
 */
public class EachBolt extends BaseRichBolt {

    Map<String,Flow> flows;
    OutputCollector collector;

    @Override
    public void prepare(Map map, TopologyContext topologyContext, OutputCollector outputCollector) {
        this.collector = outputCollector;
        flows = new HashMap<String, Flow>();
    }

    @Override
    public void execute(Tuple tuple) {

        if(FLOW_LOADER_STREAM.equals(tuple.getSourceStreamId())) {
            for(Flow flow : (Collection<Flow>)tuple.getValue(0))
                flows.put(flow.getId(), flow);
        } else if(!"tick".equals(tuple.getSourceStreamId())){
            String flowId = tuple.getStringByField(FLOW_ID);
            Event event = (Event) tuple.getValueByField(EVENT);
            int idx = tuple.getIntegerByField(FLOW_OP_IDX);
            idx++;
            String streamName = tuple.getStringByField(STREAM_NAME);
            String previousStream = tuple.getStringByField(LAST_STREAM);

            Flow flow = flows.get(flowId);

            if(flow != null) {
                EachOp functionOp = (EachOp) flow.getStream(streamName).getFlowOps().get(idx);

                String nextStream = idx+1 < flow.getStream(streamName).getFlowOps().size() ? flow.getStream(streamName).getFlowOps().get(idx + 1).getComponentName() : "output";
                List<Event> events = functionOp.getFunction().execute(event);

                if((nextStream.equals("output") && flow.getStream(streamName).isStdOutput()) || !nextStream.equals("output")) {
                  if (events != null) {
                    for (Event newEvent : events)
                      collector.emit(nextStream, tuple, new Values(flowId, newEvent, idx, streamName, previousStream));
                  }
                }

                // send directly to any non std output streams that may be configured
                if (nextStream.equals("output") && flow.getStream(streamName).getOutputs() != null) {
                  for (String output : flow.getStream(streamName).getOutputs()) {
                    if (events != null) {
                      for (Event newEvent : events) {
                        String outputStream = flow.getStream(output).getFlowOps().get(0).getComponentName();
                        collector.emit(outputStream, tuple, new Values(flowId, newEvent, -1, output, streamName));
                      }
                    }
                  }
                }
              }
        }

      collector.ack(tuple);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        FlowmixFactory.declareOutputStreams(outputFieldsDeclarer);
    }
}
