package com.agent.tool.builtin;

import com.agent.tool.Tool;
import com.agent.tool.ToolParam;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class TimeTool {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z");

    @Tool(name = "get_current_time", description = "获取当前时间")
    public String getCurrentTime(
            @ToolParam(name = "timezone", description = "时区，如 Asia/Shanghai", required = false) String timezone) {
        ZoneId zoneId = (timezone != null && !timezone.isEmpty())
                ? ZoneId.of(timezone)
                : ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(zoneId);
        return now.format(FORMATTER);
    }
}
