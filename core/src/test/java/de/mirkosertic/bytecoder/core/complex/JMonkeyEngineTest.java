/*
 * Copyright 2020 Mirko Sertic
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
package de.mirkosertic.bytecoder.core.complex;

import com.jme3.math.Vector3f;
import de.mirkosertic.bytecoder.core.test.UnitTestRunner;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.logging.Level;
import java.util.logging.Logger;

@RunWith(UnitTestRunner.class)
public class JMonkeyEngineTest {

    @Test
    @Ignore
    public void testVector() {
        Vector3f v = Vector3f.UNIT_X;
        v = v.add(0f, 2f, 0f);
        v.normalizeLocal();
    }

    @Test
    public void testLogger() {
        final Logger logger = Logger.getLogger("Testlogger");
        logger.setLevel(Level.INFO);
        logger.config("config");
        logger.fine("fine");
        logger.finer("finer");
        logger.finest("finest");
        logger.info("info");
        logger.severe("severe");
        logger.warning("warning");
    }
}
