import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong; // Para somar resultados de forma thread-safe se necessário

public class ThreadComparisonDemo {

    // --- Configurações para o cenário I/O-Bound ---
    static final int IO_TASK_COUNT = 10_000; // Número grande de tarefas de I/O
    static final int IO_TASK_SLEEP_MS = 50;  // Cada tarefa "espera" por 50ms

    // --- Configurações para o cenário CPU-Bound ---
    // Usaremos um número de tarefas CPU-bound que seja um múltiplo do número de processadores
    // para ver como são distribuídas.
    static final int CPU_CORE_MULTIPLIER = 2;
    static final int CPU_TASK_COUNT = Runtime.getRuntime().availableProcessors() * CPU_CORE_MULTIPLIER;
    // Ajuste este valor para que cada tarefa CPU leve um tempo considerável (ex: algumas centenas de ms)
    static final long CPU_TASK_ITERATIONS = 30_000_000L; // Número de iterações para simular trabalho CPU

    // Variável para garantir que o JIT não otimize excessivamente o trabalho da CPU
    static volatile double sharedCpuResultSink = 0;

    /**
     * Simula uma tarefa I/O-bound que passa a maior parte do tempo esperando.
     */
    static void ioBoundTask(int taskNumber) {
        // System.out.printf("I/O Task %d starting on: %s%n", taskNumber, Thread.currentThread());
        try {
            Thread.sleep(IO_TASK_SLEEP_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("I/O Task " + taskNumber + " interrupted.");
        }
        // System.out.printf("I/O Task %d finished on: %s%n", taskNumber, Thread.currentThread());
    }

    /**
     * Simula uma tarefa CPU-bound que realiza cálculos intensos.
     */
    static void cpuBoundTask(int taskNumber, long iterations) {
        // System.out.printf("CPU Task %d starting on: %s%n", taskNumber, Thread.currentThread());
        double result = 0;
        for (long i = 0; i < iterations; i++) {
            // Operação matemática para consumir CPU
            result += Math.sin(i) * Math.cos(i) * Math.tan(i / (iterations / 10.0 + 1.0));
        }
        sharedCpuResultSink += result; // Usar o resultado para evitar otimização excessiva
        // System.out.printf("CPU Task %d finished on: %s%n", taskNumber, Thread.currentThread());
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("JDK Version: " + System.getProperty("java.version"));
        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println("======================================================");

        // --- Cenário I/O-Bound ---
        System.out.println("--- I/O-Bound Scenario ---");
        System.out.printf("Number of I/O tasks: %,d | Each task sleeps for: %d ms%n%n", IO_TASK_COUNT, IO_TASK_SLEEP_MS);

        // I/O-bound com Platform Threads (Pool Fixo)
        // Usamos um pool fixo relativamente grande, mas ainda limitado em comparação com o número de tarefas.
        int platformIoThreadPoolSize = 200;
        System.out.printf("Running I/O tasks with Platform Threads (Fixed Pool Size: %d)...%n", platformIoThreadPoolSize);
        ExecutorService platformIoExecutor = Executors.newFixedThreadPool(platformIoThreadPoolSize);
        long startTimePlatformIo = System.currentTimeMillis();
        for (int i = 0; i < IO_TASK_COUNT; i++) {
            final int taskNum = i;
            platformIoExecutor.submit(() -> ioBoundTask(taskNum));
        }
        platformIoExecutor.shutdown();
        boolean finishedPlatformIo = platformIoExecutor.awaitTermination(5, TimeUnit.MINUTES); // Espera um tempo razoável
        long endTimePlatformIo = System.currentTimeMillis();
        if (!finishedPlatformIo) System.out.println("Platform I/O tasks timed out!");
        System.out.printf("Platform Threads (I/O): Total time = %,d ms%n%n", (endTimePlatformIo - startTimePlatformIo));

        // I/O-bound com Virtual Threads
        System.out.println("Running I/O tasks with Virtual Threads...");
        ExecutorService virtualIoExecutor = Executors.newVirtualThreadPerTaskExecutor();
        long startTimeVirtualIo = System.currentTimeMillis();
        for (int i = 0; i < IO_TASK_COUNT; i++) {
            final int taskNum = i;
            virtualIoExecutor.submit(() -> ioBoundTask(taskNum));
        }
        virtualIoExecutor.shutdown();
        boolean finishedVirtualIo = virtualIoExecutor.awaitTermination(5, TimeUnit.MINUTES);
        long endTimeVirtualIo = System.currentTimeMillis();
        if (!finishedVirtualIo) System.out.println("Virtual I/O tasks timed out!");
        System.out.printf("Virtual Threads (I/O): Total time = %,d ms%n%n", (endTimeVirtualIo - startTimeVirtualIo));
        System.out.println("------------------------------------------------------");


        // --- Cenário CPU-Bound ---
        System.out.println("\n--- CPU-Bound Scenario ---");
        System.out.printf("Number of CPU tasks: %d | Iterations per CPU task: %,d%n%n", CPU_TASK_COUNT, CPU_TASK_ITERATIONS);
        sharedCpuResultSink = 0; // Resetar

        // CPU-bound com Platform Threads (Pool com tamanho igual ao número de cores)
        int platformCpuThreadPoolSize = Runtime.getRuntime().availableProcessors();
        System.out.printf("Running CPU tasks with Platform Threads (Fixed Pool Size: %d)...%n", platformCpuThreadPoolSize);
        ExecutorService platformCpuExecutor = Executors.newFixedThreadPool(platformCpuThreadPoolSize);
        long startTimePlatformCpu = System.currentTimeMillis();
        for (int i = 0; i < CPU_TASK_COUNT; i++) {
            final int taskNum = i;
            platformCpuExecutor.submit(() -> cpuBoundTask(taskNum, CPU_TASK_ITERATIONS));
        }
        platformCpuExecutor.shutdown();
        boolean finishedPlatformCpu = platformCpuExecutor.awaitTermination(5, TimeUnit.MINUTES);
        long endTimePlatformCpu = System.currentTimeMillis();
        if (!finishedPlatformCpu) System.out.println("Platform CPU tasks timed out!");
        System.out.printf("Platform Threads (CPU): Total time = %,d ms%n%n", (endTimePlatformCpu - startTimePlatformCpu));
        // System.out.println("CPU Platform Sink: " + sharedCpuResultSink); // Para debug
        sharedCpuResultSink = 0; // Resetar

        // CPU-bound com Virtual Threads
        // As virtual threads para tarefas CPU-bound ainda são limitadas pelo número de platform threads (carrier threads)
        // que, por padrão, é igual ao número de processadores disponíveis.
        System.out.println("Running CPU tasks with Virtual Threads...");
        ExecutorService virtualCpuExecutor = Executors.newVirtualThreadPerTaskExecutor();
        long startTimeVirtualCpu = System.currentTimeMillis();
        for (int i = 0; i < CPU_TASK_COUNT; i++) {
            final int taskNum = i;
            virtualCpuExecutor.submit(() -> cpuBoundTask(taskNum, CPU_TASK_ITERATIONS));
        }
        virtualCpuExecutor.shutdown();
        boolean finishedVirtualCpu = virtualCpuExecutor.awaitTermination(5, TimeUnit.MINUTES);
        long endTimeVirtualCpu = System.currentTimeMillis();
        if (!finishedVirtualCpu) System.out.println("Virtual CPU tasks timed out!");
        System.out.printf("Virtual Threads (CPU): Total time = %,d ms%n%n", (endTimeVirtualCpu - startTimeVirtualCpu));
        // System.out.println("CPU Virtual Sink: " + sharedCpuResultSink); // Para debug
        System.out.println("======================================================");
    }
}