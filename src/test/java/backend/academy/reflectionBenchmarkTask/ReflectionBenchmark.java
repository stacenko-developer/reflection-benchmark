package backend.academy.reflectionBenchmarkTask;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

@State(Scope.Thread)
public class ReflectionBenchmark {

    private static final boolean SHOULD_FAIL_ON_ERROR = true;
    private static final boolean SHOULD_DO_GC = true;

    private static final int FORKS_COUNT = 1;

    private static final int WARMUP_FORKS_COUNT = 1;
    private static final int WARMUP_ITERATIONS_COUNT = 10;
    private static final int WARMUP_TIME = 5;

    private static final int MEASUREMENT_ITERATIONS_COUNT = 50;
    private static final int MEASUREMENT_TIME = 5;

    private static final String TESTING_METHOD_NAME = "name";
    private static final String STUDENT_NAME = "Artem";
    private static final String STUDENT_SURNAME = "Stacenko";

    private Student student;
    private Method method;
    private MethodHandle methodHandle;
    private Function<Student, String> lambdaMetaFactory;

    public static void main(String[] args) throws RunnerException {
        final Options options = new OptionsBuilder()
            .include(ReflectionBenchmark.class.getSimpleName())
            .shouldFailOnError(SHOULD_FAIL_ON_ERROR)
            .shouldDoGC(SHOULD_DO_GC)
            .mode(Mode.AverageTime)
            .timeUnit(TimeUnit.NANOSECONDS)
            .forks(FORKS_COUNT)
            .warmupForks(WARMUP_FORKS_COUNT)
            .warmupIterations(WARMUP_ITERATIONS_COUNT)
            .warmupTime(TimeValue.seconds(WARMUP_TIME))
            .measurementIterations(MEASUREMENT_ITERATIONS_COUNT)
            .measurementTime(TimeValue.seconds(MEASUREMENT_TIME))
            .build();

        new Runner(options).run();
    }

    @Setup
    public void setup() throws Throwable {
        final MethodHandles.Lookup lookup = MethodHandles.lookup();
        final MethodType methodType = MethodType.methodType(String.class);
        final String interfaceMethodName = "apply";

        student = new Student(STUDENT_NAME, STUDENT_SURNAME);

        method = Student.class.getMethod(TESTING_METHOD_NAME);

        methodHandle = lookup.findVirtual(Student.class, TESTING_METHOD_NAME, methodType);

        lambdaMetaFactory = (Function<Student, String>) LambdaMetafactory.metafactory(
            lookup,
            interfaceMethodName,
            MethodType.methodType(Function.class),
            MethodType.methodType(Object.class, Object.class),
            methodHandle,
            MethodType.methodType(String.class, Student.class)
        ).getTarget().invokeExact();
    }

    @Benchmark
    public void directAccess(Blackhole blackhole) {
        final String name = student.name();

        blackhole.consume(name);
    }

    @Benchmark
    public void reflection(Blackhole blackhole) throws InvocationTargetException, IllegalAccessException {
        final String name = (String) method.invoke(student);

        blackhole.consume(name);
    }

    @Benchmark
    public void methodHandle(Blackhole blackhole) throws Throwable {
        final String name = (String) methodHandle.invoke(student);

        blackhole.consume(name);
    }

    @Benchmark
    public void lambdaMetaFactory(Blackhole blackhole) {
        final String name = lambdaMetaFactory.apply(student);

        blackhole.consume(name);
    }
}
