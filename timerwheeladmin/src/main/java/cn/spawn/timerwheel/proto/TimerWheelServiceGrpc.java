package cn.spawn.timerwheel.proto;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.25.0)",
    comments = "Source: proto/timerwheel.proto")
public final class TimerWheelServiceGrpc {

  private TimerWheelServiceGrpc() {}

  public static final String SERVICE_NAME = "model.TimerWheelService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<cn.spawn.timerwheel.proto.AddTaskRequest,
      cn.spawn.timerwheel.proto.AddTaskResponse> getSendMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Send",
      requestType = cn.spawn.timerwheel.proto.AddTaskRequest.class,
      responseType = cn.spawn.timerwheel.proto.AddTaskResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<cn.spawn.timerwheel.proto.AddTaskRequest,
      cn.spawn.timerwheel.proto.AddTaskResponse> getSendMethod() {
    io.grpc.MethodDescriptor<cn.spawn.timerwheel.proto.AddTaskRequest, cn.spawn.timerwheel.proto.AddTaskResponse> getSendMethod;
    if ((getSendMethod = TimerWheelServiceGrpc.getSendMethod) == null) {
      synchronized (TimerWheelServiceGrpc.class) {
        if ((getSendMethod = TimerWheelServiceGrpc.getSendMethod) == null) {
          TimerWheelServiceGrpc.getSendMethod = getSendMethod =
              io.grpc.MethodDescriptor.<cn.spawn.timerwheel.proto.AddTaskRequest, cn.spawn.timerwheel.proto.AddTaskResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Send"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  cn.spawn.timerwheel.proto.AddTaskRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  cn.spawn.timerwheel.proto.AddTaskResponse.getDefaultInstance()))
              .setSchemaDescriptor(new TimerWheelServiceMethodDescriptorSupplier("Send"))
              .build();
        }
      }
    }
    return getSendMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static TimerWheelServiceStub newStub(io.grpc.Channel channel) {
    return new TimerWheelServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static TimerWheelServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new TimerWheelServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static TimerWheelServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new TimerWheelServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class TimerWheelServiceImplBase implements io.grpc.BindableService {

    /**
     */
    public void send(cn.spawn.timerwheel.proto.AddTaskRequest request,
        io.grpc.stub.StreamObserver<cn.spawn.timerwheel.proto.AddTaskResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSendMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getSendMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                cn.spawn.timerwheel.proto.AddTaskRequest,
                cn.spawn.timerwheel.proto.AddTaskResponse>(
                  this, METHODID_SEND)))
          .build();
    }
  }

  /**
   */
  public static final class TimerWheelServiceStub extends io.grpc.stub.AbstractStub<TimerWheelServiceStub> {
    private TimerWheelServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private TimerWheelServiceStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TimerWheelServiceStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new TimerWheelServiceStub(channel, callOptions);
    }

    /**
     */
    public void send(cn.spawn.timerwheel.proto.AddTaskRequest request,
        io.grpc.stub.StreamObserver<cn.spawn.timerwheel.proto.AddTaskResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getSendMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class TimerWheelServiceBlockingStub extends io.grpc.stub.AbstractStub<TimerWheelServiceBlockingStub> {
    private TimerWheelServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private TimerWheelServiceBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TimerWheelServiceBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new TimerWheelServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public cn.spawn.timerwheel.proto.AddTaskResponse send(cn.spawn.timerwheel.proto.AddTaskRequest request) {
      return blockingUnaryCall(
          getChannel(), getSendMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class TimerWheelServiceFutureStub extends io.grpc.stub.AbstractStub<TimerWheelServiceFutureStub> {
    private TimerWheelServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private TimerWheelServiceFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected TimerWheelServiceFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new TimerWheelServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<cn.spawn.timerwheel.proto.AddTaskResponse> send(
        cn.spawn.timerwheel.proto.AddTaskRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getSendMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEND = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final TimerWheelServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(TimerWheelServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND:
          serviceImpl.send((cn.spawn.timerwheel.proto.AddTaskRequest) request,
              (io.grpc.stub.StreamObserver<cn.spawn.timerwheel.proto.AddTaskResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class TimerWheelServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    TimerWheelServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return cn.spawn.timerwheel.proto.TimerWheelRpcService.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("TimerWheelService");
    }
  }

  private static final class TimerWheelServiceFileDescriptorSupplier
      extends TimerWheelServiceBaseDescriptorSupplier {
    TimerWheelServiceFileDescriptorSupplier() {}
  }

  private static final class TimerWheelServiceMethodDescriptorSupplier
      extends TimerWheelServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    TimerWheelServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (TimerWheelServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new TimerWheelServiceFileDescriptorSupplier())
              .addMethod(getSendMethod())
              .build();
        }
      }
    }
    return result;
  }
}
