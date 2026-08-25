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

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OffsetDateTimeAdapterTest {

    // Test Variables
    private final OffsetDateTimeAdapter offsetDateTimeAdapter = new OffsetDateTimeAdapter();

    /**
     * Test that UTC date-times are marshalled in the S-100 Part 17 form
     * yyyy-mm-ddThh:mm:ssZ.
     */
    @Test
    void testMarshalUsesUtcExtendedIsoForm() {
        assertEquals("1985-04-12T10:15:30Z", this.offsetDateTimeAdapter.marshal(
                OffsetDateTime.of(1985, 4, 12, 10, 15, 30, 0, ZoneOffset.UTC)));
    }

    /**
     * Test that a non-UTC date-time is converted to UTC before the 'Z' zone
     * designator is printed, so that the marshalled value denotes the same
     * instant as the marshalled object.
     */
    @Test
    void testMarshalNormalisesOffsetsToUtc() {
        assertEquals("1985-04-12T09:15:30Z", this.offsetDateTimeAdapter.marshal(
                OffsetDateTime.of(1985, 4, 12, 10, 15, 30, 0, ZoneOffset.ofHours(1))));
        assertEquals("2026-01-15T15:00:00Z", this.offsetDateTimeAdapter.marshal(
                OffsetDateTime.of(2026, 1, 15, 10, 0, 0, 0, ZoneOffset.ofHours(-5))));
    }

    /**
     * Test that all the xs:dateTime lexical forms listed by S-100 Part 1
     * Table 1-2 can be unmarshalled; a value without a zone offset is assumed
     * to be UTC.
     */
    @Test
    void testUnmarshalAcceptsTheXsdDateTimeLexicalSpace() {
        assertEquals(OffsetDateTime.of(1985, 4, 12, 10, 15, 30, 0, ZoneOffset.UTC),
                this.offsetDateTimeAdapter.unmarshal("1985-04-12T10:15:30Z"));
        assertEquals(OffsetDateTime.of(1985, 4, 12, 10, 15, 30, 0, ZoneOffset.ofHours(1)),
                this.offsetDateTimeAdapter.unmarshal("1985-04-12T10:15:30+01:00"));
        assertEquals(OffsetDateTime.of(1985, 4, 12, 10, 15, 30, 0, ZoneOffset.UTC),
                this.offsetDateTimeAdapter.unmarshal("1985-04-12T10:15:30"));
    }

    /**
     * Test that a marshalled date-time is unmarshalled back to the same
     * instant.
     */
    @Test
    void testMarshalUnmarshalRoundTrip() {
        final OffsetDateTime dateTime = OffsetDateTime.of(2023, 7, 10, 8, 0, 0, 0, ZoneOffset.ofHours(2));
        assertEquals(dateTime.toInstant(),
                this.offsetDateTimeAdapter.unmarshal(this.offsetDateTimeAdapter.marshal(dateTime)).toInstant());
    }

}
