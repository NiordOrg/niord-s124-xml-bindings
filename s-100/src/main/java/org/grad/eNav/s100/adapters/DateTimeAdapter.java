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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;

import static org.grad.eNav.s100.adapters.DateAdapter.S100_DATE_FORMAT;
import static org.grad.eNav.s100.adapters.TimeAdapter.S100_TIME_FORMAT;

/**
 * The DateTime Adapter Class.
 * <p/>
 * This is used to translate between the Java util.Date objects and the XML
 * dateTime elements.
 * <p/>
 * The bound {@link LocalDateTime} values are defined to carry UTC.
 * Date-times are therefore marshalled in the S-100 Part 17 form
 * ({@code yyyy-mm-ddThh:mm:ssZ}, seconds included) mandated e.g. for the
 * S100_ExchangeCatalogueIdentifier dateTime attribute. Unmarshalling accepts
 * the full {@code xs:dateTime} lexical space and normalises any explicit
 * offset to UTC before dropping it; the basic form ({@code 20260115T093000},
 * with an optional offset) produced by earlier versions of these bindings is
 * still accepted.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class DateTimeAdapter extends XmlAdapter<String, LocalDateTime> {

    public static final DateTimeFormatter S100_DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern(S100_DATE_FORMAT)
            .appendPattern("'T'")
            .appendPattern(S100_TIME_FORMAT)
            .optionalStart()
            .parseLenient()
            .appendOffset("+HHMM", "Z")
            .parseStrict()
            .toFormatter();

    // xs:dateTime requires the seconds field, which
    // DateTimeFormatter.ISO_LOCAL_DATE_TIME omits for whole-minute values;
    // the literal 'Z' is correct because the bound LocalDateTime carries UTC
    private static final DateTimeFormatter XSD_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss'Z'");

    /**
     * Marshall a Java Date object into an XML element.
     *
     * @param date      The java Date object
     * @return The XML element
     */
    @Override
    public String marshal(LocalDateTime date) {
        return XSD_DATE_TIME_FORMATTER.format(date);
    }

    /**
     * Unmarshall an XML element into a Java Date object.
     *
     * @param xml       The XML element
     * @return The Java Date object
     */
    @Override
    public LocalDateTime unmarshal(String xml) {
        try {
            // ISO_DATE_TIME also tolerates fractional seconds and an optional
            // zone offset, as xs:dateTime does
            return toUtc(DateTimeFormatter.ISO_DATE_TIME.parse(xml));
        } catch (DateTimeParseException e) {
            // fall back to the legacy basic form
            synchronized (S100_DATE_TIME_FORMATTER) {
                return toUtc(S100_DATE_TIME_FORMATTER.parse(xml));
            }
        }
    }

    /**
     * Normalises a parsed date-time to UTC, since the bound
     * {@link LocalDateTime} values are defined to carry UTC: any explicit
     * offset is converted to UTC before being dropped, while offset-less
     * values are assumed to already be UTC.
     *
     * @param parsed    The parsed temporal accessor
     * @return The UTC local date-time
     */
    private static LocalDateTime toUtc(TemporalAccessor parsed) {
        final LocalDateTime localDateTime = LocalDateTime.from(parsed);
        if (parsed.isSupported(ChronoField.OFFSET_SECONDS)) {
            return localDateTime
                    .atOffset(ZoneOffset.ofTotalSeconds(parsed.get(ChronoField.OFFSET_SECONDS)))
                    .withOffsetSameInstant(ZoneOffset.UTC)
                    .toLocalDateTime();
        }
        return localDateTime;
    }

}
