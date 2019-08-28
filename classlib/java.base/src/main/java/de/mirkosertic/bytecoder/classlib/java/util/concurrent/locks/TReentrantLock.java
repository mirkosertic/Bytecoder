/*
 * Copyright 2019 Mirko Sertic
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
package de.mirkosertic.bytecoder.classlib.java.util.concurrent.locks;

import de.mirkosertic.bytecoder.api.SubstitutesInClass;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

@SubstitutesInClass(completeReplace = true)
public class TReentrantLock implements Lock {

    @Override
    public void lock() {
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }

    @Override
    public boolean tryLock() {
        return true;
    }

    @Override
    public boolean tryLock(final long time, final TimeUnit unit) throws InterruptedException {
        return true;
    }

    @Override
    public void unlock() {
    }

    @Override
    public Condition newCondition() {
        return new Condition() {
            @Override
            public void await() throws InterruptedException {

            }

            @Override
            public void awaitUninterruptibly() {
            }

            @Override
            public long awaitNanos(final long nanosTimeout) throws InterruptedException {
                return 0;
            }

            @Override
            public boolean await(final long time, final TimeUnit unit) throws InterruptedException {
                return true;
            }

            @Override
            public boolean awaitUntil(final Date deadline) throws InterruptedException {
                return true;
            }

            @Override
            public void signal() {
            }

            @Override
            public void signalAll() {
            }
        };
    }
}
