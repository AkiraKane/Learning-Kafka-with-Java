package tutorial1;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

public class ProducerDemoKeys {

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        // create a logger for my class
        final Logger logger = LoggerFactory.getLogger(ProducerDemoKeys.class);

        String bootstrapServers = "127.0.0.1:9092";

        // create Producer properties
        Properties properties = new Properties();

        /*
        // This is old way (hardcoded) to set up the properties
        properties.setProperty("bootstrap.servers", bootstrapServers);
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());

        */

        // Better way to set up the properties.
        properties.setProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // create the producer
        final KafkaProducer<String, String> producer = new KafkaProducer<String, String>(properties);

        for(int i = 0; i < 10; i++) {
            // create a producer record
            String topic = "first_topic";
            String value = "hello world" + Integer.toString(i);
            String key = "id_" + Integer.toString(i);

            ProducerRecord record = new ProducerRecord<String, String>(topic, key, value);

            logger.info("Key: " + key);  // log the key

            // id_0 is going to partition 1
            // id_1 is going to partition 0
            // id_2 is going to partition 2
            // id_3 is going to partition 0
            // id_4 is going to partition 2
            // id_5 is going to partition 2
            // id_6 is going to partition 0
            // id_7 is going to partition 2
            // id_8 is going to partition 1
            // id_9 is going to partition 2

            // When I run the code again, the above is the same, which means same key always go to the same partition.

            // send the data - asynchronous
            producer.send(record, new Callback() {
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    // executes every time a record is successfully sent or an exception is thrown
                    if (e == null) {
                        // the record was successfully sent
                        logger.info("Received new metadata. \n" +
                                "Topic: " + recordMetadata.topic() + "\n" +
                                "Partition: " + recordMetadata.partition() + "\n" +
                                "Offset: " + recordMetadata.offset() + "\n" +
                                "Timestamp: " + recordMetadata.timestamp());
                    } else {
                        logger.error("Error while producing", e);
                    }

                }
            }).get(); // block the .send() to make it synchronous - don't do it in production

            // It blocks your producer thread by calling .get(), so you cannot take advantage of btaching of requests,
            // if we are using producer.send().get(), will send each every record
            // if we are using producer.send(), will send records in batch.
        }

        // wait for data to be produced, flush data
        producer.flush();

        // flush and close producer
        producer.close();
    }
}

