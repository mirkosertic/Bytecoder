/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.api.opencl;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;

public class OpenCLOptions {

    private final Predicate<PlatformProperties> platformFilter;
    public Predicate<PlatformProperties> getPlatformFilter() {
        return platformFilter;
    }

    private final Comparator<DeviceProperties> preferredDeviceComparator;
    public Comparator<DeviceProperties> getPreferredDeviceComparator() {
        return preferredDeviceComparator;
    }

    /**
     * @apiNote not exposed, use builder to create an instance
     */
    private OpenCLOptions(
            final Predicate<PlatformProperties> platformFilter,
            final Comparator<DeviceProperties> preferredDeviceComparator) {
        this.platformFilter = platformFilter;
        this.preferredDeviceComparator = preferredDeviceComparator;
    }

    // -- DEFAULTS

    public static OpenCLOptions defaults() {
        return OpenCLOptions.builder().build();
    }

    // -- OPTIONS BUILDER

    public static final class Builder {
        private Predicate<PlatformProperties> platformFilter = p -> true;
        private Comparator<DeviceProperties> preferredDeviceComparator = Comparator.comparingInt(DeviceProperties::getNumberOfComputeUnits);

        /**
         * Platforms are rejected if the platformFilter predicate returns false.
         * @param platformFilter
         */
        public Builder platformFilter(final Predicate<PlatformProperties> platformFilter) {
            Objects.requireNonNull(platformFilter);
            this.platformFilter = platformFilter;
            return this;
        }

        /**
         * The device that compares highest is chosen by the {@link PlatformFactory}, unless explicitly
         * overridden by system property {@code OPENCL_DEVICE}.
         * @param preferredDeviceComparator
         */
        public Builder preferredDeviceComparator(final Comparator<DeviceProperties> preferredDeviceComparator) {
            Objects.requireNonNull(preferredDeviceComparator);
            this.preferredDeviceComparator = preferredDeviceComparator;
            return this;
        }

        public OpenCLOptions build() {
            return new OpenCLOptions(platformFilter, preferredDeviceComparator);
        }
    }

    public static Builder builder() {
        return new Builder();
    }



}
