package com.tenx.ai.gateway;

import com.tenx.ai.gateway.model.SpeechRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SpeechRequestTest {

    @Test
    public void copiesRequestForResolvedProviderModel() {
        SpeechRequest request = new SpeechRequest();
        request.setModel("cosyvoice");
        request.setInput("hello");
        request.setVoice("default");
        request.setResponseFormat("wav");
        request.setSpeed(Double.valueOf(1.1));

        SpeechRequest copy = request.copyForModel("cosyvoice-v1");

        Assertions.assertEquals("cosyvoice-v1", copy.getModel());
        Assertions.assertEquals("hello", copy.getInput());
        Assertions.assertEquals("default", copy.getVoice());
        Assertions.assertEquals("wav", copy.getResponseFormat());
        Assertions.assertEquals(Double.valueOf(1.1), copy.getSpeed());
    }
}
