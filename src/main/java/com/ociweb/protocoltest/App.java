package com.ociweb.protocoltest;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.HdrHistogram.Histogram;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ociweb.pronghorn.pipe.util.StreamRegulator;
import com.ociweb.pronghorn.util.CPUMonitor;
import com.ociweb.protocoltest.data.SequenceExampleA;
import com.ociweb.protocoltest.data.SequenceExampleAFactory;
import com.ociweb.protocoltest.data.build.SequenceExampleAFuzzGenerator;
import com.ociweb.protocoltest.speedTest.*;
import com.ociweb.protocoltest.protobuf.speed.PBSpeedConsumer;
import com.ociweb.protocoltest.protobuf.speed.PBSpeedProducer;
import com.ociweb.protocoltest.protobuf.size.PBSizeConsumer;
import com.ociweb.protocoltest.protobuf.size.PBSizeProducer;

public class App {

    //Put this line at the top of every class and be sure to change the Class name to that of the class in question.
    private static final Logger log = LoggerFactory.getLogger(App.class);

    public enum TestType {
        PBSpeed,
        PBSize
    }

    public static void main(String[] args) {

        log.info("Hello World, we are running...");

//        SequenceExampleAFactory testDataFactory = new SequenceExampleAFuzzGenerator();
//
//        //NOTE: this is how objects are fetched for writing.
//        SequenceExampleA writeMe = testDataFactory.nextObject();
        
        
        
        int totalMessageCount = 100000; //large fixed value for running the test
        Histogram histogram = new Histogram(3600000000000L, 3);
        
        long termination_wait = 240; //Seconds to wait for test to complete
        
        long bitPerSecond = 100L*1024L*1024L*1024L;
        int maxWrittenChunksInFlight = 10;
        int maxWrittenChunkSizeInBytes= 50*1024;
        StreamRegulator regulator = new StreamRegulator(bitPerSecond, maxWrittenChunksInFlight, maxWrittenChunkSizeInBytes);

        CPUMonitor cpuMonitor = new CPUMonitor(100);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        Runnable p,c;

        TestType type = TestType.PBSpeed;
        switch (type) {
        case PBSize:
            System.out.println("Running Protobuf Size Test");
            p = new PBSizeProducer(regulator, totalMessageCount);
            c = new PBSizeConsumer(regulator, totalMessageCount, histogram);
            break;
        case PBSpeed:
            System.out.println("Running Protobuf Speed Test");
            p = new PBSpeedProducer(regulator, totalMessageCount);
            c = new PBSpeedConsumer(regulator, totalMessageCount, histogram);
            break;
        default:
            System.out.println("Running Default Speed Test");
            p = new Producer(regulator, totalMessageCount);
            c = new Consumer(regulator, totalMessageCount, histogram);
        }


        long startTime = System.currentTimeMillis();

        cpuMonitor.start();
        executor.execute(p);
        executor.execute(c);

        executor.shutdown();//prevent any new submissions to execution service but let those started run.

        try {
            if (!executor.awaitTermination(termination_wait, TimeUnit.SECONDS)) {
                log.error("test time out, no valid results");
                System.exit(-1);
            }
        } catch (InterruptedException e) {
            //Nothing to do Just exit
        }
        Histogram cpuHist = cpuMonitor.stop();

        long totalBytesSent =regulator.getBytesWritten();
        long durationInMs = System.currentTimeMillis()-startTime;

        long bitsSent = totalBytesSent * 8L;
        float mBitsPerSec = (1000L*bitsSent)/(float)(durationInMs*1024*1024);
        float kBitsPerSec = (1000L*bitsSent)/(float)(durationInMs*1024);


        System.out.println("Latency Value in microseconds");
        histogram.outputPercentileDistribution(System.out, 1000.0);

        System.out.println();
        System.out.println("Process CPU Usage (All threads started by this Java instance)");
        cpuHist.outputPercentileDistribution(System.out, CPUMonitor.UNIT_SCALING_RATIO);

        log.info("Total duration {}ms",durationInMs);
        log.info("TotalBytes {}",totalBytesSent);

        log.info("{} Kbps",kBitsPerSec);
        log.info("{} Mbps",mBitsPerSec);
    }


}
