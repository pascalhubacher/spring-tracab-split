package com.mas2022datascience.springtracabsplit.processor;

import com.mas2022datascience.avro.v1.Frame;
import com.mas2022datascience.avro.v1.Object;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@Configuration
@EnableKafkaStreams
public class KafkaStreamsRunnerDSL {

  @Value(value = "${spring.kafka.properties.schema.registry.url}") private String schemaRegistry;
  @Value(value = "${topic.tracab-01.name}") private String topicIn;
  @Value(value = "${topic.general-01.name}") private String topicOutPlayer;
  @Value(value = "${topic.general-02.name}") private String topicOutTeam;

  @Bean
  public KStream<String, Frame> kStream(StreamsBuilder kStreamBuilder) {

    // When you want to override serdes explicitly/selectively
    final Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url",
        schemaRegistry);
    final Serde<Frame> frameSerde = new SpecificAvroSerde<>();
    frameSerde.configure(serdeConfig, false); // `false` for record values

    KStream<String, Frame> stream = kStreamBuilder.stream(topicIn,
        Consumed.with(Serdes.String(), frameSerde));

    KStream<String, Object> playerStream = stream.flatMap(
      (key, value) -> {
        List<KeyValue<String, Object>> result = new LinkedList<>();
        for ( Object valueObject : value.getObjects() ) {
          result.add(KeyValue.pair(value.getMatch().getId()+"-"+valueObject.getId(), valueObject));
        }
        return result;
      }
    );
    playerStream.to(topicOutPlayer);

    return stream;

  }
}


