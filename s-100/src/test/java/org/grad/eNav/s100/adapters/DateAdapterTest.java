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

import static org.junit.jupiter.api.Assertions.assertEquals;

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
     * Test that the legacy basic form produced by earlier versions of these
     * bindings is still accepted when unmarshalling.
     */
    @Test
    void testUnmarshalAcceptsLegacyBasicForm() {
        assertEquals(LocalDate.of(2026, 1, 15), this.dateAdapter.unmarshal("20260115"));
    }

}
