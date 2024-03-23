package org.server;

import com.example.grpc.NumbersGeneratorGrpc;
import com.example.grpc.Service.NumbersRequest;
import com.example.grpc.Service.NumbersResponse;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class ServiceImpl extends NumbersGeneratorGrpc.NumbersGeneratorImplBase {
    private static final Logger log = LoggerFactory.getLogger(ServiceImpl.class);

    @Override
    public void generateNumbers(NumbersRequest request, StreamObserver<NumbersResponse> responseObserver) {
        log.info("request for the new sequence of numbers, firstValue:{}, lastValue:{}", request.getFirstValue(),
                request.getLastValue());

        AtomicLong currentValue = new AtomicLong(request.getFirstValue());
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
        Runnable task = () -> {
            long value = currentValue.incrementAndGet();
            NumbersResponse response = NumbersResponse.newBuilder().setValue((int) value).build();
            responseObserver.onNext(response);
            if (value == request.getLastValue()) {
                executor.shutdown();
                responseObserver.onCompleted();
                log.info("sequence of numbers finished");
            }
        };
        executor.scheduleAtFixedRate(task, 0, 2, TimeUnit.SECONDS);
    }
}