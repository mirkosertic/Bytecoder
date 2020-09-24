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

    public static enum CodeGeneratorStyle {
        
        /**
         * The Stackifier tries to remove all GOTO statements and 
         * replaces them with structured control flow elements and multi level break and continues. 
         * <p>
         * The Stackifier does only work for reducible control flows and also does not support 
         * exception handling. The generated output is smaller and in some cases faster compared 
         * to the Relooper output.
         */
        STACKIFIER,
        
        /**
         * The Relooper output generator tries to recover high level control flow constructs 
         * from the intermediate representation. This step eliminates the needs of GOTO 
         * statements and thus allows generation of more natural source code, which in turn 
         * can be easier read and optimized by Web Browsers or other tools.
         * <p> 
         * The Relooper supports all styles of control flows and also supports exception handling.
         */
        RELOOPER
    }
    
    private final CodeGeneratorStyle codeGeneratorStyle;
    public CodeGeneratorStyle getCodeGeneratorStyle() {
        return codeGeneratorStyle;
    }
    
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
            final CodeGeneratorStyle codeGeneratorStyle, 
            final Predicate<PlatformProperties> platformFilter,
            final Comparator<DeviceProperties> preferredDeviceComparator) {
        this.codeGeneratorStyle = codeGeneratorStyle;
        this.platformFilter = platformFilter;
        this.preferredDeviceComparator = preferredDeviceComparator;
    }

    /**
     * 
     * @deprecated use {@link OpenCLOptions#builder()} instead.
     * 
     */
    @Deprecated
    public OpenCLOptions(final boolean preferStackifier) {
        this.codeGeneratorStyle = preferStackifier
                ? CodeGeneratorStyle.STACKIFIER
                : CodeGeneratorStyle.RELOOPER;
        final OpenCLOptions defaults = defaults();
        this.platformFilter = defaults.getPlatformFilter();
        this.preferredDeviceComparator = defaults.getPreferredDeviceComparator();
    }

    public boolean isPreferStackifier() {
        return codeGeneratorStyle == CodeGeneratorStyle.STACKIFIER;
    }

    // -- DEFAULTS
    
    public static OpenCLOptions defaults() {
        return OpenCLOptions.builder().build();
    }
    
    // -- OPTIONS BUILDER
    
    public static final class OpenCLOptionsBuilder {
        private CodeGeneratorStyle codeGeneratorStyle = CodeGeneratorStyle.STACKIFIER;
        private Predicate<PlatformProperties> platformFilter = p -> true;
        private Comparator<DeviceProperties> preferredDeviceComparator = (a, b) -> Integer.compare(
                a.getNumberOfComputeUnits(),
                b.getNumberOfComputeUnits());
        
        /**
         * There are different output styles available for generated code.
         * @param codeGeneratorStyle
         */
        public OpenCLOptionsBuilder codeGeneratorStyle(CodeGeneratorStyle codeGeneratorStyle) {
            Objects.requireNonNull(codeGeneratorStyle);
            this.codeGeneratorStyle = codeGeneratorStyle;
            return this;
        }
        
        /**
         * Platforms are rejected if the platformFilter predicate returns false.
         * @param platformFilter
         */
        public OpenCLOptionsBuilder platformFilter(Predicate<PlatformProperties> platformFilter) {
            Objects.requireNonNull(platformFilter);
            this.platformFilter = platformFilter;
            return this;
        }
        
        /**
         * The device that compares highest is chosen by the {@link PlatformFactory}, unless explicitly 
         * overridden by system property {@code OPENCL_DEVICE}.  
         * @param preferredPlatformComparator
         */
        public OpenCLOptionsBuilder preferredDeviceComparator(Comparator<DeviceProperties> preferredDeviceComparator) {
            Objects.requireNonNull(preferredDeviceComparator);
            this.preferredDeviceComparator = preferredDeviceComparator;
            return this;
        }
        
        public OpenCLOptions build() {
            return new OpenCLOptions(codeGeneratorStyle, platformFilter, preferredDeviceComparator);
        }
    }
    
    public static OpenCLOptionsBuilder builder() {
        return new OpenCLOptionsBuilder();
    }
    

    
}
