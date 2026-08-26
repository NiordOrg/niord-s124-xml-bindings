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

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DurationAdapterTest {

    // Test Variables
    private final DurationAdapter durationAdapter = new DurationAdapter();

    /**
     * Test that durations are marshalled in the ISO-8601 form of the
     * xs:duration lexical space.
     */
    @Test
    void testMarshalUsesIsoForm() {
        assertEquals("PT100H", this.durationAdapter.marshal(Duration.ofHours(100)));
        assertEquals("PT24H", this.durationAdapter.marshal(Duration.ofDays(1)));
        assertEquals("PT0S", this.durationAdapter.marshal(Duration.ZERO));
    }

    /**
     * Test that negative durations are marshalled with the single leading
     * minus of the xs:duration lexical space, rather than with the
     * component-level signs of Duration.toString() (e.g. PT-100H) which the
     * unsigned integer components of xs:duration do not allow.
     */
    @Test
    void testMarshalSignsNegativeDurationsWithALeadingMinus() {
        assertEquals("-PT100H", this.durationAdapter.marshal(Duration.ofHours(-100)));
        assertEquals("-PT1H30M", this.durationAdapter.marshal(Duration.ofHours(-1).minusMinutes(30)));
        assertEquals("-PT24H", this.durationAdapter.marshal(Duration.ofDays(-1)));
    }

    /**
     * Test that the day and time components of the xs:duration lexical space
     * are unmarshalled, including the 'M' of the time part which denotes
     * minutes rather than months.
     */
    @Test
    void testUnmarshalReadsDayAndTimeComponents() {
        assertEquals(Duration.ofHours(100), this.durationAdapter.unmarshal("PT100H"));
        assertEquals(Duration.ofMinutes(10), this.durationAdapter.unmarshal("PT10M"));
        assertEquals(Duration.ofDays(1), this.durationAdapter.unmarshal("P1D"));
        assertEquals(Duration.ofDays(1).plusHours(2), this.durationAdapter.unmarshal("P1DT2H"));
    }

    /**
     * Test that the year and month components of the xs:duration lexical
     * space, which S-100 Part 17 clause 17-4.9 allows for
     * userDefinedMaintenanceFrequency, are unmarshalled through the estimated
     * ChronoUnit durations.
     */
    @Test
    void testUnmarshalReadsYearAndMonthComponents() {
        assertEquals(ChronoUnit.MONTHS.getDuration(), this.durationAdapter.unmarshal("P1M"));
        assertEquals(ChronoUnit.YEARS.getDuration(), this.durationAdapter.unmarshal("P1Y"));
        assertEquals(ChronoUnit.YEARS.getDuration()
                        .plus(ChronoUnit.MONTHS.getDuration().multipliedBy(2))
                        .plus(Duration.ofDays(3))
                        .plus(Duration.ofHours(4))
                        .plus(Duration.ofMinutes(5))
                        .plus(Duration.ofSeconds(6)),
                this.durationAdapter.unmarshal("P1Y2M3DT4H5M6S"));
    }

    /**
     * Test that the negative durations of the xs:duration lexical space are
     * unmarshalled, whether or not they carry year/month components.
     */
    @Test
    void testUnmarshalReadsNegativeDurations() {
        assertEquals(Duration.ofHours(-100), this.durationAdapter.unmarshal("-PT100H"));
        assertEquals(ChronoUnit.MONTHS.getDuration().negated(), this.durationAdapter.unmarshal("-P1M"));
    }

    /**
     * Test that a marshalled duration is unmarshalled back to the same value.
     */
    @Test
    void testMarshalUnmarshalRoundTrip() {
        final Duration duration = Duration.ofDays(100);
        assertEquals(duration, this.durationAdapter.unmarshal(this.durationAdapter.marshal(duration)));
    }

    /**
     * Test that a negative duration accepted by unmarshalling round-trips
     * back to the same lexical representation.
     */
    @Test
    void testMarshalUnmarshalRoundTripForNegativeDurations() {
        assertEquals("-PT100H", this.durationAdapter.marshal(this.durationAdapter.unmarshal("-PT100H")));
        assertEquals(Duration.ofHours(-100), this.durationAdapter.unmarshal(this.durationAdapter.marshal(Duration.ofHours(-100))));
    }

}
