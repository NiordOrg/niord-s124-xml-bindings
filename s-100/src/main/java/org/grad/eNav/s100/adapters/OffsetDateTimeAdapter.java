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

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

/**
 * The DateTime Adapter Class.
 * <p/>
 * This is used to translate between the Java util.Date objects and the XML dateTime elements.
 * <p/>
 * Date-times are marshalled normalised to UTC in the S-100 Part 17 form
 * ({@code yyyy-mm-ddThh:mm:ssZ}), so the 'Z' zone designator always labels
 * the correct instant. Unmarshalling accepts the full {@code xs:dateTime}
 * lexical space ('Z', an explicit {@code +hh:mm}/{@code -hh:mm} offset, or no
 * offset at all); values without an offset are assumed to be UTC.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class OffsetDateTimeAdapter extends XmlAdapter<String, OffsetDateTime> {

    // The literal 'Z' is only correct for values normalised to UTC first
    public static final DateTimeFormatter S100_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss'Z'");

    /**
     * Marshall a Java Date object into an XML element.
     *
     * @param date
     *            The java Date object
     * @return The XML element
     */
    @Override
    public String marshal(OffsetDateTime date) {
        // Normalise to UTC before printing the 'Z' designator, so non-UTC
        // values are not mislabelled as UTC (ISO 8601 / S-100 Part 1 Table 1-2)
        return S100_DATE_TIME_FORMATTER.format(date.withOffsetSameInstant(ZoneOffset.UTC));
    }

    /**
     * Unmarshall an XML element into a Java Date object.
     *
     * @param xml
     *            The XML element
     * @return The Java Date object
     */
    @Override
    public OffsetDateTime unmarshal(String xml) {
        // Accept the full xs:dateTime lexical space: 'Z', '+hh:mm'/'-hh:mm'
        // or no offset; offset-less values are assumed to be UTC
        final TemporalAccessor parsed = DateTimeFormatter.ISO_DATE_TIME
                .parseBest(xml, OffsetDateTime::from, LocalDateTime::from);
        return parsed instanceof OffsetDateTime offsetDateTime
                ? offsetDateTime
                : ((LocalDateTime) parsed).atOffset(ZoneOffset.UTC);
    }
}
