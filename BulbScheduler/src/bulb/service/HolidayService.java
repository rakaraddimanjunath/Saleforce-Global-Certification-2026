package bulb.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Retrieves REAL public / government holiday data using the free, keyless
 * "Nager.Date" public holiday API (https://date.nager.at).
 *
 * Example endpoint used:
 *   https://date.nager.at/api/v3/PublicHolidays/2026/IN
 *
 * If the machine running this program has no internet access (or the API
 * is unreachable), the service automatically falls back to a small local
 * holiday list so the application keeps working offline/in a demo.
 *
 * Country codes follow ISO 3166-1 alpha-2, e.g. "IN" India, "US" USA,
 * "GB" United Kingdom, "AU" Australia, "DE" Germany, etc.
 */
public class HolidayService {

    private static final String API_TEMPLATE =
            "https://date.nager.at/api/v3/PublicHolidays/%d/%s";

    private static final Pattern ENTRY_PATTERN = Pattern.compile(
            "\\{[^{}]*?\"date\"\\s*:\\s*\"(\\d{4}-\\d{2}-\\d{2})\"[^{}]*?\"localName\"\\s*:\\s*\"([^\"]*)\"[^{}]*?}"
    );

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * Returns a map of holidayDate -> holidayName for the given year/country.
     * Falls back to {@link #localFallback(int, String)} if the API call fails.
     */
    public Map<LocalDate, String> getHolidays(int year, String countryCode) {
        try {
            String url = String.format(API_TEMPLATE, year, countryCode.toUpperCase());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(6))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parse(response.body());
            } else {
                System.out.println("[HolidayService] API returned status "
                        + response.statusCode() + " - using offline fallback list.");
                return localFallback(year, countryCode);
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("[HolidayService] Could not reach date.nager.at ("
                    + e.getMessage() + ") - using offline fallback list.");
            return localFallback(year, countryCode);
        }
    }

    private Map<LocalDate, String> parse(String json) {
        Map<LocalDate, String> holidays = new LinkedHashMap<>();
        Matcher m = ENTRY_PATTERN.matcher(json);
        while (m.find()) {
            LocalDate date = LocalDate.parse(m.group(1));
            String name = m.group(2);
            holidays.put(date, name);
        }
        return holidays;
    }

    /** Small offline sample so the GUI still works without internet access. */
    private Map<LocalDate, String> localFallback(int year, String countryCode) {
        Map<LocalDate, String> map = new LinkedHashMap<>();
        map.put(LocalDate.of(year, 1, 1), "New Year's Day");
        if ("IN".equalsIgnoreCase(countryCode)) {
            map.put(LocalDate.of(year, 1, 26), "Republic Day");
            map.put(LocalDate.of(year, 8, 15), "Independence Day");
            map.put(LocalDate.of(year, 10, 2), "Gandhi Jayanti");
        } else if ("US".equalsIgnoreCase(countryCode)) {
            map.put(LocalDate.of(year, 7, 4), "Independence Day");
            map.put(LocalDate.of(year, 12, 25), "Christmas Day");
        } else {
            map.put(LocalDate.of(year, 12, 25), "Christmas Day");
        }
        return map;
    }
}
