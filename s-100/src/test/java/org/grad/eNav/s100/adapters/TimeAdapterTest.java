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

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeAdapterTest {

    // Test Variables
    private final TimeAdapter timeAdapter = new TimeAdapter();

    /**
     * Test that times are marshalled in the extended ISO-8601 form required
     * by the xs:time lexical space, always including the seconds field.
     */
    @Test
    void testMarshalUsesExtendedIsoForm() {
        assertEquals("09:30:15", this.timeAdapter.marshal(LocalTime.of(9, 30, 15)));
        // whole minutes must not drop the mandatory seconds field
        assertEquals("00:00:00", this.timeAdapter.marshal(LocalTime.MIDNIGHT));
    }

    /**
     * Test that the extended ISO-8601 form can be unmarshalled.
     */
    @Test
    void testUnmarshalAcceptsExtendedIsoForm() {
        assertEquals(LocalTime.of(9, 30, 15), this.timeAdapter.unmarshal("09:30:15"));
    }

    /**
     * Test that the legacy basic form produced by earlier versions of these
     * bindings is still accepted when unmarshalling.
     */
    @Test
    void testUnmarshalAcceptsLegacyBasicForm() {
        assertEquals(LocalTime.of(9, 30, 15), this.timeAdapter.unmarshal("093015"));
    }

}
