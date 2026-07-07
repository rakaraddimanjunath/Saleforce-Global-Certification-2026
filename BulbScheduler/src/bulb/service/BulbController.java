package bulb.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Abstraction over the physical bulbs.
 *
 * This simulation logs every ON/OFF event with a timestamp. To drive REAL
 * hardware, replace the body of {@link #setBulb(int, boolean)} with your
 * relay / GPIO / smart-plug call, for example:
 *
 *   - Raspberry Pi (via pi4j):      gpio.setState(pin, on ? HIGH : LOW);
 *   - Arduino over Serial:          serialPort.writeBytes(...)
 *   - Wi-Fi smart bulb (HTTP/MQTT): httpClient.send(request...) / mqttClient.publish(...)
 *
 * Everything else in this project (the GUI, ScheduleEngine, HolidayService)
 * stays the same regardless of which real backend you plug in here.
 */
public class BulbController {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");
    private final boolean[] bulbOn = new boolean[8]; // 1-indexed, index 0 unused
    private Consumer<String> logSink = System.out::println;

    public void setLogSink(Consumer<String> logSink) {
        this.logSink = logSink;
    }

    public boolean isOn(int bulbId) {
        return bulbOn[bulbId];
    }

    /** Turns every bulb OFF (used before applying a new day's pattern). */
    public void allOff() {
        for (int i = 1; i <= 7; i++) {
            setBulb(i, false);
        }
    }

    /** Applies the given set of bulb ids as ON, everything else OFF. */
    public void applyPattern(int[] onBulbs) {
        allOff();
        for (int id : onBulbs) {
            setBulb(id, true);
        }
    }

    public void setBulb(int bulbId, boolean on) {
        bulbOn[bulbId] = on;
        // ---- Hook point for real hardware goes here ----
        log("Bulb " + bulbId + " -> " + (on ? "ON" : "OFF"));
    }

    private void log(String msg) {
        String line = "[" + LocalDateTime.now().format(TS) + "] " + msg;
        logSink.accept(line);
    }
}
