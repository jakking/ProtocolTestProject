package com.ociweb.protocoltest.protobuf.speed;

import java.io.IOException;
import java.io.OutputStream;
//import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.*;
import com.ociweb.pronghorn.pipe.DataOutputBlobWriter;
import com.ociweb.pronghorn.pipe.RawDataSchema;
import com.ociweb.pronghorn.pipe.util.StreamRegulator;
import com.ociweb.protocoltest.App;
import com.ociweb.protocoltest.data.SequenceExampleA;
import com.ociweb.protocoltest.data.SequenceExampleAFactory;
import com.ociweb.protocoltest.data.SequenceExampleASample;
import com.ociweb.protocoltest.data.build.SequenceExampleAFuzzGenerator;
import com.ociweb.protocoltest.protobuf.speed.PBSpeedQueryProvider.PBQuery;

public class PBSpeedProducer implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(PBSpeedProducer.class);

    private final StreamRegulator regulator;
    private final int count;
    private final SequenceExampleAFactory testDataFactory;

    public PBSpeedProducer(StreamRegulator regulator, int count, SequenceExampleAFactory testSentDataFactory) {
        this.regulator = regulator;
        this.count = count;
        this.testDataFactory = testSentDataFactory;
    }

    public int getSerializedSize(final SequenceExampleASample sample) {
      int size;// = memoizedSampleSizes.get(sample.getId());
      size = 0;
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(1, sample.getId());
        size += com.google.protobuf.CodedOutputStream
          .computeInt64Size(2, sample.getTime());
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(3, sample.getMeasurement());
        size += com.google.protobuf.CodedOutputStream
          .computeInt32Size(4, sample.getAction());

      return size;
    }

    private int memoizedSerializedSize = -1;
    public int getSerializedSize(final SequenceExampleA sample) {
        int size = memoizedSerializedSize;
        if (size != -1) return size;

        size = 0;
        size += com.google.protobuf.CodedOutputStream
            .computeInt32Size(1, sample.getUser());
          size += com.google.protobuf.CodedOutputStream
            .computeInt32Size(2, sample.getYear());
          size += com.google.protobuf.CodedOutputStream
            .computeInt32Size(3, sample.getMonth());
          size += com.google.protobuf.CodedOutputStream
            .computeInt32Size(4, sample.getDate());
          size += com.google.protobuf.CodedOutputStream
            .computeInt32Size(5, sample.getSampleCount());
        memoizedSerializedSize = size;
        return size;
      }

    public void writeTo(com.google.protobuf.CodedOutputStream output, final SequenceExampleASample sample)
            throws java.io.IOException {
        output.writeInt32(1, sample.getId());
        output.writeInt64(2, sample.getTime());
        output.writeInt32(3, sample.getMeasurement());
        output.writeInt32(4, sample.getAction());
    }

    public void writeTo(final com.google.protobuf.CodedOutputStream output, final SequenceExampleA sample)
            throws java.io.IOException {
//        getSerializedSize();
        output.writeInt32(1, sample.getUser());
        output.writeInt32(2, sample.getYear());
        output.writeInt32(3, sample.getMonth());
        output.writeInt32(4, sample.getDate());
        output.writeInt32(5, sample.getSampleCount());
        for (int i = 0; i < sample.getSamples().size(); i++) {
            output.writeTag(6, WireFormat.WIRETYPE_LENGTH_DELIMITED);
            output.writeRawVarint32(getSerializedSize(sample.getSamples().get(i)));
            writeTo(output, sample.getSamples().get(i));
        }
    }
    final int DEFAULT_BUFFER_SIZE = 4096;

    public void writeTo(final OutputStream output, final SequenceExampleA sample) throws IOException {
        final int dataLength = getSerializedSize(sample);
        final int bufferSize = dataLength > DEFAULT_BUFFER_SIZE ? DEFAULT_BUFFER_SIZE : dataLength;
        final CodedOutputStream codedOutput =
            CodedOutputStream.newInstance(output, bufferSize);
        writeTo(codedOutput, sample);
        codedOutput.flush();
        output.close();
        memoizedSerializedSize = -1;
      }

//    private void writeDelimitedTo(final OutputStream output, final SequenceExampleA sample) throws IOException {
//        final int serialized = getSerializedSize(sample);
////        System.out.println("My Query: "+sample.getUser()+" Serialized Size: "+serialized);
//        final int dataLength = CodedOutputStream.computeRawVarint32Size(serialized) + serialized;
//        final int bufferSize = dataLength > DEFAULT_BUFFER_SIZE ?
//                  DEFAULT_BUFFER_SIZE : dataLength;
//        final CodedOutputStream codedOutput =
//            CodedOutputStream.newInstance(output, bufferSize);
//        codedOutput.writeRawVarint32(serialized);
//        writeTo(codedOutput, sample);
//        codedOutput.flush();
//        memoizedSerializedSize = -1;
////        Arrays.fill(memoizedSampleSizes, -1);
//    }

    @Override
    public void run() {
        try {

            OutputStream out = regulator.getOutputStream();
            DataOutputBlobWriter<RawDataSchema> blobWriter = regulator.getBlobWriter();
            long lastNow = 0;

           
//            Arrays.fill(memoizedSampleSizes, -1);
            PBQuery.Builder query_builder = PBQuery.newBuilder();
            PBQuery.PBSample.Builder sample_builder = PBQuery.PBSample.newBuilder();
            int i = count;
            while (i>0) {
                while (regulator.hasRoomForChunk() && --i>=0) { //Note we are only dec when there is room for write

                    lastNow = App.recordSentTime(lastNow, blobWriter);
                    
                    //NOTE: this is how objects are fetched for writing.
                    SequenceExampleA writeMe = testDataFactory.nextObject();
                    switch (App.pbSpeedRunType) {
                    case DirectSend:
//                      writeDelimitedTo(out, writeMe);
                        writeTo(out, writeMe);
                        out.close();
                        break;
                    case Translate:
                    default:
                        query_builder.setUser(writeMe.getUser())
                          .setYear(writeMe.getYear())
                          .setMonth(writeMe.getMonth())
                          .setDate(writeMe.getDate())
                          .setSampleCount(writeMe.getSampleCount());

                        for (SequenceExampleASample sample : writeMe.getSamples()) {
                            query_builder.addSamples(sample_builder
                                .setId(sample.getId())
                                .setTime(sample.getTime())
                                .setMeasurement(sample.getMeasurement())
                                .setAction(sample.getAction())
                                .build());
      //                      System.out.println("Sample: "+sample_builder.getId()+" serializedSize: "+sample_builder.build().getSerializedSize());
                            sample_builder.clear();
                        }

                 //       query_builder.build();
                 //       System.out.println("Query: "+query_builder.getUser()+" serializedSize: "+query_builder.build().getSerializedSize());
//
//                        query_builder.build().writeDelimitedTo(out);
                        query_builder.build().writeTo(out);
                        out.close();

                        query_builder.clear();
                    }

                }
                App.commmonWait(); //we are faster than the consumer
            }


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        log.info("PBSpeedProducer finished");
    }
}
