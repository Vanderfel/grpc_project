package org.client;

import com.example.grpc.NumbersGeneratorGrpc;
import com.example.grpc.Service.NumbersRequest;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class Client {
    private static final Logger log = LoggerFactory.getLogger(Client.class);
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final int NUMBER_SEQUENCE_LIMIT = 50;
    private static final int REQUEST_FIRST_VALUE = 1;
    private static final int REQUEST_LAST_VALUE = 30;
    private long value = 0;

    public static void main(String[] args) throws InterruptedException {
        log.info("Numbers Client is starting...");

        ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(SERVER_HOST, SERVER_PORT).usePlaintext().build();

        NumbersGeneratorGrpc.NumbersGeneratorStub asyncClient = NumbersGeneratorGrpc.newStub(managedChannel);
        CountDownLatch latch = new CountDownLatch(1);
        new Client().action(asyncClient, latch);
        latch.await();

        log.info("Numbers Client is shutting down...");
        managedChannel.shutdown();
    }

    private void action(NumbersGeneratorGrpc.NumbersGeneratorStub asyncClient, CountDownLatch latch) {
        NumbersRequest request = makeRequest();

        RemoteClientStreamObserver remoteClientStreamObserver = new RemoteClientStreamObserver(latch);
        asyncClient.generateNumbers(request, remoteClientStreamObserver);

        for (int i = 0; i < NUMBER_SEQUENCE_LIMIT; i++) {
            long currentValue = getNextValue(remoteClientStreamObserver);
            log.info("currentValue: {}", currentValue);
            sleep();
        }
        log.info("The number sequence has ended");
    }

    private long getNextValue(RemoteClientStreamObserver remoteClientStreamObserver) {
        value = value + remoteClientStreamObserver.getCurrentValueAndReset() + 1;
        return value;
    }

    private void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static NumbersRequest makeRequest() {
        return NumbersRequest.newBuilder().setFirstValue(REQUEST_FIRST_VALUE).setLastValue(REQUEST_LAST_VALUE).build();
    }
}