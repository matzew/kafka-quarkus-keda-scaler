package net.wessendorf.kafka;

import io.smallrye.reactive.messaging.kafka.Record;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class QuarkusConsumer {
  private final Logger logger = Logger.getLogger(QuarkusConsumer.class);

  @Incoming("records-in")
  public void receive(Record<String, String> record) {
    logger.infof("Got a record: %s - %s", record.key(), record.value());
  }
}
