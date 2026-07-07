package bulb.service;

import bulb.model.BulbState;
import bulb.model.DaySchedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Core scheduling logic.
 *
 * Base weekly plan (Monday -> Sunday), Sunday counted as a WORKING day:
 *
 *   Monday    -> ODD
 *   Tuesday   -> EVEN
 *   Wednesday -> ODD
 *   Thursday  -> EVEN
 *   Friday    -> ODD
 *   Saturday  -> EVEN
 *   Sunday    -> ODD
 *
 * If a day in that plan turns out to be a government holiday, no bulbs are
 * triggered that day and its planned slot (ODD/EVEN) is pushed forward onto
 * the next working day of the same week. This means all following days in
 * that week shift by one slot for every holiday encountered.
 *
 * (Any slot that cannot be placed because the week runs out of days - e.g.
 * two holidays very late in the week - is simply dropped for that week, the
 * next week always restarts cleanly with Monday = ODD.)
 */
public class ScheduleEngine {

    private static final BulbState[] WEEKLY_PLAN = {
            BulbState.ODD,  // Monday
            BulbState.EVEN, // Tuesday
            BulbState.ODD,  // Wednesday
            BulbState.EVEN, // Thursday
            BulbState.ODD,  // Friday
            BulbState.EVEN, // Saturday
            BulbState.ODD   // Sunday
    };

    /**
     * Computes the full Monday-Sunday schedule for the week that contains
     * {@code anyDateInWeek}, applying holiday shifting.
     */
    public List<DaySchedule> computeWeek(LocalDate anyDateInWeek, Map<LocalDate, String> holidays) {
        LocalDate monday = anyDateInWeek.with(DayOfWeek.MONDAY);
        List<DaySchedule> week = new ArrayList<>();

        int slotIndex = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            if (holidays.containsKey(d)) {
                week.add(new DaySchedule(d, d.getDayOfWeek(), BulbState.HOLIDAY, true, holidays.get(d)));
                // slotIndex is NOT advanced -> its planned state carries to the next working day
            } else if (slotIndex < WEEKLY_PLAN.length) {
                BulbState state = WEEKLY_PLAN[slotIndex++];
                week.add(new DaySchedule(d, d.getDayOfWeek(), state, false, null));
            } else {
                // Ran out of plan slots this week (multiple holidays) - no trigger.
                week.add(new DaySchedule(d, d.getDayOfWeek(), BulbState.HOLIDAY, false, "No slot left this week"));
            }
        }
        return week;
    }

    /** Convenience: get today's schedule entry out of the computed week. */
    public DaySchedule getForDate(LocalDate date, Map<LocalDate, String> holidays) {
        List<DaySchedule> week = computeWeek(date, holidays);
        for (DaySchedule ds : week) {
            if (ds.getDate().equals(date)) return ds;
        }
        throw new IllegalStateException("Date not found in its own computed week: " + date);
    }
}
