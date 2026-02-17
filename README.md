# Reactive Analytic Framework

Reactor Context-based analytic transaction framework for Spring WebFlux. A reactive replacement for `annotation-analytic-framework` that uses Reactor `Context` instead of `ThreadLocal` — making it safe for WebFlux/reactive pipelines where threads hop between operators.

## Why This Exists

The existing `annotation-analytic-framework` stores transaction state in `ThreadLocal`. In Spring WebFlux, a single request can execute across multiple threads, causing `ThreadLocal` state to be lost. This library stores transaction state in **Reactor Context**, which propagates correctly through the entire reactive chain regardless of thread scheduling.

## Requirements

- Java 17+
- Spring Boot 3.2+
- Project Reactor 3.6+

## Installation

Add the dependency to your service's `pom.xml`:

```xml
<dependency>
    <groupId>in.airtel.entertainment.platform</groupId>
    <artifactId>reactive-analytic-framework</artifactId>
    <version>1.0.0</version>
</dependency>
```

Spring Boot auto-configuration registers the AOP aspect automatically. No additional `@EnableXxx` annotation is needed.

## Quick Start

### 1. Annotation-Based (Recommended)

Annotate any method that returns `Mono` or `Flux`:

```java
@AnalyseTransaction(name = "getRecommendation")
public Mono<CollectionDTO> getRecommendation(String collectionId) {
    return fetchContent(collectionId)
        .transform(ReactiveAnalytic.update("collectionId", collectionId))
        .transform(ReactiveAnalytic.update("source", "multisource"));
}
```

The AOP aspect wraps the returned publisher with `trace()`, which:
1. Pushes a new `TransactionData` onto the Reactor Context stack
2. On completion/error, flushes the transaction as structured JSON to the `analyticLogger`

### 2. Operator-Based (Manual)

Use `trace()` directly when you don't want AOP:

```java
return fetchContent()
    .transform(ReactiveAnalytic.update("source", "multisource"))
    .transform(ReactiveAnalytic.trace("fetchContent"));
```

> **Operator ordering:** `update()` must be chained **before** `trace()`. This is because Reactor Context flows downstream-to-upstream — `trace()` places its `contextWrite` at the outermost position so all upstream operators can see the transaction stack.

### 3. Dynamic Values via `doOnEach`

For values that depend on the signal data (e.g., response size):

```java
return fetchContent()
    .doOnEach(signal -> {
        if (signal.isOnNext())
            ReactiveAnalytic.updateFromSignal(signal, "contentCount", signal.get().size());
    })
    .transform(ReactiveAnalytic.trace("fetchContent"));
```

### 4. Entity Extraction

Annotate classes with `@AnalysedEntity` and fields with `@Analysed`:

```java
@AnalysedEntity(name = "request")
public class ContentRequest {
    @Analysed(name = "msisdn")
    private String msisdn;

    @Analysed
    public String platform;

    @Analysed(name = "contentType")
    public String getType() {
        return this.type;
    }
}
```

Then extract all annotated fields into the transaction:

```java
return processRequest(request)
    .transform(ReactiveAnalytic.updateEntity(request))
    .transform(ReactiveAnalytic.trace("processRequest"));
```

If no fields are annotated with `@Analysed`, all public primitive fields are extracted automatically.

## API Reference

### `ReactiveAnalytic` (Main API)

| Method | Description |
|--------|-------------|
| `trace(String name)` | Returns `Function<Mono<T>, Mono<T>>` — wraps a Mono with a named transaction |
| `traceFlux(String name)` | Returns `Function<Flux<T>, Flux<T>>` — wraps a Flux with a named transaction |
| `update(String key, Object value)` | Returns `Function<Mono<T>, Mono<T>>` — sets a static key-value at subscription time |
| `updateEntity(Object entity)` | Returns `Function<Mono<T>, Mono<T>>` — extracts `@Analysed` fields from an entity |
| `updateFromSignal(Signal<?>, String, Object)` | Mutates the current transaction from a `doOnEach` callback |
| `updateEntityFromSignal(Signal<?>, Object)` | Extracts entity fields from a `doOnEach` callback |
| `currentTransaction(ContextView)` | Returns the current `TransactionData`, or null |

### Annotations

| Annotation | Target | Description |
|------------|--------|-------------|
| `@AnalyseTransaction` | Method | Marks a method for AOP-based transaction wrapping |
| `@AnalysedEntity` | Class | Marks a class for field extraction |
| `@Analysed` | Field, Method | Marks a field/method for extraction (optional `name` attribute) |

