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

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The Double List Adapter Class.
 *
 * This is used to translate between the Java Double Array objects and the XML
 * String elements. It backs the whitespace separated GML coordinate lists
 * ({@code gml:pos}, {@code gml:posList}) as well as the envelope corners.
 * <p/>
 * Every value is written with exactly seven fractional digits. Ordinates are
 * therefore rounded to 1e-7 of a unit - for the EPSG:4326 degrees used
 * throughout S-124 that is roughly a centimetre of latitude, far below any
 * navigational significance, but it is a lossy quantisation: values are not
 * guaranteed to survive a marshal/unmarshal round trip bit for bit, and short
 * values are padded ({@code 55.5} is written as {@code 55.5000000}). Tests that
 * compare marshalled output against a fixture must expect the padded form.
 *
 * @author Nikolaos Vastardis (email: Nikolaos.Vastardis@gla-rad.org)
 */
public class DoubleListAdapter extends XmlAdapter<String, Double[]> {

    /**
     * Marshall a Java Double Array object into an XML element.
     *
     * @param doubleList    The java Double Array object
     * @return The XML element
     */
    @Override
    public String marshal(Double[] doubleList) {
        // Locale.ROOT guarantees the '.' decimal separator required by the
        // xs:double lexical space, regardless of the JVM default locale
        return Stream.of(doubleList)
                .map(d -> String.format(Locale.ROOT, "%.7f", d))
                .collect(Collectors.joining(" "));
    }

    /**
     * Unmarshall an XML element into a Java Double Array object.
     *
     * @param xml           The XML element
     * @return The Java Double Array object
     */
    @Override
    public Double[] unmarshal(String xml) {
        // First parse all the coordinates, tolerating leading/trailing
        // whitespace and multi-space/newline separators
        return Arrays.stream(xml.split("\\s+"))
                .filter(Predicate.not(String::isEmpty))
                .map(Double::parseDouble)
                .toArray(Double[]::new);
    }

}