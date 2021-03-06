package com.ociweb.protocoltest.template;

import java.io.OutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.pronghorn.pipe.DataOutputBlobWriter;
import com.ociweb.pronghorn.pipe.RawDataSchema;
import com.ociweb.pronghorn.pipe.util.StreamRegulator;
import com.ociweb.protocoltest.App;
import com.ociweb.protocoltest.data.SequenceExampleA;
import com.ociweb.protocoltest.data.SequenceExampleAFactory;
import com.ociweb.protocoltest.data.build.SequenceExampleAFuzzGenerator;

public class EmptyProducer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(EmptyProducer.class);

    private final StreamRegulator regulator;
    private final int count;
    private final SequenceExampleAFactory testDataFactory;

    public EmptyProducer(StreamRegulator regulator, int count, SequenceExampleAFactory testSentDataFactory) {
        this.regulator = regulator;
        this.count = count;
        this.testDataFactory = testSentDataFactory;
    }
    
    @Override
    public void run() {
        try {

            OutputStream out = regulator.getOutputStream();
            DataOutputBlobWriter<RawDataSchema> blobWriter = regulator.getBlobWriter();
            long lastNow = 0;

            
            SequenceExampleA writeMe = testDataFactory.nextObject();            
            int i = count;
            while (i>0) {
                while (regulator.hasRoomForChunk() && --i>=0) { //Note we are only dec when there is room for write
                    lastNow = App.recordSentTime(lastNow, blobWriter);

                    //TODO: write the writeMeObject to the out
                    
                    
                    
                    writeMe = testDataFactory.nextObject();   
                }
                Thread.yield(); //we are faster than the consumer
            }


        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        log.info("Empty producer finished");
    }
}
