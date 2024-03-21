package no.nav.syfo.infrastructure.metric

import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry

const val METRICS_NS = "syfojanitor-backend"

val METRICS_REGISTRY = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
