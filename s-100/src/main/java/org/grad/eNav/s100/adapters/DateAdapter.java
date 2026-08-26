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

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

/**
 * The Date Adapter Class.
 * <p/>
 * This is used to translate between the Java util.Date objects and the XML
 * date elements.
 * <p/>
 * The adapter is bound to the ISO 19115-3 {@code gco:Date_Type}, which is the
 * union of {@code xs:date}, {@code xs:gYearMonth} and {@code xs:gYear}, so
 * unmarshalling accepts all three lexical spaces (each with the optional
 * trailing time zone designator they all allow). Since {@link LocalDate}
 * cannot represent a reduced precision, the truncated members are widened to
 * the first day of the period they denote: {@code 2026-01} becomes
 * {@code 2026-01-01} and {@code 2026} becomes {@code 2026-01-01}.
 * <p/>
 * Dates are marshalled in the extended ISO-8601 form ({@code 2026-01-15})
 * required by the {@code xs:date} lexical space - which is also a member of
 * the {@code gco:Date_Type} union, so any unmarshalled value round-trips to a
 * valid (if day-precision) date. The basic form ({@code 20260115}) produced by
 * earlier versions of these bindings is still accepted when unmarshalling.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class DateAdapter extends XmlAdapter<String, LocalDate> {

    public static final String S100_DATE_FORMAT = "yyyyMMdd";
    public final DateTimeFormatter S100_DATE_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern(S100_DATE_FORMAT)
            .parseStrict()
            .toFormatter();

    /**
     * The xs:gYearMonth lexical space (e.g. 2026-01, 2026-01Z, 2026-01+02:00).
     */
    private static final DateTimeFormatter G_YEAR_MONTH_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
            .appendLiteral('-')
            .appendValue(ChronoField.MONTH_OF_YEAR, 2)
            .optionalStart()
            .appendOffsetId()
            .optionalEnd()
            .toFormatter();

    /**
     * The xs:gYear lexical space (e.g. 2026, 2026Z, 2026+02:00). Only the
     * four digit years are accepted, so that the legacy basic date form
     * ({@code 20260115}) cannot be mistaken for a year.
     */
    private static final DateTimeFormatter G_YEAR_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.YEAR, 4, 4, SignStyle.NORMAL)
            .optionalStart()
            .appendOffsetId()
            .optionalEnd()
            .toFormatter();

    /**
     * Marshall a Java Date object into an XML element.
     *
     * @param date      The java Date object
     * @return The XML element
     */
    @Override
    public String marshal(LocalDate date) {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(date);
    }

    /**
     * Unmarshall an XML element into a Java Date object.
     * <p/>
     * All the member types of the {@code gco:Date_Type} union are accepted,
     * with the reduced precision ones widened as described in the class
     * javadoc.
     *
     * @param xml       The XML element
     * @return The Java Date object
     */
    @Override
    public LocalDate unmarshal(String xml) {
        // The xs:date member type - ISO_DATE also tolerates the optional zone
        // offset that xs:date allows
        try {
            return LocalDate.parse(xml, DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            // not an xs:date, so try the remaining lexical spaces
        }

        // The legacy basic form produced by earlier versions of these bindings
        try {
            synchronized (S100_DATE_FORMATTER) {
                return LocalDate.parse(xml, S100_DATE_FORMATTER);
            }
        } catch (DateTimeParseException e) {
            // not the legacy basic form, so try the truncated member types
        }

        // The xs:gYearMonth member type, widened to the first day of the month
        // (a DateTimeException also covers the out of range month values that
        // the parser only rejects when the fields are read)
        try {
            final TemporalAccessor yearMonth = G_YEAR_MONTH_FORMATTER.parse(xml);
            return LocalDate.of(yearMonth.get(ChronoField.YEAR), yearMonth.get(ChronoField.MONTH_OF_YEAR), 1);
        } catch (DateTimeException e) {
            // not an xs:gYearMonth, so only the xs:gYear member type is left
        }

        // The xs:gYear member type, widened to the first day of the year
        try {
            final TemporalAccessor year = G_YEAR_FORMATTER.parse(xml);
            return LocalDate.of(year.get(ChronoField.YEAR), 1, 1);
        } catch (DateTimeException e) {
            throw new DateTimeParseException(
                    "Text '" + xml + "' could not be parsed as an xs:date, xs:gYearMonth or xs:gYear value",
                    xml,
                    0,
                    e);
        }
    }

}