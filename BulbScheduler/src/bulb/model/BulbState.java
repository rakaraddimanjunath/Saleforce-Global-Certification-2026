package bulb.model;

/**
 * Represents which set of bulbs should be active on a given calendar day.
 *
 *  ODD     -> Bulb 1, 3, 5, 7 are triggered ON
 *  EVEN    -> Bulb 2, 4, 6     are triggered ON
 *  HOLIDAY -> No bulbs are triggered; the day's slot is pushed to the next
 *             working day by the ScheduleEngine.
 */
public enum BulbState {
    ODD,
    EVEN,
    HOLIDAY;

    /** Bulb ids (1-7) that should be lit for this state. */
    public int[] activeBulbs() {
        switch (this) {
            case ODD:  return new int[]{1, 3, 5, 7};
            case EVEN: return new int[]{2, 4, 6};
            default:   return new int[]{};
        }
    }
}
