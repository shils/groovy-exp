Some experimental Groovy code... featuring:


### @AutoBreak

```groovy
@AutoBreak
int test() {
    int result = 0
    switch(1) {
        case 1:
        case 2:
            result = 2
        default:
            result = 3
    }
    return result
}
assert test() == 2

@AutoBreak(includeEmptyCases = true)
int test() {
    int result = 0
    switch(1) {
        case 1:
        case 2:
            result = 2
        default:
            result = 3
    }
    return result
}
assert test() == 0
```

### @LocalStatic

```groovy
class Bar {
    static final String CONSTANT = 'def'
}

class Foo {
    List<String> test() {
        @LocalStatic final List<String> x = ['abc', Bar.CONSTANT]
        x
    }
}
def foo = new Foo()
assert foo.test() == ['abc', 'def']
assert foo.test().is(new Foo().test())
```

### @Use

```groovy
@CompileStatic
class A {
    @Use(IntCat.class)
    int multiply(int a, int b){
        a.times(b)
    }

    static class IntCat {
        static int times(int a, int b) {
            a*b
        }
    }
}

assert new A().multiply(3,4) == A.IntCat.times(3,4)
```
