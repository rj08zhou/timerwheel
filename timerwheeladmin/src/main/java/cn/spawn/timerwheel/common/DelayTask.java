package cn.spawn.timerwheel.common;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DelayTask {

    @JSONField(ordinal = 1, name="Key")
    private String key ;
    @JSONField(ordinal = 2, name="Delay")
    private int delay ;
    @JSONField(ordinal = 3, name="Circle")
    private int circle ;
    @JSONField(ordinal = 4, name="MonitorRule")
    private String monitorRule ;
    @JSONField(ordinal = 5, name="ProcessTime")
    private long processTime ;
    @JSONField(ordinal = 6, name="Timestamp")
    private long timestamp ;
    @JSONField(ordinal = 7, name="ExpiredTime")
    private long expiredTime ;

}
