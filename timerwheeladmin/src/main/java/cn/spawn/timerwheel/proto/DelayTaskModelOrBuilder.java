// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: proto/timerwheel.proto

package cn.spawn.timerwheel.proto;

public interface DelayTaskModelOrBuilder extends
    // @@protoc_insertion_point(interface_extends:model.DelayTaskModel)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>string key = 1;</code>
   * @return The key.
   */
  java.lang.String getKey();
  /**
   * <code>string key = 1;</code>
   * @return The bytes for key.
   */
  com.google.protobuf.ByteString
      getKeyBytes();

  /**
   * <code>int32 delay = 2;</code>
   * @return The delay.
   */
  int getDelay();

  /**
   * <code>int32 circle = 3;</code>
   * @return The circle.
   */
  int getCircle();

  /**
   * <code>string monitorRule = 4;</code>
   * @return The monitorRule.
   */
  java.lang.String getMonitorRule();
  /**
   * <code>string monitorRule = 4;</code>
   * @return The bytes for monitorRule.
   */
  com.google.protobuf.ByteString
      getMonitorRuleBytes();

  /**
   * <code>int64 processTime = 5;</code>
   * @return The processTime.
   */
  long getProcessTime();

  /**
   * <code>int64 timestamp = 6;</code>
   * @return The timestamp.
   */
  long getTimestamp();

  /**
   * <code>int64 expiredTime = 7;</code>
   * @return The expiredTime.
   */
  long getExpiredTime();
}
