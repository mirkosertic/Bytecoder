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
package de.mirkosertic.bytecoder.integrationtest;

import de.mirkosertic.bytecoder.api.OpaqueProperty;
import de.mirkosertic.bytecoder.api.vue.Vue;
import de.mirkosertic.bytecoder.api.vue.VueBuilder;
import de.mirkosertic.bytecoder.api.vue.VueEventListener;
import de.mirkosertic.bytecoder.api.vue.VueInstance;
import de.mirkosertic.bytecoder.api.web.MouseEvent;

public class VueDemo {

    public interface MyVueInstance extends VueInstance {

        @OpaqueProperty
        void welcomemessage(final String aNewMessage);
    }

    public static void main(final String[] args) {

        final VueBuilder<MyVueInstance> theBuilder = Vue.builder();
        theBuilder.bindToTemplateSelector("#vuetemplate");
        theBuilder.data().setProperty("welcomemessage", "hello world!");
        theBuilder.addEventListener("clicked", new VueEventListener<MyVueInstance, MouseEvent>() {
            @Override
            public void handle(final MyVueInstance instance, final MouseEvent event) {
                instance.welcomemessage(String.format("hello world, you have clicked. Timestamp is %s", System.currentTimeMillis()));
            }
        });
        MyVueInstance instance = theBuilder.build();
    }
}