## Log Output

Each transaction is flushed as a JSON string to the SLF4J logger named `analyticLogger`:

```json
{"transactionName":"getRecommendation","startTime":"2026-02-16T09:30:00.000+0000","endTime":"2026-02-16T09:30:00.245+0000","timeTaken":245,"collectionId":"banner_xstream","contentCount":12}
```

On error, `exceptionMessage` and `exceptionClass` are included automatically.

## Structured JSON Logging with Logback

For production, configure a dedicated appender with `AnalyticJsonEncoder` in `logback-spring.xml`:

```xml
<appender name="ANALYTIC_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/analytic.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/analytic.%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>7</maxHistory>
    </rollingPolicy>
    <encoder class="in.airtel.entertainment.platform.analytic.encoder.AnalyticJsonEncoder" />
</appender>

<logger name="analyticLogger" level="INFO" additivity="false">
    <appender-ref ref="ANALYTIC_FILE" />
</logger>
```

The encoder produces structured JSON with `@timestamp`, `level`, `loggerName`, all MDC properties (e.g., `correlationid`), and the transaction data nested under `"transaction"`:

```json
{
  "@timestamp": "2026-02-16T18:00:00.245Z",
  "level": "INFO",
  "loggerName": "analyticLogger",
  "correlationid": "abc-123",
  "transaction": {
    "transactionName": "getRecommendation",
    "startTime": "2026-02-16T18:00:00.000+0000",
    "endTime": "2026-02-16T18:00:00.245+0000",
    "timeTaken": 245,
    "collectionId": "banner_xstream",
    "contentCount": 12
  }
}
```

## Optional WebFilter

To automatically create a root transaction for every HTTP request, enable the WebFilter:

```properties
# application.yml or application.properties
reactive.analytic.webfilter.enabled=true
```

This captures `httpMethod`, `requestPath`, `correlationid` (from header), and `signalType` for every request. All `@AnalyseTransaction` methods within the request will nest under this root transaction.

## Architecture

```
in.airtel.entertainment.platform.analytic/
  annotation/
    AnalyseTransaction.java         @annotation on methods returning Mono/Flux
    Analysed.java                   marks fields/methods for entity extraction
    AnalysedEntity.java             marks classes for entity extraction
  core/
    TransactionData.java            mutable transaction state (ConcurrentHashMap)
    TransactionStack.java           immutable stack stored in Reactor Context
    AnalyticContextKeys.java        Context key constant
    EntityExtractor.java            reflection-based @Analysed extraction (cached)
    AnalyticJsonLogger.java         serializes Map to JSON, logs via SLF4J
  api/
    ReactiveAnalytic.java           main API: trace(), update(), updateFromSignal()
  aop/
    AnalyseTransactionAspect.java   Spring AOP @Around for @AnalyseTransaction
  encoder/
    AnalyticJsonEncoder.java        Logback encoder producing structured JSON
  autoconfigure/
    ReactiveAnalyticAutoConfiguration.java   Spring Boot 3.x auto-config
  filter/
    AnalyticWebFilter.java          optional WebFilter for auto root transaction
```

### How Context Propagation Works

Reactor Context flows from **subscriber (downstream) to publisher (upstream)**. `contextWrite` enriches the context for all operators upstream of it.

```
Mono.just(data)                    <- sees enriched context (has transaction stack)
  .doOnEach(updateFromSignal)      <- sees enriched context
  .doOnEach(flush on complete)     <- sees enriched context
  .contextWrite(push transaction)  <- enriches context for everything above
```

- `trace()` places `contextWrite` at the **bottom** (downstream), making the transaction stack visible to all operators above it
- `update()` uses `contextWrite` to mutate the `ConcurrentHashMap` inside `TransactionData` at subscription time
- `updateFromSignal()` reads `signal.getContextView()` to find the stack and mutates `TransactionData` directly
- Nested transactions are supported — child data is promoted to the parent on completion

### Dependency Scopes

Only `reactor-core` and `slf4j-api` are compile-scope dependencies. Everything else (`spring-aop`, `spring-webflux`, `logback`, `jackson`) is `provided` — the consuming project supplies them.

## Coexistence with Existing Logging

This library uses its own Reactor Context key (`reactive-analytic-tx-stack`) and logger name (`analyticLogger`). It does not interfere with existing `CorrelationFilter`, `Logging.createContext()`, or MDC-based logging in discovery-api services.

## Building

```bash
mvn clean install
```
