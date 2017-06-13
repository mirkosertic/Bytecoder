#include <stdio.h>

#define REFERENCE void*
#define INT int

struct TFloatRuntimeClass {
};

struct TFloat {
    struct TFloatRuntimeClass* runtimeClass;
    INT value1;
};

void* TFloat_newInstance() {
    return 0;
}

void TFloat_init(REFERENCE aInstance, INT aValue) {
    struct TFloat* theInstance = (struct TFloat*) aInstance + 8;  // First bytes are used for memory allocation
    theInstance->value1 = aValue;
}

int main(int argc, char** argv) {
    void* theNew = TFloat_newInstance();
    TFloat_init(theNew, 10);
}