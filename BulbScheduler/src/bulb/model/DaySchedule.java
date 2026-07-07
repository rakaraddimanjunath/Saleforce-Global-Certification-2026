package bulb.model;

import java.time.DayOfWeek;
import java.time.LocalDate;

/** Holds the computed bulb-trigger outcome for a single calendar date. */
public class DaySchedule {

    private final LocalDate date;
    private final DayOfWeek dayOfWeek;
    private final BulbState state;
    private final boolean holiday;
    private final String holidayName; // null if not a holiday

    public DaySchedule(LocalDate date, DayOfWeek dayOfWeek, BulbState state,
                        boolean holiday, String holidayName) {
        this.date = date;
        this.dayOfWeek = dayOfWeek;
        this.state = state;
        this.holiday = holiday;
        this.holidayName = holidayName;
    }

    public LocalDate getDate() { return date; }
    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public BulbState getState() { return state; }
    public boolean isHoliday() { return holiday; }
    public String getHolidayName() { return holidayName; }

    public String describe() {
        if (holiday) {
            return date + " (" + dayOfWeek + ") - HOLIDAY"
                    + (holidayName != null ? " [" + holidayName + "]" : "")
                    + " -> trigger shifted to next working day";
        }
        return date + " (" + dayOfWeek + ") -> " + state
                + " bulbs " + java.util.Arrays.toString(state.activeBulbs());
    }
}
