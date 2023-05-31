module mock.statsd.main {
    requires kotlin.stdlib;
    requires org.slf4j;
    requires org.junit.jupiter.api;
    exports me.kpavlov.mocks.statsd.client;
    exports me.kpavlov.mocks.statsd.junit5;
    exports me.kpavlov.mocks.statsd.server;
}
