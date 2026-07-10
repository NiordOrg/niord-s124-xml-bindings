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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;

/**
 * The Time Adapter Class.
 *
 * This is used to translate between the java.time.LocalTime objects and the XML
 * time elements.
 * <p/>
 * Times are marshalled in the extended ISO-8601 form ({@code 09:30:00})
 * required by the {@code xs:time} lexical space (seconds included). The basic
 * form ({@code 093000}) produced by earlier versions of these bindings is
 * still accepted when unmarshalling.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class TimeAdapter extends XmlAdapter<String, LocalTime> {

    public static final String S100_TIME_FORMAT = "HHmmss";
    public final DateTimeFormatter S100_TIME_FORMATTER = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern(S100_TIME_FORMAT)
            .parseStrict()
            .toFormatter();

    // xs:time requires the seconds field, which DateTimeFormatter.ISO_LOCAL_TIME
    // omits for whole-minute values
    private static final DateTimeFormatter XSD_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Marshall a Java Date object into an XML element.
     *
     * @param date      The java Date object
     * @return The XML element
     */
    @Override
    public String marshal(LocalTime date) {
        return XSD_TIME_FORMATTER.format(date);
    }

    /**
     * Unmarshall an XML element into a Java Date object.
     *
     * @param xml       The XML element
     * @return The Java Date object
     */
    @Override
    public LocalTime unmarshal(String xml) {
        try {
            // ISO_TIME also tolerates fractional seconds and an optional zone
            // offset, as xs:time does
            return LocalTime.parse(xml, DateTimeFormatter.ISO_TIME);
        } catch (DateTimeParseException e) {
            // fall back to the legacy basic form
            synchronized (S100_TIME_FORMATTER) {
                return LocalTime.parse(xml, S100_TIME_FORMATTER);
            }
        }
    }

}