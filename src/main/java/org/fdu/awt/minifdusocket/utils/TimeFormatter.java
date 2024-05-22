package org.fdu.awt.minifdusocket.utils;

/**
 * @author ZMark
 * @date 2024/5/23 上午2:27
 */
public class TimeFormatter {

    /**
     * 格式化时长
     *
     * @param durationInSec 时长(秒)
     * @return 格式化后的时长（hh:mm:ss）
     */
    public static String formatDuration(Long durationInSec) {
        Long hours = durationInSec / 3600;
        Long minutes = (durationInSec % 3600) / 60;
        Long seconds = durationInSec % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
