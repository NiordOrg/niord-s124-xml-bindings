/*
 * Copyright (c) 2024 GLA Research and Development Directorate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grad.eNav.s100.adapters;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.Duration;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * The DateTime Adapter Class.
 *
 * This is used to translate between the java.time.LocalDateTime objects and the
 * XML dateTime elements.
 * <p/>
 * Unmarshalling accepts the full {@code xs:duration} lexical space
 * ({@code PnYnMnDTnHnMnS}, S-100 Part 17 clause 17-4.9), including the
 * year/month components that {@link Duration#parse(CharSequence)} rejects.
 * Since {@link Duration} cannot represent calendar-based year/month lengths
 * exactly, those components are converted using the ISO-8601 estimated
 * durations of {@link ChronoUnit#YEARS} (365.2425 days) and
 * {@link ChronoUnit#MONTHS} (a twelfth of that).
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class DurationAdapter extends XmlAdapter<String, Duration> {

    //private final DateTimeFormatter dateFormat = DurationAdapter;

    /**
     * Marshall a Java Date object into an XML element.
     *
     * @param duration  The java Duration object
     * @return The XML element
     */
    @Override
    public String marshal(Duration duration) {
        return duration.toString();
    }

    /**
     * Unmarshall an XML element into a Java Duration object.
     *
     * @param xml       The XML element
     * @return The Java Duration object
     */
    @Override
    public Duration unmarshal(String xml) {
        try {
            return Duration.parse(xml);
        } catch (DateTimeParseException e) {
            // Duration.parse rejects the xs:duration year/month components
            // (e.g. P1Y, P1M), so parse the date part separately
            return parseWithDatePart(xml);
        }
    }

    /**
     * Parses an xs:duration whose date part contains year/month/day
     * components, converting years and months through the estimated
     * {@link ChronoUnit} durations described in the class javadoc.
     *
     * @param xml       The xs:duration lexical representation
     * @return The Java Duration object
     */
    private static Duration parseWithDatePart(String xml) {
        final boolean negative = xml.startsWith("-");
        final String unsigned = negative ? xml.substring(1) : xml;
        final int timeIndex = unsigned.indexOf('T');
        final Period period = Period.parse(timeIndex < 0 ? unsigned : unsigned.substring(0, timeIndex));
        Duration duration = ChronoUnit.YEARS.getDuration().multipliedBy(period.getYears())
                .plus(ChronoUnit.MONTHS.getDuration().multipliedBy(period.getMonths()))
                .plus(ChronoUnit.DAYS.getDuration().multipliedBy(period.getDays()));
        if (timeIndex >= 0) {
            duration = duration.plus(Duration.parse("P" + unsigned.substring(timeIndex)));
        }
        return negative ? duration.negated() : duration;
    }

}
