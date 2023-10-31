package org.cru.redegg.reporting.rollbar;

import com.google.common.collect.Lists;
import com.rollbar.api.payload.data.Data;
import com.rollbar.api.payload.data.Data.Builder;
import com.rollbar.api.payload.data.body.Body;
import com.rollbar.api.payload.data.body.Trace;
import com.rollbar.api.payload.data.body.TraceChain;
import com.rollbar.notifier.transformer.Transformer;
import java.util.List;

public class TraceChainInverter implements Transformer {

    @Override
    public Data transform(Data data) {
        if (!(data.getBody().getContents() instanceof TraceChain)) {
            return data;
        }
        TraceChain traceChain = (TraceChain) data.getBody().getContents();
        final List<Trace> traces = traceChain.getTraces();

        TraceChain.Builder traceChainBuilder = new TraceChain.Builder(traceChain);
        traceChainBuilder.traces(Lists.reverse(traces));

        Body.Builder bodyBuilder = new Body.Builder(data.getBody());
        bodyBuilder.bodyContent(traceChainBuilder.build());

        Data.Builder dataBuilder = new Builder(data);
        return dataBuilder.body(bodyBuilder.build())
            .build();
    }
}
