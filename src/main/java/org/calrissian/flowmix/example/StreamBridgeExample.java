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
package org.calrissian.flowmix.example;

import org.calrissian.flowmix.example.support.ExampleRunner;
import org.calrissian.flowmix.example.support.FlowProvider;
import org.calrissian.flowmix.model.Event;
import org.calrissian.flowmix.model.Flow;
import org.calrissian.flowmix.model.builder.FlowBuilder;
import org.calrissian.flowmix.support.Criteria;
import org.calrissian.flowmix.support.Function;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
/**
 * An example showing how a stream can output directly to another stream without sending its output events to the
 * standard output component (stream bridging). This essentially leads to streams feeding directly into other
 * streams, allowing for things like joins.
 */
public class StreamBridgeExample implements FlowProvider {

  @Override
  public List<Flow> getFlows() {
    Flow flow = new FlowBuilder()

      .id("flow")
      .flowDefs()
        .stream("stream1")
          .filter().criteria(new Criteria() {
              @Override
              public boolean matches(Event event) {
                return true;
              }
            }).end()
        .endStream(false, "stream2")   // send ALL results to stream2 and not to standard output
        .stream("stream2", false)      // don't read any events from standard input
          .filter().criteria(new Criteria() {
              @Override
              public boolean matches(Event event) {
                return true;
              }
            }).end()
          .select().fields("key4").end()
          .each().function(new Function() {
            @Override
            public List<Event> execute(Event event) {
              return singletonList(event);
            }
          }).end()
        .endStream()
      .endDefs()
    .createFlow();

    return asList(new Flow[]{flow});
  }

  public static void main(String args[]) {
    new ExampleRunner(new StreamBridgeExample()).run();
  }
}
