package fr.ailegalcase.blog.scheduler;

public sealed interface CircuitBreakerVerdict
        permits CircuitBreakerVerdict.Ok,
                CircuitBreakerVerdict.DailyLimit,
                CircuitBreakerVerdict.WeeklyLimit {

    record Ok() implements CircuitBreakerVerdict {}

    record DailyLimit(long publishedToday, int cap) implements CircuitBreakerVerdict {}

    record WeeklyLimit(long publishedThisWeek, int cap) implements CircuitBreakerVerdict {}

    static CircuitBreakerVerdict ok() {
        return new Ok();
    }
}
