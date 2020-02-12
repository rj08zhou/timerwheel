package cn.spawn.timerwheel.rpc;

import java.util.List;
import java.util.concurrent.TimeUnit;


import cn.spawn.timerwheel.proto.AddTaskRequest;
import cn.spawn.timerwheel.proto.AddTaskResponse;
import cn.spawn.timerwheel.proto.DelayTaskModel;
import cn.spawn.timerwheel.proto.TimerWheelServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.netty.NettyChannelBuilder;

public class RpcClient {

    private final ManagedChannel channel;
    private final TimerWheelServiceGrpc.TimerWheelServiceBlockingStub blockingStub;

    public RpcClient(String host, int port){
        channel =  NettyChannelBuilder.forAddress(host, port).usePlaintext().build();
        blockingStub = TimerWheelServiceGrpc.newBlockingStub(channel);
    }

    public void shutdown()throws InterruptedException{
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    public int send(List<DelayTaskModel> tasks) {
        AddTaskRequest request = AddTaskRequest.newBuilder().addAllDelayTasks(tasks).build() ;
        AddTaskResponse response = blockingStub.send(request) ;
        return response.getCode() ;
    }

}
