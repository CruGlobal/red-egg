package org.cru.redegg;

import org.cru.redegg.recording.api.NoOpParameterSanitizer;
import org.cru.redegg.recording.api.ParameterSanitizer;
import org.cru.redegg.recording.api.RecorderFactory;
import org.cru.redegg.recording.api.WebErrorRecorder;
import org.cru.redegg.test.AnswerWithSelf;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
* @author Matt Drees
*/
@ApplicationScoped
public class RecordingMocks
{

    @Produces
    @Mock
    RecorderFactory factory;

    @Produces
    WebErrorRecorder recorder;

    @Produces
    ParameterSanitizer sanitizer = new NoOpParameterSanitizer();

    @PostConstruct
    public void init()
    {
        MockitoAnnotations.initMocks(this);
        recorder = mock(WebErrorRecorder.class, new AnswerWithSelf(WebErrorRecorder.class));
        reset();
    }

    public void reset()
    {
        // we use Mockito.reset() instead of building new mocks, because the servlet listener is only initialized
        // once for this test class, and there is no easy way to modify its reference to a new mock
        Mockito.reset(recorder, factory);
        when(factory.getRecorder()).thenReturn(recorder);
        when(factory.getWebRecorder()).thenReturn(recorder);
        when(recorder.getErrorLink()).thenReturn(Optional.empty());
    }

}
