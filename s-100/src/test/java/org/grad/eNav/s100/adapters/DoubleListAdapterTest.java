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

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DoubleListAdapterTest {

    // Test Variables
    private final DoubleListAdapter doubleListAdapter = new DoubleListAdapter();

    /**
     * Test that coordinates are marshalled with up to 7 decimals, as required
     * by S-124 clause 8.2.
     */
    @Test
    void testMarshalUsesSevenDecimals() {
        assertEquals("55.6710000 12.5890000", this.doubleListAdapter.marshal(new Double[]{55.671, 12.589}));
    }

    /**
     * Test that the decimal separator is always a point, whatever the JVM
     * default locale is, since the gml:doubleList lexical space is that of
     * xs:double.
     */
    @Test
    void testMarshalIsLocaleIndependent() {
        final Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertEquals("55.6710000 12.5890000", this.doubleListAdapter.marshal(new Double[]{55.671, 12.589}));
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    /**
     * Test that a space separated coordinate list can be unmarshalled.
     */
    @Test
    void testUnmarshalReadsSpaceSeparatedList() {
        assertArrayEquals(new Double[]{55.671, 12.589}, this.doubleListAdapter.unmarshal("55.671 12.589"));
    }

    /**
     * Test that the whitespace tolerated by the XML list lexical space - any
     * mixture of spaces, tabs and newlines, including leading and trailing
     * ones - does not produce empty or unparseable entries.
     */
    @Test
    void testUnmarshalToleratesArbitraryWhitespace() {
        assertArrayEquals(new Double[]{55.671, 12.589, 56.0, 13.0},
                this.doubleListAdapter.unmarshal("\n    55.671 12.589\n    56.0\t13.0\n"));
        assertArrayEquals(new Double[]{}, this.doubleListAdapter.unmarshal(""));
        assertArrayEquals(new Double[]{}, this.doubleListAdapter.unmarshal("   "));
    }

    /**
     * Test that a marshalled coordinate list is unmarshalled back to the same
     * values, whatever the JVM default locale is.
     */
    @Test
    void testMarshalUnmarshalRoundTrip() {
        final Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            final Double[] coordinates = new Double[]{55.671, 12.589};
            assertArrayEquals(coordinates, this.doubleListAdapter.unmarshal(this.doubleListAdapter.marshal(coordinates)));
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

}
