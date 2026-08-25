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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DateTimeAdapterTest {

    // Test Variables
    private final DateTimeAdapter dateTimeAdapter = new DateTimeAdapter();

    /**
     * Test that date-times are marshalled in the S-100 Part 17 form
     * yyyy-mm-ddThh:mm:ssZ, i.e. the extended ISO-8601 form with the seconds
     * field and the zone designator of the UTC values these bindings carry.
     */
    @Test
    void testMarshalUsesUtcExtendedIsoForm() {
        assertEquals("2026-01-15T09:30:15Z", this.dateTimeAdapter.marshal(LocalDateTime.of(2026, 1, 15, 9, 30, 15)));
        // whole minutes must not drop the mandatory seconds field
        assertEquals("2026-01-15T09:30:00Z", this.dateTimeAdapter.marshal(LocalDateTime.of(2026, 1, 15, 9, 30)));
    }

    /**
     * Test that the extended ISO-8601 form can be unmarshalled, with and
     * without the optional xs:dateTime zone offset.
     */
    @Test
    void testUnmarshalAcceptsExtendedIsoForm() {
        assertEquals(LocalDateTime.of(2026, 1, 15, 9, 30, 15), this.dateTimeAdapter.unmarshal("2026-01-15T09:30:15"));
        assertEquals(LocalDateTime.of(2026, 1, 15, 9, 30, 15), this.dateTimeAdapter.unmarshal("2026-01-15T09:30:15Z"));
    }

    /**
     * Test that an explicit zone offset is converted to UTC, since the bound
     * LocalDateTime values are defined to carry UTC.
     */
    @Test
    void testUnmarshalNormalisesOffsetsToUtc() {
        assertEquals(LocalDateTime.of(2026, 1, 15, 7, 30, 15), this.dateTimeAdapter.unmarshal("2026-01-15T09:30:15+02:00"));
        assertEquals(LocalDateTime.of(2026, 1, 15, 14, 30, 15), this.dateTimeAdapter.unmarshal("2026-01-15T09:30:15-05:00"));
    }

    /**
     * Test that a marshalled date-time is unmarshalled back to the same value.
     */
    @Test
    void testMarshalUnmarshalRoundTrip() {
        final LocalDateTime dateTime = LocalDateTime.of(2026, 1, 15, 9, 30, 15);
        assertEquals(dateTime, this.dateTimeAdapter.unmarshal(this.dateTimeAdapter.marshal(dateTime)));
    }

    /**
     * Test that the legacy basic form produced by earlier versions of these
     * bindings is still accepted when unmarshalling, with and without the
     * optional offset.
     */
    @Test
    void testUnmarshalAcceptsLegacyBasicForm() {
        assertEquals(LocalDateTime.of(2026, 1, 15, 9, 30, 15), this.dateTimeAdapter.unmarshal("20260115T093015"));
        assertEquals(LocalDateTime.of(2026, 1, 15, 9, 30, 15), this.dateTimeAdapter.unmarshal("20260115T093015Z"));
        assertEquals(LocalDateTime.of(2026, 1, 15, 7, 30, 15), this.dateTimeAdapter.unmarshal("20260115T093015+0200"));
    }

}
