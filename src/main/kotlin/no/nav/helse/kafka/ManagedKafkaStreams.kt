package no.nav.helse.kafka

import io.prometheus.client.Gauge
import no.nav.helse.dusseldorf.ktor.health.HealthCheck
import no.nav.helse.dusseldorf.ktor.health.Healthy
import no.nav.helse.dusseldorf.ktor.health.Result
import no.nav.helse.dusseldorf.ktor.health.UnHealthy
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.Topology
import org.slf4j.LoggerFactory
import java.time.Duration
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.*

internal class ManagedKafkaStreams(
    private val name: String,
    topology: Topology,
    properties: Properties,
    private val unreadyAfterStreamStoppedIn: Duration
) : HealthCheck {

    private companion object {
        private val streamStatus = Gauge
            .build("stream_status",
                "Indikerer streamens status. 0 er Running, 1 er stopped.")
            .labelNames("stream")
            .register()
    }

    private fun safeStoppedIn() : Duration {
        val stoppedAt = stopped ?: LocalDateTime.now().minus(
            unreadyAfterStreamStoppedIn.seconds,
            ChronoUnit.SECONDS
        ).minusSeconds(1)
        return Duration.between(stoppedAt, LocalDateTime.now())
    }
    private fun Duration.hasReachedUnready() = compareTo(unreadyAfterStreamStoppedIn) >= 0

    private fun result(notRunningBlock: (Duration) -> Result) : Result {
        return when(kafkaStreams.state()) {
            KafkaStreams.State.PENDING_SHUTDOWN, KafkaStreams.State.NOT_RUNNING -> {
                notRunningBlock(safeStoppedIn())
            }
            KafkaStreams.State.RUNNING, KafkaStreams.State.REBALANCING, KafkaStreams.State.CREATED -> {
                Healthy(name, "Kjører som normalt i state ${kafkaStreams.state().name}.")
            }
            else -> UnHealthy(name, "Stream befinner seg i state '${kafkaStreams.state().name}'.")
        }
    }

    internal fun ready() = result { stoppedIn ->
        if (stoppedIn.hasReachedUnready()) {
            UnHealthy(name, "Stream har vært stoppet i ${stoppedIn.toMinutes()} minutter.")
        } else Healthy(name, "Stream har vært stoppet i ${stoppedIn.toMinutes()} minutter. Unready først etter ${unreadyAfterStreamStoppedIn.toMinutes()} minutter.")
    }
    override suspend fun check() = result { stoppedIn ->
        UnHealthy(name, "Stream har vært stoppet i ${stoppedIn.toMinutes()} minutter.")
    }

    private val log = LoggerFactory.getLogger("no.nav.$name.stream")
    private var kafkaStreams = managed(KafkaStreams(topology, properties))
    private var stopped : LocalDateTime? = null

    init {
        if (unreadyAfterStreamStoppedIn.toMinutes() < 1) throw IllegalStateException("unreadyAfterStreamStoppedIn må være over 1 minutt.")
        start()
    }

    private fun start() {
        log.info("Starter")
        streamStatus.running()
        kafkaStreams.start()
    }

    internal fun stop() {
        when (kafkaStreams.state()) {
            KafkaStreams.State.PENDING_SHUTDOWN, KafkaStreams.State.NOT_RUNNING -> log.info("Stoppes allerede. er i state ${kafkaStreams.state().name}")
            else -> {
                stopped = LocalDateTime.now()
                streamStatus.stopped()
                log.info("Stopper fra state ${kafkaStreams.state().name}")
                kafkaStreams.close()
            }
        }
    }

    private fun managed(streams: KafkaStreams) : KafkaStreams{
        streams.setStateListener { newState, oldState ->
            log.info("Stream endret state fra $oldState til $newState")
            if (newState == KafkaStreams.State.ERROR) {
                stop()
            }
        }

        streams.setUncaughtExceptionHandler { _, _ -> stop() }

        Runtime.getRuntime().addShutdownHook(Thread {
            stop()
        })

        return streams
    }

    private fun Gauge.running() = labels(name).set(0.0)
    private fun Gauge.stopped() = labels(name).set(1.0)
}

fun main() {
    val unreadyAfter = Duration.ofMinutes(15)
    val stoppedIn = Duration.ofMinutes(16)
    println(stoppedIn.compareTo(unreadyAfter))
}


