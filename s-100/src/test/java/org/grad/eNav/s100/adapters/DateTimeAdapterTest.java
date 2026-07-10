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
     * Test that date-times are marshalled in the extended ISO-8601 form
     * required by the xs:dateTime lexical space, always including the seconds
     * field.
     */
    @Test
    void testMarshalUsesExtendedIsoForm() {
        assertEquals("2026-01-15T09:30:15", this.dateTimeAdapter.marshal(LocalDateTime.of(2026, 1, 15, 9, 30, 15)));
        // whole minutes must not drop the mandatory seconds field
        assertEquals("2026-01-15T09:30:00", this.dateTimeAdapter.marshal(LocalDateTime.of(2026, 1, 15, 9, 30)));
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
     * Test that the legacy basic form produced by earlier versions of these
     * bindings is still accepted when unmarshalling, with and without the
     * optional offset.
     */
    @Test
    void testUnmarshalAcceptsLegacyBasicForm() {
        assertEquals(LocalDateTime.of(2026, 1, 15, 9, 30, 15), this.dateTimeAdapter.unmarshal("20260115T093015"));
        assertEquals(LocalDateTime.of(2026, 1, 15, 9, 30, 15), this.dateTimeAdapter.unmarshal("20260115T093015Z"));
    }

}
