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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

import static org.grad.eNav.s100.adapters.DateAdapter.S100_DATE_FORMAT;
import static org.grad.eNav.s100.adapters.TimeAdapter.S100_TIME_FORMAT;

/**
 * The DateTime Adapter Class.
 * <p/>
 * This is used to translate between the Java util.Date objects and the XML
 * dateTime elements.
 * <p/>
 * Date-times are marshalled in the extended ISO-8601 form
 * ({@code 2026-01-15T09:30:00}) required by the {@code xs:dateTime} lexical
 * space (seconds included). The basic form ({@code 20260115T093000}, with an
 * optional offset) produced by earlier versions of these bindings is still
 * accepted when unmarshalling.
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
    // DateTimeFormatter.ISO_LOCAL_DATE_TIME omits for whole-minute values
    private static final DateTimeFormatter XSD_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss");

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
            return LocalDateTime.parse(xml, DateTimeFormatter.ISO_DATE_TIME);
        } catch (DateTimeParseException e) {
            // fall back to the legacy basic form
            synchronized (S100_DATE_TIME_FORMATTER) {
                return LocalDateTime.parse(xml, S100_DATE_TIME_FORMATTER);
            }
        }
    }

}