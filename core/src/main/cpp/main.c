#include "Testclass.h"

int main() {
    Testclass* test2 = new Testclass();
    test2->__java_constructor__(10);

    return test2->calculate(10, 20);
}