#ifndef TESTCLASS_H
#define TESTCLASS_H

#include "Object.h"

class Testclass : Object {
    private:
        static Runtimeclass* __java__runtimeclass;
        int v;
    public:
        Testclass() : Object() {
            if (__java__runtimeclass == 0) {
                __java__runtimeclass = new Runtimeclass();

                // Perform static class initialization here
            }
        }

        void __java_constructor__(int value) {
            this->v = value;
        }

        int calculate(int a, int b) {
            return v + a + b;
        }
};

#endif