import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.time.Duration;
import java.util.Arrays;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

public class FluxTest {

    @Test
    void firstFlux() {
        Flux.just("A", "B", "C")
                .log()
                .subscribe();
    }

    @Test
    void fluxFromIterable() {
        Flux.fromIterable(Arrays.asList("A", "B", "C"))
                .log()
                .subscribe();
    }

    @Test
    void fluxFromRange() {
        Flux.range(10, 5)
                .log()
                .subscribe();
    }

    @Test
    void fluxFromInterval() throws Exception {
        Flux.interval(Duration.ofSeconds(1))
                .log()
                .take(2)
                .subscribe();
        Thread.sleep(5000);
    }

    @Test
    void fluxRequest() {
        Flux.range(1, 5)
                .log()
                .subscribe(null,
                        null,
                        null,
                        s -> s.request(3)
                );
    }

    @Test
    void fluxCustomSubscriber() {
        Flux.range(1, 10)
                .log()
                .subscribe(new BaseSubscriber<Integer>() {
                    int elementsToProcess = 3;
                    int counter = 0;

                    public void hookOnSubscribe(Subscription subscription) {
                        System.out.println("Subscribed!");
                        request(elementsToProcess);
                    }

                    public void hookOnNext(Integer value) {
                        counter++;
                        if(counter == elementsToProcess) {
                            counter = 0;

                            Random r = new Random();
                            elementsToProcess = r.ints(1, 4)
                                    .findFirst().getAsInt();
                            request(elementsToProcess);
                        }
                    }
                });
    }

    @Test
    void fluxLimitRate() {
        Flux.range(1, 5)
                .log()
                .limitRate(3)
                .subscribe();
    }

    @Test
    void fluxJoinOtherFlux() {
        Flux<String> from = Flux.just("A", "B", "C", "D");
        Flux<String>  to = Flux.just("Z", "Z", "Z", "Z", "C");

        from.join(to, s -> Flux.never(), s -> Flux.never(), Tuples::of)
                .filter(tuple -> tuple.getT1().equals(tuple.getT2()))
                .map(tuple -> "If was found " + tuple.getT1() + " and " + tuple.getT2())
                .log()
                .subscribe(System.out::println);
    }

    @Test
    void fluxJoinTreeFlux() {
        Flux<String> from = Flux.just("A", "B", "C", "D");
        Flux<String>  to = Flux.just("Z", "Z", "Z", "Z", "C");
        Flux<Integer> number = Flux.range(1, 5);

        Predicate<Tuple2<String, String>> whenFromToElementAreEquals = t -> t.getT1().equals(t.getT2());
        Predicate<Tuple2<String, Integer>> whenC = t -> t.getT1().equals("C");
        Predicate<Tuple2<String, Integer>> when5 = t -> t.getT2().equals(5);

        from.join(to, s -> Flux.never(), s -> Flux.never(), Tuples::of)
                .filter(whenFromToElementAreEquals)
                .map(Tuple2::getT1)
                .join(number, s -> Flux.never(), s -> Flux.never(), Tuples::of)
                .filter(whenC.and(when5))
                .map(tuple -> "If was found " + tuple.getT1() + " and " + tuple.getT2())
                .log()
                .subscribe(System.out::println);
    }
}
