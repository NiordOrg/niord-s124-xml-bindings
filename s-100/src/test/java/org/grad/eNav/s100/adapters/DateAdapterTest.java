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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DateAdapterTest {

    // Test Variables
    private final DateAdapter dateAdapter = new DateAdapter();

    /**
     * Test that dates are marshalled in the extended ISO-8601 form required
     * by the xs:date lexical space.
     */
    @Test
    void testMarshalUsesExtendedIsoForm() {
        assertEquals("2026-01-15", this.dateAdapter.marshal(LocalDate.of(2026, 1, 15)));
    }

    /**
     * Test that the extended ISO-8601 form can be unmarshalled, with and
     * without the optional xs:date zone offset.
     */
    @Test
    void testUnmarshalAcceptsExtendedIsoForm() {
        assertEquals(LocalDate.of(2026, 1, 15), this.dateAdapter.unmarshal("2026-01-15"));
        assertEquals(LocalDate.of(2026, 1, 15), this.dateAdapter.unmarshal("2026-01-15Z"));
    }

    /**
     * Test that extended years are handled as xs:gYear values rather than as
     * the ambiguous, non-standard legacy basic-date representation.
     */
    @Test
    void testUnmarshalPrefersGYearOverAmbiguousLegacyBasicForm() {
        assertEquals(LocalDate.of(2026, 1, 1), this.dateAdapter.unmarshal("2026"));
        assertEquals(LocalDate.of(12004, 1, 1), this.dateAdapter.unmarshal("12004"));
        assertEquals(LocalDate.of(20_260_115, 1, 1), this.dateAdapter.unmarshal("20260115"));
    }

    /**
     * Test that the xs:gYearMonth member type of the gco:Date_Type union is
     * unmarshalled, widened to the first day of the month, with and without
     * the optional zone designator.
     */
    @Test
    void testUnmarshalAcceptsGYearMonth() {
        assertEquals(LocalDate.of(2026, 1, 1), this.dateAdapter.unmarshal("2026-01"));
        assertEquals(LocalDate.of(12004, 1, 1), this.dateAdapter.unmarshal("12004-01"));
        assertEquals(LocalDate.of(2026, 12, 1), this.dateAdapter.unmarshal("2026-12"));
        assertEquals(LocalDate.of(2026, 1, 1), this.dateAdapter.unmarshal("2026-01Z"));
        assertEquals(LocalDate.of(2026, 1, 1), this.dateAdapter.unmarshal("2026-01+02:00"));
    }

    /**
     * Test that the xs:gYear member type of the gco:Date_Type union is
     * unmarshalled, widened to the first day of the year, with and without
     * the optional zone designator.
     */
    @Test
    void testUnmarshalAcceptsGYear() {
        assertEquals(LocalDate.of(2026, 1, 1), this.dateAdapter.unmarshal("2026"));
        assertEquals(LocalDate.of(2026, 1, 1), this.dateAdapter.unmarshal("2026Z"));
        assertEquals(LocalDate.of(2026, 1, 1), this.dateAdapter.unmarshal("2026-05:00"));
    }

    /**
     * Test that the values widened from the truncated union member types
     * round-trip through the xs:date form they are marshalled into.
     */
    @Test
    void testMarshalUnmarshalRoundTrip() {
        assertEquals("2026-01-01", this.dateAdapter.marshal(this.dateAdapter.unmarshal("2026-01")));
        assertEquals("2026-01-01", this.dateAdapter.marshal(this.dateAdapter.unmarshal("2026")));
        assertEquals(LocalDate.of(2026, 1, 15),
                this.dateAdapter.unmarshal(this.dateAdapter.marshal(LocalDate.of(2026, 1, 15))));
    }

    /**
     * Test that a value belonging to none of the gco:Date_Type union member
     * lexical spaces is still rejected.
     */
    @Test
    void testUnmarshalRejectsInvalidDates() {
        assertThrows(DateTimeParseException.class, () -> this.dateAdapter.unmarshal("not-a-date"));
        assertThrows(DateTimeParseException.class, () -> this.dateAdapter.unmarshal("2026-13"));
        assertThrows(DateTimeParseException.class, () -> this.dateAdapter.unmarshal("26"));
    }

}
