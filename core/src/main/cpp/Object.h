#ifndef OBJECT_H
#define OBJECT_H

#include "Runtimeclass.h"

class Object {
    private:
        static Runtimeclass* __java__runtimeclass;
    public:
        Object() {
            if (__java__runtimeclass == 0) {
                __java__runtimeclass = new Runtimeclass();

                // Perform static class initialization
            }
        }

        bool equals(Object* object) {
            return this == object;
        }
};

#endif