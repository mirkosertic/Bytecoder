/*
 * Copyright 2023 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mirkosertic.bytecoder.integrationtest;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.ConstraintViolations;
import am.ik.yavi.core.Validator;
import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(UnitTestRunner.class)
public class YaviValidationTest {

    public static class Car {

        private final String manufacturer;

        private final String licensePlate;

        private final int seatCount;

        public static final Validator<Car> validator = ValidatorBuilder.<Car>of()
                .constraint(Car::getManufacturer, "manufacturer", c -> c.notNull())
                .constraint(Car::getLicensePlate, "licensePlate", c -> c.notNull().greaterThanOrEqual(2).lessThanOrEqual(14))
                .constraint(Car::getSeatCount, "seatCount", c -> c.greaterThanOrEqual(2))
                .build();

        public Car(final String manufacturer, final String licencePlate, final int seatCount) {
            this.manufacturer = manufacturer;
            this.licensePlate = licencePlate;
            this.seatCount = seatCount;
        }

        public String getManufacturer() {
            return manufacturer;
        }

        public String getLicensePlate() {
            return licensePlate;
        }

        public int getSeatCount() {
            return seatCount;
        }
    }

    @Test
    public void testValidation() {
        final Car car = new Car(null, "DD-AB-123", 2);
        final ConstraintViolations violations = Car.validator.validate(car);
        violations.forEach(violation -> {
            System.out.println(violation.toString());
        });
    }
}
