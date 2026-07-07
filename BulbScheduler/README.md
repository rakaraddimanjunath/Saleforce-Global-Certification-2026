# Bulb Trigger Scheduler (Java Swing)

An interactive Java GUI that decides which of **7 bulbs** to switch ON each day,
based on the day of the week, and adjusts automatically around **real
government holidays**.

## Rules implemented

| Day       | Pattern |
|-----------|---------|
| Monday    | ODD bulbs (1, 3, 5, 7) |
| Tuesday   | EVEN bulbs (2, 4, 6) |
| Wednesday | ODD |
| Thursday  | EVEN |
| Friday    | ODD |
| Saturday  | EVEN |
| Sunday    | ODD  *(Sunday is treated as a normal working day, not skipped)* |

**Holiday rule:** if a day in the plan turns out to be a government holiday,
no bulbs trigger that day, and that day's slot shifts forward onto the next
working day — cascading the rest of the week by one slot for every holiday
encountered. Each new week always restarts cleanly at Monday = ODD.

## Real calendar data

Holidays are fetched live from the free, keyless public API
[date.nager.at](https://date.nager.at) (`GET /api/v3/PublicHolidays/{year}/{countryCode}`),
so the app reflects **actual official government holidays** for the country/year
you select (India, US, UK, Australia, Germany, Canada in the dropdown — add
more ISO country codes as needed). If there's no internet connection, it
automatically falls back to a small built-in offline holiday list so the app
still runs.

## Features

- Live bulb indicators (7 custom-drawn bulbs, glow when ON)
- "This Week's Schedule" table showing Mon–Sun, status, and holiday name
- **Simulate** any date (date picker) to preview its bulb pattern without
  affecting the real/current trigger state
- **Trigger Today Now** button to force-apply the real, current day's pattern
- Auto-checks every 30 seconds for a date rollover (midnight) and
  re-triggers automatically
- Live event log with timestamps
- Country + year selector to reload a different holiday set

## Project structure

```
src/bulb/
  Main.java                     - entry point
  model/BulbState.java          - ODD / EVEN / HOLIDAY enum + which bulbs light up
  model/DaySchedule.java        - result for one calendar day
  service/HolidayService.java   - fetches real holidays from date.nager.at (with offline fallback)
  service/ScheduleEngine.java   - the odd/even + holiday-shift logic
  service/BulbController.java   - simulated bulb driver (swap this for real hardware, see below)
  gui/BulbSchedulerGUI.java     - the Swing interface
```

## Build & run

Requires JDK 11+ (uses `java.net.http.HttpClient` and `java.time`).

```bash
cd BulbScheduler
mkdir -p out
javac -d out $(find src -name "*.java")
java -cp out bulb.Main
```

## Connecting to REAL bulbs/hardware

Everything (GUI, schedule engine, holiday logic) stays the same. Only
`BulbController.setBulb(int bulbId, boolean on)` needs to be edited to talk
to your real hardware, e.g.:

- **Raspberry Pi (pi4j / GPIO relay board):** call `gpio.setState(pin, on ? HIGH : LOW)`
- **Arduino over serial:** write a byte/command over the serial port
- **Wi-Fi smart bulbs / relays (HTTP or MQTT):** send an HTTP request or
  publish an MQTT message to the device

## Notes / assumptions

- "Odd bulbs" = bulbs numbered 1, 3, 5, 7 (4 bulbs); "even bulbs" = 2, 4, 6
  (3 bulbs), since there are 7 bulbs total.
- Holiday data country is selectable in the GUI (default India, "IN").
  Change the dropdown/year and click **Load Holidays** to refresh.
